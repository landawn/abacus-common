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
 * Abstract base implementation of the {@link Logger} interface providing template-based logging methods.
 *
 * <p>This class implements all the template-based logging methods defined in the {@code Logger} interface,
 * delegating to the abstract methods that must be implemented by concrete logger implementations.
 * It provides efficient string formatting using placeholders ({@code {}} or {@code %s}) and lazy evaluation
 * through {@link Supplier}-based methods.</p>
 *
 * <p>The formatting supports two placeholder styles:</p>
 * <ul>
 *   <li>{@code {}} - SLF4J style placeholders</li>
 *   <li>{@code %s} - printf style placeholders</li>
 * </ul>
 *
 * <p><b>Usage Examples for implementation:</b></p>
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
 * @see Logger
 */
public abstract class AbstractLogger implements Logger {

    /**
     * The name of the logger.
     */
    protected final String name;

    /**
     * Constructs an {@code AbstractLogger} with the specified name.
     *
     * @param name the name of the logger
     */
    protected AbstractLogger(final String name) {
        this.name = name;
    }

    /**
     * Returns the name of this logger.
     *
     * @return the name of this logger
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Logs a message at {@code TRACE} level with one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code TRACE} level with two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Processing {} items for user {}", itemCount, userId);
     * }</pre>
     *
     * @param template the message template containing two placeholders
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
     * Logs a message at {@code TRACE} level with three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Method {} called with args: {}, {}", methodName, arg1, arg2);
     * }</pre>
     *
     * @param template the message template containing three placeholders
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
     * Logs a message at {@code TRACE} level with four parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Processing {} items from {} with priority {} at {}", count, source, priority, timestamp);
     * }</pre>
     *
     * @param template the message template containing four placeholders
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
     * Logs a message at {@code TRACE} level with five parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Request {} method {} path {} params {} headers {}", id, method, path, params, headers);
     * }</pre>
     *
     * @param template the message template containing five placeholders
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
     * Logs a message at {@code TRACE} level with six parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Event {} type {} user {} timestamp {} source {} data {}", id, type, user, ts, src, data);
     * }</pre>
     *
     * @param template the message template containing six placeholders
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
     * Logs a message at {@code TRACE} level with seven parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Detailed trace: {} {} {} {} {} {} {}", p1, p2, p3, p4, p5, p6, p7);
     * }</pre>
     *
     * @param template the message template containing seven placeholders
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
     * Logs a message at {@code TRACE} level with variable number of parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Processing batch: id={}, size={}, status={}, timestamp={}",
     *              batchId, size, status, timestamp);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     * @deprecated Prefer {@link #trace(Supplier)} for lazy evaluation to avoid object creation
     */
    @Deprecated
    @Override
    public void trace(final String template, final Object... args) {
        if (isTraceEnabled()) {
            trace(format(template, args));
        }
    }

    /**
     * Logs an exception at the {@code TRACE} level with an accompanying message.
     *
     * <p>This method provides an alternative parameter order for consistency with other exception-handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(exception, "Failed to load resource");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param msg the message to log
     */
    @Override
    public void trace(final Throwable t, final String msg) {
        trace(msg, t);
    }

