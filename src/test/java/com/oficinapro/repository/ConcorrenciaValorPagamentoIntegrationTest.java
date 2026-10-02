package com.oficinapro.repository;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corrida entre uma alteração do VALOR DA OS (desconto, peça, mão de obra) e um recebimento.
 *
 * <p>O {@code @Version} do pagamento já protegia dois recebimentos simultâneos, mas uma alteração
 * de valor da OS não escrevia no pagamento e, por isso, não conflitava com um recebimento: os dois
 * confirmavam e o valor pago podia ficar maior que o devido. A busca com {@code
 * OPTIMISTIC_FORCE_INCREMENT} faz o commit da alteração de valor também incrementar a versão do
 * pagamento.
 *
 * <p>O incremento forçado acontece no COMMIT, então este teste usa transações reais (sem
 * {@code @Transactional} na classe) e limpa o que cria.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcorrenciaValorPagamentoIntegrationTest {

  @Autowired private PlatformTransactionManager txManager;
  @Autowired private EntityManager em;
  @Autowired private PagamentoRepository pagamentoRepository;

  private TransactionTemplate tx;
  private TransactionTemplate txNova;

  private Long oficinaId;
  private Long osId;
  private Long pagamentoId;

  @BeforeEach
  void criarCenarioComCommit() {
    tx = new TransactionTemplate(txManager);
    txNova = new TransactionTemplate(txManager);
    txNova.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    tx.executeWithoutResult(
        status -> {
          Oficina oficina = new Oficina();
          oficina.setNome("OFICINA CONCORRENCIA");
          oficina.setCnpj("99999999000199");
          oficina.setAtivo(true);
          em.persist(oficina);

          Unidade unidade = new Unidade();
          unidade.setOficina(oficina);
          unidade.setNome("UNIDADE");
          unidade.setEndereco("RUA DA CONCORRENCIA, 1");
          em.persist(unidade);

          Veiculo veiculo = new Veiculo();
          veiculo.setOficina(oficina);
          veiculo.setModelo("CIVIC");
          veiculo.setCor("AZUL");
          veiculo.setPlaca("CNC1A23");
          em.persist(veiculo);

          OrdemDeServico os = new OrdemDeServico();
          os.setOficina(oficina);
          os.setUnidade(unidade);
          os.setVeiculo(veiculo);
          os.setStatus(StatusOrdemDeServico.EM_EXECUCAO);
          os.setDataAbertura(LocalDateTime.now());
          os.setValorTotal(new BigDecimal("1000"));
          os.setDesconto(BigDecimal.ZERO);
          os.setValorComDesconto(new BigDecimal("1000"));
          em.persist(os);

          Pagamento pagamento = new Pagamento();
          pagamento.setOrdemDeServico(os);
          pagamento.setValorPago(BigDecimal.ZERO);
          pagamento.setObs("");
          pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);
          em.persist(pagamento);

          em.flush();
          oficinaId = oficina.getId();
          osId = os.getId();
          pagamentoId = pagamento.getId();
        });
  }

  @AfterEach
  void limpar() {
    tx.executeWithoutResult(
        status -> {
          em.createQuery("delete from Pagamento p where p.id = :id")
              .setParameter("id", pagamentoId)
              .executeUpdate();
          em.createQuery("delete from OrdemDeServico o where o.id = :id")
              .setParameter("id", osId)
              .executeUpdate();
          em.createQuery("delete from Veiculo v where v.oficina.id = :id")
              .setParameter("id", oficinaId)
              .executeUpdate();
          em.createQuery("delete from Unidade u where u.oficina.id = :id")
              .setParameter("id", oficinaId)
              .executeUpdate();
          em.createQuery("delete from Oficina o where o.id = :id")
              .setParameter("id", oficinaId)
              .executeUpdate();
        });
  }

  @Test
  @DisplayName(
      "alteração de valor da OS concorrente com um recebimento: a segunda a confirmar falha")
  void alteracaoDeValorEntraEmConflitoComRecebimento() {
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    status -> {
                      // "Desconto/peça": lê o pagamento do jeito que recalcularStatus passa a ler
                      pagamentoRepository
                          .findByOrdemDeServicoIdParaAlterarValor(osId)
                          .orElseThrow();

                      // No meio do caminho, outro usuário registra um recebimento e confirma.
                      txNova.executeWithoutResult(
                          outra -> {
                            Pagamento p = pagamentoRepository.findById(pagamentoId).orElseThrow();
                            p.setValorPago(new BigDecimal("1000"));
                            p.setStatus(StatusPagamento.PAGA);
                            pagamentoRepository.saveAndFlush(p);
                          });
                    }))
        .as(
            "sem o incremento forçado, a alteração de valor confirmaria junto com o recebimento e"
                + " o valor pago poderia ficar maior que o devido")
        .isInstanceOf(OptimisticLockingFailureException.class);

    tx.executeWithoutResult(
        status ->
            assertThat(pagamentoRepository.findById(pagamentoId).orElseThrow().getValorPago())
                .as("o recebimento que confirmou primeiro permanece")
                .isEqualByComparingTo("1000"));
  }

  @Test
  @DisplayName("sem concorrência, a alteração de valor da OS confirma normalmente")
  void semConcorrenciaConfirma() {
    tx.executeWithoutResult(
        status -> pagamentoRepository.findByOrdemDeServicoIdParaAlterarValor(osId).orElseThrow());

    tx.executeWithoutResult(
        status ->
            assertThat(pagamentoRepository.findById(pagamentoId).orElseThrow().getVersion())
                .as("a versão avança mesmo sem nenhum campo do pagamento ter mudado")
                .isEqualTo(1L));
  }
}
