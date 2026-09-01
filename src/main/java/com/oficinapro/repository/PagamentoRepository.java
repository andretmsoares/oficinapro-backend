package com.oficinapro.repository;

import com.oficinapro.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Pagamento findByOrdemDeServicoId(Long osId);
    Optional<Pagamento> findById(Long id);
}
