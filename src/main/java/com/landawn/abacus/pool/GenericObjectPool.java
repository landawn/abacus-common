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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 *
 * @param <E>
 */
public class GenericObjectPool<E extends Poolable> extends AbstractPool implements ObjectPool<E> {

    @Serial
    private static final long serialVersionUID = -5055744987721643286L;

    private final ObjectPool.MemoryMeasure<E> memoryMeasure;

    final Deque<E> pool;

    final Comparator<E> cmp;

    ScheduledFuture<?> scheduleFuture;

    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        super(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        this.memoryMeasure = memoryMeasure;
        pool = new ArrayDeque<>(Math.min(capacity, 1000));

        switch (this.evictionPolicy) {
            // =============================================== For Priority Queue
            case LAST_ACCESS_TIME:

                cmp = Comparator.comparingLong(o -> o.activityPrint().getLastAccessTime());

                break;

            case ACCESS_COUNT:
                cmp = Comparator.comparingLong(o -> o.activityPrint().getAccessCount());

                break;

            case EXPIRATION_TIME:
                cmp = Comparator.comparingLong(o -> o.activityPrint().getExpirationTime());

                break;

            default:
                throw new RuntimeException("Unsupported eviction policy: " + evictionPolicy.name());
        }

        if (evictDelay > 0) {
            final Runnable evictTask = () -> {
                // Evict from the pool
                try {
                    evict();
                } catch (final Exception e) {
                    // ignore
                    if (logger.isWarnEnabled()) {
                        logger.warn(ExceptionUtil.getErrorMessage(e, true));
                    }
                }
            };

            scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelay, evictDelay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     *
     * @param e
     * @return {@code true}, if successful
     * @throws IllegalStateException
     */
    @Override
    public boolean add(final E e) throws IllegalStateException {
        assertNotClosed();

        if (e == null) {
            throw new IllegalArgumentException();
        }

        if (e.activityPrint().isExpired()) {
            return false;
        }

        putCount.incrementAndGet();

        lock.lock();

        try {
            if (pool.size() >= capacity) {
                if (autoBalance) {
                    vacate();
                } else {
                    return false;
                }
            }

            if (memoryMeasure != null && memoryMeasure.sizeOf(e) > maxMemorySize - totalDataSize.get()) {
                // ignore.

                return false;
            } else {
                pool.push(e);

                if (memoryMeasure != null) {
                    totalDataSize.addAndGet(memoryMeasure.sizeOf(e)); //NOSONAR
                }

                notEmpty.signal();

                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param e
     * @param autoDestroyOnFailedToAdd
     * @return {@code true}, if successful
     */
    @Override
    public boolean add(final E e, final boolean autoDestroyOnFailedToAdd) {
        boolean success = false;

        try {
            success = add(e);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && e != null) {
                e.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     *
     * @param e
     * @param timeout
     * @param unit
     * @return {@code true}, if successful
     * @throws IllegalStateException
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public boolean add(final E e, final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        assertNotClosed();

        if (e == null) {
            throw new IllegalArgumentException();
        }

        if (e.activityPrint().isExpired()) {
            return false;
        }

        putCount.incrementAndGet();

        long nanos = unit.toNanos(timeout);
        lock.lock();

        try {
            if ((pool.size() >= capacity) && autoBalance) {
                vacate();
            }

            while (true) {
                if (pool.size() < capacity) {
                    if (memoryMeasure != null && memoryMeasure.sizeOf(e) > maxMemorySize - totalDataSize.get()) {
                        // ignore.

                        return false;
                    } else {
                        pool.push(e);

                        if (memoryMeasure != null) {
                            totalDataSize.addAndGet(memoryMeasure.sizeOf(e)); //NOSONAR
                        }

                        notEmpty.signal();

                        return true;
                    }
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = notFull.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param e
     * @param timeout
     * @param unit
     * @param autoDestroyOnFailedToAdd
     * @return {@code true}, if successful
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public boolean add(final E e, final long timeout, final TimeUnit unit, final boolean autoDestroyOnFailedToAdd) throws InterruptedException {
        boolean success = false;

        try {
            success = add(e, timeout, unit);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && e != null) {
                e.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     *
     * @return
     * @throws IllegalStateException
     */
    @Override
    public E take() throws IllegalStateException {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.size() > 0 ? pool.pop() : null;

            if (e != null) {
                final ActivityPrint activityPrint = e.activityPrint();

                if (activityPrint.isExpired()) {
                    destroy(e, Caller.EVICT);
                    e = null;
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();

                    if (memoryMeasure != null) {
                        totalDataSize.addAndGet(-memoryMeasure.sizeOf(e)); //NOSONAR
                    }
                }

                notFull.signal();
            }
        } finally {
            if (e != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }

            lock.unlock();
        }

        return e;
    }

    /**
     *
     * @param timeout
     * @param unit
     * @return
     * @throws IllegalStateException
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public E take(final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        assertNotClosed();

        E e = null;
        long nanos = unit.toNanos(timeout);

        lock.lock();

        try {
            while (true) {
                e = pool.size() > 0 ? pool.pop() : null;

                if (e != null) {
                    final ActivityPrint activityPrint = e.activityPrint();

                    if (activityPrint.isExpired()) {
                        destroy(e, Caller.EVICT);
                        e = null;
                    } else {
                        activityPrint.updateLastAccessTime();
                        activityPrint.updateAccessCount();

                        if (memoryMeasure != null) {
                            totalDataSize.addAndGet(-memoryMeasure.sizeOf(e)); //NOSONAR
                        }
                    }

                    notFull.signal();

                    if (e != null) {
                        return e;
                    }
                }

                if (nanos <= 0) {
                    return null;
                }

                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            if (e != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }

            lock.unlock();
        }
    }

    /**
     *
     * @param valueToFind
     * @return {@code true}, if successful
     * @throws IllegalStateException
     */
    @Override
    public boolean contains(final E valueToFind) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            return pool.contains(valueToFind);
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @throws IllegalStateException
     */
    @Override
    public void vacate() throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            vacate((int) (pool.size() * balanceFactor)); // NOSONAR

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @throws IllegalStateException
     */
    @Override
    public void clear() throws IllegalStateException {
        assertNotClosed();

        removeAll(Caller.REMOVE_REPLACE_CLEAR);
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        try {
            if (scheduleFuture != null) {
                scheduleFuture.cancel(true);
            }
        } finally {
            removeAll(Caller.CLOSE);
        }
    }

    /**
     *
     * @return
     * @throws IllegalStateException
     */
    @Override
    public int size() throws IllegalStateException {
        // assertNotClosed();

        return pool.size();
    }

    @Override
    public int hashCode() {
        return pool.hashCode();
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof GenericObjectPool && N.equals(((GenericObjectPool<E>) obj).pool, pool));
    }

    @Override
    public String toString() {
        return pool.toString();
    }

    /**
     *
     * @param vacationNumber
     */
    protected void vacate(final int vacationNumber) {
        final int size = pool.size();

        if (vacationNumber >= size) {
            destroyAll(new ArrayList<>(pool), Caller.VACATE);
            pool.clear();
        } else {
            final Queue<E> heap = new PriorityQueue<>(vacationNumber, cmp);

            for (final E e : pool) {
                if (heap.size() < vacationNumber) {
                    heap.offer(e);
                } else if (cmp.compare(e, heap.peek()) < 0) {
                    heap.poll();
                    heap.offer(e);
                }
            }

            for (final E e : heap) {
                pool.remove(e);
            }

            destroyAll(heap, Caller.VACATE);
        }
    }

    /**
     * scan the object pool to find the idle object which inactive time greater than permitted the inactive time and remove it
     *
     */
    @SuppressWarnings("deprecation")
    protected void evict() {
        lock.lock();

        List<E> removingObjects = null;

        try {
            for (final E e : pool) {
                if (e.activityPrint().isExpired()) {
                    if (removingObjects == null) {
                        removingObjects = Objectory.createList();
                    }

                    removingObjects.add(e);
                }
            }

            if (N.notEmpty(removingObjects)) {
                pool.removeAll(removingObjects);

                destroyAll(removingObjects, Caller.EVICT);

                notFull.signalAll();
            }
        } finally {
            lock.unlock();

            Objectory.recycle(removingObjects);
        }
    }

    /**
     *
     * @param value
     * @param caller
     */
    protected void destroy(final E value, final Caller caller) {
        if (caller == Caller.EVICT || caller == Caller.VACATE) {
            evictionCount.incrementAndGet();
        }

        if (value != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Destroying cached object " + ClassUtil.getSimpleClassName(value.getClass()) + " with activity print: " + value.activityPrint());
            }

            if (memoryMeasure != null) {
                totalDataSize.addAndGet(-memoryMeasure.sizeOf(value));
            }

            try {
                value.destroy(caller);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }
        }
    }

    /**
     *
     * @param c
     * @param caller
     */
    protected void destroyAll(final Collection<E> c, final Caller caller) {
        if (N.notEmpty(c)) {
            for (final E e : c) {
                destroy(e, caller);
            }
        }
    }

    /**
     * Removes the all.
     * @param caller
     */
    private void removeAll(final Caller caller) {
        lock.lock();

        try {
            destroyAll(new ArrayList<>(pool), caller);

            pool.clear();

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Serial
    private void writeObject(final ObjectOutputStream os) throws IOException {
        lock.lock();

        try {
            os.defaultWriteObject();
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param is
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    @Serial
    private void readObject(final ObjectInputStream is) throws IOException, ClassNotFoundException {
        lock.lock();

        try {
            is.defaultReadObject();
        } finally {
            lock.unlock();
        }
    }
}
