package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.OSCanceledException;
import com.oficinapro.exception.OSFinishedException;
import com.oficinapro.exception.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.model.*;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listar() {
        return ordemServicoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorVeiculo(Long veiculoId) {
        return ordemServicoRepository.findByVeiculoId(veiculoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorMecanico(Long mecanicoId) {
        return ordemServicoRepository.findByMecanicoId(mecanicoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorUnidade(Long unidadeId) {
        return ordemServicoRepository.findByUnidadeId(unidadeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorCliente(Long clienteId) {
        return ordemServicoRepository.findByClienteId(clienteId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoResponseDTO> listarPorOficina(Long oficinaId) {
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
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atualizarStatus(Long id, AtualizarStatusOSRequestDTO dto) {
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));

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
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));

        Mecanico mecanico = mecanicoService.buscarPorEntidadeId(dto.mecanicoId());

        os.setMecanico(mecanico);

        return toResponseDTO(ordemServicoRepository.save(os));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO atribuirCliente(Long id, AtribuirClienteRequestDTO dto) {
        OrdemDeServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeServicoNotFoundException(id));

        Cliente cliente = clienteService.buscarPorEntidadeId(dto.clienteId());

        os.setCliente(cliente);

        return toResponseDTO(ordemServicoRepository.save(os));
    }

    @Override
    @Transactional
    public OrdemDeServicoResponseDTO criar(OrdemDeServicoRequestDTO request) {
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
        OrdemDeServico os = this.buscarPorEntidadeId(id);

        if (!Objects.equals(os.getOficina().getId(), request.oficinaId())) throw new OSIsNotPossibleSwapWorkshopException();
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
