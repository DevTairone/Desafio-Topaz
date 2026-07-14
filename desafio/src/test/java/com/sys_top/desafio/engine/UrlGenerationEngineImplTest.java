package com.sys_top.desafio.engine;

import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UrlGenerationEngineImplTest {

    private ShortUrlRepository repository;
    private UrlGenerationEngineImpl engine;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        engine = new UrlGenerationEngineImpl(repository);
    }

    @AfterEach
    void tearDown() {
        engine.stop();
    }

    @Test
    void deveProcessarApenasUmaRequisicaoPorVezDeFormaSerializada() throws Exception {
        AtomicLong idSequence = new AtomicLong(0);
        AtomicInteger emExecucao = new AtomicInteger(0);
        AtomicInteger picoDeExecucaoSimultanea = new AtomicInteger(0);

        when(repository.findByOriginalUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            int atual = emExecucao.incrementAndGet();
            picoDeExecucaoSimultanea.updateAndGet(max -> Math.max(max, atual));
            try {
                // simula trabalho de persistência, expondo eventual paralelismo indevido
                Thread.sleep(20);
                ShortUrl shortUrl = invocation.getArgument(0);
                if (shortUrl.getId() == null) {
                    shortUrl.setId(idSequence.incrementAndGet());
                }
                return shortUrl;
            } finally {
                emExecucao.decrementAndGet();
            }
        });

        int totalRequisicoes = 20;
        ExecutorService clientePool = Executors.newFixedThreadPool(totalRequisicoes);
        try {
            List<Future<ShortUrl>> futuros = IntStream.range(0, totalRequisicoes)
                    .mapToObj(i -> clientePool.submit(() -> engine.generate("https://exemplo.com/pagina-" + i)))
                    .toList();

            for (Future<ShortUrl> futuro : futuros) {
                assertNotNull(futuro.get(5, TimeUnit.SECONDS));
            }
        } finally {
            clientePool.shutdown();
        }

        assertEquals(1, picoDeExecucaoSimultanea.get(),
                "o motor de geração não deve processar mais de uma requisição por vez");
    }

    @Test
    void deveReaproveitarCodigoQuandoUrlOriginalJaExistir() {
        ShortUrl existente = ShortUrl.builder()
                .id(1L)
                .shortCode("A1")
                .originalUrl("https://exemplo.com")
                .status(UrlStatus.ACTIVE)
                .accessCount(0L)
                .build();

        when(repository.findByOriginalUrl("https://exemplo.com")).thenReturn(Optional.of(existente));

        ShortUrl resultado = engine.generate("https://exemplo.com");

        assertEquals("A1", resultado.getShortCode());
    }
}
