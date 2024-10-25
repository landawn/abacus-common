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

import java.io.Serializable;

public interface Pool extends Serializable {

    /**
     * Lock.
     */
    void lock();

    /**
     * Unlock.
     */
    void unlock();

    /**
     * Gets the capacity.
     *
     * @return
     */
    int getCapacity();

    int size();

    /**
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    boolean isEmpty();

    /**
     * Vacate.
     */
    void vacate();

    /**
     * Clear.
     */
    void clear();

    /**
     * Close.
     */
    void close();

    /**
     * Checks if is closed.
     *
     * @return {@code true}, if is closed
     */
    boolean isClosed();

    long putCount();

    long hitCount();

    long missCount();

    /**
     *  Returns the number of values that have been evicted.
     *
     * @return
     */
    long evictionCount();
}
