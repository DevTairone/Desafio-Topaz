package com.sys_top.desafio.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Resposta retornada após o encurtamento de uma URL.
 *
 * Datas expostas como String (ISO-8601) em vez de LocalDateTime: evita
 * depender do módulo Jackson JSR-310 (jackson-datatype-jsr310), que não
 * vem registrado por padrão no provider JSON embutido do WildFly 10.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlResponse {

    private String shortUrl;
    private String shortCode;
    private String originalUrl;
    private String createdAt;
    private String expiresAt;
}
