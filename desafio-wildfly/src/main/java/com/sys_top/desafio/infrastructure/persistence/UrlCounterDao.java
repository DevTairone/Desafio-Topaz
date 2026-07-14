package com.sys_top.desafio.infrastructure.persistence;

import com.sys_top.desafio.domain.model.UrlCounter;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.util.Optional;

/**
 * Acesso a dados de {@link UrlCounter} via JPA puro (EntityManager).
 *
 * Inclui o lock pessimista de escrita usado pelo UrlCounterService para
 * incrementar o contador de forma atômica também no nível do banco.
 */
@ApplicationScoped
public class UrlCounterDao {

    @PersistenceContext(unitName = "desafioPU")
    private EntityManager entityManager;

    public boolean existsById(Long id) {
        return entityManager.find(UrlCounter.class, id) != null;
    }

    public UrlCounter save(UrlCounter counter) {
        if (entityManager.find(UrlCounter.class, counter.getId()) == null) {
            entityManager.persist(counter);
            return counter;
        }
        return entityManager.merge(counter);
    }

    /**
     * Busca o contador aplicando lock pessimista de escrita (equivalente a
     * um SELECT ... FOR UPDATE), garantindo leitura + incremento atômicos
     * mesmo que múltiplas instâncias da aplicação concorram pelo mesmo
     * contador no banco.
     */
    public Optional<UrlCounter> findByIdForUpdate(Long id) {
        UrlCounter counter = entityManager.find(UrlCounter.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(counter);
    }
}
