package com.oficinapro.dto.unidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
        String endereco,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone
) {
}