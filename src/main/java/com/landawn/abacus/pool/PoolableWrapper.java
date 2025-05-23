/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.pool;

import com.landawn.abacus.util.Immutable;
import com.landawn.abacus.util.N;

/**
 *
 * @param <T>
 */
@com.landawn.abacus.annotation.Immutable
public final class PoolableWrapper<T> extends AbstractPoolable implements Immutable {

    private final T srcObject;

    /**
     * Wrap the source object with {@code Long.MAX_VALUE} {@code liveTime} and {@code Long.MAX_VALUE} {@code maxIdleTime}.
     *
     * @param srcObject
     */
    public PoolableWrapper(final T srcObject) {
        this(srcObject, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     *
     * @param srcObject
     * @param liveTime
     * @param maxIdleTime
     */
    public PoolableWrapper(final T srcObject, final long liveTime, final long maxIdleTime) {
        super(liveTime, maxIdleTime);
        this.srcObject = srcObject;
    }

    /**
     * Wrap the source object with {@code Long.MAX_VALUE} {@code liveTime} and {@code Long.MAX_VALUE} {@code maxIdleTime}.
     *
     * @param <T>
     * @param srcObject
     * @return
     */
    public static <T> PoolableWrapper<T> of(final T srcObject) {
        return new PoolableWrapper<>(srcObject);
    }

    /**
     * Wrap the source object with specified {@code liveTime} and {@code maxIdleTime}.
     *
     * @param <T>
     * @param srcObject
     * @param liveTime
     * @param maxIdleTime
     * @return
     */
    public static <T> PoolableWrapper<T> of(final T srcObject, final long liveTime, final long maxIdleTime) {
        return new PoolableWrapper<>(srcObject, liveTime, maxIdleTime);
    }

    /**
     *
     * @return T
     */
    public T value() {
        return srcObject;
    }

    /**
     * @param caller
     */
    @Override
    public void destroy(final Caller caller) {
        // should not set the srcObject to null because it may be retrieved by
        // other thread and evicted out pool later.
        // srcObject = null;
    }

    @Override
    public int hashCode() {
        return N.hashCode(srcObject.hashCode());
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof PoolableWrapper && N.equals(((PoolableWrapper<?>) obj).srcObject, srcObject));
    }

    @Override
    public String toString() {
        return "{srcObject=" + srcObject + "; activityPrint=" + activityPrint + "}";
    }
}
