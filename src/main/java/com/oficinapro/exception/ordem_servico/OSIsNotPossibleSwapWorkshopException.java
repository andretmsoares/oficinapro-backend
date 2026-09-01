package com.oficinapro.exception.ordem_servico;

public class OSIsNotPossibleSwapWorkshopException extends RuntimeException {
    public OSIsNotPossibleSwapWorkshopException() {
        super("Não é possível trocar a oficina de uma OS");
    }
}
