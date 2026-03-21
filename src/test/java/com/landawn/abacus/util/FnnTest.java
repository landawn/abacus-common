package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables.Consumer;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;

public class FnnTest extends TestBase {

    // --- memoize(Supplier) ---

    @Test
    public void testMemoize_Supplier() throws Exception {
        int[] callCount = { 0 };
        Throwables.Supplier<String, Exception> supplier = Fnn.memoize(() -> {
            callCount[0]++;
            return "result";
        });

        assertEquals("result", supplier.get());
        assertEquals("result", supplier.get());
        assertEquals("result", supplier.get());
        assertEquals(1, callCount[0], "Supplier should only be called once");
    }

    @Test
    public void testMemoize_Supplier_nullResult() throws Exception {
        int[] callCount = { 0 };
        Throwables.Supplier<String, Exception> supplier = Fnn.memoize(() -> {
            callCount[0]++;
            return null;
        });

        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(1, callCount[0]);
    }

    @Test
    public void testMemoizeWithExpiration_TimeUnit() throws Exception {
        int[] callCount = { 0 };
        Throwables.Supplier<String, Exception> supplier = Fnn.memoizeWithExpiration(() -> {
            callCount[0]++;
            return "value";
        }, 1, java.util.concurrent.TimeUnit.HOURS);
        assertEquals("value", supplier.get());
        assertEquals("value", supplier.get()); // should be cached
        assertEquals(1, callCount[0]); // only called once
    }

    @Test
    public void testMemoizeWithExpiration_expiredEntry() throws Exception {
        int[] callCount = { 0 };
        // Use very short duration so it expires
        Throwables.Supplier<String, Exception> supplier = Fnn.memoizeWithExpiration(() -> {
            callCount[0]++;
            return "val" + callCount[0];
        }, 1, java.util.concurrent.TimeUnit.MILLISECONDS);
        String first = supplier.get();
        assertEquals(1, callCount[0]);
        // sleep to let it expire
        Thread.sleep(10);
        String second = supplier.get();
        assertEquals(2, callCount[0]);
        assertNotNull(first);
        assertNotNull(second);
    }

    // Additional tests for uncovered branches

    @Test
    public void testMemoizeWithExpiration_secondCallUsesCached() throws Exception {
        // Tests that memoized result is returned from cache on second call within expiration
        final int[] callCount = { 0 };
        Throwables.Supplier<String, Exception> supplier = () -> {
            callCount[0]++;
            return "result-" + callCount[0];
        };
        Throwables.Supplier<String, Exception> memoized = Fnn.memoizeWithExpiration(supplier, 60, java.util.concurrent.TimeUnit.SECONDS);
        String first = memoized.get();
        String second = memoized.get();
        assertEquals(first, second);
        assertEquals(1, callCount[0]); // only called once
    }

    @Test
    public void testMemoizeWithExpiration_expiredAndRefetched() throws Exception {
        // Tests that expired entries are re-fetched
        final int[] callCount = { 0 };
        Throwables.Supplier<String, Exception> supplier = () -> {
            callCount[0]++;
            return "result-" + callCount[0];
        };
        Throwables.Supplier<String, Exception> memoized = Fnn.memoizeWithExpiration(supplier, 1, java.util.concurrent.TimeUnit.MILLISECONDS);
        String first = memoized.get();
        Thread.sleep(5);
        String second = memoized.get();
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(callCount[0] >= 2); // called at least twice
    }

    // --- identity() ---

    @Test
    public void testIdentity() throws Exception {
        Throwables.Function<String, String, Exception> id = Fnn.identity();
        assertNotNull(id);
        assertEquals("hello", id.apply("hello"));
        assertNull(id.apply(null));
        assertEquals("", id.apply(""));
    }

    @Test
    public void testIdentity_sameInstance() throws Exception {
        Throwables.Function<Integer, Integer, Exception> id = Fnn.identity();
        Integer val = 42;
        assertSame(val, id.apply(val));
    }

    // --- alwaysTrue() ---

