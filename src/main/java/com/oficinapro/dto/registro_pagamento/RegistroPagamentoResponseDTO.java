package com.oficinapro.dto.registro_pagamento;

import com.oficinapro.enums.MeioPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroPagamentoResponseDTO(
        Long id,
        Long pagamentoId,
        BigDecimal valor,
        MeioPagamento meioPagamento,
        LocalDateTime data
) {
}
