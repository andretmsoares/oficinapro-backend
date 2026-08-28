package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.model.Oficina;

import java.util.List;

public interface OficinaService {

    List<OficinaResponseDTO> listar();

    OficinaResponseDTO buscarPorId(Long id);

    Oficina buscarPorEntidadeId(Long id);

    boolean existsById(Long id);

    OficinaResponseDTO criar(OficinaRequestDTO request);

    OficinaResponseDTO atualizar(Long id, OficinaRequestDTO request);

    void deletar(Long id);
}