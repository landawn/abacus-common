package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

public class NDifferenceTest extends NTestSupport {

    @Test
    public void testDifference_arrays() {
        assertArrayEquals(new boolean[] { true }, N.difference(new boolean[] { true, true, false }, new boolean[] { true, false }));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, N.difference(new char[] { 'a', 'b', 'b', 'c', 'd' }, new char[] { 'a', 'b', 'e' }));
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, N.difference(new byte[] { 1, 2, 2, 3, 4 }, new byte[] { 2, 5 }));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, N.difference(new short[] { 1, 2, 2, 3, 4 }, new short[] { 2, 5 }));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.difference(new int[] { 1, 2, 2, 3, 4 }, new int[] { 2, 5 }));
        assertArrayEquals(new int[] { 0, 2, 3 }, N.difference(new int[] { 0, 1, 2, 2, 3 }, new int[] { 2, 5, 1 }));
        assertTrue(CommonUtil.equals(N.removeAll(new int[] { 0, 1, 2, 2, 3 }, new int[] { 2, 5, 1 }), Array.of(0, 3)));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, N.difference(new long[] { 1L, 2L, 2L, 3L, 4L }, new long[] { 2L, 5L }));
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, N.difference(new float[] { 1.5f, 2.5f, 2.5f, 3.5f }, new float[] { 2.5f, 4.5f }), DELTAf);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, N.difference(new double[] { 1.1, 2.2, 2.2, 3.3 }, new double[] { 2.2, 4.4 }), DELTA);
        assertEquals(Arrays.asList("B", "C", "D"), N.difference(new String[] { "A", "B", "B", "C", "D" }, new String[] { "A", "B", "E" }));

        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.difference((int[]) null, new int[] { 1, 2 }));
        assertArrayEquals(new int[] { 1, 2 }, N.difference(new int[] { 1, 2 }, null));
        assertArrayEquals(new boolean[0], N.difference((boolean[]) null, new boolean[] { true }));
        assertTrue(N.difference((String[]) null, new String[] { "A" }).isEmpty());
        assertEquals(Arrays.asList("A", "B"), N.difference(new String[] { "A", "B" }, (Object[]) null));
    }

    @Test
    public void testDifference_collections() {
        assertEquals(Arrays.asList("B", "C", "D"), N.difference(Arrays.asList("A", "B", "B", "C", "D"), Arrays.asList("A", "B", "E")));
        assertEquals(Arrays.asList(1, 3), N.difference(Arrays.asList(1, 2, 2, 3), Arrays.asList(2, 2, 2, 4)));
        assertTrue(N.difference((Collection<String>) null, Arrays.asList("A")).isEmpty());
        assertEquals(new ArrayList<>(Arrays.asList("A", "B")), N.difference(Arrays.asList("A", "B"), (Collection<?>) null));
    }
}
