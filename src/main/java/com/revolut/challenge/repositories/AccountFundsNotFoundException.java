package com.revolut.challenge.repositories;

import io.micronaut.data.exceptions.DataAccessException;
import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class AccountFundsNotFoundException extends DataAccessException {

    public AccountFundsNotFoundException(UUID accountId) {
        super("Account funds for account " + accountId + " weren't found");
    }
}
