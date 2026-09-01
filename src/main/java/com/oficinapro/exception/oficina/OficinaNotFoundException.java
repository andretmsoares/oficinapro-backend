package com.oficinapro.exception.oficina;

public class OficinaNotFoundException extends RuntimeException {

    public OficinaNotFoundException(Long id) {
        super("Oficina não encontrada com o id: " + id);
    }
}