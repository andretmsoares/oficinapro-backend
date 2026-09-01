package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.model.Pagamento;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

public interface PagamentoService {
    PagamentoResponseDTO criar(PagamentoRequestDTO request);
    PagamentoResponseDTO buscarPorId(Long id);
    PagamentoResponseDTO buscarPorOsId(Long osId);
    Pagamento buscarEntidadePorId(Long id);
    PagamentoResponseDTO atualizar(Long id, PagamentoRequestDTO request);
    PagamentoResponseDTO atualizarValorPago(Long id, BigDecimal valor);
    PagamentoResponseDTO estornarValorPago(Long id, BigDecimal valor);
    PagamentoResponseDTO aplicarDesconto(Long id, BigDecimal desconto);
    Optional<PagamentoResponseDTO> buscarPorOsIdSeExistir(Long osId);
}