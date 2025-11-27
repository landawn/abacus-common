package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class SeqTest extends AbstractTest {

    @Test
    public void test_bufferred() throws Exception {
        final int millisToSleep = 100;
        final int elementCount = 10;

        long startTime = System.currentTimeMillis();
        Seq.range(0, elementCount).delay(Duration.ofMillis(millisToSleep)).buffered().map(it -> it * 2).delay(Duration.ofMillis(millisToSleep)).println();
        N.println("Duration: " + (System.currentTimeMillis() - startTime));

        assertTrue(System.currentTimeMillis() - startTime < millisToSleep * elementCount * 2);

        startTime = System.currentTimeMillis();
        Seq.range(0, elementCount).delay(Duration.ofMillis(millisToSleep)).map(it -> it * 2).delay(Duration.ofMillis(millisToSleep)).println();
        N.println("Duration: " + (System.currentTimeMillis() - startTime));

        assertTrue(System.currentTimeMillis() - startTime > millisToSleep * elementCount * 2);
    }

    @Test
    public void test_transform() throws Exception {

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2)).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2), false).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2), true).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).sps(s -> s.map(e -> e * 2)).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
            throw new SQLException("TheXyzSQLException");
        }).sps(64, s -> s.map(e -> e * 2)).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
                throw new SQLException("TheXyzSQLException");
            }).transformB(s -> s.map(e -> e * 2)).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
                throw new SQLException("TheXyzSQLException");
            }).transformB(s -> s.map(e -> e * 2), true).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).onEach(e -> {
                throw new SQLException("TheXyzSQLException");
            }).sps(s -> s.map(e -> e * 2)).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }
    }

    @Test
    public void test_transform2() throws Exception {
        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).onEachE(e -> {
            throw new SQLException("TheXyzSQLException");
        })).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).onEachE(e -> {
            throw new SQLException("TheXyzSQLException");
        })).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).onEachE(e -> {
                throw new SQLException("TheXyzSQLException");
            })).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).onEachE(e -> {
                throw new SQLException("TheXyzSQLException");
            })).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }
    }

    @Test
    public void test_transform3() throws Exception {
        assertThrows(IOException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).onEachE(e -> {
            throw new IOException("TheXyzIOException");
        })).println());

        assertThrows(IOException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).onEachE(e -> {
            throw new IOException("TheXyzIOException");
        })).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).onEachE(e -> {
                throw new IOException("TheXyzIOException");
            })).println();
        } catch (final Exception e) {
            assertEquals("TheXyzIOException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).onEachE(e -> {
                throw new IOException("TheXyzIOException");
            })).println();
        } catch (final Exception e) {
            assertEquals("TheXyzIOException", e.getMessage());
        }
    }

    @Test
    public void test_cycle() throws Exception {
        {
            Seq.of("a").cycled().limit(10).println();
            Seq.of("a", "b").cycled().limit(10).println();
            Stream.of("a").cycled().limit(10).println();
            Stream.of(CommonUtil.asList("a").iterator()).cycled().limit(9).println();
            Stream.of(CommonUtil.asList("a", "b").iterator()).cycled().limit(9).println();
        }
        {
            Seq.of("a").cycled(10).limit(10).println();
            Seq.of("a", "b").cycled(10).limit(10).println();
            Stream.of("a").cycled(10).limit(10).println();
            Stream.of(CommonUtil.asList("a").iterator()).cycled(10).limit(9).println();
            Stream.of(CommonUtil.asList("a", "b").iterator()).cycled(10).limit(9).println();
        }
    }

    @Test
    public void test_flatMap() throws Exception {

        Seq.of("abc", "123").forEach(Fn.println());
    }

    @Test
    public void test_mapPartitial() throws Exception {
        Seq.range(0, 10).mapPartial(it -> it % 2 == 0 ? Optional.of(it) : Optional.empty()).println();
        Seq.range(0, 10).mapPartialToInt(it -> it % 2 == 0 ? OptionalInt.of(it) : OptionalInt.empty()).println();

    }

    @Test
    public void test_cast() {
        Seq.<String, SQLException> of("a", "b").transform(s -> s.map(String::length));
        Seq.of(CommonUtil.asList("a", "b"), SQLException.class)
                .cast()
                .onEach(it -> IOUtils.readLines(new FileInputStream(new File("./test.txt")), Charsets.DEFAULT));
    }

    @Test
    public void test_sps() throws Exception {

        Seq.<String, Exception> of("a", "b", "c").peek(Fn.println()).forEach(Fn.println());
    }

    @Test
    public void test_distinctBy() throws Exception {
        Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).dropWhile(i -> i < 4).println();
        Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).takeWhile(i -> i < 4).println();
    }

    @Test
    public void test_index() {
        Iterables.indexOf(CommonUtil.asList(1, 2, 5, 1), 1).boxed().ifPresent(Fn.println());
        Iterables.lastIndexOf(CommonUtil.asLinkedHashSet(1, 2, 5, 3), 3).boxed().ifPresent(Fn.println());
    }

}
