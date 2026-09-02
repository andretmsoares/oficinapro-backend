package com.oficinapro.dto.registro_pagamento;

import com.oficinapro.enums.MeioPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistroPagamentoRequestDTO(
        @NotNull(message = "O ID do Pagamento principal é obrigatório")
        Long pagamentoId,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor do registro deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "O meio de pagamento é obrigatório")
        MeioPagamento meioPagamento
) {
}
