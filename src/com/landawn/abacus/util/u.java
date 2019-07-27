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
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

public class u {
    private u() {
        // utility class
    }

    public static final class Optional<T> {
        private static final Optional<?> EMPTY = new Optional<>();

        private final T value;

        private Optional() {
            this.value = null;
        }

        private Optional(T value) {
            this.value = N.checkArgNotNull(value);
        }

        public static <T> Optional<T> empty() {
            return (Optional<T>) EMPTY;
        }

        public static <T> Optional<T> of(T value) {
            return new Optional<>(value);
        }

        public static <T> Optional<T> ofNullable(T value) {
            if (value == null) {
                return empty();
            }

            return new Optional<>(value);
        }

        public static <T> Optional<T> from(java.util.Optional<T> op) {
            if (op.isPresent()) {
                return of(op.get());
            } else {
                return empty();
            }
        }

        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return value != null;
        }

        public boolean isEmpty() {
            return value == null;
        }

        /**
         * 
         * @param action
         * @return itself
         * @throws E
         */
        public <E extends Exception> Optional<T> ifPresent(Try.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * 
         * @param action
         * @param emptyAction
         * @return itself
         * @throws E
         * @throws E2
         */
        public <E extends Exception, E2 extends Exception> Optional<T> ifPresentOrElse(Try.Consumer<? super T, E> action, Try.Runnable<E2> emptyAction)
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

        public <E extends Exception> Optional<T> filter(Try.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <U, E extends Exception> Nullable<U> map(final Try.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.<U> of(mapper.apply(value));
            } else {
                return Nullable.<U> empty();
            }
        }

        public <E extends Exception> OptionalBoolean mapToBoolean(final Try.ToBooleanFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        public <E extends Exception> OptionalChar mapToChar(final Try.ToCharFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        public <E extends Exception> OptionalByte mapToByte(final Try.ToByteFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        public <E extends Exception> OptionalShort mapToShort(final Try.ToShortFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <E extends Exception> OptionalLong mapToLong(final Try.ToLongFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        public <E extends Exception> OptionalFloat mapToFloat(final Try.ToFloatFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDouble(final Try.ToDoubleFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <U, E extends Exception> Optional<U> flatMap(Try.Function<? super T, Optional<U>, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> Optional<T> or(Try.Supplier<Optional<? extends T>, E> supplier) throws E {
            N.checkArgNotNull(supplier, "supplier");

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull((Optional<T>) supplier.get());
            }
        }

        public T orNull() {
            return isPresent() ? value : null;
        }

        public T orElse(T other) {
            return isPresent() ? value : other;
        }

        public <E extends Exception> T orElseGet(Try.Supplier<? extends T, E> other) throws E {
            if (isPresent()) {
                return value;
            } else {
                return other.get();
            }
        }

        //    public T orElseNull() {
        //        return isPresent() ? value : null;
        //    }

        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException("No value is present");
            }
        }

        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return new HashSet<>();
            }
        }

        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.<T> empty();
            }
        }

        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.<T> empty();
            }
        }

