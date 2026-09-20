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

package com.landawn.abacus.parser;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Internal implementation of {@link JsonReader} for parsing JSON from string sources.
 * This class provides efficient JSON parsing by working directly with character arrays
 * and minimizing object allocation during parsing.
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Efficient character buffer management</li>
 *   <li>Direct number parsing without intermediate string creation</li>
 *   <li>Support for escape character handling</li>
 *   <li>Optimized parsing of common JSON values ({@code true}, {@code false}, {@code null})</li>
 * </ul>
 *
 * <p>This is an internal class and should not be used directly by application code.
 * Instances are not thread-safe and must not be shared across threads.</p>
 */
class JsonStringReader extends AbstractJsonReader {
    private static final Logger logger = LoggerFactory.getLogger(JsonStringReader.class);

    /**
     * Default empty optional instances for various types.
     */
    static final Map<Class<?>, Object> defaultOptionals = new HashMap<>(16);

    static {
        defaultOptionals.put(OptionalBoolean.class, OptionalBoolean.empty());
        defaultOptionals.put(OptionalChar.class, OptionalChar.empty());
        defaultOptionals.put(OptionalByte.class, OptionalByte.empty());
        defaultOptionals.put(OptionalShort.class, OptionalShort.empty());
        defaultOptionals.put(OptionalInt.class, OptionalInt.empty());
        defaultOptionals.put(OptionalLong.class, OptionalLong.empty());
        defaultOptionals.put(OptionalFloat.class, OptionalFloat.empty());
        defaultOptionals.put(OptionalDouble.class, OptionalDouble.empty());
        defaultOptionals.put(Optional.class, Optional.empty());
        defaultOptionals.put(Nullable.class, Nullable.empty());

        defaultOptionals.put(java.util.Optional.class, java.util.Optional.empty());
        defaultOptionals.put(java.util.OptionalInt.class, java.util.OptionalInt.empty());
        defaultOptionals.put(java.util.OptionalLong.class, java.util.OptionalLong.empty());
        defaultOptionals.put(java.util.OptionalDouble.class, java.util.OptionalDouble.empty());
    }

    /** The underlying reader, if any. */
    final Reader reader;

    /** The source character array. */
    final char[] strValue;

    /** The end index of the string content. */
    int strEndIndex = 0;

    /** The current beginning index for parsing. */
    int strBeginIndex = 0;

    /** The starting index for the current text token. */
    int startIndexForText = 0;

    /** The ending index for the current text token. */
    int endIndexForText = 0;

    /** The internal character buffer for escape handling and token building. */
    char[] cbuf;

    /** The length of the internal character buffer. */
    int cbufLen = 0;

    /** The last parsed token. */
    int lastEvent = -1;

    /** The next token to be processed. */
    int nextEvent = -1;

    /** The next character position in {@code cbuf}. */
    int nextChar = 0;

    /** Whether whitespace has terminated the current unquoted value. */
    boolean whitespaceAfterText = false;

    /** The string representation of the current token. */
    String text = null;

    /** The numeric representation of the current token. */
    Number numValue = null;

    /**
     * Constructs a new {@code JsonStringReader} with the specified string and buffer.
     *
     * @param str the JSON string to parse
     * @param cbuf the character buffer to use for parsing
     * @throws IllegalArgumentException if {@code str} or {@code cbuf} is {@code null}.
     */
    JsonStringReader(final String str, final char[] cbuf) throws IllegalArgumentException {
        this(N.checkArgNotNull(str, cs.str), 0, str.length(), cbuf);
    }

    /**
     * Constructs a new {@code JsonStringReader} with the specified string range and buffer.
     *
     * @param str the JSON string to parse; {@code null} is treated as an empty string
     * @param beginIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @param cbuf the character buffer to use for parsing
     * @throws IllegalArgumentException if the requested string range is invalid, or {@code cbuf} is {@code null}.
     */
    @SuppressWarnings("deprecation")
    JsonStringReader(final String str, final int beginIndex, final int toIndex, final char[] cbuf) throws IllegalArgumentException {
        this(com.landawn.abacus.util.InternalUtil.getCharsForReadOnly(str), beginIndex, toIndex, cbuf, null);
    }

