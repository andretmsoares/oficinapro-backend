package com.oficinapro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI oficinaProOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("OficinaPro API")
                .description(
                    """
                                API REST para gerenciamento de oficinas mecânicas.

                                A API permite o gerenciamento de oficinas,
                                unidades, clientes, veículos, ordens de serviço
                                e demais recursos do sistema OficinaPro.

                                ## Autenticação

                                Obtenha um token em `POST /api/auth/login` e informe-o
                                no botão *Authorize* (apenas o token, sem o prefixo `Bearer`).

                                Cargos:
                                - `ADMIN`: administrador do SaaS, sem vínculo com oficina.
                                  Gerencia oficinas e contas de usuário de qualquer oficina,
                                  mas **não** acessa dados operacionais (clientes, veículos,
                                  ordens de serviço, peças, mão de obra e pagamentos).
                                - `GERENTE`: administrador de uma oficina; cria `GERENTE` e
                                  `MECANICO` e só acessa dados da sua própria oficina.
                                - `MECANICO`: atua nas ordens de serviço da sua oficina.
                                  Não gerencia usuários, não acessa o financeiro e não pode
                                  finalizar, entregar ou cancelar uma OS.
                                """)
                .version("1.0.0")
                .contact(new Contact().name("OficinaPro")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token JWT obtido em POST /api/auth/login")))

        // Aplica o cadeado a todos os endpoints. Rotas públicas (ex.: login)
        // se desmarcam com @SecurityRequirements no controller.
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
