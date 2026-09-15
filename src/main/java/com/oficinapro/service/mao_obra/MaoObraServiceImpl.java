package com.oficinapro.service.mao_obra;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.mao_obra.MaoObraNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.model.MaoObra;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.MaoObraRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoValorRecalculator;
import com.oficinapro.service.pagamento.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaoObraServiceImpl implements MaoObraService {

    private final MaoObraRepository maoObraRepository;
    private final OrdemDeServicoService ordemDeServicoService;
    private final PagamentoService pagamentoService;
    private final OrdemDeServicoValorRecalculator valorRecalculator;

    @Override
    @Transactional(readOnly = true)
    public List<MaoObraResponseDTO> listarPorOrdemServico(Long osId) {
        ordemDeServicoService.buscarPorEntidadeId(osId);

        return maoObraRepository.findByOrdemDeServicoId(osId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaoObraResponseDTO buscarPorId(Long id) {
        return toResponse(buscarPorEntidadeId(id));
    }

    @Override
    @Transactional
    public MaoObraResponseDTO criar(MaoObraRequestDTO request) {
        OrdemDeServico os = ordemDeServicoService.buscarPorEntidadeId(request.osId());

        validarOsEditavel(os);

        MaoObra maoObra = new MaoObra();
        maoObra.setOrdemDeServico(os);
        maoObra.setValor(request.valor());
        maoObra.setDescricao(request.descricao());

        maoObra = maoObraRepository.save(maoObra);

        valorRecalculator.recalcular(os);

        return toResponse(maoObra);
    }

    @Override
    @Transactional
    public MaoObraResponseDTO atualizar(Long id, MaoObraRequestDTO request) {
        MaoObra maoObra = buscarPorEntidadeId(id); // já valida acesso

        OrdemDeServico os = maoObra.getOrdemDeServico();
        validarOsEditavel(os);

        // osId do request é ignorado propositalmente: não é permitido
        // mover uma mão de obra para outra OS via update.
        maoObra.setValor(request.valor());
        maoObra.setDescricao(request.descricao());

        maoObra = maoObraRepository.save(maoObra);

        valorRecalculator.recalcular(os);

        return toResponse(maoObra);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        MaoObra maoObra = buscarPorEntidadeId(id); // já valida acesso

        OrdemDeServico os = maoObra.getOrdemDeServico();
        validarOsEditavel(os);

        PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

        BigDecimal valorRestante = os.getValorComDesconto().subtract(maoObra.getValor());

        int comparacao = valorRestante.compareTo(pagamento.valorPago());

        if (comparacao < 0) {
            throw new PagamentoValorExcedidoException(valorRestante, os.getValorTotal());
        }

        maoObraRepository.delete(maoObra);

        valorRecalculator.recalcular(os);
    }

    private void validarOsEditavel(OrdemDeServico os) {
        if (os.getStatus() == StatusOrdemDeServico.CANCELADA) {
            throw new OSCanceledException();
        }
        if (os.getStatus() == StatusOrdemDeServico.ENTREGUE) {
            throw new OSFinishedException();
        }
    }

    private MaoObraResponseDTO toResponse(MaoObra maoObra) {
        return new MaoObraResponseDTO(
                maoObra.getId(),
                maoObra.getOrdemDeServico().getId(),
                maoObra.getValor(),
                maoObra.getDescricao()
        );
    }

    private MaoObra buscarPorEntidadeId(Long id) {
        MaoObra maoObra = maoObraRepository.findById(id)
                .orElseThrow(() -> new MaoObraNotFoundException(id));

        ordemDeServicoService.buscarPorEntidadeId(maoObra.getOrdemDeServico().getId());

        return maoObra;
    }
}