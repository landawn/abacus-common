/*
 * Copyright (c) 2022, Haiyang Li.
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

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

/**
 * This class represents a Holder that can hold a value of type {@code T}.
 * It implements the Mutable interface, indicating that the value it holds can be changed.
 * The class provides methods to set and get the value, as well as other utility methods for operations like checking if the value is {@code null},
 * updating the value, and performing actions if the value is not {@code null}.
 *
 * @param <T> The type of the value this Holder can hold.
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.Result
 * @see com.landawn.abacus.util.Pair
 * @see com.landawn.abacus.util.Triple
 * @see com.landawn.abacus.util.Tuple
 */
public final class Holder<T> implements Mutable {

    private T value;

    /**
     * Constructs a new Holder with a {@code null} value.
     *
     * <p>This is equivalent to calling {@code Holder(null)}. The created Holder can later
     * be populated with a value using {@link #setValue(Object)} or other setter methods.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = new Holder<>();
     * holder.setValue("Hello");
     * }</pre>
     */
    public Holder() {
        this(null);
    }

    /**
     * Constructs a new Holder with the specified initial value.
     * This constructor is package-private and intended for internal use.
     * Use {@link #of(Object)} to create new Holder instances.
     *
     * @param value the initial value to be held by this Holder, may be {@code null}
     */
    Holder(final T value) {
        this.value = value;
    }

    /**
     * Creates a new Holder instance containing the specified value.
     *
     * <p>This is a static factory method that provides a convenient and type-safe way to create
     * a Holder instance. The method allows for type inference, making the code more concise.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(42);
     * Holder<String> nullHolder = Holder.of(null);
     * }</pre>
     *
     * @param <T> the type of the value to be held
     * @param value the value to be held by the new Holder, may be {@code null}
     * @return a new Holder instance containing the specified value
     */
    public static <T> Holder<T> of(final T value) {
        return new Holder<>(value);
    }

    /**
     * Returns the value held by this Holder.
     *
     * <p>This method provides direct access to the held value without any null checks or
     * validation. The caller is responsible for handling potential {@code null} values.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("test");
     * String value = holder.value(); // returns "test"
     * }</pre>
     *
     * @return the value held by this Holder, or {@code null} if no value is set
     */
    public T value() {
        return value;
    }

    /**
     * Returns the value held by this Holder.
     * The value may be {@code null}.
     *
     * @return the value held by this Holder, or {@code null} if no value is set
     * @deprecated This method is deprecated in favor of the more concise {@link #value()} method.
     */
    @Deprecated
    public T getValue() {
        return value;
    }

    /**
     * Sets the value held by this Holder to the specified value.
     *
     * <p>This method replaces any existing value with the new value. The new value can be
     * {@code null}, which effectively clears the Holder. No value is returned; use
     * {@link #setAndGet(Object)} if you need the value returned.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(10);
     * holder.setValue(20); // holder now contains 20
     * }</pre>
     *
     * @param value the new value to be held by this Holder, may be {@code null}
     */
    public void setValue(final T value) {
        this.value = value;
    }

    /**
     * Atomically sets the value to the given updated value and returns the previous value.
     *
     * <p>This method performs a swap operation: it sets the new value and returns what was
     * previously held. This is particularly useful in scenarios where you need to track state
     * changes or implement swap-based algorithms.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("old");
     * String previous = holder.getAndSet("new"); // previous = "old", holder now contains "new"
     * }</pre>
     *
     * @param value the new value to be set, may be {@code null}
     * @return the previous value held by this Holder before the update, may be {@code null}
     */
    public T getAndSet(final T value) {
        final T result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value to the given updated value and returns the new value.
     *
     * <p>This method sets a new value and returns that same value, allowing for method chaining
     * or immediate usage of the newly set value. While the return value is the same as the
     * parameter, this pattern is useful in functional programming contexts.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(10);
     * Integer newValue = holder.setAndGet(20); // newValue = 20, holder contains 20
     * }</pre>
     *
     * @param value the new value to be set, may be {@code null}
     * @return the new value that was just set (same as the parameter), may be {@code null}
     */
    public T setAndGet(final T value) {
        this.value = value;
        return this.value;
    }

