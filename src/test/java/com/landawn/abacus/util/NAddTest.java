package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NAddTest extends NTestSupport {

    @Test
    public void testAdd_arrays() {
        boolean[] bools = { true, false };
        assertArrayEquals(new boolean[] { true, false, true }, N.add(bools, true));
        assertArrayEquals(new boolean[] { true, false }, bools);
        assertArrayEquals(new boolean[] { true }, N.add((boolean[]) null, true));
        assertArrayEquals(new boolean[] { false }, N.add(new boolean[0], false));

        char[] chars = { 'a', 'b' };
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.add(chars, 'c'));
        assertArrayEquals(new char[] { 'a', 'b' }, chars);
        assertArrayEquals(new char[] { 'x' }, N.add((char[]) null, 'x'));

        byte[] bytes = { 1, 2 };
        assertArrayEquals(new byte[] { 1, 2, 3 }, N.add(bytes, (byte) 3));
        assertArrayEquals(new byte[] { 5 }, N.add((byte[]) null, (byte) 5));

        short[] shorts = { 10, 20 };
        assertArrayEquals(new short[] { 10, 20, 30 }, N.add(shorts, (short) 30));
        assertArrayEquals(new short[] { 50 }, N.add((short[]) null, (short) 50));

        int[] ints = { 1, 2, 3 };
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.add(ints, 4));
        assertArrayEquals(new int[] { 99 }, N.add((int[]) null, 99));

        long[] longs = { 100L, 200L };
        assertArrayEquals(new long[] { 100L, 200L, 300L }, N.add(longs, 300L));
        assertArrayEquals(new long[] { 999L }, N.add((long[]) null, 999L));

        float[] floats = { 1.5f, 2.5f };
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, N.add(floats, 3.5f));
        assertArrayEquals(new float[] { 9.9f }, N.add((float[]) null, 9.9f));

        double[] doubles = { 1.0, 2.0 };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.add(doubles, 3.0));
        assertArrayEquals(new double[] { 99.9 }, N.add((double[]) null, 99.9));

        String[] strings = { "hello", "world" };
        assertArrayEquals(new String[] { "hello", "world", "test" }, N.add(strings, "test"));
        assertArrayEquals(new String[] { "hello", "world" }, strings);
        assertArrayEquals(new String[] { "new" }, N.add((String[]) null, "new"));

        Integer[] integers = { 1, 2, 3 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, N.add(integers, 4));
        assertArrayEquals(new Integer[] { 1 }, N.add(new Integer[0], 1));
        assertThrows(IllegalArgumentException.class, () -> N.add((Integer[]) null, 1));
    }

    @Test
    public void testAddAll_arrays() {
        boolean[] bools = { true, false };
        assertArrayEquals(new boolean[] { true, false, true, true }, N.addAll(bools, true, true));
        assertArrayEquals(new boolean[] { true, false }, bools);
        assertArrayEquals(new boolean[] { true, false }, N.addAll((boolean[]) null, true, false));
        assertArrayEquals(new boolean[] { true }, N.addAll(new boolean[] { true }));
        assertArrayEquals(bools, N.addAll(bools, new boolean[0]));

        char[] chars = { 'a', 'b' };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, N.addAll(chars, 'c', 'd'));
        assertArrayEquals(new char[] { 'x', 'y' }, N.addAll((char[]) null, 'x', 'y'));
        char[] charClone = N.addAll(chars, new char[0]);
        assertArrayEquals(chars, charClone);
        assertNotSame(chars, charClone);

        byte[] bytes = { 1, 2 };
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, N.addAll(bytes, (byte) 3, (byte) 4));
        assertArrayEquals(new byte[] { 5, 6 }, N.addAll((byte[]) null, (byte) 5, (byte) 6));
        assertArrayEquals(bytes, N.addAll(bytes, new byte[0]));

        short[] shorts = { 10, 20 };
        assertArrayEquals(new short[] { 10, 20, 30, 40 }, N.addAll(shorts, (short) 30, (short) 40));
        assertArrayEquals(new short[] { 50, 60 }, N.addAll((short[]) null, (short) 50, (short) 60));
        assertArrayEquals(shorts, N.addAll(shorts, new short[0]));

        int[] ints = { 1, 2 };
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.addAll(ints, 3, 4, 5));
        assertArrayEquals(new int[] { 99, 100 }, N.addAll((int[]) null, 99, 100));
        assertArrayEquals(ints, N.addAll(ints, new int[0]));

        long[] longs = { 100L, 200L };
        assertArrayEquals(new long[] { 100L, 200L, 300L, 400L }, N.addAll(longs, 300L, 400L));
        assertArrayEquals(new long[] { 999L }, N.addAll((long[]) null, 999L));
        assertArrayEquals(longs, N.addAll(longs, new long[0]));

        float[] floats = { 1.5f, 2.5f };
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f, 4.5f }, N.addAll(floats, 3.5f, 4.5f));
        assertArrayEquals(new float[] { 9.9f }, N.addAll((float[]) null, 9.9f));

        double[] doubles = { 1.0, 2.0 };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, N.addAll(doubles, 3.0, 4.0));
        assertArrayEquals(new double[] { 99.9 }, N.addAll((double[]) null, 99.9));

        String[] strings = { "hello", "world" };
        assertArrayEquals(new String[] { "hello", "world", "foo", "bar" }, N.addAll(strings, "foo", "bar"));
        assertArrayEquals(new String[] { "new" }, N.addAll((String[]) null, "new"));

        Integer[] integers = { 1, 2 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, N.addAll(integers, 3, 4));
        assertArrayEquals(new Integer[] { 1, 2 }, N.addAll(new Integer[0], 1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Integer[]) null, 1, 2));
        assertArrayEquals(new Integer[] { 1 }, N.addAll(new Integer[] { 1 }, (Integer[]) null));
        assertArrayEquals(new Integer[] {}, N.addAll(new Integer[] {}, (Integer[]) null));
    }

    @Test
    public void testAddAll_collection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.addAll(list, "c", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.addAll(list));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, "a"));

        List<String> fromIterable = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.addAll(fromIterable, Arrays.asList("c", "d")));
        assertEquals(Arrays.asList("a", "b", "c", "d"), fromIterable);
        assertFalse(N.addAll(fromIterable, (Iterable<String>) null));
        assertFalse(N.addAll(fromIterable, new ArrayList<String>()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a")));

        List<String> viewBacked = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.addAll(viewBacked, viewBacked.subList(0, 2)));
        assertEquals(Arrays.asList("a", "b", "c", "a", "b"), viewBacked);

        List<String> fromIterator = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.addAll(fromIterator, Arrays.asList("c", "d").iterator()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), fromIterator);
        assertFalse(N.addAll(fromIterator, (Iterator<String>) null));
        assertFalse(N.addAll(fromIterator, Collections.emptyIterator()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a").iterator()));

        List<String> selfIterated = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.addAll(selfIterated, selfIterated.iterator()));
        assertEquals(Arrays.asList("a", "b", "a", "b"), selfIterated);

        List<String> fromNonCollection = new ArrayList<>(Arrays.asList("existing"));
        Iterable<String> nonCollIterable = () -> Arrays.asList("new1", "new2").iterator();
        assertTrue(N.addAll(fromNonCollection, nonCollIterable));
        assertEquals(Arrays.asList("existing", "new1", "new2"), fromNonCollection);
    }

    @Test
    public void testAdd_nullArrayContractDiffersBetweenPrimitiveAndGenericOverloads() {
        // Pins the class-javadoc "Exception Philosophy" bullet: only the generic Object[] overload rejects a
        // null array (its runtime component type cannot be fabricated); the primitive/String ones treat it as empty.
        assertArrayEquals(new boolean[] { true }, N.add((boolean[]) null, true));
        assertArrayEquals(new int[] { 1 }, N.add((int[]) null, 1));
        assertArrayEquals(new String[] { "x" }, N.add((String[]) null, "x"));
        assertThrows(IllegalArgumentException.class, () -> N.add((Integer[]) null, 1));

        // ... and a "safe operation" on a null input is a no-op rather than an exception.
        N.reverse((Object[]) null);
    }

    @Test
    public void testAddAll_nullArrayAndNullElementsAreTreatedAsEmpty() {
        assertArrayEquals(new int[] { 1, 2 }, N.addAll((int[]) null, 1, 2));
        assertArrayEquals(new int[] { 1 }, N.addAll(new int[] { 1 }, (int[]) null));
        assertArrayEquals(new int[] {}, N.addAll((int[]) null, (int[]) null));
        assertArrayEquals(new String[] { "x" }, N.addAll((String[]) null, "x"));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Integer[]) null, 1, 2));
    }

    @Test
    public void testAddAll_oversizedResultReportsArithmeticException() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.getRuntime().maxMemory() > 3L * 1024 * 1024 * 1024, "needs a heap large enough for a 2GB byte[]");

        final byte[] huge = new byte[Integer.MAX_VALUE - 2];
        assertThrows(ArithmeticException.class, () -> N.addAll(huge, new byte[10]));
    }
}
