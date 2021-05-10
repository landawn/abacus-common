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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;

/**
 * Note: This class contains the methods copied from Apache Commons and Google Guava under Apache License v2.
 *
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
        // singleton
    }

    private static final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> toRuntimeExceptionFuncMap = new HashMap<>();

    static {
        toRuntimeExceptionFuncMap.put(RuntimeException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return (RuntimeException) e;
            }
        });

        toRuntimeExceptionFuncMap.put(IOException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return new UncheckedIOException((IOException) e);
            }
        });

        toRuntimeExceptionFuncMap.put(SQLException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return new UncheckedSQLException((SQLException) e);
            }
        });

        toRuntimeExceptionFuncMap.put(ExecutionException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });

        toRuntimeExceptionFuncMap.put(InvocationTargetException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });

        toRuntimeExceptionFuncMap.put(UndeclaredThrowableException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });
    }

    private static final Function<Throwable, RuntimeException> RUNTIME_FUNC = new Function<Throwable, RuntimeException>() {
        @Override
        public RuntimeException apply(Throwable e) {
            return (RuntimeException) e;
        }
    };

    private static final Function<Throwable, RuntimeException> CHECKED_FUNC = new Function<Throwable, RuntimeException>() {
        @Override
        public RuntimeException apply(Throwable e) {
            return new UncheckedException(e);
        }
    };

    private static final String UncheckedSQLExceptionClassName = UncheckedSQLException.class.getSimpleName();

    private static final String UncheckedIOExceptionClassName = UncheckedIOException.class.getSimpleName();

    /**
     *
     * @param <E>
     * @param exceptionClass
     * @param runtimeExceptionMapper
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Throwable> void registerRuntimeExceptionMapper(final Class<E> exceptionClass,
            final Function<E, RuntimeException> runtimeExceptionMapper) {
        if (toRuntimeExceptionFuncMap.containsKey(exceptionClass)) {
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
        }

        return func.apply(e);
    }

    /**
     * Gets the error msg.
     *
     * @param e
     * @return
     */
    public static String getMessage(Throwable e) {
        if (e instanceof SQLException) {
            return e.getClass().getSimpleName() + "|" + ((SQLException) e).getErrorCode() + "|"
                    + (N.isNullOrEmpty(e.getMessage()) ? e.getCause() : e.getMessage());
        } else {
            return e.getClass().getSimpleName() + "|" + (N.isNullOrEmpty(e.getMessage()) ? e.getCause() : e.getMessage());
        }
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

    public static boolean hasCause(Throwable throwable, final Class<? extends Throwable> type) {
        while (throwable != null) {
            if (type.isAssignableFrom(throwable.getClass())) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    public static boolean hasCause(Throwable throwable, final Predicate<? super Throwable> predicate) {
        while (throwable != null) {
            if (predicate.test(throwable)) {
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
            return e.getClass().getCanonicalName();
        } else if (withExceptionClassName) {
            return withExceptionClassName + ": " + msg;
        } else {
            return msg;
        }
    }

    // --------------------------------------------------------------------------------------------->
}
