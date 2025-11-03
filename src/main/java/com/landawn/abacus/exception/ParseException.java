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

package com.landawn.abacus.exception;

import java.io.Serial;

/**
 * Exception thrown when a parsing operation fails.
 * This is a runtime exception that indicates an error occurred while parsing text, data structures,
 * or other formatted input. Unlike {@link java.text.ParseException}, this is an unchecked exception.
 * 
 * <p>This exception can optionally store a token value that indicates the position or type of token
 * where the parsing error occurred. The default token value is -2 when not specified.</p>
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>JSON/XML parsing failures</li>
 *   <li>Custom data format parsing errors</li>
 *   <li>Configuration file parsing problems</li>
 *   <li>Query language or expression parsing failures</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic usage
 * if (!isValidFormat(input)) {
 *     throw new ParseException("Invalid date format: " + input);
 * }
 * 
 * // With token information
 * if (unexpectedToken) {
 *     throw new ParseException(currentToken, "Unexpected token at position " + position);
 * }
 * }</pre>
 * 
 * @see RuntimeException
 * @see java.text.ParseException
 */
public class ParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7678894353902496315L;

    /** The token value associated with this parse exception. Default value is -2. */
    private int token = -2; //NOSONAR

    /**
     * Constructs a new {@code ParseException} with no detail message.
     * The cause is not initialized, and the token value is set to the default of -2.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ParseException();
     * }</pre>
     */
    public ParseException() {
    }

    /**
     * Constructs a new {@code ParseException} with the specified detail message.
     * The cause is not initialized, and the token value is set to the default of -2.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ParseException("Malformed JSON at line " + lineNumber);
     * }</pre>
     *
     * @param message the detail message which provides information about the parsing error.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ParseException} with the specified token and detail message.
     * This constructor is useful when you want to indicate the specific token or position
     * where the parsing error occurred.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Token could represent a position, token type, or error code
     * throw new ParseException(42, "Unexpected character '}' at position 42");
     * }</pre>
     *
     * @param token an integer value representing the token, position, or error code where the parsing failed.
     *              This value can be retrieved later using {@link #getToken()}.
     * @param message the detail message which provides information about the parsing error.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ParseException(int token, String message) {
        super(message);
        this.token = token;
    }

    /**
     * Constructs a new {@code ParseException} with the specified detail message and cause.
     * The token value is set to the default of -2.
     * 
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     return xmlParser.parse(document);
     * } catch (SAXException e) {
     *     throw new ParseException("Failed to parse XML document", e);
     * }
     * }</pre>
     *
     * @param message the detail message which provides information about the parsing error.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ParseException} with the specified cause and a detail
     * message of {@code (cause==null ? {@code null} : cause.toString())} (which typically contains the
     * class and detail message of {@code cause}). The token value is set to the default of -2.
     * 
     * <p>This constructor is useful for exceptions that are little more than wrappers for other
     * throwables.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (IOException e) {
     *     throw new ParseException(e);
     * }
     * }</pre>
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ParseException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns the token value associated with this parsing exception.
     * The token can represent various things depending on the context:
     * <ul>
     *   <li>The position in the input where parsing failed</li>
     *   <li>The type of token that caused the error</li>
     *   <li>An error code specific to the parser</li>
     *   <li>-2 if no token was specified (default value)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     parseExpression(input);
     * } catch (ParseException e) {
     *     System.err.println("Parse error at token: " + e.getToken());
     * }
     * }</pre>
     *
     * @return the token value where the parsing error occurred, or -2 if not specified
     */
    public int getToken() {
        return token;
    }
}
