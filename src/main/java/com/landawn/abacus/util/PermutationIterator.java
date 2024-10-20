/*
 * Copyright (C) 2008 The Guava Authors
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.stream.ObjIteratorEx;

/**
 * Copied from Google Guava.
 *
 * Provides static methods for working with {@code Collection} instances.
 *
 * @author Chris Povirk
 * @author Mike Bostock
 * @author Jared Levy
 */
public final class PermutationIterator {

    private PermutationIterator() {
    }

    /**
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
     * @param <T>
     * @param elements the original collection whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original collection.
     * @throws NullPointerException if the specified collection is {@code null} or has any
     *     {@code null} elements.
     */
    public static <T> ObjIterator<List<T>> of(final Collection<T> elements) {
        return new ObjIteratorEx<>() {
            final T[] items = (T[]) elements.toArray();
            final int[] c = Array.repeat(0, items.length);
            final int[] o = Array.repeat(1, items.length);
            int j = Integer.MAX_VALUE;
            int hasNext = items.length == 0 ? -1 : 1; // 0 = read; 1 = yes, -1 = done.

            @Override
            public boolean hasNext() {
                switch (hasNext) {
                    case -1:
                        return false;
                    case 1:
                        return true;
                    default:
                        computeNext();
                }

                return hasNext == 1;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = 0;
                return N.asList(items);
            }

            private void computeNext() {
                j = items.length - 1;
                int s = 0;

                while (true) {
                    final int q = c[j] + o[j];
                    if (q < 0) {
                        switchDirection();
                        continue;
                    }

                    if (q == j + 1) {
                        if (j == 0) {
                            break;
                        }
                        s++;
                        switchDirection();
                        continue;
                    }

                    N.swap(items, j - c[j] + s, j - q + s);

                    c[j] = q;
                    break;
                }

                hasNext = j <= 0 ? -1 : 1;
            }

            private void switchDirection() {
                o[j] = -o[j];
                j--;
            }
        };
    }

    /**
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
     * @param <T>
     * @param elements the original iterable whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException if the specified iterable is {@code null} or has any
     *     {@code null} elements.
     */
    public static <T extends Comparable<? super T>> ObjIterator<List<T>> ordered(final Collection<T> elements) {
        return ordered(elements, Comparators.naturalOrder());
    }

    /**
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
     * @param <T>
     * @param elements the original iterable whose elements have to be permuted.
     * @param comparator a comparator for the iterable's elements.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException If the specified iterable is {@code null}, has any
     *     {@code null} elements, or if the specified comparator is {@code null}.
     */
    public static <T> ObjIterator<List<T>> ordered(final Collection<T> elements, final Comparator<? super T> comparator) {
        return new ObjIteratorEx<>() {
            T[] next = (T[]) elements.toArray();
            { //NOSONAR
                N.sort(next, comparator);
            }

            int hasNext = next.length == 0 ? -1 : 1; // 0 = read; 1 = yes, -1 = done.

            @Override
            public boolean hasNext() {
                switch (hasNext) {
                    case -1:
                        return false;
                    case 1:
                        return true;
                    default:
                        computeNext();
                }

                return hasNext == 1;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = 0;
                return N.asList(next);
            }

            private void computeNext() {
                if (next == null) {
                    hasNext = -1;
                    return;
                }

                final int j = findNextJ();

                if (j == -1) {
                    next = null;
                    hasNext = -1;
                    return;
                }

                final int l = findNextL(j);
                N.swap(next, j, l);
                final int n = next.length;
                N.reverse(next, j + 1, n);

                hasNext = 1;
            }

            private int findNextJ() {
                for (int k = next.length - 2; k >= 0; k--) {
                    if (comparator.compare(next[k], next[k + 1]) < 0) {
                        return k;
                    }
                }
                return -1;
            }

            private int findNextL(final int j) {
                final T ak = next[j];

                for (int l = next.length - 1; l > j; l--) {
                    if (comparator.compare(ak, next[l]) < 0) {
                        return l;
                    }
                }

                throw new AssertionError("this statement should be unreachable");
            }
        };
    }
}
