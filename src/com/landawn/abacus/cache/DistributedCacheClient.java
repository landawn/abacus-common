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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface DistributedCacheClient<T> {
    public static final long DEFAULT_TIMEOUT = 1000;
    public static final String MEMCACHED = "Memcached";
    public static final String REDIS = "Redis";

    String serverUrl();

    T get(String key);

    Map<String, T> getBulk(String... keys);

    Map<String, T> getBulk(Collection<String> keys);

    boolean set(String key, T obj, long liveTime);

    boolean delete(String key);

    long incr(String key);

    long incr(String key, int deta);

    long decr(String key);

    long decr(String key, int deta);

    /**
     * Delete all the keys from all the servers
     */
    void flushAll();

    void disconnect();
}
