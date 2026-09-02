package com.oficinapro.exception.pagamento;

public class PagamentoNotFoundException extends RuntimeException {
    public PagamentoNotFoundException(Long id) {
        super("Pagamento não encontrado com esse id: " + id);
    }
}
