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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the 2026-09-24 IOUtil review (ledger
 * {@code scripts/cross_review/Strings_IOUtil_CommonUtil_N_Array_Iterables_Iterators_Maps_Beans_Files_Multiset_Multimap_ledger_2026-09-24.md}).
 * Each test method name carries the ledger ID it pins.
 */
public class IOUtilReview20260924Test extends TestBase {

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------------

    /**
     * A reader that hands out the given chunks one per bulk read; an empty chunk is a read that returns 0. It does NOT
     * override read(), so the JDK default (which turns a zero into U+0000) is what a single-char read would get.
     */
    private static class ChunkReader extends Reader {
        private final String[] chunks;
        private final boolean markable;
        private int chunk;
        private int pos;
        private int markChunk;
        private int markPos;

        ChunkReader(final boolean markable, final String... chunks) {
            this.chunks = chunks;
            this.markable = markable;
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            if (len == 0) {
                return 0;
            }

            if (chunk >= chunks.length) {
                return -1;
            }

            final String c = chunks[chunk];

            if (c.isEmpty()) {
                chunk++;
                return 0;
            }

            final int n = Math.min(len, c.length() - pos);
            c.getChars(pos, pos + n, cbuf, off);
            pos += n;

            if (pos == c.length()) {
                chunk++;
                pos = 0;
            }

            return n;
        }

        @Override
        public boolean markSupported() {
            return markable;
        }

        @Override
        public void mark(final int readAheadLimit) throws IOException {
            if (!markable) {
                throw new IOException("mark not supported");
            }

            markChunk = chunk;
            markPos = pos;
        }

        @Override
        public void reset() throws IOException {
            if (!markable) {
                throw new IOException("reset not supported");
            }

            chunk = markChunk;
            pos = markPos;
        }

        @Override
        public void close() {
        }
    }

    /** An input stream that hands out the given chunks one per bulk read; an empty chunk is a read that returns 0. */
    private static InputStream chunkStream(final String... chunks) {
        return new InputStream() {
            private int chunk;

            @Override
            public int read(final byte[] b, final int off, final int len) {
                if (len == 0) {
                    return 0;
                }

                if (chunk >= chunks.length) {
                    return -1;
                }

                final byte[] c = chunks[chunk++].getBytes(StandardCharsets.UTF_8);

                if (c.length == 0) {
                    return 0;
                }

                assertTrue(c.length <= len, "test chunk larger than the requested length");
                System.arraycopy(c, 0, b, off, c.length);
                return c.length;
            }

            @Override
            public int read() {
                throw new AssertionError("single-byte read() must not be used");
            }
        };
    }

