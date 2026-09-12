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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IOUtil.LineIterationOptions;

/**
 * Regression tests for the 8th independent review pass over {@link IOUtil} (2026-08-31, run "c").
 *
 * <p>Covers, in order: the {@code skip(InputStream, long)} seek fast path (B1), {@code transfer(..)} reporting a
 * short transfer as success (B2), {@code contentEqualsIgnoreEOL}'s charset-validation order (B3),
 * {@code walk} vs {@code listFiles} traversal order (D2), {@code split}'s documented size distribution (J1),
 * and {@code copyFile}'s parent-directory diagnostics (B5).
 */
public class IOUtilSkipAndTransferTest extends TestBase {

    @TempDir
    File tempDir;

    private File write(final String name, final byte[] content) throws IOException {
        final File file = new File(tempDir, name);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content);
        return file;
    }

    private File write(final String name, final String content) throws IOException {
        return write(name, content.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------
    // B1: skip(InputStream, long) must never call the stream's own skip(..)
    // ------------------------------------------------------------------------------------------------

    /**
     * A stream whose {@code read} works but whose {@code skip} fails, exactly as {@code FileInputStream.skip}
     * does on a Unix FIFO/pipe (lseek answers ESPIPE). {@code available()} deliberately reports the whole
     * remainder, which is what used to steer the old fast path into calling {@code skip}.
     */
    private static final class UnseekableStream extends InputStream {
        private final byte[] data;
        private int pos;
        int skipCalls;

        UnseekableStream(final byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xff : -1;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            if (pos >= data.length) {
                return -1;
            }
            final int n = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public int available() {
            return data.length - pos;
        }

        @Override
        public long skip(final long n) throws IOException {
            skipCalls++;
            throw new IOException("Illegal seek");
        }
    }

    /** A {@code skip} that advances part of the way and only then fails - the case a catch-and-retry would over-skip. */
    private static final class PartiallyAdvancingSkipStream extends FilterInputStream {
        PartiallyAdvancingSkipStream(final byte[] data) {
            super(new ByteArrayInputStream(data));
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public long skip(final long n) throws IOException {
            in.skip(n / 2); // moves half way...
            throw new IOException("Illegal seek"); // ...and only then fails
        }
    }

    private static byte[] alphabet(final int len) {
        final byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) ('a' + (i % 26));
        }
        return data;
    }

    @Test
    public void testSkipDoesNotUseTheStreamsOwnSkipOnANonSeekableSource() throws IOException {
        final byte[] data = alphabet(100_000);
        final UnseekableStream is = new UnseekableStream(data);

        // Used to throw IOException("Illegal seek"): available() was >= 8192, so the old fast path called skip().
        assertEquals(10_000, IOUtil.skip(is, 10_000));
        assertEquals(0, is.skipCalls, "IOUtil.skip must not call the stream's own skip(..)");
        assertEquals(data[10_000], (byte) is.read());
    }

    @Test
    public void testSkipIsNotSizeDependentOnANonSeekableSource() throws IOException {
        // The old failure appeared only once >= 8192 bytes were buffered, so the same source worked or threw
        // depending on how much data happened to be available. Both sides of that threshold must now behave.
        for (final int len : new int[] { 10, 8191, 8192, 8193, 100_000 }) {
            final byte[] data = alphabet(len);
            final long want = Math.min(len, 9000);
            assertEquals(want, IOUtil.skip(new UnseekableStream(data), want), "len=" + len);
        }
    }

    @Test
    public void testSkipDoesNotOverSkipWhenSkipAdvancesPartiallyThenThrows() throws IOException {
        // Catching the IOException and re-reading the full remainder would silently skip 2x here.
        final byte[] data = alphabet(50_000);
        final PartiallyAdvancingSkipStream is = new PartiallyAdvancingSkipStream(data);

        assertEquals(10_000, IOUtil.skip(is, 10_000));
        assertEquals(data[10_000], (byte) is.read(), "position must be exactly 10_000, not further");
    }

    @Test
    public void testByteAndCharSlicersAgreeOnANonSeekableSource() throws IOException {
        // readBytes/write/skipFully used to throw where readChars/readToString on the same stream succeeded,
        // because only the Reader side restricted its fast path by type.
        final byte[] data = alphabet(100_000);
        final String expected = new String(data, 10_000, 5, StandardCharsets.UTF_8);

        assertArrayEquals(Arrays.copyOfRange(data, 10_000, 10_005), IOUtil.readBytes(new UnseekableStream(data), 10_000, 5));
        assertEquals(expected, new String(IOUtil.readChars(new UnseekableStream(data), 10_000, 5)));
        assertEquals(expected, IOUtil.readToString(new UnseekableStream(data), 10_000, 5));

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(5, IOUtil.write(new UnseekableStream(data), 10_000L, 5L, out));
        assertArrayEquals(Arrays.copyOfRange(data, 10_000, 10_005), out.toByteArray());

        IOUtil.skipFully(new UnseekableStream(data), 10_000); // must not throw
    }

    @Test
    public void testSkipStillSeeksARegularFileAndStaysExact() throws IOException {
        final byte[] data = alphabet(200_000);
        final File file = write("skip-seek.bin", data);

        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(150_000, IOUtil.skip(is, 150_000));
            // The channel move must be visible to the stream itself.
            assertArrayEquals(Arrays.copyOfRange(data, 150_000, 200_000), IOUtil.readAllBytes(is));
        }

        // Bounded by the file size: never reports bytes past the end, unlike FileInputStream.skip.
        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(200_000, IOUtil.skip(is, 999_999));
            assertEquals(-1, is.read());
        }

        // An empty file has size 0, which is the same probe the FIFO case answers - it must simply skip nothing.
        final File empty = write("skip-empty.bin", new byte[0]);
        try (FileInputStream is = new FileInputStream(empty)) {
            assertEquals(0, IOUtil.skip(is, 100));
        }
    }

    @Test
    public void testSkipHandlesAnEnormousCountWithoutOverflowing() throws IOException {
        final byte[] data = alphabet(5000);
        final File file = write("skip-overflow.bin", data);

        // position + remain would wrap negative here, and FileChannel.position(negative) throws outright.
        // Long.MAX_VALUE is not exotic: readAllBytes(..) and the unbounded write(..) forms pass it through.
        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(data[0], (byte) is.read()); // move the position off zero: that is what makes the sum overflow
            assertEquals(4999, IOUtil.skip(is, Long.MAX_VALUE));
            assertEquals(-1, is.read());
        }

        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(5000, IOUtil.skip(is, Long.MAX_VALUE));
        }

        // The same count through a sliced read, which is how a caller reaches it in practice.
        try (FileInputStream is = new FileInputStream(file)) {
            assertEquals(0, IOUtil.readBytes(is, Long.MAX_VALUE, 10).length);
        }
    }

    @Test
    public void testSkipRemainsExactAcrossStreamShapes() throws IOException {
        final byte[] data = alphabet(40_000);
        final File file = write("skip-shapes.bin", data);

        assertEquals(1234, IOUtil.skip(new ByteArrayInputStream(data), 1234));
        assertEquals(data.length, IOUtil.skip(new ByteArrayInputStream(data), data.length + 500));

        try (InputStream is = new java.io.BufferedInputStream(new FileInputStream(file))) {
            assertEquals(30_000, IOUtil.skip(is, 30_000));
            assertEquals(data[30_000], (byte) is.read());
        }

        assertEquals(500, IOUtil.skip(new StringReader(new String(data, StandardCharsets.ISO_8859_1)), 500));
    }

    // ------------------------------------------------------------------------------------------------
    // B2: transfer(..) must not report a short transfer as success
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testTransferMovesEveryByteAndLeavesTheDestinationPositioned() throws IOException {
        final byte[] data = alphabet(70_000);
        final File src = write("xfer-src.bin", data);
        final File dst = write("xfer-dst.bin", new byte[0]);

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            final FileChannel dc = out.getChannel();
            assertEquals(data.length, IOUtil.transfer(in.getChannel(), dc));
            assertEquals(data.length, dc.position());
        }

        assertArrayEquals(data, Files.readAllBytes(dst.toPath()));
    }

    @Test
    public void testTransferHonoursBothChannelPositions() throws IOException {
        final byte[] data = alphabet(1000);
        final File src = write("xfer-pos-src.bin", data);
        final File dst = write("xfer-pos-dst.bin", "HEADER".getBytes(StandardCharsets.UTF_8));

        try (RandomAccessFile in = new RandomAccessFile(src, "r");
             RandomAccessFile out = new RandomAccessFile(dst, "rw")) {
            in.getChannel().position(400);
            out.getChannel().position(6); // just after "HEADER"

            assertEquals(600, IOUtil.transfer(in.getChannel(), out.getChannel()));
            assertEquals(606, out.getChannel().position());
        }

        final byte[] written = Files.readAllBytes(dst.toPath());
        assertEquals(606, written.length);
        assertArrayEquals("HEADER".getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(written, 0, 6));
        assertArrayEquals(Arrays.copyOfRange(data, 400, 1000), Arrays.copyOfRange(written, 6, 606));
    }

    @Test
    public void testTransferFallsBackWhenTransferFromMakesNoProgress() throws IOException {
        // A destination FileChannel that always answers 0 from transferFrom - the platform behaviour
        // doCopyFile(..) already guards against, and which transfer(..) used to report as a completed copy.
        // Deliberately larger than the fallback's 8 KB buffer (and not a multiple of it), so the fallback loops
        // many times and the buffer is REUSED rather than reallocated - the path the lazy allocation added.
        final byte[] data = alphabet(70_001);
        final File src = write("xfer-stall-src.bin", data);
        final File dst = write("xfer-stall-dst.bin", new byte[0]);

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            final FileChannel real = out.getChannel();
            final FileChannel stalling = new StallingTransferChannel(real);

            assertEquals(data.length, IOUtil.transfer(in.getChannel(), stalling));
            assertEquals(data.length, stalling.position(), "the destination must be left after the last byte");
        }

        assertArrayEquals(data, Files.readAllBytes(dst.toPath()), "the read/write fallback must move every byte");
    }

    @Test
    public void testTransferFallbackHonoursNonZeroPositionsAndPartialWrites() throws IOException {
        // The fallback writes with dest.write(buffer, position) and tracks position itself. A destination that
        // accepts only a few bytes per call, starting at a non-zero position, is what would expose an error in
        // that bookkeeping - the earlier test's destination always accepted the whole buffer.
        final byte[] data = alphabet(30_000);
        final File src = write("xfer-partial-src.bin", data);
        final File dst = write("xfer-partial-dst.bin", "HEAD".getBytes(StandardCharsets.UTF_8));

        try (RandomAccessFile in = new RandomAccessFile(src, "r");
             RandomAccessFile out = new RandomAccessFile(dst, "rw")) {
            in.getChannel().position(10_000);
            out.getChannel().position(4); // just after "HEAD"

            final FileChannel awkward = new StallingTransferChannel(out.getChannel()) {
                @Override
                public int write(final java.nio.ByteBuffer src, final long position) throws IOException {
                    // Accept at most 100 bytes per call, so the inner drain loop runs repeatedly.
                    final int originalLimit = src.limit();
                    src.limit(Math.min(src.position() + 100, originalLimit));
                    try {
                        return super.write(src, position);
                    } finally {
                        src.limit(originalLimit);
                    }
                }
            };

            assertEquals(20_000, IOUtil.transfer(in.getChannel(), awkward));
            assertEquals(20_004, awkward.position());
        }

        final byte[] written = Files.readAllBytes(dst.toPath());
        assertEquals(20_004, written.length);
        assertArrayEquals("HEAD".getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(written, 0, 4));
        assertArrayEquals(Arrays.copyOfRange(data, 10_000, 30_000), Arrays.copyOfRange(written, 4, 20_004));
    }

    /**
     * Delegates everything to a real {@link FileChannel} except {@code transferFrom}, which never progresses.
     * Not {@code final}: one test overrides {@code write(ByteBuffer, long)} to accept only part of the buffer.
     */
    private static class StallingTransferChannel extends FileChannel {
        private final FileChannel delegate;

        StallingTransferChannel(final FileChannel delegate) {
            this.delegate = delegate;
        }

        @Override
        public long transferFrom(final java.nio.channels.ReadableByteChannel src, final long position, final long count) {
            return 0; // "no progress on some platforms"
        }

        @Override
        public int read(final java.nio.ByteBuffer dst) throws IOException {
            return delegate.read(dst);
        }

        @Override
        public long read(final java.nio.ByteBuffer[] dsts, final int offset, final int length) throws IOException {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public int write(final java.nio.ByteBuffer src) throws IOException {
            return delegate.write(src);
        }

        @Override
        public long write(final java.nio.ByteBuffer[] srcs, final int offset, final int length) throws IOException {
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
        public long transferTo(final long position, final long count, final java.nio.channels.WritableByteChannel target) throws IOException {
            return delegate.transferTo(position, count, target);
        }

        @Override
        public int read(final java.nio.ByteBuffer dst, final long position) throws IOException {
            return delegate.read(dst, position);
        }

        @Override
        public int write(final java.nio.ByteBuffer src, final long position) throws IOException {
            return delegate.write(src, position);
        }

        @Override
        public java.nio.MappedByteBuffer map(final MapMode mode, final long position, final long size) throws IOException {
            return delegate.map(mode, position, size);
        }

        @Override
        public java.nio.channels.FileLock lock(final long position, final long size, final boolean shared) throws IOException {
            return delegate.lock(position, size, shared);
        }

        @Override
        public java.nio.channels.FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {
            return delegate.tryLock(position, size, shared);
        }

        @Override
        protected void implCloseChannel() {
            // The caller owns the delegate and closes it through try-with-resources.
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 + J2: contentEqualsIgnoreEOL validates the charset name on every path
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testContentEqualsIgnoreEOLRejectsABadCharsetNameOnEveryShortCircuit() throws IOException {
        final File a = write("ce-a.txt", "same\n");
        final File b = write("ce-b.txt", "same\n");
        final File aliasOfA = new File(tempDir.getPath() + File.separator + "." + File.separator + "ce-a.txt");
        final File missing1 = new File(tempDir, "ce-missing-1.txt");
        final File missing2 = new File(tempDir, "ce-missing-2.txt");

        // Each of these used to return true without ever looking at the charset name.
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.contentEqualsIgnoreEOL(a, a, "NO-SUCH-CHARSET"));
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.contentEqualsIgnoreEOL(a, aliasOfA, "NO-SUCH-CHARSET"));
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.contentEqualsIgnoreEOL(missing1, missing2, "NO-SUCH-CHARSET"));
        // ...and this one always did.
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.contentEqualsIgnoreEOL(a, b, "NO-SUCH-CHARSET"));

        // An illegally *spelled* name is a different exception, and is now documented.
        assertThrows(IllegalCharsetNameException.class, () -> IOUtil.contentEqualsIgnoreEOL(a, a, "!!illegal!!"));
    }

    @Test
    public void testContentEqualsIgnoreEOLWrongKindArgumentStillOutranksTheCharsetName() throws IOException {
        // The charset is resolved after the wrong-kind check, so a directory is still the reported problem.
        final File dir = new File(tempDir, "ce-dir");
        assertTrue(dir.mkdirs());
        final File file = write("ce-c.txt", "x");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEqualsIgnoreEOL(dir, file, "NO-SUCH-CHARSET"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEqualsIgnoreEOL(file, dir, "NO-SUCH-CHARSET"));
    }

    @Test
    public void testContentEqualsIgnoreEOLStillAnswersNormallyForValidNames() throws IOException {
        final File a = write("ce-ok-a.txt", "one\ntwo\n");
        final File b = write("ce-ok-b.txt", "one\r\ntwo");
        final File c = write("ce-ok-c.txt", "one\nthree\n");

        assertTrue(IOUtil.contentEqualsIgnoreEOL(a, b, "UTF-8"));
        assertFalse(IOUtil.contentEqualsIgnoreEOL(a, c, "UTF-8"));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(a, a, null), "a null name still means UTF-8");
        assertTrue(IOUtil.contentEqualsIgnoreEOL(a, a, ""), "an empty name still means UTF-8");
    }

    // ------------------------------------------------------------------------------------------------
    // D2: walk(..) produces exactly the same sequence as listFiles(..)
    // ------------------------------------------------------------------------------------------------

    private File buildTree() throws IOException {
        final File root = new File(tempDir, "tree");
        assertTrue(new File(root, "a/b").mkdirs());
        assertTrue(new File(root, "c").mkdirs());
        write("tree/x.txt", "x");
        write("tree/a/y.txt", "y");
        write("tree/a/b/z.txt", "z");
        write("tree/c/w.txt", "w");
        return root;
    }

    private static List<String> names(final File root, final List<File> files) {
        final List<String> out = new ArrayList<>(files.size());
        for (final File f : files) {
            out.add(root.toPath().relativize(f.toPath()).toString().replace('\\', '/'));
        }
        return out;
    }

    @Test
    public void testWalkProducesTheSameOrderAsListFiles() throws IOException {
        final File root = buildTree();

        for (final boolean recursively : new boolean[] { false, true }) {
            for (final boolean excludeDirectory : new boolean[] { false, true }) {
                final List<String> eager = names(root, IOUtil.listFiles(root, recursively, excludeDirectory));
                final List<String> lazy = names(root, IOUtil.walk(root, recursively, excludeDirectory).toList());

                // Deliberately compared as ORDERED lists. walk(..) used to delegate to Stream.listFiles(..),
                // which is breadth-first, so the two agreed only after sorting.
                assertEquals(eager, lazy, "recursively=" + recursively + ", excludeDirectory=" + excludeDirectory);
            }
        }
    }

    @Test
    public void testWalkIsDepthFirstPreOrder() throws IOException {
        final File root = buildTree();
        final List<String> walked = names(root, IOUtil.walk(root, true, false).toList());

        assertEquals(7, walked.size()); // x.txt, a, a/b, a/b/z.txt, a/y.txt, c, c/w.txt

        // The defining property of pre-order DFS, asserted without assuming any File.listFiles() order:
        // a directory's descendants occupy a CONTIGUOUS block starting immediately after it. Breadth-first
        // order (what walk(..) used to produce) violates this for every directory that has a sibling.
        int directoriesChecked = 0;

        for (int i = 0; i < walked.size(); i++) {
            final String entry = walked.get(i);
            final String prefix = entry + "/";
            int descendants = 0;

            for (final String other : walked) {
                if (other.startsWith(prefix)) {
                    descendants++;
                }
            }

            if (descendants == 0) {
                continue;
            }

            directoriesChecked++;

            // Checked before indexing, so a violation reads as a failed assertion rather than as an
            // IndexOutOfBoundsException from walked.get(i + k).
            assertTrue(i + descendants < walked.size(),
                    "'" + entry + "' has " + descendants + " descendants but only " + (walked.size() - i - 1) + " entries follow it: " + walked);

            for (int k = 1; k <= descendants; k++) {
                assertTrue(walked.get(i + k).startsWith(prefix),
                        "entry " + (i + k) + " (" + walked.get(i + k) + ") should be inside '" + entry + "': " + walked);
            }
        }

        // Non-vacuity: on a tree with no nested directories every inner assertion above would be skipped.
        // Three entries have descendants here - "a" (3), "a/b" (1) and "c" (1) - so the contiguity check
        // above runs against a nested subtree, not just against two flat ones.
        assertEquals(3, directoriesChecked, "expected the 'a', 'a/b' and 'c' subtrees to be checked");
    }

    @Test
    public void testWalkOrderIsDeterministicAcrossCalls() throws IOException {
        final File root = buildTree();

        final List<String> first = names(root, IOUtil.walk(root, true, false).toList());
        final List<String> second = names(root, IOUtil.walk(root, true, false).toList());
        final List<String> eager = names(root, IOUtil.listFiles(root, true, false));

        assertEquals(first, second);
        assertEquals(first, eager);
    }

    @Test
    public void testWalkIsStillLazyAndStillGuardsBadInput() throws IOException {
        final File root = buildTree();

        // Laziness: taking 2 elements must not require materializing the whole tree.
        assertEquals(2, IOUtil.walk(root, true, false).limit(2).toList().size());

        assertEquals(0, IOUtil.walk(null).count());
        assertEquals(0, IOUtil.walk(new File(tempDir, "does-not-exist"), true, true).count());

        final File file = write("walk-not-a-dir.txt", "x");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(file));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file));
    }

    @Test
    public void testWalkExcludesDirectoriesButStillDescendsIntoThem() throws IOException {
        final File root = buildTree();
        final List<String> lazy = names(root, IOUtil.walk(root, true, true).toList());

        assertFalse(lazy.contains("a"));
        assertFalse(lazy.contains("a/b"));
        assertTrue(lazy.contains("a/b/z.txt"), "an excluded directory is still descended into");
        assertEquals(4, lazy.size());
    }

    @Test
    public void testListDirectoriesUsesTheSameDepthFirstOrder() throws IOException {
        // listDirectories(d, true) runs through the same recursion as listFiles(..), so it must return exactly
        // the directory entries of listFiles(d, true, false) in the same relative order. Pinned because the
        // order is now documented on all three of listFiles / listDirectories / walk.
        final File root = buildTree();

        final List<String> dirsOnly = names(root, IOUtil.listDirectories(root, true));
        final List<String> dirsFromListFiles = new ArrayList<>();

        for (final String n : names(root, IOUtil.listFiles(root, true, false))) {
            if (!n.endsWith(".txt")) {
                dirsFromListFiles.add(n);
            }
        }

        assertEquals(dirsFromListFiles, dirsOnly);

        // The SET of directories is fixed; the order of the siblings "a" and "c" is File.listFiles() order, which
        // is filesystem-dependent (alphabetical on NTFS, hash order on ext4). Pre-order is the invariant: "a/b"
        // comes right after "a", whichever sibling the filesystem lists first.
        assertEquals(Arrays.asList("a", "a/b", "c"), new ArrayList<>(new java.util.TreeSet<>(dirsOnly)));
        assertEquals(dirsOnly.indexOf("a") + 1, dirsOnly.indexOf("a/b"));
    }

    @Test
    public void testWalkOverAnEmptyDirectory() throws IOException {
        final File empty = new File(tempDir, "empty-dir");
        assertTrue(empty.mkdirs());

        assertEquals(0, IOUtil.walk(empty, true, false).count());
        assertEquals(Collections.emptyList(), IOUtil.listFiles(empty, true, false));
    }

    // ------------------------------------------------------------------------------------------------
    // J1: split(..)'s documented size distribution
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSplitGivesTheExtraByteToTheFirstParts() throws IOException {
        final File src = write("split-10.bin", "0123456789");
        final File dir = new File(tempDir, "split-out");
        assertTrue(dir.mkdirs());

        IOUtil.split(src, 4, dir);

        final File[] parts = dir.listFiles();
        Arrays.sort(parts, java.util.Comparator.comparing(File::getName));

        // 10 bytes over 4 parts: floor is 2, remainder is 2, so the FIRST two parts take the extra byte.
        assertArrayEquals(new long[] { 3, 3, 2, 2 }, new long[] { parts[0].length(), parts[1].length(), parts[2].length(), parts[3].length() });

        // No two parts differ by more than one byte, and the whole thing still round-trips.
        final File merged = new File(tempDir, "split-merged.bin");
        IOUtil.merge(Arrays.asList(parts), merged);
        assertArrayEquals("0123456789".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(merged.toPath()));
    }

    @Test
    public void testSplitSizeDistributionMatchesTheDocumentedRule() throws IOException {
        // "Each part is either fileLength/countOfParts or that + 1 bytes; the FIRST fileLength % countOfParts
        // parts take the extra byte." Checked over a range rather than one case.
        for (final int len : new int[] { 1, 7, 10, 33, 100, 257 }) {
            for (final int countOfParts : new int[] { 1, 2, 3, 4, 7, 10 }) {
                final byte[] data = alphabet(len);
                final File src = write("dist-src-" + len + ".bin", data);
                final File dir = new File(tempDir, "dist-" + len + "-" + countOfParts);
                assertTrue(dir.mkdirs());

                IOUtil.split(src, countOfParts, dir);

                final File[] parts = dir.listFiles();
                Arrays.sort(parts, java.util.Comparator.comparing(File::getName));
                assertEquals(countOfParts, parts.length, "len=" + len + " parts=" + countOfParts);

                final long base = len / countOfParts;
                final long remainder = len % countOfParts;

                for (int i = 0; i < parts.length; i++) {
                    final long expected = base + (i < remainder ? 1 : 0);
                    assertEquals(expected, parts[i].length(), "len=" + len + " parts=" + countOfParts + " part " + (i + 1));
                }

                // ...and the parts still concatenate back to the source, in name order.
                final File merged = new File(dir, "merged.out");
                IOUtil.merge(Arrays.asList(parts), merged);
                assertArrayEquals(data, Files.readAllBytes(merged.toPath()), "len=" + len + " parts=" + countOfParts);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5: copyFile reports an uncreatable parent directory
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyFileReportsAParentDirectoryItCannotCreate() throws IOException {
        final File src = write("cp-src.txt", "data");
        // A regular file where the destination's parent directory would have to go.
        final File blocker = write("cp-blocker", "not a directory");
        final File dest = new File(blocker, "sub" + File.separator + "dest.txt");

        final IOException ex = assertThrows(IOException.class, () -> IOUtil.copyFile(src, dest));
        assertTrue(ex.getMessage().contains("Failed to create parent directory"), "expected a message naming the directory, got: " + ex.getMessage());
    }

    @Test
    public void testCopyFileStillCreatesMissingParentDirectories() throws IOException {
        final File src = write("cp-ok-src.txt", "payload");
        final File dest = new File(tempDir, "deep/er/still/dest.txt");

        IOUtil.copyFile(src, dest);

        assertTrue(dest.exists());
        assertEquals("payload", new String(Files.readAllBytes(dest.toPath()), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------
    // D1: readThreads must not keep the JVM alive, and must still deliver every line
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testForEachLineWithReadThreadsUsesOnlyDaemonThreads() throws Exception {
        final StringBuilder sb = new StringBuilder();
        final List<String> expected = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            expected.add("line" + i);
            sb.append("line").append(i).append('\n');
        }
        final File file = write("fel.txt", sb.toString());

        final List<String> seen = Collections.synchronizedList(new ArrayList<>());
        IOUtil.forEachLine(file, LineIterationOptions.builder().readThreads(2).queueSize(16).build(), seen::add);

        final List<String> sorted = new ArrayList<>(seen);
        Collections.sort(sorted);
        final List<String> expectedSorted = new ArrayList<>(expected);
        Collections.sort(expectedSorted);
        assertEquals(expectedSorted, sorted);

        // The pool workers stay parked for their keep-alive (180 s) after the call returns. While they were
        // non-daemon the JVM could not terminate for ~181 seconds; every surviving worker must now be a daemon.
        int poolThreads = 0;

        for (final Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("abacus-stream-")) {
                poolThreads++;
                assertTrue(t.isDaemon(), "shared stream-pool thread must be a daemon: " + t.getName());
            }
        }

        // Non-vacuity: readThreads > 0 must actually have gone through the shared pool, otherwise the loop
        // above would pass simply because no such thread was ever created.
        assertTrue(poolThreads > 0, "expected readThreads > 0 to use the shared stream pool, but found no abacus-stream-* thread");
    }

    @Test
    public void testForEachLineSlicingIsUnchangedOnTheSequentialPath() throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("line").append(i).append('\n');
        }
        final File file = write("fel-slice.txt", sb.toString());

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(file, LineIterationOptions.builder().offset(10).count(5).build(), seen::add);

        assertEquals(Arrays.asList("line10", "line11", "line12", "line13", "line14"), seen);
    }

    // ------------------------------------------------------------------------------------------------
    // G14-006: on the buffered path an incomplete transfer is a SHORT RETURN VALUE, not an IOException
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testTransferBufferedPathReturnsAShortCountWhenTheSourceStopsMakingProgress() {
        final byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);
        final int beforeTheStall = 5;

        final ReadableByteChannel stalling = new ReadableByteChannel() {
            private int position;
            private boolean open = true;

            @Override
            public int read(final ByteBuffer dst) {
                if (position >= beforeTheStall) {
                    // permitted by the interface: "no bytes right now", which is not end of input
                    return 0;
                }

                final int n = Math.min(dst.remaining(), beforeTheStall - position);
                dst.put(content, position, n);
                position += n;

                return n;
            }

            @Override
            public boolean isOpen() {
                return open;
            }

            @Override
            public void close() {
                open = false;
            }
        };

        final ByteArrayOutputStream collected = new ByteArrayOutputStream();
        // preemptive timeout: a source that never progresses must end the transfer, not spin inside it
        final long moved = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> IOUtil.transfer(stalling, Channels.newChannel(collected)));

        assertEquals(beforeTheStall, moved);
        assertArrayEquals("01234".getBytes(StandardCharsets.UTF_8), collected.toByteArray());
    }

    @Test
    public void testTransferBufferedPathFailsUncheckedWhenTheDestinationStopsAcceptingBytes() {
        final byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);

        final ReadableByteChannel source = new ReadableByteChannel() {
            private int position;
            private boolean open = true;

            @Override
            public int read(final ByteBuffer dst) {
                if (position >= content.length) {
                    return -1;
                }

                final int n = Math.min(dst.remaining(), content.length - position);
                dst.put(content, position, n);
                position += n;

                return n;
            }

            @Override
            public boolean isOpen() {
                return open;
            }

            @Override
            public void close() {
                open = false;
            }
        };

        final WritableByteChannel deaf = new WritableByteChannel() {
            private int accepted;
            private boolean open = true;

            @Override
            public int write(final ByteBuffer src) {
                if (accepted >= 4) {
                    // permitted by the interface: "cannot accept anything right now"
                    return 0;
                }

                final int n = Math.min(src.remaining(), 4 - accepted);
                src.position(src.position() + n);
                accepted += n;

                return n;
            }

            @Override
            public boolean isOpen() {
                return open;
            }

            @Override
            public void close() {
                open = false;
            }
        };

        // Unlike a stalled SOURCE - which ends the transfer with a short return value - a stalled DESTINATION
        // fails, and the failure comes from the OutputStream that wraps the channel, so it is UNCHECKED.
        // assertThrows(RuntimeException.class, ..) also fails the test if an IOException is thrown instead,
        // which is exactly the distinction the javadoc draws.
        assertThrows(RuntimeException.class, () -> IOUtil.transfer(source, deaf));
    }
}
