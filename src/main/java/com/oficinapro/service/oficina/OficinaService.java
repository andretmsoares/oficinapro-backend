package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;

import java.util.List;

public interface OficinaService {

    List<OficinaResponseDTO> listar();

    OficinaResponseDTO buscarPorId(Long id);

    OficinaResponseDTO criar(OficinaRequestDTO request);

    OficinaResponseDTO atualizar(Long id, OficinaRequestDTO request);

    void deletar(Long id);
}