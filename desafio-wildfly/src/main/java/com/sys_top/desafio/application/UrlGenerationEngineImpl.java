package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.exception.UrlGenerationException;
import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.domain.model.UrlStatus;
import com.sys_top.desafio.infrastructure.persistence.ShortUrlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
@ApplicationScoped
public class UrlGenerationEngineImpl implements UrlGenerationEngine {

    private static final Logger LOG = Logger.getLogger(UrlGenerationEngineImpl.class.getName());
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private ShortUrlDao shortUrlDao;
    private UrlCounterService urlCounterService;

    /** Único worker thread: é ele que serializa todo o processamento de geração. */
    private ExecutorService executor;

    /** Construtor exigido pelo CDI para a criação do proxy do bean. */
    protected UrlGenerationEngineImpl() {
    }

    @Inject
    public UrlGenerationEngineImpl(ShortUrlDao shortUrlDao, UrlCounterService urlCounterService) {
        this.shortUrlDao = shortUrlDao;
        this.urlCounterService = urlCounterService;
    }

    @PostConstruct
    void start() {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "url-generation-engine");
                thread.setDaemon(true);
                return thread;
            }
        });
        LOG.info("Motor de geração de URLs iniciado (processamento serializado, thread única)");
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
    public ShortUrl generate(final String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new UrlGenerationException("A URL original não pode ser vazia");
        }

        Callable<ShortUrl> task = new Callable<ShortUrl>() {
            @Override
            public ShortUrl call() {
                return processGeneration(originalUrl.trim());
            }
        };

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
        Optional<ShortUrl> existing = shortUrlDao.findByOriginalUrl(originalUrl);
        if (existing.isPresent() && existing.get().isRedirectable()) {
            LOG.fine("URL já encurtada anteriormente, reaproveitando código: " + existing.get().getShortCode());
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

        shortUrl = shortUrlDao.save(shortUrl);

        LOG.info("Código curto gerado: " + code + " -> " + originalUrl);
        return shortUrl;
    }
}
