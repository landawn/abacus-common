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

import java.util.Collection;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Interface DistributedCacheClient.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public interface DistributedCacheClient<T> {

    /** The Constant DEFAULT_TIMEOUT. */
    public static final long DEFAULT_TIMEOUT = 1000;

    /** The Constant MEMCACHED. */
    public static final String MEMCACHED = "Memcached";

    /** The Constant REDIS. */
    public static final String REDIS = "Redis";

    /**
     *
     * @return
     */
    String serverUrl();

    /**
     *
     * @param key
     * @return
     */
    T get(String key);

    /**
     * Gets the bulk.
     *
     * @param keys
     * @return
     */
    Map<String, T> getBulk(String... keys);

    /**
     * Gets the bulk.
     *
     * @param keys
     * @return
     */
    Map<String, T> getBulk(Collection<String> keys);

    /**
     *
     * @param key
     * @param obj
     * @param liveTime
     * @return true, if successful
     */
    boolean set(String key, T obj, long liveTime);

    /**
     *
     * @param key
     * @return true, if successful
     */
    boolean delete(String key);

    /**
     *
     * @param key
     * @return
     */
    long incr(String key);

    /**
     *
     * @param key
     * @param deta
     * @return
     */
    long incr(String key, int deta);

    /**
     *
     * @param key
     * @return
     */
    long decr(String key);

    /**
     *
     * @param key
     * @param deta
     * @return
     */
    long decr(String key, int deta);

    /**
     * Delete all the keys from all the servers.
     */
    void flushAll();

    /**
     * Disconnect.
     */
    void disconnect();
}
