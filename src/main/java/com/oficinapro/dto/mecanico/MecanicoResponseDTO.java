package com.oficinapro.dto.mecanico;

import java.math.BigDecimal;

public record MecanicoResponseDTO (
        Long id,
        String nome,
        String telefone,
        String documento,
        Long oficinaId,
        BigDecimal salario,
        String obs
){
}
