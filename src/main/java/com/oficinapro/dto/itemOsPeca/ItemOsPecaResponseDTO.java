package com.oficinapro.dto.itemOsPeca;

import java.math.BigDecimal;

public record ItemOsPecaResponseDTO(
        Long id,
        Long osId,
        String nome,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal
) {}