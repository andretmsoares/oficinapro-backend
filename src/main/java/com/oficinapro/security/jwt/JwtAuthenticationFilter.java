package com.oficinapro.security.jwt;

import com.oficinapro.model.Usuario;
import com.oficinapro.security.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o header {@code Authorization: Bearer <token>} e popula o SecurityContext.
 *
 * O principal é a entidade {@link Usuario} recarregada do banco, e não as claims do
 * token, porque o {@code AuthenticatedUserProvider} e as regras de isolamento por
 * oficina dependem do estado atual do usuário.
 *
 * Token ausente ou inválido não gera erro aqui: o filtro apenas não autentica e deixa
 * o {@code AuthenticationEntryPoint} responder 401 caso a rota exija autenticação.
 * Isso mantém as rotas públicas (login, swagger, health) funcionando normalmente.
 *
 * Propositalmente NÃO é um bean: é instanciado à mão em {@code SecurityConfig}. Como
 * bean do tipo Filter, o Boot o registraria também na cadeia de filtros do servlet
 * (além da cadeia do Spring Security) e o {@code @WebMvcTest} passaria a carregá-lo,
 * exigindo nos testes de controller dependências que nada têm a ver com eles.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            Jwt jwt = jwtService.decodificar(token);

            Usuario usuario = (Usuario) usuarioDetailsService.loadUserByUsername(jwt.getSubject());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException exception) {
            // Assinatura, issuer ou validade inválidos.
            logger.debug("Token JWT rejeitado: " + exception.getMessage());
            SecurityContextHolder.clearContext();

        } catch (UsernameNotFoundException exception) {
            // Token válido, mas o usuário foi removido depois da emissão.
            logger.debug("Usuário do token JWT não existe mais");
            SecurityContextHolder.clearContext();
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        return token.isEmpty() ? null : token;
    }
}
