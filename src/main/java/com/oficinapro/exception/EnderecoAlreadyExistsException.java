package com.oficinapro.exception;

public class EnderecoAlreadyExistsException extends RuntimeException {
  public EnderecoAlreadyExistsException(String endereco) {
      super("Já existe uma unidade cadastrada com o endereço: " + endereco);
  }
}
