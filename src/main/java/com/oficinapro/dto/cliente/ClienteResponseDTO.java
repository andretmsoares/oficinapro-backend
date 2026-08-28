package com.oficinapro.dto.cliente;

public record ClienteResponseDTO(
        Long id,
        String nome,
        String telefone,
        String documento,
        Long oficinaId
) {}
