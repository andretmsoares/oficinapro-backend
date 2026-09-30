package com.oficinapro.service.auth;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final OficinaAccessValidator oficinaAccessValidator;
  private final LoginAttemptService loginAttemptService;

  public AuthServiceImpl(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      OficinaAccessValidator oficinaAccessValidator,
      LoginAttemptService loginAttemptService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.oficinaAccessValidator = oficinaAccessValidator;
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * O DaoAuthenticationProvider converte "usuário inexistente" em BadCredentialsException, de modo
   * que a resposta é idêntica à de senha errada e não permite descobrir quais usernames existem.
   */
  @Override
  public LoginResponseDTO login(LoginRequestDTO request) {
    // O bloqueio e checado ANTES da senha: conta bloqueada recusa ate a senha
    // correta.
    loginAttemptService.verificarBloqueio(request.username());

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (BadCredentialsException e) {
      loginAttemptService.registrarFalha(request.username());
      // Se esta falha acionou o bloqueio, avisa o usuario em vez de "credenciais
      // invalidas".
      loginAttemptService.verificarBloqueio(request.username());
      throw e;
    }

    Usuario usuario = (Usuario) authentication.getPrincipal();

    oficinaAccessValidator.validarOficinaAtiva(usuario);

    loginAttemptService.registrarSucesso(usuario.getUsername());

    String token = jwtService.gerarToken(usuario);

    return LoginResponseDTO.bearer(
        token, jwtService.expiracao().toSeconds(), UsuarioResponseDTO.de(usuario));
  }

  @Override
  public UsuarioResponseDTO usuarioLogado() {
    return UsuarioResponseDTO.de(oficinaAccessValidator.getUsuarioAutenticado());
  }
}
