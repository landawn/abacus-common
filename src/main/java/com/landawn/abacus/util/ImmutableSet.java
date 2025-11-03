/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.landawn.abacus.annotation.Beta;

/**
 * An immutable, thread-safe implementation of the {@link Set} interface.
 * Once created, the contents of an ImmutableSet cannot be modified.
 * All mutating operations (add, remove, clear, etc.) will throw {@link UnsupportedOperationException}.
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 *   <li>{@link #empty()} - returns an empty set</li>
 *   <li>{@link #of(Object[])} - creates sets with specific elements</li>
 *   <li>{@link #copyOf(Collection)} - creates a defensive copy from another collection</li>
 *   <li>{@link #wrap(Set)} - wraps an existing set (changes to the underlying set will be reflected)</li>
 *   <li>{@link #builder()} - provides a builder for constructing sets incrementally</li>
 * </ul>
 * 
 * <p>The implementation maintains the iteration order when created from a List, LinkedHashSet,
 * or SortedSet, otherwise no specific iteration order is guaranteed.
 * 
 * <p>Null elements are supported if the underlying set implementation supports them.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create using factory methods
 * ImmutableSet<String> colors = ImmutableSet.of("red", "green", "blue");
 * 
 * // Create from existing collection
 * Set<Integer> mutable = new HashSet<>(Arrays.asList(1, 2, 3));
 * ImmutableSet<Integer> numbers = ImmutableSet.copyOf(mutable);
 * 
 * // Create using builder
 * ImmutableSet<String> names = ImmutableSet.<String>builder()
 *     .add("Alice")
 *     .add("Bob", "Charlie")
 *     .addAll(otherNames)
 *     .build();
 * }</pre>
 *
 * @param <E> the type of elements maintained by this set
 * @see Set
 * @see ImmutableCollection
 */
