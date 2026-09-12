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
        final Integer[] wrappers = {1000, null, -2000};
        final Integer first = wrappers[0];
        final Integer last = wrappers[2];

        assertArrayEquals(new int[] {1000, 0, -2000}, Array.unbox(wrappers));
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
        // Contract pin for the corrected javadoc: Array.random draws from the SecureRandom held by N, which is
        // NOT the instance IntList.random and the other PrimitiveList random(...) methods use.
        assertSame(CommonUtil.RAND, N.RAND);
        assertTrue(N.RAND instanceof java.security.SecureRandom);
        assertNotSame(N.RAND, IntList.RAND);
        assertNotSame(N.RAND, LongList.RAND);
        assertNotSame(N.RAND, DoubleList.RAND);
        assertNotSame(IntList.RAND, LongList.RAND);

        assertEquals(4, Array.random(4).length);
        assertEquals(4, Array.random(0, 10, 4).length);
    }
}
