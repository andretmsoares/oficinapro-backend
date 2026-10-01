package com.oficinapro.service.oficina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.exception.logo.LogoInvalidaException;
import com.oficinapro.exception.logo.LogoNotFoundException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.storage.LogoStorage;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class OficinaLogoServiceImplTest {

  private static final byte[] PNG = {
    (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
  };
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

  @Mock private OficinaRepository oficinaRepository;
  @Mock private OficinaService oficinaService;
  @Mock private OficinaAccessValidator oficinaAccessValidator;
  @Mock private LogoStorage logoStorage;

  @InjectMocks private OficinaLogoServiceImpl service;

  private Oficina oficina;

  @BeforeEach
  void setUp() {
    oficina = new Oficina(1L, "Oficina Test", "12345678000195", "83999999999", true);
  }

  private static MockMultipartFile arquivo(byte[] conteudo) {
    return new MockMultipartFile("arquivo", "logo", "application/octet-stream", conteudo);
  }

  // ---------------------------------------------------------------
  // atualizar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("atualizar: PNG válido deve ser salvo e a oficina apontar para o novo caminho")
  void deveSalvarLogoPng() {
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.salvar(1L, PNG, "image/png")).thenReturn("logos/1/nova.png");

    service.atualizar(1L, arquivo(PNG));

    assertThat(oficina.getLogoPath()).isEqualTo("logos/1/nova.png");
    verify(oficinaRepository).save(oficina);
    verify(logoStorage, never()).remover(anyString());
  }

  @Test
  @DisplayName("atualizar: JPEG válido deve ser salvo com content-type image/jpeg")
  void deveSalvarLogoJpeg() {
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.salvar(1L, JPEG, "image/jpeg")).thenReturn("logos/1/nova.jpg");

    service.atualizar(1L, arquivo(JPEG));

    assertThat(oficina.getLogoPath()).isEqualTo("logos/1/nova.jpg");
  }

  @Test
  @DisplayName("atualizar: ao substituir, deve remover a logo antiga do storage")
  void deveRemoverLogoAntigaAoSubstituir() {
    oficina.setLogoPath("logos/1/antiga.png");
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.salvar(1L, PNG, "image/png")).thenReturn("logos/1/nova.png");

    service.atualizar(1L, arquivo(PNG));

    assertThat(oficina.getLogoPath()).isEqualTo("logos/1/nova.png");
    verify(logoStorage).remover("logos/1/antiga.png");
  }

  @Test
  @DisplayName("atualizar: sem permissão na oficina não deve tocar em storage nem repositório")
  void deveNegarAtualizacaoSemAcessoAOficina() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarAcessoOficina(1L);

    assertThatThrownBy(() -> service.atualizar(1L, arquivo(PNG)))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(logoStorage, oficinaRepository, oficinaService);
  }

  @Test
  @DisplayName("atualizar: arquivo nulo deve lançar LogoInvalidaException")
  void deveRejeitarArquivoNulo() {
    assertThatThrownBy(() -> service.atualizar(1L, null))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessage("Envie um arquivo de imagem.");

    verifyNoInteractions(logoStorage, oficinaRepository);
  }

  @Test
  @DisplayName("atualizar: arquivo vazio deve lançar LogoInvalidaException")
  void deveRejeitarArquivoVazio() {
    assertThatThrownBy(() -> service.atualizar(1L, arquivo(new byte[0])))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessage("Envie um arquivo de imagem.");
  }

  @Test
  @DisplayName("atualizar: arquivo acima de 2 MB deve lançar LogoInvalidaException")
  void deveRejeitarArquivoGrandeDemais() {
    byte[] grande = new byte[(int) OficinaLogoServiceImpl.TAMANHO_MAXIMO + 1];
    System.arraycopy(PNG, 0, grande, 0, PNG.length);

    assertThatThrownBy(() -> service.atualizar(1L, arquivo(grande)))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessage("A logo deve ter no máximo 2 MB.");

    verifyNoInteractions(logoStorage, oficinaRepository);
  }

  @Test
  @DisplayName("atualizar: arquivo de exatamente 2 MB ainda é aceito")
  void deveAceitarArquivoNoLimiteDeTamanho() {
    byte[] limite = new byte[(int) OficinaLogoServiceImpl.TAMANHO_MAXIMO];
    System.arraycopy(PNG, 0, limite, 0, PNG.length);
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.salvar(1L, limite, "image/png")).thenReturn("logos/1/limite.png");

    service.atualizar(1L, arquivo(limite));

    assertThat(oficina.getLogoPath()).isEqualTo("logos/1/limite.png");
  }

  @Test
  @DisplayName("atualizar: falha de leitura do arquivo deve virar LogoInvalidaException")
  void deveRejeitarArquivoIlegivel() throws IOException {
    MultipartFile ilegivel = mock(MultipartFile.class);
    when(ilegivel.isEmpty()).thenReturn(false);
    when(ilegivel.getSize()).thenReturn(10L);
    when(ilegivel.getBytes()).thenThrow(new IOException("disco"));

    assertThatThrownBy(() -> service.atualizar(1L, ilegivel))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessage("Não foi possível ler o arquivo enviado.");
  }

  @Test
  @DisplayName("atualizar: o tipo vem da assinatura dos bytes, não do content-type informado")
  void deveRejeitarConteudoQueNaoEPngNemJpegMesmoComContentTypeDeImagem() {
    MockMultipartFile falso =
        new MockMultipartFile("arquivo", "logo.png", "image/png", "<svg></svg>".getBytes());

    assertThatThrownBy(() -> service.atualizar(1L, falso))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessage("Formato inválido. Envie uma imagem PNG ou JPEG.");

    verifyNoInteractions(logoStorage, oficinaRepository);
  }

  @Test
  @DisplayName("atualizar: oficina inexistente deve propagar OficinaNotFoundException")
  void devePropagarOficinaInexistente() {
    when(oficinaService.buscarPorEntidadeId(99L)).thenThrow(new OficinaNotFoundException(99L));

    assertThatThrownBy(() -> service.atualizar(99L, arquivo(PNG)))
        .isInstanceOf(OficinaNotFoundException.class);

    verifyNoInteractions(logoStorage);
  }

  // ---------------------------------------------------------------
  // remover()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("remover: deve limpar o caminho na oficina e apagar o arquivo do storage")
  void deveRemoverLogoExistente() {
    oficina.setLogoPath("logos/1/atual.png");
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);

    service.remover(1L);

    assertThat(oficina.getLogoPath()).isNull();
    verify(oficinaRepository).save(oficina);
    verify(logoStorage).remover("logos/1/atual.png");
  }

  @Test
  @DisplayName("remover: é idempotente, sem logo não salva nem toca no storage")
  void removerSemLogoNaoFazNada() {
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);

    service.remover(1L);

    verify(oficinaRepository, never()).save(any());
    verifyNoInteractions(logoStorage);
  }

  @Test
  @DisplayName("remover: sem permissão na oficina deve lançar AccessDeniedException")
  void deveNegarRemocaoSemAcessoAOficina() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarAcessoOficina(2L);

    assertThatThrownBy(() -> service.remover(2L)).isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(oficinaService, oficinaRepository, logoStorage);
  }

  // ---------------------------------------------------------------
  // buscar()
  // ---------------------------------------------------------------

  @Test
  @DisplayName("buscar: deve devolver os bytes com o tipo detectado pelo conteúdo")
  void deveBuscarLogo() {
    oficina.setLogoPath("logos/1/atual.png");
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.ler("logos/1/atual.png")).thenReturn(Optional.of(PNG));

    OficinaLogoService.Logo logo = service.buscar(1L);

    assertThat(logo.conteudo()).isEqualTo(PNG);
    assertThat(logo.contentType()).isEqualTo("image/png");
  }

  @Test
  @DisplayName("buscar: oficina sem logo deve lançar LogoNotFoundException")
  void deveLancarQuandoOficinaNaoTemLogo() {
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);

    assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(LogoNotFoundException.class);

    verifyNoInteractions(logoStorage);
  }

  @Test
  @DisplayName("buscar: caminho registrado mas objeto ausente no storage deve lançar 404")
  void deveLancarQuandoStorageNaoTemOObjeto() {
    oficina.setLogoPath("logos/1/sumiu.png");
    when(oficinaService.buscarPorEntidadeId(1L)).thenReturn(oficina);
    when(logoStorage.ler("logos/1/sumiu.png")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(LogoNotFoundException.class);
  }

  @Test
  @DisplayName("buscar: sem permissão na oficina deve lançar AccessDeniedException")
  void deveNegarBuscaSemAcessoAOficina() {
    doThrow(new AccessDeniedException("negado"))
        .when(oficinaAccessValidator)
        .validarAcessoOficina(2L);

    assertThatThrownBy(() -> service.buscar(2L)).isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(oficinaService, logoStorage);
  }
}
