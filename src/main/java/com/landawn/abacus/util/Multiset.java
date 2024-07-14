/*
 * Copyright (c) 2015, Haiyang Li.
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
import java.util.Map.Entry;
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
import com.landawn.abacus.util.stream.EntryStream;
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
 * <p>{@code Multiset} refines the specifications of several methods from {@code Collection}. It
 * also defines an additional query operation, {@link #count}, which returns the count of an
 * element. There are five new bulk-modification operations, for example {@link #add(Object, int)},
 * to add or remove multiple occurrences of an element at once, or to set the count of an element to
 * a specific value. These modification operations are optional, but implementations which support
 * the standard collection operations {@link #add(Object)} or {@link #remove(Object)} are encouraged
 * to implement the related methods as well. Finally, two collection views are provided: {@link
 * #elementSet} contains the distinct elements of the multiset "with duplicates collapsed", and
 * {@link #entrySet} is similar but contains {@link Entry Multiset.Entry} instances, each providing
 * both a distinct element and the count of that element.
 *
 * <p>In addition to these required methods, implementations of {@code Multiset} are expected to
 * provide two {@code static} creation methods: {@code create()}, returning an empty multiset, and
 * {@code create(Iterable<? extends E>)}, returning a multiset containing the given initial
 * elements. This is simply a refinement of {@code Collection}'s constructor recommendations,
 * reflecting the new developments of Java 5.
 *
 * <p>As with other collection types, the modification operations are optional, and should throw
 * {@link UnsupportedOperationException} when they are not implemented. Most implementations should
 * support either all add operations or none of them, all removal operations or none of them, and if
 * and only if all of these are supported, the {@code setCount} methods as well.
 *
 * <p>A multiset uses {@link Object#equals} to determine whether two instances should be considered
 * "the same," <i>unless specified otherwise</i> by the implementation.
 *
 * <p><b>Warning:</b> as with normal {@link Set}s, it is almost always a bad idea to modify an
 * element (in a way that affects its {@link Object#equals} behavior) while it is contained in a
 * multiset. Undefined behavior and bugs will result.
 *
 * <h3>Implementations</h3>
 *
 * <ul>
 *   <li>{@link ImmutableMultiset}
 *   <li>{@link ImmutableSortedMultiset}
 *   <li>{@link HashMultiset}
 *   <li>{@link LinkedHashMultiset}
 *   <li>{@link TreeMultiset}
 *   <li>{@link EnumMultiset}
 *   <li>{@link ConcurrentHashMultiset}
 * </ul>
 *
 * <p>If your values may be zero, negative, or outside the range of an int, you may wish to use
 * {@link com.google.common.util.concurrent.AtomicLongMap} instead. Note, however, that unlike
 * {@code Multiset}, {@code AtomicLongMap} does not automatically remove zeros.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multiset">{@code Multiset}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
public final class Multiset<E> implements Collection<E> {

    private static final Comparator<Map.Entry<?, MutableInt>> cmpByCount = (a, b) -> N.compare(a.getValue().value(), b.getValue().value());

    private final Supplier<Map<E, MutableInt>> backingMapSupplier;

    private final Map<E, MutableInt> backingMap;

    /**
     *
     */
    public Multiset() {
        this(HashMap.class);
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public Multiset(int initialCapacity) {
        this.backingMapSupplier = Suppliers.ofMap();
        this.backingMap = N.newHashMap(initialCapacity);
    }

    /**
     *
     *
     * @param c
     */
    public Multiset(final Collection<? extends E> c) {
        this((c == null || c instanceof Set) ? N.size(c) : N.size(c) / 2);

        addAll(c);
    }

    /**
     *
     *
     * @param valueMapType
     */
    @SuppressWarnings("rawtypes")
    public Multiset(final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(valueMapType));
    }

    /**
     *
     *
     * @param mapSupplier
     */
    @SuppressWarnings("rawtypes")
    public Multiset(final Supplier<? extends Map<? extends E, ?>> mapSupplier) {
        this.backingMapSupplier = (Supplier) mapSupplier;
        this.backingMap = this.backingMapSupplier.get();
    }

    @SuppressWarnings("rawtypes")
    Multiset(final Map<E, MutableInt> valueMap) {
        this.backingMapSupplier = (Supplier) Suppliers.ofMap(valueMap.getClass());
        this.backingMap = valueMap;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Multiset<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return new Multiset<>();
        }

        final Multiset<T> multiset = new Multiset<>(N.<T, MutableInt> newHashMap(a.length));

        for (T e : a) {
            multiset.add(e);
        }

        return multiset;
    }

    /**
     *
     * @param <T>
     * @param coll
     * @return
     */
    public static <T> Multiset<T> create(final Collection<? extends T> coll) {
        return new Multiset<>(coll);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
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
     * Returns the occurrences of the specified element.
     *
     * @param e
     * @return
     * @see #getCount(Object)
     */
    public int occurrencesOf(final Object e) {
        return getCount(e);
    }

    /**
     *
     *
     * @return
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
     *
     *
     * @return
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
     * All min occurrences.
     *
     * @return
     */
    public Optional<Pair<Integer, List<E>>> allMinOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        int min = Integer.MAX_VALUE;

        for (MutableInt e : backingMap.values()) {
            if (e.value() < min) {
                min = e.value();
            }
        }

        final List<E> res = new ArrayList<>();

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            if (entry.getValue().value() == min) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(min, res));
    }

    /**
     * All max occurrences.
     *
     * @return
     */
    public Optional<Pair<Integer, List<E>>> allMaxOccurrences() {
        if (backingMap.isEmpty()) {
            return Optional.empty();
        }

        int max = Integer.MIN_VALUE;

        for (MutableInt e : backingMap.values()) {
            if (e.value() > max) {
                max = e.value();
            }
        }

        final List<E> res = new ArrayList<>();

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            if (entry.getValue().value() == max) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(max, res));
    }

    /**
     * Sum of occurrences.
     *
     * @return
     * @see #size()
     */
    public long sumOfOccurrences() {
        if (backingMap.isEmpty()) {
            return 0;
        }

        long sum = 0;

        for (MutableInt count : backingMap.values()) {
            sum = Numbers.addExact(sum, count.value());
        }

        return sum;
    }

    /**
     * Average of occurrences.
     *
     * @return
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
     * <p><b>Note:</b> the utility method {@link Iterables#frequency} generalizes this operation; it
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
     * <p><b>Note:</b> the utility method {@link Iterables#frequency} generalizes this operation; it
     * correctly delegates to this method when dealing with a multiset, but it can also accept any
     * other iterable type.
     *
     * @param element the element to count occurrences of
     * @return the number of occurrences of the element in this multiset; possibly zero but never
     *     negative
     */
    public int getCount(final Object element) {
        final MutableInt count = backingMap.get(element);

        return count == null ? 0 : count.value();
    }

    // Bulk Operations

    /**
     * Adds or removes the necessary occurrences of an element such that the element attains the
     * desired count.
     *
     * @param element the element to add or remove occurrences of; may be null only if explicitly
     *     allowed by the implementation
     * @param occurrences the desired count of the element in this multiset
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws NullPointerException if {@code element} is null and this implementation does not permit
     *     null elements. Note that if {@code count} is zero, the implementor may optionally return
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
     * @param element the element to conditionally set the count of; may be null only if explicitly
     *     allowed by the implementation
     * @param oldOccurrences
     * @param newOccurrences the desired count of the element in this multiset
     * @return {@code true} if the condition for modification was met. This implies that the multiset
     *     was indeed modified, unless {@code oldCount == newCount}.
     * @throws IllegalArgumentException if {@code oldCount} or {@code newCount} is negative
     * @throws NullPointerException if {@code element} is null and the implementation does not permit
     *     null elements. Note that if {@code oldCount} and {@code newCount} are both zero, the
     *     implementor may optionally return {@code true} instead.
     */
    public boolean setCount(final E element, int oldOccurrences, int newOccurrences) {
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
     * @param element the element to add one occurrence of; may be null only if explicitly allowed by
     *     the implementation
     * @return {@code true} always, since this call is required to modify the multiset, unlike other
     *     {@link Collection} types
     * @throws NullPointerException if {@code element} is null and this implementation does not permit
     *     null elements
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
     * @param element the element to add occurrences of; may be null only if explicitly allowed by the
     *     implementation
     * @param occurrencesToAdd the number of occurrences of the element to add. May be zero, in which case
     *     no change will be made.
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code occurrences} is negative, or if this operation would
     *     result in more than {@link Integer#MAX_VALUE} occurrences of the element
     * @throws NullPointerException if {@code element} is null and this implementation does not permit
     *     null elements. Note that if {@code occurrences} is zero, the implementation may opt to
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
     *
     * @param element
     * @param occurrences
     * @return the count of the element after the operation.
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
            }
        } else {
            count.add(occurrences);
        }

        return count == null ? occurrences : count.value();
    }

    /**
     *
     *
     * @param c
     * @return
     */
    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return addAll(c, 1);
    }

    /**
     *
     *
     * @param c
     * @param occurrencesToAdd
     * @return {@code true} if this {@code Multiset} is modified by this operation.
     */
    @Beta
    public boolean addAll(final Collection<? extends E> c, final int occurrencesToAdd) {
        checkOccurrences(occurrencesToAdd);

        if (N.isEmpty(c) || occurrencesToAdd == 0) {
            return false;
        }

        for (E e : c) {
            add(e, occurrencesToAdd);
        }

        return true;
    }

    /**
     * Removes a <i>single</i> occurrence of the specified element from this multiset, if present.
     *
     * <p>This method refines {@link Collection#remove} to further specify that it <b>may not</b>
     * throw an exception in response to {@code element} being null or of the wrong type.
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

        final MutableInt count = backingMap.get(element);

        if (count == null) {
            return 0;
        }

        int oldCount = count.value();

        count.subtract(occurrencesToRemove);

        if (count.value() <= 0) {
            backingMap.remove(element);
        }

        return oldCount;
    }

    /**
     *
     * @param element
     * @param occurrences
     * @return the count of the element after the operation; possibly zero
     * @see #remove(Object, int)
     */
    @Beta
    public int removeAndGetCount(final Object element, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = backingMap.get(element);

        if (count == null) {
            return 0;
        }

        count.subtract(occurrences);

        if (count.value() <= 0) {
            backingMap.remove(element);
        }

        return count.value();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This method ignores how often any element might appear in {@code c}, and only
     * cares whether or not an element appears at all. If you wish to remove one occurrence in this
     * multiset for every occurrence in {@code c}, see {@link Multisets#removeOccurrences(Multiset,
     * Multiset)}.
     *
     * <p>This method refines {@link Collection#removeAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being null or of the wrong type.
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

        for (Object e : c) {
            if (!result) {
                result = remove(e);
            } else {
                remove(e);
            }
        }

        return result;
    }

    /**
     *
     * @param c
     * @param occurrencesToRemove
     * @return {@code true} if this {@code Multiset} is modified by this operation.
     */
    public boolean removeAll(final Collection<?> c, final int occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        if (N.isEmpty(c) || occurrencesToRemove == 0) {
            return false;
        }

        boolean result = false;

        for (Object e : c) {
            if (!result) {
                result = remove(e, occurrencesToRemove) > 0;
            } else {
                remove(e, occurrencesToRemove);
            }
        }

        return result;
    }

    /**
     * Removes the all occurrences.
     *
     * @param e
     * @return the count of the element before the operation; possibly zero
     * @see #removeAll(Collection)
     */
    public int removeAllOccurrences(final Object e) {
        final MutableInt count = backingMap.remove(e);

        return count == null ? 0 : count.value();
    }

    /**
     *
     * @param c
     * @return
     * @see #removeAll(Collection)
     */
    public boolean removeAllOccurrences(final Collection<?> c) {
        return removeAll(c);
    }

    /**
     * Removes the all occurrences if.
     *
     * @param predicate
     * @return
     */
    public boolean removeAllOccurrencesIf(final Predicate<? super E> predicate) {
        N.checkArgNotNull(predicate);

        Set<E> removingKeys = null;

        for (E key : this.backingMap.keySet()) {
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

        for (Object e : removingKeys) {
            backingMap.remove(e);
        }

        return true;
    }

    /**
     * Removes the all occurrences if.
     *
     * @param predicate
     * @return
     */
    public boolean removeAllOccurrencesIf(final ObjIntPredicate<? super E> predicate) {
        N.checkArgNotNull(predicate);

        Set<E> removingKeys = null;

        for (Map.Entry<E, MutableInt> entry : this.backingMap.entrySet()) {
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

        for (Object e : removingKeys) {
            backingMap.remove(e);
        }

        return true;
    }

    /**
     * The associated elements will be removed if zero or negative occurrences are returned by the specified <code>function</code>.
     *
     * @param function
     */
    public void updateAllOccurrences(final ObjIntFunction<? super E, Integer> function) {
        N.checkArgNotNull(function);

        List<E> keyToRemove = null;
        Integer newVal = null;

        for (Map.Entry<E, MutableInt> entry : this.backingMap.entrySet()) {
            newVal = function.apply(entry.getKey(), entry.getValue().value());

            if (newVal == null || newVal.intValue() <= 0) {
                if (keyToRemove == null) {
                    keyToRemove = new ArrayList<>();
                }

                keyToRemove.add(entry.getKey());
            } else {
                entry.getValue().setValue(newVal);
            }
        }

        if (N.notEmpty(keyToRemove)) {
            for (E key : keyToRemove) {
                backingMap.remove(key);
            }
        }
    }

    /**
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
     * @param e
     * @param mappingFunction
     * @return the new count after computation.
     * @throws IllegalArgumentException
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
     * @param e
     * @param remappingFunction
     * @return the new count after computation.
     * @throws IllegalArgumentException
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
     * @param key
     * @param remappingFunction
     * @return the new count after computation.
     * @throws IllegalArgumentException
     */
    public int compute(E key, ObjIntFunction<? super E, Integer> remappingFunction) throws IllegalArgumentException {
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
     * @param key
     * @param value
     * @param remappingFunction
     * @return the new count after computation.
     * @throws IllegalArgumentException
     */
    public int merge(final E key, final int value, final IntBiFunction<Integer> remappingFunction) throws IllegalArgumentException {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(value);

        int oldValue = getCount(key);
        int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);

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
     * cares whether or not an element appears at all. If you wish to remove one occurrence in this
     * multiset for every occurrence in {@code c}, see {@link Multisets#retainOccurrences(Multiset,
     * Multiset)}.
     *
     * <p>This method refines {@link Collection#retainAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being null or of the wrong type.
     *
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            boolean result = backingMap.size() > 0;
            clear();
            return result;
        }

        Set<E> others = null;

        for (E e : backingMap.keySet()) {
            if (!c.contains(e)) {
                if (others == null) {
                    others = N.newHashSet(backingMap.size());
                }

                others.add(e);
            }
        }

        return N.isEmpty(others) ? false : removeAllOccurrences(others);
    }

    /**
     * Determines whether this multiset contains the specified element.
     *
     * <p>This method refines {@link Collection#contains} to further specify that it <b>may not</b>
     * throw an exception in response to {@code element} being null or of the wrong type.
     *
     * @param element the element to check for
     * @return {@code true} if this multiset contains at least one occurrence of the element
     */
    @Override
    public boolean contains(final Object element) {
        return backingMap.containsKey(element);
    }

    /**
     * Returns {@code true} if this multiset contains at least one occurrence of each element in the
     * specified collection.
     *
     * <p>This method refines {@link Collection#containsAll} to further specify that it <b>may not</b>
     * throw an exception in response to any of {@code elements} being null or of the wrong type.
     *
     * <p><b>Note:</b> this method does not take into account the occurrence count of an element in
     * the two collections; it may still return {@code true} even if {@code elements} contains several
     * occurrences of an element and this multiset contains only one. This is no different than any
     * other collection type like {@link List}, but it may be unexpected to the user of a multiset.
     *
     * @param elements the collection of elements to be checked for containment in this multiset
     * @return {@code true} if this multiset contains at least one occurrence of each element
     *     contained in {@code elements}
     */
    @Override
    public boolean containsAll(Collection<?> elements) {
        if (N.isEmpty(elements)) {
            return true;
        }

        return backingMap.keySet().containsAll(elements);
    }

    // It won't work.
    //    public Multiset<T> synchronizedd() {
    //        return new Multiset<>(Collections.synchronizedMap(valueMap));
    //    }

    // Query Operations

    // Comparison and hashing

    // Refined Collection Methods

    /**
     * Returns the set of distinct elements contained in this multiset. The element set is backed by
     * the same data as the multiset, so any change to either is immediately reflected in the other.
     * The order of the elements in the element set is unspecified.
     *
     * <p>If the element set supports any removal operations, these necessarily cause <b>all</b>
     * occurrences of the removed element(s) to be removed from the multiset. Implementations are not
     * expected to support the add operations, although this is possible.
     *
     * <p>A common use for the element set is to find the number of distinct elements in the multiset:
     * {@code elementSet().size()}.
     *
     * @return a view of the set of distinct elements in this multiset
     */
    public Set<E> elementSet() {
        return ImmutableSet.wrap(backingMap.keySet());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Elements that occur multiple times in the multiset will appear multiple times in this
     * iterator, though not necessarily sequentially.
     */
    @Override
    public Iterator<E> iterator() {
        return new ObjIterator<>() {
            private Iterator<Map.Entry<E, MutableInt>> entryIter = null;
            private Map.Entry<E, MutableInt> entry = null;
            private E element = null;
            private int count = 0;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (entryIter == null) {
                    init();
                }

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

            private void init() {
                if (entryIter == null) {
                    entryIter = backingMap.entrySet().iterator();
                }
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
     * <p><b>Note:</b> this method does not return the number of <i>distinct elements</i> in the
     * multiset, which is given by {@code entrySet().size()}.
     *
     * @return
     * @throws ArithmeticException if the total number of all occurrences of all elements overflows an int
     * @see #sumOfOccurrences()
     */
    @Override
    public int size() throws ArithmeticException {
        return backingMap.isEmpty() ? 0 : Numbers.toIntExact(sumOfOccurrences());
    }

    /**
     *
     * @return the count of distinct elements.
     */
    @Beta
    public int countOfDistinctElements() {
        return backingMap.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        backingMap.clear();
    }

    //    public Set<Map.Entry<E, MutableInt>> entrySet() {
    //        return valueMap.entrySet();
    //    }

    /**
     *
     *
     * @return
     */
    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @return
     */
    @Override
    public <T> T[] toArray(T[] a) {
        final int size = size();
        final T[] ret = a == null || a.length < size ? N.newArray(a.getClass().getComponentType(), size) : a;

        int idx = 0;
        int occurrences = 0;

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            occurrences = entry.getValue().value();
            N.fill(ret, idx, idx + occurrences, entry.getKey());
            idx += occurrences;
        }

        return ret;
    }

    /**
     *
     *
     * @return
     */
    public Map<E, Integer> toMap() {
        final Map<E, Integer> result = Maps.newTargetMap(backingMap);

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    /**
     *
     * @param <M>
     * @param supplier
     * @return
     */
    public <M extends Map<E, Integer>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(backingMap.size());

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    /**
     * To map sorted by occurrences.
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public Map<E, Integer> toMapSortedByOccurrences() {
        return toMapSortedBy((Comparator) cmpByCount);
    }

    /**
     * To map sorted by occurrences.
     *
     * @param cmp
     * @return
     */
    public Map<E, Integer> toMapSortedByOccurrences(final Comparator<? super Integer> cmp) {
        return toMapSortedBy((o1, o2) -> cmp.compare(o1.getValue().value(), o2.getValue().value()));
    }

    /**
     * To map sorted by key.
     *
     * @param cmp
     * @return
     */
    public Map<E, Integer> toMapSortedByKey(final Comparator<? super E> cmp) {
        return toMapSortedBy(Comparators.<E, MutableInt> comparingByKey(cmp));
    }

    /**
     * To map sorted by.
     *
     * @param cmp
     * @return
     */
    Map<E, Integer> toMapSortedBy(final Comparator<Map.Entry<E, MutableInt>> cmp) {
        if (N.isEmpty(backingMap)) {
            return new LinkedHashMap<>();
        }

        final int distinctElementSize = backingMap.size();
        final Map.Entry<E, MutableInt>[] entries = backingMap.entrySet().toArray(new Map.Entry[distinctElementSize]);

        Arrays.sort(entries, cmp);

        final Map<E, Integer> sortedValues = N.newLinkedHashMap(distinctElementSize);

        for (Map.Entry<E, MutableInt> entry : entries) {
            sortedValues.put(entry.getKey(), entry.getValue().value());
        }

        return sortedValues;
    }

    /**
     * To immutable map.
     *
     * @return
     */
    public ImmutableMap<E, Integer> toImmutableMap() {
        return ImmutableMap.wrap(toMap());
    }

    /**
     * To immutable map.
     *
     * @param mapSupplier
     * @return
     */
    public ImmutableMap<E, Integer> toImmutableMap(final IntFunction<? extends Map<E, Integer>> mapSupplier) {
        return ImmutableMap.wrap(toMap(mapSupplier));
    }

    // It won't work.
    //    public Multiset<T> synchronizedd() {
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
    public void forEach(final Consumer<? super E> action) {
        N.checkArgNotNull(action);

        final Iterator<E> iter = iterator();

        while (iter.hasNext()) {
            action.accept(iter.next());
        }
    }

    /**
     * Runs the specified action for each distinct element in this multiset, and the number of
     * occurrences of that element. For some {@code Multiset} implementations, this may be more
     * efficient than iterating over the {@link #entrySet()} either explicitly or with {@code
     * entrySet().forEach(action)}.
     *
     * @param action
     * @since 21.0
     */
    public void forEach(final ObjIntConsumer<? super E> action) {
        N.checkArgNotNull(action);

        for (Map.Entry<E, MutableInt> entry : backingMap.entrySet()) {
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
     *
     * @return
     */
    public Stream<E> elements() {
        return Stream.of(iterator());
    }

    private static final com.landawn.abacus.util.function.Function<MutableInt, Integer> TO_INT = MutableInt::value;

    /**
     *
     *
     * @return
     */
    public EntryStream<E, Integer> entries() {
        return EntryStream.of(backingMap).mapValue(TO_INT);
    }

    /**
     *
     * @param <R>
     * @param <X>
     * @param func
     * @return
     * @throws X the e
     */
    public <R, X extends Exception> R apply(Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return func.apply(this);
    }

    /**
     * Apply if not empty.
     *
     * @param <R>
     * @param <X>
     * @param func
     * @return
     * @throws X the e
     */
    public <R, X extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super Multiset<E>, ? extends R, X> func) throws X {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <X>
     * @param action
     * @throws X the e
     */
    public <X extends Exception> void accept(Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        action.accept(this);
    }

    /**
     * Accept if not empty.
     *
     * @param <X>
     * @param action
     * @return
     * @throws X the e
     */
    public <X extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super Multiset<E>, X> action) throws X {
        return If.is(backingMap.size() > 0).then(this, action);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multiset && backingMap.equals(((Multiset<E>) obj).backingMap));
    }

    /**
     *
     *
     * @return
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
}
