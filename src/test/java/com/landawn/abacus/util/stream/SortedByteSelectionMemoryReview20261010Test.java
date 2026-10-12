package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
// The rank results themselves are covered in the regular suite by SortedByteSelectionReview20261010Test.
@Tag("slow-test")
class SortedByteSelectionMemoryReview20261010Test extends TestBase {
    @Test
    void largeSortedRankUsesBoundedMemory() {
        final java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
        assumeTrue(platformBean instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean allocationBean = (com.sun.management.ThreadMXBean) platformBean;
        assumeTrue(allocationBean.isThreadAllocatedMemorySupported() && allocationBean.isThreadAllocatedMemoryEnabled());

        // Warm up stream construction, OptionalByte and both selection paths before measuring this thread.
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) 42, constantBytes(4096).kthLargest(2049).get());
            assertEquals((byte) 42, constantBytes(16).kthLargest(1).get());
        }

        final long threadId = Thread.currentThread().threadId();
        final long before = allocationBean.getThreadAllocatedBytes(threadId);
        final byte result = constantBytes(1_000_000).kthLargest(1_000_000).get();
        final long allocated = allocationBean.getThreadAllocatedBytes(threadId) - before;

        assertEquals((byte) 42, result);
        // Leave ample room for runtime instrumentation; the old growing ring allocated over 2 MB.
        assertTrue(allocated < 128 * 1024, "large byte ranks must use bounded storage; allocated " + allocated + " bytes");
    }

    private static ByteStream constantBytes(final int count) {
        return new IteratorByteStream(new ByteIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                cursor++;
                return 42;
            }
        }, true, null);
    }
}
