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
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 *
 * @param <K> the key type
 * @param <E>
 */
public class GenericKeyedObjectPool<K, E extends Poolable> extends AbstractPool implements KeyedObjectPool<K, E> {

    @Serial
    private static final long serialVersionUID = 4137548490922758243L;

    private final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure;

    final Map<K, E> pool;

    final Comparator<Map.Entry<K, E>> cmp;

    ScheduledFuture<?> scheduleFuture;

    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        super(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        this.memoryMeasure = memoryMeasure;
        pool = new HashMap<>(Math.min(capacity, 1000));

        switch (this.evictionPolicy) {
            case LAST_ACCESS_TIME:

                cmp = Comparator.comparingLong(o -> o.getValue().activityPrint().getLastAccessTime());

                break;

            case ACCESS_COUNT:
                cmp = Comparator.comparingLong(o -> o.getValue().activityPrint().getAccessCount());

                break;

            case EXPIRATION_TIME:
                cmp = Comparator.comparingLong(o -> o.getValue().activityPrint().getExpirationTime());

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
     * @param key
     * @param e
     * @return {@code true}, if successful
     * @throws IllegalStateException
     */
    @Override
    public boolean put(final K key, final E e) throws IllegalStateException {
        assertNotClosed();

        if (key == null || e == null) {
            throw new IllegalArgumentException();
        }

        if (e.activityPrint().isExpired()) {
            return false;
        }

        putCount.incrementAndGet();

        lock.lock();

        try {
            if (pool.size() >= capacity || totalDataSize.get() > maxMemorySize) {
                if (autoBalance) {
                    vacate();
                } else {
                    return false;
                }
            }

            final long memorySize = memoryMeasure == null ? 0 : memoryMeasure.sizeOf(key, e);

            if (memoryMeasure != null && memorySize > maxMemorySize - totalDataSize.get()) {
                // ignore.

                return false;
            } else {
                final E oldValue = pool.put(key, e);

                if (oldValue != null) {
                    destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }

                if (memoryMeasure != null) {
                    totalDataSize.addAndGet(memorySize);
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
     * @param key
     * @param e
     * @param autoDestroyOnFailedToPut
     * @return {@code true}, if successful
     */
    @Override
    public boolean put(final K key, final E e, final boolean autoDestroyOnFailedToPut) {
        boolean success = false;

        try {
            success = put(key, e);
        } finally {
            if (autoDestroyOnFailedToPut && !success && e != null) {
                e.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     *
     * @param key
     * @return
     * @throws IllegalStateException
     */
    @Override
    public E get(final K key) throws IllegalStateException {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.get(key);

            if (e != null) {
                final ActivityPrint activityPrint = e.activityPrint();

                if (activityPrint.isExpired()) {
                    pool.remove(key);
                    destroy(key, e, Caller.EVICT);
                    e = null;
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();
                }
            }

            return e;
        } finally {
            lock.unlock();

            if (e != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }
    }

    /**
     *
     * @param key
     * @return
     * @throws IllegalStateException
     */
    @Override
    public E remove(final K key) throws IllegalStateException {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.remove(key);

            if (e != null) {
                final ActivityPrint activityPrint = e.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                if (memoryMeasure != null) {
                    totalDataSize.addAndGet(-memoryMeasure.sizeOf(key, e)); //NOSONAR
                }

                notFull.signal();
            }

            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param key
     * @return
     * @throws IllegalStateException
     */
    @Override
    public E peek(final K key) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            final E e = pool.get(key);

            if (e != null) {
                final ActivityPrint activityPrint = e.activityPrint();

                if (activityPrint.isExpired()) {
                    pool.remove(key);
                    destroy(key, e, Caller.EVICT);

                    return null;
                }
            }

            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param key
     * @return {@code true}, if successful
     * @throws IllegalStateException
     */
    @Override
    public boolean containsKey(final K key) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            return pool.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    //    /**
    //     *
    //     *
    //     * @param e
    //     * @return true, if successful
    //     * @throws IllegalStateException
    //     */
    //    @Override
    //    public boolean containsValue(final E e) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        lock.lock();
    //
    //        try {
    //            return pool.containsValue(e);
    //        } finally {
    //            lock.unlock();
    //        }
    //    }

    /**
     *
     * @return
     * @throws IllegalStateException
     */
    @Override
    public Set<K> keySet() throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            return N.newHashSet(pool.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @return
     * @throws IllegalStateException
     */
    @Override
    public Collection<E> values() throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            return new ArrayList<>(pool.values());
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
        return this == obj || (obj instanceof GenericKeyedObjectPool && N.equals(((GenericKeyedObjectPool<K, E>) obj).pool, pool));
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
            destroyAll(new HashMap<>(pool), Caller.VACATE);
            pool.clear();
        } else {
            final Queue<Map.Entry<K, E>> heap = new PriorityQueue<>(vacationNumber, cmp);

            for (final Map.Entry<K, E> entry : pool.entrySet()) {
                if (heap.size() < vacationNumber) {
                    heap.offer(entry);
                } else if (cmp.compare(entry, heap.peek()) < 0) {
                    heap.poll();
                    heap.offer(entry);
                }
            }

            final Map<K, E> removingObjects = N.newHashMap(heap.size());

            for (final Map.Entry<K, E> entry : heap) {
                pool.remove(entry.getKey());
                removingObjects.put(entry.getKey(), entry.getValue());
            }

            destroyAll(removingObjects, Caller.VACATE);
        }
    }

    /**
     * scan the object pool to find the idle object which inactive time greater than permitted the inactive time and remove it.
     *
     */
    @SuppressWarnings({ "null", "deprecation" })
    protected void evict() {
        lock.lock();

        Map<K, E> removingObjects = null;

        try {
            for (final Map.Entry<K, E> entry : pool.entrySet()) {
                if (entry.getValue().activityPrint().isExpired()) {
                    if (removingObjects == null) {
                        removingObjects = Objectory.createMap();
                    }

                    removingObjects.put(entry.getKey(), entry.getValue());
                }
            }

            if (N.notEmpty(removingObjects)) {
                for (final K key : removingObjects.keySet()) {
                    pool.remove(key);
                }

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
     * @param key
     * @param value
     * @param caller
     */
    protected void destroy(final K key, final E value, final Caller caller) {
        if (caller == Caller.EVICT || caller == Caller.VACATE) {
            evictionCount.incrementAndGet();
        }

        if (value != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Destroying cached object " + ClassUtil.getSimpleClassName(value.getClass()) + " with activity print: " + value.activityPrint());
            }

            if (memoryMeasure != null) {
                totalDataSize.addAndGet(-memoryMeasure.sizeOf(key, value));
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
     * @param map
     * @param caller
     */
    protected void destroyAll(final Map<K, E> map, final Caller caller) {
        if (N.notEmpty(map)) {
            for (final Map.Entry<K, E> entry : map.entrySet()) {
                destroy(entry.getKey(), entry.getValue(), caller);
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
            destroyAll(new HashMap<>(pool), caller);

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
    private void writeObject(final java.io.ObjectOutputStream os) throws IOException {
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
     * @throws ClassNotFoundException
     */
    @Serial
    private void readObject(final java.io.ObjectInputStream is) throws IOException, ClassNotFoundException {
        lock.lock();

        try {
            is.defaultReadObject();
        } finally {
            lock.unlock();
        }
    }
}
