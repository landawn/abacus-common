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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ObjectPool<E extends Poolable> extends Pool {
    /**
     * 
     * @param e
     * @return
     */
    boolean add(E e);

    /**
     * 
     * @param e
     * @param autoDestroyOnFailedToAdd
     * @return
     */
    boolean add(E e, boolean autoDestroyOnFailedToAdd);

    /**
     * 
     * @param e
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    boolean add(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 
     * @param e
     * @param timeout
     * @param unit
     * @param autoDestroyOnFailedToAdd
     * @return
     * @throws InterruptedException
     */
    boolean add(E e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, or returns <tt>null</tt> if this queue is empty.
     * 
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */
    E take();

    /**
     * 
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    E take(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Method contains
     * 
     * @param e
     * @return boolean
     */
    boolean contains(E e);

    public static interface MemoryMeasure<E> {
        long sizeOf(E e);
    }
}
