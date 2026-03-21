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

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Logger implementation that delegates to Java's built-in logging framework ({@code java.util.logging}).
 *
 * <p>This implementation maps the {@link Logger} interface methods to JDK logging levels as follows:</p>
 * <ul>
 *   <li>{@code TRACE} &rarr; {@code FINEST}</li>
 *   <li>{@code DEBUG} &rarr; {@code FINE}</li>
 *   <li>{@code INFO} &rarr; {@code INFO}</li>
 *   <li>{@code WARN} &rarr; {@code WARNING}</li>
 *   <li>{@code ERROR} &rarr; {@code SEVERE}</li>
 * </ul>
 *
 * <p>The logger properly handles caller location information by inspecting the stack trace
 * to determine the actual calling class and method, excluding the logging framework classes.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // JDKLogger is automatically used by LoggerFactory when appropriate
 * Logger logger = LoggerFactory.getLogger(MyClass.class);
 * logger.info("Application started");
 *
 * Exception exception = new Exception("Connection error");
 * logger.error("Failed to connect", exception);
 * }</pre>
 */
class JDKLogger extends AbstractLogger {

    static final String SELF = JDKLogger.class.getName();

    static final String SUPER = AbstractLogger.class.getName();

    private final java.util.logging.Logger loggerImpl;

    /**
     * Constructs a {@code JDKLogger} with the specified name.
     *
     * <p>The name is passed directly to the underlying {@code java.util.logging.Logger}.</p>
     *
     * <p><b>Note:</b> This class is package-private and this constructor should not be called directly
     * from outside the logging package. Use {@link LoggerFactory#getLogger(Class)} or {@link LoggerFactory#getLogger(String)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Correct way to obtain a logger
     * Logger logger = LoggerFactory.getLogger(MyClass.class);
     * logger.info("Logger initialized");
     * }</pre>
     *
     * @param name the name of the logger
     */
    public JDKLogger(final String name) {
        super(name);
        loggerImpl = java.util.logging.Logger.getLogger(name);
    }

    /**
     * Checks if {@code TRACE} level logging is enabled.
     *
     * <p>In JDK logging, {@code TRACE} is mapped to {@code FINEST} level.</p>
     *
     * @return {@code true} if this {@code Logger} is enabled for the {@code TRACE} level, {@code false} otherwise
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isLoggable(Level.FINEST);
    }

    /**
     * Logs a message at {@code TRACE} level.
     *
     * <p>The message is logged at {@code FINEST} level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void trace(final String msg) {
        log(Level.FINEST, msg);
    }

    /**
     * Logs a message at {@code TRACE} level with an exception.
     *
     * <p>The message and exception are logged at {@code FINEST} level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        log(Level.FINEST, msg, t);
    }

    /**
     * Checks if {@code DEBUG} level logging is enabled.
     *
     * <p>In JDK logging, {@code DEBUG} is mapped to {@code FINE} level.</p>
     *
     * @return {@code true} if this {@code Logger} is enabled for the {@code DEBUG} level, {@code false} otherwise
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isLoggable(Level.FINE);
    }

    /**
     * Logs a message at {@code DEBUG} level.
     *
     * <p>The message is logged at {@code FINE} level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void debug(final String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Logs a message at {@code DEBUG} level with an exception.
     *
     * <p>The message and exception are logged at {@code FINE} level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        log(Level.FINE, msg, t);
    }

    /**
     * Checks if {@code INFO} level logging is enabled.
     *
     * <p>In JDK logging, {@code INFO} level corresponds directly to {@code Level.INFO}.</p>
     *
     * @return {@code true} if this {@code Logger} is enabled for the {@code INFO} level, {@code false} otherwise
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isLoggable(Level.INFO);
    }

    /**
     * Logs a message at {@code INFO} level.
     *
     * <p>The message is logged at {@code INFO} level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void info(final String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Logs a message at {@code INFO} level with an exception.
     *
     * <p>The message and exception are logged at {@code INFO} level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        log(Level.INFO, msg, t);
    }

    /**
     * Checks if {@code WARN} level logging is enabled.
     *
     * <p>In JDK logging, {@code WARN} is mapped to {@code WARNING} level.</p>
     *
     * @return {@code true} if this {@code Logger} is enabled for the {@code WARN} level, {@code false} otherwise
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isLoggable(Level.WARNING);
    }

    /**
     * Logs a message at {@code WARN} level.
     *
     * <p>The message is logged at {@code WARNING} level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void warn(final String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Logs a message at {@code WARN} level with an exception.
     *
     * <p>The message and exception are logged at {@code WARNING} level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        log(Level.WARNING, msg, t);
    }

    /**
     * Checks if {@code ERROR} level logging is enabled.
     *
     * <p>In JDK logging, {@code ERROR} is mapped to {@code SEVERE} level.</p>
     *
     * @return {@code true} if this {@code Logger} is enabled for the {@code ERROR} level, {@code false} otherwise
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isLoggable(Level.SEVERE);
    }

    /**
     * Logs a message at {@code ERROR} level.
     *
     * <p>The message is logged at {@code SEVERE} level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void error(final String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Logs a message at {@code ERROR} level with an exception.
     *
     * <p>The message and exception are logged at {@code SEVERE} level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    /**
     * Logs a message at the specified level.
     *
     * <p>This method creates a {@link LogRecord} and fills in caller information before
     * delegating to the JDK logger.</p>
     *
     * @param level the logging level
     * @param msg the message to log
     */
    private void log(final Level level, final String msg) {
        log(SELF, level, msg, null);
    }

