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

  private static final Font FONT_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(31, 78, 121));
  private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD);
  private static final Font FONT_LABEL = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
  private static final Font FONT_TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL);
  private static final Font FONT_HEADER_TABELA =
      new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
  private static final Font FONT_HEADER_SECAO =
      new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
  private static final Font FONT_VALOR_LABEL = new Font(Font.HELVETICA, 9, Font.BOLD);
  private static final Font FONT_TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font FONT_RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

  // Paleta alinhada ao card de pagamento do modal do frontend (border #e5e7eb,
  // fundo branco), para que o PDF pareça uma extensão da mesma tela.
  private static final Color COR_HEADER_SECAO = new Color(31, 78, 121);
  private static final Color COR_BORDA_SECAO = new Color(229, 231, 235);
  private static final Color COR_ZEBRA = new Color(247, 248, 250);
  private static final Color COR_DESTAQUE = new Color(31, 78, 121);

  private static final float LOGO_ALTURA = 50f;

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
      Document document = new Document(PageSize.A4, 30, 30, 36, 40);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(new CabecalhoRepeticaoEvent(os, "OS #" + formatarId(os.getId())));

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
      Document document = new Document(PageSize.A4, 30, 30, 36, 40);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(
          new CabecalhoRepeticaoEvent(os, "Comprovante de pagamento — OS #" + formatarId(os.getId())));

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
    montarCabecalhoComLogo(
        document,
        "ORDEM DE SERVIÇO #" + formatarId(os.getId()),
        (os.getDataAbertura() != null ? "Abertura: " + os.getDataAbertura().format(DATA_FMT) : "")
            + (os.getDataFechamento() != null
                ? (os.getDataAbertura() != null ? " | " : "")
                    + "Fechamento: "
                    + os.getDataFechamento().format(DATA_FMT)
                : ""));
  }

  private void montarCabecalhoComprovante(Document document, OrdemDeServico os)
      throws DocumentException {
    montarCabecalhoComLogo(
        document,
        "COMPROVANTE DE PAGAMENTO",
        "Ordem de serviço #"
            + formatarId(os.getId())
            + (os.getDataAbertura() != null
                ? " | Abertura: " + os.getDataAbertura().format(DATA_FMT)
                : ""));
  }

  /**
   * Cabeçalho com título à esquerda e um slot reservado para a logo da oficina à direita. Para
   * ativar a logo, substitua o conteúdo da célula retornada por {@link #slotLogo()} por um {@link
   * Image} ajustado ao tamanho do slot.
   */
  private void montarCabecalhoComLogo(Document document, String titulo, String subtitulo)
      throws DocumentException {
    PdfPTable cab = new PdfPTable(2);
    cab.setWidthPercentage(100);
    cab.setWidths(new float[] {4f, 1f});

    PdfPCell esquerda = celulaSemBorda();
    esquerda.setVerticalAlignment(Element.ALIGN_MIDDLE);
    esquerda.addElement(new Paragraph(titulo, FONT_TITULO));
    if (!subtitulo.isBlank()) esquerda.addElement(new Paragraph(subtitulo, FONT_TEXTO));
    cab.addCell(esquerda);

    cab.addCell(slotLogo());
    document.add(cab);
  }

  private PdfPCell slotLogo() {
    PdfPCell logo = new PdfPCell(new Phrase(" ", FONT_TEXTO));
    logo.setFixedHeight(LOGO_ALTURA);
    logo.setBorderColor(COR_BORDA_SECAO);
    logo.setHorizontalAlignment(Element.ALIGN_CENTER);
    logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
    return logo;
  }

  private String formatarId(Long id) {
    return String.format("%04d", id);
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

    PdfPTable t = new PdfPTable(3);
    t.setWidthPercentage(100);

    PdfPCell cOficina = celulaSemBorda();
    cOficina.addElement(new Paragraph(nullToDash(oficina.getNome()), FONT_SUBTITULO));
    cOficina.addElement(campo("CNPJ", oficina.getCnpj()));
    cOficina.addElement(campo("Telefone", oficina.getTelefone()));
    t.addCell(cOficina);

    PdfPCell cUnidade = celulaSemBorda();
    cUnidade.addElement(new Paragraph("Unidade", FONT_SUBTITULO));
    if (unidade != null) {
      cUnidade.addElement(campo("Nome", unidade.getNome()));
      cUnidade.addElement(campo("Endereço", unidade.getEndereco()));
      cUnidade.addElement(campo("Telefone", unidade.getTelefone()));
    } else {
      cUnidade.addElement(new Paragraph("Não informado", FONT_TEXTO));
    }
    t.addCell(cUnidade);

    PdfPCell cMecanico = celulaSemBorda();
    cMecanico.addElement(new Paragraph("Mecânico responsável", FONT_SUBTITULO));
    cMecanico.addElement(
        new Paragraph(mecanico != null ? mecanico.getNome() : "Não informado", FONT_TEXTO));
    t.addCell(cMecanico);

    PdfPCell cVeiculo = celulaSemBorda();
    cVeiculo.setPaddingTop(6f);
    cVeiculo.addElement(new Paragraph("Veículo", FONT_SUBTITULO));
    cVeiculo.addElement(campo("Placa", veiculo.getPlaca()));
    cVeiculo.addElement(
        campo(
            "Modelo",
            (nullToDash(veiculo.getMarca()) + " " + nullToDash(veiculo.getModelo())).trim()));
    cVeiculo.addElement(
        campo(
            "Ano / Cor",
            (veiculo.getAno() != null ? String.valueOf(veiculo.getAno()) : "-")
                + " / "
                + nullToDash(veiculo.getCor())));
    t.addCell(cVeiculo);

    PdfPCell cCliente = celulaSemBorda();
    cCliente.setPaddingTop(6f);
    cCliente.addElement(new Paragraph("Cliente", FONT_SUBTITULO));
    if (cliente != null) {
      cCliente.addElement(campo("Nome", cliente.getNome()));
      cCliente.addElement(campo("CPF", cliente.getDocumento()));
      cCliente.addElement(campo("Telefone", cliente.getTelefone()));
    } else {
      cCliente.addElement(new Paragraph("Não informado", FONT_TEXTO));
    }
    t.addCell(cCliente);

    PdfPCell vazio = celulaSemBorda();
    t.addCell(vazio);

    conteudo.addElement(t);

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
      obs.setSpacingAfter(5f);
      conteudo.addElement(obs);
    }

    List<ItemOsPeca> pecas = itemOsPecaRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    tabelaPecas(conteudo, pecas);

    List<MaoObra> maoObras = maoObraRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    tabelaMaoObra(conteudo, maoObras);

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
    tabela.setSpacingAfter(5f);
    tabela.setWidths(new float[] {3f, 1f, 1.3f, 1.3f});
    cabecalhoLinha(tabela, "Peça", "Qtd", "Valor unit.", "Valor total");
    int idx = 0;

    for (ItemOsPeca p : pecas) {
      boolean z = idx++ % 2 == 1;
      tabela.addCell(celulaTexto(p.getNome(), Element.ALIGN_LEFT, z));
      tabela.addCell(
          celulaTexto(
              p.getQuantidade().stripTrailingZeros().toPlainString(), Element.ALIGN_CENTER, z));
      tabela.addCell(celulaTexto(formatarMoeda(p.getValorUnitario()), Element.ALIGN_RIGHT, z));
      tabela.addCell(celulaTexto(formatarMoeda(p.getValorTotal()), Element.ALIGN_RIGHT, z));
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
    tabela.setSpacingAfter(5f);
    tabela.setWidths(new float[] {3f, 1.3f});
    cabecalhoLinha(tabela, "Descrição", "Valor");

    int idx = 0;
    for (MaoObra m : itens) {
      boolean z = idx++ % 2 == 1;
      tabela.addCell(celulaTexto(m.getDescricao(), Element.ALIGN_LEFT, z));
      BigDecimal valor = m.getValor() != null ? m.getValor() : BigDecimal.ZERO;
      tabela.addCell(celulaTexto(formatarMoeda(valor), Element.ALIGN_RIGHT, z));
    }
    conteudo.addElement(tabela);
  }

  // ---------- SEÇÃO — PAGAMENTO ----------

  private void montarSecaoPagamento(Document document, OrdemDeServico os) throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

    PdfPCell conteudo = novaCelulaConteudo();

    PdfPTable valores = new PdfPTable(4);
    valores.setWidthPercentage(100);
    valores.setSpacingAfter(6f);
    caixaValor(valores, "Valor da OS", os.getValorTotal());
    caixaValor(valores, "Valor com desconto", os.getValorComDesconto());
    caixaValor(valores, "Valor pago", pagamento.valorPago());
    caixaValor(valores, "Saldo restante", pagamento.valorPendente());
    conteudo.addElement(valores);

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

    int idx = 0;
    for (RegistroPagamentoResponseDTO r : registros) {
      boolean z = idx++ % 2 == 1;
      tabela.addCell(
          celulaTexto(r.data() != null ? r.data().format(DATA_FMT) : "-", Element.ALIGN_LEFT, z));
      tabela.addCell(
          celulaTexto(
              r.meioPagamento() != null ? r.meioPagamento().toString() : "-",
              Element.ALIGN_LEFT,
              z));
      tabela.addCell(celulaTexto(formatarMoeda(r.valor()), Element.ALIGN_RIGHT, z));
    }
    conteudo.addElement(tabela);
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
    secao.setSpacingBefore(8f);
    secao.setKeepTogether(false);

    PdfPCell header = new PdfPCell(new Phrase(titulo, FONT_HEADER_SECAO));
    header.setBackgroundColor(COR_HEADER_SECAO);
    header.setPadding(4f);
    header.setBorder(Rectangle.NO_BORDER);
    secao.addCell(header);

    secao.addCell(conteudo);
    document.add(secao);
  }

  private PdfPCell novaCelulaConteudo() {
    PdfPCell conteudo = new PdfPCell();
    conteudo.setPadding(7f);
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
    for (int i = 0; i < titulos.length; i++) {
      PdfPCell c = new PdfPCell(new Phrase(titulos[i], FONT_HEADER_TABELA));
      c.setBackgroundColor(COR_DESTAQUE);
      c.setBorderColor(COR_BORDA_SECAO);
      c.setPadding(3f);
      c.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
      tabela.addCell(c);
    }
  }

  private PdfPCell celulaTexto(String texto, int alinhamento, boolean zebra) {
    PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", FONT_TEXTO));
    c.setPadding(2.5f);
    c.setBorderColor(COR_BORDA_SECAO);
    c.setHorizontalAlignment(alinhamento);
    if (zebra) c.setBackgroundColor(COR_ZEBRA);
    return c;
  }

  private void caixaValor(PdfPTable t, String label, BigDecimal valor) {
    PdfPCell c = new PdfPCell();
    c.setBorderColor(COR_BORDA_SECAO);
    c.setBackgroundColor(COR_ZEBRA);
    c.setPadding(4f);
    c.addElement(new Paragraph(label, FONT_LABEL));
    c.addElement(new Paragraph(formatarMoeda(valor), FONT_TOTAL));
    t.addCell(c);
  }

  private Paragraph campo(String label, String valor) {
    Paragraph p = new Paragraph();
    p.setSpacingAfter(2f);
    p.add(new Chunk(label + ": ", FONT_VALOR_LABEL));
    p.add(new Chunk(valor != null && !valor.isBlank() ? valor : "-", FONT_TEXTO));
    return p;
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
      PdfContentByte cb = writer.getDirectContent();
      ColumnText.showTextAligned(
          cb,
          Element.ALIGN_CENTER,
          new Phrase("Página " + writer.getPageNumber(), FONT_RODAPE),
          (document.left() + document.right()) / 2,
          document.bottom() - 20,
          0);
      if (writer.getPageNumber() == 1) return;
      Phrase header = new Phrase(os.getOficina().getNome() + " — " + texto, FONT_LABEL);
      ColumnText.showTextAligned(
          cb, Element.ALIGN_LEFT, header, document.left(), document.top() + 20, 0);
    }
  }
}
