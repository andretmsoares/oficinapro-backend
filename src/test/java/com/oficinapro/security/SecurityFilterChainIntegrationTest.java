package com.oficinapro.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testa a cadeia de segurança real (SecurityConfig + JwtAuthenticationFilter +
 * SecurityErrorResponder).
 *
 * <p>Os testes de controller usam {@code @WebMvcTest}, que NÃO carrega o {@code SecurityConfig} —
 * lá só é possível validar {@code @PreAuthorize} (403). A distinção entre 401 (não autenticado) e
 * 403 (autenticado sem permissão) só pode ser verificada com a aplicação completa, que é o papel
 * deste teste.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityFilterChainIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  private MockMvc mockMvc() {
    if (mockMvc == null) {
      mockMvc =
          MockMvcBuilders.webAppContextSetup(context)
              .apply(
                  org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                      .springSecurity())
              .build();
    }
    return mockMvc;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/clientes",
        "/api/veiculos",
        "/api/mecanicos",
        "/api/unidades",
        "/api/oficinas",
        "/api/usuarios",
        "/api/ordens-servico",
        "/api/pagamentos/1",
        "/api/itens-os-peca/1",
        "/api/mao-obra/1",
        "/api/registros-pagamento/1",
        "/api/auth/me"
      })
  @DisplayName("requisição sem token a endpoint protegido deve retornar 401")
  void semTokenDeveRetornar401(String path) throws Exception {
    mockMvc()
        .perform(get(path))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @DisplayName("token malformado deve retornar 401, não 500")
  void tokenMalformadoDeveRetornar401() throws Exception {
    mockMvc()
        .perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("token com assinatura inválida deve retornar 401")
  void tokenComAssinaturaInvalidaDeveRetornar401() throws Exception {
    String jwtFalso =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJzdWIiOiJoYWNrZXIiLCJyb2xlIjoiQURNSU4ifQ"
            + ".assinatura-invalida";

    mockMvc()
        .perform(get("/api/oficinas").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtFalso))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("header Authorization sem o prefixo Bearer é ignorado e resulta em 401")
  void headerSemPrefixoBearerDeveRetornar401() throws Exception {
    mockMvc()
        .perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Token abc123"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST sem token também deve retornar 401, e não 403 por CSRF")
  void postSemTokenDeveRetornar401() throws Exception {
    mockMvc()
        .perform(post("/api/oficinas").contentType("application/json").content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("credenciais inválidas no login devem retornar 401 sem revelar se o usuário existe")
  void loginComCredenciaisInvalidasDeveRetornar401() throws Exception {
    mockMvc()
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"inexistente\",\"password\":\"senha-errada\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
  }

  @Test
  @DisplayName("o endpoint de login é público: não exige token para ser alcançado")
  void loginEhPublico() throws Exception {
    mockMvc()
        .perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(
            result -> {
              int statusRecebido = result.getResponse().getStatus();
              org.assertj.core.api.Assertions.assertThat(statusRecebido)
                  .as(
                      "o login deve ser alcançável sem autenticação; esperado 400"
                          + " (validação do corpo) ou 401 (credenciais), nunca 403")
                  .isIn(400, 401);
            });
  }

  @Test
  @DisplayName("o health check é público")
  void healthCheckEhPublico() throws Exception {
    mockMvc().perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
