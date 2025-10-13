/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.u.Optional;

/**
 * This is the Joiner class. It provides various methods to join elements into a string.
 * The class implements the Closeable interface, indicating that instances of this class can be closed to release resources.
 * The class provides methods to append different types of elements, including primitives, arrays, collections, and maps.
 * It also provides methods to control the joining behavior, such as whether to trim before appending, whether to skip nulls, and whether to use a cached buffer.
 * The class also includes methods for repeating a string, merging another Joiner, and converting the joined elements into a string, a map, or a stream.
 *
 * @see Splitter
 */
public final class Joiner implements Closeable {

    public static final String DEFAULT_DELIMITER = Strings.ELEMENT_SEPARATOR;

    public static final String DEFAULT_KEY_VALUE_DELIMITER = "=";

    private final String prefix;

    private final String separator;

    private final String keyValueSeparator;

    private final String suffix;

    private final boolean isEmptySeparator;

    private final boolean isEmptyKeyValueSeparator;

    private boolean trimBeforeAppend = false;

    private boolean skipNulls = false;

    private boolean useCachedBuffer = false;

    private String nullText = Strings.NULL;

    private StringBuilder buffer;

    private String emptyValue;

    private String latestToStringValue;

    private boolean isClosed;

    Joiner(final CharSequence separator) {
        this(separator, DEFAULT_KEY_VALUE_DELIMITER);
    }

    Joiner(final CharSequence separator, final CharSequence keyValueDelimiter) {
        this(separator, keyValueDelimiter, "", "");
    }

    Joiner(final CharSequence separator, final CharSequence prefix, final CharSequence suffix) {
        this(separator, DEFAULT_KEY_VALUE_DELIMITER, prefix, suffix);
    }

    Joiner(final CharSequence separator, final CharSequence keyValueSeparator, final CharSequence prefix, final CharSequence suffix) {
        N.checkArgNotNull(prefix, "The prefix must not be null");
        N.checkArgNotNull(separator, "The separator must not be null");
        N.checkArgNotNull(keyValueSeparator, "The keyValueSeparator must not be null");
        N.checkArgNotNull(suffix, "The suffix must not be null");

        // make defensive copies of arguments
        this.prefix = prefix.toString();
        this.separator = separator.toString();
        this.keyValueSeparator = keyValueSeparator.toString();
        this.suffix = suffix.toString();
        emptyValue = this.prefix + this.suffix;
        isEmptySeparator = Strings.isEmpty(separator);
        isEmptyKeyValueSeparator = Strings.isEmpty(keyValueSeparator);
    }

    /**
     * Returns a default Joiner instance with default delimiter (", ") and default key-value delimiter ("=").
     * This is a convenience method equivalent to calling {@code with(DEFAULT_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER)}.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.defauLt().appendAll("a", "b", "c").toString(); // Returns: "a, b, c"
     * }</pre>
     *
     * @return a new Joiner instance with default delimiters
     * @see #with(CharSequence)
     * @see #with(CharSequence, CharSequence)
     * @see Splitter#defauLt()
     * @see Splitter.MapSplitter#defauLt()
     */
    @Beta
    public static Joiner defauLt() {
        return with(DEFAULT_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER);
    }

    /**
     * Creates a new Joiner instance with the specified separator.
     * The key-value separator will be set to the default value ("=").
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendAll("a", "b", "c").toString(); // Returns: "a, b, c"
     * Joiner.with("-").appendAll(1, 2, 3).toString();        // Returns: "1-2-3"
     * }</pre>
     *
     * @param separator the delimiter to use between joined elements, must not be null
     * @return a new Joiner instance with the specified separator
     * @throws IllegalArgumentException if separator is null
     */
    public static Joiner with(final CharSequence separator) {
        return new Joiner(separator);
    }

    /**
     * Creates a new Joiner instance with the specified separator and key-value delimiter.
     * This is useful for joining map entries or key-value pairs.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ", "=").appendEntry("key", "value").toString(); // Returns: "key=value"
     * Joiner.with("; ", ": ").appendEntries(map).toString();         // Returns: "a: 1; b: 2"
     * }</pre>
     *
     * @param separator the delimiter to use between joined elements, must not be null
     * @param keyValueDelimiter the delimiter to use between keys and values, must not be null
     * @return a new Joiner instance with the specified separators
     * @throws IllegalArgumentException if separator or keyValueDelimiter is null
     */
    public static Joiner with(final CharSequence separator, final CharSequence keyValueDelimiter) {
        return new Joiner(separator, keyValueDelimiter);
    }

    /**
     * Creates a new Joiner instance with the specified separator, prefix, and suffix.
     * The prefix is prepended to the result and the suffix is appended to the result.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ", "[", "]").appendAll("a", "b", "c").toString(); // Returns: "[a, b, c]"
     * Joiner.with(" | ", "{", "}").appendAll(1, 2, 3).toString();      // Returns: "{1 | 2 | 3}"
     * }</pre>
     *
     * @param separator the delimiter to use between joined elements, must not be null
     * @param prefix the string to prepend to the result, must not be null
     * @param suffix the string to append to the result, must not be null
     * @return a new Joiner instance with the specified separator, prefix, and suffix
     * @throws IllegalArgumentException if any parameter is null
     */
    public static Joiner with(final CharSequence separator, final CharSequence prefix, final CharSequence suffix) {
        return new Joiner(separator, prefix, suffix);
    }

    /**
     * Creates a new Joiner instance with the specified separator, key-value separator, prefix, and suffix.
     * This provides full control over all formatting aspects of the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ", "=", "{", "}").appendEntry("a", 1).appendEntry("b", 2).toString(); 
     * // Returns: "{a=1, b=2}"
     * }</pre>
     *
     * @param separator the delimiter to use between joined elements, must not be null
     * @param keyValueSeparator the delimiter to use between keys and values, must not be null
     * @param prefix the string to prepend to the result, must not be null
     * @param suffix the string to append to the result, must not be null
     * @return a new Joiner instance with all specified formatting options
     * @throws IllegalArgumentException if any parameter is null
     */
    public static Joiner with(final CharSequence separator, final CharSequence keyValueSeparator, final CharSequence prefix, final CharSequence suffix) {
        return new Joiner(separator, keyValueSeparator, prefix, suffix);
    }

