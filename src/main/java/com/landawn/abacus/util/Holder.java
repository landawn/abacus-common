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

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

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
    Holder(final T value) {
        this.value = value;
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> Holder<T> of(final T value) {
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
        value = updateFunction.apply(value);
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
        value = updateFunction.apply(value);
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
            value = newValue;
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
            value = newValue;
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
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Exception> void ifNotNull(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
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
     * @throws IllegalArgumentException
     * @throws E the e
     * @throws E2 the e2
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
     *
     * @param <E>
     * @param action
     * @throws E the e
     * @deprecated
     */
    @Deprecated
    public <E extends Exception> void accept(final Throwables.Consumer<? super T, E> action) throws E {
        action.accept(value);
    }

    /**
     * Accept if not null.
     *
     * @param <E>
     * @param action
     * @throws IllegalArgumentException
     * @throws E the e
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
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <U, E extends Exception> Nullable<U> mapIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper) throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, cs.mapper);

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
     * @throws IllegalArgumentException
     * @throws E
     */
    public <U, E extends Exception> Optional<U> mapToNonNullIfNotNull(final Throwables.Function<? super T, ? extends U, E> mapper)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, cs.mapper);

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
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Exception> Optional<T> filterIfNotNull(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate, cs.Predicate);

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
    public T orElseIfNull(final T other) {
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
        N.checkArgNotNull(other, cs.other);

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
