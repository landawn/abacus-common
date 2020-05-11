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

import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ShortStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Class u.
 */
public class u {

    /**
     * Instantiates a new u.
     */
    private u() {
        // utility class
    }

    /**
     * The Class Optional.
     *
     * @param <T>
     */
    public static final class Optional<T> {

        /** Presents {@code Boolean.TRUE}. */
        public static final Optional<Boolean> TRUE = new Optional<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Optional<Boolean> FALSE = new Optional<>(Boolean.FALSE);

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
         */
        private Optional(T value) {
            this.value = N.checkArgNotNull(value);
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
         * @param <T>
         * @param value
         * @return
         */
        public static <T> Optional<T> of(T value) {
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
         * @param <E>
         * @param action
         * @return itself
         * @throws E the e
         */
        public <E extends Exception> Optional<T> ifPresent(Throwables.Consumer<? super T, E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Optional<T> ifPresentOrElse(Throwables.Consumer<? super T, E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Optional<T> filter(Throwables.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> map(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.<U> of(mapper.apply(value));
            } else {
                return Nullable.<U> empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> flatMap(Throwables.Function<? super T, Optional<U>, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public boolean contains(final T element) {
            return isPresent() && N.equals(this.value, element);
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> Optional<T> or(Throwables.Supplier<Optional<? extends T>, E> supplier) throws E {
            N.checkArgNotNull(supplier, "supplier");

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull((Optional<T>) supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public T orNull() {
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> T orElseGet(Throwables.Supplier<? extends T, E> other) throws E {
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
                throw new NoSuchElementException("No value is present");
            }
        }

        /**
         * Or else throw.
         *
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public java.util.Optional<T> __() {
            if (isPresent()) {
                return java.util.Optional.of(value);
            } else {
                return java.util.Optional.empty();
            }
        }

        /**
         *
         * @param obj
         * @return true, if successful
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
    public static final class OptionalBoolean implements Comparable<OptionalBoolean> {

        /** Presents {@code true}. */
        public static final OptionalBoolean TRUE = new OptionalBoolean(Boolean.TRUE);

        /** Presents {@code true}. */
        public static final OptionalBoolean FALSE = new OptionalBoolean(Boolean.FALSE);

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
         * @return true, if successful
         * @throws NoSuchElementException the no such element exception
         */
        public boolean get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean ifPresent(Throwables.BooleanConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalBoolean ifPresentOrElse(Throwables.BooleanConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean filter(Throwables.BooleanPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean map(final Throwables.BooleanUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to obj.
         *
         * @param <T>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.BooleanFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.BooleanFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean flatMap(Throwables.BooleanFunction<OptionalBoolean, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean or(Throwables.Supplier<OptionalBoolean, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return true, if successful
         */
        public boolean orFalse() {
            return isPresent ? value : false;
        }

        //    public boolean orElseFalse() {
        //        return isPresent ? value : false;
        //    }

        /**
         *
         * @return true, if successful
         */
        public boolean orTrue() {
            return isPresent ? value : true;
        }

        //    public boolean orElseTrue() {
        //        return isPresent ? value : true;
        //    }

        /**
         * Or else throw.
         *
         * @return true, if successful
         * @throws NoSuchElementException the no such element exception
         */
        public boolean orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        /**
         *
         * @param other
         * @return true, if successful
         */
        public boolean orElse(boolean other) {
            return isPresent ? value : other;
        }

        /**
         * Or else get.
         *
         * @param <E>
         * @param other
         * @return true, if successful
         * @throws E the e
         */
        public <E extends Exception> boolean orElseGet(Throwables.BooleanSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return true, if successful
         * @throws X the x
         */
        public <X extends Throwable> boolean orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public int compareTo(OptionalBoolean optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Boolean.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalBoolean) {
                final OptionalBoolean other = (OptionalBoolean) obj;

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
    public static final class OptionalChar implements Comparable<OptionalChar> {

        /** The Constant EMPTY. */
        private static final OptionalChar EMPTY = new OptionalChar();

        /** The Constant CHAR_0. */
        private static final OptionalChar CHAR_0 = new OptionalChar(N.CHAR_0);

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
            return value == N.CHAR_0 ? CHAR_0 : new OptionalChar(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalChar ifPresent(Throwables.CharConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalChar ifPresentOrElse(Throwables.CharConsumer<E> action, Throwables.Runnable<E2> emptyAction)
                throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalChar filter(Throwables.CharPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalChar map(final Throwables.CharUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Character, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.CharFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.CharFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalChar flatMap(Throwables.CharFunction<OptionalChar, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalChar or(Throwables.Supplier<OptionalChar, E> supplier) throws E {
            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public char orZero() {
            return isPresent() ? value : 0;
        }

        //    public char orElseZero() {
        //        return isPresent() ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> char orElseGet(Throwables.CharSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> char orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public int compareTo(OptionalChar optional) {
            if (optional == null || optional.isPresent() == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Character.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalChar) {
                final OptionalChar other = (OptionalChar) obj;

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
    public static final class OptionalByte implements Comparable<OptionalByte> {

        /** The Constant EMPTY. */
        private static final OptionalByte EMPTY = new OptionalByte();

        /** The Constant POOL. */
        private static final OptionalByte[] POOL = new OptionalByte[256];

        static {
            for (int i = 0; i < 256; i++) {
                POOL[i] = new OptionalByte((byte) (i - 128));
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
            return POOL[value - Byte.MIN_VALUE];
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalByte ifPresent(Throwables.ByteConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalByte ifPresentOrElse(Throwables.ByteConsumer<E> action, Throwables.Runnable<E2> emptyAction)
                throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalByte filter(Throwables.BytePredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalByte map(final Throwables.ByteUnaryOperator<E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Byte, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.ByteFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.ByteFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalByte flatMap(Throwables.ByteFunction<OptionalByte, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalByte or(Throwables.Supplier<OptionalByte, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public byte orZero() {
            return isPresent ? value : 0;
        }

        //    public byte orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> byte orElseGet(Throwables.ByteSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> byte orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public int compareTo(OptionalByte optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Byte.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalByte) {
                final OptionalByte other = (OptionalByte) obj;

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
    public static final class OptionalShort implements Comparable<OptionalShort> {

        /** The Constant EMPTY. */
        private static final OptionalShort EMPTY = new OptionalShort();

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
            return new OptionalShort(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalShort ifPresent(Throwables.ShortConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalShort ifPresentOrElse(Throwables.ShortConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalShort filter(Throwables.ShortPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalShort map(final Throwables.ShortUnaryOperator<E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Short, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.ShortFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.ShortFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalShort flatMap(Throwables.ShortFunction<OptionalShort, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalShort or(Throwables.Supplier<OptionalShort, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public short orZero() {
            return isPresent ? value : 0;
        }

        //    public short orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> short orElseGet(Throwables.ShortSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> short orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public int compareTo(OptionalShort optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Short.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalShort) {
                final OptionalShort other = (OptionalShort) obj;

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
    public static final class OptionalInt implements Comparable<OptionalInt> {

        /** The Constant EMPTY. */
        private static final OptionalInt EMPTY = new OptionalInt();

        /** The Constant MIN_CACHED_VALUE. */
        private static final int MIN_CACHED_VALUE = -128;

        /** The Constant MAX_CACHED_VALUE. */
        private static final int MAX_CACHED_VALUE = 1025;

        /** The Constant POOL. */
        private static final OptionalInt[] POOL = new OptionalInt[MAX_CACHED_VALUE - MIN_CACHED_VALUE];

        static {
            for (int i = 0, to = MAX_CACHED_VALUE - MIN_CACHED_VALUE; i < to; i++) {
                POOL[i] = new OptionalInt(i + MIN_CACHED_VALUE);
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
            return value >= MIN_CACHED_VALUE && value < MAX_CACHED_VALUE ? POOL[value - MIN_CACHED_VALUE] : new OptionalInt(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalInt ifPresent(Throwables.IntConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalInt ifPresentOrElse(Throwables.IntConsumer<E> action, Throwables.Runnable<E2> emptyAction)
                throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalInt filter(Throwables.IntPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalInt map(final Throwables.IntUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to long.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Integer, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Integer, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.IntFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.IntFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalInt flatMap(Throwables.IntFunction<OptionalInt, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalInt or(Throwables.Supplier<OptionalInt, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public int orZero() {
            return isPresent ? value : 0;
        }

        //    public int orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> int orElseGet(Throwables.IntSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public java.util.OptionalInt __() {
            if (isPresent) {
                return java.util.OptionalInt.of(value);
            } else {
                return java.util.OptionalInt.empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(OptionalInt optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Integer.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalInt) {
                final OptionalInt other = (OptionalInt) obj;

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
    public static final class OptionalLong implements Comparable<OptionalLong> {

        /** The Constant EMPTY. */
        private static final OptionalLong EMPTY = new OptionalLong();

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
            return new OptionalLong(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong ifPresent(Throwables.LongConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalLong ifPresentOrElse(Throwables.LongConsumer<E> action, Throwables.Runnable<E2> emptyAction)
                throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong filter(Throwables.LongPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong map(final Throwables.LongUnaryOperator<E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Long, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Long, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.LongFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.LongFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong flatMap(Throwables.LongFunction<OptionalLong, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalLong or(Throwables.Supplier<OptionalLong, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public long orZero() {
            return isPresent ? value : 0;
        }

        //    public long orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> long orElseGet(Throwables.LongSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public java.util.OptionalLong __() {
            if (isPresent) {
                return java.util.OptionalLong.of(value);
            } else {
                return java.util.OptionalLong.empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(OptionalLong optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Long.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalLong) {
                final OptionalLong other = (OptionalLong) obj;

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
    public static final class OptionalFloat implements Comparable<OptionalFloat> {

        /** The Constant EMPTY. */
        private static final OptionalFloat EMPTY = new OptionalFloat();

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
            return new OptionalFloat(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat ifPresent(Throwables.FloatConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalFloat ifPresentOrElse(Throwables.FloatConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat filter(Throwables.FloatPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat map(final Throwables.FloatUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return empty();
            }
        }

        /**
         * Map to double.
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<Float, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.FloatFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.FloatFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat flatMap(Throwables.FloatFunction<OptionalFloat, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat or(Throwables.Supplier<OptionalFloat, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public float orZero() {
            return isPresent ? value : 0;
        }

        //    public float orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> float orElseGet(Throwables.FloatSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> float orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public int compareTo(OptionalFloat optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Float.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalFloat) {
                final OptionalFloat other = (OptionalFloat) obj;

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
    public static final class OptionalDouble implements Comparable<OptionalDouble> {

        /** The Constant EMPTY. */
        private static final OptionalDouble EMPTY = new OptionalDouble();

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
            return new OptionalDouble(value);
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

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isEmpty() {
            return !isPresent;
        }

        /**
         *
         * @param <E>
         * @param action
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble ifPresent(Throwables.DoubleConsumer<E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> OptionalDouble ifPresentOrElse(Throwables.DoubleConsumer<E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble filter(Throwables.DoublePredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble map(final Throwables.DoubleUnaryOperator<E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<Double, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<Double, E> mapper) throws E {
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
         * @throws E the e
         */
        public <T, E extends Exception> Nullable<T> mapToObj(final Throwables.DoubleFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <T, E extends Exception> Optional<T> mapToNonNull(final Throwables.DoubleFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Optional.of(mapper.apply(value));
            } else {
                return Optional.<T> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble flatMap(Throwables.DoubleFunction<OptionalDouble, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble or(Throwables.Supplier<OptionalDouble, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public double orZero() {
            return isPresent ? value : 0;
        }

        //    public double orElseZero() {
        //        return isPresent ? value : 0;
        //    }

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
                throw new NoSuchElementException("No value present");
            }
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> double orElseGet(Throwables.DoubleSupplier<E> other) throws E {
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
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        public java.util.OptionalDouble __() {
            if (isPresent) {
                return java.util.OptionalDouble.of(value);
            } else {
                return java.util.OptionalDouble.empty();
            }
        }

        /**
         *
         * @param optional
         * @return
         */
        @Override
        public int compareTo(OptionalDouble optional) {
            if (optional == null || optional.isPresent == false) {
                return isPresent ? 1 : 0;
            }

            if (isPresent == false) {
                return -1;
            }

            return Double.compare(this.get(), optional.get());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OptionalDouble) {
                final OptionalDouble other = (OptionalDouble) obj;

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
    public static final class Nullable<T> {

        /** Presents {@code Boolean.TRUE}. */
        public static final Nullable<Boolean> TRUE = new Nullable<>(Boolean.TRUE);

        /** Presents {@code Boolean.FALSE}. */
        public static final Nullable<Boolean> FALSE = new Nullable<>(Boolean.FALSE);

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
         * @param <T>
         * @param value
         * @return
         */
        public static <T> Nullable<T> of(T value) {
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
            return isPresent == false;
        }

        /**
         * Returns {@code true} if the value is not present, otherwise returns {@code false}.
         *
         * @return true, if is empty
         * @deprecated replaced by {@link #isNotPresent()}
         */
        @Deprecated
        public boolean isEmpty() {
            return isPresent == false;
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
         * @param <E>
         * @param action
         * @return itself
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> ifPresent(Throwables.Consumer<? super T, E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifPresentOrElse(Throwables.Consumer<? super T, E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> ifNotNull(Throwables.Consumer<? super T, E> action) throws E {
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
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifNotNullOrElse(Throwables.Consumer<? super T, E> action,
                Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> filter(Throwables.Predicate<? super T, E> predicate) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> Optional<T> filterIfNotNull(Throwables.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isNotNull() && predicate.test(value)) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> map(Throwables.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNull(Throwables.Function<? super T, ? extends U, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> mapIfNotNull(Throwables.Function<? super T, ? extends U, E> mapper) throws E {
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
         * @throws E the e
         */
        public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(Throwables.Function<? super T, ? extends U, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalBoolean mapToBooleanIfNotNull(final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalChar mapToCharIfNotNull(final Throwables.ToCharFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalByte mapToByteIfNotNull(final Throwables.ToByteFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalShort mapToShortIfNotNull(final Throwables.ToShortFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalInt mapToIntIfNotNull(final Throwables.ToIntFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalLong mapToLongIfNotNull(final Throwables.ToLongFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalFloat mapToFloatIfNotNull(final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
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
         * @throws E the e
         */
        public <E extends Exception> OptionalDouble mapToDoubleIfNotNull(final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> flatMap(Throwables.Function<? super T, Nullable<U>, E> mapper) throws E {
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
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> flatMapIfNotNull(Throwables.Function<? super T, Nullable<U>, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public boolean contains(final T element) {
            return isPresent() && N.equals(this.value, element);
        }

        /**
         *
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> or(Throwables.Supplier<Nullable<? extends T>, E> supplier) throws E {
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
         * @param <E>
         * @param supplier
         * @return
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> orIfNull(Throwables.Supplier<Nullable<? extends T>, E> supplier) throws E {
            N.checkArgNotNull(supplier, "supplier");

            if (isNotNull()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        /**
         *
         * @return
         */
        public T orNull() {
            return isPresent() ? value : null;
        }

        //    public T orElseNull() {
        //        return isPresent() ? value : null;
        //    }

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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> T orElseGet(Throwables.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent()) {
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
                throw new NoSuchElementException("No value is present");
            }
        }

        /**
         * Or else throw.
         *
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> T orElseGetIfNull(Throwables.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isNotNull()) {
                return value;
            } else {
                return other.get();
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
                throw new NoSuchElementException("No value is present");
            }
        }

        /**
         * Or else throw if null.
         *
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> T orElseThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
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
         * To jdk optional.
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
         * @return true, if successful
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

    /**
     * The Class Holder.
     *
     * @param <T>
     */
    public static final class Holder<T> extends Reference<T, Holder<T>> {

        /**
         * Instantiates a new holder.
         */
        public Holder() {
            this(null);
        }

        /**
         * Instantiates a new holder.
         *
         * @param value
         */
        Holder(T value) {
            super(value);
        }

        /**
         *
         * @param <T>
         * @param value
         * @return
         */
        public static <T> Holder<T> of(T value) {
            return new Holder<>(value);
        }
    }

    /**
     * The Class R.
     *
     * @param <T>
     */
    public static final class R<T> extends Reference<T, R<T>> {

        /**
         * Instantiates a new r.
         */
        public R() {
            this(null);
        }

        /**
         * Instantiates a new r.
         *
         * @param value
         */
        R(T value) {
            super(value);
        }

        /**
         *
         * @param <T>
         * @param value
         * @return
         */
        public static <T> R<T> of(T value) {
            return new R<>(value);
        }
    }

    /**
     * The Class Reference.
     *
     * @param <T>
     * @param <H>
     */
    static abstract class Reference<T, H extends Reference<T, H>> {

        /** The value. */
        private T value;

        /**
         * Instantiates a new reference.
         */
        protected Reference() {
            this(null);
        }

        /**
         * Instantiates a new reference.
         *
         * @param value
         */
        protected Reference(T value) {
            this.value = value;
        }

        /**
         *
         * @return
         */
        public T value() {
            return value;
        }

        /**
         * Gets the value.
         *
         * @return
         * @deprecated replace by {@link #value()}.
         */
        @Deprecated
        public T getValue() {
            return value;
        }

        /**
         * Sets the value.
         *
         * @param value the new value
         */
        public void setValue(final T value) {
            this.value = value;
        }

        /**
         * Gets the and set.
         *
         * @param value
         * @return
         */
        public T getAndSet(final T value) {
            final T result = this.value;
            this.value = value;
            return result;
        }

        /**
         * Sets the and get.
         *
         * @param value
         * @return
         */
        public T setAndGet(final T value) {
            this.value = value;
            return this.value;
        }

        /**
         * Gets the and update.
         *
         * @param <E>
         * @param updateFunction
         * @return
         * @throws E the e
         */
        public final <E extends Exception> T getAndUpdate(Throwables.UnaryOperator<T, E> updateFunction) throws E {
            final T res = value;
            this.value = updateFunction.apply(value);
            return res;
        }

        /**
         * Update and get.
         *
         * @param <E>
         * @param updateFunction
         * @return
         * @throws E the e
         */
        public final <E extends Exception> T updateAndGet(Throwables.UnaryOperator<T, E> updateFunction) throws E {
            this.value = updateFunction.apply(value);
            return value;
        }

        /**
         * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
         * Otherwise just return <code>false</code> without setting the value to new value.
         *
         * @param <E>
         * @param newValue
         * @param predicate - test the current value.
         * @return true, if successful
         * @throws E the e
         */
        public <E extends Exception> boolean setIf(final T newValue, final Throwables.Predicate<? super T, E> predicate) throws E {
            if (predicate.test(value)) {
                this.value = newValue;
                return true;
            }

            return false;
        }

        /**
         * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
         * Otherwise just return <code>false</code> without setting the value to new value.
         *
         * @param <E>
         * @param newValue
         * @param predicate the first parameter is the current value, the second parameter is the new value.
         * @return true, if successful
         * @throws E the e
         */
        public <E extends Exception> boolean setIf(final T newValue, final Throwables.BiPredicate<? super T, ? super T, E> predicate) throws E {
            if (predicate.test(value, newValue)) {
                this.value = newValue;
                return true;
            }

            return false;
        }

        /**
         * Checks if is null.
         *
         * @return true, if is null
         */
        public boolean isNull() {
            return value == null;
        }

        /**
         * Checks if is not null.
         *
         * @return true, if is not null
         */
        public boolean isNotNull() {
            return value != null;
        }

        /**
         * If not null.
         *
         * @param <E>
         * @param action
         * @throws E the e
         */
        public <E extends Exception> void ifNotNull(Throwables.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }
        }

        /**
         * If not null or else.
         *
         * @param <E>
         * @param <E2>
         * @param action
         * @param emptyAction
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> void ifNotNullOrElse(Throwables.Consumer<? super T, E> action, Throwables.Runnable<E2> emptyAction)
                throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isNotNull()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }
        }

        /**
         *
         * @param <E>
         * @param action
         * @throws E the e
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super T, E> action) throws E {
            action.accept(value);
        }

        /**
         * Accept if not null.
         *
         * @param <E>
         * @param action
         * @throws E the e
         * @deprecated replaced by {@link #ifNotNull(com.landawn.abacus.util.Throwables.Consumer)}
         */
        @Deprecated
        public <E extends Exception> void acceptIfNotNull(final Throwables.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }
        }

        /**
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> U map(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
            return mapper.apply(value);
        }

        /**
         * Map if not null.
         *
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return Nullable.<U> empty();
            }
        }

        /**
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Nullable<T> filter(final Throwables.Predicate<? super T, E> predicate) throws E {
            if (predicate.test(value)) {
                return Nullable.of(value);
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         * Filter if not null.
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Optional<T> filterIfNotNull(final Throwables.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isNotNull() && predicate.test(value)) {
                return Optional.of(value);
            } else {
                return Optional.<T> empty();
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
         * @param <E>
         * @param other
         * @return
         * @throws E the e
         */
        public <E extends Exception> T orElseGetIfNull(Throwables.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isNotNull()) {
                return value;
            } else {
                return other.get();
            }
        }

        /**
         * Or else throw if null.
         *
         * @param <X>
         * @param exceptionSupplier
         * @return
         * @throws X the x
         */
        public <X extends Throwable> T orElseThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
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
            return Stream.of(value);
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
         * Returns a non-empty {@code Nullable} with the {@code value}.
         *
         * @return
         */
        public Nullable<T> toNullable() {
            return Nullable.of(value);
        }

        /**
         * Returns an {@code Optional} with the {@code value} if {@code value} is not null, otherwise an empty {@code Optional} is returned.
         *
         * @return
         */
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return (value == null) ? 0 : value.hashCode();
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(final Object obj) {
            return this == obj || (obj instanceof Reference && N.equals(((Reference) obj).value, value));
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            if (value == null) {
                return "Reference[null]";
            } else {
                return String.format("Reference[%s]", N.toString(value));
            }
        }
    }

    //    public static final class t extends u {
    //        private t() {
    //            // utility class
    //        }
    //    }
    //
    //    public static final class m extends u {
    //        private m() {
    //            // utility class
    //        }
    //    }
}
