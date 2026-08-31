package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.model.OrdemDeServico;

import java.util.List;

public interface OrdemDeServicoService {

    List<OrdemDeServicoResponseDTO> listar();

    List<OrdemDeServicoResponseDTO> listarPorVeiculo(Long veiculoId);

    List<OrdemDeServicoResponseDTO> listarPorMecanico(Long mecanicoId);

    List<OrdemDeServicoResponseDTO> listarPorUnidade(Long unidadeId);

    List<OrdemDeServicoResponseDTO> listarPorCliente(Long clienteId);

    List<OrdemDeServicoResponseDTO> listarPorOficina(Long oficinaId);

    OrdemDeServicoResponseDTO buscarPorId(Long id);

    OrdemDeServico buscarPorEntidadeId(Long id);

    OrdemDeServicoResponseDTO atualizarStatus(Long id, AtualizarStatusOSRequestDTO dto);

    OrdemDeServicoResponseDTO atribuirMecanico(Long id, AtribuirMecanicoRequestDTO dto);

    OrdemDeServicoResponseDTO atribuirCliente(Long id, AtribuirClienteRequestDTO dto);

    OrdemDeServicoResponseDTO criar(OrdemDeServicoRequestDTO request);

    OrdemDeServicoResponseDTO atualizar(Long id, OrdemDeServicoRequestDTO request);

    void deletar(Long id);
}
