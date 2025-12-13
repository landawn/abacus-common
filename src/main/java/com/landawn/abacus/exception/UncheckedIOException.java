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

import java.io.IOException;
import java.io.Serial;

/**
 * A runtime exception that wraps {@link IOException}, allowing I/O exceptions to be thrown
 * without being declared in method signatures.
 * 
 * <p>This exception is particularly useful in contexts where IOException cannot be declared, such as:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces</li>
 *   <li>Stream operations</li>
 *   <li>Implementing interfaces that don't declare IOException</li>
 *   <li>Simplifying exception handling in I/O-heavy code</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> Java 8+ includes its own {@link java.io.UncheckedIOException}.
 * This class predates the Java 8 version and may be retained for backward compatibility
 * or to maintain consistency with other unchecked exceptions in this framework.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a stream operation
 * files.stream()
 *     .map(file -> {
 *         try {
 *             return Files.readAllLines(file);
 *         } catch (IOException e) {
 *             throw new UncheckedIOException("Failed to read file: " + file, e);
 *         }
 *     })
 *     .forEach(System.out::println);
 * 
 * // In a lambda expression
 * Supplier<String> fileReader = () -> {
 *     try {
 *         return new String(Files.readAllBytes(path));
 *     } catch (IOException e) {
 *         throw new UncheckedIOException(e);
 *     }
 * };
 * }</pre>
 * 
 * @see UncheckedException
 * @see IOException
 * @see java.io.UncheckedIOException
 */
public class UncheckedIOException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = -8702336402043331418L;

    /**
     * Constructs a new {@code UncheckedIOException} by wrapping the specified {@link IOException}.
     *
     * <p>This constructor preserves all information from the original IOException including
     * its message, stack trace, and any suppressed exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     fileInputStream.read();
     * } catch (IOException e) {
     *     throw new UncheckedIOException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link IOException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedIOException(final IOException cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UncheckedIOException} with the specified detail message
     * and {@link IOException}.
     *
     * <p>This constructor allows you to provide additional context about the I/O operation
     * that failed, while preserving all information from the original exception.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Files.copy(source, target);
     * } catch (IOException e) {
     *     throw new UncheckedIOException(
     *         "Failed to copy file from " + source + " to " + target, e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the {@link IOException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedIOException(final String message, final IOException cause) {
        super(message, cause);
    }
}
