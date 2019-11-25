package com.revolut.challenge.service.model;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.math.BigDecimal;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder(toBuilder = true)
@Data
@MappedEntity
public class AccountFunds {

    @Id
    @NonNull
    private final UUID accountId;
    @NotNull
    private final BigDecimal balance;
    @NotNull
    @Size(min = 3, max = 3)
    private final String currency;

    //non-Lombok public constructor is used because Lombok assigns arg names that Micronaut Data rejects
    public AccountFunds(
        UUID accountId,
        BigDecimal balance,
        String currency
    ) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
    }
}
