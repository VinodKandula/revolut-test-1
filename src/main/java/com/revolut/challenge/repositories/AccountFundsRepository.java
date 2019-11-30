package com.revolut.challenge.repositories;

import com.revolut.challenge.service.AccountFundsNotFoundException;
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

    @Transactional(rollbackOn = Exception.class)
    public boolean transferFunds(
        UUID senderAccountId,
        UUID recipientAccountId,
        BigDecimal amount
    ) {
        selectForUpdate(senderAccountId, recipientAccountId);
        if (!senderHasEnoughFunds(senderAccountId, amount)) {
            return false;
        }
        creditTheSenderAccount(senderAccountId, amount);
        debitTheRecipientAccount(recipientAccountId, amount);
        return true;
    }

    private void selectForUpdate(UUID senderAccountId, UUID recipientAccountId) {
        jdbcOperations.prepareStatement(
            "SELECT account_id FROM account_funds WHERE account_id = ? OR account_id = ? FOR UPDATE",
            statement -> {
                statement.setString(1, senderAccountId.toString());
                statement.setString(2, recipientAccountId.toString());
                return statement.executeQuery();
            }
        );
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

    private boolean senderHasEnoughFunds(UUID senderAccountId, BigDecimal transferAmount) {
        ResultSet result = jdbcOperations.prepareStatement(
            "SELECT balance FROM account_funds WHERE account_id = ?",
            statement -> {
                statement.setString(1, senderAccountId.toString());
                return statement.executeQuery();
            });
        try {
            if (!result.next()) {
                throw new AccountFundsNotFoundException(senderAccountId);
            }
            return transferAmount.compareTo(result.getBigDecimal("balance")) <= 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
