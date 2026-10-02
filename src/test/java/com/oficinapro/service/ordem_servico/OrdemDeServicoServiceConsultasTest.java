package com.oficinapro.service.ordem_servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.ordemDeServico.FluxoMensalOSResponseDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Unidade;
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
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Complementa {@link OrdemDeServicoServiceTest}: consultas por entidade relacionada (com filtro de
 * escopo por oficina), criação/atualização com relações opcionais e relatório de fluxo mensal.
 */
@ExtendWith(MockitoExtension.class)
class OrdemDeServicoServiceConsultasTest {

  @Mock private OrdemDeServicoRepository ordemServicoRepository;
  @Mock private OficinaService oficinaService;
  @Mock private UnidadeService unidadeService;
  @Mock private VeiculoService veiculoService;
  @Mock private ClienteService clienteService;
  @Mock private MecanicoService mecanicoService;
  @Mock private PagamentoService pagamentoService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;
  @Mock private OrdemDeServicoPdfService ordemDeServicoPdfService;

  @InjectMocks private OrdemDeServicoServiceImpl service;

  private Oficina oficina;
  private Oficina outraOficina;
  private Unidade unidade;
  private Veiculo veiculo;
  private Cliente cliente;
  private Mecanico mecanico;
  private OrdemDeServico os;

  @BeforeEach
  void setUp() {
    oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999", true);
    outraOficina = new Oficina(2L, "Outra Oficina", "11222333000181", "83977776666", true);

    unidade = new Unidade(oficina, "Unidade Central", "Rua das Flores, 100", "83911112222");
    ReflectionTestUtils.setField(unidade, "id", 1L);

    veiculo = new Veiculo();
    ReflectionTestUtils.setField(veiculo, "id", 1L);
    veiculo.setOficina(oficina);
    veiculo.setPlaca("ABC1234");

    cliente = new Cliente();
    ReflectionTestUtils.setField(cliente, "id", 1L);
    cliente.setOficina(oficina);
    cliente.setNome("JOAO SILVA");

    mecanico = new Mecanico();
    ReflectionTestUtils.setField(mecanico, "id", 1L);
    mecanico.setOficina(oficina);
    mecanico.setNome("CARLOS");

    os = novaOs(1L, oficina);
  }

  private OrdemDeServico novaOs(Long id, Oficina dona) {
    OrdemDeServico nova = new OrdemDeServico();
    ReflectionTestUtils.setField(nova, "id", id);
    nova.setOficina(dona);
    nova.setUnidade(unidade);
    nova.setVeiculo(veiculo);
    nova.setCliente(cliente);
    nova.setMecanico(mecanico);
    nova.setStatus(StatusOrdemDeServico.ABERTA);
    nova.setValorTotal(BigDecimal.ZERO);
    nova.setDesconto(BigDecimal.ZERO);
    nova.setValorComDesconto(BigDecimal.ZERO);
    nova.setDataAbertura(LocalDateTime.now());
    return nova;
  }

  // ---------------------------------------------------------------
  // listarPor{Veiculo,Mecanico,Unidade,Cliente}()
  // ---------------------------------------------------------------

  private static final Pageable PAGINA = PageRequest.of(0, 20);

  @Nested
  @DisplayName("listagens por entidade relacionada")
  class ListagensPorRelacao {

    @BeforeEach
    void escopo() {
      when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    }

    @Test
    @DisplayName("listarPorVeiculo: valida o veículo e consulta já restrito à oficina do usuário")
    void listarPorVeiculoFiltraPorOficina() {
      when(ordemServicoRepository.findByOficinaIdAndVeiculoId(eq(1L), eq(1L), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(os)));

      Page<OrdemDeServicoResponseDTO> resultado = service.listarPorVeiculo(1L, PAGINA);

      assertThat(resultado.getContent())
          .extracting(OrdemDeServicoResponseDTO::id)
          .containsExactly(1L);
      verify(veiculoService).buscarPorEntidadeId(1L);
    }

