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
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

/**
 * Internal interface for reading JSON tokens and values.
 * This interface defines the low-level JSON parsing operations used by the JSON parser
 * implementation. It provides methods for tokenizing JSON input and reading values
 * according to their types.
 * 
 * <p>Token Constants:</p>
 * <ul>
 *   <li>{@link #EOF} - End of file/stream marker</li>
 *   <li>{@link #START_BRACE} - Opening brace '{' for objects</li>
 *   <li>{@link #END_BRACE} - Closing brace '}' for objects</li>
 *   <li>{@link #START_BRACKET} - Opening bracket '[' for arrays</li>
 *   <li>{@link #END_BRACKET} - Closing bracket ']' for arrays</li>
 *   <li>{@link #START_QUOTATION_D} - Double quote '"' start</li>
 *   <li>{@link #END_QUOTATION_D} - Double quote '"' end</li>
 *   <li>{@link #START_QUOTATION_S} - Single quote '\'' start</li>
 *   <li>{@link #END_QUOTATION_S} - Single quote '\'' end</li>
 *   <li>{@link #COLON} - Colon ':' separator</li>
 *   <li>{@link #COMMA} - Comma ',' separator</li>
 * </ul>
 * 
 * <p>This is an internal interface and should not be used directly by application code.</p>
 * 
 * @author HaiYang Li
 * @since 0.8
 */
interface JSONReader {
    // < 32 = ' ' (Space) White space
    /**
     * End of file marker. Returned when no more input is available.
     */
    int EOF = -1;

    /**
     * Undefined token. Initial state or when no specific token is identified.
     */
    int UNDEFINED = 0;

    /**
     * Start brace token '{'. Indicates the beginning of a JSON object.
     */
    int START_BRACE = 1;

    /**
     * End brace token '}'. Indicates the end of a JSON object.
     */
    int END_BRACE = 2;

    /**
     * Start bracket token '['. Indicates the beginning of a JSON array.
     */
    int START_BRACKET = 3;

    /**
     * End bracket token ']'. Indicates the end of a JSON array.
     */
    int END_BRACKET = 4;

    /**
     * Start double quotation token '"'. Indicates the beginning of a double-quoted string.
     */
    int START_QUOTATION_D = 5;

    /**
     * End double quotation token '"'. Indicates the end of a double-quoted string.
     */
    int END_QUOTATION_D = 6;

    /**
     * Start single quotation token '\''. Indicates the beginning of a single-quoted string.
     * Note: Single quotes are not standard JSON but may be supported for flexibility.
     */
    int START_QUOTATION_S = 7;

    /**
     * End single quotation token '\''. Indicates the end of a single-quoted string.
     * Note: Single quotes are not standard JSON but may be supported for flexibility.
     */
    int END_QUOTATION_S = 8;

    /**
     * Colon token ':'. Used as the key-value separator in JSON objects.
     */
    int COLON = 9;

    /**
     * Comma token ','. Used as the element separator in JSON objects and arrays.
     */
    int COMMA = 10;

    // > 32 = ' ' (Space)

    /**
     * Reads the previous token from the JSON input.
     * This method allows backtracking to the last token read,
     * which can be useful for re-evaluating or skipping tokens.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * int token = reader.nextToken();
     * if (token == SOME_TOKEN) {
     *     // Do something with the token
     * } else {
     *     // Backtrack to the previous token
     *     reader.prevToken();
     * }
     * }</pre>
     *
     * @return the identifier of the previous token, or -1 if no previous token exists
     */
    int lastToken();

    /**
     * Reads and returns the next token from the JSON input.
     * This method advances the reader position and identifies the next
     * structural token in the JSON stream.
     * 
     * <p>The returned value will be one of the defined token constants,
     * or -1 if no next symbol is found (EOF).</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * int token = reader.nextToken();
     * if (token == START_BRACE) {
     *     // Beginning of JSON object
     * }
     * }</pre>
     *
     * @return the token identifier, or -1 if no next symbol is found
     * @throws UncheckedIOException if an I/O error occurs during reading
     */
    int nextToken() throws UncheckedIOException;

