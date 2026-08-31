package com.oficinapro.dto.ordemDeServico;

import jakarta.validation.constraints.NotNull;

public record AtribuirClienteRequestDTO(
        @NotNull(message = "O ID do cliente é obrigatório")
        Long clienteId
) {
}
