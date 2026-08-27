package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;

import java.util.List;

public interface UnidadeService {

    List<UnidadeResponseDTO> listar();

    UnidadeResponseDTO buscarPorId(Long id);

    List<UnidadeResponseDTO> listarPorOficina(Long oficinaId);

    UnidadeResponseDTO criar(Long oficinaId, UnidadeRequestDTO request);

    UnidadeResponseDTO atualizar(Long id, UnidadeRequestDTO request);

    void deletar(Long id);
}