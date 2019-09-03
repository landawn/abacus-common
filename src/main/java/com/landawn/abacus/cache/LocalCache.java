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

package com.landawn.abacus.cache;

import java.util.Set;

import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.PoolableWrapper;

// TODO: Auto-generated Javadoc
/**
 * The Class LocalCache.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public class LocalCache<K, V> extends AbstractCache<K, V> {

    /** The pool. */
    private final KeyedObjectPool<K, PoolableWrapper<V>> pool;

    /**
     * Instantiates a new local cache.
     *
     * @param capacity
     * @param evictDelay
     */
    public LocalCache(int capacity, long evictDelay) {
        this(capacity, evictDelay, DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
    }

    /**
     * Instantiates a new local cache.
     *
     * @param capacity
     * @param evictDelay
     * @param defaultLiveTime
     * @param defaultMaxIdleTime
     */
    public LocalCache(int capacity, long evictDelay, long defaultLiveTime, long defaultMaxIdleTime) {
        super(defaultLiveTime, defaultMaxIdleTime);

        pool = PoolFactory.createKeyedObjectPool(capacity, evictDelay);
    }

    /**
     * Gets the t.
     *
     * @param key
     * @return
     */
    @Override
    public V gett(K key) {
        final PoolableWrapper<V> w = pool.get(key);

        return w == null ? null : w.value();
    }

    /**
     *
     * @param key
     * @param value
     * @param liveTime
     * @param maxIdleTime
     * @return true, if successful
     */
    @Override
    public boolean put(K key, V value, long liveTime, long maxIdleTime) {
        return pool.put(key, PoolableWrapper.of(value, liveTime, maxIdleTime));
    }

    /**
     *
     * @param key
     */
    @Override
    public void remove(K key) {
        pool.remove(key);
    }

    /**
     *
     * @param key
     * @return true, if successful
     */
    @Override
    public boolean containsKey(K key) {
        return pool.containsKey(key);
    }

    /**
     *
     * @return
     */
    @Override
    public Set<K> keySet() {
        return pool.keySet();
    }

    /**
     *
     * @return
     */
    @Override
    public int size() {
        return pool.size();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        pool.clear();
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        pool.close();
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    @Override
    public boolean isClosed() {
        return pool.isClosed();
    }
}
