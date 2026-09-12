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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the 9th independent review pass over {@link IOUtil} (2026-09-02).
 *
 * <p>Covers, in order: {@code newBufferedReader(Path)} following the class path contract (1), the {@code sizeOf}
 * overflow sentinel surviving a nested walk (2), the delete family unlinking a dangling symbolic link handed in
 * directly (3), {@code write(.., offset, 0, ..)} no longer moving the source (4), {@code map(READ_WRITE)} creating
 * missing parent directories (5), {@code copyFile} preserving the date of a followed link source (6), {@code zip}
 * skipping directory and dangling links instead of failing (7), the twin-consistent argument order of
 * {@code read(File, ..)} (9), the host-name resolver being released on every outcome (10), and the
 * {@code listFiles} / {@code deleteRecursivelyIfExists} recursion refactors preserving order and semantics (18).
 *
 * <p>The symbolic-link cases are skipped, not failed, where the platform cannot create a link (Windows without the
 * "Create symbolic links" privilege).
 */
public class IOUtilBufferedReaderPathTest extends TestBase {

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

    private File dir(final String name) {
        final File d = new File(tempDir, name);
        assertTrue(d.mkdirs() || d.isDirectory());
        return d;
    }

    /** Creates {@code link -> target}, or aborts the test where the platform cannot create symbolic links. */
    private static Path linkOrSkip(final Path link, final Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("This platform cannot create symbolic links here: " + e);
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // 1: newBufferedReader(Path) answers a directory, an absent file and malformed input as the File twin does
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNewBufferedReader_PathRejectsADirectoryAsABadArgument() {
        final File d = dir("adir");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(d.toPath()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(d.toPath(), StandardCharsets.UTF_8));
        // and still the same answer as the twin
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(d));
    }

    @Test
    public void testNewBufferedReader_PathReportsAnAbsentFileAsFileNotFound() {
        final Path missing = new File(tempDir, "nope.txt").toPath();

        for (final Runnable call : new Runnable[] { () -> IOUtil.newBufferedReader(missing), () -> IOUtil.newBufferedReader(missing, null) }) {
            final UncheckedIOException e = assertThrows(UncheckedIOException.class, call::run);
            assertInstanceOf(FileNotFoundException.class, e.getCause());
        }
    }

    @Test
    public void testNewBufferedReader_PathReplacesMalformedInputLikeEveryOtherDecoderInTheClass() throws IOException {
        final File bad = write("bad.txt", new byte[] { 'a', (byte) 0xFF, 'b', '\n', 'c' });

        try (BufferedReader br = IOUtil.newBufferedReader(bad.toPath())) {
            assertEquals("a�b", br.readLine());
            assertEquals("c", br.readLine());
            assertNull(br.readLine());
        }

        try (BufferedReader br = IOUtil.newBufferedReader(bad.toPath(), StandardCharsets.UTF_8)) {
            assertEquals("a�b", br.readLine());
        }

        // The twin, for reference.
        assertEquals(Arrays.asList("a�b", "c"), IOUtil.readAllLines(bad));
    }

    @Test
    public void testNewBufferedReader_PathDecodesWithTheGivenCharsetAndDefaultsToUtf8() throws IOException {
        final File f = write("latin.txt", "héllo".getBytes(StandardCharsets.ISO_8859_1));

        try (BufferedReader br = IOUtil.newBufferedReader(f.toPath(), StandardCharsets.ISO_8859_1)) {
            assertEquals("héllo", br.readLine());
        }

        final File utf8 = write("utf8.txt", "héllo");

        try (BufferedReader br = IOUtil.newBufferedReader(utf8.toPath(), null)) {
            assertEquals("héllo", br.readLine());
        }

        try (BufferedReader br = IOUtil.newBufferedReader(utf8.toPath())) {
            assertEquals("héllo", br.readLine());
        }

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((Path) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((Path) null, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewBufferedReader_PathGivesTheSameThreeAnswersOnAnotherFileSystemProvider() throws IOException {
        final Path zip = new File(tempDir, "fs.zip").toPath();

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zip.toUri()), Map.of("create", "true"))) {
            Files.createDirectory(fs.getPath("/d"));
            Files.write(fs.getPath("/d/bad.txt"), new byte[] { 'x', (byte) 0xFE, 'y' });

            // malformed input is replaced, not reported
            try (BufferedReader br = IOUtil.newBufferedReader(fs.getPath("/d/bad.txt"), StandardCharsets.UTF_8)) {
                assertEquals("x�y", br.readLine());
            }

            try (BufferedReader br = IOUtil.newBufferedReader(fs.getPath("/d/bad.txt"))) {
                assertEquals("x�y", br.readLine());
            }

            // a directory is a bad argument
            assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(fs.getPath("/d")));

            // an absent file is FileNotFoundException, wrapped
            final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedReader(fs.getPath("/d/nope.txt")));
            assertInstanceOf(FileNotFoundException.class, e.getCause());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // 2: the sizeOf overflow sentinel survives a nested walk
    // ------------------------------------------------------------------------------------------------

