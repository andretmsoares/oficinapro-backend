package com.oficinapro.service.ordem_servico;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoValorRecalculatorTest {

  private static final Long OS_ID = 100L;

  @Mock private ItemOsPecaRepository itemOsPecaRepository;
  @Mock private MaoObraRepository maoObraRepository;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private PagamentoService pagamentoService;

  @InjectMocks private OrdemDeServicoValorRecalculator recalculator;

  private OrdemDeServico os(StatusOrdemDeServico status) {
    OrdemDeServico os = new OrdemDeServico();
    os.setId(OS_ID);
    os.setStatus(status);
    return os;
  }

  private ItemOsPeca peca(String valorTotal) {
    ItemOsPeca item = new ItemOsPeca();
    item.setValorTotal(new BigDecimal(valorTotal));
    return item;
  }

  private MaoObra maoObra(String valor) {
    MaoObra maoObra = new MaoObra();
    maoObra.setValor(new BigDecimal(valor));
    return maoObra;
  }

  private BigDecimal totalRecalculado() {
    ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
    verify(ordemDeServicoService).recalcularValorTotal(eq(OS_ID), captor.capture());
    return captor.getValue();
  }

  // ------------------------------------------------------------------
  // recalcular
  // ------------------------------------------------------------------

  @Test
  @DisplayName("deve somar peças E mão de obra no valor total da OS")
  void deveSomarPecasEMaoDeObra() {
    OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO);
    when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID))
        .thenReturn(List.of(peca("120.00"), peca("35.50")));
    when(maoObraRepository.findByOrdemDeServicoId(OS_ID))
        .thenReturn(List.of(maoObra("200.00"), maoObra("44.50")));

    recalculator.recalcular(os);

    assertThatBigDecimal(totalRecalculado(), "400.00");
  }

  @Test
  @DisplayName("deve considerar apenas as peças quando não há mão de obra lançada")
  void deveConsiderarSomentePecasQuandoNaoHaMaoDeObra() {
    OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO);
    when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of(peca("99.90")));
    when(maoObraRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of());

    recalculator.recalcular(os);

    assertThatBigDecimal(totalRecalculado(), "99.90");
  }

  @Test
  @DisplayName("deve considerar apenas a mão de obra quando não há peças lançadas")
  void deveConsiderarSomenteMaoDeObraQuandoNaoHaPecas() {
    OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO);
    when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of());
    when(maoObraRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of(maoObra("150.00")));

    recalculator.recalcular(os);

    assertThatBigDecimal(totalRecalculado(), "150.00");
  }

  @Test
  @DisplayName("deve resultar em zero quando a OS não tem peças nem mão de obra")
  void deveResultarEmZeroQuandoOsEstaVazia() {
    OrdemDeServico os = os(StatusOrdemDeServico.ABERTA);
    when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of());
    when(maoObraRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of());

    recalculator.recalcular(os);

    assertThatBigDecimal(totalRecalculado(), "0");
  }

  @Test
  @DisplayName("deve recalcular o status do pagamento depois de atualizar o valor da OS")
  void deveRecalcularStatusDoPagamentoAposAtualizarValor() {
    OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO);
    when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of(peca("50.00")));
    when(maoObraRepository.findByOrdemDeServicoId(OS_ID)).thenReturn(List.of());

    recalculator.recalcular(os);

    InOrder ordem = inOrder(ordemDeServicoService, pagamentoService);
    ordem.verify(ordemDeServicoService).recalcularValorTotal(eq(OS_ID), any(BigDecimal.class));
    ordem.verify(pagamentoService).recalcularStatus(OS_ID);
  }

  // ------------------------------------------------------------------
  // validarOsEditavel
  // ------------------------------------------------------------------

  @Test
  @DisplayName("não deve permitir lançamentos em OS cancelada")
  void naoDevePermitirLancamentoEmOsCancelada() {
    assertThatThrownBy(() -> recalculator.validarOsEditavel(os(StatusOrdemDeServico.CANCELADA)))
        .isInstanceOf(OSCanceledException.class);
  }

  @Test
  @DisplayName("não deve permitir lançamentos em OS fechada")
  void naoDevePermitirLancamentoEmOsFechada() {
    assertThatThrownBy(() -> recalculator.validarOsEditavel(os(StatusOrdemDeServico.FECHADA)))
        .isInstanceOf(OSFinishedException.class);
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusOrdemDeServico.class,
      names = {"CANCELADA", "FECHADA"},
      mode = EnumSource.Mode.EXCLUDE)
  @DisplayName(
      "deve permitir lançamentos em todos os demais status, inclusive FINALIZADA e ENTREGUE")
  void devePermitirLancamentoNosDemaisStatus(StatusOrdemDeServico status) {
    assertThatCode(() -> recalculator.validarOsEditavel(os(status)))
        .as(
            "Regra atual: só CANCELADA e FECHADA travam lançamentos. FINALIZADA e"
                + " ENTREGUE ainda aceitam alteração de peças e mão de obra.")
        .doesNotThrowAnyException();
  }

  private static void assertThatBigDecimal(BigDecimal atual, String esperado) {
    org.assertj.core.api.Assertions.assertThat(atual)
        .as("valor total recalculado da OS")
        .isEqualByComparingTo(new BigDecimal(esperado));
  }
}
