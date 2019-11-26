CREATE TABLE account_funds
(
    account_id VARCHAR(255) NOT NULL,
    balance    DECIMAL      NOT NULL,
    currency   VARCHAR(255) NOT NULL
);
CREATE TABLE transfer
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_id         VARCHAR(255) NOT NULL UNIQUE,
    currency             VARCHAR(255) NOT NULL,
    amount               DECIMAL      NOT NULL,
    sender_account_id    VARCHAR(255) NOT NULL,
    recipient_account_id VARCHAR(255) NOT NULL,
    created_at           TIMESTAMP,
    status               VARCHAR(255) NOT NULL
);
