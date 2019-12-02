package com.revolut.challenge.service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
@Immutable
public class Transfer {

    private final Long id;
    @NotNull
    private final UUID operationId;
    @Size(min = 3, max = 3)
    @NotNull
    private final String currency;
    @NotNull
    private final BigDecimal amount;
    @NotNull
    private final UUID senderAccountId;
    @NotNull
    private final UUID recipientAccountId;
    private final LocalDateTime createdAt;
    @NotNull
    private final TransferStatus status;
}
