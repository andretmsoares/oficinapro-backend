package com.oficinapro.dto.ordemDeServico;

import jakarta.validation.constraints.NotNull;

public record OrdemDeServicoRequestDTO(
    @NotNull(message = "O ID da oficina é obrigatório") Long oficinaId,
    @NotNull(message = "O ID da unidade é obrigatório") Long unidadeId,
    @NotNull(message = "O ID do veículo é obrigatório") Long veiculoId,
    Long clienteId,
    Long mecanicoId,
    String obs) {}
