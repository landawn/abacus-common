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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the 2026-09-25 IOUtil review, cycle 1 (ledger rows C-601..C-648). Each test method name
 * carries the finding it pins. The child-process tests (C-612, C-613) launch a second JVM whose stdin/stdout are
 * pipes; the child entry point is {@link #main(String[])}.
 */
public class IOUtilReview20260925bTest extends TestBase {

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------------

    private static final Charset DECODE_ONLY = Charset.forName("ISO-2022-CN");

    private File newFile(final String name, final String content) throws IOException {
        final File f = tempDir.resolve(name).toFile();
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private File newDir(final String name) throws IOException {
        final File d = tempDir.resolve(name).toFile();
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

    /** An InputStream whose bulk reads follow a script: 0 = answer zero, -1 = end of stream, n > 0 = deliver n bytes. */
    private static final class ScriptedInputStream extends InputStream {
        private final int[] script;
        private final byte[] data;
        private int step = 0;
        private int pos = 0;
        int reads = 0;

        ScriptedInputStream(final byte[] data, final int... script) {
            this.data = data;
            this.script = script;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException("bulk reads only");
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            reads++;

            if (step >= script.length) {
                return -1;
            }

            final int s = script[step++];

            if (s <= 0) {
                return s;
            }

            final int n = Math.min(Math.min(s, len), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
    }

    /** The Reader twin of {@link ScriptedInputStream}. */
    private static final class ScriptedReader extends Reader {
        private final int[] script;
        private final char[] data;
        private int step = 0;
        private int pos = 0;
        int reads = 0;

        ScriptedReader(final String data, final int... script) {
            this.data = data.toCharArray();
            this.script = script;
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            reads++;

            if (step >= script.length) {
                return -1;
            }

            final int s = script[step++];

            if (s <= 0) {
                return s;
            }

            final int n = Math.min(Math.min(s, len), data.length - pos);
            System.arraycopy(data, pos, cbuf, off, n);
            pos += n;
            return n;
        }

        @Override
        public void close() {
        }
    }

    /** A reader that delivers at most {@code chunk} characters per read and never supports mark. */
    private static final class ChunkyReader extends Reader {
        private final String text;
        private final int chunk;
        private int pos = 0;

        ChunkyReader(final String text, final int chunk) {
            this.text = text;
            this.chunk = chunk;
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            if (pos >= text.length()) {
                return -1;
            }

            final int n = Math.min(Math.min(chunk, len), text.length() - pos);
            text.getChars(pos, pos + n, cbuf, off);
            pos += n;
            return n;
        }

        @Override
        public void close() {
        }
    }

    private static Field ioField(final String name) throws NoSuchFieldException {
        final Field f = IOUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-601: readAllToString(InputStream) reads bytes (a first zero is retried); the decoded twins throw
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC601_readAllToStringToleratesAFirstZeroWhereTheDecodedTwinsThrow() {
        final byte[] data = "hello\nworld".getBytes(StandardCharsets.UTF_8);

        assertEquals("hello\nworld", IOUtil.readAllToString(new ScriptedInputStream(data, 0, 11, -1)));
        assertEquals("hello\nworld", IOUtil.readAllToString(new ScriptedInputStream(data, 0, 11, -1), StandardCharsets.UTF_8));

        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllChars(new ScriptedInputStream(data, 0, 11, -1)));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readChars(new ScriptedInputStream(data, 0, 11, -1), 0, 5));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readToString(new ScriptedInputStream(data, 0, 11, -1), 0, 5));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllLines(new ScriptedInputStream(data, 0, 11, -1)));

        // A well-behaved stream reads identically through all of them.
        assertEquals("hello\nworld", IOUtil.readAllToString(new ByteArrayInputStream(data)));
        assertEquals("hello\nworld", new String(IOUtil.readAllChars(new ByteArrayInputStream(data))));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-602: the locked section consults the cached host name (no-backoff race)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC602_nameCachedByAPeerWithoutBackoffIsReturnedWithoutANewLookup() throws Exception {
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
                retryField.setLong(null, 0L); // NO backoff: the branch C-042 did not cover

                caller = new Thread(() -> result.set(IOUtil.getHostName()));
                caller.start();

                final long deadline = System.currentTimeMillis() + 10_000;

                while (caller.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
                    Thread.onSpinWait();
                }

                assertEquals(Thread.State.BLOCKED, caller.getState());

                hostNameField.set(null, "cached-by-peer-C602");
            } catch (final Throwable t) {
                hostNameField.set(null, savedName);
                futureField.set(null, savedFuture);
                retryField.setLong(null, savedRetry);
                throw t;
            }
        }

        try {
            caller.join(10_000);
            assertEquals("cached-by-peer-C602", result.get());

            synchronized (lock) {
                // no lookup was started for an answer already in hand
                assertNull(futureField.get(null));
            }
        } finally {
            synchronized (lock) {
                hostNameField.set(null, savedName);
                futureField.set(null, savedFuture);
                retryField.setLong(null, savedRetry);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-603: readToString(Reader, offset, maxLength) rewritten - exactness fuzz
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC603_readToStringReaderSliceIsExactAndLeavesTheReaderAfterTheSlice() throws IOException {
        final Random rnd = new Random(603);
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 40_000; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
        }

        // a supplementary character and a lone surrogate somewhere in the middle
        sb.setCharAt(20_000, '\uD83D');
        sb.setCharAt(20_001, '\uDE00');
        sb.setCharAt(30_000, '\uD800');

        final String text = sb.toString();

        for (int i = 0; i < 3_000; i++) {
            final int offset = rnd.nextInt(text.length() + 10);
            final int maxLength = rnd.nextInt(text.length() + 10);
            final int from = Math.min(offset, text.length());
            final int to = Math.min(from + maxLength, text.length());
            final String expected = maxLength == 0 || offset >= text.length() ? "" : text.substring(from, to);

            final Reader plain = new StringReader(text);
            assertEquals(expected, IOUtil.readToString(plain, offset, maxLength));

            if (maxLength > 0 && offset < text.length()) {
                assertEquals(text.substring(to), IOUtil.readAllToString(plain), "reader position after the slice (StringReader)");
            }

            final Reader chunky = new ChunkyReader(text, 1 + rnd.nextInt(700));
            assertEquals(expected, IOUtil.readToString(chunky, offset, maxLength));

            if (maxLength > 0 && offset < text.length()) {
                assertEquals(text.substring(to), IOUtil.readAllToString(chunky), "reader position after the slice (chunky reader)");
            }
        }

        // a large slice crossing the pooled buffer several times
        assertEquals(text.substring(5, 5 + 39_000), IOUtil.readToString(new ChunkyReader(text, 4_099), 5, 39_000));
    }

    @Test
    public void testC603_readToStringReaderMaxLengthZeroDoesNotTouchTheReader() {
        final ScriptedReader reader = new ScriptedReader("abc", 3, -1);
        assertEquals("", IOUtil.readToString(reader, 100, 0));
        assertEquals(0, reader.reads);

        assertEquals("", IOUtil.readToString(new StringReader("abc"), 3, 5), "offset == length");
        assertEquals("", IOUtil.readToString(new StringReader("abc"), 10, 5), "offset beyond the end");
        assertEquals("abc", IOUtil.readToString(new ScriptedReader("abc", 0, 3, -1), 0, 10), "a first zero is retried");
        assertEquals("", IOUtil.readToString(new StringReader(""), 0, 10));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readToString((Reader) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readToString(new StringReader("a"), -1, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readToString(new StringReader("a"), 0, -1));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-604 / C-605: readLines slicer docs; the unchecked read family wraps FileNotFoundException
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC604_readLinesCountZeroAndOffsetBeyondTheEnd() throws IOException {
        final File f = newFile("c604.txt", "a\nb\nc\n");

        assertEquals(Collections.emptyList(), IOUtil.readLines(f, 0, 0));
        assertEquals(Collections.emptyList(), IOUtil.readLines(f, StandardCharsets.UTF_8, 5, 10));
        assertEquals(Collections.emptyList(), IOUtil.readLines(new ByteArrayInputStream("a\nb\n".getBytes()), 5, 10));

        final ScriptedInputStream untouched = new ScriptedInputStream("a\nb\n".getBytes(), 4, -1);
        assertEquals(Collections.emptyList(), IOUtil.readLines(untouched, 1, 0));
        assertEquals(0, untouched.reads, "count 0 does not read the stream");

        // count 0 still validates the file
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines(tempDir.toFile(), 0, 0));
        assertThrows(UncheckedIOException.class, () -> IOUtil.readLines(tempDir.resolve("missing").toFile(), 0, 0));
    }

    @Test
    public void testC605_uncheckedReadFamilyWrapsFileNotFoundException() {
        final File missing = tempDir.resolve("missing-c605.txt").toFile();

        final UncheckedIOException e1 = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllLines(missing));
        assertTrue(e1.getCause() instanceof FileNotFoundException);
        final UncheckedIOException e2 = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllBytes(missing));
        assertTrue(e2.getCause() instanceof FileNotFoundException);
        final UncheckedIOException e3 = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllToString(missing));
        assertTrue(e3.getCause() instanceof FileNotFoundException);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-607: read(source, buf, offset, length): a first zero whose retry hits EOF is -1
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC607_readStreamZeroThenEofIsEof() throws IOException {
        final byte[] data = "abcdef".getBytes(StandardCharsets.UTF_8);
        final byte[] buf = new byte[10];

        assertEquals(-1, IOUtil.read(new ScriptedInputStream(data, 0, -1), buf), "[0, EOF] -> -1");
        assertEquals(-1, IOUtil.read(new ScriptedInputStream(data, 0, -1), buf, 2, 5));
        assertEquals(0, IOUtil.read(new ScriptedInputStream(data, 0, 0), buf), "[0, 0] -> 0 (no progress, not the end)");
        assertEquals(-1, IOUtil.read(new ScriptedInputStream(data, -1), buf), "[EOF] -> -1");

        final byte[] out = new byte[10];
        assertEquals(6, IOUtil.read(new ScriptedInputStream(data, 0, 6, -1), out), "[0, data] -> data");
        assertArrayEquals(data, Arrays.copyOf(out, 6));

        assertEquals(3, IOUtil.read(new ScriptedInputStream(data, 3, -1), out, 0, 10), "short data then EOF -> the count, not -1");

        final ScriptedInputStream untouched = new ScriptedInputStream(data, 6, -1);
        assertEquals(0, IOUtil.read(untouched, buf, 0, 0), "length 0 -> 0");
        assertEquals(0, untouched.reads, "length 0 does not read");

        // the idiomatic loop terminates on a source that answers zero once per call before EOF
        final ScriptedInputStream spinner = new ScriptedInputStream(data, 0, -1, 0, -1, 0, -1);
        int n;
        int iterations = 0;

        while ((n = IOUtil.read(spinner, buf)) != -1) {
            assertTrue(n >= 0);
            iterations++;
            assertTrue(iterations < 100, "loop must terminate");
        }

        assertEquals(0, iterations);
    }

    @Test
    public void testC607_readReaderZeroThenEofIsEof() throws IOException {
        final char[] buf = new char[10];

        assertEquals(-1, IOUtil.read(new ScriptedReader("abcdef", 0, -1), buf), "[0, EOF] -> -1");
        assertEquals(-1, IOUtil.read(new ScriptedReader("abcdef", 0, -1), buf, 1, 4));
        assertEquals(0, IOUtil.read(new ScriptedReader("abcdef", 0, 0), buf), "[0, 0] -> 0");
        assertEquals(-1, IOUtil.read(new ScriptedReader("abcdef", -1), buf), "[EOF] -> -1");

        final char[] out = new char[10];
        assertEquals(6, IOUtil.read(new ScriptedReader("abcdef", 0, 6, -1), out), "[0, data] -> data");
        assertEquals("abcdef", new String(out, 0, 6));

        final ScriptedReader untouched = new ScriptedReader("abcdef", 6, -1);
        assertEquals(0, IOUtil.read(untouched, buf, 0, 0));
        assertEquals(0, untouched.reads);

        // Unicode: a surrogate pair split across two reads still arrives whole
        assertEquals(2, IOUtil.read(new ScriptedReader("\uD83D\uDE00", 1, 1, -1), out, 0, 2));
        assertEquals("\uD83D\uDE00", new String(out, 0, 2));

        // File forms delegate to the same core
        final File f = newFile("c607.txt", "xyz");
        assertEquals(3, IOUtil.read(f, out, 0, 10));
        assertEquals(0, IOUtil.read(f, out, 0, 0));
    }

    @Test
    public void testC607_readAllBytesAndReadBytesOnZeroThenEofStillReturnEmpty() {
        assertEquals(0, IOUtil.readAllBytes(new ScriptedInputStream(new byte[0], 0, -1)).length);
        assertEquals(0, IOUtil.readBytes(new ScriptedInputStream(new byte[0], 0, -1), 0, 10).length);
        assertEquals(0, IOUtil.readChars(new ScriptedReader("", 0, -1), 0, 10).length);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-608: drainPooledWriter drains without flushing the destination
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC608_writeLinesWithoutFlushStillDeliversTheLinesToAPlainWriter() throws IOException {
        final AtomicInteger flushes = new AtomicInteger();
        final StringWriter sink = new StringWriter();
        final Writer plain = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) {
                sink.write(cbuf, off, len);
            }

            @Override
            public void flush() {
                flushes.incrementAndGet();
            }

            @Override
            public void close() {
            }
        };

        IOUtil.writeLines(Arrays.asList("a", "b"), plain, false);
        assertEquals("a\nb\n", sink.toString());
        assertEquals(0, flushes.get(), "drained, not flushed");

        IOUtil.writeLines(Arrays.asList("c").iterator(), plain, true);
        assertEquals("a\nb\nc\n", sink.toString());
        assertEquals(1, flushes.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-609 / C-627 / C-635: renamed parameters appear in the validation messages
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC627_C635_destinationParameterNamesInMessages() throws IOException {
        final File src = newFile("c627.txt", "x");

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, (File) null));
        assertTrue(e1.getMessage().contains("destinationFile"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Arrays.asList(src), (File) null));
        assertTrue(e2.getMessage().contains("destinationFile"), e2.getMessage());
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(src, null));
        assertTrue(e3.getMessage().toLowerCase().contains("destination"), e3.getMessage());
    }

    @Test
    public void testC609_writeObjectAndCharSequenceOverloadsStillResolve() throws IOException {
        final StringWriter sw = new StringWriter();
        IOUtil.write((Object) null, sw);
        IOUtil.write((CharSequence) null, sw);
        IOUtil.write("x", sw, true);
        IOUtil.writeLine((Object) 1, sw);
        assertEquals("nullnullx1\n", sw.toString());

        final File f = tempDir.resolve("c609.txt").toFile();
        IOUtil.write((CharSequence) null, f);
        assertEquals("", read(f));
        IOUtil.append("ab", f);
        IOUtil.append((CharSequence) null, StandardCharsets.UTF_8, f);
        assertEquals("ab", read(f));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-606 / C-610: a decode-only charset is refused before the file is created or truncated (12 + 6 entry points)
    // ------------------------------------------------------------------------------------------------------------

    private interface FileAction {
        void run(File target) throws Exception;
    }

    private void assertDecodeOnlyRefused(final String name, final FileAction action) throws Exception {
        assumeTrue(!DECODE_ONLY.canEncode());

        final File existing = newFile("c606/" + name + "/existing.txt", "KEEP ME");
        assertThrows(UnsupportedOperationException.class, () -> action.run(existing), name + " on an existing file");
        assertEquals("KEEP ME", read(existing), name + ": content unchanged");
        assertTrue(existing.delete(), name + ": no leaked handle (deletable without a GC)");

        final File missing = tempDir.resolve("c606/" + name + "/missing.txt").toFile();
        assertThrows(UnsupportedOperationException.class, () -> action.run(missing), name + " on a missing file");
        assertFalse(missing.exists(), name + ": missing file is not created");

        // a directory target: validation before I/O, so the charset is reported first
        final File dir = newDir("c606/" + name + "/dir");
        assertThrows(UnsupportedOperationException.class, () -> action.run(dir), name + " on a directory");
    }

    @Test
    public void testC606_writeLinesDecodeOnlyCharsetRefusedBeforeTheFileIsTouched() throws Exception {
        assertDecodeOnlyRefused("writeLinesIterable", f -> IOUtil.writeLines(Arrays.asList("x", "y"), DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLinesIterator", f -> IOUtil.writeLines(Arrays.asList("x", "y").iterator(), DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLinesEmptyIterable", f -> IOUtil.writeLines(Collections.emptyList(), DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLinesEmptyIterator", f -> IOUtil.writeLines(Collections.emptyIterator(), DECODE_ONLY, f));
    }

    @Test
    public void testC606_writeAndAppendReaderDecodeOnlyCharsetRefused() throws Exception {
        assertDecodeOnlyRefused("writeReader", f -> IOUtil.write(new StringReader("x"), DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeReaderSlice", f -> IOUtil.write(new StringReader("xyz"), 1, 1, DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendReader", f -> IOUtil.append(new StringReader("x"), DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendReaderSlice", f -> IOUtil.append(new StringReader("xyz"), 1, 1, DECODE_ONLY, f));
    }

    @Test
    public void testC606_appendLinesDecodeOnlyCharsetRefused() throws Exception {
        assertDecodeOnlyRefused("appendLinesIterable", f -> IOUtil.appendLines(Arrays.asList("x"), DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendLinesIterator", f -> IOUtil.appendLines(Arrays.asList("x").iterator(), DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendLinesEmptyIterable", f -> IOUtil.appendLines(Collections.emptyList(), DECODE_ONLY, f));
        assertDecodeOnlyRefused("appendLinesEmptyIterator", f -> IOUtil.appendLines(Collections.emptyIterator(), DECODE_ONLY, f));
    }

    @Test
    public void testC606_writerFactoriesDecodeOnlyCharsetRefused() throws Exception {
        assertDecodeOnlyRefused("newFileWriter", f -> IOUtil.newFileWriter(f, DECODE_ONLY).close());
        assertDecodeOnlyRefused("newFileWriterAppend", f -> IOUtil.newFileWriter(f, DECODE_ONLY, true).close());
        assertDecodeOnlyRefused("newBufferedWriter", f -> IOUtil.newBufferedWriter(f, DECODE_ONLY).close());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter(null, DECODE_ONLY));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter(null, DECODE_ONLY, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((File) null, DECODE_ONLY));
    }

    @Test
    public void testC610_encodeFirstFileWritersDecodeOnlyCharsetRefused() throws Exception {
        assertDecodeOnlyRefused("writeCharSequence", f -> IOUtil.write("x", DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeChars", f -> IOUtil.write(new char[] { 'x' }, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeCharsSlice", f -> IOUtil.write(new char[] { 'x', 'y' }, 0, 1, DECODE_ONLY, f));
        assertDecodeOnlyRefused("writeLine", f -> IOUtil.writeLine("x", DECODE_ONLY, f));
    }

    @Test
    public void testC606_nullAndUtf8CharsetsStillWrite() throws IOException {
        final File f = tempDir.resolve("c606-ok.txt").toFile();

        IOUtil.writeLines(Arrays.asList("a", "b"), null, f);
        assertEquals("a\nb\n", read(f));
        IOUtil.writeLines(Arrays.asList("c").iterator(), StandardCharsets.UTF_8, f);
        assertEquals("c\n", read(f));
        IOUtil.write(new StringReader("\u00e9"), StandardCharsets.UTF_8, f);
        assertEquals("\u00e9", read(f));
        IOUtil.append(new StringReader("x"), (Charset) null, f);
        assertEquals("\u00e9x", read(f));
        IOUtil.appendLines(Arrays.asList("y"), StandardCharsets.UTF_8, f);
        assertEquals("\u00e9x" + "y\n", read(f));

        try (Writer w = IOUtil.newFileWriter(f, (Charset) null)) {
            w.write("z");
        }

        assertEquals("z", read(f));

        try (Writer w = IOUtil.newBufferedWriter(f, StandardCharsets.UTF_8)) {
            w.write("w");
        }

        assertEquals("w", read(f));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-611 / C-638: FileNotFoundException on the File copiers and unzip
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC611_C638_missingSourceIsFileNotFoundException() throws IOException {
        final File missing = tempDir.resolve("missing-c611").toFile();
        final File out = tempDir.resolve("out-c611").toFile();

        assertThrows(FileNotFoundException.class, () -> IOUtil.write(missing, new ByteArrayOutputStream()));
        assertThrows(FileNotFoundException.class, () -> IOUtil.write(missing, 0, 1, new ByteArrayOutputStream(), true));
        assertThrows(FileNotFoundException.class, () -> IOUtil.write(missing, out));
        assertFalse(out.exists());
        assertThrows(FileNotFoundException.class, () -> IOUtil.unzip(missing, newDir("c638")));
        assertThrows(FileNotFoundException.class, () -> IOUtil.unzip(missing, newDir("c638b"), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-612: the seek fast path is verified
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC612_regularFileStillSeeksAndStaysExact() throws IOException {
        final byte[] data = randomBytes(100_000, 612);
        final File f = tempDir.resolve("c612.bin").toFile();
        Files.write(f.toPath(), data);

        try (FileInputStream in = new FileInputStream(f)) {
            assertEquals(70_000, IOUtil.skip(in, 70_000));
            assertEquals(30_000, in.available(), "on the seek path: available() == size - n");
            assertEquals(70_000, in.getChannel().position());
            assertArrayEquals(Arrays.copyOfRange(data, 70_000, 100_000), IOUtil.readAllBytes(in));
        }

        try (FileInputStream in = new FileInputStream(f)) {
            assertEquals(100_000, IOUtil.skip(in, Long.MAX_VALUE), "capped at the end");
            assertEquals(-1, in.read());
        }

        try (FileInputStream in = new FileInputStream(f)) {
            assertArrayEquals(Arrays.copyOfRange(data, 12_345, 12_345 + 777), IOUtil.readBytes(in, 12_345, 777));
        }
    }

    @Test
    public void testC612_aFileInputStreamWhoseAvailableLiesTakesTheReadPathAndStaysExact() throws IOException {
        final byte[] data = randomBytes(50_000, 6120);
        final File f = tempDir.resolve("c612-liar.bin").toFile();
        Files.write(f.toPath(), data);

        try (FileInputStream in = new FileInputStream(f) {
            @Override
            public int available() {
                return 7; // never the arithmetic of a regular file
            }
        }) {
            assertEquals(20_000, IOUtil.skip(in, 20_000));
            assertEquals(20_000, in.getChannel().position(), "read path advanced the stream for real");
            assertArrayEquals(Arrays.copyOfRange(data, 20_000, 50_000), IOUtil.readAllBytes(in));
        }
    }

    /** Launches a child JVM running {@link #main(String[])} with the given arguments; stdin and stdout are pipes. */
    private Process launchChild(final String... args) throws IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File argFile = tempDir.resolve("child-" + System.nanoTime() + ".args").toFile();
        final StringBuilder sb = new StringBuilder();
        sb.append("-cp\n\"").append(System.getProperty("java.class.path").replace("\\", "\\\\")).append("\"\n");
        sb.append(IOUtilReview20260925bTest.class.getName()).append('\n');

        for (final String arg : args) {
            sb.append('"').append(arg.replace("\\", "\\\\")).append("\"\n");
        }

        Files.write(argFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));

        return new ProcessBuilder(javaBin, "@" + argFile.getAbsolutePath()).start();
    }

    private static byte[] runChild(final Process child, final byte[] stdin, final long pauseAfter, final long pauseMillis) throws Exception {
        final Thread feeder = new Thread(() -> {
            try (OutputStream os = child.getOutputStream()) {
                if (pauseAfter > 0 && pauseAfter < stdin.length) {
                    os.write(stdin, 0, (int) pauseAfter);
                    os.flush();
                    Thread.sleep(pauseMillis);
                    os.write(stdin, (int) pauseAfter, stdin.length - (int) pauseAfter);
                } else {
                    os.write(stdin);
                }
            } catch (final Exception e) {
                // the child may have exited; the assertions below report it
            }
        }, "c612-feeder");
        feeder.setDaemon(true);
        feeder.start();

        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final Thread drainer = new Thread(() -> {
            try (InputStream es = child.getErrorStream()) {
                es.transferTo(err);
            } catch (final IOException e) {
                // ignore
            }
        }, "c612-stderr");
        drainer.setDaemon(true);
        drainer.start();

        final byte[] out;

        try (InputStream is = child.getInputStream()) {
            out = is.readAllBytes();
        }

        if (!child.waitFor(30, TimeUnit.SECONDS)) {
            child.destroyForcibly();
            throw new AssertionError("child JVM timed out; stderr: " + err.toString(StandardCharsets.UTF_8));
        }

        assertEquals(0, child.exitValue(), "child exit code; stderr: " + err.toString(StandardCharsets.UTF_8));
        return out;
    }

    /** Child entry point for the pipe tests. Writes results as raw bytes to stdout, counts to stderr. */
    public static void main(final String[] args) throws Exception {
        final FileInputStream stdin = new FileInputStream(FileDescriptor.in);
        final FileOutputStream stdout = new FileOutputStream(FileDescriptor.out);

        switch (args[0]) {
            case "skip": {
                final long skipped = IOUtil.skip(stdin, 3);
                final byte[] rest = IOUtil.readAllBytes(stdin);
                stdout.write(rest);
                System.err.println("skipped=" + skipped);
                break;
            }
            case "readBytes": {
                stdout.write(IOUtil.readBytes(stdin, 3, 5));
                break;
            }
            case "write": {
                final long n = IOUtil.write(stdin, 3, 5, stdout, true);
                System.err.println("written=" + n);
                break;
            }
            case "transfer-in": {
                final File target = new File(args[1]);

                try (FileChannel dest = FileChannel.open(target.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    final long n = IOUtil.transfer(stdin.getChannel(), dest);
                    stdout.write(String.valueOf(n).getBytes(StandardCharsets.UTF_8));
                }

                break;
            }
            case "transfer-out": {
                try (FileChannel src = FileChannel.open(new File(args[1]).toPath(), StandardOpenOption.READ)) {
                    final long n = IOUtil.transfer(src, stdout.getChannel());
                    System.err.println("count=" + n);
                }

                break;
            }
            default:
                throw new IllegalArgumentException(args[0]);
        }

        stdout.flush();
        System.err.flush();
        System.exit(0);
    }

    @Test
    public void testC612_pipedStdinIsSkippedByReadingOnWindows() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "the phantom seek is a Windows pipe behaviour");

        final byte[] input = "hello world\n".getBytes(StandardCharsets.US_ASCII);

        assertEquals("lo world\n", new String(runChild(launchChild("skip"), input, 0, 0), StandardCharsets.US_ASCII));
        assertEquals("lo wo", new String(runChild(launchChild("readBytes"), input, 0, 0), StandardCharsets.US_ASCII));
        assertEquals("lo wo", new String(runChild(launchChild("write"), input, 0, 0), StandardCharsets.US_ASCII));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-613: transfer proves both channels before taking the sized path
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC613_pipedStdinChannelIsTransferredToEndOfInputOnWindows() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "the phantom size is a Windows pipe behaviour");

        final byte[] payload = randomBytes(131 * 1024, 613);

        // (a) everything written up front
        final File target1 = tempDir.resolve("c613-a.bin").toFile();
        final byte[] out1 = runChild(launchChild("transfer-in", target1.getAbsolutePath()), payload, 0, 0);
        assertEquals(String.valueOf(payload.length), new String(out1, StandardCharsets.US_ASCII));
        assertArrayEquals(payload, Files.readAllBytes(target1.toPath()));

        // (b) 1,000 bytes, a pause, the rest
        final File target2 = tempDir.resolve("c613-b.bin").toFile();
        final byte[] out2 = runChild(launchChild("transfer-in", target2.getAbsolutePath()), payload, 1_000, 1_000);
        assertEquals(String.valueOf(payload.length), new String(out2, StandardCharsets.US_ASCII));
        assertArrayEquals(payload, Files.readAllBytes(target2.toPath()));
    }

    @Test
    public void testC613_pipedStdoutChannelDestinationTakesTheBufferedPath() throws Exception {
        final byte[] payload = randomBytes(200_000, 6130);
        final File source = tempDir.resolve("c613-src.bin").toFile();
        Files.write(source.toPath(), payload);

        final byte[] out = runChild(launchChild("transfer-out", source.getAbsolutePath()), new byte[0], 0, 0);
        assertArrayEquals(payload, out);
    }

    /** A FileChannel that delegates everything to a real one, with hooks for the position probes. */
    private static class DelegatingFileChannel extends FileChannel {
        final FileChannel delegate;

        DelegatingFileChannel(final FileChannel delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            return delegate.read(dst);
        }

        @Override
        public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            return delegate.write(src);
        }

        @Override
        public long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
            return delegate.write(srcs, offset, length);
        }

        @Override
        public long position() throws IOException {
            return delegate.position();
        }

        @Override
        public FileChannel position(final long newPosition) throws IOException {
            delegate.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return delegate.size();
        }

        @Override
        public FileChannel truncate(final long size) throws IOException {
            delegate.truncate(size);
            return this;
        }

        @Override
        public void force(final boolean metaData) throws IOException {
            delegate.force(metaData);
        }

        @Override
        public long transferTo(final long position, final long count, final WritableByteChannel target) throws IOException {
            return delegate.transferTo(position, count, target);
        }

        @Override
        public long transferFrom(final ReadableByteChannel src, final long position, final long count) throws IOException {
            return delegate.transferFrom(src, position, count);
        }

        @Override
        public int read(final ByteBuffer dst, final long position) throws IOException {
            return delegate.read(dst, position);
        }

        @Override
        public int write(final ByteBuffer src, final long position) throws IOException {
            return delegate.write(src, position);
        }

        @Override
        public MappedByteBuffer map(final MapMode mode, final long position, final long size) throws IOException {
            return delegate.map(mode, position, size);
        }

        @Override
        public FileLock lock(final long position, final long size, final boolean shared) throws IOException {
            return delegate.lock(position, size, shared);
        }

        @Override
        public FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {
            return delegate.tryLock(position, size, shared);
        }

        @Override
        protected void implCloseChannel() throws IOException {
            delegate.close();
        }
    }

    @Test
    public void testC613_sourceWhosePositionDoesNotFollowItsReadsTakesTheBufferedPath() throws IOException {
        final byte[] payload = randomBytes(300_000, 6131);
        final File source = tempDir.resolve("c613-pos.bin").toFile();
        final File target = tempDir.resolve("c613-pos-out.bin").toFile();
        Files.write(source.toPath(), payload);

        try (FileChannel real = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel dest = FileChannel.open(target.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileChannel stuck = new DelegatingFileChannel(real) {
                @Override
                public long position() {
                    return 0; // a Windows pipe: the position never follows a read
                }
            };

            assertEquals(payload.length, IOUtil.transfer(stuck, dest));
        }

        assertArrayEquals(payload, Files.readAllBytes(target.toPath()));
    }

    @Test
    public void testC613_positionThatThrowsTakesTheBufferedPathButAClosedChannelPropagates() throws IOException {
        final byte[] payload = randomBytes(100_000, 6132);
        final File source = tempDir.resolve("c613-throw.bin").toFile();
        final File target = tempDir.resolve("c613-throw-out.bin").toFile();
        Files.write(source.toPath(), payload);

        // destination that cannot answer position(): a pipe-backed channel on any platform
        try (FileChannel real = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel realDest = FileChannel.open(target.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileChannel pipeLike = new DelegatingFileChannel(realDest) {
                @Override
                public long position() throws IOException {
                    throw new IOException("Illegal seek");
                }
            };

            assertEquals(payload.length, IOUtil.transfer(real, pipeLike));
        }

        assertArrayEquals(payload, Files.readAllBytes(target.toPath()));

        // source that cannot answer position(): buffered path too
        final File target2 = tempDir.resolve("c613-throw-out2.bin").toFile();

        try (FileChannel real = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel dest = FileChannel.open(target2.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final FileChannel pipeLike = new DelegatingFileChannel(real) {
                @Override
                public long position() throws IOException {
                    throw new IOException("Illegal seek");
                }
            };

            assertEquals(payload.length, IOUtil.transfer(pipeLike, dest));
        }

        assertArrayEquals(payload, Files.readAllBytes(target2.toPath()));

        // a closed channel is reported as such
        try (FileChannel real = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel dest = FileChannel.open(target2.toPath(), StandardOpenOption.WRITE)) {
            final FileChannel closed = new DelegatingFileChannel(dest) {
                @Override
                public long position() throws IOException {
                    throw new ClosedChannelException();
                }
            };

            assertThrows(ClosedChannelException.class, () -> IOUtil.transfer(real, closed));
        }
    }

    @Test
    public void testC613_readOnlyDestinationAndEmptyRemainderPins() throws IOException {
        final byte[] payload = randomBytes(10_000, 6133);
        final File source = tempDir.resolve("c613-ro-src.bin").toFile();
        final File target = tempDir.resolve("c613-ro-dst.bin").toFile();
        Files.write(source.toPath(), payload);
        Files.write(target.toPath(), new byte[] { 1, 2, 3 });

        // (4) read-only destination: NonWritableChannelException before any source byte is consumed
        try (FileChannel in = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel readOnly = FileChannel.open(target.toPath(), StandardOpenOption.READ)) {
            in.position(10);
            assertThrows(NonWritableChannelException.class, () -> IOUtil.transfer(in, readOnly));
            assertEquals(10, in.position(), "source position unchanged");
        }

        // (5) source position == size (> 0): 0, destination untouched
        try (FileChannel in = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel dest = FileChannel.open(target.toPath(), StandardOpenOption.WRITE)) {
            in.position(in.size());
            assertEquals(0, IOUtil.transfer(in, dest));
            assertEquals(0, dest.position());
        }

        assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(target.toPath()));

        // (6) regular files: exact, both positions honoured
        try (FileChannel in = FileChannel.open(source.toPath(), StandardOpenOption.READ);
                FileChannel dest = FileChannel.open(target.toPath(), StandardOpenOption.WRITE)) {
            in.position(100);
            dest.position(3);
            assertEquals(payload.length - 100, IOUtil.transfer(in, dest));
            assertEquals(3 + payload.length - 100, dest.position());
        }

        final byte[] written = Files.readAllBytes(target.toPath());
        assertArrayEquals(new byte[] { 1, 2, 3 }, Arrays.copyOf(written, 3));
        assertArrayEquals(Arrays.copyOfRange(payload, 100, payload.length), Arrays.copyOfRange(written, 3, written.length));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-614: getFileExtension(String) ignores trailing separators, as the File twin does
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC614_getFileExtensionStringAgreesWithTheFileTwinOnTrailingSeparators() {
        for (final String name : new String[] { "a/b.c/", "a/b.c\\", "a/b.c//", "dir/", "/", "", "a/b.c", "x.tar.gz/", ".bashrc/", "a.", "a./",
                "caf\u00e9.t\u00fcxt/" }) {
            assertEquals(IOUtil.getFileExtension(new File(name)), IOUtil.getFileExtension(name), "input: " + name);
        }

        assertEquals("c", IOUtil.getFileExtension("a/b.c/"));
        assertEquals("c", IOUtil.getFileExtension("a/b.c\\"));
        assertEquals("", IOUtil.getFileExtension("dir/"));
        assertEquals("", IOUtil.getFileExtension("/"));
        assertEquals("", IOUtil.getFileExtension(""));
        assertNull(IOUtil.getFileExtension((String) null));
        assertNull(IOUtil.getFileExtension((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.getFileExtension("a\u0000b.txt/"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-616 / C-617: appendLines short-circuit; appendLine validates the target first
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC616_appendLinesIterableEmptyStillCreatesAndValidates() throws IOException {
        final File f = tempDir.resolve("c616.txt").toFile();
        IOUtil.appendLines(Collections.emptyList(), f);
        assertTrue(f.isFile());
        assertEquals(0, f.length());

        IOUtil.appendLines(Arrays.asList("a"), f);
        IOUtil.appendLines((Iterable<?>) null, f);
        assertEquals("a\n", read(f));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.emptyList(), tempDir.toFile()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.emptyList(), (File) null));
    }

    @Test
    public void testC617_appendLineReportsANullTargetBeforeRenderingTheObject() {
        final Object explosive = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("toString must not run");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine(explosive, StandardCharsets.UTF_8, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine(explosive, (File) null));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-620: doCopyFile compares against the length snapshot (a source under append copies as a snapshot)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC620_copyToDirectoryAndCopyDirectoryOfAGrowingFileSucceed() throws Exception {
        final int initial = 17 * 1024 * 1024;
        final File srcDir = newDir("c620/src");
        final File live = new File(srcDir, "live.log");
        Files.write(live.toPath(), randomBytes(initial, 620));

        final AtomicBoolean stop = new AtomicBoolean();
        final byte[] chunk = randomBytes(4096, 6201);
        final Thread appender = new Thread(() -> {
            try (OutputStream os = new FileOutputStream(live, true)) {
                while (!stop.get()) {
                    os.write(chunk);
                    os.flush();
                    Thread.sleep(1);
                }
            } catch (final Exception e) {
                // the test's assertions report the outcome
            }
        }, "c620-appender");
        appender.setDaemon(true);
        appender.start();

        try {
            final File backup = newDir("c620/backup");
            final File copy = IOUtil.copyToDirectory(live, backup);
            final File backup2 = newDir("c620/backup2");
            IOUtil.copyDirectory(srcDir, backup2);
            final File copy2 = new File(backup2, "live.log");

            stop.set(true);
            appender.join(10_000);

            for (final File c : new File[] { copy, copy2 }) {
                assertTrue(c.isFile());
                assertTrue(c.length() >= initial, "copy holds at least the initial length: " + c.length());
                assertTrue(c.length() <= live.length(), "copy never exceeds the source");

                try (InputStream src = new FileInputStream(live); InputStream cp = new FileInputStream(c)) {
                    final byte[] prefix = IOUtil.readBytes(src, 0, (int) c.length());
                    assertArrayEquals(prefix, IOUtil.readAllBytes(cp), "prefix bytes equal");
                }
            }
        } finally {
            stop.set(true);
            appender.join(10_000);
        }
    }

    @Test
    public void testC620_stableSizesCopyByteIdentically() throws IOException {
        final int eightMiB = 8 * 1024 * 1024;

        for (final int size : new int[] { 0, 1, eightMiB - 1, eightMiB, eightMiB + 1 }) {
            final byte[] data = randomBytes(size, size);
            final File src = tempDir.resolve("c620-stable/src-" + size + ".bin").toFile();
            src.getParentFile().mkdirs();
            Files.write(src.toPath(), data);
            final File dest = newDir("c620-stable/dest-" + size);
            final File copy = IOUtil.copyToDirectory(src, dest);
            assertArrayEquals(data, Files.readAllBytes(copy.toPath()), "size " + size);
        }

        // an existing destination is still refused, untouched
        final File src = newFile("c620-exists/src.txt", "new");
        final File dest = newDir("c620-exists/dest");
        final File existing = newFile("c620-exists/dest/src.txt", "old");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertEquals("old", read(existing));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-621 / C-622: moveToDirectory refuses an existing target without REPLACE_EXISTING (ATOMIC_MOVE included) and
    // a hard-link alias
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC621_atomicMoveWithoutReplaceExistingRefusesAnExistingFile() throws IOException {
        final File src = newFile("c621/s.txt", "moved-in");
        final File dest = newDir("c621/dest");
        final File existing = newFile("c621/dest/s.txt", "previous");

        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, dest, StandardCopyOption.ATOMIC_MOVE));
        assertEquals("previous", read(existing));
        assertTrue(src.exists());

        @SuppressWarnings("deprecation")
        final Runnable deprecated = () -> {
            try {
                IOUtil.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, deprecated::run);
        assertTrue(e.getCause() instanceof FileAlreadyExistsException);
        assertEquals("previous", read(existing));

        // ATOMIC_MOVE + REPLACE_EXISTING still replaces a file
        IOUtil.moveToDirectory(src, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        assertEquals("moved-in", read(existing));
        assertFalse(src.exists());

        // ATOMIC_MOVE into a fresh directory moves
        final File src2 = newFile("c621/s2.txt", "two");
        final File fresh = tempDir.resolve("c621/fresh").toFile();
        IOUtil.moveToDirectory(src2, fresh, StandardCopyOption.ATOMIC_MOVE);
        assertEquals("two", read(new File(fresh, "s2.txt")));
        assertFalse(src2.exists());
    }

    @Test
    public void testC621_directorySourceOntoAnExistingFileOrDirectoryUnderAtomicMoveIsRefused() throws IOException {
        final File srcDir = newDir("c621d/src/sub");
        final File inside = newFile("c621d/src/sub/x.txt", "x");
        final File dest = newDir("c621d/dest");
        final File existingFile = newFile("c621d/dest/sub", "a file called sub");

        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(srcDir, dest, StandardCopyOption.ATOMIC_MOVE));
        assertEquals("a file called sub", read(existingFile));
        assertTrue(inside.exists());

        final File dest2 = newDir("c621d/dest2");
        newDir("c621d/dest2/sub");
        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(srcDir, dest2, StandardCopyOption.ATOMIC_MOVE));
        assertTrue(inside.exists());
        assertTrue(new File(dest2, "sub").isDirectory());
        assertEquals(0, new File(dest2, "sub").list().length);
    }

    @Test
    public void testC621_sameDirectoryNoOpSurvivesUnderAtomicMove() throws IOException {
        final File parent = newDir("c621n/parent");
        final File src = newFile("c621n/parent/f.txt", "same");

        IOUtil.moveToDirectory(src, parent, StandardCopyOption.ATOMIC_MOVE);
        assertEquals("same", read(src));

        IOUtil.moveToDirectory(src, new File(parent, "."), StandardCopyOption.ATOMIC_MOVE);
        assertEquals("same", read(src));

        IOUtil.moveToDirectory(src, parent);
        assertEquals("same", read(src));
    }

    @Test
    public void testC622_hardLinkAliasAtTheTargetIsADifferentEntry() throws IOException {
        final File src = newFile("c622/src.txt", "content");
        final File dest = newDir("c622/dest");
        final Path alias = dest.toPath().resolve("src.txt");

        try {
            Files.createLink(alias, src.toPath());
        } catch (final IOException | UnsupportedOperationException e) {
            assumeTrue(false, "hard links are not available here: " + e);
        }

        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, dest));
        assertTrue(src.exists(), "source still present");
        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, dest, StandardCopyOption.ATOMIC_MOVE));
        assertTrue(src.exists());
        assertEquals("content", read(alias.toFile()));
    }

    @Test
    public void testC621_existingTargetWithANullOptionElement() throws IOException {
        final File src = newFile("c621z/s.txt", "x");
        final File dest = newDir("c621z/dest");
        newFile("c621z/dest/s.txt", "old");

        // the existing target is reported before the JDK sees the null element
        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, dest, new StandardCopyOption[] { null }));
        assertTrue(src.exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-623: an existing same-name directory is merged into and keeps its own times
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC623_copyToDirectoryMergesIntoAnExistingDirectoryWithoutRestampingIt() throws IOException {
        final File srcDir = newDir("c623/src/tree");
        newFile("c623/src/tree/new.txt", "new");
        newDir("c623/src/tree/sub");
        final FileTime sourceStamp = FileTime.fromMillis(946_684_800_000L); // 2000-01-01
        Files.setLastModifiedTime(srcDir.toPath(), sourceStamp);

        final File dest = newDir("c623/dest");
        final File existing = newDir("c623/dest/tree");
        final File old = newFile("c623/dest/tree/old.txt", "old");
        final FileTime destStamp = FileTime.fromMillis(1_262_304_000_000L); // 2010-01-01
        Files.setLastModifiedTime(existing.toPath(), destStamp);

        final File returned = IOUtil.copyToDirectory(srcDir, dest, true);
        assertEquals(existing.getCanonicalFile(), returned.getCanonicalFile());
        assertEquals("old", read(old));
        assertEquals("new", read(new File(existing, "new.txt")));
        assertTrue(new File(existing, "sub").isDirectory());
        assertNotEquals(sourceStamp.toMillis(), existing.lastModified(), "a merged directory is not re-stamped with the source's time");

        // an EMPTY source directory merged into an existing one leaves that directory's time exactly alone
        final File emptySrc = newDir("c623e/src/empty");
        Files.setLastModifiedTime(emptySrc.toPath(), sourceStamp);
        final File dest2 = newDir("c623e/dest");
        final File existing2 = newDir("c623e/dest/empty");
        Files.setLastModifiedTime(existing2.toPath(), destStamp);
        IOUtil.copyToDirectory(emptySrc, dest2, true);
        assertEquals(destStamp.toMillis(), existing2.lastModified());

        // a directory the call CREATED is stamped
        final File dest3 = newDir("c623e/dest3");
        final File created = IOUtil.copyToDirectory(emptySrc, dest3, true);
        assertEquals(sourceStamp.toMillis(), created.lastModified());

        // a file that already exists inside the merged directory is still never overwritten
        newFile("c623/src/tree/old.txt", "clash");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(srcDir, dest, true));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertEquals("old", read(old));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-624: a trailing ".." is rejected by the delete / move family
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC624_trailingDotDotIsRejectedAndNothingIsDeletedOrMoved() throws IOException {
        final File parent = newDir("c624/parent");
        final File child = newDir("c624/parent/child");
        final File sibling = newFile("c624/parent/sibling.txt", "s");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteRecursivelyIfExists(new File(child, "..")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(new File(child, "..")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteRecursivelyIfExists(new File("..")));
        assertTrue(parent.isDirectory());
        assertTrue(child.isDirectory());
        assertEquals("s", read(sibling));

        final File dest = newDir("c624/dest");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(new File(child, ".."), dest));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(new File(child, ".."), dest, StandardCopyOption.REPLACE_EXISTING));
        assertTrue(parent.isDirectory());
        assertEquals(0, dest.list().length);

        // a missing path ending in ".." is a bad argument too, not "absent"
        assertThrows(IllegalArgumentException.class, () -> IOUtil.deleteIfExists(new File(tempDir.toFile(), "missing/..")));

        // a trailing "." still folds
        final File dotChild = new File(child, ".");
        IOUtil.moveToDirectory(dotChild, dest);
        assertTrue(new File(dest, "child").isDirectory());
        assertFalse(child.exists());
        assertTrue(IOUtil.deleteRecursivelyIfExists(new File(new File(dest, "child"), ".")));
        assertFalse(new File(dest, "child").exists());
        assertTrue(IOUtil.deleteIfExists(new File(dest, ".")));
        assertFalse(dest.exists());

        assertFalse(IOUtil.deleteIfExists(null));
        assertFalse(IOUtil.deleteRecursivelyIfExists(null));
        assertFalse(IOUtil.deleteQuietly(new File(child, "..")), "deleteQuietly swallows the rejection");
        assertTrue(parent.isDirectory());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-625: copyFile(src, "newdir/.") creates nothing
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC625_copyFileRejectsADotTerminatedDestinationWithoutCreatingItsParent() throws IOException {
        final File src = newFile("c625/src.txt", "x");
        final File newdir = tempDir.resolve("c625/newdir").toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, new File(newdir, ".")));
        assertFalse(newdir.exists(), "no directory left behind");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, new File(tempDir.toFile(), "c625/p/q/."), true));
        assertFalse(tempDir.resolve("c625/p").toFile().exists());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, new File(tempDir.toFile(), "c625/p/..")));
        assertFalse(tempDir.resolve("c625/p").toFile().exists());

        // an existing directory is still a bad argument, and a plain missing parent is still created
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, tempDir.toFile()));
        final File nested = tempDir.resolve("c625/a/b/copy.txt").toFile();
        IOUtil.copyFile(src, nested);
        assertEquals("x", read(nested));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-626: copyDirectory copies a regular-file child through doCopyFile
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC626_copyDirectoryFileChildKeepsContentAndTimeAndRefusesAnExistingEntry() throws IOException {
        final File srcDir = newDir("c626/src");
        final File f = newFile("c626/src/f.txt", "content");
        final long stamp = 1_262_304_000_000L;
        assertTrue(f.setLastModified(stamp));
        final File dest = newDir("c626/dest");

        IOUtil.copyDirectory(srcDir, dest);
        final File copy = new File(dest, "f.txt");
        assertEquals("content", read(copy));
        assertEquals(stamp, copy.lastModified());

        // an existing dest/<name> regular file: IOException "already exists", untouched
        final File dest2 = newDir("c626/dest2");
        final File existing = newFile("c626/dest2/f.txt", "old");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyDirectory(srcDir, dest2));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertEquals("old", read(existing));

        // never a "Copy of" fallback from copyDirectory
        assertFalse(new File(dest2, "Copy of f.txt").exists());
    }

    @Test
    public void testC626_copyDirectoryFileChildWhoseDestinationIsASymlinkBackToItIsRefused() throws IOException {
        final File srcDir = newDir("c626s/src");
        final File f = newFile("c626s/src/f.txt", "content");
        final File dest = newDir("c626s/dest");
        final Path link = dest.toPath().resolve("f.txt");

        try {
            Files.createSymbolicLink(link, f.toPath());
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "symbolic links cannot be created here: " + e);
        }

        assertThrows(IOException.class, () -> IOUtil.copyDirectory(srcDir, dest));
        assertEquals("content", read(f));
        assertTrue(Files.isSymbolicLink(link));
        assertFalse(new File(dest, "Copy of f.txt").exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-630: a backslash-terminated directory entry is a directory entry
    // ------------------------------------------------------------------------------------------------------------

    private File writeZip(final String name, final String... entries) throws IOException {
        final File zip = tempDir.resolve(name).toFile();
        zip.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (final String entry : entries) {
                zos.putNextEntry(new ZipEntry(entry));

                if (!entry.endsWith("/") && !entry.endsWith("\\")) {
                    zos.write(("payload of " + entry).getBytes(StandardCharsets.UTF_8));
                }

                zos.closeEntry();
            }
        }

        return zip;
    }

    @Test
    public void testC630_backslashTerminatedDirectoryEntriesExtractAsDirectories() throws IOException {
        final File zip1 = writeZip("c630/a.zip", "d\\", "d\\x.txt");
        final File out1 = newDir("c630/out1");
        IOUtil.unzip(zip1, out1);
        assertTrue(new File(out1, "d").isDirectory());
        assertEquals("payload of d\\x.txt", read(new File(new File(out1, "d"), "x.txt")));

        final File zip2 = writeZip("c630/b.zip", "d\\x.txt", "d\\");
        final File out2 = newDir("c630/out2");
        IOUtil.unzip(zip2, out2);
        assertTrue(new File(out2, "d").isDirectory());
        assertEquals("payload of d\\x.txt", read(new File(new File(out2, "d"), "x.txt")));

        final File zip3 = writeZip("c630/c.zip", "d/", "d/x.txt", "e\\f\\", "\u00e9\\");
        final File out3 = newDir("c630/out3");
        IOUtil.unzip(zip3, out3);
        assertTrue(new File(out3, "d").isDirectory());
        assertTrue(new File(out3, "e" + File.separator + "f").isDirectory());
        assertTrue(new File(out3, "\u00e9").isDirectory());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-631: the archive (and a hard-link alias of it) inside the source is still excluded
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC631_archiveAndItsHardLinkAliasInsideTheSourceAreExcluded() throws IOException {
        final File srcDir = newDir("c631/src");
        newFile("c631/src/a.txt", "a");
        newFile("c631/src/sub/b.txt", "b");
        final File target = new File(srcDir, "out.zip");
        Files.write(target.toPath(), new byte[] { 0 });
        final Path alias = srcDir.toPath().resolve("sub/alias.bin");

        try {
            Files.createLink(alias, target.toPath());
        } catch (final IOException | UnsupportedOperationException e) {
            assumeTrue(false, "hard links are not available here: " + e);
        }

        IOUtil.zip(srcDir, target);

        final File out = newDir("c631/out");
        IOUtil.unzip(target, out);
        final File root = new File(out, "src");
        assertEquals("a", read(new File(root, "a.txt")));
        assertEquals("b", read(new File(root, "sub/b.txt")));
        assertFalse(new File(root, "out.zip").exists(), "the archive itself is excluded");
        assertFalse(new File(root, "sub/alias.bin").exists(), "a hard-link alias of the archive is excluded");

        // and a NEW target: the plain-name exclusion still works, other files are archived
        final File fresh = new File(srcDir, "fresh.zip");
        IOUtil.zip(srcDir, fresh);
        final File out2 = newDir("c631/out2");
        IOUtil.unzip(fresh, out2);
        assertTrue(new File(out2, "src/a.txt").isFile());
        assertFalse(new File(out2, "src/fresh.zip").exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-632: a null destination is reported before a missing source
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC632_nullDestinationIsReportedBeforeAMissingSource() throws IOException {
        final File missing = tempDir.resolve("c632/missing").toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(missing, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(missing, null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(missing, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(missing, null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.split(missing, 2, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(missing, 2, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(missing, 2, null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(missing, 2, null, StandardCharsets.UTF_8));

        // a destination that is an existing FILE is a bad argument before the source is looked at, too
        final File file = newFile("c632/file.txt", "x");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(missing, file));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.split(missing, 2, file));

        // a null source is still the first complaint, and a missing source with a good destination is still FNFE
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip((File) null, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(null, null));
        assertThrows(FileNotFoundException.class, () -> IOUtil.zip(missing, tempDir.resolve("c632/o.zip").toFile()));
        assertThrows(FileNotFoundException.class, () -> IOUtil.split(missing, 2, newDir("c632/parts")));
        assertFalse(tempDir.resolve("c632/o.zip").toFile().exists());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-633 / C-634: zip charset pre-checks
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC633_zipOfAnEmptyCollectionWithADecodeOnlyCharsetIsRefused() throws IOException {
        assumeTrue(!DECODE_ONLY.canEncode());

        final File target = tempDir.resolve("c633/empty.zip").toFile();
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.zip(Collections.emptyList(), target, DECODE_ONLY));
        assertFalse(target.exists(), "target not created");

        final File existing = newFile("c633/existing.zip", "keep");
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.zip(Collections.emptyList(), existing, DECODE_ONLY));
        assertEquals("keep", read(existing));

        // an empty collection with a usable charset still writes an empty archive
        IOUtil.zip(Collections.emptyList(), target, StandardCharsets.UTF_8);
        assertTrue(target.isFile());
        final File out = newDir("c633/out");
        IOUtil.unzip(target, out);
        assertEquals(0, out.list().length);
    }

    @Test
    public void testC634_zipOfAFileWithAnUnencodableNameToANewTargetLeavesNothingBehind() throws IOException {
        final File src = newFile("c634/caf\u00e9.txt", "x");
        final File fresh = tempDir.resolve("c634/fresh.zip").toFile();

        final IOException e = assertThrows(IOException.class, () -> IOUtil.zip(src, fresh, StandardCharsets.US_ASCII));
        assertTrue(e.getMessage().contains("cannot be encoded"), e.getMessage());
        assertTrue(e.getMessage().contains("caf\u00e9.txt"), "the entry name is in the message: " + e.getMessage());
        assertFalse(fresh.exists(), "no junk archive left behind");

        // the same file zips fine under UTF-8, and a directory source with a new target still relies on the write
        IOUtil.zip(src, fresh, StandardCharsets.UTF_8);
        assertTrue(fresh.isFile());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-636 / C-637: splitByLine part-is-directory; merge example names
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC636_splitByLinePartPathThatIsADirectoryIsIllegalArgument() throws IOException {
        final File g = newFile("c636/g.txt", "1\n2\n3\n4\n");
        newDir("c636/g_0001.txt");
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(g, 2));
        assertTrue(e.getMessage().contains("is a directory"), e.getMessage());
    }

    @Test
    public void testC637_splitPartNamesMergeBackAsTheExampleShows() throws IOException {
        final File data = newFile("c637/data.txt", "0123456789");
        IOUtil.split(data, 3);
        final List<File> parts = Arrays.asList(new File(data.getParentFile(), "data.txt_0001"), new File(data.getParentFile(), "data.txt_0002"),
                new File(data.getParentFile(), "data.txt_0003"));

        for (final File part : parts) {
            assertTrue(part.isFile(), part.getName());
        }

        final File merged = tempDir.resolve("c637/data_merged.txt").toFile();
        assertEquals(10, IOUtil.merge(parts, merged));
        assertEquals("0123456789", read(merged));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-639: reserved device names in unzip (Windows)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC639_reservedDeviceNamesAreRejectedOnWindows() throws IOException {
        assumeTrue(IOUtil.IS_OS_WINDOWS);

        int i = 0;

        for (final String entry : new String[] { "NUL", "nul", "sub/NUL", "NUL.txt", "COM1", "lpt9.log", "COM\u00b9", "x/NUL/y.txt", "NUL/", "aux.c",
                "Con.tar.gz", "prn .txt" }) {
            final File zip = writeZip("c639/r" + (i++) + ".zip", entry);
            final File out = newDir("c639/out" + i);
            final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, out), entry);
            assertTrue(e.getMessage().contains("reserved device name"), entry + " -> " + e.getMessage());
            assertTrue(e.getMessage().contains(entry), entry + " -> " + e.getMessage());
            assertEquals(0, out.list().length, entry + ": nothing created");
        }

        for (final String entry : new String[] { "NULL", "CONX", "COM10", "x.NUL", "COM", "LPT", "nulx/y.txt", "sub/AUXILIARY" }) {
            final File zip = writeZip("c639/ok" + (i++) + ".zip", entry);
            final File out = newDir("c639/okout" + i);
            IOUtil.unzip(zip, out);
            assertTrue(new File(out, entry.replace('/', File.separatorChar)).exists(), entry + " extracts");
        }

        // the dot / space rule still comes first
        for (final String entry : new String[] { "NUL.", "NUL " }) {
            final File zip = writeZip("c639/ds" + (i++) + ".zip", entry);
            final File out = newDir("c639/dsout" + i);
            final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, out), entry);
            assertTrue(e.getMessage().contains("dot or a space"), entry + " -> " + e.getMessage());
        }
    }

    @Test
    public void testC639_reservedDeviceNamesAreOrdinaryNamesElsewhere() throws IOException {
        assumeTrue(!IOUtil.IS_OS_WINDOWS);

        final File zip = writeZip("c639u/nul.zip", "NUL");
        final File out = newDir("c639u/out");
        IOUtil.unzip(zip, out);
        assertEquals("payload of NUL", read(new File(out, "NUL")));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-640 / C-641 / C-642 / C-647: toFile(URL)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC640_pathLessHostAndBareDriveAreRejected() throws Exception {
        // C-681: the bare-drive rule is Windows-only; elsewhere "/C:" is an ordinary name.
        if (IOUtil.IS_OS_WINDOWS) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://C:")));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///C:")));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///c:")));
        } else {
            assertEquals(new File("/C:"), IOUtil.toFile(new URL("file:///C:")));
        }
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://localhost")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:")));

        if (IOUtil.IS_OS_WINDOWS) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://server")));
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://server/")));
            assertEquals("\\\\server\\share\\x", IOUtil.toFile(new URL("file://server/share/x")).getPath());
            assertEquals(new File("\\\\server\\share"), IOUtil.toFile(new URL("file://server/share")));
        }

        // the drive root and paths below it are fine, on every platform, in both spellings
        assertEquals(new File("/C:/"), IOUtil.toFile(new URL("file:///C:/")));
        assertEquals(new File("/C:/x"), IOUtil.toFile(new URL("file:///C:/x")));
        assertEquals(new File("/C:/x"), IOUtil.toFile(new URL("file://C:/x")));
        assertEquals(IOUtil.toFile(new URL("file:///C:/x")), IOUtil.toFile(new URL("file://C:/x")));
        assertEquals(new File("/tmp/a.txt"), IOUtil.toFile(new URL("file:///tmp/a.txt")));
        assertEquals(new File("/tmp/a.txt"), IOUtil.toFile(new URL("file://localhost/tmp/a.txt")));
    }

    @Test
    public void testC641_remoteHostIsUncOnWindowsAndRejectedElsewhere() throws Exception {
        final URL unc = new URL("file://server/share/f.txt");

        if (IOUtil.IS_OS_WINDOWS) {
            final File f = IOUtil.toFile(unc);
            assertEquals("\\\\server\\share\\f.txt", f.getPath());
            f.toPath(); // parseable
            final URL back = IOUtil.toUrl(f);
            assertTrue(back.toString().contains("server"), back.toString());
            assertEquals(f, IOUtil.toFile(back), "toUrl/toFile round trip keeps the host");
        } else {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(unc));
            assertTrue(e.getMessage().contains("remote host"), e.getMessage());
        }

        // unchanged on every platform
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://host:80/share/f.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://user@host/share/f.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file://[::1]/share/f.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("http://example.com/x.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile((URL) null));
    }

    @Test
    public void testC642_percentEncodedNulIsRejectedLikeAnEncodedSeparator() throws Exception {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///C:/tmp/a%00b.txt")));
        assertTrue(e.getMessage().contains("NUL"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///tmp/%00")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///tmp/a%2F..%2Fb")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL("file:///tmp/a%5cb")));

        // "%0" and "%" alone stay lenient; "%20" still decodes
        assertEquals(new File("/tmp/a%0"), IOUtil.toFile(new URL("file:///tmp/a%0")));
        assertEquals(new File("/tmp/a%"), IOUtil.toFile(new URL("file:///tmp/a%")));
        assertEquals(new File("/my docs/f.txt"), IOUtil.toFile(new URL("file:///my%20docs/f.txt")));
        assertEquals(new File("/tmp/\u00e9.txt"), IOUtil.toFile(new URL("file:///tmp/%C3%A9.txt")));
    }

    @Test
    public void testC647_wellFormedEscapesThatAreNotUtf8BecomeReplacementCharacters() throws Exception {
        assertEquals(new File("/tmp/\uFFFD.txt"), IOUtil.toFile(new URL("file:///tmp/%FF.txt")));
        assertTrue(IOUtil.toFile(new URL("file:///tmp/%C0%AF.txt")).getPath().contains("\uFFFD"), "overlong never folds to a separator");
        assertFalse(IOUtil.toFile(new URL("file:///tmp/%C0%AF.txt")).getPath().contains("/tmp/\uFFFD\uFFFD.txt".substring(0, 0) + "/."));
        assertEquals(new File("/tmp/a%zz.txt"), IOUtil.toFile(new URL("file:///tmp/a%zz.txt")), "malformed: literal");
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-643 / C-648: walk(File) lists the direct children only
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC643_walkOneArgListsDirectChildrenOnly() throws IOException {
        final File root = newDir("c643/root");
        newFile("c643/root/a.txt", "a");
        newFile("c643/root/sub/b.txt", "b");

        final List<String> names = new ArrayList<>();

        for (final File f : IOUtil.walk(root).toList()) {
            names.add(f.getName());
        }

        Collections.sort(names);
        assertEquals(Arrays.asList("a.txt", "sub"), names);
        assertEquals(3, IOUtil.walk(root, true, false).count());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-645: null elements named after the caller's parameter
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC645_nullElementsAreReportedUnderTheCollectionParameterName() throws Exception {
        final URL ok = new File("x.txt").toURI().toURL();

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles(new URL[] { ok, null }));
        assertEquals("'urls' cannot hold a null element", e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles(Arrays.asList(ok, null)));
        assertEquals("'urls' cannot hold a null element", e2.getMessage());
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls(new File[] { new File("x"), null }));
        assertEquals("'files' cannot hold a null element", e3.getMessage());
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls(Arrays.asList(new File("x"), null)));
        assertEquals("'files' cannot hold a null element", e4.getMessage());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles((URL[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls((File[]) null));
        assertEquals(0, IOUtil.toFiles(new URL[0]).length);
        assertEquals(0, IOUtil.toUrls(Collections.<File> emptyList()).size());
        assertEquals(1, IOUtil.toFiles(new URL[] { ok }).length);
        assertEquals(1, IOUtil.toUrls(Arrays.asList(new File("x"))).size());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-646: contentEquals(InputStream, InputStream) unchanged
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC646_contentEqualsStreamsStillCompares() {
        final byte[] a = randomBytes(20_000, 646);
        final byte[] b = a.clone();
        b[19_999]++;

        assertTrue(IOUtil.contentEquals(new ByteArrayInputStream(a), new ByteArrayInputStream(a.clone())));
        assertFalse(IOUtil.contentEquals(new ByteArrayInputStream(a), new ByteArrayInputStream(b)));
        assertTrue(IOUtil.contentEquals((InputStream) null, (InputStream) null));
        assertFalse(IOUtil.contentEquals(new ByteArrayInputStream(a), null));
        assertSame(Boolean.TRUE, IOUtil.contentEquals(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0])));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-618: map @throws IAE (documented; the > Integer.MAX_VALUE case needs a 2 GB file and is not exercised)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC618_mapRejectsNullAndDirectory() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(tempDir.toFile()));
        assertNotNull(assertThrows(UncheckedIOException.class, () -> IOUtil.map(tempDir.resolve("missing").toFile())).getCause());
    }
}
