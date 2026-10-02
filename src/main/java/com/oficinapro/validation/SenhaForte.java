package com.oficinapro.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Política mínima de senha: de 8 a 72 bytes (limite do BCrypt), com pelo menos uma letra e um
 * dígito, e fora da lista de senhas comuns. Valor nulo ou em branco é considerado válido, para
 * servir também a campos opcionais; combine com {@code @NotBlank} quando for obrigatório.
 */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

  String message() default
      "Senha fraca: use de 8 a 72 caracteres, com pelo menos uma letra e um número, e evite"
          + " senhas comuns";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
