/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class AsyncSQLExecutorTest extends AbstractTest {

    @Test
    public void test_01() {
        // dummy test.
    }

    //    @Override
    //    public void setUp() {
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
    //    }
    //
    //    @Override
    //    public void tearDown() {
    //        sqlExecutor.update(MySqlDef.deleteAllAccount);
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
    //        DataSet result = sqlExecutor.query(sql_1, uuid);
    //        N.println(result);
    //
    //        sqlExecutor.insert(insertAccount_2, "fn", "ln", uuid, now, now);
    //
    //        result = asyncSQLExecutor.query(sql_2, uuid).get();
    //        N.println(result);
    //
    //        result = asyncSQLExecutor.queryAll(N.asList(sql_1, sql_2), null, uuid).get();
    //        N.println(result);
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
    //        assertFalse(asyncSQLExecutor.sync().createTableIfNotExists(tableName, schema));
    //
    //        assertTrue(asyncSQLExecutor.sync().dropTableIfExists(tableName));
    //
    //        assertFalse(asyncSQLExecutor.sync().dropTableIfExists(tableName));
    //
    //        assertTrue(asyncSQLExecutor.sync().createTableIfNotExists(tableName, schema));
    //    }
    //
    //    @Test
    //    public void test_query() throws Exception {
    //        Connection conn = asyncSQLExecutor.sync().getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, "1", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ResultSetExtractor.TO_DATA_SET, id).get();
    //        N.println(result);
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ResultSetExtractor.TO_DATA_SET, null, id).get();
    //        N.println(result);
    //
    //        //
    //        //        Map<String, Object> m = asyncSQLExecutor.queryForMap(MySqlDef.selectAccountById, id).get();
    //        //        N.println(m);
    //        //        assertEquals(id, m.get("id"));
    //        //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        asyncSQLExecutor.sync().closeConnection(conn);
    //    }
    //
    //    @Test
    //    public void test_singleQuery() throws Exception {
    //
    //        sqlExecutor.execute("ALTER TABLE account AUTO_INCREMENT = 1");
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, "1", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        String sql = "select first_name from account where id = ?";
    //        boolean bool = asyncSQLExecutor.queryForBoolean(sql, id).get().orElse(false);
    //        assertTrue(bool);
    //
    //        char ch = asyncSQLExecutor.queryForChar(sql, id).get().orElse((char) 0);
    //        assertEquals('1', ch);
    //
    //        byte b = asyncSQLExecutor.queryForByte(sql, id).get().orElse((byte) 0);
    //        assertEquals(1, b);
    //
    //        short s = asyncSQLExecutor.queryForShort(sql, id).get().orElse((short) 0);
    //        assertEquals(1, s);
    //
    //        int i = asyncSQLExecutor.queryForInt(sql, id).get().orElse(0);
    //        assertEquals(1, i);
    //
    //        long l = asyncSQLExecutor.queryForLong(sql, id).get().orElse(0);
    //        assertEquals(1, l);
    //
    //        float f = asyncSQLExecutor.queryForFloat(sql, id).get().orElse(0);
    //        assertEquals(1f, f);
    //
    //        double d = asyncSQLExecutor.queryForDouble(sql, id).get().orElse(0);
    //        assertEquals(1d, d);
    //
    //        sql = "select id from account where id = ?";
    //
    //        i = asyncSQLExecutor.queryForInt(sql, id).get().orElse(0);
    //        assertEquals(id, i);
    //
    //        l = asyncSQLExecutor.queryForLong(sql, id).get().orElse(0);
    //        assertEquals(id, l);
    //
    //        f = asyncSQLExecutor.queryForFloat(sql, id).get().orElse(0);
    //        assertEquals((float) id, f);
    //
    //        d = asyncSQLExecutor.queryForDouble(sql, id).get().orElse(0);
    //        assertEquals((double) id, d);
    //
    //        d = asyncSQLExecutor.queryForSingleResult(double.class, sql, id).get().orElse(0d);
    //        assertEquals((double) id, d);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
    //
    //    public void testInsert2() throws Exception {
    //        Connection conn = asyncSQLExecutor.sync().getConnection();
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) asyncSQLExecutor.insert(conn, MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        StatementSetter statementSetter = null;
    //        id = (Long) asyncSQLExecutor.insert(conn, MySqlDef.insertAccount, statementSetter, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, statementSetter, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        JdbcSettings jdbcSettings = null;
    //        id = (Long) asyncSQLExecutor.insert(conn, MySqlDef.insertAccount, statementSetter, jdbcSettings, "fn", "ln", UUID.randomUUID().toString(), now, now)
    //                .get();
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        asyncSQLExecutor.sync().closeConnection(conn);
    //    }
    //
    //    public void test_batchInsert() throws Exception {
    //        Connection conn = asyncSQLExecutor.sync().getConnection();
    //        Account account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        Account account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, null, null, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(conn, ExtendDirtySqlMapper.INSERT_ACCOUNT, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        // ...
    //        account = createAccount(Account.class);
    //        asyncSQLExecutor.batchInsert(ExtendDirtySqlMapper.INSERT_ACCOUNT, null, null, N.asList(account)).get();
    //
    //        account2 = asyncSQLExecutor.findFirst(Account.class, ExtendDirtySqlMapper.SELECT_ACCOUNT_BY_ID, account).get().orElse(null);
    //        assertEquals(account.getGUI(), account.getGUI());
    //
    //        asyncSQLExecutor.batchUpdate(ExtendDirtySqlMapper.DELETE_ACCOUNT_BY_ID, N.asList(account)).get();
    //        account2 = asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, account.getId()).get().orElse(null);
    //
    //        assertNull(account2);
    //
    //        asyncSQLExecutor.sync().closeConnection(conn);
    //    }
    //
    //    public void test_update() throws Exception {
    //        Connection conn = asyncSQLExecutor.sync().getConnection();
    //
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        long id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //
    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName1", id).get();
    //        assertEquals("updatedFirstName1", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        asyncSQLExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName2", id).get();
    //        assertEquals("updatedFirstName2", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        asyncSQLExecutor.update(MySqlDef.updateAccountFirstNameById, "updatedFirstName3", id).get();
    //        assertEquals("updatedFirstName3", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        asyncSQLExecutor.update(conn, MySqlDef.updateAccountFirstNameById, "updatedFirstName4", id).get();
    //        assertEquals("updatedFirstName4", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        List<Object> listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName5", id));
    //        asyncSQLExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, listOfParameters).get();
    //        assertEquals("updatedFirstName5", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName6", id));
    //        asyncSQLExecutor.batchUpdate(MySqlDef.updateAccountFirstNameById, listOfParameters).get();
    //        assertEquals("updatedFirstName6", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName7", id));
    //        asyncSQLExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, listOfParameters).get();
    //        assertEquals("updatedFirstName7", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        listOfParameters = new ArrayList<>();
    //        listOfParameters.add(N.asArray("updatedFirstName8", id));
    //        asyncSQLExecutor.batchUpdate(conn, MySqlDef.updateAccountFirstNameById, listOfParameters).get();
    //        assertEquals("updatedFirstName8", asyncSQLExecutor.findFirst(Account.class, MySqlDef.selectAccountById, id).get().get().getFirstName());
    //
    //        asyncSQLExecutor.execute(MySqlDef.deleteAccountById, id).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        asyncSQLExecutor.sync().closeConnection(conn);
    //    }
    //
    //    //    public void testTransaction() throws Exception {
    //    //        SQLTransaction tran = asyncSQLExecutor.sync().beginTransaction(IsolationLevel.READ_COMMITTED);
    //    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //    //        long id = (Long) asyncSQLExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now).get();
    //    //
    //    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //    //        N.println(result);
    //    //        assertEquals(1, result.size());
    //    //
    //    //        tran.commit();
    //    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, id).get();
    //    //        N.println(result);
    //    //        assertEquals(1, result.size());
    //    //
    //    //        asyncSQLExecutor.update(MySqlDef.deleteAccountById, id).get();
    //    //    }
    //
    //    public void testCRUD() throws InterruptedException, ExecutionException {
    //        Account account = createAccount(Account.class);
    //        Future<Long> f = asyncSQLExecutor.insert(INSERT_ACCOUNT, account);
    //        long id = f.get();
    //
    //        DataSet dataSet = asyncSQLExecutor.query(SELECT_ACCOUNT_BY_ID, account.getId()).get();
    //        N.println(dataSet);
    //        assertEquals(1, dataSet.size());
    //
    //        account.setFirstName("updatedFirtName");
    //
    //        int result = asyncSQLExecutor.update(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, account).get();
    //        N.println(dataSet);
    //        assertEquals(1, result);
    //
    //        result = asyncSQLExecutor.update(DELETE_ACCOUNT_BY_ID, id).get();
    //        N.println(dataSet);
    //        assertEquals(1, result);
    //        dataSet = asyncSQLExecutor.query(SELECT_ACCOUNT_BY_ID, id).get();
    //        assertEquals(0, dataSet.size());
    //    }
    //
    //    public void testBatchCRUDWithListParamter() throws InterruptedException, ExecutionException {
    //        int count = 999;
    //        List<Account> accounts = new ArrayList<>();
    //
    //        for (int i = 0; i < count; i++) {
    //            accounts.add(createAccount(Account.class));
    //        }
    //
    //        Future<List<Long>> f = asyncSQLExecutor.batchInsert(INSERT_ACCOUNT, accounts);
    //        List<Long> ids = f.get();
    //        assertEquals(count, ids.size());
    //
    //        DataSet dataSet = asyncSQLExecutor.query(SELECT_ALL_ACCOUNT).get();
    //        N.println(dataSet);
    //        assertEquals(count, dataSet.size());
    //
    //        int result = asyncSQLExecutor.batchUpdate(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, accounts).get();
    //        N.println(dataSet);
    //        assertEquals(count, result);
    //
    //        result = asyncSQLExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts).get();
    //        N.println(dataSet);
    //
    //        dataSet = asyncSQLExecutor.query(SELECT_ALL_ACCOUNT).get();
    //        N.println(dataSet);
    //        assertEquals(0, dataSet.size());
    //    }
    //
    //    public void testBatchCRUDWithArrayParameters() throws InterruptedException, ExecutionException {
    //        int count = 999;
    //        Account[] accounts = new Account[count];
    //
    //        for (int i = 0; i < count; i++) {
    //            accounts[i] = createAccount(Account.class);
    //        }
    //
    //        Future<List<Long>> f = asyncSQLExecutor.batchInsert(INSERT_ACCOUNT, N.asList(accounts));
    //        List<Long> ids = f.get();
    //        assertEquals(count, ids.size());
    //
    //        DataSet dataSet = asyncSQLExecutor.query(SELECT_ALL_ACCOUNT).get();
    //        N.println(dataSet);
    //        assertEquals(count, dataSet.size());
    //
    //        int result = asyncSQLExecutor.batchUpdate(UPDATE_ACCOUNT_FIRST_NAME_BY_ID, N.asList(accounts)).get();
    //        N.println(dataSet);
    //        assertEquals(count, result);
    //
    //        result = asyncSQLExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, N.asList(accounts)).get();
    //        N.println(dataSet);
    //
    //        dataSet = asyncSQLExecutor.query(SELECT_ALL_ACCOUNT).get();
    //        N.println(dataSet);
    //        assertEquals(0, dataSet.size());
    //    }
    //
    //    @Test
    //    public void test_batchInsert_2() throws Exception {
    //        Timestamp now = new Timestamp(System.currentTimeMillis());
    //        Object[] array = new Object[999];
    //
    //        for (int i = 0; i < array.length; i++) {
    //            array[i] = N.asArray("fn", "ln", UUID.randomUUID().toString(), now, now);
    //        }
    //
    //        List<Long> ids = (List) asyncSQLExecutor.batchInsert(MySqlDef.insertAccount, N.asList(array)).get();
    //
    //        DataSet result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ids.get(0)).get();
    //        N.println(result);
    //
    //        asyncSQLExecutor.update(MySqlDef.deleteAllAccount).get();
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ids.get(0)).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        ((Object[]) array[array.length - 2])[2] = ((Object[]) array[array.length - 1])[2];
    //
    //        try {
    //            ids = (List) asyncSQLExecutor.batchInsert(MySqlDef.insertAccount, N.asList(array)).get();
    //            fail("Should throw AbacusSQLException");
    //        } catch (ExecutionException e) {
    //        }
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ids.get(0)).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //
    //        try {
    //            asyncSQLExecutor.batchUpdate(MySqlDef.insertAccount, N.asList(array)).get();
    //            fail("Should throw AbacusSQLException");
    //        } catch (ExecutionException e) {
    //        }
    //
    //        result = asyncSQLExecutor.query(MySqlDef.selectAccountById, ids.get(0)).get();
    //        N.println(result);
    //        assertEquals(0, result.size());
    //    }
}
