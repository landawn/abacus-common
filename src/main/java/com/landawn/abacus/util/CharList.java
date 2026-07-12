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

import java.io.Serial;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.stream.CharStream;

/**
 * A high-performance, resizable array implementation for primitive char values that provides
 * specialized operations optimized for character data types. This class extends {@link PrimitiveList}
 * to offer memory-efficient storage and operations that avoid the boxing overhead associated with
 * {@code List<Character>}, making it ideal for applications requiring intensive character array
 * manipulation with optimal performance characteristics.
 *
 * <p>CharList is specifically designed for scenarios involving large collections of character
 * values such as text processing, string manipulation, character encoding/decoding, lexical
 * analysis, document processing, and performance-critical applications requiring efficient
 * character storage. The implementation uses a compact char array as the underlying storage
 * mechanism, providing direct primitive access without wrapper object allocation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct char primitive storage without Character wrapper allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact char array storage with minimal memory overhead</li>
 *   <li><b>Unicode Support:</b> Full support for 16-bit Unicode characters (BMP)</li>
 *   <li><b>High Performance:</b> Optimized algorithms for character-specific operations</li>
 *   <li><b>Rich Text API:</b> String conversion, character search, pattern matching</li>
 *   <li><b>Set Operations:</b> Efficient intersection, union, and difference operations</li>
 *   <li><b>Range Generation:</b> Built-in support for character sequences and alphabets</li>
 *   <li><b>Random Access:</b> O(1) element access and modification by index</li>
 *   <li><b>Dynamic Sizing:</b> Automatic capacity management with intelligent growth</li>
 *   <li><b>String Integration:</b> Seamless conversion to/from String and StringBuilder</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Text Processing:</b> Document parsing, text analysis, natural language processing</li>
 *   <li><b>String Building:</b> Efficient string construction, character buffer management</li>
 *   <li><b>Lexical Analysis:</b> Tokenizers, parsers, compilers, syntax highlighting</li>
 *   <li><b>Data Encoding:</b> Base64 encoding, URL encoding, character set conversion</li>
 *   <li><b>Template Engines:</b> Dynamic text generation, template processing</li>
 *   <li><b>Configuration Parsing:</b> INI files, CSV processing, configuration readers</li>
 *   <li><b>Protocol Processing:</b> Network protocols, message parsing, data serialization</li>
 *   <li><b>Search Algorithms:</b> Text search, pattern matching, string algorithms</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and initializing character lists
 * CharList buffer = CharList.of('H', 'e', 'l', 'l', 'o');
 * CharList alphabet = CharList.rangeClosed('a', 'z');   // returns ['a', 'b', 'c', ..., 'z']
 * CharList digits = CharList.rangeClosed('0', '9');     // returns ['0', '1', '2', ..., '9']
 * CharList textBuffer = new CharList(1000);
 *
 * // String integration
 * CharList fromString = CharList.of("Hello World".toCharArray());   // Convert from string
 * String result = buffer.toString();                                // Bracketed form: "[H, e, l, l, o]"
 * char[] charArray = buffer.toArray();                              // Convert to char array: ['H', 'e', 'l', 'l', 'o']
 *
 * // Text manipulation operations
 * buffer.add(' ');                        // Append space
 * buffer.addAll("World".toCharArray());   // Append more characters
 * char firstChar = buffer.get(0);         // Access by index: 'H'
 * buffer.set(0, 'h');                     // Modify: "hello World"
 *
 * // Character searching and analysis
 * int spaceIndex = buffer.indexOf(' ');   // Find space character
 * boolean hasVowels = buffer.stream().anyMatch(c -> "aeiou".indexOf(c) >= 0);
 * long letterCount = buffer.stream().filter(Character::isLetter).count();
 *
 * // Case conversion and transformations
 * CharList uppercase = buffer.stream()
 *     .map(Character::toUpperCase)
 *     .collect(CharList::new, CharList::add, CharList::addAll);
 *
 * // Set operations for character analysis
 * CharList vowels = CharList.of('a', 'e', 'i', 'o', 'u');
 * CharList consonants = alphabet.difference(vowels);   // Remove vowels
 * CharList common = buffer.intersection(vowels);       // Find vowels in text
 *
 * // High-performance sorting and searching
 * buffer.sort();                          // Sort characters
 * int index = buffer.binarySearch('e');   // Fast character lookup
 *
 * // Efficient text building
 * CharList builder = new CharList();
 * builder.addAll("The quick ".toCharArray());
 * builder.addAll("brown fox ".toCharArray());
 * builder.addAll("jumps over".toCharArray());
 * String sentence = builder.toString();   // Bracketed form: "[T, h, e,  , q, u, i, c, k, ...]"
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Element Access:</b> O(1) for get/set operations by index</li>
 *   <li><b>Insertion:</b> O(1) amortized for append, O(n) for middle insertion</li>
 *   <li><b>Deletion:</b> O(1) for last element, O(n) for arbitrary position</li>
 *   <li><b>Search:</b> O(n) for contains/indexOf, O(log n) for binary search on sorted data</li>
 *   <li><b>Sorting:</b> O(n log n) using optimized primitive sorting algorithms</li>
 *   <li><b>String Conversion:</b> O(n) with efficient array copying</li>
 *   <li><b>Set Operations:</b> O(n) to O(n²) depending on algorithm selection and data size</li>
 * </ul>
 *
 * <p><b>Memory Efficiency:</b>
 * <ul>
 *   <li><b>Storage:</b> 2 bytes per element (16 bits) with no object overhead</li>
 *   <li><b>vs List&lt;Character&gt;:</b> ~8x less memory usage (no Character wrapper objects)</li>
 *   <li><b>vs String:</b> Mutable with similar memory footprint when capacity matches size</li>
 *   <li><b>vs StringBuilder:</b> Comparable memory usage with primitive-specific operations</li>
 *   <li><b>Capacity Management:</b> 1.75x growth factor balances memory and performance</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} (typically Integer.MAX_VALUE - 8)</li>
 * </ul>
 *
 * <p><b>Character-Specific Operations:</b>
 * <ul>
 *   <li><b>Range Generation:</b> {@code range()}, {@code rangeClosed()} for character sequences</li>
 *   <li><b>String Integration:</b> {@code toString()} for string conversion</li>
 *   <li><b>Character Analysis:</b> Integration with Character utility methods</li>
 *   <li><b>Case Operations:</b> Efficient case conversion via stream transformations</li>
 *   <li><b>Pattern Matching:</b> Character-level pattern searching and matching</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li><b>{@code of(char...)}:</b> Create from varargs array</li>
 *   <li><b>{@code copyOf(char[])}:</b> Create defensive copy of array</li>
 *   <li><b>{@code range(char, char)}:</b> Create character sequence [start, end)</li>
 *   <li><b>{@code rangeClosed(char, char)}:</b> Create character sequence [start, end]</li>
 *   <li><b>{@code repeat(char, int)}:</b> Create with repeated characters</li>
 *   <li><b>{@code random(int)}:</b> Create with random characters</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toArray()}:</b> Convert to primitive char array</li>
 *   <li><b>{@code toString()}:</b> Convert to String</li>
 *   <li><b>{@code boxed()}:</b> Convert to {@code List<Character>}</li>
 *   <li><b>{@code stream()}:</b> Convert to CharStream for functional processing</li>
 * </ul>
 *
 * <p><b>Deque-like Operations:</b>
 * <ul>
 *   <li><b>{@code addFirst(char)}:</b> Insert at beginning (O(n) operation)</li>
 *   <li><b>{@code addLast(char)}:</b> Insert at end (O(1) amortized)</li>
 *   <li><b>{@code removeFirst()}:</b> Remove from beginning (O(n) operation)</li>
 *   <li><b>{@code removeLast()}:</b> Remove from end (O(1) operation)</li>
 *   <li><b>{@code getFirst()}:</b> Access first character (O(1) operation)</li>
 *   <li><b>{@code getLast()}:</b> Access last character (O(1) operation)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Not Thread-Safe:</b> This implementation is not synchronized</li>
 *   <li><b>External Synchronization:</b> Required for concurrent access</li>
 *   <li><b>Iterators:</b> Not fail-fast; concurrent modification yields undefined results</li>
 *   <li><b>Read-Only Access:</b> Multiple threads can safely read simultaneously</li>
 * </ul>
 *
 * <p><b>Capacity Management:</b>
 * <ul>
 *   <li><b>Initial Capacity:</b> Default capacity of 10 elements</li>
 *   <li><b>Growth Strategy:</b> 1.75x expansion when capacity exceeded</li>
 *   <li><b>Manual Control:</b> specify the initial capacity via the {@code CharList(int)} constructor</li>
 *   <li><b>Trimming:</b> {@code trimToSize()} to reduce memory footprint</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IndexOutOfBoundsException:</b> For invalid index access</li>
 *   <li><b>NoSuchElementException:</b> For operations on empty lists</li>
 *   <li><b>IllegalArgumentException:</b> For invalid method parameters</li>
 *   <li><b>OutOfMemoryError:</b> When capacity exceeds available memory</li>
 * </ul>
 *
 * <p><b>Serialization Support:</b>
 * <ul>
 *   <li><b>Serializable:</b> Implements {@link java.io.Serializable}</li>
 *   <li><b>Version Compatibility:</b> Stable serialVersionUID for version compatibility</li>
 *   <li><b>Efficient Format:</b> Optimized serialization of char arrays</li>
 *   <li><b>Cross-Platform:</b> Platform-independent serialized format</li>
 * </ul>
 *
 * <p><b>Integration with Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Collection Compatibility:</b> Seamless conversion to standard collections</li>
 *   <li><b>Utility Integration:</b> Works with Collections utility methods via boxed()</li>
 *   <li><b>Stream API:</b> Full integration with CharStream for functional processing</li>
 * </ul>
 *
 * <p><b>Text Processing Operations:</b>
 * <ul>
 *   <li><b>Character Counting:</b> {@code frequency()} for frequency analysis</li>
 *   <li><b>Case Analysis:</b> Integration with Character.isUpperCase(), isLowerCase()</li>
 *   <li><b>Character Classification:</b> Letter, digit, whitespace detection via streams</li>
 *   <li><b>Duplicate Detection:</b> {@code containsDuplicates()}, {@code removeDuplicates()}</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs List&lt;Character&gt;:</b> 8x less memory, significantly faster operations</li>
 *   <li><b>vs char[]:</b> Dynamic sizing, rich API, set operations, text functions</li>
 *   <li><b>vs String:</b> Mutable, can be modified in-place, similar memory footprint</li>
 *   <li><b>vs StringBuilder:</b> Primitive operations, set operations, statistical functions</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code CharList} for mutable character sequences requiring set operations</li>
 *   <li>Specify initial capacity for known text sizes to avoid resizing</li>
 *   <li>Use bulk operations ({@code addAll}, {@code removeAll}) instead of loops</li>
 *   <li>Convert to String only when immutable result is needed</li>
 *   <li>Leverage stream API for complex text transformations</li>
 *   <li>Sort character data before using binary search operations</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Pre-size lists with known capacity using the {@code CharList(int)} constructor</li>
 *   <li>Use {@code addLast()} instead of {@code addFirst()} for better performance</li>
 *   <li>Sort data before using {@code binarySearch()} for O(log n) lookups</li>
 *   <li>Use {@code stream()} API for complex character transformations</li>
 *   <li>Consider {@code StringBuilder} for simple string building without set operations</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Text Building:</b> {@code CharList buffer = new CharList(expectedLength);}</li>
 *   <li><b>Character Analysis:</b> {@code CharList chars = CharList.of(text.toCharArray());}</li>
 *   <li><b>Alphabet Generation:</b> {@code CharList letters = CharList.range('a', 'z');}</li>
 *   <li><b>Character Filtering:</b> {@code CharList filtered = chars.stream().filter(...).collect(...);}</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link PrimitiveList}:</b> Abstract base class for all primitive list types</li>
 *   <li><b>{@link String}:</b> Immutable character sequence</li>
 *   <li><b>{@link StringBuilder}:</b> Mutable character sequence builder</li>
 *   <li><b>{@link CharIterator}:</b> Specialized iterator for char primitives</li>
 *   <li><b>{@link CharStream}:</b> Functional processing of character sequences</li>
 * </ul>
 *
 * <p><b>Usage Examples: Text Processing</b>
 * <pre>{@code
 * // Process text document
 * String document = "The Quick Brown Fox Jumps Over The Lazy Dog";
 * CharList text = CharList.of(document.toCharArray());
 *
 * // Character analysis
 * long letterCount = text.stream().filter(Character::isLetter).count();
 * long digitCount = text.stream().filter(Character::isDigit).count();
 * long spaceCount = text.stream().filter(Character::isWhitespace).count();
 *
 * // Case conversion
 * CharList lowercase = text.stream()
 *     .map(Character::toLowerCase)
 *     .collect(CharList::new, CharList::add, CharList::addAll);
 *
 * // Character frequency analysis
 * CharList uniqueChars = text.stream()
 *     .distinct()
 *     .sorted()
 *     .collect(CharList::new, CharList::add, CharList::addAll);
 *
 * // Build modified text
 * CharList result = new CharList();
 * result.addAll("Processed: ".toCharArray());
 * result.addAll(lowercase);
 * result.add('!');
 * String processed = result.toString();
 *
 * // Pattern matching
 * CharList vowels = CharList.of("aeiou".toCharArray());
 * CharList consonants = CharList.rangeClosed('a', 'z').difference(vowels);
 * boolean hasAllVowels = vowels.stream().allMatch(v -> text.contains(v));
 * }</pre>
 *
 * @see PrimitiveList
 * @see CharIterator
 * @see CharStream
 * @see String
 * @see StringBuilder
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see java.util.List
 * @see java.util.RandomAccess
 * @see java.io.Serializable
 */
