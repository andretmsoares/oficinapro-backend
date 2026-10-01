package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.model.Mecanico;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MecanicoRepository extends PessoaCrudRepository<Mecanico> {
  long countByOficinaId(Long oficinaId);

  @Query(
      """
      select m from Mecanico m
      where m.oficina.id = :oficinaId
        and (lower(m.nome) like lower(concat('%', :termo, '%'))
             or m.documento like concat('%', :termo, '%')
             or m.telefone like concat('%', :termo, '%'))
      """)
  Page<Mecanico> buscar(
      @Param("oficinaId") Long oficinaId, @Param("termo") String termo, Pageable pageable);

  // LEFT JOIN a partir de Oficina para que oficina sem nenhum registro apareça
  // com zero, em vez de sumir do relatório.
  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(m.id))"
          + " from Oficina o left join Mecanico m on m.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
