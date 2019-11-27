package com.revolut.challenge.service;

import com.revolut.challenge.repositories.AccountFundsRepository;
import com.revolut.challenge.repositories.TransferRepository;
import com.revolut.challenge.service.model.AccountFunds;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import io.micronaut.data.exceptions.DataAccessException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;
import javax.inject.Singleton;

@Singleton
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

    public Transfer processTransfer(Transfer transfer) {
        var senderAccount = accountFundsRepository
            .findById(transfer.getSenderAccountId()).orElseThrow(); //TODO: throw correct exception
        var recipientAccount = accountFundsRepository
            .findById(transfer.getRecipientAccountId())
            .orElseThrow(); //TODO: throw correct exception
        if (!Objects.equals(transfer.getCurrency(), senderAccount.getCurrency()) ||
            !Objects.equals(transfer.getCurrency(), recipientAccount.getCurrency())) {
            return null; //TODO: throw correct exception
        }
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

    private Transfer transferFunds(
        AccountFunds senderAccount,
        AccountFunds recipientAccount,
        Transfer transfer
    ) {
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
        return transfer.toBuilder() //TODO: better get from repo
            .status(TransferStatus.OK)
            .build();
    }
}
