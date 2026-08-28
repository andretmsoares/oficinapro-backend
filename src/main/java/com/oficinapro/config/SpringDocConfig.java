package com.oficinapro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public org.springdoc.core.customizers.OperationCustomizer customizeOperation() {
        return (operation, handlerMethod) -> operation;
    }
}