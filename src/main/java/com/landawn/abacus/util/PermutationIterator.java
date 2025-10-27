/*
 * Copyright (C) 2008 The Guava Authors
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
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.stream.ObjIteratorEx;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 *
 * Provides static methods for generating permutations of collections.
 * This class offers two main approaches for generating permutations:
 * <ul>
 *   <li>Plain Changes algorithm - generates all permutations without considering order</li>
 *   <li>Lexicographical algorithm - generates permutations in a specific order based on a comparator</li>
 * </ul>
 * 
 * <p>All methods return iterators that generate permutations lazily, making them memory-efficient
 * for large collections. The generated lists are new instances and modifications to them do not
 * affect the original collection.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * List<String> items = Arrays.asList("A", "B", "C");
 * ObjIterator<List<String>> perms = PermutationIterator.of(items);
 * while (perms.hasNext()) {
 *     System.out.println(perms.next()); // Prints each permutation
 * }
 * }</pre>
 *
 * @author Chris Povirk
 * @author Mike Bostock
 * @author Jared Levy
 */
public final class PermutationIterator {

    private PermutationIterator() {
    }

    /**
     * Returns an iterator over all permutations of the specified collection.
     * 
     * <p>This implementation uses the Plain Changes algorithm for permutation generation,
     * described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2.</p>
     * 
     * <p>If the input collection contains duplicate elements, some of the generated
     * permutations will be equal. An empty collection has only one permutation: an empty list.</p>
     * 
     * <p>The returned iterator generates permutations lazily and does not store all permutations
     * in memory, making it suitable for large collections.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3);
     * ObjIterator<List<Integer>> perms = PermutationIterator.of(numbers);
     * // Generates: [1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param elements the original collection whose elements have to be permuted
     * @return an iterator containing all the different permutations of the original collection
     * @throws IllegalArgumentException if elements is null
     */
    public static <T> ObjIterator<List<T>> of(final Collection<T> elements) {
        N.checkArgNotNull(elements, cs.elements);

        if (elements.isEmpty()) {
            return new ObjIteratorEx<>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public List<T> next() {
                    if (done) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    done = true;

                    return new ArrayList<>(0);
                }
            };
        }

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

                hasNext = j == 0 ? -1 : 1;
            }

            private void switchDirection() {
                o[j] = -o[j];
                j--;
            }
        };
    }

    /**
     * Returns an iterator over all permutations of the specified collection in lexicographical order.
     * The elements must implement {@link Comparable}.
     * 
     * <p>This is an implementation of the algorithm for Lexicographical Permutations Generation,
     * described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2.
     * The iteration order follows the lexicographical order. This means that the first permutation
     * will be in ascending order, and the last will be in descending order.</p>
     * 
     * <p>Duplicate elements are considered equal. For example, the list [1, 1] will have only
     * one permutation, instead of two. This is why the elements have to implement {@link Comparable}.</p>
     * 
     * <p>An empty collection has only one permutation: an empty list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(3, 1, 2);
     * ObjIterator<List<Integer>> perms = PermutationIterator.ordered(numbers);
     * // Generates: [1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param elements the original iterable whose elements have to be permuted
     * @return an iterator containing all the different permutations of the original iterable
     * @throws IllegalArgumentException if elements is null
     */
    public static <T extends Comparable<? super T>> ObjIterator<List<T>> ordered(final Collection<T> elements) {
        return ordered(elements, Comparators.naturalOrder());
    }

    /**
     * Returns an iterator over all permutations of the specified collection using the specified
     * {@link Comparator} for establishing the lexicographical ordering.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String permutations in alphabetical order
     * List<String> words = Arrays.asList("b", "c", "a");
     * ObjIterator<List<String>> perms = PermutationIterator.ordered(words, String::compareTo);
     * // Generates: [a,b,c], [a,c,b], [b,a,c], [b,c,a], [c,a,b], [c,b,a]
     * 
     * // Handling duplicates
     * List<Integer> numbers = Arrays.asList(1, 2, 2, 1);
     * ObjIterator<List<Integer>> perms2 = PermutationIterator.ordered(numbers, Integer::compare);
     * // Generates: [1,1,2,2], [1,2,1,2], [1,2,2,1], [2,1,1,2], [2,1,2,1], [2,2,1,1]
     * }</pre>
     * 
     * <p>This is an implementation of the algorithm for Lexicographical Permutations Generation,
     * described in Knuth's "The Art of Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2.
     * The iteration order follows the lexicographical order defined by the comparator.</p>
     * 
     * <p>Elements that compare as equal are considered identical and no new permutations
     * are created by swapping them.</p>
     * 
     * <p>An empty collection has only one permutation: an empty list.</p>
     *
     * @param <T> the type of elements in the collection
     * @param elements the original iterable whose elements have to be permuted
     * @param comparator a comparator for the iterable's elements
     * @return an iterator containing all the different permutations of the original iterable
     * @throws IllegalArgumentException if elements or comparator is null
     */
    public static <T> ObjIterator<List<T>> ordered(final Collection<T> elements, final Comparator<? super T> comparator) {
        N.checkArgNotNull(elements, cs.elements);

        if (elements.isEmpty()) {
            return new ObjIteratorEx<>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public List<T> next() {
                    if (done) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    done = true;

                    return new ArrayList<>(0);
                }
            };
        }

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
