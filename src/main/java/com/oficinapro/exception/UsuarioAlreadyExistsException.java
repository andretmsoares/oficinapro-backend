package com.oficinapro.exception;

public class UsuarioAlreadyExistsException extends RuntimeException {
    public UsuarioAlreadyExistsException() {
        super("Já existe um usuário com este documento nesta oficina");
    }
}