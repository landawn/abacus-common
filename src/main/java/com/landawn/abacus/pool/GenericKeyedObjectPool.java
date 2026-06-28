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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * This implementation stores key-value mappings in insertion order.
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
 *   <li>CREATED_TIME - Evicts the oldest-created entries first</li>
 *   <li>FIFO - Evicts in insertion order (oldest-added first)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a keyed pool for database connections by schema
 * KeyedObjectPool<String, DBConnection> pool = PoolFactory.createKeyedObjectPool(
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
    transient Comparator<Map.Entry<K, E>> cmp;

    /**
     * Future representing the scheduled eviction task, {@code null} if eviction is disabled.
     */
    transient ScheduledFuture<?> scheduleFuture;

    /**
     * Constructs a new GenericKeyedObjectPool with basic configuration.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelayInMillis, evictionPolicy, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with memory-based constraints.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate entry memory size, or {@code null} if not using memory limits
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        this(capacity, evictDelayInMillis, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Constructs a new GenericKeyedObjectPool with full configuration options.
     *
     * @param capacity the maximum number of entries the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate entry memory size, or {@code null} if not using memory limits
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        super(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        this.memoryMeasure = memoryMeasure;
        pool = new LinkedHashMap<>(Math.min(capacity, 1000));

        cmp = createComparator();
        scheduleEvictionTask();

        // Register shutdown hook AFTER subclass init completes; otherwise a JVM shutdown racing
        // the constructor would invoke close() with a null pool/cmp.
        registerShutdownHook();
    }

    private Comparator<Map.Entry<K, E>> createComparator() {
        switch (evictionPolicy) {
            case LAST_ACCESS_TIME:
                return Comparator.comparingLong(o -> o.getValue().activityPrint().getLastAccessTime());

            case ACCESS_COUNT:
                return Comparator.comparingLong(o -> o.getValue().activityPrint().getAccessCount());

            case EXPIRATION_TIME:
                return Comparator.comparingLong(o -> o.getValue().activityPrint().getExpirationTime());

            case CREATED_TIME:
                return Comparator.comparingLong(o -> o.getValue().activityPrint().getCreatedTime());

            case FIFO:
                return Comparator.comparingLong(o -> o.getValue().activityPrint().getCreatedTime());

            default:
                // Defensive guard: unreachable today since the switch exhaustively covers every
                // EvictionPolicy constant. It exists so that adding a new constant without updating
                // this switch fails loudly at runtime with a clear, specific message.
                throw new IllegalStateException(
                        "No eviction comparator defined for EvictionPolicy." + evictionPolicy.name() + " (createComparator must be updated for new policies)");
        }
    }

    private void scheduleEvictionTask() {
        if (evictDelayInMillis <= 0 || isClosed) {
            return;
        }

        final Runnable evictTask = () -> {
            // Evict from the pool
            try {
                removeExpired();
            } catch (final Exception e) {
                // ignore
                if (logger.isWarnEnabled()) {
                    logger.warn("Error removing expired pooled entries", e);
                }
            }
        };

        scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelayInMillis, evictDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Associates the specified element with the specified key in this pool.
     * If the pool previously contained a mapping for the key, the old element is removed and
     * destroyed with {@link Caller#REMOVE_REPLACE_CLEAR} <em>before</em> the new value is
     * inserted; this happens even when the subsequent in-lock insertion fails (capacity/memory
     * rejection, so a failing {@code put} can still evict the previous mapping for {@code key}).
     * The one exception is an already-expired {@code value}, which is rejected up front (before the
     * lock) without removing or destroying any existing mapping for {@code key}. The old element is
     * <em>not</em> destroyed when it is the same instance as {@code value} (re-pooling the same
     * instance simply re-inserts it).
     *
     * <p>The put operation will fail if:</p>
     * <ul>
     *   <li>The element has already expired</li>
     *   <li>The pool is at capacity and auto-balancing is disabled (or balancing did not free a slot)</li>
     *   <li>The element would exceed memory constraints (when memory measure is configured)</li>
     *   <li>The memory measure returns a negative size or throws an exception</li>
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
     * @throws IllegalArgumentException if the key or value is null
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean put(final K key, final E value) throws IllegalStateException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        assertNotClosed();

        if (value.activityPrint().isExpired()) {
            return false;
        }

        lock.lock();

        try {
            // Re-check inside the lock; a concurrent close() between an unlocked check and lock
            // acquisition would otherwise leak this entry.
            assertNotClosed();
            // Make sure the old value is removed regardless if the new value will put successfully or not.
            // Use pool.remove() directly to avoid double memory subtraction (remove() subtracts memory, and destroy() also subtracts).
            E oldValue = pool.remove(key);

            // Identity guard: get() does not remove the mapping, so the documented "put it back"
            // pattern re-puts the SAME instance - destroying it would close a live resource and
            // re-pool the corpse.
            final boolean rePoolingSameInstance = oldValue != null && oldValue == value;

            if (oldValue != null && oldValue != value) {
                destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
            } else if (rePoolingSameInstance && memoryMeasure != null) {
                // The old mapping's memory is still counted in totalDataSize (pool.remove() above
                // does not adjust it, and destroy() was skipped for the same instance). The success
                // path below re-adds the freshly-measured size, so subtract the previously-counted
                // size here to avoid double-counting when re-pooling the same instance.
                try {
                    final long oldMemorySize = memoryMeasure.sizeOf(key, oldValue);

                    if (oldMemorySize >= 0) {
                        totalDataSize.addAndGet(-oldMemorySize); //NOSONAR
                    } else {
                        logger.warn("Memory measure returned negative size for key/value: " + oldMemorySize);
                    }
                } catch (final Exception ex) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error measuring memory size during put (same-instance re-pool): " + ExceptionUtil.getErrorMessage(ex, true));
                    }
                }
            }

            if (pool.size() >= capacity) {
                if (autoBalance) {
                    evict();

                    if (pool.size() >= capacity) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            if (memoryMeasure != null) {
                long keyValueMemorySize;

                try {
                    keyValueMemorySize = memoryMeasure.sizeOf(key, value);
                } catch (final Exception ex) {
                    logger.warn("Error measuring memory size of entry", ex);
                    return false;
                }

                if (keyValueMemorySize < 0) {
                    logger.warn("Memory measure returned negative size for key/value: {}", keyValueMemorySize);
                    return false;
                }

                if (maxMemorySize > 0 && keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                    if (autoBalance) {
                        evict();

                        if (maxMemorySize > 0 && keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                            // ignore.
                            return false;
                        }
                    } else {
                        // ignore.
                        return false;
                    }
                }

                // Re-check expiry inside the lock: time spent in sizeOf()/evict() above may have
                // expired the value; pooling it would corrupt hit/miss accounting and expose a
                // doomed entry to the next get()er (mirrors the timed put variant).
                if (value.activityPrint().isExpired()) {
                    return false;
                }

                oldValue = pool.put(key, value);

                if (oldValue != null && oldValue != value) {
                    destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }

                totalDataSize.addAndGet(keyValueMemorySize);
            } else {
                // Re-check expiry inside the lock (the in-lock evict() above may run slow destroy
                // callbacks), mirroring the timed put variant.
                if (value.activityPrint().isExpired()) {
                    return false;
                }

                oldValue = pool.put(key, value);

                if (oldValue != null && oldValue != value) {
                    destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }
            }

            putCount.incrementAndGet();

            // signalAll (not signal): waiters in get(K, timeout, unit) park on notEmpty for a
            // SPECIFIC key. Waking only one waiter could wake a get(B) waiter for this put of A;
            // it would re-check, still miss B, and re-await — consuming the signal — leaving a
            // concurrent get(A) to spuriously time out. signalAll wakes every key-waiter so each
            // re-checks its own key.
            notEmpty.signalAll();

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
     * @throws IllegalArgumentException if the key or value is null
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
     * Attempts to associate the value with the key, waiting up to the given timeout for a capacity
     * slot when the pool is full. Mirrors {@link GenericObjectPool#add(Poolable, long, TimeUnit)}:
     * a maxSpins (10,000) safety limit guards against pathological wakeup loops; under normal
     * operation the timeout triggers first.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param timeout the maximum time to wait for a slot
     * @param unit the time unit of the timeout
     * @return {@code true} if the value was added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or value is null
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean put(final K key, final E value, final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        assertNotClosed();

        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        if (value.activityPrint().isExpired()) {
            return false;
        }

        long nanos = unit.toNanos(timeout);

        // Hoisted so the finally can tell whether a same-key slot was freed up-front (oldValue != null)
        // but the new value was never stored (valueStored == false) — in which case a notFull waiter
        // must be woken (see the finally block).
        E oldValue = null;
        boolean valueStored = false;

        lock.lock();

        try {
            // Re-check closed-state inside the lock; a concurrent close() between an unlocked
            // check and lock acquisition would otherwise leak this entry.
            assertNotClosed();

            // Make sure the old value is removed regardless of whether the new value will be put
            // successfully or not (mirrors the non-timed put). Use pool.remove() directly to avoid
            // double memory subtraction (remove() subtracts memory, and destroy() also subtracts).
            oldValue = pool.remove(key);

            // Identity guard: get() does not remove the mapping, so the documented "put it back"
            // pattern re-puts the SAME instance - destroying it would close a live resource.
            final boolean rePoolingSameInstance = oldValue != null && oldValue == value;

            if (oldValue != null && oldValue != value) {
                destroy(key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
            } else if (rePoolingSameInstance && memoryMeasure != null) {
                // The old mapping's memory is still counted in totalDataSize; the success path below
                // re-adds the freshly-measured size, so subtract the previously-counted size here to
                // avoid double-counting when re-pooling the same instance.
                try {
                    final long oldMemorySize = memoryMeasure.sizeOf(key, oldValue);

                    if (oldMemorySize >= 0) {
                        totalDataSize.addAndGet(-oldMemorySize); //NOSONAR
                    } else {
                        logger.warn("Memory measure returned negative size for key/value: " + oldMemorySize);
                    }
                } catch (final Exception ex) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error measuring memory size during put (same-instance re-pool): " + ExceptionUtil.getErrorMessage(ex, true));
                    }
                }
            }

            if ((pool.size() >= capacity) && autoBalance) {
                evict();
            }

            int maxSpins = 10000;

            while (maxSpins-- > 0) {
                // Re-check inside the loop: a concurrent close()/removeAll() (which signals
                // notFull) emptied the pool; without this check the awakened thread would push
                // the value into the newly-closed pool, leaking it.
                assertNotClosed();

                if (pool.size() < capacity) {
                    // Re-check expiry: the value may have expired during the awaitNanos wait below.
                    if (value.activityPrint().isExpired()) {
                        return false;
                    }

                    if (memoryMeasure != null) {
                        final long keyValueMemorySize;

                        try {
                            keyValueMemorySize = memoryMeasure.sizeOf(key, value);
                        } catch (final Exception ex) {
                            logger.warn("Error measuring memory size of entry", ex);
                            return false;
                        }

                        if (keyValueMemorySize < 0) {
                            logger.warn("Memory measure returned negative size for key/value: {}", keyValueMemorySize);
                            return false;
                        }

                        if (maxMemorySize > 0 && keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                            if (autoBalance) {
                                evict();

                                if (maxMemorySize > 0 && keyValueMemorySize > maxMemorySize - totalDataSize.get()) {
                                    // ignore.
                                    return false;
                                }
                            } else {
                                // ignore.
                                return false;
                            }
                        }

                        if (value.activityPrint().isExpired()) {
                            return false;
                        }

                        pool.put(key, value);

                        totalDataSize.addAndGet(keyValueMemorySize); //NOSONAR
                    } else {
                        pool.put(key, value);
                    }

                    putCount.incrementAndGet();
                    valueStored = true;
                    // signalAll (not signal): waiters in get(K, timeout, unit) park on notEmpty
                    // for a SPECIFIC key, so wake all key-waiters to let each re-check its own key
                    // (see the matching note in put(K, E)).
                    notEmpty.signalAll();

                    return true;
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = notFull.awaitNanos(nanos);
            }

            return false; // Safety timeout after max spins
        } finally {
            // If an existing same-key mapping was removed up-front (freeing a slot) but the new value
            // was never stored (early bail, timeout, or exception), the net effect is a freed slot.
            // Wake a put(...) waiter parked on notFull so the slot is not silently withheld. Signalled
            // under the still-held lock, before unlock. (The success path leaves the slot count net-zero
            // and instead signals notEmpty.)
            if (oldValue != null && !valueStored) {
                notFull.signalAll();
            }

            lock.unlock();
        }
    }

    /**
     * Attempts to associate the value with the key within the timeout, with optional automatic
     * destruction on failure.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param timeout the maximum time to wait for a slot
     * @param unit the time unit of the timeout
     * @param autoDestroyOnFailedToPut if {@code true}, calls value.destroy(PUT_ADD_FAILURE) if put fails
     * @return {@code true} if the value was added, {@code false} otherwise
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean put(final K key, final E value, final long timeout, final TimeUnit unit, final boolean autoDestroyOnFailedToPut)
            throws InterruptedException {
        boolean success = false;

        try {
            success = put(key, value, timeout, unit);
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
            // Re-check inside the lock: a concurrent close() could have set isClosed and
            // started removeAll() between the unlocked check above and our lock acquisition.
            // Without this check, get() would return a "live" element from a closed pool that
            // close()'s pre-snapshot will not destroy.
            assertNotClosed();

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

        } finally {
            lock.unlock();
        }

        // Only account hit/miss on a normal completion. If the body threw (e.g. a
        // concurrent close() made assertNotClosed() raise IllegalStateException), the
        // call neither hit nor missed the pool and must not skew the statistics.
        if (element != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }

        return element;
    }

    /**
     * Returns the element associated with the specified key, waiting up to the given timeout for a
     * non-expired mapping for that key to become available. Mirrors
     * {@link GenericObjectPool#take(long, TimeUnit)} but keyed: it blocks on {@code notEmpty} until
     * the specific {@code key} is populated (by another thread's put), an expired mapping for the key
     * is skipped (removed+destroyed), or the timeout expires. On success the element stays in the
     * pool (it is not removed) and its activity print is updated.
     *
     * @param key the key whose associated element is to be returned
     * @param timeout the maximum time to wait for a valid mapping for the key
     * @param unit the time unit of the timeout
     * @return the element associated with the key, or {@code null} if the timeout elapsed
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @MayReturnNull
    @Override
    public E get(final K key, final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        assertNotClosed();

        E element = null;
        long nanos = unit.toNanos(timeout);

        lock.lock();

        try {
            getLoop: while (true) {
                // Re-check on every iteration: a concurrent close()/removeAll() now signals
                // notEmpty.signalAll(), so a waiter parked on awaitNanos wakes up and must
                // notice the closed state instead of looping back to wait again.
                assertNotClosed();

                element = pool.get(key);

                if (element != null) {
                    final ActivityPrint activityPrint = element.activityPrint();

                    if (activityPrint.isExpired()) {
                        // Expired mapping for the key: remove+destroy and keep waiting for a
                        // fresh one until the timeout.
                        pool.remove(key);
                        destroy(key, element, Caller.EVICT);
                        element = null;
                        notFull.signal();
                    } else {
                        activityPrint.updateLastAccessTime();
                        activityPrint.updateAccessCount();
                        break getLoop;
                    }
                }

                if (nanos <= 0) {
                    break getLoop;
                }

                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }

        // Only account hit/miss on a normal completion (see get(K)).
        if (element != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }

        return element;
    }

    /**
     * Removes and returns the element associated with the specified key.
     * The element's activity print is updated (last access time and access count)
     * to reflect this access.
     *
     * <p>Unlike {@link #get(Object)}, this method removes the element from the pool,
     * so it will not be available for future requests unless re-added. The pool does
     * <em>not</em> invoke {@link Poolable#destroy(Caller)} on the returned element; the
     * caller takes ownership and is responsible for destroying it when no longer needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E element = pool.remove("myKey");
     * if (element != null) {
     *     try {
     *         // use the element exclusively
     *     } finally {
     *         // The caller owns the removed element; remove() does not destroy it.
     *         element.destroy(Caller.OTHER_EXTERNAL);
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
            // Re-check inside the lock: see get() for rationale.
            assertNotClosed();

            element = pool.remove(key);

            if (element != null) {
                final ActivityPrint activityPrint = element.activityPrint();
                activityPrint.updateLastAccessTime();
                activityPrint.updateAccessCount();

                if (memoryMeasure != null) {
                    // Wrap memoryMeasure call: if a user-supplied sizeOf throws after we've already
                    // removed the entry from the map, the exception would propagate while leaving
                    // the entry neither in the pool nor returned to the caller — a pure leak. Log
                    // and continue with totalDataSize unchanged; better to drift one accounting
                    // unit than leak a live resource.
                    try {
                        final long keyValueMemorySize = memoryMeasure.sizeOf(key, element);

                        if (keyValueMemorySize >= 0) {
                            totalDataSize.addAndGet(-keyValueMemorySize); //NOSONAR
                        } else {
                            logger.warn("Memory measure returned negative size for key/value: " + keyValueMemorySize);
                        }
                    } catch (final Exception ex) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Error measuring memory size during remove: " + ExceptionUtil.getErrorMessage(ex, true));
                        }
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
            // Re-check inside the lock: see get() for rationale.
            assertNotClosed();

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
        lock.lock();

        try {
            if (isClosed) {
                return;
            }

            isClosed = true;
        } finally {
            lock.unlock();
        }

        removeShutdownHook();

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
    public void evict() throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            vacate(numberToAutoBalance()); // NOSONAR

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current number of key-value mappings in the pool.
     *
     * @return the number of mappings currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public int size() throws IllegalStateException {
        assertNotClosed();

        lock.lock();
        try {
            return pool.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the hash code value for this pool.
     * The hash code is based on the internal pool map.
     *
     * @return a hash code value for this pool
     */
    @Override
    public int hashCode() {
        lock.lock();
        try {
            return pool.hashCode();
        } finally {
            lock.unlock();
        }
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
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof GenericKeyedObjectPool)) {
            return false;
        }

        // Snapshot both maps under their OWN locks before comparing. Reading the other pool's
        // map while only holding this.lock is a concurrent-modification hazard: the other pool
        // can be mutated by an unrelated thread, producing ConcurrentModificationException or
        // a torn comparison. Locking both pools simultaneously would also risk deadlock if two
        // threads called a.equals(b) and b.equals(a) at the same time, so we snapshot
        // independently.
        final Map<K, E> snapshot = snapshot();
        final Map<K, E> otherSnapshot = ((GenericKeyedObjectPool<K, E>) obj).snapshot();

        return N.equals(snapshot, otherSnapshot);
    }

    private Map<K, E> snapshot() {
        lock.lock();
        try {
            return new HashMap<>(pool);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a string representation of this pool that summarizes its configuration
     * (capacity, eviction delay, eviction policy, auto-balance, balance factor, memory limit,
     * configured memory measure) and current total data size. The actual pooled entries are
     * <em>not</em> included in the output.
     *
     * @return a string representation of this pool's configuration and total data size
     */
    @Override
    public String toString() {
        return "{pool=GenericKeyedObjectPool, capacity=" + capacity + ", evictDelayInMillis=" + evictDelayInMillis + ", evictionPolicy=" + evictionPolicy
                + ", autoBalance=" + autoBalance + ", balanceFactor=" + balanceFactor + ", maxMemorySize=" + maxMemorySize + ", memoryMeasure=" + memoryMeasure
                + ", totalDataSize=" + totalDataSize.get() + "}";
    }

    /**
     * Removes (vacates) the specified number of entries from the pool based on the eviction policy.
     * This is the sized counterpart to the public no-arg {@link #evict()} (which removes a
     * balance-factor fraction); it removes <em>exactly</em> {@code numberToEvict} entries, choosing
     * victims via the configured {@link EvictionPolicy}. Destroyed entries use {@link Caller#VACATE}.
     * This method is called internally during eviction operations.
     *
     * @param numberToEvict the number of entries to remove
     */
    protected void vacate(final int numberToEvict) {
        final int size = pool.size();

        if (numberToEvict >= size) {
            destroyAll(new HashMap<>(pool), Caller.VACATE);
            pool.clear();
        } else if (numberToEvict > 0) {
            if (evictionPolicy == EvictionPolicy.FIFO) {
                final Map<K, E> removingObjects = new LinkedHashMap<>(numberToEvict);
                final Iterator<Map.Entry<K, E>> it = pool.entrySet().iterator();

                while (it.hasNext() && removingObjects.size() < numberToEvict) {
                    final Map.Entry<K, E> entry = it.next();
                    removingObjects.put(entry.getKey(), entry.getValue());
                    it.remove();
                }

                destroyAll(removingObjects, Caller.VACATE);
                return;
            }

            final Comparator<Map.Entry<K, E>> reversedCmp = cmp.reversed();
            final Queue<Map.Entry<K, E>> heap = new PriorityQueue<>(numberToEvict, reversedCmp);

            for (final Map.Entry<K, E> entry : pool.entrySet()) {
                if (heap.size() < numberToEvict) {
                    heap.offer(entry);
                } else if (cmp.compare(entry, heap.peek()) < 0) {
                    heap.poll();
                    heap.offer(entry);
                }
            }

            final Map<K, E> removingObjects = N.newHashMap(heap.size());

            for (final Map.Entry<K, E> entry : heap) {
                final K key = entry.getKey();
                final E value = entry.getValue();
                pool.remove(key);
                removingObjects.put(key, value);
            }

            destroyAll(removingObjects, Caller.VACATE);
        }
    }

    private int numberToAutoBalance() {
        if (pool.isEmpty()) {
            return 0;
        }

        return Math.max(1, (int) (pool.size() * balanceFactor));
    }

    /**
     * Scans the pool for expired entries and removes them.
     * This method is called periodically by the scheduled eviction task.
     */
    @SuppressWarnings({ "null", "deprecation" })
    protected void removeExpired() {
        // Phase 1: under the lock, collect expired entries and remove them from the pool.
        Map<K, E> removingObjects = null;
        lock.lock();
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
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }

        // Phase 2: destroy outside the lock so user destroy() doesn't stall the whole pool.
        try {
            if (N.notEmpty(removingObjects)) {
                destroyAll(removingObjects, Caller.EVICT);
            }
        } finally {
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
                logger.debug("Destroying cached object {} with activity print: {}", ClassUtil.getSimpleClassName(value.getClass()), value.activityPrint());
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
        // Snapshot under lock; destroy outside. See GenericObjectPool.removeAll for rationale.
        final Map<K, E> doomed;
        lock.lock();
        try {
            doomed = new HashMap<>(pool);
            pool.clear();
            // Wake BOTH condition queues: notFull for parked put(...) waiters AND notEmpty for
            // parked timed get(key, timeout) waiters. The timed-get loop re-checks assertNotClosed()
            // only when woken, so without notEmpty.signalAll() a get(key, longTimeout) thread would
            // block until its full timeout after close()/clear() instead of failing fast. Mirrors
            // GenericObjectPool.removeAll(), which signals both conditions for the same reason.
            notFull.signalAll();
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
        destroyAll(doomed, caller);
    }

    /**
     * Serializes this pool to an ObjectOutputStream.
     * The pool is locked during serialization to ensure consistency.
     *
     * @param os the output stream
     * @throws IOException if an I/O error occurs
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
     * Deserializes this pool from an ObjectInputStream and reinitializes transient fields
     * (lock, conditions, comparator, and eviction task).
     *
     * @param is the input stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Serial
    private void readObject(final ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();

        lock = newLock();
        notEmpty = newCondition(lock);
        notFull = newCondition(lock);
        cmp = createComparator();
        scheduleEvictionTask();

        if (!isClosed) {
            initShutdownHook();
            registerShutdownHook();
        }
    }
}
