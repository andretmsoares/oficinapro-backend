package com.oficinapro.service.veiculo;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.VeiculoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VeiculoServiceImpl implements VeiculoService {

  private final VeiculoRepository veiculoRepository;
  private final OficinaService oficinaService;
  private final OficinaAccessValidator oficinaAccessValidator;

  private String normalizarPlaca(String placa) {
    return placa == null ? null : placa.toUpperCase().replace("-", "").trim();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<VeiculoResponseDTO> listar(Pageable pageable) {
    Usuario logado = oficinaAccessValidator.getUsuarioAutenticado();

    Page<Veiculo> page =
        logado.getRole() == Role.ADMIN
            ? veiculoRepository.findAll(pageable)
            : veiculoRepository.findByOficinaId(
                oficinaAccessValidator.getOficinaIdUsuarioLogado(), pageable);

    return page.map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<VeiculoResponseDTO> listarPorOficinaId(Long oficinaId, Pageable pageable) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);
    return veiculoRepository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Veiculo buscarPorEntidadeId(Long id) {
    Veiculo veiculo =
        veiculoRepository.findById(id).orElseThrow(() -> new VeiculoNotFoundException(id));
    oficinaAccessValidator.validarAcessoAoRegistro(
        veiculo.getOficina() != null ? veiculo.getOficina().getId() : null,
        new VeiculoNotFoundException(id));
    return veiculo;
  }

  @Override
  @Transactional(readOnly = true)
  public VeiculoResponseDTO buscarPorId(Long id) {
    return toResponse(buscarPorEntidadeId(id));
  }

  @Override
  @Transactional(readOnly = true)
  public VeiculoResponseDTO buscarPorPlaca(String placa) {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    Veiculo veiculo =
        veiculoRepository
            .findByPlaca(oficinaId, normalizarPlaca(placa))
            .orElseThrow(() -> new VeiculoNotFoundException(null));

    oficinaAccessValidator.validarAcessoAoRegistro(
        veiculo.getOficina() != null ? veiculo.getOficina().getId() : null,
        new VeiculoNotFoundException(veiculo.getId()));
    return toResponse(veiculo);
  }

  @Override
  @Transactional
  public VeiculoResponseDTO criar(VeiculoRequestDTO request) {
    oficinaAccessValidator.validarAcessoOficina(request.oficinaId());

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
  @Transactional
  public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO request) {
    Veiculo veiculo = buscarPorEntidadeId(id); // já valida acesso ao registro atual

    oficinaAccessValidator.validarAcessoOficina(
        request.oficinaId()); // valida também a oficina de destino

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

  @Transactional
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
        veiculo.getPlaca());
  }
}
