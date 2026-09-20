package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class CharacterWriterSliceTest extends TestBase {
    @Test
    void stringSlicesMatchCharacterArrayEscapingIncludingUtf16Boundaries() throws Exception {
        for (final String input : List.of("", "plain", "\"\\\n", "\uD83D\uDE00", "\uD800x\uDC00", "a<b>&c")) {
            for (int offset = 0; offset <= input.length(); offset++) {
                for (int length = 0; length <= input.length() - offset; length++) {
                    try (final var actual = new BufferedJsonWriter();
                         final var expected = new BufferedJsonWriter()) {
                        actual.writeCharacter(input, offset, length);
                        expected.writeCharacter(input.toCharArray(), offset, length);
                        assertEquals(expected.toString(), actual.toString());
                    }
                }
            }
        }
        try (final var writer = new BufferedJsonWriter()) {
            writer.writeCharacter((String) null);
            writer.writeCharacter((String) null, 1, 2);
            assertEquals("nullul", writer.toString());
            assertThrows(IndexOutOfBoundsException.class, () -> writer.writeCharacter("x", -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> writer.writeCharacter("x", 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> writer.writeCharacter("x", 1, Integer.MAX_VALUE));
        }
        final var closed = new BufferedJsonWriter();
        closed.close();
        assertThrows(IOException.class, () -> closed.writeCharacter("", 0, 0));
    }

    @Test
    void tinySlicesDoNotAllocateCopiesOfTheFullSource() throws Exception {
        final var bean = ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final var allocations = (com.sun.management.ThreadMXBean) bean;
        Assumptions.assumeTrue(allocations.isThreadAllocatedMemorySupported() && allocations.isThreadAllocatedMemoryEnabled());
        final String source = "x".repeat(1_000_000);
        try (final var writer = new BufferedJsonWriter()) {
            for (int i = 0; i < 100; i++) {
                writer.writeCharacter(source, 999999, 1);
            }
            final long thread = Thread.currentThread().threadId();
            final long before = allocations.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 64; i++) {
                writer.writeCharacter(source, 999999, 1);
            }
            final long allocated = allocations.getThreadAllocatedBytes(thread) - before;
            assertTrue(allocated < 1_000_000, "Tiny slices allocated " + allocated + " bytes");
            assertEquals(164, writer.toString().length());
        }
    }
}
