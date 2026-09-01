package com.oficinapro.service.veiculo;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.VeiculoRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VeiculoServiceImpl implements VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final OficinaService oficinaService;
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

    private void validarAcessoAoRegistro(Veiculo veiculo) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        Long oficinaDoVeiculo = veiculo.getOficina() != null ? veiculo.getOficina().getId() : null;
        if (!oficinaDoLogado.equals(oficinaDoVeiculo)) {
            throw new VeiculoNotFoundException(veiculo.getId());
        }
    }

    private String normalizarPlaca(String placa) {
        return placa == null ? null : placa.toUpperCase().replace("-", "").trim();
    }

    @Override
    public Page<VeiculoResponseDTO> listar(Pageable pageable) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        Page<Veiculo> page = logado.getRole() == Role.ADMIN
                ? veiculoRepository.findAll(pageable)
                : veiculoRepository.findByOficinaId(oficinaObrigatoriaDoLogado(logado), pageable);

        return page.map(this::toResponse);
    }

    @Override
    public Page<VeiculoResponseDTO> listarPorOficinaId(Long oficinaId, Pageable pageable) {
        validarAcessoOficina(oficinaId);
        return veiculoRepository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
    }

    @Override
    public Veiculo buscarPorEntidadeId(Long id) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new VeiculoNotFoundException(id));
        validarAcessoAoRegistro(veiculo);
        return veiculo;
    }

    @Override
    public VeiculoResponseDTO buscarPorId(Long id) {
        return toResponse(buscarPorEntidadeId(id));
    }

    @Override
    public VeiculoResponseDTO buscarPorPlaca(String placa) {
        Veiculo veiculo = veiculoRepository.findByPlaca(normalizarPlaca(placa))
                .orElseThrow(() -> new VeiculoNotFoundException(null));
        validarAcessoAoRegistro(veiculo);
        return toResponse(veiculo);
    }

    @Override
    public VeiculoResponseDTO criar(VeiculoRequestDTO request) {
        validarAcessoOficina(request.oficinaId());

        Oficina oficina = oficinaService.buscarPorEntidadeId(request.oficinaId());

        String placa = normalizarPlaca(request.placa());
        if (veiculoRepository.existsByOficinaIdAndPlaca(request.oficinaId(), placa)) {
            throw new PlacaAlreadyExistsException(placa);
        }

        Veiculo veiculo = new Veiculo();
        veiculo.setOficina(oficina);
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());
        veiculo.setMarca(request.marca());
        veiculo.setPlaca(placa);

        Veiculo saved = veiculoRepository.save(veiculo);

        return toResponse(saved);
    }

    @Override
    public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO request) {
        Veiculo veiculo = buscarPorEntidadeId(id); // já valida acesso ao registro atual

        validarAcessoOficina(request.oficinaId()); // valida também a oficina de destino

        String placa = normalizarPlaca(request.placa());
        if (!veiculo.getPlaca().equals(placa)
                && veiculoRepository.existsByOficinaIdAndPlacaAndIdNot(request.oficinaId(), placa, id)) {
            throw new PlacaAlreadyExistsException(placa);
        }

        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());
        veiculo.setMarca(request.marca());
        veiculo.setPlaca(placa);

        Veiculo updated = veiculoRepository.save(veiculo);

        return toResponse(updated);
    }

    @Override
    public void deletar(Long id) {
        Veiculo veiculo = buscarPorEntidadeId(id); // já valida acesso
        veiculoRepository.delete(veiculo);
    }

    private VeiculoResponseDTO toResponse(Veiculo veiculo) {
        return new VeiculoResponseDTO(
                veiculo.getId(),
                veiculo.getOficina().getId(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getMarca(),
                veiculo.getPlaca()
        );
    }
}