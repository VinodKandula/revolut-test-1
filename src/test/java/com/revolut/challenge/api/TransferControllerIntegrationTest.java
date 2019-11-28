package com.revolut.challenge.api;

import static com.revolut.challenge.TestConstants.INTEGRATION_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.revolut.challenge.api.model.TransferAccount.TransferAccountBuilder;
import com.revolut.challenge.api.model.TransferAccounts.TransferAccountsBuilder;
import com.revolut.challenge.api.model.TransferAmount.TransferAmountBuilder;
import com.revolut.challenge.api.model.TransferRequest;
import com.revolut.challenge.api.model.TransferRequest.TransferRequestBuilder;
import com.revolut.challenge.api.model.TransferResponse;
import com.revolut.challenge.api.model.TransferStatus;
import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest
@Tag(INTEGRATION_TAG)
class TransferControllerIntegrationTest {

    @Inject
    @Client("/api/v1")
    private RxHttpClient client;

    @Test
    @DisplayName("Should successfully transfer money between accounts")
    void shouldTransferFundsBetweenAccounts() {
        //GIVEN a recipient account with 0 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
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
        TransferRequest transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
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

    @ParameterizedTest
    @MethodSource("provideAccountIds")
    @DisplayName("Should return 404 if one or both of account funds entries are not found")
    void shouldReturnErrorIfAccountFundsNotFound(
        UUID senderAccountId,
        UUID recipientAccountId,
        UUID existingAccountId
    ) {
        //GIVEN some of the accounts' funds don't exist
        createAccount(existingAccountId, "1000.0");

        //AND a 4.81 EUR transfer between them is attempted
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = failTransfer(transferRequest);

        //THEN a not found error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());

        //TODO: check body
        //AND the account balance is unchanged
        assertAccountBalance(existingAccountId, "1000.0");
    }

    //WHERE
    static Stream<Arguments> provideAccountIds() {
        var existingRecipientId = UUID.randomUUID();
        var existingSenderId = UUID.randomUUID();
        return Stream.of(
            Arguments.arguments(UUID.randomUUID(), existingRecipientId, existingRecipientId),
            Arguments.arguments(existingSenderId, UUID.randomUUID(), existingSenderId),
            Arguments.arguments(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCurrencies")
    @DisplayName("Should return 400 if one or both of account funds currencies do not match the transfer currency")
    void shouldReturnErrorIfAccountCurrencyMismatches(
        String senderAccountCurrency,
        String recipientAccountCurrency
    ) {
        //GIVEN some of the accounts' currency doesn't match transaction's currency
        //AND a recipient account with 0 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, recipientAccountCurrency, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, senderAccountCurrency, "10.0");

        //AND a 4.81 EUR transfer between them is attempted
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = failTransfer(transferRequest);

        //THEN a bad request error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());

        //TODO: check body
        //AND the account balances are unchanged
        assertAccountBalance(recipientAccountId, "0.0");
        assertAccountBalance(senderAccountId, "10.0");
    }

    //WHERE
    static Stream<Arguments> provideCurrencies() {
        return Stream.of(
            Arguments.arguments("EUR", "USD"),
            Arguments.arguments("USD", "EUR"),
            Arguments.arguments("USD", "USD")
        );
    }

    @Test
    @DisplayName("Should reject the transfer if the sender doesn't have enough accound funds")
    void shouldRejectedTransferIfSenderDoesntHaveEnoughFunds() {
        //GIVEN a recipient account with 100 EURO balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "100.0");
        //AND a sender account with 1 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "1.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        TransferRequest transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = doTransfer(transferRequest);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.REJECTED);
        //AND a transfer number should be assigned to it
        assertThat(result.getTransferNumber()).matches("^\\d+$");
        //AND the createdAt field should be populated
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        //AND the recipient account still has 100.0 EUR balance
        assertAccountBalance(recipientAccountId, "100.0");
        //AND the sender account has 1 EUR balance left
        assertAccountBalance(senderAccountId, "1.0");
    }

    private TransferResponse doTransfer(TransferRequest transferRequest) {
        return client.toBlocking().retrieve(HttpRequest.POST("/transfer", transferRequest),
            TransferResponse.class);
    }

    private HttpResponse<?> failTransfer(TransferRequest transferRequest) {
        try {
            client.toBlocking().exchange(HttpRequest.POST("/transfer", transferRequest));
            return fail("The transfer should've failed");
        } catch (HttpClientResponseException e) {
            return e.getResponse();
        }
    }

    private TransferRequest buildTransferRequest(UUID senderAccountId, UUID recipientAccountId,
        UUID operationId, String amount) {
        return new TransferRequestBuilder()
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
        createAccount(accountId, "EUR", balance);
    }

    private void createAccount(UUID accountId, String currency, String balance) {
        var accountFunds = AccountFunds.builder()
            .accountId(accountId)
            .balance(new BigDecimal(balance))
            .currency(currency)
            .build();
        client.toBlocking().exchange(HttpRequest.POST("/account-funds", accountFunds));
    }
}