package com.oficinapro.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.dashboard.DashboardResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.veiculo.VeiculoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

  @Mock private VeiculoService veiculoService;
  @Mock private ClienteService clienteService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private DashboardServiceImpl dashboardService;

  @Test
  @DisplayName("deve agregar os indicadores usando a oficina do usuário logado")
  void deveAgregarIndicadoresDaOficinaLogada() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(7L);
    when(ordemDeServicoService.contarPorStatus(StatusOrdemDeServico.ABERTA)).thenReturn(2L);
    when(veiculoService.count()).thenReturn(10);
    when(clienteService.count()).thenReturn(4);
    when(pagamentoService.calcularValorParaReceber(7L)).thenReturn(new BigDecimal("350.50"));
    when(pagamentoService.contarPorStatus(7L, StatusPagamento.PAGAMENTO_PENDENTE)).thenReturn(3L);

    DashboardResponseDTO resultado = dashboardService.getData();

    assertThat(resultado.ordensAbertas()).isEqualTo(2);
    assertThat(resultado.veiculosCadastrados()).isEqualTo(10);
    assertThat(resultado.clientesCadastrados()).isEqualTo(4);
    assertThat(resultado.aReceber()).isEqualByComparingTo("350.50");
    assertThat(resultado.pagamentosPendentes()).isEqualTo(3);
  }

  @Test
  @DisplayName("sem nada cadastrado, os contadores devem ser zero")
  void deveRetornarZerosQuandoNaoHaDados() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(7L);
    when(ordemDeServicoService.contarPorStatus(StatusOrdemDeServico.ABERTA)).thenReturn(0L);
    when(veiculoService.count()).thenReturn(0);
    when(clienteService.count()).thenReturn(0);
    when(pagamentoService.calcularValorParaReceber(7L)).thenReturn(BigDecimal.ZERO);
    when(pagamentoService.contarPorStatus(7L, StatusPagamento.PAGAMENTO_PENDENTE)).thenReturn(0L);

    DashboardResponseDTO resultado = dashboardService.getData();

    assertThat(resultado.ordensAbertas()).isZero();
    assertThat(resultado.veiculosCadastrados()).isZero();
    assertThat(resultado.clientesCadastrados()).isZero();
    assertThat(resultado.aReceber()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(resultado.pagamentosPendentes()).isZero();
  }

  @Test
  @DisplayName("usuário sem oficina deve propagar o acesso negado sem consultar os serviços")
  void devePropagarAcessoNegadoParaUsuarioSemOficina() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado())
        .thenThrow(new UsuarioAcessDeniedException());

    assertThatThrownBy(() -> dashboardService.getData())
        .isInstanceOf(UsuarioAcessDeniedException.class);

    verifyNoInteractions(ordemDeServicoService, veiculoService, clienteService, pagamentoService);
  }
}
