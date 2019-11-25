package com.revolut.challenge.api;

import com.revolut.challenge.api.model.TransferRequest;
import com.revolut.challenge.api.model.TransferResponse;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.validation.Validated;
import javax.inject.Singleton;
import javax.validation.Valid;

@Singleton
@Validated
public class TransferConverter {

    @NonNull
    public Transfer fromCreateRequest(@NonNull @Valid TransferRequest transferRequest) {
        return Transfer.builder()
            .operationId(transferRequest.getOperationId())
            .currency(transferRequest.getAmount().getCurrency().toUpperCase())
            .amount(transferRequest.getAmount().getValue())
            .status(TransferStatus.ACCEPTED)
            .senderAccountId(transferRequest.getAccounts().getFrom().getId())
            .recipientAccountId(transferRequest.getAccounts().getTo().getId())
            .build();
    }

    @NonNull
    public TransferResponse toTransferResponse(@NonNull @Valid Transfer transfer) {
        return new TransferResponse.TransferResponseBuilder<>()
            .withCreatedAt(transfer.getCreatedAt())
            .withTransferNumber(Long.toString(transfer.getId()))
            .withStatus(com.revolut.challenge.api.model.TransferStatus
                .fromValue(transfer.getStatus().toString()))
            .build();
    }
}
