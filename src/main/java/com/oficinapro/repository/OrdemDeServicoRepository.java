package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.model.OrdemDeServico;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  List<OrdemDeServico> findByOficinaId(Long oficinaId);

  /**
   * Listagem paginada com busca no servidor, sobre TODAS as OS da oficina. {@code status} vazio =
   * qualquer status; {@code termo} vazio = sem busca. O termo casa com placa, nome do cliente,
   * status ou número da OS ({@code osId}, -1 quando o termo não é número).
   */
  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  @Query(
      value =
          """
          select os from OrdemDeServico os
          left join os.cliente c
          where os.oficina.id = :oficinaId
            and (:status = '' or cast(os.status as string) = :status)
            and (:termo = ''
                 or lower(os.veiculo.placa) like :placaLike escape '!'
                 or lower(c.nome) like :termoLike escape '!'
                 or lower(cast(os.status as string)) like :termoLike escape '!'
                 or os.id = :osId)
          """,
      countQuery =
          """
          select count(os) from OrdemDeServico os
          left join os.cliente c
          where os.oficina.id = :oficinaId
            and (:status = '' or cast(os.status as string) = :status)
            and (:termo = ''
                 or lower(os.veiculo.placa) like :placaLike escape '!'
                 or lower(c.nome) like :termoLike escape '!'
                 or lower(cast(os.status as string)) like :termoLike escape '!'
                 or os.id = :osId)
          """)
  Page<OrdemDeServico> buscar(
      @Param("oficinaId") Long oficinaId,
      @Param("status") String status,
      @Param("termo") String termo,
      @Param("termoLike") String termoLike,
      @Param("placaLike") String placaLike,
      @Param("osId") Long osId,
      Pageable pageable);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  Page<OrdemDeServico> findByOficinaIdAndVeiculoId(Long oficinaId, Long veiculoId, Pageable p);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  Page<OrdemDeServico> findByOficinaIdAndMecanicoId(Long oficinaId, Long mecanicoId, Pageable p);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  Page<OrdemDeServico> findByOficinaIdAndUnidadeId(Long oficinaId, Long unidadeId, Pageable p);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  Page<OrdemDeServico> findByOficinaIdAndClienteId(Long oficinaId, Long clienteId, Pageable p);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
  Page<OrdemDeServico> findByOficinaIdAndStatus(
      Long oficinaId, StatusOrdemDeServico status, Pageable p);

  long countByOficinaIdAndStatus(Long oficinaId, StatusOrdemDeServico status);

  List<OrdemDeServico> findByStatus(StatusOrdemDeServico status);

  List<OrdemDeServico> findByVeiculoId(Long veiculoId);

  List<OrdemDeServico> findByClienteId(Long clienteId);

  List<OrdemDeServico> findByMecanicoId(Long mecanicoId);

  List<OrdemDeServico> findByUnidadeId(Long unidadeId);

  @EntityGraph(attributePaths = {"oficina", "unidade", "veiculo", "cliente", "mecanico"})
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
