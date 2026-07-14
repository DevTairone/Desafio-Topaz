package com.sys_top.desafio.infrastructure.web;

import com.sys_top.desafio.infrastructure.web.dto.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Monta a resposta HTTP padrão de erro, reutilizada por todos os
 * ExceptionMapper da aplicação.
 */
final class ErrorResponses {

    private ErrorResponses() {
    }

    static Response build(Response.Status status, String message) {
        ErrorResponse error = ErrorResponse.builder()
                .status(status.getStatusCode())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
