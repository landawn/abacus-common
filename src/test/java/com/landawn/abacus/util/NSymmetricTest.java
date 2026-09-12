package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

public class NSymmetricTest extends NTestSupport {

    @Test
    public void testSymmetricDifference_arrays() {
        assertArrayEquals(new boolean[] { true }, N.symmetricDifference(new boolean[] { true, true, false }, new boolean[] { true, false }));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, N.symmetricDifference(new char[] { 'a', 'b', 'b', 'c' }, new char[] { 'b', 'd', 'a' }));
        assertArrayEquals(new byte[] { 0, 2, 3, 5 }, N.symmetricDifference(new byte[] { 0, 1, 2, 2, 3 }, new byte[] { 2, 5, 1 }));
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, N.symmetricDifference(new short[] { 1, 2, 2, 3, 4 }, new short[] { 2, 5, 1 }));
        assertArrayEquals(new int[] { 0, 2, 3, 5 }, N.symmetricDifference(new int[] { 0, 1, 2, 2, 3 }, new int[] { 2, 5, 1 }));
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, N.symmetricDifference(new long[] { 1L, 2L, 2L, 3L, 4L }, new long[] { 2L, 5L, 1L }));
        assertArrayEquals(new float[] { 2.5f, 3.5f, 4.5f }, N.symmetricDifference(new float[] { 1.5f, 2.5f, 2.5f, 3.5f }, new float[] { 2.5f, 4.5f, 1.5f }),
                DELTAf);
        assertArrayEquals(new double[] { 2.2, 3.3, 4.4 }, N.symmetricDifference(new double[] { 1.1, 2.2, 2.2, 3.3 }, new double[] { 2.2, 4.4, 1.1 }), DELTA);
        assertEquals(Arrays.asList("B", "C", "D", "E"), N.symmetricDifference(new String[] { "A", "B", "B", "C", "D" }, new String[] { "B", "E", "A" }));

        assertArrayEquals(new boolean[] { true, false }, N.symmetricDifference((boolean[]) null, new boolean[] { true, false }));
        assertArrayEquals(new boolean[0], N.symmetricDifference((boolean[]) null, (boolean[]) null));
        assertArrayEquals(new int[] { 1, 2 }, N.symmetricDifference((int[]) null, new int[] { 1, 2 }));
        assertArrayEquals(new int[] { 1, 2 }, N.symmetricDifference(new int[] { 1, 2 }, null));
        assertTrue(N.symmetricDifference((String[]) null, new String[] { "A" }).contains("A"));
        assertEquals(Arrays.asList("A", "B"), N.symmetricDifference(new String[] { "A", "B" }, (Object[]) null));
    }

    @Test
    public void testSymmetricDifference_collections() {
        assertEquals(Arrays.asList("B", "C", "D", "E"), N.symmetricDifference(Arrays.asList("A", "B", "B", "C", "D"), Arrays.asList("B", "E", "A")));
        assertEquals(Arrays.asList(1, 2), N.symmetricDifference(Arrays.asList(1, 2, 2), Arrays.asList(2, 2, 2)));
        assertTrue(N.symmetricDifference((Collection<String>) null, Arrays.asList("A")).contains("A"));
        assertEquals(Arrays.asList("A", "B"), N.symmetricDifference(Arrays.asList("A", "B"), (Collection<?>) null));
    }
}
