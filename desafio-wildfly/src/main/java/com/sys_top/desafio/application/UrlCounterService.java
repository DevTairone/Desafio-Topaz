package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.model.UrlCounter;
import com.sys_top.desafio.infrastructure.persistence.UrlCounterDao;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * Camada de persistência do contador utilizado pelo motor de geração.
 *
 * Complementa a serialização em memória do {@link UrlGenerationEngineImpl}
 * (executor de thread única): aqui a leitura + incremento do contador é
 * feita dentro de uma transação (JTA) com lock pessimista de escrita,
 * garantindo atomicidade também no nível do banco — relevante caso a
 * aplicação venha a rodar futuramente com múltiplas instâncias.
 */
@ApplicationScoped
public class UrlCounterService {

    private static final Long COUNTER_ID = 1L;

    private UrlCounterDao urlCounterDao;

    /** Construtor exigido pelo CDI para a criação do proxy do bean. */
    protected UrlCounterService() {
    }

    @Inject
    public UrlCounterService(UrlCounterDao urlCounterDao) {
        this.urlCounterDao = urlCounterDao;
    }

    /** Garante que a linha única do contador exista antes de qualquer incremento. */
    @PostConstruct
    @Transactional
    public void ensureCounterExists() {
        if (!urlCounterDao.existsById(COUNTER_ID)) {
            urlCounterDao.save(UrlCounter.builder()
                    .id(COUNTER_ID)
                    .currentValue(0L)
                    .build());
        }
    }

    /** Incrementa e retorna o próximo valor do contador, de forma atômica. */
    @Transactional
    public long nextValue() {
        UrlCounter counter = urlCounterDao.findByIdForUpdate(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Contador de URLs não inicializado"));

        long next = counter.getCurrentValue() + 1;
        counter.setCurrentValue(next);
        urlCounterDao.save(counter);
        return next;
    }
}
