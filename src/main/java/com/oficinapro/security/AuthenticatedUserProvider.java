package com.oficinapro.security;

import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
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

  /**
   * {@link UsuarioAcessDeniedException} é unchecked (RuntimeException) de propósito: usar {@code
   * java.nio.file.AccessDeniedException} aqui já causou um bug real — por ser checked, ela forçou
   * try/catch em cima na pilha (controller e service), e o catch acabou engolindo a exceção em vez
   * de propagá-la, fazendo endpoints devolverem 200 com corpo vazio em vez de 403.
   */
  public Long getOficinaIdUsuarioLogado() {
    Usuario usuario = getUsuarioAutenticado();

    if (usuario.getOficina() == null) {
      throw new UsuarioAcessDeniedException();
    }

    return usuario.getOficina().getId();
  }
}
