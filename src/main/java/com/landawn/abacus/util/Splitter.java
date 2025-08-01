/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * A utility class for splitting strings based on various delimiters and patterns.
 * This class provides a fluent API for configuring and executing string splitting operations
 * with support for different delimiter types (character, string, pattern) and various
 * configuration options such as trimming, stripping whitespace, omitting empty strings,
 * and limiting the number of resulting substrings.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Split by comma
 * List<String> parts = Splitter.with(",").split("a,b,c");
 * 
 * // Split with trimming and omitting empty strings
 * List<String> parts = Splitter.with(",")
 *     .trimResults()
 *     .omitEmptyStrings()
 *     .split("a, ,b, c");
 * 
 * // Split with limit
 * List<String> parts = Splitter.with(",")
 *     .limit(2)
 *     .split("a,b,c"); // Returns ["a", "b,c"]
 * }</pre>
 *
 * @see Joiner
 */
@SuppressWarnings("java:S1192")
public final class Splitter {

    public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final SubStringFunc defaultSubStringFunc = (source, start, end) -> source.subSequence(start, end).toString();

    private static final SubStringFunc trimSubStringFunc = (source, start, end) -> {
        while (start < end && source.charAt(start) == ' ') {
            start++;
        }

        while (end > start && source.charAt(end - 1) == ' ') {
            end--;
        }

        return start >= end ? Strings.EMPTY : source.subSequence(start, end).toString();
    };

    private static final SubStringFunc stripSubStringFunc = (source, start, end) -> {
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        return start >= end ? Strings.EMPTY : source.subSequence(start, end).toString();
    };

    private final Strategy strategy;
    private boolean omitEmptyStrings = false;
    private boolean trimResults = false;
    private boolean stripResults = false;
    private int limit = Integer.MAX_VALUE;

