package com.oficinapro.service.item_os_peca;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemOsPecaService {

  List<ItemOsPecaResponseDTO> listarPorOrdemServico(Long osId);

  /**
   * Página das peças da oficina, com busca no servidor sobre todas elas.
   *
   * @param termo nome da peça ou parte do número da OS; vazio = sem busca
   * @param somenteAvulsas só as peças sem OS (candidatas a vincular)
   */
  Page<ItemOsPecaResponseDTO> listar(String termo, boolean somenteAvulsas, Pageable pageable);

  ItemOsPecaResponseDTO buscarPorId(Long id);

  ItemOsPecaResponseDTO criar(ItemOsPecaRequestDTO request);

  ItemOsPecaResponseDTO atualizar(Long id, ItemOsPecaUpdateRequestDTO request);

  void deletar(Long id);

  ItemOsPecaResponseDTO vincularOs(Long id, Long osId);

  ItemOsPecaResponseDTO desvincularOs(Long id);
}
