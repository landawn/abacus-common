package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class NFlattenTest extends NTestSupport {

    @Test
    public void testFlatten_primitive2D() {
        assertArrayEquals(new boolean[] { true, false, true, false, false },
                N.flatten(new boolean[][] { { true, false }, { true }, {}, null, { false, false } }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten((boolean[][]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] { null, null }));

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, N.flatten(new char[][] { { 'a', 'b' }, { 'c' }, {}, null, { 'd', 'e' } }));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.flatten((char[][]) null));

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.flatten(new byte[][] { { 1, 2 }, { 3 }, {}, null, { 4, 5 } }));
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.flatten(new short[][] { { 1, 2 }, { 3 }, {}, null, { 4, 5 } }));
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.flatten(new int[][] { { 1, 2 }, { 3 }, {}, null, { 4, 5 } }));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, N.flatten(new long[][] { { 1L, 2L }, { 3L }, {}, null, { 4L, 5L } }));
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, N.flatten(new float[][] { { 1f, 2f }, { 3f }, {}, null, { 4f, 5f } }), DELTAf);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, N.flatten(new double[][] { { 1.0, 2.0 }, { 3.0 }, {}, null, { 4.0, 5.0 } }), DELTA);
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.flatten((int[][]) null));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.flatten(new int[][] { { 1, 2 }, {}, { 3, 4 } }));
    }

    @Test
    public void testFlatten_objectAndIterables() {
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(new String[][] { { "a", "b" }, { "c" }, {}, null, { "d", "e" } }));
        assertNull(N.flatten((String[][]) null));
        assertArrayEquals(new String[0], N.flatten(new String[][] {}));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(new String[][] { { "a", "b" }, { "c" }, {}, null, { "d", "e" } }, String.class));
        assertArrayEquals(new String[0], N.flatten((String[][]) null, String.class));

        Iterable<Iterable<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Collections.singletonList("c"), Collections.emptyList(), null,
                Arrays.asList("d", "e"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.flatten(nested));
        assertTrue(N.flatten((Iterable<Iterable<String>>) null).isEmpty());
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")),
                N.flatten(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c")), LinkedHashSet::new));

        Iterable<String> nonColl = () -> Arrays.asList("x", "y").iterator();
        assertEquals(Arrays.asList("x", "y"), N.flatten(Arrays.asList(nonColl), ArrayList::new));

        AtomicBoolean created = new AtomicBoolean();
        Iterable<Iterable<String>> oneShot = () -> created.compareAndSet(false, true)
                ? Arrays.<Iterable<String>> asList(Arrays.asList("a", "b"), Arrays.asList("c")).iterator()
                : Collections.emptyIterator();
        assertEquals(Arrays.asList("a", "b", "c"), N.flatten(oneShot, ArrayList::new));

        Collection<String> huge = new java.util.AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
        assertThrows(ArithmeticException.class, () -> N.flatten(Arrays.<Iterable<String>> asList(huge, huge), ignored -> new ArrayList<>()));

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), iteratorToList(N.flatten(Arrays.asList(Arrays.asList("a", "b").iterator(),
                Collections.singletonList("c").iterator(), Collections.<String> emptyIterator(), null, Arrays.asList("d", "e").iterator()).iterator())));
        assertFalse(N.flatten((Iterator<Iterator<String>>) null).hasNext());
    }

    @Test
    public void testFlattenEachElement() {
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"),
                N.flattenEachElement(Arrays.asList("a", Arrays.asList("b", "c"), "d", Arrays.asList("e", "f"))));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), N.flattenEachElement(Arrays.asList("a", Arrays.asList("b", "c"), "d"), HashSet::new));
        assertTrue(N.flattenEachElement(null).isEmpty());
        assertTrue(N.flattenEachElement(Collections.emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b", "c", "d"), N.flattenEachElement(Arrays.asList("a", Arrays.asList("b", Arrays.asList("c")), "d")));
    }

    @Test
    public void testFlatten_nullComponentTypeReportsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> N.flatten(new String[][] { { "a" } }, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> N.flatten((String[][]) null, (Class<String>) null));
    }
}
