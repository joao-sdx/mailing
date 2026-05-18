-- Dev seed: password is {noop}admin (plaintext).
-- Before production replace with {bcrypt}<BCryptPasswordEncoder().encode("...")>.
INSERT INTO users (username, password, enabled)
VALUES ('admin', '{noop}admin', TRUE);

INSERT INTO authorities (username, authority)
VALUES ('admin', 'ROLE_USER');
