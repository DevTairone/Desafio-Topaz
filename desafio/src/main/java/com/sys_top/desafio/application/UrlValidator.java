package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.exception.InvalidUrlException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validação da URL informada pelo usuário antes de ser encaminhada ao
 * motor de geração.
 */
public final class UrlValidator {

    private UrlValidator() {
    }

    /**
     * Valida que a URL não é vazia, é sintaticamente válida e usa o
     * protocolo http ou https.
     *
     * @throws InvalidUrlException se a URL for inválida
     */
    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("A URL não pode ser vazia");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("A URL informada é inválida: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("A URL deve usar o protocolo http ou https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("A URL informada é inválida: " + url);
        }
    }
}
