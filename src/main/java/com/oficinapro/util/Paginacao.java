package com.oficinapro.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Apoio às listagens paginadas com busca no servidor.
 *
 * <p>A busca roda sobre TODOS os registros da oficina (a página é só o recorte do resultado), e não
 * sobre o que o frontend já carregou: assim um registro que está fora da página atual continua
 * sendo encontrado.
 */
public final class Paginacao {

  /** Teto de registros por página, qualquer que seja o {@code size} pedido pelo cliente. */
  public static final int TAMANHO_MAXIMO = 100;

  public static final int TAMANHO_PADRAO = 20;

  private Paginacao() {}

  /**
   * Sanitiza o {@link Pageable} vindo do cliente: limita o tamanho da página e só aceita ordenar
   * por campos permitidos (escalares da própria entidade). Ordenar por caminho livre permitiria
   * ordenar por campos internos (ex.: hash de senha) ou, em campos opcionais, descartar linhas sem
   * querer por causa do join implícito.
   */
  public static Pageable segura(Pageable pedido, Set<String> camposOrdenaveis, Sort padrao) {
    if (pedido == null || pedido.isUnpaged()) {
      return PageRequest.of(0, TAMANHO_PADRAO, padrao);
    }

    int tamanho = Math.min(Math.max(pedido.getPageSize(), 1), TAMANHO_MAXIMO);

    List<Sort.Order> permitidos = new ArrayList<>();
    for (Sort.Order ordem : pedido.getSort()) {
      if (camposOrdenaveis.contains(ordem.getProperty())) {
        permitidos.add(ordem);
      }
    }

    Sort ordenacao = permitidos.isEmpty() ? padrao : Sort.by(permitidos);

    return PageRequest.of(Math.max(pedido.getPageNumber(), 0), tamanho, ordenacao);
  }

  /**
   * Prepara um termo para {@code LIKE ... ESCAPE '!'}: minúsculo, com os curingas do usuário
   * ({@code %}, {@code _}) tratados como texto comum, entre {@code %}. Sem isso, digitar "%"
   * casaria tudo.
   */
  public static String termoLike(String termo) {
    String limpo = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);
    String escapado = limpo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    return "%" + escapado + "%";
  }

  /** Termo ausente ou em branco vira "", que as consultas tratam como "sem filtro". */
  public static String termoOuVazio(String termo) {
    return termo == null ? "" : termo.trim();
  }

  /**
   * Interpreta o termo como número de OS/código ("12", "#0012", "0012"). Devolve -1 quando não é um
   * número, valor que nenhum registro tem.
   */
  public static long comoId(String termo) {
    String limpo = termoOuVazio(termo).replace("#", "");
    if (limpo.isEmpty() || limpo.length() > 18 || !limpo.chars().allMatch(Character::isDigit)) {
      return -1L;
    }
    return Long.parseLong(limpo);
  }
}
