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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * A generic implementation of KeyedObjectPool that manages poolable objects by keys.
 * This implementation uses a HashMap internally to store key-value mappings.
 * 
 * <p>Features:
 * <ul>
 *   <li>Thread-safe operations using ReentrantLock</li>
 *   <li>Automatic eviction of expired objects based on configurable policies</li>
 *   <li>Memory-based capacity constraints when configured with MemoryMeasure</li>
 *   <li>Auto-balancing to maintain optimal pool size</li>
 *   <li>Per-key storage and retrieval of poolable objects</li>
 * </ul>
 * 
 * <p>The pool can be configured with different eviction policies:
 * <ul>
 *   <li>LAST_ACCESS_TIME - Evicts least recently accessed entries</li>
 *   <li>ACCESS_COUNT - Evicts least frequently accessed entries</li>
 *   <li>EXPIRATION_TIME - Evicts entries closest to expiration</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a keyed pool for database connections by schema
 * GenericKeyedObjectPool<String, DBConnection> pool = new GenericKeyedObjectPool<>(
 *     100, 300000, EvictionPolicy.LAST_ACCESS_TIME
 * );
 * 
 * // Store connections
 * pool.put("schema1", new DBConnection("schema1"));
 * pool.put("schema2", new DBConnection("schema2"));
 * 
 * // Retrieve connections
 * DBConnection conn = pool.get("schema1");
 * }</pre>
 * 
 * @param <K> the type of keys maintained by this pool
 * @param <E> the type of pooled values, must implement Poolable
 * @see KeyedObjectPool
 * @see AbstractPool
 * @see PoolFactory
 */
public class GenericKeyedObjectPool<K, E extends Poolable> extends AbstractPool implements KeyedObjectPool<K, E> {

    @Serial
    private static final long serialVersionUID = 4137548490922758243L;

    /**
     * Optional memory measure for tracking memory usage of key-value pairs.
     */
    private final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure;

    /**
     * Internal storage for key-value mappings.
     */
    final Map<K, E> pool;

    /**
     * Comparator used to determine eviction order based on the configured eviction policy.
     */
    final Comparator<Map.Entry<K, E>> cmp;

    /**
     * Future representing the scheduled eviction task, {@code null} if eviction is disabled.
     */
    ScheduledFuture<?> scheduleFuture;

    /**
     * Constructs a new GenericKeyedObjectPool with basic configuration.
     * Uses default auto-balancing and balance factor settings.
     * 
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable
     * @param evictionPolicy the policy to use for selecting entries to evict
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with memory-based constraints.
     * Uses default auto-balancing and balance factor settings.
     * 
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit
     * @param memoryMeasure the function to calculate key-value pair memory size, or {@code null} if not using memory limits
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with auto-balancing configuration.
     * Does not use memory-based constraints.
     * 
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing (0-1)
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with full configuration options.
     * 
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing (0-1)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit
     * @param memoryMeasure the function to calculate key-value pair memory size, or {@code null} if not using memory limits
     */
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
     * Associates the specified value with the specified key in this pool.
     * If the pool previously contained a mapping for the key, the old value is replaced and destroyed.
     * 
     * <p>The put operation will fail if:
     * <ul>
     *   <li>Either key or value is null</li>
     *   <li>The value has already expired</li>
     *   <li>The pool is at capacity and auto-balancing is disabled</li>
     *   <li>The key-value pair would exceed memory constraints</li>
     * </ul>
     * 
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @return {@code true} if the mapping was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or value is null
     * @throws IllegalStateException if the pool has been closed
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
     * Associates the specified value with the specified key in this pool,
     * with optional automatic destruction on failure.
     * 
     * @param key the key with which the specified value is to be associated
     * @param e the value to be associated with the specified key
     * @param autoDestroyOnFailedToPut if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if put fails
     * @return {@code true} if the mapping was successfully added, {@code false} otherwise
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
     * Returns the value associated with the specified key, or {@code null} if no mapping exists.
     * If the value has expired, it is removed and destroyed, and {@code null} is returned.
     * The value's activity print is updated on successful retrieval.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or {@code null} if no mapping exists or value expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
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
     * Removes and returns the value associated with the specified key.
     * The value's activity print is updated before removal.
     *
     * @param key the key whose mapping is to be removed
     * @return the value previously associated with the key, or {@code null} if no mapping existed
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
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
     * Returns the value associated with the specified key without updating access statistics.
     * If the value has expired, it is removed and destroyed, and {@code null} is returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or {@code null} if no mapping exists or value expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
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
     * Returns {@code true} if this pool contains a mapping for the specified key.
     * 
     * @param key the key whose presence in this pool is to be tested
     * @return {@code true} if this pool contains a mapping for the specified key
     * @throws IllegalStateException if the pool has been closed
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

