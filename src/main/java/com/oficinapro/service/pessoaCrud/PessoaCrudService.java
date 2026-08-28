package com.oficinapro.service.pessoaCrud;

import com.oficinapro.model.Pessoa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PessoaCrudService<C, U, RES, T extends Pessoa> {
    Page<RES> listar(Pageable pageable);
    Page<RES> listarPorOficinaId(Long oficinaId, Pageable pageable);
    T buscarPorEntidadeId(Long id);
    RES buscarPorId(Long id);
    RES buscarPorNome(String nome);
    RES buscarPorDocumento(String documento);
    RES criar(C request);
    RES atualizar(Long id, U request);
    void deletar(Long id);
}