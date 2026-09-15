package com.oficinapro.repository;

import com.oficinapro.model.MaoObra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaoObraRepository extends JpaRepository<MaoObra, Long> {
  List<MaoObra> findByOrdemDeServicoId(Long osId);
}
