/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.pool;

import java.io.Serial;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.MoreExecutors;

/**
 * Abstract base class for implementing object pools with eviction, balancing, and memory management capabilities.
 * This class provides the core functionality for thread-safe object pooling including statistics tracking,
 * automatic eviction of idle objects, and memory-based constraints.
 *
 * <p>The pool maintains various statistics such as hit/miss counts, eviction counts, and memory usage.
 * It uses a ReentrantLock for thread synchronization and condition variables for coordinating
 * producer-consumer interactions.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe operations with ReentrantLock</li>
 *   <li>Automatic eviction of expired objects based on configurable policies</li>
 *   <li>Memory-based capacity constraints</li>
 *   <li>Statistical tracking of pool operations</li>
 *   <li>Automatic shutdown hook for cleanup</li>
 * </ul>
 *
 * @see Pool
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public abstract class AbstractPool implements Pool {

    @Serial
    private static final long serialVersionUID = -7780250223658416202L;

    static final Logger logger = LoggerFactory.getLogger(AbstractPool.class);

    /**
     * Default delay in milliseconds between eviction runs.
     */
    static final long DEFAULT_EVICT_DELAY = 3000;

    /**
     * Default balance factor used when auto-balancing is enabled.
     * Determines the proportion of objects to remove during balancing operations.
     */
    static final float DEFAULT_BALANCE_FACTOR = 0.2f;

    /**
     * Shared scheduled executor service for all pool instances.
     * Used for running periodic eviction tasks.
     */
    static final ScheduledExecutorService scheduledExecutor;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(64);
        executor.setKeepAliveTime(180, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    /**
     * Counter for the number of put operations performed on this pool.
     */
    final AtomicLong putCount = new AtomicLong();

    /**
     * Counter for the number of successful get operations (hits) on this pool.
     */
    final AtomicLong hitCount = new AtomicLong();

    /**
     * Counter for the number of unsuccessful get operations (misses) on this pool.
     */
    final AtomicLong missCount = new AtomicLong();

    /**
     * Counter for the number of objects evicted from this pool.
     */
    final AtomicLong evictionCount = new AtomicLong();

    /**
     * Total size of data in bytes currently stored in the pool.
     * Only tracked when a memory measure is configured.
     */
    final AtomicLong totalDataSize = new AtomicLong();

    /**
     * Lock used for synchronizing pool operations.
     */
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Condition variable signaled when the pool is not empty.
     */
    final transient Condition notEmpty = lock.newCondition();

    /**
     * Condition variable signaled when the pool is not full.
     */
    final transient Condition notFull = lock.newCondition();

    /**
     * Maximum number of objects the pool can hold.
     */
    final int capacity;

    /**
     * Delay in milliseconds between eviction runs.
     * A value of 0 or less disables automatic eviction.
     */
    final long evictDelay;

    /**
     * Maximum total memory size in bytes the pool can use.
     * A value of 0 or less indicates no memory limit.
     */
    final long maxMemorySize;

    /**
     * Policy used to determine which objects to evict when the pool is full.
     */
    final EvictionPolicy evictionPolicy;

    /**
     * Whether automatic balancing is enabled for this pool.
     */
    final boolean autoBalance;

    /**
     * Factor used to determine how many objects to remove during balancing.
     * Should be between 0 and 1.
     */
    final float balanceFactor;

    /**
     * Flag indicating whether this pool has been closed.
     */
    boolean isClosed = false;

    /**
     * Constructs a new AbstractPool with the specified configuration.
     * This protected constructor is used by subclasses to initialize core pool functionality.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy for determining which objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @throws IllegalArgumentException if any numeric parameter is negative
     */
    protected AbstractPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor,
            final long maxMemorySize) {
        if (capacity < 0 || evictDelay < 0 || balanceFactor < 0 || maxMemorySize < 0) {
            throw new IllegalArgumentException("Capacity(" + capacity + "), evict delay(" + evictDelay + "), balance factor(" + balanceFactor
                    + "), max memory size(" + maxMemorySize + ") cannot be negative");
        }

        this.capacity = capacity;
        this.evictDelay = evictDelay < 0 ? 0 : evictDelay; // NOSONAR - allow 0 to disable eviction
        this.maxMemorySize = maxMemorySize;
        this.evictionPolicy = evictionPolicy == null ? EvictionPolicy.LAST_ACCESS_TIME : evictionPolicy;
        this.autoBalance = autoBalance;
        this.balanceFactor = balanceFactor == 0f ? DEFAULT_BALANCE_FACTOR : balanceFactor; // NOSONAR

        final Class<?> cls = this.getClass();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Starting to shutdown pool: " + ClassUtil.getCanonicalClassName(cls));

            try {
                close();
            } finally {
                logger.warn("Completed to shutdown pool: " + ClassUtil.getCanonicalClassName(cls));
            }
        }));
    }

    /**
     * Acquires the lock for this pool.
     * This method blocks until the lock is available.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * pool.lock();
     * try {
     *     // perform thread-safe operations
     * } finally {
     *     pool.unlock();
     * }
     * }</pre>
     *
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void lock() {
        lock.lock();
    }

    /**
     * Releases the lock for this pool.
     * This method should always be called in a finally block after {@link #lock()}.
     * 
     * @throws IllegalMonitorStateException if the current thread does not hold the lock
     */
    @Override
    public void unlock() {
        lock.unlock();
    }

    /**
     * Returns the maximum capacity of this pool.
     * The capacity represents the maximum number of objects that can be stored in the pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (pool.size() >= pool.capacity() * 0.9) {
     *     // Pool is 90% full, consider scaling
     * }
     * }</pre>
     *
     * @return the maximum number of objects this pool can hold
     */
    @Override
    public int capacity() {
        return capacity;
    }

    /**
     * Returns a snapshot of the current pool statistics.
     * The statistics include capacity, current size, operation counts, and memory usage.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PoolStats stats = pool.stats();
     * double hitRate = stats.getCount() > 0 ?
     *     (double) stats.hitCount() / stats.getCount() : 0.0;
     * System.out.println("Hit rate: " + (hitRate * 100) + "%");
     * System.out.println("Pool utilization: " + stats.size() + "/" + stats.capacity());
     * }</pre>
     *
     * @return a PoolStats object containing current pool statistics
     */
    @Override
    public PoolStats stats() {
        final long currentHitCount = hitCount.get();
        final long currentMissCount = missCount.get();
        final long currentGetCount = currentHitCount + currentMissCount;

        return new PoolStats(capacity, size(), putCount.get(), currentGetCount, currentHitCount, currentMissCount, evictionCount.get(),
                maxMemorySize <= 0 ? -1 : maxMemorySize, maxMemorySize <= 0 ? -1 : totalDataSize.get());
    }

    /**
     * Checks whether this pool is currently empty.
     * This is a convenience method equivalent to {@code size() == 0}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (pool.isEmpty()) {
     *     System.out.println("Pool has no available objects");
     * }
     * }</pre>
     *
     * @return {@code true} if the pool contains no objects, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Checks whether this pool has been closed.
     * Once closed, a pool cannot be reopened and all operations will throw IllegalStateException.
     * 
     * @return {@code true} if the pool has been closed, {@code false} otherwise
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Verifies that this pool is not closed, throwing an exception if it is.
     * This method should be called at the beginning of any operation that requires the pool to be open.
     * 
     * @throws IllegalStateException if the pool has been closed
     */
    protected void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException(ClassUtil.getCanonicalClassName(getClass()) + " has been closed");
        }
    }

}
