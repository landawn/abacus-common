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

import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToIntFunction;

// TODO: Auto-generated Javadoc
/**
 * The Class Wrapper.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public final class Wrapper<T> {

    /** The Constant arrayHashFunction. */
    static final ToIntFunction<Object> arrayHashFunction = new ToIntFunction<Object>() {
        @Override
        public int applyAsInt(Object value) {
            return N.deepHashCode(value);
        }
    };

    /** The Constant arrayEqualsFunction. */
    static final BiPredicate<Object, Object> arrayEqualsFunction = new BiPredicate<Object, Object>() {
        @Override
        public boolean test(Object t, Object u) {
            return N.deepEquals(t, u);
        }
    };

    /** The value. */
    private final T value;

    /** The hash function. */
    private final ToIntFunction<? super T> hashFunction;

    /** The equals function. */
    private final BiPredicate<? super T, ? super T> equalsFunction;

    /** The to string function. */
    private final Function<? super T, String> toStringFunction;

    /** The hash code. */
    private int hashCode;

    /**
     * Instantiates a new wrapper.
     *
     * @param value the value
     * @param hashFunction the hash function
     * @param equalsFunction the equals function
     */
    private Wrapper(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction) {
        this(value, hashFunction, equalsFunction, null);
    }

    /**
     * Instantiates a new wrapper.
     *
     * @param value the value
     * @param hashFunction the hash function
     * @param equalsFunction the equals function
     * @param toStringFunction the to string function
     */
    private Wrapper(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction,
            Function<? super T, String> toStringFunction) {
        this.value = value;
        this.hashFunction = hashFunction;
        this.equalsFunction = equalsFunction;
        this.toStringFunction = toStringFunction;

    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param array the array
     * @return the wrapper
     */
    public static <T> Wrapper<T> of(T array) {
        // return new Wrapper<T>(checkArray(array), arrayHashFunction, arrayEqualsFunction);
        return new Wrapper<T>(array, arrayHashFunction, arrayEqualsFunction);
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param value the value
     * @param hashFunction the hash function
     * @param equalsFunction the equals function
     * @return the wrapper
     */
    public static <T> Wrapper<T> of(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction) {
        N.checkArgNotNull(hashFunction, "hashFunction");
        N.checkArgNotNull(equalsFunction, "equalsFunction");

        return new Wrapper<T>(value, hashFunction, equalsFunction);
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param value the value
     * @param hashFunction the hash function
     * @param equalsFunction the equals function
     * @param toStringFunction the to string function
     * @return the wrapper
     */
    public static <T> Wrapper<T> of(T value, ToIntFunction<? super T> hashFunction, BiPredicate<? super T, ? super T> equalsFunction,
            Function<? super T, String> toStringFunction) {
        N.checkArgNotNull(hashFunction, "hashFunction");
        N.checkArgNotNull(equalsFunction, "equalsFunction");
        N.checkArgNotNull(toStringFunction, "toStringFunction");

        return new Wrapper<T>(value, hashFunction, equalsFunction, toStringFunction);
    }

    /**
     * Value.
     *
     * @return the t
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
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = value == null ? 0 : hashFunction.applyAsInt(value);
        }

        return hashCode;
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return (obj == this) || (obj instanceof Wrapper && equalsFunction.test(((Wrapper<T>) obj).value, value));
    }

    /**
     * To string.
     *
     * @return the string
     */
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
