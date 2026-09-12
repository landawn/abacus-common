package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the seventh independent review pass over {@code IOUtil} (2026-08-31).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>B1</b> - {@code IOUtil.JAVA_VERSION} can no longer abort this class's static initializer.
 *       {@code JavaVersion.of} now strips the JEP 223 pre-release/build suffix, so an undotted build such as
 *       {@code "25-ea"} or {@code "17-internal"} resolves instead of throwing; and
 *       {@code IOUtil.resolveJavaVersion()} falls back to {@code java.specification.version} and then to
 *       {@code JAVA_RECENT} rather than letting anything escape.</li>
 *   <li><b>B2</b> - a directory handed to a write <i>destination</i> is an {@code IllegalArgumentException},
 *       not the platform's {@code FileNotFoundException: ... (Access is denied)}. Three of the four
 *       wrong-kind quadrants already answered that way; this was the fourth.</li>
 *   <li><b>B3</b> - {@code zip} stamps every entry with its source's last-modified time (the directory walk
 *       used to date entries "now" while a single-file source kept them), and {@code unzip} restores it, so a
 *       round trip preserves timestamps.</li>
 *   <li><b>B4/B5</b> - {@code writeLines(.., Writer, false)} no longer flushes the caller's {@code Writer}.
 *       It used to, for every {@code Writer} that was not already a {@code java.io.BufferedWriter} and so had
 *       to be wrapped in a pooled buffer - which also made an empty {@code Collection} and an empty
 *       non-{@code Collection} {@code Iterable} behave differently.</li>
 *   <li><b>D1</b> - {@code deleteRecursivelyIfExists} is best-effort like its sibling
 *       {@code deleteFilesFromDirectory}; it used to stop at the first entry it could not remove, leaving
 *       far more behind than the {@code false} result revealed.</li>
 *   <li><b>D2</b> - {@code skipFully} reports a short input as {@code EOFException}, the type Commons IO
 *       uses, rather than a bare {@code IOException} indistinguishable from a read failure.</li>
 *   <li><b>D3</b> - {@code simplifyPath} treats only ASCII letters as Windows drive letters.</li>
 *   <li><b>D4</b> - {@code readAllToString(Reader)} accumulates directly instead of building a whole
 *       {@code char[]} and copying it again.</li>
 *   <li><b>J2</b> - the documented difference between {@code write((CharSequence) null, file)} (empty) and
 *       {@code writeLine(null, file)} ({@code "null\n"}).</li>
 * </ul>
 */
public class IOUtilJavaVersionAndZipTest extends TestBase {

    @TempDir
    File tempDir;

    // ------------------------------------------------------------------ helpers

    /** A {@code Writer} that counts {@code flush()} calls and records everything written to it. */
    private static final class CountingWriter extends Writer {
        private final StringBuilder written = new StringBuilder();
        private int flushes = 0;

