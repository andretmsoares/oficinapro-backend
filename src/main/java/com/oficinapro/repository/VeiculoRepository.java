package com.oficinapro.repository;

import com.oficinapro.model.Veiculo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

  Page<Veiculo> findByOficinaId(Long oficinaId, Pageable pageable);

  Optional<Veiculo> findByPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlaca(Long oficinaId, String placa);

  boolean existsByOficinaIdAndPlacaAndIdNot(Long oficinaId, String placa, Long id);
}
