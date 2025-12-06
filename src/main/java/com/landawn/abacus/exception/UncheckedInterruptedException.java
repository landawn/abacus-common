/*
 * Copyright (C) 2023 HaiYang Li
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
 * A runtime exception that wraps {@link InterruptedException}, allowing thread interruption
 * exceptions to be thrown without being declared in method signatures.
 * 
 * <p>This exception is useful in contexts where you need to handle InterruptedException but
 * cannot change the method signature to declare it, such as in:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces that don't declare checked exceptions</li>
 *   <li>Stream operations where checked exceptions are not allowed</li>
 *   <li>Implementing interfaces that don't declare InterruptedException</li>
 * </ul>
 * 
 * <p><strong>Important:</strong> When catching and wrapping InterruptedException, it's generally
 * recommended to preserve the thread's interrupted status by calling {@code Thread.currentThread().interrupt()}
 * before throwing this exception, unless the interruption has been fully handled.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a lambda expression
 * list.parallelStream().forEach(item -> {
 *     try {
 *         Thread.sleep(1000);
 *     } catch (InterruptedException e) {
 *         Thread.currentThread().interrupt();   // Restore interrupted status
 *         throw new UncheckedInterruptedException("Processing interrupted", e);
 *     }
 * });
 * 
 * // In a method that can't declare InterruptedException
 * public void process() {
 *     try {
 *         blockingQueue.take();
 *     } catch (InterruptedException e) {
 *         Thread.currentThread().interrupt();
 *         throw new UncheckedInterruptedException(e);
 *     }
 * }
 * }</pre>
 * 
 * @see UncheckedException
 * @see InterruptedException
 * @see Thread#interrupt()
 */
public class UncheckedInterruptedException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = -3613481776785882140L;

    /**
     * Constructs a new {@code UncheckedInterruptedException} by wrapping the specified {@link InterruptedException}.
     * 
     * <p>This constructor preserves all information from the original InterruptedException including
     * its message, stack trace, and any suppressed exceptions.</p>
     * 
     * <p><strong>Note:</strong> Remember to restore the thread's interrupted status before throwing
     * this exception if the interruption hasn't been fully handled:</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (InterruptedException e) {
     *     Thread.currentThread().interrupt();   // Restore interrupted status
     *     throw new UncheckedInterruptedException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link InterruptedException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedInterruptedException(final InterruptedException cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UncheckedInterruptedException} with the specified detail message
     * and {@link InterruptedException}.
     *
     * <p>This constructor allows you to provide additional context about where or why the
     * interruption occurred, while preserving all information from the original exception.</p>
     *
     * <p><strong>Note:</strong> Remember to restore the thread's interrupted status before throwing
     * this exception if the interruption hasn't been fully handled:</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (InterruptedException e) {
     *     Thread.currentThread().interrupt();   // Restore interrupted status
     *     throw new UncheckedInterruptedException("Interrupted while waiting for resource", e);
     * }
     * }</pre>
     *
     * @param message the detail message providing context about the interruption.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the {@link InterruptedException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedInterruptedException(final String message, final InterruptedException cause) {
        super(message, cause);
    }
}
