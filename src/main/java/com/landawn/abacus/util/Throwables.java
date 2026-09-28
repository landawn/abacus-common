/*
 * Copyright (C) 2019 HaiYang Li
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
package com.landawn.abacus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.u.Nullable;

/**
 * A comprehensive utility class providing exception-handling functional interfaces and utilities
 * for working with checked exceptions in functional programming contexts. This final class serves
 * as the central hub for all exception-throwing functional operations, offering a complete set of
 * throwable variants of standard Java functional interfaces, along with utilities for safe execution
 * and exception management in functional code.
 *
 * <p>Throwables bridges the fundamental gap between Java's checked exception system and modern
 * functional programming paradigms. It provides exception-safe versions of all standard functional
 * interfaces. The {@code unchecked()} adapters provided by selected core interfaces make those
 * operations usable where an unchecked functional interface is required; checked failures are
 * converted to runtime exceptions at that boundary.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Complete Functional Interface Coverage:</b> Exception-throwing variants of all standard Java functional interfaces</li>
 *   <li><b>Primitive Type Support:</b> Specialized interfaces for all primitive types (boolean, char, byte, short, int, long, float, double)</li>
 *   <li><b>Multi-Arity Operations:</b> Support for unary, binary, ternary, and n-ary functional operations</li>
 *   <li><b>Type-Safe Exception Handling:</b> Generic exception types provide compile-time safety and documentation</li>
 *   <li><b>Lazy Initialization:</b> Support through {@link N#lazyInitChecked(Throwables.Supplier)}</li>
 *   <li><b>Utility Execution Methods:</b> Safe execution wrappers for exception-throwing operations</li>
 *   <li><b>Standard API Adapters:</b> {@code unchecked()} adapters on selected core interfaces</li>
 *   <li><b>Performance Optimization:</b> Primitive specializations avoid boxing/unboxing overhead</li>
 * </ul>
 *
 * <p><b>Core Problem Solved:</b>
 * Java's functional interfaces (Function, Consumer, Predicate, etc.) cannot throw checked exceptions,
 * forcing developers to either:
 * <ul>
 *   <li>Wrap checked exceptions in RuntimeException (losing type safety)</li>
 *   <li>Use verbose try-catch blocks within lambda expressions</li>
 *   <li>Avoid functional programming patterns when checked exceptions are involved</li>
 * </ul>
 * Throwables solves this by providing exception-throwing variants that maintain type safety
 * and let exception-throwing code be written as lambdas and method references.
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>File I/O Operations:</b> Processing files in streams with IOException handling</li>
 *   <li><b>Database Operations:</b> JDBC operations in functional pipelines with SQLException handling</li>
 *   <li><b>Network Operations:</b> HTTP requests and API calls with various checked exceptions</li>
 *   <li><b>Serialization:</b> Object serialization/deserialization with ClassNotFoundException handling</li>
 *   <li><b>Reflection:</b> Dynamic method invocation with reflection exceptions</li>
 *   <li><b>External API Integration:</b> Third-party library calls that throw checked exceptions</li>
 *   <li><b>Resource Management:</b> Safe resource operations with exception propagation</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // File processing with IOException handling
 * List<String> fileContents = filePaths.stream()
 *     .map(((Throwables.Function<Path, String, IOException>) Files::readString).unchecked())
 *     .collect(Collectors.toList());
 *
 * // Database operations with SQLException handling
 * List<User> users = userIds.stream()
 *     .map(((Throwables.Function<Integer, User, SQLException>) this::findUserById).unchecked())
 *     .collect(Collectors.toList());
 *
 * // Safe execution with exception handling
 * Throwables.run(() -> {
 *     // Code that may throw checked exceptions
 *     Files.delete(tempFile);
 *     connection.close();
 * });
 *
 * // Safe call with fallback for exception-throwing operations
 * String content = Throwables.call(() -> Files.readString(filePath), "");
 *
 * // Lazy initialization with exception handling
 * Throwables.Supplier<DatabaseConnection, SQLException> connectionSupplier =
 *     N.lazyInitChecked(() -> createDatabaseConnection());
 *
 * // Processing with exception-aware operations
 * List<Integer> indices = IntStream.range(0, 100).boxed().collect(Collectors.toList());
 * indices.forEach(i -> Throwables.run(() -> processIndex(i)));
 * }</pre>
 *
 * <p><b>Functional Interface Categories:</b>
 * <ul>
 *   <li><b>Core Interfaces:</b> {@code Supplier<T, E>}, {@code Consumer<T, E>}, {@code Function<T, R, E>}, {@code Predicate<T, E>}</li>
 *   <li><b>Binary Operations:</b> {@code BiFunction<T, U, R, E>}, {@code BiConsumer<T, U, E>}, {@code BiPredicate<T, U, E>}</li>
 *   <li><b>Operators:</b> {@code UnaryOperator<T, E>}, {@code BinaryOperator<T, E>}, {@code TernaryOperator<T, E>}</li>
 *   <li><b>Primitive Suppliers:</b> {@code BooleanSupplier<E>}, {@code IntSupplier<E>}, {@code LongSupplier<E>}, etc.</li>
 *   <li><b>Primitive Consumers:</b> {@code BooleanConsumer<E>}, {@code IntConsumer<E>}, {@code LongConsumer<E>}, etc.</li>
 *   <li><b>Primitive Functions:</b> {@code ToIntFunction<T, E>}, {@code IntFunction<R, E>}, {@code IntToLongFunction<E>}, etc.</li>
 *   <li><b>Primitive Predicates:</b> {@code IntPredicate<E>}, {@code LongPredicate<E>}, {@code DoublePredicate<E>}, etc.</li>
 *   <li><b>Mixed Type Operations:</b> {@code ObjIntConsumer<T, E>}, {@code IntObjFunction<T, R, E>}, etc.</li>
 * </ul>
 *
 * <p><b>Exception Type Safety:</b>
 * <ul>
 *   <li><b>Generic Exception Types:</b> All interfaces are parameterized with exception type {@code <E extends Throwable>}</li>
 *   <li><b>Compile-Time Safety:</b> Exception types are checked at compilation time</li>
 *   <li><b>Documentation:</b> Exception types serve as documentation of possible failure modes</li>
 *   <li><b>Multiple Exceptions:</b> Use a common superclass as the exception type, or the {@link EE}/{@link EEE} interfaces for separate exception types</li>
 * </ul>
 *
 * <p><b>Primitive Type Support:</b>
 * Complete coverage for all primitive types with optimized performance:
 * <ul>
 *   <li><b>Boolean:</b> {@code BooleanSupplier}, {@code BooleanConsumer}, {@code BooleanPredicate}, etc.</li>
 *   <li><b>Character:</b> {@code CharSupplier}, {@code CharConsumer}, {@code CharFunction}, etc.</li>
 *   <li><b>Numeric Types:</b> byte, short, int, long, float, double with full interface coverage</li>
 *   <li><b>Performance Benefit:</b> Avoid boxing/unboxing overhead compared to object variants</li>
 * </ul>
 *
 * <p><b>Multi-Arity Operations:</b>
 * <ul>
 *   <li><b>Unary:</b> Single parameter operations (standard Function, Consumer, Predicate)</li>
 *   <li><b>Binary:</b> Two parameter operations (BiFunction, BiConsumer, BiPredicate)</li>
 *   <li><b>Ternary:</b> Three parameter operations (TriFunction, TriConsumer, TriPredicate)</li>
 *   <li><b>N-Ary:</b> Variable parameter operations (NFunction for flexible parameter counts)</li>
 * </ul>
 *
 * <p><b>Lazy Initialization Support:</b>
 * <ul>
 *   <li><b>{@link N#lazyInitChecked(Throwables.Supplier)}:</b> Thread-safe lazy initialization with exception handling</li>
 *   <li><b>Single Computation:</b> Ensures supplier is called exactly once on success</li>
 *   <li><b>Result Caching:</b> Caches the successfully computed result for all subsequent accesses</li>
 *   <li><b>Memory Efficiency:</b> Minimal overhead until first access</li>
 * </ul>
 *
 * <p><b>Utility Execution Methods:</b>
 * <ul>
 *   <li><b>{@code run()}:</b> Execute exception-throwing Runnable with RuntimeException wrapping</li>
 *   <li><b>{@code call()}:</b> Execute exception-throwing Callable with RuntimeException wrapping</li>
 *   <li><b>Safe Execution:</b> Automatic exception handling and wrapping for functional contexts</li>
 *   <li><b>Error Propagation:</b> Preserve original exception information in wrapped exceptions</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Interface creation: O(1) - minimal object creation overhead</li>
 *   <li>Primitive operations: No boxing/unboxing overhead compared to object variants</li>
 *   <li>Exception handling: Minimal overhead when no exceptions are thrown</li>
 *   <li>Lazy initialization: O(1) access after first computation, thread-safe</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Functional Interfaces:</b> Instances are only as thread-safe as their implementations and captured state</li>
 *   <li><b>Iterators:</b> Stateful and not generally thread-safe</li>
 *   <li><b>Lazy Initialization:</b> Suppliers returned by {@link N#lazyInitChecked(Throwables.Supplier)} are thread-safe</li>
 *   <li><b>Utility Methods:</b> The static methods keep no per-call state, but do not synchronize caller-provided callbacks</li>
 * </ul>
 *
 * <p><b>Integration with Standard APIs:</b>
 * <ul>
 *   <li><b>Stream API:</b> Selected core interfaces can be adapted with {@code unchecked()}</li>
 *   <li><b>Other standard APIs:</b> Use the appropriate unchecked adapter where a JDK functional interface is required</li>
 *   <li><b>Direct invocation:</b> Calling a throwable interface directly preserves its declared exception type</li>
 * </ul>
 *
 * <p><b>Exception Handling Philosophy:</b>
 * <ul>
 *   <li><b>Preserve Type Information:</b> Generic exception types maintain compile-time safety</li>
 *   <li><b>Fail Fast:</b> Exceptions are propagated immediately rather than being silently ignored</li>
 *   <li><b>Limited composition:</b> The interfaces here are mostly bare functional contracts. The only composition
 *       helpers are {@link Predicate#negate()} and the {@code andThen} methods of {@link NFunction} and its primitive
 *       {@code *NFunction} variants; there is no {@code Function.andThen/compose}, {@code Predicate.and/or} or
 *       {@code Consumer.andThen}; {@link Fnn} adds only the {@code not(..)} negations for the predicate
 *       interfaces. Compose other operations with an explicit lambda.</li>
 *   <li><b>Interoperability:</b> Most {@code com.landawn.abacus.util.function} interfaces extend their throwable
 *       counterpart (with {@code RuntimeException}) and offer {@code toThrowable()}; in the other direction,
 *       {@code unchecked()} adapters exist only on {@link Runnable}, {@link Callable}, {@link Supplier},
 *       {@link Predicate}, {@link BiPredicate}, {@link Function}, {@link BiFunction}, {@link UnaryOperator},
 *       {@link BinaryOperator}, {@link Consumer} and {@link BiConsumer}.</li>
 * </ul>
 *
 * <p><b>{@code Error} gets no special treatment.</b> All eight static {@code run}/{@code call} helpers here are
 * bounded on {@link Throwable}, so an {@link Error} is caught and handled exactly like a checked exception -
 * the {@code Error} instance itself is never rethrown:</p>
 * <ul>
 *   <li>{@link #run(Throwables.Runnable)} and {@link #call(Throwables.Callable)} convert it to a runtime exception;</li>
 *   <li>the {@code actionOnError}, {@code supplier} and {@code defaultValue} overloads hand it to the handler or
 *       return the fallback, so it never reaches the caller at all - an {@code OutOfMemoryError} or
 *       {@code StackOverflowError} is silently absorbed;</li>
 *   <li>the {@code predicate} overloads absorb it the same way when the predicate accepts it, and convert it to a
 *       runtime exception when the predicate rejects it.</li>
 * </ul>
 * <p>Use {@link Try#run(Throwables.Runnable)} / {@link Try#call(java.util.concurrent.Callable)} instead when an
 * {@code Error} must reach the caller unchanged.</p>
 *
 * <p><b>Unchecked adapter behavior:</b> An {@code unchecked()} adapter catches any {@link Throwable} from
 * its source operation. A {@link RuntimeException} is normally rethrown as the same instance; a checked
 * exception or {@link Error} is converted to a runtime exception. An {@link InterruptedException} also
 * restores the current thread's interrupted status. Registered exception mappings in {@link ExceptionUtil}
 * can customize the concrete runtime exception.</p>
 *
 * <p><b>Wrapper exceptions are unwrapped before conversion.</b> Every conversion described above - the
 * {@code unchecked()} adapters and the static {@code run}/{@code call} helpers alike - first peels an
 * {@link java.util.concurrent.ExecutionException}, {@link java.lang.reflect.InvocationTargetException} or
 * {@link java.lang.reflect.UndeclaredThrowableException} down to its cause and converts <i>that</i>, so the
 * wrapper instance does not appear in the thrown exception at all. An {@code ExecutionException(IOException)}
 * therefore surfaces as {@code UncheckedIOException(IOException)}, and an
 * {@code ExecutionException(IllegalStateException)} surfaces as that {@code IllegalStateException} itself
 * rather than being wrapped. A wrapper whose cause is {@code null} has nothing to peel and is converted like
 * any other checked exception. This applies only to the rethrown exception: an {@code actionOnError} or
 * {@code predicate} argument is always handed the original, un-peeled throwable.</p>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use specific exception types rather than generic Exception for better error handling</li>
 *   <li>Prefer primitive specializations when working with primitive data for better performance</li>
 *   <li>Use {@link N#lazyInitChecked(Throwables.Supplier)} for expensive computations that may throw exceptions</li>
 *   <li>Combine with {@link Fnn} utility methods for enhanced functional programming capabilities</li>
 *   <li>Consider exception handling strategy at the application boundary rather than within streams</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Methods requiring callbacks validate them eagerly and throw {@link IllegalArgumentException} for {@code null}</li>
 *   <li>Preserves original exception types and stack traces through functional chains</li>
 *   <li>Provides automatic RuntimeException wrapping for checked exceptions in utility methods</li>
 *   <li>Maintains exception causality for debugging and error analysis</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li>Functional interfaces are lightweight with minimal memory footprint</li>
 *   <li>A lazy initializer holds a strong reference to its computed value for its own lifetime; it does not support
 *       weak or soft values, so use an external cache with weak/soft values when a large value must remain
 *       collectable</li>
 *   <li>Primitive specializations reduce memory pressure compared to boxed variants</li>
 *   <li>The shared empty iterator holds no user data; its only field is the inherited close flag, whose flip is
 *       a no-op because it has no resource to release</li>
 * </ul>
 *
 * <p><b>Nested Utility Classes:</b>
 * <ul>
 *   <li><b>{@link Iterator}:</b> An iterator whose {@code hasNext()}/{@code next()} may throw a checked exception</li>
 *   <li><b>{@link EE}:</b> Utility class for handling multiple exception types simultaneously</li>
 *   <li><b>{@link EEE}:</b> Utility class for handling three different exception types in operations</li>
 * </ul>
 *
 * <p><b>Comparison with Standard Functional Interfaces:</b>
 * <ul>
 *   <li><b>Exception Handling:</b> Can throw checked exceptions unlike standard interfaces</li>
 *   <li><b>Type Safety:</b> Compile-time exception type checking and documentation</li>
 *   <li><b>Completeness:</b> Full coverage including primitive types and multi-arity operations</li>
 *   <li><b>Interoperability:</b> Standard-side {@code com.landawn.abacus.util.function} interfaces convert to throwable
 *       ones with {@code toThrowable()}; the reverse {@code unchecked()} adapter exists only on the core object
 *       interfaces listed under "Exception Handling Philosophy"</li>
 * </ul>
 *
 * <p><b>Integration with Fnn:</b>
 * This class works seamlessly with {@link Fnn} which provides factory methods and utilities
 * for creating and manipulating throwable functional interfaces:
 * <ul>
 *   <li>Fnn provides factory methods for creating Throwables functional interfaces</li>
 *   <li>Throwables provides the interface definitions and utility execution methods</li>
 *   <li>Together they form a complete functional programming toolkit with exception handling</li>
 * </ul>
 *
 * @see Seq
 * @see Fnn
 * @see java.util.function
 * @see java.util.stream.Stream
 * @see java.util.Optional
 * @see java.util.concurrent.CompletableFuture
 * @see Supplier
 * @see Consumer
 * @see Function
 * @see Predicate
 */
