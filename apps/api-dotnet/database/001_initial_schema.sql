CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    cognito_subject TEXT UNIQUE,
    email TEXT NULL,
    display_name TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id TEXT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    calorie_target_min INTEGER NULL,
    calorie_target_max INTEGER NULL,
    weight_unit TEXT NOT NULL DEFAULT 'kilograms',
    goal_weight_kg DOUBLE PRECISION NULL,
    sync_enabled BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS food_products (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    visibility TEXT NOT NULL DEFAULT 'private',
    name TEXT NOT NULL,
    brand TEXT NULL,
    serving_definition JSONB NOT NULL,
    nutrients JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS barcode_aliases (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    product_id UUID NOT NULL REFERENCES food_products(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    UNIQUE (owner_id, barcode)
);

CREATE TABLE IF NOT EXISTS diary_entries (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    food_id UUID NULL REFERENCES food_products(id),
    logged_at TIMESTAMPTZ NOT NULL,
    meal_type TEXT NULL,
    amount NUMERIC(12, 3) NOT NULL,
    multiplier NUMERIC(12, 3) NOT NULL DEFAULT 1,
    product_snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS weight_entries (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    logged_at TIMESTAMPTZ NOT NULL,
    weight NUMERIC(10, 2) NOT NULL,
    source TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS devices (
    owner_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    platform TEXT NULL,
    app_version TEXT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sync_cursor TEXT NULL,
    PRIMARY KEY (owner_id, device_id)
);

CREATE TABLE IF NOT EXISTS households (
    household_id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 120),
    created_by_user_id TEXT NOT NULL REFERENCES users(user_id),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS household_memberships (
    household_id UUID NOT NULL REFERENCES households(household_id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('owner', 'admin', 'member')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'removed')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed_at TIMESTAMPTZ NULL,
    PRIMARY KEY (household_id, user_id)
);

CREATE TABLE IF NOT EXISTS household_invitations (
    invitation_id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(household_id) ON DELETE CASCADE,
    invited_by_user_id TEXT NOT NULL REFERENCES users(user_id),
    token_hash TEXT NOT NULL UNIQUE,
    intended_email TEXT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'expired', 'revoked')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS household_membership_user_idx ON household_memberships(user_id, status);
CREATE INDEX IF NOT EXISTS household_invitation_lookup_idx ON household_invitations(household_id, status);
CREATE INDEX IF NOT EXISTS food_products_owner_idx ON food_products(owner_id, updated_at);
CREATE INDEX IF NOT EXISTS diary_entries_owner_logged_at_idx ON diary_entries(owner_id, logged_at);
CREATE INDEX IF NOT EXISTS weight_entries_owner_logged_at_idx ON weight_entries(owner_id, logged_at);
