package com.sys_top.desafio.service;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UrlRedirectServiceTest {

    private ShortUrlRepository repository;
    private UrlRedirectService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        service = new UrlRedirectService(repository);
    }

    @Test
    void deveResolverUrlAtivaEIncrementarAcessos() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://exemplo.com/pagina")
                .status(UrlStatus.ACTIVE)
                .accessCount(0L)
                .build();
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl resolvido = service.resolve("abc123");

        assertEquals("https://exemplo.com/pagina", resolvido.getOriginalUrl());
        assertEquals(1L, resolvido.getAccessCount());
    }

    @Test
    void deveLancarNotFoundQuandoCodigoNaoExistir() {
        when(repository.findByShortCode("naoexiste")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> service.resolve("naoexiste"));
    }

    @Test
    void deveLancarExpiredQuandoStatusForDisabled() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("desativado")
                .originalUrl("https://exemplo.com")
                .status(UrlStatus.DISABLED)
                .accessCount(0L)
                .build();
        when(repository.findByShortCode("desativado")).thenReturn(Optional.of(shortUrl));

        assertThrows(ShortUrlExpiredException.class, () -> service.resolve("desativado"));
    }
}
