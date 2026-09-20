package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Regression tests for the eighth independent review pass over {@code IOUtil} (2026-08-31, run b).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>B1/D2/J1</b> - the line-slicing {@code Reader} overloads
 *       ({@code readFirstLine}, {@code readLine(Reader, int)}, {@code readLines(Reader, int, int)}) no longer
 *       read past the line they return. They used to wrap a non-{@code BufferedReader} source in a pooled
 *       buffer and discard it, so asking for one line handed back that line and left the caller with an
 *       exhausted reader - while the class contract recommended a caller-owned {@code Reader} as the safe way
 *       to read incrementally.</li>
 *   <li><b>D1</b> - {@code listFiles}/{@code listDirectories}/{@code walk} reject a path that exists but is
 *       not a directory instead of answering with an empty result, which was indistinguishable from an empty
 *       directory. A {@code null} or non-existent path still answers empty.</li>
 *   <li><b>B2/J3</b> - a {@code zip}/{@code unzip} round trip carries timestamps only to whole-second
 *       resolution, which is now what the javadoc says; {@code copyFile} is exact.</li>
 *   <li><b>B3</b> - a {@code null} {@code CopyOption[]} is an {@code IllegalArgumentException} naming the
 *       argument, not a {@code NullPointerException} from inside {@code Files.copy}.</li>
 *   <li><b>B4</b> - {@code LineIterationOptions.charset()} never answers {@code null}.</li>
 *   <li><b>B5</b> - {@code getHostName()} uses a resolution that has already finished even while the
 *       retry backoff from an earlier timeout is still running.</li>
 *   <li><b>B6</b> - "does not exist" and "exists but cannot be read" are separate messages.</li>
 *   <li><b>D4</b> - {@code unzip} logs and skips a timestamp the filesystem refuses instead of aborting an
 *       extraction whose content is already on disk.</li>
 *   <li><b>D5</b> - the GZIP factories validate {@code bufferSize} with this class's own message.</li>
 *   <li><b>O4</b> - {@code OS_VERSION}/{@code OS_ARCH} are never {@code null}, matching {@code OS_NAME}.</li>
 * </ul>
 */
public class IOUtilLineSlicingReaderTest extends TestBase {

    @TempDir
    File tempDir;

    private static final String FIVE_LINES = "l1\nl2\nl3\nl4\nl5\n";

