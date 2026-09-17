package com.oficinapro.dto.itemOsPeca;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemOsPecaUpdateRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,
    // Peça é contada em unidades inteiras (2 pastilhas, 1 correia), nunca em fração.
    // Precisa ficar igual à validação de ItemOsPecaRequestDTO: manter as duas
    // divergentes já permitiu criar com quantidade inteira e depois editar para
    // fracionária pelo mesmo endpoint que deveria proibir os dois casos igualmente.
    @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        @Digits(integer = 10, fraction = 0, message = "Quantidade deve ser um número inteiro")
        BigDecimal quantidade,
    @NotNull(message = "Valor unitário é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @DecimalMin(value = "0.01")
        BigDecimal valorUnitario) {}