    @Test
    public void testAlwaysTrue() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.alwaysTrue();
        assertNotNull(pred);
        assertTrue(pred.test("anything"));
        assertTrue(pred.test(null));
        assertTrue(pred.test(""));
    }

    // --- alwaysFalse() ---

    @Test
    public void testAlwaysFalse() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.alwaysFalse();
        assertNotNull(pred);
        assertFalse(pred.test("anything"));
        assertFalse(pred.test(null));
        assertFalse(pred.test(""));
    }

    // --- toStr() ---

    @Test
    public void testToStr() throws Exception {
        Throwables.Function<Object, String, Exception> toStr = Fnn.toStr();
        assertNotNull(toStr);
        assertEquals("42", toStr.apply(42));
        assertEquals("hello", toStr.apply("hello"));
        assertEquals("null", toStr.apply(null));
        assertEquals("true", toStr.apply(true));
    }

    @Test
    public void testToStr_functionality() throws Exception {
        Throwables.Function<Integer, String, Exception> toStr = Fnn.toStr();
        assertEquals("42", toStr.apply(42));
        assertEquals("null", toStr.apply(null));
    }

    // --- key() ---

    @Test
    public void testKey() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, String, Exception> keyFn = Fnn.key();
        assertNotNull(keyFn);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("name", 42);
        assertEquals("name", keyFn.apply(entry));
    }

    @Test
    public void testKey_nullKey() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, String, Exception> keyFn = Fnn.key();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>(null, 42);
        assertNull(keyFn.apply(entry));
    }

    // --- value() ---

    @Test
    public void testValue() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, Integer, Exception> valFn = Fnn.value();
        assertNotNull(valFn);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("name", 42);
        assertEquals(Integer.valueOf(42), valFn.apply(entry));
    }

    @Test
    public void testValue_nullValue() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, Integer, Exception> valFn = Fnn.value();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("name", null);
        assertNull(valFn.apply(entry));
    }

    // --- invert() ---

    @Test
    public void testInvert() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>, Exception> invertFn = Fnn.invert();
        assertNotNull(invertFn);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        Map.Entry<Integer, String> inverted = invertFn.apply(entry);
        assertEquals(Integer.valueOf(100), inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    @Test
    public void testInvert_nullKeyAndValue() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>, Exception> invertFn = Fnn.invert();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>(null, null);
        Map.Entry<Integer, String> inverted = invertFn.apply(entry);
        assertNull(inverted.getKey());
        assertNull(inverted.getValue());
    }

    @Test
    public void testInvert_functionality() throws Exception {
        Throwables.Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>, Exception> inverter = Fnn.invert();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 42);
        Map.Entry<Integer, String> inverted = inverter.apply(entry);
        assertEquals(42, (int) inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    // --- entry() ---

    @Test
    public void testEntry() throws Exception {
        Throwables.BiFunction<String, Integer, Map.Entry<String, Integer>, Exception> entryFn = Fnn.entry();
        assertNotNull(entryFn);

        Map.Entry<String, Integer> entry = entryFn.apply("key", 42);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
    }

    @Test
    public void testEntry_functionality() throws Exception {
        Throwables.BiFunction<String, Integer, Map.Entry<String, Integer>, Exception> entryFn = Fnn.entry();
        Map.Entry<String, Integer> e = entryFn.apply("hello", 5);
        assertEquals("hello", e.getKey());
        assertEquals(5, (int) e.getValue());
    }

    // --- pair() ---

    @Test
    public void testPair() throws Exception {
        Throwables.BiFunction<String, Integer, Pair<String, Integer>, Exception> pairFn = Fnn.pair();
        assertNotNull(pairFn);

        Pair<String, Integer> p = pairFn.apply("left", 42);
        assertEquals("left", p.left());
        assertEquals(Integer.valueOf(42), p.right());
    }

    @Test
    public void testPair_nullValues() throws Exception {
        Throwables.BiFunction<String, Integer, Pair<String, Integer>, Exception> pairFn = Fnn.pair();
        Pair<String, Integer> p = pairFn.apply(null, null);
        assertNull(p.left());
        assertNull(p.right());
    }

    @Test
    public void testPair_functionality() throws Exception {
        Throwables.BiFunction<String, Integer, Pair<String, Integer>, Exception> pairFn = Fnn.pair();
        Pair<String, Integer> p = pairFn.apply("left", 99);
        assertEquals("left", p.left());
        assertEquals(99, (int) p.right());
    }

    // --- triple() ---

    @Test
    public void testTriple() throws Exception {
        Throwables.TriFunction<String, Integer, Boolean, Triple<String, Integer, Boolean>, Exception> tripleFn = Fnn.triple();
        assertNotNull(tripleFn);

        Triple<String, Integer, Boolean> t = tripleFn.apply("a", 1, true);
        assertEquals("a", t.left());
        assertEquals(Integer.valueOf(1), t.middle());
        assertEquals(Boolean.TRUE, t.right());
    }

    @Test
    public void testTriple_functionality() throws Exception {
        Throwables.TriFunction<String, Integer, Boolean, Triple<String, Integer, Boolean>, Exception> tripleFn = Fnn.triple();
        Triple<String, Integer, Boolean> t = tripleFn.apply("a", 1, true);
        assertEquals("a", t.left());
        assertEquals(1, (int) t.middle());
        assertTrue(t.right());
    }

    // --- tuple1() ---

    @Test
    public void testTuple1() throws Exception {
        Throwables.Function<String, Tuple1<String>, Exception> t1Fn = Fnn.tuple1();
        assertNotNull(t1Fn);

        Tuple1<String> t = t1Fn.apply("hello");
        assertEquals("hello", t._1);
    }

    // --- tuple2() ---

    @Test
    public void testTuple2() throws Exception {
        Throwables.BiFunction<String, Integer, Tuple2<String, Integer>, Exception> t2Fn = Fnn.tuple2();
        assertNotNull(t2Fn);

        Tuple2<String, Integer> t = t2Fn.apply("abc", 3);
        assertEquals("abc", t._1);
        assertEquals(Integer.valueOf(3), t._2);
    }

    // --- tuple3() ---

    @Test
    public void testTuple3() throws Exception {
        Throwables.TriFunction<String, Integer, Boolean, Tuple3<String, Integer, Boolean>, Exception> t3Fn = Fnn.tuple3();
        assertNotNull(t3Fn);

        Tuple3<String, Integer, Boolean> t = t3Fn.apply("x", 10, false);
        assertEquals("x", t._1);
        assertEquals(Integer.valueOf(10), t._2);
        assertEquals(Boolean.FALSE, t._3);
    }

    // --- emptyAction() ---

    @Test
    public void testEmptyAction() throws Exception {
        Throwables.Runnable<Exception> action = Fnn.emptyAction();
        assertNotNull(action);
        // Should not throw
        action.run();
    }

    @Test
    public void testEmptyAction_functionality() throws Throwable {
        Throwables.Runnable<?> action = Fnn.emptyAction();
        assertNotNull(action);
        action.run(); // should do nothing, no exception
    }

    // --- doNothing() ---

    @Test
    public void testDoNothing() throws Exception {
        Throwables.Consumer<String, Exception> consumer = Fnn.doNothing();
        assertNotNull(consumer);
        // Should not throw
        consumer.accept("anything");
        consumer.accept(null);
    }

    // --- throwRuntimeException() ---

    @Test
    public void testThrowRuntimeException() throws Exception {
        Consumer<Object, RuntimeException> consumer = Fnn.throwRuntimeException("test error");
        assertNotNull(consumer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept("anything"));
        assertTrue(ex.getMessage().contains("test error"));
    }

    @Test
    public void testThrowRuntimeException_withString() {
        Throwables.Consumer<Object, RuntimeException> consumer = Fnn.throwRuntimeException("error msg");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept("ignored"));
        assertEquals("error msg", ex.getMessage());
    }

    @Test
    public void testThrowException_withString() {
        Throwables.Consumer<Object, Exception> consumer = Fnn.throwException("checked error");
        Exception ex = assertThrows(Exception.class, () -> consumer.accept("ignored"));
        assertEquals("checked error", ex.getMessage());
    }

    @Test
    public void testSleep_shortDuration() throws Exception {
        Throwables.Consumer<String, Exception> sleeper = Fnn.sleep(1);
        assertNotNull(sleeper);
        long start = System.currentTimeMillis();
        sleeper.accept("ignored");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 0);
    }

    @Test
    public void testSleepUninterruptibly_shortDuration() throws Exception {
        Throwables.Consumer<String, Exception> sleeper = Fnn.sleepUninterruptibly(1);
        assertNotNull(sleeper);
        sleeper.accept("ignored");
    }

    // --- close() ---

    @Test
    public void testClose() throws Exception {
        Throwables.Consumer<AutoCloseable, Exception> closeFn = Fnn.close();
        assertNotNull(closeFn);

        boolean[] closed = { false };
        AutoCloseable resource = () -> closed[0] = true;
        closeFn.accept(resource);
        assertTrue(closed[0]);
    }

    @Test
    public void testClose_null() throws Exception {
        Throwables.Consumer<AutoCloseable, Exception> closeFn = Fnn.close();
        // Should not throw on null
        closeFn.accept(null);
    }

    @Test
    public void testClose_callsClose() throws Exception {
        boolean[] closed = { false };
        AutoCloseable ac = () -> closed[0] = true;
        Throwables.Consumer<AutoCloseable, Exception> closer = Fnn.close();
        closer.accept(ac);
        assertTrue(closed[0]);
    }

    @Test
    public void testClose_nullAutoCloseable() throws Exception {
        Throwables.Consumer<AutoCloseable, Exception> closeConsumer = Fnn.close();
        // should not throw when given null (via lambda$0)
        closeConsumer.accept(null);
    }

    @Test
    public void testClose_validAutoCloseable() throws Exception {
        Throwables.Consumer<AutoCloseable, Exception> closeConsumer = Fnn.close();
        final boolean[] closed = { false };
        AutoCloseable resource = () -> closed[0] = true;
        closeConsumer.accept(resource);
        assertTrue(closed[0]);
    }

    // --- closeQuietly() ---

    @Test
    public void testCloseQuietly() throws Exception {
        Throwables.Consumer<AutoCloseable, Exception> closeFn = Fnn.closeQuietly();
        assertNotNull(closeFn);

        // Should not throw even if close() throws
        AutoCloseable throwingResource = () -> {
            throw new IOException("test");
        };
        closeFn.accept(throwingResource);
    }

    @Test
    public void testCloseQuietly_callsCloseQuietly() throws Exception {
        boolean[] closed = { false };
        AutoCloseable ac = () -> closed[0] = true;
        Throwables.Consumer<AutoCloseable, Exception> closer = Fnn.closeQuietly();
        closer.accept(ac);
        assertTrue(closed[0]);
    }

    // --- println() ---

    @Test
    public void testPrintln() throws Exception {
        Throwables.Consumer<String, Exception> printlnFn = Fnn.println();
        assertNotNull(printlnFn);
        // Should not throw
        printlnFn.accept("test");
        printlnFn.accept(null);
    }

    @Test
    public void testPrintln_withSeparator() throws Exception {
        // Should not throw; just verify the BiConsumer works
        Throwables.BiConsumer<String, Integer, Exception> printer = Fnn.println("=");
        assertNotNull(printer);
        printer.accept("key", 42); // prints "key=42"
    }

    // --- isNull() ---

    @Test
    public void testIsNull() throws Exception {
        Throwables.Predicate<Object, Exception> pred = Fnn.isNull();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertFalse(pred.test("not null"));
        assertFalse(pred.test(0));
        assertFalse(pred.test(""));
    }

    // --- isEmpty() ---

    @Test
    public void testIsEmpty() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.isEmpty();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(""));
        assertFalse(pred.test("a"));
        assertFalse(pred.test(" "));
    }

    // --- isBlank() ---

    @Test
    public void testIsBlank() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.isBlank();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(""));
        assertTrue(pred.test("   "));
        assertFalse(pred.test("a"));
        assertFalse(pred.test(" a "));
    }

    // --- isEmptyArray() ---

    @Test
    public void testIsEmptyArray() throws Exception {
        Throwables.Predicate<String[], Exception> pred = Fnn.isEmptyArray();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(new String[0]));
        assertFalse(pred.test(new String[] { "a" }));
    }

    // --- isEmptyCollection() ---

    @Test
    @SuppressWarnings("rawtypes")
    public void testIsEmptyCollection() throws Exception {
        Throwables.Predicate<List, Exception> pred = Fnn.isEmptyCollection();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(new ArrayList<>()));
        assertFalse(pred.test(N.asList("a")));
    }

    // --- isEmptyMap() ---

    @Test
    @SuppressWarnings("rawtypes")
    public void testIsEmptyMap() throws Exception {
        Throwables.Predicate<Map, Exception> pred = Fnn.isEmptyMap();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(new HashMap<>()));
        assertFalse(pred.test(N.asMap("k", "v")));
    }

    // --- notNull() ---

    @Test
    public void testNotNull() throws Exception {
        Throwables.Predicate<Object, Exception> pred = Fnn.notNull();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertTrue(pred.test("not null"));
        assertTrue(pred.test(0));
        assertTrue(pred.test(""));
    }

    // --- notEmpty() ---

    @Test
    public void testNotEmpty() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.notEmpty();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertFalse(pred.test(""));
        assertTrue(pred.test("a"));
        assertTrue(pred.test(" "));
    }

    // --- notBlank() ---

    @Test
    public void testNotBlank() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.notBlank();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertFalse(pred.test(""));
        assertFalse(pred.test("   "));
        assertTrue(pred.test("a"));
        assertTrue(pred.test(" a "));
    }

    // --- notEmptyArray() ---

    @Test
    public void testNotEmptyArray() throws Exception {
        Throwables.Predicate<String[], Exception> pred = Fnn.notEmptyArray();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertFalse(pred.test(new String[0]));
        assertTrue(pred.test(new String[] { "a" }));
    }

    // --- notEmptyCollection() ---

    @Test
    @SuppressWarnings("rawtypes")
    public void testNotEmptyCollection() throws Exception {
        Throwables.Predicate<List, Exception> pred = Fnn.notEmptyCollection();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertFalse(pred.test(new ArrayList<>()));
        assertTrue(pred.test(N.asList("a")));
    }

    // --- notEmptyMap() ---

    @Test
    @SuppressWarnings("rawtypes")
    public void testNotEmptyMap() throws Exception {
        Throwables.Predicate<Map, Exception> pred = Fnn.notEmptyMap();
        assertNotNull(pred);
        assertFalse(pred.test(null));
        assertFalse(pred.test(new HashMap<>()));
        assertTrue(pred.test(N.asMap("k", "v")));
    }

    @Test
    public void testIgnoringMerger() throws Exception {
        Throwables.BinaryOperator<String, Exception> merger = Fnn.ignoringMerger();
        assertNotNull(merger);
        assertEquals("first", merger.apply("first", "second"));
    }

    @Test
    public void testReplacingMerger() throws Exception {
        Throwables.BinaryOperator<String, Exception> merger = Fnn.replacingMerger();
        assertNotNull(merger);
        assertEquals("second", merger.apply("first", "second"));
    }

    @Test
    public void testAtMost() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.atMost(3);
        assertTrue(pred.test("a"));
        assertTrue(pred.test("b"));
        assertTrue(pred.test("c"));
        assertFalse(pred.test("d"));
        assertFalse(pred.test("e"));
    }

    @Test
    public void testAtMost_zero() throws Exception {
        Throwables.Predicate<String, Exception> pred = Fnn.atMost(0);
        assertFalse(pred.test("a"));
    }

    @Test
    public void testFrom_Supplier() throws Exception {
        java.util.function.Supplier<String> supplier = () -> "test";
        Throwables.Supplier<String, Exception> ts = Fnn.from(supplier);
        assertEquals("test", ts.get());
    }

    @Test
    public void testFrom_IntFunction() throws Exception {
        java.util.function.IntFunction<String> func = i -> "val" + i;
        Throwables.IntFunction<String, Exception> tf = Fnn.from(func);
        assertEquals("val5", tf.apply(5));
    }

    @Test
    public void testFrom_Predicate() throws Exception {
        java.util.function.Predicate<String> pred = s -> s.isEmpty();
        Throwables.Predicate<String, Exception> tp = Fnn.from(pred);
        assertTrue(tp.test(""));
        assertFalse(tp.test("hello"));
    }

    @Test
    public void testFrom_BiPredicate() throws Exception {
        java.util.function.BiPredicate<String, Integer> pred = (s, i) -> s.length() == i;
        Throwables.BiPredicate<String, Integer, Exception> tp = Fnn.from(pred);
        assertTrue(tp.test("hello", 5));
        assertFalse(tp.test("hi", 5));
    }

    @Test
    public void testFrom_Consumer() throws Exception {
        List<String> results = new ArrayList<>();
        java.util.function.Consumer<String> consumer = s -> results.add(s);
        Throwables.Consumer<String, Exception> tc = Fnn.from(consumer);
        tc.accept("hello");
        assertEquals(1, results.size());
        assertEquals("hello", results.get(0));
    }

    @Test
    public void testFrom_Function() throws Exception {
        java.util.function.Function<String, Integer> func = s -> s.length();
        Throwables.Function<String, Integer, Exception> tf = Fnn.from(func);
        assertEquals(5, (int) tf.apply("hello"));
    }

    @Test
    public void testFrom_BiFunction() throws Exception {
        java.util.function.BiFunction<String, Integer, String> func = (s, i) -> s + i;
        Throwables.BiFunction<String, Integer, String, Exception> tf = Fnn.from(func);
        assertEquals("hello5", tf.apply("hello", 5));
    }

    @Test
    public void testFrom_UnaryOperator() throws Exception {
        java.util.function.UnaryOperator<String> op = s -> s.toUpperCase();
        Throwables.UnaryOperator<String, Exception> to = Fnn.from(op);
        assertEquals("HELLO", to.apply("hello"));
    }

    @Test
    public void testFrom_BinaryOperator() throws Exception {
        java.util.function.BinaryOperator<Integer> op = (a, b) -> a + b;
        Throwables.BinaryOperator<Integer, Exception> to = Fnn.from(op);
        assertEquals(7, (int) to.apply(3, 4));
    }

    @Test
    public void testFrom_Supplier_wraps() throws Exception {
        // Test the wrapping path - regular java.util.function.Supplier
        java.util.function.Supplier<String> supplier = () -> "wrapped";
        Throwables.Supplier<String, Exception> result = Fnn.from(supplier);
        assertNotNull(result);
        assertEquals("wrapped", result.get());
    }

    @Test
    public void testFrom_Predicate_wraps() throws Exception {
        java.util.function.Predicate<String> pred = s -> s.length() > 3;
        Throwables.Predicate<String, Exception> result = Fnn.from(pred);
        assertNotNull(result);
        assertTrue(result.test("hello"));
        assertFalse(result.test("hi"));
    }

    @Test
    public void testFrom_BiPredicate_wraps() throws Exception {
        java.util.function.BiPredicate<String, Integer> pred = (s, i) -> s.length() > i;
        Throwables.BiPredicate<String, Integer, Exception> result = Fnn.from(pred);
        assertNotNull(result);
        assertTrue(result.test("hello", 3));
        assertFalse(result.test("hi", 3));
    }

    @Test
    public void testFrom_Consumer_wraps() throws Exception {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = s -> list.add(s);
        Throwables.Consumer<String, Exception> result = Fnn.from(consumer);
        assertNotNull(result);
        result.accept("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testFrom_Function_wraps() throws Exception {
        java.util.function.Function<String, Integer> func = s -> s.length() * 2;
        Throwables.Function<String, Integer, Exception> result = Fnn.from(func);
        assertNotNull(result);
        assertEquals(Integer.valueOf(10), result.apply("hello"));
    }

    @Test
    public void testFrom_BiFunction_wraps() throws Exception {
        java.util.function.BiFunction<Integer, Integer, Integer> func = (a, b) -> a * b;
        Throwables.BiFunction<Integer, Integer, Integer, Exception> result = Fnn.from(func);
        assertNotNull(result);
        assertEquals(Integer.valueOf(12), result.apply(3, 4));
    }

    @Test
    public void testFrom_UnaryOperator_wraps() throws Exception {
        java.util.function.UnaryOperator<String> op = s -> s.toLowerCase();
        Throwables.UnaryOperator<String, Exception> result = Fnn.from(op);
        assertNotNull(result);
        assertEquals("hello", result.apply("HELLO"));
    }

    @Test
    public void testFrom_BinaryOperator_wraps() throws Exception {
        java.util.function.BinaryOperator<String> op = (a, b) -> a + b;
        Throwables.BinaryOperator<String, Exception> result = Fnn.from(op);
        assertNotNull(result);
        assertEquals("hello world", result.apply("hello ", "world"));
    }

    @Test
    public void testFrom_IntFunction_wraps() throws Exception {
        java.util.function.IntFunction<String> func = i -> "number" + i;
        Throwables.IntFunction<String, Exception> result = Fnn.from(func);
        assertNotNull(result);
        assertEquals("number42", result.apply(42));
    }

    @Test
    public void testC_Callable() throws Exception {
        Throwables.Callable<String, Exception> callable = () -> "value";
        Throwables.Callable<String, Exception> result = Fnn.c(callable);

        assertSame(callable, result);
        assertEquals("value", result.call());
    }

    @Test
    public void testC_Callable_Null() {
        assertThrows(IllegalArgumentException.class, () -> Fnn.c((Throwables.Callable<String, Exception>) null));
    }

    @Test
    public void testJr2r_regularRunnable() throws Throwable {
        boolean[] ran = { false };
        Runnable javaRunnable = () -> ran[0] = true;
        Throwables.Runnable<?> r = Fnn.jr2r(javaRunnable);
        r.run();
        assertTrue(ran[0]);
    }

    @Test
    public void testJr2r_throwableRunnable() throws Throwable {
        // If it's already a Throwables.Runnable, should return same
        boolean[] ran = { false };
        java.lang.Runnable tr = () -> ran[0] = true;
        Throwables.Runnable<?> r = Fnn.jr2r(tr);
        r.run();
        assertTrue(ran[0]);
    }

    @Test
    public void testJr2r_wrapsRegularRunnable() throws Exception {
        // Tests the wrapping path for jr2r - non-Throwables.Runnable
        final int[] executed = { 0 };
        java.lang.Runnable runnable = () -> executed[0]++;
        Throwables.Runnable<Exception> result = Fnn.jr2r(runnable);
        assertNotNull(result);
        result.run();
        assertEquals(1, executed[0]);
    }

    @Test
    public void testR2jr_regularThrowableRunnable() throws Exception {
        boolean[] ran = { false };
        Throwables.Runnable<Exception> tr = () -> ran[0] = true;
        Runnable r = Fnn.r2jr(tr);
        r.run();
        assertTrue(ran[0]);
    }

    @Test
    public void testR2jr_wrapsThrowableRunnable() throws Exception {
        // Tests that r2jr wraps a Throwables.Runnable into a java.lang.Runnable
        final int[] executed = { 0 };
        Throwables.Runnable<Exception> tr = () -> executed[0]++;
        java.lang.Runnable result = Fnn.r2jr(tr);
        assertNotNull(result);
        result.run();
        assertEquals(1, executed[0]);
    }

    @Test
    public void testJc2c_regularCallable() throws Exception {
        java.util.concurrent.Callable<String> callable = () -> "hello";
        Throwables.Callable<String, Exception> c = Fnn.jc2c(callable);
        assertEquals("hello", c.call());
    }

    @Test
    public void testJc2c_wrapsCallable() throws Exception {
        // Tests the wrapping path for jc2c
        java.util.concurrent.Callable<String> callable = () -> "hello_new";
        Throwables.Callable<String, Exception> result = Fnn.jc2c(callable);
        assertNotNull(result);
        assertEquals("hello_new", result.call());
    }

    @Test
    public void testC2jc_regularThrowableCallable() throws Exception {
        Throwables.Callable<String, Exception> tc = () -> "world";
        java.util.concurrent.Callable<String> c = Fnn.c2jc(tc);
        assertEquals("world", c.call());
    }

    @Test
    public void testC2jc_wrapsThrowableCallable() throws Exception {
        // Tests that c2jc wraps a Throwables.Callable into a java.util.concurrent.Callable
        Throwables.Callable<String, Exception> tc = () -> "result_new";
        java.util.concurrent.Callable<String> result = Fnn.c2jc(tc);
        assertNotNull(result);
        assertEquals("result_new", result.call());
    }
}
