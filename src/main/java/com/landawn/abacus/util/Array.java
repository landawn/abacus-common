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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.NullSafe;

/**
 * A comprehensive utility class providing an extensive collection of static methods for array manipulation,
 * creation, transformation, and processing operations. This class serves as the primary array utility facade
 * in the Abacus library, offering performance-optimized operations for all primitive array types and
 * multi-dimensional arrays with null-safety and type-safety as core design principles.
 *
 * <p>The {@code Array} class provides a complete toolkit for array operations including boxing/unboxing,
 * matrix operations, array creation, type conversions, and advanced manipulations. All methods are static,
 * thread-safe, and designed to handle edge cases gracefully while maintaining optimal performance for
 * large-scale array processing.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Comprehensive Boxing/Unboxing:</b> Efficient conversion between primitive and wrapper arrays</li>
 *   <li><b>Matrix Operations:</b> Transpose operations for 2D arrays of all primitive types</li>
 *   <li><b>Multi-dimensional Support:</b> Operations on 1D, 2D, and 3D arrays</li>
 *   <li><b>Null-Safe Operations:</b> Graceful handling of null inputs with configurable default values</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms with minimal object allocation</li>
 *   <li><b>Range Operations:</b> Subset processing with fromIndex/toIndex parameters</li>
 *   <li><b>Reflection Integration:</b> Dynamic array creation using component types</li>
 * </ul>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Array Creation:</b> {@code newInstance()} with dynamic type specification</li>
 *   <li><b>Boxing Operations:</b> Convert primitive arrays to wrapper arrays</li>
 *   <li><b>Unboxing Operations:</b> Convert wrapper arrays to primitive arrays with null handling</li>
 *   <li><b>Matrix Operations:</b> {@code transpose()} for 2D array matrix transformations</li>
 *   <li><b>Range Processing:</b> Subset operations with index-based boundaries</li>
 *   <li><b>Multi-dimensional:</b> Support for 1D, 2D, and 3D array operations</li>
 *   <li><b>Type Conversions:</b> Safe conversions between different array types</li>
 *   <li><b>Validation:</b> Array structure validation for matrix operations</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Null Safety:</b> Methods handle {@code null} inputs gracefully, returning {@code null} or
 *       using provided default values rather than throwing exceptions</li>
 *   <li><b>Index Conventions:</b> Methods use {@code fromIndex} and {@code toIndex} parameters following
 *       half-open range conventions [fromIndex, toIndex)</li>
 *   <li><b>Exception Minimization:</b> Exceptions are thrown only when method contracts are violated
 *       (e.g., negative array sizes, invalid index ranges)</li>
 *   <li><b>Performance First:</b> Optimized algorithms with minimal overhead and object allocation</li>
 *   <li><b>Type Preservation:</b> Maintains array component types through generic methods</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Dynamic array creation with reflection
 * int[] intArray = Array.newInstance(int.class, 10);
 * String[] stringArray = Array.newInstance(String.class, 5);
 *
 * // Boxing operations - primitive to wrapper arrays
 * int[] primitives = {1, 2, 3, 4, 5};
 * Integer[] boxed = Array.box(primitives);  // [1, 2, 3, 4, 5]
 *
 * // Unboxing operations - wrapper to primitive arrays
 * Integer[] wrappers = {1, null, 3, null, 5};
 * int[] unboxed = Array.unbox(wrappers, -1);  // [1, -1, 3, -1, 5] (null replaced with -1)
 *
 * // Range-based unboxing
 * int[] subset = Array.unbox(wrappers, 1, 4, 0);  // [0, 3, 0] (from index 1 to 4)
 *
 * // Matrix operations - 2D array transpose
 * int[][] matrix = {{1, 2, 3}, {4, 5, 6}};
 * int[][] transposed = Array.transpose(matrix);  // {{1, 4}, {2, 5}, {3, 6}}
 *
 * // Multi-dimensional array operations
 * Integer[][][] cube = new Integer[2][3][4];
 * int[][][] unboxedCube = Array.unbox(cube, 0);
 *
 * // Null-safe operations
 * int[] result = Array.unbox((Integer[]) null, 42);  // Returns null
 * int[][] transposed2 = Array.transpose((int[][]) null);  // Returns null
 * }</pre>
 *
 * <p><b>Boxing Operations:</b>
 * <ul>
 *   <li><b>All Primitive Types:</b> boolean, char, byte, short, int, long, float, double</li>
 *   <li><b>Multi-dimensional:</b> Support for 2D and 3D arrays</li>
 *   <li><b>Null Handling:</b> Proper handling of null elements in source arrays</li>
 *   <li><b>Type Safety:</b> Maintains correct wrapper types for each primitive</li>
 * </ul>
 *
 * <p><b>Unboxing Operations:</b>
 * <ul>
 *   <li><b>Null Value Replacement:</b> Configurable default values for null elements</li>
 *   <li><b>Range Support:</b> Process subsets of arrays with fromIndex/toIndex</li>
 *   <li><b>Multi-dimensional:</b> Support for 2D and 3D wrapper arrays</li>
 *   <li><b>Varargs Support:</b> Convenient varargs methods for simple cases</li>
 * </ul>
 *
 * <p><b>Matrix Operations:</b>
 * <ul>
 *   <li><b>Transpose:</b> Matrix transposition for all primitive and object types</li>
 *   <li><b>Validation:</b> Automatic validation of matrix structure (rectangular arrays)</li>
 *   <li><b>Null Safety:</b> Graceful handling of null or malformed matrices</li>
 *   <li><b>Generic Support:</b> Type-safe transposition for object arrays</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Memory Efficient:</b> Minimal object allocation and copying</li>
 *   <li><b>Cache Friendly:</b> Sequential access patterns optimized for CPU cache</li>
 *   <li><b>Algorithm Selection:</b> Optimal algorithms chosen based on array size and type</li>
 *   <li><b>Zero-Copy Operations:</b> Direct array manipulation where possible</li>
 *   <li><b>Bulk Processing:</b> Optimized for large array operations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All static methods are stateless and thread-safe</li>
 *   <li><b>Immutable Operations:</b> Methods create new arrays rather than modifying inputs</li>
 *   <li><b>No Shared State:</b> No static mutable fields that could cause race conditions</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <p><b>Index Range Conventions:</b>
 * <ul>
 *   <li><b>Half-Open Ranges:</b> fromIndex (inclusive) to toIndex (exclusive)</li>
 *   <li><b>Boundary Validation:</b> Comprehensive index bounds checking</li>
 *   <li><b>Empty Ranges:</b> Graceful handling of empty index ranges</li>
 *   <li><b>Consistent API:</b> Uniform index parameter conventions across all methods</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>Graceful Degradation:</b> Methods handle edge cases gracefully</li>
 *   <li><b>Null Tolerance:</b> Comprehensive null input handling throughout the API</li>
 *   <li><b>Index Validation:</b> Clear IndexOutOfBoundsException for invalid ranges</li>
 *   <li><b>Matrix Validation:</b> Validation of array structure for matrix operations</li>
 * </ul>
 *
 * <p><b>Integration with Java Arrays:</b>
 * <ul>
 *   <li><b>Arrays Class Extension:</b> Extends java.util.Arrays functionality</li>
 *   <li><b>Reflection Integration:</b> Full integration with java.lang.reflect.Array</li>
 *   <li><b>Collection Compatibility:</b> Seamless integration with Java Collections</li>
 *   <li><b>Stream Support:</b> Compatible with Java 8+ Stream operations</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate default values for null replacement during unboxing</li>
 *   <li>Validate matrix structure before performing transpose operations</li>
 *   <li>Prefer range-based operations for processing array subsets</li>
 *   <li>Consider memory implications when working with large multi-dimensional arrays</li>
 *   <li>Use the type-safe generic methods for object array operations</li>
 *   <li>Cache the results of expensive operations like matrix transposition</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Use primitive arrays when possible to avoid boxing overhead</li>
 *   <li>Process arrays in sequential order for better cache performance</li>
 *   <li>Consider the cost of array copying in performance-critical code</li>
 *   <li>Use appropriate array sizes to minimize memory allocation</li>
 *   <li>Prefer bulk operations over element-by-element processing</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Safe Unboxing:</b> {@code int[] result = Array.unbox(wrappers, defaultValue);}</li>
 *   <li><b>Matrix Processing:</b> {@code int[][] transposed = Array.transpose(matrix);}</li>
 *   <li><b>Range Operations:</b> {@code int[] subset = Array.unbox(array, from, to, defaultValue);}</li>
 *   <li><b>Dynamic Creation:</b> {@code T[] array = Array.newInstance(type, size);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with array operations</li>
 *   <li><b>{@link com.landawn.abacus.util.CommonUtil}:</b> Base utility operations</li>
 *   <li><b>{@link java.util.Arrays}:</b> Core Java array utilities</li>
 *   <li><b>{@link java.lang.reflect.Array}:</b> Reflection-based array operations</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterables}:</b> Iterable utilities for array processing</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Stream operations for arrays</li>
 * </ul>
 *
 * <p><b>Example: Data Processing Pipeline</b>
 * <pre>{@code
 * // Complete array processing example
 * Integer[] rawData = {1, null, 3, null, 5, 6, null, 8};
 *
 * // Safe unboxing with default value for nulls
 * int[] processedData = Array.unbox(rawData, 0);  // [1, 0, 3, 0, 5, 6, 0, 8]
 *
 * // Process subset of data
 * int[] subset = Array.unbox(rawData, 2, 6, -1);  // [3, -1, 5, 6]
 *
 * // Work with 2D arrays
 * Integer[][] matrix = {{1, 2, 3}, {4, null, 6}, {7, 8, 9}};
 * int[][] cleanMatrix = Array.unbox(matrix, 0);   // Replace nulls with 0
 * int[][] transposed = Array.transpose(cleanMatrix);  // Transpose the matrix
 *
 * // Dynamic array creation
 * double[] doubles = Array.newInstance(double.class, 10);
 * Arrays.fill(doubles, 3.14);
 *
 * // Multi-dimensional processing
 * Double[][][] cube = new Double[2][3][4];
 * // Fill with some data...
 * double[][][] processedCube = Array.unbox(cube, 0.0);
 * }</pre>
 *
 * <p><b>Example: Matrix Mathematics</b>
 * <pre>{@code
 * // Matrix operations for mathematical computations
 * double[][] matrixA = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
 * double[][] matrixB = {{7.0, 8.0}, {9.0, 10.0}, {11.0, 12.0}};
 *
 * // Transpose matrices for multiplication compatibility
 * double[][] transposedA = Array.transpose(matrixA);  // 3x2 matrix
 * double[][] transposedB = Array.transpose(matrixB);  // 2x3 matrix
 *
 * // Working with null-containing matrices
 * Double[][] sparseMatrix = {{1.0, null, 3.0}, {null, 5.0, null}};
 * double[][] denseMatrix = Array.unbox(sparseMatrix, 0.0);  // Fill nulls with 0.0
 * double[][] result = Array.transpose(denseMatrix);
 *
 * // Validate matrix structure
 * try {
 *     double[][] invalid = {{1.0, 2.0}, {3.0}};  // Irregular matrix
 *     double[][] transposed = Array.transpose(invalid);  // Will handle gracefully
 * } catch (Exception e) {
 *     // Handle validation errors
 * }
 * }</pre>
 *
 * <p><b>Nested Classes:</b>
 * <ul>
 *   <li><b>{@link ArrayUtil}:</b> Deprecated concrete implementation class</li>
 * </ul>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang and other open source projects under
 * the Apache License 2.0. Methods from these libraries may have been modified for consistency,
 * performance optimization, and enhanced null-safety within the Abacus framework.</p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.CommonUtil
 * @see java.util.Arrays
 * @see java.lang.reflect.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.stream.Stream
 */
@SuppressWarnings({ "java:S1168" })
public abstract sealed class Array permits Array.ArrayUtil {
    private Array() {
        // Utility class.
    }

    /**
     * Creates a new instance of an array with the specified component type and length.
     *
     * <p>This method uses {@link java.lang.reflect.Array#newInstance(Class, int)} to create a new instance of the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] intArray = Array.newInstance(Integer.class, 5); // returns an Integer array of length 5
     * String[] strArray = Array.newInstance(String.class, 10); // returns a String array of length 10
     * }</pre>
     *
     * @param <T> the type of the array.
     * @param componentType the Class object representing the component type of the new array.
     * @param length the length of the new array.
     * @return the new array.
     * @throws NegativeArraySizeException if the specified length is negative.
     */
    public static <T> T newInstance(final Class<?> componentType, final int length) throws NegativeArraySizeException {
        N.checkArgNotNull(componentType, cs.componentType);

        if (length == 0) {
            final Object result = N.CLASS_EMPTY_ARRAY.computeIfAbsent(componentType, k -> java.lang.reflect.Array.newInstance(componentType, length));

            return (T) result;
        }

        return (T) java.lang.reflect.Array.newInstance(componentType, length);
    }

