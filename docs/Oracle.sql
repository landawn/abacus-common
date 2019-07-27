-- create db
-- DROP DATABASE IF EXISTS codes;
-- CREATE DATABASE codes DEFAULT CHARACTER SET utf8;

-- begin
-- execute immediate 'drop table login';
-- exception when others then null;
-- end;

DROP TABLE login;
DROP TABLE account_device;
DROP TABLE account;

DROP SEQUENCE codes_seq;
CREATE SEQUENCE codes_seq start with 10000000 increment by 1 nomaxvalue;

-- account table.
CREATE TABLE account(
    id NUMBER(20),
    gui varchar(64) NOT NULL,
    first_name varchar(32) NOT NULL,
    last_name varchar(32) NOT NULL,
    status NUMBER(3) DEFAULT 0,
    last_update_time timestamp DEFAULT SYSDATE,
    create_time timestamp DEFAULT SYSDATE,
    UNIQUE(gui),
    PRIMARY KEY (id)
);
CREATE INDEX account_first_name_ind on account (first_name);
CREATE INDEX account_last_name_ind on account (last_name);

-- login table
CREATE TABLE login(
    id NUMBER(20),
    account_id NUMBER(20) NOT NULL,
    login_id varchar(64) NOT NULL,
    login_password varchar(128) NOT NULL,
    status NUMBER(6) DEFAULT 0,
    last_update_time timestamp DEFAULT SYSDATE,
    create_time timestamp DEFAULT SYSDATE,
    UNIQUE(login_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON DELETE CASCADE
);

-- device table
CREATE TABLE account_device(
    id NUMBER(20),
    account_id NUMBER(20) NOT NULL,
    name varchar(64) NOT NULL,
    udid varchar(64) NOT NULL,
    platform varchar(64) DEFAULT NULL,
    model varchar(64) DEFAULT NULL,
    manufacturer varchar(64) DEFAULT NULL,
    produce_time timestamp,
    category varchar(32) DEFAULT NULL,
    description varchar(1024) DEFAULT NULL,
    status NUMBER(6) DEFAULT 0,
    last_update_time timestamp DEFAULT SYSDATE,
    create_time timestamp DEFAULT SYSDATE,
    UNIQUE(udid),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON DELETE CASCADE
);
