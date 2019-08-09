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

// TODO: Auto-generated Javadoc
/**
 * The Class PoolableWrapper.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public final class PoolableWrapper<T> extends AbstractPoolable {

    /** The src object. */
    private T srcObject;

    /**
     * Wrap the the source object with <code>Long.MAX_VALUE</code> <code>liveTime</code> and <code>Long.MAX_VALUE</code> <code>maxIdleTime</code>.
     *
     * @param srcObject the src object
     */
    public PoolableWrapper(T srcObject) {
        this(srcObject, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Instantiates a new poolable wrapper.
     *
     * @param srcObject the src object
     * @param liveTime the live time
     * @param maxIdleTime the max idle time
     */
    public PoolableWrapper(T srcObject, long liveTime, long maxIdleTime) {
        super(liveTime, maxIdleTime);
        this.srcObject = srcObject;
    }

    /**
     * Wrap the the source object with <code>Long.MAX_VALUE</code> <code>liveTime</code> and <code>Long.MAX_VALUE</code> <code>maxIdleTime</code>.
     *
     * @param <T> the generic type
     * @param srcObject the src object
     * @return the poolable wrapper
     */
    public static <T> PoolableWrapper<T> of(T srcObject) {
        return new PoolableWrapper<T>(srcObject);
    }

    /**
     * Wrap the the source object with specified <code>liveTime</code> and <code>maxIdleTime</code>.
     *
     * @param <T> the generic type
     * @param srcObject the src object
     * @param liveTime the live time
     * @param maxIdleTime the max idle time
     * @return the poolable wrapper
     */
    public static <T> PoolableWrapper<T> of(T srcObject, long liveTime, long maxIdleTime) {
        return new PoolableWrapper<T>(srcObject, liveTime, maxIdleTime);
    }

    /**
     * Value.
     *
     * @return T
     */
    public T value() {
        return srcObject;
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        // should not set the srcobject to null because it may be retrieved by
        // other thread and evicted out pool later.
        // srcObject = null;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return N.hashCode(srcObject.hashCode());
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PoolableWrapper && N.equals(((PoolableWrapper<?>) obj).srcObject, srcObject));
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "{srcObject=" + srcObject + "; activityPrint=" + activityPrint + "}";
    }
}