    // int nextNameToken() throws UncheckedIOException;

    /**
     * Checks if the reader has text content available.
     * This is typically true after reading a string token or value token
     * (like numbers, booleans, or null).
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * if (reader.hasText()) {
     *     String value = reader.getText();
     * }
     * }</pre>
     *
     * @return {@code true} if text content is available, {@code false} otherwise
     */
    boolean hasText();

    /**
     * Gets the text content from the last read token.
     * This method should only be called when {@link #hasText()} returns true.
     * The returned text represents the value of the last parsed token, such as
     * a string value, number, boolean, or null.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * int token = reader.nextToken();
     * if (reader.hasText()) {
     *     String value = reader.getText();
     *     // Process the value
     * }
     * }</pre>
     *
     * @return the text content of the last token
     */
    String getText();

    /**
     * Reads and converts the current value according to the specified type.
     * This method reads the current token value and converts it to the
     * requested type using the type's conversion logic.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * Type<Integer> intType = N.typeOf(Integer.class);
     * Integer value = reader.readValue(intType);
     * 
     * Type<Person> personType = N.typeOf(Person.class);
     * Person person = reader.readValue(personType);
     * }</pre>
     *
     * @param <T> the target type
     * @param type the type to convert the value to
     * @return the converted value
     */
    <T> T readValue(Type<? extends T> type);

    /**
     * Reads property information using the provided symbol reader.
     * This method is used internally for efficient property name resolution
     * during object deserialization.
     * 
     * <p>The symbol reader provides optimized property lookup based on
     * character buffers or strings.</p>
     *
     * @param symbolReader the symbol reader for property resolution
     * @return the property information, or null if not found
     */
    PropInfo readPropInfo(SymbolReader symbolReader);

    /**
     * Closes the reader and releases any associated resources.
     * This method should be called when the reader is no longer needed
     * to ensure proper resource cleanup.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * JSONReader reader = null;
     * try {
     *     reader = createReader(input);
     *     // Use the reader
     * } finally {
     *     if (reader != null) {
     *         reader.close();
     *     }
     * }
     * }</pre>
     *
     * @throws UncheckedIOException if an I/O error occurs during closing
     */
    void close() throws UncheckedIOException;

    /**
     * Interface for efficient property name resolution during JSON parsing.
     * This interface provides methods for looking up property information
     * based on property names, with optimizations for character buffer access.
     * 
     * <p>This is an internal interface used for performance optimization
     * and should not be implemented by application code.</p>
     * 
     * @author HaiYang Li
     * @since 0.8
     */
    interface SymbolReader {
        /**
         * Gets property information for the specified property name.
         * This method performs a lookup to find the property metadata
         * associated with the given name.
         * 
         * <p>Usage example:</p>
         * <pre>{@code
         * PropInfo propInfo = symbolReader.getPropInfo("firstName");
         * if (propInfo != null) {
         *     // Use property information for deserialization
         * }
         * }</pre>
         *
         * @param propName the property name to look up
         * @return the property information, or null if not found
         */
        PropInfo getPropInfo(String propName);

        /**
         * Reads property information from a character buffer.
         * This method provides optimized property lookup by reading directly
         * from a character buffer without creating intermediate strings.
         * 
         * <p>This is a performance optimization for high-throughput parsing
         * scenarios where string allocation overhead should be minimized.</p>
         * 
         * <p>Usage example:</p>
         * <pre>{@code
         * char[] buffer = {'f', 'i', 'r', 's', 't', 'N', 'a', 'm', 'e'};
         * PropInfo propInfo = symbolReader.readPropInfo(buffer, 0, 9);
         * }</pre>
         *
         * @param cbuf the character buffer containing the property name
         * @param fromIndex the starting index (inclusive)
         * @param toIndex the ending index (exclusive)
         * @return the property information, or null if not found
         */
        PropInfo readPropInfo(char[] cbuf, int fromIndex, int toIndex);

        // PropInfo readPropInfo(String str, int fromIndex, int toIndex);
    }
}