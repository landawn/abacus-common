/*
 * Copyright (c) 2026, Haiyang Li.
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

import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A small utility facade for selecting {@code null}-tolerant values that returns the chosen element
 * <em>directly</em> instead of wrapping it in an {@link com.landawn.abacus.util.u.Optional}.
 *
 * <p>This class groups two related families of helpers:
 * <ul>
 *   <li><b>Non-{@code null} selection</b> &mdash; {@link #firstNonNull firstNonNull} /
 *       {@link #lastNonNull lastNonNull} scan two or three arguments, an array, an {@link Iterable}, or an
 *       {@link Iterator} and return the first (respectively last) element that is not {@code null}. They
 *       return {@code null} when every candidate is {@code null}, or when the source is {@code null} or
 *       empty.</li>
 *   <li><b>Positional first element</b> &mdash; {@link #firstElement firstElement} returns the element at
 *       position&nbsp;0 of an array, {@link Iterable}, or {@link Iterator}, or {@code null} when the source
 *       is {@code null} or empty. Unlike {@code firstNonNull}, it does <em>not</em> skip {@code null}: a
 *       present first element that happens to be {@code null} is returned as {@code null}, which is then
 *       indistinguishable from the "empty source" case.</li>
 * </ul>
 *
 * <p><b>Relationship to {@link N}:</b> the {@link N} facade exposes same-named {@code firstNonNull} /
 * {@code lastNonNull} methods that return an {@link com.landawn.abacus.util.u.Optional Optional&lt;T&gt;},
 * which cleanly distinguishes "no {@code non-null} element was found" from "a present element whose value
 * is {@code null}". The methods here deliberately trade that distinction away for a terser, wrapper-free
 * direct return; prefer them when {@code null} simply means "absent" in your domain and an {@code Optional}
 * would only add noise. Each method links to its {@code N} counterpart via {@code @see}.</p>
 *
 * <p><b>Null-safety, exceptions, and thread-safety:</b> every method is {@code null}-safe &mdash; a
 * {@code null} array, {@link Iterable}, or {@link Iterator} yields {@code null} rather than throwing &mdash;
 * and each may return {@code null} (marked {@link MayReturnNull}). Iterable/iterator inputs are traversed
 * at most once, so single-use sources (such as a stream's iterator) are handled correctly. All methods are
 * {@link Beta @Beta}. The class holds no state, is therefore thread-safe, and cannot be instantiated.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Nulls.firstNonNull(null, "b", "c");                  // returns "b"
 * Nulls.lastNonNull("a", "b", null);                   // returns "b"
 * Nulls.firstNonNull(Arrays.asList(null, null, "x"));  // returns "x"
 * Nulls.firstElement(Arrays.asList(null, "x"));        // returns null (position 0 holds null)
 * }</pre>
 *
 * @see N
 * @see Iterables
 * @see com.landawn.abacus.util.u.Optional
 */
@Beta
public final class Nulls {
    private Nulls() {
        // utility class
    }

