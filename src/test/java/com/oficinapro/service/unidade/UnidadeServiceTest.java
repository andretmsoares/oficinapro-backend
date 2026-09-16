package com.oficinapro.service.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UnidadeRepository;
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
class UnidadeServiceTest {

  @Mock private UnidadeRepository unidadeRepository;

  @Mock private OficinaService oficinaService;

  // Adicionado no refactor: o isolamento por oficina saiu dos services e passou
  // a viver em OficinaAccessValidator.
  @Mock private com.oficinapro.security.OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private UnidadeServiceImpl unidadeService;

  private Oficina oficina;
  private Unidade unidade;
  private Usuario adminUser;
  private Usuario normalUser;

  @BeforeEach
  void setUp() {
    oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999", true);

    // Unidade não tem @AllArgsConstructor, usa o construtor (Oficina, String, String, String)
    unidade = new Unidade(oficina, "Unidade Central", "Rua das Flores, 100", "83911112222");
    // id não tem setter público → usa ReflectionTestUtils
    ReflectionTestUtils.setField(unidade, "id", 1L);

    adminUser = new Usuario();
    adminUser.setRole(Role.ADMIN);

    normalUser = new Usuario();
    normalUser.setRole(Role.GERENTE);
    normalUser.setOficina(oficina);
  }

  // ---------------------------------------------------------------
  // listar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve chamar findAll() e retornar todas as unidades")
  void deveListarTodasAsUnidadesComoAdmin() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(unidadeRepository.findAll()).thenReturn(List.of(unidade));

