package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.model.Veiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

  Page<Veiculo> findByOficinaId(Long oficinaId, Pageable pageable);

  Optional<Veiculo> findByPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlacaAndIdNot(Long oficinaId, String placa, Long id);

  Integer countByOficinaId(Long oficinaId);

  // LEFT JOIN a partir de Oficina para que oficina sem nenhum registro apareça
  // com zero, em vez de sumir do relatório.
  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(v.id))"
          + " from Oficina o left join Veiculo v on v.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
