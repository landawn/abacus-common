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
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Deserialization of a nonempty measured pool requires its stored admission charges; older
 * serialized forms without those charges must be recreated.
 *
 * <p><b>Serialization:</b> a pool is serializable only if every pooled key and value and the
 * configured {@link KeyedObjectPool.MemoryMeasure} (a non-transient field) are
 * {@link java.io.Serializable}; otherwise {@code writeObject} fails with
 * {@link java.io.NotSerializableException}. A lambda measure must be declared with an intersection
 * cast, e.g. {@code (KeyedObjectPool.MemoryMeasure<K, E> & Serializable) (k, v) -> v.size()}.</p>
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

    /** A detached entry whose accounting is complete and whose user callback awaits lock release. */
    private static final class DestroyTask<K, E extends Poolable> {
        @SuppressWarnings("unused")
        final K key;
        final E value;
        final Caller caller;

        DestroyTask(final K key, final E value, final Caller caller) {
            this.key = key;
            this.value = value;
            this.caller = caller;
        }
    }

    /**
     * Optional memory measure for tracking memory usage of key-value pairs.
     */
    private final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure;

    /** Retain the charge of each admitted mapping; removal must not remeasure a mutable value. */
    private Map<K, Long> memoryCharges = new HashMap<>();

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
     * @param memoryMeasure the function to calculate entry memory size; required when {@code maxMemorySize > 0}
     * @throws IllegalArgumentException if capacity, eviction delay, or maximum memory size is negative;
     *         if the balance factor is non-finite or outside [0, 1]; or if a positive memory limit is specified without a memory measure.
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) throws IllegalArgumentException {
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
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be finite and in [0, 1]; 0 selects the default 0.2)
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
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (must be finite and in [0, 1]; 0 selects the default 0.2)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate entry memory size; required when {@code maxMemorySize > 0}
     * @throws IllegalArgumentException if a positive memory limit is specified without a memory measure.
     */
    protected GenericKeyedObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) throws IllegalArgumentException {
        super(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        if (maxMemorySize > 0 && memoryMeasure == null) {
            throw new IllegalArgumentException("A memory measure is required when maxMemorySize is positive: " + maxMemorySize);
        }

        this.memoryMeasure = memoryMeasure;
        pool = new LinkedHashMap<>(Math.min(capacity, 1000));

        cmp = createComparator();
        scheduleEvictionTask();

        // Register shutdown hook AFTER subclass init completes; otherwise a JVM shutdown racing
        // the constructor would invoke close() with a null pool/cmp.
        registerShutdownHook();
    }

    /**
     * Memory is tracked (and reported by {@link #stats()}) whenever a memory measure is configured,
     * even when no positive {@code maxMemorySize} limit is set.
     *
     * @return {@code true} if a memory measure is configured
     */
    @Override
    boolean isMemoryTracked() {
        return memoryMeasure != null;
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
            // Periodically remove expired entries from the pool
            try {
                removeExpired();
            } catch (final Throwable e) { // NOSONAR - an unchecked nonfatal failure must not cancel all future eviction runs
                rethrowIfFatal(e);

                if (logger.isWarnEnabled()) {
                    logger.warn("Error removing expired pooled entries", e);
                }
            }
        };

        scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelayInMillis, evictDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Associates the specified element with the specified key in this pool.
     * If the pool previously contained a mapping for the key, the old element is detached and
     * accounted with {@link Caller#REMOVE_REPLACE_CLEAR} before the new value is inserted. Its
     * destruction callback runs after the pool lock is released, so on a successful replacement
     * the callback can already observe the new mapping. Detachment still happens when the later
     * insertion fails (capacity/memory rejection), so a failing {@code put} can remove the
     * previous mapping for {@code key}.
     * A {@code value} that is already expired on entry is rejected up front (before the lock)
     * without removing or destroying any existing mapping for {@code key}; a value that expires
     * afterwards (during memory measurement or lock acquisition) is rejected in-lock after the
     * previous mapping has been detached, exactly like a capacity or memory rejection. The old
     * element is <em>not</em> destroyed when it is the same instance as {@code value} (re-pooling
     * the same instance simply re-inserts it, and the mapping is restored if that re-pooling fails).
     *
     * <p>The put operation returns {@code false} (does not insert) if:</p>
     * <ul>
     *   <li>The element has already expired (on entry, or by the time it is checked under the lock)</li>
     *   <li>The pool is at capacity and either auto-balancing is disabled, or balancing did not free a slot</li>
     *   <li>The element would exceed memory constraints (when a memory measure is configured) and balancing did not free enough memory</li>
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
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key or value is null.
     */
    @Override
    public boolean put(final K key, final E value) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        if (value.activityPrint().isExpired()) {
            return false;
        }

        final long admissionMemorySize = measureMemory(key, value);

        boolean valueStored = false;
        E removedValue = null;
        List<DestroyTask<K, E>> pendingDestroys = null;
        boolean rePoolingSameInstance = false;
        long sameInstanceMemorySubtracted = 0L;

        lock.lock();

        try {
            // Re-check inside the lock; a concurrent close() between an unlocked check and lock
            // acquisition would otherwise leak this entry.
            assertNotClosed();
            // Remove the old value even if the new value is later rejected. Use the backing map
            // directly: the public remove(K) performs ownership-transfer memory accounting, while
            // this replacement path accounts the detached value as a destruction.
            E oldValue = pool.remove(key);
            removedValue = oldValue;

            // Identity guard: get() does not remove the mapping, so the documented "put it back"
            // pattern re-puts the SAME instance - destroying it would close a live resource and
            // re-pool the corpse.
            rePoolingSameInstance = oldValue != null && oldValue == value;

            if (oldValue != null && oldValue != value) {
                pendingDestroys = appendPendingDestroy(pendingDestroys, key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
            } else if (rePoolingSameInstance && memoryMeasure != null) {
                // Preserve the original admission charge if this same-instance replacement fails.
                sameInstanceMemorySubtracted = removeMemoryCharge(key);
            }

            if (pool.size() >= capacity) {
                if (autoBalance) {
                    pendingDestroys = appendAutoBalanceVictimsUnderLock(pendingDestroys);

                    if (pool.size() >= capacity) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            if (memoryMeasure != null) {
                final long keyValueMemorySize = admissionMemorySize;

                if (keyValueMemorySize < 0) {
                    logger.warn("Memory measure returned negative size for key/value: " + keyValueMemorySize);
                    return false;
                }

                if (keyValueMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                    if (autoBalance) {
                        pendingDestroys = appendAutoBalanceVictimsUnderLock(pendingDestroys);

                        if (keyValueMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                            // ignore.
                            return false;
                        }
                    } else {
                        // ignore.
                        return false;
                    }
                }

                // Re-check expiry inside the lock: time spent measuring, acquiring the lock or selecting victims may have
                // expired the value; pooling it would corrupt hit/miss accounting and expose a
                // doomed entry to the next get()er (mirrors the timed put variant).
                if (value.activityPrint().isExpired()) {
                    return false;
                }

                oldValue = pool.put(key, value);

                if (oldValue != null && oldValue != value) {
                    pendingDestroys = appendPendingDestroy(pendingDestroys, key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }

                recordMemoryCharge(key, keyValueMemorySize);
            } else {
                // Re-check expiry inside the lock after any balancing work, mirroring the timed
                // put variant.
                if (value.activityPrint().isExpired()) {
                    return false;
                }

                oldValue = pool.put(key, value);

                if (oldValue != null && oldValue != value) {
                    pendingDestroys = appendPendingDestroy(pendingDestroys, key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                }
            }

            putCount.incrementAndGet();
            valueStored = true;

            // signalAll (not signal): waiters in get(K, timeout, unit) park on notEmpty for a
            // SPECIFIC key. Waking only one waiter could wake a get(B) waiter for this put of A;
            // it would re-check, still miss B, and re-await — consuming the signal — leaving a
            // concurrent get(A) to spuriously time out. signalAll wakes every key-waiter so each
            // re-checks its own key.
            notEmpty.signalAll();
            notFull.signalAll(); // Same-key producer waiters can now replace this mapping.

            return true;
        } finally {
            // Same-instance re-pool can fail after pool.remove (e.g. sizeOf returns negative, mid-flight
            // expiry). Restore the mapping so the instance is not orphaned (neither pooled nor destroyed).
            try {
                if (rePoolingSameInstance && !valueStored && removedValue != null) {
                    pool.put(key, removedValue);
                    if (memoryMeasure != null) {
                        recordMemoryCharge(key, sameInstanceMemorySubtracted);
                    }
                } else if (removedValue != null && !valueStored) {
                    // Freed a different-key slot up-front but never stored the new value — wake notFull waiters.
                    notFull.signalAll();
                }
            } finally {
                // Keep unlock on an unconditional path even if condition signalling unexpectedly
                // fails (for example, after a future lock/condition implementation change).
                lock.unlock();
            }

            invokeDestroyCallbacks(pendingDestroys);
        }
    }

    /**
     * Associates the specified element with the specified key in this pool,
     * with optional automatic destruction on failure.
     *
     * <p>This is a convenience method that wraps {@link #put(Object, Poolable)} and optionally
     * destroys the element if the put operation fails. The destruction occurs in a finally block
     * to ensure cleanup even if an exception is thrown, unless the same instance remains pooled
     * under any key at the cleanup check. See {@link KeyedObjectPool#put(Object, Poolable, boolean)}
     * for the concurrency rules.</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param autoDestroyOnFailedToPut if {@code true}, calls {@code value.destroy(PUT_ADD_FAILURE)} when put fails,
     *        unless the same instance remains pooled at the cleanup check
     * @return {@code true} if the mapping was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or value is null.
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean put(final K key, final E value, final boolean autoDestroyOnFailedToPut) throws IllegalArgumentException, IllegalStateException {
        boolean success = false;

        try {
            success = put(key, value);
        } finally {
            if (autoDestroyOnFailedToPut && !success && value != null && !containsSameInstance(value)) {
                value.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     * Attempts to associate the value with the key, waiting up to the given timeout for a capacity
     * slot when the pool is full and auto-balancing is disabled. Mirrors
     * {@link GenericObjectPool#add(Poolable, long, TimeUnit)}: the remaining timeout is rechecked
     * after every wakeup, including spurious wakeups.
     *
     * <p>When auto-balancing is enabled (the default for every {@link PoolFactory} overload without
     * an explicit {@code autoBalance} flag), a full pool is first balanced under the lock - a
     * balance-factor share of the existing mappings is detached and destroyed with
     * {@link Caller#VACATE} - and the value is inserted without waiting. Waiting for a slot occurs
     * only when auto-balancing is disabled (or the capacity is {@code 0}).
     * Auto-balance and replacement victims are detached and accounted while locked; their
     * destruction callbacks run after this invocation releases the pool lock.</p>
     *
     * <p>As with {@link #put(Object, Poolable)}, a value that is already expired on entry is rejected
     * before any existing mapping for {@code key} is touched, while a value that expires later
     * (during measurement, lock acquisition or the wait) is rejected after the previous mapping has
     * been detached.</p>
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param timeout the maximum time to wait for a slot
     * @param unit the time unit of the timeout, must not be {@code null}
     * @return {@code true} if the value was added, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key, value, or unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean put(final K key, final E value, final long timeout, final TimeUnit unit)
            throws IllegalStateException, IllegalArgumentException, InterruptedException {
        assertNotClosed();

        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        if (unit == null) {
            throw new IllegalArgumentException("Time unit cannot be null");
        }

        if (value.activityPrint().isExpired()) {
            return false;
        }

        final long admissionMemorySize = measureMemory(key, value);

        long nanos = Math.max(0, unit.toNanos(timeout));

        // Hoisted so the finally can tell whether a same-key slot was freed up-front (oldValue != null)
        // but the new value was never stored (valueStored == false) — in which case a notFull waiter
        // must be woken (see the finally block).
        E oldValue = null;
        boolean valueStored = false;
        List<DestroyTask<K, E>> pendingDestroys = null;
        boolean rePoolingSameInstance = false;
        long sameInstanceMemorySubtracted = 0L;

        final long lockStart = System.nanoTime();
        if (!lock.tryLock(nanos, TimeUnit.NANOSECONDS)) {
            assertNotClosed();
            return false;
        }
        // Initial lock contention consumes the same waiting budget as the condition wait.
        nanos = Math.max(0, nanos - (System.nanoTime() - lockStart));

        try {
            // Re-check closed-state inside the lock; a concurrent close() between an unlocked
            // check and lock acquisition would otherwise leak this entry.
            assertNotClosed();

            while (true) {
                // Re-check inside the loop: a concurrent close()/removeAll() (which signals
                // notFull) emptied the pool; without this check the awakened thread would push
                // the value into the newly-closed pool, leaking it.
                assertNotClosed();

                // A same-key mapping can arrive during a condition wait, making replacement
                // possible even at capacity. Detach its charge before testing the new admission.
                // Make sure the old value is removed regardless of whether the new value will be put
                // successfully or not (mirrors the non-timed put). Use the backing map directly so the
                // public remove(K) ownership-transfer accounting is not combined with destruction accounting.
                oldValue = pool.remove(key);

                // Identity guard: get() does not remove the mapping, so the documented "put it back"
                // pattern re-puts the SAME instance - destroying it would close a live resource.
                rePoolingSameInstance = oldValue != null && oldValue == value;

                if (oldValue != null && oldValue != value) {
                    pendingDestroys = appendPendingDestroy(pendingDestroys, key, oldValue, Caller.REMOVE_REPLACE_CLEAR);
                } else if (rePoolingSameInstance && memoryMeasure != null) {
                    // Preserve the original admission charge if this same-instance replacement fails.
                    sameInstanceMemorySubtracted = removeMemoryCharge(key);
                }

                if ((pool.size() >= capacity) && autoBalance) {
                    pendingDestroys = appendAutoBalanceVictimsUnderLock(pendingDestroys);
                }

                if (pool.size() < capacity) {
                    // Re-check expiry: the value may have expired during the awaitNanos wait below.
                    if (value.activityPrint().isExpired()) {
                        return false;
                    }

                    if (memoryMeasure != null) {
                        final long keyValueMemorySize = admissionMemorySize;

                        if (keyValueMemorySize < 0) {
                            logger.warn("Memory measure returned negative size for key/value: " + keyValueMemorySize);
                            return false;
                        }

                        if (keyValueMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                            if (autoBalance) {
                                pendingDestroys = appendAutoBalanceVictimsUnderLock(pendingDestroys);

                                if (keyValueMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
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

                        final E displacedValue = pool.put(key, value);

                        if (displacedValue != null && displacedValue != value) {
                            pendingDestroys = appendPendingDestroy(pendingDestroys, key, displacedValue, Caller.REMOVE_REPLACE_CLEAR);
                        }

                        recordMemoryCharge(key, keyValueMemorySize);
                    } else {
                        final E displacedValue = pool.put(key, value);

                        if (displacedValue != null && displacedValue != value) {
                            pendingDestroys = appendPendingDestroy(pendingDestroys, key, displacedValue, Caller.REMOVE_REPLACE_CLEAR);
                        }
                    }

                    putCount.incrementAndGet();
                    valueStored = true;
                    // signalAll (not signal): waiters in get(K, timeout, unit) park on notEmpty
                    // for a SPECIFIC key, so wake all key-waiters to let each re-check its own key
                    // (see the matching note in put(K, E)).
                    notEmpty.signalAll();
                    notFull.signalAll(); // Re-check replacement eligibility for every producer key.

                    return true;
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = notFull.awaitNanos(nanos);
            }
        } finally {
            // Same-instance re-pool can fail after pool.remove (e.g. sizeOf returns negative, mid-flight
            // expiry, timeout). Restore the mapping so the instance is not orphaned. For a different
            // replacement that failed after detach, free the slot and wake notFull waiters.
            try {
                if (rePoolingSameInstance && !valueStored && oldValue != null) {
                    pool.put(key, oldValue);
                    if (memoryMeasure != null) {
                        recordMemoryCharge(key, sameInstanceMemorySubtracted);
                    }
                } else if (oldValue != null && !valueStored) {
                    notFull.signalAll();
                }
                // Check after rollback: restoration may consume the slot. Otherwise pass unused
                // capacity onward when this notified producer rejects, throws, or leaves room.
                if (pool.size() < capacity) {
                    notFull.signal();
                }
            } finally {
                // Keep unlock on an unconditional path even if condition signalling unexpectedly
                // fails (for example, after a future lock/condition implementation change).
                lock.unlock();
            }

            invokeDestroyCallbacks(pendingDestroys);
        }
    }

    /**
     * Attempts to associate the value with the key within the timeout, with optional automatic
     * destruction on failure. See {@link KeyedObjectPool#put(Object, Poolable, boolean)} for cleanup
     * ownership rules; the ownership check may wait for the pool lock after timeout or interruption.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param timeout the maximum time to wait for a slot
     * @param unit the time unit of the timeout, must not be {@code null}
     * @param autoDestroyOnFailedToPut if {@code true}, calls {@code value.destroy(PUT_ADD_FAILURE)} if put fails,
     *        unless the same instance remains pooled at the cleanup check
     * @return {@code true} if the value was added, {@code false} otherwise
     * @throws IllegalArgumentException if the key, value, or unit is null.
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean put(final K key, final E value, final long timeout, final TimeUnit unit, final boolean autoDestroyOnFailedToPut)
            throws IllegalArgumentException, IllegalStateException, InterruptedException {
        boolean success = false;

        try {
            success = put(key, value, timeout, unit);
        } finally {
            if (autoDestroyOnFailedToPut && !success && value != null && !containsSameInstance(value)) {
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
     * <p>An expired mapping is detached and accounted while locked. Its destruction callback
     * runs after the pool lock is released.</p>
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
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping ({@code null} is returned and a miss is recorded)
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E get(final K key) throws IllegalStateException {
        assertNotClosed();

        E element = null;
        List<DestroyTask<K, E>> pendingDestroys = null;

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
                    pendingDestroys = appendPendingDestroy(pendingDestroys, key, element, Caller.EVICT);
                    element = null;
                    notFull.signal();
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();
                }
            }

        } finally {
            lock.unlock();
            invokeDestroyCallbacks(pendingDestroys);
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
     * {@link GenericObjectPool#poll(long, TimeUnit)} but keyed: it blocks on {@code notEmpty} until
     * the specific {@code key} is populated (by another thread's put), an expired mapping for the key
     * is skipped (removed+destroyed), or the timeout expires. On success the element stays in the
     * pool (it is not removed) and its activity print is updated.
     * Destruction callbacks for expired mappings run after this invocation releases the pool lock.
     *
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping (the call waits out the timeout, returns {@code null} and records a miss)
     * @param timeout the maximum time to wait for a valid mapping for the key
     * @param unit the time unit of the timeout, must not be {@code null}
     * @return the element associated with the key, or {@code null} if the timeout elapsed
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    @MayReturnNull
    @Override
    public E get(final K key, final long timeout, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException, InterruptedException {
        assertNotClosed();

        if (unit == null) {
            throw new IllegalArgumentException("Time unit cannot be null");
        }

        E element = null;
        List<DestroyTask<K, E>> pendingDestroys = null;
        long nanos = Math.max(0, unit.toNanos(timeout));

        final long lockStart = System.nanoTime();
        if (!lock.tryLock(nanos, TimeUnit.NANOSECONDS)) {
            assertNotClosed();
            missCount.incrementAndGet();
            return null;
        }
        // Initial lock contention consumes the same waiting budget as the condition wait.
        nanos = Math.max(0, nanos - (System.nanoTime() - lockStart));

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
                        pendingDestroys = appendPendingDestroy(pendingDestroys, key, element, Caller.EVICT);
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
            invokeDestroyCallbacks(pendingDestroys);
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
     * <p>Unlike {@link #get(Object)}, {@link #peek(Object)} and {@link GenericObjectPool#poll()},
     * the expiry of the mapping is <em>not</em> checked: an expired element that has not yet been
     * evicted is returned (ownership transferred), its admission charge is released and
     * {@code notFull} waiters are signalled exactly as for a live element, and the pool does not
     * destroy it. Borrowers that care should test {@code element.activityPrint().isExpired()}
     * before using the element.</p>
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
     * @param key the key whose mapping is to be removed; a {@code null} key matches no mapping
     *        ({@code null} is returned)
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

                removeMemoryCharge(key);

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
     * <p>An expired mapping is detached and accounted while locked. Its destruction callback
     * runs after the pool lock is released.</p>
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
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping ({@code null} is returned)
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E peek(final K key) throws IllegalStateException {
        assertNotClosed();

        E element = null;
        List<DestroyTask<K, E>> pendingDestroys = null;

        lock.lock();

        try {
            // Re-check inside the lock: see get() for rationale.
            assertNotClosed();

            element = pool.get(key);

            if (element != null) {
                final ActivityPrint activityPrint = element.activityPrint();

                if (activityPrint.isExpired()) {
                    pool.remove(key);
                    pendingDestroys = appendPendingDestroy(pendingDestroys, key, element, Caller.EVICT);
                    element = null;
                    notFull.signal();
                }
            }
        } finally {
            lock.unlock();
            invokeDestroyCallbacks(pendingDestroys);
        }

        return element;
    }

    /**
     * Returns {@code true} if this pool contains a mapping for the specified key.
     * This method uses the key's equals method for comparison.
     *
     * <p>An expired mapping counts as present until it is evicted (by the scheduled sweep,
     * {@link #evict()}, or a {@code get}/{@code peek} that detects the expiry), so this method
     * may answer {@code true} while the following {@code get(key)} returns {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (pool.containsKey("database1")) {
     *     DBConnection conn = pool.get("database1");   // may still be null if the mapping had expired
     *     // use connection
     * } else {
     *     // create and add new connection
     * }
     * }</pre>
     *
     * @param key the key whose presence in this pool is to be tested; a {@code null} key matches no
     *        mapping ({@code false} is returned)
     * @return {@code true} if this pool contains a mapping for the specified key
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean containsKey(final K key) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            assertNotClosed();
            return pool.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a snapshot of the keys contained in this pool.
     * The returned set is a copy and will not reflect subsequent changes to the pool. Keys of
     * expired mappings that have not yet been evicted are included.
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
            assertNotClosed();
            return N.newHashSet(pool.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a snapshot of the elements contained in this pool.
     * The returned collection is a copy and will not reflect subsequent changes to the pool. Values
     * of expired mappings that have not yet been evicted are included.
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
            assertNotClosed();
            return new ArrayList<>(pool.values());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all mappings from this pool.
     * All removed entries are destroyed with the REMOVE_REPLACE_CLEAR reason.
     * Pool state and accounting are cleared atomically; user destruction callbacks run after the
     * lock is released.
     *
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void clear() throws IllegalStateException {
        assertNotClosed();

        removeAll(Caller.REMOVE_REPLACE_CLEAR, true);
    }

    /**
     * Closes this pool and releases all resources.
     * Cancels the eviction task if scheduled and destroys all pooled entries.
     * This method is idempotent. Concurrent calls are serialized, so when any invocation returns,
     * the invocation that initiated closure has finished all destruction callbacks. Removal and
     * accounting complete under lock before user destruction callbacks are invoked.
     */
    @Override
    public synchronized void close() {
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
     * The victim count, selection, detachment, accounting, and waiter signaling are atomic with
     * respect to other pool operations. User destruction callbacks run after the lock is released.
     *
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void evict() throws IllegalStateException {
        assertNotClosed();

        final Map<K, E> removingObjects;
        lock.lock();
        try {
            assertNotClosed();
            // Count and detach in one critical section. Computing the count and then calling
            // vacate(int) after an unlock allowed an intervening mutation to make the count stale.
            removingObjects = prepareVacateUnderLock(numberToAutoBalance());
        } finally {
            lock.unlock();
        }

        invokeDestroyCallbacks(removingObjects, Caller.VACATE);
    }

    /**
     * Returns the current number of key-value mappings in the pool.
     * Expired mappings that have not yet been evicted are counted.
     *
     * @return the number of mappings currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public int size() throws IllegalStateException {
        assertNotClosed();

        lock.lock();
        try {
            assertNotClosed();
            return pool.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the hash code value for this pool. The hash code is computed from a snapshot of the
     * pool's key-value mappings taken under the pool lock, using {@link java.util.Map#hashCode()}
     * semantics on that snapshot (order-independent, sum of entry hash codes).
     *
     * @return a hash code value for this pool
     */
    @Override
    public int hashCode() {
        return snapshot().hashCode();
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

    private boolean containsSameInstance(final E value) {
        // A reentrant admission may retain this candidate under a different key.
        // Compare identities without key/equality callbacks, then release before destruction.
        lock.lock();

        try {
            for (final E retained : pool.values()) {
                if (retained == value) {
                    return true;
                }
            }
            return false;
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
     * Victims are detached atomically under the pool lock, but user destruction callbacks run
     * after the lock is released. This sized operation is available to subclasses; public
     * {@link #evict()} performs its balance-factor count and detachment in one critical section.
     *
     * @param numberToEvict the number of entries to remove
     */
    protected void vacate(final int numberToEvict) {
        Map<K, E> removingObjects = null;

        lock.lock();
        try {
            assertNotClosed();
            removingObjects = prepareVacateUnderLock(numberToEvict);
        } finally {
            lock.unlock();
        }

        invokeDestroyCallbacks(removingObjects, Caller.VACATE);
    }

    /** Detaches and accounts vacate victims, and signals capacity waiters. The caller must hold {@link #lock}. */
    private Map<K, E> prepareVacateUnderLock(final int numberToEvict) {
        final Map<K, E> removingObjects = detachForVacateUnderLock(numberToEvict);
        accountDestroyAll(removingObjects, Caller.VACATE);

        if (N.notEmpty(removingObjects)) {
            notFull.signalAll();
        }

        return removingObjects;
    }

    /**
     * Selects and detaches vacate victims. The caller must hold {@link #lock}; this method never
     * invokes user code and does not update destruction statistics or memory accounting.
     */
    private Map<K, E> detachForVacateUnderLock(final int numberToEvict) {
        final int size = pool.size();
        Map<K, E> removingObjects = null;

        if (numberToEvict >= size) {
            removingObjects = new HashMap<>(pool);
            pool.clear();
        } else if (numberToEvict > 0) {
            if (evictionPolicy == EvictionPolicy.FIFO) {
                removingObjects = new LinkedHashMap<>(numberToEvict);
                final Iterator<Map.Entry<K, E>> it = pool.entrySet().iterator();

                while (it.hasNext() && removingObjects.size() < numberToEvict) {
                    final Map.Entry<K, E> entry = it.next();
                    removingObjects.put(entry.getKey(), entry.getValue());
                    it.remove();
                }
            } else {
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

                removingObjects = N.newHashMap(heap.size());

                for (final Map.Entry<K, E> entry : heap) {
                    final K key = entry.getKey();
                    final E value = entry.getValue();
                    pool.remove(key);
                    removingObjects.put(key, value);
                }
            }
        }

        return removingObjects;
    }

    private int numberToAutoBalance() {
        if (pool.isEmpty()) {
            return 0;
        }

        return Math.max(1, (int) (pool.size() * balanceFactor));
    }

    /**
     * Scans the pool for expired entries and removes them.
     * This method is called periodically by the scheduled eviction task. Detachment and pool
     * accounting are completed under lock; user destruction callbacks run after unlock.
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
                accountDestroyAll(removingObjects, Caller.EVICT);
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }

        // Phase 2: only user callbacks remain; detachment and accounting are already complete.
        try {
            invokeDestroyCallbacks(removingObjects, Caller.EVICT);
        } finally {
            Objectory.recycle(removingObjects);
        }
    }

    /**
     * Destroys a single entry and updates statistics.
     * Updates memory tracking and eviction counts as appropriate, and handles exceptions gracefully.
     *
     * <p>This hook is <em>not</em> invoked by the pool's own lifecycle paths ({@code get}/{@code peek}
     * of an expired mapping, replacement in {@code put}, {@code removeExpired}, {@code evict}/
     * {@code vacate}, auto-balancing, {@code clear} and {@code close}): those account for detached
     * entries under the pool lock and invoke the {@link Poolable#destroy(Caller)} callbacks after the
     * lock is released. It is intended for subclass-initiated destruction of entries that are already
     * detached from the pool. Because it updates the memory accounting (a non-thread-safe charge map
     * and the total), it must be called either while holding the pool lock or for entries that are no
     * longer pooled; calling it for a key still mapped strips that mapping's admission charge.</p>
     *
     * @param key the key part of the entry (used for memory calculation if memoryMeasure is configured)
     * @param value the value of the entry to destroy
     * @param caller the reason for destruction (determines whether eviction count is incremented)
     */
    protected void destroy(final K key, final E value, final Caller caller) {
        accountDestroy(key, value, caller);
        invokeDestroyCallback(value, caller);
    }

    /** Updates pool-owned accounting without invoking the pooled object's destruction callback. */
    private void accountDestroy(final K key, final E value, final Caller caller) {
        if (caller == Caller.EVICT || caller == Caller.VACATE) {
            evictionCount.incrementAndGet();
        }

        if (value != null) {
            removeMemoryCharge(key);
        }
    }

    // Measure before admission acquires the pool lock: callbacks can reenter or close the pool.
    private long measureMemory(final K key, final E value) {
        if (memoryMeasure == null) {
            return 0;
        }
        try {
            return memoryMeasure.sizeOf(key, value);
        } catch (final Exception e) {
            logger.warn("Error measuring memory size of entry", e);
            return -1;
        }
    }

    private void recordMemoryCharge(final K key, final long charge) {
        memoryCharges.put(key, charge);
        totalDataSize.addAndGet(charge);
    }

    private long removeMemoryCharge(final K key) {
        final Long charge = memoryCharges.remove(key);
        if (charge == null) {
            return 0;
        }
        totalDataSize.addAndGet(-charge);
        return charge;
    }

    /** Invokes the pooled object's destruction callback. Callers must not hold {@link #lock}. */
    private void invokeDestroyCallback(final E value, final Caller caller) {
        if (value != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Destroying cached object " + ClassUtil.getSimpleClassName(value.getClass()) + " with activity print: " + value.activityPrint());
            }

            try {
                value.destroy(caller);
            } catch (final Throwable exception) { // NOSONAR - isolate a broken user callback so remaining resources are still released
                rethrowIfFatal(exception);

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getErrorMessage(exception, true));
                }
            }
        }
    }

    private void accountDestroyAll(final Map<K, E> map, final Caller caller) {
        if (N.notEmpty(map)) {
            for (final Map.Entry<K, E> entry : map.entrySet()) {
                accountDestroy(entry.getKey(), entry.getValue(), caller);
            }
        }
    }

    private void invokeDestroyCallbacks(final Map<K, E> map, final Caller caller) {
        if (N.notEmpty(map)) {
            for (final Map.Entry<K, E> entry : map.entrySet()) {
                invokeDestroyCallback(entry.getValue(), caller);
            }
        }
    }

    private List<DestroyTask<K, E>> appendPendingDestroy(List<DestroyTask<K, E>> pendingDestroys, final K key, final E value, final Caller caller) {
        accountDestroy(key, value, caller);

        if (value != null) {
            if (pendingDestroys == null) {
                pendingDestroys = new ArrayList<>();
            }

            pendingDestroys.add(new DestroyTask<>(key, value, caller));
        }

        return pendingDestroys;
    }

    private List<DestroyTask<K, E>> appendPendingDestroy(List<DestroyTask<K, E>> pendingDestroys, final Map<K, E> values, final Caller caller) {
        if (N.notEmpty(values)) {
            if (pendingDestroys == null) {
                pendingDestroys = new ArrayList<>(values.size());
            }

            for (final Map.Entry<K, E> entry : values.entrySet()) {
                accountDestroy(entry.getKey(), entry.getValue(), caller);

                if (entry.getValue() != null) {
                    pendingDestroys.add(new DestroyTask<>(entry.getKey(), entry.getValue(), caller));
                }
            }

            notFull.signalAll();
        }

        return pendingDestroys;
    }

    private List<DestroyTask<K, E>> appendAutoBalanceVictimsUnderLock(final List<DestroyTask<K, E>> pendingDestroys) {
        return appendPendingDestroy(pendingDestroys, detachForVacateUnderLock(numberToAutoBalance()), Caller.VACATE);
    }

    private void invokeDestroyCallbacks(final List<DestroyTask<K, E>> pendingDestroys) {
        if (N.notEmpty(pendingDestroys)) {
            for (final DestroyTask<K, E> task : pendingDestroys) {
                invokeDestroyCallback(task.value, task.caller);
            }
        }
    }

    /**
     * Destroys all entries in the provided map by calling
     * {@link #destroy(Object, Poolable, Caller)} on each of them.
     *
     * <p>Like {@code destroy}, this hook is not invoked by the pool's own eviction, clear or close
     * paths; it is intended for subclass-initiated destruction of entries already detached from the
     * pool, and must be called while holding the pool lock or for entries no longer pooled because it
     * updates the memory accounting.</p>
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
        removeAll(caller, false);
    }

    private void removeAll(final Caller caller, final boolean requireOpen) {
        // Snapshot, clear, and account under lock; invoke only user callbacks outside. See
        // GenericObjectPool.removeAll for the concurrency rationale.
        final Map<K, E> doomed;
        lock.lock();
        try {
            if (requireOpen) {
                assertNotClosed();
            }

            doomed = new HashMap<>(pool);
            pool.clear();
            // Complete accounting before exposing the newly-empty pool to another put(). If this
            // were deferred with the callbacks, maxMemorySize checks could observe stale usage.
            accountDestroyAll(doomed, caller);
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
        invokeDestroyCallbacks(doomed, caller);
    }

    /**
     * Serializes this pool to an ObjectOutputStream.
     * The pool is locked during serialization to ensure consistency. Every pooled key and value and
     * the configured memory measure are written with the pool, so each must be {@code Serializable};
     * otherwise a {@link java.io.NotSerializableException} is thrown.
     *
     * @param os the output stream
     * @throws IOException if {@code os.defaultWriteObject()} cannot write the pool state, including a nonserializable key or value or
     *         memory measure
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
     * @throws IOException if reading the serialized pool data fails or the stream contains invalid pool state
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Serial
    private void readObject(final ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();

        restoreMemoryAccounting();

        lock = newLock();
        notEmpty = newCondition(lock);
        notFull = newCondition(lock);
        cmp = createComparator();
        // A superclass readObject() runs before a serializable subclass restores its fields.
        // Scheduling here can therefore invoke an overridden removeExpired() on a partially
        // deserialized subclass. Validation defers all external publication until the graph is
        // complete while still restoring the lifecycle before readObject() returns to the caller.
        is.registerValidation(this::restoreTransientLifecycle, 0);
    }

    private void restoreTransientLifecycle() {
        scheduleEvictionTask();

        if (!isClosed) {
            initShutdownHook();
            registerShutdownHook();
        }
    }

    /**
     * @throws InvalidObjectException if a nonempty measured pool has no serialized admission charges,
     *         a charge is negative or the total overflows a long, or the charge keys differ from the keys of a measured pool
     */
    private void restoreMemoryAccounting() throws InvalidObjectException {
        if (memoryCharges == null) {
            if (memoryMeasure != null && !pool.isEmpty()) {
                throw new InvalidObjectException("Serialized measured pool has no admission charges; recreate the pool");
            }
            memoryCharges = new HashMap<>();
        }
        if (memoryMeasure != null && !memoryCharges.keySet().equals(pool.keySet())) {
            throw new InvalidObjectException("Serialized admission charges do not match pool keys");
        }
        long total = 0;
        for (final long charge : memoryCharges.values()) {
            if (charge < 0 || charge > Long.MAX_VALUE - total) {
                throw new InvalidObjectException("Invalid serialized admission charge: " + charge);
            }
            total += charge;
        }
        // Superclass counters are serialized before this class locks its mapping snapshot.
        totalDataSize.set(total);
    }
}
