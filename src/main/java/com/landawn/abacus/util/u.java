/*
 * Copyright (c) 2019, Haiyang Li.
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

@SuppressWarnings("java:S1192")
public class u {//NOSONAR

    private static final String NO_VALUE_PRESENT = "No value present"; // should change it to InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX

    private u() {
        // utility class
    }

    /**
     * The Class Optional.
     *
     * @param <T>
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class Optional<T> implements Immutable {

        /** Presents {@code Boolean.TRUE}. */
        public static final Optional<Boolean> TRUE = new Optional<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Optional<Boolean> FALSE = new Optional<>(Boolean.FALSE);

        private static final Optional<String> EMPTY_STRING = new Optional<>(Strings.EMPTY_STRING);

        /** The Constant EMPTY. */
        private static final Optional<?> EMPTY = new Optional<>();

        /** The value. */
        private final T value;

        /**
         * Instantiates a new optional.
         */
        private Optional() {
            this.value = null;
        }

        /**
         * Instantiates a new optional.
         *
         * @param value
         * @throws NullPointerException if {@code value} is {@code null}
         */
        private Optional(T value) throws NullPointerException {
            this.value = Objects.requireNonNull(value);
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> Optional<T> empty() {
            return (Optional<T>) EMPTY;
        }

        /**
         *
         *
         * @param value
         * @return
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public static Optional<String> of(final String value) throws NullPointerException {
            Objects.requireNonNull(value);

            if (value.length() == 0) {
                return EMPTY_STRING;
            }

            return new Optional<>(value);
        }

        /**
         *
         * @param <T>
         * @param value
         * @return
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public static <T> Optional<T> of(final T value) throws NullPointerException {
            return new Optional<>(value);
        }

        /**
         *
         *
         * @param value
         * @return
         */
        public static Optional<String> ofNullable(final String value) {
            if (value == null) {
                return empty();
            } else if (value.length() == 0) {
                return EMPTY_STRING;
            }

            return new Optional<>(value);
        }

        /**
         *
         * @param <T>
         * @param value
         * @return
         */
        public static <T> Optional<T> ofNullable(T value) {
            if (value == null) {
                return empty();
            }

            return new Optional<>(value);
        }

        /**
         *
         * @param <T>
         * @param op
         * @return
         */
        public static <T> Optional<T> from(java.util.Optional<T> op) {
            if (op.isPresent()) {
                return of(op.get());
            } else {
                return empty();
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Checks if is present.
         *
         * @return true, if is present
         */
        public boolean isPresent() {
            return value != null;
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        public boolean isEmpty() {
            return value == null;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> Optional<T> ifPresent(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Optional<T> ifPresentOrElse(final Throwables.Consumer<? super T, E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> Optional<T> filter(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> map(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.<U> of(mapper.apply(value));
            } else {
                return Nullable.<U> empty();
            }
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Optional.<U> of(mapper.apply(value));
            } else {
                return Optional.<U> empty();
            }
        }

        /**
         * Map to boolean.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Map to char.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Map to byte.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * Map to short.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to float.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> flatMap(final Throwables.Function<? super T, Optional<U>, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param valueToFind
         * @return
         */
        public boolean contains(final T valueToFind) {
            return isPresent() && N.equals(this.value, valueToFind);
        }

        /**
         *
         *
         * @param supplier
         * @return
         * @throws IllegalArgumentException
         */
        public Optional<T> or(final Supplier<Optional<T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, "supplier");

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
         *
         * @return
         */
        @Beta
        public T orElseNull() {
            return isPresent() ? value : null;
        }

        /**
         *
         * @param other
         * @return
         */
        public T orElse(T other) {
            return isPresent() ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
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
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws E
         */
        public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws E {
            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        /**
         *
         * @return
         */
        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<T> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<T> empty();
            }
        }

        /**
         *
         * @return
         */
        @Beta
        public Nullable<T> toNullable() {
            if (isPresent()) {
                return Nullable.of(value);
            } else {
                return Nullable.empty();
            }
        }

        /**
         *
         * @return
         */
        public java.util.Optional<T> toJdkOptional() {
            if (isPresent()) {
                return java.util.Optional.of(value);
            } else {
                return java.util.Optional.empty();
            }
        }

        /**
         *
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public java.util.Optional<T> __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Optional) {
                final Optional<?> other = (Optional<?>) obj;

                return N.equals(value, other.value);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalBoolean.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalBoolean implements Comparable<OptionalBoolean>, Immutable {

        /** Presents {@code true}. */
        public static final OptionalBoolean TRUE = new OptionalBoolean(true);

        /** Presents {@code true}. */
        public static final OptionalBoolean FALSE = new OptionalBoolean(false);

        /** The Constant EMPTY. */
        private static final OptionalBoolean EMPTY = new OptionalBoolean();

        /** The value. */
        private final boolean value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional boolean.
         */
        private OptionalBoolean() {
            this.value = false;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional boolean.
         *
         * @param value
         */
        private OptionalBoolean(boolean value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalBoolean empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalBoolean of(boolean value) {
            return value ? TRUE : FALSE;
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalBoolean ofNullable(Boolean val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public boolean get() throws NoSuchElementException { // NOSONAR
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean ifPresent(final Throwables.BooleanConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalBoolean ifPresentOrElse(final Throwables.BooleanConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean filter(final Throwables.BooleanPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean map(final Throwables.BooleanUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to char.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to Long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to Double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Boolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.BooleanFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.BooleanFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean flatMap(final Throwables.BooleanFunction<OptionalBoolean, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         *
         * @param supplier
         * @return
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

        //    public boolean orElseTrue() {
        //        return isPresent ? value : true;
        //    }

        /**
         *
         * @param other
         * @return
         */
        public boolean orElse(boolean other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public boolean orElseGet(final BooleanSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsBoolean();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public boolean orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> boolean orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public Stream<Boolean> stream() {
            if (isPresent) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Boolean> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Boolean> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Boolean> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Boolean> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Boolean> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Boolean> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Boolean> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Boolean> empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalBoolean optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Boolean.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalBoolean other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalChar.
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

        /** The value. */
        private final char value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional char.
         */
        private OptionalChar() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional char.
         *
         * @param value
         */
        private OptionalChar(char value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalChar empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalChar of(char value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalChar(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalChar ofNullable(Character val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public char get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar ifPresent(final Throwables.CharConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalChar ifPresentOrElse(final Throwables.CharConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar filter(final Throwables.CharPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar map(final Throwables.CharUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to boolean.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBolean(final Throwables.ToBooleanFunction<Character, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Character, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.CharFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.CharFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar flatMap(final Throwables.CharFunction<OptionalChar, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public char orElseZero() {
            return isPresent() ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public char orElse(char other) {
            return isPresent() ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public char orElseGet(final CharSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent()) {
                return value;
            } else {
                return other.getAsChar();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public char orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> char orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public CharStream stream() {
            if (isPresent) {
                return CharStream.of(value);
            } else {
                return CharStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Character> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Character> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Character> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Character> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Character> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Character> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Character> boxed() {
            if (isPresent()) {
                return Optional.of(value);
            } else {
                return Optional.<Character> empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalChar optional) {
            if (optional == null || !optional.isPresent()) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Character.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalChar other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalByte.
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

        /** The value. */
        private final byte value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional byte.
         */
        private OptionalByte() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional byte.
         *
         * @param value
         */
        private OptionalByte(byte value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalByte empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalByte of(byte value) {
            return cached[value - MIN_CACHED_VALUE];
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalByte ofNullable(Byte val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalByte.of(val);
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public byte get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte ifPresent(final Throwables.ByteConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalByte ifPresentOrElse(final Throwables.ByteConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte filter(final Throwables.BytePredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte map(final Throwables.ByteUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Byte, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.ByteFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.ByteFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte flatMap(final Throwables.ByteFunction<OptionalByte, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public byte orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public byte orElse(byte other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public byte orElseGet(final ByteSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsByte();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public byte orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> byte orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public ByteStream stream() {
            if (isPresent) {
                return ByteStream.of(value);
            } else {
                return ByteStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Byte> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Byte> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Byte> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Byte> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Byte> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Byte> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Byte> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Byte> empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalByte optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Byte.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalByte other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalShort.
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

        /** The value. */
        private final short value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional short.
         */
        private OptionalShort() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional short.
         *
         * @param value
         */
        private OptionalShort(short value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalShort empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalShort of(short value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalShort(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalShort ofNullable(Short val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalShort.of(val);
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public short get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort ifPresent(final Throwables.ShortConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalShort ifPresentOrElse(final Throwables.ShortConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort filter(final Throwables.ShortPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort map(final Throwables.ShortUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Short, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.ShortFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.ShortFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort flatMap(final Throwables.ShortFunction<OptionalShort, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public short orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public short orElse(short other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public short orElseGet(final ShortSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsShort();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public short orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> short orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public ShortStream stream() {
            if (isPresent) {
                return ShortStream.of(value);
            } else {
                return ShortStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Short> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Short> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Short> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Short> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Short> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Short> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Short> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Short> empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalShort optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Short.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalShort other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalInt.
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

        /** The value. */
        private final int value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional int.
         */
        private OptionalInt() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional int.
         *
         * @param value
         */
        private OptionalInt(int value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalInt empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalInt of(int value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[value - MIN_CACHED_VALUE] : new OptionalInt(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalInt ofNullable(Integer val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalInt.of(val);
            }
        }

        /**
         *
         * @param op
         * @return
         */
        public static OptionalInt from(java.util.OptionalInt op) {
            if (op.isPresent()) {
                return of(op.getAsInt());
            } else {
                return empty();
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public int get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt ifPresent(final Throwables.IntConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalInt ifPresentOrElse(final Throwables.IntConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt filter(final Throwables.IntPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt map(final Throwables.IntUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to boolean.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBolean(final Throwables.ToBooleanFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Map to char.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Map to long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to float.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Integer, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.IntFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.IntFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt flatMap(final Throwables.IntFunction<OptionalInt, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public int orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public int orElse(int other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public int orElseGet(final IntSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsInt();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public int orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> int orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public IntStream stream() {
            if (isPresent) {
                return IntStream.of(value);
            } else {
                return IntStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Integer> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Integer> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Integer> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Integer> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Integer> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Integer> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Integer> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Integer> empty();
            }
        }

        /**
         *
         * @return
         */
        public java.util.OptionalInt toJdkOptional() {
            if (isPresent) {
                return java.util.OptionalInt.of(value);
            } else {
                return java.util.OptionalInt.empty();
            }
        }

        /**
         *
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public java.util.OptionalInt __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalInt optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Integer.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalInt other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalLong.
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

        /** The value. */
        private final long value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional long.
         */
        private OptionalLong() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional long.
         *
         * @param value
         */
        private OptionalLong(long value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalLong empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalLong of(long value) {
            return value >= MIN_CACHED_VALUE && value <= MAX_CACHED_VALUE ? cached[(int) (value - MIN_CACHED_VALUE)] : new OptionalLong(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalLong ofNullable(Long val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalLong.of(val);
            }
        }

        /**
         *
         * @param op
         * @return
         */
        public static OptionalLong from(java.util.OptionalLong op) {
            if (op.isPresent()) {
                return of(op.getAsLong());
            } else {
                return empty();
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public long get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong ifPresent(final Throwables.LongConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalLong ifPresentOrElse(final Throwables.LongConsumer<E> action,
                final Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong filter(final Throwables.LongPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong map(final Throwables.LongUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Long, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Long, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.LongFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.LongFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong flatMap(final Throwables.LongFunction<OptionalLong, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public long orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public long orElse(long other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public long orElseGet(final LongSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsLong();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public long orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> long orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public LongStream stream() {
            if (isPresent) {
                return LongStream.of(value);
            } else {
                return LongStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Long> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Long> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Long> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Long> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Long> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Long> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Long> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Long> empty();
            }
        }

        /**
         *
         * @return
         */
        public java.util.OptionalLong toJdkOptional() {
            if (isPresent) {
                return java.util.OptionalLong.of(value);
            } else {
                return java.util.OptionalLong.empty();
            }
        }

        /**
         *
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public java.util.OptionalLong __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalLong optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Long.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalLong other) {
                return (isPresent && other.isPresent) ? value == other.value : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalFloat.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalFloat implements Comparable<OptionalFloat>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalFloat EMPTY = new OptionalFloat();

        private static final OptionalFloat ZERO = new OptionalFloat(0f);

        /** The value. */
        private final float value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional float.
         */
        private OptionalFloat() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional float.
         *
         * @param value
         */
        private OptionalFloat(float value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalFloat empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalFloat of(float value) {
            return value == 0f ? ZERO : new OptionalFloat(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalFloat ofNullable(Float val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalFloat.of(val);
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public float get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat ifPresent(final Throwables.FloatConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalFloat ifPresentOrElse(final Throwables.FloatConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat filter(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat map(final Throwables.FloatUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Float, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Float, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.FloatFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.FloatFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat flatMap(final Throwables.FloatFunction<OptionalFloat, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public float orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public float orElse(float other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public float orElseGet(final FloatSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsFloat();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public float orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> float orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public FloatStream stream() {
            if (isPresent) {
                return FloatStream.of(value);
            } else {
                return FloatStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Float> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Float> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Float> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Float> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Float> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Float> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Float> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Float> empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalFloat optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Float.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalFloat other) {
                return (isPresent && other.isPresent) ? N.equals(value, other.value) : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class OptionalDouble.
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class OptionalDouble implements Comparable<OptionalDouble>, Immutable {

        /** The Constant EMPTY. */
        private static final OptionalDouble EMPTY = new OptionalDouble();

        private static final OptionalDouble ZERO = new OptionalDouble(0d);

        /** The value. */
        private final double value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new optional double.
         */
        private OptionalDouble() {
            this.value = 0;
            this.isPresent = false;
        }

        /**
         * Instantiates a new optional double.
         *
         * @param value
         */
        private OptionalDouble(double value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @return
         */
        public static OptionalDouble empty() {
            return EMPTY;
        }

        /**
         *
         * @param value
         * @return
         */
        public static OptionalDouble of(double value) {
            return value == 0d ? ZERO : new OptionalDouble(value);
        }

        /**
         *
         * @param val
         * @return
         */
        public static OptionalDouble ofNullable(Double val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalDouble.of(val);
            }
        }

        /**
         *
         * @param op
         * @return
         */
        public static OptionalDouble from(java.util.OptionalDouble op) {
            if (op.isPresent()) {
                return of(op.getAsDouble());
            } else {
                return empty();
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public double get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         *
         *
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         *
         *
         * @return
         */
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble ifPresent(final Throwables.DoubleConsumer<E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalDouble ifPresentOrElse(final Throwables.DoubleConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble filter(final Throwables.DoublePredicate<E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble map(final Throwables.DoubleUnaryOperator<E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Double, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Double, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.DoubleFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.DoubleFunction<? extends T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble flatMap(final Throwables.DoubleFunction<OptionalDouble, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

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
         *
         * @param supplier
         * @return
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
         *
         *
         * @return
         */
        public double orElseZero() {
            return isPresent ? value : 0;
        }

        /**
         *
         * @param other
         * @return
         */
        public double orElse(double other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public double orElseGet(final DoubleSupplier other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsDouble();
            }
        }

        /**
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public double orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException(NO_VALUE_PRESENT);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> double orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         *
         * @return
         */
        public DoubleStream stream() {
            if (isPresent) {
                return DoubleStream.of(value);
            } else {
                return DoubleStream.empty();
            }
        }

        /**
         *
         * @return
         */
        public List<Double> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<Double> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<Double> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<Double> empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<Double> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<Double> empty();
            }
        }

        /**
         *
         * @return
         */
        public Optional<Double> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Double> empty();
            }
        }

        /**
         *
         * @return
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
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public java.util.OptionalDouble __() {//NOSONAR
            return toJdkOptional();
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(final OptionalDouble optional) {
            if (optional == null || !optional.isPresent) {
                return isPresent ? 1 : 0;
            }

            if (!isPresent) {
                return -1;
            }

            return Double.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalDouble other) {
                return (isPresent && other.isPresent) ? N.equals(value, other.value) : isPresent == other.isPresent;
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
     * The Class Nullable.
     *
     * @param <T>
     */
    @com.landawn.abacus.annotation.Immutable
    public static final class Nullable<T> implements Immutable {

        /** Presents {@code Boolean.TRUE}. */
        public static final Nullable<Boolean> TRUE = new Nullable<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Nullable<Boolean> FALSE = new Nullable<>(Boolean.FALSE);

        private static final Nullable<String> NULL_STRING = new Nullable<>(null);

        private static final Nullable<String> EMPTY_STRING = new Nullable<>(Strings.EMPTY_STRING);

        /** The Constant EMPTY. */
        private static final Nullable<?> EMPTY = new Nullable<>();

        /** The value. */
        private final T value;

        /** The is present. */
        private final boolean isPresent;

        /**
         * Instantiates a new nullable.
         */
        private Nullable() {
            this.value = null;
            this.isPresent = false;
        }

        /**
         * Instantiates a new nullable.
         *
         * @param value
         */
        private Nullable(T value) {
            this.value = value;
            this.isPresent = true;
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> Nullable<T> empty() {
            return (Nullable<T>) EMPTY;
        }

        /**
         *
         *
         * @param value
         * @return
         */
        public static Nullable<String> of(final String value) {
            if (value == null) {
                return NULL_STRING;
            } else if (value.length() == 0) {
                return EMPTY_STRING;
            }

            return new Nullable<>(value);
        }

        /**
         *
         * @param <T>
         * @param value
         * @return
         */
        public static <T> Nullable<T> of(final T value) {
            return new Nullable<>(value);
        }

        /**
         *
         * @param <T>
         * @param optional
         * @return
         */
        public static <T> Nullable<T> from(Optional<T> optional) {
            if (optional.isPresent()) {
                return new Nullable<>(optional.get());
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         * @param <T>
         * @param optional
         * @return
         */
        public static <T> Nullable<T> from(java.util.Optional<T> optional) {
            if (optional.isPresent()) {
                return new Nullable<>(optional.get());
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if the value is present, otherwise returns {@code false}.
         *
         * @return true, if is present
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if the value is not present, otherwise returns {@code false}.
         *
         * @return true, if is not present
         */
        public boolean isNotPresent() {
            return !isPresent;
        }

        /**
         * Returns {@code true} if the value is not present, otherwise returns {@code false}.
         *
         * @return true, if is empty
         * @deprecated replaced by {@link #isNotPresent()}
         */
        @Deprecated
        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         * Returns {@code true} if the value is not present, or it is present but it's {@code null}, otherwise returns {@code false}.
         *
         * @return true, if is null
         */
        public boolean isNull() {
            return value == null;
        }

        /**
         * Returns {@code true} if the value is present and it's not {@code null}, otherwise returns {@code false}.
         *
         * @return true, if is not null
         */
        public boolean isNotNull() {
            return value != null;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> ifPresent(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If present or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifPresentOrElse(final Throwables.Consumer<? super T, E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         * If not null.
         *
         * @param <E>
         * @param action
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> ifNotNull(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * If not null or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @return itself
         * @throws IllegalArgumentException
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifNotNullOrElse(final Throwables.Consumer<? super T, E> action,
                Throwables.Runnable<E2> emptyAction) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isNotNull()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> filter(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         * Filter if not null.
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
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
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> map(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Optional.of((U) mapper.apply(value));
            } else {
                return Optional.<U> empty();
            }
        }

        /**
         * Map to boolean.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Map to char.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Map to byte.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * Map to short.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * Map to int.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to float.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         * Map if not null.
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * Map if not null.
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Optional.of((U) mapper.apply(value));
            } else {
                return Optional.<U> empty();
            }
        }

        /**
         * Map to boolean if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBooleanIfNotNull(final Throwables.ToBooleanFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        /**
         * Map to char if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToCharIfNotNull(final Throwables.ToCharFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        /**
         * Map to byte if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByteIfNotNull(final Throwables.ToByteFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        /**
         * Map to short if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShortIfNotNull(final Throwables.ToShortFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        /**
         * Map to int if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToIntIfNotNull(final Throwables.ToIntFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        /**
         * Map to long if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLongIfNotNull(final Throwables.ToLongFunction<? super T, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to float if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloatIfNotNull(final Throwables.ToFloatFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        /**
         * Map to double if not null.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDoubleIfNotNull(final Throwables.ToDoubleFunction<? super T, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> flatMap(final Throwables.Function<? super T, Nullable<U>, E> mapper) throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         * Flat map if not null.
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> flatMapIfNotNull(final Throwables.Function<? super T, Nullable<U>, E> mapper)
                throws IllegalArgumentException, E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         *
         * @param valueToFind
         * @return
         */
        public boolean contains(final T valueToFind) {
            return isPresent() && N.equals(this.value, valueToFind);
        }

        /**
         *
         *
         * @param supplier
         * @return
         * @throws IllegalArgumentException
         */
        public Nullable<T> or(final Supplier<Nullable<? extends T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, "supplier");

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        /**
         * Or if null.
         *
         * @param supplier
         * @return
         * @throws IllegalArgumentException
         */
        public Nullable<T> orIfNull(final Supplier<Nullable<? extends T>> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, "supplier");

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
         *
         * @return
         */
        @Beta
        public T orElseNull() {
            return isPresent() ? value : null;
        }

        /**
         *
         * @param other
         * @return
         */
        public T orElse(T other) {
            return isPresent() ? value : other;
        }

        /**
         * Or else get.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
         */
        public T orElseGet(final Supplier<? extends T> other) throws IllegalArgumentException {
            N.checkArgNotNull(other, "other");

            if (isPresent()) {
                return value;
            } else {
                return other.get();
            }
        }

        /**
         * Or else if null.
         *
         * @param other
         * @return
         */
        public T orElseIfNull(T other) {
            return isNotNull() ? value : other;
        }

        /**
         * Or else get if null.
         *
         * @param other
         * @return
         * @throws IllegalArgumentException
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
         * Or else throw.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
         */
        public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        /**
         * Or else throw if null.
         *
         * @return
         * @throws NoSuchElementException the no such element exception
         */
        public T orElseThrowIfNull() throws NoSuchElementException {
            if (isNotNull()) {
                return value;
            } else {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NULL_ELEMENT_EX);
            }
        }

        /**
         * Or else throw.
         * @param errorMessage
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param param1
         * @param param2
         * @param param3
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw.
         * @param errorMessage
         * @param params
         *
         * @return
         * @throws NoSuchElementException the no such element exception
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
         * Or else throw if null.
         *
         * @param <E>
         * @param exceptionSupplier
         * @return
         * @throws IllegalArgumentException
         * @throws E
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
         *
         * @return
         */
        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        /**
         * Stream if not null.
         *
         * @return
         */
        public Stream<T> streamIfNotNull() {
            if (isNotNull()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        /**
         *
         * @return
         */
        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * To list if not null.
         *
         * @return
         */
        public List<T> toListIfNotNull() {
            if (isNotNull()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         *
         * @return
         */
        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To set if not null.
         *
         * @return
         */
        public Set<T> toSetIfNotNull() {
            if (isNotNull()) {
                return N.asSet(value);
            } else {
                return N.newHashSet();
            }
        }

        /**
         * To immutable list.
         *
         * @return
         */
        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * To immutable list if not null.
         *
         * @return
         */
        public ImmutableList<T> toImmutableListIfNotNull() {
            if (isNotNull()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        /**
         * To immutable set.
         *
         * @return
         */
        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * To immutable set if not null.
         *
         * @return
         */
        public ImmutableSet<T> toImmutableSetIfNotNull() {
            if (isNotNull()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        /**
         * Returns an empty {@code Optional} if it's empty or value is null.
         *
         * @return
         */
        public Optional<T> toOptional() {
            if (value == null) {
                return Optional.<T> empty();
            } else {
                return Optional.of(value);
            }
        }

        /**
         * Returns an empty {@code java.util.Optional} if it's empty or value is null.
         *
         * @return
         */
        public java.util.Optional<T> toJdkOptional() {
            if (value == null) {
                return java.util.Optional.<T> empty();
            } else {
                return java.util.Optional.of(value);
            }
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Nullable) {
                final Nullable<?> other = (Nullable<?>) obj;

                return N.equals(isPresent, other.isPresent) && N.equals(value, other.value);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        /**
         *
         * @return
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
