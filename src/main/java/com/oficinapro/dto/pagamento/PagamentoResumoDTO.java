package com.oficinapro.dto.pagamento;

import java.math.BigDecimal;

/**
 * Totais da oficina calculados no banco (soma/contagem sobre TODOS os pagamentos). O frontend não
 * soma a lista que tem na tela: com paginação ela é só uma página.
 *
 * @param totalRecebido soma do valor já recebido em todos os pagamentos
 * @param valorAReceber saldo em aberto dos pagamentos pendentes ou parciais
 * @param pendentes quantidade de pagamentos com saldo a receber
 */
public record PagamentoResumoDTO(
    BigDecimal totalRecebido, BigDecimal valorAReceber, long pendentes) {}
