package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoResumoDTO;
import com.oficinapro.dto.pagamento.PagamentoUpdateRequestDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.*;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.RegistroPagamento;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.repository.PagamentoRepository;
import com.oficinapro.repository.RegistroPagamentoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.util.Paginacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagamentoServiceImpl implements PagamentoService {

  private final PagamentoRepository repository;
  // Repository no lugar de OrdemDeServicoService: é isso que elimina o ciclo
  // Pagamento -> OS -> PDF -> Pagamento.
  private final OrdemDeServicoRepository ordemDeServicoRepository;
  private final OficinaAccessValidator oficinaAccessValidator;
  private final RegistroPagamentoRepository registroPagamentoRepository;

  @Override
  @Transactional
  public PagamentoResponseDTO criar(PagamentoRequestDTO request) {

    oficinaAccessValidator.validarRole(Role.GERENTE);

    OrdemDeServico os = buscarOrdemDeServico(request.osId());

    if (repository.findByOrdemDeServicoId(os.getId()) != null) {
      throw new PagamentoAlreadyExistsException();
    }

    Pagamento pagamento = new Pagamento();
    pagamento.setOrdemDeServico(os);
    pagamento.setValorPago(BigDecimal.ZERO);
    pagamento.setObs(request.obs());
    pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);

    return toResponseDTO(repository.save(pagamento));
  }

  @Override
  @Transactional
  public PagamentoResponseDTO atualizar(Long id, PagamentoUpdateRequestDTO request) {

    oficinaAccessValidator.validarRole(Role.GERENTE);

    Pagamento pagamento = buscarEntidadePorId(id);

    pagamento.setObs(request.obs());

    return toResponseDTO(repository.save(pagamento));
  }

  @Override
  @Transactional(readOnly = true)
  public PagamentoResponseDTO buscarPorId(Long id) {
    return toResponseDTO(buscarEntidadePorId(id));
  }

  @Override
  @Transactional(readOnly = true)
  public PagamentoResponseDTO buscarPorOsId(Long osId) {
    Pagamento pagamento = this.buscarPorEntidadeOsId(osId);

    oficinaAccessValidator.validarAcessoAoRegistro(
        pagamento.getOrdemDeServico().getOficina().getId(),
        new PagamentoNotFoundForThisOsException(osId));

    return toResponseDTO(pagamento);
  }

  private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("id", "status", "valorPago");

  private static final Sort ORDENACAO_PADRAO = Sort.by(Sort.Direction.DESC, "id");

  @Override
  @Transactional(readOnly = true)
  public Page<PagamentoResponseDTO> buscarPorOficina(
      Long oficinaId, String termo, StatusPagamento status, Pageable pageable) {

    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    String termoLimpo = Paginacao.termoOuVazio(termo);

    return repository
        .buscar(
            oficinaId,
            status == null ? "" : status.name(),
            termoLimpo,
            Paginacao.termoLike(termoLimpo),
            Paginacao.segura(pageable, CAMPOS_ORDENAVEIS, ORDENACAO_PADRAO))
        .map(this::toResponseDTO);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PagamentoResponseDTO> buscarPorStatus(
      Long oficinaId, StatusPagamento status, Pageable pageable) {
    return buscarPorOficina(oficinaId, "", status, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public long contarPorStatus(Long oficinaId, StatusPagamento status) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    return repository.countByOrdemDeServicoOficinaIdAndStatus(oficinaId, status);
  }

  @Override
  @Transactional(readOnly = true)
  public PagamentoResumoDTO resumo(Long oficinaId) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    return new PagamentoResumoDTO(
        repository.somarRecebido(oficinaId),
        repository.calcularValorParaReceber(
            oficinaId,
            List.of(StatusPagamento.PAGAMENTO_PENDENTE, StatusPagamento.PAGO_PARCIALMENTE)),
        repository.contarComSaldo(oficinaId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PagamentoResponseDTO> buscarPorOsIds(Long oficinaId, Collection<Long> osIds) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    if (osIds == null || osIds.isEmpty()) {
      return List.of();
    }

    return repository.findByOrdemDeServicoIdInAndOrdemDeServicoOficinaId(osIds, oficinaId).stream()
        .map(this::toResponseDTO)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal calcularValorParaReceber(Long oficinaId) {

    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    return repository.calcularValorParaReceber(
        oficinaId, List.of(StatusPagamento.PAGAMENTO_PENDENTE, StatusPagamento.PAGO_PARCIALMENTE));
  }

  @Override
  @Transactional(readOnly = true)
  public Pagamento buscarEntidadePorId(Long id) {

    Pagamento pagamento =
        repository.findById(id).orElseThrow(() -> new PagamentoNotFoundException(id));

    Long oficinaDoPagamento = pagamento.getOrdemDeServico().getOficina().getId();

    oficinaAccessValidator.validarAcessoAoRegistro(
        oficinaDoPagamento, new PagamentoNotFoundException(id));

    return pagamento;
  }

  @Override
  @Transactional
  public PagamentoResponseDTO atualizarValorPago(Long id, BigDecimal valor) {
    oficinaAccessValidator.validarRole(Role.GERENTE);
    return ajustarValorPago(id, valor);
  }

  @Transactional
  @Override
  public PagamentoResponseDTO estornarValorPago(Long id, BigDecimal valor) {
    oficinaAccessValidator.validarRole(Role.GERENTE);
    return ajustarValorPago(id, valor.negate());
  }

  @Transactional
  @Override
  public void recalcularStatus(Long osId) {
    // Chamado quando o valor da OS muda: trava o pagamento por versão (ver o repository).
    Pagamento pagamento =
        repository
            .findByOrdemDeServicoIdParaAlterarValor(osId)
            .orElseThrow(() -> new PagamentoNotFoundForThisOsException(osId));

    BigDecimal valorPago = pagamento.getValorPago();
    BigDecimal valorOS = pagamento.getOrdemDeServico().getValorComDesconto();

    if (valorPago.compareTo(valorOS) > 0) {
      throw new PagamentoValorExcedidoException(valorPago, valorOS);
    }

    atualizarStatusEData(pagamento, valorOS);

    repository.save(pagamento);
  }

  @Override
  @Transactional
  public void deletar(Long id) {
    Pagamento pagamento = buscarEntidadePorId(id);
    List<RegistroPagamento> registros = registroPagamentoRepository.findByPagamentoId(id);
    for (RegistroPagamento r : registros) {
      registroPagamentoRepository.delete(r);
    }
    repository.delete(pagamento);
  }

  /**
   * Núcleo compartilhado entre "receber pagamento" (delta positivo) e "estornar pagamento" (delta
   * negativo, usado ao excluir um RegistroPagamento). Recalcula o status do pagamento em qualquer
   * direção.
   */
  private PagamentoResponseDTO ajustarValorPago(Long id, BigDecimal delta) {
    Pagamento pagamento = this.buscarEntidadePorId(id);

    BigDecimal novoValor = pagamento.getValorPago().add(delta);

    if (novoValor.compareTo(BigDecimal.ZERO) < 0) {
      throw new PagamentoValorInvalidoException(
          "O estorno excede o valor já pago para este pagamento");
    }

    OrdemDeServico os = pagamento.getOrdemDeServico();

    if (novoValor.compareTo(os.getValorComDesconto()) > 0) {
      throw new PagamentoValorExcedidoException(novoValor, os.getValorComDesconto());
    }

    pagamento.setValorPago(novoValor);
    atualizarStatusEData(pagamento, os.getValorComDesconto());

    return toResponseDTO(repository.save(pagamento));
  }

  /**
   * Única regra de status: pendente sem nada pago, parcial abaixo do valor da OS, paga ao igualar.
   * A data de quitação só existe enquanto o pagamento está PAGA. OS zerada sem pagamento permanece
   * pendente de propósito.
   */
  private void atualizarStatusEData(Pagamento pagamento, BigDecimal valorOS) {
    BigDecimal valorPago = pagamento.getValorPago();

    if (valorPago.compareTo(BigDecimal.ZERO) == 0) {
      pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);
      pagamento.setDataPagamentoTotal(null);
    } else if (valorPago.compareTo(valorOS) == 0) {
      pagamento.setStatus(StatusPagamento.PAGA);
      pagamento.setDataPagamentoTotal(LocalDateTime.now());
    } else {
      pagamento.setStatus(StatusPagamento.PAGO_PARCIALMENTE);
      pagamento.setDataPagamentoTotal(null);
    }
  }

  /** Mesma regra de {@code OrdemDeServicoServiceImpl#buscarPorEntidadeId}, sem depender dele. */
  private OrdemDeServico buscarOrdemDeServico(Long osId) {
    OrdemDeServico os =
        ordemDeServicoRepository
            .findById(osId)
            .orElseThrow(() -> new OrdemDeServicoNotFoundException(osId));

    oficinaAccessValidator.validarAcessoAoRegistro(
        os.getOficina() != null ? os.getOficina().getId() : null,
        new OrdemDeServicoNotFoundException(osId));

    return os;
  }

  private PagamentoResponseDTO toResponseDTO(Pagamento pagamento) {
    return new PagamentoResponseDTO(
        pagamento.getId(),
        pagamento.getOrdemDeServico().getId(),
        pagamento.getOrdemDeServico().getValorComDesconto(),
        pagamento.getValorPago(),
        pagamento.getOrdemDeServico().getValorComDesconto().subtract(pagamento.getValorPago()),
        pagamento.getObs(),
        pagamento.getDataPagamentoTotal(),
        pagamento.getStatus());
  }

  private Pagamento buscarPorEntidadeOsId(Long osId) {

    Pagamento pagamento = repository.findByOrdemDeServicoId(osId);

    if (pagamento == null) {
      throw new PagamentoNotFoundForThisOsException(osId);
    }

    return pagamento;
  }
}
