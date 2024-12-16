/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Once the object is stored in a {@code Set} or {@code Map}, it should not be modified, otherwise, the behavior is undefined.
 *
 * @param <T> The type of the object that this wrapper will hold.
 *
 * @param <T>
 * @see Keyed
 * @see IndexedKeyed
 */
@com.landawn.abacus.annotation.Immutable
public abstract class Wrapper<T> implements Immutable {

    static final ToIntFunction<Object> arrayHashFunction = N::deepHashCode;

    static final BiPredicate<Object, Object> arrayEqualsFunction = N::deepEquals;

    static final Function<Object, String> defaultToStringFunction = N::toString;

    final T value;

    Wrapper(final T value) {
        this.value = value;
    }

    /**
     * Returns a new instance of Wrapper for the given array.
     *
     * @param <T> The type of the array elements.
     * @param array The array to be wrapped.
     * @return A Wrapper instance for the given array.
     */
    public static <T> Wrapper<T> of(final T array) {
        if (array == null) {
            return ArrayWrapper.WRAPPER_FOR_NULL_ARRAY;
        }

        Wrapper<T> result = null;

        if (array.getClass().isArray() && java.lang.reflect.Array.getLength(array) == 0) {
            result = ArrayWrapper.WRAPPER_POOL.get(array.getClass().getComponentType());

            if (result == null) {
                result = new ArrayWrapper<>(array);
                ArrayWrapper.WRAPPER_POOL.put(array.getClass().getComponentType(), result);
            }

            return result;
        }

        // return new Wrapper<T>(checkArray(array), arrayHashFunction, arrayEqualsFunction);
        return new ArrayWrapper<>(array);
    }

    /**
     * Creates a new instance of Wrapper for the given value, using the provided hash function and equals function.
     * The hash function is used to calculate the hash code of the wrapped value, and the equals function is used to compare the wrapped value with other objects.
     * This method is useful when the wrapped value's natural hash code and equals methods are not suitable. For example: array.
     *
     * @param <T> The type of the value to be wrapped.
     * @param value The value to be wrapped.
     * @param hashFunction The function to calculate the hash code of the wrapped value.
     * @param equalsFunction The function to compare the wrapped value with other objects.
     * @return A Wrapper instance for the given value.
     * @throws IllegalArgumentException if the hashFunction or equalsFunction is {@code null}.
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);

        return new AnyWrapper<>(value, hashFunction, equalsFunction, defaultToStringFunction);
    }

    /**
     * Creates a new instance of Wrapper for the given value, using the provided hash function and equals function.
     * The hash function is used to calculate the hash code of the wrapped value, and the equals function is used to compare the wrapped value with other objects.
     * This method is useful when the wrapped value's natural hash code and equals methods are not suitable. For example: array.
     *
     * @param <T> The type of the value to be wrapped.
     * @param value The value to be wrapped.
     * @param hashFunction The function to calculate the hash code of the wrapped value.
     * @param equalsFunction The function to compare the wrapped value with other objects.
     * @return A Wrapper instance for the given value.
     * @throws IllegalArgumentException if the hashFunction or equalsFunction or toStringFunction is {@code null}.
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction,
            final Function<? super T, String> toStringFunction) throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);
        N.checkArgNotNull(toStringFunction, cs.toStringFunction);

        return new AnyWrapper<>(value, hashFunction, equalsFunction, toStringFunction);
    }

    public T value() {
        return value;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(final Object obj);

    //    static <T> T checkArray(T a) {
    //        if (a != null && a.getClass().isArray() == false) {
    //            throw new IllegalArgumentException(a.getClass().getCanonicalName() + " is not array type");
    //        }
    //
    //        return a;
    //    }

    static final class AnyWrapper<T> extends Wrapper<T> {

        private final ToIntFunction<? super T> hashFunction;
        private final BiPredicate<? super T, ? super T> equalsFunction;
        private final Function<? super T, String> toStringFunction;

        AnyWrapper(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction) {
            this(value, hashFunction, equalsFunction, defaultToStringFunction);
        }

        AnyWrapper(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction,
                final Function<? super T, String> toStringFunction) {
            super(value);
            this.hashFunction = hashFunction;
            this.equalsFunction = equalsFunction;
            this.toStringFunction = toStringFunction;
        }

        @Override
        public int hashCode() {
            return hashFunction.applyAsInt(value);
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj == this) || (obj instanceof Wrapper && equalsFunction.test(((Wrapper<T>) obj).value, value));
        }

        @Override
        public String toString() {
            return String.format("Wrapper[%s]", toStringFunction.apply(value));
        }
    }

    static final class ArrayWrapper<T> extends Wrapper<T> {

        @SuppressWarnings("rawtypes")
        static final Wrapper WRAPPER_FOR_NULL_ARRAY = new ArrayWrapper<>(null);

        @SuppressWarnings("rawtypes")
        static final Map<Object, Wrapper> WRAPPER_POOL = new ConcurrentHashMap<>();

        static {
            WRAPPER_POOL.put(boolean.class, new ArrayWrapper<>(new boolean[0]));
        }

        ArrayWrapper(final T value) {
            super(value);
        }

        @Override
        public int hashCode() {
            return arrayHashFunction.applyAsInt(value);
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj == this) || (obj instanceof Wrapper && arrayEqualsFunction.test(((Wrapper<Object[]>) obj).value, value));
        }

        @Override
        public String toString() {
            return String.format("Wrapper[%s]", defaultToStringFunction.apply(value));
        }
    }
}
