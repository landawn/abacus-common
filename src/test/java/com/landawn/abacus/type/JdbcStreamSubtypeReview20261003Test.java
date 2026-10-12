package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("unit")
class JdbcStreamSubtypeReview20261003Test extends TestBase {
    private static final byte[] BYTES = { 0, 1, (byte) 128, (byte) 255 };
    private static final String TEXT = "a\u00e9\ud83d\ude00\u0000z";

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void binaryWrappersKeepRawContentAndReadLazily(final boolean byIndex) throws Exception {
        for (Class<? extends InputStream> target : List.of(BufferedInputStream.class, DataInputStream.class, PushbackInputStream.class,
                FilterInputStream.class)) {
            final TrackedInput source = new TrackedInput();
            final InputStream result = get(Type.of(target), source, byIndex);
            assertTrue(target.isInstance(result), target.getName());
            assertEquals(0, source.reads);
            assertEquals(0, source.closes);
            assertArrayEquals(BYTES, result.readAllBytes());
            result.close();
            assertEquals(1, source.closes);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void binaryCopyClosesAcquiredSource(final boolean byIndex) throws Exception {
        final TrackedInput source = new TrackedInput();
        final ByteArrayInputStream result = get(Type.of(ByteArrayInputStream.class), source, byIndex);
        assertEquals(1, source.closes);
        assertArrayEquals(BYTES, result.readAllBytes());
        result.close();
        assertEquals(1, source.closes);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void characterWrappersKeepUnicodeAndReadLazily(final boolean byIndex) throws Exception {
        for (Class<? extends Reader> target : List.of(BufferedReader.class, PushbackReader.class, FilterReader.class)) {
            final TrackedReader source = new TrackedReader();
            final Reader result = get(Type.of(target), source, byIndex);
            assertTrue(target.isInstance(result), target.getName());
            assertEquals(0, source.reads);
            assertEquals(0, source.closes);
            assertEquals(TEXT, read(result));
            result.close();
            assertEquals(1, source.closes);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void characterCopiesCloseAcquiredSource(final boolean byIndex) throws Exception {
        for (Class<? extends Reader> target : List.of(StringReader.class, CharArrayReader.class)) {
            final TrackedReader source = new TrackedReader();
            final Reader result = get(Type.of(target), source, byIndex);
            assertTrue(target.isInstance(result), target.getName());
            assertEquals(1, source.closes);
            assertEquals(TEXT, read(result));
            result.close();
            assertEquals(1, source.closes);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void baseMatchingAndNullValuesKeepIdentity(final boolean byIndex) throws Exception {
        final TrackedInput binary = new TrackedInput();
        final TrackedReader chars = new TrackedReader();
        assertSame(binary, get(Type.of(InputStream.class), binary, byIndex));
        assertSame(chars, get(Type.of(Reader.class), chars, byIndex));
        assertSame(binary, get(Type.of(TrackedInput.class), binary, byIndex));
        assertSame(chars, get(Type.of(TrackedReader.class), chars, byIndex));
        assertEquals(0, binary.reads + binary.closes + chars.reads + chars.closes);

        for (Class<? extends InputStream> target : List.of(InputStream.class, ByteArrayInputStream.class, BufferedInputStream.class, DataInputStream.class,
                PushbackInputStream.class, FilterInputStream.class, TrackedInput.class)) {
            assertNull(get(Type.of(target), null, byIndex));
        }
        for (Class<? extends Reader> target : List.of(Reader.class, StringReader.class, CharArrayReader.class, BufferedReader.class, PushbackReader.class,
                FilterReader.class, TrackedReader.class)) {
            assertNull(get(Type.of(target), null, byIndex));
        }

        final ByteArrayInputStream bytes = new ByteArrayInputStream(BYTES);
        final StringReader text = new StringReader(TEXT);
        assertSame(bytes, get(Type.of(ByteArrayInputStream.class), bytes, byIndex));
        assertSame(text, get(Type.of(StringReader.class), text, byIndex));
        assertArrayEquals(BYTES, bytes.readAllBytes());
        assertEquals(TEXT, read(text));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void copyFailuresCloseAndKeepOriginalCause(final boolean byIndex) {
        final IOException readFailure = new IOException("read");
        final IOException closeFailure = new IOException("close");
        final TrackedInput binary = new TrackedInput();
        binary.readFailure = readFailure;
        binary.closeFailure = closeFailure;
        final UncheckedIOException bytesThrown = assertThrows(UncheckedIOException.class, () -> get(Type.of(ByteArrayInputStream.class), binary, byIndex));
        assertSame(readFailure, bytesThrown.getCause());
        assertArrayEquals(new Throwable[] { closeFailure }, bytesThrown.getSuppressed());
        assertEquals(1, binary.closes);

        for (Class<? extends Reader> target : List.of(StringReader.class, CharArrayReader.class)) {
            final TrackedReader chars = new TrackedReader();
            chars.readFailure = readFailure;
            chars.closeFailure = closeFailure;
            final UncheckedIOException charsThrown = assertThrows(UncheckedIOException.class, () -> get(Type.of(target), chars, byIndex));
            assertSame(readFailure, charsThrown.getCause());
            assertArrayEquals(new Throwable[] { closeFailure }, charsThrown.getSuppressed());
            assertEquals(1, chars.closes);
        }

        final TrackedInput closeOnly = new TrackedInput();
        closeOnly.closeFailure = closeFailure;
        assertSame(closeFailure, assertThrows(UncheckedIOException.class, () -> get(Type.of(ByteArrayInputStream.class), closeOnly, byIndex)).getCause());
        assertEquals(1, closeOnly.closes);
        final TrackedReader closeOnlyReader = new TrackedReader();
        closeOnlyReader.closeFailure = closeFailure;
        assertSame(closeFailure, assertThrows(UncheckedIOException.class, () -> get(Type.of(StringReader.class), closeOnlyReader, byIndex)).getCause());
        assertEquals(1, closeOnlyReader.closes);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void unsupportedMismatchClosesWithoutConsuming(final boolean byIndex) {
        final IOException closeFailure = new IOException("close");
        final TrackedInput binary = new TrackedInput();
        binary.closeFailure = closeFailure;
        final UnsupportedOperationException bytesThrown = assertThrows(UnsupportedOperationException.class,
                () -> get(Type.of(UnsupportedInput.class), binary, byIndex));
        assertEquals(0, binary.reads);
        assertEquals(1, binary.closes);
        assertArrayEquals(new Throwable[] { closeFailure }, bytesThrown.getSuppressed());

        final TrackedReader chars = new TrackedReader();
        chars.closeFailure = closeFailure;
        final UnsupportedOperationException charsThrown = assertThrows(UnsupportedOperationException.class,
                () -> get(Type.of(UnsupportedReader.class), chars, byIndex));
        assertEquals(0, chars.reads);
        assertEquals(1, chars.closes);
        assertArrayEquals(new Throwable[] { closeFailure }, charsThrown.getSuppressed());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void jdbcFailuresPropagateUnchanged(final boolean byIndex) {
        final SQLException failure = new SQLException("jdbc");
        final ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ResultSet.class }, (proxy, method, args) -> {
            throw failure;
        });
        for (Type<?> type : List.of(Type.of(BufferedInputStream.class), Type.of(BufferedReader.class))) {
            assertSame(failure, assertThrows(SQLException.class, () -> {
                if (byIndex) {
                    type.get(rs, 1);
                } else {
                    type.get(rs, "content");
                }
            }));
        }
    }

    private static <T> T get(final Type<T> type, final Object source, final boolean byIndex) throws SQLException {
        final AtomicInteger calls = new AtomicInteger();
        final ResultSet rs = (ResultSet) Proxy.newProxyInstance(JdbcStreamSubtypeReview20261003Test.class.getClassLoader(), new Class<?>[] { ResultSet.class },
                (proxy, method, args) -> {
                    assertEquals(type.isInputStream() ? "getBinaryStream" : "getCharacterStream", method.getName());
                    assertEquals(byIndex ? 1 : "content", args[0]);
                    calls.incrementAndGet();
                    return source;
                });
        try {
            return byIndex ? type.get(rs, 1) : type.get(rs, "content");
        } finally {
            assertEquals(1, calls.get());
        }
    }

    private static String read(final Reader reader) throws IOException {
        final StringBuilder text = new StringBuilder();
        final char[] buffer = new char[8];
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            text.append(buffer, 0, count);
        }
        return text.toString();
    }

    public static class TrackedInput extends InputStream {
        int reads;
        int closes;
        int position;
        IOException readFailure;
        IOException closeFailure;

        @Override
        public int read() throws IOException {
            reads++;
            if (readFailure != null) {
                throw readFailure;
            }
            return position == BYTES.length ? -1 : Byte.toUnsignedInt(BYTES[position++]);
        }

        @Override
        public void close() throws IOException {
            closes++;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    public static class TrackedReader extends Reader {
        int reads;
        int closes;
        int position;
        IOException readFailure;
        IOException closeFailure;

        @Override
        public int read(final char[] buffer, final int offset, final int length) throws IOException {
            reads++;
            if (readFailure != null) {
                throw readFailure;
            }
            if (length == 0) {
                return 0;
            } else if (position == TEXT.length()) {
                return -1;
            }
            final int count = Math.min(length, TEXT.length() - position);
            TEXT.getChars(position, position + count, buffer, offset);
            position += count;
            return count;
        }

        @Override
        public void close() throws IOException {
            closes++;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    public static class UnsupportedInput extends TrackedInput {
    }

    public static class UnsupportedReader extends TrackedReader {
    }
}
