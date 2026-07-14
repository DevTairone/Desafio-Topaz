package com.sys_top.desafio.service;

/**
 * Lançada quando o código curto existe, porém a URL está expirada ou
 * desativada e não deve mais redirecionar.
 */
public class ShortUrlExpiredException extends RuntimeException {

    public ShortUrlExpiredException(String shortCode) {
        super("URL encurtada expirada ou desativada: " + shortCode);
    }
}
