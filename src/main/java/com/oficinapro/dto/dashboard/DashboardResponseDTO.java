package com.oficinapro.dto.dashboard;

import java.math.BigDecimal;

public record DashboardResponseDTO(
    Integer ordensAbertas,
    Integer veiculosCadastrados,
    Integer clientesCadastrados,
    BigDecimal aReceber,
    Integer pagamentosPendentes) {}
