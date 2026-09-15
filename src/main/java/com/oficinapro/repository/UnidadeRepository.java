package com.oficinapro.repository;

import com.oficinapro.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByOficinaId(Long oficinaId);

    /**
     * Unicidade de endereço é por oficina, não global: duas oficinas diferentes podem
     * operar no mesmo endereço, e perguntar globalmente vazava a existência de unidades
     * de outros tenants na forma de um 409.
     */
    boolean existsByOficinaIdAndEndereco(Long oficinaId, String endereco);

    boolean existsByOficinaIdAndEnderecoAndIdNot(Long oficinaId, String endereco, Long id);
}
