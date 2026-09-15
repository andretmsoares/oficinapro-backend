package com.oficinapro.dto.mao_obra;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MaoObraRequestDTO(

    @NotNull(message = "O ID da Ordem de Serviço é obrigatório")
    Long osId,

    @NotNull(message = "O valor é obrigatório")
    BigDecimal valor,

    @NotNull(message = "A Descrição é obrigatório")
    String descricao
) {
}
