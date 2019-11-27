package com.revolut.challenge.service;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.UUID;

public class AccountFundsNotFoundException extends RuntimeException {

    public AccountFundsNotFoundException(@NonNull UUID accountId) {
        super("Account funds for account " + accountId + " weren't found");
    }
}
