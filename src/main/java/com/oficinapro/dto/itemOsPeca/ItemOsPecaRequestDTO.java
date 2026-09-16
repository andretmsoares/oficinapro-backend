package com.oficinapro.dto.itemOsPeca;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemOsPecaRequestDTO(
    @NotNull(message = "O ID da OS é obrigatório") Long osId,
    @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,
    @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        @Digits(integer = 10, fraction = 0, message = "Quantidade deve ser um número inteiro")
        BigDecimal quantidade,
    @NotNull(message = "Valor unitário é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @DecimalMin(value = "0.01")
        BigDecimal valorUnitario) {}
