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
public class AccountFundsTestController {

    public AccountFundsTestController(
        AccountFundsRepository accountFundsRepository) {
        this.accountFundsRepository = accountFundsRepository;
    }

    private final AccountFundsRepository accountFundsRepository;

    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public AccountFunds createAccountFunds(@Valid @Body AccountFunds accountFunds) {
        return accountFundsRepository.save(accountFunds);
    }

    @Get(value = "/{accountId}", produces = MediaType.APPLICATION_JSON)
    public AccountFunds getAccountFunds(@Valid @RequestAttribute UUID accountId) {
        var accountFunds = accountFundsRepository.getById(accountId);
        return accountFunds
            .toBuilder()
            .balance(accountFunds.getBalance().setScale(2))
            .build();
    }

}
