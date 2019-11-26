package com.revolut.challenge.repositories;

import com.revolut.challenge.service.model.Transfer;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.H2)
public interface TransferRepository extends CrudRepository<Transfer, Integer> {

    Optional<Transfer> findByOperationId(UUID operationId);
}
