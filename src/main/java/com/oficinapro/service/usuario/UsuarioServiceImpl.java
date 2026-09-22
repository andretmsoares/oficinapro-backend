package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioMeUpdateRequestDTO;
import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.usuario.OficinaIncompativelComRoleException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioCannotDeleteSelfException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl
    extends AbstractPessoaServiceImpl<
        Usuario, UsuarioRequestDTO, UsuarioUpdateRequestDTO, UsuarioResponseDTO>
    implements UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioServiceImpl(
      UsuarioRepository usuarioRepository,
      OficinaServiceImpl oficinaService,
      PessoaService pessoaService,
      PasswordEncoder passwordEncoder,
      OficinaAccessValidator oficinaAccessValidator) {
    super(usuarioRepository, oficinaService, pessoaService, oficinaAccessValidator);
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  // Único ponto do sistema onde listar() ainda se ramifica por role. O ADMIN do
  // SaaS administra as contas de acesso — sem isto ele não consegue nem achar o
  // GERENTE de um tenant para redefinir acesso. O desvio existe aqui e em mais
  // lugar nenhum: para dado operacional (clientes, mecânicos, veículos, OS) o
  // ADMIN vê apenas contagem.
  //
  // O ramo do ADMIN não pode cair no super.listar(): ele chama
  // getOficinaIdUsuarioLogado(), e o ADMIN não pertence a nenhuma oficina.
  @Override
  @Transactional(readOnly = true)
  public Page<UsuarioResponseDTO> listar(Pageable pageable) {
    Usuario logado = oficinaAccessValidator.getUsuarioAutenticado();

    if (logado.getRole() == Role.ADMIN) {
      return usuarioRepository.findAll(pageable).map(this::toResponse);
    }
    return super.listar(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  protected void validateBeforeCreate(UsuarioRequestDTO request) {
    if (usuarioRepository.existsByUsername(request.username())) {
      throw new UsernameAlreadyExistsException();
    }
    // Permissão antes de coerência: quem não pode atribuir a role recebe 403,
    // sem pistas sobre o formato correto do payload.
    validarPermissaoParaAtribuirRole(request.role());
    validarCoerenciaRoleOficina(request.role(), request.oficinaId());
  }

  @Override
  @Transactional(readOnly = true)
  protected void validateBeforeUpdate(Long id, UsuarioUpdateRequestDTO request) {
    if (usuarioRepository.existsByUsernameAndIdNot(request.username(), id)) {
      throw new UsernameAlreadyExistsException();
    }

    Usuario alvo = buscarPorEntidadeId(id);
    Usuario logado = oficinaAccessValidator.getUsuarioAutenticado();

    if (alvo.getRole() == Role.ADMIN && logado.getRole() != Role.ADMIN) {
      throw new AccessDeniedException("Apenas ADMIN pode editar uma conta com role ADMIN");
    }

    validarPermissaoParaAtribuirRole(request.role());
    validarCoerenciaRoleOficina(request.role(), request.oficinaId());
  }

  /**
   * ADMIN é o administrador do SaaS: não tem filiação com nenhuma oficina. GERENTE e MECANICO
   * existem sempre dentro de uma oficina.
   */
  private void validarCoerenciaRoleOficina(Role role, Long oficinaId) {
    if (role == Role.ADMIN && oficinaId != null) {
      throw new OficinaIncompativelComRoleException(
          "ADMIN é o administrador do SaaS e não pode ser vinculado a uma oficina");
    }
    if (role != Role.ADMIN && oficinaId == null) {
      throw new OficinaIncompativelComRoleException(
          "O ID da oficina é obrigatório para o cargo " + role);
    }
  }

  /**
   * Hierarquia de criação/promoção de usuários: - ADMIN (do SaaS) pode atribuir qualquer role,
   * inclusive ADMIN. - GERENTE (da oficina) pode criar GERENTE e MECANICO, nunca ADMIN. O
   * isolamento por oficina é garantido em AbstractPessoaServiceImpl.resolverOficina. - MECANICO não
   * gerencia usuários (já bloqueado no @PreAuthorize do controller, replicado aqui como defesa em
   * profundidade).
   */
  private void validarPermissaoParaAtribuirRole(Role roleAlvo) {
    Usuario logado = oficinaAccessValidator.getUsuarioAutenticado();
    Role roleLogado = logado.getRole();

    boolean podeGerenciarUsuarios = roleLogado == Role.ADMIN || roleLogado == Role.GERENTE;
    if (!podeGerenciarUsuarios) {
      throw new AccessDeniedException("Seu perfil não tem permissão para gerenciar usuários");
    }

    if (roleAlvo == Role.ADMIN && roleLogado != Role.ADMIN) {
      throw new AccessDeniedException("Apenas um usuário ADMIN pode criar ou promover outro ADMIN");
    }
  }

  @Override
  protected UsuarioResponseDTO toResponse(Usuario u) {
    return UsuarioResponseDTO.de(u);
  }

  @Override
  @Transactional
  protected Usuario toEntity(UsuarioRequestDTO request, Oficina oficina) {
    Usuario usuario = new Usuario();
    usuario.setNome(request.nome());
    usuario.setDocumento(request.documento());
    usuario.setTelefone(request.telefone());
    usuario.setOficina(oficina); // nulo quando role == ADMIN
    usuario.setUsername(request.username());
    usuario.setPassword(passwordEncoder.encode(request.password()));
    usuario.setRole(request.role());
    return usuario;
  }

  @Override
  @Transactional
  protected void applyUpdate(Usuario usuario, UsuarioUpdateRequestDTO request) {
    usuario.setNome(request.nome());
    usuario.setDocumento(request.documento());
    usuario.setTelefone(request.telefone());
    usuario.setUsername(request.username());
    usuario.setRole(request.role());

    // Mantém o vínculo coerente com a role: promover para ADMIN desliga a oficina,
    // rebaixar para GERENTE/MECANICO exige (e aplica) uma oficina.
    usuario.setOficina(
        request.oficinaId() == null
            ? null
            : oficinaService.buscarPorEntidadeId(request.oficinaId()));

    if (request.password() != null && !request.password().isBlank()) {
      usuario.setPassword(passwordEncoder.encode(request.password()));
    }
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoCreate(UsuarioRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdCreate(UsuarioRequestDTO r) {
    return r.oficinaId();
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoUpdate(UsuarioUpdateRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdUpdate(UsuarioUpdateRequestDTO r) {
    return r.oficinaId();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException notFoundException() {
    return new UsuarioNotFoundException();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException alreadyExistsException() {
    return new UsuarioAlreadyExistsException();
  }

  @Override
  @Transactional
  public UsuarioResponseDTO atualizarMe(UsuarioMeUpdateRequestDTO request) {
    // 1. Obtém o usuário do contexto de segurança (Detached)
    Usuario usuarioLogado = oficinaAccessValidator.getUsuarioAutenticado();

    // 2. Busca a entidade real no banco DENTRO desta transação (Managed)
    Usuario usuario = buscarPorEntidadeId(usuarioLogado.getId());

    // 3. Valida se o novo username já existe
    if (usuarioRepository.existsByUsernameAndIdNot(
        request.username(), usuario.getId())) {
      throw new UsernameAlreadyExistsException();
    }

    // 4. Atualiza os dados na entidade rastreada pelo Hibernate
    usuario.setNome(request.nome());
    usuario.setDocumento(request.documento());
    usuario.setTelefone(request.telefone());
    usuario.setUsername(request.username());

    if (request.password() != null && !request.password().isBlank()) {
      usuario.setPassword(passwordEncoder.encode(request.password()));
    }

    // 5. Salva no banco de dados
    usuario = usuarioRepository.save(usuario);

    return toResponse(usuario);
  }

  @Transactional
  @Override
  public void deletar(Long id) {
    Usuario usuarioLogado = oficinaAccessValidator.getUsuarioAutenticado();
    Usuario usuario = buscarPorEntidadeId(id);

    if (usuarioLogado.getId().equals(usuario.getId())) {
      throw new UsuarioCannotDeleteSelfException();
    }
    
    repository.delete(usuario);
  }
}
