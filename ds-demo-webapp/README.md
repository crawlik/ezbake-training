# Datasets Demo App

#### Table of Contents

1. [Overview](#overview)
2. [Prerequisite](#iprerequisite)
3. [Install](#install)

## Overview

The app demonstrates an ability to interact securily and employ various visibility filters
with tree currently supported datastores.
Supported datastores are MongoDB 2.6, Elastic 1.5.2 and Postgres 9.3.

## Prerequisite

Your Postgres(PG) instance needs to be updated with ds_demo user and extended with ezbake_visibility
extension. SSH to your PG server.


```
sudo su -l  postgres -c "psql -c \"create user ds_demo with login createdb
password 'ds_demo';\""
sudo adduser ds_demo
sudo su -l ds_demo -c 'createdb ds_demo'
```

Edit `/var/lib/pgsql/9.3/data/postgresql.conf` and set the following values
```
listen_addresses = '*'
shared_preload_libraries = 'ezbake_visibility'
```

Edit `/var/lib/pgsql/9.3/data/pg_hba.conf` and allow passwordless access for
ds_demo user
```
# allow ds_demo user to connect w/o password
host    ds_demo ds_demo 10.0.1.0/24     trust
```

```
scp user@ezbake0:/etc/yum.repos.d/ezbake-open.repo /etc/yum.repos.d/
yum install -y ezbake-postgresql-visibility
service postgresql-9.3 restart
sudo su -l postgres -c "psql -U postgres -d ds_demo -c 'CREATE EXTENSION IF NOT EXISTS ezbake_visibility'"
```

## Install

Download dataset JARs using below commands

```
wget http://artifacts.chimpy.us:8081/artifactory/stronghold-libs-release/ezbake/data/ezmongo/2.1/ezmongo-2.1-jar-with-dependencies.jar
wget http://artifacts.chimpy.us:8081/artifactory/stronghold-libs-release/ezbake/data/ezelastic/2.1/ezelastic-2.1-jar-with-dependencies.jar
```

Register and approve ds_demo application using Registration and Admin UIs.
Obtain application id assigned to it to be used later as secuirty_id. Set
obtained security_id in all *-manifest.yml files. Install ezmongo-*.jar and
ezelastic-*.jar with related manifest and properties files.
