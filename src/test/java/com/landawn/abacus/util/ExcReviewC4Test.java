package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Validation-order pins for group C4 (CommonUtil 15533-20675): an invalid {@code [fromIndex, toIndex)} range is
 * rejected (IndexOutOfBoundsException) BEFORE a null later parameter (array / supplier) is rejected
 * (IllegalArgumentException), following parameter order. A valid range with a null later parameter still gets IAE.
 */
public class ExcReviewC4Test extends TestBase {

    @Test
    public void toArray_range_before_destinationArray() {
        final List<String> list = List.of("x");
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, -1, 1, (String[]) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 0, 2, (String[]) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(null, 0, 1, (String[]) null));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 0, 1, (String[]) null)).getMessage().contains("a"));
    }

    @Test
    public void toArray_range_before_arraySupplier() {
        final List<String> list = List.of("x");
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, -1, 1, (IntFunction<String[]>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 0, 2, (IntFunction<String[]>) null));
        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 0, 1, (IntFunction<String[]>) null));
    }

    @Test
    public void toArray_range_before_targetType() {
        // already reordered on the baseline; kept here so the three Collection-range overloads are pinned together
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(List.of("x"), -1, 1, (Class<String[]>) null));
        assertThrows(IllegalArgumentException.class, () -> N.toArray(List.of("x"), 0, 1, (Class<String[]>) null));
    }

    @Test
    public void toCollection_range_before_supplier_primitiveAndGeneric() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new boolean[1], -1, 1, (IntFunction<List<Boolean>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new char[1], 0, 2, (IntFunction<List<Character>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new byte[1], 1, 0, (IntFunction<List<Byte>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new short[1], -1, 1, (IntFunction<List<Short>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new int[1], 0, 2, (IntFunction<List<Integer>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new long[1], 1, 0, (IntFunction<List<Long>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new float[1], -1, 1, (IntFunction<List<Float>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new double[1], 0, 2, (IntFunction<List<Double>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection(new String[1], -1, 1, (IntFunction<List<String>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toCollection((String[]) null, 0, 1, (IntFunction<List<String>>) null));

        // valid range + null supplier: still IllegalArgumentException, naming the supplier
        assertTrue(assertThrows(IllegalArgumentException.class, () -> N.toCollection(new int[1], 0, 1, (IntFunction<List<Integer>>) null))
                .getMessage().contains("supplier"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> N.toCollection(new String[1], 0, 1, (IntFunction<List<String>>) null))
                .getMessage().contains("supplier"));
        // and the happy path is untouched
        assertTrue(N.toCollection(new int[] { 7 }, 0, 1, ArrayList::new).contains(7));
    }
}
