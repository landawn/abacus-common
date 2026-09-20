package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Group C6 (CommonUtil.java 25831-30996): doc-only changes. These assertions pin the conditions the
 * reworded {@code @throws} tags now state, so they are expected to be GREEN on both the patched copy and
 * the r9600+ baseline.
 */
public class ExcReviewC6Test extends com.landawn.abacus.TestBase {

    @Test
    public void mismatch_primitiveRange_docConditions() {
        assertThrows(IllegalArgumentException.class, () -> N.mismatch(new int[] { 1 }, 0, new int[] { 1 }, 0, -1));
        // null array is treated as length zero -> IOOBE, not NPE/IAE
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch((int[]) null, 0, new int[] { 1 }, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(new int[] { 1 }, -1, new int[] { 1 }, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(new int[] { 1 }, 0, new int[] { 1 }, 1, 1));
        assertEquals(-1, N.mismatch((int[]) null, 0, (int[]) null, 0, 0));
    }

    @Test
    public void mismatch_objectAndCollectionRange_docConditions() {
        assertThrows(IllegalArgumentException.class, () -> N.mismatch(new String[] { "a" }, 0, new String[] { "a" }, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> N.mismatch(new String[] { "a" }, 0, new String[] { "a" }, 0, 1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch((String[]) null, 0, new String[] { "a" }, 0, 1, String::length));

        final List<String> l = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> N.mismatch(l, 0, l, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> N.mismatch(l, 0, l, 0, 1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(l, 1, l, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch((List<String>) null, 0, l, 0, 1));
        assertEquals(-1, N.mismatch((List<String>) null, 0, (List<String>) null, 0, 0));
    }

    @Test
    public void shuffle_range_docConditions() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(new byte[] { 1, 2 }, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(new int[] { 1, 2 }, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(new String[] { "a" }, 0, 2));
    }

    @Test
    public void fill_range_docConditions() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.fill(new boolean[] { true }, -1, 1, false));
        assertThrows(IndexOutOfBoundsException.class, () -> N.fill(new char[] { 'a' }, 1, 0, 'b'));
        assertThrows(IndexOutOfBoundsException.class, () -> N.fill(new byte[] { 1 }, 0, 2, (byte) 2));
    }
}
