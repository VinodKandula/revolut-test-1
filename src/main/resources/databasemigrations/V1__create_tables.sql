CREATE TABLE account_funds
(
    account_id VARCHAR(36)       NOT NULL,
    balance    DECIMAL(65535, 4) NOT NULL,
    currency   VARCHAR(3)        NOT NULL
);
CREATE TABLE transfer
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_id         VARCHAR(36) NOT NULL UNIQUE,
    currency             VARCHAR(3) NOT NULL,
    amount               DECIMAL(65535, 4)      NOT NULL,
    sender_account_id    VARCHAR(36) NOT NULL,
    recipient_account_id VARCHAR(36) NOT NULL,
    created_at           TIMESTAMP,
    status               VARCHAR(255) NOT NULL
);
