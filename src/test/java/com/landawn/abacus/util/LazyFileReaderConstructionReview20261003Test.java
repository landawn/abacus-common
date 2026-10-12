package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class LazyFileReaderConstructionReview20261003Test extends TestBase {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({ "txt,false", "gz,false", "zip,false", "txt,true", "gz,true", "zip,true" })
    void constructionFailureReleasesOwnedFile(final String extension, final boolean error) throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "Windows delete detects an open owned file handle");
        final Path file = temporaryDirectory.resolve("input." + extension);
        write(file, extension);
        final Path output = temporaryDirectory.resolve("child.log");
        // An isolated JVM releases a baseline leak before TempDir cleanup, so a failed regression cannot leave
        // the suite holding the file or turn the primary failure into an unrelated cleanup failure.
        final Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx128m", "-XX:+UseSerialGC", "-cp",
                System.getProperty("java.class.path"), LazyFileReaderConstructionReview20261003Test.class.getName(), file.toString(), Boolean.toString(error))
                        .redirectErrorStream(true)
                        .redirectOutput(output.toFile())
                        .start();
        try {
            assertTrue(child.waitFor(20, TimeUnit.SECONDS), "child must finish");
            assertEquals(0, child.exitValue(), Files.readString(output));
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
                assertTrue(child.waitFor(10, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * The Windows-only test above detects a leaked handle through delete semantics. The original-failure identity, and
     * where the platform exposes open descriptors ({@code /proc/self/fd} or {@code /dev/fd}), the release of every
     * descriptor the failed construction opened, are checked in-process on every platform.
     */
    @ParameterizedTest
    @CsvSource({ "txt,false", "gz,false", "zip,false", "txt,true", "gz,true", "zip,true" })
    void constructionFailurePreservesTheOriginalFailureOnEveryPlatform(final String extension, final boolean error) throws Exception {
        final Path file = temporaryDirectory.resolve("input." + extension);
        write(file, extension);
        final Throwable failure = error ? new AssertionError("decoder") : new IllegalStateException("decoder");

        // The first attempt loads every class on the path, so the measured attempt opens only the file itself.
        assertSame(failure, assertThrows(Throwable.class, () -> readWithFailingDecoder(file, failure)));
        final long before = openDescriptorCount();
        assertSame(failure, assertThrows(Throwable.class, () -> readWithFailingDecoder(file, failure)));
        assertEquals(before, openDescriptorCount(), "a failed reader construction must release the file it opened");
        Files.delete(file);
    }

    /** Counts this process's open descriptors, or returns -1 where the platform does not list them (Windows). */
    private static long openDescriptorCount() throws IOException {
        for (final String directory : new String[] { "/proc/self/fd", "/dev/fd" }) {
            final Path path = Path.of(directory);
            if (!IOUtil.IS_OS_WINDOWS && Files.isDirectory(path)) {
                try (Stream<Path> entries = Files.list(path)) {
                    return entries.count();
                }
            }
        }
        return -1;
    }

    @ParameterizedTest
    @ValueSource(strings = { "txt", "gz", "zip" })
    void successfulIterationStillReadsAndReleasesEveryFileKind(final String extension) throws IOException {
        final Path file = temporaryDirectory.resolve("input." + extension);
        write(file, extension);
        final List<String> lines = new ArrayList<>();
        IOUtil.forEachLine(file.toFile(), lines::add);
        assertEquals(List.of("line"), lines);
        Files.delete(file);
    }

    public static void main(final String[] args) throws Exception {
        final Path file = Path.of(args[0]);
        final Throwable failure = Boolean.parseBoolean(args[1]) ? new AssertionError("decoder") : new IllegalStateException("decoder");
        try {
            readWithFailingDecoder(file, failure);
            throw new AssertionError("decoder failure missing");
        } catch (final RuntimeException | Error actual) {
            if (actual != failure) {
                throw new AssertionError("original decoder failure must be preserved", actual);
            }
        }
        Files.delete(file);
    }

    /** Iterates {@code file} with a charset whose decoder construction throws {@code failure}. */
    private static void readWithFailingDecoder(final Path file, final Throwable failure) {
        final Charset charset = new Charset("x-g055-failing", null) {
            @Override
            public boolean contains(final Charset other) {
                return false;
            }

            @Override
            public CharsetEncoder newEncoder() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CharsetDecoder newDecoder() {
                if (failure instanceof Error error) {
                    throw error;
                }
                throw (RuntimeException) failure;
            }
        };
        IOUtil.forEachLine(file.toFile(), IOUtil.LineIterationOptions.builder().charset(charset).build(), line -> {
            throw new AssertionError("no line action before reader construction");
        });
    }

    private static void write(final Path file, final String extension) throws IOException {
        final byte[] content = "line\n".getBytes(StandardCharsets.UTF_8);
        if (extension.equals("gz")) {
            try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(file))) {
                output.write(content);
            }
        } else if (extension.equals("zip")) {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file))) {
                output.putNextEntry(new ZipEntry("input.txt"));
                output.write(content);
                output.closeEntry();
            }
        } else {
            Files.write(file, content);
        }
    }
}
