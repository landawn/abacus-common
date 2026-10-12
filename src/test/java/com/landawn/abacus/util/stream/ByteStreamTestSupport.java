package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;

/** Shared fixtures and lifecycle for the ByteStream contract tests. */
public abstract class ByteStreamTestSupport extends TestBase {

    protected static final byte[] TEST_ARRAY = { 1, 2, 3, 4, 5 };

    protected ByteStream byteStream;

    protected ByteStream createByteStream(final byte... elements) {
        return ByteStream.of(elements);
    }

    protected ByteStream createByteStream(final byte[] elements, final int fromIndex, final int toIndex) {
        return ByteStream.of(elements, fromIndex, toIndex);
    }

    protected ByteStream createByteStream(final Byte[] elements) {
        return ByteStream.of(elements);
    }

    protected ByteStream createByteStream(final Byte[] elements, final int fromIndex, final int toIndex) {
        return ByteStream.of(elements, fromIndex, toIndex);
    }

    protected ByteStream createByteStream(final Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()]));
    }

    protected ByteStream createByteStream(final ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray());
    }

    protected ByteStream createByteStream(final ByteBuffer buff) {
        return ByteStream.of(buff);
    }

    protected ByteStream createByteStream(final File file) {
        return ByteStream.of(file);
    }

    protected ByteStream createByteStream(final InputStream is) {
        return ByteStream.of(is);
    }

    protected ByteStream createByteStream(final InputStream is, final boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose);
    }

    // ---- perf review 2026-09-26 G087 end ----
    // ---- perf review 2026-09-26 G090 begin ----
    protected static final class G090ChunkedInputStream extends InputStream {
        private final byte[] data;
        private final int chunk;
        private final int failAfterReads;
        private int pos = 0;
        int readCalls = 0;

        G090ChunkedInputStream(final byte[] data, final int chunk, final int failAfterReads) {
            this.data = data;
            this.chunk = chunk;
            this.failAfterReads = failAfterReads;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            readCalls++;

            if (failAfterReads >= 0 && readCalls > failAfterReads) {
                throw new IOException("G090 read failure");
            }

            if (pos >= data.length) {
                return -1;
            }

            final int n = Math.min(Math.min(len, chunk), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
    }

    protected static byte[] g090Data(final int size) {
        final byte[] data = new byte[size];

        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i * 31 + 7);
        }

        return data;
    }

    // ---- perf review 2026-09-26 G090 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static byte[] flattenVerticallyReferenceG114(final byte[][] a) {
        final ByteList ret = new ByteList();
        int maxLen = 0;

        for (final byte[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final byte[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final byte[][] a, final int skip) {
        final byte[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, ByteStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, ByteStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), ByteStream.flatten(a, true).skip(skip).toArray(),
                message);

        final ByteIterator iter = ByteStream.flatten(a, true).iterator();

        for (final byte element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextByte());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertFalse(iter.hasNext());
    }
}
