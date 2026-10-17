package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DeepToStringAllocationTest {
    @Test
    @Tag("slow-test")
    void shortElementsDoNotRequireAnOversizedRenderingBuffer() {
        final com.sun.management.ThreadMXBean allocations = allocationCounter();
        final Object[] values = new Object[200_000];
        Arrays.fill(values, "x");
        final String expected = "[" + "x, ".repeat(values.length - 1) + "x]";
        assertEquals(expected, N.deepToString(values));

        final long before = allocations.getCurrentThreadAllocatedBytes();
        final String rendered = N.deepToString(values);
        final long allocated = allocations.getCurrentThreadAllocatedBytes() - before;

        assertEquals(expected, rendered);
        // Allow both compact and UTF-16 builders while rejecting the old 6.4 MB initial reservation.
        assertTrue(allocated < 4_000_000, "Rendering short elements allocated " + allocated + " bytes");
    }

    @Test
    @Tag("slow-test")
    void earlyRenderingFailureDoesNotReserveSpaceForEveryRemainingElement() {
        final com.sun.management.ThreadMXBean allocations = allocationCounter();
        final IllegalStateException failure = new IllegalStateException("rendering failed");
        final Object failingValue = new Object() {
            @Override
            public String toString() {
                throw failure;
            }
        };
        final Object[] values = new Object[1_000_000];
        values[0] = failingValue;
        assertSame(failure, assertThrows(IllegalStateException.class, () -> N.deepToString(new Object[] { failingValue })));

        final long before = allocations.getCurrentThreadAllocatedBytes();
        final IllegalStateException actual = assertThrows(IllegalStateException.class, () -> N.deepToString(values));
        final long allocated = allocations.getCurrentThreadAllocatedBytes() - before;

        assertSame(failure, actual);
        assertTrue(allocated < 4_000_000, "Failing before the second element allocated " + allocated + " bytes");
    }

    @Test
    void nestedUnicodeValuesAndCyclesRemainIntactWhenTheBufferGrows() {
        final Object[] nested = { "\u4e2d".repeat(32), new int[] { 1, 2 } };
        final Object[] values = new Object[30_000];
        Arrays.fill(values, nested);
        values[values.length - 1] = values;

        assertEquals(Arrays.deepToString(values), N.deepToString(values));
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        final java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean, "per-thread allocation counters unavailable");
        final com.sun.management.ThreadMXBean allocations = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported() && allocations.isThreadAllocatedMemoryEnabled(),
                "per-thread allocation counters unavailable");
        return allocations;
    }
}