    /**
     * Sets the value to be returned when no elements have been added to the joiner.
     * By default, when no elements are added, the joiner returns prefix + suffix.
     * This method allows you to override that behavior.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").setEmptyValue("NONE").toString();                    // Returns: "NONE"
     * Joiner.with(", ").setEmptyValue("[]").appendAll(new int[0]).toString(); // Returns: "[]"
     * }</pre>
     *
     * @param emptyValue the value to return when no elements have been added, must not be null
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if emptyValue is null
     */
    public Joiner setEmptyValue(final CharSequence emptyValue) throws IllegalArgumentException {
        this.emptyValue = N.checkArgNotNull(emptyValue, "The empty value must not be null").toString();

        return this;
    }

    /**
     * Configures the joiner to trim whitespace from the beginning and end of each string element before appending.
     * This affects String, CharSequence, and the string representation of objects.
     * Primitive values are not affected.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").trimBeforeAppend().appendAll("  a  ", " b ", "c  ").toString(); 
     * // Returns: "a, b, c"
     * }</pre>
     *
     * @return this Joiner instance for method chaining
     */
    public Joiner trimBeforeAppend() {
        trimBeforeAppend = true;

        return this;
    }

    /**
     * Configures the joiner to skip null elements instead of adding them to the result.
     * When enabled, null values will be ignored during appending operations.
     * By default, null values are converted to the string specified by {@link #useForNull(String)}.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").skipNulls().appendAll("a", null, "b").toString(); // Returns: "a, b"
     * Joiner.with(", ").appendAll("a", null, "b").toString();             // Returns: "a, null, b"
     * }</pre>
     *
     * @return this Joiner instance for method chaining
     */
    public Joiner skipNulls() {
        skipNulls = true;

        return this;
    }

    /**
     * Sets the string representation to use for null values.
     * This setting is ignored if {@link #skipNulls()} has been called.
     * The default value is "null".
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").useForNull("N/A").appendAll("a", null, "b").toString(); 
     * // Returns: "a, N/A, b"
     * }</pre>
     *
     * @param nullText the string to use for null values, if null is passed, "null" will be used
     * @return this Joiner instance for method chaining
     */
    public Joiner useForNull(final String nullText) {
        this.nullText = nullText == null ? Strings.NULL : nullText;

        return this;
    }

    //    /**

    /**
     * Enables the use of a cached StringBuilder from an object pool to improve performance.
     * When enabled, the internal buffer will be obtained from and returned to an object pool.
     * 
     * <p><b>Important:</b> When using cached buffer, you must call one of the terminal operations
     * ({@link #toString()}, {@link #appendTo(Appendable)}, {@link #map(Function)}, {@link #mapIfNotEmpty(Function)}, 
     * or {@link #close()} to properly recycle the buffer.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner joiner = Joiner.with(", ").reuseCachedBuffer();
     * try {
     *     String result = joiner.appendAll("a", "b", "c").toString();
     * } finally {
     *     joiner.close(); // Optional if toString() was called
     * }
     * }</pre>
     *
     * @return this Joiner instance for method chaining
     * @throws IllegalStateException if the buffer has already been created
     */
    @Beta
    public Joiner reuseCachedBuffer() {
        if (buffer != null) {
            throw new IllegalStateException("Can't reset because the buffer has been created");
        }

        useCachedBuffer = true;

        return this;
    }

    /**
     * Appends a boolean value to the joiner.
     * The boolean will be converted to "true" or "false" string representation.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(true).append(false).toString(); // Returns: "true, false"
     * }</pre>
     *
     * @param element the boolean value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final boolean element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends a char value to the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append('a').append('b').toString(); // Returns: "a, b"
     * }</pre>
     *
     * @param element the char value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final char element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends an int value to the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(1).append(2).append(3).toString(); // Returns: "1, 2, 3"
     * }</pre>
     *
     * @param element the int value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final int element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends a long value to the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(100L).append(200L).toString(); // Returns: "100, 200"
     * }</pre>
     *
     * @param element the long value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final long element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends a float value to the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(1.5f).append(2.5f).toString(); // Returns: "1.5, 2.5"
     * }</pre>
     *
     * @param element the float value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final float element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends a double value to the joiner.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(1.5).append(2.5).toString(); // Returns: "1.5, 2.5"
     * }</pre>
     *
     * @param element the double value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final double element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     * Appends a String to the joiner.
     * If the string is null, it will be handled according to the skipNulls and useForNull settings.
     * If trimBeforeAppend is enabled, the string will be trimmed before appending.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append("hello").append("world").toString(); // Returns: "hello, world"
     * Joiner.with(", ").skipNulls().append("a").append(null).append("b").toString(); // Returns: "a, b"
     * }</pre>
     *
     * @param element the String to append, may be null
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final String element) {
        if (element != null || !skipNulls) {
            prepareBuilder().append(element == null ? nullText : (trimBeforeAppend ? element.trim() : element));
        }

        return this;
    }

    /**
     * Appends a CharSequence to the joiner.
     * If the CharSequence is null, it will be handled according to the skipNulls and useForNull settings.
     * If trimBeforeAppend is enabled, the CharSequence will be converted to string and trimmed before appending.
     * 
     * <p>Example:
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("test");
     * Joiner.with(", ").append("hello").append(sb).toString(); // Returns: "hello, test"
     * }</pre>
     *
     * @param element the CharSequence to append, may be null
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final CharSequence element) {
        if (element != null || !skipNulls) {
            prepareBuilder().append(element == null ? nullText : (trimBeforeAppend ? element.toString().trim() : element));
        }

        return this;
    }

    /**
     * Appends a subsequence of the specified CharSequence to the joiner.
     * Characters from start (inclusive) to end (exclusive) will be appended.
     * If the CharSequence is null, it will be handled according to the skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append("hello", 0, 2).append("world", 1, 4).toString(); 
     * // Returns: "he, orl"
     * }</pre>
     *
     * @param element the CharSequence to append from, may be null
     * @param start the start index (inclusive)
     * @param end the end index (exclusive)
     * @return this Joiner instance for method chaining
     * @see Appendable#append(CharSequence, int, int)
     */
    public Joiner append(final CharSequence element, final int start, final int end) {
        if (element != null || !skipNulls) {
            if (element == null) {
                prepareBuilder().append(nullText);
            } else if (trimBeforeAppend) {
                prepareBuilder().append(element.subSequence(start, end).toString().trim());
            } else {
                prepareBuilder().append(element, start, end);
            }
        }

        return this;
    }

