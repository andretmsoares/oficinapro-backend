package com.oficinapro.exception.auth;

public class LoginTemporariamenteBloqueadoException extends RuntimeException {

  private final long segundosRestantes;

  public LoginTemporariamenteBloqueadoException(long segundosRestantes) {
    super(
        "Muitas tentativas de login. Tente novamente em "
            + Math.max(1, (long) Math.ceil(segundosRestantes / 60.0))
            + " minuto(s).");
    this.segundosRestantes = segundosRestantes;
  }

  public long getSegundosRestantes() {
    return segundosRestantes;
  }
}
