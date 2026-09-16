package com.oficinapro.exception.usuario;

public class UsuarioAcessDeniedException extends RuntimeException {
  public UsuarioAcessDeniedException() {
    super("Usuário não está vinculado a nenhuma oficina");
  }
}
