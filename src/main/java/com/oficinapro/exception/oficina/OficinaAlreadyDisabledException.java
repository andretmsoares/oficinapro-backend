package com.oficinapro.exception.oficina;

public class OficinaAlreadyDisabledException extends RuntimeException {
  public OficinaAlreadyDisabledException() {
    super("Oficina está desativada");
  }
}
