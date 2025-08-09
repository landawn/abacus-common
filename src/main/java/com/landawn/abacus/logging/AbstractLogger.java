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

package com.landawn.abacus.logging;

import java.util.function.Supplier;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * Abstract base implementation of the Logger interface providing template-based logging methods.
 * 
 * <p>This class implements all the template-based logging methods defined in the Logger interface,
 * delegating to the abstract methods that must be implemented by concrete logger implementations.
 * It provides efficient string formatting using placeholders ({} or %s) and lazy evaluation
 * through Supplier-based methods.</p>
 * 
 * <p>The formatting supports two placeholder styles:</p>
 * <ul>
 *   <li>{} - SLF4J style placeholders</li>
 *   <li>%s - printf style placeholders</li>
 * </ul>
 * 
 * <p>Implementation example:</p>
 * <pre>{@code
 * public class MyLogger extends AbstractLogger {
 *     public MyLogger(String name) {
 *         super(name);
 *     }
 *     
 *     @Override
 *     public void info(String msg) {
 *         // Implementation specific logging
 *     }
 *     // ... implement other abstract methods
 * }
 * }</pre>
 * 
 * @author HaiYang Li
 * @since 1.0
 */
public abstract class AbstractLogger implements Logger {

    protected final String name;

    /**
     * Constructs an AbstractLogger with the specified name.
     * 
     * @param name the name of the logger
     */
    protected AbstractLogger(final String name) {
        this.name = name;
    }

    /**
     * Gets the name of this logger.
     *
     * @return the logger name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Logs a message at TRACE level with one parameter.
     * 
     * <p>The message template can use {} or %s as placeholder.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.trace("User {} logged in", username);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void trace(final String template, final Object arg) {
        if (isTraceEnabled()) {
            trace(format(template, arg));
        }
    }

    /**
     * Logs a message at TRACE level with two parameters.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.trace("Processing {} items for user {}", itemCount, userId);
     * }</pre>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2));
        }
    }

    /**
     * Logs a message at TRACE level with three parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at TRACE level with four parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a message at TRACE level with five parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

    /**
     * Logs a message at TRACE level with six parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

    /**
     * Logs a message at TRACE level with seven parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     * Logs a message at TRACE level with variable number of parameters.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.trace("Processing batch: id={}, size={}, status={}, timestamp={}", 
     *              batchId, size, status, timestamp);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     */
    @Override
    public void trace(final String template, final Object... args) {
        if (isTraceEnabled()) {
            trace(format(template, args));
        }
    }

    /**
     * Logs a message at TRACE level with an exception.
     * 
     * <p>This method is a convenience wrapper that delegates to trace(String, Throwable).</p>
     *
     * @param t the exception to log
     * @param msg the message to log
     */
    @Override
    public void trace(final Throwable t, final String msg) {
        trace(msg, t);
    }

    /**
     * Logs a formatted message at TRACE level with an exception and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void trace(final Throwable t, final String template, final Object arg) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg));
        }
    }

    /**
     * Logs a formatted message at TRACE level with an exception and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void trace(final Throwable t, final String template, final Object arg1, final Object arg2) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2));
        }
    }

    /**
     * Logs a formatted message at TRACE level with an exception and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void trace(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at TRACE level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only called if TRACE level is enabled.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.trace(() -> "Expensive calculation result: " + calculateExpensiveValue());
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    @Override
    public void trace(final Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(supplier.get());
        }
    }

    /**
     * Logs a message at TRACE level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #trace(Throwable, Supplier)} instead
     */
    @Deprecated
    @Override
    public void trace(final Supplier<String> supplier, final Throwable t) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     * Logs a message at TRACE level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void trace(final Throwable t, final Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     * Logs a message at DEBUG level with one parameter.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.debug("Loading configuration from: {}", configPath);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void debug(final String template, final Object arg) {
        if (isDebugEnabled()) {
            debug(format(template, arg));
        }
    }

    /**
     * Logs a message at DEBUG level with two parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2));
        }
    }

    /**
     * Logs a message at DEBUG level with three parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at DEBUG level with four parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a message at DEBUG level with five parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

    /**
     * Logs a message at DEBUG level with six parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

    /**
     * Logs a message at DEBUG level with seven parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     * Logs a message at DEBUG level with variable number of parameters.
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     */
    @Override
    public void debug(final String template, final Object... args) {
        if (isDebugEnabled()) {
            debug(format(template, args));
        }
    }

    /**
     * Logs a message at DEBUG level with an exception.
     *
     * @param t the exception to log
     * @param msg the message to log
     */
    @Override
    public void debug(final Throwable t, final String msg) {
        debug(msg, t);
    }

    /**
     * Logs a formatted message at DEBUG level with an exception and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void debug(final Throwable t, final String template, final Object arg) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg));
        }
    }

    /**
     * Logs a formatted message at DEBUG level with an exception and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void debug(final Throwable t, final String template, final Object arg1, final Object arg2) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2));
        }
    }

    /**
     * Logs a formatted message at DEBUG level with an exception and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void debug(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at DEBUG level using a supplier for lazy evaluation.
     *
     * @param supplier the supplier that provides the message
     */
    @Override
    public void debug(final Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(supplier.get());
        }
    }

