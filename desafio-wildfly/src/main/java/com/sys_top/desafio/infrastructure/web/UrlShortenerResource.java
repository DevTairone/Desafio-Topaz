package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.application.UrlGenerationEngine;
import com.sys_top.desafio.application.UrlValidator;
import com.sys_top.desafio.domain.model.ShortUrl;
import com.sys_top.desafio.infrastructure.web.dto.ShortenUrlRequest;
import com.sys_top.desafio.infrastructure.web.dto.ShortenUrlResponse;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.format.DateTimeFormatter;

/**
 * Recurso JAX-RS responsável por receber a URL original e retornar a URL
 * encurtada. Equivalente ao UrlShortenerController da versão Spring Boot.
 */
@Path("/api/shorten")
public class UrlShortenerResource {

    private UrlGenerationEngine urlGenerationEngine;

    /** Construtor exigido pelo CDI para a criação do proxy do bean. */
    protected UrlShortenerResource() {
    }

    @Inject
    public UrlShortenerResource(UrlGenerationEngine urlGenerationEngine) {
        this.urlGenerationEngine = urlGenerationEngine;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response shorten(ShortenUrlRequest request, @Context UriInfo uriInfo) {
        String url = request == null ? null : request.getUrl();
        UrlValidator.validate(url);

        ShortUrl shortUrl = urlGenerationEngine.generate(url);

        String baseUrl = uriInfo.getBaseUri().toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String fullShortUrl = baseUrl + "/" + shortUrl.getShortCode();

        ShortenUrlResponse response = ShortenUrlResponse.builder()
                .shortUrl(fullShortUrl)
                .shortCode(shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .createdAt(formatOrNull(shortUrl.getCreatedAt()))
                .expiresAt(formatOrNull(shortUrl.getExpiresAt()))
                .build();

        return Response.status(Response.Status.CREATED)
                .location(URI.create(fullShortUrl))
                .entity(response)
                .build();
    }

    private String formatOrNull(java.time.LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
