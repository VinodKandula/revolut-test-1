package com.revolut.challenge.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.revolut.challenge.service.model.AccountFunds;
import io.micronaut.test.annotation.MicronautTest;
import java.math.BigDecimal;
import java.util.UUID;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@MicronautTest(transactional = false)
class AccountFundsRepositoryTest {

    private final UUID senderAccountId = UUID.randomUUID();
    private final UUID recipientAccountId = UUID.randomUUID();

    @Inject
    private AccountFundsRepository accountFundsRepository;

    @AfterEach
    void cleanUp() {
        accountFundsRepository.deleteAll();
    }

    @Test
    void shouldTransferFunds() {
        createFunds(senderAccountId, "100.0");
        createFunds(recipientAccountId, "0.0");
        assertThat(accountFundsRepository
            .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("100.00"))
        ).isTrue();
        assertThat(accountBalance(senderAccountId)).isEqualTo("0.00");
        assertThat(accountBalance(recipientAccountId)).isEqualTo("100.00");
    }

    @Test
    void shouldNotTransferIfSenderFundsNotFound() {
        createFunds(recipientAccountId, "100.0");
        assertThatExceptionOfType(AccountFundsNotFoundException.class)
            .isThrownBy(() ->
                accountFundsRepository
                    .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0")))
            .withMessageContaining(senderAccountId.toString());
        assertThat(accountBalance(recipientAccountId)).isEqualTo("100.00");
    }

    @Test
    void shouldNotTransferIfRecipientFundsNotFound() {
        createFunds(senderAccountId, "100.0");
        assertThatExceptionOfType(AccountFundsNotFoundException.class)
            .isThrownBy(() ->
                accountFundsRepository
                    .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0")))
            .withMessageContaining(recipientAccountId.toString());
        assertThat(accountBalance(senderAccountId)).isEqualTo("100.00");
    }

    @Test
    void shouldNotTransferIfSenderHasInsufficientFunds() {
        createFunds(senderAccountId, "9.99");
        createFunds(recipientAccountId, "0.0");
        assertThat(accountFundsRepository
            .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0"))
        ).isFalse();
        assertThat(accountBalance(senderAccountId)).isEqualTo("9.99");
        assertThat(accountBalance(recipientAccountId)).isEqualTo("0.00");
    }

    @Test
    void shouldThrowIfAccountFundsNotFound() {
        assertThatExceptionOfType(AccountFundsNotFoundException.class)
            .isThrownBy(() ->
                accountFundsRepository
                    .getById(UUID.randomUUID()));
    }

    private void createFunds(UUID accountId, String balance) {
        accountFundsRepository.save(AccountFunds.builder()
            .accountId(accountId)
            .currency("EUR")
            .balance(new BigDecimal(balance))
            .build());
    }

    private BigDecimal accountBalance(UUID accountId) {
        return accountFundsRepository.getById(accountId)
            .getBalance();
    }
}
