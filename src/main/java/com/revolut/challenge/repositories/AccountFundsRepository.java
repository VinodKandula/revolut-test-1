package com.revolut.challenge.repositories;

import com.revolut.challenge.service.model.AccountFunds;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.validation.Validated;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.transaction.Transactional;
import javax.validation.Valid;

@ParametersAreNonnullByDefault
@Validated
public class AccountFundsRepository {

    private final JdbcOperations jdbcOperations;

    public AccountFundsRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    //for testing
    @NonNull
    @Transactional(rollbackOn = Exception.class)
    public AccountFunds save(@Valid AccountFunds accountFunds) {
        jdbcOperations.prepareStatement(
            "INSERT INTO account_funds (account_id, balance, currency) VALUES (?, ?, ?)",
            statement -> {
                statement.setString(1, accountFunds.getAccountId().toString());
                statement.setBigDecimal(2, accountFunds.getBalance());
                statement.setString(3, accountFunds.getCurrency());
                return statement.executeUpdate();
            });
        return accountFunds;
    }

    @NonNull
    @Transactional(rollbackOn = Exception.class)
    public AccountFunds getById(UUID accountId) {
        return jdbcOperations.prepareStatement(
            "SELECT * FROM account_funds WHERE account_id = ?",
            statement -> {
                statement.setString(1, accountId.toString());
                var resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    throw new AccountFundsNotFoundException(accountId);
                }
                return buildAccountFunds(resultSet);
            });
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

    //for testing
    @Transactional(rollbackOn = Exception.class)
    public void deleteAll() {
        jdbcOperations.prepareStatement("DELETE FROM account_funds",
            PreparedStatement::executeUpdate);
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
        return transferAmount.compareTo(getById(senderAccountId).getBalance()) <= 0;
    }

    private static AccountFunds buildAccountFunds(ResultSet resultSet) throws SQLException {
        return AccountFunds.builder()
            .accountId(UUID.fromString(resultSet.getString("account_id")))
            .balance(resultSet.getBigDecimal("balance"))
            .currency(resultSet.getString("currency"))
            .build();
    }
}