    /**
     * Logs a message at DEBUG level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #debug(Throwable, Supplier)} instead
     */
    @Deprecated
    @Override
    public void debug(final Supplier<String> supplier, final Throwable t) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     * Logs a message at DEBUG level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void debug(final Throwable t, final Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     * Logs a message at INFO level with one parameter.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.info("Application started on port {}", port);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void info(final String template, final Object arg) {
        if (isInfoEnabled()) {
            info(format(template, arg));
        }
    }

    /**
     * Logs a message at INFO level with two parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2));
        }
    }

    /**
     * Logs a message at INFO level with three parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at INFO level with four parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a message at INFO level with five parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

    /**
     * Logs a message at INFO level with six parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

    /**
     * Logs a message at INFO level with seven parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     * Logs a message at INFO level with variable number of parameters.
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     */
    @Override
    public void info(final String template, final Object... args) {
        if (isInfoEnabled()) {
            info(format(template, args));
        }
    }

    /**
     * Logs a message at INFO level with an exception.
     *
     * @param t the exception to log
     * @param msg the message to log
     */
    @Override
    public void info(final Throwable t, final String msg) {
        info(msg, t);
    }

    /**
     * Logs a formatted message at INFO level with an exception and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void info(final Throwable t, final String template, final Object arg) {
        if (isInfoEnabled()) {
            info(t, format(template, arg));
        }
    }

    /**
     * Logs a formatted message at INFO level with an exception and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void info(final Throwable t, final String template, final Object arg1, final Object arg2) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2));
        }
    }

    /**
     * Logs a formatted message at INFO level with an exception and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void info(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at INFO level using a supplier for lazy evaluation.
     *
     * @param supplier the supplier that provides the message
     */
    @Override
    public void info(final Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(supplier.get());
        }
    }

    /**
     * Logs a message at INFO level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #info(Throwable, Supplier)} instead
     */
    @Deprecated
    @Override
    public void info(final Supplier<String> supplier, final Throwable t) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     * Logs a message at INFO level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void info(final Throwable t, final Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     * Logs a message at WARN level with one parameter.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.warn("Connection pool exhausted, size: {}", poolSize);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void warn(final String template, final Object arg) {
        if (isWarnEnabled()) {
            warn(format(template, arg));
        }
    }

    /**
     * Logs a message at WARN level with two parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2));
        }
    }

    /**
     * Logs a message at WARN level with three parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at WARN level with four parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a message at WARN level with five parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

    /**
     * Logs a message at WARN level with six parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

    /**
     * Logs a message at WARN level with seven parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     * Logs a message at WARN level with variable number of parameters.
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     */
    @Override
    public void warn(final String template, final Object... args) {
        if (isWarnEnabled()) {
            warn(format(template, args));
        }
    }

    /**
     * Logs a message at WARN level with an exception.
     *
     * @param t the exception to log
     * @param msg the message to log
     */
    @Override
    public void warn(final Throwable t, final String msg) {
        warn(msg, t);
    }

    /**
     * Logs a formatted message at WARN level with an exception and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void warn(final Throwable t, final String template, final Object arg) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg));
        }
    }

    /**
     * Logs a formatted message at WARN level with an exception and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void warn(final Throwable t, final String template, final Object arg1, final Object arg2) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2));
        }
    }

    /**
     * Logs a formatted message at WARN level with an exception and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void warn(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at WARN level using a supplier for lazy evaluation.
     *
     * @param supplier the supplier that provides the message
     */
    @Override
    public void warn(final Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(supplier.get());
        }
    }

