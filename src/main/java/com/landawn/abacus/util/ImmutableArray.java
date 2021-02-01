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
    public final int length;

    ImmutableArray(final T[] elements) {
        this.elements = elements == null ? (T[]) N.EMPTY_OBJECT_ARRAY : elements;
        this.length = N.len(this.elements);
    }

    public static <T> ImmutableArray<T> of(final T[] elements) {
        return new ImmutableArray<>(elements);
    }

    public static <T> ImmutableArray<T> copyOf(final T[] elements) {
        return new ImmutableArray<>(elements == null ? null : elements.clone());
    }

    public T get(int index) {
        return elements[index];
    }

    public <E extends Exception> void forEach(final Throwables.Consumer<T, E> consumer) throws E {
        N.checkArgNotNull(consumer, "consumer");

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    public Stream<T> stream() {
        return Stream.of(elements);
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
