package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;

public class Fn102Test extends TestBase {

    @Test
    public void testMemoize() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = Fn.memoize(() -> counter.incrementAndGet());

        assertEquals(1, supplier.get());
        assertEquals(1, supplier.get()); // Should return cached value
        assertEquals(1, supplier.get()); // Should return cached value
        assertEquals(1, counter.get()); // Counter should only be incremented once
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = Fn.memoizeWithExpiration(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);

        assertEquals(1, supplier.get());
        assertEquals(1, supplier.get()); // Should return cached value

        Thread.sleep(150); // Wait for expiration

        assertEquals(2, supplier.get()); // Should compute new value
        assertEquals(2, supplier.get()); // Should return new cached value
    }

    @Test
    public void testMemoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> func = Fn.memoize(s -> {
            counter.incrementAndGet();
            return N.len(s);
        });

        assertEquals(5, func.apply("hello"));
        assertEquals(5, func.apply("hello")); // Should return cached value
        assertEquals(3, func.apply("bye"));
        assertEquals(2, counter.get()); // Counter should be incremented twice

        // Test null handling
        assertEquals(0, func.apply(null));
        assertEquals(0, func.apply(null)); // Should return cached null result
    }

    @Test
    public void testClose() {
        TestAutoCloseable closeable = new TestAutoCloseable();
        Runnable closeRunnable = Fn.close(closeable);

        assertFalse(closeable.isClosed());
        closeRunnable.run();
        assertTrue(closeable.isClosed());

        // Should only close once
        closeRunnable.run();
        assertEquals(1, closeable.getCloseCount());
    }

    @Test
    public void testCloseAll() {
        TestAutoCloseable closeable1 = new TestAutoCloseable();
        TestAutoCloseable closeable2 = new TestAutoCloseable();
        Runnable closeRunnable = Fn.closeAll(closeable1, closeable2);

        assertFalse(closeable1.isClosed());
        assertFalse(closeable2.isClosed());
        closeRunnable.run();
        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void testCloseAllCollection() {
        TestAutoCloseable closeable1 = new TestAutoCloseable();
        TestAutoCloseable closeable2 = new TestAutoCloseable();
        List<TestAutoCloseable> closeables = Arrays.asList(closeable1, closeable2);
        Runnable closeRunnable = Fn.closeAll(closeables);

        closeRunnable.run();
        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void testCloseQuietly() {
        TestAutoCloseable closeable = new TestAutoCloseable();
        closeable.setThrowOnClose(true);
        Runnable closeRunnable = Fn.closeQuietly(closeable);

        assertDoesNotThrow(() -> closeRunnable.run());
        assertTrue(closeable.isClosed());
    }

    @Test
    public void testCloseAllQuietly() {
        TestAutoCloseable closeable1 = new TestAutoCloseable();
        closeable1.setThrowOnClose(true);
        TestAutoCloseable closeable2 = new TestAutoCloseable();
        Runnable closeRunnable = Fn.closeAllQuietly(closeable1, closeable2);

        assertDoesNotThrow(() -> closeRunnable.run());
        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void testCloseAllQuietlyCollection() {
        TestAutoCloseable closeable1 = new TestAutoCloseable();
        closeable1.setThrowOnClose(true);
        TestAutoCloseable closeable2 = new TestAutoCloseable();
        List<TestAutoCloseable> closeables = Arrays.asList(closeable1, closeable2);
        Runnable closeRunnable = Fn.closeAllQuietly(closeables);

        assertDoesNotThrow(() -> closeRunnable.run());
        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void testEmptyAction() {
        Runnable emptyAction = Fn.emptyAction();
        assertDoesNotThrow(() -> emptyAction.run());
    }

    @Test
    public void testShutDown() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Runnable shutdownRunnable = Fn.shutDown(service);

        assertFalse(service.isShutdown());
        shutdownRunnable.run();
        assertTrue(service.isShutdown());
    }

    @Test
    public void testShutDownWithTimeout() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Runnable shutdownRunnable = Fn.shutDown(service, 100, TimeUnit.MILLISECONDS);
        shutdownRunnable.run();
        assertTrue(service.isShutdown());
    }

    @Test
    public void testDoNothing() {
        Consumer<String> consumer = Fn.doNothing();
        assertDoesNotThrow(() -> consumer.accept("test"));
    }

    @Test
    public void testEmptyConsumer() {
        Consumer<String> consumer = Fn.emptyConsumer();
        assertDoesNotThrow(() -> consumer.accept("test"));
    }

    @Test
    public void testThrowRuntimeException() {
        Consumer<String> consumer = Fn.throwRuntimeException("Test error");
        assertThrows(RuntimeException.class, () -> consumer.accept("test"), "Test error");
    }

    @Test
    public void testThrowException() {
        Consumer<String> consumer = Fn.throwException(() -> new IllegalStateException("Test exception"));
        assertThrows(IllegalStateException.class, () -> consumer.accept("test"), "Test exception");
    }

    @Test
    public void testToRuntimeException() {
        Function<Throwable, RuntimeException> func = Fn.toRuntimeException();

        RuntimeException re = new RuntimeException("test");
        assertSame(re, func.apply(re));

        Exception e = new Exception("test");
        RuntimeException wrapped = func.apply(e);
        assertNotNull(wrapped);
        assertEquals(e, wrapped.getCause());
    }

    @Test
    public void testCloseConsumer() {
        TestAutoCloseable closeable = new TestAutoCloseable();
        Consumer<TestAutoCloseable> closeConsumer = Fn.close();

        closeConsumer.accept(closeable);
        assertTrue(closeable.isClosed());
    }

    @Test
    public void testCloseQuietlyConsumer() {
        TestAutoCloseable closeable = new TestAutoCloseable();
        closeable.setThrowOnClose(true);
        Consumer<TestAutoCloseable> closeConsumer = Fn.closeQuietly();

        assertDoesNotThrow(() -> closeConsumer.accept(closeable));
        assertTrue(closeable.isClosed());
    }

    @Test
    public void testSleep() throws InterruptedException {
        Consumer<String> sleepConsumer = Fn.sleep(50);
        long start = System.currentTimeMillis();
        sleepConsumer.accept("test");
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 40); // Allow some margin
    }

    @Test
    public void testSleepUninterruptibly() {
        Consumer<String> sleepConsumer = Fn.sleepUninterruptibly(50);
        long start = System.currentTimeMillis();
        sleepConsumer.accept("test");
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 40); // Allow some margin
    }

    @Test
    public void testRateLimiter() throws InterruptedException {
        Consumer<String> rateLimitedConsumer = Fn.rateLimiter(2.0); // 2 permits per second

        long start = System.currentTimeMillis();
        rateLimitedConsumer.accept("1");
        rateLimitedConsumer.accept("2");
        rateLimitedConsumer.accept("3"); // This should wait
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 400); // Should take at least 500ms for 3 permits at 2/sec
    }

    @Test
    public void testPrintln() {
        Consumer<String> printlnConsumer = Fn.println();
        assertDoesNotThrow(() -> printlnConsumer.accept("test"));
    }

    @Test
    public void testPrintlnWithSeparator() {
        BiConsumer<String, String> printlnConsumer = Fn.println("=");
        assertDoesNotThrow(() -> printlnConsumer.accept("key", "value"));

        // Test various separators
        assertDoesNotThrow(() -> Fn.println(":").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println(", ").accept("key", "value"));
        assertDoesNotThrow(() -> Fn.println("-").accept("key", "value"));
    }

    @Test
    public void testToStr() {
        Function<Object, String> toStr = Fn.toStr();
        assertEquals("hello", toStr.apply("hello"));
        assertEquals("123", toStr.apply(123));
        assertEquals("null", toStr.apply(null));
    }

    @Test
    public void testToCamelCase() {
        UnaryOperator<String> toCamelCase = Fn.toCamelCase();
        assertEquals("helloWorld", toCamelCase.apply("hello_world"));
        assertEquals("helloWorld", toCamelCase.apply("HELLO_WORLD"));
    }

    @Test
    public void testToLowerCase() {
        UnaryOperator<String> toLowerCase = Fn.toLowerCase();
        assertEquals("hello", toLowerCase.apply("HELLO"));
        assertEquals("hello", toLowerCase.apply("Hello"));
    }

    @Test
    public void testToLowerCaseWithUnderscore() {
        UnaryOperator<String> toLowerCaseWithUnderscore = Fn.toLowerCaseWithUnderscore();
        assertEquals("hello_world", toLowerCaseWithUnderscore.apply("helloWorld"));
        assertEquals("hello_world", toLowerCaseWithUnderscore.apply("HelloWorld"));
    }

    @Test
    public void testToUpperCase() {
        UnaryOperator<String> toUpperCase = Fn.toUpperCase();
        assertEquals("HELLO", toUpperCase.apply("hello"));
        assertEquals("HELLO", toUpperCase.apply("Hello"));
    }

    @Test
    public void testToUpperCaseWithUnderscore() {
        UnaryOperator<String> toUpperCaseWithUnderscore = Fn.toUpperCaseWithUnderscore();
        assertEquals("HELLO_WORLD", toUpperCaseWithUnderscore.apply("helloWorld"));
        assertEquals("HELLO_WORLD", toUpperCaseWithUnderscore.apply("HelloWorld"));
    }

    @Test
    public void testToJson() {
        Function<Object, String> toJson = Fn.toJson();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String json = toJson.apply(map);
        assertNotNull(json);
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    public void testToXml() {
        Function<Object, String> toXml = Fn.toXml();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String xml = toXml.apply(map);
        assertNotNull(xml);
    }

    @Test
    public void testIdentity() {
        Function<String, String> identity = Fn.identity();
        assertEquals("test", identity.apply("test"));
        assertNull(identity.apply(null));
    }

    @Test
    public void testKeyed() {
        Function<String, Keyed<Integer, String>> keyedFunc = Fn.keyed(String::length);
        Keyed<Integer, String> keyed = keyedFunc.apply("hello");
        assertEquals(5, keyed.key());
        assertEquals("hello", keyed.val());
    }

    @Test
    public void testVal() {
        Function<Keyed<String, Integer>, Integer> valFunc = Fn.val();
        Keyed<String, Integer> keyed = Keyed.of("key", 123);
        assertEquals(123, valFunc.apply(keyed));
    }

    @Test
    public void testKkv() {
        Function<Map.Entry<Keyed<String, Integer>, String>, Integer> kkvFunc = Fn.kkv();
        Keyed<String, Integer> keyed = Keyed.of("key", 123);
        Map.Entry<Keyed<String, Integer>, String> entry = new AbstractMap.SimpleEntry<>(keyed, "value");
        assertEquals(123, kkvFunc.apply(entry));
    }

    @Test
    public void testWrap() {
        Function<String, Wrapper<String>> wrapFunc = Fn.wrap();
        Wrapper<String> wrapped = wrapFunc.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testWrapWithCustomFunctions() {
        Function<String, Wrapper<String>> wrapFunc = Fn.wrap(String::length, String::equals);
        Wrapper<String> wrapped = wrapFunc.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testUnwrap() {
        Function<Wrapper<String>, String> unwrapFunc = Fn.unwrap();
        Wrapper<String> wrapped = Wrapper.of("test");
        assertEquals("test", unwrapFunc.apply(wrapped));
    }

    @Test
    public void testKey() {
        Function<Map.Entry<String, Integer>, String> keyFunc = Fn.key();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        assertEquals("key", keyFunc.apply(entry));
    }

    @Test
    public void testValue() {
        Function<Map.Entry<String, Integer>, Integer> valueFunc = Fn.value();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        assertEquals(123, valueFunc.apply(entry));
    }

    @Test
    public void testLeft() {
        Function<Pair<String, Integer>, String> leftFunc = Fn.left();
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals("left", leftFunc.apply(pair));
    }

    @Test
    public void testRight() {
        Function<Pair<String, Integer>, Integer> rightFunc = Fn.right();
        Pair<String, Integer> pair = Pair.of("left", 123);
        assertEquals(123, rightFunc.apply(pair));
    }

    @Test
    public void testInverse() {
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>> inverseFunc = Fn.inverse();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        Map.Entry<Integer, String> inverted = inverseFunc.apply(entry);
        assertEquals(123, inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    @Test
    public void testEntry() {
        BiFunction<String, Integer, Map.Entry<String, Integer>> entryFunc = Fn.entry();
        Map.Entry<String, Integer> entry = entryFunc.apply("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryWithKey() {
        Function<Integer, Map.Entry<String, Integer>> entryFunc = Fn.entryWithKey("fixedKey");
        Map.Entry<String, Integer> entry = entryFunc.apply(123);
        assertEquals("fixedKey", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryByKeyMapper() {
        Function<String, Map.Entry<Integer, String>> entryFunc = Fn.entryByKeyMapper(String::length);
        Map.Entry<Integer, String> entry = entryFunc.apply("hello");
        assertEquals(5, entry.getKey());
        assertEquals("hello", entry.getValue());
    }

    @Test
    public void testEntryWithValue() {
        Function<String, Map.Entry<String, Integer>> entryFunc = Fn.entryWithValue(123);
        Map.Entry<String, Integer> entry = entryFunc.apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    @Test
    public void testEntryByValueMapper() {
        Function<String, Map.Entry<String, Integer>> entryFunc = Fn.entryByValueMapper(String::length);
        Map.Entry<String, Integer> entry = entryFunc.apply("hello");
        assertEquals("hello", entry.getKey());
        assertEquals(5, entry.getValue());
    }

    @Test
    public void testPair() {
        BiFunction<String, Integer, Pair<String, Integer>> pairFunc = Fn.pair();
        Pair<String, Integer> pair = pairFunc.apply("left", 123);
        assertEquals("left", pair.left());
        assertEquals(123, pair.right());
    }

    @Test
    public void testTriple() {
        TriFunction<String, Integer, Boolean, Triple<String, Integer, Boolean>> tripleFunc = Fn.triple();
        Triple<String, Integer, Boolean> triple = tripleFunc.apply("left", 123, true);
        assertEquals("left", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void testTuple1() {
        Function<String, Tuple1<String>> tupleFunc = Fn.tuple1();
        Tuple1<String> tuple = tupleFunc.apply("value");
        assertEquals("value", tuple._1);
    }

    @Test
    public void testTuple2() {
        BiFunction<String, Integer, Tuple2<String, Integer>> tupleFunc = Fn.tuple2();
        Tuple2<String, Integer> tuple = tupleFunc.apply("first", 123);
        assertEquals("first", tuple._1);
        assertEquals(123, tuple._2);
    }

    @Test
    public void testTuple3() {
        TriFunction<String, Integer, Boolean, Tuple3<String, Integer, Boolean>> tupleFunc = Fn.tuple3();
        Tuple3<String, Integer, Boolean> tuple = tupleFunc.apply("first", 123, true);
        assertEquals("first", tuple._1);
        assertEquals(123, tuple._2);
        assertEquals(true, tuple._3);
    }

    @Test
    public void testTuple4() {
        QuadFunction<String, Integer, Boolean, Double, Tuple4<String, Integer, Boolean, Double>> tupleFunc = Fn.tuple4();
        Tuple4<String, Integer, Boolean, Double> tuple = tupleFunc.apply("first", 123, true, 3.14);
        assertEquals("first", tuple._1);
        assertEquals(123, tuple._2);
        assertEquals(true, tuple._3);
        assertEquals(3.14, tuple._4);
    }

    @Test
    public void testTrim() {
        UnaryOperator<String> trim = Fn.trim();
        assertEquals("hello", trim.apply("  hello  "));
        assertEquals("", trim.apply("   "));
        assertNull(trim.apply(null));
    }

    @Test
    public void testTrimToEmpty() {
        UnaryOperator<String> trimToEmpty = Fn.trimToEmpty();
        assertEquals("hello", trimToEmpty.apply("  hello  "));
        assertEquals("", trimToEmpty.apply("   "));
        assertEquals("", trimToEmpty.apply(null));
    }

    @Test
    public void testTrimToNull() {
        UnaryOperator<String> trimToNull = Fn.trimToNull();
        assertEquals("hello", trimToNull.apply("  hello  "));
        assertNull(trimToNull.apply("   "));
        assertNull(trimToNull.apply(null));
    }

    @Test
    public void testStrip() {
        UnaryOperator<String> strip = Fn.strip();
        assertEquals("hello", strip.apply("  hello  "));
        assertEquals("", strip.apply("   "));
        assertNull(strip.apply(null));
    }

    @Test
    public void testStripToEmpty() {
        UnaryOperator<String> stripToEmpty = Fn.stripToEmpty();
        assertEquals("hello", stripToEmpty.apply("  hello  "));
        assertEquals("", stripToEmpty.apply("   "));
        assertEquals("", stripToEmpty.apply(null));
    }

    @Test
    public void testStripToNull() {
        UnaryOperator<String> stripToNull = Fn.stripToNull();
        assertEquals("hello", stripToNull.apply("  hello  "));
        assertNull(stripToNull.apply("   "));
        assertNull(stripToNull.apply(null));
    }

    @Test
    public void testNullToEmpty() {
        UnaryOperator<String> nullToEmpty = Fn.nullToEmpty();
        assertEquals("hello", nullToEmpty.apply("hello"));
        assertEquals("", nullToEmpty.apply(null));
    }

    @Test
    public void testNullToEmptyList() {
        UnaryOperator<List<String>> nullToEmptyList = Fn.nullToEmptyList();
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, nullToEmptyList.apply(list));
        assertEquals(0, nullToEmptyList.apply(null).size());
    }

    @Test
    public void testNullToEmptySet() {
        UnaryOperator<Set<String>> nullToEmptySet = Fn.nullToEmptySet();
        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        assertSame(set, nullToEmptySet.apply(set));
        assertEquals(0, nullToEmptySet.apply(null).size());
    }

    @Test
    public void testNullToEmptyMap() {
        UnaryOperator<Map<String, Integer>> nullToEmptyMap = Fn.nullToEmptyMap();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, nullToEmptyMap.apply(map));
        assertEquals(0, nullToEmptyMap.apply(null).size());
    }

    @Test
    public void testLen() {
        Function<String[], Integer> len = Fn.len();
        assertEquals(3, len.apply(new String[] { "a", "b", "c" }));
        assertEquals(0, len.apply(new String[] {}));
        assertEquals(0, len.apply(null));
    }

    @Test
    public void testLength() {
        Function<String, Integer> length = Fn.length();
        assertEquals(5, length.apply("hello"));
        assertEquals(0, length.apply(""));
        assertEquals(0, length.apply(null));
    }

    @Test
    public void testSize() {
        Function<List<String>, Integer> size = Fn.size();
        assertEquals(3, size.apply(Arrays.asList("a", "b", "c")));
        assertEquals(0, size.apply(new ArrayList<>()));
        assertEquals(0, size.apply(null));
    }

    @Test
    public void testSizeM() {
        Function<Map<String, Integer>, Integer> sizeM = Fn.sizeM();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, sizeM.apply(map));
        assertEquals(0, sizeM.apply(new HashMap<>()));
        assertEquals(0, sizeM.apply(null));
    }

    @Test
    public void testCast() {
        Function<Object, String> cast = Fn.cast(String.class);
        assertEquals("hello", cast.apply("hello"));
        assertThrows(ClassCastException.class, () -> cast.apply(123));
    }

    @Test
    public void testAlwaysTrue() {
        Predicate<Object> alwaysTrue = Fn.alwaysTrue();
        assertTrue(alwaysTrue.test("test"));
        assertTrue(alwaysTrue.test(123));
        assertTrue(alwaysTrue.test(null));
    }

    @Test
    public void testAlwaysFalse() {
        Predicate<Object> alwaysFalse = Fn.alwaysFalse();
        assertFalse(alwaysFalse.test("test"));
        assertFalse(alwaysFalse.test(123));
        assertFalse(alwaysFalse.test(null));
    }

    @Test
    public void testIsNull() {
        Predicate<Object> isNull = Fn.isNull();
        assertTrue(isNull.test(null));
        assertFalse(isNull.test("test"));
        assertFalse(isNull.test(123));
    }

    @Test
    public void testIsNullWithExtractor() {
        Predicate<String> isNull = Fn.isNull(s -> s == null ? null : s.substring(0, 1));
        assertTrue(isNull.test(null));
        assertFalse(isNull.test("test"));
    }

    @Test
    public void testIsEmpty() {
        Predicate<String> isEmpty = Fn.isEmpty();
        assertTrue(isEmpty.test(""));
        assertTrue(isEmpty.test(null));
        assertFalse(isEmpty.test("test"));
    }

    @Test
    public void testIsEmptyWithExtractor() {
        Predicate<Person> isEmpty = Fn.isEmpty(Person::getName);
        assertTrue(isEmpty.test(new Person("")));
        assertTrue(isEmpty.test(new Person(null)));
        assertFalse(isEmpty.test(new Person("John")));
    }

    @Test
    public void testIsBlank() {
        Predicate<String> isBlank = Fn.isBlank();
        assertTrue(isBlank.test(""));
        assertTrue(isBlank.test("   "));
        assertTrue(isBlank.test(null));
        assertFalse(isBlank.test("test"));
    }

    @Test
    public void testIsBlankWithExtractor() {
        Predicate<Person> isBlank = Fn.isBlank(Person::getName);
        assertTrue(isBlank.test(new Person("")));
        assertTrue(isBlank.test(new Person("   ")));
        assertTrue(isBlank.test(new Person(null)));
        assertFalse(isBlank.test(new Person("John")));
    }

    @Test
    public void testIsEmptyA() {
        Predicate<String[]> isEmptyA = Fn.isEmptyA();
        assertTrue(isEmptyA.test(new String[] {}));
        assertTrue(isEmptyA.test(null));
        assertFalse(isEmptyA.test(new String[] { "a" }));
    }

    @Test
    public void testIsEmptyC() {
        Predicate<List<String>> isEmptyC = Fn.isEmptyC();
        assertTrue(isEmptyC.test(new ArrayList<>()));
        assertTrue(isEmptyC.test(null));
        assertFalse(isEmptyC.test(Arrays.asList("a")));
    }

    @Test
    public void testIsEmptyM() {
        Predicate<Map<String, Integer>> isEmptyM = Fn.isEmptyM();
        assertTrue(isEmptyM.test(new HashMap<>()));
        assertTrue(isEmptyM.test(null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(isEmptyM.test(map));
    }

    @Test
    public void testNotNull() {
        Predicate<Object> notNull = Fn.notNull();
        assertFalse(notNull.test(null));
        assertTrue(notNull.test("test"));
        assertTrue(notNull.test(123));
    }

    @Test
    public void testNotNullWithExtractor() {
        Predicate<String> notNull = Fn.notNull(s -> s == null ? null : s.substring(0, 1));
        assertFalse(notNull.test(null));
        assertTrue(notNull.test("test"));
    }

    @Test
    public void testNotEmpty() {
        Predicate<String> notEmpty = Fn.notEmpty();
        assertFalse(notEmpty.test(""));
        assertFalse(notEmpty.test(null));
        assertTrue(notEmpty.test("test"));
    }

    @Test
    public void testNotEmptyWithExtractor() {
        Predicate<Person> notEmpty = Fn.notEmpty(Person::getName);
        assertFalse(notEmpty.test(new Person("")));
        assertFalse(notEmpty.test(new Person(null)));
        assertTrue(notEmpty.test(new Person("John")));
    }

    @Test
    public void testNotBlank() {
        Predicate<String> notBlank = Fn.notBlank();
        assertFalse(notBlank.test(""));
        assertFalse(notBlank.test("   "));
        assertFalse(notBlank.test(null));
        assertTrue(notBlank.test("test"));
    }

    @Test
    public void testNotBlankWithExtractor() {
        Predicate<Person> notBlank = Fn.notBlank(Person::getName);
        assertFalse(notBlank.test(new Person("")));
        assertFalse(notBlank.test(new Person("   ")));
        assertFalse(notBlank.test(new Person(null)));
        assertTrue(notBlank.test(new Person("John")));
    }

    @Test
    public void testNotEmptyA() {
        Predicate<String[]> notEmptyA = Fn.notEmptyA();
        assertFalse(notEmptyA.test(new String[] {}));
        assertFalse(notEmptyA.test(null));
        assertTrue(notEmptyA.test(new String[] { "a" }));
    }

    @Test
    public void testNotEmptyC() {
        Predicate<List<String>> notEmptyC = Fn.notEmptyC();
        assertFalse(notEmptyC.test(new ArrayList<>()));
        assertFalse(notEmptyC.test(null));
        assertTrue(notEmptyC.test(Arrays.asList("a")));
    }

    @Test
    public void testNotEmptyM() {
        Predicate<Map<String, Integer>> notEmptyM = Fn.notEmptyM();
        assertFalse(notEmptyM.test(new HashMap<>()));
        assertFalse(notEmptyM.test(null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(notEmptyM.test(map));
    }

    @Test
    public void testIsFile() {
        Predicate<File> isFile = Fn.isFile();
        File file = new File("test.txt");
        File dir = new File(".");
        assertFalse(isFile.test(file)); // Doesn't exist
        assertFalse(isFile.test(new File(getClass().getResource("/").getFile())));
        assertFalse(isFile.test(null));
    }

    @Test
    public void testIsDirectory() {
        Predicate<File> isDirectory = Fn.isDirectory();
        File dir = new File(".");
        assertTrue(isDirectory.test(dir));
        assertFalse(isDirectory.test(new File("nonexistent.txt")));
        assertFalse(isDirectory.test(null));
    }

    @Test
    public void testEqual() {
        Predicate<String> equal = Fn.equal("test");
        assertTrue(equal.test("test"));
        assertFalse(equal.test("other"));
        assertFalse(equal.test(null));

        Predicate<String> equalNull = Fn.equal(null);
        assertTrue(equalNull.test(null));
        assertFalse(equalNull.test("test"));
    }

    @Test
    public void testEqOr2() {
        Predicate<String> eqOr = Fn.eqOr("test1", "test2");
        assertTrue(eqOr.test("test1"));
        assertTrue(eqOr.test("test2"));
        assertFalse(eqOr.test("test3"));
        assertFalse(eqOr.test(null));
    }

    @Test
    public void testEqOr3() {
        Predicate<String> eqOr = Fn.eqOr("test1", "test2", "test3");
        assertTrue(eqOr.test("test1"));
        assertTrue(eqOr.test("test2"));
        assertTrue(eqOr.test("test3"));
        assertFalse(eqOr.test("test4"));
        assertFalse(eqOr.test(null));
    }

    @Test
    public void testNotEqual() {
        Predicate<String> notEqual = Fn.notEqual("test");
        assertFalse(notEqual.test("test"));
        assertTrue(notEqual.test("other"));
        assertTrue(notEqual.test(null));
    }

    @Test
    public void testGreaterThan() {
        Predicate<Integer> greaterThan = Fn.greaterThan(5);
        assertTrue(greaterThan.test(6));
        assertFalse(greaterThan.test(5));
        assertFalse(greaterThan.test(4));
    }

    @Test
    public void testGreaterEqual() {
        Predicate<Integer> greaterEqual = Fn.greaterEqual(5);
        assertTrue(greaterEqual.test(6));
        assertTrue(greaterEqual.test(5));
        assertFalse(greaterEqual.test(4));
    }

    @Test
    public void testLessThan() {
        Predicate<Integer> lessThan = Fn.lessThan(5);
        assertTrue(lessThan.test(4));
        assertFalse(lessThan.test(5));
        assertFalse(lessThan.test(6));
    }

    @Test
    public void testLessEqual() {
        Predicate<Integer> lessEqual = Fn.lessEqual(5);
        assertTrue(lessEqual.test(4));
        assertTrue(lessEqual.test(5));
        assertFalse(lessEqual.test(6));
    }

    @Test
    public void testGtAndLt() {
        Predicate<Integer> between = Fn.gtAndLt(3, 7);
        assertTrue(between.test(4));
        assertTrue(between.test(5));
        assertTrue(between.test(6));
        assertFalse(between.test(3));
        assertFalse(between.test(7));
    }

    @Test
    public void testGeAndLt() {
        Predicate<Integer> between = Fn.geAndLt(3, 7);
        assertTrue(between.test(3));
        assertTrue(between.test(4));
        assertTrue(between.test(6));
        assertFalse(between.test(2));
        assertFalse(between.test(7));
    }

    @Test
    public void testGeAndLe() {
        Predicate<Integer> between = Fn.geAndLe(3, 7);
        assertTrue(between.test(3));
        assertTrue(between.test(5));
        assertTrue(between.test(7));
        assertFalse(between.test(2));
        assertFalse(between.test(8));
    }

    @Test
    public void testGtAndLe() {
        Predicate<Integer> between = Fn.gtAndLe(3, 7);
        assertTrue(between.test(4));
        assertTrue(between.test(5));
        assertTrue(between.test(7));
        assertFalse(between.test(3));
        assertFalse(between.test(8));
    }

    @Test
    public void testBetween() {
        Predicate<Integer> between = Fn.between(3, 7);
        assertTrue(between.test(4));
        assertTrue(between.test(5));
        assertTrue(between.test(6));
        assertFalse(between.test(3));
        assertFalse(between.test(7));
    }

    @Test
    public void testIn() {
        List<String> collection = Arrays.asList("a", "b", "c");
        Predicate<String> in = Fn.in(collection);
        assertTrue(in.test("a"));
        assertTrue(in.test("b"));
        assertFalse(in.test("d"));

        Predicate<String> inEmpty = Fn.in(new ArrayList<>());
        assertFalse(inEmpty.test("a"));
    }

    @Test
    public void testNotIn() {
        List<String> collection = Arrays.asList("a", "b", "c");
        Predicate<String> notIn = Fn.notIn(collection);
        assertFalse(notIn.test("a"));
        assertFalse(notIn.test("b"));
        assertTrue(notIn.test("d"));

        Predicate<String> notInEmpty = Fn.notIn(new ArrayList<>());
        assertTrue(notInEmpty.test("a"));
    }

    @Test
    public void testInstanceOf() {
        Predicate<Object> instanceOf = Fn.instanceOf(String.class);
        assertTrue(instanceOf.test("test"));
        assertFalse(instanceOf.test(123));
        assertFalse(instanceOf.test(null));
    }

    @Test
    public void testSubtypeOf() {
        Predicate<Class<?>> subtypeOf = Fn.subtypeOf(Number.class);
        assertTrue(subtypeOf.test(Integer.class));
        assertTrue(subtypeOf.test(Double.class));
        assertFalse(subtypeOf.test(String.class));
    }

    @Test
    public void testStartsWith() {
        Predicate<String> startsWith = Fn.startsWith("test");
        assertTrue(startsWith.test("test123"));
        assertFalse(startsWith.test("123test"));
        assertFalse(startsWith.test(null));
    }

    @Test
    public void testEndsWith() {
        Predicate<String> endsWith = Fn.endsWith("test");
        assertTrue(endsWith.test("123test"));
        assertFalse(endsWith.test("test123"));
        assertFalse(endsWith.test(null));
    }

    @Test
    public void testContains() {
        Predicate<String> contains = Fn.contains("test");
        assertTrue(contains.test("123test456"));
        assertFalse(contains.test("123456"));
        assertFalse(contains.test(null));
    }

    @Test
    public void testNotStartsWith() {
        Predicate<String> notStartsWith = Fn.notStartsWith("test");
        assertFalse(notStartsWith.test("test123"));
        assertTrue(notStartsWith.test("123test"));
        assertTrue(notStartsWith.test(null));
    }

    @Test
    public void testNotEndsWith() {
        Predicate<String> notEndsWith = Fn.notEndsWith("test");
        assertFalse(notEndsWith.test("123test"));
        assertTrue(notEndsWith.test("test123"));
        assertTrue(notEndsWith.test(null));
    }

    @Test
    public void testNotContains() {
        Predicate<String> notContains = Fn.notContains("test");
        assertFalse(notContains.test("123test456"));
        assertTrue(notContains.test("123456"));
        assertTrue(notContains.test(null));
    }

    @Test
    public void testMatches() {
        Pattern pattern = Pattern.compile("\\d+");
        Predicate<CharSequence> matches = Fn.matches(pattern);
        assertTrue(matches.test("123"));
        assertTrue(matches.test("456"));
        assertFalse(matches.test("abc"));
    }

    @Test
    public void testEqualBiPredicate() {
        BiPredicate<String, String> equal = Fn.equal();
        assertTrue(equal.test("test", "test"));
        assertFalse(equal.test("test", "other"));
        assertTrue(equal.test(null, null));
        assertFalse(equal.test("test", null));
    }

    @Test
    public void testNotEqualBiPredicate() {
        BiPredicate<String, String> notEqual = Fn.notEqual();
        assertFalse(notEqual.test("test", "test"));
        assertTrue(notEqual.test("test", "other"));
        assertFalse(notEqual.test(null, null));
        assertTrue(notEqual.test("test", null));
    }

    @Test
    public void testGreaterThanBiPredicate() {
        BiPredicate<Integer, Integer> greaterThan = Fn.greaterThan();
        assertTrue(greaterThan.test(6, 5));
        assertFalse(greaterThan.test(5, 5));
        assertFalse(greaterThan.test(4, 5));
    }

    @Test
    public void testGreaterEqualBiPredicate() {
        BiPredicate<Integer, Integer> greaterEqual = Fn.greaterEqual();
        assertTrue(greaterEqual.test(6, 5));
        assertTrue(greaterEqual.test(5, 5));
        assertFalse(greaterEqual.test(4, 5));
    }

    @Test
    public void testLessThanBiPredicate() {
        BiPredicate<Integer, Integer> lessThan = Fn.lessThan();
        assertTrue(lessThan.test(4, 5));
        assertFalse(lessThan.test(5, 5));
        assertFalse(lessThan.test(6, 5));
    }

    @Test
    public void testLessEqualBiPredicate() {
        BiPredicate<Integer, Integer> lessEqual = Fn.lessEqual();
        assertTrue(lessEqual.test(4, 5));
        assertTrue(lessEqual.test(5, 5));
        assertFalse(lessEqual.test(6, 5));
    }

    @Test
    public void testNotPredicate() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> notNull = Fn.not(isNull);
        assertFalse(notNull.test(null));
        assertTrue(notNull.test("test"));
    }

    @Test
    public void testNotBiPredicate() {
        BiPredicate<String, String> equal = Fn.equal();
        BiPredicate<String, String> notEqual = Fn.not(equal);
        assertFalse(notEqual.test("test", "test"));
        assertTrue(notEqual.test("test", "other"));
    }

    @Test
    public void testNotTriPredicate() {
        TriPredicate<String, String, String> allEqual = (a, b, c) -> a.equals(b) && b.equals(c);
        TriPredicate<String, String, String> notAllEqual = Fn.not(allEqual);
        assertFalse(notAllEqual.test("test", "test", "test"));
        assertTrue(notAllEqual.test("test", "test", "other"));
    }

    @Test
    public void testAndBooleanSupplier2() {
        BooleanSupplier and = Fn.and(() -> true, () -> true);
        assertTrue(and.getAsBoolean());

        and = Fn.and(() -> true, () -> false);
        assertFalse(and.getAsBoolean());

        and = Fn.and(() -> false, () -> true);
        assertFalse(and.getAsBoolean());
    }

    @Test
    public void testAndBooleanSupplier3() {
        BooleanSupplier and = Fn.and(() -> true, () -> true, () -> true);
        assertTrue(and.getAsBoolean());

        and = Fn.and(() -> true, () -> false, () -> true);
        assertFalse(and.getAsBoolean());
    }

    @Test
    public void testAndPredicate2() {
        Predicate<Integer> and = Fn.and(i -> i > 0, i -> i < 10);
        assertTrue(and.test(5));
        assertFalse(and.test(-1));
        assertFalse(and.test(15));
    }

    @Test
    public void testAndPredicate3() {
        Predicate<Integer> and = Fn.and(i -> i > 0, i -> i < 10, i -> i % 2 == 0);
        assertTrue(and.test(4));
        assertFalse(and.test(5));
        assertFalse(and.test(-2));
    }

    @Test
    public void testAndPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(i -> i > 0, i -> i < 10, i -> i % 2 == 0);
        Predicate<Integer> and = Fn.and(predicates);
        assertTrue(and.test(4));
        assertFalse(and.test(5));
    }

    @Test
    public void testAndBiPredicate2() {
        BiPredicate<Integer, Integer> and = Fn.and((a, b) -> a > 0, (a, b) -> b > 0);
        assertTrue(and.test(1, 2));
        assertFalse(and.test(-1, 2));
        assertFalse(and.test(1, -2));
    }

    @Test
    public void testAndBiPredicate3() {
        BiPredicate<Integer, Integer> and = Fn.and((a, b) -> a > 0, (a, b) -> b > 0, (a, b) -> a + b < 10);
        assertTrue(and.test(2, 3));
        assertFalse(and.test(5, 6));
    }

    @Test
    public void testAndBiPredicateList() {
        List<BiPredicate<Integer, Integer>> predicates = Arrays.asList((a, b) -> a > 0, (a, b) -> b > 0, (a, b) -> a + b < 10);
        BiPredicate<Integer, Integer> and = Fn.and(predicates);
        assertTrue(and.test(2, 3));
        assertFalse(and.test(5, 6));
    }

    @Test
    public void testOrBooleanSupplier2() {
        BooleanSupplier or = Fn.or(() -> true, () -> false);
        assertTrue(or.getAsBoolean());

        or = Fn.or(() -> false, () -> false);
        assertFalse(or.getAsBoolean());
    }

    @Test
    public void testOrBooleanSupplier3() {
        BooleanSupplier or = Fn.or(() -> false, () -> false, () -> true);
        assertTrue(or.getAsBoolean());

        or = Fn.or(() -> false, () -> false, () -> false);
        assertFalse(or.getAsBoolean());
    }

    @Test
    public void testOrPredicate2() {
        Predicate<Integer> or = Fn.or(i -> i < 0, i -> i > 10);
        assertTrue(or.test(-5));
        assertTrue(or.test(15));
        assertFalse(or.test(5));
    }

    @Test
    public void testOrPredicate3() {
        Predicate<Integer> or = Fn.or(i -> i < 0, i -> i > 10, i -> i == 5);
        assertTrue(or.test(-5));
        assertTrue(or.test(15));
        assertTrue(or.test(5));
        assertFalse(or.test(3));
    }

    @Test
    public void testOrPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(i -> i < 0, i -> i > 10, i -> i == 5);
        Predicate<Integer> or = Fn.or(predicates);
        assertTrue(or.test(-5));
        assertTrue(or.test(15));
        assertTrue(or.test(5));
        assertFalse(or.test(3));
    }

    @Test
    public void testOrBiPredicate2() {
        BiPredicate<Integer, Integer> or = Fn.or((a, b) -> a < 0, (a, b) -> b < 0);
        assertTrue(or.test(-1, 2));
        assertTrue(or.test(1, -2));
        assertFalse(or.test(1, 2));
    }

    @Test
    public void testOrBiPredicate3() {
        BiPredicate<Integer, Integer> or = Fn.or((a, b) -> a < 0, (a, b) -> b < 0, (a, b) -> a + b > 10);
        assertTrue(or.test(-1, 2));
        assertTrue(or.test(6, 6));
        assertFalse(or.test(2, 3));
    }

    @Test
    public void testOrBiPredicateList() {
        List<BiPredicate<Integer, Integer>> predicates = Arrays.asList((a, b) -> a < 0, (a, b) -> b < 0, (a, b) -> a + b > 10);
        BiPredicate<Integer, Integer> or = Fn.or(predicates);
        assertTrue(or.test(-1, 2));
        assertTrue(or.test(6, 6));
        assertFalse(or.test(2, 3));
    }

    @Test
    public void testTestByKey() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Predicate<Map.Entry<String, Integer>> testByKey = Fn.testByKey(k -> k.startsWith("t"));
        assertTrue(testByKey.test(entry));

        entry = new AbstractMap.SimpleEntry<>("other", 123);
        assertFalse(testByKey.test(entry));
    }

    @Test
    public void testTestByValue() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Predicate<Map.Entry<String, Integer>> testByValue = Fn.testByValue(v -> v > 100);
        assertTrue(testByValue.test(entry));

        entry = new AbstractMap.SimpleEntry<>("test", 50);
        assertFalse(testByValue.test(entry));
    }

    @Test
    public void testAcceptByKey() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        List<String> keys = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptByKey = Fn.acceptByKey(keys::add);
        acceptByKey.accept(entry);
        assertEquals(1, keys.size());
        assertEquals("test", keys.get(0));
    }

    @Test
    public void testAcceptByValue() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        List<Integer> values = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptByValue = Fn.acceptByValue(values::add);
        acceptByValue.accept(entry);
        assertEquals(1, values.size());
        assertEquals(123, values.get(0));
    }

    @Test
    public void testApplyByKey() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Function<Map.Entry<String, Integer>, Integer> applyByKey = Fn.applyByKey(String::length);
        assertEquals(4, applyByKey.apply(entry));
    }

    @Test
    public void testApplyByValue() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Function<Map.Entry<String, Integer>, String> applyByValue = Fn.applyByValue(Object::toString);
        assertEquals("123", applyByValue.apply(entry));
    }

    @Test
    public void testMapKey() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, Integer>> mapKey = Fn.mapKey(String::length);
        Map.Entry<Integer, Integer> result = mapKey.apply(entry);
        assertEquals(4, result.getKey());
        assertEquals(123, result.getValue());
    }

    @Test
    public void testMapValue() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Function<Map.Entry<String, Integer>, Map.Entry<String, String>> mapValue = Fn.mapValue(Object::toString);
        Map.Entry<String, String> result = mapValue.apply(entry);
        assertEquals("test", result.getKey());
        assertEquals("123", result.getValue());
    }

    @Test
    public void testTestKeyVal() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 4);
        Predicate<Map.Entry<String, Integer>> testKeyVal = Fn.testKeyVal((k, v) -> k.length() == v);
        assertTrue(testKeyVal.test(entry));

        entry = new AbstractMap.SimpleEntry<>("test", 5);
        assertFalse(testKeyVal.test(entry));
    }

    @Test
    public void testAcceptKeyVal() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        List<String> results = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptKeyVal = Fn.acceptKeyVal((k, v) -> results.add(k + ":" + v));
        acceptKeyVal.accept(entry);
        assertEquals(1, results.size());
        assertEquals("test:123", results.get(0));
    }

    @Test
    public void testApplyKeyVal() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
        Function<Map.Entry<String, Integer>, String> applyKeyVal = Fn.applyKeyVal((k, v) -> k + ":" + v);
        assertEquals("test:123", applyKeyVal.apply(entry));
    }

    @Test
    public void testAcceptIfNotNull() {
        List<String> results = new ArrayList<>();
        Consumer<String> acceptIfNotNull = Fn.acceptIfNotNull(results::add);

        acceptIfNotNull.accept("test");
        assertEquals(1, results.size());

        acceptIfNotNull.accept(null);
        assertEquals(1, results.size()); // Should not add null
    }

    @Test
    public void testAcceptIf() {
        List<Integer> results = new ArrayList<>();
        Consumer<Integer> acceptIf = Fn.acceptIf(i -> i > 5, results::add);

        acceptIf.accept(10);
        assertEquals(1, results.size());

        acceptIf.accept(3);
        assertEquals(1, results.size()); // Should not add 3
    }

    @Test
    public void testAcceptIfOrElse() {
        List<Integer> positive = new ArrayList<>();
        List<Integer> negative = new ArrayList<>();
        Consumer<Integer> acceptIfOrElse = Fn.acceptIfOrElse(i -> i > 0, positive::add, negative::add);

        acceptIfOrElse.accept(5);
        acceptIfOrElse.accept(-3);

        assertEquals(1, positive.size());
        assertEquals(5, positive.get(0));
        assertEquals(1, negative.size());
        assertEquals(-3, negative.get(0));
    }

    @Test
    public void testApplyIfNotNullOrEmpty() {
        Function<String, Collection<Character>> mapper = s -> {
            List<Character> chars = new ArrayList<>();
            for (char c : s.toCharArray()) {
                chars.add(c);
            }
            return chars;
        };

        Function<String, Collection<Character>> func = Fn.applyIfNotNullOrEmpty(mapper);

        Collection<Character> result = func.apply("abc");
        assertEquals(3, result.size());

        result = func.apply(null);
        assertEquals(0, result.size());
    }

    @Test
    public void testApplyIfNotNullOrDefault2() {
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault(String::length, Integer::valueOf, -1);

        assertEquals(5, func.apply("hello"));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault(String::length, i -> i * 2, i -> i + 10, -1);

        assertEquals(20, func.apply("hello")); // length=5, *2=10, +10=20
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault4() {
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault(String::length, i -> i * 2, i -> i + 10, i -> i / 2, -1);

        assertEquals(10, func.apply("hello")); // length=5, *2=10, +10=20, /2=10
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet2() {
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet(String::length, Integer::valueOf, () -> -1);

        assertEquals(5, func.apply("hello"));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet(String::length, i -> i * 2, i -> i + 10, () -> -1);

        assertEquals(20, func.apply("hello"));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet4() {
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet(String::length, i -> i * 2, i -> i + 10, i -> i / 2, () -> -1);

        assertEquals(10, func.apply("hello"));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfOrElseDefault() {
        Function<Integer, String> func = Fn.applyIfOrElseDefault(i -> i > 0, i -> "positive: " + i, "non-positive");

        assertEquals("positive: 5", func.apply(5));
        assertEquals("non-positive", func.apply(-3));
    }

    @Test
    public void testApplyIfOrElseGet() {
        Function<Integer, String> func = Fn.applyIfOrElseGet(i -> i > 0, i -> "positive: " + i, () -> "non-positive");

        assertEquals("positive: 5", func.apply(5));
        assertEquals("non-positive", func.apply(-3));
    }

    @Test
    public void testFlatmapValue() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));

        Function<Map<String, ? extends Collection<Integer>>, List<Map<String, Integer>>> func = Fn.flatmapValue();
        List<Map<String, Integer>> result = func.apply(map);

        assertEquals(2, result.size());
    }

    @Test
    public void testParseByte() {
        ToByteFunction<String> parseByte = Fn.parseByte();
        assertEquals((byte) 123, parseByte.applyAsByte("123"));
        assertEquals((byte) -45, parseByte.applyAsByte("-45"));
    }

    @Test
    public void testParseShort() {
        ToShortFunction<String> parseShort = Fn.parseShort();
        assertEquals((short) 12345, parseShort.applyAsShort("12345"));
        assertEquals((short) -1234, parseShort.applyAsShort("-1234"));
    }

    @Test
    public void testParseInt() {
        ToIntFunction<String> parseInt = Fn.parseInt();
        assertEquals(123456, parseInt.applyAsInt("123456"));
        assertEquals(-123456, parseInt.applyAsInt("-123456"));
    }

    @Test
    public void testParseLong() {
        ToLongFunction<String> parseLong = Fn.parseLong();
        assertEquals(123456789L, parseLong.applyAsLong("123456789"));
        assertEquals(-123456789L, parseLong.applyAsLong("-123456789"));
    }

    @Test
    public void testParseFloat() {
        ToFloatFunction<String> parseFloat = Fn.parseFloat();
        assertEquals(123.45f, parseFloat.applyAsFloat("123.45"), 0.001f);
        assertEquals(-123.45f, parseFloat.applyAsFloat("-123.45"), 0.001f);
    }

    @Test
    public void testParseDouble() {
        ToDoubleFunction<String> parseDouble = Fn.parseDouble();
        assertEquals(123.456, parseDouble.applyAsDouble("123.456"), 0.001);
        assertEquals(-123.456, parseDouble.applyAsDouble("-123.456"), 0.001);
    }

    @Test
    public void testCreateNumber() {
        Function<String, Number> createNumber = Fn.createNumber();
        assertNull(createNumber.apply(""));
        assertEquals(123, createNumber.apply("123"));
        assertEquals(123.456, createNumber.apply("123.456"));
    }

    @Test
    public void testNumToInt() {
        ToIntFunction<Number> numToInt = Fn.numToInt();
        assertEquals(123, numToInt.applyAsInt(123.456));
        assertEquals(456, numToInt.applyAsInt(456L));
    }

    @Test
    public void testNumToLong() {
        ToLongFunction<Number> numToLong = Fn.numToLong();
        assertEquals(123L, numToLong.applyAsLong(123.456));
        assertEquals(456L, numToLong.applyAsLong(456));
    }

    @Test
    public void testNumToDouble() {
        ToDoubleFunction<Number> numToDouble = Fn.numToDouble();
        assertEquals(123.456, numToDouble.applyAsDouble(123.456f), 0.001);
        assertEquals(456.0, numToDouble.applyAsDouble(456), 0.001);
    }

    @Test
    public void testAtMost() {
        Predicate<String> atMost = Fn.atMost(3);
        assertTrue(atMost.test("1"));
        assertTrue(atMost.test("2"));
        assertTrue(atMost.test("3"));
        assertFalse(atMost.test("4"));
        assertFalse(atMost.test("5"));
    }

    @Test
    public void testLimitThenFilterPredicate() {
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<Integer> limitThenFilter = Fn.limitThenFilter(3, i -> {
            counter.incrementAndGet();
            return i > 5;
        });

        assertFalse(limitThenFilter.test(3)); // Fails predicate
        assertTrue(limitThenFilter.test(7)); // Passes
        assertTrue(limitThenFilter.test(8)); // Passes
        assertFalse(limitThenFilter.test(10)); // Limit reached

        assertEquals(3, counter.get()); // Predicate only called 3 times
    }

    @Test
    public void testLimitThenFilterBiPredicate() {
        AtomicInteger counter = new AtomicInteger(0);
        BiPredicate<Integer, Integer> limitThenFilter = Fn.limitThenFilter(2, (a, b) -> {
            counter.incrementAndGet();
            return a + b > 10;
        });

        assertFalse(limitThenFilter.test(3, 5)); // Sum = 8, fails
        assertTrue(limitThenFilter.test(7, 6)); // Sum = 13, passes
        assertFalse(limitThenFilter.test(8, 8)); // Limit reached

        assertEquals(2, counter.get());
    }

    @Test
    public void testFilterThenLimitPredicate() {
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<Integer> filterThenLimit = Fn.filterThenLimit(i -> {
            counter.incrementAndGet();
            return i > 5;
        }, 2);

        assertFalse(filterThenLimit.test(3)); // Fails predicate
        assertTrue(filterThenLimit.test(7)); // Passes
        assertTrue(filterThenLimit.test(8)); // Passes
        assertFalse(filterThenLimit.test(10)); // Passes predicate but limit reached

        assertEquals(4, counter.get()); // Predicate called for all 4 tests
    }

    @Test
    public void testFilterThenLimitBiPredicate() {
        AtomicInteger counter = new AtomicInteger(0);
        BiPredicate<Integer, Integer> filterThenLimit = Fn.filterThenLimit((a, b) -> {
            counter.incrementAndGet();
            return a + b > 10;
        }, 2);

        assertFalse(filterThenLimit.test(3, 5)); // Sum = 8, fails
        assertTrue(filterThenLimit.test(7, 6)); // Sum = 13, passes
        assertTrue(filterThenLimit.test(8, 5)); // Sum = 13, passes
        assertFalse(filterThenLimit.test(8, 8)); // Sum = 16, passes but limit reached

        assertEquals(4, counter.get());
    }

    @Test
    public void testTimeLimitMillis() throws InterruptedException {
        Predicate<String> timeLimit = Fn.timeLimit(100);

        assertTrue(timeLimit.test("1"));
        Thread.sleep(50);
        assertTrue(timeLimit.test("2"));
        Thread.sleep(80); // Total > 100ms
        assertFalse(timeLimit.test("3"));
    }

    @Test
    public void testTimeLimitDuration() throws InterruptedException {
        Predicate<String> timeLimit = Fn.timeLimit(Duration.ofMillis(100));

        assertTrue(timeLimit.test("1"));
        Thread.sleep(50);
        assertTrue(timeLimit.test("2"));
        Thread.sleep(80); // Total > 100ms
        assertFalse(timeLimit.test("3"));
    }

    @Test
    public void testIndexed() {
        Function<String, Indexed<String>> indexed = Fn.indexed();

        Indexed<String> first = indexed.apply("a");
        assertEquals("a", first.value());
        assertEquals(0, first.index());

        Indexed<String> second = indexed.apply("b");
        assertEquals("b", second.value());
        assertEquals(1, second.index());
    }

    @Test
    public void testIndexedPredicate() {
        Predicate<String> indexed = Fn.indexed((index, value) -> index < 2 && value.startsWith("t"));

        assertTrue(indexed.test("test")); // index 0
        assertTrue(indexed.test("two")); // index 1
        assertFalse(indexed.test("three")); // index 2
        assertFalse(indexed.test("one")); // index 3, doesn't start with 't'
    }

    @Test
    public void testSelectFirst() {
        BinaryOperator<String> selectFirst = Fn.selectFirst();
        assertEquals("first", selectFirst.apply("first", "second"));
        assertEquals("a", selectFirst.apply("a", "b"));
    }

    @Test
    public void testSelectSecond() {
        BinaryOperator<String> selectSecond = Fn.selectSecond();
        assertEquals("second", selectSecond.apply("first", "second"));
        assertEquals("b", selectSecond.apply("a", "b"));
    }

    @Test
    public void testMin() {
        BinaryOperator<Integer> min = Fn.min();
        assertEquals(3, min.apply(5, 3));
        assertEquals(3, min.apply(3, 5));
        assertEquals(3, min.apply(3, 3));
    }

    @Test
    public void testMinWithComparator() {
        BinaryOperator<String> min = Fn.min(Comparator.comparingInt(String::length));
        assertEquals("hi", min.apply("hello", "hi"));
        assertEquals("hi", min.apply("hi", "hello"));
    }

    @Test
    public void testMinBy() {
        BinaryOperator<String> minBy = Fn.minBy(String::length);
        assertEquals("hi", minBy.apply("hello", "hi"));
        assertEquals("hi", minBy.apply("hi", "hello"));
    }

    @Test
    public void testMinByKey() {
        BinaryOperator<Map.Entry<Integer, String>> minByKey = Fn.minByKey();
        Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(5, "five");
        Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(3, "three");
        assertEquals(entry2, minByKey.apply(entry1, entry2));
    }

    @Test
    public void testMinByValue() {
        BinaryOperator<Map.Entry<String, Integer>> minByValue = Fn.minByValue();
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("five", 5);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("three", 3);
        assertEquals(entry2, minByValue.apply(entry1, entry2));
    }

    @Test
    public void testMax() {
        BinaryOperator<Integer> max = Fn.max();
        assertEquals(5, max.apply(5, 3));
        assertEquals(5, max.apply(3, 5));
        assertEquals(5, max.apply(5, 5));
    }

    @Test
    public void testMaxWithComparator() {
        BinaryOperator<String> max = Fn.max(Comparator.comparingInt(String::length));
        assertEquals("hello", max.apply("hello", "hi"));
        assertEquals("hello", max.apply("hi", "hello"));
    }

    @Test
    public void testMaxBy() {
        BinaryOperator<String> maxBy = Fn.maxBy(String::length);
        assertEquals("hello", maxBy.apply("hello", "hi"));
        assertEquals("hello", maxBy.apply("hi", "hello"));
    }

    @Test
    public void testMaxByKey() {
        BinaryOperator<Map.Entry<Integer, String>> maxByKey = Fn.maxByKey();
        Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(5, "five");
        Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(3, "three");
        assertEquals(entry1, maxByKey.apply(entry1, entry2));
    }

    @Test
    public void testMaxByValue() {
        BinaryOperator<Map.Entry<String, Integer>> maxByValue = Fn.maxByValue();
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("five", 5);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("three", 3);
        assertEquals(entry1, maxByValue.apply(entry1, entry2));
    }

    @Test
    public void testCompareTo() {
        Function<Integer, Integer> compareTo = Fn.compareTo(5);
        assertTrue(compareTo.apply(3) < 0); // 3 < 5
        assertEquals(0, compareTo.apply(5)); // 5 == 5
        assertTrue(compareTo.apply(7) > 0); // 7 > 5
    }

    @Test
    public void testCompareToWithComparator() {
        Function<String, Integer> compareTo = Fn.compareTo("hello", Comparator.comparingInt(String::length));
        assertTrue(compareTo.apply("hi") < 0); // length 2 < 5
        assertEquals(0, compareTo.apply("world")); // length 5 == 5
        assertTrue(compareTo.apply("goodbye") > 0); // length 7 > 5
    }

    @Test
    public void testCompare() {
        BiFunction<Integer, Integer, Integer> compare = Fn.compare();
        assertTrue(compare.apply(3, 5) < 0);
        assertEquals(0, compare.apply(5, 5));
        assertTrue(compare.apply(7, 5) > 0);
    }

    @Test
    public void testCompareWithComparator() {
        BiFunction<String, String, Integer> compare = Fn.compare(Comparator.comparingInt(String::length));
        assertTrue(compare.apply("hi", "hello") < 0);
        assertEquals(0, compare.apply("hello", "world"));
        assertTrue(compare.apply("goodbye", "hello") > 0);
    }

    @Test
    public void testFutureGetOrDefaultOnError() throws ExecutionException, InterruptedException {
        Function<Future<String>, String> futureGet = Fn.futureGetOrDefaultOnError("default");

        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        assertEquals("success", futureGet.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertEquals("default", futureGet.apply(failedFuture));
    }

    @Test
    public void testFutureGet() throws ExecutionException, InterruptedException {
        Function<Future<String>, String> futureGet = Fn.futureGet();

        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        assertEquals("success", futureGet.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertThrows(RuntimeException.class, () -> futureGet.apply(failedFuture));
    }

    @Test
    public void testFromSupplier() {
        java.util.function.Supplier<String> jdkSupplier = () -> "test";
        Supplier<String> abacusSupplier = Fn.from(jdkSupplier);
        assertEquals("test", abacusSupplier.get());

        // Test when already an abacus Supplier
        Supplier<String> supplier = () -> "test";
        assertSame(supplier, Fn.from(supplier));
    }

    @Test
    public void testFromIntFunction() {
        java.util.function.IntFunction<String> jdkFunc = i -> "value: " + i;
        IntFunction<String> abacusFunc = Fn.from(jdkFunc);
        assertEquals("value: 5", abacusFunc.apply(5));
    }

    @Test
    public void testFromPredicate() {
        java.util.function.Predicate<String> jdkPredicate = s -> s.length() > 3;
        Predicate<String> abacusPredicate = Fn.from(jdkPredicate);
        assertTrue(abacusPredicate.test("hello"));
        assertFalse(abacusPredicate.test("hi"));
    }

    @Test
    public void testFromBiPredicate() {
        java.util.function.BiPredicate<String, Integer> jdkBiPredicate = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> abacusBiPredicate = Fn.from(jdkBiPredicate);
        assertTrue(abacusBiPredicate.test("hello", 5));
        assertFalse(abacusBiPredicate.test("hello", 3));
    }

    @Test
    public void testFromConsumer() {
        List<String> results = new ArrayList<>();
        java.util.function.Consumer<String> jdkConsumer = results::add;
        Consumer<String> abacusConsumer = Fn.from(jdkConsumer);
        abacusConsumer.accept("test");
        assertEquals(1, results.size());
        assertEquals("test", results.get(0));
    }

    @Test
    public void testFromBiConsumer() {
        List<String> results = new ArrayList<>();
        java.util.function.BiConsumer<String, Integer> jdkBiConsumer = (s, i) -> results.add(s + ":" + i);
        BiConsumer<String, Integer> abacusBiConsumer = Fn.from(jdkBiConsumer);
        abacusBiConsumer.accept("test", 5);
        assertEquals(1, results.size());
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testFromFunction() {
        java.util.function.Function<String, Integer> jdkFunction = String::length;
        Function<String, Integer> abacusFunction = Fn.from(jdkFunction);
        assertEquals(5, abacusFunction.apply("hello"));
    }

    @Test
    public void testFromBiFunction() {
        java.util.function.BiFunction<String, Integer, String> jdkBiFunction = (s, i) -> s + ":" + i;
        BiFunction<String, Integer, String> abacusBiFunction = Fn.from(jdkBiFunction);
        assertEquals("test:5", abacusBiFunction.apply("test", 5));
    }

    @Test
    public void testFromUnaryOperator() {
        java.util.function.UnaryOperator<String> jdkOp = s -> s.toUpperCase();
        UnaryOperator<String> abacusOp = Fn.from(jdkOp);
        assertEquals("HELLO", abacusOp.apply("hello"));
    }

    @Test
    public void testFromBinaryOperator() {
        java.util.function.BinaryOperator<Integer> jdkOp = Integer::sum;
        BinaryOperator<Integer> abacusOp = Fn.from(jdkOp);
        assertEquals(8, abacusOp.apply(3, 5));
    }

    @Test
    public void testS() {
        Supplier<String> supplier = () -> "test";
        assertSame(supplier, Fn.s(supplier));
    }

    @Test
    public void testSWithArgument() {
        Supplier<Integer> supplier = Fn.s("hello", String::length);
        assertEquals(5, supplier.get());
    }

    @Test
    public void testPPredicate() {
        Predicate<String> predicate = s -> s.length() > 3;
        assertSame(predicate, Fn.p(predicate));
    }

    @Test
    public void testPPredicateWithArgument() {
        Predicate<String> predicate = Fn.p("test", String::equals);
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testPPredicateWithTwoArguments() {
        Predicate<Integer> predicate = Fn.p(1, 10, (min, max, val) -> val >= min && val <= max);
        assertTrue(predicate.test(5));
        assertFalse(predicate.test(0));
        assertFalse(predicate.test(15));
    }

    @Test
    public void testPBiPredicate() {
        BiPredicate<String, Integer> biPredicate = (s, i) -> s.length() == i;
        assertSame(biPredicate, Fn.p(biPredicate));
    }

    @Test
    public void testPBiPredicateWithArgument() {
        BiPredicate<Integer, Integer> biPredicate = Fn.p(10, (base, a, b) -> a + b > base);
        assertTrue(biPredicate.test(6, 5));
        assertFalse(biPredicate.test(3, 4));
    }

    @Test
    public void testPTriPredicate() {
        TriPredicate<String, Integer, Boolean> triPredicate = (s, i, b) -> b && s.length() == i;
        assertSame(triPredicate, Fn.p(triPredicate));
    }

    @Test
    public void testCConsumer() {
        Consumer<String> consumer = System.out::println;
        assertSame(consumer, Fn.c(consumer));
    }

    @Test
    public void testCConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.c(results, (result, val) -> result.add(val));
        consumer.accept("test");
        assertEquals(1, results.size());
        assertEquals("test", results.get(0));
    }

    @Test
    public void testCConsumerWithTwoArguments() {
        List<String> results = new ArrayList<>();
        Consumer<Integer> consumer = Fn.c("prefix-", results, (prefix, list, val) -> list.add(prefix + val));
        consumer.accept(123);
        assertEquals(1, results.size());
        assertEquals("prefix-123", results.get(0));
    }

    @Test
    public void testCBiConsumer() {
        BiConsumer<String, Integer> biConsumer = (s, i) -> System.out.println(s + ":" + i);
        assertSame(biConsumer, Fn.c(biConsumer));
    }

    @Test
    public void testCBiConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.c(results, (list, s, i) -> list.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testCTriConsumer() {
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> System.out.println(s + ":" + i + ":" + b);
        assertSame(triConsumer, Fn.c(triConsumer));
    }

    @Test
    public void testFFunction() {
        Function<String, Integer> function = String::length;
        assertSame(function, Fn.f(function));
    }

    @Test
    public void testFFunctionWithArgument() {
        Function<Integer, String> function = Fn.f("test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testFFunctionWithTwoArguments() {
        Function<Boolean, String> function = Fn.f("hello", 5, (s, i, b) -> b ? s : String.valueOf(i));
        assertEquals("hello", function.apply(true));
        assertEquals("5", function.apply(false));
    }

    @Test
    public void testFBiFunction() {
        BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
        assertSame(biFunction, Fn.f(biFunction));
    }

    @Test
    public void testFBiFunctionWithArgument() {
        BiFunction<String, Integer, String> biFunction = Fn.f("-", (sep, s, i) -> s + sep + i);
        assertEquals("test-5", biFunction.apply("test", 5));
    }

    @Test
    public void testFTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunction = (s, i, b) -> s + ":" + i + ":" + b;
        assertSame(triFunction, Fn.f(triFunction));
    }

    @Test
    public void testOUnaryOperator() {
        UnaryOperator<String> op = s -> s.toUpperCase();
        assertSame(op, Fn.o(op));
    }

    @Test
    public void testOBinaryOperator() {
        BinaryOperator<Integer> op = Integer::sum;
        assertSame(op, Fn.o(op));
    }

    @Test
    public void testSs() {
        Supplier<String> supplier = Fn.ss(() -> "test");
        assertEquals("test", supplier.get());

        Supplier<String> throwingSupplier = Fn.ss(() -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingSupplier::get);
    }

    @Test
    public void testSsWithArgument() {
        Supplier<Integer> supplier = Fn.ss("hello", s -> s.length());
        assertEquals(5, supplier.get());

        Supplier<Integer> throwingSupplier = Fn.ss("hello", s -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingSupplier::get);
    }

    @Test
    public void testPp() {
        Predicate<String> predicate = Fn.pp(s -> s.length() > 3);
        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));

        Predicate<String> throwingPredicate = Fn.pp(s -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, () -> throwingPredicate.test("test"));
    }

    @Test
    public void testPpWithArgument() {
        Predicate<String> predicate = Fn.pp("test", (ref, s) -> ref.equals(s));
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testPpWithTwoArguments() {
        Predicate<Integer> predicate = Fn.pp(1, 10, (min, max, val) -> val >= min && val <= max);
        assertTrue(predicate.test(5));
        assertFalse(predicate.test(0));
    }

    @Test
    public void testPpBiPredicate() {
        BiPredicate<String, Integer> biPredicate = Fn.pp((s, i) -> s.length() == i);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("hello", 3));
    }

    @Test
    public void testPpBiPredicateWithArgument() {
        BiPredicate<Integer, Integer> biPredicate = Fn.pp(10, (base, a, b) -> a + b > base);
        assertTrue(biPredicate.test(6, 5));
        assertFalse(biPredicate.test(3, 4));
    }

    @Test
    public void testPpTriPredicate() {
        TriPredicate<String, Integer, Boolean> triPredicate = Fn.pp((s, i, b) -> b && s.length() == i);
        assertTrue(triPredicate.test("hello", 5, true));
        assertFalse(triPredicate.test("hello", 5, false));
    }

    @Test
    public void testCc() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.cc(e -> results.add(e));
        consumer.accept("test");
        assertEquals(1, results.size());

        Consumer<String> throwingConsumer = Fn.cc(s -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, () -> throwingConsumer.accept("test"));
    }

    @Test
    public void testCcWithArgument() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.cc(results, (list, s) -> list.add(s));
        consumer.accept("test");
        assertEquals(1, results.size());
    }

    @Test
    public void testCcWithTwoArguments() {
        List<String> results = new ArrayList<>();
        Consumer<Integer> consumer = Fn.cc("prefix-", results, (prefix, list, val) -> list.add(prefix + val));
        consumer.accept(123);
        assertEquals(1, results.size());
        assertEquals("prefix-123", results.get(0));
    }

    @Test
    public void testCcBiConsumer() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.cc((s, i) -> results.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testCcBiConsumerWithArgument() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.cc(results, (list, s, i) -> list.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
    }

    @Test
    public void testCcTriConsumer() {
        List<String> results = new ArrayList<>();
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.cc((s, i, b) -> results.add(s + ":" + i + ":" + b));
        triConsumer.accept("test", 5, true);
        assertEquals(1, results.size());
        assertEquals("test:5:true", results.get(0));
    }

    @Test
    public void testFf() {
        Function<String, Integer> function = Fn.ff(String::length);
        assertEquals(5, function.apply("hello"));

        Function<String, Integer> throwingFunction = Fn.ff(s -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, () -> throwingFunction.apply("test"));
    }

    @Test
    public void testFfWithDefault() {
        Function<String, Integer> function = Fn.ff(s -> {
            if (s == null)
                throw new Exception("null");
            return s.length();
        }, -1);

        assertEquals(5, function.apply("hello"));
        assertEquals(-1, function.apply(null));
    }

    @Test
    public void testFfWithArgument() {
        Function<Integer, String> function = Fn.ff("test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testFfWithTwoArguments() {
        Function<Boolean, String> function = Fn.ff("hello", 5, (s, i, b) -> b ? s : String.valueOf(i));
        assertEquals("hello", function.apply(true));
        assertEquals("5", function.apply(false));
    }

    @Test
    public void testFfBiFunction() {
        BiFunction<String, Integer, String> biFunction = Fn.ff((s, i) -> s + ":" + i);
        assertEquals("test:5", biFunction.apply("test", 5));
    }

    @Test
    public void testFfBiFunctionWithDefault() {
        BiFunction<String, Integer, String> biFunction = Fn.ff((s, i) -> {
            if (s == null)
                throw new Exception("null");
            return s + ":" + i;
        }, "default");

        assertEquals("test:5", biFunction.apply("test", 5));
        assertEquals("default", biFunction.apply(null, 5));
    }

    @Test
    public void testFfBiFunctionWithArgument() {
        BiFunction<String, Integer, String> biFunction = Fn.ff("-", (sep, s, i) -> s + sep + i);
        assertEquals("test-5", biFunction.apply("test", 5));
    }

    @Test
    public void testFfTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff((s, i, b) -> s + ":" + i + ":" + b);
        assertEquals("test:5:true", triFunction.apply("test", 5, true));
    }

    @Test
    public void testFfTriFunctionWithDefault() {
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff((s, i, b) -> {
            if (s == null)
                throw new Exception("null");
            return s + ":" + i + ":" + b;
        }, "default");

        assertEquals("test:5:true", triFunction.apply("test", 5, true));
        assertEquals("default", triFunction.apply(null, 5, true));
    }

    @Test
    public void testSpPredicate() {
        Object mutex = new Object();
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<String> predicate = Fn.sp(mutex, s -> {
            counter.incrementAndGet();
            return s.length() > 3;
        });

        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));
        assertEquals(2, counter.get());
    }

    @Test
    public void testSpPredicateWithArgument() {
        Object mutex = new Object();
        Predicate<String> predicate = Fn.sp(mutex, "test", String::equals);
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
    }

    @Test
    public void testSpBiPredicate() {
        Object mutex = new Object();
        BiPredicate<String, Integer> biPredicate = Fn.sp(mutex, (s, i) -> s.length() == i);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("hello", 3));
    }

    @Test
    public void testSpTriPredicate() {
        Object mutex = new Object();
        TriPredicate<String, Integer, Boolean> triPredicate = Fn.sp(mutex, (s, i, b) -> b && s.length() == i);
        assertTrue(triPredicate.test("hello", 5, true));
        assertFalse(triPredicate.test("hello", 5, false));
    }

    @Test
    public void testScConsumer() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.sc(mutex, e -> results.add(e));
        consumer.accept("test");
        assertEquals(1, results.size());
    }

    @Test
    public void testScConsumerWithArgument() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = Fn.sc(mutex, results, List::add);
        consumer.accept("test");
        assertEquals(1, results.size());
    }

    @Test
    public void testScBiConsumer() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> biConsumer = Fn.sc(mutex, (s, i) -> results.add(s + ":" + i));
        biConsumer.accept("test", 5);
        assertEquals(1, results.size());
    }

    @Test
    public void testSpTriConsumer() {
        Object mutex = new Object();
        List<String> results = new ArrayList<>();
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.sc(mutex, (s, i, b) -> results.add(s + ":" + i + ":" + b));
        triConsumer.accept("test", 5, true);
        assertEquals(1, results.size());
    }

    @Test
    public void testSfFunction() {
        Object mutex = new Object();
        Function<String, Integer> function = Fn.sf(mutex, String::length);
        assertEquals(5, function.apply("hello"));
    }

    @Test
    public void testSfFunctionWithArgument() {
        Object mutex = new Object();
        Function<Integer, String> function = Fn.sf(mutex, "test", (str, i) -> str + ":" + i);
        assertEquals("test:5", function.apply(5));
    }

    @Test
    public void testSfBiFunction() {
        Object mutex = new Object();
        BiFunction<String, Integer, String> biFunction = Fn.sf(mutex, (s, i) -> s + ":" + i);
        assertEquals("test:5", biFunction.apply("test", 5));
    }

    @Test
    public void testSfTriFunction() {
        Object mutex = new Object();
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.sf(mutex, (s, i, b) -> s + ":" + i + ":" + b);
        assertEquals("test:5:true", triFunction.apply("test", 5, true));
    }

    @Test
    public void testC2f() {
        List<String> results = new ArrayList<>();
        Function<String, Void> function = Fn.c2f(e -> results.add(e));
        assertNull(function.apply("test"));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fWithReturn() {
        List<String> results = new ArrayList<>();
        Function<String, Integer> function = Fn.c2f(e -> results.add(e), 123);
        assertEquals(123, function.apply("test"));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fBiConsumer() {
        List<String> results = new ArrayList<>();
        BiFunction<String, Integer, Void> biFunction = Fn.c2f((s, i) -> results.add(s + ":" + i));
        assertNull(biFunction.apply("test", 5));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fBiConsumerWithReturn() {
        List<String> results = new ArrayList<>();
        BiFunction<String, Integer, String> biFunction = Fn.c2f((s, i) -> results.add(s + ":" + i), "done");
        assertEquals("done", biFunction.apply("test", 5));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fTriConsumer() {
        List<String> results = new ArrayList<>();
        TriFunction<String, Integer, Boolean, Void> triFunction = Fn.c2f((s, i, b) -> results.add(s + ":" + i + ":" + b));
        assertNull(triFunction.apply("test", 5, true));
        assertEquals(1, results.size());
    }

    @Test
    public void testC2fTriConsumerWithReturn() {
        List<String> results = new ArrayList<>();
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.c2f((s, i, b) -> results.add(s + ":" + i + ":" + b), "done");
        assertEquals("done", triFunction.apply("test", 5, true));
        assertEquals(1, results.size());
    }

    @Test
    public void testF2c() {
        Consumer<String> consumer = Fn.f2c(String::length);
        assertDoesNotThrow(() -> consumer.accept("test"));
    }

    @Test
    public void testF2cBiFunction() {
        BiConsumer<String, Integer> biConsumer = Fn.f2c((s, i) -> s + ":" + i);
        assertDoesNotThrow(() -> biConsumer.accept("test", 5));
    }

    @Test
    public void testF2cTriFunction() {
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.f2c((s, i, b) -> s + ":" + i + ":" + b);
        assertDoesNotThrow(() -> triConsumer.accept("test", 5, true));
    }

    @Test
    public void testRr() {
        AtomicInteger counter = new AtomicInteger(0);
        Runnable runnable = Fn.rr(() -> counter.incrementAndGet());
        runnable.run();
        assertEquals(1, counter.get());

        Runnable throwingRunnable = Fn.rr(() -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingRunnable::run);
    }

    @Test
    public void testCcCallable() {
        Callable<String> callable = Fn.cc(() -> "test");
        assertEquals("test", callable.call());

        Callable<String> throwingCallable = Fn.cc(() -> {
            throw new Exception("error");
        });
        assertThrows(RuntimeException.class, throwingCallable::call);
    }

    @Test
    public void testR() {
        Runnable runnable = () -> {
        };
        assertSame(runnable, Fn.r(runnable));
    }

    @Test
    public void testC() {
        Callable<String> callable = () -> "test";
        assertSame(callable, Fn.c(callable));
    }

    @Test
    public void testJr() {
        java.lang.Runnable runnable = () -> {
        };
        assertSame(runnable, Fn.jr(runnable));
    }

    @Test
    public void testJc() {
        java.util.concurrent.Callable<String> callable = () -> "test";
        assertSame(callable, Fn.jc(callable));
    }

    @Test
    public void testR2c() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Callable<Void> callable = Fn.r2c(() -> counter.incrementAndGet());
        assertNull(callable.call());
        assertEquals(1, counter.get());
    }

    @Test
    public void testR2cWithReturn() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Callable<String> callable = Fn.r2c(() -> counter.incrementAndGet(), "done");
        assertEquals("done", callable.call());
        assertEquals(1, counter.get());
    }

    @Test
    public void testC2r() {
        Runnable runnable = Fn.c2r(() -> "test");
        assertDoesNotThrow(runnable::run);
    }

    @Test
    public void testJr2r() {
        java.lang.Runnable javaRunnable = () -> {
        };
        Runnable abacusRunnable = Fn.jr2r(javaRunnable);
        assertDoesNotThrow(abacusRunnable::run);

        // Test when already an abacus Runnable
        Runnable runnable = () -> {
        };
        assertSame(runnable, Fn.jr2r(runnable));
    }

    @Test
    public void testJc2c() throws Exception {
        java.util.concurrent.Callable<String> javaCallable = () -> "test";
        Callable<String> abacusCallable = Fn.jc2c(javaCallable);
        assertEquals("test", abacusCallable.call());

        // Test when already an abacus Callable
        Callable<String> callable = () -> "test";
        assertSame(callable, Fn.jc2c(callable));
    }

    @Test
    public void testJc2r() {
        java.util.concurrent.Callable<String> callable = () -> "test";
        Runnable runnable = Fn.jc2r(callable);
        assertDoesNotThrow(runnable::run);

        java.util.concurrent.Callable<String> throwingCallable = () -> {
            throw new Exception("error");
        };
        Runnable throwingRunnable = Fn.jc2r(throwingCallable);
        assertThrows(RuntimeException.class, throwingRunnable::run);
    }

    @Test
    public void testThrowingMerger() {
        BinaryOperator<String> merger = Fn.throwingMerger();
        assertThrows(IllegalStateException.class, () -> merger.apply("a", "b"));
    }

    @Test
    public void testIgnoringMerger() {
        BinaryOperator<String> merger = Fn.ignoringMerger();
        assertEquals("first", merger.apply("first", "second"));
    }

    @Test
    public void testReplacingMerger() {
        BinaryOperator<String> merger = Fn.replacingMerger();
        assertEquals("second", merger.apply("first", "second"));
    }

    @Test
    public void testGetIfPresentOrElseNull() {
        Function<Optional<String>, String> func = Fn.getIfPresentOrElseNull();
        assertEquals("value", func.apply(Optional.of("value")));
        assertNull(func.apply(Optional.empty()));
    }

    @Test
    public void testGetIfPresentOrElseNullJdk() {
        Function<java.util.Optional<String>, String> func = Fn.getIfPresentOrElseNullJdk();
        assertEquals("value", func.apply(java.util.Optional.of("value")));
        assertNull(func.apply(java.util.Optional.empty()));
    }

    @Test
    public void testIsPresent() {
        Predicate<Optional<String>> predicate = Fn.isPresent();
        assertTrue(predicate.test(Optional.of("value")));
        assertFalse(predicate.test(Optional.empty()));
    }

    @Test
    public void testIsPresentJdk() {
        Predicate<java.util.Optional<String>> predicate = Fn.isPresentJdk();
        assertTrue(predicate.test(java.util.Optional.of("value")));
        assertFalse(predicate.test(java.util.Optional.empty()));
    }

    @Test
    public void testAlternate() {
        BiFunction<String, String, MergeResult> alternate = Fn.alternate();
        assertEquals(MergeResult.TAKE_FIRST, alternate.apply("a", "b"));
        assertEquals(MergeResult.TAKE_SECOND, alternate.apply("c", "d"));
        assertEquals(MergeResult.TAKE_FIRST, alternate.apply("e", "f"));
    }

    @Test
    public void testPublicStaticFieldsAsGetters() {
        // Test GET_AS_BOOLEAN
        assertEquals(true, Fn.GET_AS_BOOLEAN.applyAsBoolean(OptionalBoolean.of(true)));

        // Test GET_AS_CHAR
        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(OptionalChar.of('a')));

        // Test GET_AS_BYTE
        assertEquals((byte) 5, Fn.GET_AS_BYTE.applyAsByte(OptionalByte.of((byte) 5)));

        // Test GET_AS_SHORT
        assertEquals((short) 10, Fn.GET_AS_SHORT.applyAsShort(OptionalShort.of((short) 10)));

        // Test GET_AS_INT
        assertEquals(100, Fn.GET_AS_INT.applyAsInt(OptionalInt.of(100)));

        // Test GET_AS_LONG
        assertEquals(1000L, Fn.GET_AS_LONG.applyAsLong(OptionalLong.of(1000L)));

        // Test GET_AS_FLOAT
        assertEquals(3.14f, Fn.GET_AS_FLOAT.applyAsFloat(OptionalFloat.of(3.14f)), 0.001f);

        // Test GET_AS_DOUBLE
        assertEquals(2.718, Fn.GET_AS_DOUBLE.applyAsDouble(OptionalDouble.of(2.718)), 0.001);

        // Test GET_AS_INT_JDK
        assertEquals(100, Fn.GET_AS_INT_JDK.applyAsInt(java.util.OptionalInt.of(100)));

        // Test GET_AS_LONG_JDK
        assertEquals(1000L, Fn.GET_AS_LONG_JDK.applyAsLong(java.util.OptionalLong.of(1000L)));

        // Test GET_AS_DOUBLE_JDK
        assertEquals(2.718, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(java.util.OptionalDouble.of(2.718)), 0.001);
    }

    @Test
    public void testPublicStaticFieldsAsPredicates() {
        // Test IS_PRESENT_BOOLEAN
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        // Test IS_PRESENT_CHAR
        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        // Test IS_PRESENT_BYTE
        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 5)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        // Test IS_PRESENT_SHORT
        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 10)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        // Test IS_PRESENT_INT
        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(100)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));

        // Test IS_PRESENT_LONG
        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(1000L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));

        // Test IS_PRESENT_FLOAT
        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(3.14f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));

        // Test IS_PRESENT_DOUBLE
        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(2.718)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));

        // Test IS_PRESENT_INT_JDK
        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(100)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        // Test IS_PRESENT_LONG_JDK
        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1000L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        // Test IS_PRESENT_DOUBLE_JDK
        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(2.718)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    // Helper classes for testing
    private static class TestAutoCloseable implements AutoCloseable {
        private boolean closed = false;
        private int closeCount = 0;
        private boolean throwOnClose = false;

        @Override
        public void close() throws Exception {
            closed = true;
            closeCount++;
            if (throwOnClose) {
                throw new IOException("Close error");
            }
        }

        public boolean isClosed() {
            return closed;
        }

        public int getCloseCount() {
            return closeCount;
        }

        public void setThrowOnClose(boolean throwOnClose) {
            this.throwOnClose = throwOnClose;
        }
    }

    private static class Person {
        private final String name;

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
