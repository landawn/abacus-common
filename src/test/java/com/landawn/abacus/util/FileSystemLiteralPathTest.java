package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class FileSystemLiteralPathTest {
    @TempDir
    Path directory;

    @Test
    void windowsTreatsPercentExclamationAndCommandPunctuationAsFilenameData() throws Exception {
        Assumptions.assumeTrue(File.separatorChar == '\\');
        for (final String name : new String[] { "ordinary", "%TEMP%", "a!b&c", "space \u03B1" }) {
            final Path path = Files.createDirectory(directory.resolve(name));
            assertTrue(FileSystemUtil.freeSpaceKb(path.toString(), 5000) >= 0, name);
        }
        assertThrows(IllegalArgumentException.class, () -> FileSystemUtil.freeSpaceKb(directory + "\\bad\"name", 5000));
        assertThrows(java.io.IOException.class, () -> FileSystemUtil.freeSpaceKb(directory.resolve("missing").toString(), 5000));
    }

    @Test
    void unixLeadingDashRelativeOperandIsAPath() throws Exception {
        Assumptions.assumeTrue(File.separatorChar == '/');
        final Path relative = Path.of("-util-fs-" + java.util.UUID.randomUUID());
        Files.createDirectory(relative);
        try {
            assertTrue(FileSystemUtil.freeSpaceKb(relative.toString(), 5000) >= 0);
        } finally {
            Files.delete(relative);
        }
    }
}
