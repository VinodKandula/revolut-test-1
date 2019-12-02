package com.revolut.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionHelperTest {

    @Test
    void shouldReturnTheResultOfSupplier() {
        var expectedResult = UUID.randomUUID();
        assertThat(new TransactionHelper().getFromTransaction(() -> expectedResult))
            .isEqualTo(expectedResult);
    }

    @Test
    void shouldThrowIfSupplierReturnsNull() {
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new TransactionHelper().getFromTransaction(() -> null));
    }
}