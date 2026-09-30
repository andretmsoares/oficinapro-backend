package com.oficinapro;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OficinaProApplication {

  /**
   * Fuso de negócio. As datas do sistema são {@code LocalDateTime} (sem fuso), então o relógio
   * "de parede" do servidor é o que o usuário vê. Em container o fuso padrão da JVM é UTC, o que
   * deixava abertura/fechamento de OS e registros de pagamento 3h adiantados.
   */
  public static final String FUSO_NEGOCIO = "America/Sao_Paulo";

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone(FUSO_NEGOCIO));
    SpringApplication.run(OficinaProApplication.class, args);
  }
}
