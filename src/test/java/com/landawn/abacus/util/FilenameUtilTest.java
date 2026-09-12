package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FilenameUtilTest extends TestBase {

    @Test
    public void testEmptyStringHandling() {
        String empty = "";
        assertEquals("", FilenameUtil.normalize(empty));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator(empty));
        assertEquals(0, FilenameUtil.getPrefixLength(empty));
        assertEquals(-1, FilenameUtil.indexOfLastSeparator(empty));
        assertEquals(-1, FilenameUtil.indexOfExtension(empty));
        assertEquals("", FilenameUtil.getPrefix(empty));
        assertEquals("", FilenameUtil.getPath(empty));
        assertEquals("", FilenameUtil.getPathNoEndSeparator(empty));
        assertEquals("", FilenameUtil.getFullPath(empty));
        assertEquals("", FilenameUtil.getFullPathNoEndSeparator(empty));
        assertEquals("", FilenameUtil.getName(empty));
        assertEquals("", FilenameUtil.getBaseName(empty));
        assertEquals("", FilenameUtil.getExtension(empty));
        assertEquals("", FilenameUtil.removeExtension(empty));
        assertEquals(".txt", FilenameUtil.changeExtension(empty, "txt"));
        assertEquals("", FilenameUtil.separatorsToUnix(empty));
        assertEquals("", FilenameUtil.separatorsToWindows(empty));
        assertEquals("", FilenameUtil.separatorsToSystem(empty));
    }

    @Test
    public void testNullByteRejected() {
        String poisonedFilename = "report" + '\0' + ".txt";
        String poisonedPath = "safe" + File.separator + "bad" + '\0' + File.separator + "report.txt";

        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize("foo" + '\0' + "bar"));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(poisonedFilename));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getBaseName(poisonedFilename));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.removeExtension(poisonedFilename));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.indexOfExtension(poisonedFilename));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getExtension(poisonedFilename));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(poisonedFilename, "txt"));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(poisonedFilename, new String[] { "txt" }));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(poisonedFilename, Arrays.asList("txt")));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.changeExtension("report.txt", "log" + '\0'));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPath(poisonedPath));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPathNoEndSeparator(poisonedPath));
    }

    @Test
    public void testDirectoryContains() {
        assertTrue(FilenameUtil.directoryContains("/Users/john", "/Users/john/documents"));
        assertTrue(FilenameUtil.directoryContains("/a/b", "/a/b/c/d/e"));
        assertEquals(java.io.File.separatorChar == '\\', FilenameUtil.directoryContains("C:\\Users", "C:\\Users\\john"));
        assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/jane"));
        assertFalse(FilenameUtil.directoryContains("/a/b/c", "/a/b"));
        assertFalse(FilenameUtil.directoryContains("/Users/john", "/Users/john"));
        assertFalse(FilenameUtil.directoryContains("/Users/john", null));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.directoryContains(null, "/Users/john"));

        if (IOUtil.IS_OS_WINDOWS) {
            assertTrue(FilenameUtil.directoryContains("C:\\Users\\JOHN", "C:\\Users\\john\\docs"));
        } else {
            assertFalse(FilenameUtil.directoryContains("/Users/JOHN", "/Users/john/docs"));
        }
    }

    @Test
    public void testChangeExtension() {
        assertEquals("file.log", FilenameUtil.changeExtension("file.txt", "log"));
        assertEquals("file.log", FilenameUtil.changeExtension("file.txt", ".log"));
        assertEquals("a/b/c.md", FilenameUtil.changeExtension("a/b/c.txt", "md"));
        assertEquals("file", FilenameUtil.changeExtension("file.txt", ""));
        assertEquals("file.txt", FilenameUtil.changeExtension("file", "txt"));
        assertEquals("archive.tar.gz", FilenameUtil.changeExtension("archive.tar.bz2", "gz"));
        assertNull(FilenameUtil.changeExtension(null, "txt"));
    }

    @Test
    public void testGetName_EdgeCase() {
        assertEquals("", FilenameUtil.getName("a/b/c/"));
        assertEquals("", FilenameUtil.getName("a/b/c\\"));
        assertEquals("file.txt", FilenameUtil.getName("C:\\foo\\bar\\file.txt"));
        assertEquals("file.txt", FilenameUtil.getName("C:/foo/bar/file.txt"));
        assertEquals(".gitignore", FilenameUtil.getName(".gitignore"));
        assertEquals("", FilenameUtil.getBaseName(".gitignore"));
        assertEquals("gitignore", FilenameUtil.getExtension(".gitignore"));
        assertEquals("gz", FilenameUtil.getExtension("archive.tar.gz"));
        assertEquals("archive.tar", FilenameUtil.getBaseName("archive.tar.gz"));
        assertEquals("", FilenameUtil.getExtension("README"));
        assertEquals("", FilenameUtil.getExtension("a.b/c"));
        assertEquals("", FilenameUtil.getExtension("a.b\\c"));
    }

    @Test
    public void testConcat_EdgeCase() {
        assertEquals(FilenameUtil.separatorsToSystem("/bar"), FilenameUtil.concat("/foo", "/bar"));
        assertEquals(FilenameUtil.separatorsToSystem("/foo/bar"), FilenameUtil.concat("/foo", "bar"));
        assertEquals(FilenameUtil.separatorsToSystem("/foo/bar"), FilenameUtil.concat("/foo/", "bar"));
        assertNull(FilenameUtil.concat("/foo/", "../../bar"));
        assertNull(FilenameUtil.concat(null, null));
    }

    @Test
    public void testNormalize_EdgeCase() {
        assertNull(FilenameUtil.normalize("/../"));
        assertNull(FilenameUtil.normalize("../foo"));
        assertEquals(FilenameUtil.separatorsToSystem("/"), FilenameUtil.normalize("/foo/bar/../.."));
        assertEquals("C:/foo/bar/baz", FilenameUtil.normalize("C:\\foo\\bar\\baz", true));
        assertEquals("\\foo\\bar", FilenameUtil.normalize("/foo/./bar", false));
        assertEquals("C:/foo/bar", FilenameUtil.normalizeNoEndSeparator("C:\\foo\\bar\\", true));
        assertEquals("\\foo\\bar", FilenameUtil.normalizeNoEndSeparator("/foo/bar/", false));
    }

    @Test
    public void testSeparatorsToUnix() {
        assertEquals("C:/Program Files/Java", FilenameUtil.separatorsToUnix("C:\\Program Files\\Java"));
        String unix = "/foo/bar/baz";
        assertSame(unix, FilenameUtil.separatorsToUnix(unix));
        assertNull(FilenameUtil.separatorsToUnix(null));
        assertEquals("C:\\foo\\bar", FilenameUtil.separatorsToWindows("C:/foo/bar"));
        assertNull(FilenameUtil.separatorsToWindows(null));
        assertNull(FilenameUtil.separatorsToSystem(null));
    }

    @Test
    public void testGetPrefixLength_TildeWithoutSeparator() {
        assertEquals("~user".length() + 1, FilenameUtil.getPrefixLength("~user"));
        assertEquals("~username".length() + 1, FilenameUtil.getPrefixLength("~username"));
        assertEquals(-1, FilenameUtil.getPrefixLength("//"));
        assertEquals(-1, FilenameUtil.getPrefixLength("//server"));
        assertEquals(9, FilenameUtil.getPrefixLength("//server/share"));
    }

    @Test
    public void testWildcardMatch() {
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.txt"));
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt"));
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "file.*"));
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "*"));
        assertTrue(FilenameUtil.wildcardMatch("cat", "c?t"));
        assertFalse(FilenameUtil.wildcardMatch("cat", "c??t"));
        assertTrue(FilenameUtil.wildcardMatch("file123.txt", "file*.tx?"));
        assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/b/*"));
        assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/*/c.txt"));
        assertTrue(FilenameUtil.wildcardMatch("file.txt.bak", "file*"));
        assertTrue(FilenameUtil.wildcardMatch("abcccd", "*ccd"));
        assertTrue(FilenameUtil.wildcardMatch("mississipissippi", "*issip*"));
        assertTrue(FilenameUtil.wildcardMatch("abcabczzzde", "*abc*de"));
        assertTrue(FilenameUtil.wildcardMatch("aaabbbaaabbb", "*aaa*bbb"));
        assertTrue(FilenameUtil.wildcardMatch("aaaaab", "a*ab"));
        assertFalse(FilenameUtil.wildcardMatch("aaaaac", "a*ab"));
        assertTrue(FilenameUtil.wildcardMatch("abcdefghi", "a*c*f*i"));
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "****.txt"));
        assertTrue(FilenameUtil.wildcardMatch(null, null));
        assertFalse(FilenameUtil.wildcardMatch("file.txt", null));
        assertFalse(FilenameUtil.wildcardMatch(null, "*.txt"));
        assertTrue(FilenameUtil.wildcardMatch("", ""));
        assertTrue(FilenameUtil.wildcardMatch("", "*"));
        assertFalse(FilenameUtil.wildcardMatch("", "?"));
        assertFalse(FilenameUtil.wildcardMatch("file", ""));
    }

    @Test
    public void testWildcardMatch_CaseSensitivity() {
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", IOCase.SENSITIVE));
        assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.SENSITIVE));
        assertTrue(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.INSENSITIVE));
        assertTrue(FilenameUtil.wildcardMatch("file.txt", "*.txt", null));
        assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", null));
        assertTrue(FilenameUtil.wildcardMatch(null, null, IOCase.SENSITIVE));
        assertFalse(FilenameUtil.wildcardMatch("file", null, IOCase.SENSITIVE));

        if (IOUtil.IS_OS_WINDOWS) {
            assertTrue(FilenameUtil.wildcardMatchOnSystem("FILE.TXT", "*.txt"));
        } else {
            assertFalse(FilenameUtil.wildcardMatchOnSystem("FILE.TXT", "*.txt"));
            assertTrue(FilenameUtil.wildcardMatchOnSystem("file.txt", "*.txt"));
        }
        assertTrue(FilenameUtil.wildcardMatchOnSystem(null, null));
        assertFalse(FilenameUtil.wildcardMatchOnSystem("file", null));
    }

    @Test
    public void testIsExtension_EdgeCase() {
        assertTrue(FilenameUtil.isExtension("file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("file.txt", "doc"));
        assertTrue(FilenameUtil.isExtension("file", ""));
        assertFalse(FilenameUtil.isExtension("file.txt", (String) null));
        assertTrue(FilenameUtil.isExtension("file", (String) null));
        assertFalse(FilenameUtil.isExtension(null, "txt"));
        assertTrue(FilenameUtil.isExtension("file.txt", new String[] { "doc", "txt" }));
        assertFalse(FilenameUtil.isExtension("file.txt", new String[0]));
        assertFalse(FilenameUtil.isExtension(null, new String[] { "txt" }));
        Collection<String> exts = Arrays.asList("txt", "doc");
        assertTrue(FilenameUtil.isExtension("file.txt", exts));
        assertFalse(FilenameUtil.isExtension("file.pdf", exts));
        assertFalse(FilenameUtil.isExtension("file.txt", Collections.emptyList()));
        assertFalse(FilenameUtil.isExtension(null, exts));
    }

    @Test
    public void testEquals_EdgeCase() {
        assertTrue(FilenameUtil.equals(null, null));
        assertFalse(FilenameUtil.equals("a", null));
        assertTrue(FilenameUtil.equals("a/b", "a/b"));
        assertTrue(FilenameUtil.equalsNormalized("a/./b", "a/b"));
        assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT"));
        assertNotNull(Boolean.valueOf(FilenameUtil.equalsOnSystem("FILE.TXT", "file.txt")));
        assertNotNull(Boolean.valueOf(FilenameUtil.equalsNormalizedOnSystem("a/./b", "a/b")));
        assertTrue(FilenameUtil.equals("a/b", "a/b", false, IOCase.SENSITIVE));
        assertTrue(FilenameUtil.equals("A/B", "a/b", false, IOCase.INSENSITIVE));
        assertTrue(FilenameUtil.equals("a/./b", "a/b", true, null));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.equals("a/../../c", "c", true, IOCase.SENSITIVE));
    }

    // Finding 135 (2026-09-08): getPrefixLength already rejects "." and ".." as a UNC hostname (the
    // CVE-2021-29425 traversal vector), but the worked-example block and the @return prose never mentioned it.
    // Pin the examples the javadoc now lists, in both separator styles.
    @Test
    public void reviewFixes20260908_uncHostDotAndDotDotAreInvalidPrefixes() {
        assertEquals(-1, FilenameUtil.getPrefixLength("//./a/b/c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("//../a/b/c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\.\\a\\b\\c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\..\\a\\b\\c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("//../foo"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\.\\foo"));

        // A real hostname is still accepted, and one that merely contains dots is not otherwise validated.
        assertEquals(9, FilenameUtil.getPrefixLength("//server/a/b/c.txt"));
        assertEquals(6, FilenameUtil.getPrefixLength("//.../foo"));

        // normalize / getPrefix / getFullPath all route through getPrefixLength, so they reject it too.
        assertNull(FilenameUtil.normalize("//../foo"));
        assertNull(FilenameUtil.getPrefix("//../foo"));
        assertNull(FilenameUtil.getFullPath("//../foo"));
        assertNull(FilenameUtil.getPath("//../foo"));
    }

    /**
     * A doubled separator immediately after the prefix must be merged like any other doubled separator.
     *
     * <p>The adjoining-slash pass tests the pair {@code (i-1, i)}, and used to start at {@code prefix + 1}, so the
     * pair straddling the end of the prefix was never examined.</p>
     */
    @Test
    public void testNormalize_DoubledSeparatorRightAfterPrefix() {
        assertEquals(FilenameUtil.separatorsToSystem("/a/b"), FilenameUtil.normalize("/a//b"));
        assertEquals(FilenameUtil.separatorsToSystem("C:/a"), FilenameUtil.normalize("C://a"));
        assertEquals(FilenameUtil.separatorsToSystem("C:/"), FilenameUtil.normalize("C://"));
        assertEquals(FilenameUtil.separatorsToSystem("~/"), FilenameUtil.normalize("~//"));
        assertEquals(FilenameUtil.separatorsToSystem("a/b"), FilenameUtil.normalize("a//b"));
    }

    /**
     * A {@code ..} that escapes the root must be rejected whether or not a doubled separator precedes it.
     *
     * <p>With the doubled separator left unmerged, the {@code ..} pass no longer saw it at {@code prefix + 2} and
     * so absorbed it into the prefix instead of returning {@code null}: {@code normalize("C://../a")} answered
     * {@code "C:/a"} while {@code normalize("C:/../a")} correctly answered {@code null}.</p>
     */
    @Test
    public void testNormalize_RootEscapeAfterDoubledSeparator() {
        assertNull(FilenameUtil.normalize("C:/../a"));
        assertNull(FilenameUtil.normalize("C://../a"));
        assertNull(FilenameUtil.normalize("/../a"));
        assertNull(FilenameUtil.normalize("~/../a"));
        assertNull(FilenameUtil.normalize("~//../a"));
    }

    /** A leading UNC {@code //} is a prefix, not a doubled separator, and must survive. */
    @Test
    public void testNormalize_UncPrefixIsNotMerged() {
        assertEquals(FilenameUtil.separatorsToSystem("//server/share/x"), FilenameUtil.normalize("//server/share/x"));
        assertEquals(FilenameUtil.separatorsToSystem("//server/share/x"), FilenameUtil.normalize("//server/share//x"));
    }

    /**
     * The sibling entry points share {@code doNormalize}, so they inherit the same correction.
     *
     * <p>Worth pinning separately: the fix is one line inside the shared private routine, and these four public
     * methods are the only way callers reach it.</p>
     */
    @Test
    public void testNormalize_SiblingEntryPointsAgreeAfterPrefix() {
        assertEquals(FilenameUtil.separatorsToSystem("C:/a"), FilenameUtil.normalizeNoEndSeparator("C://a"));
        assertNull(FilenameUtil.normalizeNoEndSeparator("C://../a"));

        assertEquals("C:/a", FilenameUtil.normalize("C://a", true));
        assertEquals("C:" + '\\' + "a", FilenameUtil.normalize("C://a", false));
        assertEquals("C:/a", FilenameUtil.normalizeNoEndSeparator("C://a", true));
    }

    /**
     * The {@code <separator>:} special case must not depend on WHICH separator spells the leading slash:
     * {@code doNormalize} reads the prefix from the raw text and only afterwards rewrites one separator into the
     * other, so the two spellings are the same path.
     */
    @Test
    public void testGetPrefixLength_LeadingSeparatorBeforeColon() {
        assertEquals(1, FilenameUtil.getPrefixLength("/:a"));
        assertEquals(1, FilenameUtil.getPrefixLength("\\:a"));
        assertEquals(1, FilenameUtil.getPrefixLength("/:"));
        assertEquals(1, FilenameUtil.getPrefixLength("\\:"));

        assertEquals("/", FilenameUtil.getPrefix("/:a"));
        assertEquals("\\", FilenameUtil.getPrefix("\\:a"));

        // unrelated shapes keep their verdicts
        assertEquals(3, FilenameUtil.getPrefixLength("C:\\a"));
        assertEquals(2, FilenameUtil.getPrefixLength("C:a"));
        assertEquals(-1, FilenameUtil.getPrefixLength("1:\\a"));
        assertEquals(-1, FilenameUtil.getPrefixLength(":"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\.\\foo"));
    }

    /**
     * Pins the divergence from Apache Commons IO that {@link FilenameUtil#getPrefixLength(String)} now documents.
     * Commons accepts the {@code '/'} spelling of {@code <separator>:} and rejects the {@code '\'} one, so
     * normalizing the spelling it accepts produces the spelling it rejects; this class answers 1 for both.
     */
    @Test
    public void testGetPrefixLength_LeadingSeparatorBeforeColon_divergesFromCommonsIo() {
        assertEquals(1, FilenameUtils.getPrefixLength("/:a"), "commons-io changed: re-check the getPrefixLength javadoc");
        assertEquals(-1, FilenameUtils.getPrefixLength("\\:a"), "commons-io changed: re-check the getPrefixLength javadoc");

        assertEquals(1, FilenameUtil.getPrefixLength("/:a"));
        assertEquals(1, FilenameUtil.getPrefixLength("\\:a"));

        // why the commons pair of verdicts cannot hold: normalization rewrites the accepted spelling into the
        // rejected one, so normalize is not idempotent there and equalsNormalized is not even reflexive
        assertEquals("\\:a", FilenameUtils.normalize("/:a", false));
        assertNull(FilenameUtils.normalize("\\:a", false));
        assertFalse(FilenameUtils.equalsNormalized("\\:a", "\\:a"));

        // this class is self-consistent instead: both spellings normalize alike and compare equal
        assertEquals("\\:a", FilenameUtil.normalize("/:a", false));
        assertEquals("\\:a", FilenameUtil.normalize("\\:a", false));
        assertTrue(FilenameUtil.equalsNormalized("\\:a", "\\:a"));
        assertTrue(FilenameUtil.equalsNormalized("/:a", "\\:a"));
    }

    /**
     * Before the fix {@code equalsNormalized} on the two spellings of {@code <separator>:} did not merely disagree,
     * it THREW, because one of them normalized to {@code null}.
     */
    @Test
    public void testNormalize_LeadingSeparatorBeforeColon() {
        final String expected = FilenameUtil.separatorsToSystem("/:a");
        assertEquals(expected, FilenameUtil.normalize("/:a"));
        assertEquals(expected, FilenameUtil.normalize("\\:a"));
        assertEquals("/:a", FilenameUtil.normalize("\\:a", true));
        assertEquals("\\:a", FilenameUtil.normalize("/:a", false));
        assertTrue(FilenameUtil.equalsNormalized("/:a", "\\:a"));
    }

    /**
     * Contract pin for the documented asymmetry of {@code getFullPathNoEndSeparator}: when the filename has a
     * name part, a prefix-terminating separator is dropped like any other trailing separator, so among those
     * only a bare leading separator survives; a filename that is nothing but a prefix is instead returned
     * unchanged. This matches Apache Commons IO byte-for-byte, and the javadoc now says so.
     */
    @Test
    public void testGetFullPathNoEndSeparator_DropsPrefixTerminatingSeparator() {
        assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:\\a"));
        assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:/a"));
        assertEquals("~", FilenameUtil.getFullPathNoEndSeparator("~/a"));
        assertEquals("~user", FilenameUtil.getFullPathNoEndSeparator("~user/a"));
        assertEquals("//host", FilenameUtil.getFullPathNoEndSeparator("//host/a"));
        assertEquals("/", FilenameUtil.getFullPathNoEndSeparator("/a"));

        // the UNC answer is not itself a valid filename: getPrefixLength rejects it and normalize returns null
        assertEquals(-1, FilenameUtil.getPrefixLength("//host"));
        assertNull(FilenameUtil.normalize("//host"));

        // getFullPath keeps the root separator in every one of those cases
        assertEquals("C:\\", FilenameUtil.getFullPath("C:\\a"));
        assertEquals("C:/", FilenameUtil.getFullPath("C:/a"));
        assertEquals("~/", FilenameUtil.getFullPath("~/a"));
        assertEquals("~user/", FilenameUtil.getFullPath("~user/a"));
        assertEquals("//host/", FilenameUtil.getFullPath("//host/a"));
        assertEquals("/", FilenameUtil.getFullPath("/a"));

        // a filename that is NOTHING BUT a prefix has no name part: doGetFullPath returns it unchanged, so the
        // prefix-terminating separator is kept here even without the separator - the javadoc example
        // getFullPathNoEndSeparator("C:\\") -> "C:\\" is this case, not an inconsistency
        assertEquals("C:\\", FilenameUtil.getFullPathNoEndSeparator("C:\\"));
        assertEquals("C:/", FilenameUtil.getFullPathNoEndSeparator("C:/"));
        assertEquals("~/", FilenameUtil.getFullPathNoEndSeparator("~/"));
        assertEquals("//host/", FilenameUtil.getFullPathNoEndSeparator("//host/"));
        assertEquals("/", FilenameUtil.getFullPathNoEndSeparator("/"));
        assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:"));
    }
}
