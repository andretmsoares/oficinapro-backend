package com.oficinapro.service.ordem_servico;

import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.MaoObra;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.repository.MaoObraRepository;
import com.oficinapro.service.pagamento.PagamentoService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Única fonte de verdade para o valor total da OS = soma(ItemOsPeca) + soma(MaoObra). Usado por
 * ItemOsPecaServiceImpl e MaoObraServiceImpl para evitar que um dos dois sobrescreva o total
 * ignorando o outro.
 */
@Component
@RequiredArgsConstructor
public class OrdemDeServicoValorRecalculator {

  private final ItemOsPecaRepository itemOsPecaRepository;
  private final MaoObraRepository maoObraRepository;
  private final OrdemDeServicoService ordemDeServicoService;
  private final PagamentoService pagamentoService;

  public void recalcular(OrdemDeServico os) {
    BigDecimal totalPecas =
        itemOsPecaRepository
            .findByOrdemDeServicoIdAndOficinaId(os.getId(), os.getOficina().getId())
            .stream()
            .map(ItemOsPeca::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalMaoObra =
        maoObraRepository.findByOrdemDeServicoId(os.getId()).stream()
            .map(MaoObra::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    ordemDeServicoService.recalcularValorTotal(os.getId(), totalPecas.add(totalMaoObra));
    pagamentoService.recalcularStatus(os.getId());
  }

  public void validarOsEditavel(OrdemDeServico os) {
    if (os.getStatus() == StatusOrdemDeServico.CANCELADA) {
      throw new OSCanceledException();
    }
    if (os.getStatus() == StatusOrdemDeServico.FECHADA) {
      throw new OSFinishedException();
    }
  }
}
