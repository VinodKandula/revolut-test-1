package com.revolut.challenge.api.exception.handlers;

import com.revolut.challenge.api.model.ErrorResponse;
import com.revolut.challenge.api.model.ErrorResponse.ErrorResponseBuilder;
import com.revolut.challenge.service.AccountFundsNotFoundException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;

@Singleton
@Produces
public class AccountFundsNotFoundExceptionHandler implements
    ExceptionHandler<AccountFundsNotFoundException, HttpResponse> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request,
        AccountFundsNotFoundException exception) {
        return HttpResponse.notFound(new ErrorResponseBuilder()
            .withMessage(exception.getMessage())
            .build());
    }
}
