package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Reachability evidence for the exceptions newly DECLARED by review group N2 (no behaviour changed; every
 * assertion is expected to be green on the baseline as well).
 */
public class ExcReviewN2Test extends TestBase {

    @Test
    public void add_generic_declares_reachable_ArrayStoreException() {
        final Object[] a = new String[] { "x" };
        assertThrows(ArrayStoreException.class, () -> N.add(a, Integer.valueOf(1)));
    }

    @Test
    public void insert_generic_declares_reachable_ArrayStoreException() {
        final Object[] a = new String[] { "x" };
        assertThrows(ArrayStoreException.class, () -> N.insert(a, 0, Integer.valueOf(1)));
    }

    @Test
    public void list_mutators_declare_reachable_UnsupportedOperationException() {
        final List<Integer> ro = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));

        assertThrows(UnsupportedOperationException.class, () -> N.replaceIf(ro, x -> x == 2, 9));
        assertThrows(UnsupportedOperationException.class, () -> N.replaceAll(ro, 2, 9));
        assertThrows(UnsupportedOperationException.class, () -> N.replaceAll(ro, x -> x + 1));
        assertThrows(UnsupportedOperationException.class, () -> N.updateAll(ro, x -> x + 1));
        assertThrows(UnsupportedOperationException.class, () -> N.setAll(ro, i -> i));
        assertThrows(UnsupportedOperationException.class, () -> N.setAll(ro, (i, x) -> x + i));
        assertThrows(UnsupportedOperationException.class, () -> N.addAll(ro, 4, 5));
        assertThrows(UnsupportedOperationException.class, () -> N.addAll(ro, Arrays.asList(4, 5)));
        assertThrows(UnsupportedOperationException.class, () -> N.addAll(ro, Arrays.asList(4, 5).iterator()));
    }

    @Test
    public void insert_index_condition_as_documented() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert((int[]) null, 1, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new int[] { 1 }, 2, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new int[] { 1 }, -1, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll((int[]) null, 1, 7, 8));
    }
}
