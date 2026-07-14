package com.sys_top.desafio.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados exibidos na documentação Swagger/OpenAPI da API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI desafioOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Encurtador de URLs - API")
                        .description("API REST para encurtamento de URLs e redirecionamento pelo código curto gerado.")
                        .version("v1"));
    }
}
