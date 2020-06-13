/*
 * Copyright (c) 2020, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Queue;

import com.landawn.abacus.util.function.Supplier;

public class LazyQueue<T> extends LazyCollection<T> implements Queue<T> {
    private Queue<T> queue;

    LazyQueue(final Supplier<? extends Queue<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            queue = (Queue<T>) coll;
        }
    }

    public static <T> LazyQueue<T> of(final QueueSupplier<T> supplier) {
        return new LazyQueue<T>(supplier);
    }

    /**
     * 
     * @param <T>
     * @param supplier
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <T> LazyCollection<T> of(final Supplier<? extends Collection<T>> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T e) {
        if (isInitialized == false) {
            init();
        }

        return queue.offer(e);
    }

    @Override
    public T remove() {
        if (isInitialized == false) {
            init();
        }

        return queue.remove();
    }

    @Override
    public T poll() {
        if (isInitialized == false) {
            init();
        }

        return queue.poll();
    }

    @Override
    public T element() {
        if (isInitialized == false) {
            init();
        }

        return queue.element();
    }

    @Override
    public T peek() {
        if (isInitialized == false) {
            init();
        }

        return queue.peek();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return queue.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return queue.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return queue.toString();
    }

    public static interface QueueSupplier<T> extends Supplier<Queue<T>> {

        @Override
        Queue<T> get();

        public static <T> QueueSupplier<T> of(QueueSupplier<T> supplier) {
            return supplier;
        }
    }
}
