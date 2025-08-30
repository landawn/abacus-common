/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.annotation.JsonXmlField;

import lombok.Builder;
import lombok.Data;

public class CSVUtilTest extends AbstractTest {
    @Test
    public void test_001() throws Exception {

        String csv = """
                id,name,date
                123,nameA,01-JAN-2025 12:00:00
                456,    \\"nameB, 01-FEB-2025 12:00:00
                ,,
                ,

                ,,
                789,    "\""nameC", 01-MAR-2025 12:00:00
                """;

        File file = new File("./src/test/resources/test_001.csv");

        IOUtil.write(csv, file);
        Dataset dataset = CSVUtil.loadCSV(file, ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        dataset = CSVUtil.loadCSV(file, N.asList("name", "date"), ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        dataset = CSVUtil.loadCSV(file, N.asList("id", "date"), ClassA.class);
        dataset.println();

        N.println(Strings.repeat("=", 80));

        List<ClassA> list = CSVUtil.stream(file, N.asList("name"), ClassA.class).toList();
        list.forEach(Fn.println());

        N.println(Strings.repeat("=", 80));

        dataset = CSVUtil.stream(file, N.asList("name"), ClassA.class).toDataset();
        dataset.println();

        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_load() throws Exception {
        File file = new File("./src/test/resources/test_a.csv");

        //    List<ClassA> list = new ArrayList<>();
        //    list.add(ClassA.builder().id(123).name("nameA").date(DateUtil.currentDate()).build());
        //    list.add(ClassA.builder().id(123).name("nameA").date(DateUtil.currentDate()).build());
        //    Stream.of(list).toDataset().toCsv(file);

        // CSVUtil.setCSVLineParser(CSVUtil.CSV_LINE_PARSER_IN_JSON);
        Dataset dataset = CSVUtil.loadCSV(file, ClassA.class);

        dataset.println();

        CSVUtil.stream(file, ClassA.class).forEach(Fn.println());

        CSVUtil.stream(file, N.asList("name"), ClassA.class).forEach(Fn.println());

        CSVUtil.stream(file, N.asSet("date"), ClassA.class).forEach(Fn.println());

    }

    @Builder
    @Data
    public static class ClassA {
        private long id;
        private String name;
        @JsonXmlField(dateFormat = "dd-MMM-yyyy HH:mm:ss")
        private Date date;
    }

    //    public void testImportCSV() throws SQLException {
    //        List<Account> accounts = new ArrayList<>();
    //
    //        for (int i = 0; i < 101; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName(uuid.substring(0, 16));
    //            account.setLastName(uuid.substring(0, 16));
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
    //        result = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result.println();
    //
    //        Connection conn = sqlExecutor.getConnection();
    //        File file = new File("./src/test/resources/test.csv");
    //        String sql = "SELECT first_name, last_name, gui, last_update_time, create_time FROM account";
    //        JdbcUtils.exportCSV(file, conn, sql);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        Dataset result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size(), result2.size());
    //
    //        // ---------------------
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
    //        if (file.exists()) {
    //            file.delete();
    //        }
    //
    //        // CSVUtil.exportCSV(file, result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size()));
    //        result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time")).toCsv(file);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, Type.ofAll(String.class, String.class, String.class, Timestamp.class, Timestamp.class));
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtil.BiParametersSetter<PreparedStatement, Object[]> func = new JdbcUtil.BiParametersSetter<PreparedStatement, Object[]>() {
    //            @Override
    //            public void accept(PreparedStatement stmt, Object[] strs) throws SQLException {
    //                N.println(strs);
    //                stmt.setObject(1, strs[0]);
    //                stmt.setObject(2, strs[1]);
    //                stmt.setString(3, Strings.guid());
    //                stmt.setTimestamp(4, DateUtil.parseTimestamp(strs[3].toString()));
    //                stmt.setTimestamp(5, DateUtil.parseTimestamp(strs[4].toString()));
    //            }
    //        };
    //
    //        JdbcUtils.importCSV(file, conn, sql, func);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        result2.println();
    //        assertEquals(result.size() * 2, result2.size());
    //
    //        sqlExecutor.update(DELETE_ALL_ACCOUNT);
    //
    //        result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
    //        assertEquals(0, result2.size());
    //
    //        sqlExecutor.closeConnection(conn);
    //        IOUtil.deleteIfExists(file);
    //    }

    //    public void testImportCSV_1() {
    //        final Random rand = new Random();
    //        List<Account> accounts = new ArrayList<>();
    //
    //        for (int i = 0; i < 101; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-\" \\ ' , \", ");
    //            account.setLastName("ln-" + (rand.nextInt() % (101 / 10)));
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
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, types);
    //
    //        Dataset result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
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
    //        // CSVUtil.exportCSV(file, result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size()));
    //        result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time")).toCsv(file);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
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
    //        sqlExecutor.closeConnection(conn);
    //        IOUtil.deleteIfExists(file);
    //    }

    //    public void testImportCSV_2() {
    //        final Random rand = new Random();
    //        List<Account> accounts = new ArrayList<>();
    //
    //        for (int i = 0; i < 101; i++) {
    //            Account account = new Account();
    //            String uuid = Strings.uuid() + "-" + i;
    //            account.setGUI(uuid);
    //            account.setFirstName("fn-\" \\ ' , \", ");
    //            account.setLastName("ln-" + (rand.nextInt() % (101 / 10)));
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
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
    //
    //        sql = "INSERT INTO account (first_name, last_name, gui, last_update_time, create_time) VALUES ( ?,  ?,  ?, ?, ?)";
    //        JdbcUtils.importCSV(file, conn, sql, types);
    //
    //        Dataset result2 = sqlExecutor.query(SELECT_ALL_ACCOUNT);
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
    //        // CSVUtil.exportCSV(file, result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time"), 0, result.size()));
    //        result.copy(N.asList("first_name", "last_name", "gui", "last_update_time", "create_time")).toCsv(file);
    //
    //        sqlExecutor.batchUpdate(DELETE_ACCOUNT_BY_ID, accounts);
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
    //        sqlExecutor.closeConnection(conn);
    //        IOUtil.deleteIfExists(file);
    //    }

}
