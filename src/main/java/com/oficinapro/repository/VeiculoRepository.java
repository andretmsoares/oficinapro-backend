package com.oficinapro.repository;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.model.Veiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

  Page<Veiculo> findByOficinaId(Long oficinaId, Pageable pageable);

  Optional<Veiculo> findByOficinaIdAndPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlacaAndIdNot(Long oficinaId, String placa, Long id);

  Integer countByOficinaId(Long oficinaId);

  @Query(
      """
      select v from Veiculo v
      where v.oficina.id = :oficinaId
        and (upper(v.placa) like concat('%', :placa, '%')
             or lower(v.modelo) like lower(concat('%', :termo, '%'))
             or lower(v.marca) like lower(concat('%', :termo, '%')))
      """)
  Page<Veiculo> buscar(
      @Param("oficinaId") Long oficinaId,
      @Param("termo") String termo,
      @Param("placa") String placa,
      Pageable pageable);

  // LEFT JOIN a partir de Oficina para que oficina sem nenhum registro apareça
  // com zero, em vez de sumir do relatório.
  @Query(
      "select new com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO(o.id, o.nome, count(v.id))"
          + " from Oficina o left join Veiculo v on v.oficina = o"
          + " group by o.id, o.nome order by o.nome")
  List<ContagemPorOficinaDTO> contarPorOficina();
}
