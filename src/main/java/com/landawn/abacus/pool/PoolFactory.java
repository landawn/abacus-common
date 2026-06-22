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
 *     public PooledBuffer() { super(3600000, 600000); }
 *     public int capacity() { return capacity; }
 *     @Override public void destroy(Poolable.Caller caller) {}   // release resources here
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
public final class PoolFactory { //NOSONAR

    private PoolFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a new ObjectPool with the specified capacity.
     * Uses default eviction delay of 3 seconds and LAST_ACCESS_TIME eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(100);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0 (empty until objects are added)
     * pool.isEmpty();   // returns true
     *
     * // A capacity of 0 creates a pool that can never hold an object
     * ObjectPool<Poolable> zeroPool = PoolFactory.createObjectPool(0);
     * zeroPool.capacity();  // returns 0
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @return a new ObjectPool instance with the specified capacity
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity) {
        return new GenericObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY_IN_MILLIS, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new ObjectPool with the specified capacity and eviction delay.
     * Uses LAST_ACCESS_TIME eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Capacity 50, eviction runs every 5 minutes
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(50, 300_000);
     * pool.capacity();  // returns 50
     * pool.size();      // returns 0
     *
     * // Passing 0 as the eviction delay disables periodic eviction
     * ObjectPool<Poolable> noEvict = PoolFactory.createObjectPool(50, 0);
     * noEvict.capacity();  // returns 50
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @return a new ObjectPool instance with the specified capacity and eviction delay
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelayInMillis) {
        return new GenericObjectPool<>(capacity, evictDelayInMillis, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new ObjectPool with the specified capacity, eviction delay, and eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // LFU-style pool: evicts least frequently accessed objects when balancing
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(
     *     100, 60_000, EvictionPolicy.ACCESS_COUNT);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0
     *
     * // Time-based eviction order
     * ObjectPool<Poolable> timePool = PoolFactory.createObjectPool(
     *     200, 60_000, EvictionPolicy.EXPIRATION_TIME);
     * timePool.capacity();  // returns 200
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @return a new ObjectPool instance with the specified configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy) {
        return new GenericObjectPool<>(capacity, evictDelayInMillis, evictionPolicy);
    }

    /**
     * Creates a new ObjectPool with memory-based capacity constraints.
     * Uses default auto-balancing with a balance factor of 0.2.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Each pooled object is measured at a fixed 1 KB; limit the pool to 100 MB total
     * ObjectPool.MemoryMeasure<Poolable> measure = obj -> 1024L;
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(
     *     1000, 30_000, EvictionPolicy.LAST_ACCESS_TIME,
     *     100L * 1024 * 1024, measure);
     * pool.capacity();  // returns 1000
     * pool.size();      // returns 0
     *
     * // Passing 0 for maxMemorySize disables the memory limit
     * ObjectPool<Poolable> noLimit = PoolFactory.createObjectPool(
     *     500, 30_000, EvictionPolicy.LAST_ACCESS_TIME, 0L, measure);
     * noLimit.capacity();  // returns 500
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param maxMemorySize the maximum total memory in bytes the pool can use
     * @param memoryMeasure the function to calculate memory size of pool elements
     * @return a new ObjectPool instance with memory constraints
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy,
            final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new ObjectPool with custom auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Auto-balance enabled, removing 30% of objects when the pool is full
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(
     *     100, 60_000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.3f);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0
     *
     * // Auto-balance disabled: add() simply fails once the pool is full
     * ObjectPool<Poolable> strict = PoolFactory.createObjectPool(
     *     100, 60_000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
     * strict.capacity();  // returns 100
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing (typically 0.1 to 0.5)
     * @return a new ObjectPool instance with custom balancing configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor) {
        return new GenericObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new ObjectPool with full configuration options including memory constraints and auto-balancing.
     * This is the most flexible factory method for creating object pools.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Fully configured pool: auto-balance 25% on full, 50 MB memory cap at 2 KB/object
     * ObjectPool.MemoryMeasure<Poolable> measure = obj -> 2048L;
     * ObjectPool<Poolable> pool = PoolFactory.createObjectPool(
     *     500, 30_000, EvictionPolicy.LAST_ACCESS_TIME,
     *     true, 0.25f, 50L * 1024 * 1024, measure);
     * pool.capacity();  // returns 500
     * pool.size();      // returns 0
     *
     * // No memory limit (maxMemorySize 0, null measure)
     * ObjectPool<Poolable> plain = PoolFactory.createObjectPool(
     *     500, 30_000, EvictionPolicy.ACCESS_COUNT, true, 0.2f, 0L, null);
     * plain.capacity();  // returns 500
     * }</pre>
     *
     * @param <E> the type of elements in the pool, must implement Poolable
     * @param capacity the maximum number of objects the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting objects to evict
     * @param autoBalance whether to automatically remove objects when the pool is full
     * @param balanceFactor the proportion of objects to remove during balancing, typically 0.1 to 0.5
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no memory limit
     * @param memoryMeasure the function to calculate memory size of pool elements, or {@code null} if not using memory limits
     * @return a new ObjectPool instance with full configuration
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelayInMillis, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity.
     * Uses default eviction delay of 3 seconds and LAST_ACCESS_TIME eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KeyedObjectPool<String, Poolable> pool = PoolFactory.createKeyedObjectPool(100);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0 (no keys mapped yet)
     * pool.isEmpty();   // returns true
     *
     * // A capacity of 0 creates a pool that can never hold an entry
     * KeyedObjectPool<String, Poolable> zeroPool = PoolFactory.createKeyedObjectPool(0);
     * zeroPool.capacity();  // returns 0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @return a new KeyedObjectPool instance with the specified capacity
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity) {
        return new GenericKeyedObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY_IN_MILLIS, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity and eviction delay.
     * Uses LAST_ACCESS_TIME eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Capacity 50, eviction runs every minute
     * KeyedObjectPool<String, Poolable> pool = PoolFactory.createKeyedObjectPool(50, 60_000);
     * pool.capacity();  // returns 50
     * pool.size();      // returns 0
     *
     * // Passing 0 as the eviction delay disables periodic eviction
     * KeyedObjectPool<String, Poolable> noEvict = PoolFactory.createKeyedObjectPool(50, 0);
     * noEvict.capacity();  // returns 50
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @return a new KeyedObjectPool instance with the specified capacity and eviction delay
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelayInMillis) {
        return new GenericKeyedObjectPool<>(capacity, evictDelayInMillis, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new KeyedObjectPool with the specified capacity, eviction delay, and eviction policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // LFU-style keyed pool: evicts least frequently accessed entries when balancing
     * KeyedObjectPool<String, Poolable> pool = PoolFactory.createKeyedObjectPool(
     *     100, 60_000, EvictionPolicy.ACCESS_COUNT);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0
     *
     * // Time-based eviction order
     * KeyedObjectPool<Integer, Poolable> timePool = PoolFactory.createKeyedObjectPool(
     *     200, 60_000, EvictionPolicy.EXPIRATION_TIME);
     * timePool.capacity();  // returns 200
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @return a new KeyedObjectPool instance with the specified configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelayInMillis,
            final EvictionPolicy evictionPolicy) {
        return new GenericKeyedObjectPool<>(capacity, evictDelayInMillis, evictionPolicy);
    }

    /**
     * Creates a new KeyedObjectPool with memory-based capacity constraints.
     * Uses default auto-balancing with a balance factor of 0.2.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Measure each entry by key length plus a fixed value size; cap total at 50 MB
     * KeyedObjectPool.MemoryMeasure<String, Poolable> measure =
     *     (key, value) -> key.length() * 2L + 1024L;
     * KeyedObjectPool<String, Poolable> pool = PoolFactory.createKeyedObjectPool(
     *     1000, 30_000, EvictionPolicy.LAST_ACCESS_TIME,
     *     50L * 1024 * 1024, measure);
     * pool.capacity();  // returns 1000
     * pool.size();      // returns 0
     *
     * // Passing 0 for maxMemorySize disables the memory limit
     * KeyedObjectPool<String, Poolable> noLimit = PoolFactory.createKeyedObjectPool(
     *     500, 30_000, EvictionPolicy.LAST_ACCESS_TIME, 0L, measure);
     * noLimit.capacity();  // returns 500
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param maxMemorySize the maximum total memory in bytes the pool can use
     * @param memoryMeasure the function to calculate memory size of key-value pairs
     * @return a new KeyedObjectPool instance with memory constraints
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelayInMillis,
            final EvictionPolicy evictionPolicy, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new KeyedObjectPool with custom auto-balancing configuration.
     * Does not use memory-based constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Auto-balance enabled, removing 30% of entries when the pool is full
     * KeyedObjectPool<String, Poolable> pool = PoolFactory.createKeyedObjectPool(
     *     100, 60_000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.3f);
     * pool.capacity();  // returns 100
     * pool.size();      // returns 0
     *
     * // Auto-balance disabled: put() simply fails once the pool is full
     * KeyedObjectPool<String, Poolable> strict = PoolFactory.createKeyedObjectPool(
     *     100, 60_000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
     * strict.capacity();  // returns 100
     * }</pre>
     *
     * @param <K> the type of keys maintained by the pool
     * @param <E> the type of pooled values, must implement Poolable
     * @param capacity the maximum number of key-value pairs the pool can hold
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing (typically 0.1 to 0.5)
     * @return a new KeyedObjectPool instance with custom balancing configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelayInMillis,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor) {
        return new GenericKeyedObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new KeyedObjectPool with full configuration options including memory constraints and auto-balancing.
     * This is the most flexible factory method for creating keyed object pools.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example Poolable connection class
     * class DBConnection extends AbstractPoolable {
     *     private long memoryUsage = 1024;  // 1024 bytes is the example size
     *     public DBConnection() { super(3600000, 600000); }
     *     public long getMemoryUsage() { return memoryUsage; }
     *     @Override public void destroy(Poolable.Caller caller) {}   // release resources here
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
     * @param evictDelayInMillis the delay in milliseconds between eviction runs, or 0 to disable eviction
     * @param evictionPolicy the policy to use for selecting entries to evict
     * @param autoBalance whether to automatically remove entries when the pool is full
     * @param balanceFactor the proportion of entries to remove during balancing, typically 0.1 to 0.5
     * @param maxMemorySize the maximum total memory in bytes, or 0 for no memory limit
     * @param memoryMeasure the function to calculate memory size of key-value pairs, or {@code null} if not using memory limits
     * @return a new KeyedObjectPool instance with full configuration
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelayInMillis,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelayInMillis, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }
}
