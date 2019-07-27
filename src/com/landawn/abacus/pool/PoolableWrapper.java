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

import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class PoolableWrapper<T> extends AbstractPoolable {
    private T srcObject;

    /**
     * Wrap the the source object with <code>Long.MAX_VALUE</code> <code>liveTime</code> and <code>Long.MAX_VALUE</code> <code>maxIdleTime</code>.
     * 
     * @param srcObject
     */
    public PoolableWrapper(T srcObject) {
        this(srcObject, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    public PoolableWrapper(T srcObject, long liveTime, long maxIdleTime) {
        super(liveTime, maxIdleTime);
        this.srcObject = srcObject;
    }

    /**
     * Wrap the the source object with <code>Long.MAX_VALUE</code> <code>liveTime</code> and <code>Long.MAX_VALUE</code> <code>maxIdleTime</code>.
     * 
     * @param srcObject
     * @return
     */
    public static <T> PoolableWrapper<T> of(T srcObject) {
        return new PoolableWrapper<T>(srcObject);
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
    public static <T> PoolableWrapper<T> of(T srcObject, long liveTime, long maxIdleTime) {
        return new PoolableWrapper<T>(srcObject, liveTime, maxIdleTime);
    }

    /**
     * @return T
     */
    public T value() {
        return srcObject;
    }

    @Override
    public void destroy() {
        // should not set the srcobject to null because it may be retrieved by
        // other thread and evicted out pool later.
        // srcObject = null;
    }

    @Override
    public int hashCode() {
        return N.hashCode(srcObject.hashCode());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PoolableWrapper && N.equals(((PoolableWrapper<?>) obj).srcObject, srcObject));
    }

    @Override
    public String toString() {
        return "{srcObject=" + srcObject + "; activityPrint=" + activityPrint + "}";
    }
}
