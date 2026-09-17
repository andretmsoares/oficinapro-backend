package com.oficinapro.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.OficinaDisabledException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.security.jwt.JwtService;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Autenticação. Classe que não tinha teste nenhum até aqui, apesar de ser a porta de entrada do
 * sistema.
 *
 * <p>A regra nova coberta aqui é o bloqueio de login para usuário de oficina desativada. Ela é
 * aplicada DEPOIS da checagem de credenciais, de propósito: informar "oficina desativada" antes de
 * validar a senha revelaria a existência do usuário para quem só está chutando credenciais.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  private static final String TOKEN = "jwt-de-teste";

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;
  @Mock private Authentication authentication;

  @InjectMocks private AuthServiceImpl service;

  private Usuario usuario(Role role, Boolean oficinaAtiva) {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setNome("Ana Gerente");
    usuario.setUsername("ana");
    usuario.setRole(role);

    if (oficinaAtiva != null) {
      Oficina oficina = new Oficina();
      oficina.setId(7L);
      oficina.setNome("Oficina Central");
      oficina.setAtivo(oficinaAtiva);
      usuario.setOficina(oficina);
    }

    return usuario;
  }

  private void autenticaComSucesso(Usuario usuario) {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(usuario);
  }

  // ------------------------------------------------------------------
  // login
  // ------------------------------------------------------------------

  @Test
  @DisplayName("login válido devolve token, validade em segundos e os dados do usuário")
  void loginValidoDevolveTokenEUsuario() {
    Usuario gerente = usuario(Role.GERENTE, true);
    autenticaComSucesso(gerente);
    when(jwtService.gerarToken(gerente)).thenReturn(TOKEN);
    when(jwtService.expiracao()).thenReturn(Duration.ofHours(8));

    LoginResponseDTO resposta = service.login(new LoginRequestDTO("ana", "senha1234"));

    assertThat(resposta.accessToken()).isEqualTo(TOKEN);
    assertThat(resposta.tokenType()).isEqualTo("Bearer");
    assertThat(resposta.expiresIn()).isEqualTo(28800L);
    assertThat(resposta.usuario().username()).isEqualTo("ana");
    assertThat(resposta.usuario().role()).isEqualTo(Role.GERENTE);
    assertThat(resposta.usuario().oficinaId()).isEqualTo(7L);
  }

  @Test
  @DisplayName("login do ADMIN do SaaS funciona e não expõe oficinaId")
  void loginDoAdminNaoTemOficina() {
    Usuario admin = usuario(Role.ADMIN, null);
    autenticaComSucesso(admin);
    when(jwtService.gerarToken(admin)).thenReturn(TOKEN);
    when(jwtService.expiracao()).thenReturn(Duration.ofHours(8));

    LoginResponseDTO resposta = service.login(new LoginRequestDTO("admin", "senha1234"));

    assertThat(resposta.usuario().role()).isEqualTo(Role.ADMIN);
    assertThat(resposta.usuario().oficinaId())
        .as("o ADMIN do SaaS não pertence a oficina alguma")
        .isNull();
  }

  @Test
  @DisplayName("credenciais inválidas propagam BadCredentialsException e não geram token")
  void credenciaisInvalidasNaoGeramToken() {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana", "senha-errada")))
        .isInstanceOf(BadCredentialsException.class);

    verify(jwtService, never()).gerarToken(any());
    verify(oficinaAccessValidator, never()).validarOficinaAtiva(any());
  }

  // ------------------------------------------------------------------
  // Bloqueio por oficina desativada
  // ------------------------------------------------------------------

  @Test
  @DisplayName("usuário de oficina desativada não recebe token")
  void oficinaDesativadaBloqueiaLogin() {
    Usuario gerente = usuario(Role.GERENTE, false);
    autenticaComSucesso(gerente);
    doThrow(new OficinaDisabledException())
        .when(oficinaAccessValidator)
        .validarOficinaAtiva(gerente);

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana", "senha1234")))
        .isInstanceOf(OficinaDisabledException.class);

    verify(jwtService, never())
        .gerarToken(any()); // nenhum token pode ser emitido para oficina desativada
  }

  @Test
  @DisplayName("a oficina só é verificada depois das credenciais, para não revelar usuários")
  void oficinaEhVerificadaDepoisDasCredenciais() {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana", "errada")))
        .isInstanceOf(BadCredentialsException.class);

    verify(oficinaAccessValidator, never())
        .validarOficinaAtiva(
            any()); // se rodasse antes, a mensagem denunciaria que o usuário existe
  }

  // ------------------------------------------------------------------
  // /api/auth/me
  // ------------------------------------------------------------------

  @Test
  @DisplayName("usuarioLogado devolve os dados do usuário do contexto de segurança")
  void usuarioLogadoDevolveDadosDoContexto() {
    Usuario gerente = usuario(Role.GERENTE, true);
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(gerente);

    UsuarioResponseDTO resposta = service.usuarioLogado();

    assertThat(resposta.username()).isEqualTo("ana");
    assertThat(resposta.role()).isEqualTo(Role.GERENTE);
    assertThat(resposta.oficinaId()).isEqualTo(7L);
  }
}
