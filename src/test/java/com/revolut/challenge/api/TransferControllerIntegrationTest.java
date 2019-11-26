package com.revolut.challenge.api;

import static com.revolut.challenge.TestConstants.INTEGRATION_TAG;
import static org.assertj.core.api.Assertions.assertThat;

import com.revolut.challenge.api.model.TransferAccount;
import com.revolut.challenge.api.model.TransferAccounts;
import com.revolut.challenge.api.model.TransferAmount;
import com.revolut.challenge.api.model.TransferRequest;
import com.revolut.challenge.api.model.TransferResponse;
import com.revolut.challenge.api.model.TransferStatus;
import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@MicronautTest
@Tag(INTEGRATION_TAG)
class TransferControllerIntegrationTest {

    @Inject
    @Client("/api/v1")
    private RxHttpClient client;

    @Test
    @DisplayName("Should transfer money between accounts")
    void shouldTransferFundsBetweenAccounts() {
        //GIVEN a recipient account with 0 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(recipientAccountId, senderAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = doTransfer(transferRequest);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.OK);
        //AND a transfer number should be assigned to it
        assertThat(result.getTransferNumber()).matches("^\\d+$");
        //AND the createdAt field should be populated
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        //AND the recipient account has 6 4.81 EUR balance
        assertAccountBalance(recipientAccountId, "4.81");
        //AND the sender account has 5.19 EUR balance left
        assertAccountBalance(senderAccountId, "5.19");
    }

    @Test
    @DisplayName("A duplicate transfer should return the same result as the original one, and should not be processed twice")
    void shouldIdempotentlyHandleDuplicateTransfers() {
        //GIVEN a recipient account with 0 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(recipientAccountId, senderAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed twice
        var originalResult = doTransfer(transferRequest);
        var result = doTransfer(transferRequest);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.OK);
        //AND the transfer number should not change
        assertThat(result.getTransferNumber()).isEqualTo(originalResult.getTransferNumber());
        //AND the createdAt should not change
        assertThat(result.getCreatedAt()).isEqualTo(originalResult.getCreatedAt());
        //AND the recipient account has 6 4.81 EUR balance
        assertAccountBalance(recipientAccountId, "4.81");
        //AND the sender account has 5.19 EUR balance left
        assertAccountBalance(senderAccountId, "5.19");
    }

    private TransferResponse doTransfer(TransferRequest transferRequest) {
        return client.toBlocking().retrieve(HttpRequest.POST("/transfer", transferRequest),
            TransferResponse.class);
    }

    private TransferRequest buildTransferRequest(UUID recipientAccountId, UUID senderAccountId,
        UUID operationId, String amount) {
        return new TransferRequest.TransferRequestBuilder()
            .withOperationId(operationId)
            .withAccounts(new TransferAccounts.TransferAccountsBuilder()
                .withFrom(new TransferAccount.TransferAccountBuilder()
                    .withId(senderAccountId)
                    .build())
                .withTo(new TransferAccount.TransferAccountBuilder()
                    .withId(recipientAccountId)
                    .build())
                .build()
            )
            .withAmount(new TransferAmount.TransferAmountBuilder()
                .withCurrency("EUR")
                .withValue(new BigDecimal(amount))
                .build())
            .withMessage("test transfer")
            .build();
    }

    private void assertAccountBalance(UUID accountId, String balance) {
        assertThat(client.toBlocking()
            .retrieve(HttpRequest.GET("/account-funds/" + accountId), AccountFunds.class)
            .getBalance())
            .isEqualTo(new BigDecimal(balance));
    }

    private void createAccount(UUID accountId, String balance) {
        var accountFunds = AccountFunds.builder()
            .accountId(accountId)
            .balance(new BigDecimal(balance))
            .currency("EUR")
            .build();
        client.toBlocking().exchange(HttpRequest.POST("/account-funds", accountFunds));
    }
}