    List<UnidadeResponseDTO> resultado = unidadeService.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).id()).isEqualTo(1L);
    assertThat(resultado.get(0).nome()).isEqualTo("Unidade Central");
    verify(unidadeRepository).findAll();
    verify(unidadeRepository, never()).findByOficinaId(anyLong());
  }

  @Test
  @DisplayName("GERENTE: deve chamar findByOficinaId e retornar apenas unidades da sua oficina")
  void deveListarUnidadesDaPropriaOficinaComoAdministrativo() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(normalUser);
    // Quem resolve a oficina do usuário logado agora é o OficinaAccessValidator.
    when(oficinaAccessValidator.getOficinaIdUsuarioLogado()).thenReturn(1L);
    when(unidadeRepository.findByOficinaId(1L)).thenReturn(List.of(unidade));

    List<UnidadeResponseDTO> resultado = unidadeService.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).oficinaId()).isEqualTo(1L);
    verify(unidadeRepository).findByOficinaId(1L);
    verify(unidadeRepository, never()).findAll();
  }

  // ---------------------------------------------------------------
  // buscarPorId()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve buscar unidade por ID e retornar o DTO correto")
  void deveBuscarUnidadePorIdComoAdmin() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

    UnidadeResponseDTO resultado = unidadeService.buscarPorId(1L);

    assertThat(resultado).isNotNull();
    assertThat(resultado.id()).isEqualTo(1L);
    assertThat(resultado.nome()).isEqualTo("Unidade Central");
    assertThat(resultado.endereco()).isEqualTo("Rua das Flores, 100");
    assertThat(resultado.telefone()).isEqualTo("83911112222");
  }

  @Test
  @DisplayName("GERENTE: deve lançar UnidadeNotFoundException ao acessar unidade de outra oficina")
  void deveLancarExcecaoAoBuscarUnidadeDeOutraOficinaComoAdministrativo() {
    Oficina outraOficina = new Oficina(2L, "Outra Oficina", "98765432000110", "83888888888", true);
    Unidade unidadeOutraOficina =
        new Unidade(outraOficina, "Unidade Remota", "Av. Distante, 999", "83922223333");
    ReflectionTestUtils.setField(unidadeOutraOficina, "id", 2L);

    // normalUser pertence à oficina 1; unidadeOutraOficina pertence à oficina 2
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(normalUser);
    when(unidadeRepository.findById(2L)).thenReturn(Optional.of(unidadeOutraOficina));
    // O isolamento é delegado ao validador, que devolve "não encontrado" para não
    // revelar que a unidade existe em outra oficina.
    doThrow(new UnidadeNotFoundException(2L))
        .when(oficinaAccessValidator)
        .validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));

    assertThatThrownBy(() -> unidadeService.buscarPorId(2L))
        .isInstanceOf(UnidadeNotFoundException.class);

    verify(oficinaAccessValidator).validarAcessoAoRegistro(eq(2L), any(RuntimeException.class));
  }

  // ---------------------------------------------------------------
  // criar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve criar unidade com sucesso quando o endereço não existe")
  void deveCriarUnidadeComSucessoComoAdmin() {
    UnidadeRequestDTO request =
        new UnidadeRequestDTO("Unidade Nova", "Rua Nova, 200", "83933334444");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(unidadeRepository.existsByOficinaIdAndEndereco(1L, "Rua Nova, 200")).thenReturn(false);
    when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

    UnidadeResponseDTO resultado = unidadeService.criar(1L, request);

    assertThat(resultado).isNotNull();
    verify(unidadeRepository).save(any(Unidade.class));
  }

  @Test
  @DisplayName(
      "deve lançar EnderecoAlreadyExistsException ao criar unidade com endereço já usado na MESMA oficina")
  void deveLancarExcecaoAoCriarComEnderecoDuplicadoNaMesmaOficina() {
    UnidadeRequestDTO request =
        new UnidadeRequestDTO("Duplicada", "Rua das Flores, 100", "83944445555");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(unidadeRepository.existsByOficinaIdAndEndereco(1L, "Rua das Flores, 100"))
        .thenReturn(true);

    assertThatThrownBy(() -> unidadeService.criar(1L, request))
        .isInstanceOf(EnderecoAlreadyExistsException.class);

    verify(unidadeRepository, never()).save(any());
  }

  @Test
  @DisplayName("deve permitir o mesmo endereço em oficinas diferentes: a unicidade é por oficina")
  void devePermitirMesmoEnderecoEmOficinasDiferentes() {
    Long outraOficinaId = 2L;
    Oficina outraOficina =
        new Oficina(outraOficinaId, "Outra Oficina", "98765432000155", "8388887777", true);
    UnidadeRequestDTO request =
        new UnidadeRequestDTO("Filial", "Rua das Flores, 100", "83977778888");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(oficinaService.buscarPorEntidadeId(outraOficinaId)).thenReturn(outraOficina);
    // O endereço já existe na oficina 1, mas a consulta é escopada pela oficina 2.
    when(unidadeRepository.existsByOficinaIdAndEndereco(outraOficinaId, "Rua das Flores, 100"))
        .thenReturn(false);
    when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

    UnidadeResponseDTO resultado = unidadeService.criar(outraOficinaId, request);

    assertThat(resultado)
        .as(
            "Duas oficinas podem operar no mesmo endereço. A verificação global"
                + " anterior gerava 409 e revelava a existência de unidades de"
                + " outro tenant.")
        .isNotNull();
    verify(unidadeRepository).save(any(Unidade.class));
  }

  // ---------------------------------------------------------------
  // atualizar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve atualizar unidade mantendo o mesmo endereço sem acusar duplicidade")
  void deveAtualizarUnidadeComSucesso() {
    UnidadeRequestDTO request =
        new UnidadeRequestDTO("Unidade Atualizada", "Rua das Flores, 100", "83955556666");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
    // A consulta exclui o próprio registro (IdNot), então manter o endereço não conflita.
    when(unidadeRepository.existsByOficinaIdAndEnderecoAndIdNot(1L, "Rua das Flores, 100", 1L))
        .thenReturn(false);
    when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

    UnidadeResponseDTO resultado = unidadeService.atualizar(1L, request);

    assertThat(resultado).isNotNull();
    verify(unidadeRepository).save(any(Unidade.class));
  }

  @Test
  @DisplayName(
      "deve lançar EnderecoAlreadyExistsException ao mover a unidade para um endereço já usado na oficina")
  void deveLancarExcecaoAoAtualizarParaEnderecoJaUsadoNaOficina() {
    UnidadeRequestDTO request =
        new UnidadeRequestDTO("Unidade Atualizada", "Rua Ocupada, 500", "83955556666");

    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
    when(unidadeRepository.existsByOficinaIdAndEnderecoAndIdNot(1L, "Rua Ocupada, 500", 1L))
        .thenReturn(true);

    assertThatThrownBy(() -> unidadeService.atualizar(1L, request))
        .isInstanceOf(EnderecoAlreadyExistsException.class);

    verify(unidadeRepository, never()).save(any());
  }

  // ---------------------------------------------------------------
  // deletar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("ADMIN: deve deletar unidade com sucesso")
  void deveDeletarUnidadeComSucesso() {
    when(oficinaAccessValidator.getUsuarioAutenticado()).thenReturn(adminUser);
    when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

    unidadeService.deletar(1L);

    verify(unidadeRepository).delete(unidade);
  }
}
