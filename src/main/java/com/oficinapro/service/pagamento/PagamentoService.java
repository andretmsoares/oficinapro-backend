package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface PagamentoService {
  PagamentoResponseDTO criar(PagamentoRequestDTO request);

  PagamentoResponseDTO buscarPorId(Long id);

  PagamentoResponseDTO buscarPorOsId(Long osId);

  List<PagamentoResponseDTO> buscarPorOficina(Long oficinaId);

  List<PagamentoResponseDTO> buscarPorStatus(Long oficinaId, StatusPagamento status);

  BigDecimal calcularValorParaReceber(Long oficinaId);

  Pagamento buscarEntidadePorId(Long id);

  PagamentoResponseDTO atualizar(Long id, PagamentoRequestDTO request);

  PagamentoResponseDTO atualizarValorPago(Long id, BigDecimal valor);

  PagamentoResponseDTO estornarValorPago(Long id, BigDecimal valor);

  @Transactional
  void recalcularStatus(Long pagamentoId);
}
