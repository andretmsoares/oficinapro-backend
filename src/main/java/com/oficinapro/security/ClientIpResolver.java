package com.oficinapro.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Descobre o IP do cliente. Os headers de proxy ({@code CF-Connecting-IP}, {@code X-Forwarded-For})
 * só são confiáveis quando a API NÃO é alcançável diretamente (atrás do túnel Cloudflare, sem porta
 * publicada); por isso ficam desligados por padrão e são ligados por configuração ({@code
 * oficinapro.security.trust-proxy-headers=true}).
 */
public final class ClientIpResolver {

  private ClientIpResolver() {}

  public static String resolver(HttpServletRequest request, boolean confiarEmHeadersDeProxy) {
    if (request == null) {
      return null;
    }

    if (confiarEmHeadersDeProxy) {
      String cloudflare = limpar(request.getHeader("CF-Connecting-IP"));
      if (cloudflare != null) {
        return cloudflare;
      }

      String encaminhado = request.getHeader("X-Forwarded-For");
      if (encaminhado != null) {
        String primeiro = limpar(encaminhado.split(",")[0]);
        if (primeiro != null) {
          return primeiro;
        }
      }
    }

    return limpar(request.getRemoteAddr());
  }

  /** Aceita só caracteres de endereço IP (v4/v6): o valor vai para logs e para o banco. */
  private static String limpar(String valor) {
    if (valor == null) {
      return null;
    }
    String ip = valor.trim();
    if (ip.isEmpty() || ip.length() > 45 || !ip.matches("[0-9a-fA-F:.]+")) {
      return null;
    }
    return ip;
  }
}
