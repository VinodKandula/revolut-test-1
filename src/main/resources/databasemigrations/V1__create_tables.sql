CREATE TABLE account_funds
(
    account_id VARCHAR(36)       NOT NULL PRIMARY KEY,
    balance    DECIMAL(65535, 2) NOT NULL,
    currency   VARCHAR(3)        NOT NULL
);
CREATE TABLE transfer
(
    id                   BIGINT IDENTITY PRIMARY KEY,
    operation_id         VARCHAR(36)    NOT NULL UNIQUE,
    currency             VARCHAR(3)     NOT NULL,
    amount               DECIMAL(15, 2) NOT NULL CHECK (amount > 0.00),
    sender_account_id    VARCHAR(36)    NOT NULL,
    recipient_account_id VARCHAR(36)    NOT NULL,
    created_at           TIMESTAMP      NOT NULL,
    status               VARCHAR(255)   NOT NULL
);