    Splitter(final Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Creates a Splitter with the default delimiter: ", " (comma followed by space).
     * This is the standard delimiter used in many contexts such as CSV files or
     * comma-separated lists.
     *
     * @return a new Splitter instance configured with the default delimiter
     * @see Joiner#DEFAULT_DELIMITER
     * @see Joiner#DEFAULT_KEY_VALUE_DELIMITER
     * @see Joiner#defauLt()
     */
    @Beta
    public static Splitter defauLt() {
        return with(Joiner.DEFAULT_DELIMITER);
    }

    /**
     * Creates a Splitter configured to split text by line separators.
     * This method recognizes various line separator patterns including \n, \r, and \r\n.
     * Useful for splitting multi-line text into individual lines.
     *
     * @return a new Splitter instance configured to split by line separators
     */
    @Beta
    public static Splitter forLines() {
        return with(RegExUtil.LINE_SEPARATOR);
    }

    /**
     * Creates a Splitter that uses the specified character as a delimiter.
     * This is the most efficient option when splitting by a single character.
     *
     * @param delimiter the character to use as a delimiter for splitting
     * @return a new Splitter instance configured with the specified character delimiter
     */
    public static Splitter with(final char delimiter) {
        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new ObjIterator<>() {
                private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                private final int sourceLen = source.length();
                private String next = null;
                private int start = 0;
                private int cursor = 0;
                private int cnt = 0;

                @Override
                public boolean hasNext() {
                    if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                        if (limit - cnt == 1) {
                            next = subStringFunc.subString(source, start, sourceLen);
                            start = (cursor = sourceLen + 1);

                            if (omitEmptyStrings && next.isEmpty()) {
                                next = null;
                            }
                        } else {
                            while (cursor >= 0 && cursor <= sourceLen) {
                                if (cursor == sourceLen || source.charAt(cursor) == delimiter) {
                                    next = subStringFunc.subString(source, start, cursor);
                                    start = ++cursor;

                                    if (omitEmptyStrings && next.isEmpty()) {
                                        next = null;
                                    }

                                    if (next != null) {
                                        break;
                                    }
                                } else {
                                    cursor++;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String result = next;
                    next = null;
                    cnt++;
                    return result;
                }
            };
        });
    }

    /**
     * Creates a Splitter that uses the specified character sequence as a delimiter.
     * The delimiter is treated as a literal string, not as a pattern.
     * If the delimiter is a single character, this method delegates to the more
     * efficient single-character version.
     *
     * @param delimiter the character sequence to use as a delimiter for splitting
     * @return a new Splitter instance configured with the specified delimiter
     * @throws IllegalArgumentException if the specified delimiter is null or empty
     */
    public static Splitter with(final CharSequence delimiter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter);

        if (Strings.isEmpty(delimiter)) {
            return with(WHITE_SPACE_PATTERN);
        } else if (delimiter.length() == 1) {
            return with(delimiter.charAt(0));
        } else {
            return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
                if (source == null) {
                    return ObjIterator.empty();
                }

                return new ObjIterator<>() {
                    private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                    @SuppressWarnings("deprecation")
                    private final char[] delimiterChars = InternalUtil.getCharsForReadOnly(delimiter.toString());
                    private final int sourceLen = source.length();
                    private final int delimiterLen = delimiterChars.length;
                    private String next = null;
                    private int start = 0;
                    private int cursor = 0;
                    private int cnt = 0;

                    @Override
                    public boolean hasNext() {
                        if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                            if (limit - cnt == 1) {
                                next = subStringFunc.subString(source, start, sourceLen);
                                start = (cursor = sourceLen + 1);

                                if (omitEmptyStrings && next.isEmpty()) {
                                    next = null;
                                }
                            } else {
                                while (cursor >= 0 && cursor <= sourceLen) {
                                    if (cursor > sourceLen - delimiterLen || (source.charAt(cursor) == delimiterChars[0] && match(cursor))) {
                                        if (cursor > sourceLen - delimiterLen) {
                                            next = subStringFunc.subString(source, start, sourceLen);
                                            start = (cursor = sourceLen + 1);
                                        } else {
                                            next = subStringFunc.subString(source, start, cursor);
                                            start = (cursor += delimiter.length());
                                        }

                                        if (omitEmptyStrings && next.isEmpty()) {
                                            next = null;
                                        }

                                        if (next != null) {
                                            break;
                                        }
                                    } else {
                                        cursor++;
                                    }
                                }
                            }
                        }

                        return next != null;
                    }

                    @Override
                    public String next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final String result = next;
                        next = null;
                        cnt++;
                        return result;
                    }

                    private boolean match(final int cursor) {
                        for (int i = 1; i < delimiterLen; i++) {
                            if (source.charAt(cursor + i) != delimiterChars[i]) {
                                return false;
                            }
                        }

                        return true;
                    }
                };
            });
        }
    }

    /**
     * Creates a Splitter that uses the specified regular expression pattern as a delimiter.
     * The pattern is applied using Java's regular expression engine. Empty strings
     * cannot be matched by the pattern.
     *
     * @param delimiter the Pattern to use as a delimiter for splitting
     * @return a new Splitter instance configured with the specified pattern delimiter
     * @throws IllegalArgumentException if the specified delimiter is null, or if the
     *         pattern can match an empty string
     */
    public static Splitter with(final Pattern delimiter) throws IllegalArgumentException {
        N.checkArgNotNull(delimiter, cs.delimiter);
        N.checkArgument(!delimiter.matcher("").matches(), "Empty string may be matched by pattern: %s", delimiter);

        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new ObjIterator<>() {
                private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                private final int sourceLen = source.length();
                private final Matcher matcher = delimiter.matcher(source);
                private String next = null;
                private int start = 0;
                private int cursor = 0;
                private int cnt = 0;
                private boolean matches = false;

                @Override
                public boolean hasNext() {
                    if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                        if (limit - cnt == 1) {
                            next = subStringFunc.subString(source, start, sourceLen);
                            start = (cursor = sourceLen + 1);

                            if (omitEmptyStrings && next.isEmpty()) {
                                next = null;
                            }
                        } else {
                            while (cursor >= 0 && cursor <= sourceLen) {
                                if (cursor == sourceLen || (matches = matcher.find(start))) {
                                    if (matches) {
                                        next = subStringFunc.subString(source, start, matcher.start());
                                        start = (cursor = matcher.end());
                                        matches = false;
                                    } else {
                                        next = subStringFunc.subString(source, start, sourceLen);
                                        start = (cursor = sourceLen + 1);
                                    }

                                    if (omitEmptyStrings && next.isEmpty()) {
                                        next = null;
                                    }

                                    if (next != null) {
                                        break;
                                    }
                                } else {
                                    cursor++;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String result = next;
                    next = null;
                    cnt++;
                    return result;
                }
            };
        });
    }

    /**
     * Creates a Splitter that uses the specified regular expression as a delimiter.
     * This is a convenience method that compiles the provided regular expression
     * string into a Pattern and then creates a Splitter with it.
     *
     * @param delimiterRegex the regular expression to use as a delimiter for splitting
     * @return a new Splitter instance configured with the compiled pattern delimiter
     * @throws IllegalArgumentException if the specified delimiter regex is null or empty,
     *         or if the resulting pattern can match an empty string
     */
    public static Splitter pattern(final CharSequence delimiterRegex) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiterRegex, cs.delimiterRegex);

        return with(Pattern.compile(delimiterRegex.toString()));
    }

    /**
     * Configures this Splitter to omit empty strings from the results when the specified
     * parameter is true. Empty strings can occur when there are consecutive delimiters
     * or when delimiters appear at the beginning or end of the input.
     *
     * @param omitEmptyStrings true to omit empty strings from results, false to include them
     * @return this Splitter instance for method chaining
     * @deprecated replaced by {@link #omitEmptyStrings()}
     */
    @Deprecated
    public Splitter omitEmptyStrings(final boolean omitEmptyStrings) {
        this.omitEmptyStrings = omitEmptyStrings;

        return this;
    }

    /**
     * Configures this Splitter to omit empty strings from the results.
     * Empty strings can occur when there are consecutive delimiters or when
     * delimiters appear at the beginning or end of the input.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Splitter.with(",").omitEmptyStrings().split("a,,b,")
     * // Returns ["a", "b"] instead of ["a", "", "b", ""]
     * }</pre>
     *
     * @return this Splitter instance for method chaining
     */
    public Splitter omitEmptyStrings() {
        omitEmptyStrings = true;

        return this;
    }

    /**
     * Configures this Splitter to trim leading and trailing spaces from each
     * resulting substring when the specified parameter is true. Only space
     * characters (not all whitespace) are trimmed.
     *
     * @param trim true to trim spaces from results, false to leave them as-is
     * @return this Splitter instance for method chaining
     * @deprecated replaced by {@link #trimResults()}
     */
    @Deprecated
    public Splitter trim(final boolean trim) {
        trimResults = trim;

        return this;
    }

    /**
     * Configures this Splitter to trim leading and trailing spaces from each
     * resulting substring. Only space characters (not all whitespace) are trimmed.
     * For trimming all whitespace characters, use {@link #stripResults()}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Splitter.with(",").trimResults().split("a , b , c")
     * // Returns ["a", "b", "c"] instead of ["a ", " b ", " c"]
     * }</pre>
     *
     * @return this Splitter instance for method chaining
     */
    public Splitter trimResults() {
        trimResults = true;

        return this;
    }

    /**
     * Configures this Splitter to remove leading and trailing whitespace characters
     * from each resulting substring when the specified parameter is true. This method
     * removes all whitespace as defined by {@link Character#isWhitespace(char)},
     * including spaces, tabs, newlines, etc.
     *
     * @param strip true to strip whitespace from results, false to leave them as-is
     * @return this Splitter instance for method chaining
     * @see Character#isWhitespace(char)
     * @deprecated replaced by {@link #stripResults()}
     */
    @Deprecated
    public Splitter strip(final boolean strip) {
        stripResults = strip;

        return this;
    }

    /**
     * Configures this Splitter to remove leading and trailing whitespace characters
     * from each resulting substring. This method removes all whitespace as defined
     * by {@link Character#isWhitespace(char)}, including spaces, tabs, newlines, etc.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Splitter.with(",").stripResults().split("a\t,\nb\t,\tc")
     * // Returns ["a", "b", "c"] with all whitespace removed
     * }</pre>
     *
     * @return this Splitter instance for method chaining
     */
    public Splitter stripResults() {
        stripResults = true;

        return this;
    }

    /**
     * Sets the maximum number of substrings to return when splitting. If the limit
     * is reached, the remainder of the input string will be included in the last
     * substring, without further splitting.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Splitter.with(",").limit(2).split("a,b,c,d")
     * // Returns ["a", "b,c,d"]
     * }</pre>
     *
     * @param limit the maximum number of substrings to return; must be positive
     * @return this Splitter instance for method chaining
     * @throws IllegalArgumentException if the provided limit is not a positive integer
     */
    public Splitter limit(final int limit) throws IllegalArgumentException {
        N.checkArgPositive(limit, cs.limit);

        this.limit = limit;

        return this;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a List of strings.
     *
     * @param source the CharSequence to split; may be null
     * @return a new ArrayList containing the split results; returns an empty list if source is null
     */
    public List<String> split(final CharSequence source) {
        final List<String> result = new ArrayList<>();

        split(source, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results in a Collection created by the provided supplier.
     *
     * @param <C> the type of Collection to return
     * @param source the CharSequence to split; may be null
     * @param supplier a Supplier that creates a new Collection instance to hold the results
     * @return the Collection created by the supplier, populated with the split results
     */
    public <C extends Collection<String>> C split(final CharSequence source, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided mapping function to each resulting substring.
     *
     * @param <T> the type of elements in the result list
     * @param source the CharSequence to split; may be null
     * @param mapper a function to apply to each split string
     * @return a List containing the mapped results
     */
    public <T> List<T> split(final CharSequence source, final Function<? super String, ? extends T> mapper) {
        final List<String> tmp = new ArrayList<>();
        split(source, tmp);

        @SuppressWarnings("rawtypes")
        final List<T> result = (List) tmp;

        for (int i = 0, size = tmp.size(); i < size; i++) {
            result.set(i, mapper.apply(tmp.get(i)));
        }

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * converts each resulting substring to the specified target type.
     *
     * @param <T> the target type for conversion
     * @param source the CharSequence to split; may be null
     * @param targetType the Class representing the type to convert each substring to
     * @return a List containing the converted results
     * @throws IllegalArgumentException if targetType is null
     */
    public <T> List<T> split(final CharSequence source, final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        final Type<T> type = N.typeOf(targetType);

        return split(source, type);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and returns
     * the results in a Collection created by the provided supplier.
     *
     * @param <T> the target type for conversion
     * @param <C> the type of Collection to return
     * @param source the CharSequence to split; may be null
     * @param targetType the Class representing the type to convert each substring to
     * @param supplier a Supplier that creates a new Collection instance to hold the results
     * @return the Collection created by the supplier, populated with the converted results
     */
    public <T, C extends Collection<T>> C split(final CharSequence source, final Class<? extends T> targetType, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * converts each resulting substring to the specified target type using the
     * provided Type instance for conversion.
     *
     * @param <T> the target type for conversion
     * @param source the CharSequence to split; may be null
     * @param targetType the Type instance used for converting strings to the target type
     * @return a List containing the converted results
     * @throws IllegalArgumentException if targetType is null
     */
    public <T> List<T> split(final CharSequence source, final Type<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        final List<T> result = new ArrayList<>();

        split(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type using the
     * provided Type instance, and returns the results in a Collection created
     * by the provided supplier.
     *
     * @param <T> the target type for conversion
     * @param <C> the type of Collection to return
     * @param source the CharSequence to split; may be null
     * @param targetType the Type instance used for converting strings to the target type
     * @param supplier a Supplier that creates a new Collection instance to hold the results
     * @return the Collection created by the supplier, populated with the converted results
     */
    public <T, C extends Collection<T>> C split(final CharSequence source, final Type<? extends T> targetType, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * adds the resulting substrings to the provided output collection.
     *
     * @param <C> the type of Collection to populate
     * @param source the CharSequence to split; may be null
     * @param output the Collection to add the split results to
     * @throws IllegalArgumentException if output is null
     */
    public <C extends Collection<String>> void split(final CharSequence source, final C output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(iter.next());
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and adds
     * the converted values to the provided output collection.
     *
     * @param <T> the target type for conversion
     * @param <C> the type of Collection to populate
     * @param source the CharSequence to split; may be null
     * @param targetType the Class representing the type to convert each substring to
     * @param output the Collection to add the converted results to
     * @throws IllegalArgumentException if targetType or output is null
     */
    public <T, C extends Collection<T>> void split(final CharSequence source, final Class<? extends T> targetType, final C output)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(output, cs.output);

        final Type<T> type = N.typeOf(targetType);

        split(source, type, output);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type using the
     * provided Type instance, and adds the converted values to the provided
     * output collection.
     *
     * @param <T> the target type for conversion
     * @param <C> the type of Collection to populate
     * @param source the CharSequence to split; may be null
     * @param targetType the Type instance used for converting strings to the target type
     * @param output the Collection to add the converted results to
     * @throws IllegalArgumentException if targetType or output is null
     */
    public <T, C extends Collection<T>> void split(final CharSequence source, final Type<? extends T> targetType, final C output)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(targetType.valueOf(iter.next()));
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as an ImmutableList. The returned list cannot be modified.
     *
     * @param source the CharSequence to split; may be null
     * @return an ImmutableList containing the split results
     */
    public ImmutableList<String> splitToImmutableList(final CharSequence source) {
        return ImmutableList.wrap(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and returns
     * the results as an ImmutableList. The returned list cannot be modified.
     *
     * @param <T> the target type for conversion
     * @param source the CharSequence to split; may be null
     * @param targetType the Class representing the type to convert each substring to
     * @return an ImmutableList containing the converted results
     */
    public <T> ImmutableList<T> splitToImmutableList(final CharSequence source, final Class<? extends T> targetType) {
        return ImmutableList.wrap(split(source, targetType));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a String array.
     *
     * @param source the CharSequence to split; may be null
     * @return a String array containing the split results
     */
    public String[] splitToArray(final CharSequence source) {
        final List<String> substrs = split(source);

        return substrs.toArray(new String[0]);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * applies the provided mapping function to each resulting substring, and
     * returns the results as a String array.
     *
     * @param source the CharSequence to split; may be null
     * @param mapper a function to apply to each split string
     * @return a String array containing the mapped results
     */
    public String[] splitToArray(final CharSequence source, final Function<? super String, String> mapper) {
        final List<String> substrs = split(source, mapper);

        return substrs.toArray(new String[0]);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts the results to the specified array type, and returns the array.
     * The array type must be an array class (e.g., String[].class, Integer[].class).
     *
     * @param <T> the array type
     * @param source the CharSequence to split; may be null
     * @param arrayType the Class object representing the desired array type
     * @return an array of the specified type containing the split and converted results
     * @throws IllegalArgumentException if arrayType is null or not an array type
     */
    public <T> T splitToArray(final CharSequence source, final Class<T> arrayType) throws IllegalArgumentException {
        N.checkArgNotNull(arrayType, cs.arrayType);

        final Class<?> eleCls = arrayType.getComponentType();

        final List<String> substrs = split(source);

        if (eleCls.equals(String.class) || eleCls.equals(Object.class)) {
            return (T) substrs.toArray((Object[]) N.newArray(eleCls, substrs.size()));
        } else {
            final Type<?> eleType = N.typeOf(eleCls);
            final Object a = N.newArray(eleCls, substrs.size());

            if (ClassUtil.isPrimitiveType(eleCls)) {
                for (int i = 0, len = substrs.size(); i < len; i++) {
                    Array.set(a, i, eleType.valueOf(substrs.get(i)));
                }
            } else {
                final Object[] objArray = (Object[]) a;

                for (int i = 0, len = substrs.size(); i < len; i++) {
                    objArray[i] = eleType.valueOf(substrs.get(i));
                }
            }

            return (T) a;
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * populates the provided String array with the results. If the array is larger
     * than the number of split results, remaining elements are left unchanged.
     * If the array is smaller than the number of split results, only the first
     * array.length results are stored.
     *
     * @param source the CharSequence to split; may be null
     * @param output the String array to populate with split results
     * @throws IllegalArgumentException if output is null or empty
     */
    public void splitToArray(final CharSequence source, final String[] output) throws IllegalArgumentException {
        N.checkArgNotEmpty(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        for (int i = 0, len = output.length; i < len && iter.hasNext(); i++) {
            output[i] = iter.next();
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a Stream of strings. This allows for lazy evaluation
     * and further stream operations on the split results.
     *
     * @param source the CharSequence to split; may be null
     * @return a Stream containing the split results
     */
    public Stream<String> splitToStream(final CharSequence source) {
        return Stream.of(iterate(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided function to the resulting list of strings. This is
     * useful for transforming or aggregating the split results in a single operation.
     *
     * @param <R> the type of the result
     * @param source the CharSequence to split; may be null
     * @param converter a function that transforms the list of split strings into a result
     * @return the result of applying the converter function to the split results
     */
    public <R> R splitThenApply(final CharSequence source, final Function<? super List<String>, R> converter) {
        return converter.apply(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * passes the resulting list of strings to the provided consumer. This is
     * useful for performing side effects with the split results.
     *
     * @param source the CharSequence to split; may be null
     * @param consumer a consumer that processes the list of split strings
     */
    public void splitThenAccept(final CharSequence source, final Consumer<? super List<String>> consumer) {
        consumer.accept(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided action to each resulting substring. This method processes
     * each substring as it is produced, without creating an intermediate collection.
     *
     * @param source the CharSequence to split; may be null
     * @param action the Consumer to apply to each resulting substring
     */
    @Beta
    public void splitAndForEach(final CharSequence source, final Consumer<? super String> action) {
        iterate(source).forEachRemaining(action);
    }

    /**
     *
     * @param source
     * @return
     */
    ObjIterator<String> iterate(final CharSequence source) {
        return strategy.split(source, omitEmptyStrings, trimResults, stripResults, limit);
    }

    /**
     * A specialized splitter for creating maps from strings. This class splits
     * strings into key-value pairs using two levels of delimiters: one for
     * separating entries and another for separating keys from values within
     * each entry.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, String> map = MapSplitter.with(",", "=")
     *     .split("a=1,b=2,c=3");
     * // Returns {a=1, b=2, c=3}
     * }</pre>
     */
    public static final class MapSplitter {

        /** The entry splitter. */
        private final Splitter entrySplitter;

        /** The key value splitter. */
        private final Splitter keyValueSplitter;

        /**
         * Instantiates a new map splitter.
         *
         * @param entrySplitter
         * @param keyValueSplitter
         */
        MapSplitter(final Splitter entrySplitter, final Splitter keyValueSplitter) {
            this.entrySplitter = entrySplitter;
            this.keyValueSplitter = keyValueSplitter;
        }

        /**
         * Creates a MapSplitter with the default entry and key-value delimiters.
         * The default entry delimiter is ", " (comma followed by space) and the
         * default key-value delimiter is "=" (equals sign).
         *
         * @return a new MapSplitter instance with default delimiters
         * @see Joiner#DEFAULT_DELIMITER
         * @see Joiner#DEFAULT_KEY_VALUE_DELIMITER
         * @see Joiner#defauLt()
         */
        @Beta
        public static MapSplitter defauLt() {
            return with(Joiner.DEFAULT_DELIMITER, Joiner.DEFAULT_KEY_VALUE_DELIMITER);
        }

        /**
         * Creates a MapSplitter with the specified entry and key-value delimiters.
         * The entry delimiter separates different key-value pairs, while the
         * key-value delimiter separates keys from values within each pair.
         *
         * @param entryDelimiter the delimiter that separates entries (key-value pairs)
         * @param keyValueDelimiter the delimiter that separates keys from values
         * @return a new MapSplitter instance with the specified delimiters
         * @throws IllegalArgumentException if either delimiter is null or empty
         * @see Splitter#with(CharSequence)
         */
        public static MapSplitter with(final CharSequence entryDelimiter, final CharSequence keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * Creates a MapSplitter with the specified entry and key-value delimiter patterns.
         * The patterns are used as regular expressions for splitting.
         *
         * @param entryDelimiter the Pattern that separates entries (key-value pairs)
         * @param keyValueDelimiter the Pattern that separates keys from values
         * @return a new MapSplitter instance with the specified pattern delimiters
         * @throws IllegalArgumentException if either delimiter is null, or if either
         *         pattern can match an empty string
         * @see Splitter#with(Pattern)
         */
        public static MapSplitter with(final Pattern entryDelimiter, final Pattern keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * Creates a MapSplitter with the specified entry and key-value delimiter
         * regular expressions. The regular expressions are compiled into Patterns
         * and used for splitting.
         *
         * @param entryDelimiterRegex the regular expression that separates entries
         * @param keyValueDelimiterRegex the regular expression that separates keys from values
         * @return a new MapSplitter instance with the compiled pattern delimiters
         * @throws IllegalArgumentException if either regex is null or empty, or if
         *         the compiled patterns can match an empty string
         * @see Splitter#pattern(CharSequence)
         */
        public static MapSplitter pattern(final CharSequence entryDelimiterRegex, final CharSequence keyValueDelimiterRegex) throws IllegalArgumentException {
            return new MapSplitter(Splitter.pattern(entryDelimiterRegex), Splitter.pattern(keyValueDelimiterRegex));
        }

        /**
         * Configures this MapSplitter to omit entries with empty values when the
         * specified parameter is true. This applies to the entries splitting phase.
         *
         * @param omitEmptyStrings true to omit entries with empty keys and values, false to include them
         * @return this MapSplitter instance for method chaining
         * @deprecated replaced by {@link #omitEmptyStrings()}
         */
        @Deprecated
        public MapSplitter omitEmptyStrings(final boolean omitEmptyStrings) {
            entrySplitter.omitEmptyStrings(omitEmptyStrings);

            return this;
        }

        /**
         * Configures this MapSplitter to omit entries with empty values.
         * This applies to the entries splitting phase, filtering out entries
         *
         * @return this MapSplitter instance for method chaining
         */
        public MapSplitter omitEmptyStrings() {
            entrySplitter.omitEmptyStrings();

            return this;
        }

        /**
         * Configures this MapSplitter to trim spaces from both entries and
         * key-value pairs when the specified parameter is true.
         *
         * @param trim true to trim spaces, false to leave them as-is
         * @return this MapSplitter instance for method chaining
         * @deprecated replaced by {@link #trimResults()}
         */
        @Deprecated
        public MapSplitter trim(final boolean trim) {
            entrySplitter.trim(trim);
            keyValueSplitter.trim(trim);

            return this;
        }

        /**
         * Configures this MapSplitter to trim leading and trailing spaces from
         * both entries and key-value pairs. This ensures clean keys and values
         * without surrounding spaces.
         *
         * @return this MapSplitter instance for method chaining
         */
        public MapSplitter trimResults() {
            entrySplitter.trimResults();
            keyValueSplitter.trimResults();

            return this;
        }

        /**
         * Configures this MapSplitter to strip all leading and trailing whitespace
         * characters from both entries and key-value pairs when the specified
         * parameter is true.
         *
         * @param strip true to strip whitespace, false to leave it as-is
         * @return this MapSplitter instance for method chaining
         * @see Character#isWhitespace(char)
         * @deprecated replaced by {@link #stripResults()}
         */
        @Deprecated
        public MapSplitter strip(final boolean strip) {
            entrySplitter.strip(strip);
            keyValueSplitter.strip(strip);

            return this;
        }

        /**
         * Configures this MapSplitter to strip all leading and trailing whitespace
         * characters from both entries and key-value pairs. This removes all forms
         * of whitespace as defined by {@link Character#isWhitespace(char)}.
         *
         * @return this MapSplitter instance for method chaining
         */
        public MapSplitter stripResults() {
            entrySplitter.stripResults();
            keyValueSplitter.stripResults();

            return this;
        }

        /**
         * Sets the maximum number of entries to parse when splitting. If the limit
         * is reached, remaining text is ignored.
         *
         * @param limit the maximum number of entries to parse; must be positive
         * @return this MapSplitter instance for method chaining
         * @throws IllegalArgumentException if the provided limit is not positive
         */
        public MapSplitter limit(final int limit) throws IllegalArgumentException {
            N.checkArgPositive(limit, cs.limit);

            entrySplitter.limit(limit);

            return this;
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * using this MapSplitter's configuration. The order of entries is preserved
         * in the returned LinkedHashMap.
         *
         * @param source the CharSequence to split into a map; may be null
         * @return a LinkedHashMap containing the parsed key-value pairs
         */
        public Map<String, String> split(final CharSequence source) {
            final LinkedHashMap<String, String> result = new LinkedHashMap<>();

            split(source, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * using this MapSplitter's configuration and returns the results in a Map
         * created by the provided supplier.
         *
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be null
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the parsed key-value pairs
         */
        public <M extends Map<String, String>> M split(final CharSequence source, final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types using this MapSplitter's configuration.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @return a LinkedHashMap containing the parsed and converted key-value pairs
         * @throws IllegalArgumentException if keyType or valueType is null
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Class<K> keyType, final Class<V> valueType) throws IllegalArgumentException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            return split(source, typeOfKey, typeOfValue);
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types using the provided Type instances for conversion.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Type instance used for converting strings to keys
         * @param valueType the Type instance used for converting strings to values
         * @return a LinkedHashMap containing the parsed and converted key-value pairs
         * @throws IllegalArgumentException if keyType or valueType is null
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Type<K> keyType, final Type<V> valueType) throws IllegalArgumentException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);

            final LinkedHashMap<K, V> result = new LinkedHashMap<>();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types, and returns the results in a Map created by the
         * provided supplier.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the converted key-value pairs
         */
        public <K, V, M extends Map<K, V>> M split(final CharSequence source, final Class<K> keyType, final Class<V> valueType,
                final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * using the provided Type instances, and returns the results in a Map created
         * by the provided supplier.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Type instance used for converting strings to keys
         * @param valueType the Type instance used for converting strings to values
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the converted key-value pairs
         */
        public <K, V, M extends Map<K, V>> M split(final CharSequence source, final Type<K> keyType, final Type<V> valueType,
                final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into string key-value pairs and adds
         * them to the provided output map.
         *
         * @param <M> the type of Map to populate
         * @param source the CharSequence to split into a map; may be null
         * @param output the Map to add the parsed key-value pairs to
         * @throws IllegalArgumentException if output is null, or if any entry string
         *         cannot be properly parsed into a key-value pair
         */
        public <M extends Map<String, String>> void split(final CharSequence source, final M output) throws IllegalArgumentException {
            N.checkArgNotNull(output, cs.output);

            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (keyValueIter.hasNext()) {
                    key = keyValueIter.next();

                    if (keyValueIter.hasNext()) {
                        value = keyValueIter.next();
                    } else {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    }

                    if (keyValueIter.hasNext()) {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    } else {
                        output.put(key, value);
                    }
                }
            }
        }

        /**
         * Splits the specified CharSequence into key-value pairs, converts them to
         * the specified types, and adds them to the provided output map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to populate
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @param output the Map to add the converted key-value pairs to
         * @throws IllegalArgumentException if keyType, valueType, or output is null,
         *         or if any entry string cannot be properly parsed into a key-value pair
         */
        public <K, V, M extends Map<K, V>> void split(final CharSequence source, final Class<K> keyType, final Class<V> valueType, final M output)
                throws IllegalArgumentException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(output, cs.output);

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            split(source, typeOfKey, typeOfValue, output);
        }

        /**
         * Splits the specified CharSequence into key-value pairs, converts them using
         * the provided Type instances, and adds them to the provided output map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to populate
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Type instance used for converting strings to keys
         * @param valueType the Type instance used for converting strings to values
         * @param output the Map to add the converted key-value pairs to
         * @throws IllegalArgumentException if keyType, valueType, or output is null,
         *         or if any entry string cannot be properly parsed into a key-value pair
         */
        public <K, V, M extends Map<K, V>> void split(final CharSequence source, final Type<K> keyType, final Type<V> valueType, final M output)
                throws IllegalArgumentException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(output, cs.output);

            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (keyValueIter.hasNext()) {
                    key = keyValueIter.next();

                    if (keyValueIter.hasNext()) {
                        value = keyValueIter.next();
                    } else {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    }

                    if (keyValueIter.hasNext()) {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    } else {
                        output.put(keyType.valueOf(key), valueType.valueOf(value));
                    }
                }
            }
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * and returns the results as an ImmutableMap. The returned map cannot be modified.
         *
         * @param source the CharSequence to split into a map; may be null
         * @return an ImmutableMap containing the parsed key-value pairs
         */
        public ImmutableMap<String, String> splitToImmutableMap(final CharSequence source) {
            return ImmutableMap.wrap(split(source));
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types, and returns the results as an ImmutableMap.
         * The returned map cannot be modified.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be null
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @return an ImmutableMap containing the parsed and converted key-value pairs
         */
        public <K, V> ImmutableMap<K, V> splitToImmutableMap(final CharSequence source, final Class<K> keyType, final Class<V> valueType) {
            return ImmutableMap.wrap(split(source, keyType, valueType));
        }

        /**
         * Splits the specified CharSequence into a Stream of Map.Entry objects.
         * Each entry represents a parsed key-value pair. This allows for lazy
         * evaluation and further stream operations on the entries.
         *
         * @param source the CharSequence to split into entries; may be null
         * @return a Stream of Map.Entry objects containing the parsed key-value pairs
         * @throws IllegalArgumentException if any entry string cannot be properly
         *         parsed into a key-value pair during iteration
         */
        public Stream<Map.Entry<String, String>> splitToStream(final CharSequence source) {
            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            return Stream.of(new ObjIteratorEx<>() {
                private final ObjIterator<String> iter = entrySplitter.iterate(source);
                private ObjIterator<String> keyValueIter = null;
                private String entryString = null;
                private String key = null;
                private String value = null;
                private Map.Entry<String, String> next;

                @Override
                public boolean hasNext() {
                    if (next == null) {
                        while (iter.hasNext()) {
                            entryString = iter.next();
                            keyValueIter = keyValueSplitter.iterate(entryString);

                            if (keyValueIter.hasNext()) {
                                key = keyValueIter.next();

                                if (keyValueIter.hasNext()) {
                                    value = keyValueIter.next();
                                } else {
                                    throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                                }

                                if (keyValueIter.hasNext()) {
                                    throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                                } else {
                                    next = new ImmutableEntry<>(key, value);
                                    break;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public Map.Entry<String, String> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Map.Entry<String, String> result = next;
                    next = null;
                    return result;
                }
            });
        }

        /**
         * Splits the specified CharSequence into an EntryStream of string key-value
         * pairs. EntryStream provides specialized operations for working with
         * key-value pairs.
         *
         * @param source the CharSequence to split into entries; may be null
         * @return an EntryStream containing the parsed key-value pairs
         */
        public EntryStream<String, String> splitToEntryStream(final CharSequence source) {
            //noinspection resource
            return splitToStream(source).mapToEntry(Fn.identity());
        }

        /**
         * Splits the specified CharSequence into a map and applies the provided
         * function to transform the resulting map. This is useful for converting
         * or aggregating the map in a single operation.
         *
         * @param <T> the type of the result
         * @param source the CharSequence to split into a map; may be null
         * @param converter a function that transforms the parsed map into a result
         * @return the result of applying the converter function to the parsed map
         */
        public <T> T splitThenApply(final CharSequence source, final Function<? super Map<String, String>, T> converter) {
            return converter.apply(split(source));
        }

        /**
         * Splits the specified CharSequence into a map and passes it to the provided
         * consumer. This is useful for performing side effects with the parsed map.
         *
         * @param source the CharSequence to split into a map; may be null
         * @param consumer a consumer that processes the parsed map
         */
        public void splitThenAccept(final CharSequence source, final Consumer<? super Map<String, String>> consumer) {
            consumer.accept(split(source));
        }
    }

    /**
     * The Interface Strategy.
     */
    interface Strategy {

        /**
         *
         * @param toSplit
         * @param omitEmptyStrings
         * @param trim
         * @param strip
         * @param limit
         * @return
         */
        ObjIterator<String> split(CharSequence toSplit, boolean omitEmptyStrings, boolean trim, final boolean strip, int limit);
    }

    /**
     * The Interface SubStringFunc.
     */
    interface SubStringFunc {

        /**
         *
         * @param source
         * @param start
         * @param end
         * @return
         */
        String subString(CharSequence source, int start, int end);
    }
}