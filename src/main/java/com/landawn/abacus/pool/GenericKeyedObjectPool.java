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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

// TODO: Auto-generated Javadoc
/**
 * The Class GenericKeyedObjectPool.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E>
 * @since 0.8
 */
public class GenericKeyedObjectPool<K, E extends Poolable> extends AbstractPool implements KeyedObjectPool<K, E> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 2208516321399679864L;

    /** The max memory size. */
    private final long maxMemorySize;

    /** The memory measure. */
    private final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure;

    /** The used memory size. */
    private volatile long usedMemorySize = 0;

    /** The pool. */
    final Map<K, E> pool;

    /** The cmp. */
    final Comparator<Map.Entry<K, E>> cmp;

    /** The schedule future. */
    ScheduledFuture<?> scheduleFuture;

    /**
     * Instantiates a new generic keyed object pool.
     *
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     */
    protected GenericKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    /**
     * Instantiates a new generic keyed object pool.
     *
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param maxMemorySize
     * @param memoryMeasure
     */
    protected GenericKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, long maxMemorySize,
            KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Instantiates a new generic keyed object pool.
     *
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param autoBalance
     * @param balanceFactor
     */
    protected GenericKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Instantiates a new generic keyed object pool.
     *
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param autoBalance
     * @param balanceFactor
     * @param maxMemorySize
     * @param memoryMeasure
     */
    protected GenericKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor, long maxMemorySize,
            KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        super(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);

        this.maxMemorySize = maxMemorySize;
        this.memoryMeasure = memoryMeasure;
        this.pool = new HashMap<K, E>((capacity > 1000) ? 1000 : capacity);

        switch (this.evictionPolicy) {
            case LAST_ACCESS_TIME:

                cmp = new Comparator<Map.Entry<K, E>>() {
                    @Override
                    public int compare(Map.Entry<K, E> o1, Map.Entry<K, E> o2) {
                        return Long.compare(o1.getValue().activityPrint().getLastAccessTime(), o2.getValue().activityPrint().getLastAccessTime());
                    }
                };

                break;

            case ACCESS_COUNT:
                cmp = new Comparator<Map.Entry<K, E>>() {
                    @Override
                    public int compare(Map.Entry<K, E> o1, Map.Entry<K, E> o2) {
                        return Long.compare(o1.getValue().activityPrint().getAccessCount(), o2.getValue().activityPrint().getAccessCount());
                    }
                };

                break;

            case EXPIRATION_TIME:
                cmp = new Comparator<Map.Entry<K, E>>() {
                    @Override
                    public int compare(Map.Entry<K, E> o1, Map.Entry<K, E> o2) {
                        return Long.compare(o1.getValue().activityPrint().getExpirationTime(), o2.getValue().activityPrint().getExpirationTime());
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
     * Put.
     *
     * @param key
     * @param e
     * @return true, if successful
     */
    @Override
    public boolean put(K key, E e) {
        assertNotClosed();

        if (key == null || e == null) {
            throw new NullPointerException();
        }

        if (e.activityPrint().isExpired()) {
            return false;
        }

        putCount.incrementAndGet();

        lock.lock();

        try {
            if (pool.size() >= capacity || usedMemorySize > maxMemorySize) {
                if (autoBalance) {
                    vacate();
                } else {
                    return false;
                }
            }

            if (memoryMeasure != null && memoryMeasure.sizeOf(key, e) > maxMemorySize - usedMemorySize) {
                // ignore.

                return false;
            } else {
                E oldValue = pool.put(key, e);

                if (oldValue != null) {
                    destroy(key, oldValue);
                }

                if (memoryMeasure != null) {
                    usedMemorySize += memoryMeasure.sizeOf(key, e);
                }

                notEmpty.signal();

                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Put.
     *
     * @param key
     * @param e
     * @param autoDestroyOnFailedToPut
     * @return true, if successful
     */
    @Override
    public boolean put(K key, E e, boolean autoDestroyOnFailedToPut) {
        boolean sucess = false;

        try {
            sucess = put(key, e);
        } finally {
            if (autoDestroyOnFailedToPut && sucess == false && e != null) {
                e.destroy();
            }
        }

        return sucess;
    }

    /**
     * Gets the.
     *
     * @param key
     * @return
     */
    @Override
    public E get(K key) {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.get(key);

            if (e != null) {
                ActivityPrint activityPrint = e.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }

            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the.
     *
     * @param key
     * @return
     */
    @Override
    public E remove(K key) {
        assertNotClosed();

        E e = null;

        lock.lock();

        try {
            e = pool.remove(key);

            if (e != null) {
                ActivityPrint activityPrint = e.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                if (memoryMeasure != null) {
                    usedMemorySize -= memoryMeasure.sizeOf(key, e);
                }

                notFull.signal();
            }

            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Peek.
     *
     * @param key
     * @return
     */
    @Override
    public E peek(K key) {
        assertNotClosed();

        lock.lock();

        try {
            return pool.get(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Contains key.
     *
     * @param key
     * @return true, if successful
     */
    @Override
    public boolean containsKey(K key) {
        assertNotClosed();

        lock.lock();

        try {
            return pool.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Contains value.
     *
     * @param e
     * @return true, if successful
     */
    @Override
    public boolean containsValue(E e) {
        assertNotClosed();

        lock.lock();

        try {
            return pool.containsValue(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Key set.
     *
     * @return
     */
    @Override
    public Set<K> keySet() {
        assertNotClosed();

        lock.lock();

        try {
            return new HashSet<K>(pool.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Values.
     *
     * @return
     */
    @Override
    public Collection<E> values() {
        assertNotClosed();

        lock.lock();

        try {
            return new ArrayList<E>(pool.values());
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
     * Size.
     *
     * @return
     */
    @Override
    public int size() {
        // assertNotClosed();

        return pool.size();
    }

    /**
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return pool.hashCode();
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof GenericKeyedObjectPool && N.equals(((GenericKeyedObjectPool<K, E>) obj).pool, pool));
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return pool.toString();
    }

    /**
     * Vacate.
     *
     * @param vacationNumber
     */
    protected void vacate(int vacationNumber) {
        int size = pool.size();

        if (vacationNumber >= size) {
            destroyAll(new HashMap<>(pool));
            pool.clear();
        } else {
            final Queue<Map.Entry<K, E>> heap = new PriorityQueue<>(vacationNumber, cmp);

            for (Map.Entry<K, E> entry : pool.entrySet()) {
                if (heap.size() < vacationNumber) {
                    heap.offer(entry);
                } else if (cmp.compare(entry, heap.peek()) < 0) {
                    heap.poll();
                    heap.offer(entry);
                }
            }

            final Map<K, E> removingObjects = new HashMap<>(N.initHashCapacity(heap.size()));

            for (Map.Entry<K, E> entry : heap) {
                pool.remove(entry.getKey());
                removingObjects.put(entry.getKey(), entry.getValue());
            }

            destroyAll(removingObjects);
        }
    }

    /**
     * scan the object pool to find the idle object which inactive time greater than permitted the inactive time for it
     * or it's time out.
     * 
     */
    protected void evict() {
        lock.lock();

        Map<K, E> removingObjects = null;

        try {
            for (Map.Entry<K, E> entry : pool.entrySet()) {
                if (entry.getValue().activityPrint().isExpired()) {
                    if (removingObjects == null) {
                        removingObjects = Objectory.createMap();
                    }

                    removingObjects.put(entry.getKey(), entry.getValue());
                }
            }

            if (N.notNullOrEmpty(removingObjects)) {
                for (K key : removingObjects.keySet()) {
                    pool.remove(key);
                }

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
     * @param key
     * @param value
     */
    protected void destroy(K key, E value) {
        evictionCount.incrementAndGet();

        if (value != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroying cached object " + ClassUtil.getSimpleClassName(value.getClass()) + " with activity print: " + value.activityPrint());
            }

            if (memoryMeasure != null) {
                usedMemorySize -= memoryMeasure.sizeOf(key, value);
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
     * @param map
     */
    protected void destroyAll(Map<K, E> map) {
        if (N.notNullOrEmpty(map)) {
            for (Map.Entry<K, E> entry : map.entrySet()) {
                destroy(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Removes the all.
     */
    private void removeAll() {
        lock.lock();

        try {
            destroyAll(new HashMap<>(pool));

            pool.clear();

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write object.
     *
     * @param os
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
     * @param is
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
