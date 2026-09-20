package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Group N7 (N.java 31407-36664): no code change was made; these assertions verify the contracts the
 * review relied on (all expected GREEN on the baseline as well).
 */
public class ExcReviewN7Test extends com.landawn.abacus.TestBase {

    @Test
    public void zipToArray_nullTargetElementType_isIAE_viaArrayNewInstance() {
        final String[] a = { "a" };
        final Integer[] b = { 1 };
        final Long[] c = { 1L };
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, (s, i) -> s + i, (Class<String>) null));
        assertTrue(ex.getMessage().contains("componentType"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, "", 0, (s, i) -> s + i, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, c, (s, i, l) -> s + i + l, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, c, "", 0, 0L, (s, i, l) -> s + i + l, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> N.zip(new String[0], new Integer[0], (s, i) -> s + i, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, null, String.class));
    }

    @Test
    public void merge_nullIterable_isAcceptedThroughAsList() {
        final List<Integer> b = Arrays.asList(2, 4);
        assertEquals(b, N.merge((Iterable<Integer>) null, b, (x, y) -> MergeResult.TAKE_FIRST));
        assertEquals(b, N.merge(b, (Iterable<Integer>) null, (x, y) -> MergeResult.TAKE_FIRST));
        assertThrows(IllegalArgumentException.class, () -> N.merge(b, b, null));
    }

    @Test
    public void unzip_supplierReturningNull_isIAE() {
        final List<String> pairs = Arrays.asList("a=1");
        assertThrows(IllegalArgumentException.class, () -> N.unzip(pairs, (s, out) -> out.set(s, s), size -> null));
        assertThrows(IllegalArgumentException.class, () -> N.unzip(pairs, null));
        assertThrows(IllegalArgumentException.class, () -> N.unzip(pairs.iterator(), (s, out) -> out.set(s, s), null));
    }

    @Test
    public void countOverloads_filterNull_isIAE_before_anything() {
        assertThrows(IllegalArgumentException.class, () -> N.count((List<String>) null, null));
        assertThrows(IllegalArgumentException.class, () -> N.count((String[]) null, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.count((String[]) null, 0, 1, s -> true));
        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(Arrays.asList(1), 2, 1, x -> true));
    }
}
