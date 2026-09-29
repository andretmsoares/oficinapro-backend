package com.oficinapro.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Executa as migrations reais (src/main/resources/db/migration) do zero, sem passar pelo schema
 * gerado pelas entidades JPA. Os demais testes usam {@code ddl-auto=create-drop} e Flyway
 * desligado, entao um erro apenas no SQL (constraint errada, coluna duplicada, ordem de criacao)
 * passaria despercebido.
 *
 * <p>Roda em H2 (modo PostgreSQL) para nao exigir Docker na suite. A validacao completa contra um
 * PostgreSQL real (incluindo {@code ddl-auto=validate}) continua sendo subir a aplicacao com o
 * banco limpo, descrito em docs/development.md.
 */
class FlywayMigrationsTest {

  private static final String URL =
      "jdbc:h2:mem:flyway_migrations;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static final Set<String> TABELAS_ESPERADAS =
      Set.of(
          "OFICINA",
          "UNIDADE",
          "PESSOA",
          "CLIENTE",
          "USUARIO",
          "MECANICO",
          "VEICULO",
          "ORDEM_SERVICO",
          "ITEM_OS_PECA",
          "MAO_OBRA",
          "PAGAMENTO",
          "REGISTRO_PAGAMENTO");

  private static int migrationsAplicadas;

  @BeforeAll
  static void migrar() {
    Flyway flyway =
        Flyway.configure().dataSource(URL, "sa", "").locations("classpath:db/migration").load();
    migrationsAplicadas = flyway.migrate().migrationsExecuted;
  }

  private static Connection conectar() throws SQLException {
    return DriverManager.getConnection(URL, "sa", "");
  }

  @Test
  @DisplayName("deve aplicar uma migration por entidade persistente, sem sobras")
  void deveAplicarUmaMigrationPorEntidade() {
    assertThat(migrationsAplicadas).isEqualTo(TABELAS_ESPERADAS.size());
  }

  @Test
  @DisplayName("deve criar exatamente as tabelas das entidades, sem tabelas orfas de compras")
  void deveCriarApenasAsTabelasDasEntidades() throws SQLException {
    String sql =
        "SELECT table_name FROM information_schema.tables"
            + " WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'";
    Set<String> tabelas = new HashSet<>();

    try (Connection con = conectar();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        tabelas.add(rs.getString(1).toUpperCase());
      }
    }

    tabelas.remove("FLYWAY_SCHEMA_HISTORY");

    assertThat(tabelas)
        .containsExactlyInAnyOrderElementsOf(TABELAS_ESPERADAS)
        .doesNotContain("FORNECEDOR", "NOTA_COMPRA", "ITEM_NOTA_COMPRA");
  }

  @Test
  @DisplayName("chk_usuario_role deve aceitar GERENTE e recusar o papel legado ADMINISTRATIVO")
  void deveAceitarGerenteERecusarAdministrativo() throws SQLException {
    try (Connection con = conectar();
        Statement st = con.createStatement()) {
      long gerente = inserirPessoa(st, "Gerente");
      long legado = inserirPessoa(st, "Legado");

      inserirUsuario(st, gerente, "gerente.check", "GERENTE");

      assertThatThrownBy(() -> inserirUsuario(st, legado, "legado.check", "ADMINISTRATIVO"))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  @DisplayName("endereco da unidade e unico por oficina, nao globalmente")
  void enderecoDaUnidadeEUnicoPorOficina() throws SQLException {
    try (Connection con = conectar();
        Statement st = con.createStatement()) {
      long oficinaA = inserirOficina(st, "11111111000101");
      long oficinaB = inserirOficina(st, "22222222000102");

      inserirUnidade(st, oficinaA, "Rua das Flores, 100");
      inserirUnidade(st, oficinaB, "Rua das Flores, 100");

      assertThatThrownBy(() -> inserirUnidade(st, oficinaA, "Rua das Flores, 100"))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  @DisplayName("cnpj da oficina e unico globalmente")
  void cnpjDaOficinaEUnico() throws SQLException {
    try (Connection con = conectar();
        Statement st = con.createStatement()) {
      inserirOficina(st, "33333333000103");

      assertThatThrownBy(() -> inserirOficina(st, "33333333000103"))
          .isInstanceOf(SQLException.class);
    }
  }

  private static long inserirPessoa(Statement st, String nome) throws SQLException {
    st.executeUpdate("INSERT INTO pessoa (nome) VALUES ('%s')".formatted(nome));
    return ultimoId(st, "pessoa");
  }

  private static void inserirUsuario(Statement st, long pessoaId, String username, String role)
      throws SQLException {
    String sql =
        "INSERT INTO usuario (pessoa_id, username, password, role) VALUES (%d, '%s', 'x', '%s')";
    st.executeUpdate(sql.formatted(pessoaId, username, role));
  }

  private static long inserirOficina(Statement st, String cnpj) throws SQLException {
    st.executeUpdate("INSERT INTO oficina (nome, cnpj) VALUES ('Oficina', '%s')".formatted(cnpj));
    return ultimoId(st, "oficina");
  }

  private static void inserirUnidade(Statement st, long oficinaId, String endereco)
      throws SQLException {
    String sql = "INSERT INTO unidade (oficina_id, nome, endereco) VALUES (%d, 'Unidade', '%s')";
    st.executeUpdate(sql.formatted(oficinaId, endereco));
  }

  private static long ultimoId(Statement st, String tabela) throws SQLException {
    try (ResultSet rs = st.executeQuery("SELECT MAX(id) FROM " + tabela)) {
      rs.next();
      return rs.getLong(1);
    }
  }
}