    @Test
    @DisplayName("listarPorMecanico: valida o mecânico e consulta já restrito à oficina do usuário")
    void listarPorMecanicoFiltraPorOficina() {
      when(ordemServicoRepository.findByOficinaIdAndMecanicoId(eq(1L), eq(1L), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(os)));

      Page<OrdemDeServicoResponseDTO> resultado = service.listarPorMecanico(1L, PAGINA);

      assertThat(resultado.getContent())
          .extracting(OrdemDeServicoResponseDTO::id)
          .containsExactly(1L);
      verify(mecanicoService).buscarPorEntidadeId(1L);
    }

    @Test
    @DisplayName("listarPorUnidade: valida a unidade e consulta já restrito à oficina do usuário")
    void listarPorUnidadeFiltraPorOficina() {
      when(ordemServicoRepository.findByOficinaIdAndUnidadeId(eq(1L), eq(1L), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(os)));

      Page<OrdemDeServicoResponseDTO> resultado = service.listarPorUnidade(1L, PAGINA);

      assertThat(resultado.getContent())
          .extracting(OrdemDeServicoResponseDTO::id)
          .containsExactly(1L);
      verify(unidadeService).buscarPorId(1L);
    }

    @Test
    @DisplayName("listarPorCliente: valida o cliente e consulta já restrito à oficina do usuário")
    void listarPorClienteFiltraPorOficina() {
      when(ordemServicoRepository.findByOficinaIdAndClienteId(eq(1L), eq(1L), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(os)));

      Page<OrdemDeServicoResponseDTO> resultado = service.listarPorCliente(1L, PAGINA);

      assertThat(resultado.getContent())
          .extracting(OrdemDeServicoResponseDTO::id)
          .containsExactly(1L);
      verify(clienteService).buscarPorEntidadeId(1L);
    }

    @Test
    @DisplayName("listarPorCliente: sem OS do cliente na oficina devolve página vazia")
    void listarPorClienteSemResultadosDevolveVazio() {
      when(ordemServicoRepository.findByOficinaIdAndClienteId(eq(1L), eq(1L), any(Pageable.class)))
          .thenReturn(Page.empty());

      assertThat(service.listarPorCliente(1L, PAGINA).getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("listagens por relação inexistente")
  class ListagensComRelacaoInexistente {

    @Test
    @DisplayName("listarPorVeiculo: veículo inexistente propaga o erro sem consultar OS")
    void veiculoInexistente() {
      when(veiculoService.buscarPorEntidadeId(9L)).thenThrow(new VeiculoNotFoundException(9L));

      assertThatThrownBy(() -> service.listarPorVeiculo(9L, PAGINA))
          .isInstanceOf(VeiculoNotFoundException.class);

      verifyNoInteractions(ordemServicoRepository);
    }

    @Test
    @DisplayName("listarPorMecanico: mecânico inexistente propaga o erro sem consultar OS")
    void mecanicoInexistente() {
      when(mecanicoService.buscarPorEntidadeId(9L)).thenThrow(new MecanicoNotFoundException());

      assertThatThrownBy(() -> service.listarPorMecanico(9L, PAGINA))
          .isInstanceOf(MecanicoNotFoundException.class);

      verifyNoInteractions(ordemServicoRepository);
    }

    @Test
    @DisplayName("listarPorUnidade: unidade inexistente propaga o erro sem consultar OS")
    void unidadeInexistente() {
      when(unidadeService.buscarPorId(9L)).thenThrow(new UnidadeNotFoundException(9L));

      assertThatThrownBy(() -> service.listarPorUnidade(9L, PAGINA))
          .isInstanceOf(UnidadeNotFoundException.class);

      verifyNoInteractions(ordemServicoRepository);
    }

    @Test
    @DisplayName("listarPorCliente: cliente inexistente propaga o erro sem consultar OS")
    void clienteInexistente() {
      when(clienteService.buscarPorEntidadeId(9L)).thenThrow(new ClienteNotFoundException());

      assertThatThrownBy(() -> service.listarPorCliente(9L, PAGINA))
          .isInstanceOf(ClienteNotFoundException.class);

      verifyNoInteractions(ordemServicoRepository);
    }
  }

  // ---------------------------------------------------------------
  // buscarPorEntidadeId()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("buscarPorEntidadeId: OS inexistente deve lançar OrdemDeServicoNotFoundException")
  void buscarPorEntidadeIdInexistente() {
    when(ordemServicoRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscarPorEntidadeId(99L))
        .isInstanceOf(OrdemDeServicoNotFoundException.class);

    verifyNoInteractions(oficinaAccessValidator);
  }

  @Test
  @DisplayName("buscarPorEntidadeId: OS de outra oficina deve ser tratada como não encontrada")
  void buscarPorEntidadeIdDeOutraOficina() {
    OrdemDeServico alheia = novaOs(2L, outraOficina);
    when(ordemServicoRepository.findById(2L)).thenReturn(Optional.of(alheia));
    doThrow(new OrdemDeServicoNotFoundException(2L))
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));

    assertThatThrownBy(() -> service.buscarPorId(2L))
        .isInstanceOf(OrdemDeServicoNotFoundException.class);
  }

