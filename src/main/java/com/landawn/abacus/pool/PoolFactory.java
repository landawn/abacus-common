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

/**
 * Factory class for creating various types of object pools.
 * This class provides static factory methods to create ObjectPool and KeyedObjectPool instances
 * with different configurations.
 *
 * <p>The factory methods allow creating pools with various combinations of:
 * <ul>
 *   <li>Capacity limits</li>
 *   <li>Eviction delays and policies</li>
 *   <li>Auto-balancing behavior</li>
 *   <li>Memory-based constraints</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example Poolable resource class
 * class MyResource extends AbstractPoolable {
 *     // ... resource implementation
 * }
 *
 * // Simple object pool with capacity 100
 * ObjectPool<MyResource> pool1 = PoolFactory.createObjectPool(100);
 *
 * // Object pool with eviction every 5 minutes
 * ObjectPool<MyResource> pool2 = PoolFactory.createObjectPool(100, 300000);
 *
 * // Keyed pool with custom eviction policy
 * class DBConnection extends AbstractPoolable {
 *     // ... connection implementation
 * }
 * KeyedObjectPool<String, DBConnection> pool3 = PoolFactory.createKeyedObjectPool(
 *     50, 60000, EvictionPolicy.ACCESS_COUNT
 * );
 *
 * // Pool with memory constraints
 * class PooledBuffer extends AbstractPoolable {
 *     private int capacity;
 *     public int capacity() { return capacity; }
 * }
 * ObjectPool.MemoryMeasure<PooledBuffer> measure = buffer -> buffer.capacity();
 * ObjectPool<PooledBuffer> pool4 = PoolFactory.createObjectPool(
 *     1000, 30000, EvictionPolicy.LAST_ACCESS_TIME,
 *     1024 * 1024 * 100, // 100MB max
 *     measure
 * );
 * }</pre>
 *
 * @see ObjectPool
 * @see KeyedObjectPool
 * @see GenericObjectPool
 * @see GenericKeyedObjectPool
 */
public abstract class PoolFactory { //NOSONAR

    private PoolFactory() {
        // singleton
    }

    /**
     * Creates a new ObjectPool with the specified capacity.
     * Uses default eviction delay of 3 seconds and LAST_ACCESS_TIME eviction policy.
     * 
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @return a new ObjectPool instance with the specified capacity
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity) {
        return new GenericObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new ObjectPool with the specified capacity and eviction delay.
     * Uses LAST_ACCESS_TIME eviction policy.
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @return a new ObjectPool instance with the specified capacity and eviction delay
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay) {
        return new GenericObjectPool<>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new ObjectPool with the specified capacity, eviction delay, and eviction policy.
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting elements (LAST_ACCESS_TIME, ACCESS_COUNT, or EXPIRATION_TIME)
     * @return a new ObjectPool instance with the specified configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy);
    }

    /**
     * Creates a new ObjectPool with memory-based capacity constraints.
     * Uses default auto-balancing with a balance factor of 0.2.
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting elements
     * @param maxMemorySize the maximum total memory in bytes the pool can use
     * @param memoryMeasure the function to calculate memory size of pool elements
     * @return a new ObjectPool instance with memory constraints
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new ObjectPool with custom auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting elements
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing (typically 0.1 to 0.5)
     * @return a new ObjectPool instance with custom balancing configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new ObjectPool with full configuration options including memory constraints and auto-balancing.
     * This is the most flexible factory method for creating object pools.
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting elements (default: LAST_ACCESS_TIME)
     * @param autoBalance whether to automatically remove objects when the pool is full (default: true)
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5 (default: 0.2)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no memory limit
     * @param memoryMeasure the function to calculate memory size of pool elements, or {@code null} if not using memory limits
     * @return a new ObjectPool instance with full configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity.
     * Uses default eviction delay of 3 seconds and LAST_ACCESS_TIME eviction policy.
     * 
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @return a new KeyedObjectPool instance with the specified capacity
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity) {
        return new GenericKeyedObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity and eviction delay.
     * Uses LAST_ACCESS_TIME eviction policy.
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @return a new KeyedObjectPool instance with the specified capacity and eviction delay
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity, eviction delay, and eviction policy.
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting entries (LAST_ACCESS_TIME, ACCESS_COUNT, or EXPIRATION_TIME)
     * @return a new KeyedObjectPool instance with the specified configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy);
    }

    /**
     * Creates a new KeyedObjectPool with memory-based capacity constraints.
     * Uses default auto-balancing with a balance factor of 0.2.
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting entries
     * @param maxMemorySize the maximum total memory in bytes the pool can use
     * @param memoryMeasure the function to calculate memory size of key-value pairs
     * @return a new KeyedObjectPool instance with memory constraints
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new KeyedObjectPool with custom auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting entries
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing (typically 0.1 to 0.5)
     * @return a new KeyedObjectPool instance with custom balancing configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new KeyedObjectPool with full configuration options including memory constraints and auto-balancing.
     * This is the most flexible factory method for creating keyed object pools.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example Poolable connection class
     * class DBConnection extends AbstractPoolable {
     *     private long memoryUsage = 1024;  // example size in bytes
     *     public long getMemoryUsage() { return memoryUsage; }
     * }
     *
     * KeyedObjectPool.MemoryMeasure<String, DBConnection> measure = (key, conn) ->
     *     key.length() * 2 + conn.getMemoryUsage();
     * KeyedObjectPool<String, DBConnection> pool = PoolFactory.createKeyedObjectPool(
     *     100, 60000, EvictionPolicy.LAST_ACCESS_TIME,
     *     true, 0.3f, 1024*1024*50, measure
     * );
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelay the interval in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for evicting entries (default: LAST_ACCESS_TIME)
     * @param autoBalance whether to automatically remove entries when the pool is full (default: true)
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5 (default: 0.2)
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no memory limit
     * @param memoryMeasure the function to calculate memory size of key-value pairs, or {@code null} if not using memory limits
     * @return a new KeyedObjectPool instance with full configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }
}
