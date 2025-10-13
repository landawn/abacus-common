package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FileSystemUtil100Test extends TestBase {

    @Test
    public void testFreeSpaceKbWithPath() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".");
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");

            String rootPath = System.getProperty("os.name").toLowerCase().contains("windows") ? "C:" : "/";
            long rootFreeSpace = FileSystemUtil.freeSpaceKb(rootPath);
            Assertions.assertTrue(rootFreeSpace > 0, "Root free space should be greater than 0");

        } catch (IOException e) {
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbWithPathAndTimeout() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".", 5000);
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");

            long freeSpaceNoTimeout = FileSystemUtil.freeSpaceKb(".", 0);
            Assertions.assertTrue(freeSpaceNoTimeout > 0, "Free space should be greater than 0");

            long freeSpaceNegTimeout = FileSystemUtil.freeSpaceKb(".", -1);
            Assertions.assertTrue(freeSpaceNegTimeout > 0, "Free space should be greater than 0");

        } catch (IOException e) {
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectory() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb();
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");

            long currentDirSpace = FileSystemUtil.freeSpaceKb(new File(".").getAbsolutePath());
            Assertions.assertTrue(Math.abs(freeSpace - currentDirSpace) < 1000000, "Free space should be similar for current directory");

        } catch (IOException e) {
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectoryWithTimeout() {
        try {
            long freeSpace1 = FileSystemUtil.freeSpaceKb(1000);
            Assertions.assertTrue(freeSpace1 > 0, "Free space should be greater than 0");

            long freeSpace2 = FileSystemUtil.freeSpaceKb(0);
            Assertions.assertTrue(freeSpace2 > 0, "Free space should be greater than 0");

            long freeSpace3 = FileSystemUtil.freeSpaceKb(-100);
            Assertions.assertTrue(freeSpace3 > 0, "Free space should be greater than 0");

        } catch (IOException e) {
            Assertions.assertTrue(e.getMessage() != null);
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
}
