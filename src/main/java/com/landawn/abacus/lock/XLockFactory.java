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
 * A factory for creating XLock objects.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class XLockFactory {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(XLockFactory.class);

    /**
     * Instantiates a new x lock factory.
     */
    private XLockFactory() {
        // singleton
    }

    /**
     * Creates a new XLock object.
     *
     * @param <T>
     * @return
     */
    public static <T> XLock<T> createLocalXLock() {
        return new LocalXLock<T>();
    }

    /**
     * Creates a new XLock object.
     *
     * @param <T>
     * @param timeout
     * @return
     */
    public static <T> XLock<T> createLocalXLock(long timeout) {
        return new LocalXLock<T>(timeout);
    }

    /**
     * Creates a new XLock object.
     *
     * @param <T>
     * @param provider
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> XLock<T> createLock(String provider) {
        if (logger.isInfoEnabled()) {
            logger.info("creating lock: " + provider);
        }

        TypeAttrParser attrResult = TypeAttrParser.parse(provider);
        String clsName = attrResult.getClassName();

        Class<?> cls = null;

        if (DistributedCacheClient.MEMCACHED.equals(clsName)) {
            cls = MemcachedXLock.class;
        } else {
            cls = ClassUtil.forClass(clsName);
        }

        return (XLock<T>) TypeAttrParser.newInstance(cls, provider);
    }
}
