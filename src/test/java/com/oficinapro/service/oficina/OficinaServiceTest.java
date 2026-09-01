package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OficinaServiceTest {

    @Mock
    private OficinaRepository oficinaRepository;

    @InjectMocks
    private OficinaServiceImpl service;

    private Oficina oficina;
    private OficinaRequestDTO request;

    @BeforeEach
    void setUp() {
        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Central");
        oficina.setCnpj("12345678000195");
        oficina.setTelefone("83999998888");

        request = new OficinaRequestDTO("Oficina Central", "12345678000195", "83999998888");
    }

    // ─────────────────────────── listar ───────────────────────────

    @Test
    @DisplayName("listar() deve retornar lista de DTOs mapeados do repositório")
    void listar_retornaListaDeDTOs() {
        Oficina outra = new Oficina();
        outra.setId(2L);
        outra.setNome("Oficina Sul");
        outra.setCnpj("98765432000110");
        outra.setTelefone("83988887777");

        when(oficinaRepository.findAll()).thenReturn(List.of(oficina, outra));

        List<OficinaResponseDTO> resultado = service.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).nome()).isEqualTo("Oficina Central");
        assertThat(resultado.get(0).cnpj()).isEqualTo("12345678000195");
        assertThat(resultado.get(1).id()).isEqualTo(2L);
        verify(oficinaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listar() deve retornar lista vazia quando não há oficinas")
    void listar_repositorioVazio_retornaListaVazia() {
        when(oficinaRepository.findAll()).thenReturn(List.of());

        List<OficinaResponseDTO> resultado = service.listar();

        assertThat(resultado).isEmpty();
    }

    // ─────────────────────────── buscarPorId ───────────────────────────

    @Test
    @DisplayName("buscarPorId() deve retornar DTO quando oficina existe")
    void buscarPorId_encontrado_retornaDTO() {
        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

        OficinaResponseDTO resultado = service.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Oficina Central");
        assertThat(resultado.cnpj()).isEqualTo("12345678000195");
        assertThat(resultado.telefone()).isEqualTo("83999998888");
    }

    @Test
    @DisplayName("buscarPorId() deve lançar OficinaNotFoundException quando ID não existe")
    void buscarPorId_naoEncontrado_lancaOficinaNotFoundException() {
        when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(OficinaNotFoundException.class);
    }

    // ─────────────────────────── buscarPorEntidadeId ───────────────────────────

    @Test
    @DisplayName("buscarPorEntidadeId() deve retornar entidade quando oficina existe")
    void buscarPorEntidadeId_encontrado_retornaEntidade() {
        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

        Oficina resultado = service.buscarPorEntidadeId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCnpj()).isEqualTo("12345678000195");
    }

    @Test
    @DisplayName("buscarPorEntidadeId() deve lançar OficinaNotFoundException quando ID não existe")
    void buscarPorEntidadeId_naoEncontrado_lancaOficinaNotFoundException() {
        when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorEntidadeId(99L))
                .isInstanceOf(OficinaNotFoundException.class);
    }

    // ─────────────────────────── existsById ───────────────────────────

    @Test
    @DisplayName("existsById() deve retornar true quando ID existe")
    void existsById_idExiste_retornaTrue() {
        when(oficinaRepository.existsById(1L)).thenReturn(true);

        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("existsById() deve retornar false quando ID não existe")
    void existsById_idNaoExiste_retornaFalse() {
        when(oficinaRepository.existsById(99L)).thenReturn(false);

        assertThat(service.existsById(99L)).isFalse();
    }

    // ─────────────────────────── criar ───────────────────────────

    @Test
    @DisplayName("criar() deve criar e retornar DTO quando CNPJ não está em uso")
    void criar_cnpjNovoCaso_sucesso() {
        when(oficinaRepository.existsByCnpj("12345678000195")).thenReturn(false);
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        OficinaResponseDTO resultado = service.criar(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Oficina Central");
        assertThat(resultado.cnpj()).isEqualTo("12345678000195");
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("criar() deve lançar CnpjAlreadyExistsException quando CNPJ já está cadastrado")
    void criar_cnpjDuplicado_lancaCnpjAlreadyExistsException() {
        when(oficinaRepository.existsByCnpj("12345678000195")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(CnpjAlreadyExistsException.class);

        verify(oficinaRepository, never()).save(any());
    }

    // ─────────────────────────── atualizar ───────────────────────────

    @Test
    @DisplayName("atualizar() deve atualizar com sucesso mantendo o mesmo CNPJ")
    void atualizar_mesmoCnpj_sucesso() {
        // O CNPJ do request é igual ao da entidade -> não chama existsByCnpj
        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        OficinaResponseDTO resultado = service.atualizar(1L, request);

        assertThat(resultado).isNotNull();
        verify(oficinaRepository, never()).existsByCnpj(anyString());
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("atualizar() deve atualizar com sucesso quando novo CNPJ está disponível")
    void atualizar_novoCnpjDisponivel_sucesso() {
        OficinaRequestDTO requestNovoCnpj =
                new OficinaRequestDTO("Oficina Atualizada", "98765432000110", "83988887777");

        Oficina atualizada = new Oficina();
        atualizada.setId(1L);
        atualizada.setNome("Oficina Atualizada");
        atualizada.setCnpj("98765432000110");
        atualizada.setTelefone("83988887777");

        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.existsByCnpj("98765432000110")).thenReturn(false);
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(atualizada);

        OficinaResponseDTO resultado = service.atualizar(1L, requestNovoCnpj);

        assertThat(resultado).isNotNull();
        assertThat(resultado.cnpj()).isEqualTo("98765432000110");
        verify(oficinaRepository, times(1)).existsByCnpj("98765432000110");
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("atualizar() deve lançar OficinaNotFoundException quando ID não existe")
    void atualizar_idNaoEncontrado_lancaOficinaNotFoundException() {
        when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request))
                .isInstanceOf(OficinaNotFoundException.class);

        verify(oficinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar() deve lançar CnpjAlreadyExistsException quando novo CNPJ já pertence a outra oficina")
    void atualizar_novoCnpjDuplicado_lancaCnpjAlreadyExistsException() {
        OficinaRequestDTO requestNovoCnpj =
                new OficinaRequestDTO("Oficina Central", "98765432000110", "83999998888");

        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.existsByCnpj("98765432000110")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, requestNovoCnpj))
                .isInstanceOf(CnpjAlreadyExistsException.class);

        verify(oficinaRepository, never()).save(any());
    }

    // ─────────────────────────── deletar ───────────────────────────

    @Test
    @DisplayName("deletar() deve remover a oficina quando ID existe")
    void deletar_idExistente_sucesso() {
        when(oficinaRepository.existsById(1L)).thenReturn(true);
        doNothing().when(oficinaRepository).deleteById(1L);

        service.deletar(1L);

        verify(oficinaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deletar() deve lançar OficinaNotFoundException quando ID não existe")
    void deletar_idNaoExistente_lancaOficinaNotFoundException() {
        when(oficinaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(OficinaNotFoundException.class);

        verify(oficinaRepository, never()).deleteById(anyLong());
    }
}
