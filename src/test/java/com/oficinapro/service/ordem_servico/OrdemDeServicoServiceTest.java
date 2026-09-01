package com.oficinapro.service.ordem_servico;

import com.oficinapro.dto.ordemDeServico.AtribuirClienteRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtribuirMecanicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtualizarStatusOSRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.OSCanceledException;
import com.oficinapro.exception.OSFinishedException;
import com.oficinapro.exception.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.model.Veiculo;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.unidade.UnidadeService;
import com.oficinapro.service.veiculo.VeiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrdemDeServicoServiceTest {

    @Mock
    private OrdemDeServicoRepository ordemServicoRepository;

    @Mock
    private OficinaService oficinaService;

    // Interfaces → Mockito gera o mock diretamente
    @Mock
    private UnidadeService unidadeService;

    @Mock
    private VeiculoService veiculoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private MecanicoService mecanicoService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    // Atenção: a impl tem double "Service" no nome
    @InjectMocks
    private OrdemDeServicoServiceServiceImpl ordemDeServicoService;

    private Oficina oficina;
    private Unidade unidade;
    private Veiculo veiculo;
    private Cliente cliente;
    private Mecanico mecanico;
    private OrdemDeServico os;
    private Usuario adminUser;
    private Usuario normalUser;

    @BeforeEach
    void setUp() {
        oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999");

        unidade = new Unidade(oficina, "Unidade Central", "Rua das Flores, 100", "83911112222");
        ReflectionTestUtils.setField(unidade, "id", 1L);

        veiculo = new Veiculo();
        ReflectionTestUtils.setField(veiculo, "id", 1L);
        veiculo.setOficina(oficina);
        veiculo.setModelo("Civic");
        veiculo.setAno(2020);
        veiculo.setMarca("Honda");
        veiculo.setPlaca("ABC1234");

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", 1L);
        cliente.setOficina(oficina);
        cliente.setNome("João Silva");

        mecanico = new Mecanico();
        ReflectionTestUtils.setField(mecanico, "id", 1L);
        mecanico.setOficina(oficina);
        mecanico.setNome("Carlos Mecânico");

        // OrdemDeServico.id é long (primitivo) → ReflectionTestUtils define 1L
        os = new OrdemDeServico();
        ReflectionTestUtils.setField(os, "id", 1L);
        os.setOficina(oficina);
        os.setUnidade(unidade);
        os.setVeiculo(veiculo);
        os.setStatus(StatusOrdemDeServico.ABERTA);
        os.setValorTotal(BigDecimal.ZERO);
        os.setDataAbertura(LocalDateTime.now());

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
    @DisplayName("ADMIN: deve chamar findAll() e retornar todas as OS")
    void deveListarTodasAsOSComoAdmin() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findAll()).thenReturn(List.of(os));

        List<OrdemDeServicoResponseDTO> resultado = ordemDeServicoService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(StatusOrdemDeServico.ABERTA);
        assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ordemServicoRepository).findAll();
        verify(ordemServicoRepository, never()).findByOficinaId(anyLong());
    }

    @Test
    @DisplayName("ADMINISTRATIVO: deve chamar findByOficinaId e retornar apenas OS da sua oficina")
    void deveListarOSDaPropriaOficinaComoAdministrativo() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(normalUser);
        when(ordemServicoRepository.findByOficinaId(1L)).thenReturn(List.of(os));

        List<OrdemDeServicoResponseDTO> resultado = ordemDeServicoService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).oficinaId()).isEqualTo(1L);
        verify(ordemServicoRepository).findByOficinaId(1L);
        verify(ordemServicoRepository, never()).findAll();
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve buscar OS por ID e retornar o DTO correto")
    void deveBuscarOSPorIdComoAdmin() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusOrdemDeServico.ABERTA);
        assertThat(resultado.oficinaId()).isEqualTo(1L);
        assertThat(resultado.veiculoId()).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve criar OS com sucesso sem cliente e sem mecânico opcionais")
    void deveCriarOSComSucessoComoAdmin() {
        OrdemDeServicoRequestDTO request = new OrdemDeServicoRequestDTO(
                1L, 1L, 1L, null, null, "Troca de óleo"
        );

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
        when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
        when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
        when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.criar(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusOrdemDeServico.ABERTA);
        assertThat(resultado.valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ordemServicoRepository).save(any(OrdemDeServico.class));
        // clienteId e mecanicoId são null → serviços opcionais não devem ser chamados
        verify(clienteService, never()).buscarPorEntidadeId(anyLong());
        verify(mecanicoService, never()).buscarPorEntidadeId(anyLong());
    }

    // ---------------------------------------------------------------
    // atualizarStatus()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve atualizar status de ABERTA para EM_EXECUCAO com sucesso")
    void deveAtualizarStatusDeAbertaParaEmExecucao() {
        AtualizarStatusOSRequestDTO dto = new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.EM_EXECUCAO);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atualizarStatus(1L, dto);

        assertThat(resultado).isNotNull();
        // o status foi alterado na entidade antes do save; o mock devolve a mesma referência
        assertThat(resultado.status()).isEqualTo(StatusOrdemDeServico.EM_EXECUCAO);
        verify(ordemServicoRepository).save(any(OrdemDeServico.class));
    }

    @Test
    @DisplayName("deve lançar OSCanceledException ao tentar atualizar status de OS cancelada")
    void deveLancarExcecaoAoAtualizarStatusDeOSCancelada() {
        os.setStatus(StatusOrdemDeServico.CANCELADA);
        AtualizarStatusOSRequestDTO dto = new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.ABERTA);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemDeServicoService.atualizarStatus(1L, dto))
                .isInstanceOf(OSCanceledException.class);

        verify(ordemServicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar OSFinishedException ao tentar regredir OS entregue para ABERTA")
    void deveLancarExcecaoAoTentarVoltarOSEntregueParaAberta() {
        os.setStatus(StatusOrdemDeServico.ENTREGUE);
        AtualizarStatusOSRequestDTO dto = new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.ABERTA);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemDeServicoService.atualizarStatus(1L, dto))
                .isInstanceOf(OSFinishedException.class);

        verify(ordemServicoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // atribuirMecanico()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve atribuir mecânico à OS com sucesso")
    void deveAtribuirMecanicoAOSComSucesso() {
        AtribuirMecanicoRequestDTO dto = new AtribuirMecanicoRequestDTO(1L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
        when(mecanicoService.buscarPorEntidadeId(1L)).thenReturn(mecanico);
        when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atribuirMecanico(1L, dto);

        assertThat(resultado).isNotNull();
        verify(mecanicoService).buscarPorEntidadeId(1L);
        verify(ordemServicoRepository).save(any(OrdemDeServico.class));
    }

    // ---------------------------------------------------------------
    // atribuirCliente()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve atribuir cliente à OS com sucesso")
    void deveAtribuirClienteAOSComSucesso() {
        AtribuirClienteRequestDTO dto = new AtribuirClienteRequestDTO(1L);

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
        when(clienteService.buscarPorEntidadeId(1L)).thenReturn(cliente);
        when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atribuirCliente(1L, dto);

        assertThat(resultado).isNotNull();
        verify(clienteService).buscarPorEntidadeId(1L);
        verify(ordemServicoRepository).save(any(OrdemDeServico.class));
    }

    // ---------------------------------------------------------------
    // atualizar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve atualizar OS com sucesso mantendo a mesma oficina")
    void deveAtualizarOSComSucessoMantendoAMesmaOficina() {
        // mesma oficinaId (1L) → sem troca de oficina
        OrdemDeServicoRequestDTO request = new OrdemDeServicoRequestDTO(
                1L, 1L, 1L, null, null, "Revisão completa"
        );

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));
        when(unidadeService.buscarPorEntidadeId(1L)).thenReturn(unidade);
        when(veiculoService.buscarPorEntidadeId(1L)).thenReturn(veiculo);
        when(ordemServicoRepository.save(any(OrdemDeServico.class))).thenReturn(os);

        OrdemDeServicoResponseDTO resultado = ordemDeServicoService.atualizar(1L, request);

        assertThat(resultado).isNotNull();
        verify(ordemServicoRepository).save(any(OrdemDeServico.class));
    }

    @Test
    @DisplayName("deve lançar OSIsNotPossibleSwapWorkshopException ao tentar trocar a oficina da OS")
    void deveLancarExcecaoAoTentarTrocarOficinaDeOS() {
        // OS pertence à oficina 1, request tenta mover para oficina 2
        OrdemDeServicoRequestDTO request = new OrdemDeServicoRequestDTO(
                2L, 1L, 1L, null, null, null
        );

        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemDeServicoService.atualizar(1L, request))
                .isInstanceOf(OSIsNotPossibleSwapWorkshopException.class);

        verify(ordemServicoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // deletar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN: deve deletar OS com sucesso")
    void deveDeletarOSComSucessoComoAdmin() {
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(adminUser);
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(os));

        ordemDeServicoService.deletar(1L);

        verify(ordemServicoRepository).delete(os);
    }
}
