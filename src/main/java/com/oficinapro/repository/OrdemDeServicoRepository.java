package com.oficinapro.repository;

import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.enums.StatusOrdemDeServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

    List<OrdemDeServico> findByOficinaId(Long oficinaId);

    List<OrdemDeServico> findByStatus(StatusOrdemDeServico status);

    List<OrdemDeServico> findByVeiculoId(Long veiculoId);

    List<OrdemDeServico> findByClienteId(Long clienteId);

    List<OrdemDeServico> findByMecanicoId(Long mecanicoId);

    List<OrdemDeServico> findByUnidadeId(Long unidadeId);

    List<OrdemDeServico> findByOficinaIdAndStatus(Long oficinaId, StatusOrdemDeServico status);
}