    /** A {@link File} whose size and listing are faked, so a tree larger than Long.MAX_VALUE can be walked. */
    @SuppressWarnings("serial")
    private static File fake(final String name, final long length, final File[] children) {
        return new File(name) {
            @Override
            public long length() {
                return length;
            }

            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public boolean canRead() {
                return true;
            }

            @Override
            public boolean isFile() {
                return children == null;
            }

            @Override
            public boolean isDirectory() {
                return children != null;
            }

            @Override
            public File[] listFiles() {
                return children;
            }

            @Override
            public Path toPath() {
                return new File(name).toPath();
            }
        };
    }

    @Test
    public void testSizeOf_ReportsTheOverflowSentinelWhenASubdirectoryOverflowed() {
        final File huge1 = fake("huge1", Long.MAX_VALUE, null);
        final File huge2 = fake("huge2", Long.MAX_VALUE, null);
        final File sub = fake("sub", 0, new File[] { huge1, huge2 });
        final File small = fake("small", 100, null);
        // The small file comes FIRST, so the parent's running sum is positive when the subdirectory's sentinel arrives.
        final File root = fake("root", 0, new File[] { small, sub });

        assertTrue(IOUtil.sizeOf(sub) < 0, "the subdirectory itself overflows");
        assertTrue(IOUtil.sizeOf(root) < 0, "the parent must not absorb the sentinel into a positive total");
        assertTrue(IOUtil.sizeOfDirectory(root) < 0);
        assertTrue(IOUtil.sizeOf(root, false) < 0);
        assertTrue(IOUtil.sizeOfDirectory(root, false) < 0);

        // deeper nesting: root -> mid -> sub
        final File mid = fake("mid", 0, new File[] { fake("s2", 7, null), sub });
        final File root2 = fake("root2", 0, new File[] { fake("s1", 1000, null), mid });
        assertTrue(IOUtil.sizeOf(root2) < 0);

        // the flat case still overflows too
        assertTrue(IOUtil.sizeOf(fake("flat", 0, new File[] { small, huge1, huge2 })) < 0);

        // and the overflow-free twin measures the real total
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1).add(BigInteger.valueOf(100)), IOUtil.sizeOfAsBigInteger(root));
    }

    @Test
    public void testSizeOf_StillSumsATreeThatDoesNotOverflow() {
        final File sub = fake("sub", 0, new File[] { fake("a", 5, null), fake("b", 6, null) });
        final File root = fake("root", 0, new File[] { fake("c", 100, null), sub, fake("empty", 0, new File[0]) });

        assertEquals(111, IOUtil.sizeOf(root));
        assertEquals(111, IOUtil.sizeOfDirectory(root));
        assertEquals(BigInteger.valueOf(111), IOUtil.sizeOfAsBigInteger(root));
        assertEquals(11, IOUtil.sizeOf(sub));
    }

    // ------------------------------------------------------------------------------------------------
    // 3: a dangling symbolic link handed directly to the delete family is unlinked
    // ------------------------------------------------------------------------------------------------

    private Path danglingLink(final String name) throws IOException {
        final Path target = new File(tempDir, name + ".target").toPath();
        Files.writeString(target, "x");
        final Path link = linkOrSkip(new File(tempDir, name).toPath(), target);
        Files.delete(target);
        assertTrue(Files.isSymbolicLink(link));
        assertFalse(link.toFile().exists(), "File.exists() follows the link, which is the whole point");
        return link;
    }

    @Test
    public void testDeleteIfExists_UnlinksADanglingLink() throws IOException {
        final Path link = danglingLink("dangling1");

        assertTrue(IOUtil.deleteIfExists(link.toFile()));
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testDeleteRecursivelyIfExists_UnlinksADanglingLink() throws IOException {
        final Path link = danglingLink("dangling2");

        assertTrue(IOUtil.deleteRecursivelyIfExists(link.toFile()));
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testDeleteQuietly_UnlinksADanglingLink() throws IOException {
        final Path link = danglingLink("dangling3");

        assertTrue(IOUtil.deleteQuietly(link.toFile()));
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testDeleteFamilyUnlinksADanglingLinkToADirectory() throws IOException {
        final Path target = new File(tempDir, "gone-dir").toPath();
        Files.createDirectory(target);
        final Path link = linkOrSkip(new File(tempDir, "dangling-dir").toPath(), target);
        Files.delete(target);
        assertTrue(Files.isSymbolicLink(link));

        assertTrue(IOUtil.deleteRecursivelyIfExists(link.toFile()));
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testDeleteRecursivelyIfExists_StillUnlinksADanglingLinkMetInsideATree() throws IOException {
        final File root = dir("tree-with-dangling");
        final Path target = new File(root, "t").toPath();
        Files.writeString(target, "x");
        final Path link = linkOrSkip(new File(root, "l").toPath(), target);
        Files.delete(target);
        Files.writeString(new File(root, "keep.txt").toPath(), "k");

        assertTrue(IOUtil.deleteRecursivelyIfExists(root));
        assertFalse(root.exists());
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testDeleteRecursivelyIfExists_DoesNotFollowALinkedDirectoryInsideATree() throws IOException {
        final File outside = dir("outside");
        final File kept = write("outside/kept.txt", "kept");
        final File root = dir("tree-with-link");
        write("tree-with-link/a.txt", "a");
        linkOrSkip(new File(root, "to-outside").toPath(), outside.toPath());

        assertTrue(IOUtil.deleteRecursivelyIfExists(root));
        assertFalse(root.exists());
        assertTrue(kept.exists(), "the link was unlinked, its target left alone");
    }

    @Test
    public void testDeleteFamilyStillAnswersFalseForAnAbsentOrUnparseablePath() {
        final File absent = new File(tempDir, "never-created");
        assertFalse(IOUtil.deleteIfExists(absent));
        assertFalse(IOUtil.deleteRecursivelyIfExists(absent));
        assertFalse(IOUtil.deleteQuietly(absent));
        assertFalse(IOUtil.deleteIfExists(null));
        assertFalse(IOUtil.deleteRecursivelyIfExists(null));

        // A NUL byte is not a legal path character on any platform: toPath() throws InvalidPathException, which
        // must stay "absent" rather than escape from a method that never threw.
        final File unparseable = new File(tempDir, "bad\u0000name");
        assertFalse(IOUtil.deleteIfExists(unparseable));
        assertFalse(IOUtil.deleteRecursivelyIfExists(unparseable));
    }

    @Test
    public void testDeleteRecursivelyIfExists_RemovesANestedTreeAndReportsIt() throws IOException {
        final File root = dir("nested");
        write("nested/a.txt", "a");
        write("nested/sub/b.txt", "b");
        write("nested/sub/deeper/c.txt", "c");
        dir("nested/sub/empty");

        assertTrue(IOUtil.deleteRecursivelyIfExists(root));
        assertFalse(root.exists());

        final File single = write("single.txt", "s");
        assertTrue(IOUtil.deleteRecursivelyIfExists(single));
        assertFalse(single.exists());
    }

    // ------------------------------------------------------------------------------------------------
    // 4: write(.., offset, 0, ..) does not move the source, matching readBytes/readChars
    // ------------------------------------------------------------------------------------------------

    /** Counts flushes and records what was written. */
    private static final class CountingOutputStream extends ByteArrayOutputStream {
        int flushes;

        @Override
        public void flush() throws IOException {
            flushes++;
            super.flush();
        }
    }

    private static final class CountingWriter extends StringWriter {
        int flushes;

        @Override
        public void flush() {
            flushes++;
            super.flush();
        }
    }

    @Test
    public void testWrite_WithCountZeroLeavesTheStreamWhereItWas() throws IOException {
        final byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);

        final InputStream is = new ByteArrayInputStream(data);
        final CountingOutputStream os = new CountingOutputStream();
        assertEquals(0, IOUtil.write(is, 5, 0, os));
        assertEquals('0', is.read(), "nothing was skipped");
        assertEquals(0, os.size());
        assertEquals(0, os.flushes);

        // flush is still honoured
        final InputStream is2 = new ByteArrayInputStream(data);
        final CountingOutputStream os2 = new CountingOutputStream();
        assertEquals(0, IOUtil.write(is2, 5, 0, os2, true));
        assertEquals('0', is2.read());
        assertEquals(1, os2.flushes);

        // parity with the read twin
        final InputStream is3 = new ByteArrayInputStream(data);
        assertEquals(0, IOUtil.readBytes(is3, 5, 0).length);
        assertEquals('0', is3.read());

        // a positive count still skips and writes
        final InputStream is4 = new ByteArrayInputStream(data);
        final ByteArrayOutputStream os4 = new ByteArrayOutputStream();
        assertEquals(3, IOUtil.write(is4, 5, 3, os4));
        assertEquals("567", os4.toString(StandardCharsets.US_ASCII));
        assertEquals('8', is4.read());

        // offset past the end still answers 0 without an error
        final InputStream is5 = new ByteArrayInputStream(data);
        assertEquals(0, IOUtil.write(is5, 50, 3, new ByteArrayOutputStream()));
    }

    @Test
    public void testWrite_ReaderWithCountZeroLeavesTheReaderWhereItWas() throws IOException {
        final Reader r = new StringReader("0123456789");
        final CountingWriter w = new CountingWriter();
        assertEquals(0, IOUtil.write(r, 5, 0, w));
        assertEquals('0', r.read(), "nothing was skipped");
        assertEquals("", w.toString());
        assertEquals(0, w.flushes);

        final Reader r2 = new StringReader("0123456789");
        final CountingWriter w2 = new CountingWriter();
        assertEquals(0, IOUtil.write(r2, 5, 0, w2, true));
        assertEquals('0', r2.read());
        assertEquals(1, w2.flushes);

        final Reader r3 = new StringReader("0123456789");
        assertEquals(0, IOUtil.readChars(r3, 5, 0).length);
        assertEquals('0', r3.read());

        final Reader r4 = new StringReader("0123456789");
        final Writer w4 = new StringWriter();
        assertEquals(3, IOUtil.write(r4, 5, 3, w4));
        assertEquals("567", w4.toString());
        assertEquals('8', r4.read());
    }

    @Test
    public void testFileWriteWithCountZeroStillReplacesTheTargetWithNothing() throws IOException {
        final File src = write("src.bin", "0123456789");
        final File out = write("out.bin", "old content");

        assertEquals(0, IOUtil.write(src, 5, 0, out));
        assertTrue(out.exists());
        assertEquals(0, out.length(), "a write is a complete replacement, even of nothing");

        try (InputStream is = IOUtil.newFileInputStream(src)) {
            final File out2 = write("out2.bin", "old");
            assertEquals(0, IOUtil.write(is, 5, 0, out2));
            assertEquals(0, out2.length());
            assertEquals('0', is.read());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // 5: map(READ_WRITE / PRIVATE) creates missing parent directories
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMap_ReadWriteCreatesTheFileAndItsMissingParentDirectories() {
        final File deep = new File(tempDir, "no-such-dir/deeper/new.bin");
        assertFalse(deep.getParentFile().exists());

        final MappedByteBuffer buffer = IOUtil.map(deep, MapMode.READ_WRITE, 0, 16);

        try {
            assertEquals(16, buffer.capacity());
            buffer.put(0, (byte) 42);
        } finally {
            unmap(buffer);
        }

        assertTrue(deep.isFile());
        assertEquals(16, deep.length());
    }

    @Test
    public void testMap_PrivateCreatesTheFileAndItsMissingParentDirectories() {
        final File deep = new File(tempDir, "no-such-dir-2/new.bin");

        unmap(IOUtil.map(deep, MapMode.PRIVATE, 0, 8));

        assertTrue(deep.isFile());
        assertEquals(8, deep.length());
    }

    @Test
    public void testMap_ReadOnlyStillNeverCreatesAnything() {
        final File deep = new File(tempDir, "no-such-dir-3/new.bin");

        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.map(deep, MapMode.READ_ONLY, 0, 8));
        assertInstanceOf(FileNotFoundException.class, e.getCause());
        assertFalse(deep.getParentFile().exists(), "READ_ONLY must not create the directory either");

        // a directory where the file should be is still a bad argument, whatever the mode
        final File d = dir("mapdir");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(d, MapMode.READ_WRITE, 0, 8));
    }

    @Test
    public void testMap_ReadWriteOnAnExistingFileIsUnchanged() throws IOException {
        final File f = write("existing/data.bin", new byte[] { 1, 2, 3, 4 });

        final MappedByteBuffer buffer = IOUtil.map(f, MapMode.READ_WRITE, 0, 4);

        try {
            assertEquals(3, buffer.get(2));
        } finally {
            unmap(buffer);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // 6: copyFile preserves the date of a followed symbolic-link source
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyFile_PreservesTheDateWhenTheSourceIsAFollowedLink() throws IOException {
        final File real = write("real.txt", "hello");
        final FileTime stamp = FileTime.fromMillis(1_600_000_001_000L);
        Files.setLastModifiedTime(real.toPath(), stamp);
        final Path link = linkOrSkip(new File(tempDir, "real.lnk").toPath(), real.toPath());

        final File copy = new File(tempDir, "copy.txt");
        IOUtil.copyFile(link.toFile(), copy);

        assertFalse(Files.isSymbolicLink(copy.toPath()), "the default follows the link: the copy is a plain file");
        assertEquals("hello", IOUtil.readAllToString(copy));
        assertEquals(stamp.toMillis(), Files.getLastModifiedTime(copy.toPath()).toMillis());

        // the plain-file case is unchanged
        final File copy2 = new File(tempDir, "copy2.txt");
        IOUtil.copyFile(real, copy2);
        assertEquals(stamp.toMillis(), Files.getLastModifiedTime(copy2.toPath()).toMillis());
    }

    @Test
    public void testCopyFile_WithNoFollowLinksCopiesTheLinkAndLeavesTheTargetAlone() throws IOException {
        final File real = write("real2.txt", "hello");
        final FileTime stamp = FileTime.fromMillis(1_600_000_002_000L);
        Files.setLastModifiedTime(real.toPath(), stamp);
        final Path link = linkOrSkip(new File(tempDir, "real2.lnk").toPath(), real.toPath());

        final File copy = new File(tempDir, "copy-link.lnk");
        IOUtil.copyFile(link.toFile(), copy, true, LinkOption.NOFOLLOW_LINKS);

        assertTrue(Files.isSymbolicLink(copy.toPath()));
        assertEquals("hello", IOUtil.readAllToString(copy));
        assertEquals(stamp.toMillis(), Files.getLastModifiedTime(real.toPath()).toMillis(), "the target's time is not touched through the copied link");
    }

    // ------------------------------------------------------------------------------------------------
    // 7: zip(directory) with nested symbolic links
    // ------------------------------------------------------------------------------------------------

    private static Map<String, String> entries(final File archive) throws IOException {
        final Map<String, String> result = new TreeMap<>();

        try (ZipFile zip = new ZipFile(archive)) {
            for (final ZipEntry entry : java.util.Collections.list(zip.entries())) {
                result.put(entry.getName(), entry.isDirectory() ? "<dir>" : new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        return result;
    }

    @Test
    public void testZip_DirectoryNamesEntriesExactlyAsBefore() throws IOException {
        final File src = dir("zsrc");
        write("zsrc/a.txt", "aaa");
        write("zsrc/sub/b.txt", "bbb");
        dir("zsrc/sub/empty");
        final File archive = new File(tempDir, "plain.zip");

        IOUtil.zip(src, archive);

        final Map<String, String> expected = new TreeMap<>();
        expected.put("zsrc/", "<dir>");
        expected.put("zsrc/a.txt", "aaa");
        expected.put("zsrc/sub/", "<dir>");
        expected.put("zsrc/sub/b.txt", "bbb");
        expected.put("zsrc/sub/empty/", "<dir>");
        assertEquals(expected, entries(archive));

        // an archive written INTO the source is still left out of itself
        final File inside = new File(src, "inside.zip");
        IOUtil.zip(src, inside);
        assertFalse(entries(inside).containsKey("zsrc/inside.zip"));

        // and the collision pass agrees with the write pass
        final File collection = new File(tempDir, "collection.zip");
        IOUtil.zip(Arrays.asList(src, write("loose.txt", "L")), collection);
        assertTrue(entries(collection).containsKey("zsrc/sub/b.txt"));
        assertEquals("L", entries(collection).get("loose.txt"));

        // round trip
        final File out = dir("unzipped");
        IOUtil.unzip(archive, out);
        assertEquals("bbb", IOUtil.readAllToString(new File(out, "zsrc/sub/b.txt")));
        assertTrue(new File(out, "zsrc/sub/empty").isDirectory());
    }

    @Test
    public void testZip_DirectoryArchivesAFileLinkAsItsTargetAndSkipsDirectoryAndDanglingLinks() throws IOException {
        final File src = dir("zlinks");
        write("zlinks/a.txt", "aaa");
        final File other = dir("zother");
        write("zother/b.txt", "bbb");
        final File target = write("ztarget.txt", "ttt");
        final FileTime stamp = FileTime.fromMillis(1_500_000_000_000L);
        Files.setLastModifiedTime(target.toPath(), stamp);

        linkOrSkip(new File(src, "dirlink").toPath(), other.toPath());
        linkOrSkip(new File(src, "filelink.txt").toPath(), target.toPath());
        final Path gone = new File(tempDir, "zgone.txt").toPath();
        Files.writeString(gone, "g");
        linkOrSkip(new File(src, "dangling.txt").toPath(), gone);
        Files.delete(gone);

        final File archive = new File(tempDir, "links.zip");
        IOUtil.zip(src, archive); // used to fail on the directory link with a partial archive left behind

        final Map<String, String> expected = new TreeMap<>();
        expected.put("zlinks/", "<dir>");
        expected.put("zlinks/a.txt", "aaa");
        expected.put("zlinks/filelink.txt", "ttt");
        assertEquals(expected, entries(archive));

        try (ZipFile zip = new ZipFile(archive)) {
            assertEquals(stamp.toMillis(), zip.getEntry("zlinks/filelink.txt").getLastModifiedTime().toMillis(), "dated as the target, not as the link");
        }

        // the collection form walks the same way, so its collision pass sees the same names
        final File archive2 = new File(tempDir, "links2.zip");
        IOUtil.zip(Arrays.asList(src), archive2);
        assertEquals(expected, entries(archive2));

        // copyToDirectory, for reference, still copies the same tree (links as links)
        final File dest = dir("zdest");
        IOUtil.copyToDirectory(src, dest);
        assertTrue(Files.isSymbolicLink(new File(dest, "zlinks/dirlink").toPath()));
    }

    @Test
    public void testZip_FollowsATopLevelDirectoryLinkAndNamesEntriesUnderTheLink() throws IOException {
        final File real = dir("zreal");
        write("zreal/x.txt", "xxx");
        final Path link = linkOrSkip(new File(tempDir, "zalias").toPath(), real.toPath());

        final File archive = new File(tempDir, "alias.zip");
        IOUtil.zip(link.toFile(), archive);

        final Map<String, String> expected = new TreeMap<>();
        expected.put("zalias/", "<dir>");
        expected.put("zalias/x.txt", "xxx");
        assertEquals(expected, entries(archive));
    }

    // ------------------------------------------------------------------------------------------------
    // 9: read(File, ..) validates the source first, as the stream twins do
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testReadFileReportsANullSourceBeforeANullBuffer() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, (byte[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, StandardCharsets.UTF_8, (char[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, new byte[4], 0, 4));

        // the buffer keeps java.io's own answer once the source is fine
        final File f = write("r.txt", "abc");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(f, (byte[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(f, (char[]) null, 0, 0));

        // and a bad range is still reported before the file is touched
        final File missing = new File(tempDir, "missing.txt");
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(missing, new byte[2], 1, 5));

        final byte[] buf = new byte[3];
        assertEquals(3, IOUtil.read(f, buf, 0, 3));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), buf);
    }

    // ------------------------------------------------------------------------------------------------
    // 10: the host-name resolver is released whenever no attempt is in flight
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testHostNameResolverIsNotKeptOnceNoLookupIsInFlight() throws Exception {
        assertNotNull(IOUtil.getHostName());

        final Field futureField = IOUtil.class.getDeclaredField("hostNameFuture");
        final Field resolverField = IOUtil.class.getDeclaredField("hostNameResolver");
        futureField.setAccessible(true);
        resolverField.setAccessible(true);

        if (futureField.get(null) == null) {
            assertNull(resolverField.get(null), "no attempt in flight, so no executor may be parked for the JVM's lifetime");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // 18: listFiles recursion refactor keeps order, filter arguments and link policy
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testListFiles_RecursionKeepsDepthFirstOrderAndMatchesWalk() throws IOException {
        final File root = dir("lf");
        write("lf/a/b/z.txt", "z");
        write("lf/a/y.txt", "y");
        write("lf/x.txt", "x");

        for (final boolean excludeDirectory : new boolean[] { false, true }) {
            final List<File> eager = IOUtil.listFiles(root, true, excludeDirectory);
            final List<File> lazy = IOUtil.walk(root, true, excludeDirectory).toList();
            assertEquals(lazy, eager);

            // pre-order: a directory is followed by its own subtree before its next sibling
            final List<String> names = new ArrayList<>();
            for (final File f : eager) {
                names.add(root.toPath().relativize(f.toPath()).toString().replace('\\', '/'));
            }
            final int a = names.indexOf("a");
            final int z = names.indexOf("a/b/z.txt");
            final int y = names.indexOf("a/y.txt");
            if (!excludeDirectory) {
                assertTrue(a >= 0 && a < z && a < y);
                assertTrue(names.indexOf("a/b") < z);
            }
            assertTrue(names.contains("x.txt") && z >= 0 && y >= 0);
        }

        // the filter sees each entry with ITS parent, at every depth, and a rejected directory is still descended into
        final List<String> seen = new ArrayList<>();
        final List<File> matched = IOUtil.listFiles(root, true, (parent, file) -> {
            seen.add(parent.getName() + "|" + file.getName());
            return file.getName().endsWith(".txt");
        });
        assertEquals(3, matched.size());
        assertTrue(seen.contains("lf|a"));
        assertTrue(seen.contains("a|b"));
        assertTrue(seen.contains("b|z.txt"));

        // non-recursive: direct children only
        assertEquals(2, IOUtil.listFiles(root).size());
        assertEquals(1, IOUtil.listDirectories(root).size());
        assertEquals(2, IOUtil.listDirectories(root, true).size());

        // absent and null still answer empty; a file is still a bad argument
        assertTrue(IOUtil.listFiles(new File(tempDir, "absent"), true, false).isEmpty());
        assertTrue(IOUtil.listFiles(null, true, false).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(new File(root, "x.txt"), true, false));
    }

    @Test
    public void testListFiles_RecursionStillDoesNotDescendIntoALinkedDirectory() throws IOException {
        final File root = dir("lf-link");
        write("lf-link/a.txt", "a");
        final File outside = dir("lf-outside");
        write("lf-outside/o.txt", "o");
        linkOrSkip(new File(root, "link").toPath(), outside.toPath());

        final List<File> files = IOUtil.listFiles(root, true, false);
        assertEquals(2, files.size(), "the link itself is listed, its target is not entered: " + files);
        assertEquals(IOUtil.walk(root, true, false).toList(), files);
    }
}
