package com.oficinapro.repository;

import com.oficinapro.model.Unidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

  List<Unidade> findByOficinaId(Long oficinaId);

  /**
   * Unicidade de endereço é por oficina, não global: duas oficinas diferentes podem operar no mesmo
   * endereço, e perguntar globalmente vazava a existência de unidades de outros tenants na forma de
   * um 409.
   */
  boolean existsByOficinaIdAndEndereco(Long oficinaId, String endereco);

  boolean existsByOficinaIdAndEnderecoAndIdNot(Long oficinaId, String endereco, Long id);
}
