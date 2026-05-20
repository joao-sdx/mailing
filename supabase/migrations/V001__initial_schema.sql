CREATE TABLE IF NOT EXISTS seo_query (
  id              BIGSERIAL PRIMARY KEY,
  keyword         TEXT,
  language_code   TEXT,
  depth           INT,
  location_code   INT,
  location_name   TEXT,
  file_prefix     TEXT,
  execution_date  TIMESTAMPTZ,
  check_url       TEXT
);

CREATE TABLE IF NOT EXISTS seo_result (
  id            BIGSERIAL PRIMARY KEY,
  type          TEXT,
  title         TEXT,
  domain        TEXT,
  url           TEXT UNIQUE,
  article       TEXT,
  summary       TEXT,
  scan_status   TEXT DEFAULT 'No',
  seo_query_id  BIGINT REFERENCES seo_query (id)
);

CREATE TABLE IF NOT EXISTS crm_companies (
  id    BIGSERIAL PRIMARY KEY,
  name  TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS crm_contacts (
  id              BIGSERIAL PRIMARY KEY,
  name            TEXT,
  first_name      TEXT,
  email           TEXT UNIQUE,
  company_id      BIGINT REFERENCES crm_companies (id)
);

CREATE TABLE IF NOT EXISTS seo_result_contacts (
  seo_result_id  BIGINT REFERENCES seo_result (id),
  contact_id     BIGINT REFERENCES crm_contacts (id),
  PRIMARY KEY (seo_result_id, contact_id)
);
