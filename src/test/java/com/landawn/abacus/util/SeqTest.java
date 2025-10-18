/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.Stream;

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

        //    Seq.range(0, 10).mapPartialJdk(it -> it % 2 == 0 ? java.util.Optional.of(it) : java.util.Optional.empty()).println();
        //    Seq.range(0, 10).mapPartialToLongJdk(it -> it % 2 == 0 ? java.util.OptionalLong.of(it) : java.util.OptionalLong.empty()).println();

        //        Seq.range(0, 10).flatMapByStream(it -> Stream.of(it * 2, it * 3)).println();
        //        Seq.range(0, 10).flatMapByStreamJdk(it -> java.util.stream.Stream.of(it * 2, it * 3)).println();
    }

    //    @Test
    //    public void test_appendOnError() throws Exception {
    //        Seq.of("a", "b") //
    //                .onEach(it -> {
    //                    if (it.equals("b")) {
    //                        throw new RuntimeException();
    //                    }
    //                })
    //                .onErrorReturn("c") //
    //                .onEach(it -> {
    //                    if (it.equals("c")) {
    //                        throw new RuntimeException();
    //                    }
    //                })
    //                .onErrorReturn("d")
    //                .onEach(it -> {
    //                    if (it.equals("d")) {
    //                        throw new IOException();
    //                    }
    //                })
    //                .onErrorReturn(e -> IOException.class.isAssignableFrom(e.getClass()), () -> "e")
    //                .forEach(Fn.println());
    //    }

    @Test
    public void test_cast() {
        Seq.<String, SQLException> of("a", "b").transform(s -> s.map(String::length));
        Seq.of(CommonUtil.asList("a", "b"), SQLException.class).cast().onEach(it -> IOUtils.readLines(new FileInputStream(new File("./test.txt")), Charsets.DEFAULT));
    }

    @Test
    public void test_sps() throws Exception {
        // Seq.<String, SQLException> repeat("a", 1000).spsOnEach(it -> N.sleep(10)).forEach(Fn.println());

        Seq.<String, Exception> of("a", "b", "c").peek(Fn.println()).forEach(Fn.println());
    }

    //    @Test
    //    public void test_sps_exception() throws Exception {
    //        try {
    //            Seq.of("a", "b", "c").spsFlatMapE(it -> {
    //                throw new IOException("test");
    //            }).count();
    //
    //            fail("Should throw IOException");
    //        } catch (final IOException e) {
    //            // ignore
    //        }
    //
    //        try {
    //            Seq.of("a", "b", "c").spsFlatMapE(it -> {
    //                throw new IllegalArgumentException("test");
    //            }).count();
    //
    //            fail("Should throw IllegalArgumentException");
    //        } catch (final IllegalArgumentException e) {
    //            // ignore
    //        }
    //    }

    @Test
    public void test_distinctBy() throws Exception {
        //  CheckedStream.of(1, 2, 1, 2, 3, 1, 4, 5, 5).distinctBy(e -> e, e -> e.getValue() >= 2).println();
        Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).dropWhile(i -> i < 4).println();
        Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).takeWhile(i -> i < 4).println();
    }


    @Test
    public void test_index() {
        Iterables.indexOf(CommonUtil.asList(1, 2, 5, 1), 1).boxed().ifPresent(Fn.println());
        Iterables.lastIndexOf(CommonUtil.asLinkedHashSet(1, 2, 5, 3), 3).boxed().ifPresent(Fn.println());
    }

    //    @Test
    //    public void test_repeat() {
    //        N.println(N.repeat(null, 10));
    //        N.println(N.repeat(N.asList("a"), 10));
    //        N.println(N.repeatCollection(N.asList("a"), 10));
    //        N.println(Iterators.toList(Iterators.repeatCollection(N.asList("a"), 10)));
    //        N.println(Iterators.toList(Iterators.repeat(N.asList("a"), 10)));
    //        N.println(Iterators.toList(Iterators.repeat("a", 10)));
    //    }
    //
    //    @Test
    //    public void test_rollup() {
    //        N.println(Iterables.rollup(N.asList("a", "b", "c")));
    //    }
    //
    //    public void test_Allmax() {
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.maxAll()));
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.minAll()));
    //
    //        Function<Integer, Integer> keyExtractor = Fn.identity();
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.maxAll(Comparators.comparingBy(keyExtractor))));
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.minAll(Comparators.comparingBy(keyExtractor))));
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.minMax(Comparators.naturalOrder(), Fn.pair())));
    //
    //        N.println(Seq.of(1, 1, 2, 3, 3).collect(Collectors.combine(Collectors.max(), Collectors.max(), Fn.pair())));
    //    }
    //
    //    public void test_findFirst() {
    //        List<Integer> list = N.asLinkedList(1, 2, 3, 4, 5);
    //        assertEquals(5, Seq.of(list).findLast(Fn.greaterThan(4)).get().intValue());
    //        assertEquals(4, Seq.of(list).findLastIndex(Fn.greaterThan(4)).get());
    //
    //        list = N.asLinkedList(1, 2, 5, 4, 3);
    //        assertEquals(4, Seq.of(list).findLast(Fn.greaterThan(3)).get().intValue());
    //        assertEquals(3, Seq.of(list).findLastIndex(Fn.greaterThan(3)).get());
    //    }
    //
    //    public void test_join() {
    //        List<String[]> a = N.asList(N.asArray("a", "1"), N.asArray("b", "2"), N.asArray("c", "3"));
    //        List<String[]> b = N.asList(N.asArray("3", "d"), N.asArray("2", "e"), N.asArray("2", "f"), N.asArray("4", "f"));
    //
    //        N.println(Seq.of(a).innerJoin(b, new BiPredicate<String[], String[]>() {
    //            @Override
    //            public boolean test(String[] t, String[] u) {
    //                return N.equals(t[1], u[0]);
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).fullJoin(b, new BiPredicate<String[], String[]>() {
    //            @Override
    //            public boolean test(String[] t, String[] u) {
    //                return N.equals(t[1], u[0]);
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).leftJoin(b, new BiPredicate<String[], String[]>() {
    //            @Override
    //            public boolean test(String[] t, String[] u) {
    //                return N.equals(t[1], u[0]);
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).rightJoin(b, new BiPredicate<String[], String[]>() {
    //            @Override
    //            public boolean test(String[] t, String[] u) {
    //                return N.equals(t[1], u[0]);
    //            }
    //        }));
    //    }
    //
    //    public void test_join_2() {
    //        List<String[]> a = N.asList(N.asArray("a", "1"), N.asArray("b", "2"), N.asArray("c", "3"));
    //        List<String[]> b = N.asList(N.asArray("3", "d"), N.asArray("2", "e"), N.asArray("2", "f"), N.asArray("4", "f"));
    //
    //        N.println(Seq.of(a).innerJoin(b, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[1];
    //            }
    //        }, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[0];
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).fullJoin(b, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[1];
    //            }
    //        }, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[0];
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).leftJoin(b, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[1];
    //            }
    //        }, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[0];
    //            }
    //        }));
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        N.println(Seq.of(a).rightJoin(b, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[1];
    //            }
    //        }, new Function<String[], String>() {
    //            @Override
    //            public String apply(String[] t) {
    //                return t[0];
    //            }
    //        }));
    //    }
    //
    //    public void test_cartesianProduct() {
    //        List<List<String>> res = Iterables.cartesianProduct(N.asList("a", "b", "c"), N.asList("1", "2", "3"));
    //
    //        for (List<String> e : res) {
    //            N.println(e);
    //        }
    //
    //        N.println(StringUtil.repeat('=', 80));
    //
    //        res = Stream.of("a", "b", "c").cartesianProduct(N.asList("1", "2", "3")).toList();
    //
    //        for (List<String> e : res) {
    //            N.println(e);
    //        }
    //    }
    //
    //    public void test_containsAll() {
    //        List<String> list = N.asList("a", "b", "c");
    //        assertTrue(list.containsAll(N.asList()));
    //
    //        Seq<String> seq = Seq.of(N.asList("a", "b", "c"));
    //        assertTrue(seq.containsAll(N.asList()));
    //    }
    //
    //    public void test_containsAny() {
    //        assertTrue(Seq.of(N.asList("a", "b", "c")).containsAny(N.asSet("1", "2", "a", "3")));
    //        assertFalse(Seq.of(N.asList("a", "b", "c")).containsAny(N.asSet("1", "2", "3")));
    //
    //        assertFalse(Seq.of(N.asList("a", "b", "c")).disjoint(N.asSet("1", "2", "a", "3")));
    //        assertTrue(Seq.of(N.asList("a", "b", "c")).disjoint(N.asSet("1", "2", "3")));
    //    }
}
