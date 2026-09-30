package com.oficinapro.service.auth;

import com.oficinapro.exception.auth.ContaBloqueadaException;
import com.oficinapro.exception.auth.LoginTemporariamenteBloqueadoException;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protecao contra forca bruta. A cada {@code tentativasPorBloqueio} falhas consecutivas o usuario
 * fica bloqueado por {@code duracaoBloqueio * n} (n = numero do bloqueio); no bloqueio de numero
 * {@code bloqueiosAtePermanente} a conta passa a exigir o desbloqueio por um administrador. Login
 * bem-sucedido zera o contador.
 *
 * <p>O estado fica no banco (e nao em memoria) para sobreviver a reinicios e valer com varias
 * instancias. Usernames inexistentes nao tem estado e recebem sempre a resposta generica de
 * credenciais invalidas.
 */
@Service
public class LoginAttemptService {

  private final UsuarioRepository usuarioRepository;
  private final int tentativasPorBloqueio;
  private final Duration duracaoBloqueio;
  private final int bloqueiosAtePermanente;

  public LoginAttemptService(
      UsuarioRepository usuarioRepository,
      @Value("${oficinapro.login.tentativas-por-bloqueio:5}") int tentativasPorBloqueio,
      @Value("${oficinapro.login.duracao-bloqueio:5m}") Duration duracaoBloqueio,
      @Value("${oficinapro.login.bloqueios-ate-permanente:3}") int bloqueiosAtePermanente) {
    this.usuarioRepository = usuarioRepository;
    this.tentativasPorBloqueio = tentativasPorBloqueio;
    this.duracaoBloqueio = duracaoBloqueio;
    this.bloqueiosAtePermanente = bloqueiosAtePermanente;
  }

  @Transactional(readOnly = true)
  public void verificarBloqueio(String username) {
    usuarioRepository.findByUsername(username).ifPresent(this::verificarBloqueio);
  }

  @Transactional
  public void registrarFalha(String username) {
    usuarioRepository
        .findByUsernameParaAtualizar(username)
        .ifPresent(
            usuario -> {
              int falhas = usuario.getFalhasLogin() + 1;
              usuario.setFalhasLogin(falhas);

              if (falhas % tentativasPorBloqueio == 0) {
                int numeroBloqueio = falhas / tentativasPorBloqueio;
                if (numeroBloqueio >= bloqueiosAtePermanente) {
                  usuario.setBloqueioPermanente(true);
                  usuario.setBloqueadoAte(null);
                } else {
                  usuario.setBloqueadoAte(
                      LocalDateTime.now().plus(duracaoBloqueio.multipliedBy(numeroBloqueio)));
                }
              }
              usuarioRepository.save(usuario);
            });
  }

  @Transactional
  public void registrarSucesso(String username) {
    usuarioRepository
        .findByUsernameParaAtualizar(username)
        .filter(u -> u.getFalhasLogin() > 0 || u.getBloqueadoAte() != null)
        .ifPresent(
            u -> {
              u.resetarBloqueioLogin();
              usuarioRepository.save(u);
            });
  }

  private void verificarBloqueio(Usuario usuario) {
    if (usuario.isBloqueioPermanente()) {
      throw new ContaBloqueadaException();
    }
    LocalDateTime ate = usuario.getBloqueadoAte();
    if (ate != null && ate.isAfter(LocalDateTime.now())) {
      throw new LoginTemporariamenteBloqueadoException(
          Duration.between(LocalDateTime.now(), ate).toSeconds() + 1);
    }
  }
}
