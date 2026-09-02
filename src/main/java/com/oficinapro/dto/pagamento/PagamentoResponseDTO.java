package com.oficinapro.dto.pagamento;

import com.oficinapro.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponseDTO(
        Long id,
        Long osId,
        BigDecimal valorPago,
        String obs,
        LocalDateTime dataPagamentoTotal,
        StatusPagamento status
) {}