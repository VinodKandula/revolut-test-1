package com.revolut.challenge.api;

import com.revolut.challenge.repositories.AccountFundsRepository;
import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.RequestAttribute;
import io.micronaut.validation.Validated;
import io.reactivex.Single;
import java.util.UUID;
import javax.validation.Valid;

@Controller("/api/v1/account-funds")
@Validated
public class AccountFundsController { //TODO: a controller for tests. replace with a proper controller

    public AccountFundsController(
        AccountFundsRepository accountFundsRepository) {
        this.accountFundsRepository = accountFundsRepository;
    }

    private final AccountFundsRepository accountFundsRepository;

    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public Single<AccountFunds> createAccountFunds(@Valid @Body AccountFunds accountFunds) {
        accountFundsRepository.save(accountFunds);
        return Single.just(accountFunds);
    }

    @Get(value = "/{accountId}", produces = MediaType.APPLICATION_JSON)
    public Single<AccountFunds> getAccountFunds(@Valid @RequestAttribute UUID accountId) {
        return Single.just(accountFundsRepository.findById(accountId).orElseThrow());
    }

}
