package com.oficinapro.exception.pagamento;

import java.math.BigDecimal;

public class PagamentoValorExcedidoException extends RuntimeException {
    public PagamentoValorExcedidoException(BigDecimal newValue, BigDecimal valorAtual) {

        super("Pagamento ultrapassou o valor da OS: valor total: " + valorAtual + ", valor pago: " + newValue);
    }
}
