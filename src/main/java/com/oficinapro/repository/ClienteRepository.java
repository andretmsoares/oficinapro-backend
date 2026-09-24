package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.model.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends PessoaCrudRepository<Cliente> {
  Integer countByOficinaId(Long oficinaId);

  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(c.id))"
          + " from Oficina o left join Cliente c on c.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
