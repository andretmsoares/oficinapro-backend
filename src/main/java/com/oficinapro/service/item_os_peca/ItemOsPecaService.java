package com.oficinapro.service.item_os_peca;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;

import java.util.List;

public interface ItemOsPecaService {

    List<ItemOsPecaResponseDTO> listarPorOrdemServico(Long osId);

    ItemOsPecaResponseDTO buscarPorId(Long id);

    ItemOsPecaResponseDTO criar(ItemOsPecaRequestDTO request);

    ItemOsPecaResponseDTO atualizar(Long id, ItemOsPecaUpdateRequestDTO request);

    void deletar(Long id);
}