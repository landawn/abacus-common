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
 * @param <E>
 */
public interface ObjectPool<E extends Poolable> extends Pool {

    /**
     * Adds a new element to the pool.
     *
     * @param e The element to be added to the pool.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean add(E e);

    /**
     * Adds a new element to the pool. If the addition fails and autoDestroyOnFailedToAdd is {@code true}, the element will be destroyed.
     *
     * @param e The element to be added to the pool.
     * @param autoDestroyOnFailedToAdd If {@code true}, the element will be destroyed if it cannot be added to the pool.
     * @return {@code true} if the element was successfully added, {@code false} otherwise.
     */
    boolean add(E e, boolean autoDestroyOnFailedToAdd);

    /**
     * Attempts to add a new element to the pool, waiting up to the specified wait time if necessary for space to become available.
     *
     * @param e The element to be added to the pool.
     * @param timeout The maximum time to wait for space to become available.
     * @param unit The time unit of the timeout argument.
     * @return {@code true} if the element was successfully added, {@code false} if the specified waiting time elapses before space is available.
     * @throws InterruptedException if interrupted while waiting.
     */
    boolean add(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Attempts to add a new element to the pool, waiting up to the specified wait time if necessary for space to become available.
     * If the addition fails and autoDestroyOnFailedToAdd is {@code true}, the element will be destroyed.
     *
     * @param e The element to be added to the pool.
     * @param timeout The maximum time to wait for space to become available.
     * @param unit The time unit of the timeout argument.
     * @param autoDestroyOnFailedToAdd If {@code true}, the element will be destroyed if it cannot be added to the pool.
     * @return {@code true} if the element was successfully added, {@code false} if the specified waiting time elapses before space is available.
     * @throws InterruptedException if interrupted while waiting.
     */
    boolean add(E e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
     *
     * @return The head of the queue or {@code null} if the queue is empty.
     */
    E take();

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary.
     * Returns {@code null} if this queue is empty after waiting for the specified time.
     *
     * @param timeout The maximum time to wait for an element to become available.
     * @param unit The time unit of the timeout argument.
     * @return The head of the queue or {@code null} if the queue is empty after waiting for the specified time.
     * @throws InterruptedException if interrupted while waiting.
     */
    E take(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Checks if the specified element is present in the pool.
     *
     * @param valueToFind The element whose presence in the pool is to be tested.
     * @return {@code true} if the pool contains the specified element, {@code false} otherwise.
     */
    boolean contains(E valueToFind);

    /**
     * The Interface MemoryMeasure.
     *
     * @param <E>
     */
    interface MemoryMeasure<E> {

        /**
         *
         * @param e
         * @return
         */
        long sizeOf(E e);
    }
}
