package com.oficinapro.dto.ordemDeServico;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrdemDeServicoRequestDTO(
    @NotNull(message = "O ID da unidade é obrigatório") Long unidadeId,
    @NotNull(message = "O ID do veículo é obrigatório") Long veiculoId,
    Long clienteId,
    Long mecanicoId,
    @Size(max = 2000, message = "Observação deve ter no máximo 2000 caracteres") String obs) {}
