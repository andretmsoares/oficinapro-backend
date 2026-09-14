package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoRequestDTO(

        @NotNull(message = "O ID da Ordem de Serviço é obrigatório")
        Long osId,

        String obs
) {}