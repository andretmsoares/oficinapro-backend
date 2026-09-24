package com.oficinapro.service.pessoaCrud;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Pessoa;
import com.oficinapro.repository.PessoaCrudRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPessoaServiceImpl<T extends Pessoa, C, U, RES>
    implements PessoaCrudService<C, U, RES, T> {

  protected final PessoaCrudRepository<T> repository;
  protected final OficinaServiceImpl oficinaService;
  protected final PessoaService pessoaService;
  protected final OficinaAccessValidator oficinaAccessValidator;

  protected AbstractPessoaServiceImpl(
      PessoaCrudRepository<T> repository,
      OficinaServiceImpl oficinaService,
      PessoaService pessoaService,
      OficinaAccessValidator oficinaAccessValidator) {
    this.repository = repository;
    this.oficinaService = oficinaService;
    this.pessoaService = pessoaService;
    this.oficinaAccessValidator = oficinaAccessValidator;
  }

  protected abstract RES toResponse(T entity);

  protected abstract T toEntity(C request, Oficina oficina);

  protected abstract void applyUpdate(T entity, U request);

  protected abstract String extractDocumentoCreate(C request);

  protected abstract Long extractOficinaIdCreate(C request);

  protected abstract String extractDocumentoUpdate(U request);

  protected abstract Long extractOficinaIdUpdate(U request);

  protected abstract RuntimeException notFoundException();

  protected abstract RuntimeException alreadyExistsException();

  protected void validateBeforeCreate(C request) {}

  protected void validateBeforeUpdate(Long id, U request) {}

  @Transactional(readOnly = true)
  @Override
  public Page<RES> listar(Pageable pageable) {
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();
    return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  @Override
  public Page<RES> listarTodos(Pageable pageable) {
    oficinaAccessValidator.validarRole(Role.ADMIN);
    return repository.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  @Override
  public T buscarPorEntidadeId(Long id) {
    T entity = repository.findById(id).orElseThrow(this::notFoundException);
    oficinaAccessValidator.validarAcessoAoRegistro(
        entity.getOficina() != null ? entity.getOficina().getId() : null, notFoundException());
    return entity;
  }

  @Transactional(readOnly = true)
  @Override
  public RES buscarPorId(Long id) {
    return toResponse(buscarPorEntidadeId(id));
  }

  @Transactional(readOnly = true)
  @Override
  public List<RES> buscarPorNome(String nome) {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    return repository.findByOficinaIdAndNome(oficinaId, nome).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  @Override
  public RES buscarPorDocumento(String documento) {

    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();

    T entity =
        repository
            .findByOficinaIdAndDocumento(oficinaId, documento)
            .orElseThrow(this::notFoundException);

    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  @Override
  public List<RES> buscarPorNomeAdmin(String nome) {

    oficinaAccessValidator.validarRole(Role.ADMIN);

    return repository.findByNome(nome).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  @Override
  public List<RES> buscarPorDocumentoAdmin(String documento) {

    oficinaAccessValidator.validarRole(Role.ADMIN);

    return repository.findByDocumento(documento).stream().map(this::toResponse).toList();
  }

  @Transactional
  @Override
  public RES criar(C request) {
    Long oficinaId = extractOficinaIdCreate(request);

    if (oficinaId != null) {
      oficinaAccessValidator.validarAcessoOficina(oficinaId);
    }

    Oficina oficina = oficinaId == null ? null : oficinaService.buscarPorEntidadeId(oficinaId);

    if (oficinaId != null
        && pessoaService.existsByOficinaIdAndDocumento(
            oficinaId, extractDocumentoCreate(request))) {
      throw alreadyExistsException();
    }

    validateBeforeCreate(request);

    T entity = toEntity(request, oficina);
    entity.setNome(entity.getNome().toUpperCase());
    entity = repository.save(entity);
    return toResponse(entity);
  }

  @Transactional
  @Override
  public RES atualizar(Long id, U request) {
    T entity = buscarPorEntidadeId(id); // já valida acesso ao registro atual

    Long oficinaId = extractOficinaIdUpdate(request);

    if (oficinaId != null) {
      oficinaAccessValidator.validarAcessoOficina(oficinaId);
      oficinaService.buscarPorEntidadeId(oficinaId);
    }

    if (oficinaId != null
        && pessoaService.existsByOficinaIdAndDocumentoExcluindoId(
            oficinaId, extractDocumentoUpdate(request), id)) {
      throw alreadyExistsException();
    }

    validateBeforeUpdate(id, request);

    applyUpdate(entity, request);
    entity.setNome(entity.getNome().toUpperCase());
    repository.save(entity);
    return toResponse(entity);
  }

  @Transactional
  @Override
  public void deletar(Long id) {
    T entity = buscarPorEntidadeId(id); // já valida acesso
    repository.delete(entity);
  }
}