        @Override
        public void write(final char[] cbuf, final int off, final int len) {
            written.append(cbuf, off, len);
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    /** An {@code Iterable} that is deliberately NOT a {@code Collection}. */
    private static final class PlainIterable<T> implements Iterable<T> {
        private final List<T> backing;

        PlainIterable(final List<T> backing) {
            this.backing = backing;
        }

        @Override
        public Iterator<T> iterator() {
            return backing.iterator();
        }
    }

    private File newFile(final String name, final String content) throws IOException {
        final File f = new File(tempDir, name);
        IOUtil.write(content, f);
        return f;
    }

    private File newDir(final String name) {
        final File d = new File(tempDir, name);
        assertTrue(d.mkdirs() || d.isDirectory());
        return d;
    }

    // =================================================================== B1

    @Test
    public void testJavaVersionAcceptsRealWorldPreReleaseAndBuildSuffixes() {
        // Every one of these is a java.version string a real JVM has reported, and every one of them used to
        // be rejected outright - which, read in IOUtil's static initializer, was an ExceptionInInitializerError.
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17-internal"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21-internal"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25-ea"));
        assertEquals(JavaVersion.JAVA_22, JavaVersion.of("22-beta"));
        assertEquals(JavaVersion.JAVA_13, JavaVersion.of("13-loom"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21+35"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.9+9"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25-ea+12-1234"));
    }

    @Test
    public void testJavaVersionStillParsesTheFormsItAlreadyDid() {
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8.0_291"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8.0-internal"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.of("1.7.0_80-b15"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.of("1.6.0_45"));
        assertEquals(JavaVersion.JAVA_9, JavaVersion.of("9"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11.0.2"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11.0.2-internal"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("99"));
    }

    @Test
    public void testJavaVersionStillRejectsWhatIsNotAVersion() {
        // A string that is nothing but a suffix has no version number to isolate, so it must still be rejected
        // rather than silently becoming the empty string.
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("-1"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("+1"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("-"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("abc-def"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("0"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(""));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(null));
    }

    @Test
    public void testJavaVersionRejectionNamesTheCallersOwnString() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("nope-suffix"));
        assertTrue(e.getMessage().contains("nope-suffix"), "message should name the original string, was: " + e.getMessage());
    }

    @Test
    public void testIoUtilJavaVersionConstantIsResolved() {
        assertNotNull(IOUtil.JAVA_VERSION);
        assertEquals(JavaVersion.of(System.getProperty("java.version")), IOUtil.JAVA_VERSION);
    }

    @Test
    public void testResolveJavaVersionNeverThrowsAndFallsBack() throws Exception {
        final Method resolve = IOUtil.class.getDeclaredMethod("resolveJavaVersion");
        resolve.setAccessible(true);

        final String realVersion = System.getProperty("java.version");
        final String realSpec = System.getProperty("java.specification.version");

        try {
            // 1. An unparseable java.version falls back to java.specification.version rather than throwing.
            System.setProperty("java.version", "not-a-version");
            System.setProperty("java.specification.version", "17");
            assertEquals(JavaVersion.JAVA_17, resolve.invoke(null));

            // 2. A missing java.version does the same.
            System.clearProperty("java.version");
            assertEquals(JavaVersion.JAVA_17, resolve.invoke(null));

            // 3. An empty java.version does the same.
            System.setProperty("java.version", "");
            assertEquals(JavaVersion.JAVA_17, resolve.invoke(null));

            // 4. With BOTH unusable it still answers, rather than taking the whole class down.
            System.setProperty("java.version", "0");
            System.setProperty("java.specification.version", "0");
            assertEquals(JavaVersion.JAVA_RECENT, resolve.invoke(null));

            // 5. And a normal java.version is still what wins.
            System.setProperty("java.version", "21.0.1");
            assertEquals(JavaVersion.JAVA_21, resolve.invoke(null));
        } finally {
            restoreProperty("java.version", realVersion);
            restoreProperty("java.specification.version", realSpec);
        }
    }

    private static void restoreProperty(final String key, final String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    // =================================================================== B2

    @Test
    public void testDirectoryAsWriteDestinationIsIllegalArgument() throws Exception {
        final File dir = newDir("dest-is-a-directory");
        final File src = newFile("b2-src.txt", "payload");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[] { 1 }, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[] { 'x' }, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", StandardCharsets.UTF_8, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(src, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(List.of("x"), dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new byte[] { 1 }, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("x", dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine("x", dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(List.of("x"), dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(src.toURI().toURL(), dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(new File[] { src }, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(src, dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream(dir, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream(dir.getAbsolutePath()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter(dir, StandardCharsets.UTF_8, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter(dir, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream(dir));

        try (java.io.InputStream is = IOUtil.newFileInputStream(src)) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.write(is, dir));
        }

        try (java.io.Reader r = IOUtil.newFileReader(src)) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.write(r, dir));
        }
    }

    /**
     * The narrow translation {@code zip} performs for an entry name the charset cannot encode must not swallow
     * the {@code IllegalArgumentException} raised while <i>acquiring</i> the target. It briefly did: the catch
     * sat on the try-<b>with-resources</b> statement, whose resource expression opens the target, so
     * {@code openFileOutputStream}'s "is a directory, not a file" came back as
     * {@code IOException("Zip entry name cannot be encoded in UTF-8 ..")} - the wrong type, naming a cause that
     * had not happened.
     */
    @Test
    public void testZipToADirectoryTargetStaysIllegalArgument() throws Exception {
        final File dir = newDir("zip-target-is-a-directory");
        final File src = newFile("zip-target-src.txt", "payload");
        final File srcDir = newDir("zip-target-src-dir");
        IOUtil.write("payload", new File(srcDir, "inner.txt"));

        // Both source kinds - a single file (zipSingleFile) and a directory (the walk) - and both charset
        // overloads, plus the Collection entry points, which validate their sources before opening the target.
        assertDirectoryTargetRejected(() -> IOUtil.zip(src, dir));
        assertDirectoryTargetRejected(() -> IOUtil.zip(src, dir, StandardCharsets.UTF_8));
        assertDirectoryTargetRejected(() -> IOUtil.zip(srcDir, dir));
        assertDirectoryTargetRejected(() -> IOUtil.zip(srcDir, dir, StandardCharsets.ISO_8859_1));
        assertDirectoryTargetRejected(() -> IOUtil.zip(List.of(src, srcDir), dir));
        assertDirectoryTargetRejected(() -> IOUtil.zip(List.of(src, srcDir), dir, StandardCharsets.UTF_8));

        // and the rejected target is untouched - still a directory, still empty
        assertTrue(dir.isDirectory());
        assertEquals(0, dir.listFiles().length);
    }

    private static void assertDirectoryTargetRejected(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertTrue(e.getMessage().contains("is a directory, not a file"), "unexpected message: " + e.getMessage());
    }

    @Test
    public void testDirectoryAsReadSourceIsIllegalArgument() {
        final File dir = newDir("source-is-a-directory");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileInputStream(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileInputStream(dir.getAbsolutePath()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileReader(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileReader(dir, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(dir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader(dir, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream(dir));
    }

    @Test
    public void testDirectoryRejectionKeepsThePlatformFailureAsItsCause() {
        final File dir = newDir("cause-carrier");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[] { 1 }, dir));
        assertTrue(e.getMessage().contains("is a directory, not a file"), e.getMessage());
        assertInstanceOf(IOException.class, e.getCause(), "the platform's own failure should be preserved as the cause");
    }

    @Test
    public void testAMissingFileIsStillFileNotFound() {
        // Only a wrong KIND of path is reclassified; absence keeps the exception the contract calls for.
        final File missing = new File(tempDir, "no-such-file.txt");

        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.read(missing, new byte[4]));
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileInputStream(missing));
    }

    @Test
    public void testWritingToARegularFileStillWorks() throws Exception {
        // The guard must not disturb the happy path.
        final File f = new File(tempDir, "b2-happy.txt");
        IOUtil.write("hello", f);
        assertEquals("hello", IOUtil.readAllToString(f));

        IOUtil.append(" world", f);
        assertEquals("hello world", IOUtil.readAllToString(f));
    }

    // =================================================================== B3

    /** A timestamp that is NOT a multiple of two seconds, so MS-DOS-resolution storage would lose it. */
    private static final long ODD_STAMP = 1_000_000_001_000L;

    @Test
    public void testZipOfASingleFilePreservesTheEntryTimestamp() throws Exception {
        final File src = newFile("b3-single.txt", "hello");
        assertTrue(src.setLastModified(ODD_STAMP));

        final File archive = new File(tempDir, "b3-single-archive.zip");
        IOUtil.zip(src, archive);

        assertEquals(ODD_STAMP, onlyFileEntryTime(archive));
    }

    @Test
    public void testZipOfADirectoryPreservesTheEntryTimestamp() throws Exception {
        // This is the fix: the Files.walkFileTree branch never stamped its entries, so ZipOutputStream dated
        // them "now" - the same zip(File, File) call answering differently depending on the kind of source.
        final File dir = newDir("b3-dir");
        final File inner = new File(dir, "inner.txt");
        IOUtil.write("hello", inner);
        assertTrue(inner.setLastModified(ODD_STAMP));

        final File archive = new File(tempDir, "b3-dir-archive.zip");
        IOUtil.zip(dir, archive);

        assertEquals(ODD_STAMP, onlyFileEntryTime(archive));
    }

    @Test
    public void testZipUnzipRoundTripPreservesFileAndDirectoryTimestamps() throws Exception {
        final File dir = newDir("b3-round");
        final File sub = new File(dir, "sub");
        assertTrue(sub.mkdirs());
        final File leaf = new File(sub, "leaf.txt");
        IOUtil.write("content", leaf);

        assertTrue(leaf.setLastModified(ODD_STAMP));
        assertTrue(sub.setLastModified(ODD_STAMP));

        final File archive = new File(tempDir, "b3-round-archive.zip");
        IOUtil.zip(dir, archive);

        final File out = newDir("b3-round-out");
        IOUtil.unzip(archive, out);

        final File extractedLeaf = new File(out, "b3-round/sub/leaf.txt");
        final File extractedSub = new File(out, "b3-round/sub");

        assertTrue(extractedLeaf.exists(), "leaf should have been extracted");
        assertEquals("content", IOUtil.readAllToString(extractedLeaf));
        assertEquals(ODD_STAMP, extractedLeaf.lastModified());

        // Directory times are applied after every entry has been written; extracting leaf.txt into sub/ would
        // otherwise have bumped sub/'s own time back to "now".
        assertTrue(extractedSub.isDirectory());
        assertEquals(ODD_STAMP, extractedSub.lastModified());
    }

    @Test
    public void testUnzipOfAnEntryWithoutATimestampStillExtracts() throws Exception {
        final File archive = new File(tempDir, "b3-no-time.zip");

        try (java.util.zip.ZipOutputStream zos = IOUtil.newZipOutputStream(IOUtil.newFileOutputStream(archive))) {
            final ZipEntry entry = new ZipEntry("plain.txt");
            // Deliberately no setTime/setLastModifiedTime.
            zos.putNextEntry(entry);
            zos.write("body".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File out = newDir("b3-no-time-out");
        IOUtil.unzip(archive, out);

        final File extracted = new File(out, "plain.txt");
        assertTrue(extracted.exists());
        assertEquals("body", IOUtil.readAllToString(extracted));
    }

    private static long onlyFileEntryTime(final File archive) throws IOException {
        try (ZipFile zf = new ZipFile(archive)) {
            final Enumeration<? extends ZipEntry> entries = zf.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory()) {
                    final FileTime time = entry.getLastModifiedTime();
                    assertNotNull(time, "entry '" + entry.getName() + "' carries no timestamp");
                    return time.toMillis();
                }
            }
        }

        throw new AssertionError("no file entry in " + archive);
    }

    // =================================================================== B4 / B5

    @Test
    public void testWriteLinesDoesNotFlushACallerWriterWhenFlushIsFalse() throws Exception {
        final CountingWriter w = new CountingWriter();
        IOUtil.writeLines(Arrays.asList("a", "b"), w, false);

        assertEquals(0, w.flushes, "flush=false must not flush the caller's Writer");
        assertEquals("a\nb\n", w.written.toString(), "every line must still reach the caller's Writer");
    }

    @Test
    public void testWriteLinesFlushesACallerWriterWhenFlushIsTrue() throws Exception {
        final CountingWriter w = new CountingWriter();
        IOUtil.writeLines(Arrays.asList("a", "b"), w, true);

        assertTrue(w.flushes > 0, "flush=true must flush the caller's Writer");
        assertEquals("a\nb\n", w.written.toString());
    }

    @Test
    public void testWriteLinesFlushBehaviourDoesNotDependOnTheWriterKind() throws Exception {
        // A java.io.BufferedWriter is used as-is; anything else is wrapped in a pooled buffer. The wrapping
        // used to leak into the contract, because draining the pooled buffer also flushed its destination.
        final CountingWriter plain = new CountingWriter();
        IOUtil.writeLines(Arrays.asList("a", "b"), plain, false);

        final CountingWriter behindBuffer = new CountingWriter();
        final java.io.BufferedWriter buffered = new java.io.BufferedWriter(behindBuffer);
        IOUtil.writeLines(Arrays.asList("a", "b"), buffered, false);

        assertEquals(behindBuffer.flushes, plain.flushes);
        assertEquals(0, plain.flushes);
    }

    @Test
    public void testWriteLinesTreatsAnEmptyIterableTheSameWhetherOrNotItIsACollection() throws Exception {
        final CountingWriter fromCollection = new CountingWriter();
        IOUtil.writeLines(Collections.<String> emptyList(), fromCollection, false);

        final CountingWriter fromIterable = new CountingWriter();
        IOUtil.writeLines(new PlainIterable<>(new ArrayList<String>()), fromIterable, false);

        assertEquals(fromCollection.flushes, fromIterable.flushes);
        assertEquals(0, fromIterable.flushes);
        assertEquals("", fromIterable.written.toString());
    }

    @Test
    public void testWriteLinesIteratorOverloadHonoursFlushFalseToo() throws Exception {
        final CountingWriter w = new CountingWriter();
        IOUtil.writeLines(Arrays.asList("a", "b").iterator(), w, false);

        assertEquals(0, w.flushes);
        assertEquals("a\nb\n", w.written.toString());
    }

    @Test
    public void testWriteLinesToAFileStillFlushesAndTruncates() throws Exception {
        // The File overloads pass flush=true internally; nothing about them changes.
        final File f = newFile("b4-file.txt", "SEED-SEED-SEED");
        IOUtil.writeLines(Arrays.asList("a", "b"), f);
        assertEquals("a\nb\n", IOUtil.readAllToString(f));

        IOUtil.writeLines(Collections.<String> emptyList(), f);
        assertEquals(0, f.length(), "an empty write still truncates");
    }

    /** A {@code Writer} that fails once a given number of characters has been written. */
    private static final class FailingWriter extends Writer {
        private final int failAfter;
        private int written = 0;

        FailingWriter(final int failAfter) {
            this.failAfter = failAfter;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            written += len;

            if (written > failAfter) {
                throw new IOException("destination failed");
            }
        }

        @Override
        public void flush() throws IOException {
            throw new IOException("destination failed");
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    @Test
    public void testWriteLinesStillReportsADestinationFailureAsIoException() {
        // Regression guard for the 2026-08-30e fix: releasing the pooled writer must not replace the primary
        // failure with an UncheckedIOException that no catch (IOException) ever sees.
        final List<String> lines = new ArrayList<>();

        for (int i = 0; i < 4096; i++) {
            lines.add("line-" + i);
        }

        assertThrows(IOException.class, () -> IOUtil.writeLines(lines, new FailingWriter(16), false));
        assertThrows(IOException.class, () -> IOUtil.writeLines(lines, new FailingWriter(16), true));
    }

    // =================================================================== D1

    @Test
    public void testDeleteRecursivelyIsBestEffort() throws Exception {
        final File dir = newDir("d1-best-effort");
        final File[] kids = new File[6];

        for (int i = 0; i < kids.length; i++) {
            kids[i] = new File(dir, "f" + i + ".txt");
            IOUtil.write("x", kids[i]);
        }

        // Holding a file open makes Windows refuse to delete it. On platforms where the delete succeeds
        // anyway there is nothing to be best-effort about, so the assertion adapts.
        try (FileInputStream lock = new FileInputStream(kids[2])) {
            final boolean deleted = IOUtil.deleteRecursivelyIfExists(dir);

            int survivors = 0;

            for (final File kid : kids) {
                if (kid.exists()) {
                    survivors++;
                }
            }

            if (survivors == 0) {
                assertTrue(deleted, "everything went away, so the result must be true");
            } else {
                assertFalse(deleted, "something survived, so the result must be false");
                assertEquals(1, survivors, "only the locked entry may survive: a failure must not stop the rest");
                assertTrue(kids[2].exists(), "the locked entry is the one that should survive");
                assertTrue(dir.exists(), "a directory that still holds something is not removed");
            }
        }
    }

    @Test
    public void testDeleteRecursivelyStillDeletesAWholeTreeAndReportsTrue() throws Exception {
        final File dir = newDir("d1-whole-tree");
        final File sub = new File(dir, "sub/deeper");
        assertTrue(sub.mkdirs());
        IOUtil.write("a", new File(dir, "a.txt"));
        IOUtil.write("b", new File(sub, "b.txt"));

        assertTrue(IOUtil.deleteRecursivelyIfExists(dir));
        assertFalse(dir.exists());
    }

    @Test
    public void testDeleteRecursivelyOnAbsentOrNullInputIsFalse() {
        assertFalse(IOUtil.deleteRecursivelyIfExists(null));
        assertFalse(IOUtil.deleteRecursivelyIfExists(new File(tempDir, "never-created")));
    }

    @Test
    public void testDeleteRecursivelySiblingsAgreeOnEffort() throws Exception {
        // The two recursive deleters walk the same shape; only the fate of the top directory differs.
        final File a = newDir("d1-agree-a");
        final File b = newDir("d1-agree-b");

        for (final File dir : new File[] { a, b }) {
            for (int i = 0; i < 4; i++) {
                IOUtil.write("x", new File(dir, "f" + i + ".txt"));
            }
        }

        try (FileInputStream lockA = new FileInputStream(new File(a, "f1.txt")); //
             FileInputStream lockB = new FileInputStream(new File(b, "f1.txt"))) {

            IOUtil.deleteRecursivelyIfExists(a);
            IOUtil.deleteFilesFromDirectory(b);

            assertEquals(countChildren(b), countChildren(a), "the two deleters must leave the same amount behind");
        }
    }

    private static int countChildren(final File dir) {
        final File[] kids = dir.listFiles();
        return kids == null ? 0 : kids.length;
    }

    // =================================================================== D2

    @Test
    public void testSkipFullyReportsAShortStreamAsEofException() {
        final EOFException e = assertThrows(EOFException.class, () -> IOUtil.skipFully(new ByteArrayInputStream(new byte[3]), 10));
        assertTrue(e.getMessage().contains("10"), e.getMessage());

        // EOFException is an IOException, so callers that only catch IOException are unaffected.
        assertInstanceOf(IOException.class, e);
    }

    @Test
    public void testSkipFullyReaderReportsAShortReaderAsEofException() {
        assertThrows(EOFException.class, () -> IOUtil.skipFully(new StringReader("abc"), 10));
    }

    @Test
    public void testSkipFullySucceedsWhenThereIsEnoughInput() throws Exception {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[10]);
        IOUtil.skipFully(bytes, 10);
        assertEquals(-1, bytes.read());

        final StringReader chars = new StringReader("abcdefghij");
        IOUtil.skipFully(chars, 10);
        assertEquals(-1, chars.read());

        final ByteArrayInputStream emptySkip = new ByteArrayInputStream(new byte[10]);
        IOUtil.skipFully(emptySkip, 0);
        assertEquals(0, emptySkip.read());
    }

    // =================================================================== D3

    @Test
    public void testSimplifyPathTreatsOnlyAsciiLettersAsDriveLetters() {
        // "\u03b1:" is not a drive, so the path is relative and ".." may ascend past it - exactly as the
        // neighbouring "1:" case already did.
        assertEquals(IOUtil.simplifyPath("1:/a/../.."), IOUtil.simplifyPath("\u03b1:/a/../.."));
        assertEquals(".", IOUtil.simplifyPath("\u03b1:/a/../.."));
        assertEquals(".", IOUtil.simplifyPath("\u4e00:/a/../.."));
    }

    @Test
    public void testSimplifyPathStillHandlesRealWindowsDrives() {
        assertEquals("C:/b", IOUtil.simplifyPath("C:/a/../b"));
        assertEquals("c:/b", IOUtil.simplifyPath("c:/a/../b"));
        assertEquals("C:/", IOUtil.simplifyPath("C:/.."));
        assertEquals("C:/", IOUtil.simplifyPath("C:/"));
        assertEquals("Z:/x/y", IOUtil.simplifyPath("Z:\\x\\.\\y"));
    }

    @Test
    public void testSimplifyPathUnixFormsAreUnchanged() {
        assertEquals("/a/b", IOUtil.simplifyPath("/a/./b"));
        assertEquals("/b", IOUtil.simplifyPath("/a/../b"));
        assertEquals("/", IOUtil.simplifyPath("/.."));
        assertEquals("../b", IOUtil.simplifyPath("a/../../b"));
        assertEquals("//host/share", IOUtil.simplifyPath("//host/share/../.."));
    }

    // =================================================================== D4

    @Test
    public void testReadAllToStringFromReaderMatchesTheCharArrayForm() throws Exception {
        for (final int len : new int[] { 0, 1, 10, 8191, 8192, 8193, 40000 }) {
            final StringBuilder sb = new StringBuilder(len);

            for (int i = 0; i < len; i++) {
                sb.append((char) ('a' + (i % 26)));
            }

            final String expected = sb.toString();
            assertEquals(expected, IOUtil.readAllToString(new StringReader(expected)), "length " + len);
            assertEquals(expected, String.valueOf(IOUtil.readAllChars(new StringReader(expected))), "length " + len);
        }
    }

    @Test
    public void testReadAllToStringFromReaderKeepsSurrogatePairsIntact() throws Exception {
        final StringBuilder sb = new StringBuilder();

        // Long enough to straddle the internal buffer, so a pair can land on a buffer boundary.
        for (int i = 0; i < 5000; i++) {
            sb.append("ab\uD83D\uDE00");
        }

        final String expected = sb.toString();
        final String actual = IOUtil.readAllToString(new StringReader(expected));

        assertEquals(expected, actual);
        assertEquals(expected.codePointCount(0, expected.length()), actual.codePointCount(0, actual.length()));
    }

    @Test
    public void testReadAllToStringFromReaderRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((java.io.Reader) null));
    }

    @Test
    public void testReadAllToStringFromReaderDoesNotCloseTheReader() throws Exception {
        final StringReader reader = new StringReader("abc");
        assertEquals("abc", IOUtil.readAllToString(reader));

        // A closed StringReader throws from read(); reaching EOF instead proves it is still open.
        assertEquals("", IOUtil.readAllToString(reader));
    }

    @Test
    public void testReadAllToStringFromFileIsUnaffected() throws Exception {
        final File f = newFile("d4-file.txt", "line1\nline2\n");
        assertEquals("line1\nline2\n", IOUtil.readAllToString(f));
    }

    // =================================================================== J2

    @Test
    public void testNullInputToTheFileTargetingWritersIsDocumentedAndPinned() throws Exception {
        // Emptiness truncates ...
        final File empty = newFile("j2-empty.txt", "SEED");
        IOUtil.write((CharSequence) null, empty);
        assertEquals(0, empty.length());

        final File emptyLines = newFile("j2-empty-lines.txt", "SEED");
        IOUtil.writeLines((Iterable<?>) null, emptyLines);
        assertEquals(0, emptyLines.length());

        // ... but an Object is rendered, and null renders as the four-character text "null".
        final File rendered = newFile("j2-rendered.txt", "SEED");
        IOUtil.writeLine(null, rendered);
        assertEquals("null\n", IOUtil.readAllToString(rendered));

        final File appended = new File(tempDir, "j2-appended.txt");
        IOUtil.appendLine(null, appended);
        assertEquals("null\n", IOUtil.readAllToString(appended));
    }

    @Test
    public void testWriteNullCharSequenceToAStreamStillWritesTheTextNull() throws Exception {
        // The OutputStream/Writer side keeps Appendable semantics; only the File side means "empty".
        final java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        IOUtil.write((CharSequence) null, StandardCharsets.UTF_8, os);
        assertEquals("null", os.toString(StandardCharsets.UTF_8));

        final java.io.StringWriter sw = new java.io.StringWriter();
        IOUtil.write((CharSequence) null, sw);
        assertEquals("null", sw.toString());
    }

    // =================================================================== sanity

    @Test
    public void testPooledWriterIsStillRecycledAndReusable() throws Exception {
        // drainPooledWriter(..) + releasePooledWriter(..) must leave the pool in a usable state: a second
        // call has to behave exactly like the first, whichever flush mode was used.
        for (int i = 0; i < 8; i++) {
            final CountingWriter w = new CountingWriter();
            IOUtil.writeLines(Arrays.asList("x" + i, "y" + i), w, i % 2 == 0);
            assertEquals("x" + i + "\ny" + i + "\n", w.written.toString(), "iteration " + i);
        }

        final File f = new File(tempDir, "pool-sanity.txt");
        IOUtil.writeLines(Arrays.asList("p", "q"), f);
        assertEquals("p\nq\n", IOUtil.readAllToString(f));

        final CountingWriter first = new CountingWriter();
        final CountingWriter second = new CountingWriter();
        IOUtil.writeLines(Arrays.asList("1"), first, false);
        IOUtil.writeLines(Arrays.asList("2"), second, false);
        assertNotSame(first.written.toString(), second.written.toString());
        assertEquals("1\n", first.written.toString());
        assertEquals("2\n", second.written.toString());
    }

    // ------------------------------------------------------------------------------------------------
    // G15-001: unzip takes its pooled buffer immediately above the try whose finally recycles it
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testUnzipDoesNotKeepItsPooledBufferWhenTheSetupFails() throws Exception {
        final File source = newFile("pool-src.txt", "content");
        final File archive = new File(tempDir, "pool.zip");
        IOUtil.zip(source, archive);

        // getCanonicalFile() is the first throwing statement unzip reaches after it has created the target
        // directory, and createDestDirectory(..) never canonicalizes - so the failure point is deterministic.
        final File failingTarget = new File(new File(tempDir, "pool-out").getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public File getCanonicalFile() throws IOException {
                throw new IOException("synthetic canonicalization failure");
            }
        };

        final Queue<byte[]> pool = byteArrayBufferPool();
        final List<byte[]> parked = new ArrayList<>();
        byte[] pooled;

        while ((pooled = pool.poll()) != null) {
            parked.add(pooled);
        }

        try {
            Objectory.recycle(Objectory.createByteArrayBuffer());
            assertEquals(1, pool.size());

            assertThrows(IOException.class, () -> IOUtil.unzip(archive, failingTarget, null));

            assertEquals(1, pool.size(), "a failed unzip must not keep the pooled buffer it took");
        } finally {
            pool.clear();
            pool.addAll(parked);
        }
    }

    @SuppressWarnings("unchecked")
    private static Queue<byte[]> byteArrayBufferPool() throws ReflectiveOperationException {
        final Field field = Objectory.class.getDeclaredField("byteArrayBufferPool");
        field.setAccessible(true);

        return (Queue<byte[]>) field.get(null);
    }
}
