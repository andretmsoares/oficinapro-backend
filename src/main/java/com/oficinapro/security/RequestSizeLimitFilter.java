package com.oficinapro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita o tamanho do corpo das requisições que não são upload. O Spring Boot não impõe limite ao
 * corpo JSON, então sem isto um cliente autenticado (ou o login, que é público) poderia mandar
 * dezenas de MB por requisição. Uploads (multipart) seguem o limite próprio de {@code
 * spring.servlet.multipart}.
 *
 * <p>Com {@code Content-Length} conhecido a recusa é imediata (413); em requisições chunked, a
 * leitura é interrompida ao passar do limite.
 */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  private final long limiteEmBytes;

  public RequestSizeLimitFilter(
      @Value("${oficinapro.security.max-json-body-bytes:1048576}") long limiteEmBytes) {
    this.limiteEmBytes = limiteEmBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String contentType = request.getContentType();
    boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

    if (multipart) {
      filterChain.doFilter(request, response);
      return;
    }

    if (request.getContentLengthLong() > limiteEmBytes) {
      response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response
          .getWriter()
          .write(
              """
              {"status":413,"error":"Payload Too Large","message":"Corpo da requisição grande demais."}""");
      return;
    }

    filterChain.doFilter(new LimitedRequest(request, limiteEmBytes), response);
  }

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private final long limite;
    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request, long limite) {
      super(request);
      this.limite = limite;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream(), limite);
      }
      return stream;
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long limite;
    private long lidos;

    LimitedStream(ServletInputStream delegate, long limite) {
      this.delegate = delegate;
      this.limite = limite;
    }

    private void contar(int bytes) throws IOException {
      if (bytes > 0) {
        lidos += bytes;
        if (lidos > limite) {
          throw new IOException("Corpo da requisição excede o limite permitido");
        }
      }
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b >= 0) {
        contar(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      contar(n);
      return n;
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      delegate.setReadListener(readListener);
    }
  }
}
