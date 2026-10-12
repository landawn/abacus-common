package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.Objectory;

@Tag("unit")
class TypeMemoryReviewTest extends TestBase {

    @Test
    void patternObjectConversionRetainsExternalCompilationFlags() {
        final Type<Pattern> type = Type.of(Pattern.class);
        final Pattern source = Pattern.compile("a.b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern converted = type.valueOf((Object) source);

        assertSame(source, converted);
        assertEquals(source.flags(), converted.flags());
        assertTrue(converted.matcher("A.B").matches());
        assertFalse(converted.matcher("axb").matches());
        assertNull(type.valueOf((Object) null));
        assertTrue(type.valueOf((Object) "a.b").matcher("axb").matches());
    }

    @Test
    void byteArrayObjectConversionKeepsAnIndependentCopyOfEveryByteValue() {
        final Type<byte[]> type = Type.of(byte[].class);
        final byte[] input = new byte[256];

        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }

        final byte[] converted = type.valueOf((Object) input);
        assertArrayEquals(input, converted);
        assertNotSame(input, converted);
        converted[0] = 1;
        assertEquals(0, input[0]);
        assertArrayEquals(new byte[0], type.valueOf((Object) new byte[0]));
        assertNull(type.valueOf((Object) null));
        assertArrayEquals(new byte[] { -128, 0, 127 }, type.valueOf((Object) "[-128, 0, 127]"));
    }

    @Test
    void charArrayObjectConversionPreservesAllUtf16CodeUnitsAndCopyIsolation() {
        final Type<char[]> type = Type.of(char[].class);
        final char[] input = new char[Character.MAX_VALUE + 1];

        for (int i = 0; i < input.length; i++) {
            input[i] = (char) i;
        }

        final char[] converted = type.valueOf((Object) input);
        assertArrayEquals(input, converted);
        assertNotSame(input, converted);
        converted[0] = 'x';
        assertEquals('\0', input[0]);
        assertArrayEquals(new char[0], type.valueOf((Object) new char[0]));
        assertNull(type.valueOf((Object) null));
        assertArrayEquals(new char[] { '\n', '\'', '\\' }, type.valueOf((Object) "['\\n', '\\'', '\\\\']"));
    }

    @Test
    void readerAppendsIncrementallyAndLeavesTheSourceOpen() throws IOException {
        final String content = "a\u0000\uD83D\uDE03\n\"<&".repeat(20_000);
        final CountingReader reader = new CountingReader(content);
        final StringBuilder output = new StringBuilder();
        final int[] largestAppend = { 0 };
        final Appendable destination = new Appendable() {
            @Override
            public Appendable append(final CharSequence chars) {
                largestAppend[0] = Math.max(largestAppend[0], chars.length());
                output.append(chars);
                return this;
            }

            @Override
            public Appendable append(final CharSequence chars, final int start, final int end) {
                return append(chars.subSequence(start, end));
            }

            @Override
            public Appendable append(final char ch) {
                return append(String.valueOf(ch));
            }
        };

        Type.of(Reader.class).appendTo(destination, reader);

        assertEquals(content, output.toString());
        assertEquals(content.length(), reader.consumed);
        assertTrue(largestAppend[0] < content.length(), "Reader content must not be retained as one temporary String");
        assertFalse(reader.closed);
    }

