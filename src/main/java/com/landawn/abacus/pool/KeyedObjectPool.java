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

/**
 *
 * @param <K> the key type
 * @param <E>
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {

    /**
     * Attempts to add a new element associated with the given key to the pool.
     *
     * @param key The key with which the specified element is to be associated.
     * @param e The element to be added to the pool.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean put(K key, E e);

    /**
     * Attempts to add a new element associated with the given key to the pool.
     * If the addition fails and autoDestroyOnFailedToPut is {@code true}, the element will be destroyed.
     *
     * @param key The key with which the specified element is to be associated.
     * @param e The element to be added to the pool.
     * @param autoDestroyOnFailedToPut If {@code true}, the element will be destroyed if it cannot be added to the pool.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean put(K key, E e, boolean autoDestroyOnFailedToPut);

    /**
     * Retrieves the element associated with the given key from the pool.
     *
     * @param key The key whose associated element is to be returned.
     * @return The element associated with the specified key, or {@code null} if the pool contains no mapping for the key.
     */
    E get(K key);

    /**
     * Removes the element associated with the given key from the pool.
     *
     * @param key The key whose associated element is to be removed.
     * @return The element that was associated with the key, or {@code null} if the pool contained no mapping for the key.
     */
    E remove(K key);

    /**
     * Retrieves the element associated with the given key from the pool without updating the last access time.
     *
     * @param key The key whose associated element is to be returned.
     * @return The element associated with the specified key, or {@code null} if the pool contains no mapping for the key.
     */
    E peek(K key);

    /**
     * Retrieves a set of the keys contained in the pool.
     *
     * @return A set of the keys contained in the pool.
     */
    Set<K> keySet();

    /**
     * Retrieves a collection of the values contained in the pool.
     *
     * @return A collection of the values contained in the pool.
     */
    Collection<E> values();

    /**
     * Checks if the pool contains an element associated with the given key.
     *
     * @param key The key to be checked for presence in the pool.
     * @return {@code true} if the pool contains an element associated with the specified key, {@code false} otherwise.
     */
    boolean containsKey(K key);

    //    /**
    //     * Checks if the pool contains the specified element.
    //     *
    //     * @param e The element to be checked for presence in the pool.
    //     * @return true if the pool contains the specified element, false otherwise.
    //     */
    //    boolean containsValue(E e);

    /**
     * The Interface MemoryMeasure.
     *
     * @param <K> the key type
     * @param <E>
     */
    interface MemoryMeasure<K, E> {

        /**
         *
         * @param key
         * @param e
         * @return
         */
        long sizeOf(K key, E e);
    }
}