    /** A reader that deliberately does not support {@code mark()}, like {@code InputStreamReader}. */
    private static Reader nonMarkable(final String s) {
        return new InputStreamReader(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    private static String drain(final Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int c;

        while ((c = reader.read()) != -1) {
            sb.append((char) c);
        }

        return sb.toString();
    }

    /** What {@code java.io.BufferedReader.readLine()} makes of {@code s}: the semantics being matched. */
    private static List<String> referenceLines(final String s) throws IOException {
        final List<String> lines = new ArrayList<>();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new StringReader(s))) {
            String line;

            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    // ------------------------------------------------------------------------------------------------------
    // B1 - the bounded Reader overloads leave the reader exactly where the returned content ends.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB1_lineSlicersLeaveTheReaderExactlyAfterTheLastLineReturned() throws IOException {
        Reader r = new StringReader(FIVE_LINES);
        assertEquals(List.of("l1"), IOUtil.readLines(r, 0, 1));
        assertEquals("l2\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals("l1", IOUtil.readFirstLine(r));
        assertEquals("l2\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals("l3", IOUtil.readLine(r, 2));
        assertEquals("l4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals(List.of("l2", "l3"), IOUtil.readLines(r, 1, 2));
        assertEquals("l4\nl5\n", drain(r));

        // count == 0 asks for nothing and leaves the reader alone: the offset is NOT consumed, matching
        // readChars(reader, offset, 0) and the write(.., offset, 0, ..) forms (10th pass, 2026-09-02).
        r = new StringReader(FIVE_LINES);
        assertEquals(List.of(), IOUtil.readLines(r, 2, 0));
        assertEquals(FIVE_LINES, drain(r));

        // offset/count both 0: nothing read at all.
        r = new StringReader(FIVE_LINES);
        assertEquals(List.of(), IOUtil.readLines(r, 0, 0));
        assertEquals(FIVE_LINES, drain(r));
    }

    @Test
    public void testB1_lineSlicersAreExactOnAReaderThatDoesNotSupportMark() throws IOException {
        Reader r = nonMarkable(FIVE_LINES);
        assertEquals("l1", IOUtil.readFirstLine(r));
        assertEquals("l2\nl3\nl4\nl5\n", drain(r));

        // CRLF: the '\n' of the pair is consumed and nothing else is.
        r = nonMarkable("a\r\nb\r\nc");
        assertEquals("a", IOUtil.readFirstLine(r));
        assertEquals("b\r\nc", drain(r));

        r = nonMarkable("a\r\nb\r\nc");
        assertEquals(List.of("a", "b"), IOUtil.readLines(r, 0, 2));
        assertEquals("c", drain(r));
    }

    @Test
    public void testB1_aLoneCarriageReturnIsPutBackWhenTheReaderSupportsMark() throws IOException {
        // StringReader supports mark(), so the look-ahead is undone exactly.
        Reader r = new StringReader("a\rb\rc");
        assertEquals("a", IOUtil.readFirstLine(r));
        assertEquals("b\rc", drain(r));

        r = new StringReader("a\rb\rc");
        assertEquals(List.of("a", "b"), IOUtil.readLines(r, 0, 2));
        assertEquals("c", drain(r));
    }

    @Test
    public void testB1_aLoneCarriageReturnCostsOneCharacterOnAReaderWithoutMark() throws IOException {
        // The documented residual: without mark() the character looked at after a lone '\r' cannot be put
        // back, so it is lost when it is the last line the call reads. Pinned so the contract stays honest.
        final Reader r = nonMarkable("a\rb\rc");
        assertEquals("a", IOUtil.readFirstLine(r));
        assertEquals("\rc", drain(r));

        // Within one call the carry is kept, so the LINES are still exactly right.
        assertEquals(List.of("a", "b", "c"), IOUtil.readLines(nonMarkable("a\rb\rc"), 0, Integer.MAX_VALUE));
    }

    @Test
    public void testB1_aBufferedReaderSourceIsReadThroughDirectlyAndStaysExact() throws IOException {
        final java.io.BufferedReader br = new java.io.BufferedReader(new StringReader(FIVE_LINES));

        assertEquals(List.of("l1"), IOUtil.readLines(br, 0, 1));
        // The caller owns the buffer, so what it read ahead is still theirs.
        assertEquals("l2", br.readLine());
        assertEquals("l3", IOUtil.readFirstLine(br));
        assertEquals("l4", br.readLine());
    }

    @Test
    public void testB1_theCharacterSlicersWereAlreadyExactAndStillAre() throws IOException {
        Reader r = new StringReader(FIVE_LINES);
        assertEquals("l1\nl2", new String(IOUtil.readChars(r, 0, 5)));
        assertEquals("\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals("l1\nl2", IOUtil.readToString(r, 0, 5));
        assertEquals("\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        final char[] buf = new char[5];
        assertEquals(5, IOUtil.read(r, buf, 0, 5));
        assertEquals("\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals(3, IOUtil.skip(r, 3));
        assertEquals("l2\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        IOUtil.skipFully(r, 3);
        assertEquals("l2\nl3\nl4\nl5\n", drain(r));

        r = new StringReader(FIVE_LINES);
        final StringWriter w = new StringWriter();
        assertEquals(5, IOUtil.write(r, 0, 5, w));
        assertEquals("l1\nl2", w.toString());
        assertEquals("\nl3\nl4\nl5\n", drain(r));
    }

    @Test
    public void testB1_theReadAllOverloadsStillConsumeToEndOfInput() throws IOException {
        Reader r = new StringReader(FIVE_LINES);
        assertEquals(List.of("l1", "l2", "l3", "l4", "l5"), IOUtil.readAllLines(r));
        assertEquals("", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals("l5", IOUtil.readLastLine(r));
        assertEquals("", drain(r));

        r = new StringReader(FIVE_LINES);
        assertEquals(FIVE_LINES, IOUtil.readAllToString(r));
        assertEquals("", drain(r));
    }

    @Test
    public void testB1_theConsumingOverloadsStayConsuming() throws IOException {
        // forEachLine is a "process the whole source" operation, like readAllLines, and is documented as such
        // even when a count stops it early. Pinned so the documented split stays a deliberate one.
        Reader r = new StringReader(FIVE_LINES);
        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(r, 0, 1, seen::add);
        assertEquals(List.of("l1"), seen);
        assertEquals("", drain(r), "forEachLine is documented as consuming the reader");

        r = new StringReader("a\nb\nc\n");
        final Reader other = new StringReader("z\nb\nc\n");
        assertFalse(IOUtil.contentEqualsIgnoreEOL(r, other));
        assertEquals("", drain(r), "contentEqualsIgnoreEOL is documented as leaving an unspecified position");
    }

    /** The exact reader must produce exactly what {@code BufferedReader.readLine()} would, terminator for terminator. */
    @Test
    public void testB1_lineSplittingMatchesBufferedReaderForEveryTerminatorShape() throws IOException {
        final String[] inputs = { "", "a", "a\n", "a\r", "a\r\n", "\n", "\r", "\r\n", "\n\n", "\r\r", "\r\n\r\n", "a\nb", "a\rb", "a\r\nb", "a\n\nb", "a\r\rb",
                "a\r\n\r\nb", "a\nb\r\nc\rd", "\na", "\ra", "\r\na", "a\rb\n", "abc\r\n\ndef\r", "x\r\n\r\n\ny" };

        for (final String input : inputs) {
            final List<String> expected = referenceLines(input);
            final String label = "input=" + input.replace("\r", "\\r").replace("\n", "\\n");

            assertEquals(expected, IOUtil.readLines(new StringReader(input), 0, Integer.MAX_VALUE), label + " (markable)");
            assertEquals(expected, IOUtil.readLines(nonMarkable(input), 0, Integer.MAX_VALUE), label + " (non-markable)");

            assertEquals(expected.isEmpty() ? null : expected.get(0), IOUtil.readFirstLine(new StringReader(input)), label + " (readFirstLine)");

            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i), IOUtil.readLine(new StringReader(input), i), label + " (readLine " + i + ")");
                assertEquals(expected.get(i), IOUtil.readLine(nonMarkable(input), i), label + " (readLine " + i + ", non-markable)");
            }

            assertNull(IOUtil.readLine(new StringReader(input), expected.size()), label + " (past the end)");
        }
    }

    /** The {@code File} and {@code InputStream} entry points own their reader, and must answer identically. */
    @Test
    public void testB1_fileAndStreamEntryPointsAgreeWithTheReaderOverloads() throws IOException {
        final String content = "a\r\nb\nc\rd";
        final File file = new File(tempDir, "lines.txt");
        IOUtil.write(content, file);

        final List<String> expected = referenceLines(content);
        assertEquals(expected, IOUtil.readAllLines(file));
        assertEquals(expected, IOUtil.readLines(file, 0, Integer.MAX_VALUE));
        assertEquals(expected.subList(1, 3), IOUtil.readLines(file, 1, 2));
        assertEquals(expected.get(0), IOUtil.readFirstLine(file));
        assertEquals(expected.get(2), IOUtil.readLine(file, 2));
        assertNull(IOUtil.readLine(file, expected.size()));

        try (InputStream is = IOUtil.newFileInputStream(file)) {
            assertEquals(expected.subList(0, 2), IOUtil.readLines(is, 0, 2));
        }
    }

    @Test
    public void testB1_emptySourcesBehaveTheSameAsBefore() throws IOException {
        assertEquals(List.of(), IOUtil.readLines(new StringReader(""), 0, 5));
        assertNull(IOUtil.readFirstLine(new StringReader("")));
        assertNull(IOUtil.readLine(new StringReader(""), 0));
        assertNull(IOUtil.readLine(new StringReader("only"), 1));
        assertEquals(List.of(), IOUtil.readLines(new StringReader("abc"), 5, 5));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines((Reader) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLine((Reader) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines(new StringReader("a"), -1, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines(new StringReader("a"), 0, -1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLine(new StringReader("a"), -1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines((InputStream) null, 0, 1));
    }

    // ------------------------------------------------------------------------------------------------------
    // D1 - the listing family rejects a path that exists but is not a directory.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD1_aRegularFileIsARejectedArgumentForTheListingFamily() throws IOException {
        final File file = new File(tempDir, "a.txt");
        IOUtil.write("x", file);

        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file, true, false));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file, true, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file, false, (parent, f) -> true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listDirectories(file));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.listDirectories(file, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(file).toList());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(file, true, false).toList());
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(file, true, true).toList());

        // The message names the argument, not the platform's open failure.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(file));
        assertTrue(e.getMessage().contains("is not a directory"), e.getMessage());
        assertTrue(e.getMessage().contains(file.getName()), e.getMessage());
    }

    @Test
    public void testD1_nullAndMissingPathsStillAnswerEmpty() {
        final File missing = new File(tempDir, "no-such-dir");

        assertEquals(List.of(), IOUtil.listFiles(null));
        assertEquals(List.of(), IOUtil.listFiles(null, true, false));
        assertEquals(List.of(), IOUtil.listFiles(missing));
        assertEquals(List.of(), IOUtil.listFiles(missing, true, true));
        assertEquals(List.of(), IOUtil.listDirectories(null));
        assertEquals(List.of(), IOUtil.listDirectories(missing, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null));
        assertEquals(List.of(), IOUtil.walk(missing, true, false).toList());
    }

    @Test
    public void testD1_realDirectoriesAreUnaffected() throws IOException {
        final File dir = new File(tempDir, "tree");
        final File sub = new File(dir, "sub");
        assertTrue(sub.mkdirs());
        IOUtil.write("x", new File(dir, "top.txt"));
        IOUtil.write("y", new File(sub, "deep.txt"));

        assertEquals(2, IOUtil.listFiles(dir).size());
        assertEquals(3, IOUtil.listFiles(dir, true, false).size());
        assertEquals(2, IOUtil.listFiles(dir, true, true).size());
        assertEquals(1, IOUtil.listDirectories(dir).size());
        assertEquals(3, IOUtil.walk(dir, true, false).toList().size());
        assertEquals(0, IOUtil.listFiles(sub, false, (parent, f) -> false).size());
    }

    // ------------------------------------------------------------------------------------------------------
    // B2/J3 - archive timestamps are whole-second; copyFile is exact.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB2_anArchiveRoundTripCarriesTimestampsOnlyToWholeSeconds() throws IOException {
        final File src = new File(tempDir, "stamped.txt");
        IOUtil.write("hello", src);
        // Deliberately not aligned to a second, so a claim of exactness would fail here.
        final FileTime stamp = FileTime.fromMillis(1_600_000_001_234L);
        Files.setLastModifiedTime(src.toPath(), stamp);

        final File copy = new File(tempDir, "copy.txt");
        IOUtil.copyFile(src, copy);
        assertEquals(stamp.toMillis(), Files.getLastModifiedTime(copy.toPath()).toMillis(), "copyFile is exact");

        final File zip = new File(tempDir, "stamped.zip");
        IOUtil.zip(src, zip);
        final File out = new File(tempDir, "unzipped");
        IOUtil.unzip(zip, out);

        final long roundTripped = Files.getLastModifiedTime(new File(out, "stamped.txt").toPath()).toMillis();
        assertEquals(1_600_000_001_000L, roundTripped, "the ZIP extended-timestamp field records whole seconds only");
        assertEquals(stamp.toMillis() / 1000L, roundTripped / 1000L, "the second itself must survive");
    }

    // ------------------------------------------------------------------------------------------------------
    // D4 - unzip does not abort over a timestamp it cannot apply.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD4_unzipExtractsAnEntryWhoseRecordedTimeIsExtreme() throws IOException {
        final File zip = new File(tempDir, "odd.zip");

        try (ZipOutputStream zos = IOUtil.newZipOutputStream(IOUtil.newFileOutputStream(zip))) {
            final ZipEntry entry = new ZipEntry("x.txt");
            entry.setLastModifiedTime(FileTime.fromMillis(-5_000_000_000L)); // long before the epoch
            zos.putNextEntry(entry);
            zos.write("q".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        final File out = new File(tempDir, "oddOut");
        IOUtil.unzip(zip, out);

        // The content is what matters, and it must be there whatever the filesystem made of the stamp.
        assertEquals("q", IOUtil.readAllToString(new File(out, "x.txt")));
    }

    // ------------------------------------------------------------------------------------------------------
    // B3 - a null CopyOption[] names the argument instead of escaping as an NPE.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB3_aNullCopyOptionArrayIsARejectedArgument() throws IOException {
        final File src = new File(tempDir, "src.txt");
        IOUtil.write("x", src);
        final File dst = new File(tempDir, "dst.txt");
        final Path srcPath = src.toPath();
        final Path dstPath = new File(tempDir, "dst2.txt").toPath();
        final File destDir = new File(tempDir, "destDir");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, dst, (CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, dst, true, (CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy(srcPath, dstPath, (CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy(new ByteArrayInputStream(new byte[1]), dstPath, (CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.move(srcPath, dstPath, (CopyOption[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(src, destDir, (CopyOption[]) null));

        // Nothing was created by any of the rejected calls.
        assertFalse(dst.exists());
        assertFalse(dstPath.toFile().exists());
        assertFalse(destDir.exists());

        // The ordinary no-options form still works.
        IOUtil.copyFile(src, dst);
        assertEquals("x", IOUtil.readAllToString(dst));
        IOUtil.moveToDirectory(dst, destDir);
        assertEquals("x", IOUtil.readAllToString(new File(destDir, "dst.txt")));
    }

    // ------------------------------------------------------------------------------------------------------
    // B4 - LineIterationOptions is a value type and never answers a null charset.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB4_lineIterationOptionsNormalisesANullCharset() throws IOException {
        assertEquals(StandardCharsets.UTF_8, IOUtil.LineIterationOptions.builder().build().charset());
        assertEquals(StandardCharsets.UTF_8, IOUtil.LineIterationOptions.builder().charset(null).build().charset());
        assertEquals(StandardCharsets.ISO_8859_1, IOUtil.LineIterationOptions.builder().charset(StandardCharsets.ISO_8859_1).build().charset());

        // Rebuilding from a read-back charset must not blow up, which is what the null used to risk.
        final IOUtil.LineIterationOptions opts = IOUtil.LineIterationOptions.builder().charset(null).offset(1).count(1).build();
        assertEquals(StandardCharsets.UTF_8, IOUtil.LineIterationOptions.builder().charset(opts.charset()).build().charset());

        final File file = new File(tempDir, "opts.txt");
        IOUtil.writeLines(List.of("a", "b", "c"), file);
        final List<String> seen = new ArrayList<>();
        IOUtil.forEachLine(file, opts, seen::add);
        assertEquals(List.of("b"), seen);
    }

    // ------------------------------------------------------------------------------------------------------
    // B5 - a completed host-name resolution is used even during the retry backoff.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB5_aFinishedHostNameResolutionIsHandedOverDuringTheBackoff() throws Exception {
        final Method submit = IOUtil.class.getDeclaredMethod("submitHostNameResolutionIfAbsent");
        submit.setAccessible(true);
        final Field futureField = IOUtil.class.getDeclaredField("hostNameFuture");
        futureField.setAccessible(true);
        final Field retryField = IOUtil.class.getDeclaredField("hostNameRetryAtMillis");
        retryField.setAccessible(true);

        final Object savedFuture = futureField.get(null);
        final long savedRetryAt = retryField.getLong(null);

        try {
            // An attempt that timed out, then finished: the answer is right there, so it must be used.
            final Future<String> done = CompletableFuture.completedFuture("resolved-host");
            futureField.set(null, done);
            retryField.setLong(null, System.currentTimeMillis() + 600_000L);
            assertSame(done, submit.invoke(null), "a completed Future must not be withheld by the backoff");

            // An attempt that is still hanging is exactly what the backoff exists for.
            futureField.set(null, new CompletableFuture<>());
            retryField.setLong(null, System.currentTimeMillis() + 600_000L);
            assertNull(submit.invoke(null), "an unfinished Future stays behind the backoff");
        } finally {
            futureField.set(null, savedFuture);
            retryField.setLong(null, savedRetryAt);
        }

        // The public entry point still answers.
        assertNotNull(IOUtil.getHostName());
    }

    // ------------------------------------------------------------------------------------------------------
    // B6 - "absent" and "unreadable" are different messages.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB6_anAbsentFileIsNotReportedAsPossiblyUnreadable() {
        final File missing = new File(tempDir, "missing.txt");
        final File dst = new File(tempDir, "dst.txt");

        final FileNotFoundException direct = assertThrows(FileNotFoundException.class, () -> IOUtil.checkFileExists(missing));
        assertTrue(direct.getMessage().contains("does not exist"), direct.getMessage());
        assertFalse(direct.getMessage().contains("is not readable"), direct.getMessage());

        // Reached through a public entry point that validates rather than letting the open fail.
        final FileNotFoundException viaCopy = assertThrows(FileNotFoundException.class, () -> IOUtil.copyFile(missing, dst));
        assertTrue(viaCopy.getMessage().contains("does not exist"), viaCopy.getMessage());
        assertFalse(viaCopy.getMessage().contains("is not readable"), viaCopy.getMessage());

        final UncheckedIOException viaSizeOf = assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOf(missing));
        assertTrue(viaSizeOf.getMessage().contains("does not exist"), viaSizeOf.getMessage());
        assertFalse(viaSizeOf.getMessage().contains("is not readable"), viaSizeOf.getMessage());
    }

    @Test
    public void testB6_anUnreadableFileSaysSo() {
        // A File subclass rather than a real permission change: Windows cannot make a file unreadable to its
        // own owner through File.setReadable(..), so the real-filesystem form of this test can only ever be
        // skipped there. Overriding the two predicates checkFileExists consults exercises the same branch on
        // every platform.
        final File pretendUnreadable = new File(tempDir, "pretend.txt") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public boolean canRead() {
                return false;
            }
        };

        final FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> IOUtil.checkFileExists(pretendUnreadable));
        assertTrue(e.getMessage().contains("exists but cannot be read"), e.getMessage());
        assertFalse(e.getMessage().contains("does not exist"), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------
    // D5 - the GZIP factories validate bufferSize the way the rest of the class validates a size.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD5_theGzipFactoriesRejectANonPositiveBufferSize() throws IOException {
        final byte[] gzipped;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (OutputStream gz = IOUtil.newGZIPOutputStream(bos)) {
                gz.write("hi".getBytes(StandardCharsets.UTF_8));
            }

            gzipped = bos.toByteArray();
        }

        for (final int bad : new int[] { 0, -1, Integer.MIN_VALUE }) {
            final IllegalArgumentException in = assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.newGZIPInputStream(new ByteArrayInputStream(gzipped), bad));
            assertTrue(in.getMessage().contains("bufferSize"), in.getMessage());

            final IllegalArgumentException out = assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.newGZIPOutputStream(new ByteArrayOutputStream(), bad));
            assertTrue(out.getMessage().contains("bufferSize"), out.getMessage());
        }

        // A positive size is still accepted, and the null-stream check still comes first.
        try (InputStream is = IOUtil.newGZIPInputStream(new ByteArrayInputStream(gzipped), 512)) {
            assertEquals("hi", IOUtil.readAllToString(is));
        }

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newGZIPInputStream(null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newGZIPOutputStream(null, 0));
    }

    // ------------------------------------------------------------------------------------------------------
    // O4 - the OS constants are a consistent family.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testO4_theOsConstantsAreNeverNull() {
        assertNotNull(IOUtil.OS_NAME);
        assertNotNull(IOUtil.OS_VERSION);
        assertNotNull(IOUtil.OS_ARCH);

        final String country = System.getProperty("user.country");

        if (country != null) {
            assertEquals(country, IOUtil.USER_COUNTRY);
        }
    }
}
