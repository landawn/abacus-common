package com.landawn.abacus.util;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FileSystemUtil101Test extends TestBase {

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
}
