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
 * Index-finding operations for arrays, collections, iterators, and strings, returning an {@link OptionalInt} instead
 * of a magic {@code -1}. Every operation is a single static call; nothing here is stateful or chained.
 *
 * <p><b>Four families.</b> {@code of(...)} finds the first occurrence and {@code last(...)} the last;
 * {@code ofSubArray}/{@code ofSubList} and {@code lastOfSubArray}/{@code lastOfSubList} find a contiguous run;
 * {@code allOf(...)} returns every matching position as a {@link BitSet}.
 *
 * <p>All four families take every primitive array and object arrays. Beyond that the overload sets differ:
 * {@code of}/{@code last}/{@code allOf} also take a {@link Collection}; {@code of}/{@code last} also take a
 * {@link String}; {@code of} alone also takes an {@link Iterator}, which it consumes. The pattern searches take a
 * {@link List} rather than a {@code Collection}, under the names {@code ofSubList}/{@code lastOfSubList}. On top of
 * that there are case-insensitive variants for {@code String} and {@code String[]} ({@code ofIgnoreCase},
 * {@code lastOfIgnoreCase}), predicate-based {@code allOf} for arrays and collections, and tolerance-based
 * {@code float}/{@code double} overloads of {@code of}, {@code last} and {@code allOf}.
 *
 * <p><b>An unsuccessful search returns an empty result</b> - an empty {@link OptionalInt} from the
 * single-index families, an empty {@link BitSet} from {@code allOf}. With valid search arguments, a {@code null}
 * source produces an empty result. Empty inputs and out-of-range starts do not always mean a miss:
 * an empty pattern can match an empty input or the end of an input, and start indices are handled as described below.
 * Object elements are compared with {@link N#equals(Object, Object)}, so
 * {@code null} elements compare correctly; {@code float}/{@code double} elements are compared with
 * {@link Float#compare(float, float)} / {@link Double#compare(double, double)}, so {@code NaN} matches itself and
 * {@code -0.0} does <i>not</i> match {@code 0.0}. The tolerance overloads are the exception: they match by
 * {@link Numbers#fuzzyEquals(double, double, double)}, under which {@code -0.0} and {@code 0.0} <i>do</i> match even
 * at a tolerance of {@code 0}.
 *
 * <p><b>The two start-index conventions.</b> A forward {@code fromIndex} is inclusive and a negative value is treated
 * as {@code 0}. A backward {@code startIndexFromBack} is inclusive too, but a negative value finds nothing, and it
 * means slightly different things in the two families - because an empty pattern legitimately matches one position
 * past the last element:
 * <ul>
 *   <li>Element searches - {@code last} over an array, a {@link Collection}, or a single character of a
 *       {@link String} - read it as an <i>element</i> index; the useful range is {@code [0, length - 1]} and
 *       anything larger searches the whole input.</li>
 *   <li>Pattern searches - {@code lastOfSubArray}, {@code lastOfSubList} and the substring forms
 *       {@code last(String, String, int)} / {@code lastOfIgnoreCase(String, String, int)} - read it as the highest
 *       index at which a match may <i>start</i>; the useful range is {@code [0, length]}, and the no-argument
 *       overloads pass {@code length} so an empty pattern matches at {@code length}, exactly as
 *       {@link String#lastIndexOf(String)} does.</li>
 * </ul>
 * Empty-pattern results follow {@code String.indexOf}/{@code lastIndexOf} throughout. In contrast, the explicit
 * pattern-slice bounds ({@code startIndexOfSubArray}, {@code sizeToMatch}) are validated: a negative
 * {@code sizeToMatch} raises {@link IllegalArgumentException} and any other invalid slice raises
 * {@link IndexOutOfBoundsException}; a negative or {@code NaN} {@code tolerance}, and a {@code null}
 * {@code predicate}, raise {@link IllegalArgumentException}; and the {@link Iterator} overloads of {@code of} raise
 * {@link ArithmeticException} when the match is found past element {@code Integer.MAX_VALUE}, because the index is
 * counted as a {@code long} and then narrowed exactly.
 *
 * <p><b>Cost.</b> Element and predicate searches are O(n) and allocate nothing beyond the result, with the one
 * exception noted below. Subarray and sublist
 * searches are a straightforward scan with early termination on the first mismatch - O(n*m) worst case, with no
 * pattern preprocessing (no KMP/Boyer-Moore), so a long, highly self-similar pattern really does cost that.
 * Single-element collection searches take an indexed path when the collection is a {@link RandomAccess} list;
 * otherwise {@code of} and {@code allOf} walk the iterator, while {@code last} calls the collection's own public
 * {@code descendingIterator()} if it has one and, failing that, copies the whole collection to an array first -
 * that copy being the exception, so {@code last} on, say, a {@link java.util.LinkedHashSet} costs O(n) extra
 * space. A sublist search takes the indexed path only when <i>both</i> lists are
 * {@code RandomAccess}, and otherwise copies the searched region and the pattern to arrays first, which costs O(n)
 * extra space.
 *
 * <p>All methods are static. Array and collection contents are left unchanged; iterator searches consume input elements. Concurrent modification of an input is the
 * caller's problem, as usual.
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * int[] numbers = {1, 2, 3, 4, 5, 3, 2, 1};
 *
 * Index.of(numbers, 3);        // OptionalInt[2]
 * Index.last(numbers, 3);      // OptionalInt[5]
 * Index.of(numbers, 2, 3);     // OptionalInt[6] - searching forward from index 3
 * Index.allOf(numbers, 1);     // {0, 7}
 * Index.of(numbers, 10);       // OptionalInt.empty
 *
 * // Idiomatic handling
 * Index.of(numbers, 3).ifPresent(i -> System.out.println("found at " + i));
 * boolean present = Index.of(numbers, 3).isPresent();
 *
 * // Strings
 * Index.of("Hello World", 'o');                  // OptionalInt[4]
 * Index.of("Hello World", "World");              // OptionalInt[6]
 * Index.ofIgnoreCase("Hello World", "WORLD");    // OptionalInt[6]
 *
 * // Collections and patterns
 * List<String> document = Arrays.asList("The", "quick", "brown", "fox");
 * Index.ofSubList(document, Arrays.asList("quick", "brown"));   // OptionalInt[1]
 * Index.ofSubArray(new int[] {1, 2, 3, 4, 5}, new int[] {3, 4});   // OptionalInt[2]
 *
 * // Predicates
 * String[] words = {"apple", "apricot", "banana", "avocado"};
 * Index.allOf(words, s -> s.startsWith("a"));   // {0, 1, 3}
 *
 * // Floating point within a tolerance (the 4-arg overloads; 0 = from the start)
 * double[] measurements = {1.0, 2.001, 3.0, 2.002, 4.0};
 * Index.of(measurements, 2.0, 0, 0.01);      // OptionalInt[1]
 * Index.allOf(measurements, 2.0, 0, 0.01);   // {1, 3}
 * }</pre>
 *
 * <p><b>The same operation exists in three places, with two "not found" conventions.</b> {@code Index.of} /
 * {@code Index.last} return an empty {@link OptionalInt}; {@link N#indexOf} / {@link N#lastIndexOf} (arrays and
 * collections) and {@link Strings#indexOf(String, String)} / {@link Strings#lastIndexOf(String, String)} return a
 * primitive {@code int} using {@code -1}. Reach for this class when the {@code OptionalInt} ergonomics help, and for
 * {@code N}/{@code Strings} when a bare {@code int} is what the surrounding code wants. Note that {@code of} reads
 * like a factory method but here means <i>index of the first occurrence</i>.
 *
 * @see OptionalInt
 * @see BitSet
 * @see N#indexOf
 * @see N#lastIndexOf
 * @see Strings#indexOf(String, String)
 * @see Strings#lastIndexOf(String, String)
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
        // Utility class - prevent instantiation
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
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(boolean[], boolean, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final boolean[] source, final boolean valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
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
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final boolean[] source, final boolean valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
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
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(char[], char, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final char[] source, final char valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
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
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(char[], char)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final char[] source, final char valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code byte} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.of(arr, (byte) 10).get();                   // returns 1
     * Index.of(arr, (byte) 20).get();                   // returns 2
     * Index.of(arr, (byte) 90).isPresent();             // returns false
     * Index.of((byte[]) null, (byte) 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(byte[], byte, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final byte[] source, final byte valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified byte value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code byte} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.of(arr, (byte) 10, 0).get();          // returns 1
     * Index.of(arr, (byte) 20, 2).get();          // returns 2
     * Index.of(arr, (byte) 90, 0).isPresent();    // returns false
     * Index.of(arr, (byte) 10, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(byte[], byte)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final byte[] source, final byte valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code short} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.of(arr, (short) 10).get();                    // returns 1
     * Index.of(arr, (short) 20).get();                    // returns 2
     * Index.of(arr, (short) 90).isPresent();              // returns false
     * Index.of((short[]) null, (short) 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(short[], short, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final short[] source, final short valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified short value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code short} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.of(arr, (short) 10, 0).get();          // returns 1
     * Index.of(arr, (short) 20, 2).get();          // returns 2
     * Index.of(arr, (short) 90, 0).isPresent();    // returns false
     * Index.of(arr, (short) 10, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(short[], short)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final short[] source, final short valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code int} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.of(arr, 10).get();                  // returns 1
     * Index.of(arr, 20).get();                  // returns 2
     * Index.of(arr, 90).isPresent();            // returns false
     * Index.of((int[]) null, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(int[], int, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final int[] source, final int valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified int value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code int} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.of(arr, 10, 0).get();          // returns 1
     * Index.of(arr, 20, 2).get();          // returns 2
     * Index.of(arr, 90, 0).isPresent();    // returns false
     * Index.of(arr, 10, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(int[], int)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final int[] source, final int valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.of(arr, 10L).get();                   // returns 1
     * Index.of(arr, 20L).get();                   // returns 2
     * Index.of(arr, 90L).isPresent();             // returns false
     * Index.of((long[]) null, 10L).isPresent();   // returns false
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(long[], long, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final long[] source, final long valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified long value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.of(arr, 10L, 0).get();          // returns 1
     * Index.of(arr, 20L, 2).get();          // returns 2
     * Index.of(arr, 90L, 0).isPresent();    // returns false
     * Index.of(arr, 10L, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(long[], long)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final long[] source, final long valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.of(arr, 2.0f).get();                    // returns 1
     * Index.of(arr, 1.0f).get();                    // returns 0
     * Index.of(arr, 5.0f).isPresent();              // returns false
     * Index.of((float[]) null, 1.0f).isPresent();   // returns false
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(float[], float, int)
     * @see #of(float[], float, int, float)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final float[] source, final float valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.of(arr, 2.0f, 0).get();          // returns 1
     * Index.of(arr, 2.0f, 2).get();          // returns 3
     * Index.of(arr, 5.0f, 0).isPresent();    // returns false
     * Index.of(arr, 1.0f, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(float[], float)
     * @see #of(float[], float, int, float)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final float[] source, final float valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified float value in the array, within a given tolerance and starting from the specified index.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given float array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * Matching uses {@link Numbers#fuzzyEquals(float, float, float)}, so two {@link Float#NaN} values are
     * considered equal and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0f} matches {@code 0.0f} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(float, float, float)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #of(float[], float, int)}, which orders by {@link Float#compare(float, float)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.1f, 3.0f, 2.2f, 4.0f};
     * Index.of(arr, 2.0f, 0, 0.2f).get();         // returns 1
     * Index.of(arr, 2.0f, 2, 0.2f).isPresent();   // returns false (2.2f - 2.0f > 0.2f)
     * Index.of(arr, 5.0f, 0, 0.1f).isPresent();   // returns false
     * Index.of(arr, 2.0f, 5, 0.2f).isPresent();   // returns false
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param tolerance the tolerance for matching; must be non-negative and not NaN. A value matches if it's within
     *                  {@code valueToFind +/- tolerance}
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance at or after {@code fromIndex},
     *         or an empty OptionalInt if no value is found within tolerance, the array is {@code null}, or {@code fromIndex >= array.length}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #of(float[], float, int)
     * @see N#indexOf(float[], float, int, float)
     */
    public static OptionalInt of(final float[] source, final float valueToFind, final int fromIndex, final float tolerance) throws IllegalArgumentException {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex, tolerance));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.of(arr, 2.0).get();                     // returns 1
     * Index.of(arr, 1.0).get();                     // returns 0
     * Index.of(arr, 5.0).isPresent();               // returns false
     * Index.of((double[]) null, 1.0).isPresent();   // returns false
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #of(boolean[], boolean)
     * @see #of(double[], double, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final double[] source, final double valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, starting from the specified index.
     * <p>
     * This method works identically to {@link #of(boolean[], boolean, int)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.of(arr, 2.0, 0).get();          // returns 1
     * Index.of(arr, 2.0, 2).get();          // returns 3
     * Index.of(arr, 5.0, 0).isPresent();    // returns false
     * Index.of(arr, 1.0, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #of(boolean[], boolean, int)
     * @see #of(double[], double)
     * @see #of(double[], double, int, double)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final double[] source, final double valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified double value in the array, within a given tolerance and starting from the specified index.
     * <p>
     * This method searches for the first occurrence of a value that falls within the range
     * {@code [valueToFind - tolerance, valueToFind + tolerance]} in the given double array,
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * Matching uses {@link Numbers#fuzzyEquals(double, double, double)}, so two {@link Double#NaN} values are
     * considered equal and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0} matches {@code 0.0} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(double, double, double)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #of(double[], double, int)}, which orders by {@link Double#compare(double, double)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.1, 3.0, 2.2, 4.0};
     * Index.of(arr, 2.0, 0, 0.2).get();         // returns 1
     * Index.of(arr, 2.0, 2, 0.2).isPresent();   // returns false (2.2 - 2.0 = 0.200...018 > 0.2)
     * Index.of(arr, 5.0, 0, 0.1).isPresent();   // returns false
     * Index.of(arr, 2.0, 5, 0.2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param tolerance the tolerance for matching; must be non-negative and not NaN. A value matches if it's within
     *                  {@code valueToFind +/- tolerance}
     * @return an OptionalInt containing the zero-based index of the first occurrence of a value within tolerance at or after {@code fromIndex},
     *         or an empty OptionalInt if no value is found within tolerance, the array is {@code null}, or {@code fromIndex >= array.length}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #of(double[], double, int)
     * @see N#indexOf(double[], double, int, double)
     */
    public static OptionalInt of(final double[] source, final double valueToFind, final int fromIndex, final double tolerance) throws IllegalArgumentException {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex, tolerance));
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
     * @param source the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt of(final Object[] source, final Object valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the array, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given object array,
     * beginning at the specified {@code fromIndex}. Uses {@code equals()} for comparison.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b", "a"};
     * Index.of(arr, "b", 0).get();         // returns 1
     * Index.of(arr, "b", 2).get();         // returns 3
     * Index.of(arr, "b", 4).isPresent();   // returns false
     * Index.of(arr, "a", 0).get();         // returns 0
     * }</pre>
     *
     * @param source the object array to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #of(Object[], Object)
     */
    public static OptionalInt of(final Object[] source, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the collection.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} in the given collection
     * using {@code equals()} for comparison. The index represents the position in iteration order.
     * {@code null} values are handled correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b");
     * Index.of(list, "b").get();                   // returns 1
     * Index.of(list, "a").get();                   // returns 0
     * Index.of(list, "x").isPresent();             // returns false
     * Index.of((List<?>) null, "a").isPresent();   // returns false
     * }</pre>
     *
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #of(Collection, Object, int)
     */
    public static OptionalInt of(final Collection<?> source, final Object valueToFind) {
        return toOptionalInt(N.indexOf(source, valueToFind));
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
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found, the collection is {@code null}, or {@code fromIndex >= collection.size()}
     * @see #of(Collection, Object)
     */
    public static OptionalInt of(final Collection<?> source, final Object valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} by iterating through the iterator
     * and using {@code equals()} for comparison. The iterator will be consumed up to and including the matching element.
     * Note that the iterator cannot be reset, so this operation is destructive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * Index.of(list.iterator(), "b").get();            // returns 1
     * Index.of(list.iterator(), "x").isPresent();      // returns false
     * Index.of((Iterator<?>) null, "a").isPresent();   // returns false
     * }</pre>
     *
     * @param source the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @throws ArithmeticException if the matching value has a zero-based index greater than {@link Integer#MAX_VALUE}.
     * @see #of(Iterator, Object, int)
     */
    public static OptionalInt of(final Iterator<?> source, final Object valueToFind) throws ArithmeticException {
        return toOptionalInt(N.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified object in the iterator, starting from the specified index.
     * <p>
     * This method skips the first {@code fromIndex} elements, then searches for {@code valueToFind} using {@code equals()}
     * for comparison. The iterator will be consumed up to and including the matching element (or exhausted if not found).
     * Note that the iterator cannot be reset, so this operation is destructive. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b");
     * Index.of(list.iterator(), "b", 1).get();            // returns 1
     * Index.of(list.iterator(), "x", 0).isPresent();      // returns false
     * Index.of((Iterator<?>) null, "a", 0).isPresent();   // returns false
     * }</pre>
     *
     * @param source the iterator to be searched, may be {@code null}
     * @param valueToFind the object to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index (in iteration order) of the first occurrence of the value at or after {@code fromIndex},
     *         or an empty OptionalInt if the value is not found or the iterator is {@code null}
     * @throws ArithmeticException if the matching value has a zero-based index greater than {@link Integer#MAX_VALUE}.
     * @see #of(Iterator, Object)
     */
    public static OptionalInt of(final Iterator<?> source, final Object valueToFind, final int fromIndex) throws ArithmeticException {
        return toOptionalInt(N.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified character in the string.
     * <p>
     * This method searches for the first occurrence of the character (represented as an int Unicode code point)
     * in the given string. If the string is {@code null} or empty, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello", 'o').get();               // returns 4
     * Index.of("hello", 'h').get();               // returns 0
     * Index.of("hello", 'x').isPresent();         // returns false
     * Index.of((String) null, 'a').isPresent();   // returns false
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character,
     *         or an empty OptionalInt if the character is not found or the string is {@code null}
     * @see #of(String, int, int)
     * @see Strings#indexOf(String, int)
     * @see String#indexOf(int)
     */
    public static OptionalInt of(final String source, final int charValueToFind) {
        return toOptionalInt(Strings.indexOf(source, charValueToFind));
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
     * @param source the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the character at or after {@code fromIndex},
     *         or an empty OptionalInt if the character is not found, the string is {@code null}, or {@code fromIndex >= source.length()}
     * @see #of(String, int)
     * @see Strings#indexOf(String, int, int)
     * @see String#indexOf(int, int)
     */
    public static OptionalInt of(final String source, final int charValueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(source, charValueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code source}.
     * If either argument is {@code null}, an empty OptionalInt is returned. Note that an <i>empty</i>
     * {@code source} is not automatically a miss: as with {@link String#indexOf(String)}, an empty
     * {@code valueToFind} is found at index 0, so {@code Index.of("", "")} returns {@code OptionalInt[0]}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello world", "world").get();       // returns 6
     * Index.of("hello world", "bye").isPresent();   // returns false
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring,
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #of(String, String, int)
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOf(String, String)
     * @see String#indexOf(String)
     */
    public static OptionalInt of(final String source, final String valueToFind) {
        return toOptionalInt(Strings.indexOf(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, starting from the specified index.
     * <p>
     * This method searches for the first occurrence of {@code valueToFind} within {@code source},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.of("hello world hello", "hello", 1).get();   // returns 12
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring at or after {@code fromIndex}
     *         (an empty substring returns {@code Math.min(Math.max(fromIndex, 0), source.length())}), or an empty OptionalInt if the substring is not found or either
     *         parameter is {@code null}
     * @see #of(String, String)
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOf(String, String, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt of(final String source, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOf(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code source}.
     * Matching is a same-length UTF-16 compare via
     * {@link String#regionMatches(boolean, int, String, int, int)}, not Unicode case folding: non-ASCII characters
     * are compared case-insensitively, but length-changing mappings such as {@code "ß"}/{@code "SS"} do not match.
     * As with {@link String#indexOf(String)}, an empty {@code valueToFind} is found at index 0, so
     * {@code Index.ofIgnoreCase("", "")} returns {@code OptionalInt[0]}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.ofIgnoreCase("Hello World", "world").get();       // returns 6
     * Index.ofIgnoreCase("Hello World", "HELLO").get();       // returns 0
     * Index.ofIgnoreCase("Hello World", "bye").isPresent();   // returns false
     * Index.ofIgnoreCase((String) null, "a").isPresent();     // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} never matches here - a substring search for "no string" has no
     * answer - whereas the {@code String[]} overload treats it as a search for a {@code null} <i>element</i>.</p>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case),
     *         or an empty OptionalInt if the substring is not found or either parameter is {@code null}
     * @see #ofIgnoreCase(String, String, int)
     * @see Strings#indexOfIgnoreCase(String, String)
     */
    public static OptionalInt ofIgnoreCase(final String source, final String valueToFind) {
        return toOptionalInt(Strings.indexOfIgnoreCase(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified substring in the given string, ignoring case and starting from the specified index.
     * <p>
     * This method performs a case-insensitive search for {@code valueToFind} within {@code source},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     * Matching is a same-length UTF-16 compare via
     * {@link String#regionMatches(boolean, int, String, int, int)}, not Unicode case folding: non-ASCII characters
     * are compared case-insensitively, but length-changing mappings such as {@code "ß"}/{@code "SS"} do not match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.ofIgnoreCase("Hello Hello", "HELLO", 1).get();   // returns 6
     * Index.ofIgnoreCase("Hello World", "WORLD", 0).get();   // returns 6
     * Index.ofIgnoreCase("Hello", "bye", 0).isPresent();     // returns false
     * Index.ofIgnoreCase("Hello", "HELLO", 5).isPresent();   // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} never matches here - a substring search for "no string" has no
     * answer - whereas the {@code String[]} overload treats it as a search for a {@code null} <i>element</i>.</p>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first occurrence of the substring (ignoring case) at or after {@code fromIndex}
     *         (an empty substring returns {@code Math.min(Math.max(fromIndex, 0), source.length())}), or an empty OptionalInt if the substring is not found or either
     *         parameter is {@code null}
     * @see #ofIgnoreCase(String, String)
     * @see Strings#indexOfIgnoreCase(String, String, int)
     */
    public static OptionalInt ofIgnoreCase(final String source, final String valueToFind, final int fromIndex) {
        return toOptionalInt(Strings.indexOfIgnoreCase(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified string in the given string array, ignoring case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.ofIgnoreCase(new String[] {"Hello", "World"}, "hello").get();   // returns 0
     * Index.ofIgnoreCase(new String[] {"Hello"}, "xyz").isPresent();        // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} matches a {@code null} <i>element</i> of the array, unlike the
     * {@code String} overload, where a {@code null} substring never matches.</p>
     *
     * @param source the string array to be searched, may be {@code null}
     * @param valueToFind the string to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the first element equal (ignoring case) to {@code valueToFind},
     *         or an empty OptionalInt if not found or the array is {@code null}
     * @see #ofIgnoreCase(String[], String, int)
     * @see N#indexOfIgnoreCase(String[], String)
     */
    public static OptionalInt ofIgnoreCase(final String[] source, final String valueToFind) {
        return toOptionalInt(N.indexOfIgnoreCase(source, valueToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified string in the given string array, ignoring case, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.ofIgnoreCase(new String[] {"Hello", "World", "HELLO"}, "hello", 1).get();   // returns 2
     * Index.ofIgnoreCase(new String[] {"Hello"}, "xyz", 0).isPresent();                 // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} matches a {@code null} <i>element</i> of the array, unlike the
     * {@code String} overload, where a {@code null} substring never matches.</p>
     *
     * @param source the string array to be searched, may be {@code null}
     * @param valueToFind the string to search for (case-insensitive), may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return an OptionalInt containing the zero-based index of the first element equal (ignoring case) to {@code valueToFind} at or after {@code fromIndex},
     *         or an empty OptionalInt if not found, the array is {@code null}, or {@code fromIndex >= array.length}
     * @see #ofIgnoreCase(String[], String)
     * @see N#indexOfIgnoreCase(String[], String, int)
     */
    public static OptionalInt ofIgnoreCase(final String[] source, final String valueToFind, final int fromIndex) {
        return toOptionalInt(N.indexOfIgnoreCase(source, valueToFind, fromIndex));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source}.
     * It's similar to {@link String#indexOf(String)} but for boolean arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false};
     * boolean[] sub = {true, true};
     * Index.ofSubArray(source, sub).get();   // returns 2
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final boolean[] source, final boolean[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] source, final int fromIndex, final boolean[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false};
     * boolean[] sub = {true, true, false};
     * Index.ofSubArray(source, 0, sub, 0, 2).get();                   // returns 2
     * Index.ofSubArray(source, 0, sub, 0, 3).get();                   // returns 2
     * Index.ofSubArray(source, 3, sub, 0, 2).isPresent();             // returns false
     * Index.ofSubArray(source, 3, sub, 0, 0).get();                   // returns 3 (empty match clamped to fromIndex)
     * Index.ofSubArray((boolean[]) null, 0, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final boolean[] source, final int fromIndex, final boolean[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source}.
     * It's similar to {@link String#indexOf(String)} but for char arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'l', 'l', 'o'};
     * Index.ofSubArray(source, sub).get();   // returns 2
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(char[], int, char[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final char[] source, final char[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source},
     * beginning at the specified {@code fromIndex}. Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'o', 'r'};
     * Index.ofSubArray(source, 0, sub).get();         // returns 7
     * Index.ofSubArray(source, 5, sub).get();         // returns 7
     * Index.ofSubArray(source, 9, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(char[], char[])
     * @see #ofSubArray(char[], int, char[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] source, final int fromIndex, final char[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This allows for flexible partial subarray matching.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
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
     * // Start search from index 5 (still finds the match at index 7)
     * Index.ofSubArray(source, 5, sub, 1, 3).get();   // returns 7
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(char[], char[])
     * @see #ofSubArray(char[], int, char[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final char[] source, final int fromIndex, final char[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source}.
     * This is particularly useful for binary data pattern matching.
     * It's similar to {@link String#indexOf(String)} but for byte arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50};
     * byte[] sub = {(byte) 30, (byte) 40};
     * Index.ofSubArray(source, sub).get();                // returns 3
     * Index.ofSubArray(new byte[0], sub).isPresent();     // returns false
     * Index.ofSubArray((byte[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(byte[], int, byte[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final byte[] source, final byte[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(byte[], byte[])
     * @see #ofSubArray(byte[], int, byte[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] source, final int fromIndex, final byte[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(byte[], byte[])
     * @see #ofSubArray(byte[], int, byte[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final byte[] source, final int fromIndex, final byte[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code short} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40, (short) 50};
     * short[] sub = {(short) 30, (short) 40};
     * Index.ofSubArray(source, sub).get();                 // returns 3
     * Index.ofSubArray(new short[0], sub).isPresent();     // returns false
     * Index.ofSubArray((short[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(short[], int, short[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final short[] source, final short[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(short[], short[])
     * @see #ofSubArray(short[], int, short[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] source, final int fromIndex, final short[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(short[], short[])
     * @see #ofSubArray(short[], int, short[])
     * @see #ofSubArray(boolean[], int, boolean[], int, int)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final short[] source, final int fromIndex, final short[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code int} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {0, 10, 20, 30, 40, 50};
     * int[] sub = {30, 40};
     * Index.ofSubArray(source, sub).get();               // returns 3
     * Index.ofSubArray(new int[0], sub).isPresent();     // returns false
     * Index.ofSubArray((int[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(int[], int, int[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final int[] source, final int[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code int} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {0, 10, 20, 30, 40, 50};
     * int[] sub = {20, 30};
     * Index.ofSubArray(source, 0, sub).get();               // returns 2
     * Index.ofSubArray(source, 3, sub).isPresent();         // returns false
     * Index.ofSubArray(source, 10, sub).isPresent();        // returns false
     * Index.ofSubArray((int[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(int[], int[])
     * @see #ofSubArray(int[], int, int[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] source, final int fromIndex, final int[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This allows for flexible partial subarray matching.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[], int, int)} for {@code int} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {1, 2, 3, 2, 3, 4};
     * int[] sub = {2, 3, 9};
     * Index.ofSubArray(source, 0, sub, 0, 2).get();               // returns 1
     * Index.ofSubArray(source, 2, sub, 0, 2).get();               // returns 3
     * Index.ofSubArray(source, 0, sub, 0, 3).isPresent();         // returns false
     * Index.ofSubArray(source, 2, sub, 0, 0).get();               // returns 2 (empty match clamped to fromIndex)
     * Index.ofSubArray((int[]) null, 0, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(int[], int[])
     * @see #ofSubArray(int[], int, int[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final int[] source, final int fromIndex, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code long} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {0L, 10L, 20L, 30L, 40L, 50L};
     * long[] sub = {30L, 40L};
     * Index.ofSubArray(source, sub).get();                // returns 3
     * Index.ofSubArray(new long[0], sub).isPresent();     // returns false
     * Index.ofSubArray((long[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(long[], int, long[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final long[] source, final long[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code long} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {0L, 10L, 20L, 30L, 40L, 50L};
     * long[] sub = {20L, 30L};
     * Index.ofSubArray(source, 0, sub).get();                // returns 2
     * Index.ofSubArray(source, 3, sub).isPresent();          // returns false
     * Index.ofSubArray(source, 10, sub).isPresent();         // returns false
     * Index.ofSubArray((long[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(long[], long[])
     * @see #ofSubArray(long[], int, long[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] source, final int fromIndex, final long[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. This allows for flexible partial subarray matching.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[], int, int)} for {@code long} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {1L, 2L, 3L, 2L, 3L, 4L};
     * long[] sub = {2L, 3L, 9L};
     * Index.ofSubArray(source, 0, sub, 0, 2).get();                // returns 1
     * Index.ofSubArray(source, 2, sub, 0, 2).get();                // returns 3
     * Index.ofSubArray(source, 0, sub, 0, 3).isPresent();          // returns false
     * Index.ofSubArray(source, 2, sub, 0, 0).get();                // returns 2 (empty match clamped to fromIndex)
     * Index.ofSubArray((long[]) null, 0, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(long[], long[])
     * @see #ofSubArray(long[], int, long[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final long[] source, final int fromIndex, final long[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (source[k] != subArrayToFind[j]) {
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
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code float} arrays.
     * Elements are compared using {@link N#equals(float, float)}, consistent with {@link Float#compare(float, float)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {0.0f, 10.0f, 20.0f, 30.0f, 40.0f, 50.0f};
     * float[] sub = {30.0f, 40.0f};
     * Index.ofSubArray(source, sub).get();                 // returns 3
     * Index.ofSubArray(new float[0], sub).isPresent();     // returns false
     * Index.ofSubArray((float[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(float[], int, float[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final float[] source, final float[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code float} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {0.0f, 10.0f, 20.0f, 30.0f, 40.0f, 50.0f};
     * float[] sub = {20.0f, 30.0f};
     * Index.ofSubArray(source, 0, sub).get();                 // returns 2
     * Index.ofSubArray(source, 3, sub).isPresent();           // returns false
     * Index.ofSubArray(source, 10, sub).isPresent();          // returns false
     * Index.ofSubArray((float[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(float[], float[])
     * @see #ofSubArray(float[], int, float[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] source, final int fromIndex, final float[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. Elements are compared using {@link N#equals(float, float)},
     * consistent with {@link Float#compare(float, float)}. This allows for flexible partial subarray matching.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {1f, 2f, 3f, 2f, 3f, 4f};
     * float[] sub = {2f, 3f, 9f};
     * Index.ofSubArray(source, 0, sub, 0, 2).get();                 // returns 1
     * Index.ofSubArray(source, 2, sub, 0, 2).get();                 // returns 3
     * Index.ofSubArray(source, 0, sub, 0, 3).isPresent();           // returns false
     * Index.ofSubArray(source, 2, sub, 0, 0).get();                 // returns 2 (empty match clamped to fromIndex)
     * Index.ofSubArray((float[]) null, 0, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(float[], float[])
     * @see #ofSubArray(float[], int, float[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final float[] source, final int fromIndex, final float[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(source[k], subArrayToFind[j])) {
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
     * This method works identically to {@link #ofSubArray(boolean[], boolean[])} for {@code double} arrays.
     * Elements are compared using {@link N#equals(double, double)}, consistent with {@link Double#compare(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0};
     * double[] sub = {30.0, 40.0};
     * Index.ofSubArray(source, sub).get();                  // returns 3
     * Index.ofSubArray(new double[0], sub).isPresent();     // returns false
     * Index.ofSubArray((double[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(boolean[], boolean[])
     * @see #ofSubArray(double[], int, double[])
     * @see #ofSubArray(Object[], Object[])
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final double[] source, final double[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method works identically to {@link #ofSubArray(boolean[], int, boolean[])} for {@code double} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0};
     * double[] sub = {20.0, 30.0};
     * Index.ofSubArray(source, 0, sub).get();                  // returns 2
     * Index.ofSubArray(source, 3, sub).isPresent();            // returns false
     * Index.ofSubArray(source, 10, sub).isPresent();           // returns false
     * Index.ofSubArray((double[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(boolean[], int, boolean[])
     * @see #ofSubArray(double[], double[])
     * @see #ofSubArray(double[], int, double[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] source, final int fromIndex, final double[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. Elements are compared using {@link N#equals(double, double)},
     * consistent with {@link Double#compare(double, double)}. This allows for flexible partial subarray matching.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {1d, 2d, 3d, 2d, 3d, 4d};
     * double[] sub = {2d, 3d, 9d};
     * Index.ofSubArray(source, 0, sub, 0, 2).get();                  // returns 1
     * Index.ofSubArray(source, 2, sub, 0, 2).get();                  // returns 3
     * Index.ofSubArray(source, 0, sub, 0, 3).isPresent();            // returns false
     * Index.ofSubArray(source, 2, sub, 0, 0).get();                  // returns 2 (empty match clamped to fromIndex)
     * Index.ofSubArray((double[]) null, 0, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(double[], double[])
     * @see #ofSubArray(double[], int, double[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final double[] source, final int fromIndex, final double[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(source[k], subArrayToFind[j])) {
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
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source}.
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
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     *         (including a zero-length source), matching {@link String#indexOf(String)}
     * @see #ofSubArray(Object[], int, Object[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubArray(final Object[] source, final Object[] subArrayToFind) {
        return ofSubArray(source, 0, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the specified subarray in the given source array, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subArrayToFind} as a contiguous sequence within {@code source},
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
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the subarray starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}. Note an EMPTY
     *         subarray is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.length)},
     *         so {@code fromIndex >= source.length} yields {@code source.length}, not an empty result
     * @see #ofSubArray(Object[], Object[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] source, final int fromIndex, final Object[] subArrayToFind) {
        return ofSubArray(source, fromIndex, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified subarray in the given source array.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subArrayToFind}
     * starting at {@code startIndexOfSubArray}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. This allows for flexible partial subarray matching.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both arrays are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.length} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
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
     * // Start search from index 3, matching {"d", "e"} (elements at indices 1-2 of pattern)
     * Index.ofSubArray(source, 3, pattern, 1, 2).get();   // returns 3 (matches {"d", "e"} at positions 3-4)
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the subarray portion is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #ofSubArray(Object[], Object[])
     * @see #ofSubArray(Object[], int, Object[])
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubArray(final Object[] source, final int fromIndex, final Object[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subArrayToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++, j++) {
                if (!N.equals(source[k], subArrayToFind[j])) {
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
     * This method searches for the complete {@code subListToFind} as a contiguous sequence within {@code source}.
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
     * @param source the list to be searched, may be {@code null}
     * @param subListToFind the sublist to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the sublist starts,
     *         or an empty OptionalInt if the sublist is not found or either list is {@code null}.
     *         An empty pattern is a zero-width match at index {@code 0} of a non-null source
     * @see #ofSubList(List, int, List)
     * @see #ofSubList(List, int, List, int, int)
     * @see #ofSubArray(Object[], Object[])
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String)
     */
    public static OptionalInt ofSubList(final List<?> source, final List<?> subListToFind) {
        return ofSubList(source, 0, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the specified sublist in the given source list, starting from the specified index.
     * <p>
     * This method searches for the complete {@code subListToFind} as a contiguous sequence within {@code source},
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
     * @param source the list to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subListToFind the sublist to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the sublist starts at or after {@code fromIndex},
     *         or an empty OptionalInt if the sublist is not found or either list is {@code null}. Note an EMPTY
     *         sublist is a zero-width match that is always found: it answers {@code min(max(fromIndex, 0), source.size())},
     *         so {@code fromIndex >= source.size()} yields {@code source.size()}, not an empty result
     * @see #ofSubList(List, List)
     * @see #ofSubList(List, int, List, int, int)
     * @see #ofSubArray(Object[], int, Object[])
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> source, final int fromIndex, final List<?> subListToFind) {
        return ofSubList(source, fromIndex, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the first occurrence of a portion of the specified sublist in the given source list.
     * <p>
     * This method searches for the first occurrence of a portion of {@code subListToFind} within {@code source},
     * starting the search at {@code fromIndex}. It looks for {@code sizeToMatch} elements from {@code subListToFind}
     * starting at {@code startIndexOfSubList}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly. This allows for flexible partial sublist matching.
     * <p>
     * The implementation is optimized for {@link RandomAccess} lists. For non-RandomAccess lists,
     * it converts sublists to arrays for comparison.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0 and both lists are {@code non-null}, returns {@code fromIndex} (clamped to valid range)</li>
     *   <li>If either list is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code fromIndex} is negative, it's treated as 0</li>
     *   <li>If {@code fromIndex >= source.size()} (and {@code sizeToMatch > 0}), returns empty OptionalInt</li>
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
     * // Start search from index 3, matching {"d", "e"} (elements at indices 1-2 of pattern)
     * Index.ofSubList(source, 3, pattern, 1, 2).get();   // returns 3 (matches {"d", "e"} at positions 3-4)
     * }</pre>
     *
     * @param source the list to be searched, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param subListToFind the sublist to search for, may be {@code null}
     * @param startIndexOfSubList the starting index within {@code subListToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subListToFind}
     * @return an OptionalInt containing the zero-based index where the sublist portion is found,
     *         or an empty OptionalInt if the sublist is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubList} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subListToFind}
     * @see #ofSubList(List, List)
     * @see #ofSubList(List, int, List)
     * @see #ofSubArray(Object[], int, Object[], int, int)
     * @see Collections#indexOfSubList(List, List)
     * @see String#indexOf(String, int)
     */
    public static OptionalInt ofSubList(final List<?> source, final int fromIndex, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(source);

        // "aaa".indexOf("", -1) => 0
        // "aaa".indexOf("") => 0
        // "aaa".indexOf("", 1) => 1
        // "aaa".indexOf("", 3) => 3
        // "aaa".indexOf("", 4) => 3
        if (sizeToMatch == 0) {
            if (source == null || subListToFind == null) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(fromIndex < 0 ? 0 : N.min(fromIndex, len));
            }
        }

        if (source == null || subListToFind == null || fromIndex >= len || len - N.max(fromIndex, 0) < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (source instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.max(fromIndex, 0), maxFromIndex = len - sizeToMatch; i <= maxFromIndex; i++) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++, j++) {
                    if (!N.equals(source.get(k), subListToFind.get(j))) {
                        break;
                    } else if (j == endIndexOfTargetSubList - 1) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            final int adjustedFromIndex = N.max(fromIndex, 0);
            final OptionalInt result = ofSubArray(source.subList(adjustedFromIndex, source.size()).toArray(), 0,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);

            return result.isPresent() ? toOptionalInt(result.orElseThrow() + adjustedFromIndex) : result;
        }
    }

    /**
     * Returns the index of the last occurrence of the specified boolean value in the given array.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of {@code valueToFind}.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {false, true, false, true};
     * Index.last(arr, true).get();                      // returns 3
     * Index.last(arr, false).get();                     // returns 2
     * Index.last(arr, true).orElse(-1);                 // returns 3
     * Index.last((boolean[]) null, true).isPresent();   // returns false
     * }</pre>
     *
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final boolean[] source, final boolean valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified boolean value in the given array, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} (inclusive) towards the beginning of the array.
     * If {@code startIndexFromBack} is greater than or equal to the array length, the entire array is searched.
     * If the array is {@code null} or empty, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {false, true, false, true, false};
     * Index.last(arr, true, 3).get();    // returns 3
     * Index.last(arr, true, 1).get();    // returns 1
     * Index.last(arr, false, 3).get();   // returns 2
     * Index.last(arr, true, 10).get();   // returns 3
     * }</pre>
     *
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final boolean[] source, final boolean valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified char value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code char} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.last(arr, 'l').get();                   // returns 3
     * Index.last(arr, 'h').get();                   // returns 0
     * Index.last(arr, 'x').isPresent();             // returns false
     * Index.last((char[]) null, 'a').isPresent();   // returns false
     * }</pre>
     *
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(char[], char, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final char[] source, final char valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified char value in the given array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code char} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.last(arr, 'l', 3).get();         // returns 3
     * Index.last(arr, 'l', 2).get();         // returns 2
     * Index.last(arr, 'h', 0).get();         // returns 0
     * Index.last(arr, 'x', 3).isPresent();   // returns false
     * }</pre>
     *
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); if greater than
     *                            or equal to the array length, the entire array is searched, and a negative
     *                            value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(char[], char)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final char[] source, final char valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified byte value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code byte} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.last(arr, (byte) 10).get();                   // returns 1
     * Index.last(arr, (byte) 20).get();                   // returns 2
     * Index.last(arr, (byte) 90).isPresent();             // returns false
     * Index.last((byte[]) null, (byte) 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(byte[], byte, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final byte[] source, final byte valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified byte value in the given array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code byte} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.last(arr, (byte) 10, 2).get();         // returns 1
     * Index.last(arr, (byte) 20, 4).get();         // returns 2
     * Index.last(arr, (byte) 90, 3).isPresent();   // returns false
     * Index.last(arr, (byte) 10, 10).get();        // returns 1
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); if greater than
     *                            or equal to the array length, the entire array is searched, and a negative
     *                            value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(byte[], byte)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final byte[] source, final byte valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified short value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code short} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.last(arr, (short) 10).get();                    // returns 1
     * Index.last(arr, (short) 20).get();                    // returns 2
     * Index.last(arr, (short) 90).isPresent();              // returns false
     * Index.last((short[]) null, (short) 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(short[], short, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final short[] source, final short valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified short value in the given array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code short} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.last(arr, (short) 10, 2).get();         // returns 1
     * Index.last(arr, (short) 20, 4).get();         // returns 2
     * Index.last(arr, (short) 90, 3).isPresent();   // returns false
     * Index.last(arr, (short) 10, 10).get();        // returns 1
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); if greater than
     *                            or equal to the array length, the entire array is searched, and a negative
     *                            value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(short[], short)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final short[] source, final short valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified int value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code int} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.last(arr, 10).get();                  // returns 1
     * Index.last(arr, 20).get();                  // returns 2
     * Index.last(arr, 90).isPresent();            // returns false
     * Index.last((int[]) null, 10).isPresent();   // returns false
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(int[], int, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final int[] source, final int valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified int value in the given array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code int} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.last(arr, 10, 2).get();         // returns 1
     * Index.last(arr, 20, 4).get();         // returns 2
     * Index.last(arr, 90, 3).isPresent();   // returns false
     * Index.last(arr, 10, 10).get();        // returns 1
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); if greater than
     *                            or equal to the array length, the entire array is searched, and a negative
     *                            value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(int[], int)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final int[] source, final int valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified long value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.last(arr, 10L).get();                   // returns 1
     * Index.last(arr, 20L).get();                   // returns 2
     * Index.last(arr, 90L).isPresent();             // returns false
     * Index.last((long[]) null, 10L).isPresent();   // returns false
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(long[], long, int)
     * @see #last(Object[], Object)
     */
    public static OptionalInt last(final long[] source, final long valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified long value in the given array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.last(arr, 10L, 2).get();         // returns 1
     * Index.last(arr, 20L, 4).get();         // returns 2
     * Index.last(arr, 90L, 3).isPresent();   // returns false
     * Index.last(arr, 10L, 10).get();        // returns 1
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); if greater than
     *                            or equal to the array length, the entire array is searched, and a negative
     *                            value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(long[], long)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final long[] source, final long valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified float value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.last(arr, 2.0f).get();                    // returns 3
     * Index.last(arr, 1.0f).get();                    // returns 0
     * Index.last(arr, 5.0f).isPresent();              // returns false
     * Index.last((float[]) null, 1.0f).isPresent();   // returns false
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(float[], float, int)
     * @see #last(float[], float, int, float)
     * @see #last(Object[], Object)
     * @see Float#compare(float, float)
     */
    public static OptionalInt last(final float[] source, final float valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified float value in the given array, searching backwards from a specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code float} values.
     * Comparison is performed using {@link Float#compare(float, float)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.last(arr, 2.0f, 3).get();         // returns 3
     * Index.last(arr, 2.0f, 1).get();         // returns 1
     * Index.last(arr, 5.0f, 2).isPresent();   // returns false
     * Index.last(arr, 1.0f, 0).get();         // returns 0
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(float[], float)
     * @see #last(float[], float, int, float)
     * @see #last(Object[], Object, int)
     * @see Float#compare(float, float)
     */
    public static OptionalInt last(final float[] source, final float valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified float value in the given array within a specified tolerance,
     * searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of a value
     * that falls within the range {@code [valueToFind - tolerance, valueToFind + tolerance]}.
     * Matching uses {@link Numbers#fuzzyEquals(float, float, float)}, so two {@link Float#NaN} values are
     * considered equal and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0f} matches {@code 0.0f} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(float, float, float)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #last(float[], float, int)}, which orders by {@link Float#compare(float, float)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.1f, 3.0f, 2.2f, 4.0f};
     * Index.last(arr, 2.0f, 4, 0.2f).get();                    // returns 1 (2.2f at index 3 excluded: 2.2f - 2.0f > 0.2f)
     * Index.last(arr, 2.0f, 2, 0.2f).get();                    // returns 1
     * Index.last(arr, 5.0f, 3, 0.1f).isPresent();              // returns false
     * Index.last((float[]) null, 2.0f, 4, 0.1f).isPresent();   // returns false
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @param tolerance the tolerance for matching; must be non-negative and not NaN. A value matches if it's within
     *                  {@code valueToFind +/- tolerance}
     * @return an OptionalInt containing the zero-based index of the last occurrence of a value within tolerance at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #last(float[], float, int)
     * @see N#lastIndexOf(float[], float, int, float)
     */
    public static OptionalInt last(final float[] source, final float valueToFind, final int startIndexFromBack, final float tolerance)
            throws IllegalArgumentException {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack, tolerance));
    }

    /**
     * Returns the index of the last occurrence of the specified double value in the given array.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.last(arr, 2.0).get();                     // returns 3
     * Index.last(arr, 1.0).get();                     // returns 0
     * Index.last(arr, 5.0).isPresent();               // returns false
     * Index.last((double[]) null, 1.0).isPresent();   // returns false
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean)
     * @see #last(double[], double, int)
     * @see #last(Object[], Object)
     * @see Double#compare(double, double)
     */
    public static OptionalInt last(final double[] source, final double valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified double value in the given array, searching backwards from a specified position.
     * <p>
     * This method works identically to {@link #last(boolean[], boolean, int)} for {@code double} values.
     * Comparison is performed using {@link Double#compare(double, double)}, which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.last(arr, 2.0, 3).get();         // returns 3
     * Index.last(arr, 2.0, 1).get();         // returns 1
     * Index.last(arr, 5.0, 2).isPresent();   // returns false
     * Index.last(arr, 1.0, 0).get();         // returns 0
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null} or empty
     * @see #last(boolean[], boolean, int)
     * @see #last(double[], double)
     * @see #last(Object[], Object, int)
     * @see Double#compare(double, double)
     */
    public static OptionalInt last(final double[] source, final double valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified double value in the given array within a specified tolerance,
     * searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of a value
     * that falls within the range {@code [valueToFind - tolerance, valueToFind + tolerance]}.
     * Matching uses {@link Numbers#fuzzyEquals(double, double, double)}, so two {@link Double#NaN} values are
     * considered equal and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0} matches {@code 0.0} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(double, double, double)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #last(double[], double, int)}, which orders by {@link Double#compare(double, double)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.1, 3.0, 2.2, 4.0};
     * Index.last(arr, 2.0, 4, 0.2).get();                     // returns 1 (2.2 at index 3 is outside the tolerance)
     * Index.last(arr, 2.0, 2, 0.2).get();                     // returns 1
     * Index.last(arr, 5.0, 3, 0.1).isPresent();               // returns false
     * Index.last((double[]) null, 2.0, 4, 0.1).isPresent();   // returns false
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @param tolerance the tolerance for matching; must be non-negative and not NaN. A value matches if it's within
     *                  {@code valueToFind +/- tolerance}
     * @return an OptionalInt containing the zero-based index of the last occurrence of a value within tolerance at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if no value is found within tolerance or the array is {@code null}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #last(double[], double, int)
     * @see N#lastIndexOf(double[], double, int, double)
     */
    public static OptionalInt last(final double[] source, final double valueToFind, final int startIndexFromBack, final double tolerance)
            throws IllegalArgumentException {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack, tolerance));
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
     * @param source the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #last(Object[], Object, int)
     * @see #of(Object[], Object)
     */
    public static OptionalInt last(final Object[] source, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
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
     * @param source the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @param startIndexFromBack the index to start the search from (inclusive), searching backwards; a value at
     *                           or beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the array is {@code null}
     * @see #last(Object[], Object)
     * @see #of(Object[], Object, int)
     */
    public static OptionalInt last(final Object[] source, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
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
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @return an OptionalInt containing the zero-based index (in iteration order) of the last occurrence of the value,
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #last(Collection, Object, int)
     * @see #of(Collection, Object)
     */
    public static OptionalInt last(final Collection<?> source, final Object valueToFind) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified value in the given collection, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} (inclusive) towards the beginning of the collection
     * for the last occurrence of {@code valueToFind}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b");
     * Index.last(list, "b", 3).get();         // returns 3
     * Index.last(list, "b", 2).get();         // returns 1
     * Index.last(list, "a", 0).get();         // returns 0
     * Index.last(list, "x", 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @param startIndexFromBack the index to start the search from (inclusive), searching backwards; a value at
     *                           or beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index (in iteration order) of the last occurrence of the value at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the value is not found or the collection is {@code null}
     * @see #last(Collection, Object)
     * @see #last(Object[], Object, int)
     */
    public static OptionalInt last(final Collection<?> source, final Object valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified character in the given string.
     * <p>
     * If the string is {@code null}, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.last("hello", 'l').get();               // returns 3
     * Index.last("hello", 'o').get();               // returns 4
     * Index.last("hello", 'x').isPresent();         // returns false
     * Index.last((String) null, 'a').isPresent();   // returns false
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @return an OptionalInt containing the zero-based index of the last occurrence of the character,
     *         or an empty OptionalInt if the character is not found or the string is {@code null}
     * @see #last(String, int, int)
     * @see Strings#lastIndexOf(String, int)
     * @see String#lastIndexOf(int)
     */
    public static OptionalInt last(final String source, final int charValueToFind) {
        return toOptionalInt(Strings.lastIndexOf(source, charValueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified character in the given string, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of the character.
     * If the string is {@code null}, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.last("hello", 'l', 3).get();         // returns 3
     * Index.last("hello", 'l', 2).get();         // returns 2
     * Index.last("hello", 'h', 0).get();         // returns 0
     * Index.last("hello", 'x', 3).isPresent();   // returns false
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param charValueToFind the character value (Unicode code point) to search for
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the character at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the character is not found, the string is {@code null}, or {@code startIndexFromBack < 0}
     * @see #last(String, int)
     * @see Strings#lastIndexOf(String, int, int)
     * @see String#lastIndexOf(int, int)
     */
    public static OptionalInt last(final String source, final int charValueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(source, charValueToFind, startIndexFromBack));
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
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring
     *         (an empty substring returns {@code source.length()}), or an empty OptionalInt if the substring is not
     *         found or either parameter is {@code null}
     * @see #last(String, String, int)
     * @see #lastOfIgnoreCase(String, String)
     * @see #of(String, String)
     * @see Strings#lastIndexOf(String, String)
     * @see String#lastIndexOf(String)
     */
    public static OptionalInt last(final String source, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOf(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified substring in the given string, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of {@code valueToFind}
     * within {@code source}. If the string is {@code null}, an empty OptionalInt is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.last("hello world hello", "hello", 12).get();   // returns 12
     * Index.last("hello world hello", "hello", 6).get();    // returns 0
     * Index.last("hello", "bye", 4).isPresent();            // returns false
     * Index.last("hello", "he", 1).get();                   // returns 0
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for, may be {@code null}
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring at or before {@code startIndexFromBack}
     *         (an empty substring returns {@code Math.min(startIndexFromBack, source.length())}), or an empty
     *         OptionalInt if the substring is not found, either parameter is {@code null}, or {@code startIndexFromBack < 0}
     * @see #last(String, String)
     * @see Strings#lastIndexOf(String, String, int)
     * @see String#lastIndexOf(String, int)
     */
    public static OptionalInt last(final String source, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOf(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified substring in the given string, ignoring case.
     * <p>
     * This method performs a case-insensitive backwards search for {@code valueToFind} within {@code source},
     * starting from the end of the string. Matching is a same-length UTF-16 compare via
     * {@link String#regionMatches(boolean, int, String, int, int)}, not Unicode case folding: non-ASCII characters
     * are compared case-insensitively, but length-changing mappings such as {@code "ß"}/{@code "SS"} do not match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.lastOfIgnoreCase("Hello World Hello", "Hello").get();   // returns 12
     * Index.lastOfIgnoreCase("Hello World", "WORLD").get();         // returns 6
     * Index.lastOfIgnoreCase("Hello", "bye").isPresent();           // returns false
     * Index.lastOfIgnoreCase((String) null, "a").isPresent();       // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} never matches here - a substring search for "no string" has no
     * answer - whereas the {@code String[]} overload treats it as a search for a {@code null} <i>element</i>.</p>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring (ignoring case)
     *         (an empty substring returns {@code source.length()}), or an empty OptionalInt if the substring is not
     *         found or either parameter is {@code null}
     * @see #lastOfIgnoreCase(String, String, int)
     * @see Strings#lastIndexOfIgnoreCase(String, String)
     */
    public static OptionalInt lastOfIgnoreCase(final String source, final String valueToFind) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified substring in the given string, ignoring case and searching backwards from the specified position.
     * <p>
     * This method performs a case-insensitive backwards search for {@code valueToFind} within {@code source},
     * starting from the specified {@code startIndexFromBack}. Matching is a same-length UTF-16 compare via
     * {@link String#regionMatches(boolean, int, String, int, int)}, not Unicode case folding: non-ASCII characters
     * are compared case-insensitively, but length-changing mappings such as {@code "ß"}/{@code "SS"} do not match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.lastOfIgnoreCase("Hello World Hello", "HELLO", 12).get();   // returns 12
     * Index.lastOfIgnoreCase("Hello World Hello", "HELLO", 6).get();    // returns 0
     * Index.lastOfIgnoreCase("Hello", "bye", 4).isPresent();            // returns false
     * Index.lastOfIgnoreCase("Hello", "he", 1).get();                   // returns 0
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} never matches here - a substring search for "no string" has no
     * answer - whereas the {@code String[]} overload treats it as a search for a {@code null} <i>element</i>.</p>
     *
     * @param source the string to be searched, may be {@code null}
     * @param valueToFind the substring to search for (case-insensitive), may be {@code null}
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last occurrence of the substring (ignoring case) at or before {@code startIndexFromBack}
     *         (an empty substring returns {@code Math.min(startIndexFromBack, source.length())}), or an empty
     *         OptionalInt if the substring is not found, either parameter is {@code null}, or {@code startIndexFromBack < 0}
     * @see #lastOfIgnoreCase(String, String)
     * @see Strings#lastIndexOfIgnoreCase(String, String, int)
     */
    public static OptionalInt lastOfIgnoreCase(final String source, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(Strings.lastIndexOfIgnoreCase(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified string in the given string array, ignoring case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.lastOfIgnoreCase(new String[] {"Hello", "World", "HELLO"}, "hello").get();   // returns 2
     * Index.lastOfIgnoreCase(new String[] {"Hello"}, "xyz").isPresent();                 // returns false
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} matches a {@code null} <i>element</i> of the array, unlike the
     * {@code String} overload, where a {@code null} substring never matches.</p>
     *
     * @param source the string array to be searched, may be {@code null}
     * @param valueToFind the string to search for (case-insensitive), may be {@code null}
     * @return an OptionalInt containing the zero-based index of the last element equal (ignoring case) to {@code valueToFind},
     *         or an empty OptionalInt if not found or the array is {@code null}
     * @see #lastOfIgnoreCase(String[], String, int)
     * @see N#lastIndexOfIgnoreCase(String[], String)
     */
    public static OptionalInt lastOfIgnoreCase(final String[] source, final String valueToFind) {
        return toOptionalInt(N.lastIndexOfIgnoreCase(source, valueToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified string in the given string array, ignoring case, searching backwards from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Index.lastOfIgnoreCase(new String[] {"Hello", "World", "HELLO"}, "hello", 2).get();   // returns 2
     * Index.lastOfIgnoreCase(new String[] {"Hello", "World", "HELLO"}, "hello", 1).get();   // returns 0
     * }</pre>
     *
     * <p>A {@code null} {@code valueToFind} matches a {@code null} <i>element</i> of the array, unlike the
     * {@code String} overload, where a {@code null} substring never matches.</p>
     *
     * @param source the string array to be searched, may be {@code null}
     * @param valueToFind the string to search for (case-insensitive), may be {@code null}
     * @param startIndexFromBack the position to start the backwards search from (inclusive); a value at or
     *                           beyond the end searches the whole input, and a negative value finds nothing
     * @return an OptionalInt containing the zero-based index of the last element equal (ignoring case) to {@code valueToFind} at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if not found or the array is {@code null}
     * @see #lastOfIgnoreCase(String[], String)
     * @see N#lastIndexOfIgnoreCase(String[], String, int)
     */
    public static OptionalInt lastOfIgnoreCase(final String[] source, final String valueToFind, final int startIndexFromBack) {
        return toOptionalInt(N.lastIndexOfIgnoreCase(source, valueToFind, startIndexFromBack));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of {@code subArrayToFind}
     * as a contiguous sequence within {@code source}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false, true, true};
     * boolean[] sub = {true, true};
     * Index.lastOfSubArray(source, sub).get();                   // returns 5
     * Index.lastOfSubArray(source, new boolean[0]).get();        // returns 7
     * Index.lastOfSubArray((boolean[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final boolean[] source, final boolean[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of {@code subArrayToFind}
     * as a contiguous sequence within {@code source}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, false, true, true, false, true, true};
     * boolean[] sub = {true, true};
     * Index.lastOfSubArray(source, 6, sub).get();                   // returns 5
     * Index.lastOfSubArray(source, 2, sub).get();                   // returns 2
     * Index.lastOfSubArray(source, 1, sub).isPresent();             // returns false
     * Index.lastOfSubArray((boolean[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(boolean[], int, boolean[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     */
    public static OptionalInt lastOfSubArray(final boolean[] source, final int startIndexFromBack, final boolean[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from a specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] source = {true, true, false, true, true, false};
     * boolean[] sub = {true, true, false};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                   // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                   // returns 0
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();            // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                   // returns 5 (empty match)
     * Index.lastOfSubArray((boolean[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final boolean[] source, final int startIndexFromBack, final boolean[] subArrayToFind,
            final int startIndexOfSubArray, final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        // "aaa".lastIndexOf("") = 3
        // "aaa".lastIndexOf("", 0) = 0
        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code char} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'l', 'l'};
     * Index.lastOfSubArray(source, sub).get();                          // returns 2
     * Index.lastOfSubArray(source, new char[]{'x', 'y'}).isPresent();   // returns false
     * Index.lastOfSubArray((char[]) null, sub).isPresent();             // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(char[], int, char[])
     * @see #lastOfSubArray(Object[], Object[])
     */
    public static OptionalInt lastOfSubArray(final char[] source, final char[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code char} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};
     * char[] sub = {'l', 'l'};
     * Index.lastOfSubArray(source, 10, sub).get();               // returns 2
     * Index.lastOfSubArray(source, 1, sub).isPresent();          // returns false
     * Index.lastOfSubArray(source, 6, sub).get();                // returns 2
     * Index.lastOfSubArray((char[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(char[], char[])
     * @see #lastOfSubArray(Object[], int, Object[])
     */
    public static OptionalInt lastOfSubArray(final char[] source, final int startIndexFromBack, final char[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code char} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] source = {'a', 'b', 'c', 'b', 'c', 'd'};
     * char[] sub = {'b', 'c', 'z'};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();         // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                // returns 5 (empty match)
     * Index.lastOfSubArray((char[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(char[], char[])
     * @see #lastOfSubArray(char[], int, char[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final char[] source, final int startIndexFromBack, final char[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code byte} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50, (byte) 60};
     * byte[] sub = {(byte) 10, (byte) 20};
     * Index.lastOfSubArray(source, sub).get();                // returns 1
     * Index.lastOfSubArray(new byte[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((byte[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(byte[], int, byte[])
     * @see #lastOfSubArray(Object[], Object[])
     */
    public static OptionalInt lastOfSubArray(final byte[] source, final byte[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code byte} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50, (byte) 60};
     * byte[] sub = {(byte) 10, (byte) 20};
     * Index.lastOfSubArray(source, 6, sub).get();                // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();          // returns false
     * Index.lastOfSubArray(source, 10, sub).get();               // returns 1
     * Index.lastOfSubArray((byte[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(byte[], byte[])
     * @see #lastOfSubArray(Object[], int, Object[])
     */
    public static OptionalInt lastOfSubArray(final byte[] source, final int startIndexFromBack, final byte[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code byte} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] source = {1, 2, 3, 2, 3, 4};
     * byte[] sub = {2, 3, 9};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();         // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                // returns 5 (empty match)
     * Index.lastOfSubArray((byte[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(byte[], byte[])
     * @see #lastOfSubArray(byte[], int, byte[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final byte[] source, final int startIndexFromBack, final byte[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code short} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40, (short) 50, (short) 60};
     * short[] sub = {(short) 10, (short) 20};
     * Index.lastOfSubArray(source, sub).get();                 // returns 1
     * Index.lastOfSubArray(new short[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((short[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(short[], int, short[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final short[] source, final short[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code short} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40, (short) 50, (short) 60};
     * short[] sub = {(short) 10, (short) 20};
     * Index.lastOfSubArray(source, 6, sub).get();                 // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();           // returns false
     * Index.lastOfSubArray(source, 10, sub).get();                // returns 1
     * Index.lastOfSubArray((short[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(short[], short[])
     * @see #lastOfSubArray(short[], int, short[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final short[] source, final int startIndexFromBack, final short[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code short} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] source = {1, 2, 3, 2, 3, 4};
     * short[] sub = {2, 3, 9};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                 // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                 // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();          // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                 // returns 5 (empty match)
     * Index.lastOfSubArray((short[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(short[], short[])
     * @see #lastOfSubArray(short[], int, short[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final short[] source, final int startIndexFromBack, final short[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code int} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {0, 10, 20, 30, 40, 50, 60};
     * int[] sub = {10, 20};
     * Index.lastOfSubArray(source, sub).get();               // returns 1
     * Index.lastOfSubArray(new int[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((int[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(int[], int, int[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final int[] source, final int[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code int} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {0, 10, 20, 30, 40, 50, 60};
     * int[] sub = {10, 20};
     * Index.lastOfSubArray(source, 6, sub).get();               // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();         // returns false
     * Index.lastOfSubArray(source, 10, sub).get();              // returns 1
     * Index.lastOfSubArray((int[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(int[], int[])
     * @see #lastOfSubArray(int[], int, int[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final int[] source, final int startIndexFromBack, final int[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code int} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] source = {1, 2, 3, 2, 3, 4};
     * int[] sub = {2, 3, 9};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();               // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();               // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();        // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();               // returns 5 (empty match)
     * Index.lastOfSubArray((int[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(int[], int[])
     * @see #lastOfSubArray(int[], int, int[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final int[] source, final int startIndexFromBack, final int[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code long} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {0L, 10L, 20L, 30L, 40L, 50L, 60L};
     * long[] sub = {10L, 20L};
     * Index.lastOfSubArray(source, sub).get();                // returns 1
     * Index.lastOfSubArray(new long[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((long[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(long[], int, long[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final long[] source, final long[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code long} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {0L, 10L, 20L, 30L, 40L, 50L, 60L};
     * long[] sub = {10L, 20L};
     * Index.lastOfSubArray(source, 6, sub).get();                // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();          // returns false
     * Index.lastOfSubArray(source, 10, sub).get();               // returns 1
     * Index.lastOfSubArray((long[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(long[], long[])
     * @see #lastOfSubArray(long[], int, long[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final long[] source, final int startIndexFromBack, final long[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code long} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] source = {1L, 2L, 3L, 2L, 3L, 4L};
     * long[] sub = {2L, 3L, 9L};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();         // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                // returns 5 (empty match)
     * Index.lastOfSubArray((long[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(long[], long[])
     * @see #lastOfSubArray(long[], int, long[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final long[] source, final int startIndexFromBack, final long[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (source[k] != subArrayToFind[j++]) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code float} arrays.
     * Elements are compared using {@link N#equals(float, float)}, consistent with {@link Float#compare(float, float)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {0.0f, 10.0f, 20.0f, 30.0f, 40.0f, 50.0f, 60.0f};
     * float[] sub = {10.0f, 20.0f};
     * Index.lastOfSubArray(source, sub).get();                 // returns 1
     * Index.lastOfSubArray(new float[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((float[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(float[], int, float[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final float[] source, final float[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code float} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {0.0f, 10.0f, 20.0f, 30.0f, 40.0f, 50.0f, 60.0f};
     * float[] sub = {10.0f, 20.0f};
     * Index.lastOfSubArray(source, 6, sub).get();                 // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();           // returns false
     * Index.lastOfSubArray(source, 10, sub).get();                // returns 1
     * Index.lastOfSubArray((float[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(float[], float[])
     * @see #lastOfSubArray(float[], int, float[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final float[] source, final int startIndexFromBack, final float[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}. Elements are compared using
     * {@link N#equals(float, float)}, consistent with {@link Float#compare(float, float)}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code float} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] source = {1f, 2f, 3f, 2f, 3f, 4f};
     * float[] sub = {2f, 3f, 9f};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                 // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                 // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();          // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                 // returns 5 (empty match)
     * Index.lastOfSubArray((float[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(float[], float[])
     * @see #lastOfSubArray(float[], int, float[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final float[] source, final int startIndexFromBack, final float[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(source[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], boolean[])} for {@code double} arrays.
     * Elements are compared using {@link N#equals(double, double)}, consistent with {@link Double#compare(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0};
     * double[] sub = {10.0, 20.0};
     * Index.lastOfSubArray(source, sub).get();                  // returns 1
     * Index.lastOfSubArray(new double[0], sub).isPresent();     // returns false
     * Index.lastOfSubArray((double[]) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(boolean[], boolean[])
     * @see #lastOfSubArray(double[], int, double[])
     * @see #lastOfSubArray(Object[], Object[])
     * @see Strings#lastIndexOf(String, String)
     */
    public static OptionalInt lastOfSubArray(final double[] source, final double[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[])} for {@code double} arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0};
     * double[] sub = {10.0, 20.0};
     * Index.lastOfSubArray(source, 6, sub).get();                  // returns 1
     * Index.lastOfSubArray(source, 0, sub).isPresent();            // returns false
     * Index.lastOfSubArray(source, 10, sub).get();                 // returns 1
     * Index.lastOfSubArray((double[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(boolean[], int, boolean[])
     * @see #lastOfSubArray(double[], double[])
     * @see #lastOfSubArray(double[], int, double[], int, int)
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see Strings#lastIndexOf(String, String, int)
     */
    public static OptionalInt lastOfSubArray(final double[] source, final int startIndexFromBack, final double[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}. Elements are compared using
     * {@link N#equals(double, double)}, consistent with {@link Double#compare(double, double)}.
     * <p>
     * This method works identically to {@link #lastOfSubArray(boolean[], int, boolean[], int, int)} for {@code double} arrays.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] source = {1d, 2d, 3d, 2d, 3d, 4d};
     * double[] sub = {2d, 3d, 9d};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                  // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                  // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();           // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                  // returns 5 (empty match)
     * Index.lastOfSubArray((double[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(double[], double[])
     * @see #lastOfSubArray(double[], int, double[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final double[] source, final int startIndexFromBack, final double[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(source[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array.
     * <p>
     * This method searches backwards from the end of the array for the last occurrence of {@code subArrayToFind}
     * as a contiguous sequence within {@code source}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "d", "c", "d", "e"};
     * String[] sub = {"c", "d"};
     * Index.lastOfSubArray(source, sub).get();                            // returns 4
     * Index.lastOfSubArray(source, new String[]{"x", "y"}).isPresent();   // returns false
     * Index.lastOfSubArray((String[]) null, sub).isPresent();             // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts,
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.length} for the two-arg form
     *         (and at {@code min(startIndexFromBack, length)} for the indexed form when {@code startIndexFromBack >= 0};
     *         a negative {@code startIndexFromBack} finds nothing, as in {@link String#lastIndexOf(String, int)}), matching
     *         {@link String#lastIndexOf(String)}
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see #ofSubArray(Object[], Object[])
     */
    public static OptionalInt lastOfSubArray(final Object[] source, final Object[] subArrayToFind) {
        return lastOfSubArray(source, N.len(source), subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of {@code subArrayToFind}
     * as a contiguous sequence within {@code source}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "d", "c", "d", "e"};
     * String[] sub = {"c", "d"};
     * Index.lastOfSubArray(source, 6, sub).get();                  // returns 4
     * Index.lastOfSubArray(source, 2, sub).get();                  // returns 2
     * Index.lastOfSubArray(source, 1, sub).isPresent();            // returns false
     * Index.lastOfSubArray((String[]) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the subarray is not found or either array is {@code null}
     * @see #lastOfSubArray(Object[], Object[])
     * @see #lastOfSubArray(Object[], int, Object[], int, int)
     * @see #ofSubArray(Object[], int, Object[])
     */
    public static OptionalInt lastOfSubArray(final Object[] source, final int startIndexFromBack, final Object[] subArrayToFind) {
        return lastOfSubArray(source, startIndexFromBack, subArrayToFind, 0, N.len(subArrayToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified subarray in the given source array, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subArrayToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subArrayToFind} starting at {@code startIndexOfSubArray}. Elements are compared using
     * {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both arrays are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.length)}</li>
     *   <li>If either array is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.length < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] source = {"a", "b", "c", "b", "c", "d"};
     * String[] sub = {"b", "c", "z"};
     * Index.lastOfSubArray(source, 5, sub, 0, 2).get();                  // returns 3
     * Index.lastOfSubArray(source, 2, sub, 0, 2).get();                  // returns 1
     * Index.lastOfSubArray(source, -1, sub, 0, 2).isPresent();           // returns false
     * Index.lastOfSubArray(source, 5, sub, 0, 0).get();                  // returns 5 (empty match)
     * Index.lastOfSubArray((Object[]) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subArrayToFind the subarray to search for, may be {@code null}
     * @param startIndexOfSubArray the starting index within {@code subArrayToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subArrayToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the subarray is found,
     *         or an empty OptionalInt if the subarray is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubArray} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subArrayToFind}
     * @see #lastOfSubArray(Object[], Object[])
     * @see #lastOfSubArray(Object[], int, Object[])
     * @see #ofSubArray(Object[], int, Object[], int, int)
     */
    public static OptionalInt lastOfSubArray(final Object[] source, final int startIndexFromBack, final Object[] subArrayToFind, final int startIndexOfSubArray,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubArray, sizeToMatch, N.len(subArrayToFind));

        final int len = N.len(source);

        if (sizeToMatch == 0) {
            if (source == null || subArrayToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subArrayToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        final int endIndexOfTargetSubArray = startIndexOfSubArray + sizeToMatch;

        for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
            for (int k = i, j = startIndexOfSubArray; j < endIndexOfTargetSubArray; k++) {
                if (!N.equals(source[k], subArrayToFind[j++])) {
                    break;
                } else if (j == endIndexOfTargetSubArray) {
                    return toOptionalInt(i);
                }
            }
        }

        return toOptionalInt(N.INDEX_NOT_FOUND);
    }

    /**
     * Returns the index of the last occurrence of the specified sub-list in the given source list.
     * <p>
     * This method searches backwards from the end of the list for the last occurrence of {@code subListToFind}
     * as a contiguous sequence within {@code source}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d", "e");
     * List<String> sub = Arrays.asList("c", "d");
     * Index.lastOfSubList(source, sub).get();                 // returns 4
     * Index.lastOfSubList(source, Arrays.asList()).get();     // returns 7
     * Index.lastOfSubList((List<?>) null, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the list to be searched, may be {@code null}
     * @param subListToFind the sub-list to find in the source list, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the sub-list starts,
     *         or an empty OptionalInt if the sub-list is not found or either list is {@code null}.
     *         An empty pattern is a zero-width match at {@code source.size()} of a non-null source
     * @see #lastOfSubList(List, int, List)
     * @see #ofSubList(List, List)
     */
    public static OptionalInt lastOfSubList(final List<?> source, final List<?> subListToFind) {
        return lastOfSubList(source, N.size(source), subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the last occurrence of the specified sub-list in the given source list, searching backwards from the specified position.
     * <p>
     * This method searches backwards from {@code startIndexFromBack} for the last occurrence of {@code subListToFind}
     * as a contiguous sequence within {@code source}. Elements are compared using {@link N#equals(Object, Object)},
     * which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d", "e");
     * List<String> sub = Arrays.asList("c", "d");
     * Index.lastOfSubList(source, 6, sub).get();                 // returns 4
     * Index.lastOfSubList(source, 3, sub).get();                 // returns 2
     * Index.lastOfSubList(source, 1, sub).isPresent();           // returns false
     * Index.lastOfSubList((List<?>) null, 0, sub).isPresent();   // returns false
     * }</pre>
     *
     * @param source the list to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subListToFind the sub-list to find in the source list, may be {@code null}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the sub-list starts at or before {@code startIndexFromBack},
     *         or an empty OptionalInt if the sub-list is not found or either list is {@code null}
     * @see #lastOfSubList(List, List)
     * @see #lastOfSubList(List, int, List, int, int)
     * @see #ofSubList(List, int, List)
     */
    public static OptionalInt lastOfSubList(final List<?> source, final int startIndexFromBack, final List<?> subListToFind) {
        return lastOfSubList(source, startIndexFromBack, subListToFind, 0, N.size(subListToFind));
    }

    /**
     * Returns the index of the last occurrence of a portion of the specified sub-list in the given source list, searching backwards from the specified position.
     * <p>
     * This method searches backwards for the last occurrence of a portion of {@code subListToFind} within {@code source},
     * starting the backwards search at {@code startIndexFromBack}. It looks for {@code sizeToMatch} elements from
     * {@code subListToFind} starting at {@code startIndexOfSubList}. Elements are compared using
     * {@link N#equals(Object, Object)}, which handles {@code null} values correctly. The implementation is
     * optimized for {@link RandomAccess} lists; for non-RandomAccess lists it converts sublists to arrays for comparison.
     * <p>
     * Special cases (after validating the pattern slice, treating a null pattern as length zero):
     * <ul>
     *   <li>If {@code sizeToMatch} is 0, {@code startIndexFromBack >= 0}, and both lists are {@code non-null},
     *       returns {@code min(startIndexFromBack, source.size())}</li>
     *   <li>If either list is {@code null}, returns empty OptionalInt</li>
     *   <li>If {@code startIndexFromBack < 0}, returns empty OptionalInt</li>
     *   <li>If {@code source.size() < sizeToMatch}, returns empty OptionalInt</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> source = Arrays.asList("a", "b", "c", "b", "c", "d");
     * List<String> sub = Arrays.asList("b", "c", "z");
     * Index.lastOfSubList(source, 5, sub, 0, 2).get();                 // returns 3
     * Index.lastOfSubList(source, 2, sub, 0, 2).get();                 // returns 1
     * Index.lastOfSubList(source, -1, sub, 0, 2).isPresent();          // returns false
     * Index.lastOfSubList(source, 5, sub, 0, 0).get();                 // returns 5 (empty match)
     * Index.lastOfSubList((List<?>) null, 5, sub, 0, 2).isPresent();   // returns false
     * }</pre>
     *
     * @param source the list to be searched, may be {@code null}
     * @param startIndexFromBack the highest index at which a match may start; the search includes this position.
     *                           A value at or beyond the end searches the whole input, and a negative value
     *                           finds nothing (an empty pattern included)
     * @param subListToFind the sub-list to find in the source list, may be {@code null}
     * @param startIndexOfSubList the starting index within {@code subListToFind} of the portion to match
     * @param sizeToMatch the number of elements to match from {@code subListToFind}
     * @return an OptionalInt containing the zero-based index where the last occurrence of the sub-list is found,
     *         or an empty OptionalInt if the sub-list is not found or either input is {@code null}
     * @throws IllegalArgumentException if {@code sizeToMatch} is negative
     * @throws IndexOutOfBoundsException if {@code startIndexOfSubList} and {@code sizeToMatch} do not denote
     *                                   a valid range in {@code subListToFind}
     * @see #lastOfSubList(List, List)
     * @see #lastOfSubList(List, int, List)
     * @see #ofSubList(List, int, List, int, int)
     */
    public static OptionalInt lastOfSubList(final List<?> source, final int startIndexFromBack, final List<?> subListToFind, final int startIndexOfSubList,
            final int sizeToMatch) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(startIndexOfSubList, sizeToMatch, N.size(subListToFind));

        final int len = N.size(source);

        if (sizeToMatch == 0) {
            if (source == null || subListToFind == null || startIndexFromBack < 0) {
                return toOptionalInt(N.INDEX_NOT_FOUND);
            } else {
                return toOptionalInt(Math.min(startIndexFromBack, len));
            }
        }

        if (source == null || subListToFind == null || startIndexFromBack < 0 || len < sizeToMatch) {
            return toOptionalInt(N.INDEX_NOT_FOUND);
        }

        if (source instanceof RandomAccess && subListToFind instanceof RandomAccess) {
            final int endIndexOfTargetSubList = startIndexOfSubList + sizeToMatch;

            for (int i = N.min(startIndexFromBack, len - sizeToMatch); i >= 0; i--) {
                for (int k = i, j = startIndexOfSubList; j < endIndexOfTargetSubList; k++) {
                    if (!N.equals(source.get(k), subListToFind.get(j++))) {
                        break;
                    } else if (j == endIndexOfTargetSubList) {
                        return toOptionalInt(i);
                    }
                }
            }

            return toOptionalInt(N.INDEX_NOT_FOUND);
        } else {
            return lastOfSubArray(source.subList(0, N.min(startIndexFromBack, len - sizeToMatch) + sizeToMatch).toArray(), startIndexFromBack,
                    subListToFind.subList(startIndexOfSubList, startIndexOfSubList + sizeToMatch).toArray(), 0, sizeToMatch);
        }
    }

    /**
     * Returns the indices of all occurrences of the specified boolean value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit in the BitSet corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {true, false, true, false, true};
     * Index.allOf(arr, true).cardinality();            // returns 3
     * Index.allOf(arr, true).get(0);                   // returns true
     * Index.allOf(arr, true).toString();               // returns "{0, 2, 4}"
     * Index.allOf((boolean[]) null, true).isEmpty();   // returns true
     * }</pre>
     *
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value in the array;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(boolean[], boolean, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final boolean[] source, final boolean valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified boolean value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] arr = {true, false, true, false, true};
     * Index.allOf(arr, true, 0).cardinality();    // returns 3
     * Index.allOf(arr, true, 2).toString();       // returns "{2, 4}"
     * Index.allOf(arr, false, 0).cardinality();   // returns 2
     * }</pre>
     *
     * @param source the boolean array to be searched, may be {@code null}
     * @param valueToFind the boolean value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(boolean[], boolean)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final boolean[] source, final boolean valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified byte value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.allOf(arr, (byte) 10).cardinality();   // returns 1
     * Index.allOf(arr, (byte) 10).toString();      // returns "{1}"
     * Index.allOf(arr, (byte) 90).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(byte[], byte, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final byte[] source, final byte valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified byte value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {(byte) 0, (byte) 10, (byte) 20, (byte) 30, (byte) 40};
     * Index.allOf(arr, (byte) 10, 0).cardinality();   // returns 1
     * Index.allOf(arr, (byte) 20, 2).cardinality();   // returns 1
     * Index.allOf(arr, (byte) 90, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the byte array to be searched, may be {@code null}
     * @param valueToFind the byte value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(byte[], byte)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final byte[] source, final byte valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified char value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.allOf(arr, 'l').cardinality();   // returns 2
     * Index.allOf(arr, 'l').get(2);          // returns true
     * Index.allOf(arr, 'l').toString();      // returns "{2, 3}"
     * Index.allOf(arr, 'x').isEmpty();       // returns true
     * }</pre>
     *
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(char[], char, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final char[] source, final char valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified char value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'h', 'e', 'l', 'l', 'o'};
     * Index.allOf(arr, 'l', 0).cardinality();   // returns 2
     * Index.allOf(arr, 'l', 3).toString();      // returns "{3}"
     * Index.allOf(arr, 'x', 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the char array to be searched, may be {@code null}
     * @param valueToFind the char value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(char[], char)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final char[] source, final char valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified short value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.allOf(arr, (short) 10).cardinality();   // returns 1
     * Index.allOf(arr, (short) 10).toString();      // returns "{1}"
     * Index.allOf(arr, (short) 90).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(short[], short, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final short[] source, final short valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified short value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] arr = {(short) 0, (short) 10, (short) 20, (short) 30, (short) 40};
     * Index.allOf(arr, (short) 10, 0).cardinality();   // returns 1
     * Index.allOf(arr, (short) 20, 2).cardinality();   // returns 1
     * Index.allOf(arr, (short) 90, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the short array to be searched, may be {@code null}
     * @param valueToFind the short value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(short[], short)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final short[] source, final short valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified int value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.allOf(arr, 10).cardinality();   // returns 1
     * Index.allOf(arr, 10).toString();      // returns "{1}"
     * Index.allOf(arr, 90).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(int[], int, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final int[] source, final int valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified int value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] arr = {0, 10, 20, 30, 40};
     * Index.allOf(arr, 10, 0).cardinality();   // returns 1
     * Index.allOf(arr, 20, 2).cardinality();   // returns 1
     * Index.allOf(arr, 90, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the int array to be searched, may be {@code null}
     * @param valueToFind the int value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(int[], int)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final int[] source, final int valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified long value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Each set bit corresponds to an index where the value was found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.allOf(arr, 10L).cardinality();   // returns 1
     * Index.allOf(arr, 10L).toString();      // returns "{1}"
     * Index.allOf(arr, 90L).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(long[], long, int)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final long[] source, final long valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified long value in the given array, starting from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] arr = {0L, 10L, 20L, 30L, 40L};
     * Index.allOf(arr, 10L, 0).cardinality();   // returns 1
     * Index.allOf(arr, 20L, 2).cardinality();   // returns 1
     * Index.allOf(arr, 90L, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the long array to be searched, may be {@code null}
     * @param valueToFind the long value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(long[], long)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final long[] source, final long valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (source[i] == valueToFind) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified float value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Comparison is performed using {@link Float#compare(float, float)},
     * which handles NaN and -0.0/+0.0 correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.allOf(arr, 2.0f).cardinality();   // returns 2
     * Index.allOf(arr, 2.0f).toString();      // returns "{1, 3}"
     * Index.allOf(arr, 5.0f).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(float[], float, int)
     * @see #allOf(float[], float, int, float)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final float[] source, final float valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified float value in the given array, starting from the specified index.
     * <p>
     * Comparison is performed using {@link Float#compare(float, float)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.0f, 3.0f, 2.0f, 4.0f};
     * Index.allOf(arr, 2.0f, 0).cardinality();   // returns 2
     * Index.allOf(arr, 2.0f, 2).toString();      // returns "{3}"
     * Index.allOf(arr, 5.0f, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(float[], float)
     * @see #allOf(float[], float, int, float)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final float[] source, final float valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Float.compare(source[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array within a specified tolerance, starting from the specified index.
     * <p>
     * Matching uses {@link Numbers#fuzzyEquals(float, float, float)}, which is consistent with
     * {@link #of(float[], float, int, float)} and {@link #last(float[], float, int, float)}.
     * In particular, two {@link Float#NaN} values are considered equal, and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0f} matches {@code 0.0f} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(float, float, float)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #allOf(float[], float, int)}, which orders by {@link Float#compare(float, float)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] arr = {1.0f, 2.1f, 3.0f, 2.2f, 4.0f};
     * Index.allOf(arr, 2.0f, 0, 0.2f).cardinality();   // returns 1  (only 2.1f matches; 2.2f - 2.0f > 0.2f)
     * Index.allOf(arr, 2.0f, 2, 0.2f).isEmpty();       // returns true (no match from index 2)
     * Index.allOf(arr, 5.0f, 0, 0.1f).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the float array to be searched, may be {@code null}
     * @param valueToFind the float value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param tolerance the tolerance within which matches will be found; must be non-negative and not NaN
     * @return a BitSet containing the zero-based indices of all values within the specified tolerance at or after {@code fromIndex};
     *         returns an empty BitSet if no values are found within tolerance, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #allOf(float[], float, int)
     */
    public static BitSet allOf(final float[] source, final float valueToFind, final int fromIndex, final float tolerance) throws IllegalArgumentException {
        N.checkArgNotNegative(tolerance, cs.tolerance);

        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Numbers.fuzzyEquals(source[i], valueToFind, tolerance)) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified double value in the given array.
     * <p>
     * This method finds all positions where {@code valueToFind} appears in the array and returns
     * them as a BitSet. Comparison is performed using {@link Double#compare(double, double)},
     * which handles NaN and -0.0/+0.0 correctly. For tolerance-based matching, use
     * {@link #allOf(double[], double, int, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.allOf(arr, 2.0).cardinality();   // returns 2
     * Index.allOf(arr, 2.0).toString();      // returns "{1, 3}"
     * Index.allOf(arr, 5.0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(double[], double, int)
     * @see #allOf(double[], double, int, double)
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final double[] source, final double valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified double value in the given array, starting from the specified index.
     * <p>
     * Comparison is performed using {@link Double#compare(double, double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.0, 3.0, 2.0, 4.0};
     * Index.allOf(arr, 2.0, 0).cardinality();   // returns 2
     * Index.allOf(arr, 2.0, 2).toString();      // returns "{3}"
     * Index.allOf(arr, 5.0, 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(double[], double)
     * @see #allOf(double[], double, int, double)
     * @see #allOf(Object[], Object, int)
     */
    public static BitSet allOf(final double[] source, final double valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Double.compare(source[i], valueToFind) == 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array within a specified tolerance, starting from the specified index.
     * <p>
     * Matching uses {@link Numbers#fuzzyEquals(double, double, double)}, which is consistent with
     * {@link #of(double[], double, int, double)} and {@link #last(double[], double, int, double)}.
     * In particular, two {@link Double#NaN} values are considered equal, and infinities of the same sign match.
     *
     * <p><b>Signed zero:</b> {@code -0.0} matches {@code 0.0} here, and it does so even when
     * {@code tolerance} is {@code 0}, because {@link Numbers#fuzzyEquals(double, double, double)} treats them as
     * equal. A {@code tolerance} of {@code 0} is therefore <i>not</i> equivalent to
     * {@link #allOf(double[], double, int)}, which orders by {@link Double#compare(double, double)} and reports the two zeros as
     * different.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] arr = {1.0, 2.1, 3.0, 2.2, 4.0};
     * Index.allOf(arr, 2.0, 0, 0.2).cardinality();   // returns 1  (only 2.1 matches; 2.2 - 2.0 = 0.200...018 > 0.2)
     * Index.allOf(arr, 2.0, 2, 0.2).isEmpty();       // returns true (no match from index 2)
     * Index.allOf(arr, 5.0, 0, 0.1).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the double array to be searched, may be {@code null}
     * @param valueToFind the double value to search for
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @param tolerance the tolerance within which matches will be found; must be non-negative and not NaN
     * @return a BitSet containing the zero-based indices of all values within the specified tolerance at or after {@code fromIndex};
     *         returns an empty BitSet if no values are found within tolerance, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN.
     * @see #allOf(double[], double, int)
     */
    public static BitSet allOf(final double[] source, final double valueToFind, final int fromIndex, final double tolerance) throws IllegalArgumentException {
        N.checkArgNotNegative(tolerance, cs.tolerance);

        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (Numbers.fuzzyEquals(source[i], valueToFind, tolerance)) {
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
     * // the cast is required: a bare null binds to allOf(T[], Predicate), which rejects it
     * BitSet nullIndices = Index.allOf(withNulls, (Object) null);
     * // nullIndices contains {1, 3}
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @return a BitSet containing the zero-based indices of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the array is {@code null} or empty
     * @see #allOf(Object[], Object, int)
     * @see #of(Object[], Object)
     */
    public static BitSet allOf(final Object[] source, final Object valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given array, starting from the specified index.
     * <p>
     * Elements are compared using {@link N#equals(Object, Object)}, which handles {@code null} values correctly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "b", "a"};
     * Index.allOf(arr, "b", 0).cardinality();   // returns 2
     * Index.allOf(arr, "b", 2).toString();      // returns "{3}"
     * Index.allOf(arr, "x", 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the array to be searched, may be {@code null}
     * @param valueToFind the value to find in the array, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the array is {@code null} or empty, or {@code fromIndex >= array.length}
     * @see #allOf(Object[], Object)
     */
    public static BitSet allOf(final Object[] source, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int i = N.max(fromIndex, 0); i < len; i++) {
            if (N.equals(source[i], valueToFind)) {
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
     * int first = indices.stream().findFirst().getAsInt();   // returns 0
     * int last = indices.stream().max().orElse(-1);          // returns 4
     *
     * // Check specific index
     * boolean foundAt2 = indices.get(2);                     // returns true
     * }</pre>
     *
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @return a BitSet containing the zero-based indices (in iteration order) of all occurrences of the value;
     *         returns an empty BitSet if the value is not found or the collection is {@code null} or empty
     * @see #allOf(Collection, Object, int)
     * @see #of(Collection, Object)
     */
    public static BitSet allOf(final Collection<?> source, final Object valueToFind) {
        return allOf(source, valueToFind, 0);
    }

    /**
     * Returns the indices of all occurrences of the specified value in the given collection, starting from the specified index.
     * <p>
     * Elements are compared (in iteration order) using {@link N#equals(Object, Object)}, which handles
     * {@code null} values correctly. The implementation is optimized for {@link RandomAccess} lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "b");
     * Index.allOf(list, "b", 0).cardinality();   // returns 2
     * Index.allOf(list, "b", 2).toString();      // returns "{3}"
     * Index.allOf(list, "x", 0).isEmpty();       // returns true
     * }</pre>
     *
     * @param source the collection to be searched, may be {@code null}
     * @param valueToFind the value to find in the collection, may be {@code null}
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices (in iteration order) of all occurrences of the value at or after {@code fromIndex};
     *         returns an empty BitSet if the value is not found, the collection is {@code null} or empty, or {@code fromIndex >= collection.size()}
     * @see #allOf(Collection, Object)
     */
    public static BitSet allOf(final Collection<?> source, final Object valueToFind, final int fromIndex) {
        final BitSet bitSet = new BitSet();
        final int size = N.size(source);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (source instanceof List<?> list && source instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (N.equals(list.get(idx), valueToFind)) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<?> iter = source.iterator();
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
     * If {@code source} is {@code null} or empty, returns an empty BitSet without invoking the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"apple", "banana", "avocado", "cherry"};
     * BitSet indices = Index.allOf(arr, s -> s.startsWith("a"));
     * // indices contains {0, 2} (positions of "apple" and "avocado")
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param source the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements
     * @return a BitSet containing the zero-based indices of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the array is {@code null} or empty
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #allOf(Object[], Predicate, int)
     */
    public static <T> BitSet allOf(final T[] source, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        return allOf(source, predicate, 0);
    }

    /**
     * Returns the indices of all elements in the given array that match the provided predicate, starting from the specified index.
     * <p>
     * This method tests each element in the array (starting from {@code fromIndex}) against the predicate
     * and returns a BitSet containing the indices of all elements for which the predicate returns {@code true}.
     * Negative {@code fromIndex} values are treated as 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] arr = {"apple", "banana", "avocado", "cherry", "apricot"};
     * Index.allOf(arr, s -> s.startsWith("a"), 1).toString();   // returns "{2, 4}"
     * Index.allOf(arr, s -> s.startsWith("a"), 0).toString();   // returns "{0, 2, 4}"
     * Index.allOf(arr, s -> s.startsWith("z"), 0).isEmpty();    // returns true
     * Index.allOf(arr, s -> s.startsWith("a"), 10).isEmpty();   // returns true (fromIndex past end)
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param source the array to be searched, may be {@code null}
     * @param predicate the predicate to test elements
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices of all elements at or after {@code fromIndex} matching the predicate;
     *         returns an empty BitSet if no elements match, the array is {@code null}, or {@code fromIndex >= array.length}
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #allOf(Object[], Predicate)
     */
    public static <T> BitSet allOf(final T[] source, final Predicate<? super T> predicate, final int fromIndex) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        final BitSet bitSet = new BitSet();
        final int len = N.len(source);

        if (len == 0 || fromIndex >= len) {
            return bitSet;
        }

        for (int idx = N.max(fromIndex, 0); idx < len; idx++) {
            if (predicate.test(source[idx])) {
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
     *   <li>If {@code source} is {@code null}, returns empty BitSet</li>
     *   <li>If {@code predicate} is {@code null}, throws {@code IllegalArgumentException}</li>
     *   <li>Null elements in the collection are passed to the predicate</li>
     * </ul>
     *
     * <p><b>Common Mistakes:</b></p>
     * <pre>{@code
     * // DON'T: Pass null predicate
     * Index.allOf(collection, null);   // throws IllegalArgumentException!
     *
     * // DO: Provide valid predicate
     * Index.allOf(collection, Objects::nonNull);
     *
     * // DON'T: Assume predicate won't receive nulls
     * Index.allOf(Arrays.asList(1, null, 3), x -> x > 0);   // throws NullPointerException inside the predicate!
     *
     * // DO: Handle nulls in predicate
     * Index.allOf(Arrays.asList(1, null, 3), x -> x != null && x > 0);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param source the collection to be searched, may be {@code null}
     * @param predicate the predicate to test elements
     * @return a BitSet containing the zero-based indices (in iteration order) of all elements matching the predicate;
     *         returns an empty BitSet if no elements match or the collection is {@code null} or empty
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #allOf(Collection, Predicate, int)
     */
    public static <T> BitSet allOf(final Collection<? extends T> source, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        return allOf(source, predicate, 0);
    }

    /**
     * Returns the indices of all elements in the given collection that match the provided predicate, starting from the specified index.
     * <p>
     * This method tests each element in the collection (in iteration order, starting from {@code fromIndex})
     * against the predicate and returns a BitSet containing the indices of all elements for which the predicate
     * returns {@code true}. Negative {@code fromIndex} values are treated as 0. The implementation is optimized
     * for {@link RandomAccess} lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
     * Index.allOf(list, s -> s.startsWith("a"), 1).toString();   // returns "{2, 4}"
     * Index.allOf(list, s -> s.startsWith("a"), 0).toString();   // returns "{0, 2, 4}"
     * Index.allOf(list, s -> s.startsWith("z"), 0).isEmpty();    // returns true
     * Index.allOf(list, s -> s.startsWith("a"), 10).isEmpty();   // returns true (fromIndex past end)
     * }</pre>
     *
     * @param <T> the type of the elements in the collection
     * @param source the collection to be searched, may be {@code null}
     * @param predicate the predicate to test elements
     * @param fromIndex the index to start the search from (inclusive); negative values are treated as 0
     * @return a BitSet containing the zero-based indices (in iteration order) of all elements at or after {@code fromIndex} matching the predicate;
     *         returns an empty BitSet if no elements match, the collection is {@code null} or empty, or {@code fromIndex >= collection.size()}
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #allOf(Collection, Predicate)
     */
    public static <T> BitSet allOf(final Collection<? extends T> source, final Predicate<? super T> predicate, final int fromIndex)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        final BitSet bitSet = new BitSet();
        final int size = N.size(source);

        if (size == 0 || fromIndex >= size) {
            return bitSet;
        }

        if (source instanceof List<? extends T> list && source instanceof RandomAccess) {

            for (int idx = N.max(fromIndex, 0); idx < size; idx++) {
                if (predicate.test(list.get(idx))) {
                    bitSet.set(idx);
                }
            }
        } else {
            final Iterator<? extends T> iter = source.iterator();
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
