/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;

/**
 * A fluent alternative to {@code if}/{@code else} for functional code: a condition is evaluated once by a static
 * factory, and the resulting object runs one action or the other.
 *
 * <p><b>&#9888;&#65039; Prefer a plain {@code if} statement.</b> This class exists for the cases where a chain reads
 * better - a long validation sequence, or conditional logic that would otherwise interrupt a functional pipeline. For
 * an ordinary two-branch decision, {@code if}/{@code else} or a ternary is clearer and cheaper.
 *
 * <p><b>The shape.</b> A factory ({@link #is(boolean)}, {@link #notNull(Object)}, {@link #notEmpty(Collection)},
 * {@link #notBlank(CharSequence)}, ...) produces an {@code If}; {@link #then(Throwables.Runnable)} runs the true
 * branch and returns an {@link OrElse}; {@link OrElse#orElse(Throwables.Runnable)} runs the false branch.
 * {@link #thenThrow(Supplier)} and {@link OrElse#orElseThrow(Supplier)} are the throwing counterparts - note that
 * {@code thenThrow} returns an {@code OrElse} only when the condition was false, since a true condition leaves by
 * throwing. Every branch is optional - a chain may stop at any point, and {@link #thenDoNothing()} /
 * {@link OrElse#orElseDoNothing()} exist to say so explicitly.
 *
 * <p>Actions may throw checked exceptions: the exception type is a type parameter of {@code then}/{@code orElse}, so
 * it propagates to the caller without wrapping. The action arguments themselves are validated eagerly - passing
 * {@code null} raises {@link IllegalArgumentException} whether or not that branch is the one taken.
 *
 * <p>An {@code If} carries only the evaluated {@code boolean}, so it is immutable, allocation-free (the two instances
 * are cached), and safe to share; whether the actions it runs are safe is up to the actions. Note that the condition
 * has already been computed by the time the factory returns - only the <i>actions</i> are deferred.
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Two branches
 * If.is(user.isActive())
 *   .then(() -> sendWelcomeEmail(user))
 *   .orElse(() -> sendReactivationEmail(user));
 *
 * // Validation: run, or fail with a specific exception
 * If.notEmpty(orders)
 *   .then(() -> processOrders(orders))
 *   .orElseThrow(() -> new IllegalStateException("No orders to process"));
 *
 * // Guard clause
 * If.isNull(user)
 *   .thenThrow(() -> new IllegalArgumentException("User cannot be null"));
 *
 * // Only the false branch is interesting
 * If.is(cache.contains(key))
 *   .thenDoNothing()
 *   .orElse(() -> cache.load(key));
 * // ... though If.not(cache.contains(key)).then(() -> cache.load(key)) says it better
 *
 * // Passing a value into the action instead of capturing it
 * If.notNull(user)
 *   .then(user, u -> save(u))
 *   .orElse(() -> createNewUser());
 * }</pre>
 *
 * <p>Conditions are available for booleans ({@link #is(boolean)}, {@link #not(boolean)}), nullity
 * ({@link #isNull(Object)}, {@link #notNull(Object)}), emptiness of every array type plus {@link Collection},
 * {@link Map}, {@link CharSequence}, {@link PrimitiveList}, {@link Multiset} and {@link Multimap}, blankness
 * ({@link #isBlank(CharSequence)}, {@link #notBlank(CharSequence)}), and index validity ({@link #exists(int)}, for
 * results of {@code -1}-returning searches).
 *
 * @see N#ifOrEmpty(boolean, Throwables.Supplier)
 * @see N#ifOrElse(boolean, Throwables.Runnable, Throwables.Runnable)
 * @see N#ifNotNull(Object, Throwables.Consumer)
 * @see N#ifNotEmpty(CharSequence, Throwables.Consumer)
 * @see N#ifNotEmpty(Collection, Throwables.Consumer)
 * @see N#ifNotEmpty(Map, Throwables.Consumer)
 * @see u.Optional
 * @see Supplier
 * @see Throwables.Runnable
 * @see Throwables.Consumer
 * @see Collection
 * @see Map
 * @see CharSequence
 */
@Beta
public final class If {

    private static final If TRUE = new If(true);

    private static final If FALSE = new If(false);

    /** The evaluated condition result. Package-private to allow access from test code. */
    final boolean b;

    private If(final boolean b) {
        this.b = b;
    }

