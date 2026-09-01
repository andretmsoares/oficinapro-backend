package com.oficinapro.exception.unidade;

public class EnderecoAlreadyExistsException extends RuntimeException {
  public EnderecoAlreadyExistsException(String endereco) {
      super("Já existe uma unidade cadastrada com o endereço: " + endereco);
  }
}