@SuppressWarnings("java:S6539")
public final class Throwables {

    /**
     * Prevents instantiation of this utility class.
     */
    private Throwables() {
        // Singleton for utility class.
    }

    /**
     * Executes the specified runnable command that may throw a checked exception.
     * If the command throws a checked exception, it will be wrapped in a RuntimeException and rethrown;
     * a runtime exception is rethrown as-is.
     *
     * <p>This method is useful for executing exception-throwing code in contexts where
     * checked exceptions are not allowed, such as within lambda expressions passed to
     * standard functional interfaces.</p>
     *
     * <p><b>Interruption:</b> If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread's interrupted status is restored before the converted
     * exception is thrown.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Execute code that throws IOException
     * Throwables.run(() -> {
     *     Files.delete(tempFile);
     *     Files.createDirectory(newDir);
     * });
     *
     * // Use in stream operations
     * filePaths.forEach(path -> Throwables.run(() -> Files.delete(path)));
     * }</pre>
     *
     * @param command the runnable command to execute that may throw a checked exception
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     * @throws RuntimeException if the command throws an exception; a checked exception (or an {@link Error}) is wrapped in a RuntimeException, while a runtime exception is rethrown as-is.
     *         An {@link java.util.concurrent.ExecutionException}, {@link java.lang.reflect.InvocationTargetException}
     *         or {@link java.lang.reflect.UndeclaredThrowableException} is peeled down to its cause first and that
     *         cause is converted instead, so the wrapper does not appear in what is thrown - see the class-level
     *         "Wrapper exceptions are unwrapped before conversion" note
     * @see Try#run(Throwables.Runnable)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> command) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(command, cs.command);

        try {
            command.run();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the specified runnable command that may throw a checked exception.
     * If the command throws an exception, the specified error handler will be invoked with the exception.
     * An {@link Error} is handled the same way.
     *
     * <p>This method allows custom exception handling logic instead of propagating exceptions.
     * It's useful for logging, recovery, or graceful degradation scenarios.</p>
     *
     * <p><b>Interruption:</b> If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread is re-interrupted before the error handler is invoked.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Log errors instead of throwing
     * Throwables.run(
     *     () -> riskyOperation(),
     *     ex -> logger.error("Operation failed", ex)
     * );
     *
     * // Collect errors for batch processing
     * List<Throwable> errors = new ArrayList<>();
     * files.forEach(file -> Throwables.run(
     *     () -> processFile(file),
     *     errors::add
     * ));
     * }</pre>
     *
     * @param command the runnable command to execute that may throw a checked exception
     * @param actionOnError the consumer that will handle any exception thrown by the command
     * @throws IllegalArgumentException if any of {@code command}, {@code actionOnError} is {@code null}.
     * @see Try#run(Throwables.Runnable, java.util.function.Consumer)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> command, final java.util.function.Consumer<? super Throwable> actionOnError)
            throws IllegalArgumentException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            command.run();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);
            actionOnError.accept(e);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws a checked exception, it will be wrapped in a RuntimeException and rethrown;
     * a runtime exception is rethrown as-is.
     *
     * <p>This method allows using exception-throwing code in functional contexts that require
     * a return value, such as map operations in streams or Optional transformations.</p>
     *
     * <p><b>Interruption:</b> If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread's interrupted status is restored before the converted
     * exception is thrown.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read file content
     * String content = Throwables.call(() -> Files.readString(path));
     *
     * // Use in stream map operation
     * List<String> contents = paths.stream()
     *     .map(p -> Throwables.call(() -> Files.readString(p)))
     *     .collect(Collectors.toList());
     *
     * // Parse with exceptions
     * Config config = Throwables.call(() -> objectMapper.readValue(json, Config.class));
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable
     * @param command the callable command to execute that may throw a checked exception
     * @return the result returned by the callable command
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     * @throws RuntimeException if the command throws an exception; a checked exception (or an {@link Error}) is wrapped in a RuntimeException, while a runtime exception is rethrown as-is.
     *         An {@link java.util.concurrent.ExecutionException}, {@link java.lang.reflect.InvocationTargetException}
     *         or {@link java.lang.reflect.UndeclaredThrowableException} is peeled down to its cause first and that
     *         cause is converted instead, so the wrapper does not appear in what is thrown - see the class-level
     *         "Wrapper exceptions are unwrapped before conversion" note
     * @see Try#call(java.util.concurrent.Callable)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(command, cs.command);

        try {
            return command.call();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the specified error handler function will be invoked with the exception
     * and its result will be returned instead. An {@link Error} is handled the same way.
     *
     * <p>This method enables transforming exceptions into valid return values, useful for
     * error recovery and functional error handling patterns.</p>
     *
     * <p><b>Interruption:</b> If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread is re-interrupted before the error handler is invoked.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).</p>
     *
     * <p><b>Overload selection:</b> the {@code call} overload that a second argument binds to is chosen by ordinary
     * Java overload resolution on the argument's static type. A handler lambda or method reference selects this
     * overload. A handler <i>variable</i> selects it when its type is a {@code java.util.function.Function} (including
     * {@code com.landawn.abacus.util.function.Function}) whose input type accepts a {@code Throwable}, such as
     * {@code Function<Throwable, X>}, {@code Function<Object, X>} or {@code Function<? super Throwable, X>}.
     * {@code R} is then inferred from both {@code X} and the command's result type, so a
     * {@code Function<Throwable, Object>} handler compiles only with an {@code Object}-typed target. Any other
     * variable cannot bind here, for example a {@code Function<Exception, String>} (it cannot accept a
     * {@code Throwable}) or a {@code Throwables.Function} (not a {@code java.util.function.Function}). When the call's
     * target admits it, such as an {@code Object} target, that variable binds to
     * {@link #call(Throwables.Callable, Object)} and is returned as the default value itself, without being invoked.
     * Otherwise the call does not compile. See {@link #call(Throwables.Callable, Object)} for examples.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Return empty list on error
     * List<String> lines = Throwables.call(
     *     () -> Files.readAllLines(path),
     *     ex -> Collections.emptyList()
     * );
     *
     * // Transform exception to error response
     * Response response = Throwables.call(
     *     () -> apiClient.fetchData(),
     *     ex -> Response.error(ex.getMessage())
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the error handler
     * @param command the callable command to execute that may throw a checked exception
     * @param actionOnError the function that will handle any exception thrown by the command and provide an alternative result
     * @return the result returned by the callable command if successful, or the result of the error handler if an exception occurs
     * @throws IllegalArgumentException if any of {@code command}, {@code actionOnError} is {@code null}.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Function)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command,
            final java.util.function.Function<? super Throwable, ? extends R> actionOnError) throws IllegalArgumentException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            return command.call();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);
            return actionOnError.apply(e);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the result from the specified supplier will be returned instead.
     * An {@link Error} is handled the same way.
     * This method provides a safe way to handle exceptions by providing a fallback value supplier.
     * If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread is re-interrupted before the fallback supplier is invoked.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).
     *
     * <p><b>Overload selection:</b> the {@code call} overload that a second argument binds to is chosen by ordinary
     * Java overload resolution on the argument's static type, so a {@code Supplier} <i>variable</i> is not always
     * invoked. A supplier lambda or method reference, such as {@code () -> "x"} or {@code v::get}, selects this
     * overload. A supplier variable selects it when its type is a {@code java.util.function.Supplier<X>} whose
     * element type {@code X} can take the command's result. That includes a
     * {@code com.landawn.abacus.util.function.Supplier<X>}, and a {@code Supplier<? super String>} for a command that
     * returns a {@code String}. {@code R} is then {@code X}. If the call's target cannot accept {@code X}, the call
     * does not compile; it does not fall back to the value overload. For example, passing a {@code Supplier<Object>}
     * while assigning the result to a {@code String} does not compile. Any other variable binds to
     * {@link #call(Throwables.Callable, Object)} when the call's target admits it, such as an {@code Object} target.
     * It is then returned as the default <i>value</i> itself and is never invoked. Such variables include:</p>
     * <ul>
     *   <li>a {@code Throwables.Supplier}, which is not a {@code java.util.function.Supplier};</li>
     *   <li>a {@code Supplier<Integer>} when the command returns a {@code String};</li>
     *   <li>a {@code Supplier<? extends String>} when the command returns a {@code String}, because a {@code String}
     *       need not fit the unknown subtype. The same variable <i>is</i> invoked when the command lambda returns no
     *       value of its own and only throws, such as {@code () -> { throw new IOException(); }};</li>
     *   <li>a {@code Supplier<String>} when the command itself returns a {@code Supplier<String>}; here the supplier
     *       is the intended default value.</li>
     * </ul>
     * <p>See {@link #call(Throwables.Callable, Object)} for examples. To make sure a supplier variable {@code v} is
     * invoked, pass {@code v::get}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Throwables.call(
     *     () -> riskyOperation(),
     *     () -> "default value"
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the supplier
     * @param command the callable command to execute that may throw a checked exception
     * @param supplier the supplier that provides an alternative result if the command throws an exception
     * @return the result returned by the callable command if successful, or the result from the supplier if an exception occurs
     * @throws IllegalArgumentException if any of {@code command}, {@code supplier} is {@code null}.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command, final java.util.function.Supplier<R> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return command.call();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);
            return supplier.get();
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the specified default value will be returned instead.
     * An {@link Error} is handled the same way.
     *
     * <p>This is the simplest form of error handling with a known fallback value.</p>
     * If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread's interrupted status is restored.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).
     *
     * <p><b>Note:</b> The default value may be any object, including a non-{@link Comparable} one such as a
     * {@code List}. A lambda or method reference as the second argument selects the {@code Supplier} or
     * {@code Function} overload instead. For a <i>variable</i>, ordinary Java overload resolution on its static type
     * decides. A {@code java.util.function.Supplier} or {@code java.util.function.Function} variable, including the
     * {@code com.landawn.abacus.util.function} subtypes, is invoked when it fits the {@code Supplier} or
     * {@code Function} overload. {@link #call(Throwables.Callable, java.util.function.Supplier)} and
     * {@link #call(Throwables.Callable, java.util.function.Function)} give the exact conditions. Any other variable
     * binds here when the call's target admits it, and is returned as the default value itself without being
     * invoked. Whether a {@code Supplier<? extends String>} is invoked can therefore depend on the command, as the
     * examples below show.
     * A bare {@code null} default is ambiguous between the overloads and does not compile; write a typed null such as
     * {@code (String) null}.</p>
     *
     * <p><b>BREAKING (1.1.2):</b> {@code R} was bounded by {@code Comparable<? super R>} in earlier releases. Dropping
     * the bound changed this method's erasure from {@code Comparable} to {@code Object}: source-compatible, but code
     * compiled against an earlier release must be recompiled or it fails with {@code NoSuchMethodError}.</p>
     *
     * <p><b>Overload selection examples</b> (each command fails, as when {@code path} does not exist):</p>
     * <pre>{@code
     * java.util.function.Supplier<? extends String> s = () -> "fallback";
     * Object a = Throwables.call(() -> { throw new IOException(); }, s);  // "fallback": s is invoked
     * Object b = Throwables.call(() -> Files.readString(path), s);        // s itself: the default value
     *
     * com.landawn.abacus.util.function.Supplier<String> t = () -> "fallback";
     * String c = Throwables.call(() -> Files.readString(path), t);       // "fallback": t is invoked
     *
     * Throwables.Supplier<String, RuntimeException> u = () -> "fallback";
     * java.util.function.Function<Exception, String> f = e -> "handled";
     * Object d = Throwables.call(() -> Files.readString(path), u);  // u itself: not a java.util.function.Supplier
     * Object g = Throwables.call(() -> Files.readString(path), f);  // f itself: cannot accept a Throwable
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parse with default
     * int value = Throwables.call(() -> Integer.parseInt(userInput), 0);
     *
     * // Load configuration with fallback
     * String setting = Throwables.call(
     *     () -> loadFromFile("config.properties"),
     *     "default-config"
     * );
     *
     * // Any fallback type works
     * List<String> lines = Throwables.call(() -> Files.readAllLines(path), Collections.emptyList());
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the default value
     * @param command the callable command to execute that may throw a checked exception
     * @param defaultValue the default value to return if the command throws an exception
     * @return the result returned by the callable command if successful, or the default value if an exception occurs
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     * @see #call(Throwables.Callable, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(command, cs.command);

        try {
            return command.call();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);
            return defaultValue;
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception and the predicate returns {@code true} for that exception,
     * the result from the supplier will be returned. If the predicate returns {@code false},
     * the exception will be rethrown, wrapped in a RuntimeException if it is a checked exception.
     * An {@link Error} is passed to the predicate like any other throwable.
     *
     * <p>This method enables selective exception handling based on exception type or properties.</p>
     * If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread is re-interrupted before the predicate is evaluated.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).
     * The third argument binds by the same overload-resolution rule that
     * {@link #call(Throwables.Callable, java.util.function.Supplier)} describes. A supplier lambda or method reference
     * selects this overload. So does a {@code java.util.function.Supplier<X>} variable, including a
     * {@code com.landawn.abacus.util.function.Supplier<X>}, whose element type {@code X} can take the command's result.
     * Any other variable binds, when the call's target admits it, to
     * {@link #call(Throwables.Callable, java.util.function.Predicate, Object)} and is returned as the default value
     * itself. Examples are a {@code Throwables.Supplier}, or a {@code Supplier<? extends String>} with a command that
     * returns a {@code String}. A bare {@code null} third argument also binds <i>here</i> and fails with
     * {@link IllegalArgumentException}; write a typed null such as {@code (String) null} for a {@code null} default.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Handle only IOException
     * String content = Throwables.call(
     *     () -> Files.readString(path),
     *     ex -> ex instanceof IOException,
     *     () -> "default content"
     * );
     *
     * // Fall back to the cache on specific errors
     * Data result = Throwables.call(
     *     () -> fetchFromRemote(),
     *     ex -> ex.getMessage() != null && ex.getMessage().contains("timeout"),
     *     () -> fetchFromCache()
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the supplier
     * @param command the callable command to execute that may throw a checked exception
     * @param predicate the predicate that tests whether to handle the exception or rethrow it; it receives the
     *        exception exactly as thrown, including any {@code ExecutionException}/{@code InvocationTargetException}/
     *        {@code UndeclaredThrowableException} wrapper - only the rethrow path peels those off
     * @param supplier the supplier that provides an alternative result if the predicate returns true
     * @return the result returned by the callable command if successful, or the result from the supplier if an exception occurs and the predicate returns true
     * @throws IllegalArgumentException if any of {@code command}, {@code predicate}, {@code supplier} is {@code null}.
     * @throws RuntimeException if the command throws an exception and the predicate returns false
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Predicate, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command,
            final java.util.function.Predicate<? super Throwable> predicate, final java.util.function.Supplier<R> supplier)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(predicate, cs.predicate);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return command.call();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);

            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception and the predicate returns {@code true} for that exception,
     * the specified default value will be returned. If the predicate returns {@code false},
     * the exception will be rethrown, wrapped in a RuntimeException if it is a checked exception.
     * An {@link Error} is passed to the predicate like any other throwable.
     *
     * <p>Combines predicate-based exception filtering with a simple default value.</p>
     * If the command throws an {@link InterruptedException} - bare, or wrapped in an
     * {@code InvocationTargetException}/{@code UndeclaredThrowableException} (not an {@code ExecutionException},
     * which reports another thread) - the current thread is re-interrupted before the predicate is evaluated.
     * Only those two wrapper types are looked through: an {@code InterruptedException} that is the cause of any
     * other exception (for example {@code new RuntimeException(ie)}) or is suppressed on one leaves the status
     * unchanged, whereas the {@link Try} {@code run}/{@code call} methods search the whole cause and suppressed chain,
     * except beneath an {@code ExecutionException} or {@code CompletionException} (both report another thread).
     *
     * <p><b>Note:</b> The default value may be any object, including a non-{@link Comparable} one such as a
     * {@code List}. A lambda or method reference as the third argument selects the {@code Supplier} overload instead.
     * For a {@code Supplier} <i>variable</i>, ordinary Java overload resolution on its static type decides, as described
     * on {@link #call(Throwables.Callable, java.util.function.Supplier)}. A {@code java.util.function.Supplier<X>}
     * (including a {@code com.landawn.abacus.util.function.Supplier<X>}) whose {@code X} can take the command's
     * result is invoked. Any other variable, for example a {@code Throwables.Supplier} or a
     * {@code Supplier<? extends String>} with a {@code String}-returning command, is accepted here as the default
     * value itself when the call's target admits it, and is never invoked. A bare {@code null}
     * default binds to the {@code Supplier} overload, which rejects it with {@link IllegalArgumentException}; write a
     * typed null such as {@code (String) null}, as in the example below.</p>
     *
     * <p><b>BREAKING (1.1.2):</b> {@code R} was bounded by {@code Comparable<? super R>} in earlier releases. Dropping
     * the bound changed this method's erasure from {@code Comparable} to {@code Object}: source-compatible, but code
     * compiled against an earlier release must be recompiled or it fails with {@code NoSuchMethodError}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Return -1 only for NumberFormatException
     * int value = Throwables.call(
     *     () -> Integer.parseInt(input),
     *     ex -> ex instanceof NumberFormatException,
     *     -1
     * );
     *
     * // Return null only when the file does not exist
     * String content = Throwables.call(
     *     () -> Files.readString(path),
     *     ex -> ex instanceof NoSuchFileException,
     *     (String) null
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the default value
     * @param command the callable command to execute that may throw a checked exception
     * @param predicate the predicate that tests whether to handle the exception or rethrow it; it receives the
     *        exception exactly as thrown, including any {@code ExecutionException}/{@code InvocationTargetException}/
     *        {@code UndeclaredThrowableException} wrapper - only the rethrow path peels those off
     * @param defaultValue the default value to return if the predicate returns true
     * @return the result returned by the callable command if successful, or the default value if an exception occurs and the predicate returns true
     * @throws IllegalArgumentException if any of {@code command}, {@code predicate} is {@code null}.
     * @throws RuntimeException if the command throws an exception and the predicate returns false
     * @see #call(Throwables.Callable, java.util.function.Predicate, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<? extends R, ? extends Throwable> command,
            final java.util.function.Predicate<? super Throwable> predicate, final R defaultValue) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(predicate, cs.predicate);

        try {
            return command.call();
        } catch (final Throwable e) {
            restoreInterruptedStatusIfNeeded(e);

            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Restores the current thread's interrupted status when {@code e} is an interruption signal: an
     * {@link InterruptedException} itself, or one wrapped in an
     * {@link java.lang.reflect.InvocationTargetException} or {@link java.lang.reflect.UndeclaredThrowableException}
     * (looked through up to {@code MAX_INTERRUPT_UNWRAP_DEPTH} = 100 nested wrappers, the same bound as
     * {@code ExceptionUtil.toRuntimeException}; a deeper chain leaves the status unchanged).
     * An {@code InterruptedException} found under an {@link java.util.concurrent.ExecutionException} was raised on
     * another thread and does not interrupt this one.
     *
     * @param e the failure caught from a user-supplied operation
     */
    private static void restoreInterruptedStatusIfNeeded(Throwable e) {
        // Mirror ExceptionUtil.toRuntimeException(e, true), which the rethrow paths use, so that a handled or
        // fallback-absorbed failure keeps the interrupt exactly when a rethrown one would: InvocationTargetException
        // and UndeclaredThrowableException are raised on the calling thread and keep the interrupt's meaning, while an
        // ExecutionException reports another thread's failure. The depth bound guards against cyclic cause chains
        // (ExceptionUtil also gives up without interrupting when its equivalent bound is exhausted).
        for (int depth = 0; depth <= MAX_INTERRUPT_UNWRAP_DEPTH; depth++) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return;
            }

