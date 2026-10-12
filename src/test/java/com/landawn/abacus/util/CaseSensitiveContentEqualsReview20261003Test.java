package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
@Timeout(30)
class CaseSensitiveContentEqualsReview20261003Test extends TestBase {
    @TempDir
    Path temporaryDirectory;

    static Stream<Arguments> contents() {
        return Stream.of(Arguments.of("left", "else", false, false), Arguments.of("short", "longer", false, false),
                Arguments.of("a\nb\n", "a\r\nb\r\n", false, true), Arguments.of("same", "same", true, true), Arguments.of("", "x", false, false),
                Arguments.of("", "", true, true));
    }

    @ParameterizedTest
    @MethodSource("contents")
    void caseDistinctFilesAreComparedByContents(final String first, final String second, final boolean bytesEqual, final boolean linesEqual) throws Exception {
        if (IOUtil.IS_OS_WINDOWS) {
            final Process process = new ProcessBuilder("fsutil", "file", "setCaseSensitiveInfo", temporaryDirectory.toAbsolutePath().toString(), "enable")
                    .redirectErrorStream(true)
                    .start();
            try {
                assumeTrue(process.waitFor(10, TimeUnit.SECONDS), "fsutil did not finish");
                final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assumeTrue(process.exitValue() == 0, "case-sensitive directories unavailable: " + output);
            } finally {
                process.destroy();
            }
        }
        final File upper = temporaryDirectory.resolve("Case.txt").toFile();
        final File lower = temporaryDirectory.resolve("case.txt").toFile();
        Files.writeString(upper.toPath(), first);
        Files.writeString(lower.toPath(), second);
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assumeTrue(files.count() == 2, "filesystem is not case sensitive");
        }
        assertEquals(first, Files.readString(upper.toPath()));
        assertEquals(second, Files.readString(lower.toPath()));
        assertEquals(bytesEqual, IOUtil.contentEquals(upper, lower));
        assertEquals(bytesEqual, IOUtil.contentEquals(lower, upper));
        assertEquals(linesEqual, IOUtil.contentEqualsIgnoreEOL(upper, lower, "UTF-8"));
        assertEquals(linesEqual, IOUtil.contentEqualsIgnoreEOL(lower, upper, null));
    }

    @Test
    void identicalCanonicalPathAndDifferentNamesStillCompareCorrectly() throws Exception {
        final File first = temporaryDirectory.resolve("first.txt").toFile();
        final File second = temporaryDirectory.resolve("second.txt").toFile();
        Files.writeString(first.toPath(), "same\n");
        Files.writeString(second.toPath(), "same\n");
        final Path child = Files.createDirectory(temporaryDirectory.resolve("child"));
        final File alias = child.resolve("..").resolve("first.txt").toFile();
        assertTrue(IOUtil.contentEquals(first, alias));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(first, alias, "UTF-8"));
        assertTrue(IOUtil.contentEquals(first, second));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(first, second, "UTF-8"));
        Files.writeString(second.toPath(), "else\n");
        assertFalse(IOUtil.contentEquals(first, second));
        assertFalse(IOUtil.contentEqualsIgnoreEOL(first, second, "UTF-8"));
    }

    @Test
    void nullMissingDirectoryAndCharsetPrecedenceArePreserved() {
        final File missing = temporaryDirectory.resolve("missing").toFile();
        final File otherMissing = temporaryDirectory.resolve("other-missing").toFile();
        assertTrue(IOUtil.contentEquals((File) null, null));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(null, null, "UTF-8"));
        assertFalse(IOUtil.contentEquals(missing, null));
        assertFalse(IOUtil.contentEqualsIgnoreEOL(missing, null, "UTF-8"));
        assertTrue(IOUtil.contentEquals(missing, otherMissing));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(missing, otherMissing, "UTF-8"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEquals(temporaryDirectory.toFile(), temporaryDirectory.toFile()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEqualsIgnoreEOL(temporaryDirectory.toFile(), null, "no-such-charset"));
        assertThrows(UnsupportedCharsetException.class, () -> IOUtil.contentEqualsIgnoreEOL(missing, missing, "no-such-charset"));
    }
}
