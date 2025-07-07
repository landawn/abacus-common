/*
 * Copyright (c) 2019, Haiyang Li.
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ShortStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * A utility class containing various Optional and Nullable implementations for primitive types and objects.
 * This class provides container objects which may or may not contain a non-null value.
 */
public class u { // NOSONAR

    private static final String NO_VALUE_PRESENT = "No value present"; // should change it to InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX

    private u() {
        // utility class
    }

    /**
     * A container object which may or may not contain a {@code boolean} value.
     * If a value is present, {@code isPresent()} returns {@code true}. If no
     * value is present, the object is considered <i>empty</i> and
     * {@code isPresent()} returns {@code false}.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalBoolean implements Comparable<OptionalBoolean>, Immutable {

        /** Presents {@code true}. */
        public static final OptionalBoolean TRUE = new OptionalBoolean(true);

        /** Presents {@code true}. */
        public static final OptionalBoolean FALSE = new OptionalBoolean(false);

        private static final OptionalBoolean EMPTY = new OptionalBoolean();

        private final boolean value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional boolean.
         */
        private OptionalBoolean() {
            value = false;
            isPresent = false;
        }

        /**
         * Instantiates a new optional boolean.
         *
         * @param value
         */
        private OptionalBoolean(final boolean value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalBoolean} instance. No value is present for this OptionalBoolean.
         *
         * @return an empty {@code OptionalBoolean}
         */
        public static OptionalBoolean empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalBoolean} with the specified value present.
         *
         * @param value the value to describe
         * @return an {@code OptionalBoolean} with the value present
         */
        public static OptionalBoolean of(final boolean value) {
            return value ? TRUE : FALSE;
        }

        /**
         * Returns an {@code OptionalBoolean} describing the given value, if
         * non-null, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param val the possibly-null value to describe
         * @return an {@code OptionalBoolean} with a present value if the specified value
         *         is non-null, otherwise an empty {@code OptionalBoolean}
         */
        public static OptionalBoolean ofNullable(final Boolean val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        public boolean get() throws NoSuchElementException { // NOSONAR
            return orElseThrow();
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public boolean getAsBoolean() throws NoSuchElementException { // For AI
            return orElseThrow();
        }

        /**
         * If a value is present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * If a value is not present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is not present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed, if a value is present
         * @return this {@code OptionalBoolean}
         * @throws IllegalArgumentException if the action is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalBoolean ifPresent(final Throwables.BooleanConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed, if a value is present
         * @param emptyAction the empty-based action to be performed, if no value is present
         * @return this {@code OptionalBoolean}
         * @throws IllegalArgumentException if either action is null
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalBoolean ifPresentOrElse(final Throwables.BooleanConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate,
         * returns an {@code OptionalBoolean} describing the value, otherwise returns an
         * empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to a value, if present
         * @return an {@code OptionalBoolean} describing the value of this
         *         {@code OptionalBoolean}, if a value is present and the value matches the
         *         given predicate, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if the predicate is null
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> OptionalBoolean filter(final Throwables.BooleanPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalBoolean} describing (as if by
         * {@link #of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalBoolean} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean map(final Throwables.BooleanUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalChar} describing (as if by
         * {@link OptionalChar#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalChar} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalInt} describing (as if by
         * {@link OptionalInt#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalInt} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalLong} describing (as if by
         * {@link OptionalLong#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalLong}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalLong} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalLong}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalDouble} describing (as if by
         * {@link OptionalDouble#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalDouble} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} describing (as if by
         * {@link Optional#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code Optional} describing the result of applying a mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.BooleanFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, returns the result of applying the given
         * {@code OptionalBoolean}-bearing mapping function to the value, otherwise returns
         * an empty {@code OptionalBoolean}.
         *
         * <p>This method is similar to {@link #map(Throwables.BooleanUnaryOperator)}, but the mapping
         * function is one whose result is already an {@code OptionalBoolean}, and if
         * invoked, {@code flatMap} does not wrap it within an additional
         * {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return the result of applying an {@code OptionalBoolean}-bearing mapping
         *         function to the value of this {@code OptionalBoolean}, if a value is
         *         present, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean flatMap(final Throwables.BooleanFunction<OptionalBoolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final boolean valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Boolean valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.booleanValue());
        //        }

        /**
         * If a value is present, returns this {@code OptionalBoolean}, otherwise
         * returns the {@code OptionalBoolean} produced by the supplying function.
         *
         * @param supplier the supplying function that produces an {@code OptionalBoolean}
         *        to be returned
         * @return this {@code OptionalBoolean}, if a value is present, otherwise the
         *         {@code OptionalBoolean} produced by the supplying function
         */
        public OptionalBoolean or(final Supplier<OptionalBoolean> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElse(false)}
        //     */
        //    @Deprecated
        //    public boolean orFalse() {
        //        return isPresent ? value : false;
        //    }

        //    public boolean orElseFalse() {
        //        return isPresent ? value : false;
        //    }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElse(true)}
        //     */
        //    @Deprecated
        //    public boolean orTrue() {
        //        return isPresent ? value : true;
        //    }
        //
        //    public boolean orElseTrue() {
        //        return isPresent ? value : true;
        //    }

        /**
         * If a value is present, returns the value, otherwise returns {@code other}.
         *
         * @param other the value to be returned, if no value is present
         * @return the value, if present, otherwise {@code other}
         */
        public boolean orElse(final boolean other) {
            return isPresent ? value : other;
        }

        /**
         * If a value is present, returns the value, otherwise returns the result
         * produced by the supplying function.
         *
         * @param other a {@code BooleanSupplier} whose result is returned if no value
         *        is present
         * @return the value, if present, otherwise the result produced by the
         *         supplying function
         * @throws IllegalArgumentException if no value is present and the supplying
         *         function is null
         */
        public boolean orElseGet(final BooleanSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsBoolean();
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        public boolean orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with the given error message.
         *
         * @param errorMessage the message to be used in the exception, if no value is present
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public boolean orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameter.
         *
         * @param errorMessage the error message template, which can contain a placeholder for the parameter
         * @param param the parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public boolean orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param param1 the first parameter to be used in formatting the error message
         * @param param2 the second parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public boolean orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param param1 the first parameter to be used in formatting the error message
         * @param param2 the second parameter to be used in formatting the error message
         * @param param3 the third parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public boolean orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param params the parameters to be used in formatting the error message
         * @return the value described by this {@code OptionalBoolean}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public boolean orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws an exception
         * produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an
         *        exception to be thrown
         * @return the value, if present
         * @throws IllegalArgumentException if no value is present and the exception
         *         supplying function is null
         * @throws E if no value is present
         */
        public <E extends Throwable> boolean orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * If a value is present, returns a sequential {@link Stream} containing
         * only that value, otherwise returns an empty {@code Stream}.
         *
         * @return the optional value as a {@code Stream}
         */
        public Stream<Boolean> stream() {
            if (isPresent) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        /**
         * If a value is present, returns a {@code List} containing only
         * that value, otherwise returns an empty {@code List}.
         *
         * @return a {@code List} containing the value if present, otherwise an empty {@code List}
         */
        public List<Boolean> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * If a value is present, returns a {@code Set} containing only
         * that value, otherwise returns an empty {@code Set}.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty {@code Set}
         */
        public Set<Boolean> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * If a value is present, returns an {@code ImmutableList} containing only
         * that value, otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<Boolean> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * If a value is present, returns an {@code ImmutableSet} containing only
         * that value, otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<Boolean> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} containing the value,
         * otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional} containing the value if present, otherwise an empty {@code Optional}
         */
        public Optional<Boolean> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Compares this {@code OptionalBoolean} to another {@code OptionalBoolean}.
         * The comparison is first based on presence of values. An empty {@code OptionalBoolean}
         * is considered less than a non-empty one. If both are non-empty, the contained
         * values are compared using {@link Boolean#compare}.
         *
         * @param optional the {@code OptionalBoolean} to compare to
         * @return a negative integer, zero, or a positive integer as this
         *         {@code OptionalBoolean} is less than, equal to, or greater than the
         *         specified {@code OptionalBoolean}
         */
        @Override
        public int compareTo(final OptionalBoolean optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Boolean.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalBoolean}.
         * The other object is considered equal if:
         * <ul>
         *  <li>it is also an {@code OptionalBoolean} and;
         *  <li>both instances have no value present or;
         *  <li>the present values are equal via {@code ==}
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object
         *         otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalBoolean other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value, if present, otherwise {@code 0}
         * (zero) if no value is present.
         *
         * @return hash code value of the present value or {@code 0} if no value is present
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this {@code OptionalBoolean}
         * suitable for debugging. The exact presentation format is unspecified and
         * may vary between implementations and versions.
         *
         * <p>If a value is present the result must include its string representation
         * in the result. Empty and present {@code OptionalBoolean}s must be unambiguously
         * differentiable.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalBoolean[%s]", value);
            }

            return "OptionalBoolean.empty";
        }
    }

    /**
     * A container object which may or may not contain a {@code char} value.
     * If a value is present, {@code isPresent()} returns {@code true}. If no
     * value is present, the object is considered <i>empty</i> and
     * {@code isPresent()} returns {@code false}.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalChar implements Comparable<OptionalChar>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalChar EMPTY = new OptionalChar();

        /** The Constant MIN_CACHED_VALUE. */
        private static final char MIN_CACHED_VALUE = 0;

        /** The Constant MAX_CACHED_VALUE. */
        private static final char MAX_CACHED_VALUE = 128;

        /** The Constant cached. */
        private static final OptionalChar[] cached = new OptionalChar[MAX_CACHED_VALUE - MIN_CACHED_VALUE + 1];

        static {
            for (int i = 0; i < cached.length; i++) {
                cached[i] = new OptionalChar((char) (i + MIN_CACHED_VALUE));
            }
        }

        private final char value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional char.
         */
        private OptionalChar() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional char.
         *
         * @param value
         */
        private OptionalChar(final char value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalChar} instance. No value is present for this OptionalChar.
         *
         * @return an empty {@code OptionalChar}
         */
        public static OptionalChar empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalChar} with the specified value present.
         *
         * @param value the value to describe
         * @return an {@code OptionalChar} with the value present
         */
        public static OptionalChar of(final char value) {
            //noinspection ConstantValue
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalChar(value);
        }

        /**
         * Returns an {@code OptionalChar} describing the given value, if
         * non-null, otherwise returns an empty {@code OptionalChar}.
         *
         * @param val the possibly-null value to describe
         * @return an {@code OptionalChar} with a present value if the specified value
         *         is non-null, otherwise an empty {@code OptionalChar}
         */
        public static OptionalChar ofNullable(final Character val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        public char get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public char getAsChar() throws NoSuchElementException { // For AI
            return orElseThrow();
        }

        /**
         * If a value is present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * If a value is not present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is not present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed, if a value is present
         * @return this {@code OptionalChar}
         * @throws IllegalArgumentException if the action is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalChar ifPresent(final Throwables.CharConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed, if a value is present
         * @param emptyAction the empty-based action to be performed, if no value is present
         * @return this {@code OptionalChar}
         * @throws IllegalArgumentException if either action is null
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalChar ifPresentOrElse(final Throwables.CharConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate,
         * returns an {@code OptionalChar} describing the value, otherwise returns an
         * empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to a value, if present
         * @return an {@code OptionalChar} describing the value of this
         *         {@code OptionalChar}, if a value is present and the value matches the
         *         given predicate, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if the predicate is null
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> OptionalChar filter(final Throwables.CharPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalChar} describing (as if by
         * {@link #of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalChar} describing the result of applying a mapping
         *         function to the value of this {@code OptionalChar}, if a value is
         *         present, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar map(final Throwables.CharUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalBoolean} describing (as if by
         * {@link OptionalBoolean#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalBoolean} describing the result of applying a mapping
         *         function to the value of this {@code OptionalChar}, if a value is
         *         present, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<Character, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalInt} describing (as if by
         * {@link OptionalInt#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code OptionalInt} describing the result of applying a mapping
         *         function to the value of this {@code OptionalChar}, if a value is
         *         present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Character, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} describing (as if by
         * {@link Optional#of}) the result of applying the given mapping function to
         * the value, otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return an {@code Optional} describing the result of applying a mapping
         *         function to the value of this {@code OptionalChar}, if a value is
         *         present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.CharFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, returns the result of applying the given
         * {@code OptionalChar}-bearing mapping function to the value, otherwise returns
         * an empty {@code OptionalChar}.
         *
         * <p>This method is similar to {@link #map(Throwables.CharUnaryOperator)}, but the mapping
         * function is one whose result is already an {@code OptionalChar}, and if
         * invoked, {@code flatMap} does not wrap it within an additional
         * {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to a value, if present
         * @return the result of applying an {@code OptionalChar}-bearing mapping
         *         function to the value of this {@code OptionalChar}, if a value is
         *         present, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if the mapping function is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar flatMap(final Throwables.CharFunction<OptionalChar, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final char valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Character valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.charValue());
        //        }

        /**
         * If a value is present, returns this {@code OptionalChar}, otherwise
         * returns the {@code OptionalChar} produced by the supplying function.
         *
         * @param supplier the supplying function that produces an {@code OptionalChar}
         *        to be returned
         * @return this {@code OptionalChar}, if a value is present, otherwise the
         *         {@code OptionalChar} produced by the supplying function
         */
        public OptionalChar or(final Supplier<OptionalChar> supplier) {
            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public char orZero() {
        //        return isPresent() ? value : 0;
        //    }

        /**
         * If a value is present, returns the value, otherwise returns zero.
         *
         * @return the value, if present, otherwise zero
         */
        public char orElseZero() {
            return isPresent() ? value : 0;
        }

        /**
         * If a value is present, returns the value, otherwise returns {@code other}.
         *
         * @param other the value to be returned, if no value is present
         * @return the value, if present, otherwise {@code other}
         */
        public char orElse(final char other) {
            return isPresent() ? value : other;
        }

        /**
         * If a value is present, returns the value, otherwise returns the result
         * produced by the supplying function.
         *
         * @param other a {@code CharSupplier} whose result is returned if no value
         *        is present
         * @return the value, if present, otherwise the result produced by the
         *         supplying function
         * @throws IllegalArgumentException if no value is present and the supplying
         *         function is null
         */
        public char orElseGet(final CharSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent()) {
                return value;
            } else {
                return other.getAsChar();
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        public char orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with the given error message.
         *
         * @param errorMessage the message to be used in the exception, if no value is present
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public char orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameter.
         *
         * @param errorMessage the error message template, which can contain a placeholder for the parameter
         * @param param the parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public char orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param param1 the first parameter to be used in formatting the error message
         * @param param2 the second parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public char orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param param1 the first parameter to be used in formatting the error message
         * @param param2 the second parameter to be used in formatting the error message
         * @param param3 the third parameter to be used in formatting the error message
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public char orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException} with an error message formatted using the given message and parameters.
         *
         * @param errorMessage the error message template, which can contain placeholders for the parameters
         * @param params the parameters to be used in formatting the error message
         * @return the value described by this {@code OptionalChar}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public char orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * If a value is present, returns the value, otherwise throws an exception
         * produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an
         *        exception to be thrown
         * @return the value, if present
         * @throws IllegalArgumentException if no value is present and the exception
         *         supplying function is null
         * @throws E if no value is present
         */
        public <E extends Throwable> char orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * If a value is present, returns a sequential {@link CharStream} containing
         * only that value, otherwise returns an empty {@code CharStream}.
         *
         * @return the optional value as a {@code CharStream}
         */
        public CharStream stream() {
            if (isPresent) {
                return CharStream.of(value);
            } else {
                return CharStream.empty();
            }
        }

        /**
         * If a value is present, returns a {@code List} containing only
         * that value, otherwise returns an empty {@code List}.
         *
         * @return a {@code List} containing the value if present, otherwise an empty {@code List}
         */
        public List<Character> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * If a value is present, returns a {@code Set} containing only
         * that value, otherwise returns an empty {@code Set}.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty {@code Set}
         */
        public Set<Character> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * If a value is present, returns an {@code ImmutableList} containing only
         * that value, otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<Character> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * If a value is present, returns an {@code ImmutableSet} containing only
         * that value, otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<Character> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} containing the value,
         * otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional} containing the value if present, otherwise an empty {@code Optional}
         */
        public Optional<Character> boxed() {
            if (isPresent()) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Compares this {@code OptionalChar} to another {@code OptionalChar}.
         * The comparison is first based on presence of values. An empty {@code OptionalChar}
         * is considered less than a non-empty one. If both are non-empty, the contained
         * values are compared using {@link Character#compare}.
         *
         * @param optional the {@code OptionalChar} to compare to
         * @return a negative integer, zero, or a positive integer as this
         *         {@code OptionalChar} is less than, equal to, or greater than the
         *         specified {@code OptionalChar}
         */
        @Override
        public int compareTo(final OptionalChar optional) {
            if (optional == null || !optional.isPresent()) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Character.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalChar}.
         * The other object is considered equal if:
         * <ul>
         *  <li>it is also an {@code OptionalChar} and;
         *  <li>both instances have no value present or;
         *  <li>the present values are equal via {@code ==}
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object
         *         otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalChar other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value, if present, otherwise {@code 0}
         * (zero) if no value is present.
         *
         * @return hash code value of the present value or {@code 0} if no value is present
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this {@code OptionalChar}
         * suitable for debugging. The exact presentation format is unspecified and
         * may vary between implementations and versions.
         *
         * <p>If a value is present the result must include its string representation
         * in the result. Empty and present {@code OptionalChar}s must be unambiguously
         * differentiable.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            if (isPresent()) {
                return String.format("OptionalChar[%s]", value);
            }

            return "OptionalChar.empty";
        }
    }

    /**
     * A container object which may or may not contain a byte value.
     * If a value is present, {@code isPresent()} returns {@code true} and
     * {@code get()} returns the value.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalByte implements Comparable<OptionalByte>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalByte EMPTY = new OptionalByte();

        /** The Constant MIN_CACHED_VALUE. */
        private static final byte MIN_CACHED_VALUE = Byte.MIN_VALUE;

        /** The Constant MAX_CACHED_VALUE. */
        private static final byte MAX_CACHED_VALUE = Byte.MAX_VALUE;

        /** The Constant cached. */
        private static final OptionalByte[] cached = new OptionalByte[MAX_CACHED_VALUE - MIN_CACHED_VALUE + 1];

        static {
            for (int i = 0; i < cached.length; i++) {
                cached[i] = new OptionalByte((byte) (i + MIN_CACHED_VALUE));
            }
        }

        private final byte value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional byte.
         */
        private OptionalByte() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional byte.
         *
         * @param value
         */
        private OptionalByte(final byte value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalByte} instance. No value is present for this
         * {@code OptionalByte}.
         *
         * @return an empty {@code OptionalByte}
         */
        public static OptionalByte empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalByte} with the specified value present.
         *
         * @param value the byte value to be present
         * @return an {@code OptionalByte} with the value present
         */
        public static OptionalByte of(final byte value) {
            return cached[value - MIN_CACHED_VALUE];
        }

        /**
         * Returns an {@code OptionalByte} describing the specified value, if non-null,
         * otherwise returns an empty {@code OptionalByte}.
         *
         * @param val the possibly-null value to describe
         * @return an {@code OptionalByte} with a present value if the specified value
         *         is non-null, otherwise an empty {@code OptionalByte}
         */
        public static OptionalByte ofNullable(final Byte val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalByte.of(val);
            }
        }

        /**
         * Returns the byte value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the byte value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        public byte get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the byte value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the byte value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public byte getAsByte() throws NoSuchElementException { // For AI
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if no value is present, otherwise {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param <E> the type of exception the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code OptionalByte}
         * @throws IllegalArgumentException if {@code action} is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalByte ifPresent(final Throwables.ByteConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception the action may throw
         * @param <E2> the type of exception the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code OptionalByte}
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is null
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalByte ifPresentOrElse(final Throwables.ByteConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate,
         * returns an {@code OptionalByte} describing the value, otherwise returns an
         * empty {@code OptionalByte}.
         *
         * @param <E> the type of exception the predicate may throw
         * @param predicate the predicate to apply to the value, if present
         * @return an {@code OptionalByte} describing the value of this
         *         {@code OptionalByte} if a value is present and the value matches the
         *         given predicate, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code predicate} is null
         * @throws E if the predicate evaluation throws an exception
         */
        public <E extends Exception> OptionalByte filter(final Throwables.BytePredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, applies the provided mapping function to it,
         * and returns an {@code OptionalByte} describing the result.
         * Otherwise returns an empty {@code OptionalByte}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code OptionalByte} describing the result of applying the mapping
         *         function to the value of this {@code OptionalByte}, if a value is
         *         present, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalByte map(final Throwables.ByteUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code byte}-to-{@code int} mapping
         * function to it, and returns an {@code OptionalInt} describing the result.
         * Otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code OptionalInt} describing the result of applying the mapping
         *         function to the value of this {@code OptionalByte}, if a value is
         *         present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Byte, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code byte}-to-{@code T} mapping
         * function to it, and returns an {@code Optional} describing the result.
         * Otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the result of the mapping function
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code Optional} describing the result of applying the mapping
         *         function to the value of this {@code OptionalByte}, if a value is
         *         present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.ByteFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code OptionalByte}-bearing
         * mapping function to it, and returns the result. Otherwise returns an
         * empty {@code OptionalByte}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return the result of applying an {@code OptionalByte}-bearing mapping
         *         function to the value of this {@code OptionalByte}, if a value is
         *         present, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalByte flatMap(final Throwables.ByteFunction<OptionalByte, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final byte valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Byte valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.byteValue());
        //        }

        /**
         * If a value is not present, returns an {@code OptionalByte} produced by the
         * supplying function. Otherwise returns this {@code OptionalByte}.
         *
         * @param supplier the supplying function that produces an {@code OptionalByte}
         *        to be returned
         * @return this {@code OptionalByte} if a value is present, otherwise an
         *         {@code OptionalByte} produced by the supplying function
         */
        public OptionalByte or(final Supplier<OptionalByte> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public byte orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns zero.
         *
         * @return the value if present, otherwise {@code 0}
         */
        public byte orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns {@code other}.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public byte orElse(final byte other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the
         * supplying function.
         *
         * @param other a supplying function to be invoked to produce a value to be returned
         * @return the value if present, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is null
         */
        public byte orElseGet(final ByteSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsByte();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        public byte orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message.
         *
         * @param errorMessage the error message to use if no value is present
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public byte orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameter.
         *
         * @param errorMessage the error message format string
         * @param param the parameter to be substituted into the error message
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public byte orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter to be substituted into the error message
         * @param param2 the second parameter to be substituted into the error message
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public byte orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter to be substituted into the error message
         * @param param2 the second parameter to be substituted into the error message
         * @param param3 the third parameter to be substituted into the error message
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public byte orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param params the parameters to be substituted into the error message
         * @return the value held by this {@code OptionalByte}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public byte orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the
         * exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value held by this {@code OptionalByte}
         * @throws IllegalArgumentException if {@code exceptionSupplier} is null
         * @throws E if no value is present
         */
        public <E extends Throwable> byte orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a {@code ByteStream} containing only the value if present,
         * otherwise returns an empty {@code ByteStream}.
         *
         * @return the optional value as a {@code ByteStream}
         */
        public ByteStream stream() {
            if (isPresent) {
                return ByteStream.of(value);
            } else {
                return ByteStream.empty();
            }
        }

        /**
         * Returns a {@code List} containing only the value if present,
         * otherwise returns an empty {@code List}.
         *
         * @return a {@code List} containing the optional value if present, otherwise an empty list
         */
        public List<Byte> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing only the value if present,
         * otherwise returns an empty {@code Set}.
         *
         * @return a {@code Set} containing the optional value if present, otherwise an empty set
         */
        public Set<Byte> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing only the value if present,
         * otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the optional value if present, otherwise an empty immutable list
         */
        public ImmutableList<Byte> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing only the value if present,
         * otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the optional value if present, otherwise an empty immutable set
         */
        public ImmutableSet<Byte> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an {@code Optional} containing the value if present,
         * otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional} containing the optional value if present, otherwise an empty {@code Optional}
         */
        public Optional<Byte> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Compares this {@code OptionalByte} to the specified {@code OptionalByte}.
         * The comparison is first based on the presence of a value; an {@code OptionalByte}
         * with a value is considered greater than an empty {@code OptionalByte}.
         * If both are present, the comparison is based on the contained values.
         *
         * @param optional the {@code OptionalByte} to be compared
         * @return a negative integer, zero, or a positive integer as this
         *         {@code OptionalByte} is less than, equal to, or greater than the
         *         specified {@code OptionalByte}
         */
        @Override
        public int compareTo(final OptionalByte optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Byte.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalByte}.
         * The other object is considered equal if:
         * <ul>
         * <li>it is also an {@code OptionalByte} and;
         * <li>both instances have no value present or;
         * <li>the present values are equal
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalByte other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns {@code 0}
         * (zero) if no value is present.
         *
         * @return hash code value of the present value or {@code 0} if no value is present
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this {@code OptionalByte}
         * suitable for debugging. The exact presentation format is unspecified and
         * may vary between implementations and versions.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalByte[%s]", value);
            }

            return "OptionalByte.empty";
        }
    }

    /**
     * A container object which may or may not contain a short value.
     * If a value is present, {@code isPresent()} returns {@code true} and
     * {@code get()} returns the value.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalShort implements Comparable<OptionalShort>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalShort EMPTY = new OptionalShort();

        /** The Constant MIN_CACHED_VALUE. */
        private static final short MIN_CACHED_VALUE = -128;

        /** The Constant MAX_CACHED_VALUE. */
        private static final short MAX_CACHED_VALUE = 256;

        /** The Constant cached. */
        private static final OptionalShort[] cached = new OptionalShort[MAX_CACHED_VALUE - MIN_CACHED_VALUE + 1];

        static {
            for (int i = 0; i < cached.length; i++) {
                cached[i] = new OptionalShort((short) (i + MIN_CACHED_VALUE));
            }
        }

        private final short value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional short.
         */
        private OptionalShort() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional short.
         *
         * @param value
         */
        private OptionalShort(final short value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalShort} instance. No value is present for this
         * {@code OptionalShort}.
         *
         * @return an empty {@code OptionalShort}
         */
        public static OptionalShort empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalShort} with the specified value present.
         *
         * @param value the short value to be present
         * @return an {@code OptionalShort} with the value present
         */
        public static OptionalShort of(final short value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalShort(value);
        }

        /**
         * Returns an {@code OptionalShort} describing the specified value, if non-null,
         * otherwise returns an empty {@code OptionalShort}.
         *
         * @param val the possibly-null value to describe
         * @return an {@code OptionalShort} with a present value if the specified value
         *         is non-null, otherwise an empty {@code OptionalShort}
         */
        public static OptionalShort ofNullable(final Short val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalShort.of(val);
            }
        }

        /**
         * Returns the short value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the short value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        public short get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the short value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the short value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public short getAsShort() throws NoSuchElementException { // For AI
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if no value is present, otherwise {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param <E> the type of exception the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code OptionalShort}
         * @throws IllegalArgumentException if {@code action} is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalShort ifPresent(final Throwables.ShortConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception the action may throw
         * @param <E2> the type of exception the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code OptionalShort}
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is null
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalShort ifPresentOrElse(final Throwables.ShortConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate,
         * returns an {@code OptionalShort} describing the value, otherwise returns an
         * empty {@code OptionalShort}.
         *
         * @param <E> the type of exception the predicate may throw
         * @param predicate the predicate to apply to the value, if present
         * @return an {@code OptionalShort} describing the value of this
         *         {@code OptionalShort} if a value is present and the value matches the
         *         given predicate, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code predicate} is null
         * @throws E if the predicate evaluation throws an exception
         */
        public <E extends Exception> OptionalShort filter(final Throwables.ShortPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, applies the provided mapping function to it,
         * and returns an {@code OptionalShort} describing the result.
         * Otherwise returns an empty {@code OptionalShort}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code OptionalShort} describing the result of applying the mapping
         *         function to the value of this {@code OptionalShort}, if a value is
         *         present, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalShort map(final Throwables.ShortUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code short}-to-{@code int} mapping
         * function to it, and returns an {@code OptionalInt} describing the result.
         * Otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code OptionalInt} describing the result of applying the mapping
         *         function to the value of this {@code OptionalShort}, if a value is
         *         present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Short, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code short}-to-{@code T} mapping
         * function to it, and returns an {@code Optional} describing the result.
         * Otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the result of the mapping function
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return an {@code Optional} describing the result of applying the mapping
         *         function to the value of this {@code OptionalShort}, if a value is
         *         present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.ShortFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, applies the provided {@code OptionalShort}-bearing
         * mapping function to it, and returns the result. Otherwise returns an
         * empty {@code OptionalShort}.
         *
         * @param <E> the type of exception the mapping function may throw
         * @param mapper the mapping function to apply to the value, if present
         * @return the result of applying an {@code OptionalShort}-bearing mapping
         *         function to the value of this {@code OptionalShort}, if a value is
         *         present, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code mapper} is null
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalShort flatMap(final Throwables.ShortFunction<OptionalShort, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final short valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Short valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.shortValue());
        //        }

        /**
         * If a value is not present, returns an {@code OptionalShort} produced by the
         * supplying function. Otherwise returns this {@code OptionalShort}.
         *
         * @param supplier the supplying function that produces an {@code OptionalShort}
         *        to be returned
         * @return this {@code OptionalShort} if a value is present, otherwise an
         *         {@code OptionalShort} produced by the supplying function
         */
        public OptionalShort or(final Supplier<OptionalShort> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public short orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns zero.
         *
         * @return the value if present, otherwise {@code 0}
         */
        public short orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns {@code other}.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public short orElse(final short other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the
         * supplying function.
         *
         * @param other a supplying function to be invoked to produce a value to be returned
         * @return the value if present, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is null
         */
        public short orElseGet(final ShortSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsShort();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        public short orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message.
         *
         * @param errorMessage the error message to use if no value is present
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public short orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameter.
         *
         * @param errorMessage the error message format string
         * @param param the parameter to be substituted into the error message
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public short orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter to be substituted into the error message
         * @param param2 the second parameter to be substituted into the error message
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public short orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter to be substituted into the error message
         * @param param2 the second parameter to be substituted into the error message
         * @param param3 the third parameter to be substituted into the error message
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public short orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}
         * with the specified error message formatted with the provided parameters.
         *
         * @param errorMessage the error message format string
         * @param params the parameters to be substituted into the error message
         * @return the value held by this {@code OptionalShort}
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public short orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the
         * exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value held by this {@code OptionalShort}
         * @throws IllegalArgumentException if {@code exceptionSupplier} is null
         * @throws E if no value is present
         */
        public <E extends Throwable> short orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a {@code ShortStream} containing only the value if present,
         * otherwise returns an empty {@code ShortStream}.
         *
         * @return the optional value as a {@code ShortStream}
         */
        public ShortStream stream() {
            if (isPresent) {
                return ShortStream.of(value);
            } else {
                return ShortStream.empty();
            }
        }

        /**
         * Returns a {@code List} containing only the value if present,
         * otherwise returns an empty {@code List}.
         *
         * @return a {@code List} containing the optional value if present, otherwise an empty list
         */
        public List<Short> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing only the value if present,
         * otherwise returns an empty {@code Set}.
         *
         * @return a {@code Set} containing the optional value if present, otherwise an empty set
         */
        public Set<Short> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing only the value if present,
         * otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the optional value if present, otherwise an empty immutable list
         */
        public ImmutableList<Short> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing only the value if present,
         * otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the optional value if present, otherwise an empty immutable set
         */
        public ImmutableSet<Short> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an {@code Optional} containing the value if present,
         * otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional} containing the optional value if present, otherwise an empty {@code Optional}
         */
        public Optional<Short> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Compares this {@code OptionalShort} to the specified {@code OptionalShort}.
         * The comparison is first based on the presence of a value; an {@code OptionalShort}
         * with a value is considered greater than an empty {@code OptionalShort}.
         * If both are present, the comparison is based on the contained values.
         *
         * @param optional the {@code OptionalShort} to be compared
         * @return a negative integer, zero, or a positive integer as this
         *         {@code OptionalShort} is less than, equal to, or greater than the
         *         specified {@code OptionalShort}
         */
        @Override
        public int compareTo(final OptionalShort optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Short.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalShort}.
         * The other object is considered equal if:
         * <ul>
         * <li>it is also an {@code OptionalShort} and;
         * <li>both instances have no value present or;
         * <li>the present values are equal
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalShort other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns {@code 0}
         * (zero) if no value is present.
         *
         * @return hash code value of the present value or {@code 0} if no value is present
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this {@code OptionalShort}
         * suitable for debugging. The exact presentation format is unspecified and
         * may vary between implementations and versions.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalShort[%s]", value);
            }

            return "OptionalShort.empty";
        }
    }

    /**
     * A container object which may or may not contain an int value.
     * If a value is present, {@code isPresent()} returns {@code true} and
     * {@code get()} returns the value.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalInt implements Comparable<OptionalInt>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalInt EMPTY = new OptionalInt();

        /** The Constant MIN_CACHED_VALUE. */
        private static final int MIN_CACHED_VALUE = -256;

        /** The Constant MAX_CACHED_VALUE. */
        private static final int MAX_CACHED_VALUE = 1024;

        /** The Constant cached. */
        private static final OptionalInt[] cached = new OptionalInt[MAX_CACHED_VALUE - MIN_CACHED_VALUE + 1];

        static {
            for (int i = 0; i < cached.length; i++) {
                cached[i] = new OptionalInt(i + MIN_CACHED_VALUE);
            }
        }

        private final int value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional int.
         */
        private OptionalInt() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional int.
         *
         * @param value
         */
        private OptionalInt(final int value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalInt} instance. No value is present for this
         * {@code OptionalInt}.
         *
         * @return an empty {@code OptionalInt}
         */
        public static OptionalInt empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalInt} with the specified value present.
         *
         * @param value the int value to be present
         * @return an {@code OptionalInt} with the value present
         */
        public static OptionalInt of(final int value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalInt(value);
        }

        /**
         * Returns an {@code OptionalInt} describing the specified value, if non-null,
         * otherwise returns an empty {@code OptionalInt}.
         *
         * @param val the possibly-null value to describe
         * @return an {@code OptionalInt} with a present value if the specified value
         *         is non-null, otherwise an empty {@code OptionalInt}
         */
        public static OptionalInt ofNullable(final Integer val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalInt.of(val);
            }
        }

        /**
         * Returns an {@code OptionalInt} from the specified {@code java.util.OptionalInt}.
         * 
         * @param op the {@code java.util.OptionalInt} to convert
         * @return an {@code OptionalInt} with a present value if the specified
         *         {@code java.util.OptionalInt} is present, otherwise an empty {@code OptionalInt}
         */
        public static OptionalInt from(final java.util.OptionalInt op) {
            if (op.isPresent()) {
                return of(op.getAsInt());
            } else {
                return empty();
            }
        }

        /**
         * Returns the int value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the int value held by this {@code OptionalInt}
         * @throws NoSuchElementException if no value is present
         */
        public int get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the int value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the int value held by this {@code OptionalInt}
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public int getAsInt() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if no value is present, otherwise {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param <E> the type of exception the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code OptionalInt}
         * @throws IllegalArgumentException if {@code action} is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalInt ifPresent(final Throwables.IntConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception the action may throw
         * @param <E2> the type of exception the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code OptionalInt}
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is null
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalInt ifPresentOrElse(final Throwables.IntConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate,
         * returns an {@code OptionalInt} describing the value, otherwise returns an
         * empty {@code OptionalInt}.
         *
         * @param <E> the type of exception the predicate may throw
         * @param predicate the predicate to apply to the value, if present
         * @return an {@code OptionalInt} describing the value of this
         *         {@code OptionalInt} if a value is present and the value matches the
         *         given predicate, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code predicate} is null
         * @throws E if the predicate evaluation throws an exception
         */
        public <E extends Exception> OptionalInt filter(final Throwables.IntPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalInt containing the result.
         * If this OptionalInt is empty, returns an empty OptionalInt.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalInt containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalInt
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalInt map(final Throwables.IntUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalBoolean containing the result.
         * If this OptionalInt is empty, returns an empty OptionalBoolean.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalBoolean containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalBoolean
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalChar containing the result.
         * If this OptionalInt is empty, returns an empty OptionalChar.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalChar containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalChar
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalLong containing the result.
         * If this OptionalInt is empty, returns an empty OptionalLong.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalLong containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalLong
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalFloat containing the result.
         * If this OptionalInt is empty, returns an empty OptionalFloat.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalFloat containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalFloat
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalDouble containing the result.
         * If this OptionalInt is empty, returns an empty OptionalDouble.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalDouble containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalDouble
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an Optional containing the result.
         * If this OptionalInt is empty, returns an empty Optional.
         *
         * @param <T> the type of the result of the mapper function
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an Optional containing the result of applying the mapper to the value if present,
         *         otherwise an empty Optional
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.IntFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning the resulting OptionalInt.
         * If this OptionalInt is empty, returns an empty OptionalInt.
         * This method is similar to {@code map}, but the mapping function returns an OptionalInt, and if invoked,
         * flatMap does not wrap it within an additional OptionalInt.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return the result of applying an OptionalInt-bearing mapping function to the value of this OptionalInt,
         *         if a value is present, otherwise an empty OptionalInt
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalInt flatMap(final Throwables.IntFunction<OptionalInt, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final int valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Integer valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.intValue());
        //        }

        /**
         * Returns this OptionalInt if a value is present, otherwise returns the OptionalInt produced by the supplying function.
         *
         * @param supplier the supplying function that produces an OptionalInt to be returned
         * @return this OptionalInt if a value is present, otherwise the result of the supplying function
         */
        public OptionalInt or(final Supplier<OptionalInt> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public int orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns 0.
         *
         * @return the value if present, otherwise 0
         */
        public int orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns the given default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public int orElse(final int other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result of the supplying function.
         *
         * @param other the supplying function that produces a value to be returned
         * @return the value if present, otherwise the result of the supplying function
         * @throws IllegalArgumentException if other is null
         */
        public int orElseGet(final IntSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsInt();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public int orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with the given error message.
         *
         * @param errorMessage the error message to be used in the exception
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public int orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameter.
         *
         * @param errorMessage the error message format string
         * @param param the parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public int orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter for formatting the error message
         * @param param2 the second parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public int orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter for formatting the error message
         * @param param2 the second parameter for formatting the error message
         * @param param3 the third parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public int orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param params the parameters for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public int orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws IllegalArgumentException if exceptionSupplier is null
         * @throws E if no value is present
         */
        public <E extends Throwable> int orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns an IntStream containing the value if present, otherwise returns an empty IntStream.
         *
         * @return an IntStream containing the value if present, otherwise an empty IntStream
         */
        public IntStream stream() {
            if (isPresent) {
                return IntStream.of(value);
            } else {
                return IntStream.empty();
            }
        }

        /**
         * Returns a List containing the value if present, otherwise returns an empty List.
         *
         * @return a List containing the value if present, otherwise an empty List
         */
        public List<Integer> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a Set containing the value if present, otherwise returns an empty Set.
         *
         * @return a Set containing the value if present, otherwise an empty Set
         */
        public Set<Integer> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an ImmutableList containing the value if present, otherwise returns an empty ImmutableList.
         *
         * @return an ImmutableList containing the value if present, otherwise an empty ImmutableList
         */
        public ImmutableList<Integer> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an ImmutableSet containing the value if present, otherwise returns an empty ImmutableSet.
         *
         * @return an ImmutableSet containing the value if present, otherwise an empty ImmutableSet
         */
        public ImmutableSet<Integer> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an Optional containing the boxed Integer value if present, otherwise returns an empty Optional.
         *
         * @return an Optional containing the boxed Integer value if present, otherwise an empty Optional
         */
        public Optional<Integer> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Converts this OptionalInt to a java.util.OptionalInt.
         *
         * @return a java.util.OptionalInt containing the value if present, otherwise an empty java.util.OptionalInt
         */
        public java.util.OptionalInt toJdkOptional() {
            if (isPresent) {
                return java.util.OptionalInt.of(value);
            } else {
                return java.util.OptionalInt.empty();
            }
        }

        /**
         * Converts this OptionalInt to a java.util.OptionalInt.
         *
         * @return a java.util.OptionalInt containing the value if present, otherwise an empty java.util.OptionalInt
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public java.util.OptionalInt __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         * Compares this OptionalInt with another OptionalInt for ordering.
         * Empty OptionalInt instances are considered less than non-empty ones.
         * Two empty OptionalInt instances are considered equal.
         * Two non-empty OptionalInt instances are compared by their values.
         *
         * @param optional the OptionalInt to be compared
         * @return a negative integer, zero, or a positive integer as this OptionalInt is less than, equal to,
         *         or greater than the specified OptionalInt
         */
        @Override
        public int compareTo(final OptionalInt optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Integer.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this OptionalInt.
         * The other object is considered equal if:
         * <ul>
         * <li>it is also an OptionalInt and;
         * <li>both instances have no value present or;
         * <li>the present values are equal via {@code ==}.
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object, otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalInt other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns a hash code for empty.
         *
         * @return hash code value of the present value or a hash code for empty
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a string representation of this OptionalInt.
         * If a value is present, the result is "OptionalInt[" + value + "]".
         * If no value is present, the result is "OptionalInt.empty".
         *
         * @return a string representation of this OptionalInt
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalInt[%s]", value);
            }

            return "OptionalInt.empty";
        }
    }

    /**
     * A container object which may or may not contain a long value.
     * If a value is present, {@code isPresent()} returns {@code true} and
     * {@code get()} returns the value.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalLong implements Comparable<OptionalLong>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalLong EMPTY = new OptionalLong();

        /** The Constant MIN_CACHED_VALUE. */
        private static final long MIN_CACHED_VALUE = -256;

        /** The Constant MAX_CACHED_VALUE. */
        private static final long MAX_CACHED_VALUE = 1024;

        /** The Constant cached. */
        private static final OptionalLong[] cached = new OptionalLong[(int) (MAX_CACHED_VALUE - MIN_CACHED_VALUE + 1)];

        static {
            for (int i = 0; i < cached.length; i++) {
                cached[i] = new OptionalLong(i + MIN_CACHED_VALUE);
            }
        }

        private final long value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional long.
         */
        private OptionalLong() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional long.
         *
         * @param value
         */
        private OptionalLong(final long value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty OptionalLong instance. No value is present for this OptionalLong.
         *
         * @return an empty OptionalLong
         */
        public static OptionalLong empty() {
            return EMPTY;
        }

        /**
         * Returns an OptionalLong with the specified value present.
         * For values between -256 and 1024 inclusive, cached instances are returned.
         *
         * @param value the value to be present
         * @return an OptionalLong with the value present
         */
        public static OptionalLong of(final long value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[(int) (value - MIN_CACHED_VALUE)] : new OptionalLong(value);
        }

        /**
         * Returns an OptionalLong describing the given value, if non-null, otherwise returns an empty OptionalLong.
         *
         * @param val the possibly-null value
         * @return an OptionalLong with a present value if the specified value is non-null, otherwise an empty OptionalLong
         */
        public static OptionalLong ofNullable(final Long val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalLong.of(val);
            }
        }

        /**
         * Returns an OptionalLong with the value from the specified java.util.OptionalLong if present,
         * otherwise returns an empty OptionalLong.
         *
         * @param op the java.util.OptionalLong to convert
         * @return an OptionalLong with the value if present, otherwise an empty OptionalLong
         */
        public static OptionalLong from(final java.util.OptionalLong op) {
            if (op.isPresent()) {
                return of(op.getAsLong());
            } else {
                return empty();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public long get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public long getAsLong() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if a value is not present, otherwise {@code false}.
         *
         * @return {@code true} if a value is not present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a value is present
         * @return this OptionalLong
         * @throws IllegalArgumentException if action is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalLong ifPresent(final Throwables.LongConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the emptyAction may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this OptionalLong
         * @throws IllegalArgumentException if action or emptyAction is null
         * @throws E if the action throws an exception
         * @throws E2 if the emptyAction throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalLong ifPresentOrElse(final Throwables.LongConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate, returns an OptionalLong describing the value,
         * otherwise returns an empty OptionalLong.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if present
         * @return an OptionalLong describing the value of this OptionalLong if a value is present and matches the predicate,
         *         otherwise an empty OptionalLong
         * @throws IllegalArgumentException if predicate is null
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> OptionalLong filter(final Throwables.LongPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalLong containing the result.
         * If this OptionalLong is empty, returns an empty OptionalLong.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalLong containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalLong
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalLong map(final Throwables.LongUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalInt containing the result.
         * If this OptionalLong is empty, returns an empty OptionalInt.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalInt containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalInt
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Long, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalDouble containing the result.
         * If this OptionalLong is empty, returns an empty OptionalDouble.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalDouble containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalDouble
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Long, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an Optional containing the result.
         * If this OptionalLong is empty, returns an empty Optional.
         *
         * @param <T> the type of the result of the mapper function
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an Optional containing the result of applying the mapper to the value if present,
         *         otherwise an empty Optional
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.LongFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning the resulting OptionalLong.
         * If this OptionalLong is empty, returns an empty OptionalLong.
         * This method is similar to {@code map}, but the mapping function returns an OptionalLong, and if invoked,
         * flatMap does not wrap it within an additional OptionalLong.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return the result of applying an OptionalLong-bearing mapping function to the value of this OptionalLong,
         *         if a value is present, otherwise an empty OptionalLong
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalLong flatMap(final Throwables.LongFunction<OptionalLong, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final long valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Long valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.longValue());
        //        }

        /**
         * Returns this OptionalLong if a value is present, otherwise returns the OptionalLong produced by the supplying function.
         *
         * @param supplier the supplying function that produces an OptionalLong to be returned
         * @return this OptionalLong if a value is present, otherwise the result of the supplying function
         */
        public OptionalLong or(final Supplier<OptionalLong> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public long orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns 0.
         *
         * @return the value if present, otherwise 0
         */
        public long orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns the given default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public long orElse(final long other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result of the supplying function.
         *
         * @param other the supplying function that produces a value to be returned
         * @return the value if present, otherwise the result of the supplying function
         * @throws IllegalArgumentException if other is null
         */
        public long orElseGet(final LongSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsLong();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public long orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with the given error message.
         *
         * @param errorMessage the error message to be used in the exception
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public long orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameter.
         *
         * @param errorMessage the error message format string
         * @param param the parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public long orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter for formatting the error message
         * @param param2 the second parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public long orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param param1 the first parameter for formatting the error message
         * @param param2 the second parameter for formatting the error message
         * @param param3 the third parameter for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public long orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with an error message formatted with the given parameters.
         *
         * @param errorMessage the error message format string
         * @param params the parameters for formatting the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public long orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws IllegalArgumentException if exceptionSupplier is null
         * @throws E if no value is present
         */
        public <E extends Throwable> long orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a LongStream containing the value if present, otherwise returns an empty LongStream.
         *
         * @return a LongStream containing the value if present, otherwise an empty LongStream
         */
        public LongStream stream() {
            if (isPresent) {
                return LongStream.of(value);
            } else {
                return LongStream.empty();
            }
        }

        /**
         * Returns a List containing the value if present, otherwise returns an empty List.
         *
         * @return a List containing the value if present, otherwise an empty List
         */
        public List<Long> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a Set containing the value if present, otherwise returns an empty Set.
         *
         * @return a Set containing the value if present, otherwise an empty Set
         */
        public Set<Long> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an ImmutableList containing the value if present, otherwise returns an empty ImmutableList.
         *
         * @return an ImmutableList containing the value if present, otherwise an empty ImmutableList
         */
        public ImmutableList<Long> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an ImmutableSet containing the value if present, otherwise returns an empty ImmutableSet.
         *
         * @return an ImmutableSet containing the value if present, otherwise an empty ImmutableSet
         */
        public ImmutableSet<Long> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an Optional containing the boxed Long value if present, otherwise returns an empty Optional.
         *
         * @return an Optional containing the boxed Long value if present, otherwise an empty Optional
         */
        public Optional<Long> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Converts this OptionalLong to a java.util.OptionalLong.
         *
         * @return a java.util.OptionalLong containing the value if present, otherwise an empty java.util.OptionalLong
         */
        public java.util.OptionalLong toJdkOptional() {
            if (isPresent) {
                return java.util.OptionalLong.of(value);
            } else {
                return java.util.OptionalLong.empty();
            }
        }

        /**
         * Converts this OptionalLong to a java.util.OptionalLong.
         *
         * @return a java.util.OptionalLong containing the value if present, otherwise an empty java.util.OptionalLong
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public java.util.OptionalLong __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         * Compares this OptionalLong with another OptionalLong for ordering.
         * Empty OptionalLong instances are considered less than non-empty ones.
         * Two empty OptionalLong instances are considered equal.
         * Two non-empty OptionalLong instances are compared by their values.
         *
         * @param optional the OptionalLong to be compared
         * @return a negative integer, zero, or a positive integer as this OptionalLong is less than, equal to,
         *         or greater than the specified OptionalLong
         */
        @Override
        public int compareTo(final OptionalLong optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Long.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this OptionalLong.
         * The other object is considered equal if:
         * <ul>
         * <li>it is also an OptionalLong and;
         * <li>both instances have no value present or;
         * <li>the present values are equal via {@code ==}.
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object, otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalLong other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns a hash code for empty.
         *
         * @return hash code value of the present value or a hash code for empty
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a string representation of this OptionalLong.
         * If a value is present, the result is "OptionalLong[" + value + "]".
         * If no value is present, the result is "OptionalLong.empty".
         *
         * @return a string representation of this OptionalLong
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalLong[%s]", value);
            }

            return "OptionalLong.empty";
        }
    }

    /**
     * A container object which may or may not contain a float value.
     * If a value is present, {@code isPresent()} returns {@code true} and
     * {@code get()} returns the value.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalFloat implements Comparable<OptionalFloat>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalFloat EMPTY = new OptionalFloat();

        private static final OptionalFloat ZERO = new OptionalFloat(0f);

        private final float value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional float.
         */
        private OptionalFloat() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional float.
         *
         * @param value
         */
        private OptionalFloat(final float value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty OptionalFloat instance. No value is present for this OptionalFloat.
         *
         * @return an empty OptionalFloat
         */
        public static OptionalFloat empty() {
            return EMPTY;
        }

        /**
         * Returns an OptionalFloat with the specified value present.
         * For value 0.0f, a cached instance is returned.
         *
         * @param value the value to be present
         * @return an OptionalFloat with the value present
         */
        public static OptionalFloat of(final float value) {
            return value == 0f ? ZERO : new OptionalFloat(value);
        }

        /**
         * Returns an OptionalFloat describing the given value, if non-null, otherwise returns an empty OptionalFloat.
         *
         * @param val the possibly-null value
         * @return an OptionalFloat with a present value if the specified value is non-null, otherwise an empty OptionalFloat
         */
        public static OptionalFloat ofNullable(final Float val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalFloat.of(val);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public float get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public float getAsFloat() throws NoSuchElementException { // For AI
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if a value is not present, otherwise {@code false}.
         *
         * @return {@code true} if a value is not present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a value is present
         * @return this OptionalFloat
         * @throws IllegalArgumentException if action is null
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalFloat ifPresent(final Throwables.FloatConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the emptyAction may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this OptionalFloat
         * @throws IllegalArgumentException if action or emptyAction is null
         * @throws E if the action throws an exception
         * @throws E2 if the emptyAction throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalFloat ifPresentOrElse(final Throwables.FloatConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate, returns an OptionalFloat describing the value,
         * otherwise returns an empty OptionalFloat.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if present
         * @return an OptionalFloat describing the value of this OptionalFloat if a value is present and matches the predicate,
         *         otherwise an empty OptionalFloat
         * @throws IllegalArgumentException if predicate is null
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> OptionalFloat filter(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalFloat containing the result.
         * If this OptionalFloat is empty, returns an empty OptionalFloat.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalFloat containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalFloat
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalFloat map(final Throwables.FloatUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalInt containing the result.
         * If this OptionalFloat is empty, returns an empty OptionalInt.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalInt containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalInt
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Float, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an OptionalDouble containing the result.
         * If this OptionalFloat is empty, returns an empty OptionalDouble.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an OptionalDouble containing the result of applying the mapper to the value if present,
         *         otherwise an empty OptionalDouble
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Float, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning an Optional containing the result.
         * If this OptionalFloat is empty, returns an empty Optional.
         *
         * @param <T> the type of the result of the mapper function
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return an Optional containing the result of applying the mapper to the value if present,
         *         otherwise an empty Optional
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.FloatFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * Applies the given mapper function to the value if present, returning the resulting OptionalFloat.
         * If this OptionalFloat is empty, returns an empty OptionalFloat.
         * This method is similar to {@code map}, but the mapping function returns an OptionalFloat, and if invoked,
         * flatMap does not wrap it within an additional OptionalFloat.
         *
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the mapper function to apply to the value if present
         * @return the result of applying an OptionalFloat-bearing mapping function to the value of this OptionalFloat,
         *         if a value is present, otherwise an empty OptionalFloat
         * @throws IllegalArgumentException if mapper is null
         * @throws E if the mapper function throws an exception
         */
        public <E extends Exception> OptionalFloat flatMap(final Throwables.FloatFunction<OptionalFloat, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final float valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Float valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.floatValue());
        //        }

        /**
         * Returns this OptionalFloat if a value is present, otherwise returns the OptionalFloat produced by the supplying function.
         *
         * @param supplier the supplying function that produces an OptionalFloat to be returned
         * @return this OptionalFloat if a value is present, otherwise the result of the supplying function
         */
        public OptionalFloat or(final Supplier<OptionalFloat> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public float orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns 0.
         *
         * @return the value if present, otherwise 0
         */
        public float orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns the given default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public float orElse(final float other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the supplying function.
         *
         * @param other a {@code FloatSupplier} whose result is returned if no value is present
         * @return the value if present, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is {@code null}
         */
        public float orElseGet(final FloatSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsFloat();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public float orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with the specified error message.
         *
         * @param errorMessage the error message to use if no value is present
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the specified error message
         */
        @Beta
        public float orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param the parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public float orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to be substituted into the error message template
         * @param param2 the second parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public float orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to be substituted into the error message template
         * @param param2 the second parameter to be substituted into the error message template
         * @param param3 the third parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public float orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param params the parameters to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public float orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}
         * @throws E if no value is present
         */
        public <E extends Throwable> float orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a {@code FloatStream} containing the value if present, otherwise returns an empty stream.
         *
         * @return a {@code FloatStream} containing the value if present, otherwise an empty stream
         */
        public FloatStream stream() {
            if (isPresent) {
                return FloatStream.of(value);
            } else {
                return FloatStream.empty();
            }
        }

        /**
         * Returns a {@code List} containing the value if present, otherwise returns an empty list.
         *
         * @return a {@code List} containing the value if present, otherwise an empty list
         */
        public List<Float> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing the value if present, otherwise returns an empty set.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty set
         */
        public Set<Float> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing the value if present, otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<Float> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing the value if present, otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<Float> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an {@code Optional<Float>} containing the boxed value if present, otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional<Float>} containing the boxed value if present, otherwise an empty {@code Optional}
         */
        public Optional<Float> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Compares this {@code OptionalFloat} with the specified {@code OptionalFloat} for order.
         * Empty {@code OptionalFloat}s are considered less than non-empty ones.
         * If both are non-empty, their values are compared using {@code Float.compare}.
         *
         * @param optional the {@code OptionalFloat} to be compared
         * @return a negative integer, zero, or a positive integer as this {@code OptionalFloat}
         *         is less than, equal to, or greater than the specified {@code OptionalFloat}
         */
        @Override
        public int compareTo(final OptionalFloat optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Float.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalFloat}.
         * The other object is considered equal if:
         * <ul>
         *   <li>it is also an {@code OptionalFloat} and;
         *   <li>both instances have no value present or;
         *   <li>the present values are "equal to" each other via {@code N.equals()}.
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object, otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalFloat other) {
                return (isPresent && other.isPresent) ? N.equals(value, other.value) : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns {@code 0} (zero).
         *
         * @return the hash code of the value if present, otherwise {@code 0}
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a string representation of this {@code OptionalFloat}.
         * If a value is present, the string representation is "OptionalFloat[" followed by the value and "]".
         * If no value is present, the string representation is "OptionalFloat.empty".
         *
         * @return a string representation of this {@code OptionalFloat}
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalFloat[%s]", value);
            }

            return "OptionalFloat.empty";
        }
    }

    /**
     * A container object which may or may not contain a double value.
     * If a value is present, {@code isPresent()} returns {@code true}.
     * If no value is present, the object is considered empty and {@code isPresent()} returns {@code false}.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalDouble implements Comparable<OptionalDouble>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalDouble EMPTY = new OptionalDouble();

        private static final OptionalDouble ZERO = new OptionalDouble(0d);

        private final double value;

        private final boolean isPresent;

        /**
         * Instantiates a new optional double.
         */
        private OptionalDouble() {
            value = 0;
            isPresent = false;
        }

        /**
         * Instantiates a new optional double.
         *
         * @param value
         */
        private OptionalDouble(final double value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code OptionalDouble} instance. No value is present for this {@code OptionalDouble}.
         *
         * @return an empty {@code OptionalDouble}
         */
        public static OptionalDouble empty() {
            return EMPTY;
        }

        /**
         * Returns an {@code OptionalDouble} containing the specified value.
         *
         * @param value the value to store
         * @return an {@code OptionalDouble} containing the specified value
         */
        public static OptionalDouble of(final double value) {
            return value == 0d ? ZERO : new OptionalDouble(value);
        }

        /**
         * Returns an {@code OptionalDouble} containing the specified {@code Double} value, or an empty {@code OptionalDouble} if the value is {@code null}.
         *
         * @param val the {@code Double} value to store, possibly {@code null}
         * @return an {@code OptionalDouble} containing the specified value if non-null, otherwise an empty {@code OptionalDouble}
         */
        public static OptionalDouble ofNullable(final Double val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalDouble.of(val);
            }
        }

        /**
         * Returns an {@code OptionalDouble} containing the value from the specified {@code java.util.OptionalDouble} if present, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param op the {@code java.util.OptionalDouble} to convert
         * @return an {@code OptionalDouble} containing the value from the specified {@code java.util.OptionalDouble} if present, otherwise an empty {@code OptionalDouble}
         */
        public static OptionalDouble from(final java.util.OptionalDouble op) {
            if (op.isPresent()) {
                return of(op.getAsDouble());
            } else {
                return empty();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public double get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         * @deprecated This method is deprecated in favor of the more concise {@link #get()} method.
         * @see #get()
         */
        @Deprecated
        public double getAsDouble() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if no value is present, otherwise {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code action} is {@code null}
         * @throws E if the action throws an exception
         */
        public <E extends Exception> OptionalDouble ifPresent(final Throwables.DoubleConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is {@code null}
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> OptionalDouble ifPresentOrElse(final Throwables.DoubleConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate, returns an {@code OptionalDouble} describing the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if present
         * @return an {@code OptionalDouble} describing the value if present and matching the predicate, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code predicate} is {@code null}
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> OptionalDouble filter(final Throwables.DoublePredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalDouble} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalDouble} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble map(final Throwables.DoubleUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalInt} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalInt} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Double, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalLong} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalLong}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalLong} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalLong}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Double, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code Optional} describing the result of applying the mapping function to the value if present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <T, E extends Exception> Optional<T> mapToObj(final Throwables.DoubleFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, returns the result of applying the given {@code OptionalDouble}-bearing mapping function to the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble flatMap(final Throwables.DoubleFunction<OptionalDouble, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final double valueToFind) {
        //            return isPresent() && N.equals(this.value, valueToFind);
        //        }
        //
        //        /**
        //         *
        //         *
        //         * @param element
        //         * @return
        //         */
        //        public boolean contains(final Double valueToFind) {
        //            return valueToFind != null && isPresent() && N.equals(this.value, valueToFind.doubleValue());
        //        }

        /**
         * If a value is present, returns this {@code OptionalDouble}, otherwise returns the {@code OptionalDouble} produced by the supplying function.
         *
         * @param supplier the supplying function that produces an {@code OptionalDouble} to be returned
         * @return this {@code OptionalDouble} if a value is present, otherwise the {@code OptionalDouble} produced by the supplying function
         */
        public OptionalDouble or(final Supplier<OptionalDouble> supplier) {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated use {@link #orElseZero()}
        //     */
        //    @Deprecated
        //    public double orZero() {
        //        return isPresent ? value : 0;
        //    }

        /**
         * Returns the value if present, otherwise returns {@code 0}.
         *
         * @return the value if present, otherwise {@code 0}
         */
        public double orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         * Returns the value if present, otherwise returns the specified default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public double orElse(final double other) {
            return isPresent ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the supplying function.
         *
         * @param other a {@code DoubleSupplier} whose result is returned if no value is present
         * @return the value if present, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is {@code null}
         */
        public double orElseGet(final DoubleSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent) {
                return value;
            } else {
                return other.getAsDouble();
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public double orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with the specified error message.
         *
         * @param errorMessage the error message to use if no value is present
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the specified error message
         */
        @Beta
        public double orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param the parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public double orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to be substituted into the error message template
         * @param param2 the second parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public double orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to be substituted into the error message template
         * @param param2 the second parameter to be substituted into the error message template
         * @param param3 the third parameter to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public double orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException} with a formatted error message.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param params the parameters to be substituted into the error message template
         * @return the value if present
         * @throws NoSuchElementException if no value is present, with the formatted error message
         */
        @Beta
        public double orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of the exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}
         * @throws E if no value is present
         */
        public <E extends Throwable> double orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a {@code DoubleStream} containing the value if present, otherwise returns an empty stream.
         *
         * @return a {@code DoubleStream} containing the value if present, otherwise an empty stream
         */
        public DoubleStream stream() {
            if (isPresent) {
                return DoubleStream.of(value);
            } else {
                return DoubleStream.empty();
            }
        }

        /**
         * Returns a {@code List} containing the value if present, otherwise returns an empty list.
         *
         * @return a {@code List} containing the value if present, otherwise an empty list
         */
        public List<Double> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing the value if present, otherwise returns an empty set.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty set
         */
        public Set<Double> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing the value if present, otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<Double> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing the value if present, otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<Double> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an {@code Optional<Double>} containing the boxed value if present, otherwise returns an empty {@code Optional}.
         *
         * @return an {@code Optional<Double>} containing the boxed value if present, otherwise an empty {@code Optional}
         */
        public Optional<Double> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Converts this {@code OptionalDouble} to a {@code java.util.OptionalDouble}.
         *
         * @return a {@code java.util.OptionalDouble} containing the value if present, otherwise an empty {@code java.util.OptionalDouble}
         */
        public java.util.OptionalDouble toJdkOptional() {
            if (isPresent) {
                return java.util.OptionalDouble.of(value);
            } else {
                return java.util.OptionalDouble.empty();
            }
        }

        /**
         *
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public java.util.OptionalDouble __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         * Compares this {@code OptionalDouble} with the specified {@code OptionalDouble} for order.
         * Empty {@code OptionalDouble}s are considered less than non-empty ones.
         * If both are non-empty, their values are compared using {@code Double.compare}.
         *
         * @param optional the {@code OptionalDouble} to be compared
         * @return a negative integer, zero, or a positive integer as this {@code OptionalDouble}
         *         is less than, equal to, or greater than the specified {@code OptionalDouble}
         */
        @Override
        public int compareTo(final OptionalDouble optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Double.compare(get(), optional.get());
        }

        /**
         * Indicates whether some other object is "equal to" this {@code OptionalDouble}.
         * The other object is considered equal if:
         * <ul>
         *   <li>it is also an {@code OptionalDouble} and;
         *   <li>both instances have no value present or;
         *   <li>the present values are "equal to" each other via {@code N.equals()}.
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object, otherwise {@code false}
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalDouble other) {
                return (isPresent && other.isPresent) ? N.equals(value, other.value) : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         * Returns the hash code of the value if present, otherwise returns {@code 0} (zero).
         *
         * @return the hash code of the value if present, otherwise {@code 0}
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a string representation of this {@code OptionalDouble}.
         * If a value is present, the string representation is "OptionalDouble[" followed by the value and "]".
         * If no value is present, the string representation is "OptionalDouble.empty".
         *
         * @return a string representation of this {@code OptionalDouble}
         */
        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalDouble[%s]", value);
            }

            return "OptionalDouble.empty";
        }
    }

    /**
     * A container object which may or may not contain a non-null value.
     * If a value is present, {@code isPresent()} returns {@code true}.
     * If no value is present, the object is considered empty and {@code isPresent()} returns {@code false}.
     *
     * @param <T> the type of value
     * @see com.landawn.abacus.util.u.Nullable
     * @see com.landawn.abacus.util.Holder
     * @see com.landawn.abacus.util.Result
     * @see com.landawn.abacus.util.Pair
     * @see com.landawn.abacus.util.Triple
     * @see com.landawn.abacus.util.Tuple
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class Optional<T> implements Immutable {

        /** Presents {@code Boolean.TRUE}. */
        public static final Optional<Boolean> TRUE = new Optional<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Optional<Boolean> FALSE = new Optional<>(Boolean.FALSE);

        private static final Optional<String> EMPTY_STRING = new Optional<>(Strings.EMPTY);

        /** The Constant EMPTY. */
        private static final Optional<?> EMPTY = new Optional<>();

        private final T value;

        /**
         * Instantiates a new optional.
         */
        private Optional() {
            value = null;
        }

        /**
         * Instantiates a new optional.
         *
         * @param value
         */
        private Optional(final T value) throws NullPointerException {
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Returns an empty {@code Optional} instance. No value is present for this {@code Optional}.
         *
         * @param <T> the type of the non-existent value
         * @return an empty {@code Optional}
         */
        public static <T> Optional<T> empty() {
            return (Optional<T>) EMPTY;
        }

        /**
         * Returns an {@code Optional} containing the specified non-null value.
         * Special handling for empty strings: returns a cached instance for empty strings.
         *
         * @param value the non-null value to store
         * @return an {@code Optional} containing the specified value
         */
        public static Optional<String> of(final String value) throws NullPointerException {
            Objects.requireNonNull(value);

            if (value.isEmpty()) {
                return EMPTY_STRING;
            }

            return new Optional<>(value);
        }

        /**
         * Returns an {@code Optional} containing the specified non-null value.
         *
         * @param <T> the type of the value
         * @param value the non-null value to store
         * @return an {@code Optional} containing the specified value
         */
        public static <T> Optional<T> of(final T value) throws NullPointerException {
            return new Optional<>(value);
        }

        /**
         * Returns an {@code Optional} containing the specified {@code String} value if non-null, otherwise returns an empty {@code Optional}.
         * Special handling for empty strings: returns a cached instance for empty strings.
         *
         * @param value the possibly-null value to store
         * @return an {@code Optional} containing the specified value if non-null, otherwise an empty {@code Optional}
         */
        public static Optional<String> ofNullable(final String value) {
            if (value == null) {
                return empty();
            } else if (value.isEmpty()) {
                return EMPTY_STRING;
            }

            return new Optional<>(value);
        }

        /**
         * Returns an {@code Optional} containing the specified value if non-null, otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the value
         * @param value the possibly-null value to store
         * @return an {@code Optional} containing the specified value if non-null, otherwise an empty {@code Optional}
         */
        public static <T> Optional<T> ofNullable(final T value) {
            if (value == null) {
                return empty();
            }

            return new Optional<>(value);
        }

        /**
         * Returns an {@code Optional} containing the value from the specified {@code java.util.Optional} if present, otherwise returns an empty {@code Optional}.
         *
         * @param <T> the type of the value
         * @param op the {@code java.util.Optional} to convert, possibly {@code null}
         * @return an {@code Optional} containing the value from the specified {@code java.util.Optional} if present, otherwise an empty {@code Optional}
         */
        public static <T> Optional<T> from(final java.util.Optional<T> op) {
            if (op == null || op.isEmpty()) {
                return empty();
            } else {
                return of(op.get());
            }
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value is present, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return value != null;
        }

        /**
         * Returns {@code true} if no value is present, otherwise {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return value == null;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code Optional}
         * @throws IllegalArgumentException if {@code action} is {@code null}
         * @throws E if the action throws an exception
         */
        public <E extends Exception> Optional<T> ifPresent(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code Optional}
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is {@code null}
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> Optional<T> ifPresentOrElse(final Throwables.Consumer<? super T, E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present, and the value matches the given predicate, returns an {@code Optional} describing the value, otherwise returns an empty {@code Optional}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if present
         * @return an {@code Optional} describing the value if present and matching the predicate, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code predicate} is {@code null}
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> Optional<T> filter(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code Optional}.
         *
         * @param <U> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code Optional} describing the result of applying the mapping function to the value if present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Optional<U> map(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalBoolean} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalBoolean} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalChar} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalChar} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalByte} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalByte}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalByte} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalShort} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalShort}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalShort} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalInt} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalInt} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalLong} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalLong}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalLong} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalLong}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalFloat} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalFloat}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalFloat} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalFloat}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalDouble} describing the result of applying the given mapping function to the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalDouble} describing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * If a value is present, returns the result of applying the given {@code Optional}-bearing mapping function to the value, otherwise returns an empty {@code Optional}.
         *
         * @param <U> the type of value of the {@code Optional} returned by the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return the result of applying the mapping function to the value if present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Optional<U> flatMap(final Throwables.Function<? super T, Optional<U>, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * Returns {@code true} if a value is present and the value equals the specified value, otherwise {@code false}.
         *
         * @param valueToFind the value to check for equality
         * @return {@code true} if a value is present and equals the specified value, otherwise {@code false}
         */
        public boolean contains(final T valueToFind) {
            return isPresent() && N.equals(value, valueToFind);
        }

        /**
         * If a value is present, returns this {@code Optional}, otherwise returns the {@code Optional} produced by the supplying function.
         *
         * @param supplier the supplying function that produces an {@code Optional} to be returned
         * @return this {@code Optional} if a value is present, otherwise the {@code Optional} produced by the supplying function
         * @throws IllegalArgumentException if {@code supplier} is {@code null}
         */
        public Optional<T> or(final Supplier<Optional<T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, cs.Supplier);

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated using {@link #orElseNull()}
        //     */
        //    @Deprecated
        //    public T orNull() {
        //        return isPresent() ? value : null;
        //    }

        /**
         * Returns the value if present, otherwise returns {@code null}.
         *
         * @return the value if present, otherwise {@code null}
         */
        @Beta
        public T orElseNull() {
            return isPresent() ? value : null;
        }

        /**
         * Returns the value if present, otherwise returns the specified default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise {@code other}
         */
        public T orElse(final T other) {
            return isPresent() ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the supplying function.
         *
         * @param other a {@code Supplier} whose result is returned if no value is present
         * @return the value if present, otherwise the result produced by the supplying function
         */
        public T orElseGet(final Supplier<? extends T> other) {
            if (isPresent()) {
                return value;
            } else {
                return other.get();
            }
        }

        //    public T orElseNull() {
        //        return isPresent() ? value : null;
        //    }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameter.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param the parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @param param3 the third parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param params the parameters to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws E if no value is present
         */
        public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws E {
            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns a {@code Stream} containing only the value if present, otherwise returns an empty {@code Stream}.
         *
         * @return a {@code Stream} containing the value if present, otherwise an empty {@code Stream}
         */
        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        /**
         * Returns a {@code List} containing the value if present, otherwise returns an empty {@code List}.
         *
         * @return a {@code List} containing the value if present, otherwise an empty {@code List}
         */
        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing the value if present, otherwise returns an empty {@code Set}.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty {@code Set}
         */
        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing the value if present, otherwise returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing the value if present, otherwise returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Converts this {@code Optional} to a {@code java.util.Optional}.
         * Returns a {@code java.util.Optional} containing the value if present, otherwise returns an empty {@code java.util.Optional}.
         *
         * @return a {@code java.util.Optional} containing the value if present, otherwise an empty {@code java.util.Optional}
         */
        public java.util.Optional<T> toJdkOptional() {
            if (isPresent()) {
                return java.util.Optional.of(value);
            } else {
                return java.util.Optional.empty();
            }
        }

        /**
         * Converts this {@code Optional} to a {@code java.util.Optional}.
         * This is a shorthand method for {@link #toJdkOptional()}.
         *
         * @return a {@code java.util.Optional} containing the value if present, otherwise an empty {@code java.util.Optional}
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public java.util.Optional<T> __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         * Indicates whether some other object is "equal to" this {@code Optional}.
         * The other object is considered equal if:
         * <ul>
         * <li>it is also an {@code Optional} and;</li>
         * <li>both instances have no value present or;</li>
         * <li>the present values are "equal to" each other via {@code equals()}.</li>
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Optional<?> other) {
                return N.equals(value, other.value);
            }

            return false;
        }

        /**
         * Returns the hash code of the value, if present, otherwise 0 (zero) if no value is present.
         *
         * @return hash code value of the present value or 0 if no value is present
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this {@code Optional} suitable for debugging.
         * The exact presentation format is unspecified and may vary between implementations and versions.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            if (isPresent()) {
                return String.format("Optional[%s]", N.toString(value));
            }

            return "Optional.empty";
        }
    }

    /**
     * A container object which may contain a {@code null} or non-null value.
     * Unlike {@code Optional}, this class allows {@code null} values to be present.
     * If a value has been set (even if {@code null}), {@code isPresent()} returns {@code true}.
     * If no value has been set, the object is considered empty and {@code isPresent()} returns {@code false}.
     *
     * @param <T> the type of value
     * @see com.landawn.abacus.util.u.Optional
     * @see com.landawn.abacus.util.Holder
     * @see com.landawn.abacus.util.Result
     * @see com.landawn.abacus.util.Pair
     * @see com.landawn.abacus.util.Triple
     * @see com.landawn.abacus.util.Tuple
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class Nullable<T> implements Immutable {

        /** Presents {@code Boolean.TRUE}. */
        public static final Nullable<Boolean> TRUE = new Nullable<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Nullable<Boolean> FALSE = new Nullable<>(Boolean.FALSE);

        private static final Nullable<String> NULL_STRING = new Nullable<>(null);

        private static final Nullable<String> EMPTY_STRING = new Nullable<>(Strings.EMPTY);

        /** The Constant EMPTY. */
        private static final Nullable<?> EMPTY = new Nullable<>();

        private final T value;

        private final boolean isPresent;

        /**
         * Instantiates a new {@code nullable}.
         */
        private Nullable() {
            value = null;
            isPresent = false;
        }

        /**
         * Instantiates a new {@code nullable}.
         *
         * @param value
         */
        private Nullable(final T value) {
            this.value = value;
            isPresent = true;
        }

        /**
         * Returns an empty {@code Nullable} instance. No value is present for this {@code Nullable}.
         *
         * @param <T> the type of the non-existent value
         * @return an empty {@code Nullable}
         */
        public static <T> Nullable<T> empty() {
            return (Nullable<T>) EMPTY;
        }

        /**
         * Returns a {@code Nullable} containing the specified {@code String} value.
         * Special handling is provided for {@code null} and empty strings to return singleton instances.
         *
         * @param value the value to be present, which may be {@code null}
         * @return a {@code Nullable} containing the value
         */
        public static Nullable<String> of(final String value) {
            if (value == null) {
                return NULL_STRING;
            } else if (value.isEmpty()) {
                return EMPTY_STRING;
            }

            return new Nullable<>(value);
        }

        /**
         * Returns a {@code Nullable} containing the specified value.
         *
         * @param <T> the type of the value
         * @param value the value to be present, which may be {@code null}
         * @return a {@code Nullable} containing the value
         */
        public static <T> Nullable<T> of(final T value) {
            return new Nullable<>(value);
        }

        /**
         * Returns a {@code Nullable} containing the value from the specified {@code Optional} if present,
         * otherwise returns an empty {@code Nullable}.
         *
         * @param <T> the type of the value
         * @param optional the {@code Optional} to convert
         * @return a {@code Nullable} containing the value if present in the {@code Optional}, otherwise an empty {@code Nullable}
         */
        public static <T> Nullable<T> from(final Optional<T> optional) {
            if (optional.isPresent()) {
                return new Nullable<>(optional.get());
            } else {
                return Nullable.empty();
            }
        }

        /**
         * Returns a {@code Nullable} containing the value from the specified {@code java.util.Optional} if present,
         * otherwise returns an empty {@code Nullable}.
         *
         * @param <T> the type of the value
         * @param optional the {@code java.util.Optional} to convert
         * @return a {@code Nullable} containing the value if present in the {@code java.util.Optional}, otherwise an empty {@code Nullable}
         */
        public static <T> Nullable<T> from(final java.util.Optional<T> optional) {
            return optional.map(Nullable::new).orElseGet(Nullable::empty);
        }

        /**
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if a value has been set (even if {@code null}), otherwise returns {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if no value has been set, otherwise returns {@code false}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         */
        public boolean isNotPresent() {
            return !isPresent;
        }

        /**
         * Returns {@code true} if no value has been set, otherwise returns {@code false}.
         * This method is equivalent to {@link #isNotPresent()}.
         *
         * @return {@code true} if no value is present, otherwise {@code false}
         * @deprecated replaced by {@link #isNotPresent()}
         */
        @Deprecated
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * Returns {@code true} if no value is present or the value is present but is {@code null}, otherwise returns {@code false}.
         *
         * @return {@code true} if the value is {@code null} or not present, otherwise {@code false}
         */
        public boolean isNull() {
            return value == null;
        }

        /**
         * Returns {@code true} if a value is present and it is not {@code null}, otherwise returns {@code false}.
         *
         * @return {@code true} if a non-null value is present, otherwise {@code false}
         */
        public boolean isNotNull() {
            return value != null;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a value is present
         * @return this {@code Nullable} instance
         * @throws IllegalArgumentException if {@code action} is {@code null}
         * @throws E if the action throws an exception
         */
        public <E extends Exception> Nullable<T> ifPresent(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed if a value is present
         * @param emptyAction the empty-based action to be performed if no value is present
         * @return this {@code Nullable} instance
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is {@code null}
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifPresentOrElse(final Throwables.Consumer<? super T, E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present and is not {@code null}, performs the given action with the value, otherwise does nothing.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed if a non-null value is present
         * @return this {@code Nullable} instance
         * @throws IllegalArgumentException if {@code action} is {@code null}
         * @throws E if the action throws an exception
         */
        public <E extends Exception> Nullable<T> ifNotNull(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action);

            if (isNotNull()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If a value is present and is not {@code null}, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param <E> the type of exception that the action may throw
         * @param <E2> the type of exception that the empty action may throw
         * @param action the action to be performed if a non-null value is present
         * @param emptyAction the empty-based action to be performed if the value is {@code null} or not present
         * @return this {@code Nullable} instance
         * @throws IllegalArgumentException if {@code action} or {@code emptyAction} is {@code null}
         * @throws E if the action throws an exception
         * @throws E2 if the empty action throws an exception
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifNotNullOrElse(final Throwables.Consumer<? super T, E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(emptyAction, cs.emptyAction);

            if (isNotNull()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If a value is present and matches the given predicate, returns this {@code Nullable},
         * otherwise returns an empty {@code Nullable}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if present
         * @return this {@code Nullable} if the value is present and matches the predicate, otherwise an empty {@code Nullable}
         * @throws IllegalArgumentException if {@code predicate} is {@code null}
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> Nullable<T> filter(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, cs.Predicate);

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, and matches the given predicate,
         * returns an {@code Optional} containing the value, otherwise returns an empty {@code Optional}.
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to apply to the value if it is not {@code null}
         * @return an {@code Optional} containing the value if it is not {@code null} and matches the predicate, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code predicate} is {@code null}
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
         * If a value is present, returns a {@code Nullable} containing the result of applying the given mapping function to the value,
         * otherwise returns an empty {@code Nullable}.
         *
         * @param <U> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return a {@code Nullable} containing the result of applying the mapping function to the value if present, otherwise an empty {@code Nullable}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Nullable<U> map(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Nullable.of(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present, returns an {@code Optional} containing the result of applying the given mapping function to the value,
         * otherwise returns an empty {@code Optional}.
         * The mapping function must not return {@code null}.
         *
         * @param <U> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present, must not return {@code null}
         * @return an {@code Optional} containing the result of applying the mapping function to the value if present, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Optional<U> mapToNonNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalBoolean} containing the result of applying
         * the given boolean-valued mapping function to the value, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalBoolean} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalChar} containing the result of applying
         * the given char-valued mapping function to the value, otherwise returns an empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalChar} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalByte} containing the result of applying
         * the given byte-valued mapping function to the value, otherwise returns an empty {@code OptionalByte}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalByte} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalShort} containing the result of applying
         * the given short-valued mapping function to the value, otherwise returns an empty {@code OptionalShort}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalShort} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalInt} containing the result of applying
         * the given int-valued mapping function to the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalInt} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalLong} containing the result of applying
         * the given long-valued mapping function to the value, otherwise returns an empty {@code OptionalLong}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalLong} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalLong}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalFloat} containing the result of applying
         * the given float-valued mapping function to the value, otherwise returns an empty {@code OptionalFloat}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalFloat} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalFloat}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * If a value is present, returns an {@code OptionalDouble} containing the result of applying
         * the given double-valued mapping function to the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return an {@code OptionalDouble} containing the result of applying the mapping function to the value if present, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns a {@code Nullable} containing the result of applying
         * the given mapping function to the value, otherwise returns an empty {@code Nullable}.
         *
         * @param <U> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return a {@code Nullable} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code Nullable}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return Nullable.of(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code Optional} containing the result of applying
         * the given mapping function to the value, otherwise returns an empty {@code Optional}.
         * The mapping function must not return {@code null}.
         *
         * @param <U> the type of the value returned from the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}, must not return {@code null}
         * @return an {@code Optional} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code Optional}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
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
         * If a value is present and is not {@code null}, returns an {@code OptionalBoolean} containing the result of applying
         * the given boolean-valued mapping function to the value, otherwise returns an empty {@code OptionalBoolean}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalBoolean} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalBoolean}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalBoolean mapToBooleanIfNotNull(final Throwables.ToBooleanFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalChar} containing the result of applying
         * the given char-valued mapping function to the value, otherwise returns an empty {@code OptionalChar}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalChar} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalChar}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalChar mapToCharIfNotNull(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalByte} containing the result of applying
         * the given byte-valued mapping function to the value, otherwise returns an empty {@code OptionalByte}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalByte} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalByte}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalByte mapToByteIfNotNull(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalShort} containing the result of applying
         * the given short-valued mapping function to the value, otherwise returns an empty {@code OptionalShort}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalShort} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalShort}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalShort mapToShortIfNotNull(final Throwables.ToShortFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalInt} containing the result of applying
         * the given int-valued mapping function to the value, otherwise returns an empty {@code OptionalInt}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalInt} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalInt}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalInt mapToIntIfNotNull(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalLong} containing the result of applying
         * the given long-valued mapping function to the value, otherwise returns an empty {@code OptionalLong}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalLong} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalLong}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalLong mapToLongIfNotNull(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalFloat} containing the result of applying
         * the given float-valued mapping function to the value, otherwise returns an empty {@code OptionalFloat}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalFloat} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalFloat}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalFloat mapToFloatIfNotNull(final Throwables.ToFloatFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, returns an {@code OptionalDouble} containing the result of applying
         * the given double-valued mapping function to the value, otherwise returns an empty {@code OptionalDouble}.
         *
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return an {@code OptionalDouble} containing the result of applying the mapping function to the value if it is not {@code null}, otherwise an empty {@code OptionalDouble}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <E extends Exception> OptionalDouble mapToDoubleIfNotNull(final Throwables.ToDoubleFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * If a value is present, applies the {@code Nullable}-bearing mapping function to it,
         * and returns that result, otherwise returns an empty {@code Nullable}.
         * This method is similar to {@link #map(Throwables.Function)}, but the mapping function is one whose result is already a {@code Nullable},
         * and if invoked, {@code flatMap} does not wrap it within an additional {@code Nullable}.
         *
         * @param <U> the type of value of the {@code Nullable} returned by the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if present
         * @return the result of applying a {@code Nullable}-bearing mapping function to the value of this {@code Nullable}, if a value is present, otherwise an empty {@code Nullable}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Nullable<U> flatMap(final Throwables.Function<? super T, Nullable<U>, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * If a value is present and is not {@code null}, applies the {@code Nullable}-bearing mapping function to it,
         * and returns that result, otherwise returns an empty {@code Nullable}.
         * This method is similar to {@link #mapIfNotNull(Throwables.Function)}, but the mapping function is one whose result is already a {@code Nullable},
         * and if invoked, {@code flatMapIfNotNull} does not wrap it within an additional {@code Nullable}.
         *
         * @param <U> the type of value of the {@code Nullable} returned by the mapping function
         * @param <E> the type of exception that the mapping function may throw
         * @param mapper the mapping function to apply to the value if it is not {@code null}
         * @return the result of applying a {@code Nullable}-bearing mapping function to the value of this {@code Nullable}, if the value is not {@code null}, otherwise an empty {@code Nullable}
         * @throws IllegalArgumentException if {@code mapper} is {@code null}
         * @throws E if the mapping function throws an exception
         */
        public <U, E extends Exception> Nullable<U> flatMapIfNotNull(final Throwables.Function<? super T, Nullable<U>, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, cs.mapper);

            if (isNotNull()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * Returns {@code true} if a value is present and equals the specified value, otherwise returns {@code false}.
         *
         * @param valueToFind the value to check for equality
         * @return {@code true} if a value is present and equals the specified value, otherwise {@code false}
         */
        public boolean contains(final T valueToFind) {
            return isPresent() && N.equals(value, valueToFind);
        }

        /**
         * Returns this {@code Nullable} if a value is present, otherwise returns the {@code Nullable} produced by the supplying function.
         *
         * @param supplier the supplying function that produces a {@code Nullable} to be returned
         * @return this {@code Nullable} if a value is present, otherwise the {@code Nullable} produced by the supplying function
         * @throws IllegalArgumentException if {@code supplier} is {@code null}
         */
        public Nullable<T> or(final Supplier<Nullable<? extends T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, cs.Supplier);

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        /**
         * Returns this {@code Nullable} if the value is not {@code null}, otherwise returns the {@code Nullable} produced by the supplying function.
         *
         * @param supplier the supplying function that produces a {@code Nullable} to be returned
         * @return this {@code Nullable} if the value is not {@code null}, otherwise the {@code Nullable} produced by the supplying function
         * @throws IllegalArgumentException if {@code supplier} is {@code null}
         */
        public Nullable<T> orIfNull(final Supplier<Nullable<? extends T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, cs.Supplier);

            if (isNotNull()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        //    /**
        //     *
        //     * @return
        //     * @deprecated using {@link #orElseNull()}
        //     */
        //    @Deprecated
        //    public T orNull() {
        //        return isPresent() ? value : null;
        //    }

        /**
         * Returns the value if present, otherwise returns {@code null}.
         *
         * @return the value if present, otherwise {@code null}
         * @see #orElse(Object)
         */
        @Beta
        public T orElseNull() {
            return isPresent() ? value : null;
        }

        /**
         * Returns the value if present, otherwise returns the specified default value.
         *
         * @param other the value to be returned if no value is present
         * @return the value if present, otherwise the specified default value
         */
        public T orElse(final T other) {
            return isPresent() ? value : other;
        }

        /**
         * Returns the value if it is not {@code null}, otherwise returns the specified default value.
         *
         * @param other the value to be returned if the value is {@code null}
         * @return the value if it is not {@code null}, otherwise the specified default value
         */
        public T orElseIfNull(final T other) {
            return isNotNull() ? value : other;
        }

        /**
         * Returns the value if present, otherwise returns the result produced by the supplying function.
         *
         * @param other the supplying function that produces a value to be returned
         * @return the value if present, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is {@code null}
         */
        public T orElseGet(final Supplier<? extends T> other) throws IllegalArgumentException {
            N.checkArgNotNull(other, cs.other);

            if (isPresent()) {
                return value;
            } else {
                return other.get();
            }
        }

        /**
         * Returns the value if it is not {@code null}, otherwise returns the result produced by the supplying function.
         *
         * @param other the supplying function that produces a value to be returned
         * @return the value if it is not {@code null}, otherwise the result produced by the supplying function
         * @throws IllegalArgumentException if {@code other} is {@code null}
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
         * Returns the value if present, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message.
         *
         * @param errorMessage the error message to use if no value is present
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(errorMessage);
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameter.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param the parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param1, final Object param2) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @param param3 the third parameter to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object param1, final Object param2, final Object param3) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, param1, param2, param3));
            }
        }

        /**
         * Returns the value if present, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if no value is present
         * @param params the parameters to format into the error message
         * @return the value if present
         * @throws NoSuchElementException if no value is present
         */
        @Beta
        public T orElseThrow(final String errorMessage, final Object... params) throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(N.format(errorMessage, params));
            }
        }

        /**
         * Returns the value if present, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if present
         * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}
         * @throws E if no value is present
         */
        public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Returns the value if it is not {@code null}, otherwise throws {@code NoSuchElementException}.
         *
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws a {@code NoSuchElementException} with the specified error message.
         *
         * @param errorMessage the error message to use if the value is {@code null}
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameter.
         *
         * @param errorMessage the error message template to use if the value is {@code null}
         * @param param the parameter to format into the error message
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if the value is {@code null}
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if the value is {@code null}
         * @param param1 the first parameter to format into the error message
         * @param param2 the second parameter to format into the error message
         * @param param3 the third parameter to format into the error message
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws a {@code NoSuchElementException} with the specified error message
         * formatted with the given parameters.
         *
         * @param errorMessage the error message template to use if the value is {@code null}
         * @param params the parameters to format into the error message
         * @return the value if it is not {@code null}
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
         * Returns the value if it is not {@code null}, otherwise throws an exception produced by the exception supplying function.
         *
         * @param <E> the type of exception to be thrown
         * @param exceptionSupplier the supplying function that produces an exception to be thrown
         * @return the value if it is not {@code null}
         * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}
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

        /**
         * Returns a {@code Stream} containing the value of this {@code Nullable} if a value is present,
         * otherwise returns an empty {@code Stream}.
         * <p>
         * This method creates a stream with zero or one element depending on whether a value is present.
         * If the {@code Nullable} is empty (not present), an empty stream is returned.
         * If the {@code Nullable} contains a value (including {@code null}), a stream containing that single value is returned.
         *
         * @return a {@code Stream} containing the value if present, otherwise an empty {@code Stream}
         */
        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        /**
         * Returns a {@code Stream} containing the value of this {@code Nullable} if the value is not {@code null},
         * otherwise returns an empty {@code Stream}.
         * <p>
         * This method differs from {@link #stream()} in that it returns an empty stream both when the {@code Nullable}
         * is empty and when it contains a {@code null} value. A stream with the value is only returned when
         * the {@code Nullable} contains a non-null value.
         *
         * @return a {@code Stream} containing the value if not {@code null}, otherwise an empty {@code Stream}
         */
        public Stream<T> streamIfNotNull() {
            if (isNotNull()) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        /**
         * Returns a {@code List} containing the value of this {@code Nullable} if a value is present,
         * otherwise returns an empty {@code List}.
         * <p>
         * If the {@code Nullable} contains a value (including {@code null}), returns a list containing that single value.
         * If the {@code Nullable} is empty (not present), returns an empty {@code ArrayList}.
         *
         * @return a {@code List} containing the value if present, otherwise an empty {@code List}
         */
        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code List} containing the value of this {@code Nullable} if the value is not {@code null},
         * otherwise returns an empty {@code List}.
         * <p>
         * This method differs from {@link #toList()} in that it returns an empty list both when the {@code Nullable}
         * is empty and when it contains a {@code null} value. A list with the value is only returned when
         * the {@code Nullable} contains a non-null value.
         *
         * @return a {@code List} containing the value if not {@code null}, otherwise an empty {@code List}
         */
        public List<T> toListIfNotNull() {
            if (isNotNull()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * Returns a {@code Set} containing the value of this {@code Nullable} if a value is present,
         * otherwise returns an empty {@code Set}.
         * <p>
         * If the {@code Nullable} contains a value (including {@code null}), returns a set containing that single value.
         * If the {@code Nullable} is empty (not present), returns an empty {@code HashSet}.
         *
         * @return a {@code Set} containing the value if present, otherwise an empty {@code Set}
         */
        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns a {@code Set} containing the value of this {@code Nullable} if the value is not {@code null},
         * otherwise returns an empty {@code Set}.
         * <p>
         * This method differs from {@link #toSet()} in that it returns an empty set both when the {@code Nullable}
         * is empty and when it contains a {@code null} value. A set with the value is only returned when
         * the {@code Nullable} contains a non-null value.
         *
         * @return a {@code Set} containing the value if not {@code null}, otherwise an empty {@code Set}
         */
        public Set<T> toSetIfNotNull() {
            if (isNotNull()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing the value of this {@code Nullable} if a value is present,
         * otherwise returns an empty {@code ImmutableList}.
         * <p>
         * If the {@code Nullable} contains a value (including {@code null}), returns an immutable list containing that single value.
         * If the {@code Nullable} is empty (not present), returns an empty {@code ImmutableList}.
         *
         * @return an {@code ImmutableList} containing the value if present, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableList} containing the value of this {@code Nullable} if the value is not {@code null},
         * otherwise returns an empty {@code ImmutableList}.
         * <p>
         * This method differs from {@link #toImmutableList()} in that it returns an empty immutable list both when 
         * the {@code Nullable} is empty and when it contains a {@code null} value. An immutable list with the value 
         * is only returned when the {@code Nullable} contains a non-null value.
         *
         * @return an {@code ImmutableList} containing the value if not {@code null}, otherwise an empty {@code ImmutableList}
         */
        public ImmutableList<T> toImmutableListIfNotNull() {
            if (isNotNull()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing the value of this {@code Nullable} if a value is present,
         * otherwise returns an empty {@code ImmutableSet}.
         * <p>
         * If the {@code Nullable} contains a value (including {@code null}), returns an immutable set containing that single value.
         * If the {@code Nullable} is empty (not present), returns an empty {@code ImmutableSet}.
         *
         * @return an {@code ImmutableSet} containing the value if present, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an {@code ImmutableSet} containing the value of this {@code Nullable} if the value is not {@code null},
         * otherwise returns an empty {@code ImmutableSet}.
         * <p>
         * This method differs from {@link #toImmutableSet()} in that it returns an empty immutable set both when 
         * the {@code Nullable} is empty and when it contains a {@code null} value. An immutable set with the value 
         * is only returned when the {@code Nullable} contains a non-null value.
         *
         * @return an {@code ImmutableSet} containing the value if not {@code null}, otherwise an empty {@code ImmutableSet}
         */
        public ImmutableSet<T> toImmutableSetIfNotNull() {
            if (isNotNull()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Converts this {@code Nullable} to an {@code Optional}.
         * <p>
         * If this {@code Nullable} contains a non-null value, returns an {@code Optional} containing that value.
         * If this {@code Nullable} is empty or contains a {@code null} value, returns an empty {@code Optional}.
         * <p>
         * Note that this conversion loses the distinction between an empty {@code Nullable} and a {@code Nullable}
         * containing {@code null}, as both are mapped to an empty {@code Optional}.
         *
         * @return an {@code Optional} containing the value if present and not {@code null}, otherwise an empty {@code Optional}
         */
        public Optional<T> toOptional() {
            if (value == null) {
                return Optional.empty();
            } else {
                return Optional.of(value);
            }
        }

        /**
         * Converts this {@code Nullable} to a {@code java.util.Optional}.
         * <p>
         * If this {@code Nullable} contains a non-null value, returns a {@code java.util.Optional} containing that value.
         * If this {@code Nullable} is empty or contains a {@code null} value, returns an empty {@code java.util.Optional}.
         * <p>
         * Note that this conversion loses the distinction between an empty {@code Nullable} and a {@code Nullable}
         * containing {@code null}, as both are mapped to an empty {@code java.util.Optional}.
         *
         * @return a {@code java.util.Optional} containing the value if present and not {@code null}, otherwise an empty {@code java.util.Optional}
         */
        public java.util.Optional<T> toJdkOptional() {
            if (value == null) {
                return java.util.Optional.empty();
            } else {
                return java.util.Optional.of(value);
            }
        }

        /**
         * Indicates whether some other object is "equal to" this {@code Nullable}.
         * <p>
         * Two {@code Nullable} instances are considered equal if:
         * <ul>
         * <li>Both are empty (not present), or</li>
         * <li>Both are present and their values are equal according to {@link Objects#equals(Object, Object)}</li>
         * </ul>
         * <p>
         * Note that a {@code Nullable} containing {@code null} is not equal to an empty {@code Nullable},
         * as they represent different states.
         *
         * @param obj the object to be tested for equality
         * @return {@code true} if the other object is a {@code Nullable} and both instances have the same presence state
         *         and equal values; {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Nullable<?> other) {
                return N.equals(isPresent, other.isPresent) && N.equals(value, other.value);
            }

            return false;
        }

        /**
         * Returns the hash code of this {@code Nullable}.
         * <p>
         * The hash code is computed based on both the presence state and the value.
         * If the {@code Nullable} is empty, the hash code is based only on the presence state.
         * If the {@code Nullable} contains a value (including {@code null}), the hash code combines
         * the presence state and the value's hash code.
         *
         * @return the hash code value for this {@code Nullable}
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         * Returns a string representation of this {@code Nullable}.
         * <p>
         * The string representation varies based on the state:
         * <ul>
         * <li>If empty (not present): returns "Nullable.empty"</li>
         * <li>If present with {@code null} value: returns "Nullable[null]"</li>
         * <li>If present with non-null value: returns "Nullable[value]" where value is the string representation of the contained value</li>
         * </ul>
         *
         * @return a string representation of this {@code Nullable}
         */
        @Override
        public String toString() {
            if (value == null) {
                return isPresent ? "Nullable[null]" : "Nullable.empty";
            } else {
                return String.format("Nullable[%s]", N.toString(value));
            }
        }
    }
}
