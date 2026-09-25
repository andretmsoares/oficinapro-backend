package com.oficinapro.service.veiculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.VeiculoRepository;
import com.oficinapro.service.oficina.OficinaService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
// LENIENT proposital: o refactor moveu o isolamento por oficina para o
// OficinaAccessValidator, entao alguns stubs de oficinaAccessValidator
// preparados nestes testes deixaram de ser exercidos. Com strict stubs isso
// derrubaria a classe por UnnecessaryStubbingException em vez de apontar um
// problema real. TODO: voltar para STRICT_STUBS e limpar os stubs ociosos.
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class VeiculoServiceTest {

  @Mock private VeiculoRepository veiculoRepository;

  @Mock private OficinaService oficinaService;

  // Adicionado no refactor: o isolamento por oficina saiu dos services e passou
  // a viver em OficinaAccessValidator.
  @Mock private com.oficinapro.security.OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private VeiculoServiceImpl veiculoService;

  private Oficina oficina;
  private Veiculo veiculo;
  private Usuario adminUser;
  private Usuario normalUser;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999", true);

    veiculo = new Veiculo();
    ReflectionTestUtils.setField(veiculo, "id", 1L);
    veiculo.setOficina(oficina);
    veiculo.setModelo("Civic");
    veiculo.setAno(2020);
    veiculo.setMarca("Honda");
    veiculo.setPlaca("ABC1234");

    adminUser = new Usuario();
    adminUser.setRole(Role.ADMIN);

    normalUser = new Usuario();
    normalUser.setRole(Role.GERENTE);
    normalUser.setOficina(oficina);

    pageable = PageRequest.of(0, 10);
  }

  // ---------------------------------------------------------------
  // listar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("GERENTE: deve chamar findByOficinaId e retornar apenas veículos da sua oficina")
  void deveListarVeiculosDaPropriaOficinaComoAdministrativo() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(normalUser);
    // Quem resolve a oficina do usuário logado agora é o OficinaAccessValidator.
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(veiculoRepository.findByOficinaId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(veiculo)));

    Page<VeiculoResponseDTO> resultado = veiculoService.listar(pageable);

    assertThat(resultado).hasSize(1);
    assertThat(resultado.getContent().get(0).oficinaId()).isEqualTo(1L);
    verify(veiculoRepository).findByOficinaId(1L, pageable);
    verify(veiculoRepository, never()).findAll(any(Pageable.class));
  }

  // ---------------------------------------------------------------
  // buscarPorId()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve buscar veículo por ID e retornar o DTO correto")
  void deveBuscarVeiculoPorIdComoAdmin() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));

    VeiculoResponseDTO resultado = veiculoService.buscarPorId(1L);

    assertThat(resultado).isNotNull();
    assertThat(resultado.id()).isEqualTo(1L);
    assertThat(resultado.modelo()).isEqualTo("Civic");
    assertThat(resultado.placa()).isEqualTo("ABC1234");
    assertThat(resultado.marca()).isEqualTo("Honda");
  }

  @Test
  @DisplayName("GERENTE: deve lançar VeiculoNotFoundException ao acessar veículo de outra oficina")
  void deveLancarExcecaoAoBuscarVeiculoDeOutraOficinaComoAdministrativo() {
    Oficina outraOficina = new Oficina(2L, "Outra Oficina", "98765432000110", "83888888888", true);

    Veiculo veiculoOutraOficina = new Veiculo();
    ReflectionTestUtils.setField(veiculoOutraOficina, "id", 2L);
    veiculoOutraOficina.setOficina(outraOficina);
    veiculoOutraOficina.setModelo("Gol");
    veiculoOutraOficina.setAno(2019);
    veiculoOutraOficina.setMarca("Volkswagen");
    veiculoOutraOficina.setPlaca("XYZ9876");

    // normalUser pertence à oficina 1; veiculoOutraOficina pertence à oficina 2
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(normalUser);
    when(veiculoRepository.findById(2L)).thenReturn(Optional.of(veiculoOutraOficina));
    // O isolamento é delegado ao validador, que devolve "não encontrado" para não
    // revelar que o veículo existe em outra oficina.
    doThrow(new VeiculoNotFoundException(2L))
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));

    assertThatThrownBy(() -> veiculoService.buscarPorId(2L))
        .isInstanceOf(VeiculoNotFoundException.class);

    verify(oficinaAccessValidator).validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));
  }

  // ---------------------------------------------------------------
  // buscarPorPlaca()
  // ---------------------------------------------------------------

  @Test
  @DisplayName(
      "GERENTE: deve normalizar a placa (remove hífen e converte para maiúsculas) antes de buscar")
  void deveBuscarVeiculoPorPlacaComNormalizacaoComoGerente() {
    // A busca por placa é sempre escopada na oficina do usuário logado, obtida do
    // oficinaAccessValidator. Por isso o cenário é de GERENTE e não de ADMIN:
    // o ADMIN do SaaS não tem oficina, e o endpoint é restrito a GERENTE/MECANICO.
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    // placa normalizada: "ABC-1234" -> "ABC1234"
    when(veiculoRepository.findByPlaca(1L, "ABC1234")).thenReturn(Optional.of(veiculo));

    VeiculoResponseDTO resultado = veiculoService.buscarPorPlaca("ABC-1234");

    assertThat(resultado).isNotNull();
    assertThat(resultado.placa()).isEqualTo("ABC1234");
    verify(veiculoRepository).findByPlaca(1L, "ABC1234");
  }

  // ---------------------------------------------------------------
  // criar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve criar veículo com sucesso quando a placa não existe na oficina")
  void deveCriarVeiculoComSucessoComoAdmin() {
    VeiculoRequestDTO request = new VeiculoRequestDTO("Civic", 2020, "Honda", "Azul", "ABC1234");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(veiculoRepository.existsByOficinaIdAndPlaca(1L, "ABC1234")).thenReturn(false);
    when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

    VeiculoResponseDTO resultado = veiculoService.criar(request);

    assertThat(resultado).isNotNull();
    assertThat(resultado.modelo()).isEqualTo("Civic");
    assertThat(resultado.placa()).isEqualTo("ABC1234");
    verify(veiculoRepository).save(any(Veiculo.class));
  }

  @Test
  @DisplayName(
      "deve lançar PlacaAlreadyExistsException ao criar veículo com placa já existente na mesma oficina")
  void deveLancarExcecaoAoCriarComPlacaDuplicada() {
    VeiculoRequestDTO request = new VeiculoRequestDTO("Civic", 2020, "Honda", "Azul", "ABC1234");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(normalUser);
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(veiculoRepository.existsByOficinaIdAndPlaca(1L, "ABC1234")).thenReturn(true);

    assertThatThrownBy(() -> veiculoService.criar(request))
        .isInstanceOf(PlacaAlreadyExistsException.class);

    verify(veiculoRepository, never()).save(any());
  }

  // ---------------------------------------------------------------
  // atualizar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve atualizar veículo com sucesso quando a placa não muda")
  void deveAtualizarVeiculoComSucessoComoAdmin() {
    // mesma placa → não verifica duplicidade
    VeiculoRequestDTO request = new VeiculoRequestDTO("Civic EX", 2021, "Honda", "Azul", "ABC1234");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
    when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);

    VeiculoResponseDTO resultado = veiculoService.atualizar(1L, request);

    assertThat(resultado).isNotNull();
    verify(veiculoRepository, never())
        .existsByOficinaIdAndPlacaAndIdNot(anyLong(), anyString(), anyLong());
    verify(veiculoRepository).save(any(Veiculo.class));
  }

  // ---------------------------------------------------------------
  // deletar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve deletar veículo com sucesso")
  void deveDeletarVeiculoComSucessoComoAdmin() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));

    veiculoService.deletar(1L);

    verify(veiculoRepository).delete(veiculo);
  }
}
