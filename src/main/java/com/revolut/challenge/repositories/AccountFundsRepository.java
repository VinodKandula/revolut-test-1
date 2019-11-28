package com.revolut.challenge.repositories;

import com.revolut.challenge.service.AccountFundsNotFoundException;
import com.revolut.challenge.service.NotEnoughFundsException;
import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.transaction.Transactional;

@JdbcRepository(dialect = Dialect.H2)
public abstract class AccountFundsRepository implements CrudRepository<AccountFunds, UUID> {

    private final JdbcOperations jdbcOperations;

    public AccountFundsRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Transactional(rollbackOn = {
        NotEnoughFundsException.class,
        AccountFundsNotFoundException.class,
        IllegalStateException.class
    })
    public void transferFunds(
        UUID senderAccountId,
        UUID recipientAccountId,
        BigDecimal amount
    ) {
        creditTheSenderAccount(senderAccountId, amount);
        checkIfSenderHasEnoughFunds(senderAccountId);
        debitTheRecipientAccount(recipientAccountId, amount);
    }

    private void debitTheRecipientAccount(UUID recipientAccountId, BigDecimal amount) {
        if (jdbcOperations.prepareStatement(
            "UPDATE account_funds SET balance = balance + ? WHERE account_id = ?",
            statement -> {
                statement.setBigDecimal(1, amount);
                statement.setString(2, recipientAccountId.toString());
                return statement.executeUpdate();
            }) < 1) {
            throw new AccountFundsNotFoundException(recipientAccountId);
        }
    }

    private void creditTheSenderAccount(UUID senderAccountId, BigDecimal amount) {
        if (jdbcOperations.prepareStatement(
            "UPDATE account_funds SET balance = balance - ? WHERE account_id = ?",
            statement -> {
                statement.setBigDecimal(1, amount);
                statement.setString(2, senderAccountId.toString());
                return statement.executeUpdate();
            }) < 1) {
            throw new AccountFundsNotFoundException(senderAccountId);
        }
    }

    private void checkIfSenderHasEnoughFunds(UUID senderAccountId) {
        ResultSet result = jdbcOperations.prepareStatement(
            "SELECT balance FROM account_funds WHERE account_id = ?",
            statement -> {
                statement.setString(1, senderAccountId.toString());
                return statement.executeQuery();
            });
        try {
            if (result.isLast()) {
                throw new AccountFundsNotFoundException(senderAccountId);
            }
            result.next();
            if (BigDecimal.ZERO.compareTo(result.getBigDecimal("balance")) > 0) {
                throw new NotEnoughFundsException(senderAccountId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
