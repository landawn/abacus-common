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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the 10th independent review pass over {@link IOUtil} (2026-09-02, second report of the day).
 *
 * <p>Covers, in order: a missing {@code .zip}-named source reported as {@link FileNotFoundException} like every
 * other missing source (B1), {@code copyToDirectory} keeping a symlink source's own name (B2),
 * {@code sizeOf}/{@code sizeOfDirectory} agreeing on an unreadable directory (B3), {@code forEachLine} accepting a
 * FIFO the way {@code readAllLines} does (B4), {@code readLines(reader, offset, 0)} not touching the reader (B5),
 * {@code copyFile} with {@code NOFOLLOW_LINKS} copying dangling and directory links and no longer demanding a
 * readable destination (B6), {@code transfer} reading a zero-size {@code FileChannel} to end of input (B7), the
 * null-before-range argument order (D3), {@code copyURLToFile} keeping the old content when the transfer fails
 * (D4), and {@code walk} listing nothing before the stream is advanced (D5).
 *
 * <p>Symbolic-link, permission and FIFO cases are skipped, not failed, where the platform cannot set them up
 * (Windows without the symlink privilege, a filesystem that ignores {@code setReadable(false)}, no {@code mkfifo}).
 */
public class IOUtilMissingZipSourceTest extends TestBase {

    @TempDir
    File tempDir;

