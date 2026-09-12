package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NIntersectionTest extends NTestSupport {

    @Test
    public void testIntersection_arrays() {
        assertArrayEquals(new boolean[] { true, false, false },
                N.intersection(new boolean[] { true, false, false, true }, new boolean[] { false, false, true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection((boolean[]) null, new boolean[] { true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, new boolean[] { false }));

        assertArrayEquals(new char[] { 'a', 'b', 'b' }, N.intersection(new char[] { 'a', 'b', 'b', 'c' }, new char[] { 'a', 'b', 'b', 'b', 'd' }));
        assertEquals(0, N.intersection(new char[] { 'x', 'y' }, new char[] { 'z', 'w' }).length);
        assertArrayEquals(new byte[] { 1, 2, 2 }, N.intersection(new byte[] { 1, 2, 2, 3, 4 }, new byte[] { 1, 2, 2, 2, 5, 6 }));
        assertArrayEquals(new short[] { 1, 2, 2 }, N.intersection(new short[] { 1, 2, 2, 3, 4 }, new short[] { 1, 2, 2, 2, 5, 6 }));
        assertArrayEquals(new int[] { 1, 2, 2 }, N.intersection(new int[] { 1, 2, 2, 3, 4 }, new int[] { 1, 2, 2, 2, 5, 6 }));
        assertArrayEquals(new int[] { 1, 2 }, N.intersection(new int[] { 0, 1, 2, 2, 3 }, new int[] { 2, 5, 1 }));
        assertArrayEquals(new int[] { 2, 3, 3 }, N.intersection(new int[] { 1, 2, 2, 3, 3, 3 }, new int[] { 2, 3, 3, 4, 4, 4 }));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.intersection(new int[] { 1 }, null));
        assertArrayEquals(new long[] { 1L, 2L, 2L }, N.intersection(new long[] { 1L, 2L, 2L, 3L, 4L }, new long[] { 1L, 2L, 2L, 2L, 5L, 6L }));
        assertArrayEquals(new float[] { 1.5f, 2.5f, 2.5f },
                N.intersection(new float[] { 1.5f, 2.5f, 2.5f, 3.5f }, new float[] { 1.5f, 2.5f, 2.5f, 2.5f, 4.5f }), DELTAf);
        assertArrayEquals(new double[] { 1.1, 2.2, 2.2 }, N.intersection(new double[] { 1.1, 2.2, 2.2, 3.3 }, new double[] { 1.1, 2.2, 2.2, 2.2, 4.4 }), DELTA);

        assertEquals(Arrays.asList("A", "B", "B"), N.intersection(new String[] { "A", "B", "B", "C", "D" }, new String[] { "A", "B", "B", "B", "E", "F" }));
        assertTrue(N.intersection((String[]) null, new String[] { "A" }).isEmpty());
        assertTrue(N.intersection(new String[0], new String[] { "A" }).isEmpty());
    }

    @Test
    public void testIntersection_collections() {
        assertEquals(Arrays.asList("A", "B", "B"), N.intersection(Arrays.asList("A", "B", "B", "C", "D"), Arrays.asList("A", "B", "B", "B", "E", "F")));
        assertTrue(N.intersection(new ArrayList<String>(), Arrays.asList("A")).isEmpty());
        assertTrue(N.intersection((Collection<String>) null, Arrays.asList("A")).isEmpty());
        assertTrue(N.intersection(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)).isEmpty());

        assertEquals(Arrays.asList("A", "B"),
                N.intersection(Arrays.asList(Arrays.asList("A", "B", "B", "C"), Arrays.asList("A", "B", "B", "B", "D"), Arrays.asList("A", "B", "E"))));
        assertTrue(N.intersection(new ArrayList<List<String>>()).isEmpty());
        assertEquals(Arrays.asList("A", "B", "C"), N.intersection(Arrays.asList(Arrays.asList("A", "B", "C"))));
        assertTrue(N.intersection(Arrays.asList(Arrays.asList("A", "B"), new ArrayList<String>())).isEmpty());
        assertTrue(N.intersection((Collection<Collection<String>>) null).isEmpty());
    }
}
