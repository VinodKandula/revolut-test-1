package com.revolut.challenge.service;

import com.revolut.challenge.repositories.AccountFundsRepository;
import com.revolut.challenge.repositories.TransferRepository;
import com.revolut.challenge.service.model.AccountFunds;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.data.exceptions.DataAccessException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Singleton;

@Singleton
@ParametersAreNonnullByDefault
public class TransferService {

    private final AccountFundsRepository accountFundsRepository;
    private final TransferRepository transferRepository;
    private final TransactionHelper transactionHelper;

    public TransferService(
        AccountFundsRepository accountFundsRepository,
        TransferRepository transferRepository,
        TransactionHelper transactionHelper) {
        this.accountFundsRepository = accountFundsRepository;
        this.transferRepository = transferRepository;
        this.transactionHelper = transactionHelper;
    }

    @NonNull
    public Transfer processTransfer(Transfer transfer) {
        var senderAccount = getAccountFunds(transfer.getSenderAccountId());
        var recipientAccount = getAccountFunds(transfer.getRecipientAccountId());
        validateCurrency(transfer, senderAccount, recipientAccount);
        Transfer persistedTransfer;
        try {
            persistedTransfer = transferRepository.save(
                transfer.toBuilder()
                    .status(TransferStatus.ACCEPTED)
                    .build());
        } catch (DataAccessException e) {
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                return transferRepository.findByOperationId(transfer.getOperationId())
                    .orElseThrow(() -> new IllegalStateException(
                        "The original transfer with ID " + transfer.getOperationId()
                            + " was not found"));
            } else {
                throw e;
            }
        }
        return transferFunds(senderAccount, recipientAccount, persistedTransfer);
    }

    private static void validateCurrency(Transfer transfer, AccountFunds senderAccount,
        AccountFunds recipientAccount) {
        validateCurrency(transfer, senderAccount);
        validateCurrency(transfer, recipientAccount);
    }

    private static void validateCurrency(Transfer transfer, AccountFunds senderAccount) {
        if (!Objects.equals(transfer.getCurrency(), senderAccount.getCurrency())) {
            throw new CurrencyMismatchException(senderAccount.getAccountId(),
                senderAccount.getCurrency());
        }
    }

    @NonNull
    private AccountFunds getAccountFunds(UUID accountId) {
        return accountFundsRepository
            .findById(accountId)
            .orElseThrow(() -> new AccountFundsNotFoundException(accountId));
    }

    @NonNull
    private Transfer transferFunds(
        AccountFunds senderAccount,
        AccountFunds recipientAccount,
        Transfer transfer
    ) {
        try {
            //a helper is used to avoid the hack of exposing this private method for @Transactional to work
            transactionHelper.runInTransaction(() -> {
                accountFundsRepository
                    .transferFunds(
                        senderAccount.getAccountId(),
                        recipientAccount.getAccountId(),
                        transfer.getAmount());
                transferRepository.update(transfer.getId(), TransferStatus.OK);
                return null;
            });
        } catch (NotEnoughFundsException e) {
            return transfer.toBuilder()
                .status(TransferStatus.REJECTED)
                .build();
        }
        return transfer.toBuilder() //TODO: better get from repo
            .status(TransferStatus.OK)
            .build();
    }
}
