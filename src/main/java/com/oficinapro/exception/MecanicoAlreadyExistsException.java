package com.oficinapro.exception;

public class MecanicoAlreadyExistsException extends RuntimeException {
    public MecanicoAlreadyExistsException() {
        super(
                "Mecânico já cadastrado."
        );
    }
}
