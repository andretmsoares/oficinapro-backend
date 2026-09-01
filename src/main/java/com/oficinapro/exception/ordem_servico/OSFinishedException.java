package com.oficinapro.exception.ordem_servico;

public class OSFinishedException extends RuntimeException {
    public OSFinishedException() {
        super("Uma Ordem de Serviço já entregue não pode voltar para ABERTA sem auditoria.");
    }
}
