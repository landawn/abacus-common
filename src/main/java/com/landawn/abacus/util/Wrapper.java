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
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 * @see Keyed
 * @see IndexedKeyed
 */
@com.landawn.abacus.annotation.Immutable
public final class Wrapper<T> implements Immutable {

    static final ToIntFunction<Object> arrayHashFunction = N::deepHashCode;

    static final BiPredicate<Object, Object> arrayEqualsFunction = N::deepEquals;

    static final Function<Object, String> defaultToStringFunction = N::toString;

    @SuppressWarnings("rawtypes")
    private static final Wrapper WRAPPER_FOR_NULL_ARRAY = new Wrapper<>(null, arrayHashFunction, arrayEqualsFunction);

    @SuppressWarnings("rawtypes")
    private static final Map<Object, Wrapper> arrayWapperPool = new ConcurrentHashMap<>();

    static {
        arrayWapperPool.put(boolean.class, new Wrapper<>(new boolean[0], arrayHashFunction, arrayEqualsFunction));
    }

    private final T value;

    private final ToIntFunction<? super T> hashFunction;

    private final BiPredicate<? super T, ? super T> equalsFunction;

    private final Function<? super T, String> toStringFunction;

    private int hashCode;

    private Wrapper(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction) {
        this(value, hashFunction, equalsFunction, defaultToStringFunction);
    }

    private Wrapper(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction,
            final Function<? super T, String> toStringFunction) {
        this.value = value;
        this.hashFunction = hashFunction;
        this.equalsFunction = equalsFunction;
        this.toStringFunction = toStringFunction;
    }

    /**
     *
     * @param <T>
     * @param array
     * @return
     */
    public static <T> Wrapper<T> of(final T array) {
        if (array == null) {
            return WRAPPER_FOR_NULL_ARRAY;
        }

        Wrapper<T> result = null;

        if (array.getClass().isArray() && java.lang.reflect.Array.getLength(array) == 0) {
            result = arrayWapperPool.get(array.getClass().getComponentType());

            if (result == null) {
                result = new Wrapper<>(array, arrayHashFunction, arrayEqualsFunction);
                arrayWapperPool.put(array.getClass().getComponentType(), result);
            }

            return result;
        }

        // return new Wrapper<T>(checkArray(array), arrayHashFunction, arrayEqualsFunction);
        return new Wrapper<>(array, arrayHashFunction, arrayEqualsFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param value
     * @param hashFunction
     * @param equalsFunction
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);

        return new Wrapper<>(value, hashFunction, equalsFunction, defaultToStringFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param value
     * @param hashFunction
     * @param equalsFunction
     * @param toStringFunction
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction,
            final Function<? super T, String> toStringFunction) throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);
        N.checkArgNotNull(toStringFunction, cs.toStringFunction);

        return new Wrapper<>(value, hashFunction, equalsFunction, toStringFunction);
    }

    /**
     *
     *
     * @return
     */
    public T value() {
        return value;
    }

    //    static <T> T checkArray(T a) {
    //        if (a != null && a.getClass().isArray() == false) {
    //            throw new IllegalArgumentException(a.getClass().getCanonicalName() + " is not array type");
    //        }
    //
    //        return a;
    //    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = value == null ? 0 : hashFunction.applyAsInt(value);
        }

        return hashCode;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj == this) || (obj instanceof Wrapper && equalsFunction.test(((Wrapper<T>) obj).value, value));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("Wrapper[%s]", toStringFunction.apply(value));
    }
}
