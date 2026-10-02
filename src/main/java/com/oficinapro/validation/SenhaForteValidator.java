package com.oficinapro.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

  static final int TAMANHO_MINIMO = 8;
  static final int BYTES_MAXIMOS = 72;

  /** Senhas muito comuns que passariam na regra de letra + número. */
  private static final Set<String> COMUNS =
      Set.of(
          "password1",
          "password123",
          "senha123",
          "senha12345",
          "admin123",
          "admin1234",
          "qwerty123",
          "qwerty1234",
          "abc12345",
          "abcd1234",
          "a1234567",
          "teste123",
          "teste1234",
          "mudar123",
          "trocar123",
          "oficina123",
          "oficinapro1",
          "welcome1",
          "iloveyou1",
          "12345678a",
          "123456789a");

  @Override
  public boolean isValid(String senha, ConstraintValidatorContext context) {
    return senha == null || senha.isBlank() || valida(senha);
  }

  /** Regra pura, reutilizável fora do Bean Validation (ex.: seed do ADMIN). */
  public static boolean valida(String senha) {
    if (senha == null || senha.length() < TAMANHO_MINIMO) {
      return false;
    }
    if (senha.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
      return false;
    }

    boolean temLetra = false;
    boolean temDigito = false;
    for (int i = 0; i < senha.length(); i++) {
      char c = senha.charAt(i);
      if (Character.isLetter(c)) {
        temLetra = true;
      } else if (Character.isDigit(c)) {
        temDigito = true;
      }
    }
    if (!temLetra || !temDigito) {
      return false;
    }

    if (senha.chars().distinct().count() < 4) {
      return false;
    }

    return !COMUNS.contains(senha.toLowerCase(Locale.ROOT));
  }
}
