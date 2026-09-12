package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WrapperTest extends TestBase {

    private static final class EmptyArrayComponent {
    }

    @Test
    public void testOf() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);
        assertSame(array, wrapper.value());
        assertEquals(wrapper, Wrapper.of(new int[] { 1, 2, 3 }));
        assertNotEquals(wrapper, Wrapper.of(new int[] { 1, 2, 4 }));

        String value = "hello";
        Wrapper<String> nonArray = Wrapper.of(value);
        assertSame(value, nonArray.value());
        assertEquals(nonArray, Wrapper.of("hello"));
        assertEquals(nonArray.hashCode(), Wrapper.of("hello").hashCode());

        Wrapper<int[]> n1 = Wrapper.of(null);
        Wrapper<int[]> n2 = Wrapper.of(null);
        assertSame(n1, n2);
        assertEquals(null, n1.value());
        assertEquals(n1.hashCode(), n2.hashCode());
        assertTrue(n1.equals(n2));
        assertTrue(n1.toString().contains("Wrapper["));
    }

    @Test
    public void testOf_Arrays() {
        assertEquals(Wrapper.of(new int[] { 1, 2, 3 }), Wrapper.of(new int[] { 1, 2, 3 }));
        assertEquals(Wrapper.of(new byte[] { 1, 2, 3 }), Wrapper.of(new byte[] { 1, 2, 3 }));
        assertEquals(Wrapper.of(new short[] { 1, 2, 3 }), Wrapper.of(new short[] { 1, 2, 3 }));
        assertEquals(Wrapper.of(new long[] { 1L, 2L, 3L }), Wrapper.of(new long[] { 1L, 2L, 3L }));
        assertEquals(Wrapper.of(new float[] { 1.0f, 2.0f, 3.0f }), Wrapper.of(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(Wrapper.of(new double[] { 1.0, 2.0, 3.0 }), Wrapper.of(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(Wrapper.of(new char[] { 'a', 'b', 'c' }), Wrapper.of(new char[] { 'a', 'b', 'c' }));
        assertEquals(Wrapper.of(new boolean[] { true, false, true }), Wrapper.of(new boolean[] { true, false, true }));
        assertEquals(Wrapper.of(new String[] { "a", "b", "c" }), Wrapper.of(new String[] { "a", "b", "c" }));
        assertNotEquals(Wrapper.of(new String[] { "a", "b", "c" }), Wrapper.of(new String[] { "a", "b", "d" }));

        int[][] matrix = { { 1, 2 }, { 3, 4 } };
        assertEquals(Wrapper.of(matrix), Wrapper.of(new int[][] { { 1, 2 }, { 3, 4 } }));
        assertNotEquals(Wrapper.of(matrix), Wrapper.of(new int[][] { { 1, 2 }, { 3, 5 } }));
        assertEquals(Wrapper.of(matrix).hashCode(), Wrapper.of(new int[][] { { 1, 2 }, { 3, 4 } }).hashCode());

        int[][][] cube = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        assertEquals(Wrapper.of(cube), Wrapper.of(new int[][][] { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } }));
        assertNotEquals(Wrapper.of(cube), Wrapper.of(new int[][][] { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 9 } } }));

        assertNotEquals(Wrapper.of(new int[0]), Wrapper.of(new long[0]));
        assertNotEquals(Wrapper.of(new int[0]), Wrapper.of(new String[0]));
        assertNotEquals(Wrapper.of(new long[0]), Wrapper.of(new String[0]));
    }

    @Test
    public void testOf_EmptyArrayCaching() {
        assertSame(Wrapper.of(new boolean[0]), Wrapper.of(new boolean[0]));
        assertSame(Wrapper.of(new int[0]), Wrapper.of(new int[0]));
        assertSame(Wrapper.of(new String[0]), Wrapper.of(new String[0]));
        assertSame(Wrapper.of(new double[0]), Wrapper.of(new double[0]));
        assertSame(Wrapper.of(new char[0]), Wrapper.of(new char[0]));
        assertSame(Wrapper.of(new float[0]), Wrapper.of(new float[0]));
    }

    @Test
    public void testOf_Custom() {
        ToIntFunction<String> firstCharHash = s -> s.charAt(0);
        BiPredicate<String, String> firstCharEq = (s1, s2) -> s1.charAt(0) == s2.charAt(0);
        Wrapper<String> apple = Wrapper.of("apple", firstCharHash, firstCharEq);
        assertEquals(apple, Wrapper.of("apricot", firstCharHash, firstCharEq));
        assertEquals(apple.hashCode(), Wrapper.of("apricot", firstCharHash, firstCharEq).hashCode());
        assertNotEquals(apple, Wrapper.of("banana", firstCharHash, firstCharEq));

        ToIntFunction<String> lengthHash = String::length;
        BiPredicate<String, String> ignoreCase = String::equalsIgnoreCase;
        Wrapper<String> custom = Wrapper.of("TEST", lengthHash, ignoreCase);
        assertEquals("TEST", custom.value());
        assertEquals(4, custom.hashCode());
        assertEquals(custom, Wrapper.of("test", lengthHash, ignoreCase));
        assertNotEquals(custom, Wrapper.of("other", lengthHash, ignoreCase));

        Function<String, String> branded = s -> "CUSTOM[" + s + "]";
        assertEquals("Wrapper[CUSTOM[test]]", Wrapper.of("test", String::hashCode, String::equals, branded).toString());
        assertEquals("Wrapper[***hello***]", Wrapper.of("hello", String::hashCode, String::equals, s -> "***" + s + "***").toString());
        assertTrue(Wrapper.of("test", String::hashCode, String::equals, s -> "Value is: " + s).toString().contains("Value is: test"));

        ToIntFunction<String> nullSafeHash = s -> s == null ? 0 : s.hashCode();
        BiPredicate<String, String> nullSafeEq = (s1, s2) -> s1 == null ? s2 == null : s1.equals(s2);
        Wrapper<String> nullCustom = Wrapper.of(null, nullSafeHash, nullSafeEq);
        assertEquals(null, nullCustom.value());
        assertEquals(0, nullCustom.hashCode());
        assertEquals("Wrapper[NULL]", Wrapper.of(null, nullSafeHash, nullSafeEq, s -> s == null ? "NULL" : s).toString());

        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("test", null, String::equals));
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("test", String::hashCode, null));
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("test", null, String::equals, String::toUpperCase));
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("test", String::hashCode, null, String::toUpperCase));
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("test", String::hashCode, String::equals, null));
    }

    @Test
    public void testEqualsHashCodeToString() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        Wrapper<int[]> wa = Wrapper.of(a);
        Wrapper<int[]> wb = Wrapper.of(b);
        Wrapper<int[]> wc = Wrapper.of(new int[] { 1, 2, 4 });
        assertTrue(a != b);
        assertEquals(wa, wa);
        assertEquals(wa, wb);
        assertEquals(wb, wa);
        assertNotEquals(wa, wc);
        assertFalse(wa.equals(null));
        assertFalse(wa.equals("not a wrapper"));
        assertEquals(wa.hashCode(), wb.hashCode());
        assertNotEquals(0, wa.hashCode());
        assertEquals(wa.hashCode(), wa.hashCode());
        assertTrue(wa.toString().startsWith("Wrapper["));
        assertTrue(wa.toString().contains("[1, 2, 3]"));

        Wrapper<int[]> w1 = Wrapper.of(a);
        Wrapper<int[]> w2 = Wrapper.of(b);
        Wrapper<int[]> w3 = Wrapper.of(new int[] { 1, 2, 3 });
        assertTrue(w1.equals(w2) && w2.equals(w3) && w1.equals(w3));

        ToIntFunction<String> constantHashFn = s -> 42;
        Wrapper<String> constantHash = Wrapper.of("hello", constantHashFn, String::equals);
        assertEquals(42, constantHash.hashCode());
        assertEquals(42, Wrapper.of("world", constantHashFn, String::equals).hashCode());

        ToIntFunction<String> lengthHash = String::length;
        BiPredicate<String, String> equalByLength = (s1, s2) -> s1.length() == s2.length();
        Wrapper<String> byLength = Wrapper.of("abc", lengthHash, equalByLength);
        assertTrue(byLength.equals(Wrapper.of("xyz", lengthHash, equalByLength)));
        assertFalse(byLength.equals(Wrapper.of("abcd", lengthHash, equalByLength)));
    }

    @Test
    public void testEquals_EdgeCase() {
        Wrapper<String> deep = Wrapper.of("value");
        Wrapper<String> customRejecting = Wrapper.of("value", String::hashCode, (left, right) -> false);
        Wrapper<String> customAcceptingWithDifferentHash = Wrapper.of("value", value -> 7, String::equals);
        assertFalse(deep.equals(customRejecting));
        assertFalse(customRejecting.equals(deep));
        assertFalse(deep.equals(customAcceptingWithDifferentHash));
        assertFalse(customAcceptingWithDifferentHash.equals(deep));

        Wrapper<String> oneWay = Wrapper.of("left", value -> 0, (left, right) -> true);
        Wrapper<String> reverseRejecting = Wrapper.of("right", value -> 0, (left, right) -> false);
        assertFalse(oneWay.equals(reverseRejecting));
        assertFalse(reverseRejecting.equals(oneWay));

        ToIntFunction<String> hashByLengthA = String::length;
        ToIntFunction<String> hashByLengthB = String::length;
        BiPredicate<String, String> equalByLengthA = (left, right) -> left.length() == right.length();
        BiPredicate<String, String> equalByLengthB = (left, right) -> left.length() == right.length();
        Wrapper<String> strategyA = Wrapper.of("one", hashByLengthA, equalByLengthA);
        Wrapper<String> strategyB = Wrapper.of("two", hashByLengthB, equalByLengthB);
        assertFalse(strategyA.equals(strategyB));
        assertFalse(strategyB.equals(strategyA));

        Object o1 = new Object();
        Object o2 = new Object();
        ToIntFunction<Object> idHash = System::identityHashCode;
        BiPredicate<Object, Object> idEq = (x, y) -> x == y;
        Wrapper<Object> w1 = Wrapper.of(o1, idHash, idEq);
        assertTrue(w1.equals(Wrapper.of(o1, idHash, idEq)));
        assertEquals(w1.hashCode(), Wrapper.of(o1, idHash, idEq).hashCode());
        assertFalse(w1.equals(Wrapper.of(o2, idHash, idEq)));
    }

    @Test
    public void testCollections() {
        Map<Wrapper<int[]>, String> map = new HashMap<>();
        map.put(Wrapper.of(new int[] { 1, 2, 3 }), "value1");
        map.put(Wrapper.of(new int[] { 4, 5, 6 }), "value3");
        assertEquals("value1", map.get(Wrapper.of(new int[] { 1, 2, 3 })));
        assertEquals("value3", map.get(Wrapper.of(new int[] { 4, 5, 6 })));
        assertEquals(null, map.get(Wrapper.of(new int[] { 9, 9, 9 })));

        Set<Wrapper<int[]>> intSet = new HashSet<>();
        intSet.add(Wrapper.of(new int[] { 1, 2, 3 }));
        assertTrue(intSet.contains(Wrapper.of(new int[] { 1, 2, 3 })));

        Set<Wrapper<String[]>> set = new HashSet<>();
        set.add(Wrapper.of(new String[] { "a", "b", "c" }));
        set.add(Wrapper.of(new String[] { "a", "b", "c" }));
        set.add(Wrapper.of(new String[] { "x", "y", "z" }));
        assertEquals(2, set.size());
        assertTrue(set.contains(Wrapper.of(new String[] { "a", "b", "c" })));
    }

    @Test
    public void testEmptyArrayPool_Concurrent() throws Exception {
        Wrapper.ArrayWrapper.WRAPPER_POOL.remove(EmptyArrayComponent.class);

        int threadCount = 24;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Wrapper<EmptyArrayComponent[]>>> futures = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return Wrapper.of(new EmptyArrayComponent[0]);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            Wrapper<EmptyArrayComponent[]> expected = futures.get(0).get();
            for (Future<Wrapper<EmptyArrayComponent[]>> future : futures) {
                assertSame(expected, future.get());
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
