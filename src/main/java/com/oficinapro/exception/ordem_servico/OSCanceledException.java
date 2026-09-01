package com.oficinapro.exception.ordem_servico;

public class OSCanceledException extends RuntimeException {
    public OSCanceledException() {
        super("Uma Ordem de Serviço cancelada não pode ter seu status alterado.");
    }
}
