package com.revolut.challenge.repositories;

import io.micronaut.data.exceptions.DataAccessException;
import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DuplicateOperationIdException extends DataAccessException {

    public DuplicateOperationIdException(UUID operationId) {
        super("A transfer with operation ID " + operationId + " already exists");
    }
}
