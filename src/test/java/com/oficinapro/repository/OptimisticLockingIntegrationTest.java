package com.oficinapro.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Veiculo;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lock otimista em {@code Pagamento} e {@code OrdemDeServico}.
 *
 * <p>Sem {@code @Version}, o fluxo "ler valorPago → somar → gravar" perdia atualizações: dois
 * recebimentos simultâneos numa OS de R$ 1.000 podiam gravar 600 em vez de 1.000, com os dois
 * registros de pagamento existindo e o saldo divergindo do histórico.
 *
 * <p>Precisa ser teste de integração: {@code @Version} é comportamento do provedor JPA e não pode
 * ser verificado com mock.
 *
 * <p>O conflito é simulado de forma determinística com uma entidade <b>destacada</b> do contexto de
 * persistência: ela guarda a versão antiga, alguém grava no meio do caminho, e a tentativa de
 * mesclar a cópia velha falha. Isso reproduz exatamente o cenário de dois usuários simultâneos sem
 * depender de threads, que tornariam o teste intermitente.
 *
 * <p>Usa {@code @SpringBootTest} em vez de {@code @DataJpaTest} para reaproveitar o contexto já
 * carregado pelos outros testes de integração do projeto.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OptimisticLockingIntegrationTest {

  @Autowired private EntityManager entityManager;
  @Autowired private PagamentoRepository pagamentoRepository;
  @Autowired private OrdemDeServicoRepository ordemDeServicoRepository;

  private OrdemDeServico os;
  private Pagamento pagamento;

  @BeforeEach
  void prepararCenario() {
    Oficina oficina = new Oficina();
    oficina.setNome("Oficina Central");
    oficina.setCnpj("12345678000195");
    oficina.setTelefone("83999998888");
    oficina.setAtivo(true);
    entityManager.persist(oficina);

    Unidade unidade = new Unidade();
    unidade.setOficina(oficina);
    unidade.setNome("Unidade Centro");
    unidade.setEndereco("Rua das Flores, 100");
    entityManager.persist(unidade);

    Veiculo veiculo = new Veiculo();
    veiculo.setOficina(oficina);
    veiculo.setModelo("Civic");
    veiculo.setMarca("Honda");
    veiculo.setAno(2020);
    veiculo.setPlaca("ABC1D23");
    entityManager.persist(veiculo);

    os = new OrdemDeServico();
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);
    os.setStatus(StatusOrdemDeServico.EM_EXECUCAO);
    os.setDataAbertura(LocalDateTime.now());
    os.setValorTotal(new BigDecimal("1000.00"));
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(new BigDecimal("1000.00"));
    entityManager.persist(os);

    pagamento = new Pagamento();
    pagamento.setOrdemDeServico(os);
    pagamento.setValorPago(BigDecimal.ZERO);
    pagamento.setObs("");
    pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);
    entityManager.persist(pagamento);

    entityManager.flush();
    entityManager.clear();
  }

  // ------------------------------------------------------------------
  // O campo version precisa existir e evoluir
  // ------------------------------------------------------------------

  @Test
  @DisplayName("Pagamento nasce com version preenchida")
  void pagamentoNasceComVersion() {
    Pagamento salvo = pagamentoRepository.findById(pagamento.getId()).orElseThrow();

    assertThat(salvo.getVersion()).as("sem version não há lock otimista").isNotNull();
  }

  @Test
  @DisplayName("OrdemDeServico nasce com version preenchida")
  void osNasceComVersion() {
    OrdemDeServico salva = ordemDeServicoRepository.findById(os.getId()).orElseThrow();

    assertThat(salva.getVersion()).isNotNull();
  }

  @Test
  @DisplayName("version do Pagamento é incrementada a cada atualização")
  void versionDoPagamentoIncrementa() {
    Pagamento carregado = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
    Long versaoInicial = carregado.getVersion();

    carregado.setValorPago(new BigDecimal("100.00"));
    pagamentoRepository.saveAndFlush(carregado);

    assertThat(carregado.getVersion()).isGreaterThan(versaoInicial);
    assertThat(carregado.getValorPago()).isEqualByComparingTo("100.00");
  }

  // ------------------------------------------------------------------
  // O conflito em si
  // ------------------------------------------------------------------

  @Test
  @DisplayName("dois recebimentos concorrentes: o segundo falha em vez de sobrescrever o primeiro")
  void recebimentoConcorrenteFalhaNoSegundo() {
    // Gerente B abre a tela e carrega o pagamento (valorPago = 0, version = N).
    Pagamento visaoDoGerenteB = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
    entityManager.detach(visaoDoGerenteB);

    // Gerente A registra R$ 400 e grava primeiro — a version no banco avança.
    Pagamento visaoDoGerenteA = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
    visaoDoGerenteA.setValorPago(new BigDecimal("400.00"));
    pagamentoRepository.saveAndFlush(visaoDoGerenteA);

    // Gerente B grava R$ 600 partindo do estado que leu antes.
    visaoDoGerenteB.setValorPago(new BigDecimal("600.00"));

    assertThatThrownBy(() -> pagamentoRepository.saveAndFlush(visaoDoGerenteB))
        .as(
            "Sem lock otimista, os R$ 400 do gerente A seriam silenciosamente"
                + " sobrescritos pelos R$ 600 do gerente B: os dois registros de"
                + " pagamento existiriam, mas o saldo diria 600.")
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  @Test
  @DisplayName("duas alterações concorrentes no valor da OS: a segunda falha")
  void alteracaoConcorrenteDaOsFalha() {
    // Usuário B carrega a OS com valorTotal = 1000.
    OrdemDeServico visaoB = ordemDeServicoRepository.findById(os.getId()).orElseThrow();
    entityManager.detach(visaoB);

    // Usuário A lança uma peça e o total sobe para 1200.
    OrdemDeServico visaoA = ordemDeServicoRepository.findById(os.getId()).orElseThrow();
    visaoA.setValorTotal(new BigDecimal("1200.00"));
    visaoA.setValorComDesconto(new BigDecimal("1200.00"));
    ordemDeServicoRepository.saveAndFlush(visaoA);

    // Usuário B aplica desconto sobre o total desatualizado.
    visaoB.setDesconto(new BigDecimal("100.00"));
    visaoB.setValorComDesconto(new BigDecimal("900.00"));

    assertThatThrownBy(() -> ordemDeServicoRepository.saveAndFlush(visaoB))
        .as("aplicar desconto sobre um total desatualizado produziria valor errado")
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  // ------------------------------------------------------------------
  // O lock não pode atrapalhar o fluxo normal
  // ------------------------------------------------------------------

  @Test
  @DisplayName("parcelas sequenciais no mesmo pagamento funcionam normalmente")
  void parcelasSequenciaisFuncionam() {
    Pagamento primeira = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
    primeira.setValorPago(new BigDecimal("300.00"));
    pagamentoRepository.saveAndFlush(primeira);

    Pagamento segunda = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
    segunda.setValorPago(segunda.getValorPago().add(new BigDecimal("700.00")));

    assertThatCode(() -> pagamentoRepository.saveAndFlush(segunda))
        .as("o lock otimista não pode quebrar o pagamento parcelado, que é o fluxo normal")
        .doesNotThrowAnyException();

    assertThat(segunda.getValorPago()).isEqualByComparingTo("1000.00");
  }
}
