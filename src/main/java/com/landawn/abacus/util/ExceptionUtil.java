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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;

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

        toRuntimeExceptionFuncMap.put(IOException.class, e -> new UncheckedIOException((IOException) e));

        toRuntimeExceptionFuncMap.put(SQLException.class, e -> new UncheckedSQLException((SQLException) e));

        toRuntimeExceptionFuncMap.put(ExecutionException.class, e -> e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause()));

        toRuntimeExceptionFuncMap.put(InvocationTargetException.class,
                e -> e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause()));

        toRuntimeExceptionFuncMap.put(UndeclaredThrowableException.class,
                e -> e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause()));
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
     * @param <E>
     * @param exceptionClass
     * @param runtimeExceptionMapper
     * @param force
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Throwable> void registerRuntimeExceptionMapper(final Class<E> exceptionClass,
            final Function<E, RuntimeException> runtimeExceptionMapper, final boolean force) {
        N.checkArgNotNull(exceptionClass, "exceptionClass");
        N.checkArgNotNull(runtimeExceptionMapper, "runtimeExceptionMapper");

        if (!force && toRuntimeExceptionFuncMap.containsKey(exceptionClass)) {
            throw new IllegalArgumentException("Exception class: " + ClassUtil.getCanonicalClassName(exceptionClass) + " has already been registered");
        }

        toRuntimeExceptionFuncMap.put((Class) exceptionClass, (Function) runtimeExceptionMapper);
    }

    /**
     * To runtime exception.
     *
     * @param e
     * @return
     * @see #registerRuntimeExceptionMapper(Class, Function)
     */
    public static RuntimeException toRuntimeException(Throwable e) {
        final Class<Throwable> cls = (Class<Throwable>) e.getClass();
        Function<Throwable, RuntimeException> func = toRuntimeExceptionFuncMap.get(cls);

        if (func == null) {
            for (Class<?> key : toRuntimeExceptionFuncMap.keySet()) {
                if (key.isAssignableFrom(cls)) {
                    func = toRuntimeExceptionFuncMap.get(key);
                    break;
                }
            }

            if (func == null) {
                if (e instanceof RuntimeException) {
                    func = RUNTIME_FUNC;
                } else {
                    func = CHECKED_FUNC;
                }
            }

            toRuntimeExceptionFuncMap.put(cls, func);
        }

        return func.apply(e);
    }

    static final java.util.function.Predicate<String> uncheckedExceptionNameTester = Pattern.compile("Unchecked[a-zA-Z0-9]*Exception").asPredicate();
    static final Map<Class<? extends Throwable>, Class<? extends Throwable>> runtimeToCheckedExceptionClassMap = new ConcurrentHashMap<>();

    public static Exception tryToGetOriginalCheckedException(final Exception e) {
        if (e instanceof RuntimeException && e.getCause() != null && (!(e.getCause() instanceof RuntimeException) && (e.getCause() instanceof Exception))) {
            if (e instanceof UncheckedException //
                    || (uncheckedExceptionNameTester.test(ClassUtil.getSimpleClassName(e.getClass())))) {
                return (Exception) e.getCause();
            }

            final Throwable cause = e.getCause();

            if (runtimeToCheckedExceptionClassMap.containsKey(e.getClass()) && runtimeToCheckedExceptionClassMap.get(e.getClass()).equals(cause.getClass())) {
                return (Exception) cause;
            }

            if (toRuntimeExceptionFuncMap.containsKey(cause.getClass())
                    && toRuntimeExceptionFuncMap.get(cause.getClass()).apply(cause).getClass().equals(e.getClass())) {

                runtimeToCheckedExceptionClassMap.put(e.getClass(), cause.getClass());

                return (Exception) cause;
            }
        } else if (e.getCause() != null && e.getCause() instanceof Exception && (e instanceof InvocationTargetException || e instanceof ExecutionException)) {
            return tryToGetOriginalCheckedException((Exception) e.getCause());
        }

        return e;
    }

    public static boolean hasCause(Throwable throwable, final Class<? extends Throwable> targetExceptionType) {
        while (throwable != null) {
            if (targetExceptionType.isAssignableFrom(throwable.getClass())) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    public static boolean hasCause(Throwable throwable, final Predicate<? super Throwable> targetExceptionTester) {
        while (throwable != null) {
            if (targetExceptionTester.test(throwable)) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    public static boolean hasSQLCause(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof SQLException || UncheckedSQLExceptionClassName.equals(throwable.getClass().getSimpleName())) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    public static boolean hasIOCause(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof IOException || UncheckedIOExceptionClassName.equals(throwable.getClass().getSimpleName())) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    public static List<Throwable> listCause(Throwable throwable) {
        final List<Throwable> list = new ArrayList<>();

        while (throwable != null && !list.contains(throwable)) {
            list.add(throwable);
            throwable = throwable.getCause();
        }

        return list;
    }

    /**
     * Returns the specified {@code throwable} if there is no cause found in it ({@code throwable.getCause() == null}).
     *
     * @param throwable
     * @return
     */
    public static Throwable firstCause(final Throwable throwable) {
        Throwable result = throwable;

        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
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
     * @param throwable the <code>Throwable</code> to be examined
     * @return
     *  <code>printStackTrace(PrintWriter)</code> method
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Gets the error msg.
     *
     * @param e
     * @return
     * @deprecated replaced by {@link #getErrorMessage(Throwable, true)}
     */
    @Deprecated
    @Internal
    public static String getMessage(Throwable e) {
        return getErrorMessage(e, true);
    }

    public static String getErrorMessage(final Throwable e) {
        return getErrorMessage(e, false);
    }

    public static String getErrorMessage(final Throwable e, final boolean withExceptionClassName) {
        String msg = e.getMessage();

        if (N.isNullOrEmpty(msg) && e.getCause() != null) {
            Throwable cause = e.getCause();

            do {
                msg = cause.getMessage();

                if (N.notNullOrEmpty(msg)) {
                    break;
                }
            } while ((cause = e.getCause()) != null);
        }

        if (N.isNullOrEmpty(msg)) {
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
