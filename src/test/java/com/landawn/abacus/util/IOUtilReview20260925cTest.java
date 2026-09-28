/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.NonReadableChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-25 IOUtil review, cycle 2 (ledger rows C-649..C-686) and its follow-up review
 * (C-687..C-696, R1-06). Each test method name
 * carries the finding it pins.
 *
 * <p>File-system safety: every fixture lives under {@link #base()}, an absolute directory five levels below the JUnit
 * temp dir, and no test hands a relative path to a method that creates, writes, moves or deletes.
 */
public class IOUtilReview20260925cTest extends TestBase {

    @TempDir
    Path tempDir;

    private static final Charset DECODE_ONLY = Charset.forName("ISO-2022-CN");

    // ------------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------------

    /** The absolute fixture root, five levels below the JUnit temp dir. */
    private File base() throws IOException {
        final File b = tempDir.toAbsolutePath().resolve("c2").resolve("d1").resolve("d2").resolve("d3").resolve("d4").toFile();
        Files.createDirectories(b.toPath());
        assertTrue(b.isAbsolute());
        return b;
    }

    private File newFile(final String name, final byte[] content) throws IOException {
        final File f = new File(base(), name);
        Files.createDirectories(f.getParentFile().toPath());
        Files.write(f.toPath(), content);
        return f;
    }

    private File newFile(final String name, final String content) throws IOException {
        return newFile(name, content.getBytes(StandardCharsets.UTF_8));
    }

    private File newDir(final String name) throws IOException {
        final File d = new File(base(), name);
        Files.createDirectories(d.toPath());
        return d;
    }

    private static String read(final File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static byte[] randomBytes(final int n, final long seed) {
        final byte[] b = new byte[n];
        new Random(seed).nextBytes(b);
        return b;
    }

    private static Field ioField(final String name) throws NoSuchFieldException {
        final Field f = IOUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * An InputStream whose bulk reads follow a script of chunks: a chunk of n > 0 bytes is delivered across as many
     * reads as the caller's lengths need, a 0 entry answers one read with zero, and the end of the script is EOF.
     */
    private static final class ChunkScriptInputStream extends InputStream {
        private final int[] script;
        private int step = 0;
        private int leftInChunk = 0;
        private int next = 0;

        ChunkScriptInputStream(final int... script) {
            this.script = script;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException("bulk reads only");
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            if (len == 0) {
                return 0;
            }

            if (leftInChunk == 0) {
                if (step >= script.length) {
                    return -1;
                }

                final int s = script[step++];

                if (s == 0) {
                    return 0;
                }

                leftInChunk = s;
            }

            final int n = Math.min(len, leftInChunk);

            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) ('a' + (next++ % 26));
            }

            leftInChunk -= n;
            return n;
        }
    }

    /** The Reader twin of {@link ChunkScriptInputStream}. */
    private static final class ChunkScriptReader extends Reader {
        private final int[] script;
        private int step = 0;
        private int leftInChunk = 0;
        private int next = 0;

        ChunkScriptReader(final int... script) {
            this.script = script;
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            if (len == 0) {
                return 0;
            }

            if (leftInChunk == 0) {
                if (step >= script.length) {
                    return -1;
                }

                final int s = script[step++];

                if (s == 0) {
                    return 0;
                }

                leftInChunk = s;
            }

            final int n = Math.min(len, leftInChunk);

            for (int i = 0; i < n; i++) {
                cbuf[off + i] = (char) ('a' + (next++ % 26));
            }

            leftInChunk -= n;
            return n;
        }

        @Override
        public void close() {
        }
    }

    private static String alphabet(final int n) {
        final StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            sb.append((char) ('a' + (i % 26)));
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-649: readAllBytes(File) sizes a plain file's result from its length; readAllToString/readAllChars inherit it
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC649_plainFileSizesAreByteIdentical() throws IOException {
        final int[] sizes = { 0, 1, 16 * 1024 - 1, 16 * 1024, 16 * 1024 + 1, 300 * 1024 };

        for (final int size : sizes) {
            final File f = newFile("c649/plain-" + size + ".bin", randomBytes(size, size));
            final byte[] expected = Files.readAllBytes(f.toPath());

            assertArrayEquals(expected, IOUtil.readAllBytes(f), "size " + size);
        }
    }

    @Test
    public void testC649_textReadersInheritTheSizedRead() throws IOException {
        // Multi-byte characters straddling the 16 KB pooled-buffer boundary.
        final StringBuilder sb = new StringBuilder();

        while (sb.length() < 40_000) {
            sb.append("ab\u00e9\u65e5\ud83d\ude00-");
        }

        final String text = sb.toString();
        final File f = newFile("c649/text.txt", text);

        assertEquals(text, IOUtil.readAllToString(f));
        assertEquals(text, IOUtil.readAllToString(f, StandardCharsets.UTF_8));
        assertEquals(text, IOUtil.readAllToString(f, (Charset) null));
        assertEquals(text, IOUtil.readAllToString(f, "UTF-8"));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(f));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(f, StandardCharsets.UTF_8));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(f, null));

        // Empty file.
        final File empty = newFile("c649/empty.txt", "");
        assertEquals("", IOUtil.readAllToString(empty));
        assertEquals(0, IOUtil.readAllChars(empty).length);

        // BOM handling unchanged: kept for UTF-8, consumed for UTF-16.
        final File utf8Bom = newFile("c649/bom8.txt", new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'A' });
        assertEquals("\ufeffA", IOUtil.readAllToString(utf8Bom));
        assertArrayEquals("\ufeffA".toCharArray(), IOUtil.readAllChars(utf8Bom));

        final File utf16Bom = newFile("c649/bom16.txt", new byte[] { (byte) 0xFE, (byte) 0xFF, 0, 'A' });
        assertEquals("A", IOUtil.readAllToString(utf16Bom, StandardCharsets.UTF_16));
        assertArrayEquals("A".toCharArray(), IOUtil.readAllChars(utf16Bom, StandardCharsets.UTF_16));

        // Malformed input is replaced exactly as the streaming decoder of the InputStream twin replaces it.
        final byte[] malformed = { 'x', (byte) 0xFF, (byte) 0xC0, (byte) 0xAF, 'y', (byte) 0xE6 };
        final File bad = newFile("c649/bad.txt", malformed);
        final String viaStream;

        try (InputStream is = new FileInputStream(bad)) {
            viaStream = new String(IOUtil.readAllChars(is));
        }

        assertEquals(viaStream, IOUtil.readAllToString(bad));
        assertEquals(viaStream, new String(IOUtil.readAllChars(bad)));
    }

    @Test
    public void testC649_lengthIsOnlyAHint_grownAndShrunkFilesAreReadExactly() throws IOException {
        final byte[] big = randomBytes(300 * 1024 + 7, 649);
        final File real = newFile("c649/hint.bin", big);

        // The file "grew" after it was measured: a length far below the content is read on through the growth loop.
        final File underReported = new File(real.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long length() {
                return 10;
            }
        };

        assertArrayEquals(big, IOUtil.readAllBytes(underReported));

        // The file "shrank" after it was measured: a length far above the content is trimmed.
        final File overReported = new File(real.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long length() {
                return 5_000_000;
            }
        };

        assertArrayEquals(big, IOUtil.readAllBytes(overReported));
        assertEquals(new String(big, StandardCharsets.ISO_8859_1), IOUtil.readAllToString(overReported, StandardCharsets.ISO_8859_1));

        // Exact length, and a length of exactly the content plus one byte.
        final File exactPlusOne = new File(real.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long length() {
                return big.length + 1L;
            }
        };

        assertArrayEquals(big, IOUtil.readAllBytes(exactPlusOne));
    }

    @Test
    public void testC649_compressedFilesStillDecompress() throws IOException {
        final String text = alphabet(50_000) + "\u00e9";
        final File gz = new File(base(), "c649/data.txt.gz");
        Files.createDirectories(gz.getParentFile().toPath());

        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(gz.toPath()))) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }

        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), IOUtil.readAllBytes(gz));
        assertEquals(text, IOUtil.readAllToString(gz));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(gz));

        final File zip = new File(base(), "c649/data.zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            zos.putNextEntry(new ZipEntry("dir/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("dir/data.txt"));
            zos.write(text.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertEquals(text, IOUtil.readAllToString(zip));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(zip));
    }

    @Test
    public void testC649_validationUnchanged() throws IOException {
        final File dir = newDir("c649/dir");
        final File missing = new File(base(), "c649/missing.txt");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllChars(dir));
        assertTrue(assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> IOUtil.readAllBytes(missing))
                .getCause() instanceof FileNotFoundException);
        assertTrue(assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> IOUtil.readAllChars(missing))
                .getCause() instanceof FileNotFoundException);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-650: readLines(Reader, offset, count) with an offset beyond the end (documented claim)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC650_readerOffsetBeyondTheEndIsEmptyAndLeavesTheReaderAtEof() throws IOException {
        final Reader reader = new StringReader("a\nb\nc\n");

        assertEquals(Collections.emptyList(), IOUtil.readLines(reader, 5, 2));
        assertEquals(-1, reader.read());

        // count 0 leaves the reader untouched
        final Reader untouched = new StringReader("a\nb\n");
        assertEquals(Collections.emptyList(), IOUtil.readLines(untouched, 5, 0));
        assertEquals('a', untouched.read());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-652: getHostName failed-attempt, timeout and interrupt paths
    // ------------------------------------------------------------------------------------------------------------

    /** Runs {@code body} with the host-name state replaced by the given values and restores everything afterwards. */
    private static void withHostNameState(final Future<String> future, final long retryAt, final HostNameCheck body) throws Exception {
        final Field hostNameField = ioField("hostName");
        final Field lockField = ioField("hostNameLock");
        final Field futureField = ioField("hostNameFuture");
        final Field retryField = ioField("hostNameRetryAtMillis");
        final Field resolverField = ioField("hostNameResolver");
        final Object lock = lockField.get(null);

        final Object savedName;
        final Object savedFuture;
        final long savedRetry;
        final Object savedResolver;

        synchronized (lock) {
            savedName = hostNameField.get(null);
            savedFuture = futureField.get(null);
            savedRetry = retryField.getLong(null);
            savedResolver = resolverField.get(null);

            hostNameField.set(null, null);
            futureField.set(null, future);
            retryField.setLong(null, retryAt);
            // No live executor is handed to the paths under test: they release it, and a shut-down executor must
            // never be restored into the class.
            resolverField.set(null, null);
        }

        try {
            body.run(futureField, retryField, hostNameField, resolverField);
        } finally {
            synchronized (lock) {
                final Object leftover = resolverField.get(null);

                if (leftover != null && leftover != savedResolver) {
                    ((ExecutorService) leftover).shutdownNow();
                }

                hostNameField.set(null, savedName);
                futureField.set(null, savedFuture);
                retryField.setLong(null, savedRetry);
                resolverField.set(null, savedResolver);
            }
        }
    }

    @FunctionalInterface
    private interface HostNameCheck {
        void run(Field futureField, Field retryField, Field hostNameField, Field resolverField) throws Exception;
    }

    @Test
    public void testC652_failedAttemptAnswersUnknownArmsTheBackoffAndClearsTheFuture() throws Exception {
        final CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new UnknownHostException("C-652 failed lookup"));

        withHostNameState(failed, 0L, (futureField, retryField, hostNameField, resolverField) -> {
            final long before = System.currentTimeMillis();

            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());

            assertNull(futureField.get(null), "the failed attempt is cleared");
            assertTrue(retryField.getLong(null) >= before + 30_000L, "the retry backoff is armed");
            assertNull(hostNameField.get(null), "a failure is never cached");
            assertNull(resolverField.get(null));

            // During the backoff no new lookup is started and the answer stays UNKNOWN.
            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
            assertNull(futureField.get(null));
        });
    }

    @Test
    public void testC652_timeoutArmsTheBackoffAndKeepsTheAttemptInFlight() throws Exception {
        final AtomicInteger gets = new AtomicInteger();

        // A lookup that "hangs": get(timeout) answers TimeoutException at once, so the path costs no real 5 s wait.
        final Future<String> hanging = new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                throw new AssertionError("the attempt must not be cancelled");
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() {
                throw new AssertionError("only the timed get is used");
            }

            @Override
            public String get(final long timeout, final TimeUnit unit) throws TimeoutException {
                gets.incrementAndGet();
                assertEquals(5L, unit.toSeconds(timeout));
                throw new TimeoutException("C-652 simulated timeout");
            }
        };

        withHostNameState(hanging, 0L, (futureField, retryField, hostNameField, resolverField) -> {
            final long before = System.currentTimeMillis();

            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
            assertTrue(System.currentTimeMillis() - before < 1000L);

            assertEquals(1, gets.get());
            assertSame(hanging, futureField.get(null), "the attempt stays in flight for a later caller");
            assertTrue(retryField.getLong(null) >= before + 30_000L, "the retry backoff is armed");
            assertNull(resolverField.get(null), "the resolver executor is released");

            // Behind the backoff the hanging attempt is not awaited again.
            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
            assertEquals(1, gets.get());
        });
    }

    @Test
    public void testC652_interruptAnswersUnknownRestoresTheFlagAndKeepsTheAttempt() throws Exception {
        final CompletableFuture<String> pending = new CompletableFuture<>();

        withHostNameState(pending, 0L, (futureField, retryField, hostNameField, resolverField) -> {
            Thread.currentThread().interrupt();

            final String result;

            try {
                result = IOUtil.getHostName();
            } finally {
                // Always cleared here, so a failure cannot leak the flag into the next test.
                assertTrue(Thread.interrupted(), "the interrupt flag is restored");
            }

            assertEquals("UNKNOWN_HOST_NAME", result);
            assertSame(pending, futureField.get(null), "the attempt did not fail and is kept");
            assertEquals(0L, retryField.getLong(null), "an interrupt arms no backoff");
            assertNull(hostNameField.get(null));

            // The kept attempt is used by the next caller once it completes.
            pending.complete("host-C652");
            assertEquals("host-C652", IOUtil.getHostName());
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-653: one zero rule for the File-target (buffer-filling) and stream-target copiers
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC653_doubleZeroStraddlingTwoReadsEndsTheCopyForFileTargets() throws IOException {
        final File out = new File(base(), "c653/out.txt");

        // ["ab", "", "", "cd"] -> 2 on every path
        assertEquals(2, IOUtil.write(new ChunkScriptInputStream(2, 0, 0, 2), out));
        assertEquals("ab", read(out));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertEquals(2, IOUtil.write(new ChunkScriptInputStream(2, 0, 0, 2), bos));
        assertEquals("ab", bos.toString(StandardCharsets.UTF_8));

        assertEquals(2, IOUtil.write(new ChunkScriptReader(2, 0, 0, 2), out));
        assertEquals("ab", read(out));

        assertEquals(2, IOUtil.write(new ChunkScriptReader(2, 0, 0, 2), StandardCharsets.UTF_8, out));
        assertEquals("ab", read(out));

        final File appendTarget = newFile("c653/append.txt", "PRE");
        assertEquals(2, IOUtil.append(new ChunkScriptInputStream(2, 0, 0, 2), appendTarget));
        assertEquals("PREab", read(appendTarget));
        assertEquals(2, IOUtil.append(new ChunkScriptReader(2, 0, 0, 2), appendTarget));
        assertEquals("PREabab", read(appendTarget));

        final java.io.StringWriter sw = new java.io.StringWriter();
        assertEquals(2, IOUtil.write(new ChunkScriptReader(2, 0, 0, 2), sw));
        assertEquals("ab", sw.toString());
    }

    @Test
    public void testC653_singleZerosAreRetriedAndTheSecondConsecutiveOneEnds() throws IOException {
        final File out = new File(base(), "c653/out2.txt");

        // ["ab", "", "cd", "", "", "ef"] -> 4
        assertEquals(4, IOUtil.write(new ChunkScriptInputStream(2, 0, 2, 0, 0, 2), out));
        assertEquals("abcd", read(out));
        assertEquals(4, IOUtil.write(new ChunkScriptReader(2, 0, 2, 0, 0, 2), out));
        assertEquals("abcd", read(out));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertEquals(4, IOUtil.write(new ChunkScriptInputStream(2, 0, 2, 0, 0, 2), bos));

        // ["", "", "cd"] -> 0
        assertEquals(0, IOUtil.write(new ChunkScriptInputStream(0, 0, 2), out));
        assertEquals("", read(out));
        assertEquals(0, IOUtil.write(new ChunkScriptReader(0, 0, 2), out));
        assertEquals(0, IOUtil.write(new ChunkScriptInputStream(0, 0, 2), new ByteArrayOutputStream()));

        // ["", "cd"] -> 2: a single leading zero is retried
        assertEquals(2, IOUtil.write(new ChunkScriptInputStream(0, 2), out));
        assertEquals("ab", read(out));
        assertEquals(2, IOUtil.write(new ChunkScriptReader(0, 2), out));

        // an always-zero source ends (does not spin) on both paths
        final int[] zeros = new int[1000];
        assertEquals(0, IOUtil.write(new ChunkScriptInputStream(zeros), out));
        assertEquals(0, IOUtil.write(new ChunkScriptReader(zeros), out));
        assertEquals(0, IOUtil.write(new ChunkScriptInputStream(zeros), new ByteArrayOutputStream()));

        // empty source
        assertEquals(0, IOUtil.write(new ChunkScriptInputStream(), out));
        assertTrue(out.exists());
        assertEquals(0, out.length());
    }

    @Test
    public void testC653_largeChunksAcrossTheBufferBoundaryAgreeOnBothPaths() throws IOException {
        final File out = new File(base(), "c653/large.txt");

        final int[][] scripts = { { 20_000, 0, 5 }, { 20_000, 0, 0, 5 }, { 8192, 0, 8192, 0, 0, 1 }, { 8191, 0, 1, 0, 8192 }, { 100_000 } };
        final int[] expected = { 20_005, 20_000, 16_384, 16_384, 100_000 };

        for (int i = 0; i < scripts.length; i++) {
            assertEquals(expected[i], IOUtil.write(new ChunkScriptInputStream(scripts[i]), out), "file bytes " + i);
            assertEquals(alphabet(expected[i]), read(out), "file content " + i);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            assertEquals(expected[i], IOUtil.write(new ChunkScriptInputStream(scripts[i]), bos), "stream bytes " + i);

            assertEquals(expected[i], IOUtil.write(new ChunkScriptReader(scripts[i]), out), "reader file chars " + i);
            assertEquals(alphabet(expected[i]), read(out), "reader content " + i);

            final java.io.StringWriter sw = new java.io.StringWriter();
            assertEquals(expected[i], IOUtil.write(new ChunkScriptReader(scripts[i]), sw), "writer chars " + i);
        }

        // offset/count slices still honoured on the File path
        assertEquals(5, IOUtil.write(new ChunkScriptInputStream(20_000), 3, 5, out));
        assertEquals(alphabet(8).substring(3), read(out));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-654 / C-655 / C-659 / C-661: a decode-only charset is refused by every encode-first writer, payload or not
    // ------------------------------------------------------------------------------------------------------------

    private interface FileWrite {
        void apply(File f) throws IOException;
    }

    private void assertDecodeOnlyRefused(final String label, final FileWrite write) throws IOException {
        final File existing = newFile("c654/" + label + "-existing.txt", "KEEP ME");
        final UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> write.apply(existing), label);
        assertTrue(e.getMessage() != null && e.getMessage().contains("ISO-2022-CN"), label + " -> " + e.getMessage());
        assertEquals("KEEP ME", read(existing), label + ": existing file kept");

        final File missing = new File(base(), "c654/" + label + "-missing/sub/f.txt");
        assertThrows(UnsupportedOperationException.class, () -> write.apply(missing), label);
        assertFalse(missing.exists(), label + ": missing file not created");
        assertFalse(missing.getParentFile().getParentFile().exists(), label + ": no parent directory created");
    }

    @Test
    public void testC654_fileWritersRefuseADecodeOnlyCharsetWhateverThePayload() throws IOException {
        assertDecodeOnlyRefused("writeCsNull", f -> IOUtil.write((CharSequence) null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCsEmpty", f -> IOUtil.write("", DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCsText", f -> IOUtil.write("x", DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCsNull", f -> IOUtil.append((CharSequence) null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCsEmpty", f -> IOUtil.append("", DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCsText", f -> IOUtil.append("x", DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCharsNull", f -> IOUtil.write((char[]) null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCharsEmpty", f -> IOUtil.write(new char[0], DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCharsSliceEmpty", f -> IOUtil.write(new char[] { 'a' }, 1, 0, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCharsSliceNull", f -> IOUtil.write((char[]) null, 0, 0, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCharsNull", f -> IOUtil.append((char[]) null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCharsEmpty", f -> IOUtil.append(new char[0], DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCharsSliceEmpty", f -> IOUtil.append(new char[] { 'a' }, 0, 0, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendCharsText", f -> IOUtil.append(new char[] { 'a' }, 0, 1, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLineNull", f -> IOUtil.writeLine(null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLineText", f -> IOUtil.writeLine("x", DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendLineNull", f -> IOUtil.appendLine(null, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendLineText", f -> IOUtil.appendLine("x", DECODE_ONLY, f));
    }

    @Test
    public void testC654_argumentChecksStillComeFirst() throws IOException {
        // A null target is the bad argument, reported before the charset.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", DECODE_ONLY, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("x", DECODE_ONLY, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", DECODE_ONLY, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine("x", DECODE_ONLY, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[0], DECODE_ONLY, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", DECODE_ONLY, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newOutputStreamWriter(null, DECODE_ONLY));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((OutputStream) null, DECODE_ONLY));

        // A bad slice is still reported before the charset (C-053 order).
        final File f = new File(base(), "c654/slice.txt");
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[1], 0, 2, DECODE_ONLY, f));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[1], -1, 0, DECODE_ONLY, f));
        assertFalse(f.exists());
    }

    @Test
    public void testC655_streamWritersRefuseADecodeOnlyCharsetWithAMessageAndWriteNothing() {
        final AtomicInteger flushes = new AtomicInteger();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream() {
            @Override
            public void flush() {
                flushes.incrementAndGet();
            }
        };

        final List<org.junit.jupiter.api.function.Executable> calls = Arrays.asList( //
                () -> IOUtil.write("", DECODE_ONLY, bos), //
                () -> IOUtil.write((CharSequence) null, DECODE_ONLY, bos), //
                () -> IOUtil.write("x", DECODE_ONLY, bos, true), //
                () -> IOUtil.write(new char[0], DECODE_ONLY, bos), //
                () -> IOUtil.write((char[]) null, DECODE_ONLY, bos), //
                () -> IOUtil.write(new char[] { 'a' }, DECODE_ONLY, bos), //
                () -> IOUtil.write(new char[] { 'a' }, 0, 0, DECODE_ONLY, bos), //
                () -> IOUtil.write(new char[] { 'a' }, 0, 1, DECODE_ONLY, bos), //
                () -> IOUtil.write(new char[] { 'a' }, 0, 0, DECODE_ONLY, bos, true), //
                () -> IOUtil.write(new char[] { 'a' }, 0, 1, DECODE_ONLY, bos, true), //
                () -> IOUtil.newOutputStreamWriter(bos, DECODE_ONLY), //
                () -> IOUtil.newBufferedWriter(bos, DECODE_ONLY));

        for (int i = 0; i < calls.size(); i++) {
            final UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, calls.get(i), "call " + i);
            assertTrue(e.getMessage() != null && e.getMessage().contains("ISO-2022-CN"), "call " + i + " -> " + e.getMessage());
        }

        assertEquals(0, bos.size(), "nothing written");
        assertEquals(0, flushes.get(), "nothing flushed");
    }

    @Test
    public void testC654_encodingCharsetsBehaveAsBefore() throws IOException {
        final File f = newFile("c654/ok.txt", "OLD");

        IOUtil.write("h\u00e9llo", StandardCharsets.UTF_8, f);
        assertEquals("h\u00e9llo", read(f));

        IOUtil.write((CharSequence) null, StandardCharsets.UTF_8, f);
        assertEquals("", read(f));

        IOUtil.append((CharSequence) null, StandardCharsets.UTF_8, f);
        IOUtil.append("", (Charset) null, f);
        assertEquals("", read(f));

        IOUtil.append("\u65e5", StandardCharsets.UTF_8, f);
        IOUtil.append(new char[0], StandardCharsets.UTF_8, f);
        IOUtil.append(new char[] { 'x', 'y' }, 1, 1, null, f);
        IOUtil.appendLine("z", StandardCharsets.UTF_8, f);
        assertEquals("\u65e5yz\n", read(f));

        IOUtil.write(new char[] { '\u00e9' }, StandardCharsets.ISO_8859_1, f);
        assertArrayEquals(new byte[] { (byte) 0xE9 }, Files.readAllBytes(f.toPath()));

        IOUtil.writeLine(null, null, f);
        assertEquals("null\n", read(f));

        final File created = new File(base(), "c654/created/f.txt");
        IOUtil.append((CharSequence) null, StandardCharsets.UTF_8, created);
        assertTrue(created.isFile());
        assertEquals(0, created.length());

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtil.write((CharSequence) null, StandardCharsets.UTF_8, bos);
        IOUtil.write(new char[0], StandardCharsets.UTF_8, bos);
        IOUtil.write(new char[] { 'a', 'b' }, 1, 1, StandardCharsets.UTF_8, bos, true);
        assertEquals("nullb", bos.toString(StandardCharsets.UTF_8));

        final java.io.Writer w = IOUtil.newOutputStreamWriter(bos, null);
        w.write("!");
        w.flush();
        assertEquals("nullb!", bos.toString(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-656: transfer proves the source readable before the "nothing to move" answer
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC656_appendModeSourceIsNonReadable() throws IOException {
        final File src = newFile("c656/src.bin", randomBytes(5000, 656));
        final File dst = new File(base(), "c656/dst.bin");

        try (FileChannel in = FileChannel.open(src.toPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
             FileChannel out = FileChannel.open(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            assertEquals(in.size(), in.position(), "append mode reports position == size");
            assertThrows(NonReadableChannelException.class, () -> IOUtil.transfer(in, out));
            assertEquals(0, out.size(), "nothing moved");
        }

        assertEquals(5000, src.length(), "source untouched");

        // A READ source already at its end still answers 0.
        try (FileChannel in = FileChannel.open(src.toPath(), StandardOpenOption.READ);
             FileChannel out = FileChannel.open(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            in.position(in.size());
            assertEquals(0, IOUtil.transfer(in, out));
            assertEquals(0, out.size());
        }

        // And a normal transfer is unchanged.
        try (FileChannel in = FileChannel.open(src.toPath(), StandardOpenOption.READ);
             FileChannel out = FileChannel.open(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            in.position(1000);
            assertEquals(4000, IOUtil.transfer(in, out));
        }

        assertArrayEquals(Arrays.copyOfRange(Files.readAllBytes(src.toPath()), 1000, 5000), Files.readAllBytes(dst.toPath()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-657: getNameWithoutExtension(String) strips trailing separators as getFileExtension(String) does
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC657_nameWithoutExtensionDropsTrailingSeparators() {
        assertEquals("a/b", IOUtil.getNameWithoutExtension("a/b.c/"));
        assertEquals("x.tar", IOUtil.getNameWithoutExtension("x.tar.gz/"));
        assertEquals("x.tar", IOUtil.getNameWithoutExtension("x.tar.gz\\\\"));
        assertEquals("a", IOUtil.getNameWithoutExtension("a./"));
        assertEquals("", IOUtil.getNameWithoutExtension("/"));
        assertEquals("", IOUtil.getNameWithoutExtension("\\/\\"));
        assertEquals("dir\\name", IOUtil.getNameWithoutExtension("dir\\name.txt\\"));
        assertEquals("d\u00e9j\u00e0/\u65e5\u672c", IOUtil.getNameWithoutExtension("d\u00e9j\u00e0/\u65e5\u672c.\u00e9/"));

        // unchanged without a trailing separator
        assertEquals("foo", IOUtil.getNameWithoutExtension("foo.txt"));
        assertEquals("a.b\\c", IOUtil.getNameWithoutExtension("a.b\\c"));
        assertEquals("", IOUtil.getNameWithoutExtension(""));
        assertEquals("", IOUtil.getNameWithoutExtension(".hidden"));
        assertNull(IOUtil.getNameWithoutExtension((String) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.getNameWithoutExtension("a\u0000b.c/"));

        // The pair reconstructs the separator-stripped input: the extension one reports is the one the other removes.
        final String[] inputs = { "a/b.c/", "x.tar.gz/", "x.tar.gz", "a./", "a.", "dir/", "dir", "/", "", "a.b/c.d//", "c:\\x.y\\", ".hidden/",
                "\u65e5\u672c.\u00e9\\", "no-ext", "a.b\\c" };

        for (final String input : inputs) {
            String stripped = input;

            while (!stripped.isEmpty() && (stripped.endsWith("/") || stripped.endsWith("\\"))) {
                stripped = stripped.substring(0, stripped.length() - 1);
            }

            final String name = IOUtil.getNameWithoutExtension(input);
            final String ext = IOUtil.getFileExtension(input);
            final String rebuilt = name.length() < stripped.length() ? name + "." + ext : name;

            assertEquals(stripped, rebuilt, "input '" + input + "'");
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-658: skip(InputStream) does not probe the seek on every chunk (C-692: at most twice per call)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC658_seekIsNotReprobedOnEveryChunk() throws IOException {
        final byte[] data = randomBytes(100_000, 658);
        final File f = newFile("c658/data.bin", data);
        final AtomicInteger channelProbes = new AtomicInteger();

        // available() lies, so the seek is refused (as on a Windows pipe) and the skip reads - ~7 chunks of 8 KB.
        try (FileInputStream in = new FileInputStream(f) {
            @Override
            public int available() {
                return 0;
            }

            @Override
            public FileChannel getChannel() {
                channelProbes.incrementAndGet();
                return super.getChannel();
            }
        }) {
            assertEquals(50_000, IOUtil.skip(in, 50_000));
            // C-692 (R2-05): one re-probe after the first chunk (a transient refusal must not doom the whole skip),
            // then none - not one per chunk
            assertEquals(2, channelProbes.get(), "two seek probes for the whole call");
            assertEquals(data[50_000] & 0xFF, in.read());

            // A skip past the end reads to EOF and reports the true count.
            assertEquals(100_000 - 50_001, IOUtil.skip(in, 1_000_000));
            assertEquals(-1, in.read());
        }

        // A regular file still seeks (no read loop): skip lands exactly.
        try (FileInputStream in = new FileInputStream(f)) {
            assertEquals(99_999, IOUtil.skip(in, 99_999));
            assertEquals(data[99_999] & 0xFF, in.read());
            assertEquals(0, IOUtil.skip(in, 10));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-660: append(File, [long, long,] File) reports a missing source as FileNotFoundException
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC660_appendOfAMissingSourceIsFileNotFound() throws IOException {
        final File missing = new File(base(), "c660/missing.bin");
        final File target = new File(base(), "c660/new/target.bin");

        assertThrows(FileNotFoundException.class, () -> IOUtil.append(missing, target));
        assertThrows(FileNotFoundException.class, () -> IOUtil.append(missing, 0, 10, target));
        assertFalse(target.exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-665: copyToDirectory refuses a source that resolves to no name
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC665_trailingDotsSpellingIsRefusedOnWindows() throws IOException {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "Windows folds trailing dots away");

        final File parent = newDir("c665/parent");
        final File child = newDir("c665/parent/child");
        newFile("c665/parent/child/in.txt", "in");
        final File dest = new File(base(), "c665/dest");
        final File source = new File(child, "...");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(source, dest));
        assertTrue(e.getMessage().contains("has no name to be copied under"), e.getMessage());
        assertFalse(dest.exists(), "nothing created");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(source, dest, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(source, dest, false, (p, f) -> true));
        assertFalse(dest.exists(), "nothing created");

        // Into an existing destination: nothing is merged into it.
        final File existingDest = newDir("c665/existingDest");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(source, existingDest));
        assertEquals(0, existingDest.list().length);
        assertTrue(new File(child, "in.txt").isFile(), "source untouched");
        assertTrue(parent.isDirectory());

        // The plain spelling still copies under the child's own name.
        assertEquals(new File(existingDest.getCanonicalFile(), "child"), IOUtil.copyToDirectory(child, existingDest));
        assertEquals("in", read(new File(existingDest, "child/in.txt")));
    }

    @Test
    public void testC665_filesystemRootIsRefusedBeforeAnythingIsCopied() throws IOException {
        final File[] roots = File.listRoots();
        assumeTrue(roots != null && roots.length > 0, "no filesystem root to test with");

        final File root = roots[0];
        final File dest = new File(base(), "c665/rootDest");
        final AtomicBoolean walked = new AtomicBoolean();

        // The filter would be asked about the first entry before anything is copied: if the refusal ever regresses,
        // the walk fails at once instead of copying a whole volume.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(root, dest, false, (p, f) -> {
            walked.set(true);
            throw new IllegalStateException("C-665: the root was walked");
        }));

        assertTrue(e.getMessage().contains("has no name to be copied under"), e.getMessage());
        assertFalse(walked.get());
        assertFalse(dest.exists(), "nothing created");
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-666: a "."/".."-terminated file target is refused before any parent directory is created
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC666_dotTerminatedFileTargetsLeaveNoDirectoryBehind() throws IOException {
        final File src = newFile("c666/src.txt", "payload");
        final URL url = src.toURI().toURL();
        final File b = new File(base(), "c666");

        final List<String> names = new ArrayList<>();
        final List<org.junit.jupiter.api.function.Executable> calls = new ArrayList<>();

        names.add("n1");
        calls.add(() -> IOUtil.copyURLToFile(url, new File(b, "n1/.")));
        names.add("n2");
        calls.add(() -> IOUtil.copyURLToFile(url, new File(b, "n2/q/."), 1000, 1000));
        names.add("n3");
        calls.add(() -> IOUtil.write("x", new File(b, "n3/.")));
        names.add("n4");
        calls.add(() -> IOUtil.write("x".getBytes(StandardCharsets.UTF_8), new File(b, "n4/sub/..")));
        names.add("n5");
        calls.add(() -> IOUtil.writeLines(Arrays.asList("a", "b"), new File(b, "n5/.")));
        names.add("n6");
        calls.add(() -> IOUtil.append("x", new File(b, "n6/.")));
        names.add("n7");
        calls.add(() -> IOUtil.newFileOutputStream(new File(b, "n7/.")).close());
        names.add("n8");
        calls.add(() -> IOUtil.newFileWriter(new File(b, "n8/."), StandardCharsets.UTF_8, false).close());
        names.add("n9");
        calls.add(() -> IOUtil.write(src, new File(b, "n9/.")));
        names.add("n10");
        calls.add(() -> IOUtil.writeLine("x", new File(b, "n10/.")));
        names.add("n11");
        calls.add(() -> IOUtil.write(new StringReader("x"), new File(b, "n11/.")));

        for (int i = 0; i < calls.size(); i++) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, calls.get(i), names.get(i));
            assertTrue(e.getMessage().contains("is a directory, not a file"), names.get(i) + " -> " + e.getMessage());
            assertFalse(new File(b, names.get(i)).exists(), names.get(i) + " left a directory behind");
        }

        // An existing directory spelled "dir/." is refused as before, and left intact.
        final File dir = newDir("c666/existing");
        newFile("c666/existing/keep.txt", "keep");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", new File(dir, ".")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(url, new File(dir, ".")));
        assertEquals("keep", read(new File(dir, "keep.txt")));

        // A normal target still works, parents created.
        final File ok = new File(b, "ok/deep/f.txt");
        IOUtil.copyURLToFile(url, ok);
        assertEquals("payload", read(ok));
        IOUtil.write("y", ok);
        assertEquals("y", read(ok));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-668: the doCopyFile post-check message names the condition (POSIX only: Windows refuses the truncate)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC668_shrinkingSourceIsReportedAsShrunk() throws Exception {
        assumeFalse(IOUtil.IS_OS_WINDOWS, "Windows refuses to truncate a file with a mapped section open");

        final File src = newFile("c668/big.bin", randomBytes(64 * 1024 * 1024, 668));

        for (int attempt = 0; attempt < 5; attempt++) {
            final File destDir = new File(base(), "c668/dest" + attempt);
            final File copy = new File(destDir, src.getName());
            final AtomicBoolean truncated = new AtomicBoolean();

            final Thread truncator = new Thread(() -> {
                final long deadline = System.currentTimeMillis() + 10_000;

                while (System.currentTimeMillis() < deadline) {
                    if (copy.length() > 0) {
                        try (RandomAccessFile raf = new RandomAccessFile(src, "rw")) {
                            raf.setLength(1);
                            truncated.set(true);
                        } catch (final IOException e) {
                            // leave it
                        }

                        return;
                    }

                    Thread.onSpinWait();
                }
            });

            truncator.start();

            IOException failure = null;

            try {
                IOUtil.copyToDirectory(src, destDir);
            } catch (final IOException e) {
                failure = e;
            }

            truncator.join(20_000);

            if (failure != null) {
                assertTrue(failure.getMessage().contains("the source shrank to"), failure.getMessage());
                assertFalse(copy.exists(), "the partial copy is removed");
                return;
            }

            // The copy finished before the truncation: restore the source and try again.
            if (truncated.get()) {
                Files.write(src.toPath(), randomBytes(64 * 1024 * 1024, 668));
            }
        }

        org.junit.jupiter.api.Assumptions.abort("the truncation never landed inside the copy window");
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-672: zip/merge report a directory target right after the null checks
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC672_directoryTargetIsReportedBeforeTheSources() throws IOException {
        final File missing = new File(base(), "c672/missing.txt");
        final File dirTarget = newDir("c672/target-dir");
        final File cafeDir = newDir("c672/cafe");
        newFile("c672/cafe/caf\u00e9.txt", "x");
        final File ok = newFile("c672/ok.txt", "ok");

        assertTrue(assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(missing, dirTarget)).getMessage().contains("is a directory"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(missing, dirTarget, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Collections.singletonList(missing), dirTarget));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Collections.singletonList(missing), dirTarget, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Collections.singletonList(missing), dirTarget));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Collections.singletonList(missing), new byte[] { '\n' }, dirTarget));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(new File[] { missing }, dirTarget));

        // an unencodable name no longer masks it
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(cafeDir, dirTarget, StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Collections.singletonList(cafeDir), dirTarget, StandardCharsets.US_ASCII));

        // nor does a decode-only charset
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(ok, dirTarget, DECODE_ONLY));

        // a "."-terminated target is refused up front and creates nothing
        final File dotTarget = new File(base(), "c672/newdir/.");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(ok, dotTarget));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Collections.singletonList(ok), dotTarget));
        assertFalse(new File(base(), "c672/newdir").exists());

        assertEquals(0, dirTarget.list().length, "directory target untouched");

        // a valid target still works
        final File archive = new File(base(), "c672/out.zip");
        IOUtil.zip(ok, archive);
        assertTrue(archive.length() > 0);
        final File merged = new File(base(), "c672/merged.txt");
        assertEquals(4, IOUtil.merge(Arrays.asList(ok, ok), merged));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-673: unzip names the entry that lies below an existing file
    // ------------------------------------------------------------------------------------------------------------

    private File writeZip(final String name, final String... entries) throws IOException {
        final File zip = new File(base(), name);
        Files.createDirectories(zip.getParentFile().toPath());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (final String entry : entries) {
                zos.putNextEntry(new ZipEntry(entry));

                if (!entry.endsWith("/")) {
                    zos.write(entry.getBytes(StandardCharsets.UTF_8));
                }

                zos.closeEntry();
            }
        }

        return zip;
    }

    @Test
    public void testC673_fileEntryBelowAFileNamesTheEntry() throws IOException {
        final File zip1 = writeZip("c673/a.zip", "f", "f/x.txt");
        final IOException e1 = assertThrows(IOException.class, () -> IOUtil.unzip(zip1, new File(base(), "c673/out1")));
        assertTrue(e1.getMessage().contains("'f/x.txt'") && e1.getMessage().contains("below a file"), e1.getMessage());

        final File zip2 = writeZip("c673/b.zip", "f", "f/d/x.txt");
        final IOException e2 = assertThrows(IOException.class, () -> IOUtil.unzip(zip2, new File(base(), "c673/out2")));
        assertTrue(e2.getMessage().contains("'f/d/x.txt'") && e2.getMessage().contains("below a file"), e2.getMessage());
        assertFalse(new File(base(), "c673/out2/f/d").exists());

        final File zip3 = writeZip("c673/c.zip", "\u00e9/", "\u00e9/g", "\u00e9/g/\u65e5.txt");
        final IOException e3 = assertThrows(IOException.class, () -> IOUtil.unzip(zip3, new File(base(), "c673/out3")));
        assertTrue(e3.getMessage().contains("\u00e9/g/\u65e5.txt"), e3.getMessage());

        // well-formed nesting still extracts
        final File ok = writeZip("c673/ok.zip", "d/", "d/e/x.txt", "d/y.txt");
        final File out = new File(base(), "c673/ok");
        IOUtil.unzip(ok, out);
        assertEquals("d/e/x.txt", read(new File(out, "d/e/x.txt")));
        assertEquals("d/y.txt", read(new File(out, "d/y.txt")));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-674: a null element of zip(Collection)/merge is named after 'sourceFiles'
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC674_nullElementNamesTheCollection() throws IOException {
        final File f = newFile("c674/f.txt", "f");
        final File target = new File(base(), "c674/out.zip");
        final File merged = new File(base(), "c674/merged.txt");

        final List<org.junit.jupiter.api.function.Executable> calls = Arrays.asList( //
                () -> IOUtil.zip(Arrays.asList(f, null), target), //
                () -> IOUtil.zip(Arrays.asList(null, f), target, StandardCharsets.UTF_8), //
                () -> IOUtil.merge(Arrays.asList(f, null), merged), //
                () -> IOUtil.merge(Arrays.asList(f, null), new byte[] { ',' }, merged), //
                () -> IOUtil.merge(new File[] { f, null }, merged));

        for (int i = 0; i < calls.size(); i++) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, calls.get(i), "call " + i);
            assertEquals("'sourceFiles' cannot hold a null element", e.getMessage(), "call " + i);
        }

        assertFalse(target.exists());
        assertFalse(merged.exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-675: the write-path unencodable-name failure names the entry
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC675_writePathUnencodableNameNamesTheEntry() throws IOException {
        final File dir = newDir("c675/cafe");
        newFile("c675/cafe/caf\u00e9.txt", "x");
        final File target = new File(base(), "c675/new.zip");

        final IOException e = assertThrows(IOException.class, () -> IOUtil.zip(dir, target, StandardCharsets.US_ASCII));
        assertTrue(e.getMessage().contains("'cafe/caf\u00e9.txt'"), e.getMessage());
        assertTrue(e.getMessage().contains("US-ASCII"), e.getMessage());
        assertTrue(e.getCause() instanceof IllegalArgumentException, String.valueOf(e.getCause()));

        // the pre-check path (existing target) keeps its wording
        final File existing = newFile("c675/existing.zip", "OLD");
        final IOException e2 = assertThrows(IOException.class, () -> IOUtil.zip(dir, existing, StandardCharsets.US_ASCII));
        assertTrue(e2.getMessage().contains("'cafe/caf\u00e9.txt'"), e2.getMessage());
        assertEquals("OLD", read(existing));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-676: isDirectory/isRegularFile(File, LinkOption...) reject null options as IllegalArgumentException
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC676_nullLinkOptionsAreIllegalArguments() throws IOException {
        final File f = newFile("c676/f.txt", "f");
        final File d = newDir("c676/d");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.isDirectory(d, (LinkOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isDirectory(d, new LinkOption[] { null }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isDirectory(d, LinkOption.NOFOLLOW_LINKS, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isDirectory(null, (LinkOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isRegularFile(f, (LinkOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isRegularFile(f, new LinkOption[] { null }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isRegularFile(null, (LinkOption[]) null));

        assertTrue(IOUtil.isDirectory(d));
        assertTrue(IOUtil.isDirectory(d, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isDirectory(f, new LinkOption[0]));
        assertFalse(IOUtil.isDirectory(null, LinkOption.NOFOLLOW_LINKS));
        assertTrue(IOUtil.isRegularFile(f));
        assertTrue(IOUtil.isRegularFile(f, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isRegularFile(d, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isRegularFile(null));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-677: splitByLine of a source named exactly ".gz"/".zip"
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC677_bareCompressedSuffixKeepsTheWholeName() throws IOException {
        final String text = "l1\nl2\nl3\nl4\n";

        final File gzDir = newDir("c677/gz");
        final File gz = new File(gzDir, ".gz");

        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(gz.toPath()))) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }

        final File gzOut = newDir("c677/gzOut");
        IOUtil.splitByLine(gz, 2, gzOut);
        final String[] gzParts = gzOut.list();
        Arrays.sort(gzParts);
        assertArrayEquals(new String[] { ".gz_0001", ".gz_0002" }, gzParts);
        assertEquals(text, read(new File(gzOut, ".gz_0001")) + read(new File(gzOut, ".gz_0002")));

        final File zipDir = newDir("c677/zip");
        final File zip = new File(zipDir, ".ZIP");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            zos.putNextEntry(new ZipEntry("t.txt"));
            zos.write(text.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File zipOut = newDir("c677/zipOut");
        IOUtil.splitByLine(zip, 2, zipOut);
        final String[] zipParts = zipOut.list();
        Arrays.sort(zipParts);
        assertArrayEquals(new String[] { ".ZIP_0001", ".ZIP_0002" }, zipParts);

        // unchanged: a real stem loses the suffix
        final File named = new File(newDir("c677/named"), "app.log.gz");

        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(named.toPath()))) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }

        final File namedOut = newDir("c677/namedOut");
        IOUtil.splitByLine(named, 2, namedOut);
        final String[] namedParts = namedOut.list();
        Arrays.sort(namedParts);
        assertArrayEquals(new String[] { "app_0001.log", "app_0002.log" }, namedParts);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-678: the reserved-name refusal is worded as a portability policy
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC678_reservedNameMessageIsAPolicy() throws IOException {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "the policy applies on Windows only");

        final File zip = writeZip("c678/r.zip", "COM1.txt");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, new File(base(), "c678/out")));
        assertTrue(e.getMessage().contains("reserved device name") && e.getMessage().contains("policy"), e.getMessage());
        assertTrue(e.getMessage().contains("'COM1.txt'"), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-680 / C-681 / C-683: toFile(URL)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC680_fourSlashShareLessUncRootsAreRejectedOnWindows() throws Exception {
        final String[] shareLess = { "file:////server", "file:////server/", "file:////", "file://///", "file://localhost//server" };

        if (IOUtil.IS_OS_WINDOWS) {
            for (final String spelling : shareLess) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
                assertTrue(e.getMessage().contains("must also name a share"), spelling + " -> " + e.getMessage());
            }

            // a UNC path with a share still converts, and round-trips through toUrl
            final File unc = IOUtil.toFile(new URL("file:////server/share/f.txt"));
            assertEquals("\\\\server\\share\\f.txt", unc.getPath());
            assertEquals(new File("\\\\server\\share"), IOUtil.toFile(new URL("file:////server/share")));
            assertEquals(unc, IOUtil.toFile(IOUtil.toUrl(unc)));
            assertEquals("file:////server", IOUtil.toUrl(new File("\\\\server")).toString());
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(IOUtil.toUrl(new File("\\\\server"))));
        } else {
            // documented Unix reading: "//server" is the local "/server"
            assertEquals(new File("/server"), IOUtil.toFile(new URL("file:////server")));
        }
    }

    @Test
    public void testC681_bareDriveRuleIsWindowsOnlyAndCoversDriveRelativePaths() throws Exception {
        if (IOUtil.IS_OS_WINDOWS) {
            for (final String spelling : new String[] { "file:///C:", "file://C:", "file:///c:", "file:///C:x", "file:///C:x/y" }) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
                assertTrue(e.getMessage().contains("must also name an absolute path"), spelling + " -> " + e.getMessage());
            }
        } else {
            // "/C:" is an ordinary name off Windows, and all three spellings agree
            assertEquals(new File("/C:"), IOUtil.toFile(new URL("file:///C:")));
            assertEquals(new File("/C:"), IOUtil.toFile(new URL("file://C:")));
            assertEquals(new File("/C:x"), IOUtil.toFile(new URL("file:///C:x")));
        }

        // drive roots and paths below them are fine everywhere
        assertEquals(new File("/C:/"), IOUtil.toFile(new URL("file:///C:/")));
        assertEquals(new File("/C:/x"), IOUtil.toFile(new URL("file:///C:/x")));
        assertEquals(new File("/C:/x"), IOUtil.toFile(new URL("file://C:/x")));
        // a name that merely starts with a letter and a colon further in is not a drive
        assertEquals(new File("/Cx:"), IOUtil.toFile(new URL("file:///Cx:")));
    }

    @Test
    public void testC683_fragmentNeverReachesThePath() throws Exception {
        // Every URL constructor splits the ref off; the result is the same file with or without it.
        assertEquals(new File("/tmp/a.txt"), IOUtil.toFile(new URL("file:///tmp/a.txt#frag")));
        assertEquals(new File("/tmp/a.txt"), IOUtil.toFile(new URL("file", "", "/tmp/a.txt#frag")));
        assertEquals(new File("/tmp/a.txt"), IOUtil.toFile(new URL("file", "", -1, "/tmp/a.txt#frag")));
        // an encoded '#' is a literal character of the name
        assertEquals(new File("/tmp/a#b.txt"), IOUtil.toFile(new URL("file:///tmp/a%23b.txt")));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-682 (ruling: keep the %5C rejection everywhere): the documented Unix round-trip gap
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC682_backslashNameDoesNotRoundTripOnUnix() throws IOException {
        assumeFalse(IOUtil.IS_OS_WINDOWS, "on Windows a backslash is a separator and the round trip holds");

        final File withBackslash = new File(base(), "a\\b");
        final URL url = IOUtil.toUrl(withBackslash);
        assertTrue(url.toString().contains("%5C"), url.toString());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(url));
        assertTrue(e.getMessage().contains("percent-encoded path separator"), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-662: the trailing-".." guard decides on the last element that is not a self-reference
    // ------------------------------------------------------------------------------------------------------------

    private static boolean entry(final File f) {
        return Files.exists(f.toPath(), LinkOption.NOFOLLOW_LINKS);
    }

    @Test
    public void testC662_dotDotFollowedByDotsIsRejectedAndNothingIsTouched() throws IOException {
        final List<String> spellings = new ArrayList<>(Arrays.asList("../.", ".././", "./../.", "../../.", "../././.", "x/../.", "x/.././."));

        if (IOUtil.IS_OS_WINDOWS) {
            spellings.addAll(Arrays.asList("..\\.\\", "../.\\.", "..\\..\\.", "../...", "../. .", "..\\... "));
        }

        int n = 0;

        for (final String spelling : spellings) {
            final String c = "c662/s" + (n++);
            final File child = newDir(c + "/d1/d2/d3/parent/child");
            final File parent = child.getParentFile();
            final File grandParent = parent.getParentFile();
            final File sibling = newFile(c + "/d1/d2/d3/parent/sibling.txt", "s");
            final File inChild = newFile(c + "/d1/d2/d3/parent/child/in.txt", "i");
            final File dest = newDir(c + "/dest");
            final File target = new File(child, spelling);

            assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteRecursivelyIfExists(target), spelling);
            assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(target), spelling);
            assertFalse(IOUtil.deleteQuietly(target), spelling);
            assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(target, dest), spelling);
            assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(target, dest, java.nio.file.StandardCopyOption.ATOMIC_MOVE),
                    spelling);
            assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.moveToDirectory(target, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING), spelling);

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(target), spelling);
            assertTrue(e.getMessage().contains("must not end in '..'"), e.getMessage());

            assertTrue(child.isDirectory() && parent.isDirectory() && grandParent.isDirectory(), spelling + ": fixture intact");
            assertEquals("s", read(sibling), spelling);
            assertEquals("i", read(inChild), spelling);
            assertEquals(0, dest.list().length, spelling + ": nothing moved");
        }

        // an EMPTY directory spelled through a missing name is not deleted either
        final File empty = newDir("c662/empty/d1/d2/d3/empty");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(new File(empty, "nothere/../.")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteRecursivelyIfExists(new File(empty, "nothere/.././")));

        if (IOUtil.IS_OS_WINDOWS) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(new File(empty, "nothere/../...")));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(new File(empty, "nothere/../. .")));
        }

        assertTrue(empty.isDirectory());
    }

    @Test
    public void testC662_selfReferencesAfterARealNameStillFold() throws IOException {
        final File child = newDir("c662f/d1/d2/d3/p/child");
        newFile("c662f/d1/d2/d3/p/child/in.txt", "i");
        final File dest = newDir("c662f/d1/d2/d3/dest");

        IOUtil.moveToDirectory(new File(child, "./."), dest);
        assertEquals("i", read(new File(dest, "child/in.txt")));
        assertFalse(child.exists());

        // a ".." followed by a real name is fine
        assertTrue(IOUtil.deleteRecursivelyIfExists(new File(dest, "child/../child/.")));
        assertFalse(new File(dest, "child").exists());

        final File other = newDir("c662f/d1/d2/d3/other");
        newFile("c662f/d1/d2/d3/other/f.txt", "f");
        assertTrue(IOUtil.deleteRecursivelyIfExists(new File(other, ".")));
        assertFalse(other.exists());

        final File single = newFile("c662f/d1/d2/d3/single.txt", "x");
        assertTrue(IOUtil.deleteIfExists(new File(dest, "../single.txt")));
        assertFalse(single.exists());

        if (IOUtil.IS_OS_WINDOWS) {
            // "child/..." names child on Windows (Win32 folds the dots away): the guard lets it through (not refused), and
            // whatever the delete manages, it never reaches beyond child.
            final File p2 = newDir("c662f/d1/d2/d3/p2");
            final File c2 = newDir("c662f/d1/d2/d3/p2/child2");
            final File keep = newFile("c662f/d1/d2/d3/p2/keep.txt", "k");
            IOUtil.deleteRecursivelyIfExists(new File(c2, "..."));
            IOUtil.deleteIfExists(new File(c2, "..."));
            assertTrue(p2.isDirectory());
            assertEquals("k", read(keep));
        }
    }

    @Test
    public void testC662_aRealDirectoryNamedThreeDotsStaysDeletableOnPosix() throws IOException {
        assumeFalse(IOUtil.IS_OS_WINDOWS, "\"...\" is an ordinary name only off Windows");

        final File parent = newDir("c662p/d1/d2/d3/parent");
        final File dots = newDir("c662p/d1/d2/d3/parent/...");
        newFile("c662p/d1/d2/d3/parent/.../in.txt", "i");
        final File child = newDir("c662p/d1/d2/d3/parent/child");

        assertTrue(IOUtil.deleteRecursivelyIfExists(dots));
        assertFalse(dots.exists());
        assertTrue(child.isDirectory());
        assertTrue(parent.isDirectory());

        // and "..." after a ".." is an ordinary (absent) name there, not a self-reference
        assertFalse(IOUtil.deleteRecursivelyIfExists(new File(child, "../...")));
        assertTrue(child.isDirectory());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-663 / C-664 / C-667: moveToDirectory decides "the same entry" by the identity of the two directories
    // ------------------------------------------------------------------------------------------------------------

    private static void junction(final File link, final File target) throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "junctions are Windows-only");
        assertTrue(link.isAbsolute() && target.isAbsolute());

        final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true)
                .start();
        p.getInputStream().readAllBytes();
        assumeTrue(p.waitFor() == 0, "mklink /J failed");
    }

    @Test
    public void testC663_twoLinkEntriesToOneTargetAreDifferentEntries() throws Exception {
        final File t = newDir("c663/d1/d2/d3/T");
        newFile("c663/d1/d2/d3/T/t.txt", "t");
        final File a = newDir("c663/d1/d2/d3/A");
        final File b = newDir("c663/d1/d2/d3/B");
        junction(new File(a, "j"), t);
        junction(new File(b, "j"), t);

        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(new File(a, "j"), b));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> IOUtil.moveToDirectory(new File(a, "j"), b, java.nio.file.StandardCopyOption.ATOMIC_MOVE));
        assertTrue(entry(new File(a, "j")));
        assertTrue(entry(new File(b, "j")));
        assertEquals("t", read(new File(t, "t.txt")));
    }

    // R1-07: this guards JDK 21 (the release target). Under JDK 22+ getCanonicalPath resolves junctions itself, so the
    // pre-C-664 code passes it there too; it fails on JDK 21 only.
    @Test
    public void testC664_sameDirectoryNoOpThroughAJunctionAliasOnEitherSide() throws Exception {
        final File p = newDir("c664/d1/d2/d3/P");
        final File f = newFile("c664/d1/d2/d3/P/f.txt", "same");
        final File sub = newDir("c664/d1/d2/d3/P/sub");
        newFile("c664/d1/d2/d3/P/sub/in.txt", "in");
        final File j = new File(base(), "c664/d1/d2/d3/J");
        junction(j, p);

        IOUtil.moveToDirectory(f, j);
        IOUtil.moveToDirectory(f, j, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        IOUtil.moveToDirectory(new File(j, "f.txt"), p);
        IOUtil.moveToDirectory(new File(j, "f.txt"), p, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        IOUtil.moveToDirectory(sub, j, java.nio.file.StandardCopyOption.ATOMIC_MOVE);

        assertEquals("same", read(f));
        assertEquals("in", read(new File(sub, "in.txt")));
        assertEquals(2, p.list().length);
    }

    @Test
    public void testC667_danglingLinkSpelledThroughDotIsTheNoOp() throws Exception {
        final File dest = newDir("c667/d1/d2/d3/dest");
        final File t = newDir("c667/d1/d2/d3/T");
        junction(new File(dest, "x"), t);
        Files.delete(t.toPath());

        IOUtil.moveToDirectory(new File(dest, "./x"), dest);
        IOUtil.moveToDirectory(new File(dest, "./x"), dest, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertTrue(entry(new File(dest, "x")));

        // two dangling links in different directories are different entries
        final File a = newDir("c667/d1/d2/d3/A");
        junction(new File(a, "x"), t);
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> IOUtil.moveToDirectory(new File(a, "x"), dest, java.nio.file.StandardCopyOption.ATOMIC_MOVE));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(new File(a, "x"), dest));
        assertTrue(entry(new File(a, "x")));
        assertTrue(entry(new File(dest, "x")));
    }

    @Test
    public void testC622_hardLinkAliasIsStillADifferentEntry() throws IOException {
        final File a = newDir("c622/d1/d2/d3/A");
        final File b = newDir("c622/d1/d2/d3/B");
        final File f = newFile("c622/d1/d2/d3/A/f.txt", "f");
        final File alias = new File(b, "f.txt");

        try {
            Files.createLink(alias.toPath(), f.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            assumeTrue(false, "hard links not supported: " + e);
        }

        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(f, b));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(f, b, java.nio.file.StandardCopyOption.ATOMIC_MOVE));
        assertTrue(f.isFile());
        assertTrue(alias.isFile());

        // the source itself, moved into its own directory, is the no-op
        IOUtil.moveToDirectory(f, a, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertEquals("f", read(f));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-686: copyToDirectory into the source's own directory spelled through a junction gives "Copy of"
    // ------------------------------------------------------------------------------------------------------------

    // R1-07: this guards JDK 21 (the release target). Under JDK 22+ getCanonicalPath resolves junctions itself, so the
    // pre-C-686 code passes it there too; it fails on JDK 21 only.
    @Test
    public void testC686_copyIntoTheOwnDirectoryThroughAJunctionIsACopyOf() throws Exception {
        final File p = newDir("c686/d1/d2/d3/P");
        final File f = newFile("c686/d1/d2/d3/P/f.txt", "orig");
        final File j = new File(base(), "c686/d1/d2/d3/J");
        junction(j, p);

        final File copy = IOUtil.copyToDirectory(f, j);
        assertEquals("Copy of f.txt", copy.getName());
        assertEquals("orig", read(new File(p, "Copy of f.txt")));
        assertEquals("orig", read(f));

        // the plain spelling is unchanged: a second "Copy of" is refused as already existing
        assertThrows(IOException.class, () -> IOUtil.copyToDirectory(f, p));

        // and a copy into a genuinely different directory keeps the name
        final File other = newDir("c686/d1/d2/d3/other");
        assertEquals("f.txt", IOUtil.copyToDirectory(f, other).getName());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-687 / C-688: readAllChars(File) streams; readAllBytes(File) / readAllToString(File) take the sized path
    // ------------------------------------------------------------------------------------------------------------

    /** Records whether length() was asked from inside the sized read (readAllBytesOfOpenedFile). */
    private static final class SizedReadProbeFile extends File {
        private static final long serialVersionUID = 1L;

        private final AtomicInteger sizedReadLengthCalls = new AtomicInteger();

        SizedReadProbeFile(final File file) {
            super(file.getPath());
        }

        @Override
        public long length() {
            if (StackWalker.getInstance().walk(frames -> frames.anyMatch(f -> "readAllBytesOfOpenedFile".equals(f.getMethodName())))) {
                sizedReadLengthCalls.incrementAndGet();
            }

            return super.length();
        }
    }

    @Test
    public void testC688_plainFileBytesAndStringReadsTakeTheSizedPath() throws IOException {
        final byte[] data = randomBytes(100_000, 688);
        final File real = newFile("c688/plain.bin", data);

        // Pins that the sized branch is live: no other test fails if it is dead (the growth path reads the same bytes).
        final SizedReadProbeFile bytesProbe = new SizedReadProbeFile(real);
        assertArrayEquals(data, IOUtil.readAllBytes(bytesProbe));
        assertEquals(1, bytesProbe.sizedReadLengthCalls.get(), "readAllBytes(File) sizes a plain file from its length");

        final SizedReadProbeFile stringProbe = new SizedReadProbeFile(real);
        assertEquals(new String(data, StandardCharsets.ISO_8859_1), IOUtil.readAllToString(stringProbe, StandardCharsets.ISO_8859_1));
        assertEquals(1, stringProbe.sizedReadLengthCalls.get(), "readAllToString(File) sizes a plain file from its length");

        // A compressed source never takes it: its decompressed size is unknown.
        final File gz = new File(base(), "c688/plain.bin.gz");

        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(gz.toPath()))) {
            os.write(data);
        }

        final SizedReadProbeFile gzProbe = new SizedReadProbeFile(gz);
        assertArrayEquals(data, IOUtil.readAllBytes(gzProbe));
        assertEquals(0, gzProbe.sizedReadLengthCalls.get());
    }

    @Test
    public void testC687_readAllCharsOfAFileStreamsThroughTheDecoder() throws IOException {
        final StringBuilder sb = new StringBuilder();

        while (sb.length() < 60_000) {
            sb.append("\u65e5\u672c\u8a9e\ud83d\ude00\u00e9-");
        }

        final String text = sb.toString();
        final File real = newFile("c687/cjk.txt", text);

        // C-687 (R2-01): not through the sized byte[] + String + char[] copy of C-649 - the streaming decoder of the
        // InputStream twin, which never holds the whole content three times.
        final SizedReadProbeFile probe = new SizedReadProbeFile(real);
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(probe));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(probe, StandardCharsets.UTF_8));
        assertArrayEquals(text.toCharArray(), IOUtil.readAllChars(probe, null));
        assertEquals(0, probe.sizedReadLengthCalls.get(), "readAllChars(File) must not take the sized byte[] path");

        // Identical to the InputStream twin, malformed input included.
        final byte[] malformed = { 'x', (byte) 0xFF, (byte) 0xC0, (byte) 0xAF, 'y', (byte) 0xE6, (byte) 0x97 };
        final File bad = newFile("c687/bad.txt", malformed);

        try (InputStream is = new FileInputStream(bad)) {
            assertArrayEquals(IOUtil.readAllChars(is, StandardCharsets.UTF_8), IOUtil.readAllChars(bad, StandardCharsets.UTF_8));
        }

        assertEquals(0, IOUtil.readAllChars(newFile("c687/empty.txt", "")).length);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-689 / C-690: toFile(URL) - share-less authority paths and slash-less drive spellings
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC689_authorityWithASeparatorOnlyPathNamesNoShare() throws Exception {
        for (final String spelling : new String[] { "file://server", "file://server/", "file://server//", "file://server///", "file://server/\\" }) {
            final URL url;

            try {
                url = new URL(spelling);
            } catch (final java.net.MalformedURLException e) {
                continue; // not every spelling is a parseable URL
            }

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(url), spelling);

            if (IOUtil.IS_OS_WINDOWS) {
                assertTrue(e.getMessage().contains("must also name a share"), spelling + " -> " + e.getMessage());
            }
        }

        if (IOUtil.IS_OS_WINDOWS) {
            assertEquals("\\\\server\\share", IOUtil.toFile(new URL("file://server/share")).getPath());
        }
    }

    @Test
    public void testC690_slashLessDriveSpellingsAreRejectedOnWindows() throws Exception {
        final String[] noFixedFile = { "file:C:", "file:c:", "file:C:x", "file:C:x/y", "file:z:." };

        if (IOUtil.IS_OS_WINDOWS) {
            for (final String spelling : noFixedFile) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
                assertTrue(e.getMessage().contains("must also name an absolute path"), spelling + " -> " + e.getMessage());
            }

            assertEquals(new File("C:\\x"), IOUtil.toFile(new URL("file:C:/x")));
            assertEquals(new File("C:\\"), IOUtil.toFile(new URL("file:C:/")));
        } else {
            // today's result off Windows: an ordinary relative name
            assertEquals(new File("C:x"), IOUtil.toFile(new URL("file:C:x")));
            assertEquals(new File("C:"), IOUtil.toFile(new URL("file:C:")));
            assertEquals(new File("C:/x"), IOUtil.toFile(new URL("file:C:/x")));
        }

        // a name that merely has a colon further in is not a drive
        assertEquals(new File("Cx:"), IOUtil.toFile(new URL("file:Cx:")));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-691: newZipOutputStream(OutputStream, Charset) and splitByLine(.., Charset) refuse a decode-only charset up front
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC691_zipOutputStreamAndSplitByLineRefuseADecodeOnlyCharset() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.newZipOutputStream(os, DECODE_ONLY));
        assertEquals(0, os.size());
        // the null check still comes first, and a null charset is UTF-8
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newZipOutputStream(null, DECODE_ONLY));
        IOUtil.newZipOutputStream(os, null).close();

        final File src = newFile("c691/src.txt", "a\nb\nc\n");
        final File empty = newFile("c691/empty.txt", "");
        final File missing = new File(base(), "c691/missing.txt");
        final File dest = new File(base(), "c691/parts");

        assertThrows(UnsupportedOperationException.class, () -> IOUtil.splitByLine(src, 2, dest, DECODE_ONLY));
        assertFalse(dest.exists(), "no destination directory is created");
        // an empty source writes no part, so the encoder was never reached: refused all the same
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.splitByLine(empty, 2, dest, DECODE_ONLY));
        assertFalse(dest.exists(), "no destination directory is created");
        // reported before the source's existence
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.splitByLine(missing, 2, dest, DECODE_ONLY));
        // argument checks still come first
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(src, 0, dest, DECODE_ONLY));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(src, 2, null, DECODE_ONLY));
        assertFalse(dest.exists());

        // an encoding charset still works
        IOUtil.splitByLine(src, 2, dest, StandardCharsets.UTF_8);
        assertTrue(dest.isDirectory());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-692: skip(InputStream) re-probes the seek once after a transient refusal
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC692_aTransientSeekRefusalIsReprobedAfterOneChunk() throws IOException {
        final byte[] data = randomBytes(1_000_000, 692);
        final File f = newFile("c692/data.bin", data);
        final AtomicInteger channelProbes = new AtomicInteger();
        final AtomicInteger availableCalls = new AtomicInteger();
        final AtomicInteger bytesRead = new AtomicInteger();

        // The first available() answer is off (a file that changed size between size() and available()): the first
        // probe is refused; the second, after one chunk, succeeds and the rest of the skip is a seek, not a copy.
        try (FileInputStream in = new FileInputStream(f) {
            @Override
            public int available() throws IOException {
                return availableCalls.incrementAndGet() == 1 ? 0 : super.available();
            }

            @Override
            public FileChannel getChannel() {
                channelProbes.incrementAndGet();
                return super.getChannel();
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                final int n = super.read(b, off, len);

                if (n > 0) {
                    bytesRead.addAndGet(n);
                }

                return n;
            }
        }) {
            assertEquals(900_000, IOUtil.skip(in, 900_000));
            assertEquals(2, channelProbes.get(), "one re-probe after the first chunk");
            assertTrue(bytesRead.get() > 0 && bytesRead.get() <= 64 * 1024, "only one chunk was read and discarded: " + bytesRead.get());
            assertEquals(data[900_000] & 0xFF, in.read());
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-693: "the same directory" is decided case-sensitively on Windows (case-sensitive directories)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC693_caseSensitiveSiblingsAreDifferentDirectories() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "Files.isSameFile's case-insensitive shortcut is Windows-only");

        final File cs = newDir("c693/d1/d2/d3/cs");
        assertTrue(cs.isAbsolute());

        final Process process = new ProcessBuilder("fsutil", "file", "setCaseSensitiveInfo", cs.getAbsolutePath(), "enable").redirectErrorStream(true)
                .start();
        process.getInputStream().readAllBytes();
        assumeTrue(process.waitFor() == 0, "fsutil setCaseSensitiveInfo is not available");

        final File upper = new File(cs, "P");
        final File lower = new File(cs, "p");
        assertTrue(upper.mkdir());
        assumeTrue(lower.mkdir(), "the directory is not case-sensitive");

        final File src = new File(upper, "f.txt");
        final File victim = new File(lower, "f.txt");
        Files.write(src.toPath(), "src".getBytes(StandardCharsets.UTF_8));
        Files.write(victim.toPath(), "keep".getBytes(StandardCharsets.UTF_8));

        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, lower));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> IOUtil.moveToDirectory(src, lower, java.nio.file.StandardCopyOption.ATOMIC_MOVE));
        assertEquals("keep", read(victim), "p/f.txt must not be replaced");
        assertEquals("src", read(src));

        // copy: a name clash in a different directory is "already exists", never a "Copy of" in the wrong directory
        final File g = new File(upper, "g.txt");
        Files.write(g.toPath(), "g".getBytes(StandardCharsets.UTF_8));
        final File copied = IOUtil.copyToDirectory(g, lower);
        assertEquals("g.txt", copied.getName());
        assertEquals("g", read(new File(lower, "g.txt")));
        assertFalse(new File(lower, "Copy of g.txt").exists());

        // the genuine same-directory cases are unchanged
        IOUtil.moveToDirectory(src, upper, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertEquals("src", read(src));
        assertEquals("Copy of g.txt", IOUtil.copyToDirectory(g, upper).getName());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-694: deleteFilesFromDirectory refuses a directory spelled as the parent of another
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC694_deleteFilesFromDirectoryRefusesTheParentSpellings() throws IOException {
        final List<String> spellings = new ArrayList<>(Arrays.asList("..", "../.", ".././", "./../.", "../../.", "../././.", "x/../.", "x/.././."));

        if (IOUtil.IS_OS_WINDOWS) {
            spellings.addAll(Arrays.asList("..\\.\\", "../...", "../. .", "..\\... "));
        }

        int n = 0;

        for (final String spelling : spellings) {
            final String c = "c694/s" + (n++);
            final File child = newDir(c + "/d1/d2/d3/parent/child");
            final File parent = child.getParentFile();
            final File grandParent = parent.getParentFile();
            final File sibling = newFile(c + "/d1/d2/d3/parent/sibling.txt", "s");
            final File inChild = newFile(c + "/d1/d2/d3/parent/child/in.txt", "i");
            final File target = new File(child, spelling);

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteFilesFromDirectory(target), spelling);
            assertTrue(e.getMessage().contains("must not end in '..'"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteFilesFromDirectory(target, (dir, f) -> {
                throw new AssertionError("the filter must not be consulted");
            }), spelling);

            assertTrue(child.isDirectory() && parent.isDirectory() && grandParent.isDirectory(), spelling + ": fixture intact");
            assertEquals("s", read(sibling), spelling);
            assertEquals("i", read(inChild), spelling);
        }

        // the filter's own null check still comes first; a null directory is still "false"
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteFilesFromDirectory(new File(base(), "c694/x/.."), null));
        assertFalse(IOUtil.deleteFilesFromDirectory(null));

        // "x/." is x, and "a/../x" is fine
        final File x = newDir("c694/ok/d1/d2/x");
        newFile("c694/ok/d1/d2/x/a.txt", "a");
        final File keep = newFile("c694/ok/d1/d2/keep.txt", "k");
        assertTrue(IOUtil.deleteFilesFromDirectory(new File(x, ".")));
        assertTrue(x.isDirectory());
        assertEquals(0, x.list().length);
        newFile("c694/ok/d1/d2/x/b.txt", "b");
        assertTrue(IOUtil.deleteFilesFromDirectory(new File(x, "../x"), (dir, f) -> true));
        assertEquals(0, x.list().length);
        assertEquals("k", read(keep));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-695: map / createFileIfNotExists / touch refuse a "."/".."-terminated target before creating anything
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC695_dotTerminatedTargetsOfMapCreateFileAndTouchLeaveNothingBehind() throws IOException {
        final File b = newDir("c695");
        final List<String> names = new ArrayList<>();
        final List<org.junit.jupiter.api.function.Executable> calls = new ArrayList<>();

        names.add("m1");
        calls.add(() -> IOUtil.map(new File(b, "m1/."), MapMode.READ_WRITE, 0, 16));
        names.add("m2");
        calls.add(() -> IOUtil.map(new File(b, "m2/q/.."), MapMode.PRIVATE, 0, 16));
        names.add("m4");
        calls.add(() -> IOUtil.map(new File(b, "m4/."), MapMode.READ_ONLY, 0, 0));
        names.add("c1");
        calls.add(() -> IOUtil.createFileIfNotExists(new File(b, "c1/.")));
        names.add("c2");
        calls.add(() -> IOUtil.createFileIfNotExists(new File(b, "c2/q/..")));
        names.add("t1");
        calls.add(() -> IOUtil.touch(new File(b, "t1/.")));
        names.add("t2");
        calls.add(() -> IOUtil.touch(new File(b, "t2/q/..")));

        for (int i = 0; i < calls.size(); i++) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, calls.get(i), names.get(i));
            assertTrue(e.getMessage().contains("is a directory, not a file"), names.get(i) + " -> " + e.getMessage());
            assertFalse(new File(b, names.get(i)).exists(), names.get(i) + " left a directory behind");
        }

        // an existing directory spelled "dir/." is refused too, and left alone
        final File dir = newDir("c695/existing");
        newFile("c695/existing/keep.txt", "keep");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.createFileIfNotExists(new File(dir, ".")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.touch(new File(dir, ".")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(new File(dir, "."), MapMode.READ_WRITE, 0, 1));
        assertEquals("keep", read(new File(dir, "keep.txt")));

        // ordinary targets still work, parents created
        final File created = new File(b, "ok/deep/f.txt");
        assertTrue(IOUtil.createFileIfNotExists(created));
        assertFalse(IOUtil.createFileIfNotExists(created));
        final File touched = new File(b, "ok/deeper/t.txt");
        IOUtil.touch(touched);
        assertTrue(touched.isFile());
        // a ".." followed by a real name is fine
        final File viaDotDot = new File(b, "ok/deep/../deep/g.txt");
        assertTrue(IOUtil.createFileIfNotExists(viaDotDot));
        assertTrue(new File(b, "ok/deep/g.txt").isFile());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-696: on Windows a trailing element made only of dots and spaces names the directory before it
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC696_dotsAndSpacesTargetsAreDirectoriesOnWindows() throws IOException {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "Win32 strips trailing dots and spaces; elsewhere \"...\" is an ordinary name");

        final File src = newFile("c696/src.txt", "payload");
        final URL url = src.toURI().toURL();
        final File b = new File(base(), "c696");

        final List<String> names = new ArrayList<>();
        final List<org.junit.jupiter.api.function.Executable> calls = new ArrayList<>();

        names.add("w1");
        calls.add(() -> IOUtil.write("x", new File(b, "w1/...")));
        names.add("w2");
        calls.add(() -> IOUtil.map(new File(b, "w2/. ."), MapMode.READ_WRITE, 0, 1));
        names.add("w3");
        calls.add(() -> IOUtil.createFileIfNotExists(new File(b, "w3/...")));
        names.add("w4");
        calls.add(() -> IOUtil.copyFile(src, new File(b, "w4/...")));
        names.add("w5");
        calls.add(() -> IOUtil.copyURLToFile(url, new File(b, "w5/.. .")));
        names.add("w6");
        calls.add(() -> IOUtil.zip(src, new File(b, "w6/...")));
        names.add("w7");
        calls.add(() -> IOUtil.touch(new File(b, "w7/ ")));
        names.add("w8");
        calls.add(() -> IOUtil.merge(Collections.singletonList(src), new File(b, "w8/....")));

        for (int i = 0; i < calls.size(); i++) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, calls.get(i), names.get(i));
            assertTrue(e.getMessage().contains("is a directory, not a file"), names.get(i) + " -> " + e.getMessage());
            assertFalse(new File(b, names.get(i)).exists(), names.get(i) + " left a directory behind");
        }

        assertEquals("payload", read(src));
    }

    // ------------------------------------------------------------------------------------------------------------
    // R1-06: moveToDirectory's no-name refusal names both causes
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testR1_06_noNameMoveMessageCoversTheWindowsSpelling() throws IOException {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "Windows folds trailing dots away");

        final File child = newDir("r106/parent/child");
        newFile("r106/parent/child/in.txt", "in");
        final File dest = new File(base(), "r106/dest");
        final File source = new File(child, "...");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(source, dest));
        assertTrue(e.getMessage().contains("has no name to be moved under"), e.getMessage());
        assertTrue(e.getMessage().contains("or a spelling the platform resolves to no name"), e.getMessage());
        assertFalse(dest.exists(), "nothing created");
        assertEquals("in", read(new File(child, "in.txt")));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-668 (white box): the "destination holds N bytes" branch of the doCopyFile post-check
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC668_destinationLengthMismatchIsReportedAsSuch() throws Exception {
        final File src = newFile("c668w/src.bin", randomBytes(10_000, 6680));
        final File real = new File(base(), "c668w/out/dest.bin");

        // A destination whose reported length disagrees with the bytes copied: reached without truncating anything
        // (the only real-world trigger is a concurrent writer on the destination), through IOUtil's own private method.
        final File lying = new File(real.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long length() {
                return super.length() + 7;
            }
        };

        final Method doCopyFile = IOUtil.class.getDeclaredMethod("doCopyFile", File.class, File.class, boolean.class);
        doCopyFile.setAccessible(true);

        final InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, () -> doCopyFile.invoke(null, src, lying, false));
        final Throwable cause = wrapped.getCause();
        assertTrue(cause instanceof IOException, String.valueOf(cause));
        assertTrue(cause.getMessage().contains("the destination holds 10007 bytes after the copy, not the 10000 bytes copied"), cause.getMessage());
        assertFalse(cause.getMessage().contains("shrank"), cause.getMessage());
        assertFalse(real.exists(), "the partial copy is removed");
    }
}