    /**
     * Logs a formatted message at {@code TRACE} level with an exception and one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(exception, "Failed to load resource: {}", resourceName);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code TRACE} level with an exception and two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(exception, "Operation {} failed for item {}", operation, itemId);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code TRACE} level with an exception and three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(exception, "Error in {} at position {} for value {}", method, position, value);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a message at {@code TRACE} level using a {@link Supplier} for lazy evaluation.
     *
     * <p>The supplier is only called if {@code TRACE} level is enabled.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code TRACE} level with an exception using a {@link Supplier}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(() -> "Failed to process data", exception);
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     * @param t the exception or error to log
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
     * Logs a message at {@code TRACE} level with an exception using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace(exception, () -> "Failed to process data");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void trace(final Throwable t, final Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     * Logs a message at {@code DEBUG} level with one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code DEBUG} level with two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("User {} accessed resource {} ", username, resourceId);
     * }</pre>
     *
     * @param template the message template containing two placeholders
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
     * Logs a message at {@code DEBUG} level with three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("Query {} returned {} results in {}ms", queryId, resultCount, executionTime);
     * }</pre>
     *
     * @param template the message template containing three placeholders
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
     * Logs a message at {@code DEBUG} level with four parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("Connection {} to {} established with timeout {}ms and pool size {}",
     *              connId, host, timeout, poolSize);
     * }</pre>
     *
     * @param template the message template containing four placeholders
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
     * Logs a message at {@code DEBUG} level with five parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("Query {} executed on {} with params {} in {}ms returning {} rows",
     *              queryId, database, params, duration, rowCount);
     * }</pre>
     *
     * @param template the message template containing five placeholders
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
     * Logs a message at {@code DEBUG} level with six parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("HTTP {} {} from {} returned {} in {}ms with size {} bytes",
     *              method, path, clientIp, statusCode, duration, responseSize);
     * }</pre>
     *
     * @param template the message template containing six placeholders
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
     * Logs a message at {@code DEBUG} level with seven parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("API call {} to {} with params {} auth {} returned {} in {}ms status {}",
     *              callId, endpoint, params, authType, response, duration, status);
     * }</pre>
     *
     * @param template the message template containing seven placeholders
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
     * Logs a message at {@code DEBUG} level with variable number of parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("Processing records: id={}, name={}, type={}", id, name, type);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     * @deprecated Prefer {@link #debug(Supplier)} for lazy evaluation to avoid object creation
     */
    @Deprecated
    @Override
    public void debug(final String template, final Object... args) {
        if (isDebugEnabled()) {
            debug(format(template, args));
        }
    }

    /**
     * Logs an exception at the {@code DEBUG} level with an accompanying message.
     *
     * <p>This method provides an alternative parameter order for consistency with other exception-handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(exception, "Failed to connect to database");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param msg the message accompanying the exception
     */
    @Override
    public void debug(final Throwable t, final String msg) {
        debug(msg, t);
    }

    /**
     * Logs a formatted message at {@code DEBUG} level with an exception and one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(exception, "Failed to parse config file: {}", filename);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code DEBUG} level with an exception and two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(exception, "Connection to {} failed on port {}", host, port);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code DEBUG} level with an exception and three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(exception, "Failed to execute {} on {} with params {}", operation, target, params);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a message at {@code DEBUG} level using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(() -> "Complex object state: " + complexObject.debugString());
     * }</pre>
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
     * Logs a message at {@code DEBUG} level with an exception using a {@link Supplier}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(() -> "Failed to process data", exception);
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     * @param t the exception or error to log
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
     * Logs a message at {@code DEBUG} level with an exception using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug(exception, () -> "Failed to process data");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void debug(final Throwable t, final Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     * Logs a message at {@code INFO} level with one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code INFO} level with two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Processed {} records in {}ms", recordCount, duration);
     * }</pre>
     *
     * @param template the message template containing two placeholders
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
     * Logs a message at {@code INFO} level with three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Server {} started on port {} with protocol {}", serverName, port, protocol);
     * }</pre>
     *
     * @param template the message template containing three placeholders
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
     * Logs a message at {@code INFO} level with four parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Service {} started on {} with {} threads and {} MB memory",
     *             serviceName, host, threadCount, memoryMB);
     * }</pre>
     *
     * @param template the message template containing four placeholders
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
     * Logs a message at {@code INFO} level with five parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Batch {} completed: {} items processed, {} succeeded, {} failed in {}ms",
     *             batchId, totalItems, successCount, failCount, duration);
     * }</pre>
     *
     * @param template the message template containing five placeholders
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
     * Logs a message at {@code INFO} level with six parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Deploy {} to {} environment: {} instances, {} version, status {} at {}",
     *             appName, envName, instanceCount, version, status, timestamp);
     * }</pre>
     *
     * @param template the message template containing six placeholders
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
     * Logs a message at {@code INFO} level with seven parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Migration {} from {} to {}: {} tables, {} rows, {} errors, completed in {}s",
     *             migrationId, sourceDb, targetDb, tableCount, rowCount, errorCount, duration);
     * }</pre>
     *
     * @param template the message template containing seven placeholders
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
     * Logs a message at {@code INFO} level with variable number of parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Processing batch: id={}, size={}, status={}, timestamp={}",
     *              batchId, size, status, timestamp);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     * @deprecated Prefer {@link #info(Supplier)} for lazy evaluation to avoid object creation
     */
    @Deprecated
    @Override
    public void info(final String template, final Object... args) {
        if (isInfoEnabled()) {
            info(format(template, args));
        }
    }

