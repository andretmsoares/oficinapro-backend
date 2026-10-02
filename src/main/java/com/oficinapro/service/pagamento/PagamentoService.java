package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoResumoDTO;
import com.oficinapro.dto.pagamento.PagamentoUpdateRequestDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Pagamento;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface PagamentoService {
  PagamentoResponseDTO criar(PagamentoRequestDTO request);

  PagamentoResponseDTO buscarPorId(Long id);

  PagamentoResponseDTO buscarPorOsId(Long osId);

  /**
   * Página dos pagamentos da oficina, com busca no servidor sobre todos eles.
   *
   * @param termo parte do número da OS ou do pagamento; vazio = sem busca
   * @param status filtro de status; nulo = qualquer
   */
  Page<PagamentoResponseDTO> buscarPorOficina(
      Long oficinaId, String termo, StatusPagamento status, Pageable pageable);

  Page<PagamentoResponseDTO> buscarPorStatus(
      Long oficinaId, StatusPagamento status, Pageable pageable);

  long contarPorStatus(Long oficinaId, StatusPagamento status);

  /** Totais da oficina somados no banco (não dependem da página exibida). */
  PagamentoResumoDTO resumo(Long oficinaId);

  /** Pagamentos das OS informadas (no máximo as de uma página), restritos à oficina. */
  List<PagamentoResponseDTO> buscarPorOsIds(Long oficinaId, Collection<Long> osIds);

  BigDecimal calcularValorParaReceber(Long oficinaId);

  Pagamento buscarEntidadePorId(Long id);

  PagamentoResponseDTO atualizar(Long id, PagamentoUpdateRequestDTO request);

  PagamentoResponseDTO atualizarValorPago(Long id, BigDecimal valor);

  PagamentoResponseDTO estornarValorPago(Long id, BigDecimal valor);

  @Transactional
  void recalcularStatus(Long pagamentoId);

  void deletar(Long id);
}
