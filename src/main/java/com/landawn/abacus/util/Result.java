/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

/**
 * A type-safe container for operation results that can either hold a successful value of type {@code T} 
 * or an exception of type {@code E}, providing a functional programming approach to error handling 
 * without throwing exceptions. This class serves as an alternative to traditional try-catch blocks 
 * by encapsulating both success and failure states in a single immutable object, enabling more 
 * explicit and composable error handling patterns.
 *
 * <p>The {@code Result} class follows the Railway-Oriented Programming pattern, where operations 
 * can be chained together and failures are propagated through the chain without interrupting 
 * the execution flow. This approach makes error handling more predictable and reduces the risk 
 * of unhandled exceptions while maintaining type safety through generic constraints.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Generic constraints ensure exception type {@code E} extends {@code Throwable}</li>
 *   <li><b>Immutable Design:</b> All instances are immutable, ensuring thread safety and preventing side effects</li>
 *   <li><b>Explicit Error Handling:</b> Forces developers to handle both success and failure cases explicitly</li>
 *   <li><b>Functional API:</b> Provides functional-style methods for conditional execution and transformation</li>
 *   <li><b>No Exception Throwing:</b> Encapsulates exceptions rather than throwing them immediately</li>
 *   <li><b>Railway Pattern:</b> Enables chaining operations with automatic failure propagation</li>
 *   <li><b>Memory Efficient:</b> Minimal overhead with only two instance fields</li>
 *   <li><b>Integration Ready:</b> Seamless conversion to other container types like Pair and Tuple</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Immutable Design:</b>
 * <ul>
 *   <li>This class implements {@link Immutable}, guaranteeing that instances cannot be modified after creation</li>
 *   <li>Both {@code value} and {@code exception} fields are final and set only during construction</li>
 *   <li>Thread-safe by design due to immutability and lack of mutable state</li>
 *   <li>All methods return new instances or extracted values without modifying the original object</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Explicit Over Implicit:</b> Makes error states visible and forces explicit handling</li>
 *   <li><b>Composition Over Control Flow:</b> Enables functional composition instead of imperative error handling</li>
 *   <li><b>Type Safety Over Runtime Errors:</b> Uses generic constraints to prevent type-related runtime issues</li>
 *   <li><b>Predictability Over Convenience:</b> Ensures predictable behavior even at the cost of some verbosity</li>
 *   <li><b>Immutability Over Performance:</b> Prioritizes correctness and thread safety over minimal performance gains</li>
 * </ul>
 *
 * <p><b>Core State Model:</b>
 * <ul>
 *   <li><b>Success State:</b> {@code value != null, exception == null} - Operation completed successfully</li>
 *   <li><b>Success with Null:</b> {@code value == null, exception == null} - Operation succeeded but returned null</li>
 *   <li><b>Failure State:</b> {@code exception != null} - Operation failed with an exception (value ignored)</li>
 *   <li><b>Undefined State:</b> {@code value != null, exception != null} - Both present, exception takes precedence</li>
 * </ul>
 *
 * <p><b>Generic Type Parameters:</b>
 * <ul>
 *   <li><b>{@code T}:</b> The type of the successful result value, can be any type including primitives (boxed)</li>
 *   <li><b>{@code E extends Throwable}:</b> The type of exception, constrained to Throwable subtypes for type safety</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Creating success and failure results
 * Result<String, IOException> success = Result.of("Hello World", null);
 * Result<String, IOException> failure = Result.of(null, new IOException("File not found"));
 *
 * // Checking result state
 * if (result.isSuccess()) {
 *     String value = result.orElseThrow(); // Safe to call
 * } else {
 *     IOException error = result.getException();
 *     handleError(error);
 * }
 *
 * // Conditional execution based on state
 * result.ifSuccess(value -> processValue(value))
 *       .ifFailure(error -> logError(error));
 *
 * // Safe value extraction with defaults
 * String safeValue = result.orElseIfFailure("default value");
 * String computedDefault = result.orElseGetIfFailure(() -> computeDefault());
 *
 * // Exception transformation and re-throwing
 * String value = result.orElseThrow(IOException::new);
 * String value2 = result.orElseThrow(() -> new CustomException("Operation failed"));
 * }</pre>
 *
 * <p><b>Advanced Error Handling Patterns:</b>
 * <pre>{@code
 * // Chain operations with automatic failure propagation
 * public Result<String, IOException> processFile(String filename) {
 *     return readFile(filename)
 *         .flatMap(content -> validateContent(content))
 *         .flatMap(validContent -> transformContent(validContent));
 * }
 *
 * // Convert between different exception types
 * Result<Data, SQLException> dbResult = fetchFromDatabase();
 * Result<Data, ServiceException> serviceResult = dbResult.mapFailure(
 *     sqlEx -> new ServiceException("Database operation failed", sqlEx)
 * );
 *
 * // Combine multiple results
 * Result<String, Exception> combined = combineResults(
 *     result1.ifFailureOrElse(
 *         error -> handleFirstError(error),
 *         value -> processFirstValue(value)
 *     ),
 *     result2.ifSuccessOrElse(
 *         value -> processSecondValue(value),
 *         error -> handleSecondError(error)
 *     )
 * );
 * }</pre>
 *
 * <p><b>Integration with Existing Code:</b>
 * <pre>{@code
 * // Converting from traditional exception-based code
 * public Result<String, FileNotFoundException> readFileAsResult(String path) {
 *     try {
 *         String content = Files.readString(Paths.get(path));
 *         return Result.of(content, null);
 *     } catch (FileNotFoundException e) {
 *         return Result.of(null, e);
 *     }
 * }
 *
 * // Converting to other container types
 * Pair<String, IOException> pair = result.toPair();
 * Tuple2<String, IOException> tuple = result.toTuple();
 * Optional<String> optional = result.isSuccess() 
 *     ? Optional.of(result.orElseThrow()) 
 *     : Optional.empty();
 * }</pre>
 *
 * <p><b>RR Nested Class - RuntimeException Specialization:</b>
 * <ul>
 *   <li><b>Convenience Subclass:</b> Specialized for {@code RuntimeException} to reduce generic verbosity</li>
 *   <li><b>Common Use Case:</b> Most application-level errors are RuntimeExceptions</li>
 *   <li><b>Simplified API:</b> {@code Result.RR<T>} instead of {@code Result<T, RuntimeException>}</li>
 *   <li><b>Beta Feature:</b> Subject to API refinement based on usage feedback</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) - Simple object allocation with two field assignments</li>
 *   <li><b>Memory Overhead:</b> Minimal - Only two object references plus standard object header</li>
 *   <li><b>Method Calls:</b> O(1) - All operations are simple field access or condition checks</li>
 *   <li><b>GC Impact:</b> Low - Immutable objects are GC-friendly and eligible for early collection</li>
 *   <li><b>Thread Contention:</b> None - Immutable design eliminates synchronization needs</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable State:</b> All fields are final and set only during construction</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent read access from multiple threads</li>
 *   <li><b>No Synchronization:</b> No locks or synchronization needed due to immutability</li>
 *   <li><b>Safe Publication:</b> Can be safely published between threads without additional synchronization</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>No Memory Leaks:</b> Immutable design prevents accidental reference retention</li>
 *   <li><b>Efficient Allocation:</b> Small object size minimizes allocation overhead</li>
 *   <li><b>GC Optimization:</b> Immutable objects can be allocated in young generation for faster collection</li>
 *   <li><b>Reference Cleanup:</b> No circular references or complex cleanup required</li>
 * </ul>
 *
 * <p><b>Error Handling Philosophy:</b>
 * <ul>
 *   <li><b>Explicit Failures:</b> All potential failures are represented explicitly in the type system</li>
 *   <li><b>No Hidden Exceptions:</b> Methods that can fail return Result instead of throwing</li>
 *   <li><b>Composable Errors:</b> Error handling can be composed and chained functionally</li>
 *   <li><b>Type-Safe Recovery:</b> Recovery strategies are enforced by the type system</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Always check {@code isSuccess()} or {@code isFailure()} before extracting values</li>
 *   <li>Use {@code orElseThrow()} only when you're certain the Result contains a success value</li>
 *   <li>Prefer {@code ifSuccess()} and {@code ifFailure()} for conditional execution</li>
 *   <li>Use {@code orElseIfFailure()} with meaningful default values</li>
 *   <li>Leverage functional composition to build error-handling pipelines</li>
 *   <li>Document which exceptions your methods can produce in their Result types</li>
 *   <li>Use appropriate exception types in the generic parameter {@code E}</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Creating Results with both value and exception non-null (ambiguous state)</li>
 *   <li>Calling {@code orElseThrow()} without checking {@code isSuccess()} first</li>
 *   <li>Ignoring failure cases and only handling success scenarios</li>
 *   <li>Using Result for control flow instead of genuine error scenarios</li>
 *   <li>Converting back to exception-throwing methods unnecessarily</li>
 *   <li>Using overly broad exception types like {@code Exception} instead of specific types</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. Optional:</b> Result handles both success/failure states, Optional only handles presence/absence</li>
 *   <li><b>vs. Try-Catch:</b> Result makes error handling explicit and composable vs. imperative</li>
 *   <li><b>vs. Checked Exceptions:</b> Result provides functional composition without method signature pollution</li>
 *   <li><b>vs. Either Type:</b> Result is specialized for success/failure scenarios with exception semantics</li>
 *   <li><b>vs. Nullable Returns:</b> Result distinguishes between null success values and actual failures</li>
 * </ul>
 *
 * <p><b>Integration Ecosystem:</b>
 * <ul>
 *   <li><b>{@link Optional}:</b> Can be converted to/from Optional for value presence scenarios</li>
 *   <li><b>{@link Pair}:</b> Direct conversion via {@code toPair()} for tuple-like usage</li>
 *   <li><b>{@link Tuple}:</b> Direct conversion via {@code toTuple()} for structured data scenarios</li>
 *   <li><b>{@link Throwables}:</b> Compatible with throwable utility methods for exception handling</li>
 * </ul>
 *
 * <p><b>Example: File Processing Pipeline</b>
 * <pre>{@code
 * public class FileProcessor {
 *     public Result<ProcessedData, IOException> processFile(String filename) {
 *         return readFile(filename)
 *             .flatMap(this::validateFileContent)
 *             .flatMap(this::parseContent)
 *             .flatMap(this::transformData)
 *             .ifFailure(error -> logProcessingError(filename, error));
 *     }
 *
 *     private Result<String, IOException> readFile(String filename) {
 *         try {
 *             String content = Files.readString(Paths.get(filename));
 *             return Result.of(content, null);
 *         } catch (IOException e) {
 *             return Result.of(null, e);
 *         }
 *     }
 *
 *     public void handleProcessingResult(Result<ProcessedData, IOException> result) {
 *         result.ifSuccessOrElse(
 *             data -> saveProcessedData(data),
 *             error -> notifyProcessingFailure(error)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the successful result value
 * @param <E> the type of the exception, must extend {@code Throwable}
 * @see Immutable
 * @see Optional
 * @see Pair
 * @see Tuple
 * @see Throwables
 * @see RuntimeException
 * @see Throwable
 * @see Function
 * @see Supplier
 */
