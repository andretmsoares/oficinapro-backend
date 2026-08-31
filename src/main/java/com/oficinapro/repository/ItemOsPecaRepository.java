package com.oficinapro.repository;

import com.oficinapro.model.ItemOsPeca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemOsPecaRepository extends JpaRepository<ItemOsPeca, Long> {
    List<ItemOsPeca> findByOrdemDeServicoId(Long osId);
}