package com.revolut.challenge;

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
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@MicronautTest
@Tag(INTEGRATION_TAG)
class AccountFundsApplicationSpecificationTest {

    private static final String MAXIMUM_TRANSFER = "9999999999999.99";

    @Inject
    @Client("/api/v1")
    private RxHttpClient client;

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1.00", "5.90", "9.99", "10.00", MAXIMUM_TRANSFER})
    @DisplayName("Should successfully transfer money between accounts")
    void shouldTransferFundsBetweenAccounts(String amount) {
        //GIVEN a recipient account with 0 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10000000000000.00 EUR balance
        var senderAccountId = UUID.randomUUID();
        var senderAccountBalance = "10000000000000.00";
        createAccount(senderAccountId, senderAccountBalance);
        //AND a transfer between them
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, amount);

        //WHEN the transfer is performed
        var result = doTransfer(transferRequest);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.OK);
        //AND a transfer number should be assigned to it
        assertThat(result.getTransferNumber()).matches("^\\d+$");
        //AND the createdAt field should be populated
        assertThat(result.getCreatedAt()).isCloseToUtcNow(new TemporalUnitLessThanOffset(1L,
            ChronoUnit.SECONDS));
        //AND the recipient account has the balance equal to the amount
        assertAccountBalance(recipientAccountId, amount);
        //AND the sender account has 10 - amount EUR left
        assertAccountBalance(senderAccountId,
            new BigDecimal(senderAccountBalance).subtract(new BigDecimal(amount)).toPlainString());
    }

    @Test
    @DisplayName("A duplicate transfer should return the same result as the original one, and should not be processed twice")
    void shouldIdempotentlyHandleDuplicateTransfers() {
        //GIVEN a recipient account with 0 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
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

    @Test
    @DisplayName("A different transfer with the same operation ID should result in status code 409")
    void shouldReturnErrorIfDifferentTransferHasSameOperationId() {
        //GIVEN a recipient account with 0 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the another transfer is performed with the same operationId but different amount
        doTransfer(transferRequest);
        var anotherRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "5.00");
        var result = failTransfer(anotherRequest);

        //THEN a not found error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.CONFLICT.getCode());

        //TODO: check body
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
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = failTransfer(transferRequest);

        //THEN a not found error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());

        //TODO: check body
        //AND the account balance is unchanged
        assertAccountBalance(existingAccountId, "1000.00");
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
        //GIVEN some of the accounts' currency doesn't match transfer's currency
        //AND a recipient account with 0 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, recipientAccountCurrency, "0.0");
        //AND a sender account with 10 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, senderAccountCurrency, "10.0");

        //AND a 4.81 EUR transfer between them is attempted
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = failTransfer(transferRequest);

        //THEN a bad request error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());

        //TODO: check body
        //AND the account balances are unchanged
        assertAccountBalance(recipientAccountId, "0.00");
        assertAccountBalance(senderAccountId, "10.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-999.99", "-1.00", "-0.0001", "0.00", "0.0001", "0.001", "0.0099",
        "9.9999", "9.999", "9", "10000000000000.00"})
    @DisplayName("Should return 400 if transfer amount is invalid")
    void shouldReturnErrorIfAmountValueIsInvalid(
        String amount
    ) {
        //GIVEN some of the accounts' currency doesn't match transfer's currency
        //AND a recipient account with 0 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "0.0");
        //AND a sender account with 10000000000000.00 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "10000000000000.00");

        //AND a 4.81 EUR transfer between them is attempted
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, amount);

        //WHEN the transfer is performed
        var result = failTransfer(transferRequest);

        //THEN a bad request error is returned
        assertThat(result.getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());

        //TODO: check body
        //AND the account balances are unchanged
        assertAccountBalance(recipientAccountId, "0.00");
        assertAccountBalance(senderAccountId, "10000000000000.00");
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
        //GIVEN a recipient account with 100 EUR balance
        var recipientAccountId = UUID.randomUUID();
        createAccount(recipientAccountId, "100.0");
        //AND a sender account with 1 EUR balance
        var senderAccountId = UUID.randomUUID();
        createAccount(senderAccountId, "1.0");
        //AND a 4.81 EUR transfer between them
        var operationId = UUID.randomUUID();
        var transferRequest = buildTransferRequest(senderAccountId, recipientAccountId,
            operationId, "4.81");

        //WHEN the transfer is performed
        var result = doTransfer(transferRequest);

        //THEN it should have positive outcome
        assertThat(result.getStatus()).isEqualTo(TransferStatus.REJECTED);
        //AND a transfer number should be assigned to it
        assertThat(result.getTransferNumber()).matches("^\\d+$");
        //AND the createdAt field should be populated
        assertThat(result.getCreatedAt()).isCloseToUtcNow(new TemporalUnitLessThanOffset(1L,
            ChronoUnit.SECONDS));
        //AND the recipient account still has 100.0 EUR balance
        assertAccountBalance(recipientAccountId, "100.00");
        //AND the sender account has 1 EUR balance left
        assertAccountBalance(senderAccountId, "1.00");
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
                .withValue(amount)
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