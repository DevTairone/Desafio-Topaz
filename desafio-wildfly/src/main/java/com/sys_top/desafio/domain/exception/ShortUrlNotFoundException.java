package com.sys_top.desafio.domain.exception;

/**
 * Lançada quando não existe nenhuma URL encurtada para o código informado.
 */
public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String shortCode) {
        super("Código curto não encontrado: " + shortCode);
    }
}
