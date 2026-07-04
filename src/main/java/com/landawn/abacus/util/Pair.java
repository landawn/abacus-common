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

import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

/**
 * A mutable container class that holds two related objects as a pair.
 * This class implements {@link Map.Entry} to provide compatibility with Java's Map interface
 * and {@link Mutable} to indicate that its state can be modified after creation.
 *
 * <p>The pair consists of two elements referred to as "left" and "right". Both elements
 * can be of any type, including {@code null}. This class is particularly useful when you need
 * to return two values from a method or store two related objects together.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Pair<String, Integer> nameAge = Pair.of("John", 25);
 * String name = nameAge.left();
 * Integer age = nameAge.right();
 *
 * // Modify the pair
 * nameAge.setLeft("Jane");
 * nameAge.setRight(30);
 * }</pre>
 *
 * <h2>{@code Pair} vs {@link Tuple2}</h2>
 *
 * <p>{@code Pair} and {@code Tuple2} both hold two heterogeneous values, but they target
 * different use cases. Choose based on whether you need mutability and {@link Map.Entry}
 * interop, or immutability and a functional/tuple-style API.</p>
 *
 * <table border="1">
 *   <caption>Pair vs Tuple2</caption>
 *   <tr>
 *     <th></th>
 *     <th>{@code Pair<L, R>}</th>
 *     <th>{@code Tuple2<T1, T2>}</th>
 *   </tr>
 *   <tr>
 *     <td>Mutability</td>
 *     <td><b>Mutable</b> — implements {@link Mutable}; values can be reassigned via
 *         {@link #setLeft(Object)} / {@link #setRight(Object)}</td>
 *     <td><b>Effectively immutable</b> — {@code _1} and {@code _2} are {@code public final}</td>
 *   </tr>
 *   <tr>
 *     <td>Field access</td>
 *     <td>Named getters/setters: {@link #left()}, {@link #right()},
 *         {@link #getKey()}, {@link #getValue()}</td>
 *     <td>Direct field access: {@code t._1}, {@code t._2}</td>
 *   </tr>
 *   <tr>
 *     <td>Map.Entry interop</td>
 *     <td><b>Yes</b> — implements {@link Map.Entry}, so it can be used directly with
 *         {@code Map} APIs and {@link #setValue(Object)} mutates the right element</td>
 *     <td>No — convert via {@link Tuple2#toImmutableEntry()} or {@link Tuple2#toPair()}</td>
 *   </tr>
 *   <tr>
 *     <td>Functional/tuple API</td>
 *     <td>Minimal: {@code map}, {@code filter}, {@code accept}, {@code forEach} (overloads accept
 *         either the two elements or the pair as a whole)</td>
 *     <td>Rich: {@code accept(BiConsumer)}, {@code map(BiFunction)}, {@code filter(BiPredicate)},
 *         {@code reverse()}, {@code toArray()}, plus integration with the
 *         {@link Tuple} hierarchy</td>
 *   </tr>
 *   <tr>
 *     <td>Hash/equals stability</td>
 *     <td>Hash code changes when elements are mutated — <b>do not use as a {@code HashMap} key
 *         or {@code HashSet} element while mutating</b></td>
 *     <td>Stable — safe to use as a {@code Map} key or {@code Set} element</td>
 *   </tr>
 *   <tr>
 *     <td>Use when</td>
 *     <td>You need to mutate the contents after construction, or you need a {@link Map.Entry}
 *         (e.g. building entries, iterating maps, two-value accumulator in a loop)</td>
 *     <td>You want immutable, value-style semantics, a richer functional API, or a tuple that
 *         composes with {@code Tuple3}, {@code Tuple4}, etc.</td>
 *   </tr>
 * </table>
 *
 * <p>Conversion: use {@link #toTuple()} to obtain an immutable snapshot as a {@code Tuple2},
 * and {@link Tuple2#toPair()} to obtain a fresh mutable {@code Pair}.</p>
 *
 * @param <L> the type of the left element.
 * @param <R> the type of the right element.
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.Holder
 * @see com.landawn.abacus.util.Result
 * @see com.landawn.abacus.util.Triple
 * @see com.landawn.abacus.util.Tuple
 * @see com.landawn.abacus.util.Tuple.Tuple2
 */
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
public final class Pair<L, R> implements Map.Entry<L, R>, Mutable {
    // implements Map.Entry<L, R> {
    private L left; //NOSONAR

