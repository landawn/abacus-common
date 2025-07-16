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
import java.util.Set;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;

/**
 * An abstract base class for immutable iterators that do not support element removal.
 * This class implements the Iterator interface but throws UnsupportedOperationException
 * for the remove() operation, ensuring true immutability.
 * 
 * <p>ImmutableIterator provides additional utility methods for converting the remaining
 * elements to various collection types, including immutable collections. It serves as
 * the base class for iterators returned by immutable collection implementations.</p>
 * 
 * <p>Subclasses must implement the hasNext() and next() methods to provide iteration
 * functionality. The remove() method is final and always throws an exception.</p>
 * 
 * <p>Example usage:
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
 * </p>
 *
 * @param <T> the type of elements returned by this iterator
 * @see java.util.Iterator
 * @see Immutable
 */
@com.landawn.abacus.annotation.Immutable
abstract class ImmutableIterator<T> implements java.util.Iterator<T>, Immutable {

    /**
     * This operation is not supported by ImmutableIterator.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableIterator does not support element removal
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
     * the iterator will be exhausted and hasNext() will return false.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "a", "c").iterator();
     * Set<String> set = iter.toSet();
     * System.out.println(set); // [a, b, c] (order may vary)
     * }</pre>
     * </p>
     *
     * @return a Set containing all remaining elements from this iterator
     */
    public Set<T> toSet() {
        return toCollection(Suppliers.ofSet());
    }

    /**
     * Collects all remaining elements from this iterator into a collection
     * created by the provided supplier.
     * 
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and hasNext() will return false.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "c").iterator();
     * LinkedList<String> list = iter.toCollection(LinkedList::new);
     * System.out.println(list); // [a, b, c]
     * }</pre>
     * </p>
     *
     * @param <C> the type of the collection to create
     * @param supplier a supplier that creates a new empty collection instance
     * @return a collection containing all remaining elements from this iterator
     */
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) {
        final C c = supplier.get();

        while (hasNext()) {
            c.add(next());
        }

        return c;
    }

    /**
     * Collects all remaining elements from this iterator into an ImmutableList.
     * 
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and hasNext() will return false.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableIterator<Integer> iter = ImmutableSet.of(1, 2, 3).iterator();
     * ImmutableList<Integer> list = iter.toImmutableList();
     * System.out.println(list); // [1, 2, 3] (order depends on source)
     * }</pre>
     * </p>
     *
     * @return an ImmutableList containing all remaining elements from this iterator
     */
    public ImmutableList<T> toImmutableList() {
        return ImmutableList.wrap(toCollection(Suppliers.ofList()));
    }

    /**
     * Collects all remaining elements from this iterator into an ImmutableSet.
     * Duplicate elements will be removed according to their equals() method.
     * 
     * <p>This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and hasNext() will return false.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "a", "c").iterator();
     * ImmutableSet<String> set = iter.toImmutableSet();
     * System.out.println(set.size()); // 3 (duplicates removed)
     * }</pre>
     * </p>
     *
     * @return an ImmutableSet containing all remaining unique elements from this iterator
     */
    public ImmutableSet<T> toImmutableSet() {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Returns the number of remaining elements in this iterator.
     * This method consumes all remaining elements to count them.
     * 
     * <p><b>Warning:</b> This method consumes the iterator. After calling this method,
     * the iterator will be exhausted and hasNext() will return false. If you need
     * both the count and the elements, consider collecting to a list first.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableIterator<String> iter = ImmutableList.of("a", "b", "c").iterator();
     * iter.next(); // Skip first element
     * long remaining = iter.count();
     * System.out.println(remaining); // 2
     * System.out.println(iter.hasNext()); // false (iterator exhausted)
     * }</pre>
     * </p>
     *
     * @return the number of remaining elements
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

    // Not efficient for primitive iterators because function is not for primitives.
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param keyExtractor
    //     * @return
    //     * @throws E the e
    //     */
    //    public <K, E extends Exception> Map<K, T> toMap(final Try.Function<? super T, K, E> keyExtractor) throws E {
    //        return Iterators.toMap(this, keyExtractor);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor) throws E, E2 {
    //        return Iterators.toMap(this, keyExtractor, valueExtractor);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor, final Supplier<? extends M> mapSupplier) throws E, E2 {
    //        return Iterators.toMap(this, keyExtractor, valueExtractor, mapSupplier);
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param keyExtractor
    //     * @return
    //     * @throws E the e
    //     */
    //    public <K, E extends Exception> ImmutableMap<K, T> toImmutableMap(final Try.Function<? super T, K, E> keyExtractor) throws E {
    //        return ImmutableMap.wrap(toMap(keyExtractor));
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, E extends Exception, E2 extends Exception> ImmutableMap<K, V> toImmutableMap(final Try.Function<? super T, K, E> keyExtractor,
    //            final Try.Function<? super T, ? extends V, E2> valueExtractor) throws E, E2 {
    //        return ImmutableMap.wrap(toMap(keyExtractor, valueExtractor));
    //    }
    //
    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @param <M>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyExtractor
    //     * @param valueExtractor
    //     * @param mapSupplier
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> ImmutableMap<K, V> toImmutableMap(
    //            final Try.Function<? super T, K, E> keyExtractor, final Try.Function<? super T, ? extends V, E2> valueExtractor,
    //            final Supplier<? extends M> mapSupplier) throws E, E2 {
    //        return ImmutableMap.wrap(toMap(keyExtractor, valueExtractor, mapSupplier));
    //    }
}