package com.oficinapro.repository;

import com.oficinapro.model.Oficina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OficinaRepository extends JpaRepository<Oficina, Long> {

    Optional<Oficina> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}