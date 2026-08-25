package com.oficinapro.dto.unidade;

public record UnidadeResponse(
        Long id,
        Long oficinaId,
        String nome,
        String endereco,
        String telefone
) {
}