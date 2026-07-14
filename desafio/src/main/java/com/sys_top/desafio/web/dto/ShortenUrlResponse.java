package com.sys_top.desafio.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resposta retornada após o encurtamento de uma URL.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlResponse {

    private String shortUrl;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
