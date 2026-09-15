package com.oficinapro.service.item_os_peca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import com.oficinapro.service.ordem_servico.OrdemDeServicoValorRecalculator;
import com.oficinapro.service.pagamento.PagamentoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Peças lançadas em uma Ordem de Serviço.
 *
 * <p>O que mudou no refactor e está travado aqui: o service não recalcula mais o total da OS por
 * conta própria — ele delega ao {@link OrdemDeServicoValorRecalculator}, que soma peças e mão de
 * obra. E a exclusão passou a recusar deixar a OS valer menos do que já foi pago.
 */
@ExtendWith(MockitoExtension.class)
class ItemOsPecaServiceTest {

  private static final Long OS_ID = 100L;
  private static final Long ITEM_ID = 9L;

  @Mock private ItemOsPecaRepository itemOsPecaRepository;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OrdemDeServicoValorRecalculator valorRecalculator;

  @InjectMocks private ItemOsPecaServiceImpl service;

  private OrdemDeServico os(StatusOrdemDeServico status, String valorComDesconto) {
    Oficina oficina = new Oficina();
    oficina.setId(1L);

    OrdemDeServico os = new OrdemDeServico();
    os.setId(OS_ID);
    os.setOficina(oficina);
    os.setStatus(status);
    os.setValorTotal(new BigDecimal(valorComDesconto));
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(new BigDecimal(valorComDesconto));
    return os;
  }

  private ItemOsPeca item(OrdemDeServico os, String quantidade, String valorUnitario) {
    ItemOsPeca item = new ItemOsPeca();
    item.setId(ITEM_ID);
    item.setOrdemDeServico(os);
    item.setNome("Pastilha de freio");
    item.setQuantidade(new BigDecimal(quantidade));
    item.setValorUnitario(new BigDecimal(valorUnitario));
    item.setValorTotal(new BigDecimal(quantidade).multiply(new BigDecimal(valorUnitario)));
    return item;
  }

  private PagamentoResponseDTO pagamentoComValorPago(String valorPago) {
    return new PagamentoResponseDTO(
        50L,
        OS_ID,
        new BigDecimal(valorPago),
        "",
        LocalDateTime.now(),
        StatusPagamento.PAGO_PARCIALMENTE);
  }

