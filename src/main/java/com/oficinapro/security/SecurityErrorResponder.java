package com.oficinapro.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Erros de autenticação/autorização que acontecem na cadeia de filtros (antes de
 * chegar a um controller) não passam pelo {@code GlobalExceptionHandler}, que é um
 * {@code @RestControllerAdvice}. Sem isto o Spring devolveria uma página de erro HTML.
 * Este componente responde no mesmo formato JSON usado pelo resto da API.
 *
 * Instanciado à mão em {@code SecurityConfig}, junto do resto da configuração de segurança.
 */
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** 401 - token ausente, expirado ou inválido. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        escrever(response, HttpStatus.UNAUTHORIZED,
                "Não autenticado: envie um token válido no header Authorization.");
    }

    /** 403 - autenticado, mas sem permissão para o recurso. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        escrever(response, HttpStatus.FORBIDDEN,
                "Acesso negado: Você não tem permissão para acessar este recurso.");
    }

    /**
     * O corpo é montado à mão de propósito: são mensagens fixas, sem nenhum dado vindo
     * da requisição, então não há risco de injeção e evita-se depender do ObjectMapper aqui.
     */
    private void escrever(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
                {"status":%d,"error":"%s","message":"%s","timestamp":"%s"}"""
                .formatted(status.value(), status.getReasonPhrase(), message, LocalDateTime.now()));
    }
}
