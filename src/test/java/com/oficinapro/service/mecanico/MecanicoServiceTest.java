package com.oficinapro.service.mecanico;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.MecanicoRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MecanicoServiceTest {

    @Mock
    private MecanicoRepository mecanicoRepository;

    @Mock
    private OficinaServiceImpl oficinaService;

    @Mock
    private PessoaService pessoaService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private MecanicoServiceImpl service;

    // ─── entidades de apoio ───
    private Oficina oficina;
    private Usuario adminUser;
    private Usuario normalUser;
    private Mecanico mecanico;
    private MecanicoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        // Oficina compartilhada (id = 1)
        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Test");
        oficina.setCnpj("12345678000195");
        oficina.setTelefone("83999999999");

        // Usuário ADMIN – sem restrição de oficina
        adminUser = new Usuario();
        adminUser.setRole(Role.ADMIN);

        // Usuário ADMINISTRATIVO – vinculado à oficina 1
        normalUser = new Usuario();
        normalUser.setRole(Role.ADMINISTRATIVO);
        normalUser.setOficina(oficina);

        // Mecânico pertencente à oficina 1
        mecanico = new Mecanico();
        mecanico.setId(1L);
        mecanico.setNome("Carlos Mecânico");
        mecanico.setTelefone("83988887777");
        mecanico.setDocumento("12345678901");
        mecanico.setOficina(oficina);
        mecanico.setSalario(BigDecimal.valueOf(3500.00));
        mecanico.setObs("Especialista em motores");

        requestDTO = new MecanicoRequestDTO(
                "Carlos Mecânico", "83988887777", "12345678901",
                1L, BigDecimal.valueOf(3500.00), "Especialista em motores");
    }

    // ─────────────────────────── listar ───────────────────────────

    @Test
    @DisplayName("listar() como ADMIN deve retornar todos os mecânicos paginados")
    void listar_comoAdmin_retornaTodosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Mecanico> page = new PageImpl<>(List.of(mecanico));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findAll(pageable)).thenReturn(page);

        Page<MecanicoResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);
        assertThat(resultado.getContent().get(0).nome()).isEqualTo("Carlos Mecânico");
        assertThat(resultado.getContent().get(0).salario()).isEqualByComparingTo(BigDecimal.valueOf(3500.00));
        assertThat(resultado.getContent().get(0).obs()).isEqualTo("Especialista em motores");

        verify(mecanicoRepository, times(1)).findAll(pageable);
        verify(mecanicoRepository, never()).findByOficinaId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("listar() como ADMINISTRATIVO deve retornar apenas mecânicos da sua oficina")
    void listar_comoAdministrativo_retornaMecanicosDaSuaOficina() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Mecanico> page = new PageImpl<>(List.of(mecanico));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser);
        when(mecanicoRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<MecanicoResponseDTO> resultado = service.listar(pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1L);

        verify(mecanicoRepository, times(1)).findByOficinaId(1L, pageable);
        verify(mecanicoRepository, never()).findAll(any(Pageable.class));
    }

    // ─────────────────────────── buscarPorId ───────────────────────────

    @Test
    @DisplayName("buscarPorId() como ADMIN deve encontrar mecânico de qualquer oficina")
    void buscarPorId_comoAdmin_encontrado_retornaDTO() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findById(1L)).thenReturn(Optional.of(mecanico));

        MecanicoResponseDTO resultado = service.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Carlos Mecânico");
        assertThat(resultado.documento()).isEqualTo("12345678901");
        assertThat(resultado.oficinaId()).isEqualTo(1L);
        assertThat(resultado.salario()).isEqualByComparingTo(BigDecimal.valueOf(3500.00));
    }

    @Test
    @DisplayName("buscarPorId() deve lançar MecanicoNotFoundException quando ID não existe")
    void buscarPorId_naoEncontrado_lancaMecanicoNotFoundException() {

        when(mecanicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(MecanicoNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId() deve lançar MecanicoNotFoundException ao acessar mecânico de outra oficina (oculta existência)")
    void buscarPorId_mecanicoDeOutraOficina_lancaMecanicoNotFoundException() {
        Oficina outraOficina = new Oficina();
        outraOficina.setId(2L);
        outraOficina.setNome("Outra Oficina");
        outraOficina.setCnpj("11222333000181");
        outraOficina.setTelefone("83977776666");

        Mecanico mecanicoAlheio = new Mecanico();
        mecanicoAlheio.setId(5L);
        mecanicoAlheio.setNome("Mecânico Alheio");
        mecanicoAlheio.setDocumento("98765432100");
        mecanicoAlheio.setOficina(outraOficina);
        mecanicoAlheio.setSalario(BigDecimal.valueOf(2000));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser); // oficina 1
        when(mecanicoRepository.findById(5L)).thenReturn(Optional.of(mecanicoAlheio));

        assertThatThrownBy(() -> service.buscarPorId(5L))
                .isInstanceOf(MecanicoNotFoundException.class);
    }

    // ─────────────────────────── criar ───────────────────────────

    @Test
    @DisplayName("criar() como ADMIN deve criar mecânico em qualquer oficina com sucesso")
    void criar_comoAdmin_sucesso() {
        Mecanico salvo = new Mecanico();
        salvo.setId(1L);
        salvo.setNome("Carlos Mecânico");
        salvo.setTelefone("83988887777");
        salvo.setDocumento("12345678901");
        salvo.setOficina(oficina);
        salvo.setSalario(BigDecimal.valueOf(3500.00));
        salvo.setObs("Especialista em motores");

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(false);
        when(mecanicoRepository.save(any(Mecanico.class))).thenReturn(salvo);

        MecanicoResponseDTO resultado = service.criar(requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Carlos Mecânico");
        assertThat(resultado.documento()).isEqualTo("12345678901");
        assertThat(resultado.salario()).isEqualByComparingTo(BigDecimal.valueOf(3500.00));
        assertThat(resultado.obs()).isEqualTo("Especialista em motores");
        assertThat(resultado.oficinaId()).isEqualTo(1L);

        verify(mecanicoRepository, times(1)).save(any(Mecanico.class));
    }

    @Test
    @DisplayName("criar() deve lançar MecanicoAlreadyExistsException quando documento já está cadastrado na oficina")
    void criar_documentoDuplicado_lancaMecanicoAlreadyExistsException() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(pessoaService.existsByOficinaIdAndDocumento(1L, "12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(requestDTO))
                .isInstanceOf(MecanicoAlreadyExistsException.class);

        verify(mecanicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar() como ADMINISTRATIVO tentando criar em outra oficina deve lançar AccessDeniedException")
    void criar_comoAdministrativo_outraOficina_lancaAccessDeniedException() {
        // Request aponta para oficina 2, mas normalUser pertence à oficina 1
        MecanicoRequestDTO requestOutraOficina = new MecanicoRequestDTO(
                "Novo Mecânico", "83999999999", "11122233344",
                2L, BigDecimal.valueOf(2500), null);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser); // oficina 1

        assertThatThrownBy(() -> service.criar(requestOutraOficina))
                .isInstanceOf(AccessDeniedException.class);

        verify(mecanicoRepository, never()).save(any());
    }

    // ─────────────────────────── atualizar ───────────────────────────

    @Test
    @DisplayName("atualizar() como ADMIN deve atualizar mecânico com sucesso")
    void atualizar_comoAdmin_sucesso() {
        MecanicoRequestDTO requestAtualizar = new MecanicoRequestDTO(
                "Carlos Atualizado", "83977776666", "12345678901",
                1L, BigDecimal.valueOf(4000.00), "Atualizado");

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findById(1L)).thenReturn(Optional.of(mecanico));
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "12345678901", 1L)).thenReturn(false);
        when(mecanicoRepository.save(any(Mecanico.class))).thenReturn(mecanico);

        MecanicoResponseDTO resultado = service.atualizar(1L, requestAtualizar);

        assertThat(resultado).isNotNull();
        // applyUpdate modifica o objeto mecanico em lugar; os dados devem refletir o request
        assertThat(resultado.nome()).isEqualTo("Carlos Atualizado");
        assertThat(resultado.salario()).isEqualByComparingTo(BigDecimal.valueOf(4000.00));
        verify(mecanicoRepository, times(1)).save(any(Mecanico.class));
    }

    @Test
    @DisplayName("atualizar() deve lançar MecanicoNotFoundException quando ID não existe")
    void atualizar_idNaoEncontrado_lancaMecanicoNotFoundException() {
        MecanicoRequestDTO requestAtualizar = new MecanicoRequestDTO(
                "Carlos Atualizado", "83977776666", "12345678901",
                1L, BigDecimal.valueOf(4000), null);

        when(mecanicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, requestAtualizar))
                .isInstanceOf(MecanicoNotFoundException.class);

        verify(mecanicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar() deve lançar MecanicoAlreadyExistsException quando documento já pertence a outro mecânico da mesma oficina")
    void atualizar_documentoDuplicadoOutroMecanico_lancaMecanicoAlreadyExistsException() {
        MecanicoRequestDTO requestAtualizar = new MecanicoRequestDTO(
                "Carlos Atualizado", "83977776666", "99988877766",
                1L, BigDecimal.valueOf(4000), null);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findById(1L)).thenReturn(Optional.of(mecanico));
        when(pessoaService.existsByOficinaIdAndDocumentoExcluindoId(1L, "99988877766", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, requestAtualizar))
                .isInstanceOf(MecanicoAlreadyExistsException.class);

        verify(mecanicoRepository, never()).save(any());
    }

    // ─────────────────────────── deletar ───────────────────────────

    @Test
    @DisplayName("deletar() como ADMIN deve remover mecânico com sucesso")
    void deletar_comoAdmin_sucesso() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findById(1L)).thenReturn(Optional.of(mecanico));

        service.deletar(1L);

        verify(mecanicoRepository, times(1)).delete(mecanico);
    }

    @Test
    @DisplayName("deletar() deve lançar MecanicoNotFoundException quando ID não existe")
    void deletar_idNaoEncontrado_lancaMecanicoNotFoundException() {

        when(mecanicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(MecanicoNotFoundException.class);

        verify(mecanicoRepository, never()).delete(any());
    }

    // ─────────────────────────── listarPorOficinaId ───────────────────────────

    @Test
    @DisplayName("listarPorOficinaId() como ADMIN deve retornar mecânicos de qualquer oficina")
    void listarPorOficinaId_comoAdmin_sucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Mecanico> page = new PageImpl<>(List.of(mecanico));

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(mecanicoRepository.findByOficinaId(1L, pageable)).thenReturn(page);

        Page<MecanicoResponseDTO> resultado = service.listarPorOficinaId(1L, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).oficinaId()).isEqualTo(1L);
    }
}