            if (e instanceof java.util.concurrent.ExecutionException || e == null) {
                return;
            }

            if (!(e instanceof java.lang.reflect.InvocationTargetException || e instanceof java.lang.reflect.UndeclaredThrowableException)) {
                return;
            }

            e = e.getCause();
        }
    }

    /** Same depth budget as {@code ExceptionUtil}'s wrapper-unwrapping loop. */
    private static final int MAX_INTERRUPT_UNWRAP_DEPTH = 100;

    /**
     * Shared instance used by all empty iterators; it holds no user data and its inherited close flag releases nothing.
     */
    @SuppressWarnings("rawtypes")
    private static final Throwables.Iterator EMPTY = new Throwables.Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         * @throws NoSuchElementException if this iterator has no remaining element
         */
        @Override
        public Object next() throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * An iterator that can throw checked exceptions during iteration.
     * This iterator provides hasNext(), next(), and other iteration methods that can throw exceptions of type E.
     * It declares its own {@link #closeResource()} method and deliberately does not implement
     * {@code AutoCloseable}, so it cannot be used in try-with-resources; close it explicitly
     * (for example in a {@code finally} block) when it holds resources. Like every iterator, it is
     * stateful and consumption changes its cursor; it is neither immutable nor generally thread-safe.
     * Exhaustion and terminal operations do not close the iterator automatically. Callers must close
     * resource-backed iterators. Closing an iterator returned by
     * {@link #filter(Throwables.Predicate)} or {@link #map(Throwables.Function)} closes its source.
     * What a closed iterator does next depends on the factory: iterators returned by {@link #defer(java.util.function.Supplier)},
     * {@link #concat(Collection)}, {@link #filter(Throwables.Predicate)}, {@link #map(Throwables.Function)} and
     * {@link #ofLines(Reader)} report themselves exhausted once closed, while the resource-free iterators returned by
     * {@link #empty()}, {@link #just(Object)}, {@link #of(Object...)}, {@link #of(Object[], int, int)},
     * {@link #of(Iterable)} and {@link #of(java.util.Iterator)} ignore {@link #closeResource()} and keep iterating.
     *
     * <p><b>Note on the factory methods' exception bound:</b> this class is declared with
     * {@code <E extends Throwable>}, but {@link #of(Object...)}, {@link #of(Object[], int, int)},
     * {@link #of(java.util.Iterator)}, {@link #defer(java.util.function.Supplier)} and the two {@code concat}
     * overloads bound {@code E} to {@link Exception} instead. That is deliberate: because a factory call used as
     * the <i>receiver</i> of a chained call is a standalone expression, {@code E} is inferred from the bound alone
     * there, so widening the bound would silently change {@code Throwables.Iterator.of(x).filter(p)} from
     * {@code Iterator<X, Exception>} to {@code Iterator<X, Throwable>} and break existing call sites. Use
     * {@link #empty()}, {@link #just(Object)} or {@link #of(Iterable)} when {@code E} must be {@code Throwable}.</p>
     *
     * @param <T> the type of elements in the iterator
     * @param <E> the type of exception that may be thrown
     * @see ObjIterator
     */
    @SuppressWarnings("java:S6548")
    public abstract static class Iterator<T, E extends Throwable> {
        /**
         * Whether a failed bulk advance leaves the logical output position unchanged.
         * Internal arithmetic-only iterators opt in so slicing can retain bulk advancement;
         * other iterators are advanced one successfully returned element at a time.
         *
         * @return whether {@link #advance(long)} is failure-atomic
         * @throws E if determining the capability requires initialization that fails
         */
        boolean supportsFailureAtomicAdvance() throws E {
            return false;
        }

        /**
         * Constructor for subclasses.
         */
        protected Iterator() {
        }

        /**
         * Returns an empty iterator that has no elements and whose hasNext() always returns {@code false}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, IOException> empty = Throwables.Iterator.empty();
         * assert !empty.hasNext();  // always returns false
         *
         * Throwables.Iterator<Integer, RuntimeException> noItems = Throwables.Iterator.empty();
         * }</pre>
         *
         * @param <T> the type of elements that would be returned by this iterator
         * @param <E> the type of exception that may be thrown
         * @return an empty iterator
         */
        public static <T, E extends Throwable> Throwables.Iterator<T, E> empty() {
            return EMPTY;
        }

        /**
         * Returns an iterator containing only the specified single element.
         *
         * <p>The returned iterator holds no resource, so {@link #closeResource()} is a no-op on it: it does
         * <i>not</i> end the iteration, and the remaining elements can still be read after it is called.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, IOException> single = Throwables.Iterator.just("hello");
         * String value = single.next();  // returns "hello"
         * assert !single.hasNext();      // returns false (iterator exhausted)
         *
         * Throwables.Iterator<Integer, RuntimeException> one = Throwables.Iterator.just(42);
         * }</pre>
         *
         * @param <T> the type of the element
         * @param <E> the type of exception that may be thrown
         * @param value the single element to be contained in the iterator
         * @return an iterator containing only the specified element
         */
        public static <T, E extends Throwable> Throwables.Iterator<T, E> just(final T value) {
            return new Throwables.Iterator<>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if this iterator has no remaining element
                 */
                @Override
                public T next() throws NoSuchElementException {
                    if (done) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    done = true;

                    return value;
                }
            };
        }

        /**
         * Returns an iterator over the specified array of elements.
         *
         * <p>The returned iterator holds no resource, so {@link #closeResource()} is a no-op on it: it does
         * <i>not</i> end the iteration, and the remaining elements can still be read after it is called.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
         * String first = iter.next(); // returns "a"
         *
         * String[] data = {"x", "y", "z"};
         * Throwables.Iterator<String, RuntimeException> iter2 = Throwables.Iterator.of(data);
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param <E> the type of exception that may be thrown
         * @param a the array of elements to iterate over
         * @return an iterator over the elements in the array, or an empty iterator if the array is {@code null} or empty
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final T... a) {
            return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
        }

        /**
         * Returns an iterator over a range of elements in the specified array.
         *
         * <p>The returned iterator holds no resource, so {@link #closeResource()} is a no-op on it: it does
         * <i>not</i> end the iteration, and the remaining elements can still be read after it is called.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String[] data = {"a", "b", "c", "d", "e"};
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of(data, 1, 4);
         * iter.next();  // returns "b"
         * iter.next();  // returns "c"
         * iter.next();  // returns "d"
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param <E> the type of exception that may be thrown
         * @param a the array of elements to iterate over
         * @param fromIndex the starting index (inclusive) of the range to iterate
         * @param toIndex the ending index (exclusive) of the range to iterate
         * @return an iterator over the specified range of elements in the array, or an empty iterator if the array
         *         is {@code null} or empty, or {@code fromIndex} equals {@code toIndex}
         * @throws IndexOutOfBoundsException if fromIndex is negative, toIndex is greater than the array length,
         *         or fromIndex is greater than toIndex
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final T[] a, final int fromIndex, final int toIndex)
                throws IndexOutOfBoundsException {
            N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

            if (N.isEmpty(a) || fromIndex == toIndex) {
                return EMPTY;
            }

            return new Throwables.Iterator<>() {
                private int cursor = fromIndex;

                @Override
                boolean supportsFailureAtomicAdvance() {
                    return true;
                }

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if this iterator has no remaining element
                 */
                @Override
                public T next() throws NoSuchElementException {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return a[cursor++];
                }

                @Override
                public void advance(final long n) throws E {
                    if (n <= 0) {
                        return;
                    }

                    final long remaining = toIndex - cursor;
                    if (n >= remaining) {
                        cursor = toIndex;
                    } else {
                        // Safe cast since n < remaining and remaining fits in int
                        cursor += (int) n;
                    }
                }

                @Override
                public long count() {
                    final int ret = toIndex - cursor; //NOSONAR
                    cursor = toIndex; // Mark as finished.
                    return ret;
                }
            };
        }

        /**
         * Returns an iterator over the elements in the specified Iterable.
         *
         * <p>The returned iterator holds no resource, so {@link #closeResource()} is a no-op on it: it does
         * <i>not</i> end the iteration, and the remaining elements can still be read after it is called.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> list = Arrays.asList("a", "b", "c");
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of(list);
         * iter.next(); // returns "a"
         * }</pre>
         *
         * @param <T> the type of elements in the iterable
         * @param <E> the type of exception that may be thrown
         * @param iterable the iterable whose elements are to be iterated over
         * @return an iterator over the elements in the iterable, or an empty iterator if the iterable is null
         */
        public static <T, E extends Throwable> Iterator<T, E> of(final Iterable<? extends T> iterable) {
            if (iterable == null) {
                return empty();
            }

            final java.util.Iterator<? extends T> iter = iterable.iterator();

            return new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public T next() throws E {
                    return iter.next();
                }
            };
        }

        /**
         * Returns a Throwables.Iterator that wraps the specified java.util.Iterator.
         *
         * <p>{@link #closeResource()} is a no-op on the returned iterator: it neither ends the iteration nor
         * releases the wrapped iterator, whose remaining elements can still be read after it is called.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * java.util.Iterator<String> utilIter = Arrays.asList("x", "y").iterator();
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of(utilIter);
         * iter.next(); // returns "x"
         * }</pre>
         *
         * @param <T> the type of elements returned by the iterator
         * @param <E> the type of exception that may be thrown
         * @param iterator the java.util.Iterator to wrap
         * @return a Throwables.Iterator wrapping the specified iterator, or an empty iterator if {@code iterator} is null
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final java.util.Iterator<? extends T> iterator) {
            if (iterator == null) {
                return EMPTY;
            }

            return new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() throws E {
                    return iterator.hasNext();
                }

                @Override
                public T next() throws E {
                    return iterator.next();
                }
            };
        }

        /**
         * Returns a Throwables.Iterator instance that is created lazily using the provided Supplier.
         * The Supplier is responsible for producing the Iterator instance when the Iterator's methods are first called.
         * This is useful for deferring expensive operations until the iterator is actually used.
         *
         * <p>The iterator is initialized on the first call to {@code hasNext()}, {@code next()}, a positive
         * {@code advance(long)}, or {@code count()}. A non-positive advance remains a no-op and does not initialize it.
         * The underlying iterator is only closed if it has been initialized when {@code closeResource()} is called.
         * If creation fails with an unchecked exception, that exception is propagated and creation is retried on the next access
         * while the wrapper remains open.
         * Recursive initialization throws {@link IllegalStateException} and also leaves creation retryable.
         * If the supplier closes the wrapper during creation, its returned iterator is immediately closed
         * and the wrapper remains exhausted.
         * The returned iterator throws {@link IllegalStateException} on access if {@code iteratorSupplier}
         * returns {@code null} or the wrapper itself; this factory does not invoke the supplier.
         * Closing before initialization releases the supplier without invoking it, so a closed wrapper can never
         * acquire a resource. After {@code closeResource()} the wrapper reports itself exhausted, exactly as
         * {@link #concat(Collection)}, {@link #filter(Throwables.Predicate)} and {@link #map(Throwables.Function)}
         * do: {@code hasNext()} returns {@code false}, {@code next()} throws {@link NoSuchElementException},
         * {@code advance(long)} is a no-op and {@code count()} returns {@code 0}. This includes a close made by the
         * underlying iterator's own {@code hasNext()}: that call then returns {@code false} whatever the underlying
         * iterator reported.
         *
         * <p>Unlike {@code concat}, {@code filter} and {@code map}, which count and skip element by element, this
         * wrapper hands {@code count()} and a positive {@code advance(long)} to the underlying iterator's own
         * {@code count()} and {@code advance(long)}, so a bulk implementation of the underlying iterator is kept. Those
         * two methods therefore see only a close made before they are called. If the underlying iterator closes this
         * wrapper while such a delegated call is running, the call still runs to completion and its result stands:
         * {@code count()} can then include elements after the point of the close, where {@code map} and {@code filter}
         * would stop counting at the close.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, IOException> iter = Throwables.Iterator.defer(() ->
         *         Throwables.Iterator.ofLines(Throwables.call(() -> new FileReader("large-file.txt"))));
         * try {
         *     // The file is not opened until this first access.
         *     while (iter.hasNext()) {
         *         System.out.println(iter.next());
         *     }
         * } finally {
         *     // See ofLines(Reader) for closing without discarding the exception that ended the loop.
         *     iter.closeResource();
         * }
         * }</pre>
         *
         * @param <T> the type of the elements in the Iterator.
         * @param <E> the type of the exception that may be thrown.
         * @param iteratorSupplier a Supplier that provides the Throwables.Iterator when needed; it may supply an
         *        iterator of any subtype of {@code T} that throws any subtype of {@code E}, so for example
         *        {@code defer(() -> ofLines(reader))} can be typed as {@code Throwables.Iterator<String, Exception>}.
         * @return a Throwables.Iterator that is initialized on the first call to {@code hasNext()}, {@code next()},
         *         a positive {@code advance(long)}, or {@code count()}
         * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}.
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> defer(
                final java.util.function.Supplier<? extends Throwables.Iterator<? extends T, ? extends E>> iteratorSupplier) throws IllegalArgumentException {
            N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

            return new DeferredIterator<>(iteratorSupplier);
        }

        // Keep lifecycle wrappers static: anonymous classes can retain synthetic source/callback captures
        // even after their explicitly clearable fields have been nulled during close.
        private static final class DeferredIterator<T, E extends Exception> extends Throwables.Iterator<T, E> {
            private Throwables.Iterator<? extends T, ? extends E> iter = null;
            private java.util.function.Supplier<? extends Throwables.Iterator<? extends T, ? extends E>> supplier;
            private boolean isInitialized = false;
            private boolean isClosed = false;
            private boolean isInitializing = false;
            private IllegalStateException recursiveFailure;

            private DeferredIterator(final java.util.function.Supplier<? extends Throwables.Iterator<? extends T, ? extends E>> supplier) {
                this.supplier = supplier;
            }

            @Override
            boolean supportsFailureAtomicAdvance() throws E {
                return init() && iter.supportsFailureAtomicAdvance();
            }

            @Override
            public boolean hasNext() throws E {
                if (!init()) {
                    return false;
                }

                // The source's hasNext() may close this wrapper re-entrantly, which nulls `iter`. Call it through a
                // local and re-check the flag afterwards, so a wrapper closed mid-call reports exhaustion instead of
                // returning true and then throwing NoSuchElementException from next().
                final Throwables.Iterator<? extends T, ? extends E> src = iter;
                final boolean hasNext = src.hasNext();

                return hasNext && !isClosed;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator is closed or has no remaining element
             * @throws E if creating or advancing the source iterator throws an exception
             */
            @Override
            public T next() throws NoSuchElementException, E {
                if (!init()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            public void advance(final long n) throws E {
                if (n <= 0 || !init()) {
                    return;
                }

                iter.advance(n);
            }

            @Override
            public long count() throws E {
                // Delegated (not counted through hasNext()/next()) to keep the source's bulk count. The price, as
                // documented: a close made by the source during this call does not stop the delegated count.
                return init() ? iter.count() : 0;
            }

            @Override
            protected void closeResourceInternal() {
                isClosed = true;
                supplier = null;

                if (iter != null) {
                    try {
                        iter.closeResource();
                    } finally {
                        iter = null;
                    }
                }
            }

            private boolean init() {
                if (isClosed) {
                    return false;
                }

                if (!isInitialized) {
                    if (isInitializing) {
                        if (recursiveFailure == null) {
                            recursiveFailure = new IllegalStateException("Recursive initialization of deferred iterator");
                        }
                        throw recursiveFailure;
                    }

                    isInitializing = true;
                    try {
                        final Throwables.Iterator<? extends T, ? extends E> supplied = supplier.get();
                        if (supplied == null) {
                            throw new IllegalStateException("Iterator supplier returned null");
                        }
                        // A self-return does not reenter initialization, but would recurse forever during delegation.
                        if (supplied == this) {
                            throw recursiveFailure == null ? new IllegalStateException("Iterator supplier returned the deferred iterator itself")
                                    : recursiveFailure;
                        }
                        if (recursiveFailure != null) {
                            // Even if the supplier swallowed the recursive failure, its abandoned resource must be closed.
                            try {
                                supplied.closeResource();
                            } catch (final Throwable failure) {
                                if (failure != recursiveFailure) {
                                    recursiveFailure.addSuppressed(failure);
                                }
                            }
                            throw recursiveFailure;
                        }
                        if (isClosed) {
                            // The supplier can close the wrapper before returning a newly acquired resource.
                            supplied.closeResource();
                            return false;
                        }
                        iter = supplied;
                        supplier = null;
                        isInitialized = true;
                    } finally {
                        isInitializing = false;
                        recursiveFailure = null;
                    }
                }
                return true;
            }
        }

        /**
         * Concatenates multiple iterators into a single iterator that iterates over all elements
         * from all the iterators in sequence.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter1 = Throwables.Iterator.of("a", "b");
         * Throwables.Iterator<String, RuntimeException> iter2 = Throwables.Iterator.of("c", "d");
         * Throwables.Iterator<String, RuntimeException> combined = Throwables.Iterator.concat(iter1, iter2);
         * combined.next();  // returns "a"
         * combined.next();  // returns "b"
         * combined.next();  // returns "c"
         * combined.next();  // returns "d"
         * }</pre>
         *
         * @param <T> the type of elements returned by the iterators
         * @param <E> the type of exception that may be thrown
         * @param a the array of iterators to concatenate
         * @return a single iterator that iterates over all elements from all iterators in order,
         *         skipping {@code null} entries, or an empty iterator if {@code a} is {@code null} or empty
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> concat(final Throwables.Iterator<? extends T, ? extends E>... a) {
            return concat(N.toList(a));
        }

        /**
         * Concatenates a collection of iterators into a single iterator that iterates over all elements
         * from all the iterators in sequence.
         * Closing the returned iterator closes every supplied iterator, including iterators that have
         * not yet been reached. If multiple close operations fail, the first failure is rethrown after
         * all iterators have been closed and distinct later failures are attached as suppressed exceptions.
         * Source references are released after close, and the returned iterator is then exhausted. This also holds
         * when a source closes the returned iterator from inside its own {@code hasNext()}: the returned iterator
         * reports {@code false} instead of moving on to the next source.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Throwables.Iterator<String, RuntimeException>> iterators = Arrays.asList(
         *     Throwables.Iterator.of("a", "b"),
         *     Throwables.Iterator.of("c", "d")
         * );
         * Throwables.Iterator<String, RuntimeException> combined = Throwables.Iterator.concat(iterators);
         * combined.next(); // returns "a"
         * }</pre>
         *
         * @param <T> the type of elements returned by the iterators
         * @param <E> the type of exception that may be thrown
         * @param c the collection of iterators to concatenate
         * @return a single iterator that iterates over all elements from all iterators in order,
         *         skipping {@code null} entries, or an empty iterator if the collection is {@code null} or empty
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> concat(final Collection<? extends Throwables.Iterator<? extends T, ? extends E>> c) {
            if (N.isEmpty(c)) {
                return Iterator.empty();
            }

            final List<Throwables.Iterator<? extends T, ? extends E>> sources = new ArrayList<>(c);

            return new Throwables.Iterator<>() {
                // An index rather than sources.iterator(): closing this wrapper clears the list, and a source callback
                // may do that re-entrantly while hasNext() is walking it (an ArrayList iterator would then throw CME).
                private int cursor;
                private Throwables.Iterator<? extends T, ? extends E> cur;
                // Remembers a positive hasNext() so that next() does not ask the source again: a source whose
                // hasNext() closes this wrapper could otherwise turn hasNext() == true into a NoSuchElementException.
                private boolean ready;
                private boolean closed;

                @Override
                public boolean hasNext() throws E {
                    if (ready) {
                        return true;
                    }

                    // A source's hasNext() may close this wrapper (the only early-exit hook this iterator has). As in
                    // defer, filter and map, a wrapper closed mid-traversal then just reports exhaustion, so `closed` is
                    // re-checked after every source callback instead of reading the fields that closeResourceInternal()
                    // has reset.
                    while (!closed) {
                        final Throwables.Iterator<? extends T, ? extends E> c = cur;

                        if (c != null) {
                            final boolean hasMore = c.hasNext();

                            if (closed) {
                                return false;
                            }

                            if (hasMore) {
                                ready = true;
                                return true;
                            }
                        }

                        if (cursor >= sources.size()) {
                            return false;
                        }

                        cur = sources.get(cursor++);
                    }

                    return false;
                }

                /**
                 * {@inheritDoc}
                 * @throws E if advancing the source iterator or evaluating an intermediate operation throws an exception
                 * @throws NoSuchElementException if this iterator is closed or has no remaining element
                 */
                @Override
                public T next() throws E, NoSuchElementException {
                    // hasNext() returns true only while open and with cur positioned on a source that has an element;
                    // closeResourceInternal() clears `ready`, so a close in between is reported as exhaustion.
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    ready = false;
                    return cur.next();
                }

                @Override
                protected void closeResourceInternal() {
                    closed = true;
                    ready = false;
                    Throwable failure = null;

                    for (final Throwables.Iterator<? extends T, ? extends E> source : sources) {
                        if (source != null) {
                            try {
                                source.closeResource();
                            } catch (final Throwable e) {
                                if (failure == null) {
                                    failure = e;
                                } else if (failure != e) {
                                    // Throwable rejects self-suppression. Separate resources can still
                                    // deliberately throw the same exception instance while closing.
                                    failure.addSuppressed(e);
                                }
                            }
                        }
                    }

                    cur = null;
                    sources.clear();

                    if (failure != null) {
                        throw ExceptionUtil.toRuntimeException(failure, true, true);
                    }
                }
            };
        }

        /**
         * Returns an iterator that reads lines from the specified Reader.
         * The iterator wraps the reader in a BufferedReader if it isn't one already.
         *
         * <p><b>Resource Management:</b> When {@code closeResource()} is called, it will close the underlying
         * BufferedReader (and thus the original Reader). Because this iterator does not implement
         * {@code AutoCloseable}, close it explicitly to ensure proper resource cleanup. Because this
         * iterator's {@code closeResource()} method does not declare checked exceptions, a reader close
         * failure is propagated as a runtime exception rather than silently discarded. Do <i>not</i> close it
         * in a bare {@code finally} block: a throw from that {@code finally} would replace the exception that
         * ended the iteration. Use the shape below instead — the same one {@code try}-with-resources compiles
         * to — which suppresses a close failure into the primary exception.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(new FileReader("file.txt"));
         * try {
         *     while (lines.hasNext()) {
         *         System.out.println(lines.next());
         *     }
         * } catch (Throwable primary) {
         *     try {
         *         lines.closeResource();
         *     } catch (Throwable closeFailure) {
         *         if (closeFailure != primary) {
         *             primary.addSuppressed(closeFailure);
         *         }
         *     }
         *
         *     throw primary;
         * }
         *
         * lines.closeResource();   // normal completion: a close failure is the only exception, so it stands
         * }</pre>
         *
         * @param reader the Reader to read lines from
         * @return an iterator over the lines in the reader, or an empty iterator if the reader is null
         */
        public static Throwables.Iterator<String, IOException> ofLines(final Reader reader) {
            if (reader == null) {
                return empty();
            }

            return new Throwables.Iterator<>() {
                private final BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
                private String cachedLine;
                /** A flag indicating if the iterator has been fully read. */
                private boolean finished = false;

                @Override
                public boolean hasNext() throws IOException {
                    if (cachedLine != null) {
                        return true;
                    } else if (finished) {
                        return false;
                    } else {
                        cachedLine = br.readLine();

                        if (cachedLine == null) {
                            finished = true;
                            return false;
                        } else {
                            return true;
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 * @throws IOException if reading the next line from the supplied reader fails
                 * @throws NoSuchElementException if this iterator has no remaining element
                 */
                @Override
                public String next() throws IOException, NoSuchElementException {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String res = cachedLine;
                    cachedLine = null;
                    return res;
                }

                @Override
                protected void closeResourceInternal() {
                    try {
                        br.close();
                    } catch (final IOException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    } finally {
                        cachedLine = null;
                        finished = true;
                    }
                }
            };
        }

        /**
         * Returns {@code true} if the iterator has more elements to iterate over.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<Integer, RuntimeException> iter = Throwables.Iterator.of(1, 2);
         * iter.hasNext(); // returns true
         * iter.next();
         * iter.next();
         * iter.hasNext();               // returns false
         *
         * Throwables.Iterator<String, RuntimeException> none = Throwables.Iterator.empty();
         * none.hasNext();   // returns false
         * }</pre>
         *
         * @return {@code true} if there are more elements, {@code false} otherwise
         * @throws E if the iterator implementation throws while determining whether another element is available
         */
        public abstract boolean hasNext() throws E;

        /**
         * Returns the next element in the iteration.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b");
         * iter.next();  // returns "a"
         * iter.next();  // returns "b"
         * iter.next();  // throws NoSuchElementException (no more elements)
         * }</pre>
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if there are no more elements
         * @throws E if the iterator implementation throws while retrieving the next element
         */
        public abstract T next() throws NoSuchElementException, E;

        /**
         * Advances the iterator by skipping the specified number of elements.
         * If {@code n} is greater than the number of remaining elements, the iterator will be
         * positioned at the end. If {@code n} is zero or negative, this method has no effect.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<Integer, RuntimeException> iter = Throwables.Iterator.of(1, 2, 3, 4, 5);
         * iter.advance(2);  // skips 1 and 2
         * iter.next();      // returns 3
         *
         * iter.advance(10);  // skips past the end (only 4, 5 remained)
         * iter.hasNext();    // returns false
         *
         * Throwables.Iterator<Integer, RuntimeException> unmoved = Throwables.Iterator.of(1, 2, 3);
         * unmoved.advance(0);  // no-op
         * unmoved.next();      // still returns 1
         * }</pre>
         *
         * @param n the number of elements to skip; no-op if zero or negative
         * @throws E if {@code n} is positive and checking for or retrieving an element to skip throws
         */
        public void advance(long n) throws E {
            if (n <= 0) {
                return;
            }

            while (n-- > 0 && hasNext()) {
                next();
            }
        }

        /**
         * Returns the count of remaining elements in the iterator.
         * This method will consume all remaining elements in the iterator.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<Integer, RuntimeException> iter = Throwables.Iterator.of(10, 20, 30);
         * iter.count(); // returns 3 (and consumes all elements)
         *
         * Throwables.Iterator<Integer, RuntimeException> iter2 = Throwables.Iterator.of(10, 20, 30);
         * iter2.next();   // consumes 10
         * iter2.count();  // returns 2 (counts only the remaining elements)
         * }</pre>
         *
         * @return the number of remaining elements
         * @throws E if checking for or retrieving a remaining element while counting throws
         */
        public long count() throws E {
            long result = 0;

            while (hasNext()) {
                next();
                result++;
            }

            return result;
        }

        /** Whether {@link #closeResource()} has already been invoked. */
        private boolean isClosed = false;

        /**
         * Closes this iterator and releases any resources associated with it.
         * If the iterator is already closed, this method has no effect.
         * This method calls closeResourceInternal() which can be overridden by subclasses
         * to perform specific cleanup operations.
         *
         * @throws RuntimeException if releasing an underlying resource fails with a checked exception
         */
        public final void closeResource() throws RuntimeException {
            if (isClosed) {
                return;
            }

            isClosed = true;
            closeResourceInternal();
        }

        /**
         * Releases any resources held by this iterator. Called by {@link #closeResource()} exactly once.
         * Subclasses should override this method to perform specific cleanup, such as closing
         * underlying streams or readers. The default implementation does nothing.
         */
        @Internal
        protected void closeResourceInternal() {

        }

        /**
         * Returns a new iterator that contains only elements matching the specified predicate.
         * Elements that do not satisfy the predicate will be skipped.
         * Closing the returned iterator closes this source iterator, releases its buffered element and
         * callback references, and leaves the returned iterator exhausted. This also holds when the predicate or
         * the source closes the returned iterator while it is searching for the next match: the search stops, the
         * element being tested is dropped (even if the predicate accepts it) and {@code hasNext()} returns
         * {@code false}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<Integer, RuntimeException> iter = Throwables.Iterator.of(1, 2, 3, 4, 5);
         * Throwables.Iterator<Integer, RuntimeException> even = iter.filter(x -> x % 2 == 0);
         * even.next();  // returns 2
         * even.next();  // returns 4
         * }</pre>
         *
         * @param predicate the predicate to test each element; it may throw any subtype of {@code E}
         * @return a new iterator containing only elements that satisfy the predicate; exceptions thrown
         *         by the predicate propagate from the returned iterator's {@code hasNext()}/{@code next()} calls
         * @throws IllegalArgumentException if {@code predicate} is {@code null}.
         */
        public Throwables.Iterator<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate, cs.predicate);

            return new FilteringIterator<>(this, predicate);
        }

        private static final class FilteringIterator<T, E extends Throwable> extends Throwables.Iterator<T, E> {
            private Throwables.Iterator<T, E> iter;
            private Throwables.Predicate<? super T, ? extends E> predicateRef;
            private T next;
            private boolean nextReady;
            private boolean closed;

            private FilteringIterator(final Throwables.Iterator<T, E> iter, final Throwables.Predicate<? super T, ? extends E> predicate) {
                this.iter = iter;
                predicateRef = predicate;
            }

            @Override
            public boolean hasNext() throws E {
                if (!nextReady && !closed) {
                    // The predicate or the source may close this wrapper re-entrantly (the only early-exit hook
                    // this iterator has); closeResourceInternal() then nulls the fields and resets nextReady. Work
                    // on locals and re-check `closed` after every callback, so a wrapper closed mid-traversal just
                    // reports exhaustion (defer, concat and map re-check their flag the same way) - never an NPE,
                    // and never hasNext() == true followed by a NoSuchElementException from next().
                    final Throwables.Iterator<T, E> src = iter;
                    final Throwables.Predicate<? super T, ? extends E> p = predicateRef;

                    while (src.hasNext() && !closed) {
                        final T candidate = src.next();

                        if (!closed && p.test(candidate) && !closed) {
                            next = candidate;
                            nextReady = true;
                            break;
                        }

                        if (closed) {
                            break;
                        }
                    }
                }

                return nextReady;
            }

            /**
             * {@inheritDoc}
             * @throws E if advancing the source iterator or evaluating an intermediate operation throws an exception
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public T next() throws E, NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = next;
                next = null;
                nextReady = false;
                return result;
            }

            @Override
            protected void closeResourceInternal() {
                closed = true;

                try {
                    iter.closeResource();
                } finally {
                    iter = null;
                    predicateRef = null;
                    next = null;
                    nextReady = false;
                }
            }
        }

        /**
         * Returns a new iterator that applies the specified mapping function to each element.
         * Closing the returned iterator closes this source iterator, releases its callback and source
         * references, and leaves the returned iterator exhausted. If the mapper closes the returned iterator, the
         * value it returns is still returned by that {@code next()} call; the iterator is exhausted afterwards. If this
         * source's {@code hasNext()} closes the returned iterator, that {@code hasNext()} call returns {@code false}
         * whatever the source reported, so {@code hasNext() == true} is never followed by a
         * {@link NoSuchElementException} from {@code next()}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("1", "2", "3");
         * Throwables.Iterator<Integer, RuntimeException> ints = iter.map(Integer::parseInt);
         * ints.next();  // returns 1
         * ints.next();  // returns 2
         * }</pre>
         *
         * @param <U> the type of elements returned by the new iterator
         * @param mapper the function to apply to each element; it may return any subtype of {@code U} and throw any
         *        subtype of {@code E}
         * @return a new iterator with the mapping function applied to each element; exceptions thrown
         *         by the mapper propagate from the returned iterator's {@code next()} calls
         * @throws IllegalArgumentException if {@code mapper} is {@code null}.
         */
        public <U> Throwables.Iterator<U, E> map(final Throwables.Function<? super T, ? extends U, ? extends E> mapper) throws IllegalArgumentException {
            N.checkArgNotNull(mapper, cs.mapper);

            return new MappingIterator<>(this, mapper);
        }

        private static final class MappingIterator<T, U, E extends Throwable> extends Throwables.Iterator<U, E> {
            private Throwables.Iterator<T, E> iter;
            private Throwables.Function<? super T, ? extends U, ? extends E> mapperRef;
            private boolean closed;

            private MappingIterator(final Throwables.Iterator<T, E> iter, final Throwables.Function<? super T, ? extends U, ? extends E> mapper) {
                this.iter = iter;
                mapperRef = mapper;
            }

            @Override
            public boolean hasNext() throws E {
                if (closed) {
                    return false;
                }

                // The source's hasNext() may close this wrapper re-entrantly (exactly as in filter/concat); re-check
                // the flag afterwards so hasNext() == true is never followed by a NoSuchElementException from next().
                final Throwables.Iterator<T, E> src = iter;
                final boolean hasNext = src.hasNext();

                return hasNext && !closed;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator is closed or has no remaining element
             * @throws E if advancing the source iterator or applying the mapper throws an exception
             */
            @Override
            public U next() throws NoSuchElementException, E {
                if (closed) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapperRef.apply(iter.next());
            }

            @Override
            protected void closeResourceInternal() {
                closed = true;

                try {
                    iter.closeResource();
                } finally {
                    iter = null;
                    mapperRef = null;
                }
            }
        }

        /**
         * Returns the first element from this iterator wrapped in a {@code Nullable}.
         * If the iterator is empty, returns an empty {@code Nullable}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
         * Nullable<String> first = iter.first(); // returns Nullable.of("a")
         * }</pre>
         *
         * @return a {@code Nullable} containing the first element if present, otherwise an empty Nullable
         * @throws E if checking for or retrieving the first remaining element throws
         */
        public Nullable<T> first() throws E {
            if (hasNext()) {
                return Nullable.of(next());
            } else {
                return Nullable.empty();
            }
        }

        /**
         * Returns the first {@code non-null} element from this iterator wrapped in an Optional.
         * If no {@code non-null} element is found or the iterator is empty, returns an empty Optional.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of(null, "b", "c");
         * u.Optional<String> first = iter.firstNonNull(); // returns Optional.of("b")
         * }</pre>
         *
         * @return an Optional containing the first {@code non-null} element if present, otherwise an empty Optional
         * @throws E if checking for or retrieving an element while searching for the first non-null value throws
         */
        public u.Optional<T> firstNonNull() throws E {
            T next = null;

            while (hasNext()) {
                next = next();

                if (next != null) {
                    return u.Optional.of(next);
                }
            }

            return u.Optional.empty();
        }

        /**
         * Returns the last element from this iterator wrapped in a {@code Nullable}.
         * This method will consume all elements in the iterator.
         * If the iterator is empty, returns an empty {@code Nullable}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
         * Nullable<String> last = iter.last(); // returns Nullable.of("c")
         * }</pre>
         *
         * @return a {@code Nullable} containing the last element if present, otherwise an empty Nullable
         * @throws E if checking for or retrieving a remaining element while finding the last value throws
         */
        public Nullable<T> last() throws E {
            if (hasNext()) {
                T next = next();

                while (hasNext()) {
                    next = next();
                }

                return Nullable.of(next);
            } else {
                return Nullable.empty();
            }
        }

        /**
         * Returns an array containing all remaining elements in this iterator.
         * This method will consume all remaining elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b");
         * Object[] arr = iter.toArray(); // returns ["a", "b"]
         * }</pre>
         *
         * @return an array containing all remaining elements
         * @throws E if checking for or retrieving a remaining element throws
         */
        public Object[] toArray() throws E {
            return toArray(N.EMPTY_OBJECT_ARRAY);
        }

        /**
         * Returns an array containing all remaining elements in this iterator.
         * If the specified array is large enough, the elements are stored in it; following the
         * {@link Collection#toArray(Object[])} contract, if it has room to spare, the element immediately after the
         * last stored element is set to {@code null} and the later elements are left untouched.
         * Otherwise, a new array of the same runtime component type is created.
         * This method will consume all remaining elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b");
         * String[] arr = iter.toArray(new String[0]); // returns ["a", "b"]
         *
         * String[] big = {"x", "x", "x", "x"};
         * Throwables.Iterator.of("a", "b").toArray(big);   // returns big, now ["a", "b", null, "x"]
         * }</pre>
         *
         * @param <A> the component type of the array
         * @param a the array into which the elements are to be stored, if it is big enough
         * @return an array containing all remaining elements
         * @throws IllegalArgumentException if {@code a} is {@code null}; validation occurs before this iterator is
         *         consumed.
         * @throws E if checking for or retrieving a remaining element throws
         * @throws ArrayStoreException if a remaining element cannot be stored in the runtime component type of {@code a}
         */
        public <A> A[] toArray(final A[] a) throws IllegalArgumentException, E, ArrayStoreException {
            N.checkArgNotNull(a, cs.a);

            return toList().toArray(a);
        }

        /**
         * Returns a List containing all remaining elements in this iterator.
         * This method will consume all remaining elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
         * List<String> list = iter.toList(); // returns ["a", "b", "c"]
         * }</pre>
         *
         * @return a List containing all remaining elements
         * @throws E if checking for or retrieving a remaining element throws
         */
        public List<T> toList() throws E {
            final List<T> list = new ArrayList<>();

            while (hasNext()) {
                list.add(next());
            }

            return list;
        }

        /**
         * Performs the given action for each remaining element in this iterator.
         * This method will consume all remaining elements.
         *
         * <p><b>Naming note:</b> this is the JDK-cased {@code forEachRemaining} that takes an unchecked
         * {@code java.util.function.Consumer}. Its case-twin {@link #foreachRemaining(Throwables.Consumer)}
         * (all-lowercase {@code foreach}) takes a checked {@link Throwables.Consumer} instead; the two
         * differ only by the capital {@code E}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
         * iter.forEachRemaining(System.out::println); // prints a b c
         * }</pre>
         *
         * @param action the action to be performed for each element
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         * @throws E if checking for or retrieving a remaining element throws
         * @see #foreachRemaining(Throwables.Consumer)
         */
        public void forEachRemaining(final java.util.function.Consumer<? super T> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action, cs.action); // NOSONAR

            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         * Performs the given action for each remaining element in this iterator.
         * This method will consume all remaining elements.
         *
         * <p><b>Naming note:</b> the all-lowercase {@code foreach} spelling marks the library variant
         * that accepts a checked {@link Throwables.Consumer}. It differs only by one letter's case from
         * the JDK-cased {@link #forEachRemaining(java.util.function.Consumer)}, which takes an unchecked
         * {@code java.util.function.Consumer}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> collected = new ArrayList<>();
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("x", "y", "z");
         * iter.foreachRemaining(collected::add); // collected becomes [x, y, z]
         *
         * List<String> rest = new ArrayList<>();
         * Throwables.Iterator<String, RuntimeException> iter2 = Throwables.Iterator.of("x", "y", "z");
         * iter2.next();                       // consumes "x"
         * iter2.foreachRemaining(rest::add);  // rest becomes [y, z]
         * }</pre>
         *
         * @param <E2> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         * @throws E if checking for or retrieving a remaining element throws
         * @throws E2 if the action throws an exception
         * @see #forEachRemaining(java.util.function.Consumer)
         * @see #foreachIndexed(Throwables.IntObjConsumer)
         */
        public <E2 extends Throwable> void foreachRemaining(final Throwables.Consumer<? super T, E2> action) throws IllegalArgumentException, E, E2 {
            N.checkArgNotNull(action, cs.action); // NOSONAR

            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         * Performs the given action for each remaining element in this iterator,
         * providing both the element and its index (starting from 0).
         * This method will consume all remaining elements.
         *
         * <p><b>Naming note:</b> like {@link #foreachRemaining(Throwables.Consumer)}, the all-lowercase
         * {@code foreach} spelling marks a library variant that accepts a checked action; there is no
         * JDK-cased {@code forEachIndexed} counterpart.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> collected = new ArrayList<>();
         * Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("x", "y", "z");
         * // action receives (index, element): the int index comes first
         * iter.foreachIndexed((idx, val) -> collected.add(idx + "=" + val)); // collected becomes [0=x, 1=y, 2=z]
         * }</pre>
         *
         * @param <E2> the type of exception that the action may throw
         * @param action the action to be performed for each element with its index
         * @throws IllegalArgumentException if {@code action} is {@code null}.
         * @throws E if checking for or retrieving the next element fails
         * @throws IllegalStateException if the iterator holds more elements than an {@code int} index can address,
         *         that is more than {@code Integer.MAX_VALUE + 1} of them (indices {@code 0 .. Integer.MAX_VALUE}
         *         are all delivered first). {@link Seq#forEachIndexed(Throwables.IntObjConsumer)} enforces the same
         *         limit but reports it as an {@link ArithmeticException}
         * @throws E2 if the action throws an exception
         * @see #foreachRemaining(Throwables.Consumer)
         * @see Seq#forEachIndexed(Throwables.IntObjConsumer)
         */
        public <E2 extends Throwable> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E2> action)
                throws IllegalArgumentException, E, IllegalStateException, E2 {
            N.checkArgNotNull(action, cs.action);

            int idx = 0;

            while (hasNext()) {
                if (idx < 0) {
                    throw new IllegalStateException("Index overflow: iterator has more elements than an int index can address");
                }
                action.accept(idx++, next());
            }
        }
    }

    /**
     * Represents an operation that takes no arguments and returns no result, and that may throw
     * a checked exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.lang.Runnable}.
     *
     * @param <E> the type of exception that may be thrown
     * @see java.lang.Runnable
     */
    @FunctionalInterface
    public interface Runnable<E extends Throwable> {

        /**
         * Executes this runnable operation.
         *
         * @throws E if the task implementation throws while executing its action
         */
        void run() throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.Runnable} (a {@code java.lang.Runnable}) that wraps this Throwables.Runnable.
         * Any checked exception - and any {@link Error} - thrown by this runnable is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Runnable} that executes this runnable and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Runnable unchecked() {
            return () -> {
                try {
                    run();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a task that returns a result and may throw a checked exception of type {@code E}.
     * This is the exception-typed equivalent of {@link java.util.concurrent.Callable}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     * @see java.util.concurrent.Callable
     */
    @FunctionalInterface
    public interface Callable<R, E extends Throwable> {

        /**
         * Computes a result.
         *
         * @return the computed result
         * @throws E if the callable implementation throws while computing its result
         */
        R call() throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.Callable} (a {@code java.util.concurrent.Callable}) that wraps this Throwables.Callable.
         * Any checked exception - and any {@link Error} - thrown by this callable is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Callable} that executes this callable and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Callable<R> unchecked() {
            return () -> {
                try {
                    return call();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a supplier of results that may throw a checked exception of type {@code E}.
     * This is the exception-throwing equivalent of {@link java.util.function.Supplier}.
     *
     * @param <T> the type of the value supplied
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.Supplier
     */
    @FunctionalInterface
    public interface Supplier<T, E extends Throwable> {

        /**
         * Gets a result.
         *
         * @return the supplied value
         * @throws E if the supplier implementation throws while producing the requested value
         */
        T get() throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.Supplier} (a {@code java.util.function.Supplier}) that wraps this Throwables.Supplier.
         * Any checked exception - and any {@link Error} - thrown by this supplier is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Supplier} that executes this supplier and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Supplier<T> unchecked() {
            return () -> {
                try {
                    return get();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a supplier of {@code boolean}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code boolean}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface BooleanSupplier<E extends Throwable> {

        /**
         * Returns a boolean result from this supplier.
         * Implementations may throw a checked exception while computing the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.BooleanSupplier<Exception> supplier = () -> true;
         * boolean result = supplier.getAsBoolean();
         * }</pre>
         *
         * @return the supplied boolean result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        boolean getAsBoolean() throws E; // NOSONAR
    }

    /**
     * Represents a supplier of {@code char}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code char}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface CharSupplier<E extends Throwable> {

        /**
         * Gets a char result.
         *
         * @return the char result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        char getAsChar() throws E;
    }

    /**
     * Represents a supplier of {@code byte}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code byte}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ByteSupplier<E extends Throwable> {

        /**
         * Gets a byte result.
         *
         * @return the byte result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        byte getAsByte() throws E;
    }

    /**
     * Represents a supplier of {@code short}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code short}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ShortSupplier<E extends Throwable> {

        /**
         * Gets a short result.
         *
         * @return the short result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        short getAsShort() throws E;
    }

    /**
     * Represents a supplier of {@code int}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code int}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntSupplier<E extends Throwable> {

        /**
         * Gets an int result.
         *
         * @return the int result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        int getAsInt() throws E;
    }

    /**
     * Represents a supplier of {@code long}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code long}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongSupplier<E extends Throwable> {

        /**
         * Gets a long result.
         *
         * @return the long result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        long getAsLong() throws E;
    }

    /**
     * Represents a supplier of {@code float}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code float}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatSupplier<E extends Throwable> {

        /**
         * Gets a float result.
         *
         * @return the float result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        float getAsFloat() throws E;
    }

    /**
     * Represents a supplier of {@code double}-valued results that may throw a checked exception
     * of type {@code E}. This is the {@code double}-producing primitive specialization of
     * {@link Supplier}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface DoubleSupplier<E extends Throwable> {

        /**
         * Gets a double result.
         *
         * @return the double result
         * @throws E if the supplier implementation throws while producing the requested value
         */
        double getAsDouble() throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one argument that may throw a checked
     * exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.Predicate}.
     *
     * @param <T> the type of the input to the predicate
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.Predicate
     */
    @FunctionalInterface
    public interface Predicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given argument.
         *
         * @param t the input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}
         */
        boolean test(T t) throws E;

        /**
         * Returns a predicate that represents the logical negation of this predicate.
         *
         * @return a predicate that represents the logical negation of this predicate
         */
        default Predicate<T, E> negate() {
            return t -> !test(t);
        }

        /**
         * Returns a {@code com.landawn.abacus.util.function.Predicate} (a {@code java.util.function.Predicate}) that wraps this Throwables.Predicate.
         * Any checked exception - and any {@link Error} - thrown by this predicate is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Predicate} that executes this predicate and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Predicate<T> unchecked() {
            return t -> {
                try {
                    return test(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a predicate (boolean-valued function) of two arguments that may throw a checked
     * exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.BiPredicate}.
     *
     * @param <T> the type of the first input
     * @param <U> the type of the second input
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.BiPredicate
     */
    @FunctionalInterface
    public interface BiPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(T t, U u) throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.BiPredicate} (a {@code java.util.function.BiPredicate}) that wraps this Throwables.BiPredicate.
         * Any checked exception - and any {@link Error} - thrown by this predicate is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.BiPredicate} that executes this predicate and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.BiPredicate<T, U> unchecked() {
            return (t, u) -> {
                try {
                    return test(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a predicate (boolean-valued function) of three arguments that may throw a checked
     * exception of type {@code E}. This is the three-arity specialization of {@link Predicate}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface TriPredicate<A, B, C, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(A a, B b, C c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of four arguments that may throw a checked
     * exception of type {@code E}. This is the four-arity specialization of {@link Predicate}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <D> the type of the fourth input
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface QuadPredicate<A, B, C, D, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @param d the fourth input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}, {@code d}
         */
        boolean test(A a, B b, C c, D d) throws E;
    }

    /**
     * Represents a function that accepts one argument and produces a result, and that may throw
     * a checked exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.Function}.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.Function
     */
    @FunctionalInterface
    public interface Function<T, R, E extends Throwable> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        R apply(T t) throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.Function} (a {@code java.util.function.Function}) that wraps this Throwables.Function.
         * Any checked exception - and any {@link Error} - thrown by this function is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Function} that executes this function and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Function<T, R> unchecked() {
            return t -> {
                try {
                    return apply(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a function that accepts two arguments and produces a result, and that may throw
     * a checked exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.BiFunction}.
     *
     * @param <T> the type of the first input
     * @param <U> the type of the second input
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.BiFunction
     */
    @FunctionalInterface
    public interface BiFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(T t, U u) throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.BiFunction} (a {@code java.util.function.BiFunction}) that wraps this Throwables.BiFunction.
         * Any checked exception - and any {@link Error} - thrown by this function is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.BiFunction} that executes this function and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.BiFunction<T, U, R> unchecked() {
            return (t, u) -> {
                try {
                    return apply(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents a function that accepts three arguments and produces a result, and that may throw
     * a checked exception of type {@code E}. This is the three-arity specialization of
     * {@link Function}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(A a, B b, C c) throws E;
    }

    /**
     * Represents a function that accepts four arguments and produces a result, and that may throw
     * a checked exception of type {@code E}. This is the four-arity specialization of
     * {@link Function}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <D> the type of the fourth input
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface QuadFunction<A, B, C, D, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @param d the fourth function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}, {@code d}
         */
        R apply(A a, B b, C c, D d) throws E;
    }

    /**
     * Represents an operation that accepts a single input argument and returns no result, and that
     * may throw a checked exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.Consumer}.
     *
     * @param <T> the type of the input to the consumer
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.Consumer
     */
    @FunctionalInterface
    public interface Consumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @throws E if the consumer implementation throws while processing {@code t}
         */
        void accept(T t) throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.Consumer} (a {@code java.util.function.Consumer}) that wraps this Throwables.Consumer.
         * Any checked exception - and any {@link Error} - thrown by this consumer is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.Consumer} that executes this consumer and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.Consumer<T> unchecked() {
            return t -> {
                try {
                    accept(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents an operation that accepts two input arguments and returns no result, and that
     * may throw a checked exception of type {@code E}. This is the exception-throwing equivalent of
     * {@link java.util.function.BiConsumer}.
     *
     * @param <T> the type of the first input
     * @param <U> the type of the second input
     * @param <E> the type of exception that may be thrown
     * @see java.util.function.BiConsumer
     */
    @FunctionalInterface
    public interface BiConsumer<T, U, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(T t, U u) throws E;

        /**
         * Returns a {@code com.landawn.abacus.util.function.BiConsumer} (a {@code java.util.function.BiConsumer}) that wraps this Throwables.BiConsumer.
         * Any checked exception - and any {@link Error} - thrown by this consumer is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * @return a {@code com.landawn.abacus.util.function.BiConsumer} that executes this consumer and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        default com.landawn.abacus.util.function.BiConsumer<T, U> unchecked() {
            return (t, u) -> {
                try {
                    accept(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents an operation that accepts three input arguments and returns no result, and that
     * may throw a checked exception of type {@code E}. This is the three-arity specialization of
     * {@link Consumer}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(A a, B b, C c) throws E;
    }

    /**
     * Represents an operation that accepts four input arguments and returns no result, and that
     * may throw a checked exception of type {@code E}. This is the four-arity specialization of
     * {@link Consumer}.
     *
     * @param <A> the type of the first input
     * @param <B> the type of the second input
     * @param <C> the type of the third input
     * @param <D> the type of the fourth input
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @param d the fourth input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}, {@code d}
         */
        void accept(A a, B b, C c, D d) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code boolean}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code boolean}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface BooleanConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given boolean argument.
         *
         * @param value the boolean input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(boolean value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code boolean}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code boolean} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface BooleanPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given boolean argument.
         *
         * @param value the boolean input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(boolean value) throws E;
    }

    /**
     * Represents a function that accepts a {@code boolean}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code boolean}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface BooleanFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given boolean argument.
         *
         * @param value the boolean function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(boolean value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code char}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code char}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface CharConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given char argument.
         *
         * @param value the char input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(char value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code char}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code char} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface CharPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given char argument.
         *
         * @param value the char input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(char value) throws E;
    }

    /**
     * Represents a function that accepts a {@code char}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code char}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface CharFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given char argument.
         *
         * @param value the char function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(char value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code byte}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code byte}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ByteConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given byte argument.
         *
         * @param value the byte input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(byte value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code byte}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code byte} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface BytePredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given byte argument.
         *
         * @param value the byte input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(byte value) throws E;
    }

    /**
     * Represents a function that accepts a {@code byte}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code byte}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ByteFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given byte argument.
         *
         * @param value the byte function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(byte value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code short}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code short}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ShortConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given short argument.
         *
         * @param value the short input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(short value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code short}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code short} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ShortPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given short argument.
         *
         * @param value the short input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(short value) throws E;
    }

    /**
     * Represents a function that accepts a {@code short}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code short}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ShortFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given short argument.
         *
         * @param value the short function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(short value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code int}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code int}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given int argument.
         *
         * @param value the int input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(int value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code int}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code int} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given int argument.
         *
         * @param value the int input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(int value) throws E;
    }

    /**
     * Represents a function that accepts an {@code int}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code int}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given int argument.
         *
         * @param value the int function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(int value) throws E;
    }

    /**
     * Represents a function that accepts an {@code int}-valued argument and produces a
     * {@code long}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntToLongFunction<E extends Throwable> {

        /**
         * Applies this function to the given int argument and produces a long result.
         *
         * @param value the int function argument
         * @return the long function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        long applyAsLong(int value) throws E;
    }

    /**
     * Represents a function that accepts an {@code int}-valued argument and produces a
     * {@code double}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface IntToDoubleFunction<E extends Throwable> {

        /**
         * Applies this function to the given int argument and produces a double result.
         *
         * @param value the int function argument
         * @return the double function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        double applyAsDouble(int value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code long}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code long}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given long argument.
         *
         * @param value the long input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(long value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code long}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code long} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given long argument.
         *
         * @param value the long input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(long value) throws E;
    }

    /**
     * Represents a function that accepts a {@code long}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code long}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given long argument.
         *
         * @param value the long function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(long value) throws E;
    }

    /**
     * Represents a function that accepts a {@code long}-valued argument and produces an
     * {@code int}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongToIntFunction<E extends Throwable> {

        /**
         * Applies this function to the given long argument and produces an int result.
         *
         * @param value the long function argument
         * @return the int function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        int applyAsInt(long value) throws E;
    }

    /**
     * Represents a function that accepts a {@code long}-valued argument and produces a
     * {@code double}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface LongToDoubleFunction<E extends Throwable> {

        /**
         * Applies this function to the given long argument and produces a double result.
         *
         * @param value the long function argument
         * @return the double function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        double applyAsDouble(long value) throws E;
    }

    /**
     * Represents a function that accepts a {@code float}-valued argument and produces an
     * {@code int}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatToIntFunction<E extends Throwable> {
        /**
         * Applies this function to the given float argument and produces an int result.
         *
         * @param value the float function argument
         * @return the int function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        int applyAsInt(float value) throws E;
    }

    /**
     * Represents a function that accepts a {@code float}-valued argument and produces a
     * {@code long}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatToLongFunction<E extends Throwable> {
        /**
         * Applies this function to the given float argument and produces a long result.
         *
         * @param value the float function argument
         * @return the long function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        long applyAsLong(float value) throws E;
    }

    /**
     * Represents a function that accepts a {@code float}-valued argument and produces a
     * {@code double}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatToDoubleFunction<E extends Throwable> {
        /**
         * Applies this function to the given float argument and produces a double result.
         *
         * @param value the float function argument
         * @return the double function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        double applyAsDouble(float value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code float}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code float}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given float argument.
         *
         * @param value the float input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(float value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code float}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code float} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given float argument.
         *
         * @param value the float input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(float value) throws E;
    }

    /**
     * Represents a function that accepts a {@code float}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code float}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface FloatFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given float argument.
         *
         * @param value the float function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(float value) throws E;
    }

    /**
     * Represents an operation that accepts a single {@code double}-valued argument and returns no
     * result, and that may throw a checked exception of type {@code E}. This is the {@code double}
     * primitive specialization of {@link Consumer}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface DoubleConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given double argument.
         *
         * @param value the double input argument
         * @throws E if the consumer implementation throws while processing {@code value}
         */
        void accept(double value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of one {@code double}-valued argument that
     * may throw a checked exception of type {@code E}. This is the {@code double} primitive
     * specialization of {@link Predicate}.
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface DoublePredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given double argument.
         *
         * @param value the double input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}
         */
        boolean test(double value) throws E;
    }

    /**
     * Represents a function that accepts a {@code double}-valued argument and produces a result,
     * and that may throw a checked exception of type {@code E}. This is the {@code double}
     * primitive specialization of {@link Function}.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given double argument.
         *
         * @param value the double function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        R apply(double value) throws E;
    }

    /**
     * Represents a function that accepts a {@code double}-valued argument and produces an
     * {@code int}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleToIntFunction<E extends Throwable> {

        /**
         * Applies this function to the given double argument and produces an int result.
         *
         * @param value the double function argument
         * @return the int function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        int applyAsInt(double value) throws E;
    }

    /**
     * Represents a function that accepts a {@code double}-valued argument and produces a
     * {@code long}-valued result, and that may throw a checked exception of type {@code E}.
     *
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleToLongFunction<E extends Throwable> {

        /**
         * Applies this function to the given double argument and produces a long result.
         *
         * @param value the double function argument
         * @return the long function result
         * @throws E if the function implementation throws while computing the result for {@code value}
         */
        long applyAsLong(double value) throws E;
    }

    /**
     * Represents a function that produces a boolean-valued result.
     * This is the boolean-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToBooleanFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a boolean result.
         *
         * @param t the input argument
         * @return the boolean result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        boolean applyAsBoolean(T t) throws E;
    }

    /**
     * Represents a function that produces a char-valued result.
     * This is the char-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToCharFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a char result.
         *
         * @param t the input argument
         * @return the char result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        char applyAsChar(T t) throws E;
    }

    /**
     * Represents a function that produces a byte-valued result.
     * This is the byte-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToByteFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a byte result.
         *
         * @param t the input argument
         * @return the byte result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        byte applyAsByte(T t) throws E;
    }

    /**
     * Represents a function that produces a short-valued result.
     * This is the short-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToShortFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a short result.
         *
         * @param t the input argument
         * @return the short result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        short applyAsShort(T t) throws E;
    }

    /**
     * Represents a function that produces an int-valued result.
     * This is the int-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns an int result.
         *
         * @param t the input argument
         * @return the int result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        int applyAsInt(T t) throws E;
    }

    /**
     * Represents a function that produces a long-valued result.
     * This is the long-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a long result.
         *
         * @param t the input argument
         * @return the long result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        long applyAsLong(T t) throws E;
    }

    /**
     * Represents a function that produces a float-valued result.
     * This is the float-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToFloatFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a float result.
         *
         * @param t the input argument
         * @return the float result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        float applyAsFloat(T t) throws E;
    }

    /**
     * Represents a function that produces a double-valued result.
     * This is the double-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a double result.
         *
         * @param t the input argument
         * @return the double result
         * @throws E if the function implementation throws while computing the result for {@code t}
         */
        double applyAsDouble(T t) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a boolean-valued result.
     * This is the boolean-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToBooleanBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a boolean result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the boolean result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        boolean applyAsBoolean(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a byte-valued result.
     * This is the byte-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToByteBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a byte result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the byte result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        byte applyAsByte(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a char-valued result.
     * This is the char-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToCharBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a char result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the char result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        char applyAsChar(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a float-valued result.
     * This is the float-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToFloatBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a float result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the float result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        float applyAsFloat(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a short-valued result.
     * This is the short-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToShortBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a short result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the short result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        short applyAsShort(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces an int-valued result.
     * This is the int-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns an int result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the int result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        int applyAsInt(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a long-valued result.
     * This is the long-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a long result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the long result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        long applyAsLong(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a double-valued result.
     * This is the double-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a double result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the double result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}
         */
        double applyAsDouble(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces an int-valued result.
     * This is the int-producing primitive specialization for {@code TriFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns an int result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the int result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        int applyAsInt(A a, B b, C c) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces a long-valued result.
     * This is the long-producing primitive specialization for {@code TriFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a long result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the long result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        long applyAsLong(A a, B b, C c) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces a double-valued result.
     * This is the double-producing primitive specialization for {@code TriFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a double result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the double result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        double applyAsDouble(A a, B b, C c) throws E;
    }

    /**
     * Represents an operation on a single operand that produces a result of the same type as its operand.
     * This is a specialization of {@code Function} for the case where the operand and result are of the same type.
     *
     * @param <T> the type of the operand and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface UnaryOperator<T, E extends Throwable> extends Function<T, T, E> {

        /**
         * Returns a {@code com.landawn.abacus.util.function.UnaryOperator} (a {@code java.util.function.UnaryOperator})
         * that wraps this Throwables.UnaryOperator, so the adapter can be passed where an operator is required, such as
         * {@link java.util.List#replaceAll(java.util.function.UnaryOperator)} or
         * {@link java.util.stream.Stream#iterate(Object, java.util.function.UnaryOperator)}.
         * Any checked exception - and any {@link Error} - thrown by this operator is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.UnaryOperator<String, IOException> canonical = s -> new File(s).getCanonicalPath();
         * paths.replaceAll(canonical.unchecked());
         * }</pre>
         *
         * @return a {@code com.landawn.abacus.util.function.UnaryOperator} that executes this operator and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        @Override
        default com.landawn.abacus.util.function.UnaryOperator<T> unchecked() {
            // Covariant override of Function.unchecked(): the inherited Function-typed adapter is not an operator, so
            // List.replaceAll / Stream.iterate rejected it.
            return t -> {
                try {
                    return apply(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents an operation upon two operands of the same type, producing a result of the same type as the operands.
     * This is a specialization of {@code BiFunction} for the case where the operands and the result are all of the same type.
     *
     * @param <T> the type of the operands and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BinaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {

        /**
         * Returns a {@code com.landawn.abacus.util.function.BinaryOperator} (a {@code java.util.function.BinaryOperator})
         * that wraps this Throwables.BinaryOperator, so the adapter can be passed where an operator is required, such as
         * {@link java.util.stream.Stream#reduce(java.util.function.BinaryOperator)}.
         * Any checked exception - and any {@link Error} - thrown by this operator is converted to a RuntimeException;
         * a RuntimeException is normally rethrown as the same instance. See the class-level &quot;Unchecked adapter behavior&quot; note.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Throwables.BinaryOperator<BigDecimal, IOException> add = (a, b) -> audit(a.add(b));
         * Optional<BigDecimal> total = amounts.stream().reduce(add.unchecked());
         * }</pre>
         *
         * @return a {@code com.landawn.abacus.util.function.BinaryOperator} that executes this operator and converts any thrown exception or error to a RuntimeException
         */
        @Beta
        @Override
        default com.landawn.abacus.util.function.BinaryOperator<T> unchecked() {
            // Covariant override of BiFunction.unchecked(): the inherited BiFunction-typed adapter is not an operator,
            // so Stream.reduce rejected it.
            return (t, u) -> {
                try {
                    return apply(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * Represents an operation upon three operands of the same type, producing a result of the same type as the operands.
     * This is a specialization of {@code TriFunction} for the case where the operands and the result are all of the same type.
     *
     * @param <T> the type of the operands and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface TernaryOperator<T, E extends Throwable> extends TriFunction<T, T, T, T, E> {
    }

    /**
     * Represents an operation on a single boolean-valued operand that produces a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        boolean applyAsBoolean(boolean operand) throws E;
    }

    /**
     * Represents an operation on a single char-valued operand that produces a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        char applyAsChar(char operand) throws E;
    }

    /**
     * Represents an operation on a single byte-valued operand that produces a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        byte applyAsByte(byte operand) throws E;
    }

    /**
     * Represents an operation on a single short-valued operand that produces a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        short applyAsShort(short operand) throws E;
    }

    /**
     * Represents an operation on a single int-valued operand that produces an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        int applyAsInt(int operand) throws E;
    }

    /**
     * Represents an operation that accepts an int-valued operand and an object operand, and produces an int-valued result.
     *
     * @param <T> the type of the object operand
     * @param <E> the type of exception that the operator may throw
     */
    @Beta
    @FunctionalInterface
    public interface IntObjOperator<T, E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param operand the int operand
         * @param obj the object operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}, {@code obj}
         */
        int applyAsInt(int operand, T obj) throws E;
    }

    /**
     * Represents an operation on a single long-valued operand that produces a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        long applyAsLong(long operand) throws E;
    }

    /**
     * Represents an operation on a single float-valued operand that produces a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        float applyAsFloat(float operand) throws E;
    }

    /**
     * Represents an operation on a single double-valued operand that produces a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the input operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code operand}
         */
        double applyAsDouble(double operand) throws E;
    }

    /**
     * Represents an operation upon two boolean-valued operands and producing a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    /**
     * Represents an operation upon two char-valued operands and producing a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        char applyAsChar(char left, char right) throws E;
    }

    /**
     * Represents an operation upon two byte-valued operands and producing a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        byte applyAsByte(byte left, byte right) throws E;
    }

    /**
     * Represents an operation upon two short-valued operands and producing a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        short applyAsShort(short left, short right) throws E;
    }

    /**
     * Represents an operation upon two int-valued operands and producing an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        int applyAsInt(int left, int right) throws E;
    }

    /**
     * Represents an operation upon two long-valued operands and producing a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        long applyAsLong(long left, long right) throws E;
    }

    /**
     * Represents an operation upon two float-valued operands and producing a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        float applyAsFloat(float left, float right) throws E;
    }

    /**
     * Represents an operation upon two double-valued operands and producing a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code left}, {@code right}
         */
        double applyAsDouble(double left, double right) throws E;
    }

    /**
     * Represents an operation upon three boolean-valued operands and producing a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents an operation upon three char-valued operands and producing a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        char applyAsChar(char a, char b, char c) throws E;
    }

    /**
     * Represents an operation upon three byte-valued operands and producing a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        byte applyAsByte(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents an operation upon three short-valued operands and producing a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        short applyAsShort(short a, short b, short c) throws E;
    }

    /**
     * Represents an operation upon three int-valued operands and producing an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        int applyAsInt(int a, int b, int c) throws E;
    }

    /**
     * Represents an operation upon three long-valued operands and producing a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        long applyAsLong(long a, long b, long c) throws E;
    }

    /**
     * Represents an operation upon three float-valued operands and producing a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        float applyAsFloat(float a, float b, float c) throws E;
    }

    /**
     * Represents an operation upon three double-valued operands and producing a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the result of applying this operator
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        double applyAsDouble(double a, double b, double c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two boolean-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BooleanBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(boolean t, boolean u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two char-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface CharBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(char t, char u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two byte-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ByteBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(byte t, byte u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two short-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ShortBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(short t, short u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two int-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(int t, int u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two long-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(long t, long u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two float-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface FloatBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(float t, float u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two double-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}
         */
        boolean test(double t, double u) throws E;
    }

    /**
     * Represents a function that accepts two boolean-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(boolean t, boolean u) throws E;
    }

    /**
     * Represents a function that accepts two char-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(char t, char u) throws E;
    }

    /**
     * Represents a function that accepts two byte-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(byte t, byte u) throws E;
    }

    /**
     * Represents a function that accepts two short-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(short t, short u) throws E;
    }

    /**
     * Represents a function that accepts two int-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(int t, int u) throws E;
    }

    /**
     * Represents a function that accepts two long-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(long t, long u) throws E;
    }

    /**
     * Represents a function that accepts two float-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(float t, float u) throws E;
    }

    /**
     * Represents a function that accepts two double-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}
         */
        R apply(double t, double u) throws E;
    }

    /**
     * Represents an operation that accepts two boolean-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BooleanBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(boolean t, boolean u) throws E;
    }

    /**
     * Represents an operation that accepts two char-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface CharBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(char t, char u) throws E;
    }

    /**
     * Represents an operation that accepts two byte-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ByteBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(byte t, byte u) throws E;
    }

    /**
     * Represents an operation that accepts two short-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ShortBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(short t, short u) throws E;
    }

    /**
     * Represents an operation that accepts two int-valued arguments and returns no result.
     * This is the {@code int} primitive specialization of {@code BiConsumer}.
     *
     * <p>Extends {@link IntIntConsumer} (whose two {@code int} parameters are conventionally an
     * index and a value) since the two interfaces share the same {@code accept(int, int)} shape,
     * allowing an {@code IntBiConsumer} to be used wherever an {@code IntIntConsumer} is expected.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBiConsumer<E extends Throwable> extends IntIntConsumer<E> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        @Override
        void accept(int t, int u) throws E;
    }

    /**
     * Represents an operation that accepts two long-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(long t, long u) throws E;
    }

    /**
     * Represents an operation that accepts two float-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface FloatBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(float t, float u) throws E;
    }

    /**
     * Represents an operation that accepts two double-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}
         */
        void accept(double t, double u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three boolean-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BooleanTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three char-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface CharTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(char a, char b, char c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three byte-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ByteTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three short-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ShortTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(short a, short b, short c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three int-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(int a, int b, int c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three long-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(long a, long b, long c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three float-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface FloatTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(float a, float b, float c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three double-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code a}, {@code b}, {@code c}
         */
        boolean test(double a, double b, double c) throws E;
    }

    /**
     * Represents a function that accepts three boolean-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents a function that accepts three char-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(char a, char b, char c) throws E;
    }

    /**
     * Represents a function that accepts three byte-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents a function that accepts three short-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(short a, short b, short c) throws E;
    }

    /**
     * Represents a function that accepts three int-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(int a, int b, int c) throws E;
    }

    /**
     * Represents a function that accepts three long-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(long a, long b, long c) throws E;
    }

    /**
     * Represents a function that accepts three float-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(float a, float b, float c) throws E;
    }

    /**
     * Represents a function that accepts three double-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code a}, {@code b}, {@code c}
         */
        R apply(double a, double b, double c) throws E;
    }

    /**
     * Represents an operation that accepts three boolean-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BooleanTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents an operation that accepts three char-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface CharTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(char a, char b, char c) throws E;
    }

    /**
     * Represents an operation that accepts three byte-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ByteTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents an operation that accepts three short-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ShortTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(short a, short b, short c) throws E;
    }

    /**
     * Represents an operation that accepts three int-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(int a, int b, int c) throws E;
    }

    /**
     * Represents an operation that accepts three long-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(long a, long b, long c) throws E;
    }

    /**
     * Represents an operation that accepts three float-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface FloatTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(float a, float b, float c) throws E;
    }

    /**
     * Represents an operation that accepts three double-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if the consumer implementation throws while processing {@code a}, {@code b}, {@code c}
         */
        void accept(double a, double b, double c) throws E;
    }

    /**
     * Represents an operation that accepts an object and a boolean value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjBooleanConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the boolean input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, boolean value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a char value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjCharConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the char input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, char value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a byte value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjByteConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the byte input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, byte value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a short value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjShortConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the short input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, short value) throws E;
    }

    /**
     * Represents an operation that accepts an object and an int value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjIntConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the int input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, int value) throws E;
    }

    /**
     * Represents a function that accepts an object and an int value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjIntFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param value the int function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code value}
         */
        R apply(T t, int value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and an int value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjIntPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param value the int input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code value}
         */
        boolean test(T t, int value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a long value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjLongConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the long input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, long value) throws E;
    }

    /**
     * Represents a function that accepts an object and a long value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjLongFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param value the long function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code value}
         */
        R apply(T t, long value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and a long value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjLongPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param value the long input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code value}
         */
        boolean test(T t, long value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a float value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjFloatConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the float input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, float value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a double value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjDoubleConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the double input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code value}
         */
        void accept(T t, double value) throws E;
    }

    /**
     * Represents a function that accepts an object and a double value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjDoubleFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param value the double function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code value}
         */
        R apply(T t, double value) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and a double value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjDoublePredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param value the double input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code value}
         */
        boolean test(T t, double value) throws E;
    }

    /**
     * Represents an operation that accepts an object and two int values and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjBiIntConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param i the first int input argument
         * @param j the second int input argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code i}, {@code j}
         */
        void accept(T t, int i, int j) throws E;
    }

    /**
     * Represents a function that accepts an object and two int values and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjBiIntFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param i the first int function argument
         * @param j the second int function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code i}, {@code j}
         */
        R apply(T t, int i, int j) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and two int values.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjBiIntPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param i the first int input argument
         * @param j the second int input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code i}, {@code j}
         */
        boolean test(T t, int i, int j) throws E;
    }

    /**
     * Represents an operation that accepts two objects and an int value and returns no result.
     * This is the indexed variant of BiConsumer.
     *
     * @param <T> the type of the first object argument to the operation
     * @param <U> the type of the second object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BiObjIntConsumer<T, U, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first object input argument
         * @param u the second object input argument
         * @param i the int index argument
         * @throws E if the consumer implementation throws while processing {@code t}, {@code u}, {@code i}
         */
        void accept(T t, U u, int i) throws E;
    }

    /**
     * Represents a function that accepts two objects and an int value and produces a result.
     * This is the indexed variant of BiFunction.
     *
     * @param <T> the type of the first object argument to the function
     * @param <U> the type of the second object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BiObjIntFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first object function argument
         * @param u the second object function argument
         * @param i the int index argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code t}, {@code u}, {@code i}
         */
        R apply(T t, U u, int i) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two objects and an int value.
     * This is the indexed variant of BiPredicate.
     *
     * @param <T> the type of the first object argument to the predicate
     * @param <U> the type of the second object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BiObjIntPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first object input argument
         * @param u the second object input argument
         * @param i the int index argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code t}, {@code u}, {@code i}
         */
        boolean test(T t, U u, int i) throws E;
    }

    /**
     * Represents an operation that accepts an int value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntObjConsumer<T, E extends Throwable> {

        /**
         * Returns the given consumer instance. This is a convenience method for type inference.
         *
         * @param <T> the type of the object argument to the consumer
         * @param <E> the type of exception that may be thrown
         * @param consumer the consumer to return
         * @return the same consumer instance
         * @throws IllegalArgumentException if {@code consumer} is {@code null}.
         */
        static <T, E extends Throwable> IntObjConsumer<T, E> of(final IntObjConsumer<T, E> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer, cs.consumer);

            return consumer;
        }

        /**
         * Performs this operation on the given arguments.
         *
         * @param i the int input argument
         * @param t the object input argument
         * @throws E if the consumer implementation throws while processing {@code i}, {@code t}
         */
        void accept(int i, T t) throws E;
    }

    /**
     * Represents a function that accepts an int value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntObjFunction<T, R, E extends Throwable> {

        /**
         * Returns the given function instance. This is a convenience method for type inference.
         *
         * @param <T> the type of the object argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of exception that may be thrown
         * @param function the function to return
         * @return the same function instance
         * @throws IllegalArgumentException if {@code function} is {@code null}.
         */
        static <T, R, E extends Throwable> IntObjFunction<T, R, E> of(final IntObjFunction<T, R, E> function) throws IllegalArgumentException {
            N.checkArgNotNull(function, cs.function);

            return function;
        }

        /**
         * Applies this function to the given arguments.
         *
         * @param i the int function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code i}, {@code t}
         */
        R apply(int i, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an int value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntObjPredicate<T, E extends Throwable> {

        /**
         * Returns the given predicate instance. This is a convenience method for type inference.
         *
         * @param <T> the type of the object argument to the predicate
         * @param <E> the type of exception that may be thrown
         * @param predicate the predicate to return
         * @return the same predicate instance
         * @throws IllegalArgumentException if {@code predicate} is {@code null}.
         */
        static <T, E extends Throwable> IntObjPredicate<T, E> of(final IntObjPredicate<T, E> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate, cs.predicate);

            return predicate;
        }

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the int input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code i}, {@code t}
         */
        boolean test(int i, T t) throws E;
    }

    /**
     * Represents an operation that accepts an int value and two objects and returns no result.
     *
     * @param <T> the type of the first object argument to the operation
     * @param <U> the type of the second object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBiObjConsumer<T, U, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param i the int input argument
         * @param t the first object input argument
         * @param u the second object input argument
         * @throws E if the consumer implementation throws while processing {@code i}, {@code t}, {@code u}
         */
        void accept(int i, T t, U u) throws E;
    }

    /**
     * Represents a function that accepts an int value and two objects and produces a result.
     *
     * @param <T> the type of the first object argument to the function
     * @param <U> the type of the second object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntBiObjFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param i the int function argument
         * @param t the first object function argument
         * @param u the second object function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code i}, {@code t}, {@code u}
         */
        R apply(int i, T t, U u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an int value and two objects.
     *
     * @param <T> the type of the first object argument to the predicate
     * @param <U> the type of the second object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntBiObjPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the int input argument
         * @param t the first object input argument
         * @param u the second object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code i}, {@code t}, {@code u}
         */
        boolean test(int i, T t, U u) throws E;
    }

    /**
     * Represents an operation that accepts two int values and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BiIntObjConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param i the first int input argument
         * @param j the second int input argument
         * @param t the object input argument
         * @throws E if the consumer implementation throws while processing {@code i}, {@code j}, {@code t}
         */
        void accept(int i, int j, T t) throws E;
    }

    /**
     * Represents a function that accepts two int values and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BiIntObjFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param i the first int function argument
         * @param j the second int function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code i}, {@code j}, {@code t}
         */
        R apply(int i, int j, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two int values and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BiIntObjPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the first int input argument
         * @param j the second int input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code i}, {@code j}, {@code t}
         */
        boolean test(int i, int j, T t) throws E;
    }

    /**
     * Represents an operation that accepts a long value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongObjConsumer<T, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param value the long input argument
         * @param t the object input argument
         * @throws E if the consumer implementation throws while processing {@code value}, {@code t}
         */
        void accept(long value, T t) throws E;
    }

    /**
     * Represents a function that accepts a long value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongObjFunction<T, R, E extends Throwable> {
        /**
         * Applies this function to the given arguments.
         *
         * @param value the long function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}, {@code t}
         */
        R apply(long value, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of a long value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongObjPredicate<T, E extends Throwable> {
        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param value the long input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}, {@code t}
         */
        boolean test(long value, T t) throws E;
    }

    /**
     * Represents an operation that accepts a double value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleObjConsumer<T, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param value the double input argument
         * @param t the object input argument
         * @throws E if the consumer implementation throws while processing {@code value}, {@code t}
         */
        void accept(double value, T t) throws E;
    }

    /**
     * Represents a function that accepts a double value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleObjFunction<T, R, E extends Throwable> {
        /**
         * Applies this function to the given arguments.
         *
         * @param value the double function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code value}, {@code t}
         */
        R apply(double value, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of a double value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleObjPredicate<T, E extends Throwable> {
        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param value the double input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if the predicate implementation throws while testing {@code value}, {@code t}
         */
        boolean test(double value, T t) throws E;
    }

    /**
     * Represents a function that accepts a boolean array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given boolean array.
         *
         * @param arguments the boolean array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(boolean... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> BooleanNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a char array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given char array.
         *
         * @param arguments the char array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(char... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> CharNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a byte array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given byte array.
         *
         * @param arguments the byte array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(byte... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> ByteNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a short array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortNFunction<R, E extends Throwable> {
        /**
         * Applies this function to the given short array.
         *
         * @param arguments the short array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(short... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> ShortNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts an int array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given int array.
         *
         * @param arguments the int array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(int... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> IntNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a long array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given long array.
         *
         * @param arguments the long array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(long... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> LongNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a float array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given float array.
         *
         * @param arguments the float array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(float... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> FloatNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a double array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given double array.
         *
         * @param arguments the double array argument
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        R apply(double... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> DoubleNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a variable number of arguments and produces a result.
     *
     * @param <T> the type of the input arguments to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface NFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param arguments the variable arguments of type T
         * @return the function result
         * @throws E if the function implementation throws while computing the result for {@code arguments}
         */
        @SuppressWarnings("unchecked")
        R apply(T... arguments) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         * @throws IllegalArgumentException if {@code after} is {@code null}.
         */
        default <V> NFunction<T, V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
            N.checkArgNotNull(after, cs.after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents an operation that accepts an int index and a boolean value and returns no result.
     * This is the indexed variant of BooleanConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBooleanConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the boolean element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, boolean e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a char value and returns no result.
     * This is the indexed variant of CharConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntCharConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the char element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, char e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a byte value and returns no result.
     * This is the indexed variant of ByteConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntByteConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the byte element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, byte e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a short value and returns no result.
     * This is the indexed variant of ShortConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntShortConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the short element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, short e) throws E;
    }

    /**
     * Represents an operation that accepts two int values and returns no result.
     * The first int typically represents an index.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntIntConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the int element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, int e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a long value and returns no result.
     * This is the indexed variant of LongConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntLongConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the long element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, long e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a float value and returns no result.
     * This is the indexed variant of FloatConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntFloatConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the float element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, float e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a double value and returns no result.
     * This is the indexed variant of DoubleConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntDoubleConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the zero-based index associated with the value
         * @param e the double element at the index
         * @throws E if the consumer implementation throws while processing {@code idx}, {@code e}
         */
        void accept(int idx, double e) throws E;
    }

    /**
     * Utility class containing functional interfaces that can throw two different types of exceptions.
     */
    public static final class EE {
        /** Prevents instantiation of this utility class. */
        private EE() {
            // Utility class.
        }

        /**
         * Represents a task that returns no result and may throw two types of exceptions.
         *
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable> {

            /**
             * Executes the task.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Runnable<IOException, SQLException> task = () -> {
             *     Files.readString(path);
             *     connection.commit();
             * };
             * }</pre>
             *
             * @throws E if the first type of error occurs during execution
             * @throws E2 if the second type of error occurs during execution
             */
            void run() throws E, E2;
        }

        /**
         * Represents a task that returns a result and may throw two types of exceptions.
         *
         * @param <R> the type of the result
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable> {

            /**
             * Computes a result.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Callable<String, IOException, SQLException> task = () -> {
             *     String data = Files.readString(path);
             *     connection.commit();
             *     return data;
             * };
             * }</pre>
             *
             * @return the computed result
             * @throws E if the first type of error occurs during computation
             * @throws E2 if the second type of error occurs during computation
             */
            R call() throws E, E2;
        }

        /**
         * Represents a supplier of results that may throw two types of exceptions.
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Gets a result.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Supplier<String, IOException, SQLException> supplier = () -> {
             *     return queryDatabase();
             * };
             * }</pre>
             *
             * @return a result
             * @throws E if the first type of error occurs while getting the result
             * @throws E2 if the second type of error occurs while getting the result
             */
            T get() throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of one argument that may throw two types of exceptions.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Predicate<String, IOException, SQLException> pred = s -> {
             *     return !Files.readString(path).isEmpty();
             * };
             * }</pre>
             *
             * @param t the input argument
             * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(T t) throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of two arguments that may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.BiPredicate<String, String, IOException, SQLException> pred = (s1, s2) -> {
             *     return Files.readString(Path.of(s1)).equals(Files.readString(Path.of(s2)));
             * };
             * }</pre>
             *
             * @param t the first input argument
             * @param u the second input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(T t, U u) throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of three arguments that may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.TriPredicate<String, String, String, IOException, SQLException> pred = (a, b, c) -> {
             *     return Files.readString(Path.of(a)).contains(Files.readString(Path.of(b)));
             * };
             * }</pre>
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(A a, B b, C c) throws E, E2;
        }

        /**
         * Represents a function that accepts one argument and produces a result, and may throw two types of exceptions.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Function<String, String, IOException, SQLException> func = path -> {
             *     return Files.readString(Path.of(path));
             * };
             * }</pre>
             *
             * @param t the function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(T t) throws E, E2;
        }

        /**
         * Represents a function that accepts two arguments and produces a result, and may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.BiFunction<String, String, String, IOException, SQLException> func = (a, b) -> {
             *     return Files.readString(Path.of(a)) + Files.readString(Path.of(b));
             * };
             * }</pre>
             *
             * @param t the first function argument
             * @param u the second function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(T t, U u) throws E, E2;
        }

        /**
         * Represents a function that accepts three arguments and produces a result, and may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.TriFunction<String, String, String, String, IOException, SQLException> func = (a, b, c) -> {
             *     return Files.readString(Path.of(a)) + Files.readString(Path.of(b)) + c;
             * };
             * }</pre>
             *
             * @param a the first function argument
             * @param b the second function argument
             * @param c the third function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(A a, B b, C c) throws E, E2;
        }

        /**
         * Represents an operation that accepts a single input argument and returns no result, and may throw two types of exceptions.
         *
         * @param <T> the type of the input to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.Consumer<String, IOException, SQLException> consumer = path -> {
             *     Files.readString(Path.of(path));
             * };
             * }</pre>
             *
             * @param t the input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(T t) throws E, E2;
        }

        /**
         * Represents an operation that accepts two input arguments and returns no result, and may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the operation
         * @param <U> the type of the second argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.BiConsumer<String, String, IOException, SQLException> consumer = (a, b) -> {
             *     Files.writeString(Path.of(a), b);
             * };
             * }</pre>
             *
             * @param t the first input argument
             * @param u the second input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(T t, U u) throws E, E2;
        }

        /**
         * Represents an operation that accepts three input arguments and returns no result, and may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the operation
         * @param <B> the type of the second argument to the operation
         * @param <C> the type of the third argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EE.TriConsumer<String, String, String, IOException, SQLException> consumer = (a, b, c) -> {
             *     System.out.println(a + b + c);
             * };
             * }</pre>
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(A a, B b, C c) throws E, E2;
        }
    }

    /**
     * Utility class containing functional interfaces that can throw three different types of exceptions.
     */
    public static final class EEE {

        /** Prevents instantiation of this utility class. */
        private EEE() {
            // Utility class.
        }

        /**
         * Represents a task that returns no result and may throw three types of exceptions.
         *
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Executes the task.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Runnable<IOException, SQLException, RuntimeException> task = () -> {
             *     Files.readString(path);
             *     connection.commit();
             * };
             * }</pre>
             *
             * @throws E if the first type of error occurs during execution
             * @throws E2 if the second type of error occurs during execution
             * @throws E3 if the third type of error occurs during execution
             */
            void run() throws E, E2, E3;
        }

        /**
         * Represents a task that returns a result and may throw three types of exceptions.
         *
         * @param <R> the type of the result
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Computes a result.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Callable<String, IOException, SQLException, RuntimeException> task = () -> {
             *     String data = Files.readString(path);
             *     connection.commit();
             *     return data;
             * };
             * }</pre>
             *
             * @return the computed result
             * @throws E if the first type of error occurs during computation
             * @throws E2 if the second type of error occurs during computation
             * @throws E3 if the third type of error occurs during computation
             */
            R call() throws E, E2, E3;
        }

        /**
         * Represents a supplier of results that may throw three types of exceptions.
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Gets a result.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Supplier<String, IOException, SQLException, RuntimeException> supplier = () -> {
             *     return queryDatabase();
             * };
             * }</pre>
             *
             * @return a result
             * @throws E if the first type of error occurs while getting the result
             * @throws E2 if the second type of error occurs while getting the result
             * @throws E3 if the third type of error occurs while getting the result
             */
            T get() throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of one argument that may throw three types of exceptions.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Predicate<String, IOException, SQLException, RuntimeException> pred = s -> {
             *     return !Files.readString(path).isEmpty();
             * };
             * }</pre>
             *
             * @param t the input argument
             * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(T t) throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of two arguments that may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.BiPredicate<String, String, IOException, SQLException, RuntimeException> pred = (s1, s2) -> {
             *     return Files.readString(Path.of(s1)).equals(Files.readString(Path.of(s2)));
             * };
             * }</pre>
             *
             * @param t the first input argument
             * @param u the second input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of three arguments that may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.TriPredicate<String, String, String, IOException, SQLException, RuntimeException> pred = (a, b, c) -> {
             *     return Files.readString(Path.of(a)).contains(Files.readString(Path.of(b)));
             * };
             * }</pre>
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(A a, B b, C c) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts one argument and produces a result, and may throw three types of exceptions.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Function<String, String, IOException, SQLException, RuntimeException> func = path -> {
             *     return Files.readString(Path.of(path));
             * };
             * }</pre>
             *
             * @param t the function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(T t) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts two arguments and produces a result, and may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.BiFunction<String, String, String, IOException, SQLException, RuntimeException> func = (a, b) -> {
             *     return Files.readString(Path.of(a)) + Files.readString(Path.of(b));
             * };
             * }</pre>
             *
             * @param t the first function argument
             * @param u the second function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts three arguments and produces a result, and may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.TriFunction<String, String, String, String, IOException, SQLException, RuntimeException> func = (a, b, c) -> {
             *     return Files.readString(Path.of(a)) + Files.readString(Path.of(b)) + c;
             * };
             * }</pre>
             *
             * @param a the first function argument
             * @param b the second function argument
             * @param c the third function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(A a, B b, C c) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts a single input argument and returns no result, and may throw three types of exceptions.
         *
         * @param <T> the type of the input to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given argument.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.Consumer<String, IOException, SQLException, RuntimeException> consumer = path -> {
             *     Files.readString(Path.of(path));
             * };
             * }</pre>
             *
             * @param t the input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(T t) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts two input arguments and returns no result, and may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the operation
         * @param <U> the type of the second argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.BiConsumer<String, String, IOException, SQLException, RuntimeException> consumer = (a, b) -> {
             *     Files.writeString(Path.of(a), b);
             * };
             * }</pre>
             *
             * @param t the first input argument
             * @param u the second input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts three input arguments and returns no result, and may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the operation
         * @param <B> the type of the second argument to the operation
         * @param <C> the type of the third argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * <p><b>Usage Examples:</b></p>
             * <pre>{@code
             * Throwables.EEE.TriConsumer<String, String, String, IOException, SQLException, RuntimeException> consumer = (a, b, c) -> {
             *     System.out.println(a + b + c);
             * };
             * }</pre>
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(A a, B b, C c) throws E, E2, E3;
        }
    }

    /**
     * A thread-safe lazy initializer that defers the creation of the underlying value until first access.
     * The value is computed using the provided supplier; once initialization succeeds, subsequent calls return the cached value (a failed initialization is retried on the next call).
     *
     * @param <T> the type of the value to be lazily initialized
     * @param <E> the type of exception that may be thrown during initialization
     */
    static final class LazyInitializer<T, E extends Throwable> implements Throwables.Supplier<T, E> {
        /**
         * A private monitor rather than {@code this}: this object is what the caller receives from
         * {@code Fnn.memoize(Throwables.Supplier)} and {@code N.lazyInitChecked(Throwables.Supplier)}, and a caller
         * doing {@code synchronized (lazy) { ... }} does not acquire the initialization monitor.
         */
        private final Object lock = new Object();

        /** Supplier retained until initialization succeeds. */
        private Supplier<T, E> supplier;

        /** Whether a value, including {@code null}, has been initialized successfully. */
        private volatile boolean initialized = false;

        /** Cached value; published by the volatile write to {@link #initialized}. */
        private volatile T value = null; //NOSONAR

        /** Guards against recursive access while the supplier is running. */
        private boolean initializing = false;

        /** Shared by all recursive accesses in one attempt, then cleared so failures remain retryable. */
        private IllegalStateException recursiveFailure = null;

        /**
         * Creates an initializer backed by {@code supplier}.
         *
         * @param supplier the non-null supplier to invoke on first access
         */
        LazyInitializer(final Throwables.Supplier<T, E> supplier) {

            this.supplier = supplier;
        }

        /**
         * Creates a new LazyInitializer with the specified supplier.
         * If the supplier is already a LazyInitializer, it is returned as-is to avoid double-wrapping.
         *
         * <p>The returned LazyInitializer calls the supplier on the first invocation of {@code get()}; once it
         * succeeds the result is cached and the supplier is never called again (a failed initialization is retried
         * on the next call). The initialization is thread-safe using double-checked locking.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // LazyInitializer is internal; callers obtain one through N.lazyInitChecked
         * Throwables.Supplier<Database, SQLException> dbInit = N.lazyInitChecked(() ->
         *     createExpensiveDatabase()
         * );
         * // Database is not created until first call to dbInit.get()
         * Database db = dbInit.get();      // initializes once and returns the instance
         * Database sameDb = dbInit.get();  // returns the cached instance
         * }</pre>
         *
         * @param <T> the type of the value to be lazily initialized
         * @param <E> the type of exception that may be thrown during initialization
         * @param supplier the supplier that will provide the value when first requested
         * @return a LazyInitializer that will use the provided supplier
         * @throws IllegalArgumentException if {@code supplier} is {@code null}.
         */
        public static <T, E extends Throwable> LazyInitializer<T, E> of(final Throwables.Supplier<T, E> supplier) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, cs.supplier);

            if (supplier instanceof LazyInitializer) {
                return (LazyInitializer<T, E>) supplier;
            }

            return new LazyInitializer<>(supplier);
        }

        /**
         * Gets the lazily initialized value. On first access, the value is computed using the supplier
         * and cached for subsequent calls. After successful initialization, the supplier reference is
         * released so objects captured only for construction can be reclaimed. This method is thread-safe:
         * initialization is serialized on a private monitor, not on this object. Synchronizing on this
         * initializer does not prevent another thread from initializing it or accessing its cached value.
         *
         * @return the lazily initialized value
         * @throws IllegalStateException if the value is accessed recursively from within its own initialization
         * @throws E if the supplier throws an exception during initialization
         */
        @Override
        public T get() throws IllegalStateException, E {
            if (!initialized) {
                synchronized (lock) {
                    if (!initialized) {
                        if (initializing) {
                            if (recursiveFailure == null) {
                                recursiveFailure = new IllegalStateException("Recursive initialization of deferred value");
                            }

                            throw recursiveFailure;
                        }

                        initializing = true;

                        try {
                            final T computed = supplier.get();

                            // A supplier may catch the recursive-access exception. Do not publish a
                            // value from an initialization attempt that already violated the invariant, and do not
                            // even store it: the rejected object would otherwise stay reachable from this
                            // initializer until some later attempt succeeded (forever, if none does).
                            if (recursiveFailure != null) {
                                throw recursiveFailure;
                            }

                            value = computed;
                            supplier = null;
                            initialized = true;
                        } finally {
                            initializing = false;
                            recursiveFailure = null;
                        }
                    }
                }
            }

            return value;
        }
    }
}
