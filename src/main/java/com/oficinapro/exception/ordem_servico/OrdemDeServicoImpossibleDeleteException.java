package com.oficinapro.exception.ordem_servico;

public class OrdemDeServicoImpossibleDeleteException extends RuntimeException {
  public OrdemDeServicoImpossibleDeleteException() {
    super("Uma Ordem de Serviço não pode ser deletada com pagamento existente.");
  }
}