public final class CharList extends PrimitiveList<Character, char[], CharList> {

    @Serial
    private static final long serialVersionUID = 7293826835233022514L;

    /** Shared random number generator used by {@link #random(int)}. */
    static final Random RAND = new SecureRandom();
    /** The number of distinct char values; used to map a non-negative random int to the full char range. */
    static final int BOUND = Character.MAX_VALUE + 1;

    /**
     * The array buffer into which the elements of the CharList are stored.
     */
    private char[] elementData = N.EMPTY_CHAR_ARRAY;

    /**
     * The size of the CharList (the number of elements it contains).
     */
    private int size = 0;

    /**
     * Constructs an empty CharList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = new CharList();
     * list.size();      // returns 0
     * list.isEmpty();   // returns true
     * list.add('a');    // list is now ['a']
     * }</pre>
     *
     */
    public CharList() {
    }

    /**
     * Constructs an empty CharList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = new CharList(100);   // capacity for 100 chars, but still empty
     * list.size();                         // returns 0
     * new CharList(-1);                    // throws IllegalArgumentException
     * }</pre>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public CharList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_CHAR_ARRAY : new char[initialCapacity];
    }

    /**
     * Constructs a CharList containing the elements of the specified array.
     * The CharList instance uses the specified array as its backing array without copying.
     * Changes to the array will be reflected in the list and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c'};
     * CharList list = new CharList(arr);   // list is ['a', 'b', 'c'], backed by arr
     * arr[0] = 'x';                        // list is now ['x', 'b', 'c'] (shares array)
     * new CharList((char[]) null);         // throws NullPointerException
     * }</pre>
     *
     * @param a the array whose elements are to be used as the backing array for this list; must not be {@code null}
     * @throws NullPointerException if the specified array is {@code null}
     */
    public CharList(final char[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a CharList using the specified array as the element array for this list without copying action.
     * The first {@code size} elements of the array will be used as the initial elements of the list.
     * Changes to the array will be reflected in the list and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c', 'd'};
     * CharList list = new CharList(arr, 2);   // list is ['a', 'b'] (only first 2 used)
     * list.size();                            // returns 2
     * new CharList(arr, 5);                   // throws IndexOutOfBoundsException (5 > arr.length)
     * }</pre>
     *
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is negative or greater than the array length
     */
    public CharList(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new CharList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');           // list is ['a', 'b', 'c']
     * CharList empty = CharList.of();                       // list is [] (empty)
     * CharList fromArr = CharList.of("hi".toCharArray());   // list is ['h', 'i']
     * }</pre>
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new CharList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static CharList of(final char... a) {
        return new CharList(N.nullToEmpty(a));
    }

    /**
     * Creates a new CharList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c'};
     * CharList list = CharList.of(arr, 2);   // list is ['a', 'b']
     * CharList all = CharList.of(arr, 3);    // list is ['a', 'b', 'c']
     * CharList.of(arr, 4);                   // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param a the array of char values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new CharList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static CharList of(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new CharList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new CharList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(char...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c'};
     * CharList list = CharList.copyOf(arr);   // list is ['a', 'b', 'c']
     * arr[0] = 'x';                           // list is still ['a', 'b', 'c'] (defensive copy)
     * CharList empty = CharList.copyOf(null); // list is [] (empty)
     * }</pre>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new CharList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static CharList copyOf(final char[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new CharList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c', 'd'};
     * CharList list = CharList.copyOf(arr, 1, 3);   // list is ['b', 'c']
     * CharList empty = CharList.copyOf(arr, 2, 2);  // list is [] (empty range)
     * CharList.copyOf(arr, 0, 5);                   // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param a the array from which a range is to be copied; must not be {@code null}
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new CharList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static CharList copyOf(final char[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive (inclusive) to endExclusive (exclusive).
     *
     * <p>For example, {@code range('a', 'd')} returns a list containing ['a', 'b', 'c'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.range('a', 'd');   // returns ['a', 'b', 'c']
     * CharList one = CharList.range('a', 'b');    // returns ['a']
     * CharList empty = CharList.range('a', 'a');  // returns [] (empty)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new CharList containing the range of values
     */
    public static CharList range(final char startInclusive, final char endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive (inclusive) to endExclusive (exclusive),
     * incrementing by the specified step.
     *
     * <p>For example, {@code range('a', 'g', 2)} returns a list containing ['a', 'c', 'e'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.range('a', 'g', 2);    // returns ['a', 'c', 'e']
     * CharList one = CharList.range('a', 'c', 5);     // returns ['a']
     * CharList empty = CharList.range('a', 'a', 2);   // returns [] (empty)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new CharList containing the range of values
     * @throws IllegalArgumentException if {@code by} is zero
     */
    public static CharList range(final char startInclusive, final char endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive to endInclusive (both inclusive).
     *
     * <p>For example, {@code rangeClosed('a', 'd')} returns a list containing ['a', 'b', 'c', 'd'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.rangeClosed('a', 'd');   // returns ['a', 'b', 'c', 'd']
     * CharList one = CharList.rangeClosed('a', 'a');    // returns ['a']
     * CharList digits = CharList.rangeClosed('0', '9'); // returns ['0', '1', ..., '9']
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new CharList containing the range of values
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive to endInclusive (both inclusive),
     * incrementing by the specified step.
     *
     * <p>For example, {@code rangeClosed('a', 'g', 2)} returns a list containing ['a', 'c', 'e', 'g'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.rangeClosed('a', 'g', 2);   // returns ['a', 'c', 'e', 'g']
     * CharList one = CharList.rangeClosed('a', 'a', 2);    // returns ['a']
     * CharList two = CharList.rangeClosed('a', 'd', 3);    // returns ['a', 'd']
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new CharList containing the range of values
     * @throws IllegalArgumentException if {@code by} is zero
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a CharList containing the specified element repeated {@code len} times.
     *
     * <p>For example, {@code repeat('a', 3)} returns a list containing ['a', 'a', 'a'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.repeat('a', 3);   // returns ['a', 'a', 'a']
     * CharList empty = CharList.repeat('a', 0);  // returns [] (empty)
     * CharList one = CharList.repeat('z', 1);    // returns ['z']
     * }</pre>
     *
     * @param element the element to repeat
     * @param len the number of times to repeat the element
     * @return a new CharList containing the repeated element
     * @throws IllegalArgumentException if {@code len} is negative
     */
    public static CharList repeat(final char element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a CharList of the specified length filled with random char values.
     * Each character will be randomly generated from the entire range of possible char values (0 to 65535).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.random(5);     // returns a list of 5 random chars, e.g. size() == 5
     * CharList empty = CharList.random(0);    // returns [] (empty)
     * CharList.random(-1);                    // throws NegativeArraySizeException
     * }</pre>
     *
     * @param len the length of the list to create
     * @return a new CharList containing random char values
     * @throws NegativeArraySizeException if {@code len} is negative
     */
    public static CharList random(final int len) {
        final char[] a = new char[len];

        for (int i = 0; i < len; i++) {
            a[i] = (char) RAND.nextInt(BOUND);
        }

        return of(a);
    }

    /**
     * Creates a CharList of the specified length filled with random char values within the specified range.
     * Each character will be randomly generated from startInclusive (inclusive) to endExclusive (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.random('a', 'z', 4);   // returns 4 random chars in ['a', 'z'), e.g. size() == 4
     * CharList empty = CharList.random('a', 'z', 0);  // returns [] (empty)
     * CharList.random('z', 'a', 3);                   // throws IllegalArgumentException (start >= end)
     * }</pre>
     *
     * @param startInclusive the minimum value (inclusive)
     * @param endExclusive the maximum value (exclusive)
     * @param len the length of the list to create
     * @return a new CharList containing random char values within the specified range
     * @throws IllegalArgumentException if {@code startInclusive >= endExclusive}
     * @throws NegativeArraySizeException if {@code len} is negative
     */
    public static CharList random(final char startInclusive, final char endExclusive, final int len) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' (" + startInclusive + ") must be less than 'endExclusive' (" + endExclusive + ")");
        }

        final char[] a = new char[len];
        final int mod = endExclusive - startInclusive;

        for (int i = 0; i < len; i++) {
            a[i] = (char) (RAND.nextInt(mod) + startInclusive);
        }

        return of(a);
    }

    /**
     * Creates a CharList of the specified length by randomly selecting from the provided candidate chars.
     * Each element in the returned list is randomly chosen from the candidates array with uniform distribution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] candidates = {'a', 'b', 'c'};
     * CharList list = CharList.random(candidates, 5);   // returns 5 chars, each one of 'a'/'b'/'c'
     * CharList empty = CharList.random(candidates, 0);  // returns [] (empty)
     * CharList.random(new char[0], 3);                  // throws IllegalArgumentException (empty candidates)
     * }</pre>
     *
     * @param candidates the array of candidate chars to choose from; must not be {@code null}, empty,
     *                   or of length {@code Integer.MAX_VALUE}
     * @param len the length of the list to create
     * @return a new CharList containing randomly selected chars from the candidates
     * @throws IllegalArgumentException if {@code candidates} is {@code null}, empty,
     *         or has exactly {@code Integer.MAX_VALUE} elements
     * @throws NegativeArraySizeException if {@code len} is negative
     */
    public static CharList random(final char[] candidates, final int len) {
        if (N.isEmpty(candidates) || candidates.length == Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        } else if (candidates.length == 1) {
            return repeat(candidates[0], len);
        }

        final int n = candidates.length;
        final char[] a = new char[len];

        for (int i = 0; i < len; i++) {
            a[i] = candidates[RAND.nextInt(n)];
        }

        return of(a);
    }

    /**
     * Returns the underlying char array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.</p>
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal char array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public char[] internalArray() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char c = list.get(0);   // returns 'a'
     * char d = list.get(2);   // returns 'c'
     * list.get(3);            // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public char get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char old = list.set(1, 'x');   // returns 'b', list is now ['a', 'x', 'c']
     * list.set(3, 'z');              // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param index the index of the element to replace
     * @param e the element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public char set(final int index, final char e) {
        rangeCheck(index);

        final char oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b');
     * list.add('c');   // list is now ['a', 'b', 'c']
     * list.add('d');   // list is now ['a', 'b', 'c', 'd']
     * }</pre>
     *
     * @param e the element to be appended to this list
     */
    public void add(final char e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'c');
     * list.add(1, 'b');   // list is now ['a', 'b', 'c']
     * list.add(0, 'x');   // list is now ['x', 'a', 'b', 'c']
     * list.add(10, 'z');  // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final char e) {
        rangeCheckForAdd(index);

        ensureCapacity(size + 1);

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + 1, numMoved);
        }

        elementData[index] = e;

        size++;
    }

    /**
     * Appends all of the elements in the specified CharList to the end of this list,
     * in the order they appear in the specified list.
     *
     * @param c the CharList containing elements to be added to this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if {@code c} was not empty)
     */
    @Override
    public boolean addAll(final CharList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew);

        N.copy(c.internalArray(), 0, elementData, size, numNew);

        size += numNew;

        return true;
    }

