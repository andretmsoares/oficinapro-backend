package com.oficinapro.dto.estatisticas;

public record EstatisticasOficinaResponseDTO(
    Long oficinaId,
    String nomeOficina,
    long clientes,
    long mecanicos,
    long veiculos,
    long ordensDeServico) {}