    /**
     * Constructs a new {@code JsonStringReader} with the specified character array range, buffer, and reader.
     *
     * @param strValue the character array to parse
     * @param beginIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @param cbuf the character buffer to use for parsing
     * @param reader the underlying reader (may be {@code null})
     * @throws IllegalArgumentException if {@code strValue} is {@code null}, {@code beginIndex} or {@code toIndex} is negative,
     *         {@code toIndex} is less than {@code beginIndex}, either index exceeds {@code strValue.length},
     *         or {@code cbuf} is {@code null}.
     */
    JsonStringReader(final char[] strValue, final int beginIndex, final int toIndex, final char[] cbuf, final Reader reader) throws IllegalArgumentException {
        N.checkArgNotNull(strValue, cs.strValue);

        if (beginIndex < 0 || toIndex < 0 || toIndex < beginIndex || beginIndex > strValue.length || toIndex > strValue.length) {
            throw new IllegalArgumentException("Invalid beginIndex or toIndex: " + beginIndex + ", " + toIndex);
        }

        N.checkArgNotNull(cbuf, cs.cbuf);

        this.reader = reader;

        this.strValue = strValue;
        strBeginIndex = beginIndex;
        strEndIndex = toIndex;
        this.cbuf = cbuf;
        cbufLen = this.cbuf.length;
    }

    /**
     * Creates a {@code JsonReader} for parsing the given JSON string.
     * This factory method creates an optimized reader for string sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\"}";
     * JsonReader reader = JsonStringReader.parse(json, new char[256]);
     * }</pre>
     *
     * @param str the JSON string to parse
     * @param cbuf the character buffer to use for parsing
     * @return a new {@code JsonReader} instance
     * @throws IllegalArgumentException if {@code str} or {@code cbuf} is {@code null}.
     */
    public static JsonReader parse(final String str, final char[] cbuf) throws IllegalArgumentException {
        //        return new JsonStreamReader(new StringReader(str), new char[1], cbuf);

        return new JsonStringReader(str, cbuf);
    }

    /**
     * Creates a {@code JsonReader} for parsing a substring of the given JSON string.
     * This allows parsing a portion of a larger string without creating a substring.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * JsonReader reader = JsonStringReader.parse(json, 6, json.length() - 6, new char[256]);
     * }</pre>
     *
     * @param str the JSON string; {@code null} is treated as an empty string
     * @param beginIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @param cbuf the character buffer to use for parsing
     * @return a new {@code JsonReader} instance
     * @throws IllegalArgumentException if the requested string range is invalid, or {@code cbuf} is {@code null}.
     */
    public static JsonReader parse(final String str, final int beginIndex, final int toIndex, final char[] cbuf) throws IllegalArgumentException {
        return new JsonStringReader(str, beginIndex, toIndex, cbuf);
    }

    /**
     * Returns the token identifier from one step before the most recent {@code nextToken} call
     * (i.e. one token earlier in the stream than the current one).
     *
     * @return the prior token identifier, or {@code -1} if no prior token has been read
     */
    @Override
    public int lastToken() {
        return lastEvent;
    }

