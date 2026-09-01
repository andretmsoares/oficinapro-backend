package com.oficinapro.repository;

import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroPagamentoRepository {

    List<Pagamento> findByPagamentoId(Long pagamentoId);
    List<Pagamento> findById(Long id);
    List<Pagamento> findByMeioPagamento(MeioPagamento meioPagamento);
}
