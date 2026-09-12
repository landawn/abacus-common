package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the sixth independent review pass over {@code IOUtil} (run E, 2026-08-30).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>C-001</b> - {@code copyURLToFile} rejects a {@code file:} URL that names the destination. It used
 *       to truncate the destination to zero bytes and return normally, because {@code write(is, destination)}
 *       opens (and therefore truncates) the target before the first byte is read from the URL's stream. Every
 *       peer operation already refused the same shape. The check is fail-open: a URL that cannot be mapped to
 *       a local {@code File} is left exactly as it was.</li>
 *   <li><b>C-002</b> - {@code copyToDirectory} rejects a copy whose target inside {@code destDir} would be the
 *       source directory itself or one of its ancestors. It used to walk the source onto itself: copying its
 *       entries <i>into</i> the source tree and returning normally, or - for a source with nothing to copy -
 *       reporting success having done nothing and handing back the source as "the copy".</li>
 *   <li><b>C-003</b> - {@code newStringWriter(int)} rejects a negative size with
 *       {@code IllegalArgumentException} like every sibling factory, not with
 *       {@code NegativeArraySizeException} from {@code StringBuilder}.</li>
 *   <li><b>C-005</b> - a charset that writes a byte-order mark emits one per {@code write}/{@code append}
 *       call, not only per {@code append}; the class contract now says so.</li>
 *   <li><b>C-006</b> - {@code splitByLine} keeps the name of a dot-file: {@code .hidden} yields
 *       {@code .hidden_0001}, not {@code _0001.hidden}.</li>
 *   <li><b>C-007</b> - {@code unzip} rejects a file entry whose name resolves to the target directory itself
 *       instead of surfacing a raw platform "Access is denied".</li>
 *   <li><b>C-008</b> - moving something into the directory it is already in is a documented no-op.</li>
 *   <li><b>C-009</b> - the containment guard fires for a filesystem-root source too; a canonical root path
 *       already ends with the separator, which used to make the prefix test answer {@code false} for every
 *       path on that volume.</li>
 *   <li><b>C-011</b> - the stream/reader {@code write}/{@code append} overloads cannot detect a source that
 *       reads from the target; that is now documented, and this pins the behaviour it describes.</li>
 * </ul>
 */
public class IOUtilCopyUrlToFileTest extends TestBase {

    @TempDir
    Path tempFolder;

    private File root() {
        return tempFolder.toFile();
    }

    private File dir(final String name) {
        final File d = new File(root(), name);
        assertTrue(d.mkdirs() || d.isDirectory());
        return d;
    }

    private File file(final File parent, final String name, final String content) throws IOException {
        final File f = new File(parent, name);
        IOUtil.write(content, f);
        return f;
    }

