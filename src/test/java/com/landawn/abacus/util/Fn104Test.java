package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

@Tag("new-test")
public class Fn104Test extends TestBase {

    @Test
    public void testGetAsBooleanConstant() {
        OptionalBoolean ob = OptionalBoolean.of(true);
        assertTrue(Fn.GET_AS_BOOLEAN.applyAsBoolean(ob));
    }

    @Test
    public void testGetAsCharConstant() {
        OptionalChar oc = OptionalChar.of('a');
        assertEquals('a', Fn.GET_AS_CHAR.applyAsChar(oc));
    }

    @Test
    public void testGetAsByteConstant() {
        OptionalByte ob = OptionalByte.of((byte) 5);
        assertEquals((byte) 5, Fn.GET_AS_BYTE.applyAsByte(ob));
    }

    @Test
    public void testGetAsShortConstant() {
        OptionalShort os = OptionalShort.of((short) 10);
        assertEquals((short) 10, Fn.GET_AS_SHORT.applyAsShort(os));
    }

    @Test
    public void testGetAsIntConstant() {
        OptionalInt oi = OptionalInt.of(100);
        assertEquals(100, Fn.GET_AS_INT.applyAsInt(oi));
    }

    @Test
    public void testGetAsLongConstant() {
        OptionalLong ol = OptionalLong.of(1000L);
        assertEquals(1000L, Fn.GET_AS_LONG.applyAsLong(ol));
    }

    @Test
    public void testGetAsFloatConstant() {
        OptionalFloat of = OptionalFloat.of(1.5f);
        assertEquals(1.5f, Fn.GET_AS_FLOAT.applyAsFloat(of));
    }

    @Test
    public void testGetAsDoubleConstant() {
        OptionalDouble od = OptionalDouble.of(2.5);
        assertEquals(2.5, Fn.GET_AS_DOUBLE.applyAsDouble(od));
    }

    @Test
    public void testGetAsIntJDKConstant() {
        java.util.OptionalInt oi = java.util.OptionalInt.of(100);
        assertEquals(100, Fn.GET_AS_INT_JDK.applyAsInt(oi));
    }

    @Test
    public void testGetAsLongJDKConstant() {
        java.util.OptionalLong ol = java.util.OptionalLong.of(1000L);
        assertEquals(1000L, Fn.GET_AS_LONG_JDK.applyAsLong(ol));
    }

    @Test
    public void testGetAsDoubleJDKConstant() {
        java.util.OptionalDouble od = java.util.OptionalDouble.of(2.5);
        assertEquals(2.5, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(od));
    }

