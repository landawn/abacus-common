package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class IOUtilReaderConstructionReview20261003Test extends TestBase {
    @TempDir
    Path temporaryDirectory;

    static Stream<Arguments> constructionFailures() {
        final RuntimeException sameFailure = new IllegalStateException("shared failure");
        return Stream.of(Arguments.of(new IllegalStateException("decoder"), null), Arguments.of(new AssertionError("decoder"), null),
                Arguments.of(new IllegalStateException("decoder"), new IOException("close")),
                Arguments.of(new AssertionError("decoder"), new AssertionError("close")), Arguments.of(sameFailure, sameFailure));
    }

    @ParameterizedTest
    @MethodSource("constructionFailures")
    void failedProviderReaderConstructionClosesOwnedInputAndRetainsFailure(final Throwable failure, final Throwable closeFailure) throws IOException {
        final TrackedInput input = new TrackedInput(closeFailure);
        final Path path = providerPath(input);

        assertSame(failure, assertThrows(failure.getClass(), () -> IOUtil.newBufferedReader(path, failingCharset(failure))));
        assertEquals(1, input.closes);
        assertEquals(0, input.reads);
        assertEquals(closeFailure == null || closeFailure == failure ? 0 : 1, failure.getSuppressed().length);
        if (closeFailure != null && closeFailure != failure) {
            assertSame(closeFailure, failure.getSuppressed()[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void successfulProviderReaderTransfersOwnershipAndPreservesReplacement(final boolean defaultCharset) throws IOException {
        final TrackedInput input = new TrackedInput(null);
        final Path path = providerPath(input);

        try (BufferedReader reader = IOUtil.newBufferedReader(path, defaultCharset ? null : StandardCharsets.UTF_8)) {
            assertEquals(0, input.closes);
            assertEquals(0, input.reads);
            assertEquals("a\uFFFDb", reader.readLine());
            assertEquals(0, input.closes);
        }
        assertEquals(1, input.closes);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void failedCallerOwnedReaderConstructionLeavesInputWithCaller(final boolean error) {
        final Throwable failure = error ? new AssertionError("decoder") : new IllegalStateException("decoder");
        final TrackedInput input = new TrackedInput(null);

        assertSame(failure, assertThrows(failure.getClass(), () -> IOUtil.newBufferedReader(input, failingCharset(failure))));
        assertEquals(0, input.closes);
        assertEquals(0, input.reads);
    }

    @Test
    void defaultFilesystemPathStillUsesTheFileReaderContract() throws IOException {
        final Path path = temporaryDirectory.resolve("malformed.txt");
        Files.write(path, new byte[] { 'a', (byte) 0xFF, 'b' });
        try (BufferedReader reader = IOUtil.newBufferedReader(path, StandardCharsets.UTF_8)) {
            assertEquals("a\uFFFDb", reader.readLine());
        }
        Files.delete(path);
    }

    private static Path providerPath(final TrackedInput input) throws IOException {
        final Path path = mock(Path.class);
        final FileSystem fileSystem = mock(FileSystem.class);
        final FileSystemProvider provider = mock(FileSystemProvider.class);
        final BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(provider);
        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attributes);
        when(provider.newInputStream(path)).thenReturn(input);
        return path;
    }

    private static Charset failingCharset(final Throwable failure) {
        return new Charset("x-g054-failing", null) {
            @Override
            public boolean contains(final Charset charset) {
                return false;
            }

            @Override
            public CharsetDecoder newDecoder() {
                if (failure instanceof Error error) {
                    throw error;
                }
                throw (RuntimeException) failure;
            }

            @Override
            public CharsetEncoder newEncoder() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class TrackedInput extends ByteArrayInputStream {
        private final Throwable closeFailure;
        private int closes;
        private int reads;

        private TrackedInput(final Throwable closeFailure) {
            super(new byte[] { 'a', (byte) 0xFF, 'b' });
            this.closeFailure = closeFailure;
        }

        @Override
        public synchronized int read(final byte[] bytes, final int offset, final int length) {
            reads++;
            return super.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closes++;
            if (closeFailure instanceof IOException exception) {
                throw exception;
            } else if (closeFailure instanceof Error error) {
                throw error;
            } else if (closeFailure instanceof RuntimeException exception) {
                throw exception;
            }
        }
    }
}