  @Nested
  @DisplayName("criar")
  class Criar {

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({
      "2,     120.00, 240.00",
      "1,     35.50,  35.50",
      "0.500, 100.00, 50.00",
      "3,     33.333, 100.00"
    })
    @DisplayName("deve calcular valorTotal = quantidade x valorUnitario com 2 casas decimais")
    void deveCalcularValorTotalDoItem(
        String quantidade, String valorUnitario, String valorTotalEsperado) {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(itemOsPecaRepository.save(any(ItemOsPeca.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      ItemOsPecaResponseDTO resposta =
          service.criar(
              new ItemOsPecaRequestDTO(
                  OS_ID, "Peça", new BigDecimal(quantidade), new BigDecimal(valorUnitario)));

      assertThat(resposta.valorTotal()).isEqualByComparingTo(valorTotalEsperado);
    }

    @Test
    @DisplayName("deve persistir o item e então delegar o recálculo do total da OS")
    void devePersistirEDelegarRecalculo() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(itemOsPecaRepository.save(any(ItemOsPeca.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      service.criar(
          new ItemOsPecaRequestDTO(OS_ID, "Peça", new BigDecimal("2"), new BigDecimal("50.00")));

      InOrder ordem = inOrder(itemOsPecaRepository, valorRecalculator);
      ordem.verify(itemOsPecaRepository).save(any(ItemOsPeca.class));
      ordem.verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve lançar peça em OS cancelada")
    void naoDeveLancarPecaEmOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSCanceledException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(
              () ->
                  service.criar(
                      new ItemOsPecaRequestDTO(
                          OS_ID, "Peça", BigDecimal.ONE, new BigDecimal("10.00"))))
          .isInstanceOf(OSCanceledException.class);

      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve lançar peça em OS fechada")
    void naoDeveLancarPecaEmOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSFinishedException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(
              () ->
                  service.criar(
                      new ItemOsPecaRequestDTO(
                          OS_ID, "Peça", BigDecimal.ONE, new BigDecimal("10.00"))))
          .isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve propagar 'OS não encontrada' quando a OS não existe ou é de outra oficina")
    void devePropagarOsInexistente() {
      when(ordemDeServicoService.buscarPorEntidadeId(999L))
          .thenThrow(new OrdemDeServicoNotFoundException(999L));

      assertThatThrownBy(
              () ->
                  service.criar(
                      new ItemOsPecaRequestDTO(
                          999L, "Peça", BigDecimal.ONE, new BigDecimal("10.00"))))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName("deve recalcular o valorTotal do item ao alterar quantidade e preço")
    void deveRecalcularValorTotalDoItem() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(os, "2", "120.00")));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(itemOsPecaRepository.save(any(ItemOsPeca.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      ItemOsPecaResponseDTO resposta =
          service.atualizar(
              ITEM_ID,
              new ItemOsPecaUpdateRequestDTO(
                  "Pastilha premium", new BigDecimal("4"), new BigDecimal("150.00")));

      assertThat(resposta.nome()).isEqualTo("Pastilha premium");
      assertThat(resposta.valorTotal()).isEqualByComparingTo("600.00");
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("deve lançar ItemOsPecaNotFoundException ao atualizar item inexistente")
    void deveLancarAoAtualizarItemInexistente() {
      when(itemOsPecaRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.atualizar(
                      404L,
                      new ItemOsPecaUpdateRequestDTO("x", BigDecimal.ONE, new BigDecimal("1.00"))))
          .isInstanceOf(ItemOsPecaNotFoundException.class);
    }

    @Test
    @DisplayName("não deve atualizar peça de OS cancelada")
    void naoDeveAtualizarPecaDeOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "240.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(os, "2", "120.00")));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSCanceledException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(
              () ->
                  service.atualizar(
                      ITEM_ID,
                      new ItemOsPecaUpdateRequestDTO("x", BigDecimal.ONE, new BigDecimal("1.00"))))
          .isInstanceOf(OSCanceledException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("deletar")
  class Deletar {

    @Test
    @DisplayName("deve excluir a peça e recalcular o total da OS")
    void deveExcluirPecaERecalcular() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoComValorPago("0.00"));

      service.deletar(ITEM_ID);

      verify(itemOsPecaRepository).delete(existente);
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve excluir peça se a OS passaria a valer menos do que já foi pago")
    void naoDeveExcluirSeOsFicariaAbaixoDoValorPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      // Restante seria 0,00 mas o cliente já pagou 100,00.
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoComValorPago("100.00"));

      assertThatThrownBy(() -> service.deletar(ITEM_ID))
          .isInstanceOf(PagamentoValorExcedidoException.class);

      verify(itemOsPecaRepository, never()).delete(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("deve excluir quando o valor restante fica exatamente igual ao já pago")
    void deveExcluirQuandoRestanteIgualAoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "1", "40.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoComValorPago("200.00"));

      service.deletar(ITEM_ID);

      verify(itemOsPecaRepository).delete(existente);
    }

    @Test
    @DisplayName("não deve excluir peça de OS fechada")
    void naoDeveExcluirPecaDeOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSFinishedException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(() -> service.deletar(ITEM_ID)).isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("consultas e isolamento por oficina")
  class Consultas {

    @Test
    @DisplayName("deve validar o acesso à OS antes de listar as peças")
    void deveValidarAcessoAntesDeListar() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(itemOsPecaRepository.findByOrdemDeServicoId(OS_ID))
          .thenReturn(List.of(item(os, "2", "120.00")));

      List<ItemOsPecaResponseDTO> resultado = service.listarPorOrdemServico(OS_ID);

      assertThat(resultado).hasSize(1);
      assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo("240.00");
      verify(ordemDeServicoService).buscarPorEntidadeId(OS_ID);
    }

    @Test
    @DisplayName("não deve listar peças de OS de outra oficina")
    void naoDeveListarPecasDeOutraOficina() {
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.listarPorOrdemServico(OS_ID))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(itemOsPecaRepository, never()).findByOrdemDeServicoId(any());
    }

    @Test
    @DisplayName("deve revalidar o acesso à OS ao buscar uma peça por id")
    void deveRevalidarAcessoAoBuscarPorId() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      when(itemOsPecaRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(os, "2", "120.00")));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);

      ItemOsPecaResponseDTO resposta = service.buscarPorId(ITEM_ID);

      assertThat(resposta.id()).isEqualTo(ITEM_ID);
      verify(ordemDeServicoService).buscarPorEntidadeId(OS_ID);
    }

    @Test
    @DisplayName("deve lançar ItemOsPecaNotFoundException ao buscar peça inexistente")
    void deveLancarAoBuscarPecaInexistente() {
      when(itemOsPecaRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.buscarPorId(404L))
          .isInstanceOf(ItemOsPecaNotFoundException.class);
    }
  }
}
