package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.NotNull;

public record PagamentoUpdateRequestDTO(
    @NotNull(message = "Observação não pode ser vazio.") String obs) {}
