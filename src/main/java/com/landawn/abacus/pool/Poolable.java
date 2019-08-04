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

package com.landawn.abacus.pool;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface Poolable {
    /**
     * 
     * @return ActivityPrint
     */
    ActivityPrint activityPrint();

    /**
     * Method destroy.
     */
    void destroy();

    /**
     * Wrap the the source object with <code>Long.MAX_VALUE</code> <code>liveTime</code> and <code>Long.MAX_VALUE</code> <code>maxIdleTime</code>.
     * 
     * @param srcObject
     * @return
     */
    public static <T> PoolableWrapper<T> wrap(T srcObject) {
        return PoolableWrapper.of(srcObject);
    }

    /**
     * Wrap the the source object with specified <code>liveTime</code> and <code>maxIdleTime</code>.
     * 
     * @param <T>
     * @param srcObject
     * @param liveTime
     * @param maxIdleTime
     * @return
     */
    public static <T> PoolableWrapper<T> wrap(T srcObject, long liveTime, long maxIdleTime) {
        return PoolableWrapper.of(srcObject, liveTime, maxIdleTime);
    }
}
