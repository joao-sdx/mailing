# Configure web services

## Activation

Set active services ```$enabledServices``` in the ```config/Api.php``` file:

```php
/** List of active services. Available: dav, webservice */
public static $enabledServices = ['webservice'];
```

## Session lifetime configuration

```config/Security.php``` file:

```php
/** Maximum session lifetime from the time it was created (in minutes) */
public static $apiLifetimeSessionCreate = 1440;

/** Maximum session lifetime since the last modification (in minutes) */
public static $apiLifetimeSessionUpdate = 240;
```

## Create an API app

Add the services you want to use in the Web service - Applications admin
panel. https://SERVER/index.php?module=WebserviceApps&view=Index&parent=Settings

- wsAppName: WebserviceApps-name
- wsAppPass: WebserviceApps-password

Authorization: Basic base64_encode($name . ':' . $password)

## Create user for API

https://SERVER/index.php?module=Users&parent=Settings&view=List

- wsUserName: Users-user
- the password is not used by webservice

## Create webservice user

https://SERVER/index.php?module=WebserviceUsers&view=List&parent=Settings

This password is used by webservice

- wsUserPass: WebserviceUsers-pass

## Get API key

Get API key from https://SERVER/index.php?module=WebserviceApps&view=Index&parent=Settings

API KEY WebserviceApps-key 
