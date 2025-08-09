package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class MapperTest extends AbstractTest {

    @Test
    public void test_01() {
        // dummy test.
    }

    //
    //    @Test
    //    public void test_exist() throws Exception {
    //        final Mapper<Account, Long> mapper = sqlExecutor.mapper(Account.class, long.class);
    //
    //        long id = 99999;
    //        Account account = N.fill(Account.class);
    //        account.setId(id);
    //        N.erase(account, "contact", "devices");
    //        mapper.insert(account);
    //
    //        assertTrue(mapper.exists(id));
    //
    //        assertEquals(01, mapper.delete(CF.eq("gui", account.getGUI())));
    //
    //        assertFalse(mapper.exists(id));
    //
    //        mapper.batchDeleteByIds(N.repeat(id, 1001));
    //        mapper.batchDeleteByIds(N.asList(id));
    //
    //        mapper.batchDeleteByIds(N.repeat(id, 1001));
    //        mapper.batchDeleteByIds(N.asList(id));
    //    }
    //
    //    @Test
    //    public void test_crud_0() throws Exception {
    //        final DataSource dataSource = sqlExecutor.dataSource();
    //
    //        String sql_insert = "INSERT INTO account (gui, email_address, first_name, middle_name, last_name, birth_date, status, last_update_time, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    //        N.println(sql_insert);
    //
    //        Account account = N.fill(Account.class);
    //        long id = JdbcUtil.prepareQuery(dataSource, sql_insert, true)
    //                .setParameters(account.getGUI(), account.getEmailAddress(), account.getFirstName(), account.getMiddleName(), account.getLastName())
    //                .setTimestamp(6, account.getBirthDate())
    //                .setInt(7, account.getStatus())
    //                .setTimestamp(8, account.getLastUpdateTime())
    //                .setTimestamp(9, account.getCreatedTime())
    //                .<Long> insert()
    //                .orElseThrow();
    //
    //        String sql_get = PSC.select("id", "firstName", "lastName").from(Account.class).where("id = ?").sql();
    //        N.println(sql_get);
    //        Account dbAccount = JdbcUtil.prepareQuery(dataSource, sql_get).setLong(1, id).get(Account.class).orElseThrow();
    //        N.println(dbAccount);
    //
    //        String sql_update = PSC.update(Account.class).set("firstName").where("id = ?").sql();
    //        N.println(sql_update);
    //        JdbcUtil.prepareQuery(dataSource, sql_update).setString(1, "updatedFirstName").setLong(2, id).update();
    //
    //        dbAccount = JdbcUtil.prepareQuery(dataSource, sql_get).setLong(1, id).get(Account.class).orElseThrow();
    //        N.println(dbAccount);
    //
    //        String sql_delete = PSC.deleteFrom(Account.class).where("id = ?").sql();
    //        N.println(sql_delete);
    //        JdbcUtil.prepareQuery(dataSource, sql_delete).setLong(1, id).update();
    //    }
    //
    //    @Test
    //    public void test_crud_1() throws Exception {
    //        String sql_insert = NSC.insertInto(Account.class, N.asSet("id")).sql();
    //        N.println(sql_insert);
    //
    //        Account account = N.fill(Account.class);
    //        long id = sqlExecutor.insert(sql_insert, account);
    //
    //        String sql_get = NSC.select("id", "firstName", "lastName").from(Account.class).where("id = ?").sql();
    //        N.println(sql_get);
    //        Account dbAccount = sqlExecutor.get(Account.class, sql_get, id).orElseThrow();
    //        N.println(dbAccount);
    //
    //        dbAccount.setFirstName("updatedFirstName");
    //        String sql_update = NSC.update(Account.class).set("firstName").where("id = :id").sql();
    //        N.println(sql_update);
    //        sqlExecutor.update(sql_update, dbAccount);
    //
    //        dbAccount = sqlExecutor.get(Account.class, sql_get, id).orElseThrow();
    //        N.println(dbAccount);
    //
    //        String sql_delete = NSC.deleteFrom(Account.class).where("id = ?").sql();
    //        N.println(sql_delete);
    //        sqlExecutor.update(sql_delete, id);
    //    }
    //
    //    @Test
    //    public void test_crud_2() throws Exception {
    //        final Mapper<Account, Long> mapper = sqlExecutor.mapper(Account.class, long.class);
    //
    //        Account account = N.fill(Account.class);
    //        account.setId(0);
    //        account.setLastUpdateTime(null);
    //        account.setCreatedTime(null);
    //        mapper.insert(account);
    //
    //        Account dbAccount = mapper.gett(account.getId());
    //        N.println(dbAccount);
    //
    //        dbAccount.setFirstName("updatedFirstName");
    //        mapper.update(dbAccount, N.asList("firstName"));
    //
    //        dbAccount = mapper.gett(account.getId());
    //        N.println(dbAccount);
    //
    //        mapper.delete(dbAccount);
    //    }
    //
    //    @Test
    //    public void test_crud_3() throws Exception {
    //        final Mapper<Account, Long> mapper = sqlExecutor.mapper(Account.class, long.class);
    //        final List<String> selectPropNames = N.asList("id", "gui", "emailAddress", "firstName", "lastName", "createdTime");
    //
    //        Account account = N.fill(Account.class);
    //        long id = mapper.insert(account, N.asList("gui", "emailAddress", "firstName", "lastName", "createdTime"));
    //        Account dbAccount = mapper.gett(id, selectPropNames);
    //
    //        dbAccount.setFirstName("newFirstName");
    //        mapper.update(dbAccount);
    //        dbAccount = mapper.findFirst(selectPropNames, CF.eq("gui", dbAccount.getGUI())).get();
    //        assertEquals("newFirstName", dbAccount.getFirstName());
    //
    //        assertEquals(1, mapper.delete(dbAccount));
    //        assertEquals(0, mapper.delete(dbAccount));
    //
    //        mapper.insert(account);
    //        assertEquals(01, mapper.delete(CF.eq("gui", account.getGUI())));
    //    }
    //
    //    @Test
    //    public void test_batchAdd() throws Exception {
    //        final Mapper<Account, Long> mapper = sqlExecutor.mapper(Account.class, long.class);
    //        final List<String> selectPropNames = N.asList("id", "gui", "emailAddress", "firstName", "lastName", "createdTime");
    //
    //        List<Account> accounts = N.fill(Account.class, 11);
    //        for (Account account : accounts) {
    //            account.setId(0);
    //            N.erase(account, "id", "contact", "devices");
    //        }
    //
    //        List<Long> ids = mapper.batchInsert(accounts);
    //
    //        for (Account account : accounts) {
    //            account.setFirstName("newFirstName");
    //        }
    //
    //        mapper.batchUpdate(accounts);
    //
    //        List<Account> dbAccounts = mapper.list(selectPropNames, CF.eq("firstName", "newFirstName"));
    //        assertEquals(accounts.size(), dbAccounts.size());
    //
    //        for (Account dbAccount : dbAccounts) {
    //            dbAccount.setLastName("newLastName");
    //        }
    //
    //        mapper.batchUpdate(dbAccounts);
    //        dbAccounts = mapper.list(selectPropNames, CF.eq("lastName", "newLastName"));
    //        assertEquals(accounts.size(), dbAccounts.size());
    //
    //        assertEquals(accounts.size(), mapper.batchDeleteByIds(ids));
    //
    //        dbAccounts = mapper.list(selectPropNames, CF.eq("lastName", "newLastName"));
    //        assertEquals(0, dbAccounts.size());
    //    }
    //
    //    @Test
    //    public void test_batchAll() throws Exception {
    //        final Mapper<Account, Long> mapper = sqlExecutor.mapper(Account.class, long.class);
    //        final List<String> selectPropNames = N.asList("id", "gui", "emailAddress", "firstName", "lastName", "createdTime");
    //
    //        List<Account> accounts = N.fill(Account.class, 201);
    //        for (Account account : accounts) {
    //            account.setId(0);
    //            N.erase(account, "id", "contact", "devices");
    //        }
    //
    //        List<Long> ids = mapper.batchInsert(accounts);
    //
    //        for (Account account : accounts) {
    //            account.setFirstName("newFirstName");
    //        }
    //
    //        List<Account> dbAccounts = mapper.batchGet(ids);
    //        assertEquals(dbAccounts.size(), ids.size());
    //
    //        mapper.batchUpdate(accounts);
    //
    //        dbAccounts = mapper.list(selectPropNames, CF.eq("firstName", "newFirstName"));
    //        assertEquals(accounts.size(), dbAccounts.size());
    //
    //        for (Account dbAccount : dbAccounts) {
    //            dbAccount.setLastName("newLastName");
    //        }
    //
    //        mapper.batchUpdate(dbAccounts);
    //        dbAccounts = mapper.list(selectPropNames, CF.eq("lastName", "newLastName"));
    //        assertEquals(accounts.size(), dbAccounts.size());
    //
    //        assertEquals(accounts.size(), mapper.batchDelete(dbAccounts));
    //
    //        dbAccounts = mapper.list(selectPropNames, CF.eq("lastName", "newLastName"));
    //        assertEquals(0, dbAccounts.size());
    //
    //        assertEquals(0, mapper.batchDelete(accounts));
    //    }
    //
    //    //    @Test
    //    //    public void test_query() throws Exception {
    //    //        final Mapper<Account> mapper = sqlExecutor.mapper(Account.class);
    //    //        final List<String> selectPropNames = N.asList("id", "gui", "emailAddress", "firstName", "lastName", "createdTime");
    //    //
    //    //        final List<Account> accounts = N.fill(Account.class, 11);
    //    //        for (Account account : accounts) {
    //    //            N.erase(account, "id", "contact", "devices");
    //    //            account.setLastName("lastName");
    //    //        }
    //    //
    //    //        final List<Long> ids = mapper.batchAdd(accounts);
    //    //
    //    //        IntStream.range(0, 1000).parallel(16).forEach(new IntConsumer() {
    //    //            @Override
    //    //            public void accept(int t) {
    //    //                assertTrue(mapper.queryForSingleResult(String.class, "gui", L.eq("lastName", "lastName")).isPresent());
    //    //
    //    //                assertTrue(mapper.findFirst(L.eq("lastName", "lastName")).isPresent());
    //    //
    //    //                assertEquals(accounts.size(), mapper.find(selectPropNames, L.eq("lastName", "lastName")).size());
    //    //
    //    //                assertEquals(accounts.size(), mapper.query(selectPropNames, L.gt("id", 0)).size());
    //    //
    //    //                mapper.stream(selectPropNames, L.eq("lastName", "lastName")).run(new Try.Consumer<Stream<Account>, Exception>() {
    //    //                    @Override
    //    //                    public void accept(Stream<Account> t) {
    //    //                        assertEquals(accounts.size(), t.count());
    //    //                    }
    //    //                });
    //    //            }
    //    //        });
    //    //
    //    //        assertEquals(accounts.size(), mapper.batchDelete(ids));
    //    //
    //    //        assertEquals(0, mapper.find(selectPropNames, L.eq("lastName", "lastName")).size());
    //    //    }
}
