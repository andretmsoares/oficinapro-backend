package com.oficinapro.dto.oficina;

public record OficinaResponse(
        Long id,
        String nome,
        String cnpj,
        String telefone
) {
}