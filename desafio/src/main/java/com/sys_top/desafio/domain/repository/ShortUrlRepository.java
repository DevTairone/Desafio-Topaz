package com.sys_top.desafio.domain.repository;

import com.sys_top.desafio.domain.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<ShortUrl> findByOriginalUrl(String originalUrl);
}
