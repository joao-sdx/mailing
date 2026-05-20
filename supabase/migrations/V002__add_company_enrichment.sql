ALTER TABLE crm_companies
  ADD COLUMN IF NOT EXISTS siren          TEXT,
  ADD COLUMN IF NOT EXISTS naf_code       TEXT,
  ADD COLUMN IF NOT EXISTS sector         TEXT,
  ADD COLUMN IF NOT EXISTS employee_range TEXT,
  ADD COLUMN IF NOT EXISTS category       TEXT,
  ADD COLUMN IF NOT EXISTS city           TEXT,
  ADD COLUMN IF NOT EXISTS country        TEXT;
