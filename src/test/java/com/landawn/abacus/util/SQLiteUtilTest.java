/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import junit.framework.TestCase;

public class SQLiteUtilTest extends TestCase {

    //    @Test
    //    public void test_insert() {
    //        final File dbFile = new File("./src/test/resources/sqliteDB");
    //        final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
    //
    //        db.execSQL("CREATE TABLE account(\r\n" + "    id bigint(20) NOT NULL AUTO_INCREMENT,\r\n" + "    gui varchar(64) NOT NULL,\r\n"
    //                + "    email_address varchar(64),\r\n" + "    first_name varchar(32) NOT NULL,\r\n" + "    middle_name varchar(32),\r\n"
    //                + "    last_name varchar(32) NOT NULL,\r\n" + "    birth_date date,\r\n" + "    status int NOT NULL DEFAULT 0,\r\n"
    //                + "    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\r\n"
    //                + "    create_time timestamp NOT NULL,\r\n" + "    UNIQUE (gui),\r\n" + "    UNIQUE (email_address),\r\n"
    //                + "    INDEX first_name_ind (first_name),\r\n" + "    INDEX last_name_ind (last_name),\r\n" + "    INDEX birth_date_ind (birth_date),\r\n"
    //                + "    PRIMARY KEY (id)\r\n" + ")");
    //    }

}
