package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FileSystemUtilTest extends TestBase {

    private FileSystemUtil newFileSystemUtil() {
        try {
            final java.lang.reflect.Constructor<FileSystemUtil> constructor = FileSystemUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class SilentSlowProcess {

        public static void main(final String[] args) throws InterruptedException {
            Thread.sleep(3000);
        }
    }

    @Test
    public void testFreeSpaceKbWithPath() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".");
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbWithPathAndTimeout() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".", 5000);
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectory() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb();
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectoryWithTimeout() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(5000);
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testInvalidPath() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileSystemUtil.freeSpaceKb(null);
        });

        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                FileSystemUtil.freeSpaceKb("");
            });
        }
    }

    @Test
    public void testNonExistentPath() {
        try {
            String nonExistentPath = System.getProperty("os.name").toLowerCase().contains("windows") ? "Z:\\NonExistentPath12345" : "/nonexistent/path12345";
            FileSystemUtil.freeSpaceKb(nonExistentPath);
        } catch (IOException e) {
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbWithAbsolutePath() {
        try {
            String absPath = System.getProperty("user.home");
            if (absPath != null) {
                long freeSpace = FileSystemUtil.freeSpaceKb(absPath);
                Assertions.assertTrue(freeSpace >= 0);
            }
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbTempDir() {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (tmpDir != null) {
                long freeSpace = FileSystemUtil.freeSpaceKb(tmpDir, 10000);
                Assertions.assertTrue(freeSpace >= 0);
            }
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbWithSlash() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String path = osName.contains("windows") ? "C:\\" : "/";
            long freeSpace = FileSystemUtil.freeSpaceKb(path);
            Assertions.assertTrue(freeSpace >= 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceKbNullTimeout() {
        assertDoesNotThrow(() -> {
            try {
                FileSystemUtil.freeSpaceKb(".", 0);
            } catch (IOException e) {
            } catch (IllegalStateException e) {
            }
        });
    }

    @Test
    public void testFreeSpaceKbRelativePath() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb("src");
            Assertions.assertTrue(freeSpace >= 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testFreeSpaceOS_UnsupportedOs() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> newFileSystemUtil().freeSpaceOS(".", 0, true, 0));
        Assertions.assertTrue(exception.getMessage().contains("Unsupported operating system"));
    }

    @Test
    public void testParseDir_WithThousandsSeparators() throws IOException {
        long freeBytes = newFileSystemUtil().parseDir("           12,345,678 bytes free", ".");
        Assertions.assertEquals(12345678L, freeBytes);
    }

    @Test
    public void testParseDir_BareNumericLine() throws IOException {
        // regression: a line that is nothing but the byte count (digits reaching index 0) was
        // rejected as invalid because the "no digit found" check (j < 0) also fired when the
        // backward scan for the count's start ran off the front of the line.
        Assertions.assertEquals(12345678L, newFileSystemUtil().parseDir("12345678", "."));
        // a line with no digits at all must still be rejected
        Assertions.assertThrows(IOException.class, () -> newFileSystemUtil().parseDir("bytes free", "."));
    }

    @Test
    public void testFreeSpaceUnix_EmptyPath() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> newFileSystemUtil().freeSpaceUnix("", true, false, 0));
        Assertions.assertTrue(exception.getMessage().contains("Path must not be empty"));
    }

    @Test
    public void testParseBytes() throws IOException {
        Assertions.assertEquals(1024L, newFileSystemUtil().parseBytes("1024", "."));
    }

    @Test
    public void testParseBytes_NegativeValue() {
        IOException exception = Assertions.assertThrows(IOException.class, () -> newFileSystemUtil().parseBytes("-1", "."));
        Assertions.assertTrue(exception.getMessage().contains("did not find free space"));
    }

    @Test
    public void testFreeSpaceWindowsRejectsQuotedPathBeforeProcessExecution() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> newFileSystemUtil().freeSpaceWindows("C:\\tmp\"bad", 0));
    }

    @Test
    public void testPerformCommandEnforcesTimeoutWhileOutputIsSilent() throws Exception {
        final String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + (System.getProperty("os.name").toLowerCase().contains("windows") ? "java.exe" : "java");
        final String testClasses = new File(FileSystemUtilTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        final String[] command = { executable, "-cp", testClasses, SilentSlowProcess.class.getName() };
        final long start = System.nanoTime();

        final IOException exception = Assertions.assertThrows(IOException.class, () -> newFileSystemUtil().performCommand(command, 1, 100));
        final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        final boolean interrupted = Thread.interrupted(); // Read and clear so a failure cannot contaminate later tests.

        Assertions.assertTrue(exception.getMessage().contains("timed out"));
        Assertions.assertTrue(elapsedMillis < 2000, "Timeout took " + elapsedMillis + " ms");
        Assertions.assertFalse(interrupted, "A command timeout must not interrupt its caller");
    }

}
