package com.oficinapro.exception.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // Retorna 403 Forbidden
public class UsuarioCannotDeleteSelfException extends RuntimeException {
  public UsuarioCannotDeleteSelfException() {
    super("Por motivos de segurança, você não pode excluir sua própria conta.");
  }
}