    private R right; //NOSONAR

    /**
     * Constructs an empty Pair with both left and right elements initialized to {@code null}.
     * This constructor is useful when you need to create a pair and set its values later.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = new Pair<>();
     * pair.setLeft("Hello");
     * pair.setRight(42);
     * }</pre>
     *
     */
    public Pair() {
    }

    Pair(final L leftValue, final R rightValue) {
        setLeft(leftValue);
        setRight(rightValue);
    }

    /**
     * Creates a new Pair instance containing the specified left and right elements.
     *
     * <p>This is a static factory method that provides a convenient and type-safe way to create
     * a Pair instance. The method allows for type inference, making the code more concise.
     * Both elements can be {@code null}. The returned pair is mutable, allowing
     * modification of its elements after creation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> coordinates = Pair.of("X", 10);
     * Pair<String, String> nullPair = Pair.of(null, null);   // valid: both elements may be null
     * }</pre>
     *
     * @param <L> the type of the left element.
     * @param <R> the type of the right element.
     * @param leftValue the left element of the pair, may be {@code null}.
     * @param rightValue the right element of the pair, may be {@code null}.
     * @return a new Pair instance containing the specified left and right elements.
     */
    public static <L, R> Pair<L, R> of(final L leftValue, final R rightValue) {
        return new Pair<>(leftValue, rightValue);
    }

