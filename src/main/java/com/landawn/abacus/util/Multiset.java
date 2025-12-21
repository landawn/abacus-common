/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;
import com.landawn.abacus.util.stream.Stream;

//Copied from Google Guava under Apache License 2.0 and modified.
/**
 * A collection that supports order-independent equality, like {@link Set}, but allows duplicate elements.
 * A multiset is also sometimes called a <i>bag</i>. This final class provides a complete implementation
 * for managing collections where elements can occur multiple times with efficient counting operations.
 *
 * <p>Elements of a multiset that are equal to one another are referred to as <i>occurrences</i> of
 * the same single element. The total number of occurrences of an element in a multiset is called
 * the <i>count</i> of that element (the terms "frequency" and "multiplicity" are equivalent, but
 * not used in this API). Since the count of an element is represented as an {@code int}, a multiset
 * may never contain more than {@link Integer#MAX_VALUE} occurrences of any one element.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Duplicate Elements:</b> Unlike Set, allows multiple occurrences of the same element</li>
 *   <li><b>Count Operations:</b> Efficient operations for getting, setting, and modifying element counts</li>
 *   <li><b>Collection Interface:</b> Implements Collection&lt;E&gt; for seamless integration</li>
 *   <li><b>Flexible Backing:</b> Customizable backing Map implementation for different performance characteristics</li>
 *   <li><b>Statistical Operations:</b> Built-in methods for analyzing occurrence patterns</li>
 *   <li><b>Stream Integration:</b> Full support for stream operations and functional programming</li>
 *   <li><b>Thread Safety:</b> Thread safety depends on the chosen backing Map implementation</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Count Limitations:</b>
 * <ul>
 *   <li>Element counts are limited to {@code Integer.MAX_VALUE}</li>
 *   <li>Negative counts are not allowed and will throw IllegalArgumentException</li>
 *   <li>Setting count to 0 effectively removes the element from the multiset</li>
 *   <li>The total size() may overflow for very large multisets</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Frequency Counting:</b> Counting occurrences of elements in datasets</li>
 *   <li><b>Statistical Analysis:</b> Analyzing distribution patterns and frequencies</li>
 *   <li><b>Inventory Management:</b> Tracking quantities of items</li>
 *   <li><b>Text Processing:</b> Word frequency analysis and text mining</li>
 *   <li><b>Data Aggregation:</b> Grouping and counting similar items</li>
 *   <li><b>Survey Results:</b> Collecting and analyzing response frequencies</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic multiset operations
 * Multiset<String> words = new Multiset<>();
 * words.add("hello", 3);   // Add 3 occurrences
 * words.add("world", 2);   // Add 2 occurrences
 * System.out.println(words.getCount("hello"));   // Prints: 3
 * System.out.println(words.size());   // Prints: 5 (total elements)
 *
 * // Creating from collections
 * List<String> data = Arrays.asList("a", "b", "a", "c", "b", "a");
 * Multiset<String> frequency = new Multiset<>(data);
 * frequency.forEach((element, count) -> 
 *     System.out.println(element + ": " + count));
 * // Output: a: 3, b: 2, c: 1
 *
 * // Statistical operations
 * Multiset<Integer> scores = Multiset.of(85, 90, 85, 92, 88, 85);
 * Optional<Pair<Integer, Integer>> mode = scores.maxOccurrences();
 * System.out.println("Most frequent score: " + mode.get().right());   // 85
 * System.out.println("Frequency: " + mode.get().left());   // 3
 *
 * // Stream operations
 * Map<String, Integer> filtered = Multiset.of("apple", "banana", "apple", "cherry")
 *     .entries()
 *     .filter(entry -> entry.count() > 1)
 *     .toMap(Multiset.Entry::element, Multiset.Entry::count);
 *
 * // Custom backing map for sorted elements
 * Multiset<String> sorted = new Multiset<>(TreeMap::new);
 * sorted.addAll(Arrays.asList("zebra", "apple", "banana"));
 * // Elements will be ordered alphabetically
 * }</pre>
 *
 * <p><b>Multiset-Specific Operations:</b>
 * <ul>
 *   <li>{@code getCount(element)} - Get the count of an element</li>
 *   <li>{@code setCount(element, count)} - Set the count of an element</li>
 *   <li>{@code add(element, count)} - Add multiple occurrences at once</li>
 *   <li>{@code remove(element, count)} - Remove specific number of occurrences</li>
 *   <li>{@code removeAllOccurrences(element)} - Remove all occurrences of an element</li>
 *   <li>{@code elementSet()} - Get the set of distinct elements</li>
 *   <li>{@code entrySet()} - Get entries with element-count pairs</li>
 *   <li>{@code toMap()} - Convert to Map&lt;E, Integer&gt;</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Backed by HashMap by default - O(1) average time for most operations</li>
 *   <li>Can use TreeMap for sorted element access - O(log n) operations</li>
 *   <li>Space complexity: O(k) where k is the number of distinct elements</li>
 *   <li>Iteration time proportional to total element count, not distinct elements</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * Thread safety depends on the backing Map implementation:
 * <ul>
 *   <li>HashMap backing: Not thread-safe, external synchronization required</li>
 *   <li>ConcurrentHashMap backing: Thread-safe for concurrent access</li>
 *   <li>Collections.synchronizedMap() can be used for thread-safe operations</li>
 * </ul>
 *
 * <p><b>Comparison with Other Collections:</b>
 * <ul>
 *   <li><b>vs Set:</b> Allows duplicates, provides count information</li>
 *   <li><b>vs List:</b> No ordering guarantees, optimized for count operations</li>
 *   <li><b>vs Map&lt;E,Integer&gt;:</b> Implements Collection interface, specialized multiset operations</li>
 * </ul>
 *
 * <p><b>Implementation Note:</b>
 * This implementation is backed by a {@link Map} where keys are the distinct elements and values
 * are their occurrence counts stored as {@link MutableInt} objects for efficiency. The backing map type
 * can be customized via constructors to achieve different performance characteristics.
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @param <E> the type of elements maintained by this multiset .
 */
public final class Multiset<E> implements Collection<E> {

    private static final Comparator<Map.Entry<?, MutableInt>> cmpByCount = (a, b) -> N.compare(a.getValue().value(), b.getValue().value());

    private final Supplier<Map<E, MutableInt>> backingMapSupplier;

    private final Map<E, MutableInt> backingMap;

    /**
     * Constructs an empty multiset with a default {@link HashMap} as the backing map.
     * The initial capacity is set to the default initial capacity of HashMap.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * multiset.add("hello");
     * }</pre>
     */
    public Multiset() {
        this(HashMap.class);
    }

    /**
     * Constructs an empty multiset with the specified initial capacity.
     * Uses a {@link HashMap} as the backing map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>(100);
     * // Can efficiently store up to 100 distinct elements
     * }</pre>
     *
     * @param initialCapacity the initial capacity of the backing map.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public Multiset(final int initialCapacity) {
        backingMapSupplier = Suppliers.ofMap();
        backingMap = N.newHashMap(initialCapacity);
    }

    /**
     * Constructs a multiset containing the elements of the specified collection.
     * The multiset will contain all elements from the collection, preserving duplicates.
     * 
     * <p>If the input collection is a {@link Set}, the initial capacity is set to its size.
     * Otherwise, the initial capacity is set to half the collection size (assuming duplicates).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
     * Multiset<String> multiset = new Multiset<>(list);
     * System.out.println(multiset.getCount("a"));   // Prints: 3
     * }</pre>
     *
     * @param c the collection whose elements are to be placed into this multiset.
     */
    public Multiset(final Collection<? extends E> c) {
        this((c == null || c instanceof Set) ? N.size(c) : N.size(c) / 2);

        addAll(c);
    }

