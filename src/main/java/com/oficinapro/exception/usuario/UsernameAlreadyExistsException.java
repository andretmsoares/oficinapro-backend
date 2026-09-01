package com.oficinapro.exception.usuario;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("Este username já está em uso");
    }
}