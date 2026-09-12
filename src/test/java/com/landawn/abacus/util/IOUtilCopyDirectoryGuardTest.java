package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the fifth independent review pass over {@code IOUtil} (2026-08-30d).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>B1</b> - {@code copyDirectory(srcDir, destDir)} rejects a destination that is the source or lies
 *       inside it, <i>before</i> anything is copied. It used to copy part of the tree into the source itself
 *       (leaving {@code "Copy of x.txt"} entries behind) and only then fail, with a message that named the
 *       recursive call's arguments rather than the caller's.</li>
 *   <li><b>B2</b> - {@code moveToDirectory} rejects the same shape instead of failing with a
 *       platform-specific {@code FileSystemException} after creating the destination inside the source.</li>
 *   <li><b>D1/D3</b> - a {@code null} caller-supplied stream/reader/writer is an
 *       {@code IllegalArgumentException} everywhere, not a {@code NullPointerException} from {@code java.io}
 *       (and {@code newBufferedInputStream}/{@code newBufferedOutputStream} no longer hand back a wrapper
 *       that fails later with a misleading "Stream closed").</li>
 *   <li><b>D2</b> - a {@code null} {@code Path} is rejected the same way a {@code null} {@code File} is.</li>
 *   <li><b>D4</b> - {@code forEachLine((Collection&lt;File&gt;) null, ..)} is rejected instead of being a
 *       silent no-op, matching {@code zip}/{@code merge}.</li>
 *   <li><b>D5</b> - {@code toFiles((URL[]) null)} is rejected like its three sibling conversions.</li>
 *   <li><b>D6</b> - a {@code null}/empty charset <i>name</i> means UTF-8, as a {@code null} {@code Charset}
 *       already did.</li>
 *   <li><b>O2</b> - a signed percent escape such as {@code %-1} is malformed and is kept literally.</li>
 * </ul>
 */
public class IOUtilCopyDirectoryGuardTest extends TestBase {

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

    /** Sorted, '/'-normalised relative listing of everything under {@code root}. */
    private static List<String> tree(final File rootDir) {
        final List<String> out = new ArrayList<>();
        collect(rootDir, rootDir, out);
        Collections.sort(out);
        return out;
    }

    private static void collect(final File base, final File cur, final List<String> out) {
        final File[] children = cur.listFiles();

        if (children == null) {
            return;
        }

        for (final File child : children) {
            out.add(base.toPath().relativize(child.toPath()).toString().replace('\\', '/') + (child.isDirectory() ? "/" : ""));

            if (child.isDirectory()) {
                collect(base, child, out);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - copyDirectory: destination inside or equal to the source
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCopyDirectory_RejectsTheSourceAsItsOwnDestinationWithoutCopyingAnything() throws IOException {
        final File src = dir("b1-same");
        file(src, "a.txt", "a");
        file(src, "b.txt", "b");
        final File sub = new File(src, "sub");
        assertTrue(sub.mkdirs());
        file(sub, "c.txt", "c");

        final List<String> before = tree(src);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(src, src));
        assertTrue(e.getMessage().contains("is in or same as the source directory"), e.getMessage());

        // Nothing was copied: no "Copy of .." entries were left inside the source.
        assertEquals(before, tree(src));
    }

    @Test
    public void testCopyDirectory_RejectsADestinationInsideTheSourceWithoutCopyingAnything() throws IOException {
        final File src = dir("b1-inside");
        file(src, "a.txt", "a");
        file(src, "b.txt", "b");
        final File nested = new File(src, "d");
        assertTrue(nested.mkdirs());
        file(nested, "d1.txt", "d1");

        final List<String> before = tree(src);
        final File dest = new File(src, "inside");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyDirectory(src, dest));
        // The message names the CALLER's arguments, not a recursive call's.
        assertTrue(e.getMessage().contains(dest.getCanonicalPath()), e.getMessage());
        assertTrue(e.getMessage().contains(src.getCanonicalPath()), e.getMessage());

        assertFalse(dest.exists(), "the rejected destination must not have been created");
        assertEquals(before, tree(src));
    }

    @Test
    public void testCopyDirectory_StillCopiesIntoAnAncestorOfTheSource() throws IOException {
        final File outer = dir("b1-outer");
        final File inner = new File(outer, "inner");
        assertTrue(inner.mkdirs());
        file(inner, "x.txt", "x");

        // A source INSIDE the destination is the legal direction and must keep working.
        IOUtil.copyDirectory(inner, outer);

        assertEquals(Arrays.asList("inner/", "inner/x.txt", "x.txt"), tree(outer));
    }

    @Test
    public void testCopyDirectory_RejectsASiblingWhoseNameMerelySharesThePrefix() throws IOException {
        // "b1-pfx2" starts with "b1-pfx" as a STRING but is not inside it, so it must be accepted.
        final File src = dir("b1-pfx");
        file(src, "a.txt", "a");
        final File dest = dir("b1-pfx2");

        IOUtil.copyDirectory(src, dest);

        assertEquals(Collections.singletonList("a.txt"), tree(dest));
    }

    // ------------------------------------------------------------------------------------------------
    // B2 - moveToDirectory: destination inside or equal to the source
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testMoveToDirectory_RejectsADirectoryMovedIntoItself() throws IOException {
        final File src = dir("b2-same");
        file(src, "a.txt", "a");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(src, src));
        assertTrue(e.getMessage().contains("is in or same as the source directory"), e.getMessage());

        assertEquals(Collections.singletonList("a.txt"), tree(src));
    }

