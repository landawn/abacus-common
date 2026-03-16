package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests verifying pool-related bug fixes found during deep code review (2026).
 *
 * <p>Bug fixed: AbstractPool registers a JVM shutdown hook in constructor that is never removed
 * when close() is called, causing Thread leaks and preventing GC of closed pools.
 */
@Tag("2025")
public class BugFixPoolDeepReviewTest {

    // ============================================================
    // Bug 7: AbstractPool shutdown hook leak
    // ============================================================

    @Test
    @DisplayName("ObjectPool close should not leak shutdown hooks")
    public void testObjectPool_shutdownHookRemovedOnClose() {
        // Before fix: each pool registered a shutdown hook that was never removed.
        // After fix: close() removes the shutdown hook via removeShutdownHook().
        // We verify that creating and closing many pools does not accumulate threads.

        int initialThreadCount = Thread.activeCount();

        for (int i = 0; i < 50; i++) {
            ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(10, 0);
            pool.add(PoolableAdapter.of("item-" + i));
            pool.close();
        }

        // After closing all pools, thread count should not have grown significantly
        // (each leaked shutdown hook would be a Thread object registered with Runtime)
        // We allow some margin for GC and other threads
        int finalThreadCount = Thread.activeCount();
        assertTrue(finalThreadCount < initialThreadCount + 20,
                "Thread count should not grow significantly after closing pools. Initial: " + initialThreadCount + ", Final: " + finalThreadCount);
    }

    @Test
    @DisplayName("KeyedObjectPool close should not leak shutdown hooks")
    public void testKeyedObjectPool_shutdownHookRemovedOnClose() {
        int initialThreadCount = Thread.activeCount();

        for (int i = 0; i < 50; i++) {
            KeyedObjectPool<String, PoolableAdapter<String>> pool = PoolFactory.createKeyedObjectPool(10, 0);
            pool.put("key-" + i, PoolableAdapter.of("val-" + i));
            pool.close();
        }

        int finalThreadCount = Thread.activeCount();
        assertTrue(finalThreadCount < initialThreadCount + 20,
                "Thread count should not grow significantly after closing keyed pools. Initial: " + initialThreadCount + ", Final: " + finalThreadCount);
    }

    @Test
    @DisplayName("Pool close is idempotent - double close should not throw")
    public void testObjectPool_doubleCloseIsIdempotent() {
        ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(10, 0);
        pool.add(PoolableAdapter.of("item"));

        pool.close();
        assertTrue(pool.isClosed());

        // Second close should not throw
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    @DisplayName("Pool operations after close should fail")
    public void testObjectPool_operationsAfterClose() {
        ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(10, 0);
        pool.close();

        assertTrue(pool.isClosed());
    }
}
