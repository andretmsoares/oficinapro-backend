package com.oficinapro.repository;

import com.oficinapro.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    boolean existsByOficinaIdAndDocumento(Long oficinaId, String documento);

    boolean existsByOficinaIdAndDocumentoAndIdNot(Long oficinaId, String documento, Long id);
}