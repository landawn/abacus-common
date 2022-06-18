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

import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToIntFunction;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 * @see Keyed
 */
@com.landawn.abacus.annotation.Immutable
public final class Wrapper<T> implements Immutable {

    static final ToIntFunction<Object> arrayHashFunction = value -> N.deepHashCode(value);

    static final BiPredicate<Object, Object> arrayEqualsFunction = (t, u) -> N.deepEquals(t, u);

    private final T value;

    private final ToIntFunction<? super T> hashFunction;

    private final BiPredicate<? super T, ? super T> equalsFunction;

    private final Function<? super T, String> toStringFunction;

    private int hashCode;

    private Wrapper(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction) {
        this(value, hashFunction, equalsFunction, null);
    }

    private Wrapper(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction,
            Function<? super T, String> toStringFunction) {
        this.value = value;
        this.hashFunction = hashFunction;
        this.equalsFunction = equalsFunction;
        this.toStringFunction = toStringFunction;

    }

    @SuppressWarnings("rawtypes")
    private static final Wrapper WRAPPER_FOR_NULL_ARRAY = new Wrapper<>(null, arrayHashFunction, arrayEqualsFunction);

    @SuppressWarnings("rawtypes")
    private static final Map<Object, Wrapper> arrayWapperPool = new ConcurrentHashMap<>();

    static {
        arrayWapperPool.put(boolean.class, new Wrapper<>(new boolean[0], arrayHashFunction, arrayEqualsFunction));
    }

    /**
     *
     * @param <T>
     * @param array
     * @return
     */
    public static <T> Wrapper<T> of(T array) {
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
     * @param <T>
     * @param value
     * @param hashFunction
     * @param equalsFunction
     * @return
     */
    public static <T> Wrapper<T> of(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction) {
        N.checkArgNotNull(hashFunction, "hashFunction");
        N.checkArgNotNull(equalsFunction, "equalsFunction");

        return new Wrapper<>(value, hashFunction, equalsFunction);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param hashFunction
     * @param equalsFunction
     * @param toStringFunction
     * @return
     */
    public static <T> Wrapper<T> of(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction,
            Function<? super T, String> toStringFunction) {
        N.checkArgNotNull(hashFunction, "hashFunction");
        N.checkArgNotNull(equalsFunction, "equalsFunction");
        N.checkArgNotNull(toStringFunction, "toStringFunction");

        return new Wrapper<>(value, hashFunction, equalsFunction, toStringFunction);
    }

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
    public boolean equals(Object obj) {
        return (obj == this) || (obj instanceof Wrapper && equalsFunction.test(((Wrapper<T>) obj).value, value));
    }

    @Override
    public String toString() {
        if (toStringFunction == null) {
            if (value == null) {
                return "Wrapper[null]";
            } else {
                return String.format("Wrapper[%s]", N.toString(value));
            }
        } else {
            return toStringFunction.apply(value);
        }
    }
}
