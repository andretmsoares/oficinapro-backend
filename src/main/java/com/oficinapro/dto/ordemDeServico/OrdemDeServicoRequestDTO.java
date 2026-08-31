package com.oficinapro.dto.ordemDeServico;

import jakarta.validation.constraints.NotNull;

public record OrdemDeServicoRequestDTO(

        @NotNull(message = "O ID da oficina é obrigatório")
        Long oficinaId,

        Long unidadeId,

        @NotNull(message = "O ID do veículo é obrigatório")
        Long veiculoId,

        Long clienteId,

        Long mecanicoId,

        String obs
) {
}
