package com.oficinapro.util;

import java.text.Normalizer;
import java.util.Locale;

/** Padrão de armazenamento de textos: sem acentos, em caixa alta e sem espaços nas pontas. */
public final class TextoUtil {

  private TextoUtil() {}

  public static String normalizar(String texto) {
    if (texto == null) {
      return null;
    }

    String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

    return semAcentos.trim().toUpperCase(Locale.ROOT);
  }
}
