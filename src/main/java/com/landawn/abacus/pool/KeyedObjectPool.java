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
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {
    /**
     * Method put.
     * 
     * @param key
     * @param e
     * @return boolean
     */
    boolean put(K key, E e);

    /**
     * Method put.
     * 
     * @param key
     * @param e
     * @param autoDestroyOnFailedToPut
     * @return boolean
     */
    boolean put(K key, E e, boolean autoDestroyOnFailedToPut);

    /**
     * Method get.
     * 
     * @param key
     * @return E
     */
    E get(K key);

    /**
     * Method remove.
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
     * @param key
     * @return boolean
     */
    boolean containsKey(K key);

    /**
     * Method containsValue.
     * 
     * @param e
     * @return boolean
     */
    boolean containsValue(E e);

    public static interface MemoryMeasure<K, E> {
        long sizeOf(K key, E e);
    }
}
