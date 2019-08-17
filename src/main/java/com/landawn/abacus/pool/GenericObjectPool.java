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

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

// TODO: Auto-generated Javadoc
/**
 * The Class GenericObjectPool.
 *
 * @author Haiyang Li
 * @param <E> the element type
 * @since 0.8
 */
public class GenericObjectPool<E extends Poolable> extends AbstractPool implements ObjectPool<E> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -5055744987721643286L;

    /** The max memory size. */
    private final long maxMemorySize;

    /** The memory measure. */
    private final ObjectPool.MemoryMeasure<E> memoryMeasure;

    /** The used memory size. */
    private volatile long usedMemorySize = 0;

    /** The pool. */
    final Deque<E> pool;

    /** The cmp. */
    final Comparator<E> cmp;

    /** The schedule future. */
    ScheduledFuture<?> scheduleFuture;

    /**
     * Instantiates a new generic object pool.
     *
     * @param capacity the capacity
     * @param evictDelay the evict delay
     * @param evictionPolicy the eviction policy
     */
    protected GenericObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    /**
     * Instantiates a new generic object pool.
     *
     * @param capacity the capacity
     * @param evictDelay the evict delay
     * @param evictionPolicy the eviction policy
     * @param maxMemorySize the max memory size
     * @param memoryMeasure the memory measure
     */
    protected GenericObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, long maxMemorySize, ObjectPool.MemoryMeasure<E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Instantiates a new generic object pool.
     *
     * @param capacity the capacity
     * @param evictDelay the evict delay
     * @param evictionPolicy the eviction policy
     * @param autoBalance the auto balance
     * @param balanceFactor the balance factor
     */
    protected GenericObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Instantiates a new generic object pool.
     *
     * @param capacity the capacity
     * @param evictDelay the evict delay
     * @param evictionPolicy the eviction policy
     * @param autoBalance the auto balance
     * @param balanceFactor the balance factor
     * @param maxMemorySize the max memory size
     * @param memoryMeasure the memory measure
     */
    protected GenericObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor, long maxMemorySize,
            ObjectPool.MemoryMeasure<E> memoryMeasure) {
        super(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);

        this.maxMemorySize = maxMemorySize;
        this.memoryMeasure = memoryMeasure;
        this.pool = new ArrayDeque<>((capacity > 1000) ? 1000 : capacity);

        switch (this.evictionPolicy) {
            // =============================================== For Priority Queue
            case LAST_ACCESS_TIME:

                cmp = new Comparator<E>() {
                    @Override
                    public int compare(E o1, E o2) {
                        return Long.compare(o1.activityPrint().getLastAccessTime(), o2.activityPrint().getLastAccessTime());
                    }
                };

                break;

            case ACCESS_COUNT:
                cmp = new Comparator<E>() {
                    @Override
                    public int compare(E o1, E o2) {
                        return Long.compare(o1.activityPrint().getAccessCount(), o2.activityPrint().getAccessCount());

                    }
                };

                break;

            case EXPIRATION_TIME:
                cmp = new Comparator<E>() {
                    @Override
                    public int compare(E o1, E o2) {
                        return Long.compare(o1.activityPrint().getExpirationTime(), o2.activityPrint().getExpirationTime());
                    }
                };

                break;

            default:
                throw new AbacusException("Unsupproted eviction policy: " + evictionPolicy.name());
        }

        if (evictDelay > 0) {
            final Runnable evictTask = new Runnable() {
                @Override
                public void run() {
                    // Evict from the pool
                    try {
                        evict();
                    } catch (Exception e) {
                        // ignore
                        if (logger.isWarnEnabled()) {
                            logger.warn(ExceptionUtil.getMessage(e));
                        }
                    }
                }
            };

            scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelay, evictDelay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Adds the.
     *
     * @param e the e
     * @return true, if successful
     */
    @Override
    public boolean add(E e) {
        assertNotClosed();

        if (e == null) {
            throw new NullPointerException();
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

            if (memoryMeasure != null && memoryMeasure.sizeOf(e) > maxMemorySize - usedMemorySize) {
                // ignore.

                return false;
            } else {
                pool.push(e);

                if (memoryMeasure != null) {
                    usedMemorySize += memoryMeasure.sizeOf(e);
                }

                notEmpty.signal();

                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds the.
     *
     * @param e the e
     * @param autoDestroyOnFailedToAdd the auto destroy on failed to add
     * @return true, if successful
     */
    @Override
    public boolean add(E e, boolean autoDestroyOnFailedToAdd) {
        boolean sucess = false;

        try {
            sucess = add(e);
        } finally {
            if (autoDestroyOnFailedToAdd && sucess == false && e != null) {
                e.destroy();
            }
        }

        return sucess;
    }

    /**
     * Adds the.
     *
     * @param e the e
     * @param timeout the timeout
     * @param unit the unit
     * @return true, if successful
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public boolean add(E e, long timeout, TimeUnit unit) throws InterruptedException {
        assertNotClosed();

        if (e == null) {
            throw new NullPointerException();
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
                    if (memoryMeasure != null && memoryMeasure.sizeOf(e) > maxMemorySize - usedMemorySize) {
                        // ignore.

                        return false;
                    } else {
                        pool.push(e);

                        if (memoryMeasure != null) {
                            usedMemorySize += memoryMeasure.sizeOf(e);
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
     * Adds the.
     *
     * @param e the e
     * @param timeout the timeout
     * @param unit the unit
     * @param autoDestroyOnFailedToAdd the auto destroy on failed to add
     * @return true, if successful
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public boolean add(E e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException {
        boolean sucess = false;

        try {
            sucess = add(e, timeout, unit);
        } finally {
            if (autoDestroyOnFailedToAdd && sucess == false && e != null) {
                e.destroy();
            }
        }

        return sucess;
    }

    /**
     * Take.
     *
     * @return the e
     */
    @Override
    public E take() {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.size() > 0 ? pool.pop() : null;

            if (e != null) {
                ActivityPrint activityPrint = e.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                if (memoryMeasure != null) {
                    usedMemorySize -= memoryMeasure.sizeOf(e);
                }

                hitCount.incrementAndGet();

                notFull.signal();
            } else {
                missCount.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }

        return e;
    }

    /**
     * Take.
     *
     * @param timeout the timeout
     * @param unit the unit
     * @return the e
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public E take(long timeout, TimeUnit unit) throws InterruptedException {
        assertNotClosed();

        E e = null;
        long nanos = unit.toNanos(timeout);

        lock.lock();

        try {
            while (true) {
                e = pool.size() > 0 ? pool.pop() : null;

                if (e != null) {
                    ActivityPrint activityPrint = e.activityPrint();
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();

                    if (memoryMeasure != null) {
                        usedMemorySize -= memoryMeasure.sizeOf(e);
                    }

                    hitCount.incrementAndGet();

                    notFull.signal();

                    return e;
                }

                if (nanos <= 0) {
                    missCount.incrementAndGet();

                    return null;
                }

                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Contains.
     *
     * @param e the e
     * @return true, if successful
     */
    @Override
    public boolean contains(E e) {
        assertNotClosed();

        lock.lock();

        try {
            return pool.contains(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Vacate.
     */
    @Override
    public void vacate() {
        assertNotClosed();

        lock.lock();

        try {
            vacate((int) (pool.size() * balanceFactor));

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        assertNotClosed();

        removeAll();
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
            removeAll();
        }
    }

    /**
     * Size.
     *
     * @return the int
     */
    @Override
    public int size() {
        // assertNotClosed();

        return pool.size();
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return pool.hashCode();
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof GenericObjectPool && N.equals(((GenericObjectPool<E>) obj).pool, pool));
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return pool.toString();
    }

    /**
     * Vacate.
     *
     * @param vacationNumber the vacation number
     */
    protected void vacate(int vacationNumber) {
        int size = pool.size();

        if (vacationNumber >= size) {
            destroyAll(new ArrayList<>(pool));
            pool.clear();
        } else {
            final Queue<E> heap = new PriorityQueue<>(vacationNumber, cmp);

            for (E e : pool) {
                if (heap.size() < vacationNumber) {
                    heap.offer(e);
                } else if (cmp.compare(e, heap.peek()) < 0) {
                    heap.poll();
                    heap.offer(e);
                }
            }

            for (E e : heap) {
                pool.remove(e);
            }

            destroyAll(heap);
        }
    }

    /**
     * scan the object pool to find the idle object which inactive time greater than permitted the inactive time for it
     * or it's time out.
     * 
     */
    protected void evict() {
        lock.lock();

        List<E> removingObjects = null;

        try {
            for (E e : pool) {
                if (e.activityPrint().isExpired()) {
                    if (removingObjects == null) {
                        removingObjects = Objectory.createList();
                    }

                    removingObjects.add(e);
                }
            }

            if (N.notNullOrEmpty(removingObjects)) {
                pool.removeAll(removingObjects);

                destroyAll(removingObjects);

                notFull.signalAll();
            }
        } finally {
            lock.unlock();

            Objectory.recycle(removingObjects);
        }
    }

    /**
     * Destroy.
     *
     * @param value the value
     */
    protected void destroy(E value) {
        evictionCount.incrementAndGet();

        if (value != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroying cached object " + ClassUtil.getSimpleClassName(value.getClass()) + " with activity print: " + value.activityPrint());
            }

            if (memoryMeasure != null) {
                usedMemorySize -= memoryMeasure.sizeOf(value);
            }

            try {
                value.destroy();
            } catch (Exception e) {

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            }
        }
    }

    /**
     * Destroy all.
     *
     * @param c the c
     */
    protected void destroyAll(Collection<E> c) {
        if (N.notNullOrEmpty(c)) {
            for (E e : c) {
                destroy(e);
            }
        }
    }

    /**
     * Removes the all.
     */
    private void removeAll() {
        lock.lock();

        try {
            destroyAll(new ArrayList<>(pool));

            pool.clear();

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write object.
     *
     * @param os the os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void writeObject(java.io.ObjectOutputStream os) throws java.io.IOException {
        lock.lock();

        try {
            os.defaultWriteObject();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Read object.
     *
     * @param is the is
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    private void readObject(java.io.ObjectInputStream is) throws java.io.IOException, ClassNotFoundException {
        lock.lock();

        try {
            is.defaultReadObject();
        } finally {
            lock.unlock();
        }
    }
}
