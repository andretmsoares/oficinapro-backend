package com.oficinapro.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.exception.logo.LogoStorageIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogoStorageDesabilitadoTest {

  private final LogoStorageDesabilitado storage = new LogoStorageDesabilitado();

  @Test
  @DisplayName("salvar: sem bucket configurado o upload deve ser recusado")
  void salvarDeveSerRecusado() {
    assertThatThrownBy(() -> storage.salvar(1L, new byte[] {1, 2, 3}, "image/png"))
        .isInstanceOf(LogoStorageIndisponivelException.class);
  }

  @Test
  @DisplayName("ler: nunca há logo armazenada, então o resultado é vazio")
  void lerDeveRetornarVazio() {
    assertThat(storage.ler("logos/qualquer.png")).isEmpty();
  }

  @Test
  @DisplayName("remover: deve ser um no-op que não lança exceção")
  void removerNaoDeveLancar() {
    assertThatCode(() -> storage.remover("logos/qualquer.png")).doesNotThrowAnyException();
  }
}
