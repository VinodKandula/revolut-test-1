package com.revolut.challenge.service.model;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
@MappedEntity
public class Transfer {

    @Id
    @GeneratedValue
    private final Integer id;
    @NotNull
    private final UUID operationId;
    @Size(min = 3, max = 3)
    private final String currency;
    @NotNull
    private final BigDecimal amount;
    @NotNull
    private final UUID senderAccountId;
    @NotNull
    private final UUID recipientAccountId;
    @DateCreated
    private final LocalDateTime createdAt;
    @NotNull
    private final TransferStatus status;
}
