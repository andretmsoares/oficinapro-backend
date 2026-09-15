package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.ordem_servico.DescontoInvalidoException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.model.*;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemDeServicoServiceImpl implements OrdemDeServicoService {

  private final OrdemDeServicoRepository ordemServicoRepository;
  private final OficinaService oficinaService;
  private final UnidadeService unidadeService;
  private final VeiculoService veiculoService;
  private final ClienteService clienteService;
  private final MecanicoService mecanicoService;
  private final AuthenticatedUserProvider authenticatedUserProvider;
  private final PagamentoService pagamentoService;
  private final OficinaAccessValidator oficinaAccessValidator;

  /**
   * Construtor escrito à mão (em vez de {@code @RequiredArgsConstructor}) porque {@code
   * pagamentoService} precisa do {@code @Lazy} no parâmetro.
   *
   * <p>Existe uma dependência circular real entre este service e o de pagamento: a OS abre o
   * pagamento ao ser criada e consulta o pagamento para permitir a transição para FECHADA, enquanto
   * o pagamento resolve e valida a OS. Como o Spring Boot proíbe referências circulares por padrão,
   * sem o {@code @Lazy} o contexto não sobe — a aplicação inteira falha na inicialização.
   *
   * <p>O {@code @Lazy} precisa estar no ponto de injeção: anotar a classe {@code
   * PagamentoServiceImpl} apenas adiaria a instanciação do bean, sem criar o proxy que efetivamente
   * rompe o ciclo.
   */
  public OrdemDeServicoServiceImpl(
      OrdemDeServicoRepository ordemServicoRepository,
      OficinaService oficinaService,
      UnidadeService unidadeService,
      VeiculoService veiculoService,
      ClienteService clienteService,
      MecanicoService mecanicoService,
      AuthenticatedUserProvider authenticatedUserProvider,
      @Lazy PagamentoService pagamentoService,
      OficinaAccessValidator oficinaAccessValidator) {
    this.ordemServicoRepository = ordemServicoRepository;
    this.oficinaService = oficinaService;
    this.unidadeService = unidadeService;
    this.veiculoService = veiculoService;
    this.clienteService = clienteService;
    this.mecanicoService = mecanicoService;
    this.authenticatedUserProvider = authenticatedUserProvider;
    this.pagamentoService = pagamentoService;
    this.oficinaAccessValidator = oficinaAccessValidator;
  }

  private List<OrdemDeServico> filtrarPorEscopo(List<OrdemDeServico> lista) {
    Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
    if (logado.getRole() == Role.ADMIN) {
      return lista;
    }
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return lista.stream()
        .filter(os -> os.getOficina() != null && oficinaId.equals(os.getOficina().getId()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listar() {
    Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

    List<OrdemDeServico> lista =
        logado.getRole() == Role.ADMIN
            ? ordemServicoRepository.findAll()
            : ordemServicoRepository.findByOficinaId(
                oficinaAccessValidator.getOficinaIdUsuarioLogado());

    return lista.stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorVeiculo(Long veiculoId) {
    veiculoService.buscarPorEntidadeId(veiculoId);
    List<OrdemDeServico> lista =
        filtrarPorEscopo(ordemServicoRepository.findByVeiculoId(veiculoId));
    return lista.stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorMecanico(Long mecanicoId) {
    mecanicoService.buscarPorEntidadeId(mecanicoId);
    List<OrdemDeServico> lista =
        filtrarPorEscopo(ordemServicoRepository.findByMecanicoId(mecanicoId));
    return lista.stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorUnidade(Long unidadeId) {
    unidadeService.buscarPorId(unidadeId);
    List<OrdemDeServico> lista =
        filtrarPorEscopo(ordemServicoRepository.findByUnidadeId(unidadeId));
    return lista.stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorCliente(Long clienteId) {
    clienteService.buscarPorEntidadeId(clienteId);
    List<OrdemDeServico> lista =
        filtrarPorEscopo(ordemServicoRepository.findByClienteId(clienteId));
    return lista.stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorOficina(Long oficinaId) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);
    return ordemServicoRepository.findByOficinaId(oficinaId).stream()
        .map(this::toResponseDTO)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrdemDeServicoResponseDTO> listarPorStatus(StatusOrdemDeServico status) {
    List<OrdemDeServico> lista = ordemServicoRepository.findByStatus(status);

    return filtrarPorEscopo(lista).stream().map(this::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public OrdemDeServicoResponseDTO buscarPorId(Long id) {
    return toResponseDTO(this.buscarPorEntidadeId(id));
  }

  @Override
  public OrdemDeServico buscarPorEntidadeId(Long id) {
    OrdemDeServico os =
        ordemServicoRepository
            .findById(id)
            .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));
    oficinaAccessValidator.validarAcessoAoRegistro(
        os.getOficina() != null ? os.getOficina().getId() : null,
        new OrdemDeServicoNotFoundException(id));
    return os;
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO aplicarDesconto(Long id, BigDecimal desconto) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);

    BigDecimal descontoValido = desconto == null ? BigDecimal.ZERO : desconto;

    if (descontoValido.compareTo(BigDecimal.ZERO) < 0) {
      throw new DescontoInvalidoException("Desconto não pode ser negativo");
    }

    if (descontoValido.compareTo(os.getValorTotal()) > 0) {
      throw new DescontoInvalidoException("Desconto não pode ser maior que o valor total da OS");
    }

    os.setDesconto(descontoValido);
    os.setValorComDesconto(os.getValorTotal().subtract(descontoValido));

    return toResponseDTO(ordemServicoRepository.save(os));
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO recalcularValorTotal(Long id, BigDecimal novoValorTotal) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);

    os.setValorTotal(novoValorTotal);

    BigDecimal descontoAtual = os.getDesconto() == null ? BigDecimal.ZERO : os.getDesconto();

    // se os itens diminuíram e o desconto antigo agora é maior que o novo total,
    // trava o desconto no valor total (nunca fica negativo)
    if (descontoAtual.compareTo(novoValorTotal) > 0) {
      descontoAtual = novoValorTotal;
      os.setDesconto(descontoAtual);
    }

    os.setValorComDesconto(novoValorTotal.subtract(descontoAtual));

    OrdemDeServico saved = ordemServicoRepository.save(os);

    return toResponseDTO(saved);
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO atualizarStatus(Long id, AtualizarStatusOSRequestDTO dto) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);

    StatusOrdemDeServico novo = dto.status();

    Usuario usuario = authenticatedUserProvider.getUsuarioAutenticado();

    validarTransicaoStatus(os, novo, usuario);

    os.setStatus(novo);

    // FECHADA precisa estar nesta lista: o relatório de fluxo mensal conta OS
    // concluídas por dataFechamento, e sem isso o passo ENTREGUE → FECHADA apagava
    // a data registrada na entrega, fazendo a OS desaparecer do relatório.
    // Nos demais status a OS voltou a estar em andamento, então a data é limpa.
    if (ehStatusDeConclusao(novo)) {
      if (os.getDataFechamento() == null) {
        os.setDataFechamento(LocalDateTime.now());
      }
    } else {
      os.setDataFechamento(null);
    }

    return toResponseDTO(ordemServicoRepository.save(os));
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO atribuirMecanico(Long id, AtribuirMecanicoRequestDTO dto) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);
    Mecanico mecanico = mecanicoService.buscarPorEntidadeId(dto.mecanicoId());
    os.setMecanico(mecanico);
    return toResponseDTO(ordemServicoRepository.save(os));
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO atribuirCliente(Long id, AtribuirClienteRequestDTO dto) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);
    Cliente cliente = clienteService.buscarPorEntidadeId(dto.clienteId());
    os.setCliente(cliente);
    return toResponseDTO(ordemServicoRepository.save(os));
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO criar(OrdemDeServicoRequestDTO request) {
    oficinaAccessValidator.validarAcessoOficina(request.oficinaId());

    Oficina oficina = oficinaService.buscarPorEntidadeId(request.oficinaId());
    Unidade unidade = unidadeService.buscarPorEntidadeId(request.unidadeId());
    Veiculo veiculo = veiculoService.buscarPorEntidadeId(request.veiculoId());

    OrdemDeServico os = new OrdemDeServico();
    os.setOficina(oficina);
    os.setUnidade(unidade);
    os.setVeiculo(veiculo);

    if (request.clienteId() != null) {
      Cliente cliente = clienteService.buscarPorEntidadeId(request.clienteId());
      os.setCliente(cliente);
    }

    if (request.mecanicoId() != null) {
      Mecanico mecanico = mecanicoService.buscarPorEntidadeId(request.mecanicoId());
      os.setMecanico(mecanico);
    }

    os.setObs(request.obs());
    os.setDataAbertura(LocalDateTime.now());
    os.setStatus(StatusOrdemDeServico.ABERTA);
    os.setValorTotal(BigDecimal.ZERO);
    os.setDesconto(BigDecimal.ZERO);
    os.setValorComDesconto(BigDecimal.ZERO);

    os = ordemServicoRepository.save(os);

    pagamentoService.criar(new PagamentoRequestDTO(os.getId(), ""));

    return toResponseDTO(os);
  }

  @Override
  @Transactional
  public OrdemDeServicoResponseDTO atualizar(Long id, OrdemDeServicoRequestDTO request) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);

    if (!Objects.equals(os.getOficina().getId(), request.oficinaId())) {
      throw new OSIsNotPossibleSwapWorkshopException();
    }

    os.setUnidade(unidadeService.buscarPorEntidadeId(request.unidadeId()));
    os.setVeiculo(veiculoService.buscarPorEntidadeId(request.veiculoId()));
    os.setObs(request.obs());

    if (request.clienteId() != null) {
      os.setCliente(clienteService.buscarPorEntidadeId(request.clienteId()));
    } else {
      os.setCliente(null);
    }

    if (request.mecanicoId() != null) {
      os.setMecanico(mecanicoService.buscarPorEntidadeId(request.mecanicoId()));
    } else {
      os.setMecanico(null);
    }

    return toResponseDTO(ordemServicoRepository.save(os));
  }

  @Override
  @Transactional
  public void deletar(Long id) {
    OrdemDeServico os = this.buscarPorEntidadeId(id);
    ordemServicoRepository.delete(os);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FluxoMensalOSResponseDTO> fluxoMensal(Long oficinaId, int mes, int ano) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    YearMonth periodo = YearMonth.of(ano, mes);

    LocalDateTime inicio = periodo.atDay(1).atStartOfDay();

    LocalDateTime fim = periodo.plusMonths(1).atDay(1).atStartOfDay();

    List<OrdemDeServico> ordens = ordemServicoRepository.findFluxoMensal(oficinaId, inicio, fim);

    return IntStream.rangeClosed(1, periodo.lengthOfMonth())
        .mapToObj(
            dia -> {
              long abertas =
                  ordens.stream()
                      .filter(os -> os.getDataAbertura() != null)
                      .filter(os -> os.getDataAbertura().getYear() == ano)
                      .filter(os -> os.getDataAbertura().getMonthValue() == mes)
                      .filter(os -> os.getDataAbertura().getDayOfMonth() == dia)
                      .count();

              long finalizadas =
                  ordens.stream()
                      .filter(os -> os.getDataFechamento() != null)
                      .filter(os -> os.getDataFechamento().getYear() == ano)
                      .filter(os -> os.getDataFechamento().getMonthValue() == mes)
                      .filter(os -> os.getDataFechamento().getDayOfMonth() == dia)
                      .count();

              return new FluxoMensalOSResponseDTO(dia, abertas, finalizadas);
            })
        .toList();
  }

  private OrdemDeServicoResponseDTO toResponseDTO(OrdemDeServico os) {
    return new OrdemDeServicoResponseDTO(
        os.getId(),
        os.getOficina().getId(),
        os.getUnidade().getId(),
        os.getVeiculo().getId(),
        os.getCliente() != null ? os.getCliente().getId() : null,
        os.getMecanico() != null ? os.getMecanico().getId() : null,
        os.getDataAbertura(),
        os.getDataFechamento(),
        os.getStatus(),
        os.getObs(),
        os.getValorTotal(),
        os.getDesconto(),
        os.getValorComDesconto());
  }

  /** Status em que a OS está concluída e a data de fechamento deve ser preservada. */
  private boolean ehStatusDeConclusao(StatusOrdemDeServico status) {
    return status == StatusOrdemDeServico.FINALIZADA
        || status == StatusOrdemDeServico.ENTREGUE
        || status == StatusOrdemDeServico.FECHADA;
  }

  private void validarTransicaoStatus(
      OrdemDeServico os, StatusOrdemDeServico novo, Usuario usuario) {
    StatusOrdemDeServico atual = os.getStatus();

    if (atual == StatusOrdemDeServico.CANCELADA) {
      throw new OSCanceledException();
    }

    if (usuario.getRole() == Role.MECANICO
        && (novo == StatusOrdemDeServico.FINALIZADA
            || novo == StatusOrdemDeServico.ENTREGUE
            || novo == StatusOrdemDeServico.CANCELADA)) {

      throw new AccessDeniedException(
          "O mecânico não possui permissão para realizar esta alteração de status");
    }

    if (novo == StatusOrdemDeServico.FECHADA) {

      PagamentoResponseDTO pagamento = pagamentoService.buscarPorOsId(os.getId());

      if (pagamento.status() != StatusPagamento.PAGA) {
        throw new IllegalStateException(
            "A Ordem de Serviço só pode ser fechada após o pagamento integral");
      }
    }

    if (atual == novo) {
      return;
    }

    if (!transicaoPermitida(atual, novo)) {
      throw new IllegalStateException("Transição de status não permitida: " + atual + " → " + novo);
    }
  }

  private boolean transicaoPermitida(StatusOrdemDeServico atual, StatusOrdemDeServico novo) {
    return switch (atual) {
      case ABERTA ->
          novo == StatusOrdemDeServico.DIAGNOSTICO || novo == StatusOrdemDeServico.CANCELADA;

      case DIAGNOSTICO ->
          novo == StatusOrdemDeServico.AGUARDANDO_APROVACAO
              || novo == StatusOrdemDeServico.CANCELADA;

      case AGUARDANDO_APROVACAO ->
          novo == StatusOrdemDeServico.AGUARDANDO_PECAS || novo == StatusOrdemDeServico.CANCELADA;

      case AGUARDANDO_PECAS ->
          novo == StatusOrdemDeServico.EM_EXECUCAO || novo == StatusOrdemDeServico.CANCELADA;

      case EM_EXECUCAO ->
          novo == StatusOrdemDeServico.FINALIZADA || novo == StatusOrdemDeServico.CANCELADA;

      case FINALIZADA ->
          novo == StatusOrdemDeServico.ENTREGUE || novo == StatusOrdemDeServico.ABERTA;

      case ENTREGUE -> novo == StatusOrdemDeServico.FECHADA || novo == StatusOrdemDeServico.ABERTA;

      case FECHADA -> novo == StatusOrdemDeServico.ABERTA;

      case CANCELADA -> false;
    };
  }
}
