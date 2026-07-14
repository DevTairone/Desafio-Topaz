package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.application.UrlRedirectService;
import com.sys_top.desafio.domain.exception.ShortUrlExpiredException;
import com.sys_top.desafio.domain.exception.ShortUrlNotFoundException;
import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedirectControllerTest {

    private UrlRedirectService urlRedirectService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        urlRedirectService = mock(UrlRedirectService.class);
        RedirectController controller = new RedirectController(urlRedirectService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveRedirecionarParaUrlOriginal() throws Exception {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://exemplo.com/pagina")
                .status(UrlStatus.ACTIVE)
                .accessCount(0L)
                .build();
        when(urlRedirectService.resolve("abc123")).thenReturn(shortUrl);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://exemplo.com/pagina"));
    }

    @Test
    void deveRetornar404QuandoCodigoNaoExistir() throws Exception {
        when(urlRedirectService.resolve("naoexiste")).thenThrow(new ShortUrlNotFoundException("naoexiste"));

        mockMvc.perform(get("/naoexiste"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar410QuandoUrlExpirada() throws Exception {
        when(urlRedirectService.resolve("expirado")).thenThrow(new ShortUrlExpiredException("expirado"));

        mockMvc.perform(get("/expirado"))
                .andExpect(status().isGone());
    }
}
