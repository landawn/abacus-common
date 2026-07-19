/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;

/**
 * An abstract base class for immutable iterators that do not support element removal.
 * This class implements the Iterator interface but its base implementation throws
 * {@link UnsupportedOperationException} for {@link #remove()}.
 *
 * <p>ImmutableIterator provides additional utility methods for converting the remaining
 * elements to various collection types, including immutable collections. It serves as
 * the base class for iterators returned by immutable collection implementations.</p>
 *
 * <p>Iterators are stateful cursors, not immutable values: traversal consumes their remaining
 * elements. Subclasses must implement {@link #hasNext()} and {@link #next()}. Because this class is
 * extensible, a subclass can override {@code remove()}; APIs requiring a guaranteed read-only
 * iterator must wrap untrusted iterator implementations rather than returning them directly.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableIterator<String> iter = new ImmutableIterator<String>() {
 *     private int index = 0;
 *     private String[] data = {"a", "b", "c"};
 *
 *     public boolean hasNext() {
 *         return index < data.length;
 *     }
 *
 *     public String next() {
 *         return data[index++];
 *     }
 * };
 *
 * ImmutableList<String> list = iter.toImmutableList();
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @see java.util.Iterator
 */
abstract class ImmutableIterator<T> implements java.util.Iterator<T> {

    /**
     * This operation is not supported by {@code ImmutableIterator}.
     * Attempting to call this method will always throw {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException always
     * @deprecated {@code ImmutableIterator} does not support element removal
     */
    @Deprecated
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Collects all remaining elements from this iterator into a Set.
     * The returned set will contain all elements from the current position
     * to the end of the iteration, with duplicates removed.
     *
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and {@link #hasNext()} will return {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "a", "c").iterator();
     * Set<String> set = iter.toSet();
     * System.out.println(set);   // [a, b, c] (order may vary)
     * }</pre>
     *
     * @return a {@link Set} containing all remaining elements from this iterator, with duplicates removed
     */
    public Set<T> toSet() {
        return toCollection(Suppliers.ofSet());
    }

    /**
     * Collects all remaining elements from this iterator into a collection
     * created by the provided supplier.
     *
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and {@link #hasNext()} will return {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "c").iterator();
     * LinkedList<String> list = iter.toCollection(LinkedList::new);
     * System.out.println(list);   // [a, b, c]
     * }</pre>
     *
     * @param <C> the type of the collection to create
     * @param supplier a {@link Supplier} that provides a new empty collection instance; must not be {@code null}
     * @return a collection containing all remaining elements from this iterator
     * @throws NullPointerException if {@code supplier} is {@code null} or if {@code supplier.get()} returns {@code null}
     */
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) {
        Objects.requireNonNull(supplier, "supplier");

        final C c = Objects.requireNonNull(supplier.get(), "supplier.get()");

        while (hasNext()) {
            c.add(next());
        }

        return c;
    }

    /**
     * Collects all remaining elements from this iterator into an ImmutableList.
     *
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and {@link #hasNext()} will return {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableIterator<Integer> iter = ImmutableSet.of(1, 2, 3).iterator();
     * ImmutableList<Integer> list = iter.toImmutableList();
     * System.out.println(list);   // [1, 2, 3] (order depends on source)
     * }</pre>
     *
     * @return an {@link ImmutableList} containing all remaining elements from this iterator
     */
    public ImmutableList<T> toImmutableList() {
        return ImmutableList.wrap(toCollection(Suppliers.ofList()));
    }

    /**
     * Collects all remaining elements from this iterator into an ImmutableSet.
     * Duplicate elements are removed according to their {@link Object#equals(Object)} method.
     *
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and {@link #hasNext()} will return {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "a", "c").iterator();
     * ImmutableSet<String> set = iter.toImmutableSet();
     * System.out.println(set.size());   // 3 (duplicates removed)
     * }</pre>
     *
     * @return an {@link ImmutableSet} containing all remaining unique elements from this iterator
     */
    public ImmutableSet<T> toImmutableSet() {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Returns the number of remaining elements in this iterator.
     * This method consumes all remaining elements to count them.
     *
     * <p><b>Warning:</b> This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and {@link #hasNext()} will return {@code false}. If you need
     * both the count and the elements, consider collecting to a list first.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "c").iterator();
     * iter.next();   // element is skipped (the first one)
     * long remaining = iter.count();
     * System.out.println(remaining);        // 2
     * System.out.println(iter.hasNext());   // false (iterator exhausted)
     * }</pre>
     *
     * @return the number of remaining elements; {@code 0} if the iterator is already exhausted
     */
    @Beta
    public long count() {
        long count = 0;

        while (hasNext()) {
            next();
            count++;
        }

        return count;
    }

}