    /**
     * Inserts all of the elements in the specified CharList into this list,
     * starting at the specified position. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (increases their indices).
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the CharList containing elements to be inserted into this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if {@code c} was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final CharList c) {
        rangeCheckForAdd(index);

        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew);

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(c.internalArray(), 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    /**
     * Appends all of the elements in the specified array to the end of this list,
     * in the order they appear in the array.
     *
     * @param a the array containing elements to be added to this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty)
     */
    @Override
    public boolean addAll(final char[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all of the elements in the specified array into this list,
     * starting at the specified position. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (increases their indices).
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be inserted into this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final char[] a) {
        rangeCheckForAdd(index);

        if (N.isEmpty(a)) {
            return false;
        }

        final int numNew = a.length;

        ensureCapacity(size + numNew);

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(a, 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If the list does not contain the element, it is unchanged.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list.</p>
     * <p><b>Note:</b> This method removes by value. To remove by index, use {@link #removeAt(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c');
     * boolean removed = list.remove('a');     // returns true, list is now ['b', 'a', 'c']
     * boolean notFound = list.remove('z');    // returns false, list unchanged
     * }</pre>
     *
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final char e) {
        for (int i = 0; i < size; i++) {
            if (elementData[i] == e) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list.
     * The list is compacted after removal, maintaining the order of remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
     * boolean removed = list.removeAllOccurrences('a');   // returns true, list is now ['b', 'c']
     * boolean notFound = list.removeAllOccurrences('z');  // returns false, list unchanged
     * }</pre>
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list was modified (at least one element was removed)
     */
    public boolean removeAllOccurrences(final char e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Private method to remove the element at the specified position without range checking.
     *
     * @param index the index of the element to remove
     */
    private void fastRemove(final int index) {
        final int numMoved = size - index - 1;

        if (numMoved > 0) {
            N.copy(elementData, index + 1, elementData, index, numMoved);
        }

        elementData[--size] = 0; // clear to let GC do its work
    }

    /**
     * Removes from this list all of its elements that are contained in the specified CharList.
     * This method compares elements by value, removing all occurrences found in the specified list.
     *
     * @param c the CharList containing elements to be removed from this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final CharList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * This method compares elements by value, removing all occurrences found in the array.
     *
     * @param a the array containing elements to be removed from this list. If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final char[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements of this list that satisfy the given predicate.
     * Errors or runtime exceptions thrown during iteration or by the predicate
     * are relayed to the caller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * boolean removed = list.removeIf(c -> c > 'b');   // returns true, list is now ['a', 'b']
     * boolean noChange = list.removeIf(c -> c > 'z');  // returns false, list unchanged
     * }</pre>
     *
     * @param p a predicate which returns {@code true} for elements to be removed; must not be {@code null}
     * @return {@code true} if any elements were removed; {@code false} if the list was unchanged
     * @throws NullPointerException if {@code p} is {@code null}
     */
    public boolean removeIf(final CharPredicate p) {
        N.requireNonNull(p, cs.predicate);

        final CharList tmp = new CharList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (char) 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes all duplicate elements from this list, keeping only the first occurrence of each element.
     * The order of elements is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c', 'b');
     * boolean removed = list.removeDuplicates();                          // returns true, list is now ['a', 'b', 'c']
     * boolean noChange = CharList.of('a', 'b', 'c').removeDuplicates();   // returns false
     * }</pre>
     *
     * @return {@code true} if any duplicates were removed
     */
    @Override
    public boolean removeDuplicates() {
        if (size < 2) {
            return false;
        }

        final boolean isSorted = isSorted();
        int idx = 0;

        if (isSorted) {
            for (int i = 1; i < size; i++) {
                if (elementData[i] != elementData[idx]) {
                    elementData[++idx] = elementData[i];
                }
            }

        } else {
            final Set<Character> set = N.newLinkedHashSet(size);
            set.add(elementData[0]);

            for (int i = 1; i < size; i++) {
                if (set.add(elementData[i])) {
                    elementData[++idx] = elementData[i];
                }
            }
        }

        if (idx == size - 1) {
            return false;
        } else {
            N.fill(elementData, idx + 1, size, (char) 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified CharList.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified CharList.
     *
     * <p>If the specified list is {@code null} or empty, all elements are removed from this list.</p>
     *
     * @param c the CharList containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final CharList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified array.
     *
     * <p>If the specified array is {@code null} or empty, all elements are removed from this list.</p>
     *
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final char[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(CharList.of(a));
    }

    /**
     * Removes or retains elements based on whether they are contained in the specified CharList.
     *
     * @param c the CharList to check against
     * @param complement if {@code false}, removes elements in c; if {@code true}, retains only elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final CharList c, final boolean complement) {
        final char[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Character> set = c.toSet();

            for (int i = 0; i < size; i++) {
                if (set.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (c.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved;
    }

    //    /**
    //     * Removes the element at the specified position in this list.
    //     * Shifts any subsequent elements to the left (subtracts one from their indices).
    //     *
    //     * @param index the index of the element to be removed
    //     * @return the element that was removed from the list
    //     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
    //     * @deprecated replaced by {@link #removeAt(int)}.
    //     */
    //    @Deprecated
    //    public char delete(final int index) {
    //        return removeAt(index);
    //    }

    /**
     * Removes and returns the element at the specified index.
     *
     * <p>This is the preferred index-based removal method.
     * Unlike {@link #remove(char)}, this method removes by index, not by value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char removed = list.removeAt(1);   // returns 'b', list is now ['a', 'c']
     * list.removeAt(5);                  // throws IndexOutOfBoundsException
     * }</pre>
     *
     * <p><b>Note:</b> this single-index form returns the removed {@code char} value; the varargs
     * {@link #removeAt(int...)} form removes several elements in place and returns {@code void}.</p>
     *
     * @param index the index of the element to remove
     * @return the removed element
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
     * @see #removeAt(int...)
     */
    public char removeAt(final int index) {
        rangeCheck(index);

        final char oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list.
     * The indices array will be sorted internally, and duplicates will be removed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
     * list.removeAt(0, 2, 4);   // list is now ['b', 'd']
     * list.removeAt();          // list unchanged (no indices)
     * }</pre>
     *
     * <p><b>Note:</b> this varargs form removes several elements in place and returns {@code void}; the
     * single-index {@link #removeAt(int)} form returns the removed {@code char} value.</p>
     *
     * @param indices the indices of elements to be removed. If {@code null} or empty, this list remains unchanged.
     * @throws IndexOutOfBoundsException if any index is out of range ({@code index < 0 || index >= size()})
     * @see #removeAt(int)
     */
    @Override
    public void removeAt(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        for (final int index : indices) {
            N.checkElementIndex(index, size);
        }

        final char[] tmp = N.removeAt(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (char) 0);
        }

        // size = tmp.length; // incorrect. the array returned N.removeAt(elementData, indices) contains empty elements after size.
        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
     * list.removeRange(1, 3);   // list is now ['a', 'd', 'e']
     * list.removeRange(0, 0);   // list unchanged (empty range)
     * }</pre>
     *
     * @param fromIndex the index of the first element to be removed
     * @param toIndex the index after the last element to be removed
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    @Override
    public void removeRange(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (fromIndex == toIndex) {
            return;
        }

        final int size = size();//NOSONAR
        final int newSize = size - (toIndex - fromIndex);

        if (toIndex < size) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size, (char) 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
     * list.moveRange(0, 2, 3);   // moves ['a','b'] to start at index 3; list is now ['c', 'd', 'e', 'a', 'b']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionAfterMove the zero-based index where the first element of the range will be placed after the move;
     *        must be between {@code 0} and {@code size() - (toIndex - fromIndex)}, inclusive
     * @throws IndexOutOfBoundsException if any index is out of bounds or if
     *         {@code newPositionAfterMove} would cause elements to be moved outside the list
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionAfterMove) {
        N.checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionAfterMove, size);
        N.moveRange(elementData, fromIndex, toIndex, newPositionAfterMove);
    }

    /**
     * Replaces each element in the specified range of this list with elements
     * from the replacement CharList. The range extends from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive). If the replacement has a different size than the
     * range being replaced, the list will grow or shrink accordingly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.replaceRange(1, 3, CharList.of('x', 'y', 'z'));   // list is now ['a', 'x', 'y', 'z', 'd']
     *
     * CharList list2 = CharList.of('a', 'b', 'c', 'd');
     * list2.replaceRange(1, 3, new CharList());              // empty replacement removes range; list2 is now ['a', 'd']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to replace
     * @param toIndex the ending index (exclusive) of the range to replace
     * @param replacement the CharList whose elements will replace the specified range. If {@code null} or empty,
     *        the range is simply removed (no elements are inserted)
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     * @throws OutOfMemoryError if the resulting size would exceed the maximum supported array size
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final CharList replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            removeRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        // Use long arithmetic to prevent integer overflow
        final long newSizeLong = (long) size - (long) (toIndex - fromIndex) + replacement.size();

        if (newSizeLong < 0 || newSizeLong > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        final int newSize = (int) newSizeLong;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.size() && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.size(), size - toIndex);
        }

        N.copy(replacement.elementData, 0, elementData, fromIndex, replacement.size());

        if (newSize < size) {
            N.fill(elementData, newSize, size, (char) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces each element in the specified range of this list with elements
     * from the replacement array. The range extends from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive). If the replacement has a different length than the
     * range being replaced, the list will grow or shrink accordingly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.replaceRange(1, 3, new char[] {'x'});   // shorter replacement shrinks list; list is now ['a', 'x', 'd']
     *
     * CharList list2 = CharList.of('a', 'b', 'c', 'd');
     * list2.replaceRange(1, 3, new char[0]);        // empty replacement removes range; list2 is now ['a', 'd']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to replace
     * @param toIndex the ending index (exclusive) of the range to replace
     * @param replacement the array whose elements will replace the specified range. If {@code null} or empty,
     *        the range is simply removed (no elements are inserted)
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     * @throws OutOfMemoryError if the resulting size would exceed the maximum supported array size
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final char[] replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            removeRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        // Use long arithmetic to prevent integer overflow
        final long newSizeLong = (long) size - (long) (toIndex - fromIndex) + replacement.length;

        if (newSizeLong < 0 || newSizeLong > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        final int newSize = (int) newSizeLong;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.length && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.length, size - toIndex);
        }

        N.copy(replacement, 0, elementData, fromIndex, replacement.length);

        if (newSize < size) {
            N.fill(elementData, newSize, size, (char) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value with the new value in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c');
     * int count = list.replaceAll('a', 'x');    // returns 2, list is now ['x', 'b', 'x', 'c']
     * int none = list.replaceAll('z', 'y');     // returns 0, list unchanged
     * }</pre>
     *
     * @param oldVal the old value to be replaced
     * @param newVal the new value to replace the old value
     * @return the number of elements replaced
     */
    public int replaceAll(final char oldVal, final char newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (elementData[i] == oldVal) {
                elementData[i] = newVal;

                result++;
            }
        }

        return result;
    }

    /**
     * Replaces each element of this list with the result of applying the operator to that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * list.replaceAll(c -> Character.toUpperCase(c));   // list is now ['A', 'B', 'C']
     * new CharList().replaceAll(c -> c);                // empty list unchanged
     * }</pre>
     *
     * @param operator the operator to apply to each element; must not be {@code null}
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public void replaceAll(final CharUnaryOperator operator) {
        N.requireNonNull(operator, "operator");

        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsChar(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * boolean replaced = list.replaceIf(c -> c > 'b', 'z');   // returns true, list is now ['a', 'b', 'z', 'z']
     * boolean noChange = list.replaceIf(c -> c > 'z', 'x');   // returns false, list unchanged
     * }</pre>
     *
     * @param predicate the predicate to test elements; must not be {@code null}
     * @param newValue the value to replace matching elements with
     * @return {@code true} if any elements were replaced
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public boolean replaceIf(final CharPredicate predicate, final char newValue) {
        N.requireNonNull(predicate, cs.predicate);

        boolean result = false;

        for (int i = 0, len = size(); i < len; i++) {
            if (predicate.test(elementData[i])) {
                elementData[i] = newValue;

                result = true;
            }
        }

        return result;
    }

    /**
     * Replaces all elements in this list with the specified value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * list.fill('x');             // list is now ['x', 'x', 'x']
     * new CharList().fill('x');   // empty list unchanged
     * }</pre>
     *
     * @param val the value to be stored in all elements of the list
     */
    public void fill(final char val) {
        fill(0, size(), val);
    }

    /**
     * Replaces the elements in the specified range of this list with the specified value.
     * The range extends from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.fill(1, 3, 'x');     // list is now ['a', 'x', 'x', 'd']
     * list.fill(0, 5, 'z');     // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value
     * @param val the value to be stored in the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public void fill(final int fromIndex, final int toIndex, final char val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code e == valueToFind}.
     *
     * <p>This method performs a linear search through the list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * boolean has = list.contains('b');      // returns true
     * boolean missing = list.contains('z');  // returns false
     * }</pre>
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element, {@code false} otherwise
     */
    public boolean contains(final char valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any element from the specified CharList.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * boolean any = list.containsAny(CharList.of('c', 'z'));     // returns true ('c' is shared)
     * boolean none = list.containsAny(CharList.of('x', 'z'));    // returns false
     * }</pre>
     *
     * @param c the CharList to check for common elements
     * @return {@code true} if this list contains at least one element from the specified CharList
     */
    @Override
    public boolean containsAny(final CharList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any element from the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * boolean any = list.containsAny(new char[] {'c', 'z'});     // returns true ('c' is shared)
     * boolean none = list.containsAny(new char[] {'x', 'z'});    // returns false
     * }</pre>
     *
     * @param a the array to check for common elements
     * @return {@code true} if this list contains at least one element from the specified array
     */
    @Override
    public boolean containsAny(final char[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified CharList.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * boolean all = list.containsAll(CharList.of('a', 'b'));     // returns true
     * boolean notAll = list.containsAll(CharList.of('a', 'z'));  // returns false ('z' missing)
     * boolean empty = list.containsAll(new CharList());          // returns true (empty is always contained)
     * }</pre>
     *
     * @param c the CharList to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified CharList
     */
    @Override
    public boolean containsAll(final CharList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Character> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (!set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (!contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * boolean all = list.containsAll(new char[] {'a', 'b'});     // returns true
     * boolean notAll = list.containsAll(new char[] {'a', 'z'});  // returns false ('z' missing)
     * boolean empty = list.containsAll(new char[0]);             // returns true (empty is always contained)
     * }</pre>
     *
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified array
     */
    @Override
    public boolean containsAll(final char[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list has no elements in common with the specified CharList.
     * Two lists are disjoint if they have no elements in common.
     *
     * @param c the CharList to check for common elements
     * @return {@code true} if the two lists have no elements in common
     */
    @Override
    public boolean disjoint(final CharList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Character> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if this list has no elements in common with the specified array.
     *
     * @param b the array to check for common elements
     * @return {@code true} if this list and the array have no elements in common
     */
    @Override
    public boolean disjoint(final char[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * CharList list2 = CharList.of('a', 'b', 'b', 'd');
     * CharList result = list1.intersection(list2);   // result will be ['a', 'b']
     * // One occurrence of 'a' (minimum count in both lists) and one occurrence of 'b'
     *
     * CharList list3 = CharList.of('x', 'x', 'y');
     * CharList list4 = CharList.of('x', 'z');
     * CharList result2 = list3.intersection(list4);   // result will be ['x']
     * // One occurrence of 'x' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list
     * @return a new CharList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(char[])
     * @see #difference(CharList)
     * @see #symmetricDifference(CharList)
     * @see N#intersection(char[], char[])
     */
    @Override
    public CharList intersection(final CharList b) {
        if (N.isEmpty(b)) {
            return new CharList();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * char[] array = new char[] {'a', 'b', 'b', 'd'};
     * CharList result = list1.intersection(array);   // result will be ['a', 'b']
     * // One occurrence of 'a' (minimum count in both sources) and one occurrence of 'b'
     *
     * CharList list2 = CharList.of('x', 'x', 'y');
     * char[] array2 = new char[] {'x', 'z'};
     * CharList result2 = list2.intersection(array2);   // result will be ['x']
     * // One occurrence of 'x' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list
     * @return a new CharList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(CharList)
     * @see #difference(char[])
     * @see #symmetricDifference(char[])
     * @see N#intersection(char[], char[])
     */
    @Override
    public CharList intersection(final char[] b) {
        if (N.isEmpty(b)) {
            return new CharList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * CharList list2 = CharList.of('a', 'd');
     * CharList result = list1.difference(list2);   // result will be ['a', 'b', 'c']
     * // One 'a' remains because list1 has two occurrences and list2 has one
     *
     * CharList list3 = CharList.of('e', 'f');
     * CharList list4 = CharList.of('e', 'e', 'f');
     * CharList result2 = list3.difference(list4);   // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list
     * @return a new CharList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(char[])
     * @see #symmetricDifference(CharList)
     * @see #intersection(CharList)
     * @see N#difference(char[], char[])
     */
    @Override
    public CharList difference(final CharList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list with the elements in this list but not in the specified array {@code b},
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * char[] array = new char[] {'a', 'd'};
     * CharList result = list1.difference(array);   // result will be ['a', 'b', 'c']
     * // One 'a' remains because list1 has two occurrences and array has one
     *
     * CharList list2 = CharList.of('e', 'f');
     * char[] array2 = new char[] {'e', 'e', 'f'};
     * CharList result2 = list2.difference(array2);   // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param b the array to compare against this list
     * @return a new CharList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(CharList)
     * @see #symmetricDifference(char[])
     * @see #intersection(char[])
     * @see N#difference(char[], char[])
     */
    @Override
    public CharList difference(final char[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new list containing the symmetric difference between this list and the specified CharList.
     * The symmetric difference consists of elements that are in either this list or the specified list,
     * but not in both. Occurrences are considered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list1 = CharList.of('a', 'b', 'c');
     * CharList list2 = CharList.of('b', 'c', 'd');
     * CharList result = list1.symmetricDifference(list2);   // result will be ['a', 'd']
     * }</pre>
     *
     * @param b the CharList to find the symmetric difference with
     * @return a new CharList containing elements that are in either list but not in both,
     *         considering the number of occurrences
     * @see #symmetricDifference(char[])
     * @see #difference(CharList)
     * @see #intersection(CharList)
     */
    @Override
    public CharList symmetricDifference(final CharList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();
        final CharList c = new CharList(N.max(9, Math.abs(size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        for (int i = 0, len = b.size(); i < len; i++) {
            if (bOccurrences.remove(b.elementData[i])) {
                c.add(b.elementData[i]);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return c;
    }

    /**
     * Returns a new list containing the symmetric difference between this list and the specified array.
     * The symmetric difference consists of elements that are in either this list or the specified array,
     * but not in both. Occurrences are considered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * CharList result = list.symmetricDifference(new char[] {'b', 'c', 'd'});   // returns ['a', 'd']
     * CharList all = list.symmetricDifference(new char[0]);                     // returns ['a', 'b', 'c'] (copy)
     * }</pre>
     *
     * @param b the array to find the symmetric difference with
     * @return a new CharList containing elements that are in either the list or array but not in both,
     *         considering the number of occurrences
     * @see #symmetricDifference(CharList)
     * @see #difference(char[])
     * @see #intersection(char[])
     */
    @Override
    public CharList symmetricDifference(final char[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Returns the number of occurrences of the specified value in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
     * int count = list.frequency('a');     // returns 3
     * int none = list.frequency('z');      // returns 0
     * }</pre>
     *
     * @param valueToFind the value whose occurrences are to be counted
     * @return the number of times the specified value appears in this list
     */
    public int frequency(final char valueToFind) {
        if (size == 0) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'b');
     * int idx = list.indexOf('b');       // returns 1
     * int missing = list.indexOf('z');   // returns -1
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int indexOf(final char valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * starting the search at the specified index, or -1 if the element is not found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'b');
     * int idx = list.indexOf('b', 2);       // returns 3 (search starts at index 2)
     * int missing = list.indexOf('a', 1);   // returns -1 (no 'a' at or after index 1)
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start the search from (inclusive)
     * @return the index of the first occurrence of the element in this list at position &gt;= fromIndex,
     *         or -1 if the element is not found
     */
    public int indexOf(final char valueToFind, final int fromIndex) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'b');
     * int idx = list.lastIndexOf('b');       // returns 3
     * int missing = list.lastIndexOf('z');   // returns -1
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int lastIndexOf(final char valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     * If {@code startIndexFromBack} is negative or the list is empty, -1 is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'b');
     * int idx = list.lastIndexOf('b', 2);       // returns 1 (searching back from index 2)
     * int missing = list.lastIndexOf('c', 1);   // returns -1 (no 'c' at or before index 1)
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start the backward search from (inclusive);
     *        if greater than or equal to the list size, the search starts from the last element
     * @return the index of the last occurrence of the element at position &lt;= startIndexFromBack,
     *         or -1 if the element is not found
     */
    public int lastIndexOf(final char valueToFind, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the minimum element in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar min = list.min();               // returns OptionalChar['a']
     * OptionalChar empty = new CharList().min();   // returns OptionalChar.empty
     * }</pre>
     *
     * @return an OptionalChar containing the minimum element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar min() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar min = list.min(1, 4);  // returns min of ['b', 'h', 'a'] = OptionalChar['a']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @return an OptionalChar containing the minimum element in the specified range,
     *         or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public OptionalChar min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar max = list.max();               // returns OptionalChar['i']
     * OptionalChar empty = new CharList().max();   // returns OptionalChar.empty
     * }</pre>
     *
     * @return an OptionalChar containing the maximum element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar max() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar max = list.max(1, 4);  // returns max of ['b', 'h', 'a'] = OptionalChar['h']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @return an OptionalChar containing the maximum element in the specified range,
     *         or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public OptionalChar max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     *
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar median = list.median();  // returns sorted: ['a', 'b', 'e', 'h', 'i']; median = OptionalChar['e']
     * }</pre>
     *
     * @return an OptionalChar containing the median value if the list is non-empty, or an empty OptionalChar if the list is empty
     */
    public OptionalChar median() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, 0, size));
    }

    /**
     * Returns the median value of elements within the specified range of this list.
     *
     * <p>The median is computed for elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * For ranges with an odd number of elements, this returns the exact middle element when sorted.
     * For ranges with an even number of elements, this returns the lower of the two middle elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('e', 'b', 'h', 'a', 'i');
     * OptionalChar median = list.median(1, 4);  // returns median of ['b', 'h', 'a'] = OptionalChar['b']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to calculate median for
     * @param toIndex the ending index (exclusive) of the range to calculate median for
     * @return an OptionalChar containing the median value if the range is non-empty, or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalChar median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element of this list in sequential order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * StringBuilder sb = new StringBuilder();
     * list.forEach(sb::append);   // sb is now "abc"
     * }</pre>
     *
     * @param action the action to be performed for each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    public void forEach(final CharConsumer action) {
        N.checkArgNotNull(action, cs.action);

        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element within the specified range of this list.
     *
     * <p>This method supports both forward and backward iteration based on the relative values of
     * {@code fromIndex} and {@code toIndex}:</p>
     * <ul>
     *   <li>If {@code fromIndex <= toIndex}: iterates forward from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)</li>
     *   <li>If {@code fromIndex > toIndex}: iterates backward from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)</li>
     *   <li>If {@code toIndex == -1}: treated as backward iteration from {@code fromIndex} to the beginning of the list</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * StringBuilder fwd = new StringBuilder();
     * list.forEach(1, 3, fwd::append);    // forward over indices 1,2; fwd is now "bc"
     * StringBuilder back = new StringBuilder();
     * list.forEach(3, -1, back::append);  // backward from index 3 to start; back is now "dcba"
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or {@code -1} for backward iteration to the start
     * @param action the action to be performed for each element; must not be {@code null}
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    public void forEach(final int fromIndex, final int toIndex, final CharConsumer action) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex), size);
        N.checkArgNotNull(action, cs.action);

        if (size > 0) {
            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(elementData[i]);
                }
            } else {
                for (int i = N.min(size - 1, fromIndex); i > toIndex; i--) {
                    action.accept(elementData[i]);
                }
            }
        }
    }

    /**
     * Returns the first element in this list wrapped in an OptionalChar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char f = list.first().get();                          // returns 'a'
     * boolean empty = new CharList().first().isPresent();   // returns false
     * }</pre>
     *
     * @return an OptionalChar containing the first element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar first() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[0]);
    }

    /**
     * Returns the last element in this list wrapped in an OptionalChar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char l = list.last().get();                          // returns 'c'
     * boolean empty = new CharList().last().isPresent();   // returns false
     * }</pre>
     *
     * @return an OptionalChar containing the last element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar last() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[size() - 1]);
    }

    /**
     * Returns a new CharList containing only the distinct elements from the specified range of this list.
     * The order of elements is preserved (keeps the first occurrence of each element).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'a', 'c', 'b');
     * CharList distinct = list.distinct(0, 5);   // returns ['a', 'b', 'c']
     * CharList part = list.distinct(1, 3);       // returns ['b', 'a']
     * }</pre>
     *
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @return a new CharList containing distinct elements from the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    @Override
    public CharList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains any duplicate elements.
     * An element is considered a duplicate if it appears more than once in the list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean dup = CharList.of('a', 'b', 'c', 'a').containsDuplicates();   // returns true
     * boolean unique = CharList.of('a', 'b', 'c').containsDuplicates();     // returns false
     * }</pre>
     *
     * @return {@code true} if the list contains at least one duplicate element, {@code false} otherwise
     */
    @Override
    public boolean containsDuplicates() {
        return N.containsDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order
     * according to their Unicode values.
     * An empty list or a list with a single element is considered sorted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean sorted = CharList.of('a', 'b', 'c').isSorted();     // returns true
     * boolean notSorted = CharList.of('c', 'b', 'a').isSorted();  // returns false
     * }</pre>
     *
     * @return {@code true} if all elements are in ascending order (allowing equal consecutive values),
     *         {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts all elements in this list in ascending order (by Unicode value).
     * This method modifies the list in place using an efficient sorting algorithm.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('c', 'a', 'b');
     * list.sort();   // list is now ['a', 'b', 'c']
     * }</pre>
     *
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in ascending order using a parallel sorting algorithm.
     * This method modifies the list in place and may offer better performance than {@link #sort()}
     * for large lists on multi-core systems. For small lists it may be slower due to thread overhead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('c', 'a', 'b');
     * list.parallelSort();   // list is now ['a', 'b', 'c']
     * }</pre>
     *
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in descending order (by Unicode value).
     * This method first sorts the list in ascending order, then reverses it.
     * The list is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('c', 'a', 'b');
     * list.reverseSort();   // list is now ['c', 'b', 'a']
     * }</pre>
     *
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value in this list using binary search algorithm.
     * The list must be sorted in ascending order prior to making this call.
     * If the list is not sorted, the results are undefined.
     *
     * <p>If the list contains multiple elements equal to the specified value,
     * there is no guarantee which one will be found.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList sorted = CharList.of('a', 'c', 'e', 'g');
     * int found = sorted.binarySearch('e');       // returns 2
     * int notFound = sorted.binarySearch('b');    // returns -2 (would insert at index 1)
     * }</pre>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the list;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined
     *         as the point at which the key would be inserted into the list
     */
    public int binarySearch(final char valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified value in the specified range of this list using binary search algorithm.
     * The range must be sorted in ascending order prior to making this call.
     * If the range is not sorted, the results are undefined.
     *
     * <p>If the range contains multiple elements equal to the specified value,
     * there is no guarantee which one will be found.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList sorted = CharList.of('a', 'c', 'e', 'g');
     * int found = sorted.binarySearch(0, 4, 'e');       // returns 2
     * int notFound = sorted.binarySearch(0, 4, 'b');    // returns -2 (would insert at index 1)
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the specified range;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined
     *         as the point at which the key would be inserted into the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > size()},
     *         or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final char valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * After this method returns, the first element becomes the last,
     * the second element becomes the second to last, and so on.
     * This method modifies the list in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.reverse();   // list is now ['d', 'c', 'b', 'a']
     * }</pre>
     *
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * After this method returns, the element at {@code fromIndex} becomes the element
     * at {@code toIndex - 1}, and vice versa. Elements outside the specified range
     * are not affected. This method modifies the list in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.reverse(1, 3);   // reverses indices 1,2; list is now ['a', 'c', 'b', 'd']
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse
     * @param toIndex the ending index (exclusive) of the range to reverse
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > size()},
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public void reverse(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            N.reverse(elementData, fromIndex, toIndex);
        }
    }

    /**
     * Rotates all elements in this list by the specified distance.
     * After calling rotate(distance), the element at index i will be moved to
     * index (i + distance) % size.
     *
     * <p>Positive values of distance rotate elements towards higher indices (right rotation),
     * while negative values rotate towards lower indices (left rotation).
     * The list is modified in place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
     * list.rotate(2);    // right rotation; list is now ['d', 'e', 'a', 'b', 'c']
     *
     * CharList list2 = CharList.of('a', 'b', 'c', 'd', 'e');
     * list2.rotate(-1);  // left rotation; list2 is now ['b', 'c', 'd', 'e', 'a']
     * }</pre>
     *
     * @param distance the distance to rotate the list. Positive values rotate right,
     *                 negative values rotate left
     * @see N#rotate(char[], int)
     */
    @Override
    public void rotate(final int distance) {
        if (size > 1) {
            N.rotate(elementData, 0, size, distance);
        }
    }

    /**
     * Randomly shuffles all elements in this list.
     * After this method returns, the elements will be in a random order.
     * This method uses a default source of randomness and modifies the list in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.shuffle();        // elements are reordered randomly; size() is still 4
     * // the same chars remain, only their order changes
     * }</pre>
     *
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles all elements in this list using the specified source of randomness.
     * After this method returns, the elements will be in a random order determined by
     * the given Random object. This method modifies the list in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.shuffle(new Random(42));   // deterministic order for a fixed seed; size() is still 4
     * }</pre>
     *
     * @param rnd the source of randomness to use for shuffling; must not be {@code null}
     * @throws IllegalArgumentException if {@code rnd} is {@code null}
     */
    @Override
    public void shuffle(final Random rnd) {
        N.checkArgNotNull(rnd, cs.rnd);

        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * If {@code i} and {@code j} are equal, this method leaves the list unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * list.swap(0, 3);   // list is now ['d', 'b', 'c', 'a']
     * list.swap(1, 1);   // list unchanged (same index)
     * }</pre>
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either {@code i} or {@code j} is out of range
     *         ({@code i < 0 || i >= size()} or {@code j < 0 || j >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a new CharList containing a copy of all elements in this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * CharList copy = list.copy();   // returns an independent copy ['a', 'b', 'c']
     * copy.set(0, 'x');              // copy is ['x', 'b', 'c'], list still ['a', 'b', 'c']
     * }</pre>
     *
     * @return a new CharList containing all elements from this list
     */
    @Override
    public CharList copy() {
        return new CharList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a new CharList containing a copy of elements in the specified range of this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * CharList sub = list.copy(1, 3);   // returns ['b', 'c']
     * CharList empty = list.copy(2, 2); // returns [] (empty range)
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @return a new CharList containing the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > size()},
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public CharList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a copy of elements from this list with the specified step.
     *
     * <p>This method creates a new CharList by selecting elements at regular intervals
     * defined by the step parameter. Starting from {@code fromIndex}, it includes every
     * {@code step}-th element until reaching {@code toIndex}.</p>
     *
     * <p>Special cases:</p>
     * <ul>
     * <li>If {@code step > 0}: iterates forward from fromIndex to toIndex</li>
     * <li>If {@code step < 0}: iterates backward from fromIndex to toIndex</li>
     * <li>If {@code fromIndex > toIndex} and {@code step < 0}: creates a reversed copy</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <ul>
     * <li>{@code copy(0, 10, 2)} returns elements at indices 0, 2, 4, 6, 8</li>
     * <li>{@code copy(9, -1, -2)} returns elements at indices 9, 7, 5, 3, 1</li>
     * </ul>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive) of the range to copy; use {@code -1} for backward iteration to index 0
     * @param step the step size between selected elements
     * @return a new CharList containing the selected elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if {@code step} is 0
     * @see N#copyOfRange(char[], int, int, int)
     */
    @Override
    public CharList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        // Clamp a descending start against the logical size (like forEach): N.copyOfRange clamps
        // against the backing array's length, which may exceed size and expose phantom elements.
        return new CharList(N.copyOfRange(elementData, fromIndex > toIndex ? N.min(size - 1, fromIndex) : fromIndex, toIndex, step));
    }

    /**
     * Splits this list into multiple sublists of the specified size.
     *
     * <p>This method divides the specified range of the list into consecutive sublists,
     * each containing up to {@code chunkSize} elements. The last sublist may contain fewer
     * elements if the range size is not evenly divisible by {@code chunkSize}.</p>
     *
     * <p>Each returned sublist is a new independent CharList instance. Modifications to
     * the returned sublists do not affect this list or each other.</p>
     *
     * <p><b>Usage Examples:</b> Splitting [a,b,c,d,e,f,g,h,i] with chunkSize 3 returns [[a,b,c], [d,e,f], [g,h,i]]</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index after the last element (exclusive) to be included
     * @param chunkSize the desired size of each sublist (must be positive)
     * @return a List containing the sublists, each of type CharList
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize <= 0}
     */
    @Override
    public List<CharList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<char[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<CharList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this CharList instance to be the list's current size.
     * This method can be used to minimize the storage of a CharList instance.
     * If the capacity is already equal to the size, this method does nothing.
     *
     * <p>After this call, the capacity of the list will be equal to its size,
     * eliminating any unused capacity.</p>
     *
     * @return this CharList instance (for method chaining)
     */
    @Override
    public CharList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Removes all elements from this list. The list will be empty after this call returns.
     * The capacity of the list is not changed.
     *
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, (char) 0);
        }

        size = 0;
    }

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a List containing all elements in this list converted to Character objects.
     * The returned list is a new ArrayList and is independent of this list.
     *
     * <p>This method is useful when you need to work with APIs that require
     * {@code List<Character>} rather than primitive char arrays.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * List<Character> boxed = list.boxed();   // returns [a, b, c] as List<Character>
     * boxed.get(0);                           // returns Character 'a'
     * }</pre>
     *
     * @return a new List&lt;Character&gt; containing all elements from this list
     */
    @Override
    public List<Character> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing elements from the specified range of this list
     * converted to Character objects. The returned list is a new ArrayList and
     * is independent of this list.
     *
     * <p>This method is useful when you need to work with APIs that require
     * {@code List<Character>} rather than primitive char arrays.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * List<Character> boxed = list.boxed(1, 3);   // returns [b, c] as List<Character>
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to box
     * @param toIndex the ending index (exclusive) of the range to box
     * @return a new List&lt;Character&gt; containing elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > size()},
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public List<Character> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Character> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char[] arr = list.toArray();   // returns ['a', 'b', 'c']
     * arr[0] = 'x';                  // list unchanged (independent array)
     * }</pre>
     *
     * @return a new char array containing all elements of this list
     */
    @Override
    public char[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this CharList to an IntList.
     * Each char value is widened to an int value, preserving its Unicode code point.
     * The returned IntList is independent of this list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * IntList ints = list.toIntList();   // returns [97, 98, 99] (Unicode code points)
     * }</pre>
     *
     * @return a new IntList containing the Unicode code point values of all elements in this list
     */
    public IntList toIntList() {
        final int[] a = new int[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return IntList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of the collection to return
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @param supplier a function which produces a new collection of the desired type
     * @return a collection containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Character>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final C c = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            c.add(elementData[i]);
        }

        return c;
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     *
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @param supplier a function which produces a new Multiset of the desired type
     * @return a Multiset containing the specified range of elements with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Character> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Character>> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Character> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is a specialized primitive iterator that avoids boxing
     * overhead. It provides methods like {@code nextChar()} to retrieve primitive values
     * directly.</p>
     *
     * <p>The iterator is <b>not</b> fail-fast: it iterates over the list's backing array
     * directly, and concurrent structural modifications during iteration yield undefined
     * results rather than a {@code ConcurrentModificationException}.</p>
     *
     * @return a CharIterator over the elements in this list
     */
    @Override
    public CharIterator iterator() {
        if (isEmpty()) {
            return CharIterator.EMPTY;
        }

        return CharIterator.of(elementData, 0, size);
    }

    /**
     * Returns a CharStream with this list as its source.
     *
     * <p>This method creates a stream that can be used to perform functional-style operations
     * on the elements of this list. The stream operates on primitive char values, avoiding
     * boxing overhead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * charList.stream()
     *     .filter(ch -> Character.isLetter(ch))
     *     .map(ch -> Character.toUpperCase(ch))
     *     .forEach(System.out::print);
     * }</pre>
     *
     * @return a CharStream over all elements in this list
     */
    public CharStream stream() {
        return CharStream.of(elementData, 0, size());
    }

    /**
     * Returns a CharStream over the specified range of this list.
     *
     * <p>This method creates a stream that operates on elements from {@code fromIndex}
     * (inclusive) to {@code toIndex} (exclusive). The stream operates on primitive char
     * values, avoiding boxing overhead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * long count = list.stream(1, 3).count();   // returns 2 (elements at indices 1,2)
     * }</pre>
     *
     * @param fromIndex the index of the first element (inclusive) to include in the stream
     * @param toIndex the index after the last element (exclusive) to include in the stream
     * @return a CharStream over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public CharStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return CharStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     *
     * <p>This method provides constant-time access to the first element without removing it
     * from the list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char first = list.getFirst();   // returns 'a'
     * new CharList().getFirst();      // throws NoSuchElementException
     * }</pre>
     *
     * @return the first char value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public char getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     *
     * <p>This method provides constant-time access to the last element without removing it
     * from the list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char last = list.getLast();   // returns 'c'
     * new CharList().getLast();     // throws NoSuchElementException
     * }</pre>
     *
     * @return the last char value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public char getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * <p>This method shifts all existing elements one position to the right (increasing their
     * indices by 1) and inserts the new element at index 0. This operation has O(n) time
     * complexity where n is the size of the list.</p>
     *
     * <p>If the list's capacity needs to be increased, it will be grown automatically.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('b', 'c');
     * list.addFirst('a');   // list is now ['a', 'b', 'c']
     * }</pre>
     *
     * @param e the char element to add at the beginning of the list
     */
    public void addFirst(final char e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method adds the element after the current last element. This operation has
     * amortized O(1) time complexity. If the list's capacity needs to be increased, it will
     * be grown automatically.</p>
     *
     * <p>This method is equivalent to {@code add(e)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b');
     * list.addLast('c');   // list is now ['a', 'b', 'c']
     * }</pre>
     *
     * @param e the char element to append to the list
     */
    public void addLast(final char e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * <p>This method removes the element at index 0 and shifts all remaining elements one
     * position to the left (decreasing their indices by 1). This operation has O(n) time
     * complexity where n is the size of the list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char removed = list.removeFirst();   // returns 'a', list is now ['b', 'c']
     * new CharList().removeFirst();        // throws NoSuchElementException
     * }</pre>
     *
     * @return the first char value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public char removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return removeAt(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * <p>This method removes the element at the last position. This operation has O(1)
     * time complexity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * char removed = list.removeLast();   // returns 'c', list is now ['a', 'b']
     * new CharList().removeLast();        // throws NoSuchElementException
     * }</pre>
     *
     * @return the last char value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public char removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return removeAt(size - 1);
    }

    /**
     * Returns the hash code value for this list.
     * The hash code is computed based on the elements in the list and their order.
     * This method is consistent with {@link #equals(Object)}.
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns {@code true} if and only if the specified object is also a CharList,
     * both lists have the same size, and all corresponding pairs of elements
     * in the two lists are equal.
     *
     * @param obj the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CharList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     *
     * <p>The string representation consists of the list's elements, enclosed in square
     * brackets ("[]"). Adjacent elements are separated by the characters ", " (comma and space).
     * Elements are displayed as their string representation (the character itself).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <ul>
     * <li>An empty list: "[]"</li>
     * <li>A list containing 'a', 'b', 'c': "[a, b, c]"</li>
     * </ul>
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return size == 0 ? Strings.STR_FOR_EMPTY_ARRAY : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity < 0 || minCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new char[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}
