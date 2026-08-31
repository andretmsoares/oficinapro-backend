package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.model.Unidade;

import java.util.List;

public interface UnidadeService {

    List<UnidadeResponseDTO> listar();

    UnidadeResponseDTO buscarPorId(Long id);

    Unidade buscarPorEntidadeId(Long id);

    List<UnidadeResponseDTO> listarPorOficina(Long oficinaId);

    UnidadeResponseDTO criar(Long oficinaId, UnidadeRequestDTO request);

    UnidadeResponseDTO atualizar(Long id, UnidadeRequestDTO request);

    void deletar(Long id);
}