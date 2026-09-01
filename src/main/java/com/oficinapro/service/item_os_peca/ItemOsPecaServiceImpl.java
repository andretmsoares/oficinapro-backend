package com.oficinapro.service.item_os_peca;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemOsPecaServiceImpl implements ItemOsPecaService {

    private final ItemOsPecaRepository itemOsPecaRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final OrdemDeServicoService ordemDeServicoService;

    @Override
    @Transactional(readOnly = true)
    public List<ItemOsPecaResponseDTO> listarPorOrdemServico(Long osId) {

        ordemDeServicoService.buscarPorEntidadeId(osId);

        return itemOsPecaRepository.findByOrdemDeServicoId(osId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemOsPecaResponseDTO buscarPorId(Long id) {
        ItemOsPeca item = buscarPorEntidadeId(id);
        return toResponse(item);
    }

    private ItemOsPeca buscarPorEntidadeId(Long id) {
        ItemOsPeca item = itemOsPecaRepository.findById(id)
                .orElseThrow(() -> new ItemOsPecaNotFoundException(id));

        ordemDeServicoService.buscarPorEntidadeId(item.getOrdemDeServico().getId());

        return item;
    }

    @Override
    @Transactional
    public ItemOsPecaResponseDTO criar(ItemOsPecaRequestDTO request) {
        OrdemDeServico os = ordemDeServicoService.buscarPorEntidadeId(request.osId());

        validarOsEditavel(os);

        ItemOsPeca item = new ItemOsPeca();
        item.setOrdemDeServico(os);
        item.setNome(request.nome());
        item.setQuantidade(request.quantidade());
        item.setValorUnitario(request.valorUnitario());
        item.setValorTotal(calcularValorTotal(request.quantidade(), request.valorUnitario()));

        item = itemOsPecaRepository.save(item);

        recalcularValorTotalOS(os);

        return toResponse(item);
    }

    @Override
    @Transactional
    public ItemOsPecaResponseDTO atualizar(Long id, ItemOsPecaUpdateRequestDTO request) {
        ItemOsPeca item = buscarPorEntidadeId(id); // já valida acesso

        OrdemDeServico os = item.getOrdemDeServico();
        validarOsEditavel(os);

        item.setNome(request.nome());
        item.setQuantidade(request.quantidade());
        item.setValorUnitario(request.valorUnitario());
        item.setValorTotal(calcularValorTotal(request.quantidade(), request.valorUnitario()));

        item = itemOsPecaRepository.save(item);

        recalcularValorTotalOS(os);

        return toResponse(item);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        ItemOsPeca item = buscarPorEntidadeId(id); // já valida acesso

        OrdemDeServico os = item.getOrdemDeServico();
        validarOsEditavel(os);

        itemOsPecaRepository.delete(item);

        recalcularValorTotalOS(os);
    }

    private void validarOsEditavel(OrdemDeServico os) {
        if (os.getStatus() == StatusOrdemDeServico.CANCELADA) {
            throw new OSCanceledException();
        }
        if (os.getStatus() == StatusOrdemDeServico.ENTREGUE) {
            throw new OSFinishedException();
        }
    }

    private BigDecimal calcularValorTotal(BigDecimal quantidade, BigDecimal valorUnitario) {
        return quantidade.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalcularValorTotalOS(OrdemDeServico os) {
        BigDecimal total = itemOsPecaRepository.findByOrdemDeServicoId(os.getId()).stream()
                .map(ItemOsPeca::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        os.setValorTotal(total);
        ordemDeServicoRepository.save(os);
    }

    private ItemOsPecaResponseDTO toResponse(ItemOsPeca item) {
        return new ItemOsPecaResponseDTO(
                item.getId(),
                item.getOrdemDeServico().getId(),
                item.getNome(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.getValorTotal()
        );
    }
}