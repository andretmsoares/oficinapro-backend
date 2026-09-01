package com.oficinapro.dto.ordemDeServico;

import com.oficinapro.enums.StatusOrdemDeServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdemDeServicoResponseDTO(
        Long id,
        Long oficinaId,
        Long unidadeId,
        Long veiculoId,
        Long clienteId,
        Long mecanicoId,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        StatusOrdemDeServico status,
        String obs,
        BigDecimal valorTotal,
        BigDecimal valorComDesconto
) {}