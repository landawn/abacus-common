/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;

/** 
 * Represents a fixed-size, immutable tuple that can contain between 0 and 9 elements of potentially different types.
 * 
 * <p>Tuples are useful for:</p>
 * <ul>
 *   <li>Returning multiple values from a method without creating a dedicated class</li>
 *   <li>Grouping related values together temporarily</li>
 *   <li>Representing fixed-size heterogeneous collections</li>
 * </ul>
 * 
 * <p>For tuples with 8 or more elements, consider using a dedicated class or record with meaningful property names
 * for better code readability and maintainability.</p>
 * 
 * <p>All tuple implementations are immutable and thread-safe.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating a tuple with 3 elements
 * Tuple3<String, Integer, Boolean> person = Tuple.of("John", 30, true);
 * 
 * // Accessing elements
 * String name = person._1;  // "John"
 * Integer age = person._2;   // 30
 * Boolean active = person._3; // true
 * 
 * // Using forEach
 * person.forEach(System.out::println);
 * }</pre>
 *
 * @param <TP> the self-type of the tuple, typically extending Tuple<TP>
 * 
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.Holder
 * @see com.landawn.abacus.util.Result
 * @see com.landawn.abacus.util.Pair
 * @see com.landawn.abacus.util.Triple 
 */
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings({ "java:S116", "java:S117" })
public abstract class Tuple<TP> implements Immutable {

    Tuple() {
    }

    /**
     * Returns the number of elements in this tuple.
     * 
     * <p>The arity represents the fixed size of the tuple, which ranges from 0 to 9.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t = Tuple.of("a", 1, true);
     * int size = t.arity(); // Returns 3
     * }</pre>
     *
     * @return the arity (size) of this tuple, which is the count of elements it contains
     */
    public abstract int arity();

    /**
     * Checks if any element in this tuple is null.
     * 
     * <p>This method performs a logical OR operation across all elements,
     * returning true as soon as any null element is found.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t1 = Tuple.of("a", null, true);
     * boolean hasNull = t1.anyNull(); // Returns true
     * 
     * Tuple3<String, Integer, Boolean> t2 = Tuple.of("a", 1, true);
     * boolean hasNull2 = t2.anyNull(); // Returns false
     * }</pre>
     *
     * @return {@code true} if at least one element in this tuple is null, {@code false} if all elements are non-null
     */
    public abstract boolean anyNull();

    /**
     * Checks if all elements in this tuple are null.
     * 
     * <p>This method performs a logical AND operation across all elements,
     * returning true only if every element is null.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t1 = Tuple.of(null, null, null);
     * boolean allNull = t1.allNull(); // Returns true
     * 
     * Tuple3<String, Integer, Boolean> t2 = Tuple.of("a", null, null);
     * boolean allNull2 = t2.allNull(); // Returns false
     * }</pre>
     *
     * @return {@code true} if every element in this tuple is null, {@code false} if at least one element is non-null
     */
    public abstract boolean allNull();

    /**
     * Checks if this tuple contains the specified value.
     * 
     * <p>The comparison is performed using object equality (via N.equals() which handles null values).
     * The method checks each element in order and returns {@code true} as soon as a match is found.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t = Tuple.of("hello", 42, true);
     * boolean found1 = t.contains(42);     // Returns true
     * boolean found2 = t.contains("world"); // Returns false
     * boolean found3 = t.contains(null);    // Returns false
     * }</pre>
     *
     * @param valueToFind the value to search for in this tuple, may be null
     * @return {@code true} if this tuple contains an element equal to the specified value, {@code false} otherwise
     */
    public abstract boolean contains(final Object valueToFind);

    /**
     * Returns an array containing all elements of this tuple in their positional order.
     * 
     * <p>The returned array is a new instance and modifications to it do not affect the tuple.
     * The array length equals the arity of this tuple.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t = Tuple.of("a", 1, true);
     * Object[] array = t.toArray(); // Returns ["a", 1, true]
     * }</pre>
     *
     * @return a new Object array containing all tuple elements in order
     */
    public abstract Object[] toArray();

    /**
     * Returns an array containing all elements of this tuple, using the specified array type.
     * 
     * <p>If the provided array is large enough to hold all elements, it is used directly and
     * the element following the last tuple element (if any) is set to null. Otherwise, a new
     * array of the same runtime type is created with the exact size needed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, String> t = Tuple.of("hello", "world");
     * String[] array = t.toArray(new String[0]); // Returns ["hello", "world"]
     * 
     * // Using pre-sized array
     * String[] existing = new String[5];
     * String[] result = t.toArray(existing); // Uses existing array, sets existing[2] to null
     * }</pre>
     *
     * @param <A> the component type of the array to contain the tuple elements
     * @param a the array into which the elements of this tuple are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing all elements of this tuple
     * @throws ArrayStoreException if the runtime type of the specified array is not a supertype
     *         of the runtime type of every element in this tuple
     */
    public abstract <A> A[] toArray(A[] a);

    /**
     * Performs the given action for each element of this tuple in order.
     * 
     * <p>Elements are passed to the consumer one by one from first to last position.
     * The consumer receives each element as an Object, regardless of its actual type.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> t = Tuple.of("hello", 42, true);
     * t.forEach(System.out::println); // Prints each element on a new line
     * 
     * // Collecting elements
     * List<Object> list = new ArrayList<>();
     * t.forEach(list::add);
     * }</pre>
     *
     * @param <E> the type of exception that the consumer may throw
     * @param consumer the action to be performed for each element, must not be null
     * @throws E if the consumer throws an exception
     * @throws IllegalArgumentException if consumer is null
     */
    public abstract <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E;

    /**
     * Performs the given action on this tuple as a whole.
     * 
     * <p>The entire tuple is passed to the consumer as a single argument, allowing
     * type-safe access to all elements at once.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Integer> t = Tuple.of("age", 25);
     * t.accept(tuple -> System.out.println(tuple._1 + ": " + tuple._2));
     * // Prints: "age: 25"
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed on this tuple, must not be null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super TP, E> action) throws E {
        action.accept((TP) this);
    }

    /**
     * Applies the given mapping function to this tuple and returns the result.
     * 
     * <p>The entire tuple is passed to the mapper as a single argument, allowing
     * transformation of the tuple into any other type.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Integer> t = Tuple.of("John", 30);
     * String description = t.map(tuple -> tuple._1 + " is " + tuple._2 + " years old");
     * // Returns: "John is 30 years old"
     * 
     * // Converting to a custom object
     * Person person = t.map(tuple -> new Person(tuple._1, tuple._2));
     * }</pre>
     *
     * @param <R> the type of the result of the mapping function
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the mapping function to apply to this tuple, must not be null
     * @return the result of applying the mapping function to this tuple
     * @throws E if the mapper throws an exception
     */
    public <R, E extends Exception> R map(final Throwables.Function<? super TP, R, E> mapper) throws E {
        return mapper.apply((TP) this);
    }

    /**
     * Returns an Optional containing this tuple if it matches the given predicate,
     * otherwise returns an empty Optional.
     * 
     * <p>This method is useful for conditional processing of tuples in a functional style.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Integer> t = Tuple.of("John", 30);
     * Optional<Tuple2<String, Integer>> adult = t.filter(tuple -> tuple._2 >= 18);
     * // Returns Optional containing the tuple
     * 
     * Optional<Tuple2<String, Integer>> senior = t.filter(tuple -> tuple._2 >= 65);
     * // Returns empty Optional
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test this tuple against, must not be null
     * @return an Optional containing this tuple if the predicate returns true, empty Optional otherwise
     * @throws E if the predicate throws an exception
     */
    @Beta
    public <E extends Exception> Optional<TP> filter(final Throwables.Predicate<? super TP, E> predicate) throws E {
        return predicate.test((TP) this) ? Optional.of((TP) this) : Optional.empty();
    }

