package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Proteção do login contra força bruta e spraying: o controle é por IP (+ username digitado), não
 * por conta, e a resposta é a mesma para usuário existente e inexistente.
 *
 * <p>O {@code LoginThrottleService} é um singleton do contexto de teste compartilhado entre
 * classes; por isso cada teste usa um IP próprio, em vez de depender de estado limpo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginThrottleIntegrationTest {

  private static final String SENHA = "senha-correta-1";
  private static final String USERNAME = "gerente.throttle";
  private static final AtomicInteger PROXIMO_IP = new AtomicInteger(1);

  @Autowired private WebApplicationContext context;
  @Autowired private EntityManager em;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  private Oficina oficina;
  private Usuario gerente;
  private String ip;

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context)
        .apply(
            org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                .springSecurity())
        .build();
  }

  @BeforeEach
  void criarUsuario() {
    ip = "198.51.100." + PROXIMO_IP.getAndIncrement();

    oficina = new Oficina();
    oficina.setNome("OFICINA THROTTLE");
    oficina.setCnpj("33333333000133");
    oficina.setAtivo(true);
    em.persist(oficina);

    gerente = usuario(oficina, USERNAME, Role.GERENTE);
    em.flush();
  }

  private Usuario usuario(Oficina o, String username, Role role) {
    Usuario u = new Usuario();
    u.setNome(username.toUpperCase());
    u.setOficina(o);
    u.setUsername(username);
    u.setPassword(passwordEncoder.encode(SENHA));
    u.setRole(role);
    em.persist(u);
    return u;
  }

  private ResultActions loginDe(String origem, String username, String senha) throws Exception {
    return mockMvc()
        .perform(
            post("/api/auth/login")
                .with(
                    request -> {
                      request.setRemoteAddr(origem);
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + senha + "\"}"));
  }

  private ResultActions login(String username, String senha) throws Exception {
    return loginDe(ip, username, senha);
  }

  private void falhar(String username, int vezes) throws Exception {
    for (int i = 0; i < vezes; i++) {
      login(username, "senha-errada-1");
    }
  }

  @Test
  @DisplayName("as 5 primeiras falhas respondem 401 e a seguinte já é bloqueada com Retry-After")
  void sextaTentativaEBloqueada() throws Exception {
    for (int i = 1; i <= 5; i++) {
      login(USERNAME, "senha-errada-1").andExpect(status().isUnauthorized());
    }

    login(USERNAME, "senha-errada-1")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minuto")))
        .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
  }

  @Test
  @DisplayName("durante o bloqueio até a senha correta é recusada")
  void bloqueioRecusaSenhaCorreta() throws Exception {
    falhar(USERNAME, 5);

    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("outro IP não é afetado: ninguém consegue trancar a conta de terceiros")
  void bloqueioDeUmIpNaoAfetaOutroIp() throws Exception {
    falhar(USERNAME, 6);
    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());

    loginDe("203.0.113." + PROXIMO_IP.getAndIncrement(), USERNAME, SENHA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  @DisplayName("login bem-sucedido zera o contador de falhas")
  void sucessoZeraContador() throws Exception {
    falhar(USERNAME, 3);
    login(USERNAME, SENHA).andExpect(status().isOk());

    falhar(USERNAME, 3);
    // 3 + sucesso + 3 falhas: nunca chegou a 5 seguidas, então ainda não está bloqueado
    login(USERNAME, "senha-errada-1").andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("usuário existente e inexistente recebem respostas indistinguíveis")
  void naoHaOraculoDeEnumeracao() throws Exception {
    String ipFalso = "198.51.100." + (200 + PROXIMO_IP.getAndIncrement());

    for (int i = 0; i < 5; i++) {
      login(USERNAME, "senha-errada-1").andExpect(status().isUnauthorized());
      loginDe(ipFalso, "nao.existe", "senha-errada-1")
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    // a partir daqui os dois são bloqueados exatamente da mesma forma
    login(USERNAME, "senha-errada-1")
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minuto")));
    loginDe(ipFalso, "nao.existe", "senha-errada-1")
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minuto")));
  }

  @Test
  @DisplayName("password spraying: um IP não pode tentar mais de 30 logins na janela")
  void limitePorIpBarraSpraying() throws Exception {
    // 1 tentativa por username: o contador por par IP+username nunca chegaria ao limite
    for (int i = 0; i < 30; i++) {
      login("usuario.spray." + i, "senha-errada-1").andExpect(status().isUnauthorized());
    }

    login("usuario.spray.31", "senha-errada-1").andExpect(status().isTooManyRequests());
    // nem a senha correta de um usuário real passa por esse IP enquanto o limite vale
    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("GERENTE da mesma oficina libera o usuário bloqueado, e ele volta a logar")
  void gerenteLiberaUsuarioDaPropriaOficina() throws Exception {
    Usuario mecanico = usuario(oficina, "mecanico.throttle", Role.MECANICO);
    em.flush();
    falhar("mecanico.throttle", 6);
    login("mecanico.throttle", SENHA).andExpect(status().isTooManyRequests());

    mockMvc()
        .perform(
            patch("/api/usuarios/" + mecanico.getId() + "/desbloquear")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.gerarToken(gerente)))
        .andExpect(status().isNoContent());

    login("mecanico.throttle", SENHA).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GERENTE de outra oficina não desbloqueia (404) e MECANICO não pode (403)")
  void desbloqueioRespeitaIsolamentoEPermissao() throws Exception {
    Oficina outra = new Oficina();
    outra.setNome("OUTRA OFICINA");
    outra.setCnpj("44444444000144");
    outra.setAtivo(true);
    em.persist(outra);
    Usuario gerenteOutra = usuario(outra, "gerente.outra", Role.GERENTE);
    Usuario mecanico = usuario(oficina, "mecanico.alvo", Role.MECANICO);
    em.flush();

    mockMvc()
        .perform(
            patch("/api/usuarios/" + mecanico.getId() + "/desbloquear")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.gerarToken(gerenteOutra)))
        .andExpect(status().isNotFound());

    mockMvc()
        .perform(
            patch("/api/usuarios/" + gerente.getId() + "/desbloquear")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.gerarToken(mecanico)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("logout revoga o token: o mesmo token deixa de valer em seguida")
  void logoutRevogaOToken() throws Exception {
    String token = jwtService.gerarToken(gerente);

    mockMvc()
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc()
        .perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    em.flush();
    em.clear();

    mockMvc()
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());

    assertThat(em.find(Usuario.class, gerente.getId()).getTokenVersion()).isEqualTo(1);
  }
}
