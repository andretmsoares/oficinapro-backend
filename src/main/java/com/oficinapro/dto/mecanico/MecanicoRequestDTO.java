package com.oficinapro.dto.mecanico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MecanicoRequestDTO
        (
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
                String nome,

                @Size( max = 20, message = "Telefone deve possuir 20 caracteres")
                String telefone,

                @Size(max = 14, message = "Documento deve ter no máximo 14 caracteres")
                String documento,

                @NotNull(message = "O ID da oficina é obrigatório")
                Long oficinaId,

                @Positive(message = "O salário deve ser maior que zero")
                BigDecimal salario,

                String obs
        ) {
}
