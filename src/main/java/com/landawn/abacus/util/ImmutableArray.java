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

import com.landawn.abacus.util.stream.Stream;

@com.landawn.abacus.annotation.Immutable
public final class ImmutableArray<T> implements Immutable {
    private final T[] elements;
    private final int length;

    ImmutableArray(final T[] elements) {
        this.elements = elements == null ? (T[]) N.EMPTY_OBJECT_ARRAY : elements;
        this.length = N.len(this.elements);
    }

    public static <T> ImmutableArray<T> of(final T e1) {
        return new ImmutableArray<>(N.asArray(e1));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2) {
        return new ImmutableArray<>(N.asArray(e1, e2));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6));
    }

    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7));
    }

    public static <T> ImmutableArray<T> copyOf(final T[] elements) {
        return new ImmutableArray<>(elements == null ? null : elements.clone());
    }

    public int length() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public T get(int index) {
        return elements[index];
    }

    public int indexOf(T valueToFind) {
        return N.indexOf(elements, valueToFind);
    }

    public int lastIndexOf(T valueToFind) {
        return N.lastIndexOf(elements, valueToFind);
    }

    public boolean contains(T valueToFind) {
        return N.contains(elements, valueToFind);
    }

    public <E extends Exception> void forEach(final Throwables.Consumer<T, E> consumer) throws E {
        N.checkArgNotNull(consumer, "consumer");

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    public <E extends Exception> void forEachIndexed(final Throwables.IndexedConsumer<T, E> consumer) throws E {
        N.checkArgNotNull(consumer, "consumer");

        for (int i = 0; i < length; i++) {
            consumer.accept(i, elements[i]);
        }
    }

    public Stream<T> stream() {
        return Stream.of(elements);
    }

    public ImmutableArray<T> copy(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, length);

        return new ImmutableArray<>(N.copyOfRange(elements, fromIndex, toIndex));
    }

    @SuppressWarnings("deprecation")
    public ImmutableList<T> asList() {
        return ImmutableList.of(N.asList(elements));
    }

    @Override
    public int hashCode() {
        return N.hashCode(elements);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ImmutableArray && N.equals(this.elements, ((ImmutableArray<T>) obj).elements);
    }

    @Override
    public String toString() {
        return N.toString(elements);
    }
}
