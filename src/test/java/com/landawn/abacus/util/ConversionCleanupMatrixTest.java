package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;

/** A conversion owns its consumed source and must preserve a read failure when cleanup also fails. */
public class ConversionCleanupMatrixTest extends TestBase {
    static java.util.stream.Stream<Arguments> lobFailures() {
        List<Arguments> cases = new ArrayList<>();
        for (Class<?> target : List.of(byte[].class, char[].class, String.class)) {
            for (String read : List.of("success", "checked", "runtime", "error")) {
                for (String cleanup : List.of("success", "checked", "runtime", "error")) {
                    cases.add(Arguments.of(target, read, cleanup));
                }
            }
        }
        return cases.stream();
    }

    static java.util.stream.Stream<Arguments> streamFailures() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean reader : new boolean[] { false, true }) {
            for (Class<?> target : List.of(reader ? char[].class : byte[].class, String.class)) {
                for (String read : List.of("success", "checked", "runtime", "error")) {
                    for (String cleanup : List.of("success", "checked", "runtime", "error")) {
                        cases.add(Arguments.of(reader, target, read, cleanup));
                    }
                }
            }
        }
        return cases.stream();
    }

    private static Throwable failure(String kind, boolean sql) {
        return switch (kind) {
            case "success" -> null;
            case "checked" -> sql ? new SQLException("SQL failure") : new IOException("I/O failure");
            case "runtime" -> new IllegalStateException("runtime failure");
            case "error" -> new AssertionError("error failure");
            default -> throw new AssertionError(kind);
        };
    }

    @ParameterizedTest(name = "{0}: read={1}, free={2}")
    @MethodSource("lobFailures")
    void lobIsFreedOnceAndReadFailureWins(Class<?> target, String readKind, String freeKind) {
        Throwable readFailure = failure(readKind, true);
        Throwable freeFailure = failure(freeKind, true);
        AtomicInteger frees = new AtomicInteger();
        Class<?> lobType = target == byte[].class ? Blob.class : Clob.class;
        Object lob = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { lobType }, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "length" -> 7L;
                case "getBytes", "getSubString" -> {
                    assertEquals(1L, args[0]);
                    assertEquals(7, args[1]);
                    if (readFailure != null) {
                        throw readFailure;
                    }
                    yield lobType == Blob.class ? "payload".getBytes(StandardCharsets.UTF_8) : "payload";
                }
                case "free" -> {
                    frees.incrementAndGet();
                    if (freeFailure != null) {
                        throw freeFailure;
                    }
                    yield null;
                }
                default -> throw new AssertionError("Unexpected LOB call: " + method);
            };
        });
        verifyOutcome(() -> assertConverted(target, N.convert(lob, target)), readFailure, freeFailure);
        assertEquals(1, frees.get());
    }

    @ParameterizedTest(name = "reader={0}, {1}: read={2}, close={3}")
    @MethodSource("streamFailures")
    void consumedStreamIsClosedOnceAndReadFailureWins(boolean reader, Class<?> target, String readKind, String closeKind) {
        Throwable readFailure = failure(readKind, false);
        Throwable closeFailure = failure(closeKind, false);
        AtomicInteger closes = new AtomicInteger();
        Object source;
        if (reader) {
            source = new Reader() {
                private final StringReader delegate = new StringReader("payload");

                @Override
                public int read(char[] buffer, int offset, int length) throws IOException {
                    throwIfPresent(readFailure);
                    return delegate.read(buffer, offset, length);
                }

                @Override
                public void close() throws IOException {
                    closes.incrementAndGet();
                    throwIfPresent(closeFailure);
                }
            };
        } else {
            source = new InputStream() {
                private final ByteArrayInputStream delegate = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8));

                @Override
                public int read() throws IOException {
                    throwIfPresent(readFailure);
                    return delegate.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    throwIfPresent(readFailure);
                    return delegate.read(buffer, offset, length);
                }

                @Override
                public void close() throws IOException {
                    closes.incrementAndGet();
                    throwIfPresent(closeFailure);
                }
            };
        }
        verifyOutcome(() -> assertConverted(target, N.convert(source, target)), readFailure, closeFailure);
        assertEquals(1, closes.get());
    }

    @Test
    void oversizedLobIsFreedWithoutRequestingAnUnrepresentableArray() {
        for (Class<?> target : List.of(byte[].class, char[].class, String.class)) {
            AtomicInteger frees = new AtomicInteger();
            Object lob = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { target == byte[].class ? Blob.class : Clob.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("length")) {
                            return (long) Integer.MAX_VALUE + 1;
                        }
                        if (method.getName().equals("free")) {
                            frees.incrementAndGet();
                            return null;
                        }
                        throw new AssertionError("Must check length before reading: " + method);
                    });
            assertThrows(IllegalArgumentException.class, () -> N.convert(lob, target));
            assertEquals(1, frees.get());
        }
    }

    private static void assertConverted(Class<?> target, Object actual) {
        if (target == byte[].class) {
            assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), (byte[]) actual);
        } else if (target == char[].class) {
            assertArrayEquals("payload".toCharArray(), (char[]) actual);
        } else {
            assertEquals("payload", actual);
        }
    }

    private static void verifyOutcome(Executable conversion, Throwable primary, Throwable cleanup) {
        if (primary == null && cleanup == null) {
            assertDoesNotThrow(conversion);
            return;
        }
        Throwable actual = assertThrows(Throwable.class, conversion);
        assertWrappedFailure(primary == null ? cleanup : primary, actual);
        if (primary != null && cleanup != null) {
            assertEquals(1, actual.getSuppressed().length);
            assertWrappedFailure(cleanup, actual.getSuppressed()[0]);
        } else {
            assertEquals(0, actual.getSuppressed().length);
        }
    }

    private static void assertWrappedFailure(Throwable expected, Throwable actual) {
        if (expected instanceof SQLException) {
            assertInstanceOf(UncheckedSQLException.class, actual);
            assertSame(expected, actual.getCause());
        } else if (expected instanceof IOException) {
            assertInstanceOf(UncheckedIOException.class, actual);
            assertSame(expected, actual.getCause());
        } else {
            assertSame(expected, actual);
        }
    }

    private static void throwIfPresent(Throwable failure) throws IOException {
        if (failure instanceof IOException io) {
            throw io;
        } else if (failure instanceof RuntimeException runtime) {
            throw runtime;
        } else if (failure instanceof Error error) {
            throw error;
        }
    }
}
