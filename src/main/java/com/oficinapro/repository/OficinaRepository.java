package com.oficinapro.repository;

import com.oficinapro.model.Oficina;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OficinaRepository extends JpaRepository<Oficina, Long> {

  Optional<Oficina> findByCnpj(String cnpj);

  boolean existsByCnpj(String cnpj);
}