    /**
     * Logs a message at the specified level with an exception.
     *
     * <p>This method creates a {@link LogRecord} and fills in caller information before
     * delegating to the JDK logger.</p>
     *
     * @param level the logging level
     * @param msg the message to log
     * @param t the exception or error to log
     */
    private void log(final Level level, final String msg, final Throwable t) {
        log(SELF, level, msg, t);
    }

    /**
     * Logs a message at the specified level with the specified throwable if any.
     *
     * <p>This method creates a {@link LogRecord} and fills in caller data before calling
     * this instance's JDK logger. The caller information is determined by walking
     * the stack trace to find the first frame outside of the logging framework.</p>
     *
     * <p>See SLF4J bug report #13 for more details about why this approach is necessary.</p>
     *
     * @param callerFQCN the fully qualified class name of the caller
     * @param level the logging level
     * @param msg the message to log
     * @param t the exception or error to log
     */
    private void log(final String callerFQCN, final Level level, final String msg, final Throwable t) {
        // millis and thread are filled by the constructor
        final LogRecord logRecord = new LogRecord(level, msg);
        logRecord.setLoggerName(getName());
        logRecord.setThrown(t);
        // Note: parameters in LogRecord are not set because formatting is handled by AbstractLogger
        fillCallerData(callerFQCN, logRecord);
        loggerImpl.log(logRecord);
    }

    /**
     * Fills in caller data if possible by examining the stack trace.
     *
     * <p>This method walks the stack trace to find the first frame that is not part
     * of the logging framework (i.e., not {@code JDKLogger} or {@code AbstractLogger}), and uses that
     * frame's class and method information as the caller data.</p>
     *
     * <p>The algorithm:</p>
     * <ol>
     *   <li>Find the index of the logging framework class in the stack</li>
     *   <li>Skip all logging framework frames</li>
     *   <li>Use the first non-framework frame as the caller</li>
     * </ol>
     *
     * @param callerFQCN the fully qualified class name to search for in the stack
     * @param logRecord the {@code LogRecord} to update with caller information
     */
    private static void fillCallerData(final String callerFQCN, final LogRecord logRecord) {
        final StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (className.equals(callerFQCN) || className.equals(SUPER)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        if (selfIndex >= 0) {
            for (int i = selfIndex + 1; i < steArray.length; i++) {
                final String className = steArray[i].getClassName();
                if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                    found = i;
                    break;
                }
            }
        }

        if (found != -1) {
            final StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            logRecord.setSourceClassName(ste.getClassName());
            logRecord.setSourceMethodName(ste.getMethodName());
        }
    }
}
