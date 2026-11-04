package com.bevans.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components())
            .info(new Info()
                .title("API Rota Estudantil")
                .version("1.0.0")
                .description("Documentação oficial do motor do aplicativo. Infraestrutura completa de rotas e presenças.")
                .contact(new Contact().name("Miguel Petherson")))

            .tags(List.of(
                new Tag().name("1. Gestão de Usuários").description("Operações de Motoristas e Passageiros"),
                new Tag().name("2. Gestão de Rotas").description("Operações de presença e otimização logística"),
                new Tag().name("3. Gestão de Presenças").description("Operações para controle de embarque diário"),
                new Tag().name("4. Autenticação").description("Segurança e acesso ao sistema")
            ));
    }
}