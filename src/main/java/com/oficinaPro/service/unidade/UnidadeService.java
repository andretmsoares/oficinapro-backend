package com.oficinapro.service;

import com.oficinapro.dto.unidade.UnidadeRequest;
import com.oficinapro.dto.unidade.UnidadeResponse;

import java.util.List;

public interface UnidadeService {

    List<UnidadeResponse> listar();

    UnidadeResponse buscarPorId(Long id);

    List<UnidadeResponse> listarPorOficina(Long oficinaId);

    UnidadeResponse criar(Long oficinaId, UnidadeRequest request);

    UnidadeResponse atualizar(Long id, UnidadeRequest request);

    void deletar(Long id);
}