    /**
     * Returns a snapshot of the keys contained in this pool.
     * The returned set is a copy and will not reflect subsequent changes to the pool.
     * 
     * @return a set containing all keys currently in the pool
     * @throws IllegalStateException if the pool has been closed
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
     * Returns a snapshot of the values contained in this pool.
     * The returned collection is a copy and will not reflect subsequent changes to the pool.
     * 
     * @return a collection containing all values currently in the pool
     * @throws IllegalStateException if the pool has been closed
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
     * Removes all mappings from this pool.
     * All removed values are destroyed with the REMOVE_REPLACE_CLEAR reason.
     * 
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void clear() throws IllegalStateException {
        assertNotClosed();

        removeAll(Caller.REMOVE_REPLACE_CLEAR);
    }

    /**
     * Closes this pool and releases all resources.
     * Cancels the eviction task if scheduled and destroys all pooled values.
     * This method is idempotent.
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
     * Removes a portion of mappings from the pool based on the configured balance factor.
     * Entries are selected for removal according to the eviction policy.
     * 
     * @throws IllegalStateException if the pool has been closed
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
     * Returns the current number of key-value mappings in the pool.
     * 
     * @return the number of mappings currently in the pool
     */
    @Override
    public int size() throws IllegalStateException {
        // assertNotClosed();

        return pool.size();
    }

    /**
     * Returns the hash code value for this pool.
     * The hash code is based on the internal pool map.
     * 
     * @return a hash code value for this pool
     */
    @Override
    public int hashCode() {
        return pool.hashCode();
    }

    /**
     * Compares this pool to the specified object for equality.
     * Two pools are equal if they contain the same key-value mappings.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the pools are equal, {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof GenericKeyedObjectPool && N.equals(((GenericKeyedObjectPool<K, E>) obj).pool, pool));
    }

    /**
     * Returns a string representation of this pool.
     * The string representation consists of the string representation of the internal pool map.
     * 
     * @return a string representation of this pool
     */
    @Override
    public String toString() {
        return "{pool=GenericKeyedObjectPool, capacity=" + capacity + ", evictDelay=" + evictDelay + ", evictionPolicy=" + evictionPolicy + ", autoBalance="
                + autoBalance + ", balanceFactor=" + balanceFactor + ", maxMemorySize=" + maxMemorySize + ", memoryMeasure=" + memoryMeasure
                + ", totalDataSize=" + totalDataSize.get() + "}";
    }

    /**
     * Removes the specified number of entries from the pool based on the eviction policy.
     * This method is called internally during vacate operations.
     * 
     * @param vacationNumber the number of entries to remove
     */
    protected void vacate(final int vacationNumber) {
        final int size = pool.size();

        if (vacationNumber >= size) {
            destroyAll(new HashMap<>(pool), Caller.VACATE);
            pool.clear();
        } else if (vacationNumber > 0) {
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
     * Scans the pool for expired values and removes them.
     * This method is called periodically by the scheduled eviction task.
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
     * Destroys a single key-value pair and updates statistics.
     * 
     * @param key the key part of the pair
     * @param value the value to destroy
     * @param caller the reason for destruction
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

    protected void destroyAll(final Map<K, E> map, final Caller caller) {
        if (N.notEmpty(map)) {
            for (final Map.Entry<K, E> entry : map.entrySet()) {
                destroy(entry.getKey(), entry.getValue(), caller);
            }
        }
    }

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
     * Serializes this pool to an ObjectOutputStream.
     * The pool is locked during serialization to ensure consistency.
     * 
     * @param os the output stream
     * @throws IOException if an I/O error occurs
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
     * Deserializes this pool from an ObjectInputStream.
     * The pool is locked during deserialization to ensure consistency.
     * 
     * @param is the input stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
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