    /**
     * Creates a new Pair instance from an existing Map.Entry.
     *
     * <p>This is a static factory method that converts a Map.Entry to a Pair instance.
     * The key of the entry becomes the left element of the pair,
     * and the value becomes the right element. This method is useful when working with Map entries
     * and you need to convert them to Pair objects for further processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("age", 25);
     * Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
     * Pair<String, Integer> pair = Pair.from(entry);
     * // pair.left() returns "age", pair.right() returns 25
     * }</pre>
     *
     * @param <K> the key type of the {@code Map.Entry}, which becomes the left element type.
     * @param <V> the value type of the {@code Map.Entry}, which becomes the right element type.
     * @param entry the {@code Map.Entry} to convert to a Pair, must not be {@code null}.
     * @return a new Pair instance with the key as the left element and the value as the right element.
     * @throws NullPointerException if {@code entry} is {@code null}.
     */
    public static <K, V> Pair<K, V> from(final Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    private static final Pair<?, ?>[] EMPTY_ARRAY = new Pair[0];

    /**
     * Returns a type-safe empty array of Pair instances.
     *
     * <p>This method returns a shared, reusable empty array, avoiding the need to allocate
     * a new empty array each time. The array has length zero. Note that the array reference
     * itself is shared; the caller should not store elements into it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer>[] emptyPairs = Pair.emptyArray();
     * // emptyPairs.length == 0
     *
     * // Useful in methods that return arrays:
     * public Pair<String, Integer>[] getPairs() {
     *     if (someCondition) {
     *         return Pair.emptyArray();
     *     }
     *     // ... other logic
     * }
     * }</pre>
     *
     * @param <L> the type of the left elements in the array.
     * @param <R> the type of the right elements in the array.
     * @return a shared empty array of Pair instances with length zero.
     */
    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R>[] emptyArray() {
        return (Pair<L, R>[]) EMPTY_ARRAY;
    }

    /**
     * Returns the left element of this pair.
     *
     * <p>This is the preferred method for accessing the left element,
     * providing a more concise alternative to {@link #getLeft()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * String left = pair.left();   // returns "Hello"
     * }</pre>
     *
     * @return the left element of this pair, may be {@code null}.
     */
    public L left() {
        return this.left;
    }

    /**
     * Returns the right element of this pair.
     *
     * <p>This is the preferred method for accessing the right element,
     * providing a more concise alternative to {@link #getRight()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Integer right = pair.right();   // returns 42
     * }</pre>
     *
     * @return the right element of this pair, may be {@code null}.
     */
    public R right() {
        return this.right;
    }

    /**
     * Gets the left element of this pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * String left = pair.getLeft();   // returns "Hello"
     * }</pre>
     *
     * @return the left element of the pair, may be {@code null}.
     * @deprecated This method is deprecated in favor of the more concise {@link #left()} method.
     * @see #left()
     */
    @Deprecated
    public L getLeft() {
        return left;
    }

    /**
     * Sets the left element of this pair to the specified value.
     *
     * <p>This method modifies the pair in place. The value may be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * pair.setLeft("New");
     * // pair.left() now returns "New"
     * }</pre>
     *
     * @param left the new value for the left element, may be {@code null}.
     */
    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * Gets the right element of this pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Integer right = pair.getRight();   // returns 42
     * }</pre>
     *
     * @return the right element of the pair, may be {@code null}.
     * @deprecated This method is deprecated in favor of the more concise {@link #right()} method.
     * @see #right()
     */
    @Deprecated
    public R getRight() {
        return right;
    }

    /**
     * Sets the right element of this pair to the specified value.
     *
     * <p>This method modifies the pair in place. The value may be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * pair.setRight(20);
     * // pair.right() now returns 20
     * }</pre>
     *
     * @param right the new value for the right element, may be {@code null}.
     */
    public void setRight(final R right) {
        this.right = right;
    }

    /**
     * Sets both the left and right elements of this pair in a single operation.
     *
     * <p>This is a convenience method equivalent to calling {@link #setLeft(Object)}
     * followed by {@link #setRight(Object)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * pair.set("New", 20);
     * // pair.left() returns "New", pair.right() returns 20
     * }</pre>
     *
     * @param left the new value for the left element, may be {@code null}.
     * @param right the new value for the right element, may be {@code null}.
     */
    public void set(final L left, final R right) {
        this.setLeft(left);
        this.setRight(right);
    }

    /**
     * Sets the left element to the given value and returns the previous value.
     *
     * <p>This method performs a swap operation: it sets the new value and returns what was
     * previously held. This is particularly useful when you need to track state changes.</p>
     *
     * <p><b>Note:</b> This operation is not atomic; this class is not thread-safe.
     * External synchronization is required for concurrent use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * String oldLeft = pair.getAndSetLeft("New");
     * // oldLeft contains "Old", pair.left() returns "New"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element, may be {@code null}.
     * @return the previous value of the left element, may be {@code null}.
     */
    public L getAndSetLeft(final L newLeft) {
        final L res = left;
        setLeft(newLeft);
        return res;
    }

    /**
     * Sets the left element to the given value and returns the new value.
     *
     * <p>This method sets a new value and returns that same value, allowing for method chaining
     * or immediate usage of the newly set value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * String newLeft = pair.setAndGetLeft("New");
     * // newLeft contains "New", pair.left() also returns "New"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element, may be {@code null}.
     * @return the new value of the left element (same as the parameter), may be {@code null}.
     */
    public L setAndGetLeft(final L newLeft) {
        setLeft(newLeft);
        return left;
    }

    /**
     * Sets the right element to the given value and returns the previous value.
     *
     * <p>This method performs a swap operation: it sets the new value and returns what was
     * previously held. This is particularly useful when you need to track state changes.</p>
     *
     * <p><b>Note:</b> This operation is not atomic; this class is not thread-safe.
     * External synchronization is required for concurrent use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * Integer oldRight = pair.getAndSetRight(20);
     * // oldRight contains 10, pair.right() returns 20
     * }</pre>
     *
     * @param newRight the new value to set for the right element, may be {@code null}.
     * @return the previous value of the right element, may be {@code null}.
     */
    public R getAndSetRight(final R newRight) {
        final R res = right;
        setRight(newRight);
        return res;
    }

    /**
     * Sets the right element to the given value and returns the new value.
     *
     * <p>This method sets a new value and returns that same value, allowing for method chaining
     * or immediate usage of the newly set value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * Integer newRight = pair.setAndGetRight(20);
     * // newRight contains 20, pair.right() also returns 20
     * }</pre>
     *
     * @param newRight the new value to set for the right element, may be {@code null}.
     * @return the new value of the right element (same as the parameter), may be {@code null}.
     */
    public R setAndGetRight(final R newRight) {
        setRight(newRight);
        return right;
    }

    /**
     * Conditionally sets the left element to the specified value.
     *
     * <p>The given predicate is evaluated against the <em>current</em> left and right
     * values of this pair. If the predicate returns {@code true}, the left element
     * is updated to {@code newLeft}; otherwise this pair remains unchanged.</p>
     *
     * <p>The predicate is evaluated at most once. If it throws an exception,
     * the left element is not modified and the exception is propagated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     *
     * boolean updated = pair.setLeftIf((l, r) -> r > 5, "World");
     * // updated == true, pair.left() == "World" because 10 > 5
     *
     * updated = pair.setLeftIf((l, r) -> r > 20, "Test");
     * // updated == false, pair.left() is still "World" because 10 is not > 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to evaluate against the current left and right values;
     *                  must not be {@code null}
     * @param newLeft the new value to assign to the left element if the predicate passes;
     *                may be {@code null}
     * @return {@code true} if the left element was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public <E extends Exception> boolean setLeftIf(final Throwables.BiPredicate<? super L, ? super R, E> predicate, final L newLeft) throws E {
        if (predicate.test(left, right)) {
            setLeft(newLeft);
            return true;
        }

        return false;
    }

    /**
     * Conditionally sets the right element to the specified value.
     *
     * <p>The given predicate is evaluated against the <em>current</em> left and right
     * values of this pair. If the predicate returns {@code true}, the right element
     * is updated to {@code newRight}; otherwise this pair remains unchanged.</p>
     *
     * <p>The predicate is evaluated at most once. If it throws an exception,
     * the right element is not modified and the exception is propagated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     *
     * boolean updated = pair.setRightIf((l, r) -> l.length() > 3, 20);
     * // updated == true, pair.right() == 20 because "Hello".length() > 3
     *
     * updated = pair.setRightIf((l, r) -> r < 10, 30);
     * // updated == false, pair.right() is still 20 because 20 is not < 10
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to evaluate against the current left and right values;
     *                  must not be {@code null}
     * @param newRight the new value to assign to the right element if the predicate passes;
     *                 may be {@code null}
     * @return {@code true} if the right element was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public <E extends Exception> boolean setRightIf(final Throwables.BiPredicate<? super L, ? super R, E> predicate, final R newRight) throws E {
        if (predicate.test(left, right)) {
            setRight(newRight);
            return true;
        }

        return false;
    }

    /**
     * Conditionally sets both the left and right elements to the specified values.
     *
     * <p>The given predicate is evaluated against the <em>current</em> left and right
     * values of this pair. If the predicate returns {@code true}, both elements are
     * updated to {@code newLeft} and {@code newRight} respectively. If the predicate
     * returns {@code false}, this pair remains unchanged.</p>
     *
     * <p>From a single thread's perspective, the update of left and right is all-or-nothing:
     * either both values are changed, or neither is. The predicate is evaluated at
     * most once. If it throws an exception, neither element is modified and the
     * exception is propagated. Note: this class is not thread-safe; the two field
     * assignments are not atomic with respect to other threads.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     *
     * boolean updated = pair.setIf((l, r) -> l.length() + r < 20,
     *         "World", 20);
     * // updated == true, pair is now ("World", 20) because 5 + 10 < 20
     *
     * updated = pair.setIf((l, r) -> l.length() > r,
     *         "Test", 5);
     * // updated == false, pair remains ("World", 20) because 5 is not > 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to evaluate against the current left and right values;
     *                  must not be {@code null}
     * @param newLeft  the new value to assign to the left element if the predicate passes;
     *                 may be {@code null}
     * @param newRight the new value to assign to the right element if the predicate passes;
     *                 may be {@code null}
     * @return {@code true} if both elements were updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public <E extends Exception> boolean setIf(final Throwables.BiPredicate<? super L, ? super R, E> predicate, final L newLeft, final R newRight) throws E {
        if (predicate.test(left, right)) {
            setLeft(newLeft);
            setRight(newRight);
            return true;
        }

        return false;
    }

    /**
     * Creates and returns a new Pair with the left and right elements swapped.
     * The original pair remains unchanged.
     *
     * <p>Note: this instance method is non-mutating and returns a <i>new</i> swapped pair, in contrast to the
     * static {@link N#swap(Pair)} / {@link N#swapIf(Pair, java.util.function.Predicate)} helpers, which
     * <i>mutate</i> the given pair in place (those require both element types to be the same, {@code Pair<T, T>}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> original = Pair.of("Hello", 42);
     * Pair<Integer, String> swapped = original.swap();
     * // swapped.left() returns 42, swapped.right() returns "Hello"
     * // original remains unchanged
     * }</pre>
     *
     * @return a new Pair with the right element as the left and the left element as the right.
     * @see N#swap(Pair)
     * @see N#swapIf(Pair, java.util.function.Predicate)
     */
    @Beta
    public Pair<R, L> swap() {
        return new Pair<>(right, left);
    }

    /**
     * Creates and returns a shallow copy of this pair.
     * The new pair contains the same left and right references as this pair.
     * Note that this is a shallow copy - if the elements are mutable objects,
     * changes to those objects will be reflected in both pairs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, List<Integer>> original = Pair.of("Numbers", new ArrayList<>());
     * original.right().add(1);
     *
     * Pair<String, List<Integer>> copy = original.copy();
     * copy.right().add(2);
     * // Both original and copy will have [1, 2] in their right element
     *
     * copy.setLeft("NewNumbers");
     * // original.left() still returns "Numbers", copy.left() returns "NewNumbers"
     * }</pre>
     *
     * @return a new Pair containing the same left and right elements as this pair.
     */
    public Pair<L, R> copy() {
        return new Pair<>(left, right);
    }

    /**
     * Converts this pair to an Object array containing two elements.
     * The first element of the array is the left element of the pair,
     * and the second element is the right element. Either or both can be {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Object[] array = pair.toArray();
     * // array[0] equals "Hello", array[1] equals 42
     * // array.length equals 2
     * }</pre>
     *
     * @return a new Object array of length 2 containing [left, right].
     */
    public Object[] toArray() {
        return new Object[] { left, right };
    }

    /**
     * Converts this pair to an array of the specified type, storing the left element
     * at index 0 and the right element at index 1. If the provided array has a length
     * of at least 2, it is used directly; otherwise, a new array of the same type
     * with length 2 is created.
     *
     * <p><b>Note:</b> Unlike {@link java.util.Collection#toArray(Object[])}, this method does
     * <em>not</em> null-terminate the returned array when the supplied array has length &gt; 2;
     * any elements at indices &gt;= 2 are left untouched.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, String> pair = Pair.of("Hello", "World");
     *
     * // Using an array with sufficient size
     * String[] existing = new String[3];
     * existing[2] = "untouched";
     * String[] result = pair.toArray(existing);
     * // result == existing, result[0] = "Hello", result[1] = "World", result[2] = "untouched" (unchanged)
     *
     * // Using an array with insufficient size
     * String[] small = new String[1];
     * String[] result2 = pair.toArray(small);
     * // result2 != small, result2.length = 2, result2[0] = "Hello", result2[1] = "World"
     * }</pre>
     *
     * @param <A> the component type of the array.
     * @param a the array into which the elements are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing the left element at index 0 and the right element at index 1.
     * @throws NullPointerException if {@code a} is {@code null}
     * @throws ArrayStoreException if the runtime type of the specified array is not a
     *         supertype of the runtime type of the elements in this pair
     */
    public <A> A[] toArray(A[] a) {
        if (a.length < 2) {
            a = N.copyOf(a, 2);
        }

        a[0] = (A) left;
        a[1] = (A) right;

        return a;
    }

    /**
     * Performs the given action on each element of this pair.
     * The consumer is called first with the left element, then with the right element.
     * The consumer must be able to handle the types of both elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, String> pair = Pair.of("Hello", "World");
     * List<String> collected = new ArrayList<>();
     * pair.forEach(collected::add);
     * // collected now contains ["Hello", "World"]
     *
     * // With different types (requires consumer that accepts Object)
     * Pair<String, Integer> mixed = Pair.of("Count", 42);
     * mixed.forEach(System.out::println);
     * // Prints "Count" then "42"
     * }</pre>
     *
     * @param <E> the type of exception that the consumer may throw.
     * @param consumer the action to be performed on each element; must accept a common
     *                 supertype of both L and R (typically {@code Object}); must not be {@code null}.
     * @throws IllegalArgumentException if {@code consumer} is {@code null}.
     * @throws E if the consumer throws an exception.
     * @throws ClassCastException if the consumer cannot accept the runtime types of the elements.
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(consumer);

        final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

        objConsumer.accept(left);
        objConsumer.accept(right);
    }

    /**
     * Performs the given action with both elements of this pair as arguments.
     * This method is useful when you need to process both elements together
     * in a single operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Age", 25);
     * Map<String, Integer> map = new HashMap<>();
     * pair.accept(map::put);
     * // map now contains {"Age": 25}
     *
     * pair.accept((name, value) -> {
     *     System.out.println(name + " = " + value);
     * });
     * // Prints "Age = 25"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw.
     * @param action the action to be performed with the left and right elements as arguments;
     *               must not be {@code null}.
     * @throws E if the action throws an exception.
     */
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super L, ? super R, E> action) throws E {
        action.accept(left, right);
    }

