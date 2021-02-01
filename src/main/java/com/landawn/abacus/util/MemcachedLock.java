/*
 * Copyright (c) 2015, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import com.landawn.abacus.cache.SpyMemcached;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.8
 */
public final class MemcachedLock<K, V> {

    static final Logger logger = LoggerFactory.getLogger(MemcachedLock.class);

    private final SpyMemcached<V> mc;

    public MemcachedLock(String serverUrl) {
        mc = new SpyMemcached<>(serverUrl);
    }

    /**
     *
     * @param target
     * @param liveTime
     * @return
     */
    public boolean lock(K target, long liveTime) {
        return lock(target, (V) N.EMPTY_BYTE_ARRAY, liveTime);
    }

    /**
     *
     * @param target
     * @param value
     * @param liveTime unit is milliseconds
     * @return
     */
    public boolean lock(K target, V value, long liveTime) {
        String key = toKey(target);

        try {
            return mc.add(key, value, liveTime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to lock target with key: " + key, e);
        }
    }

    /**
     * Checks if is locked.
     *
     * @param target
     * @return true, if is locked
     */
    public boolean isLocked(K target) {
        return mc.get(toKey(target)) != null;
    }

    /**
     *
     * @param target
     * @return
     */
    public V get(K target) {
        Object value = mc.get(toKey(target));

        return (V) (value instanceof byte[] && ((byte[]) value).length == 0 ? null : value);
    }

    /**
     *
     * @param target
     * @return
     */
    public boolean unlock(K target) {
        try {
            return mc.delete(toKey(target));
        } catch (Exception e) {
            throw new RuntimeException("Failed to unlock with key: " + target, e);
        }
    }

    /**
     *
     * @param target
     * @return
     */
    protected String toKey(K target) {
        return N.stringOf(target);
    }

    public SpyMemcached<V> client() {
        return mc;
    }
}
