package com.oficinapro.exception.item_os_peca;

public class ItemOsPecaNotFoundException extends RuntimeException {
  public ItemOsPecaNotFoundException() {
    super("Item de peça não encontrado");
  }
}
