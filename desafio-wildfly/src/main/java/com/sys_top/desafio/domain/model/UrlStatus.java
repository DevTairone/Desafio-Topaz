package com.sys_top.desafio.domain.model;

/**
 * Estados possíveis de uma URL encurtada.
 */
public enum UrlStatus {

    /** URL ativa e disponível para redirecionamento. */
    ACTIVE,

    /** URL expirada (passou da data de expiração definida). */
    EXPIRED,

    /** URL desativada manualmente, não deve mais redirecionar. */
    DISABLED
}
