package com.oficinapro.service.pessoaCrud;

import com.oficinapro.model.Oficina;
import com.oficinapro.model.Pessoa;
import com.oficinapro.repository.PessoaCrudRepository;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public abstract class AbstractPessoaServiceImpl<T extends Pessoa, C, U, RES>
        implements PessoaCrudService<C, U, RES, T> {

    protected final PessoaCrudRepository<T> repository;
    protected final OficinaServiceImpl oficinaService;
    protected final PessoaService pessoaService;

    protected AbstractPessoaServiceImpl(PessoaCrudRepository<T> repository,
                                        OficinaServiceImpl oficinaService,
                                        PessoaService pessoaService) {
        this.repository = repository;
        this.oficinaService = oficinaService;
        this.pessoaService = pessoaService;
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
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<RES> listarPorOficinaId(Long oficinaId, Pageable pageable) {
        return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
    }

    @Override
    public T buscarPorEntidadeId(Long id) {
        return repository.findById(id).orElseThrow(this::notFoundException);
    }

    @Override
    public RES buscarPorId(Long id) {
        return toResponse(buscarPorEntidadeId(id));
    }

    @Override
    public RES buscarPorNome(String nome) {
        T entity = repository.findByNome(nome).orElseThrow(this::notFoundException);
        return toResponse(entity);
    }

    @Override
    public RES buscarPorDocumento(String documento) {
        T entity = repository.findByDocumento(documento).orElseThrow(this::notFoundException);
        return toResponse(entity);
    }

    @Override
    public RES criar(C request) {
        Long oficinaId = extractOficinaIdCreate(request);
        Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);

        if (pessoaService.existsByOficinaIdAndDocumento(oficinaId, extractDocumentoCreate(request))) {
            throw alreadyExistsException();
        }

        validateBeforeCreate(request);

        T entity = toEntity(request, oficina);
        entity = repository.save(entity);
        return toResponse(entity);
    }

    @Override
    public RES atualizar(Long id, U request) {
        T entity = buscarPorEntidadeId(id);
        Long oficinaId = extractOficinaIdUpdate(request);

        if (pessoaService.existsByOficinaIdAndDocumentoExcluindoId(oficinaId, extractDocumentoUpdate(request), id)) {
            throw alreadyExistsException();
        }

        validateBeforeUpdate(id, request);

        applyUpdate(entity, request);
        repository.save(entity);
        return toResponse(entity);
    }

    @Override
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw notFoundException();
        }
        repository.deleteById(id);
    }
}