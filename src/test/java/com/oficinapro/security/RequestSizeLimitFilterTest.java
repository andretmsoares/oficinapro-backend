package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeLimitFilterTest {

  private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(100);
  private final FilterChain chain = mock(FilterChain.class);

  @Test
  @DisplayName(
      "corpo maior que o limite (Content-Length) é recusado com 413 sem chegar ao controller")
  void recusaPorContentLength() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/clientes");
    request.setContentType("application/json");
    request.setContent(new byte[101]);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("\"status\":413");
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  @DisplayName("corpo dentro do limite passa e é legível")
  void aceitaDentroDoLimite() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/clientes");
    request.setContentType("application/json");
    request.setContent("{\"a\":1}".getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(chain).doFilter(captor.capture(), any());
    assertThat(new String(captor.getValue().getInputStream().readAllBytes()))
        .isEqualTo("{\"a\":1}");
  }

  @Test
  @DisplayName("sem Content-Length (chunked), a leitura é interrompida ao passar do limite")
  void interrompeLeituraSemContentLength() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/clientes") {
          @Override
          public long getContentLengthLong() {
            return -1; // chunked: o tamanho não é conhecido de antemão
          }
        };
    request.setContentType("application/json");
    request.setContent(new byte[500]);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(chain).doFilter(captor.capture(), any());
    assertThatThrownBy(() -> captor.getValue().getInputStream().readAllBytes())
        .isInstanceOf(IOException.class)
        .hasMessageContaining("limite");
  }

  @Test
  @DisplayName("upload multipart não é limitado aqui (vale o limite do spring.servlet.multipart)")
  void multipartNaoELimitado() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/oficinas/1/logo");
    request.setContentType("multipart/form-data; boundary=xyz");
    request.setContent(new byte[5000]);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }
}
