package com.oficinapro.service.item_os_peca;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.model.ItemOsPeca;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.repository.ItemOsPecaRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ItemOsPecaServiceTest {

    @Mock
    private ItemOsPecaRepository itemOsPecaRepository;


    @Mock
    private OrdemDeServicoService ordemDeServicoService;

    @InjectMocks
    private ItemOsPecaServiceImpl itemOsPecaService;

    private Oficina oficina;
    private OrdemDeServico os;
    private ItemOsPeca item;

    @BeforeEach
    void setUp() {
        oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999");

        // OrdemDeServico.id é long (primitivo) → ReflectionTestUtils
        os = new OrdemDeServico();
        ReflectionTestUtils.setField(os, "id", 1L);
        os.setStatus(StatusOrdemDeServico.ABERTA);
        os.setOficina(oficina);
        os.setValorTotal(BigDecimal.ZERO);

        // ItemOsPeca tem @AllArgsConstructor; id usa @GeneratedValue → ReflectionTestUtils
        item = new ItemOsPeca();
        ReflectionTestUtils.setField(item, "id", 1L);
        item.setOrdemDeServico(os);
        item.setNome("Pastilha de Freio");
        item.setQuantidade(new BigDecimal("2"));
        item.setValorUnitario(new BigDecimal("50.00"));
        item.setValorTotal(new BigDecimal("100.00"));
    }

    // ---------------------------------------------------------------
    // listarPorOrdemServico()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve listar itens de uma OS com sucesso, validando acesso à OS antes")
    void deveListarItensPorOrdemServicoComSucesso() {
        // buscarPorEntidadeId valida o acesso; o retorno não é usado no método
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(itemOsPecaRepository.findByOrdemDeServicoId(1L)).thenReturn(List.of(item));

        List<ItemOsPecaResponseDTO> resultado = itemOsPecaService.listarPorOrdemServico(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nome()).isEqualTo("Pastilha de Freio");
        assertThat(resultado.get(0).osId()).isEqualTo(1L);
        verify(ordemDeServicoService).buscarPorEntidadeId(1L);
        verify(itemOsPecaRepository).findByOrdemDeServicoId(1L);
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve buscar item por ID com sucesso, validando acesso à OS")
    void deveBuscarItemPorIdComSucesso() {
        when(itemOsPecaRepository.findById(1L)).thenReturn(Optional.of(item));
        // item.getOrdemDeServico().getId() retorna long (1L) → autoboxed para Long
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        ItemOsPecaResponseDTO resultado = itemOsPecaService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Pastilha de Freio");
        assertThat(resultado.valorTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("deve lançar ItemOsPecaNotFoundException ao buscar item inexistente")
    void deveLancarExcecaoAoBuscarItemInexistente() {
        when(itemOsPecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemOsPecaService.buscarPorId(99L))
                .isInstanceOf(ItemOsPecaNotFoundException.class);

        verify(ordemDeServicoService, never()).buscarPorEntidadeId(anyLong());
    }

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deve criar item com sucesso: valorTotal = qtd × valorUnitario e OS é recalculada")
    void deveCriarItemComSucessoERecalcularValorTotalDaOS() {
        ItemOsPecaRequestDTO request = new ItemOsPecaRequestDTO(
                1L, "Pastilha de Freio", new BigDecimal("2"), new BigDecimal("50.00")
        );

        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(itemOsPecaRepository.save(any(ItemOsPeca.class))).thenReturn(item);
        when(itemOsPecaRepository.findByOrdemDeServicoId(1L)).thenReturn(List.of(item));

        ItemOsPecaResponseDTO resultado = itemOsPecaService.criar(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nome()).isEqualTo("Pastilha de Freio");
        assertThat(resultado.valorTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(itemOsPecaRepository).save(any(ItemOsPeca.class));
        verify(ordemDeServicoService).recalcularValorTotal(eq(1L), eq(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("deve lançar OSCanceledException ao criar item em OS cancelada")
    void deveLancarExcecaoAoCriarItemEmOSCancelada() {
        os.setStatus(StatusOrdemDeServico.CANCELADA);
        ItemOsPecaRequestDTO request = new ItemOsPecaRequestDTO(
                1L, "Amortecedor", new BigDecimal("1"), new BigDecimal("300.00")
        );

        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        assertThatThrownBy(() -> itemOsPecaService.criar(request))
                .isInstanceOf(OSCanceledException.class);

        verify(itemOsPecaRepository, never()).save(any());
        verify(ordemDeServicoService, never()).recalcularValorTotal(any(), any());
    }

    @Test
    @DisplayName("deve lançar OSFinishedException ao criar item em OS entregue")
    void deveLancarExcecaoAoCriarItemEmOSEntregue() {
        os.setStatus(StatusOrdemDeServico.ENTREGUE);
        ItemOsPecaRequestDTO request = new ItemOsPecaRequestDTO(
                1L, "Filtro de Ar", new BigDecimal("1"), new BigDecimal("45.00")
        );

        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        assertThatThrownBy(() -> itemOsPecaService.criar(request))
                .isInstanceOf(OSFinishedException.class);

        verify(itemOsPecaRepository, never()).save(any());
        verify(ordemDeServicoService, never()).recalcularValorTotal(any(), any());
    }

    @Test
    @DisplayName("deve atualizar item com sucesso e recalcular valor total da OS")
    void deveAtualizarItemComSucessoERecalcularValorTotalDaOS() {
        ItemOsPecaUpdateRequestDTO request = new ItemOsPecaUpdateRequestDTO(
                "Óleo Motor 5W30", new BigDecimal("3"), new BigDecimal("30.00")
        );

        ItemOsPeca itemAtualizado = new ItemOsPeca();
        ReflectionTestUtils.setField(itemAtualizado, "id", 1L);
        itemAtualizado.setOrdemDeServico(os);
        itemAtualizado.setNome("Óleo Motor 5W30");
        itemAtualizado.setQuantidade(new BigDecimal("3"));
        itemAtualizado.setValorUnitario(new BigDecimal("30.00"));
        itemAtualizado.setValorTotal(new BigDecimal("90.00"));

        when(itemOsPecaRepository.findById(1L)).thenReturn(Optional.of(item));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(itemOsPecaRepository.save(any(ItemOsPeca.class))).thenReturn(itemAtualizado);
        when(itemOsPecaRepository.findByOrdemDeServicoId(1L)).thenReturn(List.of(itemAtualizado));

        ItemOsPecaResponseDTO resultado = itemOsPecaService.atualizar(1L, request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nome()).isEqualTo("Óleo Motor 5W30");
        assertThat(resultado.valorTotal()).isEqualByComparingTo(new BigDecimal("90.00"));
        verify(itemOsPecaRepository).save(any(ItemOsPeca.class));
        verify(ordemDeServicoService).recalcularValorTotal(eq(1L), eq(new BigDecimal("90.00")));
    }

    @Test
    @DisplayName("deve deletar item e recalcular valorTotal da OS como zero (lista vazia após deleção)")
    void deveDeletarItemERecalcularValorTotalDaOSComoZero() {
        when(itemOsPecaRepository.findById(1L)).thenReturn(Optional.of(item));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(itemOsPecaRepository.findByOrdemDeServicoId(1L)).thenReturn(List.of());

        itemOsPecaService.deletar(1L);

        verify(itemOsPecaRepository).delete(item);
        verify(ordemDeServicoService).recalcularValorTotal(eq(1L), eq(BigDecimal.ZERO));
    }
}