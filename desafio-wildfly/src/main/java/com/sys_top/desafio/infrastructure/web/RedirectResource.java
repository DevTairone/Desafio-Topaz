package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.application.UrlRedirectService;
import com.sys_top.desafio.domain.model.ShortUrl;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Recurso JAX-RS responsável por redirecionar um código curto para a URL
 * original. Equivalente ao RedirectController da versão Spring Boot.
 *
 * A restrição de regex no path ({@code [0-9A-Za-z]+}) evita que este
 * recurso "capture" outras rotas de um único segmento.
 */
@Path("/{code: [0-9A-Za-z]+}")
public class RedirectResource {

    private UrlRedirectService urlRedirectService;

    /** Construtor exigido pelo CDI para a criação do proxy do bean. */
    protected RedirectResource() {
    }

    @Inject
    public RedirectResource(UrlRedirectService urlRedirectService) {
        this.urlRedirectService = urlRedirectService;
    }

    @GET
    public Response redirect(@PathParam("code") String code) {
        ShortUrl shortUrl = urlRedirectService.resolve(code);

        return Response.status(Response.Status.FOUND)
                .location(URI.create(shortUrl.getOriginalUrl()))
                .build();
    }
}
