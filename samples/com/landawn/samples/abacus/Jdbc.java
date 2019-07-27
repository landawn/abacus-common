package com.landawn.samples.abacus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.junit.Test;

import com.landawn.abacus.IsolationLevel;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.JdbcUtil;
import com.landawn.abacus.util.JdbcUtil.Dao;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SQLBuilder;
import com.landawn.abacus.util.SQLBuilder.NSC;
import com.landawn.abacus.util.SQLBuilder.PSC;
import com.landawn.abacus.util.SQLExecutor;
import com.landawn.abacus.util.SQLExecutor.Mapper;
import com.landawn.abacus.util.SQLTransaction;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CRUD: insert -> read -> update -> delete a record in DB table.
 */
public class Jdbc {
    static final DataSource dataSource = JdbcUtil.createDataSource("jdbc:h2:~/test", "sa", "");
    static final SQLExecutor sqlExecutor = new SQLExecutor(dataSource);
    static final Mapper<User, Long> userMapper = sqlExecutor.mapper(User.class);
    static final UserDao userDao = Dao.newInstance(UserDao.class, dataSource);

    // initialize DB schema.
    static {
        final String sql_user_drop_table = "DROP TABLE IF EXISTS user";

        final String sql_user_creat_table = "CREATE TABLE IF NOT EXISTS user (" //
                + "id bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY, " //
                + "first_name varchar(32) NOT NULL, " //
                + "last_name varchar(32) NOT NULL, " //
                + "email varchar(32), " //
                + "create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP)";

        sqlExecutor.execute(sql_user_drop_table);
        sqlExecutor.execute(sql_user_creat_table);
    }

