/*
 * Copyright (C) 2018 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Note: This class contains the methods copied from Apache Commons and Google Guava under Apache License v2.
 *
 */
public final class ExceptionUtil {

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

        toRuntimeExceptionFuncMap.put(InterruptedException.class, e -> {
            Thread.currentThread().interrupt();// TODO is it needed ? is it right to do here?
            return new UncheckedInterruptedException((InterruptedException) e);
        });

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
     *
     * @param <E>
     * @param exceptionClass
     * @param runtimeExceptionMapper
     */
    public static <E extends Throwable> void registerRuntimeExceptionMapper(final Class<E> exceptionClass,
            final Function<E, RuntimeException> runtimeExceptionMapper) {
        registerRuntimeExceptionMapper(exceptionClass, runtimeExceptionMapper, false);
    }

    /**
     *
     *
     * @param <E>
     * @param exceptionClass
     * @param runtimeExceptionMapper
     * @param force
     * @throws IllegalArgumentException
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

        toRuntimeExceptionFuncMap.put((Class) exceptionClass, (Function) runtimeExceptionMapper);
    }

    /**
     *
     * @param e
     * @return
     */
    public static RuntimeException toRuntimeException(final Exception e) {
        return toRuntimeException(e, false);
    }

    /**
     * Converts the specified {@code Throwable} to a {@code RuntimeException} if it's a checked {@code exception} or an {@code Error}, otherwise returns itself.
     *
     * @param e
     * @return
     * @see #registerRuntimeExceptionMapper(Class, Function)
     */
    public static RuntimeException toRuntimeException(final Throwable e) {
        return toRuntimeException(e, false);
    }

    /**
     * Converts the specified {@code Throwable} to a {@code RuntimeException} if it's a checked {@code exception}, or throw it if it's an {@code Error}. Otherwise returns itself.
     *
     * @param e
     * @param throwIfItIsError
     * @return
     */
    public static RuntimeException toRuntimeException(final Throwable e, final boolean throwIfItIsError) {
        if (throwIfItIsError && e instanceof Error) {
            throw (Error) e;
        }

        final Class<Throwable> cls = (Class<Throwable>) e.getClass();
        Function<Throwable, RuntimeException> func = toRuntimeExceptionFuncMap.get(cls);

        Map.Entry<Class<? extends Throwable>, Function<Throwable, RuntimeException>> candicate = null;

        if (func == null) {
            for (final Map.Entry<Class<? extends Throwable>, Function<Throwable, RuntimeException>> entry : toRuntimeExceptionFuncMap.entrySet()) { //NOSONAR
                if (entry.getKey().isAssignableFrom(cls) && (candicate == null || candicate.getKey().isAssignableFrom(entry.getKey()))) {
                    candicate = entry;
                }
            }

            if (candicate == null) {
                if (e instanceof RuntimeException) {
                    func = RUNTIME_FUNC;
                } else {
                    func = CHECKED_FUNC;
                }
            } else {
                func = candicate.getValue();
            }

            toRuntimeExceptionFuncMap.put(cls, func);
        }

        return func.apply(e);
    }

    static final Predicate<String> uncheckedExceptionNameTester = Pattern.compile("Unchecked[a-zA-Z0-9]*Exception").asPredicate();
    static final Map<Class<? extends Throwable>, Class<? extends Throwable>> runtimeToCheckedExceptionClassMap = new ConcurrentHashMap<>();

    /**
     *
     *
     * @param e
     * @return
     */
    public static Exception tryToGetOriginalCheckedException(final Exception e) {
        if (e instanceof RuntimeException && e.getCause() != null && (!(e.getCause() instanceof RuntimeException) && (e.getCause() instanceof Exception))) {
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
            return tryToGetOriginalCheckedException((Exception) e.getCause());
        }

        return e;
    }

