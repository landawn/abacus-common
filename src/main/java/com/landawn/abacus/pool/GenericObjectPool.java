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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * A generic implementation of ObjectPool that stores poolable objects in a LIFO (Last-In-First-Out) structure.
 * This implementation uses an ArrayDeque internally for efficient add/remove operations at the head.
 * Deserialization of a nonempty measured pool requires its stored admission charges; older
 * serialized forms without those charges must be recreated.
 *
 * <p><b>Serialization:</b> a pool is serializable only if every pooled element and the configured
 * {@link ObjectPool.MemoryMeasure} (a non-transient field) are {@link java.io.Serializable};
 * otherwise {@code writeObject} fails with {@link java.io.NotSerializableException}. A lambda
 * measure must be declared with an intersection cast, e.g.
 * {@code (ObjectPool.MemoryMeasure<E> & Serializable) e -> e.size()}.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Thread-safe operations using ReentrantLock</li>
 *   <li>Automatic eviction of expired objects based on configurable policies</li>
 *   <li>Memory-based capacity constraints when configured with MemoryMeasure</li>
 *   <li>Auto-balancing to maintain optimal pool size</li>
 *   <li>Comprehensive statistics tracking</li>
 * </ul>
 *
 * <p>The pool can be configured with different eviction policies:
 * <ul>
 *   <li>LAST_ACCESS_TIME - Evicts least recently accessed objects</li>
 *   <li>ACCESS_COUNT - Evicts least frequently accessed objects</li>
 *   <li>EXPIRATION_TIME - Evicts objects closest to expiration</li>
 *   <li>CREATED_TIME - Evicts the oldest-created objects first</li>
 *   <li>FIFO - Evicts in insertion order (oldest-added first)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a pool with capacity 50, 5-minute eviction delay
 * ObjectPool<MyResource> pool = PoolFactory.createObjectPool(
 *     50, 300000, EvictionPolicy.LAST_ACCESS_TIME
 * );
 *
 * // Add resources
 * pool.add(new MyResource());
 *
 * // Poll and use resources (poll() returns null when the pool is empty; add(null) throws)
 * MyResource resource = pool.poll();
 * if (resource != null) {
 *     try {
 *         // use resource
 *     } finally {
 *         pool.add(resource);   // adds it back to the pool
 *     }
 * }
 * }</pre>
 *
 * @param <E> the type of elements in this pool, must implement Poolable
 * @see ObjectPool
 * @see AbstractPool
 * @see PoolFactory
 */
public class GenericObjectPool<E extends Poolable> extends AbstractPool implements ObjectPool<E> {

    @Serial
    private static final long serialVersionUID = -5055744987721643286L;

    /**
     * Optional memory measure for tracking memory usage of pooled objects.
     */
    private final ObjectPool.MemoryMeasure<E> memoryMeasure;

    /** Each admission has its own charge, including repeated admissions of the same instance.
     * Charges follow deque order: polling removes the first and FIFO eviction removes the last. */
    private IdentityHashMap<E, Deque<Long>> memoryCharges = new IdentityHashMap<>();

    /**
     * Internal storage for pooled objects using LIFO ordering.
     */
    final Deque<E> pool;

    /**
     * Comparator used to determine eviction order based on the configured eviction policy.
     */
    transient Comparator<E> cmp;

    /**
     * Future representing the scheduled eviction task, {@code null} if eviction is disabled.
     */
    transient ScheduledFuture<?> scheduleFuture;

