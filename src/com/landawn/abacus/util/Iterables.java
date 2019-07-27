/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * It's an extension and wrapper for Google Guava.
 * 
 * @since 1.2.7
 * 
 * @author Haiyang Li 
 */
public final class Iterables {
    private static final Logger logger = LoggerFactory.getLogger(Iterables.class);

    private Iterables() {
        // singleton.
    }

    /**
     * 
     * @param a
     * @param b
     * @return
     */
    public static <T> Set<T> differentSet(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a)) {
            return new HashSet<>();
        } else if (N.isNullOrEmpty(b)) {
            return new HashSet<>(a);
        }

        final Set<T> result = new HashSet<>(a);

        Iterables.removeAll(a, b);

        return result;
    }

    /**
     * 
     * @param a
     * @param b
     * @return
     */
    public static <T> Set<T> symmetricDifferentSet(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new HashSet<T>() : new HashSet<>(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? new HashSet<T>() : new HashSet<>(a);
        }

        final Set<T> commonSet = Iterables.commonSet(a, b);
        final Set<T> result = new HashSet<>(a);

        for (T e : a) {
            if (!commonSet.contains(e)) {
                result.add(e);
            }
        }

        for (T e : b) {
            if (!commonSet.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * 
     * @param a
     * @param b
     * @return
     */
    public static <T> Set<T> commonSet(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return new HashSet<>();
        }

        return commonSet(N.asList(a, (Collection<? extends T>) b));
    }

    public static <T> Set<T> commonSet(final Collection<? extends Collection<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return new HashSet<>();
        } else if (c.size() == 1) {
            return N.newHashSet(c.iterator().next());
        }

        Collection<? extends T> smallest = null;

        for (Collection<? extends T> e : c) {
            if (N.isNullOrEmpty(e)) {
                return new HashSet<>();
            }

            if (smallest == null || e.size() < smallest.size()) {
                smallest = e;
            }
        }

        final Map<T, MutableInt> map = new HashMap<>();

        for (T e : smallest) {
            map.put(e, new MutableInt(1));
        }

        int cnt = 1;
        MutableInt val = null;

        for (Collection<? extends T> ec : c) {
            if (ec == smallest) {
                continue;
            }

            for (T e : ec) {
                val = map.get(e);

                if (val == null) {
                    // do nothing.
                } else if (val.intValue() < cnt) {
                    // map.remove(e);
                } else if (val.intValue() == cnt) {
                    val.increment();
                }
            }

            cnt++;
        }

        final Set<T> result = new HashSet<>(map.size());

        for (Map.Entry<T, MutableInt> entry : map.entrySet()) {
            if (entry.getValue().intValue() == cnt) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public static boolean removeAll(Collection<?> c, Collection<?> objsToRemove) {
        if (N.isNullOrEmpty(c) || N.isNullOrEmpty(objsToRemove)) {
            return false;
        }

        if (c instanceof HashSet && !(objsToRemove instanceof Set)) {
            boolean result = false;

            for (Object e : objsToRemove) {
                result |= c.remove(e);

                if (c.size() == 0) {
                    break;
                }
            }

            return result;
        } else {
            return c.removeAll(objsToRemove);
        }
    }

    public static boolean retainAll(Collection<?> c, Collection<?> objsToKeep) {
        if (N.isNullOrEmpty(c)) {
            return false;
        } else if (N.isNullOrEmpty(objsToKeep)) {
            c.clear();
            return true;
        }

        if (c instanceof HashSet && !(objsToKeep instanceof Set) && (c.size() > 9 || objsToKeep.size() > 9)) {
            return c.retainAll(new HashSet<>(objsToKeep));
        } else {
            return c.retainAll(objsToKeep);
        }
    }

    /**
     * 
     * @param iterable
     * @return
     * throws DuplicatedResultException if there are more than one elements in the specified {@code iterable}.
     */
    public static <T> Nullable<T> getOnlyElement(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return Nullable.empty();
        }

        return Iterators.getOnlyElement(iterable.iterator());
    }

    /**
     * 
     * @param c
     * @param objToFind
     * @return
     */
    public static OptionalInt indexOf(final Collection<?> c, final Object objToFind) {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (Object e : c) {
            if (N.equals(e, objToFind)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    /**
     * 
     * @param c
     * @param objToFind
     * @return
     */
    public static OptionalInt lastIndexOf(final Collection<?> c, final Object objToFind) {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        final int size = c.size();

        if (c instanceof List) {
            final List<Object> list = (List<Object>) c;

            if (c instanceof RandomAccess) {
                for (int i = size - 1; i >= 0; i--) {
                    if (N.equals(list.get(i), objToFind)) {
                        return OptionalInt.of(i);
                    }
                }
            } else {
                final ListIterator<Object> iter = list.listIterator(list.size());

                for (int i = size - 1; iter.hasPrevious(); i--) {
                    if (N.equals(iter.previous(), objToFind)) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else if (c instanceof Deque) {
            final Iterator<Object> iter = ((Deque<Object>) c).descendingIterator();

            for (int i = size - 1; iter.hasNext(); i--) {
                if (N.equals(iter.next(), objToFind)) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        } else {
            final Object[] a = c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if (N.equals(a[i], objToFind)) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }
    }

    public static <T, E extends Exception> OptionalInt findFirstIndex(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    public static <T, E extends Exception> OptionalInt findFirstIndex(final Collection<? extends T> c, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (T e : c) {
            if (predicate.test(e)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    public static <T, E extends Exception> OptionalInt findLastIndex(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    public static <T, E extends Exception> OptionalInt findLastIndex(final Collection<? extends T> c, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        final int size = c.size();

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                for (int i = size - 1; i >= 0; i--) {
                    if (predicate.test(list.get(i))) {
                        return OptionalInt.of(i);
                    }
                }
            } else {
                final ListIterator<T> iter = list.listIterator(list.size());

                for (int i = size - 1; iter.hasPrevious(); i--) {
                    if (predicate.test(iter.previous())) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else if (c instanceof Deque) {
            final Iterator<T> iter = ((Deque<T>) c).descendingIterator();

            for (int i = size - 1; iter.hasNext(); i--) {
                if (predicate.test(iter.next())) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        } else {
            final T[] a = (T[]) c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if (predicate.test(a[i])) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }
    }

    public static <T, E extends Exception> Nullable<T> findFirst(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Nullable.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i])) {
                return Nullable.of(a[i]);
            }
        }

        return Nullable.empty();
    }

    public static <T, E extends Exception> Nullable<T> findFirst(final Collection<T> c, Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(c)) {
            return Nullable.empty();
        }

        for (T e : c) {
            if (predicate.test(e)) {
                return Nullable.of(e);
            }
        }

        return Nullable.empty();
    }

    public static <T, E extends Exception> Nullable<T> findLast(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Nullable.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return Nullable.of(a[i]);
            }
        }

        return Nullable.empty();
    }

    public static <T, E extends Exception> Nullable<T> findLast(final Collection<T> c, Try.Predicate<? super T, E> predicate) throws E {
        return findLast(c, predicate, false);
    }

    public static <T, E extends Exception> Optional<T> findFirstNonNull(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (a[i] != null && predicate.test(a[i])) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    public static <T, E extends Exception> Optional<T> findFirstNonNull(final Collection<T> c, Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(c)) {
            return Optional.empty();
        }

        for (T e : c) {
            if (e != null && predicate.test(e)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    public static <T, E extends Exception> Optional<T> findLastNonNull(final T[] a, final Try.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (a[i] != null && predicate.test(a[i])) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    public static <T, E extends Exception> Optional<T> findLastNonNull(final Collection<T> c, Try.Predicate<? super T, E> predicate) throws E {
        return findLast(c, predicate, true);
    }

    private static <T, R, E extends Exception> R findLast(final Collection<T> c, Try.Predicate<? super T, E> predicate, boolean isForNonNull) throws E {
        if (N.isNullOrEmpty(c)) {
            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        }

        T e = null;

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                for (int i = c.size() - 1; i >= 0; i--) {
                    e = list.get(i);

                    if ((!isForNonNull || e != null) && predicate.test(e)) {
                        return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                    }
                }
            } else {
                final ListIterator<T> iter = list.listIterator(list.size());

                while (iter.hasPrevious()) {
                    e = iter.previous();

                    if ((!isForNonNull || e != null) && predicate.test(e)) {
                        return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                    }
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        } else if (c instanceof Deque) {
            final Iterator<T> iter = ((Deque<T>) c).descendingIterator();

            while (iter.hasNext()) {
                e = iter.next();

                if ((!isForNonNull || e != null) && predicate.test(e)) {
                    return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        } else {
            final T[] a = (T[]) c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if ((!isForNonNull || a[i] != null) && predicate.test(a[i])) {
                    return (R) (isForNonNull ? Optional.of(a[i]) : Nullable.of(a[i]));
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        }
    }

    public static <T> List<List<T>> rollup(Collection<? extends T> c) {
        final List<List<T>> res = new ArrayList<>();
        res.add(new ArrayList<T>());

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                final List<T> prev = res.get(res.size() - 1);
                List<T> cur = new ArrayList<>(prev.size() + 1);
                cur.addAll(prev);
                cur.add(e);
                res.add(cur);
            }
        }

        return res;
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns the set of all possible subsets of {@code set}. For example,
     * {@code powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{},
     * {1}, {2}, {1, 2}}}.
     *
     * <p>Elements appear in these subsets in the same iteration order as they
     * appeared in the input set. The order in which these subsets appear in the
     * outer set is undefined. Note that the power set of the empty set is not the
     * empty set, but a one-element set containing the empty set.
     *
     * <p>The returned set and its constituent sets use {@code equals} to decide
     * whether two elements are identical, even if the input set uses a different
     * concept of equivalence.
     *
     * <p><i>Performance notes:</i> while the power set of a set with size {@code
     * n} is of size {@code 2^n}, its memory usage is only {@code O(n)}. When the
     * power set is constructed, the input set is merely copied. Only as the
     * power set is iterated are the individual subsets created, and these subsets
     * themselves occupy only a small constant amount of memory.
     *
     * @param set the set of elements to construct a power set from
     * @return the power set, as an immutable set of immutable sets
     * @throws IllegalArgumentException if {@code set} has more than 30 unique
     *     elements (causing the power set size to exceed the {@code int} range)
     * @throws NullPointerException if {@code set} is or contains {@code null}
     * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
     *      Wikipedia</a>
     */
    public static <E> Set<Set<E>> powerSet(Set<E> set) {
        return new PowerSet<>(set);
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Collection}.
     *
     * <p><i>Notes:</i> This is an implementation of the Plain Changes algorithm
     * for permutations generation, described in Knuth's "The Art of Computer
     * Programming", Volume 4, Chapter 7, Section 7.2.1.2.
     *
     * <p>If the input list contains equal elements, some of the generated
     * permutations will be equal.
     *
     * <p>An empty collection has only one permutation, which is an empty list.
     *
     * @param elements the original collection whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original collection.
     * @throws NullPointerException if the specified collection is null or has any
     *     null elements.
     */
    public static <E> Collection<List<E>> permutations(Collection<E> elements) {
        return new PermutationCollection<E>(elements);
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Iterable}.
     *
     * <p><i>Notes:</i> This is an implementation of the algorithm for
     * Lexicographical Permutations Generation, described in Knuth's "The Art of
     * Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2. The
     * iteration order follows the lexicographical order. This means that
     * the first permutation will be in ascending order, and the last will be in
     * descending order.
     *
     * <p>Duplicate elements are considered equal. For example, the list [1, 1]
     * will have only one permutation, instead of two. This is why the elements
     * have to implement {@link Comparable}.
     *
     * <p>An empty iterable has only one permutation, which is an empty list.
     *
     * <p>This method is equivalent to
     * {@code Collections2.orderedPermutations(list, Ordering.natural())}.
     *
     * @param elements the original iterable whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException if the specified iterable is null or has any
     *     null elements.
     */
    public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(Collection<E> elements) {
        return orderedPermutations(elements, Comparators.naturalOrder());
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Iterable} using the specified {@link Comparator} for establishing
     * the lexicographical ordering.
     *
     * <p>Examples: <pre>   {@code
     *
     *   for (List<String> perm : orderedPermutations(asList("b", "c", "a"))) {
     *     println(perm);
     *   }
     *   // -> ["a", "b", "c"]
     *   // -> ["a", "c", "b"]
     *   // -> ["b", "a", "c"]
     *   // -> ["b", "c", "a"]
     *   // -> ["c", "a", "b"]
     *   // -> ["c", "b", "a"]
     *
     *   for (List<Integer> perm : orderedPermutations(asList(1, 2, 2, 1))) {
     *     println(perm);
     *   }
     *   // -> [1, 1, 2, 2]
     *   // -> [1, 2, 1, 2]
     *   // -> [1, 2, 2, 1]
     *   // -> [2, 1, 1, 2]
     *   // -> [2, 1, 2, 1]
     *   // -> [2, 2, 1, 1]}</pre>
     *
     * <p><i>Notes:</i> This is an implementation of the algorithm for
     * Lexicographical Permutations Generation, described in Knuth's "The Art of
     * Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2. The
     * iteration order follows the lexicographical order. This means that
     * the first permutation will be in ascending order, and the last will be in
     * descending order.
     *
     * <p>Elements that compare equal are considered equal and no new permutations
     * are created by swapping them.
     *
     * <p>An empty iterable has only one permutation, which is an empty list.
     *
     * @param elements the original iterable whose elements have to be permuted.
     * @param comparator a comparator for the iterable's elements.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException If the specified iterable is null, has any
     *     null elements, or if the specified comparator is null.
     */
    public static <E> Collection<List<E>> orderedPermutations(Collection<E> elements, Comparator<? super E> comparator) {
        return new OrderedPermutationCollection<E>(elements, comparator);
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns every possible list that can be formed by choosing one element
     * from each of the given lists in order; the "n-ary
     * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
     * product</a>" of the lists. For example: <pre>   {@code
     *
     *   Lists.cartesianProduct(ImmutableList.of(
     *       ImmutableList.of(1, 2),
     *       ImmutableList.of("A", "B", "C")))}</pre>
     *
     * <p>returns a list containing six lists in the following order:
     *
     * <ul>
     * <li>{@code ImmutableList.of(1, "A")}
     * <li>{@code ImmutableList.of(1, "B")}
     * <li>{@code ImmutableList.of(1, "C")}
     * <li>{@code ImmutableList.of(2, "A")}
     * <li>{@code ImmutableList.of(2, "B")}
     * <li>{@code ImmutableList.of(2, "C")}
     * </ul>
     *
     * <p>The result is guaranteed to be in the "traditional", lexicographical
     * order for Cartesian products that you would get from nesting for loops:
     * <pre>   {@code
     *
     *   for (B b0 : lists.get(0)) {
     *     for (B b1 : lists.get(1)) {
     *       ...
     *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     *       // operate on tuple
     *     }
     *   }}</pre>
     *
     * <p>Note that if any input list is empty, the Cartesian product will also be
     * empty. If no lists at all are provided (an empty list), the resulting
     * Cartesian product has one element, an empty list (counter-intuitive, but
     * mathematically consistent).
     *
     * <p><i>Performance notes:</i> while the cartesian product of lists of size
     * {@code m, n, p} is a list of size {@code m x n x p}, its actual memory
     * consumption is much smaller. When the cartesian product is constructed, the
     * input lists are merely copied. Only as the resulting list is iterated are
     * the individual lists created, and these are not retained after iteration.
     *
     * @param cs the lists to choose elements from, in the order that
     *     the elements chosen from those lists should appear in the resulting
     *     lists
     * @param <E> any common base class shared by all axes (often just {@link
     *     Object})
     * @return the Cartesian product, as an immutable list containing immutable
     *     lists
     * @throws IllegalArgumentException if the size of the cartesian product would
     *     be greater than {@link Integer#MAX_VALUE}
     * @throws NullPointerException if {@code lists}, any one of the
     *     {@code lists}, or any element of a provided list is null
     */
    @SafeVarargs
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends E>... cs) {
        return cartesianProduct(Arrays.asList(cs));
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     * 
     * Returns every possible list that can be formed by choosing one element
     * from each of the given lists in order; the "n-ary
     * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
     * product</a>" of the lists. For example: <pre>   {@code
     *
     *   Lists.cartesianProduct(ImmutableList.of(
     *       ImmutableList.of(1, 2),
     *       ImmutableList.of("A", "B", "C")))}</pre>
     *
     * <p>returns a list containing six lists in the following order:
     *
     * <ul>
     * <li>{@code ImmutableList.of(1, "A")}
     * <li>{@code ImmutableList.of(1, "B")}
     * <li>{@code ImmutableList.of(1, "C")}
     * <li>{@code ImmutableList.of(2, "A")}
     * <li>{@code ImmutableList.of(2, "B")}
     * <li>{@code ImmutableList.of(2, "C")}
     * </ul>
     *
     * <p>The result is guaranteed to be in the "traditional", lexicographical
     * order for Cartesian products that you would get from nesting for loops:
     * <pre>   {@code
     *
     *   for (B b0 : lists.get(0)) {
     *     for (B b1 : lists.get(1)) {
     *       ...
     *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     *       // operate on tuple
     *     }
     *   }}</pre>
     *
     * <p>Note that if any input list is empty, the Cartesian product will also be
     * empty. If no lists at all are provided (an empty list), the resulting
     * Cartesian product has one element, an empty list (counter-intuitive, but
     * mathematically consistent).
     *
     * <p><i>Performance notes:</i> while the cartesian product of lists of size
     * {@code m, n, p} is a list of size {@code m x n x p}, its actual memory
     * consumption is much smaller. When the cartesian product is constructed, the
     * input lists are merely copied. Only as the resulting list is iterated are
     * the individual lists created, and these are not retained after iteration.
     *
     * @param cs the lists to choose elements from, in the order that
     *     the elements chosen from those lists should appear in the resulting
     *     lists
     * @param <E> any common base class shared by all axes (often just {@link
     *     Object})
     * @return the Cartesian product, as an immutable list containing immutable
     *     lists
     * @throws IllegalArgumentException if the size of the cartesian product would
     *     be greater than {@link Integer#MAX_VALUE}
     * @throws NullPointerException if {@code lists}, any one of the {@code lists},
     *     or any element of a provided list is null
     */
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends Collection<? extends E>> cs) {
        return new CartesianList<>(cs);
    }

    /**
     * Returns {@code true} if the second list is a permutation of the first.
     */
    private static boolean isPermutations(Collection<?> a, Collection<?> b) {
        if (a.size() != b.size()) {
            return false;
        }

        return N.difference(a, b).size() == 0;
    }

    private static final class PowerSet<E> extends AbstractSet<Set<E>> {
        final ImmutableMap<E, Integer> inputSet;

        PowerSet(Set<E> input) {
            this.inputSet = indexMap(input);
            N.checkArgument(inputSet.size() <= 30, "Too many elements to create power set: %s > 30", inputSet.size());
        }

        @Override
        public int size() {
            return 1 << inputSet.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Set<E>> iterator() {
            return new Iterator<Set<E>>() {
                private final int size = size();
                private int position;

                @Override
                public boolean hasNext() {
                    return position < size;
                }

                @Override
                public Set<E> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    return new SubSet<>(inputSet, position++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Set) {
                Set<?> set = (Set<?>) obj;
                return inputSet.keySet().containsAll(set);
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PowerSet) {
                PowerSet<?> that = (PowerSet<?>) obj;
                return inputSet.equals(that.inputSet);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            /*
             * The sum of the sums of the hash codes in each subset is just the sum of
             * each input element's hash code times the number of sets that element
             * appears in. Each element appears in exactly half of the 2^n sets, so:
             */
            return inputSet.keySet().hashCode() << (inputSet.size() - 1);
        }

        @Override
        public String toString() {
            return "powerSet(" + inputSet + ")";
        }

        /**
         * Returns a map from the ith element of list to i.
         */
        private static <E> ImmutableMap<E, Integer> indexMap(Collection<E> c) {
            final Map<E, Integer> map = new LinkedHashMap<>();

            int i = 0;

            for (E e : c) {
                map.put(e, i++);
            }

            return ImmutableMap.of(map);
        }
    }

    private static final class SubSet<E> extends AbstractSet<E> {
        private final ImmutableMap<E, Integer> inputSet;
        private final ImmutableList<E> elements;
        private final int mask;

        SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
            this.inputSet = inputSet;
            this.elements = ImmutableList.of((E[]) inputSet.keySet().toArray());
            this.mask = mask;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                int remainingSetBits = mask;

                @Override
                public boolean hasNext() {
                    return remainingSetBits != 0;
                }

                @Override
                public E next() {
                    int index = Integer.numberOfTrailingZeros(remainingSetBits);
                    if (index == 32) {
                        throw new NoSuchElementException();
                    }
                    remainingSetBits &= ~(1 << index);
                    return elements.get(index);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return Integer.bitCount(mask);
        }

        @Override
        public boolean contains(Object o) {
            Integer index = inputSet.get(o);
            return index != null && (mask & (1 << index)) != 0;
        }
    }

    private static final class PermutationCollection<E> extends AbstractCollection<List<E>> {
        final List<E> inputList;

        PermutationCollection(Collection<E> input) {
            this.inputList = new ArrayList<>(input);
        }

        @Override
        public int size() {
            return Matth.factorial(inputList.size());
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.of(inputList);
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }

            return false;
        }

        @Override
        public String toString() {
            return "permutations(" + inputList + ")";
        }
    }

    private static final class OrderedPermutationCollection<E> extends AbstractCollection<List<E>> {
        final List<E> inputList;
        final Comparator<? super E> comparator;
        final int size;

        OrderedPermutationCollection(Collection<E> input, Comparator<? super E> comparator) {
            this.inputList = new ArrayList<E>(input);
            N.sort(inputList, comparator);
            this.comparator = comparator;
            this.size = calculateSize(inputList, comparator);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.ordered(inputList, comparator);
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }
            return false;
        }

        @Override
        public String toString() {
            return "orderedPermutationCollection(" + inputList + ")";
        }

        /**
         * The number of permutations with repeated elements is calculated as
         * follows:
         * <ul>
         * <li>For an empty list, it is 1 (base case).</li>
         * <li>When r numbers are added to a list of n-r elements, the number of
         * permutations is increased by a factor of (n choose r).</li>
         * </ul>
         */
        private static <E> int calculateSize(List<E> sortedInputList, Comparator<? super E> comparator) {
            long permutations = 1;
            int n = 1;
            int r = 1;
            while (n < sortedInputList.size()) {
                int comparison = comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n));

                if (comparison < 0) {
                    // We move to the next non-repeated element.
                    permutations *= Matth.binomial(n, r);
                    r = 0;
                    if (!isPositiveInt(permutations)) {
                        return Integer.MAX_VALUE;
                    }
                }

                n++;
                r++;
            }

            permutations *= Matth.binomial(n, r);

            if (!isPositiveInt(permutations)) {
                return Integer.MAX_VALUE;
            }

            return (int) permutations;
        }

        private static boolean isPositiveInt(long n) {
            return n >= 0 && n <= Integer.MAX_VALUE;
        }
    }

    private static final class CartesianList<E> extends AbstractList<List<E>> implements RandomAccess {
        private final transient Object[][] axes;
        private final transient int[] axesSizeProduct;

        CartesianList(final Collection<? extends Collection<? extends E>> cs) {
            final Iterator<? extends Collection<? extends E>> iter = cs.iterator();
            this.axes = new Object[cs.size()][];

            for (int i = 0, len = this.axes.length; i < len; i++) {
                this.axes[i] = iter.next().toArray();
            }

            this.axesSizeProduct = new int[axes.length + 1];
            axesSizeProduct[axes.length] = 1;

            try {
                for (int i = axes.length - 1; i >= 0; i--) {
                    axesSizeProduct[i] = Matth.multiplyExact(axesSizeProduct[i + 1], axes[i].length);
                }
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Cartesian product too large; must have size at most Integer.MAX_VALUE");
            }
        }

        @Override
        public List<E> get(final int index) {
            N.checkArgument(index < size(), "Invalid index %s. It must be less than the size %s", index, size());

            final List<E> result = new ArrayList<>(axes.length);

            for (int k = 0, len = axes.length; k < len; k++) {
                result.add((E) axes[k][getAxisIndexForProductIndex(index, k)]);
            }

            return result;
        }

        @Override
        public int size() {
            return axesSizeProduct[0];
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Collection)) {
                return false;
            }

            final Collection<?> c = (Collection<?>) obj;

            if (c.size() != axes.length) {
                return false;
            }

            int idx = 0;
            for (Object e : c) {
                boolean found = false;

                for (Object p : axes[idx++]) {
                    if (N.equals(e, p)) {
                        found = true;
                        break;
                    }
                }

                if (found == false) {
                    return false;
                }
            }

            return true;
        }

        private int getAxisIndexForProductIndex(int index, int axis) {
            return (index / axesSizeProduct[axis + 1]) % axes[axis].length;
        }
    }

    public static <T, E extends Exception> void parse(final Iterator<? extends T> iter, final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iter, elementParser, Fn.emptyAction());
    }

    public static <T, E extends Exception, E2 extends Exception> void parse(final Iterator<? extends T> iter, final Try.Consumer<? super T, E> elementParser,
            final Try.Runnable<E2> onComplete) throws E, E2 {
        parse(iter, 0, Long.MAX_VALUE, elementParser, onComplete);
    }

    public static <T, E extends Exception> void parse(final Iterator<? extends T> iter, final long offset, final long count,
            final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iter, offset, count, elementParser, Fn.emptyAction());
    }

    public static <T, E extends Exception, E2 extends Exception> void parse(final Iterator<? extends T> iter, final long offset, final long count,
            final Try.Consumer<? super T, E> elementParser, final Try.Runnable<E2> onComplete) throws E, E2 {
        parse(iter, offset, count, 0, 0, elementParser, onComplete);
    }

    public static <T, E extends Exception> void parse(final Iterator<? extends T> iter, long offset, long count, final int processThreadNum,
            final int queueSize, final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iter, offset, count, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     * 
     * @param iter
     * @param offset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementParser.
     * @param onComplete
     * @param onError
     */
    public static <T, E extends Exception, E2 extends Exception> void parse(final Iterator<? extends T> iter, long offset, long count,
            final int processThreadNum, final int queueSize, final Try.Consumer<? super T, E> elementParser, final Try.Runnable<E2> onComplete) throws E, E2 {
        parse(N.asList(iter), offset, count, 0, processThreadNum, queueSize, elementParser, onComplete);
    }

    public static <T, E extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators,
            final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iterators, elementParser, Fn.emptyAction());
    }

    public static <T, E extends Exception, E2 extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators,
            final Try.Consumer<? super T, E> elementParser, final Try.Runnable<E2> onComplete) throws E, E2 {
        parse(iterators, 0, Long.MAX_VALUE, elementParser, onComplete);
    }

    public static <T, E extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iterators, offset, count, elementParser, Fn.emptyAction());
    }

    public static <T, E extends Exception, E2 extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Try.Consumer<? super T, E> elementParser, final Try.Runnable<E2> onComplete) throws E, E2 {
        parse(iterators, offset, count, 0, 0, 0, elementParser, onComplete);
    }

    public static <T, E extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iterators, readThreadNum, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    public static <T, E extends Exception, E2 extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Try.Consumer<? super T, E> elementParser,
            final Try.Runnable<E2> onComplete) throws E {
        parse(iterators, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, elementParser);
    }

    public static <T, E extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Try.Consumer<? super T, E> elementParser) throws E {
        parse(iterators, offset, count, readThreadNum, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     * 
     * @param iterators
     * @param offset
     * @param count
     * @param readThreadNum new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementParser.
     * @param onComplete
     */
    public static <T, E extends Exception, E2 extends Exception> void parse(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreadNum, final int processThreadNum, final int queueSize, final Try.Consumer<? super T, E> elementParser,
            final Try.Runnable<E2> onComplete) throws E, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can not be negative", offset, count);

        if (N.isNullOrEmpty(iterators)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("### Start to parse");
        }

        try (final Stream<T> stream = ((readThreadNum > 0 || queueSize > 0)
                ? Stream.parallelConcatt(iterators, (readThreadNum == 0 ? 1 : readThreadNum), (queueSize == 0 ? 1024 : queueSize))
                : Stream.concatt(iterators))) {

            final Iterator<? extends T> iteratorII = stream.skip(offset).limit(count).iterator();

            if (processThreadNum == 0) {
                while (iteratorII.hasNext()) {
                    elementParser.accept(iteratorII.next());
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                final AtomicInteger activeThreadNum = new AtomicInteger();
                final ExecutorService executorService = Executors.newFixedThreadPool(processThreadNum);
                final Holder<Throwable> errorHolder = new Holder<>();

                for (int i = 0; i < processThreadNum; i++) {
                    activeThreadNum.incrementAndGet();

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            T element = null;
                            try {
                                while (errorHolder.value() == null) {
                                    synchronized (iteratorII) {
                                        if (iteratorII.hasNext()) {
                                            element = iteratorII.next();
                                        } else {
                                            break;
                                        }
                                    }

                                    elementParser.accept(element);
                                }
                            } catch (Exception e) {
                                synchronized (errorHolder) {
                                    if (errorHolder.value() == null) {
                                        errorHolder.setValue(e);
                                    } else {
                                        errorHolder.value().addSuppressed(e);
                                    }
                                }
                            } finally {
                                activeThreadNum.decrementAndGet();
                            }
                        }
                    });
                }

                while (activeThreadNum.get() > 0) {
                    N.sleep(1);
                }

                if (errorHolder.value() == null && onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Exception e) {
                        errorHolder.setValue(e);
                    }
                }

                if (errorHolder.value() != null) {
                    throw N.toRuntimeException(errorHolder.value());
                }
            }
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info("### End to parse");
            }
        }
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCount(5, 7, true, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCount(5, 7, false, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     * 
     * @param maxCount
     * @param totalSize
     * @param smallerFirst
     * @param func
     * @return
     */
    public static <T> Stream<T> splitByCount(final int maxCount, final int totalSize, final boolean smallerFirst, final IntBiFunction<T> func) {
        if (smallerFirst) {
            return Iterables.splitByCountSmallerFirst(maxCount, totalSize, func);
        } else {
            return Iterables.splitByCountSmallerLast(maxCount, totalSize, func);
        }
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCountSmallerFirst(5, 7, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCountSmallerLast(5, 7, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param maxCount
     * @param totalSize
     * @param func
     * @return 
     */
    static <T> Stream<T> splitByCountSmallerFirst(final int maxCount, final int totalSize, final IntBiFunction<T> func) {
        N.checkArgPositive(maxCount, "maxCount");
        N.checkArgNotNegative(totalSize, "totalSize");
        N.checkArgNotNull(func, "func");

        if (totalSize == 0) {
            return Stream.empty();
        }

        final Iterator<T> iter = new ObjIteratorEx<T>() {
            private final int smallerSize = Math.max(totalSize / maxCount, 1);
            private final int biggerSize = totalSize % maxCount == 0 ? totalSize / maxCount : totalSize / maxCount + 1;
            private int count = totalSize >= maxCount ? maxCount : totalSize;
            private int biggerCount = totalSize % maxCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < totalSize;
            }

            @Override
            public T next() {
                if (cursor >= totalSize) {
                    throw new NoSuchElementException();
                }

                return func.apply(cursor, cursor = (count-- > biggerCount ? cursor + smallerSize : cursor + biggerSize));
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                if (n > 0) {
                    while (n-- > 0 && cursor < totalSize) {
                        cursor = count-- > biggerCount ? cursor + smallerSize : cursor + biggerSize;
                    }
                }
            }

            @Override
            public long count() {
                return count;
            }
        };

        return Stream.of(iter);
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCountSmallerFirst(5, 7, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCountSmallerLast(5, 7, (fromIndex, toIndex) ->  N.copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param maxCount
     * @param totalSize
     * @param func
     * @return
     */
    static <T> Stream<T> splitByCountSmallerLast(final int maxCount, final int totalSize, final IntBiFunction<T> func) {
        N.checkArgPositive(maxCount, "maxCount");
        N.checkArgNotNegative(totalSize, "totalSize");
        N.checkArgNotNull(func, "func");

        if (totalSize == 0) {
            return Stream.empty();
        }

        final Iterator<T> iter = new ObjIteratorEx<T>() {
            private final int smallerSize = Math.max(totalSize / maxCount, 1);
            private final int biggerSize = totalSize % maxCount == 0 ? totalSize / maxCount : totalSize / maxCount + 1;
            private int count = totalSize >= maxCount ? maxCount : totalSize;
            private int smallerCount = count - totalSize % maxCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < totalSize;
            }

            @Override
            public T next() {
                if (cursor >= totalSize) {
                    throw new NoSuchElementException();
                }

                return func.apply(cursor, cursor = (count-- > smallerCount ? cursor + biggerSize : cursor + smallerSize));
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                if (n > 0) {
                    while (n-- > 0 && cursor < totalSize) {
                        cursor = count-- > smallerCount ? cursor + biggerSize : cursor + smallerSize;
                    }
                }
            }

            @Override
            public long count() {
                return count;
            }

        };

        return Stream.of(iter);
    }
}