    /**
     * Performs the given action with this pair as the argument.
     * This method is useful when you want to process the pair as a whole
     * rather than its individual elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Score", 100);
     * List<Pair<String, Integer>> pairs = new ArrayList<>();
     * pair.accept(pairs::add);
     * // pairs now contains the pair
     *
     * pair.accept(p -> {
     *     if (p.right() > 50) {
     *         System.out.println(p.left() + " is high");
     *     }
     * });
     * // Prints "Score is high"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw.
     * @param action the action to be performed with this pair as the argument;
     *               must not be {@code null}.
     * @throws E if the action throws an exception.
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Pair<L, R>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Applies the given function to both elements of this pair and returns the result.
     * The function receives the left element as the first argument and the right element
     * as the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<Integer, Integer> dimensions = Pair.of(10, 20);
     * Integer area = dimensions.map((width, height) -> width * height);
     * // area equals 200
     *
     * Pair<String, String> names = Pair.of("John", "Doe");
     * String fullName = names.map((first, last) -> first + " " + last);
     * // fullName equals "John Doe"
     * }</pre>
     *
     * @param <U> the type of the result.
     * @param <E> the type of exception that the mapper function may throw.
     * @param mapper the function to apply to the left and right elements; must not be {@code null}.
     * @return the result of applying the mapper function to both elements.
     * @throws E if the mapper function throws an exception.
     */
    public <U, E extends Exception> U map(final Throwables.BiFunction<? super L, ? super R, ? extends U, E> mapper) throws E {
        return mapper.apply(left, right);
    }

