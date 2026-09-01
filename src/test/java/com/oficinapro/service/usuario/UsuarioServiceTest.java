package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UsuarioServiceTest {

    // UsuarioRepository estende PessoaCrudRepository<Usuario>.
    // @InjectMocks usará este mock tanto para super.repository quanto para this.usuarioRepository,
    // pois o construtor de UsuarioServiceImpl recebe um único UsuarioRepository e o passa ao super().
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OficinaServiceImpl oficinaService;

    @Mock
    private PessoaService pessoaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private UsuarioServiceImpl service;

    // ─── entidades de apoio ───
    private Oficina oficina;
    private Usuario adminUser;
    private Usuario administrativoUser;

    /** Usuario-alvo persistido (pertence à oficina 1, role ADMINISTRATIVO). */
    private Usuario usuarioAlvo;

    private UsuarioRequestDTO createRequest;
    private UsuarioUpdateRequestDTO updateRequest;

    @BeforeEach
    void setUp() {
        // Oficina id = 1
        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Test");
        oficina.setCnpj("12345678000195");
        oficina.setTelefone("83999999999");

        // ADMIN — sem restrição de oficina
        adminUser = new Usuario();
        adminUser.setRole(Role.ADMIN);

        // ADMINISTRATIVO — vinculado à oficina 1
        administrativoUser = new Usuario();
        administrativoUser.setRole(Role.ADMINISTRATIVO);
        administrativoUser.setOficina(oficina);

        // Usuário-alvo já persistido
        usuarioAlvo = new Usuario();
        usuarioAlvo.setId(1L);
        usuarioAlvo.setNome("Usuario Original");
        usuarioAlvo.setTelefone("83988887777");
        usuarioAlvo.setDocumento("12345678901");
        usuarioAlvo.setOficina(oficina);
        usuarioAlvo.setUsername("usuario.original");
        usuarioAlvo.setPassword("$2a$10$hashOriginal");
        usuarioAlvo.setRole(Role.ADMINISTRATIVO);

        // DTO de criação (ADMIN cria um usuário ADMINISTRATIVO na oficina 1)
        createRequest = new UsuarioRequestDTO(
                "Novo Usuario", "83977776666", "99988877766",
                1L, "novo.usuario", "senha1234", Role.ADMINISTRATIVO);

        // DTO de atualização (mantém username e documento, apenas muda o nome e telefone)
        updateRequest = new UsuarioUpdateRequestDTO(
                "Usuario Atualizado", "83966665555", "12345678901",
                1L, "usuario.original", null, Role.ADMINISTRATIVO);
    }

    // ─────────────────────────── listar ───────────────────────────

    @Test
    @DisplayName("listar() como ADMIN deve retornar todos os usuários paginados")
    void listar_comoAdmin_retornaTodosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuarioAlvo));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findAll(pageable)).thenReturn(page);

        Page<UsuarioResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);
        assertThat(resultado.getContent().get(0).username()).isEqualTo("usuario.original");
        assertThat(resultado.getContent().get(0).role()).isEqualTo(Role.ADMINISTRATIVO);

        verify(usuarioRepository, times(1)).findAll(pageable);
        verify(usuarioRepository, never()).findByOficinaId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("listar() como ADMINISTRATIVO deve retornar apenas usuários da sua oficina")
    void listar_comoAdministrativo_retornaUsuariosDaSuaOficina() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuarioAlvo));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(administrativoUser);
        when(usuarioRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<UsuarioResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);

        verify(usuarioRepository, times(1)).findByOficinaId(1L, pageable);
        verify(usuarioRepository, never()).findAll(any(Pageable.class));
    }

    // ─────────────────────────── buscarPorId ───────────────────────────

    @Test
    @DisplayName("buscarPorId() como ADMIN deve encontrar usuário de qualquer oficina")
    void buscarPorId_comoAdmin_encontrado_retornaDTO() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAlvo));

        UsuarioResponseDTO resultado = service.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Usuario Original");
        assertThat(resultado.username()).isEqualTo("usuario.original");
        assertThat(resultado.role()).isEqualTo(Role.ADMINISTRATIVO);
        assertThat(resultado.oficinaId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorId() deve lançar UsuarioNotFoundException quando ID não existe")
    void buscarPorId_naoEncontrado_lancaUsuarioNotFoundException() {

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(UsuarioNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId() deve lançar UsuarioNotFoundException ao acessar usuário de outra oficina (oculta existência)")
    void buscarPorId_usuarioDeOutraOficina_lancaUsuarioNotFoundException() {
        Oficina outraOficina = new Oficina();
        outraOficina.setId(2L);
        outraOficina.setNome("Outra Oficina");
        outraOficina.setCnpj("11222333000181");
        outraOficina.setTelefone("83977776666");

        Usuario usuarioAlheio = new Usuario();
        usuarioAlheio.setId(5L);
        usuarioAlheio.setNome("Usuário Alheio");
        usuarioAlheio.setDocumento("98765432100");
        usuarioAlheio.setOficina(outraOficina);
        usuarioAlheio.setUsername("alheio.user");
        usuarioAlheio.setRole(Role.ADMINISTRATIVO);

        // administrativoUser pertence à oficina 1; usuarioAlheio à oficina 2
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(administrativoUser);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuarioAlheio));

        assertThatThrownBy(() -> service.buscarPorId(5L))
                .isInstanceOf(UsuarioNotFoundException.class);
    }

    // ─────────────────────────── criar ───────────────────────────

    @Test
    @DisplayName("criar() como ADMIN deve criar usuário com sucesso")
    void criar_comoAdmin_sucesso() {
        // Usuário que será retornado pelo save
        Usuario salvo = new Usuario();
        salvo.setId(2L);
        salvo.setNome("Novo Usuario");
        salvo.setTelefone("83977776666");
        salvo.setDocumento("99988877766");
        salvo.setOficina(oficina);
        salvo.setUsername("novo.usuario");
        salvo.setPassword("$2a$10$hashNovo");
        salvo.setRole(Role.ADMINISTRATIVO);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "99988877766")).thenReturn(false);
        when(usuarioRepository.existsByUsername("novo.usuario")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("$2a$10$hashNovo");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(salvo);

        UsuarioResponseDTO resultado = service.criar(createRequest);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(2L);
        assertThat(resultado.nome()).isEqualTo("Novo Usuario");
        assertThat(resultado.username()).isEqualTo("novo.usuario");
        assertThat(resultado.role()).isEqualTo(Role.ADMINISTRATIVO);
        assertThat(resultado.oficinaId()).isEqualTo(1L);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("criar() deve lançar UsuarioAlreadyExistsException quando documento já está cadastrado na oficina")
    void criar_documentoDuplicado_lancaUsuarioAlreadyExistsException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "99988877766")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(createRequest))
                .isInstanceOf(UsuarioAlreadyExistsException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() deve lançar UsernameAlreadyExistsException quando username já está em uso")
    void criar_usernameDuplicado_lancaUsernameAlreadyExistsException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "99988877766")).thenReturn(false);
        when(usuarioRepository.existsByUsername("novo.usuario")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(createRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() como ADMINISTRATIVO tentando criar usuário com role ADMIN deve lançar AccessDeniedException")
    void criar_comoAdministrativo_roleAdmin_lancaAccessDeniedException() {
        // Request pedindo criação de um usuário ADMIN
        UsuarioRequestDTO requestAdminRole = new UsuarioRequestDTO(
                "Novo Admin", "83977776666", "55544433322",
                1L, "novo.admin", "senha1234", Role.ADMIN);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(administrativoUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "55544433322")).thenReturn(false);
        when(usuarioRepository.existsByUsername("novo.admin")).thenReturn(false);

        assertThatThrownBy(() -> service.criar(requestAdminRole))
                .isInstanceOf(AccessDeniedException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() como ADMINISTRATIVO tentando criar em outra oficina deve lançar AccessDeniedException")
    void criar_comoAdministrativo_outraOficina_lancaAccessDeniedException() {
        UsuarioRequestDTO requestOutraOficina = new UsuarioRequestDTO(
                "Novo Usuario", "83977776666", "55544433322",
                2L, "novo.usuario", "senha1234", Role.ADMINISTRATIVO);

        // administrativoUser pertence à oficina 1; request aponta para oficina 2
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(administrativoUser);

        assertThatThrownBy(() -> service.criar(requestOutraOficina))
                .isInstanceOf(AccessDeniedException.class);

        verify(usuarioRepository, never()).save(any());
    }

    // ─────────────────────────── atualizar ───────────────────────────

    @Test
    @DisplayName("atualizar() como ADMIN deve atualizar usuário com sucesso")
    void atualizar_comoAdmin_sucesso() {
        // Nota: AbstractPessoaServiceImpl.atualizar() chama buscarPorEntidadeId(id) diretamente
        // e, após, validateBeforeUpdate() que também chama buscarPorEntidadeId(id) internamente.
        // Ambas as chamadas a findById(1L) retornam o mesmo stub.
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAlvo));
        when(usuarioRepository.existsByUsernameAndIdNot("usuario.original", 1L)).thenReturn(false);
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioAlvo);

        UsuarioResponseDTO resultado = service.atualizar(1L, updateRequest);

        assertThat(resultado).isNotNull();
        // applyUpdate modifica usuarioAlvo em lugar; nome e telefone devem refletir o updateRequest
        assertThat(resultado.nome()).isEqualTo("Usuario Atualizado");
        assertThat(resultado.username()).isEqualTo("usuario.original");
        assertThat(resultado.role()).isEqualTo(Role.ADMINISTRATIVO);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("atualizar() deve lançar UsuarioNotFoundException quando ID não existe")
    void atualizar_idNaoEncontrado_lancaUsuarioNotFoundException() {

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, updateRequest))
                .isInstanceOf(UsuarioNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar() deve lançar UsernameAlreadyExistsException quando username já pertence a outro usuário")
    void atualizar_usernameDuplicadoOutroUsuario_lancaUsernameAlreadyExistsException() {
        UsuarioUpdateRequestDTO requestNovoUsername = new UsuarioUpdateRequestDTO(
                "Usuario Atualizado", "83966665555", "12345678901",
                1L, "outro.usuario.existente", null, Role.ADMINISTRATIVO);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAlvo));
        // pessoaService verifica antes de validateBeforeUpdate
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L)).thenReturn(false);
        when(usuarioRepository.existsByUsernameAndIdNot("outro.usuario.existente", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, requestNovoUsername))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar() deve lançar UsuarioAlreadyExistsException quando documento já pertence a outro usuário da mesma oficina")
    void atualizar_documentoDuplicadoOutroUsuario_lancaUsuarioAlreadyExistsException() {
        UsuarioUpdateRequestDTO requestNovoDoc = new UsuarioUpdateRequestDTO(
                "Usuario Atualizado", "83966665555", "99988877766",
                1L, "usuario.original", null, Role.ADMINISTRATIVO);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAlvo));
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "99988877766", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, requestNovoDoc))
                .isInstanceOf(UsuarioAlreadyExistsException.class);

        verify(usuarioRepository, never()).save(any());
    }

    // ─────────────────────────── deletar ───────────────────────────

    @Test
    @DisplayName("deletar() como ADMIN deve remover usuário com sucesso")
    void deletar_comoAdmin_sucesso() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAlvo));

        service.deletar(1L);

        verify(usuarioRepository, times(1)).delete(usuarioAlvo);
    }

    @Test
    @DisplayName("deletar() deve lançar UsuarioNotFoundException quando ID não existe")
    void deletar_idNaoEncontrado_lancaUsuarioNotFoundException() {

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(UsuarioNotFoundException.class);

        verify(usuarioRepository, never()).delete(any());
    }

    // ─────────────────────────── listarPorOficinaId ───────────────────────────

    @Test
    @DisplayName("listarPorOficinaId() como ADMIN deve retornar usuários de qualquer oficina")
    void listarPorOficinaId_comoAdmin_sucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuarioAlvo));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(usuarioRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<UsuarioResponseDTO> resultado = service.listarPorOficinaId(1L, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).oficinaId()).isEqualTo(1L);
    }
}
