package com.oficinapro.dto.ordemDeServico;

import jakarta.validation.constraints.NotNull;

public record AtribuirMecanicoRequestDTO(
        @NotNull(message = "O ID do mecânico é obrigatório")
        Long mecanicoId
) {}