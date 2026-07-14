package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.domain.exception.ShortUrlExpiredException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ShortUrlExpiredExceptionMapper implements ExceptionMapper<ShortUrlExpiredException> {

    @Override
    public Response toResponse(ShortUrlExpiredException exception) {
        return ErrorResponses.build(Response.Status.GONE, exception.getMessage());
    }
}
