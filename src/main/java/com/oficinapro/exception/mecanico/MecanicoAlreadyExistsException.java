package com.oficinapro.exception.mecanico;

public class MecanicoAlreadyExistsException extends RuntimeException {
    public MecanicoAlreadyExistsException() {
        super(
                "Mecânico já cadastrado."
        );
    }
}
