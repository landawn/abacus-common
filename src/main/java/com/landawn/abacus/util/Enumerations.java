/*
 * Copyright (c) 2020, Haiyang Li.
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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility class providing static methods for working with {@link Enumeration} objects.
 * This class offers functionality to create, transform, and manipulate Enumerations,
 * bridging the gap between the legacy Enumeration interface and modern Java collections.
 * 
 * <p>While {@link Enumeration} is a legacy interface, it is still used in some APIs
 * (e.g., servlet API, network interfaces). This utility class makes it easier to work
 * with such APIs in a modern Java context.</p>
 * 
 * <p>All methods in this class are static and the class cannot be instantiated.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Convert a Collection to Enumeration
 * List<String> list = Arrays.asList("a", "b", "c");
 * Enumeration<String> enum1 = Enumerations.create(list);
 * 
 * // Convert Enumeration to List
 * List<String> result = Enumerations.toList(enum1);
 * 
 * // Concatenate multiple Enumerations
 * Enumeration<String> combined = Enumerations.concat(enum1, enum2, enum3);
 * </pre>
 * 
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.Iterators
 */
public final class Enumerations {

    /**
     * Singleton instance of an empty Enumeration.
     * This instance is immutable and can be safely shared.
     */
    @SuppressWarnings("rawtypes")
    private static final Enumeration EMPTY = new Enumeration() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Private constructor to prevent instantiation of this utility class.
     * 
     * @throws AssertionError if called
     */
    private Enumerations() {
        // singleton
    }

    /**
     * Returns an empty Enumeration singleton instance.
     * The returned Enumeration has no elements and calling {@code nextElement()} will throw
     * {@link NoSuchElementException}.
     *
     * @param <T> the type of elements (not) enumerated
     * @return an empty Enumeration
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> empty = Enumerations.empty();
     * empty.hasMoreElements(); // false
     * </pre>
     */
    public static <T> Enumeration<T> empty() {
        return EMPTY;
    }

    /**
     * Creates an Enumeration containing a single element.
     * The returned Enumeration will have exactly one element to enumerate.
     *
     * @param <T> the type of the element
     * @param single the single element to enumerate
     * @return an Enumeration containing only the specified element
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> single = Enumerations.just("Hello");
     * single.hasMoreElements(); // true
     * single.nextElement();     // "Hello"
     * single.hasMoreElements(); // false
     * </pre>
     */
    public static <T> Enumeration<T> just(final T single) {
        return new Enumeration<>() {
            private boolean hasNext = true;

            @Override
            public boolean hasMoreElements() {
                return hasNext;
            }

            @Override
            public T nextElement() {
                if (!hasNext) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return single;
            }
        };
    }