    /**
     * Applies the given function to this pair and returns the result.
     * The function receives the entire pair as its argument, allowing it to
     * access both elements and any methods of the pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> entry = Pair.of("Temperature", 25);
     * String formatted = entry.map(p -> p.left() + ": " + p.right() + "°C");
     * // formatted equals "Temperature: 25°C"
     *
     * Boolean isValid = entry.map(p -> p.left() != null && p.right() != null);
     * // isValid equals true
     * }</pre>
     *
     * @param <U> the type of the result.
     * @param <E> the type of exception that the mapper function may throw.
     * @param mapper the function to apply to this pair; must not be {@code null}.
     * @return the result of applying the mapper function to this pair.
     * @throws E if the mapper function throws an exception.
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super Pair<L, R>, ? extends U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     * Returns an Optional containing this pair if the given predicate evaluates to true
     * for the left and right elements, otherwise returns an empty Optional.
     *
     * <p>This method is useful for conditional processing in functional pipelines.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 5);
     *
     * Optional<Pair<String, Integer>> filtered = pair.filter(
     *     (left, right) -> left.length() == right
     * );
     * // filtered.isPresent() is true because "Hello".length() == 5
     *
     * Optional<Pair<String, Integer>> filtered2 = pair.filter(
     *     (left, right) -> right > 10
     * );
     * // filtered2.isPresent() is false because 5 is not > 10
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw.
     * @param predicate the condition to test with the left and right elements; must not be {@code null}.
     * @return an Optional containing this pair if the predicate returns {@code true},
     *         otherwise an empty Optional.
     * @throws E if the predicate throws an exception.
     */
    public <E extends Exception> Optional<Pair<L, R>> filter(final Throwables.BiPredicate<? super L, ? super R, E> predicate) throws E {
        return predicate.test(left, right) ? Optional.of(this) : Optional.empty();
    }

