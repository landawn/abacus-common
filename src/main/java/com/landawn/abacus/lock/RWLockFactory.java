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

package com.landawn.abacus.lock;

import com.landawn.abacus.cache.DistributedCacheClient;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.TypeAttrParser;

// TODO: Auto-generated Javadoc
/**
 * A factory for creating RWLock objects.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class RWLockFactory {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RWLockFactory.class);

    /**
     * Instantiates a new RW lock factory.
     */
    private RWLockFactory() {
        // singleton
    }

    /**
     * Creates a new RWLock object.
     *
     * @param <T>
     * @return
     */
    public static <T> RWLock<T> createLocalRWLock() {
        return new LocalRWLock<T>();
    }

    /**
     * Creates a new RWLock object.
     *
     * @param <T>
     * @param timeout
     * @return
     */
    public static <T> RWLock<T> createLocalRWLock(long timeout) {
        return new LocalRWLock<T>(timeout);
    }

    /**
     * Creates a new RWLock object.
     *
     * @param <T>
     * @param provider
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> RWLock<T> createLock(String provider) {
        if (logger.isInfoEnabled()) {
            logger.info("creating lock: " + provider);
        }

        TypeAttrParser attrResult = TypeAttrParser.parse(provider);
        String clsName = attrResult.getClassName();

        Class<?> cls = null;

        if (DistributedCacheClient.MEMCACHED.equals(clsName)) {
            cls = MemcachedRWLock.class;
        } else {
            cls = ClassUtil.forClass(clsName);
        }

        return (RWLock<T>) TypeAttrParser.newInstance(cls, provider);
    }
}
