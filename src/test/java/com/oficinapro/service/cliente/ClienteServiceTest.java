package com.oficinapro.service.cliente;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.exception.ClienteAlreadyExistsException;
import com.oficinapro.exception.ClienteNotFoundException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.ClienteRepository;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OficinaServiceImpl oficinaService;

    @Mock
    private PessoaService pessoaService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private ClienteServiceImpl service;

    // ─── entidades de apoio ───
    private Oficina oficina;
    private Usuario adminUser;
    private Usuario normalUser;
    private Cliente cliente;
    private ClienteRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        // Oficina compartilhada (id = 1)
        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Test");
        oficina.setCnpj("12345678000195");
        oficina.setTelefone("83999999999");

        // Usuário ADMIN – não está vinculado a nenhuma oficina (sem restrição)
        adminUser = new Usuario();
        adminUser.setRole(Role.ADMIN);

        // Usuário ADMINISTRATIVO – vinculado à oficina 1
        normalUser = new Usuario();
        normalUser.setRole(Role.ADMINISTRATIVO);
        normalUser.setOficina(oficina);

        // Cliente pertencente à oficina 1
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("João Silva");
        cliente.setTelefone("83988887777");
        cliente.setDocumento("12345678901");
        cliente.setOficina(oficina);

        requestDTO = new ClienteRequestDTO("João Silva", "83988887777", "12345678901", 1L);
    }

    // ─────────────────────────── listar ───────────────────────────

    @Test
    @DisplayName("listar() como ADMIN deve retornar todos os clientes paginados")
    void listar_comoAdmin_retornaTodosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(cliente));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findAll(pageable)).thenReturn(page);

        Page<ClienteResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);
        assertThat(resultado.getContent().get(0).nome()).isEqualTo("João Silva");
        assertThat(resultado.getContent().get(0).oficinaId()).isEqualTo(1L);

        verify(clienteRepository, times(1)).findAll(pageable);
        verify(clienteRepository, never()).findByOficinaId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("listar() como ADMINISTRATIVO deve retornar apenas clientes da sua oficina")
    void listar_comoAdministrativo_retornaClientesDaSuaOficina() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(cliente));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser);
        when(clienteRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<ClienteResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);

        verify(clienteRepository, times(1)).findByOficinaId(1L, pageable);
        verify(clienteRepository, never()).findAll(any(Pageable.class));
    }

    // ─────────────────────────── buscarPorId ───────────────────────────

    @Test
    @DisplayName("buscarPorId() como ADMIN deve encontrar cliente de qualquer oficina")
    void buscarPorId_comoAdmin_encontrado_retornaDTO() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResponseDTO resultado = service.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("João Silva");
        assertThat(resultado.documento()).isEqualTo("12345678901");
        assertThat(resultado.oficinaId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorId() deve lançar ClienteNotFoundException quando ID não existe")
    void buscarPorId_naoEncontrado_lancaClienteNotFoundException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ClienteNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId() deve lançar ClienteNotFoundException ao acessar cliente de outra oficina (oculta existência)")
    void buscarPorId_clienteDeOutraOficina_lancaClienteNotFoundException() {
        // Cliente pertence à oficina 2; usuário logado é da oficina 1
        Oficina outraOficina = new Oficina();
        outraOficina.setId(2L);
        outraOficina.setNome("Outra Oficina");
        outraOficina.setCnpj("11222333000181");
        outraOficina.setTelefone("83977776666");

        Cliente clienteDeOutraOficina = new Cliente();
        clienteDeOutraOficina.setId(5L);
        clienteDeOutraOficina.setNome("Cliente Alheio");
        clienteDeOutraOficina.setDocumento("98765432100");
        clienteDeOutraOficina.setOficina(outraOficina);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser); // oficina 1
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(clienteDeOutraOficina));

        assertThatThrownBy(() -> service.buscarPorId(5L))
                .isInstanceOf(ClienteNotFoundException.class);
    }

    // ─────────────────────────── criar ───────────────────────────

    @Test
    @DisplayName("criar() como ADMIN deve criar cliente em qualquer oficina com sucesso")
    void criar_comoAdmin_sucesso() {
        Cliente salvo = new Cliente();
        salvo.setId(1L);
        salvo.setNome("João Silva");
        salvo.setTelefone("83988887777");
        salvo.setDocumento("12345678901");
        salvo.setOficina(oficina);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(salvo);

        ClienteResponseDTO resultado = service.criar(requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("João Silva");
        assertThat(resultado.documento()).isEqualTo("12345678901");
        assertThat(resultado.oficinaId()).isEqualTo(1L);

        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("criar() deve lançar ClienteAlreadyExistsException quando documento já está cadastrado na oficina")
    void criar_documentoDuplicado_lancaClienteAlreadyExistsException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(requestDTO))
                .isInstanceOf(ClienteAlreadyExistsException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() como ADMINISTRATIVO tentando criar em outra oficina deve lançar AccessDeniedException")
    void criar_comoAdministrativo_outraOficina_lancaAccessDeniedException() {
        // Request aponta para oficina 2, mas normalUser pertence à oficina 1
        ClienteRequestDTO requestOutraOficina =
                new ClienteRequestDTO("Novo Cliente", "83999999999", "11122233344", 2L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser); // oficina 1

        assertThatThrownBy(() -> service.criar(requestOutraOficina))
                .isInstanceOf(AccessDeniedException.class);

        verify(clienteRepository, never()).save(any());
    }

    // ─────────────────────────── atualizar ───────────────────────────

    @Test
    @DisplayName("atualizar() como ADMIN deve atualizar cliente com sucesso")
    void atualizar_comoAdmin_sucesso() {
        ClienteRequestDTO requestAtualizar =
                new ClienteRequestDTO("João Atualizado", "83977776666", "12345678901", 1L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteResponseDTO resultado = service.atualizar(1L, requestAtualizar);

        assertThat(resultado).isNotNull();
        // applyUpdate modifica o objeto cliente em lugar; o nome deve ter sido atualizado
        assertThat(resultado.nome()).isEqualTo("João Atualizado");
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("atualizar() deve lançar ClienteNotFoundException quando ID não existe")
    void atualizar_idNaoEncontrado_lancaClienteNotFoundException() {
        ClienteRequestDTO requestAtualizar =
                new ClienteRequestDTO("João Atualizado", "83977776666", "12345678901", 1L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, requestAtualizar))
                .isInstanceOf(ClienteNotFoundException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar() deve lançar ClienteAlreadyExistsException quando documento já pertence a outro cliente da mesma oficina")
    void atualizar_documentoDuplicadoOutroCliente_lancaClienteAlreadyExistsException() {
        ClienteRequestDTO requestAtualizar =
                new ClienteRequestDTO("João Atualizado", "83977776666", "99988877766", 1L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "99988877766", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, requestAtualizar))
                .isInstanceOf(ClienteAlreadyExistsException.class);

        verify(clienteRepository, never()).save(any());
    }

    // ─────────────────────────── deletar ───────────────────────────

    @Test
    @DisplayName("deletar() como ADMIN deve remover cliente com sucesso")
    void deletar_comoAdmin_sucesso() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        service.deletar(1L);

        verify(clienteRepository, times(1)).delete(cliente);
    }

    @Test
    @DisplayName("deletar() deve lançar ClienteNotFoundException quando ID não existe")
    void deletar_idNaoEncontrado_lancaClienteNotFoundException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(ClienteNotFoundException.class);

        verify(clienteRepository, never()).delete(any());
    }

    // ─────────────────────────── listarPorOficinaId ───────────────────────────

    @Test
    @DisplayName("listarPorOficinaId() como ADMIN deve retornar clientes de qualquer oficina")
    void listarPorOficinaId_comoAdmin_sucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(cliente));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(clienteRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<ClienteResponseDTO> resultado = service.listarPorOficinaId(1L, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).oficinaId()).isEqualTo(1L);
    }
}
