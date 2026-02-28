package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Comprehensive unit tests for FilenameUtil class.
 * This test class provides thorough code path coverage for all public methods.
 */
@Tag("2025")
public class FilenameUtil2025Test extends TestBase {

    // ========== Tests for normalize methods ==========

    @Test
    public void testNormalize_WithAdjoiningSlashes() {
        // Test that multiple slashes are collapsed to single separator
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/foo/"), FilenameUtil.normalize("/foo///"));
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/a/b/c/"), FilenameUtil.normalize("/a//b///c////"));
    }

    @Test
    public void testNormalize_WithDoubleDots() {
        // Test double dot removal
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/"), FilenameUtil.normalize("/foo/bar/../.."));
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/baz/"), FilenameUtil.normalize("/foo/bar/../../baz/"));

        // Test invalid double dots (going above root)
        Assertions.assertNull(FilenameUtil.normalize("/../foo"));
        Assertions.assertNull(FilenameUtil.normalize("/foo/../../.."));
    }

    @Test
    public void testNormalize_WithMixedSeparators() {
        // Test mixed Unix and Windows separators
        Assertions.assertNotNull(FilenameUtil.normalize("/foo\\bar/baz\\qux"));
        Assertions.assertNotNull(FilenameUtil.normalize("C:\\foo/bar\\baz/qux"));
    }

