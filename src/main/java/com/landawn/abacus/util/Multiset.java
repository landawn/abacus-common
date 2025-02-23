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
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;
import com.landawn.abacus.util.function.ObjIntPredicate;
import com.landawn.abacus.util.stream.Stream;

// Copied from Google Guava under Apache License 2.0 and modified.

/**
 * <pre>
 * Copied from Google Guava under Apache License 2.0 and modified.
 * </pre>
 *
 * A collection that supports order-independent equality, like {@link Set}, but may have duplicate
 * elements. A multiset is also sometimes called a <i>bag</i>.
 *
 * <p>Elements of a multiset that are equal to one another are referred to as <i>occurrences</i> of
 * the same single element. The total number of occurrences of an element in a multiset is called
 * the <i>count</i> of that element (the terms "frequency" and "multiplicity" are equivalent, but
 * not used in this API). Since the count of an element is represented as an {@code int}, a multiset
 * may never contain more than {@link Integer#MAX_VALUE} occurrences of any one element.
 *
 */
public final class Multiset<E> implements Collection<E> {

    private static final Comparator<Map.Entry<?, MutableInt>> cmpByCount = (a, b) -> N.compare(a.getValue().value(), b.getValue().value());

    private final Supplier<Map<E, MutableInt>> backingMapSupplier;

    private final Map<E, MutableInt> backingMap;

    /**
     * Constructs a new instance of Multiset with the default initial capacity.
     *
     */
    public Multiset() {
        this(HashMap.class);
    }

    /**
     * Constructs a new instance of Multiset with the specified initial capacity.
     *
     * @param initialCapacity
     */
    public Multiset(final int initialCapacity) {
        backingMapSupplier = Suppliers.ofMap();
        backingMap = N.newHashMap(initialCapacity);
    }

    /**
     * Constructs a new instance of Multiset with the elements from the specified collection.
     *
     * This constructor initializes a new instance of Multiset and adds all elements from the given collection.
     * The count of each element in the Multiset will be its frequency in the given collection.
     *
     * @param c The collection whose elements are to be placed into this Multiset.
     */
    public Multiset(final Collection<? extends E> c) {
        this((c == null || c instanceof Set) ? N.size(c) : N.size(c) / 2);

        addAll(c);
    }