    /**
     * Appends a StringBuilder to the joiner.
     * If the StringBuilder is null, it will be handled according to the skipNulls and useForNull settings.
     * Note: The contents of the StringBuilder are appended directly without trimming even if trimBeforeAppend is enabled.
     * 
     * <p>Example:
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("world");
     * Joiner.with(", ").append("hello").append(sb).toString(); // Returns: "hello, world"
     * }</pre>
     *
     * @param element the StringBuilder to append, may be null
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final StringBuilder element) {
        if (element != null || !skipNulls) {
            if (element == null) {
                prepareBuilder().append(nullText);
            } else {
                prepareBuilder().append(element);
            }
        }

        return this;
    }

    /**
     * Appends an Object to the joiner.
     * The object will be converted to string using its toString() method.
     * If the object is null, it will be handled according to the skipNulls and useForNull settings.
     * If trimBeforeAppend is enabled, the string representation will be trimmed before appending.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").append(123).append("text").append(new Date()).toString();
     * // Returns something like: "123, text, Mon Jan 01 00:00:00 UTC 2024"
     * }</pre>
     *
     * @param element the Object to append, may be null
     * @return this Joiner instance for method chaining
     */
    public Joiner append(final Object element) { // Note: DO NOT remove/update this method because it also protects append(boolean/char/byte/.../double) from NullPointerException.
        if (element != null || !skipNulls) {
            prepareBuilder().append(toString(element));
        }

        return this;
    }

    /**
     * Appends an object to the joiner only if it is not null.
     * This is a convenience method that ignores the skipNulls setting for this specific append operation.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("b").toString(); 
     * // Returns: "a, b"
     * }</pre>
     *
     * @param element the object to append if not null
     * @return this Joiner instance for method chaining
     */
    public Joiner appendIfNotNull(final Object element) {
        if (element != null) {
            prepareBuilder().append(toString(element));
        }

        return this;
    }

    /**
     * Conditionally appends a value provided by a supplier if the specified condition is true.
     * The supplier is only invoked if the condition is true.
     * 
     * <p>Example:
     * <pre>{@code
     * boolean includeDetails = true;
     * Joiner.with(", ").append("basic")
     *     .appendIf(includeDetails, () -> "detailed info")
     *     .toString(); // Returns: "basic, detailed info"
     * }</pre>
     *
     * @param b the condition to check
     * @param supplier the supplier that provides the value to append when condition is true
     * @return this Joiner instance for method chaining
     */
    public Joiner appendIf(final boolean b, final Supplier<?> supplier) {
        if (b) {
            //noinspection resource
            append(supplier.get());
        }

        return this;
    }

