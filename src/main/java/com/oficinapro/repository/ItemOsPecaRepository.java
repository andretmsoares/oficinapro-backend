package com.oficinapro.repository;

import com.oficinapro.model.ItemOsPeca;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOsPecaRepository extends JpaRepository<ItemOsPeca, Long> {
  List<ItemOsPeca> findByOrdemDeServicoId(Long osId);
}
