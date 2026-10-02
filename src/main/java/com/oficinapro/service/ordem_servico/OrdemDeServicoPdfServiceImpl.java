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
import com.oficinapro.storage.LogoStorage;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Gera os PDFs da OS e do comprovante de pagamento seguindo os templates do repositório
 * oficina-docs (cabeçalho de marca, título + datas, seções com filete, quadros de informação,
 * resumo financeiro com selo de situação e rodapé numerado).
 */
@Service
public class OrdemDeServicoPdfServiceImpl implements OrdemDeServicoPdfService {

  private static final Logger log = LoggerFactory.getLogger(OrdemDeServicoPdfServiceImpl.class);

  private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATA_HORA_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

  // Paleta dos templates (tokens oklch do oficina-docs convertidos para sRGB).
  private static final Color COR_PRIMARIA = new Color(29, 79, 126);
  private static final Color COR_TINTA = new Color(28, 34, 42);
  private static final Color COR_SUAVE = new Color(89, 97, 107);
  private static final Color COR_LINHA = new Color(215, 219, 224);
  private static final Color COR_FUNDO = new Color(244, 246, 248);
  private static final Color COR_AZUL_SUAVE = new Color(231, 242, 252);

  private static final Font F_H1 = fonte(19, Font.BOLD, COR_PRIMARIA);
  private static final Font F_EMPRESA_NOME = fonte(11, Font.BOLD, COR_TINTA);
  private static final Font F_SUAVE = fonte(8.5f, Font.NORMAL, COR_SUAVE);
  private static final Font F_ROTULO = fonte(7.5f, Font.BOLD, COR_SUAVE);
  private static final Font F_ROTULO_FINO = fonte(7.5f, Font.NORMAL, COR_SUAVE);
  private static final Font F_REF = fonte(12, Font.BOLD, COR_PRIMARIA);
  private static final Font F_TEXTO = fonte(8.5f, Font.NORMAL, COR_TINTA);
  private static final Font F_VALOR = fonte(8.5f, Font.BOLD, COR_TINTA);
  private static final Font F_SECAO = fonte(9, Font.BOLD, COR_PRIMARIA);
  private static final Font F_SECAO_PAGAMENTO = fonte(9, Font.BOLD, Color.WHITE);
  private static final Font F_SUBTITULO = fonte(9, Font.BOLD, COR_TINTA);
  private static final Font F_TH = fonte(7.5f, Font.BOLD, Color.WHITE);
  private static final Font F_FIN_ROTULO = fonte(7, Font.NORMAL, COR_SUAVE);
  private static final Font F_FIN_VALOR = fonte(9, Font.BOLD, COR_TINTA);
  private static final Font F_FIN_ROTULO_DESTAQUE = fonte(7, Font.NORMAL, Color.WHITE);
  private static final Font F_FIN_VALOR_DESTAQUE = fonte(12, Font.BOLD, Color.WHITE);
  private static final Font F_PAGO_VALOR = fonte(24, Font.BOLD, COR_PRIMARIA);
  private static final Font F_RODAPE = fonte(7.5f, Font.NORMAL, COR_SUAVE);

  private static final float MM = 72f / 25.4f;
  private static final float LOGO_ALTURA = 13f * MM;
  private static final float LOGO_LARGURA = 40f * MM;
  private static final String LOGO_PATH = "images/logo.png";

  private final ItemOsPecaRepository itemOsPecaRepository;
  private final MaoObraRepository maoObraRepository;
  private final PagamentoService pagamentoService;
  private final RegistroPagamentoService registroPagamentoService;
  private final LogoStorage logoStorage;

  public OrdemDeServicoPdfServiceImpl(
      ItemOsPecaRepository itemOsPecaRepository,
      MaoObraRepository maoObraRepository,
      PagamentoService pagamentoService,
      RegistroPagamentoService registroPagamentoService,
      LogoStorage logoStorage) {
    this.itemOsPecaRepository = itemOsPecaRepository;
    this.maoObraRepository = maoObraRepository;
    this.pagamentoService = pagamentoService;
    this.registroPagamentoService = registroPagamentoService;
    this.logoStorage = logoStorage;
  }

  private static Font fonte(float tamanho, int estilo, Color cor) {
    return new Font(Font.HELVETICA, tamanho, estilo, cor);
  }