    /**
     * Appends all elements from a boolean array to the joiner.
     * Each boolean value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * boolean[] arr = {true, false, true};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "true, false, true"
     * }</pre>
     *
     * @param a the boolean array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final boolean[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a boolean array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * boolean[] arr = {true, false, true, false};
     * Joiner.with(", ").appendAll(arr, 1, 3).toString(); // Returns: "false, true"
     * }</pre>
     *
     * @param a the boolean array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a char array to the joiner.
     * Each char value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c'};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "a, b, c"
     * }</pre>
     *
     * @param a the char array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final char[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a char array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * char[] arr = {'a', 'b', 'c', 'd'};
     * Joiner.with("-").appendAll(arr, 1, 3).toString(); // Returns: "b-c"
     * }</pre>
     *
     * @param a the char array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a byte array to the joiner.
     * Each byte value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * byte[] arr = {1, 2, 3};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "1, 2, 3"
     * }</pre>
     *
     * @param a the byte array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final byte[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a byte array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * byte[] arr = {1, 2, 3, 4};
     * Joiner.with("-").appendAll(arr, 1, 3).toString(); // Returns: "2-3"
     * }</pre>
     *
     * @param a the byte array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a short array to the joiner.
     * Each short value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * short[] arr = {10, 20, 30};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "10, 20, 30"
     * }</pre>
     *
     * @param a the short array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final short[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a short array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * short[] arr = {10, 20, 30, 40};
     * Joiner.with(" | ").appendAll(arr, 1, 3).toString(); // Returns: "20 | 30"
     * }</pre>
     *
     * @param a the short array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from an int array to the joiner.
     * Each int value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * int[] arr = {1, 2, 3};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "1, 2, 3"
     * }</pre>
     *
     * @param a the int array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final int[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from an int array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * int[] arr = {1, 2, 3, 4, 5};
     * Joiner.with("-").appendAll(arr, 1, 4).toString(); // Returns: "2-3-4"
     * }</pre>
     *
     * @param a the int array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a long array to the joiner.
     * Each long value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * long[] arr = {100L, 200L, 300L};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "100, 200, 300"
     * }</pre>
     *
     * @param a the long array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final long[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a long array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * long[] arr = {100L, 200L, 300L, 400L};
     * Joiner.with(" - ").appendAll(arr, 1, 3).toString(); // Returns: "200 - 300"
     * }</pre>
     *
     * @param a the long array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a float array to the joiner.
     * Each float value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * float[] arr = {1.5f, 2.5f, 3.5f};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "1.5, 2.5, 3.5"
     * }</pre>
     *
     * @param a the float array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final float[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a float array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * float[] arr = {1.1f, 2.2f, 3.3f, 4.4f};
     * Joiner.with("; ").appendAll(arr, 1, 3).toString(); // Returns: "2.2; 3.3"
     * }</pre>
     *
     * @param a the float array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a double array to the joiner.
     * Each double value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * double[] arr = {1.5, 2.5, 3.5};
     * Joiner.with(", ").appendAll(arr).toString(); // Returns: "1.5, 2.5, 3.5"
     * }</pre>
     *
     * @param a the double array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final double[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from a double array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * double[] arr = {1.1, 2.2, 3.3, 4.4};
     * Joiner.with(" | ").appendAll(arr, 0, 2).toString(); // Returns: "1.1 | 2.2"
     * }</pre>
     *
     * @param a the double array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from an Object array to the joiner.
     * Each element is converted to string and separated by the configured separator.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * Object[] arr = {"a", 1, null, "b"};
     * Joiner.with(", ").skipNulls().appendAll(arr).toString(); // Returns: "a, 1, b"
     * }</pre>
     *
     * @param a the Object array to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final Object[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     * Appends a range of elements from an Object array to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * String[] arr = {"a", "b", "c", "d"};
     * Joiner.with("-").appendAll(arr, 1, 3).toString(); // Returns: "b-c"
     * }</pre>
     *
     * @param a the Object array to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final Object[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != null || !skipNulls) {
                if (sb == null) {
                    sb = prepareBuilder().append(toString(a[i]));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(a[i]));
                    } else {
                        sb.append(separator).append(toString(a[i]));
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from a BooleanList to the joiner.
     * Each boolean value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "true, false, true"
     * }</pre>
     *
     * @param c the BooleanList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final BooleanList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a BooleanList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false);
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "false-true"
     * }</pre>
     *
     * @param c the BooleanList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final BooleanList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a CharList to the joiner.
     * Each char value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c');
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "a, b, c"
     * }</pre>
     *
     * @param c the CharList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final CharList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a CharList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * CharList list = CharList.of('a', 'b', 'c', 'd');
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "b-c"
     * }</pre>
     *
     * @param c the CharList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final CharList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a ByteList to the joiner.
     * Each byte value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)2, (byte)3);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "1, 2, 3"
     * }</pre>
     *
     * @param c the ByteList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final ByteList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a ByteList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)2, (byte)3, (byte)4);
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "2-3"
     * }</pre>
     *
     * @param c the ByteList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final ByteList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a ShortList to the joiner.
     * Each short value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * ShortList list = ShortList.of((short)10, (short)20, (short)30);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "10, 20, 30"
     * }</pre>
     *
     * @param c the ShortList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final ShortList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a ShortList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * ShortList list = ShortList.of((short)10, (short)20, (short)30, (short)40);
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "20-30"
     * }</pre>
     *
     * @param c the ShortList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final ShortList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from an IntList to the joiner.
     * Each int value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "1, 2, 3"
     * }</pre>
     *
     * @param c the IntList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final IntList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from an IntList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4);
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "2-3"
     * }</pre>
     *
     * @param c the IntList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final IntList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a LongList to the joiner.
     * Each long value is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * LongList list = LongList.of(100L, 200L, 300L);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "100, 200, 300"
     * }</pre>
     *
     * @param c the LongList to append, may be null or empty
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final LongList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a LongList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * 
     * <p>Example:
     * <pre>{@code
     * LongList list = LongList.of(100L, 200L, 300L, 400L);
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "200-300"
     * }</pre>
     *
     * @param c the LongList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final LongList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a FloatList to the joiner.
     * Each element is appended in sequence, separated by the configured separator.
     * Empty lists are ignored and the joiner remains unchanged.
     * 
     * <p>Example:
     * <pre>{@code
     * FloatList list = FloatList.of(1.5f, 2.5f, 3.5f);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "1.5, 2.5, 3.5"
     * }</pre>
     *
     * @param c the FloatList to append
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final FloatList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a FloatList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * Each element is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * FloatList list = FloatList.of(1.5f, 2.5f, 3.5f, 4.5f);
     * Joiner.with(", ").appendAll(list, 1, 3).toString(); // Returns: "2.5, 3.5"
     * }</pre>
     *
     * @param c the FloatList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final FloatList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a DoubleList to the joiner.
     * Each element is appended in sequence, separated by the configured separator.
     * Empty lists are ignored and the joiner remains unchanged.
     * 
     * <p>Example:
     * <pre>{@code
     * DoubleList list = DoubleList.of(1.5, 2.5, 3.5);
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "1.5, 2.5, 3.5"
     * }</pre>
     *
     * @param c the DoubleList to append
     * @return this Joiner instance for method chaining
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final DoubleList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a DoubleList to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * Each element is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * DoubleList list = DoubleList.of(1.5, 2.5, 3.5, 4.5);
     * Joiner.with(", ").appendAll(list, 1, 3).toString(); // Returns: "2.5, 3.5"
     * }</pre>
     *
     * @param c the DoubleList to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    @SuppressWarnings("deprecation")
    public Joiner appendAll(final DoubleList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     * Appends all elements from a Collection to the joiner.
     * Each element is appended in sequence, separated by the configured separator.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("apple", "banana", "cherry");
     * Joiner.with(", ").appendAll(list).toString(); // Returns: "apple, banana, cherry"
     * }</pre>
     *
     * @param c the Collection to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final Collection<?> c) {
        if (N.notEmpty(c)) {
            return appendAll(c, 0, c.size());
        }

        return this;
    }

    /**
     * Appends a range of elements from a Collection to the joiner.
     * Elements from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c", "d");
     * Joiner.with("-").appendAll(list, 1, 3).toString(); // Returns: "b-c"
     * }</pre>
     *
     * @param c the Collection to append from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendAll(final Collection<?> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        int i = 0;
        for (final Object e : c) {
            if (i++ < fromIndex) {
                continue;
            }

            if (e != null || !skipNulls) {
                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }

            if (i >= toIndex) {
                break;
            }
        }

        return this;
    }

    /**
     * Appends all elements from an Iterable to the joiner.
     * Each element is appended in sequence, separated by the configured separator.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * Iterable<String> iterable = Arrays.asList("one", "two", "three");
     * Joiner.with(" | ").appendAll(iterable).toString(); // Returns: "one | two | three"
     * }</pre>
     *
     * @param c the Iterable to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final Iterable<?> c) {
        if (c != null) {
            StringBuilder sb = null;

            for (final Object e : c) {
                if (e != null || !skipNulls) {
                    if (sb == null) {
                        sb = prepareBuilder().append(toString(e));
                    } else {
                        if (isEmptySeparator) {
                            sb.append(toString(e));
                        } else {
                            sb.append(separator).append(toString(e));
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends elements from an Iterable that satisfy the given filter predicate.
     * Only elements that pass the filter test are appended to the joiner.
     * The skipNulls setting is ignored; only the filter determines inclusion.
     *
     * <p>Example:
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
     * Joiner.with(", ").appendAll(numbers, n -> n % 2 == 0).toString(); // Returns: "2, 4"
     * }</pre>
     *
     * @param <T> the type of elements in the Iterable
     * @param c the Iterable to append from
     * @param filter the predicate to test elements; only elements that pass are appended; must not be {@code null}
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if {@code filter} is {@code null}
     */
    public <T> Joiner appendAll(final Iterable<? extends T> c, final Predicate<? super T> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); //NOSONAR

