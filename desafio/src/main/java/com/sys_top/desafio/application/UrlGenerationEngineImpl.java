package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.exception.UrlGenerationException;
import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import com.sys_top.desafio.domain.repository.ShortUrlRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementação do motor de geração baseada em um executor de thread única.
 *
 * Toda chamada a {@link #generate(String)} é despachada como uma tarefa para
 * uma fila (executor com exatamente uma worker thread) e bloqueia (get) até
 * a conclusão do seu processamento. Isso garante:
 *
 * - Execução estritamente sequencial (uma requisição por vez), mesmo que
 *   múltiplas threads HTTP chamem o método simultaneamente;
 * - Ordem de chegada (FIFO) no processamento;
 * - Ausência de condição de corrida na leitura/gravação do contador que
 *   serve de base para o código curto (ver {@link UrlCounterService}, que
 *   reforça a atomicidade também no nível do banco via lock pessimista).
 */
@Slf4j
@Component
public class UrlGenerationEngineImpl implements UrlGenerationEngine {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final UrlCounterService urlCounterService;

    /** Único worker thread: é ele que serializa todo o processamento de geração. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "url-generation-engine");
        thread.setDaemon(true);
        return thread;
    });

    public UrlGenerationEngineImpl(ShortUrlRepository shortUrlRepository, UrlCounterService urlCounterService) {
        this.shortUrlRepository = shortUrlRepository;
        this.urlCounterService = urlCounterService;
        log.info("Motor de geração de URLs iniciado (processamento serializado, thread única)");
    }

    @PreDestroy
    void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public ShortUrl generate(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new UrlGenerationException("A URL original não pode ser vazia");
        }

        Callable<ShortUrl> task = () -> processGeneration(originalUrl.trim());

        try {
            return executor.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UrlGenerationException("Geração interrompida", e);
        } catch (ExecutionException e) {
            throw new UrlGenerationException("Falha ao gerar URL encurtada", e.getCause());
        }
    }

    /**
     * Executado sempre pela mesma (e única) worker thread do executor —
     * nunca em paralelo com outra chamada a este método.
     */
    private ShortUrl processGeneration(String originalUrl) {
        Optional<ShortUrl> existing = shortUrlRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent() && existing.get().isRedirectable()) {
            log.debug("URL já encurtada anteriormente, reaproveitando código: {}", existing.get().getShortCode());
            return existing.get();
        }

        // o valor do contador (persistido de forma atômica) é a base da codificação
        // base62 — independente do id técnico da linha em short_url
        long sequence = urlCounterService.nextValue();
        String code = Base62Encoder.encode(sequence);

        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode(code)
                .originalUrl(originalUrl)
                .status(UrlStatus.ACTIVE)
                .accessCount(0L)
                .build();

        shortUrl = shortUrlRepository.save(shortUrl);

        log.info("Código curto gerado: {} -> {}", code, originalUrl);
        return shortUrl;
    }
}
