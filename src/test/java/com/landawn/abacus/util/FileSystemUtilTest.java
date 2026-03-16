package com.landawn.abacus.util;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FileSystemUtilTest extends TestBase {

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

}
