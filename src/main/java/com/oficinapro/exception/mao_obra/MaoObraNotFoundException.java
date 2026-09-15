package com.oficinapro.exception.mao_obra;

public class MaoObraNotFoundException extends RuntimeException {

    public MaoObraNotFoundException(Long id) {
        super("Mão de obra não encontrada: " + id);
    }
}