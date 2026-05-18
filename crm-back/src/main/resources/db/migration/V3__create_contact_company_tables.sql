CREATE TABLE companies (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    website    VARCHAR(500),
    phone      VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE contacts (
    id         BIGSERIAL    PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(200),
    phone      VARCHAR(50),
    job_title  VARCHAR(200),
    company_id BIGINT       REFERENCES companies (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contacts_company ON contacts (company_id);
