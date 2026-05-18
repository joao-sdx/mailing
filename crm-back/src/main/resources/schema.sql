CREATE TABLE IF NOT EXISTS users (
    username  VARCHAR(50)  NOT NULL PRIMARY KEY,
    password  VARCHAR(500) NOT NULL,
    enabled   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS authorities (
    username  VARCHAR(50) NOT NULL REFERENCES users (username),
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT uq_auth_username UNIQUE (username, authority)
);