    /**
     * Constructs a new instance of Multiset with a backing map of the specified type.
     *
     * This constructor initializes a new instance of Multiset with an empty backing map of the given type.
     * The backing map is used to store the elements of the multiset and their corresponding counts.
     *
     * @param valueMapType The type of the map to be used as the backing map. Must not be {@code null}.
     * @throws NullPointerException if the provided {@code valueMapType} is {@code null}.
     */
    @SuppressWarnings("rawtypes")
    public Multiset(final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(N.requireNonNull(valueMapType)));
    }

    /**
     * Constructs a new instance of Multiset with a backing map supplied by the provided supplier.
     *
     * This constructor initializes a new instance of Multiset with an empty backing map supplied by the given supplier.
     * The backing map is used to store the elements of the multiset and their corresponding counts.
     *
     * @param mapSupplier The supplier that provides the map to be used as the backing map. Must not be {@code null}.
     * @throws NullPointerException if the provided {@code mapSupplier} is {@code null}.
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
     * Creates a new instance of Multiset with the elements from the specified array.
     *
     * This method initializes a new instance of Multiset and adds all elements from the given array.
     * The count of each element in the Multiset will be its frequency in the given array.
     *
     * @param a The array whose elements are to be placed into this Multiset.
     * @return a new Multiset containing the elements of the specified array.
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
     * Creates a new instance of Multiset with the elements from the specified collection.
     *
     * This method initializes a new instance of Multiset and adds all elements from the given collection.
     * The count of each element in the Multiset will be its frequency in the given collection.
     *
     * @param coll The collection whose elements are to be placed into this Multiset.
     * @return a new Multiset containing the elements of the specified collection.
     */
    public static <T> Multiset<T> create(final Collection<? extends T> coll) {
        return new Multiset<>(coll);
    }

    /**
     * Creates a new instance of Multiset with the elements from the specified iterator.
     *
     * This method initializes a new instance of Multiset and adds all elements from the given iterator.
     * The count of each element in the Multiset will be its frequency in the given iterator.
     *
     * @param iter The iterator whose elements are to be placed into this Multiset.
     * @return a new Multiset containing the elements of the specified iterator.
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
     * Returns the number of occurrences of a specific element in the multiset.
     * If the element is not present, it returns 0.
     *
     * @param e The element whose occurrences are to be counted. This can be any instance of Object.
     * @return The number of occurrences of the specified element in the multiset.
     * @see #getCount(Object)
     */
    public int occurrencesOf(final Object e) {
        return getCount(e);
    }

    /**
     * Finds the element with the minimum occurrence in the multiset.
     *
     * <p>This method attempts to find the element in the multiset that has the minimum occurrence.
     * If there are multiple elements with the same minimum occurrence, one of them is arbitrarily chosen.</p>
     *
     * @return An Optional Pair containing the minimum occurrence and the corresponding element,
     *         or an empty Optional if the multiset is empty.
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
     * Finds the element with the maximum occurrence in the multiset.
     *
     * <p>This method attempts to find the element in the multiset that has the maximum occurrence.
     * If there are multiple elements with the same maximum occurrence, one of them is arbitrarily chosen.</p>
     *
     * @return An Optional Pair containing the maximum occurrence and the corresponding element,
     *         or an empty Optional if the multiset is empty.
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
     * Finds all elements with the minimum occurrence in the multiset.
     *
     * <p>This method attempts to find all elements in the multiset that have the minimum occurrence.
     * If there are multiple elements with the same minimum occurrence, all of them are included in the result.</p>
     *
     * @return An Optional Pair containing the minimum occurrence and the corresponding list of elements,
     *         or an empty Optional if the multiset is empty.
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
     * Finds all elements with the maximum occurrence in the multiset.
     *
     * <p>This method attempts to find all elements in the multiset that have the maximum occurrence.
     * If there are multiple elements with the same maximum occurrence, all of them are included in the result.</p>
     *
     * @return An Optional Pair containing the maximum occurrence and the corresponding list of elements,
     *         or an empty Optional if the multiset is empty.
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
     * Calculates the total number of occurrences of all elements in this multiset.
     *
     * @return The total number of all occurrences of all elements in this multiset.
     * @throws ArithmeticException if the total number of all occurrences of all elements overflows a long.
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
     * Calculates the average number of occurrences of all elements in this multiset.
     *
     * @return An OptionalDouble containing the average number of occurrences of all elements in this multiset.
     *         If the multiset is empty, the returned OptionalDouble is empty.
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
     * Returns the number of occurrences of an element in this multiset (the <i>count</i> of the
     * element). Note that for an {@link Object#equals}-based multiset, this gives the same result as
     * {@link Collections#frequency} (which would presumably perform more poorly).
     *
     * <p><b>Note:</b> the utility method {@link Collections#frequency} generalizes this operation; it
     * correctly delegates to this method when dealing with a multiset, but it can also accept any
     * other iterable type.
     *
     * @param element the element to count occurrences of
     * @return the number of occurrences of the element in this multiset; possibly zero but never
     *     negative
     * @deprecated Use {@link #getCount(Object)} instead
     */
    @Deprecated
    public int count(final Object element) {
        return getCount(element);
    }

    /**
     * Returns the number of occurrences of an element in this multiset (the <i>count</i> of the
     * element). Note that for an {@link Object#equals}-based multiset, this gives the same result as
     * {@link Collections#frequency} (which would presumably perform more poorly).
     *
     * <p><b>Note:</b> the utility method {@link Collections#frequency} generalizes this operation; it
     * correctly delegates to this method when dealing with a multiset, but it can also accept any
     * other iterable type.
     *
     * @param element the element to count occurrences of
     * @return the number of occurrences of the element in this multiset; possibly zero but never
     *     negative
     */
    public int getCount(final Object element) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.get(element);

        return count == null ? 0 : count.value();
    }

    // Bulk Operations

    /**
     * Adds or removes the necessary occurrences of an element such that the element attains the
     * desired count.
     *
     * @param element the element to add or remove occurrences of; may be {@code null} only if explicitly
     *     allowed by the implementation
     * @param occurrences the desired count of the element in this multiset
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws NullPointerException if {@code element} is {@code null} and this implementation does not permit
     *     {@code null} elements. Note that if {@code count} is zero, the implementor may optionally return
     *     zero instead.
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
     * Conditionally sets the count of an element to a new value, as described in {@link
     * #setCount(Object, int)}, provided that the element has the expected current count. If the
     * current count is not {@code oldCount}, no change is made.
     *
     * @param element the element to conditionally set the count of; may be {@code null} only if explicitly
     *     allowed by the implementation
     * @param oldOccurrences
     * @param newOccurrences the desired count of the element in this multiset
     * @return {@code true} if the condition for modification was met. This implies that the multiset
     *     was indeed modified, unless {@code oldCount == newCount}.
     * @throws IllegalArgumentException if {@code oldCount} or {@code newCount} is negative
     * @throws NullPointerException if {@code element} is {@code null} and the implementation does not permit
     *     {@code null} elements. Note that if {@code oldCount} and {@code newCount} are both zero, the
     *     implementor may optionally return {@code true} instead.
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
     * <p>This method refines {@link Collection#add}, which only <i>ensures</i> the presence of the
     * element, to further specify that a successful call must always increment the count of the
     * element, and the overall size of the collection, by one.
     *
     * <p>To both add the element and obtain the previous count of that element, use {@link
     * #add(Object, int) add}{@code (element, 1)} instead.
     *
     * @param element the element to add one occurrence of; may be {@code null} only if explicitly allowed by
     *     the implementation
     * @return {@code true} always, since this call is required to modify the multiset, unlike other
     *     {@link Collection} types
     * @throws NullPointerException if {@code element} is {@code null} and this implementation does not permit
     *     {@code null} elements
     * @throws IllegalArgumentException if {@link Integer#MAX_VALUE} occurrences of {@code element}
     *     are already contained in this multiset
     */
    @Override
    public boolean add(final E element) {
        add(element, 1);
        return true;
    }

    /**
     * Adds a number of occurrences of an element to this multiset. Note that if {@code occurrences ==
     * 1}, this method has the identical effect to {@link #add(Object)}. This method is functionally
     * equivalent (except in the case of overflow) to the call {@code
     * addAll(Collections.nCopies(element, occurrences))}, which would presumably perform much more
     * poorly.
     *
     * @param element the element to add occurrences of; may be {@code null} only if explicitly allowed by the
     *     implementation
     * @param occurrencesToAdd the number of occurrences of the element to add. May be zero, in which case
     *     no change will be made.
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code occurrences} is negative, or if this operation would
     *     result in more than {@link Integer#MAX_VALUE} occurrences of the element
     * @throws NullPointerException if {@code element} is {@code null} and this implementation does not permit
     *     {@code null} elements. Note that if {@code occurrences} is zero, the implementation may opt to
     *     return normally.
     *
     * @see #addAndGetCount(Object, int)
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
     * Adds a number of occurrences of an element to this multiset and returns the new count of the element.
     *
     * @param element The element to add occurrences of. This can be any instance of the generic type E.
     * @param occurrences The number of occurrences of the element to add. Must be a non-negative integer.
     * @return The new count of the element in the multiset after the operation.
     * @throws IllegalArgumentException if {@code occurrences} is negative, or if this operation would
     *     result in more than {@link Integer#MAX_VALUE} occurrences of the element.
     * @throws NullPointerException if {@code element} is {@code null} and this implementation does not permit
     *     {@code null} elements.
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
            }
        } else {
            count.add(occurrences);
        }

        return count == null ? occurrences : count.value();
    }

    /**
     * Adds all elements from the specified collection to this multiset.
     *
     * This method adds each element from the given collection to the multiset.
     * If an element is already present in the multiset, its count is incremented.
     * If an element is not present, it is added with a count of one.
     *
     * @param c The collection whose elements are to be added to this multiset.
     * @return {@code true} if this multiset changed as a result of the call.
     */
    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return addAll(c, 1);
    }

    /**
     * Adds all elements from the specified collection to this multiset with the given number of occurrences.
     *
     * This method adds each element from the given collection to the multiset with the specified number of occurrences.
     * If an element is already present in the multiset, its count is incremented by the specified number of occurrences.
     * If an element is not present, it is added with a count of the specified number of occurrences.
     *
     * @param c The collection whose elements are to be added to this multiset.
     * @param occurrencesToAdd The number of occurrences to add for each element in the collection. Must be a non-negative integer.
     * @return {@code true} if this multiset changed as a result of the call.
     * @throws IllegalArgumentException if {@code occurrencesToAdd} is negative.
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
     * Removes a <i>single</i> occurrence of the specified element from this multiset, if present.
     *
     * <p>This method refines {@link Collection#remove} to further specify that it <b>may not</b>
     * throw an exception in response to {@code element} being {@code null} or of the wrong type.
     *
     * <p>To both remove the element and obtain the previous count of that element, use {@link
     * #remove(Object, int) remove}{@code (element, 1)} instead.
     *
     * @param element the element to remove one occurrence of
     * @return {@code true} if an occurrence was found and removed
     */
    @Override
    public boolean remove(final Object element) {
        return remove(element, 1) > 0;
    }

    /**
     * Removes a number of occurrences of the specified element from this multiset. If the multiset
     * contains fewer than this number of occurrences to begin with, all occurrences will be removed.
     * Note that if {@code occurrences == 1}, this is functionally equivalent to the call {@code
     * remove(element)}.
     *
     * @param element the element to conditionally remove occurrences of
     * @param occurrencesToRemove the number of occurrences of the element to remove. May be zero, in which
     *     case no change will be made.
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code occurrences} is negative
     * @see #removeAndGetCount(Object, int)
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
     * Removes a number of occurrences of an element from this multiset and returns the new count of the element.
     *
     * This method removes the specified number of occurrences of the element from the multiset and then returns the new count of the element.
     * If the element does not exist in the multiset, it will return 0.
     * If the number of occurrences to remove is greater than the current count of the element, all occurrences of the element will be removed.
     *
     * @param element The element to remove occurrences of. This can be any instance of Object.
     * @param occurrences The number of occurrences of the element to remove. Must be a non-negative integer.
     * @return The new count of the element in the multiset after the operation. If the element does not exist, it will return 0.
     * @throws IllegalArgumentException if {@code occurrences} is negative.
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
        }

        return count.value();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This method ignores how often any element might appear in {@code c}, and only cares whether an element appears at all.
     *
     * <p>This method refines {@link Collection#removeAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being {@code null} or of the wrong type.
     * @see #removeAllOccurrences(Collection)
     * @deprecated replaced by {@code removeAllOccurrence(Collection)}
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
                result = remove(e);
            } else {
                remove(e);
            }
        }

        return result;
    }

    /**
     * Removes the specified number of occurrences of each element in the provided collection from this multiset.
     *
     * This method iterates over the given collection and removes the specified number of occurrences of each element from this multiset.
     * If the multiset contains fewer than the specified number of occurrences of an element to begin with, all occurrences will be removed.
     *
     * @param c The collection whose elements are to be removed from this multiset.
     * @param occurrencesToRemove The number of occurrences to remove for each element in the collection. Must be a non-negative integer.
     * @return {@code true} if this multiset changed as a result of the call.
     * @throws IllegalArgumentException if {@code occurrencesToRemove} is negative.
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
     *
     * This method removes all occurrences of the specified element from the multiset.
     * If the element is not present in the multiset, the method will not modify the multiset and will return 0.
     *
     * @param e The element whose occurrences are to be removed from this multiset. This can be any instance of Object.
     * @return The count of the element before the operation. If the element does not exist in the multiset, it will return 0.
     */
    public int removeAllOccurrences(final Object e) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final MutableInt count = backingMap.remove(e);

        return count == null ? 0 : count.value();
    }

    /**
     * Removes all occurrences of the elements in the specified collection from this multiset.
     *
     * This method iterates over the given collection and removes all occurrences of each element from this multiset.
     * If an element from the collection is not present in the multiset, the multiset remains unchanged for that particular element.
     *
     * @param c The collection whose elements are to be removed from this multiset.
     * @return {@code true} if this multiset changed as a result of the call.
     */
    public boolean removeAllOccurrences(final Collection<?> c) {
        return removeAll(c);
    }

    /**
     * Removes all occurrences of elements from this multiset that satisfy the given predicate.
     *
     * This method iterates over the multiset and applies the predicate to each element. If the predicate returns {@code true},
     * all occurrences of that element are removed from the multiset.
     *
     * @param predicate The predicate to apply to each element in the multiset. This can be any instance of Predicate.
     * @return {@code true} if any elements were removed from the multiset as a result of this call.
     * @throws IllegalArgumentException if the provided predicate is {@code null}.
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
     * Removes all occurrences of elements from this multiset that satisfy the given predicate.
     *
     * This method iterates over the multiset and applies the predicate to each element. If the predicate returns {@code true},
     * all occurrences of that element are removed from the multiset.
     *
     * @param predicate The predicate to apply to each element in the multiset. This can be any instance of ObjIntPredicate.
     * @return {@code true} if any elements were removed from the multiset as a result of this call.
     * @throws IllegalArgumentException if the provided predicate is {@code null}.
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
     *
     * This method iterates over the multiset and applies the provided function to each element and its count.
     * The function should return the new count for the element. If the function returns zero or a negative number,
     * the element will be removed from the multiset.
     *
     * @param function The function to apply to each element and its count in the multiset.
     *                 This function should return the new count for the element.
     *                 This can be any instance of ObjIntFunction.
     * @throws IllegalArgumentException if the provided function is {@code null}.
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
     * Computes the count of the specified element in this multiset if it is not already present.
     *
     * This method applies the provided mapping function to the specified element and sets its count to the result,
     * if the element is not already present in the multiset. If the element is present, this method returns its current count.
     * If the mapping function returns a value greater than 0, the element is added to the multiset with that count.
     * <br />
     * <br />
     * The implementation is equivalent to performing the following steps for this Multiset:
     *
     * <pre>
     * final int oldValue = get(e);
     *
     * if (oldValue > 0) {
     *     return oldValue;
     * }
     *
     * final int newValue = mappingFunction.apply(e);
     *
     * if (newValue > 0) {
     *     set(e, newValue);
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param e The element whose count is to be computed. This can be any instance of the generic type E.
     * @param mappingFunction The function to compute the count of the element. This can be any instance of ToIntFunction.
     * @return The new count of the element in the multiset after the operation. If the element does not exist, it will return the result of the mapping function.
     * @throws IllegalArgumentException if the provided mapping function is {@code null}.
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
     * Computes the count of the specified element in this multiset if it is already present.
     *
     * This method applies the provided remapping function to the specified element and its current count,
     * if the element is already present in the multiset. If the element is not present, this method returns 0.
     * If the remapping function returns a value greater than 0, the count of the element in the multiset is updated to that value.
     * If the remapping function returns 0 or a negative value, the element is removed from the multiset.
     *
     * <br />
     * <br />
     * The implementation is equivalent to performing the following steps for this Multiset:
     *
     * <pre>
     * final int oldValue = get(e);
     *
     * if (oldValue == 0) {
     *     return oldValue;
     * }
     *
     * final int newValue = remappingFunction.apply(e, oldValue);
     *
     * if (newValue > 0) {
     *     set(e, newValue);
     * } else {
     *     remove(e);
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param e The element whose count is to be computed. This can be any instance of the generic type E.
     * @param remappingFunction The function to compute the new count of the element. This can be any instance of ObjIntFunction.
     * @return The new count of the element in the multiset after the operation. If the element does not exist, it will return 0.
     * @throws IllegalArgumentException if the provided remapping function is {@code null}.
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
            remove(e);
        }

        return newValue;
    }

    /**
     * Applies the provided remapping function to the specified element and its current count in this multiset.
     *
     * This method applies the provided remapping function to the specified element and its current count.
     * The function should return the new count for the element. If the function returns zero or a negative number,
     * the element will be removed from the multiset.
     *
     * The implementation is equivalent to performing the following steps for this Multiset:
     *
     * <pre>
     * final int oldValue = get(key);
     * final int newValue = remappingFunction.apply(key, oldValue);
     *
     * if (newValue > 0) {
     *     set(key, newValue);
     * } else {
     *     if (oldValue > 0) {
     *         remove(key);
     *     }
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param key The element whose count is to be computed. This can be any instance of the generic type E.
     * @param remappingFunction The function to compute the new count of the element. This can be any instance of ObjIntFunction.
     * @return The new count of the element in the multiset after the operation.
     * @throws IllegalArgumentException if the provided remapping function is {@code null}.
     */
    public int compute(final E key, final ObjIntFunction<? super E, Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = getCount(key);
        final int newValue = remappingFunction.apply(key, oldValue);

        if (newValue > 0) {
            setCount(key, newValue);
        } else {
            if (oldValue > 0) {
                remove(key);
            }
        }

        return newValue;
    }

    /**
     * Merges the specified value with the current count of the specified element in this multiset.
     *
     * This method applies the provided remapping function to the current count of the specified element and the given value,
     * if the element is already present in the multiset. If the element is not present, the value is used as the new count.
     * If the remapping function returns a value greater than 0, the count of the element in the multiset is updated to that value.
     * If the remapping function returns 0 or a negative value, the element is removed from the multiset.
     *
     * The implementation is equivalent to performing the following steps for this Multiset:
     *
     * <pre>
     * int oldValue = get(key);
     * int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);
     *
     * if (newValue > 0) {
     *     set(key, newValue);
     * } else {
     *     if (oldValue > 0) {
     *         remove(key);
     *     }
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param key The element whose count is to be computed. This can be any instance of the generic type E.
     * @param value The value to be merged with the current count of the element.
     * @param remappingFunction The function to compute the new count of the element. This can be any instance of IntBiFunction.
     * @return The new count of the element in the multiset after the operation.
     * @throws IllegalArgumentException if the provided remapping function is {@code null} or if the provided value is {@code null}.
     */
    public int merge(final E key, final int value, final IntBiFunction<Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = getCount(key);
        final int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);

        if (newValue > 0) {
            setCount(key, newValue);
        } else {
            if (oldValue > 0) {
                remove(key);
            }
        }

        return newValue;
    }

    // Comparison and hashing

    // Refined Collection Methods

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This method ignores how often any element might appear in {@code c}, and only
     * cares whether an element appears at all.
     *
     * <p>This method refines {@link Collection#retainAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being {@code null} or of the wrong type.
     *
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
     * Determines whether this multiset contains the specified element.
     *
     * <p>This method refines {@link Collection#contains} to further specify that it <b>may not</b>
     * throw an exception in response to {@code element} being {@code null} or of the wrong type.
     *
     * @param valueToFind the element to check for
     * @return {@code true} if this multiset contains at least one occurrence of the element
     */
    @Override
    public boolean contains(final Object valueToFind) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(valueToFind);
    }

    /**
     * Returns {@code true} if this multiset contains at least one occurrence of each element in the
     * specified collection.
     *
     * <p>This method refines {@link Collection#containsAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being {@code null} or of the wrong type.
     *
     * <p><b>Note:</b> this method does not take into account the occurrence count of an element in
     * the two collections; it may still return {@code true} even if {@code elements} contains several
     * occurrences of an element and this multiset contains only one. This is no different from any
     * other collection type like {@link List}, but it may be unexpected to the user of a multiset.
     *
     * @param elements the collection of elements to be checked for containment in this multiset
     * @return {@code true} if this multiset contains at least one occurrence of each element
     *     contained in {@code elements}
     */
    @Override
    public boolean containsAll(final Collection<?> elements) {
        if (N.isEmpty(elements)) {
            return true;
        }

        return backingMap.keySet().containsAll(elements);
    }

    // It won't work.
    //    public Multiset<T> synchronized() {
    //        return new Multiset<>(Collections.synchronizedMap(valueMap));
    //    }

    // Query Operations

    // Comparison and hashing

    // Refined Collection Methods

    /**
     * Returns the set of distinct elements contained in this multiset. The element set is backed by
     * the same data as the multiset, so any change to the multiset is immediately reflected in the returned {@code Set}.
     * The order of the elements in the element set is unspecified.
     *
     *
     * @return a view of the set of distinct elements in this multiset
     */
    public Set<E> elementSet() {
        return backingMap.keySet();
    }

    private transient Set<Entry<E>> entrySet; // NOSONAR

    /**
     * Returns a view of the contents of this multiset, grouped into {@code Multiset.Entry} instances,
     * each providing an element of the multiset and the count of that element. This set contains
     * exactly one entry for each distinct element in the multiset (thus it always has the same size
     * as the {@link #elementSet}). The order of the elements in the element set is unspecified.
     *
     * <p>The entry set is backed by the same data as the multiset, so any change to the multiset is immediately reflected in the returned {@code Set}.
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
     * Returns an unmodifiable iterator over the elements in this collection.
     * There are no guarantees concerning the order in which the elements are returned(unless this collection is an instance of some class that provides a guarantee).
     *
     * <p>Elements that occur multiple times in the multiset will appear multiple times in this
     * iterator, though not necessarily sequentially.
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

    // TODO
    //    @Override
    //    public Spliterator<E> spliterator() {
    //        return Multisets.spliteratorImpl(this);
    //    }

    /**
     * Returns the total number of all occurrences of all elements in this multiset.
     *
     * <p>
     * <b>Note:</b> this method does not return the number of <i>distinct elements</i> in the multiset, which is given by {@code countOfDistinctElements()} or {@code elementSet().size()}.
     * </p>
     * @return
     * @throws ArithmeticException if the total number of all occurrences of all elements overflows an int
     * @see #countOfDistinctElements()
     * @see #sumOfOccurrences()
     */
    @Override
    public int size() throws ArithmeticException {
        return backingMap.isEmpty() ? 0 : Numbers.toIntExact(sumOfOccurrences());
    }

    /**
     * Returns the count of distinct elements in this multiset.
     *
     * This method counts the number of unique elements in the multiset. It does not consider the number of occurrences of each element.
     * For example, if the multiset contains [1, 1, 2, 2, 2, 3], this method will return 3, because there are three distinct elements: 1, 2, and 3.
     *
     * @return The count of distinct elements in the multiset.
     */
    @Beta
    public int countOfDistinctElements() {
        return backingMap.size();
    }

    /**
     * Checks if this multiset is empty.
     *
     * This method returns {@code true} if this multiset contains no elements.
     *
     * @return {@code true} if the multiset contains no elements, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Clears all elements from this multiset.
     *
     * This method removes all elements from the multiset, effectively making it empty.
     * The count of all elements in the multiset will be zero after this operation.
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
     * This method returns an array containing all the elements in this multiset.
     * Elements that occur multiple times in the multiset will appear multiple times in the array.
     * The order of elements in the array is unspecified.
     *
     * @return An array containing all the elements in this multiset.
     */
    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    /**
     * Converts the multiset to an array of a specific type.
     *
     * This method returns an array containing all the elements in this multiset.
     * Elements that occur multiple times in the multiset will appear multiple times in the array.
     * The order of elements in the array is unspecified.
     *
     * @param <T> the runtime type of the array to contain the multiset
     * @param a the array into which the elements of this multiset are to be stored, if it is big enough;
     *              otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return An array containing all the elements in this multiset.
     * @throws IllegalArgumentException if the provided array is {@code null}.
     */
    @Override
    public <T> T[] toArray(final T[] a) throws IllegalArgumentException {
        N.checkArgNotNull(a, "The specified array can't be null");

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
     * Converts the multiset to a map.
     *
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     *
     * @return A map with the elements of this multiset as keys and their counts as values.
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
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The type of the map is determined by the provided supplier function.
     *
     * @param <M> the type of the map to be returned
     * @param supplier a function that generates a new instance of the desired map type
     * @return A map with the elements of this multiset as keys and their counts as values.
     */
    public <M extends Map<E, Integer>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(backingMap.size());

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    /**
     * Converts the multiset to a map sorted by occurrences.
     *
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted in ascending order by the count of the elements.
     *
     * @return A map with the elements of this multiset as keys and their counts as values, sorted by the counts.
     */
    @SuppressWarnings("rawtypes")
    public Map<E, Integer> toMapSortedByOccurrences() {
        return toMapSortedBy((Comparator) cmpByCount);
    }

    /**
     * Converts the multiset to a map sorted by occurrences using a custom comparator.
     *
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted by the counts of the elements using the provided comparator.
     *
     * @param cmp The comparator to be used for sorting the counts of the elements.
     * @return A map with the elements of this multiset as keys and their counts as values, sorted by the counts using the provided comparator.
     */
    public Map<E, Integer> toMapSortedByOccurrences(final Comparator<? super Integer> cmp) {
        return toMapSortedBy((o1, o2) -> cmp.compare(o1.getValue().value(), o2.getValue().value()));
    }

    /**
     * Converts the multiset to a map sorted by keys.
     *
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted by the keys of the elements using the provided comparator.
     *
     * @param cmp The comparator to be used for sorting the keys of the elements.
     * @return A map with the elements of this multiset as keys and their counts as values, sorted by the keys using the provided comparator.
     */
    public Map<E, Integer> toMapSortedByKey(final Comparator<? super E> cmp) {
        return toMapSortedBy(Comparators.comparingByKey(cmp));
    }

    /**
     * Converts the multiset to a map sorted by a custom comparator.
     *
     * This method returns a map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The map is sorted according to the order induced by the provided comparator.
     *
     * @param cmp The comparator to be used for sorting the entries of the map. The comparator should compare Map.Entry objects.
     * @return A map with the elements of this multiset as keys and their counts as values, sorted according to the provided comparator.
     */
    Map<E, Integer> toMapSortedBy(final Comparator<Map.Entry<E, MutableInt>> cmp) {
        if (N.isEmpty(backingMap)) {
            return new LinkedHashMap<>();
        }

        final int distinctElementSize = backingMap.size();
        final Map.Entry<E, MutableInt>[] entries = backingMap.entrySet().toArray(new Map.Entry[distinctElementSize]);

        Arrays.sort(entries, cmp);

        final Map<E, Integer> sortedValues = N.newLinkedHashMap(distinctElementSize);

        for (final Map.Entry<E, MutableInt> entry : entries) {
            sortedValues.put(entry.getKey(), entry.getValue().value());
        }

        return sortedValues;
    }

    /**
     * Returns an unmodifiable map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     *
     * @return An immutable map with the elements of this multiset as keys and their counts as values.
     */
    public ImmutableMap<E, Integer> toImmutableMap() {
        return ImmutableMap.wrap(toMap());
    }

    /**
     * Returns an unmodifiable map where the keys are the elements in this multiset and the values are their corresponding counts.
     * Elements that occur multiple times in the multiset will appear only once in the map, with their count as the value.
     * The type of the map is determined by the provided supplier function.
     *
     * @param mapSupplier A function that generates a new instance of the desired map type.
     * @return An immutable map with the elements of this multiset as keys and their counts as values.
     */
    public ImmutableMap<E, Integer> toImmutableMap(final IntFunction<? extends Map<E, Integer>> mapSupplier) {
        return ImmutableMap.wrap(toMap(mapSupplier));
    }

    // It won't work.
    //    public Multiset<T> synchronized() {
    //        return new Multiset<>(Collections.synchronizedMap(valueMap));
    //    }

    // Query Operations

    /**
     * {@inheritDoc}
     *
     * <p>Elements that occur multiple times in the multiset will be passed to the {@code Consumer}
     * correspondingly many times, though not necessarily sequentially.
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
     * @param action The action to be performed for each distinct element in the multiset and its count. This can be any instance of ObjIntConsumer.
     * @throws IllegalArgumentException if the provided action is {@code null}.
     */
    public void forEach(final ObjIntConsumer<? super E> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        for (final Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue().value());
        }
    }

    //    /**
    //     *
    //     * @param <X>
    //     * @param action
    //     * @throws X
    //     */
    //    @Beta
    //    public <X extends Exception> void foreach(final Throwables.Consumer<? super E, X> action) throws X {
    //        N.checkArgNotNull(action);
    //
    //        final Iterator<E> iter = iterator();
    //
    //        while (iter.hasNext()) {
    //            action.accept(iter.next());
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <X>
    //     * @param action
    //     * @throws X
    //     */
    //    @Beta
    //    public <X extends Exception> void foreach(final Throwables.ObjIntConsumer<? super E, X> action) throws X {
    //        N.checkArgNotNull(action);
    //
    //        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
    //            action.accept(entry.getKey(), entry.getValue().value());
    //        }
    //    }

    /**
     * Returns an (unmodifiable) {@code Stream} with elements from the {@code Multiset}.
     * Elements that occur multiple times in the multiset will appear multiple times in this {@code Stream}, though not necessarily sequentially
     *
     * @return
     * @see #iterator()
     */
    @Beta
    public Stream<E> elements() {
        return Stream.of(iterator());
    }

    /**
     * Returns an (unmodifiable) Stream of entries in this multiset.
     *
     * Each entry in the Stream represents a distinct element in the multiset and its count.
     * The order of the entries in the Stream is unspecified.
     *
     * @return A Stream of entries representing the data of this multiset.
     */
    @Beta
    public Stream<Multiset.Entry<E>> entries() {
        return Stream.of(entrySet());
    }

    /**
     * Applies the provided function to this multiset and returns the result.
     *
     * @param <R> The type of the result returned by the function.
     * @param <X> The type of the exception that can be thrown by the function.
     * @param func The function to be applied to this multiset. This can be any instance of Throwables.Function.
     * @return The result of applying the provided function to this multiset.
     * @throws X if the provided function throws an exception of type X.
     */
    public <R, X extends Exception> R apply(final Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return func.apply(this);
    }

    /**
     * Applies the provided function to this multiset and returns the result, if the multiset is not empty.
     *
     * This method applies the provided function to this multiset and returns the result wrapped in an Optional.
     * If the multiset is empty, it returns an empty Optional.
     *
     * @param <R> The type of the result returned by the function.
     * @param <X> The type of the exception that can be thrown by the function.
     * @param func The function to be applied to this multiset. This can be any instance of Throwables.Function.
     * @return An Optional containing the result of applying the provided function to this multiset, or an empty Optional if the multiset is empty.
     * @throws X if the provided function throws an exception of type X.
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     * Applies the provided consumer function to this multiset.
     *
     * @param <X> The type of the exception that can be thrown by the consumer function.
     * @param action The consumer function to be applied to this multiset. This can be any instance of Throwables.Consumer.
     * @throws X if the provided consumer function throws an exception of type X.
     */
    public <X extends Exception> void accept(final Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        action.accept(this);
    }

    /**
     * Applies the provided consumer function to this multiset if it is not empty.
     *
     * @param <X> The type of the exception that can be thrown by the consumer function.
     * @param action The consumer function to be applied to this multiset. This can be any instance of Throwables.Consumer.
     * @return An instance of OrElse which can be used to perform some other operation if the Multiset is empty.
     * @throws X if the provided consumer function throws an exception of type X.
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        return If.is(!backingMap.isEmpty()).then(this, action);
    }

    /**
     * Returns the hash code value for this multiset.
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
     * @return a string representation of this Multiset.
     */
    @Override
    public String toString() {
        return backingMap.toString();
    }

    /**
     *
     * @param occurrences
     */
    private static int checkOccurrences(final int occurrences) {
        if (occurrences < 0) {
            throw new IllegalArgumentException("The specified 'occurrences' can not be negative");
        }

        return occurrences;
    }

    /**
     * An unmodifiable element-count pair for a multiset. The {@link Multiset#entrySet} method returns
     * a view of the multiset whose elements are of this class. A multiset implementation may return
     * Entry instances that are either live "read-through" views to the Multiset, or immutable
     * snapshots. Note that this type is unrelated to the similarly-named type {@code Map.Entry}.
     *
     */
    public interface Entry<E> {

        /**
         * Returns the multiset element corresponding to this entry. Multiple calls to this method
         * always return the same instance.
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
         * @return the count of the element; never negative
         */
        int count();

        /**
         * {@inheritDoc}
         *
         * <p>Returns {@code true} if the given object is also a multiset entry and the two entries
         * represent the same element and count. That is, two entries {@code a} and {@code b} are equal
         * if:
         *
         * <pre>{@code
         * Objects.equal(a.getElement(), b.getElement())
         *     && a.getCount() == b.getCount()
         * }</pre>
         */
        @Override
        // TODO(kevinb): check this wrt TreeMultiset?
        boolean equals(Object o);

        /**
         * {@inheritDoc}
         *
         * <p>The hash code of a multiset entry for element {@code element} and count {@code count} is
         * defined as:
         *
         * <pre>{@code
         * ((element == null) ? 0 : element.hashCode()) ^ count
         * }</pre>
         */
        @Override
        int hashCode();

        /**
         * Returns the canonical string representation of this entry, defined as follows. If the count
         * for this entry is one, this is simply the string representation of the corresponding element.
         * Otherwise, it is the string representation of the element, followed by the three characters
         * {@code " x "} (space, letter x, space), followed by the count.
         *
         * @return
         */
        @Override
        String toString();
    }

    @SuppressWarnings("ClassCanBeRecord")
    static final class ImmutableEntry<E> implements Entry<E> { // NOSONAR
        private final E element;
        private final int count;

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
         */
        @Override
        public String toString() {
            final String text = String.valueOf(element);

            return (count == 1) ? text : (text + " x " + count);
        }
    }
}
