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
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
 * Protecao contra forca bruta no login: 5 falhas seguidas bloqueiam por um tempo; na terceira
 * sequencia de falhas a conta so volta com um administrador.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginBloqueioIntegrationTest {

  private static final String SENHA = "senha-correta-1";
  private static final String USERNAME = "gerente.bloqueio";

  @Autowired private WebApplicationContext context;
  @Autowired private EntityManager em;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  private Oficina oficina;
  private Usuario gerente;

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context)
        .apply(
            org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                .springSecurity())
        .build();
  }

  @BeforeEach
  void criarUsuario() {
    oficina = new Oficina();
    oficina.setNome("OFICINA BLOQUEIO");
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

  private ResultActions login(String username, String senha) throws Exception {
    return mockMvc()
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + senha + "\"}"));
  }

  private void falhar(String username, int vezes) throws Exception {
    for (int i = 0; i < vezes; i++) {
      login(username, "senha-errada-1");
    }
  }

  private void expirarBloqueio(String username) {
    Usuario u = usuarioRepository.findByUsername(username).orElseThrow();
    u.setBloqueadoAte(LocalDateTime.now().minusMinutes(1));
    usuarioRepository.saveAndFlush(u);
  }

  private Usuario recarregar(String username) {
    // Sem o flush, o em.clear() descartaria as alteracoes ainda nao gravadas (a transacao do
    // teste nunca faz commit) e o teste leria o estado antigo do banco.
    em.flush();
    em.clear();
    return usuarioRepository.findByUsername(username).orElseThrow();
  }

  @Test
  @DisplayName("as 4 primeiras falhas respondem 401 e a 5a bloqueia por tempo, com Retry-After")
  void quintaFalhaBloqueiaTemporariamente() throws Exception {
    for (int i = 1; i <= 4; i++) {
      login(USERNAME, "senha-errada-1").andExpect(status().isUnauthorized());
    }

    login(USERNAME, "senha-errada-1")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minuto")))
        .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
  }

  @Test
  @DisplayName("durante o bloqueio ate a senha correta e recusada")
  void bloqueioRecusaSenhaCorreta() throws Exception {
    falhar(USERNAME, 5);

    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("depois do prazo o login volta a funcionar e o contador e zerado")
  void bloqueioTemporarioExpira() throws Exception {
    falhar(USERNAME, 5);
    expirarBloqueio(USERNAME);

    login(USERNAME, SENHA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());

    Usuario u = recarregar(USERNAME);
    assertThat(u.getFalhasLogin()).isZero();
    assertThat(u.getBloqueadoAte()).isNull();
  }

  @Test
  @DisplayName("login bem-sucedido zera o contador de falhas")
  void sucessoZeraContador() throws Exception {
    falhar(USERNAME, 3);
    login(USERNAME, SENHA).andExpect(status().isOk());

    falhar(USERNAME, 3);
    // 3 + sucesso + 3 falhas: nunca chegou a 5 seguidas, entao ainda nao esta bloqueado
    login(USERNAME, "senha-errada-1").andExpect(status().isUnauthorized());
    assertThat(recarregar(USERNAME).getBloqueadoAte()).isNull();
  }

  @Test
  @DisplayName("na terceira sequencia de falhas a conta e bloqueada e pede o administrador")
  void terceiraSequenciaBloqueiaPermanentemente() throws Exception {
    falhar(USERNAME, 5);
    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());
    expirarBloqueio(USERNAME);

    falhar(USERNAME, 5);
    login(USERNAME, SENHA).andExpect(status().isTooManyRequests());
    expirarBloqueio(USERNAME);

    falhar(USERNAME, 4);
    login(USERNAME, "senha-errada-1")
        .andExpect(status().isLocked())
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("administrador")));

    // permanente: nem o tempo nem a senha correta liberam
    expirarBloqueio(USERNAME);
    login(USERNAME, SENHA).andExpect(status().isLocked());
    assertThat(recarregar(USERNAME).isBloqueioPermanente()).isTrue();
  }

  @Test
  @DisplayName("usuario inexistente nunca e bloqueado e recebe sempre a resposta generica")
  void usuarioInexistenteNaoRevelaNada() throws Exception {
    for (int i = 0; i < 12; i++) {
      login("nao.existe", "senha-errada-1")
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }
  }

  @Test
  @DisplayName("GERENTE da mesma oficina desbloqueia o usuario e ele volta a logar")
  void gerenteDesbloqueiaUsuarioDaPropriaOficina() throws Exception {
    Usuario mecanico = usuario(oficina, "mecanico.bloqueio", Role.MECANICO);
    em.flush();
    falhar("mecanico.bloqueio", 5);
    login("mecanico.bloqueio", SENHA).andExpect(status().isTooManyRequests());

    mockMvc()
        .perform(
            patch("/api/usuarios/" + mecanico.getId() + "/desbloquear")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.gerarToken(gerente)))
        .andExpect(status().isNoContent());

    login("mecanico.bloqueio", SENHA).andExpect(status().isOk());
  }

  @Test
  @DisplayName("desbloqueio tambem libera bloqueio permanente")
  void desbloqueioLiberaBloqueioPermanente() throws Exception {
    Usuario mecanico = usuario(oficina, "mecanico.permanente", Role.MECANICO);
    em.flush();
    for (int ciclo = 1; ciclo <= 3; ciclo++) {
      falhar("mecanico.permanente", 5);
      expirarBloqueio("mecanico.permanente");
    }
    login("mecanico.permanente", SENHA).andExpect(status().isLocked());

    mockMvc()
        .perform(
            patch("/api/usuarios/" + mecanico.getId() + "/desbloquear")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.gerarToken(gerente)))
        .andExpect(status().isNoContent());

    login("mecanico.permanente", SENHA).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GERENTE de outra oficina nao desbloqueia (404) e MECANICO nao pode (403)")
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
}
