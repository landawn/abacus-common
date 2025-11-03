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

import java.util.Collection;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A pool that manages objects associated with keys, similar to a Map but with pooling capabilities.
 * This interface extends Pool to provide key-based storage and retrieval of poolable objects.
 *
 * <p>KeyedObjectPool is useful when you need to pool objects by category or type, such as:
 * <ul>
 *   <li>Database connections per schema or server</li>
 *   <li>Thread pools per task type</li>
 *   <li>Cached computations by input parameters</li>
 *   <li>Resources grouped by tenant or user</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Map-like interface for key-based access</li>
 *   <li>Automatic expiration of entries based on age or inactivity</li>
 *   <li>Memory-based capacity constraints</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KeyedObjectPool<String, DBConnection> pool = PoolFactory.createKeyedObjectPool(100);
 *
 * // Store connection by database name
 * DBConnection conn = new DBConnection("server1");
 * pool.put("database1", conn);
 *
 * // Retrieve connection
 * DBConnection borrowed = pool.get("database1");
 * if (borrowed != null) {
 *     try {
 *         // use connection
 *     } finally {
 *         pool.put("database1", borrowed); // return to pool
 *     }
 * }
 * }</pre>
 *
 * @param <K> the type of keys maintained by this pool
 * @param <E> the type of pooled values, must implement Poolable
 * @see Pool
 * @see Poolable
 * @see PoolFactory
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {

    /**
     * Associates the specified poolable element with the specified key in this pool.
     * If the pool previously contained an element for the key, the old element is destroyed.
     *
     * <p>The put operation will fail if:</p>
     * <ul>
     *   <li>The pool is at capacity and auto-balancing is disabled</li>
     *   <li>The element has already expired</li>
     *   <li>The operation will fail if the element would exceed memory constraints</li>
     * </ul>
     *
     * @param key the key with which the specified element is to be associated, must not be {@code null}
     * @param e the element to be associated with the specified key, must not be {@code null}
     * @return {@code true} if the element was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or element is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean put(K key, E e);

    /**
     * Associates the specified element with the specified key in this pool,
     * with optional automatic destruction on failure.
     *
     * <p>This is a convenience method that ensures proper cleanup if the object cannot be pooled.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume DBConnection extends AbstractPoolable and implements close logic in destroy()
     * DBConnection conn = new DBConnection("server1");
     * // Connection will be destroyed if it can't be added to pool
     * pool.put("db1", conn, true);
     * }</pre>
     *
     * @param key the key with which the specified element is to be associated, must not be {@code null}
     * @param e the element to be associated with the specified key, must not be {@code null}
     * @param autoDestroyOnFailedToPut if {@code true}, calls e.destroy(PUT_ADD_FAILURE) if put fails
     * @return {@code true} if the element was successfully added, {@code false} otherwise
     * @throws IllegalArgumentException if the key or element is null
     * @throws IllegalStateException if the pool has been closed
     */
    boolean put(K key, E e, boolean autoDestroyOnFailedToPut);

    /**
     * Returns the element associated with the specified key, or {@code null} if no mapping exists.
     * The element's activity print is updated to reflect this access.
     *
     * <p>If the retrieved element has expired, it will be destroyed and {@code null} will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E cached = pool.get("myKey");
     * if (cached != null) {
     *     // Use cached object
     * } else {
     *     // Create new object and add to pool
     *     E newObj = createObject();
     *     pool.put("myKey", newObj);
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned
     * @return the element associated with the key, or {@code null} if no mapping exists or the element has expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E get(K key);

    /**
     * Removes and returns the element associated with the specified key.
     * The element's activity print is updated to reflect this access.
     *
     * <p>Unlike {@link #get(Object)}, this method removes the element from the pool,
     * so it will not be available for future requests unless re-added.
     *
     * @param key the key whose mapping is to be removed from the pool
     * @return the element previously associated with the key, or {@code null} if no mapping exists
     * @throws IllegalStateException if the pool has been closed
     */
    E remove(K key);

    /**
     * Returns the element associated with the specified key without updating access statistics.
     * This method is useful for monitoring or administrative purposes.
     * 
     * <p>If the element has expired, it will be removed and destroyed, and {@code null} will be returned.
     * Unlike {@link #get(Object)}, this method does not update the last access time or access count.
     * 
     * @param key the key whose associated element is to be returned
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    E peek(K key);

    /**
     * Returns a Set view of the keys contained in this pool.
     * The returned set is a snapshot and will not reflect subsequent changes to the pool.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> keys = pool.keySet();
     * for (String key : keys) {
     *     System.out.println("Pool contains key: " + key);
     * }
     * }</pre>
     * 
     * @return a set containing all keys currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    Set<K> keySet();

    /**
     * Returns a Collection view of the values contained in this pool.
     * The returned collection is a snapshot and will not reflect subsequent changes to the pool.
     * 
     * @return a collection containing all values currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    Collection<E> values();

    /**
     * Checks if this pool contains a mapping for the specified key.
     * 
     * @param key the key whose presence in this pool is to be tested
     * @return {@code true} if this pool contains a mapping for the specified key, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     */
    boolean containsKey(K key);

    /**
     * Interface for measuring the memory size of key-value pairs in the pool.
     * This allows the pool to enforce memory-based capacity limits.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MemoryMeasure<String, CachedData> measure = (key, data) ->
     *     key.length() * 2 + data.getDataSize(); // * 2 for UTF-16 encoding (2 bytes per char)
     *
     * KeyedObjectPool<String, CachedData> pool = PoolFactory.createKeyedObjectPool(
     *     1000, 3000, EvictionPolicy.LAST_ACCESS_TIME,
     *     1024 * 1024 * 500, // 500MB max
     *     measure
     * );
     * }</pre>
     * 
     * @param <K> the type of keys
     * @param <E> the type of values being measured
     */
    interface MemoryMeasure<K, E> {

        /**
         * Calculates the memory size of the given key-value pair in bytes.
         * The returned value is used to track total memory usage and enforce memory limits.
         * 
         * @param key the key part of the pair, never {@code null} when called by the pool
         * @param e the value part of the pair, never {@code null} when called by the pool
         * @return the combined size of the key-value pair in bytes, should be non-negative
         */
        long sizeOf(K key, E e);
    }
}
