package com.oficinapro.service.ordem_servico;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.model.*;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.repository.MaoObraRepository;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.registro_pagamento.RegistroPagamentoService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrdemDeServicoPdfServiceImpl implements OrdemDeServicoPdfService {

  private static final Logger log = LoggerFactory.getLogger(OrdemDeServicoPdfServiceImpl.class);

  private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static final Font FONT_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
  private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
  private static final Font FONT_LABEL = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
  private static final Font FONT_TEXTO = new Font(Font.HELVETICA, 10, Font.NORMAL);
  private static final Font FONT_HEADER_TABELA =
      new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
  private static final Font FONT_HEADER_SECAO =
      new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
  private static final Font FONT_RESUMO = new Font(Font.HELVETICA, 11, Font.BOLD);

  // Paleta alinhada ao card de pagamento do modal do frontend (border #e5e7eb,
  // fundo branco), para que o PDF pareça uma extensão da mesma tela.
  private static final Color COR_HEADER_SECAO = new Color(51, 51, 51);
  private static final Color COR_BORDA_SECAO = new Color(229, 231, 235);
  private static final Color COR_FUNDO_RESUMO = new Color(240, 240, 240);

  private final ItemOsPecaRepository itemOsPecaRepository;
  private final MaoObraRepository maoObraRepository;
  private final PagamentoService pagamentoService;
  private final RegistroPagamentoService registroPagamentoService;

  public OrdemDeServicoPdfServiceImpl(
      ItemOsPecaRepository itemOsPecaRepository,
      MaoObraRepository maoObraRepository,
      PagamentoService pagamentoService,
      RegistroPagamentoService registroPagamentoService) {
    this.itemOsPecaRepository = itemOsPecaRepository;
    this.maoObraRepository = maoObraRepository;
    this.pagamentoService = pagamentoService;
    this.registroPagamentoService = registroPagamentoService;
  }

  @Override
  public byte[] gerar(OrdemDeServico os) {
    try {
      Document document = new Document(PageSize.A4, 36, 36, 54, 54);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(new CabecalhoRepeticaoEvent(os, "OS #" + os.getId()));

      document.open();
      montarCabecalhoPrincipal(document, os);
      montarSecaoIdentificacao(document, os);
      montarSecaoServicos(document, os);
      montarSecaoPagamento(document, os);
      document.close();

      return out.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException("Falha ao gerar PDF da OS " + os.getId(), e);
    }
  }

  @Override
  public byte[] gerarComprovantePagamento(OrdemDeServico os) {
    try {
      Document document = new Document(PageSize.A4, 36, 36, 54, 54);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(
          new CabecalhoRepeticaoEvent(os, "Comprovante de pagamento — OS #" + os.getId()));

      document.open();
      montarCabecalhoComprovante(document, os);
      montarSecaoResumoOs(document, os);
      montarSecaoComprovantePagamento(document, os);
      document.close();

      return out.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException(
          "Falha ao gerar comprovante de pagamento da OS " + os.getId(), e);
    }
  }

  // ---------- CABEÇALHOS ----------

  private void montarCabecalhoPrincipal(Document document, OrdemDeServico os)
      throws DocumentException {
    Paragraph titulo = new Paragraph("ORDEM DE SERVIÇO #" + os.getId(), FONT_TITULO);
    titulo.setSpacingAfter(4f);
    document.add(titulo);

    Paragraph status =
        new Paragraph(
            "Status: "
                + os.getStatus()
                + (os.getDataAbertura() != null
                    ? " | Abertura: " + os.getDataAbertura().format(DATA_FMT)
                    : "")
                + (os.getDataFechamento() != null
                    ? " | Fechamento: " + os.getDataFechamento().format(DATA_FMT)
                    : ""),
            FONT_TEXTO);
    status.setSpacingAfter(12f);
    document.add(status);
  }

  private void montarCabecalhoComprovante(Document document, OrdemDeServico os)
      throws DocumentException {
    Paragraph titulo = new Paragraph("COMPROVANTE DE PAGAMENTO", FONT_TITULO);
    titulo.setSpacingAfter(4f);
    document.add(titulo);

    Paragraph subtitulo =
        new Paragraph(
            "Ordem de serviço #"
                + os.getId()
                + (os.getDataAbertura() != null
                    ? " | Abertura: " + os.getDataAbertura().format(DATA_FMT)
                    : ""),
            FONT_TEXTO);
    subtitulo.setSpacingAfter(12f);
    document.add(subtitulo);
  }

  // ---------- SEÇÃO — IDENTIFICAÇÃO ----------

  private void montarSecaoIdentificacao(Document document, OrdemDeServico os)
      throws DocumentException {
    Oficina oficina = os.getOficina();
    Unidade unidade = os.getUnidade();
    Veiculo veiculo = os.getVeiculo();
    Cliente cliente = os.getCliente();
    Mecanico mecanico = os.getMecanico();

    PdfPCell conteudo = novaCelulaConteudo();

    PdfPTable topo = new PdfPTable(2);
    topo.setWidthPercentage(100);
    topo.setWidths(new float[] {1f, 1f});

    PdfPCell celulaOficina = celulaSemBorda();
    celulaOficina.addElement(new Paragraph(nullToDash(oficina.getNome()), FONT_SUBTITULO));
    celulaOficina.addElement(campo("CNPJ", oficina.getCnpj()));
    celulaOficina.addElement(campo("Telefone", oficina.getTelefone()));
    topo.addCell(celulaOficina);

    PdfPCell celulaUnidade = celulaSemBorda();
    celulaUnidade.addElement(new Paragraph("Unidade", FONT_SUBTITULO));
    if (unidade != null) {
      celulaUnidade.addElement(campo("Nome", unidade.getNome()));
      celulaUnidade.addElement(campo("Endereço", unidade.getEndereco()));
      celulaUnidade.addElement(campo("Telefone", unidade.getTelefone()));
    } else {
      celulaUnidade.addElement(new Paragraph("Não informado", FONT_TEXTO));
    }
    topo.addCell(celulaUnidade);
    conteudo.addElement(topo);

    PdfPTable meio = new PdfPTable(3);
    meio.setWidthPercentage(100);
    meio.setSpacingBefore(10f);

    PdfPCell cVeiculo = celulaSemBorda();
    cVeiculo.addElement(new Paragraph("Veículo", FONT_SUBTITULO));
    cVeiculo.addElement(campo("Placa", veiculo.getPlaca()));
    cVeiculo.addElement(campo("Marca", veiculo.getMarca()));
    cVeiculo.addElement(campo("Modelo", veiculo.getModelo()));
    cVeiculo.addElement(
        campo("Ano", veiculo.getAno() != null ? String.valueOf(veiculo.getAno()) : null));
    cVeiculo.addElement(campo("Cor", veiculo.getCor()));
    meio.addCell(cVeiculo);

    PdfPCell cCliente = celulaSemBorda();
    cCliente.addElement(new Paragraph("Cliente", FONT_SUBTITULO));
    if (cliente != null) {
      cCliente.addElement(campo("Nome", cliente.getNome()));
      cCliente.addElement(campo("CPF", cliente.getDocumento()));
      cCliente.addElement(campo("Telefone", cliente.getTelefone()));
    } else {
      cCliente.addElement(new Paragraph("Não informado", FONT_TEXTO));
    }
    meio.addCell(cCliente);

    PdfPCell cMecanico = celulaSemBorda();
    cMecanico.addElement(new Paragraph("Mecânico responsável", FONT_SUBTITULO));
    cMecanico.addElement(
        new Paragraph(mecanico != null ? mecanico.getNome() : "Não informado", FONT_TEXTO));
    meio.addCell(cMecanico);

    conteudo.addElement(meio);

    adicionarSecao(document, "IDENTIFICAÇÃO", conteudo);
  }

  private void montarSecaoResumoOs(Document document, OrdemDeServico os) throws DocumentException {
    Oficina oficina = os.getOficina();
    Veiculo veiculo = os.getVeiculo();
    Cliente cliente = os.getCliente();

    PdfPCell conteudo = novaCelulaConteudo();

    PdfPTable tabela = new PdfPTable(3);
    tabela.setWidthPercentage(100);

    PdfPCell cOficina = celulaSemBorda();
    cOficina.addElement(new Paragraph("Oficina", FONT_SUBTITULO));
    cOficina.addElement(new Paragraph(nullToDash(oficina.getNome()), FONT_TEXTO));
    tabela.addCell(cOficina);

    PdfPCell cCliente = celulaSemBorda();
    cCliente.addElement(new Paragraph("Cliente", FONT_SUBTITULO));
    cCliente.addElement(
        new Paragraph(
            cliente != null ? nullToDash(cliente.getNome()) : "Não informado", FONT_TEXTO));
    tabela.addCell(cCliente);

    PdfPCell cVeiculo = celulaSemBorda();
    cVeiculo.addElement(new Paragraph("Veículo", FONT_SUBTITULO));
    cVeiculo.addElement(
        new Paragraph(
            nullToDash(veiculo.getPlaca())
                + " — "
                + nullToDash(veiculo.getMarca())
                + " "
                + nullToDash(veiculo.getModelo()),
            FONT_TEXTO));
    tabela.addCell(cVeiculo);

    conteudo.addElement(tabela);

    adicionarSecao(document, "DADOS DA ORDEM DE SERVIÇO", conteudo);
  }

  // ---------- SEÇÃO — SERVIÇOS ----------

  private void montarSecaoServicos(Document document, OrdemDeServico os) throws DocumentException {
    PdfPCell conteudo = novaCelulaConteudo();

    if (os.getObs() != null && !os.getObs().isBlank()) {
      Paragraph obs = new Paragraph(os.getObs(), FONT_TEXTO);
      obs.setSpacingAfter(10f);
      conteudo.addElement(obs);
    }

    List<ItemOsPeca> pecas = itemOsPecaRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    tabelaPecas(conteudo, pecas);

    List<MaoObra> maoObras = maoObraRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    tabelaMaoObra(conteudo, maoObras);

    // Subtotal fica alinhado ao domínio: valorTotal já é a fonte de verdade calculada
    // no fluxo de negócio (recalcularValorTotal), então usamos ele em vez de somar
    // peças + mão de obra de novo aqui, evitando duas fontes de verdade divergentes.
    Paragraph subtotalPar =
        new Paragraph(
            "Subtotal itens/serviços: " + formatarMoeda(os.getValorTotal()), FONT_SUBTITULO);
    subtotalPar.setSpacingBefore(6f);
    conteudo.addElement(subtotalPar);

    adicionarSecao(document, "SERVIÇOS DA ORDEM DE SERVIÇO", conteudo);
  }

  private void tabelaPecas(PdfPCell conteudo, List<ItemOsPeca> pecas) {
    conteudo.addElement(new Paragraph("Peças", FONT_LABEL));
    if (pecas.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhuma peça registrada.", FONT_TEXTO));
      return;
    }

    PdfPTable tabela = new PdfPTable(4);
    tabela.setWidthPercentage(100);
    tabela.setSpacingBefore(4f);
    tabela.setSpacingAfter(10f);
    tabela.setWidths(new float[] {3f, 1f, 1.3f, 1.3f});
    cabecalhoLinha(tabela, "Peça", "Qtd", "Valor unit.", "Valor total");

    for (ItemOsPeca p : pecas) {
      tabela.addCell(celulaTexto(p.getNome()));
      tabela.addCell(celulaTexto(p.getQuantidade().stripTrailingZeros().toPlainString()));
      tabela.addCell(celulaTexto(formatarMoeda(p.getValorUnitario())));
      tabela.addCell(celulaTexto(formatarMoeda(p.getValorTotal())));
    }
    conteudo.addElement(tabela);
  }

  private void tabelaMaoObra(PdfPCell conteudo, List<MaoObra> itens) {
    conteudo.addElement(new Paragraph("Mão de obra", FONT_LABEL));
    if (itens.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhum item de mão de obra registrado.", FONT_TEXTO));
      return;
    }

    PdfPTable tabela = new PdfPTable(2);
    tabela.setWidthPercentage(100);
    tabela.setSpacingBefore(4f);
    tabela.setSpacingAfter(10f);
    tabela.setWidths(new float[] {3f, 1.3f});
    cabecalhoLinha(tabela, "Descrição", "Valor");

    for (MaoObra m : itens) {
      tabela.addCell(celulaTexto(m.getDescricao()));
      BigDecimal valor = m.getValor() != null ? m.getValor() : BigDecimal.ZERO;
      tabela.addCell(celulaTexto(formatarMoeda(valor)));
    }
    conteudo.addElement(tabela);
  }

  // ---------- SEÇÃO — PAGAMENTO ----------

  private void montarSecaoPagamento(Document document, OrdemDeServico os) throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());
    List<RegistroPagamentoResponseDTO> registros =
        registroPagamentoService.listarPorPagamento(pagamento.id());

    PdfPCell conteudo = novaCelulaConteudo();

    Paragraph valores =
        new Paragraph(
            "Valor total: "
                + formatarMoeda(os.getValorTotal())
                + "   |   Desconto: "
                + formatarMoeda(os.getDesconto())
                + "   |   Valor com desconto: "
                + formatarMoeda(os.getValorComDesconto()),
            FONT_TEXTO);
    valores.setSpacingAfter(8f);
    conteudo.addElement(valores);

    conteudo.addElement(new Paragraph("Registros de pagamento", FONT_LABEL));
    tabelaRegistrosPagamento(conteudo, registros);

    conteudo.addElement(resumoFinanceiro(os, pagamento));

    adicionarSecao(document, "PAGAMENTO", conteudo);
  }

  private void montarSecaoComprovantePagamento(Document document, OrdemDeServico os)
      throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());
    List<RegistroPagamentoResponseDTO> registros =
        registroPagamentoService.listarPorPagamento(pagamento.id());

    PdfPCell conteudo = novaCelulaConteudo();
    tabelaRegistrosPagamento(conteudo, registros);
    adicionarSecao(document, "REGISTROS DE PAGAMENTO", conteudo);

    PdfPCell resumoConteudo = novaCelulaConteudo();
    resumoConteudo.addElement(resumoFinanceiro(os, pagamento));
    adicionarSecao(document, "RESUMO DO PAGAMENTO", resumoConteudo);
  }

  private void tabelaRegistrosPagamento(
      PdfPCell conteudo, List<RegistroPagamentoResponseDTO> registros) {
    if (registros.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhum registro de pagamento.", FONT_TEXTO));
      return;
    }

    PdfPTable tabela = new PdfPTable(3);
    tabela.setWidthPercentage(100);
    tabela.setSpacingBefore(4f);
    tabela.setWidths(new float[] {1.5f, 1.5f, 1f});
    cabecalhoLinha(tabela, "Data", "Forma de pagamento", "Valor");

    for (RegistroPagamentoResponseDTO r : registros) {
      tabela.addCell(celulaTexto(r.data() != null ? r.data().format(DATA_FMT) : "-"));
      tabela.addCell(celulaTexto(r.meioPagamento() != null ? r.meioPagamento().toString() : "-"));
      tabela.addCell(celulaTexto(formatarMoeda(r.valor())));
    }
    conteudo.addElement(tabela);
  }

  private PdfPTable resumoFinanceiro(OrdemDeServico os, PagamentoResponseDTO pagamento) {
    PdfPTable resumo = new PdfPTable(1);
    resumo.setWidthPercentage(100);
    resumo.setSpacingBefore(12f);

    PdfPCell resumoCell = new PdfPCell();
    resumoCell.setPadding(10f);
    resumoCell.setBackgroundColor(COR_FUNDO_RESUMO);
    resumoCell.setBorderColor(COR_BORDA_SECAO);
    resumoCell.addElement(new Paragraph("Resumo financeiro", FONT_SUBTITULO));
    resumoCell.addElement(
        new Paragraph("Valor total: " + formatarMoeda(os.getValorTotal()), FONT_RESUMO));
    resumoCell.addElement(
        new Paragraph("Desconto: " + formatarMoeda(os.getDesconto()), FONT_RESUMO));
    resumoCell.addElement(
        new Paragraph(
            "Valor com desconto: " + formatarMoeda(os.getValorComDesconto()), FONT_RESUMO));
    resumoCell.addElement(
        new Paragraph("Total pago: " + formatarMoeda(pagamento.valorPago()), FONT_RESUMO));
    resumoCell.addElement(
        new Paragraph(
            "Restante a pagar: " + formatarMoeda(pagamento.valorPendente()), FONT_RESUMO));
    resumoCell.addElement(
        new Paragraph(
            "Status do pagamento: " + (pagamento.status() != null ? pagamento.status() : "-"),
            FONT_RESUMO));
    resumo.addCell(resumoCell);
    return resumo;
  }

  // ---------- helpers de layout (cartões/seções) ----------

  /**
   * Monta uma seção em formato de "cartão": uma barra de título com fundo escuro (igual ao
   * cabeçalho das tabelas) seguida de um bloco de conteúdo com borda clara — o mesmo padrão visual
   * usado nas seções do modal de visualização da OS no frontend (título + card com borda).
   */
  private void adicionarSecao(Document document, String titulo, PdfPCell conteudo)
      throws DocumentException {
    PdfPTable secao = new PdfPTable(1);
    secao.setWidthPercentage(100);
    secao.setSpacingBefore(14f);
    secao.setKeepTogether(false);

    PdfPCell header = new PdfPCell(new Phrase(titulo, FONT_HEADER_SECAO));
    header.setBackgroundColor(COR_HEADER_SECAO);
    header.setPadding(7f);
    header.setBorder(Rectangle.NO_BORDER);
    secao.addCell(header);

    secao.addCell(conteudo);
    document.add(secao);
  }

  private PdfPCell novaCelulaConteudo() {
    PdfPCell conteudo = new PdfPCell();
    conteudo.setPadding(12f);
    conteudo.setBorderColor(COR_BORDA_SECAO);
    conteudo.setBorderWidthTop(0f);
    return conteudo;
  }

  private PdfPCell celulaSemBorda() {
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.NO_BORDER);
    return c;
  }

  // ---------- helpers ----------

  private void cabecalhoLinha(PdfPTable tabela, String... titulos) {
    for (String t : titulos) {
      PdfPCell c = new PdfPCell(new Phrase(t, FONT_HEADER_TABELA));
      c.setBackgroundColor(new Color(51, 51, 51));
      c.setPadding(5f);
      tabela.addCell(c);
    }
  }

  private PdfPCell celulaTexto(String texto) {
    PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", FONT_TEXTO));
    c.setPadding(4f);
    return c;
  }

  private Paragraph campo(String label, String valor) {
    return new Paragraph(
        label + ": " + (valor != null && !valor.isBlank() ? valor : "-"), FONT_TEXTO);
  }

  private String nullToDash(String v) {
    return v != null && !v.isBlank() ? v : "-";
  }

  private String formatarMoeda(BigDecimal valor) {
    // Valores monetarios sao persistidos em centavos; converte para reais so na exibicao.
    BigDecimal v =
        (valor != null ? valor : BigDecimal.ZERO)
            .movePointLeft(2)
            .setScale(2, RoundingMode.HALF_UP);
    java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    return nf.format(v);
  }

  private static class CabecalhoRepeticaoEvent extends PdfPageEventHelper {
    private final OrdemDeServico os;
    private final String texto;

    CabecalhoRepeticaoEvent(OrdemDeServico os, String texto) {
      this.os = os;
      this.texto = texto;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      if (writer.getPageNumber() == 1) return;
      PdfContentByte cb = writer.getDirectContent();
      Phrase header = new Phrase(os.getOficina().getNome() + " — " + texto, FONT_LABEL);
      ColumnText.showTextAligned(
          cb, Element.ALIGN_LEFT, header, document.left(), document.top() + 20, 0);
    }
  }
}
