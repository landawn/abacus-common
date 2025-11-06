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
 * A runtime exception that wraps {@link java.text.ParseException}, allowing parsing exceptions
 * to be thrown without being declared in method signatures.
 * 
 * <p>This exception is useful when working with date/time parsing, number formatting, or other
 * text parsing operations in contexts where checked exceptions cannot be declared, such as:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces</li>
 *   <li>Stream operations for parsing collections of strings</li>
 *   <li>Implementing interfaces that don't declare ParseException</li>
 *   <li>Simplifying error handling in parsing-heavy code</li>
 * </ul>
 * 
 * <p>Note the distinction between this class and {@link com.landawn.abacus.exception.ParseException}:</p>
 * <ul>
 *   <li>{@link com.landawn.abacus.exception.ParseException} - A runtime exception for general parsing errors</li>
 *   <li>{@code UncheckedParseException} - Specifically wraps {@link java.text.ParseException}</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a stream operation parsing dates
 * SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
 * List<Date> dates = dateStrings.stream()
 *     .map(dateStr -> {
 *         try {
 *             return dateFormat.parse(dateStr);
 *         } catch (java.text.ParseException e) {
 *             throw new UncheckedParseException("Invalid date format: " + dateStr, e);
 *         }
 *     })
 *     .collect(Collectors.toList());
 *
 * // In a lambda expression
 * NumberFormat numberFormat = NumberFormat.getInstance();
 * Function<String, Number> parser = str -> {
 *     try {
 *         return numberFormat.parse(str);
 *     } catch (java.text.ParseException e) {
 *         throw new UncheckedParseException(e);
 *     }
 * };
 * }</pre>
 * 
 * @see UncheckedException
 * @see java.text.ParseException
 * @see com.landawn.abacus.exception.ParseException
 */
public class UncheckedParseException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = -2290836899125890438L;

    /**
     * Constructs a new {@code UncheckedParseException} by wrapping the specified {@link java.text.ParseException}.
     * 
     * <p>This constructor preserves all information from the original ParseException including
     * its message, error offset, stack trace, and any suppressed exceptions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
     *     return sdf.parse(dateString);
     * } catch (java.text.ParseException e) {
     *     throw new UncheckedParseException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link java.text.ParseException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedParseException(final java.text.ParseException cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UncheckedParseException} with the specified detail message
     * and {@link java.text.ParseException}.
     *
     * <p>This constructor allows you to provide additional context about what was being parsed
     * and why it failed, while preserving all information from the original exception including
     * the error offset.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     DecimalFormat df = new DecimalFormat("#,##0.00");
     *     return df.parse(amountStr);
     * } catch (java.text.ParseException e) {
     *     throw new UncheckedParseException(
     *         "Failed to parse currency amount: '" + amountStr + "'", e);
     * }
     * }</pre>
     *
     * @param message the detail message providing context about the parsing failure.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the {@link java.text.ParseException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedParseException(final String message, final java.text.ParseException cause) {
        super(message, cause);
    }
}
