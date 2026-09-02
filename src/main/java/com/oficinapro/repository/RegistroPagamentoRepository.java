package com.oficinapro.repository;

import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.model.RegistroPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroPagamentoRepository extends JpaRepository<RegistroPagamento, Long> {

    List<RegistroPagamento> findByPagamentoId(Long pagamentoId);
    Optional<RegistroPagamento> findById(Long id);
    List<RegistroPagamento> findByMeioPagamento(MeioPagamento meioPagamento);
}
