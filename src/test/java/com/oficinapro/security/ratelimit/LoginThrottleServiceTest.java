package com.oficinapro.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.exception.auth.LoginTemporariamenteBloqueadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginThrottleServiceTest {

  /** Relógio controlável: o teste avança o tempo sem dormir. */
  private static final class RelogioMutavel extends Clock {
    private Instant agora = Instant.parse("2026-10-01T12:00:00Z");

    void avancar(Duration duracao) {
      agora = agora.plus(duracao);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return agora;
    }
  }

  private RelogioMutavel relogio;
  private LoginThrottleService throttle;

  @BeforeEach
  void setUp() {
    relogio = new RelogioMutavel();
    // 5 falhas bloqueiam o par IP+username por 15 min; 30 tentativas por IP a cada 5 min
    throttle =
        new LoginThrottleService(
            relogio, 5, Duration.ofMinutes(15), Duration.ofMinutes(15), 30, Duration.ofMinutes(5));
  }

  private void falhar(String ip, String username, int vezes) {
    for (int i = 0; i < vezes; i++) {
      throttle.verificar(ip, username);
      throttle.registrarFalha(ip, username);
    }
  }

  @Test
  @DisplayName("abaixo do limite de falhas, nada é bloqueado")
  void abaixoDoLimiteNaoBloqueia() {
    falhar("1.1.1.1", "ana", 4);

    assertThatCode(() -> throttle.verificar("1.1.1.1", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("na 5ª falha o par IP+username é bloqueado, com os segundos restantes")
  void quintaFalhaBloqueia() {
    falhar("1.1.1.1", "ana", 5);

    assertThatThrownBy(() -> throttle.verificar("1.1.1.1", "ana"))
        .isInstanceOfSatisfying(
            LoginTemporariamenteBloqueadoException.class,
            e -> assertThat(e.getSegundosRestantes()).isBetween(890L, 901L));
  }

  @Test
  @DisplayName("o bloqueio nunca é permanente: expira com o tempo")
  void bloqueioExpira() {
    falhar("1.1.1.1", "ana", 5);

    relogio.avancar(Duration.ofMinutes(16));

    assertThatCode(() -> throttle.verificar("1.1.1.1", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("IP diferente não é afetado pelo bloqueio de outro IP (sem DoS de conta)")
  void bloqueioEPorIp() {
    falhar("1.1.1.1", "ana", 5);

    assertThatCode(() -> throttle.verificar("2.2.2.2", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("username é comparado sem diferenciar maiúsculas, para não burlar o limite")
  void usernameNaoDiferenciaMaiusculas() {
    falhar("1.1.1.1", "Ana", 3);
    falhar("1.1.1.1", "ANA", 2);

    assertThatThrownBy(() -> throttle.verificar("1.1.1.1", "ana"))
        .isInstanceOf(LoginTemporariamenteBloqueadoException.class);
  }

  @Test
  @DisplayName("falhas fora da janela não se acumulam")
  void falhasAntigasNaoSomam() {
    falhar("1.1.1.1", "ana", 4);
    relogio.avancar(Duration.ofMinutes(16));
    falhar("1.1.1.1", "ana", 4);

    assertThatCode(() -> throttle.verificar("1.1.1.1", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("sucesso zera as falhas do par")
  void sucessoZera() {
    falhar("1.1.1.1", "ana", 4);
    throttle.registrarSucesso("1.1.1.1", "ana");
    falhar("1.1.1.1", "ana", 4);

    assertThatCode(() -> throttle.verificar("1.1.1.1", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("usuário inexistente e existente são tratados exatamente igual")
  void inexistenteComportaIgual() {
    falhar("1.1.1.1", "fantasma", 5);

    assertThatThrownBy(() -> throttle.verificar("1.1.1.1", "fantasma"))
        .isInstanceOf(LoginTemporariamenteBloqueadoException.class);
  }

  @Test
  @DisplayName("limite por IP: spraying (1 tentativa por username) é barrado na 31ª tentativa")
  void limitePorIp() {
    for (int i = 0; i < 30; i++) {
      throttle.verificar("1.1.1.1", "usuario" + i);
    }

    assertThatThrownBy(() -> throttle.verificar("1.1.1.1", "usuario31"))
        .isInstanceOf(LoginTemporariamenteBloqueadoException.class);
    assertThatCode(() -> throttle.verificar("9.9.9.9", "usuario31")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("a janela do IP reabre depois de 5 minutos")
  void janelaDoIpReabre() {
    for (int i = 0; i < 31; i++) {
      try {
        throttle.verificar("1.1.1.1", "u" + i);
      } catch (LoginTemporariamenteBloqueadoException ignorada) {
        // esperado na última
      }
    }

    relogio.avancar(Duration.ofMinutes(6));

    assertThatCode(() -> throttle.verificar("1.1.1.1", "outro")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("liberar(username) remove o bloqueio em todos os IPs")
  void liberarRemoveBloqueios() {
    falhar("1.1.1.1", "ana", 5);
    falhar("2.2.2.2", "ana", 5);

    throttle.liberar("ANA");

    assertThatCode(() -> throttle.verificar("1.1.1.1", "ana")).doesNotThrowAnyException();
    assertThatCode(() -> throttle.verificar("2.2.2.2", "ana")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("senha atual: 5 erros seguidos bloqueiam a confirmação daquele usuário")
  void senhaAtualBloqueiaAposErros() {
    for (int i = 0; i < 5; i++) {
      throttle.verificarSenhaAtual(10L);
      throttle.registrarFalhaSenhaAtual(10L);
    }

    assertThatThrownBy(() -> throttle.verificarSenhaAtual(10L))
        .isInstanceOf(LoginTemporariamenteBloqueadoException.class);
    assertThatCode(() -> throttle.verificarSenhaAtual(11L)).doesNotThrowAnyException();

    throttle.registrarSucessoSenhaAtual(10L);
    assertThatCode(() -> throttle.verificarSenhaAtual(10L)).doesNotThrowAnyException();
  }
}
