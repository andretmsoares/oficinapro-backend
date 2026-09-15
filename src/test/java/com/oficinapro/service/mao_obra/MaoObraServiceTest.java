package com.oficinapro.service.mao_obra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.mao_obra.MaoObraNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.model.MaoObra;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.MaoObraRepository;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaoObraServiceTest {

  private static final Long OS_ID = 100L;
  private static final Long MAO_OBRA_ID = 7L;

  @Mock private MaoObraRepository maoObraRepository;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OrdemDeServicoValorRecalculator valorRecalculator;

  @InjectMocks private MaoObraServiceImpl service;

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

  private MaoObra maoObra(OrdemDeServico os, String valor) {
    MaoObra maoObra = new MaoObra();
    maoObra.setId(MAO_OBRA_ID);
    maoObra.setOrdemDeServico(os);
    maoObra.setValor(new BigDecimal(valor));
    maoObra.setDescricao("Troca de embreagem");
    return maoObra;
  }

  private PagamentoResponseDTO pagamentoPago(String valorPago) {
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

    @Test
    @DisplayName("deve criar a mão de obra vinculada à OS e disparar o recálculo do valor total")
    void deveCriarMaoObraERecalcularValorDaOs() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(maoObraRepository.save(any(MaoObra.class)))
          .thenAnswer(
              invocation -> {
                MaoObra salva = invocation.getArgument(0);
                salva.setId(MAO_OBRA_ID);
                return salva;
              });

      MaoObraResponseDTO resposta =
          service.criar(new MaoObraRequestDTO(OS_ID, new BigDecimal("250.00"), "Revisão geral"));

      assertThat(resposta.id()).isEqualTo(MAO_OBRA_ID);
      assertThat(resposta.osId()).isEqualTo(OS_ID);
      assertThat(resposta.valor()).isEqualByComparingTo("250.00");
      assertThat(resposta.descricao()).isEqualTo("Revisão geral");
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("deve salvar a mão de obra antes de recalcular o total da OS")
    void deveSalvarAntesDeRecalcular() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(maoObraRepository.save(any(MaoObra.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      service.criar(new MaoObraRequestDTO(OS_ID, new BigDecimal("100.00"), "Serviço"));

      InOrder ordem = inOrder(maoObraRepository, valorRecalculator);
      ordem.verify(maoObraRepository).save(any(MaoObra.class));
      ordem.verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName("deve validar se a OS aceita edição antes de persistir")
    void deveValidarOsEditavelAntesDePersistir() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "0.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSCanceledException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(
              () ->
                  service.criar(new MaoObraRequestDTO(OS_ID, new BigDecimal("100.00"), "Serviço")))
          .isInstanceOf(OSCanceledException.class);

      verify(maoObraRepository, never()).save(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("deve propagar OrdemDeServicoNotFoundException quando a OS não existe")
    void devePropagarQuandoOsNaoExiste() {
      when(ordemDeServicoService.buscarPorEntidadeId(999L))
          .thenThrow(new OrdemDeServicoNotFoundException(999L));

      assertThatThrownBy(
              () -> service.criar(new MaoObraRequestDTO(999L, new BigDecimal("100.00"), "Serviço")))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(maoObraRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName("deve atualizar valor e descrição e recalcular o total da OS")
    void deveAtualizarValorEDescricao() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "300.00");
      MaoObra existente = maoObra(os, "300.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(maoObraRepository.save(any(MaoObra.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      MaoObraResponseDTO resposta =
          service.atualizar(
              MAO_OBRA_ID, new MaoObraRequestDTO(OS_ID, new BigDecimal("450.00"), "Revisado"));

      assertThat(resposta.valor()).isEqualByComparingTo("450.00");
      assertThat(resposta.descricao()).isEqualTo("Revisado");
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName(
        "não deve mover a mão de obra para outra OS via update: o osId do corpo é ignorado")
    void naoDeveMoverMaoObraParaOutraOs() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "300.00");
      MaoObra existente = maoObra(os, "300.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(maoObraRepository.save(any(MaoObra.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Long outraOsId = 888L;
      MaoObraResponseDTO resposta =
          service.atualizar(
              MAO_OBRA_ID, new MaoObraRequestDTO(outraOsId, new BigDecimal("450.00"), "Revisado"));

      assertThat(resposta.osId())
          .as("a mão de obra deve permanecer na OS original")
          .isEqualTo(OS_ID);
      assertThat(existente.getOrdemDeServico().getId()).isEqualTo(OS_ID);
    }

    @Test
    @DisplayName("deve lançar MaoObraNotFoundException ao atualizar item inexistente")
    void deveLancarQuandoMaoObraNaoExiste() {
      when(maoObraRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.atualizar(
                      404L, new MaoObraRequestDTO(OS_ID, new BigDecimal("10.00"), "x")))
          .isInstanceOf(MaoObraNotFoundException.class);
    }

    @Test
    @DisplayName("deve bloquear atualização quando a OS não aceita mais edição")
    void deveBloquearAtualizacaoEmOsNaoEditavel() {
      OrdemDeServico os = os(StatusOrdemDeServico.FECHADA, "300.00");
      MaoObra existente = maoObra(os, "300.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSFinishedException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(
              () ->
                  service.atualizar(
                      MAO_OBRA_ID, new MaoObraRequestDTO(OS_ID, new BigDecimal("1.00"), "x")))
          .isInstanceOf(OSFinishedException.class);

      verify(maoObraRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("deletar")
  class Deletar {

    @Test
    @DisplayName("deve excluir a mão de obra quando o valor restante ainda cobre o já pago")
    void deveExcluirQuandoValorRestanteCobreOPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      MaoObra existente = maoObra(os, "30.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoPago("50.00"));

      service.deletar(MAO_OBRA_ID);

      verify(maoObraRepository).delete(existente);
      verify(valorRecalculator).recalcular(os);
    }

    @Test
    @DisplayName(
        "não deve excluir quando a remoção deixaria o valor da OS abaixo do que já foi pago")
    void naoDeveExcluirSeDeixarOsAbaixoDoValorPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      MaoObra existente = maoObra(os, "30.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      // Restante seria 70,00 mas o cliente já pagou 80,00 → geraria pagamento a maior.
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoPago("80.00"));

      assertThatThrownBy(() -> service.deletar(MAO_OBRA_ID))
          .isInstanceOf(PagamentoValorExcedidoException.class);

      verify(maoObraRepository, never()).delete(any());
      verify(valorRecalculator, never()).recalcular(any());
    }

    @Test
    @DisplayName("deve permitir excluir quando o valor restante fica exatamente igual ao já pago")
    void devePermitirQuandoValorRestanteIgualAoPago() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      MaoObra existente = maoObra(os, "30.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(pagamentoService.buscarPorOsId(OS_ID)).thenReturn(pagamentoPago("70.00"));

      service.deletar(MAO_OBRA_ID);

      verify(maoObraRepository).delete(existente);
    }

    @Test
    @DisplayName("deve bloquear exclusão quando a OS não aceita mais edição")
    void deveBloquearExclusaoEmOsNaoEditavel() {
      OrdemDeServico os = os(StatusOrdemDeServico.CANCELADA, "100.00");
      MaoObra existente = maoObra(os, "30.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(existente));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      doThrow(new OSCanceledException()).when(valorRecalculator).validarOsEditavel(os);

      assertThatThrownBy(() -> service.deletar(MAO_OBRA_ID))
          .isInstanceOf(OSCanceledException.class);

      verify(maoObraRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deve lançar MaoObraNotFoundException ao excluir item inexistente")
    void deveLancarQuandoMaoObraNaoExiste() {
      when(maoObraRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deletar(404L)).isInstanceOf(MaoObraNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("consultas e isolamento por oficina")
  class Consultas {

    @Test
    @DisplayName("deve validar o acesso à OS antes de listar suas mãos de obra")
    void deveValidarAcessoAOsAntesDeListar() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(maoObraRepository.findByOrdemDeServicoId(OS_ID))
          .thenReturn(List.of(maoObra(os, "40.00")));

      List<MaoObraResponseDTO> resultado = service.listarPorOrdemServico(OS_ID);

      assertThat(resultado).hasSize(1);
      assertThat(resultado.get(0).valor()).isEqualByComparingTo("40.00");
      verify(ordemDeServicoService).buscarPorEntidadeId(OS_ID);
    }

    @Test
    @DisplayName("não deve listar mãos de obra de OS de outra oficina")
    void naoDeveListarMaoObraDeOutraOficina() {
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.listarPorOrdemServico(OS_ID))
          .as("o isolamento é delegado ao service de OS, que devolve 'não encontrado'")
          .isInstanceOf(OrdemDeServicoNotFoundException.class);

      verify(maoObraRepository, never()).findByOrdemDeServicoId(any());
    }

    @Test
    @DisplayName("deve revalidar o acesso à OS ao buscar uma mão de obra por id")
    void deveRevalidarAcessoAoBuscarPorId() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(maoObra(os, "40.00")));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);

      MaoObraResponseDTO resposta = service.buscarPorId(MAO_OBRA_ID);

      assertThat(resposta.id()).isEqualTo(MAO_OBRA_ID);
      verify(ordemDeServicoService).buscarPorEntidadeId(OS_ID);
    }

    @Test
    @DisplayName("deve negar a busca por id quando a OS pertence a outra oficina")
    void deveNegarBuscaPorIdDeOutraOficina() {
      OrdemDeServico os = os(StatusOrdemDeServico.EM_EXECUCAO, "100.00");
      when(maoObraRepository.findById(MAO_OBRA_ID)).thenReturn(Optional.of(maoObra(os, "40.00")));
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.buscarPorId(MAO_OBRA_ID))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);
    }
  }
}
