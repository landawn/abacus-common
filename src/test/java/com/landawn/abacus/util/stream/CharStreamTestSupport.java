package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;

import com.google.common.base.Strings;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.IOUtil;

/** Shared fixtures and lifecycle for the CharStream contract tests. */
public abstract class CharStreamTestSupport extends TestBase {

    protected CharStream charStream;

    protected CharStream stream;

    protected CharStream stream2;

    protected CharStream stream3;

    @BeforeEach
    public void setUp() {
        charStream = createCharStream('a', 'b', 'c', 'd', 'e');
        stream = createCharStream("hello");
        stream2 = createCharStream("world");
        stream3 = createCharStream("abc");
    }

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements);
    }

    protected CharStream createCharStream(char[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex);
    }

    protected CharStream createCharStream(Character[] array) {
        return CharStream.of(array);
    }

    protected CharStream createCharStream(Character[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex);
    }

    protected CharStream createCharStream(Collection<Character> coll) {
        return CharStream.of(coll.toArray(new Character[coll.size()]));
    }

    protected CharStream createCharStream(Collection<Character> coll, int fromIndex, int toIndex) {
        return CharStream.of(coll.toArray(new Character[coll.size()]), fromIndex, toIndex);
    }

    protected CharStream createCharStream(CharIterator iter) {
        return iter == null ? CharStream.empty() : CharStream.of(iter.toArray());
    }

    protected CharStream createCharStream(CharBuffer buff) {
        return buff == null ? CharStream.empty() : CharStream.of(buff);
    }

    protected CharStream createCharStream(File file) {
        return CharStream.of(IOUtil.readAllChars(file));
    }

    protected CharStream createCharStream(Reader reader) {
        return CharStream.of(IOUtil.readAllChars(reader));
    }

    protected CharStream createCharStream(Reader reader, boolean closeReader) {
        return CharStream.of(IOUtil.readAllChars(reader)).onClose(() -> {
            if (closeReader) {
                IOUtil.closeQuietly(reader);
            }
        });
    }

    protected CharStream createCharStream(CharSequence str) {
        return CharStream.of(Strings.nullToEmpty(str == null ? "" : str.toString()).toCharArray());
    }

    protected CharStream createCharStream(CharSequence str, int fromIndex, int toIndex) {
        return CharStream.of(Strings.nullToEmpty(str == null ? "" : str.toString()).toCharArray(), fromIndex, toIndex);
    }

    protected Map.Entry<Character, Integer> entry(char key, int value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    protected static char[] g112Chars(final int length) {
        final char[] data = new char[length];

        for (int i = 0; i < length; i++) {
            data[i] = (char) ('a' + (i * 7) % 26);
        }

        return data;
    }

    // Returns at most chunk chars per read and throws an IOException on read call number failAtCall (0-based; -1 = never).
    protected static final class G112ChunkedReader extends Reader {
        private final char[] data;
        private final int chunk;
        private final int failAtCall;
        private int pos = 0;
        int readCalls = 0;
        int closeCalls = 0;

        G112ChunkedReader(final char[] data, final int chunk, final int failAtCall) {
            this.data = data;
            this.chunk = chunk;
            this.failAtCall = failAtCall;
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            final int call = readCalls++;

            if (call == failAtCall) {
                throw new IOException("read failure at call " + call);
            }

            if (pos >= data.length) {
                return -1;
            }

            final int n = Math.min(Math.min(chunk, len), data.length - pos);
            System.arraycopy(data, pos, cbuf, off, n);
            pos += n;
            return n;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }

    // ---- perf review 2026-09-26 G112 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static char[] flattenVerticallyReferenceG114(final char[][] a) {
        final CharList ret = new CharList();
        int maxLen = 0;

        for (final char[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final char[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final char[][] a, final int skip) {
        final char[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, CharStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, CharStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), CharStream.flatten(a, true).skip(skip).toArray(),
                message);

        final CharIterator iter = CharStream.flatten(a, true).iterator();

        for (final char element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextChar());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertFalse(iter.hasNext());
    }
}
