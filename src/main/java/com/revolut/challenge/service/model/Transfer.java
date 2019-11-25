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
    private Long id; //has to be non-final because of how Micronaut Data works
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
    private LocalDateTime createdAt; //has to be non-final because of how Micronaut Data works
    @NotNull
    private final TransferStatus status;

    //non-Lombok public constructor is used because Lombok assigns arg names that Micronaut Data rejects
    public Transfer(
        Long id,
        UUID operationId,
        String currency,
        BigDecimal amount,
        UUID senderAccountId,
        UUID recipientAccountId,
        LocalDateTime createdAt,
        TransferStatus status
    ) {
        this.id = id;
        this.operationId = operationId;
        this.currency = currency;
        this.amount = amount;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.createdAt = createdAt;
        this.status = status;
    }
}
