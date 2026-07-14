package com.sys_top.desafio.web;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.engine.UrlGenerationEngine;
import com.sys_top.desafio.service.UrlValidator;
import com.sys_top.desafio.web.dto.ShortenUrlRequest;
import com.sys_top.desafio.web.dto.ShortenUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Endpoint responsável por receber a URL original e retornar a URL encurtada.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlGenerationEngine urlGenerationEngine;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shorten(@RequestBody ShortenUrlRequest request) {
        UrlValidator.validate(request.getUrl());

        ShortUrl shortUrl = urlGenerationEngine.generate(request.getUrl());

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String fullShortUrl = baseUrl + "/" + shortUrl.getShortCode();

        ShortenUrlResponse response = ShortenUrlResponse.builder()
                .shortUrl(fullShortUrl)
                .shortCode(shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .createdAt(shortUrl.getCreatedAt())
                .expiresAt(shortUrl.getExpiresAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create(fullShortUrl))
                .body(response);
    }
}
