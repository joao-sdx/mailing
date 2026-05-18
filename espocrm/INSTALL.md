# EspoCRM — Installation Guide

Target: Ubuntu 22.04 / Debian 12.

---

## 1. Create a dedicated user

```bash
sudo useradd -m -s /bin/bash espocrm
sudo usermod -aG docker espocrm
```

Pre-create the data directories before first boot:

```bash
sudo mkdir -p /opt/espocrm/data/mysql /opt/espocrm/data/espocrm
sudo chmod 750 /opt/espocrm/data/mysql /opt/espocrm/data/espocrm
sudo chown -R $(id -u):$(id -g) /opt/espocrm
```

---

## 2. Install Docker

```bash
# Add Docker's official GPG key:
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update

sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

```

Log out and back in for the `docker` group to take effect.

---

## 3. Install EspoCRM

```bash
# Switch to the espocrm user
sudo -iu espocrm

# Clone or copy the project files to /opt/espocrm
cp -r /path/to/mailing/espocrm/* /opt/espocrm/
cd /opt/espocrm

# Configure
cp .env.example .env
nano .env   # set strong passwords and ESPOCRM_SITE_URL

# Start
docker compose up -d

# Check logs
docker compose logs -f espocrm
```

EspoCRM will be available at the URL defined in `ESPOCRM_SITE_URL` (default: `http://localhost:8080`).

---

## 4. Backup

All persistent data lives under `./data/`. A simple backup:

```bash
docker compose stop
tar -czf espocrm-backup-$(date +%Y%m%d).tar.gz ./data
docker compose start
```

Restore:

```bash
docker compose stop
rm -rf ./data
tar -xzf espocrm-backup-YYYYMMDD.tar.gz
docker compose start
```
