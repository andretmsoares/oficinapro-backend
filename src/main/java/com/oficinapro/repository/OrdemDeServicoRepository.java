package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.model.OrdemDeServico;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

  List<OrdemDeServico> findByOficinaId(Long oficinaId);

  List<OrdemDeServico> findByStatus(StatusOrdemDeServico status);

  List<OrdemDeServico> findByVeiculoId(Long veiculoId);

  List<OrdemDeServico> findByClienteId(Long clienteId);

  List<OrdemDeServico> findByMecanicoId(Long mecanicoId);

  List<OrdemDeServico> findByUnidadeId(Long unidadeId);

  List<OrdemDeServico> findByOficinaIdAndStatus(Long oficinaId, StatusOrdemDeServico status);

  @Query(
      """
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
      @Param("fim") LocalDateTime fim);

  long countByOficinaId(Long oficinaId);

  // LEFT JOIN a partir de Oficina para que oficina sem nenhum registro apareça
  // com zero, em vez de sumir do relatório.
  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(os.id))"
          + " from Oficina o left join OrdemDeServico os on os.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
