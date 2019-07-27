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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * A collection that supports order-independent equality, like {@link Set}, but
 * may have duplicate elements.
 *
 * <p>Elements of a Multiset that are equal to one another are referred to as
 * <i>occurrences</i> of the same single element. The total number of
 * occurrences of an element in a Multiset is called the <i>count</i> of that
 * element (the terms "frequency" and "multiplicity" are equivalent, but not
 * used in this API). Since the count of an element is represented as an {@code
 * int}, a Multiset may never contain more than {@link MutableInt#MAX_VALUE}
 * occurrences of any one element.
 *
 * @param <T>
 *
 * @since 0.8
 *
 * @author Haiyang Li
 */
public final class Multiset<T> implements Iterable<T> {
    private static final Comparator<Map.Entry<?, MutableInt>> cmpByCount = new Comparator<Map.Entry<?, MutableInt>>() {
        @Override
        public int compare(Entry<?, MutableInt> a, Entry<?, MutableInt> b) {
            return N.compare(a.getValue().value(), b.getValue().value());
        }
    };

    final Supplier<Map<T, MutableInt>> mapSupplier;
    final Map<T, MutableInt> valueMap;

    public Multiset() {
        this(HashMap.class);
    }

    public Multiset(int initialCapacity) {
        this.mapSupplier = Suppliers.ofMap();
        this.valueMap = new HashMap<T, MutableInt>(initialCapacity);
    }

    public Multiset(final Collection<? extends T> c) {
        this();

        addAll(c);
    }

    @SuppressWarnings("rawtypes")
    public Multiset(final Class<? extends Map> valueMapType) {
        this(Maps.mapType2Supplier(valueMapType));
    }

    @SuppressWarnings("rawtypes")
    public Multiset(final Supplier<? extends Map<T, ?>> mapSupplier) {
        this.mapSupplier = (Supplier) mapSupplier;
        this.valueMap = this.mapSupplier.get();
    }

    /**
     *
     * @param valueMap The valueMap and this Multiset share the same data; any changes to one will appear in the other.
     */
    @Internal
    Multiset(final Map<T, MutableInt> valueMap) {
        this.mapSupplier = Maps.mapType2Supplier(valueMap.getClass());
        this.valueMap = valueMap;
    }

    @SafeVarargs
    public static <T> Multiset<T> of(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new Multiset<>();
        }

        final Multiset<T> multiset = new Multiset<>(new HashMap<T, MutableInt>(N.initHashCapacity(a.length)));

        for (T e : a) {
            multiset.add(e);
        }

        return multiset;
    }

    public static <T> Multiset<T> from(final Collection<? extends T> coll) {
        return new Multiset<>(coll);
    }

    public static <T> Multiset<T> from(final Map<? extends T, Integer> m) {
        if (N.isNullOrEmpty(m)) {
            return new Multiset<T>();
        }

        final Multiset<T> multiset = new Multiset<>(Maps.newTargetMap(m));

        multiset.setAll(m);

        return multiset;
    }

    /**
     *
     * @param e
     * @return the occurrences of the specified object. zero is returned if it's not in this set.
     */
    public int get(final Object e) {
        final MutableInt count = valueMap.get(e);

        return count == null ? 0 : count.value();
    }

    /**
     * 
     * @param e
     * @param defaultValue
     * @return the occurrences of the specified object. the specified defaultValue is returned if it's not in this set.
     */
    public int getOrDefault(final Object e, int defaultValue) {
        final MutableInt count = valueMap.get(e);

        return count == null ? defaultValue : count.value();
    }

    /**
     * The element will be removed if the specified count is 0.
     * 
     * @param e
     * @param occurrences
     * @return
     */
    public int getAndSet(final T e, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = valueMap.get(e);
        int result = count == null ? 0 : count.value();

        if (occurrences == 0) {
            if (count != null) {
                valueMap.remove(e);
            }
        } else {
            if (count == null) {
                valueMap.put(e, MutableInt.of(occurrences));
            } else {
                count.setValue(occurrences);
            }
        }

        return result;
    }

    /**
     * The element will be removed if the specified count is 0.
     * 
     * @param e
     * @param occurrences
     * @return
     */
    public int setAndGet(final T e, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = valueMap.get(e);

        if (occurrences == 0) {
            if (count != null) {
                valueMap.remove(e);
            }
        } else {
            if (count == null) {
                valueMap.put(e, MutableInt.of(occurrences));
            } else {
                count.setValue(occurrences);
            }
        }

        return occurrences;
    }

    /**
     * The element will be removed if the specified count is 0.
     *
     * @param e
     * @param occurrences
     * @return this Multiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0
     */
    public Multiset<T> set(final T e, final int occurrences) {
        checkOccurrences(occurrences);

        if (occurrences == 0) {
            valueMap.remove(e);
        } else {
            final MutableInt count = valueMap.get(e);

            if (count == null) {
                valueMap.put(e, MutableInt.of(occurrences));
            } else {
                count.setValue(occurrences);
            }
        }

        return this;
    }

    public Multiset<T> setAll(final Collection<? extends T> c, final int occurrences) {
        checkOccurrences(occurrences);

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                set(e, occurrences);
            }
        }

        return this;
    }

    /**
     * 
     * @param m
     * @return this Multiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public Multiset<T> setAll(final Map<? extends T, Integer> m) throws IllegalArgumentException {
        if (N.notNullOrEmpty(m)) {
            for (Map.Entry<? extends T, Integer> entry : m.entrySet()) {
                checkOccurrences(entry.getValue().intValue());
            }

            for (Map.Entry<? extends T, Integer> entry : m.entrySet()) {
                set(entry.getKey(), entry.getValue().intValue());
            }
        }

        return this;
    }

    /**
     * 
     * @param m
     * @return this Multiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public Multiset<T> setAll(final Multiset<? extends T> multiset) throws IllegalArgumentException {
        if (N.notNullOrEmpty(multiset)) {
            for (Map.Entry<? extends T, MutableInt> entry : multiset.valueMap.entrySet()) {
                set(entry.getKey(), entry.getValue().value());
            }
        }

        return this;
    }

    public int occurrencesOf(final Object e) {
        return get(e);
    }

    public Optional<Pair<T, Integer>> minOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<T, MutableInt>> it = valueMap.entrySet().iterator();
        Map.Entry<T, MutableInt> entry = it.next();
        T minCountElement = entry.getKey();
        int minCount = entry.getValue().value();

        while (it.hasNext()) {
            entry = it.next();

            if (entry.getValue().value() < minCount) {
                minCountElement = entry.getKey();
                minCount = entry.getValue().value();
            }
        }

        return Optional.of(Pair.of(minCountElement, minCount));
    }

    public Optional<Pair<T, Integer>> maxOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<T, MutableInt>> it = valueMap.entrySet().iterator();
        Map.Entry<T, MutableInt> entry = it.next();
        T maxCountElement = entry.getKey();
        int maxCount = entry.getValue().value();

        while (it.hasNext()) {
            entry = it.next();

            if (entry.getValue().value() > maxCount) {
                maxCountElement = entry.getKey();
                maxCount = entry.getValue().value();
            }
        }

        return Optional.of(Pair.of(maxCountElement, maxCount));
    }

    public Optional<Pair<List<T>, Integer>> allMinOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        int min = Integer.MAX_VALUE;

        for (MutableInt e : valueMap.values()) {
            if (e.value() < min) {
                min = e.value();
            }
        }

        final List<T> res = new ArrayList<>();

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            if (entry.getValue().value() == min) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(res, min));
    }

    public Optional<Pair<List<T>, Integer>> allMaxOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        int max = Integer.MIN_VALUE;

        for (MutableInt e : valueMap.values()) {
            if (e.value() > max) {
                max = e.value();
            }
        }

        final List<T> res = new ArrayList<>();

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            if (entry.getValue().value() == max) {
                res.add(entry.getKey());
            }
        }

        return Optional.of(Pair.of(res, max));
    }

    /**
     * 
     * @return
     * @throws ArithmeticException if total occurrences overflows the maximum value of int.
     */
    public long sumOfOccurrences() {
        long sum = 0;

        for (MutableInt count : valueMap.values()) {
            sum = Matth.addExact(sum, count.value());
        }

        return sum;
    }

    public OptionalDouble averageOfOccurrences() {
        if (size() == 0) {
            return OptionalDouble.empty();
        }

        final double sum = sumOfOccurrences();

        return OptionalDouble.of(sum / size());
    }

    /**
     *
     * @param e
     * @return always true
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Integer.MAX_VALUE.
     */
    public boolean add(final T e) throws IllegalArgumentException {
        return add(e, 1);
    }

    /**
     *
     * @param e
     * @param occurrences
     * @return true if the specified occurrences is bigger than 0.
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Integer.MAX_VALUE.
     */
    public boolean add(final T e, final int occurrences) throws IllegalArgumentException {
        checkOccurrences(occurrences);

        MutableInt count = valueMap.get(e);

        if (count != null && occurrences > (Integer.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of int");
        }

        if (count == null) {
            if (occurrences > 0) {
                count = MutableInt.of(occurrences);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrences);
        }

        return occurrences > 0;
    }

    /**
     * 
     * @param e
     * @return true if the specified element is absent.
     * @throws IllegalArgumentException
     */
    public boolean addIfAbsent(final T e) throws IllegalArgumentException {
        return addIfAbsent(e, 1);
    }

    /**
     * 
     * @param e
     * @param occurrences
     * @return true if the specified element is absent and occurrences is bigger than 0.
     * @throws IllegalArgumentException 
     */
    public boolean addIfAbsent(final T e, final int occurrences) throws IllegalArgumentException {
        checkOccurrences(occurrences);

        MutableInt count = valueMap.get(e);

        if (count == null && occurrences > 0) {
            count = MutableInt.of(occurrences);
            valueMap.put(e, count);

            return true;
        }

        return false;
    }

    public int addAndGet(final T e) {
        return addAndGet(e, 1);
    }

    public int addAndGet(final T e, final int occurrences) {
        checkOccurrences(occurrences);

        MutableInt count = valueMap.get(e);

        if (count != null && occurrences > (Integer.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of int");
        }

        if (count == null) {
            if (occurrences > 0) {
                count = MutableInt.of(occurrences);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrences);
        }

        return count == null ? 0 : count.value();
    }

    public int getAndAdd(final T e) {
        return getAndAdd(e, 1);
    }

    public int getAndAdd(final T e, final int occurrences) {
        checkOccurrences(occurrences);

        MutableInt count = valueMap.get(e);

        if (count != null && occurrences > (Integer.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of int");
        }

        final int result = count == null ? 0 : count.value();

        if (count == null) {
            if (occurrences > 0) {
                count = MutableInt.of(occurrences);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrences);
        }

        return result;
    }

    /**
     * 
     * @param c
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Integer.MAX_VALUE.
     */
    public boolean addAll(final Collection<? extends T> c) throws IllegalArgumentException {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return addAll(c, 1);
    }

    /**
     * 
     * @param c
     * @param occurrences
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Integer.MAX_VALUE.
     */
    public boolean addAll(final Collection<? extends T> c, final int occurrences) throws IllegalArgumentException {
        checkOccurrences(occurrences);

        if (N.isNullOrEmpty(c) || occurrences == 0) {
            return false;
        }

        for (T e : c) {
            add(e, occurrences);
        }

        return occurrences > 0;
    }

    /**
     * 
     * @param m
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Integer.MAX_VALUE.
     */
    public boolean addAll(final Map<? extends T, Integer> m) throws IllegalArgumentException {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        for (Map.Entry<? extends T, Integer> entry : m.entrySet()) {
            checkOccurrences(entry.getValue().intValue());
        }

        boolean result = false;

        for (Map.Entry<? extends T, Integer> entry : m.entrySet()) {
            if (result == false) {
                result = add(entry.getKey(), entry.getValue().intValue());
            } else {
                add(entry.getKey(), entry.getValue().intValue());
            }
        }

        return result;
    }

    /**
     * 
     * @param m
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public boolean addAll(final Multiset<? extends T> multiset) throws IllegalArgumentException {
        if (N.isNullOrEmpty(multiset)) {
            return false;
        }

        for (Map.Entry<? extends T, MutableInt> entry : multiset.valueMap.entrySet()) {
            add(entry.getKey(), entry.getValue().value());
        }

        return true;
    }

    public boolean contains(final Object o) {
        return valueMap.containsKey(o);
    }

    public boolean containsAll(final Collection<?> c) {
        return valueMap.keySet().containsAll(c);
    }

    /**
     * Remove one occurrence from the specified elements. 
     * The element will be removed from this <code>Multiset</code> if the occurrences equals to or less than 0 after the operation.
     *
     * @param e
     * @param occurrences
     * @return
     */
    public boolean remove(final Object e) {
        return remove(e, 1);
    }

    /**
     * Remove the specified occurrences from the specified element. 
     * The element will be removed from this <code>Multiset</code> if the occurrences equals to or less than 0 after the operation.
     *
     * @param e
     * @param occurrences
     * @return
     */
    public boolean remove(final Object e, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = valueMap.get(e);

        if (count == null) {
            return false;
        } else {
            count.subtract(occurrences);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }

            return occurrences > 0;
        }
    }

    public int removeAndGet(final Object e) {
        return removeAndGet(e, 1);
    }

    public int removeAndGet(final Object e, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = valueMap.get(e);

        if (count == null) {
            return 0;
        } else {
            count.subtract(occurrences);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }

            return count.value() > 0 ? count.value() : 0;
        }
    }

    public int getAndRemove(final Object e) {
        return getAndRemove(e, 1);
    }

    public int getAndRemove(final Object e, final int occurrences) {
        checkOccurrences(occurrences);

        final MutableInt count = valueMap.get(e);
        final int result = count == null ? 0 : count.value();

        if (count != null) {
            count.subtract(occurrences);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }
        }

        return result;
    }

    /**
     * 
     * @param e
     * @return the occurrences of the specified element before it's removed.
     */
    public int removeAllOccurrences(final Object e) {
        final MutableInt count = valueMap.remove(e);

        return count == null ? 0 : count.value();
    }

    public <E extends Exception> boolean removeAllOccurrencesIf(Try.Predicate<? super T, E> predicate) throws E {
        Set<T> removingKeys = null;

        for (T key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = new HashSet<>();
                }

                removingKeys.add(key);
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        removeAll(removingKeys);

        return true;
    }

    public <E extends Exception> boolean removeAllOccurrencesIf(Try.BiPredicate<? super T, ? super Integer, E> predicate) throws E {
        Set<T> removingKeys = null;

        for (Map.Entry<T, MutableInt> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().value())) {
                if (removingKeys == null) {
                    removingKeys = new HashSet<>();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        removeAll(removingKeys);

        return true;
    }

    public <E extends Exception> boolean removeIf(final int occurrences, Try.Predicate<? super T, E> predicate) throws E {
        checkOccurrences(occurrences);

        Set<T> removingKeys = null;

        for (T key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = new HashSet<>();
                }

                removingKeys.add(key);
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        removeAll(removingKeys, occurrences);

        return true;
    }

    public <E extends Exception> boolean removeIf(final int occurrences, Try.BiPredicate<? super T, ? super Integer, E> predicate) throws E {
        checkOccurrences(occurrences);

        Set<T> removingKeys = null;

        for (Map.Entry<T, MutableInt> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().value())) {
                if (removingKeys == null) {
                    removingKeys = new HashSet<>();
                }

                removingKeys.add(entry.getKey());
            }
        }

        if (N.isNullOrEmpty(removingKeys)) {
            return false;
        }

        removeAll(removingKeys, occurrences);

        return true;
    }

    /**
     * Removes all of this Multiset's elements that are also contained in the
     * specified collection (optional operation).  After this call returns,
     * this Multiset will contain no elements in common with the specified
     * collection. This method ignores how often any element might appear in
     * {@code c}, and only cares whether or not an element appears at all.
     * 
     * @param c
     * @return <tt>true</tt> if this set changed as a result of the call
     * @see Collection#removeAll(Collection)
     */
    public boolean removeAll(final Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        boolean result = false;

        for (Object e : c) {
            if (result == false) {
                result = valueMap.remove(e) != null;
            } else {
                valueMap.remove(e);
            }
        }

        return result;
    }

    /**
     * Remove the specified occurrences from the specified elements. 
     * The elements will be removed from this set if the occurrences equals to or less than 0 after the operation.
     *
     * @param c
     * @param occurrences
     *            the occurrences to remove if the element is in the specified collection <code>c</code>.
     * @return <tt>true</tt> if this set changed as a result of the call
     */
    public boolean removeAll(final Collection<?> c, final int occurrences) {
        checkOccurrences(occurrences);

        if (N.isNullOrEmpty(c) || occurrences == 0) {
            return false;
        }

        boolean result = false;

        for (Object e : c) {
            if (result == false) {
                result = remove(e, occurrences);
            } else {
                remove(e, occurrences);
            }
        }

        return result;
    }

    /**
     * 
     * @param m
     * @return
     */
    public boolean removeAll(final Map<?, Integer> m) {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        for (Map.Entry<?, Integer> entry : m.entrySet()) {
            checkOccurrences(entry.getValue().intValue());
        }

        boolean result = false;

        for (Map.Entry<?, Integer> entry : m.entrySet()) {
            if (result == false) {
                result = remove(entry.getKey(), entry.getValue().intValue());
            } else {
                remove(entry.getKey(), entry.getValue().intValue());
            }
        }

        return result;
    }

    /**
     * 
     * @param m
     */
    public boolean removeAll(final Multiset<?> multiset) throws IllegalArgumentException {
        if (N.isNullOrEmpty(multiset)) {
            return false;
        }

        for (Map.Entry<?, MutableInt> entry : multiset.valueMap.entrySet()) {
            remove(entry.getKey(), entry.getValue().value());
        }

        return true;
    }

    public <E extends Exception> boolean replaceIf(Try.Predicate<? super T, E> predicate, final int newOccurrences) throws E {
        checkOccurrences(newOccurrences);

        boolean modified = false;

        if (newOccurrences == 0) {
            final List<T> keysToRemove = new ArrayList<>();

            for (T key : valueMap.keySet()) {
                if (predicate.test(key)) {
                    keysToRemove.add(key);
                }
            }

            if (keysToRemove.size() > 0) {
                for (T key : keysToRemove) {
                    valueMap.remove(key);
                }

                modified = true;
            }

        } else {
            for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
                if (predicate.test(entry.getKey())) {
                    entry.getValue().setValue(newOccurrences);

                    modified = true;
                }
            }
        }

        return modified;
    }

    public <E extends Exception> boolean replaceIf(Try.BiPredicate<? super T, ? super Integer, E> predicate, final int newOccurrences) throws E {
        checkOccurrences(newOccurrences);

        boolean modified = false;

        if (newOccurrences == 0) {
            final List<T> keysToRemove = new ArrayList<>();

            for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
                if (predicate.test(entry.getKey(), entry.getValue().value())) {
                    keysToRemove.add(entry.getKey());
                }
            }

            if (keysToRemove.size() > 0) {
                for (T key : keysToRemove) {
                    valueMap.remove(key);
                }

                modified = true;
            }

        } else {
            for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
                if (predicate.test(entry.getKey(), entry.getValue().value())) {
                    entry.getValue().setValue(newOccurrences);

                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * The associated elements will be removed if zero or negative occurrences are returned by the specified <code>function</code>.
     * 
     * @param function
     */
    public <E extends Exception> void replaceAll(Try.BiFunction<? super T, ? super Integer, Integer, E> function) throws E {
        List<T> keyToRemove = null;
        Integer newVal = null;

        for (Map.Entry<T, MutableInt> entry : this.valueMap.entrySet()) {
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

        if (N.notNullOrEmpty(keyToRemove)) {
            for (T key : keyToRemove) {
                valueMap.remove(key);
            }
        }
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).  In other words, removes from
     * this collection all of its elements that are not contained in the
     * specified collection.
     *
     * @param c
     * @return <tt>true</tt> if this set changed as a result of the call
     * @see Collection#retainAll(Collection)
     */
    public boolean retainAll(final Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        Set<T> others = null;

        for (T e : valueMap.keySet()) {
            if (!c.contains(e)) {
                if (others == null) {
                    others = new HashSet<>(valueMap.size());
                }

                others.add(e);
            }
        }

        return N.isNullOrEmpty(others) ? false : removeAll(others, Integer.MAX_VALUE);
    }

    public Multiset<T> copy() {
        final Multiset<T> copy = new Multiset<T>(mapSupplier);

        copy.addAll(this);

        return copy;
    }

    public Set<T> elements() {
        return ImmutableSet.of(valueMap.keySet());
    }

    public int size() {
        return valueMap.size();
    }

    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    public void clear() {
        valueMap.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return valueMap.keySet().iterator();
    }

    //    public Set<Map.Entry<E, MutableInt>> entrySet() {
    //        return valueMap.entrySet();
    //    }

    public Object[] toArray() {
        return valueMap.keySet().toArray();
    }

    public <A> A[] toArray(final A[] a) {
        return valueMap.keySet().toArray(a);
    }

    public Map<T, Integer> toMap() {
        final Map<T, Integer> result = Maps.newOrderingMap(valueMap);

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    public <M extends Map<T, Integer>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(size());

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public Map<T, Integer> toMapSortedByOccurrences() {
        return toMapSortedBy((Comparator) cmpByCount);
    }

    public Map<T, Integer> toMapSortedByOccurrences(final Comparator<? super Integer> cmp) {
        return toMapSortedBy(new Comparator<Map.Entry<T, MutableInt>>() {
            @Override
            public int compare(Entry<T, MutableInt> o1, Entry<T, MutableInt> o2) {
                return cmp.compare(o1.getValue().value(), o2.getValue().value());
            }
        });
    }

    public Map<T, Integer> toMapSortedByKey(final Comparator<? super T> cmp) {
        return toMapSortedBy(new Comparator<Map.Entry<T, MutableInt>>() {
            @Override
            public int compare(Entry<T, MutableInt> o1, Entry<T, MutableInt> o2) {
                return cmp.compare(o1.getKey(), o2.getKey());
            }
        });
    }

    Map<T, Integer> toMapSortedBy(final Comparator<Map.Entry<T, MutableInt>> cmp) {
        if (N.isNullOrEmpty(valueMap)) {
            return new LinkedHashMap<>();
        }

        final Map.Entry<T, MutableInt>[] entries = valueMap.entrySet().toArray(new Map.Entry[size()]);
        Arrays.sort(entries, cmp);

        final Map<T, Integer> sortedValues = new LinkedHashMap<>(N.initHashCapacity(size()));

        for (Map.Entry<T, MutableInt> entry : entries) {
            sortedValues.put(entry.getKey(), entry.getValue().value());
        }

        return sortedValues;
    }

    public ImmutableMap<T, Integer> toImmutableMap() {
        return ImmutableMap.of(toMap());
    }

    public ImmutableMap<T, Integer> toImmutableMap(final IntFunction<? extends Map<T, Integer>> mapSupplier) {
        return ImmutableMap.of(toMap(mapSupplier));
    }

    // It won't work.
    //    public Multiset<T> synchronizedd() {
    //        return new Multiset<>(Collections.synchronizedMap(valueMap));
    //    }

    /**
     * 
     * @return a list with all elements, each of them is repeated with the occurrences in this <code>Multiset</code>   
     */
    public List<T> flatten() {
        final long totalOccurrences = sumOfOccurrences();

        if (totalOccurrences > Integer.MAX_VALUE) {
            throw new RuntimeException("The total occurrences(" + totalOccurrences + ") is bigger than the max value of int.");
        }

        final Object[] a = new Object[(int) totalOccurrences];

        int fromIndex = 0;
        int toIndex = 0;

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            toIndex = fromIndex + entry.getValue().value();

            Arrays.fill(a, fromIndex, toIndex, entry.getKey());
            fromIndex = toIndex;
        }

        return N.asList((T[]) a);
    }

    public <E extends Exception> Multiset<T> filter(Try.Predicate<? super T, E> filter) throws E {
        final Multiset<T> result = new Multiset<>(mapSupplier.get());

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.set(entry.getKey(), entry.getValue().intValue());
            }
        }

        return result;
    }

    public <E extends Exception> Multiset<T> filter(Try.BiPredicate<? super T, Integer, E> filter) throws E {
        final Multiset<T> result = new Multiset<>(mapSupplier.get());

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue().intValue())) {
                result.set(entry.getKey(), entry.getValue().intValue());
            }
        }

        return result;
    }

    public <E extends Exception> void forEach(final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        for (T e : valueMap.keySet()) {
            action.accept(e);
        }
    }

    public <E extends Exception> void forEach(final Try.ObjIntConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        for (Map.Entry<T, MutableInt> entry : valueMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue().value());
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
     * @return
     */
    public <E extends Exception> int computeIfAbsent(T e, Try.Function<? super T, Integer, E> mappingFunction) throws E {
        N.checkArgNotNull(mappingFunction);

        final int oldValue = get(e);

        if (oldValue > 0) {
            return oldValue;
        }

        final int newValue = mappingFunction.apply(e);

        if (newValue > 0) {
            set(e, newValue);
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
     * @return
     */
    public <E extends Exception> int computeIfPresent(T e, Try.BiFunction<? super T, Integer, Integer, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = get(e);

        if (oldValue == 0) {
            return oldValue;
        }

        final int newValue = remappingFunction.apply(e, oldValue);

        if (newValue > 0) {
            set(e, newValue);
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
     * @return
     */
    public <E extends Exception> int compute(T key, Try.BiFunction<? super T, Integer, Integer, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);

        final int oldValue = get(key);
        final int newValue = remappingFunction.apply(key, oldValue);

        if (newValue > 0) {
            set(key, newValue);
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
     * @return
     */
    public <E extends Exception> int merge(T key, int value, Try.BiFunction<Integer, Integer, Integer, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(value);

        int oldValue = get(key);
        int newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);

        if (newValue > 0) {
            set(key, newValue);
        } else {
            if (oldValue > 0) {
                remove(key);
            }
        }

        return newValue;
    }

    public Stream<T> stream() {
        return Stream.of(valueMap.keySet());
    }

    public Stream<T> flatStream() {
        final Iterator<Map.Entry<T, MutableInt>> entryIter = valueMap.entrySet().iterator();

        final Iterator<T> iter = new ObjIterator<T>() {
            private Map.Entry<T, MutableInt> entry = null;
            private T element = null;
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
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                cnt++;

                return element;
            }
        };

        return Stream.of(iter);
    }

    private static final Function<MutableInt, Integer> TO_INT = new Function<MutableInt, Integer>() {
        @Override
        public Integer apply(MutableInt t) {
            return t.value();
        }
    };

    public EntryStream<T, Integer> entryStream() {
        return EntryStream.of(valueMap).mapValue(TO_INT);
    }

    public <R, E extends Exception> R apply(Try.Function<? super Multiset<T>, R, E> func) throws E {
        return func.apply(this);
    }

    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Try.Function<? super Multiset<T>, R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    public <E extends Exception> void accept(Try.Consumer<? super Multiset<T>, E> action) throws E {
        action.accept(this);
    }

    public <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super Multiset<T>, E> action) throws E {
        if (size() > 0) {
            action.accept(this);
        }
    }

    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Multiset && valueMap.equals(((Multiset<T>) obj).valueMap));
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }

    private static void checkOccurrences(final int occurrences) {
        if (occurrences < 0) {
            throw new IllegalArgumentException("The specified 'occurrences' can not be negative");
        }
    }
}
