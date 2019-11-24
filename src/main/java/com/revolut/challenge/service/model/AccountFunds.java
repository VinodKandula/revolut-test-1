package com.revolut.challenge.service.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.math.BigDecimal;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
@MappedEntity
public class AccountFunds {

    @Id
    @AutoPopulated
    private final UUID accountId;
    @NotNull
    private final BigDecimal balance;
    @NotNull
    @Size(min = 3, max = 3)
    private final String currency;
}
