package com.oficinapro.dto.ordemDeServico;

public record FluxoMensalOSResponseDTO(
        Integer day,
        Long abertas,
        Long finalizadas
) {
}