    // Moved from Iterables. These methods intentionally return the selected value directly.
    /**
     * Returns the first {@code non-null} element from the provided two elements.
     * If both are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#firstNonNull(Object, Object)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.firstNonNull("hello", "world");   // returns "hello"
     * String result2 = Nulls.firstNonNull(null, "world");     // returns "world"
     * String result3 = Nulls.firstNonNull(null, null);        // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the first element to evaluate.
     * @param b the second element to evaluate.
     * @return the first {@code non-null} element, or {@code null} if both are {@code null}.
     * @see N#firstNonNull(Object, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstNonNull(final T a, final T b) {
        return a != null ? a : b;
    }

    /**
     * Returns the first {@code non-null} element from the provided three elements.
     * If all are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#firstNonNull(Object, Object, Object)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.firstNonNull("hello", "world", "test");   // returns "hello"
     * String result2 = Nulls.firstNonNull(null, "world", "test");     // returns "world"
     * String result3 = Nulls.firstNonNull(null, null, "test");        // returns "test"
     * String result4 = Nulls.firstNonNull(null, null, null);          // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the first element to evaluate.
     * @param b the second element to evaluate.
     * @param c the third element to evaluate.
     * @return the first {@code non-null} element, or {@code null} if all are {@code null}.
     * @see N#firstNonNull(Object, Object, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstNonNull(final T a, final T b, final T c) {
        return a != null ? a : (b != null ? b : c);
    }

    /**
     * Returns the first {@code non-null} element from the provided array of elements.
     * If the array is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#firstNonNull(Object[])} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.firstNonNull("a", "b", "c");           // returns "a"
     * String result2 = Nulls.firstNonNull(null, null, "c", "d");   // returns "c"
     * String result3 = Nulls.firstNonNull(null, null, null);       // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the array of elements to evaluate.
     * @return the first {@code non-null} element, or {@code null} if the array is {@code null} or empty.
     * @see N#firstNonNull(Object[])
     */
    @MayReturnNull
    @Beta
    @SafeVarargs
    public static <T> T firstNonNull(final T... a) {
        if (N.isEmpty(a)) {
            return null;
        }

        for (final T e : a) {
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    /**
     * Returns the first {@code non-null} element from the provided iterable.
     * If the iterable is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#firstNonNull(Iterable)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList(null, null, "first", "second");
     * String result = Nulls.firstNonNull(list);
     * // result => "first"
     *
     * List<Integer> allNulls = Arrays.asList(null, null, null);
     * Integer result2 = Nulls.firstNonNull(allNulls);
     * // result2 => null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param c the iterable of elements to evaluate.
     * @return the first {@code non-null} element, or {@code null} if the iterable is {@code null} or empty.
     * @see N#firstNonNull(Iterable)
     * @see N#firstNonNullOrDefault(Iterable, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstNonNull(final Iterable<? extends T> c) {
        // Null check only: N.isEmpty would call c.iterator() for non-Collection iterables, and the
        // loop below would then call it a second time, breaking single-use iterables (stream::iterator).
        if (c == null) {
            return null;
        }

        for (final T e : c) {
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    /**
     * Returns the first {@code non-null} element from the provided iterator.
     * If the iterator is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#firstNonNull(Iterator)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList(null, "first", "second");
     * String result = Nulls.firstNonNull(list.iterator());   // returns "first"
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param iter the iterator of elements to evaluate.
     * @return the first {@code non-null} element, or {@code null} if the iterator is {@code null} or empty.
     * @see N#firstNonNull(Iterator)
     * @see N#firstNonNullOrDefault(Iterator, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return null;
        }

        T e = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                return e;
            }
        }

        return null;
    }

    /**
     * Returns the last {@code non-null} element from the provided two elements.
     * If both are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#lastNonNull(Object, Object)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.lastNonNull("hello", "world");   // returns "world"
     * String result2 = Nulls.lastNonNull("hello", null);     // returns "hello"
     * String result3 = Nulls.lastNonNull(null, null);        // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the first element to evaluate.
     * @param b the second element to evaluate.
     * @return the last {@code non-null} element, or {@code null} if both are {@code null}.
     * @see N#lastNonNull(Object, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastNonNull(final T a, final T b) {
        return b != null ? b : a;
    }

    /**
     * Returns the last {@code non-null} element from the provided three elements.
     * If all are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#lastNonNull(Object, Object, Object)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.lastNonNull("hello", "world", "test");   // returns "test"
     * String result2 = Nulls.lastNonNull("hello", "world", null);    // returns "world"
     * String result3 = Nulls.lastNonNull("hello", null, null);       // returns "hello"
     * String result4 = Nulls.lastNonNull(null, null, null);          // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the first element to evaluate.
     * @param b the second element to evaluate.
     * @param c the third element to evaluate.
     * @return the last {@code non-null} element, or {@code null} if all are {@code null}.
     * @see N#lastNonNull(Object, Object, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastNonNull(final T a, final T b, final T c) {
        return c != null ? c : (b != null ? b : a);
    }

    /**
     * Returns the last {@code non-null} element from the provided array of elements.
     * If the array is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#lastNonNull(Object[])} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Nulls.lastNonNull("a", "b", "c");       // returns "c"
     * String result2 = Nulls.lastNonNull("a", "b", null);     // returns "b"
     * String result3 = Nulls.lastNonNull(null, null, null);   // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the array of elements to evaluate.
     * @return the last {@code non-null} element, or {@code null} if the array is {@code null} or empty.
     * @see N#lastNonNull(Object[])
     */
    @MayReturnNull
    @Beta
    @SafeVarargs
    public static <T> T lastNonNull(final T... a) {
        if (N.isEmpty(a)) {
            return null;
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] != null) {
                return a[i];
            }
        }

        return null;
    }

