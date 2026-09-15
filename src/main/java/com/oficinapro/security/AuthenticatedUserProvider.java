package com.oficinapro.security;

import com.oficinapro.model.Usuario;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

  public Usuario getUsuarioAutenticado() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null
        || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof Usuario usuario)) {
      throw new AuthenticationCredentialsNotFoundException(
          "Nenhum usuário autenticado encontrado no contexto de segurança");
    }

    return usuario;
  }

  public Long getOficinaIdUsuarioLogado() {
    Usuario usuario = getUsuarioAutenticado();

    if (usuario.getOficina() == null) {
      throw new AuthenticationCredentialsNotFoundException(
          "Usuário não está vinculado a uma oficina");
    }

    return usuario.getOficina().getId();
  }
}
