package com.oficinapro.exception;

public class ClienteAlreadyExistsException extends RuntimeException {
    public ClienteAlreadyExistsException() {
        super ("Cliente já cadastrado com esse documento");
    }
}
