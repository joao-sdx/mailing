# Supabase Self-Hosted — Ubuntu Install Guide

Self-hosting Supabase on Ubuntu using the official Docker Compose setup.

## Prerequisites

| Resource | Minimum  | Recommended |
|----------|----------|-------------|
| RAM      | 4 GB     | 8 GB+       |
| CPU      | 2 cores  | 4 cores+    |
| Disk     | 50 GB SSD | 80 GB+ SSD |

- Ubuntu 22.04 LTS or later
- Git, OpenSSL

### Install Docker

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git openssl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

## Installation

### 1. Create a dedicated user

```bash
sudo useradd --system --create-home --shell /bin/bash supabase
sudo usermod -aG docker supabase

# Switch to the supabase user for all subsequent steps
sudo -iu supabase
```

### 2. Clone the Supabase repo

Use sparse checkout to avoid downloading the full monorepo:

```bash
git clone --filter=blob:none --no-checkout --depth=1 --quiet https://github.com/supabase/supabase
cd supabase
git sparse-checkout init --cone
git sparse-checkout set docker
git checkout --quiet
cd ..
```

### 3. Set up project directory

```bash
mkdir supabase-project
cp -rf supabase/docker/* supabase-project
cp supabase/docker/.env.example supabase-project/.env
cd supabase-project
docker compose pull
```

### 4. Generate secure keys

**Never use the placeholder credentials from `.env.example` in production.**

```bash
sh utils/generate-keys.sh
sh utils/add-new-auth-keys.sh
```

Review the output and copy the generated values into `.env`.

### 5. Configure environment

Edit `.env` and update:

```dotenv
# URLs — set to your server IP or domain
SUPABASE_PUBLIC_URL=http://your-server-ip:8000
API_EXTERNAL_URL=http://your-server-ip:8000
SITE_URL=http://your-server-ip:8000

# Studio dashboard credentials
DASHBOARD_USERNAME=admin
DASHBOARD_PASSWORD=your-strong-password
```

If using rootless Docker, also set:

```dotenv
DOCKER_SOCKET_LOCATION=/run/user/1000/docker.sock
```

### 6. Start Supabase

```bash
docker compose up -d
```

### 7. Verify services are running

```bash
docker compose ps
```

All services should show `Up [...] (healthy)` within ~2 minutes. If any fail:

```bash
sh tests/test-container-logs.sh
```

## Access

All services are exposed through the Kong API gateway on port **8000**.

| Service       | URL                                    |
|---------------|----------------------------------------|
| Studio (UI)   | http://your-server-ip:8000             |
| REST API      | http://your-server-ip:8000/rest/v1/    |
| Auth API      | http://your-server-ip:8000/auth/v1/    |
| Storage API   | http://your-server-ip:8000/storage/v1/ |
| Realtime API  | http://your-server-ip:8000/realtime/v1/ |

## Key Environment Variables

After running `generate-keys.sh`, these are populated in `.env`:

| Variable                   | Description                              |
|----------------------------|------------------------------------------|
| `POSTGRES_PASSWORD`        | Database password                        |
| `SUPABASE_PUBLISHABLE_KEY` | Client-side API key (safe to expose)     |
| `SUPABASE_SECRET_KEY`      | Server-side API key (never expose)       |
| `SUPABASE_PUBLIC_URL`      | URL passed to client libraries           |

## Firewall (UFW)

```bash
# API gateway + Studio
sudo ufw allow 8000/tcp

# PostgreSQL direct access (only if needed externally)
sudo ufw allow 5432/tcp
```

## Useful Commands

```bash
# Stop all services
docker compose down

# Stop and remove volumes (destroys all data)
docker compose down -v

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f db
docker compose logs -f studio

# Restart a single service
docker compose restart rest
```

## Backup & Restore

```bash
# Backup
docker compose exec db pg_dumpall -U postgres > backup.sql

# Restore
cat backup.sql | docker compose exec -T db psql -U postgres
```

## Mailing Pipeline Integration

After Supabase is running, configure the pipeline's `.env` file with these three variables:

```properties
supabase.url=http://your-server-ip:8000
supabase.anon-key=<SUPABASE_PUBLISHABLE_KEY from supabase-project/.env>
supabase.service-role-key=<SUPABASE_SECRET_KEY from supabase-project/.env>
```

The keys are found in `supabase-project/.env` after running `utils/generate-keys.sh`.

Before running the pipeline for the first time, apply the database migrations (see **Schema Migrations** below).

Verify connectivity with:

```bash
task sb-ping
```

## Schema Migrations

Migrations live in `supabase/migrations/` and follow the naming convention `V001__description.sql`, `V002__description.sql`, etc. Applied versions are tracked in a `schema_migrations` table inside PostgreSQL.

### Apply migrations

Copy the `supabase/` directory to the server (or clone the repo there), then run from `supabase-project/`:

```bash
bash /path/to/supabase/migrate.sh
```

The script iterates all `V*.sql` files in sorted order, skips already-applied ones, and records each version after applying it.

### Add a new migration

Create a new file in `supabase/migrations/` with the next version number:

```
supabase/migrations/V002__add_contact_status.sql
```

Then run `migrate.sh` again — it will apply only the new file.

## Upgrading

Stable releases publish monthly. Check the [changelog](https://github.com/supabase/supabase/blob/master/docker/CHANGELOG.md) before upgrading.

```bash
cd supabase-project
docker compose pull
docker compose down && docker compose up -d
```
