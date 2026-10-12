package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilToTest extends IOUtilTestSupport {

    // toFile

    @Test
    public void testToFile_DecodesUtf8AndMalformedPercent() throws Exception {
        final File file = IOUtil.toFile(new URL("file:/tmp/a%20b/%E2%82%AC%2B%25bad%zz.txt"));
        final String path = file.getPath();

        assertTrue(path.contains("a b"));
        assertTrue(path.contains("\u20ac+%bad%zz.txt"));
    }





    // G131-01: java.io.File drops EVERY leading separator before a drive letter, so a drive-relative path after four
    // or more slashes - plain or escaped - is refused like "file:///C:x" (it became File("C:x\\y")); the same spellings
    // of an absolute path, and a UNC host followed by such a name, are unchanged
    @Test
    public void testToFile_driveRelativePathAfterSeveralLeadingSlashesIsRejected() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "drives exist on Windows only");

        for (final String spelling : new String[] { "file:////C:x/y", "file:////C%3Ax/y", "file:////%43:x/y", "file://///C:x/y" }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
            assertTrue(e.getMessage().startsWith("A file URL naming a drive must also name an absolute path on it"), e.getMessage());
        }

        assertEquals("C:\\x", IOUtil.toFile(new URL("file:////C:/x")).getPath());
        assertEquals("C:\\x", IOUtil.toFile(new URL("file:////C%3A/x")).getPath());
        assertEquals("\\\\server\\share\\x", IOUtil.toFile(new URL("file:////server/share/x")).getPath());
        assertEquals("\\\\server\\C:x", IOUtil.toFile(new URL("file:////server/C:x")).getPath());
    }

    @Test
    public void testToFile_dropsQueryAndFragment() throws Exception {
        final File plain = IOUtil.toFile(java.net.URI.create("file:///tmp/a.txt").toURL());
        final File withQuery = IOUtil.toFile(java.net.URI.create("file:///tmp/a.txt?v=1").toURL());
        final File withFragment = IOUtil.toFile(java.net.URI.create("file:///tmp/a.txt#frag").toURL());

        assertEquals(plain.getPath(), withQuery.getPath());
        assertEquals(plain.getPath(), withFragment.getPath());
        assertFalse(withQuery.getPath().contains("?"));
        assertFalse(withFragment.getPath().contains("#"));
    }




    // G045-02: a bare or drive-relative Windows drive spelled with an escaped letter or colon is refused like its plain
    // spelling; it used to decode to File("C:") / File("C:x"), the process's current directory on drive C:
    @Test
    public void testToFile_escapedBareOrDriveRelativeDriveIsRejected() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "drives exist on Windows only");

        for (final String spelling : new String[] { "file:///C%3A", "file:///C%3a", "file:///%43:", "file:///C%3Ax", "file:///%63%3A", "file:C%3A",
                "file:%43:x" }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
            assertTrue(e.getMessage().startsWith("A file URL naming a drive must also name an absolute path on it"), e.getMessage());
        }

        // an escaped colon in an absolute path is still decoded, as before
        assertEquals(new File("C:\\x"), IOUtil.toFile(new URL("file:///C%3A/x")));
        assertEquals(new File("C:\\x y"), IOUtil.toFile(new URL("file:///%43:/x%20y")));
    }





    // G045-02: the escaped bare / drive-relative drive is refused in the other URL forms that reach the decoded check too
    // ("localhost" authority, one-slash form, lower-case escaped letter); a decoded colon that does not follow a lone drive
    // letter stays part of a name, and absolute escaped forms and the UNC form decode as before
    @Test
    public void testToFile_escapedDriveIsRejectedInEveryFormAndOtherColonsAreKept() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(IOUtil.IS_OS_WINDOWS, "drives exist on Windows only");

        for (final String spelling : new String[] { "file://localhost/C%3A", "file:/C%3A", "file:/C%3Ax", "file:///%7A:", "file:///C%3A%3A" }) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new URL(spelling)), spelling);
        }

        assertEquals(new File("\\xy:"), IOUtil.toFile(new URL("file:///xy%3A")));
        assertEquals(new File("\\1:"), IOUtil.toFile(new URL("file:///%31:")));
        assertEquals(new File("C:\\x"), IOUtil.toFile(new URL("file://localhost/C%3A/x")));
        assertEquals(new File("C:\\x"), IOUtil.toFile(new URL("file:/C%3A/x")));
        assertEquals(new File("\\\\server\\C:"), IOUtil.toFile(new URL("file://server/C%3A")));
    }

    @Test
    public void testToFile_MalformedPercentEncodingPreserved() throws Exception {
        URL url = new URL("file:/tmp/invalid%2Gname.txt");

        File result = IOUtil.toFile(url);

        assertNotNull(result);
        assertTrue(result.getPath().endsWith("invalid%2Gname.txt"));
    }

    @Test
    public void testToFile_NullURL() {
        assertThrows(Exception.class, () -> IOUtil.toFile(null));
    }

    @Test
    public void testToFile_stillPercentDecodesAndRoundTrips() throws Exception {
        final File original = new File(tempFolder.toFile(), "my docs.txt");
        Files.write(original.toPath(), "x".getBytes(UTF_8));

        final File roundTripped = IOUtil.toFile(original.toURI().toURL());

        assertEquals(original.getCanonicalPath(), roundTripped.getCanonicalPath());
    }

    @Test
    public void testToFile_ValidURL() throws Exception {
        File file = Files.createTempFile(tempFolder, "url-test", ".txt").toFile();
        java.net.URL url = file.toURI().toURL();

        File result = IOUtil.toFile(url);

        assertNotNull(result);
        assertEquals(file.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void testToFileTreatsADriveLetterAuthorityAsADrive() throws Exception {
        // "file://C:/x" is malformed but common - the URL parser reads "C:" as the AUTHORITY. It is a drive, not
        // a host: treating it as one produced "\\C:\x", a path that exists nowhere. A colon cannot appear in a
        // real authority except to introduce a port, so the two cases are distinguishable.
        final String twoSlash = IOUtil.toFile(new java.net.URL("file://C:/tmp/a.txt")).getPath();
        final String threeSlash = IOUtil.toFile(new java.net.URL("file:///C:/tmp/a.txt")).getPath();

        assertFalse(twoSlash.startsWith("\\\\"), "a drive letter must not become a UNC host: " + twoSlash);
        assertFalse(twoSlash.startsWith("//"), "a drive letter must not become a UNC host: " + twoSlash);

        // Platform-independent: the two-slash form is rebuilt as the rooted "/C:/tmp/a.txt" that the canonical
        // three-slash form already produces, so the two agree on Windows AND on Unix. Splicing the drive on
        // without the leading slash would make them diverge off Windows, where "C:/tmp/a.txt" is relative.
        assertEquals(threeSlash, twoSlash, "the two-slash and three-slash forms must agree on every platform");
        assertEquals(new File("/C:/tmp/a.txt"), IOUtil.toFile(new java.net.URL("file://C:/tmp/a.txt")));

        // C-641: a genuine host is kept only on Windows, where it is a UNC path the platform can parse and toUrl
        // round-trips; elsewhere new File("//server/share/f.txt") collapses to the LOCAL "/server/share/f.txt"
        // (UnixFileSystem.normalize), so the URL is rejected there instead.
        if (IOUtil.IS_OS_WINDOWS) {
            final File unc = IOUtil.toFile(new java.net.URL("file://server/share/f.txt"));
            assertEquals("\\\\server\\share\\f.txt", unc.getPath());
            assertDoesNotThrow(unc::toPath);
            assertTrue(IOUtil.toUrl(unc).toString().contains("server"));
            assertEquals(unc, IOUtil.toFile(IOUtil.toUrl(unc)));
        } else {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new java.net.URL("file://server/share/f.txt")));
        }

        // A file URL has nowhere to put a port or credentials, and splicing them into a UNC name yields a path
        // the platform rejects outright (\\host:80\share -> InvalidPathException). Rejected as a bad argument.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new java.net.URL("file://host:80/share/f.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new java.net.URL("file://user@host/share/f.txt")));
        // An IPv6 literal fails the same way - its colons are not legal in a path.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new java.net.URL("file://[::1]/share/f.txt")));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(new java.net.URL("file://[2001:db8::1]/share/f.txt")));
        // localhost and the empty authority both stay local.
        assertFalse(IOUtil.toFile(new java.net.URL("file://localhost/tmp/a.txt")).getPath().contains("localhost"));
    }

    // toFiles

    @Test
    public void testToFiles_EmptyArray() throws Exception {
        java.net.URL[] urls = {};

        File[] files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(0, files.length);
    }

    @Test
    public void testToFiles_EmptyCollection() throws Exception {
        java.util.List<java.net.URL> urls = new java.util.ArrayList<>();

        java.util.List<File> files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testToFiles_NullURLCollection() throws Exception {
        assertThrows(Exception.class, () -> IOUtil.toFiles((java.util.Collection<java.net.URL>) null));
    }

    @Test
    public void testToFiles_URLArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "url1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "url2", ".txt").toFile();

        java.net.URL[] urls = { file1.toURI().toURL(), file2.toURI().toURL() };

        File[] files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(2, files.length);
    }

    @Test
    public void testToFiles_URLCollection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "url-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "url-c2", ".txt").toFile();

        java.util.List<java.net.URL> urls = java.util.Arrays.asList(file1.toURI().toURL(), file2.toURI().toURL());

        java.util.List<File> files = IOUtil.toFiles(urls);

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    public void testToUrl_NullFile() throws Exception {
        assertThrows(Exception.class, () -> IOUtil.toUrl(null));
    }

    @Test
    public void testToURL_ValidFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "to-url", ".txt").toFile();

        java.net.URL url = IOUtil.toUrl(file);

        assertNotNull(url);
        assertTrue(url.toString().contains(file.getName()));
    }

    // toUrls

    @Test
    public void testToURLs_EmptyArray() throws Exception {
        File[] files = {};

        java.net.URL[] urls = IOUtil.toUrls(files);

        assertNotNull(urls);
        assertEquals(0, urls.length);
    }

    @Test
    public void testToURLs_EmptyCollection() throws Exception {
        java.util.List<File> files = new java.util.ArrayList<>();

        java.util.List<java.net.URL> urls = IOUtil.toUrls(files);

        assertNotNull(urls);
        assertEquals(0, urls.size());
    }

    @Test
    public void testToURLs_FileArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "to-urls1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "to-urls2", ".txt").toFile();

        File[] files = { file1, file2 };

        java.net.URL[] urls = IOUtil.toUrls(files);

        assertNotNull(urls);
        assertEquals(2, urls.length);
    }

    @Test
    public void testToURLs_FileCollection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "to-urls-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "to-urls-c2", ".txt").toFile();

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);

        java.util.List<java.net.URL> urls = IOUtil.toUrls(files);

        assertNotNull(urls);
        assertEquals(2, urls.size());
    }

    @Test
    public void testToUrls_NullFileArray() throws Exception {
        assertThrows(Exception.class, () -> IOUtil.toUrls((File[]) null));
    }

    @Test
    public void testToUrls_NullFileCollection() throws Exception {
        assertThrows(Exception.class, () -> IOUtil.toUrls((java.util.Collection<File>) null));
    }


    // ---- bug review 2026-10-02 final G131 end ----
}
