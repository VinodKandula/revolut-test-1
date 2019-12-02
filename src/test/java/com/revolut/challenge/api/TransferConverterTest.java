package com.revolut.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.revolut.challenge.api.model.TransferAccount.TransferAccountBuilder;
import com.revolut.challenge.api.model.TransferAccounts.TransferAccountsBuilder;
import com.revolut.challenge.api.model.TransferAmount.TransferAmountBuilder;
import com.revolut.challenge.api.model.TransferRequest.TransferRequestBuilder;
import com.revolut.challenge.api.model.TransferResponse.TransferResponseBuilder;
import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransferConverterTest {

    private final TransferConverter transferConverter = new TransferConverter();

    @Test
    @DisplayName("Should convert API TransferRequest to the Transfer entity model")
    void shouldConvertTransferRequestToTransfer() {
        //GIVEN a valid TransferRequest
        var operationId = UUID.randomUUID();
        var recipientAccountId = UUID.randomUUID();
        var senderAccountId = UUID.randomUUID();
        var transferRequest = new TransferRequestBuilder()
            .withOperationId(operationId)
            .withAccounts(new TransferAccountsBuilder()
                .withFrom(new TransferAccountBuilder()
                    .withId(senderAccountId)
                    .build())
                .withTo(new TransferAccountBuilder()
                    .withId(recipientAccountId)
                    .build())
                .build()
            )
            .withAmount(new TransferAmountBuilder()
                .withCurrency("EUR")
                .withValue("42.23")
                .build())
            .withMessage("test transfer")
            .build();

        //WHEN the TransferRequest is converted to Transfer entity
        var transfer = transferConverter.fromCreateRequest(transferRequest);

        //THEN a correct transfer is returned
        assertThat(transfer)
            .isEqualTo(Transfer.builder()
                .operationId(operationId)
                .senderAccountId(senderAccountId)
                .recipientAccountId(recipientAccountId)
                .status(TransferStatus.ACCEPTED)
                .currency("EUR")
                .amount(new BigDecimal("42.23"))
                .build());
    }

    @MethodSource("finalTransferStatuses")
    @ParameterizedTest
    @DisplayName("Should convert a Transfer with a final status to API TransferResponse")
    void shouldConvertCompletedTransferToTransferResponse(TransferStatus transferStatus,
        com.revolut.challenge.api.model.TransferStatus expectedApiStatus) {
        //GIVEN a valid Transfer with a final status
        var createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        var transfer = Transfer.builder()
            .id(16L)
            .createdAt(createdAt)
            .operationId(UUID.randomUUID())
            .senderAccountId(UUID.randomUUID())
            .recipientAccountId(UUID.randomUUID())
            .status(transferStatus)
            .currency("EUR")
            .amount(new BigDecimal("42.23"))
            .build();

        //WHEN Transfer entity is converted to TransferResponse
        var transferResponse = transferConverter.toTransferResponse(transfer);

        //THEN a correct response is returned
        assertThat(transferResponse)
            .isEqualTo(new TransferResponseBuilder()
                .withStatus(expectedApiStatus)
                .withTransferNumber("16")
                .withCreatedAt(createdAt)
                .build());
    }

    @Test
    @DisplayName("Should fail to convert a Transfer with a non-final status to API TransferResponse")
    void shouldFailToConvertAnUnfinishedTransfer() {
        //GIVEN a valid Transfer with a non-final status
        var transfer = Transfer.builder()
            .id(16L)
            .createdAt(LocalDateTime.now())
            .operationId(UUID.randomUUID())
            .senderAccountId(UUID.randomUUID())
            .recipientAccountId(UUID.randomUUID())
            .status(TransferStatus.ACCEPTED)
            .currency("EUR")
            .amount(new BigDecimal("42.23"))
            .build();

        //WHEN attempting to convert it to a TransferResponse
        //THEN an illegal argument exception should happen

        assertThatIllegalArgumentException()
            .isThrownBy(() -> transferConverter.toTransferResponse(transfer));
    }


    static Stream<Arguments> finalTransferStatuses() {
        return Stream.of(
            Arguments.arguments(TransferStatus.OK,
                com.revolut.challenge.api.model.TransferStatus.OK),
            Arguments.arguments(TransferStatus.REJECTED,
                com.revolut.challenge.api.model.TransferStatus.REJECTED)
        );
    }
}