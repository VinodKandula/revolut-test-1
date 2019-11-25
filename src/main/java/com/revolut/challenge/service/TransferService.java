package com.revolut.challenge.service;

import com.revolut.challenge.repositories.AccountFundsRepository;
import com.revolut.challenge.repositories.TransferRepository;
import com.revolut.challenge.service.model.AccountFunds;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import java.util.Objects;
import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class TransferService {

    private final AccountFundsRepository accountFundsRepository;
    private final TransferRepository transferRepository;

    public TransferService(
        AccountFundsRepository accountFundsRepository,
        TransferRepository transferRepository
    ) {
        this.accountFundsRepository = accountFundsRepository;
        this.transferRepository = transferRepository;
    }

    public Transfer processTransfer(Transfer transfer) {
        Iterable<AccountFunds> accounts = accountFundsRepository.findAll();

        var senderAccount = accountFundsRepository
            .findById(transfer.getSenderAccountId()).orElseThrow(); //TODO: throw correct exception
        var recipientAccount = accountFundsRepository
            .findById(transfer.getRecipientAccountId())
            .orElseThrow(); //TODO: throw correct exception
        if (!Objects.equals(transfer.getCurrency(), senderAccount.getCurrency()) ||
            !Objects.equals(transfer.getCurrency(), recipientAccount.getCurrency())) {
            return null; //TODO: throw correct exception
        }

        var persistedTransfer = transferRepository.save( //TODO: correctly handle idempotency
            transfer.toBuilder()
                .status(TransferStatus.ACCEPTED)
                .build());
        return transferFunds(senderAccount, recipientAccount, persistedTransfer);
    }

    @Transactional
    private Transfer transferFunds(
        AccountFunds senderAccount,
        AccountFunds recipientAccount,
        Transfer transfer
    ) {
        accountFundsRepository
            .transferFunds(
                senderAccount.getAccountId(),
                recipientAccount.getAccountId(),
                transfer.getAmount());
        return transferRepository.update(
            transfer.toBuilder()
                .status(TransferStatus.OK)
                .build()
        );
    }
}
