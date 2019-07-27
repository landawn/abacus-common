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

import java.io.Closeable;
import java.util.Set;

import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.u.Optional;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface Cache<K, V> extends Closeable {
    public static final long DEFAULT_LIVE_TIME = 3 * 60 * 60 * 1000L;
    public static final long DEFAULT_MAX_IDLE_TIME = 30 * 60 * 1000L;

    /**
     * 
     * @param k
     * @return V
     */
    Optional<V> get(final K k);

    /**
     * 
     * @param k
     * @return V
     */
    V gett(final K k);

    /**
     * 
     * @param k
     * @return
     */
    ContinuableFuture<Optional<V>> asyncGet(final K k);

    /**
     * 
     * @param k
     * @return
     */
    ContinuableFuture<V> asyncGett(final K k);

    /**
     * 
     * @param k
     * @param v
     * @return
     */
    boolean put(final K k, final V v);

    /**
     * 
     * @param k
     * @param v
     * @return
     */
    ContinuableFuture<Boolean> asyncPut(final K k, final V v);

    /**
     * 
     * @param k
     * @param v
     * @param liveTime
     *            unit is milliseconds
     * @param maxIdleTime
     *            unit is milliseconds
     * @return
     */
    boolean put(final K k, final V v, long liveTime, long maxIdleTime);

    /**
     * 
     * @param k
     * @param v
     * @param liveTime
     * @param maxIdleTime
     * @return
     */
    ContinuableFuture<Boolean> asyncPut(final K k, final V v, long liveTime, long maxIdleTime);

    /**
     * 
     * @param k
     */
    void remove(final K k);

    /**
     * 
     * @param k
     * @return
     */
    ContinuableFuture<Void> asyncRemove(final K k);

    /**
     * 
     * @param k
     * @return boolean
     */
    boolean containsKey(final K k);

    /**
     * 
     * @param k
     * @return
     */
    ContinuableFuture<Boolean> asyncContainsKey(final K k);

    /**
     * @return Set<K>
     */
    Set<K> keySet();

    /**
     * Method size
     * 
     * @return
     */
    int size();

    /**
     * Remove all cached entities from cache pool.
     */
    void clear();

    /**
     * release the resource token by this cache.
     */
    @Override
    void close();

    /**
     * method isClosed
     * 
     * @return
     */
    boolean isClosed();

    /**
     *
     * @return
     */
    Properties<String, Object> getProperties();

    /**
     *
     * @param propName
     * @return
     */
    <T> T getProperty(String propName);

    /**
     * Returns the old value associated with the property by the {@code propName}, {@code null} if it doesn't exist.
     *
     * @param propName
     * @param propValue
     * @return
     */
    <T> T setProperty(String propName, Object propValue);

    /**
     * Returns value of the property which is to be removed, {@code null} if it doesn't exist.
     *
     * @param propName
     * @return
     */
    <T> T removeProperty(String propName);
}
