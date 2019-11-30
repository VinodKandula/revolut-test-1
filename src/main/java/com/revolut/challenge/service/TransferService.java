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
        //a helper is used to avoid the hack of exposing that private method for @Transactional to work
        try {
            return transactionHelper
                .runInTransaction(() -> transferFunds(senderAccount, recipientAccount, transfer));
        } catch (DataAccessException e) {
            if (duplicateTransferHappened(e)) {
                return findProcessedTransfer(transfer);
            } else {
                throw new IllegalStateException(e);
            }
        }
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

    private static boolean duplicateTransferHappened(DataAccessException e) {
        //TODO: make sure it's the INSERT that caused the exception
        return e.getCause() instanceof SQLIntegrityConstraintViolationException;
    }

    private Transfer findProcessedTransfer(Transfer transfer) {
        return transferRepository.findByOperationId(transfer.getOperationId())
            .orElseThrow(() -> new IllegalStateException(
                "Transfer with operation ID " + transfer.getOperationId()
                    + " was not found"));
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
        long transferId = transferRepository.save(
            transfer.toBuilder()
                .status(TransferStatus.ACCEPTED)
                .build()).getId();
        if (accountFundsRepository.transferFunds(senderAccount.getAccountId(),
            recipientAccount.getAccountId(), transfer.getAmount())) {
            transferRepository.update(transferId, TransferStatus.OK);
        } else {
            transferRepository.update(transferId, TransferStatus.REJECTED);
        }
        return transferRepository.findById(transferId).orElseThrow(() -> new IllegalStateException(
            "Transfer with ID " + transferId
                + " was not found"));
    }
}
