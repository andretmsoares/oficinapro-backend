package com.oficinapro.service.pessoaCrud;

import com.oficinapro.model.Oficina;
import com.oficinapro.model.Pessoa;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.PessoaCrudRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

public abstract class AbstractPessoaServiceImpl<T extends Pessoa, C, U, RES>
        implements PessoaCrudService<C, U, RES, T> {

    protected final PessoaCrudRepository<T> repository;
    protected final OficinaServiceImpl oficinaService;
    protected final PessoaService pessoaService;
    protected final AuthenticatedUserProvider authenticatedUserProvider;

    protected AbstractPessoaServiceImpl(PessoaCrudRepository<T> repository,
                                        OficinaServiceImpl oficinaService,
                                        PessoaService pessoaService,
                                        AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.oficinaService = oficinaService;
        this.pessoaService = pessoaService;
        this.authenticatedUserProvider = authenticatedUserProvider;
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

    private Long oficinaObrigatoriaDoLogado(Usuario logado) {
        Long oficinaId = logado.getOficina() != null ? logado.getOficina().getId() : null;
        if (oficinaId == null) {
            throw new AccessDeniedException("Usuário não está vinculado a nenhuma oficina");
        }
        return oficinaId;
    }

    /** Valida se o usuário logado pode operar sobre a oficina informada (usado em criar/atualizar). */
    protected void validarAcessoOficina(Long oficinaId) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        if (!oficinaDoLogado.equals(oficinaId)) {
            throw new AccessDeniedException("Você só pode acessar dados da sua própria oficina");
        }
    }

    /**
     * Resolve a oficina do registro validando o acesso do usuário logado.
     *
     * Registro sem oficina existe apenas para o ADMIN do SaaS (ver Usuario). Para as
     * demais entidades o {@code oficinaId} é @NotNull no DTO, então o ramo do nulo
     * nunca é alcançado por elas.
     */
    protected Oficina resolverOficina(Long oficinaId) {
        if (oficinaId == null) {
            Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
            if (logado.getRole() != Role.ADMIN) {
                throw new AccessDeniedException(
                        "Somente o ADMIN do SaaS pode manter registros sem oficina");
            }
            return null;
        }
        validarAcessoOficina(oficinaId);
        return oficinaService.buscarPorEntidadeId(oficinaId);
    }

    /**
     * Valida se o usuário logado pode ver este registro específico.
     * Propositalmente lança "não encontrado" (não "acesso negado") para não revelar
     * a existência de registros de outras oficinas para quem não tem acesso a eles.
     */
    private void validarAcessoAoRegistro(T entity) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        Long oficinaDoRegistro = entity.getOficina() != null ? entity.getOficina().getId() : null;
        if (!oficinaDoLogado.equals(oficinaDoRegistro)) {
            throw notFoundException();
        }
    }

    @Override
    public Page<RES> listar(Pageable pageable) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return repository.findAll(pageable).map(this::toResponse);
        }
        Long oficinaId = oficinaObrigatoriaDoLogado(logado);
        return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
    }

    @Override
    public Page<RES> listarPorOficinaId(Long oficinaId, Pageable pageable) {
        validarAcessoOficina(oficinaId);
        return repository.findByOficinaId(oficinaId, pageable).map(this::toResponse);
    }

    @Override
    public T buscarPorEntidadeId(Long id) {
        T entity = repository.findById(id).orElseThrow(this::notFoundException);
        validarAcessoAoRegistro(entity);
        return entity;
    }

    @Override
    public RES buscarPorId(Long id) {
        return toResponse(buscarPorEntidadeId(id));
    }

    @Override
    public RES buscarPorNome(String nome) {
        T entity = repository.findByNome(nome).orElseThrow(this::notFoundException);
        validarAcessoAoRegistro(entity);
        return toResponse(entity);
    }

    @Override
    public RES buscarPorDocumento(String documento) {
        T entity = repository.findByDocumento(documento).orElseThrow(this::notFoundException);
        validarAcessoAoRegistro(entity);
        return toResponse(entity);
    }

    @Override
    public RES criar(C request) {
        Long oficinaId = extractOficinaIdCreate(request);

        Oficina oficina = resolverOficina(oficinaId);

        // Sem oficina não há escopo de unicidade: a constraint do banco é
        // (oficina_id, documento) e no Postgres NULL não colide com NULL.
        if (oficinaId != null
                && pessoaService.existsByOficinaIdAndDocumento(oficinaId, extractDocumentoCreate(request))) {
            throw alreadyExistsException();
        }

        validateBeforeCreate(request);

        T entity = toEntity(request, oficina);
        entity = repository.save(entity);
        return toResponse(entity);
    }

    @Override
    public RES atualizar(Long id, U request) {
        T entity = buscarPorEntidadeId(id); // já valida acesso ao registro atual

        Long oficinaId = extractOficinaIdUpdate(request);
        // Valida também a oficina de destino (evita "mover" o registro pra outra oficina indevidamente).
        // Quem eventualmente precisa reatribuir a oficina faz isso no próprio applyUpdate.
        resolverOficina(oficinaId);

        if (oficinaId != null
                && pessoaService.existsByOficinaIdAndDocumentoExcluindoId(oficinaId, extractDocumentoUpdate(request), id)) {
            throw alreadyExistsException();
        }

        validateBeforeUpdate(id, request);

        applyUpdate(entity, request);
        repository.save(entity);
        return toResponse(entity);
    }

    @Override
    public void deletar(Long id) {
        T entity = buscarPorEntidadeId(id); // já valida acesso
        repository.delete(entity);
    }
}