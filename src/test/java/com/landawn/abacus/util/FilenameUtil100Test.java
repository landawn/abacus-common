package com.landawn.abacus.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FilenameUtil100Test extends TestBase {

    // Tests for normalize methods
    @Test
    public void testNormalize() {
        // Basic normalization
        Assertions.assertEquals("\\foo\\", FilenameUtil.normalize("/foo//"));
        Assertions.assertEquals("\\foo\\", FilenameUtil.normalize("/foo/./"));
        Assertions.assertEquals("\\bar", FilenameUtil.normalize("/foo/../bar"));
        Assertions.assertEquals("\\bar\\", FilenameUtil.normalize("/foo/../bar/"));
        Assertions.assertEquals("\\baz", FilenameUtil.normalize("/foo/../bar/../baz"));
        Assertions.assertNull(FilenameUtil.normalize("/../"));

        // Windows paths on Windows
        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertEquals("C:\\bar", FilenameUtil.normalize("C:\\foo\\..\\bar"));
        }

        // Null input
        Assertions.assertNull(FilenameUtil.normalize(null));

        // Empty string
        Assertions.assertEquals("", FilenameUtil.normalize(""));
    }

    @Test
    public void testNormalizeWithUnixSeparator() {
        // Force Unix separators
        Assertions.assertEquals("C:/foo/bar", FilenameUtil.normalize("C:\\foo\\bar", true));

        // Force Windows separators
        Assertions.assertEquals("\\foo\\bar", FilenameUtil.normalize("/foo/bar", false));

        // Null input
        Assertions.assertNull(FilenameUtil.normalize(null, true));
        Assertions.assertNull(FilenameUtil.normalize(null, false));
    }

    @Test
    public void testNormalizeNoEndSeparator() {
        Assertions.assertEquals("\\foo", FilenameUtil.normalizeNoEndSeparator("/foo//"));
        Assertions.assertEquals("\\foo", FilenameUtil.normalizeNoEndSeparator("/foo/./"));
        Assertions.assertEquals("\\bar", FilenameUtil.normalizeNoEndSeparator("/foo/../bar"));
        Assertions.assertEquals("\\foo\\bar", FilenameUtil.normalizeNoEndSeparator("/foo/bar/"));

        // Null input
        Assertions.assertNull(FilenameUtil.normalizeNoEndSeparator(null));
    }

    @Test
    public void testNormalizeNoEndSeparatorWithUnixSeparator() {
        // Unix separators, no trailing slash
        Assertions.assertEquals("C:/foo/bar", FilenameUtil.normalizeNoEndSeparator("C:\\foo\\bar\\", true));

        // Windows separators, no trailing slash
        Assertions.assertEquals("\\foo\\bar", FilenameUtil.normalizeNoEndSeparator("/foo/bar/", false));

        // Null input
        Assertions.assertNull(FilenameUtil.normalizeNoEndSeparator(null, true));
        Assertions.assertNull(FilenameUtil.normalizeNoEndSeparator(null, false));
    }

    // Tests for concat
    @Test
    public void testConcat() {
        String separator = File.separator;
        N.println(FilenameUtil.concat("/foo/", "bar"));   // Ens
        Assertions.assertEquals(Strings.concat(separator, "foo", separator, "bar"), FilenameUtil.concat("/foo/", "bar"));
        Assertions.assertEquals(Strings.concat(separator, "foo", separator, "bar"), FilenameUtil.concat("/foo", "bar"));
        Assertions.assertEquals(Strings.concat(separator, "bar"), FilenameUtil.concat("/foo", "/bar"));
        Assertions.assertEquals(Strings.concat("C:", separator, "bar"), FilenameUtil.concat("/foo", "C:/bar"));
        Assertions.assertEquals(Strings.concat(separator, "foo", separator, "bar"), FilenameUtil.concat("/foo/a/", "../bar"));
        Assertions.assertNull(FilenameUtil.concat("/foo/", "../../bar"));

        // Null inputs
        Assertions.assertNull(FilenameUtil.concat(null, "bar"));
        Assertions.assertNull(FilenameUtil.concat("/foo", null));

        // Empty basePath
        Assertions.assertEquals("bar", FilenameUtil.concat("", "bar"));
    }

    // Tests for directoryContains
    @Test
    public void testDirectoryContains() {
        Assertions.assertTrue(FilenameUtil.directoryContains("/Users/john", "/Users/john/documents"));
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/jane/documents"));
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/john"));

        // Null child
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", null));

        // Null parent
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.directoryContains(null, "/Users/john"));
    }

    // Tests for separator conversion methods
    @Test
    public void testSeparatorsToUnix() {
        Assertions.assertEquals("C:/docs/file.txt", FilenameUtil.separatorsToUnix("C:\\docs\\file.txt"));
        Assertions.assertEquals("/already/unix", FilenameUtil.separatorsToUnix("/already/unix"));

        // Null input
        Assertions.assertNull(FilenameUtil.separatorsToUnix(null));
    }

    @Test
    public void testSeparatorsToWindows() {
        Assertions.assertEquals("\\docs\\file.txt", FilenameUtil.separatorsToWindows("/docs/file.txt"));
        Assertions.assertEquals("C:\\already\\windows", FilenameUtil.separatorsToWindows("C:\\already\\windows"));

        // Null input
        Assertions.assertNull(FilenameUtil.separatorsToWindows(null));
    }

    @Test
    public void testSeparatorsToSystem() {
        String path = "/docs/file.txt";
        String result = FilenameUtil.separatorsToSystem(path);

        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertEquals("\\docs\\file.txt", result);
        } else {
            Assertions.assertEquals("/docs/file.txt", result);
        }

        // Null input
        Assertions.assertNull(FilenameUtil.separatorsToSystem(null));
    }

    // Tests for getPrefixLength
    @Test
    public void testGetPrefixLength() {
        // Unix
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength("a/b/c.txt"));
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("/a/b/c.txt"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~/a/b/c.txt"));
        Assertions.assertEquals(6, FilenameUtil.getPrefixLength("~user/a/b/c.txt"));

        // Windows
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength("a\\b\\c.txt"));
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("\\a\\b\\c.txt"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("C:a\\b\\c.txt"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:\\a\\b\\c.txt"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("\\\\server\\a\\b\\c.txt"));

        // Special cases
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength(null));
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength(""));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength(":"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~"));
    }

    // Tests for indexOfLastSeparator
    @Test
    public void testIndexOfLastSeparator() {
        Assertions.assertEquals(3, FilenameUtil.indexOfLastSeparator("a/b/c.txt"));
        Assertions.assertEquals(3, FilenameUtil.indexOfLastSeparator("a\\b\\c.txt"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator("file.txt"));

        // Null input
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator(null));
    }

    // Tests for indexOfExtension
    @Test
    public void testIndexOfExtension() {
        Assertions.assertEquals(4, FilenameUtil.indexOfExtension("file.txt"));
        Assertions.assertEquals(8, FilenameUtil.indexOfExtension("a/b/file.txt"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("a.txt/b"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("a/b/c"));

        // Null input
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension(null));
    }

    // Tests for getPrefix
    @Test
    public void testGetPrefix() {
        Assertions.assertEquals("C:\\", FilenameUtil.getPrefix("C:\\a\\b\\c.txt"));
        Assertions.assertEquals("/", FilenameUtil.getPrefix("/a/b/c.txt"));
        Assertions.assertEquals("~/", FilenameUtil.getPrefix("~/a/b/c.txt"));
        Assertions.assertEquals("", FilenameUtil.getPrefix("a/b/c.txt"));

        // Null input
        Assertions.assertNull(FilenameUtil.getPrefix(null));

        // Invalid prefix
        Assertions.assertNull(FilenameUtil.getPrefix(":invalid"));
    }

    // Tests for getPath
    @Test
    public void testGetPath() {
        Assertions.assertEquals("a\\b\\", FilenameUtil.getPath("C:\\a\\b\\c.txt"));
        Assertions.assertEquals("a/b/", FilenameUtil.getPath("~/a/b/c.txt"));
        Assertions.assertEquals("", FilenameUtil.getPath("a.txt"));
        Assertions.assertEquals("a/b/", FilenameUtil.getPath("a/b/c"));

        // Null input
        Assertions.assertNull(FilenameUtil.getPath(null));
    }

    // Tests for getPathNoEndSeparator
    @Test
    public void testGetPathNoEndSeparator() {
        Assertions.assertEquals("a\\b", FilenameUtil.getPathNoEndSeparator("C:\\a\\b\\c.txt"));
        Assertions.assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("~/a/b/c.txt"));
        Assertions.assertEquals("", FilenameUtil.getPathNoEndSeparator("a.txt"));
        Assertions.assertEquals("a/b/c", FilenameUtil.getPathNoEndSeparator("a/b/c/"));

        // Null input
        Assertions.assertNull(FilenameUtil.getPathNoEndSeparator(null));
    }

    // Tests for getFullPath
    @Test
    public void testGetFullPath() {
        Assertions.assertEquals("C:\\a\\b\\", FilenameUtil.getFullPath("C:\\a\\b\\c.txt"));
        Assertions.assertEquals("~/a/b/", FilenameUtil.getFullPath("~/a/b/c.txt"));
        Assertions.assertEquals("", FilenameUtil.getFullPath("a.txt"));
        Assertions.assertEquals("C:", FilenameUtil.getFullPath("C:"));

        // Null input
        Assertions.assertNull(FilenameUtil.getFullPath(null));
    }

    // Tests for getFullPathNoEndSeparator
    @Test
    public void testGetFullPathNoEndSeparator() {
        Assertions.assertEquals("C:\\a\\b", FilenameUtil.getFullPathNoEndSeparator("C:\\a\\b\\c.txt"));
        Assertions.assertEquals("~/a/b", FilenameUtil.getFullPathNoEndSeparator("~/a/b/c.txt"));
        Assertions.assertEquals("", FilenameUtil.getFullPathNoEndSeparator("a.txt"));
        Assertions.assertEquals("a/b/c", FilenameUtil.getFullPathNoEndSeparator("a/b/c/"));

        // Null input
        Assertions.assertNull(FilenameUtil.getFullPathNoEndSeparator(null));
    }

    // Tests for getName
    @Test
    public void testGetName() {
        Assertions.assertEquals("c.txt", FilenameUtil.getName("a/b/c.txt"));
        Assertions.assertEquals("a.txt", FilenameUtil.getName("a.txt"));
        Assertions.assertEquals("c", FilenameUtil.getName("a/b/c"));
        Assertions.assertEquals("", FilenameUtil.getName("a/b/c/"));

        // Null input
        Assertions.assertNull(FilenameUtil.getName(null));
    }

    // Tests for getBaseName
    @Test
    public void testGetBaseName() {
        Assertions.assertEquals("c", FilenameUtil.getBaseName("a/b/c.txt"));
        Assertions.assertEquals("a", FilenameUtil.getBaseName("a.txt"));
        Assertions.assertEquals("c", FilenameUtil.getBaseName("a/b/c"));
        Assertions.assertEquals("", FilenameUtil.getBaseName("a/b/c/"));

        // Null input
        Assertions.assertNull(FilenameUtil.getBaseName(null));
    }

    // Tests for getExtension
    @Test
    public void testGetExtension() {
        Assertions.assertEquals("txt", FilenameUtil.getExtension("foo.txt"));
        Assertions.assertEquals("jpg", FilenameUtil.getExtension("a/b/c.jpg"));
        Assertions.assertEquals("", FilenameUtil.getExtension("a/b.txt/c"));
        Assertions.assertEquals("", FilenameUtil.getExtension("a/b/c"));

        // Null input
        Assertions.assertNull(FilenameUtil.getExtension(null));
    }

    // Tests for removeExtension
    @Test
    public void testRemoveExtension() {
        Assertions.assertEquals("foo", FilenameUtil.removeExtension("foo.txt"));
        Assertions.assertEquals("a\\b\\c", FilenameUtil.removeExtension("a\\b\\c.jpg"));
        Assertions.assertEquals("a\\b\\c", FilenameUtil.removeExtension("a\\b\\c"));
        Assertions.assertEquals("a.b\\c", FilenameUtil.removeExtension("a.b\\c"));

        // Null input
        Assertions.assertNull(FilenameUtil.removeExtension(null));
    }

    // Tests for equals methods
    @Test
    public void testEquals() {
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt"));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT"));
        Assertions.assertTrue(FilenameUtil.equals(null, null));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", null));
        Assertions.assertFalse(FilenameUtil.equals(null, "file.txt"));
    }

    @Test
    public void testEqualsOnSystem() {
        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertTrue(FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT"));
        } else {
            Assertions.assertFalse(FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT"));
        }

        Assertions.assertTrue(FilenameUtil.equalsOnSystem(null, null));
        Assertions.assertFalse(FilenameUtil.equalsOnSystem("file.txt", null));
        Assertions.assertFalse(FilenameUtil.equalsOnSystem(null, "file.txt"));
    }

    @Test
    public void testEqualsNormalized() {
        Assertions.assertTrue(FilenameUtil.equalsNormalized("/foo/bar", "/foo/./bar"));
        Assertions.assertTrue(FilenameUtil.equalsNormalized("/foo/bar", "/foo/../foo/bar"));
        Assertions.assertFalse(FilenameUtil.equalsNormalized("/foo/bar", "/foo/Bar"));

        Assertions.assertTrue(FilenameUtil.equalsNormalized(null, null));
        Assertions.assertFalse(FilenameUtil.equalsNormalized("/foo/bar", null));
        Assertions.assertFalse(FilenameUtil.equalsNormalized(null, "/foo/bar"));
    }

    @Test
    public void testEqualsNormalizedOnSystem() {
        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertTrue(FilenameUtil.equalsNormalizedOnSystem("/foo/bar", "/FOO/./BAR"));
        } else {
            Assertions.assertFalse(FilenameUtil.equalsNormalizedOnSystem("/foo/bar", "/FOO/./BAR"));
        }

        Assertions.assertTrue(FilenameUtil.equalsNormalizedOnSystem(null, null));
        Assertions.assertFalse(FilenameUtil.equalsNormalizedOnSystem("/foo/bar", null));
        Assertions.assertFalse(FilenameUtil.equalsNormalizedOnSystem(null, "/foo/bar"));
    }

    @Test
    public void testEqualsWithParameters() {
        // Case sensitive
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt", false, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", false, IOCase.SENSITIVE));

        // Case insensitive
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "FILE.TXT", false, IOCase.INSENSITIVE));

        // Normalized
        Assertions.assertTrue(FilenameUtil.equals("/foo/bar", "/foo/./bar", true, IOCase.SENSITIVE));

        // Null case sensitivity defaults to SENSITIVE
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt", false, null));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", false, null));

        // Invalid normalization
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.equals("/../", "/../", true, IOCase.SENSITIVE));
    }

    // Tests for isExtension methods
    @Test
    public void testIsExtensionString() {
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", "txt"));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", "TXT"));
        Assertions.assertTrue(FilenameUtil.isExtension("file", ""));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (String) null));
        Assertions.assertTrue(FilenameUtil.isExtension("file", (String) null));

        // Null filename
        Assertions.assertFalse(FilenameUtil.isExtension(null, "txt"));
    }

    @Test
    public void testIsExtensionStringArray() {
        String[] exts = { "txt", "xml", "json" };
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", exts));
        Assertions.assertFalse(FilenameUtil.isExtension("file.doc", exts));

        // Null or empty extensions
        Assertions.assertTrue(FilenameUtil.isExtension("file", (String[]) null));
        Assertions.assertTrue(FilenameUtil.isExtension("file", new String[0]));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (String[]) null));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", new String[0]));

        // Null filename
        Assertions.assertFalse(FilenameUtil.isExtension(null, exts));
    }

    @Test
    public void testIsExtensionCollection() {
        Collection<String> exts = Arrays.asList("txt", "xml", "json");
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", exts));
        Assertions.assertFalse(FilenameUtil.isExtension("file.doc", exts));

        // Null or empty extensions
        Assertions.assertTrue(FilenameUtil.isExtension("file", (Collection<String>) null));
        Assertions.assertTrue(FilenameUtil.isExtension("file", new ArrayList<>()));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (Collection<String>) null));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", new ArrayList<>()));

        // Null filename
        Assertions.assertFalse(FilenameUtil.isExtension(null, exts));
    }

    // Tests for wildcardMatch methods
    @Test
    public void testWildcardMatch() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("c.txt", "*.txt"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("c.txt", "*.jpg"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/b/*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("c.txt", "*.???"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("c.txt", "*.????"));

        // Question mark matching
        Assertions.assertTrue(FilenameUtil.wildcardMatch("cat", "c?t"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("cat", "c??t"));

        // Multiple wildcards
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc.txt", "a*c.txt"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abbc.txt", "a*c.txt"));

        // Null cases
        Assertions.assertTrue(FilenameUtil.wildcardMatch(null, null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.txt", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch(null, "*.txt"));
    }

    @Test
    public void testWildcardMatchOnSystem() {
        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem("file.TXT", "*.txt"));
        } else {
            Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem("file.TXT", "*.txt"));
        }

        // Null cases
        Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem(null, null));
        Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem("file.txt", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem(null, "*.txt"));
    }

    @Test
    public void testWildcardMatchWithCaseSensitivity() {
        // Case sensitive
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.TXT", "*.txt", IOCase.SENSITIVE));

        // Case insensitive
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.TXT", "*.txt", IOCase.INSENSITIVE));

        // Null case sensitivity defaults to SENSITIVE
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.TXT", "*.txt", null));

        // Complex patterns
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc123def.txt", "abc*def.txt", IOCase.SENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc123def456.txt", "abc*def*.txt", IOCase.SENSITIVE));
    }

    // Test for splitOnTokens (package-private method, but test the behavior through wildcardMatch)
    @Test
    public void testWildcardMatchComplexPatterns() {
        // Test consecutive wildcards (should be collapsed)
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "****.txt", IOCase.SENSITIVE));

        // Test no wildcard characters
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.txt", IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.txt", "file.doc", IOCase.SENSITIVE));

        // Test ending with wildcard
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file*", IOCase.SENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.*", IOCase.SENSITIVE));

        // Test starting with wildcard
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*txt", IOCase.SENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", IOCase.SENSITIVE));

        // Test multiple segments
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc.def.txt", "*.*.txt", IOCase.SENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abcdefghi", "a*c*f*i", IOCase.SENSITIVE));
    }

    // Edge case tests
    @Test
    public void testNullByteInPath() {
        // Test null byte detection
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName("file\0name.txt"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getBaseName("file\0name.txt"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.removeExtension("file\0name.txt"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension("file\0name.txt", "txt"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize("file\0name.txt"));
    }

    @Test
    public void testEmptyStringPaths() {
        Assertions.assertEquals("", FilenameUtil.normalize(""));
        Assertions.assertEquals("", FilenameUtil.getPath(""));
        Assertions.assertEquals("", FilenameUtil.getPathNoEndSeparator(""));
        Assertions.assertEquals("", FilenameUtil.getFullPath(""));
        Assertions.assertEquals("", FilenameUtil.getFullPathNoEndSeparator(""));
        Assertions.assertEquals("", FilenameUtil.getName(""));
        Assertions.assertEquals("", FilenameUtil.getBaseName(""));
        Assertions.assertEquals("", FilenameUtil.getExtension(""));
        Assertions.assertEquals("", FilenameUtil.removeExtension(""));
    }

    @Test
    public void testSpecialCharactersInPath() {
        // Test paths with spaces
        Assertions.assertEquals("my file.txt", FilenameUtil.getName("/path/to/my file.txt"));
        Assertions.assertEquals("my file", FilenameUtil.getBaseName("/path/to/my file.txt"));

        // Test paths with dots
        Assertions.assertEquals("gz", FilenameUtil.getExtension("file.tar.gz"));
        Assertions.assertEquals("file.tar", FilenameUtil.removeExtension("file.tar.gz"));
    }

    @Test
    public void testComplexPrefixPatterns() {
        // Test tilde patterns
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~/"));
        Assertions.assertEquals(6, FilenameUtil.getPrefixLength("~user/"));
        Assertions.assertEquals(10, FilenameUtil.getPrefixLength("~username"));

        // Test Windows drive patterns
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("C:"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:/"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:\\"));

        // Test UNC patterns
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("//"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("\\\\"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("//server"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("//server/share"));
    }
}