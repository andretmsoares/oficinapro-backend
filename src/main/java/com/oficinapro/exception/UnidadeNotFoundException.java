package com.oficinapro.exception;

public class UnidadeNotFoundException extends RuntimeException {

    public UnidadeNotFoundException(Long id) {
        super("Unidade não encontrada com o id: " + id);
    }
}