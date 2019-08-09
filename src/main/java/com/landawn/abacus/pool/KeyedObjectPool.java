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

// TODO: Auto-generated Javadoc
/**
 * The Interface KeyedObjectPool.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E> the element type
 * @since 0.8
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {

    /**
     * Method put.
     *
     * @param key the key
     * @param e the e
     * @return boolean
     */
    boolean put(K key, E e);

    /**
     * Method put.
     *
     * @param key the key
     * @param e the e
     * @param autoDestroyOnFailedToPut the auto destroy on failed to put
     * @return boolean
     */
    boolean put(K key, E e, boolean autoDestroyOnFailedToPut);

    /**
     * Method get.
     *
     * @param key the key
     * @return E
     */
    E get(K key);

    /**
     * Method remove.
     *
     * @param key the key
     * @return E
     */
    E remove(K key);

    /**
     * Get but don't update last access time.
     *
     * @param key the key
     * @return E
     */
    E peek(K key);

    /**
     * Method keySet.
     * 
     * @return Set<K>
     */
    Set<K> keySet();

    /**
     * Method values.
     * 
     * @return Collection<E>
     */
    Collection<E> values();

    /**
     * Method containsKey.
     *
     * @param key the key
     * @return boolean
     */
    boolean containsKey(K key);

    /**
     * Method containsValue.
     *
     * @param e the e
     * @return boolean
     */
    boolean containsValue(E e);

    /**
     * The Interface MemoryMeasure.
     *
     * @param <K> the key type
     * @param <E> the element type
     */
    public static interface MemoryMeasure<K, E> {

        /**
         * Size of.
         *
         * @param key the key
         * @param e the e
         * @return the long
         */
        long sizeOf(K key, E e);
    }
}
