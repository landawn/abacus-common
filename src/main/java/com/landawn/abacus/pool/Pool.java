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

import java.io.Serializable;

/**
 * Base interface for all pool implementations, providing fundamental pool operations.
 * A pool is a cache of pre-initialized objects that can be reused to improve performance
 * by avoiding the overhead of creating new objects.
 * 
 * <p>All pool implementations must be thread-safe and support concurrent access.
 * The pool lifecycle includes creation, usage, and eventual closure. Once closed,
 * a pool cannot be reopened.
 * 
 * <p>Common operations include:
 * <ul>
 *   <li>Checking pool capacity and current size</li>
 *   <li>Clearing or vacating the pool</li>
 *   <li>Retrieving pool statistics</li>
 *   <li>Proper cleanup via close()</li>
 * </ul>
 * 
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public interface Pool extends Serializable {

    /**
     * Acquires an exclusive lock on this pool.
     * This method blocks until the lock is available.
     * 
     * <p>The lock must be released by calling {@link #unlock()} in a finally block
     * to ensure proper cleanup even if an exception occurs.
     * 
     * <p>Usage example:
     * <pre>{@code
     * pool.lock();
     * try {
     *     // perform operations requiring exclusive access
     * } finally {
     *     pool.unlock();
     * }
     * }</pre>
     */
    void lock();

    /**
     * Releases the exclusive lock on this pool.
     * 
     * <p>This method must be called by the same thread that acquired the lock
     * via {@link #lock()}, typically in a finally block.
     * 
     * @throws IllegalMonitorStateException if the current thread does not hold the lock
     */
    void unlock();

    /**
     * Returns the maximum capacity of this pool.
     * 
     * <p>The capacity represents the maximum number of objects that can be
     * stored in the pool at any given time. Attempting to add objects beyond
     * this capacity may fail or trigger balancing operations depending on
     * the pool configuration.
     * 
     * @return the maximum number of objects this pool can hold
     */
    int capacity();

    /**
     * Returns the current number of objects in this pool.
     * 
     * <p>The size may change between the time this method returns and when
     * the returned value is used, as other threads may be concurrently
     * adding or removing objects.
     * 
     * @return the current number of objects in the pool
     */
    int size();

    /**
     * Checks whether this pool is empty.
     * 
     * <p>This is a convenience method equivalent to {@code size() == 0}.
     * 
     * @return {@code true} if the pool contains no objects, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Removes a portion of objects from the pool to free up space.
     * 
     * <p>This method is typically called when the pool is full and auto-balancing
     * is enabled. The number of objects removed is determined by the pool's
     * balance factor configuration. Objects are selected for removal based on
     * the pool's eviction policy.
     * 
     * <p>Usage example:
     * <pre>{@code
     * if (pool.size() >= pool.capacity() * 0.9) {
     *     pool.vacate(); // free up some space
     * }
     * }</pre>
     * 
     * @throws IllegalStateException if the pool has been closed
     */
    void vacate();

    /**
     * Removes all objects from this pool.
     * 
     * <p>All pooled objects will be destroyed according to their cleanup logic.
     * After this method returns, the pool will be empty but still usable for
     * storing new objects.
     * 
     * <p>This method differs from {@link #close()} in that the pool remains
     * open and operational after clearing.
     * 
     * @throws IllegalStateException if the pool has been closed
     */
    void clear();

    /**
     * Returns a snapshot of statistics for this pool.
     * 
     * <p>The statistics include information such as:
     * <ul>
     *   <li>Pool capacity and current size</li>
     *   <li>Number of put and get operations</li>
     *   <li>Hit and miss counts</li>
     *   <li>Number of evictions</li>
     *   <li>Memory usage (if applicable)</li>
     * </ul>
     * 
     * <p>Usage example:
     * <pre>{@code
     * PoolStats stats = pool.stats();
     * double hitRate = stats.getCount() > 0 ? 
     *     (double) stats.hitCount() / stats.getCount() : 0;
     * System.out.println("Cache hit rate: " + hitRate);
     * }</pre>
     * 
     * @return a PoolStats object containing current pool statistics
     */
    PoolStats stats();

    /**
     * Closes this pool and releases all resources.
     * 
     * <p>This method performs the following actions:
     * <ul>
     *   <li>Cancels any scheduled eviction tasks</li>
     *   <li>Destroys all objects currently in the pool</li>
     *   <li>Marks the pool as closed</li>
     *   <li>Releases any other resources held by the pool</li>
     * </ul>
     * 
     * <p>After calling this method, any attempt to use the pool will result in
     * an {@link IllegalStateException}. A closed pool cannot be reopened.
     * 
     * <p>This method is idempotent - calling it multiple times has no additional effect.
     * 
     * <p>Usage example:
     * <pre>{@code
     * try {
     *     // use the pool
     * } finally {
     *     pool.close(); // ensure cleanup
     * }
     * }</pre>
     */
    void close();

    /**
     * Checks whether this pool has been closed.
     * 
     * <p>Once a pool is closed via {@link #close()}, it cannot be reopened
     * and this method will always return {@code true}.
     * 
     * @return {@code true} if the pool has been closed, {@code false} otherwise
     */
    boolean isClosed();
}