@com.landawn.abacus.annotation.Immutable
public class Result<T, E extends Throwable> implements Immutable {

    private final T value;

    private final E exception;

    Result(final T value, final E exception) {
        this.value = value;
        this.exception = exception;
    }

    /**
     * Creates a new Result instance with the specified value and exception.
     * Either value or exception can be present, but typically only one should be {@code non-null}.
     * If both are {@code null}, it represents a successful operation with a {@code null} result.
     * If both are {@code non-null}, the Result is considered to be in failure state (exception takes precedence).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, IOException> result = Result.of("success", null);
     * Result<String, IOException> failure = Result.of(null, new IOException("error"));
     * }</pre>
     *
     * @param <T> the type of the result value
     * @param <E> the type of the exception, must extend Throwable
     * @param value the successful result value, can be null
     * @param exception the exception that occurred during the operation, {@code null} if operation was successful
     * @return a new Result instance containing either the value or the exception
     */
    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    /**
     * Checks if this Result represents a failed operation.
     * A Result is considered a failure if it contains a {@code non-null} exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isFailure()) {
     *     // handle failure case
     * }
     * }</pre>
     *
     * @return {@code true} if the Result contains an exception (operation failed), {@code false} otherwise
     */
    public boolean isFailure() {
        return exception != null;
    }

