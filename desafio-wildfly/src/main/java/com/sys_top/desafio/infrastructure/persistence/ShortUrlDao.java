package com.sys_top.desafio.infrastructure.persistence;

import com.sys_top.desafio.domain.model.ShortUrl;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Optional;

/**
 * Acesso a dados de {@link ShortUrl} via JPA puro (EntityManager).
 *
 * Substitui o Spring Data JPA (não disponível neste stack: WildFly 10 /
 * Java EE 7), expondo as mesmas operações que a camada de aplicação precisa.
 */
@ApplicationScoped
public class ShortUrlDao {

    @PersistenceContext(unitName = "desafioPU")
    private EntityManager entityManager;

    /** Busca a URL encurtada pelo código curto (usado no redirecionamento). */
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        TypedQuery<ShortUrl> query = entityManager.createQuery(
                "select s from ShortUrl s where s.shortCode = :shortCode", ShortUrl.class);
        query.setParameter("shortCode", shortCode);
        return singleResult(query);
    }

    /** Busca por URL original, usado para reaproveitar código já gerado. */
    public Optional<ShortUrl> findByOriginalUrl(String originalUrl) {
        TypedQuery<ShortUrl> query = entityManager.createQuery(
                "select s from ShortUrl s where s.originalUrl = :originalUrl", ShortUrl.class);
        query.setParameter("originalUrl", originalUrl);
        return singleResult(query);
    }

    /** Verifica rapidamente a existência de um código, sem carregar a entidade. */
    public boolean existsByShortCode(String shortCode) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(s) from ShortUrl s where s.shortCode = :shortCode", Long.class);
        query.setParameter("shortCode", shortCode);
        return query.getSingleResult() > 0;
    }

    /** Persiste (insert) ou atualiza (update) conforme a entidade já possua id ou não. */
    public ShortUrl save(ShortUrl shortUrl) {
        if (shortUrl.getId() == null) {
            entityManager.persist(shortUrl);
            return shortUrl;
        }
        return entityManager.merge(shortUrl);
    }

    /** Remove todos os registros — usado em testes para isolar cada caso. */
    public void deleteAll() {
        entityManager.createQuery("delete from ShortUrl").executeUpdate();
    }

    private Optional<ShortUrl> singleResult(TypedQuery<ShortUrl> query) {
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