  @Override
  public byte[] gerar(OrdemDeServico os) {
    try {
      Document document = novoDocumento();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(new RodapeEvent("OficinaPro  •  OS #" + formatarId(os.getId())));

      document.open();
      montarCabecalhoMarca(document, os);
      montarTitulo(
          document,
          "ORDEM DE SERVIÇO #" + formatarId(os.getId()),
          "Documento de acompanhamento de serviços automotivos",
          datas(
              "ABERTURA",
              os.getDataAbertura() != null ? os.getDataAbertura().format(DATA_HORA_FMT) : "-",
              "FECHAMENTO",
              os.getDataFechamento() != null
                  ? os.getDataFechamento().format(DATA_HORA_FMT)
                  : "Em andamento"));
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
      Document document = novoDocumento();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter writer = PdfWriter.getInstance(document, out);
      writer.setPageEvent(
          new RodapeEvent("OficinaPro  •  Comprovante da OS #" + formatarId(os.getId())));

      document.open();
      montarCabecalhoMarca(document, os);
      montarTitulo(
          document,
          "COMPROVANTE DE PAGAMENTO",
          "Ordem de Serviço #" + formatarId(os.getId()),
          datas("EMITIDO EM", LocalDateTime.now().format(DATA_HORA_FMT), null, null));
      montarSecaoResumoOs(document, os);
      montarSecaoResumoFinanceiro(document, os);
      montarSecaoRegistrosPagamento(document, os);
      montarNotaComprovante(document, os);
      document.close();

      return out.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException(
          "Falha ao gerar comprovante de pagamento da OS " + os.getId(), e);
    }
  }

  private Document novoDocumento() {
    return new Document(PageSize.A4, 14 * MM, 14 * MM, 13 * MM, 18 * MM);
  }

  // ---------- CABEÇALHO ----------

  /** Logo (da oficina ou padrão) | dados da oficina | referência, com filete inferior. */
  private void montarCabecalhoMarca(Document document, OrdemDeServico os) throws DocumentException {
    Oficina oficina = os.getOficina();
    Unidade unidade = os.getUnidade();

    PdfPTable cab = new PdfPTable(3);
    cab.setWidthPercentage(100);
    cab.setWidths(new float[] {46f, 103f, 31f});

    PdfPCell logo = celulaCabecalho();
    logo.setFixedHeight(21 * MM);
    try {
      Image img = Image.getInstance(bytesDaLogo(oficina));
      img.scaleToFit(LOGO_LARGURA, LOGO_ALTURA);
      logo.setImage(img);
    } catch (Exception e) {
      log.warn("Logo não carregada para o PDF: {}", e.getMessage());
    }
    cab.addCell(logo);

    PdfPCell empresa = celulaCabecalho();
    empresa.setBorderWidthLeft(0.75f);
    empresa.setBorderColorLeft(COR_LINHA);
    empresa.setPaddingLeft(6 * MM);
    empresa.addElement(new Paragraph(nullToDash(oficina.getNome()), F_EMPRESA_NOME));
    if (oficina.getCnpj() != null) {
      empresa.addElement(new Paragraph("CNPJ " + oficina.getCnpj(), F_SUAVE));
    }
    if (oficina.getTelefone() != null) {
      empresa.addElement(new Paragraph(oficina.getTelefone(), F_SUAVE));
    }
    if (unidade != null && unidade.getEndereco() != null) {
      empresa.addElement(new Paragraph(unidade.getEndereco(), F_SUAVE));
    }
    cab.addCell(empresa);

    PdfPCell ref = celulaCabecalho();
    Paragraph rotulo = new Paragraph("REFERÊNCIA", F_ROTULO);
    rotulo.setAlignment(Element.ALIGN_RIGHT);
    Paragraph valor = new Paragraph("OS #" + formatarId(os.getId()), F_REF);
    valor.setAlignment(Element.ALIGN_RIGHT);
    valor.setSpacingBefore(3f);
    ref.addElement(rotulo);
    ref.addElement(valor);
    cab.addCell(ref);

    document.add(cab);
  }

