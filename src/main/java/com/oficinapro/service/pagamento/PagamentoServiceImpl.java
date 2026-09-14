package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.pagamento.*;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.repository.PagamentoRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository repository;
    private final OrdemDeServicoService ordemDeServicoService;

    @Override
    @Transactional
    public PagamentoResponseDTO criar(PagamentoRequestDTO request) {
        OrdemDeServico os = ordemDeServicoService.buscarPorEntidadeId(request.osId());


        Pagamento pagamento = new Pagamento();
        pagamento.setOrdemDeServico(os);
        pagamento.setValorPago(BigDecimal.ZERO);
        pagamento.setObs(request.obs());
        pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);

        if (repository.findByOrdemDeServicoId(pagamento.getOrdemDeServico().getId()) != null) {
            throw new PagamentoAlreadyExistsException();
        }

        return toResponseDTO(repository.save(pagamento));
    }

    @Override
    @Transactional
    public PagamentoResponseDTO atualizar(Long id, PagamentoRequestDTO request) {
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
        return toResponseDTO(pagamento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> buscarPorOficina(Long oficinaId) {
        return repository
                .findByOrdemDeServicoOficinaId(oficinaId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> buscarPorStatus(
            Long oficinaId,
            StatusPagamento status
    ) {
        return repository
                .findByOrdemDeServicoOficinaIdAndStatus(oficinaId, status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularValorParaReceber(Long oficinaId) {
        return repository.calcularValorParaReceber(
                oficinaId,
                List.of(
                        StatusPagamento.PAGAMENTO_PENDENTE,
                        StatusPagamento.PAGO_PARCIALMENTE
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Pagamento buscarEntidadePorId(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new PagamentoNotFoundException(id));
    }

    @Override
    @Transactional
    public PagamentoResponseDTO atualizarValorPago(Long id, BigDecimal valor) {
        return ajustarValorPago(id, valor);
    }

    @Transactional
    @Override
    public PagamentoResponseDTO estornarValorPago(Long id, BigDecimal valor) {
        return ajustarValorPago(id, valor.negate());
    }

    @Transactional
    @Override
    public void recalcularStatus(Long osId) {
        Pagamento pagamento = this.buscarPorEntidadeOsId(osId);

        BigDecimal valorPago = pagamento.getValorPago();
        BigDecimal valorOS = pagamento.getOrdemDeServico().getValorComDesconto();

        int comparacao = valorPago.compareTo(valorOS);

        if (valorPago.compareTo(BigDecimal.ZERO) == 0) {
            pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);
            pagamento.setDataPagamentoTotal(null);

        } else if (comparacao == 0) {
            pagamento.setStatus(StatusPagamento.PAGA);
            pagamento.setDataPagamentoTotal(LocalDateTime.now());

        } else if (comparacao < 0) {
            pagamento.setStatus(StatusPagamento.PAGO_PARCIALMENTE);
            pagamento.setDataPagamentoTotal(null);

        } else {
            throw new PagamentoValorExcedidoException(
                    valorPago,
                    valorOS
            );
        }

        repository.save(pagamento);
    }

    /**
     * Núcleo compartilhado entre "receber pagamento" (delta positivo) e
     * "estornar pagamento" (delta negativo, usado ao excluir um RegistroPagamento).
     * Recalcula o status do pagamento em qualquer direção.
     */
    private PagamentoResponseDTO ajustarValorPago(Long id, BigDecimal delta) {
        Pagamento pagamento = this.buscarEntidadePorId(id);

        BigDecimal novoValor = pagamento.getValorPago().add(delta);

        if (novoValor.compareTo(BigDecimal.ZERO) < 0) {
            throw new PagamentoValorInvalidoException(
                    "O estorno excede o valor já pago para este pagamento");
        }

        OrdemDeServico os = pagamento.getOrdemDeServico();
        int comparacao = novoValor.compareTo(os.getValorComDesconto());

        if (comparacao > 0) {
            throw new PagamentoValorExcedidoException(novoValor, os.getValorComDesconto());
        } else if (comparacao == 0 && novoValor.compareTo(BigDecimal.ZERO) > 0) {
            pagamento.setStatus(StatusPagamento.PAGA);
        } else if (novoValor.compareTo(BigDecimal.ZERO) == 0) {
            pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);
        } else {
            pagamento.setStatus(StatusPagamento.PAGO_PARCIALMENTE);
        }

        pagamento.setValorPago(novoValor);

        return toResponseDTO(repository.save(pagamento));
    }

    private PagamentoResponseDTO toResponseDTO(Pagamento pagamento) {
        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getOrdemDeServico().getId(),
                pagamento.getValorPago(),
                pagamento.getObs(),
                pagamento.getDataPagamentoTotal(),
                pagamento.getStatus()
        );
    }

    public Pagamento buscarPorEntidadeOsId(Long osId) {
        Pagamento pagamento = repository.findByOrdemDeServicoId(osId);
        if (pagamento == null) {
            throw new PagamentoNotFoundForThisOsException(osId);
        }
        return pagamento;
    }
}