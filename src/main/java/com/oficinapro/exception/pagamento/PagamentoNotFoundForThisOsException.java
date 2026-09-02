package com.oficinapro.exception.pagamento;

public class PagamentoNotFoundForThisOsException extends RuntimeException {
    public PagamentoNotFoundForThisOsException(Long osId) {
        super("Pagamento não encontrado para a OS: " + osId);
    }
}
