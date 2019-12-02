package com.revolut.challenge.service;

import com.revolut.challenge.repositories.AccountFundsRepository;
import com.revolut.challenge.repositories.DuplicateOperationIdException;
import com.revolut.challenge.repositories.TransferRepository;
import com.revolut.challenge.service.model.AccountFunds;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@ParametersAreNonnullByDefault
@AllArgsConstructor
public class TransferService {

    private final AccountFundsRepository accountFundsRepository;
    private final TransferRepository transferRepository;
    private final TransactionHelper transactionHelper;

    @NonNull
    public Transfer processTransfer(Transfer transfer) {
        try {
            var senderAccount = accountFundsRepository.getById(transfer.getSenderAccountId());
            var recipientAccount = accountFundsRepository.getById(transfer.getRecipientAccountId());
            validateCurrency(transfer, senderAccount, recipientAccount);
            //a helper is used to avoid the hack of exposing that private method for @Transactional to work
            return transactionHelper.getFromTransaction(
                () -> transferFunds(senderAccount, recipientAccount, transfer));
        } catch (DuplicateOperationIdException e) {
            return transferRepository.getByOperationId(transfer.getOperationId()); //TODO: return 409 if not equal
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

    @NonNull
    private Transfer transferFunds(
        AccountFunds senderAccount,
        AccountFunds recipientAccount,
        Transfer transfer
    ) {
        long transferId = transferRepository.save(transfer).getId();
        if (accountFundsRepository.transferFunds(senderAccount.getAccountId(),
            recipientAccount.getAccountId(), transfer.getAmount())) {
            transferRepository.updateStatus(transferId, TransferStatus.OK);
        } else {
            transferRepository.updateStatus(transferId, TransferStatus.REJECTED);
        }
        return transferRepository.getById(transferId);
    }
}