        if (c != null) {
            StringBuilder sb = null;

            for (final T e : c) {
                if (!filter.test(e)) {
                    continue;
                }

                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends all elements from an Iterator to the joiner.
     * Each element is appended in sequence, separated by the configured separator.
     * Null elements are handled according to skipNulls and useForNull settings.
     * 
     * <p>Example:
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("x", "y", "z").iterator();
     * Joiner.with("->").appendAll(iter).toString(); // Returns: "x->y->z"
     * }</pre>
     *
     * @param iter the Iterator to append from
     * @return this Joiner instance for method chaining
     */
    public Joiner appendAll(final Iterator<?> iter) {
        if (iter != null) {
            StringBuilder sb = null;
            Object e = null;

            while (iter.hasNext()) {
                e = iter.next();

                if (e != null || !skipNulls) {
                    if (sb == null) {
                        sb = prepareBuilder().append(toString(e));
                    } else {
                        if (isEmptySeparator) {
                            sb.append(toString(e));
                        } else {
                            sb.append(separator).append(toString(e));
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends elements from an Iterator that satisfy the given filter predicate.
     * Only elements that pass the filter test are appended to the joiner.
     * The skipNulls setting is ignored; only the filter determines inclusion.
     *
     * <p>Example:
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("cat", "dog", "bird", "fish").iterator();
     * Joiner.with(", ").appendAll(iter, s -> s.length() > 3).toString(); // Returns: "bird, fish"
     * }</pre>
     *
     * @param <T> the type of elements from the Iterator
     * @param iter the Iterator to append from
     * @param filter the predicate to test elements; only elements that pass are appended; must not be {@code null}
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if {@code filter} is {@code null}
     */
    public <T> Joiner appendAll(final Iterator<? extends T> iter, final Predicate<? super T> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (iter != null) {
            StringBuilder sb = null;
            T e = null;

            while (iter.hasNext()) {
                e = iter.next();

                if (!filter.test(e)) {
                    continue;
                }

                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends a key-value pair with a boolean value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("enabled", true).toString(); // Returns: "enabled=true"
     * }</pre>
     *
     * @param key the key to append
     * @param value the boolean value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final boolean value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with a char value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("grade", 'A').toString(); // Returns: "grade=A"
     * }</pre>
     *
     * @param key the key to append
     * @param value the char value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final char value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with an int value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("count", 42).toString(); // Returns: "count=42"
     * }</pre>
     *
     * @param key the key to append
     * @param value the int value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final int value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with a long value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("timestamp", 1234567890L).toString(); // Returns: "timestamp=1234567890"
     * }</pre>
     *
     * @param key the key to append
     * @param value the long value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final long value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with a float value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("price", 19.99f).toString(); // Returns: "price=19.99"
     * }</pre>
     *
     * @param key the key to append
     * @param value the float value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final float value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with a double value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * If multiple entries are appended, they are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("temperature", 98.6).toString(); // Returns: "temperature=98.6"
     * }</pre>
     *
     * @param key the key to append
     * @param value the double value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final double value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     * Appends a key-value pair with a String value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * Both key and value are formatted according to trimBeforeAppend and nullText settings.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").appendEntry("name", "John").toString(); // Returns: "name=John"
     * }</pre>
     *
     * @param key the key to append
     * @param value the String value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final String value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(format(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(format(value));
        }

        return this;
    }

    /**
     * Appends a key-value pair with a CharSequence value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * Both key and value are formatted according to trimBeforeAppend and nullText settings.
     * 
     * <p>Example:
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("value");
     * Joiner.with(", ").appendEntry("key", sb).toString(); // Returns: "key=value"
     * }</pre>
     *
     * @param key the key to append
     * @param value the CharSequence value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final CharSequence value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(format(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(format(value));
        }

        return this;
    }

    /**
     * Appends a key-value pair with a StringBuilder value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * Null values are replaced with nullText. If trimBeforeAppend is true, the value is trimmed.
     * 
     * <p>Example:
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("dynamic content");
     * Joiner.with(", ").appendEntry("data", sb).toString(); // Returns: "data=dynamic content"
     * }</pre>
     *
     * @param key the key to append
     * @param value the StringBuilder value to append (can be null)
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final StringBuilder value) {
        if (value == null) {
            if (isEmptyKeyValueSeparator) {
                prepareBuilder().append(format(key)).append(nullText);
            } else {
                prepareBuilder().append(format(key)).append(keyValueSeparator).append(nullText);
            }
        } else {
            if (isEmptyKeyValueSeparator) {
                if (trimBeforeAppend) {
                    prepareBuilder().append(format(key)).append(value.toString().trim());
                } else {
                    prepareBuilder().append(format(key)).append(value);
                }
            } else {
                if (trimBeforeAppend) {
                    prepareBuilder().append(format(key)).append(keyValueSeparator).append(value.toString().trim());
                } else {
                    prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
                }
            }
        }

        return this;
    }

    /**
     * Appends a key-value pair with an Object value to the joiner.
     * The key and value are separated by the configured keyValueSeparator.
     * The value is converted to string using toString method.
     * 
     * <p>Example:
     * <pre>{@code
     * Date date = new Date();
     * Joiner.with(", ").appendEntry("created", date).toString(); // Returns: "created=<date string>"
     * }</pre>
     *
     * @param key the key to append
     * @param value the Object value to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final String key, final Object value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(toString(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(toString(value));
        }

        return this;
    }

    /**
     * Appends a Map.Entry to the joiner.
     * The entry's key and value are separated by the configured keyValueSeparator.
     * Null entries are replaced with nullText.
     * 
     * <p>Example:
     * <pre>{@code
     * Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("score", 100);
     * Joiner.with(", ").appendEntry(entry).toString(); // Returns: "score=100"
     * }</pre>
     *
     * @param entry the Map.Entry to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntry(final Map.Entry<?, ?> entry) {
        if (entry == null) {
            //noinspection resource
            append(nullText);
        } else {
            //noinspection resource
            appendEntry(toString(entry.getKey()), toString(entry.getValue()));
        }

        return this;
    }

    /**
     * Appends all entries from a given map to the Joiner.
     * Each entry is appended as a key-value pair.
     * Entries are separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * Joiner.with(", ").appendEntries(map).toString(); // Returns: "a=1, b=2"
     * }</pre>
     *
     * @param m The map containing the entries to be appended
     * @return this Joiner instance for method chaining
     */
    public Joiner appendEntries(final Map<?, ?> m) {
        if (N.notEmpty(m)) {
            return appendEntries(m, 0, m.size());
        }

        return this;
    }

    /**
     * Appends a range of entries from a Map to the joiner.
     * Entries from index fromIndex (inclusive) to toIndex (exclusive) are appended.
     * The iteration order depends on the Map implementation.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new LinkedHashMap<>();
     * map.put("a", 1); map.put("b", 2); map.put("c", 3);
     * Joiner.with(", ").appendEntries(map, 1, 3).toString(); // Returns: "b=2, c=3"
     * }</pre>
     *
     * @param m the Map to append entries from
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return this Joiner instance for method chaining
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     */
    public Joiner appendEntries(final Map<?, ?> m, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, m == null ? 0 : m.size());

        if (N.isEmpty(m) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        int i = 0;
        for (final Map.Entry<?, ?> entry : m.entrySet()) {
            if (i++ < fromIndex) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }

            if (i >= toIndex) {
                break;
            }
        }

        return this;
    }

    /**
     * Appends entries from a Map that satisfy the given filter predicate.
     * Only entries that pass the filter test are appended to the joiner.
     * Each entry is formatted as key-value pair using the configured separators.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1); map.put("b", 2); map.put("c", 3);
     * Joiner.with(", ").appendEntries(map, e -> e.getValue() > 1).toString(); // Returns: "b=2, c=3"
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param m the Map to append entries from
     * @param filter the predicate to test entries; only entries that pass are appended; must not be {@code null}
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if {@code filter} is {@code null}
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final Predicate<? super Map.Entry<K, V>> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(m)) {
            return this;
        }

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (!filter.test(entry)) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }
        }

        return this;
    }

    /**
     * Appends entries from a Map that satisfy the given key-value filter predicate.
     * Only entries whose key and value pass the filter test are appended.
     * Each entry is formatted as key-value pair using the configured separators.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 5); map.put("banana", 3); map.put("cherry", 8);
     * Joiner.with(", ").appendEntries(map, (k, v) -> k.length() > 5 && v > 4).toString(); // Returns: "cherry=8"
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param m the Map to append entries from
     * @param filter the bi-predicate to test keys and values; only entries that pass are appended; must not be {@code null}
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if {@code filter} is {@code null}
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final BiPredicate<? super K, ? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(m)) {
            return this;
        }

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (!filter.test(entry.getKey(), entry.getValue())) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }
        }

        return this;
    }

    /**
     * Appends entries from a Map with transformed keys and values.
     * Each key and value is transformed using the provided extractors before appending.
     * Entries are formatted using the configured separators.
     * 
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("item1", 100); map.put("item2", 200);
     * Joiner.with(", ").appendEntries(map, 
     *     k -> k.toUpperCase(), 
     *     v -> "$" + v).toString(); // Returns: "ITEM1=$100, ITEM2=$200"
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param m the Map to append entries from
     * @param keyExtractor the function to transform keys before appending
     * @param valueExtractor the function to transform values before appending
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if keyExtractor or valueExtractor is null
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final Function<? super K, ?> keyExtractor, final Function<? super V, ?> valueExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotNull(valueExtractor, cs.valueExtractor);

        if (N.isEmpty(m)) {
            return this;
        }

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (sb == null) {
                sb = prepareBuilder().append(toString(keyExtractor.apply(entry.getKey())))
                        .append(keyValueSeparator)
                        .append(toString(valueExtractor.apply(entry.getValue())));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(keyExtractor.apply(entry.getKey())));
                } else {
                    sb.append(separator).append(toString(keyExtractor.apply(entry.getKey())));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(valueExtractor.apply(entry.getValue())));
                } else {
                    sb.append(keyValueSeparator).append(toString(valueExtractor.apply(entry.getValue())));
                }
            }
        }

        return this;
    }

    /**
     * Appends all properties of a bean object to the joiner.
     * Each property is appended as a key-value pair using the property name and value.
     * The bean must have getter/setter methods defined.
     * 
     * <p>Example:
     * <pre>{@code
     * class Person { 
     *     String name = "John"; 
     *     int age = 30; 
     *     // getters/setters...
     * }
     * Person p = new Person();
     * Joiner.with(", ").appendBean(p).toString(); // Returns: "name=John, age=30"
     * }</pre>
     *
     * @param bean the bean object whose properties to append
     * @return this Joiner instance for method chaining
     */
    public Joiner appendBean(final Object bean) {
        return appendBean(bean, true, null);
    }

    /**
     * Appends selected properties of a bean object to the joiner.
     * <p>
     * This method extracts and appends only the properties whose names are included in the
     * {@code selectPropNames} collection. Each property is formatted as a key-value pair
     * using the configured separators. The bean object must be a valid JavaBean with
     * proper getter/setter methods.
     * If {@code selectPropNames} is {@code null} or empty, no properties are appended.
     * </p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * class Person {
     *     private String name = "John";
     *     private int age = 30;
     *     private String city = "NYC";
     *     // getter/setter methods...
     * }
     *
     * Person person = new Person();
     * String result = Joiner.with(", ")
     *     .appendBean(person, Arrays.asList("name", "city"))
     *     .toString(); // Returns: "name=John, city=NYC"
     *
     * // With custom separators
     * String result2 = Joiner.with(" | ", ":")
     *     .appendBean(person, Arrays.asList("name", "age"))
     *     .toString(); // Returns: "name:John | age:30"
     * }</pre>
     *
     * @param bean the bean object whose selected properties to append; may be {@code null}
     * @param selectPropNames collection of property names to include; if {@code null} or empty, no properties are appended
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if {@code bean} is not {@code null} and its class is not a valid JavaBean (i.e., doesn't have proper getter/setter methods)
     * @see #appendBean(Object)
     * @see #appendBean(Object, boolean, Set)
     */
    public Joiner appendBean(final Object bean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        if (bean == null || N.isEmpty(selectPropNames)) {
            return this;
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(Beans.isBeanClass(cls), "'bean' must be bean class with getter/setter methods"); //NOSONAR

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
            propValue = beanInfo.getPropValue(bean, propName);

            if (sb == null) {
                sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
            } else {
                if (isEmptySeparator) {
                    sb.append(propName);
                } else {
                    sb.append(separator).append(propName);
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(propValue));
                } else {
                    sb.append(keyValueSeparator).append(toString(propValue));
                }
            }
        }

        return this;
    }

