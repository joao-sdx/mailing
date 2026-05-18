-- Default admin user. Password is stored with {noop} prefix (plaintext) for local dev only.
-- Before going to production, replace with a bcrypt-encoded value:
--   {bcrypt}<output of new BCryptPasswordEncoder().encode("yourpassword")>
INSERT INTO users (username, password, enabled)
VALUES ('admin', '{noop}admin', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO authorities (username, authority)
VALUES ('admin', 'ROLE_USER')
ON CONFLICT ON CONSTRAINT uq_auth_username DO NOTHING;
