package com.oficinapro.repository;

import com.oficinapro.model.Pessoa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface PessoaCrudRepository<T extends Pessoa> extends JpaRepository<T, Long> {
    Page<T> findByOficinaId(Long oficinaId, Pageable pageable);
    Optional<T> findByNome(String nome);
    Optional<T> findByDocumento(String documento);
}