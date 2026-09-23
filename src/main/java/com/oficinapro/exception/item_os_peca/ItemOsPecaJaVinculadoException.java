package com.oficinapro.exception.item_os_peca;

public class ItemOsPecaJaVinculadoException extends RuntimeException {

  public ItemOsPecaJaVinculadoException() {
    super("Peça já vinculada a uma OS");
  }
}
