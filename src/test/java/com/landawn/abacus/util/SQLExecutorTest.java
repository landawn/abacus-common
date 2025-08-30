/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class SQLExecutorTest extends AbstractTest {

    @Test
    public void test_01() {
        // dummy test.
    }

    //
    //    //    @Test
    //    //    public void test_preparedQuery() throws Exception {
    //    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //    //
    //    //        String query = "select * from account where id > ?";
    //    //        sqlExecutor.prepareQuery(query).setLong(1, 0).query().println();
    //    //
    //    //        N.println(sqlExecutor.prepareQuery(query).setLong(1, 0).findFirst(BiRowMapper.TO_MAP));
    //    //
    //    //        assertTrue(sqlExecutor.prepareQuery(query).setLong(1, 0).exists());
    //    //
    //    //        assertEquals(1, sqlExecutor.prepareQuery(query).setLong(1, 0).count());
    //    //
    //    //        sqlExecutor.prepareQuery("delete from account where id = ?").setLong(1, id).execute();
    //    //
    //    //        assertFalse(sqlExecutor.prepareQuery(query).setLong(1, 0).exists());
    //    //    }
    //
    //    //    @Test
    //    //    public void test_percentiles() throws Exception {
    //    //        Account[] accounts = new Account[101];
    //    //        Random rand = new Random();
    //    //
    //    //        for (int i = 0; i < accounts.length; i++) {
    //    //            Account account = new Account();
    //    //            String uuid = Strings.uuid() + "-" + i;
    //    //            account.setGUI(uuid);
    //    //            account.setFirstName("fn-");
    //    //            account.setLastName("ln-" + (rand.nextInt() % (accounts.length / 10)));
    //    //
    //    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //    //            account.setLastUpdateTime(now);
    //    //            account.setCreatedTime(now);
    //    //            accounts[i] = account;
    //    //        }
    //    //
    //    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //    //
    //    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //    //        assertEquals(accounts.length, ids.size());
    //    //        assertEquals(accounts.length, result.size());
    //    //
    //    //        N.println(result.percentiles("id"));
    //    //
    //    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //    //
    //    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //    //        assertEquals(0, result.size());
    //    //    }
    //
    //    //    @Test
    //    //    public void test_executeQuery() {
    //    //        String sql = "select * from account";
    //    //        ResultSet resultset = sqlExecutor.executeQuery(sql);
    //    //        Dataset ds = JdbcUtil.extractData(resultset);
    //    //
    //    //        ds.println();
    //    //    }
    //
    //    @Test
    //    public void test_getPrimaryKey() throws SQLException {
    //        try (Connection conn = sqlExecutor.getConnection()) {
    //            try (ResultSet pkColumns = conn.getMetaData().getPrimaryKeys(null, null, "account")) {
    //                while (pkColumns.next()) {
    //                    N.println(pkColumns.getString("COLUMN_NAME"));
    //                }
    //            }
    //        }
    //    }
    //
    //    @Test
    //    public void test_mapper() {
    //        SQLExecutor sqlExecutor2 = new SQLExecutor(sqlExecutor.dataSource(), null, null, NamingPolicy.LOWER_CAMEL_CASE);
    //        Mapper<DataType2, EntityId> mapper = sqlExecutor2.mapper(DataType2.class, EntityId.class);
    //        EntityId entityId = mapper.insert(N.fill(DataType2.class));
    //
    //        DataType2 dataType2 = mapper.get(entityId).get();
    //        dataType2.setBigDecimalType(BigDecimal.TEN);
    //
    //        mapper.update(dataType2);
    //
    //        mapper.deleteById(entityId);
    //        assertFalse(mapper.get(entityId).isPresent());
    //    }
    //
    //    @Test
    //    public void test_returnColumnsIndex() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] parameters = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setReturnedColumnNames(Array.of("id", "first_name"));
    //        List<Long> ids = sqlExecutor.batchInsert(MySqlDef.insertAccount, jdbcSettings, N.asList((Object) parameters));
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, ids.get(0));
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_returnColumns() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] parameters = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setReturnedColumnNames(Array.of("id", "first_name"));
    //        List<Long> ids = sqlExecutor.batchInsert(MySqlDef.insertAccount, jdbcSettings, N.asList((Object) parameters));
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, ids.get(0));
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_returnColumns_2() {
    //        String sql = "INSERT INTO account (first_name, last_name, gui) VALUES ( ?,  ?,  uuid())";
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] parameters = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setReturnedColumnNames(Array.of("id", "uuid", "create_time"));
    //        List<Long> ids = sqlExecutor.batchInsert(sql, jdbcSettings, N.asList((Object) parameters));
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, ids.get(0));
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_dataSource_1() throws Exception {
    //        String dataSourcXmlFile = "./config/dataSourceManager.xml";
    //        SQLExecutor sqlExecutor = new SQLExecutor(JdbcUtil.createDataSourceManager(dataSourcXmlFile));
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //        sqlExecutor.close();
    //
    //        dataSourcXmlFile = "./config/dataSource.xml";
    //        sqlExecutor = new SQLExecutor(JdbcUtil.createDataSourceManager(dataSourcXmlFile));
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //        sqlExecutor.close();
    //
    //        dataSourcXmlFile = "./config/dataSourceManager_2.xml";
    //        sqlExecutor = new SQLExecutor(JdbcUtil.createDataSourceManager(dataSourcXmlFile));
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //        sqlExecutor.close();
    //    }
    //
    //    @Test
    //    public void test_tableNotExists() throws Exception {
    //        try {
    //            assertFalse(sqlExecutor.exists("select * from nonexist"));
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }
    //
    //    @Test
    //    public void test_exists() throws Exception {
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        assertTrue(sqlExecutor.exists("select * from account"));
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //    }
    //
    //    @Test
    //    public void test_in() throws Exception {
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        SP p = NSC.selectFrom(Account.class).where(CF.in("id", N.asList(id))).build();
    //        N.println(p.sql);
    //
    //        long[] ids = { 1, 2, 3, 4, 5, 6, id };
    //
    //        Dataset result = sqlExecutor.query(p.sql, StringUtil.join(ids, ", "));
    //        result.println();
    //        result = sqlExecutor.query(p.sql, id);
    //        result.println();
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        assertFalse(sqlExecutor.exists("select * from account"));
    //
    //        N.println(CF.in("id", N.asList(1, 2, 3)));
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
    //
    //        assertTrue(sqlExecutor.doesTableExist(tableName));
    //        assertFalse(sqlExecutor.createTableIfNotExists(tableName, schema));
    //
    //        assertTrue(sqlExecutor.dropTableIfExists(tableName));
    //
    //        assertFalse(sqlExecutor.doesTableExist(tableName));
    //        assertFalse(sqlExecutor.dropTableIfExists(tableName));
    //
    //        assertTrue(sqlExecutor.createTableIfNotExists(tableName, schema));
    //
    //        assertEquals(7, sqlExecutor.getColumnNameList(tableName).size());
    //    }
    //
    //    @Test
    //    public void test_singleQuery() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "1", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        String sql = "select first_name from account where id = ?";
    //
    //        boolean bool = sqlExecutor.queryForBoolean(sql, id).orElse(false);
    //        assertTrue(bool);
    //
    //        char ch = sqlExecutor.queryForChar(sql, id).orElse((char) 0);
    //        assertEquals('1', ch);
    //
    //        byte b = sqlExecutor.queryForByte(sql, id).orElse((byte) 0);
    //        assertEquals(1, b);
    //
    //        short s = sqlExecutor.queryForShort(sql, id).orElse((short) 0);
    //        assertEquals(1, s);
    //
    //        int i = sqlExecutor.queryForInt(sql, id).orElse(0);
    //        assertEquals(1, i);
    //
    //        long l = sqlExecutor.queryForLong(sql, id).orElse(0);
    //        assertEquals(1, l);
    //
    //        float f = sqlExecutor.queryForFloat(sql, id).orElse(0);
    //        assertEquals(1f, f);
    //
    //        double d = sqlExecutor.queryForDouble(sql, id).orElse(0);
    //        assertEquals(1d, d);
    //
    //        sql = "select id from account where id = ?";
    //
    //        l = sqlExecutor.queryForLong(sql, id).orElse(0);
    //        assertEquals(id, l);
    //
    //        f = sqlExecutor.queryForFloat(sql, id).orElse(0);
    //        assertEquals((float) id, f);
    //
    //        d = sqlExecutor.queryForDouble(sql, id).orElse(0);
    //        assertEquals((double) id, d);
    //
    //        d = sqlExecutor.queryForSingleResult(double.class, sql, id).orElse(0d);
    //        assertEquals((double) id, d);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_JdbcSettings() throws Exception {
    //        JdbcSettings jdbcSettings = JdbcSettings.create();
    //        jdbcSettings.setAutoGeneratedKeys(true).setBatchSize(100).setReturnedColumnIndexes(new int[] { 1, 2 });
    //        jdbcSettings.setReturnedColumnNames(N.asArray("a", "b")).setCount(10).setFetchDirection(1).setFetchSize(100);
    //        jdbcSettings.setLogSQL(false).setMaxRows(200).setMaxFieldSize(100).setOffset(100).setQueryTimeout(100000).setQueryWithDataSource("write");
    //        jdbcSettings.setResultSetConcurrency(1).setResultSetHoldability(10).setResultSetType(10);
    //
    //        N.println(jdbcSettings);
    //
    //        Set<JdbcSettings> set = N.asSet(jdbcSettings);
    //        assertTrue(set.contains(jdbcSettings.copy()));
    //    }
    //
    //    @Test
    //    public void test_getColumnNameList() throws Exception {
    //        N.println(sqlExecutor.getColumnNameList(Account.__));
    //        assertEquals(10, sqlExecutor.getColumnNameList(Account.__).size());
    //    }
    //
    //    @Test
    //    public void test_execute() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_query() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        assertEquals(1, result.size());
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ResultSetExtractor.TO_DATA_SET, id);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_queryAll() throws Exception {
    //        final String insertAccount_1 = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?,  ?,  ?) ";
    //        final String insertAccount_2 = "INSERT INTO account_2 (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?,  ?,  ?) ";
    //
    //        final String sql_1 = "select * from account WHERE gui = ?";
    //        final String sql_2 = "select * from account_2 WHERE gui = ?";
    //
    //        final Timestamp now = new Timestamp(System.currentTimeMillis());
    //        final String uuid = Strings.uuid();
    //
    //        sqlExecutor.insert(insertAccount_1, "fn", "ln", uuid, now, now);
    //
    //        Dataset result = sqlExecutor.query(sql_1, uuid);
    //        N.println(result);
    //
    //        sqlExecutor.insert(insertAccount_2, "fn", "ln", uuid, now, now);
    //
    //        result = sqlExecutor.query(sql_2, uuid);
    //        N.println(result);
    //
    //        for (int i = 0; i < 2000; i++) {
    //            result = sqlExecutor.queryAll(N.asList(sql_1), null, uuid);
    //            assertEquals(1, result.size());
    //        }
    //
    //        for (int i = 0; i < 2000; i++) {
    //            result = sqlExecutor.queryAll(N.asList(sql_1, sql_2), null, uuid);
    //            assertEquals(2, result.size());
    //        }
    //
    //        List<String> sqls = N.repeatCollection(N.asList(sql_1, sql_2), 50);
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setQueryInParallel(false);
    //
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.queryAll(sqls, jdbcSettings, uuid);
    //            assertEquals(100, result.size());
    //        }
    //        for (int i = 0; i < 10; i++) {
    //            assertEquals(100, sqlExecutor.streamAll(Clazz.ofList(Object.class), sqls, jdbcSettings, uuid).count());
    //        }
    //
    //        jdbcSettings.setQueryInParallel(true);
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.queryAll(sqls, jdbcSettings, uuid);
    //            assertEquals(100, result.size());
    //        }
    //        for (int i = 0; i < 10; i++) {
    //            assertEquals(100, sqlExecutor.streamAll(Clazz.ofList(Object.class), sqls, jdbcSettings, uuid).count());
    //        }
    //
    //        jdbcSettings.setQueryInParallel(false).setQueryWithDataSources(N.repeat(((com.landawn.abacus.DataSource) sqlExecutor.dataSource()).getName(), 10));
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.queryAll(sqls, jdbcSettings, uuid);
    //            assertEquals(1000, result.size());
    //        }
    //        for (int i = 0; i < 10; i++) {
    //            assertEquals(1000, sqlExecutor.streamAll(Clazz.ofList(Object.class), sqls, jdbcSettings, uuid).count());
    //        }
    //
    //        result = sqlExecutor.query(sql_1, uuid);
    //        N.println(result);
    //
    //        sqls = N.repeatCollection(N.asList(sql_1, sql_2), 5);
    //        jdbcSettings.setQueryInParallel(true);
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.queryAll(sqls, jdbcSettings, uuid);
    //            assertEquals(100, result.size());
    //        }
    //        for (int i = 0; i < 10; i++) {
    //            assertEquals(100, sqlExecutor.streamAll(Clazz.ofList(Object.class), sqls, jdbcSettings, uuid).count());
    //        }
    //    }
    //
    //    @Test
    //    public void test_queryAll_2() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result1 = sqlExecutor.queryAll(N.asList(MySqlDef.selectAccountById, MySqlDef.selectAccountById), null, id);
    //        N.println(result1);
    //        assertEquals(2, result1.size());
    //
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setQueryInParallel(false);
    //        result1 = sqlExecutor.queryAll(N.asList(MySqlDef.selectAccountById, MySqlDef.selectAccountById), jdbcSettings, id);
    //        N.println(result1);
    //        assertEquals(2, result1.size());
    //
    //        jdbcSettings = JdbcSettings.create().setQueryWithDataSources(N.asList("codes", "codes"));
    //        Dataset result2 = sqlExecutor.queryAll(MySqlDef.selectAccountById, jdbcSettings, id);
    //        N.println(result2);
    //        assertEquals(2, result2.size());
    //
    //        result2 = sqlExecutor.queryAll(MySqlDef.selectAccountById, jdbcSettings, id);
    //        N.println(result2);
    //        assertEquals(2, result2.size());
    //
    //        assertEquals(result1, result2);
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result1 = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result1);
    //        assertEquals(0, result1.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_find() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        List<Account> result = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        result = sqlExecutor.list(Account.class, conn, MySqlDef.selectAccountById, id);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_jdbc_list() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        List<Account> result = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        result = sqlExecutor.list(Account.class, conn, MySqlDef.selectAccountById, id);
    //        assertEquals(1, result.size());
    //
    //        JdbcUtil.prepareCallableQuery(conn, "select * from account").list(Account.class);
    //
    //        result = JdbcUtil.prepareCallableQuery(conn, "select * from account where id = ?").setLong(1, id).list(Account.class);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_stream() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        List<Account> result = sqlExecutor.stream(Account.class, MySqlDef.selectAccountById, id).toList();
    //
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.stream(Account.class, MySqlDef.selectAccountById, id).toList();
    //        }
    //
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_listAll() throws Exception {
    //        final String insertAccount_1 = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?,  ?,  ?) ";
    //        final String insertAccount_2 = "INSERT INTO account_2 (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?,  ?,  ?) ";
    //
    //        final String sql_1 = "select * from account WHERE gui = ?";
    //        final String sql_2 = "select * from account_2 WHERE gui = ?";
    //
    //        final Timestamp now = new Timestamp(System.currentTimeMillis());
    //        final String uuid = Strings.uuid();
    //
    //        sqlExecutor.insert(insertAccount_1, "fn", "ln", uuid, now, now);
    //
    //        List<Account> result = sqlExecutor.list(Account.class, sql_1, uuid);
    //        N.println(result);
    //
    //        sqlExecutor.insert(insertAccount_2, "fn", "ln", uuid, now, now);
    //
    //        result = sqlExecutor.list(Account.class, sql_2, uuid);
    //        N.println(result);
    //
    //        result = sqlExecutor.listAll(Account.class, N.asList(sql_1, sql_2), null, uuid);
    //        N.println(result);
    //
    //        for (int i = 0; i < 10; i++) {
    //            result = sqlExecutor.streamAll(Account.class, N.asList(sql_1, sql_2), null, uuid).filter(Fn.alwaysTrue()).toList();
    //        }
    //    }
    //
    //    @Test
    //    public void test_findAll_2() throws Exception {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        List<Account> result1 = sqlExecutor.listAll(Account.class, N.asList(MySqlDef.selectAccountById, MySqlDef.selectAccountById), null, id);
    //        N.println(result1);
    //        assertEquals(2, result1.size());
    //
    //        JdbcSettings jdbcSettings = JdbcSettings.create().setQueryInParallel(false);
    //        result1 = sqlExecutor.listAll(Account.class, N.asList(MySqlDef.selectAccountById, MySqlDef.selectAccountById), jdbcSettings, id);
    //        N.println(result1);
    //        assertEquals(2, result1.size());
    //
    //        jdbcSettings = JdbcSettings.create().setQueryWithDataSources(N.asList("codes", "codes"));
    //        List<Account> result2 = sqlExecutor.listAll(Account.class, MySqlDef.selectAccountById, jdbcSettings, id);
    //        N.println(result2);
    //        assertEquals(2, result2.size());
    //
    //        assertEquals(result1, result2);
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result1 = sqlExecutor.list(Account.class, MySqlDef.selectAccountById, id);
    //        N.println(result1);
    //        assertEquals(0, result1.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_namedParameter() throws Exception {
    //        final String insertAccount_1 = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?,  ?,  ?) ";
    //
    //        final Timestamp now = new Timestamp(System.currentTimeMillis());
    //
    //        sqlExecutor.insert(insertAccount_1, "fn", "ln", "#{gui}", now, now);
    //
    //        Dataset result = sqlExecutor.query("select * from account WHERE gui = '#{gui}'");
    //        N.println(result);
    //        assertEquals("#{gui}", result.get("gui"));
    //
    //        result = sqlExecutor.query("select * from account WHERE gui = #{gui}", "#{gui}");
    //        N.println(result);
    //        assertEquals("#{gui}", result.get("gui"));
    //
    //        result = sqlExecutor.query("select * from account WHERE gui = '#{gui}}'");
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        result = sqlExecutor.query("select * from account WHERE gui = #{gui}", "##{gui}");
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        result = sqlExecutor.query("select * from account WHERE gui = '?'");
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_sqlMapper() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, id);
    //        N.println(result);
    //
    //        sqlExecutor.execute(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    // public static final SQLExecutor sqlExecutor;
    //    //
    //    // static {
    //    // File file = Configuration.findFile("extendDirtySqlMapper.xml");
    //    // sqlExecutor = new SQLExecutor(emf.getDataSourceManager(ExtendDirtyBasicPNL._DN).getPrimaryDataSource(),
    //    // null, new SQLMapper(file));
    //    // }
    //
    //    //    public void testTLock() {
    //    //        DBLock tLock = sqlExecutor.getDBLock("t_lock");
    //    //        String target = Strings.uuid();
    //    //
    //    //        String code = tLock.lock(target, 10 * 1000L);
    //    //        N.println(sqlExecutor.query("select * from t_lock"));
    //    //
    //    //        tLock.unlock(target, code);
    //    //        N.println(sqlExecutor.query("select * from t_lock"));
    //    //
    //    //        assertTrue(sqlExecutor.dropTableIfExists("t_lock"));
    //    //    }
    //    //
    //    //    public void testSequence() {
    //    //        sqlExecutor.dropTableIfExists("t_seq");
    //    //
    //    //        DBSequence tSeq = sqlExecutor.getDBSequence("t_seq", "seq_123");
    //    //
    //    //        N.println(sqlExecutor.query("select * from t_seq"));
    //    //
    //    //        for (int i = 0; i < 10; i++) {
    //    //            N.println(tSeq.nextVal());
    //    //        }
    //    //
    //    //        N.println(sqlExecutor.query("select * from t_seq"));
    //    //
    //    //        for (int i = 9; i < 10000; i++) {
    //    //            tSeq.nextVal();
    //    //        }
    //    //
    //    //        N.println(sqlExecutor.query("select * from t_seq"));
    //    //
    //    //        tSeq = sqlExecutor.getDBSequence("t_seq", "seq_123", 1000000, 10000);
    //    //        assertEquals(1000000, sqlExecutor.queryForLong("select next_val from t_seq").orElse(0));
    //    //
    //    //        assertTrue(sqlExecutor.dropTableIfExists("t_seq"));
    //    //    }
    //
    //    @Test
    //    public void test_batchInsert() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] parameters = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        List<Long> ids = sqlExecutor.batchInsert(MySqlDef.insertAccount, N.asList((Object) parameters));
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, ids.get(0));
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void test_batchInsert_2() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] array = new Object[999];
    //
    //        for (int i = 0; i < array.length; i++) {
    //            array[i] = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        }
    //
    //        List<Long> ids = sqlExecutor.batchInsert(MySqlDef.insertAccount, N.asList(array));
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        ((Object[]) array[array.length - 2])[2] = ((Object[]) array[array.length - 1])[2];
    //
    //        try {
    //            ids = sqlExecutor.batchInsert(MySqlDef.insertAccount, N.asList(array));
    //            fail("Should throw AbacusSQLException");
    //        } catch (UncheckedSQLException e) {
    //        }
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        try {
    //            sqlExecutor.batchUpdate(MySqlDef.insertAccount, N.asList(array));
    //            fail("Should throw AbacusSQLException");
    //        } catch (UncheckedSQLException e) {
    //        }
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, ids.get(0));
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    public void testInsert() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        StatementSetter statementSetter = null;
    //        id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, statementSetter, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        JdbcSettings props = null;
    //        id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, statementSetter, props, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    public void testInsert2() {
    //        Connection conn = sqlExecutor.getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(conn, MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        StatementSetter statementSetter = null;
    //        id = (Long) sqlExecutor.insert(conn, MySqlDef.insertAccount, statementSetter, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        JdbcSettings props = null;
    //        id = (Long) sqlExecutor.insert(conn, MySqlDef.insertAccount, statementSetter, props, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    public void testBatchInsert() {
    //        Connection conn = sqlExecutor.getConnection();
    //        Account account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        Account account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        sqlExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account));
    //
    //        account2 = sqlExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        sqlExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account));
    //        account2 = sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).orElse(null);
    //
    //        assertNull(account2);
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    public void test_update() {
    //        Connection conn = sqlExecutor.getConnection();
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName1", id);
    //        assertEquals("updatedFirstName1", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        sqlExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName2", id);
    //        assertEquals("updatedFirstName2", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        sqlExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName3", id);
    //        assertEquals("updatedFirstName3", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        sqlExecutor.update(conn, MySqlDef.updateAccountFirstNameById, "updatedFirstName4", id);
    //        assertEquals("updatedFirstName4", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        List<Object> listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName5", id));
    //        sqlExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, listOfParameters);
    //        assertEquals("updatedFirstName5", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName6", id));
    //        sqlExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, listOfParameters);
    //        assertEquals("updatedFirstName6", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName7", id));
    //        sqlExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, listOfParameters);
    //        assertEquals("updatedFirstName7", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName8", id));
    //        sqlExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, listOfParameters);
    //        assertEquals("updatedFirstName8", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        Object[] arrayOfParameters = new Object[1];
    //        arrayOfParameters[0] = N.asArray("updatedFirstName9", id);
    //        sqlExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, N.asList(arrayOfParameters));
    //        assertEquals("updatedFirstName9", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        arrayOfParameters = new Object[1];
    //        arrayOfParameters[0] = N.asArray("updatedFirstName10", id);
    //        sqlExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, N.asList(arrayOfParameters));
    //        assertEquals("updatedFirstName10", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        arrayOfParameters = new Object[1];
    //        arrayOfParameters[0] = N.asArray("updatedFirstName11", id);
    //        sqlExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, N.asList(arrayOfParameters));
    //        assertEquals("updatedFirstName11", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        arrayOfParameters = new Object[1];
    //        arrayOfParameters[0] = N.asArray("updatedFirstName12", id);
    //        sqlExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, N.asList(arrayOfParameters));
    //        assertEquals("updatedFirstName12", sqlExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).orElse(null).getFirstName());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void testPureSQL() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        N.println(result.getRow(Account.class, 0));
    //
    //        sqlExecutor.update(MySqlDef.updateAccountFirstNameById, "updated fn", id);
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        N.println(result.getRow(Account.class, 0));
    //
    //        assertEquals("updated fn", result.getRow(Account.class, 0).getFirstName());
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
    //    @Test
    //    public void testNamedSQLExecutorByBean() throws Exception {
    //        Account account = new Account();
    //        account.setFirstName("firstName");
    //        account.setLastName("lastName");
    //        account.setGUI(UUID.randomUUID().toString());
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        account.setLastUpdateTime(now);
    //        account.setCreatedTime(now);
    //
    //        long id = sqlExecutor.insert(INSERT_ACCOUNT, account);
    //
    //        try {
    //            sqlExecutor.insert(INSERT_ACCOUNT, account);
    //            fail("should throw AbacusSQLException");
    //        } catch (UncheckedSQLException e) {
    //            // ignore;
    //        }
    //
    //        account.setId(id);
    //
    //        Account dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        N.println(dbAccount);
    //        assertEquals("firstName", dbAccount.getFirstName());
    //
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, account.getId()).orElse(null);
    //        N.println(dbAccount);
    //        assertEquals("firstName", dbAccount.getFirstName());
    //
    //        account.setFirstName("updated fn");
    //        sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, account);
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertEquals("updated fn", dbAccount.getFirstName());
    //
    //        account.setFirstName("updated fn2");
    //        sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, account.getFirstName(), account.getId());
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, account.getId()).orElse(null);
    //        assertEquals("updated fn2", dbAccount.getFirstName());
    //
    //        sqlExecutor.update(DELETE_ACCOUNT_BY_ID, account);
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, account).orElse(null);
    //        assertNull(dbAccount);
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        Dataset result = sqlExecutor.query(SELECT_ACCOUNT_BY_ID, account);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    //    @Test
    //    //    public void testNamedSQLExecutorByBean2() throws Exception {
    //    //        Account account = new Account();
    //    //        account.setFirstName("firstName");
    //    //        account.setLastName("lastName");
    //    //        account.setGUI(UUID.randomUUID().toString());
    //    //
    //    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //    //        account.setLastUpdateTime(now);
    //    //        account.setCreatedTime(now);
    //    //
    //    //        sqlExecutor.insert(INSERT_ACCOUNT, JdbcSettings.create().setGeneratedIdPropName("wrong"), account);
    //    //
    //    //        Dataset result = sqlExecutor.query(SELECT_ACCOUNT_BY_ID, account);
    //    //        N.println(result);
    //    //        assertEquals(0, result.size());
    //    //    }
    //
    //    @Test
    //    public void testNamedSQLExecutorByMap() throws Exception {
    //        Map<String, Object> accountProps = new HashMap<>();
    //        accountProps.put("firstName", "firstName");
    //        accountProps.put("lastName", "lastName");
    //        accountProps.put("gui", UUID.randomUUID().toString());
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        accountProps.put("lastUpdateTime", now);
    //        accountProps.put("createdTime", now);
    //
    //        long id = sqlExecutor.insert(INSERT_ACCOUNT, accountProps);
    //        accountProps.put("id", id);
    //        //
    //        //        Map<String, Object> dbAccount = sqlExecutor.queryForMap(SELECT_ACCOUNT_BY_ID, accountProps);
    //        //        N.println(dbAccount);
    //        //        assertEquals("firstName", dbAccount.get("first_name"));
    //        //
    //        //        accountProps.put("firstName", "updated fn");
    //        //        sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, accountProps);
    //        //        dbAccount = sqlExecutor.queryForMap(SELECT_ACCOUNT_BY_ID, accountProps);
    //        //        assertEquals("updated fn", dbAccount.get("first_name"));
    //        //
    //        //        sqlExecutor.update(DELETE_ACCOUNT_BY_ID, accountProps);
    //        //        dbAccount = sqlExecutor.queryForMap(SELECT_ACCOUNT_BY_ID, accountProps);
    //        //        assertNull(dbAccount);
    //        //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        Dataset result = sqlExecutor.query(SELECT_ACCOUNT_BY_ID, accountProps);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testBatchNamedSQLExecutorByArrayOfBean() throws Exception {
    //        Account[] accounts = new Account[1001];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            account.setFirstName("firstName");
    //            account.setLastName("lastName");
    //            account.setGUI(UUID.randomUUID().toString());
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            accounts[i].setFirstName("updated fn");
    //        }
    //
    //        sqlExecutor.batchUpdate(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, N.asList(accounts));
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, result.size());
    //
    //        assertEquals("updated fn", result.getRow(Account.class, 0).getFirstName());
    //        assertEquals("updated fn", result.getRow(Account.class, result.size() - 1).getFirstName());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testBatchNamedSQLExecutorByListOfBean() throws Exception {
    //        int size = 1099;
    //        List<Account> accounts = new ArrayList<>(size);
    //
    //        for (int i = 0; i < size; i++) {
    //            Account account = new Account();
    //            account.setFirstName("firstName");
    //            account.setLastName("lastName");
    //            account.setGUI(UUID.randomUUID().toString());
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            account.setLastUpdateTime(now);
    //            account.setCreatedTime(now);
    //            accounts.add(account);
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), ids.size());
    //        assertEquals(accounts.size(), result.size());
    //
    //        StatementSetter statementSetter = StatementSetter.DEFAULT;
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, statementSetter, accounts);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), ids.size());
    //        assertEquals(accounts.size(), result.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, statementSetter, JdbcSettings.create(), accounts);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), ids.size());
    //        assertEquals(accounts.size(), result.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testBatchNamedSQLExecutorByListOfMap() throws Exception {
    //        int size = 1099;
    //        List<Map<String, Object>> accounts = new ArrayList<>(size);
    //
    //        for (int i = 0; i < size; i++) {
    //            Map<String, Object> accountProps = new HashMap<>();
    //            accountProps.put("firstName", "firstName");
    //            accountProps.put("lastName", "lastName");
    //            accountProps.put("gui", UUID.randomUUID().toString());
    //
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            accountProps.put("lastUpdateTime", now);
    //            accountProps.put("createdTime", now);
    //            accounts.add(accountProps);
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), ids.size());
    //        assertEquals(accounts.size(), result.size());
    //
    //        for (int i = 0; i < accounts.size(); i++) {
    //            accounts.get(i).put("firstName", "updated fn");
    //            accounts.get(i).put("id", ids.get(i));
    //        }
    //
    //        sqlExecutor.batchUpdate(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, accounts);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), result.size());
    //
    //        assertEquals("updated fn", result.getRow(Account.class, 0).getFirstName());
    //        assertEquals("updated fn", result.getRow(Account.class, result.size() - 1).getFirstName());
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testBatchNamedSQLExecutorByListOfBean2() throws Exception {
    //        int size = 1099;
    //        List<Object[]> accounts = new ArrayList<>(size);
    //
    //        for (int i = 0; i < size; i++) {
    //            Object[] parameters = new Object[5];
    //            parameters[0] = "firstName";
    //            parameters[1] = "lastName";
    //            parameters[2] = Strings.uuid();
    //            parameters[3] = DateUtil.currentTimestamp();
    //            parameters[4] = DateUtil.currentTimestamp();
    //            accounts.add(parameters);
    //        }
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        List<Long> ids = sqlExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.size(), ids.size());
    //        assertEquals(accounts.size(), result.size());
    //
    //        accounts = new ArrayList<>(size);
    //
    //        for (int i = 0; i < size; i++) {
    //            Object[] parameters = new Object[5];
    //            parameters[0] = "updatedFirstName";
    //            parameters[1] = ids.get(i);
    //            accounts.add(parameters);
    //        }
    //
    //        sqlExecutor.batchUpdate(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, accounts);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        N.println(result.getRow(Account.class, 1));
    //        N.println(result.toList(Account.class, 1, size));
    //        N.println(result.getRow(Account.class, 1));
    //        N.println(result.toList(Account.class, 1, size));
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testSingleResultQuery() throws Exception {
    //        Long result1 = sqlExecutor.queryForSingleResult(Long.class, MySqlDef.selectAccountById, Long.MAX_VALUE).orElse(0L);
    //        N.println(result1);
    //
    //        long l = sqlExecutor.queryForSingleResult(long.class, MySqlDef.selectAccountById, Long.MAX_VALUE).orElse(0L);
    //        N.println(l);
    //
    //        boolean b = sqlExecutor.queryForBoolean(MySqlDef.selectAccountById, Long.MAX_VALUE).orElse(false);
    //        N.println(b);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //
    //        b = sqlExecutor.queryForBoolean(MySqlDef.selectAccountById, id).orElse(false);
    //        N.println(b);
    //
    //        b = sqlExecutor.queryForBoolean(MySqlDef.selectFirstNameFromAccountById, id).orElse(false);
    //        N.println(b);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //    }
    //
    //    @Test
    //    public void testBatchInsertForMysql() {
    //        final int batchSize = 200;
    //        final int batchCount = 10;
    //        String valuesPart = "," + MySqlDef.insertAccount.substring(MySqlDef.insertAccount.indexOf(") VALUES") + ") VALUES".length());
    //        StringBuilder sb = new StringBuilder();
    //        sb.append(MySqlDef.insertAccount);
    //
    //        for (int i = 1; i < batchSize; i++) {
    //            sb.append(valuesPart);
    //        }
    //
    //        String sql = sb.toString();
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] parameters = new Object[batchCount];
    //
    //        for (int i = 0; i < batchCount; i++) {
    //            Object[] parameter = new Object[batchSize * 5];
    //
    //            for (int j = 0, k = 0; j < batchSize; j++) {
    //                parameter[k++] = "fisrtName" + ((i * batchSize) + j);
    //                parameter[k++] = "lastName" + ((i * batchSize) + j);
    //                parameter[k++] = UUID.randomUUID().toString();
    //                parameter[k++] = now;
    //                parameter[k++] = now;
    //            }
    //
    //            parameters[i] = parameter;
    //        }
    //
    //        sqlExecutor.batchInsert(sql, N.asList(parameters));
    //
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //    }
    //
    //    @Test
    //    public void testSortBy() throws Exception {
    //        Account[] accounts = new Account[1001];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-" + uuid.substring(32));
    //            account.setLastName("ln-" + uuid.substring(32));
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        // result.println();
    //
    //        // result.sortBy("gui");
    //        Method method = ClassUtil.getDeclaredMethod(Dataset.class, "sortBy", String.class);
    //        MultiLoopsStatistics perf = Profiler.run(result, method, "gui", 1, 1, 1);
    //        perf.printResult();
    //
    //        perf = Profiler.run(result, method, "gui", 1, 100, 1);
    //        perf.printResult();
    //
    //        // result.println();
    //        for (int i = 0; i < result.size();) {
    //            result.absolute(i);
    //
    //            String gui = result.get("gui");
    //            String firstName = result.get("first_name");
    //            String lastName = result.get("last_name");
    //            assertTrue(firstName.endsWith(gui.substring(32)));
    //            assertTrue(lastName.endsWith(gui.substring(32)));
    //
    //            i++;
    //
    //            if (i < (result.size() - 1)) {
    //                String nextGui = result.absolute(i).get("gui");
    //                assertTrue(gui.compareTo(nextGui) <= 0);
    //            }
    //        }
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testSortBy2() throws Exception {
    //        Account[] accounts = new Account[999];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-" + uuid.substring(32));
    //            account.setLastName("ln-" + uuid.substring(32));
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        // result.println();
    //
    //        // result.sortBy("gui");
    //        Method method = ClassUtil.getDeclaredMethod(Dataset.class, "sortBy", Collection.class);
    //        MultiLoopsStatistics perf = Profiler.run(result, method, N.asList(N.asList("gui", "first_name")), 1, 1, 1);
    //        perf.printResult();
    //
    //        perf = Profiler.run(result, method, N.asList(N.asList("gui", "first_name")), 1, 100, 1);
    //        perf.printResult();
    //
    //        // result.println();
    //        for (int i = 0; i < result.size();) {
    //            result.absolute(i);
    //
    //            String gui = result.get("gui");
    //            String firstName = result.get("first_name");
    //            String lastName = result.get("last_name");
    //            assertTrue(firstName.endsWith(gui.substring(32)));
    //            assertTrue(lastName.endsWith(gui.substring(32)));
    //
    //            i++;
    //
    //            if (i < (result.size() - 1)) {
    //                String nextGui = result.absolute(i).get("gui");
    //                assertTrue(gui.compareTo(nextGui) <= 0);
    //            }
    //        }
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testGroupBy() throws Exception {
    //        Account[] accounts = new Account[1001];
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-" + uuid.substring(32));
    //            account.setLastName("ln-" + uuid.substring(32));
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result.println();
    //        // result.groupBy("gui").println();
    //
    //        Method method = ClassUtil.getDeclaredMethod(Dataset.class, "groupBy", String.class);
    //        Profiler.run(result, method, "gui", 1, 1, 1).printResult();
    //
    //        Profiler.run(result, method, "gui", 1, 100, 1).printResult();
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testGroupBy2() throws Exception {
    //        Account[] accounts = new Account[999];
    //        Random rand = new Random();
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-");
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result.println();
    //
    //        Dataset newResultSet = result.groupBy(N.asList("first_name", "last_name"));
    //        newResultSet.println();
    //        assertEquals(2, newResultSet.columnNameList().size());
    //
    //        newResultSet = result.groupBy("first_name", null, "*", "first_name", Collectors.counting());
    //        newResultSet.println();
    //        assertEquals(2, newResultSet.columnNameList().size());
    //        assertEquals(accounts.length, newResultSet.absolute(0).getInt(1));
    //
    //        newResultSet = result.groupBy("first_name", null, "*", N.asList("first_name"), Collectors.counting());
    //        newResultSet.println();
    //        assertEquals(2, newResultSet.columnNameList().size());
    //        assertEquals(accounts.length, newResultSet.absolute(0).getInt(1));
    //
    //        newResultSet = result.groupBy(N.asLinkedList("first_name"), null, "*", "first_name", Collectors.counting());
    //        newResultSet.println();
    //        assertEquals(2, newResultSet.columnNameList().size());
    //        assertEquals(accounts.length, newResultSet.absolute(0).getInt(1));
    //
    //        newResultSet = result.groupBy(N.asLinkedList("first_name"), null, "*", N.asList("first_name"), Collectors.counting());
    //        newResultSet.println();
    //        assertEquals(2, newResultSet.columnNameList().size());
    //        assertEquals(accounts.length, newResultSet.absolute(0).getInt(1));
    //
    //        Method method = ClassUtil.getDeclaredMethod(Dataset.class, "groupBy", Collection.class);
    //        Profiler.run(result, method, N.asList(N.asList("first_name", "last_name")), 1, 1, 1).printResult();
    //
    //        Profiler.run(result, method, N.asList(N.asList("first_name", "last_name")), 1, 100, 1).printResult();
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testSum() throws Exception {
    //        Account[] accounts = new Account[101];
    //        Random rand = new Random();
    //
    //        for (int i = 0; i < accounts.length; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-");
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
    //        Dataset result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(accounts.length, ids.size());
    //        assertEquals(accounts.length, result.size());
    //
    //        result.println();
    //        N.println(result.<Long> stream("id").sumDouble(ToDoubleFunction.NUM));
    //        N.println(result.<Number> stream("id").averageDouble(ToDoubleFunction.NUM).orElse(0d));
    //        N.println(result.<Long> stream("id").min(Comparators.naturalOrder()));
    //        N.println(result.<Long> stream("id").max(Comparators.naturalOrder()));
    //        N.println(result.<String> stream("first_name").max(Comparators.naturalOrder()));
    //        N.println(result.<String> stream("last_name").min(Comparators.naturalOrder()));
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts));
    //
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testIn() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        String sql = "select * from account WHERE account.id in(?, ?)";
    //        Dataset result = sqlExecutor.query(sql, id, id + 1);
    //        N.println(result);
    //
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    @Test
    //    public void testParameters() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, FIRST_NAME, LAST_NAME, UUID.randomUUID().toString(), now, now);
    //        Account bean = sqlExecutor.get(Account.class, SELECT_ACCOUNT_BY_ID, id).get();
    //
    //        Account dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, bean.getId()).orElse(null);
    //        assertEquals(FIRST_NAME, dbAccount.getFirstName());
    //
    //        Object parameters = N.asArray(bean.getId());
    //
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, parameters).orElse(null);
    //        assertEquals(FIRST_NAME, dbAccount.getFirstName());
    //
    //        parameters = new Object[] { N.asList(bean.getId()) };
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, N.asList(bean.getId())).orElse(null);
    //        assertEquals(FIRST_NAME, dbAccount.getFirstName());
    //
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, bean).orElse(null);
    //        assertEquals(FIRST_NAME, dbAccount.getFirstName());
    //
    //        dbAccount = sqlExecutor.findFirst(Account.class, SELECT_ACCOUNT_BY_ID, N.asMap("id", bean.getId())).orElse(null);
    //        assertEquals(FIRST_NAME, dbAccount.getFirstName());
    //
    //        int updatedResult = sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, bean);
    //        assertEquals(1, updatedResult);
    //
    //        updatedResult = sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, "", bean.getId());
    //        assertEquals(1, updatedResult);
    //
    //        parameters = N.asArray("", bean.getId());
    //        updatedResult = sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, parameters);
    //        assertEquals(1, updatedResult);
    //
    //        parameters = N.asList("", bean.getId());
    //        updatedResult = sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, parameters);
    //        assertEquals(1, updatedResult);
    //
    //        parameters = N.asMap("firstName", "", "id", bean.getId());
    //        updatedResult = sqlExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, parameters);
    //        assertEquals(1, updatedResult);
    //    }
    //
    //    public void testTransaction_11() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.commit();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //
    //        tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.rollback();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_12() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = 0;
    //        Dataset result = null;
    //
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        try {
    //            id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            tran.commit();
    //        } finally {
    //            tran.rollbackIfNotCommitted();
    //        }
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //
    //        try {
    //            tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //            id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            tran.rollback();
    //        } finally {
    //            tran.rollbackIfNotCommitted();
    //        }
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_2() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.commit();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //
    //        tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.rollback();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_3() {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED, false);
    //        try {
    //            long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            SQLTransaction tran2 = sqlExecutor.beginTransaction(IsolationLevel.SERIALIZABLE, true);
    //
    //            try {
    //                result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //                N.println(result);
    //                assertEquals(0, result.size());
    //
    //                sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //            } finally {
    //                tran2.rollback();
    //            }
    //
    //            id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            try {
    //                tran.commit();
    //            } catch (IllegalStateException e) {
    //
    //            }
    //
    //            result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(0, result.size());
    //
    //            sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        } catch (Exception e) {
    //            tran.rollback();
    //        }
    //    }
    //
    //    public void testTransaction_commit() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        try {
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            tran.commit();
    //            result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //        } catch (Exception e) {
    //            tran.rollback();
    //        }
    //    }
    //
    //    public void testTransaction_commit_2() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.commit();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_commit_3() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.commit();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_commit_4() {
    //        method_A();
    //    }
    //
    //    void method_A() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //        long id = 0;
    //
    //        try {
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //
    //            method_B();
    //
    //            tran.commit();
    //        } catch (Exception e) {
    //            tran.rollbackIfNotCommitted();
    //        }
    //
    //        assertEquals(0, sqlExecutor.query(MySqlDef.selectAccountById, id).size());
    //    }
    //
    //    void method_B() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        try {
    //            Timestamp now = new Timestamp(System.currentTimeMillis());
    //            long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //            Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //            N.println(result);
    //            assertEquals(1, result.size());
    //        } finally {
    //            tran.rollbackIfNotCommitted();
    //        }
    //    }
    //
    //    public void testTransaction_rollback() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.rollback();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_rollback_2() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.rollback();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void testTransaction_rollback_3() {
    //        SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
    //
    //        Dataset result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(1, result.size());
    //
    //        tran.rollback();
    //        result = sqlExecutor.query(MySqlDef.selectAccountById, id);
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        sqlExecutor.update(MySqlDef.deleteAccountById, id);
    //    }
    //
    //    public void test_named() {
    //        Account account = new Account().setFirstName("updatedFirstName").setId(1000);
    //        String sql = NSC.update(Account.class).set("firstName").where(CF.eq("id")).sql();
    //        // sql: UPDATE account SET first_name = :firstName WHERE id = :id
    //        sqlExecutor.update(sql, account);
    //    }
    //
    //    @Override
    //    protected String getDomainName() {
    //        return ExtendDirtyBasicPNL._DN;
    //    }
}