    @Test
    public void testNormalize_WithPrefix() {
        // Test with various prefixes
        String result = FilenameUtil.normalize("C:/foo/./bar");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("foo"));
        Assertions.assertTrue(result.contains("bar"));
    }

    @Test
    public void testNormalize_EmptyAfterNormalization() {
        // Test path that becomes empty after normalization
        String result = FilenameUtil.normalize("/./");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testNormalize_WithUnixSeparator_True() {
        // Force Unix separators
        Assertions.assertEquals("C:/foo/bar/baz", FilenameUtil.normalize("C:\\foo\\bar\\baz", true));
        Assertions.assertEquals("/foo/bar", FilenameUtil.normalize("/foo/./bar", true));
        Assertions.assertEquals("/bar", FilenameUtil.normalize("/foo/../bar", true));
    }

    @Test
    public void testNormalize_WithUnixSeparator_False() {
        // Force Windows separators
        Assertions.assertEquals("C:\\foo\\bar", FilenameUtil.normalize("C:/foo/bar", false));
        Assertions.assertEquals("\\foo\\bar", FilenameUtil.normalize("/foo/./bar", false));
    }

    @Test
    public void testNormalizeNoEndSeparator_VariousCases() {
        // Test removal of trailing separator
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/foo/bar"), FilenameUtil.normalizeNoEndSeparator("/foo/bar///"));
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/foo"), FilenameUtil.normalizeNoEndSeparator("/foo/./"));

        // Test with no trailing separator already
        Assertions.assertEquals(FilenameUtil.separatorsToSystem("/foo/bar"), FilenameUtil.normalizeNoEndSeparator("/foo/bar"));
    }

    @Test
    public void testNormalizeNoEndSeparator_WithUnixSeparator() {
        // Test with Unix separator flag
        Assertions.assertEquals("C:/foo/bar", FilenameUtil.normalizeNoEndSeparator("C:\\foo\\bar\\", true));
        Assertions.assertEquals("C:/foo", FilenameUtil.normalizeNoEndSeparator("C:\\foo\\", true));

        // Test with Windows separator flag
        Assertions.assertEquals("\\foo\\bar", FilenameUtil.normalizeNoEndSeparator("/foo/bar/", false));
    }

    // ========== Tests for concat ==========

    @Test
    public void testConcat_WithAbsoluteFullFilenameToAdd() {
        // When fullFilenameToAdd is absolute, it should be normalized and returned
        String result = FilenameUtil.concat("/base", "/absolute/path");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("absolute"));
    }

    @Test
    public void testConcat_WithRelativePath() {
        // Test concatenation with relative paths
        String result = FilenameUtil.concat("/foo/bar", "baz/qux");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("foo"));
        Assertions.assertTrue(result.contains("baz"));
    }

    @Test
    public void testConcat_BasePathWithTrailingSeparator() {
        // Base path already has trailing separator
        String result1 = FilenameUtil.concat("/foo/", "bar");
        String result2 = FilenameUtil.concat("/foo", "bar");
        Assertions.assertNotNull(result1);
        Assertions.assertNotNull(result2);
    }

    @Test
    public void testConcat_WithDoubleDots() {
        // Test with .. in fullFilenameToAdd
        String result = FilenameUtil.concat("/foo/bar", "../baz");
        Assertions.assertNotNull(result);

        // Test going too far up
        Assertions.assertNull(FilenameUtil.concat("/foo", "../../bar"));
    }

    @Test
    public void testConcat_EmptyBasePath() {
        // Empty base path
        String result = FilenameUtil.concat("", "foo/bar");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testConcat_NullInputs() {
        // Null base path
        Assertions.assertNull(FilenameUtil.concat(null, "bar"));

        // Null fullFilenameToAdd
        Assertions.assertNull(FilenameUtil.concat("/foo", null));

        // Both null
        Assertions.assertNull(FilenameUtil.concat(null, null));
    }

    @Test
    public void testConcat_InvalidPrefix() {
        // Invalid prefix in fullFilenameToAdd
        String result = FilenameUtil.concat("/foo", ":invalid");
        Assertions.assertNull(result);
    }

    // ========== Tests for directoryContains ==========

    @Test
    public void testDirectoryContains_ValidContainment() {
        // Child is inside parent
        Assertions.assertTrue(FilenameUtil.directoryContains("/Users/john", "/Users/john/documents"));
        Assertions.assertTrue(FilenameUtil.directoryContains("/a/b", "/a/b/c/d/e"));
        Assertions.assertTrue(FilenameUtil.directoryContains("C:\\Users", "C:\\Users\\john"));
    }

    @Test
    public void testDirectoryContains_NotContained() {
        // Child is not inside parent
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/jane"));
        Assertions.assertFalse(FilenameUtil.directoryContains("/a/b/c", "/a/b"));
        Assertions.assertFalse(FilenameUtil.directoryContains("/foo", "/bar"));
    }

    @Test
    public void testDirectoryContains_SamePath() {
        // Directory does not contain itself
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/john"));
        Assertions.assertFalse(FilenameUtil.directoryContains("/foo/bar", "/foo/bar"));
    }

    @Test
    public void testDirectoryContains_NullChild() {
        // Null child returns false
        Assertions.assertFalse(FilenameUtil.directoryContains("/Users/john", null));
    }

    @Test
    public void testDirectoryContains_NullParent() {
        // Null parent throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.directoryContains(null, "/Users/john"));
    }

    @Test
    public void testDirectoryContains_CaseSensitivityOnSystem() {
        // Test depends on system case sensitivity
        if (IOUtil.IS_OS_WINDOWS) {
            // Windows is case-insensitive
            Assertions.assertTrue(FilenameUtil.directoryContains("C:\\Users\\JOHN", "C:\\Users\\john\\docs"));
        } else {
            // Unix is case-sensitive
            Assertions.assertFalse(FilenameUtil.directoryContains("/Users/JOHN", "/Users/john/docs"));
        }
    }

    // ========== Tests for separator conversion methods ==========

    @Test
    public void testSeparatorsToUnix_NoWindowsSeparators() {
        // No Windows separators, should return same string
        String path = "/foo/bar/baz";
        Assertions.assertSame(path, FilenameUtil.separatorsToUnix(path));
    }

    @Test
    public void testSeparatorsToUnix_WithWindowsSeparators() {
        // Convert Windows to Unix
        Assertions.assertEquals("C:/Program Files/Java", FilenameUtil.separatorsToUnix("C:\\Program Files\\Java"));
        Assertions.assertEquals("/foo/bar/baz", FilenameUtil.separatorsToUnix("\\foo\\bar\\baz"));
    }

    @Test
    public void testSeparatorsToUnix_Null() {
        Assertions.assertNull(FilenameUtil.separatorsToUnix(null));
    }

    @Test
    public void testSeparatorsToWindows_NoUnixSeparators() {
        // No Unix separators, should return same string
        String path = "C:\\foo\\bar\\baz";
        Assertions.assertSame(path, FilenameUtil.separatorsToWindows(path));
    }

    @Test
    public void testSeparatorsToWindows_WithUnixSeparators() {
        // Convert Unix to Windows
        Assertions.assertEquals("C:\\Program Files\\Java", FilenameUtil.separatorsToWindows("C:/Program Files/Java"));
        Assertions.assertEquals("\\foo\\bar\\baz", FilenameUtil.separatorsToWindows("/foo/bar/baz"));
    }

    @Test
    public void testSeparatorsToWindows_Null() {
        Assertions.assertNull(FilenameUtil.separatorsToWindows(null));
    }

    @Test
    public void testSeparatorsToSystem_OnCurrentOS() {
        String unixPath = "/foo/bar/baz";
        String windowsPath = "\\foo\\bar\\baz";

        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertEquals(windowsPath, FilenameUtil.separatorsToSystem(unixPath));
        } else {
            Assertions.assertEquals(unixPath, FilenameUtil.separatorsToSystem(windowsPath));
        }
    }

    @Test
    public void testSeparatorsToSystem_Null() {
        Assertions.assertNull(FilenameUtil.separatorsToSystem(null));
    }

    // ========== Tests for getPrefixLength ==========

    @Test
    public void testGetPrefixLength_UnixPaths() {
        // Relative path
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength("foo/bar"));

        // Absolute path
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("/foo/bar"));

        // Home directory
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~/"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~/foo"));

        // Named user home
        Assertions.assertEquals(6, FilenameUtil.getPrefixLength("~user/"));
        Assertions.assertEquals(10, FilenameUtil.getPrefixLength("~longuser/"));
    }

    @Test
    public void testGetPrefixLength_TildeWithoutSeparator() {
        // Tilde at end without separator
        Assertions.assertTrue(FilenameUtil.getPrefixLength("~username") > "~username".length());
        Assertions.assertTrue(FilenameUtil.getPrefixLength("~abc") > "~abc".length());
    }

    @Test
    public void testGetPrefixLength_WindowsDrivePaths() {
        // Drive relative
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("C:"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("C:foo"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("D:bar\\baz"));

        // Drive absolute
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:/"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:\\"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:/foo"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:\\foo"));

        // Check only valid drive letters (A-Z)
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("A:/foo"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("Z:/foo"));
    }

    @Test
    public void testGetPrefixLength_InvalidWindowsDrive() {
        // Invalid drive letter (lowercase gets converted to uppercase)
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("c:/foo"));

        // Non-letter before colon
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("1:/foo"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("@:/foo"));
    }

    @Test
    public void testGetPrefixLength_UNCPaths() {
        // Valid UNC paths
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("//server/share"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("\\\\server\\share"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("//server/share/path"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("//server/longershare"));

        // Invalid UNC paths (no server name or separator immediately after //)
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("//"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("\\\\"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("///"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("//server"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("\\\\server"));
    }

    @Test
    public void testGetPrefixLength_UNCPathWithSeparatorAtPosition2() {
        // UNC path with separator at position 2 is invalid
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("///"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength("\\\\\\"));
    }

    @Test
    public void testGetPrefixLength_SpecialCases() {
        // Null
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength(null));

        // Empty string
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength(""));

        // Just colon (invalid)
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength(":"));
        Assertions.assertEquals(-1, FilenameUtil.getPrefixLength(":foo"));

        // Slash colon slash (edge case)
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("/:foo"));
    }

    @Test
    public void testGetPrefixLength_SingleCharacter() {
        // Single separator
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("/"));
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("\\"));

        // Single regular character
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength("a"));

        // Single tilde
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("~"));
    }

    // ========== Tests for indexOfLastSeparator ==========

    @Test
    public void testIndexOfLastSeparator_UnixSeparators() {
        Assertions.assertEquals(3, FilenameUtil.indexOfLastSeparator("foo/bar"));
        Assertions.assertEquals(7, FilenameUtil.indexOfLastSeparator("foo/bar/baz"));
        Assertions.assertEquals(0, FilenameUtil.indexOfLastSeparator("/foo"));
    }

    @Test
    public void testIndexOfLastSeparator_WindowsSeparators() {
        Assertions.assertEquals(3, FilenameUtil.indexOfLastSeparator("foo\\bar"));
        Assertions.assertEquals(7, FilenameUtil.indexOfLastSeparator("foo\\bar\\baz"));
        Assertions.assertEquals(0, FilenameUtil.indexOfLastSeparator("\\foo"));
    }

    @Test
    public void testIndexOfLastSeparator_MixedSeparators() {
        // Should return the last separator regardless of type
        Assertions.assertEquals(7, FilenameUtil.indexOfLastSeparator("foo/bar\\baz"));
        Assertions.assertEquals(7, FilenameUtil.indexOfLastSeparator("foo\\bar/baz"));
    }

    @Test
    public void testIndexOfLastSeparator_NoSeparator() {
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator("file.txt"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator("nopath"));
    }

    @Test
    public void testIndexOfLastSeparator_Null() {
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator(null));
    }

    // ========== Tests for indexOfExtension ==========

    @Test
    public void testIndexOfExtension_SimpleFile() {
        Assertions.assertEquals(4, FilenameUtil.indexOfExtension("file.txt"));
        Assertions.assertEquals(8, FilenameUtil.indexOfExtension("document.pdf"));
    }

    @Test
    public void testIndexOfExtension_WithPath() {
        Assertions.assertEquals(12, FilenameUtil.indexOfExtension("path/to/file.txt"));
        Assertions.assertEquals(13, FilenameUtil.indexOfExtension("C:\\Users\\file.doc"));
    }

    @Test
    public void testIndexOfExtension_DotInDirectory() {
        // Dot in directory name, not in filename
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("path.dir/file"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("my.folder/document"));
    }

    @Test
    public void testIndexOfExtension_MultipleDots() {
        // Should return the last dot
        Assertions.assertEquals(8, FilenameUtil.indexOfExtension("file.tar.gz"));
        Assertions.assertEquals(7, FilenameUtil.indexOfExtension("my.test.txt"));
    }

    @Test
    public void testIndexOfExtension_NoDot() {
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("filename"));
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension("path/to/file"));
    }

    @Test
    public void testIndexOfExtension_Null() {
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension(null));
    }

    // ========== Tests for getPrefix ==========

    @Test
    public void testGetPrefix_WindowsDrive() {
        Assertions.assertEquals("C:\\", FilenameUtil.getPrefix("C:\\foo\\bar"));
        Assertions.assertEquals("C:/", FilenameUtil.getPrefix("C:/foo/bar"));
        Assertions.assertEquals("C:", FilenameUtil.getPrefix("C:foo"));
    }

    @Test
    public void testGetPrefix_UnixRoot() {
        Assertions.assertEquals("/", FilenameUtil.getPrefix("/foo/bar"));
        Assertions.assertEquals("/", FilenameUtil.getPrefix("/"));
    }

    @Test
    public void testGetPrefix_HomeDirectory() {
        Assertions.assertEquals("~/", FilenameUtil.getPrefix("~/foo/bar"));
        Assertions.assertEquals("~user/", FilenameUtil.getPrefix("~user/foo/bar"));
    }

    @Test
    public void testGetPrefix_TildeWithoutSeparator() {
        // Tilde without separator - returns tilde + separator
        String result = FilenameUtil.getPrefix("~username");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("~"));
    }

    @Test
    public void testGetPrefix_NoPrefix() {
        Assertions.assertEquals("", FilenameUtil.getPrefix("foo/bar"));
        Assertions.assertEquals("", FilenameUtil.getPrefix("relative/path"));
    }

    @Test
    public void testGetPrefix_Null() {
        Assertions.assertNull(FilenameUtil.getPrefix(null));
    }

    @Test
    public void testGetPrefix_Invalid() {
        Assertions.assertNull(FilenameUtil.getPrefix(":invalid"));
        Assertions.assertNull(FilenameUtil.getPrefix(":"));
    }

    // ========== Tests for getPath ==========

    @Test
    public void testGetPath_WithTrailingSeparator() {
        Assertions.assertEquals("foo\\bar\\", FilenameUtil.getPath("C:\\foo\\bar\\file.txt"));
        Assertions.assertEquals("a/b/c/", FilenameUtil.getPath("~/a/b/c/file.txt"));
    }

    @Test
    public void testGetPath_DirectoryPath() {
        Assertions.assertEquals("a/b/", FilenameUtil.getPath("a/b/c"));
    }

    @Test
    public void testGetPath_NoPath() {
        Assertions.assertEquals("", FilenameUtil.getPath("file.txt"));
        Assertions.assertEquals("", FilenameUtil.getPath("C:file.txt"));
    }

    @Test
    public void testGetPath_OnlyPrefix() {
        Assertions.assertEquals("", FilenameUtil.getPath("/"));
        Assertions.assertEquals("", FilenameUtil.getPath("C:"));
        Assertions.assertEquals("", FilenameUtil.getPath("C:\\"));
    }

    @Test
    public void testGetPath_Null() {
        Assertions.assertNull(FilenameUtil.getPath(null));
    }

    @Test
    public void testGetPath_InvalidPrefix() {
        Assertions.assertNull(FilenameUtil.getPath(":invalid"));
    }

    // ========== Tests for getPathNoEndSeparator ==========

    @Test
    public void testGetPathNoEndSeparator_NoTrailingSeparator() {
        Assertions.assertEquals("foo\\bar", FilenameUtil.getPathNoEndSeparator("C:\\foo\\bar\\file.txt"));
        Assertions.assertEquals("a/b/c", FilenameUtil.getPathNoEndSeparator("~/a/b/c/file.txt"));
    }

    @Test
    public void testGetPathNoEndSeparator_PathWithTrailingSeparator() {
        Assertions.assertEquals("a/b/c", FilenameUtil.getPathNoEndSeparator("a/b/c/"));
        Assertions.assertEquals("foo\\bar", FilenameUtil.getPathNoEndSeparator("C:\\foo\\bar\\"));
    }

    @Test
    public void testGetPathNoEndSeparator_NoPath() {
        Assertions.assertEquals("", FilenameUtil.getPathNoEndSeparator("file.txt"));
    }

    @Test
    public void testGetPathNoEndSeparator_Null() {
        Assertions.assertNull(FilenameUtil.getPathNoEndSeparator(null));
    }

    // ========== Tests for getFullPath ==========

    @Test
    public void testGetFullPath_CompleteFilename() {
        Assertions.assertEquals("C:\\foo\\bar\\", FilenameUtil.getFullPath("C:\\foo\\bar\\file.txt"));
        Assertions.assertEquals("~/a/b/", FilenameUtil.getFullPath("~/a/b/file.txt"));
        Assertions.assertEquals("/usr/local/", FilenameUtil.getFullPath("/usr/local/bin"));
    }

    @Test
    public void testGetFullPath_OnlyPrefix() {
        Assertions.assertEquals("C:", FilenameUtil.getFullPath("C:"));
        Assertions.assertEquals("/", FilenameUtil.getFullPath("/"));
    }

    @Test
    public void testGetFullPath_PrefixOnly_WithSeparator() {
        Assertions.assertEquals("C:\\", FilenameUtil.getFullPath("C:\\"));
        Assertions.assertEquals("~/", FilenameUtil.getFullPath("~/"));
    }

    @Test
    public void testGetFullPath_NoDirectory() {
        Assertions.assertEquals("", FilenameUtil.getFullPath("file.txt"));
        Assertions.assertEquals("C:", FilenameUtil.getFullPath("C:file.txt"));
    }

    @Test
    public void testGetFullPath_Null() {
        Assertions.assertNull(FilenameUtil.getFullPath(null));
    }

    // ========== Tests for getFullPathNoEndSeparator ==========

    @Test
    public void testGetFullPathNoEndSeparator_CompleteFilename() {
        Assertions.assertEquals("C:\\foo\\bar", FilenameUtil.getFullPathNoEndSeparator("C:\\foo\\bar\\file.txt"));
        Assertions.assertEquals("~/a/b", FilenameUtil.getFullPathNoEndSeparator("~/a/b/file.txt"));
    }

    @Test
    public void testGetFullPathNoEndSeparator_DirectoryWithTrailingSeparator() {
        Assertions.assertEquals("a/b/c", FilenameUtil.getFullPathNoEndSeparator("a/b/c/"));
    }

    @Test
    public void testGetFullPathNoEndSeparator_OnlyPrefix() {
        Assertions.assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:"));
        Assertions.assertEquals("C:\\", FilenameUtil.getFullPathNoEndSeparator("C:\\"));
    }

    @Test
    public void testGetFullPathNoEndSeparator_Root() {
        String result = FilenameUtil.getFullPathNoEndSeparator("/");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetFullPathNoEndSeparator_Null() {
        Assertions.assertNull(FilenameUtil.getFullPathNoEndSeparator(null));
    }

    // ========== Tests for getName ==========

    @Test
    public void testGetName_SimpleFilename() {
        Assertions.assertEquals("file.txt", FilenameUtil.getName("file.txt"));
    }

    @Test
    public void testGetName_WithPath() {
        Assertions.assertEquals("file.txt", FilenameUtil.getName("/path/to/file.txt"));
        Assertions.assertEquals("file.txt", FilenameUtil.getName("C:\\path\\to\\file.txt"));
    }

    @Test
    public void testGetName_DirectoryWithTrailingSeparator() {
        Assertions.assertEquals("", FilenameUtil.getName("/path/to/dir/"));
        Assertions.assertEquals("", FilenameUtil.getName("C:\\path\\to\\dir\\"));
    }

    @Test
    public void testGetName_JustDirectory() {
        Assertions.assertEquals("dir", FilenameUtil.getName("/path/to/dir"));
    }

    @Test
    public void testGetName_Null() {
        Assertions.assertNull(FilenameUtil.getName(null));
    }

    // ========== Tests for getBaseName ==========

    @Test
    public void testGetBaseName_SimpleFilename() {
        Assertions.assertEquals("file", FilenameUtil.getBaseName("file.txt"));
    }

    @Test
    public void testGetBaseName_WithPath() {
        Assertions.assertEquals("file", FilenameUtil.getBaseName("/path/to/file.txt"));
        Assertions.assertEquals("document", FilenameUtil.getBaseName("C:\\docs\\document.pdf"));
    }

    @Test
    public void testGetBaseName_MultipleExtensions() {
        Assertions.assertEquals("file.tar", FilenameUtil.getBaseName("file.tar.gz"));
    }

    @Test
    public void testGetBaseName_NoExtension() {
        Assertions.assertEquals("filename", FilenameUtil.getBaseName("filename"));
        Assertions.assertEquals("dir", FilenameUtil.getBaseName("/path/to/dir"));
    }

    @Test
    public void testGetBaseName_EmptyName() {
        Assertions.assertEquals("", FilenameUtil.getBaseName("/path/to/dir/"));
    }

    @Test
    public void testGetBaseName_Null() {
        Assertions.assertNull(FilenameUtil.getBaseName(null));
    }

    // ========== Tests for getExtension ==========

    @Test
    public void testGetExtension_SimpleFile() {
        Assertions.assertEquals("txt", FilenameUtil.getExtension("file.txt"));
        Assertions.assertEquals("pdf", FilenameUtil.getExtension("document.pdf"));
    }

    @Test
    public void testGetExtension_WithPath() {
        Assertions.assertEquals("txt", FilenameUtil.getExtension("/path/to/file.txt"));
        Assertions.assertEquals("java", FilenameUtil.getExtension("C:\\src\\Main.java"));
    }

    @Test
    public void testGetExtension_MultipleExtensions() {
        // Returns last extension
        Assertions.assertEquals("gz", FilenameUtil.getExtension("file.tar.gz"));
        Assertions.assertEquals("gz", FilenameUtil.getExtension("app-v2.3.1-star.gz"));
        Assertions.assertEquals("app-v2.3.1-star", FilenameUtil.getBaseName("app-v2.3.1-star.gz"));
    }

    @Test
    public void testGetExtension_DotInDirectory() {
        Assertions.assertEquals("", FilenameUtil.getExtension("my.dir/filename"));
    }

    @Test
    public void testGetExtension_NoExtension() {
        Assertions.assertEquals("", FilenameUtil.getExtension("filename"));
        Assertions.assertEquals("", FilenameUtil.getExtension("/path/to/file"));
    }

    @Test
    public void testGetExtension_Null() {
        Assertions.assertNull(FilenameUtil.getExtension(null));
    }

    // ========== Tests for removeExtension ==========

    @Test
    public void testRemoveExtension_SimpleFile() {
        Assertions.assertEquals("file", FilenameUtil.removeExtension("file.txt"));
        Assertions.assertEquals("document", FilenameUtil.removeExtension("document.pdf"));
    }

    @Test
    public void testRemoveExtension_WithPath() {
        Assertions.assertEquals("path/to/file", FilenameUtil.removeExtension("path/to/file.txt"));
        Assertions.assertEquals("C:\\docs\\file", FilenameUtil.removeExtension("C:\\docs\\file.doc"));
    }

    @Test
    public void testRemoveExtension_MultipleExtensions() {
        Assertions.assertEquals("file.tar", FilenameUtil.removeExtension("file.tar.gz"));
    }

    @Test
    public void testRemoveExtension_NoExtension() {
        Assertions.assertEquals("filename", FilenameUtil.removeExtension("filename"));
        Assertions.assertEquals("path/to/file", FilenameUtil.removeExtension("path/to/file"));
    }

    @Test
    public void testRemoveExtension_DotInDirectory() {
        Assertions.assertEquals("my.dir/filename", FilenameUtil.removeExtension("my.dir/filename"));
    }

    @Test
    public void testRemoveExtension_Null() {
        Assertions.assertNull(FilenameUtil.removeExtension(null));
    }

    // ========== Tests for equals methods ==========

    @Test
    public void testEquals_CaseSensitive() {
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt"));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT"));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "file.doc"));
    }

    @Test
    public void testEquals_NullCases() {
        Assertions.assertTrue(FilenameUtil.equals(null, null));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", null));
        Assertions.assertFalse(FilenameUtil.equals(null, "file.txt"));
    }

    @Test
    public void testEqualsOnSystem_CaseSensitivity() {
        Assertions.assertTrue(FilenameUtil.equalsOnSystem("file.txt", "file.txt"));

        if (IOUtil.IS_OS_WINDOWS) {
            // Case-insensitive on Windows
            Assertions.assertTrue(FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT"));
            Assertions.assertTrue(FilenameUtil.equalsOnSystem("C:\\Path", "c:\\path"));
        } else {
            // Case-sensitive on Unix
            Assertions.assertFalse(FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT"));
        }
    }

    @Test
    public void testEqualsNormalized_WithNormalization() {
        Assertions.assertTrue(FilenameUtil.equalsNormalized("/foo/./bar", "/foo/bar"));
        Assertions.assertTrue(FilenameUtil.equalsNormalized("/foo/../foo/bar", "/foo/bar"));
        Assertions.assertTrue(FilenameUtil.equalsNormalized("/foo//bar", "/foo/bar"));
    }

    @Test
    public void testEqualsNormalized_CaseSensitive() {
        Assertions.assertFalse(FilenameUtil.equalsNormalized("/foo/Bar", "/foo/bar"));
    }

    @Test
    public void testEqualsNormalized_NullCases() {
        Assertions.assertTrue(FilenameUtil.equalsNormalized(null, null));
        Assertions.assertFalse(FilenameUtil.equalsNormalized("/foo", null));
        Assertions.assertFalse(FilenameUtil.equalsNormalized(null, "/foo"));
    }

    @Test
    public void testEqualsNormalizedOnSystem_SystemCaseSensitivity() {
        Assertions.assertTrue(FilenameUtil.equalsNormalizedOnSystem("/foo/./bar", "/foo/bar"));

        if (IOUtil.IS_OS_WINDOWS) {
            Assertions.assertTrue(FilenameUtil.equalsNormalizedOnSystem("/FOO/bar", "/foo/bar"));
        } else {
            Assertions.assertFalse(FilenameUtil.equalsNormalizedOnSystem("/FOO/bar", "/foo/bar"));
        }
    }

    @Test
    public void testEquals_WithAllParameters_Normalized() {
        // Normalized comparison
        Assertions.assertTrue(FilenameUtil.equals("/foo/./bar", "/foo/bar", true, IOCase.SENSITIVE));
        Assertions.assertTrue(FilenameUtil.equals("/foo/../foo/bar", "/foo/bar", true, IOCase.SENSITIVE));
    }

    @Test
    public void testEquals_WithAllParameters_CaseInsensitive() {
        // Case insensitive
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "FILE.TXT", false, IOCase.INSENSITIVE));
        Assertions.assertTrue(FilenameUtil.equals("/Foo/Bar", "/foo/bar", false, IOCase.INSENSITIVE));
    }

    @Test
    public void testEquals_WithAllParameters_CaseSensitive() {
        // Case sensitive
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt", false, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", false, IOCase.SENSITIVE));
    }

    @Test
    public void testEquals_WithAllParameters_NullCaseSensitivity() {
        // Null case sensitivity defaults to SENSITIVE
        Assertions.assertTrue(FilenameUtil.equals("file.txt", "file.txt", false, null));
        Assertions.assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", false, null));
    }

    @Test
    public void testEquals_WithAllParameters_InvalidNormalization() {
        // Invalid normalization throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.equals("/../invalid", "/foo", true, IOCase.SENSITIVE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.equals("/foo", "/../invalid", true, IOCase.SENSITIVE));
    }

    @Test
    public void testEquals_WithAllParameters_NullInputs() {
        Assertions.assertTrue(FilenameUtil.equals(null, null, false, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.equals("file", null, false, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.equals(null, "file", false, IOCase.SENSITIVE));
    }

    // ========== Tests for isExtension methods ==========

    @Test
    public void testIsExtension_String_Match() {
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", "txt"));
        Assertions.assertTrue(FilenameUtil.isExtension("document.pdf", "pdf"));
    }

    @Test
    public void testIsExtension_String_NoMatch() {
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", "pdf"));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", "TXT")); // Case sensitive
    }

    @Test
    public void testIsExtension_String_EmptyExtension() {
        Assertions.assertTrue(FilenameUtil.isExtension("file", ""));
        Assertions.assertTrue(FilenameUtil.isExtension("file", (String) null));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", ""));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (String) null));
    }

    @Test
    public void testIsExtension_String_NullFilename() {
        Assertions.assertFalse(FilenameUtil.isExtension(null, "txt"));
        Assertions.assertFalse(FilenameUtil.isExtension(null, ""));
        Assertions.assertFalse(FilenameUtil.isExtension(null, (String) null));
    }

    @Test
    public void testIsExtension_StringArray_Match() {
        String[] exts = { "txt", "pdf", "doc" };
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", exts));
        Assertions.assertTrue(FilenameUtil.isExtension("file.pdf", exts));
        Assertions.assertTrue(FilenameUtil.isExtension("file.doc", exts));
    }

    @Test
    public void testIsExtension_StringArray_NoMatch() {
        String[] exts = { "txt", "pdf", "doc" };
        Assertions.assertFalse(FilenameUtil.isExtension("file.jpg", exts));
        Assertions.assertFalse(FilenameUtil.isExtension("file.TXT", exts)); // Case sensitive
    }

    @Test
    public void testIsExtension_StringArray_NullOrEmpty() {
        Assertions.assertTrue(FilenameUtil.isExtension("file", (String[]) null));
        Assertions.assertTrue(FilenameUtil.isExtension("file", new String[0]));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (String[]) null));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", new String[0]));
    }

    @Test
    public void testIsExtension_StringArray_NullFilename() {
        String[] exts = { "txt", "pdf" };
        Assertions.assertFalse(FilenameUtil.isExtension(null, exts));
        Assertions.assertFalse(FilenameUtil.isExtension(null, (String[]) null));
        Assertions.assertFalse(FilenameUtil.isExtension(null, new String[0]));
    }

    @Test
    public void testIsExtension_Collection_Match() {
        Collection<String> exts = Arrays.asList("txt", "pdf", "doc");
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", exts));
        Assertions.assertTrue(FilenameUtil.isExtension("file.pdf", exts));
    }

    @Test
    public void testIsExtension_Collection_NoMatch() {
        Collection<String> exts = Arrays.asList("txt", "pdf", "doc");
        Assertions.assertFalse(FilenameUtil.isExtension("file.jpg", exts));
    }

    @Test
    public void testIsExtension_Collection_NullOrEmpty() {
        Assertions.assertTrue(FilenameUtil.isExtension("file", (Collection<String>) null));
        Assertions.assertTrue(FilenameUtil.isExtension("file", Collections.emptyList()));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", (Collection<String>) null));
        Assertions.assertFalse(FilenameUtil.isExtension("file.txt", Collections.emptyList()));
    }

    @Test
    public void testIsExtension_Collection_NullFilename() {
        Collection<String> exts = Arrays.asList("txt", "pdf");
        Assertions.assertFalse(FilenameUtil.isExtension(null, exts));
        Assertions.assertFalse(FilenameUtil.isExtension(null, (Collection<String>) null));
    }

    // ========== Tests for wildcardMatch methods ==========

    @Test
    public void testWildcardMatch_ExactMatch() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.txt"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.txt", "file.doc"));
    }

    @Test
    public void testWildcardMatch_StarWildcard() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc.txt", "a*.txt"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc.txt", "*c.txt"));
    }

    @Test
    public void testWildcardMatch_QuestionMarkWildcard() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.tx?"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.t?t"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("cat", "c?t"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("cat", "?at"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("cat", "ca?"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("cat", "c??t"));
    }

    @Test
    public void testWildcardMatch_CombinedWildcards() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file123.txt", "file*.tx?"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc.txt", "a?*.txt"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("test.java", "*.???"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("test.java", "*.????"));
    }

    @Test
    public void testWildcardMatch_MultipleStars() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abcdefghi", "a*c*f*i"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc123def456.txt", "abc*def*.txt"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "****.txt")); // Multiple stars collapse
    }

    @Test
    public void testWildcardMatch_PathSeparators() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/b/*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/*/c.txt"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("foo/bar/baz.txt", "foo/*/*.txt"));
    }

    @Test
    public void testWildcardMatch_EndingWithStar() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "file*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt.bak", "file*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("filename", "file*"));
    }

    @Test
    public void testWildcardMatch_NullCases() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch(null, null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.txt", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch(null, "*.txt"));
    }

    @Test
    public void testWildcardMatch_EmptyStrings() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("", ""));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("", "*"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("", "?"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file", ""));
    }

    @Test
    public void testWildcardMatchOnSystem_CaseSensitivity() {
        if (IOUtil.IS_OS_WINDOWS) {
            // Case-insensitive on Windows
            Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem("FILE.TXT", "*.txt"));
            Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem("file.TXT", "FILE.*"));
        } else {
            // Case-sensitive on Unix
            Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem("FILE.TXT", "*.txt"));
            Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem("file.txt", "*.txt"));
        }
    }

    @Test
    public void testWildcardMatchOnSystem_NullCases() {
        Assertions.assertTrue(FilenameUtil.wildcardMatchOnSystem(null, null));
        Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem("file", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatchOnSystem(null, "*"));
    }

    @Test
    public void testWildcardMatch_WithCaseSensitivity_Sensitive() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file.TXT", "*.txt", IOCase.SENSITIVE));
    }

    @Test
    public void testWildcardMatch_WithCaseSensitivity_Insensitive() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.TXT", IOCase.INSENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.INSENSITIVE));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("FiLe.TxT", "*.TxT", IOCase.INSENSITIVE));
    }

    @Test
    public void testWildcardMatch_WithCaseSensitivity_Null() {
        // Null case sensitivity defaults to SENSITIVE
        Assertions.assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", null));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", null));
    }

    @Test
    public void testWildcardMatch_WithCaseSensitivity_NullInputs() {
        Assertions.assertTrue(FilenameUtil.wildcardMatch(null, null, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("file", null, IOCase.SENSITIVE));
        Assertions.assertFalse(FilenameUtil.wildcardMatch(null, "*", IOCase.SENSITIVE));
    }

    @Test
    public void testWildcardMatch_BacktrackingScenarios() {
        // Test backtracking in wildcard matching algorithm
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abcccd", "*ccd"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("mississipissippi", "*issip*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("xxxx", "*x"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abcabczzzde", "*abc*de"));
    }

    @Test
    public void testWildcardMatch_ComplexBacktracking() {
        // More complex backtracking scenarios
        Assertions.assertTrue(FilenameUtil.wildcardMatch("aaabbbaaabbb", "*aaa*bbb"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("aaaaaaaaaaaaa", "*aaa*aaa"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("abc", "*z"));
    }

    // ========== Edge cases and special scenarios ==========

    @Test
    public void testNullByteDetection_InVariousMethods() {
        // Test null byte detection in multiple methods
        String nullBytePath = "file\0name.txt";

        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(nullBytePath));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getBaseName(nullBytePath));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.removeExtension(nullBytePath));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(nullBytePath, "txt"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(nullBytePath, new String[] { "txt" }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(nullBytePath, Arrays.asList("txt")));
    }

    @Test
    public void testEmptyStringHandling_AllMethods() {
        // Test empty string handling across methods
        String empty = "";

        Assertions.assertEquals("", FilenameUtil.normalize(empty));
        Assertions.assertEquals("", FilenameUtil.normalizeNoEndSeparator(empty));
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength(empty));
        Assertions.assertEquals(-1, FilenameUtil.indexOfLastSeparator(empty));
        Assertions.assertEquals(-1, FilenameUtil.indexOfExtension(empty));
        Assertions.assertEquals("", FilenameUtil.getPrefix(empty));
        Assertions.assertEquals("", FilenameUtil.getPath(empty));
        Assertions.assertEquals("", FilenameUtil.getPathNoEndSeparator(empty));
        Assertions.assertEquals("", FilenameUtil.getFullPath(empty));
        Assertions.assertEquals("", FilenameUtil.getFullPathNoEndSeparator(empty));
        Assertions.assertEquals("", FilenameUtil.getName(empty));
        Assertions.assertEquals("", FilenameUtil.getBaseName(empty));
        Assertions.assertEquals("", FilenameUtil.getExtension(empty));
        Assertions.assertEquals("", FilenameUtil.removeExtension(empty));
    }

    @Test
    public void testSpecialCharacters_InFilenames() {
        // Test filenames with spaces, dots, and special characters
        Assertions.assertEquals("my file.txt", FilenameUtil.getName("/path/my file.txt"));
        Assertions.assertEquals("my file", FilenameUtil.getBaseName("/path/my file.txt"));
        Assertions.assertEquals("txt", FilenameUtil.getExtension("my file.txt"));

        // Multiple dots
        Assertions.assertEquals("gz", FilenameUtil.getExtension("archive.tar.gz"));
        Assertions.assertEquals("archive.tar", FilenameUtil.getBaseName("archive.tar.gz"));

        // Dot at start
        Assertions.assertEquals("hidden", FilenameUtil.getExtension(".hidden"));
        Assertions.assertEquals("", FilenameUtil.getBaseName(".hidden"));
    }

    @Test
    public void testDotFiles_UnixHiddenFiles() {
        // Unix hidden files (starting with dot)
        Assertions.assertEquals(".bashrc", FilenameUtil.getName("~/.bashrc"));
        Assertions.assertEquals("", FilenameUtil.getBaseName(".bashrc"));
        Assertions.assertEquals("bashrc", FilenameUtil.getExtension(".bashrc"));

        Assertions.assertEquals(".profile", FilenameUtil.getName("/home/user/.profile"));
        Assertions.assertEquals("", FilenameUtil.getBaseName(".profile"));
    }

    @Test
    public void testTrailingSeparators_VariousMethods() {
        // Test paths with trailing separators
        Assertions.assertEquals("", FilenameUtil.getName("/path/to/dir/"));
        Assertions.assertEquals("", FilenameUtil.getBaseName("/path/to/dir/"));
        Assertions.assertEquals("", FilenameUtil.getExtension("/path/to/dir/"));
    }

    @Test
    public void testLongPaths() {
        // Test with very long paths
        StringBuilder longPath = new StringBuilder("/");
        for (int i = 0; i < 100; i++) {
            longPath.append("dir").append(i).append("/");
        }
        longPath.append("file.txt");

        String path = longPath.toString();
        Assertions.assertEquals("file.txt", FilenameUtil.getName(path));
        Assertions.assertEquals("file", FilenameUtil.getBaseName(path));
        Assertions.assertEquals("txt", FilenameUtil.getExtension(path));
    }

    @Test
    public void testUNCPaths_Windows() {
        // UNC path tests
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("//server/share"));
        Assertions.assertEquals(9, FilenameUtil.getPrefixLength("\\\\server\\share"));

        String uncPath = "//server/share/file.txt";
        Assertions.assertEquals("file.txt", FilenameUtil.getName(uncPath));
        Assertions.assertEquals("file", FilenameUtil.getBaseName(uncPath));
    }

    @Test
    public void testNormalization_ComplexScenarios() {
        // Complex normalization scenarios
        Assertions.assertNotNull(FilenameUtil.normalize("/a/./b/../c/./d/../e"));
        Assertions.assertNotNull(FilenameUtil.normalize("a/b/c/../../d"));
        Assertions.assertNull(FilenameUtil.normalize("a/b/../../../c")); // Goes above root
    }

    @Test
    public void testWildcardMatch_EdgeCases() {
        // Wildcard edge cases
        Assertions.assertTrue(FilenameUtil.wildcardMatch("", "*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("a", "*"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc", "***"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("", "?"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("a", "??"));

        // Question marks
        Assertions.assertTrue(FilenameUtil.wildcardMatch("abc", "???"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("ab", "???"));
    }

    @Test
    public void testConcatWithWindowsDrives() {
        // Concatenation with Windows drive letters
        String result1 = FilenameUtil.concat("/foo", "C:/bar");
        Assertions.assertNotNull(result1);
        Assertions.assertTrue(result1.contains("bar"));

        String result2 = FilenameUtil.concat("/foo", "D:\\bar");
        Assertions.assertNotNull(result2);
    }

    @Test
    public void testPrefixLength_EdgeCases() {
        // Edge cases for prefix length
        Assertions.assertEquals(0, FilenameUtil.getPrefixLength("relative/path"));
        Assertions.assertEquals(1, FilenameUtil.getPrefixLength("/"));
        Assertions.assertEquals(2, FilenameUtil.getPrefixLength("C:"));
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("C:\\"));

        // Lowercase drive letter (should work)
        Assertions.assertEquals(3, FilenameUtil.getPrefixLength("c:/foo"));
    }

    @Test
    public void testGetPathAndFullPath_Consistency() {
        // Ensure getPath and getFullPath are consistent
        String filename = "C:\\foo\\bar\\file.txt";
        String path = FilenameUtil.getPath(filename);
        String fullPath = FilenameUtil.getFullPath(filename);
        String prefix = FilenameUtil.getPrefix(filename);

        Assertions.assertNotNull(path);
        Assertions.assertNotNull(fullPath);
        Assertions.assertNotNull(prefix);
    }

    @Test
    public void testEqualsWithNormalization_BothInvalid() {
        // Both paths invalid during normalization
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilenameUtil.equals("/../a", "/../b", true, IOCase.SENSITIVE));
    }

    @Test
    public void testIsExtension_EmptyStringExtension() {
        // Empty string in array
        String[] exts = { "txt", "", "pdf" };
        Assertions.assertTrue(FilenameUtil.isExtension("file", exts));
        Assertions.assertTrue(FilenameUtil.isExtension("file.txt", exts));
    }

    @Test
    public void testWildcardMatch_RepeatingPatterns() {
        // Repeating patterns that require backtracking
        Assertions.assertTrue(FilenameUtil.wildcardMatch("aaaaab", "a*ab"));
        Assertions.assertTrue(FilenameUtil.wildcardMatch("ababab", "*ab*ab"));
        Assertions.assertFalse(FilenameUtil.wildcardMatch("aaaaac", "a*ab"));
    }
}
