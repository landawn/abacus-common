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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for JSON readers that provides common functionality and constants
 * for parsing JSON documents. This class implements the {@link JsonReader} interface
 * and serves as the foundation for various JSON reading implementations.
 *
 * <p>This class defines essential constants and lookup tables used for efficient JSON parsing,
 * including character event mappings, special value constants, and numeric parsing utilities.</p>
 *
 * <p>Key features provided:</p>
 * <ul>
 *   <li><strong>Character Event Mapping:</strong> Fast lookup tables for JSON structural characters</li>
 *   <li><strong>Numeric Constants:</strong> Pre-computed powers of ten for efficient number parsing</li>
 *   <li><strong>Special Value Constants:</strong> Interned string constants for {@code null}, {@code true}, and {@code false}</li>
 *   <li><strong>Alphanumeric Support:</strong> Extended character mappings for various JSON tokens</li>
 * </ul>
 *
 * <p>The character event mapping system allows for efficient parsing by providing direct
 * index-based lookup for ASCII characters (0-127), making JSON token recognition very fast.</p>
 *
 * <p>Subclasses should implement the specific parsing logic while leveraging these
 * common constants and utilities for consistent and efficient JSON processing.</p>
 *
 * @see JsonReader
 * @see Type
 */
abstract class AbstractJsonReader implements JsonReader { //NOSONAR
    /**
     * Largest digit count that the fast integer path can accumulate into a {@code long} without
     * risking overflow; longer numbers fall back to the exact parser.
     */
    static final int MAX_PARSABLE_NUM_LEN = Long.toString(Long.MAX_VALUE, 10).length() - 1;

    /** Powers of ten indexed by exponent, used to scale a decimal mantissa without string conversion. */
    static final long[] POWERS_OF_TEN = { 1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L,
            100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
            100_000_000_000_000_000L, 1_000_000_000_000_000_000L };

    /** Interned {@code "null"} literal, returned by identity so callers can compare with {@code ==}. */
    static final String NULL = Strings.NULL;

    /** Interned {@code "false"} literal, returned by identity so callers can compare with {@code ==}. */
    static final String FALSE = Boolean.FALSE.toString().intern();

    /** Interned {@code "true"} literal, returned by identity so callers can compare with {@code ==}. */
    static final String TRUE = Boolean.TRUE.toString().intern();

    /**
     * Lookup table mapping an ASCII character (0-127) to its token identifier. Structural characters
     * map to the {@link JsonReader} token constants (all below 32); value-leading characters map to
     * themselves. A zero entry means the character is not significant on its own.
     */
    protected static final int[] charEvents = new int[128];

    static {
        charEvents[','] = COMMA;
        charEvents[':'] = COLON;
        charEvents['"'] = START_DOUBLE_QUOTE;
        charEvents['\''] = START_SINGLE_QUOTE;
        charEvents['{'] = START_BRACE;
        charEvents['}'] = END_BRACE;
        charEvents['['] = START_BRACKET;
        charEvents[']'] = END_BRACKET;
        charEvents['n'] = 'n';
        charEvents['N'] = 'N';
        charEvents['f'] = 'f';
        charEvents['F'] = 'F';
        charEvents['t'] = 't';
        charEvents['T'] = 'T';
        charEvents['-'] = '-';
        charEvents['+'] = '+';
        charEvents['0'] = '0';
        charEvents['1'] = '1';
        charEvents['2'] = '2';
        charEvents['3'] = '3';
        charEvents['4'] = '4';
        charEvents['5'] = '5';
        charEvents['6'] = '6';
        charEvents['7'] = '7';
        charEvents['8'] = '8';
        charEvents['9'] = '9';
    }

    /**
     * Copy of {@link #charEvents} extended with the extra characters that may appear inside an
     * unquoted numeric or literal token (sign, decimal point, exponent, radix and type-suffix letters).
     */
    static final int[] alphanumerics = charEvents.clone();

    static {
        alphanumerics['+'] = '+';
        alphanumerics['-'] = '-';
        alphanumerics['.'] = '.';
        alphanumerics['#'] = '#';

        alphanumerics['x'] = 'x';
        alphanumerics['X'] = 'X';

        alphanumerics['e'] = 'e';
        alphanumerics['E'] = 'E';

        alphanumerics['a'] = 'a';
        alphanumerics['b'] = 'b';
        alphanumerics['c'] = 'c';
        alphanumerics['d'] = 'd';
        alphanumerics['f'] = 'f';

        alphanumerics['l'] = 'l';
        alphanumerics['L'] = 'L';
        alphanumerics['F'] = 'F';
        alphanumerics['D'] = 'D';
    }

    /**
     * Reverse mapping from token integer constants (e.g. {@link JsonReader#START_BRACE}) to
     * the corresponding character literal. Index 0 ({@link JsonReader#UNDEFINED}) maps to
     * {@code '\0'} (the {@code null} character, i.e. no entry).
     */
    protected static final char[] eventChars = new char[11];

    static {
        eventChars[START_BRACE] = '{';
        eventChars[END_BRACE] = '}';
        eventChars[START_BRACKET] = '[';
        eventChars[END_BRACKET] = ']';
        eventChars[START_DOUBLE_QUOTE] = '"';
        eventChars[END_DOUBLE_QUOTE] = '"';
        eventChars[START_SINGLE_QUOTE] = '\'';
        eventChars[END_SINGLE_QUOTE] = '\'';
        eventChars[COLON] = ':';
        eventChars[COMMA] = ',';
    }

    /** Cached {@code String} type, used as the default value-type hint by {@link #nextToken()}. */
    protected static final Type<String> strType = Type.of(String.class);

    /**
     * Reads and returns the next token from the JSON input using {@code String} as the default expected
     * value type hint. This is a convenience method that delegates to {@link #nextToken(Type)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonReader reader = JsonStringReader.parse("{\"name\":\"John\"}", new char[256]);
     * int token = reader.nextToken();
     * if (token == JsonReader.START_BRACE) {
     *     // Process JSON object
     * }
     * }</pre>
     *
     * @return the token identifier, or {@link JsonReader#EOF} ({@code -1}) if the end of input is reached
     * @throws UncheckedIOException if an I/O error occurs during reading
     */
    @Override
    public int nextToken() throws UncheckedIOException {
        return nextToken(strType);
    }
}
