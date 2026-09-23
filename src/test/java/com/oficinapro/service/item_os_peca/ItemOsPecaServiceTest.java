package com.oficinapro.service.item_os_peca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.item_os_peca.ItemOsPecaJaVinculadoException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do serviço de peças.
 *
 * <p>Regras cobertas:
 *
 * <ul>
 *   <li>A peça pertence a uma oficina (oficina_id) e pode existir sem OS.
 *   <li>O vínculo com a OS é feito apenas por vincularOs/desvincularOs (criar aceita osId opcional;
 *       atualizar nunca mexe no vínculo).
 *   <li>Toda mudança que afeta o total da OS valida se a OS é editável, se o total não fica abaixo
 *       do valor já pago e dispara o recálculo.
 *   <li>Todo acesso é isolado pela oficina do usuário autenticado.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ItemOsPecaServiceTest {

  private static final Long OFICINA_ID = 1L;
  private static final Long OUTRA_OFICINA_ID = 2L;
  private static final Long OS_ID = 100L;
  private static final Long ITEM_ID = 9L;

  @Mock private ItemOsPecaRepository itemOsPecaRepository;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OrdemDeServicoValorRecalculator valorRecalculator;
  @Mock private OficinaAccessValidator oficinaAccessValidator;
  @Mock private OficinaService oficinaService;

  @InjectMocks private ItemOsPecaServiceImpl service;

  @BeforeEach
  void setUp() {
    // lenient: alguns fluxos falham (ex.: OS inexistente) antes de consultar a oficina.
    lenient().when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(OFICINA_ID);
  }

  // ------------------------------------------------------------------
  // fixtures
  // ------------------------------------------------------------------

  private Oficina oficina(Long id) {
    Oficina oficina = new Oficina();
    oficina.setId(id);
    return oficina;
  }

  private OrdemDeServico os(StatusOrdemDeServico status, String valorComDesconto) {
    OrdemDeServico os = new OrdemDeServico();
    os.setId(OS_ID);
    os.setOficina(oficina(OFICINA_ID));
    os.setStatus(status);
    os.setValorTotal(new BigDecimal(valorComDesconto));
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(new BigDecimal(valorComDesconto));
    return os;
  }

  private ItemOsPeca item(OrdemDeServico os, String quantidade, String valorUnitario) {
    ItemOsPeca item = new ItemOsPeca();
    item.setId(ITEM_ID);
    item.setOficina(oficina(OFICINA_ID));
    item.setOrdemDeServico(os);
    item.setNome("Pastilha de freio");
    item.setQuantidade(new BigDecimal(quantidade));
    item.setValorUnitario(new BigDecimal(valorUnitario));
    item.setValorTotal(
        new BigDecimal(quantidade)
            .multiply(new BigDecimal(valorUnitario))
            .setScale(2, RoundingMode.HALF_UP));
    return item;
  }

  private ItemOsPeca itemSemOs(String quantidade, String valorUnitario) {
    return item(null, quantidade, valorUnitario);
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

  private ItemOsPecaUpdateRequestDTO update(String nome, String quantidade, String valorUnitario) {
    return new ItemOsPecaUpdateRequestDTO(
        nome, new BigDecimal(quantidade), new BigDecimal(valorUnitario));
  }

  // ------------------------------------------------------------------
  // stubs
  // ------------------------------------------------------------------

  private void itemEncontrado(ItemOsPeca item) {
    when(itemOsPecaRepository.findByIdAndOficinaId(ITEM_ID, OFICINA_ID))
        .thenReturn(Optional.of(item));
  }

  private void itemNaoEncontrado(Long id) {
    when(itemOsPecaRepository.findByIdAndOficinaId(id, OFICINA_ID)).thenReturn(Optional.empty());
  }

  private void osEncontrada(OrdemDeServico os) {
    when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
  }

  private void oficinaDoUsuarioExiste() {
    when(oficinaService.buscarPorEntidadeId(OFICINA_ID)).thenReturn(oficina(OFICINA_ID));
  }

  private void valorPagoDaOs(String valorPago) {
    when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoComValorPago(valorPago));
  }

  /** Simula o banco: devolve a própria entidade e atribui id se ainda não tiver. */
  private void salvarAtribuindoId() {
    when(itemOsPecaRepository.save(any(ItemOsPeca.class)))
        .thenAnswer(
            invocation -> {
              ItemOsPeca salvo = invocation.getArgument(0);
              if (salvo.getId() == null) {
                salvo.setId(ITEM_ID);
              }
              return salvo;
            });
  }

  private void osNaoEditavel(OrdemDeServico os, RuntimeException excecao) {
    doThrow(excecao).when(valorRecalculator).validarOsEditavel(os);
  }

  private ItemOsPeca itemSalvo() {
    ArgumentCaptor<ItemOsPeca> captor = ArgumentCaptor.forClass(ItemOsPeca.class);
    verify(itemOsPecaRepository).save(captor.capture());
    return captor.getValue();
  }

  // ==================================================================
  // criar
  // ==================================================================

  @Nested
  @DisplayName("criar")
  class Criar {

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({
      "2, 120.00, 240.00",
      "1, 35.50, 35.50",
      "0.500, 100.00, 50.00",
      "3, 33.333, 100.00"
    })
    @DisplayName("deve calcular valorTotal = quantidade x valorUnitario com 2 casas decimais")
    void deveCalcularValorTotal(String quantidade, String valorUnitario, String esperado) {
      oficinaDoUsuarioExiste();
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.criar(
              new ItemOsPecaRequestDTO(
                  null, "Peça", new BigDecimal(quantidade), new BigDecimal(valorUnitario)));

      assertThat(resposta.valorTotal()).isEqualByComparingTo(esperado);
      assertThat(itemSalvo().getValorTotal().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve criar peça sem OS associada à oficina do usuário, sem recalcular nada")
    void deveCriarPecaSemOs() {
      oficinaDoUsuarioExiste();
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.criar(
              new ItemOsPecaRequestDTO(
                  null, "Pastilha de freio", new BigDecimal("2"), new BigDecimal("120.00")));

      assertThat(resposta.id()).isEqualTo(ITEM_ID);
      assertThat(resposta.osId()).isNull();
      assertThat(resposta.nome()).isEqualTo("Pastilha de freio");
      assertThat(resposta.valorTotal()).isEqualByComparingTo("240.00");

      ItemOsPeca salvo = itemSalvo();
      assertThat(salvo.getOficina().getId()).isEqualTo(OFICINA_ID);
      assertThat(salvo.getOrdemDeServico()).isNull();

      verifyNoInteractions(ordemDeServicoService, pagamentoService, valorRecalculator);
    }

    @Test
    @DisplayName(
        "deve criar peça já vinculada à OS: valida, persiste e recalcula o total, nessa ordem")
    void deveCriarPecaVinculadaEmOrdem() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      osEncontrada(os);
      oficinaDoUsuarioExiste();
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.criar(
              new ItemOsPecaRequestDTO(
                  OS_ID, "Peça", new BigDecimal("2"), new BigDecimal("50.00")));

      assertThat(resposta.osId()).isEqualTo(OS_ID);
      assertThat(resposta.valorTotal()).isEqualByComparingTo("100.00");

      InOrder ordem = inOrder(valorRecalculator, itemOsPecaRepository);
      ordem.verify(valorRecalculator).validarOsEditavel(os);
      ordem.verify(itemOsPecaRepository).save(any(ItemOsPeca.class));
      ordem.verify(valorRecalculator).recalcular(os);

      ItemOsPeca salvo = itemSalvo();
      assertThat(salvo.getOrdemDeServico()).isSameAs(os);
      assertThat(salvo.getOficina().getId()).isEqualTo(OFICINA_ID);
    }

    @Test
    @DisplayName("deve persistir a peça uma única vez")
    void devePersistirUmaUnicaVez() {
      osEncontrada(os(StatusOrdemDeServico.EM_EXECUCAO, "0.00"));
      oficinaDoUsuarioExiste();
      salvarAtribuindoId();

      service.criar(
          new ItemOsPecaRequestDTO(OS_ID, "Peça", BigDecimal.ONE, new BigDecimal("10.00")));

      verify(itemOsPecaRepository, org.mockito.Mockito.times(1)).save(any(ItemOsPeca.class));
    }

    @Test
    @DisplayName("não deve lançar peça em OS cancelada")
    void naoDeveLancarEmOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "0.00");
      osEncontrada(os);
      osNaoEditavel(os, new OSCanceledException());

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
    void naoDeveLancarEmOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "0.00");
      osEncontrada(os);
      osNaoEditavel(os, new OSFinishedException());

      assertThatThrownBy(
              () ->
                  service.criar(
                      new ItemOsPecaRequestDTO(
                          OS_ID, "Peça", BigDecimal.ONE, new BigDecimal("10.00"))))
          .isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve propagar 'OS não encontrada' quando a OS não existe")
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

    @Test
    @DisplayName("não deve lançar peça em OS de outra oficina")
    void naoDeveLancarEmOsDeOutraOficina() {
      OrdemDeServico osDeOutra = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      osDeOutra.setOficina(oficina(OUTRA_OFICINA_ID));
      osEncontrada(osDeOutra);

      assertThatThrownBy(
              () ->
                  service.criar(
                      new ItemOsPecaRequestDTO(
                          OS_ID, "Peça", BigDecimal.ONE, new BigDecimal("10.00"))))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(valorRecalculator, never()).validarOsEditavel(any());
      verify(itemOsPecaRepository, never()).save(any());
    }
  }

  // ==================================================================
  // atualizar
  // ==================================================================

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName(
        "deve recalcular o valorTotal do item e o total da OS ao aumentar quantidade/preço")
    void deveAtualizarItemDeOs() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.atualizar(ITEM_ID, update("Pastilha premium", "4", "150.00"));

      assertThat(resposta.nome()).isEqualTo("Pastilha premium");
      assertThat(resposta.quantidade()).isEqualByComparingTo("4");
      assertThat(resposta.valorUnitario()).isEqualByComparingTo("150.00");
      assertThat(resposta.valorTotal()).isEqualByComparingTo("600.00");

      InOrder ordem = inOrder(valorRecalculator, itemOsPecaRepository);
      ordem.verify(valorRecalculator).validarOsEditavel(os);
      ordem.verify(itemOsPecaRepository).save(any(ItemOsPeca.class));
      ordem.verify(valorRecalculator).recalcular(os);

      // aumento de valor nunca precisa consultar pagamento
      verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("não deve alterar o vínculo com a OS")
    void naoDeveAlterarVinculo() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.atualizar(ITEM_ID, update("Outro nome", "2", "120.00"));

      assertThat(resposta.osId()).isEqualTo(OS_ID);
      assertThat(itemSalvo().getOrdemDeServico()).isSameAs(os);
      verify(ordemDeServicoService, never()).buscarPorEntidadeId(any());
    }

    @Test
    @DisplayName("deve atualizar peça sem OS sem validar nem recalcular OS")
    void deveAtualizarPecaSemOs() {
      itemEncontrado(itemSemOs("2", "120.00"));
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta =
          service.atualizar(ITEM_ID, update("Pastilha premium", "4", "150.00"));

      assertThat(resposta.osId()).isNull();
      assertThat(resposta.valorTotal()).isEqualByComparingTo("600.00");

      verifyNoInteractions(ordemDeServicoService, pagamentoService, valorRecalculator);
    }

    @Test
    @DisplayName("deve permitir reduzir o valor quando o restante fica exatamente igual ao já pago")
    void devePermitirReducaoAteOValorPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(os, "2", "120.00")); // 240
      valorPagoDaOs("40.00");
      salvarAtribuindoId();

      // novo valor do item = 40 => remove 200 => OS fica com 40 == pago
      ItemOsPecaResponseDTO resposta = service.atualizar(ITEM_ID, update("x", "1", "40.00"));

      assertThat(resposta.valorTotal()).isEqualByComparingTo("40.00");
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve reduzir o valor se a OS ficaria abaixo do que já foi pago")
    void naoDeveReduzirAbaixoDoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00"); // 240
      itemEncontrado(existente);
      valorPagoDaOs("100.00");

      // novo valor = 50 => OS ficaria com 50 < 100 pagos
      assertThatThrownBy(() -> service.atualizar(ITEM_ID, update("x", "1", "50.00")))
          .isInstanceOf(PagamentoValorExcedidoException.class);

      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve atualizar peça de OS cancelada")
    void naoDeveAtualizarEmOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      osNaoEditavel(os, new OSCanceledException());

      assertThatThrownBy(() -> service.atualizar(ITEM_ID, update("x", "1", "1.00")))
          .isInstanceOf(OSCanceledException.class);

      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve atualizar peça de OS fechada")
    void naoDeveAtualizarEmOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      osNaoEditavel(os, new OSFinishedException());

      assertThatThrownBy(() -> service.atualizar(ITEM_ID, update("x", "1", "1.00")))
          .isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "deve lançar ItemOsPecaNotFoundException ao atualizar item inexistente ou de outra oficina")
    void deveLancarQuandoItemNaoExiste() {
      itemNaoEncontrado(404L);

      assertThatThrownBy(() -> service.atualizar(404L, update("x", "1", "1.00")))
          .isInstanceOf(ItemOsPecaNotFoundException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }
  }

  // ==================================================================
  // deletar
  // ==================================================================

  @Nested
  @DisplayName("deletar")
  class Deletar {

    @Test
    @DisplayName("deve excluir peça sem OS diretamente")
    void deveExcluirPecaSemOs() {
      ItemOsPeca existente = itemSemOs("2", "120.00");
      itemEncontrado(existente);

      service.deletar(ITEM_ID);

      verify(itemOsPecaRepository).delete(existente);
      verifyNoInteractions(ordemDeServicoService, pagamentoService, valorRecalculator);
    }

    @Test
    @DisplayName("deve validar, excluir a peça e recalcular o total da OS, nessa ordem")
    void deveExcluirERecalcular() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      itemEncontrado(existente);
      valorPagoDaOs("0.00");

      service.deletar(ITEM_ID);

      InOrder ordem = inOrder(valorRecalculator, itemOsPecaRepository);
      ordem.verify(valorRecalculator).validarOsEditavel(os);
      ordem.verify(itemOsPecaRepository).delete(existente);
      ordem.verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve excluir peça se a OS passaria a valer menos do que já foi pago")
    void naoDeveExcluirAbaixoDoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      valorPagoDaOs("100.00");

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
      itemEncontrado(existente);
      valorPagoDaOs("200.00");

      service.deletar(ITEM_ID);

      verify(itemOsPecaRepository).delete(existente);
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve excluir peça de OS fechada")
    void naoDeveExcluirEmOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      osNaoEditavel(os, new OSFinishedException());

      assertThatThrownBy(() -> service.deletar(ITEM_ID)).isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("não deve excluir peça de OS cancelada")
    void naoDeveExcluirEmOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "240.00");
      itemEncontrado(item(os, "2", "120.00"));
      osNaoEditavel(os, new OSCanceledException());

      assertThatThrownBy(() -> service.deletar(ITEM_ID)).isInstanceOf(OSCanceledException.class);

      verify(itemOsPecaRepository, never()).delete(any());
    }

    @Test
    @DisplayName(
        "deve lançar ItemOsPecaNotFoundException ao excluir item inexistente ou de outra oficina")
    void deveLancarQuandoItemNaoExiste() {
      itemNaoEncontrado(404L);

      assertThatThrownBy(() -> service.deletar(404L))
          .isInstanceOf(ItemOsPecaNotFoundException.class);

      verify(itemOsPecaRepository, never()).delete(any());
    }
  }

  // ==================================================================
  // vincularOs
  // ==================================================================

  @Nested
  @DisplayName("vincularOs")
  class VincularOs {

    @Test
    @DisplayName("deve vincular peça avulsa a uma OS da mesma oficina e recalcular o total")
    void deveVincular() {
      ItemOsPeca existente = itemSemOs("2", "120.00");
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      itemEncontrado(existente);
      osEncontrada(os);
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta = service.vincularOs(ITEM_ID, OS_ID);

      assertThat(resposta.osId()).isEqualTo(OS_ID);
      assertThat(itemSalvo().getOrdemDeServico()).isSameAs(os);

      InOrder ordem = inOrder(valorRecalculator, itemOsPecaRepository);
      ordem.verify(valorRecalculator).validarOsEditavel(os);
      ordem.verify(itemOsPecaRepository).save(existente);
      ordem.verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("não deve vincular peça que já está vinculada a uma OS")
    void naoDeveVincularPecaJaVinculada() {
      OrdemDeServico osAtual = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(osAtual, "2", "120.00"));

      assertThatThrownBy(() -> service.vincularOs(ITEM_ID, 200L))
          .isInstanceOf(ItemOsPecaJaVinculadoException.class);

      verify(ordemDeServicoService, never()).buscarPorEntidadeId(any());
      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve vincular a uma OS de outra oficina")
    void naoDeveVincularAOsDeOutraOficina() {
      ItemOsPeca existente = itemSemOs("2", "120.00");
      OrdemDeServico osDeOutra = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      osDeOutra.setOficina(oficina(OUTRA_OFICINA_ID));
      itemEncontrado(existente);
      osEncontrada(osDeOutra);

      assertThatThrownBy(() -> service.vincularOs(ITEM_ID, OS_ID))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      assertThat(existente.getOrdemDeServico()).isNull();
      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("deve propagar 'OS não encontrada'")
    void devePropagarOsInexistente() {
      itemEncontrado(itemSemOs("2", "120.00"));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.vincularOs(ITEM_ID, OS_ID))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("não deve vincular a OS cancelada")
    void naoDeveVincularAOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "0.00");
      itemEncontrado(itemSemOs("2", "120.00"));
      osEncontrada(os);
      osNaoEditavel(os, new OSCanceledException());

      assertThatThrownBy(() -> service.vincularOs(ITEM_ID, OS_ID))
          .isInstanceOf(OSCanceledException.class);

      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve vincular a OS fechada")
    void naoDeveVincularAOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "0.00");
      itemEncontrado(itemSemOs("2", "120.00"));
      osEncontrada(os);
      osNaoEditavel(os, new OSFinishedException());

      assertThatThrownBy(() -> service.vincularOs(ITEM_ID, OS_ID))
          .isInstanceOf(OSFinishedException.class);

      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "deve lançar ItemOsPecaNotFoundException para peça inexistente ou de outra oficina")
    void deveLancarQuandoItemNaoExiste() {
      itemNaoEncontrado(404L);

      assertThatThrownBy(() -> service.vincularOs(404L, OS_ID))
          .isInstanceOf(ItemOsPecaNotFoundException.class);

      verifyNoInteractions(ordemDeServicoService, valorRecalculator);
    }
  }

  // ==================================================================
  // desvincularOs
  // ==================================================================

  @Nested
  @DisplayName("desvincularOs")
  class DesvincularOs {

    @Test
    @DisplayName("deve desvincular a peça, mantê-la na oficina e recalcular a OS anterior")
    void deveDesvincular() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      itemEncontrado(existente);
      valorPagoDaOs("0.00");
      salvarAtribuindoId();

      ItemOsPecaResponseDTO resposta = service.desvincularOs(ITEM_ID);

      assertThat(resposta.osId()).isNull();
      assertThat(resposta.id()).isEqualTo(ITEM_ID);

      ItemOsPeca salvo = itemSalvo();
      assertThat(salvo.getOrdemDeServico()).isNull();
      assertThat(salvo.getOficina().getId()).isEqualTo(OFICINA_ID);

      InOrder ordem = inOrder(valorRecalculator, itemOsPecaRepository);
      ordem.verify(valorRecalculator).validarOsEditavel(os);
      ordem.verify(itemOsPecaRepository).save(existente);
      ordem.verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName(
        "deve permitir desvincular quando o restante da OS fica exatamente igual ao já pago")
    void devePermitirQuandoRestanteIgualAoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      itemEncontrado(item(os, "1", "40.00"));
      valorPagoDaOs("200.00");
      salvarAtribuindoId();

      service.desvincularOs(ITEM_ID);

      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("deve ser idempotente para peça que já está sem OS")
    void deveSerIdempotente() {
      itemEncontrado(itemSemOs("2", "120.00"));

      ItemOsPecaResponseDTO resposta = service.desvincularOs(ITEM_ID);

      assertThat(resposta.osId()).isNull();
      verify(itemOsPecaRepository, never()).save(any());
      verifyNoInteractions(pagamentoService, valorRecalculator, ordemDeServicoService);
    }

    @Test
    @DisplayName("não deve desvincular se a OS passaria a valer menos do que já foi pago")
    void naoDeveDesvincularAbaixoDoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      itemEncontrado(existente);
      valorPagoDaOs("100.00");

      assertThatThrownBy(() -> service.desvincularOs(ITEM_ID))
          .isInstanceOf(PagamentoValorExcedidoException.class);

      assertThat(existente.getOrdemDeServico()).isSameAs(os);
      verify(itemOsPecaRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("não deve desvincular peça de OS fechada")
    void naoDeveDesvincularDeOsFechada() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      itemEncontrado(existente);
      osNaoEditavel(os, new OSFinishedException());

      assertThatThrownBy(() -> service.desvincularOs(ITEM_ID))
          .isInstanceOf(OSFinishedException.class);

      assertThat(existente.getOrdemDeServico()).isSameAs(os);
      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("não deve desvincular peça de OS cancelada")
    void naoDeveDesvincularDeOsCancelada() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "240.00");
      ItemOsPeca existente = item(os, "2", "120.00");
      itemEncontrado(existente);
      osNaoEditavel(os, new OSCanceledException());

      assertThatThrownBy(() -> service.desvincularOs(ITEM_ID))
          .isInstanceOf(OSCanceledException.class);

      assertThat(existente.getOrdemDeServico()).isSameAs(os);
      verify(itemOsPecaRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "deve lançar ItemOsPecaNotFoundException para peça inexistente ou de outra oficina")
    void deveLancarQuandoItemNaoExiste() {
      itemNaoEncontrado(404L);

      assertThatThrownBy(() -> service.desvincularOs(404L))
          .isInstanceOf(ItemOsPecaNotFoundException.class);

      verifyNoInteractions(valorRecalculator, pagamentoService);
    }
  }

  // ==================================================================
  // consultas e isolamento por oficina
  // ==================================================================

  @Nested
  @DisplayName("consultas e isolamento por oficina")
  class Consultas {

    @Test
    @DisplayName("deve listar peças da OS filtrando pela oficina do usuário")
    void deveListarPecasDaOs() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "240.00");
      osEncontrada(os);
      when(itemOsPecaRepository.findByOrdemDeServicoIdAndOficinaId(OS_ID, OFICINA_ID))
          .thenReturn(List.of(item(os, "2", "120.00")));

      List<ItemOsPecaResponseDTO> resultado = service.listarPorOrdemServico(OS_ID);

      assertThat(resultado).hasSize(1);
      assertThat(resultado.get(0).osId()).isEqualTo(OS_ID);
      assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    @DisplayName("deve retornar lista vazia quando a OS não tem peças")
    void deveRetornarListaVazia() {
      osEncontrada(os(StatusOrdemDeServico.ABERTA, "0.00"));
      when(itemOsPecaRepository.findByOrdemDeServicoIdAndOficinaId(OS_ID, OFICINA_ID))
          .thenReturn(List.of());

      assertThat(service.listarPorOrdemServico(OS_ID)).isEmpty();
    }

    @Test
    @DisplayName("não deve listar peças de OS inexistente ou de outra oficina")
    void naoDeveListarDeOsInacessivel() {
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.listarPorOrdemServico(OS_ID))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(itemOsPecaRepository, never()).findByOrdemDeServicoIdAndOficinaId(any(), any());
    }

    @Test
    @DisplayName("deve buscar peça vinculada pelo id filtrando pela oficina do usuário")
    void deveBuscarPecaVinculada() {
      itemEncontrado(item(os(StatusOrdemDeServico.EM_EXECUCAO, "240.00"), "2", "120.00"));

      ItemOsPecaResponseDTO resposta = service.buscarPorId(ITEM_ID);

      assertThat(resposta.id()).isEqualTo(ITEM_ID);
      assertThat(resposta.osId()).isEqualTo(OS_ID);
      verify(itemOsPecaRepository).findByIdAndOficinaId(ITEM_ID, OFICINA_ID);
    }

    @Test
    @DisplayName("deve buscar peça avulsa (sem OS) com osId nulo")
    void deveBuscarPecaAvulsa() {
      itemEncontrado(itemSemOs("2", "120.00"));

      ItemOsPecaResponseDTO resposta = service.buscarPorId(ITEM_ID);

      assertThat(resposta.id()).isEqualTo(ITEM_ID);
      assertThat(resposta.osId()).isNull();
      verifyNoInteractions(ordemDeServicoService);
    }

    @Test
    @DisplayName(
        "deve lançar ItemOsPecaNotFoundException para peça inexistente ou de outra oficina")
    void deveLancarQuandoItemNaoExiste() {
      itemNaoEncontrado(404L);

      assertThatThrownBy(() -> service.buscarPorId(404L))
          .isInstanceOf(ItemOsPecaNotFoundException.class);

      verify(itemOsPecaRepository).findByIdAndOficinaId(404L, OFICINA_ID);
    }
  }
}
