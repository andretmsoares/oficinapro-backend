package com.oficinapro.service.pessoaCrud;

import com.oficinapro.enums.Role;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Pessoa;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.PessoaCrudRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
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
  protected final AuthenticatedUserProvider authenticatedUserProvider;
  protected final OficinaAccessValidator oficinaAccessValidator;

  protected AbstractPessoaServiceImpl(
      PessoaCrudRepository<T> repository,
      OficinaServiceImpl oficinaService,
      PessoaService pessoaService,
      AuthenticatedUserProvider authenticatedUserProvider,
      OficinaAccessValidator oficinaAccessValidator) {
    this.repository = repository;
    this.oficinaService = oficinaService;
    this.pessoaService = pessoaService;
    this.authenticatedUserProvider = authenticatedUserProvider;
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

  @Override
  public Page<RES> listar(Pageable pageable) {
    Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
    if (logado.getRole() == Role.ADMIN) {
      return repository.findAll(pageable).map(this::toResponse);
    }
    Long oficinaId = oficinaAccessValidator.getOficinaIdUsuarioLogado();
    return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  @Override
  public Page<RES> listarPorOficinaId(Long oficinaId, Pageable pageable) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);
    return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  @Override
  public T buscarPorEntidadeId(Long id) {
    T entity = repository.findById(id).orElseThrow(this::notFoundException);
    oficinaAccessValidator.validarAcessoAoRegistro(
        entity.getOficina() != null ? entity.getOficina().getId() : null,
        new UsuarioNotFoundException());
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

    Long oficinaId = authenticatedUserProvider.getOficinaIdUsuarioLogado();

    T entity =
        repository
            .findByOficinaIdAndDocumento(oficinaId, documento)
            .orElseThrow(this::notFoundException);

    return toResponse(entity);
  }

  @Transactional
  @Override
  public RES criar(C request) {
    Long oficinaId = extractOficinaIdCreate(request);

    // oficinaId nulo é um caso legítimo: o ADMIN do SaaS não pertence a nenhuma
    // oficina. Resolver a oficina sem esse guard chamaria findById(null), que o
    // Spring Data rejeita com InvalidDataAccessApiUsageException e tornaria
    // impossível criar um ADMIN. Quem valida a coerência entre role e oficina é
    // o validateBeforeCreate de cada subclasse.
    Oficina oficina = oficinaId == null ? null : oficinaService.buscarPorEntidadeId(oficinaId);

    // Sem oficina não há escopo de unicidade: a constraint do banco é
    // (oficina_id, documento) e no Postgres NULL não colide com NULL.
    if (oficinaId != null
        && pessoaService.existsByOficinaIdAndDocumento(
            oficinaId, extractDocumentoCreate(request))) {
      throw alreadyExistsException();
    }

    validateBeforeCreate(request);

    T entity = toEntity(request, oficina);
    entity = repository.save(entity);
    return toResponse(entity);
  }

  @Transactional
  @Override
  public RES atualizar(Long id, U request) {
    T entity = buscarPorEntidadeId(id); // já valida acesso ao registro atual

    Long oficinaId = extractOficinaIdUpdate(request);
    // Valida também a oficina de destino (evita "mover" o registro pra outra oficina
    // indevidamente). Quem eventualmente precisa reatribuir a oficina faz isso no
    // próprio applyUpdate.
    //
    // Quando oficinaId vem nulo o registro passa a não ter oficina — é o caso da
    // promoção para ADMIN do SaaS. Aqui não há oficina de destino para validar, e
    // chamar findById(null) quebraria a operação.
    if (oficinaId != null) {
      oficinaService.buscarPorEntidadeId(oficinaId);
    }

    if (oficinaId != null
        && pessoaService.existsByOficinaIdAndDocumentoExcluindoId(
            oficinaId, extractDocumentoUpdate(request), id)) {
      throw alreadyExistsException();
    }

    validateBeforeUpdate(id, request);

    applyUpdate(entity, request);
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