    /**
     * Returns the last {@code non-null} element from the provided iterable.
     * If the iterable is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#lastNonNull(Iterable)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("first", "second", null);
     * String result = Nulls.lastNonNull(list);   // returns "second"
     * List<Integer> allNulls = Arrays.asList(null, null, null);
     * Integer result2 = Nulls.lastNonNull(allNulls);   // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param c the iterable of elements to evaluate.
     * @return the last {@code non-null} element, or {@code null} if the iterable is {@code null} or empty.
     * @see N#lastNonNull(Iterable)
     * @see N#lastNonNullOrDefault(Iterable, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastNonNull(final Iterable<? extends T> c) {
        // Null check only: N.isEmpty would call c.iterator() for non-Collection iterables, and the
        // fallback below would then call it a second time, breaking single-use iterables.
        if (c == null) {
            return null;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) != null) {
                    return list.get(i);
                }
            }

            return null;
        }

        final Iterator<T> descendingIterator = N.getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            T next = null;

            while (descendingIterator.hasNext()) {
                if ((next = descendingIterator.next()) != null) {
                    return next;
                }
            }

            return null;
        }

        return lastNonNull(c.iterator());
    }

    /**
     * Returns the last {@code non-null} element from the provided iterator.
     * If the iterator is {@code null} or empty, or all elements are {@code null}, it returns {@code null}.
     *
     * <p>Note: the same-named {@link N#lastNonNull(Iterator)} returns an {@code Optional<T>} instead;
     * this method returns the element itself and yields {@code null} when no {@code non-null} element exists.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("first", "second", null);
     * String result = Nulls.lastNonNull(list.iterator());   // returns "second"
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param iter the iterator of elements to evaluate.
     * @return the last {@code non-null} element, or {@code null} if the iterator is {@code null} or empty.
     * @see N#lastNonNull(Iterator)
     * @see N#lastNonNullOrDefault(Iterator, Object)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return null;
        }

        T e = null;
        T lastNonNull = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                lastNonNull = e;
            }
        }

        return lastNonNull;
    }

    /**
     * Returns the first element of the provided array.
     *
     * <p>If the array is {@code null} or empty, this method returns {@code null}. A present first
     * element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-array case.</p>
     *
     * <p>Unlike {@link #firstNonNull(Object[])}, this method does <em>not</em> skip leading
     * {@code null} elements; it always returns the element at index&nbsp;0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Nulls.firstElement(new String[] {"a", "b"});   // returns "a"
     * Nulls.firstElement(new String[] {null, "b"});  // returns null (index 0 holds null)
     * Nulls.firstElement(new String[0]);             // returns null (empty)
     * Nulls.firstElement((String[]) null);           // returns null
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param a the array to read.
     * @return the first element, which may itself be {@code null}, or {@code null} if the array is {@code null} or empty.
     * @see #firstNonNull(Object[])
     * @see N#firstOrNullIfEmpty(Object[])
     */
    @MayReturnNull
    @Beta
    public static <T> T firstElement(final T[] a) {
        if (N.isEmpty(a)) {
            return null;
        }

        return a[0];
    }

