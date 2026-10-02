package com.oficinapro.security.ratelimit;

import com.oficinapro.exception.auth.LoginTemporariamenteBloqueadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Proteção do login contra força bruta e password spraying, sem guardar estado por conta.
 *
 * <p>Duas barreiras, ambas por IP e mantidas em memória:
 *
 * <ul>
 *   <li><b>IP + username</b>: {@code tentativasPorBloqueio} falhas dentro da janela bloqueiam
 *       aquele par por {@code duracaoBloqueio}. Um terceiro, de outro IP, não consegue trancar a
 *       conta de ninguém, e o bloqueio nunca é permanente.
 *   <li><b>IP</b>: no máximo {@code maxTentativasPorIp} tentativas de login por janela de IP,
 *       qualquer que seja o username. Barra o spraying (1 senha em muitos usuários), que o contador
 *       por username nunca pegaria.
 * </ul>
 *
 * <p>A chave não depende de o usuário existir, então a resposta é idêntica para usernames reais e
 * inexistentes (não há oráculo de enumeração). Com várias instâncias da API o limite vale por
 * instância; o rate limit do Cloudflare continua sendo a barreira global.
 */
@Service
public class LoginThrottleService {

  private static final int LIMITE_DE_CHAVES = 50_000;
  private static final String IP_DESCONHECIDO = "desconhecido";

  private final Clock clock;
  private final int tentativasPorBloqueio;
  private final Duration janelaFalhas;
  private final Duration duracaoBloqueio;
  private final int maxTentativasPorIp;
  private final Duration janelaIp;

  private final Map<String, EstadoFalhas> falhasPorIpEUsername = new ConcurrentHashMap<>();
  private final Map<String, ContadorJanela> tentativasPorIp = new ConcurrentHashMap<>();

  @Autowired
  public LoginThrottleService(
      @Value("${oficinapro.login.tentativas-por-bloqueio:5}") int tentativasPorBloqueio,
      @Value("${oficinapro.login.janela-falhas:15m}") Duration janelaFalhas,
      @Value("${oficinapro.login.duracao-bloqueio:15m}") Duration duracaoBloqueio,
      @Value("${oficinapro.login.max-tentativas-por-ip:30}") int maxTentativasPorIp,
      @Value("${oficinapro.login.janela-ip:5m}") Duration janelaIp) {
    this(
        Clock.systemUTC(),
        tentativasPorBloqueio,
        janelaFalhas,
        duracaoBloqueio,
        maxTentativasPorIp,
        janelaIp);
  }

  LoginThrottleService(
      Clock clock,
      int tentativasPorBloqueio,
      Duration janelaFalhas,
      Duration duracaoBloqueio,
      int maxTentativasPorIp,
      Duration janelaIp) {
    this.clock = clock;
    this.tentativasPorBloqueio = tentativasPorBloqueio;
    this.janelaFalhas = janelaFalhas;
    this.duracaoBloqueio = duracaoBloqueio;
    this.maxTentativasPorIp = maxTentativasPorIp;
    this.janelaIp = janelaIp;
  }

  /**
   * Deve ser chamado antes de validar a senha. Conta a tentativa no limite do IP e recusa se o IP
   * estourou o limite ou se o par IP+username está bloqueado.
   *
   * @throws LoginTemporariamenteBloqueadoException (429) quando algum limite foi atingido
   */
  public void verificar(String ip, String username) {
    Instant agora = clock.instant();
    String ipChave = ip == null ? IP_DESCONHECIDO : ip;

    limparSeNecessario(agora);

    ContadorJanela contador =
        tentativasPorIp.computeIfAbsent(ipChave, k -> new ContadorJanela(agora));
    long segundosIp = contador.registrar(agora, janelaIp, maxTentativasPorIp);
    if (segundosIp > 0) {
      throw new LoginTemporariamenteBloqueadoException(segundosIp);
    }

    EstadoFalhas estado = falhasPorIpEUsername.get(chaveDoPar(ipChave, username));
    if (estado != null) {
      long segundos = estado.segundosDeBloqueio(agora);
      if (segundos > 0) {
        throw new LoginTemporariamenteBloqueadoException(segundos);
      }
    }
  }

  public void registrarFalha(String ip, String username) {
    Instant agora = clock.instant();
    String chave = chaveDoPar(ip == null ? IP_DESCONHECIDO : ip, username);

    falhasPorIpEUsername
        .computeIfAbsent(chave, k -> new EstadoFalhas(agora))
        .registrarFalha(agora, janelaFalhas, tentativasPorBloqueio, duracaoBloqueio);
  }

