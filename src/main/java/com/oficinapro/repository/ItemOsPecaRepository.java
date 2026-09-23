package com.oficinapro.repository;

import com.oficinapro.model.ItemOsPeca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOsPecaRepository extends JpaRepository<ItemOsPeca, Long> {

  List<ItemOsPeca> findByOrdemDeServicoIdAndOficinaId(Long osId, Long oficinaId);

  Optional<ItemOsPeca> findByIdAndOficinaId(Long id, Long oficinaId);
}
