package com.oficinapro.dto.unidade;

public record UnidadeResponseDTO(
        Long id,
        Long oficinaId,
        String nome,
        String endereco,
        String telefone
) {
}