        public java.util.Optional<T> __() {
            if (isPresent()) {
                return java.util.Optional.of(value);
            } else {
                return java.util.Optional.empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent()) {
                return String.format("Optional[%s]", N.toString(value));
            }

            return "Optional.empty";
        }
    }

    public static final class OptionalBoolean implements Comparable<OptionalBoolean> {
        private static final OptionalBoolean EMPTY = new OptionalBoolean();
        private static final OptionalBoolean TRUE = new OptionalBoolean(true);
        private static final OptionalBoolean FALSE = new OptionalBoolean(false);

        private final boolean value;
        private final boolean isPresent;

        private OptionalBoolean() {
            this.value = false;
            this.isPresent = false;
        }

        private OptionalBoolean(boolean value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalBoolean empty() {
            return EMPTY;
        }

        public static OptionalBoolean of(boolean value) {
            return value ? TRUE : FALSE;
        }

        public static OptionalBoolean ofNullable(Boolean val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        public boolean get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalBoolean ifPresent(Try.BooleanConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalBoolean ifPresentOrElse(Try.BooleanConsumer<E> action, Try.Runnable<E2> emptyAction)
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

        public <E extends Exception> OptionalBoolean filter(Try.BooleanPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalBoolean map(final Try.BooleanUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.BooleanFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalBoolean flatMap(Try.BooleanFunction<OptionalBoolean, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalBoolean or(Try.Supplier<OptionalBoolean, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public boolean orFalse() {
            return isPresent ? value : false;
        }

        //    public boolean orElseFalse() {
        //        return isPresent ? value : false;
        //    }

        public boolean orTrue() {
            return isPresent ? value : true;
        }

        //    public boolean orElseTrue() {
        //        return isPresent ? value : true;
        //    }

        public boolean orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public boolean orElse(boolean other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> boolean orElseGet(Try.BooleanSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsBoolean();
            }
        }

        public <X extends Throwable> boolean orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public Stream<Boolean> stream() {
            if (isPresent) {
                return Stream.of(value);
            } else {
                return Stream.empty();
            }
        }

        public Optional<Boolean> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Boolean> empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalBoolean[%s]", value);
            }

            return "OptionalBoolean.empty";
        }
    }

    public static final class OptionalChar implements Comparable<OptionalChar> {
        private static final OptionalChar EMPTY = new OptionalChar();
        private static final OptionalChar CHAR_0 = new OptionalChar(N.CHAR_0);

        private final char value;
        private final boolean isPresent;

        private OptionalChar() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalChar(char value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalChar empty() {
            return EMPTY;
        }

        public static OptionalChar of(char value) {
            return value == N.CHAR_0 ? CHAR_0 : new OptionalChar(value);
        }

        public static OptionalChar ofNullable(Character val) {
            if (val == null) {
                return empty();
            } else {
                return of(val);
            }
        }

        public char get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalChar ifPresent(Try.CharConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalChar ifPresentOrElse(Try.CharConsumer<E> action, Try.Runnable<E2> emptyAction) throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        public <E extends Exception> OptionalChar filter(Try.CharPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalChar map(final Try.CharUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<Character, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.CharFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalChar flatMap(Try.CharFunction<OptionalChar, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalChar or(Try.Supplier<OptionalChar, E> supplier) throws E {
            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public char orZero() {
            return isPresent() ? value : 0;
        }

        //    public char orElseZero() {
        //        return isPresent() ? value : 0;
        //    }

        public char orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public char orElse(char other) {
            return isPresent() ? value : other;
        }

        public <E extends Exception> char orElseGet(Try.CharSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent()) {
                return value;
            } else {
                return other.getAsChar();
            }
        }

        public <X extends Throwable> char orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public Optional<Character> boxed() {
            if (isPresent()) {
                return Optional.of(value);
            } else {
                return Optional.<Character> empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent()) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent()) {
                return String.format("OptionalChar[%s]", value);
            }

            return "OptionalChar.empty";
        }
    }

    public static final class OptionalByte implements Comparable<OptionalByte> {
        private static final OptionalByte EMPTY = new OptionalByte();
        private static final OptionalByte[] POOL = new OptionalByte[256];

        static {
            for (int i = 0; i < 256; i++) {
                POOL[i] = new OptionalByte((byte) (i - 128));
            }
        }

        private final byte value;
        private final boolean isPresent;

        private OptionalByte() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalByte(byte value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalByte empty() {
            return EMPTY;
        }

        public static OptionalByte of(byte value) {
            return POOL[value - Byte.MIN_VALUE];
        }

        public static OptionalByte ofNullable(Byte val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalByte.of(val);
            }
        }

        public byte get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalByte ifPresent(Try.ByteConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalByte ifPresentOrElse(Try.ByteConsumer<E> action, Try.Runnable<E2> emptyAction) throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        public <E extends Exception> OptionalByte filter(Try.BytePredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalByte map(final Try.ByteUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<Byte, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.ByteFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalByte flatMap(Try.ByteFunction<OptionalByte, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalByte or(Try.Supplier<OptionalByte, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public byte orZero() {
            return isPresent ? value : 0;
        }

        //    public byte orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public byte orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public byte orElse(byte other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> byte orElseGet(Try.ByteSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsByte();
            }
        }

        public <X extends Throwable> byte orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public Optional<Byte> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Byte> empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalByte[%s]", value);
            }

            return "OptionalByte.empty";
        }
    }

    public static final class OptionalShort implements Comparable<OptionalShort> {
        private static final OptionalShort EMPTY = new OptionalShort();

        private final short value;
        private final boolean isPresent;

        private OptionalShort() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalShort(short value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalShort empty() {
            return EMPTY;
        }

        public static OptionalShort of(short value) {
            return new OptionalShort(value);
        }

        public static OptionalShort ofNullable(Short val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalShort.of(val);
            }
        }

        public short get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalShort ifPresent(Try.ShortConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalShort ifPresentOrElse(Try.ShortConsumer<E> action, Try.Runnable<E2> emptyAction)
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

        public <E extends Exception> OptionalShort filter(Try.ShortPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalShort map(final Try.ShortUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<Short, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.ShortFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalShort flatMap(Try.ShortFunction<OptionalShort, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalShort or(Try.Supplier<OptionalShort, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public short orZero() {
            return isPresent ? value : 0;
        }

        //    public short orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public short orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public short orElse(short other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> short orElseGet(Try.ShortSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsShort();
            }
        }

        public <X extends Throwable> short orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public Optional<Short> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Short> empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalShort[%s]", value);
            }

            return "OptionalShort.empty";
        }
    }

    public static final class OptionalInt implements Comparable<OptionalInt> {
        private static final OptionalInt EMPTY = new OptionalInt();

        private static final int MIN_CACHED_VALUE = -128;
        private static final int MAX_CACHED_VALUE = 1025;

        private static final OptionalInt[] POOL = new OptionalInt[MAX_CACHED_VALUE - MIN_CACHED_VALUE];

        static {
            for (int i = 0, to = MAX_CACHED_VALUE - MIN_CACHED_VALUE; i < to; i++) {
                POOL[i] = new OptionalInt(i + MIN_CACHED_VALUE);
            }
        }

        private final int value;
        private final boolean isPresent;

        private OptionalInt() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalInt(int value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalInt empty() {
            return EMPTY;
        }

        public static OptionalInt of(int value) {
            return value >= MIN_CACHED_VALUE && value < MAX_CACHED_VALUE ? POOL[value - MIN_CACHED_VALUE] : new OptionalInt(value);
        }

        public static OptionalInt ofNullable(Integer val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalInt.of(val);
            }
        }

        public static OptionalInt from(java.util.OptionalInt op) {
            if (op.isPresent()) {
                return of(op.getAsInt());
            } else {
                return empty();
            }
        }

        public int get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalInt ifPresent(Try.IntConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalInt ifPresentOrElse(Try.IntConsumer<E> action, Try.Runnable<E2> emptyAction) throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        public <E extends Exception> OptionalInt filter(Try.IntPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt map(final Try.IntUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalLong mapToLong(final Try.ToLongFunction<Integer, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDouble(final Try.ToDoubleFunction<Integer, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.IntFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalInt flatMap(Try.IntFunction<OptionalInt, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt or(Try.Supplier<OptionalInt, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public int orZero() {
            return isPresent ? value : 0;
        }

        //    public int orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public int orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public int orElse(int other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> int orElseGet(Try.IntSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsInt();
            }
        }

        public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public IntStream stream() {
            if (isPresent) {
                return IntStream.of(value);
            } else {
                return IntStream.empty();
            }
        }

        public Optional<Integer> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Integer> empty();
            }
        }

        public java.util.OptionalInt __() {
            if (isPresent) {
                return java.util.OptionalInt.of(value);
            } else {
                return java.util.OptionalInt.empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalInt[%s]", value);
            }

            return "OptionalInt.empty";
        }
    }

    public static final class OptionalLong implements Comparable<OptionalLong> {
        private static final OptionalLong EMPTY = new OptionalLong();

        private final long value;
        private final boolean isPresent;

        private OptionalLong() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalLong(long value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalLong empty() {
            return EMPTY;
        }

        public static OptionalLong of(long value) {
            return new OptionalLong(value);
        }

        public static OptionalLong ofNullable(Long val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalLong.of(val);
            }
        }

        public static OptionalLong from(java.util.OptionalLong op) {
            if (op.isPresent()) {
                return of(op.getAsLong());
            } else {
                return empty();
            }
        }

        public long get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalLong ifPresent(Try.LongConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalLong ifPresentOrElse(Try.LongConsumer<E> action, Try.Runnable<E2> emptyAction) throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isPresent) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        public <E extends Exception> OptionalLong filter(Try.LongPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalLong map(final Try.LongUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<Long, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDouble(final Try.ToDoubleFunction<Long, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.LongFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalLong flatMap(Try.LongFunction<OptionalLong, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalLong or(Try.Supplier<OptionalLong, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public long orZero() {
            return isPresent ? value : 0;
        }

        //    public long orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public long orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public long orElse(long other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> long orElseGet(Try.LongSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsLong();
            }
        }

        public <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public LongStream stream() {
            if (isPresent) {
                return LongStream.of(value);
            } else {
                return LongStream.empty();
            }
        }

        public Optional<Long> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Long> empty();
            }
        }

        public java.util.OptionalLong __() {
            if (isPresent) {
                return java.util.OptionalLong.of(value);
            } else {
                return java.util.OptionalLong.empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalLong[%s]", value);
            }

            return "OptionalLong.empty";
        }
    }

    public static final class OptionalFloat implements Comparable<OptionalFloat> {
        private static final OptionalFloat EMPTY = new OptionalFloat();

        private final float value;
        private final boolean isPresent;

        private OptionalFloat() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalFloat(float value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalFloat empty() {
            return EMPTY;
        }

        public static OptionalFloat of(float value) {
            return new OptionalFloat(value);
        }

        public static OptionalFloat ofNullable(Float val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalFloat.of(val);
            }
        }

        public float get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalFloat ifPresent(Try.FloatConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalFloat ifPresentOrElse(Try.FloatConsumer<E> action, Try.Runnable<E2> emptyAction)
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

        public <E extends Exception> OptionalFloat filter(Try.FloatPredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalFloat map(final Try.FloatUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDouble(final Try.ToDoubleFunction<Float, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.FloatFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalFloat flatMap(Try.FloatFunction<OptionalFloat, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalFloat or(Try.Supplier<OptionalFloat, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public float orZero() {
            return isPresent ? value : 0;
        }

        //    public float orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public float orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public float orElse(float other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> float orElseGet(Try.FloatSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsFloat();
            }
        }

        public <X extends Throwable> float orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public Optional<Float> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Float> empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalFloat[%s]", value);
            }

            return "OptionalFloat.empty";
        }
    }

    public static final class OptionalDouble implements Comparable<OptionalDouble> {
        private static final OptionalDouble EMPTY = new OptionalDouble();

        private final double value;
        private final boolean isPresent;

        private OptionalDouble() {
            this.value = 0;
            this.isPresent = false;
        }

        private OptionalDouble(double value) {
            this.value = value;
            this.isPresent = true;
        }

        public static OptionalDouble empty() {
            return EMPTY;
        }

        public static OptionalDouble of(double value) {
            return new OptionalDouble(value);
        }

        public static OptionalDouble ofNullable(Double val) {
            if (val == null) {
                return empty();
            } else {
                return OptionalDouble.of(val);
            }
        }

        public static OptionalDouble from(java.util.OptionalDouble op) {
            if (op.isPresent()) {
                return of(op.getAsDouble());
            } else {
                return empty();
            }
        }

        public double get() throws NoSuchElementException {
            return orElseThrow();
        }

        public boolean isPresent() {
            return isPresent;
        }

        public <E extends Exception> OptionalDouble ifPresent(Try.DoubleConsumer<E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent) {
                action.accept(value);
            }

            return this;
        }

        public <E extends Exception, E2 extends Exception> OptionalDouble ifPresentOrElse(Try.DoubleConsumer<E> action, Try.Runnable<E2> emptyAction)
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

        public <E extends Exception> OptionalDouble filter(Try.DoublePredicate<E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalDouble map(final Try.DoubleUnaryOperator<E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<Double, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <E extends Exception> OptionalLong mapToLong(final Try.ToLongFunction<Double, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        public <T, E extends Exception> Nullable<T> mapToObj(final Try.DoubleFunction<T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Nullable.of(mapper.apply(value));
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> OptionalDouble flatMap(Try.DoubleFunction<OptionalDouble, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalDouble or(Try.Supplier<OptionalDouble, E> supplier) throws E {
            if (isPresent) {
                return this;
            } else {
                return Objects.requireNonNull(supplier.get());
            }
        }

        public double orZero() {
            return isPresent ? value : 0;
        }

        //    public double orElseZero() {
        //        return isPresent ? value : 0;
        //    }

        public double orElseThrow() throws NoSuchElementException {
            if (isPresent) {
                return value;
            } else {
                throw new NoSuchElementException("No value present");
            }
        }

        public double orElse(double other) {
            return isPresent ? value : other;
        }

        public <E extends Exception> double orElseGet(Try.DoubleSupplier<E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent) {
                return value;
            } else {
                return other.getAsDouble();
            }
        }

        public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

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

        public DoubleStream stream() {
            if (isPresent) {
                return DoubleStream.of(value);
            } else {
                return DoubleStream.empty();
            }
        }

        public Optional<Double> boxed() {
            if (isPresent) {
                return Optional.of(value);
            } else {
                return Optional.<Double> empty();
            }
        }

        public java.util.OptionalDouble __() {
            if (isPresent) {
                return java.util.OptionalDouble.of(value);
            } else {
                return java.util.OptionalDouble.empty();
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (isPresent) {
                return String.format("OptionalDouble[%s]", value);
            }

            return "OptionalDouble.empty";
        }
    }

    public static final class Nullable<T> {
        private static final Nullable<?> EMPTY = new Nullable<>();

        private final T value;
        private final boolean isPresent;

        private Nullable() {
            this.value = null;
            this.isPresent = false;
        }

        private Nullable(T value) {
            this.value = value;
            this.isPresent = true;
        }

        public static <T> Nullable<T> empty() {
            return (Nullable<T>) EMPTY;
        }

        public static <T> Nullable<T> of(T value) {
            return new Nullable<>(value);
        }

        public static <T> Nullable<T> from(Optional<T> optional) {
            if (optional.isPresent()) {
                return new Nullable<>(optional.get());
            } else {
                return Nullable.<T> empty();
            }
        }

        public static <T> Nullable<T> from(java.util.Optional<T> optional) {
            if (optional.isPresent()) {
                return new Nullable<>(optional.get());
            } else {
                return Nullable.<T> empty();
            }
        }

        public T get() throws NoSuchElementException {
            return orElseThrow();
        }

        /**
         * Returns {@code true} if the value is present, otherwise returns {@code false}.
         * 
         * @return
         */
        public boolean isPresent() {
            return isPresent;
        }

        /**
         * Returns {@code true} if the value is not present, otherwise returns {@code false}.
         * 
         * @return
         */
        public boolean isNotPresent() {
            return isPresent == false;
        }

        /**
         * Returns {@code true} if the value is not present, otherwise returns {@code false}.
         * 
         * @return
         * @deprecated replaced by {@link #isNotPresent()}
         */
        @Deprecated
        public boolean isEmpty() {
            return isPresent == false;
        }

        /**
         * Returns {@code true} if the value is not present, or it is present but it's {@code null}, otherwise returns {@code false}.
         * 
         * @return
         */
        public boolean isNull() {
            return value == null;
        }

        /**
         * Returns {@code true} if the value is present and it's not {@code null}, otherwise returns {@code false}.
         * 
         * @return
         */
        public boolean isNotNull() {
            return value != null;
        }

        /**
         * 
         * @param action
         * @return itself
         * @throws E
         */
        public <E extends Exception> Nullable<T> ifPresent(Try.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isPresent()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * 
         * @param action
         * @param emptyAction
         * @return itself
         * @throws E
         * @throws E2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifPresentOrElse(Try.Consumer<? super T, E> action, Try.Runnable<E2> emptyAction)
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
         * @param action
         * @return itself
         * @throws E
         */
        public <E extends Exception> Nullable<T> ifNotNull(Try.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }

            return this;
        }

        /**
         * 
         * @param action
         * @param emptyAction
         * @return itself
         * @throws E
         * @throws E2
         */
        public <E extends Exception, E2 extends Exception> Nullable<T> ifNotNullOrElse(Try.Consumer<? super T, E> action, Try.Runnable<E2> emptyAction)
                throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isNotNull()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }

            return this;
        }

        public <E extends Exception> Nullable<T> filter(Try.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isPresent() && predicate.test(value)) {
                return this;
            } else {
                return empty();
            }
        }

        public <E extends Exception> Optional<T> filterIfNotNull(Try.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isNotNull() && predicate.test(value)) {
                return Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        public <U, E extends Exception> Nullable<U> map(Try.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalBoolean mapToBoolean(final Try.ToBooleanFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        public <E extends Exception> OptionalChar mapToChar(final Try.ToCharFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        public <E extends Exception> OptionalByte mapToByte(final Try.ToByteFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        public <E extends Exception> OptionalShort mapToShort(final Try.ToShortFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        public <E extends Exception> OptionalInt mapToInt(final Try.ToIntFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <E extends Exception> OptionalLong mapToLong(final Try.ToLongFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        public <E extends Exception> OptionalFloat mapToFloat(final Try.ToFloatFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDouble(final Try.ToDoubleFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <U, E extends Exception> Nullable<U> mapIfNotNull(Try.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> OptionalBoolean mapToBooleanIfNotNull(final Try.ToBooleanFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalBoolean.of(mapper.applyAsBoolean(value));
            } else {
                return OptionalBoolean.empty();
            }
        }

        public <E extends Exception> OptionalChar mapToCharIfNotNull(final Try.ToCharFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalChar.of(mapper.applyAsChar(value));
            } else {
                return OptionalChar.empty();
            }
        }

        public <E extends Exception> OptionalByte mapToByteIfNotNull(final Try.ToByteFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalByte.of(mapper.applyAsByte(value));
            } else {
                return OptionalByte.empty();
            }
        }

        public <E extends Exception> OptionalShort mapToShortIfNotNull(final Try.ToShortFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalShort.of(mapper.applyAsShort(value));
            } else {
                return OptionalShort.empty();
            }
        }

        public <E extends Exception> OptionalInt mapToIntIfNotNull(final Try.ToIntFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalInt.of(mapper.applyAsInt(value));
            } else {
                return OptionalInt.empty();
            }
        }

        public <E extends Exception> OptionalLong mapToLongIfNotNull(final Try.ToLongFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalLong.of(mapper.applyAsLong(value));
            } else {
                return OptionalLong.empty();
            }
        }

        public <E extends Exception> OptionalFloat mapToFloatIfNotNull(final Try.ToFloatFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalFloat.of(mapper.applyAsFloat(value));
            } else {
                return OptionalFloat.empty();
            }
        }

        public <E extends Exception> OptionalDouble mapToDoubleIfNotNull(final Try.ToDoubleFunction<? super T, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return OptionalDouble.of(mapper.applyAsDouble(value));
            } else {
                return OptionalDouble.empty();
            }
        }

        public <U, E extends Exception> Nullable<U> flatMap(Try.Function<? super T, Nullable<U>, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isPresent()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <U, E extends Exception> Nullable<U> flatMapIfNotNull(Try.Function<? super T, Nullable<U>, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Objects.requireNonNull(mapper.apply(value));
            } else {
                return empty();
            }
        }

        public <E extends Exception> Nullable<T> or(Try.Supplier<Nullable<? extends T>, E> supplier) throws E {
            N.checkArgNotNull(supplier, "supplier");

            if (isPresent()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        public <E extends Exception> Nullable<T> orIfNull(Try.Supplier<Nullable<? extends T>, E> supplier) throws E {
            N.checkArgNotNull(supplier, "supplier");

            if (isNotNull()) {
                return this;
            } else {
                return Objects.requireNonNull((Nullable<T>) supplier.get());
            }
        }

        public T orNull() {
            return isPresent() ? value : null;
        }

        //    public T orElseNull() {
        //        return isPresent() ? value : null;
        //    }

        public T orElse(T other) {
            return isPresent() ? value : other;
        }

        public <E extends Exception> T orElseGet(Try.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isPresent()) {
                return value;
            } else {
                return other.get();
            }
        }

        public T orElseThrow() throws NoSuchElementException {
            if (isPresent()) {
                return value;
            } else {
                throw new NoSuchElementException("No value is present");
            }
        }

        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isPresent()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        public T orElseIfNull(T other) {
            return isNotNull() ? value : other;
        }

        public <E extends Exception> T orElseGetIfNull(Try.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isNotNull()) {
                return value;
            } else {
                return other.get();
            }
        }

        public T orElseThrowIfNull() throws NoSuchElementException {
            if (isNotNull()) {
                return value;
            } else {
                throw new NoSuchElementException("No value is present");
            }
        }

        public <X extends Throwable> T orElseThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isNotNull()) {
                return value;
            } else {
                throw exceptionSupplier.get();
            }
        }

        public Stream<T> stream() {
            if (isPresent()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        public Stream<T> streamIfNotNull() {
            if (isNotNull()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        public List<T> toList() {
            if (isPresent()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        public List<T> toListIfNotNull() {
            if (isNotNull()) {
                return N.asList(value);
            } else {
                return new ArrayList<>();
            }
        }

        public Set<T> toSet() {
            if (isPresent()) {
                return N.asSet(value);
            } else {
                return new HashSet<>();
            }
        }

        public Set<T> toSetIfNotNull() {
            if (isNotNull()) {
                return N.asSet(value);
            } else {
                return new HashSet<>();
            }
        }

        public ImmutableList<T> toImmutableList() {
            if (isPresent()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        public ImmutableList<T> toImmutableListIfNotNull() {
            if (isNotNull()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.empty();
            }
        }

        public ImmutableSet<T> toImmutableSet() {
            if (isPresent()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        public ImmutableSet<T> toImmutableSetIfNotNull() {
            if (isNotNull()) {
                return ImmutableSet.of(value);
            } else {
                return ImmutableSet.empty();
            }
        }

        public Optional<T> toOptional() {
            if (value == null) {
                return Optional.<T> empty();
            } else {
                return Optional.of(value);
            }
        }

        public java.util.Optional<T> toJdkOptional() {
            if (value == null) {
                return java.util.Optional.<T> empty();
            } else {
                return java.util.Optional.of(value);
            }
        }

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

        @Override
        public int hashCode() {
            return N.hashCode(isPresent) * 31 + N.hashCode(value);
        }

        @Override
        public String toString() {
            if (value == null) {
                return isPresent ? "Nullable[null]" : "Nullable.empty";
            } else {
                return String.format("Nullable[%s]", N.toString(value));
            }
        }
    }

    public static final class Holder<T> extends Reference<T, Holder<T>> {
        public Holder() {
            this(null);
        }

        Holder(T value) {
            super(value);
        }

        public static <T> Holder<T> of(T value) {
            return new Holder<>(value);
        }
    }

    public static final class R<T> extends Reference<T, R<T>> {
        public R() {
            this(null);
        }

        R(T value) {
            super(value);
        }

        public static <T> R<T> of(T value) {
            return new R<>(value);
        }
    }

    static abstract class Reference<T, H extends Reference<T, H>> {
        private T value;

        protected Reference() {
            this(null);
        }

        protected Reference(T value) {
            this.value = value;
        }

        public T value() {
            return value;
        }

        /**
         * 
         * @return
         * @deprecated replace by {@link #value()}.
         */
        @Deprecated
        public T getValue() {
            return value;
        }

        public void setValue(final T value) {
            this.value = value;
        }

        public T getAndSet(final T value) {
            final T result = this.value;
            this.value = value;
            return result;
        }

        public T setAndGet(final T value) {
            this.value = value;
            return this.value;
        }

        public final <E extends Exception> T getAndUpdate(Try.UnaryOperator<T, E> updateFunction) throws E {
            final T res = value;
            this.value = updateFunction.apply(value);
            return res;
        }

        public final <E extends Exception> T updateAndGet(Try.UnaryOperator<T, E> updateFunction) throws E {
            this.value = updateFunction.apply(value);
            return value;
        }

        /**
         * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
         * Otherwise just return <code>false</code> without setting the value to new value.
         * 
         * @param newValue
         * @param predicate - test the current value.
         * @return
         */
        public <E extends Exception> boolean setIf(final T newValue, final Try.Predicate<? super T, E> predicate) throws E {
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
         * @param newValue
         * @param predicate the first parameter is the current value, the second parameter is the new value.
         * @return
         */
        public <E extends Exception> boolean setIf(final T newValue, final Try.BiPredicate<? super T, ? super T, E> predicate) throws E {
            if (predicate.test(value, newValue)) {
                this.value = newValue;
                return true;
            }

            return false;
        }

        public boolean isNull() {
            return value == null;
        }

        public boolean isNotNull() {
            return value != null;
        }

        public <E extends Exception> void ifNotNull(Try.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }
        }

        public <E extends Exception, E2 extends Exception> void ifNotNullOrElse(Try.Consumer<? super T, E> action, Try.Runnable<E2> emptyAction) throws E, E2 {
            N.checkArgNotNull(action, "action");
            N.checkArgNotNull(emptyAction, "emptyAction");

            if (isNotNull()) {
                action.accept(value);
            } else {
                emptyAction.run();
            }
        }

        public <E extends Exception> void accept(final Try.Consumer<? super T, E> action) throws E {
            action.accept(value);
        }

        @Deprecated
        public <E extends Exception> void acceptIfNotNull(final Try.Consumer<? super T, E> action) throws E {
            N.checkArgNotNull(action, "action");

            if (isNotNull()) {
                action.accept(value);
            }
        }

        public <U, E extends Exception> U map(final Try.Function<? super T, ? extends U, E> mapper) throws E {
            return mapper.apply(value);
        }

        public <U, E extends Exception> Nullable<U> mapIfNotNull(final Try.Function<? super T, ? extends U, E> mapper) throws E {
            N.checkArgNotNull(mapper, "mapper");

            if (isNotNull()) {
                return Nullable.of((U) mapper.apply(value));
            } else {
                return Nullable.<U> empty();
            }
        }

        public <E extends Exception> Nullable<T> filter(final Try.Predicate<? super T, E> predicate) throws E {
            if (predicate.test(value)) {
                return Nullable.of(value);
            } else {
                return Nullable.<T> empty();
            }
        }

        public <E extends Exception> Optional<T> filterIfNotNull(final Try.Predicate<? super T, E> predicate) throws E {
            N.checkArgNotNull(predicate, "predicate");

            if (isNotNull() && predicate.test(value)) {
                return Optional.of(value);
            } else {
                return Optional.<T> empty();
            }
        }

        public Stream<T> stream() {
            return Stream.of(value);
        }

        public Stream<T> streamIfNotNull() {
            if (isNotNull()) {
                return Stream.of(value);
            } else {
                return Stream.<T> empty();
            }
        }

        public T orElseIfNull(T other) {
            return isNotNull() ? value : other;
        }

        public <E extends Exception> T orElseGetIfNull(Try.Supplier<? extends T, E> other) throws E {
            N.checkArgNotNull(other, "other");

            if (isNotNull()) {
                return value;
            } else {
                return other.get();
            }
        }

        public <X extends Throwable> T orElseThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
            N.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

            if (isNotNull()) {
                return value;
            } else {
                throw exceptionSupplier.get();
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

        @Override
        public int hashCode() {
            return (value == null) ? 0 : value.hashCode();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(final Object obj) {
            return this == obj || (obj instanceof Reference && N.equals(((Reference) obj).value, value));
        }

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