    /**
     * Appends properties of a bean object with control over null handling and property exclusion.
     * Properties can be filtered based on null values and/or explicit exclusion list.
     * Each included property is formatted as a key-value pair.
     * 
     * <p>Example:
     * <pre>{@code
     * class User { 
     *     String id = "123";
     *     String name = "Alice"; 
     *     String email = null;
     *     String password = "secret";
     *     // getters/setters...
     * }
     * User u = new User();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password"));
     * Joiner.with(", ").appendBean(u, true, ignored).toString(); // Returns: "id=123, name=Alice"
     * }</pre>
     *
     * @param bean the bean object whose properties to append
     * @param ignoreNullProperty if true, properties with null values are skipped
     * @param ignoredPropNames set of property names to exclude from appending
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if bean is not a valid bean class with getter/setter methods
     */
    public Joiner appendBean(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) throws IllegalArgumentException {
        if (bean == null) {
            return this;
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(Beans.isBeanClass(cls), "'bean' must be bean class with getter/setter methods");

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : Beans.getPropNameList(cls)) {
            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = beanInfo.getPropValue(bean, propName);

            if (propValue != null || !ignoreNullProperty) {
                if (sb == null) {
                    sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
                } else {
                    if (isEmptySeparator) {
                        sb.append(propName);
                    } else {
                        sb.append(separator).append(propName);
                    }

                    if (isEmptyKeyValueSeparator) {
                        sb.append(toString(propValue));
                    } else {
                        sb.append(keyValueSeparator).append(toString(propValue));
                    }
                }
            }
        }

        return this;
    }

