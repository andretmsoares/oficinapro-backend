package com.oficinapro.repository;

import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Pagamento;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

  Pagamento findByOrdemDeServicoId(Long osId);

  /**
   * Usada quando o VALOR DA OS muda (desconto, peças, mão de obra). O OPTIMISTIC_FORCE_INCREMENT
   * faz o commit incrementar a versão do pagamento mesmo que nenhum campo dele mude, então um
   * recebimento concorrente (que também atualiza o pagamento) e a alteração de valor nunca
   * confirmam os dois: o segundo falha com conflito (409) em vez de deixar valor pago maior que o
   * devido.
   */
  @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
  @Query("select p from Pagamento p where p.ordemDeServico.id = :osId")
  Optional<Pagamento> findByOrdemDeServicoIdParaAlterarValor(@Param("osId") Long osId);

  List<Pagamento> findByOrdemDeServicoOficinaId(Long oficinaId);

  List<Pagamento> findByOrdemDeServicoOficinaIdAndStatus(Long oficinaId, StatusPagamento status);

  @Query(
      """
    SELECT COALESCE(
        SUM(p.ordemDeServico.valorComDesconto - COALESCE(p.valorPago, 0)),
        0
    )
    FROM Pagamento p
    WHERE p.ordemDeServico.oficina.id = :oficinaId
      AND p.status IN :statuses
""")
  BigDecimal calcularValorParaReceber(
      @Param("oficinaId") Long oficinaId, @Param("statuses") List<StatusPagamento> statuses);
}