  public void registrarSucesso(String ip, String username) {
    falhasPorIpEUsername.remove(chaveDoPar(ip == null ? IP_DESCONHECIDO : ip, username));
  }

  /**
   * Libera um username de todos os bloqueios de login (qualquer IP). Usado pelo "desbloquear" do
   * GERENTE/ADMIN: um usuário legítimo que errou a senha vezes demais, atrás do mesmo IP de uma
   * rede compartilhada, não precisa esperar o bloqueio expirar.
   */
  public void liberar(String username) {
    String sufixo = "|" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    falhasPorIpEUsername.keySet().removeIf(chave -> chave.endsWith(sufixo));
  }

  /**
   * Mesma proteção para a confirmação da senha atual em {@code PUT /api/usuarios/me}: um token
   * roubado não pode ser usado para adivinhar a senha por tentativa e erro. A chave é o id do
   * usuário (a rota é autenticada), sem passar pelo limite por IP do login.
   */
  public void verificarSenhaAtual(Long usuarioId) {
    EstadoFalhas estado = falhasPorIpEUsername.get(chaveSenhaAtual(usuarioId));
    if (estado != null) {
      long segundos = estado.segundosDeBloqueio(clock.instant());
      if (segundos > 0) {
        throw new LoginTemporariamenteBloqueadoException(segundos);
      }
    }
  }

  public void registrarFalhaSenhaAtual(Long usuarioId) {
    Instant agora = clock.instant();
    falhasPorIpEUsername
        .computeIfAbsent(chaveSenhaAtual(usuarioId), k -> new EstadoFalhas(agora))
        .registrarFalha(agora, janelaFalhas, tentativasPorBloqueio, duracaoBloqueio);
  }

  public void registrarSucessoSenhaAtual(Long usuarioId) {
    falhasPorIpEUsername.remove(chaveSenhaAtual(usuarioId));
  }

  private static String chaveSenhaAtual(Long usuarioId) {
    return "me|" + usuarioId;
  }

  private static String chaveDoPar(String ip, String username) {
    String usuario = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    if (usuario.length() > 100) {
      usuario = usuario.substring(0, 100);
    }
    return ip + "|" + usuario;
  }

  /** Mantém a memória limitada: descarta entradas vencidas quando o mapa cresce demais. */
  private void limparSeNecessario(Instant agora) {
    if (falhasPorIpEUsername.size() > LIMITE_DE_CHAVES) {
      falhasPorIpEUsername.values().removeIf(e -> e.expirado(agora, janelaFalhas));
    }
    if (tentativasPorIp.size() > LIMITE_DE_CHAVES) {
      tentativasPorIp.values().removeIf(c -> c.expirado(agora, janelaIp));
    }
  }

  private static final class EstadoFalhas {
    private int falhas;
    private Instant inicioJanela;
    private Instant bloqueadoAte;

    EstadoFalhas(Instant agora) {
      this.inicioJanela = agora;
    }

    synchronized void registrarFalha(
        Instant agora, Duration janela, int limite, Duration duracaoBloqueio) {
      if (agora.isAfter(inicioJanela.plus(janela))) {
        falhas = 0;
        inicioJanela = agora;
      }
      falhas++;
      if (falhas >= limite) {
        bloqueadoAte = agora.plus(duracaoBloqueio);
        falhas = 0;
        inicioJanela = agora;
      }
    }

    synchronized long segundosDeBloqueio(Instant agora) {
      if (bloqueadoAte != null && bloqueadoAte.isAfter(agora)) {
        return Duration.between(agora, bloqueadoAte).toSeconds() + 1;
      }
      return 0;
    }

    synchronized boolean expirado(Instant agora, Duration janela) {
      boolean bloqueado = bloqueadoAte != null && bloqueadoAte.isAfter(agora);
      return !bloqueado && agora.isAfter(inicioJanela.plus(janela));
    }
  }

  private static final class ContadorJanela {
    private int tentativas;
    private Instant inicio;

    ContadorJanela(Instant agora) {
      this.inicio = agora;
    }

    /** Registra a tentativa; devolve os segundos até a janela reabrir, ou 0 se ainda cabe. */
    synchronized long registrar(Instant agora, Duration janela, int limite) {
      if (agora.isAfter(inicio.plus(janela))) {
        tentativas = 0;
        inicio = agora;
      }
      tentativas++;
      if (tentativas > limite) {
        return Duration.between(agora, inicio.plus(janela)).toSeconds() + 1;
      }
      return 0;
    }

    synchronized boolean expirado(Instant agora, Duration janela) {
      return agora.isAfter(inicio.plus(janela));
    }
  }
}