  /** Célula do cabeçalho de marca: centralizada na vertical, com filete inferior primário. */
  private PdfPCell celulaCabecalho() {
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.BOTTOM);
    c.setBorderWidthBottom(1.5f);
    c.setBorderColorBottom(COR_PRIMARIA);
    c.setVerticalAlignment(Element.ALIGN_MIDDLE);
    c.setPaddingBottom(5 * MM);
    return c;
  }

  private void montarTitulo(Document document, String titulo, String subtitulo, PdfPTable datas)
      throws DocumentException {
    PdfPTable t = new PdfPTable(2);
    t.setWidthPercentage(100);
    // com uma única data (comprovante) sobra mais espaço para o título não quebrar linha
    t.setWidths(datas.getNumberOfColumns() == 1 ? new float[] {4.2f, 1f} : new float[] {3f, 2f});

    PdfPCell esquerda = semBorda();
    esquerda.setPaddingTop(7 * MM);
    esquerda.setPaddingBottom(2 * MM);
    esquerda.addElement(new Paragraph(titulo, F_H1));
    Paragraph sub = new Paragraph(subtitulo, F_SUAVE);
    sub.setSpacingBefore(3f);
    esquerda.addElement(sub);
    t.addCell(esquerda);

    PdfPCell direita = semBorda();
    direita.setPaddingTop(7 * MM);
    direita.setPaddingBottom(2 * MM);
    direita.setVerticalAlignment(Element.ALIGN_BOTTOM);
    direita.addElement(datas);
    t.addCell(direita);
    document.add(t);
  }

  /** Bloco de datas alinhado à direita: um ou dois pares rótulo/valor lado a lado. */
  private PdfPTable datas(String rotulo1, String valor1, String rotulo2, String valor2) {
    boolean dois = rotulo2 != null;
    PdfPTable t = new PdfPTable(dois ? 2 : 1);
    t.setWidthPercentage(100);
    t.addCell(parData(rotulo1, valor1));
    if (dois) t.addCell(parData(rotulo2, valor2));
    return t;
  }

  private PdfPCell parData(String rotulo, String valor) {
    PdfPCell c = semBorda();
    Paragraph r = new Paragraph(rotulo, fonte(7, Font.BOLD, COR_SUAVE));
    r.setAlignment(Element.ALIGN_RIGHT);
    Paragraph v = new Paragraph(valor, F_VALOR);
    v.setAlignment(Element.ALIGN_RIGHT);
    v.setSpacingBefore(2f);
    c.addElement(r);
    c.addElement(v);
    return c;
  }

  /**
   * Logo da oficina; qualquer falha (sem logo, bucket fora do ar, imagem corrompida) cai na logo
   * padrão do sistema. A geração do PDF nunca pode falhar por causa da logo.
   */
  private byte[] bytesDaLogo(Oficina oficina) throws IOException {
    String caminho = oficina != null ? oficina.getLogoPath() : null;
    if (caminho != null) {
      try {
        Optional<byte[]> bytes = logoStorage.ler(caminho);
        if (bytes.isPresent()) {
          Image.getInstance(bytes.get());
          return bytes.get();
        }
      } catch (Exception e) {
        log.warn("Logo da oficina indisponível ({}): {}", caminho, e.getMessage());
      }
    }
    try (InputStream in = new ClassPathResource(LOGO_PATH).getInputStream()) {
      return in.readAllBytes();
    }
  }

  private String formatarId(Long id) {
    return String.format("%04d", id);
  }

  // ---------- OS: IDENTIFICAÇÃO ----------

  private void montarSecaoIdentificacao(Document document, OrdemDeServico os)
      throws DocumentException {
    Oficina oficina = os.getOficina();
    Unidade unidade = os.getUnidade();
    Veiculo veiculo = os.getVeiculo();
    Cliente cliente = os.getCliente();
    Mecanico mecanico = os.getMecanico();

    PdfPTable grid = new PdfPTable(3);
    grid.setWidthPercentage(100);
    grid.setWidths(new float[] {1.1f, 1f, 1.1f});

    grid.addCell(
        grupoInfo(
            "Oficina",
            new String[][] {
              {"Nome", oficina.getNome()},
              {"Unidade", unidade != null ? unidade.getNome() : null},
              {"CNPJ", oficina.getCnpj()},
              {"Telefone", oficina.getTelefone()}
            },
            true));
    grid.addCell(
        grupoInfo(
            "Cliente",
            new String[][] {
              {"Nome", cliente != null ? cliente.getNome() : null},
              {"CPF/CNPJ", cliente != null ? cliente.getDocumento() : null},
              {"Telefone", cliente != null ? cliente.getTelefone() : null}
            },
            true));
    grid.addCell(
        grupoInfo(
            "Veículo",
            new String[][] {
              {"Placa", veiculo.getPlaca()},
              {"Descrição", descricaoVeiculo(veiculo)}
            },
            false));

    PdfPTable secao = secao("IDENTIFICAÇÃO", false);
    secao.addCell(envolver(grid));

    PdfPCell mec = new PdfPCell();
    mec.setBorderColor(COR_LINHA);
    mec.setBorderWidthTop(0f);
    mec.setPadding(2.5f * MM);
    Paragraph p = new Paragraph();
    p.add(new Chunk("MECÂNICO RESPONSÁVEL     ", F_ROTULO));
    p.add(new Chunk(mecanico != null ? mecanico.getNome() : "Não informado", F_VALOR));
    mec.addElement(p);
    secao.addCell(mec);
    document.add(secao);
  }

  private String descricaoVeiculo(Veiculo v) {
    StringBuilder sb = new StringBuilder();
    String[] partes = {
      v.getMarca(),
      v.getModelo(),
      v.getAno() != null ? String.valueOf(v.getAno()) : null,
      v.getCor()
    };
    for (String parte : partes) {
      if (parte != null && !parte.isBlank()) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(parte);
      }
    }
    return sb.length() > 0 ? sb.toString() : "-";
  }

  // ---------- OS: SERVIÇOS ----------

  private void montarSecaoServicos(Document document, OrdemDeServico os) throws DocumentException {
    PdfPTable secao = secao("SERVIÇOS DA ORDEM DE SERVIÇO", false);

    PdfPCell conteudo = semBorda();
    conteudo.setPadding(0f);

    PdfPTable obs = new PdfPTable(1);
    obs.setWidthPercentage(100);
    PdfPCell obsCell = new PdfPCell();
    obsCell.setBorderColor(COR_LINHA);
    obsCell.setBackgroundColor(COR_FUNDO);
    obsCell.setPadding(3 * MM);
    obsCell.setMinimumHeight(15 * MM);
    obsCell.addElement(new Paragraph("OBSERVAÇÕES", F_ROTULO));
    String texto = os.getObs() != null && !os.getObs().isBlank() ? os.getObs() : "Sem observações.";
    Paragraph pObs = new Paragraph(texto, F_TEXTO);
    pObs.setSpacingBefore(2 * MM);
    obsCell.addElement(pObs);
    obs.addCell(obsCell);
    conteudo.addElement(obs);

    List<ItemOsPeca> pecas = itemOsPecaRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    conteudo.addElement(subtitulo("Peças"));
    if (pecas.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhuma peça registrada.", F_TEXTO));
    } else {
      PdfPTable t =
          tabela(
              new float[] {46f, 16f, 19f, 19f},
              "Peça",
              "Quantidade",
              "Valor unitário",
              "Valor total");
      int idx = 0;
      for (ItemOsPeca p : pecas) {
        boolean z = idx++ % 2 == 1;
        t.addCell(celula(p.getNome(), Element.ALIGN_LEFT, z));
        t.addCell(
            celula(p.getQuantidade().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT, z));
        t.addCell(celula(formatarMoeda(p.getValorUnitario()), Element.ALIGN_RIGHT, z));
        t.addCell(celula(formatarMoeda(p.getValorTotal()), Element.ALIGN_RIGHT, z));
      }
      conteudo.addElement(t);
    }

    List<MaoObra> maoObras = maoObraRepository.findByOrdemDeServicoIdOrderByIdAsc(os.getId());
    conteudo.addElement(subtitulo("Mão de obra"));
    if (maoObras.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhum item de mão de obra registrado.", F_TEXTO));
    } else {
      PdfPTable t = tabela(new float[] {81f, 19f}, "Descrição", "Valor");
      int idx = 0;
      for (MaoObra m : maoObras) {
        boolean z = idx++ % 2 == 1;
        t.addCell(celula(m.getDescricao(), Element.ALIGN_LEFT, z));
        BigDecimal valor = m.getValor() != null ? m.getValor() : BigDecimal.ZERO;
        t.addCell(celula(formatarMoeda(valor), Element.ALIGN_RIGHT, z));
      }
      conteudo.addElement(t);
    }

    secao.addCell(conteudo);
    document.add(secao);
  }

  // ---------- OS: PAGAMENTO ----------

  private void montarSecaoPagamento(Document document, OrdemDeServico os) throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

    PdfPTable secao = secao("PAGAMENTO", true);
    secao.addCell(
        envolver(
            resumoFinanceiro(
                new String[][] {
                  {"Valor da OS", formatarMoeda(os.getValorTotal())},
                  {"Desconto", formatarMoeda(os.getDesconto())},
                  {"Valor com desconto", formatarMoeda(os.getValorComDesconto())},
                  {"Valor pago", formatarMoeda(pagamento.valorPago())},
                  {"Saldo restante", formatarMoeda(pagamento.valorPendente())}
                },
                "Saldo restante")));
    document.add(secao);
  }

  // ---------- COMPROVANTE ----------

  private void montarSecaoResumoOs(Document document, OrdemDeServico os) throws DocumentException {
    Veiculo veiculo = os.getVeiculo();
    Cliente cliente = os.getCliente();

    PdfPTable grid = new PdfPTable(3);
    grid.setWidthPercentage(100);
    grid.setWidths(new float[] {1f, 1f, 1.25f});
    grid.addCell(grupoInfo("Oficina", new String[][] {{"Nome", os.getOficina().getNome()}}, true));
    grid.addCell(
        grupoInfo(
            "Cliente",
            new String[][] {{"Nome", cliente != null ? cliente.getNome() : "Não informado"}},
            true));
    grid.addCell(
        grupoInfo(
            "Veículo",
            new String[][] {
              {
                "Identificação",
                nullToDash(veiculo.getPlaca())
                    + "  •  "
                    + (nullToDash(veiculo.getMarca()) + " " + nullToDash(veiculo.getModelo()))
                        .trim()
              }
            },
            false));

    PdfPTable secao = secao("DADOS DA ORDEM DE SERVIÇO", false);
    secao.addCell(envolver(grid));
    document.add(secao);
  }

  private void montarSecaoResumoFinanceiro(Document document, OrdemDeServico os)
      throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

    PdfPTable secao = secao("RESUMO FINANCEIRO", true);

    PdfPTable callout = new PdfPTable(1);
    callout.setWidthPercentage(100);
    PdfPCell c = new PdfPCell();
    c.setBorderColor(COR_PRIMARIA);
    c.setBorderWidth(1.5f);
    c.setBackgroundColor(COR_AZUL_SUAVE);
    c.setPadding(5 * MM);
    c.addElement(centralizado("VALOR EFETIVAMENTE PAGO", F_ROTULO, 0));
    c.addElement(centralizado(formatarMoeda(pagamento.valorPago()), F_PAGO_VALOR, 1 * MM));
    c.addElement(
        centralizado("Pagamentos registrados até a emissão deste comprovante", F_ROTULO, 1 * MM));
    callout.addCell(c);
    callout.setSpacingAfter(3 * MM);

    PdfPCell conteudo = semBorda();
    conteudo.setPadding(0f);
    conteudo.addElement(callout);
    conteudo.addElement(
        resumoFinanceiro(
            new String[][] {
              {"Valor total da OS", formatarMoeda(os.getValorTotal())},
              {"Desconto", formatarMoeda(os.getDesconto())},
              {"Total com desconto", formatarMoeda(os.getValorComDesconto())},
              {"Total já pago", formatarMoeda(pagamento.valorPago())},
              {"Saldo restante", formatarMoeda(pagamento.valorPendente())}
            },
            "Total já pago"));
    secao.addCell(conteudo);
    document.add(secao);
  }

  private void montarSecaoRegistrosPagamento(Document document, OrdemDeServico os)
      throws DocumentException {
    PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());
    List<RegistroPagamentoResponseDTO> registros =
        registroPagamentoService.listarPorPagamento(pagamento.id());

    PdfPTable secao = secao("REGISTROS DE PAGAMENTO", false);
    PdfPCell conteudo = semBorda();
    conteudo.setPadding(0f);

    if (registros.isEmpty()) {
      conteudo.addElement(new Paragraph("Nenhum registro de pagamento.", F_TEXTO));
    } else {
      PdfPTable t = tabela(new float[] {28f, 46f, 26f}, "Data", "Forma de pagamento", "Valor");
      int idx = 0;
      for (RegistroPagamentoResponseDTO r : registros) {
        boolean z = idx++ % 2 == 1;
        t.addCell(
            celula(r.data() != null ? r.data().format(DATA_FMT) : "-", Element.ALIGN_LEFT, z));
        t.addCell(
            celula(
                r.meioPagamento() != null ? r.meioPagamento().toString() : "-",
                Element.ALIGN_RIGHT,
                z));
        t.addCell(celula(formatarMoeda(r.valor()), Element.ALIGN_RIGHT, z));
      }
      conteudo.addElement(t);
    }
    secao.addCell(conteudo);
    document.add(secao);
  }

  private void montarNotaComprovante(Document document, OrdemDeServico os)
      throws DocumentException {
    PdfPTable t = new PdfPTable(1);
    t.setWidthPercentage(100);
    t.setSpacingBefore(8 * MM);
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
    c.setBorderColor(COR_LINHA);
    c.setPadding(4 * MM);
    c.addElement(centralizado("Comprovante válido para conferência", F_SECAO, 0));
    c.addElement(
        centralizado(
            "Este documento apresenta os pagamentos registrados para a Ordem de Serviço #"
                + formatarId(os.getId())
                + " até a data de emissão.",
            fonte(8, Font.NORMAL, COR_SUAVE),
            1.5f * MM));
    t.addCell(c);
    document.add(t);
  }

  // ---------- COMPONENTES ----------

  /** Barra de título da seção; em `pagamento` usa o fundo primário, senão o filete lateral. */
  private PdfPTable secao(String titulo, boolean pagamento) {
    PdfPTable secao = new PdfPTable(1);
    secao.setWidthPercentage(100);
    secao.setSpacingBefore(4.5f * MM);
    secao.setKeepTogether(false);

    PdfPCell header = new PdfPCell(new Phrase(titulo, pagamento ? F_SECAO_PAGAMENTO : F_SECAO));
    header.setBackgroundColor(pagamento ? COR_PRIMARIA : COR_FUNDO);
    header.setBorder(pagamento ? Rectangle.NO_BORDER : Rectangle.LEFT);
    header.setBorderColor(COR_PRIMARIA);
    header.setBorderWidthLeft(3f);
    header.setPaddingTop(2.1f * MM);
    header.setPaddingBottom(2.6f * MM);
    header.setPaddingLeft(3 * MM);
    secao.addCell(header);

    PdfPCell espaco = semBorda();
    espaco.setFixedHeight(3 * MM);
    secao.addCell(espaco);
    return secao;
  }

  private PdfPCell envolver(PdfPTable interno) {
    PdfPCell c = new PdfPCell(interno);
    c.setBorder(Rectangle.NO_BORDER);
    c.setPadding(0f);
    return c;
  }

  /** Grupo "título + rótulo/valor" de um quadro com bordas finas. */
  private PdfPCell grupoInfo(String titulo, String[][] itens, boolean bordaDireita) {
    PdfPCell c = new PdfPCell();
    c.setBorderColor(COR_LINHA);
    c.setBorderWidthRight(bordaDireita ? 0.75f : 0.75f);
    c.setPadding(3 * MM);
    c.setMinimumHeight(20 * MM);
    Paragraph h = new Paragraph(titulo, F_SECAO);
    h.setSpacingAfter(2.5f * MM);
    c.addElement(h);

    PdfPTable dl = new PdfPTable(2);
    dl.setWidthPercentage(100);
    try {
      dl.setWidths(new float[] {19f, 40f});
    } catch (DocumentException e) {
      throw new IllegalStateException(e);
    }
    for (String[] item : itens) {
      PdfPCell dt = semBorda();
      dt.setPaddingBottom(1.5f * MM);
      dt.addElement(new Paragraph(item[0], F_ROTULO_FINO));
      dl.addCell(dt);
      PdfPCell dd = semBorda();
      dd.setPaddingBottom(1.5f * MM);
      dd.addElement(new Paragraph(nullToDash(item[1]), F_VALOR));
      dl.addCell(dd);
    }
    c.addElement(dl);
    return c;
  }

  /** Valores lado a lado, em largura total (a coluna de destaque em fundo primário). */
  private PdfPTable resumoFinanceiro(String[][] itens, String rotuloDestaque) {
    PdfPTable valores = new PdfPTable(itens.length);
    valores.setWidthPercentage(100);
    for (String[] item : itens) {
      boolean destaque = item[0].equals(rotuloDestaque);
      PdfPCell c = new PdfPCell();
      c.setBorderColor(COR_LINHA);
      c.setBackgroundColor(destaque ? COR_PRIMARIA : Color.WHITE);
      c.setPadding(2.5f * MM);
      c.setMinimumHeight(18 * MM);
      c.setVerticalAlignment(Element.ALIGN_MIDDLE);
      c.addElement(new Paragraph(item[0], destaque ? F_FIN_ROTULO_DESTAQUE : F_FIN_ROTULO));
      Paragraph v = new Paragraph(item[1], destaque ? F_FIN_VALOR_DESTAQUE : F_FIN_VALOR);
      v.setSpacingBefore(2f);
      c.addElement(v);
      valores.addCell(c);
    }
    return valores;
  }

  private Paragraph subtitulo(String texto) {
    Paragraph p = new Paragraph(texto, F_SUBTITULO);
    p.setSpacingBefore(3.5f * MM);
    p.setSpacingAfter(1.5f * MM);
    return p;
  }

  private Paragraph centralizado(String texto, Font f, float espacoAntes) {
    Paragraph p = new Paragraph(texto, f);
    p.setAlignment(Element.ALIGN_CENTER);
    p.setSpacingBefore(espacoAntes);
    return p;
  }

  private PdfPTable tabela(float[] larguras, String... titulos) {
    PdfPTable tabela = new PdfPTable(titulos.length);
    tabela.setWidthPercentage(100);
    try {
      tabela.setWidths(larguras);
    } catch (DocumentException e) {
      throw new IllegalStateException(e);
    }
    for (int i = 0; i < titulos.length; i++) {
      PdfPCell c = new PdfPCell(new Phrase(titulos[i], F_TH));
      c.setBackgroundColor(COR_PRIMARIA);
      c.setBorderColor(COR_LINHA);
      c.setPaddingTop(2.2f * MM);
      c.setPaddingBottom(2.7f * MM);
      c.setPaddingLeft(2.5f * MM);
      c.setPaddingRight(2.5f * MM);
      c.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
      tabela.addCell(c);
    }
    tabela.setHeaderRows(1);
    return tabela;
  }

  private PdfPCell celula(String texto, int alinhamento, boolean zebra) {
    PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", F_TEXTO));
    c.setPaddingTop(2.3f * MM);
    c.setPaddingBottom(2.8f * MM);
    c.setPaddingLeft(2.5f * MM);
    c.setPaddingRight(2.5f * MM);
    c.setBorderColor(COR_LINHA);
    c.setHorizontalAlignment(alinhamento);
    if (zebra) c.setBackgroundColor(COR_FUNDO);
    return c;
  }

  private PdfPCell semBorda() {
    PdfPCell c = new PdfPCell();
    c.setBorder(Rectangle.NO_BORDER);
    return c;
  }

  private String nullToDash(String v) {
    return v != null && !v.isBlank() ? v : "-";
  }

  @SuppressWarnings("deprecation")
  private String formatarMoeda(BigDecimal valor) {
    // Valores monetarios sao persistidos em centavos; converte para reais so na exibicao.
    BigDecimal v =
        (valor != null ? valor : BigDecimal.ZERO)
            .movePointLeft(2)
            .setScale(2, RoundingMode.HALF_UP);
    java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    return nf.format(v);
  }

  /** Rodapé centralizado com filete superior e numeração de página. */
  private static class RodapeEvent extends PdfPageEventHelper {
    private final String texto;

    RodapeEvent(String texto) {
      this.texto = texto;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();
      float y = 8 * MM;
      cb.setColorStroke(COR_LINHA);
      cb.setLineWidth(0.75f);
      cb.moveTo(document.left(), y + 3 * MM + 7);
      cb.lineTo(document.right(), y + 3 * MM + 7);
      cb.stroke();
      ColumnText.showTextAligned(
          cb,
          Element.ALIGN_CENTER,
          new Phrase(texto + "  •  Página " + writer.getPageNumber(), F_RODAPE),
          (document.left() + document.right()) / 2,
          y,
          0);
    }
  }
}
