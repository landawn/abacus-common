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

import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A pool of reusable objects that extends the base Pool interface with object-specific operations.
 * This interface provides methods for adding and retrieving objects from the pool with various
 * timeout and cleanup options.
 *
 * <p>ObjectPool is designed for scenarios where you need to reuse expensive objects like:
 * <ul>
 *   <li>Database connections</li>
 *   <li>Thread instances</li>
 *   <li>Large buffers or arrays</li>
 *   <li>Complex initialized objects</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe add and take operations</li>
 *   <li>Optional timeout support for blocking operations</li>
 *   <li>Automatic destruction of objects that fail to be added</li>
 *   <li>Memory-based capacity constraints via MemoryMeasure</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjectPool<MyPoolable> pool = PoolFactory.createObjectPool(10);
 *
 * // Add object to pool
 * MyPoolable obj = new MyPoolable();
 * if (!pool.add(obj)) {
 *     obj.destroy(Caller.PUT_ADD_FAILURE);
 * }
 *
 * // Take object from pool
 * MyPoolable borrowed = pool.take();
 * try {
 *     // use borrowed object
 * } finally {
 *     pool.add(borrowed);   // return to pool
 * }
 * }</pre>
 *
 * @param <E> the type of objects in this pool, must implement Poolable
 * @see Pool
 * @see Poolable
 * @see PoolFactory
 */
public interface ObjectPool<E extends Poolable> extends Pool {

    /**
     * Adds a new object to the pool.
     * The object will only be added if the pool has capacity and the object has not expired.
     *
     * <p>This method will fail if:</p>
     * <ul>
     *   <li>The pool is at capacity and auto-balancing is disabled</li>
     *   <li>The object has already expired</li>
     *   <li>The object would exceed memory constraints</li>
     * </ul>
     *
     * @param e the object to be added to the pool, must not be {@code null}
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean add(E e);

    /**
     * Adds a new object to the pool with optional automatic destruction on failure.
     * This is a convenience method that ensures proper cleanup if the object cannot be pooled.
     *
     * <p><b>Execution Order:</b></p>
     * <ol>
     *   <li>Attempts to add the object to the pool using {@link #add(Poolable)}</li>
     *   <li>If add fails and {@code autoDestroyOnFailedToAdd} is {@code true}, calls {@code e.destroy(PUT_ADD_FAILURE)}</li>
     *   <li>Returns the success status of the add operation</li>
     * </ol>
     *
     * <p>The destroy operation is guaranteed to execute in a finally block if the add fails,
     * even if an exception occurs during the add attempt. This ensures no resource leaks.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyPoolable obj = createExpensiveObject();
     * // Object will be automatically destroyed if it can't be added
     * pool.add(obj, true);
     * }</pre>
     *
     * @param e the object to be added to the pool, must not be {@code null}
     * @param autoDestroyOnFailedToAdd if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if the object was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean add(E e, boolean autoDestroyOnFailedToAdd);

    /**
     * Attempts to add an object to the pool, waiting if necessary for space to become available.
     * This method blocks until space is available, the timeout expires, or the thread is interrupted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyPoolable obj = new MyPoolable();
     * if (pool.add(obj, 5, TimeUnit.SECONDS)) {
     *     // Successfully added
     * } else {
     *     // Timeout - handle the object
     *     obj.destroy(Caller.PUT_ADD_FAILURE);
     * }
     * }</pre>
     *
     * @param e the object to be added to the pool, must not be {@code null}
     * @param timeout the maximum time to wait for space to become available
     * @param unit the time unit of the timeout argument
     * @return {@code true} if successful, {@code false} if timeout elapsed before space was available
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean add(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Attempts to add an object to the pool with timeout and automatic destruction on failure.
     * Combines the timeout and auto-destroy features for maximum convenience.
     *
     * <p><b>Execution Order:</b></p>
     * <ol>
     *   <li>Attempts to add the object to the pool using {@link #add(Poolable, long, TimeUnit)}, waiting up to the specified timeout</li>
     *   <li>If add fails (timeout or capacity) and {@code autoDestroyOnFailedToAdd} is {@code true}, calls {@code e.destroy(PUT_ADD_FAILURE)}</li>
     *   <li>Returns the success status of the add operation</li>
     * </ol>
     *
     * <p>The destroy operation is guaranteed to execute in a finally block if the add fails,
     * even if an exception occurs during the add attempt. This ensures no resource leaks.</p>
     *
     * @param e the object to be added to the pool, must not be {@code null}
     * @param timeout the maximum time to wait for space to become available
     * @param unit the time unit of the timeout argument
     * @param autoDestroyOnFailedToAdd if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if add fails
     * @return {@code true} if successful, {@code false} if timeout elapsed or add failed
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalArgumentException if the object is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean add(E e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException;

    /**
     * Retrieves and removes an object from the pool, or returns {@code null} if the pool is empty.
     * The object's activity print is updated to reflect this access.
     *
     * <p>If the retrieved object has expired, it will be destroyed and {@code null} will be returned.
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
     * }
     * }</pre>
     *
     * @return an object from the pool, or {@code null} if the pool is empty
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E take();

    /**
     * Retrieves and removes an object from the pool, waiting if necessary for an object to become available.
     * This method blocks until an object is available, the timeout expires, or the thread is interrupted.
     *
     * <p>The object's activity print is updated to reflect the access.
     * Expired objects are automatically destroyed and the method continues waiting.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E obj = pool.take(10, TimeUnit.SECONDS);
     * if (obj != null) {
     *     try {
     *         // use the object
     *     } finally {
     *         pool.add(obj);   // return to pool
     *     }
     * } else {
     *     // timeout - pool was empty
     * }
     * }</pre>
     *
     * @param timeout the maximum time to wait for an object to become available
     * @param unit the time unit of the timeout argument
     * @return an object from the pool, or {@code null} if the timeout elapsed before an object was available
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E take(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Checks if the specified object is currently in the pool.
     * This method uses object equality (equals method) for comparison.
     *
     * @param valueToFind the object to search for in the pool
     * @return {@code true} if the pool contains the specified object, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     */
    boolean contains(E valueToFind);

    /**
     * Interface for measuring the memory size of objects in the pool.
     * This allows the pool to enforce memory-based capacity limits in addition to count-based limits.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with ByteBuffer wrapper
     * class PooledBuffer extends AbstractPoolable {
     *     private final ByteBuffer buffer;
     *     public PooledBuffer(int capacity) { this.buffer = ByteBuffer.allocate(capacity); }
     *     public int capacity() { return buffer.capacity(); }
     * }
     *
     * MemoryMeasure<PooledBuffer> measure = buffer -> buffer.capacity();
     * ObjectPool<PooledBuffer> pool = PoolFactory.createObjectPool(
     *     100, 3000, EvictionPolicy.LAST_ACCESS_TIME,
     *     1024 * 1024 * 100, // 100MB max
     *     measure
     * );
     * }</pre>
     * 
     * @param <E> the type of objects being measured
     */
    interface MemoryMeasure<E> {

        /**
         * Calculates the memory size of the given object in bytes.
         * The returned value is used to track total memory usage and enforce memory limits.
         * 
         * @param e the object to measure, never {@code null} when called by the pool
         * @return the size of the object in bytes, should be non-negative
         */
        long sizeOf(E e);
    }
}
