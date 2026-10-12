package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.u.OptionalDouble;

/** Shared fixtures and lifecycle for the DoubleStream contract tests. */
public abstract class DoubleStreamTestSupport extends TestBase {

    protected DoubleStream createDoubleStream(double... a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Double[] a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(Double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Collection<Double> coll) {
        return DoubleStream.of(coll.toArray(new Double[coll.size()]));
    }

    protected DoubleStream createDoubleStream(DoubleIterator iter) {
        return iter == null ? DoubleStream.empty() : DoubleStream.of(iter.toArray());
    }

    protected DoubleStream createDoubleStream(DoubleBuffer buff) {
        return DoubleStream.of(buff);
    }

    protected DoubleStream createDoubleStream(OptionalDouble op) {
        return DoubleStream.of(op);
    }

    // ---- perf review 2026-09-26 G094 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static double[] flattenVerticallyReferenceG114(final double[][] a) {
        final DoubleList ret = new DoubleList();
        int maxLen = 0;

        for (final double[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final double[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final double[][] a, final int skip) {
        final double[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, DoubleStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, DoubleStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), DoubleStream.flatten(a, true).skip(skip).toArray(),
                message);

        final DoubleIterator iter = DoubleStream.flatten(a, true).iterator();

        for (final double element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextDouble());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextDouble);
        assertFalse(iter.hasNext());
    }
}
