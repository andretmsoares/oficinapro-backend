package com.oficinapro.service.registro_pagamento;

import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.model.OrdemDeServico;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.RegistroPagamento;
import com.oficinapro.repository.RegistroPagamentoRepository;
import com.oficinapro.service.pagamento.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroPagamentoServiceImplTest {

    @Mock
    private RegistroPagamentoRepository registroPagamentoRepository;

    @Mock
    private PagamentoService pagamentoService;

    @InjectMocks
    private RegistroPagamentoServiceImpl registroPagamentoService;

    private Pagamento pagamento;
    private RegistroPagamento registro;

    @BeforeEach
    void setUp() {
        OrdemDeServico os = new OrdemDeServico();
        os.setId(1L);

        pagamento = new Pagamento();
        pagamento.setId(10L);
        pagamento.setOrdemDeServico(os);

        registro = new RegistroPagamento();
        registro.setId(100L);
        registro.setPagamento(pagamento);
        registro.setValor(new BigDecimal("150.00"));
        registro.setMeioPagamento(MeioPagamento.PIX);
    }

    // ---------------------------------------------------------
    // criar
    // ---------------------------------------------------------

    @Test
    void criar_atualizaValorPagoAntesDeSalvarRegistro() {
        RegistroPagamentoRequestDTO request = new RegistroPagamentoRequestDTO(
                10L, new BigDecimal("150.00"), MeioPagamento.PIX);

        when(pagamentoService.buscarEntidadePorId(10L)).thenReturn(pagamento);
        when(registroPagamentoRepository.save(any(RegistroPagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroPagamentoResponseDTO response = registroPagamentoService.criar(request);

        verify(pagamentoService).atualizarValorPago(10L, new BigDecimal("150.00"));
        assertThat(response.pagamentoId()).isEqualTo(10L);
        assertThat(response.valor()).isEqualByComparingTo("150.00");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void criar_propagaExcecaoDoPagamentoSemSalvarRegistro() {
        RegistroPagamentoRequestDTO request = new RegistroPagamentoRequestDTO(
                10L, new BigDecimal("9999.00"), MeioPagamento.PIX);

        when(pagamentoService.buscarEntidadePorId(10L)).thenReturn(pagamento);
        doThrow(new RuntimeException("valor excedido"))
                .when(pagamentoService).atualizarValorPago(10L, new BigDecimal("9999.00"));

        assertThatThrownBy(() -> registroPagamentoService.criar(request))
                .isInstanceOf(RuntimeException.class);

        verify(registroPagamentoRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // listarPorPagamento
    // ---------------------------------------------------------

    @Test
    void listarPorPagamento_validaAcessoAoPagamentoERetornaLista() {
        when(pagamentoService.buscarEntidadePorId(10L)).thenReturn(pagamento);
        when(registroPagamentoRepository.findByPagamentoId(10L)).thenReturn(List.of(registro));

        List<RegistroPagamentoResponseDTO> resultado = registroPagamentoService.listarPorPagamento(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(100L);
    }

    // ---------------------------------------------------------
    // buscarPorId
    // ---------------------------------------------------------

    @Test
    void buscarPorId_naoEncontrado_lancaExcecao() {
        when(registroPagamentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registroPagamentoService.buscarPorId(999L))
                .isInstanceOf(RegistroPagamentoNotFoundException.class);
    }

    @Test
    void buscarPorId_encontrado_retornaDTO() {
        when(registroPagamentoRepository.findById(100L)).thenReturn(Optional.of(registro));

        RegistroPagamentoResponseDTO response = registroPagamentoService.buscarPorId(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.meioPagamento()).isEqualTo(MeioPagamento.PIX);

        // Isolamento por oficina: o acesso é delegado ao pagamento
        verify(pagamentoService).buscarEntidadePorId(10L);
    }

    @Test
    void buscarPorId_registroDeOutraOficina_naoVazaOsDados() {
        when(registroPagamentoRepository.findById(100L)).thenReturn(Optional.of(registro));
        when(pagamentoService.buscarEntidadePorId(10L))
                .thenThrow(new PagamentoNotFoundException(10L));

        assertThatThrownBy(() -> registroPagamentoService.buscarPorId(100L))
                .isInstanceOf(PagamentoNotFoundException.class);
    }

    // ---------------------------------------------------------
    // deletar
    // ---------------------------------------------------------

    @Test
    void deletar_estornaValorNoPagamentoAntesDeExcluir() {
        when(registroPagamentoRepository.findById(100L)).thenReturn(Optional.of(registro));

        registroPagamentoService.deletar(100L);

        verify(pagamentoService).estornarValorPago(10L, new BigDecimal("150.00"));
        verify(registroPagamentoRepository).delete(registro);
    }

    @Test
    void deletar_naoEncontrado_lancaExcecaoENaoEstorna() {
        when(registroPagamentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registroPagamentoService.deletar(999L))
                .isInstanceOf(RegistroPagamentoNotFoundException.class);

        verify(pagamentoService, never()).estornarValorPago(any(), any());
        verify(registroPagamentoRepository, never()).delete(any());
    }
}