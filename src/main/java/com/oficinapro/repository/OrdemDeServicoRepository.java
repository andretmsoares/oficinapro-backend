package com.oficinapro.repository;

import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.enums.StatusOrdemDeServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

    List<OrdemDeServico> findByOficinaId(Long oficinaId);

    List<OrdemDeServico> findByStatus(StatusOrdemDeServico status);

    List<OrdemDeServico> findByVeiculoId(Long veiculoId);

    List<OrdemDeServico> findByClienteId(Long clienteId);

    List<OrdemDeServico> findByMecanicoId(Long mecanicoId);

    List<OrdemDeServico> findByUnidadeId(Long unidadeId);

    List<OrdemDeServico> findByOficinaIdAndStatus(Long oficinaId, StatusOrdemDeServico status);

    @Query("""
        SELECT os
        FROM OrdemDeServico os
        WHERE os.oficina.id = :oficinaId
        AND (
            (os.dataAbertura >= :inicio AND os.dataAbertura < :fim)
            OR
            (os.dataFechamento >= :inicio AND os.dataFechamento < :fim)
        )
    """)
    List<OrdemDeServico> findFluxoMensal(
            @Param("oficinaId") Long oficinaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}