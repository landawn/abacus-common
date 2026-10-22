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
     * The powers of ten from 1e0 to 1e22, every one of them exactly representable as a {@code double}.
     */
    private static final double[] EXACT_DOUBLE_POWERS_OF_TEN = { 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16,
            1e17, 1e18, 1e19, 1e20, 1e21, 1e22 };

    /**
     * The powers of ten from 1e0 to 1e10, every one of them exactly representable as a {@code float}.
     */
    private static final float[] EXACT_FLOAT_POWERS_OF_TEN = { 1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f };

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
     * The admitted form is a {@code [} ... {@code ]} pair around ASCII decimal elements separated by commas, with
     * optional structural whitespace around the array and each element, as in {@link #split(String)}. A decimal
     * element may be enclosed in matching single or double quotes; escapes and whitespace inside quotes are left
     * to the original parser. An element may also be the bare {@code null} token,
     * read as the original parser reads it: a {@code null} element of a wrapper array and the default (zero) of a
     * primitive array. Only an exact captured built-in handler may take this path: no application conversion is
     * attempted, and fallback must never replay application callbacks. Any other input, and any element the
     * conversion rejects, yields {@code null} so that the caller's original parser produces the result or the
     * failure, with its diagnostics and validation precedence.
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

        final int start = skipArrayWhitespace(str, 0, str.length());
        int limit = str.length() - 1;

        while (limit > start && Character.isWhitespace(str.charAt(limit))) {
            limit--;
        }

        if (limit - start < 2 || str.charAt(start) != '[' || str.charAt(limit) != ']') {
            return null;
        }

        final boolean floatingPoint = kind % 6 >= 4;
        final int first = skipArrayWhitespace(str, start + 1, limit);

        // An escaped or suffixed element is a common reason for falling back, and a trailing one would otherwise be
        // found only after the pass below has converted every element before it. Checking the last element first
        // rejects that input in time proportional to the element; it only returns earlier for input the complete
        // pass rejects as well.
        int pos = skipArrayWhitespace(str, Math.max(start + 1, str.lastIndexOf(',', limit - 1) + 1), limit);
        pos = numericElementEnd(str, pos, limit, floatingPoint);

        if (pos < 0 || skipArrayWhitespace(str, pos, limit) != limit) {
            return null;
        }

        // A quoted first element with escapes or other unsupported content can be rejected before allocating the
        // result. Ordinary quoted decimals are read directly, without copying or unescaping their content.
        if (str.charAt(first) == '"' || str.charAt(first) == '\'') {
            pos = numericElementEnd(str, first, limit, floatingPoint);

            if (pos < 0) {
                return null;
            }

            pos = skipArrayWhitespace(str, pos, limit);

            if (pos != limit && str.charAt(pos) != ',') {
                return null;
            }
        }

        // Every comma separates two elements of any input this method accepts. Input with a comma anywhere else is
        // rejected below, and the array is then simply discarded. Rejecting an empty element here bounds that array by
        // the size a valid input of the same length needs: a run of commas is not worth an allocation.
        int count = 1;

        for (int previous = start, i = str.indexOf(',', first); i >= 0 && i < limit; previous = i, i = str.indexOf(',', i + 1)) {
            if (i - previous < 2) {
                return null;
            }

            count++;
        }

        // Bulk searches let quote handling stay invariant throughout the loop for ordinary unquoted arrays.
        final boolean quotedElements = str.indexOf('"', first) >= 0 || str.indexOf('\'', first) >= 0;
        final Object result = Array.newInstance(componentType, count);
        pos = first;

        // One pass reads and converts each element. Unlike split, it does not check the whole input before the
        // first conversion: the order cannot matter, because the result is returned only when every element was
        // read. Input with a malformed, missing or out-of-range element anywhere is left to the original parser,
        // which reproduces its result or its failure, including split's precedence (a missing final slot wins over
        // an earlier overflowing value).
        for (int i = 0; i < count; i++) {
            pos = skipArrayWhitespace(str, pos, limit);
            final char quote = quotedElements && pos < limit && (str.charAt(pos) == '"' || str.charAt(pos) == '\'') ? str.charAt(pos) : 0;

            if (quote != 0) {
                pos++;
            }

            if (quote == 0 && isNullToken(str, pos, limit)) {
                // The new array already holds the null element or the primitive default.
                pos += NULL_STRING.length();
            } else {
                pos = floatingPoint ? readFloatingElement(str, pos, limit, kind, result, i) : readIntegralElement(str, pos, limit, kind, result, i);

                if (pos < 0) {
                    return null;
                }
            }

            if (quote != 0) {
                if (pos == limit || str.charAt(pos) != quote) {
                    return null;
                }

                pos++;
            }

            if (i == count - 1) {
                if (skipArrayWhitespace(str, pos, limit) != limit) {
                    return null;
                }
            } else {
                // The usual spelling has a comma immediately after the number. Only inspect structural
                // whitespace here when that delimiter is not already at the current index.
                if (pos < limit && str.charAt(pos) != ',') {
                    pos = skipArrayWhitespace(str, pos, limit);
                }

                if (pos == limit || str.charAt(pos++) != ',') {
                    return null;
                }
            }
        }

        return result;
    }

    /**
     * Skips structural whitespace without creating the stripped strings used by {@link #split(String)}.
     *
     * @param str the array text
     * @param from the first index to inspect
     * @param limit the exclusive end of the region
     * @return the first non-whitespace index, or {@code limit}
     */
    private static int skipArrayWhitespace(final String str, final int from, final int limit) {
        int pos = from;

        // Spaces are common, and other printable ASCII characters cannot be whitespace. Consult the Unicode
        // classification only for controls and non-ASCII text, keeping ordinary numeric elements inexpensive.
        char ch;

        while (pos < limit && ((ch = str.charAt(pos)) == ' ' || ((ch < ' ' || ch >= '\u0080') && Character.isWhitespace(ch)))) {
            pos++;
        }

        return pos;
    }

    /**
     * Checks one fast-path element, including optional matching quotes around a decimal number.
     *
     * @param str the array text
     * @param from the first non-whitespace index of the element
     * @param limit the index of the closing bracket
     * @param floatingPoint whether fractions and exponents are admitted
     * @return the index after the element, or {@code -1} for unsupported content; the caller checks the separator
     */
    private static int numericElementEnd(final String str, final int from, final int limit, final boolean floatingPoint) {
        if (from == limit) {
            return -1;
        }

        final char quote = str.charAt(from);

        if (quote == '"' || quote == '\'') {
            final int end = decimalEnd(str, from + 1, limit, floatingPoint);
            return end >= 0 && end < limit && str.charAt(end) == quote ? end + 1 : -1;
        }

        return isNullToken(str, from, limit) ? from + NULL_STRING.length() : decimalEnd(str, from, limit, floatingPoint);
    }

    /**
     * Tests whether the bare {@code null} token starts at {@code pos}; the caller checks what follows it.
     *
     * @param str the array text
     * @param pos the index of the element's first character
     * @param limit the index of the closing bracket
     * @return {@code true} if {@code null} starts at {@code pos} and ends before {@code limit}
     */
    private static boolean isNullToken(final String str, final int pos, final int limit) {
        return pos + NULL_STRING.length() <= limit && str.startsWith(NULL_STRING, pos);
    }

    /**
     * Reads one decimal integer element of {@link #parseNumericArray(String, Type, Class)} and stores it.
     * <p>
     * Leading zeros are skipped and at most 19 significant digits can be accepted. Their magnitude fits in an
     * unsigned {@code long}, so a negative accumulated magnitude marks signed overflow, except for the magnitude of
     * {@link Long#MIN_VALUE} with a minus sign. Checking the length and sign after the digit scan admits the full
     * {@code long} range without rescanning a token or adding overflow checks to each digit of shorter numbers.
     * A rejected value is left to the original parser to retain its overflow diagnostics and validation order.
     * </p>
     *
     * @param str the array text
     * @param from the index of the element's first character (an optional sign)
     * @param limit the index of the closing bracket
     * @param kind the array kind chosen by {@code parseNumericArray}
     * @param result the array to store the element into
     * @param index the element's index in {@code result}
     * @return the index after the element, or {@code -1} if it has no digits or is out of the component's range
     */
    private static int readIntegralElement(final String str, final int from, final int limit, final int kind, final Object result, final int index) {
        int pos = from;
        boolean negative = false;

        if (pos < limit) {
            final char sign = str.charAt(pos);

            if (sign == '-') {
                negative = true;
                pos++;
            } else if (sign == '+') {
                pos++;
            }
        }

        final int digitStart = pos;
        long value = 0;

        while (pos < limit && str.charAt(pos) == '0') {
            pos++;
        }

        final int significantStart = pos;

        for (char ch; pos < limit && (ch = str.charAt(pos)) >= '0' && ch <= '9'; pos++) {
            value = value * 10 + (ch - '0');
        }

        // A 19-digit magnitude is below 10^19 < 2^64, so wrapping cannot turn an overflowing magnitude positive.
        // For longer runs the wrapped value is ignored. MIN_VALUE intentionally negates to itself below.
        if (pos == digitStart || pos - significantStart > 19 || (value < 0 && (!negative || value != Long.MIN_VALUE))) {
            return -1;
        }

        if (negative) {
            value = -value;
        }

        switch (kind) {
            case 0, 6: {
                if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                    return -1;
                }

                if (kind == 0) {
                    ((int[]) result)[index] = (int) value;
                } else {
                    ((Integer[]) result)[index] = (int) value;
                }

                break;
            }

            case 1, 7: {
                if (kind == 1) {
                    ((long[]) result)[index] = value;
                } else {
                    ((Long[]) result)[index] = value;
                }

                break;
            }

            case 2, 8: {
                if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                    return -1;
                }

                if (kind == 2) {
                    ((byte[]) result)[index] = (byte) value;
                } else {
                    ((Byte[]) result)[index] = (byte) value;
                }

                break;
            }

            case 3, 9: {
                if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                    return -1;
                }

                if (kind == 3) {
                    ((short[]) result)[index] = (short) value;
                } else {
                    ((Short[]) result)[index] = (short) value;
                }

                break;
            }

            default:
                throw new AssertionError("Unexpected integral array kind: " + kind);
        }

        return pos;
    }

    /**
     * Reads one decimal floating-point element of {@link #parseNumericArray(String, Type, Class)} and stores it.
     * <p>
     * The element has the grammar {@link #decimalEnd(String, int, int, boolean)} admits. When its significant digits
     * form an integer {@code m} of at most 2<sup>53</sup> ({@code double}) or 2<sup>24</sup> ({@code float}) and its
     * decimal exponent {@code e} is at most 22 (10) in magnitude, both {@code m} and 10<sup>|e|</sup> are exact, so one
     * multiplication or division gives the correctly rounded value, the one {@link Double#parseDouble(String)} /
     * {@link Float#parseFloat(String)} return (the classic Clinger fast path). Trailing significand zeros may be
     * removed, increasing the decimal exponent by the same amount, to bring a longer spelling into these bounds.
     * No nonzero digits are discarded. Any other element is parsed by those methods from its original text.
     * </p>
     *
     * @param str the array text
     * @param from the index of the element's first character (an optional sign)
     * @param limit the index of the closing bracket
     * @param kind the array kind chosen by {@code parseNumericArray}
     * @param result the array to store the element into
     * @param index the element's index in {@code result}
     * @return the index after the element, or {@code -1} if it is not a decimal number
     */
    private static int readFloatingElement(final String str, final int from, final int limit, final int kind, final Object result, final int index) {
        int pos = from;
        boolean negative = false;

        if (pos < limit) {
            final char sign = str.charAt(pos);

            if (sign == '-') {
                negative = true;
                pos++;
            } else if (sign == '+') {
                pos++;
            }
        }

        long significand = 0;
        int significantDigits = 0; // digits after the leading zeros; the significand wraps only beyond 18 of them
        int digits = 0;
        int fractionDigits = 0;

        for (char ch; pos < limit && (ch = str.charAt(pos)) >= '0' && ch <= '9'; pos++) {
            digits++;

            if (significantDigits != 0 || ch != '0') {
                significantDigits++;
                significand = significand * 10 + (ch - '0');
            }
        }

        if (pos < limit && str.charAt(pos) == '.') {
            pos++;

            for (char ch; pos < limit && (ch = str.charAt(pos)) >= '0' && ch <= '9'; pos++) {
                digits++;
                fractionDigits++;

                if (significantDigits != 0 || ch != '0') {
                    significantDigits++;
                    significand = significand * 10 + (ch - '0');
                }
            }
        }

        if (digits == 0) {
            return -1;
        }

        final int significandEnd = pos;
        int exponent = 0;
        boolean exactExponent = true;

        if (pos < limit && (str.charAt(pos) == 'e' || str.charAt(pos) == 'E')) {
            pos++;
            boolean negativeExponent = false;

            if (pos < limit && (str.charAt(pos) == '+' || str.charAt(pos) == '-')) {
                negativeExponent = str.charAt(pos) == '-';
                pos++;
            }

            final int exponentStart = pos;

            for (char ch; pos < limit && (ch = str.charAt(pos)) >= '0' && ch <= '9'; pos++) {
                if (exponent < 100_000) {
                    exponent = exponent * 10 + (ch - '0');
                } else {
                    // A capped exponent can be cancelled by a long fractional part. Never use that approximation
                    // for direct conversion, even if the resulting scale appears to fall inside the exact range.
                    exactExponent = false;
                }
            }

            if (pos == exponentStart) {
                return -1;
            }

            if (negativeExponent) {
                exponent = -exponent;
            }
        }

        long scale = (long) exponent - fractionDigits;
        final boolean singlePrecision = kind == 4 || kind == 10;
        final int maxScale = singlePrecision ? 10 : 22;
        final long maxSignificand = singlePrecision ? 1L << 24 : 1L << 53;

        boolean directlyConvertible = exactExponent && significantDigits <= 18 && significand <= maxSignificand && scale >= -maxScale && scale <= maxScale;

        // The ordinary short-decimal path needs no extra scan. Only try normalization when it can turn a fallback
        // into one exact multiplication/division; raising a scale already above the bound cannot help.
        if (!directlyConvertible && exactExponent && significantDigits > 0 && scale <= maxScale) {
            int end = significandEnd;
            int trailingZeros = 0;

            while (end > from) {
                final char ch = str.charAt(end - 1);

                if (ch == '0') {
                    trailingZeros++;
                } else if (ch != '.') {
                    break;
                }

                end--;
            }

            final int retainedDigits = significantDigits - trailingZeros;

            if (trailingZeros > 0 && retainedDigits <= 18) {
                // The original accumulator may have wrapped. Rebuild only the retained digits (at most 18),
                // working backwards to avoid rescanning an arbitrarily long run of leading zeros.
                significand = 0;
                long place = 1;

                for (int remaining = retainedDigits; remaining > 0;) {
                    final char ch = str.charAt(--end);

                    if (ch != '.') {
                        significand += (ch - '0') * place;
                        place *= 10;
                        remaining--;
                    }
                }

                scale += trailingZeros;
                directlyConvertible = significand <= maxSignificand && scale >= -maxScale && scale <= maxScale;
            }
        }

        try {
            if (singlePrecision) {
                final float value;

                if (directlyConvertible) {
                    final float magnitude = scale < 0 ? significand / EXACT_FLOAT_POWERS_OF_TEN[(int) -scale]
                            : significand * EXACT_FLOAT_POWERS_OF_TEN[(int) scale];
                    value = negative ? -magnitude : magnitude;
                } else {
                    value = Float.parseFloat(str.substring(from, pos));
                }

                if (kind == 4) {
                    ((float[]) result)[index] = value;
                } else {
                    ((Float[]) result)[index] = value;
                }
            } else {
                final double value;

                if (directlyConvertible) {
                    final double magnitude = scale < 0 ? significand / EXACT_DOUBLE_POWERS_OF_TEN[(int) -scale]
                            : significand * EXACT_DOUBLE_POWERS_OF_TEN[(int) scale];
                    value = negative ? -magnitude : magnitude;
                } else {
                    value = Double.parseDouble(str.substring(from, pos));
                }

                if (kind == 5) {
                    ((double[]) result)[index] = value;
                } else {
                    ((Double[]) result)[index] = value;
                }
            }
        } catch (final NumberFormatException e) {
            // Not expected for the admitted grammar; the original parser then reports the element.
            return -1;
        }

        return pos;
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
