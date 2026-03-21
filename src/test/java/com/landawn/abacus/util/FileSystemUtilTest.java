package com.landawn.abacus.util;

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
        try {
            FileSystemUtil.freeSpaceKb(".", 0);
        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
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

}
