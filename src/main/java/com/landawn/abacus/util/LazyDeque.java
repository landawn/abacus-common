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

import java.util.Deque;
import java.util.Iterator;

import com.landawn.abacus.util.function.Supplier;

public final class LazyDeque<T> extends LazyQueue<T> implements Deque<T> {
    private Deque<T> deque;

    LazyDeque(final Supplier<? extends Deque<T>> supplier) {
        super(supplier);
    }

    @Override
    protected void init() {
        if (isInitialized == false) {
            super.init();
            deque = (Deque<T>) coll;
        }
    }

    /**
     * 
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T> LazyDeque<T> of(final DequeSupplier<T> supplier) {
        return new LazyDeque<T>(supplier);
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
    public static <T> LazyQueue<T> of(final QueueSupplier<T> supplier) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(T e) {
        if (isInitialized == false) {
            init();
        }

        deque.addFirst(e);
    }

    @Override
    public void addLast(T e) {
        if (isInitialized == false) {
            init();
        }

        deque.addLast(e);
    }

    @Override
    public boolean offerFirst(T e) {
        if (isInitialized == false) {
            init();
        }

        return deque.offerFirst(e);
    }

    @Override
    public boolean offerLast(T e) {
        if (isInitialized == false) {
            init();
        }

        return deque.offerLast(e);
    }

    @Override
    public T removeFirst() {
        if (isInitialized == false) {
            init();
        }

        return deque.removeFirst();
    }

    @Override
    public T removeLast() {
        if (isInitialized == false) {
            init();
        }

        return deque.removeLast();
    }

    @Override
    public T pollFirst() {
        if (isInitialized == false) {
            init();
        }

        return deque.pollFirst();
    }

    @Override
    public T pollLast() {
        if (isInitialized == false) {
            init();
        }

        return deque.pollLast();
    }

    @Override
    public T getFirst() {
        if (isInitialized == false) {
            init();
        }

        return deque.getFirst();
    }

    @Override
    public T getLast() {
        if (isInitialized == false) {
            init();
        }

        return deque.getLast();
    }

    @Override
    public T peekFirst() {
        if (isInitialized == false) {
            init();
        }

        return deque.peekFirst();
    }

    @Override
    public T peekLast() {
        if (isInitialized == false) {
            init();
        }

        return deque.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (isInitialized == false) {
            init();
        }

        return deque.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (isInitialized == false) {
            init();
        }

        return deque.removeLastOccurrence(o);
    }

    @Override
    public void push(T e) {
        if (isInitialized == false) {
            init();
        }

        deque.push(e);
    }

    @Override
    public T pop() {
        if (isInitialized == false) {
            init();
        }

        return deque.pop();
    }

    @Override
    public Iterator<T> descendingIterator() {
        if (isInitialized == false) {
            init();
        }

        return deque.descendingIterator();
    }

    @Override
    public int hashCode() {
        if (isInitialized == false) {
            init();
        }

        return deque.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (isInitialized == false) {
            init();
        }

        return deque.equals(obj);
    }

    @Override
    public String toString() {
        if (isInitialized == false) {
            init();
        }

        return deque.toString();
    }

    public static interface DequeSupplier<T> extends Supplier<Deque<T>> {

        @Override
        Deque<T> get();

        public static <T> DequeSupplier<T> of(DequeSupplier<T> supplier) {
            return supplier;
        }
    }
}
