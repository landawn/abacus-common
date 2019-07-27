-- create db
-- DROP DATABASE IF EXISTS codes;
-- CREATE DATABASE codes DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS login;
DROP TABLE IF EXISTS account_device;
DROP TABLE IF EXISTS account;

-- account table.
CREATE TABLE account(
    id bigserial,
    gui varchar(64) NOT NULL,
    first_name varchar(32) NOT NULL,
    last_name varchar(32) NOT NULL,
    status int DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX gui_ind on account (gui);
CREATE INDEX first_name_ind on account (first_name);
CREATE INDEX last_name_ind on account (last_name);

-- login table
CREATE TABLE login(
    id bigserial,
    account_id bigint NOT NULL,
    login_id varchar(64) NOT NULL,
    login_password varchar(128) NOT NULL,
    status int DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE UNIQUE INDEX login_id_ind on login (login_id);

-- device table
CREATE TABLE account_device(
    id bigserial,
    account_id bigint NOT NULL,
    name varchar(64) NOT NULL,
    udid varchar(64) NOT NULL,
    platform varchar(64) DEFAULT NULL,
    model varchar(64) DEFAULT NULL,
    manufacturer varchar(64) DEFAULT NULL,
    produce_time timestamp,
    category varchar(32) DEFAULT NULL,
    description varchar(1024) DEFAULT NULL,
    status int NOT NULL DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE UNIQUE INDEX udid_ind on account_device (udid);
