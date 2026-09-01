package com.oficinapro.service.registro_pagamento;

import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;

import java.util.List;

public interface RegistroPagamentoService {

    List<RegistroPagamentoResponseDTO> listarPorPagamento(Long pagamentoId);

    RegistroPagamentoResponseDTO buscarPorId(Long id);

    RegistroPagamentoResponseDTO criar(RegistroPagamentoRequestDTO request);

    void deletar(Long id);
}