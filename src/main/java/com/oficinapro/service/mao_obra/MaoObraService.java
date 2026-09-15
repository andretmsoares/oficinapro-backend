package com.oficinapro.service.mao_obra;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import java.util.List;

public interface MaoObraService {
  List<MaoObraResponseDTO> listarPorOrdemServico(Long osId);

  MaoObraResponseDTO buscarPorId(Long id);

  MaoObraResponseDTO criar(MaoObraRequestDTO request);

  MaoObraResponseDTO atualizar(Long id, MaoObraRequestDTO request);

  void deletar(Long id);
}
