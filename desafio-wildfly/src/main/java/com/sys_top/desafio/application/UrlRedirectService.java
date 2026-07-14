package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.exception.ShortUrlExpiredException;
import com.sys_top.desafio.domain.exception.ShortUrlNotFoundException;
import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.infrastructure.persistence.ShortUrlDao;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * Resolve um código curto para a URL original, validando disponibilidade
 * (existência, expiração e status) e registrando o acesso.
 */
@ApplicationScoped
public class UrlRedirectService {

    private ShortUrlDao shortUrlDao;

    /** Construtor exigido pelo CDI para a criação do proxy do bean. */
    protected UrlRedirectService() {
    }

    @Inject
    public UrlRedirectService(ShortUrlDao shortUrlDao) {
        this.shortUrlDao = shortUrlDao;
    }

    @Transactional
    public ShortUrl resolve(String shortCode) {
        ShortUrl shortUrl = shortUrlDao.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (!shortUrl.isRedirectable()) {
            throw new ShortUrlExpiredException(shortCode);
        }

        shortUrl.incrementAccessCount();
        return shortUrlDao.save(shortUrl);
    }
}
