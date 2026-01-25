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
 *   <li><strong>Special Value Constants:</strong> Interned string constants for {@code null}, true, and false</li>
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
    static final int MAX_PARSABLE_NUM_LEN = Long.toString(Long.MAX_VALUE, 10).length() - 1;

    static final long[] POWERS_OF_TEN = { 1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L,
            100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
            100_000_000_000_000_000L, 1_000_000_000_000_000_000L };

    static final String NULL = Strings.NULL;
    static final String FALSE = Boolean.FALSE.toString().intern();
    static final String TRUE = Boolean.TRUE.toString().intern();

    protected static final int[] charEvents = new int[128];

    static {
        charEvents[','] = COMMA;
        charEvents[':'] = COLON;
        charEvents['"'] = START_QUOTATION_D;
        charEvents['\''] = START_QUOTATION_S;
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

    protected static final char[] eventChars = new char[11];

    static {
        eventChars[START_BRACE] = '{';
        eventChars[END_BRACE] = '}';
        eventChars[START_BRACKET] = '[';
        eventChars[END_BRACKET] = ']';
        eventChars[START_QUOTATION_D] = '"';
        eventChars[END_QUOTATION_D] = '"';
        eventChars[START_QUOTATION_S] = '\'';
        eventChars[END_QUOTATION_S] = '\'';
        eventChars[COLON] = ':';
        eventChars[COMMA] = ',';
    }

    protected static final Type<String> strType = Type.of(String.class);

    /**
     * Reads and returns the next token from the JSON input using String as the default expected type.
     * This is a convenience method that delegates to {@link #nextToken(Type)} with String type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonReader reader = // obtain reader
     * int token = reader.nextToken();
     * if (token == START_BRACE) {
     *     // Process JSON object
     * }
     * }</pre>
     *
     * @return the token identifier, or -1 if no next token is found
     * @throws UncheckedIOException if an I/O error occurs during reading
     */
    @Override
    public int nextToken() throws UncheckedIOException {
        return nextToken(strType);
    }
}
