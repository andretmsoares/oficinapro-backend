package com.oficinapro.exception;

public class ItemOsPecaNotFoundException extends RuntimeException {
    public ItemOsPecaNotFoundException(Long id) {
        super("Item de peça não encontrado: " + id);
    }
}