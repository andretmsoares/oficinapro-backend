package com.oficinapro.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PaginacaoTest {

  private static final Set<String> PERMITIDOS = Set.of("id", "valorTotal");
  private static final Sort PADRAO = Sort.by(Sort.Direction.DESC, "id");

  @Test
  @DisplayName("tamanho da página é limitado a 100, qualquer que seja o pedido")
  void limitaTamanho() {
    Pageable resultado = Paginacao.segura(PageRequest.of(2, 100000), PERMITIDOS, PADRAO);

    assertThat(resultado.getPageSize()).isEqualTo(100);
    assertThat(resultado.getPageNumber()).isEqualTo(2);
  }

  @Test
  @DisplayName(
      "sem paginação (unpaged) ou nulo vira a primeira página de 20, nunca a tabela inteira")
  void semPaginacaoViraPrimeiraPagina() {
    assertThat(Paginacao.segura(Pageable.unpaged(), PERMITIDOS, PADRAO))
        .isEqualTo(PageRequest.of(0, 20, PADRAO));
    assertThat(Paginacao.segura(null, PERMITIDOS, PADRAO)).isEqualTo(PageRequest.of(0, 20, PADRAO));
  }

  @Test
  @DisplayName("ordenação só por campos permitidos; o resto é descartado")
  void filtraOrdenacao() {
    Pageable pedido =
        PageRequest.of(
            0,
            10,
            Sort.by(
                Sort.Order.asc("valorTotal"),
                Sort.Order.desc("cliente.senha"),
                Sort.Order.asc("password")));

    assertThat(Paginacao.segura(pedido, PERMITIDOS, PADRAO).getSort())
        .isEqualTo(Sort.by(Sort.Order.asc("valorTotal")));
  }

  @Test
  @DisplayName("ordenação inteira inválida cai na ordenação padrão")
  void ordenacaoInvalidaUsaPadrao() {
    Pageable pedido = PageRequest.of(0, 10, Sort.by("oficina.cnpj"));

    assertThat(Paginacao.segura(pedido, PERMITIDOS, PADRAO).getSort()).isEqualTo(PADRAO);
  }

  @Test
  @DisplayName("termoLike: minúsculo, entre %, e os curingas do usuário viram texto comum")
  void termoLikeEscapaCuringas() {
    assertThat(Paginacao.termoLike("  Abc ")).isEqualTo("%abc%");
    assertThat(Paginacao.termoLike("100%")).isEqualTo("%100!%%");
    assertThat(Paginacao.termoLike("a_b")).isEqualTo("%a!_b%");
    assertThat(Paginacao.termoLike("oi!")).isEqualTo("%oi!!%");
    assertThat(Paginacao.termoLike(null)).isEqualTo("%%");
  }

  @Test
  @DisplayName("comoId reconhece número e código (#0012); outra coisa vira -1")
  void comoId() {
    assertThat(Paginacao.comoId("12")).isEqualTo(12L);
    assertThat(Paginacao.comoId("#0012")).isEqualTo(12L);
    assertThat(Paginacao.comoId(" 0012 ")).isEqualTo(12L);
    assertThat(Paginacao.comoId("abc")).isEqualTo(-1L);
    assertThat(Paginacao.comoId("12a")).isEqualTo(-1L);
    assertThat(Paginacao.comoId("")).isEqualTo(-1L);
    assertThat(Paginacao.comoId(null)).isEqualTo(-1L);
    assertThat(Paginacao.comoId("9".repeat(30))).as("estouraria o long").isEqualTo(-1L);
  }

  @Test
  @DisplayName("termoOuVazio apara e trata nulo")
  void termoOuVazio() {
    assertThat(Paginacao.termoOuVazio("  x ")).isEqualTo("x");
    assertThat(Paginacao.termoOuVazio(null)).isEmpty();
  }
}
