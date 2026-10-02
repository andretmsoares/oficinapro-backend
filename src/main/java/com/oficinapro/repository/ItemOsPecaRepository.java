package com.oficinapro.repository;

import com.oficinapro.model.ItemOsPeca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemOsPecaRepository extends JpaRepository<ItemOsPeca, Long> {

  List<ItemOsPeca> findByOrdemDeServicoIdAndOficinaId(Long osId, Long oficinaId);

  Optional<ItemOsPeca> findByIdAndOficinaId(Long id, Long oficinaId);

  List<ItemOsPeca> findByOficinaId(Long oficinaId);

  /**
   * Listagem paginada com busca no servidor sobre TODAS as peças da oficina. {@code termo} casa com
   * o nome ou com parte do número da OS; {@code somenteAvulsas} restringe às peças sem OS (as que
   * podem ser vinculadas).
   */
  @EntityGraph(attributePaths = {"ordemDeServico"})
  @Query(
      value =
          """
          select i from ItemOsPeca i
          left join i.ordemDeServico os
          where i.oficina.id = :oficinaId
            and (:somenteAvulsas = false or os is null)
            and (:termo = ''
                 or lower(i.nome) like :termoLike escape '!'
                 or cast(os.id as string) like :termoLike escape '!')
          """,
      countQuery =
          """
          select count(i) from ItemOsPeca i
          left join i.ordemDeServico os
          where i.oficina.id = :oficinaId
            and (:somenteAvulsas = false or os is null)
            and (:termo = ''
                 or lower(i.nome) like :termoLike escape '!'
                 or cast(os.id as string) like :termoLike escape '!')
          """)
  Page<ItemOsPeca> buscar(
      @Param("oficinaId") Long oficinaId,
      @Param("somenteAvulsas") boolean somenteAvulsas,
      @Param("termo") String termo,
      @Param("termoLike") String termoLike,
      Pageable pageable);

  List<ItemOsPeca> findByOrdemDeServicoIdOrderByIdAsc(Long ordemDeServicoId);
}
