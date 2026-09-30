package com.oficinapro.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtAuthenticationFilterTest {

  private JwtService jwtService;
  private UsuarioDetailsService usuarioDetailsService;
  private FilterChain chain;
  private JwtAuthenticationFilter filter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    jwtService = mock(JwtService.class);
    usuarioDetailsService = mock(UsuarioDetailsService.class);
    chain = mock(FilterChain.class);
    filter = new JwtAuthenticationFilter(jwtService, usuarioDetailsService);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static Jwt jwtDe(String subject) {
    return Jwt.withTokenValue("token").header("alg", "HS256").subject(subject).build();
  }

  private static Usuario usuario(String username, Role role, Boolean oficinaAtiva) {
    Usuario usuario = new Usuario();
    usuario.setUsername(username);
    usuario.setRole(role);
    if (oficinaAtiva != null) {
      usuario.setOficina(new Oficina(1L, "Oficina", "12345678000195", "83999999999", oficinaAtiva));
    }
    return usuario;
  }

  private void executar() throws Exception {
    filter.doFilter(request, response, chain);
  }

  @Test
  @DisplayName("sem header Authorization: não autentica, mas deixa a requisição seguir")
  void semHeaderNaoAutentica() throws Exception {
    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwtService, usuarioDetailsService);
  }

  @Test
  @DisplayName("header que não é Bearer deve ser ignorado")
  void headerQueNaoEBearerEhIgnorado() throws Exception {
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwtService, usuarioDetailsService);
  }

  @Test
  @DisplayName("Bearer sem token (apenas espaços) deve ser ignorado")
  void bearerVazioEhIgnorado() throws Exception {
    request.addHeader("Authorization", "Bearer    ");

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwtService, usuarioDetailsService);
  }

  @Test
  @DisplayName("token válido deve autenticar com o usuário recarregado do banco como principal")
  void tokenValidoAutentica() throws Exception {
    Usuario gerente = usuario("ana.gerente", Role.GERENTE, true);
    request.addHeader("Authorization", "Bearer abc.def.ghi");
    when(jwtService.decodificar("abc.def.ghi")).thenReturn(jwtDe("ana.gerente"));
    when(usuarioDetailsService.loadUserByUsername("ana.gerente")).thenReturn(gerente);

    executar();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isSameAs(gerente);
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_GERENTE");
    assertThat(authentication.getDetails()).isNotNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("ADMIN do SaaS, sem oficina, deve ser autenticado")
  void adminSemOficinaAutentica() throws Exception {
    Usuario admin = usuario("admin.saas", Role.ADMIN, null);
    request.addHeader("Authorization", "Bearer token-admin");
    when(jwtService.decodificar("token-admin")).thenReturn(jwtDe("admin.saas"));
    when(usuarioDetailsService.loadUserByUsername("admin.saas")).thenReturn(admin);

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isSameAs(admin);
  }

  @Test
  @DisplayName("oficina desativada após a emissão do token: não autentica")
  void oficinaDesativadaNaoAutentica() throws Exception {
    Usuario gerente = usuario("ana.gerente", Role.GERENTE, false);
    request.addHeader("Authorization", "Bearer token");
    when(jwtService.decodificar("token")).thenReturn(jwtDe("ana.gerente"));
    when(usuarioDetailsService.loadUserByUsername("ana.gerente")).thenReturn(gerente);

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("usuário não-ADMIN sem oficina: não autentica")
  void usuarioSemOficinaNaoAutentica() throws Exception {
    Usuario semOficina = usuario("mec", Role.MECANICO, null);
    request.addHeader("Authorization", "Bearer token");
    when(jwtService.decodificar("token")).thenReturn(jwtDe("mec"));
    when(usuarioDetailsService.loadUserByUsername("mec")).thenReturn(semOficina);

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("token inválido/expirado: não autentica e não interrompe a cadeia")
  void tokenInvalidoNaoAutentica() throws Exception {
    request.addHeader("Authorization", "Bearer adulterado");
    when(jwtService.decodificar("adulterado")).thenThrow(new JwtException("assinatura inválida"));

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
    verifyNoInteractions(usuarioDetailsService);
  }

  @Test
  @DisplayName("token válido de usuário removido depois da emissão: não autentica")
  void usuarioRemovidoNaoAutentica() throws Exception {
    request.addHeader("Authorization", "Bearer token");
    when(jwtService.decodificar("token")).thenReturn(jwtDe("fantasma"));
    when(usuarioDetailsService.loadUserByUsername("fantasma"))
        .thenThrow(new UsernameNotFoundException("Credenciais inválidas"));

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("contexto já autenticado não deve ser sobrescrito nem reprocessar o token")
  void contextoJaAutenticadoNaoEhSobrescrito() throws Exception {
    Authentication existente = new TestingAuthenticationToken("outro", "x", "ROLE_ADMIN");
    SecurityContextHolder.getContext().setAuthentication(existente);
    request.addHeader("Authorization", "Bearer token");

    executar();

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwtService, usuarioDetailsService);
  }
}
