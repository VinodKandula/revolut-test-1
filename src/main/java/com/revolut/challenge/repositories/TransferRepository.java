package com.revolut.challenge.repositories;

import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.data.runtime.config.DataSettings;
import io.micronaut.validation.Validated;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.transaction.Transactional;
import javax.validation.Valid;

@ParametersAreNonnullByDefault
@Validated
public class TransferRepository {

    private final JdbcOperations jdbcOperations;

    public TransferRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    public Transfer save(@Valid Transfer transfer) {
        var sql = "INSERT INTO transfer ("
            + "operation_id, "
            + "amount, "
            + "currency, "
            + "sender_account_id,"
            + "recipient_account_id,"
            + "status,"
            + "created_at"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        var createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        PreparedStatement statement;
        try {
            statement = jdbcOperations.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, transfer.getOperationId().toString());
            statement.setBigDecimal(2, transfer.getAmount());
            statement.setString(3, transfer.getCurrency());
            statement.setString(4, transfer.getSenderAccountId().toString());
            statement.setString(5, transfer.getRecipientAccountId().toString());
            statement.setString(6, transfer.getStatus().name());
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
        } catch (SQLException e) {
            throw new DataAccessException("Error preparing SQL statement: " + e.getMessage(), e);
        }
        if (DataSettings.QUERY_LOG.isErrorEnabled()) {
            DataSettings.QUERY_LOG.debug("Executing Query: {}", sql);
        }
        try {
            statement.executeUpdate();
            var generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            return transfer.toBuilder()
                .id(generatedKeys.getLong("id"))
                .createdAt(createdAt)
                .build();
        } catch (SQLIntegrityConstraintViolationException e) {
            //can be improved to make sure it's the ID conflict that caused the exception (vendor-specific)
            throw new DuplicateOperationIdException(transfer.getOperationId(), e);
        } catch (SQLException e) {
            throw new DataAccessException("Error executing SQL statement: " + e.getMessage(), e);
        }
    }

    @NonNull
    @Transactional(rollbackOn = Exception.class)
    public Transfer getById(long transferId) {
        return jdbcOperations.prepareStatement(
            "SELECT * FROM transfer WHERE id = ?",
            statement -> {
                statement.setLong(1, transferId);
                var resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    throw new TransferNotFoundException(
                        "Transfer with ID " + transferId + " not found");
                }
                return buildTransfer(resultSet);
            }
        );
    }

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    public Transfer getByOperationId(UUID operationId) {
        return jdbcOperations.prepareStatement(
            "SELECT * FROM transfer WHERE operation_id = ?",
            statement -> {
                statement.setString(1, operationId.toString());
                var resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    throw new TransferNotFoundException(
                        "Transfer with operation ID " + operationId + " not found");
                }
                return buildTransfer(resultSet);
            }
        );
    }

    @Transactional(rollbackOn = Exception.class)
    public void updateStatus(long transferId, TransferStatus status) {
        if (jdbcOperations.prepareStatement("UPDATE transfer SET status = ? WHERE id = ?",
            statement -> {
                statement.setString(1, status.name());
                statement.setLong(2, transferId);
                return statement.executeUpdate();
            }
        ) < 1) {
            throw new TransferNotFoundException("Transfer with ID " + transferId + " not found");
        }
    }

    //for testing
    @Transactional(rollbackOn = Exception.class)
    public void deleteAll() {
        jdbcOperations.prepareStatement("DELETE FROM transfer",
            PreparedStatement::executeUpdate);
    }

    private static Transfer buildTransfer(ResultSet resultSet) throws SQLException {
        return Transfer.builder()
            .id(resultSet.getLong("id"))
            .status(TransferStatus.valueOf(resultSet.getString("status")))
            .amount(resultSet.getBigDecimal("amount"))
            .currency(resultSet.getString("currency"))
            .senderAccountId(UUID.fromString(resultSet.getString("sender_account_id")))
            .recipientAccountId(
                UUID.fromString(resultSet.getString("recipient_account_id")))
            .operationId(UUID.fromString(resultSet.getString("operation_id")))
            .createdAt(
                resultSet.getTimestamp("created_at").toLocalDateTime())
            .build();
    }
}
