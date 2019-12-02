package com.revolut.challenge.service;

import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ConflictingTransferException extends RuntimeException {

    public ConflictingTransferException(UUID operationId) {
        super("A transfer with operation ID " + operationId + " already exists");
    }
}
