/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class PoolFactory {

    /**
     * Instantiates a new pool factory.
     */
    private PoolFactory() {
        // singleton
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity) {
        return new GenericObjectPool<E>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity, long evictDelay) {
        return new GenericObjectPool<E>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy) {
        return new GenericObjectPool<E>(capacity, evictDelay, evictionPolicy);
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
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, long maxMemorySize,
            ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<E>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
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
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance,
            float balanceFactor) {
        return new GenericObjectPool<E>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy default value is <code>EvictionPolicy.LAST_ACCESS_TIME</code>
     * @param autoBalance default value is <code>true</code>
     * @param balanceFactor default value is <code>0.2</code>
     * @param maxMemorySize
     * @param memoryMeasure
     * @return
     */
    public static <E extends Poolable> ObjectPool<E> createObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance,
            float balanceFactor, long maxMemorySize, ObjectPool.MemoryMeasure<E> memoryMeasure) {
        return new GenericObjectPool<E>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity) {
        return new GenericKeyedObjectPool<K, E>(capacity, AbstractPool.DEFAULT_EVICT_DELAY, EvictionPolicy.LAST_ACCESS_TIME);
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
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity, long evictDelay) {
        return new GenericKeyedObjectPool<K, E>(capacity, evictDelay, EvictionPolicy.LAST_ACCESS_TIME);
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
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy) {
        return new GenericKeyedObjectPool<K, E>(capacity, evictDelay, evictionPolicy);
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
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy,
            long maxMemorySize, KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<K, E>(capacity, evictDelay, evictionPolicy, maxMemorySize, memoryMeasure);
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
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy,
            boolean autoBalance, float balanceFactor) {
        return new GenericKeyedObjectPool<K, E>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor);
    }

    /**
     * Creates a new Pool object.
     *
     * @param <K> the key type
     * @param <E>
     * @param capacity
     * @param evictDelay
     * @param evictionPolicy default value is <code>EvictionPolicy.LAST_ACCESS_TIME</code>
     * @param autoBalance default value is <code>true</code>
     * @param balanceFactor default value is <code>0.2</code>
     * @param maxMemorySize
     * @param memoryMeasure
     * @return
     */
    public static <K, E extends Poolable> KeyedObjectPool<K, E> createKeyedObjectPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy,
            boolean autoBalance, float balanceFactor, long maxMemorySize, KeyedObjectPool.MemoryMeasure<K, E> memoryMeasure) {
        return new GenericKeyedObjectPool<K, E>(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize, memoryMeasure);
    }
}
