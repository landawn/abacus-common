/*
 * Copyright (c) 2022, Haiyang Li.
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

import java.util.function.Supplier;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Class Holder.
 *
 * @param <T>
 */
public final class Holder<T> implements Mutable {

    private T value;

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
        this.value = value;
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
    public <E extends Exception> T getAndUpdate(final Throwables.UnaryOperator<T, E> updateFunction) throws E { // NOSONAR
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
    public <E extends Exception> T updateAndGet(final Throwables.UnaryOperator<T, E> updateFunction) throws E { // NOSONAR
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
     * @return
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
     * @return
     * @throws E the e
     * @deprecated
     */
    @Deprecated
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
    public <E extends Exception> void ifNotNull(final Throwables.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action, "action"); //NOSONAR

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
    public <E extends Exception, E2 extends Exception> void ifNotNullOrElse(final Throwables.Consumer<? super T, E> action,
            final Throwables.Runnable<E2> emptyAction) throws E, E2 {
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
     * @deprecated replaced by {@link #ifNotNull(Throwables.Consumer)}
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
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E
     */
    public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");

        if (isNotNull()) {
            return Optional.of((U) mapper.apply(value));
        } else {
            return Optional.<U> empty();
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
    public <E extends Exception> T orElseGetIfNull(final Throwables.Supplier<? extends T, E> other) throws E {
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
    public <X extends Throwable> T orElseThrowIfNull(final Supplier<? extends X> exceptionSupplier) throws X {
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
     * Returns a {@code Stream} with the {@code value} if {@code value} is not null, otherwise an empty {@code Stream} is returned.
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
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Holder && N.equals(((Holder) obj).value, value));
    }

    /**
     *
     * @return
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
