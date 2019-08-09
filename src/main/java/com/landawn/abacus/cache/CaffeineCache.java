/*
 * Copyright (C) 2017 HaiYang Li
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

import com.github.benmanes.caffeine.cache.Cache;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * TODO.
 *
 * @author haiyang li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.9
 */
public class CaffeineCache<K, V> extends AbstractCache<K, V> {

    /** The cache impl. */
    private final Cache<K, V> cacheImpl;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new caffeine cache.
     *
     * @param cache the cache
     */
    public CaffeineCache(Cache<K, V> cache) {
        this.cacheImpl = cache;
    }

    /**
     * Gets the t.
     *
     * @param k the k
     * @return the t
     */
    @Override
    public V gett(K k) {
        assertNotClosed();

        return cacheImpl.getIfPresent(k);
    }

    /**
     * Put.
     *
     * @param k the k
     * @param v the v
     * @param liveTime the live time
     * @param maxIdleTime the max idle time
     * @return true, if successful
     */
    @Override
    public boolean put(K k, V v, long liveTime, long maxIdleTime) {
        assertNotClosed();

        cacheImpl.put(k, v); // TODO

        return true;
    }

    /**
     * Removes the.
     *
     * @param k the k
     */
    @Override
    public void remove(K k) {
        assertNotClosed();

        cacheImpl.invalidate(k);
    }

    /**
     * Contains key.
     *
     * @param k the k
     * @return true, if successful
     */
    @Override
    public boolean containsKey(K k) {
        assertNotClosed();

        return get(k).isPresent();
    }

    /**
     * Key set.
     *
     * @return the sets the
     */
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Size.
     *
     * @return the int
     */
    @Override
    public int size() {
        assertNotClosed();

        return N.toIntExact(cacheImpl.estimatedSize());
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        assertNotClosed();

        cacheImpl.cleanUp();
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        assertNotClosed();

        clear();

        isClosed = true;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Assert not closed.
     */
    protected void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This object pool has been closed");
        }
    }
}