    @Test
    void failedAppendStopsConsumingTheReaderAndKeepsTheOriginalException() {
        final String content = "0123456789".repeat(100_000);
        final CountingReader reader = new CountingReader(content);
        final IOException failure = new IOException("destination failed");
        final Appendable destination = new Appendable() {
            @Override
            public Appendable append(final CharSequence chars) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final CharSequence chars, final int start, final int end) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final char ch) throws IOException {
                throw failure;
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> Type.of(Reader.class).appendTo(destination, reader)));
        assertTrue(reader.consumed < content.length(), "A failed destination must not force consumption of the entire source");
        assertFalse(reader.closed);
    }

    @Test
    void nonWriterReadFailuresRemainUncheckedAndDoNotCloseTheSource() {
        final IOException failure = new IOException("source failed");
        final boolean[] closed = { false };
        final Reader reader = new Reader() {
            @Override
            public int read(final char[] buffer, final int offset, final int length) throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };

        final UncheckedIOException actual = assertThrows(UncheckedIOException.class, () -> Type.of(Reader.class).appendTo(new StringBuilder(), reader));
        assertSame(failure, actual.getCause());
        assertFalse(closed[0]);
    }

    @Test
    void nonWriterStopsAfterZeroProgressAndKeepsPreviouslyReadContent() throws IOException {
        for (final String content : new String[] { "", "partial content" }) {
            final int expectedReads = content.isEmpty() ? 2 : 4;
            final int[] reads = { 0 };
            final boolean[] closed = { false };
            final Reader reader = new Reader() {
                @Override
                public int read(final char[] buffer, final int offset, final int length) {
                    // Bound the regression: the old loop fails here instead of hanging the test suite.
                    if (++reads[0] > expectedReads) {
                        throw new AssertionError("Reading continued after a read and its retry both returned zero");
                    }

                    if (reads[0] == 1 && !content.isEmpty()) {
                        content.getChars(0, content.length(), buffer, offset);
                        return content.length();
                    }

                    return 0;
                }

                @Override
                public void close() {
                    closed[0] = true;
                }
            };
            final StringBuilder output = new StringBuilder("prefix:");

            Type.of(Reader.class).appendTo(output, reader);

            assertEquals("prefix:" + content, output.toString());
            assertEquals(expectedReads, reads[0]);
            assertFalse(closed[0]);
        }
    }

    @Test
    void serializeToStopsAfterZeroProgressAndKeepsPreviouslyReadContent() throws IOException {
        for (final JsonSerConfig config : new JsonSerConfig[] { JsonSerConfig.create(), null }) {
            for (final String content : new String[] { "", "partial \"content\"" }) {
                final int expectedReads = content.isEmpty() ? 2 : 4;
                final int[] reads = { 0 };
                final Reader reader = new Reader() {
                    @Override
                    public int read(final char[] buffer, final int offset, final int length) {
                        // Bound the regression: the old loop fails here instead of hanging the test suite.
                        if (++reads[0] > expectedReads) {
                            throw new AssertionError("Reading continued after a read and its retry both returned zero");
                        }

                        if (reads[0] == 1 && !content.isEmpty()) {
                            content.getChars(0, content.length(), buffer, offset);
                            return content.length();
                        }

                        return 0;
                    }

                    @Override
                    public void close() {
                    }
                };
                final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

                try {
                    Type.of(Reader.class).serializeTo(writer, reader, config);

                    final String escaped = content.replace("\"", "\\\"");
                    assertEquals(config == null ? escaped : "\"" + escaped + "\"", writer.toString());
                    assertEquals(expectedReads, reads[0]);
                } finally {
                    Objectory.recycle(writer);
                }
            }
        }

        // An ordinary reader is still read to its end.
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            Type.of(Reader.class).serializeTo(writer, new StringReader("abc"), JsonSerConfig.create());
            assertEquals("\"abc\"", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void emptyReaderPreservesDestinationAndStillRejectsANullAppendable() throws IOException {
        final Type<Reader> type = Type.of(Reader.class);
        final StringBuilder output = new StringBuilder("prefix");
        type.appendTo(output, new StringReader(""));
        assertEquals("prefix", output.toString());
        assertThrows(NullPointerException.class, () -> type.appendTo(null, new StringReader("")));
        type.appendTo(output, null);
        assertEquals("prefixnull", output.toString());
    }

    private static final class CountingReader extends StringReader {
        private int consumed;
        private boolean closed;

        CountingReader(final String content) {
            super(content);
        }

        @Override
        public int read(final char[] buffer, final int offset, final int length) throws IOException {
            final int count = super.read(buffer, offset, length);
            consumed += Math.max(count, 0);
            return count;
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }
}
