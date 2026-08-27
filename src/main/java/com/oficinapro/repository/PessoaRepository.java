package com.oficinapro.repository;

import com.oficinapro.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
    Optional<Pessoa> findByDocumento(String documento);

    boolean existsByOficinaIdAndDocumento(String oficinaId, String documento);
}
