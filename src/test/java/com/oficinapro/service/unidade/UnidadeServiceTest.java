package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UnidadeRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private OficinaService oficinaService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private UnidadeServiceImpl unidadeService;

    private Oficina oficina;
    private Unidade unidade;
    private Usuario adminUser;
    private Usuario normalUser;

    @BeforeEach
    void setUp() {
        oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999");

        // Unidade não tem @AllArgsConstructor, usa o construtor (Oficina, String, String, String)
        unidade = new Unidade(oficina, "Unidade Central", "Rua das Flores, 100", "83911112222");
        // id não tem setter público → usa ReflectionTestUtils
        ReflectionTestUtils.setField(unidade, "id", 1L);

        adminUser = new Usuario();
        adminUser.setRole(Role.ADMIN);

        normalUser = new Usuario();
        normalUser.setRole(Role.ADMINISTRATIVO);
        normalUser.setOficina(oficina);
    }

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve chamar findAll() e retornar todas as unidades")
    void deveListarTodasAsUnidadesComoAdmin() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(unidadeRepository.findAll()).thenReturn(List.of(unidade));

        List<UnidadeResponseDTO> resultado = unidadeService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).nome()).isEqualTo("Unidade Central");
        verify(unidadeRepository).findAll();
        verify(unidadeRepository, never()).findByOficinaId(anyLong());
    }

    @Test
    @DisplayName("ADMINISTRATIVO: deve chamar findByOficinaId e retornar apenas unidades da sua oficina")
    void deveListarUnidadesDaPropriaOficinaComoAdministrativo() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser);
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
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        UnidadeResponseDTO resultado = unidadeService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Unidade Central");
        assertThat(resultado.endereco()).isEqualTo("Rua das Flores, 100");
        assertThat(resultado.telefone()).isEqualTo("83911112222");
    }

    @Test
    @DisplayName("ADMINISTRATIVO: deve lançar UnidadeNotFoundException ao acessar unidade de outra oficina")
    void deveLancarExcecaoAoBuscarUnidadeDeOutraOficinaComoAdministrativo() {
        Oficina outraOficina = new Oficina(2L, "Outra Oficina", "98765432000110", "83888888888");
        Unidade unidadeOutraOficina = new Unidade(outraOficina, "Unidade Remota", "Av. Distante, 999", "83922223333");
        ReflectionTestUtils.setField(unidadeOutraOficina, "id", 2L);

        // normalUser pertence à oficina 1; unidadeOutraOficina pertence à oficina 2
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser);
        when(unidadeRepository.findById(2L)).thenReturn(Optional.of(unidadeOutraOficina));

        assertThatThrownBy(() -> unidadeService.buscarPorId(2L))
                .isInstanceOf(UnidadeNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // listarPorOficina()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve listar unidades de uma oficina específica com sucesso")
    void deveListarUnidadesPorOficinaComoAdmin() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(unidadeRepository.findByOficinaId(1L)).thenReturn(List.of(unidade));

        List<UnidadeResponseDTO> resultado = unidadeService.listarPorOficina(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        verify(unidadeRepository).findByOficinaId(1L);
    }

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve criar unidade com sucesso quando o endereço não existe")
    void deveCriarUnidadeComSucessoComoAdmin() {
        UnidadeRequestDTO request = new UnidadeRequestDTO("Unidade Nova", "Rua Nova, 200", "83933334444");

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(unidadeRepository.existsByEndereco("Rua Nova, 200")).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

        UnidadeResponseDTO resultado = unidadeService.criar(1L, request);

        assertThat(resultado).isNotNull();
        verify(unidadeRepository).save(any(Unidade.class));
    }

    @Test
    @DisplayName("deve lançar EnderecoAlreadyExistsException ao criar unidade com endereço duplicado")
    void deveLancarExcecaoAoCriarComEnderecoDuplicado() {
        UnidadeRequestDTO request = new UnidadeRequestDTO("Duplicada", "Rua das Flores, 100", "83944445555");

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(unidadeRepository.existsByEndereco("Rua das Flores, 100")).thenReturn(true);

        assertThatThrownBy(() -> unidadeService.criar(1L, request))
                .isInstanceOf(EnderecoAlreadyExistsException.class);

        verify(unidadeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // atualizar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve atualizar unidade com sucesso mantendo o mesmo endereço")
    void deveAtualizarUnidadeComSucesso() {
        // mesmo endereço → não verifica duplicidade
        UnidadeRequestDTO request = new UnidadeRequestDTO("Unidade Atualizada", "Rua das Flores, 100", "83955556666");

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

        UnidadeResponseDTO resultado = unidadeService.atualizar(1L, request);

        assertThat(resultado).isNotNull();
        verify(unidadeRepository, never()).existsByEndereco(anyString());
        verify(unidadeRepository).save(any(Unidade.class));
    }

    // ---------------------------------------------------------------
    // deletar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve deletar unidade com sucesso")
    void deveDeletarUnidadeComSucesso() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        unidadeService.deletar(1L);

        verify(unidadeRepository).delete(unidade);
    }
}
