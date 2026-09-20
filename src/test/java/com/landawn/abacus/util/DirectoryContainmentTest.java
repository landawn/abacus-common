package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class DirectoryContainmentTest extends TestBase {
    @Test
    void backslashOnlyCreatesABoundaryOnWindows() {
        final boolean windows = File.separatorChar == '\\';
        assertEquals(windows, FilenameUtil.directoryContains("/tmp/parent", "/tmp/parent\\sibling"));
        assertEquals(windows, FilenameUtil.directoryContains("/tmp/parent\\", "/tmp/parent\\sibling"));
        assertTrue(FilenameUtil.directoryContains("/tmp/parent\\", "/tmp/parent\\/child"));
        assertFalse(FilenameUtil.directoryContains("/tmp/parent", "/tmp/parentSibling"));
        assertTrue(FilenameUtil.directoryContains("/tmp/parent", "/tmp/parent/\uD83D\uDE00"));
        assertTrue(FilenameUtil.directoryContains("/tmp/parent/", "/tmp/parent/child"));
        assertTrue(FilenameUtil.directoryContains("/", "/child"));
    }

    @Test
    void existingNullEmptyAndEqualityPoliciesRemain() {
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.directoryContains(null, "x"));
        assertFalse(FilenameUtil.directoryContains("x", null));
        assertFalse(FilenameUtil.directoryContains("x", "x"));
        assertFalse(FilenameUtil.directoryContains("", ""));
        assertTrue(FilenameUtil.directoryContains("", "x"));
        assertFalse(FilenameUtil.directoryContains("xy", "x"));
    }
}
