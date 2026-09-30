package com.oficinapro.service.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.ClienteRepository;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ClienteServiceTest {

  @Mock private ClienteRepository clienteRepository;

  @Mock private OficinaServiceImpl oficinaService;

  @Mock private PessoaService pessoaService;

  // Adicionado no refactor: o isolamento por oficina saiu dos services e passou
  // a viver em OficinaAccessValidator. Sem este mock o campo fica nulo e
  // qualquer chamada ao service estoura NullPointerException.
  @Mock private com.oficinapro.security.OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private ClienteServiceImpl service;

  // ─── entidades de apoio ───
  private Oficina oficina;
  private Cliente cliente;
  private ClienteRequestDTO requestDTO;

  @BeforeEach
  void setUp() {
    // Oficina compartilhada (id = 1)
    oficina = new Oficina();
    oficina.setId(1L);
    oficina.setNome("OFICINA TEST");
    oficina.setCnpj("12345678000195");
    oficina.setTelefone("83999999999");

    // Cliente pertencente à oficina 1
    cliente = new Cliente();
    cliente.setId(1L);
    cliente.setNome("JOÃO SILVA");
    cliente.setTelefone("83988887777");
    cliente.setDocumento("12345678901");
    cliente.setOficina(oficina);

    requestDTO = new ClienteRequestDTO("João Silva", "83988887777", "12345678901");
  }

  // ─────────────────────────── listar ───────────────────────────

  @Test
  @DisplayName("listar() como GERENTE deve retornar apenas clientes da sua oficina")
  void listar_comoAdministrativo_retornaClientesDaSuaOficina() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Cliente> page = new PageImpl<>(List.of(cliente));

    // Quem resolve a oficina do usuário logado agora é o OficinaAccessValidator.
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaId(1L, pageable)).thenReturn(page);

    Page<ClienteResponseDTO> resultado = service.listar(pageable);

    assertThat(resultado).isNotNull();
    assertThat(resultado.getContent()).hasSize(1);
    assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);

    verify(clienteRepository, times(1)).findByOficinaId(1L, pageable);
    verify(clienteRepository, never()).findAll(any(Pageable.class));
  }

  // ─────────────────────────── buscar (paginado) ───────────────────────────

  @Test
  @DisplayName("buscar() deve consultar a oficina do usuário com o termo sem espaços nas pontas")
  void buscar_comTermo_consultaNaOficinaDoUsuario() {
    Pageable pageable = PageRequest.of(0, 10);

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.buscar(1L, "JOAO", pageable))
        .thenReturn(new PageImpl<>(List.of(cliente)));

    Page<ClienteResponseDTO> resultado = service.buscar("  joao ", pageable);

    assertThat(resultado.getContent()).extracting(ClienteResponseDTO::id).containsExactly(1L);
  }

  @Test
  @DisplayName("buscar() sem termo deve listar a oficina do usuário")
  void buscar_semTermo_listaDaOficina() {
    Pageable pageable = PageRequest.of(0, 10);

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(cliente)));

    Page<ClienteResponseDTO> resultado = service.buscar("   ", pageable);

    assertThat(resultado.getContent()).hasSize(1);
    verify(clienteRepository, never()).buscar(any(), any(), any());
  }

  // ─────────────────────────── buscarPorId ───────────────────────────

  @Test
  @DisplayName("buscarPorId() deve devolver o DTO do cliente encontrado")
  void buscarPorId_encontrado_retornaDTO() {
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

    ClienteResponseDTO resultado = service.buscarPorId(1L);

    assertThat(resultado).isNotNull();
    assertThat(resultado.id()).isEqualTo(1L);
    assertThat(resultado.nome()).isEqualTo("JOÃO SILVA");
    assertThat(resultado.documento()).isEqualTo("12345678901");
    assertThat(resultado.oficinaId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("buscarPorId() deve lançar ClienteNotFoundException quando ID não existe")
  void buscarPorId_naoEncontrado_lancaClienteNotFoundException() {

    when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(ClienteNotFoundException.class);
  }

  @Test
  @DisplayName(
      "buscarPorId() deve lançar ClienteNotFoundException ao acessar cliente de outra oficina (oculta existência)")
  void buscarPorId_clienteDeOutraOficina_lancaClienteNotFoundException() {
    // Cliente pertence à oficina 2; usuário logado é da oficina 1
    Oficina outraOficina = new Oficina();
    outraOficina.setId(2L);
    outraOficina.setNome("OUTRA OFICINA");
    outraOficina.setCnpj("11222333000181");
    outraOficina.setTelefone("83977776666");

    Cliente clienteDeOutraOficina = new Cliente();
    clienteDeOutraOficina.setId(5L);
    clienteDeOutraOficina.setNome("CLIENTE ALHEIO");
    clienteDeOutraOficina.setDocumento("98765432100");
    clienteDeOutraOficina.setOficina(outraOficina);

    when(clienteRepository.findById(5L)).thenReturn(Optional.of(clienteDeOutraOficina));
    // O isolamento é delegado ao validador, que devolve a exceção de "não
    // encontrado" da própria entidade para não revelar que o registro existe.
    doThrow(new ClienteNotFoundException())
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));

    assertThatThrownBy(() -> service.buscarPorId(5L)).isInstanceOf(ClienteNotFoundException.class);

    verify(oficinaAccessValidator).validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));
  }

  // ─────────────────────────── criar ───────────────────────────

  @Test
  @DisplayName("criar() como ADMIN deve ser negado, pois o ADMIN do SaaS não pertence a oficina")
  void criar_comoAdmin_negado() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado())
        .thenThrow(new UsuarioAcessDeniedException());

    assertThatThrownBy(() -> service.criar(requestDTO))
        .isInstanceOf(UsuarioAcessDeniedException.class);

    verify(clienteRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "criar() deve lançar ClienteAlreadyExistsException quando documento já está cadastrado na oficina")
  void criar_documentoDuplicado_lancaClienteAlreadyExistsException() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(requestDTO))
        .isInstanceOf(ClienteAlreadyExistsException.class);

    verify(clienteRepository, never()).save(any());
  }

  @Test
  @DisplayName("criar() como GERENTE deve criar o cliente na oficina do usuário autenticado")
  void criar_comoGerente_usaOficinaDoUsuarioAutenticado() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);

    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);

    when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(false);

    when(clienteRepository.save(any(Cliente.class)))
        .thenAnswer(
            invocation -> {
              Cliente clienteSalvo = invocation.getArgument(0);
              clienteSalvo.setId(2L);
              return clienteSalvo;
            });

    ClienteResponseDTO resultado = service.criar(requestDTO);

    assertThat(resultado).isNotNull();
    assertThat(resultado.oficinaId()).isEqualTo(1L);

    verify(oficinaAccessValidator).validarAcessoOficina(1L);

    verify(oficinaService).buscarPorEntidadeId(1L);

    verify(clienteRepository).save(argThat(m -> m.getOficina().getId().equals(1L)));
  }

  // ─────────────────────────── atualizar ───────────────────────────

  @Test
  @DisplayName("atualizar() como GERENTE deve atualizar cliente com sucesso")
  void atualizar_comoGerente_sucesso() {
    ClienteRequestDTO requestAtualizar =
        new ClienteRequestDTO("João Atualizado", "83977776666", "12345678901");

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L))
        .thenReturn(false);
    when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

    ClienteResponseDTO resultado = service.atualizar(1L, requestAtualizar);

    assertThat(resultado).isNotNull();
    // applyUpdate modifica o objeto cliente em lugar; o nome deve ter sido atualizado
    assertThat(resultado.nome()).isEqualTo("JOAO ATUALIZADO");
    verify(clienteRepository, times(1)).save(any(Cliente.class));
  }

  @Test
  @DisplayName("atualizar() deve lançar ClienteNotFoundException quando ID não existe")
  void atualizar_idNaoEncontrado_lancaClienteNotFoundException() {
    ClienteRequestDTO requestAtualizar =
        new ClienteRequestDTO("João Atualizado", "83977776666", "12345678901");

    when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atualizar(99L, requestAtualizar))
        .isInstanceOf(ClienteNotFoundException.class);

    verify(clienteRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "atualizar() deve lançar ClienteAlreadyExistsException quando documento já pertence a outro cliente da mesma oficina")
  void atualizar_documentoDuplicadoOutroCliente_lancaClienteAlreadyExistsException() {
    ClienteRequestDTO requestAtualizar =
        new ClienteRequestDTO("João Atualizado", "83977776666", "99988877766");

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "99988877766", 1L))
        .thenReturn(true);

    assertThatThrownBy(() -> service.atualizar(1L, requestAtualizar))
        .isInstanceOf(ClienteAlreadyExistsException.class);

    verify(clienteRepository, never()).save(any());
  }

  // ─────────────────────────── deletar ───────────────────────────

  @Test
  @DisplayName("deletar() deve remover o cliente encontrado")
  void deletar_encontrado_sucesso() {
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

    service.deletar(1L);

    verify(clienteRepository, times(1)).delete(cliente);
  }

  @Test
  @DisplayName("deletar() deve lançar ClienteNotFoundException quando ID não existe")
  void deletar_idNaoEncontrado_lancaClienteNotFoundException() {

    when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ClienteNotFoundException.class);

    verify(clienteRepository, never()).delete(any());
  }

  // ─────────────────────────── buscar (termo nulo) ───────────────────────────

  @Test
  @DisplayName("buscar() com termo nulo deve listar a oficina do usuário sem filtrar")
  void buscar_termoNulo_listaDaOficina() {
    Pageable pageable = PageRequest.of(0, 10);

    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(cliente)));

    Page<ClienteResponseDTO> resultado = service.buscar(null, pageable);

    assertThat(resultado.getContent()).hasSize(1);
    verify(clienteRepository, never()).buscar(any(), any(), any());
  }

  // ─────────────────────────── buscarPorEntidadeId ───────────────────────────

  @Test
  @DisplayName(
      "buscarPorEntidadeId() com cliente sem oficina deve validar o acesso com oficina nula")
  void buscarPorEntidadeId_clienteSemOficina_validaComOficinaNula() {
    Cliente semOficina = new Cliente();
    semOficina.setId(8L);
    when(clienteRepository.findById(8L)).thenReturn(Optional.of(semOficina));

    Cliente resultado = service.buscarPorEntidadeId(8L);

    assertThat(resultado).isSameAs(semOficina);
    verify(oficinaAccessValidator).validarAcessoAoRegistro(eq(null), any(RuntimeException.class));
  }

  // ─────────────────────────── buscarPorNome ───────────────────────────

  @Test
  @DisplayName("buscarPorNome() deve normalizar o nome e filtrar pela oficina do usuário")
  void buscarPorNome_normalizaEFiltraPelaOficina() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaIdAndNomeContainingIgnoreCase(1L, "JOAO"))
        .thenReturn(List.of(cliente));

    List<ClienteResponseDTO> resultado = service.buscarPorNome("  joão ");

    assertThat(resultado).extracting(ClienteResponseDTO::id).containsExactly(1L);
  }

  @Test
  @DisplayName("buscarPorNome() sem resultados deve devolver lista vazia")
  void buscarPorNome_semResultados_listaVazia() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaIdAndNomeContainingIgnoreCase(1L, "ZZZ"))
        .thenReturn(List.of());

    assertThat(service.buscarPorNome("zzz")).isEmpty();
  }

  // ─────────────────────────── buscarPorDocumento ───────────────────────────

  @Test
  @DisplayName("buscarPorDocumento() deve devolver o cliente da oficina do usuário")
  void buscarPorDocumento_encontrado_retornaDTO() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaIdAndDocumento(1L, "12345678901"))
        .thenReturn(Optional.of(cliente));

    ClienteResponseDTO resultado = service.buscarPorDocumento("12345678901");

    assertThat(resultado.id()).isEqualTo(1L);
    assertThat(resultado.documento()).isEqualTo("12345678901");
  }

  @Test
  @DisplayName("buscarPorDocumento() inexistente deve lançar ClienteNotFoundException")
  void buscarPorDocumento_naoEncontrado_lancaClienteNotFoundException() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.findByOficinaIdAndDocumento(1L, "000")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscarPorDocumento("000"))
        .isInstanceOf(ClienteNotFoundException.class);
  }

  // ─────────────────────────── consultas de ADMIN ───────────────────────────

  @Test
  @DisplayName("listarTodos() como ADMIN deve paginar clientes de todas as oficinas")
  void listarTodos_comoAdmin_retornaTodos() {
    Pageable pageable = PageRequest.of(0, 10);
    when(clienteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(cliente)));

    Page<ClienteResponseDTO> resultado = service.listarTodos(pageable);

    assertThat(resultado.getContent()).extracting(ClienteResponseDTO::id).containsExactly(1L);
    verify(oficinaAccessValidator).validarRole(Role.ADMIN);
  }

  @Test
  @DisplayName("listarTodos() sem ser ADMIN deve ser negado sem consultar o repositório")
  void listarTodos_semSerAdmin_negado() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.listarTodos(PageRequest.of(0, 10)))
        .isInstanceOf(AccessDeniedException.class);

    verify(clienteRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("buscarPorNomeAdmin() como ADMIN deve buscar pelo nome exato em todas as oficinas")
  void buscarPorNomeAdmin_comoAdmin_retornaResultados() {
    when(clienteRepository.findByNome("JOÃO SILVA")).thenReturn(List.of(cliente));

    List<ClienteResponseDTO> resultado = service.buscarPorNomeAdmin("JOÃO SILVA");

    assertThat(resultado).extracting(ClienteResponseDTO::id).containsExactly(1L);
    verify(oficinaAccessValidator).validarRole(Role.ADMIN);
  }

  @Test
  @DisplayName("buscarPorNomeAdmin() sem ser ADMIN deve ser negado")
  void buscarPorNomeAdmin_semSerAdmin_negado() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.buscarPorNomeAdmin("JOÃO SILVA"))
        .isInstanceOf(AccessDeniedException.class);

    verify(clienteRepository, never()).findByNome(any());
  }

  @Test
  @DisplayName("buscarPorDocumentoAdmin() como ADMIN deve buscar o documento em todas as oficinas")
  void buscarPorDocumentoAdmin_comoAdmin_retornaResultados() {
    when(clienteRepository.findByDocumento("12345678901")).thenReturn(List.of(cliente));

    List<ClienteResponseDTO> resultado = service.buscarPorDocumentoAdmin("12345678901");

    assertThat(resultado).extracting(ClienteResponseDTO::documento).containsExactly("12345678901");
    verify(oficinaAccessValidator).validarRole(Role.ADMIN);
  }

  @Test
  @DisplayName("buscarPorDocumentoAdmin() sem ser ADMIN deve ser negado")
  void buscarPorDocumentoAdmin_semSerAdmin_negado() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.buscarPorDocumentoAdmin("12345678901"))
        .isInstanceOf(AccessDeniedException.class);

    verify(clienteRepository, never()).findByDocumento(any());
  }

  // ─────────────────────────── count ───────────────────────────

  @Test
  @DisplayName("count() deve contar apenas os clientes da oficina do usuário")
  void count_contaClientesDaOficinaDoUsuario() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(clienteRepository.countByOficinaId(1L)).thenReturn(6);

    assertThat(service.count()).isEqualTo(6);
  }

  // ─────────────────────────── criar/atualizar (oficina) ───────────────────────────

  @Test
  @DisplayName("criar() deve normalizar o nome e persistir o cliente na oficina do usuário")
  void criar_normalizaNomeEPersisteNaOficina() {
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(false);
    when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

    ClienteResponseDTO resultado =
        service.criar(new ClienteRequestDTO("  joão da silva ", "83988887777", "12345678901"));

    assertThat(resultado.nome()).isEqualTo("JOAO DA SILVA");
    assertThat(resultado.oficinaId()).isEqualTo(1L);
    verify(oficinaAccessValidator).validarAcessoOficina(1L);
  }

  @Test
  @DisplayName("atualizar() deve normalizar o nome e persistir as alterações")
  void atualizar_normalizaNomeEPersiste() {
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L))
        .thenReturn(false);

    ClienteResponseDTO resultado =
        service.atualizar(1L, new ClienteRequestDTO("maria á", "83911112222", "12345678901"));

    assertThat(resultado.nome()).isEqualTo("MARIA A");
    assertThat(resultado.telefone()).isEqualTo("83911112222");
    verify(clienteRepository).save(cliente);
  }
}