    /**
     * Creates an Enumeration from a varargs array of elements.
     * The elements will be enumerated in the order they appear in the array.
     *
     * @param <T> the type of elements
     * @param a the elements to enumerate
     * @return an Enumeration over the specified elements
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<Integer> numbers = Enumerations.of(1, 2, 3, 4, 5);
     * while (numbers.hasMoreElements()) {
     *     System.out.println(numbers.nextElement());
     * }
     * </pre>
     */
    @SafeVarargs
    public static <T> Enumeration<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return new Enumeration<>() {
            private final int len = a.length;
            private int cursor = 0;

            @Override
            public boolean hasMoreElements() {
                return cursor < len;
            }

            @Override
            public T nextElement() {
                if (cursor >= len) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Creates an Enumeration from a Collection.
     * The enumeration order depends on the collection's iterator order.
     *
     * @param <T> the type of elements
     * @param c the collection to create an Enumeration from
     * @return an Enumeration over the collection's elements, or empty if collection is null/empty
     * 
     * <p>Example:</p>
     * <pre>
     * List<String> list = Arrays.asList("a", "b", "c");
     * Enumeration<String> enum1 = Enumerations.create(list);
     * </pre>
     */
    public static <T> Enumeration<T> create(final Collection<? extends T> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return create(c.iterator());
    }

    /**
     * Creates an Enumeration from an Iterator.
     * This provides a bridge from the Iterator interface to the Enumeration interface.
     * 
     * <p>Note: The returned Enumeration is backed by the Iterator, so any modifications
     * to the underlying collection during enumeration may cause undefined behavior.</p>
     *
     * @param <T> the type of elements
     * @param iter the iterator to wrap as an Enumeration
     * @return an Enumeration that delegates to the specified Iterator
     * 
     * <p>Example:</p>
     * <pre>
     * Iterator<String> iter = list.iterator();
     * Enumeration<String> enum1 = Enumerations.create(iter);
     * </pre>
     */
    public static <T> Enumeration<T> create(final Iterator<? extends T> iter) {
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            @Override
            public T nextElement() {
                return iter.next();
            }
        };
    }

    /**
     * Concatenates multiple Enumerations into a single Enumeration.
     * Elements are enumerated in order: all elements from the first Enumeration,
     * then all elements from the second, and so on.
     *
     * @param <T> the type of elements
     * @param a the Enumerations to concatenate
     * @return a single Enumeration containing all elements from all input Enumerations
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> enum1 = Enumerations.of("a", "b");
     * Enumeration<String> enum2 = Enumerations.of("c", "d");
     * Enumeration<String> combined = Enumerations.concat(enum1, enum2);
     * // combined will enumerate: "a", "b", "c", "d"
     * </pre>
     */
    @SafeVarargs
    public static <T> Enumeration<T> concat(final Enumeration<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a collection of Enumerations into a single Enumeration.
     * Elements are enumerated in the order of the Enumerations in the collection,
     * with all elements from each Enumeration being exhausted before moving to the next.
     *
     * @param <T> the type of elements
     * @param c a collection of Enumerations to concatenate
     * @return a single Enumeration containing all elements from all input Enumerations
     * 
     * <p>Example:</p>
     * <pre>
     * List<Enumeration<Integer>> enums = new ArrayList<>();
     * enums.add(Enumerations.of(1, 2));
     * enums.add(Enumerations.of(3, 4));
     * enums.add(Enumerations.of(5, 6));
     * Enumeration<Integer> combined = Enumerations.concat(enums);
     * // combined will enumerate: 1, 2, 3, 4, 5, 6
     * </pre>
     */
    public static <T> Enumeration<T> concat(final Collection<? extends Enumeration<? extends T>> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return new Enumeration<>() {
            private final Iterator<? extends Enumeration<? extends T>> iter = c.iterator();
            private Enumeration<? extends T> cur;

            @Override
            public boolean hasMoreElements() {
                while ((cur == null || !cur.hasMoreElements()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasMoreElements();
            }

            @Override
            public T nextElement() {
                if ((cur == null || !cur.hasMoreElements()) && !hasMoreElements()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextElement();
            }
        };
    }

    /**
     * Converts an Enumeration to an ObjIterator.
     * This provides a bridge from the legacy Enumeration interface to the modern Iterator-based API.
     * The returned ObjIterator provides additional functional operations like map, filter, etc.
     *
     * @param <T> the type of elements
     * @param e the Enumeration to convert, may be null
     * @return an ObjIterator over the Enumeration's elements, or empty iterator if null
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> enum1 = getEnumeration();
     * ObjIterator<String> iter = Enumerations.toIterator(enum1);
     * 
     * // Now can use iterator methods
     * List<String> filtered = iter
     *     .filter(s -> s.length() > 5)
     *     .toList();
     * </pre>
     */
    public static <T> ObjIterator<T> toIterator(final Enumeration<? extends T> e) {
        if (e == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }

            @Override
            public T next() { // NOSONAR
                return e.nextElement();
            }
        };
    }

    /**
     * Converts an Enumeration to a List.
     * All elements from the Enumeration are consumed and added to a new ArrayList
     * in the order they are enumerated.
     *
     * @param <T> the type of elements
     * @param e the Enumeration to convert, may be null
     * @return a new ArrayList containing all elements from the Enumeration, or empty list if null
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> enum1 = getEnumeration();
     * List<String> list = Enumerations.toList(enum1);
     * // list now contains all elements from the enumeration
     * </pre>
     */
    public static <T> List<T> toList(final Enumeration<? extends T> e) {
        if (e == null) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();

        while (e.hasMoreElements()) {
            result.add(e.nextElement());
        }

        return result;
    }

    /**
     * Converts an Enumeration to a Set.
     * All elements from the Enumeration are consumed and added to a new HashSet.
     * Duplicate elements (according to equals()) will be removed.
     *
     * @param <T> the type of elements
     * @param e the Enumeration to convert, may be null
     * @return a new HashSet containing unique elements from the Enumeration, or empty set if null
     * 
     * <p>Example:</p>
     * <pre>
     * Enumeration<String> enum1 = Enumerations.of("a", "b", "a", "c");
     * Set<String> set = Enumerations.toSet(enum1);
     * // set contains: "a", "b", "c" (duplicates removed)
     * </pre>
     */
    public static <T> Set<T> toSet(final Enumeration<? extends T> e) {
        if (e == null) {
            return new HashSet<>();
        }

        final Set<T> result = new HashSet<>();

        while (e.hasMoreElements()) {
            result.add(e.nextElement());
        }

        return result;
    }

    /**
     * Converts an Enumeration to a Collection using the provided Supplier.
     * This allows converting to any Collection type by providing an appropriate supplier.
     * All elements from the Enumeration are consumed and added to the collection.
     *
     * @param <T> the type of elements
     * @param <C> the type of the Collection to create
     * @param e the Enumeration to convert, may be null
     * @param supplier the supplier to create the target collection
     * @return a collection containing all elements from the Enumeration
     * 
     * <p>Example:</p>
     * <pre>
     * // Convert to LinkedList
     * Enumeration<String> enum1 = getEnumeration();
     * LinkedList<String> list = Enumerations.toCollection(enum1, LinkedList::new);
     * 
     * // Convert to TreeSet
     * TreeSet<String> sorted = Enumerations.toCollection(enum1, TreeSet::new);
     * </pre>
     */
    public static <T, C extends Collection<T>> C toCollection(final Enumeration<? extends T> e, final Supplier<? extends C> supplier) {
        final C c = supplier.get();

        if (e != null) {
            while (e.hasMoreElements()) {
                c.add(e.nextElement());
            }
        }

        return c;
    }
}