package com.oficinapro.dto.ordemDeServico;

import com.oficinapro.enums.StatusOrdemDeServico;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusOSRequestDTO(
        @NotNull(message = "O novo status é obrigatório")
        StatusOrdemDeServico status
) {}