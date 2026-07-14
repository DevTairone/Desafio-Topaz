package com.sys_top.desafio.service;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve um código curto para a URL original, validando disponibilidade
 * (existência, expiração e status) e registrando o acesso.
 */
@Service
@RequiredArgsConstructor
public class UrlRedirectService {

    private final ShortUrlRepository shortUrlRepository;

    @Transactional
    public ShortUrl resolve(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (!shortUrl.isRedirectable()) {
            throw new ShortUrlExpiredException(shortCode);
        }

        shortUrl.incrementAccessCount();
        return shortUrlRepository.save(shortUrl);
    }
}
