package com.oficinapro.dto.itemOsPeca;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemOsPecaUpdateRequestDTO(
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres") String nome,
    @Positive(message = "Quantidade deve ser maior que zero")
        @Digits(integer = 10, fraction = 0, message = "Quantidade deve ser um número inteiro")
        BigDecimal quantidade,
    @Positive(message = "Valor deve ser maior que zero") @DecimalMin(value = "0.01")
        BigDecimal valorUnitario) {}
