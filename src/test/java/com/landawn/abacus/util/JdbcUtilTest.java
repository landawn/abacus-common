/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class JdbcUtilTest extends AbstractTest {

    @Test
    public void test_01() {
        // dummy test.
    }

    //
    //    public void test_retrieve_sql() throws Exception {
    //        try (final Connection conn = ds.getConnection(); //
    //                final PreparedStatement stmt = conn.prepareStatement("select * from account")) {
    //            N.println(stmt.toString());
    //        }
    //    }
    //
    //    public void test_comments() throws Exception {
    //
//        // @formatter:off
//        String sql =
//                "/*test comments*/\r\n" +
//                "SELECT \r\n" +
//                "    *\r\n" +
//                "FROM\r\n" +
//                "-- test comments\r\n" +
//                "    account\r\n" +
//                "    \r\n" +
//                "-- test comments\r\n" +
//                "WHERE\r\n" +
//                "    id > 0 AND first_name = 'a\\'\\nb'\r\n" +
//                "    \r\n" +
//                "/* test comments*/\r\n;";
//
//        // @formatter:on
    //
    //        String newSql = ParsedSql.parse(sql).getParameterizedSql();
    //        N.println(newSql);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        JdbcUtil.prepareQuery(ds, MySqlDef.insertAccount).setParameters("fn", "ln", uuid, now, now).execute();
    //
    //        JdbcUtil.prepareQuery(ds, newSql).list(Account.class).forEach(Fn.println());
    //
    //        JdbcUtil.prepareQuery(ds, "delete from account").execute();
    //    }
    //
    //    public void test_comments_2() throws Exception {
    //
//        // @formatter:off
//        String sql =
//                "-- Keep comments\r\n" +
//                "/*test comments*/\r\n" +
//                "SELECT \r\n" +
//                "    *\r\n" +
//                "FROM\r\n" +
//                "-- test comments\r\n" +
//                "    account\r\n" +
//                "    \r\n" +
//                "-- test comments\r\n" +
//                "WHERE\r\n" +
//                "    id > 0 AND first_name = 'a\\'\\nb'\r\n" +
//                "    \r\n" +
//                "/* test comments*/\r\n;";
//
//        // @formatter:on
    //
    //        String newSql = ParsedSql.parse(sql).getParameterizedSql();
    //        N.println(newSql);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        JdbcUtil.prepareQuery(ds, MySqlDef.insertAccount).setParameters("fn", "ln", uuid, now, now).execute();
    //
    //        JdbcUtil.prepareQuery(ds, newSql).list(Account.class).forEach(Fn.println());
    //
    //        JdbcUtil.prepareQuery(ds, "delete from account").execute();
    //    }
    //
    //    public void test_procedure() throws Exception {
    //        JdbcUtil.prepareCallableQuery(ds, "CALL display_account(?)").setInt(1, 0).list(Account.class).forEach(Fn.println());
    //
    //        JdbcUtil.prepareNamedQuery(ds, "select * from account where id >= :id").setInt(1, Integer.MAX_VALUE).list(Account.class).forEach(Fn.println());
    //
    //        JdbcUtil.prepareNamedQuery(ds, "CALL display_account(?)").setInt(1, Integer.MAX_VALUE).list(Account.class).forEach(Fn.println());
    //    }
    //
    //    public void test_NamedQuery() throws Exception {
    //        new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        String sql_insertAccount = NSC.insertInto(Account.class, N.asSet("id")).sql();
    //        Account account = N.fill(Account.class);
    //        account.setGUI(uuid);
    //        JdbcUtil.prepareNamedQuery(ds, sql_insertAccount).setParameters(account).execute();
    //
    //        long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //        account.setId(id);
    //
    //        String sql_selectAccountById = NSC.selectFrom(Account.class).where(CF.eq("id")).sql();
    //        DataSet rs = JdbcUtil.prepareNamedQuery(ds, sql_selectAccountById).setLong(1, id).query();
    //        assertEquals(uuid, rs.get("gui"));
    //
    //        String gui = JdbcUtil.prepareNamedQuery(ds, sql_selectAccountById).setParameters(account).stream(Account.class).first().get().getGUI();
    //        assertEquals(uuid, gui);
    //
    //        gui = JdbcUtil.prepareNamedQuery(ds, sql_selectAccountById)
    //                .setParameters(N.asMap("id", id))
    //                .stream(Account.class)
    //                .skip(0)
    //                .limit(10)
    //                .first()
    //                .get()
    //                .getGUI();
    //        assertEquals(uuid, gui);
    //
    //        String sql_deleteAccountById = NSC.deleteFrom(Account.class).where("id = :id").sql();
    //        assertFalse(JdbcUtil.prepareNamedQuery(ds, sql_deleteAccountById).setLong(1, id).execute());
    //
    //        account = JdbcUtil.prepareNamedQuery(ds, sql_selectAccountById).setLong(1, id).findFirst(Account.class).orElseNull();
    //        assertNull(account);
    //
    //        //        account = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).tried().val().setLong(1, id).findFirst(Account.class).orElseNull();
    //        //        assertNull(account);
    //    }
    //
    //    public void test_PreparedQuery() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        JdbcUtil.prepareQuery(ds, MySqlDef.insertAccount).setParameters("fn", "ln", uuid, now, now).execute();
    //
    //        long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //
    //        DataSet rs = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).setLong(1, id).query();
    //        assertEquals(uuid, rs.get("gui"));
    //
    //        String gui = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).setLong(1, id).stream(Account.class).first().get().getGUI();
    //        assertEquals(uuid, gui);
    //
    //        gui = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).setLong(1, id).stream(Account.class).skip(0).limit(10).first().get().getGUI();
    //        assertEquals(uuid, gui);
    //
    //        assertFalse(JdbcUtil.prepareQuery(ds, MySqlDef.deleteAccountById).setLong(1, id).execute());
    //
    //        Account account = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).setLong(1, id).findFirst(Account.class).orElseNull();
    //        assertNull(account);
    //
    //        //        account = JdbcUtil.prepareQuery(ds, MySqlDef.selectAccountById).tried().val().setLong(1, id).findFirst(Account.class).orElseNull();
    //        //        assertNull(account);
    //    }
    //
    //    public void testRetrieveResult() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        PreparedStatement stmt = conn.prepareStatement(MySqlDef.selectAccountById);
    //        stmt.setLong(1, id);
    //
    //        java.sql.ResultSet rs = stmt.executeQuery();
    //        DataSet resultList = JdbcUtil.extractData(rs);
    //
    //        resultList.println();
    //
    //        JdbcUtil.closeQuietly(rs);
    //
    //        rs = stmt.executeQuery();
    //        resultList = JdbcUtil.extractData(rs, true);
    //
    //        resultList.println();
    //        try {
    //            JdbcUtil.extractData(rs, true);
    //
    //            fail("Should throw SQLException");
    //        } catch (SQLException e) {
    //
    //        }
    //
    //        rs = stmt.executeQuery();
    //        resultList = JdbcUtil.extractData(rs, 1, 1);
    //        resultList.println();
    //        assertEquals(0, resultList.size());
    //
    //        JdbcUtil.closeQuietly(rs);
    //        rs = stmt.executeQuery();
    //        resultList = JdbcUtil.extractData(rs, 0, 0);
    //        resultList.println();
    //        assertEquals(0, resultList.size());
    //
    //        JdbcUtil.closeQuietly(rs, stmt, conn);
    //    }
    //
    //    public void test_prepareCall() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        CallableStatement stmt = JdbcUtil.prepareCall(conn, MySqlDef.insertAccount, "fn", "ln", uuid, now, now);
    //
    //        stmt.executeUpdate();
    //        JdbcUtil.closeQuietly(stmt);
    //
    //        long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //
    //        stmt = JdbcUtil.prepareCall(conn, MySqlDef.selectAccountById, N.asList(id));
    //
    //        java.sql.ResultSet rs = stmt.executeQuery();
    //
    //        JdbcUtil.closeQuietly(rs, stmt);
    //
    //        JdbcUtil.execute(conn, MySqlDef.deleteAccountById, id);
    //
    //        Account account = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null);
    //        assertNull(account);
    //
    //        JdbcUtil.execute(conn, MySqlDef.deleteAccountById, N.asList(id));
    //
    //        JdbcUtil.closeQuietly(conn);
    //    }
    //
    //    //    public void test_prepareStatement() throws Exception {
    //    //        Connection conn = sqlExecutor.getConnection();
    //    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //    //        String uuid = Strings.uuid();
    //    //        PreparedStatement stmt = JdbcUtil.prepareStmt(conn, MySqlDef.insertAccount, "fn", "ln", uuid, now, now);
    //    //
    //    //        stmt.executeUpdate();
    //    //        JdbcUtil.closeQuietly(stmt);
    //    //
    //    //        long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //    //
    //    //        JdbcUtil.execute(conn, MySqlDef.deleteAccountById, id);
    //    //
    //    //        Account account = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null);
    //    //        assertNull(account);
    //    //
    //    //        JdbcUtil.execute(conn, MySqlDef.deleteAccountById, N.asList(id));
    //    //
    //    //        JdbcUtil.closeQuietly(conn);
    //    //    }
    //
    //    public void test_execute() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        String uuid = Strings.uuid();
    //        JdbcUtil.executeUpdate(conn, MySqlDef.insertAccount, "fn", "ln", uuid, now, now);
    //
    //        long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //
    //        DataSet rs = JdbcUtil.executeQuery(conn, MySqlDef.selectAccountById, id);
    //        assertEquals(uuid, rs.get("gui"));
    //
    //        rs = JdbcUtil.executeQuery(conn, MySqlDef.selectAccountById, N.asList(id));
    //        assertEquals(uuid, rs.get("gui"));
    //
    //        JdbcUtil.executeUpdate(conn, MySqlDef.deleteAccountById, id);
    //
    //        Account account = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null);
    //        assertNull(account);
    //
    //        JdbcUtil.closeQuietly(conn);
    //    }
    //
    //    public void test_execute_2() throws Exception {
    //        for (int i = 0; i < 1; i++) {
    //            Connection conn = sqlExecutor.getConnection();
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            String uuid = Strings.uuid();
    //            JdbcUtil.executeUpdate(conn, MySqlDef.insertAccount, N.asList("fn", "ln", uuid, now, now));
    //
    //            long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //
    //            DataSet rs = JdbcUtil.executeQuery(conn, MySqlDef.selectAccountById, id);
    //            assertEquals(uuid, rs.get("gui"));
    //
    //            rs = JdbcUtil.executeQuery(conn, MySqlDef.selectAccountById, N.asList(id));
    //            assertEquals(uuid, rs.get("gui"));
    //
    //            JdbcUtil.executeUpdate(conn, MySqlDef.deleteAccountById, N.asList(id));
    //
    //            Account account = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null);
    //            assertNull(account);
    //
    //            JdbcUtil.closeQuietly(conn);
    //        }
    //    }
    //
    //    public void test_parse() throws Exception {
    //        for (int i = 0; i < 1; i++) {
    //            Connection conn = sqlExecutor.getConnection();
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            String uuid = Strings.uuid();
    //            JdbcUtil.executeUpdate(conn, MySqlDef.insertAccount, N.asList("fn", "ln", uuid, now, now));
    //
    //            long id = sqlExecutor.queryForLong("select id from account where gui = ?", uuid).get();
    //
    //            //        JdbcUtil.parse(conn, MySqlDef.selectAccountById, N.asList(id), new Consumer<Object[]>() {
    //            //            @Override
    //            //            public void accept(Object[] row) {
    //            //                N.println(row);
    //            //            }
    //            //        });
    //            //
    //            //        JdbcUtil.parse(conn, MySqlDef.selectAccountById, N.asList(id), 1, 1, new Consumer<Object[]>() {
    //            //            @Override
    //            //            public void accept(Object[] row) {
    //            //                N.println(row);
    //            //            }
    //            //        });
    //
    //            JdbcUtil.executeUpdate(conn, MySqlDef.deleteAccountById, N.asList(id));
    //
    //            Account account = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null);
    //            assertNull(account);
    //
    //            JdbcUtil.closeQuietly(conn);
    //        }
    //    }
    //
    //    public void test_parse_2() throws Exception {
    //
    //        for (int i = 0; i < 1; i++) {
    //            Connection conn = sqlExecutor.getConnection();
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            String uuid = Strings.uuid();
    //            JdbcUtil.executeUpdate(conn, MySqlDef.insertAccount, N.asList("fn", "ln", uuid, now, now));
    //
    //            long id = sqlExecutor.queryForLong("select id from account where gui = ? and id > -" + (i % 300), uuid).get();
    //
    //            JdbcUtil.executeUpdate(conn, MySqlDef.deleteAccountById, N.asList(id));
    //            String sql = "select * from account WHERE account.id = ? and id > -" + (i % 300);
    //            Account account = sqlExecutor.findFirst(Account.class, sql, id).orElse(null);
    //            assertNull(account);
    //
    //            JdbcUtil.closeQuietly(conn);
    //        }
    //    }
    //
    //    @Test
    //    public void testReplicate() throws Exception {
    //        String sql_drop = "DROP TABLE IF EXISTS account_copy;";
    //
    //        try {
    //            sqlExecutor.update(sql_drop);
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //
    //        String sql_create = "CREATE TABLE account_copy(" + "    id bigint(20) NOT NULL AUTO_INCREMENT," + "    gui varchar(64) NOT NULL,"
    //                + "    email_address varchar(64)," + "    first_name varchar(32) NOT NULL," + "    middle_name varchar(32),"
    //                + "    last_name varchar(32) NOT NULL," + "    birth_date datetime," + "    status int NOT NULL DEFAULT 0,"
    //                + "    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," + "    create_time timestamp NOT NULL,"
    //                + "    UNIQUE gui_ind (gui)," + "    UNIQUE email_ind (email_address)," + "    INDEX first_name_ind (first_name),"
    //                + "    INDEX middle_name_ind (middle_name)," + "    INDEX last_name_ind (last_name)," + "    INDEX birth_date_ind (birth_date),"
    //                + "    PRIMARY KEY (id)" + ") ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;";
    //
    //        sqlExecutor.update(sql_create);
    //
    //        // #############################################
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Account[] accounts = new Account[101];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            account.setFirstName("firstName");
    //            account.setLastName("lastName");
    //            account.setGUI(UUID.randomUUID().toString());
    //
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //
    //        // #############################################
    //        String driver = "com.mysql.jdbc.Driver";
    //        String url = "jdbc:mysql://localhost:3306/abacustest";
    //        String user = "root";
    //        String password = "admin";
    //        Class<? extends Driver> driverClass = ClassUtil.forClass(driver);
    //        Connection sourceConn = JdbcUtil.createConnection(driverClass, url, user, password);
    //        Connection targetConn = JdbcUtil.createConnection(driverClass, url, user, password);
    //
    //        String sql_select = "select id, gui, email_address, first_name, middle_name, last_name, birth_date, status, last_update_time, create_time from account where id > 0";
    //        String sql_insert = "insert into account_copy (id, gui, email_address, first_name, middle_name, last_name, birth_date, status, last_update_time, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    //        JdbcUtils.copy(sourceConn, sql_select, targetConn, sql_insert);
    //
    //        DataSet result = sqlExecutor.query("select * from account_copy");
    //
    //        assertEquals(accounts.length, result.size());
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        JdbcUtil.closeQuietly(sourceConn);
    //        JdbcUtil.closeQuietly(targetConn);
    //    }
    //
    //    @Test
    //    public void testReplicate_2() throws Exception {
    //        String sql_drop = "DROP TABLE IF EXISTS account_copy;";
    //
    //        try {
    //            sqlExecutor.update(sql_drop);
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //
    //        String sql_create = "CREATE TABLE account_copy(" + "    id bigint(20) NOT NULL AUTO_INCREMENT," + "    gui varchar(64) NOT NULL,"
    //                + "    email_address varchar(64)," + "    first_name varchar(32) NOT NULL," + "    middle_name varchar(32),"
    //                + "    last_name varchar(32) NOT NULL," + "    birth_date datetime," + "    status int NOT NULL DEFAULT 0,"
    //                + "    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," + "    create_time timestamp NOT NULL,"
    //                + "    UNIQUE gui_ind (gui)," + "    UNIQUE email_ind (email_address)," + "    INDEX first_name_ind (first_name),"
    //                + "    INDEX middle_name_ind (middle_name)," + "    INDEX last_name_ind (last_name)," + "    INDEX birth_date_ind (birth_date),"
    //                + "    PRIMARY KEY (id)" + ") ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;";
    //
    //        sqlExecutor.update(sql_create);
    //
    //        // #############################################
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Account[] accounts = new Account[101];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            account.setFirstName("firstName");
    //            account.setLastName("lastName");
    //            account.setGUI(UUID.randomUUID().toString());
    //
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //
    //        // #############################################
    //        String driver = "com.mysql.jdbc.Driver";
    //        String url = "jdbc:mysql://localhost:3306/abacustest";
    //        String user = "root";
    //        String password = "admin";
    //        Class<? extends Driver> driverClass = ClassUtil.forClass(driver);
    //        Connection sourceConn = JdbcUtil.createConnection(driverClass, url, user, password);
    //        Connection targetConn = JdbcUtil.createConnection(driverClass, url, user, password);
    //
    //        String sql_select = "select id, gui, email_address, first_name, middle_name, last_name, birth_date, status, last_update_time, create_time from account where id > 0";
    //        String sql_insert = "insert into account_copy (id, gui, email_address, first_name, middle_name, last_name, birth_date, status, last_update_time, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    //        JdbcUtils.copy(sourceConn, sql_select, 200, 0, Long.MAX_VALUE, targetConn, sql_insert, null, 200, 0, true);
    //
    //        DataSet result = sqlExecutor.query("select * from account_copy");
    //
    //        assertEquals(accounts.length, result.size());
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        JdbcUtil.closeQuietly(sourceConn);
    //        JdbcUtil.closeQuietly(targetConn);
    //    }
    //
    //    @Test
    //    public void test_close() throws Exception {
    //        for (int i = 0; i < 1000; i++) {
    //            Connection conn = sqlExecutor.getConnection();
    //            PreparedStatement stmt = conn.prepareStatement(MySqlDef.selectAccountById);
    //            stmt.setLong(1, 1);
    //
    //            java.sql.ResultSet rs = stmt.executeQuery();
    //
    //            JdbcUtil.closeQuietly(rs, stmt, conn);
    //            JdbcUtil.closeQuietly(rs, stmt);
    //            JdbcUtil.closeQuietly(stmt, conn);
    //
    //            JdbcUtil.closeQuietly(conn);
    //        }
    //    }
    //
    //    @Test
    //    public void test_table() throws Exception {
    //        String tableName = "login";
    //        String schema = "CREATE TABLE login(\r\n" + "    id bigint(20) NOT NULL AUTO_INCREMENT,\r\n" + "    account_id bigint(20) NOT NULL,\r\n"
    //                + "    login_id varchar(64) NOT NULL,\r\n" + "    login_password varchar(128) NOT NULL,\r\n" + "    status int NOT NULL DEFAULT 0,\r\n"
    //                + "    last_update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\r\n"
    //                + "    create_time timestamp NOT NULL,\r\n" + "    UNIQUE (login_id),\r\n" + "    PRIMARY KEY (id),\r\n" + "    FOREIGN KEY (account_id)\r\n"
    //                + "        REFERENCES account (id)\r\n" + "        ON UPDATE CASCADE ON DELETE RESTRICT\r\n"
    //                + ") ENGINE=InnoDB AUTO_INCREMENT=100000000 DEFAULT CHARSET=utf8;\r\n" + "";
    //        Connection conn = sqlExecutor.getConnection();
    //
    //        assertTrue(JdbcUtil.doesTableExist(conn, tableName));
    //        assertFalse(JdbcUtil.createTableIfNotExists(conn, tableName, schema));
    //
    //        assertTrue(JdbcUtil.dropTableIfExists(conn, tableName));
    //
    //        assertFalse(JdbcUtil.doesTableExist(conn, tableName));
    //        assertFalse(JdbcUtil.dropTableIfExists(conn, tableName));
    //
    //        assertTrue(JdbcUtil.createTableIfNotExists(conn, tableName, schema));
    //
    //        assertEquals(7, JdbcUtil.getColumnNameList(conn, tableName).size());
    //
    //        JdbcUtil.closeQuietly(conn);
    //    }
    //
    //    public void testCreateDataSource() {
    //        String driver = "com.mysql.jdbc.Driver";
    //        String url = "jdbc:mysql://localhost:3306/abacustest";
    //        String user = "root";
    //        String password = "admin";
    //
    //        DataSource ds = JdbcUtil.createDataSource(driver, url, user, password);
    //        SQLExecutor sqlExecutor = new SQLExecutor(ds);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        DataSet result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        N.println(result.getRow(Account.class, 0));
    //
    //        sqlExecutor.update(MySqlDef.updateAccountFirstNameById, new Object[] { "updated fn", id });
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        N.println(result.getRow(Account.class, 0));
    //
    //        Account account = result.getRow(Account.class, 0);
    //
    //        assertEquals("updated fn", account.getFirstName());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    public void test_loadData() throws IOException, SQLException {
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        Account[] accounts = new Account[21];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //
    //        String sql_selectAll = NSC.select("*").from("account").sql();
    //        final Connection conn = sqlExecutor.getConnection();
    //        final PreparedStatement stmt = JdbcUtil.prepareStatement(conn, sql_selectAll);
    //        final ResultSet rs = stmt.executeQuery();
    //
    //        DataSet ds = JdbcUtil.extractData(rs);
    //        ds.println();
    //
    //        JdbcUtil.close(rs, stmt, conn);
    //
    //        File csvFile = new File("./src/test/resources/test.csv");
    //        ds.toCsv(csvFile);
    //
    //        DataSet ds1 = CSVUtil.loadCSV(csvFile);
    //        ds1.println();
    //
    //        assertEquals(ds.size(), ds1.size());
    //
    //        ds1 = CSVUtil.loadCSV(csvFile, new ArrayList<String>());
    //        ds1.println();
    //
    //        assertEquals(0, ds1.size());
    //
    //        DataSet ds2 = CSVUtil.loadCSV(Account.class, csvFile);
    //        ds2.println();
    //
    //        assertEquals(ds.size(), ds2.size());
    //
    //        ds2 = CSVUtil.loadCSV(Account.class, csvFile, new ArrayList<String>());
    //        ds2.println();
    //
    //        assertEquals(0, ds2.size());
    //
    //        List<Type<?>> columnTypeList = new ArrayList<>();
    //        for (int i = 0; i < ds.columnNameList().size(); i++) {
    //            if (i % 2 == 0) {
    //                columnTypeList.add(N.typeOf(ClassUtil.getPropSetMethod(Account.class, ds.columnNameList().get(i)).getParameterTypes()[0]));
    //            } else {
    //                columnTypeList.add(null);
    //            }
    //        }
    //
    //        DataSet ds3 = CSVUtil.loadCSV(csvFile, columnTypeList);
    //
    //        ds3.println();
    //
    //        assertEquals(ds.size(), ds3.size());
    //
    //        Map<String, Type<?>> columnTypeMap = new HashMap<>();
    //        for (int i = 0; i < ds.columnNameList().size(); i++) {
    //            if (i % 2 == 0) {
    //                columnTypeMap.put(ds.columnNameList().get(i),
    //                        N.typeOf(ClassUtil.getPropSetMethod(Account.class, ds.columnNameList().get(i)).getParameterTypes()[0]));
    //            }
    //        }
    //
    //        DataSet ds4 = CSVUtil.loadCSV(csvFile, columnTypeMap);
    //
    //        ds4.println();
    //
    //        assertEquals(ds.size(), ds4.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        IOUtil.deleteIfExists(csvFile);
    //    }
    //
    //    public void test_absolute() throws IOException, SQLException {
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        Account[] accounts = new Account[21];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //
    //        String sql_selectAll = NSC.select("*").from("account").sql();
    //        final Connection conn = sqlExecutor.getConnection();
    //        final PreparedStatement stmt = JdbcUtil.prepareStatement(conn, sql_selectAll);
    //        final ResultSet rs = stmt.executeQuery();
    //        // rs.next();
    //        // rs.next();
    //        JdbcUtil.skip(rs, 20);
    //
    //        DataSet ds = JdbcUtil.extractData(rs);
    //        ds.println();
    //
    //        N.println(ds.size());
    //
    //        JdbcUtil.close(rs, stmt, conn);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //    }
    //
    //    public void testImportCSV() throws IOException, SQLException {
    //        Account[] accounts = new Account[101];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        DataSet result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result.println();
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        File file = new File("./src/test/resources/test.csv");
    //        String sql = "SELECT first_name, last_name, gui, last_update_time, create_time FROM account";
    //        JdbcUtils.exportCSV(file, conn, sql);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        DataSet result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        sql = "SELECT first_name, last_name, gui, last_update_time, create_time FROM account";
    //        PreparedStatement stmt = JdbcUtil.prepareStatement(conn, sql);
    //
    //        JdbcUtils.exportCSV(file, stmt);
    //
    //        JdbcUtil.closeQuietly(stmt);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        DataSet copy = result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"));
    //        copy.toCsv(file, copy.columnNameList(), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        result.toCsv(file, N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        JdbcUtil.closeQuietly(conn);
    //
    //        IOUtil.deleteIfExists(file);
    //    }
    //
    //    public void testImportCSV_1() throws IOException {
    //        Account[] accounts = new Account[101];
    //        Random rand = new Random();
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-\" \\ ' , \", ");
    //            account.setLastName("ln-" + (rand.nextInt() % (accounts.length / 10)));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        DataSet result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result.println();
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        File file = new File("./src/test/resources/test.csv");
    //        String sql = "SELECT first_name, last_name, gui, last_update_time, create_time FROM account";
    //        List<Type<Object>> types = Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class);
    //
    //        JdbcUtils.exportCSV(file, conn, sql);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, types);
    //
    //        DataSet result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        DataSet copy = result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"));
    //        copy.toCsv(file, copy.columnNameList(), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        result.toCsv(file, N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        JdbcUtil.closeQuietly(conn);
    //
    //        IOUtil.deleteIfExists(file);
    //    }
    //
    //    public void testImportCSV_2() throws IOException {
    //        Account[] accounts = new Account[101];
    //        Random rand = new Random();
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-\" \\ ' , \", ");
    //            account.setLastName("ln-" + (rand.nextInt() % (accounts.length / 10)));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        DataSet result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result.println();
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        File file = new File("./src/test/resources/test.csv");
    //        String sql = "SELECT first_name, last_name, gui, last_update_time, create_time FROM account";
    //        List<Type<Object>> types = Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class);
    //
    //        JdbcUtils.exportCSV(file, conn, sql);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, types);
    //
    //        DataSet result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        DataSet copy = result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"));
    //        copy.toCsv(file, copy.columnNameList(), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        // ---------------------
    //
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        result.toCsv(file, N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        JdbcUtil.closeQuietly(conn);
    //
    //        IOUtil.deleteIfExists(file);
    //    }
    //
    //    public void testExportCSV_bigData() throws IOException {
    //        long startTime = System.currentTimeMillis();
    //
    //        final int size = 101;
    //
    //        for (int i = 0; i < 1; i++) {
    //            List<Account> accounts = new ArrayList<>(size);
    //
    //            for (int j = 0; j < size; j++) {
    //                accounts.add(AbstractAbacusTest.createAccount(Account.class, FIRST_NAME + j, LAST_NAME + j));
    //            }
    //
    //            sqlExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //        }
    //
    //        N.println("It took: " + (System.currentTimeMillis() - startTime) + " to add data");
    //
    //        startTime = System.currentTimeMillis();
    //
    //        File out = new File("./src/test/resources/exportedData.csv");
    //        if (out.exists()) {
    //            out.delete();
    //        }
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        String sql = SCSB.select("*").from("account").sql();
    //        JdbcUtils.exportCSV(out, conn, sql, 0, Integer.MAX_VALUE, true, false);
    //
    //        N.println("It took: " + (System.currentTimeMillis() - startTime) + " to export data");
    //
    //        //        sql = E.deleteFrom("account").sql();
    //        JdbcUtil.closeQuietly(conn);
    //        IOUtil.deleteIfExists(out);
    //    }
    //
    //    public void testImportData() throws IOException, SQLException {
    //        Account[] accounts = new Account[101];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        DataSet result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        Connection conn = sqlExecutor.getConnection();
    //
    //        String sql = PSC.insert(result.columnNameList()).into(Account.class).sql();
    //        N.println(sql);
    //        JdbcUtils.importData(result, conn, sql);
    //
    //        DataSet result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //
    //        assertEquals(result, result2);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        JdbcUtil.closeQuietly(conn);
    //    }
    //
    //    public void testImportData_2() throws IOException, SQLException {
    //        Account[] accounts = new Account[101];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts[i] = account;
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        sqlExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        DataSet result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        File csvFile = new File("./src/test/resources/test.csv");
    //        result.toCsv(csvFile);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        Connection conn = sqlExecutor.getConnection();
    //
    //        String sql = PSC.insert(result.columnNameList()).into(Account.class).sql();
    //        N.println(sql);
    //        final JSONParser jsonParser = ParserFactory.createJSONParser();
    //        final Object[] row = new Object[result.columnNameList().size()];
    //        JdbcUtils.importData(csvFile, 1, Long.MAX_VALUE, conn, sql, 200, 0, new Function<String, Object[]>() {
    //            @Override
    //            public Object[] apply(String t) {
    //                jsonParser.readString(row, t);
    //                row[6] = DateUtil.parseTimestamp((String) row[6]);
    //                row[8] = DateUtil.parseTimestamp((String) row[8]);
    //                row[9] = DateUtil.parseTimestamp((String) row[9]);
    //                return row;
    //            }
    //        });
    //
    //        DataSet result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //
    //        assertEquals(result, result2);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        JdbcUtil.closeQuietly(conn);
    //
    //        IOUtil.deleteIfExists(csvFile);
    //    }
    //
    //    public void estJdbcUtil3() {
    //        Connection conn = sqlExecutor.getConnection();
    //        File file = new File("C:\\Users\\Haiyang Li\\Documents\\CSVFile_2012-11-19T15_28_53.csv");
    //        String sql = "INSERT INTO sales_log_2 (CREATE_DATE, PTN, TRANSACTION_TYPE, CARRIER_CODE, PLATFORM, PHONE_MODEL, CLIENT_VERSION, OFFER_CODE, RESP_CODE, RESP_MINOR_CODE) "
    //                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, (List<? extends Type<?>>) null);
    //
    //        JdbcUtil.closeQuietly(conn);
    //    }
    //
    //    @Test
    //    public void estReplicate2() throws Exception {
    //        // ##########################################################################################
    //        String driver = "com.mysql.jdbc.Driver";
    //        String url = "jdbc:mysql://localhost:3306/abacustest";
    //        String user = "root";
    //        String password = "admin";
    //
    //        Class<? extends Driver> driverClass = ClassUtil.forClass(driver);
    //        Connection sourceConn = JdbcUtil.createConnection(driverClass, url, user, password);
    //
    //        // ##########################################################################################
    //        Connection targetConn = sqlExecutor.getConnection();
    //
    //        String sql_select = "select ID, PTN, TRANSACTION_TYPE, REQUEST_SOURCE, PROGRAM_CODE, CARRIER_CODE, PRODUCT_TYPE, PLATFORM, PHONE_MODEL, CLIENT_VERSION, OFFER_CODE, SOC_CODE, RESP_CODE, RESP_MINOR_CODE, CREATE_TIME"
    //                + " from tn_sales_log where ID >= 70000000 AND ID < 77000000 AND OFFER_CODE NOT IN ('tn_car_icon', 'tn_car_icon_free', 'tn_voice', 'tn_voice_free','tn_movie_free','tn_weather_free')";
    //
    //        String sql_insert = "insert into tn_sales_log_dev (ID, PTN, TRANSACTION_TYPE, REQUEST_SOURCE, PROGRAM_CODE, CARRIER_CODE, PRODUCT_TYPE, PLATFORM, PHONE_MODEL, CLIENT_VERSION, OFFER_CODE, SOC_CODE, RESP_CODE, RESP_MINOR_CODE, CREATE_TIME) values (?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?)";
    //        JdbcUtils.copy(sourceConn, sql_select, 1000, 10, 10000, targetConn, sql_insert, null, 1000, 0, false);
    //
    //        JdbcUtil.closeQuietly(targetConn);
    //        JdbcUtil.closeQuietly(sourceConn);
    //    }
    //
    //    @Override
    //    protected String getDomainName() {
    //        return ExtendDirtyBasicPNL._DN;
    //    }
}
