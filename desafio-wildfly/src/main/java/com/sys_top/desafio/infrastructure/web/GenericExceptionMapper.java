package com.sys_top.desafio.infrastructure.web;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Rede de segurança para qualquer exceção não mapeada especificamente —
 * o JAX-RS escolhe automaticamente o ExceptionMapper mais específico para
 * cada tipo, então este só é acionado quando nenhum dos outros bate.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        return ErrorResponses.build(Response.Status.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
    }
}