    /**
     * Creates a new instance of an array with the specified component type and dimensions.
     *
     * <p>This method uses {@link java.lang.reflect.Array#newInstance(Class, int...)} to create a new instance of the specified array.
     * The dimensions should be a valid int array representing the dimensions of the new array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Array.newInstance(Integer.class, 5, 5); // returns a 5x5 Integer array
     * Array.newInstance(String.class, 3, 3, 3); // returns a 3x3x3 String array
     * }</pre>
     *
     * @param <T> the type of the array.
     * @param componentType the Class object representing the component type of the new array.
     * @param dimensions the dimensions of the new array.
     * @return the new array
     * @throws NegativeArraySizeException if any of the specified dimensions is negative.
     * @see java.lang.reflect.Array#newInstance(Class, int...)
     */
    public static <T> T newInstance(final Class<?> componentType, final int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        return (T) java.lang.reflect.Array.newInstance(componentType, dimensions);
    }

    /**
     * Retrieves the length of the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getLength(Object)} to determine the length of the array.
     * The array can be an object array or a primitive array.
     * If the array is {@code null}, this method returns 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * int length = Array.getLength(array); // returns 3
     * int[] nums = {1, 2, 3, 4};
     * int numLength = Array.getLength(nums); // returns 4
     * int nullLength = Array.getLength(null); // returns 0
     * }</pre>
     *
     * @param array the array whose length is to be determined.
     * @return the length of the array, or 0 if the array is {@code null}.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @see java.lang.reflect.Array#getLength(Object)
     */
    public static int getLength(final Object array) throws IllegalArgumentException {
        return array == null ? 0 : java.lang.reflect.Array.getLength(array);
    }

    /**
     * Retrieves the element at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#get(Object, int)} to retrieve the element.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] array = {1, 2, 3};
     * Integer element = Array.get(array, 1); // returns 2
     * }</pre>
     *
     * @param <T> the type of the array.
     * @param array the array from which to retrieve the element.
     * @param index the index of the element to be retrieved.
     * @return the element at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#get(Object, int)
     */
    public static <T> T get(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return (T) java.lang.reflect.Array.get(array, index);
    }

    /**
     * Retrieves the boolean value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getBoolean(Object, int)} to retrieve the boolean value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = {true, false, true};
     * boolean element = Array.getBoolean(array, 1); // returns false
     * }</pre>
     *
     * @param array the array from which to retrieve the boolean value.
     * @param index the index of the boolean value to be retrieved.
     * @return the boolean value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getBoolean(Object, int)
     */
    public static boolean getBoolean(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getBoolean(array, index);
    }

    /**
     * Retrieves the byte value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getByte(Object, int)} to retrieve the byte value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] array = {1, 2, 3};
     * byte element = Array.getByte(array, 1); // returns 2
     * }</pre>
     *
     * @param array the array from which to retrieve the byte value.
     * @param index the index of the byte value to be retrieved.
     * @return the byte value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getByte(Object, int)
     */
    public static byte getByte(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getByte(array, index);
    }

    /**
     * Retrieves the char value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getChar(Object, int)} to retrieve the char value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] array = {'a', 'b', 'c'};
     * char element = Array.getChar(array, 1); // returns 'b'
     * }</pre>
     *
     * @param array the array from which to retrieve the char value.
     * @param index the index of the char value to be retrieved.
     * @return the char value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getChar(Object, int)
     */
    public static char getChar(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getChar(array, index);
    }

    /**
     * Retrieves the short value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getShort(Object, int)} to retrieve the short value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array = {1, 2, 3};
     * short element = Array.getShort(array, 1); // returns 2
     * }</pre>
     *
     * @param array the array from which to retrieve the short value.
     * @param index the index of the short value to be retrieved.
     * @return the short value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getShort(Object, int)
     */
    public static short getShort(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getShort(array, index);
    }

    /**
     * Retrieves the integer value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getInt(Object, int)} to retrieve the integer value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = {1, 2, 3};
     * int element = Array.getInt(array, 1); // returns 2
     * }</pre>
     *
     * @param array the array from which to retrieve the integer value.
     * @param index the index of the integer value to be retrieved.
     * @return the integer value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getInt(Object, int)
     */
    public static int getInt(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getInt(array, index);
    }

    /**
     * Retrieves the long value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getLong(Object, int)} to retrieve the long value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = {1L, 2L, 3L};
     * long element = Array.getLong(array, 1); // returns 2L
     * }</pre>
     *
     * @param array the array from which to retrieve the long value.
     * @param index the index of the long value to be retrieved.
     * @return the long value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getLong(Object, int)
     */
    public static long getLong(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getLong(array, index);
    }

    /**
     * Retrieves the float value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getFloat(Object, int)} to retrieve the float value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = {1.1f, 2.2f, 3.3f};
     * float element = Array.getFloat(array, 1); // returns 2.2f
     * }</pre>
     *
     * @param array the array from which to retrieve the float value.
     * @param index the index of the float value to be retrieved.
     * @return the float value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getFloat(Object, int)
     */
    public static float getFloat(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getFloat(array, index);
    }

    /**
     * Retrieves the double value at the specified index from the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#getDouble(Object, int)} to retrieve the double value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] array = {1.1, 2.2, 3.3};
     * double element = Array.getDouble(array, 1); // returns 2.2
     * }</pre>
     *
     * @param array the array from which to retrieve the double value.
     * @param index the index of the double value to be retrieved.
     * @return the double value at the specified index.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#getDouble(Object, int)
     */
    public static double getDouble(final Object array, final int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        return java.lang.reflect.Array.getDouble(array, index);
    }

    /**
     * Sets the value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#set(Object, int, Object)} to set the value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] array = {1, 2, 3};
     * Array.set(array, 1, 4); // array now is {1, 4, 3}
     * }</pre>
     *
     * @param array the array in which to set the value.
     * @param index the index at which the value is to be set.
     * @param value the value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#set(Object, int, Object)
     */
    public static void set(final Object array, final int index, final Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.set(array, index, value);
    }

