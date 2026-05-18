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
 * String result = map.get(Wrapper.of(array2));   // Returns "value"
 *
 * // Custom wrapper with specific hash/equals logic
 * Person person = new Person("John", 30);
 * Wrapper<Person> wrapper = Wrapper.of(person,
 *     p -> p.getName().hashCode(),
 *     (p1, p2) -> p1.getName().equals(p2.getName())
 * );
 * }</pre>
 *
 * @param <T> the type of the object that this wrapper will hold.
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
     * Creates a new {@code Wrapper} instance for the given value using deep equality semantics
     * (via {@link N#deepHashCode(Object)} and {@link N#deepEquals(Object, Object)}).
     * When {@code array} is an actual array type, this yields content-based hash/equals behaviour
     * suitable for using arrays as {@code Map} keys or {@code Set} elements.
     * Non-array values are also accepted; they will be compared with the same deep-equality functions.
     *
     * <p>Zero-length arrays of the same component type share a single cached {@code Wrapper}
     * instance to reduce memory allocation. {@code null} is also cached as a single instance.</p>
     *
     * <p>The wrapped value should not be mutated after wrapping; doing so may change its hash code
     * and break collection invariants.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using primitive arrays as map keys
     * int[] nums = {1, 2, 3};
     * Wrapper<int[]> wrapper = Wrapper.of(nums);
     *
     * Map<Wrapper<int[]>, String> map = new HashMap<>();
     * map.put(wrapper, "value");
     *
     * // Works with multi-dimensional arrays
     * String[][] matrix = {{"a", "b"}, {"c", "d"}};
     * Wrapper<String[][]> matrixWrapper = Wrapper.of(matrix);
     *
     * // Zero-length arrays share a cached instance
     * Wrapper<int[]> empty1 = Wrapper.of(new int[0]);
     * Wrapper<int[]> empty2 = Wrapper.of(new int[0]);
     * // empty1 == empty2
     * }</pre>
     *
     * @param <T> the type of the value to be wrapped.
     * @param array the value to be wrapped; may be any array type (primitive or object arrays),
     *              any non-array object, or {@code null}.
     * @return a {@code Wrapper} instance for the given value with deep equality semantics.
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
     * are not suitable for use in collections, or when you need to define custom equality
     * semantics based on specific fields or computed values.
     *
     * <p>The custom functions allow you to control exactly how the wrapped object behaves
     * in hash-based collections, enabling use cases like comparing objects by specific fields,
     * ignoring certain properties, or using custom comparison logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Wrap a Person object but only consider the ID for equality
     * Person person = new Person(123, "John", 30);
     * Wrapper<Person> wrapper = Wrapper.of(person,
     *     p -> p.getId(),                      // Hash by ID
     *     (p1, p2) -> p1.getId() == p2.getId() // Compare by ID
     * );
     *
     * // Use in a Set - only unique IDs allowed
     * Set<Wrapper<Person>> uniquePersons = new HashSet<>();
     * uniquePersons.add(wrapper);
     *
     * // Case-insensitive string wrapper
     * String text = "Hello";
     * Wrapper<String> caseInsensitive = Wrapper.of(text,
     *     s -> s.toLowerCase().hashCode(),
     *     (s1, s2) -> s1.equalsIgnoreCase(s2)
     * );
     * }</pre>
     *
     * @param <T> the type of the value to be wrapped.
     * @param value the value to be wrapped; can be {@code null}.
     * @param hashFunction the function to calculate the hash code of the wrapped value;
     *                     must not be {@code null}.
     * @param equalsFunction the function to compare the wrapped value with other objects;
     *                       must not be {@code null}.
     * @return a Wrapper instance with the specified custom hash and equals behavior.
     * @throws IllegalArgumentException if {@code hashFunction} or {@code equalsFunction} is {@code null}.
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
     * <p>This is the most flexible factory method, allowing you to customize all three aspects
     * of the wrapper's behavior: hash code computation, equality comparison, and string representation.
     * This is particularly useful for complex objects where you need fine-grained control over
     * comparison logic and debugging output.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complete custom behavior for a complex object
     * ComplexObject obj = new ComplexObject(123, "data", Arrays.asList(1, 2, 3));
     * Wrapper<ComplexObject> wrapper = Wrapper.of(obj,
     *     o -> o.computeCustomHash(),        // Custom hash function
     *     (o1, o2) -> o1.isEquivalent(o2),   // Custom equals logic
     *     o -> o.toDisplayString()           // Custom toString for debugging
     * );
     *
     * // Use in collections with custom display
     * Map<Wrapper<ComplexObject>, String> map = new HashMap<>();
     * map.put(wrapper, "value");
     * System.out.println(wrapper);  // Uses custom toString
     *
     * // Wrapper for records/tuples with specific formatting
     * Tuple<String, Integer> tuple = new Tuple<>("key", 100);
     * Wrapper<Tuple<String, Integer>> tupleWrapper = Wrapper.of(tuple,
     *     t -> Objects.hash(t.first, t.second),
     *     (t1, t2) -> Objects.equals(t1.first, t2.first) && Objects.equals(t1.second, t2.second),
     *     t -> String.format("(%s: %d)", t.first, t.second)
     * );
     * }</pre>
     *
     * @param <T> the type of the value to be wrapped.
     * @param value the value to be wrapped; can be {@code null}.
     * @param hashFunction the function to calculate the hash code of the wrapped value;
     *                     must not be {@code null}.
     * @param equalsFunction the function to compare the wrapped value with other objects;
     *                       must not be {@code null}.
     * @param toStringFunction the function to generate string representation of the wrapped value;
     *                         must not be {@code null}.
     * @return a Wrapper instance with the specified custom hash, equals, and toString behavior.
     * @throws IllegalArgumentException if any of the function parameters ({@code hashFunction},
     *                                  {@code equalsFunction}, or {@code toStringFunction}) is {@code null}.
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
     * <p>The returned value is the same instance that was passed to the {@code of()} factory
     * method — no copy is made. If the returned value is mutable, mutating it after the wrapper
     * has been placed into a hash-based collection may corrupt the collection's invariants.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retrieve the wrapped array
     * int[] array = {1, 2, 3};
     * Wrapper<int[]> wrapper = Wrapper.of(array);
     * int[] unwrapped = wrapper.value();   // Returns the original array
     *
     * // Access wrapped object in a map
     * Map<Wrapper<String[]>, String> map = new HashMap<>();
     * String[] key = {"a", "b"};
     * map.put(Wrapper.of(key), "value");
     *
     * for (Wrapper<String[]> wrappedKey : map.keySet()) {
     *     String[] originalKey = wrappedKey.value();
     *     // Process original array
     * }
     * }</pre>
     *
     * @return the wrapped value, which may be {@code null} if {@code null} was wrapped.
     */
    public T value() {
        return value;
    }

    /**
     * Returns the hash code of the wrapped value.
     *
     * <p>The computation depends on how this wrapper was created:</p>
     * <ul>
     *   <li>For wrappers created by {@link #of(Object)}: uses deep hash code computation
     *       via {@link N#deepHashCode(Object)}, so two arrays with equal contents produce
     *       the same hash code.</li>
     *   <li>For wrappers created by {@link #of(Object, ToIntFunction, BiPredicate)} or
     *       {@link #of(Object, ToIntFunction, BiPredicate, Function)}: delegates to the
     *       caller-supplied hash function.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * Wrapper<int[]> w1 = Wrapper.of(arr1);
     * Wrapper<int[]> w2 = Wrapper.of(arr2);
     * // Both wrappers have the same hash code despite different array references
     * assert w1.hashCode() == w2.hashCode();
     * }</pre>
     *
     * @return the hash code of the wrapped value.
     */
    @Override
    public abstract int hashCode();

    /**
     * Compares this wrapper with another object for equality.
     *
     * <p>The comparison depends on how this wrapper was created:</p>
     * <ul>
     *   <li>For wrappers created by {@link #of(Object)}: uses deep equality via
     *       {@link N#deepEquals(Object, Object)}, so two array wrappers whose arrays have
     *       equal contents (including nested arrays) are considered equal.</li>
     *   <li>For wrappers created by {@link #of(Object, ToIntFunction, BiPredicate)} or
     *       {@link #of(Object, ToIntFunction, BiPredicate, Function)}: delegates to the
     *       caller-supplied equals function. A {@link ClassCastException} during comparison
     *       is silently treated as {@code false}.</li>
     * </ul>
     *
     * <p>Returns {@code false} if {@code obj} is not a {@code Wrapper} instance.</p>
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
     * @param obj the object to compare with this wrapper.
     * @return {@code true} if {@code obj} is a {@code Wrapper} whose wrapped value is equal
     *         to this wrapper's value according to the applicable equality semantics;
     *         {@code false} otherwise.
     */
    @Override
    public abstract boolean equals(final Object obj);

    /**
     * A {@link Wrapper} implementation that delegates {@code hashCode}, {@code equals}, and
     * {@code toString} to caller-supplied functions. Created by
     * {@link Wrapper#of(Object, ToIntFunction, BiPredicate)} and
     * {@link Wrapper#of(Object, ToIntFunction, BiPredicate, Function)}.
     *
     * @param <T> the type of the wrapped value
     */
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

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof Wrapper<?> other) {
                try {
                    return equalsFunction.test(value, (T) other.value);
                } catch (final ClassCastException e) {
                    return false;
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return String.format("Wrapper[%s]", toStringFunction.apply(value));
        }
    }

    /**
     * A {@link Wrapper} implementation that uses deep-equality semantics (via
     * {@link N#deepHashCode(Object)} and {@link N#deepEquals(Object, Object)}) for arrays.
     * Created by {@link Wrapper#of(Object)} and used for all array-typed values, including
     * {@code null}. Zero-length arrays are pooled to reduce allocation.
     *
     * @param <T> the type of the wrapped (array) value
     */
    static final class ArrayWrapper<T> extends Wrapper<T> {

        /** Shared instance used when {@code null} is passed to {@link Wrapper#of(Object)}. */
        @SuppressWarnings("rawtypes")
        static final Wrapper WRAPPER_FOR_NULL_ARRAY = new ArrayWrapper<>(null);

        /** Pool of pre-allocated wrappers for zero-length arrays, keyed by component type. */
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
