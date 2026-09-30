package com.oficinapro.service.oficina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaAlreadyActivatedException;
import com.oficinapro.exception.oficina.OficinaAlreadyDisabledException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OficinaServiceTest {

  @Mock private OficinaRepository oficinaRepository;

  // Adicionado no refactor: OficinaServiceImpl passou a exigir role ADMIN em
  // listar/buscarPorId/criar/atualizar via OficinaAccessValidator.
  @Mock private com.oficinapro.security.OficinaAccessValidator oficinaAccessValidator;

  @InjectMocks private OficinaServiceImpl service;

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

    assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(OficinaNotFoundException.class);
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

    assertThatThrownBy(() -> service.criar(request)).isInstanceOf(CnpjAlreadyExistsException.class);

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
  @DisplayName(
      "atualizar() deve lançar CnpjAlreadyExistsException quando novo CNPJ já pertence a outra oficina")
  void atualizar_novoCnpjDuplicado_lancaCnpjAlreadyExistsException() {
    OficinaRequestDTO requestNovoCnpj =
        new OficinaRequestDTO("Oficina Central", "98765432000110", "83999998888");

    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));
    when(oficinaRepository.existsByCnpj("98765432000110")).thenReturn(true);

    assertThatThrownBy(() -> service.atualizar(1L, requestNovoCnpj))
        .isInstanceOf(CnpjAlreadyExistsException.class);

    verify(oficinaRepository, never()).save(any());
  }

  // ─────────────────────────── desativar / ativar ───────────────────────────
  //
  // A exclusão lógica substitui a física: apagar uma oficina levava junto, por
  // cascata, unidades, pessoas, veículos, OS, pagamentos e todo o histórico.

  @Test
  @DisplayName("desativar() deve marcar a oficina como inativa e persistir")
  void desativar_oficinaAtiva_marcaComoInativa() {
    oficina.setAtivo(true);
    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

    service.desativar(1L);

    assertThat(oficina.getAtivo()).isFalse();
    verify(oficinaRepository).save(oficina);
  }

  @Test
  @DisplayName("desativar() deve exigir papel ADMIN")
  void desativar_exigeAdmin() {
    doThrow(new AccessDeniedException("sem permissão"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.desativar(1L)).isInstanceOf(AccessDeniedException.class);

    verify(oficinaRepository, never()).save(any());
  }

  @Test
  @DisplayName("desativar() uma oficina já inativa deve lançar OficinaAlreadyDisabledException")
  void desativar_oficinaJaInativa_lancaExcecao() {
    oficina.setAtivo(false);
    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

    assertThatThrownBy(() -> service.desativar(1L))
        .isInstanceOf(OficinaAlreadyDisabledException.class);

    verify(oficinaRepository, never()).save(any());
  }

  @Test
  @DisplayName("desativar() deve lançar OficinaNotFoundException quando o ID não existe")
  void desativar_idNaoExistente_lancaExcecao() {
    when(oficinaRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.desativar(99L)).isInstanceOf(OficinaNotFoundException.class);
  }

  @Test
  @DisplayName("ativar() deve reativar uma oficina desativada")
  void ativar_oficinaInativa_reativa() {
    oficina.setAtivo(false);
    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

    service.ativar(1L);

    assertThat(oficina.getAtivo())
        .as("reativar é o caminho de volta da exclusão lógica implementada em desativar()")
        .isTrue();
    verify(oficinaRepository).save(oficina);
  }

  @Test
  @DisplayName("ativar() uma oficina já ativa deve lançar OficinaAlreadyActivatedException")
  void ativar_oficinaJaAtiva_lancaExcecao() {
    oficina.setAtivo(true);
    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

    assertThatThrownBy(() -> service.ativar(1L))
        .isInstanceOf(OficinaAlreadyActivatedException.class);

    verify(oficinaRepository, never()).save(any());
  }

  @Test
  @DisplayName("ativar() deve exigir papel ADMIN")
  void ativar_exigeAdmin() {
    doThrow(new AccessDeniedException("sem permissão"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.ativar(1L)).isInstanceOf(AccessDeniedException.class);

    verify(oficinaRepository, never()).save(any());
  }

  @Test
  @DisplayName("a resposta deve expor o flag ativo")
  void respostaExpoeFlagAtivo() {
    oficina.setAtivo(false);
    when(oficinaRepository.findById(1L)).thenReturn(Optional.of(oficina));

    OficinaResponseDTO resposta = service.buscarPorId(1L);

    assertThat(resposta.ativo()).isFalse();
  }

  // ─────────────────────────── buscar (paginado) ───────────────────────────

  @Test
  @DisplayName("buscar() deve normalizar o termo e pesquisar por nome ou CNPJ")
  void buscar_comTermo_normalizaEPesquisaPorNomeOuCnpj() {
    Pageable pageable = PageRequest.of(0, 10);
    when(oficinaRepository.findByNomeContainingIgnoreCaseOrCnpjContainingIgnoreCase(
            "OFICINA", "OFICINA", pageable))
        .thenReturn(new PageImpl<>(List.of(oficina)));

    Page<OficinaResponseDTO> resultado = service.buscar("  oficína ", pageable);

    assertThat(resultado.getContent()).extracting(OficinaResponseDTO::id).containsExactly(1L);
    verify(oficinaAccessValidator).validarRole(Role.ADMIN);
  }

  @Test
  @DisplayName("buscar() com termo nulo deve pesquisar com termo vazio (lista tudo)")
  void buscar_termoNulo_pesquisaComTermoVazio() {
    Pageable pageable = PageRequest.of(0, 10);
    when(oficinaRepository.findByNomeContainingIgnoreCaseOrCnpjContainingIgnoreCase(
            "", "", pageable))
        .thenReturn(new PageImpl<>(List.of(oficina)));

    Page<OficinaResponseDTO> resultado = service.buscar(null, pageable);

    assertThat(resultado.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("buscar() deve exigir papel ADMIN antes de consultar")
  void buscar_exigeAdmin() {
    doThrow(new AccessDeniedException("sem permissão"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.buscar("x", PageRequest.of(0, 10)))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(oficinaRepository);
  }

  // ─────────────────────────── exigência de ADMIN ───────────────────────────

  @Test
  @DisplayName("listar, buscarPorId, criar e atualizar devem exigir papel ADMIN")
  void operacoesAdministrativasExigemAdmin() {
    doThrow(new AccessDeniedException("sem permissão"))
        .when(oficinaAccessValidator)
        .validarRole(Role.ADMIN);

    assertThatThrownBy(() -> service.listar()).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> service.buscarPorId(1L)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> service.criar(request)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> service.atualizar(1L, request))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(oficinaRepository);
  }

  @Test
  @DisplayName("criar() deve normalizar o nome e nascer ativa")
  void criar_normalizaNomeENasceAtiva() {
    when(oficinaRepository.existsByCnpj("12345678000195")).thenReturn(false);
    when(oficinaRepository.save(any(Oficina.class))).thenAnswer(inv -> inv.getArgument(0));

    OficinaResponseDTO resultado =
        service.criar(new OficinaRequestDTO(" oficína sul ", "12345678000195", "83999998888"));

    assertThat(resultado.nome()).isEqualTo("OFICINA SUL");
    assertThat(resultado.ativo()).isTrue();
  }
}
