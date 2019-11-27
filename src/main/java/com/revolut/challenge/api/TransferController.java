package com.revolut.challenge.api;

import com.revolut.challenge.api.model.TransferRequest;
import com.revolut.challenge.api.model.TransferResponse;
import com.revolut.challenge.service.TransferService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import javax.validation.Valid;

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
        return transferConverter.toTransferResponse(
            transferService.processTransfer(transfer));
    }
}