    /**
     * Constructs a new GenericObjectPool with basic configuration.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelayInMillis, evictionPolicy, 0, null);
    }

    /**
     * Constructs a new GenericObjectPool with memory-based constraints.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate object memory size; required when {@code maxMemorySize > 0}
     * @throws IllegalArgumentException if capacity, eviction delay, or maximum memory size is negative;
     *         if the balance factor is non-finite or outside [0, 1]; or if a positive memory limit is specified without a memory measure.
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final ObjectPool.MemoryMeasure<E> memoryMeasure) throws IllegalArgumentException {
        this(capacity, evictDelayInMillis, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Constructs a new GenericObjectPool with auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be finite and in [0, 1]; 0 selects the default 0.2)
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Constructs a new GenericObjectPool with full configuration options.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be finite and in [0, 1]; 0 selects the default 0.2)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate object memory size; required when {@code maxMemorySize > 0}
     * @throws IllegalArgumentException if a positive memory limit is specified without a memory measure.
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) throws IllegalArgumentException {
        super(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        if (maxMemorySize > 0 && memoryMeasure == null) {
            throw new IllegalArgumentException("A memory measure is required when maxMemorySize is positive: " + maxMemorySize);
        }

        this.memoryMeasure = memoryMeasure;
        pool = new ArrayDeque<>(Math.max(1, Math.min(capacity, 1000)));

        cmp = createComparator();
        scheduleEvictionTask();

        // Register shutdown hook AFTER all subclass state is fully initialized so a JVM shutdown
        // racing the constructor cannot invoke close() against a null pool/cmp.
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

    private Comparator<E> createComparator() {
        switch (evictionPolicy) {
            // =============================================== For Priority Queue
            case LAST_ACCESS_TIME:
                return Comparator.comparingLong(o -> o.activityPrint().getLastAccessTime());

            case ACCESS_COUNT:
                return Comparator.comparingLong(o -> o.activityPrint().getAccessCount());

            case EXPIRATION_TIME:
                return Comparator.comparingLong(o -> o.activityPrint().getExpirationTime());

            case CREATED_TIME:
                return Comparator.comparingLong(o -> o.activityPrint().getCreatedTime());

            case FIFO:
                return Comparator.comparingLong(o -> o.activityPrint().getCreatedTime());

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
            // Periodically remove expired objects from the pool
            try {
                removeExpired();
            } catch (final Throwable e) { // NOSONAR - an unchecked nonfatal failure must not cancel all future eviction runs
                rethrowIfFatal(e);

                if (logger.isWarnEnabled()) {
                    logger.warn("Error removing expired pooled objects", e);
                }
            }
        };

        scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelayInMillis, evictDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds an object to the pool.
     * The object is added to the head of the internal deque for LIFO ordering.
     *
     * <p>The add operation returns {@code false} (does not insert) when:</p>
     * <ul>
     *   <li>The object has already expired</li>
     *   <li>The pool is at capacity and either auto-balancing is disabled, or balancing did not free a slot</li>
     *   <li>The object would exceed memory constraints (when memory measure is configured) and balancing did not free enough memory</li>
     *   <li>The memory measure returns a negative size or throws an exception</li>
     * </ul>
     *
     * <p>When auto-balancing removes victims, their pool state and accounting are updated under
     * the pool lock. Their destruction callbacks run only after the lock is released; on a
     * successful add, the newly added object is already visible to a reentrant callback.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyPoolable obj = new MyPoolable();
     * if (pool.add(obj)) {
     *     System.out.println("Object added successfully");
     * } else {
     *     System.out.println("Failed to add - pool full or object expired");
     *     obj.destroy(Caller.PUT_ADD_FAILURE);
     * }
     * }</pre>
     *
     * @param element the object to add, must not be {@code null}
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the element is null.
     */
    @Override
    public boolean add(final E element) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        if (element.activityPrint().isExpired()) {
            return false;
        }

        final long admissionMemorySize = measureMemory(element);

        List<E> pendingVacated = null;

        lock.lock();

