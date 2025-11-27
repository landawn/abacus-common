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
 * Logger implementation that delegates to Java's built-in logging framework (java.util.logging).
 * 
 * <p>This implementation maps the Logger interface methods to JDK logging levels as follows:</p>
 * <ul>
 *   <li>TRACE → FINEST</li>
 *   <li>DEBUG → FINE</li>
 *   <li>INFO → INFO</li>
 *   <li>WARN → WARNING</li>
 *   <li>ERROR → SEVERE</li>
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
 * Exception exception = new Exception("Connection error");
 * logger.error("Failed to connect", exception);
 * }</pre>
 * 
 */
class JDKLogger extends AbstractLogger {

    static final String SELF = JDKLogger.class.getName();

    static final String SUPER = AbstractLogger.class.getName();

    private final java.util.logging.Logger loggerImpl;

    /**
     * Constructs a JDKLogger with the specified name.
     *
     * <p>The name is passed directly to the underlying java.util.logging.Logger.</p>
     *
     * <p><b>Note:</b> This constructor is package-private and should not be called directly.
     * Use {@link LoggerFactory#getLogger(Class)} or {@link LoggerFactory#getLogger(String)} instead.</p>
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
     * Checks if TRACE level logging is enabled.
     *
     * <p>In JDK logging, TRACE is mapped to FINEST level.</p>
     *
     * @return {@code true} if this Logger is enabled for the TRACE level, {@code false} otherwise
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isLoggable(Level.FINEST);
    }

    /**
     * Logs a message at TRACE level.
     * 
     * <p>The message is logged at FINEST level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void trace(final String msg) {
        log(Level.FINEST, msg);
    }

    /**
     * Logs a message at TRACE level with an exception.
     * 
     * <p>The message and exception are logged at FINEST level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        log(Level.FINEST, msg, t);
    }

    /**
     * Checks if DEBUG level logging is enabled.
     *
     * <p>In JDK logging, DEBUG is mapped to FINE level.</p>
     *
     * @return {@code true} if this Logger is enabled for the DEBUG level, {@code false} otherwise
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isLoggable(Level.FINE);
    }

    /**
     * Logs a message at DEBUG level.
     * 
     * <p>The message is logged at FINE level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void debug(final String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Logs a message at DEBUG level with an exception.
     * 
     * <p>The message and exception are logged at FINE level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        log(Level.FINE, msg, t);
    }

    /**
     * Checks if INFO level logging is enabled.
     *
     * <p>In JDK logging, INFO level corresponds directly to Level.INFO.</p>
     *
     * @return {@code true} if this Logger is enabled for the INFO level, {@code false} otherwise
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isLoggable(Level.INFO);
    }

    /**
     * Logs a message at INFO level.
     *
     * <p>The message is logged at INFO level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void info(final String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Logs a message at INFO level with an exception.
     *
     * <p>The message and exception are logged at INFO level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        log(Level.INFO, msg, t);
    }

    /**
     * Checks if WARN level logging is enabled.
     *
     * <p>In JDK logging, WARN is mapped to WARNING level.</p>
     *
     * @return {@code true} if this Logger is enabled for the WARN level, {@code false} otherwise
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isLoggable(Level.WARNING);
    }

    /**
     * Logs a message at WARN level.
     * 
     * <p>The message is logged at WARNING level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void warn(final String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Logs a message at WARN level with an exception.
     * 
     * <p>The message and exception are logged at WARNING level in JDK logging.</p>
     *
     * @param msg the message to log
     * @param t the exception or error to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        log(Level.WARNING, msg, t);
    }

    /**
     * Checks if ERROR level logging is enabled.
     *
     * <p>In JDK logging, ERROR is mapped to SEVERE level.</p>
     *
     * @return {@code true} if this Logger is enabled for the ERROR level, {@code false} otherwise
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isLoggable(Level.SEVERE);
    }

    /**
     * Logs a message at ERROR level.
     * 
     * <p>The message is logged at SEVERE level in JDK logging.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void error(final String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Logs a message at ERROR level with an exception.
     * 
     * <p>The message and exception are logged at SEVERE level in JDK logging.</p>
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
     * <p>This method creates a LogRecord and fills in caller information before
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
     * <p>This method creates a LogRecord and fills in caller information before
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
     * <p>This method creates a LogRecord and fills in caller data before calling
     * this instance's JDK logger. The caller information is determined by walking
     * the stack trace to find the first frame outside of the logging framework.</p>
     *
     * <p>See bug report #13 for more details about why this approach is necessary.</p>
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
        // Note: parameters in record are not set because SLF4J only
        // supports a single formatting style
        fillCallerData(callerFQCN, logRecord);
        loggerImpl.log(logRecord);
    }

    /**
     * Fills in caller data if possible by examining the stack trace.
     * 
     * <p>This method walks the stack trace to find the first frame that is not part
     * of the logging framework (i.e., not JDKLogger or AbstractLogger), and uses that
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
     * @param logRecord the LogRecord to update with caller information
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
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                found = i;
                break;
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
