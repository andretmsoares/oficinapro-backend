package com.oficinapro.service.veiculo;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.model.Oficina;
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
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();
    return veiculoRepository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Integer count() {
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();
    return veiculoRepository.countByOficinaId(oficinaId);
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
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);

    String placa = normalizarPlaca(request.placa());
    if (veiculoRepository.existsByOficinaIdAndPlaca(oficinaId, placa)) {
      throw new PlacaAlreadyExistsException(placa);
    }

    Veiculo veiculo = new Veiculo();
    veiculo.setOficina(oficina);
    veiculo.setModelo(request.modelo().toUpperCase());
    veiculo.setAno(request.ano());
    veiculo.setMarca(request.marca().toUpperCase());
    veiculo.setCor(request.cor());
    veiculo.setPlaca(placa);

    Veiculo saved = veiculoRepository.save(veiculo);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO request) {
    Veiculo veiculo = buscarPorEntidadeId(id); // já valida acesso ao registro atual

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    String placa = normalizarPlaca(request.placa());
    if (!veiculo.getPlaca().equals(placa)
        && veiculoRepository.existsByOficinaIdAndPlacaAndIdNot(oficinaId, placa, id)) {
      throw new PlacaAlreadyExistsException(placa);
    }

    veiculo.setModelo(request.modelo().toUpperCase());
    veiculo.setAno(request.ano());
    veiculo.setMarca(request.marca().toUpperCase());
    veiculo.setPlaca(placa);
    veiculo.setCor(request.cor());

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
        veiculo.getCor(),
        veiculo.getPlaca());
  }
}