    /**
     * Atomically updates the current value with the results of applying the given function,
     * returning the previous value.
     *
     * <p>The function is applied to the current value to compute a new value, which then replaces
     * the current value. This method returns the original value before the update. This is useful
     * when you need to perform transformations while keeping track of the old state.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(5);
     * Integer old = holder.getAndUpdate(n -> n * 2); // old = 5, holder now contains 10
     * }</pre>
     *
     * @param <E> the type of exception that the update function may throw
     * @param updateFunction a function that takes the current value and returns a new value, must not be {@code null}
     * @return the previous value held by this Holder before the update, may be {@code null}
     * @throws E if the update function throws an exception
     */
    public <E extends Exception> T getAndUpdate(final Throwables.UnaryOperator<T, E> updateFunction) throws E { // NOSONAR
        final T res = value;
        value = updateFunction.apply(value);
        return res;
    }

    /**
     * Atomically updates the current value with the results of applying the given function,
     * returning the updated value.
     *
     * <p>The function is applied to the current value to compute a new value, which then replaces
     * the current value. Unlike {@link #getAndUpdate(Throwables.UnaryOperator)}, this method
     * returns the new value after the update rather than the old value.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(5);
     * Integer updated = holder.updateAndGet(n -> n * 2); // updated = 10, holder contains 10
     * }</pre>
     *
     * @param <E> the type of exception that the update function may throw
     * @param updateFunction a function that takes the current value and returns a new value, must not be {@code null}
     * @return the new value held by this Holder after the update, may be {@code null}
     * @throws E if the update function throws an exception
     */
    public <E extends Exception> T updateAndGet(final Throwables.UnaryOperator<T, E> updateFunction) throws E { // NOSONAR
        value = updateFunction.apply(value);
        return value;
    }

