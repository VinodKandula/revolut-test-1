package com.revolut.challenge.repositories;

import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.math.BigDecimal;
import java.util.UUID;
import javax.transaction.Transactional;

@JdbcRepository(dialect = Dialect.H2)
public abstract class AccountFundsRepository implements CrudRepository<AccountFunds, UUID> {

    private final JdbcOperations jdbcOperations;

    public AccountFundsRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Transactional
    public void transferFunds(UUID senderAccountId, UUID recipientAccountId,
        BigDecimal amount) {
        String debitSql = "UPDATE account_funds SET balance = balance - ? WHERE account_id = ?";
        String creditSql = "UPDATE account_funds SET balance = balance + ? WHERE account_id = ?";
        jdbcOperations.prepareStatement(debitSql, statement -> {
            statement.setBigDecimal(1, amount);
            statement.setString(2, senderAccountId.toString());
            return statement.execute();
        });
        jdbcOperations.prepareStatement(creditSql, statement -> {
            statement.setBigDecimal(1, amount);
            statement.setString(2, recipientAccountId.toString());
            return statement.execute();
        });
    }
}
