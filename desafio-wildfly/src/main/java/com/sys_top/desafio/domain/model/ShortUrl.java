package com.sys_top.desafio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Representa uma URL original e o seu respectivo código encurtado.
 *
 * O identificador (id) é gerado pelo banco (sequência/auto-incremento) e é
 * apenas a chave técnica da linha; o código curto (shortCode) é calculado
 * pelo motor de geração a partir de um contador dedicado (ver UrlCounter),
 * não a partir deste id.
 */
@Entity
@Table(name = "short_url", uniqueConstraints = @UniqueConstraint(name = "uk_short_url_code", columnNames = "short_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código curto exposto na URL encurtada (ex: "aZ3kT9"). */
    @Column(name = "short_code", unique = true, length = 10)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private Long accessCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UrlStatus status = UrlStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UrlStatus.ACTIVE;
        }
        if (accessCount == null) {
            accessCount = 0L;
        }
    }

    /** Verifica se a URL já passou da data de expiração configurada. */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /** Verifica se a URL pode ser usada para redirecionamento. */
    public boolean isRedirectable() {
        return status == UrlStatus.ACTIVE && !isExpired();
    }

    /** Incrementa o contador de acessos (chamado a cada redirecionamento). */
    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null ? 0L : this.accessCount) + 1;
    }
}
