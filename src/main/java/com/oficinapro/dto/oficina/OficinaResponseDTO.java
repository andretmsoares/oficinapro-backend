package com.oficinapro.dto.oficina;

public record OficinaResponseDTO(
        Long id,
        String nome,
        String cnpj,
        String telefone
) {
}