    // Entity Object mapped to record in DB table.
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        @Id
        private long id;
        private String firstName;
        private String lastName;
        private String email;
        @ReadOnly
        private Timestamp createTime;
    }

    @Test
    public void crud_by_Jdbc() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            final String sql = PSC.insertInto(User.class).sql();
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, 100);
            stmt.setString(2, "Forrest");
            stmt.setString(3, "Gump");
            stmt.setString(4, "123@email.com");
            stmt.execute();
        } finally {
            JdbcUtil.closeQuietly(stmt, conn);
        }

        User userFromDB = null;

        try {
            final String sql = PSC.selectFrom(User.class).where("id = ?").sql();

            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, 100);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    userFromDB = User.builder()
                            .id(rs.getLong(1))
                            .firstName(rs.getString(2))
                            .lastName(rs.getString(3))
                            .email(rs.getString(4))
                            .createTime(rs.getTimestamp(5))
                            .build();
                }
            }
        } finally {
            JdbcUtil.closeQuietly(stmt, conn);
        }

        System.out.println(userFromDB);

        try {
            final String sql = PSC.update(User.class).set("firstName", "lastName").where("id = ?").sql();

            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "Tom");
            stmt.setString(2, "Hanks");
            stmt.setLong(3, 100);
            stmt.execute();
        } finally {
            JdbcUtil.closeQuietly(stmt, conn);
        }

        try {
            final String sql = PSC.deleteFrom(User.class).where("id = ?").sql();

            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, 100);
            stmt.execute();
        } finally {
            JdbcUtil.closeQuietly(stmt, conn);
        }

        // As we see, the problems here are:
        // 1, Manually create/open and close Connection/Statement/ResultSet.
        // 2, Manually set parameters to Statement.
        // 3, Manually read records from ResultSet.
        // If you're working on small project, where 20 tables defined with average 20 columns.
        // On average, if 20 (select/insert/update/delete) queries/methods created for each table, 
        // On average, each query/method requires 20 lines codes. 20 tables * 20 queries/table * 20 lines/query = 8000 lines.
        // A lot of efforts will be paid to write the codes and maintain (add/update/delete/rename/... tables/columns).
    }

    // A bit(lot?) improvements:
    @Test
    public void crud_by_PreparedQuery() throws SQLException {

        String sql = PSC.insertInto(User.class).sql();
        JdbcUtil.prepareQuery(dataSource, sql) //
                .setLong(1, 100)
                .setString(2, "Forrest")
                .setString(3, "Gump")
                .setString(4, "123@email.com")
                .insert();

        sql = PSC.selectFrom(User.class).where("id = ?").sql();
        JdbcUtil.prepareQuery(dataSource, sql) //
                .setLong(1, 100)
                .get(User.class) // or findFirst/list/stream/... a lot more we can do.
                .ifPresent(System.out::println);

        sql = PSC.update(User.class).set("firstName", "lastName").where("id = ?").sql();
        JdbcUtil.prepareQuery(dataSource, sql) //
                .setString(1, "Tom")
                .setString(2, "Hanks")
                .setLong(3, 100)
                .update();

        sql = PSC.deleteFrom(User.class).where("id = ?").sql();
        JdbcUtil.prepareQuery(dataSource, sql) //
                .setLong(1, 100)
                .update();

        // As we see, the problems here are:
        // 1, No need to manually create/open and close Connection/Statement/ResultSet.
        // 2, No need to manually read records from ResultSet.
        // 3, Still need to manually set parameters to Statement.
        // 4, Flexible/fluent APIs
    }

    @Test
    public void crud_by_SQLExecutor() {
        String sql = NSC.insertInto(User.class).sql();
        User user = User.builder().id(100).firstName("Forrest").lastName("Gump").email("123@email.com").build();
        sqlExecutor.insert(sql, user);

        sql = NSC.selectFrom(User.class).where("id = :id").sql();
        User userFromDB = sqlExecutor.get(User.class, sql, 100).orNull();
        System.out.println(userFromDB);

        sql = NSC.update(User.class).set("firstName", "lastName").where("id = :id").sql();
        sqlExecutor.update(sql, N.asList("Tom", "Hanks", 100));

        sql = NSC.deleteFrom(User.class).where("id = :id").sql();
        sqlExecutor.update(sql, 100);

        // As we see, the problems here are:
        // 1, No need to manually create/open and close Connection/Statement/ResultSet.
        // 2, No need to manually read records from ResultSet.
        // 3, No need to manually set parameters to Statement.
        // 4, Flexible/fluent APIs
        // But how to manage/maintain/reuse the tens/hundreds/thousands of SQL scripts?
        // see below samples by Dao/Mapper: 
    }

    public static interface UserDao extends JdbcUtil.CrudDao<User, Long, SQLBuilder.PSC> {
        @NamedInsert("INSERT INTO user (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)")
        void insertWithId(User user) throws SQLException;

        @NamedUpdate("UPDATE user SET first_name = :firstName, last_name = :lastName WHERE id = :id")
        int updateFirstAndLastName(@Bind("firstName") String newFirstName, @Bind("lastName") String newLastName, @Bind("id") long id) throws SQLException;

        @NamedSelect("SELECT first_name, last_name FROM user WHERE id = :id")
        User getFirstAndLastNameBy(@Bind("id") long id) throws SQLException;

        @NamedSelect("SELECT id, first_name, last_name, email FROM user")
        Stream<User> allUsers() throws SQLException;
    }

    @Test
    public void crud_by_Dao() throws SQLException {
        User user = User.builder().id(100).firstName("Forrest").lastName("Gump").email("123@email.com").build();
        userDao.insertWithId(user);

        User userFromDB = userDao.gett(100L);
        System.out.println(userFromDB);

        userDao.updateFirstAndLastName("Tom", "Hanks", 100);

        userDao.allUsers().map(e -> e.getFirstName() + " " + e.getLastName()).forEach(Fn.println());

        userDao.deleteById(100L);

        // As we see, the problems here are:
        // 1, No need to manually create/open and close Connection/Statement/ResultSet.
        // 2, No need to manually read records from ResultSet.
        // 3, No need to manually set parameters to Statement.
        // 4, Flexible/fluent APIs
        // 5, SQL scripts are managed/mapped with static type info, without any implementation required.
        // 6, But how about if we don't want to write any SQL scripts?
        // Try Mapper, see below sample:
    }

    @Test
    public void crud_by_Mapper() {
        User user = User.builder().id(100).firstName("Forrest").lastName("Gump").email("123@email.com").build();
        userMapper.insert(user);

        User userFromDB = userMapper.gett(100L);
        System.out.println(userFromDB);

        userMapper.update(N.asProps("firstName", "Tom", "lastName", "Hanks"), CF.eq("id", 100));

        userMapper.deleteById(100L);

        // How about transaction?
        // See last sample.
    }

    @Test
    public void test_transaction() throws SQLException {
        User user = User.builder().id(100).firstName("Forrest").lastName("Gump").email("123@email.com").build();
        userDao.insertWithId(user);

        User userFromDB = userDao.gett(100L);
        System.out.println(userFromDB);

        SQLTransaction tran = JdbcUtil.beginTransaction(dataSource, IsolationLevel.DEFAULT);

        try {
            userDao.updateFirstAndLastName("Tom", "Hanks", 100);

            userDao.queryForBoolean("firstName", CF.eq("id", 100)); // throw exception.
            tran.commit();
        } catch (SQLException e) {
            // ignore
        } finally {
            tran.rollbackIfNotCommitted();
        }

        assertEquals("Forrest", userDao.gett(100L).getFirstName());

        tran = JdbcUtil.beginTransaction(dataSource, IsolationLevel.DEFAULT);

        try {
            userDao.updateFirstAndLastName("Tom", "Hanks", 100);

            tran.commit();
        } finally {
            tran.rollbackIfNotCommitted();
        }

        assertEquals("Tom", userDao.gett(100L).getFirstName());

        userDao.deleteById(100L);
        assertNull(userDao.gett(100L));

        // In you're in Spring and want to use Spring transaction management, 
        // then don't need to call beginTransaction because Spring transaction is integrated and supported. 
    }
}
