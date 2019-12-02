package com.revolut.challenge.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.revolut.challenge.service.model.Transfer;
import com.revolut.challenge.service.model.TransferStatus;
import io.micronaut.test.annotation.MicronautTest;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest(transactional = false)
class TransferRepositoryTest {

    private final UUID senderAccountId = UUID.randomUUID();
    private final UUID recipientAccountId = UUID.randomUUID();
    private final UUID operationId = UUID.randomUUID();

    @Inject
    private TransferRepository transferRepository;

    @AfterEach
    void cleanUp() {
        transferRepository.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("transferAmounts")
    void shouldPersistTransfer(BigDecimal amount) {
        var transfer = buildTransfer(amount);
        var persistedTransfer = transferRepository.save(transfer);
        assertThat(persistedTransfer.getId()).isNotNull();
        assertThat(persistedTransfer.getCreatedAt())
            .isCloseToUtcNow(new TemporalUnitLessThanOffset(1L,
                ChronoUnit.SECONDS));
        assertThat(
            persistedTransfer.toBuilder()
                .id(null)
                .createdAt(null)
                .build()
        ).isEqualTo(transfer);
    }

    static Stream<BigDecimal> transferAmounts() {
        return Stream.of(
            BigDecimal.ONE,
            BigDecimal.TEN,
            new BigDecimal("0.01"),
            buildMaximumTransferAmount()
        );
    }

    @Test
    void shouldQueryTransferById() {
        var persistedTransfer = transferRepository.save(buildTransfer());
        assertThat(transferRepository.getById(persistedTransfer.getId()))
            .isEqualTo(persistedTransfer);
    }

    @Test
    void shouldQueryTransferByOperationId() {
        var persistedTransfer = transferRepository.save(buildTransfer());
        assertThat(transferRepository.getByOperationId(persistedTransfer.getOperationId()))
            .isEqualTo(persistedTransfer);
    }

    @Test
    void shouldUpdateTransferStatus() {
        var persistedTransfer = transferRepository.save(buildTransfer());
        transferRepository.updateStatus(persistedTransfer.getId(), TransferStatus.REJECTED);
        assertThat(transferRepository.getById(persistedTransfer.getId()))
            .isEqualTo(persistedTransfer.toBuilder()
                .status(TransferStatus.REJECTED)
                .build());
    }

    @Test
    void shouldFailToQueryNonExistentTransferById() {
        assertThatExceptionOfType(TransferNotFoundException.class)
            .isThrownBy(() -> transferRepository.getById(999));
    }

    @Test
    void shouldFailToQueryNonExistentTransferByOperationId() {
        assertThatExceptionOfType(TransferNotFoundException.class)
            .isThrownBy(() -> transferRepository.getByOperationId(UUID.randomUUID()));
    }

    @Test
    void shouldFailToUpdateStatusOfNonExistentTransfer() {
        assertThatExceptionOfType(TransferNotFoundException.class)
            .isThrownBy(() -> transferRepository.updateStatus(999, TransferStatus.REJECTED));
    }

    @Test
    void shouldFailToPersistTransferWithSameOperationId() {
        transferRepository.save(buildTransfer());
        assertThatExceptionOfType(DuplicateOperationIdException.class)
            .isThrownBy(() -> transferRepository.save(buildTransfer()));
    }

    private Transfer buildTransfer() {
        return buildTransfer(BigDecimal.TEN.setScale(4));
    }

    private Transfer buildTransfer(BigDecimal amount) {
        return Transfer.builder()
            .operationId(operationId)
            .recipientAccountId(recipientAccountId)
            .senderAccountId(senderAccountId)
            .currency("EUR")
            .amount(amount)
            .status(TransferStatus.ACCEPTED)
            .build();
    }

    private static BigDecimal buildMaximumTransferAmount() {
        return new BigDecimal(IntStream.range(0, 13).mapToObj(i -> "9")
            .collect(Collectors.joining("", "", ".99")));
    }
}
