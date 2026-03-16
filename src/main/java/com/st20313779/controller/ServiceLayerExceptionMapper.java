package com.st20313779.controller;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class ServiceLayerExceptionMapper implements ExceptionMapper<ServiceLayerException> {

    @Override
    public Response toResponse(final ServiceLayerException exception) {
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(Map.of(
                        "status", Response.Status.BAD_GATEWAY.getStatusCode(),
                        "error", Response.Status.BAD_GATEWAY.getReasonPhrase(),
                        "message", exception.getMessage() == null ? "Service layer failure" : exception.getMessage()
                ))
                .build();
    }
}

