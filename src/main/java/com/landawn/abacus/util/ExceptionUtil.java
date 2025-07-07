/*
 * Copyright (C) 2018 HaiYang Li
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.exception.UncheckedParseException;
import com.landawn.abacus.exception.UncheckedReflectiveOperationException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.u.Optional;

/**
 * Utility class for exception handling and conversion.
 * Provides methods to convert checked exceptions to runtime exceptions, extract stack traces,
 * find causes, and handle exception messages.
 * 
 * <p>This class contains methods copied from Apache Commons and Google Guava under Apache License v2.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Convert checked exception to runtime exception
 * try {
 *     someMethodThatThrowsIOException();
 * } catch (IOException e) {
 *     throw ExceptionUtil.toRuntimeException(e);
 * }
 * 
 * // Get stack trace as string
 * try {
 *     riskyOperation();
 * } catch (Exception e) {
 *     String stackTrace = ExceptionUtil.getStackTrace(e);
 *     logger.error(stackTrace);
 * }
 * 
 * // Find specific cause in exception chain
 * try {
 *     databaseOperation();
 * } catch (Exception e) {
 *     Optional<SQLException> sqlEx = ExceptionUtil.findCause(e, SQLException.class);
 *     if (sqlEx.isPresent()) {
 *         handleSQLException(sqlEx.get());
 *     }
 * }
 * }</pre>
 */
public final class ExceptionUtil {

    private static final int MAX_DEPTH_FOR_LOOP_CAUSE = 100;

    private ExceptionUtil() {
        // singleton
    }

    private static final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> toRuntimeExceptionFuncMap = new ConcurrentHashMap<>();

    static {
        toRuntimeExceptionFuncMap.put(RuntimeException.class, e -> (RuntimeException) e);

        toRuntimeExceptionFuncMap.put(Error.class, RuntimeException::new); // right or not?

        toRuntimeExceptionFuncMap.put(IOException.class, e -> new UncheckedIOException((IOException) e));

        toRuntimeExceptionFuncMap.put(SQLException.class, e -> new UncheckedSQLException((SQLException) e));

        toRuntimeExceptionFuncMap.put(ReflectiveOperationException.class, e -> new UncheckedReflectiveOperationException((ReflectiveOperationException) e));

        toRuntimeExceptionFuncMap.put(ParseException.class, e -> new UncheckedParseException((ParseException) e));

        toRuntimeExceptionFuncMap.put(InterruptedException.class, e -> new UncheckedInterruptedException((InterruptedException) e));

        toRuntimeExceptionFuncMap.put(ExecutionException.class, e -> e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause()));

