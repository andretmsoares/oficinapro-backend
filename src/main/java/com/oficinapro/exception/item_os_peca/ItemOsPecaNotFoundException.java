package com.oficinapro.exception.item_os_peca;

public class ItemOsPecaNotFoundException extends RuntimeException {
    public ItemOsPecaNotFoundException(Long id) {
        super("Item de peça não encontrado: " + id);
    }
}