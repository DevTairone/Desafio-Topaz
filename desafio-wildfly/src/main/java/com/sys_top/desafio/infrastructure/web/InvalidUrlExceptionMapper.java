package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.domain.exception.InvalidUrlException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidUrlExceptionMapper implements ExceptionMapper<InvalidUrlException> {

    @Override
    public Response toResponse(InvalidUrlException exception) {
        return ErrorResponses.build(Response.Status.BAD_REQUEST, exception.getMessage());
    }
}
