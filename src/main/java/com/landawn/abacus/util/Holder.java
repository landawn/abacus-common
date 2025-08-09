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
     * This is equivalent to calling {@code Holder(null)}.
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
     * This is a factory method that provides a convenient way to create a Holder.
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
     * The value may be {@code null}.
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
     * The previous value is replaced and can be {@code null}.
     *
     * @param value the new value to be held by this Holder, may be {@code null}
     */
    public void setValue(final T value) {
        this.value = value;
    }

    /**
     * Atomically sets the value to the given updated value and returns the previous value.
     * This method is useful when you need to know the old value before replacing it.
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
     * This method is useful when you want to set a value and immediately use the same value.
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
     * returning the previous value. The function is applied with the current value as its argument.
     *
     * @param <E> the type of exception that the update function may throw
     * @param updateFunction a function that takes the current value and returns a new value
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
     * returning the updated value. The function is applied with the current value as its argument.
     *
     * @param <E> the type of exception that the update function may throw
     * @param updateFunction a function that takes the current value and returns a new value
     * @return the new value held by this Holder after the update, may be {@code null}
     * @throws E if the update function throws an exception
     */
    public <E extends Exception> T updateAndGet(final Throwables.UnaryOperator<T, E> updateFunction) throws E { // NOSONAR
        value = updateFunction.apply(value);
        return value;
    }

    /**
     * Sets the value to the specified new value if the given predicate returns {@code true}
     * when tested with the current value. If the predicate returns {@code false}, the value
     * remains unchanged.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newValue the new value to set if the predicate returns {@code true}, may be {@code null}
     * @param predicate the predicate that tests the current value
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
     * @return {@code true} if the value is {@code null}, {@code false} otherwise
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Checks whether the value held by this Holder is not {@code null}.
     *
     * @return {@code true} if the value is not {@code null}, {@code false} otherwise
     */
    public boolean isNotNull() {
        return value != null;
    }

    /**
     * Performs the given action with the value if the value is not {@code null}.
     * If the value is {@code null}, no action is performed.
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed with the non-null value
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
     * otherwise performs the given empty action. Exactly one of the two actions will be performed.
     *
     * @param <E> the type of exception that the action may throw
     * @param <E2> the type of exception that the empty action may throw
     * @param action the action to be performed with the non-null value
     * @param emptyAction the action to be performed when the value is {@code null}
     * @throws IllegalArgumentException if either action or emptyAction is {@code null}
     * @throws E if the action throws an exception
     * @throws E2 if the emptyAction throws an exception
     */
    public <E extends Exception, E2 extends Exception> void ifNotNullOrElse(final Throwables.Consumer<? super T, E> action,
            final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
        N.checkArgNotNull(action, cs.action);
        N.checkArgNotNull(emptyAction, cs.emptyAction);

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
        N.checkArgNotNull(action, cs.action);

        if (isNotNull()) {
            action.accept(value);
        }
    }

    /**
     * Applies the given mapping function to the value held by this Holder and returns the result.
     * The function is applied regardless of whether the value is {@code null} or not.
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the value
     * @return the result of applying the mapping function to the value
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
        return mapper.apply(value);
    }

    /**
     * Applies the given mapping function to the value if the value is not {@code null},
     * and returns a {@code Nullable} containing the mapped value. If the value is {@code null},
     * returns an empty {@code Nullable}.
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the non-null value
     * @return a {@code Nullable} containing the mapped value if the value was not {@code null}, otherwise an empty {@code Nullable}
     * @throws IllegalArgumentException if the mapper is {@code null}
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, cs.mapper);

        if (isNotNull()) {
            return Nullable.of(mapper.apply(value));
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Applies the given mapping function to the value if the value is not {@code null},
     * and returns an {@code Optional} containing the mapped value. The mapped value must not be {@code null}.
     * If the original value is {@code null}, returns an empty {@code Optional}.
     *
     * @param <U> the type of the result of the mapping function
     * @param <E> the type of exception that the mapping function may throw
     * @param mapper the mapping function to apply to the non-null value, must not return {@code null}
     * @return an {@code Optional} containing the mapped value if the value was not {@code null}, otherwise an empty {@code Optional}
     * @throws IllegalArgumentException if the mapper is {@code null}
     * @throws E if the mapping function throws an exception
     */
    public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, cs.mapper);

        if (isNotNull()) {
            return Optional.of(mapper.apply(value));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Tests the value with the given predicate and returns a {@code Nullable} containing the value
     * if the predicate returns {@code true}, otherwise returns an empty {@code Nullable}.
     * The predicate is applied regardless of whether the value is {@code null} or not.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test the value
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
     * If the value is {@code null} or the predicate returns {@code false}, returns an empty {@code Optional}.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test the non-null value
     * @return an {@code Optional} containing the value if it is not {@code null} and the predicate returns {@code true}, otherwise an empty {@code Optional}
     * @throws IllegalArgumentException if the predicate is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Optional<T> filterIfNotNull(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (isNotNull() && predicate.test(value)) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise returns the given default value.
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
     * @param other a {@code Supplier} whose result is returned if the held value is {@code null}
     * @return the value if not {@code null}, otherwise the result of {@code other.get()}
     * @throws IllegalArgumentException if the supplier is {@code null}
     */
    public T orElseGetIfNull(final Supplier<? extends T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        if (isNotNull()) {
            return value;
        } else {
            return other.get();
        }
    }

    /**
     * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}.
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
     * @param <E> the type of exception to be thrown
     * @param exceptionSupplier the supplier which will provide the exception to be thrown if the value is {@code null}
     * @return the value held by this Holder
     * @throws IllegalArgumentException if the exception supplier is {@code null}
     * @throws E if the value is {@code null}
     */
    public <E extends Throwable> T orElseThrowIfNull(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
        N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        if (isNotNull()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    // Simplify the apis.
    //    /**
    //     *
    //     * @return
    //     */
    //    public Stream<T> stream() {
    //        return Stream.of(value);
    //    }
    //
    //    /**
    //     * Returns a {@code Stream} with the {@code value} if {@code value} is not null, otherwise an empty {@code Stream} is returned.
    //     *
    //     * @return
    //     */
    //    public Stream<T> streamIfNotNull() {
    //        if (isNotNull()) {
    //            return Stream.of(value);
    //        } else {
    //            return Stream.<T> empty();
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @return
    //     */
    //    public List<T> toList() {
    //        return N.asList(value);
    //    }
    //
    //    /**
    //     * To list if not null.
    //     *
    //     * @return
    //     */
    //    public List<T> toListIfNotNull() {
    //        if (isNotNull()) {
    //            return N.asList(value);
    //        } else {
    //            return new ArrayList<>();
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @return
    //     */
    //    public Set<T> toSet() {
    //        return N.asSet(value);
    //    }
    //
    //    /**
    //     * To set if not null.
    //     *
    //     * @return
    //     */
    //    public Set<T> toSetIfNotNull() {
    //        if (isNotNull()) {
    //            return N.asSet(value);
    //        } else {
    //            return N.newHashSet();
    //        }
    //    }
    //
    //    /**
    //     * To immutable list.
    //     *
    //     * @return
    //     */
    //    public ImmutableList<T> toImmutableList() {
    //        return ImmutableList.of(value);
    //    }
    //
    //    /**
    //     * To immutable list if not null.
    //     *
    //     * @return
    //     */
    //    public ImmutableList<T> toImmutableListIfNotNull() {
    //        if (isNotNull()) {
    //            return ImmutableList.of(value);
    //        } else {
    //            return ImmutableList.empty();
    //        }
    //    }
    //
    //    /**
    //     * To immutable set.
    //     *
    //     * @return
    //     */
    //    public ImmutableSet<T> toImmutableSet() {
    //        return ImmutableSet.of(value);
    //    }
    //
    //    /**
    //     * To immutable set if not null.
    //     *
    //     * @return
    //     */
    //    public ImmutableSet<T> toImmutableSetIfNotNull() {
    //        if (isNotNull()) {
    //            return ImmutableSet.of(value);
    //        } else {
    //            return ImmutableSet.empty();
    //        }
    //    }
    //
    //    /**
    //     * Returns a non-empty {@code Nullable} with the {@code value}.
    //     *
    //     * @return
    //     */
    //    public Nullable<T> toNullable() {
    //        return Nullable.of(value);
    //    }
    //
    //    /**
    //     * Returns an {@code Optional} with the {@code value} if {@code value} is not null, otherwise an empty {@code Optional} is returned.
    //     *
    //     * @return
    //     */
    //    public Optional<T> toOptional() {
    //        return Optional.ofNullable(value);
    //    }

    /**
     * Returns the hash code of the value held by this Holder.
     * If the value is {@code null}, returns 0.
     *
     * @return the hash code of the value, or 0 if the value is {@code null}
     */
    @Override
    public int hashCode() {
        return (value == null) ? 0 : value.hashCode();
    }

    /**
     * Compares this Holder to the specified object for equality.
     * Two Holders are considered equal if they both contain the same value
     * (including {@code null} values).
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
     * The string representation consists of "Holder[" followed by the string representation
     * of the value and "]". If the value is {@code null}, returns "Holder[null]".
     *
     * @return a string representation of this Holder
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