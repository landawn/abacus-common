package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/** Temporary-file fixtures shared by Files contract tests. */
public abstract class FilesTestSupport extends TestBase {

    @TempDir
    Path tempDir;

    protected File testFile;

    protected Path testPath;

    @BeforeEach
    public void setUp() throws IOException {
        testFile = tempDir.resolve("test.txt").toFile();
        testPath = testFile.toPath();
        Files.writeString(testFile, "Line 1\nLine 2\nLine 3");
    }

    protected File file(String name) {
        return tempDir.resolve(name).toFile();
    }

    protected Path path(String name) {
        return tempDir.resolve(name);
    }

    protected File writeRawBytes(String name, byte[] bytes) throws IOException {
        File f = file(name);
        java.nio.file.Files.write(f.toPath(), bytes);
        return f;
    }

    protected static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    protected File writeBytes(final String name, final byte[] bytes) throws IOException {
        final File f = file(name);
        java.nio.file.Files.write(f.toPath(), bytes);
        return f;
    }

    protected static void assertInvalidPath(final Executable call) {
        final FileNotFoundException e = assertThrows(FileNotFoundException.class, call);
        assertInstanceOf(InvalidPathException.class, e.getCause());
        assertTrue(e.getMessage().startsWith("Invalid file path: "), e.getMessage());
    }

    /** Reads through java.io, which (unlike the {@code Path} parser) accepts an NTFS alternate-data-stream name. */
    protected static byte[] readViaStream(final File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            return in.readAllBytes();
        }
    }

    /**
     * Creates the NTFS alternate data stream {@code <host>:<streamName>} holding {@code content} through java.io, or
     * aborts the test when the file system does not support streams (a FAT/exFAT temp directory, a non-NTFS mount).
     */
    protected static File alternateDataStream(final File host, final String streamName, final byte[] content) {
        final File ads = new File(host.getPath() + ":" + streamName);

        try (FileOutputStream out = new FileOutputStream(ads)) {
            out.write(content);
        } catch (final IOException e) {
            Assumptions.abort("NTFS alternate data streams are not supported here: " + e);
        }

        Assumptions.assumeTrue(ads.isFile() && ads.exists(), "alternate data stream not visible through java.io");
        // precondition of the finding: java.io accepts the name, the Path parser does not
        assertThrows(InvalidPathException.class, ads::toPath);

        return ads;
    }
}
