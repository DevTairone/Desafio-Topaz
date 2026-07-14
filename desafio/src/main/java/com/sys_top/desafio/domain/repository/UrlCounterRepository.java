package com.sys_top.desafio.domain.repository;

import com.sys_top.desafio.domain.model.UrlCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositório de acesso ao contador global de geração de códigos curtos.
 */
public interface UrlCounterRepository extends JpaRepository<UrlCounter, Long> {

    /**
     * Busca o contador aplicando lock pessimista de escrita (equivalente a
     * um SELECT ... FOR UPDATE), garantindo leitura + incremento atômicos
     * mesmo que múltiplas instâncias da aplicação concorram pelo mesmo
     * contador no banco.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from UrlCounter c where c.id = :id")
    Optional<UrlCounter> findByIdForUpdate(Long id);
}