    /**
     *
     *
     * @param e
     * @param targetExceptionType
     * @return
     */
    public static boolean hasCause(Throwable e, final Class<? extends Throwable> targetExceptionType) {
        while (e != null) {
            if (targetExceptionType.isAssignableFrom(e.getClass())) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    /**
     *
     *
     * @param e
     * @param targetExceptionTester
     * @return
     */
    public static boolean hasCause(Throwable e, final Predicate<? super Throwable> targetExceptionTester) {
        while (e != null) {
            if (targetExceptionTester.test(e)) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    /**
     *
     *
     * @param e
     * @return
     */
    public static boolean hasSQLCause(Throwable e) {
        while (e != null) {
            if (e instanceof SQLException || UncheckedSQLExceptionClassName.equals(e.getClass().getSimpleName())) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    /**
     *
     *
     * @param e
     * @return
     */
    public static boolean hasIOCause(Throwable e) {
        while (e != null) {
            if (e instanceof IOException || UncheckedIOExceptionClassName.equals(e.getClass().getSimpleName())) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    /**
     *
     * @param e
     * @return
     */
    @Beta
    public static boolean isNullPointerOrIllegalArgumentException(final Throwable e) {
        return e instanceof NullPointerException || e instanceof IllegalArgumentException;
    }

    /**
     *
     *
     * @param e
     * @return
     */
    public static List<Throwable> listCause(Throwable e) {
        final List<Throwable> list = new ArrayList<>();

        while (e != null && !list.contains(e)) {
            list.add(e);
            e = e.getCause();
        }

        return list;
    }

    /**
     * Returns the specified {@code throwable} if there is no cause found in it ({@code throwable.getCause() == null}).
     *
     * @param e
     * @return
     */
    public static Throwable firstCause(final Throwable e) {
        Throwable result = e;

        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param e
     * @param targetExceptionType
     * @return
     */
    public static <E extends Throwable> Optional<E> findCause(Throwable e, final Class<? extends E> targetExceptionType) {
        while (e != null) {
            if (targetExceptionType.isAssignableFrom(e.getClass())) {
                return Optional.of((E) e);
            }

            e = e.getCause();
        }

        return Optional.empty();
    }

    /**
     * @param <E>
     * @param e
     * @param targetExceptionTester
     * @return
     */
    public static <E extends Throwable> Optional<E> findCause(Throwable e, final Predicate<? super Throwable> targetExceptionTester) {
        while (e != null) {
            if (targetExceptionTester.test(e)) {
                return Optional.of((E) e);
            }

            e = e.getCause();
        }

        return Optional.empty();
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Gets the stack trace from a Throwable as a String.</p>
     *
     * <p>The result of this method vary by JDK version as this method
     * uses {@link Throwable#printStackTrace(java.io.PrintWriter)}.
     * On JDK1.3 and earlier, the cause exception will not be shown
     * unless the specified throwable alters printStackTrace.</p>
     *
     * @param e the {@code Throwable} to be examined
     * @return
     *  {@code printStackTrace(PrintWriter)} method
     */
    public static String getStackTrace(final Throwable e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return new StringBuilder(sw.getBuffer().toString()).toString();
    }

    //    /**
    //     * Gets the error msg.
    //     *
    //     * @param e
    //     * @return
    //     * @deprecated replaced by {@link #getErrorMessage(Throwable, true)}
    //     */
    //    @Deprecated
    //    @Internal
    //    public static String getMessage(Throwable e) {
    //        return getErrorMessage(e, true);
    //    }

    /**
     *
     *
     * @param e
     * @return
     */
    public static String getErrorMessage(final Throwable e) {
        return getErrorMessage(e, false);
    }

    /**
     *
     *
     * @param e
     * @param withExceptionClassName
     * @return
     */
    public static String getErrorMessage(final Throwable e, final boolean withExceptionClassName) {
        String msg = e.getMessage();

        if (Strings.isEmpty(msg) && e.getCause() != null) {
            Throwable cause = e.getCause();

            do {
                msg = cause.getMessage();

                if (Strings.isNotEmpty(msg)) {
                    break;
                }
            } while ((cause = e.getCause()) != null);
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
