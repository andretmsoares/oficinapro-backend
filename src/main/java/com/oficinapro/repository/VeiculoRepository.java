package com.oficinapro.repository;

import com.oficinapro.model.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Page<Veiculo> findByOficinaId(Long oficinaId, Pageable pageable);

    Optional<Veiculo> findByPlaca(String placa);

    boolean existsByOficinaIdAndPlaca(Long oficinaId, String placa);

    boolean existsByOficinaIdAndPlacaAndIdNot(Long oficinaId, String placa, Long id);
}