    /**
     * Creates an If instance based on the given boolean condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.is(temperature > 30)
     *   .then(() -> System.out.println("It's hot!"));
     * }</pre>
     *
     * @param b the boolean condition to evaluate
     * @return an If instance representing the condition
     */
    public static If is(final boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Creates an If instance with the negation of the given boolean condition.
     *
     * <p>This is equivalent to {@code is(!b)} but can be more readable in certain contexts.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.not(list.isEmpty())
     *   .then(() -> processList(list));
     * }</pre>
     *
     * @param b the boolean condition to negate
     * @return an If instance representing the negated condition
     */
    public static If not(final boolean b) {
        return b ? FALSE : TRUE;
    }

    /**
     * Creates an If instance that checks if an index is valid (non-negative).
     *
     * <p>Returns {@code true} for {@code index >= 0}, {@code false} for {@code index < 0}.
     * This is commonly used for checking the result of indexOf operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.exists(list.indexOf(element))
     *   .then(() -> System.out.println("Element found"))
     *   .orElse(() -> System.out.println("Element not found"));
     * }</pre>
     *
     * @param index the index value to check
     * @return an If instance that is {@code true} if the index is non-negative
     */
    public static If exists(final int index) {
        return index >= 0 ? TRUE : FALSE;
    }

