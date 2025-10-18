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
 * can be of any type, including null. This class is particularly useful when you need
 * to return two values from a method or store two related objects together.</p>
 * 
 * <p>Example usage:</p>
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
 * @param <L> the type of the left element
 * @param <R> the type of the right element
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.Holder
 * @see com.landawn.abacus.util.Result
 * @see com.landawn.abacus.util.Triple
 * @see com.landawn.abacus.util.Tuple
 */
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
public final class Pair<L, R> implements Map.Entry<L, R>, Mutable {
    // implements Map.Entry<L, R> {
    private L left; //NOSONAR

    private R right; //NOSONAR

    /**
     * Constructs an empty Pair with both left and right elements initialized to null.
     * This constructor is useful when you need to create a pair and set its values later.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = new Pair<>();
     * pair.setLeft("Hello");
     * pair.setRight(42);
     * }</pre>
     */
    public Pair() {
    }

    Pair(final L l, final R r) {
        setLeft(l);
        setRight(r);
    }

    /**
     * Creates a new Pair with the specified left and right elements.
     * This is the preferred way to create a Pair instance as it provides
     * a more concise syntax than using the constructor.
     * 
     * <p>Both elements can be null. The returned pair is mutable, allowing
     * modification of its elements after creation.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> coordinates = Pair.of("X", 10);
     * Pair<String, String> nullPair = Pair.of(null, null); // Valid
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <R> the type of the right element
     * @param l the left element of the pair (can be null)
     * @param r the right element of the pair (can be null)
     * @return a new Pair containing the specified left and right elements
     */
    public static <L, R> Pair<L, R> of(final L l, final R r) {
        return new Pair<>(l, r);
    }

