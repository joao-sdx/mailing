# YetiForce CRM — Self-Hosted Installation

YetiForce is a B2B-oriented open source CRM built on PHP + MariaDB.

## Prerequisites

- Debian 12 / Ubuntu 22.04+
- 2 GB RAM minimum
- PHP 8.1–8.2
- MariaDB 10.6+
- Apache 2.4+

## 1. Create a user

```sh
useradd -m -s /bin/bash yetiforce
```

## 2. Install dependencies

```sh
apt update
apt install -y lsb-release ca-certificates curl

# Add PHP 8.2 repository (not in default Debian/Ubuntu repos)
curl -sSL https://packages.sury.org/php/apt.gpg -o /etc/apt/trusted.gpg.d/php.gpg
echo "deb https://packages.sury.org/php/ $(lsb_release -sc) main" > /etc/apt/sources.list.d/php.list
apt update

apt install -y apache2 mariadb-server \
  php8.2 php8.2-{cli,mysql,curl,gd,imap,mbstring,soap,xml,zip,bcmath,intl,opcache,apcu} \
  php-pear php8.2-dev libmagickwand-dev imagemagick

# Build and install imagick via PECL
pecl install imagick
echo "extension=imagick.so" > /etc/php/8.2/mods-available/imagick.ini
phpenmod imagick
```

## 3. Create database

```sh
mysql -u root <<'SQL'
CREATE DATABASE yetiforce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'yetiforce'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL ON yetiforce.* TO 'yetiforce'@'localhost';
FLUSH PRIVILEGES;
SQL
```

## 4. Configure MariaDB

Create `/etc/mysql/mariadb.conf.d/99-yetiforce.cnf`:

```ini
[mysqld]
innodb_lock_wait_timeout = 600
```

```sh
systemctl restart mariadb
```

## 5. Download YetiForce

```sh
cd /var/www
curl -L https://github.com/YetiForceCompany/YetiForceCRM/releases/latest/download/YetiForceCRM.zip -o yetiforce.zip
unzip yetiforce.zip -d yetiforce
chown -R yetiforce:www-data yetiforce
chmod -R 755 yetiforce
mkdir -p yetiforce/{cache,config,logs,storage,user_privileges,app_data/shop,languages,modules,public_html/modules/OSSMail,public_html/libraries,public_html/layouts/resources/Logo}
chmod -R 775 yetiforce/{cache,config,logs,storage,user_privileges}
chmod -R 775 yetiforce/{app_data,languages,install,modules}
chmod -R 775 yetiforce/{public_html/modules/OSSMail,public_html/libraries,public_html/layouts/resources/Logo}
chmod 664 yetiforce/cron.php yetiforce/app_data/moduleHierarchy.php yetiforce/app_data/icons.php yetiforce/app_data/libraries.json
```

## 5. Configure Apache

Create `/etc/apache2/sites-available/yetiforce.conf`:

```apache
<VirtualHost *:80>
    ServerName cc.synapsedx.com
    DocumentRoot /var/www/yetiforce
    DirectoryIndex index.php

    <Directory /var/www/yetiforce>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/yetiforce_error.log
    CustomLog ${APACHE_LOG_DIR}/yetiforce_access.log combined
</VirtualHost>
```

```sh
a2ensite yetiforce
a2enmod rewrite
a2dissite 000-default
systemctl reload apache2
```

## 6. Configure PHP

Edit `/etc/php/8.2/apache2/php.ini`:

```ini
memory_limit = 2G
upload_max_filesize = 100M
post_max_size = 100M
max_execution_time = 600
date.timezone = Europe/Paris
session.use_strict_mode = 1
session.cookie_samesite = Lax
disable_functions = shell_exec, exec, system, passthru, popen
display_errors = Off
display_startup_errors = Off
log_errors = On
error_log = /var/log/php8.2-error.log
error_reporting = E_ALL & ~E_DEPRECATED & ~E_STRICT
```

```sh
systemctl restart apache2
```

## 7. Run the installer

Open `http://cc.synapsedx.com` — the web installer will guide you through:
1. System requirements check
2. Database connection: host `localhost`, user `yetiforce`, password from step 3
3. Admin account creation
4. Default data import

## 8. Backup

```sh
# Database
mariadb-dump -u yetiforce -p yetiforce > backup-$(date +%Y%m%d).sql

# Files
tar czf yetiforce-files-$(date +%Y%m%d).tar.gz -C /var/www yetiforce
```

## 9. Upgrade

YetiForce has a built-in updater: Admin Panel → Updates → Check for updates.

## Uninstall

```sh
rm -rf /var/www/yetiforce
mysql -u root -e "DROP DATABASE yetiforce; DROP USER 'yetiforce'@'localhost';"
a2dissite yetiforce
systemctl reload apache2
```

## API access

REST API at `http://your-server/webservice/`.

Generate credentials: Admin Panel → Integration → Web Service → Add application.
