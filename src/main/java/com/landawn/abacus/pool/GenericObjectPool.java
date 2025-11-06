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
 *     pool.add(resource); // return to pool
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
    final Comparator<E> cmp;

    /**
     * Future representing the scheduled eviction task, {@code null} if eviction is disabled.
     */
    ScheduledFuture<?> scheduleFuture;

    /**
     * Constructs a new GenericObjectPool with basic configuration.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     */
    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        this(capacity, evictDelay, evictionPolicy, 0, null);
    }

    /**
     * Constructs a new GenericObjectPool with memory-based constraints.
     * Uses default auto-balancing and balance factor settings.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate object memory size, or {@code null} if not using memory limits
     */
    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final long maxMemorySize,
            final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        this(capacity, evictDelay, evictionPolicy, true, DEFAULT_BALANCE_FACTOR, maxMemorySize, memoryMeasure);
    }

    /**
     * Constructs a new GenericObjectPool with auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     */
    protected GenericObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy, final boolean autoBalance,
            final float balanceFactor) {
        this(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, 0, null);
    }

    /**
     * Constructs a new GenericObjectPool with full configuration options.
     *
     * @param capacity the maximum number of objects the pool can hold (must be non-negative)
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction (must be non-negative)
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (must be non-negative)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no limit (must be non-negative)
     * @param memoryMeasure the function to calculate object memory size, or {@code null} if not using memory limits
     */
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
     * @param e the object to add, must not be null
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
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

            if (memoryMeasure != null) {
                try {
                    final long elementSize = memoryMeasure.sizeOf(e);

                    if (elementSize < 0) {
                        logger.warn("Memory measure returned negative size for element: " + elementSize);
                        return false;
                    }

                    if (elementSize > maxMemorySize - totalDataSize.get()) {
                        // ignore.
                        return false;
                    }

                    pool.push(e);
                    totalDataSize.addAndGet(elementSize); //NOSONAR
                } catch (final Exception ex) {
                    logger.warn("Error measuring memory size of element", ex);
                    return false;
                }
            } else {
                pool.push(e);
            }

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
     * @param e the object to add, must not be null
     * @param autoDestroyOnFailedToAdd if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if the object was successfully added, {@code false} otherwise
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
     * @param e the object to add, must not be null
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if successful, {@code false} if the timeout elapsed before space was available
     *         or if the maxSpins safety limit (10,000 iterations) is reached
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
     * @throws InterruptedException if interrupted while waiting
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

            int maxSpins = 10000;
            while (maxSpins-- > 0) {
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

            return false; // Safety timeout after max spins
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attempts to add an object to the pool with timeout and automatic destruction on failure.
     * Combines timeout waiting with automatic resource cleanup.
     *
     * @param e the object to add, must not be null
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param autoDestroyOnFailedToAdd if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if successful, {@code false} if the timeout elapsed or add failed
     * @throws InterruptedException if interrupted while waiting
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
     * Retrieves and removes an object from the pool.
     * Objects are taken from the head of the deque (LIFO order).
     *
     * <p>If the retrieved object has expired, it will be destroyed and the method
     * will return {@code null}. The object's activity print is updated to reflect this access.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Removes an object from the head of the pool (LIFO)</li>
     *   <li>Checks if the object has expired</li>
     *   <li>If expired: destroys the object, returns {@code null}</li>
     *   <li>If valid: updates last access time and access count, returns the object</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E obj = pool.take();
     * if (obj != null) {
     *     try {
     *         // use the object
     *     } finally {
     *         pool.add(obj); // return to pool
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
            lock.unlock();

            if (e != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
        }

        return e;
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
            lock.unlock();

            if (e != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
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
     * @param valueToFind the object to search for
     * @return {@code true} if the pool contains the object, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
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
     * Removes a portion of objects from the pool based on the configured balance factor.
     * Objects are selected for removal according to the eviction policy.
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
     * Returns the current number of objects in the pool.
     *
     * @return the number of objects currently in the pool
     */
    @Override
    public int size() throws IllegalStateException {
        // assertNotClosed();

        return pool.size();
    }

    /**
     * Returns the hash code value for this pool.
     * The hash code is based on the internal pool structure.
     * 
     * @return a hash code value for this pool
     */
    @Override
    public int hashCode() {
        return pool.hashCode();
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
        return this == obj || (obj instanceof GenericObjectPool && N.equals(((GenericObjectPool<E>) obj).pool, pool));
    }

    /**
     * Returns a string representation of this pool.
     * The string representation consists of the string representation of the internal pool.
     * 
     * @return a string representation of this pool
     */
    @Override
    public String toString() {
        return "{pool=GenericObjectPool, capacity=" + capacity + ", evictDelay=" + evictDelay + ", evictionPolicy=" + evictionPolicy + ", autoBalance="
                + autoBalance + ", balanceFactor=" + balanceFactor + ", maxMemorySize=" + maxMemorySize + ", memoryMeasure=" + memoryMeasure
                + ", totalDataSize=" + totalDataSize.get() + "}";
    }

    /**
     * Removes the specified number of objects from the pool based on the eviction policy.
     * This method is called internally during vacate operations.
     *
     * @param vacationNumber the number of objects to remove
     */
    protected void vacate(final int vacationNumber) {
        final int size = pool.size();

        if (vacationNumber >= size) {
            destroyAll(new ArrayList<>(pool), Caller.VACATE);
            pool.clear();
        } else if (vacationNumber > 0) {
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
     * Scans the pool for expired objects and removes them.
     * This method is called periodically by the scheduled eviction task.
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
     * Destroys a single pooled object and updates statistics.
     * Updates memory tracking and eviction counts as appropriate, and handles exceptions gracefully.
     *
     * @param value the object to destroy
     * @param caller the reason for destruction (determines whether eviction count is incremented)
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
     * Destroys all objects in the provided collection.
     * This is a convenience method that calls {@link #destroy(Poolable, Caller)} for each object.
     *
     * @param c the collection of objects to destroy
     * @param caller the reason for destruction
     */
    protected void destroyAll(final Collection<E> c, final Caller caller) {
        if (N.notEmpty(c)) {
            for (final E e : c) {
                destroy(e, caller);
            }
        }
    }

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
     * Deserializes this pool from an ObjectInputStream.
     * The pool is locked during deserialization to ensure consistency.
     * 
     * @param is the input stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
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