    /**
     * Creates a new Pair from an existing Map.Entry.
     * The key of the entry becomes the left element of the pair,
     * and the value becomes the right element.
     * 
     * <p>This method is useful when working with Map entries and you need
     * to convert them to Pair objects for further processing.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("age", 25);
     * Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
     * Pair<String, Integer> pair = Pair.create(entry);
     * // pair.left returns "age", pair.right returns 25
     * }</pre>
     *
     * @param <K> the key type of the Map.Entry, which becomes the left element type
     * @param <V> the value type of the Map.Entry, which becomes the right element type
     * @param entry the Map.Entry to convert to a Pair (must not be null)
     * @return a new Pair with the key as the left element and the value as the right element
     */
    public static <K, V> Pair<K, V> create(final Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    private static final Pair<?, ?>[] EMPTY_ARRAY = new Pair[0];

    /**
     * Returns a type-safe empty array of Pairs.
     * This method is useful when you need to return an empty array of Pairs
     * from a method or pass it as a parameter, avoiding the need to create
     * a new empty array each time.
     * 
     * <p>The returned array is immutable and shared across all calls to this method.
     * Attempting to modify the returned array will result in an exception.</p>
     * 
     * <p>Example:</p>
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
     * @param <L> the type of the left elements in the array
     * @param <R> the type of the right elements in the array
     * @return an empty array of Pairs
     */
    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R>[] emptyArray() {
        return (Pair<L, R>[]) EMPTY_ARRAY;
    }

    /**
     * Returns the left element of this pair.
     * This is the preferred method for accessing the left element,
     * providing a more concise alternative to {@link #getLeft()}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * String left = pair.left(); // Returns "Hello"
     * }</pre>
     *
     * @return the left element of this pair (can be null)
     */
    public L left() {
        return this.left;
    }

    /**
     * Returns the right element of this pair.
     * This is the preferred method for accessing the right element,
     * providing a more concise alternative to {@link #getRight()}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Integer right = pair.right(); // Returns 42
     * }</pre>
     *
     * @return the right element of this pair (can be null)
     */
    public R right() {
        return this.right;
    }

    /**
     * Gets the left element of this pair.
     * 
     * @return the left element of the pair
     * @deprecated This method is deprecated in favor of the more concise {@link #left()} method.
     * @see #left()
     */
    @Deprecated
    public L getLeft() {
        return left;
    }

    /**
     * Sets the left element of this pair to the specified value.
     * The value can be null. This method modifies the pair in place.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * pair.setLeft("New");
     * // pair.left now returns "New"
     * }</pre>
     *
     * @param left the new value for the left element (can be null)
     */
    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * Gets the right element of this pair.
     * 
     * @return the right element of the pair
     * @deprecated This method is deprecated in favor of the more concise {@link #right()} method.
     * @see #right()
     */
    @Deprecated
    public R getRight() {
        return right;
    }

    /**
     * Sets the right element of this pair to the specified value.
     * The value can be null. This method modifies the pair in place.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * pair.setRight(20);
     * // pair.right now returns 20
     * }</pre>
     *
     * @param right the new value for the right element (can be null)
     */
    public void setRight(final R right) {
        this.right = right;
    }

    /**
     * Sets both the left and right elements of this pair in a single operation.
     * This is a convenience method equivalent to calling {@link #setLeft(Object)}
     * followed by {@link #setRight(Object)}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * pair.set("New", 20);
     * // pair.left returns "New", pair.right returns 20
     * }</pre>
     *
     * @param left the new value for the left element (can be null)
     * @param right the new value for the right element (can be null)
     */
    public void set(final L left, final R right) {
        this.setLeft(left);
        this.setRight(right);
    }

    /**
     * Returns the current left value and then sets it to the specified new value.
     * This method is useful when you need to retrieve the old value while updating it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * String oldLeft = pair.getAndSetLeft("New");
     * // oldLeft contains "Old", pair.left returns "New"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element (can be null)
     * @return the previous value of the left element (can be null)
     */
    public L getAndSetLeft(final L newLeft) {
        final L res = left;
        setLeft(newLeft);
        return res;
    }

    /**
     * Sets the left element to the specified value and then returns the new value.
     * This method is useful when you want to update the value and immediately
     * use the new value in subsequent operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Old", 10);
     * String newLeft = pair.setAndGetLeft("New");
     * // newLeft contains "New", pair.left also returns "New"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element (can be null)
     * @return the new value of the left element (same as the parameter)
     */
    public L setAndGetLeft(final L newLeft) {
        setLeft(newLeft);
        return left;
    }

    /**
     * Returns the current right value and then sets it to the specified new value.
     * This method is useful when you need to retrieve the old value while updating it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * Integer oldRight = pair.getAndSetRight(20);
     * // oldRight contains 10, pair.right returns 20
     * }</pre>
     *
     * @param newRight the new value to set for the right element (can be null)
     * @return the previous value of the right element (can be null)
     */
    public R getAndSetRight(final R newRight) {
        final R res = right;
        setRight(newRight);
        return res;
    }

    /**
     * Sets the right element to the specified value and then returns the new value.
     * This method is useful when you want to update the value and immediately
     * use the new value in subsequent operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * Integer newRight = pair.setAndGetRight(20);
     * // newRight contains 20, pair.right also returns 20
     * }</pre>
     *
     * @param newRight the new value to set for the right element (can be null)
     * @return the new value of the right element (same as the parameter)
     */
    public R setAndGetRight(final R newRight) {
        setRight(newRight);
        return right;
    }

    /**
     * Sets the left element to the specified value if and only if the given predicate
     * evaluates to true. The predicate receives both the current pair and the new value
     * as parameters for evaluation.
     * 
     * <p>This method is useful for conditional updates where you want to change
     * the left value only when certain conditions are met.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * boolean updated = pair.setLeftIf("World", (p, newVal) -> p.right > 5);
     * // updated is true, pair.left returns "World" because 10 > 5
     * 
     * updated = pair.setLeftIf("Test", (p, newVal) -> p.right > 20);
     * // updated is false, pair.left still returns "World" because 10 is not > 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newLeft the new value to potentially set for the left element (can be null)
     * @param predicate the condition to test; receives the current pair as the first parameter
     *                  and the new left value as the second parameter
     * @return {@code true} if the value was updated (predicate returned true), {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setLeftIf(final L newLeft, final Throwables.BiPredicate<? super Pair<L, R>, ? super L, E> predicate) throws E {
        if (predicate.test(this, newLeft)) {
            setLeft(newLeft);
            return true;
        }

        return false;
    }

    /**
     * Sets the right element to the specified value if and only if the given predicate
     * evaluates to true. The predicate receives both the current pair and the new value
     * as parameters for evaluation.
     * 
     * <p>This method is useful for conditional updates where you want to change
     * the right value only when certain conditions are met.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * boolean updated = pair.setRightIf(20, (p, newVal) -> p.left.length() > 3);
     * // updated is true, pair.right returns 20 because "Hello".length() > 3
     * 
     * updated = pair.setRightIf(30, (p, newVal) -> newVal < p.right);
     * // updated is false, pair.right still returns 20 because 30 is not < 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newRight the new value to potentially set for the right element (can be null)
     * @param predicate the condition to test; receives the current pair as the first parameter
     *                  and the new right value as the second parameter
     * @return {@code true} if the value was updated (predicate returned true), {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setRightIf(final R newRight, final Throwables.BiPredicate<? super Pair<L, R>, ? super R, E> predicate) throws E {
        if (predicate.test(this, newRight)) {
            setRight(newRight);
            return true;
        }

        return false;
    }

    /**
     * Sets both the left and right elements to the specified values if and only if
     * the given predicate evaluates to true. The predicate receives the current pair,
     * the new left value, and the new right value as parameters for evaluation.
     * 
     * <p>This method is useful for conditional updates where you want to change
     * both values atomically only when certain conditions are met. If the predicate
     * returns false, neither value is changed.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 10);
     * boolean updated = pair.setIf("World", 20, 
     *     (p, newL, newR) -> p.left.length() + p.right < 20);
     * // updated is true, pair is now ("World", 20) because 5 + 10 < 20
     * 
     * updated = pair.setIf("Test", 5,
     *     (p, newL, newR) -> newL.length() > newR);
     * // updated is false, pair remains ("World", 20) because 4 is not > 5
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newLeft the new value to potentially set for the left element (can be null)
     * @param newRight the new value to potentially set for the right element (can be null)
     * @param predicate the condition to test; receives the current pair as the first parameter,
     *                  the new left value as the second parameter, and the new right value as
     *                  the third parameter
     * @return {@code true} if both values were updated (predicate returned true), {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final L newLeft, final R newRight,
            final Throwables.TriPredicate<? super Pair<L, R>, ? super L, ? super R, E> predicate) throws E {
        if (predicate.test(this, newLeft, newRight)) {
            setLeft(newLeft);
            setRight(newRight);
            return true;
        }

        return false;
    }

    /**
     * Creates and returns a new Pair with the left and right elements swapped.
     * The original pair remains unchanged. This method is marked as @Beta,
     * indicating it may be subject to change in future versions.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> original = Pair.of("Hello", 42);
     * Pair<Integer, String> reversed = original.reverse();
     * // reversed.left returns 42, reversed.right returns "Hello"
     * // original remains unchanged
     * }</pre>
     *
     * @return a new Pair with the right element as the left and the left element as the right
     */
    @Beta
    public Pair<R, L> reverse() {
        return new Pair<>(right, left);
    }

    /**
     * Creates and returns a shallow copy of this pair.
     * The new pair contains the same left and right references as this pair.
     * Note that this is a shallow copy - if the elements are mutable objects,
     * changes to those objects will be reflected in both pairs.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, List<Integer>> original = Pair.of("Numbers", new ArrayList<>());
     * original.right.add(1);
     * 
     * Pair<String, List<Integer>> copy = original.copy();
     * copy.right.add(2);
     * // Both original and copy will have [1, 2] in their right element
     * 
     * copy.setLeft("NewNumbers");
     * // original.left still returns "Numbers", copy.left returns "NewNumbers"
     * }</pre>
     *
     * @return a new Pair containing the same left and right elements as this pair
     */
    public Pair<L, R> copy() {
        return new Pair<>(left, right);
    }

    /**
     * Converts this pair to an Object array containing two elements.
     * The first element of the array is the left element of the pair,
     * and the second element is the right element. Either or both can be null.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Object[] array = pair.toArray();
     * // array[0] equals "Hello", array[1] equals 42
     * // array.length equals 2
     * }</pre>
     *
     * @return a new Object array of length 2 containing [left, right]
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
     * <p>This method follows the contract of {@link java.util.Collection#toArray(Object[])}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, String> pair = Pair.of("Hello", "World");
     * 
     * // Using an array with sufficient size
     * String[] existing = new String[3];
     * String[] result = pair.toArray(existing);
     * // result == existing, result[0] = "Hello", result[1] = "World", result[2] = null
     * 
     * // Using an array with insufficient size
     * String[] small = new String[1];
     * String[] result2 = pair.toArray(small);
     * // result2 != small, result2.length = 2, result2[0] = "Hello", result2[1] = "World"
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which the elements are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing the left element at index 0 and the right element at index 1
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
     * <p>Example:</p>
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
     * @param <E> the type of exception that the consumer may throw
     * @param consumer the action to be performed on each element; must accept a common
     *                 supertype of both L and R (typically Object)
     * @throws E if the consumer throws an exception
     * @throws ClassCastException if the consumer cannot accept the types of the elements
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
        final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

        objConsumer.accept(left);
        objConsumer.accept(right);
    }

    /**
     * Performs the given action with both elements of this pair as arguments.
     * This method is useful when you need to process both elements together
     * in a single operation.
     * 
     * <p>Example:</p>
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
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with the left and right elements as arguments
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super L, ? super R, E> action) throws E {
        action.accept(left, right);
    }

    /**
     * Performs the given action with this pair as the argument.
     * This method is useful when you want to process the pair as a whole
     * rather than its individual elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Score", 100);
     * List<Pair<String, Integer>> pairs = new ArrayList<>();
     * pair.accept(pairs::add);
     * // pairs now contains the pair
     * 
     * pair.accept(p -> {
     *     if (p.right > 50) {
     *         System.out.println(p.left + " is high");
     *     }
     * });
     * // Prints "Score is high"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with this pair as the argument
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Pair<L, R>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Applies the given function to both elements of this pair and returns the result.
     * The function receives the left element as the first argument and the right element
     * as the second argument.
     * 
     * <p>Example:</p>
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
     * @param <U> the type of the result
     * @param <E> the type of exception that the mapper function may throw
     * @param mapper the function to apply to the left and right elements
     * @return the result of applying the mapper function to both elements
     * @throws E if the mapper function throws an exception
     */
    public <U, E extends Exception> U map(final Throwables.BiFunction<? super L, ? super R, ? extends U, E> mapper) throws E {
        return mapper.apply(left, right);
    }

    /**
     * Applies the given function to this pair and returns the result.
     * The function receives the entire pair as its argument, allowing it to
     * access both elements and any methods of the pair.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> entry = Pair.of("Temperature", 25);
     * String formatted = entry.map(p -> p.left + ": " + p.right + "°C");
     * // formatted equals "Temperature: 25°C"
     * 
     * Boolean isValid = entry.map(p -> p.left != null && p.right != null);
     * // isValid equals true
     * }</pre>
     *
     * @param <U> the type of the result
     * @param <E> the type of exception that the mapper function may throw
     * @param mapper the function to apply to this pair
     * @return the result of applying the mapper function to this pair
     * @throws E if the mapper function throws an exception
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
     * <p>Example:</p>
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test with the left and right elements
     * @return an Optional containing this pair if the predicate returns true,
     *         otherwise an empty Optional
     * @throws E if the predicate throws an exception
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
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Test", 10);
     * 
     * Optional<Pair<String, Integer>> filtered = pair.filter(
     *     p -> p.left.startsWith("T") && p.right % 2 == 0
     * );
     * // filtered.isPresent() is true
     * 
     * Optional<Pair<String, Integer>> filtered2 = pair.filter(
     *     p -> p.left.length() + p.right > 20
     * );
     * // filtered2.isPresent() is false because 4 + 10 = 14, which is not > 20
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test with this pair
     * @return an Optional containing this pair if the predicate returns true,
     *         otherwise an empty Optional
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Optional<Pair<L, R>> filter(final Throwables.Predicate<? super Pair<L, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    /**
     * Converts this pair to a Tuple2 (2-element tuple).
     * Tuples are similar to pairs but may provide different functionality
     * or be required by certain APIs.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * Tuple2<String, Integer> tuple = pair.toTuple();
     * // tuple._1 equals "Hello", tuple._2 equals 42
     * }</pre>
     *
     * @return a new Tuple2 containing the same elements as this pair
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
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Age", 30);
     * Map.Entry<String, Integer> entry = pair.toImmutableEntry();
     * // entry.getKey() returns "Age", entry.getValue() returns 30
     * // entry.setValue(31) would throw UnsupportedOperationException
     * }</pre>
     *
     * @return a new immutable Map.Entry with the same key-value mapping as this pair
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
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * String key = entry.getKey(); // Returns "key"
     * }</pre>
     *
     * @return the left element of this pair (can be null)
     * @deprecated Use {@link #left()} instead for cleaner code
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
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * Integer value = entry.getValue(); // Returns 100
     * }</pre>
     *
     * @return the right element of this pair (can be null)
     * @deprecated Use {@link #right()} instead for cleaner code
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
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("key", 100);
     * Map.Entry<String, Integer> entry = pair;
     * Integer oldValue = entry.setValue(200);
     * // oldValue equals 100, pair.right() now returns 200
     * }</pre>
     *
     * @param value the new value to set for the right element (can be null)
     * @return the previous value of the right element (can be null)
     * @deprecated Use {@link #setRight(Object)} or {@link #getAndSetRight(Object)} instead
     */
    @Deprecated
    @Override
    public R setValue(final R value) {
        return getAndSetRight(value);
    }

    /**
     * Returns a hash code value for this pair.
     * The hash code is computed using both the left and right elements.
     * 
     * <p>This implementation ensures that pairs with the same elements
     * in the same order will have the same hash code, making them suitable
     * for use as keys in hash-based collections.</p>
     *
     * @return a hash code value for this pair
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(left);
        return prime * result + N.hashCode(right);
    }

    /**
     * Compares this pair to the specified object for equality.
     * Returns true if and only if the specified object is also a Pair,
     * and both pairs have equal left and right elements.
     * 
     * <p>Two elements are considered equal if they are both null,
     * or if they are equal according to their equals() method.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair1 = Pair.of("Hello", 42);
     * Pair<String, Integer> pair2 = Pair.of("Hello", 42);
     * Pair<String, Integer> pair3 = Pair.of("Hello", 43);
     * 
     * pair1.equals(pair2); // returns true
     * pair1.equals(pair3); // returns false
     * pair1.equals("Hello"); // returns {@code false} (different type)
     * 
     * Pair<String, Integer> pair4 = Pair.of(null, null);
     * Pair<String, Integer> pair5 = Pair.of(null, null);
     * pair4.equals(pair5); // returns true
     * }</pre>
     *
     * @param obj the object to be compared for equality with this pair
     * @return {@code true} if the specified object is equal to this pair, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Pair<?, ?> other) {
            return N.equals(left, other.left) && N.equals(right, other.right);
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
     * <p>This format is compatible with Apache Commons Lang's Pair.toString()
     * for consistency across libraries.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("Hello", 42);
     * System.out.println(pair); // Prints: (Hello, 42)
     * 
     * Pair<String, String> nullPair = Pair.of(null, "World");
     * System.out.println(nullPair); // Prints: (null, World)
     * }</pre>
     *
     * @return a string representation of this pair in the format (left, right)
     */
    @Override
    public String toString() {
        return "(" + N.toString(left) + ", " + N.toString(right) + ")"; // To align with Pair.toString() in Apache Commons Lang
    }
}
