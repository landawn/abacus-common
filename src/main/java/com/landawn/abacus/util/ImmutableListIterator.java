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
 * <p>This class extends {@link ObjIterator} and implements {@link ListIterator}, but
 * all modification operations ({@link #set(Object)}, {@link #add(Object)}, and inherited
 * {@link #remove()}) will throw {@link UnsupportedOperationException}.
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
@SuppressWarnings({ "java:S6548" })
public abstract class ImmutableListIterator<T> extends ObjIterator<T> implements ListIterator<T> {

    /**
     * Constructs a new ImmutableListIterator.
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

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
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
     * System.out.println(empty.hasNext());         // prints: false
     * System.out.println(empty.nextIndex());       // prints: 0
     * System.out.println(empty.previousIndex());   // prints: -1
     * }</pre>
     *
     * @param <T> the type of elements (not) returned by this iterator
     * @return an empty ImmutableListIterator instance
     */
    public static <T> ImmutableListIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Creates an ImmutableListIterator that wraps the provided ListIterator.
     * The returned iterator provides read-only access to the elements.
     * 
     * <p>If the provided iterator is {@code null}, an empty ImmutableListIterator is returned.
     * If the provided iterator is already an ImmutableListIterator, it is returned as-is.
     * 
     * <p>The returned iterator reflects the current state of the provided iterator,
     * including its current position. Modifications to the underlying collection
     * after creating the immutable iterator may lead to undefined behavior.
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
     * // immutableIter.set(4);  // Would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of elements returned by the iterator
     * @param iter the ListIterator to wrap, may be null
     * @return an ImmutableListIterator wrapping the provided iterator, or empty if iter is null
     */
    public static <T> ImmutableListIterator<T> of(final ListIterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ImmutableListIterator) {
            return (ImmutableListIterator<T>) iter;
        }

        return new ImmutableListIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public T previous() {
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
     * This operation is not supported by ImmutableListIterator.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     * 
     * <p>Use a mutable ListIterator if you need to modify elements during iteration.
     *
     * @param e the element with which to replace the last element returned by next or previous
     * @throws UnsupportedOperationException always, as this is an immutable iterator
     * @deprecated ImmutableListIterator does not support modification operations
     */
    @Deprecated
    @Override
    public void set(final T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableListIterator.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     * 
     * <p>Use a mutable ListIterator if you need to add elements during iteration.
     *
     * @param e the element to insert
     * @throws UnsupportedOperationException always, as this is an immutable iterator
     * @deprecated ImmutableListIterator does not support modification operations
     */
    @Deprecated
    @Override
    public void add(final T e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
