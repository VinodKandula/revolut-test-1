package com.revolut.challenge.service;

import java.util.UUID;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(UUID accountId, String accountCurrency) {
        super("Transfer currency doesn't match currency " + accountCurrency + " of account "
            + accountId);
    }

}
