package com.sys_top.desafio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa uma URL original e o seu respectivo código encurtado.
 *
 * O identificador (id) é gerado pelo banco (sequência/auto-incremento) e serve
 * de base para a geração do código curto (short code) via encoding base62,
 * garantindo unicidade sem a necessidade de verificação de colisão.
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

    /**
     * Código curto exposto na URL encurtada (ex: "aZ3kT9").
     * Preenchido pelo motor de geração após a persistência do id.
     */
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
