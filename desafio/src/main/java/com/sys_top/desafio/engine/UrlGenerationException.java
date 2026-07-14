package com.sys_top.desafio.engine;

/**
 * Erro ocorrido durante a geração de um código curto.
 */
public class UrlGenerationException extends RuntimeException {

    public UrlGenerationException(String message) {
        super(message);
    }

    public UrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
