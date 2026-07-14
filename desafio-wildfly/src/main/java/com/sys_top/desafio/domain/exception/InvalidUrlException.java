package com.sys_top.desafio.domain.exception;

/**
 * Lançada quando a URL informada pelo usuário é inválida (vazia,
 * malformada ou com protocolo não suportado).
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}