    /**
     * Sets the boolean value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setBoolean(Object, int, boolean)} to set the boolean value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = {true, false, true};
     * Array.setBoolean(array, 1, true); // array now is {true, true, true}
     * }</pre>
     *
     * @param array the array in which to set the boolean value.
     * @param index the index at which the boolean value is to be set.
     * @param z the boolean value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setBoolean(Object, int, boolean)
     */
    public static void setBoolean(final Object array, final int index, final boolean z) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setBoolean(array, index, z);
    }

    /**
     * Sets the byte value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setByte(Object, int, byte)} to set the byte value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] array = {1, 2, 3};
     * Array.setByte(array, 1, (byte)4); // array now is {1, 4, 3}
     * }</pre>
     *
     * @param array the array in which to set the byte value.
     * @param index the index at which the byte value is to be set.
     * @param b the byte value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setByte(Object, int, byte)
     */
    public static void setByte(final Object array, final int index, final byte b) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setByte(array, index, b);
    }

    /**
     * Sets the char value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setChar(Object, int, char)} to set the char value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] array = {'a', 'b', 'c'};
     * Array.setChar(array, 1, 'd'); // array now is {'a', 'd', 'c'}
     * }</pre>
     *
     * @param array the array in which to set the char value.
     * @param index the index at which the char value is to be set.
     * @param c the char value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setChar(Object, int, char)
     */
    public static void setChar(final Object array, final int index, final char c) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setChar(array, index, c);
    }

    /**
     * Sets the short value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setShort(Object, int, short)} to set the short value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array = {1, 2, 3};
     * Array.setShort(array, 1, (short)4); // array now is {1, 4, 3}
     * }</pre>
     *
     * @param array the array in which to set the short value.
     * @param index the index at which the short value is to be set.
     * @param s the short value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setShort(Object, int, short)
     */
    public static void setShort(final Object array, final int index, final short s) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setShort(array, index, s);
    }

    /**
     * Sets the integer value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setInt(Object, int, int)} to set the integer value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = {1, 2, 3};
     * Array.setInt(array, 1, 4); // array now is {1, 4, 3}
     * }</pre>
     *
     * @param array the array in which to set the integer value.
     * @param index the index at which the integer value is to be set.
     * @param i the integer value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setInt(Object, int, int)
     */
    public static void setInt(final Object array, final int index, final int i) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setInt(array, index, i);
    }

    /**
     * Sets the long value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setLong(Object, int, long)} to set the long value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = {1L, 2L, 3L};
     * Array.setLong(array, 1, 4L); // array now is {1L, 4L, 3L}
     * }</pre>
     *
     * @param array the array in which to set the long value.
     * @param index the index at which the long value is to be set.
     * @param l the long value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setLong(Object, int, long)
     */
    public static void setLong(final Object array, final int index, final long l) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setLong(array, index, l);
    }

    /**
     * Sets the float value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setFloat(Object, int, float)} to set the float value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = {1.1f, 2.2f, 3.3f};
     * Array.setFloat(array, 1, 4.4f); // array now is {1.1f, 4.4f, 3.3f}
     * }</pre>
     *
     * @param array the array in which to set the float value.
     * @param index the index at which the float value is to be set.
     * @param f the float value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setFloat(Object, int, float)
     */
    public static void setFloat(final Object array, final int index, final float f) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setFloat(array, index, f);
    }

    /**
     * Sets the double value at the specified index in the provided array.
     *
     * <p>This method uses {@link java.lang.reflect.Array#setDouble(Object, int, double)} to set the double value.
     * The array can be an object array or a primitive array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] array = {1.1, 2.2, 3.3};
     * Array.setDouble(array, 1, 4.4); // array now is {1.1, 4.4, 3.3}
     * }</pre>
     *
     * @param array the array in which to set the double value.
     * @param index the index at which the double value is to be set.
     * @param d the double value to be set.
     * @throws IllegalArgumentException if the provided object is not an array.
     * @throws ArrayIndexOutOfBoundsException if the index is out of the array's bounds.
     * @see java.lang.reflect.Array#setDouble(Object, int, double)
     */
    public static void setDouble(final Object array, final int index, final double d) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        java.lang.reflect.Array.setDouble(array, index, d);
    }

    /**
     * Returns a fixed-size list backed by the specified array if it's not {@code null} or empty, 
     * otherwise an immutable/unmodifiable empty list is returned.
     *
     * <p>This method provides a null-safe wrapper around {@link Arrays#asList(Object...)} that
     * handles empty and {@code null} arrays gracefully by returning an empty list instead of a list
     * backed by an empty array. This can help avoid potential issues with list operations
     * on empty arrays.</p>
     *
     * <p>The returned list is:</p>
     * <ul>
     *   <li>Fixed-size - cannot be resized (no add/remove operations)</li>
     *   <li>Backed by the original array - changes to the list affect the array and vice versa</li>
     *   <li>Immutable empty list if the input array is {@code null} or empty</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * List<String> list = Array.asList(array);
     * list.set(0, "x"); // Modifies both list and original array
     * 
     * String[] emptyArray = {};
     * List<String> emptyList = Array.asList(emptyArray); // Returns empty list
     * 
     * String[] nullArray = null;
     * List<String> nullList = Array.asList(nullArray); // Returns empty list
     * }</pre>
     *
     * @param <T> the type of elements in the array and returned list
     * @param a the array to be converted to a list. Can be {@code null} or empty.
     * @return a fixed-size list backed by the specified array, or an empty list if the array is {@code null} or empty
     * @see Arrays#asList(Object...)
     * @see N#asList(Object...)
     * @see List#of(Object...)
     * @see N#emptyList()
     * @since 1.0
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(@NullSafe final T... a) {
        return N.isEmpty(a) ? N.emptyList() : Arrays.asList(a);
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = Array.of(true, false, true); // returns boolean array {true, false, true}
     * boolean[] empty = Array.of(); // returns empty boolean array
     * }</pre>
     *
     * @param a the input array of booleans
     * @return the same input array
     */
    public static boolean[] of(final boolean... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] array = Array.of('a', 'b', 'c'); // returns char array {'a', 'b', 'c'}
     * char[] vowels = Array.of('a', 'e', 'i', 'o', 'u'); // returns char array of vowels
     * }</pre>
     *
     * @param a the input array of characters
     * @return the same input array
     */
    public static char[] of(final char... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] array = Array.of((byte) 1, (byte) 2, (byte) 3); // returns byte array {1, 2, 3}
     * byte[] empty = Array.of(); // returns empty byte array
     * }</pre>
     *
     * @param a the input array of bytes
     * @return the same input array
     */
    public static byte[] of(final byte... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array = Array.of((short) 10, (short) 20, (short) 30); // returns short array {10, 20, 30}
     * short[] empty = Array.of(); // returns empty short array
     * }</pre>
     *
     * @param a the input array of shorts
     * @return the same input array
     */
    public static short[] of(final short... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = Array.of(1, 2, 3, 4, 5); // returns int array {1, 2, 3, 4, 5}
     * int[] primes = Array.of(2, 3, 5, 7, 11); // returns int array of prime numbers
     * }</pre>
     *
     * @param a the input array of integers
     * @return the same input array
     */
    public static int[] of(final int... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = Array.of(1L, 2L, 3L); // returns long array {1L, 2L, 3L}
     * long[] timestamps = Array.of(1609459200000L, 1612137600000L); // returns long array of timestamps
     * }</pre>
     *
     * @param a the input array of longs
     * @return the same input array
     */
    public static long[] of(final long... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = Array.of(1.1f, 2.2f, 3.3f); // returns float array {1.1f, 2.2f, 3.3f}
     * float[] empty = Array.of(); // returns empty float array
     * }</pre>
     *
     * @param a the input array of floats
     * @return the same input array
     */
    public static float[] of(final float... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] array = Array.of(1.1, 2.2, 3.3); // returns double array {1.1, 2.2, 3.3}
     * double[] prices = Array.of(19.99, 29.99, 39.99); // returns double array of prices
     * }</pre>
     *
     * @param a the input array of doubles
     * @return the same input array
     */
    public static double[] of(final double... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = Array.of("apple", "banana", "cherry"); // returns String array
     * String[] names = Array.of("Alice", "Bob", "Charlie"); // returns String array of names
     * }</pre>
     *
     * @param a the input array of strings
     * @return the same input array
     * @see N#asArray(Object...)
     */
    public static String[] of(final String... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.util.Date> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.util.Calendar> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends java.time.temporal.Temporal> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @see N#asArray(Object...)
     */
    @SafeVarargs
    public static <T extends Enum<?>> T[] of(final T... a) {
        return a;
    }

    /**
     * Returns the input array as-is without any modification or copying.
     *
     * @param <T> the type of the elements in the array
     * @param a the input array
     * @return the same input array
     * @deprecated replaced by {@code N.asArray(Object...)}.
     * @see N#asArray(Object...)
     */
    @Deprecated
    @SafeVarargs
    public static <T> T[] oF(final T... a) { //NOSONAR
        return a;
    }

    /**
     * Generates a range of characters from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to, but not including, the <i>endExclusive</i> character.
     * The characters are generated in ascending order. If the start is greater than or equal to the end, an empty array is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = Array.range('a', 'e'); // returns {'a', 'b', 'c', 'd'}
     * char[] digits = Array.range('0', '5'); // returns {'0', '1', '2', '3', '4'}
     * char[] empty = Array.range('z', 'a'); // returns empty array
     * }</pre>
     *
     * @param startInclusive the first character (inclusive) in the char array.
     * @param endExclusive the upper bound (exclusive) of the char array.
     * @return a char array containing characters from <i>startInclusive</i> to <i>endExclusive</i>, or an empty array if startInclusive &gt;= endExclusive.
     */
    public static char[] range(char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] a = new char[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to, but not including, the <i>endExclusive</i> byte.
     * The bytes are generated in ascending order. If the start is greater than or equal to the end, an empty array is returned.
     *
     * @param startInclusive the first byte (inclusive) in the byte array.
     * @param endExclusive the upper bound (exclusive) of the byte array.
     * @return a byte array containing bytes from <i>startInclusive</i> to <i>endExclusive</i>, or an empty array if startInclusive &gt;= endExclusive.
     */
    public static byte[] range(byte startInclusive, final byte endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] a = new byte[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to, but not including, the <i>endExclusive</i> short integer.
     * The short integers are generated in ascending order. If the start is greater than or equal to the end, an empty array is returned.
     *
     * @param startInclusive the first short integer (inclusive) in the short array.
     * @param endExclusive the upper bound (exclusive) of the short array.
     * @return a short array containing short integers from <i>startInclusive</i> to <i>endExclusive</i>, or an empty array if startInclusive &gt;= endExclusive.
     */
    public static short[] range(short startInclusive, final short endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] a = new short[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to, but not including, the <i>endExclusive</i> integer.
     * The integers are generated in ascending order. If the start is greater than or equal to the end, an empty array is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] nums = Array.range(0, 5); // returns {0, 1, 2, 3, 4}
     * int[] range = Array.range(10, 15); // returns {10, 11, 12, 13, 14}
     * int[] empty = Array.range(5, 5); // returns empty array
     * }</pre>
     *
     * @param startInclusive the first integer (inclusive) in the integer array.
     * @param endExclusive the upper bound (exclusive) of the integer array.
     * @return an integer array containing integers from <i>startInclusive</i> to <i>endExclusive</i>, or an empty array if startInclusive &gt;= endExclusive.
     * @throws IllegalArgumentException if the range size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static int[] range(int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_INT_ARRAY;
        }

        if ((long) endExclusive - startInclusive > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow"); //NOSONAR
        }

        final int[] a = new int[endExclusive - startInclusive];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (exclusive).
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to, but not including, the <i>endExclusive</i> long integer.
     * The long integers are generated in ascending order. If the start is greater than or equal to the end, an empty array is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] nums = Array.range(0L, 5L); // returns {0L, 1L, 2L, 3L, 4L}
     * long[] range = Array.range(100L, 105L); // returns {100L, 101L, 102L, 103L, 104L}
     * long[] empty = Array.range(5L, 5L); // returns empty array
     * }</pre>
     *
     * @param startInclusive the first long integer (inclusive) in the long array.
     * @param endExclusive the upper bound (exclusive) of the long array.
     * @return a long array containing long integers from <i>startInclusive</i> to <i>endExclusive</i>, or an empty array if startInclusive &gt;= endExclusive.
     * @throws IllegalArgumentException if the range size is negative or exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static long[] range(long startInclusive, final long endExclusive) {
        if (startInclusive >= endExclusive) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long range = endExclusive - startInclusive;

        if (range < 0 || range > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) range];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of characters from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to, but not including, the <i>endExclusive</i> character.
     * The characters are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     *
     * @param startInclusive the first character (inclusive) in the char array.
     * @param endExclusive the upper bound (exclusive) of the char array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent character.
     * @return a char array containing characters from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static char[] range(char startInclusive, final char endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero"); //NOSONAR
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final char[] a = new char[len];
        final char byChar = (char) by;

        for (int i = 0; i < len; i++, startInclusive += byChar) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to, but not including, the <i>endExclusive</i> byte.
     * The bytes are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     *
     * @param startInclusive the first byte (inclusive) in the byte array.
     * @param endExclusive the upper bound (exclusive) of the byte array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent byte.
     * @return a byte array containing bytes from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static byte[] range(byte startInclusive, final byte endExclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to, but not including, the <i>endExclusive</i> short integer.
     * The short integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     *
     * @param startInclusive the first short integer (inclusive) in the short array.
     * @param endExclusive the upper bound (exclusive) of the short array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent short integer.
     * @return a short array containing short integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static short[] range(short startInclusive, final short endExclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to, but not including, the <i>endExclusive</i> integer.
     * The integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     *
     * @param startInclusive the first integer (inclusive) in the integer array.
     * @param endExclusive the upper bound (exclusive) of the integer array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent integer.
     * @return an integer array containing integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero or if the resulting array size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static int[] range(int startInclusive, final int endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_INT_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        final long len = ((long) endExclusive - startInclusive) / by + (((long) endExclusive - startInclusive) % by == 0 ? 0 : 1);

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (exclusive) with a specific step.
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to, but not including, the <i>endExclusive</i> long integer.
     * The long integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * This method handles potential overflow scenarios using BigInteger for large ranges.
     *
     * @param startInclusive the first long integer (inclusive) in the long array.
     * @param endExclusive the upper bound (exclusive) of the long array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent long integer.
     * @return a long array containing long integers from <i>startInclusive</i> to <i>endExclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero or if the resulting array size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static long[] range(long startInclusive, final long endExclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        //        if (endExclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endExclusive' (" + endExclusive + ") are not consistent with by (" + by + ").");
        //        }

        long len = 0;

        if ((by > 0 && endExclusive - startInclusive < 0) || (by < 0 && startInclusive - endExclusive < 0)) {
            final BigInteger m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(by));

            if (m.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("Overflow. Array size is too large to allocate: " + m);
            }

            len = m.multiply(BigInteger.valueOf(by)).add(BigInteger.valueOf(startInclusive)).equals(BigInteger.valueOf(endExclusive)) ? m.longValue()
                    : m.longValue() + 1;
        } else {
            len = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);
        }

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of characters from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to and including the <i>endInclusive</i> character.
     * The characters are generated in ascending order. If start is greater than end, an empty array is returned.
     * If start equals end, a single-element array containing that value is returned.
     *
     * @param startInclusive the first character (inclusive) in the char array.
     * @param endInclusive the upper bound (inclusive) of the char array.
     * @return a char array containing characters from <i>startInclusive</i> to <i>endInclusive</i>, or an empty array if startInclusive &gt; endInclusive.
     */
    public static char[] rangeClosed(char startInclusive, final char endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final char[] a = new char[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to and including the <i>endInclusive</i> byte.
     * The bytes are generated in ascending order. If start is greater than end, an empty array is returned.
     * If start equals end, a single-element array containing that value is returned.
     *
     * @param startInclusive the first byte (inclusive) in the byte array.
     * @param endInclusive the upper bound (inclusive) of the byte array.
     * @return a byte array containing bytes from <i>startInclusive</i> to <i>endInclusive</i>, or an empty array if startInclusive &gt; endInclusive.
     */
    public static byte[] rangeClosed(byte startInclusive, final byte endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final byte[] a = new byte[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to and including the <i>endInclusive</i> short integer.
     * The short integers are generated in ascending order. If start is greater than end, an empty array is returned.
     * If start equals end, a single-element array containing that value is returned.
     *
     * @param startInclusive the first short integer (inclusive) in the short array.
     * @param endInclusive the upper bound (inclusive) of the short array.
     * @return a short array containing short integers from <i>startInclusive</i> to <i>endInclusive</i>, or an empty array if startInclusive &gt; endInclusive.
     */
    public static short[] rangeClosed(short startInclusive, final short endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final short[] a = new short[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to and including the <i>endInclusive</i> integer.
     * The integers are generated in ascending order. If start is greater than end, an empty array is returned.
     * If start equals end, a single-element array containing that value is returned.
     *
     * @param startInclusive the first integer (inclusive) in the integer array.
     * @param endInclusive the upper bound (inclusive) of the integer array.
     * @return an integer array containing integers from <i>startInclusive</i> to <i>endInclusive</i>, or an empty array if startInclusive &gt; endInclusive.
     * @throws IllegalArgumentException if the range size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static int[] rangeClosed(int startInclusive, final int endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_INT_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        if ((long) endInclusive - startInclusive + 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[endInclusive - startInclusive + 1];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (inclusive).
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to and including the <i>endInclusive</i> long integer.
     * The long integers are generated in ascending order. If start is greater than end, an empty array is returned.
     * If start equals end, a single-element array containing that value is returned.
     *
     * @param startInclusive the first long integer (inclusive) in the long array.
     * @param endInclusive the upper bound (inclusive) of the long array.
     * @return a long array containing long integers from <i>startInclusive</i> to <i>endInclusive</i>, or an empty array if startInclusive &gt; endInclusive.
     * @throws IllegalArgumentException if the range size is invalid (negative or exceeds Integer.MAX_VALUE - overflow detected).
     */
    public static long[] rangeClosed(long startInclusive, final long endInclusive) {
        if (startInclusive > endInclusive) {
            return N.EMPTY_LONG_ARRAY;
        } else if (startInclusive == endInclusive) {
            return Array.of(startInclusive);
        }

        final long range = endInclusive - startInclusive + 1;

        if (range <= 0 || range > Integer.MAX_VALUE) { // Check the final length
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) range];

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = startInclusive++;
        }

        return a;
    }

    /**
     * Generates a range of characters from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new char array starting from the <i>startInclusive</i> character up to and including the <i>endInclusive</i> character.
     * The characters are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * If start equals end, a single-element array containing that value is returned regardless of the step value.
     *
     * @param startInclusive the first character (inclusive) in the char array.
     * @param endInclusive the upper bound (inclusive) of the char array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent character.
     * @return a char array containing characters from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static char[] rangeClosed(char startInclusive, final char endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new char[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive - startInclusive) / by + 1;
        final char[] a = new char[len];
        final char byChar = (char) by;

        for (int i = 0; i < len; i++, startInclusive += byChar) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of bytes from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new byte array starting from the <i>startInclusive</i> byte up to and including the <i>endInclusive</i> byte.
     * The bytes are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * If start equals end, a single-element array containing that value is returned regardless of the step value.
     *
     * @param startInclusive the first byte (inclusive) in the byte array.
     * @param endInclusive the upper bound (inclusive) of the byte array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent byte.
     * @return a byte array containing bytes from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static byte[] rangeClosed(byte startInclusive, final byte endInclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new byte[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive - startInclusive) / by + 1;
        final byte[] a = new byte[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of short integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new short array starting from the <i>startInclusive</i> short integer up to and including the <i>endInclusive</i> short integer.
     * The short integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * If start equals end, a single-element array containing that value is returned regardless of the step value.
     *
     * @param startInclusive the first short integer (inclusive) in the short array.
     * @param endInclusive the upper bound (inclusive) of the short array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent short integer.
     * @return a short array containing short integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero.
     */
    public static short[] rangeClosed(short startInclusive, final short endInclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new short[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final int len = (endInclusive - startInclusive) / by + 1;
        final short[] a = new short[len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new integer array starting from the <i>startInclusive</i> integer up to and including the <i>endInclusive</i> integer.
     * The integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * If start equals end, a single-element array containing that value is returned regardless of the step value.
     *
     * @param startInclusive the first integer (inclusive) in the integer array.
     * @param endInclusive the upper bound (inclusive) of the integer array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent integer.
     * @return an integer array containing integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero or if the resulting array size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static int[] rangeClosed(int startInclusive, final int endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new int[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_INT_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        final long len = ((long) endInclusive - startInclusive) / by + 1;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] a = new int[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a range of long integers from the start (inclusive) to the end (inclusive) with a specific step.
     *
     * <p>This method generates a new long array starting from the <i>startInclusive</i> long integer up to and including the <i>endInclusive</i> long integer.
     * The long integers are generated in ascending order if <i>by</i> is positive, and in descending order if <i>by</i> is negative.
     * If the step direction is inconsistent with the range (e.g., positive step but end &lt; start), an empty array is returned.
     * If start equals end, a single-element array containing that value is returned regardless of the step value.
     * This method handles potential overflow scenarios using BigInteger for large ranges.
     *
     * @param startInclusive the first long integer (inclusive) in the long array.
     * @param endInclusive the upper bound (inclusive) of the long array.
     * @param by the step to increment (if positive) or decrement (if negative) for each subsequent long integer.
     * @return a long array containing long integers from <i>startInclusive</i> to <i>endInclusive</i> incremented or decremented by <i>by</i>, or an empty array if the range is empty or direction is inconsistent.
     * @throws IllegalArgumentException if <i>by</i> is zero or if the resulting array size exceeds Integer.MAX_VALUE (overflow detected).
     */
    public static long[] rangeClosed(long startInclusive, final long endInclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return new long[] { startInclusive };
        } else if (endInclusive > startInclusive != by > 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        //        if (endInclusive > startInclusive != by > 0) {
        //            throw new IllegalArgumentException(
        //                    "The input 'startInclusive' (" + startInclusive + ") and 'endInclusive' (" + endInclusive + ") are not consistent with by (" + by + ").");
        //        }

        long len = 0;

        if ((by > 0 && endInclusive - startInclusive < 0) || (by < 0 && startInclusive - endInclusive < 0) || ((endInclusive - startInclusive) / by + 1 <= 0)) {
            final BigInteger m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(by));

            if (m.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("Overflow. Array size is too large to allocate: " + m);
            }

            len = m.longValue() + 1;
        } else {
            len = (endInclusive - startInclusive) / by + 1;
        }

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] a = new long[(int) len];

        for (int i = 0; i < len; i++, startInclusive += by) {
            a[i] = startInclusive;
        }

        return a;
    }

    /**
     * Generates a new boolean array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the boolean value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a boolean array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static boolean[] repeat(final boolean element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final boolean[] a = new boolean[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new boolean array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new boolean array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] original = {true, false, true};
     * boolean[] repeated = Array.repeat(original, 3);
     * // Result: {true, false, true, true, false, true, true, false, true}
     *
     * boolean[] single = {false};
     * boolean[] multiplied = Array.repeat(single, 5);
     * // Result: {false, false, false, false, false}
     *
     * // Edge case: empty array
     * boolean[] empty = {};
     * boolean[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input boolean array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new boolean array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     * @see #repeat(boolean, int)
     */
    public static boolean[] repeat(final boolean[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final boolean[] ret = new boolean[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new char array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the char value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a char array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static char[] repeat(final char element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final char[] a = new char[n];
        N.fill(a, element);
        return a;
    }

    /** 
     * Generates a new char array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new char array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] original = {'a', 'b', 'c'};
     * char[] repeated = Array.repeat(original, 3);
     * // Result: {'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c'}
     *
     * char[] single = {'x'};
     * char[] multiplied = Array.repeat(single, 5);
     * // Result: {'x', 'x', 'x', 'x', 'x'}
     *
     * // Edge case: empty array
     * char[] empty = {};
     * char[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input char array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new char array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static char[] repeat(final char[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final char[] ret = new char[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new byte array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the byte value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a byte array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static byte[] repeat(final byte element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final byte[] a = new byte[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new byte array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new byte array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] original = {1, 2, 3};
     * byte[] repeated = Array.repeat(original, 3);
     * // Result: {1, 2, 3, 1, 2, 3, 1, 2, 3}
     *
     * byte[] single = {0};
     * byte[] multiplied = Array.repeat(single, 5);
     * // Result: {0, 0, 0, 0, 0}
     *
     * // Edge case: empty array
     * byte[] empty = {};
     * byte[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input byte array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new byte array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static byte[] repeat(final byte[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final byte[] ret = new byte[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new short array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the short value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a short array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static short[] repeat(final short element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final short[] a = new short[n];
        N.fill(a, element);
        return a;
    }

    /**     
     * Generates a new short array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new short array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] original = {1, 2, 3};
     * short[] repeated = Array.repeat(original, 3);
     * // Result: {1, 2, 3, 1, 2, 3, 1, 2, 3}
     *
     * short[] single = {0};
     * short[] multiplied = Array.repeat(single, 5);
     * // Result: {0, 0, 0, 0, 0}
     *
     * // Edge case: empty array
     * short[] empty = {};
     * short[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input short array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new short array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static short[] repeat(final short[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final short[] ret = new short[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new integer array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the integer value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return an integer array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static int[] repeat(final int element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final int[] a = new int[n];
        N.fill(a, element);
        return a;
    }

    /**     
     * Generates a new integer array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new integer array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] original = {1, 2, 3};
     * int[] repeated = Array.repeat(original, 3);
     * // Result: {1, 2, 3, 1, 2, 3, 1, 2, 3}
     *
     * int[] single = {0};
     * int[] multiplied = Array.repeat(single, 5);
     * // Result: {0, 0, 0, 0, 0}
     *
     * // Edge case: empty array
     * int[] empty = {};
     * int[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input integer array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new integer array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static int[] repeat(final int[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final int[] ret = new int[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new long array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the long value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a long array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static long[] repeat(final long element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final long[] a = new long[n];
        N.fill(a, element);
        return a;
    }

    /**     
     * Generates a new long array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new long array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] original = {1L, 2L, 3L};
     * long[] repeated = Array.repeat(original, 3);
     * // Result: {1L, 2L, 3L, 1L, 2L, 3L, 1L, 2L, 3L}
     *
     * long[] single = {0L};
     * long[] multiplied = Array.repeat(single, 5);
     * // Result: {0L, 0L, 0L, 0L, 0L}
     *
     * // Edge case: empty array
     * long[] empty = {};
     * long[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input long array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new long array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static long[] repeat(final long[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final long[] ret = new long[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new float array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the float value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a float array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static float[] repeat(final float element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final float[] a = new float[n];
        N.fill(a, element);
        return a;
    }

    /**     
     * Generates a new float array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new float array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] original = {1.1f, 2.2f, 3.3f};
     * float[] repeated = Array.repeat(original, 3);
     * // Result: {1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f}
     *
     * float[] single = {0.0f};
     * float[] multiplied = Array.repeat(single, 5);
     * // Result: {0.0f, 0.0f, 0.0f, 0.0f, 0.0f}
     *
     * // Edge case: empty array
     * float[] empty = {};
     * float[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input float array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new float array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static float[] repeat(final float[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final float[] ret = new float[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new double array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the double value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a double array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static double[] repeat(final double element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final double[] a = new double[n];
        N.fill(a, element);
        return a;
    }

    /**     
     * Generates a new double array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new double array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] original = {1.1, 2.2, 3.3};
     * double[] repeated = Array.repeat(original, 3);
     * // Result: {1.1, 2.2, 3.3, 1.1, 2.2, 3.3, 1.1, 2.2, 3.3}
     *
     * double[] single = {0.0};
     * double[] multiplied = Array.repeat(single, 5);
     * // Result: {0.0, 0.0, 0.0, 0.0, 0.0}
     *
     * // Edge case: empty array
     * double[] empty = {};
     * double[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input double array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new double array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static double[] repeat(final double[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final double[] ret = new double[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new String array of a specified length, with all elements set to the <i>element</i> value.
     *
     * @param element the String value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return a String array of length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if n is negative
     */
    public static String[] repeat(final String element, final int n) {
        N.checkArgNotNegative(n, cs.n);

        final String[] a = new String[n];
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new String array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new String array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] original = {"a", "b", "c"};
     * String[] repeated = Array.repeat(original, 3);
     * // Result: {"a", "b", "c", "a", "b", "c", "a", "b", "c"}
     *
     * String[] single = {"x"};
     * String[] multiplied = Array.repeat(single, 5);
     * // Result: {"x", "x", "x", "x", "x"}
     *
     * // Edge case: empty array
     * String[] empty = {};
     * String[] result = Array.repeat(empty, 10);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param a the input String array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @return a new String array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static String[] repeat(final String[] a, final int n) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return N.EMPTY_STRING_ARRAY;
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final String[] ret = new String[(int) len];

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the type of <i>element</i>.
     *
     * @param <T> the type of the elements in the array.
     * @param element the value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @return an array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException <i>element</i> is {@code null} or {@code n} is negative.
     * @deprecated prefer to {@link Array#repeatNonNull(Object, int)} or {@link Array#repeat(Object, int, Class)} 
     *  because this method throws NullPointerException when element is {@code null}
     * @see #repeat(Object, int, Class)
     * @see #repeatNonNull(Object, int)
     * @see N#repeat(Object, int)
     */
    @Deprecated
    public static <T> T[] repeat(final T element, final int n) throws IllegalArgumentException {
        N.checkArgNotNull(element, cs.element);
        N.checkArgNotNegative(n, cs.n);

        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the <i>elementClass</i> parameter.
     *
     * @param <T> the type of the elements in the array.
     * @param element the value to be repeated in the array.
     * @param n the length of the array to be generated.
     * @param elementClass the class of the elements in the array.
     * @return an array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if <i>n</i> is negative.
     * @see N#repeat(Object, int)
     */
    public static <T> T[] repeat(final T element, final int n, final Class<? extends T> elementClass) {
        N.checkArgNotNegative(n, cs.n);

        final T[] a = N.newArray(elementClass, n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates a new array by repeating the input array a specified number of times.
     *
     * <p>This method creates a new array where the input array is repeated consecutively
     * for the specified number of times. The resulting array will have a length equal to the
     * original array length multiplied by the repetition count.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] original = {1, 2, 3};
     * Integer[] repeated = Array.repeat(original, 3, Integer.class);
     * // Result: {1, 2, 3, 1, 2, 3, 1, 2, 3}
     *
     * Integer[] single = {0};
     * Integer[] multiplied = Array.repeat(single, 5, Integer.class);
     * // Result: {0, 0, 0, 0, 0}
     *
     * // Edge case: empty array
     * Integer[] empty = {};
     * Integer[] result = Array.repeat(empty, 10, Integer.class);
     * // Result: {} (empty array)
     * }</pre>
     *
     * @param <T> the type of the elements in the array.
     * @param a the input array to be repeated
     * @param n the number of times to repeat the array (must be non-negative)
     * @param elementClass the class of the elements in the array.
     * @return a new array containing the input array repeated n times
     * @throws IllegalArgumentException if n is negative or if the resulting length exceeds Integer.MAX_VALUE
     */
    public static <T> T[] repeat(final T[] a, final int n, final Class<? extends T> elementClass) {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(a)) {
            return Array.newInstance(elementClass, 0);
        }

        final int aLen = a.length;
        final long len = aLen * (long) n;

        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("overflow");
        }

        final T[] ret = N.newArray(elementClass, (int) len);

        for (int i = 0; i < n; i++) {
            N.copy(a, 0, ret, i * aLen, aLen);
        }

        return ret;
    }

    /**
     * Generates a new array of a specified length, with all elements set to the <i>element</i> value.
     * The type of the array is determined by the type of <i>element</i>.
     *
     * <p>This method provides a clearer alternative to the deprecated {@link Array#repeat(Object, int)} method
     * by explicitly indicating in its name that {@code null} elements are not allowed. Unlike the deprecated method which
     * may throw NullPointerException, this method consistently throws IllegalArgumentException for {@code null} elements.</p>
     *
     * @param <T> the type of the elements in the array.
     * @param element the value to be repeated in the array. Must not be {@code null}.
     * @param n the length of the array to be generated. Must be non-negative.
     * @return an array of type 'T' and length <i>n</i> with all elements set to <i>element</i>.
     * @throws IllegalArgumentException if <i>element</i> is {@code null} or if <i>n</i> is negative.
     * @see #repeat(Object, int, Class)
     * @see N#repeat(Object, int)
     */
    public static <T> T[] repeatNonNull(final T element, final int n) throws IllegalArgumentException {
        N.checkArgNotNull(element, cs.element);
        N.checkArgNotNegative(n, cs.n);

        final T[] a = N.newArray(element.getClass(), n);
        N.fill(a, element);
        return a;
    }

    /**
     * Generates an array of random integers of the specified length.
     *
     * <p>Each element in the returned array is a random integer that can be any value
     * in the full range of int values (from Integer.MIN_VALUE to Integer.MAX_VALUE).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] randomInts = Array.random(5);
     * // Result: an array of 5 random integers, e.g., [-1234567, 987654321, -42, 2147483647, 0]
     * }</pre>
     *
     * @param len the length of the array to be generated. Must be non-negative.
     * @return an array of random integers of the specified length
     * @throws NegativeArraySizeException if len is negative
     * @see Random#nextInt()
     * @see IntList#random(int)
     * @see #random(int, int, int)
     */
    @Beta
    public static int[] random(final int len) {
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = N.RAND.nextInt();
        }

        return a;
    }

    /**
     * Generates an array of random integers within the specified range.
     *
     * <p>Each element in the returned array is a random integer in the range [startInclusive, endExclusive).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] randomInts = Array.random(1, 100, 5);
     * // Result: an array of 5 random integers between 1 (inclusive) and 100 (exclusive)
     * // e.g., [42, 7, 99, 23, 65]
     * }</pre>
     *
     * @param startInclusive the lower bound (inclusive) of the random integers
     * @param endExclusive the upper bound (exclusive) of the random integers
     * @param len the length of the array to be generated. Must be non-negative.
     * @return an array of random integers within the specified range
     * @throws IllegalArgumentException if startInclusive is not less than endExclusive
     * @throws NegativeArraySizeException if len is negative
     * @see Random#nextInt(int)
     * @see IntList#random(int, int, int)
     * @see #random(int)
     */
    @Beta
    public static int[] random(final int startInclusive, final int endExclusive, final int len) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final int[] a = new int[len];
        final long mod = (long) endExclusive - (long) startInclusive;

        if (mod < Integer.MAX_VALUE) {
            final int n = (int) mod;

            for (int i = 0; i < len; i++) {
                a[i] = N.RAND.nextInt(n) + startInclusive;
            }
        } else {
            for (int i = 0; i < len; i++) {
                a[i] = (int) (Math.abs(N.RAND.nextLong() % mod) + startInclusive);
            }
        }

        return a;
    }

    /**
     * Concatenates two two-dimensional boolean arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * boolean[][] a = {{true, false}, {true}};
     * boolean[][] b = {{false}, {true, true}};
     * boolean[][] result = Array.concat(a, b);
     * // result = {{true, false, false}, {true, true, true}}
     *
     * // Example 2: Arrays with different lengths
     * boolean[][] a = {{true, false}, {true}};
     * boolean[][] b = {{false}};
     * boolean[][] result = Array.concat(a, b);
     * // result = {{true, false, false}, {true}}
     *
     * // Example 3: Concatenation with empty array
     * boolean[][] a = {{true, false}};
     * boolean[][] b = new boolean[0][];
     * boolean[][] result = Array.concat(a, b);
     * // result = {{true, false}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * boolean[][] result = Array.concat(null, null);
     * // result = new boolean[0][]
     * }</pre>
     *
     * @param a the first two-dimensional boolean array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional boolean array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional boolean array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static boolean[][] concat(final boolean[][] a, final boolean[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new boolean[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final boolean[][] result = new boolean[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional boolean arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * boolean[][][] a = {{{true, false}}, {{true}}};
     * boolean[][][] b = {{{false}}, {{true, true}}};
     * boolean[][][] result = Array.concat(a, b);
     * // result = {{{true, false, false}}, {{true, true, true}}}
     *
     * // Example 2: Arrays with different lengths
     * boolean[][][] a = {{{true, false}}, {{true}}};
     * boolean[][][] b = {{{false}}};
     * boolean[][][] result = Array.concat(a, b);
     * // result = {{{true, false, false}}, {{true}}}
     *
     * // Example 3: Concatenation with empty array
     * boolean[][][] a = {{{true, false}}};
     * boolean[][][] b = new boolean[0][][];
     * boolean[][][] result = Array.concat(a, b);
     * // result = {{{true, false}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * boolean[][][] result = Array.concat(null, null);
     * // result = new boolean[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional boolean array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional boolean array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional boolean array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static boolean[][][] concat(final boolean[][][] a, final boolean[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new boolean[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final boolean[][][] result = new boolean[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional char arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * char[][] a = {{'a', 'b'}, {'c'}};
     * char[][] b = {{'d'}, {'e', 'f'}};
     * char[][] result = Array.concat(a, b);
     * // result = {{'a', 'b', 'd'}, {'c', 'e', 'f'}}
     *
     * // Example 2: Arrays with different lengths
     * char[][] a = {{'a', 'b'}, {'c'}};
     * char[][] b = {{'d'}};
     * char[][] result = Array.concat(a, b);
     * // result = {{'a', 'b', 'd'}, {'c'}}
     *
     * // Example 3: Concatenation with empty array
     * char[][] a = {{'a', 'b'}};
     * char[][] b = new char[0][];
     * char[][] result = Array.concat(a, b);
     * // result = {{'a', 'b'}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * char[][] result = Array.concat(null, null);
     * // result = new char[0][]
     * }</pre>
     *
     * @param a the first two-dimensional char array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional char array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional char array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static char[][] concat(final char[][] a, final char[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new char[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final char[][] result = new char[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional char arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * char[][][] a = {{{'a', 'b'}}, {{'c'}}};
     * char[][][] b = {{{'d'}}, {{'e', 'f'}}};
     * char[][][] result = Array.concat(a, b);
     * // result = {{{'a', 'b', 'd'}}, {{'c', 'e', 'f'}}}
     *
     * // Example 2: Arrays with different lengths
     * char[][][] a = {{{'a', 'b'}}, {{'c'}}};
     * char[][][] b = {{{'d'}}};
     * char[][][] result = Array.concat(a, b);
     * // result = {{{'a', 'b', 'd'}}, {{'c'}}}
     *
     * // Example 3: Concatenation with empty array
     * char[][][] a = {{{'a', 'b'}}};
     * char[][][] b = new char[0][][];
     * char[][][] result = Array.concat(a, b);
     * // result = {{{'a', 'b'}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * char[][][] result = Array.concat(null, null);
     * // result = new char[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional char array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional char array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional char array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static char[][][] concat(final char[][][] a, final char[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new char[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final char[][][] result = new char[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional byte arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * byte[][] a = {{1, 2}, {3}};
     * byte[][] b = {{4}, {5, 6}};
     * byte[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3, 5, 6}}
     *
     * // Example 2: Arrays with different lengths
     * byte[][] a = {{1, 2}, {3}};
     * byte[][] b = {{4}};
     * byte[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3}}
     *
     * // Example 3: Concatenation with empty array
     * byte[][] a = {{1, 2}};
     * byte[][] b = new byte[0][];
     * byte[][] result = Array.concat(a, b);
     * // result = {{1, 2}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * byte[][] result = Array.concat(null, null);
     * // result = new byte[0][]
     * }</pre>
     *
     * @param a the first two-dimensional byte array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional byte array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional byte array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static byte[][] concat(final byte[][] a, final byte[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new byte[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final byte[][] result = new byte[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional byte arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * byte[][][] a = {{{1, 2}}, {{3}}};
     * byte[][][] b = {{{4}}, {{5, 6}}};
     * byte[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3, 5, 6}}}
     *
     * // Example 2: Arrays with different lengths
     * byte[][][] a = {{{1, 2}}, {{3}}};
     * byte[][][] b = {{{4}}};
     * byte[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3}}}
     *
     * // Example 3: Concatenation with empty array
     * byte[][][] a = {{{1, 2}}};
     * byte[][][] b = new byte[0][][];
     * byte[][][] result = Array.concat(a, b);
     * // result = {{{1, 2}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * byte[][][] result = Array.concat(null, null);
     * // result = new byte[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional byte array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional byte array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional byte array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static byte[][][] concat(final byte[][][] a, final byte[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new byte[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final byte[][][] result = new byte[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional short arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * short[][] a = {{1, 2}, {3}};
     * short[][] b = {{4}, {5, 6}};
     * short[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3, 5, 6}}
     *
     * // Example 2: Arrays with different lengths
     * short[][] a = {{1, 2}, {3}};
     * short[][] b = {{4}};
     * short[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3}}
     *
     * // Example 3: Concatenation with empty array
     * short[][] a = {{1, 2}};
     * short[][] b = new short[0][];
     * short[][] result = Array.concat(a, b);
     * // result = {{1, 2}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * short[][] result = Array.concat(null, null);
     * // result = new short[0][]
     * }</pre>
     *
     * @param a the first two-dimensional short array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional short array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional short array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static short[][] concat(final short[][] a, final short[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new short[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final short[][] result = new short[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional short arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * short[][][] a = {{{1, 2}}, {{3}}};
     * short[][][] b = {{{4}}, {{5, 6}}};
     * short[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3, 5, 6}}}
     *
     * // Example 2: Arrays with different lengths
     * short[][][] a = {{{1, 2}}, {{3}}};
     * short[][][] b = {{{4}}};
     * short[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3}}}
     *
     * // Example 3: Concatenation with empty array
     * short[][][] a = {{{1, 2}}};
     * short[][][] b = new short[0][][];
     * short[][][] result = Array.concat(a, b);
     * // result = {{{1, 2}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * short[][][] result = Array.concat(null, null);
     * // result = new short[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional short array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional short array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional short array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static short[][][] concat(final short[][][] a, final short[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new short[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final short[][][] result = new short[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional integer arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * int[][] a = {{1, 2}, {3}};
     * int[][] b = {{4}, {5, 6}};
     * int[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3, 5, 6}}
     *
     * // Example 2: Arrays with different lengths
     * int[][] a = {{1, 2}, {3}};
     * int[][] b = {{4}};
     * int[][] result = Array.concat(a, b);
     * // result = {{1, 2, 4}, {3}}
     *
     * // Example 3: Concatenation with empty array
     * int[][] a = {{1, 2}};
     * int[][] b = new int[0][];
     * int[][] result = Array.concat(a, b);
     * // result = {{1, 2}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * int[][] result = Array.concat(null, null);
     * // result = new int[0][]
     * }</pre>
     *
     * @param a the first two-dimensional int array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional int array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional int array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static int[][] concat(final int[][] a, final int[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new int[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final int[][] result = new int[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional integer arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * int[][][] a = {{{1, 2}}, {{3}}};
     * int[][][] b = {{{4}}, {{5, 6}}};
     * int[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3, 5, 6}}}
     *
     * // Example 2: Arrays with different lengths
     * int[][][] a = {{{1, 2}}, {{3}}};
     * int[][][] b = {{{4}}};
     * int[][][] result = Array.concat(a, b);
     * // result = {{{1, 2, 4}}, {{3}}}
     *
     * // Example 3: Concatenation with empty array
     * int[][][] a = {{{1, 2}}};
     * int[][][] b = new int[0][][];
     * int[][][] result = Array.concat(a, b);
     * // result = {{{1, 2}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * int[][][] result = Array.concat(null, null);
     * // result = new int[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional int array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional int array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional int array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static int[][][] concat(final int[][][] a, final int[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new int[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final int[][][] result = new int[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional long arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * long[][] a = {{1L, 2L}, {3L}};
     * long[][] b = {{4L}, {5L, 6L}};
     * long[][] result = Array.concat(a, b);
     * // result = {{1L, 2L, 4L}, {3L, 5L, 6L}}
     *
     * // Example 2: Arrays with different lengths
     * long[][] a = {{1L, 2L}, {3L}};
     * long[][] b = {{4L}};
     * long[][] result = Array.concat(a, b);
     * // result = {{1L, 2L, 4L}, {3L}}
     *
     * // Example 3: Concatenation with empty array
     * long[][] a = {{1L, 2L}};
     * long[][] b = new long[0][];
     * long[][] result = Array.concat(a, b);
     * // result = {{1L, 2L}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * long[][] result = Array.concat(null, null);
     * // result = new long[0][]
     * }</pre>
     *
     * @param a the first two-dimensional long array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional long array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional long array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static long[][] concat(final long[][] a, final long[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new long[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final long[][] result = new long[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional long arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * long[][][] a = {{{1L, 2L}}, {{3L}}};
     * long[][][] b = {{{4L}}, {{5L, 6L}}};
     * long[][][] result = Array.concat(a, b);
     * // result = {{{1L, 2L, 4L}}, {{3L, 5L, 6L}}}
     *
     * // Example 2: Arrays with different lengths
     * long[][][] a = {{{1L, 2L}}, {{3L}}};
     * long[][][] b = {{{4L}}};
     * long[][][] result = Array.concat(a, b);
     * // result = {{{1L, 2L, 4L}}, {{3L}}}
     *
     * // Example 3: Concatenation with empty array
     * long[][][] a = {{{1L, 2L}}};
     * long[][][] b = new long[0][][];
     * long[][][] result = Array.concat(a, b);
     * // result = {{{1L, 2L}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * long[][][] result = Array.concat(null, null);
     * // result = new long[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional long array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional long array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional long array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static long[][][] concat(final long[][][] a, final long[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new long[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final long[][][] result = new long[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional float arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * float[][] a = {{1.0f, 2.0f}, {3.0f}};
     * float[][] b = {{4.0f}, {5.0f, 6.0f}};
     * float[][] result = Array.concat(a, b);
     * // result = {{1.0f, 2.0f, 4.0f}, {3.0f, 5.0f, 6.0f}}
     *
     * // Example 2: Arrays with different lengths
     * float[][] a = {{1.0f, 2.0f}, {3.0f}};
     * float[][] b = {{4.0f}};
     * float[][] result = Array.concat(a, b);
     * // result = {{1.0f, 2.0f, 4.0f}, {3.0f}}
     *
     * // Example 3: Concatenation with empty array
     * float[][] a = {{1.0f, 2.0f}};
     * float[][] b = new float[0][];
     * float[][] result = Array.concat(a, b);
     * // result = {{1.0f, 2.0f}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * float[][] result = Array.concat(null, null);
     * // result = new float[0][]
     * }</pre>
     *
     * @param a the first two-dimensional float array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional float array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional float array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static float[][] concat(final float[][] a, final float[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new float[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final float[][] result = new float[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional float arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * float[][][] a = {{{1.0f, 2.0f}}, {{3.0f}}};
     * float[][][] b = {{{4.0f}}, {{5.0f, 6.0f}}};
     * float[][][] result = Array.concat(a, b);
     * // result = {{{1.0f, 2.0f, 4.0f}}, {{3.0f, 5.0f, 6.0f}}}
     *
     * // Example 2: Arrays with different lengths
     * float[][][] a = {{{1.0f, 2.0f}}, {{3.0f}}};
     * float[][][] b = {{{4.0f}}};
     * float[][][] result = Array.concat(a, b);
     * // result = {{{1.0f, 2.0f, 4.0f}}, {{3.0f}}}
     *
     * // Example 3: Concatenation with empty array
     * float[][][] a = {{{1.0f, 2.0f}}};
     * float[][][] b = new float[0][][];
     * float[][][] result = Array.concat(a, b);
     * // result = {{{1.0f, 2.0f}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * float[][][] result = Array.concat(null, null);
     * // result = new float[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional float array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional float array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional float array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static float[][][] concat(final float[][][] a, final float[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new float[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final float[][][] result = new float[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional double arrays element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional arrays
     * double[][] a = {{1.0, 2.0}, {3.0}};
     * double[][] b = {{4.0}, {5.0, 6.0}};
     * double[][] result = Array.concat(a, b);
     * // result = {{1.0, 2.0, 4.0}, {3.0, 5.0, 6.0}}
     *
     * // Example 2: Arrays with different lengths
     * double[][] a = {{1.0, 2.0}, {3.0}};
     * double[][] b = {{4.0}};
     * double[][] result = Array.concat(a, b);
     * // result = {{1.0, 2.0, 4.0}, {3.0}}
     *
     * // Example 3: Concatenation with empty array
     * double[][] a = {{1.0, 2.0}};
     * double[][] b = new double[0][];
     * double[][] result = Array.concat(a, b);
     * // result = {{1.0, 2.0}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * double[][] result = Array.concat(null, null);
     * // result = new double[0][]
     * }</pre>
     *
     * @param a the first two-dimensional double array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second two-dimensional double array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new two-dimensional double array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     */
    public static double[][] concat(final double[][] a, final double[][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new double[0][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final double[][] result = new double[maxLen][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional double arrays element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concat method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional arrays
     * double[][][] a = {{{1.0, 2.0}}, {{3.0}}};
     * double[][][] b = {{{4.0}}, {{5.0, 6.0}}};
     * double[][][] result = Array.concat(a, b);
     * // result = {{{1.0, 2.0, 4.0}}, {{3.0, 5.0, 6.0}}}
     *
     * // Example 2: Arrays with different lengths
     * double[][][] a = {{{1.0, 2.0}}, {{3.0}}};
     * double[][][] b = {{{4.0}}};
     * double[][][] result = Array.concat(a, b);
     * // result = {{{1.0, 2.0, 4.0}}, {{3.0}}}
     *
     * // Example 3: Concatenation with empty array
     * double[][][] a = {{{1.0, 2.0}}};
     * double[][][] b = new double[0][][];
     * double[][][] result = Array.concat(a, b);
     * // result = {{{1.0, 2.0}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * double[][][] result = Array.concat(null, null);
     * // result = new double[0][][]
     * }</pre>
     *
     * @param a the first three-dimensional double array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned (or an empty array if both are null/empty).
     * @param b the second three-dimensional double array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned (or an empty array if both are null/empty).
     * @return a new three-dimensional double array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     */
    public static double[][][] concat(final double[][][] a, final double[][][] b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? new double[0][][] : N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final double[][][] result = new double[maxLen][][];

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two two-dimensional arrays of generic type T element-wise by combining their corresponding row elements.
     *
     * <p>This method performs element-wise concatenation by merging rows at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each row index:
     * <ul>
     *   <li>If both arrays have a row at that index, the rows are concatenated together</li>
     *   <li>If only one array has a row at that index, that row is used in the result</li>
     *   <li>If neither array has a row at that index (both are null), the result row is an empty array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully. The method preserves the component type of the input arrays in the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two two-dimensional String arrays
     * String[][] a = {{"a", "b"}, {"c"}};
     * String[][] b = {{"d"}, {"e", "f"}};
     * String[][] result = Array.concatt(a, b);
     * // result = {{"a", "b", "d"}, {"c", "e", "f"}}
     *
     * // Example 2: Arrays with different lengths
     * Integer[][] a = {{1, 2}, {3}};
     * Integer[][] b = {{4}};
     * Integer[][] result = Array.concatt(a, b);
     * // result = {{1, 2, 4}, {3}}
     *
     * // Example 3: Concatenation with empty array
     * String[][] a = {{"hello", "world"}};
     * String[][] b = new String[0][];
     * String[][] result = Array.concatt(a, b);
     * // result = {{"hello", "world"}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * String[][] result = Array.concatt(null, null);
     * // result = null
     * }</pre>
     *
     * @param <T> the component type of the elements in the arrays.
     * @param a the first two-dimensional array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned.
     * @param b the second two-dimensional array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned.
     * @return a new two-dimensional array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each row in the result is the concatenation of the corresponding rows from {@code a} and {@code b}.
     *         Returns {@code null} if both input arrays are {@code null}.
     */
    public static <T> T[][] concatt(final T[][] a, final T[][] b) {
        if (N.isEmpty(a)) {
            return N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final T[][] result = newInstance(a.getClass().getComponentType(), maxLen);

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Concatenates two three-dimensional arrays of generic type T element-wise by combining their corresponding two-dimensional layer elements.
     *
     * <p>This method performs element-wise concatenation by recursively merging two-dimensional layers at the same index position from both input arrays.
     * The resulting array's length equals the maximum length of the two input arrays. For each layer index:
     * <ul>
     *   <li>If both arrays have a layer at that index, the layers are concatenated using the two-dimensional concatt method</li>
     *   <li>If only one array has a layer at that index, that layer is used in the result</li>
     *   <li>If neither array has a layer at that index (both are null), the result layer is an empty two-dimensional array</li>
     * </ul>
     *
     * <p>The operation creates a new array and does not modify the input arrays. Both input arrays can be {@code null} or empty,
     * which will be handled gracefully. The method preserves the component type of the input arrays in the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Basic concatenation of two three-dimensional String arrays
     * String[][][] a = {{{"a", "b"}}, {{"c"}}};
     * String[][][] b = {{{"d"}}, {{"e", "f"}}};
     * String[][][] result = Array.concatt(a, b);
     * // result = {{{"a", "b", "d"}}, {{"c", "e", "f"}}}
     *
     * // Example 2: Arrays with different lengths
     * Integer[][][] a = {{{1, 2}}, {{3}}};
     * Integer[][][] b = {{{4}}};
     * Integer[][][] result = Array.concatt(a, b);
     * // result = {{{1, 2, 4}}, {{3}}}
     *
     * // Example 3: Concatenation with empty array
     * String[][][] a = {{{"hello", "world"}}};
     * String[][][] b = new String[0][][];
     * String[][][] result = Array.concatt(a, b);
     * // result = {{{"hello", "world"}}} (clone of a)
     *
     * // Example 4: Both arrays are null/empty
     * String[][][] result = Array.concatt(null, null);
     * // result = null
     * }</pre>
     *
     * @param <T> the component type of the elements in the arrays.
     * @param a the first three-dimensional array to concatenate. Can be {@code null} or empty, in which case a clone of {@code b} is returned.
     * @param b the second three-dimensional array to concatenate. Can be {@code null} or empty, in which case a clone of {@code a} is returned.
     * @return a new three-dimensional array containing the element-wise concatenation of the input arrays. The length equals max(a.length, b.length).
     *         Each two-dimensional layer in the result is the concatenation of the corresponding layers from {@code a} and {@code b}.
     *         Returns {@code null} if both input arrays are {@code null}.
     */
    public static <T> T[][][] concatt(final T[][][] a, final T[][][] b) {
        if (N.isEmpty(a)) {
            return N.clone(b);
        } else if (N.isEmpty(b)) {
            return N.clone(a);
        }

        final int maxLen = N.max(N.len(a), N.len(b));
        final T[][][] result = newInstance(a.getClass().getComponentType(), maxLen);

        for (int i = 0, aLen = N.len(a), bLen = N.len(b); i < maxLen; i++) {
            result[i] = concatt(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        }

        return result;
    }

    /**
     * Converts an array of primitive booleans to an array of Boolean objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] primitives = {true, false, true};
     * Boolean[] objects = Array.box(primitives); // returns {Boolean.TRUE, Boolean.FALSE, Boolean.TRUE}
     * Boolean[] nullResult = Array.box(null); // returns null
     * }</pre>
     *
     * @param a the array of primitive booleans to be converted. May be {@code null}.
     * @return an array of Boolean objects, or {@code null} if the input array is {@code null}
     */
    @MayReturnNull
    public static Boolean[] box(final boolean... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive booleans to an array of Boolean objects.
     *
     * @param a the array of primitive booleans to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Boolean objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Boolean[] box(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_OBJ_ARRAY;
        }

        final Boolean[] result = new Boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive chars to an array of Character objects.
     *
     * @param a the array of primitive chars to be converted.
     * @return an array of Character objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Character[] box(final char... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive chars to an array of Character objects.
     *
     * @param a the array of primitive chars to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Character objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Character[] box(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHAR_OBJ_ARRAY;
        }

        final Character[] result = new Character[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive bytes to an array of Byte objects.
     *
     * @param a the array of primitive bytes to be converted.
     * @return an array of Byte objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Byte[] box(final byte... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive bytes to an array of Byte objects.
     *
     * @param a the array of primitive bytes to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Byte objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Byte[] box(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_OBJ_ARRAY;
        }

        final Byte[] result = new Byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive shorts to an array of Short objects.
     *
     * @param a the array of primitive shorts to be converted.
     * @return an array of Short objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Short[] box(final short... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive shorts to an array of Short objects.
     *
     * @param a the array of primitive shorts to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Short objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Short[] box(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_OBJ_ARRAY;
        }

        final Short[] result = new Short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive integers to an array of Integer objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] primitives = {1, 2, 3};
     * Integer[] objects = Array.box(primitives); // returns {Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)}
     * Integer[] nullResult = Array.box(null); // returns null
     * }</pre>
     *
     * @param a the array of primitive integers to be converted. May be {@code null}.
     * @return an array of Integer objects, or {@code null} if the input array is {@code null}
     */
    @MayReturnNull
    public static Integer[] box(final int... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive integers to an array of Integer objects.
     *
     * @param a the array of primitive integers to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Integer objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Integer[] box(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INT_OBJ_ARRAY;
        }

        final Integer[] result = new Integer[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive longs to an array of Long objects.
     *
     * @param a the array of primitive longs to be converted.
     * @return an array of Long objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Long[] box(final long... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive longs to an array of Long objects.
     *
     * @param a the array of primitive longs to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Long objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Long[] box(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_OBJ_ARRAY;
        }

        final Long[] result = new Long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive floats to an array of Float objects.
     *
     * @param a the array of primitive floats to be converted.
     * @return an array of Float objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Float[] box(final float... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive floats to an array of Float objects.
     *
     * @param a the array of primitive floats to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Float objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Float[] box(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_OBJ_ARRAY;
        }

        final Float[] result = new Float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts an array of primitive doubles to an array of Double objects.
     *
     * @param a the array of primitive doubles to be converted.
     * @return an array of Double objects, {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    public static Double[] box(final double... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    /**
     * Converts a portion of an array of primitive doubles to an array of Double objects.
     *
     * @param a the array of primitive doubles to be converted.
     * @param fromIndex the start index of the portion to be converted.
     * @param toIndex the end index of the portion to be converted.
     * @return an array of Double objects representing the specified portion of the input array, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds.
     */
    @MayReturnNull
    public static Double[] box(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_OBJ_ARRAY;
        }

        final Double[] result = new Double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = a[j];
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive booleans to a two-dimensional array of Boolean objects.
     *
     * @param a the two-dimensional array of primitive booleans to be converted.
     * @return a two-dimensional array of Boolean objects, {@code null} if the input array is {@code null}.
     * @see #box(boolean[])
     */
    @MayReturnNull
    public static Boolean[][] box(final boolean[][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][] result = new Boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive chars to a two-dimensional array of Character objects.
     *
     * @param a the two-dimensional array of primitive chars to be converted.
     * @return a two-dimensional array of Character objects, {@code null} if the input array is {@code null}.
     * @see #box(char[])
     */
    @MayReturnNull
    public static Character[][] box(final char[][] a) {
        if (a == null) {
            return null;
        }

        final Character[][] result = new Character[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive bytes to a two-dimensional array of Byte objects.
     *
     * @param a the two-dimensional array of primitive bytes to be converted.
     * @return a two-dimensional array of Byte objects, {@code null} if the input array is {@code null}.
     * @see #box(byte[])
     */
    @MayReturnNull
    public static Byte[][] box(final byte[][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][] result = new Byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive shorts to a two-dimensional array of Short objects.
     *
     * @param a the two-dimensional array of primitive shorts to be converted.
     * @return a two-dimensional array of Short objects, {@code null} if the input array is {@code null}.
     * @see #box(short[])
     */
    @MayReturnNull
    public static Short[][] box(final short[][] a) {
        if (a == null) {
            return null;
        }

        final Short[][] result = new Short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive integers to a two-dimensional array of Integer objects.
     *
     * @param a the two-dimensional array of primitive integers to be converted.
     * @return a two-dimensional array of Integer objects, {@code null} if the input array is {@code null}.
     * @see #box(int[])
     */
    @MayReturnNull
    public static Integer[][] box(final int[][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][] result = new Integer[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive longs to a two-dimensional array of Long objects.
     *
     * @param a the two-dimensional array of primitive longs to be converted.
     * @return a two-dimensional array of Long objects, {@code null} if the input array is {@code null}.
     * @see #box(long[])
     */
    @MayReturnNull
    public static Long[][] box(final long[][] a) {
        if (a == null) {
            return null;
        }

        final Long[][] result = new Long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive floats to a two-dimensional array of Float objects.
     *
     * @param a the two-dimensional array of primitive floats to be converted.
     * @return a two-dimensional array of Float objects, {@code null} if the input array is {@code null}.
     * @see #box(float[])
     */
    @MayReturnNull
    public static Float[][] box(final float[][] a) {
        if (a == null) {
            return null;
        }

        final Float[][] result = new Float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of primitive doubles to a two-dimensional array of Double objects.
     *
     * @param a the two-dimensional array of primitive doubles to be converted.
     * @return a two-dimensional array of Double objects, {@code null} if the input array is {@code null}.
     * @see #box(double[])
     */
    @MayReturnNull
    public static Double[][] box(final double[][] a) {
        if (a == null) {
            return null;
        }

        final Double[][] result = new Double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive booleans to a three-dimensional array of Boolean objects.
     *
     * @param a the three-dimensional array of primitive booleans to be converted.
     * @return a three-dimensional array of Boolean objects, {@code null} if the input array is {@code null}.
     * @see #box(boolean[])
     * @see #box(boolean[][])
     */
    @MayReturnNull
    public static Boolean[][][] box(final boolean[][][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][][] result = new Boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive chars to a three-dimensional array of Character objects.
     *
     * @param a the three-dimensional array of primitive chars to be converted.
     * @return a three-dimensional array of Character objects, {@code null} if the input array is {@code null}.
     * @see #box(char[])
     * @see #box(char[][])
     */
    @MayReturnNull
    public static Character[][][] box(final char[][][] a) {
        if (a == null) {
            return null;
        }

        final Character[][][] result = new Character[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive bytes to a three-dimensional array of Byte objects.
     *
     * @param a the three-dimensional array of primitive bytes to be converted.
     * @return a three-dimensional array of Byte objects, {@code null} if the input array is {@code null}.
     * @see #box(byte[])
     * @see #box(byte[][])
     */
    @MayReturnNull
    public static Byte[][][] box(final byte[][][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][][] result = new Byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive shorts to a three-dimensional array of Short objects.
     *
     * @param a the three-dimensional array of primitive shorts to be converted.
     * @return a three-dimensional array of Short objects, {@code null} if the input array is {@code null}.
     * @see #box(short[])
     * @see #box(short[][])
     */
    @MayReturnNull
    public static Short[][][] box(final short[][][] a) {
        if (a == null) {
            return null;
        }

        final Short[][][] result = new Short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive integers to a three-dimensional array of Integer objects.
     *
     * @param a the three-dimensional array of primitive integers to be converted.
     * @return a three-dimensional array of Integer objects, {@code null} if the input array is {@code null}.
     * @see #box(int[])
     * @see #box(int[][])
     */
    @MayReturnNull
    public static Integer[][][] box(final int[][][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][][] result = new Integer[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive longs to a three-dimensional array of Long objects.
     *
     * @param a the three-dimensional array of primitive longs to be converted.
     * @return a three-dimensional array of Long objects, {@code null} if the input array is {@code null}.
     * @see #box(long[])
     * @see #box(long[][])
     */
    @MayReturnNull
    public static Long[][][] box(final long[][][] a) {
        if (a == null) {
            return null;
        }

        final Long[][][] result = new Long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive floats to a three-dimensional array of Float objects.
     *
     * @param a the three-dimensional array of primitive floats to be converted.
     * @return a three-dimensional array of Float objects, {@code null} if the input array is {@code null}.
     * @see #box(float[])
     * @see #box(float[][])
     */
    @MayReturnNull
    public static Float[][][] box(final float[][][] a) {
        if (a == null) {
            return null;
        }

        final Float[][][] result = new Float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of primitive doubles to a three-dimensional array of Double objects.
     *
     * @param a the three-dimensional array of primitive doubles to be converted.
     * @return a three-dimensional array of Double objects, {@code null} if the input array is {@code null}.
     * @see #box(double[])
     * @see #box(double[][])
     */
    @MayReturnNull
    public static Double[][][] box(final double[][][] a) {
        if (a == null) {
            return null;
        }

        final Double[][][] result = new Double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    // ----------------------------------------------------------------------

    /**
     * Converts an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Boolean[] objects = {Boolean.TRUE, null, Boolean.FALSE};
     * boolean[] primitives = Array.unbox(objects); // returns {true, false, false}
     * boolean[] nullResult = Array.unbox(null); // returns null
     * }</pre>
     *
     * @param a the array of Boolean objects to be converted. May be {@code null}.
     * @return an array of primitive booleans, or {@code null} if the input array is {@code null}
     * @see #unbox(Boolean[], boolean)
     * @see #unbox(Boolean[], int, int, boolean)
     */
    public static boolean[] unbox(final Boolean... a) {
        return unbox(a, false);
    }

    /**
     * Converts an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Boolean objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive booleans, {@code null} if the input array is {@code null}.
     * See #unbox(Boolean[], int, int, boolean)
     */
    @MayReturnNull
    public static boolean[] unbox(final Boolean[] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Boolean objects into an array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Boolean objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive booleans, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Boolean[], boolean)
     */
    @MayReturnNull
    public static boolean[] unbox(final Boolean[] a, final int fromIndex, final int toIndex, final boolean valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] result = new boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a the array of Character objects to be converted.
     * @return an array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], char)
     * @see #unbox(Character[], int, int, char)
     */
    public static char[] unbox(final Character... a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Character objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], int, int, char)
     */
    @MayReturnNull
    public static char[] unbox(final Character[] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Character objects into an array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Character objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive chars, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Character[], char)
     * @see #unbox(Character[])
     */
    @MayReturnNull
    public static char[] unbox(final Character[] a, final int fromIndex, final int toIndex, final char valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] result = new char[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a the array of Byte objects to be converted.
     * @return an array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], byte)
     * @see #unbox(Byte[], int, int, byte)
     */
    public static byte[] unbox(final Byte... a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Byte objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], int, int, byte)
     */
    @MayReturnNull
    public static byte[] unbox(final Byte[] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Byte objects into an array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Byte objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive bytes, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Byte[], byte)
     * @see #unbox(Byte[])
     */
    @MayReturnNull
    public static byte[] unbox(final Byte[] a, final int fromIndex, final int toIndex, final byte valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] result = new byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a the array of Short objects to be converted.
     * @return an array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], short)
     * @see #unbox(Short[], int, int, short)
     */
    public static short[] unbox(final Short... a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Short objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], int, int, short)
     */
    @MayReturnNull
    public static short[] unbox(final Short[] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Short objects into an array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Short objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive shorts, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Short[], short)
     * @see #unbox(Short[])
     */
    @MayReturnNull
    public static short[] unbox(final Short[] a, final int fromIndex, final int toIndex, final short valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] result = new short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] objects = {Integer.valueOf(1), null, Integer.valueOf(3)};
     * int[] primitives = Array.unbox(objects); // returns {1, 0, 3}
     * int[] nullResult = Array.unbox(null); // returns null
     * }</pre>
     *
     * @param a the array of Integer objects to be converted. May be {@code null}.
     * @return an array of primitive integers, or {@code null} if the input array is {@code null}
     * @see #unbox(Integer[], int)
     * @see #unbox(Integer[], int, int, int)
     */
    public static int[] unbox(final Integer... a) {
        return unbox(a, 0);
    }

    /**
     * Converts an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Integer objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[], int, int, int)
     * @see #unbox(Integer...)
     */
    @MayReturnNull
    public static int[] unbox(final Integer[] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Integer objects into an array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Integer objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive integers, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Integer[], int)
     * @see #unbox(Integer...)
     */
    @MayReturnNull
    public static int[] unbox(final Integer[] a, final int fromIndex, final int toIndex, final int valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] result = new int[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a the array of Long objects to be converted.
     * @return an array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], long)
     * @see #unbox(Long[], int, int, long)
     */
    public static long[] unbox(final Long... a) {
        return unbox(a, 0L);
    }

    /**
     * Converts an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Long objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], int, int, long)
     * @see #unbox(Long...)
     */
    @MayReturnNull
    public static long[] unbox(final Long[] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Long objects into an array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Long objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive longs, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Long[], long)
     * @see #unbox(Long...)
     */
    @MayReturnNull
    public static long[] unbox(final Long[] a, final int fromIndex, final int toIndex, final long valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long[] result = new long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a the array of Float objects to be converted.
     * @return an array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], float)
     * @see #unbox(Float[], int, int, float)
     */
    public static float[] unbox(final Float... a) {
        return unbox(a, 0f);
    }

    /**
     * Converts an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Float objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], int, int, float)
     * @see #unbox(Float...)
     */
    @MayReturnNull
    public static float[] unbox(final Float[] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Float objects into an array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Float objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive floats, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Float[], float)
     * @see #unbox(Float...)
     */
    @MayReturnNull
    public static float[] unbox(final Float[] a, final int fromIndex, final int toIndex, final float valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final float[] result = new float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a the array of Double objects to be converted.
     * @return an array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], double)
     * @see #unbox(Double[], int, int, double)
     */
    public static double[] unbox(final Double... a) {
        return unbox(a, 0d);
    }

    /**
     * Converts an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Double objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], int, int, double)
     * @see #unbox(Double...)
     */
    @MayReturnNull
    public static double[] unbox(final Double[] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    /**
     * Converts a portion of an array of Double objects into an array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the array of Double objects to be converted.
     * @param fromIndex the starting index (inclusive) in the array to be converted.
     * @param toIndex the ending index (exclusive) in the array to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return an array of primitive doubles, {@code null} if the input array is {@code null}.
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; a.length or fromIndex &gt; toIndex.
     * @see #unbox(Double[], double)
     * @see #unbox(Double...)
     */
    @MayReturnNull
    public static double[] unbox(final Double[] a, final int fromIndex, final int toIndex, final double valueForNull) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final double[] result = new double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j]);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Boolean objects into a two-dimensional array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (boolean) {@code false}.
     *
     * @param a the two-dimensional array of Boolean objects to be converted.
     * @return a two-dimensional array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][], boolean)
     * @see #unbox(Boolean[])
     */
    public static boolean[][] unbox(final Boolean[][] a) {
        return unbox(a, false);
    }

    /**
     * Converts a two-dimensional array of Boolean objects into a two-dimensional array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Boolean objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[], boolean)
     */
    @MayReturnNull
    public static boolean[][] unbox(final Boolean[][] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][] result = new boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Character objects into a two-dimensional array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a the two-dimensional array of Character objects to be converted.
     * @return a two-dimensional array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][], char)
     * @see #unbox(Character[])
     */
    public static char[][] unbox(final Character[][] a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts a two-dimensional array of Character objects into a two-dimensional array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Character objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[], char)
     */
    @MayReturnNull
    public static char[][] unbox(final Character[][] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][] result = new char[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Byte objects into a two-dimensional array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a the two-dimensional array of Byte objects to be converted.
     * @return a two-dimensional array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][], byte)
     * @see #unbox(Byte[])
     */
    public static byte[][] unbox(final Byte[][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts a two-dimensional array of Byte objects into a two-dimensional array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Byte objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[], byte)
     */
    @MayReturnNull
    public static byte[][] unbox(final Byte[][] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][] result = new byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Short objects into a two-dimensional array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a the two-dimensional array of Short objects to be converted.
     * @return a two-dimensional array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][], short)
     * @see #unbox(Short[])
     */
    public static short[][] unbox(final Short[][] a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts a two-dimensional array of Short objects into a two-dimensional array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Short objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[], short)
     */
    @MayReturnNull
    public static short[][] unbox(final Short[][] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][] result = new short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Integer objects into a two-dimensional array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * @param a the two-dimensional array of Integer objects to be converted.
     * @return a two-dimensional array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][], int)
     * @see #unbox(Integer[])
     */
    public static int[][] unbox(final Integer[][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a two-dimensional array of Integer objects into a two-dimensional array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Integer objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[], int)
     */
    @MayReturnNull
    public static int[][] unbox(final Integer[][] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][] result = new int[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Long objects into a two-dimensional array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a the two-dimensional array of Long objects to be converted.
     * @return a two-dimensional array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][], long)
     * @see #unbox(Long[])
     */
    public static long[][] unbox(final Long[][] a) {
        return unbox(a, 0L);
    }

    /**
     * Converts a two-dimensional array of Long objects into a two-dimensional array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Long objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[], long)
     */
    @MayReturnNull
    public static long[][] unbox(final Long[][] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][] result = new long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Float objects into a two-dimensional array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a the two-dimensional array of Float objects to be converted.
     * @return a two-dimensional array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][], float)
     * @see #unbox(Float[])
     */
    public static float[][] unbox(final Float[][] a) {
        return unbox(a, 0f);
    }

    /**
     * Converts a two-dimensional array of Float objects into a two-dimensional array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Float objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[], float)
     */
    @MayReturnNull
    public static float[][] unbox(final Float[][] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][] result = new float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a two-dimensional array of Double objects into a two-dimensional array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a the two-dimensional array of Double objects to be converted.
     * @return a two-dimensional array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][], double)
     * @see #unbox(Double[])
     */
    public static double[][] unbox(final Double[][] a) {
        return unbox(a, 0d);
    }

    /**
     * Converts a two-dimensional array of Double objects into a two-dimensional array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the two-dimensional array of Double objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a two-dimensional array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[], double)
     */
    @MayReturnNull
    public static double[][] unbox(final Double[][] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][] result = new double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Boolean objects into a three-dimensional array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (boolean) {@code false}.
     *
     * @param a the three-dimensional array of Boolean objects to be converted.
     * @return a three-dimensional array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][][], boolean)
     */
    public static boolean[][][] unbox(final Boolean[][][] a) {
        return unbox(a, false);
    }

    /**
     * Converts a three-dimensional array of Boolean objects into a three-dimensional array of primitive booleans.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Boolean objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive booleans, {@code null} if the input array is {@code null}.
     * @see #unbox(Boolean[][], boolean)
     */
    @MayReturnNull
    public static boolean[][][] unbox(final Boolean[][][] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][][] result = new boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Character objects into a three-dimensional array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (char) 0.
     *
     * @param a the three-dimensional array of Character objects to be converted.
     * @return a three-dimensional array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][][], char)
     */
    public static char[][][] unbox(final Character[][][] a) {
        return unbox(a, (char) 0);
    }

    /**
     * Converts a three-dimensional array of Character objects into a three-dimensional array of primitive chars.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Character objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive chars, {@code null} if the input array is {@code null}.
     * @see #unbox(Character[][], char)
     */
    @MayReturnNull
    public static char[][][] unbox(final Character[][][] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][][] result = new char[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Byte objects into a three-dimensional array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (byte) 0.
     *
     * @param a the three-dimensional array of Byte objects to be converted.
     * @return a three-dimensional array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][][], byte)
     */
    public static byte[][][] unbox(final Byte[][][] a) {
        return unbox(a, (byte) 0);
    }

    /**
     * Converts a three-dimensional array of Byte objects into a three-dimensional array of primitive bytes.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Byte objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive bytes, {@code null} if the input array is {@code null}.
     * @see #unbox(Byte[][], byte)
     */
    @MayReturnNull
    public static byte[][][] unbox(final Byte[][][] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][][] result = new byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Short objects into a three-dimensional array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (short) 0.
     *
     * @param a the three-dimensional array of Short objects to be converted.
     * @return a three-dimensional array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][][], short)
     */
    public static short[][][] unbox(final Short[][][] a) {
        return unbox(a, (short) 0);
    }

    /**
     * Converts a three-dimensional array of Short objects into a three-dimensional array of primitive shorts.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Short objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive shorts, {@code null} if the input array is {@code null}.
     * @see #unbox(Short[][], short)
     */
    @MayReturnNull
    public static short[][][] unbox(final Short[][][] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][][] result = new short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Integer objects into a three-dimensional array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (int) 0.
     *
     * @param a the three-dimensional array of Integer objects to be converted.
     * @return a three-dimensional array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][][], int)
     */
    public static int[][][] unbox(final Integer[][][] a) {
        return unbox(a, 0);
    }

    /**
     * Converts a three-dimensional array of Integer objects into a three-dimensional array of primitive integers.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Integer objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive integers, {@code null} if the input array is {@code null}.
     * @see #unbox(Integer[][], int)
     */
    @MayReturnNull
    public static int[][][] unbox(final Integer[][][] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][][] result = new int[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Long objects into a three-dimensional array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (long) 0.
     *
     * @param a the three-dimensional array of Long objects to be converted.
     * @return a three-dimensional array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][][], long)
     */
    public static long[][][] unbox(final Long[][][] a) {
        return unbox(a, 0L);
    }

    /**
     * Converts a three-dimensional array of Long objects into a three-dimensional array of primitive longs.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Long objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive longs, {@code null} if the input array is {@code null}.
     * @see #unbox(Long[][], long)
     */
    @MayReturnNull
    public static long[][][] unbox(final Long[][][] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][][] result = new long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Float objects into a three-dimensional array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (float) 0.
     *
     * @param a the three-dimensional array of Float objects to be converted.
     * @return a three-dimensional array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][][], float)
     */
    public static float[][][] unbox(final Float[][][] a) {
        return unbox(a, 0f);
    }

    /**
     * Converts a three-dimensional array of Float objects into a three-dimensional array of primitive floats.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Float objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive floats, {@code null} if the input array is {@code null}.
     * @see #unbox(Float[][], float)
     */
    @MayReturnNull
    public static float[][][] unbox(final Float[][][] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][][] result = new float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Converts a three-dimensional array of Double objects into a three-dimensional array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the default value (double) 0.
     *
     * @param a the three-dimensional array of Double objects to be converted.
     * @return a three-dimensional array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][][], double)
     */
    public static double[][][] unbox(final Double[][][] a) {
        return unbox(a, 0d);
    }

    /**
     * Converts a three-dimensional array of Double objects into a three-dimensional array of primitive doubles.
     * If a {@code null} value is encountered in the input array, it is replaced with the specified default value.
     *
     * @param a the three-dimensional array of Double objects to be converted.
     * @param valueForNull the value to be used for {@code null} values in the input array.
     * @return a three-dimensional array of primitive doubles, {@code null} if the input array is {@code null}.
     * @see #unbox(Double[][], double)
     */
    @MayReturnNull
    public static double[][][] unbox(final Double[][][] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][][] result = new double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[][] matrix = {
     *     {true, false, true},
     *     {false, true, false}
     * };
     * boolean[][] transposed = Array.transpose(matrix);
     * // Result: {{true, false}, {false, true}, {true, false}}
     * // Original matrix is 2x3, transposed is 3x2
     * }</pre>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static boolean[][] transpose(final boolean[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final boolean[][] c = new boolean[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static char[][] transpose(final char[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final char[][] c = new char[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static byte[][] transpose(final byte[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final byte[][] c = new byte[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static short[][] transpose(final short[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final short[][] c = new short[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static int[][] transpose(final int[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final int[][] c = new int[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static long[][] transpose(final long[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final long[][] c = new long[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static float[][] transpose(final float[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final float[][] c = new float[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the input two-dimensional array.
     *
     * <p>The transpose of a matrix is obtained by moving the rows data to the column and columns data to the rows.
     * If the input is a matrix of size m x n, then the output will be another matrix of size n x m.
     * This method will return {@code null} if the input array is {@code null}.</p>
     *
     * @param a the two-dimensional array to be transposed
     * @return the transposed two-dimensional array, or {@code null} if the input array is {@code null}.
     */
    @MayReturnNull
    @Beta
    public static double[][] transpose(final double[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final double[][] c = new double[cols][rows];

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    /**
     * Transposes the given two-dimensional array.
     * The original two-dimensional array is unchanged; a new two-dimensional array representing the transposed matrix is returned.
     * This method can be used to interchange the rows and columns of the two-dimensional array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[][] matrix = {
     *     {"A", "B", "C"},
     *     {"D", "E", "F"}
     * };
     * String[][] transposed = Array.transpose(matrix);
     * // Result: {{"A", "D"}, {"B", "E"}, {"C", "F"}}
     * // Original matrix is 2x3, transposed is 3x2
     * }</pre>
     *
     * @param <T> the type of the elements in the two-dimensional array.
     * @param a the original two-dimensional array to be transposed.
     * @return a new two-dimensional array representing the transposed matrix, or {@code null} if the input array is {@code null}.
     * @throws IllegalArgumentException if the input array is not a matrix.
     */
    @MayReturnNull
    @Beta
    public static <T> T[][] transpose(final T[][] a) {
        checkIfMatrixArray(a);

        if (a == null) {
            return null;
        } else if (a.length == 0) {
            return a.clone();
        }

        final int rows = a.length;
        final int cols = a[0].length;

        final T[][] c = newInstance(a[0].getClass().getComponentType(), cols, rows);

        if (rows <= cols) {
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return c;
    }

    private static void checkIfMatrixArray(final Object[] a) {
        if (a == null || a.length <= 1) {
            return;
        }

        final int cols = getLength(a[0]);

        for (int i = 1, len = a.length; i < len; i++) {
            if (getLength(a[i]) != cols) {
                throw new IllegalArgumentException("The length of sub arrays must be same");
            }
        }
    }

    /**
     * Utility class that extends Array, providing access to all Array methods.
     * This class is marked as Beta and may be subject to changes in future releases.
     *
     * @see Array
     */
    @Beta
    public static final class ArrayUtil extends Array {
        private ArrayUtil() {
            // utility class
        }
    }
}
