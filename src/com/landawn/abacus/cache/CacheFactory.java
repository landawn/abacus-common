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

import static com.landawn.abacus.cache.DistributedCacheClient.DEFAULT_TIMEOUT;

import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.TypeAttrParser;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class CacheFactory {
    private CacheFactory() {
    }

    public static <K, V> LocalCache<K, V> createLocalCache(final int capacity, final long evictDelay) {
        return new LocalCache<K, V>(capacity, evictDelay);
    }

    /**
     * 
     * @param capacity
     * @param evictDelay
     * @param defaultLiveTime default value is 3 hours
     * @param defaultMaxIdleTime default value is 30 minutes
     * @return
     */
    public static <K, V> LocalCache<K, V> createLocalCache(final int capacity, final long evictDelay, final long defaultLiveTime,
            final long defaultMaxIdleTime) {
        return new LocalCache<K, V>(capacity, evictDelay, defaultLiveTime, defaultMaxIdleTime);
    }

    public static <K, V> DistributedCache<K, V> createDistributedCache(final DistributedCacheClient<V> dcc) {
        return new DistributedCache<K, V>(dcc);
    }

    public static <K, V> DistributedCache<K, V> createDistributedCache(final DistributedCacheClient<V> dcc, final String keyPrefix) {
        return new DistributedCache<K, V>(dcc, keyPrefix);
    }

    public static <K, V> DistributedCache<K, V> createDistributedCache(final DistributedCacheClient<V> dcc, final String keyPrefix,
            final int maxFailedNumForRetry, final long retryDelay) {
        return new DistributedCache<K, V>(dcc, keyPrefix, maxFailedNumForRetry, retryDelay);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Cache<K, V> createCache(final String provider) {
        TypeAttrParser attrResult = TypeAttrParser.parse(provider);
        String[] parameters = attrResult.getParameters();
        String url = parameters[0];
        String className = attrResult.getClassName();
        Class<?> cls = null;

        if (DistributedCacheClient.MEMCACHED.equalsIgnoreCase(className)) {
            final net.spy.memcached.transcoders.Transcoder<Object> transcoder = ParserFactory.isKryoAvailable() ? new KryoTranscoder<Object>() : null;

            if (parameters.length == 1) {
                return new DistributedCache<K, V>(new SpyMemcached<V>(url, DEFAULT_TIMEOUT, transcoder));
            } else if (parameters.length == 2) {
                return new DistributedCache<K, V>(new SpyMemcached<V>(url, DEFAULT_TIMEOUT, transcoder), parameters[1]);
            } else if (parameters.length == 3) {
                return new DistributedCache<K, V>(new SpyMemcached<V>(url, N.parseLong(parameters[2]), transcoder), parameters[1]);
            } else {
                throw new IllegalArgumentException("Unsupported parameters: " + StringUtil.join(parameters));
            }
        } else if (DistributedCacheClient.REDIS.equalsIgnoreCase(className)) {
            if (parameters.length == 1) {
                return new DistributedCache<K, V>(new JRedis<V>(url, DEFAULT_TIMEOUT));
            } else if (parameters.length == 2) {
                return new DistributedCache<K, V>(new JRedis<V>(url, DEFAULT_TIMEOUT), parameters[1]);
            } else if (parameters.length == 3) {
                return new DistributedCache<K, V>(new JRedis<V>(url, N.parseLong(parameters[2])), parameters[1]);
            } else {
                throw new IllegalArgumentException("Unsupported parameters: " + StringUtil.join(parameters));
            }
        } else {
            cls = ClassUtil.forClass(className);

            return (Cache<K, V>) TypeAttrParser.newInstance(cls, provider);
        }
    }
}
