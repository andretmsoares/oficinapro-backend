package com.oficinapro.service.ordem_servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.MaoObra;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.repository.MaoObraRepository;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.registro_pagamento.RegistroPagamentoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoPdfServiceImplTest {

  @Mock private ItemOsPecaRepository itemOsPecaRepository;

  @Mock private MaoObraRepository maoObraRepository;

  @Mock private PagamentoService pagamentoService;

  @Mock private RegistroPagamentoService registroPagamentoService;

  @InjectMocks private OrdemDeServicoPdfServiceImpl pdfService;

  private static final Long OS_ID = 1L;
  private static final Long PAGAMENTO_ID = 10L;

  /** OS completa, com unidade, cliente e mecânico preenchidos. */
  private OrdemDeServico osCompleta() {
    Oficina oficina = new Oficina(1L, "Oficina Central", "12345678000195", "83999999999", true);

    Unidade unidade = new Unidade(oficina, "Unidade Matriz", "Rua Principal, 100", "8333334444");
    ReflectionTestUtils.setField(unidade, "id", 1L);

    Veiculo veiculo = new Veiculo();
    ReflectionTestUtils.setField(veiculo, "id", 1L);
    veiculo.setOficina(oficina);
    veiculo.setPlaca("ABC1234");
    veiculo.setMarca("Honda");
    veiculo.setModelo("Civic");
    veiculo.setAno(2020);
    veiculo.setCor("Preto");

    Cliente cliente = new Cliente();
    ReflectionTestUtils.setField(cliente, "id", 1L);
    cliente.setOficina(oficina);
    cliente.setNome("João Silva");
    cliente.setDocumento("12345678900");
    cliente.setTelefone("83988887777");

    Mecanico mecanico = new Mecanico();
    ReflectionTestUtils.setField(mecanico, "id", 1L);
    mecanico.setOficina(oficina);
    mecanico.setNome("Carlos Mecânico");

    OrdemDeServico os = new OrdemDeServico();
    ReflectionTestUtils.setField(os, "id", OS_ID);
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);
    os.setCliente(cliente);
    os.setMecanico(mecanico);
    os.setStatus(StatusOrdemDeServico.EM_EXECUCAO);
    os.setObs("Revisão completa");
    os.setDataAbertura(LocalDateTime.of(2025, 1, 10, 9, 0));
    os.setDataFechamento(LocalDateTime.of(2025, 1, 12, 18, 0));
    os.setValorTotal(new BigDecimal("500.00"));
    os.setDesconto(new BigDecimal("50.00"));
    os.setValorComDesconto(new BigDecimal("450.00"));

    return os;
  }

  /** OS mínima: sem unidade, sem cliente, sem mecânico e sem observação. */
  private OrdemDeServico osMinima() {
    Oficina oficina = new Oficina(2L, "Oficina Simples", "98765432000199", null, true);

    Veiculo veiculo = new Veiculo();
    ReflectionTestUtils.setField(veiculo, "id", 2L);
    veiculo.setOficina(oficina);
    veiculo.setPlaca("XYZ9876");
    veiculo.setMarca(null);
    veiculo.setModelo(null);
    veiculo.setAno(null);
    veiculo.setCor(null);

    OrdemDeServico os = new OrdemDeServico();
    ReflectionTestUtils.setField(os, "id", 2L);
    os.setOficina(oficina);
    os.setUnidade(null);
    os.setVeiculo(veiculo);
    os.setCliente(null);
    os.setMecanico(null);
    os.setStatus(StatusOrdemDeServico.ABERTA);
    os.setObs(null);
    os.setDataAbertura(LocalDateTime.of(2025, 2, 1, 8, 0));
    os.setDataFechamento(null);
    os.setValorTotal(BigDecimal.ZERO);
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(BigDecimal.ZERO);

    return os;
  }

  private ItemOsPeca peca() {
    ItemOsPeca peca = new ItemOsPeca();
    peca.setNome("Pastilha de freio");
    peca.setQuantidade(new BigDecimal("2"));
    peca.setValorUnitario(new BigDecimal("100.00"));
    peca.setValorTotal(new BigDecimal("200.00"));
    return peca;
  }

  private MaoObra maoObra() {
    MaoObra maoObra = new MaoObra();
    maoObra.setDescricao("Troca de pastilhas");
    maoObra.setValor(new BigDecimal("100.00"));
    return maoObra;
  }

  private PagamentoResponseDTO pagamento(String valorPago, StatusPagamento status) {
    return new PagamentoResponseDTO(
        PAGAMENTO_ID,
        OS_ID,
        new BigDecimal("450.00"),
        new BigDecimal(valorPago),
        new BigDecimal("450.00").subtract(new BigDecimal(valorPago)),
        "obs",
        null,
        status);
  }

  private RegistroPagamentoResponseDTO registroPagamento() {
    return new RegistroPagamentoResponseDTO(
        100L, PAGAMENTO_ID, new BigDecimal("450.00"), MeioPagamento.PIX, LocalDateTime.now());
  }

  private void assertEhPdfValido(byte[] pdf) {
    assertThat(pdf).isNotEmpty();

    String cabecalho = new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII);
    assertThat(cabecalho).isEqualTo("%PDF-");
  }

  @Test
  @DisplayName("gerar() deve produzir um PDF válido com peças, mão de obra e pagamentos")
  void deveGerarPdfComTodosOsDados() {
    OrdemDeServico os = osCompleta();

    when(itemOsPecaRepository.findByOrdemDeServicoIdOrderByIdAsc(OS_ID))
        .thenReturn(List.of(peca()));
    when(maoObraRepository.findByOrdemDeServicoIdOrderByIdAsc(OS_ID))
        .thenReturn(List.of(maoObra()));
    when(pagamentoService.buscarPorOsId(OS_ID))
        .thenReturn(pagamento("450.00", StatusPagamento.PAGA));
    when(registroPagamentoService.listarPorPagamento(PAGAMENTO_ID))
        .thenReturn(List.of(registroPagamento()));

    byte[] pdf = pdfService.gerar(os);

    assertEhPdfValido(pdf);
  }

  @Test
  @DisplayName("gerar() deve produzir um PDF válido mesmo sem unidade, cliente, mecânico ou itens")
  void deveGerarPdfComDadosMinimos() {
    OrdemDeServico os = osMinima();

    when(itemOsPecaRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId())).thenReturn(List.of());
    when(maoObraRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId())).thenReturn(List.of());
    when(pagamentoService.buscarPorOsId(os.getId()))
        .thenReturn(pagamento("0.00", StatusPagamento.PAGAMENTO_PENDENTE));
    when(registroPagamentoService.listarPorPagamento(PAGAMENTO_ID)).thenReturn(List.of());

    byte[] pdf = pdfService.gerar(os);

    assertEhPdfValido(pdf);
  }

  @Test
  @DisplayName(
      "gerarComprovantePagamento() deve produzir um PDF válido com os registros de pagamento")
  void deveGerarComprovanteComRegistros() {
    OrdemDeServico os = osCompleta();

    when(pagamentoService.buscarPorOsId(OS_ID))
        .thenReturn(pagamento("450.00", StatusPagamento.PAGA));
    when(registroPagamentoService.listarPorPagamento(PAGAMENTO_ID))
        .thenReturn(List.of(registroPagamento()));

    byte[] pdf = pdfService.gerarComprovantePagamento(os);

    assertEhPdfValido(pdf);
  }

  @Test
  @DisplayName("gerarComprovantePagamento() deve produzir um PDF válido sem nenhum registro")
  void deveGerarComprovanteSemRegistros() {
    OrdemDeServico os = osMinima();

    when(pagamentoService.buscarPorOsId(os.getId()))
        .thenReturn(pagamento("0.00", StatusPagamento.PAGAMENTO_PENDENTE));
    when(registroPagamentoService.listarPorPagamento(PAGAMENTO_ID)).thenReturn(List.of());

    byte[] pdf = pdfService.gerarComprovantePagamento(os);

    assertEhPdfValido(pdf);
  }
}
