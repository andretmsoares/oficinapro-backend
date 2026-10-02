package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.model.OrdemDeServico;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface OrdemDeServicoService {

  // A negação de acesso é sinalizada por org.springframework.security.access.AccessDeniedException,
  // que é unchecked. Declarar "throws" aqui já apontou para java.nio.file.AccessDeniedException por
  // engano — uma exceção checked de I/O, sem relação com autorização — e isso forçou try/catch no
  // controller, que acabou engolindo o erro e devolvendo 200 com corpo vazio em vez de 403.
  /**
   * Lista as OS da oficina do usuário, com busca no servidor sobre todos os registros.
   *
   * @param termo placa, nome do cliente, status ou número da OS; vazio = sem busca
   * @param status filtro de status; nulo = qualquer
   */
  Page<OrdemDeServicoResponseDTO> listar(
      String termo, StatusOrdemDeServico status, Pageable pageable);

  Page<OrdemDeServicoResponseDTO> listarPorVeiculo(Long veiculoId, Pageable pageable);

  Page<OrdemDeServicoResponseDTO> listarPorMecanico(Long mecanicoId, Pageable pageable);

  Page<OrdemDeServicoResponseDTO> listarPorUnidade(Long unidadeId, Pageable pageable);

  Page<OrdemDeServicoResponseDTO> listarPorCliente(Long clienteId, Pageable pageable);

  Page<OrdemDeServicoResponseDTO> listarPorStatus(StatusOrdemDeServico status, Pageable pageable);

  /** Contagem direta no banco (o dashboard não precisa carregar as OS para contar). */
  long contarPorStatus(StatusOrdemDeServico status);

  OrdemDeServicoResponseDTO buscarPorId(Long id);

  OrdemDeServico buscarPorEntidadeId(Long id);

  OrdemDeServicoResponseDTO aplicarDesconto(Long id, BigDecimal desconto);

  OrdemDeServicoResponseDTO recalcularValorTotal(Long id, BigDecimal novoValorTotal);

  OrdemDeServicoResponseDTO atualizarStatus(Long id, AtualizarStatusOSRequestDTO dto);

  OrdemDeServicoResponseDTO atribuirMecanico(Long id, AtribuirMecanicoRequestDTO dto);

  OrdemDeServicoResponseDTO atribuirCliente(Long id, AtribuirClienteRequestDTO dto);

  OrdemDeServicoResponseDTO criar(OrdemDeServicoRequestDTO request);

  OrdemDeServicoResponseDTO atualizar(Long id, OrdemDeServicoRequestDTO request);

  void deletar(Long id);

  List<FluxoMensalOSResponseDTO> fluxoMensal(int mes, int ano);

  @Transactional(readOnly = true)
  byte[] gerarPdf(Long id);

  @Transactional(readOnly = true)
  byte[] gerarComprovantePagamento(Long id);
}
