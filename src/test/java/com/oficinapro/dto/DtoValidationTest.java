package com.oficinapro.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Bean Validation dos DTOs de entrada.
 *
 * <p>Exercita as anotações diretamente com um {@link Validator}, sem subir contexto Spring nem
 * MockMvc. É rápido e testa exatamente o que o {@code @Valid} do controller vai aplicar — cada
 * violação aqui vira um `400` com a mensagem dentro de {@code fields} na resposta.
 *
 * <p>Cobre as validações endurecidas nesta branch: valor de mão de obra, formato de CNPJ, ano do
 * veículo e obrigatoriedade da unidade na OS.
 */
class DtoValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void abrirValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void fecharValidator() {
    if (factory != null) {
      factory.close();
    }
  }

  /** Nomes dos campos que violaram alguma regra. */
  private static <T> Set<String> camposInvalidos(T objeto) {
    return validator.validate(objeto).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  // ==================================================================
  // Mão de obra — valor precisa ser positivo; descrição não pode ser vazia
  // ==================================================================

  @Nested
  @DisplayName("MaoObraRequestDTO")
  class MaoObra {

    private MaoObraRequestDTO comValor(BigDecimal valor) {
      return new MaoObraRequestDTO(1L, valor, "Troca de embreagem");
    }

    @Test
    @DisplayName("aceita um lançamento válido")
    void aceitaLancamentoValido() {
      assertThat(camposInvalidos(comValor(new BigDecimal("250.00")))).isEmpty();
    }

    @Test
    @DisplayName("recusa valor negativo")
    void recusaValorNegativo() {
      assertThat(camposInvalidos(comValor(new BigDecimal("-500.00"))))
          .as(
              "Valor negativo abatia peças legitimamente lançadas e reduzia o total"
                  + " da OS sem deixar rastro de desconto.")
          .contains("valor");
    }

    @Test
    @DisplayName("recusa valor zero")
    void recusaValorZero() {
      assertThat(camposInvalidos(comValor(BigDecimal.ZERO))).contains("valor");
    }

    @Test
    @DisplayName("recusa valor abaixo de um centavo")
    void recusaValorAbaixoDeUmCentavo() {
      assertThat(camposInvalidos(comValor(new BigDecimal("0.001")))).contains("valor");
    }

    @Test
    @DisplayName("aceita exatamente um centavo, o menor valor permitido")
    void aceitaUmCentavo() {
      assertThat(camposInvalidos(comValor(new BigDecimal("0.01")))).isEmpty();
    }

    @Test
    @DisplayName("recusa valor nulo")
    void recusaValorNulo() {
      assertThat(camposInvalidos(comValor(null))).contains("valor");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("recusa descrição vazia, em branco ou nula")
    void recusaDescricaoVazia(String descricao) {
      MaoObraRequestDTO dto = new MaoObraRequestDTO(1L, new BigDecimal("100.00"), descricao);

      assertThat(camposInvalidos(dto))
          .as("@NotNull sozinho deixava passar string vazia")
          .contains("descricao");
    }

    @Test
    @DisplayName("recusa descrição acima de 500 caracteres")
    void recusaDescricaoLonga() {
      MaoObraRequestDTO dto =
          new MaoObraRequestDTO(1L, new BigDecimal("100.00"), "x".repeat(501));

      assertThat(camposInvalidos(dto)).contains("descricao");
    }

    @Test
    @DisplayName("recusa lançamento sem OS")
    void recusaSemOs() {
      MaoObraRequestDTO dto = new MaoObraRequestDTO(null, new BigDecimal("100.00"), "Serviço");

      assertThat(camposInvalidos(dto)).contains("osId");
    }
  }

  // ==================================================================
  // Peça da OS
  // ==================================================================

  @Nested
  @DisplayName("ItemOsPecaRequestDTO")
  class ItemOsPeca {

    private ItemOsPecaRequestDTO com(BigDecimal quantidade, BigDecimal valorUnitario) {
      return new ItemOsPecaRequestDTO(1L, "Pastilha de freio", quantidade, valorUnitario);
    }

    @Test
    @DisplayName("aceita uma peça válida")
    void aceitaPecaValida() {
      assertThat(camposInvalidos(com(new BigDecimal("2"), new BigDecimal("120.00")))).isEmpty();
    }

    @Test
    @DisplayName("recusa quantidade zero")
    void recusaQuantidadeZero() {
      assertThat(camposInvalidos(com(BigDecimal.ZERO, new BigDecimal("120.00"))))
          .contains("quantidade");
    }

    @Test
    @DisplayName("recusa quantidade negativa")
    void recusaQuantidadeNegativa() {
      assertThat(camposInvalidos(com(new BigDecimal("-1"), new BigDecimal("120.00"))))
          .contains("quantidade");
    }

    @Test
    @DisplayName("recusa valor unitário zero")
    void recusaValorUnitarioZero() {
      assertThat(camposInvalidos(com(new BigDecimal("1"), BigDecimal.ZERO)))
          .contains("valorUnitario");
    }

    @Test
    @DisplayName("recusa valor unitário abaixo de um centavo")
    void recusaValorUnitarioAbaixoDeUmCentavo() {
      assertThat(camposInvalidos(com(new BigDecimal("1"), new BigDecimal("0.009"))))
          .contains("valorUnitario");
    }

    @Test
    @DisplayName("recusa nome em branco")
    void recusaNomeEmBranco() {
      ItemOsPecaRequestDTO dto =
          new ItemOsPecaRequestDTO(1L, "   ", new BigDecimal("1"), new BigDecimal("10.00"));

      assertThat(camposInvalidos(dto)).contains("nome");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "2.5", "0.750", "1.1"})
    @DisplayName("recusa quantidade fracionária: peça é contada em unidades inteiras")
    void recusaQuantidadeFracionaria(String quantidade) {
      ItemOsPecaRequestDTO dto =
          new ItemOsPecaRequestDTO(
              1L, "Pastilha de freio", new BigDecimal(quantidade), new BigDecimal("48.00"));

      assertThat(camposInvalidos(dto))
          .as("quantidade de peça é sempre inteira (2 pastilhas, 1 correia), nunca fração")
          .contains("quantidade");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "10", "999"})
    @DisplayName("aceita quantidade inteira em qualquer magnitude razoável")
    void aceitaQuantidadeInteira(String quantidade) {
      ItemOsPecaRequestDTO dto =
          new ItemOsPecaRequestDTO(
              1L, "Pastilha de freio", new BigDecimal(quantidade), new BigDecimal("48.00"));

      assertThat(camposInvalidos(dto)).doesNotContain("quantidade");
    }
  }

  // ==================================================================
  // Peça — update precisa validar igual à criação
  //
  // A validação de update ficou desalinhada da de criação: era possível criar uma
  // peça com quantidade inteira (barrada corretamente) e depois editá-la para uma
  // quantidade fracionária pelo mesmo endpoint que deveria vetar os dois casos.
  // ==================================================================

  @Nested
  @DisplayName("ItemOsPecaUpdateRequestDTO")
  class ItemOsPecaUpdate {

    private ItemOsPecaUpdateRequestDTO com(BigDecimal quantidade, BigDecimal valorUnitario) {
      return new ItemOsPecaUpdateRequestDTO("Pastilha de freio", quantidade, valorUnitario);
    }

    @Test
    @DisplayName("aceita uma atualização válida")
    void aceitaAtualizacaoValida() {
      assertThat(camposInvalidos(com(new BigDecimal("3"), new BigDecimal("120.00")))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "2.5", "1.1"})
    @DisplayName("recusa quantidade fracionária na atualização, igual à criação")
    void recusaQuantidadeFracionariaNaAtualizacao(String quantidade) {
      assertThat(camposInvalidos(com(new BigDecimal(quantidade), new BigDecimal("120.00"))))
          .as("a validação de update precisa proibir fração tanto quanto a de criação")
          .contains("quantidade");
    }

    @Test
    @DisplayName("recusa quantidade zero ou negativa na atualização")
    void recusaQuantidadeNaoPositivaNaAtualizacao() {
      assertThat(camposInvalidos(com(BigDecimal.ZERO, new BigDecimal("120.00"))))
          .contains("quantidade");
      assertThat(camposInvalidos(com(new BigDecimal("-1"), new BigDecimal("120.00"))))
          .contains("quantidade");
    }

    @Test
    @DisplayName("recusa valor unitário abaixo de um centavo na atualização")
    void recusaValorUnitarioBaixoNaAtualizacao() {
      assertThat(camposInvalidos(com(new BigDecimal("1"), new BigDecimal("0.00"))))
          .contains("valorUnitario");
    }
  }

  // ==================================================================
  // Oficina — CNPJ precisa ser 14 dígitos
  // ==================================================================

  @Nested
  @DisplayName("OficinaRequestDTO")
  class OficinaDto {

    private OficinaRequestDTO comCnpj(String cnpj) {
      return new OficinaRequestDTO("Oficina Central", cnpj, "83999998888");
    }

    @Test
    @DisplayName("aceita CNPJ com 14 dígitos")
    void aceitaCnpjValido() {
      assertThat(camposInvalidos(comCnpj("12345678000195"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "abcdefghijklmn", // letras
          "12345678/0001-95", // com máscara
          "1234567800019A", // dígito trocado por letra
          "1234567800019 " // espaço no fim
        })
    @DisplayName("recusa CNPJ que não seja exatamente 14 dígitos")
    void recusaCnpjMalFormatado(String cnpj) {
      assertThat(camposInvalidos(comCnpj(cnpj))).contains("cnpj");
    }

    @Test
    @DisplayName("recusa CNPJ curto demais")
    void recusaCnpjCurto() {
      assertThat(camposInvalidos(comCnpj("123"))).contains("cnpj");
    }

    @Test
    @DisplayName("recusa CNPJ em branco")
    void recusaCnpjEmBranco() {
      assertThat(camposInvalidos(comCnpj("   "))).contains("cnpj");
    }
  }

  // ==================================================================
  // Veículo — ano mínimo e formato da placa
  // ==================================================================

  @Nested
  @DisplayName("VeiculoRequestDTO")
  class VeiculoDto {

    private VeiculoRequestDTO comAno(Integer ano) {
      return new VeiculoRequestDTO("Civic", ano, "Honda", "ABC1D23", 1L);
    }

    @Test
    @DisplayName("aceita um veículo válido")
    void aceitaVeiculoValido() {
      assertThat(camposInvalidos(comAno(2020))).isEmpty();
    }

    @Test
    @DisplayName("recusa ano anterior a 1900")
    void recusaAnoAntigo() {
      assertThat(camposInvalidos(comAno(1899))).contains("ano");
    }

    @Test
    @DisplayName("recusa ano zero")
    void recusaAnoZero() {
      assertThat(camposInvalidos(comAno(0))).contains("ano");
    }

    @Test
    @DisplayName("aceita exatamente 1900, o limite inferior")
    void aceitaLimiteInferior() {
      assertThat(camposInvalidos(comAno(1900))).isEmpty();
    }

    @Test
    @DisplayName("aceita ano nulo: o campo é opcional")
    void aceitaAnoNulo() {
      assertThat(camposInvalidos(comAno(null))).doesNotContain("ano");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1234", "ABC-1234", "ABC1D23", "abc1d23"})
    @DisplayName("aceita placa nos formatos antigo e Mercosul")
    void aceitaPlacasValidas(String placa) {
      VeiculoRequestDTO dto = new VeiculoRequestDTO("Civic", 2020, "Honda", placa, 1L);

      assertThat(camposInvalidos(dto)).doesNotContain("placa");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD123", "1234ABC", "ABC12345"})
    @DisplayName("recusa placa fora do padrão")
    void recusaPlacasInvalidas(String placa) {
      VeiculoRequestDTO dto = new VeiculoRequestDTO("Civic", 2020, "Honda", placa, 1L);

      assertThat(camposInvalidos(dto)).contains("placa");
    }
  }

  // ==================================================================
  // Ordem de serviço — unidade passou a ser obrigatória
  // ==================================================================

  @Nested
  @DisplayName("OrdemDeServicoRequestDTO")
  class OrdemDeServicoDto {

    @Test
    @DisplayName("aceita uma OS válida")
    void aceitaOsValida() {
      OrdemDeServicoRequestDTO dto =
          new OrdemDeServicoRequestDTO(1L, 1L, 1L, 1L, 1L, "Revisão geral");

      assertThat(camposInvalidos(dto)).isEmpty();
    }

    @Test
    @DisplayName("recusa OS sem unidade")
    void recusaOsSemUnidade() {
      OrdemDeServicoRequestDTO dto = new OrdemDeServicoRequestDTO(1L, null, 1L, 1L, 1L, "obs");

      assertThat(camposInvalidos(dto))
          .as(
              "A coluna unidade_id é NOT NULL. Sem @NotNull no DTO, a ausência virava"
                  + " findById(null) e um 400 genérico em vez de erro de campo.")
          .contains("unidadeId");
    }

    @Test
    @DisplayName("cliente e mecânico continuam opcionais")
    void clienteEMecanicoSaoOpcionais() {
      OrdemDeServicoRequestDTO dto =
          new OrdemDeServicoRequestDTO(1L, 1L, 1L, null, null, "veículo sem dono identificado");

      assertThat(camposInvalidos(dto))
          .as("a OS pode nascer antes de se saber quem é o proprietário")
          .isEmpty();
    }

    @Test
    @DisplayName("recusa OS sem oficina e sem veículo")
    void recusaOsSemOficinaESemVeiculo() {
      OrdemDeServicoRequestDTO dto = new OrdemDeServicoRequestDTO(null, 1L, null, 1L, 1L, "obs");

      assertThat(camposInvalidos(dto)).contains("oficinaId", "veiculoId");
    }
  }
}
