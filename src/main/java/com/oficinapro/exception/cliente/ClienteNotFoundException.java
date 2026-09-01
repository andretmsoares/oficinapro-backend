package com.oficinapro.exception.cliente;

public class ClienteNotFoundException extends RuntimeException {
    public ClienteNotFoundException() {
        super("Cliente não encontrada");
    }
}