    /**
     * Logs an exception at the {@code INFO} level with an accompanying message.
     *
     * <p>This method provides an alternative parameter order for consistency with other exception-handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(exception, "Application started");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param msg the message accompanying the exception
     */
    @Override
    public void info(final Throwable t, final String msg) {
        info(msg, t);
    }

    /**
     * Logs a formatted message at {@code INFO} level with an exception and one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(exception, "Using fallback configuration for {}", componentName);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code INFO} level with an exception and two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(exception, "Retrying operation {} after {} seconds", operationName, retryDelay);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code INFO} level with an exception and three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(exception, "Switched to backup {} from {} with latency {}ms", backup, primary, latency);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a message at {@code INFO} level using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(() -> "Processed " + getProcessedCount() + " records");
     * }</pre>
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
     * Logs a message at {@code INFO} level with an exception using a {@link Supplier}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(() -> "Application status", exception);
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     * @param t the exception or error to log
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
     * Logs a message at {@code INFO} level with an exception using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info(exception, () -> "Application status");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void info(final Throwable t, final Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     * Logs a message at {@code WARN} level with one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code WARN} level with two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Cache size {} exceeds limit of {}", cacheSize, maxSize);
     * }</pre>
     *
     * @param template the message template containing two placeholders
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
     * Logs a message at {@code WARN} level with three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Retry {} of {} failed for operation {}", attemptNum, maxAttempts, operationId);
     * }</pre>
     *
     * @param template the message template containing three placeholders
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
     * Logs a message at {@code WARN} level with four parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Resource {} usage at {}% exceeds threshold of {}% for {}",
     *             resourceName, currentUsage, threshold, duration);
     * }</pre>
     *
     * @param template the message template containing four placeholders
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
     * Logs a message at {@code WARN} level with five parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Queue {} size {} exceeds limit {}, {} messages dropped in {}s",
     *             queueName, currentSize, maxSize, droppedCount, duration);
     * }</pre>
     *
     * @param template the message template containing five placeholders
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
     * Logs a message at {@code WARN} level with six parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Session {} for user {} on {} idle for {}min, will timeout in {}min, limit {}",
     *             sessionId, userId, host, idleTime, timeoutRemaining, maxIdleTime);
     * }</pre>
     *
     * @param template the message template containing six placeholders
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
     * Logs a message at {@code WARN} level with seven parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Cache {} on {} hit ratio {}% below {}%, size {}, evictions {}, age {}min",
     *             cacheName, node, hitRatio, threshold, size, evictions, age);
     * }</pre>
     *
     * @param template the message template containing seven placeholders
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
     * Logs a message at {@code WARN} level with variable number of parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Processing batch: id={}, size={}, status={}, timestamp={}",
     *              batchId, size, status, timestamp);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     * @deprecated Prefer {@link #warn(Supplier)} for lazy evaluation to avoid object creation
     */
    @Deprecated
    @Override
    public void warn(final String template, final Object... args) {
        if (isWarnEnabled()) {
            warn(format(template, args));
        }
    }

    /**
     * Logs an exception at the {@code WARN} level with an accompanying message.
     *
     * <p>This method provides an alternative parameter order for consistency with other exception-handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(exception, "Database connection warning");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param msg the message accompanying the exception
     */
    @Override
    public void warn(final Throwable t, final String msg) {
        warn(msg, t);
    }

