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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * A collection that supports order-independent equality, like {@link Set}, but
 * may have duplicate elements.
 *
 * <p>Elements of a LongMultiset that are equal to one another are referred to as
 * <i>occurrences</i> of the same single element. The total number of
 * occurrences of an element in a LongMultiset is called the <i>count</i> of that
 * element (the terms "frequency" and "multiplicity" are equivalent, but not
 * used in this API). Since the count of an element is represented as an {@code
 * long}, a LongMultiset may never contain more than {@link MutableLong#MAX_VALUE}
 * occurrences of any one element.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public final class LongMultiset<T> implements Iterable<T> {

    private static final Comparator<Map.Entry<?, MutableLong>> cmpByCount = (a, b) -> N.compare(a.getValue().value(), b.getValue().value());

    final Supplier<Map<T, MutableLong>> mapSupplier;

    final Map<T, MutableLong> valueMap;

    /**
     *
     */
    public LongMultiset() {
        this(HashMap.class);
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public LongMultiset(int initialCapacity) {
        this.mapSupplier = Suppliers.ofMap();
        this.valueMap = N.newHashMap(initialCapacity);
    }

    /**
     *
     *
     * @param c
     */
    public LongMultiset(final Collection<? extends T> c) {
        this((c == null || c instanceof Set) ? N.size(c) : N.size(c) / 2);

        addAll(c);
    }

    /**
     *
     *
     * @param valueMapType
     */
    @SuppressWarnings("rawtypes")
    public LongMultiset(final Class<? extends Map> valueMapType) {
        this(Suppliers.ofMap(valueMapType));
    }

    /**
     *
     *
     * @param mapSupplier
     */
    @SuppressWarnings("rawtypes")
    public LongMultiset(final Supplier<? extends Map<? extends T, ?>> mapSupplier) {
        this.mapSupplier = (Supplier) mapSupplier;
        this.valueMap = this.mapSupplier.get();
    }

    @SuppressWarnings("rawtypes")
    @Internal
    LongMultiset(final Map<T, MutableLong> valueMap) {
        this.mapSupplier = (Supplier) Suppliers.ofMap(valueMap.getClass());
        this.valueMap = valueMap;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> LongMultiset<T> of(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new LongMultiset<>();
        }

        final LongMultiset<T> multiset = new LongMultiset<>(N.<T, MutableLong> newHashMap(a.length));

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
    public static <T> LongMultiset<T> create(final Collection<? extends T> coll) {
        return new LongMultiset<>(coll);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> LongMultiset<T> create(final Iterator<? extends T> iter) {
        final LongMultiset<T> result = new LongMultiset<>();

        if (iter != null) {
            T e = null;
            MutableLong count = null;

            while (iter.hasNext()) {
                e = iter.next();
                count = result.valueMap.get(e);

                if (count == null) {
                    result.valueMap.put(e, MutableLong.of(1));
                } else {
                    if (count.value() == Long.MAX_VALUE) {
                        throw new IllegalArgumentException("The total count is out of the bound of int");
                    }

                    count.add(1);
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param m
     * @return
     */
    public static <T> LongMultiset<T> create(final Map<? extends T, Long> m) {
        if (N.isNullOrEmpty(m)) {
            return new LongMultiset<>();
        }

        final LongMultiset<T> multiset = new LongMultiset<>(Maps.newTargetMap(m));

        multiset.setAll(m);

        return multiset;
    }

    /**
     *
     *
     * @param <T>
     * @param <V>
     * @param m
     * @param valueMapper
     * @return
     */
    @Beta
    public static <T, V> LongMultiset<T> create(final Map<? extends T, ? extends V> m, final ToLongFunction<? super V> valueMapper) {
        if (N.isNullOrEmpty(m)) {
            return new LongMultiset<>();
        }

        final LongMultiset<T> multiset = new LongMultiset<>(Maps.newTargetMap(m));

        for (Map.Entry<? extends T, ? extends V> entry : m.entrySet()) {
            multiset.set(entry.getKey(), checkOccurrences(valueMapper.applyAsLong(entry.getValue())));
        }

        return multiset;
    }

    /**
     *
     * @param <T>
     * @param multiset
     * @return
     */
    public static <T> LongMultiset<T> create(final Multiset<? extends T> multiset) {
        if (N.isNullOrEmpty(multiset)) {
            return new LongMultiset<>();
        }

        final LongMultiset<T> result = new LongMultiset<>(Maps.newTargetMap(multiset.valueMap));

        for (Map.Entry<? extends T, MutableInt> entry : multiset.valueMap.entrySet()) {
            result.set(entry.getKey(), entry.getValue().intValue());
        }

        return result;
    }

    /**
     *
     * @param e
     * @return
     */
    public long get(final Object e) {
        final MutableLong count = valueMap.get(e);

        return count == null ? 0 : count.value();
    }

    /**
     * Gets the or default.
     *
     * @param e
     * @param defaultValue
     * @return
     */
    public long getOrDefault(final Object e, long defaultValue) {
        final MutableLong count = valueMap.get(e);

        return count == null ? defaultValue : count.value();
    }

    /**
     * The element will be removed if the specified count is 0.
     *
     * @param e
     * @param occurrences
     * @return
     */
    public long getAndSet(final T e, final long occurrences) {
        checkOccurrences(occurrences);

        final MutableLong count = valueMap.get(e);
        long result = count == null ? 0 : count.value();

        if (occurrences == 0) {
            if (count != null) {
                valueMap.remove(e);
            }
        } else {
            if (count == null) {
                valueMap.put(e, MutableLong.of(occurrences));
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
    public long setAndGet(final T e, final long occurrences) {
        checkOccurrences(occurrences);

        final MutableLong count = valueMap.get(e);

        if (occurrences == 0) {
            if (count != null) {
                valueMap.remove(e);
            }
        } else {
            if (count == null) {
                valueMap.put(e, MutableLong.of(occurrences));
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
     * @return this LongMultiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0
     */
    public LongMultiset<T> set(final T e, final long occurrences) {
        checkOccurrences(occurrences);

        if (occurrences == 0) {
            valueMap.remove(e);
        } else {
            final MutableLong count = valueMap.get(e);

            if (count == null) {
                valueMap.put(e, MutableLong.of(occurrences));
            } else {
                count.setValue(occurrences);
            }
        }

        return this;
    }

    /**
     * Sets the all.
     *
     * @param c
     * @param occurrences
     * @return
     */
    public LongMultiset<T> setAll(final Collection<? extends T> c, final long occurrences) {
        checkOccurrences(occurrences);

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                set(e, occurrences);
            }
        }

        return this;
    }

    /**
     * Sets the all.
     *
     * @param m
     * @return this LongMultiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public LongMultiset<T> setAll(final Map<? extends T, Long> m) throws IllegalArgumentException {
        if (N.notNullOrEmpty(m)) {
            for (Map.Entry<? extends T, Long> entry : m.entrySet()) {
                checkOccurrences(entry.getValue());
            }

            for (Map.Entry<? extends T, Long> entry : m.entrySet()) {
                set(entry.getKey(), entry.getValue().longValue());
            }
        }

        return this;
    }

    /**
     * Sets the all.
     *
     * @param multiset
     * @return this LongMultiset.
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public LongMultiset<T> setAll(final LongMultiset<? extends T> multiset) throws IllegalArgumentException {
        if (N.notNullOrEmpty(multiset)) {
            for (Map.Entry<? extends T, MutableLong> entry : multiset.valueMap.entrySet()) {
                set(entry.getKey(), entry.getValue().value());
            }
        }

        return this;
    }

    /**
     *
     * @param e
     * @return
     */
    public long occurrencesOf(final Object e) {
        return get(e);
    }

    /**
     *
     *
     * @return
     */
    public Optional<Pair<Long, T>> minOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<T, MutableLong>> it = valueMap.entrySet().iterator();
        Map.Entry<T, MutableLong> entry = it.next();
        T minCountElement = entry.getKey();
        long minCount = entry.getValue().value();

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
    public Optional<Pair<Long, T>> maxOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        final Iterator<Map.Entry<T, MutableLong>> it = valueMap.entrySet().iterator();
        Map.Entry<T, MutableLong> entry = it.next();
        T maxCountElement = entry.getKey();
        long maxCount = entry.getValue().value();

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
    public Optional<Pair<Long, List<T>>> allMinOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        long min = Long.MAX_VALUE;

        for (MutableLong e : valueMap.values()) {
            if (e.value() < min) {
                min = e.value();
            }
        }

        final List<T> res = new ArrayList<>();

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
    public Optional<Pair<Long, List<T>>> allMaxOccurrences() {
        if (size() == 0) {
            return Optional.empty();
        }

        long max = Long.MIN_VALUE;

        for (MutableLong e : valueMap.values()) {
            if (e.value() > max) {
                max = e.value();
            }
        }

        final List<T> res = new ArrayList<>();

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
     * @throws ArithmeticException if total occurrences overflows the maximum value of long.
     */
    public long sumOfOccurrences() {
        long sum = 0;

        for (MutableLong count : valueMap.values()) {
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
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Long.MAX_VALUE.
     */
    public boolean add(final T e) throws IllegalArgumentException {
        return add(e, 1);
    }

    /**
     *
     * @param e
     * @param occurrencesToAdd
     * @return true if the specified occurrences is bigger than 0.
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Long.MAX_VALUE.
     */
    public boolean add(final T e, final long occurrencesToAdd) throws IllegalArgumentException {
        checkOccurrences(occurrencesToAdd);

        MutableLong count = valueMap.get(e);

        if (count != null && occurrencesToAdd > (Long.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of long"); //NOSONAR
        }

        if (count == null) {
            if (occurrencesToAdd > 0) {
                count = MutableLong.of(occurrencesToAdd);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrencesToAdd);
        }

        return occurrencesToAdd > 0;
    }

    /**
     * Adds the if absent.
     *
     * @param e
     * @return true if the specified element is absent.
     * @throws IllegalArgumentException the illegal argument exception
     */
    public boolean addIfAbsent(final T e) throws IllegalArgumentException {
        return addIfAbsent(e, 1);
    }

    /**
     * Adds the if absent.
     *
     * @param e
     * @param occurrencesToAdd
     * @return true if the specified element is absent and occurrences is bigger than 0.
     * @throws IllegalArgumentException the illegal argument exception
     */
    public boolean addIfAbsent(final T e, final long occurrencesToAdd) throws IllegalArgumentException {
        checkOccurrences(occurrencesToAdd);

        MutableLong count = valueMap.get(e);

        if (count == null && occurrencesToAdd > 0) {
            count = MutableLong.of(occurrencesToAdd);
            valueMap.put(e, count);

            return true;
        }

        return false;
    }

    /**
     * Adds the and get.
     *
     * @param e
     * @return
     */
    public long addAndGet(final T e) {
        return addAndGet(e, 1);
    }

    /**
     * Adds the and get.
     *
     * @param e
     * @param occurrencesToAdd
     * @return
     */
    public long addAndGet(final T e, final long occurrencesToAdd) {
        checkOccurrences(occurrencesToAdd);

        MutableLong count = valueMap.get(e);

        if (count != null && occurrencesToAdd > (Long.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of long");
        }

        if (count == null) {
            if (occurrencesToAdd > 0) {
                count = MutableLong.of(occurrencesToAdd);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrencesToAdd);
        }

        return count == null ? 0 : count.value();
    }

    /**
     * Gets the and add.
     *
     * @param e
     * @return
     */
    public long getAndAdd(final T e) {
        return getAndAdd(e, 1);
    }

    /**
     * Gets the and add.
     *
     * @param e
     * @param occurrencesToAdd
     * @return
     */
    public long getAndAdd(final T e, final long occurrencesToAdd) {
        checkOccurrences(occurrencesToAdd);

        MutableLong count = valueMap.get(e);

        if (count != null && occurrencesToAdd > (Long.MAX_VALUE - count.value())) {
            throw new IllegalArgumentException("The total count is out of the bound of long");
        }

        final long result = count == null ? 0 : count.value();

        if (count == null) {
            if (occurrencesToAdd > 0) {
                count = MutableLong.of(occurrencesToAdd);
                valueMap.put(e, count);
            }
        } else {
            count.add(occurrencesToAdd);
        }

        return result;
    }

    /**
     * Adds the all.
     *
     * @param c
     * @return
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Long.MAX_VALUE.
     */
    public boolean addAll(final Collection<? extends T> c) throws IllegalArgumentException {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return addAll(c, 1);
    }

    /**
     * Adds the all.
     *
     * @param c
     * @param occurrencesToAdd
     * @return
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Long.MAX_VALUE.
     */
    public boolean addAll(final Collection<? extends T> c, final long occurrencesToAdd) throws IllegalArgumentException {
        checkOccurrences(occurrencesToAdd);

        if (N.isNullOrEmpty(c) || occurrencesToAdd == 0) {
            return false;
        }

        for (T e : c) {
            add(e, occurrencesToAdd);
        }

        return occurrencesToAdd > 0;
    }

    /**
     * Adds the all.
     *
     * @param m
     * @return
     * @throws IllegalArgumentException if the occurrences of element after this operation is bigger than Long.MAX_VALUE.
     */
    public boolean addAll(final Map<? extends T, Long> m) throws IllegalArgumentException {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        for (Map.Entry<? extends T, Long> entry : m.entrySet()) {
            checkOccurrences(entry.getValue());
        }

        boolean result = false;

        for (Map.Entry<? extends T, Long> entry : m.entrySet()) {
            if (!result) {
                result = add(entry.getKey(), entry.getValue().longValue());
            } else {
                add(entry.getKey(), entry.getValue().longValue());
            }
        }

        return result;
    }

    /**
     * Adds the all.
     *
     * @param multiset
     * @return
     * @throws IllegalArgumentException if the occurrences of element is less than 0.
     */
    public boolean addAll(final LongMultiset<? extends T> multiset) throws IllegalArgumentException {
        if (N.isNullOrEmpty(multiset)) {
            return false;
        }

        for (Map.Entry<? extends T, MutableLong> entry : multiset.valueMap.entrySet()) {
            add(entry.getKey(), entry.getValue().value());
        }

        return true;
    }

    /**
     *
     * @param o
     * @return
     */
    public boolean contains(final Object o) {
        return valueMap.containsKey(o);
    }

    /**
     *
     * @param c
     * @return
     */
    public boolean containsAll(final Collection<?> c) {
        return valueMap.keySet().containsAll(c);
    }

    /**
     * Remove one occurrence from the specified elements.
     * The element will be removed from this <code>Multiset</code> if the occurrences equals to or less than 0 after the operation.
     *
     * @param e
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
     * @param occurrencesToRemove
     * @return
     */
    public boolean remove(final Object e, final long occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        final MutableLong count = valueMap.get(e);

        if (count == null) {
            return false;
        } else {
            count.subtract(occurrencesToRemove);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }

            return occurrencesToRemove > 0;
        }
    }

    /**
     * Removes the and get.
     *
     * @param e
     * @return
     */
    public long removeAndGet(final Object e) {
        return removeAndGet(e, 1);
    }

    /**
     * Removes the and get.
     *
     * @param e
     * @param occurrencesToRemove
     * @return
     */
    public long removeAndGet(final Object e, final long occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        final MutableLong count = valueMap.get(e);

        if (count == null) {
            return 0;
        } else {
            count.subtract(occurrencesToRemove);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }

            return count.value() > 0 ? count.value() : 0;
        }
    }

    /**
     * Gets the and remove.
     *
     * @param e
     * @return
     */
    public long getAndRemove(final Object e) {
        return getAndRemove(e, 1);
    }

    /**
     * Gets the and remove.
     *
     * @param e
     * @param occurrencesToRemove
     * @return
     */
    public long getAndRemove(final Object e, final long occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        final MutableLong count = valueMap.get(e);
        final long result = count == null ? 0 : count.value();

        if (count != null) {
            count.subtract(occurrencesToRemove);

            if (count.value() <= 0) {
                valueMap.remove(e);
            }
        }

        return result;
    }

    /**
     * Removes the all occurrences.
     *
     * @param e
     * @return
     */
    public long removeAllOccurrences(final Object e) {
        final MutableLong count = valueMap.remove(e);

        return count == null ? 0 : count.value();
    }

    /**
     * Removes the all occurrences if.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean removeAllOccurrencesIf(Throwables.Predicate<? super T, E> predicate) throws E {
        Set<T> removingKeys = null;

        for (T key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
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

    /**
     * Removes the all occurrences if.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean removeAllOccurrencesIf(Throwables.BiPredicate<? super T, ? super Long, E> predicate) throws E {
        Set<T> removingKeys = null;

        for (Map.Entry<T, MutableLong> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().value())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
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

    /**
     * Removes the all occurrences.
     *
     * @param c
     * @return
     */
    public boolean removeAllOccurrencesForEach(final Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        final int size = valueMap.size();

        for (Object e : c) {
            removeAllOccurrences(e);
        }

        return valueMap.size() < size;
    }

    /**
     * Removes the if.
     * @param <E>
     * @param occurrences
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean removeIf(final long occurrences, Throwables.Predicate<? super T, E> predicate) throws E {
        checkOccurrences(occurrences);

        Set<T> removingKeys = null;

        for (T key : this.valueMap.keySet()) {
            if (predicate.test(key)) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
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

    /**
     * Removes the if.
     * @param <E>
     * @param occurrences
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean removeIf(final long occurrences, Throwables.BiPredicate<? super T, ? super Long, E> predicate) throws E {
        checkOccurrences(occurrences);

        Set<T> removingKeys = null;

        for (Map.Entry<T, MutableLong> entry : this.valueMap.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().value())) {
                if (removingKeys == null) {
                    removingKeys = N.newHashSet();
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
     * Remove one occurrence for each element in the specified Collection.
     * The elements will be removed from this set if the occurrences equals to or less than 0 after the operation.
     *
     * @param c
     * @return <tt>true</tt> if this set changed as a result of the call
     */
    public boolean removeAll(final Collection<?> c) {
        return removeAll(c, 1);
    }

    /**
     * Remove the specified occurrences for each element in the specified Collection.
     * The elements will be removed from this set if the occurrences equals to or less than 0 after the operation.
     *
     * @param c
     * @param occurrencesToRemove the occurrences to remove if the element is in the specified collection <code>c</code>.
     * @return <tt>true</tt> if this set changed as a result of the call
     */
    public boolean removeAll(final Collection<?> c, final long occurrencesToRemove) {
        checkOccurrences(occurrencesToRemove);

        if (N.isNullOrEmpty(c) || occurrencesToRemove == 0) {
            return false;
        }

        boolean result = false;

        for (Object e : c) {
            if (!result) {
                result = remove(e, occurrencesToRemove);
            } else {
                remove(e, occurrencesToRemove);
            }
        }

        return result;
    }

    /**
     * Removes the all.
     *
     * @param m
     * @return
     */
    public boolean removeAll(final Map<?, Long> m) {
        if (N.isNullOrEmpty(m)) {
            return false;
        }

        for (Map.Entry<?, Long> entry : m.entrySet()) {
            checkOccurrences(entry.getValue());
        }

        boolean result = false;

        for (Map.Entry<?, Long> entry : m.entrySet()) {
            if (!result) {
                result = remove(entry.getKey(), entry.getValue().longValue());
            } else {
                remove(entry.getKey(), entry.getValue().longValue());
            }
        }

        return result;
    }

    /**
     * Removes the all.
     *
     * @param multiset
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     */
    public boolean removeAll(final LongMultiset<?> multiset) throws IllegalArgumentException {
        if (N.isNullOrEmpty(multiset)) {
            return false;
        }

        for (Map.Entry<?, MutableLong> entry : multiset.valueMap.entrySet()) {
            remove(entry.getKey(), entry.getValue().value());
        }

        return true;
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @param newOccurrences
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean replaceIf(Throwables.Predicate<? super T, E> predicate, final long newOccurrences) throws E {
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
            for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
                if (predicate.test(entry.getKey())) {
                    entry.getValue().setValue(newOccurrences);

                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @param newOccurrences
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean replaceIf(Throwables.BiPredicate<? super T, ? super Long, E> predicate, final long newOccurrences) throws E {
        checkOccurrences(newOccurrences);

        boolean modified = false;

        if (newOccurrences == 0) {
            final List<T> keysToRemove = new ArrayList<>();

            for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
            for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
     * @param <E>
     * @param function
     * @throws E the e
     */
    public <E extends Exception> void replaceAll(Throwables.BiFunction<? super T, ? super Long, Long, E> function) throws E {
        List<T> keyToRemove = null;
        Long newVal = null;

        for (Map.Entry<T, MutableLong> entry : this.valueMap.entrySet()) {
            newVal = function.apply(entry.getKey(), entry.getValue().value());

            if (newVal == null || newVal.longValue() <= 0) {
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
                    others = N.newHashSet(valueMap.size());
                }

                others.add(e);
            }
        }

        return N.isNullOrEmpty(others) ? false : removeAll(others, Long.MAX_VALUE);
    }

    /**
     *
     *
     * @return
     */
    public LongMultiset<T> copy() {
        final LongMultiset<T> copy = new LongMultiset<>(mapSupplier);

        copy.addAll(this);

        return copy;
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("deprecation")
    public ImmutableSet<T> elements() {
        return ImmutableSet.wrap(valueMap.keySet());
    }

    /**
     *
     *
     * @return
     */
    public int size() {
        return valueMap.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    /**
     * Clear.
     */
    public void clear() {
        valueMap.clear();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Iterator<T> iterator() {
        return valueMap.keySet().iterator();
    }

    /**
     *
     *
     * @return
     */
    public Iterator<T> flatIterator() {
        final Iterator<Map.Entry<T, MutableLong>> entryIter = valueMap.entrySet().iterator();

        return new ObjIterator<>() {
            private Map.Entry<T, MutableLong> entry = null;
            private T element = null;
            private long count = 0;
            private long cnt = 0;

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
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                cnt++;

                return element;
            }
        };
    }

    //    public Set<Map.Entry<E, MutableLong>> entrySet() {
    //        return valueMap.entrySet();
    //    }

    /**
     *
     *
     * @return
     */
    public Object[] toArray() {
        return valueMap.keySet().toArray();
    }

    /**
     *
     * @param <A>
     * @param a
     * @return
     */
    public <A> A[] toArray(final A[] a) {
        return valueMap.keySet().toArray(a);
    }

    /**
     *
     *
     * @return
     */
    public Map<T, Long> toMap() {
        final Map<T, Long> result = Maps.newTargetMap(valueMap);

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
    public <M extends Map<T, Long>> M toMap(final IntFunction<? extends M> supplier) {
        final M result = supplier.apply(size());

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
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
    public Map<T, Long> toMapSortedByOccurrences() {
        return toMapSortedBy((Comparator) cmpByCount);
    }

    /**
     * To map sorted by occurrences.
     *
     * @param cmp
     * @return
     */
    public Map<T, Long> toMapSortedByOccurrences(final Comparator<? super Long> cmp) {
        return toMapSortedBy((o1, o2) -> cmp.compare(o1.getValue().value(), o2.getValue().value()));
    }

    /**
     * To map sorted by key.
     *
     * @param cmp
     * @return
     */
    public Map<T, Long> toMapSortedByKey(final Comparator<? super T> cmp) {
        return toMapSortedBy((o1, o2) -> cmp.compare(o1.getKey(), o2.getKey()));
    }

    /**
     * To map sorted by.
     *
     * @param cmp
     * @return
     */
    Map<T, Long> toMapSortedBy(final Comparator<Map.Entry<T, MutableLong>> cmp) {
        if (N.isNullOrEmpty(valueMap)) {
            return new LinkedHashMap<>();
        }

        final Map.Entry<T, MutableLong>[] entries = valueMap.entrySet().toArray(new Map.Entry[size()]);
        Arrays.sort(entries, cmp);

        final Map<T, Long> sortedValues = N.newLinkedHashMap(size());

        for (Map.Entry<T, MutableLong> entry : entries) {
            sortedValues.put(entry.getKey(), entry.getValue().value());
        }

        return sortedValues;
    }

    /**
     * To immutable map.
     *
     * @return
     */
    @SuppressWarnings("deprecation")
    public ImmutableMap<T, Long> toImmutableMap() {
        return ImmutableMap.wrap(toMap());
    }

    /**
     * To immutable map.
     *
     * @param mapSupplier
     * @return
     */
    @SuppressWarnings("deprecation")
    public ImmutableMap<T, Long> toImmutableMap(final IntFunction<? extends Map<T, Long>> mapSupplier) {
        return ImmutableMap.wrap(toMap(mapSupplier));
    }

    // It won't work.
    //    public LongMultiset<T> synchronizedd() {
    //        return new LongMultiset<>(Collections.synchronizedMap(valueMap));
    //    }

    /**
     *
     * @return a list with all elements, each of them is repeated with the occurrences in this <code>LongMultiset</code>
     */
    public List<T> flatten() {
        final long totalOccurrences = sumOfOccurrences();

        if (totalOccurrences > Integer.MAX_VALUE) {
            throw new RuntimeException("The total occurrences(" + totalOccurrences + ") is bigger than the max value of int.");
        }

        final Object[] a = new Object[(int) totalOccurrences];

        int fromIndex = 0;
        int toIndex = 0;

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
            toIndex = fromIndex + (int) entry.getValue().value();

            Arrays.fill(a, fromIndex, toIndex, entry.getKey());
            fromIndex = toIndex;
        }

        return N.asList((T[]) a);
    }

    /**
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> LongMultiset<T> filter(Throwables.Predicate<? super T, E> filter) throws E {
        final LongMultiset<T> result = new LongMultiset<>(mapSupplier.get());

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey())) {
                result.set(entry.getKey(), entry.getValue().longValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> LongMultiset<T> filter(Throwables.BiPredicate<? super T, Long, E> filter) throws E {
        final LongMultiset<T> result = new LongMultiset<>(mapSupplier.get());

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue().longValue())) {
                result.set(entry.getKey(), entry.getValue().longValue());
            }
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        for (T e : valueMap.keySet()) {
            action.accept(e);
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(final Throwables.ObjLongConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        for (Map.Entry<T, MutableLong> entry : valueMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue().value());
        }
    }

    /**
     * The implementation is equivalent to performing the following steps for this LongMultiset:
     *
     * <pre>
     * final long oldValue = get(e);
     *
     * if (oldValue > 0) {
     *     return oldValue;
     * }
     *
     * final long newValue = mappingFunction.apply(e);
     *
     * if (newValue > 0) {
     *     set(e, newValue);
     * }
     *
     * return newValue;
     * </pre>
     *
     * @param <E>
     * @param e
     * @param mappingFunction
     * @return
     * @throws E the e
     */
    public <E extends Exception> long computeIfAbsent(T e, Throwables.Function<? super T, Long, E> mappingFunction) throws E {
        N.checkArgNotNull(mappingFunction);

        final long oldValue = get(e);

        if (oldValue > 0) {
            return oldValue;
        }

        final long newValue = mappingFunction.apply(e);

        if (newValue > 0) {
            set(e, newValue);
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this LongMultiset:
     *
     * <pre>
     * final long oldValue = get(e);
     *
     * if (oldValue == 0) {
     *     return oldValue;
     * }
     *
     * final long newValue = remappingFunction.apply(e, oldValue);
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
     * @param <E>
     * @param e
     * @param remappingFunction
     * @return
     * @throws E the e
     */
    public <E extends Exception> long computeIfPresent(T e, Throwables.BiFunction<? super T, Long, Long, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);

        final long oldValue = get(e);

        if (oldValue == 0) {
            return oldValue;
        }

        final long newValue = remappingFunction.apply(e, oldValue);

        if (newValue > 0) {
            set(e, newValue);
        } else {
            remove(e);
        }

        return newValue;
    }

    /**
     * The implementation is equivalent to performing the following steps for this LongMultiset:
     *
     * <pre>
     * final long oldValue = get(key);
     * final long newValue = remappingFunction.apply(key, oldValue);
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
     * @param <E>
     * @param key
     * @param remappingFunction
     * @return
     * @throws E the e
     */
    public <E extends Exception> long compute(T key, Throwables.BiFunction<? super T, Long, Long, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);

        final long oldValue = get(key);
        final long newValue = remappingFunction.apply(key, oldValue);

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
     * The implementation is equivalent to performing the following steps for this LongMultiset:
     *
     * <pre>
     * long oldValue = get(key);
     * long newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);
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
     * @param <E>
     * @param key
     * @param value
     * @param remappingFunction
     * @return
     * @throws E the e
     */
    public <E extends Exception> long merge(T key, long value, Throwables.BiFunction<Long, Long, Long, E> remappingFunction) throws E {
        N.checkArgNotNull(remappingFunction);
        N.checkArgNotNull(value);

        long oldValue = get(key);
        long newValue = (oldValue == 0) ? value : remappingFunction.apply(oldValue, value);

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
     *
     *
     * @return
     */
    public Stream<T> stream() {
        return Stream.of(valueMap.keySet());
    }

    /**
     *
     *
     * @return
     */
    public Stream<T> flatStream() {
        return Stream.of(flatIterator());
    }

    private static final com.landawn.abacus.util.function.Function<MutableLong, Long> TO_LONG = MutableLong::value;

    /**
     *
     *
     * @return
     */
    public EntryStream<T, Long> entryStream() {
        return EntryStream.of(valueMap).mapValue(TO_LONG);
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> R apply(Throwables.Function<? super LongMultiset<T>, ? extends R, E> func) throws E {
        return func.apply(this);
    }

    /**
     * Apply if not empty.
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super LongMultiset<T>, ? extends R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.Consumer<? super LongMultiset<T>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Accept if not empty.
     *
     * @param <E>
     * @param action
     * @return
     * @throws E the e
     */
    public <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super LongMultiset<T>, E> action) throws E {
        return If.is(size() > 0).then(this, action);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof LongMultiset && valueMap.equals(((LongMultiset<T>) obj).valueMap));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return valueMap.toString();
    }

    /**
     *
     * @param occurrences
     */
    private static long checkOccurrences(final long occurrences) {
        if (occurrences < 0) {
            throw new IllegalArgumentException("The specified 'occurrences' can not be negative");
        }

        return occurrences;
    }
}
