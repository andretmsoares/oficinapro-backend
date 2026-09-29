package com.oficinapro.dto.registro_pagamento;

import com.oficinapro.enums.MeioPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegistroPagamentoRequestDTO(
    @NotNull(message = "O ID do Pagamento principal é obrigatório") Long pagamentoId,
    @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "1", message = "O valor do registro deve ser maior que zero")
        @Digits(integer = 10, fraction = 0, message = "Valor deve ser informado em centavos")
        BigDecimal valor,
    @NotNull(message = "O meio de pagamento é obrigatório") MeioPagamento meioPagamento) {}
