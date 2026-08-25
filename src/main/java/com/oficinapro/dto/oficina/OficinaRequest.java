package com.oficinapro.dto.oficina;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OficinaRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "CNPJ é obrigatório")
        @Size(min = 14, max = 14, message = "CNPJ deve possuir 14 caracteres")
        String cnpj,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone
) {
}