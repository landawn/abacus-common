package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Code review 2026-10-05: {@code Objectory.recycle(List)} trimmed every list before pooling it, so recycling a list whose
 * capacity exceeded its size copied all its elements into a new array only to clear it, and the next use at a larger size
 * grew the array again: the pooled list no longer reused its storage. Only an empty list now releases its backing array
 * (which allocates nothing); a non-empty list is cleared and keeps its capacity.
 *
 * <p>The ArrayList capacity is not observable without opening {@code java.util}, so these tests observe it through the
 * bytes the current thread allocates.
 */
@Tag("unit")
@SuppressWarnings("deprecation") // createList/recycle are deprecated for external use; they are the subject under test
class ObjectoryReview20261005Test extends TestBase {
    private static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private Queue<List<?>> pool;
    private List<List<?>> saved;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void emptyTheListPool() throws ReflectiveOperationException {
        Assumptions.assumeTrue(THREADS.isThreadAllocatedMemorySupported() && THREADS.isThreadAllocatedMemoryEnabled(),
                "per-thread allocation counting is required");

        final Field poolField = Objectory.class.getDeclaredField("listPool");
        poolField.setAccessible(true);
        pool = (Queue<List<?>>) poolField.get(null);
        saved = new ArrayList<>(pool);
        pool.clear();

        // Resolve every call site once, so class initialization and linkage do not count as allocation below.
        final ArrayList<Object> warmUp = new ArrayList<>();
        warmUp.add(this);
        Objectory.recycle(warmUp);
        Objectory.recycle(Objectory.createList());
        Objectory.createList();
        THREADS.getCurrentThreadAllocatedBytes();
        pool.clear();
    }

    @AfterEach
    void restoreTheListPool() {
        if (pool != null) {
            pool.clear();
            pool.addAll(saved);
        }
    }

    private static long allocatedBy(final Runnable action) {
        final long overhead = -THREADS.getCurrentThreadAllocatedBytes() + THREADS.getCurrentThreadAllocatedBytes();
        final long before = THREADS.getCurrentThreadAllocatedBytes();
        action.run();
        return THREADS.getCurrentThreadAllocatedBytes() - before - overhead;
    }

    @Test
    void recyclingANonEmptyListNeitherCopiesNorShrinksIt() {
        final Object element = new Object();
        final ArrayList<Object> list = new ArrayList<>();

        for (int i = 0; i < 1_000; i++) {
            list.add(element); // grows the capacity to 1,234
        }

        final long recycleBytes = allocatedBy(() -> Objectory.recycle(list));
        assertTrue(recycleBytes < 1_024, "recycling must not copy the 1,000 elements, but allocated " + recycleBytes + " bytes");

        final List<Object> reused = Objectory.createList();
        assertSame(list, reused);
        assertTrue(reused.isEmpty());

        // The retained capacity takes 1,200 elements again without growing the array.
        final long refillBytes = allocatedBy(() -> {
            for (int i = 0; i < 1_200; i++) {
                reused.add(element);
            }
        });

        assertTrue(refillBytes < 1_024, "the pooled list should keep its capacity, but refilling it allocated " + refillBytes + " bytes");
        assertEquals(1_200, reused.size());
    }

    @Test
    void recyclingAnEmptyListReleasesItsBackingArray() {
        final ArrayList<Object> preallocated = new ArrayList<>(100_000);

        final long recycleBytes = allocatedBy(() -> Objectory.recycle(preallocated));
        assertTrue(recycleBytes < 1_024, "releasing an empty list's array must not allocate, but allocated " + recycleBytes + " bytes");

        final List<Object> reused = Objectory.createList();
        assertSame(preallocated, reused);

        // Had the 100,000-slot array stayed pooled, this first add would allocate nothing.
        final long firstAddBytes = allocatedBy(() -> reused.add(this));
        assertTrue(firstAddBytes > 0, "the empty list must not keep its 100,000-slot array in the pool");

        final ArrayList<Object> emptied = new ArrayList<>();

        for (int i = 0; i < 5_000; i++) {
            emptied.add(this);
        }

        emptied.clear();
        Objectory.recycle(emptied);
        final List<Object> reusedEmptied = Objectory.createList();
        assertSame(emptied, reusedEmptied);
        assertTrue(allocatedBy(() -> reusedEmptied.add(this)) > 0, "an emptied list must not keep its grown array in the pool");
    }

    @Test
    void ineligibleListsAreLeftUnchanged() {
        final List<Object> linked = new LinkedList<>(List.of("a", "b"));
        Objectory.recycle(linked);
        assertEquals(List.of("a", "b"), linked);

        final ArrayList<Object> oversized = new ArrayList<>();

        for (int i = 0; i <= 8_192; i++) {
            oversized.add(this);
        }

        Objectory.recycle(oversized);
        assertEquals(8_193, oversized.size(), "a list above the poolable size is neither cleared nor pooled");
        assertTrue(pool.isEmpty());

        Objectory.recycle((List<?>) null);
        assertTrue(pool.isEmpty());
    }
}
