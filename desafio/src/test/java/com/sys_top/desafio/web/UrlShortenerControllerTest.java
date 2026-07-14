package com.sys_top.desafio.web;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import com.sys_top.desafio.engine.UrlGenerationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UrlShortenerControllerTest {

    private UrlGenerationEngine urlGenerationEngine;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        urlGenerationEngine = mock(UrlGenerationEngine.class);
        UrlShortenerController controller = new UrlShortenerController(urlGenerationEngine);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveEncurtarUmaUrlValida() throws Exception {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("1")
                .originalUrl("https://exemplo.com")
                .status(UrlStatus.ACTIVE)
                .accessCount(0L)
                .build();
        when(urlGenerationEngine.generate("https://exemplo.com")).thenReturn(shortUrl);

        mockMvc.perform(post("/api/shorten")
                        .contentType(APPLICATION_JSON)
                        .content("{\"url\":\"https://exemplo.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("1"))
                .andExpect(jsonPath("$.originalUrl").value("https://exemplo.com"));
    }

    @Test
    void deveRejeitarUrlInvalidaComBadRequest() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(APPLICATION_JSON)
                        .content("{\"url\":\"ftp://invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarUrlVaziaComBadRequest() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
