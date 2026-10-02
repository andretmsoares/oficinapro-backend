package com.oficinapro.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.oficinapro.dto.usuario.UsuarioMeUpdateRequestDTO;
import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.enums.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SenhaForteValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void abrir() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void fechar() {
    factory.close();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Oficina-2026-ok", "chave8letras1", "Tenant-teste-7421", "ab12cd34"})
  @DisplayName("senhas com 8+ caracteres, letra e número são aceitas")
  void aceitaSenhasBoas(String senha) {
    assertThat(SenhaForteValidator.valida(senha)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "curta1", // menos de 8
        "somenteletras", // sem número
        "123456789012", // sem letra
        "aaaaaaaa1", // poucos caracteres distintos
        "password1", // comum
        "SENHA123", // comum (sem diferenciar maiúsculas)
        "qwerty123", // comum
      })
  @DisplayName("senhas curtas, sem letra/número, repetitivas ou comuns são recusadas")
  void recusaSenhasFracas(String senha) {
    assertThat(SenhaForteValidator.valida(senha)).isFalse();
  }

  @Test
  @DisplayName("limite do BCrypt: mais de 72 bytes é recusado (conta bytes, não caracteres)")
  void recusaMaisDe72Bytes() {
    assertThat(SenhaForteValidator.valida("ab12".repeat(18))).isTrue(); // 72 bytes
    assertThat(SenhaForteValidator.valida("ab12".repeat(18) + "cd")).isFalse(); // 74 bytes
    assertThat(SenhaForteValidator.valida("é1".repeat(25))).isFalse(); // 25*3 = 75 bytes
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName(
      "valor nulo ou em branco passa no validador (campo opcional); a obrigatoriedade é do @NotBlank")
  void nuloEEmBrancoSaoIgnorados(String senha) {
    assertThat(new SenhaForteValidator().isValid(senha, null)).isTrue();
  }

  @Test
  @DisplayName("criar usuário: senha fraca vira erro de validação no campo password")
  void criarUsuarioComSenhaFraca() {
    var dto = new UsuarioRequestDTO("Ana", null, null, 1L, "ana.silva", "12345678", Role.GERENTE);

    assertThat(validator.validate(dto))
        .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("password"));
  }

  @Test
  @DisplayName("criar usuário: senha forte passa")
  void criarUsuarioComSenhaForte() {
    var dto =
        new UsuarioRequestDTO("Ana", null, null, 1L, "ana.silva", "Oficina-2026-ok", Role.GERENTE);

    assertThat(validator.validate(dto)).isEmpty();
  }

  @Test
  @DisplayName("atualizar usuário: senha é opcional, mas se vier precisa ser forte")
  void atualizarUsuario() {
    var semSenha =
        new UsuarioUpdateRequestDTO("Ana", null, null, 1L, "ana.silva", null, Role.GERENTE);
    var fraca =
        new UsuarioUpdateRequestDTO("Ana", null, null, 1L, "ana.silva", "abc", Role.GERENTE);

    assertThat(validator.validate(semSenha)).isEmpty();
    assertThat(validator.validate(fraca))
        .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("password"));
  }

  @Test
  @DisplayName("/me: a política de senha vale também aqui (antes não havia tamanho mínimo)")
  void atualizarMeAplicaPolitica() {
    var fraca =
        new UsuarioMeUpdateRequestDTO("Ana", "12345678901", "83999999999", "ana", "a", "atual123");
    var forte =
        new UsuarioMeUpdateRequestDTO(
            "Ana", "12345678901", "83999999999", "ana", "Oficina-2026-ok", "atual123");

    assertThat(validator.validate(fraca))
        .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("password"));
    assertThat(validator.validate(forte)).isEmpty();
  }
}
