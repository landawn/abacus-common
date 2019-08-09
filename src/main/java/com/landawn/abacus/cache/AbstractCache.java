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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.u.Optional;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractCache.
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    /** The async executor. */
    protected final AsyncExecutor asyncExecutor = new AsyncExecutor(Math.min(8, IOUtil.CPU_CORES), 64, 180L, TimeUnit.SECONDS);

    /** The properties. */
    protected final Properties<String, Object> properties = new Properties<>();

    /** The default live time. */
    protected long defaultLiveTime;

    /** The default max idle time. */
    protected long defaultMaxIdleTime;

    /**
     * Instantiates a new abstract cache.
     */
    protected AbstractCache() {
        this(DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
    }

    /**
     * Instantiates a new abstract cache.
     *
     * @param defaultLiveTime the default live time
     * @param defaultMaxIdleTime the default max idle time
     */
    protected AbstractCache(long defaultLiveTime, long defaultMaxIdleTime) {
        this.defaultLiveTime = defaultLiveTime;
        this.defaultMaxIdleTime = defaultMaxIdleTime;
    }

    /**
     * Gets the.
     *
     * @param k the k
     * @return the optional
     */
    @Override
    public Optional<V> get(K k) {
        return Optional.ofNullable(gett(k));
    }

    /**
     * Put.
     *
     * @param key the key
     * @param value the value
     * @return true, if successful
     */
    @Override
    public boolean put(K key, V value) {
        return put(key, value, defaultLiveTime, defaultMaxIdleTime);
    }

    /**
     * Async get.
     *
     * @param k the k
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<Optional<V>> asyncGet(final K k) {
        return asyncExecutor.execute(new Callable<Optional<V>>() {
            @Override
            public Optional<V> call() throws Exception {
                return get(k);
            }
        });
    }

    /**
     * Async gett.
     *
     * @param k the k
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<V> asyncGett(final K k) {
        return asyncExecutor.execute(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return gett(k);
            }
        });
    }

    /**
     * Async put.
     *
     * @param k the k
     * @param v the v
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<Boolean> asyncPut(final K k, final V v) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return put(k, v);
            }
        });
    }

    /**
     * Async put.
     *
     * @param k the k
     * @param v the v
     * @param liveTime the live time
     * @param maxIdleTime the max idle time
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<Boolean> asyncPut(final K k, final V v, final long liveTime, final long maxIdleTime) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return put(k, v, liveTime, maxIdleTime);
            }
        });
    }

    /**
     * Async remove.
     *
     * @param k the k
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<Void> asyncRemove(final K k) {
        return asyncExecutor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                remove(k);

                return null;
            }
        });
    }

    /**
     * Async contains key.
     *
     * @param k the k
     * @return the continuable future
     */
    @Override
    public ContinuableFuture<Boolean> asyncContainsKey(final K k) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return containsKey(k);
            }
        });
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    @Override
    public Properties<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gets the property.
     *
     * @param <T> the generic type
     * @param propName the prop name
     * @return the property
     */
    @Override
    public <T> T getProperty(String propName) {
        return (T) properties.get(propName);
    }

    /**
     * Sets the property.
     *
     * @param <T> the generic type
     * @param propName the prop name
     * @param propValue the prop value
     * @return the t
     */
    @Override
    public <T> T setProperty(String propName, Object propValue) {
        return (T) properties.put(propName, propValue);
    }

    /**
     * Removes the property.
     *
     * @param <T> the generic type
     * @param propName the prop name
     * @return the t
     */
    @Override
    public <T> T removeProperty(String propName) {
        return (T) properties.remove(propName);
    }
}
