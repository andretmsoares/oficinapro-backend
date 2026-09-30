package com.oficinapro.service.estatisticas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.dto.estatisticas.EstatisticasOficinaResponseDTO;
import com.oficinapro.dto.estatisticas.EstatisticasSistemaResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.ClienteRepository;
import com.oficinapro.repository.MecanicoRepository;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.repository.OrdemDeServicoRepository;
import com.oficinapro.repository.VeiculoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EstatisticasServiceImplTest {

  @Mock private OficinaRepository oficinaRepository;
  @Mock private ClienteRepository clienteRepository;
  @Mock private MecanicoRepository mecanicoRepository;
  @Mock private VeiculoRepository veiculoRepository;
  @Mock private OrdemDeServicoRepository ordemDeServicoRepository;
  @Mock private OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private EstatisticasServiceImpl service;

  private static Oficina oficina(Long id, String nome, String cnpj, boolean ativo) {
    return new Oficina(id, nome, cnpj, "83999999999", ativo);
  }

  // ---------------------------------------------------------------
  // resumoDoSistema()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("resumoDoSistema: deve combinar as contagens por oficina e totalizar")
  void deveTotalizarContagensPorOficina() {
    when(oficinaRepository.findAll(any(Sort.class)))
        .thenReturn(
            List.of(
                oficina(1L, "Alfa", "11111111000111", true),
                oficina(2L, "Beta", "22222222000122", false)));
    when(clienteRepository.contarPorOficina())
        .thenReturn(
            List.of(
                new ContagemPorOficinaDTO(1L, "Alfa", 5),
                new ContagemPorOficinaDTO(2L, "Beta", 2)));
    when(mecanicoRepository.contarPorOficina())
        .thenReturn(List.of(new ContagemPorOficinaDTO(1L, "Alfa", 3)));
    when(veiculoRepository.contarPorOficina())
        .thenReturn(List.of(new ContagemPorOficinaDTO(2L, "Beta", 4)));
    when(ordemDeServicoRepository.contarPorOficina())
        .thenReturn(
            List.of(
                new ContagemPorOficinaDTO(1L, "Alfa", 10),
                new ContagemPorOficinaDTO(2L, "Beta", 1)));

    EstatisticasSistemaResponseDTO resultado = service.resumoDoSistema();

    assertThat(resultado.oficinas()).isEqualTo(2);
    assertThat(resultado.clientes()).isEqualTo(7);
    assertThat(resultado.mecanicos()).isEqualTo(3);
    assertThat(resultado.veiculos()).isEqualTo(4);
    assertThat(resultado.ordensDeServico()).isEqualTo(11);

    assertThat(resultado.porOficina()).hasSize(2);
    EstatisticasOficinaResponseDTO alfa = resultado.porOficina().get(0);
    assertThat(alfa.id()).isEqualTo(1L);
    assertThat(alfa.nome()).isEqualTo("Alfa");
    assertThat(alfa.ativo()).isTrue();
    assertThat(alfa.clientes()).isEqualTo(5);
    assertThat(alfa.mecanicos()).isEqualTo(3);
    // sem linha de contagem para a oficina => zero, não null
    assertThat(alfa.veiculos()).isZero();
    assertThat(alfa.ordensDeServico()).isEqualTo(10);

    EstatisticasOficinaResponseDTO beta = resultado.porOficina().get(1);
    assertThat(beta.ativo()).isFalse();
    assertThat(beta.mecanicos()).isZero();
    assertThat(beta.veiculos()).isEqualTo(4);
  }

  @Test
  @DisplayName("resumoDoSistema: deve ordenar as oficinas por nome")
  void deveSolicitarOficinasOrdenadasPorNome() {
    when(oficinaRepository.findAll(Sort.by("nome"))).thenReturn(List.of());
    when(clienteRepository.contarPorOficina()).thenReturn(List.of());
    when(mecanicoRepository.contarPorOficina()).thenReturn(List.of());
    when(veiculoRepository.contarPorOficina()).thenReturn(List.of());
    when(ordemDeServicoRepository.contarPorOficina()).thenReturn(List.of());

    EstatisticasSistemaResponseDTO resultado = service.resumoDoSistema();

    assertThat(resultado.oficinas()).isZero();
    assertThat(resultado.clientes()).isZero();
    assertThat(resultado.porOficina()).isEmpty();
  }

  @Test
  @DisplayName("resumoDoSistema: usuário que não é ADMIN deve ser barrado antes de qualquer query")
  void resumoDoSistemaDeveExigirAdmin() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.resumoDoSistema()).isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(
        oficinaRepository,
        clienteRepository,
        mecanicoRepository,
        veiculoRepository,
        ordemDeServicoRepository);
  }

  // ---------------------------------------------------------------
  // resumoDaOficina()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("resumoDaOficina: deve devolver os dados da oficina com as contagens dela")
  void deveResumirUmaOficina() {
    when(oficinaRepository.findById(1L))
        .thenReturn(Optional.of(oficina(1L, "Alfa", "11111111000111", true)));
    when(clienteRepository.countByOficinaId(1L)).thenReturn(5);
    when(mecanicoRepository.countByOficinaId(1L)).thenReturn(3L);
    when(veiculoRepository.countByOficinaId(1L)).thenReturn(8);
    when(ordemDeServicoRepository.countByOficinaId(1L)).thenReturn(12L);

    EstatisticasOficinaResponseDTO resultado = service.resumoDaOficina(1L);

    assertThat(resultado.id()).isEqualTo(1L);
    assertThat(resultado.nome()).isEqualTo("Alfa");
    assertThat(resultado.cnpj()).isEqualTo("11111111000111");
    assertThat(resultado.telefone()).isEqualTo("83999999999");
    assertThat(resultado.ativo()).isTrue();
    assertThat(resultado.clientes()).isEqualTo(5);
    assertThat(resultado.mecanicos()).isEqualTo(3);
    assertThat(resultado.veiculos()).isEqualTo(8);
    assertThat(resultado.ordensDeServico()).isEqualTo(12);
  }

  @Test
  @DisplayName("resumoDaOficina: oficina inexistente deve lançar OficinaNotFoundException")
  void deveLancarQuandoOficinaNaoExiste() {
    when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resumoDaOficina(99L))
        .isInstanceOf(OficinaNotFoundException.class);

    verifyNoInteractions(
        clienteRepository, mecanicoRepository, veiculoRepository, ordemDeServicoRepository);
  }

  @Test
  @DisplayName("resumoDaOficina: usuário que não é ADMIN deve ser barrado antes da consulta")
  void resumoDaOficinaDeveExigirAdmin() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.resumoDaOficina(1L)).isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(oficinaRepository);
  }
}
