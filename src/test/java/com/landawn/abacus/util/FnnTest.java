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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables.Consumer;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;

public class FnnTest extends TestBase {

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

    // --- entry() ---

    @Test
    public void testEntry() throws Exception {
        Throwables.BiFunction<String, Integer, Map.Entry<String, Integer>, Exception> entryFn = Fnn.entry();
        assertNotNull(entryFn);

        Map.Entry<String, Integer> entry = entryFn.apply("key", 42);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
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

    // --- doNothing() ---

    @Test
    public void testDoNothing() throws Exception {
        Throwables.Consumer<String, Exception> consumer = Fnn.doNothing();
        assertNotNull(consumer);
        // Should not throw
        consumer.accept("anything");
        consumer.accept(null);
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

    // --- isEmptyArray() ---

    @Test
    public void testIsEmptyArray() throws Exception {
        Throwables.Predicate<String[], Exception> pred = Fnn.isEmptyArray();
        assertNotNull(pred);
        assertTrue(pred.test(null));
        assertTrue(pred.test(new String[0]));
        assertFalse(pred.test(new String[] { "a" }));
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

    // --- throwRuntimeException() ---

    @Test
    public void testThrowRuntimeException() throws Exception {
        Consumer<Object, RuntimeException> consumer = Fnn.throwRuntimeException("test error");
        assertNotNull(consumer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept("anything"));
        assertTrue(ex.getMessage().contains("test error"));
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
}
