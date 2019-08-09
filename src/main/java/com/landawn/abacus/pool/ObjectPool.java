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

import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Interface ObjectPool.
 *
 * @author Haiyang Li
 * @param <E> the element type
 * @since 0.8
 */
public interface ObjectPool<E extends Poolable> extends Pool {

    /**
     * Adds the.
     *
     * @param e the e
     * @return true, if successful
     */
    boolean add(E e);

    /**
     * Adds the.
     *
     * @param e the e
     * @param autoDestroyOnFailedToAdd the auto destroy on failed to add
     * @return true, if successful
     */
    boolean add(E e, boolean autoDestroyOnFailedToAdd);

    /**
     * Adds the.
     *
     * @param e the e
     * @param timeout the timeout
     * @param unit the unit
     * @return true, if successful
     * @throws InterruptedException the interrupted exception
     */
    boolean add(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Adds the.
     *
     * @param e the e
     * @param timeout the timeout
     * @param unit the unit
     * @param autoDestroyOnFailedToAdd the auto destroy on failed to add
     * @return true, if successful
     * @throws InterruptedException the interrupted exception
     */
    boolean add(E e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, or returns <tt>null</tt> if this queue is empty.
     * 
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */
    E take();

    /**
     * Take.
     *
     * @param timeout the timeout
     * @param unit the unit
     * @return the e
     * @throws InterruptedException the interrupted exception
     */
    E take(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Method contains.
     *
     * @param e the e
     * @return boolean
     */
    boolean contains(E e);

    /**
     * The Interface MemoryMeasure.
     *
     * @param <E> the element type
     */
    public static interface MemoryMeasure<E> {

        /**
         * Size of.
         *
         * @param e the e
         * @return the long
         */
        long sizeOf(E e);
    }
}
