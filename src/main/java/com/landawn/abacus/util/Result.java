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
 * A structurally immutable container for an operation value and an optional failure. A result is a
 * success exactly when its exception is {@code null}; consequently {@code null} is a valid success
 * value. Callback and unwrapping methods can still throw, as documented by those methods.
 *
 * <p>This class provides conditional consumers and fallback/unwrapping operations. It does not
 * provide value-transforming {@code map}/{@code flatMap} operations, so failure propagation across
 * transformations must be implemented by the caller.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Generic constraints ensure exception type {@code E} extends {@code Throwable}</li>
 *   <li><b>Structurally Immutable:</b> The stored references cannot be reassigned after construction</li>
 *   <li><b>Explicit Error State:</b> Carries the failure alongside the value for callers to inspect</li>
 *   <li><b>Functional API:</b> Provides functional-style methods for conditional execution, fallback, and unwrapping</li>
 *   <li><b>Deferred Exception Throwing:</b> Encapsulates failures until callers explicitly unwrap them</li>
 *   <li><b>Small State:</b> Stores two object references</li>
 *   <li><b>Integration Ready:</b> Seamless conversion to other container types like Pair and Tuple</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Immutable Design:</b>
 * <ul>
 *   <li>This class implements {@link Immutable}; its own fields cannot be modified after creation</li>
 *   <li>Both {@code value} and {@code exception} fields are final and set only during construction</li>
 *   <li>Mutability and thread safety of the referenced value and exception are the caller's responsibility</li>
 *   <li>Methods inspect or expose the stored references without reassigning the container's fields</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Explicit Over Implicit:</b> Makes error states visible and forces explicit handling</li>
 *   <li><b>Branch Over Transform:</b> Callers branch on the state explicitly; there are no
 *       value-transforming combinators to hide the failure path</li>
 *   <li><b>Type Safety Over Runtime Errors:</b> Uses generic constraints to prevent type-related runtime issues</li>
 *   <li><b>Predictability Over Convenience:</b> Ensures predictable behavior even at the cost of some verbosity</li>
 *   <li><b>Immutability Over Performance:</b> Prioritizes correctness and thread safety over minimal performance gains</li>
 * </ul>
 *
 * <p><b>Core State Model:</b>
 * <ul>
 *   <li><b>Success:</b> {@code exception == null} - the operation completed successfully; {@code value}
 *       may be {@code null}</li>
 *   <li><b>Failure:</b> {@code exception != null} - success/failure methods treat the result as failed;
 *       conversion, equality, and string methods still retain and expose both the value and the exception
 *       when both were supplied</li>
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
 *     String value = result.orElseThrow();   // safe to call
 * } else {
 *     IOException error = result.getException();
 *     handleError(error);
 * }
 *
 * // Conditional execution based on state
 * result.ifSuccess(value -> processValue(value));
 * result.ifFailure(error -> logError(error));
 *
 * // Safe value extraction with defaults
 * String safeValue = result.orElseIfFailure("default value");
 * String computedDefault = result.orElseGetIfFailure(() -> computeDefault());
 *
 * // Exception transformation and re-throwing
 * String value = result.orElseThrow(ex -> new RuntimeException("Operation failed", ex));
 * String value2 = result.orElseThrow(() -> new CustomException("Operation failed"));
 * }</pre>
 *
 * <p><b>Advanced Error Handling Patterns:</b>
 * <pre>{@code
 * class ResultHandlers {
 * // Conditional execution with both success and failure handlers
 * public void processFile(Result<String, IOException> fileResult) {
 *     fileResult.ifSuccessOrElse(
 *         content -> processContent(content),
 *         error -> logError("Failed to read file", error)
 *     );
 * }
 *
 * // Convert exception to different type and re-throw
 * public String fetchData(Result<String, SQLException> dbResult) throws ServiceException {
 *     return dbResult.orElseThrow(sqlEx -> new ServiceException("Database operation failed", sqlEx));
 * }
 *
 * // Handle multiple results with fallback values
 * public void demonstrateFallbacks() {
 *     Result<String, IOException> result1 = readFile("file1.txt");
 *     Result<String, IOException> result2 = readFile("file2.txt");
 *
 *     String data1 = result1.orElseIfFailure("default1");
 *     String data2 = result2.orElseGetIfFailure(() -> computeDefault());
 * }
 * }
 * }</pre>
 *
 * <p><b>Integration with Existing Code:</b>
 * <pre>{@code
 * class ResultConversions {
 * // Converting from traditional exception-based code
 * public Result<String, IOException> readFileAsResult(String path) {
 *     try {
 *         String content = Files.readString(Paths.get(path));
 *         return Result.of(content, null);
 *     } catch (IOException e) {
 *         return Result.of(null, e);
 *     }
 * }
 *
 * // Converting to other container types
 * public void convert(Result<String, IOException> result) {
 *     Pair<String, IOException> pair = result.toPair();
 *     Tuple2<String, IOException> tuple = result.toTuple();
 *     Optional<String> optional = result.isSuccess()
 *         ? Optional.ofNullable(result.orElseIfFailure(null))
 *         : Optional.empty();
 * }
 * }
 * }</pre>
 *
 * <p><b>RR Nested Class - RuntimeException Specialization:</b>
 * <ul>
 *   <li><b>Convenience Subclass:</b> Specialized for {@code RuntimeException} to reduce generic verbosity</li>
 *   <li><b>Use Case:</b> Avoids repeating the {@code RuntimeException} type argument</li>
 *   <li><b>Simplified API:</b> {@code Result.RR<T>} instead of {@code Result<T, RuntimeException>}</li>
 *   <li><b>Create With {@code RR.of(...)}:</b> {@code RR} inherits the static {@link #success(Object)}
 *       and {@link #failure(Throwable)} factories, but they return {@code Result}, <em>not</em>
 *       {@code RR} &mdash; see {@link RR}</li>
 *   <li><b>Beta Feature:</b> Subject to API refinement based on usage feedback</li>
 * </ul>
 *
 * <p>Construction and direct state access are O(1): construction stores two references and each
 * accessor is a field read or a {@code null} check. Equality, hashing, and string conversion depend
 * on the contained values, and callback operations include the cost of the supplied callbacks.
 * Reads are unsynchronized; callbacks and operations on the contained values may have side effects.</p>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable State:</b> All fields are final and set only during construction</li>
 *   <li><b>Concurrent Access:</b> The container state is safe for concurrent read access</li>
 *   <li><b>No Synchronization:</b> No locks or synchronization needed due to immutability</li>
 *   <li><b>Contained Objects:</b> Mutable values or exceptions may still require synchronization</li>
 * </ul>
 *
 * <p><b>Error Handling Philosophy:</b>
 * <ul>
 *   <li><b>Explicit Failures:</b> All potential failures are represented explicitly in the type system</li>
 *   <li><b>Explicit Stored Failures:</b> The stored failure is visible in the result type; supplied
 *       callbacks and exception factories may themselves throw</li>
 *   <li><b>Handled At The Call Site:</b> A failure is inspected, replaced with a fallback, or
 *       rethrown where it is unwrapped &mdash; not carried through a transformation chain</li>
 *   <li><b>Type-Safe Recovery:</b> Recovery strategies are enforced by the type system</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use {@code isSuccess()} or {@code isFailure()} when branching explicitly</li>
 *   <li>Use {@code orElseThrow()} when propagating the stored failure is the desired policy</li>
 *   <li>Prefer {@code ifSuccess()} and {@code ifFailure()} for conditional execution</li>
 *   <li>Use {@code orElseIfFailure()} with meaningful default values</li>
 *   <li>Branch with {@code ifSuccessOrElse()} rather than unwrapping and re-wrapping by hand</li>
 *   <li>Document which exceptions your methods can produce in their Result types</li>
 *   <li>Use appropriate exception types in the generic parameter {@code E}</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Creating Results with both value and exception {@code non-null} (ambiguous state)</li>
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
 *   <li><b>vs. {@code Nullable} Returns:</b> Result distinguishes between {@code null} success values and actual failures</li>
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
 * <p><b>Usage Examples: File Processing Pipeline</b></p>
 * <pre>{@code
 * public class FileProcessor {
 *     public Result<String, IOException> readFile(String filename) {
 *         try {
 *             String content = Files.readString(Paths.get(filename));
 *             return Result.of(content, null);
 *         } catch (IOException e) {
 *             return Result.of(null, e);
 *         }
 *     }
 *
 *     public void processFile(String filename) {
 *         Result<String, IOException> result = readFile(filename);
 *
 *         result.ifSuccessOrElse(
 *             content -> {
 *                 // Process the file content
 *                 ProcessedData data = parseAndTransform(content);
 *                 saveProcessedData(data);
 *             },
 *             error -> {
 *                 logProcessingError(filename, error);
 *                 notifyProcessingFailure(error);
 *             }
 *         );
 *     }
 *
 *     public String getFileContentOrDefault(String filename) throws IOException {
 *         Result<String, IOException> result = readFile(filename);
 *         // Extract value or throw the IOException
 *         return result.orElseThrow();
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

    /**
     * Constructs a new Result instance with the specified value and exception.
     * This constructor is package-private and should be accessed via the {@link #of(Object, Throwable)} factory method.
     * Either value or exception can be present, but typically only one should be {@code non-null}.
     * If both are {@code null}, it represents a successful operation with a {@code null} result.
     * If both are {@code non-null}, the Result is considered to be in failure state (exception takes precedence).
     *
     * @param value the successful result value, may be {@code null}.
     * @param exception the exception that occurred during the operation, may be {@code null} if operation was successful.
     */
    Result(final T value, final E exception) {
        this.value = value;
        this.exception = exception;
    }

    /**
     * Creates a new successful Result instance containing the specified value with no exception.
     * This is the primary factory method for creating Result instances that represent successful operations.
     * The resulting Result will have {@code isSuccess() == true} and {@code isFailure() == false}.
     *
     * <p>This method is semantically equivalent to calling {@code Result.of(value, null)}, but provides
     * a more expressive and self-documenting API that clearly communicates the intent to create a
     * success case. It follows the factory method pattern commonly used in functional programming
     * for creating discriminated union types.</p>
     *
     * <p><b>State Guarantees:</b></p>
     * <ul>
     *   <li>The returned Result will always have {@code exception == null}</li>
     *   <li>{@code isSuccess()} will always return {@code true}</li>
     *   <li>{@code isFailure()} will always return {@code false}</li>
     *   <li>{@code orElseThrow()} will return the value without throwing</li>
     *   <li>{@code getException()} will return {@code null}</li>
     * </ul>
     *
     * <p><b>Null Value Handling:</b></p>
     * <ul>
     *   <li>A {@code null} value is permitted and represents a successful operation that returned {@code null}</li>
     *   <li>To distinguish between "success with null" and "failure", use {@code isSuccess()} or {@code isFailure()}</li>
     *   <li>Example: A database query that successfully executes but finds no matching record might return {@code Result.success(null)}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Creating a success result with a non-null value
     * Result<String, IOException> result = Result.success("Hello World");
     * assert result.isSuccess();                           // returns true
     * assert result.orElseThrow().equals("Hello World");   // returns "Hello World"
     *
     * // Creating a success result with a null value (valid use case)
     * Result<User, SQLException> nullResult = Result.success(null);
     * assert nullResult.isSuccess();                // returns true - operation succeeded
     * assert nullResult.orElseThrow() == null;      // returns null - but that's the actual result
     *
     * // Using with different value types
     * Result<Integer, ArithmeticException> intResult = Result.success(42);
     * Result<List<String>, Exception> listResult = Result.success(Arrays.asList("a", "b", "c"));
     * Result<Optional<String>, RuntimeException> optionalResult = Result.success(Optional.of("value"));
     *
     * // Typical pattern in service methods
     * Function<Long, Result<User, DatabaseException>> findUserById = id -> {
     *     try {
     *         User user = userRepository.findById(id);
     *         return Result.success(user);
     *     } catch (DatabaseException e) {
     *         return Result.failure(e);
     *     }
     * };
     *
     * // Conditional execution
     * Result<String, RuntimeException> successResult = Result.success(computeValue());
     * successResult.ifSuccessOrElse(
     *     value -> log.info("Computed: {}", value),
     *     ex -> log.error("Should never happen", ex)
     * );
     * }</pre>
     *
     * <p><b>Thread Safety:</b></p>
     * <ul>
     *   <li>This method is thread-safe and can be called concurrently from multiple threads</li>
     *   <li>The returned Result instance is immutable and safe for concurrent access</li>
     *   <li>No synchronization is required when sharing Result instances between threads</li>
     * </ul>
     *
     * <p><b>Performance Characteristics:</b></p>
     * <ul>
     *   <li><b>Time Complexity:</b> O(1) - constant time object allocation</li>
     *   <li><b>Space Complexity:</b> O(1) - single object with two reference fields</li>
     *   <li><b>Memory Allocation:</b> Creates exactly one new Result object per invocation</li>
     *   <li><b>No Caching:</b> Each call creates a new instance; no interning or caching is performed</li>
     * </ul>
     *
     * <p><b>Comparison with Alternative Approaches:</b></p>
     * <ul>
     *   <li><b>vs. {@code Result.of(value, null)}:</b> Semantically identical, but {@code success()} is more expressive</li>
     *   <li><b>vs. {@code Optional.of(value)}:</b> Result distinguishes success-with-null from failure; Optional cannot</li>
     *   <li><b>vs. returning value directly:</b> Result provides explicit error handling without exceptions</li>
     *   <li><b>vs. returning {@code null} for errors:</b> Result distinguishes {@code null} success values from error conditions</li>
     * </ul>
     *
     * <p><b>Best Practices:</b></p>
     * <ul>
     *   <li>Prefer {@code Result.success()} over {@code Result.of(value, null)} for clarity</li>
     *   <li>Document whether {@code null} values are valid success cases in your API</li>
     *   <li>Consider using {@code Optional<T>} as the value type if {@code null} has special meaning</li>
     *   <li>Use meaningful exception types in the generic parameter for better type safety</li>
     * </ul>
     *
     * @param <T> the type of the successful result value; can be any type including wrapper types for primitives
     * @param <E> the type of exception that could occur in failure cases; must extend {@code Throwable}.
     *            This parameter is inferred from context and does not affect the success case,
     *            but ensures type compatibility when combining with failure Results.
     * @param value the successful result value to wrap; may be {@code null} to represent a successful
     *              operation that returned no value (e.g., a void operation or a query with no results)
     * @return a new {@code Result} instance representing a successful operation containing the specified value;
     *         never returns {@code null}
     * @see #failure(Throwable)
     * @see #of(Object, Throwable)
     * @see #isSuccess()
     * @see #orElseThrow()
     */
    public static <T, E extends Throwable> Result<T, E> success(final T value) {
        return of(value, null);
    }

    /**
     * Creates a new failure Result instance containing the specified exception with no value.
     * This is the primary factory method for creating Result instances that represent failed operations.
     * The resulting Result will have {@code isFailure() == true} and {@code isSuccess() == false}.
     *
     * <p>This method is semantically equivalent to calling {@code Result.of(null, exception)}, but provides
     * a more expressive and self-documenting API that clearly communicates the intent to create a
     * failure case. It follows the factory method pattern commonly used in functional programming
     * for creating discriminated union types representing error states.</p>
     *
     * <p><b>State Guarantees:</b></p>
     * <ul>
     *   <li>The returned Result will always have {@code value == null} (internal state)</li>
     *   <li>{@code isFailure()} will always return {@code true}</li>
     *   <li>{@code isSuccess()} will always return {@code false}</li>
     *   <li>{@code orElseThrow()} will throw the contained exception</li>
     *   <li>{@code getException()} will return the provided exception</li>
     *   <li>{@code orElseIfFailure(defaultValue)} will return the default value</li>
     * </ul>
     *
     * <p>The exception must not be {@code null}. Use {@link #success(Object)} to represent a
     * successful operation, including one whose result value is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Creating a failure result with a specific exception
     * Result<String, IOException> result = Result.failure(new IOException("File not found"));
     * assert result.isFailure();                    // returns true
     * assert result.getException().getMessage().equals("File not found");
     *
     * // Attempting to get value throws the exception
     * try {
     *     String value = result.orElseThrow();      // throws IOException
     * } catch (IOException e) {
     *     System.out.println("Caught: " + e.getMessage());
     * }
     *
     * // Using with different exception types
     * Result<Integer, ArithmeticException> mathError = Result.failure(new ArithmeticException("Division by zero"));
     * Result<User, SQLException> dbError = Result.failure(new SQLException("Connection timeout"));
     * Result<Config, IllegalStateException> configError = Result.failure(new IllegalStateException("Invalid config"));
     *
     * // Typical pattern in service methods
     * Function<UserInput, Result<User, ValidationException>> validateUser = input -> {
     *     if (input.getEmail() == null) {
     *         return Result.failure(new ValidationException("Email is required"));
     *     }
     *     if (!isValidEmail(input.getEmail())) {
     *         return Result.failure(new ValidationException("Invalid email format"));
     *     }
     *     return Result.success(createUser(input));
     * };
     *
     * // Wrapping checked exceptions from external APIs
     * Function<Path, Result<String, IOException>> readFile = path -> {
     *     try {
     *         return Result.success(Files.readString(path));
     *     } catch (IOException e) {
     *         return Result.failure(e);
     *     }
     * };
     *
     * // Conditional execution on failure
     * Result<String, RuntimeException> failureResult = Result.failure(new IllegalArgumentException("Invalid input"));
     * failureResult.ifSuccessOrElse(
     *     value -> log.info("Should never execute"),
     *     ex -> log.error("Error occurred: {}", ex.getMessage())
     * );
     *
     * // Transforming and re-throwing exceptions
     * Result<Data, SQLException> dbResult = fetchFromDatabase();
     * Data data = dbResult.orElseThrow(
     *     (Function<SQLException, ServiceException>) sqlEx -> new ServiceException("Database error", sqlEx));
     *
     * // Providing fallback values for failures
     * String content = Result.<String, IOException>failure(new IOException("Read error"))
     *     .orElseIfFailure("default content");
     * assert content.equals("default content");
     *
     * // Lazy fallback computation
     * String lazyContent = result.orElseGetIfFailure(() -> loadFromBackupSource());
     * }</pre>
     *
     * <p><b>Exception Preservation:</b></p>
     * <ul>
     *   <li>The exception instance is stored by reference, not copied</li>
     *   <li>Stack trace and all exception properties are preserved</li>
     *   <li>Chained exceptions (cause chain) remain intact</li>
     *   <li>The exact exception type is preserved through generics</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b></p>
     * <ul>
     *   <li>This method is thread-safe and can be called concurrently from multiple threads</li>
     *   <li>The returned Result instance is immutable and safe for concurrent access</li>
     *   <li>The contained exception should ideally be immutable for full thread safety</li>
     *   <li>No synchronization is required when sharing Result instances between threads</li>
     * </ul>
     *
     * <p><b>Performance Characteristics:</b></p>
     * <ul>
     *   <li><b>Time Complexity:</b> O(1) - constant time object allocation</li>
     *   <li><b>Space Complexity:</b> O(1) - single object with two reference fields</li>
     *   <li><b>Memory Allocation:</b> Creates exactly one new Result object per invocation</li>
     *   <li><b>Exception Cost:</b> The exception should already be created; this method adds minimal overhead</li>
     *   <li><b>No Stack Trace Creation:</b> This method does not create or modify stack traces</li>
     * </ul>
     *
     * <p><b>Comparison with Alternative Approaches:</b></p>
     * <ul>
     *   <li><b>vs. {@code Result.of(null, exception)}:</b> Semantically identical, but {@code failure()} is more expressive</li>
     *   <li><b>vs. throwing exceptions:</b> Result allows deferred handling and functional composition</li>
     *   <li><b>vs. returning null:</b> Result provides type-safe error information with the exception</li>
     *   <li><b>vs. error codes:</b> Result preserves full exception context including stack trace and cause chain</li>
     *   <li><b>vs. {@code Optional.empty()}:</b> Result carries error details; Optional only indicates absence</li>
     * </ul>
     *
     * <p><b>Best Practices:</b></p>
     * <ul>
     *   <li>Prefer {@code Result.failure()} over {@code Result.of(null, exception)} for clarity</li>
     *   <li>Use specific exception types rather than generic {@code Exception} for better type safety</li>
     *   <li>Include meaningful error messages in the exception for debugging</li>
     *   <li>Preserve the original exception as the cause when wrapping exceptions</li>
     *   <li>Document which exception types your methods can return in their Result</li>
     *   <li>Never pass {@code null} as the exception parameter</li>
     * </ul>
     *
     * <p><b>Common Patterns:</b></p>
     * <pre>{@code
     * class ResultWorkflows {
     * // Pattern 1: Converting try-catch to Result
     * public Result<Data, ServiceException> fetchData(String id) {
     *     try {
     *         Data data = externalService.fetch(id);
     *         return Result.success(data);
     *     } catch (NetworkException | TimeoutException e) {
     *         return Result.failure(new ServiceException("Failed to fetch data", e));
     *     }
     * }
     *
     * // Pattern 2: Validation with early return
     * public Result<Order, ValidationException> createOrder(OrderRequest request) {
     *     if (request.getItems().isEmpty()) {
     *         return Result.failure(new ValidationException("Order must have at least one item"));
     *     }
     *     if (request.getCustomerId() == null) {
     *         return Result.failure(new ValidationException("Customer ID is required"));
     *     }
     *     return Result.success(orderService.create(request));
     * }
     *
     * // Pattern 3: Combining multiple Results
     * public Result<UserProfile, DbException> createProfile(long userId) throws DbException {
     *     Result<User, DbException> userResult = userRepo.findById(userId);
     *     Result<Account, DbException> accountResult = accountRepo.findByUserId(userId);
     *
     *     if (userResult.isFailure()) {
     *         return Result.failure(userResult.getException());
     *     }
     *     if (accountResult.isFailure()) {
     *         return Result.failure(accountResult.getException());
     *     }
     *     return Result.success(new UserProfile(userResult.orElseThrow(), accountResult.orElseThrow()));
     * }
     * }
     * }</pre>
     *
     * @param <T> the type of the successful result value; this parameter is inferred from context
     *            and does not affect the failure case, but ensures type compatibility when
     *            combining with success Results
     * @param <E> the type of exception contained in this failure Result; must extend {@code Throwable}.
     *            Using specific exception types (e.g., {@code IOException}, {@code SQLException})
     *            rather than generic {@code Exception} provides better type safety and documentation.
     * @param exception the non-{@code null} exception representing the failure cause
     * @return a new {@code Result} instance representing a failed operation containing the specified exception;
     *         never returns {@code null}
     * @throws IllegalArgumentException if {@code exception} is {@code null}.
     * @see #success(Object)
     * @see #of(Object, Throwable)
     * @see #isFailure()
     * @see #getException()
     * @see #orElseThrow()
     * @see #orElseIfFailure(Object)
     * @see #orElseGetIfFailure(Supplier)
     */
    public static <T, E extends Throwable> Result<T, E> failure(final E exception) throws IllegalArgumentException {
        N.checkArgNotNull(exception, cs.exception);

        return of(null, exception);
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
     * @param <T> the type of the result value.
     * @param <E> the type of the exception; must extend {@code Throwable}.
     * @param value the successful result value, may be {@code null}.
     * @param exception the exception that occurred during the operation, may be {@code null} if the operation was successful.
     * @return a new Result instance containing either the value or the exception.
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
     * @return {@code true} if the Result contains an exception (operation failed), {@code false} otherwise.
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
     * @return {@code true} if the Result does not contain an exception (operation succeeded), {@code false} otherwise.
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
     * @param <E2> the type of exception that the action might throw.
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be {@code null}.
     * @throws IllegalArgumentException if {@code actionOnFailure} is {@code null}.
     * @throws E2 if {@code actionOnFailure} is executed and throws an exception of type {@code E2}.
     */
    public <E2 extends Throwable> void ifFailure(final Throwables.Consumer<? super E, E2> actionOnFailure) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(actionOnFailure, cs.actionOnFailure);

        if (exception != null) {
            actionOnFailure.accept(exception);
        }
    }

    /**
     * Executes one of two actions based on whether this Result represents a failure or success.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * Exactly one action will be executed.
     *
     * <p><b>⚠️ The failure handler comes first here, and second in
     * {@link #ifSuccessOrElse(Throwables.Consumer, Throwables.Consumer)}.</b> Both parameters are
     * {@code Throwables.Consumer}, so whenever {@code T} is a supertype of {@code E} &mdash; which it
     * is for the common {@code Result<Object, ...>} and {@code Result<Serializable, ...>} &mdash;
     * passing the two handlers in the wrong order still compiles, and the wrong one silently runs
     * against the wrong argument. Prefer {@code ifSuccessOrElse} so that only one order is in play.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifFailureOrElse(
     *     ex -> log.error("Failed", ex),
     *     val -> log.info("Success: {}", val)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that {@code actionOnFailure} might throw.
     * @param <E3> the type of exception that {@code actionOnSuccess} might throw.
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be {@code null}.
     * @param actionOnSuccess the action to execute if this Result is successful, must not be {@code null}.
     * @throws IllegalArgumentException if any of {@code actionOnFailure}, {@code actionOnSuccess} is {@code null}.
     * @throws E2 if {@code actionOnFailure} is executed and throws an exception of type {@code E2}.
     * @throws E3 if {@code actionOnSuccess} is executed and throws an exception of type {@code E3}.
     * @deprecated use {@link #ifSuccessOrElse(Throwables.Consumer, Throwables.Consumer)} instead, which
     *             takes the same two handlers in the opposite order. Having both orders available under
     *             two names is the hazard: the parameters are indistinguishable to overload resolution
     *             whenever {@code T} is a supertype of {@code E}, so a swap is a silent runtime bug
     *             rather than a compile error. Migration is a straight swap of the two arguments -
     *             {@code r.ifFailureOrElse(f, s)} becomes {@code r.ifSuccessOrElse(s, f)}. This method's
     *             own behaviour is unchanged and will not be reordered.
     */
    @Deprecated
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
     * @param <E2> the type of exception that the action might throw.
     * @param actionOnSuccess the action to execute if this Result is successful, must not be {@code null}.
     * @throws IllegalArgumentException if {@code actionOnSuccess} is {@code null}.
     * @throws E2 if {@code actionOnSuccess} is executed and throws an exception of type {@code E2}.
     */
    public <E2 extends Throwable> void ifSuccess(final Throwables.Consumer<? super T, E2> actionOnSuccess) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(actionOnSuccess, cs.actionOnSuccess);

        if (exception == null) {
            actionOnSuccess.accept(value);
        }
    }

    /**
     * Executes one of two actions based on whether this Result represents a success or failure.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * Exactly one action will be executed.
     *
     * <p><b>The success handler comes first.</b> Both parameters are {@code Throwables.Consumer}, so
     * when {@code T} is a supertype of {@code E} the compiler cannot catch a swapped pair; this is the
     * preferred of the two orderings, and
     * {@link #ifFailureOrElse(Throwables.Consumer, Throwables.Consumer)} is deprecated for offering
     * the other one.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifSuccessOrElse(
     *     val -> processValue(val),
     *     ex -> handleError(ex)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that {@code actionOnSuccess} might throw.
     * @param <E3> the type of exception that {@code actionOnFailure} might throw.
     * @param actionOnSuccess the action to execute if this Result is successful, must not be {@code null}.
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be {@code null}.
     * @throws IllegalArgumentException if any of {@code actionOnSuccess}, {@code actionOnFailure} is {@code null}.
     * @throws E2 if {@code actionOnSuccess} is executed and throws an exception of type {@code E2}.
     * @throws E3 if {@code actionOnFailure} is executed and throws an exception of type {@code E3}.
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
     * @param defaultValueIfErrorOccurred the value to return if this Result contains an exception.
     * @return the value contained in this Result if successful, otherwise the defaultValueIfErrorOccurred.
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
     * @param otherIfErrorOccurred the supplier that provides the value to return if this Result contains an exception, must not be {@code null}.
     * @return the value contained in this Result if successful, otherwise the value provided by the supplier.
     * @throws IllegalArgumentException if {@code otherIfErrorOccurred} is {@code null}.
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
     * String value = result.orElseThrow();   // throws exception if failed
     * }</pre>
     *
     * @return the value contained in this Result if successful.
     * @throws E the exception contained in this Result if it represents a failure.
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
     * <p><b>Method references:</b> a lambda always resolves cleanly here because its arity is written
     * out, and so does a constructor reference whose target has only one applicable arity. A reference
     * such as {@code RuntimeException::new}, whose target has both a no-arg and a cause-taking
     * constructor, fits this overload and {@link #orElseThrow(Supplier)} equally and must be
     * disambiguated with a cast.</p>
     *
     * @param <E2> the type of exception to be thrown.
     * @param exceptionMapper the function that maps the contained exception to a new exception to be thrown, must not be {@code null}.
     * @return the value contained in this Result if successful.
     * @throws IllegalArgumentException if {@code exceptionMapper} is {@code null}.
     * @throws NullPointerException if this Result is a failure and {@code exceptionMapper} returns
     *         {@code null}; the {@code null} is thrown, which the JVM turns into a
     *         {@code NullPointerException} at the {@code throw} site.
     * @throws E2 if this Result is a failure; the exception is produced by applying {@code exceptionMapper} to the contained exception.
     * @see #orElseThrow(Supplier)
     * @see #orElseThrow()
     */
    public <E2 extends Throwable> T orElseThrow(final Function<? super E, E2> exceptionMapper) throws IllegalArgumentException, NullPointerException, E2 {
        N.checkArgNotNull(exceptionMapper, cs.exceptionMapper);

        if (exception == null) {
            return value;
        } else {
            throw exceptionMapper.apply(exception);
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
     * <p><b>Method references:</b> a lambda always resolves cleanly here because its arity is written
     * out, and so does a constructor reference whose target has only one applicable arity. A reference
     * such as {@code RuntimeException::new}, whose target has both a no-arg and a cause-taking
     * constructor, fits this overload and {@link #orElseThrow(Function)} equally and must be
     * disambiguated with a cast.</p>
     *
     * @param <E2> the type of exception to be thrown.
     * @param exceptionSupplier the supplier that provides the exception to throw if this Result contains an exception, must not be {@code null}.
     * @return the value contained in this Result if successful.
     * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}.
     * @throws NullPointerException if this Result is a failure and {@code exceptionSupplier} returns
     *         {@code null}; the {@code null} is thrown, which the JVM turns into a
     *         {@code NullPointerException} at the {@code throw} site.
     * @throws E2 if this Result is a failure; the exception is obtained from {@code exceptionSupplier}.
     * @see #orElseThrow(Function)
     * @see #orElseThrow()
     */
    public <E2 extends Throwable> T orElseThrow(final Supplier<? extends E2> exceptionSupplier) throws IllegalArgumentException, NullPointerException, E2 {
        N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the exception contained in this Result, or {@code null} if the Result is successful.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isFailure()) {
     *     Exception ex = result.getException();
     *     log.error("Error occurred", ex);
     * }
     * }</pre>
     *
     * @return the exception if this Result represents a failure, {@code null} if the Result is successful.
     */
    @Beta
    public E getException() {
        return exception;
    }

    /**
     * Converts this Result to a Pair containing both the value and the exception.
     * The first element of the Pair is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be {@code non-null}, but both could be {@code null} or {@code non-null} depending on how the Result was created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, IOException> pair = result.toPair();
     * String value = pair.left();
     * IOException ex = pair.right();
     * }</pre>
     *
     * @return a {@link Pair} where the first element is the value and the second element is the exception.
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
     * Tuple2<String, IOException> tuple = result.toTuple();
     * String value = tuple._1;
     * IOException ex = tuple._2;
     * }</pre>
     *
     * @return a {@link Tuple2} where the first element is the value and the second element is the exception.
     */
    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    /**
     * Returns a hash code value for this Result.
     * The hash code is the exception's hash code when an exception is present, and the value's hash code
     * otherwise. It is therefore consistent with {@link #equals(Object)}: equal Results always hash equally.
     *
     * <p><b>Implementation Note:</b> the exception takes priority, so the value contributes nothing once
     * an exception is present. Two failures carrying the same exception but different values hash the same
     * while comparing unequal &mdash; a legal collision, not an inconsistency. Conversely, two Results with
     * the same value but different exceptions are not required to hash the same, so "same value implies
     * same hash" holds only among successes. Note that {@link Throwable} does not override
     * {@code hashCode}, so a failure's hash is the identity hash of its exception instance and is not
     * stable across JVM runs.
     *
     * @return the hash code of the exception if present, otherwise the hash code of the value.
     */
    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     * Compares this Result with another object for equality.
     * Two Results are considered equal when both their stored values and their stored exceptions
     * are equal. This applies equally to success, failure, and the permitted both-present state.
     *
     * <p><b>Equality Contract:</b></p>
     * <ul>
     *   <li>Returns {@code true} if and only if both Results have equal values AND equal exceptions</li>
     *   <li>Null values are compared using null-safe equality (two nulls are considered equal)</li>
     *   <li>Exception equality is based on the exception's own equals implementation</li>
     *   <li>Type parameters T and E do not need to match exactly, only the actual instances</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, Exception> r1 = Result.of("value", null);
     * Result<String, Exception> r2 = Result.of("value", null);
     * boolean isEqual = r1.equals(r2);   // returns true
     *
     * Result<String, IOException> r3 = Result.of(null, new IOException("error"));
     * Result<String, IOException> r4 = Result.of(null, new IOException("error"));
     * boolean isEqual2 = r3.equals(r4);  // returns false - Throwable does not override equals, so distinct exception instances are never equal
     * }</pre>
     *
     * @param obj the object to compare with this Result.
     * @return {@code true} if the specified object is a Result with equal value and exception, {@code false} otherwise.
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
     * <p><b>Format:</b> The returned string always shows both fields regardless of whether
     * the Result represents success or failure, providing complete visibility into the
     * Result's internal state for troubleshooting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, IOException> success = Result.success("success");
     * success.toString();   // returns "{value=success, exception=null}"
     *
     * Result<String, IOException> failure = Result.failure(new IOException("Error"));
     * failure.toString();   // returns "{value=null, exception=java.io.IOException: Error}"
     * }</pre>
     *
     * @return a string representation of this Result showing both value and exception.
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
     * <p><b>Always create instances with {@link #of(Object, RuntimeException)}.</b> {@code RR} also
     * inherits the static factories {@link Result#success(Object)} and {@link Result#failure(Throwable)},
     * but a static method cannot be narrowed by a subclass: {@code Result.RR.success("x")} compiles and
     * returns a plain {@code Result<String, RuntimeException>}, never an {@code RR<String>}. Assigning
     * it to an {@code RR} variable is a compile error, so the mistake is caught, but the call reads as
     * though it produced an {@code RR}.</p>
     *
     * <pre>{@code
     * Result.RR<String> ok  = Result.RR.of("value", null);              // RR
     * Result.RR<String> bad = Result.RR.success("value");               // does NOT compile
     * Result<String, RuntimeException> r = Result.RR.success("value");  // plain Result
     * }</pre>
     *
     * @param <T> the type of the result value
     * @see #of(Object, RuntimeException)
     */
    @Beta
    public static class RR<T> extends Result<T, RuntimeException> {
        /**
         * Constructs a new RR instance with the specified value and RuntimeException.
         * This constructor is package-private and should be accessed via the {@link #of(Object, RuntimeException)} factory method.
         *
         * @param value the successful result value, may be {@code null}.
         * @param exception the RuntimeException that occurred during the operation, may be {@code null} if operation was successful.
         */
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
         * @param <T> the type of the result value.
         * @param value the successful result value, may be {@code null}.
         * @param exception the RuntimeException that occurred during the operation, may be {@code null} if operation was successful.
         * @return a new RR instance containing either the value or the RuntimeException.
         */
        public static <T> Result.RR<T> of(final T value, final RuntimeException exception) {
            return new RR<>(value, exception);
        }
    }
}
