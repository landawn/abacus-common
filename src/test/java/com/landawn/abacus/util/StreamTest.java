/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class StreamTest extends AbstractTest {

    @Test
    public void test_groupJoin() {

        final List<String> c = CommonUtil.asList("aa", "ab", "bc");
        final List<String> b = CommonUtil.asList("aa", "ab", "cc");

        Stream.of(c).groupJoin(b, it -> it.charAt(0)).foreach(Fn.println());

        Stream.of(b).groupJoin(c, it -> it.charAt(0)).foreach(Fn.println());
    }

    @Test
    public void test_partitionTo() {
        N.println(Strings.repeat("=", 80));
        CommonUtil.emptyList().stream().collect(Collectors.partitioningBy(it -> it != null)).forEach(Fn.println("="));

        N.println(Strings.repeat("=", 80));
        CommonUtil.emptyList().stream().collect(java.util.stream.Collectors.partitioningBy(it -> it != null)).forEach(Fn.println("="));

        N.println(Strings.repeat("=", 80));
        Stream.of(CommonUtil.emptyList()).collect(Collectors.partitioningBy(it -> it != null)).forEach(Fn.println("="));

        N.println(Strings.repeat("=", 80));
        Stream.of(CommonUtil.emptyList()).collect(java.util.stream.Collectors.partitioningBy(it -> it != null)).forEach(Fn.println("="));

        N.println(Strings.repeat("=", 80));
        Stream.of(CommonUtil.emptyList()).partitionTo(it -> it != null).forEach(Fn.println("="));

        assertEquals(2, Stream.of(CommonUtil.emptyList()).partitionTo(it -> it != null).size());

        assertEquals(2, Stream.of(CommonUtil.emptyList()).partitionBy(it -> it != null).count());
    }

    @Test
    public void test_try_catch_perf() {
        final int len = 1000_000;
        final int loopNum = 100;

        Profiler.run(1, loopNum, 3, "noTryCatch", () -> {
            final long count = Stream.range(0, len).map(it -> notThrowSQLException()).count();

            assertEquals(len, count);
        }).printResult();

        Profiler.run(1, loopNum, 3, "cmdWithTryCatch", () -> {
            final long count = Stream.range(0, len).map(Fn.ff(it -> maybeThrowSQLException())).count();

            assertEquals(len, count);
        }).printResult();

        Profiler.run(1, loopNum, 3, "cmdByCheckedStream", () -> {
            try {
                final long count = Seq.<SQLException> range(0, len).map(it -> maybeThrowSQLException()).count();
                assertEquals(len, count);
            } catch (final SQLException e) {
                throw N.toRuntimeException(e);
            }
        }).printResult();

    }

    @SuppressWarnings("unused")
    String maybeThrowSQLException() throws SQLException {
        return "abc"; // Strings.uuid();
    }

    String notThrowSQLException() {
        return "abc"; // Strings.uuid();
    }

    @Test
    public void test_split_001() {
        Stream.range(0, 7).split(it -> it % 3 == 0).forEach(Fn.println());
        Stream.of("a1", "a2", "b1", "b2").split(it -> it.startsWith("a")).println();

        Stream.range(0, 7).splitAt(3).forEach(Stream::println);
        Stream.range(0, 7).splitAt(it -> it == 4).forEach(Stream::println);

        N.println(Strings.repeat("=", 80));
        Stream.of(1, 2, 3).cartesianProduct(Arrays.asList(4, 5), Arrays.asList(6, 7)).forEach(Fn.println());
        N.println(Strings.repeat("=", 80));
        Stream.of(1, 2, 3).rollup().forEach(System.out::println);
        N.println(Strings.repeat("=", 80));
        Stream.of("apple", "apple", "apple", "banana", "banana", "cherry").intersection(String::length, Arrays.asList(5, 6, 5)).forEach(System.out::println);
        N.println(Strings.repeat("=", 80));
        Stream.of("apple", "apple", "apple", "banana", "banana", "cherry").difference(String::length, Arrays.asList(5, 6, 5)).forEach(System.out::println);
    }

    @Test
    public void test_onClose_001() {
        final MutableBoolean b = MutableBoolean.of(false);

        try (Stream<Integer> s = Stream.of(1, 2, 3).onClose(() -> b.setTrue())) {
            s.foreach(Fn.println());
        }

        assertTrue(b.value());
    }

    @Test
    public void test_LazyEvaluation() {
        final MutableInt counter = MutableInt.of(0);
        Stream.range(0, 3).onEach(it -> counter.increment()).transform(s -> s.toListThenApply(Stream::of));
        assertEquals(3, counter.value());

        counter.setValue(0);
        Stream.range(0, 3).onEach(it -> counter.increment()).transform(s -> Stream.defer(() -> s.toListThenApply(Stream::of)));
        assertEquals(0, counter.value());

        counter.setValue(0);
        Stream.range(0, 3).onEach(it -> counter.increment()).transform(s -> Stream.defer(() -> s.toListThenApply(Stream::of))).println();
        assertEquals(3, counter.value());

        counter.setValue(0);
        Stream.range(0, 3).onEach(it -> counter.increment()).transformB(s -> s.toList().stream());
        assertEquals(3, counter.value());

        counter.setValue(0);
        Stream.range(0, 3).onEach(it -> counter.increment()).transformB(s -> s.toList().stream(), true);
        assertEquals(0, counter.value());
    }

    @Test
    public void test_transform() {
        Stream.range(0, 3).transform(s -> s.mapToInt(ToIntFunction.UNBOX)).println();
        Stream.range(0, 3).transformB(s -> s.limit(3)).println();
        IntStream.range(0, 3).transformB(s -> s.limit(3)).println();
        IntStream.range(0, 3).transform(s -> s.mapToObj(it -> it)).println();
    }

    @Test
    public void test_saveEach() {
        final File file = new File("./test.txt");
        Stream.range(0, 10).saveEach(file).count();
        assertEquals(Stream.range(0, 10).map(String::valueOf).toList(), IOUtil.readAllLines(file));
        IOUtil.deleteIfExists(file);
    }

    //    @Test
    //    public void test_rateLimited() {
    //        Stream.range(0, 20).rateLimited(10).onEach(Fn.doNothing()).timed().forEach(Fn.println());
    //    }

    //    ExecutorService newVirtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    //    ExecutorService newVirtualThreadPerTaskExecutor2 = Executors.newVirtualThreadPerTaskExecutor();
    //    ExecutorService executorService = Executors.newCachedThreadPool();
    //
    //    @Test
    //    public void test_virtual_threads() {
    //        N.println("==================");
    //        // TODO a bug? dead lock
    //        Stream.range(1, 1000).parallel(64, newVirtualThreadPerTaskExecutor).onEach(it -> {
    //            N.println(Thread.currentThread().getName());
    //            Fn.sleep(10);
    //            N.println("-----------: " + it);
    //        }).parallel(128, newVirtualThreadPerTaskExecutor2).onEach(it -> {
    //            N.println(Thread.currentThread().getName());
    //            Fn.sleep(32);
    //            N.println("==================: " + it);
    //        }).count();
    //
    //        N.sleep(3000);
    //    }

    //    @Test
    //    public void test_parallel_with_virtual_thread_0() throws Exception {
    //        {
    //            long startTime = System.currentTimeMillis();
    //
    //            Stream.range(1, 1000).parallel(128, 1).onEach(Fn.sleep(10)).count();
    //            N.println("====================== 1: " + (System.currentTimeMillis() - startTime));
    //
    //            Stream.range(1, 1000).parallel(128, 1).onEach(Fn.sleep(10)).parallel(128, 1).onEach(Fn.sleep(10)).count();
    //            N.println("====================== 2: " + (System.currentTimeMillis() - startTime));
    //
    //            //   // TODO a bug? dead lock
    //            //    ExecutorService newVirtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    //            //    ExecutorService newVirtualThreadPerTaskExecutor2 = Executors.newVirtualThreadPerTaskExecutor();
    //            //    Stream.range(1, 1000)
    //            //            .parallel(128, newVirtualThreadPerTaskExecutor)
    //            //            .onEach(Fn.sleep(10))
    //            //            .parallel(128, newVirtualThreadPerTaskExecutor2)
    //            //            .onEach(Fn.sleep(10))
    //            //            .count();
    //
    //            N.println("====================== 3: " + (System.currentTimeMillis() - startTime));
    //
    //            Stream.range(1, 10000)
    //                    .parallel(6)
    //                    .mapToInt(it -> it * 1 - 100 + 100)
    //                    .parallel(64, 1)
    //                    .mapToObj(it -> it * 1 - 100 + 100)
    //                    .parallel(1024, 1)
    //                    .filter(it -> it > 0)
    //                    .onEach(Fn.sleep(10))
    //                    .parallel(1024, 1)
    //                    .filter(it -> it > 0)
    //                    .count();
    //
    //            N.println("====================== 4: " + (System.currentTimeMillis() - startTime));
    //        }
    //
    //        for (int i = 0; i < 1; i++) {
    //            int k = i;
    //            final long startTime = System.currentTimeMillis();
    //            final Holder<Integer> holder = new Holder<>();
    //            try {
    //                long ret = Stream.range(1, 10000)
    //                        .parallel(6)
    //                        .mapToInt(it -> it * 1 - 100 + 100)
    //                        .parallel(64, 1)
    //                        .mapToObj(it -> it * 1 - 100 + 100)
    //                        .parallel(1024, 1)
    //                        .filter(it -> it > 0)
    //                        .onEach(Fn.sleep(10))
    //                        .parallel(512, 3)
    //                        .filter(it -> it > 0)
    //                        .onEach(Fn.sleep(10))
    //                        .mapToInt(it -> {
    //                            if (k > 0 && k % 3 == 0 && it % (k * 7) == 0) {
    //                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
    //                                holder.setValue(it);
    //                                throw new UncheckedException("Error happening", new IOException());
    //                            }
    //
    //                            return it * 1 - 100 + 100;
    //                        })
    //                        .sum();
    //
    //                N.println(ret);
    //
    //                assertEquals(49995000, ret);
    //            } catch (UncheckedException e) {
    //                assertTrue(k > 0 && k % 3 == 0 && holder.value() % (k * 7) == 0);
    //            }
    //        }
    //    }
    @Test
    public void test_parallel_with_virtual_thread_1() throws Exception {

        //        for (int i = 0; i < 20; i++) {
        //            int k = i;
        //            final long startTime = System.currentTimeMillis();
        //            final Holder<Integer> holder = new Holder<>();
        //
        //            try {
        //                long ret = Stream.range(1, 10000)
        //                        .parallel(6)
        //                        .mapToInt(it -> it * 1 - 100 + 100)
        //                        .filter(it -> it > 0)
        //                        .mapToObj(it -> it * 1 - 100 + 100)
        //                        .parallel(64)
        //                        .onEach(Fn.sleep(1))
        //                        .parallel(16)
        //                        .flattmap(N::asArray)
        //                        .parallel(32, 1)
        //                        .mapToInt(it -> it * 1 - 100 + 100)
        //                        .parallel(16, true)
        //                        .filter(it -> it > 0)
        //                        .parallel(64, 1)
        //                        .mapToObj(it -> it * 1 - 100 + 100)
        //                        .parallel(128, 1)
        //                        .filter(it -> it > 0)
        //                        .onEach(Fn.sleep(3))
        //                        .flattmap(N::asArray)
        //                        .parallel(6)
        //                        .mapToInt(it -> it * 1 - 100 + 100)
        //                        .sequential()
        //                        .filter(it -> it > 0)
        //                        .mapToObj(it -> it * 1 - 100 + 100)
        //                        .parallel(1000, 1)
        //                        .filter(it -> it > 0)
        //                        .onEach(Fn.sleep(10))
        //                        .parallel(1024, 1)
        //                        .filter(it -> it > 0)
        //                        .onEach(Fn.sleep(10))
        //                        .parallel()
        //                        .flattmap(N::asArray)
        //                        .onClose(() -> {
        //                            if (k % 2 == 0) {
        //                                closeExecutor("###", k, startTime, null);
        //                            }
        //                        })
        //                        .mapToInt(it -> {
        //                            if (k > 0 && k % 3 == 0 && it % (k * 7) == 0) {
        //                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
        //                                holder.setValue(it);
        //                                throw new UncheckedException("Error happening", new IOException());
        //                            }
        //
        //                            return it * 1 - 100 + 100;
        //                        })
        //                        .sum();
        //
        //                N.println(ret);
        //
        //                assertEquals(49995000L, ret);
        //            } catch (UncheckedException e) {
        //                assertTrue(k > 0 && k % 3 == 0 && holder.value() % (k * 7) == 0);
        //            } finally {
        //                if (k % 2 != 0) {
        //                    closeExecutor("===", k, startTime, null);
        //                }
        //            }
        //        }
    }

    @Test
    public void test_parallel_with_many_threads_0() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(512);
        final ExecutorService executor1 = Executors.newFixedThreadPool(128);

        for (int i = 0; i < 3; i++) {

            final int k = i;
            final long startTime = System.currentTimeMillis();
            final Holder<Integer> holder = new Holder<>();

            try {
                final long ret = Stream.range(1, 10000)
                        .parallel(6)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(64)
                        .onEach(Fn.sleep(1))
                        .parallel(16)
                        .flattmap(N::asArray)
                        .parallel(32, executor1)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .parallel(16, executor1)
                        .filter(it -> it > 0)
                        .parallel(64, executor1)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(128, executor)
                        .filter(it -> it > 0)
                        .onEach(Fn.sleep(3))
                        .flattmap(N::asArray)
                        .parallel(6)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .sequential()
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel()
                        .flattmap(N::asArray)
                        .onClose(() -> {
                            if (k % 2 == 0) {
                                closeExecutor("###", k, startTime, null);
                            }
                        })
                        .mapToInt(it -> {
                            if (k > 0 && k % 3 == 0 && it % (k * 7) == 0) {
                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
                                holder.setValue(it);
                                throw new UncheckedException("Error happening", new IOException());
                            }

                            return it * 1 - 100 + 100;
                        })
                        .sum();

                N.println(ret);

                assertEquals(49995000L, ret);
            } catch (final UncheckedException e) {
                assertTrue(k > 0 && k % 3 == 0 && holder.value() % (k * 7) == 0);
            } finally {
                if (k % 2 != 0) {
                    closeExecutor("===", k, startTime, null);
                }
            }
        }

        close(executor, executor1);
    }

    @Test
    public void test_parallel_with_many_threads_1() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(1024);

        for (int i = 0; i < 3; i++) {

            final int k = i;
            final long startTime = System.currentTimeMillis();
            final Holder<Integer> holder = new Holder<>();
            final ExecutorService executor1 = Executors.newFixedThreadPool(128);

            try {
                final long ret = Stream.range(1, 10000)
                        .parallel(6)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(64)
                        .onEach(Fn.sleep(1))
                        .parallel(16)
                        .flattmap(N::asArray)
                        .parallel(32, executor1)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .parallel(16, executor1)
                        .filter(it -> it > 0)
                        .parallel(64, executor1)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(256, executor)
                        .filter(it -> it > 0)
                        .onEach(Fn.sleep(3))
                        .flattmap(N::asArray)
                        .parallel(12)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .sequential()
                        .filter(it -> it > 0)
                        .parallel()
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .flattmap(N::asArray)
                        .onClose(() -> {
                            if (k % 2 == 0) {
                                closeExecutor("###", k, startTime, executor1);
                            }
                        })
                        .mapToInt(it -> {
                            if (k > 0 && k % 3 == 0 && it % (k * 7) == 0) {
                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
                                holder.setValue(it);
                                throw new UncheckedException("Error happening", new IOException());
                            }

                            return it * 1 - 100 + 100;
                        })
                        .sum();

                N.println(ret);

                assertEquals(49995000L, ret);
            } catch (final UncheckedException e) {
                assertTrue(k > 0 && k % 3 == 0 && holder.value() % (k * 7) == 0);
            } finally {
                if (k % 2 != 0) {
                    closeExecutor("===", k, startTime, executor1);
                }
            }
        }

        close(executor, null);
    }

    @Test
    public void test_parallel_with_many_threads_2() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(512);
        final ExecutorService executor1 = Executors.newFixedThreadPool(128);

        for (int i = 0; i < 3; i++) {

            final int k = i;
            final long startTime = System.currentTimeMillis();
            final Holder<Integer> holder = new Holder<>();

            try {
                final long ret = Stream.range(1, 10000)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .parallel()
                        .filter(it -> it > 0)
                        .sequential()
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(64)
                        .onEach(Fn.sleep(1))
                        .flattmap(N::asArray)
                        .parallel(16)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .flattmap(N::asArray)
                        .parallel(32, executor1)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .parallel(16, executor1)
                        .filter(it -> it > 0)
                        .parallel(64, executor1)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(128, executor)
                        .filter(it -> it > 0)
                        .onEach(Fn.sleep(3))
                        .flattmap(N::asArray)
                        .parallel(16)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .sequential()
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(64)
                        .onEach(Fn.sleep(1))
                        .flattmap(N::asArray)
                        .onClose(() -> {
                            if (k % 2 == 0) {
                                closeExecutor("###", k, startTime, null);
                            }
                        })
                        .mapToInt(it -> {
                            if (k > 0 && k % 7 == 0 && it % (k * 7) == 0) {
                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
                                holder.setValue(it);
                                throw new UncheckedException("Error happening", new IOException());
                            }

                            return it * 1 - 100 + 100;
                        })
                        .sum();

                N.println(ret);

                assertEquals(49995000L, ret);
            } catch (final UncheckedException e) {
                assertTrue(k > 0 && k % 7 == 0 && holder.value() % (k * 7) == 0);
            } finally {
                if (k % 2 != 0) {
                    closeExecutor("===", k, startTime, null);
                }
            }
        }

        close(executor, executor1);
    }

    @Test
    public void test_parallel_with_many_threads_3() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(1024);

        for (int i = 0; i < 3; i++) {

            final int k = i;
            final long startTime = System.currentTimeMillis();
            final Holder<Integer> holder = new Holder<>();
            final ExecutorService executor1 = Executors.newFixedThreadPool(128);

            try {
                final long ret = Stream.range(1, 10000)
                        .parallel()
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .filter(it -> it > 0)
                        .sequential()
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(128)
                        .onEach(Fn.sleep(1))
                        .flattmap(N::asArray)
                        .parallel(16)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .flattmap(N::asArray)
                        .parallel(32, executor1)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .parallel(16, executor1)
                        .filter(it -> it > 0)
                        .parallel(64, executor1)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(256, executor)
                        .filter(it -> it > 0)
                        .onEach(Fn.sleep(3))
                        .flattmap(N::asArray)
                        .parallel(16)
                        .mapToInt(it -> it * 1 - 100 + 100)
                        .sequential()
                        .filter(it -> it > 0)
                        .mapToObj(it -> it * 1 - 100 + 100)
                        .parallel(64)
                        .onEach(Fn.sleep(1))
                        .flattmap(N::asArray)
                        .onClose(() -> {
                            if (k % 2 == 0) {
                                closeExecutor("###", k, startTime, executor1);
                            }
                        })
                        .mapToInt(it -> {
                            if (k > 0 && k % 7 == 0 && it % (k * 7) == 0) {
                                N.println("Error happening at...: " + k + ", it = " + it + ", t = " + (System.currentTimeMillis() - startTime) + "ms");
                                holder.setValue(it);
                                throw new UncheckedException("Error happening", new IOException());
                            }

                            return it * 1 - 100 + 100;
                        })
                        .sum();

                N.println(ret);

                assertEquals(49995000L, ret);
            } catch (final UncheckedException e) {
                assertTrue(k > 0 && k % 7 == 0 && holder.value() % (k * 7) == 0);
            } finally {
                if (k % 2 != 0) {
                    closeExecutor("===", k, startTime, executor1);
                }
            }
        }

        close(executor, null);
    }

    private void shutdown(final ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void closeExecutor(final String prefix, final int k, final long startTime, final ExecutorService executor1) {
        final long now = System.currentTimeMillis();

        shutdown(executor1);

        N.println(prefix + " Closing executor1...: " + k + ", loop-time = " + (now - startTime) + "ms, executor-shutdown-time = "
                + (System.currentTimeMillis() - now));
    }

    private void close(final ExecutorService executor, final ExecutorService executor1) {
        N.println("Active thread count before shutdown: " + Thread.activeCount());

        shutdown(executor);
        shutdown(executor1);
        Runtime.getRuntime().gc();

        N.sleep(3000);

        N.println("Active thread count after shutdown: " + Thread.activeCount());
    }

    @Test
    public void test_skipRange() {
        List<Integer> ret = Stream.range(0, 7).skipRange(0, 3).toList();
        assertEquals(ret, CommonUtil.asList(3, 4, 5, 6));

        ret = Stream.range(0, 7).skipRange(5, 5).toList();
        assertEquals(ret, CommonUtil.asList(0, 1, 2, 3, 4, 5, 6));

        ret = Stream.range(0, 7).skipRange(3, 6).toList();
        assertEquals(ret, CommonUtil.asList(0, 1, 2, 6));
    }

    @Test
    public void test_toCsv() {
        Stream.of(1, 2);
    }

    //    @Test
    //    public void test_onError() throws Exception {
    //        Stream.range(0, 10).map(it -> {
    //            if (it % 2 == 0) {
    //                return it;
    //            } else {
    //                throw new RuntimeException("" + it);
    //            }
    //        }).onErrorContinue(N::println).forEach(Fn.println());
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        Stream.range(0, 10).map(it -> {
    //            if (it % 2 == 0) {
    //                return it;
    //            } else {
    //                throw new RuntimeException("" + it);
    //            }
    //        }).onErrorReturn(10000).forEach(Fn.println());
    //
    //        N.println(StringUtil.repeat("=", 80));
    //
    //        try {
    //
    //            Stream.range(0, 10).parallel().map(it -> {
    //                if (it % 2 == 0) {
    //                    return it;
    //                } else {
    //                    throw new RuntimeException("" + it);
    //                }
    //            }).onErrorContinue(N::println).forEach(Fn.println());
    //
    //            fail("Should throw IllegalStateException");
    //        } catch (IllegalStateException e) {
    //            // ignore
    //        }
    //    }
    @Test
    public void test_onClose() throws Exception {
        java.util.stream.Stream.of(1, 2, 3).onClose(() -> N.println("Closing1")).forEach(Fn.println());

        Stream.of(4, 5, 6).onClose(() -> N.println("Closing2")).forEach(Fn.println());

        try {
            Stream.of(1, 2, 3).map(it -> {
                throw new RuntimeException();
            }).onClose(() -> N.println("Closing3")).forEach(Fn.println());

            fail("Should throw RuntimeException");
        } catch (final RuntimeException e) {
            // ignore
        }

    }

    @Test
    public void test_onClose2() throws Exception {
        Stream.of("a", "b")
                .onClose(() -> System.out.println("Stream processing started"))
                .onClose(() -> System.out.println("Stream processing finished"))
                .map(String::toUpperCase)
                .forEach(System.out::println);

    }

    @Test
    public void test_reduceUntil() throws Exception {
        Stream.range(0, 100).reduce(Fn.max()).ifPresent(Fn.println());
        Stream.range(0, 100).reduceUntil(Fn.max(), it -> it > 6).ifPresent(Fn.println());

        Stream.range(0, 100).parallel().reduce(Fn.max()).ifPresent(Fn.println());
        Stream.range(0, 100).parallel(20).reduceUntil(Fn.max(), it -> it > 6).ifPresent(Fn.println());

        Stream.range(0, 100).parallel(20).reduceUntil((t, u) -> {
            N.sleep(10);
            return N.max(t, u);
        }, it -> it > 6).ifPresent(Fn.println());
    }

    @Test
    public void test_joinByRange() throws Exception {

        Stream.range(0, 100).step(3).joinByRange(Stream.range(0, 100), (t, u) -> u < t).forEach(Fn.println());

        final Stream<Integer> s = Stream.range(0, 100);
        Stream.range(0, 100)
                .step(3)
                .joinByRange(s, (t, u) -> u < t, Collectors.mapping(String::valueOf, Collectors.joining(", ", "{", "}")), Fn.pair(),
                        iter -> Stream.of(Pair.of(0, Strings.join(iter))))
                .forEach(Fn.println());
    }

    @Test
    public void test_cycled_02() throws Exception {
        IntStream.range(1, 5000).boxed().cycled(3).limit(10000).takeLast(100).println();

        assertEquals(0, IntStream.range(0, 5000).boxed().cycled(0).count());
        assertEquals(5000, IntStream.range(0, 5000).boxed().cycled(1).count());
        assertEquals(10000, IntStream.range(0, 5000).boxed().cycled(2).count());
        assertEquals(15000, IntStream.range(0, 5000).boxed().cycled(3).count());
    }

    @Test
    public void test_writeCSV() throws Exception {
        final File file = new File("./stream.csv");

        {

            final List<Object> list = CommonUtil.asList(CommonUtil.asLinkedHashMap("k1", "v11\"'\"\\\"", "k2", 12, "k3", Dates.currentDate()),
                    CommonUtil.asMap("k1", "v21", "k2", CommonUtil.asLinkedHashMap("k1", "v11\"'\"\\\"", "k2", 12, "k3", Dates.currentDate()), "k3", Dates.currentDate()));

            final Dataset dataset = CommonUtil.newDataset(list);
            dataset.println();

            N.println(dataset.toCsv());

            dataset.toCsv(file);

            IOUtil.readAllLines(file).forEach(Fn.println());
            CSVUtil.loadCSV(file).println();

            N.println(Strings.repeat('-', 60));

            Stream.of(list).persistToCSV(file);

            IOUtil.readAllLines(file).forEach(Fn.println());
            CSVUtil.loadCSV(file).println();

            Stream.of(list).persistToCSV(CommonUtil.asList("k1", "k3", "k2"), file);

            IOUtil.readAllLines(file).forEach(Fn.println());

            Stream.of(list).persist(file);

            IOUtil.readAllLines(file).forEach(Fn.println());
        }

        N.println(Strings.repeat("=", 80));

        {
            CSVUtil.setEscapeCharToBackSlashForWrite();

            final List<Object> list = CommonUtil.asList(CommonUtil.asLinkedHashMap("k1", "v11\"'\"\\\"", "k2", 12, "k3", Dates.currentDate()),
                    CommonUtil.asMap("k1", "v21", "k2", CommonUtil.asLinkedHashMap("k1", "v11\"'\"\\\"", "k2", 12, "k3", Dates.currentDate()), "k3", Dates.currentDate()));

            final Dataset dataset = CommonUtil.newDataset(list);
            dataset.println();

            N.println(dataset.toCsv());

            dataset.toCsv(file);

            IOUtil.readAllLines(file).forEach(Fn.println());
            CSVUtil.loadCSV(file).println();

            N.println(Strings.repeat('-', 60));

            Stream.of(list).persistToCSV(file);

            IOUtil.readAllLines(file).forEach(Fn.println());
            CSVUtil.loadCSV(file).println();

            Stream.of(list).persistToCSV(CommonUtil.asList("k1", "k3", "k2"), file);

            IOUtil.readAllLines(file).forEach(Fn.println());

            Stream.of(list).persist(file);

            IOUtil.readAllLines(file).forEach(Fn.println());

            CSVUtil.resetHeaderParser();
        }

        N.println(Strings.repeat("=", 80));

        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_throwIfEmpty() throws Exception {
        try {
            Stream.of("a", "b")
                    .filter(it -> it.equals("c"))
                    .onClose(() -> N.println("###: Closing stream"))
                    .throwIfEmpty(NoSuchElementException::new)
                    .first()
                    .orElseNull();
            fail("Should throw NoSuchElementException");
        } catch (final NoSuchElementException e) {
            e.printStackTrace();
        }

        try {
            Stream.of("a", "b").filter(it -> it.equals("c")).throwIfEmpty(RuntimeException::new).skip(1).count();
            fail("Should throw RuntimeException");
        } catch (final RuntimeException e) {
            e.printStackTrace();
        }

        try {
            Seq.of("a", "b").filter(it -> it.equals("c")).throwIfEmpty(NoSuchElementException::new).first().orElseNull();
            fail("Should throw NoSuchElementException");
        } catch (final NoSuchElementException e) {
            e.printStackTrace();
        }

        try {
            Seq.of("a", "b").filter(it -> it.equals("c")).throwIfEmpty(RuntimeException::new).skip(1).count();
            fail("Should throw RuntimeException");
        } catch (final RuntimeException e) {
            e.printStackTrace();
        }
    }

    //    @Test
    //    public void test_countAnd() throws Exception {
    //        Stream.of(Array.range(1, 10)).countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //        Stream.range(1, 10).countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //
    //        Stream.of(Array.range(1, 10)).parallel().countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //        Stream.range(1, 10).parallel().countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //
    //        Stream.of(Array.range(1, 1)).countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //        Stream.range(1, 1).countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //
    //        Stream.of(Array.range(1, 1)).parallel().countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //        Stream.range(1, 1).parallel().countAnd(s -> s.sumInt(ToIntFunction.UNBOX)).accept(Fn.println());
    //
    //        Stream.of(Array.range(1, 10)).countAnd(Fn.c2f(s -> s.forEach(Fn.println()))).accept(Fn.println());
    //        Stream.range(1, 10).countAnd(Fn.c2f(s -> s.forEach(Fn.println()))).accept(Fn.println());
    //    }
    @Test
    public void test_min_max_all() throws Exception {
        assertEquals(CommonUtil.asList(10, 10), Stream.of(1, 10, 9, 10, 2, 1).maxAll(Comparators.naturalOrder()));
        assertEquals(CommonUtil.asList(10, 10), Stream.of(1, 10, 9, 10, 2, 1).sorted().maxAll(Comparators.naturalOrder()));
        assertEquals(CommonUtil.asList(1, 1), Stream.of(1, 10, 9, 10, 2, 1).minAll(Comparators.naturalOrder()));
        assertEquals(CommonUtil.asList(1, 1), Stream.of(1, 10, 9, 10, 2, 1).sorted().minAll(Comparators.naturalOrder()));
    }

    @Test
    public void test_Fn_notEmptyOrNull() throws Exception {
        Stream.just(CommonUtil.asArray(Dates.currentCalendar())).filter(Fn.notEmptyA()).forEach(Fn.println());
        Stream.just(CommonUtil.asList(Dates.currentDate())).filter(Fn.notEmptyC()).forEach(Fn.println());
        Stream.just(CommonUtil.asMap("key", Dates.currentDate())).filter(Fn.notEmptyM()).forEach(Fn.println());

        Stream.just(CommonUtil.asArray(Dates.currentCalendar())).filter(Fn.isEmptyA()).forEach(Fn.println());
        Stream.just(CommonUtil.asList(Dates.currentDate())).filter(Fn.isEmptyC()).forEach(Fn.println());
        Stream.just(CommonUtil.asMap("key", Dates.currentDate())).filter(Fn.isEmptyM()).forEach(Fn.println());

        N.println(Strings.repeat('=', 80));

        Seq.just(CommonUtil.asArray(Dates.currentCalendar())).filter(Fnn.notEmptyA()).forEach(Fn.println());
        Seq.just(CommonUtil.asList(Dates.currentDate())).filter(Fnn.notEmptyC()).forEach(Fn.println());
        Seq.just(CommonUtil.asMap("key", Dates.currentDate())).filter(Fnn.notEmptyM()).forEach(Fn.println());

        Seq.just(CommonUtil.asArray(Dates.currentCalendar())).filter(Fnn.isEmptyA()).forEach(Fn.println());
        Seq.just(CommonUtil.asList(Dates.currentDate())).filter(Fnn.isEmptyC()).forEach(Fn.println());
        Seq.just(CommonUtil.asMap("key", Dates.currentDate())).filter(Fnn.isEmptyM()).forEach(Fn.println());
    }

    @Test
    public void test_mapMulti() throws Exception {
        Stream.range(1, 3).<String> mapMulti((it, c) -> {
            for (int i = it; i < it * 2; i++) {
                c.accept(it + "...");
            }
        }).map(Fn.identity()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));

        Stream.range(1, 6).<String> mapMulti((it, c) -> {
            for (int i = it; i < it * 2; i++) {
                c.accept(it + "...");
            }
        }).map(Fn.identity()).forEach(Fn.println());

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).mapMulti((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it + "...");
                }
            }).map(Fn.identity()).count());
        }

        N.println(Strings.repeat('=', 80));

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).parallel(i % 6 + 1).mapMulti((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it + "...");
                }
            }).map(Fn.identity()).count());
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).mapMultiToInt((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        N.println(Strings.repeat('=', 80));

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).parallel(i % 6 + 1).mapMultiToInt((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).mapMultiToLong((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        N.println(Strings.repeat('=', 80));

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).parallel(i % 6 + 1).mapMultiToLong((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).mapMultiToDouble((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        N.println(Strings.repeat('=', 80));

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), Stream.range(0, i).map(Fn.identity()).parallel(i % 6 + 1).mapMultiToDouble((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), IntStream.range(0, i).map(it -> it).mapMulti((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }

        N.println(Strings.repeat('=', 80));

        for (int i = 0; i < 10; i++) {
            assertEquals(IntStream.range(0, i).sum(), IntStream.range(0, i).map(it -> it).parallel(i % 6 + 1).mapMulti((it, c) -> {
                for (int j = 0; j < it; j++) {
                    c.accept(it);
                }
            }).map(it -> it).count());
        }
    }

    @Test
    public void test_performance_comparasion() throws Exception {
        long startTime = System.currentTimeMillis();
        CommonUtil.toList(Array.range(1, 1000)).stream().parallel().map(Fn.identity()).peek(Fn.sleep(10)).filter(Fn.alwaysTrue()).count();
        N.println("java stream took: " + (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        CommonUtil.toList(Array.range(1, 1000)).stream().map(Fn.identity()).parallel().peek(Fn.sleep(10)).filter(Fn.alwaysTrue()).count();
        N.println("java stream took: " + (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        Stream.of(CommonUtil.toList(Array.range(1, 1000))).parallel(64).map(Fn.identity()).onEach(Fn.sleep(10)).filter(Fn.alwaysTrue()).count();
        N.println("abacus stream took: " + (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        Stream.of(CommonUtil.toList(Array.range(1, 1000))).map(Fn.identity()).sps(64, ss -> ss.onEach(Fn.sleep(10))).filter(Fn.alwaysTrue()).count();
        N.println("abacus stream took: " + (System.currentTimeMillis() - startTime));

        //        startTime = System.currentTimeMillis();
        //        Stream.of(N.toList(Array.range(1, 1000))).map(Fn.identity()).sps(PS.create(64), ss -> ss.onEach(Fn.sleep(10))).filter(Fn.alwaysTrue()).count();
        //        N.println("abacus stream took: " + (System.currentTimeMillis() - startTime));

        //        startTime = System.currentTimeMillis();
        //        Stream.of(N.toList(Array.range(1, 1000))).map(Fn.identity()).sps(64, 16, ss -> ss.onEach(Fn.sleep(10))).filter(Fn.alwaysTrue()).count();
        //        N.println("abacus stream took: " + (System.currentTimeMillis() - startTime));
        //
        //        startTime = System.currentTimeMillis();
        //        Stream.of(N.toList(Array.range(1, 1000))).map(Fn.identity()).sps(64, 8, ss -> ss.onEach(Fn.sleep(10))).filter(Fn.alwaysTrue()).count();
        //        N.println("abacus stream took: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void test_parallel_2() throws Exception {
        final Throwables.Runnable<RuntimeException> cmd = () -> {
            final long sum = Stream.range(0, 10_000)
                    .parallel(128)
                    .map(it -> it - 0)
                    .map(it -> it + 0)
                    .flatMap(Stream::of)
                    .map(it -> it + 1)
                    .map(it -> it - 1)
                    .filter(it -> it >= 0)
                    .buffered(1024)
                    .onEach(it -> {
                        if (it % 37 == 0) {
                            N.sleep(3);
                        }
                    })
                    .map(it -> it - 0)
                    .map(it -> it + 0)
                    .flatMap(Stream::of)
                    .map(it -> it + 1)
                    .sequential()
                    .map(it -> it - 1)
                    .filter(it -> it >= 0)
                    .buffered(1024)
                    .reverseSorted()
                    .onEach(it -> {
                        if (it % 79 == 0) {
                            N.sleep(3);
                        }
                    })
                    .map(it -> it - 0)
                    .map(it -> it + 0)
                    .flatMap(Stream::of)
                    .map(it -> it + 1)
                    .map(it -> it - 1)
                    .filter(it -> it >= 0)
                    .buffered(1024)
                    .onEach(it -> {
                        if (it % 137 == 0) {
                            N.sleep(3);
                        }
                    })
                    .map(it -> it - 0)
                    .parallel(64)
                    .map(it -> it + 0)
                    .flatMap(Stream::of)
                    .map(it -> it + 1)
                    .map(it -> it - 1)
                    .filter(it -> it >= 0)
                    .buffered(137)
                    .onEach(it -> {
                        if (it % 537 == 0) {
                            N.sleep(3);
                        }
                    })
                    .mapToInt(ToIntFunction.UNBOX)
                    .sum();

            N.println(sum);

            assertEquals(sum, IntStream.range(0, 10_000).sum());
        };

        cmd.run();

        //    for (int i = 0; i < 30; i++) {
        //        cmd.run();
        //    }

        // Profiler.run(16, 3, 2, cmd);
    }
    @Test
    public void test_maxAll() throws Exception {
        N.println(Stream.of(1, 3, 2, 9, 2, 9, 1, 4).collect(Collectors.maxAll()));
        N.println(Stream.of(1, 3, 2, 9, 2, 9, 1, 4).collect(Collectors.minAll()));
    }

    @Test
    public void test_BigIntegerSummaryStatistics() throws Exception {
        BigIntegerSummaryStatistics ret = Stream.of(BigInteger.valueOf(11), BigInteger.valueOf(17), BigInteger.valueOf(32))
                .collect(Collectors.summarizingBigInteger(Fn.identity()));

        N.println(ret);

        ret = Stream.<BigInteger> empty().collect(Collectors.summarizingBigInteger(Fn.identity()));

        N.println(ret);

        BigDecimalSummaryStatistics ret2 = Stream.of(BigDecimal.valueOf(11.11), BigDecimal.valueOf(17.171234324), BigDecimal.valueOf(32.3232341))
                .collect(Collectors.summarizingBigDecimal(Fn.identity()));

        N.println(ret2);

        ret2 = Stream.<BigDecimal> empty().collect(Collectors.summarizingBigDecimal(Fn.identity()));

        N.println(ret2);
    }

    @Test
    public void test_exception() throws Exception {

        try {
            Stream.range(0, 100).shuffled().forEach(it -> {
                if (it % 11 == 0) {
                    N.println("throwing IOException");
                    throw new IOException();
                } else if (it % 17 == 0) {
                    N.println("throwing SQLException");
                    throw new SQLException();
                }
            });

            fail("Should throw IOException or SQLException");
        } catch (IOException | SQLException e) {

        }

        try {
            Stream.range(0, 100).shuffled().parallel(16).forEach(it -> {
                if (it % 11 == 0) {
                    N.println("throwing IOException");
                    throw new IOException();
                } else if (it % 17 == 0) {
                    N.println("throwing SQLException");
                    throw new SQLException();
                }
            });

            fail("Should throw IOException or SQLException");
        } catch (IOException | SQLException e) {

        }

        try {
            Stream.range(0, 100).shuffled().parallel(16).forEach(it -> {
                if (it % 11 == 0) {
                    N.sleep(3000);
                    N.println("throwing IOException");
                    throw new IOException();
                } else if (it % 17 == 0) {
                    N.sleep(3000);
                    N.println("throwing SQLException");
                    throw new SQLException();
                }
            });

            fail("Should throw IOException or SQLException");
        } catch (IOException | SQLException e) {

        }
    }

    @Test
    public void test_forEachUntil() {
        Stream.range(1, 9).map(i -> i + 1).filter(i -> i % 2 == 0 || (i + 1) % 2 == 0).peek(Fn.println()).forEachUntil((it, flagToBreak) -> {
            if (it > 5) {
                flagToBreak.setTrue();
                return;
            }

            N.println("a: " + it);
        });

        N.println(Strings.repeat('=', 80));

        Stream.range(1, 9).map(i -> i + 1).filter(i -> i % 2 == 0 || (i + 1) % 2 == 0).peek(Fn.println()).forEachUntil((it, flagToBreak) -> {
            N.println("a: " + it);

            if (it > 5) {
                flagToBreak.setTrue();
            }
        });
    }

    @Test
    public void test_map() {
        final Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2);
        final long count = Stream.of(map).filter(it -> Strings.isNotEmpty(it.getKey())).map(Fn.value()).mapToInt(ToIntFunction.UNBOX).count();
        N.println(count);
    }

    @Test
    public void test_intersection() {
        final List<String> list = Stream.of("a", "b", "C", "d", "b", "d").intersection(CommonUtil.asList("b", "b", "d")).toList();
        N.println(list);
        assertEquals(list, CommonUtil.asList("b", "d", "b"));

        IntList a = IntList.of(0, 1, 2, 2, 3);
        IntList b = IntList.of(2, 5, 1);
        a.retainAll(b); // The elements remained in a will be: [1, 2, 2].
        N.println(a);

        a = IntList.of(0, 1, 2, 2, 3);
        b = IntList.of(2, 5, 1);
        IntList c = a.intersection(b); // The elements c in a will be: [1, 2].
        N.println(c);
        assertEquals(IntList.of(1, 2), c);

        a = IntList.of(0, 1, 2, 2, 3);
        b = IntList.of(2, 5, 1, 2);
        c = a.intersection(b); // The elements c in a will be: [1, 2, 2].
        assertEquals(IntList.of(1, 2, 2), c);

        N.println(c);

        Dataset ds1 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(1, 1), CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        Dataset ds2 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        Dataset ds3 = ds1.intersectAll(ds2);
        ds3.println();

        ds2 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds3 = ds1.intersectAll(ds2);
        ds3.println();

        N.println(Strings.repeat('=', 80));

        ds1 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(1, 1), CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds2 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds3 = ds1.intersection(ds2);
        ds3.println();

        ds2 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds3 = ds1.intersection(ds2);
        ds3.println();

        N.println(Strings.repeat('=', 80));

        ds1 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(1, 1), CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds2 = CommonUtil.newDataset(CommonUtil.asList("c1", "c2"), CommonUtil.asList(CommonUtil.asList(2, 2), CommonUtil.asList(3, 3), CommonUtil.asList(2, 2)));
        ds3 = ds1.intersect(ds2);
        ds3.println();

        ds3 = ds1.intersectAll(ds2);
        ds3.println();

    }

    @Test
    public void test_size() {
        final int size = 10_000_000;
        final List<Object> list = new ArrayList<>(size);
        final Object[] a = { 1, "a", "b", 2 };

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++) {
            list.add(java.util.stream.Stream.of(a));
        }

        N.println(list.size());

        N.println((System.currentTimeMillis() - startTime) + "ms");

        System.gc();
        list.clear();
        System.gc();

        startTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++) {
            list.add(Stream.of(a));
        }

        N.println(list.size());

        N.println((System.currentTimeMillis() - startTime) + "ms");

        System.gc();
        list.clear();
        System.gc();

        startTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++) {
            list.add(java.util.stream.Stream.of(a));
        }

        N.println(list.size());

        N.println((System.currentTimeMillis() - startTime) + "ms");

        System.gc();
        list.clear();
        System.gc();
    }

    @Test
    public void test_flatMap() {

        final byte[] a = Array.repeat((byte) 1, 10);
        N.println(a);

        Stream.of(IntStream.random(0, 10).distinct().limit(3).toList())
                .flatmapToByte(i -> Array.repeat(i.byteValue(), i.intValue()))
                .boxed()
                .countBy(Fn.identity())
                .onEach(it -> assertEquals(it.getKey().intValue(), it.getValue().intValue()))
                .forEach(N::println);

        N.println(Strings.repeat('=', 90));

        IntStream.random(0, 10)
                .distinct()
                .limit(3)
                .boxed()
                .flatmapToByte(i -> Array.repeat(i.byteValue(), i.intValue()))
                .boxed()
                .countBy(Fn.identity())
                .onEach(it -> assertEquals(it.getKey().intValue(), it.getValue().intValue()))
                .forEach(N::println);

        N.println(Strings.repeat('=', 90));

        Stream.of(IntStream.random(0, 10).distinct().limit(3).toList())
                .parallel(3)
                .flatmapToByte(i -> Array.repeat(i.byteValue(), i.intValue()))
                .boxed()
                .countBy(Fn.identity())
                .onEach(it -> assertEquals(it.getKey().intValue(), it.getValue().intValue()))
                .forEach(N::println);

        N.println(Strings.repeat('=', 90));

        IntStream.random(0, 10)
                .distinct()
                .limit(3)
                .boxed()
                .parallel(3)
                .flatmapToByte(i -> Array.repeat(i.byteValue(), i.intValue()))
                .boxed()
                .countBy(Fn.identity())
                .onEach(it -> assertEquals(it.getKey().intValue(), it.getValue().intValue()))
                .forEach(N::println);
    }


    @Test
    public void test_futureGet() {
        Stream.range(0, 10).map(it -> N.asyncExecute(() -> N.println(it))).map(Fn.futureGet()).forEach(Fn.println());

        Stream.range(0, 10).parallel().map(it -> N.asyncExecute(() -> N.println(it))).map(Fn.futureGet()).forEach(Fn.println());

    }

    @Test
    public void test_parallelConcat() {
        final List<Iterator<Integer>> iters = IntStream.range(0, 64)//
                .mapToObj(it -> CommonUtil.asList(it).iterator())
                .toList();

        N.println(Stream.parallelConcatIterators(iters).sumInt(Integer::intValue));
    }

    @Test
    public void test_rollup() {
        Iterables.rollup(CommonUtil.asList(1, 2, 3)).forEach(Fn.println());
        N.println(Strings.repeat("=", 80));
        Iterables.rollup(CommonUtil.<String> emptyList()).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));

        Stream.of(CommonUtil.<String> emptyList()).rollup().forEach(Fn.println());
        Stream.of(CommonUtil.<String> emptyList()).rollup().skip(10).forEach(Fn.println());

        N.println(Strings.repeat("=", 80));
        Stream.of(CommonUtil.asList(1, 2, 3)).rollup().forEach(Fn.println());
        Stream.of(CommonUtil.asList(1, 2, 3)).rollup().skip(2).forEach(Fn.println());
        Stream.of(CommonUtil.<String> emptyList()).rollup().skip(3).limit(1).forEach(Fn.println());
    }

    @Test
    public void test_reversed() {
        IntStream.of(1, 3, 2).reversed().println();
        IntStream.of(IntIterator.of(1, 3, 2)).reversed().println();

        Stream.of(1, 3, 2).reversed().println();
        Stream.of(ObjIterator.of(1, 3, 2)).reversed().println();

        N.println(Strings.repeat("=", 80));

        IntStream.of(1, 3, 2).rotated(0).println();
        IntStream.of(IntIterator.of(1, 3, 2)).rotated(0).println();

        Stream.of(1, 3, 2).rotated(0).println();
        Stream.of(ObjIterator.of(1, 3, 2)).rotated(0).println();

        N.println(Strings.repeat("=", 80));

        IntStream.of(1, 3, 2).rotated(1).println();
        IntStream.of(IntIterator.of(1, 3, 2)).rotated(1).println();

        Stream.of(1, 3, 2).rotated(1).println();
        Stream.of(ObjIterator.of(1, 3, 2)).rotated(1).println();

        N.println(Strings.repeat("=", 80));

        N.println(Stream.of(1, 3, 2).join(", "));
        N.println(IntStream.of(1, 3, 2).join(", ", "[", "]"));
    }

    @Test
    public void test_cycled2() {
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(0).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(1).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(2).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(3).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(4).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(5).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(6).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled().limit(7).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(0).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(1).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(2).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(3).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(4).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(0).limit(5).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(0).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(1).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(2).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(3).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(4).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(1).limit(5).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(0).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(1).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(2).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(3).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(4).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(5).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(6).println();
        Stream.of(CommonUtil.asArray(1, 2, 3)).cycled(10).limit(7).println();
    }

    @Test
    public void test_cycled() {
        Stream.of(1, 2, 3).cycled().limit(0).println();
        Stream.of(1, 2, 3).cycled().limit(1).println();
        Stream.of(1, 2, 3).cycled().limit(2).println();
        Stream.of(1, 2, 3).cycled().limit(3).println();
        Stream.of(1, 2, 3).cycled().limit(4).println();
        Stream.of(1, 2, 3).cycled().limit(5).println();
        Stream.of(1, 2, 3).cycled().limit(6).println();
        Stream.of(1, 2, 3).cycled().limit(7).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).cycled(0).limit(0).println();
        Stream.of(1, 2, 3).cycled(0).limit(1).println();
        Stream.of(1, 2, 3).cycled(0).limit(2).println();
        Stream.of(1, 2, 3).cycled(0).limit(3).println();
        Stream.of(1, 2, 3).cycled(0).limit(4).println();
        Stream.of(1, 2, 3).cycled(0).limit(5).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).cycled(1).limit(0).println();
        Stream.of(1, 2, 3).cycled(1).limit(1).println();
        Stream.of(1, 2, 3).cycled(1).limit(2).println();
        Stream.of(1, 2, 3).cycled(1).limit(3).println();
        Stream.of(1, 2, 3).cycled(1).limit(4).println();
        Stream.of(1, 2, 3).cycled(1).limit(5).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(1, 2, 3).cycled(10).limit(0).println();
        Stream.of(1, 2, 3).cycled(10).limit(1).println();
        Stream.of(1, 2, 3).cycled(10).limit(2).println();
        Stream.of(1, 2, 3).cycled(10).limit(3).println();
        Stream.of(1, 2, 3).cycled(10).limit(4).println();
        Stream.of(1, 2, 3).cycled(10).limit(5).println();
        Stream.of(1, 2, 3).cycled(10).limit(6).println();
        Stream.of(1, 2, 3).cycled(10).limit(7).println();
    }

    @Test
    public void test_acceptIfNotEmpty() {
        Stream.of(1, 2, 3).acceptIfNotEmpty(Stream::println).orElse(() -> N.println("Is empty"));

        Stream.empty().acceptIfNotEmpty(Stream::println).orElse(() -> N.println("Is empty"));
    }

    @Test
    public void test_merge() {
        final IntBiFunction<MergeResult> first = (t, u) -> MergeResult.TAKE_FIRST;

        IntStream.merge(IntStream.range(0, 10), IntStream.range(10, 20), first).println();

        Stream.merge(IntStream.range(0, 10).boxed(), IntStream.range(10, 20).boxed(), Fn.alternate()).println();
    }

    @Test
    public void test_Collectors_combine() {
        N.println(Stream.of(1, 2, 3, 1, 1, 3)
                .collect(MoreCollectors.combine(Collectors.<Integer> min(), Collectors.<Integer> minAll(), Collectors.<Integer> maxAll(), Tuple::of)));
    }

    @Test
    public void test_cartesianProduct() {
        Stream.of("1").cartesianProduct(CommonUtil.asList("a", "b")).forEach(Fn.println());

        N.println("===================================================");
        Stream.of("1", "2").cartesianProduct(CommonUtil.asList("a", "b")).forEach(Fn.println());

        N.println("===================================================");
        Stream.of("1", "2", "3").cartesianProduct(CommonUtil.asList("a", "b")).forEach(Fn.println());

        N.println("===================================================");
        Stream.of("1", "2", "3").cartesianProduct(CommonUtil.asList("a")).forEach(Fn.println());
    }

    @Test
    public void test_combinations_2() {
        Stream.of(1, 2, 3).combinations().forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).combinations(2).forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).combinations(2, false).forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).combinations(2, true).forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).combinations(3, true).forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).permutations().forEach(Fn.println());
        N.println("===================================================");
        Stream.of(1, 2, 3).orderedPermutations().forEach(Fn.println());
    }

    @Test
    public void test_only() {
        Stream.of(1, 2).collect(Collectors.onlyOne(Fn.lessThan(2))).ifPresentOrElse(Fn.println(), Fn.emptyAction());
    }

    @Test
    public void test_rotate() {
        ByteStream.range((byte) 0, (byte) 2).rotated(-3).println();

        for (int i = 0; i < 23; i++) {
            for (int j = 0; j <= i; j++) {
                final byte[] a = Array.range((byte) j, (byte) i);
                N.println(a);

                for (int k = -97; k < 97; k++) {
                    final byte[] tmp = a.clone();
                    CommonUtil.rotate(tmp, k);
                    // N.println(N.toString(tmp) + " -> " + k);
                    // N.println(ByteStream.range((byte) j, (byte) i).rotated(k).toArray());

                    assertTrue(CommonUtil.equals(tmp, ByteStream.range((byte) j, (byte) i).rotated(k).toArray()));
                }
            }
        }
    }

    @Test
    public void test_rotate_2() {
        ByteStream.range((byte) 0, (byte) 2).rotated(-3).println();

        for (int i = 0; i < 23; i++) {
            for (int j = 0; j <= i; j++) {
                final byte[] a = Array.range((byte) j, (byte) i);
                N.println(a);

                for (int k = -97; k < 97; k++) {
                    final byte[] tmp = a.clone();
                    CommonUtil.rotate(tmp, k);
                    // N.println(N.toString(tmp) + " -> " + k);
                    // N.println(ByteStream.range((byte) j, (byte) i).rotated(k).toArray());

                    assertTrue(CommonUtil.equals(tmp, ByteStream.range((byte) j, (byte) i).rotated(k).toArray()));
                }
            }
        }
    }

    @Test
    public void test_toMap() {
        Map<Integer, List<Integer>> map = IntStream.range(1, 100).boxed().parallel().groupTo(Fn.<Integer> identity(), Collectors.<Integer> toList());
        N.forEach(map.entrySet(), Fn.println());
        map = IntStream.range(1, 100).boxed().parallel().groupTo(Fn.<Integer> identity(), Collectors.<Integer> toList());
        N.forEach(map.entrySet(), Fn.println());

        ListMultimap<Integer, Integer> map2 = IntStream.range(1, 100).boxed().parallel().toMultimap(Fn.<Integer> identity());
        N.forEach(map2.entrySet(), Fn.println());
        map2 = IntStream.range(1, 100).boxed().parallel().toMultimap(Fn.<Integer> identity());
        N.forEach(map2.entrySet(), Fn.println());
    }

    @Test
    public void test_013() {
        String str = Stream.of("abc2efg", "abc3efg", "ab2fg", null).collect(Collectors.commonPrefix());
        N.println(str);

        str = Stream.of("abc2efg", "ease").collect(Collectors.commonPrefix());
        N.println(str);

        str = Stream.of("abc2efg", "abc3efg", "ab2fg").collect(Collectors.commonSuffix());
        N.println(str);
    }

    @Test
    public void test_join() {
        final List<String[]> a = CommonUtil.asList(CommonUtil.asArray("a", "1"), CommonUtil.asArray("b", "2"), CommonUtil.asArray("c", "3"));
        final List<String[]> b = CommonUtil.asList(CommonUtil.asArray("3", "d"), CommonUtil.asArray("2", "e"), CommonUtil.asArray("2", "f"), CommonUtil.asArray("4", "f"));

        Stream.of(a).innerJoin(b, (BiPredicate<String[], String[]>) (t, u) -> CommonUtil.equals(t[1], u[0])).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).fullJoin(b, (BiPredicate<String[], String[]>) (t, u) -> CommonUtil.equals(t[1], u[0])).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).leftJoin(b, (BiPredicate<String[], String[]>) (t, u) -> CommonUtil.equals(t[1], u[0])).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).rightJoin(b, (BiPredicate<String[], String[]>) (t, u) -> CommonUtil.equals(t[1], u[0])).println();
    }

    @Test
    public void test_join_2() {
        final List<String[]> a = CommonUtil.asList(CommonUtil.asArray("a", "1"), CommonUtil.asArray("b", "2"), CommonUtil.asArray("c", "3"));
        final List<String[]> b = CommonUtil.asList(CommonUtil.asArray("3", "d"), CommonUtil.asArray("2", "e"), CommonUtil.asArray("2", "f"), CommonUtil.asArray("4", "f"));

        Stream.of(a).innerJoin(b, (Function<String[], String>) t -> t[1], (Function<String[], String>) t -> t[0]).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).fullJoin(b, (Function<String[], String>) t -> t[1], (Function<String[], String>) t -> t[0]).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).leftJoin(b, (Function<String[], String>) t -> t[1], (Function<String[], String>) t -> t[0]).println();

        N.println(Strings.repeat("=", 80));

        Stream.of(a).rightJoin(b, (Function<String[], String>) t -> t[1], (Function<String[], String>) t -> t[0]).println();
    }

    @Test
    public void test_combinations() {

        final int len = 6;
        IntStream.rangeClosed(1, len).boxed().combinations().forEach(N::println);
    }

    @Test
    public void test_01() {

        assertEquals(3, Stream.of(1, 2, 3).kthLargest(1, Integer::compareTo).get().intValue());

        assertEquals(2, Stream.of(1, 2, 3).kthLargest(2, Integer::compareTo).get().intValue());

        assertEquals(1, Stream.of(1, 2, 3).kthLargest(3, Integer::compareTo).get().intValue());

        IntStream.of(1, 2, 3).forEach(N::println);

        final IntBinaryOperator op = (left, right) -> left + right;

        N.println(IntStream.of(1, 2, 3).reduce(op).get());

        final Collector<String, ?, List<String>> collector = Collectors.toList();
        final List<String> list = Stream.of(CommonUtil.asArray("a", "b", "c")).collect(collector);
        N.println(list);

        final Function<? super String, ? extends String> classifier = t -> "b";

        final Collector<String, ?, Map<String, List<String>>> collector2 = Collectors.groupingBy(classifier);
        final Map<String, List<String>> map = Stream.of(CommonUtil.asArray("a", "b", "c")).collect(collector2);
        N.println(map);

        final Predicate<? super String> predicate = "b"::equals;

        final Collector<String, ?, Map<Boolean, List<String>>> collector3 = Collectors.partitioningBy(predicate);
        final Map<Boolean, List<String>> map3 = Stream.of(CommonUtil.asArray("a", "b", "c")).collect(collector3);
        N.println(map3);
    }

    @Test
    public void test_02() {
        N.println(Stream.of(1, 5, 7, 3, 2, 1).top(2).toArray());

        N.println(Stream.of(1, 5, 7, 3, 2, 1).top(6).toArray());

        N.println(Stream.of(1, 5, 7, 3, 2, 1).top(7).toArray());

        N.println(IntStream.of(1, 5, 7, 3, 2, 1).top(7).toArray());
    }

    @Test
    public void test_perf() {
        final String[] strs = new String[1_000];
        CommonUtil.fill(strs, Strings.uuid());

        final int m = 600;
        final ToLongFunction<String> mapper = str -> {
            long result = 0;
            for (int i = 0; i < m; i++) {
                result += N.sum(str.toCharArray()) + 1;
            }
            return result;
        };

        final MutableLong sum = MutableLong.of(0);

        for (final String str : strs) {
            sum.add(mapper.applyAsLong(str));
        }

        final int threadNum = 1, loopNum = 10, roundNum = 3;

        Profiler.run(threadNum, loopNum, roundNum, "For Loop", () -> {
            long result = 0;
            for (final String str : strs) {
                result += mapper.applyAsLong(str);
            }
            assertEquals(sum.value(), result);
        }).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "JDK Sequential", () -> assertEquals(sum.value(), java.util.stream.Stream.of(strs).mapToLong(mapper).sum()))
                .printResult();

        Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel",
                () -> assertEquals(sum.value(), java.util.stream.Stream.of(strs).parallel().mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel - 2",
                () -> assertEquals(sum.value(),
                        StreamSupport.stream(Spliterators.spliteratorUnknownSize(N.iterate(strs), Spliterator.ORDERED), true).mapToLong(mapper).sum()))
                .printResult();

        //    Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel - 3",
        //            () -> assertEquals(sum.value(), Stream.of(strs).toJdkStream().parallel().mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Sequential", () -> assertEquals(sum.value(), Stream.of(strs).mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel", () -> assertEquals(sum.value(), Stream.of(strs).parallel().mapToLong(mapper).sum()))
                .printResult();

        //    Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel - 2",
        //            () -> assertEquals(sum.value(), Stream.of(strs).parallel(Splitor.ARRAY).mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel by chunk",
                () -> assertEquals(sum.value(), Stream.of(strs).split(100).parallel().mapToLong(it -> N.sumLong(it, e -> mapper.applyAsLong(e))).sum()))
                .printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel by chunk - 2",
                () -> assertEquals(sum.value(), Stream.of(strs).split(100).parallel().flatMapToLong(it -> Stream.of(it).mapToLong(mapper)).sum()))
                .printResult();
    }

    @Test
    public void test_perf_2() {
        final String[] strs = new String[1_000];
        CommonUtil.fill(strs, Strings.uuid());

        final Predicate<String> filter = it -> {
            N.sleep(1);
            return true;
        };

        final int m = 600;
        final ToLongFunction<String> mapper = str -> {
            long result = 0;
            for (int i = 0; i < m; i++) {
                result += N.sum(str.toCharArray()) + 1;
            }
            return result;
        };

        final MutableLong sum = MutableLong.of(0);

        for (final String str : strs) {
            sum.add(mapper.applyAsLong(str));
        }

        final int threadNum = 1, loopNum = 3, roundNum = 1;

        Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel",
                () -> assertEquals(sum.value(), java.util.stream.Stream.of(strs).parallel().filter(filter).mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel - 2", () -> assertEquals(sum.value(),
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(N.iterate(strs), Spliterator.ORDERED), true).filter(filter).mapToLong(mapper).sum()))
                .printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel",
                () -> assertEquals(sum.value(), Stream.of(strs).parallel().filter(filter).mapToLong(mapper).sum())).printResult();

        Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel - 2",
                () -> assertEquals(sum.value(), Stream.of(strs).parallel(64).filter(filter).parallel().mapToLong(mapper).sum())).printResult();

    }

    @Test
    public void test_map_onclose() {
        final java.util.stream.Stream<Timestamp> s1 = CommonUtil.asList("a", "b")
                .stream()
                .map(it -> new Timestamp(System.currentTimeMillis()))
                .onClose(Fn.emptyAction());
        s1.forEach(Fn.println());

        final Stream<Timestamp> s2 = Stream.of("a", "b").map(it -> new Timestamp(System.currentTimeMillis())).onClose(Fn.emptyAction());
        s2.forEach(Fn.println());

        final Stream<Timestamp> s3 = Stream.zip(CommonUtil.asList("a", "b"), CommonUtil.asList(1, 2), (a, b) -> new Timestamp(System.currentTimeMillis()))
                .onClose(Fn.emptyAction());
        s3.forEach(Fn.println());
    }

}