    /**
     * Constructs an empty multiset with a backing map of the specified type.
     * This allows customization of the internal map implementation for specific use cases
     * (e.g., {@link LinkedHashMap} for insertion order preservation, {@link TreeMap} for sorted elements).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a multiset that maintains insertion order
     * Multiset<String> orderedMultiset = new Multiset<>(LinkedHashMap.class);
     * }</pre>
     *
     * @param valueMapType the class of the map to be used as the backing map.
     * @throws IllegalArgumentException if the specified map type cannot be instantiated.
     */
    @SuppressWarnings("rawtypes")
    public Multiset(final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(N.requireNonNull(valueMapType)));
    }

    /**
     * Constructs an empty multiset with a backing map supplied by the provided supplier.
     * This provides maximum flexibility in creating the backing map instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a multiset with a custom map supplier
     * Multiset<String> multiset = new Multiset<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
     * }</pre>
     *
     * @param mapSupplier the supplier that provides the map to be used as the backing map.
     */
    @SuppressWarnings("rawtypes")
    public Multiset(final Supplier<? extends Map<? extends E, ?>> mapSupplier) {
        backingMapSupplier = (Supplier) N.requireNonNull(mapSupplier);
        backingMap = backingMapSupplier.get();
    }

    @SuppressWarnings("rawtypes")
    Multiset(final Map<E, MutableInt> valueMap) {
        backingMapSupplier = (Supplier) Suppliers.ofMap(valueMap.getClass());
        backingMap = valueMap;
    }

    /**
     * Creates a new multiset containing the specified elements.
     * Duplicate elements in the array will be counted as separate occurrences.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
     * System.out.println(multiset.getCount("a"));   // Prints: 2
     * }</pre>
     *
     * @param <T> the type of elements.
     * @param a the elements to be placed into the multiset.
     * @return a new multiset containing the specified elements.
     */
    @SafeVarargs
    public static <T> Multiset<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return new Multiset<>();
        }

        final Multiset<T> multiset = new Multiset<>(N.newHashMap(a.length));

        multiset.addAll(Array.asList(a));

        return multiset;
    }

    /**
     * Creates a new multiset containing the elements of the specified collection.
     * This is a static factory method alternative to the constructor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("x", "y", "x", "z");
     * Multiset<String> multiset = Multiset.create(list);
     * }</pre>
     *
     * @param <T> the type of elements.
     * @param coll the collection whose elements are to be placed into the multiset.
     * @return a new multiset containing the elements of the specified collection.
     */
    public static <T> Multiset<T> create(final Collection<? extends T> coll) {
        return new Multiset<>(coll);
    }

    /**
     * Creates a new multiset containing the elements from the specified iterator.
     * This method consumes the iterator and counts occurrences of each element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b", "a").iterator();
     * Multiset<String> multiset = Multiset.create(iter);
     * System.out.println(multiset.getCount("a"));   // Prints: 2
     * }</pre>
     *
     * @param <T> the type of elements.
     * @param iter the iterator whose elements are to be placed into the multiset.
     * @return a new multiset containing the elements from the iterator.
     * @throws IllegalArgumentException if adding an element would exceed {@link Integer#MAX_VALUE} occurrences.
     */
    public static <T> Multiset<T> create(final Iterator<? extends T> iter) {
        final Multiset<T> result = new Multiset<>();

        if (iter != null) {
            T e = null;
            MutableInt count = null;

            while (iter.hasNext()) {
                e = iter.next();
                count = result.backingMap.get(e);

                if (count == null) {
                    result.backingMap.put(e, MutableInt.of(1));
                } else {
                    if (count.value() == Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("The total count is out of the bound of int"); //NOSONAR
                    }

                    count.add(1);
                }
            }
        }

        return result;
    }

    /**
     * Returns the number of occurrences of the specified element in this multiset.
     * This is an alias for {@link #getCount(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
     * System.out.println(multiset.occurrencesOf("a"));   // Prints: 2
     * System.out.println(multiset.occurrencesOf("d"));   // Prints: 0
     * }</pre>
     *
     * @param element the element to count occurrences of.
     * @return the number of occurrences of the element; zero if not present.
     * @see #getCount(Object)
     */
    public int occurrencesOf(final Object element) {
        return getCount(element);
    }

    /**
     * Finds the element with the minimum number of occurrences in this multiset.
     * If multiple elements have the same minimum count, one is arbitrarily chosen.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b", "c");
     * Optional<Pair<Integer, String>> min = multiset.minOccurrences();
     * // min contains Pair.of(1, "c")
     * }</pre>
     *
     * @return an Optional containing a Pair of (count, element) with minimum occurrences,
     *         or empty Optional if the multiset is empty.
     */
    public Optional<Pair<Integer, E>> minOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<E, MutableInt>> it = backingMap.entrySet().iterator();
        Map.Entry<E, MutableInt> entry = it.next();
        E minCountElement = entry.getKey();
        int minCount = entry.getValue().value();

        while (it.hasNext()) {
            entry = it.next();

            if (entry.getValue().value() < minCount) {
                minCountElement = entry.getKey();
                minCount = entry.getValue().value();
            }
        }

        return Optional.of(Pair.of(minCount, minCountElement));
    }

    /**
     * Finds the element with the maximum number of occurrences in this multiset.
     * If multiple elements have the same maximum count, one is arbitrarily chosen.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b", "c");
     * Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
     * // max contains Pair.of(3, "b")
     * }</pre>
     *
     * @return an Optional containing a Pair of (count, element) with maximum occurrences,
     *         or empty Optional if the multiset is empty.
     */
    public Optional<Pair<Integer, E>> maxOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<E, MutableInt>> it = backingMap.entrySet().iterator();
        Map.Entry<E, MutableInt> entry = it.next();
        E maxCountElement = entry.getKey();
        int maxCount = entry.getValue().value();

        while (it.hasNext()) {
            entry = it.next();

            if (entry.getValue().value() > maxCount) {
                maxCountElement = entry.getKey();
                maxCount = entry.getValue().value();
            }
        }

        return Optional.of(Pair.of(maxCount, maxCountElement));
    }

    /**
     * Finds all elements with the minimum number of occurrences in this multiset.
     * Returns all elements that share the lowest occurrence count.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "c", "d");
     * Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
     * // allMin contains Pair.of(1, ["c", "d"])
     * }</pre>
     *
     * @return an Optional containing a Pair of (count, list of elements) with minimum occurrences,
     *         or empty Optional if the multiset is empty.
     */
    public Optional<Pair<Integer, List<E>>> allMinOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        int min = Integer.MAX_VALUE;

        for (final MutableInt e : backingMap.values()) {
            if (e.value() < min) {
                min = e.value();
            }
        }

        final List<E> res = new ArrayList<>();

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            if (entry.getValue().value() == min) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(min, res));
    }

    /**
     * Finds all elements with the maximum number of occurrences in this multiset.
     * Returns all elements that share the highest occurrence count.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "b", "c");
     * Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
     * // allMax contains Pair.of(3, ["a", "b"])
     * }</pre>
     *
     * @return an Optional containing a Pair of (count, list of elements) with maximum occurrences,
     *         or empty Optional if the multiset is empty.
     */
    public Optional<Pair<Integer, List<E>>> allMaxOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        int max = Integer.MIN_VALUE;

        for (final MutableInt e : backingMap.values()) {
            if (e.value() > max) {
                max = e.value();
            }
        }

        final List<E> res = new ArrayList<>();

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            if (entry.getValue().value() == max) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(max, res));
    }

    /**
     * Calculates the sum of all occurrences of all elements in this multiset.
     * This is equivalent to the total number of elements including duplicates.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b");
     * System.out.println(multiset.sumOfOccurrences());   // Prints: 5
     * }</pre>
     *
     * @return the total count of all elements.
     * @throws ArithmeticException if the sum exceeds {@link Long#MAX_VALUE}.
     * @see #size()
     */
    public long sumOfOccurrences() {
        if (backingMap.isEmpty()) {
            return 0;
        }

        long sum = 0;

        for (final MutableInt count : backingMap.values()) {
            sum = Numbers.addExact(sum, count.value());
        }

        return sum;
    }

    /**
     * Calculates the average number of occurrences per distinct element in this multiset.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b");
     * OptionalDouble avg = multiset.averageOfOccurrences();
     * // avg contains 2.5 (5 total occurrences / 2 distinct elements)
     * }</pre>
     *
     * @return an OptionalDouble containing the average occurrences per distinct element,
     *         or empty if the multiset is empty.
     */
    public OptionalDouble averageOfOccurrences() {
        if (backingMap.isEmpty()) {
            return OptionalDouble.empty();
        }

        final double sum = sumOfOccurrences();

        return OptionalDouble.of(sum / backingMap.size());
    }

    // Query Operations

    /**
     * Returns the number of occurrences of the specified element in this multiset.
     *
     * <p>This method gives the same result as {@link Collections#frequency} for an
     * {@link Object#equals}-based multiset, but with better performance.</p>
     *
     * <p><b>Migration Guidance:</b></p>
     * <p>This method is deprecated. Use {@link #getCount(Object)} instead for clearer semantics.</p>
     * <pre>{@code
     * // Old code:
     * int count = multiset.count(element);
     *
     * // New code:
     * int count = multiset.getCount(element);
     * }</pre>
     *
     * @param element the element to count occurrences of.
     * @return the number of occurrences of the element; zero if not present.
     * @deprecated Use {@link #getCount(Object)} instead for better clarity.
     * @see #getCount(Object)
     */
    @Deprecated
    public int count(final Object element) {
        return getCount(element);
    }

    /**
     * Returns the number of occurrences of the specified element in this multiset.
     *
     * <p>This method gives the same result as {@link Collections#frequency} for an
     * {@link Object#equals}-based multiset, but with better performance.</p>
     *
     * <p><b>Migration Guidance:</b></p>
     * <p>This method is deprecated. Use {@link #getCount(Object)} instead for clearer semantics.</p>
     * <pre>{@code
     * // Old code:
     * int count = multiset.get(element);
     *
     * // New code:
     * int count = multiset.getCount(element);
     * }</pre>
     *
     * @param element the element to count occurrences of.
     * @return the number of occurrences of the element; zero if not present.
     * @deprecated Use {@link #getCount(Object)} instead for better clarity.
     * @see #getCount(Object)
     */
    @Deprecated
    public int get(final Object element) {
        return getCount(element);
    }

    /**
     * Returns the number of occurrences of the specified element in this multiset.
     *
     * <p>This method gives the same result as {@link Collections#frequency} for an
     * {@link Object#equals}-based multiset, but with better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
     * System.out.println(multiset.getCount("a"));   // Prints: 2
     * System.out.println(multiset.getCount("d"));   // Prints: 0
     * }</pre>
     *
     * @param element the element to count occurrences of.
     * @return the number of occurrences of the element; zero if not present.
     * @see #occurrencesOf(Object)
     */
    public int getCount(final Object element) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.get(element);

        return count == null ? 0 : count.value();
    }

    // Bulk Operations

    /**
     * Sets the count of the specified element to a specific value.
     * If the new count is zero, the element is removed from the multiset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * int oldCount = multiset.setCount("apple", 5);     // oldCount is 0
     * System.out.println(multiset.getCount("apple"));   // Prints: 5
     * }</pre>
     *
     * @param element the element to set the count of.
     * @param occurrences the desired count; must be non-negative.
     * @return the count of the element before the operation..
     * @throws IllegalArgumentException if occurrences is negative..
     * @see #setCount(Object, int, int)
     * @see #add(Object, int)
     */
    public int setCount(final E element, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = backingMap.get(element);
        final int oldCount = count == null ? 0 : count.value();

        if (occurrences == 0) {
            if (count != null) {
                backingMap.remove(element);
            }
        } else {
            if (count == null) {
                backingMap.put(element, MutableInt.of(occurrences));
            } else {
                count.setValue(occurrences);
            }
        }

        return oldCount;
    }

    /**
     * Conditionally sets the count of the specified element to a new value if it currently has the expected count.
     * This operation is useful for concurrent scenarios or conditional updates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple");
     * boolean updated = multiset.setCount("apple", 2, 5);   // true
     * System.out.println(multiset.getCount("apple"));       // Prints: 5
     *
     * updated = multiset.setCount("apple", 2, 10);          // false (current count is 5, not 2)
     * }</pre>
     *
     * @param element the element to set the count of.
     * @param oldOccurrences the expected current count; must be non-negative.
     * @param newOccurrences the desired new count; must be non-negative.
     * @return {@code true} if the count was updated, {@code false} if the current count didn't match oldOccurrences.
     * @throws IllegalArgumentException if oldOccurrences or newOccurrences is negative.
     * @see #setCount(Object, int)
     */
    public boolean setCount(final E element, final int oldOccurrences, final int newOccurrences) {
        checkOccurrences(oldOccurrences);
        checkOccurrences(newOccurrences);

        final MutableInt count = backingMap.get(element);
        final int oldCount = count == null ? 0 : count.value();

        if (oldOccurrences == oldCount) {
            if (newOccurrences == 0) {
                if (count != null) {
                    backingMap.remove(element);
                }
            } else {
                if (count == null) {
                    backingMap.put(element, MutableInt.of(newOccurrences));
                } else {
                    count.setValue(newOccurrences);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Adds a single occurrence of the specified element to this multiset.
     * 
     * <p>This method always increments the count of the element by one and increases the total size
     * of the multiset by one.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * multiset.add("apple");
     * System.out.println(multiset.getCount("apple"));   // Prints: 1
     * }</pre>
     *
     * @param element the element to add one occurrence of.
     * @return {@code true} (as specified by {@link Collection#add}).
     * @throws IllegalArgumentException if the element already has {@link Integer#MAX_VALUE} occurrences.
     */
    @Override
    public boolean add(final E element) {
        add(element, 1);
        return true;
    }

    /**
     * Adds the specified number of occurrences of an element to this multiset.
     *
     * <p>This method is functionally equivalent (except for overflow checking) to calling
     * {@code addAll(Collections.nCopies(element, occurrencesToAdd))}, but performs much better.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * int oldCount = multiset.add("apple", 3);          // oldCount is 0
     * System.out.println(multiset.getCount("apple"));   // Prints: 3
     * }</pre>
     *
     * @param element the element to add occurrences of.
     * @param occurrencesToAdd the number of occurrences to add; must be non-negative.
     * @return the count of the element before the operation.
     * @throws IllegalArgumentException if occurrencesToAdd is negative., or if the addition would exceed {@link Integer#MAX_VALUE} occurrences.
     * @see #addAndGetCount(Object, int)
     * @see #setCount(Object, int)
     */
    public int add(final E element, final int occurrencesToAdd) {
        checkOccurrences(occurrencesToAdd);

        MutableInt count = backingMap.get(element);

        if (count != null && occurrencesToAdd > (Integer.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of int");
        }

        final int oldCount = count == null ? 0 : count.value();

        if (count == null) {
            if (occurrencesToAdd > 0) {
                count = MutableInt.of(occurrencesToAdd);
                backingMap.put(element, count);
            }
        } else {
            count.add(occurrencesToAdd);
        }

        return oldCount;
    }

    /**
     * Adds the specified number of occurrences of an element to this multiset and returns the new count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple");
     * int newCount = multiset.addAndGetCount("apple", 3);
     * System.out.println(newCount);   // Prints: 5
     * }</pre>
     *
     * @param element the element to add occurrences of.
     * @param occurrences the number of occurrences to add; must be non-negative.
     * @return the count of the element after the operation.
     * @throws IllegalArgumentException if occurrences is negative., or if the addition would exceed {@link Integer#MAX_VALUE} occurrences.
     * @see #add(Object, int)
     */
    @Beta
    public int addAndGetCount(final E element, final int occurrences) {
        checkOccurrences(occurrences);

        MutableInt count = backingMap.get(element);

        if (count != null && occurrences > (Integer.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of int");
        }

        if (count == null) {
            if (occurrences > 0) {
                count = MutableInt.of(occurrences);
                backingMap.put(element, count);
                return occurrences;
            }
            return 0;
        } else {
            count.add(occurrences);
            return count.value();
        }
    }

    /**
     * Adds all elements from the specified collection to this multiset.
     * Each element is added once, regardless of how many times it appears in the collection.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * List<String> list = Arrays.asList("a", "b", "a");
     * multiset.addAll(list);
     * System.out.println(multiset.getCount("a"));   // Prints: 2
     * }</pre>
     *
     * @param c the collection containing elements to be added.
     * @return {@code true} if this multiset changed as a result of the call.
     */
    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return addAll(c, 1);
    }

    /**
     * Adds all elements from the specified collection to this multiset with the given number of occurrences.
     * Each distinct element in the collection is added the specified number of times.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * List<String> list = Arrays.asList("a", "b");
     * multiset.addAll(list, 3);
     * System.out.println(multiset.getCount("a"));   // Prints: 3
     * System.out.println(multiset.getCount("b"));   // Prints: 3
     * }</pre>
     *
     * @param c the collection containing elements to be added.
     * @param occurrencesToAdd the number of occurrences to add for each element.
     * @return {@code true} if this multiset changed as a result of the call.
     * @throws IllegalArgumentException if occurrencesToAdd is negative.
     */
    @Beta
    public boolean addAll(final Collection<? extends E> c, final int occurrencesToAdd) {
        checkOccurrences(occurrencesToAdd);

        if (N.isEmpty(c) || occurrencesToAdd == 0) {
            return false;
        }

        for (final E e : c) {
            add(e, occurrencesToAdd);
        }

        return true;
    }

    /**
     * Removes a single occurrence of the specified element from this multiset, if present.
     * 
     * <p>If the element has multiple occurrences, only one is removed.
     * If the element has only one occurrence, it is removed completely from the multiset.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * multiset.remove("a");
     * System.out.println(multiset.getCount("a"));   // Prints: 1
     * }</pre>
     *
     * @param element the element to remove one occurrence of.
     * @return {@code true} if an occurrence was found and removed.
     */
    @Override
    public boolean remove(final Object element) {
        return remove(element, 1) > 0;
    }

    /**
     * Removes the specified number of occurrences of an element from this multiset.
     * If the multiset contains fewer occurrences than specified, all occurrences are removed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
     * int oldCount = multiset.remove("a", 2);       // oldCount is 3
     * System.out.println(multiset.getCount("a"));   // Prints: 1
     * }</pre>
     *
     * @param element the element to remove occurrences of.
     * @param occurrencesToRemove the number of occurrences to remove; must be non-negative.
     * @return the count of the element before the operation.
     * @throws IllegalArgumentException if occurrencesToRemove is negative.
     * @see #removeAndGetCount(Object, int)
     * @see #removeAllOccurrences(Object)
     */
    public int remove(final Object element, final int occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.get(element);

        if (count == null) {
            return 0;
        }

        final int oldCount = count.value();

        count.subtract(occurrencesToRemove);

        if (count.value() <= 0) {
            //noinspection SuspiciousMethodCalls
            backingMap.remove(element);
        }

        return oldCount;
    }

    /**
     * Removes the specified number of occurrences of an element and returns the new count.
     * If the multiset contains fewer occurrences than specified, all occurrences are removed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
     * int newCount = multiset.removeAndGetCount("a", 2);
     * System.out.println(newCount);   // Prints: 1
     * }</pre>
     *
     * @param element the element to remove occurrences of.
     * @param occurrences the number of occurrences to remove; must be non-negative.
     * @return the count of the element after the operation.
     * @throws IllegalArgumentException if occurrences is negative.
     * @see #remove(Object, int)
     */
    @Beta
    public int removeAndGetCount(final Object element, final int occurrences) {
        checkOccurrences(occurrences);

        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.get(element);

        if (count == null) {
            return 0;
        }

        count.subtract(occurrences);

        if (count.value() <= 0) {
            //noinspection SuspiciousMethodCalls
            backingMap.remove(element);

            return 0;
        }

        return count.value();
    }

    /**
     * Removes all occurrences of all elements in the specified collection from this multiset.
     *
     * <p><b>Note:</b> This method ignores the occurrence count in the collection and removes
     * ALL occurrences of each element present in the collection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "c");
     * multiset.removeAll(Arrays.asList("a", "b"));
     * System.out.println(multiset);   // Only contains "c"
     * }</pre>
     *
     * <p><b>Migration Guidance:</b></p>
     * <p>This method is deprecated. Use {@link #removeAllOccurrences(Collection)} instead for clearer semantics.</p>
     * <pre>{@code
     * // Old code:
     * multiset.removeAll(collection);
     *
     * // New code:
     * multiset.removeAllOccurrences(collection);
     * }</pre>
     *
     * @param c the collection containing elements to be removed.
     * @return {@code true} if this multiset changed as a result of the call..
     * @deprecated replaced by {@link #removeAllOccurrences(Collection)} for better clarity.
     */
    @Deprecated
    @Override
    public boolean removeAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        boolean result = false;

        for (final Object e : c) {
            if (!result) {
                result = backingMap.remove(e) != null;
            } else {
                backingMap.remove(e);
            }
        }

        return result;
    }

    /**
     * Removes the specified number of occurrences of each element in the collection from this multiset.
     * If an element has fewer occurrences than specified, all its occurrences are removed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b");
     * multiset.removeAll(Arrays.asList("a", "b"), 2);
     * System.out.println(multiset.getCount("a"));   // Prints: 1
     * System.out.println(multiset.getCount("b"));   // Prints: 0
     * }</pre>
     *
     * @param c the collection containing elements to be removed.
     * @param occurrencesToRemove the number of occurrences to remove for each element.
     * @return {@code true} if this multiset changed as a result of the call.
     * @throws IllegalArgumentException if occurrencesToRemove is negative.
     */
    public boolean removeAll(final Collection<?> c, final int occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        if (N.isEmpty(c) || occurrencesToRemove == 0) {
            return false;
        }

        boolean result = false;

        for (final Object e : c) {
            if (!result) {
                result = remove(e, occurrencesToRemove) > 0;
            } else {
                remove(e, occurrencesToRemove);
            }
        }

        return result;
    }

    /**
     * Removes all occurrences of a specific element from this multiset.
     * After this operation, the element will not be present in the multiset.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
     * int removed = multiset.removeAllOccurrences("a");
     * System.out.println(removed);                  // Prints: 3
     * System.out.println(multiset.contains("a"));   // Prints: false
     * }</pre>
     *
     * @param e the element whose all occurrences are to be removed.
     * @return the count of the element before removal, or 0 if not present.
     */
    public int removeAllOccurrences(final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.remove(e);

        return count == null ? 0 : count.value();
    }

    /**
     * Removes all occurrences of all elements in the specified collection from this multiset.
     * This is equivalent to calling {@link #removeAll(Collection)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "c");
     * multiset.removeAllOccurrences(Arrays.asList("a", "b"));
     * System.out.println(multiset);   // Only contains "c"
     * }</pre>
     *
     * @param c the collection containing elements to be removed.
     * @return {@code true} if this multiset changed as a result of the call.
     */
    @SuppressWarnings("deprecation")
    public boolean removeAllOccurrences(final Collection<?> c) {
        return removeAll(c);
    }

    /**
     * Removes all occurrences of elements that satisfy the given predicate.
     * The predicate is tested against each distinct element (not each occurrence).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple", "banana", "cherry");
     * multiset.removeAllOccurrencesIf(s -> s.startsWith("a"));
     * // Removes all occurrences of "apple"
     * }</pre>
     *
     * @param predicate the predicate which returns {@code true} for elements to be removed.
     * @return {@code true} if any elements were removed.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public boolean removeAllOccurrencesIf(final Predicate<? super E> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        Set<E> removingKeys = null;

        for (final E key : backingMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(key);
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        for (final Object e : removingKeys) {
            //noinspection SuspiciousMethodCalls
            backingMap.remove(e);
        }

        return true;
    }

    /**
     * Removes all occurrences of elements that satisfy the given predicate.
     * The predicate receives both the element and its count.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "c");
     * multiset.removeAllOccurrencesIf((element, count) -> count >= 3);
     * // Removes all occurrences of "a" (which has count 3)
     * }</pre>
     *
     * @param predicate the predicate which returns {@code true} for elements to be removed.
     * @return {@code true} if any elements were removed.
     * @throws IllegalArgumentException if the predicate is null.
     */
    public boolean removeAllOccurrencesIf(final ObjIntPredicate<? super E> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        Set<E> removingKeys = null;

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().value())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isEmpty(removingKeys)) {
            return false;
        }

        for (final Object e : removingKeys) {
            //noinspection SuspiciousMethodCalls
            backingMap.remove(e);
        }

        return true;
    }

    /**
     * Updates the count of each element in this multiset based on the provided function.
     * The function receives each element and its current count, and returns the new count.
     * If the function returns zero or negative, the element is removed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b");
     * multiset.updateAllOccurrences((element, count) -> count * 2);
     * System.out.println(multiset.getCount("a"));   // Prints: 4
     * System.out.println(multiset.getCount("b"));   // Prints: 6
     * }</pre>
     *
     * @param function the function to compute new counts.
     * @throws IllegalArgumentException if the function is null
     */
    public void updateAllOccurrences(final ObjIntFunction<? super E, Integer> function) throws IllegalArgumentException {
        N.checkArgNotNull(function);

        List<E> keyToRemove = null;
        Integer newVal = null;

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            newVal = function.apply(entry.getKey(), entry.getValue().value());

            if (newVal == null || newVal <= 0) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else {
                entry.getValue().setValue(newVal);
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (final E key : keyToRemove) {
                backingMap.remove(key);
            }
        }
    }

    /**
     * Computes the count of the specified element if it is not already present.
     * If the element is present, returns its current count without modification.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * int count = multiset.computeIfAbsent("apple", e -> 5);
     * System.out.println(count);   // Prints: 5
     * 
     * count = multiset.computeIfAbsent("apple", e -> 10);
     * System.out.println(count);   // Prints: 5 (not modified)
     * }</pre>
     *
     * @param e the element whose count is to be computed.
     * @param mappingFunction the function to compute a count.
     * @return the current (existing or computed) count of the element
     * @throws IllegalArgumentException if the mapping function is null
     */
    public int computeIfAbsent(final E e, final ToIntFunction<? super E> mappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(mappingFunction);

        final int oldValue = getCount(e);

        if (oldValue > 0) {
            return oldValue;
        }

        final int newValue = mappingFunction.applyAsInt(e);

        if (newValue > 0) {
            setCount(e, newValue);
        }

        return newValue;
    }

    /**
     * Computes a new count for the specified element if it is already present.
     * If the element is not present, returns 0 without modification.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple");
     * int newCount = multiset.computeIfPresent("apple", (e, count) -> count * 2);
     * System.out.println(newCount);   // Prints: 4
     * }</pre>
     *
     * @param e the element whose count is to be computed.
     * @param remappingFunction the function to compute a new count.
     * @return the new count of the element, or 0 if not present
     * @throws IllegalArgumentException if the remapping function is null
     */
    public int computeIfPresent(final E e, final ObjIntFunction<? super E, Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = getCount(e);

        if (oldValue == 0) {
            return oldValue;
        }

        final int newValue = remappingFunction.apply(e, oldValue);

        if (newValue > 0) {
            setCount(e, newValue);
        } else {
            backingMap.remove(e);
        }

        return newValue;
    }

    /**
     * Computes a new count for the specified element, whether present or not.
     * The remapping function receives the element and its current count (0 if not present).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple");
     * int newCount = multiset.compute("apple", (e, count) -> count + 3);
     * System.out.println(newCount);   // Prints: 5
     * 
     * newCount = multiset.compute("banana", (e, count) -> count + 2);
     * System.out.println(newCount);   // Prints: 2
     * }</pre>
     *
     * @param key the element whose count is to be computed.
     * @param remappingFunction the function to compute a new count.
     * @return the new count of the element
     * @throws IllegalArgumentException if the remapping function is null
     */
    public int compute(final E key, final ObjIntFunction<? super E, Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = getCount(key);
        final int newValue = remappingFunction.apply(key, oldValue);

        if (newValue > 0) {
            setCount(key, newValue);
        } else {
            if (oldValue > 0) {
                removeAllOccurrences(key);
            }
        }

        return newValue;
    }

    /**
     * Merges the specified value with the current count of the element using the remapping function.
     * If the element is not present, the value is used as the new count.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("apple", "apple");
     * int newCount = multiset.merge("apple", 3, (oldCount, value) -> oldCount + value);
     * System.out.println(newCount);   // Prints: 5
     * 
     * newCount = multiset.merge("banana", 4, (oldCount, value) -> oldCount + value);
     * System.out.println(newCount);   // Prints: 4
     * }</pre>
     *
     * @param key the element whose count is to be merged.
     * @param value the value to merge with the existing count.
     * @param remappingFunction the function to merge the old count and value.
     * @return the new count of the element
     * @throws IllegalArgumentException if the remapping function is null
     */
    public int merge(final E key, final int value, final IntBiFunction<Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = getCount(key);
        final int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);

        if (newValue > 0) {
            setCount(key, newValue);
        } else {
            if (oldValue > 0) {
                backingMap.remove(key);
            }
        }

        return newValue;
    }

    // Comparison and hashing

    // Refined Collection Methods

    /**
     * Retains only the elements in this multiset that are contained in the specified collection.
     * In other words, removes all elements that are not in the specified collection.
     * 
     * <p><b>Note:</b> This method only checks for element presence, not occurrence counts.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "c");
     * multiset.retainAll(Arrays.asList("a", "c"));
     * // multiset now contains only "a" (2 occurrences) and "c" (1 occurrence)
     * }</pre>
     *
     * @param c the collection containing elements to be retained.
     * @return {@code true} if this multiset changed as a result of the call.
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            final boolean result = !backingMap.isEmpty();
            clear();
            return result;
        }

        Set<E> others = null;

        for (final E e : backingMap.keySet()) {
            if (!c.contains(e)) {
                if (others == null) {
                    others = N.newHashSet(backingMap.size());
                }

                others.add(e);
            }
        }

        return !N.isEmpty(others) && removeAllOccurrences(others);
    }

    /**
     * Returns {@code true} if this multiset contains at least one occurrence of the specified element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "a");
     * System.out.println(multiset.contains("a"));   // Prints: true
     * System.out.println(multiset.contains("c"));   // Prints: false
     * }</pre>
     *
     * @param element the element whose presence is to be tested.
     * @return {@code true} if this multiset contains at least one occurrence of the element
     * @see #containsAll(Collection)
     * @see #getCount(Object)
     */
    @Override
    public boolean contains(final Object element) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(element);
    }

    /**
     * Returns {@code true} if this multiset contains at least one occurrence of each element
     * in the specified collection.
     *
     * <p><b>Note:</b> This method does not consider occurrence counts. It returns {@code true}
     * even if the collection contains multiple occurrences of an element and this multiset
     * contains only one.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "c");
     * System.out.println(multiset.containsAll(Arrays.asList("a", "b")));   // Prints: true
     * System.out.println(multiset.containsAll(Arrays.asList("a", "d")));   // Prints: false
     * }</pre>
     *
     * @param c the collection to be checked for containment.
     * @return {@code true} if this multiset contains all elements in the specified collection
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            return true;
        }

        return backingMap.keySet().containsAll(c);
    }

    // Query Operations

    // Comparison and hashing

    // Refined Collection Methods

    /**
     * Returns an unmodifiable view of the distinct elements contained in this multiset.
     * The element set is backed by the multiset, so changes to the multiset are reflected
     * in the element set.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b", "c");
     * Set<String> elements = multiset.elementSet();
     * System.out.println(elements);   // Prints: [a, b, c] (order may vary)
     * }</pre>
     *
     * @return a view of the distinct elements in this multiset
     */
    public Set<E> elementSet() {
        return backingMap.keySet();
    }

    private transient Set<Entry<E>> entrySet; // NOSONAR

    /**
     * Returns a view of the contents of this multiset, grouped into {@link Entry} instances,
     * each providing an element and its count.
     * 
     * <p>The entry set contains exactly one entry for each distinct element in the multiset.
     * The entry set is backed by the multiset, so changes to the multiset are reflected in
     * the entry set.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b");
     * for (Multiset.Entry<String> entry : multiset.entrySet()) {
     *     System.out.println(entry.element() + ": " + entry.count());
     * }
     * // Prints: a: 2
     * //         b: 3
     * }</pre>
     *
     * @return a set of entries representing the data of this multiset
     */
    public Set<Multiset.Entry<E>> entrySet() {
        Set<Multiset.Entry<E>> result = entrySet;

        if (result == null) {
            entrySet = result = new EntrySet();
        }

        return result;
    }

    class EntrySet extends AbstractSet<Multiset.Entry<E>> {

        @Override
        public boolean contains(final Object obj) {
            if (obj instanceof Multiset.Entry<?> entry) {
                @SuppressWarnings("SuspiciousMethodCalls")
                final MutableInt count = backingMap.get(entry.element());

                return count == null ? entry.count() == 0 : count.value() == entry.count();
            }

            return false;
        }

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public ObjIterator<Entry<E>> iterator() {
            final Iterator<Map.Entry<E, MutableInt>> backingEntryIter = backingMap.entrySet().iterator();

            return new ObjIterator<>() {
                private Map.Entry<E, MutableInt> next;

                @Override
                public boolean hasNext() {
                    return backingEntryIter.hasNext();
                }

                @Override
                public Entry<E> next() {
                    next = backingEntryIter.next();
                    return new ImmutableEntry<>(next.getKey(), next.getValue().value());
                }
            };
        }
    }

    /**
     * Returns an iterator over the elements in this multiset.
     * Elements that occur multiple times will appear multiple times in the iterator,
     * though not necessarily consecutively.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * Iterator<String> iter = multiset.iterator();
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     * }
     * // May print: a, a, b (order of identical elements not guaranteed)
     * }</pre>
     *
     * @return an iterator over all element occurrences in this multiset
     */
    @Override
    public ObjIterator<E> iterator() {
        final Iterator<Map.Entry<E, MutableInt>> entryIter = backingMap.entrySet().iterator();

        return new ObjIterator<>() {
            private Map.Entry<E, MutableInt> entry = null;
            private E element = null;
            private int count = 0;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (cnt >= count) {
                    while (cnt >= count && entryIter.hasNext()) {
                        entry = entryIter.next();
                        element = entry.getKey();
                        count = entry.getValue().value();
                        cnt = 0;
                    }
                }

                return cnt < count;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;

                return element;
            }
        };
    }

    /**
     * Returns the total number of all occurrences of all elements in this multiset.
     * This is the sum of counts for all distinct elements.
     * 
     * <p><b>Note:</b> This is different from {@link #countOfDistinctElements()} which returns
     * the number of distinct elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b");
     * System.out.println(multiset.size());                      // Prints: 5
     * System.out.println(multiset.countOfDistinctElements());   // Prints: 2
     * }</pre>
     *
     * @return the total number of element occurrences
     * @throws ArithmeticException if the total count exceeds {@link Integer#MAX_VALUE}
     * @see #countOfDistinctElements()
     * @see #sumOfOccurrences()
     */
    @Override
    public int size() throws ArithmeticException {
        return backingMap.isEmpty() ? 0 : Numbers.toIntExact(sumOfOccurrences());
    }

    /**
     * Returns the number of distinct elements in this multiset.
     * This is equivalent to {@code elementSet().size()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b", "c");
     * System.out.println(multiset.countOfDistinctElements());   // Prints: 3
     * System.out.println(multiset.size());                      // Prints: 6 (total occurrences)
     * }</pre>
     *
     * @return the number of distinct elements
     */
    @Beta
    public int countOfDistinctElements() {
        return backingMap.size();
    }

    /**
     * Checks if this multiset is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * System.out.println(multiset.isEmpty());   // Prints: true
     * multiset.add("test");
     * System.out.println(multiset.isEmpty());   // Prints: false
     * }</pre>
     *
     * @return {@code true} if this multiset contains no elements, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Clears all elements from this multiset.
     *
     * <p>This method removes all elements from the multiset, effectively making it empty.
     * The count of all elements in the multiset will be zero after this operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * multiset.clear();
     * System.out.println(multiset.isEmpty());   // Prints: true
     * }</pre>
     */
    @Override
    public void clear() {
        backingMap.clear();
    }

    //    public Set<Map.Entry<E, MutableInt>> entrySet() {
    //        return valueMap.entrySet();
    //    }

    /**
     * Converts the multiset to an array.
     *
     * <p>This method returns an array containing all the elements in this multiset.
     * Elements that occur multiple times in the multiset will appear multiple times in the array.
     * The order of elements in the array is unspecified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * Object[] array = multiset.toArray();
     * System.out.println(array.length);   // Prints: 3
     * }</pre>
     *
     * @return an array containing all the elements in this multiset
     */
    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    /**
     * Converts the multiset to an array of a specific type.
     *
     * <p>This method returns an array containing all the elements in this multiset.
     * Elements that occur multiple times in the multiset will appear multiple times in the array.
     * The order of elements in the array is unspecified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * String[] array = multiset.toArray(new String[0]);
     * System.out.println(array.length);   // Prints: 3
     * }</pre>
     *
     * @param <T> the runtime type of the array to contain the multiset.
     * @param a the array into which the elements of this multiset are to be stored, if it is big enough;.
     *              otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing all the elements in this multiset
     * @throws IllegalArgumentException if the provided array is {@code null}
     */
    @Override
    public <T> T[] toArray(final T[] a) throws IllegalArgumentException {
        N.checkArgNotNull(a, "The specified array cannot be null");

        final int size = size();
        final T[] ret = a.length < size ? N.newArray(a.getClass().getComponentType(), size) : a;

        int idx = 0;
        int occurrences = 0;

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            occurrences = entry.getValue().value();
            N.fill(ret, idx, idx + occurrences, entry.getKey());
            idx += occurrences;
        }

        return ret;
    }

    /**
     * Converts the multiset to a map where keys are elements and values are their counts.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * Map<String, Integer> map = multiset.toMap();
     * System.out.println(map.get("a"));   // Prints: 2
     * System.out.println(map.get("b"));   // Prints: 1
     * }</pre>
     *
     * @return a map with the elements of this multiset as keys and their counts as values
     */
    public Map<E, Integer> toMap() {
        final Map<E, Integer> result = Maps.newTargetMap(backingMap);

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    /**
     * Converts the multiset to a map created by the provided supplier function.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The type of the map is determined by the provided supplier function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * TreeMap<String, Integer> map = multiset.toMap(TreeMap::new);
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param supplier a function that generates a new instance of the desired map type.
     * @return a map with the elements of this multiset as keys and their counts as values
     */
    public <M extends Map<E, Integer>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(backingMap.size());

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    /**
     * Converts the multiset to a map sorted by occurrences in ascending order.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted in ascending order by the count of the elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "b", "c", "c", "c");
     * Map<String, Integer> sorted = multiset.toMapSortedByOccurrences();
     * // Map entries ordered: a=1, b=2, c=3
     * }</pre>
     *
     * @return a map with the elements of this multiset as keys and their counts as values, sorted by the counts
     */
    @SuppressWarnings("rawtypes")
    public Map<E, Integer> toMapSortedByOccurrences() {
        return toMapSortedBy((Comparator) cmpByCount);
    }

    /**
     * Converts the multiset to a map sorted by occurrences using a custom comparator.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted by the counts of the elements using the provided comparator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "b", "c", "c", "c");
     * Map<String, Integer> sorted = multiset.toMapSortedByOccurrences(Comparator.reverseOrder());
     * // Map entries ordered: c=3, b=2, a=1 (descending by count)
     * }</pre>
     *
     * @param cmp the comparator to be used for sorting the counts of the elements.
     * @return a map with the elements of this multiset as keys and their counts as values, sorted by the counts using the provided comparator
     */
    public Map<E, Integer> toMapSortedByOccurrences(final Comparator<? super Integer> cmp) {
        return toMapSortedBy((o1, o2) -> cmp.compare(o1.getValue().value(), o2.getValue().value()));
    }

    /**
     * Converts the multiset to a map sorted by keys.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted by the keys of the elements using the provided comparator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("c", "a", "b", "a");
     * Map<String, Integer> sorted = multiset.toMapSortedByKey(Comparator.naturalOrder());
     * // Map entries ordered: a=2, b=1, c=1
     * }</pre>
     *
     * @param cmp the comparator to be used for sorting the keys of the elements.
     * @return a map with the elements of this multiset as keys and their counts as values, sorted by the keys using the provided comparator
     */
    public Map<E, Integer> toMapSortedByKey(final Comparator<? super E> cmp) {
        return toMapSortedBy(Comparators.comparingByKey(cmp));
    }

    /**
     * Converts the multiset to a map sorted by a custom comparator.
     *
     * <p>This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted according to the order induced by the provided comparator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * Map<String, Integer> sorted = multiset.toMapSortedBy(
     *     Comparator.comparing(e -> e.getValue().value()));
     * }</pre>
     *
     * @param cmp the comparator to be used for sorting the entries of the map. The comparator should compare Map.Entry objects.
     * @return a map with the elements of this multiset as keys and their counts as values, sorted according to the provided comparator.
     */
    Map<E, Integer> toMapSortedBy(final Comparator<Map.Entry<E, MutableInt>> cmp) {
        if (N.isEmpty(backingMap)) {
            return new LinkedHashMap<>();
        }

        final int distinctElementSize = backingMap.size();
        final Map.Entry<E, MutableInt>[] entries = backingMap.entrySet().toArray(new Map.Entry[distinctElementSize]);

        Arrays.sort(entries, cmp);

        final Map<E, Integer> resultMap = N.newLinkedHashMap(distinctElementSize);

        for (final Map.Entry<E, MutableInt> entry : entries) {
            resultMap.put(entry.getKey(), entry.getValue().value());
        }

        return resultMap;
    }

    /**
     * Returns an unmodifiable map where the keys are the elements in this multiset and the values are their corresponding counts.
     *
     * <p>Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * ImmutableMap<String, Integer> immutableMap = multiset.toImmutableMap();
     * // Cannot be modified - throws UnsupportedOperationException
     * }</pre>
     *
     * @return an immutable map with the elements of this multiset as keys and their counts as values
     */
    public ImmutableMap<E, Integer> toImmutableMap() {
        return ImmutableMap.wrap(toMap());
    }

    /**
     * Returns an unmodifiable map where the keys are the elements in this multiset and the values are their corresponding counts.
     *
     * <p>Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The type of the map is determined by the provided supplier function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("c", "a", "b", "a");
     * ImmutableMap<String, Integer> immutableMap =
     *     multiset.toImmutableMap(size -> new TreeMap<>());
     * }</pre>
     *
     * @param mapSupplier a function that generates a new instance of the desired map type.
     * @return an immutable map with the elements of this multiset as keys and their counts as values
     */
    public ImmutableMap<E, Integer> toImmutableMap(final IntFunction<? extends Map<E, Integer>> mapSupplier) {
        return ImmutableMap.wrap(toMap(mapSupplier));
    }

    // Query Operations

    /**
     * Performs the given action for each element of the multiset until all elements
     * have been processed or the action throws an exception.
     * 
     * <p>Elements that occur multiple times in the multiset will be passed to the {@code Consumer}
     * correspondingly many times, though not necessarily sequentially.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * multiset.forEach(System.out::println);   // Prints: a, a, b (order may vary)
     * }</pre>
     *
     * @param action The action to be performed for each element.
     * @throws IllegalArgumentException if the provided action is {@code null}
     */
    @Override
    public void forEach(final Consumer<? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (E e : this) {
            action.accept(e);
        }
    }

    /**
     * Runs the specified action for each distinct element in this multiset, and the number of
     * occurrences of that element. For some {@code Multiset} implementations, this may be more
     * efficient than iterating over the {@link #entrySet()} either explicitly or with {@code
     * entrySet().forEach(action)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b", "b", "b");
     * multiset.forEach((element, count) -> 
     *     System.out.println(element + " occurs " + count + " times"));
     * // Prints: a occurs 2 times, b occurs 3 times (order may vary)
     * }</pre>
     *
     * @param action The action to be performed for each distinct element in the multiset and its count. This can be any instance of ObjIntConsumer.
     * @throws IllegalArgumentException if the provided action is {@code null}.
     */
    public void forEach(final ObjIntConsumer<? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue().value());
        }
    }

    /**
     * Returns an (unmodifiable) {@code Stream} with elements from the {@code Multiset}.
     * Elements that occur multiple times in the multiset will appear multiple times in this {@code Stream}, though not necessarily sequentially.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * List<String> list = multiset.elements().toList();   // [a, a, b]
     * }</pre>
     *
     * @return a Stream containing all element occurrences in this multiset
     * @see #iterator()
     */
    @Beta
    public Stream<E> elements() {
        return Stream.of(iterator());
    }

    /**
     * Returns an (unmodifiable) Stream of entries in this multiset.
     *
     * <p>Each entry in the Stream represents a distinct element in the multiset and its count.
     * The order of the entries in the Stream is unspecified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * multiset.entries().forEach(entry ->
     *     System.out.println(entry.element() + ": " + entry.count()));
     * // Prints: a: 2, b: 1 (order may vary)
     * }</pre>
     *
     * @return a Stream of entries representing the data of this multiset
     */
    @Beta
    public Stream<Multiset.Entry<E>> entries() {
        return Stream.of(entrySet());
    }

    /**
     * Applies the provided function to this multiset and returns the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "c");
     * int distinctCount = multiset.apply(ms -> ms.countOfDistinctElements());   // 3
     * }</pre>
     *
     * @param <R> the type of the result returned by the function.
     * @param <X> the type of the exception that can be thrown by the function.
     * @param func the function to be applied to this multiset.
     * @return the result of applying the provided function to this multiset
     * @throws X if the provided function throws an exception of type X
     */
    public <R, X extends Exception> R apply(final Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return func.apply(this);
    }

    /**
     * Applies the provided function to this multiset and returns the result, if the multiset is not empty.
     *
     * <p>This method applies the provided function to this multiset and returns the result wrapped in an Optional.
     * If the multiset is empty, it returns an empty Optional.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * Optional<Integer> size = multiset.applyIfNotEmpty(ms -> ms.size());   // Optional.empty()
     *
     * multiset.add("test");
     * size = multiset.applyIfNotEmpty(ms -> ms.size());   // Optional.of(1)
     * }</pre>
     *
     * @param <R> the type of the result returned by the function.
     * @param <X> the type of the exception that can be thrown by the function.
     * @param func the function to be applied to this multiset.
     * @return an Optional containing the result of applying the provided function to this multiset, or an empty Optional if the multiset is empty
     * @throws X if the provided function throws an exception of type X
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     * Applies the provided consumer function to this multiset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "b", "c");
     * multiset.accept(ms -> System.out.println("Size: " + ms.size()));   // Prints: Size: 3
     * }</pre>
     *
     * @param <X> the type of the exception that can be thrown by the consumer function.
     * @param action the consumer function to be applied to this multiset.
     * @throws X if the provided consumer function throws an exception of type X
     */
    public <X extends Exception> void accept(final Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        action.accept(this);
    }

    /**
     * Applies the provided consumer function to this multiset if it is not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = new Multiset<>();
     * multiset.acceptIfNotEmpty(ms -> System.out.println("Not empty"))
     *         .orElse(() -> System.out.println("Empty"));   // Prints: Empty
     *
     * multiset.add("test");
     * multiset.acceptIfNotEmpty(ms -> System.out.println("Not empty"))
     *         .orElse(() -> System.out.println("Empty"));   // Prints: Not empty
     * }</pre>
     *
     * @param <X> the type of the exception that can be thrown by the consumer function.
     * @param action the consumer function to be applied to this multiset.
     * @return an instance of OrElse which can be used to perform some other operation if the Multiset is empty
     * @throws X if the provided consumer function throws an exception of type X
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        return If.is(!backingMap.isEmpty()).then(this, action);
    }

    /**
     * Returns the hash code value for this multiset.
     *
     * <p>The hash code is computed based on the backing map's hash code,
     * which includes both the elements and their counts.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> ms1 = Multiset.of("a", "b");
     * Multiset<String> ms2 = Multiset.of("b", "a");
     * System.out.println(ms1.hashCode() == ms2.hashCode());   // true
     * }</pre>
     *
     * @return the hash code value for this multiset
     */
    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    /**
     * Checks if this Multiset is equal to the specified object.
     * The method returns {@code true} if the specified object is also a Multiset and has the same keys and occurrences pair.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> ms1 = Multiset.of("a", "a", "b");
     * Multiset<String> ms2 = Multiset.of("b", "a", "a");
     * System.out.println(ms1.equals(ms2));   // true
     * }</pre>
     *
     * @param obj The object to be compared with this Multiset for equality.
     * @return {@code true} if the specified object is equal to this Multiset, {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multiset && backingMap.equals(((Multiset<E>) obj).backingMap));
    }

    /**
     * Returns a string representation of this Multiset.
     *
     * The string representation consists of a list of key-value mappings in the Multiset, enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters ", " (comma and space).
     * Each key-value mapping is rendered as the key followed by an equals sign ("=") followed by the associated value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * System.out.println(multiset);   // Prints: {a=2, b=1} (order may vary)
     * }</pre>
     *
     * @return a string representation of this Multiset.
     */
    @Override
    public String toString() {
        return backingMap.toString();
    }

    private static int checkOccurrences(final int occurrences) {
        if (occurrences < 0) {
            throw new IllegalArgumentException("The specified 'occurrences' cannot be negative");
        }

        return occurrences;
    }

    /**
     * An unmodifiable element-count pair for a multiset. The {@link Multiset#entrySet} method returns
     * a view of the multiset whose elements are of this class. A multiset implementation may return
     * Entry instances that are either live "read-through" views to the Multiset, or immutable
     * snapshots. Note that this type is unrelated to the similarly-named type {@code Map.Entry}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> multiset = Multiset.of("a", "a", "b");
     * for (Multiset.Entry<String> entry : multiset.entrySet()) {
     *     System.out.println(entry.element() + " appears " + entry.count() + " times");
     * }
     * }</pre>
     *
     * @param <E> the type of element contained in the multiset.
     */
    public interface Entry<E> {

        /**
         * Returns the multiset element corresponding to this entry. Multiple calls to this method
         * always return the same instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Multiset<String> multiset = Multiset.of("a", "a", "b");
         * for (Multiset.Entry<String> entry : multiset.entrySet()) {
         *     String element = entry.element();
         *     System.out.println("Element: " + element);
         * }
         * }</pre>
         *
         * @return the element corresponding to this entry
         */
        E element();

        /**
         * Returns the count of the associated element in the underlying multiset. This count may either
         * be an unchanging snapshot of the count at the time the entry was retrieved, or a live view of
         * the current count of the element in the multiset, depending on the implementation. Note that
         * in the former case, this method can never return zero, while in the latter, it will return
         * zero if all occurrences of the element were since removed from the multiset.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Multiset<String> multiset = Multiset.of("a", "a", "b");
         * for (Multiset.Entry<String> entry : multiset.entrySet()) {
         *     int count = entry.count();
         *     System.out.println("Count: " + count);
         * }
         * }</pre>
         *
         * @return the count of the element; never negative
         */
        int count();

        /**
         * Compares the specified object with this entry for equality.
         *
         * <p>Returns {@code true} if the given object is also a multiset entry and the two entries
         * represent the same element and count. That is, two entries {@code a} and {@code b} are equal
         * if:</p>
         *
         * <pre>{@code
         * N.equals(a.element(), b.element())
         *     && a.count() == b.count()
         * }</pre>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Multiset.Entry<String> entry1 = ...;
         * Multiset.Entry<String> entry2 = ...;
         * boolean same = entry1.equals(entry2);
         * }</pre>
         *
         * @param o object to be compared for equality with this multiset entry.
         * @return {@code true} if the specified object is equal to this entry
         */
        @Override
        // TODO(kevinb): check this wrt TreeMultiset?
        boolean equals(Object o);

        /**
         * Returns the hash code value for this multiset entry.
         *
         * <p>The hash code of a multiset entry for element {@code element} and count {@code count} is
         * defined as:</p>
         *
         * <pre>{@code
         * ((element == null) ? 0 : element.hashCode()) ^ count
         * }</pre>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Multiset.Entry<String> entry = ...;
         * int hash = entry.hashCode();
         * }</pre>
         *
         * @return the hash code value for this multiset entry
         */
        @Override
        int hashCode();

        /**
         * Returns the canonical string representation of this entry, defined as follows. If the count
         * for this entry is one, this is simply the string representation of the corresponding element.
         * Otherwise, it is the string representation of the element, followed by the three characters
         * {@code " x "} (space, letter x, space), followed by the count.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Entry with element "hello" and count 1 returns: "hello"
         * Entry with element "world" and count 3 returns: "world x 3"
         * }</pre>
         *
         * @return a string representation of this entry
         */
        @Override
        String toString();
    }

    /**
     * An immutable implementation of the {@link Entry} interface.
     * This class represents a snapshot of an element-count pair at a specific point in time.
     * 
     * @param <E> the type of element in this entry.
     */
    @SuppressWarnings("ClassCanBeRecord")
    static final class ImmutableEntry<E> implements Entry<E> { // NOSONAR
        private final E element;
        private final int count;

        /**
         * Constructs an immutable entry with the specified element and count.
         *
         * @param element the element for this entry.
         * @param count the count of occurrences for this element.
         */
        ImmutableEntry(final E element, final int count) {
            this.element = element;
            this.count = count;
        }

        @Override
        public E element() {
            return element;
        }

        @Override
        public int count() {
            return count;
        }

        /**
         * Compares this entry to the specified object for equality.
         * Two entries are equal if they have the same element and count.
         *
         * @param object the object to compare with this entry.
         * @return {@code true} if the specified object is equal to this entry
         */
        @Override
        public boolean equals(final Object object) {
            if (object instanceof Multiset.Entry<?> that) {
                return count == that.count() && N.equals(element, that.element());
            }

            return false;
        }

        /**
         * Return this entry's hash code, following the behavior specified in {@link
         * Multiset.Entry#hashCode}.
         * 
         * @return the hash code for this entry
         */
        @Override
        public int hashCode() {
            return ((element == null) ? 0 : element.hashCode()) ^ count;
        }

        /**
         * Returns a string representation of this multiset entry. The string representation consists of
         * the associated element if the associated count is one, and otherwise the associated element
         * followed by the characters " x " (space, x and space) followed by the count. Elements and
         * counts are converted to strings as by {@code String.valueOf}.
         * 
         * @return a string representation of this entry
         */
        @Override
        public String toString() {
            final String text = String.valueOf(element);

            return (count == 1) ? text : (text + " x " + count);
        }
    }
}
