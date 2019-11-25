package com.revolut.challenge.repositories;

import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.H2)
public interface AccountFundsRepository extends CrudRepository<AccountFunds, UUID> {

}
