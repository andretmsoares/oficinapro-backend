package com.oficinapro.exception;

public class MecanicoNotFoundException extends RuntimeException {
    public MecanicoNotFoundException() {
        super("Mecânico não encontrado");
    }
}
