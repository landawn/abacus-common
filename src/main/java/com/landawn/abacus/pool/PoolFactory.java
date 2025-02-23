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
 * A factory for creating Pool objects.
 *
 */
public abstract class PoolFactory { //NOSONAR

    private PoolFactory() {
        // singleton
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E> the type of elements in the pool
     * @param capacity the capacity of the pool
     * @return a new ObjectPool instance with the specified capacity
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity) {
        return new GenericObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E> the type of elements in the pool
     * @param capacity the capacity of the pool
     * @param evictDelay the interval between eviction runs in milliseconds
     * @return a new ObjectPool instance with the specified capacity and eviction delay
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay) {
        return new GenericObjectPool<>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E> the type of elements in the pool
     * @param capacity the capacity of the pool
     * @param evictDelay the interval between eviction runs in milliseconds
     * @param evictionPolicy the policy to use for evicting elements
     * @return a new ObjectPool instance with the specified capacity, eviction delay, and eviction policy
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param maxMemorySize
     * @param memoryMeasure
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param autoBalance
     * @param balanceFactor
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity the capacity of the pool
     * @param evictDelay the interval between eviction runs in milliseconds
     * @param evictionPolicy default value is {@code EvictionPolicy.LAST_ACCESS_TIME}
     * @param autoBalance default value is {@code true}
     * @param balanceFactor default value is {@code 0.2}
     * @param maxMemorySize
     * @param memoryMeasure
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(final int capacity, final long evictDelay, final EvictionPolicy evictionPolicy,
            final boolean autoBalance, final float balanceFactor, final long maxMemorySize, final ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity) {
        return new GenericKeyedObjectPool<>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param maxMemorySize
     * @param memoryMeasure
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final long maxMemorySize, final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @param autoBalance
     * @param balanceFactor
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new KeyedObjectPool object.
     *
     * @param <K> the key type
     * @param <E> the type of elements in the pool
     * @param capacity the capacity of the pool
     * @param evictDelay the interval between eviction runs in milliseconds
     * @param evictionPolicy the policy to use for evicting elements. Default value is {@code EvictionPolicy.LAST_ACCESS_TIME}
     * @param autoBalance whether to automatically balance the pool. Default value is {@code true}
     * @param balanceFactor the factor used for balancing the pool. Default value is {@code 0.2}
     * @param maxMemorySize the maximum memory size for the pool
     * @param memoryMeasure the memory measure for the pool elements
     * @return a new KeyedObjectPool instance with the specified parameters
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(final int capacity, final long evictDelay,
            final EvictionPolicy evictionPolicy, final boolean autoBalance, final float balanceFactor, final long maxMemorySize,
            final KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }
}
