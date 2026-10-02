package com.oficinapro.exception.usuario;

public class SenhaAtualInvalidaException extends RuntimeException {
  public SenhaAtualInvalidaException() {
    super("Senha atual incorreta. Informe a senha atual para alterar a senha ou o username.");
  }
}
