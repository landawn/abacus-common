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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the fourth independent review pass over {@code IOUtil} (2026-08-30c).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>B1</b> - a {@code null} path argument is an {@code IllegalArgumentException} in the
 *       {@code write} / {@code append} / factory families too, not the {@code NullPointerException} that
 *       {@code java.io} raises. The 2026-08-30 pass fixed only the {@code read} family.</li>
 *   <li><b>D1</b> - {@code splitByLine(File, int, File, Charset)} accepts a {@code null} charset as UTF-8, like
 *       every other charset-taking method in the class.</li>
 *   <li><b>D2</b> - a rejection names the argument the caller actually passed.</li>
 * </ul>
 */
public class IOUtilNullPathWriteTest extends TestBase {

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

    private File file(final String name, final String content) throws IOException {
        final File f = new File(root(), name);
        IOUtil.write(content, f);
        return f;
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - null File / null file name
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByTheWriteFamily() throws IOException {
        final File real = file("b1-write-src.txt", "hello");
        final OutputStream os = new ByteArrayOutputStream();

        // File as the SOURCE
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((File) null, os));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((File) null, os, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((File) null, 0, 10, os));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((File) null, 0, 10, os, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((File) null, real));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile((File) null, os));

        // File as the OUTPUT
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(real, 0, 10, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write("x", StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[1], (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new byte[1], 0, 1, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[1], (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[1], StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[1], 0, 1, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[1], 0, 1, StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLine("x", StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Collections.singletonList("x"), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.writeLines(Collections.singletonList("x").iterator(), (File) null));
    }

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByTheAppendFamily() throws IOException {
        final File real = file("b1-append-src.txt", "hello");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.append((File) null, real));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(real, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(real, 0, 1, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new byte[1], (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new byte[1], 0, 1, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[1], (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[1], StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[1], 0, 1, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new char[1], 0, 1, StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("x", (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append("x", StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine("x", (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLine("x", StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.singletonList("x"), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.singletonList("x"), StandardCharsets.UTF_8, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.singletonList("x").iterator(), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.appendLines(Collections.singletonList("x").iterator(), StandardCharsets.UTF_8, (File) null));
    }

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByTheStreamFactories() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileInputStream((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream((File) null, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileReader((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileReader((File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter((File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileWriter((File) null, StandardCharsets.UTF_8, true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream((File) null, 1024));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream((File) null, 1024));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedReader((File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedWriter((File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.createFileIfNotExists(null));
    }

    /** {@code new File((String) null)} is itself an NPE, so the file-NAME factories need the same guard. */
    @Test
    public void testNullFileNameIsRejectedAsIllegalArgumentByTheStreamFactories() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileInputStream((String) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream((String) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream((String) null, true));
    }

    @Test
    public void testNullFileIsRejectedAsIllegalArgumentByArchiveAndMerge() throws IOException {
        final File real = file("b1-archive-src.txt", "hello");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(real, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(real, (File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Arrays.asList(real), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(Arrays.asList(real), (File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(new File[] { real }, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Arrays.asList(real), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(Arrays.asList(real), new byte[0], (File) null));

        // An EMPTY source collection must be rejected for the same reason: merging nothing truncates destFile,
        // so a null destination must not be reached through the empty short-circuit either.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(new ArrayList<>(), (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(new ArrayList<>(), (File) null));
    }

    @Test
    public void testNullUrlOrDestinationIsRejectedByCopyUrlToFile() throws IOException {
        final File real = file("b1-url-dest.txt", "hello");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(null, real));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(null, real, 1000, 1000));
        final URL url = IOUtil.toUrl(real);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(url, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyURLToFile(url, (File) null, 1000, 1000));
    }

    /**
     * The whole-API guard: no public static {@code IOUtil} method that takes a {@code File} may answer an
     * all-null argument list with a {@code NullPointerException}.
     *
     * <p>The {@code read(File, byte[]|char[], ..)} overloads are the one exception and are skipped: their NPE
     * comes from the caller's <i>destination buffer</i>, not from the path, and that matches
     * {@link java.io.InputStream#read(byte[], int, int)} exactly.
     */
    @Test
    public void testNoPublicFileMethodAnswersNullWithANullPointerException() {
        final List<String> offenders = new ArrayList<>();

        for (final Method m : IOUtil.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            final Class<?>[] ps = m.getParameterTypes();
            boolean takesFile = false;

            for (final Class<?> p : ps) {
                if (p == File.class || p == File[].class) {
                    takesFile = true;
                    break;
                }
            }

            if (!takesFile || "read".equals(m.getName())) {
                continue;
            }

            final Object[] args = new Object[ps.length];

            for (int i = 0; i < ps.length; i++) {
                args[i] = ps[i].isPrimitive() ? CommonUtil.defaultValueOf(ps[i]) : null;
            }

            try {
                m.invoke(null, args);
            } catch (final InvocationTargetException e) {
                if (e.getCause() instanceof NullPointerException) {
                    offenders.add(m.getName() + Arrays.toString(ps));
                }
            } catch (final Exception e) {
                fail("could not invoke " + m + ": " + e);
            }
        }

        assertTrue(offenders.isEmpty(), "these report NPE instead of IllegalArgumentException: " + offenders);
    }

    // ------------------------------------------------------------------------------------------------
    // D1 - splitByLine accepts a null charset as UTF-8
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testSplitByLine_TreatsANullCharsetAsUtf8() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 20; i++) {
            sb.append("café-").append(i).append('\n');
        }

        final File src = file("d1-lines.log", sb.toString());
        final File dest = dir("d1-parts");

        // Used to throw IllegalArgumentException("'charset' cannot be null"), alone in the whole class.
        IOUtil.splitByLine(src, 4, dest, (Charset) null);

        final List<File> parts = IOUtil.listFiles(dest);
        parts.sort((a, b) -> a.getName().compareTo(b.getName()));
        assertEquals(4, parts.size());

        final List<String> all = new ArrayList<>();

        for (final File p : parts) {
            all.addAll(IOUtil.readAllLines(p, StandardCharsets.UTF_8));
        }

        assertEquals(20, all.size());
        assertEquals("café-0", all.get(0));
        assertEquals("café-19", all.get(19));
    }

    @Test
    public void testSplitByLine_NullCharsetMatchesTheExplicitUtf8Overload() throws IOException {
        final File src = file("d1-same.log", "a\nb\nc\nd\ne\nf\n");
        final File viaNull = dir("d1-null");
        final File viaUtf8 = dir("d1-utf8");

        IOUtil.splitByLine(src, 3, viaNull, (Charset) null);
        IOUtil.splitByLine(src, 3, viaUtf8, StandardCharsets.UTF_8);

        final List<File> a = IOUtil.listFiles(viaNull);
        final List<File> b = IOUtil.listFiles(viaUtf8);
        a.sort((x, y) -> x.getName().compareTo(y.getName()));
        b.sort((x, y) -> x.getName().compareTo(y.getName()));

        assertEquals(b.size(), a.size());

        for (int i = 0; i < a.size(); i++) {
            assertEquals(b.get(i).getName(), a.get(i).getName());
            assertTrue(IOUtil.contentEquals(a.get(i), b.get(i)), "part " + i + " differs");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D2 - a rejection names the argument the caller actually passed
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testRejectionMessagesNameTheCallersOwnParameter() throws IOException {
        final File realDir = dir("d2-dir");
        final File real = file("d2-file.txt", "x");

        assertMessageNames("srcFile", () -> IOUtil.copyToDirectory(null, realDir));
        assertMessageNames("srcFile", () -> IOUtil.moveToDirectory(null, realDir));
        assertMessageNames("srcFile", () -> IOUtil.copyFile(null, real));
        assertMessageNames("srcDir", () -> IOUtil.copyDirectory(null, realDir));
        assertMessageNames("sourceFile", () -> IOUtil.zip((File) null, real));
        assertMessageNames("srcZipFile", () -> IOUtil.unzip(null, realDir));
        // ...and the two-File methods now distinguish their two arguments.
        assertMessageNames("targetFile", () -> IOUtil.zip(real, (File) null));
        assertMessageNames("destFile", () -> IOUtil.merge(Arrays.asList(real), (File) null));
    }

    private void assertMessageNames(final String argName, final Throwables.Runnable<Exception> call) {
        try {
            call.run();
            fail("expected IllegalArgumentException naming '" + argName + "'");
        } catch (final IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(argName), "expected the message to name '" + argName + "' but it was: " + e.getMessage());
        } catch (final Exception e) {
            fail("expected IllegalArgumentException naming '" + argName + "' but got " + e);
        }
    }

    @Test
    public void testFactoryTypesAreThisLibrarysOwnNotTheJavaIoOnesOfTheSameName() {
        assertEquals(com.landawn.abacus.util.StringWriter.class, IOUtil.newStringWriter().getClass());
        assertFalse(java.io.StringWriter.class.isAssignableFrom(IOUtil.newStringWriter().getClass()),
                "com.landawn.abacus.util.StringWriter must stay unrelated to java.io.StringWriter, " + "which is why the javadoc examples qualify it");

        assertEquals(com.landawn.abacus.util.ByteArrayOutputStream.class, IOUtil.newByteArrayOutputStream().getClass());
        assertFalse(java.io.ByteArrayOutputStream.class.isAssignableFrom(IOUtil.newByteArrayOutputStream().getClass()),
                "com.landawn.abacus.util.ByteArrayOutputStream must stay unrelated to java.io.ByteArrayOutputStream");
    }

    @Test
    public void testUncheckedIoExceptionInTheExamplesIsThisLibrarysType() {
        // catch (java.io.UncheckedIOException) would compile and then never fire, so the class javadoc says
        // which type it means. Prove the two really are unrelated.
        assertFalse(java.io.UncheckedIOException.class.isAssignableFrom(com.landawn.abacus.exception.UncheckedIOException.class));
        assertFalse(com.landawn.abacus.exception.UncheckedIOException.class.isAssignableFrom(java.io.UncheckedIOException.class));

        final com.landawn.abacus.exception.UncheckedIOException thrown = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> IOUtil.readAllToString(new File(root(), "does-not-exist.txt")));
        assertNotNull(thrown.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // The guards must not have changed any accepted behaviour.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testValidArgumentsStillBehaveExactlyAsBefore() throws IOException {
        final File out = new File(root(), "ok/deep/out.txt");

        IOUtil.write("hello", out);
        assertEquals("hello", IOUtil.readAllToString(out));

        IOUtil.append(" world", out);
        assertEquals("hello world", IOUtil.readAllToString(out));

        IOUtil.writeLines(Arrays.asList("a", "b"), out);
        assertEquals(Arrays.asList("a", "b"), IOUtil.readAllLines(out));

        IOUtil.appendLines(Arrays.asList("c"), out);
        assertEquals(Arrays.asList("a", "b", "c"), IOUtil.readAllLines(out));

        // an empty write still truncates, an empty append still only creates
        IOUtil.write((byte[]) null, out);
        assertEquals(0L, out.length());
        IOUtil.append(new byte[0], out);
        assertEquals(0L, out.length());

        final File fresh = new File(root(), "ok/deep/fresh.txt");
        assertFalse(fresh.exists());
        assertTrue(IOUtil.createFileIfNotExists(fresh));
        assertTrue(fresh.exists());

        try (OutputStream os = IOUtil.newFileOutputStream(new File(root(), "ok/deep/os.bin"))) {
            os.write(new byte[] { 1, 2, 3 });
        }

        assertEquals(3L, new File(root(), "ok/deep/os.bin").length());

        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        final File src = file("ok-src.txt", "abcdef");
        assertEquals(6L, IOUtil.write(src, sink));
        assertEquals("abcdef", sink.toString("UTF-8"));

        assertEquals(3L, IOUtil.append(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), fresh));
        assertEquals("xyz", IOUtil.readAllToString(fresh));

        assertEquals(2L, IOUtil.append(new StringReader("hi"), fresh));
        assertEquals("xyzhi", IOUtil.readAllToString(fresh));
    }

    // ------------------------------------------------------------------------------------------------
    // G14-001 / G14-002: the count-taking write/append overloads reject a null source AND a null
    // destination - the outcome their own @throws tags now list
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testCountTakingWriteAndAppendRejectANullSourceOrDestination() throws IOException {
        final File real = file("count-taking-src.txt", "hello");
        final OutputStream os = new ByteArrayOutputStream();
        final Writer writer = IOUtil.newStringWriter();

        // the two char[] File overloads
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[3], 0, 3, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new char[3], 0, 3, StandardCharsets.UTF_8, (File) null));

        // the five convenience overloads - null destination
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(real, 0L, 1L, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new ByteArrayInputStream(new byte[] { 1 }), 0L, 1L, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new StringReader("x"), 0L, 1L, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(new StringReader("x"), 0L, 1L, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(new StringReader("x"), 0L, 1L, (File) null));

        // ... and null source, which four of those five left undocumented as well
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((InputStream) null, 0L, 1L, os));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((Reader) null, 0L, 1L, real));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((Reader) null, 0L, 1L, writer));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append((Reader) null, 0L, 1L, real));

        assertEquals("hello", IOUtil.readAllToString(real), "a rejected call leaves the file untouched");
    }
}
