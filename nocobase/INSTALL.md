# NocoBase — Ubuntu Installation Guide

NocoBase runs under a dedicated `nocobase` system user.
All commands in sections 2–4 run as that user unless noted otherwise.

---

## 1. Create the dedicated user (run as root / sudo)

```sh
useradd -m -s /bin/bash nocobase
usermod -aG sudo nocobase          # optional: allow sudo for maintenance
mkdir -p /opt/nocobase
chown nocobase:nocobase /opt/nocobase
```

---

## 2. Install Docker (run as root / sudo)

```sh
apt-get update
apt-get install -y ca-certificates curl

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io \
                   docker-buildx-plugin docker-compose-plugin

# Allow the nocobase user to run Docker without sudo
usermod -aG docker nocobase

systemctl enable --now docker
```

The `docker` group membership only takes effect in new login sessions.
Apply it without logging out by running:

```sh
su - nocobase
```

Or, if you are already logged in as `nocobase`, reload the group in the current shell:

```sh
newgrp docker
```

> `newgrp` opens a new subshell. Ubuntu prints a welcome message
> ("To run a command as administrator…") when this happens — that is normal,
> not an error.

Verify the group is active and Docker is reachable:

```sh
groups          # should include "docker"
docker info     # should print engine info without permission errors
```

---

## 3. Configure NocoBase

Switch to the dedicated user:

```sh
su - nocobase
cd /opt/nocobase
```

Create `docker-compose.yml`:

```sh
cat > docker-compose.yml << 'EOF'
networks:
  nocobase:

services:
  nocobase:
    image: nocobase/nocobase:latest
    restart: unless-stopped
    networks:
      - nocobase
    depends_on:
      - postgres
    environment:
      APP_KEY: "${APP_KEY}"
      DB_DIALECT: postgres
      DB_HOST: postgres
      DB_PORT: 5432
      DB_DATABASE: nocobase
      DB_USER: nocobase
      DB_PASSWORD: "${DB_PASSWORD}"
    volumes:
      - nocobase_storage:/app/nocobase/storage
    ports:
      - "80:80"

  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    networks:
      - nocobase
    environment:
      POSTGRES_USER: nocobase
      POSTGRES_PASSWORD: "${DB_PASSWORD}"
      POSTGRES_DB: nocobase
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  nocobase_storage:
  postgres_data:
EOF
```

Create `.env` with strong secrets:

```sh
cat > .env << 'EOF'
# Generate with: openssl rand -base64 32
APP_KEY=change-me
DB_PASSWORD=change-me
EOF

chmod 600 .env
```

---

## 4. Start

```sh
docker compose up -d
```

NocoBase will be available at **http://cc.synapsedx.com**.

First-run setup wizard creates the **root** account — complete it immediately,
then disable public sign-up from Settings → Users & Permissions if not needed.

> **Important:** the UI Editor and system settings are only visible when
> logged in as the **root** user (the account created in the setup wizard).
> Regular admin accounts do not see these controls.

### Disable self-registration

By default the sign-in page shows a **"Create an account"** link.
To remove it (recommended for private deployments):

1. Sign in as the **root** user.
2. Navigate to `/admin/settings/auth/authenticators`.
3. Find the **Password** authenticator row, click its **⋯** menu →
   **Sign up settings**.
4. Set **Allow sign up** to **false**.
5. Submit.

The "Create an account" link disappears immediately from the sign-in page.

### Create a new page

1. Sign in as the **root** user.
2. In the top-right corner click the **UI Editor** icon (pencil/highlighter icon,
   between the bell and the user avatar). The interface switches to edit mode.
3. A **+** button appears at the end of the top navigation bar — click it.
4. Choose a type: **Page**, **Group** (dropdown), or **Link**.
5. Name the page and confirm. NocoBase navigates to the new empty page.
6. Inside the page click **Add block** to attach tables, forms, charts, etc.
7. Click the **UI Editor** icon again to exit edit mode.

---

## 5. Claude AI skills for NocoBase

The NocoBase CLI installs domain-knowledge skills that let Claude operate
your instance through natural language (data modelling, page building,
workflow automation).

**Requirements:** Node.js ≥ 22, npm.

### Install the CLI (once, on your workstation)

```sh
npm install -g @nocobase/cli@beta
nb --version          # verify
```

### Connect to this project's instance

```sh
nb init --ui
```

A browser wizard opens. Choose **Connect to an existing instance** and supply:

| Field | Value |
|---|---|
| API address | `http://cc.synapsedx.com` |
| Authentication | Password (root credentials) or API key |

The wizard writes a per-project config file. Set `NB_CLI_ROOT` if you want
to store it somewhere other than your home directory.

#### Using password authentication (simplest)

When the wizard asks for authentication choose **password** and enter the
root account credentials. No extra setup required.

#### Using an API key

The API keys plugin must be enabled first — this requires the root account:

1. Sign in as **root**.
2. Navigate to `/admin/pm/list` (Plugin Manager).
3. Find **Authentication: API keys** → **Enable**.
4. Click the user avatar (top-right) → **API keys** → **Add API key**.
5. Set a name and expiry, confirm. **Copy the token immediately** — it is
   only shown once.
6. Paste it into the `nb init --ui` wizard when prompted.

> The API keys plugin is Community Edition — no licence needed, just activation.

### Use in Claude Code

Restart your Claude Code session after `nb init` completes.
Claude can now manage the NocoBase instance at `cc.synapsedx.com` directly.

---

## 6. Useful commands (run as `nocobase`)

```sh
# View logs
docker compose logs -f nocobase

# Stop
docker compose down

# Upgrade
docker compose pull && docker compose up -d

# Backup Postgres
docker compose exec postgres pg_dump -U nocobase nocobase > backup-$(date +%F).sql
```
