-- create db
-- DROP DATABASE IF EXISTS codes;
-- CREATE DATABASE codes DEFAULT CHARACTER SET utf8;

DROP TABLE login;
DROP TABLE account_device;
DROP TABLE account;

-- account table.
CREATE TABLE account(
    id bigint IDENTITY,
    gui varchar(64) NOT NULL,
    first_name varchar(32) NOT NULL,
    last_name varchar(32) NOT NULL,
    status tinyint DEFAULT 0,
    last_update_time datetime DEFAULT GETDATE(),
    create_time datetime NOT NULL,
    UNIQUE(gui),
    PRIMARY KEY (id)
);
CREATE INDEX account_first_name_ind on account (first_name);
CREATE INDEX account_last_name_ind on account (last_name);

-- login table
CREATE TABLE login(
    id bigint IDENTITY,
    account_id bigint NOT NULL,
    login_id varchar(64) NOT NULL,
    login_password varchar(128) NOT NULL,
    status tinyint DEFAULT 0,
    last_update_time datetime DEFAULT GETDATE(),
    create_time datetime NOT NULL,
    UNIQUE(login_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON DELETE CASCADE
);

-- device table
CREATE TABLE account_device(
    id bigint IDENTITY,
    account_id bigint NOT NULL,
    name varchar(64) NOT NULL,
    udid varchar(64) NOT NULL,
    platform varchar(64) DEFAULT NULL,
    model varchar(64) DEFAULT NULL,
    manufacturer varchar(64) DEFAULT NULL,
    produce_time datetime,
    category varchar(32) DEFAULT NULL,
    description varchar(1024) DEFAULT NULL,
    status tinyint DEFAULT 0,
    last_update_time datetime DEFAULT GETDATE(),
    create_time datetime NOT NULL,
    UNIQUE(udid),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON DELETE CASCADE
);
