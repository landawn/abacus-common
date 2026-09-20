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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
 *         if (!hasNext()) {
 *             throw new java.util.NoSuchElementException();
 *         }
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
     * This operation is not supported. Attempting to call this method will always throw
     * {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException always
     * @deprecated this iterator does not support element removal
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
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "a", "c");
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
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
     * LinkedList<String> list = iter.toCollection(LinkedList::new);
     * System.out.println(list);   // [a, b, c]
     * }</pre>
     *
     * @param <C> the type of the collection to create
     * @param supplier a {@link Supplier} that provides a new empty collection instance
     * @return a collection containing all remaining elements from this iterator
     * @throws IllegalArgumentException if {@code supplier} is null or returns null, or a remaining element violates a destination restriction
     * @throws NullPointerException if a remaining element is null and the destination rejects null elements
     * @throws ClassCastException if a remaining element is incompatible with the destination's type or comparison requirements
     * @throws UnsupportedOperationException if an element remains and the target collection does not support adding it
     * @throws RuntimeException if invoking {@code supplier} fails
     */
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier)
            throws IllegalArgumentException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        N.checkArgNotNull(supplier, cs.supplier);

        return drainTo(N.checkArgNotNull(supplier.get(), "supplier.get()"));
    }

    /**
     * Drains the remaining elements into {@code c} and returns it. Private on purpose: the methods that
     * publish an <i>owning</i> immutable result must not route through the overridable
     * {@link #toCollection(Supplier)} or {@link #toSet()}, because a subclass may return storage it retains.
     *
     * @param <C> the type of the collection
     * @param c the collection to drain into
     * @return {@code c}
     * @throws IllegalArgumentException if a remaining element violates a destination restriction
     * @throws NullPointerException if a remaining element is null and the destination rejects null elements
     * @throws ClassCastException if a remaining element is incompatible with the destination's type or comparison requirements
     * @throws UnsupportedOperationException if an element remains and the target collection does not support adding it
     */
    private <C extends Collection<T>> C drainTo(final C c)
            throws IllegalArgumentException, NullPointerException, ClassCastException, UnsupportedOperationException {
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
     * ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
     * ImmutableList<Integer> list = iter.toImmutableList();
     * System.out.println(list);   // [1, 2, 3]
     * }</pre>
     *
     * @return an {@link ImmutableList} containing all remaining elements from this iterator
     */
    public ImmutableList<T> toImmutableList() {
        // The list is created here and nothing else can reach it, so ownership transfers to the result;
        // wrap() would mark it a live view and force ImmutableList.copyOf(...) to copy it again. It is
        // filled directly rather than through toCollection(...): that method is public and non-final on this
        // class, so a subclass returning storage it retains would make ownsBacking=true a lie. ArrayList is
        // what Suppliers.ofList() builds, so nothing else about the result changes.
        final List<T> list = drainTo(new ArrayList<>());

        return ImmutableList.create(list, false, true);
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
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "a", "c");
     * ImmutableSet<String> set = iter.toImmutableSet();
     * System.out.println(set.size());   // 3 (duplicates removed)
     * }</pre>
     *
     * @return an {@link ImmutableSet} containing all remaining unique elements from this iterator
     */
    public ImmutableSet<T> toImmutableSet() {
        // See toImmutableList(): the set is created here, so it is private to this call and the result owns
        // it. N.newHashSet() is what Suppliers.ofSet() - and therefore toSet() - builds, so the element
        // order is unchanged.
        final Set<T> set = drainTo(N.<T> newHashSet());

        return new ImmutableSet<>(set, false, true);
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
     * ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
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
