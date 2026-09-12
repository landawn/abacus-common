/*
 * Copyright (c) 2022, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * An immutable implementation of {@link ListIterator} that provides read-only iteration
 * over list elements in both forward and backward directions.
 *
 * <p>This class extends {@link ObjIterator} and implements {@link ListIterator}; its own implementations
 * of {@link #set(Object)}, {@link #add(Object)} and the inherited {@link #remove()} throw
 * {@link UnsupportedOperationException}. Because the class is extensible these methods are not
 * {@code final}, so a subclass can re-enable them; code that needs a guaranteed read-only list iterator
 * must pass an untrusted instance through {@link #of(ListIterator)}, which re-wraps it.
 *
 * <p>ImmutableListIterator is particularly useful when you need to provide iteration
 * capabilities over a list while ensuring the underlying data cannot be modified
 * through the iterator.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * List<String> list = Arrays.asList("one", "two", "three");
 * ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());
 *
 * while (iter.hasNext()) {
 *     System.out.println(iter.next());
 * }
 *
 * // Bidirectional iteration
 * while (iter.hasPrevious()) {
 *     System.out.println(iter.previous());
 * }
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @see ListIterator
 * @see ObjIterator
 */
@SuppressWarnings("java:S6548")
public abstract class ImmutableListIterator<T> extends ObjIterator<T> implements ListIterator<T> {

    /**
     * Constructs a new {@code ImmutableListIterator}.
     * This constructor is protected to allow subclassing.
     */
    protected ImmutableListIterator() {
    }

    @SuppressWarnings("rawtypes")
    private static final ImmutableListIterator EMPTY = new ImmutableListIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         * @throws NoSuchElementException if no next element remains in this iterator
         */
        @Override
        public Object next() throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        /**
         * {@inheritDoc}
         * @throws NoSuchElementException if no previous element remains in this iterator
         */
        @Override
        public Object previous() throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };

    /**
     * Returns an empty ImmutableListIterator. This iterator has no elements,
     * so {@link #hasNext()} and {@link #hasPrevious()} always return {@code false}.
     *
     * <p>The returned iterator is a singleton instance and can be safely shared.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableListIterator<String> empty = ImmutableListIterator.empty();
     * System.out.println(empty.hasNext());         // prints false
     * System.out.println(empty.nextIndex());       // prints 0
     * System.out.println(empty.previousIndex());   // prints -1
     * }</pre>
     *
     * @param <T> the type of elements (not) returned by this iterator
     * @return a shared singleton empty {@code ImmutableListIterator} instance
     */
    public static <T> ImmutableListIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Creates an ImmutableListIterator that wraps the provided ListIterator.
     * The returned iterator provides read-only access to the elements.
     *
     * <p>If the provided iterator is {@code null} — or is the shared instance returned by
     * {@link #empty()} — an empty ImmutableListIterator is returned. Otherwise, a new read-only
     * wrapper is returned; even an {@code ImmutableListIterator} argument is re-wrapped, so a
     * mutable subclass cannot expose modification operations through the returned iterator.
     *
     * <p>The returned iterator reflects the current state of the provided iterator,
     * including its current position. Modifications to the underlying collection
     * after creating the immutable iterator may lead to undefined behavior.
     *
     * <p>Exhaustion is deliberately <b>not</b> normalised: {@code next()} and {@code previous()} delegate
     * straight to the wrapped list iterator, so whatever it raises at either end reaches the caller unchanged.
     * What that is depends on the backing list - an {@link java.util.ArrayList} list iterator throws a
     * message-less {@link NoSuchElementException}, an {@link java.util.AbstractList} one throws a
     * {@code NoSuchElementException} carrying the internal {@link IndexOutOfBoundsException} as its cause (which
     * is what {@link ImmutableList}'s own views do), and a list iterator left stale by a structural change to the
     * backing list throws {@link java.util.ConcurrentModificationException} instead of reporting exhaustion at
     * all. {@link ObjIterator#of(java.util.Iterator)} and {@link ObjListIterator#of(ListIterator)} behave the
     * same way - none of the wrapping factories normalise exhaustion. Only {@link #empty()} and the iterators
     * these classes build for themselves report {@code InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX}.</p>
     *
     * <p>So one {@link ImmutableList} still reports exhaustion in two different SHAPES, because its two
     * traversals wrap different sources: {@code iterator()} wraps the backing collection's iterator and yields a
     * message-less {@code NoSuchElementException}, while {@code listIterator()} wraps its list iterator and
     * yields one carrying an {@link IndexOutOfBoundsException}. Do not match on the message or cause of either;
     * test {@code hasNext()} / {@code hasPrevious()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3));
     * ListIterator<Integer> mutableIter = numbers.listIterator();
     * ImmutableListIterator<Integer> immutableIter = ImmutableListIterator.of(mutableIter);
     *
     * // Can iterate but not modify
     * while (immutableIter.hasNext()) {
     *     System.out.println(immutableIter.next());
     * }
     * // immutableIter.set(4);   // Would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of elements returned by the iterator
     * @param iter the {@link ListIterator} to wrap; may be {@code null}
     * @return an {@code ImmutableListIterator} wrapping the provided iterator, or
     *         {@link #empty()} if {@code iter} is {@code null}
     * @see #empty()
     */
    public static <T> ImmutableListIterator<T> of(final ListIterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter == EMPTY) {
            return empty();
        }

        return new ImmutableListIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no next element remains in this iterator
             */
            @Override
            public T next() throws NoSuchElementException {
                // No hasNext() guard: exhaustion is reported by the wrapped list iterator, not normalised to
                // ERROR_MSG_FOR_NO_SUCH_EX the way ObjIterator.of / ObjListIterator.of do it. Pinned by
                // ImmutableListIteratorTest; see the note on of(ListIterator) before "fixing" the asymmetry.
                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no previous element remains in this iterator
             */
            @Override
            public T previous() throws NoSuchElementException {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }
        };
    }

    /**
     * This operation is not supported by {@code ImmutableListIterator}.
     * Attempting to call this method will always throw {@link UnsupportedOperationException}.
     *
     * <p>Use a mutable {@link ListIterator} if you need to modify elements during iteration.
     *
     * @param e the element with which to replace the last element returned by {@link #next()} or {@link #previous()}
     * @throws UnsupportedOperationException always, as this is an immutable iterator
     * @deprecated {@code ImmutableListIterator} does not support modification operations
     */
    @Deprecated
    @Override
    public void set(final T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by {@code ImmutableListIterator}.
     * Attempting to call this method will always throw {@link UnsupportedOperationException}.
     *
     * <p>Use a mutable {@link ListIterator} if you need to add elements during iteration.
     *
     * @param e the element to insert
     * @throws UnsupportedOperationException always, as this is an immutable iterator
     * @deprecated {@code ImmutableListIterator} does not support modification operations
     */
    @Deprecated
    @Override
    public void add(final T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
