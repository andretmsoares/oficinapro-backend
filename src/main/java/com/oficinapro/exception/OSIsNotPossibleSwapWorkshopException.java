package com.oficinapro.exception;

public class OSIsNotPossibleSwapWorkshopException extends RuntimeException {
    public OSIsNotPossibleSwapWorkshopException() {
        super("Não é possível trocar a oficina de uma OS");
    }
}
