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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.EscapeUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.SK;

/**
 * The abstract base class for array types in the type system.
 * <p>
 * This class provides common functionality for handling primitive and object array types,
 * including serialization, conversion, and collection interoperability operations.
 * Concrete subclasses handle specific array element types
 * (e.g., {@code int[]}, {@code String[]}, {@code Integer[]}, {@code Object[]}).
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
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractArrayType(final String typeName) throws IllegalArgumentException {
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
     * @return the new collection populated with the array elements, or {@code null} if {@code array} is {@code null}
     * @throws IllegalArgumentException if {@code array} is non-null and {@code collClass} is {@code null}, does not implement
     *         {@link Collection}, or has no supported factory or construction path
     * @throws RuntimeException if the selected collection factory or reflective constructor fails while creating the collection
     */
    @MayReturnNull
    @Override
    public <E> Collection<E> arrayToCollection(final T array, final Class<?> collClass) throws IllegalArgumentException, RuntimeException {
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
     * the surrounding brackets are removed first. The remaining content is split on commas,
     * honoring quoted regions so that a comma inside a single- or double-quoted element is not a
     * separator. Structural whitespace surrounding the array and its elements is ignored, and
     * whitespace-only content produces an empty array. A character consisting of whitespace must
     * therefore be quoted (for example, {@code [' ']}).
     * Empty unquoted elements are rejected. Use {@code stringOf}, rather than a human-readable
     * {@code appendTo} rendering, when characters include commas, quotes, or whitespace.
     * </p>
     *
     * @param str the array string to split, optionally enclosed in {@code []}; must not be {@code null}
     * @return the array of element substrings; quote characters that delimit an individual element
     *         are preserved in that element
     * @throws IllegalArgumentException if {@code str} is {@code null} or a comma-delimited element contains only whitespace
     */
    protected static String[] split(final String str) throws IllegalArgumentException {
        N.checkArgNotNull(str, cs.str);

        final String input = str.strip();
        final String source;

        if (input.length() >= 2 && input.charAt(0) == SK._BRACKET_L && input.charAt(input.length() - 1) == SK._BRACKET_R) {
            source = input.substring(1, input.length() - 1).strip();
        } else {
            source = input;
        }

        // Always use the comma itself: choosing a delimiter from the first separator loses mixed-space elements.
        return source.isEmpty() ? N.EMPTY_STRING_ARRAY : splitElements(source, String.valueOf(SK._COMMA));
    }

    /**
     * Parses the ordinary decimal form of a built-in numeric array without materializing a token list.
     * <p>
     * The admitted form is a literal {@code [} ... {@code ]} pair around ASCII decimal elements separated by
     * commas, with optional ASCII spaces around each element. Only an exact captured built-in handler may take
     * this path: no application conversion is attempted, and fallback must never replay application callbacks.
     * Any other input, and any element the conversion rejects, yields {@code null} so that the caller's original
     * parser produces the result or the failure, with its diagnostics and validation precedence.
     * </p>
     *
     * @param str the non-blank array text
     * @param elementType the array handler's captured element type
     * @param componentType the component class of the array to create
     * @return the parsed array of {@code componentType}, or {@code null} if the caller must use its original parser
     */
    static Object parseNumericArray(final String str, final Type<?> elementType, final Class<?> componentType) {
        final Class<?> handler = elementType.getClass();
        final int kind;

        if (componentType == int.class && handler == PrimitiveIntType.class) {
            kind = 0;
        } else if (componentType == long.class && handler == PrimitiveLongType.class) {
            kind = 1;
        } else if (componentType == byte.class && handler == PrimitiveByteType.class) {
            kind = 2;
        } else if (componentType == short.class && handler == PrimitiveShortType.class) {
            kind = 3;
        } else if (componentType == float.class && handler == PrimitiveFloatType.class) {
            kind = 4;
        } else if (componentType == double.class && handler == PrimitiveDoubleType.class) {
            kind = 5;
        } else if (componentType == Integer.class && handler == IntegerType.class) {
            kind = 6;
        } else if (componentType == Long.class && handler == LongType.class) {
            kind = 7;
        } else if (componentType == Byte.class && handler == ByteType.class) {
            kind = 8;
        } else if (componentType == Short.class && handler == ShortType.class) {
            kind = 9;
        } else if (componentType == Float.class && handler == FloatType.class) {
            kind = 10;
        } else if (componentType == Double.class && handler == DoubleType.class) {
            kind = 11;
        } else {
            return null;
        }

        final int limit = str.length() - 1;

        if (limit < 2 || str.charAt(0) != '[' || str.charAt(limit) != ']') {
            return null;
        }

        final boolean floatingPoint = kind % 6 >= 4;

        // A null, quoted or suffixed element is the usual reason for falling back, and a trailing one would
        // otherwise be found only after the scan below has read every element before it. Checking the last
        // element first rejects that input in time proportional to the element; it only returns earlier for
        // input the complete scan rejects as well.
        int pos = str.lastIndexOf(',', limit - 1) + 1;

        if (pos == 0) {
            pos = 1;
        }

        while (pos < limit && str.charAt(pos) == ' ') {
            pos++;
        }

        pos = decimalEnd(str, pos, limit, floatingPoint);

        if (pos < 0) {
            return null;
        }

        while (pos < limit && str.charAt(pos) == ' ') {
            pos++;
        }

        if (pos != limit) {
            return null;
        }

        int count = 0;
        pos = 1;

        // Complete lexical validation precedes every conversion, just as split does. In particular,
        // a missing final slot must win over an earlier malformed or overflowing numeric value.
        while (pos < limit) {
            while (pos < limit && str.charAt(pos) == ' ') {
                pos++;
            }

            pos = decimalEnd(str, pos, limit, floatingPoint);

            if (pos < 0) {
                return null;
            }

            while (pos < limit && str.charAt(pos) == ' ') {
                pos++;
            }

            count++;

            if (pos == limit) {
                break;
            }

            if (str.charAt(pos++) != ',' || pos == limit) {
                return null;
            }
        }

        final Object result = Array.newInstance(componentType, count);
        pos = 1;

        try {
            for (int i = 0; i < count; i++) {
                while (str.charAt(pos) == ' ') {
                    pos++;
                }

                final int from = pos;

                // The first pass has already checked this token's entire grammar.
                while (pos < limit && str.charAt(pos) != ',' && str.charAt(pos) != ' ') {
                    pos++;
                }

                switch (kind) {
                    case 0, 6: {
                        final long value = Long.parseLong(str, from, pos, 10);

                        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                            return null;
                        }

                        if (kind == 0) {
                            ((int[]) result)[i] = (int) value;
                        } else {
                            ((Integer[]) result)[i] = (int) value;
                        }

                        break;
                    }

                    case 1, 7: {
                        final long value = Long.parseLong(str, from, pos, 10);

                        if (kind == 1) {
                            ((long[]) result)[i] = value;
                        } else {
                            ((Long[]) result)[i] = value;
                        }

                        break;
                    }

                    case 2, 8: {
                        final long value = Long.parseLong(str, from, pos, 10);

                        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                            return null;
                        }

                        if (kind == 2) {
                            ((byte[]) result)[i] = (byte) value;
                        } else {
                            ((Byte[]) result)[i] = (byte) value;
                        }

                        break;
                    }

                    case 3, 9: {
                        final long value = Long.parseLong(str, from, pos, 10);

                        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                            return null;
                        }

                        if (kind == 3) {
                            ((short[]) result)[i] = (short) value;
                        } else {
                            ((Short[]) result)[i] = (short) value;
                        }

                        break;
                    }

                    case 4:
                        ((float[]) result)[i] = Float.parseFloat(str.substring(from, pos));
                        break;

                    case 5:
                        ((double[]) result)[i] = Double.parseDouble(str.substring(from, pos));
                        break;

                    case 10:
                        ((Float[]) result)[i] = Float.valueOf(str.substring(from, pos));
                        break;

                    case 11:
                        ((Double[]) result)[i] = Double.valueOf(str.substring(from, pos));
                        break;

                    default:
                        throw new AssertionError("Unexpected numeric array kind: " + kind);
                }

                while (pos < limit && str.charAt(pos) == ' ') {
                    pos++;
                }

                pos++;
            }
        } catch (final NumberFormatException e) {
            // The existing handlers distinguish overflow from malformed input and retain their diagnostics.
            return null;
        }

        return result;
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
     * @throws IllegalArgumentException if an unquoted element before, between, or after delimiters is empty or contains only whitespace
     */
    private static String[] splitElements(final String str, final String delimiter) throws IllegalArgumentException {
        final List<String> result = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        char quoteChar = 0;
        boolean escaped = false;

        for (int i = 0, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            if (quoteChar == 0) {
                if (startsWithDelimiter(str, i, delimiter)) {
                    final String element = sb.toString().strip();

                    if (element.isEmpty()) {
                        throw new IllegalArgumentException("Missing array element at index " + result.size());
                    }

                    result.add(element);
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

        final String element = sb.toString().strip();

        if (element.isEmpty()) {
            throw new IllegalArgumentException("Missing array element at index " + result.size());
        }

        result.add(element);

        return result.toArray(new String[result.size()]);
    }

    /**
     * Reads one element of an array's JSON text form for the element type's own codec: a bare {@code null}
     * token maps to {@code null} (the element type's default value then applies), a quoted element loses its
     * surrounding quotes and has its JSON escapes decoded exactly once (an empty quoted element stays quoted,
     * so it fails as malformed rather than becoming the default), and any other token is returned as is.
     * {@link #split(String)} preserves an element's quotes, so this normalization is what lets compact JSON text
     * such as {@code ["1","-2"]} or {@code [null,"a",null]} read back through {@code Type.valueOf}.
     *
     * @param element one element substring from {@link #split(String)}
     * @return the element content for the element type's codec, or {@code null} for a bare JSON null token
     * @see #jsonElementContent(String, boolean)
     */
    protected static String jsonElementContent(final String element) {
        return jsonElementContent(element, false);
    }

    /**
     * Reads one element of an array's JSON text form for the element type's own codec, as
     * {@link #jsonElementContent(String)} does, with the handling of an empty quoted element ({@code ""} or
     * {@code ''}) chosen by the caller: the numeric codecs keep it quoted so it fails as a malformed number, whereas
     * the boolean codecs read it as its empty content so the element type's default applies.
     *
     * @param element one element substring from {@link #split(String)}
     * @param emptyQuotedIsDefault {@code true} to return the empty content of an empty quoted element (the element
     *        type's default then applies); {@code false} to return the element still quoted so it fails as malformed
     * @return the element content for the element type's codec, or {@code null} for a bare JSON null token
     */
    protected static String jsonElementContent(final String element, final boolean emptyQuotedIsDefault) {
        final int len = element.length();

        if (len == 4 && element.equals(NULL_STRING)) {
            return null;
        }

        // The PrimitiveLongArrayType guard: unless the caller asks otherwise, an empty quoted element is left quoted so
        // it fails as a malformed number instead of becoming the element type's default.
        if (len >= (emptyQuotedIsDefault ? 2 : 3)) {
            final char quoteChar = element.charAt(0);

            if ((quoteChar == SK._DOUBLE_QUOTE || quoteChar == SK._SINGLE_QUOTE) && element.charAt(len - 1) == quoteChar) {
                final String content = element.substring(1, len - 1);

                // Unescaping only rewrites backslash sequences, so content without a backslash is already its own
                // unescaped form; the guard spares the translator's writer and scan for the common escape-free element
                // (code review 2026-10-05).
                return content.indexOf('\\') < 0 ? content : EscapeUtil.unescapeEcmaScript(content);
            }
        }

        return element;
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
