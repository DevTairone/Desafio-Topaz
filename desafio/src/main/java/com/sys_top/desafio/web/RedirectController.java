package com.sys_top.desafio.web;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.service.UrlRedirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoint responsável por redirecionar um código curto para a URL original.
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlRedirectService urlRedirectService;

    @GetMapping("/{code:[0-9A-Za-z]+}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        ShortUrl shortUrl = urlRedirectService.resolve(code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortUrl.getOriginalUrl()))
                .build();
    }
}
