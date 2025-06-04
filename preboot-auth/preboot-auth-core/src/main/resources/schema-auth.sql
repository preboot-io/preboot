CREATE TABLE IF NOT EXISTS user_accounts
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID UNIQUE         NOT NULL,
    username            VARCHAR(255),
    email               VARCHAR(255) UNIQUE NOT NULL,
    language            VARCHAR(255),
    timezone            VARCHAR(255),
    active              BOOLEAN,
    version             BIGINT,
    reset_token_version INT
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_uuid ON user_accounts (uuid);

CREATE TABLE IF NOT EXISTS user_account_credentials
(
    id            BIGSERIAL PRIMARY KEY,
    user_accounts BIGINT REFERENCES user_accounts (id),
    type          VARCHAR(255),
    attribute     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_account_permissions
(
    id            BIGSERIAL PRIMARY KEY,
    user_accounts BIGINT REFERENCES user_accounts (id),
    name          VARCHAR(255),
    tenant_id     UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS user_account_roles
(
    id            BIGSERIAL PRIMARY KEY,
    user_accounts BIGINT REFERENCES user_accounts (id),
    name          VARCHAR(255),
    tenant_id     UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS user_account_role_permissions
(
    id   BIGSERIAL PRIMARY KEY,
    role VARCHAR(255),
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_account_devices
(
    id                 BIGSERIAL PRIMARY KEY,
    user_accounts      BIGINT REFERENCES user_accounts (id),
    name               VARCHAR(255),
    device_fingerprint VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_account_sessions
(
    id                 BIGSERIAL PRIMARY KEY,
    session_id         UUID UNIQUE NOT NULL,
    user_account_id    UUID REFERENCES user_accounts (uuid),
    impersonated_by    UUID REFERENCES user_accounts (uuid),
    credential_type    VARCHAR(255),
    agent              VARCHAR(255),
    ip                 VARCHAR(255),
    device_fingerprint VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE,
    expires_at         TIMESTAMP WITH TIME ZONE,
    remember_me        BOOLEAN,
    tenant_id          UUID
);

CREATE TABLE IF NOT EXISTS tenants
(
    id   BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    attributes JSONB,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_account_tenants (
    id BIGSERIAL PRIMARY KEY,
    user_account_uuid UUID REFERENCES user_accounts (uuid),
    tenant_uuid UUID REFERENCES tenants (uuid),
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_user__account_sessions_session_id ON user_account_sessions (session_id);
CREATE INDEX IF NOT EXISTS idx_user__account_sessions_expires_at ON user_account_sessions (expires_at);

CREATE TABLE IF NOT EXISTS tenant_roles (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_tenant_role UNIQUE (tenant_id, role_name)
);

CREATE INDEX IF NOT EXISTS idx_tenant_roles_tenant_id ON tenant_roles (tenant_id);
