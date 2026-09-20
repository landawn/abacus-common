package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.IOUtil.LineIterationOptions;

/**
 * Regression tests for the third independent review pass over {@code IOUtil} (2026-08-30).
 *
 * <p>Each section names the finding it pins. Several of these are deliberate behaviour changes, not just bug
 * fixes, so the tests state the <i>new</i> contract explicitly.
 */
public class IOUtilNullFileReadTest extends TestBase {

    @TempDir
    Path tempFolder;

    private File dir(final String name) {
        final File d = new File(tempFolder.toFile(), name);
        assertTrue(d.mkdirs() || d.isDirectory());
        return d;
    }

    private File file(final String name, final String content) throws IOException {
        final File f = new File(tempFolder.toFile(), name);
        IOUtil.write(content, f);
        return f;
    }

    // ------------------------------------------------------------------------------------------------
    // B2: a null File reaches the read family as IllegalArgumentException, not NullPointerException.
    //
    // The class contract says a null source path is a programming error reported as IAE. Every one of these
    // used to die with "Cannot invoke java.io.File.getName() because source is null" inside openFile(..).
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByTheWholeReadFamily() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readBytes((File) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllChars((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readChars((File) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readToString((File) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines((File) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readFirstLine((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLastLine((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLine((File) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, new char[1]));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((File) null, StandardCharsets.UTF_8, new char[1]));
    }

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByToUrl() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrl(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls(new File[] { null }));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls(Arrays.asList((File) null)));
    }

    @Test
    public void testNullFileErrorMessageNamesTheArgument() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((File) null));
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("null"), "message should mention the null argument but was: " + e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // B3: a directory is the wrong KIND of path, which the contract reports as IAE - not as the
    // FileNotFoundException("... (Access is denied)") that FileInputStream produced on Windows.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testADirectoryIsRejectedAsIllegalArgumentByTheReadFamily() {
        final File d = dir("read-me-not");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString(d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readFirstLine(d));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(d, new byte[1]));
    }

    @Test
    public void testAMissingFileIsStillReportedAsNotFound() throws Exception {
        final File missing = new File(tempFolder.toFile(), "not-here.txt");

        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.readAllBytes(missing));
        assertTrue(e.getCause() instanceof FileNotFoundException, "expected FileNotFoundException but was: " + e.getCause());

        // The checked-IOException read overloads keep reporting it as the checked form.
        assertThrows(FileNotFoundException.class, () -> IOUtil.read(missing, new byte[1]));
    }

    // ------------------------------------------------------------------------------------------------
    // B9: a null ELEMENT inside a source collection is a bad argument too, not a missing file.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testANullCollectionElementIsRejectedAsIllegalArgument() throws Exception {
        final File target = new File(tempFolder.toFile(), "t.zip");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Arrays.asList((File) null), target));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Arrays.asList((File) null), new File(tempFolder.toFile(), "m.bin")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(Arrays.asList((File) null), l -> {
        }));
    }

    @Test
    public void testARejectedZipLeavesAnExistingTargetUntouched() throws Exception {
        // Deliberately NOT named "*.zip": the read family decompresses by file name, so reading a plain-text
        // guard file back through readAllToString would fail on the name rather than on the content.
        final File target = file("keep-archive.bin", "PRECIOUS");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Arrays.asList((File) null), target));

        assertEquals("PRECIOUS", IOUtil.readAllToString(target));
    }

    // ------------------------------------------------------------------------------------------------
    // D1: moveToDirectory no longer replaces an existing destination silently. Its copy twin refuses to
    // overwrite, so the operation that ALSO deletes the source must not be the quieter of the two.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMoveToDirectory_RefusesToOverwriteAndLeavesBothSidesIntact() throws Exception {
        final File srcDir = dir("mv-src");
        final File destDir = dir("mv-dest");
        final File src = new File(srcDir, "a.txt");
        IOUtil.write("NEW", src);
        final File clash = new File(destDir, "a.txt");
        IOUtil.write("PRECIOUS", clash);

        assertThrows(FileAlreadyExistsException.class, () -> IOUtil.moveToDirectory(src, destDir));

        assertEquals("PRECIOUS", IOUtil.readAllToString(clash));
        assertTrue(src.exists(), "a refused move must not delete the source");
        assertEquals("NEW", IOUtil.readAllToString(src));
    }

    @Test
    public void testMoveToDirectory_ReplacesOnlyWhenAskedTo() throws Exception {
        final File srcDir = dir("mv2-src");
        final File destDir = dir("mv2-dest");
        final File src = new File(srcDir, "a.txt");
        IOUtil.write("NEW", src);
        final File clash = new File(destDir, "a.txt");
        IOUtil.write("PRECIOUS", clash);

        IOUtil.moveToDirectory(src, destDir, StandardCopyOption.REPLACE_EXISTING);

        assertEquals("NEW", IOUtil.readAllToString(clash));
        assertFalse(src.exists());
    }

    @Test
    public void testMoveToDirectory_StillMovesIntoAFreeSlot() throws Exception {
        final File srcDir = dir("mv3-src");
        final File destDir = dir("mv3-dest");
        final File src = new File(srcDir, "a.txt");
        IOUtil.write("content", src);

        IOUtil.moveToDirectory(src, destDir);

        assertFalse(src.exists());
        assertEquals("content", IOUtil.readAllToString(new File(destDir, "a.txt")));
    }

    @Test
    public void testMoveToDirectory_AndCopyToDirectoryAgreeOnAnOccupiedDestination() throws Exception {
        final File srcDir = dir("agree-src");
        final File destDir = dir("agree-dest");
        final File src = new File(srcDir, "a.txt");
        IOUtil.write("NEW", src);
        IOUtil.write("PRECIOUS", new File(destDir, "a.txt"));

        // Both refuse; the point of the change is that they no longer disagree.
        assertThrows(IOException.class, () -> IOUtil.copyToDirectory(src, destDir));
        assertThrows(IOException.class, () -> IOUtil.moveToDirectory(src, destDir));
    }

    // ------------------------------------------------------------------------------------------------
    // B1/D2: walk(..) is the lazy twin of listFiles(..) and must answer the same inputs the same way -
    // including a null/missing parent, and including a directory symbolic link, which neither descends.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWalk_AnswersNullAndMissingLikeListFiles() {
        final File missing = new File(tempFolder.toFile(), "no-such-dir");

        assertEquals(List.of(), IOUtil.listFiles(null, true, false));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null, true, false));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null, true, true));

        assertEquals(List.of(), IOUtil.listFiles(missing, true, false));
        assertEquals(List.of(), IOUtil.walk(missing, true, false).toList());
    }

    @Test
    public void testWalk_AndListFilesReturnTheSameEntries() throws Exception {
        final File root = dir("walk-root");
        new File(root, "sub/deeper").mkdirs();
        IOUtil.write("1", new File(root, "top.txt"));
        IOUtil.write("2", new File(root, "sub/mid.txt"));
        IOUtil.write("3", new File(root, "sub/deeper/low.txt"));

        // Compared as ORDERED lists, not via sortedNames(..). Sorting both sides is what let walk(..) diverge
        // unnoticed: it delegated to the breadth-first Stream.listFiles(..) while the eager twin recurses
        // depth-first, so the two returned the same entries in different orders for years.
        // See IOUtilReviewFixes20260831cTest for the order-specific coverage.
        assertEquals(names(IOUtil.listFiles(root, true, false)), names(IOUtil.walk(root, true, false).toList()));
        assertEquals(names(IOUtil.listFiles(root, true, true)), names(IOUtil.walk(root, true, true).toList()));
        assertEquals(names(IOUtil.listFiles(root, false, false)), names(IOUtil.walk(root, false, false).toList()));
    }

    @Test
    public void testNeitherWalkNorListFilesDescendsIntoADirectorySymlink() throws Exception {
        final File real = dir("link-target");
        IOUtil.write("hidden", new File(real, "inside.txt"));
        final File root = dir("link-root");
        IOUtil.write("visible", new File(root, "plain.txt"));

        try {
            Files.createSymbolicLink(new File(root, "link").toPath(), real.toPath());
        } catch (final IOException | UnsupportedOperationException e) {
            // Creating a symlink needs a privilege this process may not hold (Windows without Developer
            // Mode). Abort so the run reports this as skipped rather than as a test that asserted nothing.
            Assumptions.abort("symbolic links cannot be created here: " + e);
        }

        // The link itself is reported by both, but neither follows it, so "inside.txt" never appears.
        final List<String> eager = sortedNames(IOUtil.listFiles(root, true, false));
        final List<String> lazy = sortedNames(IOUtil.walk(root, true, false).toList());

        assertEquals(eager, lazy);
        assertFalse(eager.contains("inside.txt"), "a directory symlink must not be descended into, but got: " + eager);
        assertTrue(eager.contains("link"));
        assertTrue(eager.contains("plain.txt"));
    }

    @Test
    public void testACyclicDirectorySymlinkDoesNotMakeTheTraversalRunForever() throws Exception {
        final File root = dir("cycle-root");
        IOUtil.write("x", new File(root, "f.txt"));

        try {
            Files.createSymbolicLink(new File(root, "self").toPath(), root.toPath());
        } catch (final IOException | UnsupportedOperationException e) {
            Assumptions.abort("symbolic links cannot be created here: " + e); // see above
        }

        // Both terminate, and both see exactly the two direct children.
        assertEquals(2, IOUtil.listFiles(root, true, false).size());
        assertEquals(2, IOUtil.walk(root, true, false).toList().size());
    }

    /**
     * File names in the order they were produced. Prefer this to {@link #sortedNames(List)} whenever the two
     * sides of an assertion are supposed to be the same traversal: sorting hides an ordering difference.
     */
    private static List<String> names(final List<File> files) {
        final List<String> names = new ArrayList<>();
        for (final File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    private static List<String> sortedNames(final List<File> files) {
        final List<String> names = names(files);
        names.sort(null);
        return names;
    }

    // ------------------------------------------------------------------------------------------------
    // D3: forEachLine is the streaming form of readAllLines and decompresses the same file names.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testForEachLine_DecompressesGzipExactlyLikeReadAllLines() throws Exception {
        final File gz = new File(tempFolder.toFile(), "app.log.gz");
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(gz)), StandardCharsets.UTF_8)) {
            w.write("alpha\nbeta\ngamma\n");
        }

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(gz, seen::add);

        assertEquals(IOUtil.readAllLines(gz), seen);
        assertEquals(List.of("alpha", "beta", "gamma"), seen);
    }

    @Test
    public void testForEachLine_ReadsTheFirstEntryOfAZipExactlyLikeReadAllLines() throws Exception {
        final File zip = new File(tempFolder.toFile(), "bundle.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("inner.txt"));
            zos.write("one\ntwo\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(zip, seen::add);

        assertEquals(IOUtil.readAllLines(zip), seen);
        assertEquals(List.of("one", "two"), seen);
    }

    @Test
    public void testForEachLine_DecompressesEveryMemberOfADirectoryAndOfACollection() throws Exception {
        final File d = dir("mixed");
        final File gz = new File(d, "a.log.gz");
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(gz)), StandardCharsets.UTF_8)) {
            w.write("zipped\n");
        }
        IOUtil.write("plain\n", new File(d, "b.log"));

        final List<String> fromDir = new ArrayList<>();
        IOUtil.forEachLine(d, fromDir::add);
        fromDir.sort(null);
        assertEquals(List.of("plain", "zipped"), fromDir);

        final List<String> fromCollection = new ArrayList<>();
        IOUtil.forEachLine(Arrays.asList(gz), fromCollection::add);
        assertEquals(List.of("zipped"), fromCollection);
    }

    @Test
    public void testForEachLine_StillHonoursSlicingAndCharsetOnACompressedSource() throws Exception {
        final File gz = new File(tempFolder.toFile(), "latin.log.gz");
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(gz)), StandardCharsets.ISO_8859_1)) {
            w.write("café\nrésumé\nnoël\n");
        }

        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(gz, LineIterationOptions.builder().charset(StandardCharsets.ISO_8859_1).offset(1).count(1).build(), seen::add);

        assertEquals(List.of("résumé"), seen);
    }

    @Test
    public void testForEachLine_ReleasesTheZipHandleSoTheArchiveCanBeDeleted() throws Exception {
        final File zip = new File(tempFolder.toFile(), "closeme.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("inner.txt"));
            zos.write("data\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        IOUtil.forEachLine(zip, l -> {
        });

        // A leaked ZipFile handle would keep this from succeeding on Windows.
        assertTrue(IOUtil.deleteIfExists(zip), "the ZipFile handle must be released once the file is exhausted");
    }

    // ------------------------------------------------------------------------------------------------
    // B5: the terminal array writes reported a null array as NPE while their File/OutputStream siblings
    // reported the documented IndexOutOfBoundsException.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testANullArrayIsAlwaysAnIndexOutOfBoundsException() throws Exception {
        final File out = new File(tempFolder.toFile(), "x.bin");

        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((char[]) null, 0, 3, new StringWriter()));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((char[]) null, 0, 3, new StringWriter(), true));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((byte[]) null, 0, 3, new ByteArrayOutputStream()));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((byte[]) null, 0, 3, new ByteArrayOutputStream(), true));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((char[]) null, 0, 3, out));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write((byte[]) null, 0, 3, out));
    }

    @Test
    public void testAnOutOfRangeSliceOfARealArrayIsAlsoIndexOutOfBounds() throws Exception {
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[2], 0, 5, new StringWriter()));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new byte[2], 0, 5, new ByteArrayOutputStream()));
    }

    @Test
    public void testAnEmptySliceStillWritesNothingWithoutComplaining() throws Exception {
        final StringWriter w = new StringWriter();
        IOUtil.write((char[]) null, 0, 0, w);
        IOUtil.write(new char[3], 3, 0, w);
        assertEquals("", w.toString());

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtil.write((byte[]) null, 0, 0, os);
        assertEquals(0, os.size());
    }

    // ------------------------------------------------------------------------------------------------
    // B7: LineIterationOptions validates at build(), where the caller set the value, rather than deep
    // inside whichever forEachLine overload eventually received it.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testLineIterationOptionsRejectsNegativeValuesAtBuildTime() {
        assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().offset(-1).build());
        assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().count(-1).build());
        assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().readThreads(-1).build());
        assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().processThreads(-1).build());
        assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().queueSize(-1).build());
    }

    @Test
    public void testLineIterationOptionsNamesTheFieldItRejected() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> LineIterationOptions.builder().offset(-1).build());
        assertTrue(e.getMessage().contains("offset"), "message should name the builder field but was: " + e.getMessage());
    }

    @Test
    public void testLineIterationOptionsKeepsItsDefaultsAndAcceptsValidValues() {
        final LineIterationOptions defaults = LineIterationOptions.builder().build();
        assertEquals(0, defaults.offset());
        assertEquals(Long.MAX_VALUE, defaults.count());
        assertEquals(0, defaults.readThreads());
        assertEquals(0, defaults.processThreads());
        assertEquals(0, defaults.queueSize());
        assertEquals(StandardCharsets.UTF_8, defaults.charset());

        final LineIterationOptions custom = LineIterationOptions.builder().offset(2).count(3).readThreads(1).processThreads(1).queueSize(8).build();
        assertEquals(2, custom.offset());
        assertEquals(3, custom.count());
        assertEquals(8, custom.queueSize());

        // @Value equality must survive the hand-written constructor.
        assertEquals(LineIterationOptions.builder().build(), LineIterationOptions.builder().build());
    }

    // ------------------------------------------------------------------------------------------------
    // B6: a failure while finishing a splitByLine part must surface as the checked IOException this
    // method declares, and must not be replaced by a secondary close failure.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSplitByLine_StillProducesTheDocumentedParts() throws Exception {
        final File log = new File(tempFolder.toFile(), "app.log");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("line ").append(i).append('\n');
        }
        IOUtil.write(sb, log);

        final File parts = dir("parts");
        IOUtil.splitByLine(log, 4, parts);

        final String[] names = parts.list();
        assertNotNull(names);
        Arrays.sort(names);
        assertEquals(4, names.length);
        assertEquals("app_0001.log", names[0]);

        // Every line survives, in order, once.
        final List<String> merged = new ArrayList<>();
        for (final String name : names) {
            merged.addAll(IOUtil.readAllLines(new File(parts, name)));
        }
        assertEquals(100, merged.size());
        assertEquals("line 0", merged.get(0));
        assertEquals("line 99", merged.get(99));
    }

    @Test
    public void testSplitByLine_FlushesEachFinishedPartAndReportsAFailureAsAnIOException() throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append("line ").append(i).append('\n');
        }
        final File log = file("rollover.log", sb.toString());

        final File parts = dir("rollover-parts");
        final File second = new File(parts, "rollover_0002.log");
        assertTrue(second.createNewFile());
        assertTrue(second.setReadOnly());
        Assumptions.assumeTrue(!second.canWrite(), "this platform/user can write a read-only file; nothing to provoke");

        try {
            assertThrows(IOException.class, () -> IOUtil.splitByLine(log, 4, parts));

            final File first = new File(parts, "rollover_0001.log");
            assertTrue(first.isFile(), "the first part should have been completed before the failure");
            assertEquals(10, IOUtil.readAllLines(first).size());
            assertEquals("line 0", IOUtil.readFirstLine(first));
            assertEquals("line 9", IOUtil.readLastLine(first));
        } finally {
            // Leave nothing read-only behind, or @TempDir cleanup fails on Windows.
            second.setWritable(true);
        }
    }

    @Test
    public void testSplitByLine_RejectsAPartPathOccupiedByADirectory() throws Exception {
        // The companion to the test above: the same shape, but with a directory in the way. A directory where
        // a destination file belongs is a bad argument, so it is reported as IllegalArgumentException - and
        // the primary failure still survives the cleanup block, which is what the pair is really guarding.
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append("line ").append(i).append('\n');
        }
        final File log = file("rollover-dir.log", sb.toString());

        final File parts = dir("rollover-dir-parts");
        assertTrue(new File(parts, "rollover-dir_0002.log").mkdirs());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(log, 4, parts));
        assertTrue(e.getMessage().contains("rollover-dir_0002.log"), "the primary failure should name the offending path, was: " + e.getMessage());

        final File first = new File(parts, "rollover-dir_0001.log");
        assertTrue(first.isFile(), "the first part should have been completed before the failure");
        assertEquals(10, IOUtil.readAllLines(first).size());
    }

    @Test
    public void testSplitByLine_RejectsADestinationThatIsAnExistingFile() throws Exception {
        final File log = file("dest-check.log", "a\nb\nc\n");
        final File destAsFile = file("dest-is-a-file", "x");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitByLine(log, 2, destAsFile));
    }

    // ------------------------------------------------------------------------------------------------
    // D5 / B4: skip(Reader, ..) uses the reader's own skip and stays exact; both skip overloads reject a
    // negative count through the shared bounds check.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSkip_ReaderIsExactAndLeavesTheReaderPositioned() throws Exception {
        final Reader r = new StringReader("abcdefghij");
        assertEquals(3, IOUtil.skip(r, 3));

        final char[] rest = new char[7];
        assertEquals(7, IOUtil.read(r, rest));
        assertEquals("defghij", new String(rest));
    }

    @Test
    public void testSkip_ReaderNeverCountsPastTheEnd() throws Exception {
        assertEquals(3, IOUtil.skip(new StringReader("abc"), 10));
        assertEquals(0, IOUtil.skip(new StringReader(""), 10));
        assertEquals(0, IOUtil.skip(new StringReader("abc"), 0));
    }

    @Test
    public void testSkip_IsExactAcrossEveryReaderKind() throws Exception {
        // The three types that take the seek fast path...
        assertEquals(5, IOUtil.skip(new StringReader("0123456789"), 5));
        assertEquals(5, IOUtil.skip(new java.io.CharArrayReader("0123456789".toCharArray()), 5));
        assertEquals(5, IOUtil.skip(new java.io.BufferedReader(new StringReader("0123456789")), 5));

        // ...and one that does not: an InputStreamReader inherits Reader.skip, so it goes through the read loop.
        final Reader isr = new java.io.InputStreamReader(new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertEquals(5, IOUtil.skip(isr, 5));
        final char[] rest = new char[5];
        assertEquals(5, IOUtil.read(isr, rest));
        assertEquals("56789", new String(rest));

        // Every kind stops at the end rather than reporting characters that were never there.
        assertEquals(3, IOUtil.skip(new java.io.BufferedReader(new StringReader("abc")), 99));
        assertEquals(3, IOUtil.skip(new java.io.CharArrayReader("abc".toCharArray()), 99));
    }

    @Test
    public void testSkip_ReaderFallsBackWhenTheReaderDeclinesToSkip() throws Exception {
        // A Reader whose skip(..) always returns 0 must still be advanced, by reading and discarding. It is also
        // why the fast path is restricted by type: Reader.skip(long) is itself a read loop that never
        // terminates on a reader whose read(..) returns 0, so it must not be handed the count.
        final Reader stubborn = new Reader() {
            private final Reader delegate = new StringReader("abcdefghij");

            @Override
            public long skip(final long n) {
                return 0;
            }

            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                return delegate.read(cbuf, off, len);
            }

            @Override
            public void close() throws IOException {
                delegate.close();
            }
        };

        assertEquals(4, IOUtil.skip(stubborn, 4));

        final char[] rest = new char[6];
        assertEquals(6, IOUtil.read(stubborn, rest));
        assertEquals("efghij", new String(rest));
    }

    @Test
    public void testBothSkipOverloadsRejectANegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip(new StringReader("a"), -1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip(new ByteArrayInputStream(new byte[] { 1 }), -1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skipFully(new StringReader("a"), -1));
    }

    @Test
    public void testSkipFully_StillFailsWhenTheSourceIsShort() {
        assertThrows(IOException.class, () -> IOUtil.skipFully(new StringReader("ab"), 5));
        assertThrows(IOException.class, () -> IOUtil.skipFully(new ByteArrayInputStream(new byte[2]), 5));
    }

    @Test
    public void testReadChars_WithAnOffsetStillSlicesCorrectlyOverTheFastPath() throws Exception {
        final File f = file("sliced.txt", "0123456789");

        assertEquals("34567", new String(IOUtil.readChars(f, 3, 5)));
        assertEquals("789", IOUtil.readToString(f, 7, 100));
        assertEquals("", IOUtil.readToString(f, 100, 5));
    }

    // ------------------------------------------------------------------------------------------------
    // B10: a percent-encoded path separator is rejected rather than decoded into a live separator.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testToFile_RejectsAPercentEncodedSeparator() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(URI.create("file:///tmp/a%2F..%2Fb").toURL()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(URI.create("file:///tmp/a%2f..%2fb").toURL()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(URI.create("file:///tmp/a%5Cb").toURL()));
    }

    @Test
    public void testToFile_StillDecodesOrdinaryEscapes() throws Exception {
        assertEquals("a b.txt", IOUtil.toFile(URI.create("file:///tmp/a%20b.txt").toURL()).getName());
        assertEquals("café.txt", IOUtil.toFile(URI.create("file:///tmp/caf%C3%A9.txt").toURL()).getName());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testToFile_ToleratesATruncatedOrMalformedEscape() throws Exception {
        assertEquals("a%", IOUtil.toFile(new java.net.URL("file:/tmp/a%")).getName());
        assertEquals("a%2", IOUtil.toFile(new java.net.URL("file:/tmp/a%2")).getName());
        assertEquals("a%zz.txt", IOUtil.toFile(new java.net.URL("file:/tmp/a%zz.txt")).getName());

        // ...and an escape that is complete but not a separator is still decoded normally.
        assertEquals("a+b.txt", IOUtil.toFile(new java.net.URL("file:/tmp/a%2Bb.txt")).getName());
    }

    @Test
    public void testZip_AndUnzipRejectANullSource() throws Exception {
        final File target = new File(tempFolder.toFile(), "null-src.zip");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip((File) null, target));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip((File) null, target, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.unzip(null, dir("unzip-null-dest")));
    }

    @Test
    public void testTheDirectoryOperationsAllRejectANullSourceAsABadArgument() {
        // Every one of these funnels through checkFileExists/checkDirectoryExists except moveToDirectory,
        // which tested srcFile.exists() directly and so answered a null source with NullPointerException.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(null, dir("copy-null-dest")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(null, dir("copy-null-dest2")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(null, dir("move-null-dest")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(null, dir("move-null-dest2"), StandardCopyOption.REPLACE_EXISTING));
    }

    @Test
    public void testToUrl_AndToFileStillRoundTrip() throws Exception {
        final File f = file("round trip.txt", "x");
        assertEquals(f.getAbsoluteFile(), IOUtil.toFile(IOUtil.toUrl(f)).getAbsoluteFile());
    }

    // ------------------------------------------------------------------------------------------------
    // D4: copyFile(File, OutputStream) is the alias of write(File, OutputStream) its javadoc claims,
    // failure mode included.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyFile_ToOutputStreamAgreesWithWriteOnAMissingSource() {
        final File missing = new File(tempFolder.toFile(), "gone.bin");

        assertThrows(FileNotFoundException.class, () -> IOUtil.write(missing, new ByteArrayOutputStream()));
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyFile(missing, new ByteArrayOutputStream()));
    }

    @Test
    public void testCopyFile_ToOutputStreamStillCopiesTheContentAndCount() throws Exception {
        final File f = file("payload.bin", "0123456789");

        final ByteArrayOutputStream viaCopy = new ByteArrayOutputStream();
        final ByteArrayOutputStream viaWrite = new ByteArrayOutputStream();

        assertEquals(10, IOUtil.copyFile(f, viaCopy));
        assertEquals(10, IOUtil.write(f, viaWrite));
        assertEquals("0123456789", viaCopy.toString("UTF-8"));
        assertEquals(viaWrite.toString("UTF-8"), viaCopy.toString("UTF-8"));
    }

    // ------------------------------------------------------------------------------------------------
    // D7: zip/unzip can now be told which charset the entry NAMES use.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testZip_AndUnzipRoundTripThroughANonUtf8EntryNameCharset() throws Exception {
        final Charset cp = StandardCharsets.ISO_8859_1;
        final File src = dir("cs-src");
        IOUtil.write("hello", new File(src, "café.txt"));

        final File archive = new File(tempFolder.toFile(), "cs.zip");
        IOUtil.zip(src, archive, cp);

        final File out = dir("cs-out");
        IOUtil.unzip(archive, out, cp);

        assertEquals("hello", IOUtil.readAllToString(new File(out, "cs-src/café.txt")));
    }

    @Test
    public void testTheCharsetLessZipAndUnzipStillDefaultToUtf8() throws Exception {
        final File src = dir("utf8-src");
        IOUtil.write("hello", new File(src, "café.txt"));

        final File archive = new File(tempFolder.toFile(), "utf8.zip");
        IOUtil.zip(src, archive);

        final File out = dir("utf8-out");
        IOUtil.unzip(archive, out);

        assertEquals("hello", IOUtil.readAllToString(new File(out, "utf8-src/café.txt")));

        // and the explicit-UTF-8 form agrees with the default one
        final File out2 = dir("utf8-out2");
        IOUtil.unzip(archive, out2, StandardCharsets.UTF_8);
        assertEquals("hello", IOUtil.readAllToString(new File(out2, "utf8-src/café.txt")));
    }

    @Test
    public void testZip_WithACharsetStillValidatesItsArgumentsBeforeTouchingTheTarget() throws Exception {
        final File target = file("guard-archive.bin", "PRECIOUS");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip((Collection<File>) null, target, StandardCharsets.UTF_8));
        assertEquals("PRECIOUS", IOUtil.readAllToString(target));
    }

    // ------------------------------------------------------------------------------------------------
    // Other: the low-level read(..) bounds failure now says what was out of bounds.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testReadBoundsFailureCarriesAMessage() throws Exception {
        final File f = file("bounds.bin", "abcdef");

        final IndexOutOfBoundsException fromFile = assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(f, new byte[4], 0, 99));
        assertNotNull(fromFile.getMessage());
        assertTrue(fromFile.getMessage().contains("99"), "message should name the length but was: " + fromFile.getMessage());

        final IndexOutOfBoundsException fromReader = assertThrows(IndexOutOfBoundsException.class,
                () -> IOUtil.read(new StringReader("abc"), new char[4], 0, 99));
        assertNotNull(fromReader.getMessage());
    }

    @Test
    public void testReadBoundsAreCheckedBeforeTheFileIsOpened() {
        // The range used to be validated from inside the lambda, after the file had already been opened - so a
        // missing file and a bad range reported different things depending on which happened to fail first.
        final File missing = new File(tempFolder.toFile(), "never-created.bin");

        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(missing, new byte[4], 0, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(missing, new char[4], 0, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(missing, StandardCharsets.UTF_8, new char[4], 0, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.read(missing, new byte[4], -1, 2));
    }

    @Test
    public void testReadWithAValidRangeStillWorks() throws Exception {
        final File f = file("range.bin", "abcdef");
        final byte[] buf = new byte[10];

        assertEquals(4, IOUtil.read(f, buf, 2, 4));
        assertEquals("abcd", new String(buf, 2, 4, StandardCharsets.UTF_8));
        assertEquals(0, IOUtil.read(f, buf, 0, 0));
    }

    // ------------------------------------------------------------------------------------------------
    // J3/J4: documented behaviour that had never been pinned - a BOM is content, and contentEquals(File,
    // File) has the same null contract as its stream and reader twins.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testAByteOrderMarkIsContentAndIsNeverStripped() throws Exception {
        final File bom = new File(tempFolder.toFile(), "bom.csv");
        try (java.io.OutputStream os = new FileOutputStream(bom)) {
            os.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            os.write("id,name".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals('\uFEFF', IOUtil.readAllToString(bom).charAt(0));
        assertEquals('\uFEFF', IOUtil.readFirstLine(bom).charAt(0));
        assertEquals(8, IOUtil.readAllToString(bom).length());
    }

    @Test
    public void testContentEquals_FileHasTheSameNullContractAsItsTwins() throws Exception {
        final File a = file("ce-a.txt", "same");
        final File b = file("ce-b.txt", "same");

        assertTrue(IOUtil.contentEquals((File) null, (File) null));
        assertFalse(IOUtil.contentEquals(a, (File) null));
        assertFalse(IOUtil.contentEquals((File) null, a));
        assertTrue(IOUtil.contentEquals(a, b));
        assertTrue(IOUtil.contentEquals(new File(tempFolder.toFile(), "nope1"), new File(tempFolder.toFile(), "nope2")));
    }
}
