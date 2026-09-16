package com.oficinapro.service.pagamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoAlreadyExistsException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundForThisOsException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.exception.pagamento.PagamentoValorInvalidoException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.repository.PagamentoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceImplTest {

  private static final Long PAGAMENTO_ID = 50L;
  private static final Long OS_ID = 100L;
  private static final Long OFICINA_ID = 1L;

  @Mock private PagamentoRepository repository;
  @Mock private OrdemDeServicoService ordemDeServicoService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private PagamentoServiceImpl service;

  private OrdemDeServico os(String valorComDesconto) {
    Oficina oficina = new Oficina();
    oficina.setId(OFICINA_ID);

    OrdemDeServico os = new OrdemDeServico();
    os.setId(OS_ID);
    os.setOficina(oficina);
    os.setValorTotal(new BigDecimal(valorComDesconto));
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(new BigDecimal(valorComDesconto));
    return os;
  }

  private Pagamento pagamento(OrdemDeServico os, String valorPago, StatusPagamento status) {
    Pagamento pagamento = new Pagamento();
    pagamento.setId(PAGAMENTO_ID);
    pagamento.setOrdemDeServico(os);
    pagamento.setValorPago(new BigDecimal(valorPago));
    pagamento.setStatus(status);
    pagamento.setObs("");
    return pagamento;
  }

  private void devolveOArgumentoSalvo() {
    when(repository.save(any(Pagamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("criar")
  class Criar {

    @Test
    @DisplayName("deve criar o pagamento zerado e pendente, ignorando qualquer valor do corpo")
    void deveCriarPagamentoZeradoEPendente() {
      OrdemDeServico os = os("500.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(null);
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta = service.criar(new PagamentoRequestDTO(OS_ID, "adiantamento"));

      assertThat(resposta.valorPago())
          .as("um pagamento recém-criado nunca nasce com valor pago")
          .isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(resposta.status()).isEqualTo(StatusPagamento.PAGAMENTO_PENDENTE);
      assertThat(resposta.osId()).isEqualTo(OS_ID);
      assertThat(resposta.obs()).isEqualTo("adiantamento");
    }

    @Test
    @DisplayName("deve recusar um segundo pagamento para a mesma OS")
    void deveRecusarSegundoPagamentoParaMesmaOs() {
      OrdemDeServico os = os("500.00");
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID)).thenReturn(os);
      when(repository.findByOrdemDeServicoId(OS_ID))
          .thenReturn(pagamento(os, "0.00", StatusPagamento.PAGAMENTO_PENDENTE));

      assertThatThrownBy(() -> service.criar(new PagamentoRequestDTO(OS_ID, "")))
          .as("a relação OS↔pagamento é 1:1, garantida também pela constraint uk_pagamento_os")
          .isInstanceOf(PagamentoAlreadyExistsException.class);

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("somente GERENTE pode criar pagamento")
    void somenteGerentePodeCriarPagamento() {
      doThrow(new AccessDeniedException("sem permissão"))
          .when(oficinaAccessValidator)
          .validarRole(Role.GERENTE);

      assertThatThrownBy(() -> service.criar(new PagamentoRequestDTO(OS_ID, "")))
          .isInstanceOf(AccessDeniedException.class);

      verify(ordemDeServicoService, never()).buscarPorEntidadeId(any());
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve propagar 'OS não encontrada' quando a OS não existe ou é de outra oficina")
    void devePropagarOsInexistente() {
      when(ordemDeServicoService.buscarPorEntidadeId(OS_ID))
          .thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

      assertThatThrownBy(() -> service.criar(new PagamentoRequestDTO(OS_ID, "")))
          .isInstanceOf(OrdemDeServicoNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName("deve alterar apenas a observação, preservando valor pago e status")
    void deveAlterarApenasObservacao() {
      OrdemDeServico os = os("500.00");
      Pagamento existente = pagamento(os, "200.00", StatusPagamento.PAGO_PARCIALMENTE);
      when(repository.findById(PAGAMENTO_ID)).thenReturn(Optional.of(existente));
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta =
          service.atualizar(PAGAMENTO_ID, new PagamentoRequestDTO(OS_ID, "nova observação"));

      assertThat(resposta.obs()).isEqualTo("nova observação");
      assertThat(resposta.valorPago())
          .as("o valor pago só muda por registro de pagamento ou estorno")
          .isEqualByComparingTo("200.00");
      assertThat(resposta.status()).isEqualTo(StatusPagamento.PAGO_PARCIALMENTE);
    }

    @Test
    @DisplayName("somente GERENTE pode atualizar pagamento")
    void somenteGerentePodeAtualizar() {
      doThrow(new AccessDeniedException("sem permissão"))
          .when(oficinaAccessValidator)
          .validarRole(Role.GERENTE);

      assertThatThrownBy(() -> service.atualizar(PAGAMENTO_ID, new PagamentoRequestDTO(OS_ID, "x")))
          .isInstanceOf(AccessDeniedException.class);

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("atualizarValorPago e estornarValorPago")
  class MovimentacaoDeValor {

    @ParameterizedTest(name = "OS de {0}, pagando {1} → status {2}")
    @CsvSource({
      "500.00, 200.00, PAGO_PARCIALMENTE",
      "500.00, 499.99, PAGO_PARCIALMENTE",
      "500.00, 500.00, PAGA",
      "0.01,   0.01,   PAGA"
    })
    @DisplayName("deve classificar o status conforme o valor pago acumulado")
    void deveClassificarStatusConformeValorPago(
        String valorOs, String valorPagamento, StatusPagamento statusEsperado) {
      OrdemDeServico os = os(valorOs);
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "0.00", StatusPagamento.PAGAMENTO_PENDENTE)));
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta =
          service.atualizarValorPago(PAGAMENTO_ID, new BigDecimal(valorPagamento));

      assertThat(resposta.status()).isEqualTo(statusEsperado);
      assertThat(resposta.valorPago()).isEqualByComparingTo(valorPagamento);
    }

    @Test
    @DisplayName("deve recusar pagamento que excede o valor da OS")
    void deveRecusarPagamentoAcimaDoValorDaOs() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "0.00", StatusPagamento.PAGAMENTO_PENDENTE)));

      assertThatThrownBy(() -> service.atualizarValorPago(PAGAMENTO_ID, new BigDecimal("500.01")))
          .isInstanceOf(PagamentoValorExcedidoException.class);

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve acumular pagamentos parciais até completar o valor da OS")
    void deveAcumularPagamentosParciais() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "300.00", StatusPagamento.PAGO_PARCIALMENTE)));
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta =
          service.atualizarValorPago(PAGAMENTO_ID, new BigDecimal("200.00"));

      assertThat(resposta.valorPago()).isEqualByComparingTo("500.00");
      assertThat(resposta.status()).isEqualTo(StatusPagamento.PAGA);
    }

    @Test
    @DisplayName("estorno total deve voltar o pagamento para pendente")
    void estornoTotalVoltaParaPendente() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "200.00", StatusPagamento.PAGO_PARCIALMENTE)));
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta =
          service.estornarValorPago(PAGAMENTO_ID, new BigDecimal("200.00"));

      assertThat(resposta.valorPago()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(resposta.status()).isEqualTo(StatusPagamento.PAGAMENTO_PENDENTE);
    }

    @Test
    @DisplayName("estorno parcial deve manter o pagamento como parcial")
    void estornoParcialMantemParcial() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "500.00", StatusPagamento.PAGA)));
      devolveOArgumentoSalvo();

      PagamentoResponseDTO resposta =
          service.estornarValorPago(PAGAMENTO_ID, new BigDecimal("100.00"));

      assertThat(resposta.valorPago()).isEqualByComparingTo("400.00");
      assertThat(resposta.status()).isEqualTo(StatusPagamento.PAGO_PARCIALMENTE);
    }

    @Test
    @DisplayName("não deve estornar mais do que já foi pago")
    void naoDeveEstornarMaisDoQueFoiPago() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "100.00", StatusPagamento.PAGO_PARCIALMENTE)));

      assertThatThrownBy(() -> service.estornarValorPago(PAGAMENTO_ID, new BigDecimal("150.00")))
          .isInstanceOf(PagamentoValorInvalidoException.class)
          .hasMessageContaining("estorno excede");

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("somente GERENTE pode movimentar valores")
    void somenteGerentePodeMovimentarValores() {
      doThrow(new AccessDeniedException("sem permissão"))
          .when(oficinaAccessValidator)
          .validarRole(Role.GERENTE);

      assertThatThrownBy(() -> service.atualizarValorPago(PAGAMENTO_ID, new BigDecimal("10.00")))
          .isInstanceOf(AccessDeniedException.class);

      verify(repository, never()).findById(any());
    }
  }

  @Nested
  @DisplayName("recalcularStatus (disparado quando o valor da OS muda)")
  class RecalcularStatus {

    @Test
    @DisplayName("deve marcar como PAGA quando o valor da OS cai exatamente para o valor já pago")
    void deveMarcarComoPagaQuandoOsIgualaValorPago() {
      OrdemDeServico os = os("200.00");
      Pagamento existente = pagamento(os, "200.00", StatusPagamento.PAGO_PARCIALMENTE);

      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      service.recalcularStatus(OS_ID);

      assertThat(existente.getStatus()).isEqualTo(StatusPagamento.PAGA);
      assertThat(existente.getDataPagamentoTotal())
          .as("quitação registra a data de pagamento total")
          .isNotNull();
      verify(repository).save(existente);
    }

    @Test
    @DisplayName("deve voltar para PAGO_PARCIALMENTE quando a OS aumenta de valor")
    void deveVoltarParaParcialQuandoOsAumenta() {
      OrdemDeServico os = os("800.00");
      Pagamento existente = pagamento(os, "500.00", StatusPagamento.PAGA);
      existente.setDataPagamentoTotal(java.time.LocalDateTime.now());
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      service.recalcularStatus(OS_ID);

      assertThat(existente.getStatus())
          .as("adicionar peça/mão de obra numa OS quitada reabre o saldo")
          .isEqualTo(StatusPagamento.PAGO_PARCIALMENTE);
      assertThat(existente.getDataPagamentoTotal()).isNull();
    }

    @Test
    @DisplayName("deve recusar reduzir o valor da OS abaixo do que já foi pago")
    void deveRecusarReduzirOsAbaixoDoValorPago() {
      OrdemDeServico os = os("100.00");
      Pagamento existente = pagamento(os, "300.00", StatusPagamento.PAGA);
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      assertThatThrownBy(() -> service.recalcularStatus(OS_ID))
          .as(
              "Antes do refactor esta situação produzia 'valor a receber' negativo."
                  + " Agora a operação é recusada.")
          .isInstanceOf(PagamentoValorExcedidoException.class);

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve manter PAGAMENTO_PENDENTE quando nada foi pago")
    void deveManterPendenteQuandoNadaFoiPago() {
      OrdemDeServico os = os("450.00");
      Pagamento existente = pagamento(os, "0.00", StatusPagamento.PAGO_PARCIALMENTE);
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      service.recalcularStatus(OS_ID);

      assertThat(existente.getStatus()).isEqualTo(StatusPagamento.PAGAMENTO_PENDENTE);
      assertThat(existente.getDataPagamentoTotal()).isNull();
    }

    @Test
    @DisplayName("OS zerada sem pagamento permanece pendente, não é tratada como quitada")
    void osZeradaSemPagamentoPermanecePendente() {
      OrdemDeServico os = os("0.00");
      Pagamento existente = pagamento(os, "0.00", StatusPagamento.PAGAMENTO_PENDENTE);
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      service.recalcularStatus(OS_ID);

      assertThat(existente.getStatus())
          .as("uma OS sem itens não deve aparecer como paga")
          .isEqualTo(StatusPagamento.PAGAMENTO_PENDENTE);
    }

    @Test
    @DisplayName("recalcularStatus não exige role de GERENTE: é chamado por fluxo interno")
    void recalcularStatusNaoExigeRole() {
      OrdemDeServico os = os("100.00");
      Pagamento existente = pagamento(os, "50.00", StatusPagamento.PAGO_PARCIALMENTE);
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(existente);

      service.recalcularStatus(OS_ID);

      verify(oficinaAccessValidator, never()).validarRole(any(Role[].class));
    }
  }

  @Nested
  @DisplayName("consultas e isolamento por oficina")
  class Consultas {

    @Test
    @DisplayName("deve validar o acesso ao registro ao buscar pagamento por id")
    void deveValidarAcessoAoBuscarPorId() {
      OrdemDeServico os = os("500.00");
      when(repository.findById(PAGAMENTO_ID))
          .thenReturn(Optional.of(pagamento(os, "0.00", StatusPagamento.PAGAMENTO_PENDENTE)));

      service.buscarPorId(PAGAMENTO_ID);

      verify(oficinaAccessValidator)
          .validarAcessoAoRegistro(any(), any(PagamentoNotFoundException.class));
    }

    @Test
    @DisplayName("deve lançar PagamentoNotFoundException para id inexistente")
    void deveLancarParaIdInexistente() {
      when(repository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.buscarPorId(404L))
          .isInstanceOf(PagamentoNotFoundException.class);
    }

    @Test
    @DisplayName("deve lançar PagamentoNotFoundForThisOsException quando a OS não tem pagamento")
    void deveLancarQuandoOsNaoTemPagamento() {
      when(repository.findByOrdemDeServicoId(OS_ID)).thenReturn(null);

      assertThatThrownBy(() -> service.buscarPorOsId(OS_ID))
          .isInstanceOf(PagamentoNotFoundForThisOsException.class);
    }

    @Test
    @DisplayName("deve validar o acesso à oficina antes de listar pagamentos dela")
    void deveValidarAcessoAntesDeListarPorOficina() {
      when(repository.findByOrdemDeServicoOficinaId(OFICINA_ID)).thenReturn(List.of());

      service.buscarPorOficina(OFICINA_ID);

      verify(oficinaAccessValidator).validarAcessoOficina(OFICINA_ID);
    }

    @Test
    @DisplayName("não deve listar pagamentos de oficina de terceiros")
    void naoDeveListarPagamentosDeOutraOficina() {
      Long outraOficina = 99L;
      doThrow(new AccessDeniedException("oficina alheia"))
          .when(oficinaAccessValidator)
          .validarAcessoOficina(outraOficina);

      assertThatThrownBy(() -> service.buscarPorOficina(outraOficina))
          .isInstanceOf(AccessDeniedException.class);

      verify(repository, never()).findByOrdemDeServicoOficinaId(any());
    }

    @Test
    @DisplayName("não deve calcular valor a receber de oficina de terceiros")
    void naoDeveCalcularValorAReceberDeOutraOficina() {
      Long outraOficina = 99L;
      doThrow(new AccessDeniedException("oficina alheia"))
          .when(oficinaAccessValidator)
          .validarAcessoOficina(outraOficina);

      assertThatThrownBy(() -> service.calcularValorParaReceber(outraOficina))
          .as("este endpoint expunha o faturamento de qualquer oficina antes do refactor")
          .isInstanceOf(AccessDeniedException.class);

      verify(repository, never()).calcularValorParaReceber(any(), any());
    }

    @Test
    @DisplayName("deve considerar apenas pendentes e parciais no cálculo do valor a receber")
    void deveConsiderarApenasPendentesEParciaisNoValorAReceber() {
      when(repository.calcularValorParaReceber(
              OFICINA_ID,
              List.of(StatusPagamento.PAGAMENTO_PENDENTE, StatusPagamento.PAGO_PARCIALMENTE)))
          .thenReturn(new BigDecimal("1200.00"));

      BigDecimal resultado = service.calcularValorParaReceber(OFICINA_ID);

      assertThat(resultado).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("deve validar o acesso à oficina ao filtrar pagamentos por status")
    void deveValidarAcessoAoFiltrarPorStatus() {
      when(repository.findByOrdemDeServicoOficinaIdAndStatus(
              OFICINA_ID, StatusPagamento.PAGAMENTO_PENDENTE))
          .thenReturn(List.of());

      service.buscarPorStatus(OFICINA_ID, StatusPagamento.PAGAMENTO_PENDENTE);

      verify(oficinaAccessValidator).validarAcessoOficina(OFICINA_ID);
    }
  }
}
