-- create db
-- DROP DATABASE IF EXISTS codes;
-- CREATE DATABASE codes DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS login;
DROP TABLE IF EXISTS account_device;
DROP TABLE IF EXISTS account;

-- account table.
CREATE TABLE account(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    gui varchar(64) NOT NULL,
    first_name varchar(32) NOT NULL,
    last_name varchar(32) NOT NULL,
    status int NOT NULL DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    UNIQUE (gui),
    INDEX first_name_ind (first_name),
    INDEX last_name_ind (last_name),
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;

-- login table
CREATE TABLE login(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    account_id bigint(20) NOT NULL,
    login_id varchar(64) NOT NULL,
    login_password varchar(128) NOT NULL,
    status int NOT NULL DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    UNIQUE (login_id),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;

-- device table
CREATE TABLE account_device(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    account_id bigint(20) NOT NULL,
    name varchar(64) NOT NULL,
    udid varchar(64) NOT NULL,
    platform varchar(64) DEFAULT NULL,
    model varchar(64) DEFAULT NULL,
    manufacturer varchar(64) DEFAULT NULL,
    produce_time date,
    category varchar(32) DEFAULT NULL,
    description varchar(1024) DEFAULT NULL,
    status int NOT NULL DEFAULT 0,
    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL,
    UNIQUE (udid),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id)
        REFERENCES account (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;
