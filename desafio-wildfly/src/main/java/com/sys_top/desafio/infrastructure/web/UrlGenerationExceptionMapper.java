package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.domain.exception.UrlGenerationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UrlGenerationExceptionMapper implements ExceptionMapper<UrlGenerationException> {

    @Override
    public Response toResponse(UrlGenerationException exception) {
        return ErrorResponses.build(Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage());
    }
}
