package com.oficinapro.exception.registro_pagamento;

public class RegistroPagamentoNotFoundException extends RuntimeException {
    public RegistroPagamentoNotFoundException(Long id) {
        super("Registro de pagamento não encontrado: " + id);
    }
}