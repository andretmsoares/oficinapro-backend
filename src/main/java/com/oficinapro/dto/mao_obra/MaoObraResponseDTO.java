package com.oficinapro.dto.mao_obra;

import java.math.BigDecimal;

public record MaoObraResponseDTO(
        Long id,
        Long osId,
        BigDecimal valor,
        String descricao
) {
}
