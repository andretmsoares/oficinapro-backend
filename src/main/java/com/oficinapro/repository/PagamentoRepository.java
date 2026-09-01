package com.oficinapro.repository;

import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository {

    List<Pagamento> findByOsId(Long osId);
    List<Pagamento> findById(Long id);
}
