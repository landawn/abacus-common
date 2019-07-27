/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.pool;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.MoreExecutors;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class AbstractPool implements Pool {
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

    final ReentrantLock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();
    final Condition notFull = lock.newCondition();
    final int capacity;
    final EvictionPolicy evictionPolicy;
    final boolean autoBalance;
    final float balanceFactor;
    boolean isClosed = false;

    protected AbstractPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor) {
        if (capacity < 0 || evictDelay < 0 || balanceFactor < 0) {
            throw new IllegalArgumentException(
                    "Capacity(" + capacity + "), evict delay(" + evictDelay + "), balanc factor(" + balanceFactor + ") can not be negative");
        }

        this.capacity = capacity;
        this.evictionPolicy = evictionPolicy == null ? EvictionPolicy.LAST_ACCESS_TIME : evictionPolicy;
        this.autoBalance = autoBalance;
        this.balanceFactor = balanceFactor == 0f ? DEFAULT_BALANCE_FACTOR : balanceFactor;

        final Class<?> cls = this.getClass();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.warn("Starting to shutdown pool: " + ClassUtil.getCanonicalClassName(cls));

                try {
                    close();
                } finally {
                    logger.warn("Completed to shutdown pool: " + ClassUtil.getCanonicalClassName(cls));
                }
            }
        });
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public long putCount() {
        return putCount.get();
    }

    @Override
    public long hitCount() {
        return hitCount.get();
    }

    @Override
    public long missCount() {
        return missCount.get();
    }

    @Override
    public long evictionCount() {
        return evictionCount.get();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    protected void assertNotClosed() {
        if (isClosed) {
            throw new AbacusException(ClassUtil.getCanonicalClassName(getClass()) + " has been closed");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (!isClosed) {
            close();
        }
    }
}
