package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class NConcatTest extends NTestSupport {

    @Test
    public void testConcat_arrays() {
        assertArrayEquals(new boolean[] { true, false, true, true }, N.concat(new boolean[] { true, false }, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }, (boolean[]) null));
        assertArrayEquals(new boolean[] { true, true }, N.concat((boolean[]) null, new boolean[] { true, true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[]) null, (boolean[]) null));
        boolean[] a = { true, false };
        boolean[] cloned = N.concat(a, new boolean[0]);
        assertArrayEquals(a, cloned);
        assertNotSame(a, cloned);
        assertArrayEquals(a, N.concat(new boolean[0], a));
        assertArrayEquals(new boolean[] { true, false, true, false }, N.concat(new boolean[] { true }, new boolean[] { false }, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[][]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(new boolean[0][0]));
        assertArrayEquals(new boolean[] { true }, N.concat(null, new boolean[] { true }, null, EMPTY_BOOLEAN_ARRAY_CONST));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true }, new boolean[0], new boolean[] { false }));

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, N.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[]) null, (char[]) null));
        assertArrayEquals(new char[] { 'x', 'y' }, N.concat(new char[] { 'x' }, new char[0], new char[] { 'y' }));
        char[] chars = { 'a', 'b', 'c' };
        char[] charClone = N.concat(new char[][] { chars });
        assertArrayEquals(chars, charClone);
        assertNotSame(chars, charClone);
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[][]) null));

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, N.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 }));
        assertArrayEquals(new byte[] { 10, 20 }, N.concat(new byte[] { 10 }, new byte[0], new byte[] { 20 }));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.concat((byte[][]) null));

        assertArrayEquals(new short[] { 1, 2, 3, 4 }, N.concat(new short[] { 1, 2 }, new short[] { 3, 4 }));
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.concat(new short[] { 1, 2 }, new short[0], new short[] { 3, 4 }, new short[] { 5 }));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.concat((short[][]) null));

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.concat(new int[] { 1, 2, 3 }, new int[] { 4, 5 }));
        assertArrayEquals(new int[] { 1, 2 }, N.concat(new int[] { 1 }, (int[]) null, new int[] { 2 }));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.concat((int[][]) null));

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, N.concat(new long[] { 1L, 2L }, new long[] { 3L, 4L }));
        assertArrayEquals(new long[] { 10L, 20L, 30L, 40L, 50L }, N.concat(new long[] { 10L, 20L }, new long[0], new long[] { 30L }, new long[] { 40L, 50L }));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.concat((long[][]) null));

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, N.concat(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f },
                N.concat(new float[] { 1.0f, 2.0f }, new float[0], new float[] { 3.0f }, new float[] { 4.0f }));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.concat((float[][]) null));

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, N.concat(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }));
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 },
                N.concat(new double[] { 1.0, 2.0 }, new double[0], new double[] { 3.0 }, new double[] { 4.0, 5.0 }));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.concat((double[][]) null));

        assertArrayEquals(new String[] { "a", "b", "c", "d" }, N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertArrayEquals(new String[] { "a", "b" }, N.concat(new String[] { "a", "b" }, (String[]) null));
        assertArrayEquals(new String[] { "c", "d" }, N.concat((String[]) null, new String[] { "c", "d" }));
        assertNull(N.concat((String[]) null, (String[]) null));
        assertArrayEquals(new String[] { "a", "b" }, N.concat(new String[] { "a" }, (String[]) null, new String[] { "b" }));
        assertThrows(IllegalArgumentException.class, () -> N.concat((String[][]) null));
        assertArrayEquals(new String[0], N.concat(new String[0][0]));
        assertArrayEquals(new String[] { "a" }, N.concat(new String[] { "a" }));
        assertArrayEquals(new String[0], N.concat(new String[][] { null, null }));
    }

    @Test
    public void testConcat_iterables() {
        assertEquals(Arrays.asList("a", "b", "c", "d"), N.concat(Arrays.asList("a", "b"), Arrays.asList("c", "d")));
        assertEquals(Arrays.asList("a", "b"), N.concat(Arrays.asList("a", "b"), (Iterable<String>) null));
        assertEquals(Arrays.asList("c", "d"), N.concat((Iterable<String>) null, Arrays.asList("c", "d")));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>) null, (Iterable<String>) null));
        assertEquals(Arrays.asList("x", "y"), N.concat(new ArrayList<>(), Arrays.asList("x", "y")));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.concat(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e")));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>[]) null));
        assertEquals(Collections.emptyList(), N.concat(new Iterable[0]));
        assertEquals(Arrays.asList("a", "b"), N.concat(null, Arrays.asList("a", "b"), null, Collections.emptyList()));

        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.concat(lists));
        assertTrue(N.concat((Collection<List<String>>) null).isEmpty());
        assertTrue(N.concat(Collections.<Iterable<String>> emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b", "c", "d"),
                N.concat(Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c", "d"), Collections.emptyList())));

        Set<String> set = N.concat(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")), IntFunctions.ofSet());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), set);

        LinkedList<String> linked = N.concat(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")), IntFunctions.ofLinkedList());
        assertTrue(linked instanceof LinkedList);
        assertEquals(Arrays.asList("a", "b", "c", "d"), linked);
        assertTrue(N.concat((Collection<Iterable<String>>) null, IntFunctions.ofLinkedList()).isEmpty());

        Iterable<Integer> raw = () -> Arrays.asList(3, 4).iterator();
        List<Iterable<? extends Integer>> input = new ArrayList<>();
        input.add(Arrays.asList(1, 2));
        input.add(raw);
        assertEquals(Arrays.asList(1, 2, 3, 4), N.concat(input, IntFunctions.ofList()));
    }

    @Test
    public void testConcat_iterators() {
        ObjIterator<String> two = N.concat(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator());
        assertEquals(Arrays.asList("a", "b", "c", "d"), iteratorToList(two));

        assertEquals(Arrays.asList("a", "b"), iteratorToList(N.concat(Arrays.asList("a", "b").iterator(), (Iterator<String>) null)));
        assertEquals(Arrays.asList("test"), iteratorToList(N.concat((Iterator<String>) null, Arrays.asList("test").iterator())));
        assertEquals(Collections.emptyList(), iteratorToList(N.concat((Iterator<String>) null, (Iterator<String>) null)));
        assertEquals(Arrays.asList("x"), iteratorToList(N.concat(Collections.emptyIterator(), Arrays.asList("x").iterator())));

        ObjIterator<Integer> three = N.concat(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator(), Arrays.asList(4, 5).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), iteratorToList(three));
        assertTrue(iteratorToList(N.concat((Iterator<String>[]) null)).isEmpty());
        assertTrue(iteratorToList(N.concat(new Iterator[0])).isEmpty());

        ObjIterator<String> withNulls = N.concat(Arrays.asList("a").iterator(), (Iterator<String>) null, Arrays.asList("b").iterator());
        assertEquals(Arrays.asList("a", "b"), iteratorToList(withNulls));

        assertEquals(CommonUtil.asList("a", "b", "c", "d"),
                CommonUtil.toList(N.concat(CommonUtil.asList("a", "b").iterator(), CommonUtil.asList("c").iterator(), CommonUtil.asList("d").iterator())));
    }

    @Test
    public void testConcat_capacityHintCountsOnlyTheCollectionElements() {
        final Iterable<String> plain = () -> Arrays.asList("a", "b", "c").iterator();
        final int[] hint = { -1 };
        final java.util.function.IntFunction<List<String>> supplier = size -> {
            hint[0] = size;
            return new ArrayList<>(Math.max(size, 0));
        };

        final List<String> result = N.concat(Arrays.asList(plain, Arrays.asList("d", "e")), supplier);

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
        // The documented ArithmeticException is computed from this estimate, which sums only the Collection
        // elements - a non-Collection Iterable contributes 0.
        assertEquals(2, hint[0]);
    }
}
