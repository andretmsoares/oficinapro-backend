package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagamentoUpdateRequestDTO(
    @NotNull(message = "Observação não pode ser vazio.")
        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        String obs) {}
