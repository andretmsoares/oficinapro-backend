package com.oficinapro.repository;

import com.oficinapro.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByOficinaId(Long oficinaId);

    boolean existsByEndereco(String endereco);
}