    /** Creates {@code link -> target}, or aborts the test where the platform cannot create symbolic links. */
    private static Path linkOrSkip(final Path link, final Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("This platform cannot create symbolic links here: " + e);
            return null;
        }
    }

    /** Makes {@code file} unreadable, or aborts the test where the platform (or the current user) cannot. */
    private static void makeUnreadableOrSkip(final File file) {
        final boolean changed = file.setReadable(false, false) && (!file.isDirectory() || file.setExecutable(false, false));
        Assumptions.assumeTrue(changed && !file.canRead(), "This platform or user cannot make a file unreadable here");
    }

    private static void restore(final File file) {
        file.setReadable(true, false);
        file.setExecutable(true, false);
        file.setWritable(true, false);
    }

    /** Creates a FIFO, or aborts the test where there is no {@code mkfifo}. */
    private static File fifoOrSkip(final File fifo) {
        Assumptions.assumeTrue(IOUtil.IS_OS_LINUX || IOUtil.IS_OS_MAC, "FIFO tests need a Unix platform");

        try {
            final Process p = new ProcessBuilder("mkfifo", fifo.getAbsolutePath()).redirectErrorStream(true).start();
            Assumptions.assumeTrue(p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0, "mkfifo is not available here");
        } catch (final IOException | InterruptedException e) {
            Assumptions.abort("mkfifo is not available here: " + e);
        }

        return fifo;
    }

    /** Starts a shell that writes {@code text} into {@code fifo}; the write blocks until a reader opens the FIFO. */
    private static Process startFifoWriter(final File fifo, final String text) throws IOException {
        return new ProcessBuilder("bash", "-c", "printf '%s' \"$1\" > \"$2\"", "bash", text, fifo.getAbsolutePath()).start();
    }

    private static Throwable rootOf(final Throwable t) {
        Throwable root = t;

        while (root.getCause() != null) {
            root = root.getCause();
        }

        return root;
    }

    // ------------------------------------------------------------------------------------------------
    // B1: a missing ".zip" source is a FileNotFoundException, like every other missing source
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMissingZipSourceIsFileNotFoundAcrossTheReadFamily() {
        final File missing = new File(tempDir, "missing.zip");
        final byte[] buf = new byte[8];
        final List<Runnable> reads = Arrays.asList(() -> IOUtil.readAllBytes(missing), () -> IOUtil.readBytes(missing, 0, 4),
                () -> IOUtil.readAllChars(missing), () -> IOUtil.readChars(missing, 0, 4), () -> IOUtil.readAllToString(missing),
                () -> IOUtil.readToString(missing, 0, 4), () -> IOUtil.readAllLines(missing), () -> IOUtil.readLines(missing, 0, 4),
                () -> IOUtil.readFirstLine(missing), () -> IOUtil.readLastLine(missing), () -> IOUtil.readLine(missing, 0), () -> {
                    try {
                        IOUtil.read(missing, buf);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        for (final Runnable read : reads) {
            final UncheckedIOException e = assertThrows(UncheckedIOException.class, read::run);
            final Throwable cause = e.getCause();

            assertInstanceOf(FileNotFoundException.class, cause, "the class contract promises FileNotFoundException for an absent source");
            assertTrue(cause.getMessage().contains("does not exist"), cause.getMessage());
            assertInstanceOf(NoSuchFileException.class, cause.getCause(), "the platform's own failure is kept as the cause");
        }

        // The other two spellings of a missing source answered correctly before and still do.
        for (final File other : new File[] { new File(tempDir, "missing.gz"), new File(tempDir, "missing.txt") }) {
            final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllBytes(other));
            assertInstanceOf(FileNotFoundException.class, e.getCause());
        }
    }

    @Test
    public void testZipSourceStillReadsAndStillClassifiesADirectory() throws IOException {
        final File zip = new File(tempDir, "data.zip");

        try (ZipOutputStream zos = new ZipOutputStream(IOUtil.newFileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("dir/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("first.txt"));
            zos.write("hello\nworld\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertEquals(Arrays.asList("hello", "world"), IOUtil.readAllLines(zip));
        assertEquals("hello", IOUtil.readFirstLine(zip));

        final File dirNamedZip = new File(tempDir, "folder.zip");
        assertTrue(dirNamedZip.mkdir());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(dirNamedZip));
        assertTrue(e.getMessage().contains("is a directory"), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // B2: copyToDirectory keeps a symlink source's own name
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyToDirectoryKeepsTheNameOfAFileLink() throws IOException {
        final File target = new File(tempDir, "app-2026.log");
        IOUtil.write("rotated", target);
        final File link = new File(tempDir, "current.log");
        linkOrSkip(link.toPath(), target.toPath());

        final File dest = new File(tempDir, "dest");
        final File copied = IOUtil.copyToDirectory(link, dest);

        assertEquals("current.log", copied.getName(), "the copy carries the link's own name, as zip and moveToDirectory do");
        assertEquals(Arrays.asList("current.log"), names(dest));
        assertFalse(Files.isSymbolicLink(copied.toPath()), "the top-level link is followed: the copy is a plain file");
        assertEquals("rotated", IOUtil.readAllToString(copied), "the content is the target's");
    }

    @Test
    public void testCopyToDirectoryKeepsTheNameOfADirectoryLink() throws IOException {
        final File target = new File(tempDir, "config.v2");
        assertTrue(target.mkdir());
        IOUtil.write("k=v", new File(target, "a.properties"));
        final File link = new File(tempDir, "config");
        linkOrSkip(link.toPath(), target.toPath());

        final File dest = new File(tempDir, "dest");
        final File copied = IOUtil.copyToDirectory(link, dest);

        assertEquals("config", copied.getName());
        assertEquals(Arrays.asList("config"), names(dest));
        assertTrue(copied.isDirectory() && !Files.isSymbolicLink(copied.toPath()));
        assertEquals("k=v", IOUtil.readAllToString(new File(copied, "a.properties")));
    }

    @Test
    public void testCopyToDirectoryOfALinkIntoItsOwnDirectoryUsesTheCopyOfPrefix() throws IOException {
        final File dir = new File(tempDir, "d");
        assertTrue(dir.mkdir());
        final File target = new File(dir, "real.txt");
        IOUtil.write("content", target);
        final File link = new File(dir, "alias.txt");
        linkOrSkip(link.toPath(), target.toPath());

        // dir/alias.txt IS the source (through the link), so the copy takes the "Copy of" name - under the link's name.
        final File copied = IOUtil.copyToDirectory(link, dir);

        assertEquals("Copy of alias.txt", copied.getName());
        assertEquals("content", IOUtil.readAllToString(copied));
        assertEquals("content", IOUtil.readAllToString(target), "the target is untouched");
        assertTrue(Files.isSymbolicLink(link.toPath()), "the link is untouched");

        // A directory link into its own directory has no fallback name: the target would be the source itself.
        final File subTarget = new File(dir, "sub.v2");
        assertTrue(subTarget.mkdir());
        final File subLink = new File(dir, "sub");
        linkOrSkip(subLink.toPath(), subTarget.toPath());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(subLink, dir));
        assertEquals(Arrays.asList("Copy of alias.txt", "alias.txt", "real.txt", "sub", "sub.v2"), names(dir), "nothing was created by the rejected call");
    }

    @Test
    public void testCopyToDirectoryStillNormalizesDotSegmentsAndStillUsesCopyOfForAPlainFile() throws IOException {
        final File src = new File(tempDir, "src");
        assertTrue(src.mkdir());
        IOUtil.write("x", new File(src, "x.txt"));
        final File dest = new File(tempDir, "dest");

        // "src/." names the same directory: the copy is called "src", not "."
        final File copied = IOUtil.copyToDirectory(new File(src, "."), dest);
        assertEquals("src", copied.getName());
        assertEquals("x", IOUtil.readAllToString(new File(copied, "x.txt")));

        // a plain file copied into its own directory still gets the "Copy of" name
        final File plain = new File(tempDir, "plain.txt");
        IOUtil.write("p", plain);
        assertEquals("Copy of plain.txt", IOUtil.copyToDirectory(plain, tempDir).getName());
        assertEquals("p", IOUtil.readAllToString(new File(tempDir, "Copy of plain.txt")));
    }

    // ------------------------------------------------------------------------------------------------
    // B3: sizeOf and sizeOfDirectory agree on an unreadable directory
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSizeFamilyReportsAnUnreadableDirectoryInsteadOfAnsweringZero() throws IOException {
        final File dir = new File(tempDir, "unreadable");
        assertTrue(dir.mkdir());
        IOUtil.write("12345", new File(dir, "f"));
        makeUnreadableOrSkip(dir);

        try {
            for (final Runnable size : List.<Runnable> of(() -> IOUtil.sizeOf(dir), () -> IOUtil.sizeOfDirectory(dir), () -> IOUtil.sizeOfAsBigInteger(dir),
                    () -> IOUtil.sizeOfDirectoryAsBigInteger(dir), () -> IOUtil.sizeOf(dir, true), () -> IOUtil.sizeOfDirectory(dir, true))) {
                final UncheckedIOException e = assertThrows(UncheckedIOException.class, size::run);
                assertInstanceOf(FileNotFoundException.class, e.getCause());
                assertTrue(e.getCause().getMessage().contains("cannot be read"), e.getCause().getMessage());
            }

            // copyDirectory goes through the same validator: an unreadable source is reported, not copied as empty.
            final File dest = new File(tempDir, "dest");
            assertThrows(FileNotFoundException.class, () -> IOUtil.copyDirectory(dir, dest));
            assertFalse(dest.exists(), "a rejected call leaves no new directory behind");
        } finally {
            restore(dir);
        }
    }

    @Test
    public void testAnUnreadableSubdirectoryStillContributesNothing() throws IOException {
        final File root = new File(tempDir, "root");
        final File sub = new File(root, "sub");
        assertTrue(sub.mkdirs());
        IOUtil.write("1234", new File(root, "a"));
        IOUtil.write("56789", new File(sub, "b"));
        makeUnreadableOrSkip(sub);

        try {
            assertEquals(4, IOUtil.sizeOf(root));
            assertEquals(4, IOUtil.sizeOfDirectory(root));
            assertEquals(BigInteger.valueOf(4), IOUtil.sizeOfDirectoryAsBigInteger(root));
        } finally {
            restore(sub);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4: forEachLine reads what readAllLines reads
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testForEachLineReadsAFifoLikeReadAllLines() throws Exception {
        final File fifo = fifoOrSkip(new File(tempDir, "pipe"));

        Process writer = startFifoWriter(fifo, "a\nb\n");
        final List<String> lines = new ArrayList<>();
        IOUtil.forEachLine(fifo, lines::add);
        assertTrue(writer.waitFor(10, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("a", "b"), lines);

        writer = startFifoWriter(fifo, "c\n");
        lines.clear();
        IOUtil.forEachLine(Arrays.asList(fifo), lines::add);
        assertTrue(writer.waitFor(10, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("c"), lines);

        writer = startFifoWriter(fifo, "d\n");
        assertEquals(Arrays.asList("d"), IOUtil.readAllLines(fifo), "the eager twin has always read it");
        assertTrue(writer.waitFor(10, TimeUnit.SECONDS));
    }

    @Test
    public void testForEachLineStillValidatesItsSource() throws IOException {
        final File missing = new File(tempDir, "missing.txt");
        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(missing, l -> {
        }));
        assertInstanceOf(FileNotFoundException.class, e.getCause());
        assertTrue(e.getCause().getMessage().contains("does not exist"));

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((File) null, l -> {
        }));
        assertTrue(iae.getMessage().contains("source"), iae.getMessage());

        iae = assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(Arrays.asList((File) null), l -> {
        }));
        assertTrue(iae.getMessage().contains("file"), iae.getMessage());

        e = assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(Arrays.asList(missing), l -> {
        }));
        assertInstanceOf(FileNotFoundException.class, e.getCause());

        // a directory is still expanded, recursively, and a plain file still read
        final File dir = new File(tempDir, "tree");
        final File sub = new File(dir, "sub");
        assertTrue(sub.mkdirs());
        IOUtil.write("1\n", new File(dir, "a.txt"));
        IOUtil.write("2\n", new File(sub, "b.txt"));
        final List<String> lines = new ArrayList<>();
        IOUtil.forEachLine(dir, lines::add);
        assertEquals(Arrays.asList("1", "2"), lines.stream().sorted().toList());
        lines.clear();
        IOUtil.forEachLine(Arrays.asList(new File(dir, "a.txt"), sub), lines::add);
        assertEquals(Arrays.asList("1", "2"), lines);
    }

    // ------------------------------------------------------------------------------------------------
    // B5: readLines(reader, offset, 0) leaves the reader alone
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testReadLinesWithZeroCountDoesNotConsumeTheOffset() throws IOException {
        final Reader plain = new StringReader("a\nb\nc\nd\n");
        assertEquals(List.of(), IOUtil.readLines(plain, 2, 0));
        assertEquals("a\nb\nc\nd\n", IOUtil.readAllToString(plain), "the offset lines were not consumed");

        final Reader buffered = new BufferedReader(new StringReader("a\nb\nc\n"));
        assertEquals(List.of(), IOUtil.readLines(buffered, 5, 0));
        assertEquals("a", IOUtil.readFirstLine(buffered));

        // the sibling slicers already answered a zero count this way
        final Reader chars = new StringReader("abcdef");
        assertEquals(0, IOUtil.readChars(chars, 2, 0).length);
        assertEquals("abcdef", IOUtil.readAllToString(chars));

        // a positive count still skips the offset exactly
        final Reader sliced = new StringReader("a\nb\nc\nd\n");
        assertEquals(Arrays.asList("c"), IOUtil.readLines(sliced, 2, 1));
        assertEquals("d\n", IOUtil.readAllToString(sliced));

        // and the File overload answers an empty list for a zero count
        final File file = new File(tempDir, "lines.txt");
        IOUtil.write("a\nb\n", file);
        assertEquals(List.of(), IOUtil.readLines(file, 1, 0));
        assertEquals(Arrays.asList("b"), IOUtil.readLines(file, 1, 5));
    }

    // ------------------------------------------------------------------------------------------------
    // B6: copyFile with NOFOLLOW_LINKS, and the destination validation
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyFileNoFollowCopiesADanglingLinkAsALink() throws IOException {
        final File dangling = new File(tempDir, "dangling");
        linkOrSkip(dangling.toPath(), Path.of("nowhere"));
        final File copy = new File(tempDir, "dangling-copy");

        IOUtil.copyFile(dangling, copy, false, LinkOption.NOFOLLOW_LINKS);

        assertTrue(Files.isSymbolicLink(copy.toPath()));
        assertEquals(Path.of("nowhere"), Files.readSymbolicLink(copy.toPath()));

        // preserveFileDate = true is fine too: no time is stamped on a link
        final File copy2 = new File(tempDir, "dangling-copy2");
        IOUtil.copyFile(dangling, copy2, true, LinkOption.NOFOLLOW_LINKS);
        assertTrue(Files.isSymbolicLink(copy2.toPath()));

        // without the option a dangling link is still an absent source
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyFile(dangling, new File(tempDir, "followed")));
    }

    @Test
    public void testCopyFileNoFollowCopiesADirectoryLinkAsALink() throws IOException {
        final File target = new File(tempDir, "target-dir");
        assertTrue(target.mkdir());
        IOUtil.write("in", new File(target, "f.txt"));
        final File link = new File(tempDir, "dir-link");
        linkOrSkip(link.toPath(), target.toPath());
        final File copy = new File(tempDir, "dir-link-copy");

        IOUtil.copyFile(link, copy, false, LinkOption.NOFOLLOW_LINKS);

        assertTrue(Files.isSymbolicLink(copy.toPath()));
        assertEquals("in", IOUtil.readAllToString(new File(copy, "f.txt")), "the new link points at the same directory");

        // without the option a directory link is still a wrong-kind source
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(link, new File(tempDir, "followed")));
    }

    @Test
    public void testCopyFileAcceptsAnUnreadableButWritableDestination() throws IOException {
        final File src = new File(tempDir, "src.txt");
        IOUtil.write("new", src);
        final File dest = new File(tempDir, "dest.txt");
        IOUtil.write("old", dest);
        makeUnreadableOrSkip(dest);

        try {
            IOUtil.copyFile(src, dest);
        } finally {
            restore(dest);
        }

        assertEquals("new", IOUtil.readAllToString(dest));
    }

    @Test
    public void testCopyFileStillRejectsADirectoryDestinationAndANullSource() throws IOException {
        final File src = new File(tempDir, "src.txt");
        IOUtil.write("x", src);
        final File dir = new File(tempDir, "a-directory");
        assertTrue(dir.mkdir());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, dir));
        assertTrue(e.getMessage().contains("is a directory"), e.getMessage());

        assertTrue(assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(null, dir, false, LinkOption.NOFOLLOW_LINKS)).getMessage()
                .contains("srcFile"));
    }

    // ------------------------------------------------------------------------------------------------
    // B7: transfer reads a zero-size FileChannel to end of input
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testTransferReadsAFifoChannelToEndOfInput() throws Exception {
        final File fifo = fifoOrSkip(new File(tempDir, "pipe"));
        final File out = new File(tempDir, "out.bin");
        final Process writer = startFifoWriter(fifo, "hello fifo");

        try (FileChannel in = FileChannel.open(fifo.toPath(), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            Assumptions.assumeTrue(in.size() == 0, "this platform reports a size for a FIFO channel, so the case under test does not arise");
            assertEquals(10, IOUtil.transfer(in, dest));
        }

        assertTrue(writer.waitFor(10, TimeUnit.SECONDS));
        assertEquals("hello fifo", IOUtil.readAllToString(out));
    }

    @Test
    public void testTransferBetweenRegularFileChannelsIsUnchanged() throws IOException {
        final File empty = new File(tempDir, "empty.bin");
        IOUtil.write(new byte[0], empty);
        final File big = new File(tempDir, "big.bin");
        final byte[] data = new byte[70_001];
        Arrays.fill(data, (byte) 7);
        IOUtil.write(data, big);
        final File out = new File(tempDir, "out.bin");

        try (FileChannel in = FileChannel.open(empty.toPath(), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            assertEquals(0, IOUtil.transfer(in, dest));
        }

        try (FileChannel in = FileChannel.open(big.toPath(), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            in.position(1);
            assertEquals(70_000, IOUtil.transfer(in, dest));
            assertEquals(70_000, dest.position());
        }

        assertArrayEquals(Arrays.copyOfRange(data, 1, data.length), IOUtil.readAllBytes(out));

        // a non-empty source positioned past its end still takes the channel path and still transfers nothing
        try (FileChannel in = FileChannel.open(big.toPath(), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.position(data.length + 10);
            assertEquals(0, IOUtil.transfer(in, dest));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D3: a null source is reported before a bad range, on every sibling
    // ------------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface Call {
        public void run() throws IOException;
    }

    @Test
    public void testNullSourceIsReportedBeforeANegativeRange() {
        final OutputStream os = new ByteArrayOutputStream();
        final Writer w = new StringWriter();
        final File file = new File(tempDir, "f.txt");
        final List<Call> calls = Arrays.asList(() -> IOUtil.readBytes((File) null, -1, 1), () -> IOUtil.readChars((File) null, StandardCharsets.UTF_8, -1, 1),
                () -> IOUtil.readToString((File) null, StandardCharsets.UTF_8, -1, 1), () -> IOUtil.readLines((File) null, StandardCharsets.UTF_8, -1, 1),
                () -> IOUtil.readLine((File) null, StandardCharsets.UTF_8, -1), () -> IOUtil.write((InputStream) null, -1, 1, file),
                () -> IOUtil.write((InputStream) null, -1, 1, os, true), () -> IOUtil.write((Reader) null, -1, 1, StandardCharsets.UTF_8, file),
                () -> IOUtil.write((Reader) null, -1, 1, w, true), () -> IOUtil.append((InputStream) null, -1, 1, file),
                () -> IOUtil.append((Reader) null, -1, 1, StandardCharsets.UTF_8, file));

        for (final Call call : calls) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call::run);
            assertTrue(e.getMessage().contains("source"), e.getMessage());
        }

        // with a source present, the range is still validated
        assertTrue(assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new ByteArrayInputStream(new byte[0]), -1, 1, os, true)).getMessage()
                .contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> IOUtil.readLine(file, StandardCharsets.UTF_8, -1)).getMessage().contains("lineIndex"));
        assertFalse(file.exists(), "validation never created the target");
    }

    // ------------------------------------------------------------------------------------------------
    // D4: copyURLToFile keeps the previous content when the transfer fails
    // ------------------------------------------------------------------------------------------------

    /** A {@code probe:} URL whose stream yields {@code good} and then fails, or yields it whole when {@code failAfter} is false. */
    private static URL probeUrl(final byte[] good, final boolean failAfter) {
        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                        // nothing to connect
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new InputStream() {
                            private int pos = 0;

                            @Override
                            public int read() throws IOException {
                                if (pos < good.length) {
                                    return good[pos++] & 0xFF;
                                }

                                if (failAfter) {
                                    throw new IOException("connection dropped after " + pos + " bytes");
                                }

                                return -1;
                            }
                        };
                    }
                };
            }
        };

        try {
            return new URL(null, "probe:///download", handler);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testCopyURLToFileKeepsTheOldContentWhenTheTransferFails() throws IOException {
        final File dest = new File(tempDir, "download.bin");
        IOUtil.write("previous content", dest);

        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyURLToFile(probeUrl("partial".getBytes(StandardCharsets.UTF_8), true), dest));
        assertTrue(e.getMessage().contains("connection dropped"), e.getMessage());
        assertEquals("previous content", IOUtil.readAllToString(dest), "a failed transfer must not truncate the destination");
        assertEquals(Arrays.asList("download.bin"), names(tempDir), "no temporary file is left behind");

        // the timeout overload takes the same path
        assertThrows(IOException.class, () -> IOUtil.copyURLToFile(probeUrl("partial".getBytes(StandardCharsets.UTF_8), true), dest, 1000, 1000));
        assertEquals("previous content", IOUtil.readAllToString(dest));
        assertEquals(Arrays.asList("download.bin"), names(tempDir));
    }

    @Test
    public void testCopyURLToFileReplacesTheContentAndCreatesParentsOnSuccess() throws IOException {
        final File dest = new File(tempDir, "download.bin");
        IOUtil.write("previous content", dest);

        IOUtil.copyURLToFile(probeUrl("complete".getBytes(StandardCharsets.UTF_8), false), dest);
        assertEquals("complete", IOUtil.readAllToString(dest));
        assertEquals(Arrays.asList("download.bin"), names(tempDir));

        final File nested = new File(tempDir, "a/b/c/nested.bin");
        IOUtil.copyURLToFile(probeUrl("nested".getBytes(StandardCharsets.UTF_8), false), nested, 1000, 1000);
        assertEquals("nested", IOUtil.readAllToString(nested));
        assertEquals(Arrays.asList("nested.bin"), names(nested.getParentFile()));

        // an empty download still replaces the destination with an empty file
        IOUtil.copyURLToFile(probeUrl(new byte[0], false), dest);
        assertEquals(0, dest.length());
        assertTrue(dest.exists());

        // and a file: URL still works end to end
        final File source = new File(tempDir, "source.txt");
        IOUtil.write("from a file url", source);
        IOUtil.copyURLToFile(IOUtil.toUrl(source), dest);
        assertEquals("from a file url", IOUtil.readAllToString(dest));
    }

    @Test
    public void testCopyURLToFileRejectsADirectoryDestinationAndASelfCopy() throws IOException {
        final File dir = new File(tempDir, "a-directory");
        assertTrue(dir.mkdir());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> IOUtil.copyURLToFile(probeUrl("x".getBytes(StandardCharsets.UTF_8), false), dir));
        assertTrue(e.getMessage().contains("is a directory"), e.getMessage());
        assertTrue(dir.isDirectory(), "an empty directory must not be replaced by the download");

        final File self = new File(tempDir, "self.txt");
        IOUtil.write("keep", self);
        e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(IOUtil.toUrl(self), self));
        assertEquals("keep", IOUtil.readAllToString(self));
    }

    // ------------------------------------------------------------------------------------------------
    // D5: walk reads nothing until the stream is advanced
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWalkListsTheTopLevelOnlyWhenAdvanced() throws IOException {
        final File dir = new File(tempDir, "walk");
        assertTrue(dir.mkdir());
        IOUtil.write("1", new File(dir, "one.txt"));

        final com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, true, false);
        IOUtil.write("2", new File(dir, "two.txt"));
        final File sub = new File(dir, "sub");
        assertTrue(sub.mkdir());
        IOUtil.write("3", new File(sub, "three.txt"));

        assertEquals(Arrays.asList("one.txt", "sub", "three.txt", "two.txt"), stream.map(File::getName).sorted().toList(),
                "entries created after walk() but before the first advance are seen");

        // the eager twin and the lazy one still agree, element for element
        assertEquals(IOUtil.listFiles(dir, true, true), IOUtil.walk(dir, true, true).toList());
        assertEquals(IOUtil.listFiles(dir, true, false), IOUtil.walk(dir, true, false).toList());

        // and the documented answers for a null, an absent and a wrong-kind argument are unchanged
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk((File) null));
        assertEquals(0, IOUtil.walk(new File(tempDir, "absent")).count());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(new File(dir, "one.txt")));
    }

    // ------------------------------------------------------------------------------------------------
    // documentation pins
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCurrentDirKeepsItsTrailingSeparatorAsDocumented() {
        assertTrue(IOUtil.CURRENT_DIR.endsWith(File.separator), IOUtil.CURRENT_DIR);
        assertFalse(IOUtil.USER_DIR.endsWith(File.separator), IOUtil.USER_DIR);
        assertEquals(new File(IOUtil.USER_DIR).getAbsolutePath(), new File(IOUtil.CURRENT_DIR).getAbsolutePath());
    }

    @Test
    public void testReadLineReadsNothingPastTheReturnedLineAfterAZeroCountSlice() throws IOException {
        // a zero-count slice followed by a bounded read: the two compose, since the first never moved the reader
        final Reader reader = new StringReader("first\nsecond\n");
        assertEquals(List.of(), IOUtil.readLines(reader, 1, 0));
        assertEquals("first", IOUtil.readLine(reader, 0));
        assertEquals("second", IOUtil.readFirstLine(reader));
        assertNull(IOUtil.readFirstLine(reader));
    }

    // ------------------------------------------------------------------------------------------------
    // cycle 2 follow-ups from the verifiers: copyURLToFile hardening, copyToDirectory on-disk name
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyURLToFileReplacesAFileHeldOpenByAReader() throws IOException {
        final File dest = new File(tempDir, "held.bin");
        IOUtil.write("old content", dest);

        try (InputStream reader = new java.io.FileInputStream(dest)) {
            // On Windows a reader's handle blocks a replacing move; the in-place fallback still succeeds.
            IOUtil.copyURLToFile(probeUrl("new content".getBytes(StandardCharsets.UTF_8), false), dest);
            assertEquals(reader.available() >= 0, true);
        }

        assertEquals("new content", IOUtil.readAllToString(dest));
        assertEquals(Arrays.asList("held.bin"), names(tempDir), "no .part sibling is left behind");
    }

    @Test
    public void testCopyURLToFileKeepsThePosixPermissionsOfTheDestination() throws IOException {
        final File dest = new File(tempDir, "perms.bin");
        IOUtil.write("old", dest);
        final java.nio.file.attribute.PosixFileAttributeView view = Files.getFileAttributeView(dest.toPath(),
                java.nio.file.attribute.PosixFileAttributeView.class);
        Assumptions.assumeTrue(view != null, "POSIX permissions are not supported here");
        final java.util.Set<java.nio.file.attribute.PosixFilePermission> wanted = java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(dest.toPath(), wanted);

        IOUtil.copyURLToFile(probeUrl("new".getBytes(StandardCharsets.UTF_8), false), dest);

        assertEquals("new", IOUtil.readAllToString(dest));
        assertEquals(wanted, Files.getPosixFilePermissions(dest.toPath()), "a download must not narrow the file's mode to the temp file's 0600");
    }

    @Test
    public void testCopyURLToFileFallsBackToAnInPlaceWriteWhenTheDirectoryIsNotWritable() throws IOException {
        final File dir = new File(tempDir, "ro");
        assertTrue(dir.mkdir());
        final File dest = new File(dir, "file.bin");
        IOUtil.write("old", dest);
        Assumptions.assumeTrue(dir.setWritable(false, false) && !dir.canWrite() && dest.canWrite(),
                "This platform or user cannot make a directory read-only here");

        try {
            IOUtil.copyURLToFile(probeUrl("new".getBytes(StandardCharsets.UTF_8), false), dest);
            assertEquals("new", IOUtil.readAllToString(dest), "the writable file inside an unwritable directory is still replaced");
            assertEquals(Arrays.asList("file.bin"), names(dir));
        } finally {
            dir.setWritable(true, false);
        }
    }

    @Test
    public void testCopyToDirectoryUsesTheNameOnDiskNotTheCallersSpelling() throws IOException {
        final File onDisk = new File(tempDir, "foo.txt");
        IOUtil.write("f", onDisk);
        final File spelled = new File(tempDir, "FOO.TXT");
        Assumptions.assumeTrue(spelled.exists(), "needs a case-insensitive file system");

        final File copied = IOUtil.copyToDirectory(spelled, new File(tempDir, "dest"));

        assertEquals("foo.txt", copied.getName());
    }

    // ------------------------------------------------------------------------------------------------
    // cycle 3, C-031: a failed parallel forEachLine releases every file it opened
    // ------------------------------------------------------------------------------------------------

    private static long openFileDescriptors() {
        final File fd = new File("/proc/self/fd");
        final String[] entries = fd.isDirectory() ? fd.list() : null;

        return entries == null ? -1 : entries.length;
    }

    @Test
    public void testFailedParallelForEachLineReleasesEveryFile() throws IOException {
        final File dir = new File(tempDir, "par");
        assertTrue(dir.mkdir());

        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 40; i++) {
                IOUtil.write("l1\nl2\nl3\n", new File(dir, "f" + i + ".txt"));
            }

            final long fdsBefore = openFileDescriptors();

            assertThrows(IllegalStateException.class,
                    () -> IOUtil.forEachLine(dir, IOUtil.LineIterationOptions.builder().readThreads(4).processThreads(4).build(), line -> {
                        if (line.equals("l2")) {
                            throw new IllegalStateException("boom");
                        }
                    }));

            // Windows refuses to delete a file that is still open, which is the check that found this;
            // on Linux the deletion succeeds regardless, so the descriptor table is compared instead.
            if (fdsBefore >= 0) {
                for (int wait = 0; wait < 20 && openFileDescriptors() > fdsBefore; wait++) {
                    Thread.onSpinWait();
                    try {
                        Thread.sleep(50);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                assertTrue(openFileDescriptors() <= fdsBefore, "round " + round + ": file descriptors leaked by the reader threads");
            }

            assertTrue(IOUtil.deleteFilesFromDirectory(dir), "round " + round + ": a reader thread still holds a file open");
        }

        // an early stop through count takes the same close-while-reading path
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 40; i++) {
                IOUtil.write("l1\nl2\nl3\n", new File(dir, "h" + i + ".txt"));
            }

            final List<String> few = java.util.Collections.synchronizedList(new ArrayList<>());
            IOUtil.forEachLine(dir, IOUtil.LineIterationOptions.builder().count(5).readThreads(4).processThreads(4).build(), few::add);
            assertEquals(5, few.size());
            assertTrue(IOUtil.deleteFilesFromDirectory(dir), "round " + round + ": a reader thread still holds a file open after an early stop");
        }

        // and the successful parallel path still delivers every line
        for (int i = 0; i < 10; i++) {
            IOUtil.write("a\nb\n", new File(dir, "g" + i + ".txt"));
        }

        final List<String> seen = java.util.Collections.synchronizedList(new ArrayList<>());
        IOUtil.forEachLine(dir, IOUtil.LineIterationOptions.builder().readThreads(4).processThreads(4).build(), seen::add);
        assertEquals(20, seen.size());
        assertTrue(IOUtil.deleteFilesFromDirectory(dir));
    }

    // ------------------------------------------------------------------------------------------------
    // cycle 2, C-027: zip(Collection) rejects a file and a directory that share a name
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testZipCollectionRejectsAFileAndADirectoryOfTheSameName() throws IOException {
        final File fileX = new File(tempDir, "a/x");
        assertTrue(fileX.getParentFile().mkdirs());
        IOUtil.write("file", fileX);
        final File dirX = new File(tempDir, "b/x");
        assertTrue(dirX.mkdirs());
        IOUtil.write("inner", new File(dirX, "inner.txt"));
        final File archive = new File(tempDir, "out.zip");
        IOUtil.write("keep", archive);

        for (final List<File> sources : List.of(List.of(fileX, dirX), List.of(dirX, fileX))) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(sources, archive));
            assertTrue(e.getMessage().contains("Duplicate ZIP entry name"), e.getMessage());
            // Files.readString, not IOUtil.readAllToString: the read family would try to unzip the ".zip" name.
            assertEquals("keep", Files.readString(archive.toPath()), "a rejected call leaves the existing archive unchanged");
        }

        // a file "x.txt" beside a directory "x" is not a collision, and the archive round-trips
        final File fileXt = new File(tempDir, "a/x.txt");
        IOUtil.write("text", fileXt);
        IOUtil.zip(List.of(fileXt, dirX), archive);
        final File out = new File(tempDir, "out");
        IOUtil.unzip(archive, out);
        assertEquals("text", IOUtil.readAllToString(new File(out, "x.txt")));
        assertEquals("inner", IOUtil.readAllToString(new File(out, "x/inner.txt")));
    }

    // ------------------------------------------------------------------------------------------------
    // cycle 2, C-028: a malformed entry is an IOException, never an IllegalArgumentException
    // ------------------------------------------------------------------------------------------------

    private static File archive(final File dir, final String name, final String... entries) throws IOException {
        final File z = new File(dir, name);

        try (ZipOutputStream zos = new ZipOutputStream(IOUtil.newFileOutputStream(z))) {
            for (final String e : entries) {
                zos.putNextEntry(new ZipEntry(e));

                if (!e.endsWith("/")) {
                    zos.write(("content of " + e).getBytes(StandardCharsets.UTF_8));
                }

                zos.closeEntry();
            }
        }

        return z;
    }

    @Test
    public void testUnzipReportsAFileEntryOnAnExistingDirectoryAsIOException() throws IOException {
        final File dirFirst = archive(tempDir, "dir-first.zip", "sub/", "sub");
        final File out1 = new File(tempDir, "out1");
        final IOException e1 = assertThrows(IOException.class, () -> IOUtil.unzip(dirFirst, out1));
        assertTrue(e1.getMessage().contains("existing directory"), e1.getMessage());
        assertTrue(new File(out1, "sub").isDirectory(), "the directory entry extracted before the bad one stays");

        final File fileFirst = archive(tempDir, "file-first.zip", "sub", "sub/");
        final File out2 = new File(tempDir, "out2");
        final IOException e2 = assertThrows(IOException.class, () -> IOUtil.unzip(fileFirst, out2));
        assertTrue(e2.getMessage().contains("existing file"), e2.getMessage());
        assertEquals("content of sub", IOUtil.readAllToString(new File(out2, "sub")));

        // an existing directory in the target that a file entry lands on is the same case
        final File plain = archive(tempDir, "plain.zip", "sub");
        final File out3 = new File(tempDir, "out3");
        assertTrue(new File(out3, "sub").mkdirs());
        final IOException e3 = assertThrows(IOException.class, () -> IOUtil.unzip(plain, out3));
        assertTrue(e3.getMessage().contains("existing directory"), e3.getMessage());

        // the traversal guards still answer as documented, and a well-formed archive still extracts
        final File traversal = archive(tempDir, "traversal.zip", "x/../../evil.txt");
        assertTrue(assertThrows(IOException.class, () -> IOUtil.unzip(traversal, new File(tempDir, "out4"))).getMessage().contains("outside"));
        final File ok = archive(tempDir, "ok.zip", "d/", "d/f.txt", "in/../g.txt", "é中.txt");
        final File out5 = new File(tempDir, "out5");
        IOUtil.unzip(ok, out5);
        assertEquals("content of d/f.txt", IOUtil.readAllToString(new File(out5, "d/f.txt")));
        assertEquals("content of in/../g.txt", IOUtil.readAllToString(new File(out5, "g.txt")));
        assertEquals("content of é中.txt", IOUtil.readAllToString(new File(out5, "é中.txt")));
    }

    private static List<String> names(final File dir) {
        return IOUtil.listFiles(dir).stream().map(File::getName).sorted().toList();
    }

    // ------------------------------------------------------------------------------------------------
    // G15-003: the duplicate-entry message names its source by describe(..), i.e. the absolute path
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testDuplicateZipEntryNameMessageNamesTheSourceByItsAbsolutePath() throws IOException {
        final File first = new File(tempDir, "describe-a/x.txt");
        IOUtil.write("first", first);
        final File secondOnDisk = new File(tempDir, "describe-b/x.txt");
        IOUtil.write("second", secondOnDisk);

        // A spy whose toString() differs from its absolute path: that is the only way to tell which of the
        // two renderings the message used, since for an ordinary absolute File they are identical.
        final String spelling = "THE-CALLERS-OWN-SPELLING";
        final File second = new File(secondOnDisk.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return spelling;
            }
        };

        final File archive = new File(tempDir, "describe.zip");
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(List.of(first, second), archive));

        assertTrue(e.getMessage().contains("Duplicate ZIP entry name"), e.getMessage());
        assertTrue(e.getMessage().contains(secondOnDisk.getAbsolutePath()), e.getMessage());
        assertFalse(e.getMessage().contains(spelling), e.getMessage());
    }
}
