package com.oficinapro.exception.cliente;

public class ClienteAlreadyExistsException extends RuntimeException {
    public ClienteAlreadyExistsException() {
        super ("Cliente já cadastrado com esse documento");
    }
}
