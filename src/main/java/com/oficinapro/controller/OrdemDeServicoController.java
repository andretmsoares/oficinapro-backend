package com.oficinapro.controller;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Ordens de Serviço",
    description =
        "Ciclo de vida da ordem de serviço: criação, máquina de estados, atribuições,"
            + " desconto e consultas")
@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
public class OrdemDeServicoController {

  private final OrdemDeServicoService service;

  @Operation(
      summary = "Criar ordem de serviço",
      description =
          "Abre uma nova OS com status ABERTA e valores zerados. Veículo e oficina são"
              + " obrigatórios; cliente e mecânico são opcionais, pois o veículo pode"
              + " chegar antes de se identificar o proprietário ou de escalar um"
              + " mecânico. Um pagamento vazio (PAGAMENTO_PENDENTE) é criado"
              + " automaticamente junto com a OS.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "OS criada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de criar em oficina de outro tenant"),
    @ApiResponse(
        responseCode = "404",
        description = "Oficina, unidade, veículo, cliente ou mecânico informado não existe")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole( 'GERENTE')")
  public ResponseEntity<OrdemDeServicoResponseDTO> criar(
      @RequestBody @Valid OrdemDeServicoRequestDTO request) {
    OrdemDeServicoResponseDTO response = service.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Listar OS da própria oficina",
      description = "Retorna todas as ordens de serviço da oficina do usuário autenticado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listar() {
    return ResponseEntity.ok(service.listar());
  }

  @Operation(
      summary = "Buscar OS por ID",
      description =
          "Busca uma OS pelo ID, restrita à oficina do usuário autenticado. OS de outra"
              + " oficina responde 404, não 403.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OS encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS não encontrada, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<OrdemDeServicoResponseDTO> buscarPorId(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id) {
    return ResponseEntity.ok(service.buscarPorId(id));
  }

  @Operation(
      summary = "Fluxo mensal de OS de uma oficina",
      description =
          "Retorna, dia a dia do mês/ano informados, a contagem de OS abertas"
              + " (por dataAbertura) e finalizadas (por dataFechamento) na oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Fluxo mensal calculado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Mês ou ano inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @GetMapping("/fluxo-mensal")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<List<FluxoMensalOSResponseDTO>> fluxoMensal(
      @Parameter(description = "Mês, de 1 a 12") @RequestParam int mes,
      @Parameter(description = "Ano com 4 dígitos") @RequestParam int ano) {
    return ResponseEntity.ok(service.fluxoMensal(mes, ano));
  }

  @Operation(
      summary = "Atualizar dados cadastrais da OS",
      description =
          "Atualiza unidade, veículo, cliente, mecânico e observações da OS. Não é"
              + " permitido mover a OS para outra oficina — o oficinaId deve permanecer"
              + " o mesmo do cadastro original.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OS atualizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS, unidade, veículo, cliente ou mecânico não encontrado"),
    @ApiResponse(
        responseCode = "422",
        description = "Tentativa de trocar a oficina da OS, o que não é permitido")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<OrdemDeServicoResponseDTO> atualizar(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id,
      @RequestBody @Valid OrdemDeServicoRequestDTO request) {
    return ResponseEntity.ok(service.atualizar(id, request));
  }

  @Operation(
      summary = "Excluir OS",
      description =
          "Remove uma ordem de serviço, junto com suas peças, mão de obra e pagamento."
              + " A exclusão é recusada se a OS já tiver recebido qualquer valor de"
              + " pagamento (mesmo parcial) — nesse caso não há como excluir sem perder"
              + " o histórico financeiro.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "OS excluída com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "OS não encontrada"),
    @ApiResponse(
        responseCode = "409",
        description = "A OS já recebeu algum pagamento e não pode ser excluída")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole( 'GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id) {
    service.deletar(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Atualizar status da OS",
      description =
          "Move a OS pela máquina de estados (ABERTA → DIAGNOSTICO → ... → FECHADA, ou"
              + " CANCELADA a qualquer momento antes de FECHADA). O MECANICO não pode"
              + " definir FINALIZADA, ENTREGUE nem CANCELADA. Fechar a OS (FECHADA) só é"
              + " permitido com o pagamento totalmente quitado. OS cancelada é estado"
              + " terminal: nenhuma transição posterior é aceita.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
    @ApiResponse(
        responseCode = "400",
        description = "Transição de status não permitida pela máquina de estados"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "MECANICO tentando finalizar, entregar ou cancelar a OS"),
    @ApiResponse(responseCode = "404", description = "OS não encontrada"),
    @ApiResponse(
        responseCode = "422",
        description = "OS já cancelada não aceita nenhuma alteração de status"),
    @ApiResponse(
        responseCode = "409",
        description = "A OS foi alterada por outro usuário (conflito de versão)")
  })
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<OrdemDeServicoResponseDTO> atualizarStatus(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id,
      @RequestBody @Valid AtualizarStatusOSRequestDTO request) {
    return ResponseEntity.ok(service.atualizarStatus(id, request));
  }

  @Operation(
      summary = "Atribuir mecânico à OS",
      description = "Associa (ou troca) o mecânico responsável pela OS.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mecânico atribuído com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS não encontrada, ou mecânico não encontrado")
  })
  @PatchMapping("/{id}/mecanico")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<OrdemDeServicoResponseDTO> atribuirMecanico(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id,
      @RequestBody @Valid AtribuirMecanicoRequestDTO request) {
    return ResponseEntity.ok(service.atribuirMecanico(id, request));
  }

  @Operation(
      summary = "Atribuir cliente à OS",
      description =
          "Associa (ou troca) o cliente proprietário do veículo na OS — útil quando a OS"
              + " foi aberta sem cliente identificado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente atribuído com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "OS não encontrada, ou cliente não encontrado")
  })
  @PatchMapping("/{id}/cliente")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<OrdemDeServicoResponseDTO> atribuirCliente(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id,
      @RequestBody @Valid AtribuirClienteRequestDTO request) {
    return ResponseEntity.ok(service.atribuirCliente(id, request));
  }

  @Operation(
      summary = "Aplicar desconto na OS",
      description =
          "Define o desconto da OS, em valor absoluto (não percentual). Não pode ser"
              + " negativo nem maior que o valor total. Se o valor total cair abaixo do"
              + " desconto vigente (ex.: após excluir uma peça), o desconto é travado no"
              + " novo total, nunca deixando o valor com desconto negativo.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Desconto aplicado com sucesso"),
    @ApiResponse(
        responseCode = "400",
        description = "Desconto negativo ou maior que o valor total da OS"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "OS não encontrada")
  })
  @PatchMapping("/{id}/desconto")
  @PreAuthorize("hasAnyRole( 'GERENTE')")
  public ResponseEntity<OrdemDeServicoResponseDTO> aplicarDesconto(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long id,
      @RequestBody @Valid BigDecimal desconto) {
    return ResponseEntity.ok(service.aplicarDesconto(id, desconto));
  }

  @Operation(
      summary = "Listar OS de um veículo",
      description =
          "Retorna o histórico de ordens de serviço do veículo informado, na oficina do"
              + " usuário autenticado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Veículo não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/veiculo/{veiculoId}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorVeiculo(
      @Parameter(description = "ID do veículo") @PathVariable Long veiculoId) {
    return ResponseEntity.ok(service.listarPorVeiculo(veiculoId));
  }

  @Operation(
      summary = "Listar OS de um mecânico",
      description = "Retorna as ordens de serviço atribuídas ao mecânico informado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Mecânico não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/mecanico/{mecanicoId}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorMecanico(
      @Parameter(description = "ID do mecânico") @PathVariable Long mecanicoId) {
    return ResponseEntity.ok(service.listarPorMecanico(mecanicoId));
  }

  @Operation(
      summary = "Listar OS de uma unidade",
      description = "Retorna as ordens de serviço abertas na unidade informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Unidade não encontrada, ou pertence a outra oficina")
  })
  @GetMapping("/unidade/{unidadeId}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorUnidade(
      @Parameter(description = "ID da unidade") @PathVariable Long unidadeId) {
    return ResponseEntity.ok(service.listarPorUnidade(unidadeId));
  }

  @Operation(
      summary = "Listar OS de um cliente",
      description = "Retorna o histórico de ordens de serviço do cliente informado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Cliente não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/cliente/{clienteId}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorCliente(
      @Parameter(description = "ID do cliente") @PathVariable Long clienteId) {
    return ResponseEntity.ok(service.listarPorCliente(clienteId));
  }

  @Operation(
      summary = "Listar OS por status",
      description =
          "Retorna, dentro do escopo da oficina do usuário autenticado, as ordens de"
              + " serviço que estão no status informado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Status inválido"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping("/status/{status}")
  @PreAuthorize("hasAnyRole( 'GERENTE', 'MECANICO')")
  public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorStatus(
      @Parameter(description = "Status da ordem de serviço") @PathVariable
          StatusOrdemDeServico status) {
    return ResponseEntity.ok(service.listarPorStatus(status));
  }
}
