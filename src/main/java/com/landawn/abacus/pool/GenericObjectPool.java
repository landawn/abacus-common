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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * A generic implementation of ObjectPool that stores poolable objects in a LIFO (Last-In-First-Out) structure.
 * This implementation uses an ArrayDeque internally for efficient add/remove operations at the head.
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
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a pool with capacity 50, 5-minute eviction delay
 * GenericObjectPool<MyResource> pool = new GenericObjectPool<>(
 *     50, 300000, EvictionPolicy.LAST_ACCESS_TIME
 * );
 *
 * // Add resources
 * pool.add(new MyResource());
 *
 * // Take and use resources
 * MyResource resource = pool.take();
 * try {
 *     // use resource
 * } finally {
 *     pool.add(resource);   // return to pool
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
     * @param memoryMeasure the function to calculate object memory size, or {@code null} if not using memory limits
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final ObjectPool.MemoryMeasure<E> memoryMeasure) {
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
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
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
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate object memory size, or {@code null} if not using memory limits
     */
    protected GenericObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        super(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);

        this.memoryMeasure = memoryMeasure;
        pool = new ArrayDeque<>(Math.min(capacity, 1000));

        cmp = createComparator();
        scheduleEvictionTask();

        // Register shutdown hook AFTER all subclass state is fully initialized so a JVM shutdown
        // racing the constructor cannot invoke close() against a null pool/cmp.
        registerShutdownHook();
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

            default:
                throw new RuntimeException("Unsupported eviction policy: " + evictionPolicy.name());
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
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }
        };

        scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelayInMillis, evictDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds an object to the pool.
     * The object is added to the head of the internal deque for LIFO ordering.
     *
     * <p>The add operation will fail if:</p>
     * <ul>
     *   <li>The object is null</li>
     *   <li>The object has already expired</li>
     *   <li>The pool is at capacity and auto-balancing is disabled</li>
     *   <li>The object would exceed memory constraints (when memory measure is configured)</li>
     * </ul>
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
     * @throws IllegalArgumentException if the element is null
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean add(final E element) throws IllegalStateException {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        if (element.activityPrint().isExpired()) {
            return false;
        }

        lock.lock();

        try {
            // Re-check inside the lock: a concurrent close() could have set isClosed between
            // the prior unlocked check and our lock acquisition, leaking this element.
            assertNotClosed();

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
                try {
                    final long elementMemorySize = memoryMeasure.sizeOf(element);

                    if (elementMemorySize < 0) {
                        logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                        return false;
                    }

                    if (maxMemorySize > 0 && elementMemorySize > maxMemorySize - totalDataSize.get()) {
                        if (autoBalance) {
                            evict();

                            if (maxMemorySize > 0 && elementMemorySize > maxMemorySize - totalDataSize.get()) {
                                // ignore.
                                return false;
                            }
                        } else {
                            // ignore.
                            return false;
                        }
                    }

                    pool.push(element);

                    totalDataSize.addAndGet(elementMemorySize); //NOSONAR
                } catch (final Exception ex) {
                    logger.warn("Error measuring memory size of element", ex);
                    return false;
                }
            } else {
                pool.push(element);
            }

            putCount.incrementAndGet();

            notEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds an object to the pool with optional automatic destruction on failure.
     * This method ensures proper cleanup of resources if the object cannot be added.
     *
     * @param element the object to add, must not be {@code null}
     * @param autoDestroyOnFailedToAdd if {@code true}, calls element.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the element is null
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public boolean add(final E element, final boolean autoDestroyOnFailedToAdd) {
        boolean success = false;

        try {
            success = add(element);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && element != null) {
                element.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
    }

    /**
     * Attempts to add an object to the pool within the specified timeout period.
     * This method blocks until space becomes available, the timeout expires, or the thread is interrupted.
     *
     * <p><b>Safety Mechanism:</b> This method implements a maxSpins (10,000) safety limit to prevent
     * potential infinite loops in edge cases. After 10,000 iterations of checking for available space,
     * the method will return {@code false} even if the timeout has not expired. This is an additional
     * safeguard against extremely high contention scenarios or potential implementation issues. Under
     * normal operation, the timeout mechanism will trigger long before reaching this limit.</p>
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
     * @param unit the time unit of the timeout argument
     * @return {@code true} if successful, {@code false} if the timeout elapsed before space was available
     *         or if the maxSpins safety limit (10,000 iterations) is reached
     * @throws IllegalArgumentException if the element is null
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean add(final E element, final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        if (element.activityPrint().isExpired()) {
            return false;
        }

        long nanos = unit.toNanos(timeout);

        lock.lock();

        try {
            // Re-check closed-state inside the lock; a concurrent close() between an unlocked
            // check and lock acquisition would otherwise leak this element.
            assertNotClosed();

            if ((pool.size() >= capacity) && autoBalance) {
                evict();
            }

            int maxSpins = 10000;

            while (maxSpins-- > 0) {
                // Re-check inside the loop: a concurrent close() that ran while this thread was
                // parked on notFull.awaitNanos() (now signaled by removeAll()) emptied the pool;
                // without this check the awakened thread would happily push the element into the
                // newly-closed pool, leaking it (its destroy() never fires on close()).
                assertNotClosed();

                if (pool.size() < capacity) {
                    // Re-check expiry: the element may have expired during the awaitNanos wait
                    // below. Pushing an expired element corrupts hit/miss accounting and exposes
                    // a doomed object to the next take()er.
                    if (element.activityPrint().isExpired()) {
                        return false;
                    }

                    if (memoryMeasure != null) {
                        final long elementMemorySize = memoryMeasure.sizeOf(element);

                        if (elementMemorySize < 0) {
                            logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                            return false;
                        }

                        if (maxMemorySize > 0 && elementMemorySize > maxMemorySize - totalDataSize.get()) {
                            if (autoBalance) {
                                evict();

                                if (maxMemorySize > 0 && elementMemorySize > maxMemorySize - totalDataSize.get()) {
                                    // ignore.
                                    return false;
                                }
                            } else {
                                // ignore.
                                return false;
                            }
                        }

                        pool.push(element);

                        totalDataSize.addAndGet(elementMemorySize); //NOSONAR
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

            return false; // Safety timeout after max spins
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attempts to add an object to the pool with timeout and automatic destruction on failure.
     * Combines timeout waiting with automatic resource cleanup.
     *
     * @param element the object to add, must not be {@code null}
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param autoDestroyOnFailedToAdd if {@code true}, calls element.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if successful, {@code false} if the timeout elapsed or add failed
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean add(final E element, final long timeout, final TimeUnit unit, final boolean autoDestroyOnFailedToAdd) throws InterruptedException {
        boolean success = false;

        try {
            success = add(element, timeout, unit);
        } finally {
            if (autoDestroyOnFailedToAdd && !success && element != null) {
                element.destroy(Caller.PUT_ADD_FAILURE);
            }
        }

        return success;
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E obj = pool.take();
     * if (obj != null) {
     *     try {
     *         // use the object
     *     } finally {
     *         pool.add(obj);   // return to pool
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
    public E take() throws IllegalStateException {
        assertNotClosed();

        E element = null;

        lock.lock();

        try {
            // Re-check inside the lock: a concurrent close() could have set isClosed and
            // started removeAll() between the unlocked check above and our lock acquisition.
            // Without this check, take() would happily pop and return an element that close()
            // will not destroy (since the snapshot was taken before our pop), leaking it and
            // handing a "live" element back from a closed pool.
            assertNotClosed();

            while (pool.size() > 0) {
                element = pool.pop();

                final ActivityPrint activityPrint = element.activityPrint();

                if (activityPrint.isExpired()) {
                    destroy(element, Caller.EVICT);
                    element = null;
                    notFull.signal();
                } else {
                    activityPrint.updateLastAccessTime();
                    activityPrint.updateAccessCount();

                    if (memoryMeasure != null) {
                        // Wrap memoryMeasure call: a user-supplied sizeOf that throws after we
                        // popped the element would otherwise propagate the exception while leaving
                        // the popped element neither in the pool nor returned to the caller — a
                        // pure leak (its destroy() never fires). Log and continue with totalDataSize
                        // unchanged; better to drift one accounting unit than leak a live resource.
                        try {
                            final long elementMemorySize = memoryMeasure.sizeOf(element);

                            if (elementMemorySize < 0) {
                                logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                            } else {
                                totalDataSize.addAndGet(-elementMemorySize); //NOSONAR
                            }
                        } catch (final Exception ex) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Error measuring memory size during take: " + ExceptionUtil.getErrorMessage(ex, true));
                            }
                        }
                    }

                    notFull.signal();
                    break;
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
     * Retrieves and removes an object from the pool within the specified timeout period.
     * This method blocks until an object becomes available, the timeout expires, or the thread is interrupted.
     *
     * <p>Expired objects are automatically destroyed and the method continues waiting
     * for a valid object until the timeout expires. The object's activity print is updated to reflect this access.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return an object from the pool, or {@code null} if the timeout elapsed before an object was available
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
     */
    @MayReturnNull
    @Override
    public E take(final long timeout, final TimeUnit unit) throws IllegalStateException, InterruptedException {
        assertNotClosed();

        E element = null;
        long nanos = unit.toNanos(timeout);

        lock.lock();

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
                        destroy(element, Caller.EVICT);
                        element = null;
                    } else {
                        activityPrint.updateLastAccessTime();
                        activityPrint.updateAccessCount();

                        if (memoryMeasure != null) {
                            // See take(): never let a user sizeOf() throw after we've popped — that
                            // would leak the live element. Log and continue.
                            try {
                                final long elementMemorySize = memoryMeasure.sizeOf(element);

                                if (elementMemorySize < 0) {
                                    logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                                } else {
                                    totalDataSize.addAndGet(-elementMemorySize); //NOSONAR
                                }
                            } catch (final Exception ex) {
                                if (logger.isWarnEnabled()) {
                                    logger.warn("Error measuring memory size during take: " + ExceptionUtil.getErrorMessage(ex, true));
                                }
                            }
                        }
                    }

                    notFull.signal();

                    if (element != null) {
                        break takeLoop;
                    }

                    // Just popped an expired element. Skip awaiting and re-check the
                    // pool — there may still be valid elements ready to take. Otherwise
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
            return pool.contains(element);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a portion of objects from the pool based on the configured balance factor.
     * Objects are selected for removal according to the eviction policy.
     * After evicting, signals waiting threads that space is now available in the pool.
     *
     * @throws IllegalStateException if the pool has been closed
     */
    @Override
    public void evict() throws IllegalStateException {
        assertNotClosed();

        lock.lock();

        try {
            evict(numberToAutoBalance()); // NOSONAR

            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all objects from the pool.
     * All removed objects are destroyed with the REMOVE_REPLACE_CLEAR reason.
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
     * Cancels the eviction task if scheduled and destroys all pooled objects.
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
            return pool.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the hash code value for this pool.
     * The hash code is based on the internal pool structure.
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
     * Returns a string representation of this pool.
     * The string representation consists of the string representation of the internal pool.
     *
     * @return a string representation of this pool
     */
    @Override
    public String toString() {
        return "{pool=GenericObjectPool, capacity=" + capacity + ", evictDelayInMillis=" + evictDelayInMillis + ", evictionPolicy=" + evictionPolicy
                + ", autoBalance=" + autoBalance + ", balanceFactor=" + balanceFactor + ", maxMemorySize=" + maxMemorySize + ", memoryMeasure=" + memoryMeasure
                + ", totalDataSize=" + totalDataSize.get() + "}";
    }

    /**
     * Removes the specified number of objects from the pool based on the eviction policy.
     * This method is called internally during eviction operations.
     *
     * @param numberToEvict the number of objects to remove
     */
    protected void evict(final int numberToEvict) {
        final int size = pool.size();

        if (numberToEvict >= size) {
            destroyAll(new ArrayList<>(pool), Caller.VACATE);
            pool.clear();
        } else if (numberToEvict > 0) {
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

            // Identity-based removal: pool.remove(Object) uses equals(), which can drop a
            // different element that happens to be equals-equal (e.g. PoolableAdapter with
            // content-based equals wrapping the same value). Walk the deque once and remove
            // the heap-selected items by identity.
            removeByIdentity(heap);

            destroyAll(heap, Caller.VACATE);
        }
    }

    private void removeByIdentity(final java.util.Collection<E> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        // Remove ONE pool entry per target, by identity. If the same instance happens to be in
        // the pool more than once, only the first occurrence is removed - matching the
        // ArrayDeque.remove(Object) cardinality but using identity instead of equals so a
        // content-equal-but-distinct entry isn't accidentally evicted.
        for (final E target : targets) {
            final java.util.Iterator<E> it = pool.iterator();
            while (it.hasNext()) {
                if (it.next() == target) {
                    it.remove();
                    break;
                }
            }
        }
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
     * This method is called periodically by the scheduled eviction task.
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
                // Identity-based removal: see evict(int). pool.removeAll uses equals which can
                // drop multiple distinct entries that happen to be equals-equal to one expired
                // element, leaking still-valid objects with no destroy callback.
                removeByIdentity(removingObjects);
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }

        // Phase 2: destroy outside the lock so user destroy() callbacks (which may close DB/TCP
        // connections) can't stall the pool against unrelated take/add/etc.
        try {
            if (N.notEmpty(removingObjects)) {
                destroyAll(removingObjects, Caller.EVICT);
            }
        } finally {
            Objectory.recycle(removingObjects);
        }
    }

    /**
     * Destroys a single pooled object and updates statistics.
     * Updates memory tracking and eviction counts as appropriate, and handles exceptions gracefully.
     *
     * @param element the object to destroy
     * @param caller the reason for destruction (determines whether eviction count is incremented)
     */
    protected void destroy(final E element, final Caller caller) {
        if (caller == Caller.EVICT || caller == Caller.VACATE) {
            evictionCount.incrementAndGet();
        }

        if (element != null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Destroying cached object " + ClassUtil.getSimpleClassName(element.getClass()) + " with activity print: " + element.activityPrint());
            }

            if (memoryMeasure != null) {
                try {
                    final long elementMemorySize = memoryMeasure.sizeOf(element);

                    if (elementMemorySize < 0) {
                        logger.warn("Memory measure returned negative size for element: " + elementMemorySize);
                    } else {
                        totalDataSize.addAndGet(-elementMemorySize); //NOSONAR
                    }
                } catch (final Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error measuring memory size during destroy: " + ExceptionUtil.getErrorMessage(e, true));
                    }
                }
            }

            try {
                element.destroy(caller);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }
        }
    }

    /**
     * Destroys all objects in the provided collection.
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
        // Snapshot under the lock and clear pool state, then release the lock BEFORE invoking
        // user destroy() callbacks. The pre-fix behavior held the pool lock across N user
        // destroy() calls (which may close DB/TCP connections and block) — for a large pool that
        // turned close()/clear() into a global stall against every other API call.
        final List<E> doomed;
        lock.lock();
        try {
            doomed = new ArrayList<>(pool);
            pool.clear();
            // Wake every waiter on either condition. notFull alone covered add(timeout) waiters,
            // but take(timeout) waiters on notEmpty would otherwise hang until their own timeout
            // expired even though the pool is being torn down.
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
    }
}
