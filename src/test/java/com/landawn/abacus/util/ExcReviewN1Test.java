package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntBiFunction;

/**
 * Exception-review group N1 (N.java lines 1-5264): pins the validation-order change in
 * {@code N.splitByChunkCount(int, int, IntBiFunction)} - parameters are now validated in signature order
 * (totalSize, maxChunkCount, func) instead of func first.
 */
public class ExcReviewN1Test extends com.landawn.abacus.TestBase {

    @Test
    public void splitByChunkCount_negativeTotalSize_and_nullFunc_reportsTotalSizeFirst() {
        // RED on the baseline: the old code checked func before totalSize, so the message named 'func'.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> N.splitByChunkCount(-1, 3, (IntBiFunction<Object>) null));
        assertTrue(ex.getMessage().contains("totalSize"), ex.getMessage());
    }

    @Test
    public void splitByChunkCount_nonPositiveMaxChunkCount_and_nullFunc_reportsMaxChunkCountFirst() {
        // RED on the baseline for the same reason.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> N.splitByChunkCount(5, 0, (IntBiFunction<Object>) null));
        assertTrue(ex.getMessage().contains("maxChunkCount"), ex.getMessage());
    }

    @Test
    public void splitByChunkCount_nullFunc_stillIllegalArgument() {
        // Unchanged: a null func with otherwise valid arguments is still an IllegalArgumentException naming 'func'.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> N.splitByChunkCount(5, 2, (IntBiFunction<Object>) null));
        assertTrue(ex.getMessage().contains("func"), ex.getMessage());
    }

    @Test
    public void splitByChunkCount_validArguments_unchanged() {
        final int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        final List<int[]> chunks = N.splitByChunkCount(7, 3, (from, to) -> N.copyOfRange(a, from, to));
        assertEquals(3, chunks.size());
        assertTrue(Arrays.equals(new int[] { 1, 2, 3 }, chunks.get(0)));
        assertTrue(Arrays.equals(new int[] { 4, 5 }, chunks.get(1)));
        assertTrue(Arrays.equals(new int[] { 6, 7 }, chunks.get(2)));
    }
}
