package com.oficinapro.dto.estatisticas;

public record EstatisticasOficinaResponseDTO(
    Long id,
    String nome,
    String cnpj,
    String telefone,
    Boolean ativo,
    long clientes,
    long mecanicos,
    long veiculos,
    long ordensDeServico) {}