    /**
     * Returns an Optional containing this pair if the given predicate evaluates to true
     * for this pair, otherwise returns an empty Optional.
     *
     * <p>This method is useful for conditional processing in functional pipelines
     * where the predicate needs access to the pair object itself.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Test", 10);
     *
     * Optional<Pair<String, Integer>> filtered = pair.filter(
     *     p -> p.left().startsWith("T") && p.right() % 2 == 0
     * );
     * // filtered.isPresent() is true
     *
     * Optional<Pair<String, Integer>> filtered2 = pair.filter(
     *     p -> p.left().length() + p.right() > 20
     * );
     * // filtered2.isPresent() is false because 4 + 10 = 14, which is not > 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw.
     * @param predicate the condition to test with this pair; must not be {@code null}.
     * @return an Optional containing this pair if the predicate returns {@code true},
     *         otherwise an empty Optional.
     * @throws E if the predicate throws an exception.
     */
    public <E extends Exception> Optional<Pair<L, R>> filter(final Throwables.Predicate<? super Pair<L, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    /**
     * Converts this pair to a Tuple2 (2-element tuple).
     * Tuples are similar to pairs but may provide different functionality
     * or be required by certain APIs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Tuple2<String, Integer> tuple = pair.toTuple();
     * // tuple._1 equals "Hello", tuple._2 equals 42
     * }</pre>
     *
     * @return a new Tuple2 containing the same elements as this pair.
     */
    public Tuple2<L, R> toTuple() {
        return Tuple.of(left, right);
    }

    /**
     * Converts this pair to an immutable Map.Entry.
     * The resulting entry cannot be modified - attempts to call setValue()
     * on the returned entry will throw an UnsupportedOperationException.
     *
     * <p>This is useful when you need to pass the pair to APIs that expect
     * Map.Entry objects but want to ensure the values cannot be modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Age", 30);
     * Map.Entry<String, Integer> entry = pair.toImmutableEntry();
     * // entry.getKey() returns "Age", entry.getValue() returns 30
     * // entry.setValue(31) would throw UnsupportedOperationException
     * }</pre>
     *
     * @return a new immutable Map.Entry with the same key-value mapping as this pair.
     */
    public ImmutableEntry<L, R> toImmutableEntry() {
        return ImmutableEntry.of(left, right);
    }

    /**
     * Returns the left element of this pair, implementing the Map.Entry interface.
     * This method allows Pair to be used wherever a Map.Entry is expected.
     *
     * <p>This method is deprecated in favor of {@link #left()} which provides
     * a more concise API. When using Pair as a Map.Entry, the left element
     * serves as the key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * String key = entry.getKey();   // returns "key"
     * }</pre>
     *
     * @return the left element of this pair, may be {@code null}.
     * @deprecated Use {@link #left()} instead for cleaner code.
     */
    @Deprecated
    @Override
    public L getKey() {
        return left;
    }

    /**
     * Returns the right element of this pair, implementing the Map.Entry interface.
     * This method allows Pair to be used wherever a Map.Entry is expected.
     *
     * <p>This method is deprecated in favor of {@link #right()} which provides
     * a more concise API. When using Pair as a Map.Entry, the right element
     * serves as the value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * Integer value = entry.getValue();   // returns 100
     * }</pre>
     *
     * @return the right element of this pair, may be {@code null}.
     * @deprecated Use {@link #right()} instead for cleaner code.
     */
    @Deprecated
    @Override
    public R getValue() {
        return right;
    }

    /**
     * Sets the right element of this pair to the specified value and returns
     * the previous value, implementing the Map.Entry interface.
     * This method allows Pair to be used wherever a mutable Map.Entry is expected.
     *
     * <p>This method is deprecated in favor of {@link #setRight(Object)} or
     * {@link #getAndSetRight(Object)} which provide a clearer API. When using
     * Pair as a Map.Entry, this method modifies the value (right element) and
     * returns the old value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * Integer oldValue = entry.setValue(200);
     * // oldValue equals 100, pair.right() now returns 200
     * }</pre>
     *
     * @param value the new value to set for the right element, may be {@code null}.
     * @return the previous value of the right element, may be {@code null}.
     * @deprecated Use {@link #setRight(Object)} or {@link #getAndSetRight(Object)} instead.
     */
    @Deprecated
    @Override
    public R setValue(final R value) {
        return getAndSetRight(value);
    }

    /**
     * Returns a hash code value for this pair.
     * The hash code is computed as the XOR of the hash codes of the left and right elements.
     *
     * <p>Equal pairs (as determined by {@link #equals(Object)}) are guaranteed to have the
     * same hash code, making them suitable for use as keys in hash-based collections.
     * Note that because XOR is commutative, {@code Pair.of(a, b)} and {@code Pair.of(b, a)} always have the
     * same hash code, regardless of the element values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair1 = Pair.of("Hello", 42);
     * Pair<String, Integer> pair2 = Pair.of("Hello", 42);
     * // pair1.hashCode() == pair2.hashCode()
     *
     * Map<Pair<String, Integer>, String> map = new HashMap<>();
     * map.put(pair1, "value");
     * // Can retrieve using pair2 because they have the same hash code and are equal
     * String value = map.get(pair2);   // returns "value"
     * }</pre>
     *
     * @return a hash code value for this pair.
     */
    @Override
    public int hashCode() {
        return N.hashCode(left) ^ N.hashCode(right);
    }

    /**
     * Compares this pair to the specified object for equality.
     * Returns {@code true} if and only if the specified object is also a {@code Map.Entry},
     * and both entries have equal keys and values.
     *
     * <p>Two elements are considered equal if they are both {@code null},
     * or if they are equal according to their equals() method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair1 = Pair.of("Hello", 42);
     * Pair<String, Integer> pair2 = Pair.of("Hello", 42);
     * Pair<String, Integer> pair3 = Pair.of("Hello", 43);
     *
     * pair1.equals(pair2);     // returns true
     * pair1.equals(pair3);     // returns false
     * pair1.equals("Hello");   // returns false (different type)
     *
     * Pair<String, Integer> pair4 = Pair.of(null, null);
     * Pair<String, Integer> pair5 = Pair.of(null, null);
     * pair4.equals(pair5);   // returns true
     * }</pre>
     *
     * <p><b>Note:</b> Equality is defined against the {@link Map.Entry} interface, not against {@code Pair}
     * specifically. Any {@code Map.Entry} with an equal key and value is considered equal,
     * so {@code Pair.of(k, v).equals(Map.entry(k, v))} is {@code true}. A {@code Tuple2} (which
     * is not a {@code Map.Entry}) is never equal to a {@code Pair}, even with the same elements.
     *
     * @param obj the object to be compared for equality with this pair.
     * @return {@code true} if the specified object is equal to this pair, {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Map.Entry<?, ?> other) {
            return N.equals(left, other.getKey()) && N.equals(right, other.getValue());
        }

        return false;
    }

    /**
     * Returns a string representation of this pair.
     * The string representation consists of the left and right elements
     * enclosed in parentheses and separated by a comma and space.
     *
     * <p>The format is: {@code (left, right)}</p>
     *
     * <p>This parenthesized, comma-separated format is similar to Apache Commons Lang's
     * {@code Pair.toString()}, but not character-identical: this implementation includes a space
     * after the comma ({@code (left, right)}) whereas Commons Lang uses none ({@code (left,right)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * System.out.println(pair);   // prints (Hello, 42)
     *
     * Pair<String, String> nullPair = Pair.of(null, "World");
     * System.out.println(nullPair);   // prints (null, World)
     * }</pre>
     *
     * @return a string representation of this pair in the format (left, right).
     */
    @Override
    public String toString() {
        return "(" + N.toString(left) + ", " + N.toString(right) + ")"; // parenthesized, comma+space form (similar to Apache Commons Lang Pair, which uses no space)
    }
}
