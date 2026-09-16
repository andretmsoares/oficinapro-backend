package com.oficinapro.dto.mao_obra;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MaoObraRequestDTO(
    @NotNull(message = "O ID da Ordem de Serviço é obrigatório") Long osId,
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @DecimalMin(value = "0.01")
        BigDecimal valor,
    @NotBlank(message = "A Descrição é obrigatório")
        @Size(max = 500, message = "Descrição com no máximo 500 caracteres")
        String descricao) {}
