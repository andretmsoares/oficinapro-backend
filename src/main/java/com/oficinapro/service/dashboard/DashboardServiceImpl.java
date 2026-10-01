package com.oficinapro.service.dashboard;

import com.oficinapro.dto.dashboard.DashboardResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.veiculo.VeiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

  private final VeiculoService veiculoService;
  private final ClienteService clienteService;
  private final PagamentoService pagamentoService;
  private final OrdemDeServicoService ordemDeServicoService;
  private final OficinaAccessValidator oficinaAccessValidator;

  @Override
  public DashboardResponseDTO getData() {
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return new DashboardResponseDTO(
        ordemDeServicoService.listarPorStatus(StatusOrdemDeServico.ABERTA).size(),
        veiculoService.count(),
        clienteService.count(),
        pagamentoService.calcularValorParaReceber(oficinaId),
        (Integer)
            pagamentoService.buscarPorStatus(oficinaId, StatusPagamento.PAGAMENTO_PENDENTE).size());
  }
}
