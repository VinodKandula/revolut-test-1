package com.revolut.challenge.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.math.BigDecimal;
import java.util.UUID;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder(toBuilder = true)
@Data
@Immutable
@JsonDeserialize(builder = AccountFunds.AccountFundsBuilder.class)
public class AccountFunds {

    @NonNull
    private final UUID accountId;
    @NotNull
    private final BigDecimal balance;
    @NotNull
    @Size(min = 3, max = 3)
    private final String currency;

    @JsonPOJOBuilder(withPrefix = "")
    public static class AccountFundsBuilder {

    }
}
