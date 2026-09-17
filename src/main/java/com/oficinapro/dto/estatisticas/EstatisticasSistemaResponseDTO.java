package com.oficinapro.dto.estatisticas;

import java.util.List;

public record EstatisticasSistemaResponseDTO(
    long oficinas,
    long clientes,
    long mecanicos,
    long veiculos,
    long ordensDeServico,
    List<EstatisticasOficinaResponseDTO> porOficina) {}
