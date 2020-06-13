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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.function.Function;

/**
 * Note: This class contains the methods copied from Apache Commons and Google Guava under Apache License v2.
 *
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
        // singleton
    }

    /** The Constant toRuntimeExceptionFuncMap. */
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

    /**
     * To runtime exception.
     *
     * @param e
     * @return
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
            return e.getClass().getSimpleName() + "|" + ((SQLException) e).getErrorCode() + "|" + ((e.getMessage() == null) ? e.getCause() : e.getMessage());
        } else {
            return e.getClass().getSimpleName() + "|" + ((e.getMessage() == null) ? e.getCause() : e.getMessage());
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

    /**
     * Does the throwable's causal chain have an immediate or wrapped exception
     * of the given type?
     *
     * @param chain
     *            The root of a Throwable causal chain.
     * @param type
     *            The exception type to test.
     * @return true, if chain is an instance of type or is an
     *         UndeclaredThrowableException wrapping a cause.
     * @since 3.5
     * @see #wrapAndThrow(Throwable)
     */
    public static boolean hasCause(Throwable chain, final Class<? extends Throwable> type) {
        if (chain instanceof UndeclaredThrowableException) {
            chain = chain.getCause();
        }

        return type.isInstance(chain);
    }

    // --------------------------------------------------------------------------------------------->
}