    /**
     * Logs a message at WARN level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #warn(Throwable, Supplier)} instead
     */
    @Deprecated
    @Override
    public void warn(final Supplier<String> supplier, final Throwable t) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     * Logs a message at WARN level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void warn(final Throwable t, final Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     * Logs a message at ERROR level with one parameter.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * logger.error("Failed to connect to database: {}", dbUrl);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void error(final String template, final Object arg) {
        if (isErrorEnabled()) {
            error(format(template, arg));
        }
    }

    /**
     * Logs a message at ERROR level with two parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2));
        }
    }

    /**
     * Logs a message at ERROR level with three parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at ERROR level with four parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a message at ERROR level with five parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

    /**
     * Logs a message at ERROR level with six parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

    /**
     * Logs a message at ERROR level with seven parameters.
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     * Logs a message at ERROR level with variable number of parameters.
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     */
    @Override
    public void error(final String template, final Object... args) {
        if (isErrorEnabled()) {
            error(format(template, args));
        }
    }

    /**
     * Logs a message at ERROR level with an exception.
     *
     * @param t the exception to log
     * @param msg the message to log
     */
    @Override
    public void error(final Throwable t, final String msg) {
        error(msg, t);
    }

    /**
     * Logs a formatted message at ERROR level with an exception and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    @Override
    public void error(final Throwable t, final String template, final Object arg) {
        if (isErrorEnabled()) {
            error(t, format(template, arg));
        }
    }

    /**
     * Logs a formatted message at ERROR level with an exception and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    @Override
    public void error(final Throwable t, final String template, final Object arg1, final Object arg2) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2));
        }
    }

    /**
     * Logs a formatted message at ERROR level with an exception and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    @Override
    public void error(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a message at ERROR level using a supplier for lazy evaluation.
     *
     * @param supplier the supplier that provides the message
     */
    @Override
    public void error(final Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(supplier.get());
        }
    }

    /**
     * Logs a message at ERROR level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #error(Throwable, Supplier)} instead
     */
    @Deprecated
    @Override
    public void error(final Supplier<String> supplier, final Throwable t) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    /**
     * Logs a message at ERROR level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void error(final Throwable t, final Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    /**
     * Formats a message template with one argument.
     * 
     * <p>Supports both {} and %s placeholders. If no placeholder is found,
     * the argument is appended in square brackets.</p>
     *
     * @param template the message template
     * @param arg the argument to substitute
     * @return the formatted message
     */
    static String format(String template, final Object arg) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        if (placeholderStart >= 0) {
            sb.append(template, 0, placeholderStart);
            sb.append(N.toString(arg));
            sb.append(template, placeholderStart + 2, template.length());
        } else {
            sb.append(template);
            sb.append(" [");
            sb.append(N.toString(arg));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with two arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 32);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with three arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 48);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with four arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 64);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with five arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 80);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with six arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 96);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                            placeholderStart = template.indexOf(placeholder, templateStart);

                            if (placeholderStart >= 0) {
                                cnt++;
                                sb.append(template, templateStart, placeholderStart);
                                sb.append(N.toString(arg6));
                                templateStart = placeholderStart + 2;
                            }
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 5) {
            sb.append(" [");
            sb.append(N.toString(arg6));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with seven arguments.
     * 
     * <p>Supports both {} and %s placeholders. Extra arguments are appended
     * in square brackets if there are fewer placeholders than arguments.</p>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 112);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                            placeholderStart = template.indexOf(placeholder, templateStart);

                            if (placeholderStart >= 0) {
                                cnt++;
                                sb.append(template, templateStart, placeholderStart);
                                sb.append(N.toString(arg6));
                                templateStart = placeholderStart + 2;
                                placeholderStart = template.indexOf(placeholder, templateStart);

                                if (placeholderStart >= 0) {
                                    cnt++;
                                    sb.append(template, templateStart, placeholderStart);
                                    sb.append(N.toString(arg7));
                                    templateStart = placeholderStart + 2;
                                }
                            }
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 5) {
            sb.append(" [");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 6) {
            sb.append(" [");
            sb.append(N.toString(arg7));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with variable number of arguments.
     * 
     * <p>Substitutes each {} or %s in the template with an argument. These are matched by
     * position: the first placeholder gets args[0], etc. If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted
     * message in square brackets.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * String msg = format("User {} performed {} operations in {}ms", 
     *                     username, opCount, duration);
     * }</pre>
     *
     * @param template a non-null string containing 0 or more {} or %s placeholders
     * @param args the arguments to be substituted into the message template. Arguments
     *     are converted to strings using {@link String#valueOf(Object)}. Arguments can be null.
     * @return the formatted message
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, final Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (N.isEmpty(args)) {
            return template;
        }

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        while (placeholderStart >= 0 && i < args.length) {
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(args[i++]));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);
        }

        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(" [");
            sb.append(N.toString(args[i++]));
            while (i < args.length) {
                sb.append(", ");
                sb.append(N.toString(args[i++]));
            }
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

}
