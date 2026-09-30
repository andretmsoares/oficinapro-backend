package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityErrorResponderTest {

  private final SecurityErrorResponder responder = new SecurityErrorResponder();

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  @DisplayName("commence: deve responder 401 em JSON sem expor a causa da falha")
  void deveResponder401EmJson() throws Exception {
    responder.commence(request, response, new BadCredentialsException("detalhe interno"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

    String corpo = response.getContentAsString();
    assertThat(corpo)
        .contains("\"status\":401")
        .contains("\"error\":\"Unauthorized\"")
        .contains("\"message\":\"Não autenticado: envie um token válido no header Authorization.\"")
        .contains("\"timestamp\":");
    assertThat(corpo).doesNotContain("detalhe interno");
  }

  @Test
  @DisplayName("handle: deve responder 403 em JSON sem expor a causa da negativa")
  void deveResponder403EmJson() throws Exception {
    responder.handle(request, response, new AccessDeniedException("regra interna"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/json");

    String corpo = response.getContentAsString();
    assertThat(corpo)
        .contains("\"status\":403")
        .contains("\"error\":\"Forbidden\"")
        .contains("Acesso negado: Você não tem permissão para acessar este recurso.")
        .contains("\"timestamp\":");
    assertThat(corpo).doesNotContain("regra interna");
  }
}
