-- CASA CONECTADA / Fase cuentas locales.
-- Guardar solo hashes de tokens; nunca texto plano de credenciales.
CREATE TABLE IF NOT EXISTS auth_sessions (
    id TEXT PRIMARY KEY NOT NULL,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE INDEX IF NOT EXISTS ix_auth_sessions_user_expiry ON auth_sessions(user_id, expires_at);

-- La clave unica impide usar una invitacion mas de una vez, incluso con peticiones simultaneas.
CREATE TABLE IF NOT EXISTS invitation_claims (
    invitation_id TEXT PRIMARY KEY NOT NULL REFERENCES invitations(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    claimed_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

-- Evita la creacion concurrente de varios administradores iniciales.
CREATE TABLE IF NOT EXISTS setup_guard (
    id INTEGER PRIMARY KEY NOT NULL CHECK (id = 1),
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