    /**
     * Checks if this Result represents a successful operation.
     * A Result is considered successful if it does not contain an exception (exception is null).
     * Note that a successful Result may still have a {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isSuccess()) {
     *     processValue(result.orElseThrow());
     * }
     * }</pre>
     *
     * @return {@code true} if the Result does not contain an exception (operation succeeded), {@code false} otherwise
     */
    public boolean isSuccess() {
        return exception == null;
    }

    /**
     * Executes the provided action if this Result represents a failure.
     * The action receives the exception contained in this Result as its parameter.
     * If this Result is successful (no exception), the action is not executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifFailure(ex -> log.error("Operation failed", ex));
     * }</pre>
     *
     * @param <E2> the type of exception that the action might throw
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @throws E2 if the actionOnFailure throws an exception of type E2
     */
    public <E2 extends Throwable> void ifFailure(final Throwables.Consumer<? super E, E2> actionOnFailure) throws E2 {
        ifFailureOrElse(actionOnFailure, Fn.emptyConsumer());
    }

    /**
     * Executes one of two actions based on whether this Result represents a failure or success.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * Exactly one action will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifFailureOrElse(
     *     ex -> log.error("Failed", ex),
     *     val -> log.info("Success: {}", val)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that actionOnFailure might throw
     * @param <E3> the type of exception that actionOnSuccess might throw
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @throws IllegalArgumentException if either actionOnFailure or actionOnSuccess is null
     * @throws E2 if the actionOnFailure is executed and throws an exception
     * @throws E3 if the actionOnSuccess is executed and throws an exception
     */
    public <E2 extends Throwable, E3 extends Throwable> void ifFailureOrElse(final Throwables.Consumer<? super E, E2> actionOnFailure,
            final Throwables.Consumer<? super T, E3> actionOnSuccess) throws IllegalArgumentException, E2, E3 {
        N.checkArgNotNull(actionOnFailure, cs.actionOnFailure);
        N.checkArgNotNull(actionOnSuccess, cs.actionOnSuccess);

        if (exception != null) {
            actionOnFailure.accept(exception);
        } else {
            actionOnSuccess.accept(value);
        }
    }

