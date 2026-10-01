package com.oficinapro.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextoUtilTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "José da Conceição|JOSE DA CONCEICAO",
        "  oficina   sul |OFICINA   SUL",
        "AÇÃO ÜNICA|ACAO UNICA",
        "Rua das Flores, 100|RUA DAS FLORES, 100",
        "Civic|CIVIC"
      })
  @DisplayName("remove acentos, coloca em caixa alta e apara as pontas")
  void normalizaTexto(String entrada, String esperado) {
    assertThat(TextoUtil.normalizar(entrada)).isEqualTo(esperado);
  }

  @Test
  @DisplayName("null continua null")
  void nullContinuaNull() {
    assertThat(TextoUtil.normalizar(null)).isNull();
  }
}