    /**
     * Creates a Tuple1 containing the specified element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple1<String> single = Tuple.of("hello");
     * String value = single._1; // "hello"
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param _1 the first element to be contained in the tuple
     * @return a new Tuple1 containing the specified element
     */
    public static <T1> Tuple1<T1> of(final T1 _1) {
        return new Tuple1<>(_1);
    }

    /**
     * Creates a Tuple2 containing the specified elements in order.
     * 
     * <p>This is commonly used for returning two values from a method or
     * grouping two related values together.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Integer> nameAge = Tuple.of("Alice", 25);
     * System.out.println(nameAge._1 + " is " + nameAge._2); // "Alice is 25"
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @return a new Tuple2 containing the specified elements
     */
    public static <T1, T2> Tuple2<T1, T2> of(final T1 _1, final T2 _2) {
        return new Tuple2<>(_1, _2);
    }

    /**
     * Creates a Tuple3 containing the specified elements in order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<String, Integer, Boolean> user = Tuple.of("Bob", 30, true);
     * // Represents name, age, and active status
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @return a new Tuple3 containing the specified elements
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(final T1 _1, final T2 _2, final T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }

    /**
     * Creates a Tuple4 containing the specified elements in order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple4<String, String, Integer, Date> record = 
     *     Tuple.of("John", "Doe", 35, new Date());
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @return a new Tuple4 containing the specified elements
     */
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4) {
        return new Tuple4<>(_1, _2, _3, _4);
    }

    /**
     * Creates a Tuple5 containing the specified elements in order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple5<String, Integer, Boolean, Double, LocalDate> data =
     *     Tuple.of("Product", 100, true, 99.99, LocalDate.now());
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @param _5 the fifth element to be contained in the tuple
     * @return a new Tuple5 containing the specified elements
     */
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5) {
        return new Tuple5<>(_1, _2, _3, _4, _5);
    }

    /**
     * Creates a Tuple6 containing the specified elements in order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple6<String, String, Integer, String, String, String> address =
     *     Tuple.of("John", "Doe", 123, "Main St", "Boston", "MA");
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @param _5 the fifth element to be contained in the tuple
     * @param _6 the sixth element to be contained in the tuple
     * @return a new Tuple6 containing the specified elements
     */
    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6) {
        return new Tuple6<>(_1, _2, _3, _4, _5, _6);
    }

    /**
     * Creates a Tuple7 containing the specified elements in order.
     * 
     * <p>For tuples of this size, consider whether a dedicated class with meaningful
     * field names would provide better code clarity.</p>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @param _5 the fifth element to be contained in the tuple
     * @param _6 the sixth element to be contained in the tuple
     * @param _7 the seventh element to be contained in the tuple
     * @return a new Tuple7 containing the specified elements
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5,
            final T6 _6, final T7 _7) {
        return new Tuple7<>(_1, _2, _3, _4, _5, _6, _7);
    }

    /**
     * Creates a Tuple8 containing the specified elements in order.
     * 
     * <p><b>Note:</b> This method is deprecated. For tuples with 8 or more elements,
     * it is strongly recommended to use a dedicated class or record with meaningful property names
     * for better code readability and maintainability.</p>
     * 
     * <p>Example of preferred approach:</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of Tuple8, consider:
     * public record PersonDetails(
     *     String firstName, String lastName, int age,
     *     String email, String phone, String address,
     *     String city, String country
     * ) {}
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     * @param <T8> the type of the eighth element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @param _5 the fifth element to be contained in the tuple
     * @param _6 the sixth element to be contained in the tuple
     * @param _7 the seventh element to be contained in the tuple
     * @param _8 the eighth element to be contained in the tuple
     * @return a new Tuple8 containing the specified elements
     * @deprecated you should consider using {@code class SomeBean/Record class: MyRecord { final T1 propName1, final T2 propName2...}}
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5,
            final T6 _6, final T7 _7, final T8 _8) {
        return new Tuple8<>(_1, _2, _3, _4, _5, _6, _7, _8);
    }

    /**
     * Creates a Tuple9 containing the specified elements in order.
     * 
     * <p><b>Note:</b> This method is deprecated. For tuples with 9 elements,
     * it is strongly recommended to use a dedicated class or record with meaningful property names.</p>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     * @param <T8> the type of the eighth element
     * @param <T9> the type of the ninth element
     * @param _1 the first element to be contained in the tuple
     * @param _2 the second element to be contained in the tuple
     * @param _3 the third element to be contained in the tuple
     * @param _4 the fourth element to be contained in the tuple
     * @param _5 the fifth element to be contained in the tuple
     * @param _6 the sixth element to be contained in the tuple
     * @param _7 the seventh element to be contained in the tuple
     * @param _8 the eighth element to be contained in the tuple
     * @param _9 the ninth element to be contained in the tuple
     * @return a new Tuple9 containing the specified elements
     * @deprecated you should consider using {@code class SomeBean/Record class: MyRecord { final T1 propName1, final T2 propName2...}}
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4,
            final T5 _5, final T6 _6, final T7 _7, final T8 _8, final T9 _9) {
        return new Tuple9<>(_1, _2, _3, _4, _5, _6, _7, _8, _9);
    }

    /**
     * Creates a Tuple2 from a Map.Entry, with the entry's key as the first element
     * and the entry's value as the second element.
     * 
     * <p>This is useful for converting Map entries to tuples for further processing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = Map.of("Alice", 25, "Bob", 30);
     * map.entrySet().stream()
     *     .map(Tuple::create)
     *     .forEach(t -> System.out.println(t._1 + ": " + t._2));
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <K> the key type of the map entry
     * @param <V> the value type of the map entry
     * @param entry the map entry to convert to a tuple, must not be null
     * @return a new Tuple2 containing the key and value from the entry
     */
    @Beta
    public static <K, V> Tuple2<K, V> create(final Map.Entry<K, V> entry) {
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    /**
     * Creates a tuple from an array of objects.
     * 
     * <p>The arity of the returned tuple matches the length of the array.
     * The array must contain between 0 and 9 elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] data = {"hello", 42, true};
     * Tuple3<String, Integer, Boolean> t = Tuple.create(data);
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <TP> the type of tuple to create
     * @param a the array of objects to convert to a tuple, may be null or empty
     * @return a tuple containing the array elements in order, or Tuple0.EMPTY if array is null or empty
     * @throws IllegalArgumentException if the array contains more than 9 elements
     */
    @Beta
    public static <TP extends Tuple<TP>> TP create(final Object[] a) {
        final int len = a == null ? 0 : a.length;

        if (len == 0) {
            return (TP) Tuple0.EMPTY;
        }

        Tuple<?> result = null;

        switch (len) {
            case 1:
                result = new Tuple1<>(a[0]);
                break;

            case 2:
                result = new Tuple2<>(a[0], a[1]);
                break;

            case 3:
                result = new Tuple3<>(a[0], a[1], a[2]);
                break;

            case 4:
                result = new Tuple4<>(a[0], a[1], a[2], a[3]);
                break;

            case 5:
                result = new Tuple5<>(a[0], a[1], a[2], a[3], a[4]);
                break;

            case 6:
                result = new Tuple6<>(a[0], a[1], a[2], a[3], a[4], a[5]);
                break;

            case 7:
                result = new Tuple7<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6]);
                break;

            case 8:
                result = new Tuple8<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]);
                break;

            case 9:
                result = new Tuple9<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]);
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + a.length + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    /**
     * Creates a tuple from a collection of objects.
     * 
     * <p>The arity of the returned tuple matches the size of the collection.
     * Elements are added to the tuple in the order returned by the collection's iterator.
     * The collection must contain between 0 and 9 elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> list = Arrays.asList("a", 1, true);
     * Tuple3<String, Integer, Boolean> t = Tuple.create(list);
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <TP> the type of tuple to create
     * @param c the collection of objects to convert to a tuple, may be null or empty
     * @return a tuple containing the collection elements in iteration order, or Tuple0.EMPTY if collection is null or empty
     * @throws IllegalArgumentException if the collection contains more than 9 elements
     */
    @Beta
    public static <TP extends Tuple<TP>> TP create(final Collection<?> c) {
        final int len = c == null ? 0 : c.size();

        if (len == 0) {
            return (TP) Tuple0.EMPTY;
        }

        final Iterator<?> iter = c.iterator();

        Tuple<?> result = null;

        switch (len) {
            case 1:
                result = new Tuple1<>(iter.next());
                break;

            case 2:
                result = new Tuple2<>(iter.next(), iter.next());
                break;

            case 3:
                result = new Tuple3<>(iter.next(), iter.next(), iter.next());
                break;

            case 4:
                result = new Tuple4<>(iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 5:
                result = new Tuple5<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 6:
                result = new Tuple6<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 7:
                result = new Tuple7<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 8:
                result = new Tuple8<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 9:
                result = new Tuple9<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + c.size() + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    /**
     * Converts a Tuple1 to a List containing its single element.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple1<String> t = Tuple.of("hello");
     * List<String> list = Tuple.toList(t); // Returns ["hello"]
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the type of the element in the tuple
     * @param tp the Tuple1 to convert to a list, must not be null
     * @return a List containing the single element from the tuple
     */
    @Beta
    public static <T> List<T> toList(final Tuple1<? extends T> tp) {
        return N.asList(tp._1);
    }

    /**
     * Converts a Tuple2 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, String> t = Tuple.of("hello", "world");
     * List<String> list = Tuple.toList(t); // Returns ["hello", "world"]
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple2 to convert to a list, must not be null
     * @return a List containing the two elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple2<? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2);
    }

    /**
     * Converts a Tuple3 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple3 to convert to a list, must not be null
     * @return a List containing the three elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple3<? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3);
    }

    /**
     * Converts a Tuple4 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple4 to convert to a list, must not be null
     * @return a List containing the four elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple4<? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4);
    }

    /**
     * Converts a Tuple5 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple5 to convert to a list, must not be null
     * @return a List containing the five elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple5<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5);
    }

    /**
     * Converts a Tuple6 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple6 to convert to a list, must not be null
     * @return a List containing the six elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple6<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6);
    }

    /**
     * Converts a Tuple7 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple7 to convert to a list, must not be null
     * @return a List containing the seven elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple7<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7);
    }

    /**
     * Converts a Tuple8 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple8 to convert to a list, must not be null
     * @return a List containing the eight elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(final Tuple8<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7, tp._8);
    }

    /**
     * Converts a Tuple9 to a List containing its elements in order.
     * 
     * <p>All elements must be assignable to the common type T.</p>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T> the common type of the elements in the tuple
     * @param tp the Tuple9 to convert to a list, must not be null
     * @return a List containing the nine elements from the tuple in order
     */
    @Beta
    public static <T> List<T> toList(
            final Tuple9<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7, tp._8, tp._9);
    }

    /**
     * Flattens a nested Tuple2 structure into a Tuple3.
     * 
     * <p>The input tuple contains a Tuple2 as its first element and a single element as its second element.
     * The result combines all three elements into a single Tuple3.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<Tuple2<String, Integer>, Boolean> nested = 
     *     Tuple.of(Tuple.of("name", 25), true);
     * Tuple3<String, Integer, Boolean> flat = Tuple.flatten(nested);
     * // Result: ("name", 25, true)
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T1> the type of the first element in the nested tuple
     * @param <T2> the type of the second element in the nested tuple
     * @param <T3> the type of the third element (second element of outer tuple)
     * @param tp the nested tuple structure to flatten, must not be null
     * @return a new Tuple3 containing all three elements in order
     */
    @Beta
    public static <T1, T2, T3> Tuple3<T1, T2, T3> flatten(final Tuple2<Tuple2<T1, T2>, T3> tp) {
        return new Tuple3<>(tp._1._1, tp._1._2, tp._2);
    }

    /**
     * Flattens a nested Tuple3 structure into a Tuple5.
     * 
     * <p>The input tuple contains a Tuple3 as its first element and two additional elements.
     * The result combines all five elements into a single Tuple5.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<Tuple3<String, Integer, Double>, Boolean, Date> nested = 
     *     Tuple.of(Tuple.of("data", 100, 3.14), true, new Date());
     * Tuple5<String, Integer, Double, Boolean, Date> flat = Tuple.flatten(nested);
     * // Result: ("data", 100, 3.14, true, Date)
     * }</pre>
     * 
     * <p><b>Note:</b> This method is marked as {@link Beta} and may be subject to change.</p>
     *
     * @param <T1> the type of the first element in the nested tuple
     * @param <T2> the type of the second element in the nested tuple
     * @param <T3> the type of the third element in the nested tuple
     * @param <T4> the type of the fourth element (second element of outer tuple)
     * @param <T5> the type of the fifth element (third element of outer tuple)
     * @param tp the nested tuple structure to flatten, must not be null
     * @return a new Tuple5 containing all five elements in order
     */
    @Beta
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> flatten(final Tuple3<Tuple3<T1, T2, T3>, T4, T5> tp) {
        return new Tuple5<>(tp._1._1, tp._1._2, tp._1._3, tp._2, tp._3);
    }

    /**
     * Represents an empty tuple with no elements.
     * 
     * <p>This internal class is used to represent the absence of values in a tuple context.
     * The singleton instance {@code EMPTY} is returned by factory methods when creating
     * tuples from empty collections or arrays.</p>
     * 
     * <p><b>Note:</b> This class is marked as {@link Beta} and is primarily for internal use.</p>
     */
    @Beta
    static final class Tuple0 extends Tuple<Tuple0> {

        private static final Tuple0 EMPTY = new Tuple0();

        Tuple0() {
        }

        /**
         * Returns the arity (number of elements) of this tuple.
         *
         * <p>For Tuple0, this always returns 0 as it contains no elements.</p>
         *
         * @return 0, as this is an empty tuple
         */
        @Override
        public int arity() {
            return 0;
        }

        /**
         * Checks if any element in this tuple is null.
         *
         * <p>For Tuple0, this always returns {@code false} since there are no elements to be null.</p>
         *
         * @return {@code false}, as an empty tuple has no null elements
         */
        @Override
        public boolean anyNull() {
            return false;
        }

        /**
         * Checks if all elements in this tuple are null.
         *
         * <p>For Tuple0, this returns {@code true} following the mathematical convention
         * that "all elements" of an empty set satisfy any predicate (vacuous truth).</p>
         *
         * @return {@code true}, following the vacuous truth principle
         */
        @Override
        public boolean allNull() {
            return true;
        }

        /**
         * Checks if this tuple contains the specified value.
         *
         * <p>For Tuple0, this always returns {@code false} since there are no elements to contain.</p>
         *
         * @param valueToFind the value to search for
         * @return {@code false}, as an empty tuple contains no elements
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return false;
        }

        /**
         * Returns an empty array.
         *
         * <p>For Tuple0, this always returns an empty Object array.</p>
         *
         * @return an empty Object array
         */
        @Override
        public Object[] toArray() {
            return N.EMPTY_OBJECT_ARRAY;
        }

        /**
         * Returns the specified array with a null terminator if it has space.
         *
         * <p>For Tuple0 (empty tuple), if the provided array has length > 0,
         * element at index 0 is set to null to mark the end of tuple elements,
         * following Java Collection contract.</p>
         *
         * @param <A> the component type of the array
         * @param a the array to use or a template for creating a new array
         * @return the same array, potentially with a[0] set to null if a.length > 0
         */
        @Override
        public <A> A[] toArray(final A[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        /**
         * Performs the given action for each element of this tuple.
         *
         * <p>For Tuple0, this method validates the consumer but performs no action
         * since there are no elements to iterate over.</p>
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the action to be performed for each element
         * @throws IllegalArgumentException if consumer is null
         * @throws E if the consumer throws an exception (won't happen for Tuple0)
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws IllegalArgumentException, E {
            N.checkArgNotNull(consumer);
            // do nothing.
        }

        /**
         * Returns a string representation of this tuple.
         *
         * <p>For Tuple0, this always returns "()" representing an empty tuple.</p>
         *
         * @return "()"
         */
        @Override
        public String toString() {
            return "()";
        }
    }

    /**
     * Represents a tuple with exactly one element.
     * 
     * <p>This class provides a type-safe container for a single value and can be useful for:</p>
     * <ul>
     *   <li>Wrapping a single value to pass through generic APIs expecting tuples</li>
     *   <li>Maintaining consistency when working with methods that return different tuple sizes</li>
     *   <li>Representing optional single values in a tuple context</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple1<String> message = Tuple.of("Hello");
     * String value = message._1; // "Hello"
     * 
     * // Converting to array
     * Object[] array = message.toArray(); // ["Hello"]
     * }</pre>
     *
     * @param <T1> the type of the single element in this tuple
     */
    public static final class Tuple1<T1> extends Tuple<Tuple1<T1>> {

        /** The first and only element of this tuple. May be null. */
        public final T1 _1;

        /**
         * Instantiates a new tuple 1.
         */
        // For Kryo
        Tuple1() {
            this(null);
        }

        /**
         * Instantiates a new tuple 1.
         *
         * @param _1 the first element
         */
        Tuple1(final T1 _1) {
            this._1 = _1;
        }

        /**
         * Returns the arity (number of elements) of this tuple.
         *
         * <p>For Tuple1, this always returns 1 as it contains exactly one element.</p>
         *
         * @return 1, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 1;
        }

        /**
         * Checks if any element in this tuple is null.
         *
         * <p>For Tuple1, this returns {@code true} if and only if the single element is null.</p>
         *
         * @return {@code true} if the element is null, {@code false} otherwise
         */
        @Override
        public boolean anyNull() {
            return _1 == null;
        }

        /**
         * Checks if all elements in this tuple are null.
         *
         * <p>For Tuple1, this returns {@code true} if and only if the single element is null.</p>
         *
         * @return {@code true} if the element is null, {@code false} otherwise
         */
        @Override
        public boolean allNull() {
            return _1 == null;
        }

        /**
         * Checks if this tuple contains the specified value.
         * 
         * <p>Since this tuple has only one element, this method returns true
         * if and only if the element equals the specified value (including null equality).</p>
         *
         * @param valueToFind the value to search for
         * @return {@code true} if the element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind);
        }

        /**
         * Returns an array containing the single element of this tuple.
         *
         * @return a new Object array of length 1 containing the element
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1 };
        }

        /**
         * Returns an array containing this tuple's element, using the specified array type.
         *
         * @param <A> the component type of the array
         * @param a the array into which the element is to be stored, if it is big enough
         * @return an array containing the tuple element
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 1) {
                a = N.copyOf(a, 1);
            }

            a[0] = (A) _1;

            return a;
        }

        /**
         * Performs the given action on the single element of this tuple.
         *
         * @param <E> the type of exception that the consumer may throw
         * @param consumer the action to be performed on the element
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
        }

        /**
         * Returns a hash code value for this tuple.
         *
         * <p>The hash code is computed based on the element value.</p>
         *
         * @return a hash code value for this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            final int result = 1;
            return prime * result + N.hashCode(_1);
        }

        /**
         * Checks if this tuple is equal to the specified object.
         * 
         * <p>Two Tuple1 instances are equal if they contain equal elements.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple1 with an equal element
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple1.class)) {
                final Tuple1<?> other = (Tuple1<?>) obj;

                return N.equals(_1, other._1);
            }

            return false;
        }

        /**
         * Returns a string representation of this tuple.
         *
         * <p>The format is: ({@code _1})</p>
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ")";
        }
    }

    /**
     * Represents a tuple with exactly two elements.
     * 
     * <p>This is one of the most commonly used tuple types, often representing:</p>
     * <ul>
     *   <li>Key-value pairs</li>
     *   <li>Coordinates (x, y)</li>
     *   <li>Range boundaries (min, max)</li>
     *   <li>Two related values that don't warrant a dedicated class</li>
     * </ul>
     * 
     * <p>Tuple2 provides additional utility methods for working with pairs of values,
     * including conversion to {@link Pair} and {@link java.util.Map.Entry}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Creating a key-value pair
     * Tuple2<String, Integer> score = Tuple.of("Alice", 95);
     * 
     * // Using with bi-consumer
     * score.accept((name, points) -> 
     *     System.out.println(name + " scored " + points));
     * 
     * // Converting to Map.Entry
     * Map.Entry<String, Integer> entry = score.toEntry();
     * 
     * // Reversing elements
     * Tuple2<Integer, String> reversed = score.reverse(); // (95, "Alice")
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     */
    public static final class Tuple2<T1, T2> extends Tuple<Tuple2<T1, T2>> {

        /** The first element of this tuple. May be null. */
        public final T1 _1;

        /** The second element of this tuple. May be null. */
        public final T2 _2;

        /**
         * Instantiates a new tuple 2.
         */
        // For Kryo
        Tuple2() {
            this(null, null);
        }

        /**
         * Instantiates a new tuple 2.
         *
         * @param _1 the first element
         * @param _2 the second element
         */
        Tuple2(final T1 _1, final T2 _2) {
            this._1 = _1;
            this._2 = _2;
        }

        /**
         * Returns the arity (number of elements) of this tuple.
         *
         * <p>For Tuple2, this always returns 2 as it contains exactly two elements.</p>
         *
         * @return 2, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 2;
        }

        /**
         * Checks if any element in this tuple is null.
         *
         * <p>For Tuple2, this returns {@code true} if either element is null.</p>
         *
         * @return {@code true} if at least one element is null, {@code false} otherwise
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null;
        }

        /**
         * Checks if all elements in this tuple are null.
         *
         * <p>For Tuple2, this returns {@code true} if both elements are null.</p>
         *
         * @return {@code true} if both elements are null, {@code false} otherwise
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null;
        }

        /**
         * Checks if this tuple contains the specified value.
         * 
         * <p>Returns true if either element equals the specified value.</p>
         *
         * @param valueToFind the value to search for
         * @return {@code true} if either element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind);
        }

        /**
         * Returns an array containing the two elements of this tuple in order.
         *
         * @return a new Object array of length 2 containing the elements
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2 };
        }

        /**
         * Returns an array containing this tuple's elements, using the specified array type.
         *
         * @param <A> the component type of the array
         * @param a the array into which the elements are to be stored, if it is big enough
         * @return an array containing the tuple elements
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 2) {
                a = N.copyOf(a, 2);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;

            return a;
        }

        /**
         * Converts this Tuple2 to a Pair with the same elements.
         * 
         * <p>The first element becomes the left value and the second element
         * becomes the right value of the Pair.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<String, Integer> tuple = Tuple.of("key", 100);
         * Pair<String, Integer> pair = tuple.toPair();
         * // pair.left = "key", pair.right = 100
         * }</pre>
         *
         * @return a new Pair containing the same elements as this tuple
         */
        public Pair<T1, T2> toPair() {
            return Pair.of(_1, _2);
        }

        /**
         * Converts this Tuple2 to an ImmutableEntry (Map.Entry) with the same elements.
         * 
         * <p>The first element becomes the key and the second element becomes the value
         * of the entry. This is useful for creating map entries or when working with
         * APIs that expect Map.Entry objects.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<String, Integer> tuple = Tuple.of("age", 25);
         * Map.Entry<String, Integer> entry = tuple.toEntry();
         * Map<String, Integer> map = new HashMap<>();
         * map.put(entry.getKey(), entry.getValue());
         * }</pre>
         *
         * @return a new ImmutableEntry containing the same elements as this tuple
         */
        public ImmutableEntry<T1, T2> toEntry() {
            return ImmutableEntry.of(_1, _2);
        }

        /**
         * Creates a new Tuple2 with the elements in reversed order.
         * 
         * <p>This is useful when you need to swap the positions of elements,
         * such as converting from (key, value) to (value, key).</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<String, Integer> original = Tuple.of("name", 42);
         * Tuple2<Integer, String> reversed = original.reverse(); // (42, "name")
         * }</pre>
         *
         * @return a new Tuple2 with elements in reversed order
         */
        public Tuple2<T2, T1> reverse() {
            return of(_2, _1); //NOSONAR
        }

        /**
         * Performs the given action for each element of this tuple.
         *
         * @param <E> the type of exception that the consumer may throw
         * @param consumer the action to be performed for each element
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
        }

        /**
         * Performs the given bi-consumer action on the two elements of this tuple.
         * 
         * <p>This method provides a more natural way to work with the two elements
         * as separate parameters rather than accessing them through fields.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<String, Integer> nameAge = Tuple.of("Alice", 30);
         * nameAge.accept((name, age) -> 
         *     System.out.println(name + " is " + age + " years old"));
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the bi-consumer action to be performed on the tuple elements
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super T1, ? super T2, E> action) throws E {
            action.accept(_1, _2);
        }

        /**
         * Applies the given bi-function to the two elements of this tuple and returns the result.
         * 
         * <p>This method allows transformation of the tuple elements into a single value
         * using a function that takes both elements as parameters.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<Integer, Integer> dimensions = Tuple.of(10, 20);
         * Integer area = dimensions.map((width, height) -> width * height); // 200
         * 
         * Tuple2<String, String> names = Tuple.of("John", "Doe");
         * String fullName = names.map((first, last) -> first + " " + last);
         * }</pre>
         *
         * @param <R> the type of the result of the bi-function
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the bi-function to apply to the tuple elements
         * @return the result of applying the bi-function to this tuple's elements
         * @throws E if the mapper throws an exception
         */
        public <R, E extends Exception> R map(final Throwables.BiFunction<? super T1, ? super T2, ? extends R, E> mapper) throws E {
            return mapper.apply(_1, _2);
        }

        /**
         * Returns an Optional containing this tuple if both elements match the given bi-predicate,
         * otherwise returns an empty Optional.
         * 
         * <p>This method is useful for filtering tuples based on conditions involving both elements.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple2<Integer, Integer> range = Tuple.of(10, 20);
         * Optional<Tuple2<Integer, Integer>> valid = range.filter((min, max) -> min < max);
         * // Returns Optional containing the tuple
         * 
         * Optional<Tuple2<Integer, Integer>> invalid = range.filter((min, max) -> min > max);
         * // Returns empty Optional
         * }</pre>
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the bi-predicate to test the tuple elements against
         * @return an Optional containing this tuple if the predicate returns true, empty Optional otherwise
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> Optional<Tuple2<T1, T2>> filter(final Throwables.BiPredicate<? super T1, ? super T2, E> predicate) throws E {
            return predicate.test(_1, _2) ? Optional.of(this) : Optional.empty();
        }

        /**
         * Returns a hash code value for this tuple.
         *
         * <p>The hash code is computed based on the values of both elements.</p>
         *
         * @return a hash code value for this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            return prime * result + N.hashCode(_2);
        }

        /**
         * Checks if this tuple is equal to the specified object.
         * 
         * <p>Two Tuple2 instances are equal if they contain equal elements in the same positions.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple2 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple2.class)) {
                final Tuple2<?, ?> other = (Tuple2<?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2);
            }

            return false;
        }

        /**
         * Returns a string representation of this tuple.
         *
         * <p>The format is: ({@code _1}, {@code _2})</p>
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ")";
        }
    }

    /**
     * Represents a tuple with exactly three elements.
     * 
     * <p>Tuple3 is commonly used for:</p>
     * <ul>
     *   <li>3D coordinates (x, y, z)</li>
     *   <li>RGB color values</li>
     *   <li>Database rows with three columns</li>
     *   <li>Returning three related values from a method</li>
     * </ul>
     * 
     * <p>This class provides additional utility methods for working with three elements,
     * including specialized consumers and functions that accept three parameters.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // 3D coordinates
     * Tuple3<Double, Double, Double> point = Tuple.of(1.0, 2.0, 3.0);
     * 
     * // RGB color
     * Tuple3<Integer, Integer, Integer> color = Tuple.of(255, 128, 0);
     * 
     * // Using tri-consumer
     * color.accept((r, g, b) -> 
     *     System.out.printf("RGB(%d, %d, %d)%n", r, g, b));
     * 
     * // Converting to Triple
     * Triple<String, Integer, Boolean> triple = 
     *     Tuple.of("data", 42, true).toTriple();
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     */
    public static final class Tuple3<T1, T2, T3> extends Tuple<Tuple3<T1, T2, T3>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple3 with all null elements.
         */
        // For Kryo
        Tuple3() {
            this(null, null, null);
        }

        /**
         * Creates a new Tuple3 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         */
        Tuple3(final T1 _1, final T2 _2, final T3 _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        /**
         * {@inheritDoc}
         *
         * @return 3, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 3;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the three elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all three elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 3) {
                a = N.copyOf(a, 3);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;

            return a;
        }

        /**
         * Converts this Tuple3 to a Triple with the same elements.
         * Triple is another representation of a three-element container.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple3<String, Integer, Boolean> tuple = Tuple.of("A", 1, true);
         * Triple<String, Integer, Boolean> triple = tuple.toTriple();
         * }</pre>
         *
         * @return a new Triple containing the same elements as this tuple
         */
        public Triple<T1, T2, T3> toTriple() {
            return Triple.of(_1, _2, _3);
        }

        /**
         * Creates a new Tuple3 with the elements in reversed order.
         * The first element becomes the third, the third element becomes the first,
         * and the second element remains in the middle position.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple3<String, Integer, Boolean> original = Tuple.of("A", 2, true);
         * Tuple3<Boolean, Integer, String> reversed = original.reverse();
         * // reversed contains (true, 2, "A")
         * }</pre>
         *
         * @return a new Tuple3 with elements in reversed order
         */
        public Tuple3<T3, T2, T1> reverse() {
            return new Tuple3<>(_3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1, _2, _3.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
        }

        /**
         * Performs the given tri-consumer action on the three elements of this tuple.
         * The elements are passed to the tri-consumer as separate arguments.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple3<String, Integer, Boolean> data = Tuple.of("John", 30, true);
         * data.accept((name, age, active) -> 
         *     System.out.println(name + " is " + age + " years old"));
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the tri-consumer action to be performed on the tuple elements
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.TriConsumer<? super T1, ? super T2, ? super T3, E> action) throws E {
            action.accept(_1, _2, _3);
        }

        /**
         * Applies the given tri-function to the three elements of this tuple and returns the result.
         * The elements are passed to the tri-function as separate arguments.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple3<Integer, Integer, Integer> rgb = Tuple.of(255, 128, 64);
         * String hex = rgb.map((r, g, b) -> 
         *     String.format("#%02X%02X%02X", r, g, b));
         * // hex = "#FF8040"
         * }</pre>
         *
         * @param <R> the type of the result of the tri-function
         * @param <E> the type of exception that the mapper may throw
         * @param mapper the tri-function to apply to the tuple elements
         * @return the result of applying the tri-function to this tuple's elements
         * @throws E if the mapper throws an exception
         */
        public <R, E extends Exception> R map(final Throwables.TriFunction<? super T1, ? super T2, ? super T3, ? extends R, E> mapper) throws E {
            return mapper.apply(_1, _2, _3);
        }

        /**
         * Returns an Optional containing this tuple if all three elements match the given tri-predicate,
         * otherwise returns an empty Optional.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple3<Integer, Integer, Integer> point = Tuple.of(3, 4, 5);
         * Optional<Tuple3<Integer, Integer, Integer>> rightTriangle = 
         *     point.filter((a, b, c) -> a*a + b*b == c*c);
         * // rightTriangle.isPresent() == true
         * }</pre>
         *
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the tri-predicate to test the tuple elements against
         * @return an Optional containing this tuple if the predicate returns true, empty Optional otherwise
         * @throws E if the predicate throws an exception
         */
        public <E extends Exception> Optional<Tuple3<T1, T2, T3>> filter(final Throwables.TriPredicate<? super T1, ? super T2, ? super T3, E> predicate)
                throws E {
            return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.empty();
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all three elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            return prime * result + N.hashCode(_3);
        }

        /**
         * {@inheritDoc}
         * Two Tuple3 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple3 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple3.class)) {
                final Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ")";
        }
    }

    /**
     * Represents an immutable tuple of 4 elements of potentially different types.
     * 
     * <p>Tuple4 is commonly used for:</p>
     * <ul>
     *   <li>RGBA color values (red, green, blue, alpha)</li>
     *   <li>Database rows with four columns</li>
     *   <li>4D coordinates or quaternions</li>
     *   <li>Returning four related values from a method</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // RGBA color
     * Tuple4<Integer, Integer, Integer, Float> color = 
     *     Tuple.of(255, 128, 0, 0.8f);
     * 
     * // Database record
     * Tuple4<Long, String, String, Date> user = 
     *     Tuple.of(123L, "John", "john@example.com", new Date());
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     */
    public static final class Tuple4<T1, T2, T3, T4> extends Tuple<Tuple4<T1, T2, T3, T4>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple4 with all null elements.
         */
        // For Kryo
        Tuple4() {
            this(null, null, null, null);
        }

        /**
         * Creates a new Tuple4 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         */
        Tuple4(final T1 _1, final T2 _2, final T3 _3, final T4 _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        /**
         * {@inheritDoc}
         *
         * @return 4, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 4;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the four elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all four elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3, _4]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 4) {
                a = N.copyOf(a, 4);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;

            return a;
        }

        /**
         * Creates a new Tuple4 with the elements in reversed order.
         * The first element becomes the fourth, the fourth element becomes the first,
         * the second element becomes the third, and the third element becomes the second.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple4<String, Integer, Double, Boolean> original = 
         *     Tuple.of("A", 1, 2.0, true);
         * Tuple4<Boolean, Double, Integer, String> reversed = 
         *     original.reverse();
         * // reversed contains (true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple4 with elements in reversed order
         */
        public Tuple4<T4, T3, T2, T1> reverse() {
            return new Tuple4<>(_4, _3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1, _2, _3, _4.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all four elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            return prime * result + N.hashCode(_4);
        }

        /**
         * {@inheritDoc}
         * Two Tuple4 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple4 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple4.class)) {
                final Tuple4<?, ?, ?, ?> other = (Tuple4<?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3, _4)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ")";
        }
    }

    /**
     * Represents an immutable tuple of 5 elements of potentially different types.
     * 
     * <p>Tuple5 is suitable for very complex data structures requiring five components.
     * At this size, consider whether a dedicated class with meaningful field names
     * would provide better code readability and maintainability.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Database record with five fields
     * Tuple5<Long, String, String, Date, Boolean> employee = 
     *     Tuple.of(123L, "John Doe", "Engineering", new Date(), true);
     * 
     * // 5D point
     * Tuple5<Double, Double, Double, Double, Double> point5D = 
     *     Tuple.of(1.0, 2.0, 3.0, 4.0, 5.0);
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     */
    public static final class Tuple5<T1, T2, T3, T4, T5> extends Tuple<Tuple5<T1, T2, T3, T4, T5>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /** The fifth element of this tuple */
        public final T5 _5;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple5 with all null elements.
         */
        // For Kryo
        Tuple5() {
            this(null, null, null, null, null);
        }

        /**
         * Creates a new Tuple5 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         * @param _5 the fifth element
         */
        Tuple5(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        /**
         * {@inheritDoc}
         *
         * @return 5, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 5;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the five elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all five elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind)
                    || N.equals(_5, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3, _4, _5]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 5) {
                a = N.copyOf(a, 5);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;

            return a;
        }

        /**
         * Creates a new Tuple5 with the elements in reversed order.
         * The first element becomes the fifth, the fifth element becomes the first,
         * the second element becomes the fourth, the fourth element becomes the second,
         * and the third element remains in the middle position.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple5<String, Integer, Double, Boolean, Long> original = 
         *     Tuple.of("A", 1, 2.0, true, 100L);
         * Tuple5<Long, Boolean, Double, Integer, String> reversed = 
         *     original.reverse();
         * // reversed contains (100L, true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple5 with elements in reversed order
         */
        public Tuple5<T5, T4, T3, T2, T1> reverse() {
            return new Tuple5<>(_5, _4, _3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1, _2, _3, _4, _5.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all five elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            return prime * result + N.hashCode(_5);
        }

        /**
         * {@inheritDoc}
         * Two Tuple5 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple5 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple5.class)) {
                final Tuple5<?, ?, ?, ?, ?> other = (Tuple5<?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3, _4, _5)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ")";
        }
    }

    /**
     * Represents an immutable tuple of 6 elements of potentially different types.
     * 
     * <p>Tuple6 is suitable for very complex data structures requiring six components.
     * At this size, consider whether a dedicated class with meaningful field names
     * would provide better code readability and maintainability.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Configuration tuple
     * Tuple6<String, Integer, Boolean, String, Double, Long> config = 
     *     Tuple.of("localhost", 8080, true, "admin", 3.14, 1000L);
     * 
     * // Date/time components
     * Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> datetime = 
     *     Tuple.of(2024, 12, 25, 14, 30, 45);
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     */
    public static final class Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple<Tuple6<T1, T2, T3, T4, T5, T6>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /** The fifth element of this tuple */
        public final T5 _5;

        /** The sixth element of this tuple */
        public final T6 _6;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple6 with all null elements.
         */
        // For Kryo
        Tuple6() {
            this(null, null, null, null, null, null);
        }

        /**
         * Creates a new Tuple6 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         * @param _5 the fifth element
         * @param _6 the sixth element
         */
        Tuple6(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
        }

        /**
         * {@inheritDoc}
         *
         * @return 6, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 6;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the six elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all six elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3, _4, _5, _6]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 6) {
                a = N.copyOf(a, 6);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;

            return a;
        }

        /**
         * Creates a new Tuple6 with the elements in reversed order.
         * Elements are rearranged so the first becomes the sixth,
         * the second becomes the fifth, and so on.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple6<String, Integer, Double, Boolean, Long, Character> original = 
         *     Tuple.of("A", 1, 2.0, true, 100L, 'X');
         * Tuple6<Character, Long, Boolean, Double, Integer, String> reversed = 
         *     original.reverse();
         * // reversed contains ('X', 100L, true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple6 with elements in reversed order
         */
        public Tuple6<T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple6<>(_6, _5, _4, _3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1, _2, _3, _4, _5, _6.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all six elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            return prime * result + N.hashCode(_6);
        }

        /**
         * {@inheritDoc}
         * Two Tuple6 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple6 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple6.class)) {
                final Tuple6<?, ?, ?, ?, ?, ?> other = (Tuple6<?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3, _4, _5, _6)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ")";
        }
    }

    /**
     * Represents an immutable tuple of 7 elements of potentially different types.
     * 
     * <p>Tuple7 is suitable for very complex data structures requiring seven components.
     * At this size, consider whether a dedicated class with meaningful field names
     * would provide better code readability and maintainability.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Weekly temperature readings
     * Tuple7<Double, Double, Double, Double, Double, Double, Double> weeklyTemp = 
     *     Tuple.of(20.5, 21.0, 19.8, 22.3, 23.1, 24.0, 22.5);
     * 
     * // Complex database record
     * Tuple7<Long, String, String, Date, Boolean, Double, String> record = 
     *     Tuple.of(123L, "John", "Doe", new Date(), true, 75000.0, "Active");
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     */
    public static final class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple<Tuple7<T1, T2, T3, T4, T5, T6, T7>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /** The fifth element of this tuple */
        public final T5 _5;

        /** The sixth element of this tuple */
        public final T6 _6;

        /** The seventh element of this tuple */
        public final T7 _7;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple7 with all null elements.
         */
        // For Kryo
        Tuple7() {
            this(null, null, null, null, null, null, null);
        }

        /**
         * Creates a new Tuple7 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         * @param _5 the fifth element
         * @param _6 the sixth element
         * @param _7 the seventh element
         */
        Tuple7(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
        }

        /**
         * {@inheritDoc}
         *
         * @return 7, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 7;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the seven elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all seven elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3, _4, _5, _6, _7]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 7) {
                a = N.copyOf(a, 7);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;

            return a;
        }

        /**
         * Creates a new Tuple7 with the elements in reversed order.
         * The ordering is completely reversed: first becomes seventh,
         * second becomes sixth, and so on.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple7<String, Integer, Double, Boolean, Long, Character, Float> original = 
         *     Tuple.of("A", 1, 2.0, true, 100L, 'X', 3.14f);
         * Tuple7<Float, Character, Long, Boolean, Double, Integer, String> reversed = 
         *     original.reverse();
         * // reversed contains (3.14f, 'X', 100L, true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple7 with elements in reversed order
         */
        public Tuple7<T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple7<>(_7, _6, _5, _4, _3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1, _2, _3, _4, _5, _6, _7.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all seven elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            return prime * result + N.hashCode(_7);
        }

        /**
         * {@inheritDoc}
         * Two Tuple7 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple7 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple7.class)) {
                final Tuple7<?, ?, ?, ?, ?, ?, ?> other = (Tuple7<?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3, _4, _5, _6, _7)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ")";
        }
    }

    /**
     * Represents an immutable tuple of 8 elements of potentially different types.
     * 
     * <p>Tuple8 is suitable for very complex data structures requiring eight components.
     * At this size, consider whether a dedicated class with meaningful field names
     * would provide better code readability and maintainability.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complex configuration with eight parameters
     * Tuple8<String, Integer, String, Boolean, Double, Long, String, Integer> config = 
     *     Tuple.of("server1", 8080, "https", true, 99.9, 5000L, "admin", 10);
     * 
     * // Multi-dimensional data point
     * Tuple8<Double, Double, Double, Double, Double, Double, Double, Double> point8D = 
     *     Tuple.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     * @param <T8> the type of the eighth element
     */
    public static final class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /** The fifth element of this tuple */
        public final T5 _5;

        /** The sixth element of this tuple */
        public final T6 _6;

        /** The seventh element of this tuple */
        public final T7 _7;

        /** The eighth element of this tuple */
        public final T8 _8;

        /**
         * Default constructor for serialization frameworks.
         * Creates a Tuple8 with all null elements.
         */
        // For Kryo
        Tuple8() {
            this(null, null, null, null, null, null, null, null);
        }

        /**
         * Creates a new Tuple8 with the specified elements.
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         * @param _5 the fifth element
         * @param _6 the sixth element
         * @param _7 the seventh element
         * @param _8 the eighth element
         */
        Tuple8(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7, final T8 _8) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
        }

        /**
         * {@inheritDoc}
         *
         * @return 8, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 8;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if any of the eight elements is null
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} if all eight elements are null
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null;
        }

        /**
         * {@inheritDoc}
         *
         * @param valueToFind the value to search for
         * @return {@code true} if any element equals the specified value
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind) || N.equals(_8, valueToFind);
        }

        /**
         * {@inheritDoc}
         *
         * @return an array containing [_1, _2, _3, _4, _5, _6, _7, _8]
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8 };
        }

        /**
         * {@inheritDoc}
         *
         * @param <A> the component type of the array
         * @param a the array to fill
         * @return the filled array
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 8) {
                a = N.copyOf(a, 8);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;
            a[7] = (A) _8;

            return a;
        }

        /**
         * Creates a new Tuple8 with the elements in reversed order.
         * All elements are reversed: first becomes eighth, second becomes seventh, etc.
         *
         * 
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple8<String, Integer, Double, Boolean, Long, Character, Float, Byte> original = 
         *     Tuple.of("A", 1, 2.0, true, 100L, 'X', 3.14f, (byte)5);
         * Tuple8<Byte, Float, Character, Long, Boolean, Double, Integer, String> reversed = 
         *     original.reverse();
         * // reversed contains ((byte)5, 3.14f, 'X', 100L, true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple8 with elements in reversed order
         */
        public Tuple8<T8, T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple8<>(_8, _7, _6, _5, _4, _3, _2, _1);
        }

        /**
         * {@inheritDoc}
         * Applies the consumer to each element in order: _1 through _8.
         *
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to apply
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
            objConsumer.accept(_8);
        }

        /**
         * {@inheritDoc}
         * Computes hash code based on all eight elements.
         *
         * @return the hash code of this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            result = prime * result + N.hashCode(_7);
            return prime * result + N.hashCode(_8);
        }

        /**
         * {@inheritDoc}
         * Two Tuple8 instances are equal if all corresponding elements are equal.
         *
         * @param obj the object to compare with
         * @return {@code true} if the specified object is a Tuple8 with equal elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple8.class)) {
                final Tuple8<?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7) && N.equals(_8, other._8);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         * Returns a string representation in the format "(_1, _2, _3, _4, _5, _6, _7, _8)".
         *
         * @return a string representation of this tuple
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + ")";
        }
    }

    /**
     * Represents an immutable tuple of 9 elements of potentially different types.
     * 
     * <p>Tuple9 is suitable for very complex data structures requiring nine components.
     * At this size, consider whether a dedicated class with meaningful field names
     * would provide better code readability and maintainability.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Creating a Tuple9
     * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
     *     Tuple.of("John", 30, 175.5, true, 'A', 1000L, 3.14f, (short)10, (byte)5);
     * 
     * // Accessing elements
     * String name = t._1;      // "John"
     * Integer age = t._2;      // 30
     * Double height = t._3;    // 175.5
     * 
     * // Using functional operations
     * t.forEach(System.out::println);  // Prints all 9 elements
     * String summary = t.map(tuple -> tuple._1 + " is " + tuple._2 + " years old");
     * }</pre>
     *
     * @param <T1> the type of the first element
     * @param <T2> the type of the second element
     * @param <T3> the type of the third element
     * @param <T4> the type of the fourth element
     * @param <T5> the type of the fifth element
     * @param <T6> the type of the sixth element
     * @param <T7> the type of the seventh element
     * @param <T8> the type of the eighth element
     * @param <T9> the type of the ninth element
     */
    public static final class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Tuple<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

        /** The first element of this tuple */
        public final T1 _1;

        /** The second element of this tuple */
        public final T2 _2;

        /** The third element of this tuple */
        public final T3 _3;

        /** The fourth element of this tuple */
        public final T4 _4;

        /** The fifth element of this tuple */
        public final T5 _5;

        /** The sixth element of this tuple */
        public final T6 _6;

        /** The seventh element of this tuple */
        public final T7 _7;

        /** The eighth element of this tuple */
        public final T8 _8;

        /** The ninth element of this tuple */
        public final T9 _9;

        /**
         * Default no-arg constructor for serialization frameworks (e.g., Kryo).
         * Creates a Tuple9 with all elements set to null.
         * 
         * <p>This constructor is primarily used by serialization libraries and should not
         * be called directly in application code.</p>
         */
        // For Kryo
        Tuple9() {
            this(null, null, null, null, null, null, null, null, null);
        }

        /**
         * Creates a new Tuple9 with the specified elements.
         * 
         * <p>Any or all elements may be null. The tuple is immutable once created.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     new Tuple9<>("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * }</pre>
         *
         * @param _1 the first element
         * @param _2 the second element
         * @param _3 the third element
         * @param _4 the fourth element
         * @param _5 the fifth element
         * @param _6 the sixth element
         * @param _7 the seventh element
         * @param _8 the eighth element
         * @param _9 the ninth element
         */
        Tuple9(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7, final T8 _8, final T9 _9) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
        }

        //    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> flatten(
        //            Tuple2<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, T9> tp) {
        //        return new Tuple9<>(tp._1._1, tp._1._2, tp._1._3, tp._1._4, tp._1._5, tp._1._6, tp._1._7, tp._1._8, tp._2);
        //    }

        /**
         * Returns the arity (number of elements) of this tuple.
         * 
         * <p>For Tuple9, this method always returns 9.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * int arity = t.arity(); // Returns 9
         * }</pre>
         *
         * @return 9, the number of elements in this tuple
         */
        @Override
        public int arity() {
            return 9;
        }

        /**
         * Checks if any element in this tuple is null.
         * 
         * <p>This method performs a short-circuit evaluation, returning true as soon as
         * the first null element is encountered.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t1 = 
         *     Tuple.of("A", null, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * boolean hasNull = t1.anyNull(); // Returns true
         * 
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t2 = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * boolean hasNull2 = t2.anyNull(); // Returns false
         * }</pre>
         *
         * @return {@code true} if at least one element is null, {@code false} otherwise
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null || _9 == null;
        }

        /**
         * Checks if all elements in this tuple are null.
         * 
         * <p>This method returns {@code true} only when every single element (_1 through _9) is null.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t1 = 
         *     Tuple.of(null, null, null, null, null, null, null, null, null);
         * boolean allNull = t1.allNull(); // Returns true
         * 
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t2 = 
         *     Tuple.of("A", null, null, null, null, null, null, null, null);
         * boolean allNull2 = t2.allNull(); // Returns false
         * }</pre>
         *
         * @return {@code true} if all 9 elements are null, {@code false} otherwise
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null && _9 == null;
        }

        /**
         * Checks if this tuple contains the specified value.
         * 
         * <p>The comparison uses N.equals() which handles null values correctly.
         * The method checks each element in order (_1 through _9) and returns true
         * as soon as a matching element is found.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * boolean found1 = t.contains("A");     // Returns true
         * boolean found2 = t.contains(100L);    // Returns true
         * boolean found3 = t.contains("B");     // Returns false
         * boolean found4 = t.contains(null);    // Returns false
         * }</pre>
         *
         * @param valueToFind the value to search for, may be null
         * @return {@code true} if any element equals the specified value, {@code false} otherwise
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind) || N.equals(_8, valueToFind) || N.equals(_9, valueToFind);
        }

        /**
         * Returns a new array containing all elements of this tuple in order.
         * 
         * <p>The returned array is always a new Object array of length 9, containing
         * the elements in positions _1 through _9. Modifications to the returned array
         * do not affect the tuple.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * Object[] array = t.toArray();
         * // array = ["A", 1, 2.0, true, 'X', 100L, 3.14f, 5, 10]
         * }</pre>
         *
         * @return a new Object array containing all 9 elements in order
         */
        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8, _9 };
        }

        /**
         * Returns an array containing all elements of this tuple, using the provided array if possible.
         * 
         * <p>If the provided array has a length of at least 9, it is filled with the tuple elements
         * and returned. If the array is larger than 9, the element at index 9 is set to null.
         * If the array is smaller than 9, a new array of the same runtime type with length 9 is created.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, String, String, String, String, String, String, String, String> t = 
         *     Tuple.of("A", "B", "C", "D", "E", "F", "G", "H", "I");
         * 
         * // Using a smaller array - new array created
         * String[] small = new String[5];
         * String[] result1 = t.toArray(small); // New String[9] array created
         * 
         * // Using exact size array
         * String[] exact = new String[9];
         * String[] result2 = t.toArray(exact); // Same array returned
         * 
         * // Using larger array
         * String[] large = new String[12];
         * String[] result3 = t.toArray(large); // Same array used, large[9] set to null
         * }</pre>
         *
         * @param <A> the component type of the array
         * @param a the array to fill, or whose runtime type to use for creating a new array
         * @return an array containing all 9 elements of this tuple
         * @throws ArrayStoreException if any tuple element cannot be stored in the array due to type mismatch
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 9) {
                a = N.copyOf(a, 9);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;
            a[7] = (A) _8;
            a[8] = (A) _9;

            return a;
        }

        /**
         * Returns a new Tuple9 with all elements in reverse order.
         * 
         * <p>Creates a new tuple where the first element becomes the last, the second becomes
         * the second-to-last, and so on. The original tuple remains unchanged.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * Tuple9<Byte, Short, Float, Long, Character, Boolean, Double, Integer, String> reversed = 
         *     t.reverse();
         * // reversed = ((byte)10, (short)5, 3.14f, 100L, 'X', true, 2.0, 1, "A")
         * }</pre>
         *
         * @return a new Tuple9 with elements in reverse order
         */
        public Tuple9<T9, T8, T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple9<>(_9, _8, _7, _6, _5, _4, _3, _2, _1);
        }

        /**
         * Performs the given action for each element of this tuple in order.
         * 
         * <p>The consumer is called once for each element, from _1 through _9 in sequence.
         * Each element is passed to the consumer as an Object, regardless of its actual type.
         * The consumer must handle any type casting if needed.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * // Print each element
         * t.forEach(System.out::println);
         * 
         * // Collect elements into a list
         * List<Object> list = new ArrayList<>();
         * t.forEach(list::add);
         * 
         * // Count non-null elements
         * AtomicInteger count = new AtomicInteger();
         * t.forEach(e -> { if (e != null) count.incrementAndGet(); });
         * }</pre>
         *
         * @param <E> the type of exception that the consumer may throw
         * @param consumer the action to perform on each element, must not be null
         * @throws E if the consumer throws an exception
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
            objConsumer.accept(_8);
            objConsumer.accept(_9);
        }

        /**
         * Returns a hash code value for this tuple.
         * 
         * <p>The hash code is computed using all 9 elements. The calculation uses a standard
         * polynomial hash formula with prime number 31, and handles null values correctly
         * through N.hashCode().</p>
         * 
         * <p>The hash code is consistent with equals(): two tuples that are equal according
         * to equals() will have the same hash code.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t1 = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t2 = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * assert t1.hashCode() == t2.hashCode(); // Same elements = same hash code
         * }</pre>
         *
         * @return the hash code value for this tuple
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            result = prime * result + N.hashCode(_7);
            result = prime * result + N.hashCode(_8);
            return prime * result + N.hashCode(_9);
        }

        /**
         * Compares this tuple to the specified object for equality.
         * 
         * <p>Two Tuple9 instances are considered equal if and only if all 9 corresponding
         * elements are equal according to N.equals() (which handles null values correctly).
         * The comparison is type-safe: the object must be exactly a Tuple9 instance.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t1 = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t2 = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t3 = 
         *     Tuple.of("B", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * assert t1.equals(t2);  // true - all elements equal
         * assert !t1.equals(t3); // false - first element differs
         * assert !t1.equals("not a tuple"); // false - different type
         * }</pre>
         *
         * @param obj the object to compare with this tuple
         * @return {@code true} if the specified object is a Tuple9 with all elements equal to this tuple's elements
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple9.class)) {
                final Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7) && N.equals(_8, other._8) && N.equals(_9, other._9);
            }

            return false;
        }

        /**
         * Returns a string representation of this tuple.
         * 
         * <p>The string representation consists of the 9 elements enclosed in parentheses
         * and separated by commas and spaces. Each element is converted to string using
         * N.toString() which handles null values by returning "null".</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t = 
         *     Tuple.of("A", 1, 2.0, true, 'X', 100L, 3.14f, (short)5, (byte)10);
         * 
         * String str = t.toString(); 
         * // Returns: "(A, 1, 2.0, true, X, 100, 3.14, 5, 10)"
         * 
         * Tuple9<String, Integer, Double, Boolean, Character, Long, Float, Short, Byte> t2 = 
         *     Tuple.of("A", null, 2.0, true, null, 100L, 3.14f, null, (byte)10);
         * 
         * String str2 = t2.toString(); 
         * // Returns: "(A, null, 2.0, true, null, 100, 3.14, null, 10)"
         * }</pre>
         *
         * @return a string representation of this tuple in the format "(e1, e2, e3, e4, e5, e6, e7, e8, e9)"
         */
        @Override
        public String toString() {
            return "(" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + ", " + N.toString(_9) + ")";
        }
    }
}