    private static List<String> names(final File d) {
        final String[] list = d.list();
        final List<String> out = list == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(list));
        Collections.sort(out);
        return out;
    }

    // ------------------------------------------------------------------------------------------------
    // C-001 - copyURLToFile onto its own source
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyURLToFile_FileUrlNamingTheDestinationIsRejected() throws Exception {
        final File d = dir("c1");

        for (final int size : new int[] { 10, 8 * 1024, 300 * 1024 }) {
            final File f = new File(d, "self" + size + ".bin");
            final byte[] data = new byte[size];

            for (int i = 0; i < size; i++) {
                data[i] = (byte) ('A' + (i % 26));
            }

            IOUtil.write(data, f);

            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(f.toURI().toURL(), f));
            assertEquals(size, f.length(), "a rejected copyURLToFile must not touch the destination");
            assertArrayEqualsBytes(data, IOUtil.readAllBytes(f));

            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(f.toURI().toURL(), f, 1000, 1000));
            assertEquals(size, f.length());
        }
    }

    @Test
    public void testCopyURLToFile_LocalhostSpellingOfTheDestinationIsRejected() throws Exception {
        final File d = dir("c1e");
        final File f = file(d, "x.txt", "PRECIOUS");
        final URL viaLocalhost = new URL("file://localhost" + f.toURI().getRawPath());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(viaLocalhost, f));
        assertEquals("PRECIOUS", IOUtil.readAllToString(f));
    }

    @Test
    public void testCopyURLToFile_HardLinkAliasOfTheDestinationIsRejected() throws Exception {
        final File d = dir("c1b");
        final File a = file(d, "a.bin", "PRECIOUS");
        final File b = new File(d, "b.bin");

        try {
            Files.createLink(b.toPath(), a.toPath());
        } catch (final IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("hard links are not supported here: " + e);
            return;
        }

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(a.toURI().toURL(), b));
        assertEquals("PRECIOUS", IOUtil.readAllToString(a));
        assertEquals("PRECIOUS", IOUtil.readAllToString(b));
    }

    @Test
    public void testCopyURLToFile_ADifferentFileStillCopies() throws Exception {
        final File d = dir("c1c");
        final File src = file(d, "src.txt", "payload-\u00e9\u4e2d");
        final File dst = new File(d, "sub/dst.txt");

        IOUtil.copyURLToFile(src.toURI().toURL(), dst);
        assertTrue(IOUtil.contentEquals(src, dst));

        // the timeout overload too, and into an existing destination
        IOUtil.write("stale", dst);
        IOUtil.copyURLToFile(src.toURI().toURL(), dst, 5000, 5000);
        assertTrue(IOUtil.contentEquals(src, dst));
    }

    @Test
    public void testCopyURLToFile_AnUnmappableFileUrlIsLeftAlone() throws Exception {
        // The legacy "file:/C|/.." drive spelling is opened by the JDK's file: handler but resolves to a path
        // this platform cannot canonicalize, so the guard must fail OPEN rather than turn a working copy into
        // an error.
        Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "the 'C|' drive spelling is a Windows form");

        final File d = dir("c1f");
        final File src = file(d, "src.txt", "0123456789");
        final File dst = new File(d, "dst.txt");
        final URL pipeUrl = new URL("file:/" + src.getAbsolutePath().replace('\\', '/').replace(":", "|"));

        IOUtil.copyURLToFile(pipeUrl, dst);
        assertEquals("0123456789", IOUtil.readAllToString(dst));
    }

    @Test
    public void testCopyURLToFile_NullAndEmptySourceStillBehave() throws Exception {
        final File d = dir("c1d");
        final File empty = new File(d, "empty.txt");
        IOUtil.write("", empty);
        final File out = new File(d, "out.txt");

        IOUtil.copyURLToFile(empty.toURI().toURL(), out);
        assertTrue(out.exists());
        assertEquals(0, out.length());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(null, out));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(empty.toURI().toURL(), (File) null));
    }

    // ------------------------------------------------------------------------------------------------
    // C-002 - copyToDirectory: the target must not be the source or one of its ancestors
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyToDirectory_EmptyDirectoryIntoItsOwnParentIsRejected() throws Exception {
        final File parent = dir("c2a");
        final File sub = dir("c2a/sub");

        // it used to return `sub` itself, having copied nothing - so a caller that deleted "the copy"
        // deleted the source.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(sub, parent));
        assertEquals(Arrays.asList("sub"), names(parent));
    }

    @Test
    public void testCopyToDirectory_PopulatedDirectoryIntoItsOwnParentIsRejected() throws Exception {
        final File parent = dir("c2b");
        final File sub = dir("c2b/sub");
        file(sub, "inner.txt", "I");

        // it used to fail with "The destination file already exists: <parent>/sub/inner.txt" - a message
        // naming a grandchild rather than either argument.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(sub, parent));
        assertTrue(e.getMessage().contains(sub.getCanonicalPath()), "the message must name the source: " + e.getMessage());
        assertEquals(Arrays.asList("inner.txt"), names(sub));
        assertEquals(Arrays.asList("sub"), names(parent));
    }

    @Test
    public void testCopyToDirectory_FilteredDirectoryIntoItsOwnParentIsRejected() throws Exception {
        final File parent = dir("c2c");
        final File sub = dir("c2c/sub");
        file(sub, "inner.txt", "I");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(sub, parent, true, (p, f) -> false));
        assertEquals(Arrays.asList("inner.txt"), names(sub));
    }

    @Test
    public void testCopyToDirectory_TargetThatIsAnAncestorOfTheSourceIsRejected() throws Exception {
        // g/b/c/b copied into g aims at g/b, an ANCESTOR of the source: it used to copy leaf.txt and
        // deep/deep.txt INTO the source tree and return normally.
        final File g = dir("c2g");
        final File gbcb = dir("c2g/b/c/b");
        file(gbcb, "leaf.txt", "LEAF");
        final File deep = dir("c2g/b/c/b/deep");
        file(deep, "deep.txt", "DEEP");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(gbcb, g));
        assertEquals(Arrays.asList("c"), names(new File(g, "b")));
        assertEquals(Arrays.asList("deep", "leaf.txt"), names(gbcb));
    }

    @Test
    public void testCopyDirectory_RepeatedNameAlongThePathIsRejected() throws Exception {
        // copyDirectory(a/b, a) reaches copyToDirectory(a/b/b, a), whose target a/b is the child's parent.
        final File a = dir("c2h");
        final File ab = dir("c2h/b");
        final File abb = dir("c2h/b/b");
        file(abb, "leaf.txt", "LEAF");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(ab, a));
        assertEquals(Arrays.asList("b"), names(ab));
        assertEquals(Arrays.asList("leaf.txt"), names(abb));
    }

    @Test
    public void testCopyToDirectory_LegalShapesAreUnaffected() throws Exception {
        // a sibling destination
        final File src = dir("c2e/src");
        file(src, "a.txt", "A");
        final File dest = dir("c2e/dest");

        final File made = IOUtil.copyToDirectory(src, dest);
        assertEquals("src", made.getName());
        assertEquals("A", IOUtil.readAllToString(new File(made, "a.txt")));

        // a destination that is an ANCESTOR of the source, with no name repeated along the path
        final File outer = dir("c2i");
        final File inner = dir("c2i/inner");
        file(inner, "x.txt", "X");

        IOUtil.copyDirectory(inner, outer);
        assertEquals(Arrays.asList("inner", "x.txt"), names(outer));
    }

    @Test
    public void testCopyToDirectory_DestinationInsideTheSourceIsStillRejected() throws Exception {
        final File src = dir("c2f/src");
        file(src, "a.txt", "A");
        final File inside = new File(src, "inner-dest");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(src, inside));
        assertFalse(inside.exists(), "a rejected copyToDirectory must not create the destination directory");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(src, src));
    }

    // ------------------------------------------------------------------------------------------------
    // C-009 - the containment guard and a filesystem root
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testContainmentGuardFiresForAFilesystemRootSource() throws Exception {
        // A canonical root path already ends with the separator ("C:\", "/"), so the character after the
        // prefix is the first character of a NAME and the old test answered false for every path on the
        // volume - leaving a root source unguarded in copyToDirectory / copyDirectory / moveToDirectory.
        final File dest = dir("c9");
        final File rootDrive = dest.getCanonicalFile().toPath().getRoot().toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(rootDrive, dest));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(rootDrive, dest));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(rootDrive, dest));
        assertEquals(Collections.emptyList(), names(dest));
    }

    // ------------------------------------------------------------------------------------------------
    // C-008 - moving something into the directory it is already in
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMoveToDirectory_IntoItsOwnParentIsANoOp() throws Exception {
        final File parent = dir("c8");
        final File f = file(parent, "a.txt", "A");

        IOUtil.moveToDirectory(f, parent);

        assertTrue(f.exists(), "the source is the target, so nothing is moved and nothing is removed");
        assertEquals("A", IOUtil.readAllToString(f));
        assertEquals(Arrays.asList("a.txt"), names(parent));

        final File sub = dir("c8/sub");
        file(sub, "x.txt", "X");
        IOUtil.moveToDirectory(sub, parent);
        assertTrue(sub.isDirectory());
        assertEquals(Arrays.asList("x.txt"), names(sub));
    }

    // ------------------------------------------------------------------------------------------------
    // C-003 - newStringWriter(int)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNewStringWriter_NegativeInitialSizeIsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newStringWriter(-1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newStringWriter(Integer.MIN_VALUE));

        assertNotNull(IOUtil.newStringWriter(0));
        assertNotNull(IOUtil.newStringWriter(1024));

        // the sibling factories already answered this way
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newByteArrayOutputStream(-1));
    }

    // ------------------------------------------------------------------------------------------------
    // C-005 - one byte-order mark per charset-aware call, write as well as append
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testUtf16EmitsOneBomPerWriteCall() throws Exception {
        final ByteArrayOutputStream bos = IOUtil.newByteArrayOutputStream();
        IOUtil.write("ab".toCharArray(), StandardCharsets.UTF_16, bos);
        IOUtil.write("cd".toCharArray(), StandardCharsets.UTF_16, bos);

        final byte[] twice = bos.toByteArray();
        assertEquals(12, twice.length, "two BOMs plus four UTF-16 code units");
        assertEquals((byte) 0xFE, twice[0]);
        assertEquals((byte) 0xFF, twice[1]);
        assertEquals((byte) 0xFE, twice[6]);
        assertEquals((byte) 0xFF, twice[7]);

        // the second BOM reads back as a U+FEFF character, not as a byte-order signal
        assertEquals("ab\uFEFFcd", new String(twice, StandardCharsets.UTF_16));

        // one call, one BOM
        final ByteArrayOutputStream once = IOUtil.newByteArrayOutputStream();
        IOUtil.write("abcd".toCharArray(), StandardCharsets.UTF_16, once);
        assertEquals(10, once.toByteArray().length);
        assertEquals("abcd", new String(once.toByteArray(), StandardCharsets.UTF_16));
    }

    @Test
    public void testUtf16AppendLineEmitsOneBomPerLine() throws Exception {
        final File f = new File(dir("c5"), "u16.txt");
        IOUtil.appendLine("a", StandardCharsets.UTF_16, f);
        IOUtil.appendLine("b", StandardCharsets.UTF_16, f);

        assertEquals(Arrays.asList("a", "\uFEFFb"), IOUtil.readAllLines(f, StandardCharsets.UTF_16));

        // appendLines opens ONE writer for the whole batch, so a batch costs one BOM, not one per line
        final File g = new File(dir("c5"), "u16b.txt");
        IOUtil.appendLines(Arrays.asList("a", "b"), StandardCharsets.UTF_16, g);
        assertEquals(Arrays.asList("a", "b"), IOUtil.readAllLines(g, StandardCharsets.UTF_16));
    }

    // ------------------------------------------------------------------------------------------------
    // C-006 - splitByLine part names
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSplitByLine_DotFileKeepsItsName() throws Exception {
        final File d = dir("c6");
        final File hidden = file(d, ".hidden", "h1\nh2\nh3\nh4\n");
        final File out = dir("c6/out");

        IOUtil.splitByLine(hidden, 2, out);

        assertEquals(Arrays.asList(".hidden_0001", ".hidden_0002"), names(out));
        assertEquals("h1\nh2\n", IOUtil.readAllToString(new File(out, ".hidden_0001")));
        assertEquals("h3\nh4\n", IOUtil.readAllToString(new File(out, ".hidden_0002")));
    }

    @Test
    public void testSplitByLine_OtherNameShapesAreUnchanged() throws Exception {
        final File d = dir("c6b");
        final String body = "l1\nl2\nl3\nl4\n";

        final File out1 = dir("c6b/o1");
        IOUtil.splitByLine(file(d, "log.txt", body), 2, out1);
        assertEquals(Arrays.asList("log_0001.txt", "log_0002.txt"), names(out1));

        final File out2 = dir("c6b/o2");
        IOUtil.splitByLine(file(d, "logfile", body), 2, out2);
        assertEquals(Arrays.asList("logfile_0001", "logfile_0002"), names(out2));

        final File out3 = dir("c6b/o3");
        IOUtil.splitByLine(file(d, "a.b.c.txt", body), 2, out3);
        assertEquals(Arrays.asList("a.b.c_0001.txt", "a.b.c_0002.txt"), names(out3));

        final File gz = new File(d, "app.log.gz");

        try (OutputStream os = IOUtil.newGZIPOutputStream(IOUtil.newFileOutputStream(gz))) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        final File out4 = dir("c6b/o4");
        IOUtil.splitByLine(gz, 2, out4);
        assertEquals(Arrays.asList("app_0001.log", "app_0002.log"), names(out4));
    }

    // ------------------------------------------------------------------------------------------------
    // C-007 - unzip entry naming the target directory itself
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testUnzip_FileEntryNamingTheTargetDirectoryIsReported() throws Exception {
        final File d = dir("c7");

        for (final String entryName : new String[] { "", "." }) {
            final File zip = new File(d, "z" + Math.abs(entryName.hashCode()) + ".zip");

            try (ZipOutputStream zos = IOUtil.newZipOutputStream(IOUtil.newFileOutputStream(zip))) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write("x".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            final File target = dir("c7/out" + Math.abs(entryName.hashCode()));
            final IOException e = assertThrows(IOException.class, () -> IOUtil.unzip(zip, target));
            assertTrue(e.getMessage().contains("does not name a file inside the target dir"), "unexpected message: " + e.getMessage());
            assertEquals(Collections.emptyList(), names(target));
        }
    }

    @Test
    public void testUnzip_DirectoryEntriesAndOrdinaryEntriesStillExtract() throws Exception {
        final File d = dir("c7b");
        final File zip = new File(d, "ok.zip");

        try (ZipOutputStream zos = IOUtil.newZipOutputStream(IOUtil.newFileOutputStream(zip))) {
            // "./" is a DIRECTORY entry naming the target itself - harmless, and must keep working
            zos.putNextEntry(new ZipEntry("./"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("a/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("a/b.txt"));
            zos.write("B".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File target = dir("c7b/out");
        IOUtil.unzip(zip, target);

        assertEquals(Arrays.asList("a"), names(target));
        assertEquals("B", IOUtil.readAllToString(new File(new File(target, "a"), "b.txt")));
    }

    // ------------------------------------------------------------------------------------------------
    // C-011 - a caller-supplied stream that reads from the target cannot be detected
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWriteFromTheTargetsOwnStreamEmptiesIt() throws Exception {
        // This pins the behaviour the javadoc now describes. The File-typed twin rejects the same operation;
        // a stream carries no path, so this overload cannot. (The append twin grows the file without bound,
        // which is documented but deliberately not exercised here.)
        final File d = dir("c11");
        final File f = file(d, "self.txt", "0123456789");

        try (InputStream is = new FileInputStream(f)) {
            assertEquals(0, IOUtil.write(is, f));
        }

        assertEquals(0, f.length());

        final File g = file(d, "guarded.txt", "0123456789");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(g, g));
        assertEquals("0123456789", IOUtil.readAllToString(g));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(g, g));
        assertEquals("0123456789", IOUtil.readAllToString(g));
    }

    // ------------------------------------------------------------------------------------------------
    // C-010 - which exception a same-named non-empty directory really reports
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMoveToDirectory_ANonEmptyDirectoryOfTheSameNameReportsFileAlreadyExists() throws Exception {
        // The 2-arg overload used to document DirectoryNotEmptyException, which it cannot raise: without
        // REPLACE_EXISTING the move never gets as far as trying to merge trees.
        final File moving = dir("c10/src/payload");
        file(moving, "a.txt", "A");
        final File dest = dir("c10/dest");
        final File clash = dir("c10/dest/payload");
        file(clash, "b.txt", "B");

        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(moving, dest));
        assertTrue(moving.isDirectory(), "nothing is moved");

        assertThrows(java.nio.file.DirectoryNotEmptyException.class,
                () -> IOUtil.moveToDirectory(moving, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING));
        assertTrue(moving.isDirectory());
        assertEquals(Arrays.asList("b.txt"), names(clash));
    }

    private static void assertArrayEqualsBytes(final byte[] expected, final byte[] actual) {
        assertEquals(expected.length, actual.length);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "byte " + i);
        }
    }
}
