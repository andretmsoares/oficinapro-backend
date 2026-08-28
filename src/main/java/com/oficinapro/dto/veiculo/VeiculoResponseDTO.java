package com.oficinapro.dto.veiculo;

public record VeiculoResponseDTO(
        Long id,
        Long oficinaId,
        String modelo,
        Integer ano,
        String marca,
        String placa
) {}