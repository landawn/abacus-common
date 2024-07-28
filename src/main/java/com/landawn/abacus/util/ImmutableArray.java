/*
 * Copyright (C) 2020 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.function.Consumer;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.stream.Stream;

@com.landawn.abacus.annotation.Immutable
public final class ImmutableArray<T> implements Iterable<T>, Immutable {
    private final T[] elements;
    private final int length;

    ImmutableArray(final T[] elements) {
        this.elements = elements == null ? (T[]) N.EMPTY_OBJECT_ARRAY : elements;
        this.length = N.len(this.elements);
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1) {
        return new ImmutableArray<>(N.asArray(e1));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2) {
        return new ImmutableArray<>(N.asArray(e1, e2));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6));
    }

    /**
     *
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @param e9
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @param e9
     * @param e10
     * @return
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9,
            final T e10) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     *
     *
     * @param <T>
     * @param elements
     * @return
     */
    public static <T> ImmutableArray<T> copyOf(final T[] elements) {
        return new ImmutableArray<>(elements == null ? null : elements.clone());
    }

    /**
     *
     * @param <T>
     * @param elements
     * @return an {@code ImmutableArray} backed by the specified {@code elements}
     * @deprecated the ImmutableArray may be modified through the specified {@code elements}
     */
    @Deprecated
    public static <T> ImmutableArray<T> wrap(final T[] elements) {
        return new ImmutableArray<>(elements);
    }

    /**
     *
     *
     * @return
     */
    public int length() {
        return length;
    }

    /**
     *
     *
     * @return
     */
    public boolean isEmpty() {
        return length == 0;
    }

    /**
     *
     *
     * @param index
     * @return
     */
    public T get(int index) {
        return elements[index];
    }

    /**
     *
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(T valueToFind) {
        return N.indexOf(elements, valueToFind);
    }

    /**
     *
     *
     * @param valueToFind
     * @return
     */
    public int lastIndexOf(T valueToFind) {
        return N.lastIndexOf(elements, valueToFind);
    }

    /**
     *
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final T valueToFind) {
        return N.contains(elements, valueToFind);
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public ImmutableArray<T> copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, length);

        return new ImmutableArray<>(N.copyOfRange(elements, fromIndex, toIndex));
    }

    /**
     *
     *
     * @return
     */
    public ImmutableList<T> asList() {
        return ImmutableList.wrap(N.asList(elements));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public ObjIterator<T> iterator() {
        return ObjIterator.of(elements);
    }

    /**
     *
     *
     * @return
     */
    public Stream<T> stream() {
        return Stream.of(elements);
    }

    /**
     *
     * @param consumer
     * @throws IllegalArgumentException
     */
    @Override
    public void forEach(final Consumer<? super T> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param consumer
     * @throws IllegalArgumentException
     * @throws E
     */
    @Beta
    public <E extends Exception> void foreach(final Throwables.Consumer<? super T, E> consumer) throws IllegalArgumentException, E { // NOSONAR
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param consumer
     * @throws IllegalArgumentException
     * @throws E
     */
    @Beta
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> consumer) throws IllegalArgumentException, E { // NOSONAR
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(i, elements[i]);
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return N.hashCode(elements) * 31;
    }

    /**
     *
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ImmutableArray && N.equals(this.elements, ((ImmutableArray<T>) obj).elements);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return N.toString(elements);
    }
}
