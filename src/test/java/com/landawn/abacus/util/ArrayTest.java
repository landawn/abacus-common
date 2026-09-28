package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ArrayTest extends ArrayTestSupport {

    @Test
    public void testUnboxPreservesExistingWrappers() {
        final Integer[] wrappers = { 1000, null, -2000 };
        final Integer first = wrappers[0];
        final Integer last = wrappers[2];

        assertArrayEquals(new int[] { 1000, 0, -2000 }, Array.unbox(wrappers));
        assertSame(first, wrappers[0]);
        assertSame(null, wrappers[1]);
        assertSame(last, wrappers[2]);
    }

    @Test
    public void testArrayUtil_CannotInstantiate() {
        assertTrue(Modifier.isFinal(Array.ArrayUtil.class.getModifiers()));
        assertTrue(Modifier.isPrivate(Array.class.getDeclaredConstructors()[0].getModifiers()));
    }

    @Test
    public void testNewInstance() {
        final int[] ints = Array.newInstance(int.class, 5);
        assertEquals(5, ints.length);
        assertEquals(0, ints[0]);

        final String[] strings = Array.newInstance(String.class, 3);
        assertEquals(3, strings.length);

        final Integer[][] nested = Array.newInstance(Integer[].class, 2);
        assertEquals(2, nested.length);

        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(null, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(void.class, 1));
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(int.class, -1));
    }

    @Test
    public void testNewInstance_EmptyArrayCache() {
        assertSame(Array.newInstance(int.class, 0), Array.newInstance(int.class, 0));
        assertSame(Array.newInstance(String.class, 0), Array.newInstance(String.class, 0));
        assertNotSame(Array.newInstance(String.class, 1), Array.newInstance(String.class, 1));
    }

    @Test
    public void testNewInstance_DoesNotPinCallerClasses() {
        assertFalse(CommonUtil.CLASS_EMPTY_ARRAY.containsKey(EmptyArrayCacheProbe.class));
        final EmptyArrayCacheProbe[] first = Array.newInstance(EmptyArrayCacheProbe.class, 0);
        final EmptyArrayCacheProbe[] second = Array.newInstance(EmptyArrayCacheProbe.class, 0);
        assertEquals(0, first.length);
        assertNotSame(first, second);
        assertFalse(CommonUtil.CLASS_EMPTY_ARRAY.containsKey(EmptyArrayCacheProbe.class));
    }

    @Test
    public void testNewInstance_Dimensions() {
        final int[][] arr2D = Array.newInstance(int.class, 2, 3);
        assertEquals(2, arr2D.length);
        assertEquals(3, arr2D[0].length);

        final boolean[][][] arr3D = Array.newInstance(boolean.class, 2, 3, 4);
        assertEquals(2, arr3D.length);
        assertEquals(3, arr3D[0].length);
        assertEquals(4, arr3D[0][0].length);

        final String[][] strings = Array.newInstance(String.class, 2, 2);
        assertEquals(2, strings.length);

        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(null, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(int.class, new int[0]));
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(int.class, 2, -1));
    }

    @Test
    public void testGetLength() {
        assertEquals(0, Array.getLength(null));
        assertEquals(0, Array.getLength(new int[0]));
        assertEquals(3, Array.getLength(new int[] { 1, 2, 3 }));
        assertEquals(2, Array.getLength(new String[] { "a", "b" }));
        assertEquals(2, Array.getLength(new boolean[] { true, false }));
        assertEquals(1, Array.getLength(new Object[] { null }));
        assertThrows(IllegalArgumentException.class, () -> Array.getLength("not-an-array"));
    }

    @Test
    public void testAsList() {
        final String[] a = { "a", "b", "c" };
        final List<String> list = Array.asList(a);
        assertEquals(3, list.size());
        list.set(0, "x");
        assertEquals("x", a[0]);

        assertTrue(Array.asList((String[]) null).isEmpty());
        assertTrue(Array.asList(new String[0]).isEmpty());
        assertEquals(1, Array.asList("only").size());
        assertThrows(UnsupportedOperationException.class, () -> Array.asList("a", "b").add("c"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOfValues() {
        final Integer[] values = Array.ofValues(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, values);
        assertSame(values, Array.ofValues(values));
        assertEquals(null, Array.ofValues((Integer[]) null));

        final String[] of = Array.oF("a", "b");
        assertArrayEquals(new String[] { "a", "b" }, of);
        assertSame(of, Array.oF(of));
    }

    @Test
    public void testRandom() {
        assertEquals(0, Array.random(0).length);
        assertEquals(5, Array.random(5).length);
        assertThrows(IllegalArgumentException.class, () -> Array.random(-1));

        final int[] ranged = Array.random(3, 7, 80);
        assertEquals(80, ranged.length);
        for (final int v : ranged) {
            assertTrue(v >= 3 && v < 7);
        }

        assertEquals(0, Array.random(0, 10, 0).length);
        assertThrows(IllegalArgumentException.class, () -> Array.random(0, 10, -1));
        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 0, 1));
    }

    @Test
    public void testRandom_FullIntRange() {
        final int[] values = Array.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 1000);
        assertEquals(1000, values.length);
        boolean hasNegative = false;
        boolean hasNonNegative = false;
        boolean hasDistinct = false;
        for (final int value : values) {
            hasNegative |= value < 0;
            hasNonNegative |= value >= 0;
            hasDistinct |= value != values[0];
        }
        assertTrue(hasNegative);
        assertTrue(hasNonNegative);
        assertTrue(hasDistinct);
    }

    @Test
    public void testRandom_RandomnessSource() {
        // Contract pin for the javadoc: Array.random draws from ThreadLocalRandom.current() (owner decision
        // 2026-09-26; it used to be a shared SecureRandom held by N). There is no shared generator field any more
        // (see testRandom_noSharedStaticGenerator); the results must still have the requested length and range
        // and vary between elements.
        final int[] values = Array.random(64);
        assertEquals(64, values.length);
        boolean hasDistinct = false;
        for (final int value : values) {
            hasDistinct |= value != values[0];
        }
        assertTrue(hasDistinct);

        assertEquals(4, Array.random(4).length);
        final int[] ranged = Array.random(0, 10, 200);
        assertEquals(200, ranged.length);
        for (final int value : ranged) {
            assertTrue(value >= 0 && value < 10, String.valueOf(value));
        }
    }

    // ---- perf review 2026-09-26 G018 begin ----
    // G018-01: pins random(int) across the bulk-chunk boundaries - length, freshness and full 32-bit coverage.
    @Test
    public void testRandom_chunkBoundariesAndBitCoverage() {
        final int[] empty1 = Array.random(0);
        final int[] empty2 = Array.random(0);
        assertEquals(0, empty1.length);
        assertNotSame(empty1, empty2);

        for (final int length : new int[] { 1, 2, 3, 2047, 2048, 2049, 4096, 5000 }) {
            final int[] values = Array.random(length);
            assertEquals(length, values.length);
            assertNotSame(values, Array.random(length));
        }

        final int[] values = Array.random(6000);
        int orAll = 0;
        int andAll = -1;
        boolean hasDistinct = false;

        for (final int value : values) {
            orAll |= value;
            andAll &= value;
            hasDistinct |= value != values[0];
        }

        // Every bit position is both set and clear somewhere (failure probability ~ 32 * 2^-6000).
        assertEquals(-1, orAll);
        assertEquals(0, andAll);
        assertTrue(hasDistinct);

        // The tail beyond the last full chunk is filled too, not left zero.
        int tailOr = 0;
        for (int i = 4096; i < values.length; i++) {
            tailOr |= values[i];
        }
        assertEquals(-1, tailOr);
    }
    // ---- perf review 2026-09-26 G018 end ----
    // ---- perf review 2026-09-26 G115 begin ----
    // G115-01: the random-value helpers use ThreadLocalRandom.current(); no class keeps a shared static generator field.
    @Test
    public void testRandom_noSharedStaticGenerator() throws Exception {
        final String[] classNames = { "com.landawn.abacus.util.CommonUtil", "com.landawn.abacus.util.N", "com.landawn.abacus.util.BooleanList",
                "com.landawn.abacus.util.ByteList", "com.landawn.abacus.util.CharList", "com.landawn.abacus.util.ShortList",
                "com.landawn.abacus.util.IntList", "com.landawn.abacus.util.LongList", "com.landawn.abacus.util.FloatList",
                "com.landawn.abacus.util.DoubleList", "com.landawn.abacus.util.stream.StreamBase", "com.landawn.abacus.util.stream.Stream",
                "com.landawn.abacus.util.stream.ByteStream", "com.landawn.abacus.util.stream.CharStream", "com.landawn.abacus.util.stream.ShortStream",
                "com.landawn.abacus.util.stream.IntStream", "com.landawn.abacus.util.stream.LongStream", "com.landawn.abacus.util.stream.FloatStream",
                "com.landawn.abacus.util.stream.DoubleStream", "com.landawn.abacus.util.function.Util" };

        for (final String className : classNames) {
            for (final java.lang.reflect.Field field : Class.forName(className).getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    assertFalse(java.util.Random.class.isAssignableFrom(field.getType()), className + "." + field.getName());
                }
            }
        }
    }

    // G115-01: Array.random(..) called from several threads at once - lengths/ranges hold, values vary, threads differ.
    @Test
    public void testRandom_concurrentCallers() throws Exception {
        final int threadCount = 4;
        final List<Throwable> errors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final int[][] firstArrays = new int[threadCount][];
        final Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;

            threads[t] = new Thread(() -> {
                try {
                    for (int round = 0; round < 20; round++) {
                        final int[] full = Array.random(500);
                        assertEquals(500, full.length);

                        boolean hasDistinct = false;
                        for (final int value : full) {
                            hasDistinct |= value != full[0];
                        }
                        assertTrue(hasDistinct);

                        if (round == 0) {
                            firstArrays[threadIndex] = full;
                        }

                        final int[] ranged = Array.random(-5, 5, 500);
                        assertEquals(500, ranged.length);
                        for (final int value : ranged) {
                            assertTrue(value >= -5 && value < 5, String.valueOf(value));
                        }

                        assertEquals(500, Array.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 500).length);
                    }
                } catch (final Throwable e) {
                    errors.add(e);
                }
            });
            threads[t].start();
        }

        for (final Thread thread : threads) {
            thread.join();
        }

        assertTrue(errors.isEmpty(), errors::toString);

        for (int t = 1; t < threadCount; t++) {
            assertFalse(java.util.Arrays.equals(firstArrays[0], firstArrays[t]), "threads produced identical sequences");
        }
    }
    // ---- perf review 2026-09-26 G115 end ----
}
