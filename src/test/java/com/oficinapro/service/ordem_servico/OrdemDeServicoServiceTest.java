package com.oficinapro.service.ordem_servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.oficinapro.dto.ordemDeServico.AtribuirClienteRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtribuirMecanicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.ordem_servico.DescontoInvalidoException;
import com.oficinapro.exception.ordem_servico.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoImpossibleDeleteException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrdemDeServicoServiceTest {

  @Mock private OrdemDeServicoRepository ordemServicoRepository;

  @Mock private OficinaService oficinaService;

  // Interfaces → Mockito gera o mock diretamente
  @Mock private UnidadeService unidadeService;

  @Mock private VeiculoService veiculoService;

  @Mock private ClienteService clienteService;

  @Mock private MecanicoService mecanicoService;

  // Dependências adicionadas no refactor. Sem estes dois mocks os campos ficam
  // nulos e praticamente todo teste desta classe estoura NullPointerException:
  // - PagamentoService: criar() passou a abrir o pagamento junto com a OS;
  // - OficinaAccessValidator: o isolamento por oficina saiu do service.
  @Mock private com.oficinapro.service.pagamento.PagamentoService pagamentoService;

  @Mock private com.oficinapro.security.OficinaAccessValidator oficinaAccessValidator;

  @Mock private OrdemDeServicoPdfService ordemDeServicoPdfService;

  @InjectMocks private OrdemDeServicoServiceImpl ordemDeServicoService;

  private Oficina oficina;
  private Unidade unidade;
  private Veiculo veiculo;
  private Cliente cliente;
  private Mecanico mecanico;
  private OrdemDeServico os;

  @BeforeEach
  void setUp() {
    oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999", true);

    unidade = new Unidade(oficina, "Unidade Central", "Rua das Flores, 100", "83911112222");
    ReflectionTestUtils.setField(unidade, "id", 1L);

    veiculo = new Veiculo();
    ReflectionTestUtils.setField(veiculo, "id", 1L);
    veiculo.setOficina(oficina);
    veiculo.setModelo("Civic");
    veiculo.setAno(2020);
    veiculo.setMarca("Honda");
    veiculo.setPlaca("ABC1234");

    cliente = new Cliente();
    ReflectionTestUtils.setField(cliente, "id", 1L);
    cliente.setOficina(oficina);
    cliente.setNome("João Silva");

    mecanico = new Mecanico();
    ReflectionTestUtils.setField(mecanico, "id", 1L);
    mecanico.setOficina(oficina);
    mecanico.setNome("Carlos Mecânico");

    os = new OrdemDeServico();
    ReflectionTestUtils.setField(os, "id", 1L);
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);
    os.setCliente(cliente);
    os.setMecanico(mecanico);
    os.setStatus(StatusOrdemDeServico.ABERTA);
    os.setValorTotal(BigDecimal.ZERO);
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(BigDecimal.ZERO);
    os.setDataAbertura(LocalDateTime.now());
  }

  // ---------------------------------------------------------------
  // listar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve negar acesso à listagem de OS")
  void deveNegarListagemDeOSComoAdmin() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado())
        .thenThrow(new UsuarioAcessDeniedException());

    assertThatThrownBy(() -> ordemDeServicoService.listar())
        .isInstanceOf(UsuarioAcessDeniedException.class);

    verify(ordemServicoRepository, never()).findAll();
    verify(ordemServicoRepository, never()).findByOficinaId(anyLong());
  }

  @Test
  @DisplayName("GERENTE: deve chamar findByOficinaId e retornar apenas OS da sua oficina")
  void deveListarOSDaPropriaOficinaComoAdministrativo() {
    // Quem resolve a oficina do usuário logado agora é o OficinaAccessValidator.
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(ordemServicoRepository.findByOficinaId(1L)).thenReturn(List.of(os));

    List<OrdemDeServicoResponseDTO> resultado = ordemDeServicoService.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).oficinaId()).isEqualTo(1L);
    verify(ordemServicoRepository).findByOficinaId(1L);
    verify(ordemServicoRepository, never()).findAll();
  }

  // ---------------------------------------------------------------
  // buscarPorId()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve buscar OS por ID e retornar o DTO correto")
  void deveBuscarOSPorIdComoAdmin() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

    OrdemDeServicoResponseDTO resultado = ordemDeServicoService.buscarPorId(1L);

    assertThat(resultado).isNotNull();
    assertThat(resultado.status()).isEqualTo(StatusOrdemDeServico.ABERTA);
    assertThat(resultado.oficinaId()).isEqualTo(1L);
    assertThat(resultado.veiculoId()).isEqualTo(1L);
  }

  // ---------------------------------------------------------------
  // criar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve criar OS com sucesso sem cliente e sem mecânico opcionais")
  void deveCriarOSComSucessoComoAdmin() {
    OrdemDeServicoRequestDTO request =
        new OrdemDeServicoRequestDTO(1L, 1L, null, null, "Troca de óleo");

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
    when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
    when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

    OrdemDeServicoResponseDTO resultado = ordemDeServicoService.criar(request);

    assertThat(resultado).isNotNull();
    assertThat(resultado.status()).isEqualTo(StatusOrdemDeServico.ABERTA);
    assertThat(resultado.valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(ordemServicoRepository).save(any(OrdemDeServico.class));
    // clienteId e mecanicoId são null → serviços opcionais não devem ser chamados
    verify(clienteService, never()).buscarPorEntidadeId(anyLong());
    verify(mecanicoService, never()).buscarPorEntidadeId(anyLong());
  }

  @Test
  @DisplayName("deve atribuir mecânico à OS com sucesso")
  void deveAtribuirMecanicoAOSComSucesso() {
    AtribuirMecanicoRequestDTO dto = new AtribuirMecanicoRequestDTO(1L);

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(mecanicoService.buscarPorEntidadeId(1L)).thenReturn(mecanico);
    when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

    OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atribuirMecanico(1L, dto);

    assertThat(resultado).isNotNull();
    verify(mecanicoService).buscarPorEntidadeId(1L);
    verify(ordemServicoRepository).save(any(OrdemDeServico.class));
  }

  // ---------------------------------------------------------------
  // atribuirCliente()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("deve atribuir cliente à OS com sucesso")
  void deveAtribuirClienteAOSComSucesso() {
    AtribuirClienteRequestDTO dto = new AtribuirClienteRequestDTO(1L);

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(clienteService.buscarPorEntidadeId(1L)).thenReturn(cliente);
    when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

    OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atribuirCliente(1L, dto);

    assertThat(resultado).isNotNull();
    verify(clienteService).buscarPorEntidadeId(1L);
    verify(ordemServicoRepository).save(any(OrdemDeServico.class));
  }

  // ---------------------------------------------------------------
  // atualizar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("deve lançar OSIsNotPossibleSwapWorkshopException ao tentar trocar a oficina da OS")
  void deveLancarExcecaoAoTentarTrocarOficinaDeOS() {
    // OS pertence à oficina 1, request tenta mover para oficina 2
    OrdemDeServicoRequestDTO request = new OrdemDeServicoRequestDTO(1L, 1L, null, null, null);

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(2L);
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

    assertThatThrownBy(() -> ordemDeServicoService.atualizar(1L, request))
        .isInstanceOf(OSIsNotPossibleSwapWorkshopException.class);

    verify(ordemServicoRepository, never()).save(any());
  }

  // ---------------------------------------------------------------
  // listarPorStatus()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("listarPorStatus() deve devolver apenas as OS da oficina do usuário logado")
  void deveListarPorStatusApenasDaOficinaDoUsuario() {
    Oficina outraOficina = new Oficina(2L, "Outra Oficina", "98765432000110", "83888888888", true);
    OrdemDeServico osDeOutraOficina = new OrdemDeServico();
    ReflectionTestUtils.setField(osDeOutraOficina, "id", 2L);
    osDeOutraOficina.setOficina(outraOficina);
    osDeOutraOficina.setStatus(StatusOrdemDeServico.ABERTA);

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(ordemServicoRepository.findByStatus(StatusOrdemDeServico.ABERTA))
        .thenReturn(List.of(os, osDeOutraOficina));

    List<OrdemDeServicoResponseDTO> resultado =
        ordemDeServicoService.listarPorStatus(StatusOrdemDeServico.ABERTA);

    assertThat(resultado).extracting(OrdemDeServicoResponseDTO::id).containsExactly(1L);
  }

  // ---------------------------------------------------------------
  // aplicarDesconto()
  // ---------------------------------------------------------------

  private void prepararOsComTotal(String valorTotal, String desconto) {
    os.setValorTotal(new BigDecimal(valorTotal));
    os.setDesconto(new BigDecimal(desconto));
    os.setValorComDesconto(new BigDecimal(valorTotal).subtract(new BigDecimal(desconto)));

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
  }

  private void salvarDevolvendoOMesmoObjeto() {
    when(ordemServicoRepository.save(any(OrdemDeServico.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("aplicarDesconto() deve subtrair o desconto do valor total")
  void deveAplicarDescontoSobreOValorTotal() {
    prepararOsComTotal("500.00", "0.00");
    salvarDevolvendoOMesmoObjeto();

    OrdemDeServicoResponseDTO resultado =
        ordemDeServicoService.aplicarDesconto(1L, new BigDecimal("120.00"));

    assertThat(resultado.valorTotal()).isEqualByComparingTo("500.00");
    assertThat(resultado.desconto()).isEqualByComparingTo("120.00");
    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("380.00");
  }

  @Test
  @DisplayName("aplicarDesconto() deve tratar desconto nulo como zero")
  void deveTratarDescontoNuloComoZero() {
    prepararOsComTotal("500.00", "100.00");
    salvarDevolvendoOMesmoObjeto();

    OrdemDeServicoResponseDTO resultado = ordemDeServicoService.aplicarDesconto(1L, null);

    assertThat(resultado.desconto()).isEqualByComparingTo("0.00");
    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("500.00");
  }

  @Test
  @DisplayName("aplicarDesconto() deve aceitar desconto igual ao total, zerando o valor a pagar")
  void deveAceitarDescontoIgualAoValorTotal() {
    prepararOsComTotal("500.00", "0.00");
    salvarDevolvendoOMesmoObjeto();

    OrdemDeServicoResponseDTO resultado =
        ordemDeServicoService.aplicarDesconto(1L, new BigDecimal("500.00"));

    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("aplicarDesconto() deve recusar desconto negativo")
  void deveRecusarDescontoNegativo() {
    prepararOsComTotal("500.00", "0.00");

    assertThatThrownBy(() -> ordemDeServicoService.aplicarDesconto(1L, new BigDecimal("-0.01")))
        .isInstanceOf(DescontoInvalidoException.class);

    verify(ordemServicoRepository, never()).save(any());
  }

  @Test
  @DisplayName("aplicarDesconto() deve recusar desconto maior que o valor total")
  void deveRecusarDescontoMaiorQueOValorTotal() {
    prepararOsComTotal("500.00", "0.00");

    assertThatThrownBy(() -> ordemDeServicoService.aplicarDesconto(1L, new BigDecimal("500.01")))
        .isInstanceOf(DescontoInvalidoException.class);

    verify(ordemServicoRepository, never()).save(any());
  }

  // ---------------------------------------------------------------
  // recalcularValorTotal()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("recalcularValorTotal() deve manter o desconto quando ele ainda cabe no novo total")
  void deveManterDescontoQuandoCabeNoNovoTotal() {
    prepararOsComTotal("300.00", "50.00");
    salvarDevolvendoOMesmoObjeto();

    OrdemDeServicoResponseDTO resultado =
        ordemDeServicoService.recalcularValorTotal(1L, new BigDecimal("200.00"));

    assertThat(resultado.valorTotal()).isEqualByComparingTo("200.00");
    assertThat(resultado.desconto()).isEqualByComparingTo("50.00");
    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("150.00");
  }

  @Test
  @DisplayName("recalcularValorTotal() deve travar o desconto no novo total, sem valor negativo")
  void deveTravarDescontoNoNovoTotalQuandoOTotalDiminui() {
    prepararOsComTotal("500.00", "100.00");
    salvarDevolvendoOMesmoObjeto();

    OrdemDeServicoResponseDTO resultado =
        ordemDeServicoService.recalcularValorTotal(1L, new BigDecimal("80.00"));

    assertThat(resultado.desconto()).isEqualByComparingTo("80.00");
    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("0.00");
  }

  // ---------------------------------------------------------------
  // deletar()
  // ---------------------------------------------------------------

  /** Pagamento da OS 1 com o valor recebido informado. */
  private com.oficinapro.dto.pagamento.PagamentoResponseDTO pagamentoCom(String valorPago) {
    return new com.oficinapro.dto.pagamento.PagamentoResponseDTO(
        50L,
        1L,
        new BigDecimal(valorPago),
        new BigDecimal(valorPago),
        new BigDecimal(valorPago),
        "",
        null,
        com.oficinapro.enums.StatusPagamento.PAGAMENTO_PENDENTE);
  }

  @Test
  @DisplayName("deve deletar OS que ainda não recebeu nenhum pagamento")
  void deveDeletarOSSemPagamentoRecebido() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(pagamentoService.buscarPorOsId(1L)).thenReturn(pagamentoCom("0.00"));

    ordemDeServicoService.deletar(1L);

    verify(ordemServicoRepository).delete(os);
  }

  @Test
  @DisplayName("não deve deletar OS que já recebeu pagamento")
  void naoDeveDeletarOSComPagamentoRecebido() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(pagamentoService.buscarPorOsId(1L)).thenReturn(pagamentoCom("500.00"));

    assertThatThrownBy(() -> ordemDeServicoService.deletar(1L))
        .as(
            "Excluir a OS apagava em cascata o pagamento e todos os registros de"
                + " recebimento. Uma OS quitada de R$ 5.000 sumia sem deixar rastro,"
                + " justamente num sistema sem auditoria.")
        .isInstanceOf(OrdemDeServicoImpossibleDeleteException.class);

    verify(ordemServicoRepository, never()).delete(any());
  }

  @Test
  @DisplayName("não deve deletar OS com pagamento parcial, por menor que seja")
  void naoDeveDeletarOSComPagamentoParcialMinimo() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(pagamentoService.buscarPorOsId(1L)).thenReturn(pagamentoCom("0.01"));

    assertThatThrownBy(() -> ordemDeServicoService.deletar(1L))
        .as("o limite é qualquer valor acima de zero, não um patamar mínimo")
        .isInstanceOf(OrdemDeServicoImpossibleDeleteException.class);

    verify(ordemServicoRepository, never()).delete(any());
  }

  @Test
  @DisplayName("deve validar o acesso à oficina antes de considerar a exclusão")
  void deveValidarAcessoAntesDeExcluir() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    doThrow(new OrdemDeServicoNotFoundException(1L))
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(any(), any(RuntimeException.class));

    assertThatThrownBy(() -> ordemDeServicoService.deletar(1L))
        .isInstanceOf(OrdemDeServicoNotFoundException.class);

    verify(pagamentoService, never()).buscarPorOsId(any());
    verify(ordemServicoRepository, never()).delete(any());
  }

  // ---------------------------------------------------------------
  // gerarPdf() e gerarComprovantePagamento()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("gerarPdf() deve buscar a OS pelo id e delegar a geração ao serviço de PDF")
  void deveGerarPdfDelegandoAoServicoDePdf() {
    byte[] pdfEsperado = "%PDF-1.5".getBytes();

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(ordemDeServicoPdfService.gerar(os)).thenReturn(pdfEsperado);

    byte[] resultado = ordemDeServicoService.gerarPdf(1L);

    assertThat(resultado).isEqualTo(pdfEsperado);
    verify(ordemDeServicoPdfService).gerar(os);
  }

  @Test
  @DisplayName("gerarComprovantePagamento() deve buscar a OS pelo id e delegar ao serviço de PDF")
  void deveGerarComprovantePagamentoDelegandoAoServicoDePdf() {
    byte[] pdfEsperado = "%PDF-1.5".getBytes();

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(ordemDeServicoPdfService.gerarComprovantePagamento(os)).thenReturn(pdfEsperado);

    byte[] resultado = ordemDeServicoService.gerarComprovantePagamento(1L);

    assertThat(resultado).isEqualTo(pdfEsperado);
    verify(ordemDeServicoPdfService).gerarComprovantePagamento(os);
  }

  @Test
  @DisplayName("gerarPdf() deve propagar 'OS não encontrada' quando o id não existe")
  void deveGerarPdfPropagarOsInexistente() {
    when(ordemServicoRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> ordemDeServicoService.gerarPdf(99L))
        .isInstanceOf(OrdemDeServicoNotFoundException.class);

    verify(ordemDeServicoPdfService, never()).gerar(any());
  }
}
