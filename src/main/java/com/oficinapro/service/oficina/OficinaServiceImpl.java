package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaAlreadyActivatedException;
import com.oficinapro.exception.oficina.OficinaAlreadyDisabledException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.security.OficinaAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OficinaServiceImpl implements OficinaService {

  private final OficinaRepository oficinaRepository;
  private final OficinaAccessValidator oficinaAccessValidator;

  @Transactional(readOnly = true)
  @Override
  public List<OficinaResponseDTO> listar() {
    oficinaAccessValidator.validarRole(Role.ADMIN);
    return oficinaRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  @Override
  public Page<OficinaResponseDTO> buscar(String search, Pageable pageable) {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    String termo = search == null ? "" : search.trim();

    return oficinaRepository
        .findByNomeContainingIgnoreCaseOrCnpjContainingIgnoreCase(termo, termo, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  @Override
  public OficinaResponseDTO buscarPorId(Long id) {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    Oficina oficina = this.buscarPorEntidadeId(id);

    return toResponse(oficina);
  }

  @Transactional(readOnly = true)
  @Override
  public Oficina buscarPorEntidadeId(Long id) {

    return oficinaRepository.findById(id).orElseThrow(() -> new OficinaNotFoundException(id));
  }

  @Transactional(readOnly = true)
  @Override
  public boolean existsById(Long id) {
    return oficinaRepository.existsById(id);
  }

  @Transactional
  @Override
  public OficinaResponseDTO criar(OficinaRequestDTO request) {

    oficinaAccessValidator.validarRole(Role.ADMIN);

    if (oficinaRepository.existsByCnpj(request.cnpj())) {
      throw new CnpjAlreadyExistsException(request.cnpj());
    }

    Oficina oficina = new Oficina();

    oficina.setNome(request.nome());
    oficina.setCnpj(request.cnpj());
    oficina.setTelefone(request.telefone());

    Oficina saved = oficinaRepository.save(oficina);

    return toResponse(saved);
  }

  @Transactional
  @Override
  public OficinaResponseDTO atualizar(Long id, OficinaRequestDTO request) {

    oficinaAccessValidator.validarRole(Role.ADMIN);

    Oficina oficina =
        oficinaRepository.findById(id).orElseThrow(() -> new OficinaNotFoundException(id));

    if (!oficina.getCnpj().equals(request.cnpj())
        && oficinaRepository.existsByCnpj(request.cnpj())) {

      throw new CnpjAlreadyExistsException(request.cnpj());
    }

    oficina.setNome(request.nome());
    oficina.setCnpj(request.cnpj());
    oficina.setTelefone(request.telefone());

    Oficina updated = oficinaRepository.save(oficina);

    return toResponse(updated);
  }

  @Transactional
  @Override
  public void deletar(Long id) {

    oficinaAccessValidator.validarRole(Role.ADMIN);

    if (!oficinaRepository.existsById(id)) {
      throw new OficinaNotFoundException(id);
    }

    oficinaRepository.deleteById(id);
  }

  private OficinaResponseDTO toResponse(Oficina oficina) {

    return new OficinaResponseDTO(
        oficina.getId(),
        oficina.getNome(),
        oficina.getCnpj(),
        oficina.getTelefone(),
        oficina.getAtivo());
  }

  @Transactional
  @Override
  public void desativar(Long id) {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    Oficina oficina = this.buscarPorEntidadeId(id);

    if (!oficina.getAtivo()) {
      throw new OficinaAlreadyDisabledException();
    }

    oficina.setAtivo(false);

    oficinaRepository.save(oficina);
  }

  @Transactional
  @Override
  public void ativar(Long id) {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    Oficina oficina = this.buscarPorEntidadeId(id);

    if (oficina.getAtivo()) {
      throw new OficinaAlreadyActivatedException();
    }

    oficina.setAtivo(true);

    oficinaRepository.save(oficina);
  }
}