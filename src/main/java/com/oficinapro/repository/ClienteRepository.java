package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.model.Cliente;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends PessoaCrudRepository<Cliente> {
  Integer countByOficinaId(Long oficinaId);

  @Query(
      """
      select c from Cliente c
      where c.oficina.id = :oficinaId
        and (lower(c.nome) like lower(concat('%', :termo, '%'))
             or c.documento like concat('%', :termo, '%')
             or c.telefone like concat('%', :termo, '%'))
      """)
  Page<Cliente> buscar(
      @Param("oficinaId") Long oficinaId, @Param("termo") String termo, Pageable pageable);

  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(c.id))"
          + " from Oficina o left join Cliente c on c.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
