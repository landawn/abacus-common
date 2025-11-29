/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * An immutable wrapper class that provides custom hashCode and equals implementations for wrapped objects.
 * This is particularly useful for using arrays or other objects with non-standard equality semantics
 * as keys in HashMaps or elements in HashSets.
 * 
 * <p>Once a Wrapper object is stored in a {@code Set} or used as a key in a {@code Map}, 
 * the wrapped object should not be modified, as this would change its hash code and equals behavior,
 * leading to undefined behavior in the collection.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Special handling for arrays with deep equality and hash code computation</li>
 *   <li>Support for custom hash and equals functions</li>
 *   <li>Object pooling for zero-length arrays to reduce memory allocation</li>
 *   <li>Immutable design to ensure collection safety</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Wrapping arrays for use as map keys
 * int[] array1 = {1, 2, 3};
 * int[] array2 = {1, 2, 3};
 * 
 * Map<Wrapper<int[]>, String> map = new HashMap<>();
 * map.put(Wrapper.of(array1), "value");
 * String result = map.get(Wrapper.of(array2));  // Returns "value"
 * 
 * // Custom wrapper with specific hash/equals logic
 * Person person = new Person("John", 30);
 * Wrapper<Person> wrapper = Wrapper.of(person,
 *     p -> p.getName().hashCode(),
 *     (p1, p2) -> p1.getName().equals(p2.getName())
 * );
 * }</pre>
 *
 * @param <T> The type of the object that this wrapper will hold
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
     * Creates a new Wrapper instance for the given array with deep equality semantics.
     * This method automatically detects arrays and applies appropriate deep hash code
     * and deep equals implementations. Zero-length arrays are cached for efficiency.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] nums = {1, 2, 3};
     * Wrapper<int[]> wrapper = Wrapper.of(nums);
     * 
     * // Works with multi-dimensional arrays
     * String[][] matrix = {{"a", "b"}, {"c", "d"}};
     * Wrapper<String[][]> matrixWrapper = Wrapper.of(matrix);
     * }</pre>
     *
     * @param <T> The type of the array elements
     * @param array The array to be wrapped (must be an array type)
     * @return A Wrapper instance for the given array
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
     * Creates a new Wrapper instance with custom hash and equals functions.
     * This method is useful when the wrapped value's natural hashCode and equals methods
     * are not suitable for use in collections.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Wrap a Person object but only consider the ID for equality
     * Person person = new Person(123, "John", 30);
     * Wrapper<Person> wrapper = Wrapper.of(person,
     *     p -> p.getId(),                    // Hash by ID
     *     (p1, p2) -> p1.getId() == p2.getId() // Compare by ID
     * );
     * }</pre>
     *
     * @param <T> The type of the value to be wrapped
     * @param value The value to be wrapped
     * @param hashFunction The function to calculate the hash code of the wrapped value
     * @param equalsFunction The function to compare the wrapped value with other objects
     * @return A Wrapper instance for the given value
     * @throws IllegalArgumentException if the hashFunction or equalsFunction is {@code null}
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);

        return new AnyWrapper<>(value, hashFunction, equalsFunction, defaultToStringFunction);
    }

    /**
     * Creates a new Wrapper instance with custom hash, equals, and toString functions.
     * This provides complete control over how the wrapped object behaves in collections
     * and how it's represented as a string.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ComplexObject obj = new ComplexObject();
     * Wrapper<ComplexObject> wrapper = Wrapper.of(obj,
     *     o -> o.computeHash(),           // Custom hash
     *     (o1, o2) -> o1.isEquivalent(o2), // Custom equals
     *     o -> o.toDisplayString()         // Custom toString
     * );
     * }</pre>
     *
     * @param <T> The type of the value to be wrapped
     * @param value The value to be wrapped
     * @param hashFunction The function to calculate the hash code of the wrapped value
     * @param equalsFunction The function to compare the wrapped value with other objects
     * @param toStringFunction The function to generate string representation of the wrapped value
     * @return A Wrapper instance for the given value
     * @throws IllegalArgumentException if any of the function parameters is {@code null}
     */
    public static <T> Wrapper<T> of(final T value, final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction,
            final Function<? super T, String> toStringFunction) throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction, cs.hashFunction);
        N.checkArgNotNull(equalsFunction, cs.equalsFunction);
        N.checkArgNotNull(toStringFunction, cs.toStringFunction);

        return new AnyWrapper<>(value, hashFunction, equalsFunction, toStringFunction);
    }

    /**
     * Returns the wrapped value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = {1, 2, 3};
     * Wrapper<int[]> wrapper = Wrapper.of(array);
     * int[] unwrapped = wrapper.value();  // Returns the original array
     * }</pre>
     *
     * @return the wrapped value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the hash code of the wrapped value.
     * The implementation depends on how the wrapper was created:
     * <ul>
     *   <li>For arrays: uses deep hash code computation via {@link N#deepHashCode(Object)}</li>
     *   <li>For custom wrappers: uses the provided hash function</li>
     * </ul>
     *
     * <p>This method ensures consistent hash codes for wrapped objects, making them
     * suitable for use in hash-based collections. For arrays, the hash code is computed
     * based on the contents rather than the array reference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * Wrapper<int[]> w1 = Wrapper.of(arr1);
     * Wrapper<int[]> w2 = Wrapper.of(arr2);
     * // Both wrappers will have the same hash code
     * assert w1.hashCode() == w2.hashCode();
     * }</pre>
     *
     * @return the hash code of the wrapped value
     */
    @Override
    public abstract int hashCode();

    /**
     * Compares this wrapper with another object for equality.
     * Two wrappers are equal if they wrap equal values according to:
     * <ul>
     *   <li>For arrays: deep equality comparison via {@link N#deepEquals(Object, Object)}</li>
     *   <li>For custom wrappers: the provided equals function</li>
     * </ul>
     *
     * <p>This method provides value-based equality instead of reference equality,
     * making wrapped objects behave correctly in collections. For arrays, the comparison
     * is performed element-by-element, including nested arrays.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * Wrapper<int[]> w1 = Wrapper.of(arr1);
     * Wrapper<int[]> w2 = Wrapper.of(arr2);
     * // Returns true despite different array references
     * assert w1.equals(w2);
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public abstract boolean equals(final Object obj);

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
            return (obj == this) || (obj instanceof Wrapper && equalsFunction.test(value, ((Wrapper<T>) obj).value));
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
            return (obj == this) || (obj instanceof Wrapper && arrayEqualsFunction.test(value, ((Wrapper<T>) obj).value));
        }

        @Override
        public String toString() {
            return String.format("Wrapper[%s]", defaultToStringFunction.apply(value));
        }
    }
}
