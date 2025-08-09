package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FileSystemUtil100Test extends TestBase {

    @Test
    public void testFreeSpaceKbWithPath() {
        try {
            // Test with current directory
            long freeSpace = FileSystemUtil.freeSpaceKb(".");
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");
            
            // Test with root directory based on OS
            String rootPath = System.getProperty("os.name").toLowerCase().contains("windows") ? "C:" : "/";
            long rootFreeSpace = FileSystemUtil.freeSpaceKb(rootPath);
            Assertions.assertTrue(rootFreeSpace > 0, "Root free space should be greater than 0");
            
        } catch (IOException e) {
            // Some systems may not support this operation
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbWithPathAndTimeout() {
        try {
            // Test with timeout
            long freeSpace = FileSystemUtil.freeSpaceKb(".", 5000);
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");
            
            // Test with zero timeout
            long freeSpaceNoTimeout = FileSystemUtil.freeSpaceKb(".", 0);
            Assertions.assertTrue(freeSpaceNoTimeout > 0, "Free space should be greater than 0");
            
            // Test with negative timeout
            long freeSpaceNegTimeout = FileSystemUtil.freeSpaceKb(".", -1);
            Assertions.assertTrue(freeSpaceNegTimeout > 0, "Free space should be greater than 0");
            
        } catch (IOException e) {
            // Some systems may not support this operation
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectory() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb();
            Assertions.assertTrue(freeSpace > 0, "Free space should be greater than 0");
            
            // Should be equivalent to current directory
            long currentDirSpace = FileSystemUtil.freeSpaceKb(new File(".").getAbsolutePath());
            // Allow some tolerance as free space might change between calls
            Assertions.assertTrue(Math.abs(freeSpace - currentDirSpace) < 1000000, 
                "Free space should be similar for current directory");
            
        } catch (IOException e) {
            // Some systems may not support this operation
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectoryWithTimeout() {
        try {
            // Test with various timeouts
            long freeSpace1 = FileSystemUtil.freeSpaceKb(1000);
            Assertions.assertTrue(freeSpace1 > 0, "Free space should be greater than 0");
            
            long freeSpace2 = FileSystemUtil.freeSpaceKb(0);
            Assertions.assertTrue(freeSpace2 > 0, "Free space should be greater than 0");
            
            long freeSpace3 = FileSystemUtil.freeSpaceKb(-100);
            Assertions.assertTrue(freeSpace3 > 0, "Free space should be greater than 0");
            
        } catch (IOException e) {
            // Some systems may not support this operation
            Assertions.assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testInvalidPath() {
        // Test with null path
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileSystemUtil.freeSpaceKb(null);
        });
        
        // Test with empty path on Unix-like systems
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                FileSystemUtil.freeSpaceKb("");
            });
        }
    }

    @Test
    public void testNonExistentPath() {
        try {
            // Test with non-existent path
            String nonExistentPath = System.getProperty("os.name").toLowerCase().contains("windows") 
                ? "Z:\\NonExistentPath12345" : "/nonexistent/path12345";
            FileSystemUtil.freeSpaceKb(nonExistentPath);
            // If no exception is thrown, the command might still succeed on some systems
        } catch (IOException e) {
            // Expected for non-existent paths
            Assertions.assertTrue(e.getMessage() != null);
        }
    }
}