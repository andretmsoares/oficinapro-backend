package com.oficinapro.service.veiculo;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.model.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VeiculoService {

    Page<VeiculoResponseDTO> listar(Pageable pageable);

    Page<VeiculoResponseDTO> listarPorOficinaId(Long oficinaId, Pageable pageable);

    Veiculo buscarPorEntidadeId(Long id);

    VeiculoResponseDTO buscarPorId(Long id);

    VeiculoResponseDTO buscarPorPlaca(String placa);

    VeiculoResponseDTO criar(VeiculoRequestDTO request);

    VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO request);

    void deletar(Long id);
}