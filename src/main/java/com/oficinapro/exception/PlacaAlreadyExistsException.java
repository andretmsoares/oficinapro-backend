package com.oficinapro.exception;

public class PlacaAlreadyExistsException extends RuntimeException {
    public PlacaAlreadyExistsException(String placa) {
        super("Já existe um veículo com a placa '" + placa + "' nesta oficina");
    }
}