    /**
     * Reads and returns the next token from the JSON input.
     * This method advances the reader position and identifies the next
     * structural token or value in the JSON stream.
     *
     * <p>The method handles:</p>
     * <ul>
     *   <li>Quoted strings (double and single quotes)</li>
     *   <li>Numbers (integers and decimals)</li>
     *   <li>Boolean values ({@code true}/{@code false})</li>
     *   <li>{@code null} values</li>
     *   <li>Structural tokens (braces, brackets, colons, commas)</li>
     * </ul>
     *
     * @param nextTokenValueType the expected type of the next token value
     * @return the token identifier, or {@code -1} if no next token is found
     * @throws UncheckedIOException if reading from the underlying character stream fails
     * @throws ParsingException if a quoted string is unterminated, an escape sequence is malformed, or unquoted token text contains unexpected whitespace
     */
    @Override
    public int nextToken(final Type<?> nextTokenValueType) throws UncheckedIOException, ParsingException {
        lastEvent = nextEvent;

        text = null;
        numValue = null;
        nextChar = 0;
        whitespaceAfterText = false;
        startIndexForText = strBeginIndex;

        if (nextEvent == START_DOUBLE_QUOTE || nextEvent == START_SINGLE_QUOTE) {
            final char quoteChar = nextEvent == START_DOUBLE_QUOTE ? SK._DOUBLE_QUOTE : SK._SINGLE_QUOTE;

            for (int ch = 0; strBeginIndex < strEndIndex;) {
                ch = strValue[strBeginIndex++];

                if (ch == quoteChar) {
                    endIndexForText = strBeginIndex - 1;
                    nextEvent = quoteChar == SK._DOUBLE_QUOTE ? END_DOUBLE_QUOTE : END_SINGLE_QUOTE;

                    return nextEvent;
                }

                if (nextChar > 0) {
                    if (nextChar >= cbufLen) {
                        enlargeCharBuffer();
                    }

                    cbuf[nextChar++] = (ch == SK._BACKSLASH) ? readEscapeCharacter() : (char) ch;
                } else {
                    if (ch == SK._BACKSLASH) {
                        saveToBuffer();

                        // strStart++;
                        cbuf[nextChar++] = readEscapeCharacter();
                    }
                }
            }

            // Reached end of input while still inside a quoted string.
            throw new ParsingException("Unterminated string");
        } else {
            for (int ch = 0; strBeginIndex < strEndIndex;) {
                ch = strValue[strBeginIndex++];

                if (ch < 128 && (nextEvent = charEvents[ch]) > 0) {
                    if (nextEvent < 32) { //
                        endIndexForText = strBeginIndex - 1;

                        return nextEvent;
                    }

                    saveChar(ch);

                    if (nextChar == 0 && strBeginIndex - startIndexForText == 1) {
                        boolean isNumber = false;

                        if (nextEvent == 'f') { // false
                            if (matchLiteralChar('a') && matchLiteralChar('l') && matchLiteralChar('s') && matchLiteralChar('e')) {
                                text = FALSE;
                            }
                        } else if (nextEvent == 't') { // true
                            if (matchLiteralChar('r') && matchLiteralChar('u') && matchLiteralChar('e')) {
                                text = TRUE;
                            }
                        } else if (nextEvent == 'n') { // null
                            if (matchLiteralChar('u') && matchLiteralChar('l') && matchLiteralChar('l')) { //NOSONAR
                                text = NULL;
                            }
                        } else if ((nextEvent >= '0' && nextEvent <= '9') || nextEvent == '-' || nextEvent == '+') { // number.
                            isNumber = true;
                            readNumber(ch, nextTokenValueType);
                            //    } else if (nextEvent == 'F') { // "False", "FALSE" // possible? TODO
                            //    } else if (nextEvent == 'T') { // "True", "TRUE" // possible? TODO
                            //    } else if (nextEvent == 'N') { // "Null", "NULL" // possible? TODO
                        }

                        if (isNumber) {
                            // done in readNumber...
                        } else {
                            while (strBeginIndex < strEndIndex) {
                                ch = strValue[strBeginIndex++];

                                if (ch < 128) {
                                    nextEvent = charEvents[ch];

                                    if (nextEvent > 0 && nextEvent < 32) {
                                        endIndexForText = strBeginIndex - 1;
                                        return nextEvent;
                                    }
                                }

                                if (saveChar(ch) > 32) {
                                    text = null;
                                }
                            }

                            endIndexForText = strBeginIndex;
                            nextEvent = -1;
                        }

                        return nextEvent;
                    }
                } else {
                    saveChar(ch);
                }
            }
        }

        endIndexForText = strBeginIndex;
        nextEvent = -1;

        return nextEvent;
    }

    /**
     * Checks if the reader has text content available.
     * This is {@code true} when the current token carries non-empty text: an unquoted value
     * (number, boolean, {@code null} or any other unquoted token) or a quoted string with at least
     * one character. It is {@code false} for an empty quoted string ({@code ""} or {@code ''}), for a
     * structural token that no unquoted text precedes (a quoted string's text belongs to its closing-quote
     * token) and at EOF when only whitespace followed the last value.
     *
     * @return {@code true} if non-empty text content is available, {@code false} otherwise
     */
    @Override
    public boolean hasText() {
        return text != null || numValue != null || (nextChar > 0) || (endIndexForText > startIndexForText);
    }

