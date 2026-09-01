package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoRequestDTO(

        @NotNull(message = "O ID da Ordem de Serviço é obrigatório")
        Long osId,

        @NotNull(message = "O valor pago é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "O valor pago não pode ser negativo")
        BigDecimal valorPago,

        @DecimalMin(value = "0.0", inclusive = true, message = "O desconto não pode ser negativo")
        BigDecimal desconto,

        String obs
) {}