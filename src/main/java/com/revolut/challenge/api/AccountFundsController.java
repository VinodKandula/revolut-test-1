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
import java.util.UUID;
import javax.validation.Valid;

@Controller("/api/v1/account-funds")
@Validated
public class AccountFundsController {

    public AccountFundsController(
        AccountFundsRepository accountFundsRepository) {
        this.accountFundsRepository = accountFundsRepository;
    }

    private final AccountFundsRepository accountFundsRepository;

    //for testing
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public AccountFunds createAccountFunds(@Valid @Body AccountFunds accountFunds) {
        return accountFundsRepository.save(accountFunds);
    }

    //for testing
    @Get(value = "/{accountId}", produces = MediaType.APPLICATION_JSON)
    public AccountFunds getAccountFunds(@Valid @RequestAttribute UUID accountId) {
        return accountFundsRepository.getById(accountId);
    }

}
