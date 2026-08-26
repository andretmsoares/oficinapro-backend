package com.oficinapro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oficinaProOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OficinaPro API")
                        .description("""
                                API REST para gerenciamento de oficinas mecânicas.

                                A API permite o gerenciamento de oficinas,
                                unidades, clientes, veículos, ordens de serviço
                                e demais recursos do sistema OficinaPro.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OficinaPro")));
    }
}