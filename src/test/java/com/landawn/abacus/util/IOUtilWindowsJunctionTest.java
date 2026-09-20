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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the 2026-09-03 IOUtil review (ledger {@code scripts/cross_review/IOUtil_Dates_ledger_2026-09-03.md}):
 * <ul>
 *   <li>C-001: Windows directory junctions are never followed (delete / list / size / copy), and a dangling one is unlinked;</li>
 *   <li>C-002: an empty {@code append*} onto a directory or an unwritable file fails exactly like its non-empty twin;</li>
 *   <li>C-003: {@code writeLines(.., Writer, ..)} hands over every accepted line when the iteration itself fails;</li>
 *   <li>C-004: {@code writeLine(Object, [Charset,] File)} renders the object before it truncates the file;</li>
 *   <li>C-005: {@code forEachLine(Reader|InputStream, ..)} leaves the caller's source open when a read fails;</li>
 *   <li>C-006: {@code moveToDirectory} names the target as {@code copyToDirectory} does and reports a missing source as
 *       {@code FileNotFoundException};</li>
 *   <li>C-007: {@code splitBySize} validates its arguments before touching the file; an empty file's single part carries
 *       the requested suffix width;</li>
 *   <li>C-008: a destination whose parent is a regular file is a bad argument, not an I/O failure.</li>
 * </ul>
 */
public class IOUtilWindowsJunctionTest extends TestBase {

    @TempDir
    Path tempDir;

    private File dir(final String name) {
        final File d = tempDir.resolve(name).toFile();
        assertTrue(d.mkdirs() || d.isDirectory());
        return d;
    }

    private File write(final String name, final String content) throws IOException {
        final File f = tempDir.resolve(name).toFile();
        Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
        return f;
    }

