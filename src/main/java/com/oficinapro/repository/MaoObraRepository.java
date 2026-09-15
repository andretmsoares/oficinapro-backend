package com.oficinapro.repository;

import com.oficinapro.model.MaoObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaoObraRepository extends JpaRepository<MaoObra, Long> {
    List<MaoObra> findByOrdemDeServicoId(Long osId);
}