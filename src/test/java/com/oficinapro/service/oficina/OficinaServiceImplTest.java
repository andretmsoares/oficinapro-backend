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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OficinaServiceImplTest {

    @Mock
    private OficinaRepository oficinaRepository;

    @InjectMocks
    private OficinaServiceImpl oficinaService;

    private Oficina oficina;
    private OficinaRequestDTO oficinaRequestDTO;

    @BeforeEach
    void setUp() {
        oficina = new Oficina();
        oficina.setId(1L);
        oficina.setNome("Oficina Central");
        oficina.setCnpj("12345678000195");
        oficina.setTelefone("83999998888");

        oficinaRequestDTO = new OficinaRequestDTO("Oficina Central", "12345678000195", "83999998888");
    }

    @Test
    @DisplayName("Deve listar todas as oficinas retornando DTOs")
    void deveListarOficinas() {
        when(oficinaRepository.findAll()).thenReturn(List.of(oficina));

        List<OficinaResponseDTO> resultado = oficinaService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).nome()).isEqualTo("Oficina Central");
        verify(oficinaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar oficina por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

        OficinaResponseDTO resultado = oficinaService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.cnpj()).isEqualTo("12345678000195");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar ID inexistente")
    void deveLancarExcecaoAoBuscarIdInexistente() {
        when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oficinaService.buscarPorId(99L))
                .isInstanceOf(OficinaNotFoundException.class);
    }

    @Test
    @DisplayName("Deve criar oficina com sucesso quando CNPJ não existir")
    void deveCriarOficinaComSucesso() {
        when(oficinaRepository.existsByCnpj(oficinaRequestDTO.cnpj())).thenReturn(false);
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        OficinaResponseDTO resultado = oficinaService.criar(oficinaRequestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nome()).isEqualTo(oficinaRequestDTO.nome());
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar oficina com CNPJ já existente")
    void deveLancarExcecaoAoCriarComCnpjExistente() {
        when(oficinaRepository.existsByCnpj(oficinaRequestDTO.cnpj())).thenReturn(true);

        assertThatThrownBy(() -> oficinaService.criar(oficinaRequestDTO))
                .isInstanceOf(CnpjAlreadyExistsException.class);

        verify(oficinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar oficina mantendo o mesmo CNPJ com sucesso")
    void deveAtualizarMantendoMesmoCnpj() {
        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        OficinaResponseDTO resultado = oficinaService.atualizar(1L, oficinaRequestDTO);

        assertThat(resultado).isNotNull();
        verify(oficinaRepository, never()).existsByCnpj(anyString());
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("Deve atualizar oficina alterando para um CNPJ novo e válido")
    void deveAtualizarComNovoCnpjValido() {
        OficinaRequestDTO requestNovoCnpj = new OficinaRequestDTO("Oficina Nova", "98765432000110", "83988887777");

        when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.existsByCnpj("98765432000110")).thenReturn(false);
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        OficinaResponseDTO resultado = oficinaService.atualizar(1L, requestNovoCnpj);

        assertThat(resultado).isNotNull();
        verify(oficinaRepository, times(1)).existsByCnpj("98765432000110");
        verify(oficinaRepository, times(1)).save(any(Oficina.class));
    }

    @Test
    @DisplayName("Deve deletar oficina existente com sucesso")
    void deveDeletarComSucesso() {
        when(oficinaRepository.existsById(1L)).thenReturn(true);
        doNothing().when(oficinaRepository).deleteById(1L);

        oficinaService.deletar(1L);

        verify(oficinaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar ID inexistente")
    void deveLancarExcecaoAoDeletarIdInexistente() {
        when(oficinaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> oficinaService.deletar(99L))
                .isInstanceOf(OficinaNotFoundException.class);

        verify(oficinaRepository, never()).deleteById(anyLong());
    }
}