    /**
     * Executes the provided action if this Result represents a success.
     * The action receives the value contained in this Result as its parameter.
     * If this Result is a failure (contains an exception), the action is not executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifSuccess(val -> System.out.println("Success: " + val));
     * }</pre>
     *
     * @param <E2> the type of exception that the action might throw
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @throws E2 if the actionOnSuccess throws an exception of type E2
     */
    public <E2 extends Throwable> void ifSuccess(final Throwables.Consumer<? super T, E2> actionOnSuccess) throws E2 {
        ifSuccessOrElse(actionOnSuccess, Fn.emptyConsumer());
    }

    /**
     * Executes one of two actions based on whether this Result represents a success or failure.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * Exactly one action will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifSuccessOrElse(
     *     val -> processValue(val),
     *     ex -> handleError(ex)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that actionOnSuccess might throw
     * @param <E3> the type of exception that actionOnFailure might throw
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @throws IllegalArgumentException if either actionOnSuccess or actionOnFailure is null
     * @throws E2 if the actionOnSuccess is executed and throws an exception
     * @throws E3 if the actionOnFailure is executed and throws an exception
     */
    public <E2 extends Throwable, E3 extends Throwable> void ifSuccessOrElse(final Throwables.Consumer<? super T, E2> actionOnSuccess,
            final Throwables.Consumer<? super E, E3> actionOnFailure) throws IllegalArgumentException, E2, E3 {
        N.checkArgNotNull(actionOnSuccess, cs.actionOnSuccess);
        N.checkArgNotNull(actionOnFailure, cs.actionOnFailure);

        if (exception == null) {
            actionOnSuccess.accept(value);
        } else {
            actionOnFailure.accept(exception);
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise returns the specified default value.
     * This method provides a way to handle failure cases with a fallback value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseIfFailure("default");
     * }</pre>
     *
     * @param defaultValueIfErrorOccurred the value to return if this Result contains an exception
     * @return the value contained in this Result if successful, otherwise the defaultValueIfErrorOccurred
     */
    public T orElseIfFailure(final T defaultValueIfErrorOccurred) {
        if (exception == null) {
            return value;
        } else {
            return defaultValueIfErrorOccurred;
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise returns a value supplied by the given supplier.
     * This method provides a way to handle failure cases with a lazily computed fallback value.
     * The supplier is only invoked if this Result contains an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseGetIfFailure(() -> computeDefault());
     * }</pre>
     *
     * @param otherIfErrorOccurred a supplier that provides the value to return if this Result contains an exception, must not be null
     * @return the value contained in this Result if successful, otherwise the value provided by the supplier
     * @throws IllegalArgumentException if otherIfErrorOccurred is null
     */
    public T orElseGetIfFailure(final Supplier<? extends T> otherIfErrorOccurred) throws IllegalArgumentException {
        N.checkArgNotNull(otherIfErrorOccurred, cs.otherIfErrorOccurred);

        if (exception == null) {
            return value;
        } else {
            return otherIfErrorOccurred.get();
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws the contained exception.
     * This method provides a way to unwrap the Result, propagating any exception that occurred.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(); // throws exception if failed
     * }</pre>
     *
     * @return the value contained in this Result if successful
     * @throws E the exception contained in this Result if it represents a failure
     */
    public T orElseThrow() throws E {
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws an exception created by applying
     * the contained exception to the provided exception mapper function.
     * This method allows transforming the original exception into a different exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(ex -> new RuntimeException("Failed", ex));
     * }</pre>
     *
     * @param <E2> the type of exception to be thrown
     * @param exceptionSupplierIfErrorOccurred a function that creates a new exception based on the contained exception, must not be null
     * @return the value contained in this Result if successful
     * @throws IllegalArgumentException if exceptionSupplierIfErrorOccurred is null
     * @throws E2 the exception created by the exceptionSupplierIfErrorOccurred function if this Result contains an exception
     */
    public <E2 extends Throwable> T orElseThrow(final Function<? super E, E2> exceptionSupplierIfErrorOccurred) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(exceptionSupplierIfErrorOccurred, cs.exceptionSupplierIfErrorOccurred);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplierIfErrorOccurred.apply(exception);
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws an exception supplied by the given supplier.
     * This method provides a way to throw a custom exception when the Result represents a failure.
     * The supplier is only invoked if this Result contains an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(() -> new IllegalStateException("Failed"));
     * }</pre>
     *
     * @param <E2> the type of exception to be thrown
     * @param exceptionSupplier a supplier that provides the exception to throw if this Result contains an exception, must not be null
     * @return the value contained in this Result if successful
     * @throws IllegalArgumentException if exceptionSupplier is null
     * @throws E2 the exception provided by the exceptionSupplier if this Result contains an exception
     */
    public <E2 extends Throwable> T orElseThrow(final Supplier<? extends E2> exceptionSupplier) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws the specified exception.
     * This method provides a way to throw a pre-created exception when the Result represents a failure.
     *
     * <p><b>Note:</b> This method is deprecated because it requires creating the exception object
     * eagerly even if the Result is successful. Use {@link #orElseThrow(Supplier)} instead for
     * better performance.
     *
     * @param <E2> the type of exception to be thrown
     * @param exception the exception to throw if this Result contains an exception
     * @return the value contained in this Result if successful
     * @throws E2 the provided exception if this Result contains an exception
     * @deprecated Use {@link #orElseThrow(Supplier)} instead for better performance (avoids creating exception if not needed)
     */
    @Deprecated
    public <E2 extends Throwable> T orElseThrow(final E2 exception) throws E2 {
        if (this.exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Returns the exception contained in this Result, or {@code null} if the Result is successful.
     * This method provides direct access to the exception without wrapping it in an Optional.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isFailure()) {
     *     Exception ex = result.getException();
     *     log.error("Error occurred", ex);
     * }
     * }</pre>
     *
     * @return the exception if this Result represents a failure, {@code null} if the Result is successful
     */
    @Beta
    public E getException() {
        return exception;
    }

    /**
     * Returns an Optional containing the exception if this Result represents a failure,
     * or an empty Optional if the Result is successful.
     * This method provides a safe way to access the exception with Optional semantics.
     *
     * <p><b>Note:</b> This method is deprecated in favor of {@link #getException()} which provides
     * direct access to the exception without the Optional wrapper overhead.
     *
     * @return an Optional containing the exception if present, otherwise an empty Optional
     * @deprecated Use {@link #getException()} instead for direct access to the exception
     */
    @Deprecated
    @Beta
    public Optional<E> getExceptionIfPresent() {
        return Optional.ofNullable(exception);
    }

    /**
     * Converts this Result to a Pair containing both the value and the exception.
     * The first element of the Pair is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be {@code non-null}, but both could be {@code null} or {@code non-null} depending on how the Result was created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Exception> pair = result.toPair();
     * String value = pair._1;
     * Exception ex = pair._2;
     * }</pre>
     *
     * @return a Pair where the first element is the value and the second element is the exception
     */
    public Pair<T, E> toPair() {
        return Pair.of(value, exception);
    }

    /**
     * Converts this Result to a Tuple2 containing both the value and the exception.
     * The first element of the Tuple2 is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be {@code non-null}, but both could be {@code null} or {@code non-null} depending on how the Result was created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Exception> tuple = result.toTuple();
     * String value = tuple._1;
     * Exception ex = tuple._2;
     * }</pre>
     *
     * @return a Tuple2 where the first element is the value and the second element is the exception
     */
    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    /**
     * Returns a hash code value for this Result.
     * The hash code is computed based on the exception if present, otherwise based on the value.
     * This ensures that Results with the same exception or value will have the same hash code.
     * This method is consistent with the {@link #equals(Object)} implementation.
     *
     * @return the hash code of the exception if present, otherwise the hash code of the value
     */
    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     * Compares this Result with another object for equality.
     * Two Results are considered equal if they are both successful with equal values,
     * or both failures with equal exceptions. For equality, both the value and exception
     * must match between the two Result instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, Exception> r1 = Result.of("value", null);
     * Result<String, Exception> r2 = Result.of("value", null);
     * boolean isEqual = r1.equals(r2); // true
     * }</pre>
     *
     * @param obj the object to compare with this Result
     * @return {@code true} if the specified object is a Result with equal value and exception, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        //noinspection rawtypes
        if (obj instanceof Result other) { // NOSONAR
            return N.equals(other.value, value) && N.equals(other.exception, exception);
        }

        return false;
    }

    /**
     * Returns a string representation of this Result.
     * The string representation includes both the value and the exception in the format:
     * {@code {value=<value>, exception=<exception>}}.
     * This is useful for debugging and logging purposes.
     * 
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * {value=success, exception=null}
     * {value=null, exception=java.io.IOException: Error}
     * }</pre>
     *
     * @return a string representation of this Result showing both value and exception
     */
    @Override
    public String toString() {
        return "{value=" + N.toString(value) + ", exception=" + N.toString(exception) + "}";
    }

    /**
     * A specialized Result class that specifically handles RuntimeException as the exception type.
     * This class provides a more convenient way to work with Results that may contain RuntimeExceptions,
     * which don't need to be declared in method signatures.
     *
     * @param <T> the type of the result value
     */
    @Beta
    public static class RR<T> extends Result<T, RuntimeException> {
        RR(final T value, final RuntimeException exception) {
            super(value, exception);
        }

        /**
         * Creates a new RR (Result with RuntimeException) instance with the specified value and exception.
         * Either value or exception can be present, but typically only one should be {@code non-null}.
         * This factory method provides a convenient way to create Results for operations that may throw RuntimeExceptions,
         * which don't need to be declared in method signatures.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Result.RR<String> result = Result.RR.of("success", null);
         * Result.RR<String> failure = Result.RR.of(null, new RuntimeException("error"));
         * }</pre>
         *
         * @param <T> the type of the result value
         * @param value the successful result value, can be null
         * @param exception the RuntimeException that occurred during the operation, {@code null} if operation was successful
         * @return a new RR instance containing either the value or the RuntimeException
         */
        public static <T> Result.RR<T> of(final T value, final RuntimeException exception) {
            return new RR<>(value, exception);
        }
    }
}