        toRuntimeExceptionFuncMap.put(InvocationTargetException.class,
                e -> e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause()));

        toRuntimeExceptionFuncMap.put(UndeclaredThrowableException.class,
                e -> e.getCause() == null ? (UndeclaredThrowableException) e : toRuntimeException(e.getCause()));
    }

    private static final Function<Throwable, RuntimeException> RUNTIME_FUNC = e -> (RuntimeException) e;

    private static final Function<Throwable, RuntimeException> CHECKED_FUNC = UncheckedException::new;

    private static final String UncheckedSQLExceptionClassName = UncheckedSQLException.class.getSimpleName();

    private static final String UncheckedIOExceptionClassName = UncheckedIOException.class.getSimpleName();

    /**
     * Registers a custom mapper for converting specific exception types to RuntimeException.
     * This allows customization of how checked exceptions are converted to unchecked exceptions.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Register custom mapper for MyCheckedException
     * ExceptionUtil.registerRuntimeExceptionMapper(
     *     MyCheckedException.class,
     *     e -> new MyRuntimeException(e.getMessage(), e)
     * );
     * }</pre>
     *
     * @param <E> the type of exception to map
     * @param exceptionClass the class of the exception to map
     * @param runtimeExceptionMapper the function that converts the exception to RuntimeException
     */
    public static <E extends Throwable> void registerRuntimeExceptionMapper(final Class<E> exceptionClass,
            final Function<E, RuntimeException> runtimeExceptionMapper) {
        registerRuntimeExceptionMapper(exceptionClass, runtimeExceptionMapper, false);
    }

    /**
     * Registers a custom mapper for converting specific exception types to RuntimeException,
     * with an option to force overwrite existing mappings.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Force registration even if mapper already exists
     * ExceptionUtil.registerRuntimeExceptionMapper(
     *     CustomException.class,
     *     e -> new CustomRuntimeException(e),
     *     true  // force overwrite
     * );
     * }</pre>
     *
     * @param <E> the type of exception to map
     * @param exceptionClass the class of the exception to map
     * @param runtimeExceptionMapper the function that converts the exception to RuntimeException
     * @param force if true, overwrites existing mapper; if false, throws exception if mapper exists
     * @throws IllegalArgumentException if trying to register built-in classes or if mapper already exists and force is false
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Throwable> void registerRuntimeExceptionMapper(final Class<E> exceptionClass,
            final Function<E, RuntimeException> runtimeExceptionMapper, final boolean force) throws IllegalArgumentException {
        N.checkArgNotNull(exceptionClass, cs.exceptionClass);
        N.checkArgNotNull(runtimeExceptionMapper, cs.runtimeExceptionMapper);

        if (N.isBuiltinClass(exceptionClass)) {
            throw new IllegalArgumentException("Can't register Exception class with package starting with \"java.\", \"javax.\", \"com.landawn.abacus\": "
                    + exceptionClass.getPackage().getName());
        }

        if (toRuntimeExceptionFuncMap.containsKey(exceptionClass) && !force) {
            throw new IllegalArgumentException("Exception class: " + ClassUtil.getCanonicalClassName(exceptionClass) + " has already been registered");
        }

        toRuntimeExceptionFuncMap.put(exceptionClass, (Function) runtimeExceptionMapper);
    }

    /**
     * Converts the specified Exception to a RuntimeException if it's a checked exception.
     * If it's already a RuntimeException, returns itself.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     Files.readAllLines(path);
     * } catch (IOException e) {
     *     throw ExceptionUtil.toRuntimeException(e);
     *     // Throws UncheckedIOException
     * }
     * }</pre>
     *
     * @param e the exception to convert
     * @return the converted runtime exception or the original if already runtime
     */
    public static RuntimeException toRuntimeException(final Exception e) {
        return toRuntimeException(e, false, false);
    }

    /**
     * Converts the specified Exception to a RuntimeException with option to call Thread.interrupt().
     * Useful when handling InterruptedException to preserve interrupted status.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     Thread.sleep(1000);
     * } catch (InterruptedException e) {
     *     throw ExceptionUtil.toRuntimeException(e, true);
     *     // Converts to UncheckedInterruptedException and calls Thread.interrupt()
     * }
     * }</pre>
     *
     * @param e the exception to convert
     * @param callInterrupt whether to call {@code Thread.currentThread().interrupt()} if the exception is an InterruptedException
     * @return the converted runtime exception
     */
    public static RuntimeException toRuntimeException(final Exception e, final boolean callInterrupt) {
        return toRuntimeException(e, callInterrupt, false);
    }

    /**
     * Converts the specified Throwable to a RuntimeException if it's a checked exception.
     * If it's already a RuntimeException, returns itself.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     reflectiveOperation();
     * } catch (Throwable t) {
     *     throw ExceptionUtil.toRuntimeException(t);
     * }
     * }</pre>
     *
     * @param e the throwable to convert
     * @return the converted runtime exception
     */
    public static RuntimeException toRuntimeException(final Throwable e) {
        return toRuntimeException(e, false, false);
    }

    /**
     * Converts the specified Throwable to a RuntimeException with option to call Thread.interrupt().
     * If it's an Error and throwIfItIsError is false, wraps it in RuntimeException.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     riskyOperation();
     * } catch (Throwable t) {
     *     throw ExceptionUtil.toRuntimeException(t, true);
     * }
     * }</pre>
     *
     * @param e the throwable to convert
     * @param callInterrupt whether to call {@code Thread.currentThread().interrupt()} if the exception is an InterruptedException
     * @return the converted runtime exception
     */
    public static RuntimeException toRuntimeException(final Throwable e, final boolean callInterrupt) {
        return toRuntimeException(e, callInterrupt, false);
    }

    /**
     * Converts the specified Throwable to a RuntimeException with full control over behavior.
     * Provides options to handle InterruptedException and Error cases.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     criticalOperation();
     * } catch (Throwable t) {
     *     // Convert to runtime, call interrupt if needed, throw Error as-is
     *     throw ExceptionUtil.toRuntimeException(t, true, true);
     * }
     * }</pre>
     *
     * @param e the throwable to convert
     * @param callInterrupt whether to call {@code Thread.currentThread().interrupt()} if the exception is an InterruptedException
     * @param throwIfItIsError whether to throw the throwable if it is an Error
     * @return the converted runtime exception
     * @throws Error if e is an Error and throwIfItIsError is true
     */
    public static RuntimeException toRuntimeException(final Throwable e, final boolean callInterrupt, final boolean throwIfItIsError) {
        if (throwIfItIsError && e instanceof Error) {
            throw (Error) e;
        }

        if (callInterrupt && e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        final Class<Throwable> cls = (Class<Throwable>) e.getClass();
        Function<Throwable, RuntimeException> func = toRuntimeExceptionFuncMap.get(cls);

        Map.Entry<Class<? extends Throwable>, Function<Throwable, RuntimeException>> candidate = null;

        if (func == null) {
            for (final Map.Entry<Class<? extends Throwable>, Function<Throwable, RuntimeException>> entry : toRuntimeExceptionFuncMap.entrySet()) { //NOSONAR
                if (entry.getKey().isAssignableFrom(cls) && (candidate == null || candidate.getKey().isAssignableFrom(entry.getKey()))) {
                    candidate = entry;
                }
            }

            if (candidate == null) {
                if (e instanceof RuntimeException) {
                    func = RUNTIME_FUNC;
                } else {
                    func = CHECKED_FUNC;
                }
            } else {
                func = candidate.getValue();
            }

            toRuntimeExceptionFuncMap.put(cls, func);
        }

        return func.apply(e);
    }

    static final Predicate<String> uncheckedExceptionNameTester = Pattern.compile("Unchecked[a-zA-Z0-9]*Exception").asPredicate();
    static final Map<Class<? extends Throwable>, Class<? extends Throwable>> runtimeToCheckedExceptionClassMap = new ConcurrentHashMap<>();

    /**
     * Attempts to extract the original checked exception from a runtime exception wrapper.
     * This is useful when dealing with wrapped exceptions from reflection or concurrent operations.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     future.get();
     * } catch (ExecutionException e) {
     *     Exception original = ExceptionUtil.tryToGetOriginalCheckedException(e);
     *     // May return the actual cause instead of ExecutionException
     * }
     * }</pre>
     *
     * @param e the exception to unwrap
     * @return the original checked exception if found, otherwise returns the input exception
     */
    public static Exception tryToGetOriginalCheckedException(final Exception e) {
        return tryToGetOriginalCheckedException(e, MAX_DEPTH_FOR_LOOP_CAUSE);
    }

    private static Exception tryToGetOriginalCheckedException(final Exception e, int loopCount) {
        if (loopCount <= 0) {
            return e;
        }

        if (e instanceof RuntimeException && e.getCause() instanceof Exception && !(e.getCause() instanceof RuntimeException)) {
            if (e instanceof UncheckedException //
                    || (uncheckedExceptionNameTester.test(ClassUtil.getSimpleClassName(e.getClass())))) {
                return (Exception) e.getCause();
            }

            final Throwable cause = e.getCause();

            if (cause.getClass().equals(runtimeToCheckedExceptionClassMap.get(e.getClass()))) {
                return (Exception) cause;
            }

            if (toRuntimeExceptionFuncMap.containsKey(cause.getClass())
                    && toRuntimeExceptionFuncMap.get(cause.getClass()).apply(cause).getClass().equals(e.getClass())) {

                runtimeToCheckedExceptionClassMap.put(e.getClass(), cause.getClass());

                return (Exception) cause;
            }
        } else if (e.getCause() instanceof Exception && (e instanceof InvocationTargetException || e instanceof ExecutionException)) {
            return tryToGetOriginalCheckedException((Exception) e.getCause(), --loopCount);
        }

        return e;
    }

    /**
     * Checks if the exception or any of its causes is of the specified type.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     complexDatabaseOperation();
     * } catch (Exception e) {
     *     if (ExceptionUtil.hasCause(e, SQLException.class)) {
     *         handleDatabaseError();
     *     }
     * }
     * }</pre>
     *
     * @param e the exception to check
     * @param targetExceptionType the exception type to look for
     * @return true if the exception or any cause is of the specified type
     */
    public static boolean hasCause(final Throwable e, final Class<? extends Throwable> targetExceptionType) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (targetExceptionType.isAssignableFrom(cause.getClass())) {
                return true;
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return false;
    }

    /**
     * Checks if the exception or any of its causes matches the specified predicate.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     operation();
     * } catch (Exception e) {
     *     if (ExceptionUtil.hasCause(e, ex -> ex.getMessage().contains("timeout"))) {
     *         handleTimeout();
     *     }
     * }
     * }</pre>
     *
     * @param e the exception to check
     * @param targetExceptionTester the predicate to test each exception in the cause chain
     * @return true if any exception in the cause chain matches the predicate
     */
    public static boolean hasCause(final Throwable e, final Predicate<? super Throwable> targetExceptionTester) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (targetExceptionTester.test(cause)) {
                return true;
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return false;
    }

    /**
     * Checks if the exception or any of its causes is a SQLException or UncheckedSQLException.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     databaseOperation();
     * } catch (Exception e) {
     *     if (ExceptionUtil.hasSQLCause(e)) {
     *         rollbackTransaction();
     *     }
     * }
     * }</pre>
     *
     * @param e the exception to check
     * @return true if the exception chain contains a SQL-related exception
     */
    public static boolean hasSQLCause(final Throwable e) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (cause instanceof SQLException || UncheckedSQLExceptionClassName.equals(cause.getClass().getSimpleName())) {
                return true;
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return false;
    }

    /**
     * Checks if the exception or any of its causes is an IOException or UncheckedIOException.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     fileOperation();
     * } catch (Exception e) {
     *     if (ExceptionUtil.hasIOCause(e)) {
     *         handleFileError();
     *     }
     * }
     * }</pre>
     *
     * @param e the exception to check
     * @return true if the exception chain contains an IO-related exception
     */
    public static boolean hasIOCause(final Throwable e) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (cause instanceof IOException || UncheckedIOExceptionClassName.equals(cause.getClass().getSimpleName())) {
                return true;
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return false;
    }

    /**
     * Checks if the throwable is either NullPointerException or IllegalArgumentException.
     * Useful for identifying common programming errors.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     validateInput(input);
     * } catch (Exception e) {
     *     if (ExceptionUtil.isNullPointerOrIllegalArgumentException(e)) {
     *         return BAD_REQUEST;
     *     }
     * }
     * }</pre>
     *
     * @param e the throwable to check
     * @return true if the throwable is NullPointerException or IllegalArgumentException
     */
    @Beta
    public static boolean isNullPointerOrIllegalArgumentException(final Throwable e) {
        return e instanceof NullPointerException || e instanceof IllegalArgumentException;
    }

    /**
     * Returns a list containing all exceptions in the cause chain.
     * The list starts with the given exception and includes all causes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     complexOperation();
     * } catch (Exception e) {
     *     List<Throwable> causes = ExceptionUtil.listCause(e);
     *     for (Throwable cause : causes) {
     *         logger.error("Cause: " + cause.getClass().getName());
     *     }
     * }
     * }</pre>
     *
     * @param e the starting exception
     * @return a list of all exceptions in the cause chain
     */
    public static List<Throwable> listCause(final Throwable e) {
        final List<Throwable> list = new ArrayList<>();
        Throwable cause = e;

        while (cause != null && !list.contains(cause)) {
            list.add(cause);
            cause = cause.getCause();
        }

        return list;
    }

    /**
     * Returns the first cause in the exception chain (the root cause).
     * If there is no cause, returns the input throwable itself.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     wrappedOperation();
     * } catch (Exception e) {
     *     Throwable root = ExceptionUtil.firstCause(e);
     *     logger.error("Root cause: " + root.getMessage());
     * }
     * }</pre>
     *
     * @param e the exception to traverse
     * @return the root cause or the input if no cause found
     */
    public static Throwable firstCause(final Throwable e) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable cause = e;
        Throwable result = e;

        while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != result) {
            result = cause;
        }

        return result;
    }

    /**
     * Finds the first occurrence of the specified exception type in the cause chain.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     serviceCall();
     * } catch (Exception e) {
     *     Optional<IOException> ioError = ExceptionUtil.findCause(e, IOException.class);
     *     if (ioError.isPresent()) {
     *         handleIOError(ioError.get());
     *     }
     * }
     * }</pre>
     *
     * @param <E> the type of exception to find
     * @param e the exception to search through
     * @param targetExceptionType the class of the exception to find
     * @return an Optional containing the found exception, or empty if not found
     */
    public static <E extends Throwable> Optional<E> findCause(final Throwable e, final Class<? extends E> targetExceptionType) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (targetExceptionType.isAssignableFrom(cause.getClass())) {
                return Optional.of((E) cause);
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return Optional.empty();
    }

    /**
     * Finds the first exception in the cause chain that matches the specified predicate.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     complexOperation();
     * } catch (Exception e) {
     *     Optional<Throwable> timeoutError = ExceptionUtil.findCause(e, 
     *         ex -> ex.getMessage() != null && ex.getMessage().contains("timeout")
     *     );
     *     if (timeoutError.isPresent()) {
     *         handleTimeout();
     *     }
     * }
     * }</pre>
     *
     * @param <E> the type of exception expected
     * @param e the exception to search through
     * @param targetExceptionTester the predicate to match exceptions
     * @return an Optional containing the found exception, or empty if not found
     */
    public static <E extends Throwable> Optional<E> findCause(final Throwable e, final Predicate<? super Throwable> targetExceptionTester) {
        int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
        Throwable prevCause = null;
        Throwable cause = e;

        do {
            if (targetExceptionTester.test(cause)) {
                return Optional.of((E) cause);
            }

            prevCause = cause;
        } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);

        return Optional.empty();
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the stack trace from a Throwable as a String.
     * 
     * <p>The result of this method varies by JDK version as this method
     * uses {@link Throwable#printStackTrace(java.io.PrintWriter)}.
     * On JDK1.3 and earlier, the cause exception will not be shown
     * unless the specified throwable alters printStackTrace.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     riskyOperation();
     * } catch (Exception e) {
     *     String stackTrace = ExceptionUtil.getStackTrace(e);
     *     logger.error("Full stack trace:\n" + stackTrace);
     *     emailAdmin(stackTrace);
     * }
     * }</pre>
     *
     * @param throwable the {@link Throwable} to be examined, which may be null
     * @return the stack trace as generated by the exception's
     *         {@code printStackTrace(PrintWriter)} method, or an empty String if {@code null} input
     */
    public static String getStackTrace(final Throwable throwable) {
        if (throwable == null) {
            return Strings.EMPTY;
        }
        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    /**
     * Gets the error message from an exception.
     * If the exception has no message, attempts to get the message from its cause.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     operation();
     * } catch (Exception e) {
     *     String msg = ExceptionUtil.getErrorMessage(e);
     *     showUserError(msg);
     * }
     * }</pre>
     *
     * @param e the exception
     * @return the error message, or the exception class name if no message is available
     */
    public static String getErrorMessage(final Throwable e) {
        return getErrorMessage(e, false);
    }

    /**
     * Gets the error message from an exception with optional exception class name prefix.
     * For SQLException, includes the error code.
     *
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     databaseOperation();
     * } catch (SQLException e) {
     *     String msg = ExceptionUtil.getErrorMessage(e, true);
     *     // Returns: "SQLException|1054|Unknown column 'xyz'"
     * }
     * }</pre>
     *
     * @param e the exception
     * @param withExceptionClassName if true, prefixes the message with the exception class name
     * @return the formatted error message
     */
    public static String getErrorMessage(final Throwable e, final boolean withExceptionClassName) {
        String msg = e.getMessage();

        if (Strings.isEmpty(msg) && e.getCause() != null) {
            int maxDepth = MAX_DEPTH_FOR_LOOP_CAUSE;
            Throwable prevCause = null;
            Throwable cause = e;

            do {
                msg = cause.getMessage();

                if (Strings.isNotEmpty(msg)) {
                    break;
                }

                prevCause = cause;
            } while (maxDepth-- > 0 && (cause = cause.getCause()) != null && cause != prevCause);
        }

        if (Strings.isEmpty(msg)) {
            msg = e.getClass().getCanonicalName();
        }

        if (withExceptionClassName) {
            if (e instanceof SQLException) {
                return e.getClass().getSimpleName() + "|" + ((SQLException) e).getErrorCode() + "|" + msg;
            } else {
                return e.getClass().getSimpleName() + "|" + msg;
            }
        } else {
            if (e instanceof SQLException) {
                return ((SQLException) e).getErrorCode() + "|" + msg;
            } else {
                return msg;
            }
        }
    }

    // --------------------------------------------------------------------------------------------->
}