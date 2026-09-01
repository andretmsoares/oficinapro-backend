package com.oficinapro.exception.mecanico;

public class MecanicoNotFoundException extends RuntimeException {
    public MecanicoNotFoundException() {
        super("Mecânico não encontrado");
    }
}