        try {
            // Re-check inside the lock: a concurrent close() could have set isClosed between
            // the prior unlocked check and our lock acquisition, leaking this element.
            assertNotClosed();

            if (pool.size() >= capacity) {
                if (autoBalance) {
                    pendingVacated = appendPendingDestroy(pendingVacated, detachForVacateUnderLock(numberToAutoBalance()), Caller.VACATE);

                    if (pool.size() >= capacity) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            if (memoryMeasure != null) {
                final long elementMemorySize = admissionMemorySize;

                if (elementMemorySize < 0) {
                    logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                    return false;
                }

                if (elementMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                    if (autoBalance) {
                        pendingVacated = appendPendingDestroy(pendingVacated, detachForVacateUnderLock(numberToAutoBalance()), Caller.VACATE);

                        if (elementMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                            // ignore.
                            return false;
                        }
                    } else {
                        // ignore.
                        return false;
                    }
                }

                // Re-check expiry inside the lock: time spent measuring, acquiring the lock or selecting victims may have
                // expired the element; pushing it would corrupt hit/miss accounting and expose
                // a doomed object to the next poller (mirrors the timed add variant).
                if (element.activityPrint().isExpired()) {
                    return false;
                }

                pool.push(element);

                recordMemoryCharge(element, elementMemorySize);
            } else {
                // Re-check expiry inside the lock after any balancing work, mirroring the timed add variant.
                if (element.activityPrint().isExpired()) {
                    return false;
                }

                pool.push(element);
            }

            putCount.incrementAndGet();

            notEmpty.signal();

            return true;
        } finally {
            lock.unlock();
            invokeDestroyCallbacks(pendingVacated, Caller.VACATE);
        }
    }

    /**
     * Adds an object to the pool with optional automatic destruction on failure.
     * See {@link ObjectPool#add(Poolable, boolean)} for cleanup ownership and concurrency rules.
     *
     * @param element the object to add, must not be {@code null}
     * @param autoDestroyOnFailedToAdd if {@code true}, destroys a rejected non-null element unless
     *        that same instance remains pooled at the cleanup check
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the element is null.
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean add(final E element, final boolean autoDestroyOnFailedToAdd) throws IllegalArgumentException, IllegalStateException {
        boolean success = false;

        try {
            success = add(element);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && element != null && !containsSameInstance(element)) {
                element.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     * Attempts to add an object to the pool within the specified timeout period.
     * This method blocks until space becomes available, the timeout expires, or the thread is interrupted.
     *
     * <p>When auto-balancing is enabled (the default for every {@link PoolFactory} overload without an
     * explicit {@code autoBalance} flag), a full pool is first <em>balanced</em> under the lock - a
     * balance-factor share of the existing elements is detached and destroyed with
     * {@link Caller#VACATE} - and the element is inserted without waiting. Waiting for space occurs
     * only when auto-balancing is disabled (or the capacity is {@code 0}). Auto-balance victims are
     * detached and accounted while locked, but their destruction callbacks are deferred until this
     * invocation releases the pool lock.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyPoolable obj = new MyPoolable();
     * // Wait up to 5 seconds to add the object
     * if (pool.add(obj, 5, TimeUnit.SECONDS)) {
     *     System.out.println("Object added successfully");
     * } else {
     *     System.out.println("Failed to add - pool full or timeout");
     *     obj.destroy(Caller.PUT_ADD_FAILURE);
     * }
     * }</pre>
     *
     * @param element the object to add, must not be {@code null}
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @return {@code true} if successful; {@code false} if the timeout elapsed before space was
     *         available, the element was already (or became) expired, the memory measure rejected the element (returned
     *         a negative size, threw, or the element would exceed {@code maxMemorySize} and
     *         balancing did not free enough memory)
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the element or unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean add(final E element, final long timeout, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException, InterruptedException {
        assertNotClosed();

        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        if (unit == null) {
            throw new IllegalArgumentException("Time unit cannot be null");
        }

        if (element.activityPrint().isExpired()) {
            return false;
        }

        final long admissionMemorySize = measureMemory(element);

        long nanos = Math.max(0, unit.toNanos(timeout));

        List<E> pendingVacated = null;

        final long lockStart = System.nanoTime();
        if (!lock.tryLock(nanos, TimeUnit.NANOSECONDS)) {
            assertNotClosed();
            return false;
        }
        // Initial lock contention consumes the same waiting budget as the condition wait.
        nanos = Math.max(0, nanos - (System.nanoTime() - lockStart));

        try {
            // Re-check closed-state inside the lock; a concurrent close() between an unlocked
            // check and lock acquisition would otherwise leak this element.
            assertNotClosed();

            if ((pool.size() >= capacity) && autoBalance) {
                pendingVacated = appendPendingDestroy(pendingVacated, detachForVacateUnderLock(numberToAutoBalance()), Caller.VACATE);
            }

            while (true) {
                // Re-check inside the loop: a concurrent close() that ran while this thread was
                // parked on notFull.awaitNanos() (now signaled by removeAll()) emptied the pool;
                // without this check the awakened thread would happily push the element into the
                // newly-closed pool, leaking it (its destroy() never fires on close()).
                assertNotClosed();

                if (pool.size() < capacity) {
                    // Re-check expiry: the element may have expired during the awaitNanos wait
                    // below. Pushing an expired element corrupts hit/miss accounting and exposes
                    // a doomed object to the next poller.
                    if (element.activityPrint().isExpired()) {
                        return false;
                    }

                    if (memoryMeasure != null) {
                        final long elementMemorySize = admissionMemorySize;

                        if (elementMemorySize < 0) {
                            logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                            return false;
                        }

                        if (elementMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                            if (autoBalance) {
                                pendingVacated = appendPendingDestroy(pendingVacated, detachForVacateUnderLock(numberToAutoBalance()), Caller.VACATE);

                                if (elementMemorySize > (maxMemorySize > 0 ? maxMemorySize : Long.MAX_VALUE) - totalDataSize.get()) {
                                    // ignore.
                                    return false;
                                }
                            } else {
                                // ignore.
                                return false;
                            }
                        }

                        // Re-check expiry inside the lock: time spent measuring, acquiring the lock or selecting victims
                        // could have expired the element; pushing it
                        // would corrupt hit/miss accounting and expose a doomed object to the next poller
                        // (mirrors the non-timed add variant).
                        if (element.activityPrint().isExpired()) {
                            return false;
                        }

                        pool.push(element);

                        recordMemoryCharge(element, elementMemorySize);
                    } else {
                        pool.push(element);
                    }

                    putCount.incrementAndGet();
                    notEmpty.signal();

                    return true;
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = notFull.awaitNanos(nanos);
            }
        } finally {
            try {
                // A notified producer can reject its candidate without consuming the free slot.
                // Pass unused capacity onward so another producer need not wait for its timeout.
                if (pool.size() < capacity) {
                    notFull.signal();
                }
            } finally {
                lock.unlock();
            }
            invokeDestroyCallbacks(pendingVacated, Caller.VACATE);
        }
    }

    /**
     * Attempts to add an object to the pool with timeout and automatic destruction on failure.
     * See {@link ObjectPool#add(Poolable, long, TimeUnit, boolean)} for cleanup ownership rules;
     * the ownership check may wait for the pool lock after timeout or interruption.
     *
     * @param element the object to add, must not be {@code null}
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @param autoDestroyOnFailedToAdd if {@code true}, destroys a rejected non-null element unless
     *        that same instance remains pooled at the cleanup check
     * @return {@code true} if successful, {@code false} if the timeout elapsed or add failed
     * @throws IllegalArgumentException if the element or unit is null.
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean add(final E element, final long timeout, final TimeUnit unit, final boolean autoDestroyOnFailedToAdd)
            throws IllegalArgumentException, IllegalStateException, InterruptedException {
        boolean success = false;

        try {
            success = add(element, timeout, unit);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && element != null && !containsSameInstance(element)) {
                element.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    private boolean containsSameInstance(final E element) {
        // A reentrant admission may retain this candidate even though the outer attempt failed.
        // Only sample ownership here; user destruction must run after releasing the lock.
        lock.lock();
        try {
            for (final E retained : pool) {
                if (retained == element) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes an object from the pool.
     * Objects are taken from the head of the deque (LIFO order).
     *
     * <p>The method scans head-to-tail discarding any expired objects (each expired object is
     * destroyed with {@link Caller#EVICT}) until a valid object is found or the pool is
     * exhausted. When a valid object is returned, its activity print's last access time and
     * access count are updated to reflect this access. {@code null} is returned only when no
     * valid object remains in the pool.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>While the pool is non-empty, pops an object from the head (LIFO)</li>
     *   <li>If the popped object has expired, destroys it and continues with the next</li>
     *   <li>Otherwise updates last access time and access count, then returns the object</li>
     *   <li>If the pool becomes empty before a valid object is found, returns {@code null}</li>
     * </ol>
     *
     * <p>Expired objects are detached and accounted while locked. Their destruction callbacks
     * run after the pool lock is released.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E obj = pool.poll();
     * if (obj != null) {
     *     try {
     *         // use the object
     *     } finally {
     *         pool.add(obj);   // adds it back to the pool
     *     }
     * } else {
     *     // pool is empty, create new object if needed
     * }
     * }</pre>
     *
     * @return an object from the pool, or {@code null} if the pool is empty
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    @Override
    public E poll() throws IllegalStateException {
        assertNotClosed();

        E element = null;
        List<E> expiredElements = null;

        lock.lock();

        try {
            // Re-check inside the lock: a concurrent close() could have set isClosed and
            // started removeAll() between the unlocked check above and our lock acquisition.
            // Without this check, poll() would happily pop and return an element that close()
            // will not destroy (since the snapshot was taken before our pop), leaking it and
            // handing a "live" element back from a closed pool.
            assertNotClosed();

            while (pool.size() > 0) {
                element = pool.pop();

                final ActivityPrint activityPrint = element.activityPrint();

                if (activityPrint.isExpired()) {
                    expiredElements = appendPendingDestroy(expiredElements, element, Caller.EVICT);
                    element = null;
                    notFull.signal();
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();

                    if (memoryMeasure != null) {
                        subtractPolledElementMemory(element);
                    }

                    notFull.signal();
                    break;
                }
            }
        } finally {
            lock.unlock();
            invokeDestroyCallbacks(expiredElements, Caller.EVICT);
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
     * Retrieves and removes an object from the pool within the specified timeout period.
     * This method blocks until an object becomes available, the timeout expires, or the thread is interrupted.
     *
     * <p>Expired objects encountered are destroyed (with {@link Caller#EVICT}) and skipped; the
     * method keeps retrying for a valid object until one is found or the timeout expires. When a
     * valid object is returned, its activity print's last access time and access count are updated.
     * Expired objects are detached and accounted while locked; their destruction callbacks run
     * after this invocation releases the pool lock.</p>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @return an object from the pool, or {@code null} if the timeout elapsed before an object was available
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    @MayReturnNull
    @Override
    public E poll(final long timeout, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException, InterruptedException {
        assertNotClosed();

        if (unit == null) {
            throw new IllegalArgumentException("Time unit cannot be null");
        }

        E element = null;
        List<E> expiredElements = null;
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
            takeLoop: while (true) {
                // Re-check on every iteration: a concurrent close()/removeAll() now signals
                // notEmpty.signalAll(), so a waiter parked on awaitNanos wakes up and must
                // notice the closed state instead of looping back to wait again.
                assertNotClosed();

                element = pool.size() > 0 ? pool.pop() : null;

                if (element != null) {
                    final ActivityPrint activityPrint = element.activityPrint();

                    if (activityPrint.isExpired()) {
                        expiredElements = appendPendingDestroy(expiredElements, element, Caller.EVICT);
                        element = null;
                    } else {
                        activityPrint.updateLastAccessTime();
                        activityPrint.updateAccessCount();

                        if (memoryMeasure != null) {
                            subtractPolledElementMemory(element);
                        }
                    }

                    notFull.signal();

                    if (element != null) {
                        break takeLoop;
                    }

                    // Just popped an expired element. Skip awaiting and re-check the
                    // pool — there may still be valid elements ready to poll. Otherwise
                    // we would block on notEmpty even though no producer needs to signal.
                    continue;
                }

                if (nanos <= 0) {
                    break takeLoop;
                }

                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
            invokeDestroyCallbacks(expiredElements, Caller.EVICT);
        }

        // Only account hit/miss on a normal completion. If the body threw (e.g. a
        // concurrent close() made assertNotClosed() raise IllegalStateException, or the
        // waiting thread was interrupted), the call neither hit nor missed the pool and
        // must not skew the statistics.
        if (element != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }

        return element;
    }

    /**
     * Subtracts the just-polled occurrence's admission charge while holding {@link #lock}.
     * Removal never calls the memory measure: the object may have changed since admission.
     *
     * @param element the element that was just popped from the pool
     */
    private void subtractPolledElementMemory(final E element) {
        removeMemoryCharge(element, false);
    }

    // Measure before admission acquires the pool lock: callbacks can reenter or close the pool.
    private long measureMemory(final E element) {
        if (memoryMeasure == null) {
            return 0;
        }
        try {
            return memoryMeasure.sizeOf(element);
        } catch (final Exception e) {
            logger.warn("Error measuring memory size of element", e);
            return -1;
        }
    }

    private void recordMemoryCharge(final E element, final long charge) {
        memoryCharges.computeIfAbsent(element, ignored -> new ArrayDeque<>()).addFirst(charge);
        totalDataSize.addAndGet(charge);
    }

    private void removeMemoryCharge(final E element, final boolean oldest) {
        final Deque<Long> charges = memoryCharges.get(element);
        if (charges != null) {
            totalDataSize.addAndGet(-(oldest ? charges.removeLast() : charges.removeFirst()));
            if (charges.isEmpty()) {
                memoryCharges.remove(element);
            }
        }
    }

    /**
     * Checks if the pool contains the specified object.
     * This method uses the equals method for comparison.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyPoolable obj = new MyPoolable();
     * pool.add(obj);
     *
     * if (pool.contains(obj)) {
     *     System.out.println("Object is in the pool");
     * }
     * }</pre>
     *
     * @param element the object to search for
     * @return {@code true} if the pool contains the object, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean contains(final E element) throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            assertNotClosed();
            return pool.contains(element);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a portion of objects from the pool based on the configured balance factor.
     * Objects are selected for removal according to the eviction policy.
     * The victim count, selection, detachment, accounting, and waiter signaling are atomic with
     * respect to other pool operations. User destruction callbacks run after the lock is released.
     *
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void evict() throws IllegalStateException {
        assertNotClosed();

        final List<E> removingObjects;
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
     * Removes all objects from the pool.
     * All removed objects are destroyed with the {@link Caller#REMOVE_REPLACE_CLEAR} reason.
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
     * Cancels the eviction task if scheduled and destroys all pooled objects.
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
     * Returns the current number of objects in the pool.
     *
     * @return the number of objects currently in the pool
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
     * Returns the hash code value for this pool. The hash code is computed from a snapshot
     * of the pooled elements taken under the pool lock, in their current LIFO order, and uses
     * {@link java.util.List#hashCode()} semantics on that snapshot.
     *
     * @return a hash code value for this pool
     */
    @Override
    public int hashCode() {
        return snapshot().hashCode();
    }

    /**
     * Compares this pool to the specified object for equality.
     * Two pools are equal if they contain the same objects in the same order.
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

        if (!(obj instanceof GenericObjectPool)) {
            return false;
        }

        final List<E> snapshot = snapshot();
        final List<E> otherSnapshot = ((GenericObjectPool<E>) obj).snapshot();

        return snapshot.equals(otherSnapshot);
    }

    /**
     * Returns a string representation of this pool that summarizes its configuration
     * (capacity, eviction delay, eviction policy, auto-balance, balance factor, memory limit,
     * configured memory measure) and current total data size. The actual pooled elements are
     * <em>not</em> included in the output.
     *
     * @return a string representation of this pool's configuration and total data size
     */
    @Override
    public String toString() {
        return "{pool=GenericObjectPool, capacity=" + capacity + ", evictDelayInMillis=" + evictDelayInMillis + ", evictionPolicy=" + evictionPolicy
                + ", autoBalance=" + autoBalance + ", balanceFactor=" + balanceFactor + ", maxMemorySize=" + maxMemorySize + ", memoryMeasure=" + memoryMeasure
                + ", totalDataSize=" + totalDataSize.get() + "}";
    }

    /**
     * Removes (vacates) the specified number of objects from the pool based on the eviction policy.
     * This is the sized counterpart to the public no-arg {@link #evict()} (which removes a
     * balance-factor fraction); it removes <em>exactly</em> {@code numberToEvict} objects, choosing
     * victims via the configured {@link EvictionPolicy}. Destroyed objects use {@link Caller#VACATE}.
     * Victims are detached atomically under the pool lock, but user destruction callbacks run
     * after the lock is released. This sized operation is available to subclasses; public
     * {@link #evict()} performs its balance-factor count and detachment in one critical section.
     *
     * @param numberToEvict the number of objects to remove
     */
    protected void vacate(final int numberToEvict) {
        List<E> removingObjects;

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
    private List<E> prepareVacateUnderLock(final int numberToEvict) {
        final List<E> removingObjects = detachForVacateUnderLock(numberToEvict);

        if (N.notEmpty(removingObjects)) {
            accountDestroyAll(removingObjects, Caller.VACATE);
            notFull.signalAll();
        }

        return removingObjects;
    }

    /** Selects and detaches victims while {@link #lock} is held. No user destruction callback is invoked. */
    private List<E> detachForVacateUnderLock(final int numberToEvict) {
        final int size = pool.size();

        if (numberToEvict >= size) {
            final List<E> removingObjects = new ArrayList<>(pool);
            pool.clear();
            return removingObjects;
        }

        if (numberToEvict <= 0) {
            return null;
        }

        if (evictionPolicy == EvictionPolicy.FIFO) {
            final List<E> removingObjects = new ArrayList<>(numberToEvict);
            final Iterator<E> it = pool.descendingIterator();

            while (it.hasNext() && removingObjects.size() < numberToEvict) {
                removingObjects.add(it.next());
                it.remove();
            }

            return removingObjects;
        }

        final Comparator<E> reversedCmp = cmp.reversed();
        final Queue<E> heap = new PriorityQueue<>(numberToEvict, reversedCmp);

        for (final E element : pool) {
            if (heap.size() < numberToEvict) {
                heap.offer(element);
            } else if (cmp.compare(element, heap.peek()) < 0) {
                heap.poll();
                heap.offer(element);
            }
        }

        // Identity-based removal: pool.remove(Object) uses equals(), which can drop a different
        // equals-equal element. Walk the deque and remove the selected instances themselves.
        removeByIdentity(heap);
        return new ArrayList<>(heap);
    }

    private void removeByIdentity(final Collection<E> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        // Remove ONE pool entry per target occurrence, by identity. If the same instance happens to
        // be in the pool more than once, only the head-most occurrences are removed (one per time
        // it appears in targets) - matching the ArrayDeque.remove(Object) cardinality but using
        // identity instead of equals so a content-equal-but-distinct entry isn't accidentally evicted.
        //
        // Single bulk pass on purpose: this runs under the pool lock, and the victims of every
        // non-FIFO policy (and of the expiry sweep on a LIFO pool) usually sit at the TAIL. Restarting
        // pool.iterator() per target and calling Iterator.remove() (which arraycopies per call in
        // ArrayDeque) was O(n * k) - seconds of lock hold time for a 100k-element pool. ArrayDeque's
        // removeIf override compacts once, so the whole removal is O(n) regardless of k.
        final IdentityHashMap<E, Integer> pending = new IdentityHashMap<>(targets.size());

        for (final E target : targets) {
            pending.merge(target, 1, Integer::sum);
        }

        pool.removeIf(e -> {
            final Integer count = pending.get(e);

            if (count == null) {
                return false;
            }

            if (count == 1) {
                pending.remove(e);
            } else {
                pending.put(e, count - 1);
            }

            return true;
        });
    }

    private int numberToAutoBalance() {
        if (pool.isEmpty()) {
            return 0;
        }

        return Math.max(1, (int) (pool.size() * balanceFactor));
    }

    private List<E> snapshot() {
        lock.lock();

        try {
            return new ArrayList<>(pool);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Scans the pool for expired objects and removes them.
     * This method is called periodically by the scheduled eviction task. Detachment and pool
     * accounting are completed under lock; user destruction callbacks run after unlock.
     */
    @SuppressWarnings("deprecation")
    protected void removeExpired() {
        // Phase 1: under the lock, identify+remove expired elements from the pool.
        List<E> removingObjects = null;
        lock.lock();
        try {
            for (final E element : pool) {
                if (element.activityPrint().isExpired()) {
                    if (removingObjects == null) {
                        removingObjects = Objectory.createList();
                    }
                    removingObjects.add(element);
                }
            }

            if (N.notEmpty(removingObjects)) {
                // Identity-based removal: see vacate(int). pool.removeAll uses equals which can
                // drop multiple distinct entries that happen to be equals-equal to one expired
                // element, leaking still-valid objects with no destroy callback.
                removeByIdentity(removingObjects);
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
     * Destroys a single pooled object and updates statistics.
     * Updates memory tracking and eviction counts as appropriate, and handles exceptions gracefully.
     *
     * <p>This hook is <em>not</em> invoked by the pool's own lifecycle paths ({@code poll},
     * {@code removeExpired}, {@code evict}/{@code vacate}, auto-balancing, {@code clear} and
     * {@code close}): those account for detached elements under the pool lock and invoke the
     * {@link Poolable#destroy(Caller)} callbacks after the lock is released. It is intended for
     * subclass-initiated destruction of elements that are already detached from the pool. Because it
     * updates the memory accounting (a non-thread-safe charge map and the total), it must be called
     * either while holding the pool lock or for elements that are no longer pooled; calling it for an
     * element still in the pool strips that element's admission charge.</p>
     *
     * @param element the object to destroy
     * @param caller the reason for destruction (determines whether eviction count is incremented)
     */
    protected void destroy(final E element, final Caller caller) {
        accountDestroy(element, caller);
        invokeDestroyCallback(element, caller);
    }

    /** Updates pool accounting for a detached element without invoking user code. */
    private void accountDestroy(final E element, final Caller caller) {
        if (caller == Caller.EVICT || caller == Caller.VACATE) {
            evictionCount.incrementAndGet();
        }

        if (element != null && memoryMeasure != null) {
            removeMemoryCharge(element, caller == Caller.VACATE && evictionPolicy == EvictionPolicy.FIFO);
        }
    }

    /** Invokes only the user-supplied destruction callback; accounting must already be complete. */
    private void invokeDestroyCallback(final E element, final Caller caller) {
        if (element != null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Destroying cached object " + ClassUtil.getSimpleClassName(element.getClass()) + " with activity print: " + element.activityPrint());
            }

            try {
                element.destroy(caller);
            } catch (final Throwable e) { // NOSONAR - isolate a broken user callback so remaining resources are still released
                rethrowIfFatal(e);

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }
        }
    }

    private void accountDestroyAll(final Collection<E> collection, final Caller caller) {
        if (N.notEmpty(collection)) {
            for (final E element : collection) {
                accountDestroy(element, caller);
            }
        }
    }

    private void invokeDestroyCallbacks(final Collection<E> collection, final Caller caller) {
        if (N.notEmpty(collection)) {
            for (final E element : collection) {
                invokeDestroyCallback(element, caller);
            }
        }
    }

    /** Adds detached elements to a pending callback list and performs their accounting under lock. */
    private List<E> appendPendingDestroy(List<E> pending, final Collection<E> detached, final Caller caller) {
        if (N.isEmpty(detached)) {
            return pending;
        }

        accountDestroyAll(detached, caller);

        if (pending == null) {
            pending = new ArrayList<>(detached.size());
        }

        pending.addAll(detached);
        notFull.signalAll();
        return pending;
    }

    private List<E> appendPendingDestroy(List<E> pending, final E detached, final Caller caller) {
        accountDestroy(detached, caller);

        if (pending == null) {
            pending = new ArrayList<>();
        }

        pending.add(detached);
        return pending;
    }

    /**
     * Destroys all objects in the provided collection by calling
     * {@link #destroy(Poolable, Caller)} on each of them.
     *
     * <p>Like {@code destroy}, this hook is not invoked by the pool's own eviction, clear or close
     * paths; it is intended for subclass-initiated destruction of elements already detached from the
     * pool, and must be called while holding the pool lock or for elements no longer pooled because
     * it updates the memory accounting.</p>
     *
     * @param collection the collection of objects to destroy
     * @param caller the reason for destruction
     */
    protected void destroyAll(final Collection<E> collection, final Caller caller) {
        if (N.notEmpty(collection)) {
            for (final E element : collection) {
                destroy(element, caller);
            }
        }
    }

    private void removeAll(final Caller caller) {
        removeAll(caller, false);
    }

    private void removeAll(final Caller caller, final boolean requireOpen) {
        // Snapshot, clear, and account under the lock, then release the lock BEFORE invoking user
        // destroy() callbacks. The pre-fix behavior held the pool lock across N user
        // destroy() calls (which may close DB/TCP connections and block) — for a large pool that
        // turned close()/clear() into a global stall against every other API call.
        final List<E> doomed;
        lock.lock();
        try {
            if (requireOpen) {
                assertNotClosed();
            }

            doomed = new ArrayList<>(pool);
            pool.clear();
            // Complete accounting before exposing the newly-empty pool to another add(). If this
            // were deferred with the callbacks, maxMemorySize checks could observe stale usage.
            accountDestroyAll(doomed, caller);
            // Wake every waiter on either condition. notFull alone covered add(timeout) waiters,
            // but poll(timeout) waiters on notEmpty would otherwise hang until their own timeout
            // expired even though the pool is being torn down.
            notFull.signalAll();
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
        invokeDestroyCallbacks(doomed, caller);
    }

    /**
     * Serializes this pool to an ObjectOutputStream.
     * The pool is locked during serialization to ensure consistency. Every pooled element and the
     * configured memory measure are written with the pool, so each must be {@code Serializable};
     * otherwise a {@link java.io.NotSerializableException} is thrown.
     *
     * @param os the output stream
     * @throws IOException if {@code os.defaultWriteObject()} cannot write the pool state, including a nonserializable element or memory
     *         measure
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
        // Do not publish this object to the eviction executor or JVM shutdown hooks from a
        // superclass readObject(). A serializable subclass has not restored its own fields yet,
        // and the scheduled task dispatches to the overridable removeExpired() method. Stream
        // validation runs only after the complete object graph has been deserialized.
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
     *         a charge is negative or the total overflows a long, or a measured pool has a different charge count than element count
     */
    private void restoreMemoryAccounting() throws InvalidObjectException {
        if (memoryCharges == null) {
            if (memoryMeasure != null && !pool.isEmpty()) {
                throw new InvalidObjectException("Serialized measured pool has no admission charges; recreate the pool");
            }
            memoryCharges = new IdentityHashMap<>();
        }

        long total = 0;
        int occurrences = 0;
        for (final Deque<Long> charges : memoryCharges.values()) {
            for (final long charge : charges) {
                if (charge < 0 || charge > Long.MAX_VALUE - total) {
                    throw new InvalidObjectException("Invalid serialized admission charge: " + charge);
                }
                total += charge;
                occurrences++;
            }
        }
        if (memoryMeasure != null && occurrences != pool.size()) {
            throw new InvalidObjectException("Serialized admission charges do not match pool size");
        }
        // The superclass counter is serialized before the subclass acquires its snapshot lock.
        // Derive usage from the charges serialized with the elements, not that earlier counter.
        totalDataSize.set(total);
    }
}