    /**
     * Appends properties of a bean object that satisfy the given filter predicate.
     * The filter receives property name and value; only properties that pass are appended.
     * Each property is formatted as a key-value pair.
     * 
     * <p>Example:
     * <pre>{@code
     * class Product { 
     *     String name = "Laptop";
     *     double price = 999.99;
     *     int stock = 0;
     *     // getters/setters...
     * }
     * Product p = new Product();
     * Joiner.with(", ").appendBean(p, (prop, val) -> 
     *     !prop.equals("stock") || (Integer)val > 0).toString(); // Returns: "name=Laptop, price=999.99"
     * }</pre>
     *
     * @param bean the bean object whose properties to append
     * @param filter the bi-predicate to test property names and values; only properties that pass are appended
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if filter is null or bean is not a valid bean class
     */
    public Joiner appendBean(final Object bean, final BiPredicate<? super String, ?> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (bean == null) {
            return this;
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(Beans.isBeanClass(cls), "'bean' must be bean class with getter/setter methods");

        final BiPredicate<? super String, Object> filterToUse = (BiPredicate<? super String, Object>) filter;
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : Beans.getPropNameList(cls)) {
            propValue = beanInfo.getPropValue(bean, propName);

            if (!filterToUse.test(propName, propValue)) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
            } else {
                if (isEmptySeparator) {
                    sb.append(propName);
                } else {
                    sb.append(separator).append(propName);
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(propValue));
                } else {
                    sb.append(keyValueSeparator).append(toString(propValue));
                }
            }
        }

        return this;
    }

    /**
     * Repeats the specified string n times and appends to the joiner.
     * Each repetition is separated by the configured separator.
     * If n is 0, nothing is appended.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner.with(", ").repeat("Hello", 3).toString(); // Returns: "Hello, Hello, Hello"
     * }</pre>
     *
     * @param str the string to repeat
     * @param n the number of times to repeat (must be non-negative)
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if n is negative
     */
    public Joiner repeat(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        final String newString = toString(str);

        if (n < 10) {
            for (int i = 0; i < n; i++) {
                //noinspection resource
                append(newString);
            }
        } else {
            //noinspection resource
            append(Strings.repeat(newString, n, separator));
        }

        return this;
    }

    /**
     * Repeats the specified object n times and appends to the joiner.
     * The object is converted to string using toString method.
     * Each repetition is separated by the configured separator.
     * 
     * <p>Example:
     * <pre>{@code
     * Integer num = 42;
     * Joiner.with("-").repeat(num, 3).toString(); // Returns: "42-42-42"
     * }</pre>
     *
     * @param obj the object to repeat
     * @param n the number of times to repeat (must be non-negative)
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if n is negative
     */
    public Joiner repeat(final Object obj, final int n) {
        return repeat(toString(obj), n);
    }

    /**
     * Adds the contents from the specified Joiner {@code other} without prefix and suffix as the next element if it is non-empty.
     * If the specified {@code Joiner} is empty, the call has no effect.
     * Only the content between prefix and suffix from the other Joiner is merged.
     *
     * <p>Remember to close {@code other} Joiner if {@code reuseCachedBuffer} is set to {@code true}.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner j1 = Joiner.with(", ").append("a").append("b");
     * Joiner j2 = Joiner.with(", ").append("c").append("d");
     * j1.merge(j2).toString(); // Returns: "a, b, c, d"
     * }</pre>
     *
     * @param other the Joiner to merge content from
     * @return this Joiner instance for method chaining
     * @throws IllegalArgumentException if the specified Joiner {@code other} is {@code null}
     */
    public Joiner merge(final Joiner other) throws IllegalArgumentException {
        N.checkArgNotNull(other);

        if (other.buffer != null) {
            final int length = other.buffer.length();
            final StringBuilder builder = prepareBuilder();
            builder.append(other.buffer, other.prefix.length(), length);
        }

        return this;
    }

    /**
     * Returns the current length of the joined content.
     * The length includes prefix, all appended elements with separators, and suffix.
     * If no elements have been appended, returns the length of prefix + suffix.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner j = Joiner.with(", ", "[", "]");
     * j.append("a").append("b");
     * j.length(); // Returns: 6 (for "[a, b]")
     * }</pre>
     *
     * @return the length of the current joined content
     */
    public int length() {
        // Remember that we never actually append the suffix unless we return
        // the full (present) value or some substring or length of it, so that
        // we can add on more if we need to.
        return (buffer != null ? buffer.length() + suffix.length() : emptyValue.length());
    }

    /**
     * Returns the current value, consisting of the {@code prefix}, the values
     * added so far separated by the {@code delimiter}, and the {@code suffix},
     * unless no elements have been added in which case, the
     * {@code prefix + suffix} or the {@code emptyValue} characters are returned.
     * 
     * The underlying {@code StringBuilder} will be recycled after this method is called 
     * if {@code reuseStringBuilder} is set to {@code true}, and should not continue 
     * to be used with this instance.
     * 
     * <p>Example:
     * <pre>{@code
     * Joiner j = Joiner.with(", ", "[", "]");
     * j.append("a").append("b").append("c");
     * j.toString(); // Returns: "[a, b, c]"
     * }</pre>
     *
     * @return the joined string with prefix and suffix
     */
    @Override
    public String toString() {
        if (buffer == null) {
            return emptyValue;
        } else {
            try {
                String result = null;

                if (suffix.isEmpty()) {
                    result = buffer.toString();
                } else {
                    final int initialLength = buffer.length();

                    result = buffer.append(suffix).toString();

                    // reset value to pre-append initialLength
                    buffer.setLength(initialLength);
                }

                latestToStringValue = result;

                return result;
            } finally {
                recycleBuffer();
            }
        }
    }

    /**
     * Appends the joined string to the specified Appendable.
     * The joined string includes prefix, all elements with separators, and suffix.
     * If no elements have been appended, nothing is appended to the Appendable.
     * 
     * <p>Example:
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("Result: ");
     * Joiner.with(", ").append("a").append("b").appendTo(sb);
     * sb.toString(); // Returns: "Result: a, b"
     * }</pre>
     *
     * @param <A> the type of Appendable
     * @param appendable the Appendable to append the joined string to
     * @return the same Appendable instance for method chaining
     * @throws IOException if an I/O error occurs during appending
     */
    public <A extends Appendable> A appendTo(final A appendable) throws IOException {
        if (buffer == null) {
            return appendable;
        }

        appendable.append(toString());

        return appendable;
    }

    /**
     * Applies the given mapping function to the joined string and returns the result.
     * The underlying {@code StringBuilder} will be recycled after this method is called 
     * if {@code reuseStringBuilder} is set to {@code true}.
     * 
     * <p>Example:
     * <pre>{@code
     * int length = Joiner.with(", ").append("a").append("b").map(String::length); // Returns: 4
     * }</pre>
     *
     * @param <T> the type of the result
     * @param mapper the function to apply to the joined string
     * @return the result of applying the mapper function
     */
    public <T> T map(final Function<? super String, T> mapper) {
        return mapper.apply(toString());
    }

    /**
     * Applies the given mapping function to the joined string only if at least one element has been appended.
     * Returns an empty Optional if no elements have been appended.
     * The underlying {@code StringBuilder} will be recycled after this method is called 
     * if {@code reuseStringBuilder} is set to {@code true}.
     * 
     * <p>Example:
     * <pre>{@code
     * Optional<Integer> result1 = Joiner.with(", ").mapIfNotEmpty(String::length); // Returns: Optional.empty()
     * Optional<Integer> result1 = Joiner.with(", ", "[", "]").mapIfNotEmpty(String::length); // Returns: Optional.empty()
     * Optional<Integer> result2 = Joiner.with(", ").append("hello").mapIfNotEmpty(String::length); // Returns: Optional.of(5)
     * }</pre>
     *
     * @param <T> the type of the result
     * @param mapper the function to apply to the joined string if not empty
     * @return a Optional containing the result, or empty if no elements were appended
     * @throws IllegalArgumentException if mapper is null
     */
    public <T> Optional<T> mapIfNotEmpty(final Function<? super String, T> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper);

        return buffer == null ? Optional.empty() : Optional.of(mapper.apply(toString()));
    }

    /**
     * Closes this Joiner and releases any system resources associated with it.
     * If the Joiner is already closed then invoking this method has no effect.
     * After closing, the Joiner should not be used for further operations.
     * This method is synchronized to ensure thread safety.
     * 
     * <p>Example:
     * <pre>{@code
     * try (Joiner j = Joiner.with(", ").reuseCachedBuffer()) {
     *     j.append("a").append("b");
     *     System.out.println(j.toString());
     * } // Joiner is automatically closed
     * }</pre>
     */
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        recycleBuffer();
    }

    private String format(final String text) {
        return text == null ? nullText : (trimBeforeAppend ? text.trim() : text);
    }

    private String format(final CharSequence text) {
        return text == null ? nullText : (trimBeforeAppend ? text.toString().trim() : text.toString());
    }

    private String toString(final Object obj) {
        return obj == null ? nullText : (trimBeforeAppend ? N.toString(obj).trim() : N.toString(obj));
    }

    private StringBuilder prepareBuilder() {
        assertNotClosed();

        if (buffer != null) {
            if (!isEmptySeparator) {
                buffer.append(separator);
            }
        } else {
            buffer = (useCachedBuffer ? Objectory.createStringBuilder() : new StringBuilder())
                    .append(latestToStringValue == null ? prefix : latestToStringValue);

            if (!isEmptySeparator && latestToStringValue != null) {
                buffer.append(separator);
            }
        }

        return buffer;
    }

    private void recycleBuffer() {
        if (useCachedBuffer) {
            Objectory.recycle(buffer);
            buffer = null;

            // reset useCachedBuffer.
            useCachedBuffer = false;
        }
    }

    private void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException();
        }
    }
}
