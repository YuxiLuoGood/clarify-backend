CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount      DECIMAL(12, 2) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    type        VARCHAR(10) NOT NULL,
    date        DATE NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tx_user_date     ON transactions(user_id, date DESC);
CREATE INDEX idx_tx_user_category ON transactions(user_id, category);