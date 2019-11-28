package com.revolut.challenge.service;

import java.util.UUID;

public class NotEnoughFundsException extends RuntimeException {

    public NotEnoughFundsException(UUID accountID) {
        super("Account " + accountID + " doesn't have enough funds");
    }
}
