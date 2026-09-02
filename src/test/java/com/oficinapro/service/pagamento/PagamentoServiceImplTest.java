package com.oficinapro.service.pagamento;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundForThisOsException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.exception.pagamento.PagamentoValorInvalidoException;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.repository.PagamentoRepository;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceImplTest {

    @Mock
    private PagamentoRepository repository;

    @Mock
    private OrdemDeServicoService ordemDeServicoService;

    @InjectMocks
    private PagamentoServiceImpl pagamentoService;

    private OrdemDeServico os;
    private Pagamento pagamento;
    private PagamentoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        os = new OrdemDeServico();
        os.setId(1L);
        os.setValorTotal(new BigDecimal("500.00"));
        os.setValorComDesconto(new BigDecimal("500.00"));

        pagamento = new Pagamento();
        pagamento.setId(10L);
        pagamento.setOrdemDeServico(os);
        pagamento.setValorPago(BigDecimal.ZERO);
        pagamento.setStatus(StatusPagamento.PAGAMENTO_PENDENTE);

        // Assumindo a ordem do seu record PagamentoRequestDTO (osId, valorPago, desconto, obs)
        requestDTO = new PagamentoRequestDTO(1L, BigDecimal.ZERO, "Observação de teste");
    }

    // ---------------------------------------------------------
    // criar e atualizar
    // ---------------------------------------------------------

    @Test
    @DisplayName("criar() deve salvar pagamento e retornar DTO")
    void criar_comDadosValidos_salvaRetornaDTO() {
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        PagamentoResponseDTO response = pagamentoService.criar(requestDTO);

        assertThat(response.osId()).isEqualTo(1L);
        assertThat(response.obs()).isEqualTo("Observação de teste");
        verify(repository).save(any(Pagamento.class));
    }

    @Test
    @DisplayName("atualizar() deve alterar os dados e retornar DTO atualizado")
    void atualizar_comDadosValidos_salvaERetornaDTO() {
        PagamentoRequestDTO requestUpdate = new PagamentoRequestDTO(1L, new BigDecimal("50.00"), "Nova obs");

        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        PagamentoResponseDTO response = pagamentoService.atualizar(10L, requestUpdate);

        assertThat(response.obs()).isEqualTo("Nova obs");
        assertThat(response.valorPago()).isEqualTo(new BigDecimal("50.00"));
        verify(repository).save(any(Pagamento.class));
    }

    // ---------------------------------------------------------
    // buscarEntidadePorId (valida escopo via OS)
    // ---------------------------------------------------------

    @Test
    @DisplayName("buscarEntidadePorId() lança exceção se não encontrar")
    void buscarEntidadePorId_naoEncontrado_lancaExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.buscarEntidadePorId(99L))
                .isInstanceOf(PagamentoNotFoundException.class);
    }

    @Test
    @DisplayName("buscarEntidadePorId() valida o escopo acionando buscarPorEntidadeId da OS")
    void buscarEntidadePorId_validaEscopoDeOficinaViaOS() {
        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        Pagamento resultado = pagamentoService.buscarEntidadePorId(10L);

        assertThat(resultado).isEqualTo(pagamento);
        verify(ordemDeServicoService).buscarPorEntidadeId(1L);
    }

    // ---------------------------------------------------------
    // buscarPorOsId / buscarPorOsIdSeExistir
    // ---------------------------------------------------------

    @Test
    @DisplayName("buscarPorOsId() sem pagamento lança exceção")
    void buscarPorOsId_semPagamento_lancaExcecao() {
        when(repository.findByOrdemDeServicoId(1L)).thenReturn(null);

        assertThatThrownBy(() -> pagamentoService.buscarPorOsId(1L))
                .isInstanceOf(PagamentoNotFoundForThisOsException.class);
    }

    @Test
    @DisplayName("buscarPorOsId() com pagamento valida acesso via OS e retorna DTO")
    void buscarPorOsId_comPagamento_retornaPagamento() {
        when(repository.findByOrdemDeServicoId(1L)).thenReturn(pagamento);
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        PagamentoResponseDTO response = pagamentoService.buscarPorOsId(1L);

        assertThat(response.osId()).isEqualTo(1L);
        verify(ordemDeServicoService).buscarPorEntidadeId(1L);
    }

    @Test
    void buscarPorOsIdSeExistir_semPagamento_retornaOptionalVazio() {
        when(repository.findByOrdemDeServicoId(1L)).thenReturn(null);

        Optional<PagamentoResponseDTO> resultado = pagamentoService.buscarPorOsIdSeExistir(1L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarPorOsIdSeExistir_comPagamento_retornaOptionalPreenchido() {
        when(repository.findByOrdemDeServicoId(1L)).thenReturn(pagamento);

        Optional<PagamentoResponseDTO> resultado = pagamentoService.buscarPorOsIdSeExistir(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().osId()).isEqualTo(1L);
    }

    // ---------------------------------------------------------
    // atualizarValorPago (delta positivo)
    // ---------------------------------------------------------

    @Test
    @DisplayName("atualizarValorPago() com valor menor que o total marca como PAGO_PARCIALMENTE")
    void atualizarValorPago_valorParcial_marcaComoPagoParcialmente() {
        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        PagamentoResponseDTO response = pagamentoService.atualizarValorPago(10L, new BigDecimal("100.00"));

        assertThat(response.status()).isEqualTo(StatusPagamento.PAGO_PARCIALMENTE);
        assertThat(response.valorPago()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("atualizarValorPago() atingindo valor total marca como PAGA")
    void atualizarValorPago_valorExato_marcaComoPaga() {
        pagamento.setValorPago(new BigDecimal("400.00"));

        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        PagamentoResponseDTO response = pagamentoService.atualizarValorPago(10L, new BigDecimal("100.00"));

        assertThat(response.status()).isEqualTo(StatusPagamento.PAGA);
        assertThat(response.valorPago()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("atualizarValorPago() excedendo valor total lança PagamentoValorExcedidoException")
    void atualizarValorPago_valorExcedeLimite_lancaExcecao() {
        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        assertThatThrownBy(() -> pagamentoService.atualizarValorPago(10L, new BigDecimal("600.00")))
                .isInstanceOf(PagamentoValorExcedidoException.class);

        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------
    // estornarValorPago (delta negativo)
    // ---------------------------------------------------------

    @Test
    @DisplayName("estornarValorPago() zerando o pagamento volta status para PENDENTE")
    void estornarValorPago_reduzValorEVoltaParaPendente() {
        pagamento.setValorPago(new BigDecimal("100.00"));
        pagamento.setStatus(StatusPagamento.PAGO_PARCIALMENTE);

        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        PagamentoResponseDTO response = pagamentoService.estornarValorPago(10L, new BigDecimal("100.00"));

        assertThat(response.valorPago()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.status()).isEqualTo(StatusPagamento.PAGAMENTO_PENDENTE);
    }

    @Test
    @DisplayName("estornarValorPago() com valor maior que o já pago lança exceção")
    void estornarValorPago_maiorQueValorPago_lancaExcecao() {
        pagamento.setValorPago(new BigDecimal("50.00"));

        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        assertThatThrownBy(() -> pagamentoService.estornarValorPago(10L, new BigDecimal("100.00")))
                .isInstanceOf(PagamentoValorInvalidoException.class);

        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------
    // aplicarDesconto
    // ---------------------------------------------------------

    @Test
    @DisplayName("aplicarDesconto() delega a ação para OrdemDeServicoService")
    void aplicarDesconto_delegaParaOSEAtualizaPagamento() {
        BigDecimal desconto = new BigDecimal("30.00");

        when(repository.findById(10L)).thenReturn(Optional.of(pagamento));
        when(ordemDeServicoService.buscarPorEntidadeId(1L)).thenReturn(os);

        pagamentoService.aplicarDesconto(10L, desconto);

        // O novo serviço delega para a OS e não salva a entidade Pagamento novamente
        verify(ordemDeServicoService).aplicarDesconto(1L, new BigDecimal("30.00"));
        verify(repository, never()).save(any());
    }
}