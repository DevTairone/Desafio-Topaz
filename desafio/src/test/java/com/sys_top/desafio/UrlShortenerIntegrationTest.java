package com.sys_top.desafio;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import com.sys_top.desafio.web.dto.ShortenUrlRequest;
import com.sys_top.desafio.web.dto.ShortenUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste de integração ponta a ponta: sobe o contexto real da aplicação
 * (servidor embutido + banco H2 do perfil "dev") e exercita o fluxo
 * completo via HTTP, sem mocks — validando o encadeamento real entre
 * controllers, motor de geração, contador e persistência.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class UrlShortenerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @BeforeEach
    void limparBanco() {
        shortUrlRepository.deleteAll();
    }

    @Test
    void fluxoCompletoDeEncurtarERedirecionar() {
        ShortenUrlResponse encurtada = encurtar("https://exemplo.com/pagina-de-teste");

        assertNotNull(encurtada.getShortCode());
        assertEquals("https://exemplo.com/pagina-de-teste", encurtada.getOriginalUrl());

        ResponseEntity<Void> redirect = restTemplate.getForEntity("/" + encurtada.getShortCode(), Void.class);

        assertEquals(HttpStatus.FOUND, redirect.getStatusCode());
        assertEquals("https://exemplo.com/pagina-de-teste", redirect.getHeaders().getLocation().toString());
    }

    @Test
    void deveReaproveitarCodigoParaMesmaUrlOriginal() {
        ShortenUrlResponse primeira = encurtar("https://exemplo.com/repetida");
        ShortenUrlResponse segunda = encurtar("https://exemplo.com/repetida");

        assertEquals(primeira.getShortCode(), segunda.getShortCode());
    }

    @Test
    void deveRetornar400ParaUrlInvalida() {
        ShortenUrlRequest request = new ShortenUrlRequest("ftp://invalido");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/shorten", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deveRetornar404ParaCodigoInexistente() {
        ResponseEntity<String> response = restTemplate.getForEntity("/codigo-que-nao-existe", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deveIncrementarContagemDeAcessosAposRedirecionar() {
        ShortenUrlResponse encurtada = encurtar("https://exemplo.com/contagem");

        restTemplate.getForEntity("/" + encurtada.getShortCode(), Void.class);
        restTemplate.getForEntity("/" + encurtada.getShortCode(), Void.class);

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

    private ShortenUrlResponse encurtar(String url) {
        ShortenUrlRequest request = new ShortenUrlRequest(url);
        ResponseEntity<ShortenUrlResponse> response =
                restTemplate.postForEntity("/api/shorten", request, ShortenUrlResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }
}