    /**
     * Returns the first element of the provided iterable.
     *
     * <p>If the iterable is {@code null} or empty, this method returns {@code null}. A present
     * first element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-iterable case.</p>
     *
     * <p>Unlike {@link #firstNonNull(Iterable)}, this method does <em>not</em> skip leading
     * {@code null} elements; it always returns the element the iteration yields first. The iterable
     * is traversed at most once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Nulls.firstElement(Arrays.asList("a", "b"));   // returns "a"
     * Nulls.firstElement(Arrays.asList(null, "b"));  // returns null (first element is null)
     * Nulls.firstElement(Collections.emptyList());   // returns null (empty)
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param c the iterable to read.
     * @return the first element, which may itself be {@code null}, or {@code null} if the iterable is {@code null} or empty.
     * @see #firstNonNull(Iterable)
     * @see N#firstOrNullIfEmpty(Iterable)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstElement(final Iterable<? extends T> c) {
        if (c == null) {
            return null;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            return list.isEmpty() ? null : list.get(0);
        }

        final Iterator<? extends T> iter = c.iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    /**
     * Returns the first element of the provided iterator.
     *
     * <p>If the iterator is {@code null} or empty, this method returns {@code null}. A present
     * first element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-iterator case.</p>
     *
     * <p>Unlike {@link #firstNonNull(Iterator)}, this method does <em>not</em> skip leading
     * {@code null} elements; it consumes and returns only the first element (advancing the iterator
     * by at most one).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Nulls.firstElement(Arrays.asList("a", "b").iterator());   // returns "a"
     * Nulls.firstElement(Arrays.asList(null, "b").iterator());  // returns null (first element is null)
     * Nulls.firstElement(Collections.emptyIterator());          // returns null (empty)
     * }</pre>
     *
     * @param <T> the type of the elements.
     * @param iter the iterator to read.
     * @return the first element, which may itself be {@code null}, or {@code null} if the iterator is {@code null} or empty.
     * @see #firstNonNull(Iterator)
     * @see N#firstOrNullIfEmpty(Iterator)
     */
    @MayReturnNull
    @Beta
    public static <T> T firstElement(final Iterator<? extends T> iter) {
        return iter != null && iter.hasNext() ? iter.next() : null;
    }

    /**
     * Returns the last element of the provided array.
     *
     * <p>If the array is {@code null} or empty, this method returns {@code null}. A present last
     * element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-array case.</p>
     *
     * <p>Unlike {@link #lastNonNull(Object[])}, this method does <em>not</em> skip trailing
     * {@code null} elements; it always returns the element at the last index.</p>
     *
     * @param <T> the type of the elements.
     * @param a the array to read.
     * @return the last element, which may itself be {@code null}, or {@code null} if the array is {@code null} or empty.
     * @see #lastNonNull(Object[])
     */
    @MayReturnNull
    @Beta
    public static <T> T lastElement(final T[] a) {
        return N.isEmpty(a) ? null : a[a.length - 1];
    }

    /**
     * Returns the last element of the provided iterable.
     *
     * <p>If the iterable is {@code null} or empty, this method returns {@code null}. A present last
     * element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-iterable case.</p>
     *
     * <p>Unlike {@link #lastNonNull(Iterable)}, this method does <em>not</em> skip trailing
     * {@code null} elements. Non-random-access iterables are traversed once to their end.</p>
     *
     * @param <T> the type of the elements.
     * @param c the iterable to read.
     * @return the last element, which may itself be {@code null}, or {@code null} if the iterable is {@code null} or empty.
     * @see #lastNonNull(Iterable)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastElement(final Iterable<? extends T> c) {
        if (c == null) {
            return null;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }

        return lastElement(c.iterator());
    }

    /**
     * Returns the last element of the provided iterator.
     *
     * <p>If the iterator is {@code null} or empty, this method returns {@code null}. A present last
     * element may itself be {@code null}, in which case {@code null} is returned and is
     * indistinguishable from the empty-iterator case.</p>
     *
     * <p>Unlike {@link #lastNonNull(Iterator)}, this method does <em>not</em> skip trailing
     * {@code null} elements. It consumes the iterator fully.</p>
     *
     * @param <T> the type of the elements.
     * @param iter the iterator to read.
     * @return the last element, which may itself be {@code null}, or {@code null} if the iterator is {@code null} or empty.
     * @see #lastNonNull(Iterator)
     */
    @MayReturnNull
    @Beta
    public static <T> T lastElement(final Iterator<? extends T> iter) {
        if (iter == null || !iter.hasNext()) {
            return null;
        }

        T result;

        do {
            result = iter.next();
        } while (iter.hasNext());

        return result;
    }

}
