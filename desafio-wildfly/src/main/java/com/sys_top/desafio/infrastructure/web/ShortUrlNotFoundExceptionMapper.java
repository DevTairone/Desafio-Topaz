package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.domain.exception.ShortUrlNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ShortUrlNotFoundExceptionMapper implements ExceptionMapper<ShortUrlNotFoundException> {

    @Override
    public Response toResponse(ShortUrlNotFoundException exception) {
        return ErrorResponses.build(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
