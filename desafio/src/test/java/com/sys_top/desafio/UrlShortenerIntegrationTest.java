package com.sys_top.desafio;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import com.sys_top.desafio.infrastructure.web.GlobalExceptionHandler;
import com.sys_top.desafio.infrastructure.web.RedirectController;
import com.sys_top.desafio.infrastructure.web.UrlShortenerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração ponta a ponta: sobe o contexto real da aplicação
 * (perfil "dev", banco H2) com os beans reais (motor de geração, contador,
 * repositórios) e exercita os controllers reais via MockMvc — sem mocks —
 * validando o encadeamento completo entre as camadas.
 */
@SpringBootTest
@ActiveProfiles("dev")
class UrlShortenerIntegrationTest {

    @Autowired
    private UrlShortenerController urlShortenerController;

    @Autowired
    private RedirectController redirectController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shortUrlRepository.deleteAll();
        mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController, redirectController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    void fluxoCompletoDeEncurtarERedirecionar() throws Exception {
        ShortUrl encurtada = encurtar("https://exemplo.com/pagina-de-teste");

        mockMvc.perform(get("/" + encurtada.getShortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://exemplo.com/pagina-de-teste"));
    }

    @Test
    void deveReaproveitarCodigoParaMesmaUrlOriginal() throws Exception {
        ShortUrl primeira = encurtar("https://exemplo.com/repetida");
        ShortUrl segunda = encurtar("https://exemplo.com/repetida");

        assertEquals(primeira.getShortCode(), segunda.getShortCode());
    }

    @Test
    void deveRetornar400ParaUrlInvalida() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(APPLICATION_JSON)
                        .content("{\"url\":\"ftp://invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404ParaCodigoInexistente() throws Exception {
        // precisa ser alfanumerico para bater na rota de redirecionamento ({code:[0-9A-Za-z]+})
        mockMvc.perform(get("/codigoQueNaoExiste123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveIncrementarContagemDeAcessosAposRedirecionar() throws Exception {
        ShortUrl encurtada = encurtar("https://exemplo.com/contagem");

        mockMvc.perform(get("/" + encurtada.getShortCode()));
        mockMvc.perform(get("/" + encurtada.getShortCode()));

        ShortUrl atualizado = shortUrlRepository.findByShortCode(encurtada.getShortCode()).orElseThrow();
        assertEquals(2L, atualizado.getAccessCount());
    }

    @Test
    void deveGerarCodigosUnicosSobConcorrenciaReal() throws Exception {
        int totalRequisicoes = 30;
        ExecutorService clientePool = Executors.newFixedThreadPool(totalRequisicoes);
        try {
            List<Future<String>> futuros = IntStream.range(0, totalRequisicoes)
                    .mapToObj(i -> clientePool.submit(() -> encurtar("https://exemplo.com/concorrencia-" + i).getShortCode()))
                    .toList();

            Set<String> codigos = new HashSet<>();
            for (Future<String> futuro : futuros) {
                codigos.add(futuro.get(15, TimeUnit.SECONDS));
            }

            assertEquals(totalRequisicoes, codigos.size(), "todos os codigos gerados devem ser unicos");
        } finally {
            clientePool.shutdown();
        }
    }

    /** Encurta a URL via HTTP real (MockMvc) e retorna a entidade persistida no banco. */
    private ShortUrl encurtar(String url) {
        try {
            mockMvc.perform(post("/api/shorten")
                            .contentType(APPLICATION_JSON)
                            .content("{\"url\":\"" + url + "\"}"))
                    .andExpect(status().isCreated());

            return shortUrlRepository.findByOriginalUrl(url).orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