    /**
     * Logs a formatted message at {@code WARN} level with an exception and one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(exception, "Deprecated API used: {}", apiName);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code WARN} level with an exception and two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(exception, "Retry attempt {} failed for {}", attemptNumber, operationName);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code WARN} level with an exception and three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(exception, "Timeout accessing {} on {} after {}ms", resource, server, timeout);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a message at {@code WARN} level using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(() -> "Memory usage " + getMemoryUsage() + " exceeds threshold");
     * }</pre>
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
     * Logs a message at {@code WARN} level with an exception using a {@link Supplier}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(() -> "Resource usage warning", exception);
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     * @param t the exception or error to log
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
     * Logs a message at {@code WARN} level with an exception using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn(exception, () -> "Resource usage warning");
     * }</pre>
     *
     * @param t the exception or error to log
     * @param supplier the supplier that provides the message
     */
    @Override
    public void warn(final Throwable t, final Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     * Logs a message at {@code ERROR} level with one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Logs a message at {@code ERROR} level with two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Failed to connect to {} on port {}", hostname, port);
     * }</pre>
     *
     * @param template the message template containing two placeholders
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
     * Logs a message at {@code ERROR} level with three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Transaction {} failed for user {} with error code {}", txnId, userId, errorCode);
     * }</pre>
     *
     * @param template the message template containing three placeholders
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
     * Logs a message at {@code ERROR} level with four parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Critical failure in {} module: {} errors in {} records after {}s",
     *              moduleName, errorCount, recordCount, duration);
     * }</pre>
     *
     * @param template the message template containing four placeholders
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
     * Logs a message at {@code ERROR} level with five parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Database {} on {} unavailable: {} connections failed, {} transactions lost in {}s",
     *              dbName, hostname, failedConns, lostTransactions, duration);
     * }</pre>
     *
     * @param template the message template containing five placeholders
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
     * Logs a message at {@code ERROR} level with six parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Payment {} for user {} failed: amount {}, currency {}, gateway {}, code {}",
     *              paymentId, userId, amount, currency, gateway, errorCode);
     * }</pre>
     *
     * @param template the message template containing six placeholders
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
     * Logs a message at {@code ERROR} level with seven parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("System failure: component {}, host {}, error {}, affected {}, started {}, duration {}s, impact {}",
     *              component, host, errorType, affectedUsers, startTime, duration, impactLevel);
     * }</pre>
     *
     * @param template the message template containing seven placeholders
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
     * Logs a message at {@code ERROR} level with variable number of parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Processing batch: id={}, size={}, status={}, timestamp={}",
     *              batchId, size, status, timestamp);
     * }</pre>
     *
     * @param template the message template
     * @param args the arguments to be substituted in the template
     * @deprecated Prefer {@link #error(Supplier)} for lazy evaluation to avoid object creation
     */
    @Deprecated
    @Override
    public void error(final String template, final Object... args) {
        if (isErrorEnabled()) {
            error(format(template, args));
        }
    }

    /**
     * Logs an exception at the {@code ERROR} level with an accompanying message.
     *
     * <p>This method provides an alternative parameter order for consistency with other exception-handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // some operation
     * } catch (Exception e) {
     *     logger.error(e, "Operation failed");
     * }
     * }</pre>
     *
     * @param t the exception or error to log
     * @param msg the message accompanying the exception
     */
    @Override
    public void error(final Throwable t, final String msg) {
        error(msg, t);
    }

    /**
     * Logs a formatted message at {@code ERROR} level with an exception and one parameter.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(exception, "Fatal error processing transaction {}", transactionId);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code ERROR} level with an exception and two parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(exception, "Service {} crashed after {} requests", serviceName, requestCount);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a formatted message at {@code ERROR} level with an exception and three parameters.
     *
     * <p>The message template can use {@code {}} or {@code %s} as placeholders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(exception, "Data corruption in {} table: {} records affected at {}", tableName, count, timestamp);
     * }</pre>
     *
     * @param t the exception or error to log
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
     * Logs a message at {@code ERROR} level using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(() -> "Critical error in module: " + module.getDetailedStatus());
     * }</pre>
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
     * Logs a message at {@code ERROR} level with an exception using a {@link Supplier}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(() -> "Critical system error", exception);
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     * @param t the exception or error to log
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
     * Logs a message at {@code ERROR} level with an exception using a {@link Supplier} for lazy evaluation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error(exception, () -> "Critical system error");
     * }</pre>
     *
     * @param t the exception or error to log
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
     * <p>Supports {@code {}} (SLF4J-style) or {@code %s} (printf-style) placeholders.
     * If the template contains {@code {}}, that style is used; otherwise {@code %s} is tried.
     * The two styles cannot be mixed in a single template.
     * If no placeholder is found, the argument is appended in square brackets.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String msg = format("User {} logged in", "john");
     * // Output: "User john logged in"
     * String msg2 = format("Count: %s", 42);
     * // Output: "Count: 42"
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to substitute
     * @return the formatted message
     */
    static String format(String template, final Object arg) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '{}' or '%s' placeholders
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
            sb.append(template, placeholderStart + placeholder.length(), template.length());
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
     * <p>Supports {@code {}} (SLF4J-style) or {@code %s} (printf-style) placeholders.
     * If the template contains {@code {}}, that style is used; otherwise {@code %s} is tried.
     * The two styles cannot be mixed in a single template.
     * Extra arguments are appended in square brackets if there are fewer placeholders than arguments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String msg = format("User {} logged in from {}", "john", "192.168.1.1");
     * // Output: "User john logged in from 192.168.1.1"
     * String msg2 = format("Only one placeholder: {}", "arg1", "arg2");
     * // Output: "Only one placeholder: arg1 [arg2]"
     * }</pre>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '{}' or '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 32);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int substitutedArgsCount = 0;

        if (placeholderStart >= 0) {
            substitutedArgsCount++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + placeholder.length();
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                substitutedArgsCount++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + placeholder.length();
            }

            sb.append(template, templateStart, template.length());
        }

        if (substitutedArgsCount == 0) {
            sb.append(template);
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(']');
        } else if (substitutedArgsCount == 1) {
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
     * <p>Supports {@code {}} (SLF4J-style) or {@code %s} (printf-style) placeholders.
     * If the template contains {@code {}}, that style is used; otherwise {@code %s} is tried.
     * The two styles cannot be mixed in a single template.
     * Extra arguments are appended in square brackets if there are fewer placeholders than arguments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String msg = format("User {} performed {} operations in {}ms",
     *                     "alice", 10, 250);
     * // Output: "User alice performed 10 operations in 250ms"
     * String msg2 = format("Result: {}", "success", "extra", "args");
     * // Output: "Result: success [extra, args]"
     * }</pre>
     *
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @return the formatted message
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '{}' or '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 48);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int substitutedArgsCount = 0;

        if (placeholderStart >= 0) {
            substitutedArgsCount++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + placeholder.length();
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                substitutedArgsCount++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + placeholder.length();
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    substitutedArgsCount++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + placeholder.length();
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (substitutedArgsCount == 0) {
            sb.append(template);
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (substitutedArgsCount == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (substitutedArgsCount == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Formats a message template with variable number of arguments.
     *
     * <p>Substitutes each {@code {}} or {@code %s} in the template with an argument. These are matched by
     * position: the first placeholder gets {@code args[0]}, etc. If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted
     * message in square brackets.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String msg = format("User {} performed {} operations in {}ms",
     *                     username, opCount, duration);
     * }</pre>
     *
     * @param template the message template containing 0 or more {@code {}} or {@code %s} placeholders, may be {@code null} (converted to "null")
     * @param args the arguments to be substituted into the message template. Arguments
     *     are converted to strings using {@code N.toString(Object)}. Arguments can be {@code null}.
     * @return the formatted message
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, final Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (N.isEmpty(args)) {
            return template;
        }

        // start substituting the arguments into the '{}' or '%s' placeholders
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
            templateStart = placeholderStart + placeholder.length();
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
