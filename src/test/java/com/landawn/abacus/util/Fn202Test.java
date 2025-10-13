package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.Fn.Entries;
import com.landawn.abacus.util.Fn.FI;
import com.landawn.abacus.util.Fn.FL;
import com.landawn.abacus.util.Fn.Predicates;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class Fn202Test extends TestBase {

    public static class MyCloseable implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger(0);

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        public int getCloseCount() {
            return closeCount.get();
        }
    }

    @Test
    public void testMemoizeSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Supplier<Integer> javaSupplier = () -> counter.incrementAndGet();

        Supplier<Integer> memoized = Fn.memoize(javaSupplier);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Supplier<Integer> javaSupplier = counter::incrementAndGet;

        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(javaSupplier, 0, TimeUnit.MILLISECONDS));

        Supplier<Integer> memoized = Fn.memoizeWithExpiration(javaSupplier, 100, TimeUnit.MILLISECONDS);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());

        Thread.sleep(150);

        assertEquals(2, memoized.get());
        assertEquals(2, memoized.get());
        assertEquals(2, counter.get());
    }

    @Test
    public void testMemoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Function<String, Integer> func = (s) -> {
            counter.incrementAndGet();
            return s == null ? -1 : s.length();
        };

        Function<String, Integer> memoized = Fn.memoize(func);

        assertEquals(3, memoized.apply("abc"));
        assertEquals(3, memoized.apply("abc"));
        assertEquals(1, counter.get());

        assertEquals(4, memoized.apply("abcd"));
        assertEquals(4, memoized.apply("abcd"));
        assertEquals(2, counter.get());

        assertEquals(-1, memoized.apply(null));
        assertEquals(-1, memoized.apply(null));
        assertEquals(3, counter.get());
    }

    @Test
    public void testClose() {
        MyCloseable closeable = new MyCloseable();
        Runnable closer = Fn.close(closeable);
        assertEquals(0, closeable.getCloseCount());
        closer.run();
        assertEquals(1, closeable.getCloseCount());
        closer.run();
        assertEquals(1, closeable.getCloseCount());
    }

    @Test
    public void testCloseAllArray() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAll(c1, c2);
        assertEquals(0, c1.getCloseCount());
        assertEquals(0, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testCloseAllCollection() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAll(Arrays.asList(c1, c2));
        assertEquals(0, c1.getCloseCount());
        assertEquals(0, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testCloseQuietly() {
        MyCloseable c = new MyCloseable();
        Runnable closer = Fn.closeQuietly(c);
        closer.run();
        assertEquals(1, c.getCloseCount());
        closer.run();
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testCloseAllQuietlyArray() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAllQuietly(c1, c2);
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testCloseAllQuietlyCollection() {
        MyCloseable c1 = new MyCloseable();
        MyCloseable c2 = new MyCloseable();
        Runnable closer = Fn.closeAllQuietly(Arrays.asList(c1, c2));
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());
    }

    @Test
    public void testEmptyAction() {
        assertDoesNotThrow(() -> Fn.emptyAction().run());
    }

    @Test
    public void testShutDown() {
        ExecutorService mockService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void shutdown() {
                super.shutdown();
            }
        };
        Runnable shutdown = Fn.shutDown(mockService);
        assertFalse(mockService.isShutdown());
        shutdown.run();
        assertTrue(mockService.isShutdown());
        shutdown.run();
    }

    @Test
    public void testShutDownWithTimeout() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Runnable shutdown = Fn.shutDown(service, 100, TimeUnit.MILLISECONDS);
        assertFalse(service.isShutdown());
        shutdown.run();
        assertTrue(service.isShutdown());
        assertTrue(service.awaitTermination(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testEmptyConsumer() {
        assertDoesNotThrow(() -> Fn.emptyConsumer().accept("test"));
        assertDoesNotThrow(() -> Fn.doNothing().accept("test"));
    }

    @Test
    public void testThrowRuntimeException() {
        Exception e = assertThrows(RuntimeException.class, () -> Fn.throwRuntimeException("error").accept("test"));
        assertEquals("error", e.getMessage());
    }

    @Test
    public void testThrowException() {
        assertThrows(IllegalStateException.class, () -> Fn.throwException(IllegalStateException::new).accept("test"));
    }

    @Test
    public void testToRuntimeException() {
        Function<Throwable, RuntimeException> func = Fn.toRuntimeException();
        RuntimeException rte = new RuntimeException("test");
        assertSame(rte, func.apply(rte));

        Exception e = new Exception("test");
        RuntimeException result = func.apply(e);
        assertEquals("java.lang.Exception: test", result.getMessage());
        assertSame(e, result.getCause());
    }

    @Test
    public void testCloseConsumer() {
        MyCloseable c = new MyCloseable();
        Consumer<MyCloseable> consumer = Fn.close();
        consumer.accept(c);
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testCloseQuietlyConsumer() {
        MyCloseable c = new MyCloseable();
        Consumer<MyCloseable> consumer = Fn.closeQuietly();
        consumer.accept(c);
        assertEquals(1, c.getCloseCount());
    }

    @Test
    public void testSleep() {
        long start = System.currentTimeMillis();
        Fn.sleep(10).accept(null);
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 10);
    }

    @Test
    public void testSleepUninterruptibly() {
        long start = System.currentTimeMillis();
        Fn.sleepUninterruptibly(10).accept(null);
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 10);
    }

    @Test
    public void testRateLimiter() {
        assertDoesNotThrow(() -> Fn.rateLimiter(10.0).accept("test"));
        RateLimiter limiter = RateLimiter.create(10);
        assertDoesNotThrow(() -> Fn.rateLimiter(limiter).accept("test"));
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> Fn.println().accept("test"));
    }

    @Test
    public void testPrintlnWithSeparator() {
        assertDoesNotThrow(() -> Fn.println("=").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(":").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(": ").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("-").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("_").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(",").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(", ").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(" custom ").accept("key", "value"));
    }

    @Test
    public void testToStr() {
        assertEquals("123", Fn.toStr().apply(123));
        assertEquals("null", Fn.toStr().apply(null));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("fooBar", Fn.toCamelCase().apply("foo_bar"));
    }

    @Test
    public void testToLowerCase() {
        assertEquals("foo", Fn.toLowerCase().apply("FOO"));
    }

    @Test
    public void testToLowerCaseWithUnderscore() {
        assertEquals("foo_bar", Fn.toLowerCaseWithUnderscore().apply("fooBar"));
    }

    @Test
    public void testToUpperCase() {
        assertEquals("FOO", Fn.toUpperCase().apply("foo"));
    }

    @Test
    public void testToUpperCaseWithUnderscore() {
        assertEquals("FOO_BAR", Fn.toUpperCaseWithUnderscore().apply("fooBar"));
    }

    @Test
    public void testToJson() {
        assertEquals("[1, 2]", Fn.toJson().apply(Arrays.asList(1, 2)));
    }

    @Test
    public void testToXml() {
        assertEquals("<list>[1, 2]</list>", Fn.toXml().apply(Arrays.asList(1, 2)));
    }

    @Test
    public void testIdentity() {
        String s = "test";
        assertSame(s, Fn.identity().apply(s));
        assertNull(Fn.identity().apply(null));
    }

    @Test
    public void testKeyed() {
        Function<String, Keyed<Integer, String>> keyedFunc = Fn.keyed(String::length);
        Keyed<Integer, String> result = keyedFunc.apply("hello");
        assertEquals(5, result.key());
        assertEquals("hello", result.val());
    }

    @Test
    public void testVal() {
        Keyed<Integer, String> keyed = Keyed.of(5, "hello");
        assertEquals("hello", Fn.<Integer, String> val().apply(keyed));
    }

    @Test
    public void testKkv() {
        Keyed<String, Integer> keyedKey = Keyed.of("key", 123);
        Map.Entry<Keyed<String, Integer>, String> entry = N.newEntry(keyedKey, "value");
        assertEquals(123, Fn.<String, Integer, String> kkv().apply(entry));
    }

    @Test
    public void testWrapAndUnwrap() {
        String s = "test";
        Wrapper<String> wrapped = Fn.<String> wrap().apply(s);
        assertEquals(s, wrapped.value());
        assertEquals(s, Fn.<String> unwrap().apply(wrapped));
    }

    @Test
    public void testWrapWithFunctions() {
        Function<String, Wrapper<String>> wrapFunc = Fn.wrap(s -> 1, String::equals);
        Wrapper<String> wrapped = wrapFunc.apply("test");
        assertEquals(1, wrapped.hashCode());
        assertTrue(wrapped.equals(Wrapper.of("test")));
    }

    @Test
    public void testKeyAndValue() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 123);
        assertEquals("key", Fn.<String, Integer> key().apply(entry));
        assertEquals(123, Fn.<String, Integer> value().apply(entry));
    }

    @Test
    public void testLeftAndRight() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals("left", Fn.<String, Integer> left().apply(pair));
        assertEquals(123, Fn.<String, Integer> right().apply(pair));
    }

    @Test
    public void testInverse() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 123);
        Map.Entry<Integer, String> inverted = Fn.<String, Integer> inverse().apply(entry);
        assertEquals(123, inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    @Test
    public void testEntry() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entry().apply("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryWithKey() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entryWithKey("fixed").apply(123);
        assertEquals("fixed", entry.getKey());
        assertEquals(123, entry.getValue());

        entry = Fn.<String, Integer> entry("fixed").apply(123);
        assertEquals("fixed", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryByKeyMapper() {
        Function<Integer, Map.Entry<String, Integer>> func = Fn.entryByKeyMapper(i -> "key" + i);
        Map.Entry<String, Integer> entry = func.apply(123);
        assertEquals("key123", entry.getKey());
        assertEquals(123, entry.getValue());

        func = Fn.entry(i -> "key" + i);
        entry = func.apply(123);
        assertEquals("key123", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryWithValue() {
        Map.Entry<String, Integer> entry = Fn.<String, Integer> entryWithValue(123).apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryByValueMapper() {
        Function<String, Map.Entry<String, Integer>> func = Fn.entryByValueMapper(s -> s.length());
        Map.Entry<String, Integer> entry = func.apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    public void testPair() {
        Pair<String, Integer> pair = Fn.<String, Integer> pair().apply("key", 123);
        assertEquals("key", pair.left());
        assertEquals(123, pair.right());
    }

    @Test
    public void testTriple() {
        Triple<String, Integer, Boolean> triple = Fn.<String, Integer, Boolean> triple().apply("key", 123, true);
        assertEquals("key", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void testTuples() {
        assertEquals(Tuple.of("a"), Fn.tuple1().apply("a"));
        assertEquals(Tuple.of("a", 1), Fn.tuple2().apply("a", 1));
        assertEquals(Tuple.of("a", 1, true), Fn.tuple3().apply("a", 1, true));
        assertEquals(Tuple.of("a", 1, true, 2.0), Fn.tuple4().apply("a", 1, true, 2.0));
    }

    @Test
    public void testStringOperators() {
        assertEquals("a", Fn.trim().apply(" a "));
        assertEquals("a", Fn.trimToEmpty().apply(" a "));
        assertEquals("", Fn.trimToEmpty().apply("  "));
        assertEquals("a", Fn.trimToNull().apply(" a "));
        assertNull(Fn.trimToNull().apply("  "));

        assertEquals("a", Fn.strip().apply(" a "));
        assertEquals("a", Fn.stripToEmpty().apply(" a "));
        assertEquals("", Fn.stripToEmpty().apply("  "));
        assertEquals("a", Fn.stripToNull().apply(" a "));
        assertNull(Fn.stripToNull().apply("  "));

        assertEquals("", Fn.nullToEmpty().apply(null));
        assertEquals("a", Fn.nullToEmpty().apply("a"));
    }

    @Test
    public void testNullToEmptyCollections() {
        assertTrue(Fn.nullToEmptyList().apply(null).isEmpty());
        assertTrue(Fn.nullToEmptySet().apply(null).isEmpty());
        assertTrue(Fn.nullToEmptyMap().apply(null).isEmpty());

        List<String> list = new ArrayList<>();
        assertSame(list, Fn.<String> nullToEmptyList().apply(list));
    }

    @Test
    public void testLenLengthSize() {
        assertEquals(3, Fn.<String> len().apply(new String[] { "a", "b", "c" }));
        assertEquals(0, Fn.<String> len().apply(null));

        assertEquals(3, Fn.<String> length().apply("abc"));
        assertEquals(0, Fn.<String> length().apply(""));

        assertEquals(2, Fn.<Collection<Integer>> size().apply(Arrays.asList(1, 2)));
        assertEquals(0, Fn.<Collection<Integer>> size().apply(null));

        assertEquals(1, Fn.<Map<String, Integer>> sizeM().apply(Collections.singletonMap("a", 1)));
        assertEquals(0, Fn.<Map<String, Integer>> sizeM().apply(null));
    }

    @Test
    public void testCast() {
        Object obj = "string";
        String s = Fn.cast(String.class).apply(obj);
        assertEquals("string", s);
        assertThrows(ClassCastException.class, () -> {
            Object o = 123;
            Fn.cast(String.class).apply(o);
        });
    }

    @Test
    public void testBooleanPredicates() {
        assertTrue(Fn.alwaysTrue().test(null));
        assertFalse(Fn.alwaysFalse().test("any"));
    }

    @Test
    public void testNullEmptyBlankPredicates() {
        assertTrue(Fn.isNull().test(null));
        assertFalse(Fn.isNull().test(""));
        assertTrue(Fn.<String> isNull(Strings::trimToNull).test(" "));
        assertFalse(Fn.isNull(s -> s).test("a"));

        assertTrue(Fn.isEmpty().test(""));
        assertFalse(Fn.isEmpty().test(" "));
        assertTrue(Fn.isEmpty(s -> (String) s).test(""));
        assertFalse(Fn.isEmpty(s -> (String) s).test("a"));

        assertTrue(Fn.isBlank().test(" "));
        assertFalse(Fn.isBlank().test("a"));
        assertTrue(Fn.isBlank(s -> (String) s).test(" "));
        assertFalse(Fn.isBlank(s -> (String) s).test("a"));

        assertTrue(Fn.isEmptyA().test(new String[0]));
        assertTrue(Fn.isEmptyC().test(new ArrayList<>()));
        assertTrue(Fn.isEmptyM().test(new HashMap<>()));

        assertFalse(Fn.notNull().test(null));
        assertTrue(Fn.notNull().test(""));
        assertTrue(Fn.notNull(s -> s).test("a"));
        assertFalse(Fn.<String> notNull(Strings::trimToNull).test(" "));

        assertTrue(Fn.notEmpty().test(" "));
        assertFalse(Fn.notEmpty().test(""));
        assertTrue(Fn.notEmpty(s -> (String) s).test("a"));
        assertFalse(Fn.notEmpty(s -> (String) s).test(""));

        assertTrue(Fn.notBlank().test("a"));
        assertFalse(Fn.notBlank().test(" "));
        assertTrue(Fn.notBlank(s -> (String) s).test("a"));
        assertFalse(Fn.notBlank(s -> (String) s).test(" "));

        assertTrue(Fn.notEmptyA().test(new String[1]));
        assertTrue(Fn.notEmptyC().test(Arrays.asList(1)));
        assertTrue(Fn.notEmptyM().test(Collections.singletonMap("a", 1)));
    }

    @Test
    public void testFilePredicates(@TempDir Path tempDir) throws IOException {
        File file = File.createTempFile("test", ".txt", tempDir.toFile());
        File dir = tempDir.resolve("subdir").toFile();
        dir.mkdir();

        assertTrue(Fn.isFile().test(file));
        assertFalse(Fn.isFile().test(dir));
        assertFalse(Fn.isFile().test(null));

        assertTrue(Fn.isDirectory().test(dir));
        assertFalse(Fn.isDirectory().test(file));
        assertFalse(Fn.isDirectory().test(null));

        file.delete();
        dir.delete();
    }

    @Test
    public void testEqualityPredicates() {
        assertTrue(Fn.equal("a").test("a"));
        assertFalse(Fn.equal("a").test("b"));
        assertTrue(Fn.equal(null).test(null));

        assertTrue(Fn.eqOr("a", "b").test("a"));
        assertTrue(Fn.eqOr("a", "b").test("b"));
        assertFalse(Fn.eqOr("a", "b").test("c"));

        assertTrue(Fn.eqOr("a", "b", "c").test("c"));
        assertFalse(Fn.eqOr("a", "b", "c").test("d"));

        assertTrue(Fn.notEqual("a").test("b"));
        assertFalse(Fn.notEqual("a").test("a"));
    }

    @Test
    public void testComparisonPredicates() {
        assertTrue(Fn.greaterThan(5).test(10));
        assertFalse(Fn.greaterThan(5).test(5));

        assertTrue(Fn.greaterEqual(5).test(5));
        assertFalse(Fn.greaterEqual(5).test(4));

        assertTrue(Fn.lessThan(5).test(0));
        assertFalse(Fn.lessThan(5).test(5));

        assertTrue(Fn.lessEqual(5).test(5));
        assertFalse(Fn.lessEqual(5).test(6));
    }

    @Test
    public void testRangePredicates() {
        assertTrue(Fn.gtAndLt(5, 10).test(7));
        assertFalse(Fn.gtAndLt(5, 10).test(5));
        assertFalse(Fn.gtAndLt(5, 10).test(10));

        assertTrue(Fn.geAndLt(5, 10).test(5));
        assertFalse(Fn.geAndLt(5, 10).test(10));

        assertTrue(Fn.geAndLe(5, 10).test(5));
        assertTrue(Fn.geAndLe(5, 10).test(10));
        assertFalse(Fn.geAndLe(5, 10).test(11));

        assertTrue(Fn.gtAndLe(5, 10).test(10));
        assertFalse(Fn.gtAndLe(5, 10).test(5));

        assertTrue(Fn.between(5, 10).test(7));
    }

    @Test
    public void testMembershipPredicates() {
        Collection<String> coll = Arrays.asList("a", "b");
        assertTrue(Fn.in(coll).test("a"));
        assertFalse(Fn.in(coll).test("c"));

        assertTrue(Fn.notIn(coll).test("c"));
        assertFalse(Fn.notIn(coll).test("a"));
    }

    @Test
    public void testTypePredicates() {
        assertTrue(Fn.instanceOf(String.class).test("a"));
        assertFalse(Fn.instanceOf(String.class).test(123));

        assertTrue(Fn.subtypeOf(Object.class).test(String.class));
        assertFalse(Fn.subtypeOf(String.class).test(Object.class));
    }

    @Test
    public void testStringContentPredicates() {
        assertTrue(Fn.startsWith("pre").test("prefix"));
        assertFalse(Fn.startsWith("pre").test("postfix"));

        assertTrue(Fn.endsWith("fix").test("prefix"));
        assertFalse(Fn.endsWith("fix").test("prelude"));

        assertTrue(Fn.contains("fix").test("prefix"));
        assertFalse(Fn.contains("fix").test("prelude"));

        assertFalse(Fn.notStartsWith("pre").test("prefix"));
        assertTrue(Fn.notStartsWith("pre").test("postfix"));

        assertFalse(Fn.notEndsWith("fix").test("prefix"));
        assertTrue(Fn.notEndsWith("fix").test("prelude"));

        assertFalse(Fn.notContains("fix").test("prefix"));
        assertTrue(Fn.notContains("fix").test("prelude"));
    }

    @Test
    public void testMatches() {
        Predicate<CharSequence> p = Fn.matches(Pattern.compile("\\d+"));
        assertTrue(p.test("123"));
        assertFalse(p.test("abc"));
    }

    @Test
    public void testBiPredicateFactories() {
        assertTrue(Fn.equal().test("a", "a"));
        assertFalse(Fn.notEqual().test("a", "a"));
        assertTrue(Fn.<Integer> greaterThan().test(10, 5));
        assertTrue(Fn.<Integer> greaterEqual().test(5, 5));
        assertTrue(Fn.<Integer> lessThan().test(5, 10));
        assertTrue(Fn.<Integer> lessEqual().test(5, 5));
    }

    @Test
    public void testNot() {
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNotEmpty = Fn.not(isEmpty);
        assertTrue(isNotEmpty.test("a"));
        assertFalse(isNotEmpty.test(""));
    }

    @Test
    public void testAndOr() {
        Predicate<String> p1 = s -> s.startsWith("a");
        Predicate<String> p2 = s -> s.length() > 2;
        assertTrue(Fn.and(p1, p2).test("abc"));
        assertFalse(Fn.and(p1, p2).test("ab"));
        assertFalse(Fn.and(p1, p2).test("bc"));

        assertTrue(Fn.or(p1, p2).test("abc"));
        assertTrue(Fn.or(p1, p2).test("ab"));
        assertFalse(Fn.or(p1, p2).test("bc"));
    }

    @Test
    public void testEntryFunctions() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 10);
        assertTrue(Fn.<String, Integer> testByKey(k -> k.equals("key")).test(entry));
        assertTrue(Fn.<String, Integer> testByValue(v -> v > 5).test(entry));

        AtomicReference<String> keyRef = new AtomicReference<>();
        Fn.<String, Integer> acceptByKey(keyRef::set).accept(entry);
        assertEquals("key", keyRef.get());

        AtomicReference<Integer> valRef = new AtomicReference<>();
        Fn.<String, Integer> acceptByValue(valRef::set).accept(entry);
        assertEquals(10, valRef.get());

        assertEquals(3, Fn.<String, Integer, Integer> applyByKey(String::length).apply(entry));
        assertEquals(20, Fn.<String, Integer, Integer> applyByValue(v -> v * 2).apply(entry));
    }

    @Test
    public void testMapKeyAndValue() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 10);
        Map.Entry<Integer, Integer> newKeyEntry = Fn.<String, Integer, Integer> mapKey(String::length).apply(entry);
        assertEquals(3, newKeyEntry.getKey());
        assertEquals(10, newKeyEntry.getValue());

        Map.Entry<String, String> newValEntry = Fn.<String, Integer, String> mapValue(Object::toString).apply(entry);
        assertEquals("key", newValEntry.getKey());
        assertEquals("10", newValEntry.getValue());
    }

    @Test
    public void testEntryKeyVal() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 3);
        assertTrue(Fn.<String, Integer> testKeyVal((k, v) -> k.length() == v).test(entry));

        AtomicReference<String> ref = new AtomicReference<>();
        Fn.<String, Integer> acceptKeyVal((k, v) -> ref.set(k + v)).accept(entry);
        assertEquals("key3", ref.get());

        String result = Fn.<String, Integer, String> applyKeyVal((k, v) -> k + v).apply(entry);
        assertEquals("key3", result);
    }

    @Test
    public void testConditionalAccept() {
        List<String> list = new ArrayList<>();
        Consumer<String> consumer = Fn.acceptIfNotNull(list::add);
        Stream.of("a", null, "b").forEach(consumer);
        assertEquals(Arrays.asList("a", "b"), list);

        list.clear();
        Consumer<String> consumer2 = Fn.acceptIf(s -> s.length() > 1, list::add);
        Stream.of("a", "bb", "c", "dd").forEach(consumer2);
        assertEquals(Arrays.asList("bb", "dd"), list);

        List<String> trueList = new ArrayList<>();
        List<String> falseList = new ArrayList<>();
        Consumer<String> consumer3 = Fn.acceptIfOrElse(s -> s.length() > 1, trueList::add, falseList::add);
        Stream.of("a", "bb", "c", "dd").forEach(consumer3);
        assertEquals(Arrays.asList("bb", "dd"), trueList);
        assertEquals(Arrays.asList("a", "c"), falseList);
    }

    @Test
    public void testConditionalApply() {
        Function<String, Collection<String>> f1 = Fn.applyIfNotNullOrEmpty(s -> Arrays.asList(s, s));
        assertEquals(Collections.emptyList(), f1.apply(null));
        assertEquals(Arrays.asList("a", "a"), f1.apply("a"));
    }

    @Test
    public void testParsingFunctions() {
        assertEquals((byte) 12, Fn.parseByte().applyAsByte("12"));
        assertEquals((short) 123, Fn.parseShort().applyAsShort("123"));
        assertEquals(1234, Fn.parseInt().applyAsInt("1234"));
        assertEquals(12345L, Fn.parseLong().applyAsLong("12345"));
        assertEquals(12.3f, Fn.parseFloat().applyAsFloat("12.3"), 0.01);
        assertEquals(12.34, Fn.parseDouble().applyAsDouble("12.34"), 0.01);
        assertEquals(123, Fn.createNumber().apply("123"));
    }

    @Test
    public void testNumToPrimitives() {
        assertEquals(123, Fn.numToInt().applyAsInt(123.45));
        assertEquals(123L, Fn.numToLong().applyAsLong(123.45));
        assertEquals(123.45, Fn.numToDouble().applyAsDouble(123.45f), 0.001);
    }

    @Test
    public void testAtMost() {
        Predicate<Object> p = Fn.atMost(2);
        assertTrue(p.test("a"));
        assertTrue(p.test("b"));
        assertFalse(p.test("c"));
        assertFalse(p.test("d"));
    }

    @Test
    public void testLimitThenFilter() {
        Predicate<Integer> p = Fn.limitThenFilter(3, i -> i % 2 == 0);
        long count = IntStream.range(0, 10).filter(p::test).count();
        assertEquals(2, count);
        Predicate<Integer> p2 = Fn.limitThenFilter(3, i -> i > 0);
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(p2).count());
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        Predicate<Object> p = Fn.timeLimit(100);
        assertTrue(p.test(null));
        Thread.sleep(150);
        assertFalse(p.test(null));
    }

    @Test
    public void testIndexed() {
        Function<String, Indexed<String>> func = Fn.indexed();
        assertEquals(Indexed.of("a", 0), func.apply("a"));
        assertEquals(Indexed.of("b", 1), func.apply("b"));
    }

    @Test
    public void testCompare() {
        assertEquals(0, Fn.<Integer> compare().apply(5, 5));
        assertTrue(Fn.<Integer> compare().apply(10, 5) > 0);
        assertTrue(Fn.<Integer> compare(Comparator.reverseOrder()).apply(10, 5) < 0);
    }

    @Test
    public void testFutureGet() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> "done");
        assertEquals("done", Fn.<String> futureGet().apply(future));

        Future<String> failedFuture = executor.submit(() -> {
            throw new IOException("fail");
        });
        assertThrows(RuntimeException.class, () -> Fn.<String> futureGet().apply(failedFuture));
        executor.shutdown();
    }

    @Nested
    public class SuppliersTest {
        @Test
        public void testOfInstance() {
            String s = "instance";
            Supplier<String> supplier = Suppliers.ofInstance(s);
            assertSame(s, supplier.get());
        }

        @Test
        public void testCollectionSuppliers() {
            assertTrue(Suppliers.ofList().get() instanceof ArrayList);
            assertTrue(Suppliers.ofSet().get() instanceof HashSet);
            assertTrue(Suppliers.ofMap().get() instanceof HashMap);
            assertTrue(Suppliers.ofCollection(LinkedList.class).get() instanceof LinkedList);
            assertTrue(Suppliers.ofMap(TreeMap.class).get() instanceof TreeMap);
        }

        @Test
        public void testRegister() {
            assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForCollection(ArrayList.class, ArrayList::new));
            assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForMap(HashMap.class, HashMap::new));

        }
    }

    @Nested
    public class IntFunctionsTest {
        @Test
        public void testArrayFactories() {
            assertArrayEquals(new int[5], IntFunctions.ofIntArray().apply(5));
            assertArrayEquals(new String[3], IntFunctions.ofStringArray().apply(3));
        }

        @Test
        public void testCollectionFactories() {
            assertEquals(0, ((ArrayList<?>) IntFunctions.ofList().apply(10)).size());
            assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
            assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
        }
    }

    @Nested
    public class PredicatesTest {
        @Test
        public void testIndexed() {
            Predicate<String> p = Predicates.indexed((idx, s) -> idx % 2 == 0);
            assertTrue(p.test("a"));
            assertFalse(p.test("b"));
            assertTrue(p.test("c"));
        }

        @Test
        public void testDistinct() {
            Predicate<String> p = Predicates.distinct();
            assertTrue(p.test("a"));
            assertTrue(p.test("b"));
            assertFalse(p.test("a"));
            assertTrue(p.test("c"));
        }

        @Test
        public void testDistinctBy() {
            Predicate<String> p = Predicates.distinctBy(String::length);
            assertTrue(p.test("a"));
            assertTrue(p.test("bb"));
            assertFalse(p.test("c"));
            assertTrue(p.test("ddd"));
        }

        @Test
        public void testConcurrentDistinct() {
            Predicate<String> p = Predicates.concurrentDistinct();
            List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
            long count = list.parallelStream().filter(p).count();
            assertEquals(3, count);
        }

        @Test
        public void testSkipRepeats() {
            Predicate<String> p = Predicates.skipRepeats();
            assertTrue(p.test("a"));
            assertFalse(p.test("a"));
            assertTrue(p.test("b"));
            assertTrue(p.test("c"));
            assertFalse(p.test("c"));
        }
    }

    @Nested
    public class BiPredicatesTest {
        @Test
        public void testAlways() {
            assertTrue(BiPredicates.alwaysTrue().test(null, null));
            assertFalse(BiPredicates.alwaysFalse().test("a", "b"));
        }

        @Test
        public void testIndexed() {
            BiPredicate<String, String> p = BiPredicates.indexed((idx, s1, s2) -> idx == 1);
            assertFalse(p.test("a", "b"));
            assertTrue(p.test("c", "d"));
        }
    }

    @Nested
    public class FnnTest {
        @Test
        public void testMemoize() {
            AtomicInteger counter = new AtomicInteger(0);
            Throwables.Supplier<Integer, IOException> supplier = () -> {
                counter.incrementAndGet();
                return 1;
            };
            Throwables.Supplier<Integer, IOException> memoized = Fnn.memoize(supplier);

            assertDoesNotThrow(() -> {
                assertEquals(1, memoized.get());
                assertEquals(1, memoized.get());
            });
            assertEquals(1, counter.get());
        }

        @Test
        public void testSs() {
            Throwables.Supplier<String, IOException> supplier = () -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.ss(supplier).get());

            Throwables.Function<String, Integer, IOException> func = s -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.ss("a", func).get());
        }

        @Test
        public void testPp() {
            Throwables.Predicate<String, IOException> predicate = s -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.pp(predicate).test("a"));

            Throwables.BiPredicate<String, String, IOException> biPredicate = (s1, s2) -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.pp("a", biPredicate).test("b"));
        }

        @Test
        public void testCc() {
            Throwables.Consumer<String, IOException> consumer = s -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.cc(consumer).accept("a"));

            Throwables.Callable<String, IOException> callable = () -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.cc(callable).call());
        }

        @Test
        public void testFf() {
            Throwables.Function<String, Integer, IOException> func = s -> {
                throw new IOException("e");
            };
            assertThrows(RuntimeException.class, () -> Fn.ff(func).apply("a"));

            assertEquals(Integer.valueOf(-1), Fn.ff(func, -1).apply("a"));
        }

        @Test
        public void testSp() {
            Object mutex = new Object();
            AtomicInteger counter = new AtomicInteger(0);
            Predicate<Integer> p = i -> {
                counter.incrementAndGet();
                N.sleep(10);
                return true;
            };
            Predicate<Integer> sp = Fn.sp(mutex, p);

            IntStream.range(0, 5).parallel().forEach(sp::test);
            assertEquals(5, counter.get());
        }

        @Test
        public void testSc() {
            Object mutex = new Object();
            List<Integer> list = new ArrayList<>();
            Consumer<Integer> c = i -> {
                list.add(i);
                N.sleep(1);
            };
            Consumer<Integer> sc = Fn.sc(mutex, c);

            IntStream.range(0, 100).parallel().forEach(sc::accept);
            assertEquals(100, list.size());
        }

        @Test
        public void testSf() {
            Object mutex = new Object();
            List<Integer> list = new ArrayList<>();
            Function<Integer, Integer> f = i -> {
                list.add(i);
                N.sleep(1);
                return i;
            };
            Function<Integer, Integer> sf = Fn.sf(mutex, f);

            List<Integer> result = IntStream.range(0, 100).parallel().mapToObj(sf::apply).collect(Collectors.toList());
            assertEquals(100, list.size());
            assertEquals(100, result.size());
        }
    }

    @Nested
    public class BinaryOperatorsTest {
        @Test
        public void testThrowingMerger() {
            assertThrows(IllegalStateException.class, () -> Fn.throwingMerger().apply("a", "b"));
        }

        @Test
        public void testIgnoringMerger() {
            assertEquals("a", Fn.ignoringMerger().apply("a", "b"));
        }

        @Test
        public void testReplacingMerger() {
            assertEquals("b", Fn.replacingMerger().apply("a", "b"));
        }
    }

    @Nested
    public class EntriesTest {
        @Test
        public void testF() {
            Map.Entry<String, Integer> entry = N.newEntry("a", 1);
            assertEquals("a1", Entries.<String, Integer, String> f((k, v) -> k + v).apply(entry));
        }

        @Test
        public void testP() {
            Map.Entry<String, Integer> entry = N.newEntry("a", 1);
            assertTrue(Entries.<String, Integer> p((k, v) -> k.equals("a") && v == 1).test(entry));
        }

        @Test
        public void testC() {
            Map.Entry<String, Integer> entry = N.newEntry("a", 1);
            AtomicReference<String> ref = new AtomicReference<>();
            Entries.<String, Integer> c((k, v) -> ref.set(k + v)).accept(entry);
            assertEquals("a1", ref.get());
        }
    }

    @Nested
    public class FITest {
        @Test
        public void testPositive() {
            assertTrue(FI.positive().test(1));
            assertFalse(FI.positive().test(0));
            assertFalse(FI.positive().test(-1));
        }

        @Test
        public void testNotNegative() {
            assertTrue(FI.notNegative().test(1));
            assertTrue(FI.notNegative().test(0));
            assertFalse(FI.notNegative().test(-1));
        }

        @Test
        public void testEqual() {
            assertTrue(FI.equal().test(1, 1));
            assertFalse(FI.equal().test(1, 2));
        }

        @Test
        public void testUnbox() {
            assertEquals(1, FI.unbox().applyAsInt(1));
        }

        @Test
        public void testLen() {
            assertEquals(3, FI.len().apply(new int[] { 1, 2, 3 }));
            assertEquals(0, FI.len().apply(null));
        }

        @Test
        public void testSum() {
            assertEquals(6, FI.sum().apply(new int[] { 1, 2, 3 }));
        }

        @Test
        public void testAverage() {
            assertEquals(2.0, FI.average().apply(new int[] { 1, 2, 3 }));
        }

        @Test
        public void testAlternate() {
            IntBiFunction<MergeResult> func = FI.alternate();
            assertEquals(MergeResult.TAKE_FIRST, func.apply(1, 2));
            assertEquals(MergeResult.TAKE_SECOND, func.apply(1, 2));
            assertEquals(MergeResult.TAKE_FIRST, func.apply(1, 2));
        }

        @Test
        public void testIntBinaryOperators() {
            assertEquals(1, FI.IntBinaryOperators.MIN.applyAsInt(1, 2));
            assertEquals(2, FI.IntBinaryOperators.MAX.applyAsInt(1, 2));
        }
    }

    @Nested
    public class FLTest {
        @Test
        public void testPositive() {
            assertTrue(FL.positive().test(1L));
            assertFalse(FL.positive().test(0L));
            assertFalse(FL.positive().test(-1L));
        }
    }

    @Test
    public void testGetAsPrimitives() {
        assertEquals(true, Fn.GET_AS_BOOLEAN.applyAsBoolean(com.landawn.abacus.util.u.OptionalBoolean.of(true)));
        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(com.landawn.abacus.util.u.OptionalChar.of('a')));
        assertEquals((byte) 1, Fn.GET_AS_BYTE.applyAsByte(com.landawn.abacus.util.u.OptionalByte.of((byte) 1)));
        assertEquals((short) 1, Fn.GET_AS_SHORT.applyAsShort(com.landawn.abacus.util.u.OptionalShort.of((short) 1)));
        assertEquals(1, Fn.GET_AS_INT.applyAsInt(com.landawn.abacus.util.u.OptionalInt.of(1)));
        assertEquals(1L, Fn.GET_AS_LONG.applyAsLong(com.landawn.abacus.util.u.OptionalLong.of(1L)));
        assertEquals(1.0f, Fn.GET_AS_FLOAT.applyAsFloat(com.landawn.abacus.util.u.OptionalFloat.of(1.0f)));
        assertEquals(1.0, Fn.GET_AS_DOUBLE.applyAsDouble(com.landawn.abacus.util.u.OptionalDouble.of(1.0)));

        assertEquals(1, Fn.GET_AS_INT_JDK.applyAsInt(java.util.OptionalInt.of(1)));
        assertEquals(1L, Fn.GET_AS_LONG_JDK.applyAsLong(java.util.OptionalLong.of(1L)));
        assertEquals(1.0, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(java.util.OptionalDouble.of(1.0)));
    }

    @Test
    public void testIsPresentPrimitives() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(com.landawn.abacus.util.u.OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(com.landawn.abacus.util.u.OptionalBoolean.empty()));
        assertTrue(Fn.IS_PRESENT_CHAR.test(com.landawn.abacus.util.u.OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(com.landawn.abacus.util.u.OptionalChar.empty()));
        assertTrue(Fn.IS_PRESENT_BYTE.test(com.landawn.abacus.util.u.OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(com.landawn.abacus.util.u.OptionalByte.empty()));
        assertTrue(Fn.IS_PRESENT_SHORT.test(com.landawn.abacus.util.u.OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(com.landawn.abacus.util.u.OptionalShort.empty()));
        assertTrue(Fn.IS_PRESENT_INT.test(com.landawn.abacus.util.u.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT.test(com.landawn.abacus.util.u.OptionalInt.empty()));
        assertTrue(Fn.IS_PRESENT_LONG.test(com.landawn.abacus.util.u.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(com.landawn.abacus.util.u.OptionalLong.empty()));
        assertTrue(Fn.IS_PRESENT_FLOAT.test(com.landawn.abacus.util.u.OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(com.landawn.abacus.util.u.OptionalFloat.empty()));
        assertTrue(Fn.IS_PRESENT_DOUBLE.test(com.landawn.abacus.util.u.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(com.landawn.abacus.util.u.OptionalDouble.empty()));

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));
        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));
        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }
}
