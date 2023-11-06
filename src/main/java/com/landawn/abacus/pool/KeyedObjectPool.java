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

import java.util.Collection;
import java.util.Set;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E>
 * @since 0.8
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {

    /**
     *
     * @param key
     * @param e
     * @return boolean
     */
    boolean put(K key, E e);

    /**
     *
     * @param key
     * @param e
     * @param autoDestroyOnFailedToPut
     * @return boolean
     */
    boolean put(K key, E e, boolean autoDestroyOnFailedToPut);

    /**
     *
     * @param key
     * @return E
     */
    E get(K key);

    /**
     *
     * @param key
     * @return E
     */
    E remove(K key);

    /**
     * Get but don't update last access time.
     *
     * @param key
     * @return E
     */
    E peek(K key);

    /**
     *
     * @return Set<K>
     */
    Set<K> keySet();

    /**
     *
     * @return Collection<E>
     */
    Collection<E> values();

    /**
     *
     * @param key
     * @return boolean
     */
    boolean containsKey(K key);

    /**
     *
     * @param e
     * @return boolean
     */
    boolean containsValue(E e);

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
