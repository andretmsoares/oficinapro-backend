package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

  private static MockHttpServletRequest requisicao(String remoto) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoto);
    return request;
  }

  @Test
  @DisplayName("sem confiar em proxy, headers forjados são ignorados e vale o IP da conexão")
  void ignoraHeadersQuandoNaoConfia() {
    MockHttpServletRequest request = requisicao("10.0.0.5");
    request.addHeader("CF-Connecting-IP", "6.6.6.6");
    request.addHeader("X-Forwarded-For", "7.7.7.7");

    assertThat(ClientIpResolver.resolver(request, false)).isEqualTo("10.0.0.5");
  }

  @Test
  @DisplayName("atrás do Cloudflare, CF-Connecting-IP tem prioridade")
  void usaCloudflare() {
    MockHttpServletRequest request = requisicao("172.16.0.2");
    request.addHeader("CF-Connecting-IP", "203.0.113.9");
    request.addHeader("X-Forwarded-For", "198.51.100.1");

    assertThat(ClientIpResolver.resolver(request, true)).isEqualTo("203.0.113.9");
  }

  @Test
  @DisplayName("sem CF-Connecting-IP, usa o primeiro IP do X-Forwarded-For")
  void usaPrimeiroDoForwardedFor() {
    MockHttpServletRequest request = requisicao("172.16.0.2");
    request.addHeader("X-Forwarded-For", "198.51.100.1, 172.16.0.9");

    assertThat(ClientIpResolver.resolver(request, true)).isEqualTo("198.51.100.1");
  }

  @Test
  @DisplayName("header com lixo (não é IP) é descartado: o valor vai para logs e banco")
  void descartaHeaderInvalido() {
    MockHttpServletRequest request = requisicao("172.16.0.2");
    request.addHeader("CF-Connecting-IP", "1.1.1.1\r\nFORJADO: x");
    request.addHeader("X-Forwarded-For", "<script>");

    assertThat(ClientIpResolver.resolver(request, true)).isEqualTo("172.16.0.2");
  }

  @Test
  @DisplayName("aceita IPv6")
  void aceitaIpv6() {
    MockHttpServletRequest request = requisicao("::1");

    assertThat(ClientIpResolver.resolver(request, false)).isEqualTo("::1");
  }

  @Test
  @DisplayName("requisição nula devolve nulo")
  void requisicaoNula() {
    assertThat(ClientIpResolver.resolver(null, true)).isNull();
  }
}
