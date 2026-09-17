package com.oficinapro.service.ordem_servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.ordemDeServico.AtualizarStatusOSRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

/**
 * Máquina de estados da Ordem de Serviço.
 *
 * <p>O grafo abaixo é declarado de forma independente da implementação, de propósito: ele descreve
 * a regra de negócio pretendida. Se alguém alterar {@code transicaoPermitida} sem alterar a regra,
 * estes testes quebram — que é exatamente o objetivo.
 *
 * <pre>
 * ABERTA               → DIAGNOSTICO | CANCELADA
 * DIAGNOSTICO          → AGUARDANDO_APROVACAO | CANCELADA
 * AGUARDANDO_APROVACAO → AGUARDANDO_PECAS | CANCELADA
 * AGUARDANDO_PECAS     → EM_EXECUCAO | CANCELADA
 * EM_EXECUCAO          → FINALIZADA | CANCELADA
 * FINALIZADA           → ENTREGUE | ABERTA
 * ENTREGUE             → FECHADA | ABERTA
 * FECHADA              → ABERTA
 * CANCELADA            → (estado terminal)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdemDeServicoStatusMachineTest {

  private static final Long OS_ID = 100L;
  private static final Long OFICINA_ID = 1L;

  /** Grafo de transições pretendido, declarado a partir da regra de negócio. */
  private static final Map<StatusOrdemDeServico, Set<StatusOrdemDeServico>> TRANSICOES_VALIDAS =
      new EnumMap<>(
          Map.of(
              StatusOrdemDeServico.ABERTA,
              EnumSet.of(StatusOrdemDeServico.DIAGNOSTICO, StatusOrdemDeServico.CANCELADA),
              StatusOrdemDeServico.DIAGNOSTICO,
              EnumSet.of(StatusOrdemDeServico.AGUARDANDO_APROVACAO, StatusOrdemDeServico.CANCELADA),
              StatusOrdemDeServico.AGUARDANDO_APROVACAO,
              EnumSet.of(StatusOrdemDeServico.AGUARDANDO_PECAS, StatusOrdemDeServico.CANCELADA),
              StatusOrdemDeServico.AGUARDANDO_PECAS,
              EnumSet.of(StatusOrdemDeServico.EM_EXECUCAO, StatusOrdemDeServico.CANCELADA),
              StatusOrdemDeServico.EM_EXECUCAO,
              EnumSet.of(StatusOrdemDeServico.FINALIZADA, StatusOrdemDeServico.CANCELADA),
              StatusOrdemDeServico.FINALIZADA,
              EnumSet.of(StatusOrdemDeServico.ENTREGUE, StatusOrdemDeServico.ABERTA),
              StatusOrdemDeServico.ENTREGUE,
              EnumSet.of(StatusOrdemDeServico.FECHADA, StatusOrdemDeServico.ABERTA),
              StatusOrdemDeServico.FECHADA,
              EnumSet.of(StatusOrdemDeServico.ABERTA),
              StatusOrdemDeServico.CANCELADA,
              EnumSet.noneOf(StatusOrdemDeServico.class)));

  @Mock private OrdemDeServicoRepository ordemServicoRepository;
  @Mock private OficinaService oficinaService;
  @Mock private UnidadeService unidadeService;
  @Mock private VeiculoService veiculoService;
  @Mock private ClienteService clienteService;
  @Mock private MecanicoService mecanicoService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private OrdemDeServicoServiceImpl service;

  private Oficina oficina;

  @BeforeEach
  void setUp() {
    oficina = new Oficina();
    oficina.setId(OFICINA_ID);

    when(ordemServicoRepository.save(any(OrdemDeServico.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private OrdemDeServico osComStatus(StatusOrdemDeServico status) {
    Unidade unidade = new Unidade();
    unidade.setId(5L);

    Veiculo veiculo = new Veiculo();
    veiculo.setId(7L);

    OrdemDeServico os = new OrdemDeServico();
    os.setId(OS_ID);
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);
    os.setStatus(status);
    os.setDataAbertura(LocalDateTime.now().minusDays(1));
    os.setValorTotal(new BigDecimal("100.00"));
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(new BigDecimal("100.00"));

    when(ordemServicoRepository.findById(OS_ID)).thenReturn(Optional.of(os));
    return os;
  }

  private void logadoComo(Role role) {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setUsername("usuario");
    usuario.setRole(role);
    if (role != Role.ADMIN) {
      usuario.setOficina(oficina);
    }
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(usuario);
  }

  private void pagamentoComStatus(StatusPagamento status, BigDecimal valorPago) {
    when(pagamentoService.buscarPorOsId(OS_ID))
        .thenReturn(
            new PagamentoResponseDTO(50L, OS_ID, valorPago, "", LocalDateTime.now(), status));
  }

  // ------------------------------------------------------------------
  // Transições válidas
  // ------------------------------------------------------------------

  static Stream<Arguments> transicoesValidas() {
    return TRANSICOES_VALIDAS.entrySet().stream()
        .flatMap(
            entrada ->
                entrada.getValue().stream()
                    // ENTREGUE → FECHADA depende do pagamento e é testado separadamente.
                    .filter(destino -> destino != StatusOrdemDeServico.FECHADA)
                    .map(destino -> Arguments.of(entrada.getKey(), destino)));
  }

  @ParameterizedTest(name = "{0} → {1} deve ser permitida")
  @MethodSource("transicoesValidas")
  @DisplayName("deve permitir todas as transições previstas na regra de negócio")
  void devePermitirTransicoesValidas(StatusOrdemDeServico origem, StatusOrdemDeServico destino) {
    osComStatus(origem);
    logadoComo(Role.GERENTE);

    var resposta = service.atualizarStatus(OS_ID, new AtualizarStatusOSRequestDTO(destino));

    assertThat(resposta.status())
        .as("a transição %s → %s deveria ser aceita", origem, destino)
        .isEqualTo(destino);
  }

  // ------------------------------------------------------------------
  // Transições inválidas
  // ------------------------------------------------------------------

  static Stream<Arguments> transicoesInvalidas() {
    return Stream.of(StatusOrdemDeServico.values())
        .filter(origem -> origem != StatusOrdemDeServico.CANCELADA)
        .flatMap(
            origem ->
                Stream.of(StatusOrdemDeServico.values())
                    .filter(destino -> destino != origem)
                    .filter(destino -> destino != StatusOrdemDeServico.FECHADA)
                    .filter(destino -> !TRANSICOES_VALIDAS.get(origem).contains(destino))
                    .map(destino -> Arguments.of(origem, destino)));
  }

  @ParameterizedTest(name = "{0} → {1} deve ser rejeitada")
  @MethodSource("transicoesInvalidas")
  @DisplayName("deve rejeitar toda transição fora do grafo com IllegalStateException")
  void deveRejeitarTransicoesInvalidas(StatusOrdemDeServico origem, StatusOrdemDeServico destino) {
    osComStatus(origem);
    logadoComo(Role.GERENTE);

    assertThatThrownBy(
            () -> service.atualizarStatus(OS_ID, new AtualizarStatusOSRequestDTO(destino)))
        .as("a transição %s → %s deveria ser rejeitada", origem, destino)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Transição de status não permitida");

    verify(ordemServicoRepository, never()).save(any());
  }

  // ------------------------------------------------------------------
  // CANCELADA é terminal
  // ------------------------------------------------------------------

  @ParameterizedTest
  @EnumSource(StatusOrdemDeServico.class)
  @DisplayName("OS cancelada é estado terminal: qualquer alteração lança OSCanceledException")
  void osCanceladaNaoAceitaNenhumaAlteracaoDeStatus(StatusOrdemDeServico destino) {
    osComStatus(StatusOrdemDeServico.CANCELADA);
    logadoComo(Role.GERENTE);

    assertThatThrownBy(
            () -> service.atualizarStatus(OS_ID, new AtualizarStatusOSRequestDTO(destino)))
        .isInstanceOf(OSCanceledException.class);

    verify(ordemServicoRepository, never()).save(any());
  }

  // ------------------------------------------------------------------
  // Permissões do MECANICO
  // ------------------------------------------------------------------

  @ParameterizedTest
  @EnumSource(
      value = StatusOrdemDeServico.class,
      names = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
  @DisplayName("MECANICO não pode finalizar, entregar nem cancelar uma OS")
  void mecanicoNaoPodeFinalizarEntregarNemCancelar(StatusOrdemDeServico destino) {
    // EM_EXECUCAO permite FINALIZADA e CANCELADA pelo grafo; a barreira aqui é a role.
    osComStatus(StatusOrdemDeServico.EM_EXECUCAO);
    logadoComo(Role.MECANICO);

    assertThatThrownBy(
            () -> service.atualizarStatus(OS_ID, new AtualizarStatusOSRequestDTO(destino)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("mecânico não possui permissão");

    verify(ordemServicoRepository, never()).save(any());
  }

  @Test
  @DisplayName("MECANICO pode avançar a OS nos status operacionais")
  void mecanicoPodeAvancarStatusOperacional() {
    osComStatus(StatusOrdemDeServico.AGUARDANDO_PECAS);
    logadoComo(Role.MECANICO);

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.EM_EXECUCAO));

    assertThat(resposta.status()).isEqualTo(StatusOrdemDeServico.EM_EXECUCAO);
  }

  // ------------------------------------------------------------------
  // FECHADA exige pagamento integral
  // ------------------------------------------------------------------

  @Test
  @DisplayName("ENTREGUE → FECHADA é permitida quando o pagamento está integralmente pago")
  void devePermitirFechamentoQuandoPagamentoIntegral() {
    osComStatus(StatusOrdemDeServico.ENTREGUE);
    logadoComo(Role.GERENTE);
    pagamentoComStatus(StatusPagamento.PAGA, new BigDecimal("100.00"));

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FECHADA));

    assertThat(resposta.status()).isEqualTo(StatusOrdemDeServico.FECHADA);
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusPagamento.class,
      names = {"PAGAMENTO_PENDENTE", "PAGO_PARCIALMENTE"})
  @DisplayName("não deve fechar a OS enquanto o pagamento não estiver integral")
  void naoDeveFecharOsComPagamentoPendente(StatusPagamento statusPagamento) {
    osComStatus(StatusOrdemDeServico.ENTREGUE);
    logadoComo(Role.GERENTE);
    pagamentoComStatus(statusPagamento, new BigDecimal("40.00"));

    assertThatThrownBy(
            () ->
                service.atualizarStatus(
                    OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FECHADA)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("só pode ser fechada após o pagamento integral");

    verify(ordemServicoRepository, never()).save(any());
  }

  @Test
  @DisplayName("a checagem de pagamento ocorre antes da validação do grafo de transição")
  void checagemDePagamentoPrecedeValidacaoDeTransicao() {
    // ABERTA → FECHADA é inválida no grafo, mas a regra de pagamento é avaliada primeiro.
    osComStatus(StatusOrdemDeServico.ABERTA);
    logadoComo(Role.GERENTE);
    pagamentoComStatus(StatusPagamento.PAGAMENTO_PENDENTE, BigDecimal.ZERO);

    assertThatThrownBy(
            () ->
                service.atualizarStatus(
                    OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FECHADA)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("pagamento integral");
  }

  // ------------------------------------------------------------------
  // Idempotência
  // ------------------------------------------------------------------

  @ParameterizedTest
  @EnumSource(
      value = StatusOrdemDeServico.class,
      names = {"CANCELADA", "FECHADA"},
      mode = EnumSource.Mode.EXCLUDE)
  @DisplayName("repetir o status atual é permitido e não quebra a máquina de estados")
  void repetirStatusAtualEhPermitido(StatusOrdemDeServico status) {
    osComStatus(status);
    logadoComo(Role.GERENTE);

    var resposta = service.atualizarStatus(OS_ID, new AtualizarStatusOSRequestDTO(status));

    assertThat(resposta.status()).isEqualTo(status);
  }

  // ------------------------------------------------------------------
  // dataFechamento
  // ------------------------------------------------------------------

  @Test
  @DisplayName("deve registrar dataFechamento ao finalizar a OS")
  void deveRegistrarDataFechamentoAoFinalizar() {
    OrdemDeServico os = osComStatus(StatusOrdemDeServico.EM_EXECUCAO);
    os.setDataFechamento(null);
    logadoComo(Role.GERENTE);

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FINALIZADA));

    assertThat(resposta.dataFechamento()).isNotNull();
  }

  @Test
  @DisplayName("deve preservar a dataFechamento original ao passar de FINALIZADA para ENTREGUE")
  void devePreservarDataFechamentoAoEntregar() {
    LocalDateTime fechamentoOriginal = LocalDateTime.now().minusDays(3);
    OrdemDeServico os = osComStatus(StatusOrdemDeServico.FINALIZADA);
    os.setDataFechamento(fechamentoOriginal);
    logadoComo(Role.GERENTE);

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.ENTREGUE));

    assertThat(resposta.dataFechamento()).isEqualTo(fechamentoOriginal);
  }

  @Test
  @DisplayName("deve limpar a dataFechamento ao reabrir uma OS finalizada")
  void deveLimparDataFechamentoAoReabrir() {
    OrdemDeServico os = osComStatus(StatusOrdemDeServico.FINALIZADA);
    os.setDataFechamento(LocalDateTime.now().minusDays(2));
    logadoComo(Role.GERENTE);

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.ABERTA));

    assertThat(resposta.dataFechamento())
        .as("reabrir a OS deve apagar a data de fechamento anterior")
        .isNull();
  }

  @Test
  @DisplayName("deve preservar a dataFechamento ao fechar uma OS entregue")
  void devePreservarDataFechamentoAoFechar() {
    LocalDateTime fechamentoOriginal = LocalDateTime.now().minusDays(1);
    OrdemDeServico os = osComStatus(StatusOrdemDeServico.ENTREGUE);
    os.setDataFechamento(fechamentoOriginal);
    logadoComo(Role.GERENTE);
    pagamentoComStatus(StatusPagamento.PAGA, new BigDecimal("100.00"));

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FECHADA));

    assertThat(resposta.dataFechamento())
        .as(
            "FECHADA é status de conclusão: apagar a data aqui faria a OS desaparecer"
                + " do relatório de fluxo mensal, que conta concluídas por dataFechamento.")
        .isEqualTo(fechamentoOriginal);
  }

  @Test
  @DisplayName("deve registrar dataFechamento ao fechar uma OS que ainda não tinha data")
  void deveRegistrarDataFechamentoAoFecharSemDataPrevia() {
    OrdemDeServico os = osComStatus(StatusOrdemDeServico.ENTREGUE);
    os.setDataFechamento(null);
    logadoComo(Role.GERENTE);
    pagamentoComStatus(StatusPagamento.PAGA, new BigDecimal("100.00"));

    var resposta =
        service.atualizarStatus(
            OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.FECHADA));

    assertThat(resposta.dataFechamento()).isNotNull();
  }

  // ------------------------------------------------------------------
  // Isolamento por oficina
  // ------------------------------------------------------------------

  @Test
  @DisplayName("deve validar o acesso à oficina do registro antes de alterar o status")
  void deveValidarAcessoAoRegistroAntesDeAlterarStatus() {
    osComStatus(StatusOrdemDeServico.ABERTA);
    logadoComo(Role.GERENTE);

    service.atualizarStatus(
        OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.DIAGNOSTICO));

    verify(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(OFICINA_ID), any(RuntimeException.class));
  }

  @Test
  @DisplayName("deve propagar a exceção do validador quando a OS é de outra oficina")
  void devePropagarNegacaoDeAcessoDeOutraOficina() {
    osComStatus(StatusOrdemDeServico.ABERTA);
    logadoComo(Role.GERENTE);
    doThrow(new AccessDeniedException("outra oficina"))
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(any(), any(RuntimeException.class));

    assertThatThrownBy(
            () ->
                service.atualizarStatus(
                    OS_ID, new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.DIAGNOSTICO)))
        .isInstanceOf(AccessDeniedException.class);
  }

  // ------------------------------------------------------------------
  // Sanidade do enum
  // ------------------------------------------------------------------

  @Test
  @DisplayName("o grafo de teste cobre todos os status declarados no enum")
  void grafoDeTesteCobreTodosOsStatus() {
    assertThat(TRANSICOES_VALIDAS.keySet())
        .as(
            "Se um status novo for adicionado ao enum, o grafo deste teste precisa"
                + " ser atualizado junto — caso contrário a cobertura fica silenciosamente"
                + " incompleta.")
        .containsExactlyInAnyOrder(StatusOrdemDeServico.values());
  }

  @Test
  @DisplayName("nenhuma transição válida aponta para a própria origem")
  void nenhumaTransicaoValidaAutorreferencia() {
    List<StatusOrdemDeServico> autorreferencias =
        TRANSICOES_VALIDAS.entrySet().stream()
            .filter(e -> e.getValue().contains(e.getKey()))
            .map(Map.Entry::getKey)
            .toList();

    assertThat(autorreferencias)
        .as("auto-transição é tratada como no-op, não deve estar no grafo")
        .isEmpty();
  }
}
