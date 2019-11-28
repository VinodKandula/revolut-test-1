package com.revolut.challenge.api.exception.handlers;

import com.revolut.challenge.api.model.ErrorResponse;
import com.revolut.challenge.api.model.ErrorResponse.ErrorResponseBuilder;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;
import javax.validation.ValidationException;

@Singleton
@Produces
public class ValidationExceptionHandler implements
    ExceptionHandler<ValidationException, HttpResponse> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request,
        ValidationException exception) {
        return HttpResponse.badRequest(new ErrorResponseBuilder()
            .withMessage(exception.getMessage())
            .build());
    }
}