    private static String read(final File f) throws IOException {
        return Files.readString(f.toPath(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------------------------------------
    // C-002: empty append* onto a directory / unwritable file
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testEmptyAppendOntoADirectoryIsRejectedLikeTheNonEmptyTwin() throws IOException {
        final File d = dir("c002-dir");
        final Iterable<String> plainEmptyIterable = () -> Collections.<String> emptyList().iterator();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new byte[0], d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new byte[] { 1 }, 0, 0, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("", d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append((CharSequence) null, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("", StandardCharsets.UTF_16, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[0], d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[0], StandardCharsets.UTF_8, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[] { 'a' }, 0, 0, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[] { 'a' }, 1, 0, StandardCharsets.UTF_8, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(List.of(), d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(List.of(), StandardCharsets.UTF_8, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(plainEmptyIterable, d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.emptyIterator(), d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.emptyIterator(), StandardCharsets.UTF_8, d));
        // the non-empty twins, unchanged
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("x", d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(List.of("x"), d));

        assertTrue(d.isDirectory(), "the directory must survive every rejected call");
    }

    @Test
    public void testEmptyAppendStillCreatesAMissingFileAndNeverTruncatesAnExistingOne() throws IOException {
        final File fresh = tempDir.resolve("c002-fresh/deeper/new.txt").toFile();
        IOUtil.append("", fresh);
        assertTrue(fresh.isFile());
        assertEquals("", read(fresh));

        final File existing = write("c002-existing.txt", "keep 日本 😀");
        IOUtil.append((CharSequence) null, existing);
        IOUtil.append(new byte[0], existing);
        IOUtil.append(new char[0], existing);
        IOUtil.appendLines(List.of(), existing);
        IOUtil.appendLines(Collections.emptyIterator(), existing);
        assertEquals("keep 日本 😀", read(existing));

        IOUtil.append("+é", existing);
        assertEquals("keep 日本 😀+é", read(existing));
    }

    @Test
    public void testEmptyAppendOntoAnUnwritableFileFailsLikeTheNonEmptyTwin() throws IOException {
        final File ro = write("c002-ro.txt", "keep");
        Assumptions.assumeTrue(ro.setReadOnly() && !ro.canWrite(), "This platform or user cannot make a file read-only here");

        try {
            assertThrows(FileNotFoundException.class, () -> IOUtil.append("", ro));
            assertThrows(FileNotFoundException.class, () -> IOUtil.append(new byte[0], ro));
            assertThrows(FileNotFoundException.class, () -> IOUtil.appendLines(List.of(), ro));
            assertThrows(FileNotFoundException.class, () -> IOUtil.append("x", ro));
            assertEquals("keep", read(ro));
        } finally {
            ro.setWritable(true);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-003: writeLines hands over accepted lines when the iteration fails
    // ------------------------------------------------------------------------------------------------

    private static Iterator<String> failingAt(final int failAt) {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                i++;

                if (i == failAt) {
                    throw new IllegalStateException("boom at " + i);
                }

                return "line" + i;
            }
        };
    }

    @Test
    public void testWriteLines_HandsOverAcceptedLinesWhenTheIteratorFails() throws IOException {
        final StringWriter plain = new StringWriter();
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(failingAt(3), plain));
        assertEquals("line1\nline2\n", plain.toString());

        final StringWriter flushed = new StringWriter();
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(failingAt(3), flushed, true));
        assertEquals("line1\nline2\n", flushed.toString());

        final StringWriter iterable = new StringWriter();
        final Iterable<String> failingIterable = () -> failingAt(3);
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(failingIterable, iterable));
        assertEquals("line1\nline2\n", iterable.toString());

        // a caller-owned java.io.BufferedWriter keeps behaving as before: the lines sit in the caller's buffer
        final StringWriter behindBuffer = new StringWriter();
        final Writer bw = new java.io.BufferedWriter(behindBuffer);
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(failingAt(3), bw));
        bw.flush();
        assertEquals("line1\nline2\n", behindBuffer.toString());
    }

    @Test
    public void testWriteLines_HandsOverAcceptedLinesWhenAnElementCannotBeRendered() {
        final Object bad = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("toString boom");
            }
        };
        final StringWriter sw = new StringWriter();
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(Arrays.asList("a", "日本😀", null, bad, "never"), sw));
        assertEquals("a\n日本😀\nnull\n", sw.toString());

        // nothing accepted yet: nothing handed over
        final StringWriter empty = new StringWriter();
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(failingAt(1), empty));
        assertEquals("", empty.toString());
    }

    // ------------------------------------------------------------------------------------------------
    // C-004: writeLine renders before truncating
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWriteLine_RendersTheObjectBeforeTruncatingTheFile() throws IOException {
        final File existing = write("c004.txt", "precious 日本");
        final Object bad = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("toString boom");
            }
        };

        assertThrows(IllegalStateException.class, () -> IOUtil.writeLine(bad, existing));
        assertEquals("precious 日本", read(existing), "a failed rendering must not have truncated the file");

        assertThrows(IllegalStateException.class, () -> IOUtil.writeLine(bad, StandardCharsets.UTF_16, existing));
        assertEquals("precious 日本", read(existing));

        IOUtil.writeLine("héllo 😀", existing);
        assertEquals("héllo 😀\n", read(existing));

        IOUtil.writeLine(null, existing);
        assertEquals("null\n", read(existing));

        IOUtil.writeLine(42, StandardCharsets.UTF_16, existing);
        assertEquals("42\n", Files.readString(existing.toPath(), StandardCharsets.UTF_16));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", dir("c004-dir")));
    }

    // ------------------------------------------------------------------------------------------------
    // C-005: forEachLine leaves the caller's source open when a read fails
    // ------------------------------------------------------------------------------------------------

    private static Reader readerFailingOnSecondRead(final AtomicBoolean closed) {
        return new Reader() {
            private int calls = 0;

            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                if (++calls == 1) {
                    final String s = "line1\nline2\n";
                    s.getChars(0, s.length(), cbuf, off);
                    return s.length();
                }

                throw new IOException("disk on fire");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
    }

    @Test
    public void testForEachLine_LeavesTheCallersReaderOpenWhenAReadFails() {
        final AtomicBoolean closed = new AtomicBoolean();
        final List<String> seen = new ArrayList<>();
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(readerFailingOnSecondRead(closed), seen::add));
        assertEquals(Arrays.asList("line1", "line2"), seen);
        assertFalse(closed.get(), "the caller's reader must not be closed on the failure path");

        // the positional and options overloads take the same route
        final AtomicBoolean closed2 = new AtomicBoolean();
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(readerFailingOnSecondRead(closed2), 1, 10, l -> {
        }));
        assertFalse(closed2.get());

        final AtomicBoolean closed3 = new AtomicBoolean();
        assertThrows(UncheckedIOException.class,
                () -> IOUtil.forEachLine(readerFailingOnSecondRead(closed3), IOUtil.LineIterationOptions.builder().processThreads(0).build(), l -> {
                }));
        assertFalse(closed3.get());
    }

    @Test
    public void testForEachLine_LeavesTheCallersBufferedReaderOpenWhenAReadFails() {
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicInteger calls = new AtomicInteger();
        final BufferedReader failing = new BufferedReader(new StringReader("a\nb\nc\n")) {
            @Override
            public String readLine() throws IOException {
                if (calls.incrementAndGet() == 3) {
                    throw new IOException("disk on fire");
                }

                return super.readLine();
            }

            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        final List<String> seen = new ArrayList<>();
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(failing, seen::add));
        assertEquals(Arrays.asList("a", "b"), seen);
        assertFalse(closed.get(), "a caller-owned BufferedReader must not be closed on the failure path");
    }

    @Test
    public void testForEachLine_LeavesTheCallersInputStreamOpenWhenAReadFails() {
        final AtomicBoolean closed = new AtomicBoolean();
        final InputStream failing = new InputStream() {
            private int calls = 0;

            @Override
            public int read() throws IOException {
                throw new IOException("disk on fire");
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (++calls == 1) {
                    final byte[] s = "l1\nl2\n".getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(s, 0, b, off, s.length);
                    return s.length;
                }

                throw new IOException("disk on fire");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        final List<String> seen = new ArrayList<>();
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(failing, seen::add));
        assertEquals(Arrays.asList("l1", "l2"), seen);
        assertFalse(closed.get(), "the caller's stream must not be closed on the failure path");
    }

    @Test
    public void testForEachLine_StillDeliversEveryLineAndLeavesAHealthySourceOpen() {
        final AtomicBoolean closed = new AtomicBoolean();
        final Reader ok = new StringReader("x\ny") {
            @Override
            public void close() {
                closed.set(true);
                super.close();
            }
        };
        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(ok, seen::add);
        assertEquals(Arrays.asList("x", "y"), seen);
        assertFalse(closed.get());

        final List<String> seen2 = new ArrayList<>();
        IOUtil.forEachLine(new BufferedReader(new StringReader("p\nq\nr")), 1, 1, seen2::add);
        assertEquals(Collections.singletonList("q"), seen2);

        final List<String> seen3 = new ArrayList<>();
        IOUtil.forEachLine(new java.io.ByteArrayInputStream("日本\n😀".getBytes(StandardCharsets.UTF_8)), seen3::add);
        assertEquals(Arrays.asList("日本", "😀"), seen3);
    }

    // ------------------------------------------------------------------------------------------------
    // C-006: moveToDirectory naming and missing-source contract
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMoveToDirectory_KeepsTheSourceNameOfADotTerminatedPath() throws IOException {
        final File b = dir("c006/a/b");
        Files.writeString(new File(b, "f.txt").toPath(), "F", StandardCharsets.UTF_8);
        final File dest = tempDir.resolve("c006/x/dest").toFile();

        IOUtil.moveToDirectory(new File(b, "."), dest, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(new File(dest, "b").isDirectory(), "the moved directory keeps its own name, as copyToDirectory does");
        assertEquals("F", read(new File(dest, "b/f.txt")));
        assertFalse(b.exists());
        assertFalse(new File(dest, "f.txt").exists(), "the contents must not have been spilled into the destination directory itself");
    }

    @Test
    public void testMoveToDirectory_ReportsAMissingSourceAsFileNotFound() {
        final File missing = tempDir.resolve("c006-missing").toFile();
        final File dest = tempDir.resolve("c006-dest").toFile();

        assertThrows(FileNotFoundException.class, () -> IOUtil.moveToDirectory(missing, dest));
        assertThrows(FileNotFoundException.class, () -> IOUtil.moveToDirectory(missing, dest, StandardCopyOption.REPLACE_EXISTING));
        assertFalse(dest.exists(), "a rejected call never creates the destination directory");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(null, dest));
    }

    @Test
    public void testMoveToDirectory_MovesADanglingLinkItself() throws IOException {
        final Path link = tempDir.resolve("c006-dangling");

        try {
            Files.createSymbolicLink(link, tempDir.resolve("c006-gone"));
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("This platform cannot create symbolic links here: " + e);
        }

        final File dest = dir("c006-linkdest");
        IOUtil.moveToDirectory(link.toFile(), dest);

        assertTrue(Files.isSymbolicLink(dest.toPath().resolve("c006-dangling")));
        assertFalse(Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    // ------------------------------------------------------------------------------------------------
    // C-007: split / splitBySize
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSplitBySize_ValidatesItsArgumentsBeforeTouchingTheFile() {
        final File missing = tempDir.resolve("c007-missing.bin").toFile();
        final File out = tempDir.resolve("c007-out").toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(missing, 0, out));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(missing, -5, out));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.split(missing, 0, out));
        assertThrows(FileNotFoundException.class, () -> IOUtil.splitBySize(missing, 10, out));
        assertThrows(FileNotFoundException.class, () -> IOUtil.split(missing, 3, out));
        assertFalse(out.exists(), "a rejected call never creates the destination directory");
    }

    @Test
    public void testSplit_OfAnEmptyFileUsesTheRequestedSuffixWidth() throws IOException {
        final File empty = write("empty.bin", "");
        final File wide = dir("c007-wide");
        IOUtil.split(empty, 100000, wide);
        assertEquals(Collections.singletonList("empty.bin_000001"), Arrays.asList(wide.list()));
        assertEquals(0L, new File(wide, "empty.bin_000001").length());

        final File narrow = dir("c007-narrow");
        IOUtil.split(empty, 3, narrow);
        assertEquals(Collections.singletonList("empty.bin_0001"), Arrays.asList(narrow.list()));

        // an existing part under the generated name is truncated, as documented
        Files.writeString(new File(narrow, "empty.bin_0001").toPath(), "stale", StandardCharsets.UTF_8);
        IOUtil.split(empty, 3, narrow);
        assertEquals(0L, new File(narrow, "empty.bin_0001").length());
    }

    // ------------------------------------------------------------------------------------------------
    // C-008: a destination whose parent is a regular file is the documented environment failure
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testADestinationWhoseParentIsARegularFileIsTheDocumentedCannotCreateFailure() throws IOException {
        final File parentFile = write("c008-parent.txt", "x");
        final File child = new File(parentFile, "child.txt");
        final File src = write("c008-src.txt", "data");

        // every File destination reports the parent it could not create, whether the blocker is the parent itself
        // or an ancestor; none of them reads it as a wrong-kind argument, and the blocking file is left alone
        for (final File dest : new File[] { child, new File(parentFile, "sub" + File.separator + "dest.txt") }) {
            assertTrue(assertThrows(IOException.class, () -> IOUtil.write("x", dest)).getMessage().contains("Failed to create parent directory"));
            assertThrows(IOException.class, () -> IOUtil.append("x", dest));
            assertThrows(IOException.class, () -> IOUtil.append("", dest));
            assertThrows(IOException.class, () -> IOUtil.writeLine("x", dest));
            assertThrows(IOException.class, () -> IOUtil.copyFile(src, dest));
            assertThrows(UncheckedIOException.class, () -> IOUtil.newFileOutputStream(dest));
            assertThrows(UncheckedIOException.class, () -> IOUtil.newFileWriter(dest));
            assertThrows(UncheckedIOException.class, () -> IOUtil.createFileIfNotExists(dest));
            assertThrows(UncheckedIOException.class, () -> IOUtil.touch(dest));
        }

        assertEquals("x", read(parentFile), "the blocking file is left alone");
        assertFalse(child.exists());
    }

    // ------------------------------------------------------------------------------------------------
    // C-001: Windows directory junctions (mklink /J needs no privilege, unlike a symbolic link)
    // ------------------------------------------------------------------------------------------------

    /** Creates a junction {@code link -> target} with {@code mklink /J}, or aborts the test where that is impossible. */
    private static void junctionOrSkip(final File link, final File target) {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "directory junctions exist on Windows only");

        try {
            final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes();
            Assumptions.assumeTrue(p.waitFor() == 0 && Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS), "mklink /J is not available here");
        } catch (final IOException | InterruptedException e) {
            Assumptions.abort("cannot create a junction here: " + e);
        }
    }

    private File tree(final String name) throws IOException {
        final File real = dir(name);
        Files.writeString(new File(real, "a.txt").toPath(), "A", StandardCharsets.UTF_8);
        assertTrue(new File(real, "sub").mkdir());
        Files.writeString(new File(real, "sub/b.txt").toPath(), "BB", StandardCharsets.UTF_8);
        return real;
    }

    @Test
    public void testDeletingAJunctionRemovesTheJunctionAndNothingBehindIt() throws IOException {
        final File real = tree("c001-real");
        final File link = tempDir.resolve("c001-link").toFile();
        junctionOrSkip(link, real);

        assertTrue(IOUtil.deleteRecursivelyIfExists(link));
        assertFalse(Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS), "the junction itself is unlinked");
        assertEquals("A", read(new File(real, "a.txt")));
        assertEquals("BB", read(new File(real, "sub/b.txt")));

        // a tree that CONTAINS a junction to somewhere else: the tree goes, the somewhere else stays
        final File outside = tree("c001-outside");
        final File holder = dir("c001-holder");
        Files.writeString(new File(holder, "own.txt").toPath(), "own", StandardCharsets.UTF_8);
        junctionOrSkip(new File(holder, "j"), outside);
        assertTrue(IOUtil.deleteRecursivelyIfExists(holder));
        assertFalse(holder.exists());
        assertEquals("A", read(new File(outside, "a.txt")));
        assertEquals("BB", read(new File(outside, "sub/b.txt")));

        // deleteFilesFromDirectory: the junction is unlinked, never emptied
        final File outside2 = tree("c001-outside2");
        final File holder2 = dir("c001-holder2");
        junctionOrSkip(new File(holder2, "j"), outside2);
        assertTrue(IOUtil.deleteFilesFromDirectory(holder2));
        assertEquals(0, holder2.list().length);
        assertEquals(2, IOUtil.listFiles(outside2, true, true).size());

        // a junction handed in as the ROOT is refused, like a symbolic-link root, and its target is not emptied
        final File rootLink = tempDir.resolve("c001-rootlink").toFile();
        junctionOrSkip(rootLink, outside2);
        assertFalse(IOUtil.deleteFilesFromDirectory(rootLink));
        assertEquals(2, IOUtil.listFiles(outside2, true, true).size());
    }

    @Test
    public void testListingSizingAndZippingNeverDescendIntoAJunction() throws IOException {
        final File real = tree("c001-list-real");
        final File holder = dir("c001-list-holder");
        Files.writeString(new File(holder, "own.txt").toPath(), "own", StandardCharsets.UTF_8);
        junctionOrSkip(new File(holder, "j"), real);

        assertEquals(1, IOUtil.listFiles(holder, true, true).size(), "only own.txt: nothing through the junction");
        assertEquals(2, IOUtil.listFiles(holder, true, false).size(), "own.txt and the junction entry itself");
        assertEquals(2, IOUtil.walk(holder, true, false).count());
        assertEquals(3L, IOUtil.sizeOfDirectory(holder), "own.txt only; the junction's target is not counted");
        assertEquals(java.math.BigInteger.valueOf(3), IOUtil.sizeOfDirectoryAsBigInteger(holder));
        assertEquals(3L, IOUtil.sizeOf(holder));

        // a cyclic junction is harmless to the listing family
        junctionOrSkip(new File(real, "loop"), real);
        assertEquals(4, IOUtil.listFiles(real, true, false).size(), "a.txt, sub, sub/b.txt and the loop entry");
        assertEquals(3L, IOUtil.sizeOfDirectory(real));

        // zip leaves a nested junction out, as it leaves out a nested directory link
        final File zipped = tempDir.resolve("c001-holder.zip").toFile();
        IOUtil.zip(holder, zipped);
        final File unz = dir("c001-unz");
        IOUtil.unzip(zipped, unz);
        assertEquals(Collections.singletonList("own.txt"), IOUtil.listFiles(new File(unz, holder.getName()), true, true).stream().map(File::getName).toList());
        assertFalse(new File(new File(unz, holder.getName()), "j").exists());
    }

    @Test
    public void testADanglingJunctionIsUnlinkedAndAJunctionMovesAsItself() throws IOException {
        final File target = dir("c001-dangling-target");
        final File link = tempDir.resolve("c001-dangling").toFile();
        junctionOrSkip(link, target);
        assertTrue(target.delete());
        assertFalse(link.exists(), "File.exists follows the junction and finds nothing");

        assertTrue(IOUtil.deleteIfExists(link));
        assertFalse(Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));

        final File real = tree("c001-move-real");
        final File link2 = tempDir.resolve("c001-move-link").toFile();
        junctionOrSkip(link2, real);
        final File dest = dir("c001-move-dest");
        IOUtil.moveToDirectory(link2, dest);
        final File moved = new File(dest, "c001-move-link");
        assertTrue(Files.exists(moved.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertEquals("A", read(new File(real, "a.txt")), "the target is untouched by the move");
        assertTrue(IOUtil.deleteRecursivelyIfExists(moved));
        assertEquals("A", read(new File(real, "a.txt")));
    }

    @Test
    public void testUnzip_RejectsAnEntryThatEscapesThroughAJunctionInsideTheTarget() throws IOException {
        final File outside = dir("c023-outside");
        final File target = dir("c023-target");
        junctionOrSkip(new File(target, "j"), outside);

        final File zipped = tempDir.resolve("c023.zip").toFile();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("j/evil.txt"));
            zos.write("evil".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target));
        assertTrue(e.getMessage().contains("outside of the target dir"), e.getMessage());
        assertFalse(new File(outside, "evil.txt").exists(), "nothing may be written behind the junction");
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 2: C-025 null source stays a bad argument; C-026 a destination failure never re-drives the buffer
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testForEachLine_ReportsANullSourceAsABadArgument() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((Reader) null, l -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((Reader) null, 0, 1, l -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((Reader) null, IOUtil.LineIterationOptions.builder().build(), l -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((InputStream) null, l -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((InputStream) null, 0, 1, l -> {
        }));
        assertThrows(IllegalArgumentException.class,
                () -> IOUtil.forEachLine((InputStream) null, IOUtil.LineIterationOptions.builder().processThreads(2).build(), l -> {
                }, () -> {
                }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((File) null, l -> {
        }));
    }

    @Test
    public void testWriteLines_DoesNotResendWhatAFailingDestinationAlreadyConsumed() {
        final StringBuilder sink = new StringBuilder();
        final Writer flaky = new Writer() {
            private boolean failed;

            @Override
            public void write(final char[] cbuf, final int off, final int len) {
                if (!failed) {
                    failed = true;
                    sink.append(cbuf, off, Math.min(3, len));
                    throw new IllegalStateException("sink broke after 3 chars");
                }

                sink.append(cbuf, off, len);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(Arrays.asList("abcdef", "ghijkl"), flaky, true));
        assertEquals("abc", sink.toString(), "the three consumed characters must not be resent, and nothing may follow the failure");

        final StringBuilder sink2 = new StringBuilder();
        final Writer flaky2 = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) {
                sink2.append(cbuf, off, Math.min(3, len));
                throw new IllegalStateException("sink broke");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final Iterator<String> it = Arrays.asList("abcdef", "ghijkl").iterator();
        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(it, flaky2));
        assertEquals("abc", sink2.toString());

        // enough lines to force the pooled buffer to flush inside the loop: the write that fails must be
        // the buffered head, and nothing after the failure may reach the sink
        final StringBuilder sink3 = new StringBuilder();
        final AtomicInteger writes = new AtomicInteger();
        final Writer flaky3 = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) {
                writes.incrementAndGet();
                sink3.append(cbuf, off, Math.min(3, len));
                throw new IllegalStateException("sink broke");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final List<String> many = new ArrayList<>();

        for (int i = 0; i < 20_000; i++) {
            many.add("line-" + i);
        }

        assertThrows(IllegalStateException.class, () -> IOUtil.writeLines(many, flaky3, true));
        assertEquals("lin", sink3.toString());
        assertEquals(1, writes.get(), "the buffered head must be offered exactly once");
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 2: C-027 unspellable entry names, C-028 dangling junctions in a source tree,
    //          C-029 dot-segment sources, C-030 a dangling link at the copy destination
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testUnzip_ReportsAnEntryNameThePlatformCannotSpellAsAnIOException() throws IOException {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "only Windows refuses these characters in a file name");
        final File zipped = tempDir.resolve("c027.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("x/log-12:30.txt"));
            zos.write("late".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c027-target");
        // assertThrows(IOException) already proves the platform's unchecked InvalidPathException did not leak
        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target));
        assertTrue(e.getCause() instanceof java.nio.file.InvalidPathException, "the platform failure is kept as the cause");
        assertTrue(e.getMessage().contains("log-12:30.txt"), e.getMessage());
    }

    @Test
    public void testADanglingJunctionInsideASourceTreeIsLeftOutOfAnArchiveAndOfACopy() throws IOException {
        final File tree = dir("c028-tree");
        Files.writeString(new File(tree, "keep.txt").toPath(), "keep", StandardCharsets.UTF_8);
        final File gone = dir("c028-gone");
        junctionOrSkip(new File(tree, "dj"), gone);
        assertTrue(gone.delete());
        assertFalse(new File(tree, "dj").exists());

        final File zipped = tempDir.resolve("c028.zip").toFile();
        IOUtil.zip(tree, zipped);
        final File unz = dir("c028-unz");
        IOUtil.unzip(zipped, unz);
        assertEquals(Collections.singletonList("keep.txt"), IOUtil.listFiles(new File(unz, tree.getName()), true, false).stream().map(File::getName).toList());

        final File copied = IOUtil.copyToDirectory(tree, dir("c028-dest"));
        assertEquals("keep", read(new File(copied, "keep.txt")));
        assertFalse(Files.exists(copied.toPath().resolve("dj"), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertEquals(4L, IOUtil.sizeOf(tree));
    }

    @Test
    public void testADotTerminatedPathIsDeletedAndMovedAsTheDirectoryItNames() throws IOException {
        final File b = dir("c029/a/b");
        Files.writeString(new File(b, "f.txt").toPath(), "F", StandardCharsets.UTF_8);
        assertTrue(IOUtil.deleteRecursivelyIfExists(new File(b, ".")));
        assertFalse(b.exists());

        final File c = dir("c029/a/c");
        Files.writeString(new File(c, "g.txt").toPath(), "G", StandardCharsets.UTF_8);
        final File dest = tempDir.resolve("c029/dest").toFile();
        IOUtil.moveToDirectory(new File(c, "."), dest);
        assertEquals("G", read(new File(dest, "c/g.txt")));
        assertFalse(c.exists());

        final File emptyDir = dir("c029/a/e");
        assertTrue(IOUtil.deleteIfExists(new File(emptyDir, ".")));
        assertFalse(emptyDir.exists());
    }

    @Test
    public void testCopy_NeverWritesThroughADanglingLinkAlreadyAtTheDestination() throws IOException {
        final File src = write("c030-x.txt", "hello");
        final File dest = dir("c030-dest");
        final File elsewhere = dir("c030-elsewhere");
        junctionOrSkip(new File(dest, "c030-x.txt"), elsewhere);
        assertTrue(elsewhere.delete());
        assertFalse(new File(dest, "c030-x.txt").exists(), "File.exists follows the dangling link and finds nothing");

        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertFalse(elsewhere.exists(), "nothing was written behind the link");
        assertTrue(Files.exists(dest.toPath().resolve("c030-x.txt"), java.nio.file.LinkOption.NOFOLLOW_LINKS), "the entry is left as it was");
    }

    // ------------------------------------------------------------------------------------------------
    // C-031: an interrupted parallel forEachLine(InputStream) must not wait on the abandoned worker's read
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testInterruptedParallelForEachLineReturnsWithoutWaitingForTheWorkersRead() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] head = "first\nsecond\n".getBytes(StandardCharsets.UTF_8);
        // Yields two lines, then blocks - ignoring interruption, like a socket or a pipe - until released.
        final InputStream blocking = new InputStream() {
            private int pos = 0;

            @Override
            public int read() throws IOException {
                if (pos < head.length) {
                    return head[pos++] & 0xFF;
                }

                entered.countDown();

                boolean interrupted = false;

                while (true) {
                    try {
                        release.await();
                        break;
                    } catch (final InterruptedException e) {
                        interrupted = true;
                    }
                }

                if (interrupted) {
                    Thread.currentThread().interrupt();
                }

                return -1;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (pos < head.length) {
                    final int n = Math.min(len, head.length - pos);
                    System.arraycopy(head, pos, b, off, n);
                    pos += n;
                    return n;
                }

                return read();
            }
        };
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final AtomicBoolean flagAfterReturn = new AtomicBoolean();
        final Thread caller = new Thread(() -> {
            try {
                IOUtil.forEachLine(blocking, IOUtil.LineIterationOptions.builder().processThreads(1).queueSize(8).build(), line -> {
                }, () -> {
                });
            } catch (final Throwable t) {
                thrown.set(t);
            }

            flagAfterReturn.set(Thread.currentThread().isInterrupted());
        }, "c031-caller");

        try {
            caller.start();
            assertTrue(entered.await(10, TimeUnit.SECONDS), "the worker never reached the blocking read");
            Thread.sleep(100); // let the worker settle inside the decoder's read
            caller.interrupt();
            caller.join(TimeUnit.SECONDS.toMillis(8));
            assertFalse(caller.isAlive(), "the interrupted caller was pinned behind the worker's read");
            assertNotNull(thrown.get(), "the interrupt must surface as an exception");
            assertTrue(flagAfterReturn.get(), "the interrupt flag must survive the return");
        } finally {
            release.countDown();
            caller.join(TimeUnit.SECONDS.toMillis(8));
        }

        // the pool is healthy afterwards: an unrelated read gets a clean reader
        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(new java.io.ByteArrayInputStream("alpha\nbeta\ngamma\n".getBytes(StandardCharsets.UTF_8)), seen::add);
        assertEquals(Arrays.asList("alpha", "beta", "gamma"), seen);
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 3: C-048 dangling junction child of copyDirectory, C-049 nested special files, C-050 entry
    //          names the charset cannot encode, C-051 junction back into an ancestor, C-055 a file where
    //          a copied subdirectory would go, C-057 touch on a dangling link, C-058 unzip messages
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyDirectory_LeavesADanglingJunctionChildOut() throws IOException {
        final File src = dir("c048-src");
        Files.writeString(new File(src, "keep.txt").toPath(), "K", StandardCharsets.UTF_8);
        final File gone = dir("c048-gone");
        final File jd = new File(src, "jd");
        junctionOrSkip(jd, gone);
        assertTrue(gone.delete());
        assertFalse(jd.exists());

        final File dest = tempDir.resolve("c048-dest").toFile();
        IOUtil.copyDirectory(src, dest);

        assertEquals("K", Files.readString(new File(dest, "keep.txt").toPath(), StandardCharsets.UTF_8));
        assertFalse(Files.exists(new File(dest, "jd").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    private static File fifoOrSkip(final File fifo) {
        Assumptions.assumeFalse(IOUtil.IS_OS_WINDOWS, "named pipes are created with mkfifo on POSIX only");

        try {
            final Process p = new ProcessBuilder("mkfifo", fifo.getAbsolutePath()).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            Assumptions.assumeTrue(p.waitFor() == 0 && fifo.exists() && !fifo.isFile() && !fifo.isDirectory(), "mkfifo is not available here");
        } catch (final IOException | InterruptedException e) {
            Assumptions.abort("cannot create a FIFO here: " + e);
        }

        return fifo;
    }

    @Test
    public void testNestedSpecialFilesAreLeftOutOfCopiesAndArchives() throws IOException {
        final File src = dir("c049-src");
        Files.writeString(new File(src, "keep.txt").toPath(), "K", StandardCharsets.UTF_8);
        assertTrue(new File(src, "sub").mkdir());
        Files.writeString(new File(src, "sub/b.txt").toPath(), "B", StandardCharsets.UTF_8);
        fifoOrSkip(new File(src, "sub/pipe"));
        fifoOrSkip(new File(src, "toppipe"));

        // reading a FIFO blocks until a writer appears: each of these used to hang for ever
        final File dest = dir("c049-dest");
        final File copied = IOUtil.copyToDirectory(src, dest);
        assertEquals("K", Files.readString(new File(copied, "keep.txt").toPath(), StandardCharsets.UTF_8));
        assertEquals("B", Files.readString(new File(copied, "sub/b.txt").toPath(), StandardCharsets.UTF_8));
        assertFalse(new File(copied, "sub/pipe").exists());
        assertFalse(new File(copied, "toppipe").exists());

        final File dest2 = tempDir.resolve("c049-dest2").toFile();
        IOUtil.copyDirectory(src, dest2);
        assertTrue(new File(dest2, "sub/b.txt").isFile());
        assertFalse(new File(dest2, "sub/pipe").exists());
        assertFalse(new File(dest2, "toppipe").exists());

        final File zipped = tempDir.resolve("c049.zip").toFile();
        IOUtil.zip(src, zipped);
        final List<String> names = new ArrayList<>();

        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipped)) {
            zf.stream().forEach(e -> names.add(e.getName()));
        }

        assertTrue(names.contains("c049-src/keep.txt"), names.toString());
        assertTrue(names.contains("c049-src/sub/b.txt"), names.toString());
        assertFalse(names.stream().anyMatch(n -> n.contains("pipe")), names.toString());

        // named as the source itself, a special file is still a bad argument
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyToDirectory(new File(src, "toppipe"), dest));
    }

    @Test
    public void testZip_RefusesAnEntryNameTheCharsetCannotEncodeBeforeTouchingTheTarget() throws IOException {
        final File src = dir("c050-src");
        Files.writeString(new File(src, "plain.txt").toPath(), "P", StandardCharsets.UTF_8);
        Files.writeString(new File(src, "café-日本.txt").toPath(), "U", StandardCharsets.UTF_8);
        final File target = tempDir.resolve("c050.zip").toFile();
        Files.write(target.toPath(), new byte[] { 1, 2, 3, 4, 5, 6, 7 });

        final IOException e = assertThrows(IOException.class, () -> IOUtil.zip(src, target, StandardCharsets.US_ASCII));
        assertTrue(e.getMessage().contains("cannot be encoded"), e.getMessage());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, Files.readAllBytes(target.toPath()), "an existing target must be left unchanged");

        assertThrows(IOException.class,
                () -> IOUtil.zip(Arrays.asList(new File(src, "plain.txt"), new File(src, "café-日本.txt")), target, StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, Files.readAllBytes(target.toPath()));
        assertThrows(IOException.class, () -> IOUtil.zip(new File(src, "café-日本.txt"), target, StandardCharsets.ISO_8859_1));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, Files.readAllBytes(target.toPath()));

        // UTF-8 (explicit or default) and a charset that can spell the names write the archive as before
        IOUtil.zip(src, target, StandardCharsets.UTF_8);
        assertTrue(target.length() > 7);
        IOUtil.zip(new File(src, "plain.txt"), target, StandardCharsets.US_ASCII);
        assertTrue(target.length() > 7);
    }

    @Test
    public void testZip_UnencodableNameOnAFreshTargetIsStillReportedAsIOException() throws IOException {
        final File src = dir("c050b-src");
        Files.writeString(new File(src, "plain.txt").toPath(), "P", StandardCharsets.UTF_8);
        Files.writeString(new File(src, "café-日本.txt").toPath(), "U", StandardCharsets.UTF_8);

        // A target that does not exist yet has nothing to lose, so the source is not walked a second time to
        // pre-check the entry names: the write itself reports, and the IllegalArgumentException ZipOutputStream
        // raises for a name it cannot encode is translated to the documented IOException.
        final File fresh = tempDir.resolve("c050b.zip").toFile();
        assertFalse(fresh.exists());

        final IOException e = assertThrows(IOException.class, () -> IOUtil.zip(src, fresh, StandardCharsets.US_ASCII));
        assertTrue(e.getMessage().contains("cannot be encoded"), e.getMessage());
        assertTrue(e.getCause() instanceof IllegalArgumentException, String.valueOf(e.getCause()));

        // a single unencodable file as the source takes the same path
        final File fresh2 = tempDir.resolve("c050c.zip").toFile();
        final IOException e2 = assertThrows(IOException.class, () -> IOUtil.zip(new File(src, "café-日本.txt"), fresh2, StandardCharsets.US_ASCII));
        assertTrue(e2.getMessage().contains("cannot be encoded"), e2.getMessage());

        // and a source the charset can spell still writes to a fresh target
        final File fresh3 = tempDir.resolve("c050d.zip").toFile();
        IOUtil.zip(new File(src, "plain.txt"), fresh3, StandardCharsets.US_ASCII);
        assertTrue(fresh3.length() > 0);
    }

    @Test
    public void testJunctionLeadingBackIntoAnAncestorIsLeftOutInsteadOfCopyingForEver() throws IOException {
        final File src = dir("c051-src");
        Files.writeString(new File(src, "a.txt").toPath(), "A", StandardCharsets.UTF_8);
        assertTrue(new File(src, "sub").mkdir());
        junctionOrSkip(new File(src, "sub/up"), src); // sub/up -> src, an ancestor of sub
        junctionOrSkip(new File(src, "self"), src); // self -> src itself

        final File dest = dir("c051-dest");
        final File copied = IOUtil.copyToDirectory(src, dest);
        assertEquals("A", Files.readString(new File(copied, "a.txt").toPath(), StandardCharsets.UTF_8));
        assertTrue(new File(copied, "sub").isDirectory());
        assertFalse(Files.exists(new File(copied, "sub/up").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(new File(copied, "self").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));

        final File dest2 = tempDir.resolve("c051-dest2").toFile();
        IOUtil.copyDirectory(src, dest2);
        assertEquals("A", Files.readString(new File(dest2, "a.txt").toPath(), StandardCharsets.UTF_8));
        assertFalse(Files.exists(new File(dest2, "self").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(new File(dest2, "sub/up").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));

        // a junction to an unrelated directory is still followed and copied as a plain directory
        final File other = dir("c051-other");
        Files.writeString(new File(other, "o.txt").toPath(), "O", StandardCharsets.UTF_8);
        junctionOrSkip(new File(src, "sub/other"), other);
        final File dest3 = dir("c051-dest3");
        final File copied3 = IOUtil.copyToDirectory(src, dest3);
        assertEquals("O", Files.readString(new File(copied3, "sub/other/o.txt").toPath(), StandardCharsets.UTF_8));
    }

    @Test
    public void testAFileWhereACopiedSubdirectoryWouldGoIsAnOverwriteNotABadArgument() throws IOException {
        final File src = tree("c055-src");
        final File dest = dir("c055-dest");
        Files.writeString(new File(dest, "c055-src").toPath(), "in the way", StandardCharsets.UTF_8);

        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertEquals("in the way", Files.readString(new File(dest, "c055-src").toPath(), StandardCharsets.UTF_8));

        final File dest2 = dir("c055-dest2");
        assertTrue(new File(dest2, "c055-src").mkdir());
        Files.writeString(new File(dest2, "c055-src/sub").toPath(), "in the way", StandardCharsets.UTF_8);
        final IOException nested = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest2));
        assertTrue(nested.getMessage().contains("already exists"), nested.getMessage());
        assertEquals("in the way", Files.readString(new File(dest2, "c055-src/sub").toPath(), StandardCharsets.UTF_8));

        final File dest3 = dir("c055-dest3");
        Files.writeString(new File(dest3, "sub").toPath(), "in the way", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> IOUtil.copyDirectory(src, dest3));

        // the caller's own destDir being a file is still a bad argument
        final File notADir = tempDir.resolve("c055-file.txt").toFile();
        Files.writeString(notADir.toPath(), "x", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(src, notADir));
    }

    private static File danglingSymlinkOrSkip(final File link, final File missingTarget) {
        try {
            Files.createSymbolicLink(link.toPath(), missingTarget.toPath());
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("cannot create a symbolic link here: " + e);
        }

        return link;
    }

    @Test
    public void testTouch_CreatesTheTargetOfADanglingLinkLikeTouchDoes() throws IOException {
        final File target = tempDir.resolve("c057-target.txt").toFile();
        final File link = danglingSymlinkOrSkip(tempDir.resolve("c057-link.txt").toFile(), target);
        assertFalse(target.exists());

        assertFalse(IOUtil.createFileIfNotExists(link), "a dangling link is an existing entry");
        assertFalse(target.exists());

        IOUtil.touch(link);
        assertTrue(target.isFile());
        assertEquals(0, target.length());

        // and a second touch updates the time through the link
        assertTrue(target.setLastModified(1_000_000_000_000L));
        IOUtil.touch(link);
        assertTrue(target.lastModified() > 1_000_000_000_000L);
    }

    @Test
    public void testUnzip_NamesTheEntryWhenItsPathPassesThroughADanglingLink() throws IOException {
        final File zipped = tempDir.resolve("c058.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("j/evil.txt"));
            zos.write("evil".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c058-target");
        final File gone = tempDir.resolve("c058-gone").toFile();
        final File j = new File(target, "j");

        if (IOUtil.IS_OS_WINDOWS) {
            final File real = dir("c058-real");
            junctionOrSkip(j, real);
            assertTrue(real.delete());
        } else {
            danglingSymlinkOrSkip(j, gone);
        }

        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target));
        assertTrue(e.getMessage().contains("j/evil.txt"), e.getMessage());
        assertFalse(new File(gone, "evil.txt").exists());
    }

    @Test
    public void testUnzip_NamesTheEntryTheHostCannotSpellWithATrailingDotOrSpace() throws IOException {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "only Windows rejects a name ending in a dot or a space");
        final File zipped = tempDir.resolve("c058b.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("x/trail. "));
            zos.write("t".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c058b-target");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target));
        assertTrue(e.getMessage().contains("x/trail. "), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // C-052: a link already present at a nested destination path is an existing entry, never written through
    // C-062: an interrupted parallel forEachLine over a file must not close a reader a worker still holds
    // ------------------------------------------------------------------------------------------------

    private static void directoryLinkOrSkip(final File link, final File target) {
        if (IOUtil.IS_OS_WINDOWS) {
            junctionOrSkip(link, target);
        } else {
            try {
                Files.createSymbolicLink(link.toPath(), target.toPath());
            } catch (final IOException | UnsupportedOperationException | SecurityException e) {
                Assumptions.abort("cannot create a symbolic link here: " + e);
            }
        }
    }

    @Test
    public void testCopy_NeverWritesThroughALiveDirectoryLinkAtANestedDestination() throws IOException {
        final File src = tree("c052-src");
        final File outside = dir("c052-outside");

        // the top entry destDir/<name> is a link to a directory outside destDir
        final File dest = dir("c052-dest");
        directoryLinkOrSkip(new File(dest, "c052-src"), outside);
        final IOException top = assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest));
        assertTrue(top.getMessage().contains("already exists"), top.getMessage());
        assertEquals(0, outside.list().length, "nothing may land behind the link");

        // a deeper entry is a link back INTO the source tree
        final File dest2 = dir("c052-dest2");
        assertTrue(new File(dest2, "c052-src").mkdir());
        directoryLinkOrSkip(new File(dest2, "c052-src/sub"), src);
        assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, dest2));
        assertFalse(new File(src, "b.txt").exists(), "the copy must not write into the source tree");
        assertEquals(2, src.list().length, "the source tree is unchanged");

        // copyDirectory: a child link at the destination
        final File dest3 = dir("c052-dest3");
        directoryLinkOrSkip(new File(dest3, "sub"), outside);
        assertThrows(IOException.class, () -> IOUtil.copyDirectory(src, dest3));
        assertEquals(0, outside.list().length);

        // the caller's own destDir may be a link: it is followed, and the result reports the resolved path
        final File dest4 = dir("c052-dest4");
        final File destLink = tempDir.resolve("c052-destlink").toFile();
        directoryLinkOrSkip(destLink, dest4);
        final File copied = IOUtil.copyToDirectory(src, destLink);
        assertEquals("A", Files.readString(new File(copied, "a.txt").toPath(), StandardCharsets.UTF_8));
        assertTrue(new File(dest4, "c052-src/sub/b.txt").isFile());

        // an existing plain directory is still merged into
        final File dest5 = dir("c052-dest5");
        assertTrue(new File(dest5, "c052-src").mkdir());
        Files.writeString(new File(dest5, "c052-src/other.txt").toPath(), "O", StandardCharsets.UTF_8);
        IOUtil.copyToDirectory(src, dest5);
        assertTrue(new File(dest5, "c052-src/a.txt").isFile());
        assertTrue(new File(dest5, "c052-src/other.txt").isFile());
    }

    @Test
    public void testInterruptedParallelForEachLineOverAFifoReturnsWithoutWaitingForTheWorkersRead() throws Exception {
        final IOUtil.LineIterationOptions[] shapes = { IOUtil.LineIterationOptions.builder().processThreads(1).build(),
                IOUtil.LineIterationOptions.builder().readThreads(1).build(), IOUtil.LineIterationOptions.builder().readThreads(1).processThreads(2).build() };

        for (int i = 0; i < shapes.length; i++) {
            final IOUtil.LineIterationOptions options = shapes[i];
            // A FIFO of its own per shape: the reader a previous shape abandoned may still drain the pipe it was on.
            final File fifo = fifoOrSkip(tempDir.resolve("c062-" + i + ".fifo").toFile());

            // Opened read-write so that the readers' open() returns at once and their read blocks - like a socket
            // or a pipe whose writer has gone quiet. Whatever is written here is what the workers get to read.
            try (java.io.RandomAccessFile holder = new java.io.RandomAccessFile(fifo, "rw")) {
                holder.write("first\nsecond\n".getBytes(StandardCharsets.UTF_8));
                final AtomicReference<Throwable> thrown = new AtomicReference<>();
                final AtomicBoolean flagAfterReturn = new AtomicBoolean();
                final CountDownLatch sawALine = new CountDownLatch(1);
                final Thread caller = new Thread(() -> {
                    try {
                        IOUtil.forEachLine(Arrays.asList(fifo), options, line -> sawALine.countDown(), () -> {
                        });
                    } catch (final Throwable t) {
                        thrown.set(t);
                    }

                    flagAfterReturn.set(Thread.currentThread().isInterrupted());
                }, "c062-caller");

                caller.start();
                assertTrue(sawALine.await(10, TimeUnit.SECONDS), "the workers never read the lines");
                Thread.sleep(300); // let the reader settle inside its blocking read
                caller.interrupt();
                caller.join(TimeUnit.SECONDS.toMillis(8));
                final boolean pinned = caller.isAlive();

                // wake the abandoned reader whatever happened, so the JVM is never left with a blocked worker
                holder.write("first\nsecond\n".getBytes(StandardCharsets.UTF_8));
                caller.join(TimeUnit.SECONDS.toMillis(8));

                assertFalse(pinned, "the interrupted caller was pinned behind the worker's read: " + options);
                assertNotNull(thrown.get(), "the interrupt must surface as an exception: " + options);
                assertTrue(flagAfterReturn.get(), "the interrupt flag must survive the return: " + options);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 4: C-086 a counted parallel read returns while a reader thread is inside a blocking read,
    //          C-087 lone surrogates under UTF-8, C-088 Windows dot/space-terminated entry components,
    //          C-089 a link to a FIFO as a source, C-090 a nested FIFO under forEachLine(directory)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCountedParallelReadOfAFifoReturnsWhileTheReaderIsStillBlocked() throws Exception {
        final IOUtil.LineIterationOptions[] shapes = { IOUtil.LineIterationOptions.builder().readThreads(1).count(2).build(),
                IOUtil.LineIterationOptions.builder().readThreads(1).processThreads(1).count(2).build() };

        for (int i = 0; i < shapes.length; i++) {
            final IOUtil.LineIterationOptions options = shapes[i];
            final File fifo = fifoOrSkip(tempDir.resolve("c086-" + i + ".fifo").toFile());

            try (java.io.RandomAccessFile holder = new java.io.RandomAccessFile(fifo, "rw")) {
                holder.write("l1\nl2\nl3\nl4\nl5\n".getBytes(StandardCharsets.UTF_8));
                final List<String> seen = Collections.synchronizedList(new ArrayList<>());
                final AtomicReference<Throwable> thrown = new AtomicReference<>();
                final Thread caller = new Thread(() -> {
                    try {
                        IOUtil.forEachLine(fifo, options, seen::add);
                    } catch (final Throwable t) {
                        thrown.set(t);
                    }
                }, "c086-caller");

                caller.start();
                caller.join(TimeUnit.SECONDS.toMillis(8));
                final boolean pinned = caller.isAlive();
                holder.write("wake\n".getBytes(StandardCharsets.UTF_8)); // free the abandoned reader whatever happened
                caller.join(TimeUnit.SECONDS.toMillis(8));

                assertFalse(pinned, "a counted read was pinned behind the reader thread's blocking read: " + options);
                assertNull(thrown.get(), String.valueOf(thrown.get()));
                assertEquals(Arrays.asList("l1", "l2"), seen);
            }
        }
    }

    @Test
    public void testZip_RefusesANameUtf8CannotEncodeBeforeTouchingTheTarget() throws IOException {
        final File src = dir("c087-src");
        final File odd = new File(src, "bad\uD800.txt");

        try {
            Files.writeString(odd.toPath(), "x", StandardCharsets.UTF_8);
        } catch (final java.nio.file.InvalidPathException | IOException e) {
            Assumptions.abort("this file system does not take a lone surrogate in a name: " + e);
        }

        Files.writeString(new File(src, "fine.txt").toPath(), "F", StandardCharsets.UTF_8);
        final File target = tempDir.resolve("c087.zip").toFile();
        final byte[] before = { 9, 8, 7, 6, 5, 4, 3, 2 };
        Files.write(target.toPath(), before);

        final IOException e = assertThrows(IOException.class, () -> IOUtil.zip(src, target));
        assertTrue(e.getMessage().contains("cannot be encoded"), e.getMessage());
        assertArrayEquals(before, Files.readAllBytes(target.toPath()), "an existing target must be left unchanged");
        assertThrows(IOException.class, () -> IOUtil.zip(src, target, StandardCharsets.UTF_8));
        assertArrayEquals(before, Files.readAllBytes(target.toPath()));
        assertThrows(IOException.class, () -> IOUtil.zip(Arrays.asList(new File(src, "fine.txt"), odd), target));
        assertArrayEquals(before, Files.readAllBytes(target.toPath()));
        assertThrows(IOException.class, () -> IOUtil.zip(odd, target));
        assertArrayEquals(before, Files.readAllBytes(target.toPath()));

        // a tree without such a name still zips under the default charset
        IOUtil.zip(new File(src, "fine.txt"), target);
        assertTrue(target.length() > before.length);
    }

    @Test
    public void testUnzip_RejectsAnEntryComponentEndingInADotOrASpaceOnWindows() throws IOException {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "only Windows folds a trailing dot away");

        for (final String entry : new String[] { "a/...", "x.txt.", "a./x.txt", "b/....", "a /x.txt", "./ok/../keep. " }) {
            final File zipped = tempDir.resolve("c088-" + Math.abs(entry.hashCode()) + ".zip").toFile();

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
                zos.putNextEntry(new java.util.zip.ZipEntry(entry));
                zos.write("t".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            final File target = dir("c088-target-" + Math.abs(entry.hashCode()));
            final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target), entry);
            assertTrue(e.getMessage().contains(entry), entry + " -> " + e.getMessage());
            assertEquals(0, target.list().length, "nothing may be created for " + entry);
        }

        // "./x" and "a/../b" style components are not names and still extract / are still contained
        final File zipped = tempDir.resolve("c088-dot.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("./x.txt"));
            zos.write("X".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c088-dot-target");
        IOUtil.unzip(zipped, target);
        assertEquals("X", Files.readString(new File(target, "x.txt").toPath(), StandardCharsets.UTF_8));
    }

    @Test
    public void testALinkToAFifoIsRejectedLikeTheFifoItself() throws IOException {
        final File fifo = fifoOrSkip(tempDir.resolve("c089.fifo").toFile());
        final File link = tempDir.resolve("c089-link").toFile();

        try {
            Files.createSymbolicLink(link.toPath(), fifo.toPath());
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("cannot create a symbolic link here: " + e);
        }

        final File dest = dir("c089-dest");
        // each of these used to block for ever in open(), or (zip) to write an empty archive
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyToDirectory(link, dest));
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyFile(link, new File(dest, "copy")));
        assertThrows(FileNotFoundException.class, () -> IOUtil.zip(link, tempDir.resolve("c089.zip").toFile()));
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyToDirectory(fifo, dest));
        assertEquals(0, dest.list().length);
    }

    @Test
    public void testForEachLine_OverADirectoryLeavesANestedFifoOut() throws IOException {
        final File src = dir("c090-src");
        Files.writeString(new File(src, "a.txt").toPath(), "A1\nA2\n", StandardCharsets.UTF_8);
        assertTrue(new File(src, "sub").mkdir());
        Files.writeString(new File(src, "sub/b.txt").toPath(), "B1\n", StandardCharsets.UTF_8);
        fifoOrSkip(new File(src, "top.fifo"));
        fifoOrSkip(new File(src, "sub/nested.fifo"));

        // reading a FIFO with no writer blocks for ever: the run used to hang
        final List<String> seen = Collections.synchronizedList(new ArrayList<>());
        IOUtil.forEachLine(src, seen::add);
        Collections.sort(seen);
        assertEquals(Arrays.asList("A1", "A2", "B1"), seen);

        final List<String> seenParallel = Collections.synchronizedList(new ArrayList<>());
        IOUtil.forEachLine(src, IOUtil.LineIterationOptions.builder().readThreads(2).processThreads(2).build(), seenParallel::add);
        Collections.sort(seenParallel);
        assertEquals(Arrays.asList("A1", "A2", "B1"), seenParallel);
    }

    // ------------------------------------------------------------------------------------------------
    // C-085: junction cycles through siblings or other junctions, and junctions into the copy's own
    //        output, are left out instead of copying until the platform's limit
    // ------------------------------------------------------------------------------------------------

    private static int deepestNameCount(final File root) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(root.toPath())) {
            return walk.mapToInt(Path::getNameCount).max().orElse(0);
        }
    }

    @Test
    public void testJunctionCyclesAndJunctionsIntoTheOutputAreLeftOut() throws IOException {
        // a mutual cycle through two siblings: src/a/j -> src/b, src/b/k -> src/a
        final File src = dir("c085-src");
        assertTrue(new File(src, "a").mkdir());
        assertTrue(new File(src, "b").mkdir());
        Files.writeString(new File(src, "a/a.txt").toPath(), "A", StandardCharsets.UTF_8);
        Files.writeString(new File(src, "b/b.txt").toPath(), "B", StandardCharsets.UTF_8);
        junctionOrSkip(new File(src, "a/j"), new File(src, "b"));
        junctionOrSkip(new File(src, "b/k"), new File(src, "a"));

        final File dest = dir("c085-dest");
        final File copied = IOUtil.copyToDirectory(src, dest);
        assertEquals("A", Files.readString(new File(copied, "a/a.txt").toPath(), StandardCharsets.UTF_8));
        assertEquals("B", Files.readString(new File(copied, "b/b.txt").toPath(), StandardCharsets.UTF_8));
        // each junction is followed once (its target is not on the path yet) and left out the second time
        assertTrue(new File(copied, "a/j/b.txt").isFile());
        assertFalse(Files.exists(new File(copied, "a/j/k").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(new File(copied, "b/k/a.txt").isFile());
        assertFalse(Files.exists(new File(copied, "b/k/j").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(deepestNameCount(copied) <= copied.toPath().getNameCount() + 3, "the copy must stay shallow");

        final File dest1b = tempDir.resolve("c085-dest1b").toFile();
        IOUtil.copyDirectory(src, dest1b);
        assertTrue(new File(dest1b, "a/j/b.txt").isFile());
        assertFalse(new File(dest1b, "a/j/k").exists());
        assertTrue(deepestNameCount(dest1b) <= dest1b.toPath().getNameCount() + 3);

        // a logical ancestor through another junction: x/j -> y, y/back -> x
        final File x = dir("c085-x");
        final File y = dir("c085-y");
        Files.writeString(new File(x, "x.txt").toPath(), "X", StandardCharsets.UTF_8);
        Files.writeString(new File(y, "y.txt").toPath(), "Y", StandardCharsets.UTF_8);
        junctionOrSkip(new File(x, "j"), y);
        junctionOrSkip(new File(y, "back"), x);
        final File dest2 = dir("c085-dest2");
        final File copiedX = IOUtil.copyToDirectory(x, dest2);
        assertTrue(new File(copiedX, "j/y.txt").isFile());
        assertFalse(Files.exists(new File(copiedX, "j/back").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        final File dest2b = tempDir.resolve("c085-dest2b").toFile();
        IOUtil.copyDirectory(x, dest2b);
        assertTrue(new File(dest2b, "j/y.txt").isFile());
        assertFalse(new File(dest2b, "j/back").exists());

        // a junction pointing at the destination: the copy used to re-copy its own output
        final File src3 = dir("c085-src3");
        Files.writeString(new File(src3, "s.txt").toPath(), "S", StandardCharsets.UTF_8);
        final File dest3 = dir("c085-dest3");
        junctionOrSkip(new File(src3, "toDest"), dest3);
        final File copied3 = IOUtil.copyToDirectory(src3, dest3);
        assertTrue(new File(copied3, "s.txt").isFile());
        assertFalse(Files.exists(new File(copied3, "toDest").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(deepestNameCount(dest3) <= dest3.toPath().getNameCount() + 2);

        // copyDirectory with an immediate child junction to destDir: left out, the siblings copied
        final File dest3b = dir("c085-dest3b");
        junctionOrSkip(new File(src3, "toDest3b"), dest3b);
        IOUtil.copyDirectory(src3, dest3b);
        assertTrue(new File(dest3b, "s.txt").isFile());
        assertFalse(new File(dest3b, "toDest3b").exists());
        // toDest -> dest3 is unrelated to dest3b: followed once, finitely (dest3 holds the copy made above)
        assertTrue(new File(dest3b, "toDest/c085-src3/s.txt").isFile());
        assertFalse(new File(dest3b, "toDest/c085-src3/toDest").exists());

        // a junction into the copy's own output, dangling when the walk starts and live once dest/src4 exists
        final File src4 = dir("c085-src4");
        Files.writeString(new File(src4, "t.txt").toPath(), "T", StandardCharsets.UTF_8);
        final File dest4 = dir("c085-dest4");
        junctionOrSkip(new File(src4, "j"), new File(dest4, "c085-src4"));
        final File copied4 = IOUtil.copyToDirectory(src4, dest4);
        assertTrue(new File(copied4, "t.txt").isFile());
        assertFalse(Files.exists(new File(copied4, "j").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(deepestNameCount(dest4) <= dest4.toPath().getNameCount() + 2);

        // the destination reached through a junction inside the source
        final File src5 = dir("c085-src5");
        Files.writeString(new File(src5, "u.txt").toPath(), "U", StandardCharsets.UTF_8);
        final File outside = dir("c085-outside");
        junctionOrSkip(new File(src5, "destlink"), outside);
        final File copied5 = IOUtil.copyToDirectory(src5, new File(src5, "destlink"));
        assertTrue(new File(outside, "c085-src5/u.txt").isFile());
        assertFalse(Files.exists(new File(copied5, "destlink").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(deepestNameCount(outside) <= outside.toPath().getNameCount() + 2);

        // control: a junction to a descendant is followed once, so its target is copied twice, finitely
        final File src6 = dir("c085-src6");
        assertTrue(new File(src6, "a/b").mkdirs());
        Files.writeString(new File(src6, "a/b/v.txt").toPath(), "V", StandardCharsets.UTF_8);
        junctionOrSkip(new File(src6, "a/j"), new File(src6, "a/b"));
        final File copied6 = IOUtil.copyToDirectory(src6, dir("c085-dest6"));
        assertTrue(new File(copied6, "a/b/v.txt").isFile());
        assertTrue(new File(copied6, "a/j/v.txt").isFile());
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 5: C-110 a reparse point Java cannot follow inside a source tree, C-112 a file entry "x/."
    // ------------------------------------------------------------------------------------------------

    /** Writes a Linux symbolic link on this Windows volume through WSL, which Java sees as neither a link nor a file. */
    private static void wslSymlinkOrSkip(final File link, final String target) {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "the shape exists on a Windows volume only");
        final String abs = link.getAbsolutePath().replace('\\', '/');
        Assumptions.assumeTrue(abs.length() > 2 && abs.charAt(1) == ':', "needs a drive-letter path");
        final String wslPath = "/mnt/" + Character.toLowerCase(abs.charAt(0)) + abs.substring(2);

        try {
            final Process p = new ProcessBuilder("wsl", "-e", "ln", "-s", target, wslPath).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            Assumptions.assumeTrue(p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0, "WSL is not available here");
        } catch (final IOException | InterruptedException e) {
            Assumptions.abort("cannot run WSL here: " + e);
        }

        final Path path = link.toPath();
        Assumptions.assumeTrue(Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path) && !link.exists(),
                "this WSL wrote a link Java can follow; the shape under test is the one it cannot");
    }

    @Test
    public void testAReparsePointJavaCannotFollowInsideTheSourceIsLeftOutOfTheCopy() throws IOException {
        final File src = dir("c110-src");
        Files.writeString(new File(src, "a.txt").toPath(), "A", StandardCharsets.UTF_8);
        assertTrue(new File(src, "sub").mkdir());
        Files.writeString(new File(src, "sub/b.txt").toPath(), "B", StandardCharsets.UTF_8);
        wslSymlinkOrSkip(new File(src, "lxfile"), "a.txt");
        wslSymlinkOrSkip(new File(src, "sub/lxnested"), "b.txt");

        // each of these used to fail half-way with "The file cannot be accessed by the system"
        final File dest = dir("c110-dest");
        final File copied = IOUtil.copyToDirectory(src, dest);
        assertEquals("A", Files.readString(new File(copied, "a.txt").toPath(), StandardCharsets.UTF_8));
        assertEquals("B", Files.readString(new File(copied, "sub/b.txt").toPath(), StandardCharsets.UTF_8));
        assertFalse(Files.exists(new File(copied, "lxfile").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(new File(copied, "sub/lxnested").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));

        final File dest2 = tempDir.resolve("c110-dest2").toFile();
        IOUtil.copyDirectory(src, dest2);
        assertTrue(new File(dest2, "sub/b.txt").isFile());
        assertFalse(Files.exists(new File(dest2, "lxfile").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));

        // zip already left it out; the two families agree
        final File zipped = tempDir.resolve("c110.zip").toFile();
        IOUtil.zip(src, zipped);
        final List<String> names = new ArrayList<>();

        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipped)) {
            zf.stream().forEach(e -> names.add(e.getName()));
        }

        assertFalse(names.stream().anyMatch(n -> n.contains("lx")), names.toString());
        assertTrue(names.contains("c110-src/sub/b.txt"), names.toString());
    }

    @Test
    public void testUnzip_RefusesAFileEntryWhoseLastComponentIsADot() throws IOException {
        final File zipped = tempDir.resolve("c112.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("x/."));
            zos.write("t".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c112-target");
        final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zipped, target));
        assertTrue(e.getMessage().contains("x/."), e.getMessage());
        assertEquals(0, target.list().length, "nothing may be created");

        // a directory entry with the same shape still extracts as the directory it names
        final File zipped2 = tempDir.resolve("c112b.zip").toFile();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipped2.toPath()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("y/./"));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("y/./f.txt"));
            zos.write("F".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target2 = dir("c112-target2");
        IOUtil.unzip(zipped2, target2);
        assertTrue(new File(target2, "y").isDirectory());
        assertEquals("F", Files.readString(new File(target2, "y/f.txt").toPath(), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------
    // C-109: the copyDirectory cycle guard is seeded from canonical paths, so a subst drive spelling of
    //        the arguments cannot hide a junction to the destination
    // ------------------------------------------------------------------------------------------------

    /** Maps a free drive letter onto {@code dir} with {@code subst}, or aborts the test; returns the letter. */
    private static char substOrSkip(final File dir) {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "subst drives exist on Windows only");

        for (char letter = 'Z'; letter >= 'M'; letter--) {
            if (new File(letter + ":\\").exists()) {
                continue;
            }

            try {
                final Process p = new ProcessBuilder("cmd", "/c", "subst", letter + ":", dir.getAbsolutePath()).redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();

                if (p.waitFor() == 0 && new File(letter + ":\\").isDirectory()) {
                    return letter;
                }
            } catch (final IOException | InterruptedException e) {
                Assumptions.abort("cannot run subst here: " + e);
            }
        }

        Assumptions.abort("no free drive letter for subst");
        return 0;
    }

    private static void unsubst(final char letter) {
        try {
            new ProcessBuilder("cmd", "/c", "subst", letter + ":", "/D").redirectErrorStream(true).start().waitFor();
        } catch (final IOException | InterruptedException e) {
            // best effort
        }
    }

    @Test
    public void testCopyDirectory_GuardSeesThroughASubstDriveSpelling() throws IOException {
        final File base = dir("c109-base");
        final File src = new File(base, "src");
        final File dest = new File(base, "dest");
        assertTrue(new File(src, "sub").mkdirs());
        assertTrue(dest.mkdir());
        Files.writeString(new File(src, "a.txt").toPath(), "A", StandardCharsets.UTF_8);
        Files.writeString(new File(src, "sub/b.txt").toPath(), "B", StandardCharsets.UTF_8);
        // the junctions' targets are spelled on the real volume, as mklink requires
        junctionOrSkip(new File(src, "sub/j"), dest);
        junctionOrSkip(new File(src, "toDest"), dest);

        final char letter = substOrSkip(base);

        try {
            final File aliasSrc = new File(letter + ":\\src");
            final File aliasDest = new File(letter + ":\\dest");
            Assumptions.assumeTrue(aliasSrc.isDirectory() && aliasDest.isDirectory(), "the subst drive is not visible");

            IOUtil.copyDirectory(aliasSrc, aliasDest);

            assertEquals("A", Files.readString(new File(dest, "a.txt").toPath(), StandardCharsets.UTF_8));
            assertEquals("B", Files.readString(new File(dest, "sub/b.txt").toPath(), StandardCharsets.UTF_8));
            assertFalse(Files.exists(new File(dest, "sub/j").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS),
                    "the junction to the destination must be left out");
            assertFalse(Files.exists(new File(dest, "toDest").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
            assertTrue(deepestNameCount(dest) <= dest.toPath().getNameCount() + 2, "the copy must stay shallow");
        } finally {
            unsubst(letter);
        }
    }

    // C-109 (UNC spelling): the guard learns an alias by identity when the roots differ, so a UNC-spelled source
    // with a junction to the real-spelled destination is caught under both entry points

    @Test
    public void testCopy_GuardSeesThroughAUncSpellingOfTheSource() throws IOException {
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "UNC administrative shares exist on Windows only");
        final File base = dir("c109u-base");
        final String abs = base.getAbsolutePath();
        Assumptions.assumeTrue(abs.length() > 2 && abs.charAt(1) == ':', "needs a drive-letter path");
        final File uncBase = new File("\\\\localhost\\" + abs.charAt(0) + "$" + abs.substring(2));
        Assumptions.assumeTrue(uncBase.isDirectory(), "the administrative share is not reachable here");

        final File src = new File(base, "src");
        final File dest = new File(base, "dest");
        assertTrue(new File(src, "sub").mkdirs());
        assertTrue(dest.mkdir());
        Files.writeString(new File(src, "a.txt").toPath(), "A", StandardCharsets.UTF_8);
        Files.writeString(new File(src, "sub/b.txt").toPath(), "B", StandardCharsets.UTF_8);
        junctionOrSkip(new File(src, "sub/j"), dest); // real spelling, as mklink requires
        junctionOrSkip(new File(src, "back"), src);

        final File uncSrc = new File(uncBase, "src");
        final File copied = IOUtil.copyToDirectory(uncSrc, dest);
        assertEquals("A", Files.readString(new File(copied, "a.txt").toPath(), StandardCharsets.UTF_8));
        assertEquals("B", Files.readString(new File(copied, "sub/b.txt").toPath(), StandardCharsets.UTF_8));
        assertFalse(Files.exists(new File(copied, "sub/j").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS),
                "the junction to the destination must be left out");
        assertFalse(Files.exists(new File(copied, "back").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS),
                "the junction back to the source must be left out");
        assertTrue(deepestNameCount(dest) <= dest.toPath().getNameCount() + 3, "the copy must stay shallow");

        final File dest2 = new File(base, "dest2");
        assertTrue(dest2.mkdir());
        junctionOrSkip(new File(src, "sub/j2"), dest2);
        IOUtil.copyDirectory(uncSrc, new File(uncBase, "dest2"));
        assertTrue(new File(dest2, "sub/b.txt").isFile());
        assertFalse(Files.exists(new File(dest2, "sub/j2").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(new File(dest2, "back").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        // sub/j -> dest is unrelated to dest2 for this copy: followed once, finitely (dest holds the first copy)
        assertTrue(new File(dest2, "sub/j/src/sub/b.txt").isFile());
        assertFalse(Files.exists(new File(dest2, "sub/j/src/sub/j").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertTrue(deepestNameCount(dest2) <= dest2.toPath().getNameCount() + 6);
    }
}
