package com.oficinapro.paginacao;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * As exigências da paginação, provadas com as consultas reais:
 *
 * <ol>
 *   <li>a BUSCA roda no servidor sobre todos os registros da oficina: um registro que estaria numa
 *       página que o frontend nunca carregou continua sendo encontrado (nada de "não encontrado"
 *       porque a página atual não o contém);
 *   <li>o AUTOCOMPLETE por nome acha o registro mesmo quando ele está além dos 20 primeiros;
 *   <li>totais e contagens vêm do banco, não da soma da página;
 *   <li>o tamanho da página tem teto e a ordenação só aceita campos permitidos;
 *   <li>nada de outra oficina entra no resultado.
 * </ol>
 *
 * <p>35 OS na oficina A (mais velha = menor id = última página na ordem padrão) e uma OS da oficina
 * B com a MESMA placa de uma das OS da A.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaginacaoBuscaIntegrationTest {

  private static final int TOTAL_OS = 35;

  @Autowired private WebApplicationContext context;
  @Autowired private EntityManager em;
  @Autowired private JwtService jwtService;

  private Oficina oficinaA;
  private Oficina oficinaB;
  private final List<OrdemDeServico> ordensA = new ArrayList<>();
  private OrdemDeServico ordemB;
  private String tokenGerenteA;
  private String tokenMecanicoA;

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context)
        .apply(
            org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                .springSecurity())
        .build();
  }

  private ResultActions chamar(String url, String token) throws Exception {
    return mockMvc()
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
  }

  /** Busca de OS com o termo como parâmetro: evita ambiguidade de encoding de #, % e _. */
  private ResultActions buscarOs(String termo) throws Exception {
    return mockMvc()
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/ordens-servico")
                .param("q", termo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGerenteA));
  }

  private ResultActions chamar(String url) throws Exception {
    return chamar(url, tokenGerenteA);
  }

  @BeforeEach
  void criarDados() {
    oficinaA = oficina("OFICINA A", "11111111000111");
    oficinaB = oficina("OFICINA B", "22222222000122");

    Unidade unidadeA = unidade(oficinaA, "UNIDADE A", "RUA A, 1");
    Unidade unidadeB = unidade(oficinaB, "UNIDADE B", "RUA B, 1");

    for (int i = 1; i <= TOTAL_OS; i++) {
      String n = "%02d".formatted(i);
      Veiculo veiculo = veiculo(oficinaA, "PLA00" + n);
      Cliente cliente = cliente(oficinaA, "CLIENTE " + n, "1000000" + n + "0");

      StatusOrdemDeServico status =
          i % 2 == 0 ? StatusOrdemDeServico.EM_EXECUCAO : StatusOrdemDeServico.ABERTA;

      OrdemDeServico os =
          ordem(oficinaA, unidadeA, veiculo, cliente, status, new BigDecimal(1000 + i * 100));
      ordensA.add(os);

      // metade paga integralmente, o resto fica com saldo
      boolean paga = i % 2 == 0;
      pagamento(os, paga ? os.getValorComDesconto() : BigDecimal.ZERO, paga);
    }

    // Oficina B tem uma OS com a MESMA placa de uma OS da A (PLA0007) e cliente homônimo.
    ordemB =
        ordem(
            oficinaB,
            unidadeB,
            veiculo(oficinaB, "PLA0007"),
            cliente(oficinaB, "CLIENTE 07", "99999999999"),
            StatusOrdemDeServico.ABERTA,
            new BigDecimal("777"));
    pagamento(ordemB, BigDecimal.ZERO, false);

    // 30 peças avulsas + 5 já vinculadas a OS, em A
    for (int i = 1; i <= 30; i++) {
      peca(oficinaA, null, "PECA AVULSA " + "%02d".formatted(i));
    }
    for (int i = 1; i <= 5; i++) {
      peca(oficinaA, ordensA.get(i - 1), "PECA VINCULADA " + i);
    }

    Usuario gerente = usuario(oficinaA, "gerente.pag", Role.GERENTE);
    Usuario mecanico = usuario(oficinaA, "mecanico.pag", Role.MECANICO);

    em.flush();
    em.clear();

    tokenGerenteA = jwtService.gerarToken(gerente);
    tokenMecanicoA = jwtService.gerarToken(mecanico);
  }

  // ------------------------------------------------------------------ OS: página

  @Test
  @DisplayName("OS: página de 10 conta só as da oficina (35), em 4 páginas, mais recentes primeiro")
  void paginaDeOrdens() throws Exception {
    chamar("/api/ordens-servico?size=10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.totalElements").value(TOTAL_OS))
        .andExpect(jsonPath("$.totalPages").value(4))
        .andExpect(jsonPath("$.content[0].id").value(ordensA.get(TOTAL_OS - 1).getId()));
  }

  @Test
  @DisplayName("OS: a última página traz o resto (5), e uma página além do fim vem vazia (200)")
  void ultimaPaginaEAlemDoFim() throws Exception {
    chamar("/api/ordens-servico?size=10&page=3")
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.last").value(true));

    chamar("/api/ordens-servico?size=10&page=50")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements").value(TOTAL_OS));
  }

  // ------------------------------------------------------------------ OS: busca fora da página

  @Test
  @DisplayName("OS: busca por placa acha a OS mais antiga mesmo ela estando na 4ª página")
  void buscaPorPlacaForaDaPaginaCarregada() throws Exception {
    // PLA0001 é a OS mais antiga: na ordem padrão (mais recentes primeiro) fica na última página.
    chamar("/api/ordens-servico?size=10&q=PLA0001")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].placaVeiculo").value("PLA0001"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("OS: busca por placa ignora caixa e hífen (abc-1234 / pla-0001)")
  void buscaPorPlacaIgnoraCaixaEHifen() throws Exception {
    chamar("/api/ordens-servico?q=pla-0001")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].placaVeiculo").value("PLA0001"));
  }

  @Test
  @DisplayName("OS: busca por nome do cliente acha o cliente da página não carregada")
  void buscaPorClienteForaDaPaginaCarregada() throws Exception {
    chamar("/api/ordens-servico?size=10&q=cliente 03")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].nomeCliente").value("CLIENTE 03"));
  }

  @Test
  @DisplayName("OS: busca pelo código (#0012) e pelo número puro acha a OS exata")
  void buscaPeloNumeroDaOs() throws Exception {
    Long id = ordensA.get(0).getId();

    buscarOs("#" + "%04d".formatted(id))
        .andExpect(jsonPath("$.content[?(@.id == " + id + ")]", hasSize(1)));

    buscarOs(String.valueOf(id))
        .andExpect(jsonPath("$.content[?(@.id == " + id + ")]", hasSize(1)));
  }

  @Test
  @DisplayName("OS: filtro de status vale sobre TODAS as OS, e combina com a busca")
  void filtroDeStatusEBuscaCombinados() throws Exception {
    // pares (2,4,...,34) estão EM_EXECUCAO: 17 OS
    chamar("/api/ordens-servico?status=EM_EXECUCAO&size=5")
        .andExpect(jsonPath("$.totalElements").value(17))
        .andExpect(jsonPath("$.totalPages").value(4))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("EM_EXECUCAO"))));

    chamar("/api/ordens-servico?status=EM_EXECUCAO&q=PLA0002")
        .andExpect(jsonPath("$.content", hasSize(1)));

    // PLA0001 é ABERTA: com o filtro EM_EXECUCAO não aparece
    chamar("/api/ordens-servico?status=EM_EXECUCAO&q=PLA0001")
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  @DisplayName("OS: busca pelo texto do status (ex.: 'execucao') também funciona")
  void buscaPeloTextoDoStatus() throws Exception {
    chamar("/api/ordens-servico?q=em_execucao").andExpect(jsonPath("$.totalElements").value(17));
  }

  // ------------------------------------------------------------------ OS: segurança da busca

  @Test
  @DisplayName("OS: curingas do usuário (% e _) são texto comum e não casam tudo")
  void curingasNaoCasamTudo() throws Exception {
    // "%" casaria tudo se fosse curinga; "_" casaria qualquer caractere. Como texto comum, nenhum
    // placa/cliente/status contém "%" e só os status com "_" no nome (EM_EXECUCAO) contêm "_".
    buscarOs("%").andExpect(jsonPath("$.totalElements").value(0));
    buscarOs("%%").andExpect(jsonPath("$.totalElements").value(0));
    // "EM_EXECUCAO" tem "_" literal: 17 OS. Se "_" fosse curinga, "EM_EXEC" casaria igual, mas
    // "EM-EXEC" (outro caractere no lugar do "_") também casaria; como texto comum não casa.
    buscarOs("EM_EXEC").andExpect(jsonPath("$.totalElements").value(17));
    buscarOs("EM-EXEC").andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("OS: nunca devolve OS de outra oficina, mesmo com placa e cliente iguais")
  void naoVazaOutraOficina() throws Exception {
    chamar("/api/ordens-servico?q=PLA0007")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].oficinaId").value(oficinaA.getId()))
        .andExpect(jsonPath("$.content[?(@.id == " + ordemB.getId() + ")]", hasSize(0)));

    chamar("/api/ordens-servico?q=cliente 07")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].oficinaId").value(oficinaA.getId()));
  }

  @Test
  @DisplayName("OS: tamanho de página tem teto de 100, qualquer que seja o size pedido")
  void tetoDeTamanho() throws Exception {
    chamar("/api/ordens-servico?size=100000")
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.content", hasSize(TOTAL_OS)));
  }

  @Test
  @DisplayName(
      "OS: ordena por campo permitido; caminho livre (cliente.nome, oficina.cnpj) é ignorado")
  void ordenacaoRestrita() throws Exception {
    chamar("/api/ordens-servico?sort=valorTotal,asc&size=3")
        .andExpect(jsonPath("$.content[0].id").value(ordensA.get(0).getId()))
        .andExpect(jsonPath("$.content[1].id").value(ordensA.get(1).getId()));

    chamar("/api/ordens-servico?sort=oficina.cnpj,asc&size=3")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(ordensA.get(TOTAL_OS - 1).getId()));

    chamar("/api/ordens-servico?sort=campoInexistente,desc")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(ordensA.get(TOTAL_OS - 1).getId()));
  }

  @Test
  @DisplayName("OS: as listagens por relação também são páginas, restritas à oficina")
  void listagemPorRelacaoPaginada() throws Exception {
    Long unidadeId = ordensA.get(0).getUnidade().getId();

    chamar("/api/ordens-servico/unidade/" + unidadeId + "?size=10")
        .andExpect(jsonPath("$.totalElements").value(TOTAL_OS))
        .andExpect(jsonPath("$.content", hasSize(10)));

    chamar("/api/ordens-servico/status/ABERTA?size=10")
        .andExpect(jsonPath("$.totalElements").value(18)); // ímpares: 1,3,...,35
  }

  // ------------------------------------------------------------------ Pagamentos

  @Test
  @DisplayName("Pagamentos: página, busca por parte do nº da OS e filtro de status sobre tudo")
  void pagamentosPaginadosEBusca() throws Exception {
    Long oficina = oficinaA.getId();

    chamar("/api/pagamentos/oficina/" + oficina + "?size=10")
        .andExpect(jsonPath("$.totalElements").value(TOTAL_OS))
        .andExpect(jsonPath("$.content", hasSize(10)));

    Long osMaisAntiga = ordensA.get(0).getId();
    chamar("/api/pagamentos/oficina/" + oficina + "?size=10&q=" + osMaisAntiga)
        .andExpect(jsonPath("$.content[?(@.osId == " + osMaisAntiga + ")]", hasSize(1)));

    // pares = PAGA (17), ímpares = PENDENTE (18)
    chamar("/api/pagamentos/oficina/" + oficina + "?status=PAGA&size=5")
        .andExpect(jsonPath("$.totalElements").value(17))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("PAGA"))));
  }

  @Test
  @DisplayName("Pagamentos: resumo soma tudo no banco (não só a página) e só da oficina")
  void resumoSomaTudoNoBanco() throws Exception {
    BigDecimal recebido = BigDecimal.ZERO;
    BigDecimal aReceber = BigDecimal.ZERO;
    int comSaldo = 0;
    for (int i = 1; i <= TOTAL_OS; i++) {
      BigDecimal valor = new BigDecimal(1000 + i * 100);
      if (i % 2 == 0) {
        recebido = recebido.add(valor);
      } else {
        aReceber = aReceber.add(valor);
        comSaldo++;
      }
    }

    chamar("/api/pagamentos/oficina/" + oficinaA.getId() + "/resumo")
        .andExpect(jsonPath("$.totalRecebido").value(recebido.doubleValue()))
        .andExpect(jsonPath("$.valorAReceber").value(aReceber.doubleValue()))
        .andExpect(jsonPath("$.pendentes").value(comSaldo));
  }

  @Test
  @DisplayName(
      "Pagamentos: por-os devolve só as OS pedidas e da oficina; mais de 100 ids é recusado")
  void pagamentosPorOs() throws Exception {
    Long os1 = ordensA.get(0).getId();
    Long os2 = ordensA.get(1).getId();

    chamar("/api/pagamentos/oficina/" + oficinaA.getId() + "/por-os?osIds=" + os1 + "," + os2)
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].osId", containsInAnyOrder(os1.intValue(), os2.intValue())));

    // a OS da outra oficina, pedida pelo id, simplesmente não vem
    chamar("/api/pagamentos/oficina/" + oficinaA.getId() + "/por-os?osIds=" + ordemB.getId())
        .andExpect(jsonPath("$", hasSize(0)));

    String muitos =
        java.util.stream.LongStream.rangeClosed(1, 101)
            .mapToObj(String::valueOf)
            .collect(java.util.stream.Collectors.joining(","));
    chamar("/api/pagamentos/oficina/" + oficinaA.getId() + "/por-os?osIds=" + muitos)
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Pagamentos: oficina de terceiros é 403 e MECANICO não acessa o financeiro")
  void pagamentosRespeitamIsolamentoEPapel() throws Exception {
    chamar("/api/pagamentos/oficina/" + oficinaB.getId()).andExpect(status().isForbidden());
    chamar("/api/pagamentos/oficina/" + oficinaB.getId() + "/resumo")
        .andExpect(status().isForbidden());
    chamar("/api/pagamentos/oficina/" + oficinaA.getId() + "/resumo", tokenMecanicoA)
        .andExpect(status().isForbidden());
  }

  // ------------------------------------------------------------------ Peças

  @Test
  @DisplayName("Peças: página, e busca por nome acha a peça de qualquer página")
  void pecasPaginadasEBusca() throws Exception {
    chamar("/api/itens-os-peca?size=10")
        .andExpect(jsonPath("$.totalElements").value(35))
        .andExpect(jsonPath("$.content", hasSize(10)));

    chamar("/api/itens-os-peca?q=vinculada 3")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].nome").value("PECA VINCULADA 3"));
  }

  @Test
  @DisplayName("Peças: avulsas=true lista só as sem OS, e a busca acha a 27ª além da 1ª página")
  void pecasAvulsasParaVincular() throws Exception {
    chamar("/api/itens-os-peca?avulsas=true&size=5")
        .andExpect(jsonPath("$.totalElements").value(30))
        .andExpect(jsonPath("$.content[*].osId", everyItem(org.hamcrest.Matchers.nullValue())));

    chamar("/api/itens-os-peca?avulsas=true&size=5&q=avulsa 27")
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].nome").value("PECA AVULSA 27"));

    // as vinculadas nunca aparecem como candidatas
    chamar("/api/itens-os-peca?avulsas=true&q=vinculada")
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  // ------------------------------------------------------------------ Autocomplete

  @Test
  @DisplayName("Autocomplete de cliente: devolve no máximo 20, em ordem alfabética")
  void autocompleteTemTeto() throws Exception {
    chamar("/api/clientes/nome/CLIENTE")
        .andExpect(jsonPath("$", hasSize(20)))
        .andExpect(jsonPath("$[0].nome").value("CLIENTE 01"))
        .andExpect(jsonPath("$[19].nome").value("CLIENTE 20"));
  }

  @Test
  @DisplayName("Autocomplete de cliente: acha quem está além dos 20 primeiros ao digitar mais")
  void autocompleteAchaAlemDosVintePrimeiros() throws Exception {
    // CLIENTE 35 não está entre os 20 primeiros de "CLIENTE", mas é encontrado ao refinar o termo.
    chamar("/api/clientes/nome/CLIENTE 35")
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nome").value("CLIENTE 35"));

    chamar("/api/clientes/nome/cliente 3")
        .andExpect(
            jsonPath(
                "$[*].nome",
                containsInAnyOrder(
                    "CLIENTE 30",
                    "CLIENTE 31",
                    "CLIENTE 32",
                    "CLIENTE 33",
                    "CLIENTE 34",
                    "CLIENTE 35")));
  }

  @Test
  @DisplayName("Autocomplete de cliente: não mistura clientes de outra oficina")
  void autocompleteNaoVazaOutraOficina() throws Exception {
    chamar("/api/clientes/nome/CLIENTE 07")
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].oficinaId").value(oficinaA.getId()));
  }

  // ------------------------------------------------------------------ Dashboard

  @Test
  @DisplayName("Dashboard: contagens vêm do banco e cobrem todos os registros da oficina")
  void dashboardContaTudo() throws Exception {
    chamar("/api/dashboard/data")
        .andExpect(jsonPath("$.ordensAbertas").value(18))
        .andExpect(jsonPath("$.pagamentosPendentes").value(18));
  }

  // ------------------------------------------------------------------ montagem de dados

  private Oficina oficina(String nome, String cnpj) {
    Oficina o = new Oficina();
    o.setNome(nome);
    o.setCnpj(cnpj);
    o.setAtivo(true);
    em.persist(o);
    return o;
  }

  private Unidade unidade(Oficina oficina, String nome, String endereco) {
    Unidade u = new Unidade(oficina, nome, endereco, "83999990000");
    em.persist(u);
    return u;
  }

  private Veiculo veiculo(Oficina oficina, String placa) {
    Veiculo v = new Veiculo();
    v.setOficina(oficina);
    v.setPlaca(placa);
    v.setModelo("CIVIC");
    v.setCor("PRETO");
    em.persist(v);
    return v;
  }

  private Cliente cliente(Oficina oficina, String nome, String documento) {
    Cliente c = new Cliente();
    c.setOficina(oficina);
    c.setNome(nome);
    c.setDocumento(documento);
    em.persist(c);
    return c;
  }

  private OrdemDeServico ordem(
      Oficina oficina,
      Unidade unidade,
      Veiculo veiculo,
      Cliente cliente,
      StatusOrdemDeServico status,
      BigDecimal valor) {
    OrdemDeServico os = new OrdemDeServico();
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);
    os.setCliente(cliente);
    os.setStatus(status);
    os.setDataAbertura(LocalDateTime.now());
    os.setValorTotal(valor);
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(valor);
    em.persist(os);
    return os;
  }

  private void pagamento(OrdemDeServico os, BigDecimal pago, boolean quitado) {
    Pagamento p = new Pagamento();
    p.setOrdemDeServico(os);
    p.setValorPago(pago);
    p.setStatus(quitado ? StatusPagamento.PAGA : StatusPagamento.PAGAMENTO_PENDENTE);
    em.persist(p);
  }

  private void peca(Oficina oficina, OrdemDeServico os, String nome) {
    ItemOsPeca p = new ItemOsPeca();
    p.setOficina(oficina);
    p.setOrdemDeServico(os);
    p.setNome(nome);
    p.setQuantidade(BigDecimal.ONE);
    p.setValorUnitario(new BigDecimal("100"));
    p.setValorTotal(new BigDecimal("100"));
    em.persist(p);
  }

  private Usuario usuario(Oficina oficina, String username, Role role) {
    Usuario u = new Usuario();
    u.setNome(username.toUpperCase());
    u.setOficina(oficina);
    u.setUsername(username);
    u.setPassword("nao-usada");
    u.setRole(role);
    em.persist(u);
    return u;
  }
}