    @Test
    public void testIsPresentPredicates() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('a')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testMemoizeSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoize(() -> counter.incrementAndGet());

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());

        Thread.sleep(150);

        assertEquals(2, memoized.get());
        assertEquals(2, memoized.get());
    }

    @Test
    public void testMemoizeWithExpirationInvalidArgument() {
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> 1, -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> 1, 0, TimeUnit.SECONDS));
    }

    @Test
    public void testMemoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> memoized = Fn.memoize(s -> {
            counter.incrementAndGet();
            return s.length();
        });

        assertEquals(5, memoized.apply("hello"));
        assertEquals(5, memoized.apply("hello"));
        assertEquals(3, memoized.apply("bye"));
        assertEquals(2, counter.get());

        assertThrows(NullPointerException.class, () -> memoized.apply(null));
    }

    @Test
    public void testClose() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);

        Runnable closer = Fn.close(closeable);
        assertFalse(closed.get());

        closer.run();
        assertTrue(closed.get());

        closed.set(false);
        closer.run();
        assertFalse(closed.get());
    }

    @Test
    public void testCloseAll() throws Exception {
        AtomicBoolean closed1 = new AtomicBoolean(false);
        AtomicBoolean closed2 = new AtomicBoolean(false);
        AutoCloseable closeable1 = () -> closed1.set(true);
        AutoCloseable closeable2 = () -> closed2.set(true);

        Runnable closer = Fn.closeAll(closeable1, closeable2);
        assertFalse(closed1.get());
        assertFalse(closed2.get());

        closer.run();
        assertTrue(closed1.get());
        assertTrue(closed2.get());
    }

    @Test
    public void testCloseAllCollection() throws Exception {
        List<AtomicBoolean> closedFlags = Arrays.asList(new AtomicBoolean(false), new AtomicBoolean(false));
        List<AutoCloseable> closeables = Arrays.asList(() -> closedFlags.get(0).set(true), () -> closedFlags.get(1).set(true));

        Runnable closer = Fn.closeAll(closeables);
        closer.run();

        assertTrue(closedFlags.get(0).get());
        assertTrue(closedFlags.get(1).get());
    }

    @Test
    public void testCloseQuietly() {
        AutoCloseable failing = () -> {
            throw new Exception("Test exception");
        };
        Runnable closer = Fn.closeQuietly(failing);

        assertDoesNotThrow(() -> closer.run());
    }

    @Test
    public void testCloseAllQuietly() {
        AutoCloseable failing1 = () -> {
            throw new Exception("Test exception 1");
        };
        AutoCloseable failing2 = () -> {
            throw new Exception("Test exception 2");
        };

        Runnable closer = Fn.closeAllQuietly(failing1, failing2);

        assertDoesNotThrow(() -> closer.run());
    }

    @Test
    public void testCloseAllQuietlyCollection() {
        List<AutoCloseable> closeables = Arrays.asList(() -> {
            throw new Exception("Test exception 1");
        }, () -> {
            throw new Exception("Test exception 2");
        });

        Runnable closer = Fn.closeAllQuietly(closeables);

        assertDoesNotThrow(() -> closer.run());
    }

    @Test
    public void testEmptyAction() {
        Runnable action = Fn.emptyAction();
        assertDoesNotThrow(() -> action.run());
    }

    @Test
    public void testShutDown() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Runnable shutdown = Fn.shutDown(service);

        assertFalse(service.isShutdown());
        shutdown.run();
        assertTrue(service.isShutdown());
    }

    @Test
    public void testShutDownWithTimeout() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Runnable shutdown = Fn.shutDown(service, 200, TimeUnit.MILLISECONDS);
        shutdown.run();

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
        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept("test"));
        assertEquals("Test error", ex.getMessage());
    }

    @Test
    public void testThrowException() {
        Consumer<String> consumer = Fn.throwException(() -> new IllegalStateException("Test"));
        assertThrows(IllegalStateException.class, () -> consumer.accept("test"));
    }

    @Test
    public void testToRuntimeException() {
        Function<Throwable, RuntimeException> converter = Fn.toRuntimeException();

        RuntimeException re = new RuntimeException("test");
        assertSame(re, converter.apply(re));

        Exception e = new Exception("test");
        RuntimeException converted = converter.apply(e);
        assertTrue(converted instanceof RuntimeException);
        assertSame(e, converted.getCause());
    }

    @Test
    public void testCloseConsumer() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);

        Consumer<AutoCloseable> consumer = Fn.close();
        consumer.accept(closeable);
        assertTrue(closed.get());
    }

    @Test
    public void testCloseQuietlyConsumer() {
        AutoCloseable failing = () -> {
            throw new Exception("Test");
        };
        Consumer<AutoCloseable> consumer = Fn.closeQuietly();

        assertDoesNotThrow(() -> consumer.accept(failing));
    }

    @Test
    public void testSleep() {
        Consumer<String> sleeper = Fn.sleep(50);
        long start = System.currentTimeMillis();
        sleeper.accept("test");
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepUninterruptibly() {
        Consumer<String> sleeper = Fn.sleepUninterruptibly(50);
        long start = System.currentTimeMillis();
        sleeper.accept("test");
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testRateLimiter() {
        Consumer<String> rateLimited = Fn.rateLimiter(2.0);

        long start = System.currentTimeMillis();
        rateLimited.accept("1");
        rateLimited.accept("2");
        rateLimited.accept("3");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 500);
    }

    @Test
    public void testPrintln() {
        Consumer<String> printer = Fn.println();
        assertDoesNotThrow(() -> printer.accept("test"));
    }

    @Test
    public void testPrintlnWithSeparator() {
        BiConsumer<String, String> printer = Fn.println("=");
        assertDoesNotThrow(() -> printer.accept("key", "value"));

        assertNotNull(Fn.println(":"));
        assertNotNull(Fn.println(": "));
        assertNotNull(Fn.println("-"));
        assertNotNull(Fn.println("_"));
        assertNotNull(Fn.println(","));
        assertNotNull(Fn.println(", "));
        assertNotNull(Fn.println(""));
        assertNotNull(Fn.println("custom"));
    }

    @Test
    public void testToStr() {
        Function<Object, String> toStr = Fn.toStr();
        assertEquals("123", toStr.apply(123));
        assertEquals("null", toStr.apply(null));
    }

    @Test
    public void testToCamelCase() {
        UnaryOperator<String> op = Fn.toCamelCase();
        assertEquals("helloWorld", op.apply("hello_world"));
    }

    @Test
    public void testToLowerCase() {
        UnaryOperator<String> op = Fn.toLowerCase();
        assertEquals("hello", op.apply("HELLO"));
    }

    @Test
    public void testToLowerCaseWithUnderscore() {
        UnaryOperator<String> op = Fn.toLowerCaseWithUnderscore();
        assertEquals("hello_world", op.apply("HelloWorld"));
    }

    @Test
    public void testToUpperCase() {
        UnaryOperator<String> op = Fn.toUpperCase();
        assertEquals("HELLO", op.apply("hello"));
    }

    @Test
    public void testToUpperCaseWithUnderscore() {
        UnaryOperator<String> op = Fn.toUpperCaseWithUnderscore();
        assertEquals("HELLO_WORLD", op.apply("HelloWorld"));
    }

    @Test
    public void testToJson() {
        Function<Object, String> toJson = Fn.toJson();
        String json = toJson.apply(Collections.singletonMap("key", "value"));
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    public void testToXml() {
        Function<Object, String> toXml = Fn.toXml();
        String xml = toXml.apply(Collections.singletonMap("key", "value"));
        assertNotNull(xml);
    }

    @Test
    public void testIdentity() {
        Function<String, String> identity = Fn.identity();
        String test = "test";
        assertSame(test, identity.apply(test));
    }

    @Test
    public void testKeyed() {
        Function<String, Keyed<Integer, String>> keyed = Fn.keyed(String::length);
        Keyed<Integer, String> result = keyed.apply("hello");
        assertEquals(5, result.key());
        assertEquals("hello", result.val());
    }

    @Test
    public void testVal() {
        Function<Keyed<String, Integer>, Integer> val = Fn.val();
        Keyed<String, Integer> keyed = Keyed.of("key", 123);
        assertEquals(123, val.apply(keyed));
    }

    @Test
    public void testKkv() {
        Function<Map.Entry<Keyed<String, Integer>, String>, Integer> kkv = Fn.kkv();
        Keyed<String, Integer> keyed = Keyed.of("key", 123);
        Map.Entry<Keyed<String, Integer>, String> entry = new AbstractMap.SimpleEntry<>(keyed, "value");
        assertEquals(123, kkv.apply(entry));
    }

    @Test
    public void testWrap() {
        Function<String, Wrapper<String>> wrap = Fn.wrap();
        Wrapper<String> wrapped = wrap.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testWrapWithCustomFunctions() {
        Function<String, Wrapper<String>> wrap = Fn.wrap(String::length, (a, b) -> a.equalsIgnoreCase(b));
        Wrapper<String> wrapped = wrap.apply("test");
        assertEquals("test", wrapped.value());
    }

    @Test
    public void testUnwrap() {
        Function<Wrapper<String>, String> unwrap = Fn.unwrap();
        Wrapper<String> wrapped = Wrapper.of("test");
        assertEquals("test", unwrap.apply(wrapped));
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
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>> inverse = Fn.inverse();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        Map.Entry<Integer, String> inverted = inverse.apply(entry);
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
    }

    @Test
    public void testTrimToEmpty() {
        UnaryOperator<String> trimToEmpty = Fn.trimToEmpty();
        assertEquals("hello", trimToEmpty.apply("  hello  "));
        assertEquals("", trimToEmpty.apply(null));
    }

    @Test
    public void testTrimToNull() {
        UnaryOperator<String> trimToNull = Fn.trimToNull();
        assertEquals("hello", trimToNull.apply("  hello  "));
        assertNull(trimToNull.apply("  "));
        assertNull(trimToNull.apply(null));
    }

    @Test
    public void testStrip() {
        UnaryOperator<String> strip = Fn.strip();
        assertEquals("hello", strip.apply("  hello  "));
    }

    @Test
    public void testStripToEmpty() {
        UnaryOperator<String> stripToEmpty = Fn.stripToEmpty();
        assertEquals("hello", stripToEmpty.apply("  hello  "));
        assertEquals("", stripToEmpty.apply(null));
    }

    @Test
    public void testStripToNull() {
        UnaryOperator<String> stripToNull = Fn.stripToNull();
        assertEquals("hello", stripToNull.apply("  hello  "));
        assertNull(stripToNull.apply("  "));
    }

    @Test
    public void testNullToEmpty() {
        UnaryOperator<String> nullToEmpty = Fn.nullToEmpty();
        assertEquals("", nullToEmpty.apply(null));
        assertEquals("hello", nullToEmpty.apply("hello"));
    }

    @Test
    public void testNullToEmptyList() {
        UnaryOperator<List<String>> nullToEmptyList = Fn.nullToEmptyList();
        assertTrue(nullToEmptyList.apply(null).isEmpty());
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, nullToEmptyList.apply(list));
    }

    @Test
    public void testNullToEmptySet() {
        UnaryOperator<Set<String>> nullToEmptySet = Fn.nullToEmptySet();
        assertTrue(nullToEmptySet.apply(null).isEmpty());
        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        assertSame(set, nullToEmptySet.apply(set));
    }

    @Test
    public void testNullToEmptyMap() {
        UnaryOperator<Map<String, Integer>> nullToEmptyMap = Fn.nullToEmptyMap();
        assertTrue(nullToEmptyMap.apply(null).isEmpty());
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, nullToEmptyMap.apply(map));
    }

    @Test
    public void testLen() {
        Function<String[], Integer> len = Fn.len();
        assertEquals(3, len.apply(new String[] { "a", "b", "c" }));
        assertEquals(0, len.apply(new String[0]));
        assertEquals(0, len.apply(null));
    }

    @Test
    public void testLength() {
        Function<String, Integer> length = Fn.length();
        assertEquals(5, length.apply("hello"));
        assertEquals(0, length.apply(""));
    }

    @Test
    public void testSize() {
        Function<List<String>, Integer> size = Fn.size();
        assertEquals(3, size.apply(Arrays.asList("a", "b", "c")));
        assertEquals(0, size.apply(Collections.emptyList()));
    }

    @Test
    public void testSizeM() {
        Function<Map<String, Integer>, Integer> sizeM = Fn.sizeM();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, sizeM.apply(map));
        assertEquals(0, sizeM.apply(Collections.emptyMap()));
    }

    @Test
    public void testCast() {
        Function<Object, String> cast = Fn.cast(String.class);
        assertEquals("hello", cast.apply("hello"));
        assertThrows(ClassCastException.class, () -> cast.apply(123));
    }

    @Test
    public void testAlwaysTrue() {
        Predicate<String> pred = Fn.alwaysTrue();
        assertTrue(pred.test("test"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testAlwaysFalse() {
        Predicate<String> pred = Fn.alwaysFalse();
        assertFalse(pred.test("test"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testIsNull() {
        Predicate<String> pred = Fn.isNull();
        assertTrue(pred.test(null));
        assertFalse(pred.test("test"));
    }

    @Test
    public void testIsNullWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isNull(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", null)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsEmpty() {
        Predicate<String> pred = Fn.isEmpty();
        assertTrue(pred.test(""));
        assertTrue(pred.test(null));
        assertFalse(pred.test("test"));
    }

    @Test
    public void testIsEmptyWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isEmpty(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "")));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsBlank() {
        Predicate<String> pred = Fn.isBlank();
        assertTrue(pred.test(""));
        assertTrue(pred.test("   "));
        assertTrue(pred.test(null));
        assertFalse(pred.test("test"));
    }

    @Test
    public void testIsBlankWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.isBlank(Map.Entry::getValue);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "   ")));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testIsEmptyA() {
        Predicate<String[]> pred = Fn.isEmptyA();
        assertTrue(pred.test(new String[0]));
        assertTrue(pred.test(null));
        assertFalse(pred.test(new String[] { "a" }));
    }

    @Test
    public void testIsEmptyC() {
        Predicate<List<String>> pred = Fn.isEmptyC();
        assertTrue(pred.test(Collections.emptyList()));
        assertTrue(pred.test(null));
        assertFalse(pred.test(Arrays.asList("a")));
    }

    @Test
    public void testIsEmptyM() {
        Predicate<Map<String, Integer>> pred = Fn.isEmptyM();
        assertTrue(pred.test(Collections.emptyMap()));
        assertTrue(pred.test(null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(pred.test(map));
    }

    @Test
    public void testNotNull() {
        Predicate<String> pred = Fn.notNull();
        assertFalse(pred.test(null));
        assertTrue(pred.test("test"));
    }

    @Test
    public void testNotNullWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notNull(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", null)));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotEmpty() {
        Predicate<String> pred = Fn.notEmpty();
        assertFalse(pred.test(""));
        assertFalse(pred.test(null));
        assertTrue(pred.test("test"));
    }

    @Test
    public void testNotEmptyWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notEmpty(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "")));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotBlank() {
        Predicate<String> pred = Fn.notBlank();
        assertFalse(pred.test(""));
        assertFalse(pred.test("   "));
        assertFalse(pred.test(null));
        assertTrue(pred.test("test"));
    }

    @Test
    public void testNotBlankWithExtractor() {
        Predicate<Map.Entry<String, String>> pred = Fn.notBlank(Map.Entry::getValue);
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", "   ")));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", "value")));
    }

    @Test
    public void testNotEmptyA() {
        Predicate<String[]> pred = Fn.notEmptyA();
        assertFalse(pred.test(new String[0]));
        assertFalse(pred.test(null));
        assertTrue(pred.test(new String[] { "a" }));
    }

    @Test
    public void testNotEmptyC() {
        Predicate<List<String>> pred = Fn.notEmptyC();
        assertFalse(pred.test(Collections.emptyList()));
        assertFalse(pred.test(null));
        assertTrue(pred.test(Arrays.asList("a")));
    }

    @Test
    public void testNotEmptyM() {
        Predicate<Map<String, Integer>> pred = Fn.notEmptyM();
        assertFalse(pred.test(Collections.emptyMap()));
        assertFalse(pred.test(null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(pred.test(map));
    }

    @Test
    public void testIsFile() throws IOException {
        Predicate<File> pred = Fn.isFile();
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        assertTrue(pred.test(tempFile));
        assertFalse(pred.test(new File(System.getProperty("java.io.tmpdir"))));
    }

    @Test
    public void testIsDirectory() {
        Predicate<File> pred = Fn.isDirectory();
        assertTrue(pred.test(new File(System.getProperty("java.io.tmpdir"))));
        assertFalse(pred.test(new File("nonexistent.txt")));
    }

    @Test
    public void testEqual() {
        Predicate<String> pred = Fn.equal("test");
        assertTrue(pred.test("test"));
        assertFalse(pred.test("other"));
        assertFalse(pred.test(null));

        Predicate<String> nullPred = Fn.equal(null);
        assertTrue(nullPred.test(null));
        assertFalse(nullPred.test("test"));
    }

    @Test
    public void testEqOr() {
        Predicate<String> pred2 = Fn.eqOr("a", "b");
        assertTrue(pred2.test("a"));
        assertTrue(pred2.test("b"));
        assertFalse(pred2.test("c"));

        Predicate<String> pred3 = Fn.eqOr("a", "b", "c");
        assertTrue(pred3.test("a"));
        assertTrue(pred3.test("b"));
        assertTrue(pred3.test("c"));
        assertFalse(pred3.test("d"));
    }

    @Test
    public void testNotEqual() {
        Predicate<String> pred = Fn.notEqual("test");
        assertFalse(pred.test("test"));
        assertTrue(pred.test("other"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testGreaterThan() {
        Predicate<Integer> pred = Fn.greaterThan(5);
        assertTrue(pred.test(6));
        assertFalse(pred.test(5));
        assertFalse(pred.test(4));
    }

    @Test
    public void testGreaterEqual() {
        Predicate<Integer> pred = Fn.greaterEqual(5);
        assertTrue(pred.test(6));
        assertTrue(pred.test(5));
        assertFalse(pred.test(4));
    }

    @Test
    public void testLessThan() {
        Predicate<Integer> pred = Fn.lessThan(5);
        assertTrue(pred.test(4));
        assertFalse(pred.test(5));
        assertFalse(pred.test(6));
    }

    @Test
    public void testLessEqual() {
        Predicate<Integer> pred = Fn.lessEqual(5);
        assertTrue(pred.test(4));
        assertTrue(pred.test(5));
        assertFalse(pred.test(6));
    }

    @Test
    public void testGtAndLt() {
        Predicate<Integer> pred = Fn.gtAndLt(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(4));
        assertFalse(pred.test(1));
        assertFalse(pred.test(5));
    }

    @Test
    public void testGeAndLt() {
        Predicate<Integer> pred = Fn.geAndLt(1, 5);
        assertTrue(pred.test(1));
        assertTrue(pred.test(2));
        assertTrue(pred.test(4));
        assertFalse(pred.test(0));
        assertFalse(pred.test(5));
    }

    @Test
    public void testGeAndLe() {
        Predicate<Integer> pred = Fn.geAndLe(1, 5);
        assertTrue(pred.test(1));
        assertTrue(pred.test(3));
        assertTrue(pred.test(5));
        assertFalse(pred.test(0));
        assertFalse(pred.test(6));
    }

    @Test
    public void testGtAndLe() {
        Predicate<Integer> pred = Fn.gtAndLe(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(5));
        assertFalse(pred.test(1));
        assertFalse(pred.test(6));
    }

    @Test
    public void testBetween() {
        Predicate<Integer> pred = Fn.between(1, 5);
        assertTrue(pred.test(2));
        assertTrue(pred.test(3));
        assertTrue(pred.test(4));
        assertFalse(pred.test(1));
        assertFalse(pred.test(5));
    }

    @Test
    public void testIn() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> pred = Fn.in(list);
        assertTrue(pred.test("a"));
        assertTrue(pred.test("b"));
        assertFalse(pred.test("d"));

        Predicate<String> emptyPred = Fn.in(Collections.emptyList());
        assertFalse(emptyPred.test("a"));
    }

    @Test
    public void testNotIn() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> pred = Fn.notIn(list);
        assertFalse(pred.test("a"));
        assertFalse(pred.test("b"));
        assertTrue(pred.test("d"));

        Predicate<String> emptyPred = Fn.notIn(Collections.emptyList());
        assertTrue(emptyPred.test("a"));
    }

    @Test
    public void testInstanceOf() {
        Predicate<Object> pred = Fn.instanceOf(String.class);
        assertTrue(pred.test("test"));
        assertFalse(pred.test(123));
        assertFalse(pred.test(null));
    }

    @Test
    public void testSubtypeOf() {
        Predicate<Class<?>> pred = Fn.subtypeOf(Number.class);
        assertTrue(pred.test(Integer.class));
        assertTrue(pred.test(Double.class));
        assertFalse(pred.test(String.class));
    }

    @Test
    public void testStartsWith() {
        Predicate<String> pred = Fn.startsWith("pre");
        assertTrue(pred.test("prefix"));
        assertFalse(pred.test("suffix"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testEndsWith() {
        Predicate<String> pred = Fn.endsWith("fix");
        assertTrue(pred.test("suffix"));
        assertTrue(pred.test("prefix"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testContains() {
        Predicate<String> pred = Fn.contains("sub");
        assertTrue(pred.test("substring"));
        assertFalse(pred.test("string"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testNotStartsWith() {
        Predicate<String> pred = Fn.notStartsWith("pre");
        assertFalse(pred.test("prefix"));
        assertTrue(pred.test("suffix"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testNotEndsWith() {
        Predicate<String> pred = Fn.notEndsWith("fix");
        assertFalse(pred.test("suffix"));
        assertFalse(pred.test("prefix"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testNotContains() {
        Predicate<String> pred = Fn.notContains("sub");
        assertFalse(pred.test("substring"));
        assertTrue(pred.test("string"));
        assertTrue(pred.test(null));
    }

    @Test
    public void testMatches() {
        Pattern pattern = Pattern.compile("\\d+");
        Predicate<CharSequence> pred = Fn.matches(pattern);
        assertTrue(pred.test("123"));
        assertTrue(pred.test("abc123def"));
        assertFalse(pred.test("abc"));
    }

    @Test
    public void testEqualBiPredicate() {
        BiPredicate<String, String> pred = Fn.equal();
        assertTrue(pred.test("test", "test"));
        assertFalse(pred.test("test", "other"));
        assertTrue(pred.test(null, null));
    }

    @Test
    public void testNotEqualBiPredicate() {
        BiPredicate<String, String> pred = Fn.notEqual();
        assertFalse(pred.test("test", "test"));
        assertTrue(pred.test("test", "other"));
        assertFalse(pred.test(null, null));
    }

    @Test
    public void testGreaterThanBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.greaterThan();
        assertTrue(pred.test(5, 3));
        assertFalse(pred.test(3, 5));
        assertFalse(pred.test(3, 3));
    }

    @Test
    public void testGreaterEqualBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.greaterEqual();
        assertTrue(pred.test(5, 3));
        assertFalse(pred.test(3, 5));
        assertTrue(pred.test(3, 3));
    }

    @Test
    public void testLessThanBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.lessThan();
        assertTrue(pred.test(3, 5));
        assertFalse(pred.test(5, 3));
        assertFalse(pred.test(3, 3));
    }

    @Test
    public void testLessEqualBiPredicate() {
        BiPredicate<Integer, Integer> pred = Fn.lessEqual();
        assertTrue(pred.test(3, 5));
        assertFalse(pred.test(5, 3));
        assertTrue(pred.test(3, 3));
    }

    @Test
    public void testNot() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> notNull = Fn.not(isNull);
        assertTrue(notNull.test("test"));
        assertFalse(notNull.test(null));
    }

    @Test
    public void testNotBiPredicate() {
        BiPredicate<String, String> equal = Fn.equal();
        BiPredicate<String, String> notEqual = Fn.not(equal);
        assertTrue(notEqual.test("a", "b"));
        assertFalse(notEqual.test("a", "a"));
    }

    @Test
    public void testNotTriPredicate() {
        TriPredicate<Integer, Integer, Integer> allEqual = (a, b, c) -> a.equals(b) && b.equals(c);
        TriPredicate<Integer, Integer, Integer> notAllEqual = Fn.not(allEqual);
        assertTrue(notAllEqual.test(1, 2, 3));
        assertFalse(notAllEqual.test(1, 1, 1));
    }

    @Test
    public void testAndBooleanSupplier() {
        BooleanSupplier and2 = Fn.and(() -> true, () -> true);
        assertTrue(and2.getAsBoolean());

        and2 = Fn.and(() -> true, () -> false);
        assertFalse(and2.getAsBoolean());

        BooleanSupplier and3 = Fn.and(() -> true, () -> true, () -> true);
        assertTrue(and3.getAsBoolean());

        and3 = Fn.and(() -> true, () -> false, () -> true);
        assertFalse(and3.getAsBoolean());
    }

    @Test
    public void testAndPredicate() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();

        Predicate<String> and2 = Fn.and(notNull, notEmpty);
        assertTrue(and2.test("test"));
        assertFalse(and2.test(""));
        assertFalse(and2.test(null));

        Predicate<String> notBlank = Fn.notBlank();
        Predicate<String> and3 = Fn.and(notNull, notEmpty, notBlank);
        assertTrue(and3.test("test"));
        assertFalse(and3.test("   "));
    }

    @Test
    public void testAndPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(x -> x > 0, x -> x < 10, x -> x % 2 == 0);
        Predicate<Integer> combined = Fn.and(predicates);
        assertTrue(combined.test(4));
        assertFalse(combined.test(5));
        assertFalse(combined.test(12));
    }

    @Test
    public void testAndBiPredicate() {
        BiPredicate<String, Integer> pred1 = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> pred2 = (s, i) -> i > 0;

        BiPredicate<String, Integer> and2 = Fn.and(pred1, pred2);
        assertTrue(and2.test("hello", 5));
        assertFalse(and2.test("hello", 4));
        assertFalse(and2.test("", 0));

        BiPredicate<String, Integer> pred3 = (s, i) -> s.startsWith("h");
        BiPredicate<String, Integer> and3 = Fn.and(pred1, pred2, pred3);
        assertTrue(and3.test("hello", 5));
        assertFalse(and3.test("world", 5));
    }

    @Test
    public void testAndBiPredicateList() {
        List<BiPredicate<String, Integer>> predicates = Arrays.asList((s, i) -> s.length() == i, (s, i) -> i > 0);
        BiPredicate<String, Integer> combined = Fn.and(predicates);
        assertTrue(combined.test("hello", 5));
        assertFalse(combined.test("hello", 4));
    }

    @Test
    public void testOrBooleanSupplier() {
        BooleanSupplier or2 = Fn.or(() -> false, () -> false);
        assertFalse(or2.getAsBoolean());

        or2 = Fn.or(() -> true, () -> false);
        assertTrue(or2.getAsBoolean());

        BooleanSupplier or3 = Fn.or(() -> false, () -> false, () -> false);
        assertFalse(or3.getAsBoolean());

        or3 = Fn.or(() -> false, () -> true, () -> false);
        assertTrue(or3.getAsBoolean());
    }

    @Test
    public void testOrPredicate() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> isEmpty = Fn.isEmpty();

        Predicate<String> or2 = Fn.or(isNull, isEmpty);
        assertTrue(or2.test(null));
        assertTrue(or2.test(""));
        assertFalse(or2.test("test"));

        Predicate<String> isBlank = Fn.isBlank();
        Predicate<String> or3 = Fn.or(isNull, isEmpty, isBlank);
        assertTrue(or3.test("   "));
    }

    @Test
    public void testOrPredicateCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(x -> x < 0, x -> x > 10, x -> x == 5);
        Predicate<Integer> combined = Fn.or(predicates);
        assertTrue(combined.test(-1));
        assertTrue(combined.test(15));
        assertTrue(combined.test(5));
        assertFalse(combined.test(3));
    }

    @Test
    public void testOrBiPredicate() {
        BiPredicate<String, Integer> pred1 = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> pred2 = (s, i) -> i == 0;

        BiPredicate<String, Integer> or2 = Fn.or(pred1, pred2);
        assertTrue(or2.test("hello", 5));
        assertTrue(or2.test("world", 0));
        assertFalse(or2.test("hello", 4));

        BiPredicate<String, Integer> pred3 = (s, i) -> s.isEmpty();
        BiPredicate<String, Integer> or3 = Fn.or(pred1, pred2, pred3);
        assertTrue(or3.test("", 10));
    }

    @Test
    public void testOrBiPredicateList() {
        List<BiPredicate<String, Integer>> predicates = Arrays.asList((s, i) -> s.length() == i, (s, i) -> i == 0);
        BiPredicate<String, Integer> combined = Fn.or(predicates);
        assertTrue(combined.test("hello", 5));
        assertTrue(combined.test("world", 0));
        assertFalse(combined.test("hello", 4));
    }

    @Test
    public void testTestByKey() {
        Predicate<Map.Entry<String, Integer>> pred = Fn.testByKey(s -> s.startsWith("k"));
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", 1)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("value", 1)));
    }

    @Test
    public void testTestByValue() {
        Predicate<Map.Entry<String, Integer>> pred = Fn.testByValue(i -> i > 5);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("key", 10)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("key", 3)));
    }

    @Test
    public void testAcceptByKey() {
        List<String> keys = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> consumer = Fn.acceptByKey(keys::add);
        consumer.accept(new AbstractMap.SimpleEntry<>("key1", 1));
        consumer.accept(new AbstractMap.SimpleEntry<>("key2", 2));
        assertEquals(Arrays.asList("key1", "key2"), keys);
    }

    @Test
    public void testAcceptByValue() {
        List<Integer> values = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> consumer = Fn.acceptByValue(values::add);
        consumer.accept(new AbstractMap.SimpleEntry<>("key1", 1));
        consumer.accept(new AbstractMap.SimpleEntry<>("key2", 2));
        assertEquals(Arrays.asList(1, 2), values);
    }

    @Test
    public void testApplyByKey() {
        Function<Map.Entry<String, Integer>, Integer> func = Fn.applyByKey(String::length);
        assertEquals(3, func.apply(new AbstractMap.SimpleEntry<>("key", 123)));
    }

    @Test
    public void testApplyByValue() {
        Function<Map.Entry<String, Integer>, String> func = Fn.applyByValue(Object::toString);
        assertEquals("123", func.apply(new AbstractMap.SimpleEntry<>("key", 123)));
    }

    @Test
    public void testMapKey() {
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, Integer>> func = Fn.mapKey(String::length);
        Map.Entry<Integer, Integer> result = func.apply(new AbstractMap.SimpleEntry<>("hello", 123));
        assertEquals(5, result.getKey());
        assertEquals(123, result.getValue());
    }

    @Test
    public void testMapValue() {
        Function<Map.Entry<String, Integer>, Map.Entry<String, String>> func = Fn.mapValue(Object::toString);
        Map.Entry<String, String> result = func.apply(new AbstractMap.SimpleEntry<>("key", 123));
        assertEquals("key", result.getKey());
        assertEquals("123", result.getValue());
    }

    @Test
    public void testTestKeyVal() {
        Predicate<Map.Entry<String, Integer>> pred = Fn.testKeyVal((k, v) -> k.length() == v);
        assertTrue(pred.test(new AbstractMap.SimpleEntry<>("hello", 5)));
        assertFalse(pred.test(new AbstractMap.SimpleEntry<>("hello", 4)));
    }

    @Test
    public void testAcceptKeyVal() {
        Map<String, Integer> map = new HashMap<>();
        Consumer<Map.Entry<String, Integer>> consumer = Fn.acceptKeyVal(map::put);
        consumer.accept(new AbstractMap.SimpleEntry<>("key1", 1));
        consumer.accept(new AbstractMap.SimpleEntry<>("key2", 2));
        assertEquals(2, map.size());
        assertEquals(1, map.get("key1").intValue());
    }

    @Test
    public void testApplyKeyVal() {
        Function<Map.Entry<String, Integer>, String> func = Fn.applyKeyVal((k, v) -> k + "=" + v);
        assertEquals("key=123", func.apply(new AbstractMap.SimpleEntry<>("key", 123)));
    }

    @Test
    public void testAcceptIfNotNull() {
        List<String> list = new ArrayList<>();
        Consumer<String> consumer = Fn.acceptIfNotNull(list::add);
        consumer.accept("test");
        consumer.accept(null);
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testAcceptIf() {
        List<Integer> list = new ArrayList<>();
        Consumer<Integer> consumer = Fn.acceptIf(i -> i > 5, list::add);
        consumer.accept(3);
        consumer.accept(7);
        consumer.accept(10);
        assertEquals(Arrays.asList(7, 10), list);
    }

    @Test
    public void testAcceptIfOrElse() {
        List<Integer> evens = new ArrayList<>();
        List<Integer> odds = new ArrayList<>();
        Consumer<Integer> consumer = Fn.acceptIfOrElse(i -> i % 2 == 0, evens::add, odds::add);
        consumer.accept(2);
        consumer.accept(3);
        consumer.accept(4);
        assertEquals(Arrays.asList(2, 4), evens);
        assertEquals(Arrays.asList(3), odds);
    }

    @Test
    public void testApplyIfNotNullOrEmpty() {
        Function<List<String>, Collection<String>> func = Fn.applyIfNotNullOrEmpty(l -> l.subList(0, 2));
        assertEquals(Arrays.asList("a", "b"), func.apply(Arrays.asList("a", "b", "c")));
        assertTrue(func.apply(null).isEmpty());
    }

    @Test
    public void testApplyIfNotNullOrDefault2() {
        Function<String, String> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, "DEFAULT");
        assertEquals("HELLO", func.apply("  hello  "));
        assertEquals("DEFAULT", func.apply(null));
        assertEquals("", func.apply("  "));
    }

    @Test
    public void testApplyIfNotNullOrDefault3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, String::length, -1);
        assertEquals(5, func.apply("  hello  "));
        assertEquals(-1, func.apply(null));
        assertEquals(0, func.apply("  "));
    }

    @Test
    public void testApplyIfNotNullOrDefault4() {
        Function<String, Character> func = Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, s -> s.substring(0, 1), s -> s.charAt(0), '?');
        assertEquals('H', func.apply("  hello  "));
        assertEquals('?', func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet2() {
        Function<String, String> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, () -> "SUPPLIED");
        assertEquals("HELLO", func.apply("  hello  "));
        assertEquals("SUPPLIED", func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet3() {
        Function<String, Integer> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, String::length, () -> -1);
        assertEquals(5, func.apply("  hello  "));
        assertEquals(-1, func.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet4() {
        Function<String, Character> func = Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, s -> s.substring(0, 1), s -> s.charAt(0), () -> '?');
        assertEquals('H', func.apply("  hello  "));
        assertEquals('?', func.apply(null));
    }

    @Test
    public void testApplyIfOrElseDefault() {
        Function<Integer, String> func = Fn.applyIfOrElseDefault(i -> i > 0, i -> "positive: " + i, "non-positive");
        assertEquals("positive: 5", func.apply(5));
        assertEquals("non-positive", func.apply(-3));
        assertEquals("non-positive", func.apply(0));
    }

    @Test
    public void testApplyIfOrElseGet() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, String> func = Fn.applyIfOrElseGet(i -> i > 0, i -> "positive: " + i, () -> "supplied: " + counter.incrementAndGet());
        assertEquals("positive: 5", func.apply(5));
        assertEquals("supplied: 1", func.apply(-3));
        assertEquals("supplied: 2", func.apply(0));
    }

    @Test
    public void testFlatmapValue() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        Function<Map<String, ? extends Collection<Integer>>, List<Map<String, Integer>>> func = Fn.flatmapValue();
        List<Map<String, Integer>> result = func.apply(map);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).get("a").intValue());
        assertEquals(4, result.get(0).get("b").intValue());
        assertEquals(7, result.get(0).get("c").intValue());
    }

    @Test
    public void testParseByte() {
        ToByteFunction<String> parser = Fn.parseByte();
        assertEquals((byte) 123, parser.applyAsByte("123"));
        assertEquals((byte) -45, parser.applyAsByte("-45"));
    }

    @Test
    public void testParseShort() {
        ToShortFunction<String> parser = Fn.parseShort();
        assertEquals((short) 12345, parser.applyAsShort("12345"));
        assertEquals((short) -678, parser.applyAsShort("-678"));
    }

    @Test
    public void testParseInt() {
        ToIntFunction<String> parser = Fn.parseInt();
        assertEquals(123456, parser.applyAsInt("123456"));
        assertEquals(-789, parser.applyAsInt("-789"));
    }

    @Test
    public void testParseLong() {
        ToLongFunction<String> parser = Fn.parseLong();
        assertEquals(123456789L, parser.applyAsLong("123456789"));
        assertEquals(-987654321L, parser.applyAsLong("-987654321"));
    }

    @Test
    public void testParseFloat() {
        ToFloatFunction<String> parser = Fn.parseFloat();
        assertEquals(123.45f, parser.applyAsFloat("123.45"), 0.001f);
        assertEquals(-67.89f, parser.applyAsFloat("-67.89"), 0.001f);
    }

    @Test
    public void testParseDouble() {
        ToDoubleFunction<String> parser = Fn.parseDouble();
        assertEquals(123.456789, parser.applyAsDouble("123.456789"), 0.000001);
        assertEquals(-987.654321, parser.applyAsDouble("-987.654321"), 0.000001);
    }

    @Test
    public void testCreateNumber() {
        Function<String, Number> creator = Fn.createNumber();
        assertTrue(creator.apply("123") instanceof Integer);
        assertTrue(creator.apply("123456789012") instanceof Long);
        assertTrue(creator.apply("123.45") instanceof Double);
        assertNull(creator.apply(""));
        assertNull(creator.apply(null));
    }

    @Test
    public void testNumToInt() {
        ToIntFunction<Number> converter = Fn.numToInt();
        assertEquals(123, converter.applyAsInt(123));
        assertEquals(123, converter.applyAsInt(123L));
        assertEquals(123, converter.applyAsInt(123.45));
    }

    @Test
    public void testNumToLong() {
        ToLongFunction<Number> converter = Fn.numToLong();
        assertEquals(123L, converter.applyAsLong(123));
        assertEquals(123456789012L, converter.applyAsLong(123456789012L));
        assertEquals(123L, converter.applyAsLong(123.45));
    }

    @Test
    public void testNumToDouble() {
        ToDoubleFunction<Number> converter = Fn.numToDouble();
        assertEquals(123.0, converter.applyAsDouble(123), 0.001);
        assertEquals(123.45, converter.applyAsDouble(123.45), 0.001);
        assertEquals(123.0, converter.applyAsDouble(123L), 0.001);
    }

    @Test
    public void testAtMost() {
        Predicate<String> atMost3 = Fn.atMost(3);
        assertTrue(atMost3.test("1"));
        assertTrue(atMost3.test("2"));
        assertTrue(atMost3.test("3"));
        assertFalse(atMost3.test("4"));
        assertFalse(atMost3.test("5"));

        assertThrows(IllegalArgumentException.class, () -> Fn.atMost(-1));
    }

    @Test
    public void testDeprecatedEntry() {
        Function<Integer, Map.Entry<String, Integer>> entryFunc = Fn.entry("fixedKey");
        Map.Entry<String, Integer> entry = entryFunc.apply(123);
        assertEquals("fixedKey", entry.getKey());
        assertEquals(123, entry.getValue());

        Function<String, Map.Entry<Integer, String>> entryFunc2 = Fn.entry(String::length);
        Map.Entry<Integer, String> entry2 = entryFunc2.apply("hello");
        assertEquals(5, entry2.getKey());
        assertEquals("hello", entry2.getValue());
    }

    @Test
    public void testMemoizeFunctionWithNull() {
        Function<String, String> memoized = Fn.memoize(s -> s == null ? "null" : s.toUpperCase());
        assertEquals("null", memoized.apply(null));
        assertEquals("null", memoized.apply(null));
        assertEquals("HELLO", memoized.apply("hello"));
    }

    @Test
    public void testPredicatesWithNullCollections() {
        assertThrows(IllegalArgumentException.class, () -> Fn.in(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notIn(null));
    }

    @Test
    public void testStringPredicatesWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Fn.startsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.endsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.contains(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notStartsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notEndsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notContains(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.matches(null));
    }

    @Test
    public void testFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.keyed(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.wrap(null, (a, b) -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.wrap(String::hashCode, null));
        assertThrows(IllegalArgumentException.class, () -> Fn.cast(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.instanceOf(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.subtypeOf(null));
    }

    @Test
    public void testMapEntryFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.testByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testKeyVal(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptKeyVal(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyKeyVal(null));
    }

    @Test
    public void testConditionalFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIf(null, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIf(x -> true, null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(null, System.out::println, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(x -> true, null, System.out::println));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfOrElse(x -> true, System.out::println, null));
    }

    @Test
    public void testLogicalOperationsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.not((Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((BiPredicate<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((TriPredicate<String, String, String>) null));

        assertThrows(IllegalArgumentException.class, () -> Fn.and((BooleanSupplier) null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(() -> true, (BooleanSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.and((Collection<Predicate<String>>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Collections.<Predicate<String>> emptyList()));

        assertThrows(IllegalArgumentException.class, () -> Fn.or((BooleanSupplier) null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(() -> true, (BooleanSupplier) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.or((Collection<Predicate<String>>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Collections.<Predicate<String>> emptyList()));
    }

    @Test
    public void testPrintlnSeparatorWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Fn.println(null));
    }

    @Test
    public void testRateLimiterWithRateLimiterInstance() {
        RateLimiter rateLimiter = RateLimiter.create(2.0);
        Consumer<String> rateLimited = Fn.rateLimiter(rateLimiter);

        long start = System.currentTimeMillis();
        rateLimited.accept("1");
        rateLimited.accept("2");
        rateLimited.accept("3");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 500);
    }

    @Test
    public void testEntryFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByKeyMapper(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByValueMapper(null));
    }

    @Test
    public void testApplyIfFunctionsWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseDefault(null, x -> x, "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseDefault(x -> true, null, "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(null, x -> x, () -> "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(x -> true, null, () -> "default"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfOrElseGet(x -> true, x -> x, null));
    }

    @Test
    public void testApplyIfNotNullOrDefaultWithNullMappers() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, null, 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, String::trim, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, null, String::length, 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(String::trim, String::toUpperCase, null, 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, s -> s, s -> s, s -> s, 'a'));
    }

    @Test
    public void testApplyIfNotNullOrElseGetWithNullMappers() {
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, null, () -> 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, String::trim, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, null, String::length, () -> 0));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(String::trim, String::toUpperCase, null, () -> 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(null, s -> s, s -> s, s -> s, () -> 'a'));
    }

    @Test
    public void testMemoizeWithExpirationConcurrency() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return counter.incrementAndGet();
        }, 200, TimeUnit.MILLISECONDS);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.supplyAsync(memoized::get));
        }

        List<Integer> results = futures.stream().map(CompletableFuture::join).collect(java.util.stream.Collectors.toList());

        assertTrue(results.stream().allMatch(r -> r.equals(1)));
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpirationEdgeCase() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(counter::incrementAndGet, 1, TimeUnit.NANOSECONDS);

        int first = memoized.get();
        Thread.sleep(1);
        int second = memoized.get();

        assertNotEquals(first, second);
    }
}
