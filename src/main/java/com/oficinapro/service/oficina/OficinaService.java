package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequest;
import com.oficinapro.dto.oficina.OficinaResponse;

import java.util.List;

public interface OficinaService {

    List<OficinaResponse> listar();

    OficinaResponse buscarPorId(Long id);

    OficinaResponse criar(OficinaRequest request);

    OficinaResponse atualizar(Long id, OficinaRequest request);

    void deletar(Long id);
}