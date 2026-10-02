package com.oficinapro.service.auth;

import com.oficinapro.audit.AcaoAuditoria;
import com.oficinapro.audit.AuditLogService;
import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.security.jwt.JwtService;
import com.oficinapro.security.ratelimit.LoginThrottleService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final OficinaAccessValidator oficinaAccessValidator;
  private final LoginThrottleService loginThrottleService;
  private final UsuarioRepository usuarioRepository;
  private final AuditLogService auditLogService;

  public AuthServiceImpl(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      OficinaAccessValidator oficinaAccessValidator,
      LoginThrottleService loginThrottleService,
      UsuarioRepository usuarioRepository,
      AuditLogService auditLogService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.oficinaAccessValidator = oficinaAccessValidator;
    this.loginThrottleService = loginThrottleService;
    this.usuarioRepository = usuarioRepository;
    this.auditLogService = auditLogService;
  }

  /**
   * O limite de tentativas é checado ANTES da senha e depende só de IP + username digitado, nunca
   * da existência do usuário: usuário real e inexistente recebem exatamente as mesmas respostas
   * (401 genérico, ou 429 igual para os dois). O DaoAuthenticationProvider converte "usuário
   * inexistente" em BadCredentialsException, idêntica à de senha errada.
   */
  @Override
  @Transactional
  public LoginResponseDTO login(LoginRequestDTO request, String clientIp) {
    loginThrottleService.verificar(clientIp, request.username());

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (BadCredentialsException e) {
      loginThrottleService.registrarFalha(clientIp, request.username());
      // Só um hash curto do username vai para o log: quem digita a senha no campo de usuário não
      // pode deixá-la gravada em texto puro, e o valor digitado é controlado pelo atacante.
      log.warn("evento=LOGIN_FALHA ip={} usuario_hash={}", clientIp, hashCurto(request.username()));
      throw e;
    }

    Usuario usuario = (Usuario) authentication.getPrincipal();

    oficinaAccessValidator.validarOficinaAtiva(usuario);

    loginThrottleService.registrarSucesso(clientIp, request.username());

    auditLogService.registrarComoAtor(
        usuario, AcaoAuditoria.LOGIN_SUCESSO, "USUARIO", usuario.getId(), null);

    String token = jwtService.gerarToken(usuario);

    return LoginResponseDTO.bearer(
        token, jwtService.expiracao().toSeconds(), UsuarioResponseDTO.de(usuario));
  }

  @Override
  public UsuarioResponseDTO usuarioLogado() {
    return UsuarioResponseDTO.de(oficinaAccessValidator.getUsuarioAutenticado());
  }

  @Override
  @Transactional
  public void logout() {
    Usuario logado = oficinaAccessValidator.getUsuarioAutenticado();

    // O principal vem do filtro e está detached: recarrega a entidade gerenciada para gravar.
    Usuario usuario = usuarioRepository.findById(logado.getId()).orElse(null);
    if (usuario == null) {
      return;
    }

    usuario.revogarTokens();
    usuarioRepository.save(usuario);

    auditLogService.registrar(AcaoAuditoria.LOGOUT, "USUARIO", usuario.getId(), null);
  }

  private static String hashCurto(String valor) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest((valor == null ? "" : valor).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 4);
    } catch (NoSuchAlgorithmException e) {
      return "n/a";
    }
  }
}
