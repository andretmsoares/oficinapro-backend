package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.model.Oficina;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OficinaService {

  List<OficinaResponseDTO> listar();

  OficinaResponseDTO buscarPorId(Long id);

  Oficina buscarPorEntidadeId(Long id);

  Page<OficinaResponseDTO> buscar(String search, Pageable pageable);

  boolean existsById(Long id);

  OficinaResponseDTO criar(OficinaRequestDTO request);

  OficinaResponseDTO atualizar(Long id, OficinaRequestDTO request);

  void deletar(Long id);

  void desativar(Long id);

  void ativar(Long id);
}
