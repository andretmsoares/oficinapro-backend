package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.exception.ordem_servico.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.model.*;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoServiceServiceImpl implements OrdemDeServicoService {

    private final OrdemDeServicoRepository ordemServicoRepository;
    private final OficinaService oficinaService;
    private final UnidadeService unidadeService;
    private final VeiculoService veiculoService;
    private final ClienteService clienteService;
    private final MecanicoService mecanicoService;
    private final AuthenticatedUserProvider authenticatedUserProvider;


    private Long oficinaObrigatoriaDoLogado(Usuario logado) {
        Long oficinaId = logado.getOficina() != null ? logado.getOficina().getId() : null;
        if (oficinaId == null) {
            throw new AccessDeniedException("Usuário não está vinculado a nenhuma oficina");
        }
        return oficinaId;
    }

    private void validarAcessoOficina(Long oficinaId) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        if (!oficinaDoLogado.equals(oficinaId)) {
            throw new AccessDeniedException("Você só pode acessar dados da sua própria oficina");
        }
    }

    private void validarAcessoAoRegistro(OrdemDeServico os) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        Long oficinaDaOS = os.getOficina() != null ? os.getOficina().getId() : null;
        if (!oficinaDoLogado.equals(oficinaDaOS)) {
            throw new OrdemDeServicoNotFoundException(os.getId());
        }
    }

    /** Filtra uma lista já obtida (buscas indiretas por veículo/mecânico/unidade/cliente),
     *  mantendo só as OS da própria oficina — sem lançar exceção, já que é uma coleção. */
    private List<OrdemDeServico> filtrarPorEscopo(List<OrdemDeServico> lista) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return lista;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        return lista.stream()
                .filter(os -> os.getOficina() != null && oficinaDoLogado.equals(os.getOficina().getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listar() {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        List<OrdemDeServico> lista = logado.getRole() == Role.ADMIN
                ? ordemServicoRepository.findAll()
                : ordemServicoRepository.findByOficinaId(oficinaObrigatoriaDoLogado(logado));

        return lista.stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorVeiculo(Long veiculoId) {
        // valida que o veículo em si é acessível ao usuário logado (já aplica escopo)
        veiculoService.buscarPorEntidadeId(veiculoId);

        List<OrdemDeServico> lista = filtrarPorEscopo(ordemServicoRepository.findByVeiculoId(veiculoId));
        return lista.stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorMecanico(Long mecanicoId) {
        mecanicoService.buscarPorEntidadeId(mecanicoId);

        List<OrdemDeServico> lista = filtrarPorEscopo(ordemServicoRepository.findByMecanicoId(mecanicoId));
        return lista.stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorUnidade(Long unidadeId) {
        unidadeService.buscarPorId(unidadeId); // valida escopo da unidade

        List<OrdemDeServico> lista = filtrarPorEscopo(ordemServicoRepository.findByUnidadeId(unidadeId));
        return lista.stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorCliente(Long clienteId) {
        clienteService.buscarPorEntidadeId(clienteId);

        List<OrdemDeServico> lista = filtrarPorEscopo(ordemServicoRepository.findByClienteId(clienteId));
        return lista.stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorOficina(Long oficinaId) {
        validarAcessoOficina(oficinaId);

        return ordemServicoRepository.findByOficinaId(oficinaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemDeServicoResponseDTO buscarPorId(Long id) {
        return toResponseDTO(this.buscarPorEntidadeId(id));
    }

    @Override
    public OrdemDeServico buscarPorEntidadeId(Long id) {
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));
        validarAcessoAoRegistro(os);
        return os;
    }

    // ---------------------------------------------------------
    // Mutações
    // ---------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atualizarStatus(Long id, AtualizarStatusOSRequestDTO dto) {
        OrdemDeServico os = this.buscarPorEntidadeId(id); // já valida acesso

        StatusOrdemDeServico statusAtual = os.getStatus();
        StatusOrdemDeServico novoStatus = dto.status();

        if (statusAtual == StatusOrdemDeServico.CANCELADA) {
            throw new OSCanceledException();
        }

        if (statusAtual == StatusOrdemDeServico.ENTREGUE && novoStatus == StatusOrdemDeServico.ABERTA) {
            throw new OSFinishedException();
        }

        os.setStatus(novoStatus);

        if (novoStatus == StatusOrdemDeServico.FINALIZADA || novoStatus == StatusOrdemDeServico.ENTREGUE) {
            if (os.getDataFechamento() == null) {
                os.setDataFechamento(LocalDateTime.now());
            }
        }

        return toResponseDTO(ordemServicoRepository.save(os));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atribuirMecanico(Long id, AtribuirMecanicoRequestDTO dto) {
        OrdemDeServico os = this.buscarPorEntidadeId(id); // já valida acesso

        // valida que o mecânico pertence à mesma oficina da OS
        Mecanico mecanico = mecanicoService.buscarPorEntidadeId(dto.mecanicoId());

        os.setMecanico(mecanico);

        return toResponseDTO(ordemServicoRepository.save(os));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atribuirCliente(Long id, AtribuirClienteRequestDTO dto) {
        OrdemDeServico os = this.buscarPorEntidadeId(id); // já valida acesso

        Cliente cliente = clienteService.buscarPorEntidadeId(dto.clienteId());

        os.setCliente(cliente);

        return toResponseDTO(ordemServicoRepository.save(os));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO criar(OrdemDeServicoRequestDTO request) {
        validarAcessoOficina(request.oficinaId());

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

        os = ordemServicoRepository.save(os);

        return toResponseDTO(os);
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atualizar(Long id, OrdemDeServicoRequestDTO request) {
        OrdemDeServico os = this.buscarPorEntidadeId(id); // já valida acesso

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
        OrdemDeServico os = this.buscarPorEntidadeId(id); // já valida acesso
        ordemServicoRepository.delete(os);
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
                os.getValorTotal()
        );
    }
}