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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@MicronautTest
@Tag(INTEGRATION_TAG)
class TransferControllerIntegrationTest {

    @Inject
    @Client("/api/v1")
    private RxHttpClient client;

    @Test
    void shouldTransferFundsBetweenAccounts() {
        //GIVEN a recipient account with 0 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");

        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        var transferRequest = new TransferRequest.TransferRequestBuilder()
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
                .withValue(new BigDecimal("4.81"))
                .build())
            .withMessage("test transfer")
            .build();

        //WHEN the transfer is performed
        var result = client.toBlocking().retrieve(HttpRequest.POST("/transfer", transferRequest),
            TransferResponse.class);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.OK);
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        //AND a transfer number should be assigned to it
        assertThat(result.getTransferNumber()).matches("^\\d+$");
        //AND the recipient account has 6 4.81 EUR balance
        assertAccountBalance(recipientAccountId, "4.81");
        //AND the sender account has 5.19 EUR balance left
        assertAccountBalance(senderAccountId, "5.19");
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