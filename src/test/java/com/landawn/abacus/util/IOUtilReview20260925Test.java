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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-25 cross review of the 2026-09-24 IOUtil changes (findings U12-01..U12-06 and
 * U13-01..U13-05). Each test method name carries the finding it pins.
 */
public class IOUtilReview20260925Test extends TestBase {

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------------

    private static File newFile(final File dir, final String name, final String content) throws IOException {
        final File f = new File(dir, name);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static String read(final File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static List<String> sortedListing(final File dir) {
        final String[] names = dir.list();
        assertTrue(names != null, "cannot list " + dir);
        final List<String> list = new ArrayList<>(Arrays.asList(names));
        Collections.sort(list);
        return list;
    }

    /** Creates a Windows directory junction {@code link -> target}; {@code false} when {@code mklink /J} is not available. */
    private static boolean junction(final File link, final File target) throws IOException, InterruptedException {
        final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor() == 0 && Files.exists(link.toPath(), LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * A reader that hands out the given chunks one per bulk read (an empty chunk is a read that returns 0), with no
     * mark/reset and no skip of its own - so {@code IOUtil.skip(Reader, long)} has to read it through a pooled buffer.
     */
    private static final class PlainReader extends Reader {
        private final String[] chunks;
        private int chunk;
        private int pos;

        PlainReader(final String... chunks) {
            this.chunks = chunks;
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

        boolean exhausted() {
            return chunk >= chunks.length;
        }

        @Override
        public void close() {
        }
    }

    /** A writer that counts its flushes. */
    private static final class CountingWriter extends StringWriter {
        final AtomicInteger flushes = new AtomicInteger();

        @Override
        public void flush() {
            flushes.incrementAndGet();
            super.flush();
        }
    }

    /** A file that counts how often the file system is asked whether it is a directory. */
    private static final class CountingFile extends File {
        private static final long serialVersionUID = 1L;
        final AtomicInteger isDirectoryCalls = new AtomicInteger();

        CountingFile(final File f) {
            super(f.getPath());
        }

        @Override
        public boolean isDirectory() {
            isDirectoryCalls.incrementAndGet();
            return super.isDirectory();
        }
    }

    /** A {@code Throwable} that is neither an {@code Error} nor an {@code Exception}. */
    private static final class NeitherErrorNorException extends Throwable {
        private static final long serialVersionUID = 1L;

        NeitherErrorNorException(final String message) {
            super(message);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }

    private static Charset decodeOnlyCharset() {
        return Charset.availableCharsets().values().stream().filter(c -> !c.canEncode()).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // U13-01 / U13-02: copyDirectory into an ancestor - the repeated-name child of ANY kind is rejected up front
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU13_02_repeatedNameFileChildIsRejectedBeforeAnySiblingIsCopied() throws IOException {
        final File a = new File(tempDir.toFile(), "a");
        final File b = new File(a, "b");
        newFile(b, "0first.txt", "1");
        newFile(b, "b", "f"); // a regular FILE whose name repeats the ancestor's: its copy would land on a/b, the source
        newFile(b, "zlast.txt", "3");

        // used to be an IOException "The destination file already exists: ...a/b" AFTER a/0first.txt had been copied
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
        assertTrue(e.getMessage().contains("is the source directory, or one of its ancestors"), e.getMessage());
        assertEquals(Arrays.asList("b"), sortedListing(a), "no sibling may be copied before the rejection");
        assertTrue(b.isDirectory(), "the source is untouched");
        assertEquals(Arrays.asList("0first.txt", "b", "zlast.txt"), sortedListing(b));
        assertEquals("f", read(new File(b, "b")));

        // two levels down: copying g/b/c into g aims g/b/c/b at g/b, an ancestor - a file there collides just the same
        final File g = new File(tempDir.toFile(), "g");
        final File c = new File(g, "b/c");
        newFile(c, "0first.txt", "1");
        newFile(c, "b", "f");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(c, g));
        assertEquals(Arrays.asList("b"), sortedListing(g));
        assertEquals(Arrays.asList("c"), sortedListing(new File(g, "b")));
        assertEquals("f", read(new File(c, "b")));

        // control: the same tree without the repeated name still copies, and a file of that name elsewhere is fine
        final File h = new File(tempDir.toFile(), "h");
        final File hb = new File(h, "b");
        newFile(hb, "0first.txt", "1");
        newFile(hb, "sub/b", "f");

        IOUtil.copyDirectory(hb, h);

        assertEquals(Arrays.asList("0first.txt", "b", "sub"), sortedListing(h));
        assertEquals("f", read(new File(h, "sub/b")));
    }

    @Test
    public void testU13_02_repeatedNameSymbolicLinkChildIsRejectedUpFront() throws IOException {
        final File a = new File(tempDir.toFile(), "a");
        final File b = new File(a, "b");
        newFile(b, "0first.txt", "1");
        final File elsewhere = newFile(tempDir.toFile(), "elsewhere.txt", "e");
        final Path link = new File(b, "b").toPath();

        try {
            Files.createSymbolicLink(link, elsewhere.toPath());
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("cannot create a symbolic link here: " + e);
        }

        // used to be an IOException from copySymbolicLink ("already exists") after a/0first.txt had been copied
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
        assertEquals(Arrays.asList("b"), sortedListing(a));
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("e", read(elsewhere));

        // dangling as well: the entry is judged without following it
        assertTrue(elsewhere.delete());
        assertTrue(Files.isSymbolicLink(link));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
        assertEquals(Arrays.asList("b"), sortedListing(a));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testU13_01_repeatedNameJunctionChildIsRejectedInsteadOfBeingCopiedIntoTheSource() throws Exception {
        final File a = new File(tempDir.toFile(), "a");
        final File b = new File(a, "b");
        newFile(b, "0first.txt", "1");
        final File x = new File(tempDir.toFile(), "x");
        newFile(x, "xfile.txt", "x");
        final File junction = new File(b, "b");

        Assumptions.assumeTrue(junction(junction, x), "mklink /J is not available");

        try {
            // used to return normally with x's content written INTO the source: a/b = [0first.txt, b, xfile.txt]
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
            assertTrue(e.getMessage().contains("is the source directory, or one of its ancestors"), e.getMessage());
            assertEquals(Arrays.asList("b"), sortedListing(a), "nothing may be copied into the destination");
            assertEquals(Arrays.asList("0first.txt", "b"), sortedListing(b), "nothing may be written into the source");
            assertEquals(Arrays.asList("xfile.txt"), sortedListing(x));
            assertTrue(IOUtil.isSymbolicLinkOrJunction(junction));

            // dangling: the walk would have left it out silently; the name can never be recreated there, so it is
            // rejected the same way
            assertTrue(new File(x, "xfile.txt").delete());
            assertTrue(x.delete());
            assertFalse(junction.exists(), "File.exists follows the junction and finds nothing");
            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(b, a));
            assertEquals(Arrays.asList("b"), sortedListing(a));
            assertEquals(Arrays.asList("0first.txt", "b"), sortedListing(b));
        } finally {
            Files.deleteIfExists(junction.toPath());
        }

        // control: a junction child of any OTHER name is still followed and copied as a plain directory
        final File a2 = new File(tempDir.toFile(), "a2");
        final File b2 = new File(a2, "b2");
        newFile(b2, "0first.txt", "1");
        final File y = new File(tempDir.toFile(), "y");
        newFile(y, "yfile.txt", "y");
        final File other = new File(b2, "j");

        Assumptions.assumeTrue(junction(other, y), "mklink /J is not available");

        try {
            IOUtil.copyDirectory(b2, a2);

            assertEquals(Arrays.asList("0first.txt", "b2", "j"), sortedListing(a2));
            assertEquals("y", read(new File(a2, "j/yfile.txt")));
            assertEquals(Arrays.asList("0first.txt", "j"), sortedListing(b2));
        } finally {
            Files.deleteIfExists(other.toPath());
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // U13-03 / U13-04: closeAll with a Throwable that is neither an Error nor an Exception
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU13_03_closeAllWrapsAThrowableThatIsNeitherErrorNorException() {
        // used to be a ClassCastException from the `(Exception) ex` cast: no cause, no suppressed exceptions
        final NeitherErrorNorException weird1 = new NeitherErrorNorException("weird");
        final IOException second = new IOException("second");
        final AtomicBoolean secondClosed = new AtomicBoolean();
        final AutoCloseable later = () -> {
            secondClosed.set(true);
            throw second;
        };

        final RuntimeException e = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(() -> sneakyThrow(weird1), later));
        assertSame(weird1, e.getCause(), () -> "wrapped, not lost: " + e);
        assertTrue(secondClosed.get(), "every element is still attempted");
        assertEquals(1, Arrays.stream(e.getSuppressed()).filter(s -> s == second).count(), Arrays.toString(e.getSuppressed()));

        // the Iterable overload, with a null element on the way
        final NeitherErrorNorException weird2 = new NeitherErrorNorException("weird");
        final IOException third = new IOException("third");
        secondClosed.set(false);
        final RuntimeException e2 = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(Arrays.asList(() -> sneakyThrow(weird2), null, () -> {
            secondClosed.set(true);
            throw third;
        })));
        assertSame(weird2, e2.getCause());
        assertTrue(secondClosed.get());
        assertEquals(1, Arrays.stream(e2.getSuppressed()).filter(s -> s == third).count(), Arrays.toString(e2.getSuppressed()));

        // alone: wrapped, nothing suppressed
        final NeitherErrorNorException weird3 = new NeitherErrorNorException("weird");
        final RuntimeException e3 = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(() -> sneakyThrow(weird3)));
        assertSame(weird3, e3.getCause());
        assertEquals(0, e3.getSuppressed().length, Arrays.toString(e3.getSuppressed()));

        // thrown AFTER a checked first failure it is suppressed on that failure's wrapper, as before
        final NeitherErrorNorException weird4 = new NeitherErrorNorException("weird");
        final IOException first = new IOException("first");
        final RuntimeException e4 = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(() -> {
            throw first;
        }, () -> sneakyThrow(weird4)));
        assertSame(first, e4.getCause());
        assertTrue(Arrays.asList(e4.getSuppressed()).contains(weird4), Arrays.toString(e4.getSuppressed()));

        // U13-04: the varargs overload documents what it delegates to - an Error is thrown after every element was attempted
        final AssertionError error = new AssertionError("error");
        secondClosed.set(false);
        final AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtil.closeAll(() -> {
            throw error;
        }, later));
        assertSame(error, thrown);
        assertTrue(secondClosed.get());
        assertTrue(Arrays.asList(thrown.getSuppressed()).contains(second));
    }

    // ------------------------------------------------------------------------------------------------------------
    // U13-05: the entry-name budget leaves room for the ZIP64 extra
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU13_05_entryNameInsideTheZip64BudgetIsRejectedBeforeAnExistingTargetIsTouched() throws IOException {
        // 87 levels of 250 CJK characters (750 UTF-8 bytes + '/') below "deep", then one of 40: an entry name of
        // 4 + 87 * 751 + 1 + 120 = 65,462 bytes. That is within the old budget of 65,480 (46-byte header + 9-byte
        // timestamp) - which ZipOutputStream accepts only while the entry sits below the 4 GB mark - and beyond the
        // new one of 65,425, which also budgets the 28-byte ZIP64 extra and a 36-byte NTFS time.
        final StringBuilder nameBuilder = new StringBuilder();

        for (int i = 0; i < 250; i++) {
            nameBuilder.append((char) (0x4e00 + i));
        }

        final String name = nameBuilder.toString();
        final String tail = name.substring(0, 40);
        final File root = new File(tempDir.toFile(), "deep");
        Path deepest = root.toPath();

        try {
            for (int i = 0; i < 87; i++) {
                deepest = deepest.resolve(name);
            }

            deepest = deepest.resolve(tail);

            try {
                Files.createDirectories(deepest);
            } catch (final IOException | InvalidPathException e) {
                Assumptions.abort("cannot create a path this long here: " + e);
            }

            final File target = new File(tempDir.toFile(), "keep.zip");
            Files.write(target.toPath(), "8 bytes!".getBytes(StandardCharsets.UTF_8));

            final IOException e1 = assertThrows(IOException.class, () -> IOUtil.zip(root, target));
            final Matcher m = Pattern.compile("too long: (\\d+) bytes").matcher(String.valueOf(e1.getMessage()));
            assertTrue(m.find(), e1.getMessage());
            final int encoded = Integer.parseInt(m.group(1));
            assertTrue(encoded > 65_425 && encoded <= 65_480, "the name must sit in the window the old budget let through: " + encoded);
            assertEquals("8 bytes!", read(target));

            final IOException e2 = assertThrows(IOException.class, () -> IOUtil.zip(Arrays.asList(root), target));
            assertTrue(e2.getMessage().contains("too long"), e2.getMessage());
            assertEquals("8 bytes!", read(target));
        } finally {
            IOUtil.deleteRecursivelyIfExists(root);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // U12-01 / U12-04: copyChars skips before it takes its buffer; the sliced Reader/append forms return 0 as documented
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU12_01_copyCharsSkipsBeforeTakingItsBufferAndReturnsZeroPastTheSource() throws IOException {
        // an offset beyond a plain reader (skipped through a pooled buffer): 0, flushed, nothing written
        PlainReader source = new PlainReader("ab", "", "cd");
        CountingWriter out = new CountingWriter();
        assertEquals(0, IOUtil.write(source, 10, 3, out, true));
        assertEquals("", out.toString());
        assertEquals(1, out.flushes.get());
        assertTrue(source.exhausted());

        // an offset inside it: the slice, with one transient zero survived
        source = new PlainReader("ab", "", "cd");
        out = new CountingWriter();
        assertEquals(3, IOUtil.write(source, 1, 3, out, false));
        assertEquals("bcd", out.toString());
        assertEquals(0, out.flushes.get());

        // count 0 never moves the source
        source = new PlainReader("ab");
        out = new CountingWriter();
        assertEquals(0, IOUtil.write(source, 5, 0, out, true));
        assertEquals(1, out.flushes.get());
        assertFalse(source.exhausted());

        // U12-04: the Reader -> File forms leave the target existing and empty; the sliced appends create a missing
        // target and never truncate an existing one
        final File written = new File(tempDir.toFile(), "w/out.txt");
        assertEquals(0, IOUtil.write(new PlainReader("ab"), 5, 3, written));
        assertTrue(written.isFile());
        assertEquals("", read(written));

        assertEquals(0, IOUtil.write(new PlainReader("ab"), 5, 3, StandardCharsets.UTF_8, written));
        assertEquals("", read(written));

        final File missing = new File(tempDir.toFile(), "w/missing.txt");
        assertEquals(0, IOUtil.append(new PlainReader("ab"), 5, 3, missing));
        assertTrue(missing.isFile());
        assertEquals("", read(missing));

        final File kept = newFile(tempDir.toFile(), "w/kept.txt", "keep");
        assertEquals(0, IOUtil.append(new PlainReader("ab"), 5, 3, StandardCharsets.UTF_8, kept));
        assertEquals("keep", read(kept));
        assertEquals(0, IOUtil.append(new PlainReader("ab"), 0, 0, kept));
        assertEquals("keep", read(kept));
        assertEquals(2, IOUtil.append(new PlainReader("ab"), 0, 5, kept));
        assertEquals("keepab", read(kept));

        final File source1 = newFile(tempDir.toFile(), "w/src.txt", "ab");
        final File missing2 = new File(tempDir.toFile(), "w/missing2.txt");
        assertEquals(0, IOUtil.append(source1, 5, 3, missing2));
        assertTrue(missing2.isFile());
        assertEquals("", read(missing2));
        try (java.io.InputStream is = IOUtil.newFileInputStream(source1)) {
            assertEquals(0, IOUtil.append(is, 5, 3, kept));
        }
        assertEquals("keepab", read(kept));
    }

    // ------------------------------------------------------------------------------------------------------------
    // U12-02: the class doc scopes the zero-read rule to this class's own line readers
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU12_02_forEachLineReaderKeepsTheJdkRuleWhileReadAllLinesEndsOnTwoZeros() throws Exception {
        assertEquals(Collections.emptyList(), IOUtil.readAllLines(new PlainReader("", "", "x\ny")));

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(new PlainReader("", "", "x\ny"), seen::add);
        assertEquals(Arrays.asList("x", "y"), seen);
    }

    // ------------------------------------------------------------------------------------------------------------
    // U12-03: the String-encoding overloads stat the source only once the charset name has been rejected
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU12_03_stringEncodingOverloadsDoNotStatTheSourceOnTheHappyPath() throws IOException {
        final CountingFile f = new CountingFile(newFile(tempDir.toFile(), "t.txt", "h\u00E9\nx"));

        // exactly as often as the Charset twin, which has no pre-check at all
        assertEquals(Arrays.asList("h\u00E9", "x"), IOUtil.readAllLines(f, StandardCharsets.UTF_8));
        final int twinLines = f.isDirectoryCalls.getAndSet(0);
        assertEquals(Arrays.asList("h\u00E9", "x"), IOUtil.readAllLines(f, "UTF-8"));
        assertEquals(twinLines, f.isDirectoryCalls.getAndSet(0), "readAllLines(File, String) stats the source although the charset name is fine");

        assertEquals("h\u00E9\nx", IOUtil.readAllToString(f, StandardCharsets.UTF_8));
        final int twinString = f.isDirectoryCalls.getAndSet(0);
        assertEquals("h\u00E9\nx", IOUtil.readAllToString(f, "UTF-8"));
        assertEquals(twinString, f.isDirectoryCalls.getAndSet(0), "readAllToString(File, String) stats the source although the charset name is fine");

        // null / empty still mean UTF-8, without a stat either
        assertEquals(Arrays.asList("h\u00E9", "x"), IOUtil.readAllLines(f, (String) null));
        assertEquals("h\u00E9\nx", IOUtil.readAllToString(f, ""));
        assertEquals(twinLines + twinString, f.isDirectoryCalls.getAndSet(0));

        // the ordering C-040 asked for is kept: null source, then a directory, then the charset name
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines((File) null, "no-such-cs"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((File) null, "bad name!"));

        final CountingFile dir = new CountingFile(tempDir.toFile());
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(dir, "no-such-cs"));
        assertTrue(e1.getMessage().contains("is a directory"), e1.getMessage());
        assertTrue(e1.getCause() instanceof UnsupportedCharsetException, String.valueOf(e1.getCause()));
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString(dir, "bad name!"));
        assertTrue(e2.getMessage().contains("is a directory"), e2.getMessage());
        assertTrue(e2.getCause() instanceof IllegalCharsetNameException, String.valueOf(e2.getCause()));
        assertEquals(2, dir.isDirectoryCalls.get(), "one stat per rejected charset name");

        // a directory with a GOOD charset name is still an IllegalArgumentException, from the failed open
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(dir, "UTF-8"));
        assertTrue(e3.getMessage().contains("is a directory"), e3.getMessage());

        // a good source with a bad charset name reports the charset
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.readAllLines(f, "no-such-cs"));
        assertThrows(IllegalCharsetNameException.class, () -> IOUtil.readAllToString(f, "bad name!"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // U12-05: every CharSequence / char[] -> OutputStream encoder reports a decode-only charset the same way
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testU12_05_decodeOnlyCharsetIsReportedByEveryOutputStreamEncoder() throws IOException {
        final Charset decodeOnly = decodeOnlyCharset();
        Assumptions.assumeTrue(decodeOnly != null, "no decode-only charset in this JVM");

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final char[] chars = { 'a', 'b' };

        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write("a", decodeOnly, os));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write("a", decodeOnly, os, true));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(chars, decodeOnly, os));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(chars, 1, 1, decodeOnly, os));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(chars, 1, 1, decodeOnly, os, true));
        assertEquals(0, os.size(), "nothing may reach the stream");

        // C-654: an empty payload is refused too - the charset is checked before the length, whatever the payload,
        // and nothing is written or flushed
        final int[] flushes = { 0 };
        final ByteArrayOutputStream emptyTarget = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                flushes[0]++;
                super.flush();
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(new char[0], decodeOnly, emptyTarget));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(chars, 1, 0, decodeOnly, emptyTarget));
        assertThrows(UnsupportedOperationException.class, () -> IOUtil.write(chars, 1, 0, decodeOnly, emptyTarget, true));
        assertEquals(0, emptyTarget.size(), "nothing may reach the stream");
        assertEquals(0, flushes[0], "the stream must not be flushed");
        assertEquals(0, os.size());

        // a null charset means UTF-8 on every form
        IOUtil.write("a", (Charset) null, os);
        IOUtil.write("b", (Charset) null, os, true);
        IOUtil.write(chars, (Charset) null, os);
        IOUtil.write(chars, 1, 1, (Charset) null, os);
        IOUtil.write(chars, 0, 1, (Charset) null, os, true);
        assertEquals("ababba", os.toString(StandardCharsets.UTF_8.name()));
    }
}
