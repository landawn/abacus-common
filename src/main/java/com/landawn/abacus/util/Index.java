/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Predicate;

import com.landawn.abacus.util.u.OptionalInt;

/**
 * A comprehensive utility class providing index-finding operations for arrays, collections, and strings with
 * a fluent API design. This class serves as a modern alternative to traditional index-finding methods by
 * returning {@link OptionalInt} instead of primitive {@code -1} for "not found" cases, making the API more
 * null-safe, expressive, and aligned with modern Java practices.
 *
 * <p>This class provides extensive support for finding first occurrence, last occurrence, and all occurrences
 * of elements in various data structures including primitive arrays, object arrays, collections, and strings.
 * Advanced features include subarray/sublist searching, case-insensitive string searching, and predicate-based
 * filtering with high-performance implementations optimized for different data types.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Modern API Design:</b> Returns {@code OptionalInt} instead of {@code -1} for better null safety</li>
 *   <li><b>Comprehensive Type Support:</b> All primitive types, objects, collections, and strings</li>
 *   <li><b>Advanced Search Operations:</b> Subarray, sublist, and predicate-based searching</li>
 *   <li><b>High Performance:</b> Optimized algorithms with minimal overhead for different data structures</li>
 *   <li><b>Fluent API:</b> Method chaining and expressive operation names for better readability</li>
 *   <li><b>Thread Safety:</b> Stateless design ensuring safe concurrent access</li>
 *   <li><b>Range-Based Operations:</b> Support for custom start indices and search ranges</li>
 *   <li><b>Tolerance Support:</b> Floating-point comparisons with configurable tolerance values</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Null Safety:</b> {@code OptionalInt} return type eliminates magic number {@code -1}</li>
 *   <li><b>Type Safety:</b> Overloaded methods for all primitive types avoid boxing overhead</li>
 *   <li><b>Performance Focus:</b> Optimized implementations for different data structure characteristics</li>
 *   <li><b>Consistency:</b> Uniform method naming and parameter conventions across all operations</li>
 *   <li><b>Expressiveness:</b> Clear method names that describe the operation being performed</li>
 * </ul>
 *
 * <p><b>Core Method Families:</b>
 * <ul>
 *   <li><b>{@code of} Methods:</b> Find the index of the first occurrence of an element or pattern</li>
 *   <li><b>{@code last} Methods:</b> Find the index of the last occurrence of an element or pattern</li>
 *   <li><b>{@code ofSubArray} Methods:</b> Find the starting index of a subarray within a larger array</li>
 *   <li><b>{@code lastOfSubArray} Methods:</b> Find the last starting index of a subarray within a larger array</li>
 *   <li><b>{@code ofSubList} Methods:</b> Find the starting index of a sublist within a larger list</li>
 *   <li><b>{@code lastOfSubList} Methods:</b> Find the last starting index of a sublist within a larger list</li>
 *   <li><b>{@code allOf} Methods:</b> Find all indices where an element or predicate matches</li>
 * </ul>
 *
 * <p><b>Supported Data Types:</b>
 * <ul>
 *   <li><b>Primitive Arrays:</b> {@code boolean[]}, {@code char[]}, {@code byte[]}, {@code short[]}, {@code int[]}, {@code long[]}, {@code float[]}, {@code double[]}</li>
 *   <li><b>Object Arrays:</b> {@code Object[]} and all typed arrays with null-safe comparisons</li>
 *   <li><b>Collections:</b> {@code Collection<?>}, {@code List<?>} with optimized {@code RandomAccess} handling</li>
 *   <li><b>Iterators:</b> {@code Iterator<?>} for streaming and lazy evaluation scenarios</li>
 *   <li><b>Strings:</b> {@code String} and {@code CharSequence} with character and substring operations</li>
 * </ul>
 *
 * <p><b>Return Type Philosophy:</b>
 * <ul>
 *   <li><b>{@code OptionalInt}:</b> For single index operations, provides null-safe "not found" handling</li>
 *   <li><b>{@code BitSet}:</b> For multiple index operations, efficient storage of all matching positions</li>
 *   <li><b>Performance Benefits:</b> Avoids autoboxing and provides efficient bit manipulation operations</li>
 *   <li><b>Fluent API:</b> Enables method chaining and expressive conditional logic</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic element searching
 * int[] numbers = {1, 2, 3, 4, 5, 3, 2, 1};
 * OptionalInt firstIndex = Index.of(numbers, 3);   // Returns OptionalInt[2]
 * OptionalInt lastIndex = Index.last(numbers, 3);   // Returns OptionalInt[5]
 *
 * // Handle "not found" cases gracefully
 * Index.of(numbers, 10).ifPresent(i -> System.out.println("Found at: " + i));
 * int index = Index.of(numbers, 10).orElse(-1);   // Traditional fallback
 * boolean exists = Index.of(numbers, 3).isPresent();   // Existence check
 *
 * // Range-based searching
 * OptionalInt fromIndex = Index.of(numbers, 2, 3);   // Start search from index 3
 * OptionalInt backIndex = Index.last(numbers, 1, 6);   // Search backward from index 6
 *
 * // String operations
 * OptionalInt charIndex = Index.of("Hello World", 'o');   // Returns OptionalInt[4]
 * OptionalInt subIndex = Index.of("Hello World", "World");   // Returns OptionalInt[6]
 * OptionalInt ignoreCase = Index.ofIgnoreCase("Hello World", "WORLD");   // Returns OptionalInt[6]
 *
 * // Collection searching
 * List<String> words = Arrays.asList("apple", "banana", "cherry", "apple");
 * OptionalInt wordIndex = Index.of(words, "banana");   // Returns OptionalInt[1]
 * OptionalInt lastApple = Index.last(words, "apple");   // Returns OptionalInt[3]
 *
 * // Find all occurrences
 * BitSet allOccurrences = Index.allOf(numbers, 1);   // Returns BitSet with bits 0 and 7 set
 * List<Integer> indices = allOccurrences.stream().boxed().collect(Collectors.toList());
 * }</pre>
 *
 * <p><b>Advanced Subarray/Sublist Operations:</b>
 * <pre>{@code
 * // Subarray searching
 * int[] source = {1, 2, 3, 4, 5, 6, 7};
 * int[] pattern = {3, 4, 5};
 * OptionalInt subArrayIndex = Index.ofSubArray(source, pattern);   // Returns OptionalInt[2]
 *
 * // Partial subarray matching
 * OptionalInt partialMatch = Index.ofSubArray(source, 1, pattern, 0, 2);   // Match {3, 4} starting from index 1
 *
 * // Sublist operations with custom objects
 * List<String> document = Arrays.asList("The", "quick", "brown", "fox", "jumps");
 * List<String> phrase = Arrays.asList("quick", "brown");
 * OptionalInt phraseIndex = Index.ofSubList(document, phrase);   // Returns OptionalInt[1]
 *
 * // Complex pattern searching
 * boolean[] flags = {true, false, true, true, false, true, true, false};
 * boolean[] pattern = {true, true, false};
 * OptionalInt lastPattern = Index.lastOfSubArray(flags, pattern);   // Returns OptionalInt[5]
 * }</pre>
 *
 * <p><b>Predicate-Based Searching:</b>
 * <pre>{@code
 * // Custom predicate matching
 * String[] words = {"apple", "apricot", "banana", "avocado"};
 * Predicate<String> startsWithA = s -> s.startsWith("a");
 * BitSet aWords = Index.allOf(words, startsWithA);   // Returns BitSet with bits 0, 1, 3 set
 *
 * // Complex predicate combinations
 * Integer[] numbers = {1, 4, 9, 16, 25, 36, 49};
 * Predicate<Integer> isPerfectSquare = n -> {
 *     int sqrt = (int) Math.sqrt(n);
 *     return sqrt * sqrt == n;
 * };
 * BitSet perfectSquares = Index.allOf(numbers, isPerfectSquare);   // All indices
 *
 * // Range-based predicate searching
 * BitSet fromIndex = Index.allOf(words, startsWithA, 1);   // Start searching from index 1
 * }</pre>
 *
 * <p><b>Floating-Point Operations with Tolerance:</b>
 * <pre>{@code
 * // Tolerance-based searching for floating-point values
 * double[] measurements = {1.0, 2.001, 3.0, 2.002, 4.0};
 * double target = 2.0;
 * double tolerance = 0.01;
 *
 * OptionalInt closeMatch = Index.of(measurements, target, tolerance);   // Returns OptionalInt[1]
 * OptionalInt lastMatch = Index.last(measurements, target, tolerance);   // Returns OptionalInt[3]
 * BitSet allMatches = Index.allOf(measurements, target, tolerance);   // BitSet with bits 1, 3 set
 *
 * // High precision requirements
 * double strictTolerance = 0.001;
 * BitSet strictMatches = Index.allOf(measurements, target, strictTolerance);   // May return empty BitSet
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Linear Search:</b> O(n) for most single-element operations</li>
 *   <li><b>Subarray Search:</b> O(n*m) worst case, optimized for common patterns</li>
 *   <li><b>All Occurrences:</b> O(n) with efficient BitSet population</li>
 *   <li><b>RandomAccess Lists:</b> O(1) element access, O(n) overall search</li>
 *   <li><b>Sequential Lists:</b> O(n) with iterator-based traversal</li>
 *   <li><b>Memory Usage:</b> O(1) for single searches, O(n) for BitSet results</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Operations:</b> All methods are stateless and thread-safe</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 *   <li><b>Input Immutability:</b> Methods do not modify input arrays or collections</li>
 *   <li><b>No Shared State:</b> No static mutable fields or shared resources</li>
 * </ul>
 *
 * <p><b>Error Handling and Edge Cases:</b>
 * <ul>
 *   <li><b>Null Inputs:</b> Graceful handling of null arrays and collections</li>
 *   <li><b>Empty Inputs:</b> Returns {@code OptionalInt.empty()} for empty arrays/collections</li>
 *   <li><b>Invalid Ranges:</b> Validates index parameters and throws {@code IndexOutOfBoundsException}</li>
 *   <li><b>Null Elements:</b> Proper null-safe comparison using {@code Objects.equals()}</li>
 * </ul>
 *
 * <p><b>Optimization Strategies:</b>
 * <ul>
 *   <li><b>Primitive Specialization:</b> Avoids boxing overhead with dedicated primitive methods</li>
 *   <li><b>RandomAccess Detection:</b> Optimized algorithms for {@code RandomAccess} collections</li>
 *   <li><b>Early Termination:</b> Breaks loops as soon as result is determined</li>
 *   <li><b>Memory Efficiency:</b> Reuses {@code OptionalInt.empty()} instance for all "not found" cases</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative APIs:</b>
 * <ul>
 *   <li><b>vs. Arrays.binarySearch():</b> Works with unsorted data and provides richer API</li>
 *   <li><b>vs. Collections.indexOfSubList():</b> Enhanced with range support and null safety</li>
 *   <li><b>vs. String.indexOf():</b> Consistent API across all data types with additional features</li>
 *   <li><b>vs. Stream.findFirst():</b> More efficient for simple searches without stream overhead</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code OptionalInt.ifPresent()} for conditional logic instead of checking for {@code -1}</li>
 *   <li>Leverage {@code OptionalInt.orElse()} for providing default values</li>
 *   <li>Use appropriate tolerance values for floating-point comparisons</li>
 *   <li>Consider {@code BitSet} operations for efficient multiple-index manipulation</li>
 *   <li>Validate input parameters before calling methods to avoid exceptions</li>
 *   <li>Use predicate-based methods for complex matching logic</li>
 * </ul>
 *
 * <p><b>Integration with Other Utilities:</b>
 * <ul>
 *   <li><b>Relationship to {@link N}:</b> Complements N.indexOf methods with modern API design</li>
 *   <li><b>Stream Integration:</b> BitSet results can be converted to IntStream for further processing</li>
 *   <li><b>Optional Pattern:</b> Follows Java 8+ Optional pattern for null-safe operations</li>
 *   <li><b>Collection Utilities:</b> Works seamlessly with other collection utility classes</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Converting {@code OptionalInt} to {@code int} unnecessarily with {@code .orElse(-1)}</li>
 *   <li>Using {@code .get()} without checking {@code .isPresent()} first</li>
 *   <li>Ignoring the null-safety benefits by falling back to traditional {@code -1} patterns</li>
 *   <li>Using inappropriate tolerance values for floating-point comparisons</li>
 * </ul>
 *
 * <p><b>Example: Complex Search Pipeline</b>
 * <pre>{@code
 * // Advanced search combining multiple techniques
 * public class DocumentSearcher {
 *     public List<SearchResult> findComplexPatterns(String[] document, String[] patterns) {
 *         List<SearchResult> results = new ArrayList<>();
 *
 *         for (String pattern : patterns) {
 *             // Find all occurrences of each pattern
 *             BitSet occurrences = Index.allOf(document, pattern);
 *
 *             // Convert BitSet to list of indices
 *             List<Integer> indices = occurrences.stream()
 *                 .boxed()
 *                 .collect(Collectors.toList());
 *
 *             if (!indices.isEmpty()) {
 *                 // Find first and last occurrence
 *                 OptionalInt first = Index.of(document, pattern);
 *                 OptionalInt last = Index.last(document, pattern);
 *
 *                 SearchResult result = new SearchResult(
 *                     pattern,
 *                     indices,
 *                     first.orElse(-1),
 *                     last.orElse(-1),
 *                     indices.size()
 *                 );
 *                 results.add(result);
 *             }
 *         }
 *
 *         return results;
 *     }
 * }
 * }</pre>
 *
 * @see OptionalInt
 * @see BitSet
 * @see N#indexOf
 * @see String#indexOf(String)
 * @see Collections#indexOfSubList
 * @see Arrays#binarySearch
 * @see java.util.stream.Stream
 * @see RandomAccess
 * @see Predicate
 */
