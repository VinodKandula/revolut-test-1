package com.revolut.challenge.api.exception.handlers;

import com.revolut.challenge.api.model.ErrorResponse;
import com.revolut.challenge.api.model.ErrorResponse.ErrorResponseBuilder;
import com.revolut.challenge.service.ConflictingTransferException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;

@Singleton
@Produces
public class ConflictingTransferExceptionHandler implements
    ExceptionHandler<ConflictingTransferException, HttpResponse> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request,
        ConflictingTransferException exception) {
        return HttpResponse.<ErrorResponse>status(HttpStatus.CONFLICT)
            .body(new ErrorResponseBuilder()
                .withMessage(exception.getMessage())
                .build());
    }
}
