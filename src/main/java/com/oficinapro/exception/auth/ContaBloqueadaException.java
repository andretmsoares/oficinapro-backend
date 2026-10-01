package com.oficinapro.exception.auth;

public class ContaBloqueadaException extends RuntimeException {
  public ContaBloqueadaException() {
    super(
        "Conta bloqueada por excesso de tentativas de login. Fale com o administrador do sistema.");
  }
}
