package com.oficinapro.security;

import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.OficinaDisabledException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OficinaAccessValidator {

  private final AuthenticatedUserProvider authenticatedUserProvider;

  public OficinaAccessValidator(AuthenticatedUserProvider authenticatedUserProvider) {
    this.authenticatedUserProvider = authenticatedUserProvider;
  }

  public Usuario getUsuarioAutenticado() {
    return authenticatedUserProvider.getUsuarioAutenticado();
  }

  public void validarRole(Role... rolesPermitidas) {
    Usuario logado = getUsuarioAutenticado();

    for (Role role : rolesPermitidas) {
      if (logado.getRole() == role) {
        return;
      }
    }

    throw new AccessDeniedException("Usuário não possui permissão para realizar esta operação");
  }

  /**
   * Retorna a oficina do usuário logado.
   *
   * <p>Usuários ADMIN do SaaS não possuem oficina.
   *
   * <p>Não deve engolir a exceção do provider: fazer isso e devolver {@code null} aqui já causou um
   * bug real — o {@code null} seguia para {@code findByOficinaId(null)} (lista vazia disfarçada de
   * sucesso) ou para {@code oficinaId.equals(...)} (NullPointerException virando 500), em vez de
   * negar o acesso com 403 como deveria.
   */
  public Long getOficinaIdUsuarioLogado() {
    return authenticatedUserProvider.getOficinaIdUsuarioLogado();
  }

  /**
   * Valida se o usuário logado pode operar sobre a oficina informada.
   *
   * <p>ADMIN do SaaS pode operar sobre qualquer oficina.
   */
  public void validarAcessoOficina(Long oficinaId) {

    Long oficinaDoLogado = getOficinaIdUsuarioLogado();

    if (!oficinaDoLogado.equals(oficinaId)) {
      throw new AccessDeniedException("Você só pode acessar dados da sua própria oficina");
    }
  }

  /**
   * Valida acesso a um registro através da oficina à qual ele pertence.
   *
   * <p>Para evitar vazamento de informação, retorna uma exceção de "não encontrado" fornecida pelo
   * service chamador.
   */
  public void validarAcessoAoRegistro(Long oficinaDoRegistro, RuntimeException notFoundException) {

    Long oficinaDoLogado = getOficinaIdUsuarioLogado();

    if (!oficinaDoLogado.equals(oficinaDoRegistro)) {
      throw notFoundException;
    }
  }

  /** Versão para registros que necessariamente possuem oficina. */
  public void validarAcessoAoRegistro(
      Long oficinaDoRegistro,
      Long id,
      java.util.function.Function<Long, RuntimeException> notFoundException) {
    validarAcessoAoRegistro(oficinaDoRegistro, notFoundException.apply(id));
  }

  /**
   * Bloqueia o login (e qualquer operação que dependa dele) de um usuário cuja oficina foi
   * desativada. O ADMIN do SaaS não tem oficina e está sempre isento desta checagem.
   *
   * <p>{@code ativo == null} é tratado como desativado: um registro legado sem o flag preenchido
   * não pode virar brecha de acesso.
   */
  public void validarOficinaAtiva(Usuario usuario) {
    if (usuario.getRole() == Role.ADMIN) {
      return;
    }

    if (usuario.getOficina() == null) {
      throw new UsuarioAcessDeniedException();
    }

    if (!Boolean.TRUE.equals(usuario.getOficina().getAtivo())) {
      throw new OficinaDisabledException();
    }
  }
}