  @Test
  @DisplayName("buscarPorEntidadeId: OS sem oficina valida o acesso com oficina nula")
  void buscarPorEntidadeIdSemOficina() {
    OrdemDeServico semOficina = novaOs(3L, null);
    when(ordemServicoRepository.findById(3L)).thenReturn(Optional.of(semOficina));

    OrdemDeServico resultado = service.buscarPorEntidadeId(3L);

    assertThat(resultado).isSameAs(semOficina);
    verify(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(null), any(OrdemDeServicoNotFoundException.class));
  }

  // ---------------------------------------------------------------
  // criar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("criar: com cliente e mecânico informados, vincula ambos e abre o pagamento")
  void criarComClienteEMecanico() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
    when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
    when(clienteService.buscarPorEntidadeId(1L)).thenReturn(cliente);
    when(mecanicoService.buscarPorEntidadeId(1L)).thenReturn(mecanico);
    when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

    OrdemDeServicoResponseDTO resultado =
        service.criar(new OrdemDeServicoRequestDTO(1L, 1L, 1L, 1L, "Revisão completa"));

    ArgumentCaptor<OrdemDeServico> captor = ArgumentCaptor.forClass(OrdemDeServico.class);
    verify(ordemServicoRepository).save(captor.capture());
    OrdemDeServico salva = captor.getValue();
    assertThat(salva.getOficina()).isSameAs(oficina);
    assertThat(salva.getCliente()).isSameAs(cliente);
    assertThat(salva.getMecanico()).isSameAs(mecanico);
    assertThat(salva.getObs()).isEqualTo("Revisão completa");
    assertThat(salva.getStatus()).isEqualTo(StatusOrdemDeServico.ABERTA);
    assertThat(salva.getDataAbertura()).isNotNull();
    assertThat(salva.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(salva.getDesconto()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(salva.getValorComDesconto()).isEqualByComparingTo(BigDecimal.ZERO);

    // o pagamento nasce junto com a OS, usando o id da OS já persistida
    verify(pagamentoService).criar(new PagamentoRequestDTO(1L, ""));
    assertThat(resultado.clienteId()).isEqualTo(1L);
    assertThat(resultado.mecanicoId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("criar: unidade inexistente deve abortar sem salvar OS nem criar pagamento")
  void criarComUnidadeInexistente() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(unidadeService.buscarPorEntidadeId(9L)).thenThrow(new UnidadeNotFoundException(9L));

    assertThatThrownBy(() -> service.criar(new OrdemDeServicoRequestDTO(9L, 1L, null, null, null)))
        .isInstanceOf(UnidadeNotFoundException.class);

    verify(ordemServicoRepository, never()).save(any());
    verifyNoInteractions(pagamentoService);
  }

  // ---------------------------------------------------------------
  // atualizar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("atualizar: com cliente e mecânico informados, troca as relações e a observação")
  void atualizarComRelacoes() {
    Cliente novoCliente = new Cliente();
    ReflectionTestUtils.setField(novoCliente, "id", 5L);
    novoCliente.setOficina(oficina);
    novoCliente.setNome("MARIA");
    Mecanico novoMecanico = new Mecanico();
    ReflectionTestUtils.setField(novoMecanico, "id", 6L);
    novoMecanico.setOficina(oficina);
    novoMecanico.setNome("PEDRO");

    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
    when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
    when(clienteService.buscarPorEntidadeId(5L)).thenReturn(novoCliente);
    when(mecanicoService.buscarPorEntidadeId(6L)).thenReturn(novoMecanico);
    when(ordemServicoRepository.save(any(OrdemDeServico.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    OrdemDeServicoResponseDTO resultado =
        service.atualizar(1L, new OrdemDeServicoRequestDTO(1L, 1L, 5L, 6L, "Nova obs"));

    assertThat(resultado.clienteId()).isEqualTo(5L);
    assertThat(resultado.nomeCliente()).isEqualTo("MARIA");
    assertThat(resultado.mecanicoId()).isEqualTo(6L);
    assertThat(resultado.mecanico()).isEqualTo("PEDRO");
    assertThat(resultado.obs()).isEqualTo("Nova obs");
  }

  @Test
  @DisplayName("atualizar: sem cliente e sem mecânico no corpo, remove os vínculos existentes")
  void atualizarSemRelacoesRemoveVinculos() {
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
    when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
    when(ordemServicoRepository.save(any(OrdemDeServico.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    OrdemDeServicoResponseDTO resultado =
        service.atualizar(1L, new OrdemDeServicoRequestDTO(1L, 1L, null, null, null));

    assertThat(resultado.clienteId()).isNull();
    assertThat(resultado.nomeCliente()).isNull();
    assertThat(resultado.mecanicoId()).isNull();
    assertThat(resultado.mecanico()).isNull();
    verify(clienteService, never()).buscarPorEntidadeId(any());
    verify(mecanicoService, never()).buscarPorEntidadeId(any());
  }

  // ---------------------------------------------------------------
  // recalcularValorTotal()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("recalcularValorTotal: desconto nulo é tratado como zero")
  void recalcularValorTotalComDescontoNulo() {
    os.setDesconto(null);
    when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
    when(ordemServicoRepository.save(any(OrdemDeServico.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    OrdemDeServicoResponseDTO resultado = service.recalcularValorTotal(1L, new BigDecimal("250"));

    assertThat(resultado.valorTotal()).isEqualByComparingTo("250");
    assertThat(resultado.valorComDesconto()).isEqualByComparingTo("250");
  }

  // ---------------------------------------------------------------
  // deletar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("deletar: OS inexistente deve lançar OrdemDeServicoNotFoundException")
  void deletarInexistente() {
    when(ordemServicoRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deletar(99L))
        .isInstanceOf(OrdemDeServicoNotFoundException.class);

    verify(ordemServicoRepository, never()).delete(any());
    verifyNoInteractions(pagamentoService);
  }

  // ---------------------------------------------------------------
  // fluxoMensal()
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("fluxoMensal")
  class FluxoMensal {

    @Test
    @DisplayName("deve gerar uma linha por dia do mês contando aberturas e finalizações")
    void deveContarAberturasEFinalizacoesPorDia() {
      OrdemDeServico abertaDia3 = novaOs(10L, oficina);
      abertaDia3.setDataAbertura(LocalDateTime.of(2024, 2, 3, 10, 0));

      OrdemDeServico abertaDia3eFechadaDia5 = novaOs(11L, oficina);
      abertaDia3eFechadaDia5.setDataAbertura(LocalDateTime.of(2024, 2, 3, 15, 30));
      abertaDia3eFechadaDia5.setDataFechamento(LocalDateTime.of(2024, 2, 5, 9, 0));

      // aberta em janeiro (veio na consulta só por ter fechado em fevereiro)
      OrdemDeServico abertaEmJaneiro = novaOs(12L, oficina);
      abertaEmJaneiro.setDataAbertura(LocalDateTime.of(2024, 1, 31, 18, 0));
      abertaEmJaneiro.setDataFechamento(LocalDateTime.of(2024, 2, 5, 11, 0));

      // sem data de abertura: não deve entrar na contagem de abertas
      OrdemDeServico semAbertura = novaOs(13L, oficina);
      semAbertura.setDataAbertura(null);

      // fechada em março (veio por ter aberto em fevereiro)
      OrdemDeServico fechadaEmMarco = novaOs(14L, oficina);
      fechadaEmMarco.setDataAbertura(LocalDateTime.of(2024, 2, 29, 8, 0));
      fechadaEmMarco.setDataFechamento(LocalDateTime.of(2024, 3, 2, 8, 0));

      // mesmo dia/mês de 2023: o ano também precisa bater
      OrdemDeServico mesmoDiaOutroAno = novaOs(15L, oficina);
      mesmoDiaOutroAno.setDataAbertura(LocalDateTime.of(2023, 2, 3, 8, 0));
      mesmoDiaOutroAno.setDataFechamento(LocalDateTime.of(2023, 2, 5, 8, 0));

      when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
      when(ordemServicoRepository.findFluxoMensal(
              1L, LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)))
          .thenReturn(
              List.of(
                  abertaDia3,
                  abertaDia3eFechadaDia5,
                  abertaEmJaneiro,
                  semAbertura,
                  fechadaEmMarco,
                  mesmoDiaOutroAno));

      List<FluxoMensalOSResponseDTO> fluxo = service.fluxoMensal(2, 2024);

      // 2024 é bissexto
      assertThat(fluxo).hasSize(29);
      assertThat(fluxo).extracting(FluxoMensalOSResponseDTO::day).startsWith(1, 2, 3).endsWith(29);

      FluxoMensalOSResponseDTO dia3 = fluxo.get(2);
      assertThat(dia3.abertas()).isEqualTo(2);
      assertThat(dia3.finalizadas()).isZero();

      FluxoMensalOSResponseDTO dia5 = fluxo.get(4);
      assertThat(dia5.abertas()).isZero();
      assertThat(dia5.finalizadas()).isEqualTo(2);

      FluxoMensalOSResponseDTO dia29 = fluxo.get(28);
      assertThat(dia29.abertas()).isEqualTo(1);
      assertThat(dia29.finalizadas()).isZero();

      assertThat(fluxo.stream().mapToLong(FluxoMensalOSResponseDTO::abertas).sum()).isEqualTo(3);
      assertThat(fluxo.stream().mapToLong(FluxoMensalOSResponseDTO::finalizadas).sum())
          .isEqualTo(2);
    }

    @Test
    @DisplayName("mês sem movimento deve devolver todos os dias com zeros")
    void mesSemMovimento() {
      when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
      when(ordemServicoRepository.findFluxoMensal(
              1L, LocalDateTime.of(2023, 2, 1, 0, 0), LocalDateTime.of(2023, 3, 1, 0, 0)))
          .thenReturn(List.of());

      List<FluxoMensalOSResponseDTO> fluxo = service.fluxoMensal(2, 2023);

      assertThat(fluxo).hasSize(28);
      assertThat(fluxo)
          .allSatisfy(
              linha -> {
                assertThat(linha.abertas()).isZero();
                assertThat(linha.finalizadas()).isZero();
              });
    }

    @Test
    @DisplayName("dezembro deve consultar até 1º de janeiro do ano seguinte")
    void dezembroUsaJaneiroSeguinteComoLimite() {
      when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
      when(ordemServicoRepository.findFluxoMensal(
              1L, LocalDateTime.of(2024, 12, 1, 0, 0), LocalDateTime.of(2025, 1, 1, 0, 0)))
          .thenReturn(List.of());

      assertThat(service.fluxoMensal(12, 2024)).hasSize(31);
    }

    @Test
    @DisplayName("mês inválido deve lançar DateTimeException sem consultar o banco")
    void mesInvalido() {
      when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);

      assertThatThrownBy(() -> service.fluxoMensal(13, 2024)).isInstanceOf(DateTimeException.class);

      verify(ordemServicoRepository, never()).findFluxoMensal(any(), any(), any());
    }
  }
}
