package com.sys_top.desafio.engine;

import com.sys_top.desafio.domain.model.ShortUrl;

/**
 * Motor de geração de URLs encurtadas.
 *
 * Contrato: implementações devem garantir que apenas UMA requisição de
 * geração seja processada por vez, de forma sincronizada/serializada,
 * independentemente de quantas chamadas concorrentes cheguem a
 * {@link #generate(String)}.
 */
public interface UrlGenerationEngine {

    /**
     * Gera (ou reaproveita, se já existir) o código curto para a URL informada.
     *
     * @param originalUrl URL original a ser encurtada
     * @return a entidade {@link ShortUrl} persistida, já com o código curto definido
     * @throws UrlGenerationException em caso de falha no processamento
     */
    ShortUrl generate(String originalUrl);
}