    /**
     * Reads and parses a number from the JSON input.
     * This method efficiently parses numbers without creating intermediate strings
     * when possible, improving performance for numeric data.
     *
     * <p>Supported number formats:</p>
     * <ul>
     *   <li>Integers: 123, -456</li>
     *   <li>Decimals: 123.456, -78.9</li>
     *   <li>Scientific notation: 1.23e10, -4.56E-7</li>
     *   <li>Type suffixes: 123L, 45.6f, 78.9d</li>
     * </ul>
     * <p>Decimal {@code float} values are rounded directly from their decimal value, without an
     * intermediate rounding to {@code double}.</p>
     *
     * @param firstChar the first character of the number
     * @param nextTokenValueType the expected type of the next token value
     * @throws UncheckedIOException if a stream-backed subclass cannot read more input while scanning the number
     * @throws ParsingException if token text has an invalid escape or unexpected whitespace, or its character buffer cannot grow
     */
    protected void readNumber(final int firstChar, final Type<?> nextTokenValueType) throws UncheckedIOException, ParsingException {
        final boolean negative = firstChar == '-';
        long ret = firstChar == '-' || firstChar == '+' ? 0 : (firstChar - '0');

        int pointPosition = -1;
        int digitCount = ret == 0 ? 0 : 1;
        int ch = 0;
        int typeFlag = 0;

        while (strBeginIndex < strEndIndex) {
            ch = strValue[strBeginIndex++];

            if (ch >= '0' && ch <= '9') {
                if (digitCount < MAX_PARSABLE_NUM_LEN || (digitCount == MAX_PARSABLE_NUM_LEN && ret <= (Long.MAX_VALUE - (ch - '0')) / 10)) {
                    ret = ret * 10 + (ch - '0');

                    if (ret > 0 || pointPosition > 0) {
                        digitCount++;
                    }
                } else {
                    digitCount += 2; // So digitCount will > MAX_PARSABLE_NUM_LEN + 1 to skip the result.
                }
            } else if (ch == '.' && pointPosition < 0) {
                if (digitCount == 0) {
                    digitCount = 1;
                }

                pointPosition = digitCount;
            } else {
                if (ch < 128) {
                    nextEvent = charEvents[ch];

                    if (nextEvent > 0 && nextEvent < 32) {
                        break;
                    }
                } else {
                    nextEvent = 0;
                }

                ch = saveChar(ch);

                //noinspection ConstantValue
                // No `nextEvent > 0` guard: charEvents has no entry for L/D (only f/F), so
                // requiring nextEvent>0 made the type-flag branch dead for `123L`/`1.5d`. Test
                // for the suffix character directly; the structural-event check above already
                // breaks the loop on real terminators.
                if (typeFlag == 0 && (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D')) {
                    typeFlag = ch;
                } else if (ch > 32) { // ignore <= 32 whitespace chars.
                    digitCount = -1; // TODO can't parse here. leave it Numbers.createNumber(...).
                }

                while (strBeginIndex < strEndIndex) {
                    ch = strValue[strBeginIndex++];

                    if (ch < 128) {
                        nextEvent = charEvents[ch];

                        if (nextEvent > 0 && nextEvent < 32) {
                            break;
                        }
                    } else {
                        nextEvent = 0;
                    }

                    ch = saveChar(ch);

                    // No `nextEvent > 0` guard: charEvents has no entry for L/D (only f/F), so
                    // requiring nextEvent>0 made the type-flag branch dead for `1e5L`/`1e5d`. Match
                    // the first occurrence above (line 428) which has no such guard.
                    if (typeFlag == 0 && (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D')) {
                        typeFlag = ch;
                    } else if (ch > 32) { // ignore <= 32 whitespace chars.
                        digitCount = -1; // TODO can't parse here. leave it Numbers.createNumber(...).
                    }
                }

                break;
            }
        }

        if (nextEvent > 0 && nextEvent < 32) {
            endIndexForText = strBeginIndex - 1;
        } else {
            endIndexForText = strBeginIndex;
            nextEvent = -1;
        }

        // The decimal fast path divides (double) ret by a power of ten, which is only correctly
        // rounded when ret converts to double exactly (<= 2^53): 17+-digit mantissas (e.g.
        // Double.toString output) would mis-parse by 1 ulp, and "-0.0" cannot survive the long
        // negation - both must fall back to the exact parser. digitCount > 0 rejects bare "-"/"+"
        // tokens, which previously parsed as a fabricated 0.
        final boolean exactDecimalFastPath = ret <= (1L << 53) && !(negative && ret == 0);

        if (digitCount > 0 && digitCount <= MAX_PARSABLE_NUM_LEN + 1 && pointPosition != digitCount && (pointPosition <= 0 || exactDecimalFastPath)) {
            if (negative) {
                ret = -ret;
            }

            if (nextTokenValueType != null && (nextTokenValueType.isNumber() || typeFlag > 0)) {
                if (pointPosition > 0) {
                    if (nextTokenValueType.isFloat() || typeFlag == 'f' || typeFlag == 'F') {
                        // Rounding through double can land on a float midpoint and round a second time in the wrong direction.
                        numValue = BigDecimal.valueOf(ret, digitCount - pointPosition).floatValue();
                    } else { // ignore 'l' or 'L' if it's specified.
                        numValue = ((double) ret) / POWERS_OF_TEN[digitCount - pointPosition];
                    }
                } else if (nextTokenValueType.isFloat() || typeFlag == 'f' || typeFlag == 'F') {
                    numValue = (float) ret;
                } else if (nextTokenValueType.isDouble() || typeFlag == 'd' || typeFlag == 'D') {
                    numValue = (double) ret;
                } else { // typeFlag == 'l' or 'L'.
                    numValue = ret;
                }
            } else {
                if (pointPosition > 0) {
                    numValue = ((double) ret) / POWERS_OF_TEN[digitCount - pointPosition];
                } else if (ret >= Integer.MIN_VALUE && ret <= Integer.MAX_VALUE) {
                    numValue = (int) ret;
                } else {
                    numValue = ret;
                }
            }
        }
        //    else { // for debug
        //        logger.warn("#######: " + getText());
        //        System.out.println("#######: " + getText());
        //    }
    }

    /**
     * Peeks at the next character of a {@code true}/{@code false}/{@code null} literal and consumes it only
     * when it can belong to the literal.
     *
     * <p>A structural character (a token event below 32: brace, bracket, quote, colon, comma) terminates the
     * unquoted token, so it is left in the buffer for the scan that follows; consuming it fused the next value
     * into this token ({@code "[tru,1]"} produced the single element {@code "tru,1"}, {@code "[t]"} ate the
     * {@code ']'}). Whitespace is deliberately not a stop: {@link #saveChar(int)} records it, so {@code "fal se"}
     * is still rejected as text after whitespace. A non-structural mismatch is consumed (and saved) exactly as
     * the surrounding scan would have consumed it.</p>
     *
     * @param expected the character the literal requires at this position
     * @return {@code true} if the next character was consumed and matched {@code expected}
     * @throws UncheckedIOException if a stream-backed subclass cannot read more input while resolving an escape
     * @throws ParsingException if the consumed character starts an invalid escape, follows whitespace within an unquoted value, or requires a token
     *         buffer that cannot grow
     */
    protected boolean matchLiteralChar(final char expected) throws UncheckedIOException, ParsingException {
        if (strBeginIndex >= strEndIndex) {
            return false;
        }

        final int ch = strValue[strBeginIndex];

        if (ch < 128 && charEvents[ch] > 0 && charEvents[ch] < 32) {
            return false;
        }

        strBeginIndex++;

        return saveChar(ch) == expected;
    }

    /**
     * Processes a token character: skips leading or trailing whitespace, resolves escape sequences, and appends to the
     * internal token buffer only when buffered mode is (or becomes) active (in zero-copy mode,
     * ordinary characters remain tracked in the source array). Whitespace after an unquoted value
     * terminates that value; a later non-whitespace character before a structural delimiter is rejected
     * instead of being silently concatenated to the preceding text. A backslash triggers escape-sequence
     * resolution via {@link #readEscapeCharacter()}.
     * This method manages the character buffer efficiently to minimize allocations.
     *
     * @param ch the input character to consider
     * @return the unescaped character if {@code ch} was a backslash, otherwise the original {@code ch}
     * @throws UncheckedIOException if a stream-backed subclass cannot read more input while resolving an escape
     * @throws ParsingException if an escape is malformed, non-whitespace follows whitespace within an unquoted value, or the token buffer cannot grow
     */
    protected int saveChar(int ch) throws UncheckedIOException, ParsingException {
        if (nextChar > 0) {
            if (ch == SK._BACKSLASH) {
                ch = readEscapeCharacter();
            }

            if (ch < 33) {
                whitespaceAfterText = true;
            } else {
                checkNoTextAfterWhitespace();

                if (nextChar >= cbufLen) {
                    enlargeCharBuffer();
                }

                cbuf[nextChar++] = (char) ch;
            }
        } else {
            if (ch < 33) {
                if (startIndexForText == (strBeginIndex - 1)) {
                    startIndexForText++;
                } else {
                    saveToBuffer();
                    whitespaceAfterText = true;
                }
            } else if (ch == SK._BACKSLASH) {
                saveToBuffer();
                // strStart++;
                ch = readEscapeCharacter();

                if (ch < 33) {
                    whitespaceAfterText = true;
                } else {
                    checkNoTextAfterWhitespace();
                    cbuf[nextChar++] = (char) ch;
                }
            } else {
                checkNoTextAfterWhitespace();
            }
        }

        return ch;
    }

    /**
     * @throws ParsingException if a non-whitespace character follows whitespace within an unquoted JSON value
     */
    private void checkNoTextAfterWhitespace() throws ParsingException {
        if (whitespaceAfterText) {
            throw new ParsingException("Unexpected non-whitespace character after an unquoted JSON value");
        }
    }

    /**
     * Saves the current string range to the internal character buffer.
     * @throws ParsingException if the token buffer cannot grow enough to hold the pending text
     */
    protected void saveToBuffer() throws ParsingException {
        endIndexForText = strBeginIndex - 1;

        // Loop, not if: a single grow only multiplies cbuf by ~1.75. For tokens much larger than
        // cbufLen (long quoted values containing an escape near the end) one grow is not enough
        // and N.copy below would overflow the buffer.
        while (endIndexForText - startIndexForText + 1 >= cbufLen) {
            enlargeCharBuffer();
        }

        N.copy(strValue, startIndexForText, cbuf, 0, endIndexForText - startIndexForText);

        nextChar = endIndexForText - startIndexForText;
    }

    /**
     * Throws a {@code ParsingException} when an unexpected non-string token is encountered.
     *
     * @throws ParsingException always thrown
     */
    protected void throwExceptionDueToUnexpectedNonStringToken() throws ParsingException {
        throw new ParsingException(
                "\"false\", \"true\", \"null\" or a number is expected in or before \"" + (nextChar > 0 ? String.valueOf(cbuf, 0, N.min(32, nextChar))
                        : String.valueOf(strValue, Math.max(0, strBeginIndex - 1), N.min(32, strEndIndex - Math.max(0, strBeginIndex - 1)))));
    }

    /**
     * Returns the text content of the current token.
     *
     * @return the text of the current token
     */
    @Override
    public String getText() {
        if (text != null) {
            return text;
        }

        return (nextChar > 0) ? String.valueOf(cbuf, 0, nextChar) : String.valueOf(strValue, startIndexForText, endIndexForText - startIndexForText);
    }

    /**
     * Reads and converts the current token value to the specified type.
     *
     * <p>Exact decimal/integer targets consume the original token, preserving decimal precision
     * and scale. Conversions from a floating-point cache to another numeric type also consume
     * the token, avoiding double rounding and spelling-dependent fractional truncation. The
     * token's Java type suffix ({@code 123L}, {@code 1.5f}) is dropped first, and a target whose
     * parser cannot read the token's spelling at all - a fractional token into {@code BigInteger}
     * or an integral slot - converts the cached {@code Number} instead (truncating toward zero)
     * rather than failing. An unquoted number read into a {@code String} target keeps the token's
     * spelling ({@code 007} stays {@code "007"}, {@code 1.50} stays {@code "1.50"}); an
     * {@code Object} target receives the parsed {@code Number}.</p>
     *
     * @param <T> the target type
     * @param type the type descriptor for conversion
     * @return the converted value
     */
    @SuppressWarnings({ "unchecked", "cast" })
    @Override
    public <T> T readValue(final Type<? extends T> type) {
        if (nextEvent != END_DOUBLE_QUOTE && nextEvent != END_SINGLE_QUOTE) {
            if (numValue != null) {
                // The final target may only become known after nextToken() cached a floating value.
                // Reusing that approximation would lose BigDecimal digits/scale or round a float twice.
                if (type.javaType() == BigDecimal.class || type.javaType() == BigInteger.class
                        || (type.isNumber() && ((numValue instanceof Float && !type.isFloat()) || (numValue instanceof Double && !type.isDouble())))) {
                    // The tokenizer accepts Java type suffixes (123L, 1.5f), but exact
                    // BigInteger/BigDecimal parsers require the bare token.
                    final String token = stripNumberTypeSuffix(getText());

                    // A fractional token has no integral spelling, so an integral target can only throw below.
                    // Answered here rather than through the catch: a NumberFormatException fills in a stack
                    // trace per value, and "1.0" into an int slot is an ordinary shape, not a malformed one -
                    // measured ~40x the cost of the same list of integral tokens.
                    if (token.indexOf('.') >= 0 && isIntegralNumberClass(type.javaType())) {
                        return (T) Numbers.convert(numValue, (Type<Number>) type);
                    }

                    try {
                        return type.valueOf(token);
                    } catch (final NumberFormatException e) {
                        // The target cannot read this spelling at all. The cached value is a valid parse of the
                        // very same token, so convert that instead of failing on a token the tokenizer already
                        // accepted. A range violation still escapes as ArithmeticException.
                        return (T) Numbers.convert(numValue, (Type<Number>) type);
                    }
                }

                if (type.isObject() || type.javaType().equals(numValue.getClass())) {
                    return (T) numValue;
                } else if (type.isNumber()) {
                    return (T) Numbers.convert(numValue, (Type<Number>) type);
                } else if (type.isDate() || type.isCalendar() || type.isJodaDateTime()) {
                    return type.valueOf(numValue);
                } else {
                    if (text != null) {
                        return type.valueOf(text);
                    } else if (type.isString()) {
                        // An unquoted number read into a String target keeps its spelling ("007",
                        // "1.50", "+5", "123L"): the fast-path Number is a parsing cache, and going
                        // through Number.toString() re-spelled only the tokens the fast path accepted.
                        return (T) getText();
                    } else {
                        return N.convert(numValue, type);
                    }
                }
            } else if (text != null) {
                if (text.equals(NULL)) {
                    // JSON null for Optional/Nullable/Holder → empty; for primitives → type default
                    // (e.g. 0/false) so primitive arrays unbox without NPE; for others → null.
                    if (type.isOptionalOrNullable()) {
                        return (T) defaultOptionals.get(type.javaType());
                    } else if (type.isPrimitive()) {
                        return (T) type.defaultValue();
                    } else {
                        return null;
                    }
                } else if ((text.equals(FALSE) || text.equals(TRUE)) && (type.isBoolean() || type.isObject())) {
                    return (T) (text.equals(FALSE) ? Boolean.FALSE : Boolean.TRUE);
                } else {
                    return type.valueOf(text);
                }
            }

            if (type.isObject()) {
                final String numberText = String
                        .valueOf(nextChar > 0 ? N.copyOfRange(cbuf, 0, nextChar) : N.copyOfRange(strValue, startIndexForText, endIndexForText));

                if (Strings.isEmpty(numberText)) {
                    return (T) numberText;
                }

                try {
                    final Number num = Numbers.createNumber(numberText);

                    if (num instanceof Float) {
                        final char lastChar = numberText.charAt(numberText.length() - 1);

                        if (!(lastChar == 'f' || lastChar == 'F')) {
                            return (T) Double.valueOf(Numbers.toDouble(num));
                        }
                    }

                    return (T) num;
                } catch (final Exception e) {
                    // Recoverable: values such as Infinity/NaN/"+"/"-" aren't valid numbers here;
                    // fall back to returning the raw text. Log at debug since this is expected.
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to parse: " + numberText + " to Number; returning it as a String");
                    }
                }

                return (T) numberText;
            }
        }

        if (nextChar > 0) {
            return type.valueOf(cbuf, 0, nextChar);
        } else {
            return type.valueOf(strValue, startIndexForText, endIndexForText - startIndexForText);
        }
    }

    /**
     * Removes the trailing Java numeric type suffix ({@code l}, {@code L}, {@code f}, {@code F},
     * {@code d}, {@code D}) that the number tokenizer accepts on an unquoted value.
     *
     * <p>The scalar primitive/boxed handlers accept a suffix, while {@code BigInteger} and
     * {@code BigDecimal} parsers require a bare numeric token. Removing it here lets a cached
     * unquoted number be reparsed by those exact numeric handlers.</p>
     *
     * @param token the raw number token
     * @return {@code token} without its type suffix, or {@code token} itself when it carries none
     */
    private static String stripNumberTypeSuffix(final String token) {
        final int len = token.length();

        if (len > 1) {
            final char lastChar = token.charAt(len - 1);

            if (lastChar == 'l' || lastChar == 'L' || lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
                return token.substring(0, len - 1);
            }
        }

        return token;
    }

    /**
     * Returns whether {@code cls} is one of the integral numeric targets whose {@code Type.valueOf(String)}
     * rejects a fractional spelling outright: the integral primitives, their wrappers and {@code BigInteger}.
     * A {@code BigDecimal} or floating-point target reads {@code "1.5"} fine and is deliberately not listed.
     *
     * @param cls the target class
     * @return {@code true} when a token holding a decimal point cannot be handed to that target's parser
     */
    private static boolean isIntegralNumberClass(final Class<?> cls) {
        return cls == int.class || cls == Integer.class || cls == long.class || cls == Long.class || cls == short.class || cls == Short.class
                || cls == byte.class || cls == Byte.class || cls == BigInteger.class;
    }

    /**
     * Reads property information from the current token using the provided {@code symbolReader}.
     *
     * @param symbolReader the symbol reader to use for lookup
     * @return the property information
     */
    @Override
    public PropInfo readPropInfo(final SymbolReader symbolReader) {
        return (nextChar > 0) ? symbolReader.readPropInfo(cbuf, 0, nextChar) : symbolReader.readPropInfo(strValue, startIndexForText, endIndexForText);
    }

    /**
     * Closes the reader and releases associated resources.
     *
     * @throws UncheckedIOException if closing the optional underlying reader fails
     */
    @Override
    public void close() throws UncheckedIOException {
        if (reader != null) {
            try {
                reader.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /*
     * Copyright (C) 2010 Google Inc.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except compliance
     * with the License. You may obtain a copy of the License at
     *
     * https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License
     * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
     * or implied. See the License for the specific language governing permissions and limitations under
     * the License.
     */

    /**
     * Increases the capacity of the internal character buffer (grows by ~1.75x, with a
     * minimum increment of one character). The minimum increment matters for valid tiny
     * caller-supplied buffers: truncating {@code 1 * 1.75} to an {@code int} otherwise leaves
     * the capacity at one and makes the first required growth fail spuriously.
     *
     * @throws ParsingException if the buffer cannot grow any further (i.e. the new capacity
     *         would not exceed the current capacity, typically because {@link Integer#MAX_VALUE}
     *         has been reached)
     */
    void enlargeCharBuffer() throws ParsingException {
        final long newCapacityLong = Math.max((long) cbufLen + 1, (long) (cbufLen * 1.75));
        final int newCapacity = (int) Math.min(newCapacityLong, Integer.MAX_VALUE);

        if (newCapacity <= cbufLen) {
            throw new ParsingException("Character buffer size exceeded maximum capacity");
        }

        cbuf = N.copyOf(cbuf, newCapacity);
        cbufLen = cbuf.length;
    }

    /**
     * Unescapes the character identified by the character or characters that immediately follow a backslash. The
     * backslash {@code '\'} should have already been read. This supports both unicode escapes (<code>&#92;u000A</code>) and
     * two-character escapes (<code>&#92;n</code>).
     *
     * @return the unescaped character
     * @throws ParsingException if the escape sequence is incomplete or malformed, including
     *         when a unicode escape sequence contains invalid hex digits
     */
    protected char readEscapeCharacter() throws ParsingException {
        if (strBeginIndex >= strEndIndex) {
            throw new ParsingException("Incomplete escape sequence at end of input");
        }

        final int escaped = strValue[strBeginIndex++];

        switch (escaped) {
            case 'u':

                // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
                char result = 0;

                for (int i = 0, c = 0; i < 4; i++) {
                    if (strBeginIndex >= strEndIndex) {
                        throw new ParsingException("Incomplete unicode escape sequence: expected 4 hex digits");
                    }

                    c = strValue[strBeginIndex++];

                    result <<= 4;

                    if ((c >= '0') && (c <= '9')) {
                        result += (char) (c - '0');
                    } else if ((c >= 'a') && (c <= 'f')) {
                        result += (char) (c - 'a' + 10);
                    } else if ((c >= 'A') && (c <= 'F')) {
                        result += (char) (c - 'A' + 10);
                    } else {
                        throw new ParsingException("Number format exception: invalid hex digit '" + (char) c + "' in unicode escape sequence");
                    }
                }

                return result;

            case 't':
                return '\t';

            case 'b':
                return '\b';

            case 'n':
                return '\n';

            case 'r':
                return '\r';

            case 'f':
                return '\f';

            // // fall-through
            // case '\'':
            // case '"':
            // case '\\':
            default:
                return (char) escaped;
        }
    }
}
