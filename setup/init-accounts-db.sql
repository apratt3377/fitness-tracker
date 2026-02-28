-- 1. Create the schema
CREATE SCHEMA IF NOT EXISTS accounts;

-- 2. Create the Trigger Function
CREATE OR REPLACE FUNCTION accounts.set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        NEW.created_at := CURRENT_TIMESTAMP;
        NEW.updated_at := CURRENT_TIMESTAMP;
    ELSIF (TG_OP = 'UPDATE') THEN
        NEW.updated_at := CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Create the Account Table
CREATE TABLE IF NOT EXISTS accounts.accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL, -- Constraint: Unique
    password_hash VARCHAR(255) NOT NULL,   -- Constraint: Not Null
    roles VARCHAR(32) CHECK (roles IN ('USER', 'ADMIN')), -- Constraint: Role values
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Apply the Trigger
DROP TRIGGER IF EXISTS set_timestamp_trigger ON accounts.accounts;
CREATE TRIGGER set_timestamp_trigger
    BEFORE INSERT OR UPDATE 
    ON accounts.accounts
    FOR EACH ROW
    EXECUTE FUNCTION accounts.set_timestamp();