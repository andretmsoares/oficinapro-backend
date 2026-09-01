package com.oficinapro.dto.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponseDTO(
        Long id,
        Long osId,
        BigDecimal valorPago,
        BigDecimal desconto,
        String obs,
        LocalDateTime dataPagamentoTotal
) {}