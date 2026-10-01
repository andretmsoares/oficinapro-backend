package com.oficinapro.exception.logo;

public class LogoNotFoundException extends RuntimeException {
  public LogoNotFoundException() {
    super("Esta oficina não possui logo cadastrada.");
  }
}