public class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSet EMPTY = new ImmutableSet(N.emptySet(), false);

    /**
     * Constructs an ImmutableSet instance with the provided set.
     * This constructor determines if the set is already unmodifiable.
     *
     * @param set the set of elements to be included in the ImmutableSet
     */
    ImmutableSet(final Set<? extends E> set) {
        this(set, ClassUtil.isPossibleImmutable(set.getClass()));
    }

    /**
     * Constructs an ImmutableSet instance with the provided set and unmodifiable flag.
     * If the set is not already unmodifiable, it will be wrapped in an unmodifiable view.
     *
     * @param set the set of elements to be included in the ImmutableSet
     * @param isUnmodifiable a boolean value indicating if the set is already unmodifiable
     */
    ImmutableSet(final Set<? extends E> set, final boolean isUnmodifiable) {
        super(isUnmodifiable ? set : Collections.unmodifiableSet(set));
    }

    /**
     * Returns an empty ImmutableSet. This method always returns the same cached instance,
     * making it memory efficient for representing empty sets.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<String> empty = ImmutableSet.empty();
     * System.out.println(empty.size()); // prints: 0
     * System.out.println(empty.isEmpty()); // prints: true
     * }</pre>
     *
     * @param <E> the type of elements in the set
     * @return an empty ImmutableSet instance
     */
    public static <E> ImmutableSet<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableSet containing a single element.
     * This is a convenience method equivalent to {@link #of(Object)}.
     * The returned set is immutable and will have a size of 1.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<String> single = ImmutableSet.just("hello");
     * System.out.println(single.contains("hello")); // prints: true
     * System.out.println(single.size()); // prints: 1
     * }</pre>
     *
     * @param <E> the type of the element
     * @param e the single element to be contained in the ImmutableSet
     * @return an ImmutableSet containing only the specified element
     */
    public static <E> ImmutableSet<E> just(final E e) {
        return new ImmutableSet<>(N.asLinkedHashSet(e), false);
    }

    /**
     * Returns an ImmutableSet containing a single element.
     * The returned set is immutable and will have a size of 1.
     * The iteration order is guaranteed for this single element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<Integer> single = ImmutableSet.of(42);
     * // single.add(43); // Would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <E> the type of the element
     * @param e1 the single element to be contained in the ImmutableSet
     * @return an ImmutableSet containing only the specified element
     */
    public static <E> ImmutableSet<E> of(final E e1) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1), false);
    }

    /**
     * Returns an ImmutableSet containing up to two distinct elements.
     * If the two elements are equal according to their equals() method, the resulting set
     * will contain only one element. The iteration order is guaranteed to match the order
     * of the first occurrence of each distinct element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<String> pair = ImmutableSet.of("first", "second");
     * System.out.println(pair.size()); // prints: 2
     *
     * ImmutableSet<String> duplicate = ImmutableSet.of("same", "same");
     * System.out.println(duplicate.size()); // prints: 1
     * }</pre>
     *
     * @param <E> the type of elements
     * @param e1 the first element
     * @param e2 the second element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2), false);
    }

    /**
     * Returns an ImmutableSet containing up to three distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3), false);
    }

    /**
     * Returns an ImmutableSet containing up to four distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4), false);
    }

    /**
     * Returns an ImmutableSet containing up to five distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5), false);
    }

    /**
     * Returns an ImmutableSet containing up to six distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6), false);
    }

    /**
     * Returns an ImmutableSet containing up to seven distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7), false);
    }

    /**
     * Returns an ImmutableSet containing up to eight distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8), false);
    }

    /**
     * Returns an ImmutableSet containing up to nine distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     *
     * @param <E> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8, e9), false);
    }

    /**
     * Returns an ImmutableSet containing up to ten distinct elements.
     * Duplicate elements (as determined by equals()) are included only once in the set.
     * The iteration order is guaranteed to match the order of the first occurrence of each distinct element.
     * Unlike some set implementations, this method supports {@code null} elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @param e10 the tenth element
     * @return an ImmutableSet containing the specified distinct elements
     */
    public static <E> ImmutableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return new ImmutableSet<>(N.asLinkedHashSet(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), false);
    }

    /**
     * Returns an ImmutableSet containing all distinct elements from the provided array.
     * The returned set is independent of the input array; changes to the array after this call
     * will not affect the returned set. Duplicate elements in the array are included only once.
     * If the array is {@code null} or empty, an empty ImmutableSet is returned.
     * Unlike some collection frameworks, this method supports {@code null} elements in the array.
     * The iteration order is guaranteed to match the order of first occurrence in the array.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] colors = {"red", "green", "blue", "red"};
     * ImmutableSet<String> colorSet = ImmutableSet.of(colors);
     * System.out.println(colorSet.size()); // prints: 3 (duplicates removed)
     * }</pre>
     *
     * @param <E> the type of the elements
     * @param a the array of elements to include in the ImmutableSet, may be {@code null} or empty
     * @return an ImmutableSet containing all distinct elements from the array, or empty set if array is null/empty
     * @see Set#of(Object...)
     */
    @SafeVarargs
    public static <E> ImmutableSet<E> of(final E... a) {
        if (N.isEmpty(a)) {
            return empty();
        } else {
            // return new ImmutableSet<>(Set.of(a), true); // Doesn't support null element
            return new ImmutableSet<>(N.asLinkedHashSet(a), false);
        }
    }

    /**
     * Returns an ImmutableSet containing all distinct elements from the provided collection.
     * If the provided collection is already an ImmutableSet, it is returned directly without copying.
     * If the collection is {@code null} or empty, an empty ImmutableSet is returned.
     * Otherwise, a new ImmutableSet is created with a defensive copy of the collection's distinct elements.
     * The iteration order is preserved if the source collection is a List, LinkedHashSet, or SortedSet.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 2, 1);
     * ImmutableSet<Integer> uniqueNumbers = ImmutableSet.copyOf(numbers);
     * System.out.println(uniqueNumbers); // order preserved from list: [1, 2, 3]
     * 
     * Set<String> mutableSet = new HashSet<>();
     * mutableSet.add("apple");
     * ImmutableSet<String> immutableCopy = ImmutableSet.copyOf(mutableSet);
     * mutableSet.add("banana"); // Does not affect immutableCopy
     * }</pre>
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose distinct elements are to be placed into the ImmutableSet
     * @return an ImmutableSet containing all distinct elements from the collection, or the same instance if already an ImmutableSet
     */
    public static <E> ImmutableSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSet) {
            return (ImmutableSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableSet<>((c instanceof List || c instanceof LinkedHashSet || c instanceof SortedSet) ? N.newLinkedHashSet(c) : N.newHashSet(c),
                    false);
        }
    }

    /**
     * Wraps the provided set into an ImmutableSet without copying the elements.
     * The returned ImmutableSet is backed by the provided set, so changes to the original set
     * will be reflected in the ImmutableSet. However, the ImmutableSet itself cannot be modified.
     * If the provided set is already an ImmutableSet, it is returned directly.
     * If the set is {@code null}, an empty ImmutableSet is returned.
     * 
     * <p><b>Warning:</b> Use this method with caution as the immutability guarantee depends on not modifying
     * the original set after wrapping. This method is marked as @Beta.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> mutableSet = new HashSet<>();
     * mutableSet.add("initial");
     * 
     * ImmutableSet<String> wrapped = ImmutableSet.wrap(mutableSet);
     * mutableSet.add("added later"); // This WILL be visible in wrapped!
     * System.out.println(wrapped.contains("added later")); // prints: true
     * }</pre>
     *
     * @param <E> the type of elements in the set
     * @param set the set to be wrapped into an ImmutableSet
     * @return an ImmutableSet view of the provided set
     */
    @Beta
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) {
        if (set instanceof ImmutableSet) {
            return (ImmutableSet<E>) set;
        } else if (set == null) {
            return empty();
        } else {
            return new ImmutableSet<>(set);
        }
    }

    /**
     * This method is deprecated and will always throw an UnsupportedOperationException.
     * Use {@link #wrap(Set)} or {@link #copyOf(Collection)} instead.
     *
     * @param <E> the type of elements
     * @param c the collection to wrap
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #wrap(Set)} for sets or {@link #copyOf(Collection)} for general collections
     */
    @Deprecated
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new Builder for constructing an ImmutableSet.
     * The builder allows adding elements one by one and then creating an immutable set.
     * This is useful when the number of elements is not known at compile time.
     * The builder uses a HashSet internally, so duplicate elements are automatically removed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<String> names = ImmutableSet.<String>builder()
     *     .add("Alice")
     *     .add("Bob")
     *     .add("Charlie", "David")
     *     .addAll(Arrays.asList("Eve", "Frank"))
     *     .build();
     * }</pre>
     *
     * @param <E> the type of elements to be maintained by the set
     * @return a new Builder instance for creating an ImmutableSet
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new HashSet<>());
    }

    /**
     * Creates a new Builder for constructing an ImmutableSet using the provided set as storage.
     * The builder will add elements to the provided set and then create an immutable view of it.
     * This allows reusing an existing set instance as the backing storage.
     * Note that the provided set should not be modified outside the builder after this call.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Integer> backingSet = new LinkedHashSet<>(); // Preserves insertion order
     * ImmutableSet<Integer> numbers = ImmutableSet.builder(backingSet)
     *     .add(1)
     *     .add(2, 3, 4)
     *     .build();
     * }</pre>
     *
     * @param <E> the type of elements to be maintained by the set
     * @param holder the set to be used as the backing storage for the Builder
     * @return a new Builder instance that will use the provided set
     */
    public static <E> Builder<E> builder(final Set<E> holder) {
        return new Builder<>(holder);
    }

    /**
     * A builder for creating ImmutableSet instances.
     * The builder pattern allows for flexible construction of immutable sets,
     * especially useful when elements are added conditionally or in loops.
     * The builder automatically handles duplicates - each distinct element is included only once.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSet<String> set = ImmutableSet.<String>builder()
     *     .add("one")
     *     .add("two", "three")
     *     .addAll(otherCollection)
     *     .build();
     * }</pre>
     *
     * @param <E> the type of elements in the set being built
     */
    public static final class Builder<E> {
        private final Set<E> set;

        Builder(final Set<E> holder) {
            set = holder;
        }

        /**
         * Adds a single element to the set being built.
         * If the element is already present in the set (as determined by equals()),
         * it is not added again. Null elements are permitted.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Builder<String> builder = ImmutableSet.<String>builder();
         * builder.add("hello")
         *        .add("world")
         *        .add("hello"); // Duplicate, won't be added again
         * }</pre>
         *
         * @param element the element to add, may be null
         * @return this builder instance for method chaining
         */
        public Builder<E> add(final E element) {
            set.add(element);

            return this;
        }

        /**
         * Adds all provided elements to the set being built.
         * Duplicate elements (including duplicates within the array and duplicates of elements
         * already in the set) are included only once. The order of addition may affect the
         * iteration order depending on the underlying set implementation.
         * If the array is {@code null} or empty, no elements are added.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.add("one", "two", "three");
         * }</pre>
         *
         * @param elements the elements to add, may be {@code null} or empty
         * @return this builder instance for method chaining
         */
        @SafeVarargs
        public final Builder<E> add(final E... elements) {
            if (N.notEmpty(elements)) {
                set.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         * Adds all elements from the specified collection to the set being built.
         * Duplicate elements (including duplicates within the collection and duplicates of elements
         * already in the set) are included only once. The order of addition may affect the
         * iteration order depending on the underlying set implementation.
         * If the collection is {@code null} or empty, no elements are added.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> moreElements = Arrays.asList("four", "five");
         * builder.addAll(moreElements);
         * }</pre>
         *
         * @param c the collection containing elements to add, may be {@code null} or empty
         * @return this builder instance for method chaining
         */
        public Builder<E> addAll(final Collection<? extends E> c) {
            if (N.notEmpty(c)) {
                set.addAll(c);
            }

            return this;
        }

        /**
         * Adds all elements from the specified iterator to the set being built.
         * Duplicate elements (including duplicates from the iterator and duplicates of elements
         * already in the set) are included only once. The iterator is consumed by this operation.
         * If the iterator is {@code null} or has no elements, no elements are added.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Iterator<String> iter = someCollection.iterator();
         * builder.addAll(iter);
         * }</pre>
         *
         * @param iter the iterator over elements to add, may be null
         * @return this builder instance for method chaining
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) {
            if (iter != null) {
                while (iter.hasNext()) {
                    set.add(iter.next());
                }
            }

            return this;
        }

        /**
         * Builds and returns an ImmutableSet containing all distinct elements added to this builder.
         * After calling this method, the builder should not be used further as the created
         * ImmutableSet may be backed by the builder's internal storage.
         * 
         * <p>The returned set is immutable and will throw UnsupportedOperationException
         * for any modification attempts. The iteration order depends on the type of set
         * used internally by the builder.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableSet<String> finalSet = builder.build();
         * System.out.println(finalSet.size()); // Number of distinct elements
         * }</pre>
         *
         * @return a new ImmutableSet containing all distinct elements added to the builder
         */
        public ImmutableSet<E> build() {
            return new ImmutableSet<>(set);
        }
    }
}