    /**
     * Creates an If instance that checks if the given object is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isNull(result)
     *   .then(() -> handleNullResult())
     *   .orElse(() -> processResult(result));
     * }</pre>
     *
     * @param obj the object to check for null
     * @return an If instance that is {@code true} if the object is null
     */
    public static If isNull(final Object obj) {
        return is(obj == null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is {@code null} or empty.
     *
     * <p>A CharSequence is considered empty if it is {@code null} or has zero length.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(username)
     *   .then(() -> System.out.println("Username is required"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is {@code null} or empty
     */
    public static If isEmpty(final CharSequence s) {
        return is(Strings.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] flags = {true, false};
     * If.isEmpty(flags).then(() -> System.out.println("Empty"));              // does nothing
     * If.isEmpty((boolean[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the boolean array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final boolean[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c'};
     * If.isEmpty(chars).then(() -> System.out.println("Empty"));           // does nothing
     * If.isEmpty((char[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the char array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final char[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * If.isEmpty(data).then(() -> System.out.println("Empty"));            // does nothing
     * If.isEmpty((byte[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the byte array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final byte[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {10, 20, 30};
     * If.isEmpty(values).then(() -> System.out.println("Empty"));           // does nothing
     * If.isEmpty((short[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the short array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final short[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3};
     * If.isEmpty(numbers).then(() -> System.out.println("Empty"));        // does nothing
     * If.isEmpty((int[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the int array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final int[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] ids = {100L, 200L, 300L};
     * If.isEmpty(ids).then(() -> System.out.println("Empty"));             // does nothing
     * If.isEmpty((long[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the long array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final long[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] prices = {1.5f, 2.5f, 3.5f};
     * If.isEmpty(prices).then(() -> System.out.println("Empty"));           // does nothing
     * If.isEmpty((float[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the float array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final float[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] scores = {98.5, 87.3, 92.1};
     * If.isEmpty(scores).then(() -> System.out.println("Empty"));            // does nothing
     * If.isEmpty((double[]) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the double array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final double[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(args)
     *   .then(() -> System.out.println("No arguments provided"));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final Object[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(resultList)
     *   .then(() -> System.out.println("No results found"))
     *   .orElse(() -> displayResults(resultList));
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is {@code true} if the Collection is {@code null} or empty
     */
    public static If isEmpty(final Collection<?> c) {
        return is(N.isEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(configMap)
     *   .then(() -> loadDefaultConfig());
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is {@code true} if the Map is {@code null} or empty
     */
    public static If isEmpty(final Map<?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is {@code null} or empty.
     *
     * <p>A PrimitiveList is considered empty if it is {@code null} or has size of 0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList numbers = IntList.of(1, 2, 3);
     * If.isEmpty(numbers).then(() -> System.out.println("Empty"));                // does nothing
     * If.isEmpty((PrimitiveList<?, ?, ?>) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * If.isEmpty(IntList.of()).then(() -> System.out.println("Empty"));           // prints "Empty"
     * }</pre>
     *
     * @param list the PrimitiveList to check (can be {@code null})
     * @return an If instance that is {@code true} if the PrimitiveList is {@code null} or empty
     */
    public static If isEmpty(final PrimitiveList<?, ?, ?> list) {
        return is(N.isEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is {@code null} or empty.
     *
     * <p>A Multiset is considered empty if it is {@code null} or has no elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> words = Multiset.of("apple", "banana", "apple");
     * If.isEmpty(words).then(() -> System.out.println("Empty"));                // does nothing
     * If.isEmpty((Multiset<?>) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * If.isEmpty(Multiset.of()).then(() -> System.out.println("Empty"));        // prints "Empty"
     * }</pre>
     *
     * @param s the Multiset to check (can be {@code null})
     * @return an If instance that is {@code true} if the Multiset is {@code null} or empty
     */
    public static If isEmpty(final Multiset<?> s) {
        return is(N.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is {@code null} or empty.
     *
     * <p>A Multimap is considered empty if it is {@code null} or has no key-value mappings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "a", 2, "b", 3);
     * If.isEmpty(map).then(() -> System.out.println("Empty"));                        // does nothing
     * If.isEmpty((Multimap<?, ?, ?>) null).then(() -> System.out.println("Empty"));   // prints "Empty"
     * If.isEmpty(N.newListMultimap()).then(() -> System.out.println("Empty"));        // prints "Empty"
     * }</pre>
     *
     * @param m the Multimap to check (can be {@code null})
     * @return an If instance that is {@code true} if the Multimap is {@code null} or empty
     */
    public static If isEmpty(final Multimap<?, ?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is {@code null}, empty, or contains only whitespace.
     *
     * <p>A CharSequence is considered blank if it is {@code null}, has zero length, or contains only
     * whitespace characters as defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isBlank("   ").then(() -> System.out.println("blank"));   // prints "blank"
     * If.isBlank("").then(() -> System.out.println("blank"));      // prints "blank"
     * If.isBlank("abc").then(() -> System.out.println("blank"));   // does nothing
     * }</pre>
     *
     * @param s the CharSequence to check (can be {@code null})
     * @return an If instance that is {@code true} if the CharSequence is {@code null}, empty, or contains only whitespace
     */
    public static If isBlank(final CharSequence s) {
        return is(Strings.isBlank(s));
    }

    /**
     * Creates an If instance that checks if the given object is not {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notNull(user)
     *   .then(() -> System.out.println("User: " + user.getName()))
     *   .orElse(() -> System.out.println("User not found"));
     * }</pre>
     *
     * @param obj the object to check
     * @return an If instance that is {@code true} if the object is not null
     */
    public static If notNull(final Object obj) {
        return is(obj != null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(searchQuery)
     *   .then(() -> performSearch(searchQuery));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is not {@code null} and has length > 0
     */
    public static If notEmpty(final CharSequence s) {
        return is(Strings.isNotEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] flags = {true, false};
     * If.notEmpty(flags).then(() -> System.out.println("Has data"));              // prints "Has data"
     * If.notEmpty((boolean[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the boolean array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final boolean[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c'};
     * If.notEmpty(chars).then(() -> System.out.println("Has data"));           // prints "Has data"
     * If.notEmpty((char[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the char array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final char[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * If.notEmpty(data).then(() -> System.out.println("Has data"));            // prints "Has data"
     * If.notEmpty((byte[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the byte array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final byte[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {10, 20, 30};
     * If.notEmpty(values).then(() -> System.out.println("Has data"));           // prints "Has data"
     * If.notEmpty((short[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the short array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final short[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3};
     * If.notEmpty(numbers).then(() -> System.out.println("Has data"));        // prints "Has data"
     * If.notEmpty((int[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the int array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final int[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] ids = {100L, 200L, 300L};
     * If.notEmpty(ids).then(() -> System.out.println("Has data"));             // prints "Has data"
     * If.notEmpty((long[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the long array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final long[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] prices = {1.5f, 2.5f, 3.5f};
     * If.notEmpty(prices).then(() -> System.out.println("Has data"));           // prints "Has data"
     * If.notEmpty((float[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the float array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final float[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] scores = {98.5, 87.3, 92.1};
     * If.notEmpty(scores).then(() -> System.out.println("Has data"));            // prints "Has data"
     * If.notEmpty((double[]) null).then(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the double array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final double[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(files)
     *   .then(() -> processFiles(files));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final Object[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(selectedItems)
     *   .then(() -> processSelection(selectedItems))
     *   .orElse(() -> showNoSelectionMessage());
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is {@code true} if the Collection is not {@code null} and not empty
     */
    public static If notEmpty(final Collection<?> c) {
        return is(N.notEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(properties)
     *   .then(() -> applyProperties(properties));
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is {@code true} if the Map is not {@code null} and not empty
     */
    public static If notEmpty(final Map<?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is not {@code null} and not empty.
     *
     * <p>A PrimitiveList is considered not empty if it is not {@code null} and has size greater than 0.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList numbers = IntList.of(1, 2, 3);
     * If.notEmpty(numbers).then(() -> System.out.println("Has data"));                // prints "Has data"
     * If.notEmpty((PrimitiveList<?, ?, ?>) null).then(() -> System.out.println("Has data"));   // does nothing
     * If.notEmpty(IntList.of()).then(() -> System.out.println("Has data"));           // does nothing
     * }</pre>
     *
     * @param list the PrimitiveList to check (can be {@code null})
     * @return an If instance that is {@code true} if the PrimitiveList is not {@code null} and not empty
     */
    public static If notEmpty(final PrimitiveList<?, ?, ?> list) {
        return is(N.notEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is not {@code null} and not empty.
     *
     * <p>A Multiset is considered not empty if it is not {@code null} and contains at least one element.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Multiset<String> words = Multiset.of("apple", "banana", "apple");
     * If.notEmpty(words).then(() -> System.out.println("Has data"));                // prints "Has data"
     * If.notEmpty((Multiset<?>) null).then(() -> System.out.println("Has data"));   // does nothing
     * If.notEmpty(Multiset.of()).then(() -> System.out.println("Has data"));        // does nothing
     * }</pre>
     *
     * @param s the Multiset to check (can be {@code null})
     * @return an If instance that is {@code true} if the Multiset is not {@code null} and not empty
     */
    public static If notEmpty(final Multiset<?> s) {
        return is(N.notEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is not {@code null} and not empty.
     *
     * <p>A Multimap is considered not empty if it is not {@code null} and contains at least one key-value mapping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "a", 2, "b", 3);
     * If.notEmpty(map).then(() -> System.out.println("Has data"));                        // prints "Has data"
     * If.notEmpty((Multimap<?, ?, ?>) null).then(() -> System.out.println("Has data"));   // does nothing
     * If.notEmpty(N.newListMultimap()).then(() -> System.out.println("Has data"));        // does nothing
     * }</pre>
     *
     * @param m the Multimap to check (can be {@code null})
     * @return an If instance that is {@code true} if the Multimap is not {@code null} and not empty
     */
    public static If notEmpty(final Multimap<?, ?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not {@code null}, not empty, and not blank.
     *
     * <p>A CharSequence is considered not blank if it is not {@code null}, has length > 0, and contains
     * at least one non-whitespace character.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notBlank("admin").then(() -> loginUser("admin"));   // executes
     * If.notBlank("   ").then(() -> loginUser("   "));       // does nothing
     * If.notBlank(null).then(() -> loginUser("guest"));      // does nothing
     * }</pre>
     *
     * @param s the CharSequence to check (can be {@code null})
     * @return an If instance that is {@code true} if the CharSequence is not {@code null}, not empty, and contains non-whitespace characters
     */
    public static If notBlank(final CharSequence s) {
        return is(Strings.isNotBlank(s));
    }

    /**
     * Executes no action if the condition is {@code true}, but allows chaining to an {@code orElse} clause.
     *
     * <p>This method is useful when you only want to execute an action in the {@code false} case.
     * It provides a way to explicitly document the intent that no action should be taken when
     * the condition is {@code true}, while still providing a fluent API for handling the
     * {@code false} case.</p>
     *
     * <p><b>Note:</b> In most cases, you can use the negated condition instead for better readability.
     * For example, {@code If.not(condition).then(action)} is often clearer than
     * {@code If.is(condition).thenDoNothing().orElse(action)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Cache check - only load if not cached
     * If.is(cache.contains(key))
     *   .thenDoNothing()
     *   .orElse(() -> cache.load(key));
     *
     * // Example 2: Better alternative using negation
     * If.not(cache.contains(key))
     *   .then(() -> cache.load(key));
     *
     * // Example 3: Explicit no-op with fallback
     * If.notNull(config)
     *   .thenDoNothing()
     *   .orElse(() -> loadDefaultConfig());
     * }</pre>
     *
     * @return an OrElse instance for chaining the else clause
     */
    public OrElse thenDoNothing() {
        return OrElse.of(b);
    }

    /**
     * Executes the given runnable if the condition is {@code true}.
     *
     * <p>This is the primary method for performing conditional actions in the {@code If} fluent API.
     * The provided runnable will be executed immediately if the condition evaluates to {@code true},
     * otherwise it will be skipped. The method returns an {@link OrElse} instance that allows you
     * to chain an alternative action to be executed when the condition is {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Simple conditional execution
     * If.is(debugMode)
     *   .then(() -> logger.debug("Debug information"))
     *   .orElse(() -> logger.info("Normal operation"));
     *
     * // Example 2: Validation and processing
     * If.notEmpty(items)
     *   .then(() -> processItems(items))
     *   .orElseThrow(() -> new IllegalStateException("No items to process"));
     *
     * // Example 3: Conditional side effects
     * If.is(shouldCache)
     *   .then(() -> cache.put(key, value));
     * }</pre>
     *
     * @param <E> the type of exception that the runnable may throw
     * @param cmd the runnable to execute if the condition is {@code true}
     * @return an OrElse instance for optional chaining of an else clause
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws E if the condition is true and the supplied callback throws during execution
     */
    public <E extends Throwable> OrElse then(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(cmd, cs.cmd);

        if (b) {
            cmd.run();
        }

        return OrElse.of(b);
    }

    /**
     * Executes the given consumer with the provided input if the condition is {@code true}.
     *
     * <p>This method is useful for conditional processing of a value, allowing you to pass
     * an initialization parameter that will be consumed if the condition evaluates to {@code true}.
     * This is particularly valuable when you need to perform an action with a specific context
     * or parameter only when certain conditions are met.</p>
     *
     * <p><b>⚠️ Beta Feature:</b> This API is experimental and may change in future versions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Conditional processing with context
     * If.notNull(user)
     *   .then(user, u -> saveUser(u))
     *   .orElse(() -> createNewUser());
     *
     * // Example 2: Conditional initialization with parameter
     * ProcessingContext context = new ProcessingContext();
     * If.notEmpty(dataList)
     *   .then(context, ctx -> {
     *       ctx.initialize();
     *       processDataWithContext(ctx, dataList);
     *   })
     *   .orElse(() -> handleEmptyData());
     * }</pre>
     *
     * @param <T> the type of the input to the consumer
     * @param <E> the type of exception that the consumer may throw
     * @param init the input value to pass to the consumer (can be {@code null})
     * @param action the consumer to execute if the condition is {@code true}
     * @return an OrElse instance for optional chaining of an else clause
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws E if the condition is true and the supplied callback throws during execution
     */
    @Beta
    public <T, E extends Throwable> OrElse then(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        if (b) {
            action.accept(init);
        }

        return OrElse.of(b);
    }

    /**
     * Throws the exception provided by the supplier if the condition is {@code true}.
     *
     * <p>This method is useful for validation scenarios where an exception should be thrown
     * when a certain condition is met. It provides a fluent way to express guard clauses and
     * validation logic, making the code more readable and self-documenting.</p>
     *
     * <p>The exception supplier is only invoked if the condition is {@code true}, allowing
     * for lazy creation of exception instances with dynamic messages or context.</p>
     *
     * <p><b>Note:</b> While this method returns an {@link OrElse} instance to maintain API
     * consistency, it will never be reached if the condition is {@code true} since an exception
     * will be thrown. The {@code orElse()} methods can only be invoked if the condition is
     * {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Validation with custom exception
     * If.isEmpty(requiredField)
     *   .thenThrow(() -> new ValidationException("Required field is empty"));
     *
     * // Example 2: Guard clause pattern
     * If.isNull(user)
     *   .thenThrow(() -> new IllegalArgumentException("User cannot be null"));
     *
     * // Example 3: Business rule validation with dynamic message
     * If.is(amount < 0)
     *   .thenThrow(() -> new IllegalArgumentException("Amount must be positive: " + amount));
     *
     * // Example 4: With orElse fallback (only executes if condition is false)
     * If.isEmpty(data)
     *   .thenThrow(() -> new DataNotFoundException("No data available"))
     *   .orElse(() -> processData(data));
     * }</pre>
     *
     * @param <E> the type of exception to throw
     * @param exceptionSupplier the supplier that provides the exception to throw
     * @return an OrElse instance for optional chaining of an else clause (unreachable if the condition is true)
     * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}.
     * @throws NullPointerException if the condition is true and {@code exceptionSupplier} returns {@code null}
     * @throws E if the condition is true
     */
    public <E extends Throwable> OrElse thenThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, NullPointerException, E {
        N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        if (b) {
            throw exceptionSupplier.get();
        }

        //noinspection ConstantValue
        return OrElse.of(b);
    }

    /**
     * Represents the else clause in a conditional chain, allowing actions to be executed
     * when the initial condition is {@code false}.
     *
     * <p>This class is returned by the {@code then()} methods of the {@code If} class and provides methods
     * to specify what should happen when the initial condition evaluates to {@code false}.</p>
     *
     * <p><b>Key Features:</b>
     * <ul>
     *   <li><b>Lazy Evaluation:</b> Actions are only executed if the initial condition was {@code false}</li>
     *   <li><b>Multiple Options:</b> Supports actions, exception throwing, and no-operation alternatives</li>
     *   <li><b>Stateless Carrier:</b> {@code OrElse} carries only the evaluated condition, not the value or its
     *       type; each {@code orElse} overload introduces its own type parameters</li>
     *   <li><b>Immutable State:</b> The condition state is immutable once created</li>
     * </ul>
     *
     * <p><b>Usage Pattern:</b>
     * <pre>{@code
     * If.is(condition)
     *   .then(() -> actionIfTrue())
     *   .orElse(() -> actionIfFalse());
     * }</pre>
     *
     */
    public static final class OrElse {
        /**
         * Cached {@code OrElse} instance representing a {@code true} condition (the {@code then} branch was taken),
         * on which every {@code orElse} method is a no-op.
         *
         * <p>There is no reason to reference this constant directly: an {@code OrElse} is obtained by calling
         * {@link If#then(Throwables.Runnable)}, {@link If#thenDoNothing()} or a sibling, which already return the
         * cached instance matching the evaluated condition.</p>
         */
        public static final OrElse TRUE = new OrElse(true);

        /**
         * Cached {@code OrElse} instance representing a {@code false} condition (the {@code then} branch was not
         * taken), on which every {@code orElse} method runs.
         *
         * <p>There is no reason to reference this constant directly; see {@link #TRUE}.</p>
         */
        public static final OrElse FALSE = new OrElse(false);

        /**
         * The boolean state indicating whether the initial If condition was {@code true}.
         * Used to determine whether to execute the then clause or the orElse clause.
         */
        private final boolean isIfTrue;

        /**
         * Constructs a new OrElse instance with the given boolean state.
         *
         * <p>This constructor is package-private and used internally by the If class
         * to create OrElse instances representing the state of the conditional chain.</p>
         *
         * @param b the boolean state indicating whether the initial If condition was true
         */
        OrElse(final boolean b) {
            isIfTrue = b;
        }

        /**
         * Factory method to create an OrElse instance based on the given boolean value.
         *
         * <p>This method uses cached instances (TRUE or FALSE) for performance optimization,
         * avoiding object creation for repeated conditional evaluations.</p>
         *
         * @param b the boolean state indicating whether the initial If condition was true
         * @return an OrElse instance representing the given state (cached instance)
         */
        static OrElse of(final boolean b) {
            return b ? TRUE : FALSE;
        }

        /**
         * Completes the conditional chain without performing any action when the initial condition was
         * {@code false}.
         *
         * <p>This is a no-op in every case; it exists so that a chain can state explicitly that the else
         * branch is intentionally empty. Simply not calling any {@code orElse} method has exactly the same
         * effect.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * If.is(cache.contains(key))
         *   .then(() -> stats.recordHit())
         *   .orElseDoNothing();   // explicit: a miss is not interesting here
         * }</pre>
         */
        public void orElseDoNothing() {
            // Do nothing.
        }

        /**
         * Executes the given runnable if the initial condition was {@code false}.
         *
         * <p>This method completes the conditional chain by providing an alternative action
         * to be executed when the initial {@code If} condition evaluates to {@code false}.
         * It represents the "else" branch in the traditional if-else statement.</p>
         *
         * <p>The provided runnable will only be executed if the initial condition was {@code false}.
         * If the condition was {@code true}, this method does nothing.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Example 1: Simple if-else pattern
         * If.is(hasPermission)
         *   .then(() -> performAction())
         *   .orElse(() -> showAccessDeniedMessage());
         *
         * // Example 2: Fallback logic
         * If.notEmpty(cache)
         *   .then(() -> loadFromCache())
         *   .orElse(() -> loadFromDatabase());
         *
         * // Example 3: Store either a preference or a default
         * String userPreference = loadUserPreference();
         * Holder<String> setting = Holder.of(null);
         * If.notNull(userPreference)
         *   .then(() -> setting.setValue(userPreference))
         *   .orElse(() -> setting.setValue("default"));
         * }</pre>
         *
         * @param <E> the type of exception that the runnable may throw
         * @param cmd the runnable to execute if the initial condition was {@code false}
         * @throws IllegalArgumentException if {@code cmd} is {@code null}.
         * @throws E if the initial condition is false and the supplied callback throws during execution
         */
        public <E extends Throwable> void orElse(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
            N.checkArgNotNull(cmd, cs.cmd);

            if (!isIfTrue) {
                cmd.run();
            }
        }

        /**
         * Executes the given consumer with the provided input if the initial condition was {@code false}.
         *
         * <p>This method allows you to pass an initialization parameter that will be consumed
         * if the initial condition evaluates to {@code false}. This is particularly useful when
         * you need to provide a fallback action with specific context or parameters.</p>
         *
         * <p><b>⚠️ Beta Feature:</b> This API is experimental and may change in future versions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Example 1: Fallback with parameter
         * String key = "user:42";
         * String cachedValue = findInCache(key);
         * Holder<String> value = Holder.of(null);
         * If.notNull(cachedValue)
         *   .then(() -> value.setValue(cachedValue))
         *   .orElse(key, k -> value.setValue(loadFromDatabase(k)));
         *
         * // Example 2: Context-based fallback
         * ProcessingContext context = new ProcessingContext();
         * If.notEmpty(dataList)
         *   .then(context, ctx -> processData(ctx, dataList))
         *   .orElse(context, ctx -> setDefaultProcessingMode(ctx));
         * }</pre>
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that the consumer may throw
         * @param init the input value to pass to the consumer (can be {@code null})
         * @param action the consumer to execute if the initial condition was {@code false}
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         * @throws E if the initial condition is false and the supplied callback throws during execution
         */
        @Beta
        public <T, E extends Throwable> void orElse(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (!isIfTrue) {
                action.accept(init);
            }
        }

        /**
         * Throws the exception provided by the supplier if the initial condition was {@code false}.
         *
         * <p>This method is useful for validation scenarios where an exception should be thrown
         * when a required condition is not met. It provides a fluent way to express validation
         * requirements and ensures that execution cannot continue when expected conditions fail.</p>
         *
         * <p>The exception supplier is only invoked if the initial condition was {@code false},
         * allowing for lazy creation of exception instances with dynamic messages or context.
         * If the condition was {@code true}, this method does nothing and execution continues normally.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Example 1: Validation after processing
         * If.notEmpty(results)
         *   .then(() -> processResults(results))
         *   .orElseThrow(() -> new NoResultsException("No results found"));
         *
         * // Example 2: Required condition check
         * If.notNull(user)
         *   .then(() -> authenticateUser(user))
         *   .orElseThrow(() -> new IllegalStateException("User must not be null"));
         *
         * // Example 3: Business rule enforcement
         * If.is(balance >= amount)
         *   .then(() -> processWithdrawal(amount))
         *   .orElseThrow(() -> new InsufficientFundsException("Balance: " + balance + ", Required: " + amount));
         *
         * // Example 4: Data validation
         * If.notBlank(email)
         *   .then(() -> sendEmail(email))
         *   .orElseThrow(() -> new ValidationException("Email address is required"));
         * }</pre>
         *
         * @param <E> the type of exception to throw
         * @param exceptionSupplier the supplier that provides the exception to throw
         * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}.
         * @throws NullPointerException if the initial condition is false and {@code exceptionSupplier} returns {@code null}
         * @throws E if the initial condition was false
         */
        public <E extends Throwable> void orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, NullPointerException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (!isIfTrue) {
                throw exceptionSupplier.get();
            }
        }
    }
}
