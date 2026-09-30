package com.oficinapro.repository;

import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.model.RegistroPagamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroPagamentoRepository extends JpaRepository<RegistroPagamento, Long> {

  List<RegistroPagamento> findByPagamentoId(Long pagamentoId);

  List<RegistroPagamento> findByMeioPagamento(MeioPagamento meioPagamento);
}
