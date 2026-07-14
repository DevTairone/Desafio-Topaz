package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.model.UrlCounter;
import com.sys_top.desafio.domain.repository.UrlCounterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de persistência do contador utilizado pelo motor de geração.
 *
 * Complementa a serialização em memória do {@link UrlGenerationEngineImpl}
 * (executor de thread única): aqui a leitura + incremento do contador é
 * feita dentro de uma transação com lock pessimista de escrita, garantindo
 * atomicidade também no nível do banco — relevante caso a aplicação venha a
 * rodar futuramente com múltiplas instâncias.
 */
@Component
@RequiredArgsConstructor
public class UrlCounterService {

    private static final Long COUNTER_ID = 1L;

    private final UrlCounterRepository urlCounterRepository;

    /** Garante que a linha única do contador exista antes de qualquer incremento. */
    @PostConstruct
    public void ensureCounterExists() {
        if (!urlCounterRepository.existsById(COUNTER_ID)) {
            urlCounterRepository.save(UrlCounter.builder()
                    .id(COUNTER_ID)
                    .currentValue(0L)
                    .build());
        }
    }

    /** Incrementa e retorna o próximo valor do contador, de forma atômica. */
    @Transactional
    public long nextValue() {
        UrlCounter counter = urlCounterRepository.findByIdForUpdate(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Contador de URLs não inicializado"));

        long next = counter.getCurrentValue() + 1;
        counter.setCurrentValue(next);
        urlCounterRepository.save(counter);
        return next;
    }
}
