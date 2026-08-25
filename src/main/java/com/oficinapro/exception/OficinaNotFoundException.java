package com.oficinapro.exception;

public class OficinaNotFoundException extends RuntimeException {

    public OficinaNotFoundException(Long id) {
        super("Oficina não encontrada com o id: " + id);
    }
}