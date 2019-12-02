package com.revolut.challenge.repositories;

import io.micronaut.data.exceptions.DataAccessException;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TransferNotFoundException extends DataAccessException {

    public TransferNotFoundException(String message) {
        super(message);
    }
}
