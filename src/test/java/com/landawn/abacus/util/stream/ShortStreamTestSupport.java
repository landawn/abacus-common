package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;

/** Shared fixtures and lifecycle for the ShortStream contract tests. */
public abstract class ShortStreamTestSupport extends TestBase {

    protected ShortStream stream;

    protected ShortStream stream2;

    protected ShortStream emptyStream;

    protected ShortStream singleElementStream;

    protected ShortStream singleElementStream2;

    protected ShortStream multiElementStream;

    protected ShortStream multiElementStream2;

    protected ShortStream multiElementStream3;

    @BeforeEach
    public void setUp() {
        stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        stream2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        emptyStream = ShortStream.empty();
        singleElementStream = createShortStream((short) 5);
        singleElementStream2 = createShortStream((short) 5);
        multiElementStream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        multiElementStream2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        multiElementStream3 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
    }

    protected ShortStream createShortStream(short... a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Short[] a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(Short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Collection<Short> coll) {
        return ShortStream.of(coll.toArray(new Short[coll.size()]));
    }

    protected ShortStream createShortStream(ShortIterator iter) {
        return iter == null ? ShortStream.empty() : ShortStream.of(iter.toArray());
    }

    protected ShortStream createShortStream(ShortBuffer buff) {
        return ShortStream.of(buff);
    }

    // ---- perf review 2026-09-26 G088 begin ----
    protected static short[] g088DistinctOracle(final short[] values, final int fromIndex, final int toIndex) {
        final java.util.LinkedHashSet<Short> set = new java.util.LinkedHashSet<>();

        for (int i = fromIndex; i < toIndex; i++) {
            set.add(values[i]);
        }

        final short[] result = new short[set.size()];
        int index = 0;

        for (final Short value : set) {
            result[index++] = value;
        }

        return result;
    }

    protected static void g088CheckDistinct(final short[] values) {
        final short[] expected = g088DistinctOracle(values, 0, values.length);

        assertArrayEquals(expected, ShortStream.of(values).distinct().toArray());
        assertEquals(expected.length, ShortStream.of(values).distinct().count());
        assertArrayEquals(Arrays.copyOfRange(expected, 3, expected.length), ShortStream.of(values).distinct().skip(3).toArray());
        assertArrayEquals(Arrays.copyOf(expected, 3), ShortStream.of(values).distinct().limit(3).toArray());
        assertArrayEquals(g088DistinctOracle(values, 2, 19), ShortStream.of(values, 2, 19).distinct().toArray());
        assertArrayEquals(g088DistinctOracle(values, 1, values.length - 1), ShortStream.of(values, 1, values.length - 1).distinct().toArray());

        final ShortIterator iter = ShortStream.of(values).distinct().iterator();
        for (final short value : expected) {
            assertTrue(iter.hasNext());
            assertEquals(value, iter.nextShort());
        }
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
    }

    // ---- perf review 2026-09-26 G112 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static short[] flattenVerticallyReferenceG114(final short[][] a) {
        final ShortList ret = new ShortList();
        int maxLen = 0;

        for (final short[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final short[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final short[][] a, final int skip) {
        final short[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, ShortStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, ShortStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), ShortStream.flatten(a, true).skip(skip).toArray(),
                message);

        final ShortIterator iter = ShortStream.flatten(a, true).iterator();

        for (final short element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextShort());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertFalse(iter.hasNext());
    }
}
