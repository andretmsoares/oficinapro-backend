package com.oficinapro.exception.pagamento;

public class PagamentoAlreadyExistsException extends RuntimeException {
  public PagamentoAlreadyExistsException() {
    super("A ordem de serviço já possui um pagamento.");
  }
}
