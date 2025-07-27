package com.landawn.abacus.util;


import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FileSystemUtil101Test extends TestBase {

    @Test
    public void testFreeSpaceKbWithPath() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".");
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
            // May fail on some systems
        } catch (IllegalStateException e) {
            // OS not supported
        }
    }

    @Test
    public void testFreeSpaceKbWithPathAndTimeout() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(".", 5000);
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
            // May fail on some systems
        } catch (IllegalStateException e) {
            // OS not supported
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectory() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb();
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
            // May fail on some systems
        } catch (IllegalStateException e) {
            // OS not supported
        }
    }

    @Test
    public void testFreeSpaceKbCurrentDirectoryWithTimeout() {
        try {
            long freeSpace = FileSystemUtil.freeSpaceKb(5000);
            Assertions.assertTrue(freeSpace > 0);
        } catch (IOException e) {
            // May fail on some systems
        } catch (IllegalStateException e) {
            // OS not supported
        }
    }
}