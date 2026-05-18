# Twenty CRM — Self-Hosted Installation

## Prerequisites

- Docker + Docker Compose
- A server or VM with at least 2 GB RAM
- A dedicated user (recommended)

## 1. Create a user

```sh
useradd -m -s /bin/bash twenty
usermod -aG docker twenty
mkdir /opt/twenty && chown twenty:twenty /opt/twenty
su - twenty
```

## 2. Get the Docker Compose file

```sh
cd /opt/twenty
curl -fsSL https://raw.githubusercontent.com/twentyhq/twenty/main/packages/twenty-docker/docker-compose.yml -o docker-compose.yml
curl -fsSL https://raw.githubusercontent.com/twentyhq/twenty/main/packages/twenty-docker/.env.example -o .env
```

## 3. Configure environment

Edit `.env` and set at minimum:

```dotenv
# Generate with: openssl rand -base64 32
APP_SECRET=your-secret-here

# Public URL of the instance
SERVER_URL=http://your-server-ip

# Postgres password (change before first start)
POSTGRES_PASSWORD=your-db-password

# Disable new workspace creation after initial setup
IS_SIGN_UP_DISABLED=true
```

> Set `IS_SIGN_UP_DISABLED=true` **after** completing the setup wizard and creating your workspace, otherwise the wizard itself will be blocked.

## 4. Expose on port 80

In `docker-compose.yml`, find the `twenty-server` service and change the port mapping:

```yaml
ports:
  - "80:3000"
```

## 5. Start

```sh
docker compose up -d
```

Twenty starts three services:
- `twenty-server` — API + background workers (exposed on port 80)
- `twenty-db` — PostgreSQL
- `twenty-redis` — Redis (job queue)

## 6. Access

Open `http://your-server-ip` in a browser and complete the setup wizard to create the first workspace and admin account.

## 7. Backup

Data lives in two named Docker volumes:

```sh
# Backup
docker run --rm -v twenty_db_data:/data -v $(pwd):/backup alpine \
  tar czf /backup/twenty-db-$(date +%Y%m%d).tar.gz -C /data .

docker run --rm -v twenty_server_local_data:/data -v $(pwd):/backup alpine \
  tar czf /backup/twenty-files-$(date +%Y%m%d).tar.gz -C /data .
```

## 8. Upgrade

```sh
docker compose pull
docker compose up -d
```

## Uninstall

```sh
docker compose down
docker volume rm twenty_db_data twenty_server_local_data
rm -rf /opt/twenty
```

## API access

Twenty exposes a **GraphQL** API at `http://your-server/api` and a **REST** (metadata) API at `http://your-server/rest`.

Generate an API key: Settings → API & Webhooks → Generate.
