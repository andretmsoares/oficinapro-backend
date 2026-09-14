package com.oficinapro.repository;

import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Pagamento findByOrdemDeServicoId(Long osId);
    Optional<Pagamento> findById(Long id);
    List<Pagamento> findByOrdemDeServicoOficinaId(Long oficinaId);
    List<Pagamento> findByOrdemDeServicoOficinaIdAndStatus(
            Long oficinaId,
            StatusPagamento status
    );
    @Query("""
    SELECT COALESCE(
        SUM(p.ordemDeServico.valorComDesconto - COALESCE(p.valorPago, 0)),
        0
    )
    FROM Pagamento p
    WHERE p.ordemDeServico.oficina.id = :oficinaId
      AND p.status IN :statuses
""")
    BigDecimal calcularValorParaReceber(
            @Param("oficinaId") Long oficinaId,
            @Param("statuses") List<StatusPagamento> statuses
    );
}
