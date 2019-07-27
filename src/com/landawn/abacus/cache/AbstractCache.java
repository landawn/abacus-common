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
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.u.Optional;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {
    protected final AsyncExecutor asyncExecutor = new AsyncExecutor(64, 300L, TimeUnit.SECONDS);
    protected final Properties<String, Object> properties = new Properties<>();

    protected long defaultLiveTime;
    protected long defaultMaxIdleTime;

    protected AbstractCache() {
        this(DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
    }

    protected AbstractCache(long defaultLiveTime, long defaultMaxIdleTime) {
        this.defaultLiveTime = defaultLiveTime;
        this.defaultMaxIdleTime = defaultMaxIdleTime;
    }

    @Override
    public Optional<V> get(K k) {
        return Optional.ofNullable(gett(k));
    }

    @Override
    public boolean put(K key, V value) {
        return put(key, value, defaultLiveTime, defaultMaxIdleTime);
    }

    @Override
    public ContinuableFuture<Optional<V>> asyncGet(final K k) {
        return asyncExecutor.execute(new Callable<Optional<V>>() {
            @Override
            public Optional<V> call() throws Exception {
                return get(k);
            }
        });
    }

    @Override
    public ContinuableFuture<V> asyncGett(final K k) {
        return asyncExecutor.execute(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return gett(k);
            }
        });
    }

    @Override
    public ContinuableFuture<Boolean> asyncPut(final K k, final V v) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return put(k, v);
            }
        });
    }

    @Override
    public ContinuableFuture<Boolean> asyncPut(final K k, final V v, final long liveTime, final long maxIdleTime) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return put(k, v, liveTime, maxIdleTime);
            }
        });
    }

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

    @Override
    public ContinuableFuture<Boolean> asyncContainsKey(final K k) {
        return asyncExecutor.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return containsKey(k);
            }
        });
    }

    @Override
    public Properties<String, Object> getProperties() {
        return properties;
    }

    @Override
    public <T> T getProperty(String propName) {
        return (T) properties.get(propName);
    }

    @Override
    public <T> T setProperty(String propName, Object propValue) {
        return (T) properties.put(propName, propValue);
    }

    @Override
    public <T> T removeProperty(String propName) {
        return (T) properties.remove(propName);
    }
}