    @Test
    public void testMoveToDirectory_RejectsADestinationInsideTheSourceAndCreatesNothing() throws IOException {
        final File src = dir("b2-inside");
        file(src, "a.txt", "a");
        final File dest = new File(src, "inside");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.moveToDirectory(src, dest));

        assertFalse(dest.exists(), "the rejected destination must not have been created");
        assertEquals(Collections.singletonList("a.txt"), tree(src));
    }

    @Test
    public void testMoveToDirectory_StillMovesAPlainFileIntoItsOwnParent() throws IOException {
        // A file (not a directory) into its own parent is a no-op move, not an error.
        final File d = dir("b2-file");
        final File f = file(d, "f.txt", "a");

        IOUtil.moveToDirectory(f, d);

        assertEquals(Collections.singletonList("f.txt"), tree(d));
        assertEquals("a", IOUtil.readAllToString(f));
    }

    // ------------------------------------------------------------------------------------------------
    // D1 / D3 - a null stream argument is IllegalArgumentException, never NullPointerException
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNullSourceStreamIsRejectedByTheReadFamily() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readBytes((InputStream) null, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllChars((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllChars((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readChars((Reader) null, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readToString((Reader) null, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLines((Reader) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readFirstLine((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLastLine((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.readLine((Reader) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((InputStream) null, new byte[4]));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((Reader) null, new char[4]));
        // Even the zero-length shortcut, which never touches the stream, still validates it.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((InputStream) null, new byte[4], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read((Reader) null, new char[4], 0, 0));
    }

    @Test
    public void testNullTargetStreamIsRejectedByTheWriteFamily() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", StandardCharsets.UTF_8, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[] { 1 }, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[] { 'a' }, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[] { 'a' }, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(1, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(1L, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(true, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write('a', (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((Object) "x", (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Arrays.asList("x"), (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Arrays.asList("x").iterator(), (Writer) null));
    }

    @Test
    public void testAnEmptyWriteAlsoRejectsANullTargetStream() {
        // These used to return silently because the empty-input shortcut ran before anything touched the target,
        // so a missing argument was invisible - while the File-targeting twin already reported it.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[0], (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[0], (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[0], (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[4], 0, 0, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[4], 0, 0, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Collections.emptyList(), (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Collections.emptyList().iterator(), (Writer) null));
    }

    @Test
    public void testNullTargetStreamIsRejectedBeforeTheSourceFileIsOpened() throws IOException {
        final File src = file(root(), "d1-src.txt", "hello");

        // write(File, .., OutputStream, ..) used to open the source first, so a missing target cost a file handle
        // (and reported FileNotFoundException instead when the source did not exist either).
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(src, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(src, 0, 10, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(src, (OutputStream) null));
    }

    @Test
    public void testNullStreamIsRejectedByTheWrapperFactories() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newInputStreamReader(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newInputStreamReader(null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newOutputStreamWriter(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newOutputStreamWriter(null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newGZIPInputStream(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newGZIPOutputStream(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newZipInputStream(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newZipOutputStream(null));
    }

    @Test
    public void testBufferedWrappersFailAtTheCallRatherThanHandingBackAPoisonedStream() {
        // java.io.BufferedInputStream/BufferedOutputStream do not null-check their delegate, so these two used
        // to succeed and fail later - the first read() reporting "Stream closed" for a stream that was never
        // opened, and the first write() an NPE from inside java.io.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream((OutputStream) null));
    }

    @Test
    public void testTheContentEqualsFamilyStillAnswersNullRatherThanRejectingIt() {
        // The documented exception to the rule: comparisons treat null as a value.
        assertTrue(IOUtil.contentEquals((InputStream) null, (InputStream) null));
        assertTrue(IOUtil.contentEquals((Reader) null, (Reader) null));
        assertTrue(IOUtil.contentEqualsIgnoreEOL((Reader) null, (Reader) null));
        assertFalse(IOUtil.contentEquals(new ByteArrayInputStream(new byte[] { 1 }), (InputStream) null));
        assertFalse(IOUtil.contentEquals(new StringReader("a"), (Reader) null));
        assertFalse(IOUtil.isBufferedReader(null));
        assertFalse(IOUtil.isBufferedWriter(null));
    }

    @Test
    public void testEveryNullStreamPositionReportsIllegalArgumentException() throws Exception {
        // Sweep: for each public static method taking an InputStream/Reader/OutputStream/Writer, pass null in
        // that position with harmless values elsewhere and require IAE (or, for the documented null-tolerant
        // predicates, no exception at all).
        final File existing = file(root(), "d1-scan.txt", "hello\nworld\n");
        final List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (final Method m : IOUtil.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            final Class<?>[] types = m.getParameterTypes();

            for (int i = 0; i < types.length; i++) {
                if (!isStream(types[i])) {
                    continue;
                }

                final Object[] args = new Object[types.length];
                boolean usable = true;

                for (int j = 0; j < types.length && usable; j++) {
                    if (j == i) {
                        continue;
                    }

                    final Object v = sampleFor(types[j], existing);
                    usable = v != NO_SAMPLE;
                    args[j] = v;
                }

                if (!usable) {
                    continue;
                }

                scanned++;

                try {
                    m.invoke(null, args);

                    if (!"isBufferedReader".equals(m.getName()) && !"isBufferedWriter".equals(m.getName())) {
                        offenders.add(m.getName() + Arrays.toString(types) + " arg#" + i + " -> no exception");
                    }
                } catch (final InvocationTargetException e) {
                    if (!(e.getCause() instanceof IllegalArgumentException)) {
                        offenders.add(m.getName() + Arrays.toString(types) + " arg#" + i + " -> " + e.getCause().getClass().getSimpleName());
                    }
                }
            }
        }

        assertTrue(scanned > 90, "the sweep should cover the whole stream-taking surface, covered " + scanned);
        assertTrue(offenders.isEmpty(), "null stream reported as something other than IllegalArgumentException: " + offenders);
    }

    private static final Object NO_SAMPLE = new Object();

    private static boolean isStream(final Class<?> t) {
        return t == InputStream.class || t == Reader.class || t == OutputStream.class || t == Writer.class;
    }

    private static Object sampleFor(final Class<?> t, final File existing) {
        if (t == File.class) {
            return existing;
        } else if (t == Charset.class) {
            return StandardCharsets.UTF_8;
        } else if (t == String.class) {
            return "UTF-8";
        } else if (t == int.class) {
            return 0;
        } else if (t == long.class) {
            return 0L;
        } else if (t == boolean.class) {
            return false;
        } else if (t == byte[].class) {
            return new byte[8];
        } else if (t == char[].class) {
            return new char[8];
        } else if (t == Object.class || t == CharSequence.class) {
            return "x";
        }

        return NO_SAMPLE;
    }

    // ------------------------------------------------------------------------------------------------
    // D2 - a null Path is rejected the way a null File is
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNullPathIsRejectedAsIllegalArgument() throws IOException {
        final File existing = file(root(), "d2.txt", "x");
        final Path p = existing.toPath();
        final OutputStream os = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((Path) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((Path) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy((Path) null, p));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy(p, (Path) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.move((Path) null, p));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.move(p, (Path) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy((Path) null, os));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy(p, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy((InputStream) null, p));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copy(new ByteArrayInputStream(new byte[0]), (Path) null));
    }

    // ------------------------------------------------------------------------------------------------
    // D4 - a null Collection<File> is a bad argument, not "read no files"
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testForEachLine_RejectsANullFileCollectionInsteadOfDoingNothing() {
        final List<String> seen = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine((Collection<File>) null, seen::add));
        assertThrows(IllegalArgumentException.class,
                () -> IOUtil.forEachLine((Collection<File>) null, IOUtil.LineIterationOptions.builder().build(), seen::add));
        assertTrue(seen.isEmpty());
    }

    @Test
    public void testForEachLine_StillAcceptsAnEmptyFileCollectionAndRunsOnComplete() throws IOException {
        final List<String> seen = new ArrayList<>();
        final boolean[] completed = { false };

        IOUtil.forEachLine(Collections.<File> emptyList(), seen::add, () -> completed[0] = true);

        assertTrue(seen.isEmpty());
        assertTrue(completed[0], "onComplete must still run for an empty collection");
    }

    // ------------------------------------------------------------------------------------------------
    // D5 - toFiles(URL[]) rejects null like its three siblings
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testAllFourUrlFileConversionsRejectANullContainer() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles((URL[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles((Collection<URL>) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls((File[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toUrls((Collection<File>) null));
    }

    @Test
    public void testToFiles_StillAcceptsAnEmptyArray() {
        final File[] files = IOUtil.toFiles(new URL[0]);

        assertNotNull(files);
        assertEquals(0, files.length);
    }

    // ------------------------------------------------------------------------------------------------
    // D6 - a null / empty charset NAME means UTF-8, as a null Charset already did
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testANullOrEmptyCharsetNameMeansUtf8() throws IOException {
        final File f = new File(root(), "d6.txt");
        IOUtil.write("café\nnaïve\n", StandardCharsets.UTF_8, f);

        assertEquals("café\nnaïve\n", IOUtil.readAllToString(f, (String) null));
        assertEquals("café\nnaïve\n", IOUtil.readAllToString(f, ""));
        assertEquals(Arrays.asList("café", "naïve"), IOUtil.readAllLines(f, (String) null));
        assertEquals(Arrays.asList("café", "naïve"), IOUtil.readAllLines(f, ""));

        // ...and the same file read through the Charset overload agrees.
        assertEquals(IOUtil.readAllToString(f, (Charset) null), IOUtil.readAllToString(f, (String) null));
    }

    @Test
    public void testAnUnknownCharsetNameIsStillReported() throws IOException {
        final File f = file(root(), "d6-bad.txt", "x");

        assertThrows(java.nio.charset.UnsupportedCharsetException.class, () -> IOUtil.readAllToString(f, "no-such-charset"));
        assertThrows(java.nio.charset.UnsupportedCharsetException.class, () -> IOUtil.readAllLines(f, "no-such-charset"));
    }

    // ------------------------------------------------------------------------------------------------
    // O2 - decodeUrl: a signed escape is malformed, not a byte
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testASignedPercentEscapeIsKeptLiterallyRatherThanDecoded() throws Exception {
        // Integer.parseInt("-1", 16) succeeds, so "%-1" used to decode to the byte 0xFF and surface as U+FFFD.
        assertEquals("a%-1b", IOUtil.toFile(new URL("file:/tmp/a%-1b")).getName());
        assertEquals("a%+1b", IOUtil.toFile(new URL("file:/tmp/a%+1b")).getName());
        // A truncated escape and a bare '%' were already handled this way.
        assertEquals("a%2", IOUtil.toFile(new URL("file:/tmp/a%2")).getName());
        assertEquals("a%", IOUtil.toFile(new URL("file:/tmp/a%")).getName());
        // A well-formed escape still decodes, in either case, and a valid escape next to a malformed one is
        // still decoded rather than being dragged down with it.
        assertEquals("a b", IOUtil.toFile(new URL("file:/tmp/a%20b")).getName());
        assertEquals("aéb", IOUtil.toFile(new URL("file:/tmp/a%c3%a9b")).getName());
        assertEquals("aéb", IOUtil.toFile(new URL("file:/tmp/a%C3%A9b")).getName());
        assertEquals("A%2GB", IOUtil.toFile(new URL("file:/tmp/%41%2G%42")).getName());
    }

    // ------------------------------------------------------------------------------------------------
    // O1 - the pooled buffers are still returned (a smoke test that the reshaped try/finally works)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testContentEquals_StillWorksAfterTheBufferAcquisitionMoved() throws IOException {
        final File a = file(root(), "o1-a.bin", "hello world");
        final File b = file(root(), "o1-b.bin", "hello world");
        final File c = file(root(), "o1-c.bin", "hello WORLD");

        assertTrue(IOUtil.contentEquals(a, b));
        assertFalse(IOUtil.contentEquals(a, c));

        try (InputStream i1 = IOUtil.newFileInputStream(a);
             InputStream i2 = IOUtil.newFileInputStream(b)) {
            assertTrue(IOUtil.contentEquals(i1, i2));
        }

        try (Reader r1 = IOUtil.newFileReader(a);
             Reader r2 = IOUtil.newFileReader(c)) {
            assertFalse(IOUtil.contentEquals(r1, r2));
        }

        try (Reader r1 = IOUtil.newFileReader(a);
             Reader r2 = IOUtil.newFileReader(b)) {
            assertTrue(IOUtil.contentEqualsIgnoreEOL(r1, r2));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Nothing that already worked was broken
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testTheOrdinaryStreamPathsStillWork() throws IOException {
        final StringWriter sw = new StringWriter();
        IOUtil.write("hello", sw);
        IOUtil.writeLine("!", sw);
        IOUtil.writeLines(Arrays.asList("a", "b"), sw);
        assertEquals("hello!\na\nb\n", sw.toString());

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtil.write(new byte[] { 1, 2, 3 }, bos);
        IOUtil.write(new char[] { 'x' }, bos);
        assertEquals(4, bos.toByteArray().length);

        try (InputStream is = new ByteArrayInputStream("l1\nl2\n".getBytes(StandardCharsets.UTF_8))) {
            assertEquals(Arrays.asList("l1", "l2"), IOUtil.readAllLines(is));
        }

        final List<String> seen = new ArrayList<>();
        final File f = file(root(), "ok.txt", "p\nq\n");
        IOUtil.forEachLine(Collections.singletonList(f), seen::add);
        assertEquals(Arrays.asList("p", "q"), seen);

        // copyToDirectory / copyDirectory on unrelated trees are untouched.
        final File src = dir("ok-src");
        file(src, "one.txt", "1");
        final File dest = dir("ok-dest");
        IOUtil.copyDirectory(src, dest);
        assertEquals(Collections.singletonList("one.txt"), tree(dest));

        final File dest2 = dir("ok-dest2");
        assertNotNull(IOUtil.copyToDirectory(src, dest2));
        assertEquals(new TreeSet<>(Arrays.asList("ok-src/", "ok-src/one.txt")), new TreeSet<>(tree(dest2)));
    }

    @Test
    public void testGuardsDoNotChangeTheHappyPathOfTheFactories() throws IOException {
        try (InputStream is = new ByteArrayInputStream("z".getBytes(StandardCharsets.UTF_8))) {
            assertNotNull(IOUtil.newBufferedInputStream(is));
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertNotNull(IOUtil.newBufferedOutputStream(bos));
        assertNotNull(IOUtil.newOutputStreamWriter(bos));
        assertNotNull(IOUtil.newZipOutputStream(bos));

        try (Reader r = new StringReader("z")) {
            assertNotNull(IOUtil.newBufferedReader(r));
        }

        if (IOUtil.newInputStreamReader(new ByteArrayInputStream(new byte[0])) == null) {
            fail("newInputStreamReader must not return null");
        }
    }
}
