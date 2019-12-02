package com.revolut.challenge.api;

import com.revolut.challenge.api.model.TransferRequest;
import com.revolut.challenge.api.model.TransferResponse;
import com.revolut.challenge.service.TransferService;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import java.math.BigDecimal;
import javax.validation.Valid;
import javax.validation.ValidationException;

@Controller("/api/v1/transfer")
@Validated
public class TransferController {

    private final TransferService transferService;
    private final TransferConverter transferConverter;

    public TransferController(
        TransferService transferService,
        TransferConverter transferConverter
    ) {
        this.transferService = transferService;
        this.transferConverter = transferConverter;
    }

    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public TransferResponse transferFunds(@Valid @Body TransferRequest transferRequest) {
        var transfer = transferConverter.fromCreateRequest(transferRequest);
        validateAmount(transfer.getAmount());
        return transferConverter.toTransferResponse(
            transferService.processTransfer(transfer));
    }

    private void validateAmount(@NonNull BigDecimal amount) { //TODO: validate max amount
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        throw new ValidationException("Transfer amount must exceed 0.00");
    }
}