    /**
     * Sets the value to the specified new value if the given predicate returns {@code true}
     * when tested with the current value.
     *
     * <p>This method provides conditional update functionality. The predicate is evaluated
     * against the current value, and only if it returns {@code true} will the new value be set.
     * This is useful for implementing compare-and-set semantics or conditional updates based
     * on the current state.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(5);
     * boolean updated = holder.setIf(10, n -> n != null && n < 10); // returns true, holder now contains 10
     * boolean notUpdated = holder.setIf(3, n -> n != null && n < 5); // returns false, holder still contains 10
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newValue the new value to set if the predicate returns {@code true}, may be {@code null}
     * @param predicate the predicate that tests the current value, must not be {@code null}
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final T newValue, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Sets the value to the specified new value if the given predicate returns {@code true}
     * when tested with both the current value and the new value. If the predicate returns
     * {@code false}, the value remains unchanged.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newValue the new value to set if the predicate returns {@code true}, may be {@code null}
     * @param predicate the predicate that tests both the current value (first parameter) and the new value (second parameter)
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     * @deprecated use {@link #setIf(Object, Throwables.Predicate)} instead
     */
    @Deprecated
    public <E extends Exception> boolean setIf(final T newValue, final Throwables.BiPredicate<? super T, ? super T, E> predicate) throws E {
        if (predicate.test(value, newValue)) {
            value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Checks whether the value held by this Holder is {@code null}.
     *
     * <p>This is a convenience method for null-checking the held value without directly
     * accessing it. It's particularly useful in conditional logic and stream operations.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of(null);
     * if (holder.isNull()) {
     *     holder.setValue("default");
     * }
     * }</pre>
     *
     * @return {@code true} if the value is {@code null}, {@code false} otherwise
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Checks whether the value held by this Holder is not {@code null}.
     *
     * <p>This is the inverse of {@link #isNull()}. It provides a convenient way to check
     * if the Holder contains a non-null value before performing operations on it.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("test");
     * if (holder.isNotNull()) {
     *     System.out.println(holder.value().length()); // safe to use
     * }
     * }</pre>
     *
     * @return {@code true} if the value is not {@code null}, {@code false} otherwise
     */
    public boolean isNotNull() {
        return value != null;
    }

    /**
     * Performs the given action with the value if the value is not {@code null}.
     *
     * <p>This method enables conditional execution based on the presence of a value. The action
     * is only executed if the value is not {@code null}, making it similar to {@code Optional.ifPresent()}.
     * This is particularly useful for side-effect operations in functional-style code.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("Hello");
     * holder.ifNotNull(value -> System.out.println(value)); // prints "Hello"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with the non-null value, must not be {@code null}
     * @throws IllegalArgumentException if the action is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void ifNotNull(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, "action"); //NOSONAR

        if (isNotNull()) {
            action.accept(value);
        }
    }

    /**
     * Performs the given action with the value if the value is not {@code null},
     * otherwise performs the given empty action.
     *
     * <p>This method provides an if-else pattern for handling both the presence and absence of
     * a value. Exactly one of the two actions will be performed. This is similar to
     * {@code Optional.ifPresentOrElse()} and is useful when you need to handle both cases.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of(null);
     * holder.ifNotNullOrElse(
     *     value -> System.out.println("Value: " + value),
     *     () -> System.out.println("No value present")
     * ); // prints "No value present"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param <E2> the type of exception that the empty action may throw
     * @param action the action to be performed with the non-null value, must not be {@code null}
     * @param emptyAction the action to be performed when the value is {@code null}, must not be {@code null}
     * @throws IllegalArgumentException if either action or emptyAction is {@code null}
     * @throws E if the action throws an exception
     * @throws E2 if the emptyAction throws an exception
     */
    public <E extends Exception, E2 extends Exception> void ifNotNullOrElse(final Throwables.Consumer<? super T, E> action,
            final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(action, "action");
        N.checkArgNotNull(emptyAction, "emptyAction");

        if (isNotNull()) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * Performs the given action with the value held by this Holder.
     * The action is performed regardless of whether the value is {@code null} or not.
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with the value
     * @throws E if the action throws an exception
     * @deprecated use {@link #ifNotNull(Throwables.Consumer)} for conditional execution or direct value access for unconditional use
     */
    @Deprecated
    public <E extends Exception> void accept(final Throwables.Consumer<? super T, E> action) throws E {
        action.accept(value);
    }

    /**
     * Performs the given action with the value if the value is not {@code null}.
     * If the value is {@code null}, no action is performed.
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with the non-null value
     * @throws IllegalArgumentException if the action is {@code null}
     * @throws E if the action throws an exception
     * @deprecated replaced by {@link #ifNotNull(Throwables.Consumer)}
     */
    @Deprecated
    public <E extends Exception> void acceptIfNotNull(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, "action");

        if (isNotNull()) {
            action.accept(value);
        }
    }

    /**
     * Applies the given mapping function to the value held by this Holder and returns the result.
     *
     * <p>This method unconditionally applies the mapper function to the current value, even if
     * it is {@code null}. The mapper is responsible for handling {@code null} inputs if necessary.
     * For conditional mapping that only applies to non-null values, use {@link #mapIfNotNull(Throwables.Function)}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("hello");
     * Integer length = holder.map(s -> s == null ? 0 : s.length()); // returns 5
     * }</pre>
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the value, must not be {@code null}
     * @return the result of applying the mapping function to the value, may be {@code null}
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
        return mapper.apply(value);
    }

    /**
     * Applies the given mapping function to the value if the value is not {@code null},
     * and returns a {@code Nullable} containing the mapped value.
     *
     * <p>This method provides safe, null-aware mapping. The mapper is only invoked when the
     * value is not {@code null}, eliminating the need for null checks within the mapper.
     * The result is wrapped in a {@code Nullable}, which can itself contain {@code null}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("hello");
     * Nullable<Integer> length = holder.mapIfNotNull(String::length); // Nullable.of(5)
     * }</pre>
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the non-null value, must not be {@code null}
     * @return a {@code Nullable} containing the mapped value if the value was not {@code null}, otherwise an empty {@code Nullable}
     * @throws IllegalArgumentException if the mapper is {@code null}
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, "mapper");

        if (isNotNull()) {
            return Nullable.of(mapper.apply(value));
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Applies the given mapping function to the value if the value is not {@code null},
     * and returns an {@code Optional} containing the mapped value.
     *
     * <p>This method is similar to {@link #mapIfNotNull(Throwables.Function)} but returns an
     * {@code Optional} instead of {@code Nullable}. The key difference is that the mapper must
     * return a non-null value, which is then wrapped in an {@code Optional}. If the original
     * value is {@code null}, an empty {@code Optional} is returned.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("test");
     * Optional<Integer> length = holder.mapToNonNullIfNotNull(String::length); // Optional.of(4)
     * }</pre>
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the non-null value, must not be {@code null} and must not return {@code null}
     * @return an {@code Optional} containing the mapped value if the value was not {@code null}, otherwise an empty {@code Optional}
     * @throws IllegalArgumentException if the mapper is {@code null}
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, "mapper");

        if (isNotNull()) {
            return Optional.of(mapper.apply(value));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Tests the value with the given predicate and returns a {@code Nullable} containing the value
     * if the predicate returns {@code true}, otherwise returns an empty {@code Nullable}.
     *
     * <p>This method applies the predicate unconditionally to the current value, even if it is
     * {@code null}. The predicate must handle {@code null} inputs appropriately. For conditional
     * filtering that only applies to non-null values, use {@link #filterIfNotNull(Throwables.Predicate)}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(10);
     * Nullable<Integer> filtered = holder.filter(n -> n != null && n > 5); // Nullable.of(10)
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test the value, must not be {@code null}
     * @return a {@code Nullable} containing the value if the predicate returns {@code true}, otherwise an empty {@code Nullable}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Nullable<T> filter(final Throwables.Predicate<? super T, E> predicate) throws E {
        if (predicate.test(value)) {
            return Nullable.of(value);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Tests the value with the given predicate if the value is not {@code null} and returns
     * an {@code Optional} containing the value if the predicate returns {@code true}.
     *
     * <p>This method provides safe, null-aware filtering. The predicate is only invoked when the
     * value is not {@code null}, eliminating the need for null checks within the predicate.
     * If the value is {@code null} or the predicate returns {@code false}, an empty {@code Optional}
     * is returned.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Integer> holder = Holder.of(10);
     * Optional<Integer> filtered = holder.filterIfNotNull(n -> n > 5); // Optional.of(10)
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test the non-null value, must not be {@code null}
     * @return an {@code Optional} containing the value if it is not {@code null} and the predicate returns {@code true}, otherwise an empty {@code Optional}
     * @throws IllegalArgumentException if the predicate is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Optional<T> filterIfNotNull(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate, "predicate");

        if (isNotNull() && predicate.test(value)) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise returns the given default value.
     *
     * <p>This method provides a simple way to supply a default value when the Holder contains
     * {@code null}. The default value is evaluated eagerly. For lazy evaluation, use
     * {@link #orElseGetIfNull(Supplier)}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of(null);
     * String value = holder.orElseIfNull("default"); // returns "default"
     * }</pre>
     *
     * @param other the value to be returned if the held value is {@code null}, may be {@code null}
     * @return the value if not {@code null}, otherwise {@code other}
     */
    public T orElseIfNull(final T other) {
        return isNotNull() ? value : other;
    }

    /**
     * Returns the value if it is not {@code null}, otherwise returns the result produced by the supplying function.
     *
     * <p>This method provides lazy evaluation of the default value. The supplier is only invoked
     * when the value is {@code null}, which is useful when computing the default value is expensive
     * or has side effects. For simple default values, use {@link #orElseIfNull(Object)}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of(null);
     * String value = holder.orElseGetIfNull(() -> computeExpensiveDefault()); // supplier only called if needed
     * }</pre>
     *
     * @param other a {@code Supplier} whose result is returned if the held value is {@code null}, must not be {@code null}
     * @return the value if not {@code null}, otherwise the result of {@code other.get()}
     * @throws IllegalArgumentException if the supplier is {@code null}
     */
    public T orElseGetIfNull(final Supplier<? extends T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");

        if (isNotNull()) {
            return value;
        } else {
            return other.get();
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}.
     *
     * <p>This method is useful when the absence of a value should be treated as an error condition.
     * It enforces that a non-null value must be present or an exception will be thrown.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("value");
     * String value = holder.orElseThrowIfNull(); // returns "value"
     * }</pre>
     *
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    public T orElseThrowIfNull() throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NULL_ELEMENT_EX);
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}
     * with the specified error message.
     *
     * <p>This method allows you to provide a custom error message that will be included in the
     * exception if the value is {@code null}. This helps provide more context about what went wrong.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of(null);
     * String value = holder.orElseThrowIfNull("User name is required"); // throws with custom message
     * }</pre>
     *
     * @param errorMessage the detail message to be used in the exception if the value is {@code null}
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    @Beta
    public T orElseThrowIfNull(final String errorMessage) throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(errorMessage);
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}
     * with a formatted error message using the specified message template and parameter.
     *
     * <p>This method allows you to provide a parameterized error message. The message template
     * can contain placeholders that will be replaced with the provided parameter value.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<User> holder = Holder.of(null);
     * User user = holder.orElseThrowIfNull("User with id %s not found", userId);
     * }</pre>
     *
     * @param errorMessage the error message template which may contain a placeholder for the parameter
     * @param param the parameter to be substituted into the error message
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    @Beta
    public T orElseThrowIfNull(final String errorMessage, final Object param) throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(N.format(errorMessage, param));
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}
     * with a formatted error message using the specified message template and two parameters.
     *
     * <p>This overload allows formatting an error message with two parameters, useful for more
     * detailed error reporting.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Item> holder = Holder.of(null);
     * Item item = holder.orElseThrowIfNull("Item %s in category %s not found", itemId, categoryId);
     * }</pre>
     *
     * @param errorMessage the error message template which may contain placeholders for the parameters
     * @param param1 the first parameter to be substituted into the error message
     * @param param2 the second parameter to be substituted into the error message
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    @Beta
    public T orElseThrowIfNull(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(N.format(errorMessage, param1, param2));
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}
     * with a formatted error message using the specified message template and three parameters.
     *
     * <p>This overload allows formatting an error message with three parameters.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Record> holder = Holder.of(null);
     * Record rec = holder.orElseThrowIfNull("Record %s-%s in table %s not found", id1, id2, tableName);
     * }</pre>
     *
     * @param errorMessage the error message template which may contain placeholders for the parameters
     * @param param1 the first parameter to be substituted into the error message
     * @param param2 the second parameter to be substituted into the error message
     * @param param3 the third parameter to be substituted into the error message
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    @Beta
    public T orElseThrowIfNull(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}
     * with a formatted error message using the specified message template and parameters.
     *
     * <p>This is the varargs overload that accepts any number of parameters. Use this when you
     * need to format an error message with four or more parameters.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<Data> holder = Holder.of(null);
     * Data data = holder.orElseThrowIfNull("Data not found: %s, %s, %s, %s", p1, p2, p3, p4);
     * }</pre>
     *
     * @param errorMessage the error message template which may contain placeholders for the parameters
     * @param params the parameters to be substituted into the error message
     * @return the value held by this Holder
     * @throws NoSuchElementException if the value is {@code null}
     */
    @Beta
    public T orElseThrowIfNull(final String errorMessage, final Object... params) throws NoSuchElementException {
        if (isNotNull()) {
            return value;
        } else {
            throw new NoSuchElementException(N.format(errorMessage, params));
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws an exception provided by the given supplier.
     *
     * <p>This method provides maximum flexibility for exception handling. You can supply any type
     * of exception (checked or unchecked) with custom logic for creating the exception. The
     * supplier is only invoked when the value is {@code null}.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<User> holder = Holder.of(null);
     * User user = holder.orElseThrowIfNull(() -> new UserNotFoundException("User not in cache"));
     * }</pre>
     *
     * @param <E> the type of exception to be thrown
     * @param exceptionSupplier the supplier which will provide the exception to be thrown if the value is {@code null}, must not be {@code null}
     * @return the value held by this Holder
     * @throws IllegalArgumentException if the exception supplier is {@code null}
     * @throws E if the value is {@code null}
     */
    public <E extends Throwable> T orElseThrowIfNull(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
        N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

        if (isNotNull()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the hash code of the value held by this Holder.
     *
     * <p>The hash code is computed based on the held value. If the value is {@code null}, this
     * method returns 0. This implementation ensures that equal Holders (as determined by
     * {@link #equals(Object)}) have the same hash code.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("test");
     * int hash = holder.hashCode(); // returns "test".hashCode()
     * }</pre>
     *
     * @return the hash code of the value, or 0 if the value is {@code null}
     */
    @Override
    public int hashCode() {
        return (value == null) ? 0 : value.hashCode();
    }

    /**
     * Compares this Holder to the specified object for equality.
     *
     * <p>Two Holders are considered equal if and only if:
     * <ul>
     * <li>They are the same instance (identity equality), OR</li>
     * <li>The other object is also a Holder AND both Holders contain equal values (as determined by {@code Objects.equals()})</li>
     * </ul>
     *
     * <p>Note that two Holders containing {@code null} are considered equal.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> h1 = Holder.of("test");
     * Holder<String> h2 = Holder.of("test");
     * boolean areEqual = h1.equals(h2); // returns true
     * }</pre>
     *
     * @param obj the object to compare with this Holder for equality
     * @return {@code true} if the specified object is equal to this Holder, {@code false} otherwise
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Holder && N.equals(((Holder) obj).value, value));
    }

    /**
     * Returns a string representation of this Holder.
     *
     * <p>The string representation consists of "Holder[" followed by the string representation
     * of the value and "]". The value's string representation is obtained using {@code N.toString()},
     * which handles the conversion appropriately. If the value is {@code null}, returns "Holder[null]".
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Holder<String> holder = Holder.of("test");
     * String str = holder.toString(); // returns "Holder[test]"
     * }</pre>
     *
     * @return a string representation of this Holder in the format "Holder[value]"
     */
    @Override
    public String toString() {
        if (value == null) {
            return "Holder[null]";
        } else {
            return String.format("Holder[%s]", N.toString(value));
        }
    }
}
