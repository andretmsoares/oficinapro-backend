package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.NotNull;

public record PagamentoRequestDTO(
    @NotNull(message = "O ID da Ordem de Serviço é obrigatório") Long osId, String obs) {}
