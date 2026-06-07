/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * The abstract base class for array types in the type system.
 * <p>
 * This class provides common functionality for handling object array types,
 * including serialization, conversion, and collection interoperability operations.
 * Concrete subclasses handle specific array element types
 * (e.g., {@code String[]}, {@code Integer[]}, {@code Object[]}).
 * </p>
 *
 * @param <T> the array type this handler represents (e.g., {@code String[]}, {@code Integer[]})
 * @see AbstractPrimitiveArrayType
 */
public abstract class AbstractArrayType<T> extends AbstractType<T> {

    /**
     * Constructs a new {@code AbstractArrayType} with the specified type name.
     *
     * @param typeName the name of the array type (e.g., "int[]", "String[]", "Object[]")
     */
    protected AbstractArrayType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents an array type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isArray() {
        return true;
    }

    /**
     * Returns the serialization type for this array type.
     * Returns {@link SerializationType#SERIALIZABLE} if the array type is serializable;
     * otherwise returns {@link SerializationType#ARRAY}.
     *
     * @return the {@link SerializationType} for this array type
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.ARRAY;
    }

    /**
     * Converts the specified array to a new collection of the given collection class.
     * <p>
     * A new collection of {@code collClass} is created with an initial capacity equal to
     * the array length, and each element is added in iteration order via
     * {@link #arrayToCollection(Object, Collection)}. If the input array is {@code null},
     * {@code null} is returned.
     * </p>
     *
     * @param <E> the element type of the resulting collection
     * @param array the array to convert; may be {@code null}
     * @param collClass the class of the collection to create
     *                  (e.g., {@code ArrayList.class}, {@code HashSet.class})
     * @return a new collection containing all elements from the array,
     *         or {@code null} if {@code array} is {@code null}
     * @throws IllegalArgumentException if {@code collClass} cannot be instantiated
     */
    @Override
    public <E> Collection<E> arrayToCollection(final T array, final Class<?> collClass) {
        if (array == null) {
            return null; // NOSONAR
        }

        @SuppressWarnings("rawtypes")
        final Collection<E> result = N.newCollection((Class<Collection>) collClass, Array.getLength(array));

        arrayToCollection(array, result);

        return result;
    }

    /**
     * Splits the string representation of an array into its element substrings.
     * <p>
     * If the string is wrapped in a matching pair of square brackets ({@code '['} ... {@code ']'}),
     * the surrounding brackets are removed first. The remaining content is then split on the
     * {@code ELEMENT_SEPARATOR}, honoring quoted regions so that a delimiter occurring inside a
     * single- or double-quoted element is not treated as a separator. As a fallback for
     * comma-delimited representations, if splitting on {@code ELEMENT_SEPARATOR} yields a single
     * element but the content contains a comma, the content is re-split on a comma instead.
     * </p>
     *
     * @param str the array string to split, optionally enclosed in {@code []}; must not be {@code null}
     * @return the array of element substrings; quote characters that delimit an individual element
     *         are preserved in that element
     * @throws NullPointerException if {@code str} is {@code null}
     */
    protected static String[] split(final String str) {
        final String source;

        if (str.length() >= 2 && str.charAt(0) == SK._BRACKET_L && str.charAt(str.length() - 1) == SK._BRACKET_R) {
            source = str.substring(1, str.length() - 1);
        } else {
            source = str;
        }

        final String[] elements = splitElements(source, ELEMENT_SEPARATOR);

        return (elements.length == 1 && source.indexOf(SK._COMMA) >= 0) ? splitElements(source, String.valueOf(SK._COMMA)) : elements;
    }

    /**
     * Splits the given string on the specified delimiter, ignoring any delimiter that occurs
     * inside a quoted element.
     * <p>
     * An element is treated as quoted when a single-quote ({@code '}) or double-quote ({@code "})
     * character appears as its first non-whitespace character and has a matching closing quote
     * followed by the delimiter or the end of the string (see
     * {@link #isQuotedElementStart(String, int, StringBuilder, String)}). Within a quoted region the
     * delimiter and the opposite quote character are treated literally, and a backslash ({@code \})
     * escapes the following character. The surrounding quote characters are retained in the returned
     * substrings.
     * </p>
     *
     * @param str the string to split
     * @param delimiter the delimiter to split on
     * @return the array of element substrings separated by unquoted occurrences of {@code delimiter}
     */
    private static String[] splitElements(final String str, final String delimiter) {
        final List<String> result = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        char quoteChar = 0;
        boolean escaped = false;

        for (int i = 0, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            if (quoteChar == 0) {
                if (startsWithDelimiter(str, i, delimiter)) {
                    result.add(sb.toString());
                    sb.setLength(0);
                    i += delimiter.length() - 1;
                } else {
                    sb.append(ch);

                    if ((ch == '\'' || ch == '"') && isQuotedElementStart(str, i, sb, delimiter)) {
                        quoteChar = ch;
                    }
                }
            } else {
                sb.append(ch);

                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == quoteChar) {
                    quoteChar = 0;
                }
            }
        }

        result.add(sb.toString());

        return result.toArray(new String[result.size()]);
    }

    /**
     * Tests whether the specified delimiter occurs in {@code str} starting exactly at {@code index}.
     *
     * @param str the string to test
     * @param index the position at which to look for the delimiter
     * @param delimiter the delimiter to match
     * @return {@code true} if {@code str} contains {@code delimiter} starting at {@code index};
     *         {@code false} otherwise, including when the delimiter would extend past the end of {@code str}
     */
    private static boolean startsWithDelimiter(final String str, final int index, final String delimiter) {
        final int delimiterLength = delimiter.length();

        return index + delimiterLength <= str.length() && str.regionMatches(index, delimiter, 0, delimiterLength);
    }

    /**
     * Determines whether the quote character at {@code quoteIndex} opens a quoted element.
     * <p>
     * The quote is treated as an element-opening quote only when both of the following hold: every
     * character accumulated for the current element before this quote (the characters in {@code sb}
     * up to, but not including, the just-appended quote) is whitespace, and a matching unescaped
     * closing quote exists later such that the next non-whitespace position after it is either the
     * end of the string or the start of {@code delimiter}. This prevents a quote character that
     * appears in the middle of an element (for example an apostrophe within an unquoted word) from
     * being misinterpreted as the start of a quoted element.
     * </p>
     *
     * @param str the full string being split
     * @param quoteIndex the index in {@code str} of the candidate opening quote character
     * @param sb the buffer holding the characters accumulated for the current element,
     *           ending with the candidate quote character
     * @param delimiter the element delimiter being used for the current split
     * @return {@code true} if the quote at {@code quoteIndex} begins a quoted element; {@code false} otherwise
     */
    private static boolean isQuotedElementStart(final String str, final int quoteIndex, final StringBuilder sb, final String delimiter) {
        for (int i = 0, len = sb.length() - 1; i < len; i++) {
            if (!Character.isWhitespace(sb.charAt(i))) {
                return false;
            }
        }

        final char quoteChar = str.charAt(quoteIndex);
        boolean escaped = false;

        for (int i = quoteIndex + 1, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            if (escaped) {
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == quoteChar) {
                int next = i + 1;

                while (next < len && Character.isWhitespace(str.charAt(next))) {
                    next++;
                }

                return next == len || startsWithDelimiter(str, next, delimiter);
            }
        }

        return false;
    }
}
