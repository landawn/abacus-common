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

public abstract class AbstractPool implements Pool {

    @Serial
    private static final long serialVersionUID = -7780250223658416202L;

    static final Logger logger = LoggerFactory.getLogger(AbstractPool.class);

    static final long DEFAULT_EVICT_DELAY = 3000;

    static final float DEFAULT_BALANCE_FACTOR = 0.2f;

    static final ScheduledExecutorService scheduledExecutor;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(64);
        executor.setKeepAliveTime(180, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    final AtomicLong putCount = new AtomicLong();

    final AtomicLong hitCount = new AtomicLong();

    final AtomicLong missCount = new AtomicLong();

    final AtomicLong evictionCount = new AtomicLong();

    final AtomicLong totalDataSize = new AtomicLong();

    final ReentrantLock lock = new ReentrantLock();

    final transient Condition notEmpty = lock.newCondition();

    final transient Condition notFull = lock.newCondition();

    final int capacity;

    final long maxMemorySize;

    final EvictionPolicy evictionPolicy;

    final boolean autoBalance;

    final float balanceFactor;

    boolean isClosed = false;

    protected AbstractPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor,
            final long maxMemorySize) {
        if (capacity < 0 || evictDelay < 0 || balanceFactor < 0 || maxMemorySize < 0) {
            throw new IllegalArgumentException("Capacity(" + capacity + "), evict delay(" + evictDelay + "), balance factor(" + balanceFactor
                    + "), max memory size(" + maxMemorySize + ") cannot be negative");
        }

        this.capacity = capacity;
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
     * Lock.
     */
    @Override
    public void lock() {
        lock.lock();
    }

    /**
     * Unlock.
     */
    @Override
    public void unlock() {
        lock.unlock();
    }

    /**
     * Gets the capacity.
     *
     * @return
     */
    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public PoolStats stats() {
        final long currentHitCount = hitCount.get();
        final long currentMissCount = missCount.get();
        final long currentGetCount = currentHitCount + currentMissCount;

        return new PoolStats(capacity, size(), putCount.get(), currentGetCount, currentHitCount, currentMissCount, evictionCount.get(),
                maxMemorySize <= 0 ? -1 : maxMemorySize, maxMemorySize <= 0 ? -1 : totalDataSize.get());
    }

    /**
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Checks if is closed.
     *
     * @return {@code true}, if is closed
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Assert not closed.
     */
    protected void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException(ClassUtil.getCanonicalClassName(getClass()) + " has been closed");
        }
    }

    //    /**
    //     *
    //     * @throws Throwable the throwable
    //     */
    //    @Override
    //    protected void finalize() throws Throwable {
    //        super.finalize();
    //
    //        if (!isClosed) {
    //            close();
    //        }
    //    }
}