public final class Index {

    private static final OptionalInt NOT_FOUND = OptionalInt.empty();

    private Index() {
        // singleton.
    }

    /**
     * Returns the index of the first occurrence of the specified boolean value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given boolean array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {false, true, false, true, false};
     * Index.of(arr, true).get();          // returns 1
     * Index.of(arr, false).get();         // returns 0
     * Index.of(null, true).isPresent();   // returns false
     * }</pre>
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(boolean[], boolean, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified boolean value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given boolean array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {false, true, false, true, false};
     * Index.of(arr, true, 0).get();         // returns 1
     * Index.of(arr, true, 2).get();         // returns 3
     * Index.of(arr, true, 4).isPresent();   // returns false
     * }</pre>
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified char value in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given char array.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.of(arr, 'l').get();         // returns 2
     * Index.of(arr, 'h').get();         // returns 0
     * Index.of(arr, 'x').isPresent();   // returns false
     * }</pre>
     *
     * @param a the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(char[], char, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final char[] a, final char valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified char value in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given char array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.of(arr, 'l', 0).get();         // returns 2
     * Index.of(arr, 'l', 3).get();         // returns 3
     * Index.of(arr, 'l', 4).isPresent();   // returns false
     * }</pre>
     *
     * @param a the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(char[], char)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final char[] a, final char valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code byte} values.
     *
     * @param a the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(byte[], byte, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code byte} values.
     *
     * @param a the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(byte[], byte)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final byte[] a, final byte valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code short} values.
     *
     * @param a the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(short[], short, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final short[] a, final short valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code short} values.
     *
     * @param a the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(short[], short)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final short[] a, final short valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code int} values.
     *
     * @param a the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(int[], int, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final int[] a, final int valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code int} values.
     *
     * @param a the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(int[], int)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final int[] a, final int valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code long} values.
     *
     * @param a the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(long[], long, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final long[] a, final long valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code long} values.
     *
     * @param a the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(long[], long)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final long[] a, final long valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(float[], float, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final float[] a, final float valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}.
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(float[], float)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final float[] a, final float valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(double[], double, int)
     * @see #of(double[], double, double)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final double[] a, final double valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(double[], double)
     * @see #of(double[], double, double, int)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, within a given tolerance.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given double array.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance,
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @see #of(double[], double, double, int)
     * @see #of(double[], double)
     * @see N#indexOf(double[], double, double)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance) {
        return of(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, within a given tolerance and starting from the specified index.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given double array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance at or after {@code fromIndex},
     *         or an empty OptionalInt if no value is found within tolerance, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(double[], double, double)
     * @see #of(double[], double, int)
     * @see N#indexOf(double[], double, double, int)
     */
    public static OptionalInt of(final double[] a, final double valueToFind, final double tolerance, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, tolerance, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the array.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given object array
     * using {@code equals()} for comparison. {@code null} values are handled correctly - a {@code null}
     * valueToFind will match the first {@code null} element in the array.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b"};
     * Index.of(arr, "b").get();         // returns 1
     * Index.of(arr, "d").isPresent();   // returns false
     * }</pre>
     *
     * @param a the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.indexOf(a, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given object array,
     * beginning at the specified {@code fromIndex}. Uses {@code equals()} for comparison.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * @param a the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final Object[] a, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(a, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the collection.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given collection
     * using {@code equals()} for comparison. The index represents the position in iteration order.
     * {@code null} values are handled correctly.
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #of(Collection, Object, int)
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.indexOf(c, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the collection, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given collection,
     * beginning at the specified {@code fromIndex}. Uses {@code equals()} for comparison.
     * Negative {@code fromIndex} values are treated as 0. The index represents the position in iteration order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b", "a");
     * Index.of(list, "b", 2).get();   // returns 3
     * }</pre>
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the collection is {@code null}, or {@code fromIndex >= collection.size()}
     * @see #of(Collection, Object)
     */
    public static OptionalInt of(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(c, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} by iterating through the iterator
     * and using {@code equals()} for comparison. The iterator will be consumed up to and including the matching element.
     * Note that the iterator cannot be reset, so this operation is destructive.
     *
     * @param iter the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @see #of(Iterator, Object, int)
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind) {
        return toOptionalInt(N.indexOf(iter, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator, starting from the specified index.
     * <p>
     * This method skips the first {@code fromIndex} elements, then searches for {@code valueToFind} using {@code equals()}
     * for comparison. The iterator will be consumed up to and including the matching element (or exhausted if not found).
     * Note that the iterator cannot be reset, so this operation is destructive. Negative {@code fromIndex} values are treated as 0.
     *
     * @param iter the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @see #of(Iterator, Object)
     */
    public static OptionalInt of(final Iterator<?> iter, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(iter, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified character in the string.
     * <p>
     * This method searches for the first occurrence of the character (represented as an int Unicode code point)
     * in the given string. If the string is {@code null} or empty, an empty OptionalInt is returned.
     *
     * @param str the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character,
     *         or an empty OptionalInt if the character is not found or the string is {@code null}
     * @see #of(String, int, int)
     * @see Strings#indexOf(String, int)
     * @see String#indexOf(int)
     */
    public static OptionalInt of(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified character in the string, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of the character (represented as an int Unicode code point)
     * in the given string, beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello world", 'o', 5).get();   // returns 7
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character at or after {@code fromIndex},
     *         or an empty OptionalInt if the character is not found, the string is {@code null}, or {@code fromIndex >= str.length()}
     * @see #of(String, int)
     * @see Strings#indexOf(String, int, int)
     * @see String#indexOf(int, int)
     */
    public static OptionalInt of(final String str, final int charValueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, charValueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code str}.
     * If the string is {@code null} or empty, an empty OptionalInt is returned.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello world", "world").get();       // returns 6
     * Index.of("hello world", "bye").isPresent();   // returns false
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring,
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #of(String, String, int)
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOf(String, String)
     * @see String#indexOf(String)
     */
    public static OptionalInt of(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(str, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code str},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello world hello", "hello", 1).get();   // returns 12
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring at or after {@code fromIndex},
     *         or an empty OptionalInt if the substring is not found, either parameter is {@code null}, or {@code fromIndex >= str.length()}
     * @see #of(String, String)
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOf(String, String, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt of(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code str}.
     * Both ASCII and Unicode characters are compared case-insensitively.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case),
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOfIgnoreCase(String, String)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case and starting from the specified index.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code str},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case) at or after {@code fromIndex},
     *         or an empty OptionalInt if the substring is not found, either parameter is {@code null}, or {@code fromIndex >= str.length()}
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOfIgnoreCase(String, String, int)
     */
    public static OptionalInt ofIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOfIgnoreCase(str, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray}.
     * It's similar to {@link String#indexOf(String)} but for boolean arrays.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false};
     * boolean[] sub = {true, true};
     * Index.ofSubArray(source, sub).get();   // returns 2
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false, true, true};
     * boolean[] sub = {true, true};
     * Index.ofSubArray(source, 0, sub).get();         // returns 2
     * Index.ofSubArray(source, 3, sub).get();         // returns 5
     * Index.ofSubArray(source, 6, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found, either array is {@code null}, or {@code fromIndex >= sourceArray.length}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceArray.length}, returns empty OptionalInt</li>
     * </ul>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] sourceArray, final int fromIndex, final boolean[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray}.
     * It's similar to {@link String#indexOf(String)} but for char arrays.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'l', 'l', 'o'};
     * Index.ofSubArray(source, sub).get();   // returns 2
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #ofSubArray(char[], int, char[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'o', 'r'};
     * Index.ofSubArray(source, 0, sub).get();         // returns 8
     * Index.ofSubArray(source, 5, sub).get();         // returns 8
     * Index.ofSubArray(source, 9, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found, either array is {@code null}, or {@code fromIndex >= sourceArray.length}
     * @see #ofSubArray(char[], char[])
     * @see #ofSubArray(char[], int, char[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This allows for flexible partial subarray matching.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceArray.length}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'w', 'o', 'r', 'l', 'd'};
     *
     * // Match entire subarray starting from index 6
     * Index.ofSubArray(source, 0, sub, 0, 5).get();   // returns 6
     *
     * // Match only "wor" (first 3 elements) from sub
     * Index.ofSubArray(source, 0, sub, 0, 3).get();   // returns 6
     *
     * // Match only "orl" (elements at indices 1-3 of sub)
     * Index.ofSubArray(source, 0, sub, 1, 3).get();   // returns 7
     *
     * // Start search from index 8
     * Index.ofSubArray(source, 8, sub, 1, 3).get();   // returns 8
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(char[], char[])
     * @see #ofSubArray(char[], int, char[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] sourceArray, final int fromIndex, final char[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray},
     * beginning at the specified {@code fromIndex}. This is particularly useful for binary data pattern matching.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {1, 2, 3, 4, 5, 6, 7, 8};
     * byte[] pattern = {4, 5, 6};
     * Index.ofSubArray(source, 0, pattern).get();         // returns 3
     * Index.ofSubArray(source, 4, pattern).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found, either array is {@code null}, or {@code fromIndex >= sourceArray.length}
     * @see #ofSubArray(byte[], byte[])
     * @see #ofSubArray(byte[], int, byte[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This is useful for binary data pattern matching where you need
     * to match only a specific portion of a pattern.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[], int, int)} for {@code byte} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {1, 2, 3, 4, 5, 6, 7, 8};
     * byte[] pattern = {3, 4, 5, 6, 7};
     *
     * // Match entire pattern
     * Index.ofSubArray(source, 0, pattern, 0, 5).get();   // returns 2
     *
     * // Match only first 3 bytes of pattern {3, 4, 5}
     * Index.ofSubArray(source, 0, pattern, 0, 3).get();   // returns 2
     *
     * // Match bytes at indices 2-4 of pattern {5, 6, 7}
     * Index.ofSubArray(source, 0, pattern, 2, 3).get();   // returns 4
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(byte[], byte[])
     * @see #ofSubArray(byte[], int, byte[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] sourceArray, final int fromIndex, final byte[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code short} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(short[], int, short[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code short} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {10, 20, 30, 40, 50, 60};
     * short[] pattern = {30, 40, 50};
     * Index.ofSubArray(source, 0, pattern).get();         // returns 2
     * Index.ofSubArray(source, 3, pattern).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found, either array is {@code null}, or {@code fromIndex >= sourceArray.length}
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(short[], short[])
     * @see #ofSubArray(short[], int, short[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This allows for flexible partial subarray matching.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[], int, int)} for {@code short} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {10, 20, 30, 40, 50, 60, 70};
     * short[] pattern = {30, 40, 50, 60};
     *
     * // Match entire pattern
     * Index.ofSubArray(source, 0, pattern, 0, 4).get();   // returns 2
     *
     * // Match only first 2 elements {30, 40}
     * Index.ofSubArray(source, 0, pattern, 0, 2).get();   // returns 2
     *
     * // Match elements at indices 2-3 of pattern {50, 60}
     * Index.ofSubArray(source, 0, pattern, 2, 2).get();   // returns 4
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(short[], short[])
     * @see #ofSubArray(short[], int, short[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] sourceArray, final int fromIndex, final short[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code int} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(int[], int, int[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code int} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(int[], int[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] sourceArray, final int fromIndex, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code long} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(long[], int, long[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code long} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(long[], long[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] sourceArray, final int fromIndex, final long[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (sourceArray[k] != subArrayToFind[j]) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code float} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(float[], int, float[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code float} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(float[], float[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] sourceArray, final int fromIndex, final float[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code double} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(double[], int, double[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code double} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(double[], double[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * The search starts at the specified <i>fromIndex</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] sourceArray, final int fromIndex, final double[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray}.
     * Elements are compared using {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     * This is the generic Object array version that works with any object type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "d", "e", "f"};
     * String[] pattern = {"c", "d", "e"};
     * Index.ofSubArray(source, pattern).get();   // returns 2
     *
     * Integer[] numbers = {1, 2, 3, 4, 5, 6};
     * Integer[] sub = {3, 4};
     * Index.ofSubArray(numbers, sub).get();   // returns 2
     *
     * // Handles null elements
     * String[] withNulls = {"a", null, "c", null, "e"};
     * String[] nullPattern = {null, "c"};
     * Index.ofSubArray(withNulls, nullPattern).get();   // returns 1
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #ofSubArray(Object[], int, Object[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code sourceArray},
     * beginning at the specified {@code fromIndex}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "d", "c", "d", "e"};
     * String[] pattern = {"c", "d"};
     * Index.ofSubArray(source, 0, pattern).get();         // returns 2
     * Index.ofSubArray(source, 3, pattern).get();         // returns 4
     * Index.ofSubArray(source, 5, pattern).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found, either array is {@code null}, or {@code fromIndex >= sourceArray.length}
     * @see #ofSubArray(Object[], Object[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind) {
        return ofSubArray(sourceArray, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. This allows for flexible partial subarray matching.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceArray.length}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "d", "e", "f", "g"};
     * String[] pattern = {"c", "d", "e", "f"};
     *
     * // Match entire pattern
     * Index.ofSubArray(source, 0, pattern, 0, 4).get();   // returns 2
     *
     * // Match only first 2 elements {"c", "d"}
     * Index.ofSubArray(source, 0, pattern, 0, 2).get();   // returns 2
     *
     * // Match elements at indices 2-3 of pattern {"e", "f"}
     * Index.ofSubArray(source, 0, pattern, 2, 2).get();   // returns 4
     *
     * // Start search from index 3
     * Index.ofSubArray(source, 3, pattern, 0, 2).get();   // returns 4 (matches {"d", "e"} at positions 3-4)
     * }</pre>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(Object[], Object[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] sourceArray, final int fromIndex, final Object[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j])) {
                    break;
                } else if (j == endIndexOfTargetSubArray - 1) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the first occurrence of the specified sublist in the given source list.
     * <p>
     * This method searches for the complete {@code subListToFind} as a contiguous sequence within {@code sourceList}.
     * Elements are compared using {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     * The implementation is optimized for {@link RandomAccess} lists to provide O(1) element access.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "d", "e", "f");
     * List<String> pattern = Arrays.asList("c", "d", "e");
     * Index.ofSubList(source, pattern).get();   // returns 2
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
     * List<Integer> sub = Arrays.asList(3, 4);
     * Index.ofSubList(numbers, sub).get();   // returns 2
     *
     * // Handles null elements
     * List<String> withNulls = Arrays.asList("a", null, "c", null, "e");
     * List<String> nullPattern = Arrays.asList(null, "c");
     * Index.ofSubList(withNulls, nullPattern).get();   // returns 1
     * }</pre>
     *
     * @param sourceList the list to be searched, may be {@code null}
     * @param subListToFind the sublist to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the sublist starts,
     *         or an empty OptionalInt if the sublist is not found or either list is {@code null}
     * @see #ofSubList(List, int, List)
     * @see #ofSubList(List, int, List, int, int)
     * @see #ofSubArray(Object[], Object[])
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final List<?> subListToFind) {
        return ofSubList(sourceList, 0, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the specified sublist in the given source list, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subListToFind} as a contiguous sequence within {@code sourceList},
     * beginning at the specified {@code fromIndex}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. Negative {@code fromIndex} values are treated as 0.
     * The implementation is optimized for {@link RandomAccess} lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d", "e");
     * List<String> pattern = Arrays.asList("c", "d");
     * Index.ofSubList(source, 0, pattern).get();         // returns 2
     * Index.ofSubList(source, 3, pattern).get();         // returns 4
     * Index.ofSubList(source, 5, pattern).isPresent();   // returns false
     * }</pre>
     *
     * @param sourceList the list to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subListToFind the sublist to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the sublist starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the sublist is not found, either list is {@code null}, or {@code fromIndex >= sourceList.size()}
     * @see #ofSubList(List, List)
     * @see #ofSubList(List, int, List, int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind) {
        return ofSubList(sourceList, fromIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified sublist in the given source list.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subListToFind} within {@code sourceList},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subListToFind}
     * starting at {@code startIndexOfSubList}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. This allows for flexible partial sublist matching.
     * <p>
     * The implementation is optimized for {@link RandomAccess} lists. For non-RandomAccess lists,
     * it converts sublists to arrays for comparison.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both lists are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either list is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= sourceList.size()}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
     * List<String> pattern = Arrays.asList("c", "d", "e", "f");
     *
     * // Match entire pattern
     * Index.ofSubList(source, 0, pattern, 0, 4).get();   // returns 2
     *
     * // Match only first 2 elements {"c", "d"}
     * Index.ofSubList(source, 0, pattern, 0, 2).get();   // returns 2
     *
     * // Match elements at indices 2-3 of pattern {"e", "f"}
     * Index.ofSubList(source, 0, pattern, 2, 2).get();   // returns 4
     *
     * // Start search from index 3
     * Index.ofSubList(source, 3, pattern, 0, 2).get();   // returns 4 (matches {"d", "e"} at positions 3-4)
     * }</pre>
     *
     * @param sourceList the list to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subListToFind the sublist to search for, may be {@code null}
     * @param startIndexOfSubList the starting index within {@code subListToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subListToFind}
     * @return an OptionalInt containing the zero-based index where the sublist portion is found,
     *         or an empty OptionalInt if the sublist is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubList} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subListToFind}
     * @see #ofSubList(List, List)
     * @see #ofSubList(List, int, List)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> sourceList, final int fromIndex, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(sourceList);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (sourceList == null || subListToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (sourceList == null || subListToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (!N.equals(sourceList.get(k), subListToFind.get(j))) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return ofSubArray(sourceList.subList(fromIndex, sourceList.size()).toArray(), 0,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
        }
    }

    /**
     * Returns the last index of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final boolean[] a, final boolean valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code char} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(char[], char, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final char[] a, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code char} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @param startIndexFromBack the index to start the search from the end of the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(char[], char)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final char[] a, final char valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code byte} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(byte[], byte, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code byte} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @param startIndexFromBack the index to start the search from the end of the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(byte[], byte)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final byte[] a, final byte valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code short} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(short[], short, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final short[] a, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code short} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @param startIndexFromBack the index to start the search from the end of the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(short[], short)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final short[] a, final short valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code int} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(int[], int, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final int[] a, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code int} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @param startIndexFromBack the index to start the search from the end of the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(int[], int)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final int[] a, final int valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code long} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(long[], long, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final long[] a, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code long} values.
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array
     * @param startIndexFromBack the index to start the search from the end of the array
     * @return an OptionalInt containing the last index of the value in the array, or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(long[], long)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final long[] a, final long valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified float value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(float[], float, int)
     * @see #last(Object[], Object)
     * @see Float#compare(float, float)
     */
    public static OptionalInt last(final float[] a, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified float value in the given array, searching backwards from a specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive)
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(float[], float)
     * @see #last(Object[], Object, int)
     * @see Float#compare(float, float)
     */
    public static OptionalInt last(final float[] a, final float valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified double value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(double[], double, int)
     * @see #last(Object[], Object)
     * @see Double#compare(double, double)
     */
    public static OptionalInt last(final double[] a, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the last index of the specified double value in the given array, searching backwards from a specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive)
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(double[], double)
     * @see #last(Object[], Object, int)
     * @see Double#compare(double, double)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified double value in the given array, within a specified tolerance.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of a value
     * that falls within the range {@code [valueToFind - tolerance, valueToFind + tolerance]}.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return an OptionalInt containing the zero-based index of the last occurrence of a value within tolerance,
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @see #last(double[], double, double, int)
     * @see #last(double[], double)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance));
    }

    /**
     * Returns the last index of the specified value in the given array within a specified tolerance, starting from the specified index from the end.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param tolerance the tolerance within which to find the value.
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @return an OptionalInt containing the last index of the value in the array within the specified tolerance, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     * @see N#lastIndexOf(double[], double, double, int)
     */
    public static OptionalInt last(final double[] a, final double valueToFind, final double tolerance, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, tolerance, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified value in the given array.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of {@code valueToFind}.
     * Elements are compared using {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b", "a"};
     * Index.last(arr, "b").get();         // returns 3
     * Index.last(arr, "a").get();         // returns 4
     * Index.last(arr, "d").isPresent();   // returns false
     *
     * // Handles null elements
     * String[] withNull = {"a", null, "b", null};
     * Index.last(withNull, null).get();   // returns 3
     * }</pre>
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #last(Object[], Object, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified value in the given array, searching backwards from the specified index.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} (inclusive) towards the beginning of the array
     * for the last occurrence of {@code valueToFind}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. If {@code startIndexFromBack} is greater than or equal to the array length,
     * the entire array is searched.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b", "a"};
     * Index.last(arr, "b", 4).get();         // returns 3 (searches from index 4 backward)
     * Index.last(arr, "b", 2).get();         // returns 1 (searches from index 2 backward)
     * Index.last(arr, "b", 0).isPresent();   // returns false (only checks index 0)
     * }</pre>
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @param startIndexFromBack the index to start the search from (inclusive), searching backwards
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #last(Object[], Object)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt last(final Object[] a, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(a, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified value in the given collection.
     * <p>
     * This method searches backwards from the end of the collection for the last occurrence of {@code valueToFind}.
     * The index represents the position in iteration order. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b", "a");
     * Index.last(list, "b").get();         // returns 3
     * Index.last(list, "a").get();         // returns 4
     * Index.last(list, "d").isPresent();   // returns false
     * }</pre>
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #last(Collection, Object, int)
     * @see #of(Collection, Object)
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind));
    }

    /**
     * Returns the last index of the specified value in the given collection, starting from the specified index from the end.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @param startIndexFromBack the index to start the search from the end of the collection.
     * @return an OptionalInt containing the last index of the value in the collection, or an empty {@code OptionalInt} if the value is not found.
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final Collection<?> c, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(c, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified character in the given string.
     *
     * @param str the string to be searched.
     * @param charValueToFind the character value to find in the string.
     * @return an OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind));
    }

    /**
     * Returns the last index of the specified character in the given string, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param charValueToFind the character value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the character in the string, or an empty {@code OptionalInt} if the character is not found.
     * @see Strings#lastIndexOf(String, int, int)
     */
    public static OptionalInt last(final String str, final int charValueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, charValueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified substring in the given string.
     * <p>
     * This method searches backwards from the end of the string for the last occurrence of {@code valueToFind}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.last("hello world hello", "hello").get();   // returns 12
     * Index.last("hello world", "world").get();         // returns 6
     * Index.last("hello world", "bye").isPresent();     // returns false
     * }</pre>
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring,
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #last(String, String, int)
     * @see #lastOfIgnoreCase(String, String)
     * @see #of(String, String)
     * @see Strings#lastIndexOf(String, String)
     * @see String#lastIndexOf(String)
     */
    public static OptionalInt last(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind));
    }

    /**
     * Returns the last index of the specified string in the given string, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param valueToFind the string value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOf(String, String, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt last(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(str, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive backwards search for {@code valueToFind} within {@code str},
     * starting from the end of the string. Both ASCII and Unicode characters are compared case-insensitively.
     *
     * @param str the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring (ignoring case),
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #lastOfIgnoreCase(String, String, int)
     * @see Strings#lastIndexOfIgnoreCase(String, String)
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind));
    }

    /**
     * Returns the last index of the specified string in the given string, ignoring case considerations, starting from the specified index from the end.
     *
     * @param str the string to be searched.
     * @param valueToFind the string value to find in the string.
     * @param startIndexFromBack the index to start the search from the end of the string.
     * @return an OptionalInt containing the last index of the string in the string, or an empty {@code OptionalInt} if the string is not found.
     * @see Strings#lastIndexOfIgnoreCase(String, String, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfIgnoreCase(final String str, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(str, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, searching backwards from a specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code sourceArray},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, sourceArray.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code sourceArray.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the position to start the backwards search from; the search includes this position
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or inputs are invalid
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] sourceArray, final int startIndexFromBack, final boolean[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        // "aaa".lastIndexOf("") = 3
        // "aaa".lastIndexOf("", 0) = 0
        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final char[] sourceArray, final int startIndexFromBack, final char[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] sourceArray, final int startIndexFromBack, final byte[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code short} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(short[], int, short[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code short} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(short[], short[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] sourceArray, final int startIndexFromBack, final short[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code int} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(int[], int, int[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code int} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(int[], int[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] sourceArray, final int startIndexFromBack, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code long} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(long[], int, long[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code long} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(long[], long[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] sourceArray, final int startIndexFromBack, final long[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (sourceArray[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code float} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(float[], int, float[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code float} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(float[], float[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] sourceArray, final int startIndexFromBack, final float[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code double} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(double[], int, double[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code double} arrays.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty OptionalInt if the subarray is not found or either array is {@code null} or empty
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(double[], double[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] sourceArray, final int startIndexFromBack, final double[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified subarray in the given source array.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, N.len(sourceArray), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind) {
        return lastOfSubArray(sourceArray, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the last index of the specified subarray in the given source array, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the subarray starting from <i>startIndexOfSubArray</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceArray the array to be searched, may be {@code null}
     * @param startIndexFromBack the index to start the search from the end of the array.
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index of the subarray to be found.
     * @param sizeToMatch the number of elements to match from the subarray.
     * @return an OptionalInt containing the last index of the subarray in the source array, or an empty {@code OptionalInt} if the subarray is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubArray</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] sourceArray, final int startIndexFromBack, final Object[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(sourceArray);

        if (sizeToMatch == 0) {
            if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceArray == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(sourceArray[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the last index of the specified sub-list in the given source list.
     *
     * @param sourceList the list to be searched.
     * @param subListToFind the sub-list to find in the source list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        return lastOfSubList(sourceList, N.size(sourceList), subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the last index of the specified sub-list in the given source list, starting from the specified index from the end.
     *
     * @param sourceList the list to be searched.
     * @param startIndexFromBack the index to start the search from the end of the list.
     * @param subListToFind the sub-list to find in the source list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind) {
        return lastOfSubList(sourceList, startIndexFromBack, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the last index of the specified sub-list in the given source list, starting from the specified index from the end.
     * The search starts at the specified <i>startIndexFromBack</i> and checks for the sub-list starting from <i>startIndexOfSubList</i> up to <i>sizeToMatch</i> elements.
     *
     * @param sourceList the list to be searched.
     * @param startIndexFromBack the index to start the search from the end of the list.
     * @param subListToFind the sub-list to find in the source list.
     * @param startIndexOfSubList the starting index of the sub-list to be found.
     * @param sizeToMatch the number of elements to match from the sub-list.
     * @return an OptionalInt containing the last index of the sub-list in the source list, or an empty {@code OptionalInt} if the sub-list is not found.
     * @throws IndexOutOfBoundsException if <i>startIndexOfSubList</i> and <i>sizeToMatch</i> do not denote a valid range in <i>subArrayToFind</i>.
     * @see Strings#lastIndexOf(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(sourceList);

        if (sizeToMatch == 0) {
            if (sourceList == null || subListToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (sourceList == null || subListToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (sourceList instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++) {
                    if (!N.equals(sourceList.get(k), subListToFind.get(j++))) {
                        break;
                    } else if (j == endIndexOfTargetSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(sourceList.subList(0, N.min(startIndexFromBack, len - sizeToMatch) + sizeToMatch).toArray(), startIndexFromBack,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
        }
    }

    /**
     * Returns the indices of all occurrences of the specified boolean value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit in the BitSet corresponds to an index where the value was found.
     *
     * @param a the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value in the array;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(boolean[], boolean, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final boolean[] a, final boolean valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final byte[] a, final byte valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final char[] a, final char valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final char[] a, final char valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final short[] a, final short valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final short[] a, final short valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final int[] a, final int valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final int[] a, final int valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final long[] a, final long valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final long[] a, final long valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final float[] a, final float valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final float[] a, final float valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Float.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final double[] a, final double valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Double.compare(a[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified double value in the given array, within a specified tolerance.
     * <p>
     * This method finds all positions where a value falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} and returns them as a BitSet.
     * This is useful for comparing floating-point values where exact equality may not be reliable.
     *
     * @param a the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param tolerance the tolerance for matching; must be non-negative. A value matches if it's within
     *                  {@code valueToFind ± tolerance}
     * @return a BitSet containing the zero-based indices of all occurrences of values within tolerance;
     *         returns an empty BitSet if no values are found within tolerance or the array is {@code null} or empty
     * @see #allOf(double[], double, double, int)
     * @see #allOf(double[], double)
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance) {
        return allOf(a, valueToFind, tolerance, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array within a specified tolerance, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param tolerance the tolerance within which matches will be found.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array within the specified tolerance, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final double[] a, final double valueToFind, final double tolerance, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        final double min = valueToFind - tolerance;
        final double max = valueToFind + tolerance;

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (a[i] >= min && a[i] <= max) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array.
     * <p>
     * This method searches through the entire array and returns a BitSet containing the indices of all positions
     * where {@code valueToFind} occurs. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "a", "c", "a", "b"};
     * BitSet indices = Index.allOf(arr, "a");
     * // indices contains {0, 2, 4}
     *
     * // Convert BitSet to List
     * List<Integer> list = indices.stream().boxed().collect(Collectors.toList());
     * // list = [0, 2, 4]
     *
     * // Count occurrences
     * int count = indices.cardinality();   // returns 3
     *
     * // Check if value exists
     * boolean hasA = !indices.isEmpty();   // returns true
     *
     * // Handles null elements
     * String[] withNulls = {"a", null, "b", null, "a"};
     * BitSet nullIndices = Index.allOf(withNulls, null);
     * // nullIndices contains {1, 3}
     * }</pre>
     *
     * @param a the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found, the array is {@code null}, or empty
     * @see #allOf(Object[], Object, int)
     * @see #of(Object[], Object)
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind) {
        return allOf(a, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     *
     * @param a the array to be searched.
     * @param valueToFind the value to find in the array.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the array, or an empty BitSet if the value is not found or the input array is {@code null}.
     */
    public static BitSet allOf(final Object[] a, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (N.equals(a[i], valueToFind)) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given collection.
     * <p>
     * This method searches through the entire collection (in iteration order) and returns a BitSet containing
     * the indices of all positions where {@code valueToFind} occurs. Elements are compared using
     * {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     * The implementation is optimized for {@link RandomAccess} lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "a", "c", "a", "b");
     * BitSet indices = Index.allOf(list, "a");
     * // indices contains {0, 2, 4}
     *
     * // Iterate over all matching indices
     * indices.stream().forEach(i -> System.out.println("Found at: " + i));
     *
     * // Get first and last occurrence
     * OptionalInt first = indices.stream().findFirst();   // returns 0
     * int last = indices.stream().max().orElse(-1);       // returns 4
     *
     * // Check specific index
     * boolean foundAt2 = indices.get(2);                  // returns true
     * }</pre>
     *
     * @param c the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @return a BitSet containing the zero-based indices (in iteration order) of all occurrences of the value;
     *         returns an empty BitSet if the value is not found, the collection is {@code null}, or empty
     * @see #allOf(Collection, Object, int)
     * @see #of(Collection, Object)
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind) {
        return allOf(c, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given collection, starting from the specified index.
     *
     * @param c the collection to be searched.
     * @param valueToFind the value to find in the collection.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all occurrences of the value in the collection starting from the specified index, or an empty BitSet if the value is not found or the input collection is {@code null}.
     */
    public static BitSet allOf(final Collection<?> c, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List<?> list && c instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (N.equals(list.get(idx), valueToFind)) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<?> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    bitSet.set(idx);
                }

                idx++;
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all elements in the given array that match the provided predicate.
     * <p>
     * This method tests each element in the array against the predicate and returns a BitSet
     * containing the indices of all elements for which the predicate returns {@code true}.
     * The predicate is not invoked for elements beyond a {@code null} array boundary.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"apple", "banana", "avocado", "cherry"};
     * BitSet indices = Index.allOf(arr, s -> s.startsWith("a"));
     * // indices contains {0, 2} (positions of "apple" and "avocado")
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @return a BitSet containing the zero-based indices of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the array is {@code null} or empty
     * @see #allOf(Object[], Predicate, int)
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate) {
        return allOf(a, predicate, 0);
    }

    /**
     * Returns the indices of all elements in the given array that match the provided predicate, starting from the specified index.
     * <p>
     * This method tests each element in the array (starting from {@code fromIndex}) against the predicate
     * and returns a BitSet containing the indices of all elements for which the predicate returns {@code true}.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * @param <T> the type of elements in the array
     * @param a the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all elements at or after {@code fromIndex} matching the predicate;
     *         returns an empty BitSet if no elements match, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #allOf(Object[], Predicate)
     */
    public static <T> BitSet allOf(final T[] a, final Predicate<? super T> predicate, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(a);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int idx = N.max(fromIndex, 0); idx < len; idx++) {
            if (predicate.test(a[idx])) {
                bitSet.set(idx);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all elements in the given collection that match the provided predicate.
     * <p>
     * This method tests each element in the collection (in iteration order) against the predicate
     * and returns a BitSet containing the indices of all elements for which the predicate returns {@code true}.
     * The implementation is optimized for {@code RandomAccess} lists.
     *
     * <p><b>Null Handling:</b></p>
     * <ul>
     *   <li>If {@code c} is {@code null}, returns empty BitSet</li>
     *   <li>If {@code predicate} is {@code null}, throws {@code NullPointerException}</li>
     *   <li>Null elements in the collection are passed to the predicate</li>
     * </ul>
     *
     * <p><b>Common Mistakes:</b></p>
     * <pre>{@code
     * // DON'T: Pass null predicate
     * Index.allOf(collection, null);   // NullPointerException!
     *
     * // DO: Provide valid predicate
     * Index.allOf(collection, Objects::nonNull);
     *
     * // DON'T: Assume predicate won't receive nulls
     * Index.allOf(Arrays.asList(1, null, 3), x -> x > 0);   // NPE inside predicate!
     *
     * // DO: Handle nulls in predicate
     * Index.allOf(Arrays.asList(1, null, 3), x -> x != null && x > 0);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param c the collection to be searched, may be {@code null}
     * @param predicate the predicate to test elements; must not be {@code null}
     * @return a BitSet containing the zero-based indices (in iteration order) of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the collection is {@code null} or empty
     * @see #allOf(Collection, Predicate, int)
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return allOf(c, predicate, 0);
    }

    /**
     * Returns the indices of all occurrences in the given collection for which the provided predicate returns {@code true}, starting from the specified index.
     *
     * @param <T> the type of the elements in the collection.
     * @param c the collection to be searched.
     * @param predicate the predicate to use to test the elements of the collection.
     * @param fromIndex the index to start the search from.
     * @return a BitSet containing the indices of all elements in the collection for which the predicate returns {@code true} starting from the specified index, or an empty BitSet if no elements match or the input collection is {@code null}.
     */
    public static <T> BitSet allOf(final Collection<? extends T> c, final Predicate<? super T> predicate, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(c);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (c instanceof List<? extends T> list && c instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (predicate.test(list.get(idx))) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (predicate.test(iter.next())) {
                    bitSet.set(idx);
                }

                idx++;
            }
        }

        return bitSet;
    }

    /**
     * Converts an integer index to an OptionalInt, treating negative values as "not found".
     * <p>
     * This is a helper method used internally by all index search methods. It converts the convention
     * of returning negative values (typically -1) for "not found" into an empty OptionalInt.
     *
     * @param index the index value; negative values indicate "not found"
     * @return an OptionalInt containing the index if non-negative, or an empty OptionalInt if negative
     */
    private static OptionalInt toOptionalInt(final int index) {
        return index < 0 ? NOT_FOUND : OptionalInt.of(index);
    }
}
