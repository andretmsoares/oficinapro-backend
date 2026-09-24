package com.oficinapro.service.item_os_peca;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.exception.item_os_peca.ItemOsPecaJaVinculadoException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoValorRecalculator;
import com.oficinapro.service.pagamento.PagamentoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemOsPecaServiceImpl implements ItemOsPecaService {

  private final ItemOsPecaRepository itemOsPecaRepository;
  private final OrdemDeServicoService ordemDeServicoService;
  private final PagamentoService pagamentoService;
  private final OrdemDeServicoValorRecalculator valorRecalculator;
  private final OficinaAccessValidator oficinaAccessValidator;
  private final OficinaService oficinaService;

  @Override
  @Transactional(readOnly = true)
  public List<ItemOsPecaResponseDTO> listarPorOrdemServico(Long osId) {

    ordemDeServicoService.buscarPorEntidadeId(osId);

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return itemOsPecaRepository.findByOrdemDeServicoIdAndOficinaId(osId, oficinaId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ItemOsPecaResponseDTO buscarPorId(Long id) {
    return toResponse(buscarPorEntidadeId(id));
  }

  /**
   * Cria a peça. Se osId for informado, a peça já nasce vinculada e o total da OS é recalculado.
   */
  @Override
  @Transactional
  public ItemOsPecaResponseDTO criar(ItemOsPecaRequestDTO request) {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    // Valida a OS antes de qualquer escrita: se ela for inválida, nada é
    // persistido.
    OrdemDeServico os =
        request.osId() != null ? buscarOsEditavelDaOficina(request.osId(), oficinaId) : null;

    Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);

    ItemOsPeca item = new ItemOsPeca();
    item.setOficina(oficina);
    item.setOrdemDeServico(os);
    item.setNome(request.nome().toUpperCase());
    item.setQuantidade(request.quantidade());
    item.setValorUnitario(request.valorUnitario());
    item.setValorTotal(calcularValorTotal(request.quantidade(), request.valorUnitario()));

    item = itemOsPecaRepository.save(item);

    if (os != null) {
      valorRecalculator.recalcular(os);
    }

    return toResponse(item);
  }

  /** Atualiza apenas os dados da peça. O vínculo com a OS é gerenciado por vincular/desvincular. */
  @Override
  @Transactional
  public ItemOsPecaResponseDTO atualizar(Long id, ItemOsPecaUpdateRequestDTO request) {

    ItemOsPeca item = buscarPorEntidadeId(id);
    OrdemDeServico os = item.getOrdemDeServico();

    BigDecimal valorAnterior = item.getValorTotal();
    BigDecimal novoValor = calcularValorTotal(request.quantidade(), request.valorUnitario());

    if (os != null) {
      valorRecalculator.validarOsEditavel(os);

      // Só uma redução de valor pode deixar a OS abaixo do que já foi pago.
      if (novoValor.compareTo(valorAnterior) < 0) {
        validarQueOsNaoFiqueAbaixoDoPago(os, valorAnterior.subtract(novoValor));
      }
    }

    item.setNome(request.nome().toUpperCase());
    item.setQuantidade(request.quantidade());
    item.setValorUnitario(request.valorUnitario());
    item.setValorTotal(novoValor);

    item = itemOsPecaRepository.save(item);

    if (os != null) {
      valorRecalculator.recalcular(os);
    }

    return toResponse(item);
  }

  @Override
  @Transactional
  public void deletar(Long id) {

    ItemOsPeca item = buscarPorEntidadeId(id);
    OrdemDeServico os = item.getOrdemDeServico();

    if (os == null) {
      itemOsPecaRepository.delete(item);
      return;
    }

    valorRecalculator.validarOsEditavel(os);
    validarQueOsNaoFiqueAbaixoDoPago(os, item.getValorTotal());

    itemOsPecaRepository.delete(item);

    valorRecalculator.recalcular(os);
  }

  /** Vincula uma peça avulsa a uma OS. Peça já vinculada precisa ser desvinculada antes. */
  @Override
  @Transactional
  public ItemOsPecaResponseDTO vincularOs(Long id, Long osId) {

    ItemOsPeca item = buscarPorEntidadeId(id);

    if (item.getOrdemDeServico() != null) {
      throw new ItemOsPecaJaVinculadoException();
    }

    OrdemDeServico os = buscarOsEditavelDaOficina(osId, item.getOficina().getId());

    item.setOrdemDeServico(os);
    item = itemOsPecaRepository.save(item);

    valorRecalculator.recalcular(os);

    return toResponse(item);
  }

  /** Desvincula a peça da OS (ela continua existindo na oficina). Idempotente. */
  @Override
  @Transactional
  public ItemOsPecaResponseDTO desvincularOs(Long id) {

    ItemOsPeca item = buscarPorEntidadeId(id);
    OrdemDeServico os = item.getOrdemDeServico();

    if (os == null) {
      return toResponse(item);
    }

    valorRecalculator.validarOsEditavel(os);
    validarQueOsNaoFiqueAbaixoDoPago(os, item.getValorTotal());

    item.setOrdemDeServico(null);
    item = itemOsPecaRepository.save(item);

    valorRecalculator.recalcular(os);

    return toResponse(item);
  }

  // ------------------------------------------------------------------
  // helpers
  // ------------------------------------------------------------------

  private ItemOsPeca buscarPorEntidadeId(Long id) {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return itemOsPecaRepository
        .findByIdAndOficinaId(id, oficinaId)
        .orElseThrow(ItemOsPecaNotFoundException::new);
  }

  private OrdemDeServico buscarOsEditavelDaOficina(Long osId, Long oficinaId) {

    OrdemDeServico os = ordemDeServicoService.buscarPorEntidadeId(osId);

    // Defesa em profundidade: a OS precisa ser da mesma oficina da peça/usuário.
    if (!os.getOficina().getId().equals(oficinaId)) {
      throw new OrdemDeServicoNotFoundException(osId);
    }

    valorRecalculator.validarOsEditavel(os);

    return os;
  }

  private void validarQueOsNaoFiqueAbaixoDoPago(OrdemDeServico os, BigDecimal valorRemovido) {

    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

    BigDecimal valorRestante = os.getValorComDesconto().subtract(valorRemovido);

    if (valorRestante.compareTo(pagamento.valorPago()) < 0) {
      throw new PagamentoValorExcedidoException(valorRestante, os.getValorTotal());
    }
  }

  private BigDecimal calcularValorTotal(BigDecimal quantidade, BigDecimal valorUnitario) {
    return quantidade.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
  }

  private ItemOsPecaResponseDTO toResponse(ItemOsPeca item) {
    return new ItemOsPecaResponseDTO(
        item.getId(),
        item.getOrdemDeServico() != null ? item.getOrdemDeServico().getId() : null,
        item.getNome(),
        item.getQuantidade(),
        item.getValorUnitario(),
        item.getValorTotal());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemOsPecaResponseDTO> listar() {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return itemOsPecaRepository.findByOficinaId(oficinaId).stream().map(this::toResponse).toList();
  }
}
