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

public interface Poolable {

    /**
     *
     * @return ActivityPrint
     */
    ActivityPrint activityPrint();

    /**
     *
     * @param caller
     * @Ssee Caller
     */
    void destroy(Caller caller);

    /**
     * Wrap the source object with {@code Long.MAX_VALUE} {@code liveTime} and {@code Long.MAX_VALUE} {@code maxIdleTime}.
     *
     * @param <T>
     * @param srcObject
     * @return
     */
    static <T> PoolableWrapper<T> wrap(final T srcObject) {
        return PoolableWrapper.of(srcObject);
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
    static <T> PoolableWrapper<T> wrap(final T srcObject, final long liveTime, final long maxIdleTime) {
        return PoolableWrapper.of(srcObject, liveTime, maxIdleTime);
    }

    enum Caller {
        CLOSE(0), EVICT(1), VACATE(2), REMOVE_REPLACE_CLEAR(3), PUT_ADD_FAILURE(4), OTHER_OUTER(5);

        private final int value;

        Caller(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }
}
