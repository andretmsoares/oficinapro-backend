package com.oficinapro.dto.pagamento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagamentoRequestDTO(
    @NotNull(message = "O ID da Ordem de Serviço é obrigatório") Long osId,
    @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres") String obs) {}
