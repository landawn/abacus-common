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
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with memory-based constraints.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate entry memory size, or {@code null} if not using memory limits
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with full configuration options.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelay the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate entry memory size, or {@code null} if not using memory limits
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
     * Associates the specified element with the specified key in this pool.
     * If the pool previously contained a mapping for the key, the old element is replaced and destroyed.
     *
     * <p>The put operation will fail if:</p>
     * <ul>
     *   <li>Either key or element is null</li>
     *   <li>The element has already expired</li>
     *   <li>The pool is at capacity and auto-balancing is disabled</li>
     *   <li>The element would exceed memory constraints (when memory measure is configured)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DBConnection conn = new DBConnection("server1");
     * if (pool.put("database1", conn)) {
     *     System.out.println("Connection added successfully");
     * } else {
     *     System.out.println("Failed to add - pool full or connection expired");
     *     conn.destroy(Caller.PUT_ADD_FAILURE);
     * }
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return {@code true} if the mapping was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or element is null
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean put(final K key, final E value) throws IllegalStateException {
        assertNotClosed();

        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        if (value.activityPrint().isExpired()) {
            return false;
        }

        putCount.incrementAndGet();

        lock.lock();

        try {
            // Make sure the old value is removed regardless if the new value will put successfully or not.
            E oldValue = remove(key);

            if (oldValue != null) {
                destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
            }

            if (pool.size() >= capacity) {
                if (autoBalance) {
                    vacate();
                } else {
                    return false;
                }
            }

            final long keyValueMemorySize = memoryMeasure == null ? 0 : memoryMeasure.sizeOf(key, value);

            if (memoryMeasure != null && keyValueMemorySize < 0) {
                logger.warn("Memory measure returned negative size for key/value: {}", keyValueMemorySize);
                return false;
            }

            if (memoryMeasure != null) {
                if (keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                    if (autoBalance) {
                        vacate();

                        if (keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                            // ignore.
                            return false;
                        }
                    } else {
                        // ignore.
                        return false;
                    }
                }

                oldValue = pool.put(key, value);

                if (oldValue != null) {
                    destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }

                totalDataSize.addAndGet(keyValueMemorySize);
            } else {
                oldValue = pool.put(key, value);

                if (oldValue != null) {
                    destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }
            }

            notEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Associates the specified element with the specified key in this pool,
     * with optional automatic destruction on failure.
     *
     * <p>This is a convenience method that wraps {@link #put(Object, Poolable)} and optionally
     * destroys the element if the put operation fails. The destruction occurs in a finally block
     * to ensure cleanup even if an exception is thrown.</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param autoDestroyOnFailedToPut if {@code true}, calls {@code value.destroy(PUT_ADD_FAILURE)} when put fails
     * @return {@code true} if the mapping was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or element is null
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean put(final K key, final E value, final boolean autoDestroyOnFailedToPut) {
        boolean success = false;

        try {
            success = put(key, value);
        } finally {
            if (autoDestroyOnFailedToPut && !success && value != null) {
                value.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     * Returns the element associated with the specified key, or {@code null} if no mapping exists.
     * If the element has expired, it is removed and destroyed, and {@code null} is returned.
     * The element's activity print is updated to reflect this access.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Retrieves the element associated with the key</li>
     *   <li>Checks if the element has expired</li>
     *   <li>If expired: removes and destroys the element, returns {@code null}</li>
     *   <li>If valid: updates last access time and access count, returns the element</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E cached = pool.get("myKey");
     * if (cached != null) {
     *     // Use cached element
     * } else {
     *     // Create new element and add to pool
     *     E newElement = createElement();
     *     pool.put("myKey", newElement);
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E get(final K key) throws IllegalStateException {
        assertNotClosed();

        E element = null;

        lock.lock();

        try {
            element = pool.get(key);

            if (element != null) {
                final ActivityPrint activityPrint = element.activityPrint();

                if (activityPrint.isExpired()) {
                    pool.remove(key);
                    destroy(key, element, Caller.EVICT);
                    element = null;
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();
                }
            }

            return element;
        } finally {
            lock.unlock();

            if (element != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }
    }

    /**
     * Removes and returns the element associated with the specified key.
     * The element's activity print is updated to reflect this access.
     *
     * <p>Unlike {@link #get(Object)}, this method removes the element from the pool,
     * so it will not be available for future requests unless re-added.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E element = pool.remove("myKey");
     * if (element != null) {
     *     try {
     *         // use the element exclusively
     *     } finally {
     *         element.destroy(Caller.REMOVE_REPLACE_CLEAR);
     *     }
     * }
     * }</pre>
     *
     * @param key the key whose mapping is to be removed
     * @return the element previously associated with the key, or {@code null} if no mapping exists
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E remove(final K key) throws IllegalStateException {
        assertNotClosed();

        E element = null;

        lock.lock();

        try {
            element = pool.remove(key);

            if (element != null) {
                final ActivityPrint activityPrint = element.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                if (memoryMeasure != null) {
                    final long keyValueMemorySize = memoryMeasure.sizeOf(key, element);

                    if (keyValueMemorySize >= 0) {
                        totalDataSize.addAndGet(-keyValueMemorySize); //NOSONAR
                    } else {
                        logger.warn("Memory measure returned negative size for key/value: " + keyValueMemorySize);
                    }
                }

                notFull.signal();
            }

            return element;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the element associated with the specified key without updating access statistics.
     * If the element has expired, it is removed and destroyed, and {@code null} is returned.
     *
     * <p><b>Important Side Effects:</b></p>
     * <ul>
     *   <li><b>Does NOT update</b> last access time - element's access time remains unchanged</li>
     *   <li><b>Does NOT update</b> access count - element's access counter remains unchanged</li>
     *   <li><b>DOES remove and destroy</b> expired elements - if the element has expired, it will be
     *       destroyed and removed from the pool, and {@code null} will be returned</li>
     *   <li><b>Does NOT remove</b> the element from the pool (if valid) - the element remains available
     *       for future requests</li>
     * </ul>
     *
     * <p>Use this method when you need to inspect pool contents for monitoring, debugging, or
     * administrative purposes without affecting the element's eviction priority. If you need to
     * use the element for regular operations, use {@link #get(Object)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if an element exists without affecting its access statistics
     * DBConnection conn = pool.peek("database1");
     * if (conn != null) {
     *     System.out.println("Connection available: " + conn.isActive());
     *     // Connection remains in pool with unchanged access statistics
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E peek(final K key) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            final E element = pool.get(key);

            if (element != null) {
                final ActivityPrint activityPrint = element.activityPrint();

                if (activityPrint.isExpired()) {
                    pool.remove(key);
                    destroy(key, element, Caller.EVICT);

                    return null;
                }
            }

            return element;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this pool contains a mapping for the specified key.
     * This method uses the key's equals method for comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (pool.containsKey("database1")) {
     *     DBConnection conn = pool.get("database1");
     *     // use connection
     * } else {
     *     // create and add new connection
     * }
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> databases = pool.keySet();
     * System.out.println("Pooled databases: " + databases);
     * for (String db : databases) {
     *     System.out.println("Database: " + db);
     * }
     * }</pre>
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
     * Returns a snapshot of the elements contained in this pool.
     * The returned collection is a copy and will not reflect subsequent changes to the pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<DBConnection> connections = pool.values();
     * System.out.println("Total pooled connections: " + connections.size());
     * for (DBConnection conn : connections) {
     *     System.out.println("Connection status: " + conn.isActive());
     * }
     * }</pre>
     *
     * @return a collection containing all elements currently in the pool
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
     * All removed entries are destroyed with the REMOVE_REPLACE_CLEAR reason.
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
     * Cancels the eviction task if scheduled and destroys all pooled entries.
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
     * Scans the pool for expired entries and removes them.
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
     * Destroys a single entry and updates statistics.
     * Updates memory tracking and eviction counts as appropriate, and handles exceptions gracefully.
     *
     * @param key the key part of the entry (used for memory calculation if memoryMeasure is configured)
     * @param value the value of the entry to destroy
     * @param caller the reason for destruction (determines whether eviction count is incremented)
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
                try {
                    final long keyValueMemorySize = memoryMeasure.sizeOf(key, value);

                    if (keyValueMemorySize >= 0) {
                        totalDataSize.addAndGet(-keyValueMemorySize);
                    } else {
                        logger.warn("Memory measure returned negative size for key/value: " + keyValueMemorySize);
                    }
                } catch (final Exception exception) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error measuring memory size during destroy: " + ExceptionUtil.getErrorMessage(exception, true));
                    }
                }
            }

            try {
                value.destroy(caller);
            } catch (final Exception exception) {
                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getErrorMessage(exception, true));
                }
            }
        }
    }

    /**
     * Destroys all entries in the provided map.
     *
     * @param map the map of entries to destroy
     * @param caller the reason for destruction
     */
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