    private static List<String> jdkLines(final String text) throws IOException {
        final List<String> result = new ArrayList<>();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new StringReader(text))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                result.add(line);
            }
        }

        return result;
    }

    private static void writeZip(final File zip, final String[] names, final long time) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (final String name : names) {
                final ZipEntry ze = new ZipEntry(name);
                ze.setLastModifiedTime(FileTime.fromMillis(time));
                zos.putNextEntry(ze);

                if (!name.endsWith("/")) {
                    zos.write(("content of " + name).getBytes(StandardCharsets.UTF_8));
                }

                zos.closeEntry();
            }
        }
    }

    private static List<String> zipEntryNames(final File zip) throws IOException {
        final List<String> names = new ArrayList<>();

        try (ZipFile zf = new ZipFile(zip)) {
            final Enumeration<? extends ZipEntry> e = zf.entries();

            while (e.hasMoreElements()) {
                names.add(e.nextElement().getName());
            }
        }

        return names;
    }

    private static File newFile(final File dir, final String name, final String content) throws IOException {
        final File f = new File(dir, name);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-039 + C-087: a reader returning 0 before (or between) its data
    // ------------------------------------------------------------------------------------------------------------

    private static final String[][] ZERO_SHAPES = { { "", "abc\ndef" }, { "a\n", "", "b" }, { "ab\r", "", "\ncd" }, { "" }, { "", "", "x" },
            { "", "\u00e9t\u00e9\r\n\uD83D\uDE00" } };

    @Test
    public void testC039_everyLineApiAgreesWithReadAllToStringOnZeroReturningReaders() throws Exception {
        for (final boolean markable : new boolean[] { false, true }) {
            for (final String[] shape : ZERO_SHAPES) {
                final String text = IOUtil.readAllToString(new ChunkReader(markable, shape));
                final List<String> expected = jdkLines(text);
                final String label = Arrays.toString(shape) + " markable=" + markable;

                assertEquals(expected, IOUtil.readAllLines(new ChunkReader(markable, shape)), "readAllLines " + label);
                assertEquals(expected, IOUtil.readLines(new ChunkReader(markable, shape), 0, 10), "readLines " + label);
                assertEquals(expected.isEmpty() ? null : expected.get(0), IOUtil.readFirstLine(new ChunkReader(markable, shape)), "readFirstLine " + label);
                assertEquals(expected.isEmpty() ? null : expected.get(expected.size() - 1), IOUtil.readLastLine(new ChunkReader(markable, shape)),
                        "readLastLine " + label);

                for (int i = 0; i <= expected.size(); i++) {
                    assertEquals(i < expected.size() ? expected.get(i) : null, IOUtil.readLine(new ChunkReader(markable, shape), i), "readLine " + i + " " + label);
                }

                // forEachLine(Reader) reads through java.io.BufferedReader, whose fill() loops while a read returns 0:
                // it agrees with the rule above except that it also reads past two CONSECUTIVE zeros.
                boolean consecutiveZeros = false;

                for (int i = 0; i + 1 < shape.length; i++) {
                    consecutiveZeros |= shape[i].isEmpty() && shape[i + 1].isEmpty();
                }

                final List<String> seen = new ArrayList<>();
                IOUtil.forEachLine(new ChunkReader(markable, shape), seen::add);
                assertEquals(consecutiveZeros ? jdkLines(String.join("", shape)) : expected, seen, "forEachLine " + label);

                for (final String line : IOUtil.readLines(new ChunkReader(markable, shape), 0, 10)) {
                    assertEquals(-1, line.indexOf('\u0000'), "no U+0000 may be manufactured: " + label);
                }
            }
        }
    }

    @Test
    public void testC039_zeroFirstReaderNoLongerYieldsANulLine() {
        assertEquals(Arrays.asList("abc", "def"), IOUtil.readLines(new ChunkReader(false, "", "abc\ndef"), 0, 5));
        assertEquals("abc", IOUtil.readFirstLine(new ChunkReader(false, "", "abc\ndef")));
        assertEquals(Arrays.asList("abc", "def"), IOUtil.readAllLines(new ChunkReader(false, "", "abc\ndef")));
        assertEquals("def", IOUtil.readLastLine(new ChunkReader(false, "", "abc\ndef")));
        // 0 then end of input: an EMPTY source, not a line holding U+0000
        assertNull(IOUtil.readFirstLine(new ChunkReader(false, "")));
        assertEquals(Collections.emptyList(), IOUtil.readLines(new ChunkReader(true, ""), 0, 5));
    }

    @Test
    public void testC039_midStreamZeroDoesNotTruncate() {
        assertEquals(Arrays.asList("a", "b"), IOUtil.readAllLines(new ChunkReader(false, "a\n", "", "b")));
        assertEquals("b", IOUtil.readLine(new ChunkReader(false, "a\n", "", "b"), 1));
        assertEquals(Arrays.asList("ab", "cd"), IOUtil.readLines(new ChunkReader(false, "ab\r", "", "\ncd"), 0, 5));
        assertEquals(Arrays.asList("ab", "cd"), IOUtil.readLines(new ChunkReader(true, "ab\r", "", "\ncd"), 0, 5));
    }

    @Test
    public void testC087_contentEqualsIgnoreEolOnZeroReturningReaders() {
        assertTrue(IOUtil.contentEqualsIgnoreEOL(new ChunkReader(false, "", "b\r\n"), new StringReader("b")));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(new ChunkReader(false, "a\n", "", "b"), new StringReader("a\nb")));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(new ChunkReader(false, "ab", "", "cd\n"), new StringReader("abcd")));
        // two zero-first readers with DIFFERENT content used to compare as equal (both read as empty)
        assertFalse(IOUtil.contentEqualsIgnoreEOL(new ChunkReader(false, "", "b\r\n"), new ChunkReader(false, "", "x\r\n")));
    }

    @Test
    public void testC039_alwaysZeroReaderDoesNotHang() {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            final String[] zeros = new String[] { "", "", "", "", "", "", "", "", "", "" };
            assertEquals(Collections.emptyList(), IOUtil.readAllLines(new ChunkReader(false, zeros)));
            assertEquals(Collections.emptyList(), IOUtil.readLines(new ChunkReader(false, zeros), 0, 3));
            assertNull(IOUtil.readFirstLine(new ChunkReader(true, zeros)));
            assertNull(IOUtil.readLastLine(new ChunkReader(false, zeros)));
            assertTrue(IOUtil.contentEqualsIgnoreEOL(new ChunkReader(false, zeros), new StringReader("")));

            final Reader forever = new Reader() {
                @Override
                public int read(final char[] cbuf, final int off, final int len) {
                    return 0;
                }

                @Override
                public void close() {
                }
            };

            assertEquals(Collections.emptyList(), IOUtil.readAllLines(forever));
            assertNull(IOUtil.readFirstLine(forever));
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-040
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC040_stringEncodingOverloadsValidateTheSourceBeforeTheCharset() throws IOException {
        final File dir = tempDir.toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines((File) null, "no-such-cs"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((File) null, "no-such-cs"));

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString(dir, "no-such-cs"));
        assertTrue(e1.getMessage().contains("is a directory"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(dir, "bad name!"));
        assertTrue(e2.getMessage().contains("is a directory"), e2.getMessage());

        // a valid source still reports the bad charset, and null/empty still mean UTF-8
        final File f = newFile(dir, "t\u00e9.txt", "h\u00e9\nx");
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.readAllLines(f, "no-such-cs"));
        assertEquals(Arrays.asList("h\u00e9", "x"), IOUtil.readAllLines(f, (String) null));
        assertEquals("h\u00e9\nx", IOUtil.readAllToString(f, ""));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-042
    // ------------------------------------------------------------------------------------------------------------

    private static Field ioField(final String name) throws NoSuchFieldException {
        final Field f = IOUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Test
    public void testC042_nameCachedByAPeerDuringTheBackoffWinsOverUnknown() throws Exception {
        final Field hostNameField = ioField("hostName");
        final Field lockField = ioField("hostNameLock");
        final Field futureField = ioField("hostNameFuture");
        final Field retryField = ioField("hostNameRetryAtMillis");
        final Object lock = lockField.get(null);

        final Object savedName;
        final Object savedFuture;
        final long savedRetry;
        final AtomicReference<String> result = new AtomicReference<>();
        final Thread caller;

        synchronized (lock) {
            savedName = hostNameField.get(null);
            savedFuture = futureField.get(null);
            savedRetry = retryField.getLong(null);

            try {
                hostNameField.set(null, null);
                futureField.set(null, null);
                retryField.setLong(null, Long.MAX_VALUE / 2); // backoff in force

                // the caller reads hostName == null, then blocks on the lock inside the backoff check
                caller = new Thread(() -> result.set(IOUtil.getHostName()));
                caller.start();

                final long deadline = System.currentTimeMillis() + 10_000;

                while (caller.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
                    Thread.onSpinWait();
                }

                assertEquals(Thread.State.BLOCKED, caller.getState());

                // a peer caches the name while the caller waits
                hostNameField.set(null, "cached-by-peer");
            } catch (final Throwable t) {
                hostNameField.set(null, savedName);
                futureField.set(null, savedFuture);
                retryField.setLong(null, savedRetry);
                throw t;
            }
        }

        try {
            caller.join(10_000);
            assertEquals("cached-by-peer", result.get());
        } finally {
            synchronized (lock) {
                hostNameField.set(null, savedName);
                futureField.set(null, savedFuture);
                retryField.setLong(null, savedRetry);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-041: no practical test (needs a > 4 GB heap); C-043/C-052: source-only changes
    // C-047: chunks are forwarded as they arrive
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC047_streamCopyForwardsTheFirstChunkBeforeTheProducerContinues() throws Exception {
        runPipeCopy(0, Long.MAX_VALUE, "abcdef", "abc");
    }

    @Test
    public void testC047_streamCopyWithOffsetAndCountForwardsEachChunk() throws Exception {
        runPipeCopy(1, 4, "bcde", "bc");
    }

    private void runPipeCopy(final long offset, final long count, final String expectedTotal, final String expectedFirst) throws Exception {
        final PipedInputStream in = new PipedInputStream(64);
        final PipedOutputStream producer = new PipedOutputStream(in);
        final CountDownLatch firstChunkSeen = new CountDownLatch(1);
        final CountDownLatch releaseProducer = new CountDownLatch(1);
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        final AtomicReference<String> firstWrite = new AtomicReference<>();

        final OutputStream sink = new OutputStream() {
            @Override
            public synchronized void write(final int b) {
                write(new byte[] { (byte) b }, 0, 1);
            }

            @Override
            public synchronized void write(final byte[] b, final int off, final int len) {
                firstWrite.compareAndSet(null, new String(b, off, len, StandardCharsets.UTF_8));
                received.write(b, off, len);
                firstChunkSeen.countDown();
            }
        };

        final Thread producerThread = new Thread(() -> {
            try {
                producer.write("abc".getBytes(StandardCharsets.UTF_8));
                producer.flush();
                releaseProducer.await(20, TimeUnit.SECONDS);
                producer.write("def".getBytes(StandardCharsets.UTF_8));
                producer.close();
            } catch (final Exception e) {
                // the copier may have stopped reading (count reached); nothing to do
            }
        });

        final AtomicReference<Object> copied = new AtomicReference<>();
        final Thread copier = new Thread(() -> {
            try {
                copied.set(offset == 0 && count == Long.MAX_VALUE ? IOUtil.write(in, sink, true) : IOUtil.write(in, offset, count, sink, true));
            } catch (final Throwable t) {
                copied.set(t);
            }
        });

        producerThread.start();
        copier.start();

        final boolean arrivedEarly = firstChunkSeen.await(5, TimeUnit.SECONDS);
        releaseProducer.countDown();
        copier.join(20_000);
        producerThread.join(20_000);

        assertTrue(arrivedEarly, "the first chunk must reach the sink while the producer is still blocked");
        assertEquals(expectedFirst, firstWrite.get());
        assertEquals((long) expectedTotal.length(), copied.get());
        assertEquals(expectedTotal, received.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void testC047_readerCopyForwardsEachChunk() throws Exception {
        for (final boolean sliced : new boolean[] { false, true }) {
            final PipedReader in = new PipedReader(64);
            final PipedWriter producer = new PipedWriter(in);
            final CountDownLatch firstChunkSeen = new CountDownLatch(1);
            final CountDownLatch releaseProducer = new CountDownLatch(1);
            final StringBuilder received = new StringBuilder();

            final Writer sink = new Writer() {
                @Override
                public synchronized void write(final char[] cbuf, final int off, final int len) {
                    received.append(cbuf, off, len);
                    firstChunkSeen.countDown();
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };

            final Thread producerThread = new Thread(() -> {
                try {
                    producer.write("\u00e9bc");
                    producer.flush();
                    releaseProducer.await(20, TimeUnit.SECONDS);
                    producer.write("def");
                    producer.close();
                } catch (final Exception e) {
                    // ignored
                }
            });

            final AtomicReference<Object> copied = new AtomicReference<>();
            final Thread copier = new Thread(() -> {
                try {
                    copied.set(sliced ? IOUtil.write(in, 1, 4, sink, true) : IOUtil.write(in, sink, true));
                } catch (final Throwable t) {
                    copied.set(t);
                }
            });

            producerThread.start();
            copier.start();

            final boolean arrivedEarly = firstChunkSeen.await(5, TimeUnit.SECONDS);
            releaseProducer.countDown();
            copier.join(20_000);
            producerThread.join(20_000);

            assertTrue(arrivedEarly, "sliced=" + sliced);
            assertEquals(sliced ? 4L : 6L, copied.get());
            assertEquals(sliced ? "bcde" : "\u00e9bcdef", received.toString());
        }
    }

    @Test
    public void testC047_zeroReadRules() throws IOException {
        // one transient zero survives
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(4, IOUtil.write(chunkStream("ab", "", "cd"), out, true));
        assertEquals("abcd", out.toString(StandardCharsets.UTF_8.name()));

        // a zero first is retried as well
        out = new ByteArrayOutputStream();
        assertEquals(2, IOUtil.write(chunkStream("", "xy"), out, true));
        assertEquals("xy", out.toString(StandardCharsets.UTF_8.name()));

        // two consecutive zeros end the copy
        out = new ByteArrayOutputStream();
        assertEquals(0, IOUtil.write(chunkStream("", "", "data"), out, true));
        assertEquals(0, out.size());

        // U12-06 (2026-09-25): the same three rules on the Reader side, which has its own retry branch in copyChars
        StringWriter chunked = new StringWriter();
        assertEquals(4, IOUtil.write(new ChunkReader(false, "ab", "", "cd"), chunked, true));
        assertEquals("abcd", chunked.toString());

        chunked = new StringWriter();
        assertEquals(2, IOUtil.write(new ChunkReader(false, "", "xy"), chunked, true));
        assertEquals("xy", chunked.toString());

        chunked = new StringWriter();
        assertEquals(0, IOUtil.write(new ChunkReader(false, "", "", "data"), chunked, true));
        assertEquals("", chunked.toString());

        // exact totals with offset/count; the remainder stays in the source
        final Reader r = new StringReader("0123456789");
        final StringWriter w = new StringWriter();
        assertEquals(5, IOUtil.write(r, 2, 5, w, false));
        assertEquals("23456", w.toString());
        assertEquals('7', r.read());
    }

    @Test
    public void testC047_fileTargetsStillCopyEverything() throws IOException {
        final File target = new File(tempDir.toFile(), "t/out.bin");
        assertEquals(6, IOUtil.write(chunkStream("ab", "", "cd", "ef"), target));
        assertEquals("abcdef", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
        assertEquals(3, IOUtil.append(chunkStream("xyz"), target));
        assertEquals("abcdefxyz", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-048: array write/append count-0 shortcut (C-053 WONTFIX: range-before-destination is a pinned standing order)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC048_countZeroBoundaries() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtil.write(new byte[3], 3, 0, out);
        IOUtil.write(new char[3], 3, 0, out);
        IOUtil.write((byte[]) null, 0, 0, out);
        IOUtil.write((char[]) null, 0, 0, new StringWriter());
        assertEquals(0, out.size());
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new byte[3], 4, 0, out));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[3], 4, 0, new StringWriter()));
        IOUtil.write("h\u00e9\uD83D\uDE00".toCharArray(), 1, 3, StandardCharsets.UTF_8, out);
        assertEquals("\u00e9\uD83D\uDE00", out.toString(StandardCharsets.UTF_8.name()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-049
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC049_twoArgWriteLinesAsksHasNextNoMoreThanNeeded() throws IOException {
        final AtomicInteger hasNextCalls = new AtomicInteger();
        final Iterator<Object> one = new Iterator<Object>() {
            private boolean done;

            @Override
            public boolean hasNext() {
                hasNextCalls.incrementAndGet();
                return !done;
            }

            @Override
            public Object next() {
                done = true;
                return "x\u00e9";
            }
        };

        final StringWriter w = new StringWriter();
        IOUtil.writeLines(one, w);
        assertEquals("x\u00e9\n", w.toString());
        assertEquals(3, hasNextCalls.get()); // the 3-arg emptiness check + the loop's two

        final StringWriter w2 = new StringWriter();
        IOUtil.writeLines((Iterator<?>) null, w2);
        IOUtil.writeLines((Iterable<?>) null, w2);
        IOUtil.writeLines(Collections.emptyList(), w2);
        assertEquals("", w2.toString());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Arrays.asList("a"), (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Collections.emptyIterator(), (Writer) null));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-051: the write(InputStream, offset, ..) early return is unchanged (buffer allocation is not observable)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC051_offsetBeyondTheSourceStillReturnsZeroAndFlushes() throws IOException {
        final AtomicInteger flushes = new AtomicInteger();
        final OutputStream out = new ByteArrayOutputStream() {
            @Override
            public void flush() {
                flushes.incrementAndGet();
            }
        };

        assertEquals(0, IOUtil.write(new java.io.ByteArrayInputStream(new byte[5]), 10, 3, out, true));
        assertEquals(1, flushes.get());
        assertEquals(3, IOUtil.write(new java.io.ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }), 2, 3, out, false));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-058: simplifyPath UNC host position
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC058_uncHostPositionIsKeptVerbatim() {
        assertEquals("//.", IOUtil.simplifyPath("//."));
        assertEquals("//..", IOUtil.simplifyPath("//.."));
        assertEquals("//.", IOUtil.simplifyPath("//./.."));
        assertEquals("//./pipe/x", IOUtil.simplifyPath("\\\\.\\pipe\\x"));
        assertEquals("//./COM1", IOUtil.simplifyPath("\\\\.\\COM1"));
        assertEquals("//./a", IOUtil.simplifyPath("//./a"));
        assertEquals("//?/C:/a", IOUtil.simplifyPath("\\\\?\\C:\\a\\b\\.."));
        assertEquals("//?/UNC/host/share", IOUtil.simplifyPath("\\\\?\\UNC\\host\\share\\x\\..\\..\\.."));
        assertEquals("//\u4e3b\u673a/\u5171\u4eab/x", IOUtil.simplifyPath("//\u4e3b\u673a/\u5171\u4eab/./x"));

        // unchanged pins
        assertEquals("//host/share", IOUtil.simplifyPath("//host/share/../.."));
        assertEquals("//host/share/x", IOUtil.simplifyPath("//host/share/y/../x"));
        assertEquals("/", IOUtil.simplifyPath("//"));
        assertEquals("//a", IOUtil.simplifyPath("//a"));
        assertEquals("/a", IOUtil.simplifyPath("///a"));
        assertEquals(".", IOUtil.simplifyPath(""));
        assertEquals(".", IOUtil.simplifyPath(null));
    }

    @Test
    public void testC058_simplifyPathIsIdempotent() {
        final char[] alphabet = { 'a', '.', '/' };
        final List<String> inputs = new ArrayList<>();
        inputs.add("");

        for (int len = 1; len <= 7; len++) {
            final int total = (int) Math.pow(alphabet.length, len);

            for (int k = 0; k < total; k++) {
                final char[] s = new char[len];
                int v = k;

                for (int i = 0; i < len; i++) {
                    s[i] = alphabet[v % alphabet.length];
                    v /= alphabet.length;
                }

                inputs.add(new String(s));
            }
        }

        for (final String s : inputs) {
            final String once = IOUtil.simplifyPath(s);
            assertEquals(once, IOUtil.simplifyPath(once), "not idempotent for '" + s + "'");
            assertEquals(once, IOUtil.simplifyPath(s.replace('/', '\\')), "backslash form differs for '" + s + "'");
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-059
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC059_rejectedMapRangeLeavesNoFileBehind() {
        for (final MapMode mode : new MapMode[] { MapMode.READ_WRITE, MapMode.PRIVATE }) {
            final File sub = new File(tempDir.toFile(), "sub-" + mode);
            final File file = new File(sub, "m\u00e9.bin");

            assertThrows(IllegalArgumentException.class, () -> IOUtil.map(file, mode, 0, Integer.MAX_VALUE + 1L));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.map(file, mode, Long.MAX_VALUE, 1));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.map(file, mode, -1, 1));

            assertFalse(file.exists(), mode.toString());
            assertFalse(sub.exists(), mode.toString());
        }

        // boundary: a zero-length region at offset 0 is fine and creates the file
        final File ok = new File(tempDir.toFile(), "ok/z.bin");
        IOUtil.map(ok, MapMode.READ_WRITE, 0, 0);
        assertTrue(ok.exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-064 / C-065 / C-067: copy family
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC064_repeatedNameIsRejectedBeforeAnySiblingIsCopied() throws IOException {
        final File a = new File(tempDir.toFile(), "a");
        final File b = new File(a, "b");
        newFile(b, "0first.txt", "1");
        newFile(b, "b/leaf.txt", "2");
        newFile(b, "zlast.txt", "3");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
        assertTrue(e.getMessage().contains("is the source directory, or one of its ancestors"), e.getMessage());
        assertFalse(new File(a, "0first.txt").exists(), "no sibling may be copied before the rejection");
        assertFalse(new File(a, "zlast.txt").exists());
        assertEquals("2", new String(Files.readAllBytes(new File(b, "b/leaf.txt").toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void testC064_copyIntoAnAncestorWithoutARepeatedNameStillWorks() throws IOException {
        final File a = new File(tempDir.toFile(), "\u00e4");
        final File b = new File(a, "b");
        newFile(b, "x.txt", "x");
        newFile(b, "c/y.txt", "y");

        IOUtil.copyDirectory(b, a);

        assertTrue(new File(a, "x.txt").isFile());
        assertTrue(new File(a, "c/y.txt").isFile());
    }

    @Test
    public void testC065_directoryLinkCopiedIntoItsOwnDirectoryIsAnIllegalArgument() throws Exception {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "junction test");
        final File real = new File(tempDir.toFile(), "real");
        newFile(real, "r.txt", "r");
        final File md = new File(tempDir.toFile(), "md");
        md.mkdirs();
        final File link = new File(md, "j4");

        final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), real.getAbsolutePath()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        Assumptions.assumeTrue(p.waitFor() == 0 && Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS), "mklink /J is not available");

        try {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(link, md));
            assertEquals(Arrays.asList("r.txt"), Arrays.asList(real.list()));
            assertEquals(Arrays.asList("j4"), Arrays.asList(md.list()));
        } finally {
            Files.deleteIfExists(link.toPath());
        }
    }

    @Test
    public void testC067_nullArgumentsAreReportedBeforeAMissingSource() {
        final File missing = new File(tempDir.toFile(), "missing");
        final File dest = new File(tempDir.toFile(), "dest");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(missing, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(missing, dest, true, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(missing, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(missing, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(missing, dest, (java.nio.file.CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(null, dest));

        // with every argument present the missing source is still a FileNotFoundException, and nothing is created
        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.copyToDirectory(missing, dest));
        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.moveToDirectory(missing, dest));
        assertFalse(dest.exists());
    }

    @Test
    public void testC066_copyFileWithoutPreserveFileDateStillCopies() throws IOException {
        final File src = newFile(tempDir.toFile(), "s.txt", "s");
        assertTrue(src.setLastModified(1_600_000_000_000L));
        final File d1 = new File(tempDir.toFile(), "d1.txt");
        final File d2 = new File(tempDir.toFile(), "d2.txt");

        IOUtil.copyFile(src, d1, false);
        IOUtil.copyFile(src, d2, true);

        assertTrue(d1.exists());
        assertEquals(src.lastModified(), d2.lastModified());
    }

    @Test
    public void testC073_copyFileWithoutReplaceExistingFailsOnAnExistingTarget() throws IOException {
        final File src = newFile(tempDir.toFile(), "s.txt", "new");
        final File dst = newFile(tempDir.toFile(), "d.txt", "old");

        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.copyFile(src, dst, new java.nio.file.CopyOption[0]));
        assertEquals("old", new String(Files.readAllBytes(dst.toPath()), StandardCharsets.UTF_8));
        IOUtil.copyFile(src, dst, StandardCopyOption.REPLACE_EXISTING);
        assertEquals("new", new String(Files.readAllBytes(dst.toPath()), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-068 / C-069: close family
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC068_laterFailuresSurviveAnUnwrappedFirstFailure() {
        final IOException inner = new IOException("inner");
        final IOException second = new IOException("second");

        final RuntimeException e = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(() -> {
            throw new ExecutionException(inner);
        }, () -> {
            throw second;
        }));

        assertSame(inner, e.getCause());
        assertTrue(Arrays.asList(e.getSuppressed()).contains(second), Arrays.toString(e.getSuppressed()));
    }

    @Test
    public void testC068_runtimeExceptionsArePropagatedUnchanged() {
        final UndeclaredThrowableException ute = new UndeclaredThrowableException(new IOException("x"));
        final IOException second = new IOException("second");

        assertSame(ute, assertThrows(UndeclaredThrowableException.class, () -> IOUtil.close(() -> {
            throw ute;
        })));

        final UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> IOUtil.closeAll(() -> {
            throw ute;
        }, () -> {
            throw second;
        }));

        assertSame(ute, thrown);
        assertTrue(Arrays.asList(thrown.getSuppressed()).contains(second));

        // an ordinary checked first failure: the wrapper carries the later ones exactly once
        final IOException first = new IOException("first");
        final IOException third = new IOException("third");
        final RuntimeException wrapped = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(() -> {
            throw first;
        }, () -> {
            throw third;
        }));
        assertSame(first, wrapped.getCause());
        assertEquals(1, Arrays.stream(wrapped.getSuppressed()).filter(s -> s == third).count());
    }

    @Test
    public void testC069_anErrorDoesNotStopTheRemainingCloses() {
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicBoolean closed2 = new AtomicBoolean();
        final AssertionError boom = new AssertionError("boom");
        final IOException later = new IOException("later");

        final AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtil.closeAll(() -> {
            throw boom;
        }, () -> closed.set(true), () -> {
            throw later;
        }));

        assertSame(boom, thrown);
        assertTrue(closed.get(), "the closeable after the Error must still be closed");
        assertTrue(Arrays.asList(thrown.getSuppressed()).contains(later));

        final AssertionError quiet = assertThrows(AssertionError.class, () -> IOUtil.closeAllQuietly(() -> {
            throw new AssertionError("quiet");
        }, () -> closed2.set(true), null));
        assertEquals("quiet", quiet.getMessage());
        assertTrue(closed2.get());

        // exceptions stay quiet
        IOUtil.closeAllQuietly(() -> {
            throw new IOException("ignored");
        }, null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-074: splitByLine balance
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC074_shortLinesAreSplitEvenly() throws IOException {
        final File src = new File(tempDir.toFile(), "x.txt");
        final StringBuilder sb = new StringBuilder(400_000);

        for (int i = 0; i < 200_000; i++) {
            sb.append("x\n");
        }

        Files.write(src.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        final File parts = new File(tempDir.toFile(), "parts");
        IOUtil.splitByLine(src, 4, parts);

        final File[] files = parts.listFiles();
        Arrays.sort(files);
        assertEquals(4, files.length);

        for (final File part : files) {
            assertEquals(50_000, IOUtil.readAllLines(part).size(), part.getName());
        }
    }

    @Test
    public void testC074_largeFileOfShortLinesIsBalancedWithinAFewPercent() throws IOException {
        final File src = new File(tempDir.toFile(), "big.txt");
        final int lines = 3_000_000;
        final byte[] content = new byte[lines * 2];

        for (int i = 0; i < lines; i++) {
            content[2 * i] = 'y';
            content[2 * i + 1] = '\n';
        }

        Files.write(src.toPath(), content);
        final File parts = new File(tempDir.toFile(), "bigparts");
        IOUtil.splitByLine(src, 4, parts);

        final File[] files = parts.listFiles();
        assertEquals(4, files.length);
        int min = Integer.MAX_VALUE;
        int max = 0;
        int total = 0;

        for (final File part : files) {
            final int n = (int) (part.length() / 2);
            min = Math.min(min, n);
            max = Math.max(max, n);
            total += n;
        }

        assertEquals(lines, total);
        assertTrue(max <= min * 1.10, "parts are unbalanced: min=" + min + ", max=" + max);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-075 / C-080 / C-081: zip
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC075_anEntryNameTooLongForAZipHeaderLeavesAnExistingTargetUnchanged() throws IOException {
        final StringBuilder nameBuilder = new StringBuilder();

        for (int i = 0; i < 250; i++) {
            nameBuilder.append((char) (0x4e00 + i));
        }

        final String name = nameBuilder.toString(); // 250 chars, 750 UTF-8 bytes
        final File root = new File(tempDir.toFile(), "deep");
        Path deepest = root.toPath();

        try {
            for (int i = 0; i < 88; i++) { // 88 * 751 > 65,480 bytes
                deepest = deepest.resolve(name);
            }

            try {
                Files.createDirectories(deepest);
            } catch (final IOException | InvalidPathException e) {
                Assumptions.abort("cannot create a path this long here: " + e);
            }

            final File target = new File(tempDir.toFile(), "keep.zip");
            Files.write(target.toPath(), "8 bytes!".getBytes(StandardCharsets.UTF_8));

            final IOException e1 = assertThrows(IOException.class, () -> IOUtil.zip(root, target));
            assertTrue(e1.getMessage().contains("too long"), e1.getMessage());
            assertEquals("8 bytes!", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));

            final IOException e2 = assertThrows(IOException.class, () -> IOUtil.zip(Arrays.asList(root), target));
            assertTrue(e2.getMessage().contains("too long"), e2.getMessage());
            assertEquals("8 bytes!", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
        } finally {
            IOUtil.deleteRecursivelyIfExists(root);
        }
    }

    @Test
    public void testC080_directoryCreatedForTheTargetIsNotArchived() throws IOException {
        final File src = new File(tempDir.toFile(), "s10");
        newFile(src, "a.txt", "a");
        final File target = new File(src, "sub/out.zip");

        IOUtil.zip(src, target);
        List<String> names = zipEntryNames(target);
        assertTrue(names.contains("s10/a.txt"), names.toString());
        assertFalse(names.contains("s10/sub/"), names.toString());
        assertFalse(names.contains("s10/sub/out.zip"), names.toString());

        final File target2 = new File(src, "sub2/deeper/out.zip");
        IOUtil.zip(Arrays.asList(src), target2);
        names = zipEntryNames(target2);
        assertFalse(names.contains("s10/sub2/"), names.toString());
        assertFalse(names.contains("s10/sub2/deeper/"), names.toString());

        // a directory that existed before the call is still archived
        final File pre = new File(tempDir.toFile(), "pre");
        newFile(pre, "keep/k.txt", "k");
        final File target3 = new File(pre, "keep/out.zip");
        IOUtil.zip(pre, target3);
        names = zipEntryNames(target3);
        assertTrue(names.contains("pre/keep/"), names.toString());
        assertTrue(names.contains("pre/keep/k.txt"), names.toString());
    }

    @Test
    public void testC081_decodeOnlyCharsetLeavesNoJunkArchive() throws IOException {
        final Charset decodeOnly = Charset.availableCharsets().values().stream().filter(c -> !c.canEncode()).findFirst().orElse(null);
        Assumptions.assumeTrue(decodeOnly != null, "no decode-only charset in this JVM");

        final File src = new File(tempDir.toFile(), "src");
        newFile(src, "a.txt", "a");
        final File absent = new File(tempDir.toFile(), "absent.zip");

        assertThrows(UnsupportedOperationException.class, () -> IOUtil.zip(src, absent, decodeOnly));
        assertFalse(absent.exists());

        final File existing = newFile(tempDir.toFile(), "existing.zip", "old");
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.zip(src, existing, decodeOnly));
        assertEquals("old", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void testC044_decodeOnlyCharsetEncodersThrowUnsupportedOperation() {
        final Charset decodeOnly = Charset.availableCharsets().values().stream().filter(c -> !c.canEncode()).findFirst().orElse(null);
        Assumptions.assumeTrue(decodeOnly != null, "no decode-only charset in this JVM");

        assertThrows(UnsupportedOperationException.class, () -> IOUtil.charsToBytes("a".toCharArray(), decodeOnly));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.charsToBytes("ab".toCharArray(), 1, 1, decodeOnly));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.stringToInputStream("a", decodeOnly));
        assertEquals(0, IOUtil.charsToBytes(new char[0], decodeOnly).length);
        assertEquals(0, IOUtil.charsToBytes("ab".toCharArray(), 1, 0, decodeOnly).length);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-076 / C-077 / C-078 / C-082: unzip
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC076_directoryEntryNamingTheTargetDoesNotRestampIt() throws IOException {
        final File zip = new File(tempDir.toFile(), "dot.zip");
        writeZip(zip, new String[] { "./", "sub/../" }, 1_000_000_000_000L);

        final File out = new File(tempDir.toFile(), "out");
        out.mkdirs();
        final long stamp = 1_700_000_000_000L;
        assertTrue(out.setLastModified(stamp));

        IOUtil.unzip(zip, out);

        assertEquals(stamp, out.lastModified());
        assertEquals(0, out.list().length);
    }

    @Test
    public void testC077_fileEntryEndingInDotDotIsRejected() throws IOException {
        final File zip = new File(tempDir.toFile(), "dd.zip");
        writeZip(zip, new String[] { "x/y/.." }, 1_600_000_000_000L);
        final File out = new File(tempDir.toFile(), "out");

        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, out));
        assertTrue(e.getMessage().contains("x/y/.."), e.getMessage());
        assertFalse(new File(out, "x").isFile());

        // an ordinary nested file still extracts
        final File ok = new File(tempDir.toFile(), "ok.zip");
        writeZip(ok, new String[] { "x/y/\u00e9.txt" }, 1_600_000_000_000L);
        final File out2 = new File(tempDir.toFile(), "out2");
        IOUtil.unzip(ok, out2);
        assertTrue(new File(out2, "x/y/\u00e9.txt").isFile());
    }

    @Test
    public void testC078_aCorruptArchiveLeavesNoTargetDirectoryBehind() throws IOException {
        final File corrupt = newFile(tempDir.toFile(), "corrupt.zip", "this is not a zip archive");
        final File target = new File(tempDir.toFile(), "out/nested");

        assertThrows(IOException.class, () -> IOUtil.unzip(corrupt, target));
        assertFalse(target.exists());
        assertFalse(target.getParentFile().exists());

        // argument errors still win over the corrupt archive
        final File aFile = newFile(tempDir.toFile(), "plain.txt", "p");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(corrupt, aFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(corrupt, null));

        // an empty but valid archive creates the target
        final File empty = new File(tempDir.toFile(), "empty.zip");
        writeZip(empty, new String[0], 0);
        IOUtil.unzip(empty, target);
        assertTrue(target.isDirectory());
    }

    @Test
    public void testC082_directoryEntryBelowAFileNamesTheEntry() throws IOException {
        final File zip = new File(tempDir.toFile(), "fd.zip");
        writeZip(zip, new String[] { "f", "f/d/" }, 1_600_000_000_000L);
        final File out = new File(tempDir.toFile(), "out");

        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, out));
        assertFalse(e instanceof NoSuchFileException, e.toString());
        assertTrue(e.getMessage().contains("Zip entry") && e.getMessage().contains("f/d/"), e.getMessage());
        assertTrue(new File(out, "f").isFile());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-079
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC079_mergeNamesTheNullElementAfterItsParameter() throws IOException {
        final File a = newFile(tempDir.toFile(), "a.txt", "a");
        final File dest = newFile(tempDir.toFile(), "dest.txt", "keep");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Arrays.asList(a, null), dest));
        assertTrue(e.getMessage().contains("sourceFile"), e.getMessage());
        assertEquals("keep", new String(Files.readAllBytes(dest.toPath()), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-088 / C-089 / C-090 / C-091: forEachLine
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC088_negativeLineOffsetIsReportedAsLineOffsetOnEverySource() throws IOException {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(new StringReader("a"), -1, 1, line -> {
        }));
        assertTrue(e.getMessage().contains("'lineOffset'"), e.getMessage());

        // the order matches the File core: the negative offset wins over the null action
        e = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(new StringReader("a"), -1, 1, null));
        assertTrue(e.getMessage().contains("'lineOffset'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(new java.io.ByteArrayInputStream(new byte[0]), 0, -2, line -> {
        }));
        assertTrue(e.getMessage().contains("'count'=-2"), e.getMessage());

        final File f = newFile(tempDir.toFile(), "f.txt", "x");
        e = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(Arrays.asList(f, null), line -> {
        }));
        assertTrue(e.getMessage().contains("'files'"), e.getMessage());
        assertFalse(e.getMessage().contains("'file' "), e.getMessage());
    }

    /** A directory whose listing hands out File objects of the given factory's making. */
    private static final class ListingDir extends File {
        private static final long serialVersionUID = 1L;
        private final transient java.util.function.Function<File, File> childFactory;

        ListingDir(final File dir, final java.util.function.Function<File, File> childFactory) {
            super(dir.getPath());
            this.childFactory = childFactory;
        }

        @Override
        public File[] listFiles() {
            final File[] real = new File(getPath()).listFiles();

            if (real == null) {
                return null;
            }

            Arrays.sort(real);
            final File[] result = new File[real.length];

            for (int i = 0; i < real.length; i++) {
                result[i] = childFactory.apply(real[i]);
            }

            return result;
        }
    }

    @Test
    public void testC089_unreadableChildOfADirectoryElementFailsBeforeAnyLine() throws IOException {
        final File dir = new File(tempDir.toFile(), "d");
        newFile(dir, "a.txt", "a1\na2");
        newFile(dir, "z.txt", "z1");

        final File listing = new ListingDir(dir, child -> child.getName().equals("z.txt") ? new File(child.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canRead() {
                return false;
            }
        } : child);

        final List<String> seenViaCollection = new ArrayList<>();
        final UncheckedIOException e1 = assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(Arrays.asList(listing), seenViaCollection::add));
        assertTrue(e1.getMessage().contains("cannot be read"), e1.getMessage());
        assertEquals(Collections.emptyList(), seenViaCollection);

        final List<String> seenViaFile = new ArrayList<>();
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(listing, seenViaFile::add));
        assertEquals(Collections.emptyList(), seenViaFile);

        // readable children: both overloads read everything
        final List<String> all = new ArrayList<>();
        IOUtil.forEachLine(Arrays.asList(dir), all::add);
        Collections.sort(all);
        assertEquals(Arrays.asList("a1", "a2", "z1"), all);
    }

    @Test
    public void testC090_aChildWhosePathCannotBeConvertedIsJudgedByJavaIo() throws IOException {
        final File dir = new File(tempDir.toFile(), "d");
        newFile(dir, "a.txt", "a1");
        newFile(dir, "b.txt", "b1");

        final File listing = new ListingDir(dir, child -> child.getName().equals("b.txt") ? new File(child.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Path toPath() {
                throw new InvalidPathException(getPath(), "unmappable (test)");
            }
        } : child);

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(listing, seen::add);
        Collections.sort(seen);
        assertEquals(Arrays.asList("a1", "b1"), seen);
    }

    @Test
    public void testC091_readThreadsOnACallerReaderDoesNotReadAhead() throws IOException {
        final java.io.BufferedReader reader = new java.io.BufferedReader(new StringReader("L0\nL1\nL2\nL3\nL4\nL5"));
        final List<String> seen = Collections.synchronizedList(new ArrayList<>());

        IOUtil.forEachLine(reader, IOUtil.LineIterationOptions.builder().readThreads(2).processThreads(2).count(3).build(), seen::add);

        final List<String> sorted = new ArrayList<>(seen);
        Collections.sort(sorted);
        assertEquals(Arrays.asList("L0", "L1", "L2"), sorted);
        assertEquals("L3", reader.readLine());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-092
    // ------------------------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    public void testC092_fileUrlWithoutAPathIsRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:?q=1")));

        final File f = newFile(tempDir.toFile(), "my docs/\u00e9.txt", "x");
        assertEquals(f.getCanonicalFile(), IOUtil.toFile(IOUtil.toUrl(f)).getCanonicalFile());
        assertNotEquals("", IOUtil.toFile(new URL("file:///x")).getPath());
    }
}
