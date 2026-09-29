package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.MaoObra;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.RegistroPagamento;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Isolamento entre oficinas (multi-tenant), de ponta a ponta: filtro JWT, @PreAuthorize, services
 * e banco (H2). O gerente da oficina A tenta ler e alterar, por ID, cada recurso da oficina B e
 * nada pode vazar nem ser modificado.
 *
 * <p>Regra esperada: registro de outra oficina responde 404 (nao revela que existe); rota que
 * recebe o proprio {@code oficinaId} de outra oficina responde 403.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private EntityManager em;
  @Autowired private JwtService jwtService;

  private Tenant a;
  private Tenant b;
  private String tokenGerenteA;

  /** Conjunto completo de dados de uma oficina. */
  private static class Tenant {
    Oficina oficina;
    Unidade unidade;
    Veiculo veiculo;
    Cliente cliente;
    Mecanico mecanico;
    Usuario gerente;
    Usuario mecanicoUsuario;
    OrdemDeServico os;
    Pagamento pagamento;
    RegistroPagamento registro;
    ItemOsPeca peca;
    ItemOsPeca pecaAvulsa;
    MaoObra maoObra;
  }

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context)
        .apply(
            org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                .springSecurity())
        .build();
  }

  @BeforeEach
  void criarDuasOficinas() {
    a = criarTenant("A", "11111111000111");
    b = criarTenant("B", "22222222000122");
    em.flush();
    em.clear();
    tokenGerenteA = jwtService.gerarToken(a.gerente);
  }

  private Tenant criarTenant(String sufixo, String cnpj) {
    Tenant t = new Tenant();

    t.oficina = new Oficina();
    t.oficina.setNome("OFICINA " + sufixo);
    t.oficina.setCnpj(cnpj);
    t.oficina.setAtivo(true);
    em.persist(t.oficina);

    t.unidade = new Unidade(t.oficina, "UNIDADE " + sufixo, "RUA " + sufixo + ", 1", "83999990000");
    em.persist(t.unidade);

    t.veiculo = new Veiculo();
    t.veiculo.setOficina(t.oficina);
    t.veiculo.setPlaca("AAA000" + (sufixo.equals("A") ? "1" : "2"));
    t.veiculo.setModelo("CIVIC");
    t.veiculo.setCor("PRETO");
    em.persist(t.veiculo);

    t.cliente = new Cliente();
    t.cliente.setOficina(t.oficina);
    t.cliente.setNome("CLIENTE " + sufixo);
    t.cliente.setDocumento("1000000000" + sufixo.charAt(0));
    em.persist(t.cliente);

    t.mecanico = new Mecanico();
    t.mecanico.setOficina(t.oficina);
    t.mecanico.setNome("MECANICO " + sufixo);
    t.mecanico.setDocumento("2000000000" + sufixo.charAt(0));
    em.persist(t.mecanico);

    t.gerente = usuario(t.oficina, "gerente." + sufixo.toLowerCase(), Role.GERENTE);
    t.mecanicoUsuario = usuario(t.oficina, "mecanico." + sufixo.toLowerCase(), Role.MECANICO);

    t.os = new OrdemDeServico();
    t.os.setOficina(t.oficina);
    t.os.setUnidade(t.unidade);
    t.os.setVeiculo(t.veiculo);
    t.os.setCliente(t.cliente);
    t.os.setMecanico(t.mecanico);
    t.os.setStatus(StatusOrdemDeServico.ABERTA);
    t.os.setDataAbertura(LocalDateTime.now());
    t.os.setValorTotal(new BigDecimal("10000"));
    t.os.setDesconto(BigDecimal.ZERO);
    t.os.setValorComDesconto(new BigDecimal("10000"));
    em.persist(t.os);

    t.pagamento = new Pagamento();
    t.pagamento.setOrdemDeServico(t.os);
    t.pagamento.setValorPago(new BigDecimal("2000"));
    t.pagamento.setStatus(StatusPagamento.PAGO_PARCIALMENTE);
    em.persist(t.pagamento);

    t.registro = new RegistroPagamento();
    t.registro.setPagamento(t.pagamento);
    t.registro.setValor(new BigDecimal("2000"));
    t.registro.setMeioPagamento(MeioPagamento.PIX);
    t.registro.setData(LocalDateTime.now());
    em.persist(t.registro);

    t.peca = peca(t.oficina, t.os, "PECA " + sufixo);
    t.pecaAvulsa = peca(t.oficina, null, "PECA AVULSA " + sufixo);

    t.maoObra = new MaoObra();
    t.maoObra.setOrdemDeServico(t.os);
    t.maoObra.setValor(new BigDecimal("3000"));
    t.maoObra.setDescricao("MAO DE OBRA " + sufixo);
    em.persist(t.maoObra);

    return t;
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

  private ItemOsPeca peca(Oficina oficina, OrdemDeServico os, String nome) {
    ItemOsPeca p = new ItemOsPeca();
    p.setOficina(oficina);
    p.setOrdemDeServico(os);
    p.setNome(nome);
    p.setQuantidade(BigDecimal.ONE);
    p.setValorUnitario(new BigDecimal("5000"));
    p.setValorTotal(new BigDecimal("5000"));
    em.persist(p);
    return p;
  }

  // ------------------------------------------------------------------
  // helpers de requisicao
  // ------------------------------------------------------------------

  private record Caso(String descricao, MockHttpServletRequestBuilder request, int esperado) {}

  private MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req) {
    return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGerenteA);
  }

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
    return comToken(req).contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private void executar(List<Caso> casos) throws Exception {
    MockMvc mvc = mockMvc();
    List<String> falhas = new ArrayList<>();
    for (Caso c : casos) {
      int status = mvc.perform(c.request()).andReturn().getResponse().getStatus();
      if (status != c.esperado()) {
        falhas.add(c.descricao() + " -> esperado " + c.esperado() + " mas veio " + status);
      }
    }
    assertThat(falhas).as("casos que violam o isolamento").isEmpty();
  }

  // ------------------------------------------------------------------
  // testes
  // ------------------------------------------------------------------

  @Test
  @DisplayName("controle: o gerente da oficina A acessa normalmente os recursos da propria oficina")
  void gerenteAcessaOsProprios() throws Exception {
    executar(
        List.of(
            new Caso("GET OS", comToken(get("/api/ordens-servico/" + a.os.getId())), 200),
            new Caso("GET PDF", comToken(get("/api/ordens-servico/" + a.os.getId() + "/pdf")), 200),
            new Caso(
                "GET comprovante",
                comToken(get("/api/ordens-servico/" + a.os.getId() + "/comprovante-pagamento")),
                200),
            new Caso("GET pagamento", comToken(get("/api/pagamentos/" + a.pagamento.getId())), 200),
            new Caso("GET peca", comToken(get("/api/itens-os-peca/" + a.peca.getId())), 200),
            new Caso("GET mao de obra", comToken(get("/api/mao-obra/" + a.maoObra.getId())), 200),
            new Caso("GET cliente", comToken(get("/api/clientes/" + a.cliente.getId())), 200),
            new Caso("GET veiculo", comToken(get("/api/veiculos/" + a.veiculo.getId())), 200),
            new Caso("GET mecanico", comToken(get("/api/mecanicos/" + a.mecanico.getId())), 200),
            new Caso("GET unidade", comToken(get("/api/unidades/" + a.unidade.getId())), 200)));
  }

  @Test
  @DisplayName("gerente da oficina A nao le nenhum recurso da oficina B por ID")
  void naoLeRecursosDeOutraOficina() throws Exception {
    executar(
        List.of(
            new Caso("GET OS", comToken(get("/api/ordens-servico/" + b.os.getId())), 404),
            new Caso("GET PDF", comToken(get("/api/ordens-servico/" + b.os.getId() + "/pdf")), 404),
            new Caso(
                "GET comprovante",
                comToken(get("/api/ordens-servico/" + b.os.getId() + "/comprovante-pagamento")),
                404),
            new Caso("GET pagamento", comToken(get("/api/pagamentos/" + b.pagamento.getId())), 404),
            new Caso("GET pagamento por OS", comToken(get("/api/pagamentos/os/" + b.os.getId())), 404),
            new Caso(
                "GET registro de pagamento",
                comToken(get("/api/registros-pagamento/" + b.registro.getId())),
                404),
            new Caso(
                "GET registros por pagamento",
                comToken(get("/api/registros-pagamento/pagamento/" + b.pagamento.getId())),
                404),
            new Caso("GET peca", comToken(get("/api/itens-os-peca/" + b.peca.getId())), 404),
            new Caso("GET pecas da OS", comToken(get("/api/itens-os-peca/os/" + b.os.getId())), 404),
            new Caso("GET mao de obra", comToken(get("/api/mao-obra/" + b.maoObra.getId())), 404),
            new Caso("GET mao de obra da OS", comToken(get("/api/mao-obra/os/" + b.os.getId())), 404),
            new Caso("GET cliente", comToken(get("/api/clientes/" + b.cliente.getId())), 404),
            new Caso("GET veiculo", comToken(get("/api/veiculos/" + b.veiculo.getId())), 404),
            new Caso("GET mecanico", comToken(get("/api/mecanicos/" + b.mecanico.getId())), 404),
            new Caso("GET unidade", comToken(get("/api/unidades/" + b.unidade.getId())), 404),
            new Caso("GET usuario", comToken(get("/api/usuarios/" + b.gerente.getId())), 404),
            new Caso(
                "GET OS por veiculo",
                comToken(get("/api/ordens-servico/veiculo/" + b.veiculo.getId())),
                404),
            new Caso(
                "GET OS por cliente",
                comToken(get("/api/ordens-servico/cliente/" + b.cliente.getId())),
                404),
            new Caso(
                "GET OS por mecanico",
                comToken(get("/api/ordens-servico/mecanico/" + b.mecanico.getId())),
                404),
            new Caso(
                "GET OS por unidade",
                comToken(get("/api/ordens-servico/unidade/" + b.unidade.getId())),
                404),
            new Caso(
                "GET pagamentos da oficina B",
                comToken(get("/api/pagamentos/oficina/" + b.oficina.getId())),
                403),
            new Caso(
                "GET a receber da oficina B",
                comToken(get("/api/pagamentos/oficina/" + b.oficina.getId() + "/a-receber")),
                403),
            new Caso(
                "GET pagamentos por status da oficina B",
                comToken(
                    get(
                        "/api/pagamentos/oficina/"
                            + b.oficina.getId()
                            + "/status/PAGO_PARCIALMENTE")),
                403)));
  }

  @Test
  @DisplayName("gerente da oficina A nao altera nem exclui nenhum recurso da oficina B")
  void naoAlteraRecursosDeOutraOficina() throws Exception {
    executar(
        List.of(
            new Caso(
                "PATCH status da OS",
                json(patch("/api/ordens-servico/" + b.os.getId() + "/status"), "{\"status\":\"DIAGNOSTICO\"}"),
                404),
            new Caso(
                "PATCH desconto da OS",
                json(patch("/api/ordens-servico/" + b.os.getId() + "/desconto"), "100"),
                404),
            new Caso("DELETE OS", comToken(delete("/api/ordens-servico/" + b.os.getId())), 404),
            new Caso(
                "PUT pagamento",
                json(put("/api/pagamentos/" + b.pagamento.getId()), "{\"obs\":\"invadido\"}"),
                404),
            new Caso(
                "DELETE registro de pagamento",
                comToken(delete("/api/registros-pagamento/" + b.registro.getId())),
                404),
            new Caso(
                "PUT peca",
                json(
                    put("/api/itens-os-peca/" + b.peca.getId()),
                    "{\"nome\":\"INVADIDA\",\"quantidade\":1,\"valorUnitario\":100}"),
                404),
            new Caso("DELETE peca", comToken(delete("/api/itens-os-peca/" + b.peca.getId())), 404),
            new Caso(
                "DELETE vinculo da peca",
                comToken(delete("/api/itens-os-peca/" + b.peca.getId() + "/os")),
                404),
            new Caso(
                "PUT mao de obra",
                json(
                    put("/api/mao-obra/" + b.maoObra.getId()),
                    "{\"osId\":" + b.os.getId() + ",\"valor\":100,\"descricao\":\"INVADIDA\"}"),
                404),
            new Caso("DELETE mao de obra", comToken(delete("/api/mao-obra/" + b.maoObra.getId())), 404),
            new Caso("DELETE cliente", comToken(delete("/api/clientes/" + b.cliente.getId())), 404),
            new Caso("DELETE veiculo", comToken(delete("/api/veiculos/" + b.veiculo.getId())), 404),
            new Caso("DELETE mecanico", comToken(delete("/api/mecanicos/" + b.mecanico.getId())), 404),
            new Caso("DELETE unidade", comToken(delete("/api/unidades/" + b.unidade.getId())), 404),
            new Caso("DELETE usuario", comToken(delete("/api/usuarios/" + b.gerente.getId())), 404),
            new Caso(
                "PATCH desbloquear usuario",
                comToken(patch("/api/usuarios/" + b.gerente.getId() + "/desbloquear")),
                404),
            new Caso(
                "PUT usuario",
                json(
                    put("/api/usuarios/" + b.gerente.getId()),
                    "{\"nome\":\"X\",\"username\":\"invadido\",\"role\":\"GERENTE\",\"oficinaId\":"
                        + a.oficina.getId()
                        + "}"),
                404)));

    em.clear();
    assertThat(em.find(OrdemDeServico.class, b.os.getId())).isNotNull();
    assertThat(em.find(OrdemDeServico.class, b.os.getId()).getStatus())
        .isEqualTo(StatusOrdemDeServico.ABERTA);
    assertThat(em.find(Pagamento.class, b.pagamento.getId()).getObs()).isNull();
    assertThat(em.find(RegistroPagamento.class, b.registro.getId())).isNotNull();
    assertThat(em.find(ItemOsPeca.class, b.peca.getId()).getNome()).isEqualTo("PECA B");
    assertThat(em.find(MaoObra.class, b.maoObra.getId()).getDescricao()).isEqualTo("MAO DE OBRA B");
    assertThat(em.find(Cliente.class, b.cliente.getId())).isNotNull();
    assertThat(em.find(Veiculo.class, b.veiculo.getId())).isNotNull();
    assertThat(em.find(Mecanico.class, b.mecanico.getId())).isNotNull();
    assertThat(em.find(Unidade.class, b.unidade.getId())).isNotNull();
    assertThat(em.find(Usuario.class, b.gerente.getId())).isNotNull();
  }

  @Test
  @DisplayName("nao e possivel criar ou vincular dados da oficina A usando IDs da oficina B")
  void naoMisturaOficinasAoCriarOuVincular() throws Exception {
    executar(
        List.of(
            new Caso(
                "POST OS com veiculo de B",
                json(
                    post("/api/ordens-servico"),
                    "{\"unidadeId\":"
                        + a.unidade.getId()
                        + ",\"veiculoId\":"
                        + b.veiculo.getId()
                        + "}"),
                404),
            new Caso(
                "POST OS com unidade de B",
                json(
                    post("/api/ordens-servico"),
                    "{\"unidadeId\":"
                        + b.unidade.getId()
                        + ",\"veiculoId\":"
                        + a.veiculo.getId()
                        + "}"),
                404),
            new Caso(
                "POST OS com cliente de B",
                json(
                    post("/api/ordens-servico"),
                    "{\"unidadeId\":"
                        + a.unidade.getId()
                        + ",\"veiculoId\":"
                        + a.veiculo.getId()
                        + ",\"clienteId\":"
                        + b.cliente.getId()
                        + "}"),
                404),
            new Caso(
                "POST OS com mecanico de B",
                json(
                    post("/api/ordens-servico"),
                    "{\"unidadeId\":"
                        + a.unidade.getId()
                        + ",\"veiculoId\":"
                        + a.veiculo.getId()
                        + ",\"mecanicoId\":"
                        + b.mecanico.getId()
                        + "}"),
                404),
            new Caso(
                "PATCH atribuir mecanico de B a OS de A",
                json(
                    patch("/api/ordens-servico/" + a.os.getId() + "/mecanico"),
                    "{\"mecanicoId\":" + b.mecanico.getId() + "}"),
                404),
            new Caso(
                "PATCH atribuir cliente de B a OS de A",
                json(
                    patch("/api/ordens-servico/" + a.os.getId() + "/cliente"),
                    "{\"clienteId\":" + b.cliente.getId() + "}"),
                404),
            new Caso(
                "POST mao de obra em OS de B",
                json(
                    post("/api/mao-obra"),
                    "{\"osId\":" + b.os.getId() + ",\"valor\":100,\"descricao\":\"INVADIDA\"}"),
                404),
            new Caso(
                "POST peca em OS de B",
                json(
                    post("/api/itens-os-peca"),
                    "{\"osId\":"
                        + b.os.getId()
                        + ",\"nome\":\"INVADIDA\",\"quantidade\":1,\"valorUnitario\":100}"),
                404),
            new Caso(
                "PUT vincular peca avulsa de A a OS de B",
                comToken(put("/api/itens-os-peca/" + a.pecaAvulsa.getId() + "/os/" + b.os.getId())),
                404),
            new Caso(
                "PUT vincular peca avulsa de B a OS de A",
                comToken(put("/api/itens-os-peca/" + b.pecaAvulsa.getId() + "/os/" + a.os.getId())),
                404),
            new Caso(
                "POST registro de pagamento em pagamento de B",
                json(
                    post("/api/registros-pagamento"),
                    "{\"pagamentoId\":"
                        + b.pagamento.getId()
                        + ",\"valor\":100,\"meioPagamento\":\"PIX\"}"),
                404),
            new Caso(
                "POST unidade na oficina B",
                json(
                    post("/api/unidades/oficina/" + b.oficina.getId()),
                    "{\"nome\":\"INVADIDA\",\"endereco\":\"RUA X\"}"),
                403),
            new Caso(
                "POST usuario na oficina B",
                json(
                    post("/api/usuarios"),
                    "{\"nome\":\"X\",\"username\":\"invasor\",\"password\":\"senha12345\","
                        + "\"role\":\"MECANICO\",\"oficinaId\":"
                        + b.oficina.getId()
                        + "}"),
                403),
            new Caso(
                "PUT mover usuario de A para a oficina B",
                json(
                    put("/api/usuarios/" + a.mecanicoUsuario.getId()),
                    "{\"nome\":\"X\",\"username\":\"mecanico.a\",\"role\":\"MECANICO\","
                        + "\"oficinaId\":"
                        + b.oficina.getId()
                        + "}"),
                403),
            new Caso(
                "POST usuario ADMIN por gerente",
                json(
                    post("/api/usuarios"),
                    "{\"nome\":\"X\",\"username\":\"novo.admin\",\"password\":\"senha12345\","
                        + "\"role\":\"ADMIN\"}"),
                403)));

    em.clear();
    assertThat(em.find(OrdemDeServico.class, a.os.getId()).getMecanico().getId())
        .isEqualTo(a.mecanico.getId());
    assertThat(em.find(ItemOsPeca.class, a.pecaAvulsa.getId()).getOrdemDeServico()).isNull();
    assertThat(em.find(ItemOsPeca.class, b.pecaAvulsa.getId()).getOrdemDeServico()).isNull();
    assertThat(em.find(Usuario.class, a.mecanicoUsuario.getId()).getOficina().getId())
        .isEqualTo(a.oficina.getId());
  }

  @Test
  @DisplayName("listagens da oficina A nunca incluem registros da oficina B")
  void listagensSaoRestritasAOficinaDoUsuario() throws Exception {
    MockMvc mvc = mockMvc();

    String os = corpo(mvc, "/api/ordens-servico");
    assertThat(os).contains("\"id\":" + a.os.getId()).doesNotContain("OFICINA B");

    String pecas = corpo(mvc, "/api/itens-os-peca");
    assertThat(pecas).contains("PECA A").doesNotContain("PECA B");

    String unidades = corpo(mvc, "/api/unidades");
    assertThat(unidades).contains("UNIDADE A").doesNotContain("UNIDADE B");

    String clientes = corpo(mvc, "/api/clientes");
    assertThat(clientes).contains("CLIENTE A").doesNotContain("CLIENTE B");

    String veiculos = corpo(mvc, "/api/veiculos");
    assertThat(veiculos).contains("AAA0001").doesNotContain("AAA0002");

    String mecanicos = corpo(mvc, "/api/mecanicos");
    assertThat(mecanicos).contains("MECANICO A").doesNotContain("MECANICO B");

    String usuarios = corpo(mvc, "/api/usuarios");
    assertThat(usuarios).contains("gerente.a").doesNotContain("gerente.b");

    String buscaClientes = corpo(mvc, "/api/clientes/buscar?termo=CLIENTE");
    assertThat(buscaClientes).contains("CLIENTE A").doesNotContain("CLIENTE B");

    String porStatus = corpo(mvc, "/api/ordens-servico/status/ABERTA");
    assertThat(porStatus).contains("\"id\":" + a.os.getId()).doesNotContain("OFICINA B");
  }

  private String corpo(MockMvc mvc, String url) throws Exception {
    var resp = mvc.perform(comToken(get(url))).andReturn().getResponse();
    assertThat(resp.getStatus()).as("GET " + url).isEqualTo(200);
    return resp.getContentAsString();
  }
}
