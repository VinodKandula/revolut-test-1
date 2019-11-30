package com.revolut.challenge.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.revolut.challenge.service.AccountFundsNotFoundException;
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
        assertThat(funds(senderAccountId)).isEqualTo("0.0000");
        assertThat(funds(recipientAccountId)).isEqualTo("100.0000");
    }

    @Test
    void shouldNotTransferIfSenderFundsNotFound() {
        createFunds(recipientAccountId, "100.0");
        assertThatExceptionOfType(AccountFundsNotFoundException.class)
            .isThrownBy(() ->
                accountFundsRepository
                    .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0")))
            .withMessageContaining(senderAccountId.toString());
        assertThat(funds(recipientAccountId)).isEqualTo("100.0000");
    }

    @Test
    void shouldNotTransferIfRecipientFundsNotFound() {
        createFunds(senderAccountId, "100.0");
        assertThatExceptionOfType(AccountFundsNotFoundException.class)
            .isThrownBy(() ->
                accountFundsRepository
                    .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0")))
            .withMessageContaining(recipientAccountId.toString());
        assertThat(funds(senderAccountId)).isEqualTo("100.0000");
    }

    @Test
    void shouldNotTransferIfSenderHasInsufficientFunds() {
        createFunds(senderAccountId, "9.99");
        createFunds(recipientAccountId, "0.0");
        assertThat(accountFundsRepository
            .transferFunds(senderAccountId, recipientAccountId, new BigDecimal("10.0"))
        ).isFalse();
        assertThat(funds(senderAccountId)).isEqualTo("9.9900");
        assertThat(funds(recipientAccountId)).isEqualTo("0.0000");
    }

    private void createFunds(UUID accountId, String balance) {
        accountFundsRepository.save(AccountFunds.builder()
            .accountId(accountId)
            .currency("EUR")
            .balance(new BigDecimal(balance))
            .build());
    }

    private BigDecimal funds(UUID accountId) {
        return accountFundsRepository.findById(accountId)
            .map(AccountFunds::getBalance)
            .orElseThrow();
    }
}
