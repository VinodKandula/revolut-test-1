package com.revolut.challenge.api.exception.handlers;

import com.revolut.challenge.api.model.ErrorResponse;
import com.revolut.challenge.api.model.ErrorResponse.ErrorResponseBuilder;
import com.revolut.challenge.service.CurrencyMismatchException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;

@Singleton
@Produces
public class CurrencyMismatchExceptionHandler implements
    ExceptionHandler<CurrencyMismatchException, HttpResponse> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request,
        CurrencyMismatchException exception) {
        return HttpResponse.badRequest(new ErrorResponseBuilder()
            .withMessage(exception.getMessage())
            .build());
    }
}