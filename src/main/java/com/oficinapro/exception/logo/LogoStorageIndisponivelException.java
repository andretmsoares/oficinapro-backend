package com.oficinapro.exception.logo;

public class LogoStorageIndisponivelException extends RuntimeException {
  public LogoStorageIndisponivelException() {
    super("O armazenamento de logos não está configurado neste ambiente.");
  }
}
