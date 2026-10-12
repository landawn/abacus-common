package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.u.OptionalLong;

/** Shared fixtures and lifecycle for the LongStream contract tests. */
public abstract class LongStreamTestSupport extends TestBase {

    protected LongStream createLongStream(long... a) {
        return LongStream.of(a);
    }

    protected LongStream createLongStream(long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex);
    }

    protected LongStream createLongStream(Long[] a) {
        return LongStream.of(a);
    }

    protected LongStream createLongStream(Long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex);
    }

    protected LongStream createLongStream(Collection<Long> coll) {
        return LongStream.of(coll.toArray(new Long[coll.size()]));
    }

    protected LongStream createLongStream(LongIterator iter) {
        return iter == null ? LongStream.empty() : LongStream.of(iter.toArray());
    }

    protected LongStream createLongStream(OptionalLong op) {
        return LongStream.of(op);
    }

    protected LongStream createLongStream(LongBuffer buff) {
        return LongStream.of(buff);
    }

    // ---- perf review 2026-09-26 G100 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static long[] flattenVerticallyReferenceG114(final long[][] a) {
        final LongList ret = new LongList();
        int maxLen = 0;

        for (final long[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final long[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final long[][] a, final int skip) {
        final long[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, LongStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, LongStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), LongStream.flatten(a, true).skip(skip).toArray(),
                message);

        final LongIterator iter = LongStream.flatten(a, true).iterator();

        for (final long element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextLong());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertFalse(iter.hasNext());
    }
}
