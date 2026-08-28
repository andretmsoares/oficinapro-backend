package com.oficinapro.exception;

public class VeiculoNotFoundException extends RuntimeException {
    public VeiculoNotFoundException(Long id) {
        super("Veículo não encontrado: " + id);
    }
}